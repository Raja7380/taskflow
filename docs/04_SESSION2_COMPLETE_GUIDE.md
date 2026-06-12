# Session 2 — Complete Learning Guide
### For someone who knows NOTHING. No shortcuts. Full industry understanding.

> The goal of this guide: After reading this, you should be able to walk into any company,
> see a Spring Boot codebase you've never seen, and understand what every annotation does
> and WHY it exists. Not just "how we use it in TaskFlow" — but WHY it exists in the world.

---

# PART 1: THE PROBLEM THAT CREATED EVERYTHING IN SESSION 2

Before learning ANY annotation, understand the problem that forced engineers to invent all this.

## The problem: Real apps have RELATED data

Imagine you're building Twitter from scratch. You have users and tweets.

A tweet belongs to a user. A user can have many tweets.

**How do you store this?**

---

### Attempt 1: Store everything in one table (the naive way)

```
tweets table:
| tweet_id | tweet_text        | user_name | user_email      | user_phone   |
|----------|-------------------|-----------|-----------------|--------------|
| 1        | "Hello world"     | Elon Musk | elon@x.com      | +1-555-0001  |
| 2        | "X is the future" | Elon Musk | elon@x.com      | +1-555-0001  |
| 3        | "I love coding"   | Elon Musk | elon@x.com      | +1-555-0001  |
| 4        | "Good morning"    | Jeff Bezos| jeff@amazon.com | +1-555-0002  |
```

Elon posts 3 tweets → his data is repeated 3 times.

**What happens when Elon changes his email?**
→ You need to find and update ALL 3 rows. What if you miss one? Now the data is inconsistent.

**What if Elon has 10,000 tweets?**
→ His email is stored 10,000 times. 10,000 places to update.

**What if a new developer writes a bug that only updates 5,000 rows?**
→ Your database now has TWO different emails for the same person. Which is real?

This is called **data anomaly** — one of the worst things that can happen in a database.

---

### Attempt 2: Two separate tables with a LINK (the correct way)

Engineers in the 1970s (at IBM) invented the solution: **Relational Databases**.

```
users table:                            tweets table:
| user_id | name       | email       |  | tweet_id | text              | user_id |
|---------|------------|-------------|  |----------|-------------------|---------|
| 1       | Elon Musk  | elon@x.com  |  | 1        | "Hello world"     | 1       |
| 2       | Jeff Bezos | jeff@amz.com|  | 2        | "X is the future" | 1       |
                                        | 3        | "I love coding"   | 1       |
                                        | 4        | "Good morning"    | 2       |
```

Elon's information is stored ONCE in `users`. His `user_id = 1`.

Each tweet just stores `user_id = 1` — a reference (pointer) to that one row.

**Now Elon changes his email:**
→ Update ONE row in the `users` table. All 10,000 tweets automatically point to the updated data.

That column `user_id` in the tweets table that points to the users table is called a **FOREIGN KEY**.

This is the ENTIRE foundation of relational databases. Everything in Session 2 — every annotation, every concept — exists to make working with these foreign keys easy in Java.

---

# PART 2: HOW JAVA + JPA + HIBERNATE SOLVES THIS

The problem: foreign keys are a database concept. Java doesn't know about them.

In Java, you have objects. In a database, you have rows with IDs.

**The mismatch:**
```java
// In Java, you work with objects:
Tweet tweet = new Tweet();
tweet.text = "Hello world";
tweet.author = elonMuskObject;  // direct object reference

// In database, you work with IDs:
-- INSERT INTO tweets (text, user_id) VALUES ('Hello world', 1)
-- No "elonMuskObject" — just the number 1
```

You're building in Java, but you need to store in a database. Someone has to translate between these two worlds.

That "someone" is **Hibernate** (via the **JPA** standard).

**JPA** (Java Persistence API) = A specification. A set of rules that says "here's how Java should talk to databases."

**Hibernate** = The most popular implementation of JPA. The actual code that does the translation.

When you write `@ManyToOne` on a field, you're telling Hibernate: "This Java object reference should become a foreign key column in the database."

Hibernate then:
1. Reads your Java classes
2. Creates the SQL tables
3. Translates every `save()`, `findById()`, `delete()` into SQL
4. Handles the object ↔ row translation automatically

You write Java. Hibernate writes SQL. This is the entire purpose of JPA/Hibernate.

---

# PART 3: THE THREE RELATIONSHIP TYPES

Every relationship between two database tables fits into one of three categories. You will see these in every project you ever work on.

## 3.1 — @ManyToOne: "Many rows point to one row"

**Real industry examples:**
- Many orders belong to one customer (Amazon)
- Many comments belong to one post (Instagram)
- Many transactions belong to one bank account (HDFC)
- Many employees belong to one department (any company)
- Many tasks belong to one project (Jira / our TaskFlow)

**The key question to identify @ManyToOne:**
> "Can the SAME [other thing] appear in multiple rows of MY table?"

- Can the same customer appear in multiple orders? YES → orders has @ManyToOne to customers
- Can the same post appear in multiple comments? YES → comments has @ManyToOne to posts
- Can the same project appear in multiple tasks? YES → tasks has @ManyToOne to projects

**What it creates in the database:**
A foreign key column in YOUR table (the "many" side).

```
orders table:           ← this is the "many" side
| order_id | amount | customer_id |   ← customer_id is the FK
|----------|--------|-------------|
| 1        | 5000   | 42          |   ← customer 42 ordered
| 2        | 1200   | 42          |   ← customer 42 ordered again
| 3        | 8000   | 17          |   ← customer 17 ordered

customers table:        ← this is the "one" side
| customer_id | name  |
|-------------|-------|
| 42          | Raja  |
| 17          | Alice |
```

**How you write it in Java:**

```java
// Inside Order.java (the "many" side — orders has the FK column)
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "customer_id")   // ← name of the FK column in orders table
private Customer customer;           // ← direct object reference in Java
```

