-- V45__business_clarification_mandatory_document_rules.sql
-- Seed business clarification security documents into authoritative mandatory document rule matrix

INSERT INTO cims_mandatory_document_rules (
    id, collateral_category, collateral_type, document_type, ownership_condition, applicability_condition, mandatory, active, exception_allowed
) VALUES
    ('doc-r-loan-agreement', 'All', 'All', 'Loan Agreement / Contracts', 'All', 'Authoritative credit facility agreement or loan contract', false, true, true),
    ('doc-r-reg-letter', 'All', 'All', 'Registration Letters', 'All', 'Official collateral or mortgage registration letter / confirmation', false, true, true)
ON CONFLICT (id) DO UPDATE SET
    collateral_category = EXCLUDED.collateral_category,
    collateral_type = EXCLUDED.collateral_type,
    document_type = EXCLUDED.document_type,
    ownership_condition = EXCLUDED.ownership_condition,
    applicability_condition = EXCLUDED.applicability_condition,
    mandatory = EXCLUDED.mandatory,
    active = EXCLUDED.active,
    exception_allowed = EXCLUDED.exception_allowed;

