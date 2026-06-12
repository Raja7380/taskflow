# Java Web Technologies — Complete Interview Guide
### JSP, Servlets, JSTL, EL, Filters, Sessions, MVC — Everything inside Java

---

## WHY THESE TECHNOLOGIES EXIST

Before React/Angular existed, Java was used for EVERYTHING — frontend too.
Companies like Newgen built their products in the 2000s-2010s when JSP was the standard.
Even today, many enterprise systems (banks, insurance, ERP) still run JSP/Servlet code.

**Evolution of Java Web:**
```
Servlet (1997)  → just Java, HTML mixed in Java code
JSP (1999)      → HTML with Java code inside (opposite of Servlet)
JSTL (2002)     → standard tags for JSP (no Java in HTML)
JSF (2004)      → component-based framework (like Angular for Java)
Spring MVC      → cleaner MVC over Servlets
Thymeleaf       → modern template engine (Spring Boot default)
React/Angular   → JavaScript takes over frontend (2013+)
```

---

# SECTION 1 — SERVLET (Foundation of everything)

## What is a Servlet?

A **Servlet** is a Java class that handles HTTP requests and sends HTTP responses.
It runs inside a **Servlet Container** (like Apache Tomcat).

Without Servlet:
- Browser sends `GET /users` → nobody handles it → 404

With Servlet:
- Browser sends `GET /users` → Tomcat finds `UserServlet` → runs `doGet()` → sends HTML back

```java
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

// This class handles HTTP requests to /hello
@WebServlet("/hello")
public class HelloServlet extends HttpServlet {

    // Called for GET requests
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String name = req.getParameter("name"); // GET /hello?name=Raja

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html><body>");
        out.println("<h1>Hello, " + name + "!</h1>");
        out.println("</body></html>");
    }

    // Called for POST requests
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        // process login...
    }
}
```

**Problem with Servlets:** HTML inside Java code = nightmare to maintain. Changing a color requires recompiling Java. → JSP was invented.

---

## Servlet Lifecycle — THE Most Asked Servlet Question

```
1. init()     → called ONCE when servlet is first loaded
               → initialize resources (DB connection, config)
               → like a constructor but for servlets

2. service()  → called for EVERY request
               → determines if GET/POST/PUT/DELETE
               → calls doGet() / doPost() accordingly

3. destroy()  → called ONCE when server shuts down
               → release resources (close DB connections)
               → like a destructor
```

```java
public class MyServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        // called once — load config, open connections
        System.out.println("Servlet initialized");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // called for every GET request
    }

    @Override
    public void destroy() {
        // called once — close connections, cleanup
        System.out.println("Servlet destroyed");
    }
}
```

**Interview answer:**
> "Servlet lifecycle has 3 phases: init() called once when servlet loads — used for one-time setup. service() called for every request — it routes to doGet/doPost/doPut etc. destroy() called once when server shuts down — used to release resources."

---

## HttpServletRequest — Reading the request

```java
// URL: GET /search?keyword=java&page=2
String keyword = req.getParameter("keyword");     // "java"
String page    = req.getParameter("page");        // "2"

// All parameters
Enumeration<String> names = req.getParameterNames();

// POST form data (same method)
String username = req.getParameter("username");

// Request info
String method  = req.getMethod();          // "GET" or "POST"
String url     = req.getRequestURL().toString(); // "http://localhost:8080/search"
String uri     = req.getRequestURI();      // "/search"
String ip      = req.getRemoteAddr();      // "127.0.0.1"

// Headers
String auth    = req.getHeader("Authorization");
String type    = req.getContentType();

// Session (see Session section)
HttpSession session = req.getSession();

// Attributes (request-scoped data set by other code)
req.setAttribute("user", userObject);
User user = (User) req.getAttribute("user");
```

---

## HttpServletResponse — Building the response

```java
// Set response type
resp.setContentType("text/html");
resp.setContentType("application/json");

// Set status code
resp.setStatus(200);                  // OK
resp.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401

// Write HTML
PrintWriter out = resp.getWriter();
out.println("<h1>Hello</h1>");

// Set header
resp.setHeader("Content-Type", "application/json");

// Redirect — send browser to a new URL
resp.sendRedirect("/dashboard");

// Forward — server-side, browser URL doesn't change
RequestDispatcher rd = req.getRequestDispatcher("/dashboard.jsp");
rd.forward(req, resp);
```

