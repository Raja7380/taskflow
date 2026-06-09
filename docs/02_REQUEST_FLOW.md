# Request Flow — How a Login Request Travels Through the Code

> **This document traces ONE real request through EVERY file it touches.**
> After reading this, the "mesh" of files becomes a clear straight line.

---

## The Big Picture

When a user sends a request to our app, it passes through files in this exact order:

```
USER (Postman/Browser)
  │
  │  POST /api/auth/register  {"fullName":"Raja", "email":"raja@gmail.com", "password":"secret123"}
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  SPRING SECURITY FILTER CHAIN                        │
│                                                      │
│  1. JwtAuthenticationFilter.java                     │
│     → Checks: Is there a JWT token? NO (it's a      │
│       register request, user has no token yet)       │
│     → Skips authentication, passes request along     │
│                                                      │
│  2. SecurityConfig.java (rules)                      │
│     → Checks: Is /api/auth/** permitted? YES         │
│     → Allows the request through                     │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  CONTROLLER LAYER                                    │
│                                                      │
│  3. AuthController.java                              │
│     → @PostMapping("/register") matches the URL      │
│     → @RequestBody converts JSON → RegisterRequest   │
│     → @Valid triggers validation                     │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  VALIDATION (automatic, no file to read)             │
│                                                      │
│  4. RegisterRequest.java (checked by @Valid)          │
│     → @NotBlank fullName: "Raja" ✓                   │
│     → @Email email: "raja@gmail.com" ✓               │
│     → @Size(min=8) password: "secret123" (9 chars) ✓ │
│     → All pass! Continue...                          │
│                                                      │
│  (If any fail → MethodArgumentNotValidException      │
│   → caught by GlobalExceptionHandler.java            │
│   → returns error JSON to user)                      │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  SERVICE LAYER                                       │
│                                                      │
│  5. AuthService.java → register() method             │
│     a. Check if email already exists in database     │
│        → calls UserRepository.existsByEmail()        │
│        → if YES: throw DuplicateResourceException    │
│        → if NO: continue                             │
│                                                      │
│     b. Create User object using Builder pattern      │
│        → User.builder()                              │
│            .fullName("Raja")                         │
│            .email("raja@gmail.com")                  │
│            .password(passwordEncoder.encode("secret123"))  │
│            .role(Role.USER)                          │
│            .build()                                  │
│                                                      │
│     c. Save to database                              │
│        → calls UserRepository.save(user)             │
│                                                      │
│     d. Generate JWT tokens                           │
│        → calls JwtService.generateAccessToken(user)  │
│        → calls JwtService.generateRefreshToken(user) │
│                                                      │
│     e. Build response                                │
│        → AuthResponse.builder()                      │
│            .accessToken("eyJhbG...")                  │
│            .refreshToken("eyJhbG...")                 │
│            .fullName("Raja")                         │
│            .email("raja@gmail.com")                  │
│            .role("USER")                             │
│            .build()                                  │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  │  (Steps 5a and 5c go to Repository layer)
  ▼
┌─────────────────────────────────────────────────────┐
│  REPOSITORY LAYER                                    │
│                                                      │
│  6. UserRepository.java                              │
│     → existsByEmail("raja@gmail.com")                │
│       Spring auto-generates: SELECT COUNT(*) > 0     │
│       FROM users WHERE email = 'raja@gmail.com'      │
│       → returns false (email not in DB)              │
│                                                      │
│     → save(user)                                     │
│       @PrePersist runs → sets createdAt = now()      │
│       Hibernate generates:                           │
│       INSERT INTO users (full_name, email, password, │
│         role, created_at, updated_at)                │
│       VALUES ('Raja', 'raja@gmail.com',              │
│         '$2a$10$...hashed...', 'USER',               │
│         '2024-06-05 16:30:00', '2024-06-05 16:30:00')│
│       → returns user with id = 1                     │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  │  (Step 5d goes to JWT service)
  ▼
┌─────────────────────────────────────────────────────┐
│  JWT SERVICE                                         │
│                                                      │
│  7. JwtService.java → generateAccessToken(user)      │
│     → Builds JWT with:                               │
│       Header:  {"alg": "HS256"}                      │
│       Payload: {                                     │
│         "sub": "raja@gmail.com",                     │
│         "role": "ROLE_USER",                         │
│         "iat": 1717600200,  (issued at)              │
│         "exp": 1717601100   (expires in 15 min)      │
│       }                                              │
│       Signature: HMAC-SHA256(header + payload,       │
│                              secret-key-from-yml)    │
│     → Returns: "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlI..."  │
│                                                      │
│     → generateRefreshToken(user)                     │
│       Same but expires in 7 days (no role in payload)│
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  │  Response travels back up the chain
  ▼
┌─────────────────────────────────────────────────────┐
│  RESPONSE (back to user)                             │
│                                                      │
│  AuthController wraps response:                      │
│  HTTP 201 CREATED                                    │
│  {                                                   │
│    "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ...",    │
│    "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJ...",   │
│    "fullName": "Raja",                               │
│    "email": "raja@gmail.com",                        │
│    "role": "USER"                                    │
│  }                                                   │
│                                                      │
└─────────────────────────────────────────────────────┘
```

---

## LOGIN Flow (After Registration)