Hibernate translates:
- `order.getCustomer()` → `SELECT * FROM customers WHERE customer_id = 42`
- Saving an order → `INSERT INTO orders (amount, customer_id) VALUES (5000, 42)`

**In our TaskFlow project — all 4 @ManyToOne usages:**

```java
// Project.java
@ManyToOne
@JoinColumn(name = "owner_id")
private User owner;          // Many projects → one owner

// Task.java
@ManyToOne
@JoinColumn(name = "project_id")
private Project project;     // Many tasks → one project

@ManyToOne
@JoinColumn(name = "assignee_id")
private User assignee;       // Many tasks → one assignee (nullable)

@ManyToOne
@JoinColumn(name = "reporter_id")
private User reporter;       // Many tasks → one reporter
```

---

## 3.2 — @OneToMany: "One row has many rows in another table"

This is NOT a new relationship. It's the same relationship as @ManyToOne — just viewed from the opposite direction.

**@ManyToOne:** From Task's perspective: "My project is [one thing]"
**@OneToMany:** From Project's perspective: "My tasks are [many things]"

Same foreign key. Same database table. Just two ways to look at it.

**The critical thing about @OneToMany: `mappedBy`**

When you put @OneToMany on a field, you're NOT creating a new column. The column already exists on the other side (@ManyToOne side).

You're just saying: "I want to be able to access my related objects from this side too."

```java
// Project.java
@OneToMany(mappedBy = "project")   // ← "project" = the field name in Task.java that has @ManyToOne
private List<Task> tasks;
```

`mappedBy = "project"` means:
> "Don't create a new column. Use the existing `project_id` column that Task already has."

**Why does this matter?**

Without `mappedBy`, Hibernate thinks you want a NEW relationship and creates a second join table. Your database would have duplicate data and confusing structure.

Rule:
- The `@ManyToOne` side → has `@JoinColumn` → owns the FK column
- The `@OneToMany` side → has `mappedBy` → reads from the existing FK column

**Real industry example:** Amazon's Order and OrderItem

```java
// OrderItem.java (the "many" side — has the FK column order_id)
@ManyToOne
@JoinColumn(name = "order_id")
private Order order;

// Order.java (the "one" side — no new column, reads from order_id in items table)
@OneToMany(mappedBy = "order")
private List<OrderItem> items;
```

---

## 3.3 — @ManyToMany: "Many rows connect to many rows in another table"

**The problem that creates @ManyToMany:**

Think of YouTube. A video can have many tags (Java, Programming, Tutorial). A tag can apply to many videos.

- "Java Tutorial" video: tags = [Java, Programming, Tutorial]
- "Java Interview Prep" video: tags = [Java, Interview, Programming]
- "Python Basics" video: tags = [Python, Programming, Tutorial]

One video → many tags. One tag → many videos. This is @ManyToMany.

**Why you can't use a simple FK column:**

```
videos table — can't store multiple tags in one column:
| video_id | title          | tag_id | ← WRONG: one column can only hold ONE value
|----------|----------------|--------|
| 1        | Java Tutorial  | ???    | ← which tag? there are 3!
```

Solution: A separate table that stores PAIRS of IDs.

```
video_tags table (the JOIN TABLE):
| video_id | tag_id |
|----------|--------|
| 1        | 10     |   ← Video 1 has Tag "Java"
| 1        | 20     |   ← Video 1 has Tag "Programming"
| 1        | 30     |   ← Video 1 has Tag "Tutorial"
| 2        | 10     |   ← Video 2 has Tag "Java"
| 2        | 20     |   ← Video 2 also has Tag "Programming"
```

This join table has NO entity class in Java. Hibernate manages it automatically based on your @ManyToMany annotation.

**How you write it:**

```java
// Video.java
@ManyToMany
@JoinTable(
    name = "video_tags",                           // name of the join table
    joinColumns = @JoinColumn(name = "video_id"),  // FK pointing to THIS table
    inverseJoinColumns = @JoinColumn(name = "tag_id") // FK pointing to OTHER table
)
private Set<Tag> tags;
```

**In our TaskFlow — projects and members:**

```java
// Project.java
@ManyToMany
@JoinTable(
    name = "project_members",
    joinColumns = @JoinColumn(name = "project_id"),
    inverseJoinColumns = @JoinColumn(name = "user_id")
)
private Set<User> members;
```

Hibernate creates:
```sql
CREATE TABLE project_members (
    project_id BIGINT REFERENCES projects(id),
    user_id    BIGINT REFERENCES users(id),
    PRIMARY KEY (project_id, user_id)
);
```

**Why `Set<User>` and not `List<User>`?**

A Set in Java doesn't allow duplicates. The same user can't be added to the same project twice.

If you used a List, you could accidentally insert the same user twice → two rows in project_members with the same (project_id, user_id) pair → bad data.

The database PRIMARY KEY on (project_id, user_id) also enforces uniqueness, but using Set in Java enforces it at the application level before it even hits the database.

**Real industry examples of @ManyToMany:**
- Students and Courses (university systems)
- Products and Categories (Amazon, Flipkart)
- Users and Roles (permissions systems)
- Actors and Movies (IMDB-style apps)
- Recipes and Ingredients (food apps)

---

# PART 4: FETCHTYPE — THE MOST MISUNDERSTOOD CONCEPT

## Why this concept exists

When you load a `Project` from the database, what should Java automatically also load?

- Load the `owner` (User)? Maybe — you almost always need the owner's name.
- Load all `tasks`? Maybe — but a project could have 10,000 tasks. Loading all of them every time would be catastrophically slow.
- Load all `members`? Maybe — but you don't need members for every operation.

This is a real performance problem that every company faces. Facebook's news feed was notoriously slow for years because of eager loading. Netflix engineers spend significant time tuning what gets loaded when.

Hibernate gives you control with `FetchType`.

## FetchType.EAGER — "Load immediately, always"

```java
@OneToMany(fetch = FetchType.EAGER)
private List<Task> tasks;
```

Every time you load a Project → Hibernate ALSO runs a SELECT to load all tasks immediately.

