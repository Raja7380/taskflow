# SQL — Complete Guide
### Why it exists, how it works, real industry use + interview questions

---

## WHY SQL EXISTS

Imagine you have 10 million users in a file. How do you find users from Mumbai who signed up in 2024?

Without a database:
- Open the file, read every line, check each one manually → takes hours

With SQL:
```sql
SELECT * FROM users WHERE city = 'Mumbai' AND YEAR(created_at) = 2024;
```
→ Returns in milliseconds.

SQL (Structured Query Language) was invented at IBM in 1974 to give a **human-readable language** to query data stored in relational databases. "Relational" means data is stored in **tables with rows and columns**, and tables can be **related** to each other via keys.

Every major company in the world — Google, Amazon, banks, hospitals — uses SQL. It's 50 years old and still the most important data skill for any developer.

---

## THE FOUR TYPES OF SQL COMMANDS

```
DDL — Data Definition Language   → Define structure (CREATE, ALTER, DROP)
DML — Data Manipulation Language → Work with data  (SELECT, INSERT, UPDATE, DELETE)
DCL — Data Control Language      → Permissions     (GRANT, REVOKE)
TCL — Transaction Control Language → Transactions  (COMMIT, ROLLBACK, SAVEPOINT)
```

---

## SAMPLE TABLES (used for all examples below)

```
employees                          departments
+----+--------+--------+-------+   +----+-------------+
| id | name   | dept_id| salary|   | id | dept_name   |
+----+--------+--------+-------+   +----+-------------+
| 1  | Raja   |  10    | 50000 |   | 10 | Engineering |
| 2  | Amit   |  20    | 60000 |   | 20 | Marketing   |
| 3  | Priya  |  10    | 70000 |   | 30 | HR          |
| 4  | Ravi   |  30    | 45000 |   +----+-------------+
| 5  | Sneha  |  NULL  | 55000 |   (dept 30 = HR has Ravi)
+----+--------+--------+-------+   (Sneha has no department)

orders
+----+------+---------+------------+--------+
| id | user_id | product | order_date | amount |
+----+------+---------+------------+--------+
| 1  | 1    | Laptop  | 2024-01-15 | 50000  |
| 2  | 1    | Mouse   | 2024-02-10 | 500    |
| 3  | 2    | Phone   | 2024-03-05 | 20000  |
| 4  | 3    | Laptop  | 2023-12-01 | 50000  |
+----+------+---------+------------+--------+
```

---

# PART 1 — SELECT (Reading Data)

## Basic SELECT

```sql
-- Get everything
SELECT * FROM employees;

-- Get specific columns only
SELECT name, salary FROM employees;

-- Give columns a different name in output (alias)
SELECT name AS employee_name, salary AS monthly_salary FROM employees;

-- Remove duplicates
SELECT DISTINCT dept_id FROM employees;
-- Output: 10, 20, 30, NULL (each dept once)
```

---

## WHERE — Filter Rows

```sql
-- Simple condition
SELECT * FROM employees WHERE salary > 55000;

-- Multiple conditions with AND
SELECT * FROM employees WHERE dept_id = 10 AND salary > 55000;

-- OR condition
SELECT * FROM employees WHERE dept_id = 10 OR dept_id = 20;

-- NOT
SELECT * FROM employees WHERE NOT dept_id = 10;

-- IN — shortcut for multiple OR
SELECT * FROM employees WHERE dept_id IN (10, 20);
-- Same as: dept_id = 10 OR dept_id = 20

-- BETWEEN — inclusive on both ends
SELECT * FROM employees WHERE salary BETWEEN 50000 AND 60000;
-- Same as: salary >= 50000 AND salary <= 60000

-- LIKE — pattern matching
SELECT * FROM employees WHERE name LIKE 'R%';     -- starts with R
SELECT * FROM employees WHERE name LIKE '%a';     -- ends with a
SELECT * FROM employees WHERE name LIKE '%aj%';   -- contains 'aj'
SELECT * FROM employees WHERE name LIKE '_aja';   -- 4 chars, ends in 'aja'
-- % = any number of characters
-- _ = exactly one character

-- NULL check — IMPORTANT: never use = NULL, always IS NULL
SELECT * FROM employees WHERE dept_id IS NULL;    -- Sneha
SELECT * FROM employees WHERE dept_id IS NOT NULL;
```

