# Annotations Cheatsheet — Quick Reference Card

> **Keep this open while reading any code file.**
> Ctrl+F to search for any annotation you don't recognize.

---

## Spring Core — "Who manages this class?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@SpringBootApplication` | "This is the starting point of the app" (combines 3 annotations below) | `TaskFlowApplication.java` |
| `@Configuration` | "This class creates and configures objects (beans)" | `SecurityConfig.java` |
| `@EnableAutoConfiguration` | "Spring, auto-configure everything based on dependencies" | Inside `@SpringBootApplication` |
| `@ComponentScan` | "Spring, scan all classes in this package for annotations" | Inside `@SpringBootApplication` |
| `@Component` | "Spring, manage this class (create + store an instance)" | `JwtAuthenticationFilter.java` |
| `@Service` | Same as @Component, but signals "business logic lives here" | `AuthService.java`, `JwtService.java` |
| `@Repository` | Same as @Component, but signals "database access lives here" | `UserRepository.java` |
| `@RestController` | Same as @Component + "handles HTTP requests, returns JSON" | `AuthController.java` |
| `@Bean` | "The object returned by this METHOD should be managed by Spring" | `SecurityConfig.java` → `passwordEncoder()` |
| `@RequiredArgsConstructor` | Lombok: "generate a constructor for all `final` fields" → enables DI | All services, controllers |
| `@Value("${key}")` | "Read this value from application.yml and inject it" | `JwtService.java` → reads jwt.secret |

### How they connect:
```
@SpringBootApplication starts scanning
  ├── Finds @Component, @Service, @Repository, @RestController
  │     → Creates instances of those classes
  ├── Finds @Configuration classes
  │     → Reads @Bean methods, creates those objects too
  └── Uses @RequiredArgsConstructor to wire everything together
```

---

## Web/REST — "How do HTTP requests work?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@RestController` | "This class handles HTTP requests and returns JSON" | `AuthController.java` |
| `@RequestMapping("/api/auth")` | "All URLs in this class start with /api/auth" | `AuthController.java` |
| `@GetMapping("/health")` | "Handle GET requests to this URL" | `HealthController.java` |
| `@PostMapping("/register")` | "Handle POST requests to this URL" | `AuthController.java` |
| `@RequestBody` | "Convert the JSON body of the request into a Java object" | `AuthController.java` |
| `@Valid` | "Validate the object (check @NotBlank, @Email etc.) before using" | `AuthController.java` |
| `@ResponseStatus` | "Return this HTTP status code" | Can set on methods |
| `@RestControllerAdvice` | "This class catches ALL exceptions from ALL controllers" | `GlobalExceptionHandler.java` |
| `@ExceptionHandler(X.class)` | "When exception X is thrown, run this method instead of crashing" | `GlobalExceptionHandler.java` |

### The request flow:
```
User sends: POST /api/auth/register  {"name":"Raja", "email":"..."}
                    │
                    ▼
@RestController + @RequestMapping("/api/auth")  ← finds AuthController
                    │
                    ▼
@PostMapping("/register")  ← finds the register() method
                    │
                    ▼
@RequestBody  ← converts JSON → RegisterRequest object
                    │
                    ▼
@Valid  ← checks @NotBlank, @Email, @Size → if invalid, throws error
                    │                              │
                    ▼                              ▼
            runs the method              @ExceptionHandler catches it
            returns AuthResponse         returns error JSON
```

---

## Validation — "Is the input data correct?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@Valid` | "Trigger validation on this object" | On method parameters in controllers |
| `@NotBlank` | "This string cannot be null, empty, or just spaces" | `RegisterRequest.fullName` |
| `@NotNull` | "This field cannot be null (but CAN be empty string)" | General use |
| `@Email` | "Must be a valid email format (has @ and domain)" | `RegisterRequest.email` |
| `@Size(min=8, max=100)` | "String length must be between min and max" | `RegisterRequest.password` |
| `@Min(1)` / `@Max(100)` | "Number must be at least/at most this value" | For numeric fields |
| `@Pattern(regexp="...")` | "Must match this regex pattern" | For custom formats |

### How validation works:
```
                    @Valid triggers checking
                         │
                         ▼
    ┌──────────────────────────────────────┐
    │  RegisterRequest fields:              │
    │                                       │
    │  @NotBlank fullName  → "Raja" ✓       │
    │  @Email    email     → "r@g.com" ✓    │
    │  @Size(8)  password  → "abc" ✗ TOO SHORT │
    └──────────────────────────────────────┘
                         │
                         ▼
              MethodArgumentNotValidException thrown
                         │
                         ▼
              @ExceptionHandler catches it
              Returns: {"fieldErrors": {"password": "must be at least 8 chars"}}
```

---