**Redirect vs Forward:**
```
sendRedirect:
  Client → Server: GET /login
  Server → Client: 302 Redirect to /dashboard
  Client → Server: GET /dashboard   (browser URL changes, new request)

forward:
  Client → Server: GET /login
  Server internally goes to /dashboard.jsp
  Server → Client: response from /dashboard.jsp
  (browser URL stays /login, same request object)
```

---

## web.xml — Servlet Configuration (old way, before annotations)

```xml
<web-app>
    <!-- Register the servlet -->
    <servlet>
        <servlet-name>HelloServlet</servlet-name>
        <servlet-class>com.example.HelloServlet</servlet-class>
        <load-on-startup>1</load-on-startup>  <!-- init on server start -->
    </servlet>

    <!-- Map URL to servlet -->
    <servlet-mapping>
        <servlet-name>HelloServlet</servlet-name>
        <url-pattern>/hello</url-pattern>
    </servlet-mapping>
</web-app>
```

Modern approach (annotations, no XML needed):
```java
@WebServlet(urlPatterns = {"/hello", "/greet"})
public class HelloServlet extends HttpServlet { }
```

---

# SECTION 2 — JSP (JavaServer Pages)

## What is JSP?

JSP = HTML file with Java code embedded inside `<% %>` tags.

Tomcat **compiles JSP into a Servlet** automatically. The JSP you write:
```jsp
<html>
<body>
  <h1>Hello, <%= request.getParameter("name") %>!</h1>
</body>
</html>
```
Gets compiled into:
```java
out.println("<html><body>");
out.println("<h1>Hello, " + request.getParameter("name") + "!</h1>");
out.println("</body></html>");
```

**JSP vs Servlet:**
```
Servlet: Java class, HTML inside Java strings → good for logic, bad for HTML
JSP: HTML file, Java inside tags → good for HTML, bad for complex logic
Best practice: Servlet for logic + JSP for display (MVC pattern)
```

---

## JSP Scripting Elements

```jsp
<%-- 1. Scriptlet — execute Java code --%>
<%
    String name = request.getParameter("name");
    int age = Integer.parseInt(request.getParameter("age"));
    if (age >= 18) {
%>
    <p>Welcome, adult user: <%= name %></p>
<%
    } else {
%>
    <p>Sorry, you must be 18+</p>
<%
    }
%>

<%-- 2. Expression — print value directly (note: no semicolon) --%>
<p>Hello, <%= name %></p>
<p>Current time: <%= new java.util.Date() %></p>

<%-- 3. Declaration — declare methods or variables at class level --%>
<%!
    int counter = 0;
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
%>

<%-- 4. Comment — not sent to browser --%>
<%-- This is a JSP comment, invisible in page source --%>
<!-- This is HTML comment, visible in page source -->
```

---

## JSP Implicit Objects — Available Without Declaration

These 9 objects are available in every JSP without importing or creating:

| Object | Type | Use |
|--------|------|-----|
| `request` | HttpServletRequest | Read request params, headers |
| `response` | HttpServletResponse | Set headers, redirect |
| `session` | HttpSession | Store user session data |
| `application` | ServletContext | App-wide shared data |
| `out` | JspWriter | Write to response |
| `config` | ServletConfig | Servlet init params |
| `pageContext` | PageContext | Access all other scopes |
| `page` | Object | Reference to this JSP (like `this`) |
| `exception` | Throwable | Only in error pages |

```jsp
<%-- Using implicit objects --%>
<p>Your IP: <%= request.getRemoteAddr() %></p>
<p>User: <%= session.getAttribute("loggedInUser") %></p>
<p>App name: <%= application.getServletContextName() %></p>
<% out.println("Hello from out object"); %>
```

---

## JSP Directives — Instructions to the JSP compiler

