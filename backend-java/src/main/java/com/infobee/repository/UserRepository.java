package com.infobee.repository;

import com.infobee.model.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    long countByEnabledTrue();
    Page<User> findByUsernameContainingIgnoreCaseOrFullNameContainingIgnoreCaseOrRoleContainingIgnoreCase(
        String username,
        String fullName,
        String role,
        Pageable pageable
    );
}
