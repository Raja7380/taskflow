package com.taskflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * REGISTER REQUEST DTO — What the client sends to register a new user.
 *
 * WHY DTOs (Data Transfer Objects)?
 *   NEVER expose your Entity directly in API requests/responses!
 *
 *   Problems with exposing Entity:
 *     1. Security: Client could set role=ADMIN, id=1, or other fields you don't want
 *     2. Coupling: Changing DB schema changes your API (breaks clients!)
 *     3. Over-exposure: Entity has password, internal fields you shouldn't return
 *     4. Validation: Different validations for create vs update
 *
 *   DTO pattern:
 *     Client -> RegisterRequest (DTO) -> Service converts -> User (Entity) -> Database
 *     Database -> User (Entity) -> Service converts -> AuthResponse (DTO) -> Client
 *
 * VALIDATION ANNOTATIONS (Jakarta Bean Validation / JSR-380):
 *   @NotBlank  = not null, not empty, not just whitespace
 *   @Email     = must be valid email format
 *   @Size      = string length constraints
 *   @Min/@Max  = number range constraints
 *   @Pattern   = regex pattern match
 *
 *   These are checked AUTOMATICALLY when you use @Valid in the controller:
 *     @PostMapping("/register")
 *     public Response register(@Valid @RequestBody RegisterRequest request) { ... }
 *
 * INTERVIEW Q: What's the difference between @NotNull, @NotEmpty, @NotBlank?
 * A: @NotNull: value != null (but "" is OK)
 *    @NotEmpty: value != null AND value.length > 0 (but "   " is OK)
 *    @NotBlank: value != null AND value.trim().length > 0 (most strict)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;
}
