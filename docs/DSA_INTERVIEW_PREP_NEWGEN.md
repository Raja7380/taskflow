# DSA Interview Prep — Newgen Software
### Only what they actually ask. Java solutions. Explain as you code.

---

## WHAT TO EXPECT AT NEWGEN SDE1

- Easy to Medium level only — no DP heavy, no graphs, no segment trees
- They watch HOW you think, not just the answer — talk while you code
- Most asked: Arrays, Strings, LinkedList, Stack, HashMap, Binary Search
- Time per problem: ~15-20 minutes
- Always say: "Can I use extra space?" and "Should I handle null/empty input?"

---

## HOW TO APPROACH ANY DSA PROBLEM (say this out loud)

```
Step 1: Repeat the problem → "So you want me to..."
Step 2: Give an example → "If input is [1,2,3], output should be..."
Step 3: Brute force first → "Naive approach is O(n²)..."
Step 4: Optimize → "Better approach using HashMap/Two Pointer..."
Step 5: Code it
Step 6: Trace through example → dry run
Step 7: Mention edge cases → null, empty, single element, negatives
```

---

# SECTION 1 — ARRAYS

---

## Q1. Find the duplicate in an array of 1 to N

**Input:** `[1, 3, 4, 2, 2]` → **Output:** `2`

### Brute Force — O(n²)
```java
// Check every pair — too slow
for (int i = 0; i < n; i++)
    for (int j = i+1; j < n; j++)
        if (arr[i] == arr[j]) return arr[i];
```

### Optimal — HashMap O(n) time, O(n) space
```java
public int findDuplicate(int[] nums) {
    Set<Integer> seen = new HashSet<>();
    for (int num : nums) {
        if (!seen.add(num)) return num; // add() returns false if already present
    }
    return -1;
}
```

### Best — Floyd's Cycle Detection O(n) time, O(1) space
```java
public int findDuplicate(int[] nums) {
    int slow = nums[0], fast = nums[0];
    // Phase 1: find meeting point inside cycle
    do {
        slow = nums[slow];
        fast = nums[nums[fast]];
    } while (slow != fast);
    // Phase 2: find cycle entrance (= duplicate)
    slow = nums[0];
    while (slow != fast) {
        slow = nums[slow];
        fast = nums[fast];
    }
    return slow;
}
```
> "Array treated as a linked list — value is the next index. Duplicate creates a cycle."

---

## Q2. Two Sum — find two numbers that add to target

**Input:** `nums = [2, 7, 11, 15]`, `target = 9` → **Output:** `[0, 1]`

```java
public int[] twoSum(int[] nums, int target) {
    // key = number, value = its index
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[]{map.get(complement), i};
        }
        map.put(nums[i], i);
    }
    return new int[]{};
}
// Time: O(n)  Space: O(n)
```
> "For each element, check if its complement (target - element) was already seen. HashMap gives O(1) lookup."

---

## Q3. Maximum subarray sum (Kadane's Algorithm)

**Input:** `[-2, 1, -3, 4, -1, 2, 1, -5, 4]` → **Output:** `6` (subarray `[4,-1,2,1]`)

```java
public int maxSubArray(int[] nums) {
    int maxSum = nums[0];
    int currentSum = nums[0];

    for (int i = 1; i < nums.length; i++) {
        // Either extend the previous subarray OR start fresh from here
        currentSum = Math.max(nums[i], currentSum + nums[i]);
        maxSum = Math.max(maxSum, currentSum);
    }
    return maxSum;
}
// Time: O(n)  Space: O(1)
```
> "At each element, decide: is it better to add this element to the running sum, or start fresh? Keep track of the global maximum."

---

## Q4. Rotate array by K positions

**Input:** `[1,2,3,4,5,6,7]`, `k=3` → **Output:** `[5,6,7,1,2,3,4]`

```java
public void rotate(int[] nums, int k) {
    int n = nums.length;
    k = k % n; // handle k > n
    reverse(nums, 0, n-1);   // [7,6,5,4,3,2,1]
    reverse(nums, 0, k-1);   // [5,6,7,4,3,2,1]
    reverse(nums, k, n-1);   // [5,6,7,1,2,3,4]
}

private void reverse(int[] arr, int left, int right) {
    while (left < right) {
        int temp = arr[left];
        arr[left] = arr[right];
        arr[right] = temp;
        left++; right--;
    }
}
// Time: O(n)  Space: O(1)
```

---

## Q5. Move all zeros to end

**Input:** `[0, 1, 0, 3, 12]` → **Output:** `[1, 3, 12, 0, 0]`

