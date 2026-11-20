package com.bank.cims.repository;

import com.bank.cims.model.CbsCustomerProvenanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CbsCustomerProvenanceLogRepository extends JpaRepository<CbsCustomerProvenanceLog, String> {
    List<CbsCustomerProvenanceLog> findByCustomerIdOrderByChangedAtDesc(String customerId);
}

