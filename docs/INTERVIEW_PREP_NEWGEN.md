# Interview Prep — Newgen Software | Java + MySQL + Spring Boot
### Tomorrow's interview — everything you need, nothing extra

---

## HOW TO USE THIS FILE

Read Section 1 first (your project intro). That's what you say in the first 2 minutes.
Then go topic by topic. Each answer is written exactly how you should SAY it — not a textbook definition.

---

# SECTION 1 — YOUR PROJECT INTRO (Say this first)

> "I built a **full-stack project management tool called TaskFlow** — similar to Jira.
> It's a Spring Boot REST API with JWT authentication, PostgreSQL database, and Redis caching.
>
> Key features I built:
> - **User auth** with JWT tokens — register, login, role-based access (ADMIN/USER)
> - **Projects & Tasks CRUD** — with JPA relationships (one user owns many projects, one project has many tasks)
> - **State machine** for task status — a task can only go TODO → IN_PROGRESS → IN_REVIEW → DONE, invalid transitions throw 400 errors
> - **AOP-based audit logging** — every create/update/delete is logged automatically without touching business code
> - **Razorpay payment integration** — subscription plans with HMAC-SHA256 signature verification
> - **Redis caching** — frequently-read data cached with TTL, fixed N+1 query problem with @EntityGraph
> - **Scheduled jobs** — daily reminders for due tasks using @Scheduled with cron expressions
>
> The architecture follows proper layering: Controller → Service → Repository → Database."

**If they ask to see it:** Open Swagger UI at `http://localhost:8080/swagger-ui.html`

---

# SECTION 2 — CORE JAVA

## OOP — The 4 Pillars (asked in almost every interview)

### Encapsulation
> "Wrapping data and methods in a class and hiding internal details. In Java, we use `private` fields and `public` getters/setters. Example: in my User entity, the `password` field is private and never exposed in the API response."

```java
public class User {
    private String password;    // hidden
    public String getEmail() { return email; }  // controlled access
}
```

### Inheritance
> "A child class gets the properties and methods of the parent class. In my project, all my exception classes extend `RuntimeException` — they inherit `getMessage()`, `getCause()` etc. without rewriting them."

```java
public class ResourceNotFoundException extends RuntimeException {
    // inherits getMessage(), getCause() from RuntimeException
}
```

### Polymorphism
> "Same method name, different behavior. Two types: compile-time (overloading) and runtime (overriding). In Spring, `JpaRepository` has a `findById` method. My `ProjectRepository` OVERRIDES it with `@EntityGraph` to change behavior — same method name, different SQL generated."

### Abstraction
> "Hiding implementation details, showing only what's needed. Interface is the best example. My `ProjectRepository` extends `JpaRepository` — I call `projectRepository.save(project)` without knowing whether it does INSERT or UPDATE. That's abstraction."

---

## Interface vs Abstract Class

| | Interface | Abstract Class |
|---|---|---|
| Variables | `public static final` only | Any type |
| Methods | Abstract + default (Java 8+) | Abstract + concrete |
| Multiple inheritance | Yes (implements multiple) | No (extends one) |
| Constructor | No | Yes |
| Use when | Defining a contract/capability | Shared base implementation |

> "I use interfaces for repositories — `JpaRepository<User, Long>` is an interface. Spring creates the implementation at runtime. If I needed shared code (like a `BaseEntity` with createdAt/updatedAt), I'd use an abstract class."

---

## String, StringBuilder, StringBuffer

| | String | StringBuilder | StringBuffer |
|---|---|---|---|
| Mutable? | No | Yes | Yes |
| Thread-safe? | Yes (immutable) | No | Yes |
| Speed | Slowest in loops | Fastest | Slower than StringBuilder |

> "String is immutable — every `+` creates a new object. If you concatenate in a loop, use StringBuilder. StringBuffer is thread-safe but slower. In an interview: if they say multi-threaded environment → StringBuffer. Otherwise → StringBuilder."

```java
// Bad in loop:
String result = "";
for (int i = 0; i < 1000; i++) result += i;  // creates 1000 String objects

// Good:
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 1000; i++) sb.append(i);  // one object
String result = sb.toString();
```

---

## Collections — The Most Asked Topic

```
Collection (interface)
├── List (ordered, duplicates allowed)
│   ├── ArrayList  — fast get(i), slow insert/delete in middle
│   └── LinkedList — slow get(i), fast insert/delete
├── Set (no duplicates)
│   ├── HashSet    — no order, O(1) add/contains
│   └── TreeSet    — sorted order, O(log n)
└── Map (key-value)
    ├── HashMap    — no order, O(1) get/put
    └── TreeMap    — sorted by key, O(log n)
```

