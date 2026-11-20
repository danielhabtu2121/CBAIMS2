package com.bank.cims.repository;

import com.bank.cims.model.CbsUnresolvedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsUnresolvedEntityRepository extends JpaRepository<CbsUnresolvedEntity, String> {
    List<CbsUnresolvedEntity> findByEntityTypeAndStatusOrderByCreatedAtDesc(String entityType, String status);
    List<CbsUnresolvedEntity> findByBatchId(String batchId);
    List<CbsUnresolvedEntity> findByStatusOrderByCreatedAtDesc(String status);
}

