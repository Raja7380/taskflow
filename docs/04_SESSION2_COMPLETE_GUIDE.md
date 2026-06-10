# Session 2 — Complete Learning Guide
## Projects & Tasks CRUD: JPA Relationships, @Transactional, @AuthenticationPrincipal

> Read this file top to bottom. Every concept you need for Session 2 is here.
> No prior knowledge assumed. Each concept is explained with real-world analogies FIRST,
> then shown in actual code from this project.

---

## TABLE OF CONTENTS

1. What We Built in Session 2 (Big Picture)
2. What is a Database Relationship?
3. @ManyToOne — The Most Common Relationship
4. @OneToMany — The Other Side
5. @ManyToMany — When Two Tables Need Each Other
6. FetchType: LAZY vs EAGER — Critical for Performance
7. CascadeType — The Domino Effect
8. @Transactional — The Most Important Annotation in Session 2
9. @AuthenticationPrincipal — Getting the Logged-In User
10. JPQL — Object-Oriented SQL Queries
11. How All the Files Connect (Full Flow)
12. The 3 Database Tables Hibernate Creates
13. Interview Questions & Answers
14. How to Test Everything in Swagger

---

## 1. WHAT WE BUILT IN SESSION 2 (BIG PICTURE)

Before diving into concepts, understand WHAT the code does:

```
A USER can:
  ✓ Create a PROJECT (they become the OWNER)
  ✓ Add other users as MEMBERS to their project
  ✓ Create TASKS inside a project
  ✓ Assign tasks to members
  ✓ Update task status (TODO → IN_PROGRESS → DONE)
  ✓ Delete their own projects/tasks
```

The real world version: Think of **Jira** or **Trello**.
- A company creates a Project called "Mobile App v2.0"
- The team lead adds developers as Members
- Developers create Tasks like "Fix login bug", "Add dark mode"
- Each task has an Assignee (who does the work) and Reporter (who logged it)
- Tasks move through TODO → IN_PROGRESS → IN_REVIEW → DONE

That's exactly what we built.

---

## 2. WHAT IS A DATABASE RELATIONSHIP?

Before learning `@ManyToOne`, you need to understand what a "relationship" means in databases.

### The Problem

You have two tables: `users` and `projects`.

Every project has an owner — a user who created it.

**BAD approach — storing everything in one table:**
```
projects table:
| id | name       | owner_name | owner_email     | owner_password |
|----|------------|------------|-----------------|----------------|
|  1 | Mobile App | Raja       | raja@gmail.com  | $2a$10$abc...  |
|  2 | Web API    | Raja       | raja@gmail.com  | $2a$10$abc...  |
|  3 | Dashboard  | Alice      | alice@gmail.com | $2a$10$xyz...  |
```

Problems:
- If Raja changes his email, you update it in 2 rows — easy to get out of sync
- You're storing Raja's password hash in the projects table — security nightmare
- What if Raja has 100 projects? You copy his data 100 times

**GOOD approach — separate tables + a foreign key:**
```
users table:                        projects table:
| id | name  | email           |    | id | name       | owner_id |
|----|-------|-----------------|    |----|------------|----------|
|  1 | Raja  | raja@gmail.com  |    |  1 | Mobile App |    1     |  ← Raja
|  2 | Alice | alice@gmail.com |    |  2 | Web API    |    1     |  ← Raja
                                    |  3 | Dashboard  |    2     |  ← Alice
```

`owner_id` is a **Foreign Key (FK)** — it's just a number that POINTS to a row in the users table.

This is called **normalization** — store each piece of information only once.

**A Foreign Key = a pointer from one table to another.**

When you have a foreign key column in your table, JPA calls that a `@ManyToOne` relationship.

---

## 3. @ManyToOne — THE MOST COMMON RELATIONSHIP

### The Analogy

Think of it like a **country and cities**:
- Many cities can belong to ONE country
- "Mumbai belongs to India" — Mumbai has a `country_id = India`
- "Delhi belongs to India" — Delhi has a `country_id = India`

"MANY cities → ONE country" = @ManyToOne

In TaskFlow:
- "MANY projects → ONE owner" = @ManyToOne owner
- "MANY tasks → ONE project" = @ManyToOne project
- "MANY tasks → ONE assignee" = @ManyToOne assignee

### In Database Terms

The `@ManyToOne` side is ALWAYS the one that has the foreign key column in its table.