You didn't ask for tasks. You don't need tasks. But Hibernate loads them anyway.

**When EAGER causes a disaster — The N+1 Problem:**

Imagine an e-commerce site. You want to show 20 products on a page.

```java
List<Product> products = productRepository.findAll();  // 20 products
```

With EAGER loading on `reviews` field:
- 1 query for products
- 20 queries (one per product) to load each product's reviews
- Total: 21 queries for ONE page load

Now imagine 1000 concurrent users. Each page load = 21 queries.
1000 users × 21 queries = 21,000 database queries per second.
Your database server crashes.

This actually happened to companies early in JPA's popularity. It's called the **N+1 problem** (N products = N extra queries).

**The rule: NEVER use EAGER on collections (@OneToMany, @ManyToMany).**

## FetchType.LAZY — "Load only when I actually ask for it"

```java
@OneToMany(fetch = FetchType.LAZY)
private List<Task> tasks;
```

```java
Project project = projectRepository.findById(1L).get();
// SQL: SELECT * FROM projects WHERE id = 1
// tasks are NOT loaded yet. Just a placeholder (proxy) sits there.

// Later, only when you need them:
List<Task> tasks = project.getTasks();
// NOW SQL runs: SELECT * FROM tasks WHERE project_id = 1
```

You only pay the cost of loading data when you actually use it.

**Default fetch types (important to memorize):**

```
@ManyToOne  → EAGER by default  (loading one row is usually fast, acceptable)
@OneToOne   → EAGER by default
@OneToMany  → LAZY by default   (collections can be huge — lazy is safer)
@ManyToMany → LAZY by default
```

Even though @OneToMany is LAZY by default, you should always write it explicitly (`fetch = FetchType.LAZY`) to make your intent clear to any developer reading the code.

---

# PART 5: CASCADETYPE — "WHEN I DO SOMETHING, DO IT TO CHILDREN TOO"

## Why this concept exists

In real applications, some entities "own" other entities. If you delete a blog post, should all its comments be deleted too? Usually yes — comments can't exist without their post.

But if you delete a user, should all their orders be deleted? NO — the order history needs to stay for legal and accounting reasons, even if the account is closed.

CascadeType gives you control over this.

## Types of Cascade (from most to least common)

```
CascadeType.PERSIST  → When I save the parent, also save new children
CascadeType.MERGE    → When I update the parent, also update changed children  
CascadeType.REMOVE   → When I delete the parent, also delete all children
CascadeType.REFRESH  → When I refresh the parent, also refresh children
CascadeType.ALL      → All of the above
```

**In our TaskFlow:**

```java
// Project.java
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Task> tasks;
```

`cascade = CascadeType.ALL` because:
- Tasks are "owned" by Project
- A task cannot exist without a project (project_id is NOT NULL)
- Delete a project → makes sense to delete all its tasks

`orphanRemoval = true`:
```java
project.getTasks().remove(specificTask);  // just removing from Java list
// With orphanRemoval: also deletes that task from the database
// Without orphanRemoval: just removes it from the in-memory list, task stays in DB
```

**We do NOT cascade on members:**

```java
// Project.java
@ManyToMany  // no cascade
private Set<User> members;
```

Why? Because User entities live independently. If you delete a project:
- You want to remove the MEMBERSHIP (the row in project_members table) ✓
- You do NOT want to delete the User accounts ✗

Hibernate automatically handles the join table rows when you call `project.getMembers().remove(user)`. No cascade needed.

## Real industry example: Blog post and comments

```java
// BlogPost.java
@OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Comment> comments;
```

Delete a blog post → automatically deletes all its comments. Correct behavior.

```java
// Order.java — Amazon keeps order history even if user deletes account
@OneToMany(mappedBy = "order")  // no cascade
private List<OrderItem> items;  // items stay, they're for accounting records
```

---

# PART 6: @TRANSACTIONAL — THE MOST IMPORTANT CONCEPT IN BACKEND DEVELOPMENT

## Why this concept exists — A real banking story

It's 1980. A bank's system needs to transfer ₹10,000 from Account A to Account B.

The code does two operations:
1. Deduct ₹10,000 from Account A
2. Add ₹10,000 to Account B

The server crashes between step 1 and step 2.

Account A lost ₹10,000. Account B never received it. ₹10,000 vanished.

Customers sued. Banks lost millions. This was a real problem in early computing.

**The solution invented by database engineers: Transactions.**

A transaction is a group of operations that must ALL succeed or ALL fail together. There is no partial success.

```
BEGIN TRANSACTION;
  UPDATE accounts SET balance = balance - 10000 WHERE id = 'A';  -- step 1
  UPDATE accounts SET balance = balance + 10000 WHERE id = 'B';  -- step 2
COMMIT;   -- Both succeed → save to disk permanently
-- OR
ROLLBACK; -- Any failure → undo EVERYTHING, as if nothing happened
```

If the server crashes after step 1: the transaction is incomplete → automatically rolled back → Account A's ₹10,000 is restored → no money lost.

This concept is so fundamental that it's in every serious database and backend system in the world. Amazon, Paytm, GPay, HDFC — every transaction you've ever made was wrapped in a database transaction.

## ACID Properties — The Guarantee of Transactions

Every database transaction guarantees these 4 properties. This is a core interview topic.

```
A = Atomicity   → "All or nothing"
                   Either ALL operations in the transaction succeed,
                   or NONE of them are saved. No partial success.

C = Consistency → "The database goes from one valid state to another"
                   Before: Account A has ₹10,000, Account B has ₹5,000
                   After:  Account A has ₹0, Account B has ₹15,000
                   Total money (₹15,000) is same. Consistency maintained.
                   A transaction that would break this rule is rejected.

I = Isolation   → "Transactions don't interfere with each other"
                   If 1000 users are transferring money simultaneously,
                   each transaction sees a consistent snapshot of the database.
                   No transaction sees half-completed work of another.

D = Durability  → "Once committed, it's permanent"
                   Even if the server crashes 1 second after COMMIT,
                   the data is on disk. It will be there when server restarts.
```

