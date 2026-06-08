package com.taskflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AUTH RESPONSE DTO — What the server returns after login/register.
 *
 * Contains:
 *   accessToken  — short-lived JWT (15 min), sent with every API request
 *   refreshToken — long-lived JWT (7 days), used to get new access tokens
 *
 * WHY two tokens?
 *   Access Token (short-lived):
 *     - Sent in every request header: "Authorization: Bearer <token>"
 *     - If stolen, attacker has access for only 15 minutes
 *     - No need to check database — just verify the signature
 *
 *   Refresh Token (long-lived):
 *     - Stored securely by the client (httpOnly cookie or secure storage)
 *     - Used ONLY to get a new access token when the old one expires
 *     - Can be revoked server-side (logout = delete refresh token)
 *
 *   Flow:
 *     1. Login -> get accessToken (15min) + refreshToken (7days)
 *     2. Use accessToken for all API calls
 *     3. Access token expires after 15 min
 *     4. Call /api/auth/refresh with refreshToken -> get NEW accessToken
 *     5. Repeat from step 2
 *     6. Logout -> delete refreshToken (access token will expire naturally)
 *
 * INTERVIEW Q: Why not just use one long-lived token?
 * A: If a long-lived token is stolen (XSS, network sniffing), the attacker has
 *    access for days/months. With short-lived access tokens, the damage window
 *    is only 15 minutes. The refresh token is stored more securely and only
 *    used for one specific endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String fullName;
    private String email;
    private String role;
}
