
# TaskFlow — Complete Project Master Plan

> **PURPOSE**: This is the single source of truth for the TaskFlow project.
> Contains everything done AND everything to build next — backend AND frontend.
>
> **PORTABLE**: Give this to ANY AI and they will know exactly where to pick up.
>
> **Last Updated**: PostgreSQL migration + full frontend + add-on sessions added (June 2026)

---

## TABLE OF CONTENTS

1. [Project Overview](#1-project-overview)
2. [Complete Tech Stack](#2-complete-tech-stack)
3. [Repo Info & Setup](#3-repo-info--setup)
4. [PostgreSQL Setup Guide](#4-postgresql-setup-guide)
5. [Frontend Technologies Explained](#5-frontend-technologies-explained)
6. [What's DONE — Session 1](#6-whats-done--session-1)
7. [Session 2: Projects & Tasks CRUD](#7-session-2-projects--tasks-crud)
8. [Session 3: Workflow + Filtering + Pagination](#8-session-3-workflow--filtering--pagination)
9. [Session 4: AOP + Events + Scheduling](#9-session-4-aop--events--scheduling)
10. [Session 5: Razorpay Payment Integration](#10-session-5-razorpay-payment-integration)
11. [Session 6: Redis Caching + Performance](#11-session-6-redis-caching--performance)
12. [Session 7: Apache Kafka + Event-Driven](#12-session-7-apache-kafka--event-driven)
13. [Session 8: React Frontend — Auth + Setup](#13-session-8-react-frontend--auth--setup)
14. [Session 9: React Frontend — Dashboard + Tasks](#14-session-9-react-frontend--dashboard--tasks)
15. [Session 10: Docker + Testing + Deployment](#15-session-10-docker--testing--deployment)
16. [Session 11: Spring AI Integration](#16-session-11-spring-ai-integration)
17. [Session 12: WebSockets — Real-Time Features](#17-session-12-websockets--real-time-features)
18. [Session 13: OAuth2 Social Login](#18-session-13-oauth2-social-login)
19. [Session 14: Rate Limiting + Email + File Upload](#19-session-14-rate-limiting--email--file-upload)
20. [Session 15: Observability — Prometheus + Grafana](#20-session-15-observability--prometheus--grafana)
21. [Session 16: Frontend Add-ons](#21-session-16-frontend-add-ons)
22. [Session 17: Elasticsearch](#22-session-17-elasticsearch)
23. [Learning Folders](#23-learning-folders)
24. [Coding Conventions](#24-coding-conventions)
25. [Complete Week-by-Week Learning Roadmap](#25-complete-week-by-week-learning-roadmap)
26. [Resume Lines — Final Version](#26-resume-lines--final-version)
27. [How to Give This to Another AI](#27-how-to-give-this-to-another-ai)

---

## 1. Project Overview

**TaskFlow** is a full-stack AI-powered Project Management System built to:
- Demonstrate enterprise Java + React skills for SDE1 interviews (15+ LPA target)
- Cover every technology companies hiring freshers in 2026 actually use
- Serve as both a working application AND a learning resource

**Target User**: Raja — 2026 BTech graduate, learning Java + Spring + React from scratch.
**Goal**: Land a 15+ LPA SDE1 job in 2026.
**Database**: PostgreSQL (switched from H2 in June 2026)

---

## 2. Complete Tech Stack

### Backend (Java/Spring)
| Technology | Version | Purpose |
|---|---|---|
| Java | 17 | Language |
| Spring Boot | 3.5.0 | Application framework + auto-configuration |
| Spring Security 6 | via Boot | Authentication & authorization |
| Spring Data JPA | via Boot | Database access (ORM) |
| Hibernate | via JPA | Object-relational mapping implementation |
| JWT (jjwt) | 0.12.6 | Stateless authentication tokens |
| Lombok | via Boot | Eliminate boilerplate code |
| Bean Validation | via Boot | Input validation (@NotBlank, @Email) |
| MapStruct | Session 2 | DTO ↔ Entity mapping |
| Spring AOP | via Boot | Cross-cutting concerns (logging, audit) |
| Spring Events | via Boot | Event-driven architecture |
| Spring Scheduler | via Boot | Cron jobs, @Scheduled tasks |
| Spring Cache + Redis | Session 6 | Caching layer |
| Spring Retry | Session 4 | Retry failed operations |
| Spring AI | Session 11 | AI integration (Claude/GPT) |
| Spring WebSocket | Session 12 | Real-time bidirectional communication |
| Spring Security OAuth2 | Session 13 | Social login (Google) |
| Bucket4j | Session 14 | Rate limiting |
| JavaMailSender | Session 14 | Email notifications |
| Springdoc OpenAPI | 2.8.8 | Swagger/API documentation |
| Spring Actuator | Session 15 | Health + metrics endpoints |

### Database & Storage
| Technology | Purpose |
|---|---|
| PostgreSQL 16 | Primary database (dev + production) |
| H2 | Tests only (fast, no Docker needed) |
| Flyway | Database migrations (Session 10) |
| Redis 7 | Caching + rate limiting |
| AWS S3 | File attachments (Session 14) |
| Elasticsearch | Full-text search (Session 17) |

### Messaging
| Technology | Purpose |
|---|---|
| Apache Kafka | Event streaming (Session 7) |

### Payments
| Technology | Purpose |
|---|---|
| Razorpay Java SDK | Payment integration (Session 5) |

### Frontend
| Technology | Purpose |
|---|---|
| React 18 | UI component library |
| TypeScript | Type-safe JavaScript |
| Vite | Build tool (converts TypeScript/JSX → browser JS) |
| Tailwind CSS | Utility-first styling |
| React Router | Client-side routing (SPA navigation) |
| Axios | HTTP client (calls Spring Boot API) |
| Zustand | Global state management (user, token) |
| React Query (TanStack) | Server state — caching, background refetch |
| React Hook Form | Form handling + validation |
| Recharts | Charts and data visualizations |
| dnd-kit | Drag-and-drop (Kanban board) |
| Framer Motion | Animations and transitions (Session 16) |
| React Testing Library | Frontend component tests (Session 16) |

### DevOps & Observability
| Technology | Purpose |
|---|---|
| Docker | Containerization |
| Docker Compose | Multi-container stack |
| GitHub Actions | CI/CD pipeline |
| Prometheus | Metrics collection |
| Grafana | Metrics visualization / dashboards |

### Testing
| Technology | Purpose |
|---|---|
| JUnit 5 | Unit testing |
| Mockito | Mocking dependencies |
| Spring Boot Test | Integration testing |
| JaCoCo | Code coverage reports |

---

## 3. Repo Info & Setup

**GitHub URL**: https://github.com/Raja7380/taskflow.git
**Default Branch**: main

**Git Commit Convention**:
- Name: `Raja`
- Email: `rajasingh12587@gmail.com`

**How to Run (Development with PostgreSQL)**:
```bash
# Step 1: Start PostgreSQL and Adminer (database viewer)
docker-compose up -d postgres adminer

# Step 2: Run Spring Boot app
./mvnw spring-boot:run

# App:     http://localhost:8080
# Swagger: http://localhost:8080/swagger-ui.html
# DB GUI:  http://localhost:8090  (login: PostgreSQL / postgres / taskflow / taskflow123 / taskflow)
```

**How to Run (Full Stack with Docker)**:
```bash
docker-compose up -d
./mvnw spring-boot:run
```

**How to Run Tests** (uses H2 automatically, no Docker needed):
```bash
./mvnw test
```

---

## 4. PostgreSQL Setup Guide

### What is PostgreSQL?

PostgreSQL is a production-grade relational database. It runs as a separate program on your computer (or server), stores data in files on disk, and survives restarts.

```
H2 (old):                          PostgreSQL (new):
─────────────────────              ──────────────────────────────
Lives inside your Java app         Runs as its own separate process
Stored in RAM — lost on restart    Stored on disk — survives forever
No installation needed             Needs to be running before app starts
Fine for learning/tests            Used in real production systems
```

### Option A — Docker (Recommended, Already Configured)

Docker is the easiest way — no PostgreSQL installation needed. Your `docker-compose.yml` already has everything.

**Step 1: Install Docker Desktop**
Download from: https://www.docker.com/products/docker-desktop/
Install it and start it (you'll see the Docker whale icon in your taskbar).

**Step 2: Start PostgreSQL**
```bash
# In your project folder:
docker-compose up -d postgres adminer
```

This downloads the PostgreSQL image once (~80MB) and starts it in a container.

**Step 3: Verify it's running**
```bash
docker-compose ps
# Should show: taskflow-postgres   running
# Should show: taskflow-adminer    running
```

**Step 4: Open Adminer (database viewer)**
Go to: http://localhost:8090

Fill in the login form:
```
System:   PostgreSQL
Server:   postgres
Username: taskflow
Password: taskflow123
Database: taskflow
```

You'll see your database. After running the app, you'll see the `users` table appear here.

### Option B — Direct Install (Without Docker)

**Step 1**: Download PostgreSQL 16 from https://www.postgresql.org/download/windows/

**Step 2**: Install with default settings. Remember the password you set for the `postgres` superuser.

**Step 3**: Open pgAdmin (installs with PostgreSQL), create a database:
```sql
CREATE DATABASE taskflow;
CREATE USER taskflow WITH PASSWORD 'taskflow123';
GRANT ALL PRIVILEGES ON DATABASE taskflow TO taskflow;
```

**Step 4**: `application.yml` is already configured — no changes needed.

### How PostgreSQL Stores Your Data

```
PostgreSQL Server (running on port 5432)
└── Database: taskflow
    └── Schema: public
        ├── Table: users
        │   ├── id (BIGSERIAL PRIMARY KEY)
        │   ├── full_name (VARCHAR 100)
        │   ├── email (VARCHAR 150 UNIQUE)
        │   ├── password (VARCHAR)
        │   ├── role (VARCHAR)
        │   ├── created_at (TIMESTAMP)
        │   └── updated_at (TIMESTAMP)
        ├── Table: projects  (Session 2)
        ├── Table: tasks     (Session 2)
        └── Table: project_members (Session 2)
```

Hibernate reads your `@Entity` classes and creates these tables automatically (`ddl-auto: update`).

### Common PostgreSQL Commands (run in Adminer SQL tab)
```sql
-- See all tables
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';

-- See all users
SELECT id, full_name, email, role, created_at FROM users;

-- See a user's password hash (should never be plain text!)
SELECT email, password FROM users;

-- Delete all data (careful!)
DELETE FROM users;

-- Count rows
SELECT COUNT(*) FROM users;
```

---

## 5. Frontend Technologies Explained

### What is the Frontend?

The frontend is everything the user sees and interacts with in the browser. It's a separate application from your Spring Boot backend.

```
BACKEND (Spring Boot)              FRONTEND (React)
─────────────────────              ─────────────────
Runs on your SERVER                Runs in USER'S BROWSER
Handles data, logic, auth          Handles UI, user interactions
Returns JSON responses             Shows JSON as visual components
Port 8080                          Port 5173 (dev) or 80 (production)
```

They communicate over HTTP — React calls your Spring Boot API.

### HTML — Structure
Every webpage is made of HTML elements. React generates HTML.
```html
<div>
  <h1>TaskFlow</h1>
  <button>Create Task</button>
</div>
```

### CSS — Appearance
Makes HTML look good — colors, sizes, layouts.
```css
button { background: blue; color: white; padding: 10px; }
```

### JavaScript — Behavior
Makes pages interactive — responds to clicks, fetches data.
```javascript
button.onclick = () => { fetch('/api/tasks') }
```

### TypeScript — JavaScript with Types
Catches bugs before they reach users.
```typescript
// Without TypeScript — crashes at runtime:
function greet(name) { return name.toUpperCase() }
greet(123)  // 123 has no toUpperCase — runtime crash

// With TypeScript — caught before running:
function greet(name: string) { return name.toUpperCase() }
greet(123)  // ERROR: number is not assignable to string
```

### React — Build UIs from Components
Components are reusable UI pieces. Small components combine into large UIs.
```tsx
// A reusable TaskCard component
function TaskCard({ title, status }: { title: string, status: string }) {
  return (
    <div className="card">
      <h3>{title}</h3>
      <span>{status}</span>
    </div>
  )
}

// Use it for every task:
tasks.map(task => <TaskCard title={task.title} status={task.status} />)
```

### Vite — Build Tool
Converts TypeScript + JSX → plain JavaScript browsers can run. Also runs a dev server with hot reload (changes appear instantly without page refresh).

### Tailwind CSS — Styling Without CSS Files
```tsx
// Style directly with class names — no separate CSS file needed
<button className="bg-blue-500 text-white px-4 py-2 rounded-lg hover:bg-blue-600">
  Create Task
</button>
```

### Axios — HTTP Client
How React calls your Spring Boot API.
```typescript
// Login
const res = await axios.post('/api/auth/login', { email, password })
const token = res.data.accessToken

// Authenticated request
const tasks = await axios.get('/api/tasks', {
  headers: { Authorization: `Bearer ${token}` }
})
```

### React Router — Navigation Without Page Reload
```tsx
<Routes>
  <Route path="/login"        element={<LoginPage />} />
  <Route path="/dashboard"    element={<DashboardPage />} />
  <Route path="/projects/:id" element={<ProjectDetailPage />} />
</Routes>
```

### React Query — Smart Data Fetching
```typescript
// Handles loading state, error state, caching, background refresh
const { data: tasks, isLoading, error } = useQuery({
  queryKey: ['tasks'],
  queryFn: () => axios.get('/api/tasks').then(r => r.data)
})
```

### Zustand — Global State
```typescript
// Stores logged-in user data accessible anywhere in the app
const useAuthStore = create((set) => ({
  user: null,
  token: null,
  login: (user, token) => set({ user, token }),
  logout: () => set({ user: null, token: null })
}))
```

### React Hook Form — Forms
```tsx
const { register, handleSubmit, formState: { errors } } = useForm()
<input {...register('email', { required: 'Email required' })} />
{errors.email && <p>{errors.email.message}</p>}
```

---

## 6. What's DONE — Session 1

### Status: COMPLETE ✓

### Files Created:
```
Entry Point:
  TaskFlowApplication.java          — @SpringBootApplication, main()

Entity:
  entity/Role.java                  — Enum: USER, MANAGER, ADMIN
  entity/User.java                  — @Entity mapping to "users" table, implements UserDetails

Repository:
  repository/UserRepository.java    — JpaRepository<User,Long>, findByEmail, existsByEmail

DTOs:
  dto/request/RegisterRequest.java  — fullName, email, password with @NotBlank/@Email/@Size
  dto/request/LoginRequest.java     — email, password
  dto/response/AuthResponse.java    — accessToken, refreshToken, fullName, email, role
  dto/response/ApiErrorResponse.java — status, error, message, timestamp, fieldErrors

Security:
  security/jwt/JwtService.java      — generateAccessToken, generateRefreshToken, isTokenValid
  security/filter/JwtAuthenticationFilter.java — reads JWT from every request
  config/SecurityConfig.java        — filter chain, public vs protected endpoints, BCrypt

Service:
  service/AuthService.java          — register(), login()
  service/UserDetailsServiceImpl.java — loadUserByUsername()

Controller:
  controller/AuthController.java    — POST /api/auth/register, POST /api/auth/login
  controller/HealthController.java  — GET /api/health

Exception:
  exception/GlobalExceptionHandler.java    — @RestControllerAdvice, handles all errors
  exception/ResourceNotFoundException.java — 404 exception
  exception/DuplicateResourceException.java — 409 exception

Config:
  resources/application.yml         — PostgreSQL, JWT, server, Swagger config
  resources/application-test.yml    — H2 for tests
  docker-compose.yml                — PostgreSQL, Adminer, Redis

Learning Guides:
  docs/00_WHAT_ARE_ANNOTATIONS.md
  docs/01_ANNOTATIONS_CHEATSHEET.md
  docs/02_REQUEST_FLOW.md
  docs/03_IMPORTS_EXPLAINED.md
```

### Endpoints:
| Method | URL | Auth | Request | Response |
|---|---|---|---|---|
| POST | /api/auth/register | Public | `{fullName, email, password}` | 201 + tokens |
| POST | /api/auth/login | Public | `{email, password}` | 200 + tokens |
| GET | /api/health | Public | none | `{status, application, timestamp}` |

---

## 7. Session 2: Projects & Tasks CRUD

### Status: NOT STARTED

### Goal
Core data model — Projects contain Tasks, Users own Projects. Full CRUD with JPA relationships.

### New Enums
```
entity/ProjectStatus.java  → PLANNING, ACTIVE, ON_HOLD, COMPLETED, ARCHIVED
entity/TaskStatus.java     → TODO, IN_PROGRESS, IN_REVIEW, DONE, CANCELLED
entity/Priority.java       → LOW, MEDIUM, HIGH, CRITICAL
```

### New Entity: Project
```
entity/Project.java
  Fields: id, name, description, status, priority, startDate, targetEndDate,
          actualEndDate, owner (User @ManyToOne), members (Set<User> @ManyToMany),
          tasks (List<Task> @OneToMany), createdAt, updatedAt
```

### New Entity: Task
```
entity/Task.java
  Fields: id, title, description, status, priority, dueDate, estimatedHours,
          actualHours, project (Project @ManyToOne), assignee (User @ManyToOne),
          reporter (User @ManyToOne), createdAt, updatedAt
```

### New DTOs
```
dto/request/CreateProjectRequest.java  — name, description, priority, startDate, targetEndDate
dto/request/UpdateProjectRequest.java  — all optional (partial update)
dto/response/ProjectResponse.java      — id, name, status, priority, ownerName, memberCount, taskCount

dto/request/CreateTaskRequest.java     — title, description, priority, dueDate, projectId, assigneeId
dto/request/UpdateTaskRequest.java     — all optional
dto/response/TaskResponse.java         — id, title, status, priority, assigneeName, projectName
```

### New Repositories
```
repository/ProjectRepository.java
  - findByOwner(User)
  - findByMembersContaining(User)
  - findByStatus(ProjectStatus)

repository/TaskRepository.java
  - findByProject(Project)
  - findByAssignee(User)
  - findByProjectAndStatus(Project, TaskStatus)
  - countByProjectAndStatus(Project, TaskStatus)
```

### New Services + Controllers
```
service/ProjectService.java    → CRUD + addMember, removeMember
service/TaskService.java       → CRUD + getMyTasks, getByProject

controller/ProjectController.java
  POST   /api/projects
  GET    /api/projects
  GET    /api/projects/{id}
  PUT    /api/projects/{id}
  DELETE /api/projects/{id}
  POST   /api/projects/{id}/members/{userId}
  DELETE /api/projects/{id}/members/{userId}

controller/TaskController.java
  POST   /api/tasks
  GET    /api/tasks
  GET    /api/tasks/{id}
  GET    /api/projects/{id}/tasks
  PUT    /api/tasks/{id}
  DELETE /api/tasks/{id}
```

### Key Concepts to Teach
- @ManyToOne, @OneToMany, @ManyToMany
- Cascade types (ALL, MERGE, PERSIST)
- FetchType.LAZY vs EAGER (N+1 problem intro)
- @JoinColumn, @JoinTable
- @AuthenticationPrincipal — get logged-in user in controller
- Builder pattern for DTOs

---

## 8. Session 3: Workflow + Filtering + Pagination

### Status: NOT STARTED

### Features
1. Task state machine (valid transitions with InvalidStateTransitionException)
2. JPA Specifications (dynamic filtering — status, priority, keyword, assignee)
3. Pagination (Pageable, Page<T>, sort)
4. Search endpoint: `GET /api/tasks/search?status=IN_PROGRESS&priority=HIGH&page=0&size=20`

### New Files
```
specification/TaskSpecification.java
specification/ProjectSpecification.java
dto/response/PagedResponse.java  — content, page, size, totalElements, totalPages
exception/InvalidStateTransitionException.java
```

---

## 9. Session 4: AOP + Events + Scheduling

### Status: NOT STARTED

### Features

**AOP — Audit Logging:**
```
aspect/AuditLogAspect.java         — @Around all service methods, logs who/what/when/duration
entity/AuditLog.java               — saved to DB: action, entityType, userId, timestamp, duration
annotation/Auditable.java          — custom @Auditable annotation
```

**Spring Events:**
```
event/TaskCreatedEvent.java
event/TaskAssignedEvent.java
event/TaskStatusChangedEvent.java
event/ProjectCreatedEvent.java

listener/NotificationListener.java — @EventListener, creates Notification entities
entity/Notification.java           — id, userId, message, type, read, createdAt
service/NotificationService.java
controller/NotificationController.java
  GET /api/notifications
  PUT /api/notifications/{id}/read
```

**Scheduling:**
```
scheduler/TaskReminderScheduler.java  — @Scheduled cron, daily 9AM reminders
scheduler/OverdueTaskScheduler.java   — finds overdue tasks, creates notifications
```

---

## 10. Session 5: Razorpay Payment Integration

### Status: NOT STARTED

### Features
```
config/RazorpayConfig.java
entity/Payment.java, entity/SubscriptionPlan.java
service/PaymentService.java  — createOrder, verifyPayment (HMAC), handleWebhook
controller/PaymentController.java
  POST /api/payments/create-order
  POST /api/payments/verify
  POST /api/webhooks/razorpay
  GET  /api/payments/history
```

---

## 11. Session 6: Redis Caching + Performance

### Status: NOT STARTED

### Features
```
config/RedisConfig.java
@Cacheable("projects") on getProjectById
@CacheEvict on update/delete
@EntityGraph to fix N+1 queries
HikariCP connection pool tuning (already in application.yml)
@Index on frequently queried columns
```

---

## 12. Session 7: Apache Kafka + Event-Driven

### Status: NOT STARTED

### Features
```
kafka/producer/TaskEventProducer.java
kafka/consumer/NotificationConsumer.java
kafka/consumer/AuditConsumer.java
kafka/config/KafkaConfig.java  — DLQ, error handler, retry

Topics: task-events, user-events, payment-events, notification-events
```

docker-compose.yml additions: Zookeeper, Kafka broker

---

## 13. Session 8: React Frontend — Auth + Setup

### Status: NOT STARTED

### Setup Commands
```bash
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install react-router-dom axios
npm install @tanstack/react-query zustand react-hook-form
npm install @heroicons/react recharts framer-motion
npm install tailwindcss @tailwindcss/vite
```

### Files to Create
```
frontend/src/
  main.tsx                        — entry point
  App.tsx                         — router setup

  pages/
    LoginPage.tsx                 — login form
    RegisterPage.tsx              — register form
    DashboardPage.tsx             — placeholder

  components/
    auth/
      LoginForm.tsx
      RegisterForm.tsx
      ProtectedRoute.tsx          — redirect to /login if not authenticated
    common/
      Button.tsx                  — reusable button
      Input.tsx                   — reusable input
      Loading.tsx                 — spinner
      Toast.tsx                   — success/error notifications

  services/
    api.ts                        — Axios instance with interceptors (auto-attach token)
    authService.ts                — login(), register(), logout()

  stores/
    authStore.ts                  — Zustand: user, token, isAuthenticated

  types/
    auth.ts                       — TypeScript interfaces: LoginRequest, AuthResponse
    api.ts                        — ApiError interface

  hooks/
    useAuth.ts                    — custom hook wrapping auth operations

  utils/
    tokenUtils.ts                 — save/get/remove token from localStorage
```

### Key Frontend Concepts to Teach
- JSX: HTML inside JavaScript/TypeScript
- useState: local component state
- useEffect: run code after render (API calls)
- Props: passing data to child components
- TypeScript interfaces for API response types
- Axios interceptors: auto-attach JWT to every request
- React Router: how SPA routing works without page reload
- Zustand: global state simpler than Redux
- Protected routes: redirect unauthenticated users

---

## 14. Session 9: React Frontend — Dashboard + Tasks

### Status: NOT STARTED

### Pages
```
pages/
  DashboardPage.tsx       — stats cards, activity feed, charts
  ProjectsPage.tsx        — list + create projects
  ProjectDetailPage.tsx   — single project: members, task list
  TasksPage.tsx           — table view with filters
  KanbanPage.tsx          — drag-and-drop Kanban board
  ProfilePage.tsx         — user settings
  NotificationsPage.tsx   — notification list
  PaymentPage.tsx         — subscription plans + Razorpay checkout
```

### Components
```
components/
  dashboard/
    StatsCard.tsx           — "12 tasks" summary box
    RecentActivity.tsx      — activity feed
    ProjectChart.tsx        — pie/bar chart (Recharts)
  projects/
    ProjectCard.tsx
    ProjectForm.tsx
    ProjectMemberList.tsx
  tasks/
    TaskCard.tsx
    TaskForm.tsx            — create/edit modal
    TaskTable.tsx           — sortable/filterable table
    TaskFilters.tsx         — status/priority filter bar
    KanbanBoard.tsx         — dnd-kit drag-and-drop board
    KanbanColumn.tsx        — single column (TODO, IN_PROGRESS...)
  payments/
    PlanCard.tsx
    RazorpayCheckout.tsx
```

### Dark Mode
Every component supports dark/light mode via Tailwind `dark:` classes.
User preference stored in localStorage.

---

## 15. Session 10: Docker + Testing + Deployment

### Status: NOT STARTED

### Backend Tests
```
test/java/com/taskflow/
  service/AuthServiceTest.java          — @ExtendWith(MockitoExtension), mock all deps
  service/ProjectServiceTest.java
  controller/AuthControllerTest.java    — @WebMvcTest (test controller only)
  repository/UserRepositoryTest.java    — @DataJpaTest (H2)
  security/JwtServiceTest.java
  integration/AuthIntegrationTest.java  — @SpringBootTest (full stack)
```

Test profile uses `application-test.yml` (H2) automatically.

### Frontend Tests
```
frontend/src/__tests__/
  LoginForm.test.tsx      — renders, validates, submits
  TaskCard.test.tsx       — renders correctly
  authStore.test.ts       — state management tests
```

### Docker Multi-Stage Build
```
Dockerfile (backend)
  Stage 1: Build with Maven (full JDK)
  Stage 2: Run with JRE only (smaller image — ~200MB vs ~600MB)

frontend/Dockerfile
  Stage 1: Build with Node
  Stage 2: Serve with Nginx
```

### GitHub Actions CI/CD
```
.github/workflows/ci.yml
  Triggers: push to main, all PRs
  Steps: checkout → Java 17 → mvn test → mvn package → build Docker image
  Frontend: Node 18 → npm ci → npm run build → npm test
```

### Postman Collection
```
postman/TaskFlow.postman_collection.json
  — auto-set JWT from login response
  — organized: Auth / Projects / Tasks / Payments
```

### Flyway Migrations (replace ddl-auto: update)
```
resources/db/migration/
  V1__create_users_table.sql
  V2__create_projects_tasks_tables.sql
  V3__add_notifications_table.sql
```

---

## 16. Session 11: Spring AI Integration

### Status: NOT STARTED

### What is Spring AI?
Spring AI is Spring's framework for integrating AI models (Claude, GPT-4, Gemini) into Spring Boot apps. It provides a unified API regardless of which AI provider you use.

### Setup
```xml
<!-- pom.xml -->
<dependency>
  <groupId>org.springframework.ai</groupId>
  <artifactId>spring-ai-anthropic-spring-boot-starter</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  ai:
    anthropic:
      api-key: ${ANTHROPIC_API_KEY}
      chat:
        options:
          model: claude-sonnet-4-6
```

### Features to Build

**Feature 1 — Smart Project Summary**
```
GET /api/projects/{id}/ai-summary
→ Reads all tasks in the project
→ Sends to AI: "Summarize this project's status and highlight blockers"
→ Returns: "Project has 20 tasks. 8 are overdue. Backend API is most blocked area."
```

**Feature 2 — AI Task Description Generator**
```
POST /api/tasks/ai-generate-description
Body: { "title": "Fix login bug with special characters" }
→ AI generates a full, professional task description
→ Returns structured description with acceptance criteria
```

**Feature 3 — Natural Language Task Search**
```
GET /api/tasks/ai-search?q=things blocking the frontend team
→ AI understands intent → searches tasks semantically
→ Returns relevant tasks even if exact words don't match
```

### Files to Create
```
service/AiService.java            — Spring AI ChatClient wrapper
controller/AiController.java
  GET  /api/projects/{id}/ai-summary
  POST /api/tasks/ai-generate-description
  GET  /api/tasks/ai-search

dto/request/AiGenerateRequest.java
dto/response/AiSummaryResponse.java
```

### Key Concepts to Teach
- What an LLM is and how it works
- Spring AI ChatClient
- Prompt templates
- System prompts vs user prompts
- Token limits and costs
- When to use AI vs regular search

---

## 17. Session 12: WebSockets — Real-Time Features

### Status: NOT STARTED

### What are WebSockets?
Normal HTTP: Client asks → Server answers (one direction per request).
WebSocket: Permanent two-way connection. Server can push data to client anytime.

### Setup
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### Features to Build

**Feature 1 — Live Notifications**
```
Raja assigns task to Priya
→ Server pushes notification to Priya's browser INSTANTLY
→ Notification badge updates without page refresh
```

**Feature 2 — Live Kanban Board**
```
Raja moves task from TODO → IN_PROGRESS on Kanban
→ All team members viewing that project see it move in real-time
→ Like Google Docs — collaborative live updates
```

### Files to Create
```
config/WebSocketConfig.java       — STOMP endpoint, message broker
controller/WebSocketController.java — @MessageMapping handlers
service/NotificationPushService.java — sends to specific users

frontend:
  hooks/useWebSocket.ts           — connect, subscribe, receive messages
  (updates notification badge and Kanban in real-time)
```

---

## 18. Session 13: OAuth2 Social Login

### Status: NOT STARTED

### What is OAuth2?
"Login with Google" — instead of your app managing passwords, you delegate authentication to Google.

Flow:
```
1. User clicks "Login with Google"
2. Redirected to Google's login page
3. User approves — Google sends a "code" to your backend
4. Backend exchanges code for user's Google profile (name, email)
5. Backend creates/finds user in your DB
6. Backend generates your JWT and returns it
7. Same JWT flow from this point
```

### Setup
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>
```

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid, profile, email
```

### Files to Create
```
security/oauth2/OAuth2SuccessHandler.java  — on success, generate JWT, redirect
security/oauth2/OAuth2UserService.java     — extract user info from Google response
```

Frontend addition:
```
"Login with Google" button on LoginPage
```

---

## 19. Session 14: Rate Limiting + Email + File Upload

### Status: NOT STARTED

### Rate Limiting (Bucket4j)
Prevent brute-force attacks and API abuse.
```xml
<dependency>
  <groupId>com.github.vladimir-bukhtoyarov</groupId>
  <artifactId>bucket4j-core</artifactId>
</dependency>
```

Rules:
```
/api/auth/login     → max 5 attempts per minute per IP
/api/auth/register  → max 3 per hour per IP
All endpoints       → max 100 per minute per user
Response: HTTP 429 Too Many Requests
```

```
config/RateLimitConfig.java
filter/RateLimitFilter.java
```

### Email Notifications (JavaMailSender)
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

Emails to send:
```
1. Welcome email after registration
2. "Task assigned to you: [title]" notification
3. "Task due tomorrow: [title]" daily reminder (via @Scheduled)
4. "You've been added to project: [name]"
```

```
service/EmailService.java
templates/email/
  welcome.html         — Thymeleaf HTML template
  task-assigned.html
  task-reminder.html
```

Use Mailtrap.io (free fake inbox for testing — no real emails sent).

### File Upload (AWS S3)
```xml
<dependency>
  <groupId>software.amazon.awssdk</groupId>
  <artifactId>s3</artifactId>
</dependency>
```

Feature: Task attachments (PDF, images, docs)
```
entity/TaskAttachment.java         — id, taskId, fileName, s3Url, fileSize, uploadedBy
service/FileStorageService.java    — upload to S3, generate pre-signed URL
controller/FileController.java
  POST   /api/tasks/{id}/attachments  → upload
  GET    /api/tasks/{id}/attachments  → list
  DELETE /api/tasks/{id}/attachments/{fileId}
```

---

## 20. Session 15: Observability — Prometheus + Grafana

### Status: NOT STARTED

### What is Observability?
Knowing what your app is doing in real-time — how fast, how many errors, resource usage.

Three pillars:
- **Metrics** — numbers over time (requests/sec, error rate, DB query time)
- **Logs** — what happened (structured JSON logs)
- **Tracing** — follow one request across all services

### Setup
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

docker-compose.yml additions:
```yaml
prometheus:
  image: prom/prometheus
  ports: ["9090:9090"]
  volumes: ["./prometheus.yml:/etc/prometheus/prometheus.yml"]

grafana:
  image: grafana/grafana
  ports: ["3001:3000"]
```

### Grafana Dashboard Shows:
```
- HTTP requests per second (by endpoint)
- Average response time (p50, p95, p99)
- Error rate (4xx and 5xx)
- JVM memory usage (heap, non-heap)
- Active database connections (HikariCP pool)
- Garbage collection stats
- Kafka consumer lag (Session 7+)
- Cache hit rate (Redis, Session 6+)
```

---

## 21. Session 16: Frontend Add-ons

### Status: NOT STARTED

### Dark Mode
```typescript
// tailwind.config.ts — enable class-based dark mode
darkMode: 'class'

// Toggle component
const toggleDark = () => {
  document.documentElement.classList.toggle('dark')
  localStorage.setItem('theme', isDark ? 'light' : 'dark')
}

// Every component uses dark: prefix:
<div className="bg-white dark:bg-gray-900 text-black dark:text-white">
```

### Framer Motion — Animations
```tsx
import { motion } from 'framer-motion'

// Tasks animate in when created
<motion.div
  initial={{ opacity: 0, y: -20 }}
  animate={{ opacity: 1, y: 0 }}
  exit={{ opacity: 0 }}
>
  <TaskCard {...task} />
</motion.div>

// Page transitions
// Notification badge bounce
// Kanban drag animations
```

### Progressive Web App (PWA)
Makes the web app installable on phones and work offline.
```
public/manifest.json          — app name, icons, colors
public/sw.js                  — service worker (offline caching)
vite.config.ts                — vite-plugin-pwa configuration
```

Users can: install on home screen, get push notifications, view tasks offline.

### React Testing Library + Vitest
```typescript
// frontend/src/__tests__/LoginForm.test.tsx
import { render, screen, fireEvent } from '@testing-library/react'

test('shows error when email is empty', async () => {
  render(<LoginForm />)
  fireEvent.click(screen.getByText('Login'))
  expect(await screen.findByText('Email required')).toBeInTheDocument()
})
```

### Storybook — Component Documentation
```bash
npx storybook@latest init
```
Showcases every UI component in isolation — great for portfolio.

---

## 22. Session 17: Elasticsearch

### Status: NOT STARTED

### What is Elasticsearch?
A search engine optimized for full-text search. Finds `"login authentication issue"` even if the task title says `"Auth bug on signin page"` — it understands language.

Database SQL `LIKE '%login%'` → slow on millions of records.
Elasticsearch → fast, relevant, ranked results.

### Setup
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

docker-compose addition:
```yaml
elasticsearch:
  image: elasticsearch:8.11.0
  ports: ["9200:9200"]
  environment:
    - discovery.type=single-node
    - xpack.security.enabled=false
```

### Features
```
GET /api/search?q=authentication bug in production
→ searches task titles AND descriptions simultaneously
→ returns results ranked by relevance
→ highlights matching words in results

Search across: tasks, projects, comments
Suggestions: autocomplete as you type
```

```
document/TaskDocument.java      — Elasticsearch index mapping
repository/TaskSearchRepository.java
service/SearchService.java
controller/SearchController.java
  GET /api/search?q=keyword&type=task|project&page=0&size=10
```

---

## 23. Learning Folders

Reference guides already created (non-executable Java files):

| File | Topics |
|---|---|
| `learn/microservices/MicroservicesGuide.java` | API Gateway, Eureka, OpenFeign, Circuit Breaker |
| `learn/kubernetes/KubernetesGuide.java` | Pod, Deployment, Service, HPA, kubectl |
| `learn/springai/SpringAiGuide.java` | ChatClient, Prompts, RAG, Embeddings |
| `learn/aws/AwsDeploymentGuide.java` | EC2, RDS, S3, ECS, ElastiCache |
| `learn/monitoring/MonitoringGuide.java` | Prometheus, Grafana, ELK Stack, Actuator |

To add in later sessions:
- `learn/performance/PerformanceGuide.java` — N+1, indexing, query optimization
- `learn/testing/TestingGuide.java` — testing pyramid, TDD, BDD
- `learn/designpatterns/DesignPatternsGuide.java` — patterns used in this project
- `learn/frontend/ReactGuide.java` — React patterns, hooks, state management
- `learn/security/SecurityGuide.java` — OWASP top 10, JWT best practices

---

## 24. Coding Conventions

### Every Java File Pattern
```
1. Package declaration
2. Imports (grouped: Spring / Jakarta / Lombok / Java)
3. Class-level comment: WHAT / WHY / HOW / Real-world analogy
4. Class declaration with annotations
5. Fields (private, final where possible)
6. Methods with comments explaining WHY (not what — the name says what)
7. Bottom: Interview Q&A block
```

### Comment Style
```java
/**
 * WHAT: AuthService handles register and login
 * WHY:  Separates business logic from HTTP layer (SRP)
 * HOW:  Register: validate → hash password → save → generate tokens
 *
 * INTERVIEW Q: Why not put this logic in the controller?
 * A: Controllers handle HTTP concerns only. Business logic in services
 *    makes code testable, reusable, and maintainable.
 */
```

### Naming Conventions
- Entities: `User`, `Project`, `Task` (singular PascalCase)
- Repos: `UserRepository` (Entity + Repository)
- Services: `AuthService`, `ProjectService` (feature + Service)
- Controllers: `AuthController` (feature + Controller)
- Request DTOs: `CreateProjectRequest`, `LoginRequest`
- Response DTOs: `ProjectResponse`, `AuthResponse`

### Git Branch Naming
```
devin/session-{N}-{feature-name}
Examples:
  devin/session-2-projects-tasks-crud
  devin/session-11-spring-ai
  devin/session-14-rate-limiting-email-s3
```

### Git Commit Format
```
Session {N}: {brief description}

- Detail 1
- Detail 2
```

---

## 25. Complete Week-by-Week Learning Roadmap

```
Week 1:   Session 2 — JPA relationships, Projects + Tasks CRUD
Week 2:   Session 3 — Search, Filters, Pagination
Week 3:   Session 4 — AOP, Events, Scheduling
Week 4:   Session 5 — Razorpay Payments
Week 5:   Session 6 — Redis Caching + Performance
Week 6:   Session 7 — Apache Kafka
Week 7:   Session 8 — React Setup + Auth Pages
Week 8:   Session 9 — Full Frontend Dashboard
Week 9:   Session 10 — Docker + Tests + CI/CD

         ← PUT ON RESUME HERE (strong backend + full frontend) →

Week 10:  Session 14 (easy add-ons) — Rate Limiting + Email + Dark Mode
Week 11:  Session 12 — WebSockets (real-time)
Week 12:  Session 11 — Spring AI
Week 13:  Session 13 — OAuth2 Social Login
Week 14:  Session 15 — Observability (Prometheus + Grafana)
Week 15:  Session 16 — Frontend Add-ons (Animations, PWA, Tests)
Week 16:  Session 17 — Elasticsearch
Week 17:  Polish — README, live deployment on Railway/Render, portfolio
```

---

## 26. Resume Lines — Final Version

```
TaskFlow — Full-Stack AI-Powered Project Management Platform
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
GitHub: github.com/Raja7380/taskflow  |  Live: taskflow.up.railway.app

• Spring Boot 3 REST API with Spring Security 6 — JWT auth with
  access/refresh token rotation, BCrypt password hashing, role-based
  access control (USER/MANAGER/ADMIN), Google OAuth2 social login

• JPA/Hibernate entity relationships (@ManyToOne, @ManyToMany) for
  Projects, Tasks, Users — dynamic filtering with JPA Specifications,
  cursor-based pagination, task state machine with transition validation

• Integrated Razorpay payment gateway — order creation, HMAC-SHA256
  signature verification, webhook processing for subscription management

• Spring AI integration — AI task summarization (Claude API), description
  generator, natural language task search using prompt engineering

• Real-time features via Spring WebSocket (STOMP) — live Kanban board
  updates and instant notifications pushed to connected clients

• Redis caching (@Cacheable, @CacheEvict) reducing DB load on frequent
  reads; Apache Kafka for async event streaming — notifications, audit
  logs, analytics consumers with Dead Letter Queue

• Rate limiting (Bucket4j) on auth endpoints, transactional email
  notifications (JavaMailSender + Thymeleaf), file attachments via AWS S3

• React 18 + TypeScript frontend — drag-and-drop Kanban (dnd-kit),
  Recharts dashboard, dark mode, PWA-enabled, Framer Motion animations,
  React Query for server state, Zustand for global auth state

• Production observability — Spring Actuator + Prometheus + Grafana
  dashboard tracking request rates, error rates, DB pool, JVM metrics

• Containerized with Docker Compose (app + PostgreSQL + Redis + Kafka);
  GitHub Actions CI/CD; 85%+ test coverage (JUnit 5, Mockito, @WebMvcTest,
  React Testing Library)

Tech Stack:
  Backend:  Java 17, Spring Boot 3, Spring Security, JPA/Hibernate,
            PostgreSQL, Redis, Kafka, Spring AI, WebSocket
  Frontend: React 18, TypeScript, Tailwind CSS, React Query, Zustand
  DevOps:   Docker, GitHub Actions, AWS S3, Prometheus, Grafana
```

---

## 27. How to Give This to Another AI

### Step 1: Share this document
Copy the entire `PROJECT_MASTER_PLAN.md` and paste it at the start of your conversation.

### Step 2: Tell them what to build
```
"I'm building TaskFlow (see master plan). Session 1 is complete.
Build Session {N}: {name}.

Branch from: main
New branch:  devin/session-{N}-{feature-name}
Commit as:   Raja <rajasingh12587@gmail.com>

Follow the exact specs in the master plan for Session {N}.
Every file must have WHAT/WHY/HOW comments + Interview Q&A at the bottom.
All code must compile: ./mvnw compile"
```

### Step 3: Remind them of rules
```
1. Every file has WHAT/WHY/HOW at top, Interview Q&A at bottom
2. Use Lombok — no manual getters/setters
3. DTOs separate from entities (never expose entity in API)
4. Global exception handler catches all errors
5. Swagger @Tag + @Operation on all controllers
6. Service layer pattern: Controller → Service → Repository
7. PostgreSQL (not H2) — Docker must be running
8. Tests use application-test.yml (H2 auto, no Docker needed)
9. Commit under: Raja <rajasingh12587@gmail.com>
```

---

*This is the single source of truth for TaskFlow. Update after every session.*
*Last updated: June 2026 — PostgreSQL migration, full frontend + 7 add-on sessions added.*