```
projects table has: owner_id column (FK pointing to users.id)
tasks table has:    project_id column (FK pointing to projects.id)
tasks table has:    assignee_id column (FK pointing to users.id)
tasks table has:    reporter_id column (FK pointing to users.id)
```

### In Code: Project.java

```java
// Inside Project.java entity
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "owner_id", nullable = false)
private User owner;
```

Line by line:
- `@ManyToOne` = "Many projects belong to one User"
- `fetch = FetchType.LAZY` = "Don't load the User until I ask for it" (explained in section 6)
- `@JoinColumn(name = "owner_id")` = "Name the FK column 'owner_id' in the projects table"
- `nullable = false` = "owner_id cannot be NULL — every project MUST have an owner"
- `private User owner` = the field type is User (not Long, not String — the whole User object!)

**What Hibernate does with this:**
```sql
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    -- ...other fields...
    owner_id BIGINT NOT NULL REFERENCES users(id)  ← this column from @ManyToOne
);
```

### In Code: Task.java

```java
// Three @ManyToOne in Task.java:

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "project_id", nullable = false)
private Project project;        // Which project this task belongs to

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "assignee_id")   // No nullable=false → CAN be NULL
private User assignee;          // Who's doing the work (nullable)

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "reporter_id", nullable = false)
private User reporter;          // Who created/logged this task
```

Three foreign keys → three columns in the `tasks` table:
```sql
tasks.project_id  → references projects.id
tasks.assignee_id → references users.id   (can be NULL = unassigned)
tasks.reporter_id → references users.id
```

---

## 4. @OneToMany — THE OTHER SIDE

### The Analogy

If `@ManyToOne` is "from the city to the country",
then `@OneToMany` is "from the country to all its cities".

Same relationship, different perspective:
- From Project's perspective: "One project HAS MANY tasks"
- From Task's perspective: "Many tasks BELONG TO one project"

### The Key Rule: mappedBy

When you use `@OneToMany`, you MUST tell JPA:
> "Where is the FK column? I don't have it — the other table does."

That's what `mappedBy` does.

```java
// Inside Project.java
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, ...)
private List<Task> tasks = new ArrayList<>();
```

`mappedBy = "project"` means:
> "Look at the `project` field inside the `Task` class — THAT has the FK column."
> "I (@OneToMany) don't have a column. Task does."

**The rules:**
1. `@ManyToOne` side = **OWNING** side = has the FK column in its table
2. `@OneToMany` side = **INVERSE** side = uses `mappedBy`, no column
3. There is always exactly ONE `@JoinColumn` and it's ALWAYS on the `@ManyToOne` side

**Visual:**
```
PROJECT table                    TASKS table
| id | name       |              | id | title    | project_id |
|----|------------|              |----|----------|------------|
|  1 | Mobile App |  ←—————————— |  1 | Fix bug  |     1      |
                                 |  2 | Add auth |     1      |

The FK "project_id" lives in TASKS, not in PROJECT.
@OneToMany with mappedBy says: "That column in tasks is MY relationship."
```

### Why NOT put it in the Project table?

Because one project can have 100 tasks. Where would you store 100 IDs in the projects row?

That's why it's always: **the "many" side stores the FK**.

---

## 5. @ManyToMany — WHEN TWO TABLES NEED EACH OTHER

### The Analogy

Think of **students and courses**:
- One student can take MANY courses (Math, Science, English)
- One course can have MANY students

Neither the students table nor the courses table can store the other's IDs well.
(A student taking 10 courses would need 10 course_id columns in the students row — impossible.)

Solution: A **JOIN TABLE** (also called bridge table, junction table, linking table).

```
students table:              student_courses JOIN TABLE:      courses table:
| id | name  |               | student_id | course_id |       | id | name    |
|----|-------|               |------------|-----------|       |----|---------|
|  1 | Raja  |               |     1      |     101   |       |101 | Math    |
|  2 | Alice |               |     1      |     102   |       |102 | Science |
                             |     2      |     101   |       |103 | English |
                             |     2      |     103   |
```

In TaskFlow: A project can have MANY members, and a user can be a member of MANY projects.