## @Transactional in Spring — How it works

In Spring Boot, you don't write `BEGIN TRANSACTION` / `COMMIT` / `ROLLBACK` manually.

You just add `@Transactional` to a method. Spring handles the rest.

```java
@Service
public class BankService {

    @Transactional  // ← Spring automatically begins a transaction here
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        Account from = accountRepo.findById(fromId).get();
        Account to   = accountRepo.findById(toId).get();

        from.setBalance(from.getBalance().subtract(amount));  // step 1
        to.setBalance(to.getBalance().add(amount));           // step 2

        // No save() needed — Hibernate's dirty checking detects the changes
    }
    // Method ends → Spring automatically COMMITS the transaction
    // Any exception → Spring automatically ROLLBACKS the transaction
}
```

**What Spring does behind the scenes:**

```
@Transactional method starts
    ↓
Spring tells JDBC: "BEGIN TRANSACTION"
    ↓
Your code runs (findById, setBalance, etc.)
    ↓
Method returns normally:
    Spring tells JDBC: "COMMIT" → changes permanently saved
Method throws any RuntimeException:
    Spring tells JDBC: "ROLLBACK" → changes undone, DB unchanged
```

You write business logic. Spring handles transaction management. This is a core benefit of Spring.

## The Second (Hidden) Reason @Transactional is Needed: Lazy Loading

This surprises most beginners. @Transactional is not just for atomicity — it's also required for lazy loading to work.

**Understanding Hibernate's Session:**

Hibernate maintains a "Session" — an open conversation with the database.

The Session:
- Is where SQL queries get sent
- Is where loaded entities live ("managed" entities)
- Is where dirty checking happens
- Is where lazy loading can occur (because it can send new queries)

When the Session closes, all these abilities disappear. Trying to lazy-load after Session closes = crash.

```
@Transactional method:
    Session OPENS ←──────────────────────────────────────┐
    │                                                      │
    │  findById() → runs SQL → loads Project              │
    │  project.getOwner() → lazy loads User (Session open)│
    │  project.getTasks() → lazy loads Tasks (Session open)│
    │                                                      │
    Session CLOSES ──────────────────────────────────────→

Without @Transactional:
    findById() → brief Session opens → loads Project → Session CLOSES
    project.getTasks() → Session is CLOSED → 💥 LazyInitializationException
```

**Real consequence:** Every Spring Boot beginner eventually hits this error:

```
org.hibernate.LazyInitializationException:
  could not initialize proxy – no Session
```

The fix is always: add `@Transactional` to the method that needs the lazy data.

**readOnly = true:**

```java
@Transactional(readOnly = true)
public List<ProjectResponse> getMyProjects(User user) { ... }
```

When you ONLY read data (no INSERT/UPDATE/DELETE), add `readOnly = true`.

Why:
1. Hibernate skips "dirty checking" (comparing entity state before/after) → 10-20% faster
2. PostgreSQL can route the query to a read replica (a separate server just for reading)
3. Makes your intent explicit to other developers: "this method must not write anything"

**Company practice:** At companies like Flipkart, Swiggy, Zomato — read-heavy operations like "get all orders", "search products" use `readOnly = true` transactions routed to read replicas to handle millions of reads per minute without overloading the primary database.

## @Transactional and the "Dirty Checking" magic

One of Hibernate's most important features: if you change a managed entity inside a transaction, you don't need to call `save()`.

```java
@Transactional
public void updateProjectName(Long id, String newName) {
    Project project = projectRepository.findById(id).get();
    // project is now a "managed entity" — Hibernate is watching it

    project.setName(newName);
    // No projectRepository.save(project) needed!
    // Hibernate takes a snapshot when you load an entity.
    // When transaction commits, it compares current state to snapshot.
    // Difference detected ("dirty") → automatically generates UPDATE SQL.
}
// Transaction commits → UPDATE projects SET name = ? WHERE id = ?
```

This is called **Automatic Dirty Checking**.

Why it exists: Imagine a complex method that changes 20 fields on 5 different entities. Calling `save()` on each one would be tedious and error-prone. Dirty checking lets Hibernate collect all changes and flush them efficiently at the end.

---

# PART 7: @AUTHENTICATIONPRINCIPAL — WHO IS MAKING THIS REQUEST?

## The problem it solves

Every API call is made by a specific user. Most operations need to know WHO is calling:

- "Create project" → who becomes the owner?
- "Delete task" → is this person authorized?
- "Get my tasks" → which user's tasks to return?

The user's identity is in their JWT token (set during login). You need a clean way to get that User object into your controller methods.

## How the user's identity travels from token to your controller

This is a 7-step journey for every API request:

```
Step 1: Client sends request
  POST /api/projects
  Headers: { Authorization: "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyYWphQGdtYWlsLmNvbSJ9.signature" }

Step 2: JwtAuthenticationFilter intercepts (before controller)
  - Reads the Authorization header
  - Extracts the token
  - Validates: is signature valid? is it expired?
  - If invalid: returns 401 Unauthorized immediately, controller never runs

Step 3: If valid, extract user identity from token
  - Token payload (middle part) contains: { "sub": "raja@gmail.com" }
  - "sub" = subject = who this token belongs to

Step 4: Load the full User object from database
  - userDetailsService.loadUserByUsername("raja@gmail.com")
  - Runs: SELECT * FROM users WHERE email = 'raja@gmail.com'
  - Returns: the full User entity with id, name, role, etc.

Step 5: Store in SecurityContextHolder
  - SecurityContextHolder.getContext().setAuthentication(auth)
  - This is like a global variable for the current request thread
  - Any code in this request can access it

Step 6: Request reaches your controller
  - Spring Security allows it (user is authenticated)

Step 7: @AuthenticationPrincipal extracts from SecurityContextHolder
  - Reading from that "global variable" set in Step 5
  - Injects the User object directly into your method parameter
```

## The annotation in code:

