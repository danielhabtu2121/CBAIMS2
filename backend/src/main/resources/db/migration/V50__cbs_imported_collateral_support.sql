-- =========================================================================
-- V50: Production CBS-Imported Collateral Management, Provenance, & Quality
-- =========================================================================

-- 1. Extend cims_collaterals with origin, quality, and CRO assignment tracking
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS origin_type VARCHAR(32) DEFAULT 'CBAIMS_REGISTERED';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS data_quality_status VARCHAR(64) DEFAULT 'COMPLETE';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS assigned_cro VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS cbs_collateral_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS import_batch_id VARCHAR(64);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS import_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS enrichment_status VARCHAR(64) DEFAULT 'NOT_APPLICABLE';

-- Ensure all existing baseline collateral records retain CBAIMS_REGISTERED origin
UPDATE cims_collaterals 
SET origin_type = 'CBAIMS_REGISTERED' 
WHERE origin_type IS NULL;

UPDATE cims_collaterals 
SET data_quality_status = 'COMPLETE' 
WHERE data_quality_status IS NULL;

UPDATE cims_collaterals 
SET enrichment_status = 'NOT_APPLICABLE' 
WHERE enrichment_status IS NULL;

-- 2. Indexes for Collateral Master origin, data quality work-queues, and organizational assignment
CREATE INDEX IF NOT EXISTS idx_cims_col_origin ON cims_collaterals (origin_type);
CREATE INDEX IF NOT EXISTS idx_cims_col_quality ON cims_collaterals (data_quality_status);
CREATE INDEX IF NOT EXISTS idx_cims_col_cro ON cims_collaterals (assigned_cro);
CREATE INDEX IF NOT EXISTS idx_cims_col_district ON cims_collaterals (district);
CREATE INDEX IF NOT EXISTS idx_cims_col_cbs_ref ON cims_collaterals (cbs_collateral_ref);
CREATE INDEX IF NOT EXISTS idx_cims_col_batch ON cims_collaterals (import_batch_id);

-- 3. CBS Import Batch History & Progress Tracking
CREATE TABLE IF NOT EXISTS cbs_collateral_import_batches (
    id VARCHAR(64) PRIMARY KEY,
    batch_reference VARCHAR(64) NOT NULL UNIQUE,
    start_time VARCHAR(50),
    end_time VARCHAR(50),
    total_rows INT DEFAULT 0,
    imported_count INT DEFAULT 0,
    quarantined_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    potential_duplicates_count INT DEFAULT 0,
    unresolved_links_count INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'RUNNING',
    source_system VARCHAR(50) DEFAULT 'CBS',
    created_by VARCHAR(64),
    notes TEXT,
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_cbs_batch_ref ON cbs_collateral_import_batches (batch_reference);
CREATE INDEX IF NOT EXISTS idx_cbs_batch_status ON cbs_collateral_import_batches (status);

-- 4. Immutable Raw Source CBS Snapshots (Preservation of Original Flexcube Payload)
CREATE TABLE IF NOT EXISTS cbs_collateral_raw_snapshots (
    id VARCHAR(64) PRIMARY KEY,
    collateral_id VARCHAR(64),
    cbs_collateral_ref VARCHAR(100) NOT NULL,
    batch_id VARCHAR(64) NOT NULL,
    raw_payload_json TEXT NOT NULL,
    source_status VARCHAR(50),
    created_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_raw_col_ref ON cbs_collateral_raw_snapshots (cbs_collateral_ref);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_col_id ON cbs_collateral_raw_snapshots (collateral_id);
CREATE INDEX IF NOT EXISTS idx_cbs_raw_batch ON cbs_collateral_raw_snapshots (batch_id);

-- 5. Field-Level Provenance & Audit Log (Tracking Original vs Enriched Values & Reasons)
CREATE TABLE IF NOT EXISTS cbs_collateral_provenance_logs (
    id VARCHAR(64) PRIMARY KEY,
    collateral_id VARCHAR(64) NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    original_cbs_value TEXT,
    previous_cbaims_value TEXT,
    new_cbaims_value TEXT,
    change_reason TEXT,
    supporting_doc_ref VARCHAR(100),
    changed_by VARCHAR(64) NOT NULL,
    changed_at VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cbs_prov_col_id ON cbs_collateral_provenance_logs (collateral_id);
CREATE INDEX IF NOT EXISTS idx_cbs_prov_field ON cbs_collateral_provenance_logs (field_name);

-- 6. Unresolved CBS Facility-to-Collateral Linkages (Preserves relations without failing master import)
CREATE TABLE IF NOT EXISTS cbs_unresolved_linkages (
    id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    cbs_collateral_ref VARCHAR(100) NOT NULL,
    cims_collateral_id VARCHAR(64),
    account_number VARCHAR(100),
    branch_code VARCHAR(50),
    linkage_type VARCHAR(50),
    linked_amount NUMERIC(18,2),
    status VARCHAR(50) DEFAULT 'UNRESOLVED',
    notes TEXT,
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_cbs_unres_batch ON cbs_unresolved_linkages (batch_id);
CREATE INDEX IF NOT EXISTS idx_cbs_unres_status ON cbs_unresolved_linkages (status);
CREATE INDEX IF NOT EXISTS idx_cbs_unres_ref ON cbs_unresolved_linkages (cbs_collateral_ref);

-- 7. Potential Duplicate Collision Review Queue (Prevents silent overwrites or duplicate active masters)
CREATE TABLE IF NOT EXISTS cbs_collateral_duplicates_queue (
    id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    cbs_collateral_ref VARCHAR(100) NOT NULL,
    potential_cbaims_collateral_id VARCHAR(64) NOT NULL,
    match_reason VARCHAR(255),
    status VARCHAR(50) DEFAULT 'PENDING_REVIEW',
    reviewed_by VARCHAR(64),
    reviewed_at VARCHAR(50),
    review_notes TEXT,
    created_at VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_cbs_dup_batch ON cbs_collateral_duplicates_queue (batch_id);
CREATE INDEX IF NOT EXISTS idx_cbs_dup_status ON cbs_collateral_duplicates_queue (status);

