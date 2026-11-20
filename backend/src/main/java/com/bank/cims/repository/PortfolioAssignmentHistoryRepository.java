package com.bank.cims.repository;

import com.bank.cims.model.PortfolioAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioAssignmentHistoryRepository extends JpaRepository<PortfolioAssignmentHistory, String> {
    List<PortfolioAssignmentHistory> findByEntityTypeAndEntityId(String entityType, String entityId);
    List<PortfolioAssignmentHistory> findByPortfolioAssignmentId(String portfolioAssignmentId);
}
