package com.taskflow.service;

import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * USER DETAILS SERVICE — Bridges Spring Security with our database.
 *
 * Spring Security doesn't know how to load users from YOUR database.
 * This class teaches it:
 *   "Hey Spring, when you need a user's details, call loadUserByUsername()
 *    and I'll fetch it from my UserRepository."
 *
 * WHERE is this called?
 *   1. During login: AuthenticationManager -> DaoAuthenticationProvider -> THIS
 *   2. During JWT validation: JwtAuthenticationFilter -> THIS
 *
 * WHY it returns UserDetails (not our User entity)?
 *   Spring Security works with the UserDetails interface.
 *   Our User entity IMPLEMENTS UserDetails, so returning User works!
 *   This is polymorphism: User IS-A UserDetails.
 *
 * INTERVIEW Q: What's the role of UserDetailsService?
 * A: It's the bridge between Spring Security and your user storage.
 *    Spring Security calls loadUserByUsername() to get user info.
 *    You can load from DB, LDAP, file, API — Spring doesn't care.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
