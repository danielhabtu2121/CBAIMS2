package com.bank.cims.repository;

import com.bank.cims.model.CbsSegmentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsSegmentMappingRepository extends JpaRepository<CbsSegmentMapping, String> {
    Optional<CbsSegmentMapping> findBySourceValue(String sourceValue);
    Optional<CbsSegmentMapping> findBySourceValueIgnoreCase(String sourceValue);
    List<CbsSegmentMapping> findByMappingStatus(String mappingStatus);
    List<CbsSegmentMapping> findByActiveTrue();
}
