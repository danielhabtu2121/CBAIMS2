package com.bank.cims.repository;

import com.bank.cims.model.CbsFacilityProvenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsFacilityProvenanceLogRepository extends JpaRepository<CbsFacilityProvenanceLog, String> {
    List<CbsFacilityProvenanceLog> findByFacilityIdOrderByChangedAtDesc(String facilityId);
}