```jsp
<%-- 1. page directive — settings for this JSP page --%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
         pageEncoding="UTF-8"
         import="java.util.*, com.example.User"
         errorPage="/error.jsp"
         isErrorPage="false"
         session="true" %>

<%-- 2. include directive — include another file at compile time --%>
<%@ include file="header.jsp" %>

<%-- 3. taglib directive — import a tag library (JSTL) --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
```

---

## JSP Actions — XML-style tags for JSP operations

```jsp
<%-- Include another JSP at request time (dynamic) --%>
<jsp:include page="header.jsp" />

<%-- Forward to another resource --%>
<jsp:forward page="dashboard.jsp" />

<%-- Create a JavaBean --%>
<jsp:useBean id="user" class="com.example.User" scope="session" />

<%-- Set a property on the bean --%>
<jsp:setProperty name="user" property="name" value="Raja" />

<%-- Get a property from the bean --%>
<jsp:getProperty name="user" property="name" />
```

---

## JSP Scopes — Where is data visible?

```
page scope      → data visible only in this JSP page (default)
request scope   → data visible in this request (including forwards)
session scope   → data visible for this user's entire session
application scope → data visible to ALL users of the app
```

```java
// Setting data in different scopes
pageContext.setAttribute("data", value, PageContext.PAGE_SCOPE);
request.setAttribute("data", value);          // request scope
session.setAttribute("data", value);          // session scope
application.setAttribute("data", value);      // application scope

// Getting data
pageContext.getAttribute("data");             // page scope only
request.getAttribute("data");                 // request scope
session.getAttribute("data");                 // session scope
application.getAttribute("data");             // application scope
```

---

# SECTION 3 — JSTL (JSP Standard Tag Library)

## Why JSTL exists?

This JSP code is ugly and hard to maintain:
```jsp
<% if (user != null && user.isAdmin()) { %>
  <p>Welcome Admin!</p>
<% } else { %>
  <p>Welcome User!</p>
<% } %>
```

JSTL makes it readable HTML-like tags:
```jsp
<c:if test="${user != null && user.admin}">
  <p>Welcome Admin!</p>
</c:if>
```

---

## JSTL Core Tags — `<%@ taglib prefix="c" uri="...jstl/core" %>`

```jsp
<%-- c:out — safe output (escapes HTML characters) --%>
<c:out value="${user.name}" default="Guest" />
<!-- Even if user.name = "<script>alert(1)</script>", it's safe -->

<%-- c:set — create/set a variable --%>
<c:set var="greeting" value="Hello World" scope="request" />
<c:set var="count" value="${items.size()}" />

<%-- c:remove — remove a variable --%>
<c:remove var="greeting" />

<%-- c:if — conditional rendering --%>
<c:if test="${user.age >= 18}">
  <p>Adult content allowed</p>
</c:if>

<%-- c:choose — if/else if/else --%>
<c:choose>
  <c:when test="${score >= 90}">Grade: A</c:when>
  <c:when test="${score >= 80}">Grade: B</c:when>
  <c:when test="${score >= 70}">Grade: C</c:when>
  <c:otherwise>Grade: F</c:otherwise>
</c:choose>

<%-- c:forEach — loop over a list --%>
<c:forEach var="user" items="${users}">
  <p>${user.name} - ${user.email}</p>
</c:forEach>

<%-- c:forEach with index --%>
<c:forEach var="item" items="${products}" varStatus="status">
  <p>${status.index + 1}. ${item.name} - ${item.price}</p>
  <!-- status.index, status.count, status.first, status.last -->
</c:forEach>

<%-- c:forTokens — split a string and loop --%>
<c:forTokens var="color" items="red,green,blue" delims=",">
  <span>${color}</span>
</c:forTokens>

<%-- c:redirect — redirect to URL --%>
<c:redirect url="/dashboard" />

<%-- c:url — build URL with context path --%>
<a href="<c:url value='/profile' />">My Profile</a>

<%-- c:import — include external content --%>
<c:import url="header.jsp" />

<%-- c:catch — catch exceptions --%>
<c:catch var="error">
  <%-- code that might throw exception --%>
</c:catch>
<c:if test="${error != null}">Error: ${error.message}</c:if>
```

---

## JSTL Formatting Tags — `<%@ taglib prefix="fmt" uri="...jstl/fmt" %>`

