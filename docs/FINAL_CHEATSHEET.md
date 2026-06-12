# FINAL CHEAT SHEET — Read This Before Interview
### Java + SQL + DSA — Everything in one place

---

# PART A — CORE JAVA

## OOP — 4 Pillars

| Pillar | One Line | Example |
|--------|----------|---------|
| **Encapsulation** | Private data + public methods | `private String password; public getEmail()` |
| **Inheritance** | Child gets parent's properties | `ResourceNotFoundException extends RuntimeException` |
| **Polymorphism** | Same name, different behavior | Override `findById()` with `@EntityGraph` |
| **Abstraction** | Hide implementation, show interface | `JpaRepository.save()` — don't know if INSERT or UPDATE |

---

## Interface vs Abstract Class

| | Interface | Abstract Class |
|--|-----------|----------------|
| Variables | `public static final` only | Any type |
| Methods | Abstract + default (Java 8+) | Abstract + concrete both |
| Inheritance | Implements multiple | Extends only one |
| Constructor | No | Yes |
| Use when | Define a contract | Share common code |

---

## String vs StringBuilder vs StringBuffer

| | String | StringBuilder | StringBuffer |
|--|--------|---------------|--------------|
| Mutable | No (immutable) | Yes | Yes |
| Thread-safe | Yes | No | Yes |
| Speed | Slow in loops | Fastest | Slower than SB |
| Use when | Single values | Loop concat (single thread) | Multi-thread concat |

```java
// WRONG in loop — creates 1000 String objects
String s = "";
for(int i=0;i<1000;i++) s += i;

// RIGHT
StringBuilder sb = new StringBuilder();
for(int i=0;i<1000;i++) sb.append(i);
String result = sb.toString();
```

---

## Collections

```
List   → ordered, duplicates allowed
  ArrayList  → fast get(index) O(1), slow insert/delete middle O(n)
  LinkedList → slow get O(n), fast insert/delete O(1)

Set    → no duplicates
  HashSet    → no order, O(1) add/contains
  TreeSet    → sorted order, O(log n)
  LinkedHashSet → insertion order, O(1)

Map    → key-value pairs
  HashMap    → no order, O(1) get/put
  TreeMap    → sorted by key, O(log n)
  LinkedHashMap → insertion order, O(1)
```

**HashMap internals:**
> Uses array of buckets. `hashCode()` finds bucket. Collision → LinkedList in bucket. Java 8+: >8 entries → Red-Black Tree. Always use immutable keys (String, Integer).

**equals() + hashCode() rule:**
> If `a.equals(b)` is true → `a.hashCode() == b.hashCode()` MUST be true. Override both together always.

---

## Exception Handling

| | Checked | Unchecked |
|--|---------|-----------|
| Extends | Exception | RuntimeException |
| Compiler forces? | Yes | No |
| Examples | IOException, SQLException | NullPointerException, ArrayIndexOutOfBounds |
| Use when | Recoverable (file not found) | Programming bugs |

```java
// try-with-resources — auto closes anything implementing AutoCloseable
try (Connection conn = dataSource.getConnection()) {
    // conn.close() called automatically even on exception
}

// finally — ALWAYS runs (even if exception thrown or return called)
try { riskyCode(); }
catch (Exception e) { handle(e); }
finally { cleanup(); } // always runs
```

---

## Multithreading

```java
// Thread vs Runnable — prefer Runnable (Java has single inheritance)
class Task implements Runnable {
    public void run() { System.out.println("running"); }
}
new Thread(new Task()).start();

// synchronized — only one thread at a time
synchronized(this) { criticalCode(); }

// volatile — don't cache, always read from main memory
volatile boolean running = true;
```

**Thread Lifecycle:**
```
NEW → RUNNABLE → RUNNING → BLOCKED/WAITING/SLEEPING → TERMINATED
```

**Deadlock:** Thread A holds Lock1, waits for Lock2. Thread B holds Lock2, waits for Lock1. Both wait forever.

---

## Java 8 Features

