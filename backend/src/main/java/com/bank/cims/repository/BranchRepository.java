package com.bank.cims.repository;

import com.bank.cims.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, String> {
    Optional<Branch> findFirstByCodeIgnoreCase(String code);
    Optional<Branch> findFirstByNameIgnoreCase(String name);
    List<Branch> findByNameIgnoreCase(String name);
    List<Branch> findByCodeIgnoreCase(String code);
    List<Branch> findByParentDistrictId(String parentDistrictId);

    default Optional<Branch> findByName(String name) {
        return findFirstByNameIgnoreCase(name);
    }

    default Optional<Branch> findByCode(String code) {
        return findFirstByCodeIgnoreCase(code);
    }
}
