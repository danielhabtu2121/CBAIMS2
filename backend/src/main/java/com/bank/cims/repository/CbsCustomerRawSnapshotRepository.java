package com.bank.cims.repository;

import com.bank.cims.model.CbsCustomerRawSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsCustomerRawSnapshotRepository extends JpaRepository<CbsCustomerRawSnapshot, String> {
    List<CbsCustomerRawSnapshot> findByCbsCustomerNoOrderByCreatedAtDesc(String cbsCustomerNo);
    Optional<CbsCustomerRawSnapshot> findFirstByCbsCustomerNoOrderByCreatedAtDesc(String cbsCustomerNo);
    Optional<CbsCustomerRawSnapshot> findFirstByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<CbsCustomerRawSnapshot> findByBatchId(String batchId);
}

