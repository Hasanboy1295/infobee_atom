package com.infobee.repository;

import com.infobee.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    boolean existsByNameIgnoreCase(String name);
    Page<Role> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