```java
// ProjectController.java
@PostMapping
public ResponseEntity<ProjectResponse> createProject(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal User currentUser  // ← Spring injects this automatically
) {
    // currentUser is REAL. It came from the validated JWT token.
    // Cannot be faked by sending "userId: 999" in the request body.
    projectService.createProject(request, currentUser);
}
```

**What it does internally (the annotation is just a shortcut):**
```java
// This one annotation replaces this entire block of boilerplate:
User currentUser = (User) SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .getPrincipal();
```

## Why we pass currentUser to the service (not get it inside the service)

**Option A (what we do):** Controller extracts user, passes to service.
```java
// Controller:
projectService.createProject(request, currentUser);

// Service:
public ProjectResponse createProject(CreateProjectRequest req, User currentUser) { ... }
```

**Option B:** Service gets user from SecurityContextHolder itself.
```java
// Service (bad practice):
public ProjectResponse createProject(CreateProjectRequest req) {
    User currentUser = (User) SecurityContextHolder.getContext()...  // hidden dependency
}
```

Why Option A is better:
1. **Testability:** In tests, pass any User object. No need to mock SecurityContextHolder.
2. **Clarity:** Anyone reading the service method signature knows it needs a User.
3. **Separation of concerns:** HTTP/security is the controller's job. Business logic is the service's job.

This is the design principle used at companies like Google, Meta, Amazon — controllers handle HTTP concerns, services handle business logic.

---

# PART 8: JPQL — QUERYING OBJECTS, NOT TABLES

## Why JPQL exists

When JPA was invented, a problem arose: how do you write queries?

SQL works with table names and column names:
```sql
SELECT * FROM projects WHERE owner_id = 5
```

But JPA works with Java class names and field names. The table might be named "projects" but the class is "Project". The column might be "owner_id" but the Java field is "owner".

If you wrote raw SQL in your JPA code, you'd be tightly coupled to the database schema. Rename a column? Update SQL in 50 places. Change databases? Rewrite all queries.

**JPQL (Java Persistence Query Language) = SQL for your Java objects.**

```sql
-- SQL (database language)
SELECT p.* FROM projects p WHERE p.owner_id = ?

-- JPQL (Java object language)
SELECT p FROM Project p WHERE p.owner = :user
```

JPQL uses:
- `Project` = Java class name (not "projects" table)
- `p.owner` = Java field name (not "owner_id" column)
- `:user` = named parameter (the User object itself, not its ID)

Hibernate translates JPQL → SQL for whatever database you're using.

## How it looks in code:

```java
// ProjectRepository.java
@Query("SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
List<Project> findByOwnerOrMember(@Param("user") User user);
```

Breaking down this JPQL:

```
SELECT DISTINCT p  →  Return Project objects, no duplicates
                       (DISTINCT because a user who is BOTH owner and member would appear twice)

FROM Project p     →  From the Project entity class, alias it as "p"

WHERE p.owner = :user  →  Where the owner field equals the :user parameter
                           Hibernate translates: WHERE owner_id = ?

OR :user MEMBER OF p.members  →  OR the user is in the members collection
                                   Hibernate translates: OR EXISTS (
                                     SELECT 1 FROM project_members
                                     WHERE project_id = p.id AND user_id = ?
                                   )
```

**Why can't Spring Data auto-derive this from the method name?**

Spring Data JPA can auto-generate queries from method names for simple cases:

```java
List<Project> findByOwner(User owner);
// Works! Generated: WHERE owner_id = ?

List<Project> findByStatus(ProjectStatus status);
// Works! Generated: WHERE status = ?

List<Project> findByOwnerAndStatus(User owner, ProjectStatus status);
// Works! Generated: WHERE owner_id = ? AND status = ?
```

But for complex queries involving @ManyToMany relationships (join tables), Spring Data can't auto-derive:

```java
List<Project> findByOwnerOrMember(User user);
// FAILS: "member" is a @ManyToMany collection, not a simple column
// Spring doesn't know how to generate the EXISTS subquery automatically
```

So you write it yourself with `@Query`. The more complex the query, the more you need @Query with JPQL (or native SQL).

**`@Param("user")` — linking Java parameter to JPQL placeholder:**

```java
@Query("... :user ...")               // ← :user is the placeholder
List<Project> findByOwnerOrMember(@Param("user") User user);
//                                 ↑ links method parameter "user" to placeholder ":user"
```

Without `@Param`, Hibernate can't figure out which Java parameter maps to which JPQL placeholder.

---

# PART 9: THE FULL PICTURE — HOW ALL FILES CONNECT

Let's trace the complete journey of **"User creates a project"** through every layer of the application.

