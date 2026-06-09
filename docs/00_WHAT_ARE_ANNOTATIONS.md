# What Are Annotations? — The Complete Beginner Guide

> **Read this FIRST before looking at any code file.**
> After reading this, every `@Something` in the project will make sense.

---

## The Simple Analogy

Think of annotations as **stickers** you put on your code.

**Real-life example:**
- You have a plain cardboard box (your Java class)
- You put a sticker on it that says "FRAGILE" (an annotation)
- The delivery person (Spring Framework) sees the sticker and handles the box carefully

The box itself doesn't change. The sticker just tells someone else HOW to treat it.

**In Java:**
```java
@Service          // ← This is a sticker that says "this class has business logic"
public class AuthService {
    // ... your code
}
```

You wrote a normal class. The `@Service` sticker tells Spring: *"Hey, create an object of this class and manage it for me."*

Without the sticker, Spring ignores the class. With the sticker, Spring takes charge.

---

## How Does Java Know What `@Service` Means?

You might think: "But `@Service` isn't a Java keyword like `public` or `class`. How does it work?"

**Answer: Someone DEFINED what `@Service` means.**

An annotation is just a special type of interface that someone created. Spring's developers created `@Service`, `@Controller`, `@Entity` etc. They also wrote code that READS these annotations at runtime and does something.

**The flow:**
```
1. You write: @Service on your class
2. Spring starts up
3. Spring scans all your classes
4. Spring finds @Service on AuthService
5. Spring thinks: "Ah, this needs to be managed by me"
6. Spring creates an object of AuthService and stores it
7. Whenever someone needs AuthService, Spring gives them the stored object
```

This is called **Inversion of Control (IoC)** — YOU don't create objects with `new AuthService()`, SPRING creates them for you.

---

## Why Not Just Use `new AuthService()`?

Good question! Look at this:

```java
public class AuthService {
    private UserRepository userRepository;   // needs database access
    private PasswordEncoder passwordEncoder; // needs password hashing
    private JwtService jwtService;           // needs token generation
    
    // If YOU create it manually:
    // AuthService service = new AuthService();
    // But wait... who creates UserRepository? And PasswordEncoder? And JwtService?
    // And JwtService needs its OWN dependencies too...
    // It becomes a nightmare!
}
```

**With annotations:**
```java
@Service                        // Spring manages this class
@RequiredArgsConstructor        // Lombok auto-creates a constructor
public class AuthService {
    private final UserRepository userRepository;      // Spring auto-provides this
    private final PasswordEncoder passwordEncoder;    // Spring auto-provides this
    private final JwtService jwtService;              // Spring auto-provides this
    
    // Spring sees the constructor needs 3 things
    // Spring already has all 3 (because they also have annotations)
    // Spring creates AuthService with all 3 injected automatically!
}
```

**One annotation (`@Service`) saves you from manually creating and wiring 10+ objects.**

---

## The 3 Types of Annotations in Our Project

### Type 1: "Who Are You?" Annotations (Identity)

These tell Spring WHAT ROLE a class plays:

| Annotation | Plain English | Example |
|-----------|--------------|---------|
| `@Controller` | "I handle web pages (HTML)" | Not used in our project (we use REST) |
| `@RestController` | "I handle API requests (JSON)" | `AuthController.java` |
| `@Service` | "I have business logic" | `AuthService.java` |
| `@Repository` | "I talk to the database" | `UserRepository.java` |
| `@Component` | "I'm a general helper class" | `JwtAuthenticationFilter.java` |
| `@Configuration` | "I set up other beans/settings" | `SecurityConfig.java` |
| `@Entity` | "I represent a database table" | `User.java` |

**Key insight:** `@RestController`, `@Service`, `@Repository` are ALL just specialized versions of `@Component`. They all mean "Spring, manage this class." The different names just make your code readable — you know at a glance what each class does.

```
@Component (generic)
  ├── @RestController  → handles HTTP requests
  ├── @Service         → business logic
  └── @Repository      → database access
```

### Type 2: "What Should I Do?" Annotations (Behavior)

These tell Spring (or Java) HOW to handle something:

