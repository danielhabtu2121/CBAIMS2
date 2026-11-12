-- =========================================================================
-- V37: Update mandatory document rules with collateral_type and conditional applicability (Item 6)
-- Ensures subtype-specific requirements (e.g. Under Construction, Leasehold)
-- apply only to corresponding collateral subtypes and conditional perfection docs.
-- =========================================================================

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Real Estate',
    collateral_type = 'All',
    mandatory = true,
    active = true,
    applicability_condition = 'Authoritative cadastral title certificate or deed'
WHERE id = 'doc-r-title-deed';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Real Estate',
    collateral_type = 'Mortgage Registration',
    mandatory = false,
    active = true,
    applicability_condition = 'Applicable upon Cadastral Land Registry mortgage filing'
WHERE id = 'doc-r-mortgage-reg';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Immovable Properties',
    collateral_type = 'Building - Under Construction',
    mandatory = true,
    active = true,
    applicability_condition = 'Certified engineering Bill of Quantities (BOQ) with remaining works schedule'
WHERE id = 'doc-r-boq-under-con';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Immovable Properties',
    collateral_type = 'Building - Under Construction',
    mandatory = true,
    active = true,
    applicability_condition = 'Approved municipal building permit and architectural design drawings'
WHERE id = 'doc-r-building-permit';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Immovable Properties',
    collateral_type = 'Building - Leasehold',
    mandatory = true,
    active = true,
    applicability_condition = 'Government lease contract and latest lease fee clearance receipt'
WHERE id = 'doc-r-lease-agreement';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Land',
    collateral_type = 'Land - Leasehold',
    mandatory = true,
    active = true,
    applicability_condition = 'Government lease contract and latest lease fee clearance receipt'
WHERE id = 'doc-r-lease-land';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Movable Properties',
    collateral_type = 'Vehicles',
    mandatory = true,
    active = true,
    applicability_condition = 'Official Ministry of Transport vehicle ownership booklet/libre'
WHERE id = 'doc-r-vehicle-liber';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Agricultural/Other',
    collateral_type = 'Livestock',
    mandatory = true,
    active = true,
    applicability_condition = 'Veterinary/agricultural ear-tag registration certificate and veterinary health record'
WHERE id = 'doc-r-livestock-eartag';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Agricultural/Other',
    collateral_type = 'Merchandise',
    mandatory = true,
    active = true,
    applicability_condition = 'Certified warehouse storage receipt and commodity inspection certificate'
WHERE id = 'doc-r-warehouse-receipt';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Intangible Assets',
    collateral_type = 'Intellectual Property',
    mandatory = true,
    active = true,
    applicability_condition = 'Ethiopian Intellectual Property Authority registration extract/certificate'
WHERE id = 'doc-r-ip-cert';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'Movable Properties',
    collateral_type = 'Third-Party Custody',
    mandatory = false,
    active = true,
    applicability_condition = 'Signed tripartite collateral custody and warehousing agreement'
WHERE id = 'doc-r-custody-agreement';

UPDATE cims_mandatory_document_rules SET
    collateral_category = 'All',
    collateral_type = 'All',
    mandatory = true,
    active = true,
    applicability_condition = 'Comprehensive appraisal report by bank approved valuation engineer'
WHERE id = 'doc-r-valuation-report';
