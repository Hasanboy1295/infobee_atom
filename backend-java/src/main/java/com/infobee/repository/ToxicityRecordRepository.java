package com.infobee.repository;

import com.infobee.model.ToxicityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ToxicityRecordRepository extends JpaRepository<ToxicityRecord, Long> {
    List<ToxicityRecord> findByCasNumber(String casNumber);
    List<ToxicityRecord> findBySubstanceNameContainingIgnoreCase(String substanceName);
    Page<ToxicityRecord> findByCasNumberOrSubstanceNameContainingIgnoreCase(
        String casNumber, String substanceName, Pageable pageable);
}