**Why = NULL doesn't work:**
> NULL means "unknown". `NULL = NULL` is also unknown (not true). SQL uses `IS NULL` specifically because equality comparison with unknown is undefined.

---

## ORDER BY — Sort Results

```sql
-- Ascending (default)
SELECT * FROM employees ORDER BY salary;
SELECT * FROM employees ORDER BY salary ASC;

-- Descending
SELECT * FROM employees ORDER BY salary DESC;

-- Sort by multiple columns
SELECT * FROM employees ORDER BY dept_id ASC, salary DESC;
-- First sort by dept, then within same dept sort by salary high→low
```

---

## LIMIT & OFFSET — Pagination

```sql
-- Get first 3 rows
SELECT * FROM employees ORDER BY salary DESC LIMIT 3;

-- Skip first 2, get next 3 (page 2, page size 3)
SELECT * FROM employees ORDER BY salary DESC LIMIT 3 OFFSET 2;
-- OFFSET 0 = page 1, OFFSET 3 = page 2, OFFSET 6 = page 3
```

**Real industry use:** Every API with pagination uses LIMIT/OFFSET or cursor-based pagination. In your TaskFlow project, `PageRequest.of(page, size)` generates exactly this SQL.

---

# PART 2 — AGGREGATE FUNCTIONS

These calculate a single value from multiple rows.

```sql
COUNT(*)         -- count rows
COUNT(column)    -- count non-NULL values in column
SUM(column)      -- add up all values
AVG(column)      -- average
MAX(column)      -- highest value
MIN(column)      -- lowest value
```

```sql
-- How many employees total?
SELECT COUNT(*) FROM employees;               -- 5

-- How many have a department assigned?
SELECT COUNT(dept_id) FROM employees;         -- 4 (Sneha's NULL not counted)

-- Total salary bill
SELECT SUM(salary) FROM employees;            -- 280000

-- Average salary
SELECT AVG(salary) FROM employees;            -- 56000

-- Highest salary
SELECT MAX(salary) FROM employees;            -- 70000

-- Lowest salary
SELECT MIN(salary) FROM employees;            -- 45000
```

---

## GROUP BY — Aggregate Per Group

Without GROUP BY, aggregate functions give ONE result for the whole table.
With GROUP BY, they give ONE result PER GROUP.

```sql
-- How many employees in each department?
SELECT dept_id, COUNT(*) AS headcount
FROM employees
GROUP BY dept_id;

-- Output:
-- dept_id | headcount
-- 10      | 2         (Raja, Priya)
-- 20      | 1         (Amit)
-- 30      | 1         (Ravi)
-- NULL    | 1         (Sneha)

-- Average salary per department
SELECT dept_id, AVG(salary) AS avg_salary, MAX(salary) AS max_salary
FROM employees
GROUP BY dept_id;
```

**Rule: In SELECT, you can only have:**
1. Columns that are in GROUP BY, OR
2. Aggregate functions

```sql
-- WRONG — name is not in GROUP BY and not an aggregate
SELECT dept_id, name, COUNT(*) FROM employees GROUP BY dept_id;

-- CORRECT
SELECT dept_id, COUNT(*), MAX(salary) FROM employees GROUP BY dept_id;
```

---

## HAVING — Filter Groups (like WHERE but for groups)

```sql
-- Departments with more than 1 employee
SELECT dept_id, COUNT(*) AS headcount
FROM employees
GROUP BY dept_id
HAVING COUNT(*) > 1;
-- Output: dept_id=10, headcount=2

-- Departments where average salary > 55000
SELECT dept_id, AVG(salary) AS avg_sal
FROM employees
GROUP BY dept_id
HAVING AVG(salary) > 55000;
```

**WHERE vs HAVING:**
```
WHERE  filters ROWS    before grouping  → works on individual rows
HAVING filters GROUPS  after grouping   → works on aggregate results

Order of execution:
FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
```

