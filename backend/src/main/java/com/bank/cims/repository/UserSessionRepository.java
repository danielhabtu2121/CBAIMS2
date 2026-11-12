package com.bank.cims.repository;

import com.bank.cims.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {
    List<UserSession> findByUserIdAndRevokedFalse(String userId);
    List<UserSession> findByUserIdOrderByCreatedAtDesc(String userId);
    List<UserSession> findByRevokedFalseAndExpiresAtBefore(LocalDateTime now);
}