```jsp
<%-- Format numbers --%>
<fmt:formatNumber value="${price}" type="currency" />
<!-- Output: ₹1,234.56 -->

<fmt:formatNumber value="${0.75}" type="percent" />
<!-- Output: 75% -->

<fmt:formatNumber value="${1234567.89}" pattern="#,##0.00" />
<!-- Output: 1,234,567.89 -->

<%-- Format dates --%>
<fmt:formatDate value="${user.createdAt}" pattern="dd-MM-yyyy" />
<!-- Output: 15-06-2024 -->

<fmt:formatDate value="${now}" dateStyle="full" />
<!-- Output: Tuesday, June 15, 2026 -->

<%-- Parse a date string --%>
<fmt:parseDate var="date" value="2024-01-15" pattern="yyyy-MM-dd" />

<%-- Set locale --%>
<fmt:setLocale value="en_IN" />

<%-- Messages from properties file (internationalization) --%>
<fmt:setBundle basename="messages" />
<fmt:message key="welcome.message" />
```

---

## JSTL Functions — `<%@ taglib prefix="fn" uri="...jstl/functions" %>`

```jsp
<!-- String functions -->
${fn:length(users)}                    <!-- list size / string length -->
${fn:toUpperCase(user.name)}           <!-- RAJA -->
${fn:toLowerCase(user.email)}          <!-- raja@email.com -->
${fn:trim(text)}                       <!-- remove spaces -->
${fn:contains(text, "java")}           <!-- true/false -->
${fn:startsWith(url, "https")}         <!-- true/false -->
${fn:endsWith(file, ".pdf")}           <!-- true/false -->
${fn:replace(text, "old", "new")}      <!-- replace text -->
${fn:substring(text, 0, 5)}            <!-- extract part -->
${fn:split(csv, ",")}                  <!-- split to array -->
${fn:join(array, ", ")}                <!-- join array to string -->
${fn:indexOf(text, "java")}            <!-- position (-1 if not found) -->
${fn:escapeXml(html)}                  <!-- escape < > & " -->
```

---

# SECTION 4 — EL (Expression Language)

## What is EL?

EL (Expression Language) is a simpler syntax to access data in JSP without Java code.

```jsp
<!-- Without EL (old way): -->
<% String name = (String) request.getAttribute("name"); %>
<p><%= name %></p>

<!-- With EL (clean): -->
<p>${name}</p>
```

## EL Syntax

```jsp
<%-- Access attributes from scopes (auto-searches page→request→session→application) --%>
${name}
${user.name}              <!-- user is an object, access .name property -->
${user["email"]}          <!-- same as user.email, bracket notation -->
${users[0].name}          <!-- first element of users list -->
${map["key"]}             <!-- access map by key -->

<%-- Math operators --%>
${price * quantity}
${total - discount}
${10 / 3}                 <!-- 3.3333 -->
${10 div 3}               <!-- same -->
${10 % 3}                 <!-- 1 (modulo) -->
${10 mod 3}               <!-- same -->

<%-- Comparison operators --%>
${age >= 18}              <!-- true/false -->
${age ge 18}              <!-- same (text form) -->
${name == "Raja"}         <!-- equality -->
${name eq "Raja"}         <!-- same -->
${name != "Raja"}         <!-- not equal -->
${name ne "Raja"}         <!-- same -->

<%-- Logical operators --%>
${age >= 18 && role == "ADMIN"}
${age >= 18 and role == "ADMIN"}  <!-- same -->
${isAdmin || isModerator}
${isAdmin or isModerator}

<%-- Ternary --%>
${age >= 18 ? "Adult" : "Minor"}

<%-- Empty check --%>
${empty users}            <!-- true if null or empty collection/string -->
${not empty users}        <!-- true if not null and not empty -->

<%-- Null-safe --%>
${user.address.city}      <!-- throws NullPointerException if address is null -->
<!-- Use: -->
${not empty user.address ? user.address.city : "Unknown"}
```

---

# SECTION 5 — SESSION MANAGEMENT

## What is a Session?

HTTP is **stateless** — each request is independent. The server doesn't remember you.

Without sessions: Every page would require login again.