### In Code: Project.java

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "project_members",                               // Name of the join table
    joinColumns = @JoinColumn(name = "project_id"),         // FK pointing to THIS table (Project)
    inverseJoinColumns = @JoinColumn(name = "user_id")      // FK pointing to OTHER table (User)
)
private Set<User> members = new HashSet<>();
```

What JPA creates in the database:
```sql
CREATE TABLE project_members (
    project_id BIGINT REFERENCES projects(id),
    user_id    BIGINT REFERENCES users(id),
    PRIMARY KEY (project_id, user_id)    -- same pair can't appear twice
);
```

**Why `Set<User>` not `List<User>`?**
- A Set does NOT allow duplicates
- A user can't be a member of the same project twice — makes logical sense
- List would allow the same user to appear 5 times — wrong behavior

**Why `HashSet` as default value?**
```java
@Builder.Default
private Set<User> members = new HashSet<>();
```
Because if `members` starts as `null` and you call `members.add(user)`, you get NullPointerException.
Starting with an empty Set is safe.

### Adding/Removing Members

When you call `project.addMember(user)`:
```java
members.add(user);
// Hibernate sees the Set changed
// On transaction commit: INSERT INTO project_members (project_id, user_id) VALUES (1, 5)
```

When you call `project.removeMember(user)`:
```java
members.remove(user);
// Hibernate sees the Set changed
// On transaction commit: DELETE FROM project_members WHERE project_id = 1 AND user_id = 5
```

No manual SQL. No `save()` call. Hibernate detects the change and writes the SQL automatically.

---

## 6. FETCHTYPE: LAZY vs EAGER — CRITICAL FOR PERFORMANCE

### The Analogy

Imagine you order a pizza. The restaurant calls you when it's ready.

**EAGER delivery:**
> The moment you ORDER, the restaurant sends you:
> - The pizza
> - A free salad you didn't ask for
> - A dessert menu you might not need
> - A copy of their entire menu
>
> You get EVERYTHING immediately, whether you needed it or not.

**LAZY delivery:**
> When you ORDER, you just get the pizza.
> If you later call and say "I want the salad too", they deliver it then.
> You only get what you ask for.

### In Java/JPA Terms

```java
// LAZY — don't load members until project.getMembers() is called
@ManyToMany(fetch = FetchType.LAZY)
private Set<User> members;

// EAGER — load members EVERY time you load a Project
@ManyToMany(fetch = FetchType.EAGER)  // ← NEVER do this for collections!
private Set<User> members;
```

**Why EAGER on collections is dangerous:**

```java
// This single line with EAGER:
projectRepository.findAll();

// Generates these SQL queries:
SELECT * FROM projects;                         -- loads 100 projects
SELECT * FROM users WHERE id IN (...);          -- loads all owners
SELECT * FROM project_members WHERE project_id = 1;   -- loads members for project 1
SELECT * FROM project_members WHERE project_id = 2;   -- loads members for project 2
... (100 more queries for each project)
SELECT * FROM tasks WHERE project_id = 1;       -- loads tasks for project 1
... (100 more queries for each project)

-- Total: 300+ SQL queries for ONE findAll() call!
```

This is called the **N+1 problem**. It's one of the most common performance bugs in Java.

**The rule: Always use `FetchType.LAZY` for collections (`@OneToMany`, `@ManyToMany`).**

With LAZY:
```java
projectRepository.findAll();
// Only runs: SELECT * FROM projects
// members and tasks are NOT loaded yet

