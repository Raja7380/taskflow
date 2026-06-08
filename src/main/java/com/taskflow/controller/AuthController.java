package com.taskflow.controller;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUTH CONTROLLER — HTTP endpoints for authentication.
 *
 * Endpoints:
 *   POST /api/auth/register  — Create a new account
 *   POST /api/auth/login     — Log in and get JWT tokens
 *
 * ANNOTATIONS:
 *   @RestController = @Controller + @ResponseBody
 *     @Controller = "I handle HTTP requests"
 *     @ResponseBody = "Return JSON, not HTML views"
 *
 *   @RequestMapping("/api/auth") = base URL prefix for all methods in this class
 *     So @PostMapping("/register") becomes POST /api/auth/register
 *
 *   @Valid = triggers Bean Validation on the request body
 *     If validation fails, MethodArgumentNotValidException is thrown
 *     Our GlobalExceptionHandler catches it and returns field-level errors
 *
 *   @RequestBody = "Parse the JSON request body into this Java object"
 *     Spring uses Jackson (JSON library) to deserialize:
 *     {"fullName": "Raja", "email": "..."} -> RegisterRequest object
 *
 * INTERVIEW Q: What's the difference between @Controller and @RestController?
 * A: @Controller returns view names (HTML). @RestController returns data (JSON).
 *    @RestController = @Controller + @ResponseBody.
 *    For REST APIs, always use @RestController.
 *
 * INTERVIEW Q: What does @Valid do?
 * A: Triggers validation annotations on the DTO (@NotBlank, @Email, @Size, etc.).
 *    If any validation fails, Spring throws MethodArgumentNotValidException
 *    BEFORE the method body executes. The controller method is never called!
 *
 * SWAGGER ANNOTATIONS:
 *   @Tag = groups endpoints in Swagger UI
 *   @Operation = describes what each endpoint does
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * REGISTER — Create a new user account.
     *
     * HTTP: POST /api/auth/register
     * Body: { "fullName": "Raja Singh", "email": "raja@email.com", "password": "password123" }
     *
     * Responses:
     *   201 Created — registration successful, returns JWT tokens
     *   400 Bad Request — validation errors (invalid email, short password, etc.)
     *   409 Conflict — email already registered
     *
     * WHY return 201 (not 200)?
     *   HTTP 200 = "OK, request processed"
     *   HTTP 201 = "Created — a new resource was created"
     *   Since we're CREATING a new user, 201 is more precise.
     *   Small detail but shows you understand HTTP semantics.
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * LOGIN — Authenticate and get JWT tokens.
     *
     * HTTP: POST /api/auth/login
     * Body: { "email": "raja@email.com", "password": "password123" }
     *
     * Responses:
     *   200 OK — login successful, returns JWT tokens
     *   401 Unauthorized — wrong email or password
     *
     * After successful login, the client should:
     *   1. Store the accessToken (for API calls)
     *   2. Store the refreshToken (to renew access token)
     *   3. Send accessToken in every request: "Authorization: Bearer <token>"
     */
    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
