package com.infobee.repository;

import com.infobee.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    boolean existsByNameIgnoreCase(String name);
    Page<Department> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
