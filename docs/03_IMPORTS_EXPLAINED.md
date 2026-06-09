# Imports Explained — What Do All Those Import Lines Mean?

> **Why does every file start with 10-20 import lines?**
> This doc explains what they are so they stop being scary.

---

## What Is an Import?

An import is just telling Java: **"I want to use this thing from this library."**

It's like ordering from a menu:
```
import org.springframework.web.bind.annotation.RestController;
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//     "I want RestController from the Spring Web library"
```

**You do NOT need to memorize imports.** Your IDE (IntelliJ, VS Code) adds them automatically when you type a class name. If you type `@RestController`, the IDE suggests the import.

---

## The 7 Import Groups You'll See

### 1. `org.springframework.*` — Spring Framework

This is the main framework we use. Everything Spring-related comes from here.

```java
// Web/REST annotations
import org.springframework.web.bind.annotation.RestController;    // makes class an API handler
import org.springframework.web.bind.annotation.RequestMapping;    // sets base URL path
import org.springframework.web.bind.annotation.PostMapping;       // handles POST requests
import org.springframework.web.bind.annotation.GetMapping;        // handles GET requests
import org.springframework.web.bind.annotation.RequestBody;       // reads JSON body

// Core Spring
import org.springframework.stereotype.Service;                    // marks business logic class
import org.springframework.stereotype.Component;                  // marks managed class
import org.springframework.beans.factory.annotation.Value;        // reads from application.yml

// Security
import org.springframework.security.config.annotation.web.builders.HttpSecurity;  // security rules
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;           // password hashing
import org.springframework.security.core.userdetails.UserDetails;                  // user interface

// HTTP
import org.springframework.http.ResponseEntity;                   // HTTP response wrapper
import org.springframework.http.HttpStatus;                       // HTTP status codes (200, 404, etc.)
```

**Think of it as:** `org.springframework` = "from the Spring library"

### 2. `jakarta.persistence.*` — Database (JPA)

Everything related to saving data to the database. Replaced the old `javax.persistence` package.

```java
import jakarta.persistence.Entity;                    // "this class = database table"
import jakarta.persistence.Table;                     // "table name is..."
import jakarta.persistence.Id;                        // "this field = primary key"
import jakarta.persistence.GeneratedValue;            // "auto-generate this value"
import jakarta.persistence.GenerationType;            // strategy: IDENTITY, AUTO, SEQUENCE
import jakarta.persistence.Column;                    // "configure this database column"
import jakarta.persistence.Enumerated;                // "store enum as string/number"
import jakarta.persistence.EnumType;                  // STRING or ORDINAL
import jakarta.persistence.PrePersist;                // "run before first save"
import jakarta.persistence.PreUpdate;                 // "run before update"
```

**Think of it as:** `jakarta.persistence` = "database stuff"

**Why `jakarta` not `javax`?**
When Java EE moved to the Eclipse Foundation, the package name changed from `javax` to `jakarta`. Same functionality, just renamed. Spring Boot 3+ uses `jakarta`.

### 3. `jakarta.validation.*` — Input Validation

Annotations that check if user input is correct.

```java
import jakarta.validation.Valid;                              // "validate this object"
import jakarta.validation.constraints.NotBlank;               // "cannot be empty"
import jakarta.validation.constraints.Email;                  // "must be valid email"
import jakarta.validation.constraints.Size;                   // "length between min and max"
import jakarta.validation.constraints.NotNull;                // "cannot be null"
import jakarta.validation.constraints.Min;                    // "number must be at least..."
```

**Think of it as:** `jakarta.validation` = "input checking rules"

### 4. `lombok.*` — Code Generator

Lombok generates boring repetitive code (getters, setters, constructors) at compile time.

```java
import lombok.Data;                        // generates getters + setters + toString + equals
import lombok.Builder;                     // generates .builder().field(value).build() pattern
import lombok.NoArgsConstructor;           // generates empty constructor
import lombok.AllArgsConstructor;          // generates constructor with all fields
import lombok.RequiredArgsConstructor;     // generates constructor with final fields
```

