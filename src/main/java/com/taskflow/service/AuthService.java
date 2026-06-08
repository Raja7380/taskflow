package com.taskflow.service;

import com.taskflow.dto.request.LoginRequest;
import com.taskflow.dto.request.RegisterRequest;
import com.taskflow.dto.response.AuthResponse;
import com.taskflow.entity.Role;
import com.taskflow.entity.User;
import com.taskflow.exception.DuplicateResourceException;
import com.taskflow.repository.UserRepository;
import com.taskflow.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AUTH SERVICE — Business logic for authentication (register + login).
 *
 * WHY a Service layer (not putting logic in Controller)?
 *   1. Separation of concerns: Controller handles HTTP, Service handles logic
 *   2. Reusability: Multiple controllers can use the same service
 *   3. Testability: Easy to unit test service without HTTP layer
 *   4. Transaction management: @Transactional goes on service methods
 *
 * LAYER ARCHITECTURE:
 *   Controller (HTTP) -> Service (Business Logic) -> Repository (Database)
 *
 *   Controller: Parse request, validate input, call service, return response
 *   Service: Business rules, data transformation, orchestration
 *   Repository: CRUD operations on the database
 *
 * @RequiredArgsConstructor (Lombok) = generates constructor with all 'final' fields.
 *   This IS constructor injection! Spring sees the constructor and injects the beans.
 *   Equivalent to writing:
 *     public AuthService(UserRepository userRepo, PasswordEncoder encoder, ...) {
 *         this.userRepository = userRepo;
 *         this.passwordEncoder = encoder;
 *         ...
 *     }
 *
 * INTERVIEW Q: Why constructor injection over field injection (@Autowired)?
 * A: 1. Fields can be final (immutable) — safer
 *    2. Dependencies are clear (visible in constructor)
 *    3. Easy to test (pass mocks in constructor)
 *    4. Fails fast: missing dependency = app won't start
 *    Spring team officially recommends constructor injection.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * REGISTER a new user.
     *
     * Flow:
     *   1. Check if email already exists -> throw DuplicateResourceException
     *   2. Create User entity from the request DTO
     *   3. Hash the password with BCrypt (NEVER store plain text!)
     *   4. Save user to database
     *   5. Generate JWT tokens (access + refresh)
     *   6. Return AuthResponse with tokens + user info
     */
    public AuthResponse register(RegisterRequest request) {
        // Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Build User entity from DTO
        // Notice: we DON'T copy the password directly — we HASH it first!
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hash!
                .role(Role.USER) // Default role for new registrations
                .build();

        // Save to database (JPA generates the ID, @PrePersist sets timestamps)
        userRepository.save(user);

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * LOGIN an existing user.
     *
     * Flow:
     *   1. AuthenticationManager.authenticate() does the heavy lifting:
     *      a. Calls UserDetailsService.loadUserByUsername(email)
     *      b. Calls PasswordEncoder.matches(rawPassword, hashedPassword)
     *      c. If either fails -> throws BadCredentialsException
     *   2. If authentication succeeds, generate JWT tokens
     *   3. Return AuthResponse
     *
     * WHY use AuthenticationManager instead of doing it manually?
     *   - It's the Spring Security standard way
     *   - Handles all edge cases (locked accounts, expired credentials, etc.)
     *   - Fires authentication events (for audit logging)
     *   - Pluggable — can swap to LDAP, OAuth, etc. without changing this code
     */
    public AuthResponse login(LoginRequest request) {
        // This throws BadCredentialsException if email/password don't match
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // If we reach here, authentication was successful
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(); // Won't happen since authenticate() already verified

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