```
═══════════════════════════════════════════════════════════════
CLIENT (Postman / Browser / React Frontend)
═══════════════════════════════════════════════════════════════
│
│  POST /api/projects
│  Headers: Authorization: Bearer eyJhbG...
│  Body: { "name": "My Startup App", "priority": "HIGH" }
│
▼
═══════════════════════════════════════════════════════════════
JwtAuthenticationFilter.java  (runs BEFORE controller)
═══════════════════════════════════════════════════════════════
│
│  1. Reads "Authorization" header
│  2. Strips "Bearer " prefix, gets the token
│  3. Calls jwtService.isTokenValid(token) → checks signature + expiry
│  4. Extracts email from token: "raja@gmail.com"
│  5. Calls userDetailsService.loadUserByUsername("raja@gmail.com")
│         → SQL: SELECT * FROM users WHERE email = 'raja@gmail.com'
│         → Returns: User{id=1, fullName="Raja Singh", role=USER, ...}
│  6. Stores in SecurityContextHolder for this request thread
│
▼
═══════════════════════════════════════════════════════════════
SecurityConfig.java  (checks: is this URL allowed?)
═══════════════════════════════════════════════════════════════
│
│  Is /api/projects in the permitAll list? NO
│  Is user authenticated? YES (filter set it)
│  → ALLOW the request through
│
▼
═══════════════════════════════════════════════════════════════
ProjectController.java — createProject()
═══════════════════════════════════════════════════════════════
│
│  @AuthenticationPrincipal User currentUser
│    → reads from SecurityContextHolder
│    → currentUser = User{id=1, fullName="Raja Singh", ...}
│
│  @Valid @RequestBody CreateProjectRequest request
│    → JSON { "name": "My Startup App", "priority": "HIGH" }
│      is deserialized into CreateProjectRequest object
│    → @NotBlank validation on name passes ✓
│    → @Valid sends it to service
│
│  calls: projectService.createProject(request, currentUser)
│
▼
═══════════════════════════════════════════════════════════════
ProjectService.java — createProject()  [@Transactional]
═══════════════════════════════════════════════════════════════
│
│  @Transactional → Hibernate Session OPENS, transaction BEGINS
│
│  Builds the entity:
│    Project project = Project.builder()
│        .name("My Startup App")
│        .priority(HIGH)
│        .status(PLANNING)     ← default from @Builder.Default
│        .owner(currentUser)   ← from JWT, not from request body!
│        .members(new HashSet<>())  ← empty
│        .tasks(new ArrayList<>()) ← empty
│        .build();
│
│  projectRepository.save(project)
│    → Hibernate generates SQL:
│       INSERT INTO projects
│         (name, description, status, priority, owner_id, created_at, updated_at)
│       VALUES
│         ('My Startup App', NULL, 'PLANNING', 'HIGH', 1, now(), now())
│    → Database returns: id = 7 (auto-generated)
│    → project.id is now 7
│
│  ProjectResponse.fromEntity(savedProject)
│    → Builds response DTO from entity
│    → project.getOwner() → owner was set in builder, already in memory (no SQL)
│    → project.getOwner().getFullName() → "Raja Singh"
│    → project.getMembers().size() → 0
│    → project.getTasks().size() → 0
│    → Returns: ProjectResponse{id=7, name="My Startup App", ownerName="Raja Singh", ...}
│
│  @Transactional → method returns normally → transaction COMMITS → Session CLOSES
│
▼
═══════════════════════════════════════════════════════════════
ProjectController.java
═══════════════════════════════════════════════════════════════
│
│  return ResponseEntity.status(201).body(projectResponse)
│
▼
═══════════════════════════════════════════════════════════════
CLIENT receives:
═══════════════════════════════════════════════════════════════

HTTP 201 Created
{
  "id": 7,
  "name": "My Startup App",
  "status": "PLANNING",
  "priority": "HIGH",
  "ownerId": 1,
  "ownerName": "Raja Singh",
  "ownerEmail": "raja@gmail.com",
  "memberCount": 0,
  "taskCount": 0,
  "createdAt": "2026-06-10T15:30:00"
}
```

---

# PART 10: THE DATABASE TABLES HIBERNATE CREATES

When you start the app with `ddl-auto: update`, Hibernate reads your 3 entity classes and creates these 3 tables (in addition to the users table from Session 1):

## Table: projects

Created by `Project.java`:
```sql
CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,         -- @GeneratedValue IDENTITY
    name            VARCHAR(200) NOT NULL,          -- @Column(nullable=false, length=200)
    description     VARCHAR(2000),                  -- @Column(length=2000), nullable
    status          VARCHAR(50) NOT NULL,           -- @Enumerated(STRING)
    priority        VARCHAR(50) NOT NULL,           -- @Enumerated(STRING)
    start_date      DATE,                           -- LocalDate
    target_end_date DATE,                           -- LocalDate
    actual_end_date DATE,                           -- LocalDate, set when COMPLETED
    owner_id        BIGINT NOT NULL                 -- @ManyToOne + @JoinColumn
                    REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL,             -- @PrePersist
    updated_at      TIMESTAMP                       -- @PreUpdate
);
```

## Table: project_members (Join Table — no entity class)

Created by `@JoinTable` in `Project.java`:
```sql
CREATE TABLE project_members (
    project_id BIGINT NOT NULL REFERENCES projects(id),   -- joinColumns
    user_id    BIGINT NOT NULL REFERENCES users(id),       -- inverseJoinColumns
    PRIMARY KEY (project_id, user_id)                      -- unique pair
);
```

This table has NO Java class. Hibernate manages it entirely.

## Table: tasks

Created by `Task.java`:
```sql
CREATE TABLE tasks (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(300) NOT NULL,
    description     VARCHAR(5000),
    status          VARCHAR(50) NOT NULL,            -- TaskStatus enum
    priority        VARCHAR(50) NOT NULL,            -- Priority enum
    due_date        DATE,
    estimated_hours INTEGER,
    actual_hours    INTEGER,
    project_id      BIGINT NOT NULL                  -- @ManyToOne project
                    REFERENCES projects(id),
    assignee_id     BIGINT                           -- @ManyToOne assignee (NO nullable=false)
                    REFERENCES users(id),            -- NULL = task is unassigned
    reporter_id     BIGINT NOT NULL                  -- @ManyToOne reporter
                    REFERENCES users(id),
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP
);
```

---

# PART 11: EVERY ENDPOINT — WHAT IT DOES, WHAT SQL IT RUNS

## Project Endpoints

```
POST /api/projects
  What: Creates a project. Logged-in user becomes owner.
  SQL:  INSERT INTO projects (name, priority, status, owner_id, ...) VALUES (...)

GET /api/projects
  What: Returns all projects where I am owner OR member.
  SQL:  SELECT DISTINCT p FROM projects p
        WHERE p.owner_id = ? OR EXISTS (
          SELECT 1 FROM project_members WHERE project_id = p.id AND user_id = ?
        )

GET /api/projects/{id}
  What: Returns one project by ID.
  SQL:  SELECT * FROM projects WHERE id = ?

PUT /api/projects/{id}
  What: Updates a project. Only owner can do this.
  SQL:  UPDATE projects SET name = ?, status = ? ... WHERE id = ?
  Auth: if (not owner) → 403 Forbidden

DELETE /api/projects/{id}
  What: Deletes project + ALL its tasks (CascadeType.ALL).
  SQL:  DELETE FROM tasks WHERE project_id = ?
        DELETE FROM project_members WHERE project_id = ?
        DELETE FROM projects WHERE id = ?
  Auth: if (not owner) → 403 Forbidden

POST /api/projects/{id}/members/{userId}
  What: Adds a user as project member.
  SQL:  INSERT INTO project_members (project_id, user_id) VALUES (?, ?)
  Auth: if (not owner) → 403 Forbidden

DELETE /api/projects/{id}/members/{userId}
  What: Removes a user from project members.
  SQL:  DELETE FROM project_members WHERE project_id = ? AND user_id = ?
  Auth: if (not owner) → 403 Forbidden
```