Sessions solve this: Server assigns a unique ID (session ID) to each user, stores data on the server side, sends the ID as a cookie. Browser sends the cookie on every request → server finds the session data.

```
User logs in:
  Browser → Server: POST /login (username=raja, password=123)
  Server: validates, creates session, stores {user: raja}
  Server → Browser: 200 OK + Set-Cookie: JSESSIONID=ABC123

Next request:
  Browser → Server: GET /dashboard + Cookie: JSESSIONID=ABC123
  Server: finds session ABC123, knows user is raja
  Server → Browser: Raja's dashboard
```

---

## HttpSession API

```java
// Get session (create if doesn't exist)
HttpSession session = request.getSession();

// Get session (don't create if doesn't exist — returns null)
HttpSession session = request.getSession(false);

// Store data
session.setAttribute("loggedInUser", userObject);
session.setAttribute("cartItems", itemList);

// Read data
User user = (User) session.getAttribute("loggedInUser");

// Remove specific data
session.removeAttribute("cartItems");

// Get session ID
String sessionId = session.getId();  // "ABC123XYZ"

// Set session timeout (in seconds)
session.setMaxInactiveInterval(30 * 60); // 30 minutes

// Check if session is new
boolean isNew = session.isNew();

// Invalidate session (logout)
session.invalidate();  // removes all attributes, session ends
```

---

## Session vs Cookie vs Token — The Big 3

| | Session | Cookie | JWT Token |
|---|---------|--------|-----------|
| Stored where | Server RAM/DB | Browser | Browser (localStorage/cookie) |
| What browser sends | Session ID (cookie) | Data itself | Token |
| Scalability | Hard (must reach same server) | Easy | Easy (stateless) |
| Security | More secure (data on server) | Less secure (data in browser) | Secure if HTTPS |
| Used in | Traditional web apps (JSP) | Remember preferences | REST APIs (your TaskFlow) |

> "In my TaskFlow project I use JWT tokens — stateless, scalable, no server-side session storage needed. In traditional JSP apps, HttpSession was the standard approach."

---

## Cookie API

```java
// Create a cookie
Cookie cookie = new Cookie("username", "raja");
cookie.setMaxAge(7 * 24 * 60 * 60);  // 7 days in seconds
cookie.setPath("/");                   // available on all paths
cookie.setHttpOnly(true);             // can't be accessed by JavaScript (security)
cookie.setSecure(true);               // only sent over HTTPS
response.addCookie(cookie);

// Read cookies
Cookie[] cookies = request.getCookies();
if (cookies != null) {
    for (Cookie c : cookies) {
        if (c.getName().equals("username")) {
            String username = c.getValue();
        }
    }
}

// Delete a cookie (set maxAge to 0)
Cookie deleteCookie = new Cookie("username", "");
deleteCookie.setMaxAge(0);
response.addCookie(deleteCookie);
```

---

# SECTION 6 — FILTERS

## What is a Filter?

A Filter intercepts every request BEFORE it reaches the Servlet, and every response AFTER the Servlet processes it.

Like a security checkpoint — every request must pass through it.

**Real uses of Filters:**
- Authentication check (is user logged in?)
- Logging every request
- Compressing response (GZIP)
- Setting character encoding
- CORS headers for REST APIs
- Rate limiting

```
Browser → Filter1 → Filter2 → Filter3 → Servlet
Browser ← Filter1 ← Filter2 ← Filter3 ← Servlet
(Filter chain — each filter calls chain.doFilter() to pass to next)
```

```java
@WebFilter("/*")  // applies to ALL URLs
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig config) throws ServletException {
        // called once when filter loads
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request  = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) resp;

        String path = request.getRequestURI();

        // Allow login page without auth check
        if (path.contains("/login") || path.contains("/register")) {
            chain.doFilter(req, resp); // pass to next filter / servlet
            return;
        }

        // Check if user is logged in
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {
            response.sendRedirect("/login");  // not logged in → redirect
            return;
        }

        // Logged in — continue
        long startTime = System.currentTimeMillis();
        chain.doFilter(req, resp); // pass request down the chain

        // Code here runs AFTER response is sent
        long duration = System.currentTimeMillis() - startTime;
        System.out.println(path + " took " + duration + "ms");
    }

    @Override
    public void destroy() {
        // called once when filter is removed
    }
}
```

