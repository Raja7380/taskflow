# Session 3 — Complete Learning Guide
### State Machines, JPA Specifications (Dynamic Filtering), and Pagination

> Same promise as before: start from WHY it was invented.
> Real industry context. Zero assumed knowledge.

---

# PART 1: WHAT WE BUILT IN SESSION 3

Three independent things, each solving a different real-world problem:

```
1. STATE MACHINE
   Problem: Users can set a task status to anything — DONE → TODO in one click.
   Fix:     Define exactly which transitions are allowed. Invalid ones get rejected.
   Result:  Tasks follow a logical workflow, just like Jira.

2. JPA SPECIFICATIONS (Dynamic Filtering)
   Problem: Search APIs have many optional filters. Without Specifications,
            you'd need 2^N repository methods (N = number of filters).
   Fix:     Build queries dynamically by composing small reusable conditions.
   Result:  GET /api/tasks/search?status=IN_PROGRESS&priority=HIGH&keyword=bug
            — any combination works with one piece of code.

3. PAGINATION
   Problem: A database with 50,000 tasks — returning all of them at once would
            crash the app and the client.
   Fix:     Return data in pages. Client asks for "page 0, 20 items."
   Result:  Fast responses regardless of how much data is in the database.
```

---

# PART 2: STATE MACHINE — THE CONCEPT

## Why state machines exist

Every object in your system goes through states. A task isn't just "exists" or "doesn't exist."
It starts as TODO, someone picks it up (IN_PROGRESS), submits for review (IN_REVIEW), and finally it's DONE.

The problem: **without rules, users can do anything.**

A developer finishes a task and marks it DONE. Two minutes later they realize they made a mistake. Should they be allowed to go back to IN_PROGRESS? **YES.**

A manager accidentally clicks DONE on a task that was just created. Should they be allowed to go from TODO directly to DONE without any work? In most systems: **NO.** It bypasses the review process.

A cancelled task — should anyone be able to restart it? In most companies: **NO.** Create a new task instead. Cancelled is final.

Without rules, all of these are possible. With a state machine, you define exactly what's allowed.

## State machines in the real world

**Traffic lights:**
- GREEN → YELLOW → RED → GREEN (one allowed path)
- GREEN → RED directly: not allowed (drivers would get no warning)

**ATM:**
- IDLE → CARD_INSERTED → PIN_ENTERED → TRANSACTION → IDLE
- You can't jump from IDLE directly to TRANSACTION (no card inserted!)

