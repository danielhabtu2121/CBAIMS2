-- ==============================================================================
-- Migration V40: Link-Level Charge Priority, Ownership Separation & Dynamic Taxonomy
-- Database: cims_db_dev
-- Grounded in: Banking Mortgage Priority, Decoupled Legal Owner vs Facility Borrower
-- ==============================================================================

-- 1. Extend cims_loan_collateral_links with Link-Level Charge Rank & 1st-Degree Priority
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS collateral_rank VARCHAR(10) DEFAULT '1st';
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS is_first_degree_internal BOOLEAN DEFAULT TRUE;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_collateral_id VARCHAR(64);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_holder_name VARCHAR(255);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_facility_ref VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_outstanding_balance NUMERIC(18,2) DEFAULT 0.00;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_balance_as_of_date VARCHAR(50);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS registered_mortgage_amount NUMERIC(18,2) DEFAULT 0.00;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS mortgage_registration_number VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS mortgage_registration_date VARCHAR(50);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_policy_number VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_insurer VARCHAR(255);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_policy_expiry VARCHAR(50);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS bank_noted_as_second_mortgagee BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_confirmation_doc_id VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS first_degree_extract_doc_id VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS borrower_customer_id VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS legal_owner_customer_id VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS legal_owner_name VARCHAR(255);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS owner_without_cif BOOLEAN DEFAULT FALSE;

-- 2. Extend cims_loan_collateral_link_history for Audit Trail
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS collateral_rank VARCHAR(10);
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS borrower_customer_id VARCHAR(100);
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS legal_owner_customer_id VARCHAR(100);

-- 3. Extend cims_collaterals with Dynamic Subtype Registration Attributes
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS livestock_tag_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS livestock_breed VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS livestock_quantity INT;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS veterinary_health_cert_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS farm_crop_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS farm_land_holding_cert VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_receipt_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_name VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_commodity_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_expiry_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_registration_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_title VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_filing_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_expiry_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_name VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantee_agreement_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS board_resolution_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantee_expiry_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS owner_without_cif BOOLEAN DEFAULT FALSE;

-- 4. Backfill existing links with rank, 1st-degree metadata, and owner/borrower separation
UPDATE cims_loan_collateral_links l
SET collateral_rank = COALESCE(c.collateral_rank, '1st'),
    is_first_degree_internal = COALESCE(c.is_first_degree_internal, TRUE),
    first_degree_collateral_id = c.first_degree_collateral_id,
    first_degree_holder_name = COALESCE(c.first_degree_holder_name, 'Dashen Bank S.C.'),
    first_degree_facility_ref = c.first_degree_facility_ref,
    first_degree_outstanding_balance = COALESCE(c.first_degree_outstanding_balance, 0.00),
    first_degree_balance_as_of_date = c.first_degree_balance_as_of_date,
    registered_mortgage_amount = COALESCE(c.registered_mortgage_amount, 0.00),
    mortgage_registration_number = c.mortgage_registration_number,
    mortgage_registration_date = c.mortgage_registration_date,
    first_degree_policy_number = c.first_degree_policy_number,
    first_degree_insurer = c.first_degree_insurer,
    first_degree_policy_expiry = c.first_degree_policy_expiry,
    bank_noted_as_second_mortgagee = COALESCE(c.bank_noted_as_second_mortgagee, FALSE),
    first_degree_confirmation_doc_id = c.first_degree_confirmation_doc_id,
    first_degree_extract_doc_id = c.first_degree_extract_doc_id,
    legal_owner_customer_id = COALESCE(l.legal_owner_customer_id, c.customer_id),
    borrower_customer_id = COALESCE(l.borrower_customer_id, l.customer_id)
FROM cims_collaterals c
WHERE l.collateral_id = c.id OR l.collateral_id = c.code;

-- Ensure default rank for any link without rank
UPDATE cims_loan_collateral_links
SET collateral_rank = '1st'
WHERE collateral_rank IS NULL;

-- 5. Indexes for fast query performance & data integrity
CREATE INDEX IF NOT EXISTS idx_links_rank ON cims_loan_collateral_links (collateral_rank);
CREATE INDEX IF NOT EXISTS idx_links_borrower ON cims_loan_collateral_links (borrower_customer_id);
CREATE INDEX IF NOT EXISTS idx_links_legal_owner ON cims_loan_collateral_links (legal_owner_customer_id);

-- 6. Seed Complete Mandatory Document Rules according to Clarification Log §2
INSERT INTO cims_mandatory_document_rules (
    id, collateral_category, collateral_type, document_type, mandatory, active, applicability_condition
) VALUES
    ('doc-r-arch-drawings', 'Immovable Properties', 'Building - Under Construction', 'Architectural Design Drawings', TRUE, TRUE, 'Approved architectural and structural drawings stamped by municipality'),
    ('doc-r-eng-progress', 'Immovable Properties', 'Building - Under Construction', 'Engineer Progress Report', TRUE, TRUE, 'Current milestone progress report signed by accredited supervising engineer'),
    ('doc-r-veh-inspection', 'Movable Properties', 'Vehicles', 'Roadworthiness Certificate', TRUE, TRUE, 'Annual roadworthiness inspection certificate from Transport Authority'),
    ('doc-r-mach-customs', 'Movable Properties', 'Machinery', 'Customs Clearance Declaration', TRUE, TRUE, 'Ethiopian Revenues & Customs Authority clearance certificate or import receipt'),
    ('doc-r-mach-inspection', 'Movable Properties', 'Machinery', 'Machine Technical Inspection', TRUE, TRUE, 'Technical inspection report validating machinery operational condition and serial number'),
    ('doc-r-livestock-health', 'Agricultural/Other', 'Livestock', 'Veterinary Health Certificate', TRUE, TRUE, 'Official veterinary health and vaccination certificate'),
    ('doc-r-farm-inspection', 'Agricultural/Other', 'Farm Products', 'Commodity Inspection Report', TRUE, TRUE, 'Commodity quality grading and moisture/pest inspection certificate'),
    ('doc-r-guarantee-resolution', 'Guarantees', 'Corporate Guarantee', 'Board Resolution / Power of Attorney', TRUE, TRUE, 'Certified corporate board resolution authorising the corporate guarantee and signing powers'),
    ('doc-r-guarantee-agreement', 'Guarantees', 'All', 'Guarantee Agreement', TRUE, TRUE, 'Duly signed and legally stamped guarantee agreement')
ON CONFLICT (id) DO UPDATE SET
    collateral_category = EXCLUDED.collateral_category,
    collateral_type = EXCLUDED.collateral_type,
    document_type = EXCLUDED.document_type,
    mandatory = EXCLUDED.mandatory,
    active = EXCLUDED.active,
    applicability_condition = EXCLUDED.applicability_condition;