**Relation to your TaskFlow project:**
> "In TaskFlow I use `JwtAuthenticationFilter` which extends `OncePerRequestFilter` — same concept as a Servlet Filter. It intercepts every request, validates the JWT token, and sets the authentication in Spring Security context."

---

## Filter for character encoding (classic use case)

```java
@WebFilter("/*")
public class EncodingFilter implements Filter {
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        req.setCharacterEncoding("UTF-8");   // fix Hindi/regional language input
        resp.setCharacterEncoding("UTF-8");
        chain.doFilter(req, resp);
    }
}
```

---

# SECTION 7 — LISTENERS

A Listener monitors events in the web application lifecycle.

```java
// 1. App starts/stops — initialize shared resources
@WebListener
public class AppStartupListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        // App starting — set up DB connection pool, load config
        ServletContext context = event.getServletContext();
        context.setAttribute("appVersion", "1.0.0");
        System.out.println("App started!");
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        // App shutting down — close connections
        System.out.println("App shutting down!");
    }
}

// 2. Session created/destroyed — track active users
@WebListener
public class SessionListener implements HttpSessionListener {

    private static int activeSessions = 0;

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        activeSessions++;
        System.out.println("New session. Active: " + activeSessions);
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        activeSessions--;
        System.out.println("Session ended. Active: " + activeSessions);
    }
}

// 3. Request started/ended — logging
@WebListener
public class RequestListener implements ServletRequestListener {

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        HttpServletRequest req = (HttpServletRequest) event.getServletRequest();
        System.out.println("Request: " + req.getRequestURI());
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        System.out.println("Request completed");
    }
}
```

---

# SECTION 8 — MVC PATTERN IN JAVA WEB

## What is MVC?

MVC = Model-View-Controller. Separates the application into 3 layers so each part has one responsibility.

```
Model      → Data + Business logic  (Java classes, DB operations)
View       → UI / Display           (JSP pages, HTML)
Controller → Bridge between M and V (Servlet — receives request, calls Model, forwards to View)
```

**Without MVC (bad — mixed everything in JSP):**
```jsp
<%-- BAD: DB query + business logic + HTML all in one JSP --%>
<%
    Connection conn = DriverManager.getConnection("jdbc:mysql://...");
    Statement st = conn.createStatement();
    ResultSet rs = st.executeQuery("SELECT * FROM users WHERE active = 1");
    while (rs.next()) {
%>
    <p><%= rs.getString("name") %></p>
<% } %>
```

**With MVC (good — each layer has one job):**

```java
// Controller (Servlet) — receives request, calls service, forwards to JSP
@WebServlet("/users")
public class UserController extends HttpServlet {
    private UserService userService = new UserService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // 1. Call Model (business logic)
        List<User> users = userService.getAllActiveUsers();

        // 2. Put data in request scope
        req.setAttribute("users", users);

        // 3. Forward to View (JSP)
        req.getRequestDispatcher("/WEB-INF/users.jsp").forward(req, resp);
    }
}
```

```java
// Model (Service + DAO)
public class UserService {
    private UserDAO userDAO = new UserDAO();
    public List<User> getAllActiveUsers() {
        return userDAO.findByActive(true);
    }
}
```

```jsp
<%-- View (JSP) — only displays data, no business logic --%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html><body>
<c:forEach var="user" items="${users}">
    <p>${user.name} - ${user.email}</p>
</c:forEach>
</body></html>
```

> "In my TaskFlow project, Spring MVC takes this pattern further. `@RestController` = Controller, `@Service` = Model logic, Spring handles the routing automatically. No need to write Servlet mapping manually."

---

# SECTION 9 — JDBC (Java Database Connectivity)

## What is JDBC?

JDBC is the standard API to connect Java to any relational database.
JPA/Hibernate uses JDBC internally — if you know JDBC, you understand what Hibernate does underneath.

