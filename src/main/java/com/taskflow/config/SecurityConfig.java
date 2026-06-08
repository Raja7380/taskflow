package com.taskflow.config;

import com.taskflow.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * SECURITY CONFIGURATION — The brain of Spring Security in this application.
 *
 * This class configures:
 *   1. Which endpoints are PUBLIC (no auth needed) vs PROTECTED
 *   2. How passwords are encoded (BCrypt)
 *   3. How authentication works (JWT, not sessions)
 *   4. Where our JWT filter sits in the filter chain
 *   5. CORS and CSRF settings
 *
 * SPRING SECURITY FILTER CHAIN:
 *   Every HTTP request passes through a chain of filters:
 *     [CorsFilter] -> [CsrfFilter] -> [JwtAuthFilter] -> [AuthorizationFilter] -> [Controller]
 *
 *   Our JwtAuthenticationFilter sits BEFORE UsernamePasswordAuthenticationFilter.
 *   It extracts + validates the JWT token and sets the authenticated user.
 *
 * @EnableWebSecurity = activates Spring Security's web security
 * @EnableMethodSecurity = enables @PreAuthorize/@PostAuthorize on methods
 *   Example: @PreAuthorize("hasRole('ADMIN')") on a controller method
 *
 * INTERVIEW Q: What is the SecurityFilterChain?
 * A: An ordered list of filters that every HTTP request passes through.
 *    Each filter can inspect, modify, or reject the request.
 *    Spring Security registers ~15 default filters. We add our JWT filter to this chain.
 *
 * INTERVIEW Q: Why STATELESS session management?
 * A: With JWT, the server doesn't store any session data. The token itself contains
 *    all needed info. This makes the app horizontally scalable — any server instance
 *    can validate the token without shared session storage.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Configure the security filter chain.
     * This is WHERE you decide what's public and what's protected.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: Disabled because we use JWT (stateless).
                // CSRF protection is for session-based auth (cookies).
                // JWT tokens are sent in headers, not cookies -> CSRF not applicable.
                .csrf(AbstractHttpConfigurer::disable)

                // Configure endpoint access rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no authentication needed
                        .requestMatchers(
                                "/api/auth/**",           // Login, register, refresh token
                                "/api/health",             // Health check
                                "/swagger-ui/**",          // Swagger UI pages
                                "/swagger-ui.html",        // Swagger entry point
                                "/v3/api-docs/**",         // OpenAPI JSON docs
                                "/h2-console/**"           // H2 database console (dev only)
                        ).permitAll()

                        // ADMIN-only endpoints
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // STATELESS session — no server-side session (we use JWT)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Use our custom authentication provider (BCrypt + UserDetailsService)
                .authenticationProvider(authenticationProvider())

                // Add JWT filter BEFORE Spring's default username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // Allow H2 console to render in iframes (dev only)
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    /**
     * AUTHENTICATION PROVIDER — Tells Spring HOW to authenticate users.
     *
     * DaoAuthenticationProvider:
     *   "Dao" = Data Access Object
     *   It loads users from a database (via UserDetailsService) and
     *   verifies passwords using the configured PasswordEncoder.
     *
     * Flow when user logs in:
     *   1. AuthenticationManager receives email + password
     *   2. Delegates to DaoAuthenticationProvider
     *   3. Provider calls userDetailsService.loadUserByUsername(email)
     *   4. Provider calls passwordEncoder.matches(rawPassword, encodedPassword)
     *   5. If both pass -> user is authenticated!
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * PASSWORD ENCODER — BCrypt.
     *
     * BCrypt features:
     *   - One-way hash (can't reverse to get original password)
     *   - Built-in SALT (random value added before hashing)
     *   - Configurable strength (default 10 = 2^10 hash iterations)
     *   - Same password generates DIFFERENT hash each time (due to random salt)
     *
     * Example:
     *   encode("password123") -> "$2a$10$N9qo8uLOickgx2ZMRZoMye..."
     *   encode("password123") -> "$2a$10$Xr4Ju1q5K3FmIcJuMOqZhe..."  (different!)
     *   matches("password123", "$2a$10$N9qo8u...") -> true
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AUTHENTICATION MANAGER — Entry point for authentication.
     * Controllers call authenticationManager.authenticate(credentials)
     * to verify user credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