## JPA/Database — "How does Java talk to the database?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@Entity` | "This class IS a database table" | `User.java` |
| `@Table(name="users")` | "The table name in the database is 'users'" | `User.java` |
| `@Id` | "This field is the PRIMARY KEY" | `User.id` |
| `@GeneratedValue(strategy=IDENTITY)` | "Database auto-generates this value (1, 2, 3...)" | `User.id` |
| `@Column(nullable=false)` | "Configure this column: cannot be null" | `User.fullName` |
| `@Column(unique=true)` | "This column must have unique values (no duplicates)" | `User.email` |
| `@Column(length=150)` | "Max characters for this column in DB" | `User.email` |
| `@Column(updatable=false)` | "Once set, this column cannot be changed" | `User.createdAt` |
| `@Enumerated(EnumType.STRING)` | "Store enum as text 'USER' not number 0" | `User.role` |
| `@PrePersist` | "Run this method BEFORE saving to DB (first time)" | `User.onCreate()` |
| `@PreUpdate` | "Run this method BEFORE updating in DB" | `User.onUpdate()` |

### What happens when you save a user:
```
userRepository.save(user)
        │
        ▼
@PrePersist → runs onCreate() → sets createdAt = now
        │
        ▼
Hibernate reads @Entity, @Table, @Column annotations
        │
        ▼
Generates SQL: INSERT INTO users (full_name, email, password, role, created_at)
               VALUES ('Raja', 'raja@gmail.com', '$2a$...hashed...', 'USER', '2024-...')
        │
        ▼
@Id @GeneratedValue → Database assigns id = 1 and returns it
```

---

## Lombok — "Generate boring code automatically"

| Annotation | What Code It Generates | Example |
|-----------|----------------------|---------|
| `@Data` | getters + setters + toString() + equals() + hashCode() | `User.java` |
| `@Builder` | `.builder().name("Raja").email("...").build()` pattern | `User.java` |
| `@NoArgsConstructor` | `public User() { }` (empty constructor) | `User.java` |
| `@AllArgsConstructor` | `public User(Long id, String name, ...)` (all fields) | `User.java` |
| `@RequiredArgsConstructor` | Constructor with only `final` fields (for DI) | `AuthService.java` |
| `@Slf4j` | Creates a `log` variable for logging | Future use |

### Without vs With Lombok:
```java
// WITHOUT Lombok — you write ALL of this manually (50+ lines):
public class User {
    private String name;
    private String email;
    
    public User() { }
    public User(String name, String email) { this.name = name; this.email = email; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String toString() { return "User(name=" + name + ", email=" + email + ")"; }
    public boolean equals(Object o) { ... }
    public int hashCode() { ... }
}

// WITH Lombok — 4 annotations replace ALL the above:
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class User {
    private String name;
    private String email;
}
// Lombok GENERATES all that code at compile time. You don't see it, but it exists.
```

---

## Security — "Who can access what?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@EnableWebSecurity` | "Activate Spring Security for this app" | `SecurityConfig.java` |
| `@EnableMethodSecurity` | "Allow @PreAuthorize on individual methods" | `SecurityConfig.java` |
| `@PreAuthorize("hasRole('ADMIN')")` | "Only ADMIN users can call this method" | Future use |

### Security flow for every request:
```
HTTP Request arrives
        │
        ▼
JwtAuthenticationFilter (our custom filter)
  ├── Has "Authorization: Bearer <token>" header?
  │     NO → skip, let Spring Security decide (public or blocked)
  │     YES ↓
  ├── Extract token, decode it
  ├── Find user in database
  ├── Token valid? Set user in SecurityContext
  │
        ▼
SecurityFilterChain checks URL
  ├── /api/auth/**  → permitAll() → anyone can access
  ├── /api/health   → permitAll() → anyone can access
  ├── /swagger-ui/** → permitAll() → anyone can access
  ├── /api/admin/** → hasRole("ADMIN") → only admins
  └── everything else → authenticated() → must be logged in
```

---

## Swagger — "Auto-generate API documentation"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@Tag(name="Auth")` | "Group these endpoints under 'Auth' in Swagger UI" | `AuthController.java` |
| `@Operation(summary="...")` | "Description of what this endpoint does" | On each @PostMapping |
| `@Schema(description="...")` | "Description of this field in the docs" | On DTO fields |

These don't change how code works — they just make the Swagger page at `http://localhost:8080/swagger-ui.html` look better.

---

## Jackson (JSON) — "How does Java convert to/from JSON?"

| Annotation | One-Line Meaning | Example |
|-----------|-----------------|---------|
| `@JsonInclude(NON_NULL)` | "Don't include null fields in the JSON response" | `AuthResponse.java` |
| `@JsonProperty("name")` | "Use this name in JSON instead of the field name" | General use |
| `@JsonIgnore` | "Never include this field in JSON (e.g., password)" | General use |

---

## How to Use This Cheatsheet

1. Open any code file in the project
2. See an annotation you don't remember? → Ctrl+F here
3. Read the one-line meaning → that's all you need
4. If you want deeper understanding → read `00_WHAT_ARE_ANNOTATIONS.md`

**Pro tip:** After reading 3-4 files, you'll start recognizing annotations without checking. It's like learning a new language — at first you translate every word, then it becomes natural.
