package com.bank.cims.repository;

import com.bank.cims.model.CbsFacilityRawSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsFacilityRawSnapshotRepository extends JpaRepository<CbsFacilityRawSnapshot, String> {
    List<CbsFacilityRawSnapshot> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    Optional<CbsFacilityRawSnapshot> findFirstByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    Optional<CbsFacilityRawSnapshot> findFirstByFacilityIdOrderByCreatedAtDesc(String facilityId);
    List<CbsFacilityRawSnapshot> findByBatchId(String batchId);
}

