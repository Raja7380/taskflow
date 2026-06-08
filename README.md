# TaskFlow — Full-Stack Project Management System

A **resume-ready, full-stack project** built to learn and demonstrate modern enterprise Java development.

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

### Suggested reading order:
1. `TaskFlowApplication.java` — Entry point, @SpringBootApplication explained
2. `entity/User.java` — JPA entity, UserDetails, Lombok
3. `repository/UserRepository.java` — Spring Data JPA, derived queries
4. `dto/` — DTO pattern, validation annotations
5. `security/jwt/JwtService.java` — JWT creation and validation
6. `security/filter/JwtAuthenticationFilter.java` — Security filter chain
7. `config/SecurityConfig.java` — Spring Security configuration
8. `service/AuthService.java` — Business logic, service layer pattern
9. `controller/AuthController.java` — REST controller, HTTP methods
10. `exception/GlobalExceptionHandler.java` — Centralized error handling

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
