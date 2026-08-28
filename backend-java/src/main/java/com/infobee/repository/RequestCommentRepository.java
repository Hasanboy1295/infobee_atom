package com.infobee.repository;

import com.infobee.model.RequestComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestCommentRepository extends JpaRepository<RequestComment, Long> {
    List<RequestComment> findByAtomRequestIdOrderByCreatedAtAsc(Long requestId);
    List<RequestComment> findByCpsrRequestIdOrderByCreatedAtAsc(Long requestId);
}