```sql
-- Combined: employees with salary > 40000, show depts with avg > 55000
SELECT dept_id, AVG(salary)
FROM employees
WHERE salary > 40000          -- filter rows first
GROUP BY dept_id
HAVING AVG(salary) > 55000;  -- then filter groups
```

---

# PART 3 — JOINS (Most Important Topic)

A JOIN combines rows from two tables based on a related column.

**Why joins exist:** To avoid storing duplicate data. Instead of putting dept_name in every employee row (wastes space, hard to update), we store it once in departments table and JOIN when we need it. This is called **normalization**.

---

## INNER JOIN — Only matching rows from both tables

```sql
SELECT e.name, e.salary, d.dept_name
FROM employees e
INNER JOIN departments d ON e.dept_id = d.id;

-- Output (only rows where dept_id matches):
-- name  | salary | dept_name
-- Raja  | 50000  | Engineering
-- Amit  | 60000  | Marketing
-- Priya | 70000  | Engineering
-- Ravi  | 45000  | HR
-- (Sneha NOT included — her dept_id is NULL, no match)
-- (No orphan departments either)
```

---

## LEFT JOIN — All rows from LEFT table + matching from right

```sql
SELECT e.name, e.salary, d.dept_name
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id;

-- Output (ALL employees, NULL for missing dept):
-- name  | salary | dept_name
-- Raja  | 50000  | Engineering
-- Amit  | 60000  | Marketing
-- Priya | 70000  | Engineering
-- Ravi  | 45000  | HR
-- Sneha | 55000  | NULL       ← included even without dept
```

**Use LEFT JOIN when:** You want all records from the main table, regardless of whether there's a match.

---

## RIGHT JOIN — All rows from RIGHT table + matching from left

```sql
SELECT e.name, d.dept_name
FROM employees e
RIGHT JOIN departments d ON e.dept_id = d.id;

-- Output (ALL departments, even without employees):
-- name  | dept_name
-- Raja  | Engineering
-- Priya | Engineering
-- Amit  | Marketing
-- Ravi  | HR
-- NULL  | HR          ← wait, HR is shown once with Ravi
-- Actually: all depts shown, NULL employee if dept has no employees
```

---

## FULL OUTER JOIN — All rows from BOTH tables

MySQL doesn't support FULL OUTER JOIN directly. Use UNION:

```sql
SELECT e.name, d.dept_name FROM employees e LEFT  JOIN departments d ON e.dept_id = d.id
UNION
SELECT e.name, d.dept_name FROM employees e RIGHT JOIN departments d ON e.dept_id = d.id;
-- All employees + all departments, NULL where no match
```

---

## SELF JOIN — Table joined with itself

```sql
-- employees table has a manager_id column pointing to another employee
-- Find each employee and their manager's name
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

---

## JOIN Visual (draw this in interview)

```
employees        departments
    ┌────────────────────┐
    │   INNER JOIN       │  ← only the intersection
    │  ┌─────────────────┤
    │  │                 │
    └──┤                 │
       └─────────────────┘

LEFT JOIN = all of LEFT circle + intersection
RIGHT JOIN = intersection + all of RIGHT circle
FULL JOIN = entire area of both circles
```

---

## Multiple JOINs

```sql
-- Three tables: orders JOIN users JOIN products
SELECT u.name, o.product, o.amount
FROM orders o
INNER JOIN users u ON o.user_id = u.id
INNER JOIN products p ON o.product_id = p.id
WHERE o.amount > 10000;
```

---

# PART 4 — SUBQUERIES

A query inside another query. Inner query runs first, result used by outer query.

```sql
-- Find employees who earn more than average salary
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);
-- Inner query: AVG(salary) = 56000
-- Outer query: WHERE salary > 56000
-- Output: Priya (70000), Amit (60000)... wait, 60000 > 56000 → Amit too

