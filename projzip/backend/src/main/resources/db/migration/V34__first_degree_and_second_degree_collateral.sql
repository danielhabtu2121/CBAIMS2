-- ==============================================================================
-- Migration V34: Module 4 Enhancement — First-Degree & Second-Degree Collateral Architecture
-- Database: cims_db_dev
-- Grounded in: Banking Mortgage Priority, Exposure Controls & Insurance Adequacy Rules
-- ==============================================================================

-- 1. Extend cims_collaterals with First-Degree / Second-Degree attributes
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS collateral_rank VARCHAR(10) DEFAULT '1st';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS is_first_degree_internal BOOLEAN DEFAULT TRUE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_collateral_id VARCHAR(64);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_holder_name VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_facility_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_outstanding_balance NUMERIC(18,2) DEFAULT 0.00;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_balance_as_of_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS registered_mortgage_amount NUMERIC(18,2) DEFAULT 0.00;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS mortgage_registration_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS mortgage_registration_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_policy_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_insurer VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_policy_expiry VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bank_noted_as_second_mortgagee BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_confirmation_doc_id VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS first_degree_extract_doc_id VARCHAR(100);

-- Promotion and Review Tracking Fields
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_eligible BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_status VARCHAR(50) DEFAULT 'NONE';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_reason TEXT;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_flag_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_review_notes TEXT;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_reviewed_by VARCHAR(64);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS promotion_reviewed_at VARCHAR(50);

-- Populate default first-degree metadata for pre-existing collaterals
UPDATE cims_collaterals
SET collateral_rank = '1st',
    is_first_degree_internal = TRUE,
    first_degree_holder_name = COALESCE(first_degree_holder_name, 'Dashen Bank S.C.'),
    promotion_status = COALESCE(promotion_status, 'NONE'),
    promotion_eligible = COALESCE(promotion_eligible, FALSE),
    first_degree_outstanding_balance = COALESCE(first_degree_outstanding_balance, 0.00),
    bank_noted_as_second_mortgagee = COALESCE(bank_noted_as_second_mortgagee, FALSE)
WHERE collateral_rank IS NULL;

-- 2. Indexes for fast query and integrity
CREATE INDEX IF NOT EXISTS idx_collaterals_rank ON cims_collaterals (collateral_rank);
CREATE INDEX IF NOT EXISTS idx_collaterals_first_degree_id ON cims_collaterals (first_degree_collateral_id);
CREATE INDEX IF NOT EXISTS idx_collaterals_promo_eligible ON cims_collaterals (promotion_eligible);
CREATE INDEX IF NOT EXISTS idx_collaterals_promo_status ON cims_collaterals (promotion_status);

-- 3. Seed Realistic Collaterals Demonstrating 1st and 2nd Degree Workflows

-- Pair 1: Internal 1st-Degree (col-m3-multi-01) with Second-Degree Collateral (col-2nd-internal-01)
-- The 1st-degree collateral is 'col-m3-multi-01' (Awash Headquarter Commercial Office Tower, MV: 45,000,000 ETB, Haircut: 20%, NSV: 36,000,000 ETB)
UPDATE cims_collaterals
SET collateral_rank = '1st',
    is_first_degree_internal = TRUE,
    first_degree_holder_name = 'Dashen Bank S.C.',
    first_degree_facility_ref = 'LN-RET-2026-003',
    registered_mortgage_amount = 30000000.00,
    mortgage_registration_number = 'REG-MORT-AA-2024-4410',
    mortgage_registration_date = '2024-03-12',
    first_degree_policy_number = 'POL-NYALA-2024-001',
    first_degree_insurer = 'Nyala Insurance S.C.',
    first_degree_policy_expiry = '2027-03-12',
    bank_noted_as_second_mortgagee = FALSE
WHERE id = 'col-m3-multi-01';

