package com.infobee.repository;

import com.infobee.model.CpsrEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CpsrEvaluationRepository extends JpaRepository<CpsrEvaluation, Long> {
    List<CpsrEvaluation> findByCpsrRequestIdOrderByCreatedAtDesc(Long cpsrRequestId);
    List<CpsrEvaluation> findByEvaluatorIdOrderByCreatedAtDesc(Long evaluatorId);
}