// Only when you specifically call:
project.getMembers()  // THEN it runs: SELECT * FROM project_members WHERE project_id = ?
project.getTasks()    // THEN it runs: SELECT * FROM tasks WHERE project_id = ?
```

### Default Fetch Types (Important to Know!)

```
@ManyToOne  → default is EAGER  ← often fine, but watch for performance
@OneToOne   → default is EAGER
@OneToMany  → default is LAZY   ← already lazy, be explicit anyway
@ManyToMany → default is LAZY   ← already lazy, be explicit anyway
```

In our code, we wrote `fetch = FetchType.LAZY` everywhere to be explicit.

---

## 7. CASCADETYPE — THE DOMINO EFFECT

### The Analogy

Think of a **folder and files** on your computer.

When you **delete a folder**, what happens to the files inside?
- Option A: Files are also deleted (cascade delete)
- Option B: Files are orphaned and stay on disk

Java/JPA gives you control over this.

### In Code

```java
// In Project.java
@OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Task> tasks;
```

`cascade = CascadeType.ALL` means:
```
When I SAVE a project      → also save all new tasks in the list
When I DELETE a project    → also delete all tasks in the list
When I REFRESH a project   → also refresh all tasks
When I MERGE a project     → also merge all tasks
```

`orphanRemoval = true` means:
```java
project.getTasks().remove(task);  // Just removing from the Java list
// Hibernate detects this and ALSO deletes the task from DB
// Without orphanRemoval: the task stays in DB but with a broken project_id reference
```

### When to Use Which CascadeType

```
CascadeType.ALL     → tasks are "owned" by project, they can't exist without it
CascadeType.PERSIST → only cascade saves (not deletes)
CascadeType.MERGE   → only cascade updates
CascadeType.REMOVE  → only cascade deletes
(none)              → no cascade — handle child entities independently
```

**We used CascadeType.ALL on tasks** because:
- Tasks cannot exist without a project (they have `nullable = false` on project_id)
- Deleting a project should clean up all its tasks — that's logical behavior

**We did NOT use cascade on members** because:
- Users exist independently of projects
- Removing a user from a project's members should NOT delete the user account!
- We just want to remove the row from the `project_members` join table

---

## 8. @TRANSACTIONAL — THE MOST IMPORTANT ANNOTATION IN SESSION 2

### The Analogy

Think of **online banking**: Transfer ₹1000 from Account A to Account B.

This involves TWO operations:
1. Deduct ₹1000 from Account A
2. Add ₹1000 to Account B

What if the app crashes AFTER step 1 but BEFORE step 2?
- Account A lost ₹1000
- Account B never got ₹1000
- ₹1000 disappeared!

A **Transaction** solves this:
> Both operations are wrapped in a transaction.
> If EITHER fails, BOTH are rolled back — like they never happened.
> Only when BOTH succeed, the changes are committed to the database.

### The ACID Properties (Important Interview Topic)

```
A = Atomicity   → all operations succeed, or NONE of them do
C = Consistency → database goes from one valid state to another
I = Isolation   → transactions don't interfere with each other
D = Durability  → once committed, changes survive crashes/restarts
```

### In Spring Boot: @Transactional

```java
@Service
@Transactional  // ← applies to ALL methods in this class
public class ProjectService {

    public ProjectResponse updateProject(...) {
        Project project = projectRepository.findById(id)...;  // START of transaction
        project.setName(request.getName());                   // in-memory change only
        // No save() needed! Hibernate watches for changes.
        return ProjectResponse.fromEntity(project);           // END of method
        // When method returns → transaction COMMITS → Hibernate flushes changes to DB
        // If ANY exception → transaction ROLLS BACK → nothing saved to DB
    }
}
```

### Why @Transactional is REQUIRED for Lazy Loading

This is the part that confuses most beginners. Here's exactly what happens:

```
Hibernate Session = an open connection to the database.
Lazy loading requires an open session to run the SQL query.
@Transactional = keeps the session open for the duration of the method.
```

**Without @Transactional:**
```java
// Method starts → Hibernate session opens → query runs
Project project = projectRepository.findById(1L).get();

// Method returns? No, we're still in the same code.
// BUT: without @Transactional, Hibernate closes the session immediately after findById.

// Later in the same method:
project.getMembers();  // ← Tries to lazy-load members
// Session is CLOSED → LazyInitializationException: "could not initialize proxy – no Session"
// 💥 CRASH
```

**With @Transactional:**
```java
@Transactional
public ProjectResponse getProjectById(Long id) {
    Project project = projectRepository.findById(id).get();
    // Session is STILL OPEN (because @Transactional keeps it open)

    project.getMembers();    // ← Works! Session is open → runs SELECT
    project.getTasks();      // ← Works! Session is open → runs SELECT

    return ProjectResponse.fromEntity(project);
    // Method ends → @Transactional commits → session closes
}
```

### readOnly = true

```java
@Transactional(readOnly = true)
public List<ProjectResponse> getMyProjects(User user) { ... }
```

Use `readOnly = true` when the method only READS data (no INSERT/UPDATE/DELETE).

Benefits:
- Hibernate skips "dirty checking" (comparing every field to detect changes) — faster
- PostgreSQL can route to a read replica for better performance
- Signals intent: "this method MUST NOT modify data"

### Summary

```
@Transactional              = all DB operations succeed or all fail
@Transactional(readOnly=true) = read-only, slightly faster
Without @Transactional     = each repository call gets its own tiny session
                             = lazy loading will CRASH