```java
// Lambda
list.sort((a, b) -> a.compareTo(b));

// Stream API
List<String> names = users.stream()
    .filter(u -> u.isActive())
    .map(User::getName)
    .sorted()
    .collect(Collectors.toList());

// Optional — avoids NullPointerException
Optional<User> user = repo.findById(1L);
user.orElseThrow(() -> new RuntimeException("Not found"));
user.ifPresent(u -> System.out.println(u.getName()));
String name = user.map(User::getName).orElse("Unknown");

// Method reference
list.forEach(System.out::println);   // instead of x -> System.out.println(x)
```

---

## OOP Design Patterns (say these in interview)

| Pattern | Where in your project | What it does |
|---------|----------------------|-------------|
| Repository | `ProjectRepository` | Separates DB access from logic |
| Builder | `User.builder().email(...).build()` | Create complex objects step by step |
| Singleton | All `@Service`, `@Repository` beans | One instance shared everywhere |
| Observer | Spring Events (`TaskCreatedEvent`) | Decouple publisher from listener |
| Proxy | AOP `@Auditable` | Intercept method calls |
| Factory | Spring IoC container | Creates beans |

---

## Spring Boot Quick Answers

**@Transactional:** Groups DB operations — all succeed or all rollback. Keeps Hibernate session open for lazy loading. `readOnly=true` skips dirty checking, faster for GETs.

**@Autowired vs Constructor Injection:**
```java
// BAD — field can be null, hard to test
@Autowired private UserRepo repo;

// GOOD — field is final, can't be null, easy to test
@RequiredArgsConstructor
public class Service {
    private final UserRepo repo; // injected via constructor
}
```

**Bean Scopes:** Singleton (default, one instance) | Prototype (new per injection) | Request (one per HTTP request) | Session (one per HTTP session)

**@RestController = @Controller + @ResponseBody** — returns JSON directly, not a view name.

---

---

# PART B — SQL / DATABASE

## SQL Command Types
```
DDL → CREATE, ALTER, DROP, TRUNCATE    (structure)
DML → SELECT, INSERT, UPDATE, DELETE   (data)
DCL → GRANT, REVOKE                    (permissions)
TCL → COMMIT, ROLLBACK, SAVEPOINT      (transactions)
```

## SELECT Order of Execution
```
FROM → JOIN → WHERE → GROUP BY → HAVING → SELECT → DISTINCT → ORDER BY → LIMIT
```
> This is why you CAN'T use SELECT alias in WHERE — WHERE runs before SELECT.

---

## JOINs — Draw this if whiteboard available

```
INNER JOIN  = only matching rows from BOTH tables
LEFT JOIN   = ALL rows from LEFT + matching from right (NULL if no match)
RIGHT JOIN  = matching from left + ALL rows from RIGHT (NULL if no match)
FULL JOIN   = ALL rows from both (MySQL: use UNION of LEFT + RIGHT)
SELF JOIN   = table joined with itself (manager-employee hierarchy)
```

```sql
-- INNER: only employees WITH a department
SELECT e.name, d.dept_name FROM employees e
INNER JOIN departments d ON e.dept_id = d.id;

-- LEFT: ALL employees, even without department
SELECT e.name, d.dept_name FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id;

-- Departments with NO employees (LEFT JOIN + NULL check)
SELECT d.dept_name FROM departments d
LEFT JOIN employees e ON d.id = e.dept_id
WHERE e.id IS NULL;
```

---

## WHERE vs HAVING

```sql
WHERE  → filters ROWS   before GROUP BY  (can't use COUNT/SUM here)
HAVING → filters GROUPS after  GROUP BY  (use COUNT/SUM here)

SELECT dept_id, COUNT(*) FROM employees
WHERE salary > 40000        -- filter rows first
GROUP BY dept_id
HAVING COUNT(*) > 1;        -- filter groups after
```

---

## NULL Rules
```sql
NULL = NULL   → false (NULL is unknown, not equal to anything)
NULL = 5      → false
NULL != 5     → false
-- Always use:
WHERE col IS NULL
WHERE col IS NOT NULL
-- COUNT(*) counts NULLs, COUNT(col) does NOT count NULLs
```

---

## Aggregate Functions
```sql
COUNT(*), COUNT(col)   -- * counts nulls, col doesn't
SUM(col), AVG(col)
MAX(col), MIN(col)
```

