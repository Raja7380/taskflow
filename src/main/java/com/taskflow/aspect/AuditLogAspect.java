package com.taskflow.aspect;

import com.taskflow.annotation.Auditable;
import com.taskflow.entity.AuditLog;
import com.taskflow.entity.User;
import com.taskflow.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ============================================================
 * AUDIT LOG ASPECT — The heart of AOP in this application.
 * ============================================================
 *
 * WHAT IS AOP (Aspect-Oriented Programming)?
 *
 * In a real application, many methods need the SAME cross-cutting behavior:
 *   - Logging: "method X was called at time T by user U"
 *   - Security: "check permission before running method X"
 *   - Transaction: "wrap method X in a database transaction"
 *   - Caching: "cache the result of method X"
 *   - Timing: "measure how long method X takes"
 *
 * WITHOUT AOP: you copy-paste this behavior into every method:
 *   public ProjectResponse createProject(...) {
 *       long start = System.currentTimeMillis();
 *       log.info("Starting createProject...");
 *       try {
 *           // actual business logic here (10 lines)
 *           // ...
 *           log.info("createProject took {} ms", duration);
 *           saveAuditLog("CREATE_PROJECT", ...);
 *           return result;
 *       } catch (Exception e) {
 *           saveAuditLog("CREATE_PROJECT", false, e.getMessage(), ...);
 *           throw e;
 *       }
 *   }
 *   // Repeat this boilerplate for EVERY service method → 500 lines of repeated code
 *
 * WITH AOP: write the behavior ONCE in an Aspect. Apply it wherever needed with an annotation.
 *   public ProjectResponse createProject(...) {
 *       // only actual business logic — 10 lines
 *   }
 *   // The aspect handles logging/auditing automatically for all @Auditable methods
 *
 * The "ASPECT" = the cross-cutting concern (audit logging in this case).
 * The "ADVICE" = the code that runs (our @Around method).
 * The "POINTCUT" = where it runs ("on methods annotated with @Auditable").
 * The "JOIN POINT" = the specific method execution being intercepted.
 *
 * HOW IT WORKS INTERNALLY (Spring AOP with Proxies):
 *   When Spring creates a @Service bean, it wraps it in a PROXY object.
 *   The proxy looks like the real service but intercepts method calls.
 *   When you call projectService.createProject():
 *     1. You're actually calling the PROXY's createProject()
 *     2. The proxy runs the @Around advice (our AuditLogAspect code)
 *     3. The advice calls pjp.proceed() → the REAL createProject() runs
 *     4. Advice captures the result/exception, saves audit log
 *     5. Returns result to the caller
 *   The caller never knows the proxy exists — it's transparent.
 *
 * TYPES OF ADVICE (when the aspect code runs):
 *   @Before    — runs BEFORE the method (can't modify return value)
 *   @After     — runs AFTER (always, success or failure)
 *   @AfterReturning — runs only on SUCCESS
 *   @AfterThrowing  — runs only on EXCEPTION
 *   @Around    — wraps the method, has full control
 *                 → we use this because we need timing AND success/failure info
 *
 * @Transactional, @Cacheable, @PreAuthorize ARE all AOP!
 * Spring uses the same proxy mechanism to implement all of them.
 * When you put @Transactional on a method, Spring generates a proxy that
 * wraps the method in BEGIN TRANSACTION / COMMIT / ROLLBACK logic.
 * You just wrote @Transactional and Spring's built-in AOP does the rest.
 *
 * This is one of Spring's biggest superpowers — AOP makes cross-cutting concerns
 * invisible to your business logic.
 * ============================================================
 *
 * @Aspect — marks this class as containing AOP advice
 * @Component — Spring needs to manage this as a bean to wire AuditLogService in
 * @Slf4j — Lombok adds: private static final Logger log = LoggerFactory.getLogger(...)
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    /**
     * @Around("@annotation(auditable)") — this is the POINTCUT expression.
     *
     * Meaning: "Run this advice Around any method that has @Auditable annotation."
     * The parameter name "auditable" must match the method parameter name.
     * Spring automatically binds the @Auditable annotation instance to that parameter,
     * so we can read auditable.action() and auditable.entityType().
     *
     * ProceedingJoinPoint pjp — gives us:
     *   pjp.proceed()           — call the actual intercepted method
     *   pjp.getSignature()      — method name, class name
     *   pjp.getArgs()           — method arguments
     */
    @Around("@annotation(auditable)")
    public Object auditServiceCall(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        Object result = null;

        try {
            result = pjp.proceed();  // call the actual service method
            return result;

        } catch (Throwable throwable) {
            success = false;
            errorMessage = throwable.getMessage();
            throw throwable;  // re-throw so the caller still sees the exception

        } finally {
            // 'finally' ALWAYS runs — whether success or exception.
            // This is how we guarantee the audit log is always saved.
            long durationMs = System.currentTimeMillis() - startTime;

            // Extract current user from Spring Security context.
            // The JwtAuthenticationFilter already set this for the current request.
            Long userId = null;
            String userEmail = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User user) {
                // 'instanceof User user' = Java 16+ pattern matching
                // Same as: if (auth.getPrincipal() instanceof User) { User user = (User) auth.getPrincipal(); }
                userId = user.getId();
                userEmail = user.getEmail();
            }

            AuditLog auditLog = AuditLog.builder()
                    .action(auditable.action())
                    .entityType(auditable.entityType())
                    .userId(userId)
                    .userEmail(userEmail)
                    .durationMs(durationMs)
                    .success(success)
                    .errorMessage(success ? null : errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            // REQUIRES_NEW = saves in a separate transaction.
            // Even if the outer service transaction rolls back, this audit log is saved.
            auditLogService.save(auditLog);

            log.info("[AUDIT] {} | user={} | success={} | {}ms",
                    auditable.action(), userEmail, success, durationMs);
        }
    }
}
