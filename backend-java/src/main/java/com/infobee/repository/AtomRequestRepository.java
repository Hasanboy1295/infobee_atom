package com.infobee.repository;

import com.infobee.model.AtomRequest;
import com.infobee.model.RequestStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AtomRequestRepository extends JpaRepository<AtomRequest, Long>, JpaSpecificationExecutor<AtomRequest> {
    Page<AtomRequest> findByOwnerUsername(String username, Pageable pageable);

    @Query("SELECT a FROM AtomRequest a WHERE a.owner.username = :username OR (a.owner.department IS NOT NULL AND a.owner.department.id = :departmentId)")
    Page<AtomRequest> findByOwnerUsernameOrDepartmentId(String username, Long departmentId, Pageable pageable);

    @Query("SELECT a.status, COUNT(a) FROM AtomRequest a GROUP BY a.status")
    List<Object[]> countByStatus();
}