-- Second-Degree record linking directly to internal first-degree collateral 'col-m3-multi-01'
INSERT INTO cims_collaterals (
    id, code, customer_id, description, category, type, owner_type, currency,
    valuation_amount, haircut, nsv, current_allocation, utilization_percentage,
    insurance_status, status, charge_type, secured, owning_segment, branch, source,
    collateral_rank, is_first_degree_internal, first_degree_collateral_id,
    first_degree_holder_name, first_degree_facility_ref, first_degree_outstanding_balance,
    first_degree_balance_as_of_date, registered_mortgage_amount, mortgage_registration_number,
    mortgage_registration_date, first_degree_policy_number, first_degree_insurer,
    first_degree_policy_expiry, bank_noted_as_second_mortgagee,
    promotion_eligible, promotion_status
)
VALUES (
    'col-2nd-internal-01', 'COL-2ND-INT-001', 'cust-m3-demo',
    'Awash Headquarter Commercial Office Tower (Second-Degree Mortgage Charge)',
    'Immovable Properties', 'Buildings - Commercial', 'Borrower-owned', 'ETB',
    45000000.00, 20.00, 36000000.00, 10000000.00, 27.78,
    'Adequately Insured', 'Active', 'Mortgage', true, 'Corporate Banking', 'BRN-001', 'CBS_SIM',
    '2nd', TRUE, 'col-m3-multi-01',
    'Dashen Bank S.C.', 'LN-RET-2026-003', 12000000.00,
    '2026-10-15', 15000000.00, 'REG-2ND-MORT-AA-8812',
    '2026-05-18', 'POL-NYALA-2024-001', 'Nyala Insurance S.C.',
    '2027-03-12', TRUE,
    FALSE, 'NONE'
)
ON CONFLICT (id) DO UPDATE SET
    collateral_rank = EXCLUDED.collateral_rank,
    is_first_degree_internal = EXCLUDED.is_first_degree_internal,
    first_degree_collateral_id = EXCLUDED.first_degree_collateral_id,
    first_degree_holder_name = EXCLUDED.first_degree_holder_name,
    first_degree_facility_ref = EXCLUDED.first_degree_facility_ref,
    first_degree_outstanding_balance = EXCLUDED.first_degree_outstanding_balance,
    first_degree_balance_as_of_date = EXCLUDED.first_degree_balance_as_of_date,
    registered_mortgage_amount = EXCLUDED.registered_mortgage_amount,
    mortgage_registration_number = EXCLUDED.mortgage_registration_number,
    mortgage_registration_date = EXCLUDED.mortgage_registration_date,
    first_degree_policy_number = EXCLUDED.first_degree_policy_number,
    first_degree_insurer = EXCLUDED.first_degree_insurer,
    first_degree_policy_expiry = EXCLUDED.first_degree_policy_expiry,
    bank_noted_as_second_mortgagee = EXCLUDED.bank_noted_as_second_mortgagee;

-- Supporting DMS Documents for External Second-Degree Collateral
INSERT INTO cims_ownership_documents (
    id, entity_type, entity_id, collateral_id, name, type, file_name, file_size, content_type,
    file_content, verification_status, upload_date, uploaded_by, uploaded_at, expiry_date,
    file_url, status, version, is_latest, version_notes, remarks
)
VALUES
(
    'doc-cbe-conf-01', 'Collateral', 'col-2nd-external-02', 'col-2nd-external-02',
    'Commercial Bank of Ethiopia (CBE) 1st Degree Consent & Outstanding Balance Confirmation Letter',
    'Confirmation Letter', 'CBE_First_Degree_Confirmation_Letter.pdf', 145200, 'application/pdf',
    'JVBERi0xLjQKJcTl8uXrCg==', 'Verified', '2026-09-10', 'MGRCOLLDOC', '2026-09-10T10:00:00', '2027-09-10',
    '/api/documents/download/doc-cbe-conf-01', 'Active', 1, true,
    'Official bank-to-bank confirmation letter confirming CBE first-degree mortgage balance and consent for second charge',
    'Verified by Legal & Collateral Documentation Department'
),
(
    'doc-cbe-extract-01', 'Collateral', 'col-2nd-external-02', 'col-2nd-external-02',
    'Federal Land & Mortgage Registry Official Extract Certificate',
    'Registration Extract', 'Cadastre_Mortgage_Registry_Extract_AA.pdf', 210400, 'application/pdf',
    'JVBERi0xLjQKJcTl8uXrCg==', 'Verified', '2026-09-12', 'MGRCOLLDOC', '2026-09-12T14:30:00', '2028-09-12',
    '/api/documents/download/doc-cbe-extract-01', 'Active', 1, true,
    'Certified cadastral registration extract showing first charge held by CBE and second charge registered in favor of Dashen Bank S.C.',
    'Verified by Collateral Documentation Department'
)
ON CONFLICT (id) DO NOTHING;

