# Session 4: AOP, Spring Events, and Scheduling — Complete Learning Guide

---

## Table of Contents

1. [The Problem We're Solving](#1-the-problem-were-solving)
2. [AOP — Aspect-Oriented Programming](#2-aop--aspect-oriented-programming)
3. [How Spring AOP Works Internally (The Proxy)](#3-how-spring-aop-works-internally-the-proxy)
4. [Custom Annotations (@Auditable)](#4-custom-annotations-auditable)
5. [Spring Events — Decoupling with Events](#5-spring-events--decoupling-with-events)
6. [Why Events Instead of Direct Calls?](#6-why-events-instead-of-direct-calls)
7. [Propagation.REQUIRES_NEW — Why Audit Logs Need Their Own Transaction](#7-propagationrequires_new--why-audit-logs-need-their-own-transaction)
8. [Scheduling — Making Things Happen on a Timer](#8-scheduling--making-things-happen-on-a-timer)
9. [Cron Expressions Explained](#9-cron-expressions-explained)
10. [The Full Picture: Everything Together](#10-the-full-picture-everything-together)
11. [New Files Added in Session 4](#11-new-files-added-in-session-4)
12. [Database Tables Created](#12-database-tables-created)
13. [Testing With Swagger](#13-testing-with-swagger)
14. [Interview Q&A](#14-interview-qa)

---

## 1. The Problem We're Solving

Imagine you are building a real app like Jira. You need to:

1. **Log every action** — "User Alice updated Task #47 at 2:34 PM, it took 120ms"
2. **Send notifications** — "Bob's task is due tomorrow"
3. **Run background checks** — "Find all overdue tasks every hour"

The naive way to do all this is to put it all inside your service methods:

```java
public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
    long startTime = System.currentTimeMillis();
    try {
        Task task = ...; // create task
        taskRepository.save(task);
        
        // LOG IT
        auditLogRepository.save(new AuditLog(...));
        
        // NOTIFY
        notificationRepository.save(new Notification(...));
        
        // SEND EMAIL
        emailService.sendEmail(...);
        
        return TaskResponse.fromEntity(task);
    } catch (Exception e) {
        // LOG FAILURE
        auditLogRepository.save(new AuditLog(..., false, e.getMessage()));
        throw e;
    }
}
```

**Problem:** `createTask` now has 5 responsibilities. It's doing the task's job + logging + notifying + emailing. This is called "tangling" — unrelated concerns are mixed together.

Now multiply this by 20 methods (createProject, updateTask, deleteProject...). You copy-paste the audit logging code 20 times. When you need to change how logging works, you update 20 places. This is called **cross-cutting concerns** — functionality that cuts across many parts of your app.

Session 4 fixes this with:
- **AOP** — handles audit logging without touching your service methods
- **Spring Events** — notifications happen without your service knowing about them
- **Scheduling** — background jobs run on a timer, not triggered by user actions

---

## 2. AOP — Aspect-Oriented Programming

### What is AOP?

**AOP** is a way to add behavior to your code WITHOUT editing the original code.

Think of it like this:

You have 100 methods in your app. You want to log the execution time of all of them. Without AOP, you add timing code to all 100 methods. With AOP, you write the timing code ONCE, and tell Spring "apply this to all methods."

Real industry examples of what companies use AOP for:
- **Netflix**: Log every API call's duration and send to their Zipkin tracing system
- **Banks**: Record who accessed what data (compliance/audit requirement)
- **Amazon**: Retry failed HTTP calls automatically (3 retries on timeout)
- **Google**: Add authentication checks to internal services
- **Datadog**: Measure performance of database queries

### The Core Idea

AOP works around **join points** (moments in your code execution) and **advice** (code to run at those moments):

```
Normal execution: Controller → Service.createTask() → Repository
AOP with @Around: Controller → [AOP intercepts] → Service.createTask() → [AOP intercepts again] → Repository
```

The AOP code runs "around" your method — before AND after.

### Your AOP Code: AuditLogAspect

```java
@Aspect        // "This class contains AOP advice"
@Component     // Make it a Spring bean
@Slf4j         // Add Logger field via Lombok
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(auditable)")  // Run around any method annotated with @Auditable
    public Object auditServiceCall(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
        long startTime = System.currentTimeMillis();
        boolean success = true;
        String errorMessage = null;
        Object result = null;

        try {
            result = pjp.proceed();  // <-- This runs the ACTUAL method
            return result;
        } catch (Throwable t) {
            success = false;
            errorMessage = t.getMessage();
            throw t;  // Re-throw so the error still propagates normally
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            
            // Read who is logged in from Spring Security
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            // ... extract userId, userEmail ...
            
            // Save audit log in a SEPARATE transaction (REQUIRES_NEW)
            auditLogService.save(new AuditLog(
                auditable.action(),     // "CREATE_TASK"
                auditable.entityType(), // "Task"
                userId, userEmail,
                durationMs, success, errorMessage
            ));
        }
    }
}
```

**Line by line:**

| Code | Meaning |
|------|---------|
| `@Aspect` | Tells Spring: "this class intercepts method calls" |
| `@Around("@annotation(auditable)")` | Match any method that has `@Auditable` annotation |
| `ProceedingJoinPoint pjp` | Represents the intercepted method — you can call it or skip it |
| `pjp.proceed()` | Actually runs the intercepted method. Without this line, your method never executes |
| `Auditable auditable` | The annotation instance — lets you read `auditable.action()` and `auditable.entityType()` |
| `finally` block | Runs ALWAYS — even if the method throws an exception |
| `System.currentTimeMillis()` | Milliseconds since Jan 1, 1970 — standard way to measure duration |

---

## 3. How Spring AOP Works Internally (The Proxy)

This is a common interview question. Understanding this makes you stand out.

When you write:

```java
@Service
public class TaskService {
    @Auditable(action = "CREATE_TASK", entityType = "Task")
    public TaskResponse createTask(...) { ... }
}
```

Spring does NOT modify your `TaskService` class. Instead, it creates a **proxy** — a wrapper class that looks identical to `TaskService` from the outside:

```
WHAT YOU THINK HAPPENS:
  Controller calls TaskService.createTask()

WHAT ACTUALLY HAPPENS:
  Controller calls TaskServiceProxy.createTask()
    → TaskServiceProxy runs @Around advice (start timer)
    → TaskServiceProxy calls real TaskService.createTask()
    → TaskServiceProxy runs @Around advice (save audit log)
    → TaskServiceProxy returns result to Controller
```

The proxy is generated by Spring at startup using Java's `java.lang.reflect.Proxy` or CGLIB. This is why:
- `@Transactional` works (Spring creates a proxy that wraps the DB transaction)
- `@Cacheable` works (proxy checks cache before calling your method)
- `@Auditable` works (proxy intercepts and logs)

**Gotcha:** If method A in `TaskService` calls method B in the same class, Spring's AOP does NOT intercept method B's call because the call goes directly to the real object, not through the proxy. This is a famous "self-invocation" problem.

---

## 4. Custom Annotations (@Auditable)

Before Session 4, you only used annotations that Spring gave you (`@Service`, `@RestController`, etc.). In Session 4 you CREATE your own annotation.

### Why Create Custom Annotations?

Annotations are metadata attached to code. They don't do anything by themselves — they're just markers. The power comes from something READING that marker and acting on it.

In your case:
- `@Auditable` marks a method: "this method should be audit-logged"
- `AuditLogAspect` reads the marker and performs the logging

This is exactly how `@Transactional` works in Spring — it's just an annotation, and Spring's `TransactionInterceptor` reads it and wraps your method in a transaction.

### Your Annotation Code

```java
@Target(ElementType.METHOD)        // Can only be placed on methods
@Retention(RetentionPolicy.RUNTIME) // Available at runtime (not just compile time)
public @interface Auditable {
    String action();      // Required field: "CREATE_TASK", "DELETE_PROJECT", etc.
    String entityType();  // Required field: "Task", "Project", etc.
}
```

**@Target** options:
| Target | Where annotation can be placed |
|--------|-------------------------------|
| `METHOD` | On methods only |
| `TYPE` | On classes/interfaces |
| `FIELD` | On fields |
| `PARAMETER` | On method parameters |

**@Retention** options:
| Retention | Meaning |
|-----------|---------|
| `RUNTIME` | Available at runtime via reflection — needed for AOP |
| `CLASS` | Only in .class files — not available at runtime |
| `SOURCE` | Only in source code — discarded by compiler (e.g., `@Override`) |

Usage in your services:

```java
@Auditable(action = "CREATE_TASK", entityType = "Task")
public TaskResponse createTask(...) { ... }

@Auditable(action = "DELETE_PROJECT", entityType = "Project")
public void deleteProject(...) { ... }
```

Now every time these methods run, the `AuditLogAspect` intercepts them and saves an audit log.

---

## 5. Spring Events — Decoupling with Events

### The Problem Before Events

Before Spring Events, your `TaskService.createTask()` had to directly call `NotificationService`:

```java
// Without Events — tightly coupled
public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
    Task task = taskRepository.save(...);
    
    // TaskService needs to KNOW ABOUT NotificationService
    notificationService.notifyProjectOwner(task, currentUser);
    notificationService.notifyAssignee(task);
    // What if we add email notifications tomorrow? Edit TaskService again.
    // What if we add Slack notifications next week? Edit TaskService AGAIN.
    
    return TaskResponse.fromEntity(task);
}
```

**Problem:** `TaskService` now depends on `NotificationService`. They're coupled together. Every new notification type requires editing the task creation code.

### The Solution: Spring Events

Instead of calling `NotificationService` directly, `TaskService` publishes an **event** — a simple announcement: "A task was created."

`NotificationService` (now called `NotificationListener`) listens for that event and reacts.

```
BEFORE (tightly coupled):
  TaskService ──calls──> NotificationService
  TaskService ──calls──> EmailService
  TaskService ──calls──> SlackService

AFTER (decoupled with events):
  TaskService ──publishes──> TaskCreatedEvent
                                 └──> NotificationListener handles it
                                 └──> EmailListener handles it      (add without touching TaskService)
                                 └──> SlackListener handles it      (add without touching TaskService)
```

### Your Event Code

**The Event** (just a plain Java class — no Spring magic required):

```java
// TaskCreatedEvent.java
@Getter
@AllArgsConstructor
public class TaskCreatedEvent {
    private final Task task;
    private final User creator;
}
```

It's just a data carrier. No annotation, no interface, no inheritance required in modern Spring.

**Publishing the Event** (in TaskService):

```java
private final ApplicationEventPublisher eventPublisher; // Injected by Spring

@Auditable(action = "CREATE_TASK", entityType = "Task")
public TaskResponse createTask(CreateTaskRequest request, User currentUser) {
    Task savedTask = taskRepository.save(...);
    
    // Just announce what happened — don't care who's listening
    eventPublisher.publishEvent(new TaskCreatedEvent(savedTask, currentUser));
    
    return TaskResponse.fromEntity(savedTask);
}
```

**Listening for the Event** (in NotificationListener):

```java
@Component
public class NotificationListener {

    private final NotificationService notificationService;

    @EventListener  // "Call this method when a TaskCreatedEvent is published"
    public void handleTaskCreated(TaskCreatedEvent event) {
        Task task = event.getTask();
        User creator = event.getCreator();
        User projectOwner = task.getProject().getOwner();
        
        // Notify project owner if they didn't create the task themselves
        if (!projectOwner.getId().equals(creator.getId())) {
            notificationService.create(
                projectOwner.getId(),
                "New task created in your project: " + task.getTitle(),
                NotificationType.TASK_CREATED,
                task.getId(),
                "Task"
            );
        }
    }
}
```

**How it flows:**

```
1. createTask() calls eventPublisher.publishEvent(new TaskCreatedEvent(...))
2. Spring's ApplicationEventPublisher looks up all @EventListener methods
   that accept TaskCreatedEvent as a parameter
3. Spring calls NotificationListener.handleTaskCreated(event)
4. (synchronously by default — same thread, same transaction)
```

**By default Spring Events are SYNCHRONOUS** — the listener runs in the same thread and transaction as the publisher. For async, add `@Async` to the listener (out of scope for now, but good to know).

---

## 6. Why Events Instead of Direct Calls?

This is the "Open-Closed Principle" from SOLID design principles. One of the most important concepts in software engineering.

> **Open-Closed Principle:** Code should be OPEN for extension but CLOSED for modification.

Meaning: you should be able to add new behavior without changing existing code.

Without events: to add "send Slack notification when task is created," you edit `TaskService.java`. You're modifying existing code.

With events: to add "send Slack notification when task is created," you add a new `SlackNotificationListener.java`. You're extending the system. `TaskService` never changes.

**Real industry example:** Uber uses an event-driven architecture for their trip lifecycle:
- `TripCreatedEvent` → notifies driver app + rider app + analytics + payment pre-auth
- None of these services need to know about each other
- They all just react to the same event

This is how large-scale systems stay manageable as they grow.

---

## 7. Propagation.REQUIRES_NEW — Why Audit Logs Need Their Own Transaction

### The Problem

Imagine this scenario:

```
1. User calls updateTask()
2. @Transactional starts Transaction A
3. task.setStatus(DONE)  ← change inside Transaction A
4. auditLogService.save(new AuditLog(...)) ← also inside Transaction A
5. CRASH! Some exception thrown
6. Transaction A ROLLS BACK
7. task.setStatus(DONE) is undone — correct!
8. auditLogService.save() is ALSO undone — WRONG!
```

The audit log should have survived! The whole point of audit logs is to record WHAT HAPPENED — including failures. If the audit log gets deleted when the main operation fails, you can't investigate what went wrong.

### The Fix: REQUIRES_NEW

```java
@Service
public class AuditLogService {

    @Transactional(propagation = Propagation.REQUIRES_NEW)  // Key annotation
    public void save(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
```

`REQUIRES_NEW` tells Spring: "Always start a BRAND NEW transaction, separate from any existing transaction."

```
WHAT HAPPENS NOW:
1. User calls updateTask()
2. @Transactional starts Transaction A
3. task.setStatus(DONE)  ← inside Transaction A
4. auditLogService.save() is called
   → REQUIRES_NEW suspends Transaction A
   → Starts Transaction B (separate)
   → Saves audit log in Transaction B
   → Transaction B COMMITS immediately (audit log is now permanent in DB)
   → Transaction A is resumed
5. CRASH! Exception thrown
6. Transaction A ROLLS BACK
7. task.setStatus(DONE) is undone — correct
8. Audit log from Transaction B is ALREADY COMMITTED — it survives!
```

### Transaction Propagation Options (Interview Ready)

| Propagation | Meaning | When to Use |
|-------------|---------|-------------|
| `REQUIRED` (default) | Join existing tx, or create new | Most service methods |
| `REQUIRES_NEW` | Always create new tx, suspend existing | Audit logs, notifications that must persist |
| `SUPPORTS` | Join existing tx if present, otherwise no tx | Read-only queries that don't need a tx |
| `NOT_SUPPORTED` | Suspend existing tx, run without tx | Non-DB operations inside tx |
| `MANDATORY` | Must have existing tx or throw | Internal methods that must be called from within a tx |
| `NEVER` | Throw if tx exists | Methods that must never run inside a tx |

---

## 8. Scheduling — Making Things Happen on a Timer

### Why Scheduling Exists

Not everything in software is request-driven (user clicks button → response). Some things must happen automatically:

| Industry | Example | Schedule |
|----------|---------|----------|
| Banking | Statement generation | 1st of every month |
| E-commerce (Amazon) | "Abandoned cart" email | 3 hours after cart abandonment |
| Social media | Trending topics recalculation | Every 15 minutes |
| SaaS (Jira) | "Task X is due tomorrow" reminders | Every morning at 9 AM |
| DevOps (GitHub Actions) | Nightly build tests | Every day at 2 AM |
| Finance | Market data refresh | Every second during trading hours |
| Your app | Due-date reminders | Every morning at 9 AM |
| Your app | Overdue task detection | Every hour |

### Enabling Scheduling

One annotation on the main application class:

```java
@SpringBootApplication
@EnableScheduling  // Without this, @Scheduled annotations are silently ignored
public class TaskFlowApplication { ... }
```

### Your Scheduled Methods

```java
@Component
@Slf4j
@RequiredArgsConstructor
public class TaskReminderScheduler {

    @Scheduled(cron = "0 0 9 * * *")     // Every day at 9:00 AM
    @Transactional(readOnly = true)
    public void sendDueTodayReminders() {
        LocalDate today = LocalDate.now();
        List<Task> tasksDueToday = taskRepository.findTasksDueOnDate(today);
        // ... create notifications
    }

    @Scheduled(cron = "0 5 9 * * *")     // Every day at 9:05 AM
    @Transactional(readOnly = true)
    public void sendDueTomorrowReminders() { ... }
}

@Component
public class OverdueTaskScheduler {

    @Scheduled(fixedDelay = 3600000, initialDelay = 60000)
    // fixedDelay: run 1 hour AFTER previous run finishes
    // initialDelay: wait 1 minute after startup before first run
    public void detectAndNotifyOverdueTasks() { ... }
}
```

---

## 9. Cron Expressions Explained

A cron expression has 6 fields separated by spaces (Spring uses 6; Unix uses 5):

```
"0  0  9  *  *  *"
 |  |  |  |  |  |
 |  |  |  |  |  └── Day of week (0-7, SUN=0/7, MON=1...SAT=6, or MON,TUE,...)
 |  |  |  |  └───── Month (1-12, or JAN,FEB,...)
 |  |  |  └──────── Day of month (1-31)
 |  |  └─────────── Hour (0-23)
 |  └────────────── Minute (0-59)
 └───────────────── Second (0-59)
```

**`*`** means "every value" (every minute, every hour, every day, etc.)

### Common Patterns (Memorize These)

| Expression | Meaning |
|------------|---------|
| `"0 0 9 * * *"` | Every day at 9:00:00 AM |
| `"0 30 8 * * *"` | Every day at 8:30:00 AM |
| `"0 0 9 * * MON-FRI"` | 9 AM on weekdays only |
| `"0 0 0 1 * *"` | Midnight on 1st of every month |
| `"0 0 * * * *"` | Every hour on the hour |
| `"0 */15 * * * *"` | Every 15 minutes |
| `"0 0 2 * * SUN"` | Every Sunday at 2 AM (weekly batch job) |
| `"0 0 9,17 * * MON-FRI"` | 9 AM and 5 PM on weekdays |

**`*/15`** means "every 15" (*/5 = every 5, */30 = every 30)

**Use crontab.guru** (website) in real life to generate and test cron expressions.

---

## 10. The Full Picture: Everything Together

Here is the complete flow when a user creates a task:

```
HTTP POST /api/tasks
         |
         v
TaskController.createTask()
   @AuthenticationPrincipal extracts User from JWT
         |
         v
[AuditLogAspect intercepts — @Around("@annotation(auditable)")]
   records startTime = System.currentTimeMillis()
         |
         v
TaskService.createTask(request, currentUser)   <── ACTUAL METHOD
   1. Load project from DB
   2. Build Task entity (title, description, dueDate, etc.)
   3. taskRepository.save(task)  → INSERT INTO tasks (...)
   4. eventPublisher.publishEvent(new TaskCreatedEvent(task, currentUser))
         |
         | Spring Event published
         v
   [NotificationListener.handleTaskCreated(event)]  <── SAME THREAD
      if project owner != creator:
         notificationService.create(...)
         → notificationRepository.save(...)  → INSERT INTO notifications (...)
         |
         v
   return TaskResponse.fromEntity(savedTask)
         |
         v
[AuditLogAspect — finally block runs]
   calculates durationMs
   reads userId/email from SecurityContextHolder
   auditLogService.save(new AuditLog(...))
         |
         v
   [AuditLogService.save() — REQUIRES_NEW transaction]
      INSERT INTO audit_logs (action, entity_type, user_id, duration_ms, success, timestamp)
      Transaction COMMITS immediately (separate from outer transaction)
         |
         v
HTTP 201 Created — TaskResponse JSON returned to client
```

Three things happened in one request:
1. The task was created (main purpose)
2. A notification was created (via event — decoupled)
3. An audit log was created (via AOP — completely transparent)

`TaskService` only knows about step 1. Steps 2 and 3 are invisible to it.

---

## 11. New Files Added in Session 4

### Annotation
- `src/main/java/com/taskflow/annotation/Auditable.java`
  — Custom annotation; marks service methods for audit logging

### Entities
- `src/main/java/com/taskflow/entity/AuditLog.java`
  — DB record of every action: who, what, when, how long, success/fail
- `src/main/java/com/taskflow/entity/Notification.java`
  — In-app notification for a user
- `src/main/java/com/taskflow/entity/NotificationType.java`
  — Enum: TASK_CREATED, TASK_ASSIGNED, TASK_STATUS_CHANGED, etc.

### Events (plain Java objects)
- `src/main/java/com/taskflow/event/TaskCreatedEvent.java`
- `src/main/java/com/taskflow/event/TaskAssignedEvent.java`
- `src/main/java/com/taskflow/event/TaskStatusChangedEvent.java`
- `src/main/java/com/taskflow/event/ProjectCreatedEvent.java`

### Repositories
- `src/main/java/com/taskflow/repository/AuditLogRepository.java`
- `src/main/java/com/taskflow/repository/NotificationRepository.java`

### Services
- `src/main/java/com/taskflow/service/AuditLogService.java`
  — save() uses REQUIRES_NEW so audit logs survive outer rollback
- `src/main/java/com/taskflow/service/NotificationService.java`
  — create, getMyNotifications, countUnread, markAsRead, markAllAsRead

### AOP
- `src/main/java/com/taskflow/aspect/AuditLogAspect.java`
  — Intercepts @Auditable methods, measures duration, saves AuditLog

### Event Listener
- `src/main/java/com/taskflow/listener/NotificationListener.java`
  — Handles all task/project events and creates notifications

### Schedulers
- `src/main/java/com/taskflow/scheduler/TaskReminderScheduler.java`
  — Cron jobs at 9:00 AM and 9:05 AM for due-today/due-tomorrow reminders
- `src/main/java/com/taskflow/scheduler/OverdueTaskScheduler.java`
  — Hourly job to detect tasks past their due date

### Controllers
- `src/main/java/com/taskflow/controller/NotificationController.java`
  — GET /api/notifications, unread-count, mark-as-read
- `src/main/java/com/taskflow/controller/AuditLogController.java`
  — GET /api/audit-logs (admin only), /api/audit-logs/my

### Modified Files
- `TaskFlowApplication.java` — added @EnableScheduling
- `TaskService.java` — added @Auditable, eventPublisher calls
- `ProjectService.java` — added @Auditable, eventPublisher calls
- `TaskRepository.java` — added findTasksDueOnDate(), findOverdueTasks()

---

## 12. Database Tables Created

Spring Boot (via Hibernate) will auto-create these tables:

```sql
-- Audit log: every important action recorded here
CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    action       VARCHAR(100) NOT NULL,        -- "CREATE_TASK", "DELETE_PROJECT"
    entity_type  VARCHAR(100) NOT NULL,        -- "Task", "Project"
    user_id      BIGINT,                       -- who did it
    user_email   VARCHAR(255),                 -- denormalized snapshot (email at time of action)
    duration_ms  BIGINT,                       -- how long the operation took
    success      BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,                        -- null if success=true
    timestamp    TIMESTAMP NOT NULL
);

-- Notifications: in-app notification inbox
CREATE TABLE notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL,        -- who receives this notification
    message             TEXT NOT NULL,          -- "Task X is due tomorrow"
    type                VARCHAR(50) NOT NULL,   -- TASK_DUE_SOON, TASK_OVERDUE, etc.
    related_entity_id   BIGINT,                 -- the task or project this is about
    related_entity_type VARCHAR(50),            -- "Task" or "Project"
    read                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL
);
```

**Why `user_email` is stored in audit_logs even though you have a `user_id`?**

This is called "denormalization" — intentionally duplicating data.

If you store only `user_id`, and a user later changes their email, the audit log query would need to join to the users table to get their email. Worse, if the user is deleted, the audit log would lose the email entirely.

By storing `user_email` at the time of the action, the audit log is a permanent historical record: "User ID 42, whose email was alice@company.com at the time, performed this action." This is standard practice for audit systems in regulated industries (banking, healthcare).

---

## 13. Testing With Swagger

Start the app and open `http://localhost:8080/swagger-ui.html`.

### Test 1: Create a task and verify audit log

1. Register + login as User A, copy the JWT token
2. Create a project (POST /api/projects)
3. Create a task in that project (POST /api/tasks)
4. Check audit logs: GET /api/audit-logs/my
   → You should see two entries: CREATE_PROJECT and CREATE_TASK
   → Each entry shows action, entityType, userId, durationMs, success=true, timestamp

### Test 2: Verify notification was created

1. After creating a task in someone else's project, check notifications
2. GET /api/notifications → should show "New task created in project: ..."
3. GET /api/notifications/unread-count → should show {"unreadCount": 1}
4. PUT /api/notifications/{id}/read → mark it as read
5. GET /api/notifications/unread-count → should show {"unreadCount": 0}

### Test 3: Verify audit log on failure

1. Try to update a task to an invalid status transition (e.g., DONE → TODO)
2. The 400 error is returned
3. GET /api/audit-logs/my → the failed attempt is recorded with success=false and errorMessage filled in

### Test 4: Verify state machine events

1. Create a task (status = TODO)
2. Update status to IN_PROGRESS → audit log + TASK_STATUS_CHANGED notification for reporter
3. Update status to IN_REVIEW → another notification
4. Update status to DONE → final notification

---

## 14. Interview Q&A

**Q: What is AOP and what is it used for in Spring?**

A: AOP (Aspect-Oriented Programming) lets you add behavior to methods across your codebase without modifying each method individually. In Spring, it's used for cross-cutting concerns: logging, transaction management (@Transactional is AOP internally), security checks, performance monitoring, and retry logic. Spring implements AOP using proxies — a wrapper class is generated at startup that intercepts method calls.

---

**Q: What is the difference between @Around, @Before, and @After advice?**

A: `@Before` runs before the method. `@After` runs after (always, even on exception). `@AfterReturning` runs only on success. `@AfterThrowing` runs only on exception. `@Around` wraps the entire execution — you control whether the method runs at all by calling `pjp.proceed()`. `@Around` is the most powerful and most common for audit logging and timing.

---

**Q: What is Spring Event? When would you use it instead of a direct method call?**

A: Spring Events implement the Observer pattern — one component publishes an event, and zero or more components react to it without being explicitly coupled. Use events when: (1) the publisher doesn't need the listener's result, (2) multiple listeners might react to the same event, (3) you want to add new behavior without modifying existing code (Open-Closed Principle). Direct method calls are simpler and fine when there's exactly one consumer and the publisher needs the result.

---

**Q: What is @Transactional(propagation = REQUIRES_NEW) and when do you use it?**

A: `REQUIRES_NEW` suspends any existing transaction and starts a completely new, independent transaction. Use it when the operation must persist even if the outer transaction rolls back — classic example is audit logging. If you used the default `REQUIRED` propagation for audit logs, a failed business operation would roll back the audit log too, losing the record of the failure.

---

**Q: What is @Retention(RUNTIME) vs @Retention(CLASS)?**

A: `@Retention(RUNTIME)` means the annotation is available at runtime via Java Reflection API — required for AOP, dependency injection, and most Spring features. `@Retention(CLASS)` stores the annotation in `.class` files but strips it at runtime — useful for bytecode tools. `@Retention(SOURCE)` discards the annotation after compilation — used for `@Override`, `@SuppressWarnings`. Most Spring annotations use RUNTIME.

---

**Q: What is the difference between fixedDelay and fixedRate in @Scheduled?**

A: `fixedRate` triggers execution every N milliseconds from the START of the previous execution — if a run takes longer than N ms, the next run starts immediately when the previous one finishes (or can overlap if `@Async` is used). `fixedDelay` waits N milliseconds after the END of the previous execution — safer for jobs that can vary in duration. Use `fixedDelay` when you want a guaranteed pause between runs; use `fixedRate` when you want consistent timing regardless of execution time.

---

**Q: How does `@EnableScheduling` work? What happens if you forget it?**

A: `@EnableScheduling` imports Spring's `SchedulingConfiguration` which registers a `ScheduledAnnotationBeanPostProcessor`. This post-processor scans all beans at startup, finds methods annotated with `@Scheduled`, and registers them with the `TaskScheduler`. Without `@EnableScheduling`, the post-processor never runs, and `@Scheduled` annotations are silently ignored — your jobs simply never fire, with no error message.

---

**Q: SOLID principles — what is the Open-Closed Principle and how does it relate to events?**

A: Open-Closed Principle: classes should be open for extension but closed for modification. With Spring Events, `TaskService` is closed for modification — creating a task always publishes `TaskCreatedEvent`. But it's open for extension — you can add new listeners (`EmailListener`, `SlackListener`, `AnalyticsListener`) without changing `TaskService`. This is the event pattern's main architectural advantage over direct method calls.