-- Find department names of employees earning > 60000
SELECT dept_name FROM departments
WHERE id IN (
    SELECT dept_id FROM employees WHERE salary > 60000
);
-- Inner query returns: [10] (only Priya's dept)
-- Outer query: WHERE id IN (10)
-- Output: Engineering
```

---

## Correlated Subquery — Inner query uses outer query's value

```sql
-- Find employees who earn more than their department's average
SELECT e.name, e.salary, e.dept_id
FROM employees e
WHERE e.salary > (
    SELECT AVG(e2.salary)
    FROM employees e2
    WHERE e2.dept_id = e.dept_id  -- uses outer query's dept_id
);
-- Runs inner query ONCE PER ROW of outer query
-- Slower than regular subquery but very powerful
```

---

## EXISTS — Check if subquery returns any rows

```sql
-- Find departments that have at least one employee
SELECT dept_name FROM departments d
WHERE EXISTS (
    SELECT 1 FROM employees e WHERE e.dept_id = d.id
);
-- EXISTS returns true if subquery has any result, false if empty
-- Faster than IN for large datasets (stops at first match)
```

---

# PART 5 — DDL (Creating Tables)

```sql
-- Create a table
CREATE TABLE employees (
    id          INT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    salary      DECIMAL(10, 2) DEFAULT 0,
    dept_id     INT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES departments(id)
);

-- Add a column
ALTER TABLE employees ADD COLUMN phone VARCHAR(15);

-- Modify a column
ALTER TABLE employees MODIFY COLUMN phone VARCHAR(20) NOT NULL;

-- Delete a column
ALTER TABLE employees DROP COLUMN phone;

-- Delete entire table (structure + data)
DROP TABLE employees;

-- Delete all data but keep structure
TRUNCATE TABLE employees;

-- Rename table
RENAME TABLE employees TO staff;
```

---

## Constraints

| Constraint | Meaning | Example |
|-----------|---------|---------|
| PRIMARY KEY | Unique + Not Null | `id INT PRIMARY KEY` |
| FOREIGN KEY | Must match another table's PK | Links dept_id → departments.id |
| UNIQUE | No duplicates in column | `email VARCHAR(150) UNIQUE` |
| NOT NULL | Cannot be empty | `name VARCHAR(100) NOT NULL` |
| DEFAULT | Value if none provided | `salary DECIMAL DEFAULT 0` |
| CHECK | Custom condition | `CHECK (salary >= 0)` |

---

# PART 6 — DML (Changing Data)

```sql
-- INSERT
INSERT INTO employees (name, email, salary, dept_id)
VALUES ('Raja', 'raja@email.com', 50000, 10);

-- Insert multiple rows at once
INSERT INTO employees (name, email, salary) VALUES
('Amit', 'amit@email.com', 60000),
('Priya', 'priya@email.com', 70000);

-- UPDATE — always use WHERE or you update ALL rows!
UPDATE employees SET salary = 55000 WHERE id = 1;
UPDATE employees SET salary = salary * 1.10 WHERE dept_id = 10; -- 10% raise

-- DELETE — always use WHERE!
DELETE FROM employees WHERE id = 5;
DELETE FROM employees WHERE dept_id = 30;
```

> **DANGER:** `UPDATE employees SET salary = 0;` updates every employee. `DELETE FROM employees;` deletes everyone. Always double-check your WHERE clause.

---

# PART 7 — TRANSACTIONS

A transaction groups multiple SQL statements into one unit. Either ALL succeed or ALL fail.

```sql1
-- Real scenario: transfer ₹10,000 from account 1 to account 2
START TRANSACTION;

UPDATE accounts SET balance = balance - 10000 WHERE id = 1;
UPDATE accounts SET balance = balance + 10000 WHERE id = 2;

-- If both succeeded:
COMMIT;

-- If something went wrong:
ROLLBACK;
```

**Why this matters:** If the server crashes after the first UPDATE but before the second, the money would disappear. With transactions, the partial update is rolled back automatically on crash.

```sql
-- SAVEPOINT — partial rollback
START TRANSACTION;
UPDATE employees SET salary = 60000 WHERE id = 1;
SAVEPOINT sp1;
UPDATE employees SET salary = 70000 WHERE id = 2;
-- Oops, wrong employee
ROLLBACK TO sp1;    -- undo only the second update
COMMIT;             -- commit only the first update
```

---

# PART 8 — INDEXES

```sql
-- Create index
CREATE INDEX idx_employees_dept ON employees(dept_id);
CREATE INDEX idx_employees_salary ON employees(salary);

-- Composite index (multiple columns)
CREATE INDEX idx_emp_dept_sal ON employees(dept_id, salary);

-- Unique index
CREATE UNIQUE INDEX idx_email ON employees(email);

-- Drop index
DROP INDEX idx_employees_dept ON employees;

-- See all indexes on a table
SHOW INDEX FROM employees;
```

**When composite index helps:**
```sql
-- idx_emp_dept_sal(dept_id, salary) helps these:
WHERE dept_id = 10                          -- ✓ leftmost column
WHERE dept_id = 10 AND salary > 50000       -- ✓ both columns
-- Does NOT help:
WHERE salary > 50000                        -- ✗ skipped leftmost column
```
> Rule: Composite index works if you filter from the LEFT. This is called the **leftmost prefix rule**.

---

# PART 9 — BUILT-IN FUNCTIONS

## String Functions
```sql
SELECT UPPER('hello');           -- 'HELLO'
SELECT LOWER('HELLO');           -- 'hello'
SELECT LENGTH('hello');          -- 5
SELECT SUBSTRING('hello', 2, 3); -- 'ell'  (start=2, length=3)
SELECT TRIM('  hello  ');        -- 'hello'
SELECT CONCAT('Raja', ' ', 'Singh'); -- 'Raja Singh'
SELECT REPLACE('hello', 'l', 'r');   -- 'herro'
SELECT INSTR('hello', 'l');      -- 3 (position of first 'l')
```

## Number Functions
```sql
SELECT ROUND(3.567, 2);     -- 3.57
SELECT CEIL(3.2);           -- 4
SELECT FLOOR(3.9);          -- 3
SELECT ABS(-5);             -- 5
SELECT MOD(10, 3);          -- 1 (remainder)
SELECT POWER(2, 10);        -- 1024
```

## Date Functions
```sql
SELECT NOW();                        -- 2026-06-12 10:30:00 (current datetime)
SELECT CURDATE();                    -- 2026-06-12 (today)
SELECT YEAR(created_at);            -- 2026
SELECT MONTH(created_at);           -- 6
SELECT DAY(created_at);             -- 12
SELECT DATEDIFF('2026-12-31', '2026-06-12');  -- 202 (days between)
SELECT DATE_ADD(NOW(), INTERVAL 30 DAY);      -- 30 days from now
SELECT DATE_FORMAT(NOW(), '%d-%m-%Y');        -- '12-06-2026'
```

---

# PART 10 — CLASSIC INTERVIEW QUERIES

These are asked in almost every company. Memorize the pattern.

---

## 1. Second Highest Salary

```sql
-- Method 1: LIMIT OFFSET
SELECT salary FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET 1;

-- Method 2: Subquery (works even without LIMIT support)
SELECT MAX(salary) FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);

