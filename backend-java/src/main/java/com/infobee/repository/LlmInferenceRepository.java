package com.infobee.repository;

import com.infobee.model.LlmInference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LlmInferenceRepository extends JpaRepository<LlmInference, Long> {
    List<LlmInference> findByCpsrRequestIdOrderByCreatedAtDesc(Long cpsrRequestId);
}