-- Pair 2: External 1st-Degree (Held by Commercial Bank of Ethiopia) with Second-Degree Collateral
INSERT INTO cims_collaterals (
    id, code, customer_id, description, category, type, owner_type, currency,
    valuation_amount, haircut, nsv, current_allocation, utilization_percentage,
    insurance_status, status, charge_type, secured, owning_segment, branch, source,
    collateral_rank, is_first_degree_internal, first_degree_collateral_id,
    first_degree_holder_name, first_degree_facility_ref, first_degree_outstanding_balance,
    first_degree_balance_as_of_date, registered_mortgage_amount, mortgage_registration_number,
    mortgage_registration_date, first_degree_policy_number, first_degree_insurer,
    first_degree_policy_expiry, bank_noted_as_second_mortgagee,
    first_degree_confirmation_doc_id, first_degree_extract_doc_id,
    promotion_eligible, promotion_status
)
VALUES (
    'col-2nd-external-02', 'COL-2ND-EXT-002', 'cust-m3-demo',
    'Bole Commercial Complex & Exhibition Center (Second-Degree Charge)',
    'Immovable Properties', 'Buildings - Commercial', 'Borrower-owned', 'ETB',
    60000000.00, 20.00, 48000000.00, 15000000.00, 31.25,
    'Adequately Insured', 'Active', 'Mortgage', true, 'Corporate Banking', 'BRN-001', 'CBS_SIM',
    '2nd', FALSE, NULL,
    'Commercial Bank of Ethiopia', 'CBE-CORP-LN-99214', 18000000.00,
    '2026-10-01', 25000000.00, 'AA-LAND-MORT-55219',
    '2025-11-20', 'EIC-PROP-2025-8832', 'Ethiopian Insurance Corporation',
    '2027-02-15', TRUE,
    'doc-cbe-conf-01', 'doc-cbe-extract-01',
    FALSE, 'NONE'
)
ON CONFLICT (id) DO UPDATE SET
    collateral_rank = EXCLUDED.collateral_rank,
    is_first_degree_internal = EXCLUDED.is_first_degree_internal,
    first_degree_holder_name = EXCLUDED.first_degree_holder_name,
    first_degree_facility_ref = EXCLUDED.first_degree_facility_ref,
    first_degree_outstanding_balance = EXCLUDED.first_degree_outstanding_balance,
    first_degree_balance_as_of_date = EXCLUDED.first_degree_balance_as_of_date,
    registered_mortgage_amount = EXCLUDED.registered_mortgage_amount,
    mortgage_registration_number = EXCLUDED.mortgage_registration_number,
    mortgage_registration_date = EXCLUDED.mortgage_registration_date,
    first_degree_policy_number = EXCLUDED.first_degree_policy_number,
    first_degree_insurer = EXCLUDED.first_degree_insurer,
    first_degree_policy_expiry = EXCLUDED.first_degree_policy_expiry,
    bank_noted_as_second_mortgagee = EXCLUDED.bank_noted_as_second_mortgagee,
    first_degree_confirmation_doc_id = EXCLUDED.first_degree_confirmation_doc_id,
    first_degree_extract_doc_id = EXCLUDED.first_degree_extract_doc_id;

-- Seed Policy for col-2nd-external-02 testing combined exposure adequacy
INSERT INTO cims_insurance_policies (
    id, policy_number, collateral_id, customer_id, insurer_name, coverage_type,
    insured_amount, premium, effective_date, expiry_date,
    status, maker_id, maker_dt_stamp, record_stat, auth_stat, version
)
VALUES (
    'pol-2nd-ext-01', 'POL-EIC-2ND-001', 'col-2nd-external-02', 'cust-m3-demo',
    'Ethiopian Insurance Corporation', 'Fire & Allied Perils',
    60000000.00, 180000.00, '2026-02-15', '2027-02-15',
    'Active', 'CRO_USER', '2026-02-15T09:00:00', 'O', 'A', 1
)
ON CONFLICT (id) DO NOTHING;