| Annotation | Plain English | Where Used |
|-----------|--------------|------------|
| `@GetMapping("/api/health")` | "When someone visits /api/health, run this method" | `HealthController.java` |
| `@PostMapping("/register")` | "When someone sends POST to /register, run this" | `AuthController.java` |
| `@Valid` | "Check the input data for errors before running" | `AuthController.java` |
| `@RequestBody` | "Take the JSON body and convert to Java object" | `AuthController.java` |
| `@NotBlank` | "This field cannot be empty" | `RegisterRequest.java` |
| `@Email` | "This field must be a valid email" | `RegisterRequest.java` |
| `@Size(min=8)` | "This field must be at least 8 characters" | `RegisterRequest.java` |
| `@PrePersist` | "Run this method just before saving to database" | `User.java` |
| `@PreUpdate` | "Run this method just before updating in database" | `User.java` |
| `@Bean` | "The object returned by this method should be managed by Spring" | `SecurityConfig.java` |
| `@Value("${jwt.secret}")` | "Read this value from application.yml" | `JwtService.java` |
| `@ExceptionHandler` | "When this type of error happens, run this method" | `GlobalExceptionHandler.java` |

### Type 3: "How Am I Built?" Annotations (Structure)

These control how your class/object is constructed:

| Annotation | Plain English | Where Used |
|-----------|--------------|------------|
| `@Data` | Lombok: auto-create getters, setters, toString, equals | `User.java`, all DTOs |
| `@Builder` | Lombok: allow `User.builder().name("Raja").build()` syntax | `User.java`, all DTOs |
| `@NoArgsConstructor` | Lombok: create empty constructor `new User()` | `User.java`, all DTOs |
| `@AllArgsConstructor` | Lombok: create constructor with ALL fields | `User.java`, all DTOs |
| `@RequiredArgsConstructor` | Lombok: create constructor for `final` fields only | Services, Controllers |
| `@Id` | JPA: "this field is the primary key" | `User.java` |
| `@GeneratedValue` | JPA: "auto-generate this value (1, 2, 3...)" | `User.java` |
| `@Column` | JPA: "configure the database column for this field" | `User.java` |
| `@Table(name="users")` | JPA: "name the database table 'users'" | `User.java` |
| `@Enumerated` | JPA: "store this enum as a string in DB" | `User.java` |

---

## The "Magic" Explained — How Spring Starts Up

When you run the application, here's what happens step by step:

```
Step 1: You run TaskFlowApplication.main()
        └── calls SpringApplication.run()

Step 2: Spring sees @SpringBootApplication on the class
        └── This is actually 3 annotations combined:
            @Configuration      → "this class can define beans"
            @EnableAutoConfiguration → "auto-setup database, security, etc."
            @ComponentScan      → "scan all classes in this package & sub-packages"

Step 3: Component Scanning begins
        Spring looks at EVERY class in com.taskflow.* packages
        
        Found @Entity on User.java → "Ah, this is a database table"
            → Spring tells Hibernate to create a 'users' table
        
        Found @Repository on UserRepository → "This talks to database"
            → Spring creates an implementation automatically!
               (You wrote an interface, Spring writes the actual code)
        
        Found @Service on JwtService → "Business logic class"
            → Spring creates one instance and stores it
        
        Found @Service on AuthService → "Business logic class"
            → Spring sees constructor needs UserRepository, PasswordEncoder, JwtService
            → Spring already has all 3! Injects them automatically
        
        Found @RestController on AuthController → "API handler"
            → Spring sees constructor needs AuthService
            → Spring already has it! Injects it
        
        Found @Configuration on SecurityConfig → "Settings class"
            → Spring reads the @Bean methods and creates those objects too

Step 4: Embedded Tomcat server starts on port 8080
        Spring registers all @GetMapping/@PostMapping URLs

Step 5: App is ready! Waiting for requests...
```

**This is why the order of annotations matters — they form a chain:**
```
@SpringBootApplication
  └── scans and finds
        @Entity → creates DB table
        @Repository → creates DB access object
        @Service → creates business logic objects (with repositories injected)
        @RestController → creates API handlers (with services injected)
        @Configuration → creates config objects
```

---

## What About Imports?

Every annotation comes from a **package** (library). The import tells Java where to find it.

```java
import org.springframework.web.bind.annotation.RestController;
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^  ^^^^^^^^^^^^^^
//     Package (where it lives)                 Annotation name
```