```java
public void moveZeroes(int[] nums) {
    int insertPos = 0; // where the next non-zero goes
    for (int num : nums) {
        if (num != 0) nums[insertPos++] = num;
    }
    // fill rest with zeros
    while (insertPos < nums.length) nums[insertPos++] = 0;
}
// Time: O(n)  Space: O(1)
```

---

## Q6. Find missing number in array 1 to N

**Input:** `[3, 0, 1]` → **Output:** `2`

```java
public int missingNumber(int[] nums) {
    int n = nums.length;
    int expected = n * (n + 1) / 2; // sum of 0..n
    int actual = 0;
    for (int num : nums) actual += num;
    return expected - actual;
}
// Time: O(n)  Space: O(1)
// Math trick: sum formula. Missing = expected - actual.
```

---

# SECTION 2 — STRINGS

---

## Q7. Reverse a string

```java
public String reverse(String s) {
    char[] chars = s.toCharArray();
    int left = 0, right = chars.length - 1;
    while (left < right) {
        char temp = chars[left];
        chars[left] = chars[right];
        chars[right] = temp;
        left++; right--;
    }
    return new String(chars);
}
// Or one-liner: new StringBuilder(s).reverse().toString()
```

---

## Q8. Check if string is Palindrome

**Input:** `"racecar"` → `true`, `"hello"` → `false`

```java
public boolean isPalindrome(String s) {
    int left = 0, right = s.length() - 1;
    while (left < right) {
        if (s.charAt(left) != s.charAt(right)) return false;
        left++; right--;
    }
    return true;
}
// Time: O(n)  Space: O(1)
```

---

## Q9. Check if two strings are Anagrams

**Input:** `"anagram"`, `"nagaram"` → `true`

```java
public boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) return false;
    int[] count = new int[26]; // for lowercase letters
    for (char c : s.toCharArray()) count[c - 'a']++;
    for (char c : t.toCharArray()) count[c - 'a']--;
    for (int n : count) if (n != 0) return false;
    return true;
}
// Time: O(n)  Space: O(1) — array size is fixed at 26
```
> "Count character frequencies. If both strings are anagrams, all counts cancel to zero."

---

## Q10. First non-repeating character

**Input:** `"leetcode"` → `0` (index of 'l')

```java
public int firstUniqChar(String s) {
    int[] count = new int[26];
    for (char c : s.toCharArray()) count[c - 'a']++;
    for (int i = 0; i < s.length(); i++) {
        if (count[s.charAt(i) - 'a'] == 1) return i;
    }
    return -1;
}
// Time: O(n)  Space: O(1)
```

---

## Q11. Count occurrences of each character (HashMap)

```java
public Map<Character, Integer> charCount(String s) {
    Map<Character, Integer> map = new LinkedHashMap<>();
    for (char c : s.toCharArray()) {
        map.put(c, map.getOrDefault(c, 0) + 1);
    }
    return map;
}
// "aabbcc" → {a=2, b=2, c=2}
```

---

## Q12. Reverse words in a sentence

**Input:** `"Hello World"` → `"World Hello"`

```java
public String reverseWords(String s) {
    String[] words = s.trim().split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (int i = words.length - 1; i >= 0; i--) {
        sb.append(words[i]);
        if (i > 0) sb.append(" ");
    }
    return sb.toString();
}
```

---

# SECTION 3 — LINKED LIST

---

## Q13. Reverse a Linked List

```java
// Node class — they may give you this or ask you to write it
class ListNode {
    int val;
    ListNode next;
    ListNode(int val) { this.val = val; }
}

public ListNode reverse(ListNode head) {
    ListNode prev = null;
    ListNode curr = head;
    while (curr != null) {
        ListNode next = curr.next; // save next
        curr.next = prev;          // reverse the pointer
        prev = curr;               // move prev forward
        curr = next;               // move curr forward
    }
    return prev; // prev is now the new head
}
// Time: O(n)  Space: O(1)
```

**Dry run on `1->2->3->NULL`:**
```
Initially: prev=null, curr=1
Step 1: next=2, 1.next=null, prev=1, curr=2
Step 2: next=3, 2.next=1,   prev=2, curr=3
Step 3: next=null, 3.next=2, prev=3, curr=null
Return prev=3  →  3->2->1->NULL ✓
```

---

## Q14. Detect cycle in Linked List (Floyd's Algorithm)

```java
public boolean hasCycle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;       // moves 1 step
        fast = fast.next.next;  // moves 2 steps
        if (slow == fast) return true; // they meet = cycle exists
    }
    return false; // fast reached end = no cycle
}
// Time: O(n)  Space: O(1)
```
> "Tortoise and Hare — fast pointer moves twice as fast. If there's a cycle, they'll eventually meet. If no cycle, fast reaches null."

