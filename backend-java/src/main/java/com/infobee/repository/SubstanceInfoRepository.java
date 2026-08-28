package com.infobee.repository;

import com.infobee.model.SubstanceInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubstanceInfoRepository extends JpaRepository<SubstanceInfo, Long> {
    List<SubstanceInfo> findByCpsrRequestId(Long cpsrRequestId);
}