**You don't need to memorize imports.** Your IDE (IntelliJ/VS Code) auto-adds them. Just understand the groups:

| Import starts with... | What it is |
|----------------------|------------|
| `org.springframework.*` | Spring Framework (the main framework) |
| `jakarta.persistence.*` | JPA/Database annotations (@Entity, @Id, @Column) |
| `jakarta.validation.*` | Validation annotations (@NotBlank, @Email, @Size) |
| `lombok.*` | Lombok (auto-generates code like getters/setters) |
| `io.jsonwebtoken.*` | JWT library (create/read tokens) |
| `io.swagger.*` | Swagger (API documentation) |
| `java.util.*` | Standard Java (List, Map, Optional, etc.) |

**Rule of thumb:** If you see an unfamiliar annotation, look at its import — the package name tells you which library it belongs to.

---

## Test Your Understanding

After reading this, you should be able to answer:

**Q1: What does `@RestController` do?**
A: It tells Spring "this class handles API requests and returns JSON." Spring will scan it, create an instance, and register its URL mappings.

**Q2: What's the difference between `@Service` and `@Repository`?**
A: Functionally, they're almost identical (both tell Spring to manage the class). The difference is just for readability — `@Service` = business logic, `@Repository` = database access. `@Repository` also adds automatic exception translation for database errors.

**Q3: Why does the code use `@RequiredArgsConstructor` instead of writing a constructor?**
A: It's a Lombok annotation that auto-generates a constructor for all `final` fields. Since Spring uses constructors to inject dependencies, this saves you from writing boilerplate like:
```java
// WITHOUT Lombok — you'd write this manually:
public AuthService(UserRepository repo, PasswordEncoder encoder, JwtService jwt) {
    this.userRepository = repo;
    this.passwordEncoder = encoder;
    this.jwtService = jwt;
}

// WITH Lombok @RequiredArgsConstructor — the above is generated automatically!
```

**Q4: What does `@Valid @RequestBody RegisterRequest request` mean?**
A: Three things happening:
1. `@RequestBody` → take the JSON from the HTTP request body and convert it to a `RegisterRequest` Java object
2. `@Valid` → before using it, check all the validation rules (@NotBlank, @Email, @Size) on RegisterRequest's fields
3. If validation fails, Spring automatically throws an error (caught by our GlobalExceptionHandler)

**Q5: What does `@PrePersist` do on the User entity?**
A: "Pre" = before, "Persist" = save to database. So `@PrePersist` means "run this method automatically just BEFORE saving this entity to the database." We use it to set `createdAt = LocalDateTime.now()`.

---

## Reading Order for TaskFlow Code

Now that you understand annotations, read the files in this order:

```
1. entity/Role.java            ← simplest file (just an enum, 1 annotation)
2. entity/User.java            ← see @Entity, @Table, @Column, @Data, @Builder
3. repository/UserRepository.java  ← see @Repository, how Spring generates code
4. dto/request/RegisterRequest.java ← see @NotBlank, @Email, @Size
5. dto/request/LoginRequest.java    ← same pattern, simpler
6. dto/response/AuthResponse.java   ← see @Builder pattern
7. service/AuthService.java        ← see @Service, @RequiredArgsConstructor, business logic
8. controller/AuthController.java  ← see @RestController, @PostMapping, @Valid
9. controller/HealthController.java ← simplest controller
10. security/jwt/JwtService.java   ← see @Value for reading config
11. security/filter/JwtAuthenticationFilter.java ← see @Component, filter chain
12. config/SecurityConfig.java     ← see @Configuration, @Bean
13. exception/GlobalExceptionHandler.java ← see @RestControllerAdvice, @ExceptionHandler
```

Each file builds on what you learned in the previous one. Don't jump around!

---

## One Final Thing

**You do NOT need to memorize all annotations.** In real work:
- You'll use the same 10-15 annotations repeatedly
- Your IDE auto-suggests them
- You'll Google new ones when needed

The goal right now is to understand the CONCEPT: annotations are stickers that tell Spring how to manage your code. The specific ones become muscle memory after writing a few files.

**Next:** Read `01_ANNOTATIONS_CHEATSHEET.md` for a quick-reference card you can keep open while reading code.
