package com.taskflow.security.filter;

import com.taskflow.security.jwt.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT AUTHENTICATION FILTER — Runs on EVERY HTTP request.
 *
 * This is the CORE of JWT authentication. It sits in Spring Security's filter chain
 * and intercepts every request BEFORE it reaches any controller.
 *
 * FLOW FOR EVERY REQUEST:
 *   1. Extract "Authorization" header from the HTTP request
 *   2. Check if it starts with "Bearer " (JWT convention)
 *   3. If no token -> skip this filter, continue to next (might be public endpoint)
 *   4. If token exists:
 *      a. Extract username (email) from the token
 *      b. Load the user from database
 *      c. Validate the token (signature + expiration + username match)
 *      d. If valid -> set the user as "authenticated" in SecurityContext
 *      e. If invalid -> do nothing (request will be rejected by SecurityConfig)
 *
 * WHY extends OncePerRequestFilter?
 *   Guarantees this filter runs EXACTLY ONCE per request.
 *   Regular Filter might run multiple times due to internal forwards/includes.
 *
 * INTERVIEW Q: What is SecurityContextHolder?
 * A: A ThreadLocal storage that holds the currently authenticated user.
 *    SecurityContextHolder.getContext().getAuthentication() gives you the current user.
 *    It's ThreadLocal = each request thread has its own copy (thread-safe).
 *    After the request completes, the SecurityContext is cleared.
 *
 * INTERVIEW Q: Why check SecurityContextHolder.getContext().getAuthentication() == null?
 * A: If the user is ALREADY authenticated (e.g., by another filter), we skip
 *    re-authentication. This avoids unnecessary database lookups.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1: Extract the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: Check if it's a Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // No JWT token -> continue filter chain (might be a public endpoint like /register)
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // Step 4: Extract username from the token
        final String userEmail = jwtService.extractUsername(jwt);

        // Step 5: If username exists AND user is NOT already authenticated
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Step 6: Load user from database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // Step 7: Validate the token
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // Step 8: Create authentication token and set it in SecurityContext
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // credentials (null because we already verified via JWT)
                        userDetails.getAuthorities()
                );

                // Attach request details (IP address, session info)
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Step 9: Tell Spring Security "this user is authenticated!"
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Step 10: Continue to the next filter in the chain
        filterChain.doFilter(request, response);
    }
}