**Think of it as:** `lombok` = "write less code"

### 5. `io.jsonwebtoken.*` — JWT Library

Third-party library for creating and reading JWT tokens.

```java
import io.jsonwebtoken.Jwts;               // main JWT builder
import io.jsonwebtoken.Claims;             // data inside the token (payload)
import io.jsonwebtoken.io.Decoders;        // decode Base64 strings
import io.jsonwebtoken.security.Keys;      // create signing keys
```

**Think of it as:** `io.jsonwebtoken` = "JWT token operations"

### 6. `io.swagger.*` / `org.springdoc.*` — API Documentation

Makes the Swagger UI page at `/swagger-ui.html`.

```java
import io.swagger.v3.oas.annotations.Operation;     // describes what an endpoint does
import io.swagger.v3.oas.annotations.tags.Tag;       // groups endpoints together
```

**Think of it as:** `io.swagger` = "API docs decorations"

### 7. `java.util.*` / `java.time.*` — Standard Java

Built-in Java classes you already know from java-zero-to-hero.

```java
import java.util.List;                     // List<User>
import java.util.Map;                      // Map<String, String>
import java.util.HashMap;                  // Map implementation
import java.util.Optional;                 // maybe-has-value wrapper
import java.util.Collection;               // parent of List, Set
import java.util.Date;                     // date/time (old style, used by JWT)
import java.util.function.Function;        // functional interface: takes input, returns output
import java.time.LocalDateTime;            // modern date/time
```

**Think of it as:** `java.util` = "standard Java tools you already know"

---

## How to Read Import Blocks

When you open a file and see 15 imports, **don't read them all**. Instead:

```java
import org.springframework.web.bind.annotation.*;    // "Ah, this is a web/REST file"
import org.springframework.security.*;                // "It involves security"
import jakarta.persistence.*;                         // "It touches the database"
import lombok.*;                                      // "It uses code generation"
```

Just glance at the PACKAGE NAMES to know what the file deals with.

---

## The Import Pattern in Our Project

| File | Main Imports | What This Tells You |
|------|-------------|---------------------|
| `User.java` | jakarta.persistence, lombok | "Database entity with auto-generated code" |
| `UserRepository.java` | org.springframework.data.jpa | "Database access interface" |
| `RegisterRequest.java` | jakarta.validation, lombok | "Input data with validation rules" |
| `AuthService.java` | org.springframework.stereotype, security | "Business logic involving security" |
| `AuthController.java` | org.springframework.web, swagger | "API endpoint with documentation" |
| `JwtService.java` | io.jsonwebtoken, org.springframework.beans | "Token handling using JWT library + Spring config" |
| `SecurityConfig.java` | org.springframework.security | "Security setup file" |
| `GlobalExceptionHandler.java` | org.springframework.web, http | "Error handling for API responses" |

---

## FAQs

**Q: Do I need to type imports manually?**
A: No! In IntelliJ: type the class name → press Alt+Enter → import is added. In VS Code with Java extensions: same thing. The IDE handles it.

**Q: What does `import ... .*` mean?**
A: It imports EVERYTHING from that package. Example: `import java.util.*` imports List, Map, Set, Optional, etc. Some people prefer specific imports (`import java.util.List`) for clarity — both work.

**Q: Why are there so many imports in some files?**
A: Because that file uses tools from many libraries. A controller uses Spring Web (for HTTP), Swagger (for docs), validation (for @Valid), and the DTO classes. Each tool needs its import. It looks scary but each line is just "I want to use X".

**Q: What if I see an import I don't recognize?**
A: Check which GROUP it belongs to (see the 7 groups above). The group name tells you what it does. If still confused, Google "java <annotation-name>" — there's always a simple explanation.
