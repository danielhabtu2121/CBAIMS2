package com.bank.cims.repository;

import com.bank.cims.model.CbsDuplicateCollision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsDuplicateCollisionRepository extends JpaRepository<CbsDuplicateCollision, String> {
    List<CbsDuplicateCollision> findByEntityTypeAndStatusOrderByCreatedAtDesc(String entityType, String status);
    List<CbsDuplicateCollision> findByBatchId(String batchId);
    List<CbsDuplicateCollision> findByStatusOrderByCreatedAtDesc(String status);
}
