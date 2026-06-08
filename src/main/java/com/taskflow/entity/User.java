package com.taskflow.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * USER ENTITY — Maps to the "users" table in PostgreSQL.
 *
 * WHY implements UserDetails?
 *   Spring Security needs a UserDetails object to authenticate users.
 *   Instead of creating a separate adapter class, our User entity directly
 *   implements UserDetails. This is the most common pattern in Spring Boot apps.
 *
 *   UserDetails methods Spring Security calls:
 *     getUsername()     — to identify the user
 *     getPassword()     — to verify credentials (BCrypt hash)
 *     getAuthorities()  — to check permissions (roles)
 *     isAccountNonExpired/Locked/etc. — account status checks
 *
 * LOMBOK ANNOTATIONS:
 *   @Data      = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
 *   @Builder   = generates User.builder().name("Raja").email("...").build()
 *   @NoArgsConstructor = JPA REQUIRES a no-arg constructor (creates empty object, then sets fields)
 *   @AllArgsConstructor = needed by @Builder
 *
 * JPA ANNOTATIONS:
 *   @Entity = "This class is a database table"
 *   @Table  = customize table name (default would be "user" — reserved in PostgreSQL!)
 *   @Id     = primary key
 *   @GeneratedValue(IDENTITY) = database auto-generates the ID (PostgreSQL SERIAL)
 *   @Column = customize column (nullable, unique, length constraints)
 *
 * INTERVIEW Q: Why @Table(name = "users") instead of default?
 * A: "user" is a RESERVED KEYWORD in PostgreSQL. Using it without quotes causes SQL errors.
 *    Always name your table "users" (plural) to avoid this.
 *
 * INTERVIEW Q: Why use BCrypt for passwords?
 * A: BCrypt is a one-way hash with built-in salt. Even if DB is leaked, passwords can't be
 *    reversed. Each hash is unique even for the same password (due to random salt).
 *    BCrypt is deliberately slow — makes brute-force attacks impractical.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password; // BCrypt hashed — NEVER store plain text!

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * @PrePersist = runs BEFORE the entity is saved to DB for the first time.
     * Automatically sets createdAt timestamp — no manual setting needed!
     *
     * INTERVIEW Q: What's the difference between @PrePersist and @PostConstruct?
     * A: @PrePersist = JPA lifecycle (before DB insert)
     *    @PostConstruct = Spring lifecycle (after bean creation + DI)
     *    They serve different purposes and belong to different frameworks.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===================================================================
    // UserDetails interface methods — required by Spring Security
    // ===================================================================

    /**
     * Returns the user's ROLES as GrantedAuthority objects.
     * Spring Security uses these to check @PreAuthorize("hasRole('ADMIN')").
     *
     * We prefix with "ROLE_" because Spring Security's hasRole() auto-adds it.
     * So hasRole("ADMIN") actually checks for authority "ROLE_ADMIN".
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email; // We use EMAIL as the unique identifier for login
    }

    // Account status — all true for now (can add email verification later)
    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}
