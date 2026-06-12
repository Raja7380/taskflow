# Session 6: Redis Caching + Performance — Complete Learning Guide

---

## Table of Contents

1. [The Performance Problem](#1-the-performance-problem)
2. [What is Redis?](#2-what-is-redis)
3. [The N+1 Query Problem — The Most Common Interview Topic](#3-the-n1-query-problem--the-most-common-interview-topic)
4. [@EntityGraph — The Fix for N+1](#4-entitygraph--the-fix-for-n1)
5. [Spring Cache Abstraction — @Cacheable, @CacheEvict, @CachePut](#5-spring-cache-abstraction--cacheable-cacheevict-cacheput)
6. [Cache Invalidation — The Hard Part](#6-cache-invalidation--the-hard-part)
7. [Why JSON Serialization in Redis (Not Java Binary)](#7-why-json-serialization-in-redis-not-java-binary)
8. [Database Indexes — Why Queries Are Slow Without Them](#8-database-indexes--why-queries-are-slow-without-them)
9. [The Full Performance Strategy](#9-the-full-performance-strategy)
10. [New Files Added in Session 6](#10-new-files-added-in-session-6)
11. [Testing Performance Improvements](#11-testing-performance-improvements)
12. [Interview Q&A](#12-interview-qa)

---

## 1. The Performance Problem

Imagine TaskFlow has 10,000 users and your most popular endpoint is:

```
GET /api/projects/1
```

It gets called 500 times per minute. Every call:
1. Spring receives HTTP request
2. JWT filter validates token
3. ProjectService.getProjectById(1) runs
4. Hibernate generates SQL: `SELECT * FROM projects WHERE id = 1`
5. PostgreSQL reads from disk, returns rows
6. Hibernate maps rows to Project object
7. ProjectResponse.fromEntity() called
8. 3 more SQL queries for owner, members, tasks
9. JSON response sent

**Total: 4 SQL queries per request × 500 requests/minute = 2,000 DB queries/minute for ONE endpoint.**

The project data almost never changes. Why query the database 2,000 times for the same data?

**Session 6 fixes this two ways:**
1. **@EntityGraph** — reduce 4 queries to 1 (fix the N+1 problem)
2. **Redis cache** — reduce 2,000 DB queries to ~1 per 10 minutes (cache the result)

---

## 2. What is Redis?

Redis (Remote Dictionary Server) was created in 2009 by Salvatore Sanfilippo to solve a real problem: MySQL was too slow for his startup's real-time analytics.

**Redis = in-memory key-value store.**

- All data lives in RAM (not disk)
- RAM speed: ~100 nanoseconds per access
- Disk speed: ~1-10 milliseconds per access
- **Redis is 10,000-100,000× faster than PostgreSQL for simple lookups**

```
Operation              | Time
-----------------------|----------
PostgreSQL query       | 5-20 ms
Redis GET              | 0.1-1 ms
L2 CPU cache           | 0.001 ms
RAM read               | 0.0001 ms
```

**Redis data structures:**
| Structure | Use case | Real example |
|-----------|----------|-------------|
| String | Simple key-value cache | Cache a JSON response |
| List | Queue of jobs | Email send queue |
| Set | Unique members | Unique visitors today |
| Hash | Object fields | User session data |
| Sorted Set | Leaderboard | Top 10 players by score |
| TTL | Auto-expiry | "Delete after 10 minutes" |

**Who uses Redis:**
- Twitter: cache tweet data, timeline
- GitHub: caching repository data
- Stack Overflow: cache question pages
- Instagram: store social graph in memory
- Uber: real-time driver location cache

**In TaskFlow:**
- Cache `ProjectResponse` for 10 minutes after first DB query
- Cache `TaskResponse` for 5 minutes
- Key format: `taskflow:projects::1` (prefix:cacheName::key)

---

## 3. The N+1 Query Problem — The Most Common Interview Topic

This is mentioned in almost every Java backend interview.

### What is it?

Let's say you have 10 projects and you want to show each one with its owner's name.

**The wrong way (N+1 problem):**

```java
// Step 1: 1 query to load all projects
List<Project> projects = projectRepository.findAll();  // SELECT * FROM projects

// Step 2: For EACH project, separately load the owner
for (Project p : projects) {
    // This triggers a NEW SQL query for each project!
    String ownerName = p.getOwner().getFullName();  // SELECT * FROM users WHERE id = ?
}
```

Result:
- 1 query: `SELECT * FROM projects` → returns 10 projects
- 10 queries: `SELECT * FROM users WHERE id = 1`
- 10 queries: `SELECT * FROM users WHERE id = 2`
- ... one per project

**Total = 1 + 10 = 11 queries.** For N projects = **N+1 queries.**

Scale this up:
- 100 projects → 101 queries
- 1,000 projects → 1,001 queries
- 10,000 projects → 10,001 queries

This is called the **N+1 problem** because you make 1 query for the list and then N queries for related data.

### Why does it happen with LAZY loading?

All your entities use `FetchType.LAZY`:

```java
@ManyToOne(fetch = FetchType.LAZY)
private User owner;
```

LAZY means: "Don't load the owner until I actually ask for it."

When you call `p.getOwner()`, Hibernate executes a new SQL query to load that specific User. If you do this inside a loop, you get N+1.

### The numbers in our app

`getTasksByProject()` with 10 tasks:
- 1 query: `SELECT * FROM tasks WHERE project_id = ?`
- 10 queries: `SELECT * FROM users WHERE id = ?` (for each task's reporter)
- 10 queries: `SELECT * FROM users WHERE id = ?` (for each task's assignee)
- = **21 queries** for 10 tasks

Without N+1 fix (1 JOIN query):
- **1 query** with LEFT JOINs for assignee and reporter

**That's a 21× improvement in the number of queries.**

---

## 4. @EntityGraph — The Fix for N+1

`@EntityGraph` tells Hibernate: "When loading this query, JOIN-fetch these related entities in the same SQL query instead of lazily."

### Your implementation (TaskRepository):

```java
// Without @EntityGraph: 1 + 3 = 4 queries per task
Optional<Task> findById(Long id);

// With @EntityGraph: 1 query with 3 LEFT JOINs
@EntityGraph(attributePaths = {"project", "assignee", "reporter"})
Optional<Task> findById(Long id);
```

**SQL generated WITH @EntityGraph:**
```sql
SELECT t.*, p.*, ua.*, ur.*
FROM tasks t
LEFT JOIN projects p  ON t.project_id = p.id
LEFT JOIN users ua    ON t.assignee_id = ua.id   -- assignee (nullable)
LEFT JOIN users ur    ON t.reporter_id = ur.id
WHERE t.id = ?
```

**1 query instead of 4.** For 100 task lookups: 100 queries instead of 400.

### Your implementation (ProjectRepository):

```java
// findById loads owner + members in one JOIN query
@EntityGraph(attributePaths = {"owner", "members"})
Optional<Project> findById(Long id);
```

**SQL generated:**
```sql
SELECT p.*, u.*, m.*
FROM projects p
LEFT JOIN users u    ON p.owner_id = u.id
LEFT JOIN project_members pm ON p.id = pm.project_id
LEFT JOIN users m   ON pm.user_id = m.id
WHERE p.id = ?
```

### @ManyToOne vs @ManyToMany with @EntityGraph

| Relationship | @EntityGraph Safety | Reason |
|-------------|---------------------|--------|
| @ManyToOne | Safe to join-fetch multiple | Single FK column, no cartesian product |
| @ManyToMany | Safe if it's a Set | Set deduplicates rows |
| @OneToMany (List) | Risky with multiple collections | `MultipleBagFetchException` |

**Rule:** Always use `Set` for @ManyToMany and @OneToMany if you want to use @EntityGraph. `List` (bag) collections can't be join-fetched in parallel.

In our project:
- `Project.members` is `Set<User>` → safe with @EntityGraph
- `Project.tasks` is `List<Task>` → kept LAZY, fetched separately

### @EntityGraph vs JOIN FETCH in JPQL

Both achieve the same result. Different syntax:

```java
// Option A: @EntityGraph (cleaner)
@EntityGraph(attributePaths = {"assignee", "reporter"})
List<Task> findByProjectOrderByCreatedAtDesc(Project project);

// Option B: JPQL JOIN FETCH (explicit)
@Query("SELECT t FROM Task t LEFT JOIN FETCH t.assignee LEFT JOIN FETCH t.reporter WHERE t.project = :project ORDER BY t.createdAt DESC")
List<Task> findByProjectWithAssigneeAndReporter(@Param("project") Project project);
```

Use `@EntityGraph` for clean code. Use JPQL JOIN FETCH when you need complex control.

---

## 5. Spring Cache Abstraction — @Cacheable, @CacheEvict, @CachePut

Spring's caching is annotation-driven. You annotate your service methods — Spring handles the rest.

### @Cacheable — Check cache first, only run method on miss

```java
@Cacheable(value = "projects", key = "#projectId")
@Transactional(readOnly = true)
public ProjectResponse getProjectById(Long projectId) {
    // This body only executes on cache MISS
    Project project = findProjectOrThrow(projectId);
    return ProjectResponse.fromEntity(project);
}
```

**Flow:**
1. Spring intercepts the method call
2. Checks Redis: key = `taskflow:projects::1`
3. **HIT:** return the cached `ProjectResponse` (method body NEVER runs, no DB query)
4. **MISS:** run method body → hit DB → cache result → return

**After one cache hit, response time drops from ~20ms to ~0.5ms.**

### @CacheEvict — Remove from cache when data changes

```java
@CacheEvict(value = "projects", key = "#projectId")
@Auditable(action = "UPDATE_PROJECT", entityType = "Project")
public ProjectResponse updateProject(Long projectId, ...) {
    // After this method, the cache entry for projectId is deleted
    // Next getProjectById(projectId) call will query DB fresh
}
```

Why evict instead of update? Eviction is simpler and avoids serving stale data. The next read will re-populate the cache with fresh data.

### @CachePut — Always run method AND update cache

```java
@CachePut(value = "projects", key = "#projectId")
public ProjectResponse updateProject(...) {
    // Method ALWAYS runs (unlike @Cacheable which skips on hit)
    // After method, cache is updated with the new value
    // Pro: Next read is still fast (fresh cached value)
    // Con: Every update writes to both DB and cache
}
```

For TaskFlow, we use `@CacheEvict` on writes (simpler). In high-traffic apps, `@CachePut` is better because it keeps the cache warm.

### Cache key strategies

```java
@Cacheable(value = "projects", key = "#projectId")
// → Redis key: "taskflow:projects::1"

@Cacheable(value = "projects", key = "#currentUser.id + ':myprojects'")
// → Redis key: "taskflow:projects::42:myprojects"

@Cacheable(value = "projects", key = "'all'")
// → Redis key: "taskflow:projects::all"

// Spring Expression Language (SpEL) in key:
@CacheEvict(value = "projects", allEntries = true)
// → Deletes ALL entries in the "projects" cache
```

---

## 6. Cache Invalidation — The Hard Part

> "There are only two hard things in Computer Science: cache invalidation and naming things."
> — Phil Karlton (this quote has been referenced in thousands of engineering talks)

**The stale cache problem:**

```
1. getProjectById(1) → DB query → returns {name: "Old Name"} → cached
2. updateProject(1, {name: "New Name"}) → DB updated → cache NOT updated
3. getProjectById(1) → cache HIT → returns {name: "Old Name"} (WRONG!)
```

**Solutions:**

1. **@CacheEvict on writes** (your approach):
   - On `updateProject(1)` → evict cache key `projects::1`
   - Next `getProjectById(1)` → cache miss → fresh DB query
   - Cons: One extra DB hit after every update

2. **TTL (Time-To-Live)** (backup):
   - Even if you forget @CacheEvict, entries expire automatically
   - `projects` cache TTL = 10 minutes
   - Worst case: stale data for up to 10 minutes
   - This is why TTL is always configured as a safety net

3. **@CachePut on writes** (alternative):
   - Update both DB and cache in the same operation
   - Pros: Cache stays warm (no extra DB hit)
   - Cons: More complex, cache update must succeed

**What you implemented:**
- `@CacheEvict` on updateProject, deleteProject, addMember, removeMember
- `@CacheEvict` on updateTask, deleteTask
- TTL: projects=10min, tasks=5min, users=30min

**Edge cases to be aware of:**
- If `updateProject` throws an exception, `@CacheEvict` may not fire (depends on order)
- Solution: use `@CacheEvict(beforeInvocation = true)` to always evict, even on error

---

## 7. Why JSON Serialization in Redis (Not Java Binary)?

Spring's default cache serializer uses Java binary serialization (`ObjectOutputStream`).

**Problems with Java binary:**
```
\xac\xed\x00\x05sr\x00!com.taskflow.dto.response.ProjectResponse...
```
- Not human-readable in `redis-cli`
- If you change the class (add/remove a field), existing cached data BREAKS
- Slow to serialize/deserialize
- Java-only — Python/Node.js microservices can't read it

**Our JSON approach:**
```json
{
  "@class": "com.taskflow.dto.response.ProjectResponse",
  "id": 1,
  "name": "TaskFlow App",
  "ownerName": "Raja Singh",
  "memberCount": 3,
  "createdAt": "2026-06-11T09:00:00"
}
```

- Human-readable in `redis-cli`
- Adding a new field: old cache deserializes fine (Jackson ignores unknown fields)
- Any language can read it
- `@class` field tells Jackson which Java class to use when deserializing

**RedisConfig implements this:**
```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());              // Handle LocalDate/LocalDateTime
mapper.disable(WRITE_DATES_AS_TIMESTAMPS);               // "2026-06-11" not [2026,6,11]
mapper.activateDefaultTyping(...);                       // Includes "@class" in JSON
return new GenericJackson2JsonRedisSerializer(mapper);
```

**Why JavaTimeModule?**

Jackson doesn't know how to serialize `LocalDate`/`LocalDateTime` by default.
`JavaTimeModule` teaches it: `LocalDate.of(2026, 6, 11)` → `"2026-06-11"`.

---

## 8. Database Indexes — Why Queries Are Slow Without Them

### What is an index?

An index is a sorted copy of specific columns that allows the database to find rows without scanning the entire table.

**Without index:**
```sql
SELECT * FROM tasks WHERE assignee_id = 42;
```
PostgreSQL has to read EVERY ROW in the tasks table to find tasks where `assignee_id = 42`.
- 1,000 tasks → check 1,000 rows
- 1,000,000 tasks → check 1,000,000 rows
- Time complexity: O(N) — linear scan

**With index on `assignee_id`:**
PostgreSQL maintains a sorted B-tree of `(assignee_id, row_pointer)`.
- Can jump directly to all rows with `assignee_id = 42` via binary search
- 1,000,000 tasks → check ~20 rows (log₂ of 1,000,000 ≈ 20)
- Time complexity: O(log N) — binary search

**Important fact PostgreSQL experts know:**
PostgreSQL does NOT automatically create indexes on foreign key columns!
It creates indexes on primary keys (`id`) automatically.
But `tasks.project_id`, `tasks.assignee_id`, `tasks.reporter_id` — NO automatic index!

**Your entity now has indexes:**
```java
@Table(name = "tasks", indexes = {
    @Index(name = "idx_task_project",  columnList = "project_id"),
    @Index(name = "idx_task_assignee", columnList = "assignee_id"),
    @Index(name = "idx_task_reporter", columnList = "reporter_id"),
    @Index(name = "idx_task_status",   columnList = "status"),
    @Index(name = "idx_task_due_date", columnList = "due_date"),
    @Index(name = "idx_task_priority", columnList = "priority")
})
```

Hibernate creates these via `CREATE INDEX` statements during `ddl-auto: update`.

### When NOT to add an index

Indexes speed up reads but SLOW DOWN writes:

```
INSERT task → 1 write to tasks table + 6 writes to index structures
UPDATE task → update rows + update all affected indexes
DELETE task → delete row + delete from all indexes
```

**Rule:** Only index columns used in WHERE clauses, JOIN conditions, or ORDER BY. Don't index every column.

Good candidates: FK columns, status/enum columns in search filters, timestamp for date range queries.

Bad candidates: `description` (text, rarely in WHERE), `created_at` (rarely sorted unless you have date range queries), columns with low cardinality (like `currency` = always "INR" — an index on "INR" is useless).

---

## 9. The Full Performance Strategy

Session 6 adds three performance improvements. Here's how they work together:

```
GET /api/projects/1 (first time):
  → Spring Security validates JWT (~1ms)
  → @Cacheable checks Redis: MISS
  → @Transactional starts
  → ProjectRepository.findById(1)
      WITH @EntityGraph: 1 SQL query with LEFT JOINs for owner + members
      (previously: 1 SELECT + separate lazy loads = 3 queries)
      indexes on owner_id, status → fast B-tree lookup
  → ProjectResponse.fromEntity(p) — tasks.size() = 1 more query
  → TOTAL: 2 DB queries, ~15ms
  → Result stored in Redis with TTL=10min
  → Return JSON

GET /api/projects/1 (same request 10 seconds later):
  → Spring Security validates JWT (~1ms)
  → @Cacheable checks Redis: HIT
  → Return cached JSON from Redis: ~0.5ms
  → TOTAL: 0 DB queries, ~1.5ms

Result: 10× faster on cached requests, 33% fewer queries on first request.
```

**Performance pyramid (fastest to slowest):**
```
L1/L2 CPU cache → 0.0001ms (hardware, automatic)
Redis cache     → 0.1-1ms   (your code)
PostgreSQL      → 5-20ms    (disk + query engine)
External API    → 50-500ms  (network round trip)
```

Cache what's at the bottom (slow) to make it feel like the top (fast).

---

## 10. New Files Added in Session 6

### New Files
- `config/RedisConfig.java` — @EnableCaching, CacheManager with TTL per cache, JSON serialization

### Modified Files
- `pom.xml` — added spring-boot-starter-data-redis + spring-boot-starter-cache
- `application.yml` — added spring.data.redis + spring.cache.type=redis
- `entity/Task.java` — added @Table(indexes) for project_id, assignee_id, status, due_date, priority, reporter_id
- `entity/Project.java` — added @Table(indexes) for owner_id, status, priority
- `repository/ProjectRepository.java` — @EntityGraph on findById, findByOwner, findByOwnerOrMember
- `repository/TaskRepository.java` — @EntityGraph on findById, findByProjectOrderByCreatedAtDesc, findByAssigneeOrderByDueDateAsc
- `service/ProjectService.java` — @Cacheable on getProjectById; @CacheEvict on update/delete/addMember/removeMember
- `service/TaskService.java` — @Cacheable on getTaskById; @CacheEvict on update/delete

---

## 11. Testing Performance Improvements

### Step 1: Start Redis

```bash
docker-compose up -d redis
```

Verify Redis is running:
```bash
docker-compose exec redis redis-cli ping
# Expected output: PONG
```

### Step 2: Inspect cache in Redis CLI

```bash
docker-compose exec redis redis-cli
```

Inside redis-cli:
```
127.0.0.1:6379> KEYS *                    # list all keys
127.0.0.1:6379> KEYS taskflow:projects*   # list project cache keys
127.0.0.1:6379> GET "taskflow:projects::1"  # see cached value (JSON)
127.0.0.1:6379> TTL "taskflow:projects::1"  # seconds until expiry
127.0.0.1:6379> DEL "taskflow:projects::1"  # manually evict
127.0.0.1:6379> FLUSHALL                  # clear everything (careful!)
```

### Step 3: Observe N+1 fix in SQL logs

`application.yml` has `show-sql: true` — Spring prints every SQL query to the console.

**Without @EntityGraph** (old behavior): calling `getTaskById(1)` would show:
```sql
select t.* from tasks where id=1
select u.* from users where id=3        -- reporter lazy load
select u.* from users where id=7        -- assignee lazy load
select p.* from projects where id=2     -- project lazy load
```

**With @EntityGraph** (new behavior): one query:
```sql
select t.*, p.*, ua.*, ur.*
from tasks t
left join projects p on t.project_id=p.id
left join users ua on t.assignee_id=ua.id
left join users ur on t.reporter_id=ur.id
where t.id=1
```

### Step 4: Measure timing via Swagger

1. `GET /api/projects/1` first time — watch response time in Swagger (or browser DevTools)
2. `GET /api/projects/1` second time — should be noticeably faster
3. `PUT /api/projects/1` — updates the project, evicts cache
4. `GET /api/projects/1` again — first call re-hits DB (cache was evicted)

The audit log (GET /api/audit-logs/my) will show the `durationMs` field — compare before/after caching.

---

## 12. Interview Q&A

**Q: What is the N+1 query problem?**

A: When loading a list of N entities, each entity triggers an additional query for a lazily-loaded association, resulting in 1 + N total queries instead of 1. Example: loading 100 tasks and accessing each task's assignee triggers 100 extra SELECT queries. Fix with @EntityGraph to JOIN-fetch associations in the initial query, or use JPQL JOIN FETCH explicitly.

---

**Q: What is @EntityGraph and when do you use it?**

A: @EntityGraph is a Spring Data JPA annotation that overrides LAZY loading for a specific repository method. It adds JOIN clauses to the generated SQL, loading related entities in a single query instead of lazily. Use it when you know a method will always need certain associations — avoids N+1. Don't use it globally because eager loading everything is worse than selective lazy loading.

---

**Q: What is the difference between @Cacheable, @CacheEvict, and @CachePut?**

A: `@Cacheable` — check cache first; if miss, run method and store result. Method skipped on cache hit. `@CacheEvict` — after method runs, delete the cache entry (marks it as stale). `@CachePut` — always run method, always update cache with new result. Use @Cacheable for reads, @CacheEvict for deletes, @CachePut for writes where you want to keep the cache warm.

---

**Q: Why is Redis faster than PostgreSQL?**

A: Redis stores all data in RAM (random access memory). RAM access is ~100 nanoseconds. PostgreSQL stores data on disk and reads via B-tree indexes — even with SSD, disk I/O is thousands of times slower than RAM. Additionally, Redis answers simple GET/SET in O(1) with no query parsing, optimization, or MVCC overhead. For simple key-value lookups, Redis is 10,000-100,000× faster.

---

**Q: What is cache invalidation and why is it hard?**

A: Cache invalidation is the process of removing or updating stale cache entries when the underlying data changes. It's hard because: (1) you must identify every write operation that could affect a cached value and add @CacheEvict; (2) in distributed systems, multiple servers each have their own caches; (3) race conditions — between eviction and re-population, another request might cache a stale value; (4) complex objects may be cached in multiple places under different keys. Common strategies: TTL (auto-expiry), event-driven eviction, cache-aside pattern.

---

**Q: Why does PostgreSQL not automatically index foreign key columns?**

A: PostgreSQL automatically indexes only the PRIMARY KEY. Foreign key columns in child tables are not indexed by default. This is a deliberate design choice — indexes consume disk space and slow down writes. PostgreSQL assumes developers will add indexes where needed. Not indexing a heavily-queried FK column (like `tasks.assignee_id`) causes full table scans, which is O(N) for N rows. Always add @Index on FK columns that appear in WHERE clauses or JOIN conditions.

---

**Q: What is TTL in caching and how do you choose the value?**

A: TTL (Time-To-Live) is the duration after which a cache entry automatically expires. Choose TTL based on: (1) how often the data changes — project names rarely change → 10 min TTL is safe; task status changes frequently → 5 min TTL; (2) how stale data affects users — financial data or medical records need very low TTL or no caching; (3) traffic patterns — high-traffic endpoints benefit most from longer TTL. The key trade-off: higher TTL = better performance but potentially stale data; lower TTL = fresher data but more DB load.
