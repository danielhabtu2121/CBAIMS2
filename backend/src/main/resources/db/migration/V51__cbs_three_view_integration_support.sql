-- =========================================================================
-- V51: CBS Production Three-View Integration, Staging, Snapshots & Provenance
-- =========================================================================

-- 1. Extend cims_customers with CBS origin, quality, and routing fields
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS origin_type VARCHAR(32) DEFAULT 'CBAIMS_REGISTERED';
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS cbs_source_ref VARCHAR(100);
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS import_batch_id VARCHAR(64);
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS import_date VARCHAR(50);
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS data_quality_status VARCHAR(64) DEFAULT 'COMPLETE';
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS enrichment_status VARCHAR(64) DEFAULT 'NOT_APPLICABLE';
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS assigned_cro VARCHAR(100);
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS district VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_cims_cust_origin ON cims_customers (origin_type);
CREATE INDEX IF NOT EXISTS idx_cims_cust_cbs_ref ON cims_customers (cbs_source_ref);
CREATE INDEX IF NOT EXISTS idx_cims_cust_batch ON cims_customers (import_batch_id);

-- 2. Extend cims_loan_accounts with CBS facility ID, origin, and routing fields
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS origin_type VARCHAR(32) DEFAULT 'CBAIMS_REGISTERED';
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS cbs_source_ref VARCHAR(100);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS cbs_facility_id VARCHAR(100);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS import_batch_id VARCHAR(64);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS import_date VARCHAR(50);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS data_quality_status VARCHAR(64) DEFAULT 'COMPLETE';
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS enrichment_status VARCHAR(64) DEFAULT 'NOT_APPLICABLE';
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS assigned_cro VARCHAR(100);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE cims_loan_accounts ADD COLUMN IF NOT EXISTS customer_match_status VARCHAR(64) DEFAULT 'MATCHED';

CREATE INDEX IF NOT EXISTS idx_cims_loan_origin ON cims_loan_accounts (origin_type);
CREATE INDEX IF NOT EXISTS idx_cims_loan_cbs_ref ON cims_loan_accounts (cbs_source_ref);
CREATE INDEX IF NOT EXISTS idx_cims_loan_cbs_fac_id ON cims_loan_accounts (cbs_facility_id);
CREATE INDEX IF NOT EXISTS idx_cims_loan_cust_match ON cims_loan_accounts (customer_match_status);

-- 3. Extend cbs_collateral_import_batches to track 3-view multi-entity statistics
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS customer_rows INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS customer_imported INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS customer_failed INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS facility_rows INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS facility_imported INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS facility_failed INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS collateral_rows INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS collateral_imported INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS collateral_failed INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS link_rows INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS link_imported INT DEFAULT 0;
ALTER TABLE cbs_collateral_import_batches ADD COLUMN IF NOT EXISTS link_unresolved INT DEFAULT 0;

-- 4. Immutable Raw Source CBS Snapshots for Customers
CREATE TABLE IF NOT EXISTS cbs_customer_raw_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64),
    cbs_customer_no VARCHAR(100) NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    raw_payload_json TEXT NOT NULL,
    source_status VARCHAR(50),
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_raw_cust_no ON cbs_customer_raw_snapshots (cbs_customer_no);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_cust_id ON cbs_customer_raw_snapshots (customer_id);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_cust_batch ON cbs_customer_raw_snapshots (batch_id);

-- 5. Immutable Raw Source CBS Snapshots for Facilities
CREATE TABLE IF NOT EXISTS cbs_facility_raw_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    facility_id VARCHAR(64),
    account_number VARCHAR(100) NOT NULL,
    cbs_facility_ref VARCHAR(100),
    batch_id VARCHAR(64) NOT NULL,
    raw_payload_json TEXT NOT NULL,
    source_status VARCHAR(50),
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_raw_fac_acc ON cbs_facility_raw_snapshots (account_number);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_fac_id ON cbs_facility_raw_snapshots (facility_id);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_fac_batch ON cbs_facility_raw_snapshots (batch_id);

