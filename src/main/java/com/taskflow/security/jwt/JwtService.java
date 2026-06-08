package com.taskflow.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT SERVICE — Creates and validates JSON Web Tokens.
 *
 * JWT = JSON Web Token — an encoded string that proves who the user is.
 *
 * STRUCTURE: xxxxx.yyyyy.zzzzz
 *   Header:    {"alg": "HS256", "typ": "JWT"} — algorithm used
 *   Payload:   {"sub": "raja@email.com", "role": "ADMIN", "exp": 1234567890} — user data
 *   Signature: HMAC-SHA256(base64(header) + "." + base64(payload), SECRET_KEY)
 *
 * HOW IT WORKS:
 *   1. User logs in with email/password
 *   2. Server validates credentials
 *   3. Server creates JWT with user info + signs it with SECRET_KEY
 *   4. Client stores JWT (localStorage or httpOnly cookie)
 *   5. Client sends JWT in header: "Authorization: Bearer eyJhbGci..."
 *   6. Server receives request, extracts JWT, verifies signature
 *   7. If valid -> extract user info from token -> process request
 *   8. If invalid/expired -> return 401 Unauthorized
 *
 * SECURITY:
 *   - SECRET_KEY is ONLY on the server — nobody else can create valid tokens
 *   - If someone tampers with the payload, the signature won't match -> rejected
 *   - Token has expiration — even if stolen, it expires after 15 minutes
 *
 * INTERVIEW Q: Is JWT encrypted?
 * A: NO! JWT is ENCODED (Base64), not encrypted. Anyone can read the payload.
 *    The SIGNATURE only proves the token wasn't tampered with.
 *    NEVER put sensitive data (passwords, credit cards) in JWT payload!
 *
 * INTERVIEW Q: Where should the client store the JWT?
 * A: httpOnly cookie (safest — immune to XSS) or in-memory variable.
 *    localStorage is vulnerable to XSS attacks but commonly used in SPAs.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    /**
     * Extract the username (email) from a JWT token.
     * "sub" (subject) is the standard JWT claim for user identity.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract any claim from the token using a resolver function.
     * This is a generic method using Java's Function interface.
     *
     * Usage: extractClaim(token, Claims::getExpiration) -> gets expiration date
     *        extractClaim(token, Claims::getSubject) -> gets username
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generate an ACCESS TOKEN (short-lived: 15 minutes).
     * Contains: subject (email), role, issued-at, expiration.
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return buildToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * Generate a REFRESH TOKEN (long-lived: 7 days).
     * Contains minimal info — just the subject. Used only to get new access tokens.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshTokenExpiration);
    }

    /**
     * Validate the token:
     *   1. Extract username from token
     *   2. Check it matches the provided UserDetails
     *   3. Check token hasn't expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Build the actual JWT token.
     *
     * Jwts.builder() = creates a new JWT
     *   .claims()         — set custom claims (role, etc.)
     *   .subject()        — set the "sub" claim (user identity)
     *   .issuedAt()       — when the token was created
     *   .expiration()     — when the token expires
     *   .signWith()       — sign with our secret key (HMAC-SHA256)
     *   .compact()        — build the final encoded string
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parse ALL claims from the token.
     * This also VALIDATES the signature — if the token was tampered with,
     * this will throw a JwtException.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Convert the Base64-encoded secret string into a SecretKey object.
     * HMAC-SHA256 requires a key of at least 256 bits (32 bytes).
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