---

## Subqueries
```sql
-- IN subquery
SELECT * FROM employees
WHERE dept_id IN (SELECT id FROM departments WHERE dept_name = 'Engineering');

-- Correlated subquery (inner uses outer's value, runs per row)
SELECT name FROM employees e
WHERE salary > (SELECT AVG(salary) FROM employees WHERE dept_id = e.dept_id);

-- EXISTS (faster than IN for large data — stops at first match)
SELECT dept_name FROM departments d
WHERE EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = d.id);
```

---

## Indexes
```sql
CREATE INDEX idx_emp_dept ON employees(dept_id);
-- B-tree structure: O(n) scan → O(log n) lookup
-- PostgreSQL/MySQL does NOT auto-index foreign keys!
-- Trade-off: faster SELECT, slower INSERT/UPDATE/DELETE
-- Leftmost prefix rule for composite indexes
```

---

## Transactions + ACID
```sql
START TRANSACTION;
UPDATE accounts SET balance = balance - 1000 WHERE id = 1;
UPDATE accounts SET balance = balance + 1000 WHERE id = 2;
COMMIT;   -- or ROLLBACK if something failed
```

| ACID | Meaning |
|------|---------|
| **A**tomicity | All or nothing |
| **C**onsistency | DB stays valid |
| **I**solation | Concurrent transactions don't interfere |
| **D**urability | Committed data survives crashes |

---

## DELETE vs TRUNCATE vs DROP
```
DELETE   → removes rows, has WHERE, logged, can ROLLBACK, slow
TRUNCATE → removes ALL rows, no WHERE, minimal log, fast, resets auto-increment
DROP     → removes entire TABLE (structure + data), permanent
```

---

## Classic Interview Queries

```sql
-- 2nd highest salary
SELECT MAX(salary) FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);

-- Nth highest salary (set N=3 for 3rd)
SELECT salary FROM employees
ORDER BY salary DESC LIMIT 1 OFFSET (N-1);

-- Find duplicates
SELECT email, COUNT(*) FROM employees
GROUP BY email HAVING COUNT(*) > 1;

-- Delete duplicates, keep one
DELETE FROM employees WHERE id NOT IN
(SELECT MIN(id) FROM employees GROUP BY email);

-- Employees earning more than their manager
SELECT e.name FROM employees e
JOIN employees m ON e.manager_id = m.id
WHERE e.salary > m.salary;

-- Departments with no employees
SELECT d.dept_name FROM departments d
LEFT JOIN employees e ON d.id = e.dept_id
WHERE e.id IS NULL;
```

---

## Window Functions (bonus)
```sql
ROW_NUMBER() → 1,2,3,4,5         (unique, no ties)
RANK()       → 1,2,2,4,5         (ties get same rank, gap after)
DENSE_RANK() → 1,2,2,3,4         (ties get same rank, NO gap)

SELECT name, salary,
       DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
FROM employees;

-- Per department rank
RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC)
```

---

---

# PART C — DSA QUICK REFERENCE

## Complexity Cheat Sheet
```
O(1)       → HashMap get/put, array[i]
O(log n)   → Binary search, BST
O(n)       → Single loop, linear scan
O(n log n) → Merge sort, heap sort
O(n²)      → Nested loops, bubble sort
O(2^n)     → Naive recursion (Fibonacci)
```

---

## Sorting Algorithms

| Algorithm | Best | Average | Worst | Space | Stable |
|-----------|------|---------|-------|-------|--------|
| Bubble | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Selection | O(n²) | O(n²) | O(n²) | O(1) | No |
| Insertion | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Merge | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes |
| Quick | O(n log n) | O(n log n) | O(n²) | O(log n) | No |

> Java `Arrays.sort()` → primitives use Dual-Pivot QuickSort, Objects use TimSort (merge+insertion, always O(n log n))

```java
// Bubble Sort
void bubbleSort(int[] arr) {
    for (int i = 0; i < arr.length-1; i++)
        for (int j = 0; j < arr.length-i-1; j++)
            if (arr[j] > arr[j+1]) { int t=arr[j]; arr[j]=arr[j+1]; arr[j+1]=t; }
}
```

