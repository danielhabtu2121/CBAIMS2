package com.bank.cims.repository;

import com.bank.cims.model.UnassignedRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnassignedRecordRepository extends JpaRepository<UnassignedRecord, String> {
    Optional<UnassignedRecord> findByEntityTypeAndEntityId(String entityType, String entityId);
    List<UnassignedRecord> findByStatus(String status);
    List<UnassignedRecord> findByEntityType(String entityType);
}