---

## Q15. Find middle of Linked List

**Input:** `1->2->3->4->5` → **Output:** node `3`

```java
public ListNode findMiddle(ListNode head) {
    ListNode slow = head, fast = head;
    while (fast != null && fast.next != null) {
        slow = slow.next;
        fast = fast.next.next;
    }
    return slow; // slow is at middle when fast reaches end
}
// Time: O(n)  Space: O(1)
```

---

## Q16. Merge two sorted Linked Lists

**Input:** `1->2->4` and `1->3->4` → **Output:** `1->1->2->3->4->4`

```java
public ListNode mergeSorted(ListNode l1, ListNode l2) {
    ListNode dummy = new ListNode(0); // dummy head to simplify
    ListNode curr = dummy;
    while (l1 != null && l2 != null) {
        if (l1.val <= l2.val) { curr.next = l1; l1 = l1.next; }
        else                  { curr.next = l2; l2 = l2.next; }
        curr = curr.next;
    }
    curr.next = (l1 != null) ? l1 : l2; // attach remaining
    return dummy.next;
}
// Time: O(n+m)  Space: O(1)
```

---

# SECTION 4 — STACK & QUEUE

---

## Q17. Valid Parentheses

**Input:** `"()[]{}"` → `true`, `"(]"` → `false`

```java
public boolean isValid(String s) {
    Stack<Character> stack = new Stack<>();
    for (char c : s.toCharArray()) {
        if (c == '(' || c == '[' || c == '{') {
            stack.push(c);
        } else {
            if (stack.isEmpty()) return false;
            char top = stack.pop();
            if (c == ')' && top != '(') return false;
            if (c == ']' && top != '[') return false;
            if (c == '}' && top != '{') return false;
        }
    }
    return stack.isEmpty(); // stack must be empty at end
}
// Time: O(n)  Space: O(n)
```

---

## Q18. Implement Stack using Queue

```java
class MyStack {
    Queue<Integer> q = new LinkedList<>();

    public void push(int x) {
        q.add(x);
        // rotate: move all elements before x to after x
        for (int i = 0; i < q.size() - 1; i++) {
            q.add(q.poll());
        }
    }

    public int pop()  { return q.poll(); }
    public int top()  { return q.peek(); }
    public boolean empty() { return q.isEmpty(); }
}
```

---

## Q19. Next Greater Element

**Input:** `[4, 5, 2, 10]` → **Output:** `[5, 10, 10, -1]`

```java
public int[] nextGreater(int[] nums) {
    int n = nums.length;
    int[] result = new int[n];
    Arrays.fill(result, -1); // default: no greater element
    Stack<Integer> stack = new Stack<>(); // stores indices

    for (int i = 0; i < n; i++) {
        // pop all elements smaller than current — current is their answer
        while (!stack.isEmpty() && nums[stack.peek()] < nums[i]) {
            result[stack.pop()] = nums[i];
        }
        stack.push(i);
    }
    return result;
}
// Time: O(n)  Space: O(n)
```

---

# SECTION 5 — SEARCHING & SORTING

---

## Q20. Binary Search

**Input:** sorted array `[1,3,5,7,9,11]`, target `7` → **Output:** index `3`

```java
public int binarySearch(int[] nums, int target) {
    int left = 0, right = nums.length - 1;
    while (left <= right) {
        int mid = left + (right - left) / 2; // avoid integer overflow
        if (nums[mid] == target) return mid;
        if (nums[mid] < target)  left = mid + 1;  // search right half
        else                     right = mid - 1; // search left half
    }
    return -1; // not found
}
// Time: O(log n)  Space: O(1)
```
> "Why `left + (right - left) / 2` instead of `(left + right) / 2`? To avoid integer overflow when left and right are large numbers."

---

## Q21. Bubble Sort (explain the logic)

```java
public void bubbleSort(int[] arr) {
    int n = arr.length;
    for (int i = 0; i < n - 1; i++) {
        boolean swapped = false;
        for (int j = 0; j < n - i - 1; j++) {
            if (arr[j] > arr[j+1]) {
                int temp = arr[j]; arr[j] = arr[j+1]; arr[j+1] = temp;
                swapped = true;
            }
        }
        if (!swapped) break; // already sorted — optimization
    }
}
// Time: O(n²)  Space: O(1)
// Best case with optimization: O(n) — already sorted
```

---

## Q22. Merge Sort (explain divide and conquer)