```java
// Full JDBC example — connect, query, read results, close
public List<User> getAllUsers() {
    List<User> users = new ArrayList<>();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        // 1. Load driver (not needed in modern JDBC)
        Class.forName("com.mysql.cj.jdbc.Driver");

        // 2. Get connection
        conn = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/taskflow",
            "root",
            "password"
        );

        // 3. Create prepared statement (prevents SQL injection)
        stmt = conn.prepareStatement(
            "SELECT id, name, email FROM users WHERE active = ?"
        );
        stmt.setBoolean(1, true); // set the ? parameter

        // 4. Execute query
        rs = stmt.executeQuery();

        // 5. Read results
        while (rs.next()) {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setEmail(rs.getString("email"));
            users.add(user);
        }

    } catch (Exception e) {
        e.printStackTrace();
    } finally {
        // 6. Always close (use try-with-resources in real code)
        try { if (rs != null) rs.close(); } catch (Exception e) {}
        try { if (stmt != null) stmt.close(); } catch (Exception e) {}
        try { if (conn != null) conn.close(); } catch (Exception e) {}
    }
    return users;
}
```

## Statement vs PreparedStatement vs CallableStatement

| | Statement | PreparedStatement | CallableStatement |
|---|-----------|------------------|-------------------|
| Use | Simple queries, no params | Queries with parameters | Stored procedures |
| SQL Injection | Vulnerable | Safe (parameterized) | Safe |
| Performance | Compiled every time | Pre-compiled (faster) | - |
| Example | `"SELECT * FROM users"` | `"SELECT * FROM users WHERE id=?"` | `"{call get_user(?)}"` |

```java
// Statement — NEVER use with user input (SQL injection risk)
Statement st = conn.createStatement();
st.executeQuery("SELECT * FROM users WHERE name = '" + userInput + "'");
// If userInput = "' OR '1'='1" → returns all users!

// PreparedStatement — ALWAYS use with parameters
PreparedStatement ps = conn.prepareStatement(
    "SELECT * FROM users WHERE name = ?"
);
ps.setString(1, userInput); // safely parameterized
```

## JDBC Execute Methods

```java
executeQuery()   → returns ResultSet (for SELECT)
executeUpdate()  → returns int (rows affected, for INSERT/UPDATE/DELETE)
execute()        → returns boolean (true if SELECT, false if update)
executeBatch()   → execute multiple statements at once
```

---

# SECTION 10 — THYMELEAF (Modern Spring Boot Template)

Modern replacement for JSP in Spring Boot applications.

```html
<!-- Thymeleaf template — looks like normal HTML, browser-renderable -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><title>Users</title></head>
<body>

  <!-- Text output -->
  <h1 th:text="${user.name}">Default Name</h1>

  <!-- Inline text (keeps surrounding HTML) -->
  <p>Hello <span th:text="${user.name}">User</span>!</p>

  <!-- Conditional -->
  <div th:if="${user.admin}">
    <p>Admin Panel</p>
  </div>
  <div th:unless="${user.admin}">
    <p>Regular user</p>
  </div>

  <!-- Loop -->
  <ul>
    <li th:each="user : ${users}" th:text="${user.name}">User</li>
  </ul>

  <!-- Loop with status -->
  <tr th:each="user, stat : ${users}">
    <td th:text="${stat.count}">1</td>
    <td th:text="${user.name}">Name</td>
  </tr>

  <!-- URL building -->
  <a th:href="@{/users/{id}(id=${user.id})}">View Profile</a>

  <!-- Form -->
  <form th:action="@{/users}" th:object="${userForm}" method="post">
    <input type="text" th:field="*{name}" />
    <input type="email" th:field="*{email}" />
    <button type="submit">Save</button>
  </form>

  <!-- Switch/case -->
  <div th:switch="${user.role}">
    <p th:case="'ADMIN'">Admin User</p>
    <p th:case="'USER'">Regular User</p>
    <p th:case="*">Unknown Role</p>
  </div>

</body>
</html>
```

**JSP vs Thymeleaf:**
```
JSP:       Requires Tomcat to render. Can't open in browser directly.
Thymeleaf: Valid HTML. Opens in browser as-is (default text shown).
           "Natural templates" — designer can work on them without running the server.
```

---

