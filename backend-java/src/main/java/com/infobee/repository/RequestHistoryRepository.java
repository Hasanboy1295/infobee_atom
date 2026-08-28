package com.infobee.repository;

import com.infobee.model.RequestHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    List<RequestHistory> findByRequestTypeAndRequestIdOrderByCreatedAtAsc(
        com.infobee.model.RequestType requestType, Long requestId);
}