```java
public void mergeSort(int[] arr, int left, int right) {
    if (left >= right) return;
    int mid = (left + right) / 2;
    mergeSort(arr, left, mid);     // sort left half
    mergeSort(arr, mid+1, right);  // sort right half
    merge(arr, left, mid, right);  // merge both halves
}

private void merge(int[] arr, int left, int mid, int right) {
    int[] temp = new int[right - left + 1];
    int i = left, j = mid + 1, k = 0;
    while (i <= mid && j <= right) {
        if (arr[i] <= arr[j]) temp[k++] = arr[i++];
        else                  temp[k++] = arr[j++];
    }
    while (i <= mid)    temp[k++] = arr[i++];
    while (j <= right)  temp[k++] = arr[j++];
    System.arraycopy(temp, 0, arr, left, temp.length);
}
// Time: O(n log n)  Space: O(n)
```

---

## Sorting Comparison Table (memorize this)

| Algorithm | Best | Average | Worst | Space | Stable? |
|-----------|------|---------|-------|-------|---------|
| Bubble Sort | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Selection Sort | O(n²) | O(n²) | O(n²) | O(1) | No |
| Insertion Sort | O(n) | O(n²) | O(n²) | O(1) | Yes |
| Merge Sort | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes |
| Quick Sort | O(n log n) | O(n log n) | O(n²) | O(log n) | No |

> "In Java, `Arrays.sort()` uses Dual-Pivot QuickSort for primitives and TimSort (merge + insertion) for objects. TimSort is always O(n log n) worst case."

---

# SECTION 6 — TREES

---

## Q23. Inorder / Preorder / Postorder Traversal

```java
class TreeNode {
    int val;
    TreeNode left, right;
    TreeNode(int val) { this.val = val; }
}

// Inorder: Left → Root → Right (gives sorted order for BST)
public void inorder(TreeNode root) {
    if (root == null) return;
    inorder(root.left);
    System.out.print(root.val + " ");
    inorder(root.right);
}

// Preorder: Root → Left → Right (used to copy a tree)
public void preorder(TreeNode root) {
    if (root == null) return;
    System.out.print(root.val + " ");
    preorder(root.left);
    preorder(root.right);
}

// Postorder: Left → Right → Root (used to delete a tree)
public void postorder(TreeNode root) {
    if (root == null) return;
    postorder(root.left);
    postorder(root.right);
    System.out.print(root.val + " ");
}
```

---

## Q24. Height of a binary tree

```java
public int height(TreeNode root) {
    if (root == null) return 0;
    return 1 + Math.max(height(root.left), height(root.right));
}
// Time: O(n) — visits every node once
```

---

## Q25. Check if binary tree is BST

```java
public boolean isValidBST(TreeNode root) {
    return validate(root, Long.MIN_VALUE, Long.MAX_VALUE);
}

private boolean validate(TreeNode node, long min, long max) {
    if (node == null) return true;
    if (node.val <= min || node.val >= max) return false;
    return validate(node.left,  min, node.val)   // left must be < current
        && validate(node.right, node.val, max);  // right must be > current
}
// Time: O(n)  Space: O(h) — h = height of tree
```

---

# SECTION 7 — RECURSION

---

## Q26. Fibonacci (recursive + memoization)

```java
// Naive recursion — O(2^n) — very slow
public int fib(int n) {
    if (n <= 1) return n;
    return fib(n-1) + fib(n-2);
}

// With memoization — O(n)
Map<Integer, Integer> memo = new HashMap<>();
public int fibMemo(int n) {
    if (n <= 1) return n;
    if (memo.containsKey(n)) return memo.get(n);
    int result = fibMemo(n-1) + fibMemo(n-2);
    memo.put(n, result);
    return result;
}

// Iterative — O(n) time, O(1) space (best)
public int fibIter(int n) {
    if (n <= 1) return n;
    int a = 0, b = 1;
    for (int i = 2; i <= n; i++) {
        int c = a + b;
        a = b; b = c;
    }
    return b;
}
```

---

## Q27. Factorial

```java
public int factorial(int n) {
    if (n == 0 || n == 1) return 1; // base case
    return n * factorial(n - 1);    // recursive case
}
// factorial(5) = 5 * 4 * 3 * 2 * 1 = 120
```

---

# SECTION 8 — HASHING PROBLEMS

---

## Q28. Group anagrams together

**Input:** `["eat","tea","tan","ate","nat","bat"]`
**Output:** `[["bat"],["nat","tan"],["ate","eat","tea"]]`