```
USER sends: POST /api/auth/login  {"email":"raja@gmail.com", "password":"secret123"}
  │
  ▼
JwtAuthenticationFilter → No token → skip
  │
  ▼
SecurityConfig → /api/auth/** → permitAll → allowed
  │
  ▼
AuthController.login() → @Valid checks LoginRequest
  │
  ▼
AuthService.login()
  │
  ├── Step 1: authenticationManager.authenticate()
  │     └── Spring Security does this internally:
  │         a. Calls UserDetailsServiceImpl.loadUserByUsername("raja@gmail.com")
  │         b. UserDetailsServiceImpl calls userRepository.findByEmail("raja@gmail.com")
  │         c. Gets the User object from database
  │         d. Compares: passwordEncoder.matches("secret123", "$2a$10$...hashed...")
  │         e. If password wrong → throws BadCredentialsException
  │                                → caught by GlobalExceptionHandler
  │                                → returns 401 "Invalid email or password"
  │         f. If password correct → authentication succeeds!
  │
  ├── Step 2: userRepository.findByEmail("raja@gmail.com") → gets User
  │
  ├── Step 3: jwtService.generateAccessToken(user) → new token
  │
  ├── Step 4: jwtService.generateRefreshToken(user) → new refresh token
  │
  └── Step 5: return AuthResponse with both tokens
  │
  ▼
HTTP 200 OK  { "accessToken": "...", "refreshToken": "...", ... }
```

---

## PROTECTED Endpoint Flow (After Login)

Now the user has a token. They call a protected endpoint:

```
USER sends: GET /api/projects
            Header: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ...
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter.java                        │
│                                                      │
│  1. Sees "Authorization: Bearer ..." header → YES!   │
│  2. Extracts token: "eyJhbGciOiJIUzI1NiJ9.eyJ..."   │
│  3. Calls jwtService.extractUsername(token)           │
│     → Decodes token → gets "raja@gmail.com"          │
│  4. Calls userDetailsService.loadUserByUsername(...)  │
│     → Gets User from database                        │
│  5. Calls jwtService.isTokenValid(token, user)       │
│     → Checks: username matches? YES                  │
│     → Checks: token expired? NO (still within 15min) │
│     → Returns TRUE                                   │
│  6. Creates authentication object                    │
│     → Sets it in SecurityContextHolder               │
│     → Now Spring knows WHO is making the request     │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  ▼
SecurityConfig → /api/projects → authenticated() → user IS authenticated → allowed!
  │
  ▼
ProjectController.getProjects() → runs normally
  │
  ▼
Response back to user
```

**What if the token is expired or invalid?**
```
JwtAuthenticationFilter
  → isTokenValid() returns FALSE
  → Does NOT set authentication
  → SecurityConfig sees: not authenticated
  → Returns 401 Unauthorized
```

---

## ERROR Flow

What happens when something goes wrong:

```
USER sends: POST /api/auth/register  {"fullName":"", "email":"bad", "password":"123"}
  │
  ▼
AuthController → @Valid triggers validation
  │
  ▼
RegisterRequest checks:
  @NotBlank fullName → "" is blank → FAIL
  @Email email → "bad" is not email → FAIL  
  @Size(min=8) password → "123" is 3 chars → FAIL
  │
  ▼
Throws: MethodArgumentNotValidException (with all 3 errors)
  │
  ▼
┌─────────────────────────────────────────────────────┐
│  GlobalExceptionHandler.java                         │
│                                                      │
│  @ExceptionHandler(MethodArgumentNotValidException)  │
│  → Extracts each field error:                        │
│    fullName → "Full name is required"                │
│    email → "Please provide a valid email address"    │
│    password → "Password must be at least 8 characters"│
│  → Builds ApiErrorResponse                           │
│                                                      │
└─────────────────────────────────────────────────────┘
  │
  ▼
HTTP 400 BAD REQUEST
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "timestamp": "2024-06-05T16:30:00",
  "fieldErrors": {
    "fullName": "Full name is required",
    "email": "Please provide a valid email address",
    "password": "Password must be at least 8 characters"
  }
}
```

---

## File-to-File Connection Map

Here's which file calls which — the arrows show the flow:

```
                    ┌──────────────────┐
                    │  application.yml │ (settings — read by @Value)
                    └────────┬─────────┘
                             │ provides config to
                             ▼
┌────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ HTTP       │───→│ JwtAuthFilter    │───→│ JwtService       │
│ Request    │    │ (security check) │    │ (token handling)  │
└────────────┘    └────────┬─────────┘    └──────────────────┘
                           │ if allowed
                           ▼
                  ┌──────────────────┐
                  │ AuthController   │ (receives request)
                  │ @RestController  │
                  └────────┬─────────┘
                           │ calls
                           ▼
                  ┌──────────────────┐    ┌──────────────────┐
                  │ AuthService      │───→│ JwtService       │
                  │ @Service         │    │ (generate tokens) │
                  └────────┬─────────┘    └──────────────────┘
                           │ calls
                           ▼
                  ┌──────────────────┐
                  │ UserRepository   │ (database queries)
                  │ @Repository      │
                  └────────┬─────────┘
                           │ manages
                           ▼
                  ┌──────────────────┐
                  │ User @Entity     │ (database table)
                  └──────────────────┘

        ERRORS anywhere? → GlobalExceptionHandler catches them
```

---

## Summary — The "Mesh" is Actually Just 6 Layers

Every Spring Boot app follows this pattern:

```
Layer 1: SECURITY      → Filter checks if user is allowed
Layer 2: CONTROLLER    → Receives the HTTP request
Layer 3: VALIDATION    → Checks if input data is valid
Layer 4: SERVICE       → Does the business logic
Layer 5: REPOSITORY    → Talks to the database
Layer 6: ENTITY        → Represents the database table

Errors at any layer → EXCEPTION HANDLER → returns clean error to user
```

That's it. Every feature you'll ever build follows these same 6 layers. Once you understand this for auth, you understand it for tasks, projects, payments — everything.