### ArrayList vs LinkedList
> "ArrayList uses an array internally — fast random access `O(1)`, slow insert/delete in the middle `O(n)` because elements shift. LinkedList uses nodes with pointers — slow access `O(n)`, fast insert/delete `O(1)` if you have the node. In practice, ArrayList is used 90% of the time. Use LinkedList only when you insert/delete frequently from the middle."

### HashMap internals (very common question)
> "HashMap stores entries in an array of buckets. It calls `hashCode()` on the key to find the bucket index. If two keys hash to the same bucket (collision), they're stored as a LinkedList inside that bucket. Since Java 8, if a bucket has more than 8 entries, it converts to a Red-Black Tree for O(log n) access instead of O(n)."

**Follow-up: What if you put a mutable object as a HashMap key?**
> "It breaks. If you change the object after putting it in the map, its `hashCode()` changes, and you can never find it again. Always use immutable objects as HashMap keys — String, Integer, Long."

### equals() and hashCode() contract
> "If two objects are equal by `equals()`, they MUST have the same `hashCode()`. If you override `equals()`, you must override `hashCode()` too. In my User entity, I override both based on the `id` field — two User objects with the same id are the same user."

---

## Exception Handling

### Checked vs Unchecked
| | Checked | Unchecked |
|---|---|---|
| Extends | Exception | RuntimeException |
| Must handle? | Yes (compiler forces you) | No |
| Examples | IOException, SQLException | NullPointerException, IllegalArgumentException |

> "Checked exceptions are for recoverable conditions — file not found, network timeout. You're forced to handle them. Unchecked exceptions are programming errors — null pointer, array out of bounds. In my project, all custom exceptions extend RuntimeException so I don't need try-catch everywhere. They're caught once by `@ControllerAdvice` (GlobalExceptionHandler)."

### finally vs try-with-resources
```java
// Old way — must remember to close
Connection conn = null;
try {
    conn = dataSource.getConnection();
} finally {
    if (conn != null) conn.close();  // always runs
}

// Modern way — auto-closes anything implementing AutoCloseable
try (Connection conn = dataSource.getConnection()) {
    // conn.close() called automatically
}
```

---

## Multithreading Basics

### Thread vs Runnable
```java
// Option 1: extend Thread
class MyThread extends Thread {
    public void run() { System.out.println("running"); }
}

// Option 2: implement Runnable (preferred — doesn't waste inheritance)
class MyTask implements Runnable {
    public void run() { System.out.println("running"); }
}
new Thread(new MyTask()).start();
```

> "Prefer Runnable because Java has single inheritance. If you extend Thread, you can't extend anything else. Runnable is just a task — Thread is the worker. Separate the task from the runner."

### synchronized keyword
> "Ensures only one thread executes a block at a time. Uses the object's intrinsic lock. `synchronized(this)` locks on the current object. Two threads calling synchronized methods on the SAME object block each other. On DIFFERENT objects, they run concurrently."

### volatile keyword
> "Tells the JVM: don't cache this variable in CPU registers. Always read from main memory. Used for flags that multiple threads read/write. `volatile boolean running = true;` — if one thread sets it to false, all threads see the change immediately."

### Common interview question: Thread lifecycle
```
NEW → RUNNABLE → RUNNING → BLOCKED/WAITING → TERMINATED
```

---

## Java 8 Features (very important for modern interviews)

### Lambda expressions
```java
// Before Java 8:
list.sort(new Comparator<String>() {
    public int compare(String a, String b) { return a.compareTo(b); }
});

// Java 8:
list.sort((a, b) -> a.compareTo(b));
// Even shorter:
list.sort(String::compareTo);
```

### Stream API
```java
// Get names of active users, sorted, as a list
List<String> names = users.stream()
    .filter(u -> u.isActive())          // keep only active users
    .map(u -> u.getFullName())          // extract name
    .sorted()                           // alphabetical
    .collect(Collectors.toList());      // collect to list
```

**In my project:** `taskRepository.findByProject(project).stream().map(TaskResponse::fromEntity).collect(Collectors.toList())`

### Optional
```java
// Avoids NullPointerException
Optional<User> user = userRepository.findById(1L);
user.orElseThrow(() -> new ResourceNotFoundException("User not found"));
user.ifPresent(u -> System.out.println(u.getName()));
String name = user.map(User::getName).orElse("Unknown");
```

