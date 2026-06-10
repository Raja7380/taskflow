package com.taskflow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation that marks a service method for audit logging.
 *
 * HOW ANNOTATIONS WORK:
 *   An annotation by itself does nothing — it's just a marker/label.
 *   Something must READ the annotation and act on it.
 *   In this case, AuditLogAspect reads @Auditable at runtime and intercepts the method.
 *
 * @Target(METHOD) — can only be placed on methods (not classes, fields, etc.)
 * @Retention(RUNTIME) — annotation is kept in bytecode and readable at runtime.
 *   RUNTIME is required for AOP — the aspect reads it using reflection at runtime.
 *   CLASS = kept in bytecode but not readable at runtime (for compile tools)
 *   SOURCE = discarded after compilation (like @Override — just for IDE/compiler)
 *
 * USAGE:
 *   @Auditable(action = "CREATE_PROJECT", entityType = "Project")
 *   public ProjectResponse createProject(...) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {

    /** Human-readable name for the operation being performed. */
    String action() default "";

    /** The type of entity being operated on (Project, Task, User, etc.). */
    String entityType() default "";
}
