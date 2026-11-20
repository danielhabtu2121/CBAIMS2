package com.bank.cims.repository;

import com.bank.cims.model.OrganizationalUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationalUnitRepository extends JpaRepository<OrganizationalUnit, String> {
    Optional<OrganizationalUnit> findByCode(String code);
    Optional<OrganizationalUnit> findByCodeIgnoreCase(String code);
    List<OrganizationalUnit> findByUnitType(String unitType);
    List<OrganizationalUnit> findByParentId(String parentId);
    List<OrganizationalUnit> findByActiveTrue();
    List<OrganizationalUnit> findByUnitTypeAndActiveTrue(String unitType);
}
