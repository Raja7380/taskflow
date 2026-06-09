# TaskFlow — Complete Project Master Plan

> **PURPOSE**: This document is the complete blueprint of the TaskFlow project.
> It contains everything that has been done AND everything that needs to be done.
> 
> **PORTABLE**: You can give this document to ANY AI (ChatGPT, Claude, Cursor, Copilot, Devin, or any other)
> and they will know exactly where to pick up and what to build next.
> 
> **Last Updated**: Session 1 complete (June 2026)

---

## TABLE OF CONTENTS

1. [Project Overview](#1-project-overview)
2. [Tech Stack (Complete)](#2-tech-stack-complete)
3. [Repo Info & Setup](#3-repo-info--setup)
4. [What's DONE (Session 1)](#4-whats-done-session-1)
5. [Session 2: Projects & Tasks CRUD](#5-session-2-projects--tasks-crud)
6. [Session 3: Workflow + Filtering + Pagination](#6-session-3-workflow--filtering--pagination)
7. [Session 4: AOP + Events + Scheduling](#7-session-4-aop--events--scheduling)
8. [Session 5: Razorpay Payment Integration](#8-session-5-razorpay-payment-integration)
9. [Session 6: Redis Caching + Performance](#9-session-6-redis-caching--performance)
10. [Session 7: Apache Kafka + Event-Driven](#10-session-7-apache-kafka--event-driven)
11. [Session 8: React Frontend — Auth + Setup](#11-session-8-react-frontend--auth--setup)
12. [Session 9: React Frontend — Dashboard + Tasks](#12-session-9-react-frontend--dashboard--tasks)
13. [Session 10: Docker + Testing + Deployment](#13-session-10-docker--testing--deployment)
14. [Learning Folders (Reference Guides)](#14-learning-folders-reference-guides)
15. [Coding Conventions](#15-coding-conventions)
16. [How to Give This to Another AI](#16-how-to-give-this-to-another-ai)

---

## 1. Project Overview

**TaskFlow** is a full-stack Project Management System built to:
- Demonstrate enterprise Java development skills for resume/interviews
- Cover 100+ technologies/concepts companies hiring at 15+ LPA expect
- Serve as BOTH a working application AND a learning resource (every file has detailed WHY/HOW comments + interview Q&A)

**Target User**: Raja — 2026 BTech graduate, learning Java + Spring + React from scratch.
**Goal**: Land a 15+ LPA job within 1 month with this project on resume.
**Timeline**: ~10 sessions across 1 month (aggressive pace).

---

## 2. Tech Stack (Complete)

### Backend (Java/Spring)
| Technology | Version | Purpose |
|-----------|---------|---------|
| Java | 17 | Language |
| Spring Boot | 3.5.0 | Application framework |
| Spring Security 6 | (via Boot) | Authentication & authorization |
| Spring Data JPA | (via Boot) | Database access (ORM) |
| Hibernate | (via JPA) | Object-relational mapping |
| JWT (jjwt) | 0.12.6 | Stateless authentication tokens |
| Lombok | (via Boot) | Reduce boilerplate code |
| Bean Validation | (via Boot) | Input validation (@NotBlank, @Email) |
| MapStruct | TBD (Session 2) | DTO ↔ Entity mapping |
| Spring AOP | (via Boot) | Cross-cutting concerns (logging, audit) |
| Spring Events | (via Boot) | Event-driven architecture |
| Spring Scheduler | (via Boot) | Cron jobs, @Scheduled tasks |
| Spring Cache + Redis | TBD (Session 6) | Caching layer |
| Spring Retry | TBD (Session 4) | Retry failed operations |
| Springdoc OpenAPI | 2.8.8 | Swagger/API documentation |

### Database
| Technology | Purpose |
|-----------|---------|
| H2 | Development (in-memory, zero setup) |
| PostgreSQL 16 | Production (via Docker) |
| Flyway | Database migrations (Session 2+) |
| Redis 7 | Caching (Session 6) |

### Messaging
| Technology | Purpose |
|-----------|---------|
| Apache Kafka | Event streaming (Session 7) |

### Payments
| Technology | Purpose |
|-----------|---------|
| Razorpay Java SDK | Payment integration (Session 5) |

### Frontend
| Technology | Purpose |
|-----------|---------|
| React 18 | UI framework |
| TypeScript | Type-safe JavaScript |
| Vite | Build tool |
| Tailwind CSS | Styling |
| React Router | Page navigation |
| Axios | HTTP client (calls Spring Boot API) |
| Zustand or Redux Toolkit | State management |
| React Query (TanStack) | Server state caching |
| React Hook Form | Form handling |
| Recharts | Dashboard charts |

### DevOps
| Technology | Purpose |
|-----------|---------|
| Docker | Containerization |
| Docker Compose | Multi-container (app + DB + Redis + Kafka) |
| GitHub Actions | CI/CD pipeline |

### Testing
| Technology | Purpose |
|-----------|---------|
| JUnit 5 | Unit testing |
| Mockito | Mocking |
| Spring Boot Test | Integration testing |
| JaCoCo | Code coverage |

---

## 3. Repo Info & Setup

**GitHub URL**: https://github.com/Raja7380/taskflow.git
**Default Branch**: main
**Current Feature Branch**: devin/session-1-project-setup (merged or ready to merge)

**Git Commit Convention**: All commits under:
- Name: `Raja`
- Email: `rajasingh12587@gmail.com`
- Use env vars: `GIT_AUTHOR_NAME="Raja" GIT_AUTHOR_EMAIL="rajasingh12587@gmail.com" GIT_COMMITTER_NAME="Raja" GIT_COMMITTER_EMAIL="rajasingh12587@gmail.com"`

**How to Run (Dev mode with H2)**:
```bash
git clone https://github.com/Raja7380/taskflow.git
cd taskflow
./mvnw spring-boot:run
# App at: http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# H2 Console: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:mem:taskflow)
```

**How to Run (with Docker — PostgreSQL + Redis)**:
```bash
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

**Build Tool**: Maven (with .mvn wrapper)
**Java Version**: 17 (set in pom.xml)

---

## 4. What's DONE (Session 1)

### Status: COMPLETE ✓

### Files Created (24 Java files + configs + docs):

**Entry Point:**
```
src/main/java/com/taskflow/TaskFlowApplication.java
  - @SpringBootApplication entry point
  - Detailed comments on what @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
```

**Entity Layer:**
```
src/main/java/com/taskflow/entity/Role.java
  - Enum: USER, MANAGER, ADMIN
  - Comments on EnumType.STRING vs ORDINAL

src/main/java/com/taskflow/entity/User.java
  - @Entity with @Table(name="users")
  - Fields: id, fullName, email, password (BCrypt hashed), role, createdAt, updatedAt
  - Implements UserDetails (Spring Security interface)
  - @PrePersist / @PreUpdate lifecycle hooks
  - Lombok: @Data, @Builder, @NoArgsConstructor, @AllArgsConstructor
  - getAuthorities() returns "ROLE_" + role.name()
```

**Repository Layer:**
```
src/main/java/com/taskflow/repository/UserRepository.java
  - extends JpaRepository<User, Long>
  - findByEmail(String email) → Optional<User>
  - existsByEmail(String email) → boolean
  - Comments on derived query methods
```

**DTO Layer:**
```
src/main/java/com/taskflow/dto/request/RegisterRequest.java
  - fullName (@NotBlank, @Size 2-100)
  - email (@NotBlank, @Email)
  - password (@NotBlank, @Size min=8)

src/main/java/com/taskflow/dto/request/LoginRequest.java
  - email (@NotBlank, @Email)
  - password (@NotBlank)

src/main/java/com/taskflow/dto/response/AuthResponse.java
  - accessToken, refreshToken, fullName, email, role
  - @JsonInclude(NON_NULL)

src/main/java/com/taskflow/dto/response/ApiErrorResponse.java
  - status, error, message, timestamp, fieldErrors (Map)
```

**Security Layer:**
```
src/main/java/com/taskflow/security/jwt/JwtService.java
  - Reads jwt.secret, jwt.access-token-expiration, jwt.refresh-token-expiration from application.yml
  - generateAccessToken(UserDetails) → token with role claim, 15 min expiry
  - generateRefreshToken(UserDetails) → token, 7 day expiry
  - extractUsername(token) → email from subject
  - isTokenValid(token, userDetails) → checks username match + not expired
  - Uses HMAC-SHA256 signing with Base64-decoded secret key
  - Uses io.jsonwebtoken (jjwt) 0.12.6 library

src/main/java/com/taskflow/security/filter/JwtAuthenticationFilter.java
  - extends OncePerRequestFilter
  - Checks Authorization header for "Bearer " prefix
  - Extracts token → gets username → loads user → validates → sets SecurityContext
  - If no token or invalid → passes through (security config decides access)

src/main/java/com/taskflow/config/SecurityConfig.java
  - @EnableWebSecurity, @EnableMethodSecurity
  - Public endpoints: /api/auth/**, /api/health, /swagger-ui/**, /v3/api-docs/**, /h2-console/**
  - /api/admin/** → hasRole("ADMIN")
  - Everything else → authenticated()
  - SessionCreationPolicy.STATELESS (no HTTP sessions)
  - JwtAuthFilter added before UsernamePasswordAuthenticationFilter
  - BCryptPasswordEncoder bean
  - DaoAuthenticationProvider with our UserDetailsService
  - Frame options sameOrigin (for H2 console)
```

**Service Layer:**
```
src/main/java/com/taskflow/service/AuthService.java
  - register(RegisterRequest):
    1. Check duplicate email → throw DuplicateResourceException
    2. Build User with BCrypt-encoded password, role=USER
    3. Save to DB
    4. Generate access + refresh tokens
    5. Return AuthResponse
  - login(LoginRequest):
    1. authenticationManager.authenticate() → Spring Security verifies password
    2. Load user from DB
    3. Generate new tokens
    4. Return AuthResponse

src/main/java/com/taskflow/service/UserDetailsServiceImpl.java
  - Implements UserDetailsService
  - loadUserByUsername(email) → finds User by email or throws UsernameNotFoundException
```

**Controller Layer:**
```
src/main/java/com/taskflow/controller/AuthController.java
  - @RestController @RequestMapping("/api/auth")
  - POST /api/auth/register → 201 CREATED + AuthResponse
  - POST /api/auth/login → 200 OK + AuthResponse
  - @Valid on request bodies, Swagger @Tag and @Operation annotations

src/main/java/com/taskflow/controller/HealthController.java
  - GET /api/health → returns Map with status, app name, timestamp
```

**Exception Handling:**
```
src/main/java/com/taskflow/exception/GlobalExceptionHandler.java
  - @RestControllerAdvice
  - Handles: MethodArgumentNotValidException (validation errors with field-level details)
  - Handles: ResourceNotFoundException → 404
  - Handles: DuplicateResourceException → 409
  - Handles: BadCredentialsException → 401
  - Handles: Exception (generic) → 500

src/main/java/com/taskflow/exception/ResourceNotFoundException.java
  - extends RuntimeException

src/main/java/com/taskflow/exception/DuplicateResourceException.java
  - extends RuntimeException
```

**Configuration Files:**
```
src/main/resources/application.yml
  - spring.datasource: H2 in-memory (jdbc:h2:mem:taskflow)
  - spring.jpa.hibernate.ddl-auto: update
  - spring.h2.console.enabled: true
  - jwt.secret: Base64-encoded HMAC key
  - jwt.access-token-expiration: 900000 (15 min)
  - jwt.refresh-token-expiration: 604800000 (7 days)
  - springdoc.swagger-ui.path: /swagger-ui.html

pom.xml
  - Parent: spring-boot-starter-parent 3.5.0
  - Dependencies: web, data-jpa, security, validation, lombok, h2, postgresql
  - JWT: jjwt-api, jjwt-impl, jjwt-jackson (0.12.6)
  - Swagger: springdoc-openapi-starter-webmvc-ui (2.8.8)
  - Java version: 17

docker-compose.yml
  - PostgreSQL 16-alpine (port 5432, db=taskflow, user=taskflow, pass=taskflow123)
  - Redis 7-alpine (port 6379)
  - Health checks for both
  - Volume: postgres-data
```

**Learning Reference Guides (non-executable):**
```
src/main/java/com/taskflow/learn/microservices/MicroservicesGuide.java
src/main/java/com/taskflow/learn/kubernetes/KubernetesGuide.java
src/main/java/com/taskflow/learn/springai/SpringAiGuide.java
src/main/java/com/taskflow/learn/aws/AwsDeploymentGuide.java
src/main/java/com/taskflow/learn/monitoring/MonitoringGuide.java
```

**Beginner Documentation:**
```
docs/00_WHAT_ARE_ANNOTATIONS.md   — What annotations are, how Spring uses them
docs/01_ANNOTATIONS_CHEATSHEET.md — Quick reference for every annotation
docs/02_REQUEST_FLOW.md           — Step-by-step request trace through all files
docs/03_IMPORTS_EXPLAINED.md      — What import packages mean
README.md                         — START HERE section + reading order
```

**API Endpoints (Session 1):**
| Method | URL | Auth | Request Body | Response |
|--------|-----|------|-------------|----------|
| POST | /api/auth/register | Public | `{"fullName":"Raja","email":"raja@gmail.com","password":"secret123"}` | 201 + AuthResponse (tokens + user info) |
| POST | /api/auth/login | Public | `{"email":"raja@gmail.com","password":"secret123"}` | 200 + AuthResponse (tokens + user info) |
| GET | /api/health | Public | none | 200 + `{"status":"UP","application":"TaskFlow","timestamp":"..."}` |

---

## 5. Session 2: Projects & Tasks CRUD

### Status: NOT STARTED

### Goal
Build the core data model — Projects contain Tasks, Users own Projects, Tasks can be assigned. Full CRUD with JPA relationships.

### New Entities to Create

**Project Entity** (`src/main/java/com/taskflow/entity/Project.java`):
```
Fields:
  - id (Long, auto-generated)
  - name (String, required, max 200)
  - description (String, max 2000)
  - status (ProjectStatus enum: PLANNING, ACTIVE, ON_HOLD, COMPLETED, ARCHIVED)
  - priority (Priority enum: LOW, MEDIUM, HIGH, CRITICAL)
  - startDate (LocalDate)
  - targetEndDate (LocalDate)
  - actualEndDate (LocalDate, nullable)
  - owner (User, @ManyToOne — the user who created the project)
  - members (Set<User>, @ManyToMany — users who are part of the project)
  - tasks (List<Task>, @OneToMany mappedBy "project")
  - createdAt, updatedAt (@PrePersist, @PreUpdate)

Relationships:
  - Project → User (owner): @ManyToOne @JoinColumn(name = "owner_id")
  - Project → Users (members): @ManyToMany @JoinTable(name = "project_members")
  - Project → Tasks: @OneToMany(mappedBy = "project", cascade = CascadeType.ALL)
```

**Task Entity** (`src/main/java/com/taskflow/entity/Task.java`):
```
Fields:
  - id (Long, auto-generated)
  - title (String, required, max 300)
  - description (String, max 5000)
  - status (TaskStatus enum: TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED)
  - priority (Priority enum: LOW, MEDIUM, HIGH, CRITICAL)
  - dueDate (LocalDate)
  - estimatedHours (Integer)
  - actualHours (Integer)
  - project (Project, @ManyToOne)
  - assignee (User, @ManyToOne — who is working on it)
  - reporter (User, @ManyToOne — who created it)
  - createdAt, updatedAt

Relationships:
  - Task → Project: @ManyToOne @JoinColumn(name = "project_id")
  - Task → User (assignee): @ManyToOne @JoinColumn(name = "assignee_id")
  - Task → User (reporter): @ManyToOne @JoinColumn(name = "reporter_id")
```

**Enums to Create:**
```
src/main/java/com/taskflow/entity/ProjectStatus.java
  → PLANNING, ACTIVE, ON_HOLD, COMPLETED, ARCHIVED

src/main/java/com/taskflow/entity/TaskStatus.java
  → TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED

src/main/java/com/taskflow/entity/Priority.java
  → LOW, MEDIUM, HIGH, CRITICAL
```

### New DTOs

**Project DTOs:**
```
dto/request/CreateProjectRequest.java
  - name (@NotBlank), description, priority, startDate, targetEndDate

dto/request/UpdateProjectRequest.java
  - name, description, status, priority, targetEndDate (all optional for partial update)

dto/response/ProjectResponse.java
  - id, name, description, status, priority, startDate, targetEndDate, ownerName, memberCount, taskCount, createdAt
```

**Task DTOs:**
```
dto/request/CreateTaskRequest.java
  - title (@NotBlank), description, priority, dueDate, estimatedHours, assigneeId, projectId (@NotNull)

dto/request/UpdateTaskRequest.java
  - title, description, status, priority, dueDate, estimatedHours, assigneeId (all optional)

dto/response/TaskResponse.java
  - id, title, description, status, priority, dueDate, estimatedHours, actualHours, assigneeName, reporterName, projectName, createdAt, updatedAt
```

### New Repositories
```
repository/ProjectRepository.java
  - extends JpaRepository<Project, Long>
  - findByOwner(User owner) → List<Project>
  - findByMembersContaining(User member) → List<Project>
  - findByStatus(ProjectStatus status) → List<Project>

repository/TaskRepository.java
  - extends JpaRepository<Task, Long>
  - findByProject(Project project) → List<Task>
  - findByAssignee(User assignee) → List<Task>
  - findByProjectAndStatus(Project project, TaskStatus status) → List<Task>
  - countByProjectAndStatus(Project project, TaskStatus status) → Long
```

### New Services
```
service/ProjectService.java
  - createProject(CreateProjectRequest, User currentUser) → ProjectResponse
  - getProjectById(Long id) → ProjectResponse
  - getMyProjects(User currentUser) → List<ProjectResponse>
  - updateProject(Long id, UpdateProjectRequest, User currentUser) → ProjectResponse
  - deleteProject(Long id, User currentUser) → void
  - addMember(Long projectId, Long userId) → ProjectResponse
  - removeMember(Long projectId, Long userId) → ProjectResponse

service/TaskService.java
  - createTask(CreateTaskRequest, User currentUser) → TaskResponse
  - getTaskById(Long id) → TaskResponse
  - getTasksByProject(Long projectId) → List<TaskResponse>
  - getMyTasks(User currentUser) → List<TaskResponse>
  - updateTask(Long id, UpdateTaskRequest, User currentUser) → TaskResponse
  - deleteTask(Long id, User currentUser) → void
```

### New Controllers
```
controller/ProjectController.java
  - POST   /api/projects         → createProject
  - GET    /api/projects         → getMyProjects (current user's projects)
  - GET    /api/projects/{id}    → getProjectById
  - PUT    /api/projects/{id}    → updateProject
  - DELETE /api/projects/{id}    → deleteProject
  - POST   /api/projects/{id}/members/{userId}   → addMember
  - DELETE /api/projects/{id}/members/{userId}   → removeMember

controller/TaskController.java
  - POST   /api/tasks              → createTask
  - GET    /api/tasks              → getMyTasks (assigned to current user)
  - GET    /api/tasks/{id}         → getTaskById
  - GET    /api/projects/{id}/tasks → getTasksByProject
  - PUT    /api/tasks/{id}         → updateTask
  - DELETE /api/tasks/{id}         → deleteTask
```

### Key Concepts to Explain in Comments
- @ManyToOne, @OneToMany, @ManyToMany relationships
- Cascade types (CascadeType.ALL, MERGE, PERSIST)
- FetchType.LAZY vs EAGER (and why LAZY is default)
- @JoinColumn, @JoinTable — how join tables work
- DTO ↔ Entity mapping (manual or MapStruct)
- Getting current user from SecurityContextHolder
- @AuthenticationPrincipal annotation
- Why we return DTOs not entities (security, circular refs)
- Builder pattern for responses

### Database Tables Created
```
users (already exists from Session 1)
projects (new)
tasks (new)
project_members (join table for @ManyToMany)
```

---

## 6. Session 3: Workflow + Filtering + Pagination

### Status: NOT STARTED

### Goal
Add task state machine (valid transitions), dynamic filtering/search, and pagination to handle large datasets.

### Features to Build

**1. Task Status Transitions (State Machine):**
```
Valid transitions:
  TODO → IN_PROGRESS
  IN_PROGRESS → IN_REVIEW, TODO (move back)
  IN_REVIEW → DONE, IN_PROGRESS (needs changes)
  DONE → (terminal state, no transition)
  Any → CANCELLED

Invalid transitions throw InvalidStateTransitionException

Implementation:
  - Add canTransitionTo(TaskStatus target) method to TaskStatus enum
  - Service validates transition before updating
```

**2. Spring Data JPA Specifications (Dynamic Filtering):**
```
Create: specification/TaskSpecification.java
  - Filter by: status, priority, assigneeId, projectId, keyword search (title/description)
  - Uses Criteria API under the hood
  - Composable: can combine multiple filters

Create: specification/ProjectSpecification.java
  - Filter by: status, priority, ownerId, keyword
```

**3. Pagination & Sorting:**
```
Update controllers to accept:
  - ?page=0&size=20&sort=createdAt,desc
  - Return Page<TaskResponse> with metadata (totalElements, totalPages, currentPage)

Update DTOs:
  dto/response/PagedResponse.java
    - content (List<T>), page, size, totalElements, totalPages, isLast
```

**4. Search Endpoint:**
```
GET /api/tasks/search?status=IN_PROGRESS&priority=HIGH&assigneeId=1&keyword=login&page=0&size=20
```

### Key Concepts to Explain
- JPA Specifications (Criteria API)
- Pageable, Page, Sort
- Dynamic query building
- State machine pattern
- Query optimization basics

---

## 7. Session 4: AOP + Events + Scheduling

### Status: NOT STARTED

### Goal
Cross-cutting concerns: automatic audit logging, event-driven notifications, scheduled tasks.

### Features to Build

**1. AOP — Audit Logging:**
```
Create: aspect/AuditLogAspect.java
  - @Around annotation on all service methods
  - Logs: who called what, when, how long it took
  - Uses @Slf4j for logging

Create: entity/AuditLog.java
  - id, action, entityType, entityId, userId, timestamp, duration, details
  - Saved to database for audit trail

Create: annotation/Auditable.java
  - Custom annotation @Auditable to mark methods that should be audited
```

**2. Spring Events:**
```
Create: event/TaskCreatedEvent.java
Create: event/TaskAssignedEvent.java
Create: event/TaskStatusChangedEvent.java
Create: event/ProjectCreatedEvent.java

Create: listener/NotificationListener.java
  - @EventListener methods
  - Listens for task events
  - Creates in-app notifications (Notification entity)

Create: entity/Notification.java
  - id, userId, message, type, read, createdAt

Create: service/NotificationService.java
  - getNotifications(userId) → List<NotificationResponse>
  - markAsRead(notificationId) → void

Create: controller/NotificationController.java
  - GET /api/notifications → get my notifications
  - PUT /api/notifications/{id}/read → mark as read
```

**3. Scheduling:**
```
Create: scheduler/TaskReminderScheduler.java
  - @Scheduled(cron = "0 0 9 * * *") → daily at 9 AM
  - Finds tasks due tomorrow, creates reminder notifications

Create: scheduler/OverdueTaskScheduler.java
  - Finds overdue tasks, auto-updates status
  - Creates notifications for assignees

Add @EnableScheduling to main application class
```

**4. Spring Retry:**
```
Add spring-retry dependency to pom.xml
Create examples showing @Retryable on service methods
```

### Key Concepts to Explain
- AOP: Aspect, Pointcut, JoinPoint, Advice types (@Before, @After, @Around)
- Spring Events: ApplicationEventPublisher, @EventListener, @Async events
- @Scheduled: cron expressions, fixedRate, fixedDelay
- @Async: async method execution, thread pools
- Custom annotations: how to create and process them
- Observer pattern (events)

---

## 8. Session 5: Razorpay Payment Integration

### Status: NOT STARTED

### Goal
Accept payments via Razorpay (India's popular payment gateway). Build premium/subscription feature for projects.

### Features to Build

**1. Razorpay Setup:**
```
Add to pom.xml: com.razorpay:razorpay-java (latest version)

Add to application.yml:
  razorpay:
    key-id: ${RAZORPAY_KEY_ID}
    key-secret: ${RAZORPAY_KEY_SECRET}

Create: config/RazorpayConfig.java
  - @Bean RazorpayClient with key-id and key-secret
```

**2. Payment Entities:**
```
Create: entity/Payment.java
  - id, orderId (Razorpay order ID), paymentId (Razorpay payment ID)
  - amount, currency, status (CREATED, AUTHORIZED, CAPTURED, REFUNDED, FAILED)
  - user (@ManyToOne), plan (subscription plan), createdAt, updatedAt

Create: entity/SubscriptionPlan.java
  - id, name, description, price, durationDays, maxProjects, maxMembers
```

**3. Payment Flow:**
```
Create: service/PaymentService.java
  - createOrder(userId, planId) → creates Razorpay order, returns orderId + amount
  - verifyPayment(orderId, paymentId, signature) → verifies HMAC signature
  - handleWebhook(payload, signature) → processes Razorpay webhook events
  - getPaymentHistory(userId) → List<PaymentResponse>

Create: controller/PaymentController.java
  - POST /api/payments/create-order → creates Razorpay order
  - POST /api/payments/verify → verifies payment after checkout
  - POST /api/webhooks/razorpay → webhook endpoint (Razorpay calls this)
  - GET  /api/payments/history → payment history

Create: DTOs:
  - CreateOrderRequest (planId)
  - CreateOrderResponse (orderId, amount, currency, razorpayKeyId)
  - VerifyPaymentRequest (orderId, paymentId, signature)
  - PaymentResponse (id, amount, status, planName, createdAt)
```

**4. Payment Verification (Security):**
```
Razorpay signature verification:
  generated_signature = HMAC_SHA256(orderId + "|" + paymentId, key_secret)
  if (generated_signature == razorpay_signature) → payment is genuine
```

### Key Concepts to Explain
- Payment gateway flow (order → checkout → verify)
- HMAC signature verification (why it's needed)
- Webhooks (server-to-server callbacks)
- Idempotency (preventing duplicate charges)
- Payment status state machine
- Environment variables for secrets (never hardcode API keys)

---

## 9. Session 6: Redis Caching + Performance

### Status: NOT STARTED

### Goal
Add Redis caching, fix N+1 queries, optimize database access.

### Features to Build

**1. Redis Caching:**
```
Add to pom.xml: spring-boot-starter-data-redis, spring-boot-starter-cache

Create: config/RedisConfig.java
  - @EnableCaching
  - RedisCacheManager with TTL configuration
  - JSON serialization for cache values

Add caching annotations to services:
  - @Cacheable("projects") on getProjectById → cache result
  - @CacheEvict("projects") on updateProject → clear cache
  - @CachePut("projects") on createProject → update cache
  - Cache user's task list, notification count
```

**2. N+1 Query Problem Fix:**
```
Demonstrate the problem:
  - Loading projects → each project lazy-loads owner → N+1 queries
  
Fix with:
  - @EntityGraph on repository methods
  - JOIN FETCH in JPQL queries
  - @BatchSize for batch loading
  - Projections (interface-based) for read-only queries
```

**3. Connection Pooling:**
```
Add to application.yml:
  spring.datasource.hikari:
    maximum-pool-size: 10
    minimum-idle: 5
    connection-timeout: 30000
    idle-timeout: 600000

Explain HikariCP configuration
```

**4. Database Indexing:**
```
Add @Index annotations on frequently queried columns:
  - User.email (already unique)
  - Task.status, Task.assignee, Task.dueDate
  - Project.owner, Project.status

Create: learn/performance/PerformanceGuide.java
  - Explain B-Tree indexes, when to index, EXPLAIN ANALYZE
```

### Key Concepts to Explain
- Redis: what it is, data structures, TTL, eviction policies
- @Cacheable, @CacheEvict, @CachePut
- N+1 problem: what causes it, how to detect, how to fix
- @EntityGraph, JOIN FETCH, @BatchSize
- Connection pooling: why, HikariCP
- Database indexes: how they work, when to use

---

## 10. Session 7: Apache Kafka + Event-Driven

### Status: NOT STARTED

### Goal
Add Kafka for event streaming. Decouple services through events.

### Features to Build

**1. Kafka Setup:**
```
Add to docker-compose.yml:
  - Zookeeper container
  - Kafka broker container
  - Kafka UI (optional, for debugging)

Add to pom.xml: spring-kafka

Add to application.yml:
  spring.kafka:
    bootstrap-servers: localhost:9092
    consumer.group-id: taskflow-group
    consumer.auto-offset-reset: earliest
```

**2. Kafka Producers:**
```
Create: kafka/producer/TaskEventProducer.java
  - publishTaskCreated(TaskEvent)
  - publishTaskStatusChanged(TaskStatusEvent)
  - publishTaskAssigned(TaskAssignEvent)
  
Create: kafka/event/TaskEvent.java (serializable event payload)
Create: kafka/event/TaskStatusEvent.java
Create: kafka/event/TaskAssignEvent.java
```

**3. Kafka Consumers:**
```
Create: kafka/consumer/NotificationConsumer.java
  - @KafkaListener(topics = "task-events")
  - Processes events → creates notifications

Create: kafka/consumer/AuditConsumer.java
  - @KafkaListener(topics = "audit-events")
  - Processes events → saves audit logs

Create: kafka/consumer/AnalyticsConsumer.java
  - Processes events → updates project statistics
```

**4. Topics:**
```
task-events: task created, updated, deleted, status changed
user-events: user registered, profile updated
payment-events: payment created, completed, failed
notification-events: notification triggers
```

**5. Dead Letter Queue:**
```
Configure DLQ for failed message processing
Create: kafka/config/KafkaConfig.java
  - Error handler with retry + DLQ
```

### Key Concepts to Explain
- What Kafka is vs RabbitMQ vs SQS
- Topics, Partitions, Consumer Groups, Offsets
- At-least-once vs at-most-once vs exactly-once delivery
- Serialization (JSON with Jackson)
- Dead Letter Queue pattern
- Event sourcing basics

---

## 11. Session 8: React Frontend — Auth + Setup

### Status: NOT STARTED

### Goal
Set up React project from scratch with TypeScript, build authentication pages.

### Project Setup
```bash
# Create React project in /frontend directory
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install react-router-dom axios tailwindcss @headlessui/react
npm install @tanstack/react-query zustand react-hook-form
npm install @heroicons/react recharts
npx tailwindcss init -p
```

### File Structure to Create
```
frontend/
  src/
    main.tsx                    # Entry point
    App.tsx                     # Router setup
    
    pages/
      LoginPage.tsx             # Login form
      RegisterPage.tsx          # Registration form
      DashboardPage.tsx         # Main dashboard (placeholder for Session 9)
      NotFoundPage.tsx          # 404 page
    
    components/
      layout/
        Navbar.tsx              # Top navigation bar
        Sidebar.tsx             # Side navigation
        Layout.tsx              # Main layout wrapper
      auth/
        LoginForm.tsx           # Login form component
        RegisterForm.tsx        # Register form component
        ProtectedRoute.tsx      # Route guard (redirect if not logged in)
      common/
        Button.tsx              # Reusable button
        Input.tsx               # Reusable input field
        Loading.tsx             # Loading spinner
        Toast.tsx               # Notification toast
    
    services/
      api.ts                    # Axios instance with base URL + interceptors
      authService.ts            # login(), register(), logout(), refreshToken()
    
    stores/
      authStore.ts              # Zustand store: user, token, isAuthenticated
    
    types/
      auth.ts                   # TypeScript interfaces: LoginRequest, RegisterRequest, AuthResponse
      api.ts                    # ApiError interface
    
    hooks/
      useAuth.ts                # Custom hook for auth operations
    
    utils/
      tokenUtils.ts             # Save/get/remove token from localStorage
    
    styles/
      index.css                 # Tailwind imports + global styles
```

### Key Features
- Login page with email/password form
- Register page with name/email/password form
- JWT token storage in localStorage
- Axios interceptor: auto-attach token to every API request
- Axios interceptor: auto-refresh token on 401 response
- Protected routes: redirect to /login if not authenticated
- Toast notifications for success/error messages
- Responsive design (mobile + desktop)

### Key Concepts to Explain (detailed comments in every file)
- What React IS (component-based UI library)
- JSX: HTML inside JavaScript
- Components: function that returns JSX
- Props: passing data to components
- useState: local state in a component
- useEffect: side effects (API calls on page load)
- TypeScript: why types matter, interfaces
- React Router: how SPA routing works
- Axios: HTTP client, interceptors
- JWT flow on frontend: login → store token → send with requests → refresh on expiry
- Zustand: simple global state (simpler than Redux)
- Tailwind CSS: utility-first classes

---

## 12. Session 9: React Frontend — Dashboard + Tasks

### Status: NOT STARTED

### Goal
Build the main application UI — dashboard, project management, task management with Kanban board.

### Pages to Build
```
pages/
  DashboardPage.tsx         # Stats cards, recent activity, charts
  ProjectsPage.tsx          # List all projects, create new
  ProjectDetailPage.tsx     # Single project view, members, tasks
  TasksPage.tsx             # All tasks view (table + filters)
  KanbanPage.tsx            # Drag-and-drop Kanban board
  ProfilePage.tsx           # User profile, settings
  NotificationsPage.tsx     # All notifications
  PaymentPage.tsx           # Subscription plans, pay with Razorpay
```

### Components to Build
```
components/
  dashboard/
    StatsCard.tsx           # Shows count (total projects, tasks, etc.)
    RecentActivity.tsx      # Activity feed
    ProjectChart.tsx        # Pie/bar chart using Recharts
    
  projects/
    ProjectCard.tsx         # Project summary card
    ProjectForm.tsx         # Create/edit project form
    ProjectMemberList.tsx   # Member management
    
  tasks/
    TaskCard.tsx            # Task summary card
    TaskForm.tsx            # Create/edit task form (modal)
    TaskTable.tsx           # Table view with sorting/filtering
    TaskFilters.tsx         # Filter bar (status, priority, assignee)
    KanbanBoard.tsx         # Drag-and-drop columns
    KanbanColumn.tsx        # Single column (TODO, IN_PROGRESS, etc.)
    
  payments/
    PlanCard.tsx            # Subscription plan card
    RazorpayCheckout.tsx    # Razorpay payment button
```

### Key Features
- Dashboard with project/task stats (Recharts)
- CRUD for projects (create, view, edit, delete)
- CRUD for tasks with modal forms
- Kanban board with drag-and-drop (react-beautiful-dnd or dnd-kit)
- Table view with pagination, sorting, filtering
- Real-time notification badge
- Razorpay checkout integration (frontend)
- Dark mode toggle
- Responsive design

### Key Concepts to Explain
- React Query (TanStack): caching, refetching, optimistic updates
- React Hook Form: controlled forms, validation
- Component composition: small reusable pieces
- Custom hooks: extract reusable logic
- Recharts: data visualization
- Drag and drop: HTML5 drag events
- Razorpay frontend SDK: loading script, opening checkout

---

## 13. Session 10: Docker + Testing + Deployment

### Status: NOT STARTED

### Goal
Containerize everything, write tests, set up CI/CD, final polish.

### Docker Setup
```
Dockerfile (backend — multi-stage build):
  Stage 1: Build with Maven
  Stage 2: Run with JRE only (smaller image)

frontend/Dockerfile:
  Stage 1: Build with Node
  Stage 2: Serve with Nginx

docker-compose.yml (updated, full stack):
  - taskflow-backend (Spring Boot)
  - taskflow-frontend (React + Nginx)
  - postgres (database)
  - redis (cache)
  - kafka + zookeeper (messaging)
  - All connected via Docker network
```

### Testing
```
Backend Tests:
  test/java/com/taskflow/
    service/AuthServiceTest.java        # Unit test with Mockito
    service/ProjectServiceTest.java     # Unit test
    service/TaskServiceTest.java        # Unit test
    controller/AuthControllerTest.java  # @WebMvcTest
    controller/ProjectControllerTest.java
    repository/UserRepositoryTest.java  # @DataJpaTest
    security/JwtServiceTest.java        # Unit test
    integration/AuthIntegrationTest.java # @SpringBootTest (full flow)

What to test:
  - Register: success, duplicate email, invalid input
  - Login: success, wrong password, non-existent user
  - CRUD: create, read, update, delete for projects and tasks
  - Security: unauthenticated access blocked, role-based access
  - JWT: token generation, validation, expiry
```

### CI/CD (GitHub Actions)
```
.github/workflows/ci.yml:
  - Trigger: push to main, pull requests
  - Steps:
    1. Checkout code
    2. Set up Java 17
    3. Run: mvn test
    4. Run: mvn verify (integration tests)
    5. Build Docker image
    6. Push to Docker Hub (optional)
  
  - Frontend:
    1. Set up Node 18
    2. npm install && npm run build
    3. npm test
```

### Postman Collection
```
Create: postman/TaskFlow.postman_collection.json
  - Organized by feature: Auth, Projects, Tasks, Payments
  - Auto-set JWT token from login response
  - Example request/response for every endpoint
```

### Final Polish
- README update with full API docs
- Swagger annotations on all endpoints
- Error messages cleanup
- Code comment review (ensure every file has WHY + interview Q&A)
- Application profiles: dev, test, prod
- Flyway migration scripts (from Hibernate ddl-auto to proper migrations)

### Key Concepts to Explain
- Docker: images, containers, Dockerfile, layers, caching
- Docker Compose: multi-container orchestration
- Multi-stage builds: why (smaller production images)
- JUnit 5: @Test, @BeforeEach, assertions
- Mockito: @Mock, @InjectMocks, when().thenReturn()
- @WebMvcTest: test controller without full app
- @DataJpaTest: test repository with embedded DB
- @SpringBootTest: full integration test
- GitHub Actions: CI/CD workflow syntax
- Code coverage: JaCoCo

---

## 14. Learning Folders (Reference Guides)

These are **non-executable reference files** already created in Session 1. They explain advanced topics that Raja can read now and implement later:

| File | Topics Covered |
|------|---------------|
| `learn/microservices/MicroservicesGuide.java` | API Gateway, Eureka, OpenFeign, Circuit Breaker, Config Server, Distributed Tracing, when to split monolith |
| `learn/kubernetes/KubernetesGuide.java` | Pod, Deployment, Service, ConfigMap, Secret, Ingress, HPA, kubectl commands, deployment strategies |
| `learn/springai/SpringAiGuide.java` | ChatClient, Prompt Templates, RAG, Embeddings, Function Calling, LLM integration |
| `learn/aws/AwsDeploymentGuide.java` | EC2, RDS, S3, ECS, ElastiCache, Route 53, ALB, deployment options, cost optimization |
| `learn/monitoring/MonitoringGuide.java` | Prometheus, Grafana, ELK Stack, Loki, Actuator, custom metrics, logging best practices |

**Future learning folders to add** (can be added in any session):
- `learn/performance/PerformanceGuide.java` — N+1, indexing, query optimization, connection pooling
- `learn/testing/TestingGuide.java` — Testing pyramid, TDD, BDD, test strategies
- `learn/designpatterns/DesignPatternsGuide.java` — All patterns used in project

---

## 15. Coding Conventions

Follow these conventions across ALL sessions:

### File Structure
```
Every Java file follows this pattern:
1. Package declaration
2. Imports (grouped by library)
3. Class-level comment block:
   - WHAT this class does
   - WHY it exists
   - HOW it works
   - Real-world analogy
4. Class declaration with annotations
5. Fields with inline comments
6. Methods with comments explaining:
   - What the method does
   - Why this approach
   - What could go wrong
7. Bottom of file: Interview Q&A block
```

### Comment Style
```java
/**
 * WHAT: AuthService handles user registration and login
 * WHY:  Separates business logic from controller (SRP — Single Responsibility)
 * HOW:  Register: validate → hash password → save → generate tokens
 *       Login: authenticate → generate tokens
 *
 * INTERVIEW Q: Why not put this logic directly in the controller?
 * A: Controllers should only handle HTTP concerns (request/response).
 *    Business logic in services makes it testable, reusable, and maintainable.
 */
```

### Naming Conventions
- Entities: `User`, `Project`, `Task` (singular, PascalCase)
- Repositories: `UserRepository`, `ProjectRepository` (Entity + Repository)
- Services: `AuthService`, `ProjectService` (feature + Service)
- Controllers: `AuthController`, `ProjectController` (feature + Controller)
- DTOs: `CreateProjectRequest`, `ProjectResponse` (Action + Entity + Request/Response)
- Exceptions: `ResourceNotFoundException`, `DuplicateResourceException`

### Git Branch Naming
```
devin/session-{N}-{feature-name}
Example: devin/session-2-projects-tasks-crud
```

### Git Commit Format
```
Session {N}: {brief description}

- Detail 1
- Detail 2
```

---

## 16. How to Give This to Another AI

If you want to continue this project with ChatGPT, Claude, Cursor, or any other AI, give them this:

### Step 1: Share this document
Copy this entire `PROJECT_MASTER_PLAN.md` and paste it at the start of your conversation.

### Step 2: Tell them what session to work on
```
"I'm building the TaskFlow project. Session 1 is complete (see master plan).
Please build Session {N}: {session name}.

Repository: https://github.com/Raja7380/taskflow.git
Branch from: main
Create branch: devin/session-{N}-{feature-name}
Commit as: Raja <rajasingh12587@gmail.com>

Follow the exact specifications in the master plan for Session {N}.
Every file must have detailed WHY/HOW comments + interview Q&A at the bottom."
```

### Step 3: Remind them of coding conventions
```
"Follow these rules:
1. Every file has WHAT/WHY/HOW comments at the top
2. Every file has Interview Q&A at the bottom
3. Use Lombok (@Data, @Builder, @RequiredArgsConstructor)
4. DTOs separate from entities
5. Global exception handler catches all errors
6. Swagger annotations on controllers
7. Service layer pattern: Controller → Service → Repository
8. All code must compile: ./mvnw compile
9. Commit under name 'Raja' email 'rajasingh12587@gmail.com'"
```

### Step 4: After each session, update this master plan
Mark completed sessions with ✓ and add any new files created.

---

## Resume Line (After All Sessions Complete)

```
TaskFlow — Full-Stack Project Management Platform
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• Built microservices-ready project management system using Spring Boot 3, 
  Spring Security 6 (JWT + OAuth2), Spring Data JPA, PostgreSQL, Redis, Apache Kafka
• Integrated Razorpay payment gateway with order creation, signature verification, 
  and webhook processing
• Implemented event-driven architecture with Kafka for real-time notifications 
  and comprehensive audit logging via AOP
• Built React 18 / TypeScript frontend with responsive dashboard, Kanban board, 
  real-time updates, and Razorpay checkout
• Containerized with Docker + Docker Compose, CI/CD with GitHub Actions
• Achieved 85%+ test coverage using JUnit 5, Mockito, and Spring Boot Test

Tech: Java 17, Spring Boot 3, Spring Security, JPA/Hibernate, PostgreSQL, 
     Redis, Kafka, React 18, TypeScript, Tailwind CSS, Docker, GitHub Actions
```

---

*This document is the single source of truth for the TaskFlow project. Keep it updated after each session.*
