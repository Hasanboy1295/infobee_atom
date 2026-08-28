package com.infobee.repository;

import com.infobee.model.CodeGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, Long> {
    Page<CodeGroup> findByGroupCodeContainingIgnoreCaseOrGroupNameContainingIgnoreCase(
        String groupCode, String groupName, Pageable pageable);
    boolean existsByGroupCodeIgnoreCase(String groupCode);
}