-- Method 3: Handle ties properly (using DENSE_RANK)
SELECT salary FROM (
    SELECT salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS rnk
    FROM employees
) ranked
WHERE rnk = 2;
```

---

## 2. Nth Highest Salary (e.g., 3rd highest)

```sql
-- Change N here
SET @N = 3;

SELECT salary FROM employees
ORDER BY salary DESC
LIMIT 1 OFFSET (@N - 1);

-- Or using subquery (works for any N):
SELECT salary FROM employees e1
WHERE (N-1) = (
    SELECT COUNT(DISTINCT salary) FROM employees e2
    WHERE e2.salary > e1.salary
);
```

---

## 3. Find Duplicate Records

```sql
-- Which emails appear more than once?
SELECT email, COUNT(*) AS count
FROM employees
GROUP BY email
HAVING COUNT(*) > 1;

-- See the full duplicate rows
SELECT * FROM employees
WHERE email IN (
    SELECT email FROM employees
    GROUP BY email
    HAVING COUNT(*) > 1
);
```

---

## 4. Delete Duplicate Rows (keep one)

```sql
-- Keep the row with smallest id, delete all others
DELETE FROM employees
WHERE id NOT IN (
    SELECT MIN(id) FROM employees GROUP BY email
);
```

---

## 5. Employees with no department (NULL check)

```sql
SELECT * FROM employees WHERE dept_id IS NULL;
```

---

## 6. Departments with no employees

```sql
SELECT d.dept_name FROM departments d
LEFT JOIN employees e ON d.id = e.dept_id
WHERE e.id IS NULL;

