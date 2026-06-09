# TaskFlow — Full-Stack Project Management System

A **resume-ready, full-stack project** built to learn and demonstrate modern enterprise Java development.

---

## 🔰 START HERE — New to Spring Boot?

If annotations like `@RestController`, `@Service`, `@Entity` look confusing, **read these docs FIRST** before looking at any code:

| # | Doc | What You'll Learn | Time |
|---|-----|-------------------|------|
| 1 | [`docs/00_WHAT_ARE_ANNOTATIONS.md`](docs/00_WHAT_ARE_ANNOTATIONS.md) | What annotations are, how Spring uses them, WHY they exist | 15 min |
| 2 | [`docs/01_ANNOTATIONS_CHEATSHEET.md`](docs/01_ANNOTATIONS_CHEATSHEET.md) | Quick-reference card for EVERY annotation in this project | Keep open |
| 3 | [`docs/02_REQUEST_FLOW.md`](docs/02_REQUEST_FLOW.md) | How a register/login request travels through ALL files step by step | 10 min |
| 4 | [`docs/03_IMPORTS_EXPLAINED.md`](docs/03_IMPORTS_EXPLAINED.md) | What all those import lines mean (7 groups) | 5 min |

**After reading these 4 docs, the code will make sense.** Then follow the reading order below.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.5, Spring Security 6, Spring Data JPA |
| **Auth** | JWT (JSON Web Tokens) with access + refresh tokens |
| **Database** | H2 (dev) / PostgreSQL (prod), Flyway migrations |
| **API Docs** | Swagger/OpenAPI 3 (auto-generated) |
| **Build** | Maven, Lombok |
| **DevOps** | Docker, Docker Compose |

## Quick Start

### Option 1: Run with H2 (simplest — no setup needed)
```bash
./mvnw spring-boot:run
```
App starts at: http://localhost:8080
Swagger UI: http://localhost:8080/swagger-ui.html
H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:taskflow`)

### Option 2: Run with Docker (PostgreSQL + Redis)
```bash
docker-compose up -d          # Start PostgreSQL + Redis
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

## API Endpoints

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT tokens |

### Health Check (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Check if app is running |

## Project Structure
```
src/main/java/com/taskflow/
    TaskFlowApplication.java     # Main entry point
    config/                       # Security, CORS, Swagger config
    security/                     # JWT service, auth filter
    entity/                       # JPA entities (DB tables)
    repository/                   # Data access layer
    dto/                          # Request/Response DTOs
    service/                      # Business logic
    controller/                   # REST endpoints
    exception/                    # Custom exceptions + global handler
    learn/                        # Advanced topic reference guides
        microservices/            # Microservices architecture
        kubernetes/               # K8s deployment
        springai/                 # Spring AI integration
        aws/                      # AWS cloud deployment
        monitoring/               # Prometheus, Grafana, ELK
```

## Learning Path

This project is designed for learning. Every file contains:
- **WHY comments** — explains the reasoning behind every decision
- **HOW comments** — explains how things work internally
- **Interview Q&A** — common interview questions related to that code

### Suggested reading order (simplest → hardest):

**Read the docs first:** `docs/00_WHAT_ARE_ANNOTATIONS.md` → `01_ANNOTATIONS_CHEATSHEET.md` → `02_REQUEST_FLOW.md` → `03_IMPORTS_EXPLAINED.md`

**Then read code files in this order:**
1. `entity/Role.java` — Simplest file (just an enum with 3 values)
2. `entity/User.java` — Database entity, see @Entity, @Table, @Column, @Data
3. `repository/UserRepository.java` — Spring auto-generates database code
4. `dto/request/RegisterRequest.java` — Validation annotations (@NotBlank, @Email)
5. `dto/request/LoginRequest.java` — Same pattern, simpler
6. `dto/response/AuthResponse.java` — Builder pattern for responses
7. `service/AuthService.java` — Business logic, @Service, dependency injection
8. `controller/AuthController.java` — REST API, @PostMapping, @Valid, @RequestBody
9. `controller/HealthController.java` — Simplest controller (sanity check)
10. `security/jwt/JwtService.java` — Token creation, @Value reads from config
11. `security/filter/JwtAuthenticationFilter.java` — Security filter
12. `config/SecurityConfig.java` — Security rules (@Configuration, @Bean)
13. `exception/GlobalExceptionHandler.java` — Catches all errors centrally

## Roadmap

- [x] Session 1: Project setup + JWT Authentication
- [ ] Session 2: Projects & Tasks CRUD + JPA Relationships
- [ ] Session 3: Task workflow + Filtering + Pagination
- [ ] Session 4: AOP Audit + Events + Scheduling
- [ ] Session 5: Razorpay Payment Integration
- [ ] Session 6: Redis Caching + Performance
- [ ] Session 7: Apache Kafka + Event-Driven
- [ ] Session 8: React Frontend (Auth)
- [ ] Session 9: React Frontend (Dashboard + Tasks)
- [ ] Session 10: Docker + Testing + Polish

## Author

Built as a portfolio project demonstrating Spring Boot, Spring Security, JPA, React, Docker, and more.