## Task Endpoints

```
POST /api/tasks
  What: Creates a task. Logged-in user becomes reporter. projectId in body.
  SQL:  INSERT INTO tasks (title, status, project_id, reporter_id, ...) VALUES (...)

GET /api/tasks/my
  What: All tasks assigned to ME.
  SQL:  SELECT * FROM tasks WHERE assignee_id = ? ORDER BY due_date ASC

GET /api/tasks/{id}
  What: One task by ID.
  SQL:  SELECT * FROM tasks WHERE id = ?

GET /api/tasks/project/{projectId}
  What: All tasks in a project.
  SQL:  SELECT * FROM tasks WHERE project_id = ? ORDER BY created_at DESC

PUT /api/tasks/{id}
  What: Update a task. Assignee OR reporter OR project owner can update.
  SQL:  UPDATE tasks SET status = ?, priority = ? ... WHERE id = ?
  Auth: only assignee, reporter, or project owner

DELETE /api/tasks/{id}
  What: Delete a task. Only project owner can delete.
  SQL:  DELETE FROM tasks WHERE id = ?
  Auth: only project owner
```

---

# PART 12: INTERVIEW QUESTIONS — ANSWER THESE OUT LOUD

Practice saying these answers. The words matter.

---

**Q: What is the difference between @ManyToOne and @OneToMany?**

A: Both represent the same relationship — they're two sides of the same coin.

@ManyToOne is on the entity that has the foreign key column in its database table. For example, Task has @ManyToOne to Project, so the tasks table has a project_id column.

@OneToMany is on the other side — the entity that doesn't have the FK. It uses `mappedBy` to point to the field on the owning side that has the @JoinColumn. @OneToMany doesn't create any new column — it just lets you navigate from the "one" side to the "many" side in Java.

The owning side (with @JoinColumn) is the only side that actually writes to the database. Changes to the inverse side (mappedBy) are ignored by Hibernate unless you also update the owning side.

---

**Q: What is the N+1 problem and how do you prevent it?**

A: The N+1 problem occurs when loading N parent records triggers N additional queries to load each parent's children — resulting in N+1 total queries instead of one efficient join query.

For example: Load 20 projects (1 query), then with EAGER loading on tasks, Hibernate runs 20 more queries — one per project — to load each project's tasks. Total: 21 queries.

Prevention:
1. Use FetchType.LAZY on all collections (default for @OneToMany/@ManyToMany)
2. When you DO need the related data, use JOIN FETCH in JPQL: `SELECT p FROM Project p JOIN FETCH p.tasks WHERE ...`
3. For large-scale systems: use projection queries that only select specific fields
4. Tools like Hibernate statistics or P6Spy can detect N+1 in development

---

**Q: What does @Transactional do?**

A: @Transactional wraps a method in a database transaction. When the method starts, Spring opens a transaction. When the method returns normally, Spring commits the transaction — all changes are permanently saved. If the method throws a RuntimeException, Spring rolls back the transaction — all changes are undone.

Beyond atomicity, @Transactional is also required for lazy loading to work. Hibernate uses a Session to communicate with the database, and the Session stays open for the duration of a @Transactional method. Without @Transactional, the Session closes after each repository call, and any lazy loading attempt after that throws LazyInitializationException.

The `readOnly = true` variant skips dirty checking (improving performance by ~10-20%) and signals that the method must not modify data. In systems with read replicas, readOnly transactions can be routed to a read-only database server.

---

**Q: What is dirty checking in Hibernate?**

A: Dirty checking is Hibernate's mechanism to automatically detect changes to managed entities and generate UPDATE SQL without you calling `save()`.

When you load an entity inside a @Transactional method, Hibernate takes a "snapshot" of all its field values. When the transaction commits, Hibernate compares the current state of each managed entity to its snapshot. If any field changed ("the entity is dirty"), Hibernate generates an UPDATE statement for those fields automatically.

This means `entity.setName("New Name")` inside a @Transactional method automatically becomes `UPDATE ... SET name = 'New Name' WHERE id = ?` at commit time, without any `repository.save(entity)` call needed.

---

**Q: What is @AuthenticationPrincipal?**

A: @AuthenticationPrincipal is a Spring Security annotation that injects the currently authenticated user into a controller method parameter. It reads from Spring's SecurityContextHolder, which was populated earlier by the JWT authentication filter.

Without @AuthenticationPrincipal, you'd write: `(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()` — verbose boilerplate repeated in every controller method. The annotation removes that boilerplate.

The user object comes from the validated JWT token, not from the request body. This makes it impossible for a client to fake their identity by sending a different userId in the request body.

---

**Q: What is JPQL? How is it different from SQL?**

A: JPQL is the Java Persistence Query Language — a query language that works with entity class names and field names instead of database table and column names. Hibernate translates JPQL to the appropriate SQL for whatever database is configured.

SQL: `SELECT * FROM projects WHERE owner_id = ?`
JPQL: `SELECT p FROM Project p WHERE p.owner = :user`

JPQL benefits: database-agnostic (same query works on PostgreSQL, MySQL, Oracle), works with object relationships (can use `p.owner` instead of joining tables), and lets you use Java object references as parameters instead of raw IDs.

When Spring Data's derived query method names aren't powerful enough (complex joins, OR conditions with relationships), you use @Query with JPQL.

---

**Q: What is a join table? Why is it needed for @ManyToMany?**

