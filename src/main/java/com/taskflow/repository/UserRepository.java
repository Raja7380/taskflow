package com.taskflow.repository;

import com.taskflow.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * USER REPOSITORY — Data Access Layer for User entity.
 *
 * WHAT IS JpaRepository?
 *   An interface from Spring Data JPA that gives you CRUD operations for FREE:
 *     save(entity)         — INSERT or UPDATE
 *     findById(id)         — SELECT by primary key
 *     findAll()            — SELECT all
 *     deleteById(id)       — DELETE by primary key
 *     count()              — COUNT all rows
 *     existsById(id)       — check if exists
 *     ... and many more!
 *
 *   You just extend JpaRepository<EntityType, IdType> — no implementation needed!
 *   Spring creates the implementation at runtime using JDK dynamic proxy.
 *
 * DERIVED QUERY METHODS:
 *   Spring generates SQL from method names automatically!
 *   findByEmail(email)  -> SELECT * FROM users WHERE email = ?
 *   existsByEmail(email) -> SELECT COUNT(*) > 0 FROM users WHERE email = ?
 *
 *   Naming convention: findBy + FieldName + Condition
 *   Examples:
 *     findByFullNameContaining(name) -> WHERE full_name LIKE '%name%'
 *     findByRoleAndEnabled(role, enabled) -> WHERE role = ? AND enabled = ?
 *     findByCreatedAtBetween(start, end) -> WHERE created_at BETWEEN ? AND ?
 *
 * WHY Optional<User> instead of User?
 *   User might not exist! Optional forces you to handle the "not found" case:
 *     userRepo.findByEmail("x").orElseThrow(() -> new UserNotFoundException("..."))
 *   Without Optional, you'd get null — and risk NullPointerException.
 *
 * INTERVIEW Q: How does Spring Data JPA generate the implementation?
 * A: At startup, Spring scans for interfaces extending JpaRepository.
 *    For each, it creates a JDK dynamic proxy that:
 *    1. Parses the method name (findByEmail -> "email" field)
 *    2. Builds a JPQL/SQL query
 *    3. Executes it via EntityManager
 *    You never write the implementation!
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
