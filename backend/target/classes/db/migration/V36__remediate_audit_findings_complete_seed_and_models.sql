-- =========================================================================
-- V36: Remediate Audit Findings - Complete Model Schema & Seed Data
-- Covers Items 6, 27, 32, 33, 36, 40:
-- 1. EMCR and ESG Compliance columns on cims_collaterals
-- 2. Dedicated cims_insurance_claims table for claims management
-- 3. Complete authoritative organizational hierarchy & Sharika IFB Branch
-- 4. Complete mandatory document rules for all collateral taxonomies
-- 5. Exact scheduler-aligned notification templates
-- =========================================================================

-- 1. Add EMCR and ESG columns to cims_collaterals
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS emcr_registration_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS emcr_status VARCHAR(50) DEFAULT 'Registered';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS emcr_registration_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS esg_exclusion_flag BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS esg_screening_status VARCHAR(50) DEFAULT 'Approved';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS esg_notes TEXT;

-- 2. Dedicated Insurance Claims table (Item 40)
CREATE TABLE IF NOT EXISTS cims_insurance_claims (
    id VARCHAR(64) PRIMARY KEY,
    claim_number VARCHAR(100) NOT NULL UNIQUE,
    policy_id VARCHAR(64) NOT NULL,
    collateral_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(64) NOT NULL,
    claim_status VARCHAR(50) NOT NULL DEFAULT 'Submitted',
    claim_type VARCHAR(100) NOT NULL DEFAULT 'Damage',
    incident_date VARCHAR(50) NOT NULL,
    claim_filing_date VARCHAR(50) NOT NULL,
    claim_amount DOUBLE PRECISION NOT NULL,
    settled_amount DOUBLE PRECISION DEFAULT 0.0,
    insurer_name VARCHAR(255) NOT NULL,
    assigned_adjuster VARCHAR(255),
    supporting_dms_ref VARCHAR(255),
    remarks TEXT,
    created_at VARCHAR(50),
    updated_at VARCHAR(50),
    maker_id VARCHAR(64),
    checker_id VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_claims_collateral ON cims_insurance_claims(collateral_id);
CREATE INDEX IF NOT EXISTS idx_claims_policy ON cims_insurance_claims(policy_id);
CREATE INDEX IF NOT EXISTS idx_claims_customer ON cims_insurance_claims(customer_id);
CREATE INDEX IF NOT EXISTS idx_claims_status ON cims_insurance_claims(claim_status);

-- 3. Dedicated IFB Branch in cims_branches and Complete Organizational Hierarchy (Item 32)
INSERT INTO cims_branches (id, code, name, type, parent_district_id, area_office, segment_ids, is_active)
VALUES 
    ('brn-ifb-sharika', 'IFB-001', 'Sharika IFB Branch', 'Branch', 'brn-dist-ifb', 'Central Islamic Banking Area Office', '["Interest-Free Banking (IFB)"]', true),
    ('brn-addis-bole', 'BR001', 'Bole Special Branch', 'Branch', 'brn-dist-east', 'Bole Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-addis-merkato', 'BR002', 'Merkato Branch', 'Branch', 'brn-dist-west', 'Merkato Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-addis-arada', 'BR003', 'Arada Branch', 'Branch', 'brn-dist-north', 'Arada Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-addis-kirkos', 'BR004', 'Kirkos Branch', 'Branch', 'brn-dist-central', 'Kirkos Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-oromia-adama', 'BR005', 'Adama Branch', 'Branch', 'brn-dist-oromia', 'Adama Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-amhara-bahirdar', 'BR006', 'Bahir Dar Branch', 'Branch', 'brn-dist-amhara', 'Bahir Dar Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-snnpr-hawassa', 'BR007', 'Hawassa Branch', 'Branch', 'brn-dist-south', 'Hawassa Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true),
    ('brn-diredawa-main', 'BR008', 'Dire Dawa Main Branch', 'Branch', 'brn-dist-eastern', 'Dire Dawa Area Office', '["Corporate Banking", "Retail Banking", "MSME Banking", "Interest-Free Banking (IFB)"]', true)
ON CONFLICT (id) DO UPDATE SET 
    name = EXCLUDED.name,
    parent_district_id = EXCLUDED.parent_district_id,
    area_office = EXCLUDED.area_office,
    segment_ids = EXCLUDED.segment_ids,
    is_active = true;

-- Complete District Hierarchy Seed
INSERT INTO cims_district_hierarchies (id, district_name, area_office, branch_name)
VALUES
    ('dist-ifb-1', 'Addis Ababa IFB District', 'Central Islamic Banking Area Office', 'Sharika IFB Branch'),
    ('dist-east-1', 'East Addis District', 'Bole Area Office', 'Bole Special Branch'),
    ('dist-east-2', 'East Addis District', 'Bole Area Office', 'Bole Medhanealem Branch'),
    ('dist-west-1', 'West Addis District', 'Merkato Area Office', 'Merkato Branch'),
    ('dist-west-2', 'West Addis District', 'Merkato Area Office', 'Teklehaimanot Branch'),
    ('dist-north-1', 'North Addis District', 'Arada Area Office', 'Arada Branch'),
    ('dist-north-2', 'North Addis District', 'Arada Area Office', 'Piazza Branch'),
    ('dist-central-1', 'Central District', 'Kirkos Area Office', 'Kirkos Branch'),
    ('dist-central-2', 'Central District', 'Kirkos Area Office', 'Head Office Branch'),
    ('dist-oromia-1', 'East Oromia District', 'Adama Area Office', 'Adama Branch'),
    ('dist-oromia-2', 'West Oromia District', 'Jimma Area Office', 'Jimma Branch'),
    ('dist-amhara-1', 'North West Amhara District', 'Bahir Dar Area Office', 'Bahir Dar Branch'),
    ('dist-amhara-2', 'South Amhara District', 'Dessie Area Office', 'Dessie Branch'),
    ('dist-south-1', 'South District', 'Hawassa Area Office', 'Hawassa Branch'),
    ('dist-east-region-1', 'Eastern District', 'Dire Dawa Area Office', 'Dire Dawa Main Branch')
ON CONFLICT (id) DO UPDATE SET
    district_name = EXCLUDED.district_name,
    area_office = EXCLUDED.area_office,
    branch_name = EXCLUDED.branch_name;

-- 4. Complete Mandatory Document Rules Seed (Items 6 & 36)
INSERT INTO cims_mandatory_document_rules (id, collateral_category, collateral_type, document_type, applicability_condition, mandatory, active)
VALUES
    ('doc-r-title-deed', 'Real Estate', 'All', 'Title Deed / Property Ownership Certificate', 'Authoritative cadastral title certificate or deed', true, true),
    ('doc-r-mortgage-reg', 'Real Estate', 'Mortgage Registration', 'Mortgage Registration Confirmation Letter', 'Applicable upon Cadastral Land Registry mortgage filing', false, true),
    ('doc-r-boq-under-con', 'Immovable Properties', 'Building - Under Construction', 'Bill of Quantities (BOQ)', 'Certified engineering Bill of Quantities (BOQ) with remaining works schedule', true, true),
    ('doc-r-building-permit', 'Immovable Properties', 'Building - Under Construction', 'Building Permit & Architectural Drawings', 'Approved municipal building permit and architectural design drawings', true, true),
    ('doc-r-lease-agreement', 'Immovable Properties', 'Building - Leasehold', 'Lease Agreement and Lease Fee Payment Receipt', 'Government lease contract and latest lease fee clearance receipt', true, true),
    ('doc-r-lease-land', 'Land', 'Land - Leasehold', 'Lease Agreement and Lease Fee Payment Receipt', 'Government lease contract and latest lease fee clearance receipt', true, true),
    ('doc-r-vehicle-liber', 'Movable Properties', 'Vehicles', 'Vehicle Ownership Certificate (Libre)', 'Official Ministry of Transport vehicle ownership booklet/libre', true, true),
    ('doc-r-livestock-eartag', 'Agricultural/Other', 'Livestock', 'Livestock Ear-tag Identification Certificate', 'Veterinary/agricultural ear-tag registration certificate and veterinary health record', true, true),
    ('doc-r-warehouse-receipt', 'Agricultural/Other', 'Merchandise', 'Warehouse Receipt', 'Certified warehouse storage receipt and commodity inspection certificate', true, true),
    ('doc-r-ip-cert', 'Intangible Assets', 'Intellectual Property', 'Intellectual Property Registration Certificate', 'Ethiopian Intellectual Property Authority registration extract/certificate', true, true),
    ('doc-r-custody-agreement', 'Movable Properties', 'Third-Party Custody', 'Collateral Custody Agreement', 'Signed tripartite collateral custody and warehousing agreement', false, true),
    ('doc-r-valuation-report', 'All', 'All', 'Valuation Report', 'Comprehensive appraisal report by bank approved valuation engineer', true, true)
ON CONFLICT (id) DO UPDATE SET
    collateral_category = EXCLUDED.collateral_category,
    collateral_type = EXCLUDED.collateral_type,
    document_type = EXCLUDED.document_type,
    applicability_condition = EXCLUDED.applicability_condition,
    mandatory = EXCLUDED.mandatory,
    active = true;

-- 5. Exact Scheduler Notification Templates (Item 33)
INSERT INTO cims_notification_templates (id, name, type, subject, body, trigger_days_before, active)
VALUES
    ('tmpl-pre-expiry-30', 'PRE_EXPIRY_30', 'Email',
     'Advance Notice: Insurance Policy {policy number} Expiring in 30 Days',
     'Dear {customer name},\n\nPlease be advised that Insurance Policy {policy number} covering collateral {collateral code} is scheduled to expire on {expiry date} ({days remaining} days remaining).\n\nPlease initiate renewal through your insurance provider {insurer name} and submit the renewed policy schedule to your branch ({branch}) or Relationship Manager ({rm name}, {rm phone}).\n\nThank you,\nCBA Credit & Collateral Management',
     30, true),

    ('tmpl-pre-expiry-10', 'PRE_EXPIRY_10', 'Email',
     'Urgent Reminder: Insurance Policy {policy number} Expiring in 10 Days',
     'Dear {customer name},\n\nThis is an urgent reminder that Insurance Policy {policy number} for collateral {collateral code} will expire on {expiry date} (in {days remaining} days).\n\nImmediate renewal is required to prevent compliance escalation and avoid bank-placed coverage at your expense.\n\nBranch: {branch} | Relationship Manager: {rm name} ({rm phone})',
     10, true),

    ('tmpl-post-expiry-10', 'POST_EXPIRY_ESCALATION_10', 'Email',
     'CRITICAL ESCALATION: Insurance Policy {policy number} is {days overdue} Days Overdue',
     'ESCALATION NOTICE:\n\nInsurance Policy {policy number} for borrower {customer name} (Collateral: {collateral code}, Branch: {branch}) expired on {expiry date} and is now {days overdue} days overdue.\n\nInsured Amount: ETB {insured amount}\nInsurer: {insurer name}\n\nBranch Manager intervention is required immediately. If renewed cover note is not received within 5 working days, proceed with forced placement or facility debiting.',
     0, true),

    ('tmpl-post-expiry-15-consolidated', 'POST_EXPIRY_ESCALATION_15_CONSOLIDATED', 'Email',
     'CIMS 15-Day Consolidated Overdue Insurance Report: {district name}',
     'CONSOLIDATED OVERDUE INSURANCE PORTFOLIO REPORT\n\nDistrict: {district name}\nReport Date: {report date}\nTotal Overdue Policies: {overdue count}\nTotal Exposure: ETB {total exposure}\n\nSummary and Actions:\n{remarks}\n\nThis report has been automatically dispatched to District Directors and Area Management per credit risk policy.',
     0, true),

    ('tmpl-policy-expiry-reminder', 'POLICY_EXPIRY_REMINDER', 'Email',
     'Internal Policy Expiration Reminder: {policy number}',
     'Internal Notification:\nPolicy {policy number} for collateral {collateral code} (Customer: {customer name}) requires attention. Expiry Date: {expiry date}.\nDays: {days remaining}.\nPlease follow up with borrower.',
     5, true),

    ('tmpl-customer-expiry-reminder', 'CUSTOMER_POLICY_EXPIRY_REMINDER', 'SMS',
     'Insurance Policy Expiry Reminder',
     'Dear {customer name}, your insurance policy {policy number} with {insurer name} for collateral {collateral code} expires on {expiry date}. Please renew promptly with your branch {branch} or RM {rm name} ({rm phone}).',
     15, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    type = EXCLUDED.type,
    subject = EXCLUDED.subject,
    body = EXCLUDED.body,
    trigger_days_before = EXCLUDED.trigger_days_before,
    active = true;