---

# SECTION 3 — MYSQL / SQL

## Joins — Draw this if they give you a whiteboard

```
Table: employees          Table: departments
id | name | dept_id       id | dept_name
---|------|--------       ---|----------
1  | Raja | 10            10 | Engineering
2  | Amit | 20            20 | Marketing
3  | Raj  | NULL          30 | HR (no employees)
```

| Join Type | Returns |
|-----------|---------|
| INNER JOIN | Only matching rows (Raja, Amit) |
| LEFT JOIN | All from left + matching right (Raja, Amit, Raj with NULL dept) |
| RIGHT JOIN | All from right + matching left (Raja, Amit, HR with NULL emp) |
| FULL OUTER JOIN | All rows from both sides (MySQL uses UNION for this) |

```sql
-- INNER JOIN: only employees with a department
SELECT e.name, d.dept_name
FROM employees e
INNER JOIN departments d ON e.dept_id = d.id;
-- Result: Raja-Engineering, Amit-Marketing

-- LEFT JOIN: all employees, even without department
SELECT e.name, d.dept_name
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id;
-- Result: Raja-Engineering, Amit-Marketing, Raj-NULL
```

---

## Aggregate Functions + GROUP BY

```sql
-- Count tasks per project
SELECT project_id, COUNT(*) AS task_count
FROM tasks
GROUP BY project_id;

-- Average, max, min
SELECT project_id,
       COUNT(*) AS total,
       AVG(estimated_hours) AS avg_hours,
       MAX(due_date) AS latest_due
FROM tasks
GROUP BY project_id
HAVING COUNT(*) > 5;  -- HAVING filters groups (WHERE filters rows)
```

**Remember:** `WHERE` filters BEFORE grouping. `HAVING` filters AFTER grouping.

---

## Subqueries

```sql
-- Find users who own at least one project
SELECT * FROM users
WHERE id IN (SELECT owner_id FROM projects);

-- Find projects with more than 5 tasks
SELECT * FROM projects
WHERE id IN (
    SELECT project_id FROM tasks
    GROUP BY project_id
    HAVING COUNT(*) > 5
);
```

---

## Indexes

> "An index is a sorted data structure (B-tree) that allows the database to find rows without scanning the entire table. Like a book's index — you jump to page 147 instead of reading every page.
>
> CREATE INDEX idx_tasks_assignee ON tasks(assignee_id);
>
> In my project, I added indexes on all foreign key columns — project_id, assignee_id, reporter_id — because PostgreSQL/MySQL doesn't auto-create indexes on FK columns. Without them, every 'WHERE assignee_id = ?' would scan the whole table."

**Trade-off:** Indexes slow down writes (INSERT/UPDATE/DELETE must update the index too). Only index columns in WHERE clauses.

---

## Transactions and ACID

| Property | Meaning | Example |
|----------|---------|---------|
| Atomicity | All or nothing | Transfer money: debit + credit both succeed or both fail |
| Consistency | DB goes from valid state to valid state | Balance can't go negative if constraint exists |
| Isolation | Concurrent transactions don't interfere | Two users updating same row — one waits |
| Durability | Committed data survives crashes | Written to disk, not just RAM |

---

## Common SQL Questions They Ask

**Find the 2nd highest salary:**
```sql
SELECT MAX(salary) FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);

-- Or with LIMIT:
SELECT salary FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;
```

**Find duplicate emails:**
```sql
SELECT email, COUNT(*) as count
FROM users
GROUP BY email
HAVING COUNT(*) > 1;
```

**Delete duplicate rows, keep only one:**
```sql
DELETE FROM users
WHERE id NOT IN (
    SELECT MIN(id) FROM users GROUP BY email
);
```

---

# SECTION 4 — SPRING BOOT

## What is Spring Boot and why use it?

> "Spring Boot is an opinionated wrapper around the Spring Framework that eliminates boilerplate configuration. Before Spring Boot, you needed hundreds of lines of XML to configure a web app. Spring Boot uses convention over configuration — sensible defaults, auto-configuration based on what's on the classpath. You add `spring-boot-starter-web` to pom.xml and you get Tomcat embedded, Jackson JSON, Spring MVC — all configured automatically."

## @RestController vs @Controller
> "`@Controller` returns a view name (HTML template for Thymeleaf/JSP). `@RestController` = `@Controller` + `@ResponseBody` — serializes the return value directly to JSON. For REST APIs, always use `@RestController`."

