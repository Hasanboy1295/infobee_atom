package com.infobee.repository;

import com.infobee.model.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
    Page<Menu> findByLabelContainingIgnoreCaseOrPathContainingIgnoreCase(
        String label,
        String path,
        Pageable pageable
    );
}
