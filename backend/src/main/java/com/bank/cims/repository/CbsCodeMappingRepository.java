package com.bank.cims.repository;

import com.bank.cims.model.CbsCodeMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CbsCodeMappingRepository extends JpaRepository<CbsCodeMapping, String> {
    List<CbsCodeMapping> findByMappingTypeAndActiveTrue(String mappingType);
    Optional<CbsCodeMapping> findFirstByMappingTypeAndSourceCodeIgnoreCaseAndActiveTrue(String mappingType, String sourceCode);
}