## @Autowired vs Constructor Injection
```java
// Option 1: @Autowired (not recommended)
@Autowired
private UserRepository userRepository;

// Option 2: Constructor injection (recommended, used in my project)
@RequiredArgsConstructor  // Lombok generates the constructor
public class UserService {
    private final UserRepository userRepository;  // final — can't be null
}
```
> "Constructor injection is preferred because the field is `final` (immutable), it makes dependencies explicit, and it's easier to test — you can just call `new UserService(mockRepo)` in tests."

## @Transactional
> "@Transactional starts a database transaction for the method. All DB operations inside it either all commit or all rollback together. It also keeps the Hibernate session open, which is needed for lazy loading. I use `readOnly = true` on GET methods — Hibernate skips dirty checking, slightly faster, and signals this method must not modify data."

## Bean Scopes
| Scope | Meaning | When to use |
|-------|---------|-------------|
| Singleton | One instance per app | Services, repositories (default) |
| Prototype | New instance per injection | Stateful beans |
| Request | One per HTTP request | Web: request-scoped data |
| Session | One per HTTP session | Web: session-scoped data |

> "Default scope is Singleton — one bean instance shared by all. This is why services must be stateless (no instance variables that hold request data)."

---

# SECTION 5 — DESIGN PATTERNS IN MY PROJECT

These are bonus points — mention these when talking about the project:

| Pattern | Where in TaskFlow | What it does |
|---------|-------------------|-------------|
| Repository | `ProjectRepository`, `TaskRepository` | Separates data access from business logic |
| Builder | `User.builder().email(...).build()` | Creates complex objects step by step |
| Factory | Spring's `ApplicationContext` | Creates beans — Spring IoC container |
| Observer | Spring Events (`TaskCreatedEvent`) | Decouple event publisher from listener |
| Proxy | Spring AOP (`@Auditable`) | Intercepts method calls for cross-cutting concerns |
| Strategy | `JwtAuthenticationFilter` | Pluggable auth strategy in Spring Security |
| Singleton | All `@Service`, `@Repository` beans | One instance, shared everywhere |

---

# SECTION 6 — QUESTIONS TO ASK THEM

Always ask 1-2 questions at the end. It shows interest.

1. "What does the day-to-day work look like for a new SDE1 joining the team?"
2. "What tech stack does the team primarily work with — is it more Spring Boot, or are there older Java EE / enterprise frameworks?"
3. "What does the onboarding process look like in the first 3 months?"

**Do NOT ask** about salary in the first round. Ask in HR round only.

---

# SECTION 7 — QUICK REVISION LIST (10 minutes before interview)

Read these aloud once:

- [ ] 4 pillars of OOP with one-line each
- [ ] ArrayList vs LinkedList vs HashMap
- [ ] INNER JOIN vs LEFT JOIN
- [ ] Checked vs Unchecked exception
- [ ] @Transactional — what it does and why
- [ ] N+1 problem — what it is, how I fixed it with @EntityGraph
- [ ] Redis — why it's faster than DB (RAM vs disk)
- [ ] ACID properties — one word each
- [ ] "Tell me about your project" — your 60-second pitch from Section 1
- [ ] Constructor injection vs @Autowired — which is better and why

---

# SECTION 8 — BE READY FOR THESE EXACT QUESTIONS

**"Tell me about yourself"**
> "I'm a 2026 BTech graduate. Over the last 6 months I've been learning Java and Spring Boot by building a full-stack project — a Jira-like task management system called TaskFlow. I've implemented JWT auth, PostgreSQL with JPA, Redis caching, payment integration with Razorpay, and scheduled jobs. I'm comfortable with REST APIs, relational databases, and the Spring Boot ecosystem."

**"What is your biggest weakness?"**
> "I'm still building experience with distributed systems and Kafka-based event-driven architecture — I've read about it but haven't deployed it in production. I'm actively working on it in my project." (honest + shows you know what you don't know + shows initiative)

**"Why Newgen Software?"**
> "Newgen works on enterprise software that large organizations use daily — BPM, document management. I want to work on software that solves real business problems at scale. The Java/Spring stack aligns with what I've been building, and I'm excited to work on a product used by actual enterprises."

**"Where do you see yourself in 3 years?"**
> "Growing as a backend engineer — understanding system design, handling scale, and eventually contributing to architecture decisions. I want to deepen my expertise in the Java ecosystem and distributed systems."

---

**Good luck tomorrow. You've built a real project with real concepts. Be confident.**