# SECTION 11 — INTERVIEW Q&A

**Q: What is the difference between GET and POST?**
> "GET sends data in the URL (query string) — visible in browser, bookmarkable, cached, idempotent. POST sends data in the request body — not visible in URL, not cached, used for form submissions and creating data. Rule: GET for reading, POST for creating/modifying. Sensitive data (passwords) must always use POST."

**Q: What is the difference between forward and redirect?**
> "forward: server-side, same request object, browser URL doesn't change, faster (one request). redirect: server tells browser to make a new request to a different URL (302 response), browser URL changes, two requests total. Use forward when you want to pass data to JSP. Use redirect after form submit (POST/Redirect/GET pattern) to prevent duplicate submission on browser refresh."

**Q: What is JSP and how is it different from Servlet?**
> "JSP is an HTML file with embedded Java code. Servlet is a pure Java class that generates HTML. Both do the same thing — JSP is compiled into a Servlet by the container. JSP is better for presentation, Servlet is better for logic. In MVC pattern, Servlet is the Controller and JSP is the View."

**Q: What are the implicit objects in JSP?**
> "9 implicit objects available without declaring: request, response, session, application, out, config, pageContext, page, exception. Most important: request (read params), session (user data across requests), out (write to response), application (app-wide shared data)."

**Q: What is a Filter and how is it different from a Servlet?**
> "Servlet handles a specific URL. Filter intercepts ALL requests (or a pattern) before and after they reach the Servlet. Filter has a chain — multiple filters can be chained. Used for authentication, logging, encoding, CORS headers. Key method is doFilter() which must call chain.doFilter() to pass the request forward."

**Q: What is session management? How does it work?**
> "HTTP is stateless. Sessions allow the server to remember users across requests. Server creates a session with a unique JSESSIONID, sends it as a cookie to the browser. Browser sends the cookie on every request. Server looks up the session by ID and retrieves stored data. Session can be invalidated on logout."

**Q: What is SQL injection and how does PreparedStatement prevent it?**
> "SQL injection is when user input is treated as SQL code. Example: input `' OR '1'='1` in a username field. With Statement: `WHERE name = '' OR '1'='1'` returns all rows. PreparedStatement treats `?` parameters as data, not SQL. The input is escaped automatically — `' OR '1'='1` becomes a literal string, not SQL code."

**Q: What is the MVC pattern?**
> "Model-View-Controller separates responsibilities. Model = data and business logic. View = presentation (JSP/HTML). Controller = bridge (Servlet) that receives requests, calls model, and forwards to view. Benefits: each layer can change independently, code is testable and maintainable. In Spring MVC, @Controller handles routing, @Service is the model, templates/JSON is the view."

**Q: What is JSTL and why use it instead of scriptlets?**
> "JSTL is a tag library for JSP with HTML-like tags for common operations — loops, conditions, output, formatting. Scriptlets mix Java code in HTML making it hard to read and maintain. JSTL separates logic from presentation, is readable, and designers can work with it. Best practice: no scriptlets in JSP, use JSTL + EL only."

**Q: Difference between include directive and jsp:include action?**
> "Include directive (`<%@ include file='...' %>`) is compile-time include — content is merged at JSP compile time. Like copy-paste. jsp:include is runtime include — file is processed when the request runs. Use directive for static content (headers, footers). Use action when included content needs to be dynamic per-request."

---

# QUICK CHEAT SHEET

```
Servlet          → Java class, handles HTTP, doGet()/doPost()
JSP              → HTML + Java, compiled to Servlet by Tomcat
JSTL             → <c:forEach>, <c:if>, <c:choose> — no Java in HTML
EL               → ${user.name} — clean data access
Filter           → intercepts all requests (auth, logging)
Listener         → monitors app/session/request events
Session          → server-side user data via JSESSIONID cookie
PreparedStatement → parameterized SQL, prevents injection
MVC              → Servlet(Controller) + JSP(View) + Java class(Model)
Thymeleaf        → modern JSP replacement, natural HTML templates
JDBC             → raw Java-to-DB API (Hibernate uses this underneath)
forward()        → server-side, same request, URL unchanged
sendRedirect()   → new browser request, URL changes
```
