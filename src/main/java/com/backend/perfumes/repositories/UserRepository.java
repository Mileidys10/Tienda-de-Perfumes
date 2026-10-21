package com.backend.perfumes.repositories;

import com.backend.perfumes.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByPendingEmail(String pendingEmail);
    Optional<User> findByPendingEmailIsNotNull();
    boolean existsByUsername(String username);
    Optional<User> findByVerificationCode(String verificationCode);
    Optional<User> findByDeletionCodeIsNotNull();
    Optional <User>findByVerificationCodeIsNotNull();

    @Query("SELECT u FROM User u WHERE u.emailVerified = false")
    java.util.List<User> findUnverifiedUsers();

}
