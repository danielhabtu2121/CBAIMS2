-- ==============================================================================
-- Migration V35: Insurance Coverage Adequacy, Collateral Data Requirements,
-- and Early Collateral Release in CBAIMS
-- ==============================================================================

-- 1. Extend cims_collaterals with required collateral fields, insurable value breakdown,
-- construction progress, location, and release tracking
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS insurable_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS land_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS estimated_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS estimated_value_source VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS estimated_valuation_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bank_assessed_value_source VARCHAR(50) DEFAULT 'CBS';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS is_under_construction BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS construction_completion_pct NUMERIC(5,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS construction_remaining_pct NUMERIC(5,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS engineering_report_doc_id VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS construction_inspection_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS expected_completion_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS construction_review_triggered BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_address VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_city VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_region VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_subcity VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_woreda VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_kebele VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS property_cert_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_plate_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_chassis_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_engine_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS identifier_source VARCHAR(50) DEFAULT 'CBS';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS pending_insurance_claim BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS pending_insurance_claim_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS pending_insurance_claim_amount NUMERIC(18,2) DEFAULT 0.00;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS active_release_request_id VARCHAR(64);

-- 2. Configurable Insurance Coverage Rules (BR-18)
CREATE TABLE IF NOT EXISTS cims_insurance_coverage_rules (
    id VARCHAR(64) PRIMARY KEY,
    collateral_type VARCHAR(100) NOT NULL,
    business_segment VARCHAR(100) NOT NULL,
    required_coverage_percentage NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    over_insurance_tolerance_pct NUMERIC(5,2) DEFAULT 10.00,
    effective_from VARCHAR(50),
    effective_to VARCHAR(50),
    status VARCHAR(20) DEFAULT 'Active',
    approved_by VARCHAR(64),
    approved_at VARCHAR(50),
    is_credit_policy_approved BOOLEAN DEFAULT TRUE,
    maker_id VARCHAR(64),
    checker_id VARCHAR(64),
    created_at VARCHAR(50),
    updated_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_cov_rule_type_seg ON cims_insurance_coverage_rules (collateral_type, business_segment, status);

-- 3. Insurance Exceptions (BR-07 and BR-12)
CREATE TABLE IF NOT EXISTS cims_insurance_exceptions (
    id VARCHAR(64) PRIMARY KEY,
    collateral_id VARCHAR(64) NOT NULL,
    policy_id VARCHAR(64),
    exception_type VARCHAR(50) NOT NULL,
    reason TEXT NOT NULL,
    approved_exception_amount NUMERIC(18,2) NOT NULL DEFAULT 0.00,
    validity_start_date VARCHAR(50) NOT NULL,
    validity_end_date VARCHAR(50) NOT NULL,
    review_date VARCHAR(50) NOT NULL,
    requesting_user_id VARCHAR(64) NOT NULL,
    supervisor_approver_id VARCHAR(64),
    approval_status VARCHAR(30) DEFAULT 'Pending',
    decision_date VARCHAR(50),
    decision_comments TEXT,
    supporting_doc_id VARCHAR(100),
    resolution_notes TEXT,
    resolved_at VARCHAR(50),
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_ins_exc_col_id ON cims_insurance_exceptions (collateral_id);
CREATE INDEX IF NOT EXISTS idx_ins_exc_status ON cims_insurance_exceptions (approval_status);

-- 4. Collateral Release Requests (Maker-Checker & Pre-Release Impact Assessment)
CREATE TABLE IF NOT EXISTS cims_collateral_release_requests (
    id VARCHAR(64) PRIMARY KEY,
    collateral_id VARCHAR(64) NOT NULL,
    facility_ids TEXT NOT NULL,
    release_type VARCHAR(30) DEFAULT 'FULL_COLLATERAL',
    requester_id VARCHAR(64) NOT NULL,
    reason TEXT NOT NULL,
    supporting_document_ids TEXT,
    status VARCHAR(30) DEFAULT 'PENDING_APPROVAL',
    is_blocked BOOLEAN DEFAULT FALSE,
    block_reasons TEXT,
    impact_assessment_json TEXT,
    approver_id VARCHAR(64),
    decision_date VARCHAR(50),
    decision_comments TEXT,
    cbs_update_status VARCHAR(30) DEFAULT 'PENDING',
    cbs_updated_by VARCHAR(64),
    cbs_updated_at VARCHAR(50),
    cbs_reference_no VARCHAR(100),
    cbs_update_notes TEXT,
    workflow_task_id VARCHAR(64),
    created_at VARCHAR(50),
    updated_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_rel_req_col_id ON cims_collateral_release_requests (collateral_id);
CREATE INDEX IF NOT EXISTS idx_rel_req_status ON cims_collateral_release_requests (status);

-- 5. Seed Initial Configurable Credit Policy Coverage Rules (BR-18)
INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-comm-bldg-corp', 'Commercial Building', 'Corporate Banking', 100.00, 10.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-comm-prop-corp', 'Commercial Property', 'Corporate Banking', 100.00, 10.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-res-prop-ret', 'Residential Property', 'Retail Banking', 100.00, 10.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-veh-all', 'Motor Vehicle', 'All', 100.00, 5.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-mach-all', 'Industrial Machinery', 'All', 100.00, 10.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO cims_insurance_coverage_rules (id, collateral_type, business_segment, required_coverage_percentage, over_insurance_tolerance_pct, effective_from, effective_to, status, approved_by, approved_at, is_credit_policy_approved, maker_id, checker_id, created_at, updated_at)
VALUES
('cov-rule-default-fallback', 'All', 'All', 100.00, 10.00, '2026-01-01', '2026-12-31', 'Active', 'CREDIT_COMMITTEE', '2026-01-01T08:00:00', TRUE, 'ADMIN_USER', 'SYSADMIN', '2026-01-01T08:00:00', '2026-01-01T08:00:00')
ON CONFLICT (id) DO NOTHING;

-- 6. Seed Building Insurable Structure Values and Locations for Existing Seed Collaterals
UPDATE cims_collaterals
SET insurable_value = ROUND(valuation_amount * 0.75, 2),
    land_value = ROUND(valuation_amount * 0.25, 2),
    bank_assessed_value_source = 'CBS',
    identifier_source = 'CBS',
    location_address = COALESCE(location_address, 'Bole Subcity, Woreda 03, House 412'),
    location_city = COALESCE(location_city, 'Addis Ababa'),
    location_region = COALESCE(location_region, 'Addis Ababa'),
    property_cert_number = COALESCE(property_cert_number, title_deed_number, 'CERT-' || code)
WHERE category ILIKE '%immovable%' OR type ILIKE '%building%' OR type ILIKE '%property%';

UPDATE cims_collaterals
SET vehicle_plate_number = COALESCE(vehicle_plate_number, registration_number, '3-B92810-AA'),
    vehicle_chassis_number = COALESCE(vehicle_chassis_number, 'WVWZZZ3CZWE' || SUBSTRING(code FROM 1 FOR 6)),
    vehicle_engine_number = COALESCE(vehicle_engine_number, 'ENG-' || code),
    identifier_source = 'CBS'
WHERE category ILIKE '%movable%' OR category ILIKE '%vehicle%' OR type ILIKE '%vehicle%';
