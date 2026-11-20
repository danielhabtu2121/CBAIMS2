package com.bank.cims.repository;

import com.bank.cims.model.CbsImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsImportBatchRepository extends JpaRepository<CbsImportBatch, String> {
    Optional<CbsImportBatch> findByBatchReference(String batchReference);
    List<CbsImportBatch> findAllByOrderByCreatedAtDesc();
}

