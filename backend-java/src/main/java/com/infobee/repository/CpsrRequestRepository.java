package com.infobee.repository;

import com.infobee.model.CpsrRequest;
import com.infobee.model.RequestStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CpsrRequestRepository extends JpaRepository<CpsrRequest, Long>, JpaSpecificationExecutor<CpsrRequest> {
    Page<CpsrRequest> findByOwnerUsername(String username, Pageable pageable);

    @Query("SELECT c FROM CpsrRequest c WHERE c.owner.username = :username OR (c.owner.department IS NOT NULL AND c.owner.department.id = :departmentId)")
    Page<CpsrRequest> findByOwnerUsernameOrDepartmentId(String username, Long departmentId, Pageable pageable);

    @Query("SELECT c.status, COUNT(c) FROM CpsrRequest c GROUP BY c.status")
    List<Object[]> countByStatus();
}
