package com.taskflow.learn.microservices;

/**
 * =====================================================================
 * MICROSERVICES — Complete Guide for Future Extension
 * =====================================================================
 *
 * WHAT ARE MICROSERVICES?
 *   An architectural style where a large application is split into
 *   small, independent services that communicate over the network.
 *
 *   Monolith (what we build first):
 *     [One big application with all features]
 *     Auth + Tasks + Projects + Payments = ONE deployable unit
 *
 *   Microservices (future extension):
 *     [auth-service] <-> [task-service] <-> [payment-service]
 *     Each service is independently deployable, scalable, and maintainable.
 *
 * WHEN TO USE MICROSERVICES?
 *   Start with a MONOLITH. Split into microservices ONLY when:
 *   1. Team is large (5+ developers working on same codebase)
 *   2. Different parts need different scaling (payment gets 10x traffic)
 *   3. Different parts need different tech stacks
 *   4. Deployment bottleneck (one small change requires redeploying everything)
 *
 *   DON'T use microservices when:
 *   - Team is small (1-5 developers)
 *   - Application is simple CRUD
 *   - You're learning / prototyping
 *
 * =====================================================================
 * KEY COMPONENTS (Spring Cloud):
 * =====================================================================
 *
 * 1. API GATEWAY (Spring Cloud Gateway)
 *    WHY: Single entry point for all client requests
 *    WHAT: Routes requests to the correct microservice
 *
 *    Client -> [API Gateway :8080]
 *                 |-> /api/auth/**    -> [Auth Service :8081]
 *                 |-> /api/tasks/**   -> [Task Service :8082]
 *                 |-> /api/payments/* -> [Payment Service :8083]
 *
 *    Also handles: Rate limiting, authentication, load balancing, logging
 *
 *    Spring dependency: spring-cloud-starter-gateway
 *
 * 2. SERVICE DISCOVERY (Eureka)
 *    WHY: Services need to find each other
 *    PROBLEM: In microservices, services run on different ports/machines.
 *             Hard-coding URLs is fragile.
 *    SOLUTION: Each service REGISTERS with Eureka on startup.
 *              When Service A needs Service B, it asks Eureka for B's address.
 *
 *    [Auth Service] --register--> [Eureka Server] <--discover-- [Task Service]
 *    "I'm at 192.168.1.5:8081"                    "Where is Auth Service?"
 *                                                  "It's at 192.168.1.5:8081"
 *
 *    Spring dependency: spring-cloud-starter-netflix-eureka-server (server)
 *                       spring-cloud-starter-netflix-eureka-client (services)
 *
 * 3. OPENFEIGN (Declarative REST Client)
 *    WHY: Services need to call each other's APIs
 *    WHAT: Write an interface, Feign generates the HTTP client
 *
 *    // In Task Service, calling Auth Service:
 *    @FeignClient(name = "auth-service")
 *    public interface AuthClient {
 *        @GetMapping("/api/users/{id}")
 *        UserDTO getUserById(@PathVariable Long id);
 *    }
 *    // Feign automatically makes HTTP GET to auth-service/api/users/123
 *
 *    Spring dependency: spring-cloud-starter-openfeign
 *
 * 4. CIRCUIT BREAKER (Resilience4j)
 *    WHY: What if a service is DOWN? Don't let it crash everything!
 *    WHAT: If a service fails N times, STOP calling it temporarily.
 *
 *    States:
 *      CLOSED (normal) -> calls go through
 *      OPEN (tripped)  -> calls immediately fail with fallback
 *      HALF-OPEN       -> allow some calls to test if service recovered
 *
 *    @CircuitBreaker(name = "authService", fallbackMethod = "authFallback")
 *    public UserDTO getUser(Long id) {
 *        return authClient.getUserById(id);  // might fail
 *    }
 *    public UserDTO authFallback(Long id, Exception ex) {
 *        return new UserDTO(id, "Unknown User", null);  // graceful degradation
 *    }
 *
 *    Spring dependency: spring-cloud-starter-circuitbreaker-resilience4j
 *
 * 5. CONFIG SERVER
 *    WHY: Each service needs configuration (DB URL, secrets, etc.)
 *    WHAT: Centralized configuration server — all services fetch config from here
 *
 *    [Config Server] -> reads from Git repo with config files
 *    [Auth Service]  -> GET /auth-service/dev -> gets its config
 *    [Task Service]  -> GET /task-service/prod -> gets its config
 *
 *    Spring dependency: spring-cloud-config-server
 *
 * 6. DISTRIBUTED TRACING (Zipkin + Micrometer)
 *    WHY: When a request goes through 5 services, how do you debug it?
 *    WHAT: Each request gets a unique TRACE ID that follows it across services
 *
 *    Request -> Gateway (traceId=abc) -> Auth (traceId=abc) -> Task (traceId=abc)
 *    In Zipkin UI: search for traceId=abc -> see the entire journey
 *
 *    Spring dependency: micrometer-tracing-bridge-brave + zipkin-reporter-brave
 *
 * =====================================================================
 * HOW TO EXTEND TASKFLOW INTO MICROSERVICES:
 * =====================================================================
 *
 * Phase 1: Extract Auth Service
 *   - Move entity/User, security/*, AuthService, AuthController to new project
 *   - Auth Service manages JWT tokens + user database
 *   - Other services validate JWTs locally (just need the secret key)
 *
 * Phase 2: Extract Task Service
 *   - Move Task/Project entities, services, controllers to new project
 *   - Uses Feign to call Auth Service for user info
 *
 * Phase 3: Extract Payment Service
 *   - Move Razorpay integration to new project
 *   - Communicates via Kafka events (not direct HTTP)
 *
 * Phase 4: Add Infrastructure
 *   - Eureka Server for service discovery
 *   - API Gateway for routing
 *   - Config Server for centralized config
 *
 * =====================================================================
 * INTERVIEW QUESTIONS:
 * =====================================================================
 *
 * Q: What are microservices?
 * A: An architectural style where an application is composed of small,
 *    independent services that communicate via APIs. Each service owns
 *    its data, can be deployed independently, and can use different tech stacks.
 *
 * Q: Monolith vs Microservices — pros and cons?
 * A: Monolith: simpler, easier to test, no network latency between modules.
 *    Microservices: scalable, independent deployment, team autonomy, but adds
 *    complexity (network, distributed transactions, eventual consistency).
 *
 * Q: How do microservices communicate?
 * A: Synchronous: REST (Feign), gRPC
 *    Asynchronous: Message queues (Kafka, RabbitMQ)
 *    Prefer async for loose coupling.
 *
 * Q: What is eventual consistency?
 * A: In microservices, each service has its own DB. When Service A updates,
 *    Service B might not see it immediately. Eventually, all services
 *    will have consistent data (via events/messages). This is a trade-off
 *    for scalability and availability (CAP theorem).
 *
 * Q: What is the CAP theorem?
 * A: A distributed system can only guarantee 2 of 3:
 *    Consistency — every read gets the latest data
 *    Availability — every request gets a response
 *    Partition tolerance — system works despite network failures
 *    In practice, P is required, so you choose between C and A.
 */
public class MicroservicesGuide {
    // This is a learning reference file — no executable code.
    // Read the comments above to understand microservices concepts.
}