-- 6. Field-Level Provenance & Audit Logs for Customers
CREATE TABLE IF NOT EXISTS cbs_customer_provenance_logs (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    original_cbs_value TEXT,
    previous_cbaims_value TEXT,
    new_cbaims_value TEXT,
    change_reason TEXT,
    supporting_doc_ref VARCHAR(100),
    changed_by VARCHAR(64) NOT NULL,
    changed_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_prov_cust_id ON cbs_customer_provenance_logs (customer_id);
CREATE INDEX IF NOT EXISTS idx_cbs_prov_cust_field ON cbs_customer_provenance_logs (field_name);

-- 7. Field-Level Provenance & Audit Logs for Facilities
CREATE TABLE IF NOT EXISTS cbs_facility_provenance_logs (
    id VARCHAR(64) PRIMARY KEY,
    facility_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    original_cbs_value TEXT,
    previous_cbaims_value TEXT,
    new_cbaims_value TEXT,
    change_reason TEXT,
    supporting_doc_ref VARCHAR(100),
    changed_by VARCHAR(64) NOT NULL,
    changed_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_prov_fac_id ON cbs_facility_provenance_logs (facility_id);
CREATE INDEX IF NOT EXISTS idx_cbs_prov_fac_field ON cbs_facility_provenance_logs (field_name);

-- 8. General Unresolved Queue for Unmatched References & Incomplete Keys
CREATE TABLE IF NOT EXISTS cbs_unresolved_entities (
    id VARCHAR(64) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL, -- CUSTOMER, FACILITY, COLLATERAL, LINKAGE
    batch_id VARCHAR(64) NOT NULL,
    source_key VARCHAR(100) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    details_json TEXT,
    status VARCHAR(50) DEFAULT 'UNRESOLVED',
    resolved_by VARCHAR(64),
    resolved_at VARCHAR(50),
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_unres_ent_type ON cbs_unresolved_entities (entity_type);
CREATE INDEX IF NOT EXISTS idx_cbs_unres_ent_batch ON cbs_unresolved_entities (batch_id);
CREATE INDEX IF NOT EXISTS idx_cbs_unres_ent_status ON cbs_unresolved_entities (status);

-- 9. General Duplicate Collision Queue for Customers and Facilities
CREATE TABLE IF NOT EXISTS cbs_duplicate_collisions (
    id VARCHAR(64) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL, -- CUSTOMER, FACILITY
    batch_id VARCHAR(64) NOT NULL,
    source_key VARCHAR(100) NOT NULL,
    cbaims_target_id VARCHAR(64) NOT NULL,
    match_reason VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING_REVIEW',
    reviewed_by VARCHAR(64),
    reviewed_at VARCHAR(50),
    review_notes TEXT,
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_dup_col_type ON cbs_duplicate_collisions (entity_type);
CREATE INDEX IF NOT EXISTS idx_cbs_dup_col_batch ON cbs_duplicate_collisions (batch_id);
CREATE INDEX IF NOT EXISTS idx_cbs_dup_col_status ON cbs_duplicate_collisions (status);

-- 10. Configurable CBS Code Mappings Table
CREATE TABLE IF NOT EXISTS cbs_code_mappings (
    id VARCHAR(64) PRIMARY KEY,
    mapping_type VARCHAR(50) NOT NULL, -- SEGMENT, BRANCH, CUSTOMER_TYPE, CUSTOMER_STATUS, COLLATERAL_TYPE, etc.
    source_code VARCHAR(100) NOT NULL,
    target_value VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(64),
    created_at VARCHAR(50) NOT NULL,
    updated_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_cbs_map_type ON cbs_code_mappings (mapping_type);
CREATE INDEX IF NOT EXISTS idx_cbs_map_source ON cbs_code_mappings (source_code);

-- Seed Initial Default Code Mappings
INSERT INTO cbs_code_mappings (id, mapping_type, source_code, target_value, description, is_active, created_at)
VALUES
    ('map-seg-1', 'SEGMENT', 'CORPORATE', 'Corporate Banking', 'CBS Corporate Segment mapping', true, CURRENT_TIMESTAMP),
    ('map-seg-2', 'SEGMENT', 'MSME', 'MSME Banking', 'CBS MSME Segment mapping', true, CURRENT_TIMESTAMP),
    ('map-seg-3', 'SEGMENT', 'MRETAIL', 'Retail Banking', 'CBS Retail Segment mapping', true, CURRENT_TIMESTAMP),
    ('map-seg-4', 'SEGMENT', 'IFB', 'Interest-Free Banking (IFB)', 'CBS IFB Segment mapping', true, CURRENT_TIMESTAMP),
    ('map-cust-type-1', 'CUSTOMER_TYPE', 'I', 'Individual', 'Individual customer type', true, CURRENT_TIMESTAMP),
    ('map-cust-type-2', 'CUSTOMER_TYPE', 'C', 'Corporate', 'Corporate customer type', true, CURRENT_TIMESTAMP),
    ('map-cust-type-3', 'CUSTOMER_TYPE', 'B', 'Business', 'Business customer type', true, CURRENT_TIMESTAMP),
    ('map-cust-stat-1', 'CUSTOMER_STATUS', 'A', 'Active', 'Active customer status', true, CURRENT_TIMESTAMP),
    ('map-cust-stat-2', 'CUSTOMER_STATUS', 'C', 'Closed', 'Closed customer status', true, CURRENT_TIMESTAMP),
    ('map-cust-stat-3', 'CUSTOMER_STATUS', 'I', 'Inactive', 'Inactive customer status', true, CURRENT_TIMESTAMP),
    ('map-sec-1', 'SECURED', 'Y', 'true', 'Secured indicator Y', true, CURRENT_TIMESTAMP),
    ('map-sec-2', 'SECURED', 'N', 'false', 'Secured indicator N', true, CURRENT_TIMESTAMP),
    ('map-tan-1', 'TANGIBLE', 'Y', 'true', 'Tangible indicator Y', true, CURRENT_TIMESTAMP),
    ('map-tan-2', 'TANGIBLE', 'N', 'false', 'Tangible indicator N', true, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

