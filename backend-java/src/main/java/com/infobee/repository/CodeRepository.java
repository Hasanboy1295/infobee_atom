package com.infobee.repository;

import com.infobee.model.Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeRepository extends JpaRepository<Code, Long> {
    Page<Code> findByGroupIdOrderBySortOrderAsc(Long groupId, Pageable pageable);
    List<Code> findByGroupIdAndEnabledTrueOrderBySortOrderAsc(Long groupId);
    List<Code> findByGroup_GroupCodeAndEnabledTrueOrderBySortOrderAsc(String groupCode);
    boolean existsByGroupIdAndCodeValueIgnoreCase(Long groupId, String codeValue);
}
