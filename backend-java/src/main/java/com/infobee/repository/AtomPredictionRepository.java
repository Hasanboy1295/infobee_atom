package com.infobee.repository;

import com.infobee.model.AtomPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AtomPredictionRepository extends JpaRepository<AtomPrediction, Long> {
    Page<AtomPrediction> findByAtomRequestIdOrderByCreatedAtDesc(Long atomRequestId, Pageable pageable);
    List<AtomPrediction> findByAtomRequestIdOrderByCreatedAtDesc(Long atomRequestId);
}
