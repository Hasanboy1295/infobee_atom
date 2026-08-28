package com.infobee.repository;

import com.infobee.model.Attachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByAtomRequestIdOrderByCreatedAtAsc(Long atomRequestId);
    List<Attachment> findByCpsrRequestIdOrderByCreatedAtAsc(Long cpsrRequestId);
    long countByAtomRequestId(Long atomRequestId);
    long countByCpsrRequestId(Long cpsrRequestId);
    Optional<Attachment> findByStoredFilename(String storedFilename);
}