```java
public List<List<String>> groupAnagrams(String[] strs) {
    Map<String, List<String>> map = new HashMap<>();
    for (String s : strs) {
        char[] chars = s.toCharArray();
        Arrays.sort(chars);
        String key = new String(chars); // sorted string is the key
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }
    return new ArrayList<>(map.values());
}
// "eat" → sorted → "aet" (key)
// "tea" → sorted → "aet" (same key) → grouped together
```

---

## Q29. Longest subarray with sum K

**Input:** `[1, 2, 3, -1, 1]`, `K = 3` → **Output:** `3` (subarray `[1,2]` or `[3]` or `[3,-1,1]`)

```java
public int longestSubarrayWithSumK(int[] nums, int k) {
    Map<Integer, Integer> prefixSumIndex = new HashMap<>();
    prefixSumIndex.put(0, -1); // sum 0 exists at index -1 (before array)
    int sum = 0, maxLen = 0;
    for (int i = 0; i < nums.length; i++) {
        sum += nums[i];
        if (prefixSumIndex.containsKey(sum - k)) {
            maxLen = Math.max(maxLen, i - prefixSumIndex.get(sum - k));
        }
        prefixSumIndex.putIfAbsent(sum, i); // only store first occurrence
    }
    return maxLen;
}
// Prefix sum trick: sum[i] - sum[j] = k means subarray (j+1)..i has sum k
```

---

# SECTION 9 — COMPLEXITY CHEAT SHEET

```
O(1)       — HashMap get/put, array access by index
O(log n)   — Binary search, BST operations, heap operations
O(n)       — Single loop, linear scan
O(n log n) — Merge sort, heap sort, good sorting algorithms
O(n²)      — Nested loops, bubble/selection sort
O(2^n)     — Naive recursion (Fibonacci), subset generation
```

**Space complexity:**
```
O(1)   — Two pointers, in-place operations
O(n)   — HashMap, extra array, recursion call stack depth = n
O(h)   — Tree recursion where h = height
O(log n) — Binary search recursion, balanced tree recursion
```

---

# SECTION 10 — QUESTIONS THEY MIGHT ASK VERBALLY (no coding)

**Q: What is the difference between Array and LinkedList?**
> "Array: contiguous memory, O(1) random access, O(n) insert/delete in middle, fixed size. LinkedList: non-contiguous, O(n) random access, O(1) insert/delete if you have the node, dynamic size. Use Array when you mostly read. Use LinkedList when you mostly insert/delete."

**Q: What is a Stack? Where is it used?**
> "LIFO — Last In First Out. Operations: push (add to top), pop (remove from top), peek (see top). Used in: function call stack (method calls), undo operations (Ctrl+Z), browser back button, valid parentheses checking, expression evaluation."

**Q: What is a Queue? Where is it used?**
> "FIFO — First In First Out. Operations: enqueue (add to back), dequeue (remove from front). Used in: printer job queue, BFS traversal, message queues (Kafka, RabbitMQ), CPU scheduling."

**Q: What is recursion and what is a base case?**
> "Recursion is a method that calls itself. Base case is the condition where it stops — without it, you get infinite recursion and StackOverflowError. Every recursive problem can be solved iteratively, but recursion is cleaner for problems with natural sub-structure like trees."

**Q: What is the difference between BFS and DFS?**
> "BFS (Breadth-First Search) explores level by level — uses a Queue. Finds shortest path. DFS (Depth-First Search) explores one branch as deep as possible before backtracking — uses Stack or recursion. BFS: shortest path in unweighted graph. DFS: topological sort, cycle detection, maze solving."

**Q: What is hashing and what is a collision?**
> "Hashing converts a key to an array index using a hash function. Collision is when two different keys produce the same index. Java's HashMap handles collision by chaining (LinkedList in the bucket). Since Java 8, if a bucket has more than 8 entries, it converts to a Red-Black Tree."

---

# QUICK REVISION — 5 Minutes Before Interview

| Problem | Technique | Time |
|---------|-----------|------|
| Two Sum | HashMap | O(n) |
| Duplicate in array | HashSet / Floyd's | O(n) |
| Max subarray | Kadane's | O(n) |
| Palindrome | Two pointers | O(n) |
| Anagram | char count array | O(n) |
| Reverse linked list | prev/curr/next | O(n) |
| Cycle detection | Slow/fast pointer | O(n) |
| Valid parentheses | Stack | O(n) |
| Binary search | left/right/mid | O(log n) |
| Tree height | Recursion | O(n) |

---

**Remember: Talk while you code. An interviewer who sees your thinking can give partial credit and hints. Silence is the worst thing in a coding interview.**