```

---

## 9. @AUTHENTICATIONPRINCIPAL — GETTING THE LOGGED-IN USER

### The Problem It Solves

In many endpoints, you need to know WHO is making the request.

For example, when creating a project, the logged-in user should become the owner.

**The Wrong Way:**
```java
// DON'T DO THIS — ownerId comes from request body, can be FAKED by anyone!
public ResponseEntity<?> createProject(@RequestBody CreateProjectRequest req) {
    User owner = userRepo.findById(req.getOwnerId()); // anyone can send ownerId: 999
    // ...
}
```

**The Right Way:**
The user is identified by their JWT token, which is already validated. We read it from there.

### How the Logged-In User Gets into the JWT

```
1. User logs in → sends email + password
2. Server verifies credentials
3. Server creates JWT token: { "sub": "raja@gmail.com", "role": "USER", "exp": 1234567890 }
4. Server signs it with a secret key → sends to client
5. Client stores the token

6. Client makes any future request:
   Headers: { Authorization: "Bearer eyJhbGciOiJIUzI1NiJ9..." }

7. JwtAuthenticationFilter reads the header, validates the signature
8. Extracts "raja@gmail.com" from the token
9. Loads the User object from database using that email
10. Stores it in SecurityContextHolder (like a global variable for this request)

11. @AuthenticationPrincipal reads from SecurityContextHolder and injects it
```

### In Code: ProjectController.java

```java
@PostMapping
public ResponseEntity<ProjectResponse> createProject(
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal User currentUser    // ← Spring Security injects this!
) {
    // currentUser is the REAL logged-in user from the JWT token
    // Can't be faked — it comes from the validated token, not from the request body
    ProjectResponse response = projectService.createProject(request, currentUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

### Why Pass currentUser to the Service (Not Get It Inside Service)?

Two reasons:

**1. Testability:**
```java
// In a test, you can pass any user:
projectService.createProject(request, testUser);
// No need to mock the entire Spring Security context
```

**2. Separation of Concerns:**
```java
// Service should contain business logic, not security logic
// Controller handles: HTTP, authentication extraction
// Service handles: business rules, data operations
```

### What @AuthenticationPrincipal Does Internally

```java
// Long version (without annotation):
User currentUser = (User) SecurityContextHolder
                            .getContext()
                            .getAuthentication()
                            .getPrincipal();

// Short version (with annotation):
@AuthenticationPrincipal User currentUser
```

Both do exactly the same thing. The annotation is just a shortcut.

---

## 10. JPQL — OBJECT-ORIENTED SQL QUERIES

### Regular SQL vs JPQL

```sql
-- SQL (works with TABLE names and COLUMN names)
SELECT * FROM projects p 
JOIN project_members pm ON p.id = pm.project_id
WHERE p.owner_id = ? OR pm.user_id = ?

-- JPQL (works with ENTITY CLASS names and FIELD names)
SELECT DISTINCT p FROM Project p 
WHERE p.owner = :user OR :user MEMBER OF p.members
```

JPQL uses:
- `Project` = the Java class name (not the table name "projects")
- `p.owner` = the field name in Java (not "owner_id" column)
- `p.members` = the collection field in Java (not the join table)
- `:user` = named parameter (`:name` syntax)
- `MEMBER OF` = "is this element in the collection?" (handles the JOIN TABLE automatically)

### In Code: ProjectRepository.java

```java
@Query("SELECT DISTINCT p FROM Project p WHERE p.owner = :user OR :user MEMBER OF p.members")
List<Project> findByOwnerOrMember(@Param("user") User user);
```

Breaking it down:
- `SELECT DISTINCT p` = return Project objects, no duplicates
- `FROM Project p` = from the Project entity, alias = p
- `p.owner = :user` = where the owner field equals the user parameter
- `:user MEMBER OF p.members` = where user is in the members collection
- `@Param("user")` = links the `User user` parameter to the `:user` in the query

**Why can't Spring Data auto-derive this?**
```java
// Spring Data CAN auto-derive simple things:
List<Project> findByOwner(User owner);
// → WHERE owner_id = ?

// Spring Data CANNOT auto-derive:
List<Project> findByOwnerOrMember(User user);
// → This would try WHERE owner = ? OR member = ? (no "member" column exists!)
// → @ManyToMany relationship requires a JOIN query
// → Must write JPQL manually
```

### @Param Annotation

```java
@Query("... :user ...")
List<Project> findByOwnerOrMember(@Param("user") User user);
//                                 ↑ This links the Java parameter name
//                                   to the :user placeholder in the query
```

Without `@Param`, Spring can't match `User user` to `:user` in the query string.

---

## 11. HOW ALL THE FILES CONNECT (FULL FLOW)

Let's trace "Create a Project" from HTTP request to database and back:

```
HTTP Request:
POST /api/projects
Headers: Authorization: Bearer eyJhbGc...
Body: { "name": "My App", "priority": "HIGH" }

1. JwtAuthenticationFilter
   ├─ Reads "Bearer eyJhbGc..." from Authorization header
   ├─ Validates the signature
   ├─ Extracts email from token payload
   ├─ Calls: userDetailsService.loadUserByUsername("raja@gmail.com")
   │         → SELECT * FROM users WHERE email = 'raja@gmail.com'
   │         → Returns User object
   └─ Stores User in SecurityContextHolder

2. SecurityConfig
   └─ Is /api/projects in permitAll list? NO
   └─ Is user authenticated? YES (JwtFilter set it)
   └─ ALLOW request through → passes to controller

3. ProjectController.createProject()
   ├─ @AuthenticationPrincipal User currentUser
   │   → Spring reads User from SecurityContextHolder
   │   → currentUser = Raja (loaded from DB in step 1)
   ├─ @Valid @RequestBody CreateProjectRequest request
   │   → Deserializes JSON body into CreateProjectRequest object
   │   → Validates @NotBlank on name → "My App" passes ✓
   └─ Calls: projectService.createProject(request, currentUser)

4. ProjectService.createProject()
   ├─ @Transactional: Hibernate session opens, transaction begins
   ├─ Builds Project entity:
   │     Project.builder()
   │       .name("My App")
   │       .priority(HIGH)
   │       .owner(currentUser)    ← owner = Raja from JWT, not from request body!
   │       .build()
   ├─ projectRepository.save(project)
   │     → Hibernate generates:
   │        INSERT INTO projects (name, priority, status, owner_id, created_at, updated_at)
   │        VALUES ('My App', 'HIGH', 'PLANNING', 1, now(), now())
   │        RETURNING id   → id = 7
   │     → project.id is now set to 7
   ├─ ProjectResponse.fromEntity(savedProject)
   │     → Extracts fields, calls project.getOwner().getFullName()
   │       (owner is already in memory — not a new DB query)
   │     → Returns ProjectResponse DTO
   └─ @Transactional: transaction commits, session closes

5. ProjectController
   └─ Returns: ResponseEntity.status(201).body(projectResponse)

6. HTTP Response:
   Status: 201 Created
   Body: {
     "id": 7,
     "name": "My App",
     "priority": "HIGH",
     "status": "PLANNING",
     "ownerName": "Raja Singh",
     "ownerEmail": "raja@gmail.com",
     "memberCount": 0,
     "taskCount": 0,
     "createdAt": "2026-06-10T..."
   }
```

---

## 12. THE 3 DATABASE TABLES HIBERNATE CREATES

When you start the app (`ddl-auto: update`), Hibernate reads your entities and creates these tables:

### Table 1: projects

```sql
CREATE TABLE projects (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    description     VARCHAR(2000),
    status          VARCHAR(50) NOT NULL DEFAULT 'PLANNING',
    priority        VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    start_date      DATE,
    target_end_date DATE,
    actual_end_date DATE,
    owner_id        BIGINT NOT NULL REFERENCES users(id),   -- @ManyToOne owner
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP
);
```

### Table 2: project_members (JOIN TABLE)

```sql
CREATE TABLE project_members (
    project_id BIGINT REFERENCES projects(id),
    user_id    BIGINT REFERENCES users(id),
    PRIMARY KEY (project_id, user_id)    -- unique pair: same user can't be added twice
);
```

This table is created by `@JoinTable` in `Project.java`. It has NO entity class — it's managed entirely by Hibernate.

### Table 3: tasks

```sql
CREATE TABLE tasks (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(300) NOT NULL,
    description      VARCHAR(5000),
    status           VARCHAR(50) NOT NULL DEFAULT 'TODO',
    priority         VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    due_date         DATE,
    estimated_hours  INTEGER,
    actual_hours     INTEGER,
    project_id       BIGINT NOT NULL REFERENCES projects(id),   -- @ManyToOne project
    assignee_id      BIGINT REFERENCES users(id),               -- @ManyToOne assignee (nullable)
    reporter_id      BIGINT NOT NULL REFERENCES users(id),      -- @ManyToOne reporter
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP
);
```

### Relationship Summary

```
users ←──────────── projects (owner_id FK)
users ←────────┐
projects ←─────┴── project_members (user_id FK + project_id FK)
projects ←──────── tasks (project_id FK)
users ←──────────── tasks (assignee_id FK, nullable)
users ←──────────── tasks (reporter_id FK)
```

---

## 13. INTERVIEW QUESTIONS & ANSWERS

These are the exact questions you'll get in Java interviews. Practice saying these out loud.

---

**Q: What is a JPA relationship? What types exist?**

A: JPA relationships map foreign key constraints in the database to object references in Java.
There are four types:
- `@ManyToOne`: the most common. Puts a FK column in the "many" side table.
- `@OneToMany`: the inverse of @ManyToOne. Uses `mappedBy` to point to the FK.
- `@ManyToMany`: creates a separate join table. Neither entity stores the other's FK.
- `@OneToOne`: similar to @ManyToOne but the FK must be unique.

---

**Q: What is the difference between the owning side and the inverse side of a relationship?**

A: The owning side is the entity that physically has the foreign key column in its database table.
In `@ManyToOne`, the entity with `@JoinColumn` is the owner.
The inverse side uses `mappedBy` to say "the other entity owns this relationship."
Only the owning side's changes are persisted — modifications to the inverse side's collections are ignored unless you also update the owning side.

---

**Q: What is FetchType.LAZY? Why should you always use it for collections?**

A: LAZY means Hibernate doesn't load the related data until you explicitly call the getter.
EAGER loads everything immediately when the parent is loaded.
Collections (OneToMany, ManyToMany) should always be LAZY because:
1. You rarely need all related data in every operation
2. EAGER on a large collection triggers hundreds of SQL queries (N+1 problem)
3. LAZY lets you load only what you need, when you need it

---

**Q: What is the N+1 problem?**

A: If you load N parent records (e.g., 100 projects) and each parent eagerly loads its children (members), Hibernate runs 1 query for projects + 100 queries for each project's members = 101 queries. The fix is LAZY loading and using JOIN FETCH in queries when you need the data.

---

**Q: What is @Transactional and why is it needed for lazy loading?**

A: @Transactional wraps a method in a database transaction — a unit of work where all operations either succeed together or fail together (atomicity). For lazy loading, it's needed because Hibernate keeps the database session open for the duration of the transaction. Without @Transactional, the session closes immediately after each repository call, and any subsequent lazy-load attempt throws LazyInitializationException.

---

**Q: What is CascadeType.ALL and when should you use it?**

A: Cascade means "when I perform an operation on the parent, perform the same on the children."
ALL = cascade save, update, delete, refresh, merge.
Use it when child entities cannot exist without the parent (tasks cannot exist without a project).
Do NOT use it for @ManyToMany relationships where the related entity has independent lifecycle (users exist independently of projects — cascading delete from project would delete user accounts!).

---

**Q: What is @AuthenticationPrincipal?**

A: It's a Spring Security annotation that injects the currently authenticated user from the SecurityContextHolder into a controller method parameter. Instead of manually writing `(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()`, you just annotate the parameter with `@AuthenticationPrincipal`. The user is set by the JWT authentication filter earlier in the request chain.

---

**Q: What is JPQL and how is it different from SQL?**

A: JPQL (Java Persistence Query Language) operates on entity class names and field names instead of table and column names. Hibernate translates JPQL to SQL at runtime. This makes queries database-agnostic — the same JPQL works on PostgreSQL, MySQL, and Oracle without changes. Example: `SELECT p FROM Project p WHERE p.owner = :user` translates to `SELECT * FROM projects WHERE owner_id = ?`.

---

**Q: What is dirty checking in Hibernate?**

A: Dirty checking is Hibernate's mechanism to detect changes to managed entities and automatically generate UPDATE SQL without calling `save()`. When a `@Transactional` method loads an entity, Hibernate takes a snapshot of its state. When the transaction commits, Hibernate compares the current state to the snapshot. If any field changed ("dirty"), Hibernate generates an UPDATE statement. This is why we can write `project.setName("New Name")` without calling `projectRepository.save(project)`.

---

## 14. HOW TO TEST EVERYTHING IN SWAGGER

### Step 1: Make sure PostgreSQL is running

Your app needs PostgreSQL. Start it if it's not running:
- Open pgAdmin → verify the `taskflow` database exists
- Or check Windows Services: look for "postgresql-x64-xx"

### Step 2: Start the app

In IntelliJ: click the green Run button (or right-click TaskFlowApplication → Run)

You should see in console:
```
Hibernate: create table if not exists projects (...)
Hibernate: create table if not exists project_members (...)
Hibernate: create table if not exists tasks (...)
Started TaskFlowApplication in 3.2 seconds
```

### Step 3: Open Swagger

Go to: http://localhost:8080/swagger-ui.html

You'll see 3 sections: Auth, Projects, Tasks

### Step 4: Register and Login

1. Click `POST /api/auth/register`
2. Click "Try it out"
3. Enter:
   ```json
   { "fullName": "Raja Singh", "email": "raja@gmail.com", "password": "password123" }
   ```
4. Click Execute → 201 Created
5. Now click `POST /api/auth/login`
6. Enter:
   ```json
   { "email": "raja@gmail.com", "password": "password123" }
   ```
7. Copy the `accessToken` from the response

### Step 5: Authorize in Swagger

1. Click the **Authorize** button (top right, looks like a lock)
2. In the Value field, type: `Bearer ` (with a space after) then paste your token
3. Click Authorize → Close

Now all padlock icons turn closed. You're authenticated for all future requests.

### Step 6: Test Projects

**Create a project:**
```json
POST /api/projects
{
  "name": "TaskFlow App",
  "description": "My first project",
  "priority": "HIGH"
}
```
Response: 201 Created — note the `id` in the response (e.g., id: 1)

**Get your projects:**
```
GET /api/projects
```
Response: Array with your project. Notice `memberCount: 0`, `taskCount: 0`.

**Register a second user, get their id, add them as member:**
```
POST /api/projects/1/members/2
```
Response: Project with `memberCount: 1`

### Step 7: Test Tasks

**Create a task in your project:**
```json
POST /api/tasks
{
  "title": "Fix login bug",
  "description": "The login form doesn't validate email properly",
  "priority": "HIGH",
  "projectId": 1
}
```
Response: 201 Created — note the `id` (e.g., id: 1)

**Now check your project again:**
```
GET /api/projects/1
```
Notice `taskCount` is now 1!

**Move task to IN_PROGRESS:**
```json
PUT /api/tasks/1
{
  "status": "IN_PROGRESS"
}
```

**Get all tasks in your project:**
```
GET /api/tasks/project/1
```

**Get YOUR assigned tasks (My Tasks dashboard):**
```
GET /api/tasks/my
```
(Empty if you didn't assign tasks to yourself. Assign first: `PUT /api/tasks/1` with `"assigneeId": 1`)

### Step 8: Test Authorization (Try to Break Things)

**Try deleting another user's project:**
Register User2, login as User2, create a project (id: 2).
Now login as User1 and try: `DELETE /api/projects/2`
Expected: 403 Forbidden — "You don't have permission to delete this project"

**Try updating a task you're not the assignee/reporter/owner of:**
As User1, try: `PUT /api/tasks/1` (a task in User2's project)
Expected: 403 Forbidden

This confirms the authorization logic works correctly.

---

## WHAT YOU LEARNED IN SESSION 2 — QUICK RECAP

```
CONCEPT                  ANNOTATION            WHAT IT DOES
───────────────────────────────────────────────────────────────────
Foreign key in my table  @ManyToOne            many rows point to one parent
No FK, other side has it @OneToMany mappedBy   read the children collection
Separate join table      @ManyToMany @JoinTable link two independent entities
Don't load until needed  FetchType.LAZY         prevents N+1 queries
Cascade to children      CascadeType.ALL        save/delete parent = save/delete children
DB transaction + session @Transactional         enables lazy loading, atomicity
Current logged-in user   @AuthenticationPrincipal inject user from JWT context
Object-oriented SQL      @Query (JPQL)          query using Java class/field names
Auto-detect changes      dirty checking         no save() needed after setField()
```

---

> **Next: Session 3** — File Upload with AWS S3 (or Spring AI integration)
> Read this guide again whenever you're confused about any relationship or annotation.
> The concepts in this guide appear in 90% of Java backend interviews.