**Order on Amazon/Swiggy:**
- PLACED → CONFIRMED → PACKED → SHIPPED → DELIVERED
- DELIVERED → PLACED: not allowed (order is done, can't restart)
- CANCELLED is a terminal state from any step

**Jira (which we're building a clone of):**
- TODO → IN_PROGRESS → IN_REVIEW → DONE
- DONE tasks: in Jira you can reopen them, but many companies disable this
- The exact rules are configurable per workflow

**Bank transaction:**
- PENDING → COMPLETED or PENDING → FAILED
- COMPLETED → PENDING: never. Money was sent. You can't "unsend" money.

## Our TaskFlow state machine

```
                    ┌─────────────────────┐
                    ↓                     │
      ┌──────────────────────┐            │
      │         TODO         │            │
      └──────────────────────┘            │
              │                           │
              ↓                           │
      ┌──────────────────────┐            │
      │     IN_PROGRESS      │ ←──────────┤ (reviewer sends back)
      └──────────────────────┘            │
              │                           │
              ↓                           │
      ┌──────────────────────┐            │
      │      IN_REVIEW       │ ───────────┘
      └──────────────────────┘
              │
              ↓
      ┌──────────────────────┐
      │         DONE         │  ← terminal (no outgoing arrows)
      └──────────────────────┘

From any non-terminal state:
      ─────────────────────→ CANCELLED  ← terminal (no outgoing arrows)
```

**Allowed transitions table:**
```
FROM          │  CAN GO TO
──────────────┼─────────────────────────────────────
TODO          │  IN_PROGRESS, CANCELLED
IN_PROGRESS   │  IN_REVIEW, TODO, CANCELLED
IN_REVIEW     │  DONE, IN_PROGRESS, CANCELLED
DONE          │  (nothing — terminal)
CANCELLED     │  (nothing — terminal)
```

**NOT allowed (examples):**
- TODO → DONE (skipping the whole workflow)
- DONE → TODO (can't undo completed work — create a new task)
- CANCELLED → anything (cancelled is final)
- IN_REVIEW → TODO (must go back to IN_PROGRESS, not all the way to start)

## How it's implemented in code

```java
// TaskService.java
private static final Map<TaskStatus, Set<TaskStatus>> VALID_TRANSITIONS = Map.of(
    TaskStatus.TODO,        Set.of(TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
    TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_REVIEW, TaskStatus.TODO, TaskStatus.CANCELLED),
    TaskStatus.IN_REVIEW,   Set.of(TaskStatus.DONE, TaskStatus.IN_PROGRESS, TaskStatus.CANCELLED),
    TaskStatus.DONE,        Set.of(),       // terminal — empty set = no valid transitions
    TaskStatus.CANCELLED,   Set.of()        // terminal
);
```

This `Map` is the entire state machine. Clean and readable.
- Key = current status
- Value = Set of allowed next statuses

**Validation:**
```java
private void validateStatusTransition(TaskStatus currentStatus, TaskStatus newStatus) {
    Set<TaskStatus> allowed = VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of());
    if (!allowed.contains(newStatus)) {
        throw new InvalidStateTransitionException(currentStatus, newStatus);
    }
}
```

`getOrDefault(currentStatus, Set.of())` — if for some reason the current status isn't in the map, return an empty set (no transitions allowed). Defensive programming.

**Where it's called in `updateTask()`:**
```java
// Only validate if status is actually being changed
if (request.getStatus() != null && !request.getStatus().equals(task.getStatus())) {
    validateStatusTransition(task.getStatus(), request.getStatus());
    task.setStatus(request.getStatus());
}
```

Two checks before validating:
1. `request.getStatus() != null` — client actually sent a status change (not null = don't change)
2. `!request.getStatus().equals(task.getStatus())` — the new status is actually different
   If you "change" a TODO task to TODO, that's not a real transition — skip the check.

## What the client sees when they try an invalid transition

Request: `PUT /api/tasks/1` with body `{ "status": "DONE" }` on a task that is currently TODO.

Response:
```json
HTTP 400 Bad Request
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid task status transition: TODO → DONE. Check the allowed transitions in TaskService.",
  "timestamp": "2026-06-10T..."
}
```

---

# PART 3: PAGINATION — WHY YOU CAN NEVER RETURN ALL DATA AT ONCE

## The disaster that happens without pagination

A startup builds an app. It works perfectly with 100 users.

6 months later: 100,000 users, each with 50 tasks. That's **5,000,000 tasks** in the database.

```java
// This was fine with 100 users:
@GetMapping("/tasks")
public List<TaskResponse> getAllTasks() {
    return taskRepository.findAll()  // PROBLEM: loads 5,000,000 rows
        .stream().map(TaskResponse::fromEntity).collect(toList());
}
```

What happens when this endpoint is called with 5 million tasks:
1. Database loads 5,000,000 rows — takes minutes
2. Spring Boot tries to hold all of them in RAM — OutOfMemoryError
3. Server crashes
4. Every user gets an error
5. Server restarts, someone calls the endpoint again, crash again

This actually happened to startups. It's a very real scaling problem.

**Pagination** is the fix: instead of returning everything, return a small page.

```java
// Page 0, 20 items per page → returns items 1-20
// Page 1, 20 items per page → returns items 21-40
// Page 249999, 20 items per page → returns items 4,999,981-5,000,000
```

Each request touches only 20 rows in the database, regardless of total count.

## How users interact with pagination (real examples)

**Google Search:**
- Results 1-10 on page 1, 11-20 on page 2
- Nobody looks past page 3 in practice

**Instagram feed:**
- "Load more" → loads next 20 photos
- Infinite scroll = pagination happening behind the scenes

**Amazon product search:**
- Page 1, 2, 3... at the bottom
- Each page = one paginated API call

**GitHub pull requests list:**
- Shows 30 per page
- URL: `/pulls?page=2`

## Pagination in Spring Data: Pageable and Page<T>

Spring Data has built-in pagination support.

**`Pageable`** — tells the query "which page and how many":
```java
Pageable pageable = PageRequest.of(
    0,                      // page number (0 = first page)
    20,                     // page size (20 items per page)
    Sort.by("createdAt").descending()  // sort
);
```

**`Page<T>`** — what the repository returns:
```java
Page<Task> page = taskRepository.findAll(spec, pageable);

page.getContent()       // the 20 Task objects for this page
page.getTotalElements() // total matching records (e.g., 157)
page.getTotalPages()    // ceil(157/20) = 8
page.getNumber()        // current page number (0)
page.isFirst()          // true (this is page 0)
page.isLast()           // false (there are more pages)
```

**SQL that Hibernate generates:**
```sql
-- Count total (needed for totalPages calculation)
SELECT COUNT(*) FROM tasks WHERE ...

-- Get the actual page data
SELECT * FROM tasks WHERE ... ORDER BY created_at DESC LIMIT 20 OFFSET 0
-- LIMIT = page size (20)
-- OFFSET = page * size (0 * 20 = 0 for first page, 1 * 20 = 20 for second page)
```

**Our `PagedResponse<T>` DTO:**
```json
{
  "content": [...20 task objects...],
  "page": 0,
  "size": 20,
  "totalElements": 157,
  "totalPages": 8,
  "first": true,
  "last": false
}
```

The frontend uses this to show "Page 1 of 8" and "Load more" buttons.

## The generic `<T>` — why PagedResponse works for any type

```java
public class PagedResponse<T> {    // <T> is a type placeholder
    private List<T> content;
    ...
}
```

`T` is replaced at compile time with whatever type you use:
```java
PagedResponse<TaskResponse>     // T = TaskResponse
PagedResponse<ProjectResponse>  // T = ProjectResponse
PagedResponse<UserResponse>     // T = UserResponse
```

One class, works for everything. This is Java **Generics** — the same concept as `List<String>`, `Optional<User>`, `Map<String, Integer>`. The `<T>` is a placeholder that becomes a real type when you use it.

---

# PART 4: JPA SPECIFICATIONS — DYNAMIC QUERY BUILDING

## The problem: combinatorial explosion of filter combinations

Search APIs let users filter by many optional parameters.

For tasks in TaskFlow: status, priority, keyword, project, assignee, due date.

Without Specifications, you write a repository method for every possible combination:

```java
// Without Specifications — what you'd have to write:
findByStatus(status)
findByPriority(priority)
findByStatusAndPriority(status, priority)
findByStatusAndKeyword(status, keyword)
findByStatusAndPriorityAndKeyword(status, priority, keyword)
findByProjectId(projectId)
findByProjectIdAndStatus(projectId, status)
findByProjectIdAndStatusAndPriority(projectId, status, priority)
findByProjectIdAndStatusAndPriorityAndKeyword(projectId, status, priority, keyword)
findByAssigneeId(assigneeId)
findByAssigneeIdAndStatus(assigneeId, status)
... and so on
```

With 6 filters: 2^6 = **64 possible combinations = 64 repository methods**.

This is called **combinatorial explosion**. It's unmanageable. Any change to filters means updating dozens of methods.

Companies that built systems this way before Specifications existed had maintenance nightmares.

## The solution: Specifications

A Specification is a single reusable condition (one piece of a WHERE clause).

You compose them at runtime based on which filters were sent.

```java
// Each Specification = one WHERE condition:
TaskSpecification.hasStatus(IN_PROGRESS)    → WHERE status = 'IN_PROGRESS'
TaskSpecification.hasPriority(HIGH)         → WHERE priority = 'HIGH'
TaskSpecification.titleContains("bug")      → WHERE LOWER(title) LIKE '%bug%'
TaskSpecification.belongsToProject(1L)      → WHERE project_id = 1
TaskSpecification.isAssignedTo(5L)          → WHERE assignee_id = 5

// Compose them with .and():
Specification.where(hasStatus(IN_PROGRESS))
    .and(hasPriority(HIGH))
    .and(titleContains("bug"))
// Result: WHERE status = 'IN_PROGRESS' AND priority = 'HIGH' AND LOWER(title) LIKE '%bug%'
```

If a filter is null (not sent by client):
```java
TaskSpecification.hasStatus(null)   → returns null
.and(null)                          → ignored completely
// No WHERE condition for status is added
```

This means you write the composition once, and it handles ALL combinations automatically:

```java
// This one method handles all 64 filter combinations:
Specification<Task> spec = Specification
    .where(TaskSpecification.hasStatus(status))       // null = skip
    .and(TaskSpecification.hasPriority(priority))     // null = skip
    .and(TaskSpecification.titleContains(keyword))    // null = skip
    .and(TaskSpecification.belongsToProject(projectId)) // null = skip
    .and(TaskSpecification.isAssignedTo(assigneeId)); // null = skip
```

Null filter → no condition. Non-null filter → condition added. Simple.

## How a Specification works internally: The Criteria API

Each Specification is a function with this signature:
```java
Predicate toPredicate(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb)
```

**`Root<Task> root`** — represents the Task entity/table. Use it to access fields:
```java
root.get("status")          // → tasks.status column
root.get("project")         // → navigates the @ManyToOne relationship
root.get("project").get("id")  // → tasks.project_id column (joining through the FK)
```

**`CriteriaBuilder cb`** — factory that creates conditions:
```java
cb.equal(root.get("status"), IN_PROGRESS)     // → status = 'IN_PROGRESS'
cb.like(cb.lower(root.get("title")), "%bug%") // → LOWER(title) LIKE '%bug%'
cb.isNull(root.get("assignee"))               // → assignee_id IS NULL
cb.lessThanOrEqualTo(root.get("dueDate"), date) // → due_date <= ?
```

**`Predicate`** — one SQL condition. The Specification returns this.

**The lambda shortcut:**
```java
// Instead of creating a class that implements Specification, use a lambda:
public static Specification<Task> hasStatus(TaskStatus status) {
    return (root, query, cb) ->           // ← lambda = implementation of toPredicate()
            status == null ? null          // ← if no filter, return null = skip
            : cb.equal(root.get("status"), status);  // ← the actual condition
}
```

## JpaSpecificationExecutor — the repository interface that enables this

For `findAll(Specification, Pageable)` to exist on the repository, the repository must implement `JpaSpecificationExecutor`:

```java
// TaskRepository.java
public interface TaskRepository
    extends JpaRepository<Task, Long>,          // gives you save, findById, delete, etc.
            JpaSpecificationExecutor<Task> {    // gives you findAll(Specification, Pageable)
```

`JpaSpecificationExecutor` adds:
```java
findAll(Specification<T> spec)                    // all matching
findAll(Specification<T> spec, Pageable pageable) // paginated
findAll(Specification<T> spec, Sort sort)          // sorted
count(Specification<T> spec)                       // count matching
exists(Specification<T> spec)                      // does any match?
```

One interface addition gives you all of this.

## Full flow of the search endpoint

```
Client: GET /api/tasks/search?status=IN_PROGRESS&priority=HIGH&page=0&size=10

Controller: TaskController.searchTasks()
  @RequestParam(required = false) TaskStatus status   → IN_PROGRESS
  @RequestParam(required = false) Priority priority   → HIGH
  @RequestParam(required = false) String keyword      → null (not sent)
  @RequestParam(required = false) Long projectId      → null (not sent)
  @RequestParam(defaultValue = "0") int page          → 0
  @RequestParam(defaultValue = "10") int size         → 10
  @RequestParam(defaultValue = "createdAt") String sortBy → createdAt
  @RequestParam(defaultValue = "desc") String sortDir → desc

Service: TaskService.searchTasks()
  Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending())

  Specification<Task> spec =
    Specification.where(hasStatus(IN_PROGRESS))    // adds: WHERE status = 'IN_PROGRESS'
    .and(hasPriority(HIGH))                        // adds: AND priority = 'HIGH'
    .and(titleContains(null))                      // null → skipped
    .and(belongsToProject(null))                   // null → skipped
    .and(isAssignedTo(null))                       // null → skipped
    .and(dueBefore(null));                         // null → skipped

  taskRepository.findAll(spec, pageable)

Hibernate generates SQL:
  SELECT COUNT(*) FROM tasks WHERE status = 'IN_PROGRESS' AND priority = 'HIGH'
  -- returns: 23 total

  SELECT * FROM tasks
  WHERE status = 'IN_PROGRESS' AND priority = 'HIGH'
  ORDER BY created_at DESC
  LIMIT 10 OFFSET 0
  -- returns: first 10 matching tasks

Response:
{
  "content": [...10 task objects...],
  "page": 0,
  "size": 10,
  "totalElements": 23,
  "totalPages": 3,
  "first": true,
  "last": false
}
```

---

# PART 5: ALL NEW CODE IN SESSION 3

## New Files

**`exception/InvalidStateTransitionException.java`**
- Thrown when a task status change violates the state machine rules
- Handled by GlobalExceptionHandler → returns 400 Bad Request
- Message explains the invalid transition clearly

**`dto/response/PagedResponse<T>.java`**
- Generic wrapper for all paginated responses
- Contains: content list, page number, size, totalElements, totalPages, first/last flags
- `fromEntity(Page<T> page)` factory method converts Spring's Page to our DTO

**`specification/TaskSpecification.java`**
- Static methods, each returning one Specification (one WHERE condition)
- Conditions: hasStatus, hasPriority, titleContains, belongsToProject, isAssignedTo, dueBefore, isUnassigned
- Returns null when filter param is null → condition is skipped

**`specification/ProjectSpecification.java`**
- Same pattern for Project: hasStatus, hasPriority, nameContains, ownedBy

## Updated Files

**`repository/TaskRepository.java`** and **`repository/ProjectRepository.java`**
- Added `extends JpaSpecificationExecutor<Task>` / `JpaSpecificationExecutor<Project>`
- Unlocks `findAll(Specification, Pageable)` method

**`service/TaskService.java`**
- Added `VALID_TRANSITIONS` map (the state machine)
- `updateTask()` now validates status transitions before applying them
- New `searchTasks()` method — takes optional filters + pagination params, returns PagedResponse

**`service/ProjectService.java`**
- New `searchProjects()` method

**`controller/TaskController.java`**
- New `GET /api/tasks/search` endpoint
- All filter params are `@RequestParam(required = false)`

**`controller/ProjectController.java`**
- New `GET /api/projects/search` endpoint

**`exception/GlobalExceptionHandler.java`**
- New handler for `InvalidStateTransitionException` → 400 Bad Request

---

# PART 6: TESTING SESSION 3 IN SWAGGER

## Test 1: State Machine

**Setup:** Create a project and a task (task starts as TODO).

**Test invalid transition — TODO → DONE:**
```
PUT /api/tasks/1
Body: { "status": "DONE" }
```
Expected: `400 Bad Request` — "Invalid task status transition: TODO → DONE"

**Test valid transition — TODO → IN_PROGRESS:**
```
PUT /api/tasks/1
Body: { "status": "IN_PROGRESS" }
```
Expected: `200 OK` — task status is now IN_PROGRESS

**Test terminal state — try to change a CANCELLED task:**
```
PUT /api/tasks/1
Body: { "status": "IN_PROGRESS" }   (after cancelling first)
```
Expected: `400 Bad Request` — CANCELLED has no valid transitions

## Test 2: Pagination

**Create 5+ tasks in a project. Then:**
```
GET /api/tasks/search?page=0&size=2
```
Expected: Response with 2 tasks, totalElements = 5, totalPages = 3

```
GET /api/tasks/search?page=1&size=2
```
Expected: Next 2 tasks (different ones), page = 1

## Test 3: Dynamic Filtering

**Single filter:**
```
GET /api/tasks/search?status=TODO
```
→ Only TODO tasks

**Two filters:**
```
GET /api/tasks/search?status=IN_PROGRESS&priority=HIGH
```
→ Only IN_PROGRESS + HIGH tasks

**Keyword search:**
```
GET /api/tasks/search?keyword=login
```
→ Tasks whose title contains "login" (case-insensitive)

**Filter + pagination + sort:**
```
GET /api/tasks/search?status=TODO&page=0&size=5&sortBy=createdAt&sortDir=asc
```
→ First 5 TODO tasks, oldest first

**No filters (all tasks, paginated):**
```
GET /api/tasks/search?page=0&size=20
```
→ First 20 tasks from all tasks

## Test 4: Project Search

```
GET /api/projects/search?status=ACTIVE
GET /api/projects/search?keyword=taskflow&priority=HIGH
GET /api/projects/search?page=0&size=5&sortBy=name&sortDir=asc
```

---

# PART 7: INTERVIEW QUESTIONS — ANSWER THESE OUT LOUD

---

**Q: What is a state machine and why would you use one?**

A: A state machine is a model where an entity can be in exactly one of a defined set of states at any time, with explicit rules for which transitions between states are allowed.

You use it when you have a workflow where not every change is valid — like task management, order processing, or payment flows. It prevents invalid state combinations (like jumping from TODO to DONE without going through review), enforces business rules at the code level rather than relying on frontend validation, and makes the workflow explicit and maintainable.

In Java, a simple implementation is a Map where keys are current states and values are Sets of allowed next states. Any transition attempt that isn't in the map is rejected.

---

**Q: What is pagination and why is it needed?**

A: Pagination is the practice of returning data in fixed-size chunks (pages) rather than all at once. It's needed because returning all records from a large database table in one response would:
1. Overwhelm the database (executing a full table scan)
2. Exhaust server memory trying to hold millions of objects
3. Send megabytes or gigabytes of data over the network
4. Be unusable by the client (a UI can't display 50,000 rows meaningfully)

In Spring Data, `Pageable` tells the repository which page to fetch, and `Page<T>` is the response containing both the data for that page and metadata like total count and number of pages. Hibernate translates this to SQL LIMIT and OFFSET clauses.

---

**Q: What is the JPA Criteria API? What is a Specification?**

A: The JPA Criteria API is a programmatic, type-safe way to build database queries in Java — as opposed to writing JPQL strings manually. It uses objects (`Root`, `CriteriaBuilder`, `Predicate`) to construct WHERE clauses at runtime.

A Specification is a Spring Data abstraction over the Criteria API that wraps one reusable query condition. Each Specification is a lambda returning a `Predicate`. Multiple Specifications are composed with `.and()` and `.or()` to build complex dynamic queries.

The benefit over JPQL strings: conditions are optional (return null to skip), composable (combine any subset at runtime), type-safe (compiler catches typos in field names), and reusable across different queries.

---

**Q: What is the N+1 problem in relation to pagination?**

A: Even with pagination (fetching 20 rows), N+1 can still occur when converting entities to DTOs. If each of the 20 tasks lazy-loads its `project`, `reporter`, and `assignee` during the mapping step, that's 20 × 3 = 60 additional SQL queries on top of the initial query — 61 total for one page of 20 items.

The fix is to use `JOIN FETCH` in JPQL or entity graphs to load all needed associations in a single query. For Session 3, the N+1 exists in our search method and is acceptable at this scale. Session 6 will fix it with proper fetch strategies. Recognizing and explaining this tradeoff is what distinguishes a good engineer.

---

**Q: What does `@RequestParam(required = false)` do? What happens if the client doesn't send it?**

A: `required = false` makes a URL query parameter optional. If the client doesn't include it in the URL, Spring injects `null` for the parameter instead of throwing a `MissingServletRequestParameterException` (which it would do with `required = true`, the default).

`defaultValue` goes further — if not sent, Spring injects the specified string value instead of null. For primitives like `int`, you must use `defaultValue` because Java primitives can't be null.

---

# QUICK REFERENCE

```
CONCEPT             │ KEY CLASS/ANNOTATION          │ WHAT IT DOES
────────────────────┼───────────────────────────────┼──────────────────────────────────
State machine       │ Map<Status, Set<Status>>       │ defines valid transitions
Invalid transition  │ InvalidStateTransitionException│ 400 Bad Request to client
Pagination          │ Pageable / PageRequest.of()   │ which page, how many, sort order
Paginated result    │ Page<T>                        │ content + count + page metadata
Our paginated DTO   │ PagedResponse<T>               │ our clean JSON wrapper for Page
Dynamic filter      │ Specification<T>               │ one reusable WHERE condition
Compose filters     │ Specification.where().and()    │ build query from conditions
Enable in repo      │ JpaSpecificationExecutor<T>    │ adds findAll(Spec, Pageable)
Skip null filter    │ return null from Specification │ .and(null) is a no-op
Access entity field │ root.get("fieldName")          │ navigates entity in Criteria API
Build condition     │ cb.equal(), cb.like(), etc.    │ CriteriaBuilder creates Predicates
Optional URL param  │ @RequestParam(required=false)  │ null if not sent by client
Default URL param   │ @RequestParam(defaultValue="") │ fallback value if not sent
Parse date from URL │ @DateTimeFormat(iso=DATE)      │ "2026-07-01" → LocalDate
```

---

> **Session 4 covers:** Spring AOP (Aspect-Oriented Programming) + Spring Events +
> @Scheduled (cron jobs). These are the "cross-cutting concerns" tools — how you add
> logging, auditing, and background jobs without polluting your business logic.