---

## Most Common Patterns + Technique

| Problem | Technique | Time |
|---------|-----------|------|
| Two Sum | HashMap | O(n) |
| Duplicate in array | HashSet | O(n) |
| Max subarray sum | Kadane's | O(n) |
| Palindrome check | Two pointers | O(n) |
| Anagram check | char count[26] | O(n) |
| Missing number | Sum formula n*(n+1)/2 | O(n) |
| Reverse linked list | prev/curr/next | O(n) |
| Cycle in linked list | Slow/fast pointer | O(n) |
| Middle of linked list | Slow/fast pointer | O(n) |
| Valid parentheses | Stack | O(n) |
| Binary search | left/right/mid | O(log n) |
| Tree height | Recursion | O(n) |

---

## Must-Know Code Snippets

```java
// Two Sum — HashMap
Map<Integer, Integer> map = new HashMap<>();
for (int i = 0; i < nums.length; i++) {
    if (map.containsKey(target - nums[i])) return new int[]{map.get(target-nums[i]), i};
    map.put(nums[i], i);
}

// Kadane's — max subarray
int maxSum = nums[0], curr = nums[0];
for (int i = 1; i < nums.length; i++) {
    curr = Math.max(nums[i], curr + nums[i]);
    maxSum = Math.max(maxSum, curr);
}

// Reverse Linked List
ListNode prev = null, curr = head;
while (curr != null) {
    ListNode next = curr.next;
    curr.next = prev;
    prev = curr;
    curr = next;
}
return prev;

// Floyd's Cycle Detection
ListNode slow = head, fast = head;
while (fast != null && fast.next != null) {
    slow = slow.next; fast = fast.next.next;
    if (slow == fast) return true;
}
return false;

// Binary Search
int left = 0, right = nums.length - 1;
while (left <= right) {
    int mid = left + (right - left) / 2;
    if (nums[mid] == target) return mid;
    if (nums[mid] < target) left = mid + 1;
    else right = mid - 1;
}

// Valid Parentheses
Stack<Character> stack = new Stack<>();
for (char c : s.toCharArray()) {
    if (c=='(' || c=='[' || c=='{') stack.push(c);
    else {
        if (stack.isEmpty()) return false;
        char t = stack.pop();
        if (c==')' && t!='(') return false;
        if (c==']' && t!='[') return false;
        if (c=='}' && t!='{') return false;
    }
}
return stack.isEmpty();
```

---

---

# PART D — YOUR PROJECT PITCH (say this first)

> "I built **TaskFlow** — a Jira-like project management REST API in Spring Boot with PostgreSQL and Redis.
>
> Key things I implemented:
> - **JWT authentication** — stateless, token-based auth with Spring Security
> - **State machine** for tasks — TODO → IN_PROGRESS → IN_REVIEW → DONE (invalid transitions = 400 error)
> - **AOP audit logging** — @Auditable annotation logs every operation without touching business code
> - **Redis caching** — @Cacheable reduces response from 15ms to 0.5ms. Fixed N+1 problem with @EntityGraph
> - **Razorpay payments** — HMAC-SHA256 signature verification, subscription upgrade flow
> - **Scheduled jobs** — @Scheduled cron runs daily reminders at 9 AM

> Architecture: Controller → Service → Repository → Database. Proper layering, DTOs, custom exceptions with GlobalExceptionHandler."

---

# LAST MINUTE — READ THESE ALOUD ONCE

- Encapsulation = private fields + public methods
- HashMap = array of buckets, hashCode finds bucket, collision = LinkedList
- Checked exception = compiler forces handle (extends Exception)
- Unchecked = extends RuntimeException, no force
- WHERE = filter rows, HAVING = filter groups
- INNER JOIN = only matches, LEFT JOIN = all left + matches
- NULL → always IS NULL, never = NULL
- Transaction = all or nothing (ACID)
- Binary search = left+right/2, O(log n)
- Kadane's = at each element, extend or restart
- Reverse linked list = prev, curr, next pointers
- Floyd's cycle = slow 1 step, fast 2 steps

---

**Interview at 12PM. You've built a real project. Be confident. Talk while you code.**