A: A join table is a separate database table that stores only foreign keys connecting two other tables. It's needed for @ManyToMany because neither table can store the other's IDs as a single column — one entity can relate to many of the other, so there's no fixed number of FK columns.

For example, a project can have 10 members, and a user can be in 5 projects. Neither the projects table nor the users table can hold the other's multiple IDs in a single column. The project_members join table stores one row per (project, member) pair, making any number of relationships possible.

In JPA, @JoinTable defines the join table name and the two FK column names. Hibernate creates and manages this table automatically — you never write entity class for it.

---

**Q: What is CascadeType.ALL and when should you NOT use it?**

A: CascadeType.ALL means that save, update, delete, refresh, and merge operations on the parent entity are automatically propagated to its children. If you delete a project with CascadeType.ALL on tasks, all tasks in that project are automatically deleted.

Use CascadeType.ALL when the child entity's existence depends entirely on the parent — tasks cannot exist without a project.

Do NOT use it in @ManyToMany relationships where the related entity has independent lifecycle. In our TaskFlow, Project has @ManyToMany to User for members. If we added cascade, deleting a project would delete all its members' user accounts — a catastrophic bug. The User entity exists independently and should never be deleted by project operations.

---

# PART 13: HOW TO TEST — SWAGGER STEP BY STEP

## Prerequisites

1. PostgreSQL is running (check Windows Services or pgAdmin)
2. Database `taskflow` exists in PostgreSQL
3. Password in `application.yml` matches your postgres user's actual password

## Start the App

In IntelliJ: Run `TaskFlowApplication.java`

Watch for in the console:
```
Hibernate: create table if not exists projects (...)
Hibernate: create table if not exists project_members (...)
Hibernate: create table if not exists tasks (...)
Started TaskFlowApplication in 3.4 seconds
```

Open: `http://localhost:8080/swagger-ui.html`

## Step 1: Create User 1 (you)

```
POST /api/auth/register
{
  "fullName": "Raja Singh",
  "email": "raja@gmail.com",
  "password": "password123"
}
```
→ 201 Created ✓

## Step 2: Login as User 1

```
POST /api/auth/login
{
  "email": "raja@gmail.com",
  "password": "password123"
}
```
→ Copy the `accessToken` from response

## Step 3: Authorize in Swagger

Click **Authorize** button (top right) → Type `Bearer ` + paste your token → Click Authorize

## Step 4: Create a Project

```
POST /api/projects
{
  "name": "My First Project",
  "description": "Learning Spring Boot",
  "priority": "HIGH"
}
```
→ 201 Created. Note the `id` (e.g., 1)

## Step 5: Create Tasks in the Project

```
POST /api/tasks
{
  "title": "Setup the database",
  "description": "Configure PostgreSQL and create tables",
  "priority": "HIGH",
  "projectId": 1
}
```
→ 201 Created. Note the task `id` (e.g., 1)

```
POST /api/tasks
{
  "title": "Build login API",
  "priority": "MEDIUM",
  "projectId": 1,
  "estimatedHours": 4
}
```

## Step 6: Check the project now has tasks

```
GET /api/projects/1
```
→ See `taskCount: 2` ← this proves the @OneToMany relationship works

## Step 7: Move a task to IN_PROGRESS

```
PUT /api/tasks/1
{
  "status": "IN_PROGRESS"
}
```
→ See `status: "IN_PROGRESS"` in response

## Step 8: Create a Second User and Add as Member

Open a new browser tab or use Postman:

```
POST /api/auth/register
{
  "fullName": "Alice Developer",
  "email": "alice@gmail.com",
  "password": "password123"
}
```
Note the user id from the response (e.g., 2)

Now (logged in as Raja):
```
POST /api/projects/1/members/2
```
→ See `memberCount: 1` in response ← @ManyToMany working

## Step 9: Test Authorization — Try to Break Things

Create a new project as Alice (login as Alice first), get its id (e.g., project id: 2).

Now login as Raja and try:
```
DELETE /api/projects/2
```
→ 403 Forbidden: "You don't have permission to delete this project. Only the project owner can do this."

This proves the authorization logic works.

---

# QUICK REFERENCE — ALL SESSION 2 ANNOTATIONS

```
ANNOTATION                    │ WHERE USED         │ WHAT IT DOES
──────────────────────────────┼────────────────────┼───────────────────────────────
@ManyToOne                    │ Entity field        │ FK column in MY table
@JoinColumn(name="col")       │ With @ManyToOne     │ Names the FK column
@OneToMany(mappedBy="field")  │ Entity field        │ Navigate to children (no column)
@ManyToMany                   │ Entity field        │ Separate join table
@JoinTable(name="...",...)    │ With @ManyToMany    │ Defines the join table
FetchType.LAZY                │ Any relationship    │ Don't load until I call getter
FetchType.EAGER               │ Any relationship    │ Load immediately (avoid on collections)
CascadeType.ALL               │ @OneToMany          │ Save/delete propagates to children
orphanRemoval = true          │ @OneToMany          │ Remove from list = delete from DB
@Transactional                │ Service class       │ Atomicity + keeps Session open
@Transactional(readOnly=true) │ Service read methods│ Faster reads, no dirty checking
@AuthenticationPrincipal      │ Controller param    │ Injects current user from JWT
@Query("JPQL...")             │ Repository method   │ Custom JPQL query
@Param("name")                │ Repository method   │ Links Java param to JPQL placeholder
@Builder.Default              │ Entity field        │ Default value when using builder
```

---

> **Session 3 covers:** File uploads (AWS S3) or Spring AI integration.
>
> **Before moving on, make sure you can answer:**
> - What is a foreign key and why does it exist?
> - What is the difference between @ManyToOne and @OneToMany?
> - Why should collections always use FetchType.LAZY?
> - What is the N+1 problem?
> - What does @Transactional do beyond just atomicity?
> - What is dirty checking?
> - How does @AuthenticationPrincipal get the logged-in user?
>
> If any answer felt unclear — re-read that section. These are the exact questions
> you will be asked in Java backend interviews at every company.