-- Or with NOT EXISTS:
SELECT dept_name FROM departments d
WHERE NOT EXISTS (
    SELECT 1 FROM employees e WHERE e.dept_id = d.id
);
```

---

## 7. Top 3 highest paid employees per department

```sql
SELECT name, salary, dept_id FROM (
    SELECT name, salary, dept_id,
           RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rnk
    FROM employees
) ranked
WHERE rnk <= 3;
```

---

## 8. Running total (cumulative sum)

```sql
SELECT name, salary,
       SUM(salary) OVER (ORDER BY id) AS running_total
FROM employees;
-- Each row shows total salary up to that row
```

---

## 9. Employees earning more than their manager

```sql
SELECT e.name AS employee, e.salary AS emp_salary,
       m.name AS manager, m.salary AS mgr_salary
FROM employees e
JOIN employees m ON e.manager_id = m.id
WHERE e.salary > m.salary;
```

---

## 10. Month-wise order count for 2024

```sql
SELECT MONTH(order_date) AS month, COUNT(*) AS orders, SUM(amount) AS revenue
FROM orders
WHERE YEAR(order_date) = 2024
GROUP BY MONTH(order_date)
ORDER BY month;
```

---

# PART 11 — WINDOW FUNCTIONS (Bonus — modern SQL)

Window functions calculate across rows related to the current row — without collapsing rows like GROUP BY does.

```sql
-- RANK vs DENSE_RANK vs ROW_NUMBER
-- salary: 70000, 60000, 60000, 55000, 50000, 45000

SELECT name, salary,
       ROW_NUMBER()  OVER (ORDER BY salary DESC) AS row_num,    -- 1,2,3,4,5,6
       RANK()        OVER (ORDER BY salary DESC) AS rnk,         -- 1,2,2,4,5,6 (gap after tie)
       DENSE_RANK()  OVER (ORDER BY salary DESC) AS dense_rnk    -- 1,2,2,3,4,5 (no gap)
FROM employees;
```

```sql
-- PARTITION BY — reset rank per group
SELECT name, dept_id, salary,
       RANK() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS dept_rank
