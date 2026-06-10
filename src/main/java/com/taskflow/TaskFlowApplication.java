package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TASKFLOW — Project Management System
 * ======================================
 *
 * This is the MAIN CLASS — the entry point of your entire application.
 *
 * @SpringBootApplication = 3 annotations combined:
 *   1. @Configuration — this class can define @Bean methods
 *   2. @EnableAutoConfiguration — Spring Boot auto-configures based on classpath
 *   3. @ComponentScan — scans THIS package + all sub-packages for beans
 *
 * WHY this class must be at the ROOT package (com.taskflow)?
 *   Because @ComponentScan scans from HERE downward.
 *   If this was in com.taskflow.config, it wouldn't find com.taskflow.service!
 *
 * Package structure:
 *   com.taskflow/
 *       TaskFlowApplication.java    <-- YOU ARE HERE (@SpringBootApplication)
 *       config/                     <-- Configuration classes
 *       security/                   <-- JWT, filters, security config
 *       entity/                     <-- JPA entities (database tables)
 *       repository/                 <-- Data access layer (JPA repositories)
 *       dto/                        <-- Data Transfer Objects (API models)
 *           request/                <-- Incoming request DTOs
 *           response/               <-- Outgoing response DTOs
 *       service/                    <-- Business logic layer
 *       controller/                 <-- REST controllers (HTTP endpoints)
 *       exception/                  <-- Custom exceptions + global handler
 *       learn/                      <-- Learning reference docs (advanced topics)
 *
 * INTERVIEW Q: What happens when you call SpringApplication.run()?
 * A: 1. Creates ApplicationContext (the Spring container)
 *    2. Scans for @Component/@Service/@Repository/@Controller beans
 *    3. Auto-configures based on classpath (DataSource, Tomcat, etc.)
 *    4. Starts embedded Tomcat server
 *    5. Application is ready to handle HTTP requests!
 */
@SpringBootApplication
@EnableScheduling  // Activates @Scheduled annotation scanning — needed for Session 4 schedulers
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
    }
}