FROM employees;
-- Rank 1 in Engineering, rank 1 in Marketing, rank 1 in HR — separately
```

---

# PART 12 — NORMALIZATION (Asked conceptually)

**Why normalize?**
> "To eliminate redundancy and avoid update anomalies. If you store dept_name in every employee row and the department renames, you'd have to update thousands of rows and might miss some — inconsistency. Instead, store dept_name once in departments table."

| Normal Form | Rule | Violation Example |
|------------|------|-------------------|
| 1NF | No repeating groups, atomic values | `skills = "Java,Python"` in one column |
| 2NF | 1NF + No partial dependency on composite key | `order_table(order_id, product_id, product_name)` — product_name depends only on product_id |
| 3NF | 2NF + No transitive dependency | `employee(id, dept_id, dept_name)` — dept_name depends on dept_id, not id |

> For the interview: "I applied 3NF. Users, Projects, Tasks are separate tables. User info isn't duplicated in the projects table — only owner_id is stored as a foreign key."

---

# PART 13 — INTERVIEW Q&A

**Q: What is the difference between WHERE and HAVING?**
> "WHERE filters individual ROWS before grouping happens. HAVING filters GROUPS after GROUP BY. You can't use aggregate functions (COUNT, SUM) in WHERE — use HAVING for that. Execution order: FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY."

**Q: What is a primary key vs foreign key?**
> "Primary key uniquely identifies each row in a table — cannot be NULL, must be unique. Foreign key is a column that references the primary key of another table — creates a relationship. In my project, `tasks.project_id` is a foreign key referencing `projects.id`. If you try to insert a task with a project_id that doesn't exist, the database rejects it — referential integrity."

**Q: What is the difference between DELETE, TRUNCATE, and DROP?**
> "DELETE removes specific rows (can have WHERE clause, is logged, can be rolled back). TRUNCATE removes ALL rows fast — can't have WHERE, minimal logging, faster than DELETE, resets auto-increment. DROP removes the entire table structure and all data permanently. DELETE = surgeon, TRUNCATE = bulldoze the interior, DROP = demolish the building."

**Q: What is a JOIN? Types?**
> "JOIN combines rows from two or more tables based on a related column. INNER JOIN returns only matching rows. LEFT JOIN returns all rows from the left table and matching from right (NULL if no match). RIGHT JOIN is the reverse. FULL OUTER JOIN returns all rows from both tables."

**Q: What is an index and what are its trade-offs?**
> "Index is a sorted data structure (B-tree) on a column that allows the database to find rows in O(log n) instead of O(n) full table scan. Trade-off: speeds up SELECT but slows down INSERT/UPDATE/DELETE because the index must also be updated. Don't index every column — only columns used in WHERE, JOIN ON, or ORDER BY."

**Q: What is a transaction and what are ACID properties?**
> "A transaction is a group of SQL operations that execute as one unit — all or nothing. ACID: Atomicity (all or nothing), Consistency (DB goes from valid state to valid state), Isolation (concurrent transactions don't interfere), Durability (committed data survives crashes). Example: bank transfer — debit and credit must both happen or neither."

**Q: Difference between CHAR and VARCHAR?**
> "CHAR(n) is fixed-length — always stores n characters, pads with spaces. VARCHAR(n) is variable-length — stores only what you provide. CHAR is faster for fixed-size data (phone numbers, status codes). VARCHAR saves space for variable-length data (names, emails). In practice, VARCHAR is used almost everywhere."

**Q: What is a view?**
> "A view is a virtual table — a saved SELECT query. You query it like a table but it has no data of its own. Used for: hiding complexity (join + filter saved as a view), security (expose only certain columns to certain users), reusability. `CREATE VIEW active_employees AS SELECT * FROM employees WHERE active = 1;`"

---

# QUICK REFERENCE — SQL ORDER OF EXECUTION

```
1. FROM        — which table(s)
2. JOIN        — combine tables
3. WHERE       — filter rows
4. GROUP BY    — group rows
5. HAVING      — filter groups
6. SELECT      — choose columns
7. DISTINCT    — remove duplicates
8. ORDER BY    — sort results
9. LIMIT       — limit rows returned
```

This is why you **cannot** use a SELECT alias in a WHERE clause — WHERE runs before SELECT:
```sql
-- WRONG:
SELECT salary * 12 AS annual WHERE annual > 600000  -- annual not defined yet

-- CORRECT:
SELECT salary * 12 AS annual FROM employees WHERE salary * 12 > 600000
-- OR use subquery:
SELECT * FROM (SELECT salary * 12 AS annual FROM employees) t WHERE annual > 600000
```

---

# CHEAT SHEET — One Line Each

```sql
SELECT col FROM table WHERE cond ORDER BY col LIMIT n;    -- basic query
GROUP BY col HAVING COUNT(*) > 1;                         -- grouping + filter
INNER JOIN t2 ON t1.id = t2.fid                          -- matching rows only
LEFT JOIN t2 ON t1.id = t2.fid                           -- all left rows
WHERE col IN (SELECT col FROM ...)                        -- subquery
WHERE col IS NULL                                         -- null check
RANK() OVER (PARTITION BY col ORDER BY col2 DESC)        -- window rank
SELECT MAX(salary) WHERE salary < (SELECT MAX(salary)...)-- 2nd highest
COUNT(*) vs COUNT(col)  → * counts nulls, col doesn't   -- null in count
DELETE has WHERE, TRUNCATE doesn't                        -- delete vs truncate
```
