-- ==============================================================================
-- Migration V42: Comprehensive Collateral Registration Module Enhancements
-- Grounded in: CIMS Master Implementation Specification (Parts 1-18)
-- Extends: Taxonomies, Document Rules, Dynamic Collateral Attributes
-- ==============================================================================

-- 1. Extend cims_collaterals with asset-specific subtype attributes
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_make VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_model VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_year INT;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_manufacturer VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_model VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_serial_no VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_purchase_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_ownership_cert_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS machinery_purchase_doc_ref VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS furniture_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS furniture_quantity INT;

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS share_issuer VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS share_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS share_quantity BIGINT;
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS share_face_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS share_cert_ref VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_drawer VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_drawee VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_issue_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_maturity_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_face_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_reference VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS bill_payment_place VARCHAR(255);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS blocked_account_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS deposit_amount NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS deposit_currency VARCHAR(10) DEFAULT 'ETB';
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS deposit_maturity_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS pledge_confirmation_ref VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS treasury_bill_series VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS treasury_bill_face_value NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS treasury_bill_issue_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS treasury_bill_maturity_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS custody_account_ref VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS farm_quantity NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS farm_storage_location VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS farm_harvest_season VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_quantity NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_receipt_date VARCHAR(50);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS warehouse_inspection_ref VARCHAR(100);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_authority VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS ip_registration_date VARCHAR(50);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_cif VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_id_number VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_tin VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_contact VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantor_address VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantee_amount NUMERIC(18,2);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS guarantee_effective_date VARCHAR(50);

ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS building_type VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS building_use VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS location_zone VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS coffee_site_location VARCHAR(255);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS coffee_site_cert_ref VARCHAR(100);
ALTER TABLE cims_collaterals ADD COLUMN IF NOT EXISTS land_use_right_cert_ref VARCHAR(100);

-- 2. Seed Canonical 6 Collateral Categories and their exact Subtypes (Part 3)
INSERT INTO cims_collateral_taxonomies (id, category, sub_category, insurance_mandatory, mandatory_coverage_type, active)
VALUES
  -- Category 1: IMMOVABLE PROPERTIES
  ('tax-bldg-comm', 'Immovable Properties', 'Buildings - Commercial', true, 'Fire & Lightning', true),
  ('tax-bldg-res', 'Immovable Properties', 'Buildings - Residential', true, 'Fire & Lightning', true),
  ('tax-bldg-mix', 'Immovable Properties', 'Buildings - Mixed-use', true, 'Fire & Lightning', true),
  ('tax-coffee', 'Immovable Properties', 'Coffee Sites', true, 'Fire, Theft & Combustion', true),
  ('tax-land', 'Immovable Properties', 'Land Use Rights', false, 'None', true),

  -- Category 2: MOVABLE PROPERTIES
  ('tax-veh', 'Movable Properties', 'Vehicles', true, 'Comprehensive Motor', true),
  ('tax-const-mach', 'Movable Properties', 'Construction Machineries', true, 'Contractor All-Risk / Machinery Breakdown', true),

  -- Category 3: BUSINESS MORTGAGES
  ('tax-ind-mach', 'Business Mortgages', 'Industrial Machinery', true, 'Machinery Breakdown & Fire', true),
  ('tax-off-mach', 'Business Mortgages', 'Office Machinery', true, 'Fire & Burglary', true),
  ('tax-furn', 'Business Mortgages', 'Furniture', true, 'Fire & Burglary', true),

  -- Category 4: FINANCIAL ASSETS
  ('tax-shares-bank', 'Financial Assets', 'Marketable Shares - Banks', false, 'None', true),
  ('tax-shares-ins', 'Financial Assets', 'Marketable Shares - Insurance Companies', false, 'None', true),
  ('tax-bills', 'Financial Assets', 'Negotiable Bills of Payments', false, 'None', true),
  ('tax-cash-blocked', 'Financial Assets', 'Cash and Cash Substitutes - Blocked Accounts', false, 'None', true),
  ('tax-cash-tbills', 'Financial Assets', 'Cash and Cash Substitutes - Treasury Bills', false, 'None', true),

  -- Category 5: AGRICULTURAL/OTHER
  ('tax-live', 'Agricultural/Other', 'Livestock', true, 'Livestock Mortality', true),
  ('tax-farm', 'Agricultural/Other', 'Farm Products', true, 'Fire & Crop Protection', true),
  ('tax-whr', 'Agricultural/Other', 'Warehouse Receipts', true, 'Warehouse All-Risk', true),
  ('tax-ip', 'Agricultural/Other', 'Intellectual Property Rights', false, 'None', true),

  -- Category 6: GUARANTEES
  ('tax-guar-pers', 'Guarantees', 'Personal Guarantee', false, 'None', true)
ON CONFLICT (id) DO UPDATE SET
  category = EXCLUDED.category,
  sub_category = EXCLUDED.sub_category,
  insurance_mandatory = EXCLUDED.insurance_mandatory,
  mandatory_coverage_type = EXCLUDED.mandatory_coverage_type,
  active = EXCLUDED.active;

-- Also update any existing legacy Agricultural / Other without space
UPDATE cims_collateral_taxonomies SET category = 'Agricultural/Other' WHERE category = 'Agricultural / Other';

-- 3. Seed Mandatory Document Rules Matrix (Part 8.1 & Part 8.2)
INSERT INTO cims_mandatory_document_rules (
    id, collateral_category, collateral_type, document_type, ownership_condition, applicability_condition, mandatory, active
) VALUES
    -- Buildings / Real Estate
    ('doc-r-title-deed', 'Immovable Properties', 'All', 'Title Deed / Property Ownership Certificate', 'All', 'Authoritative cadastral title certificate or deed', true, true),
    ('doc-r-building-plan', 'Immovable Properties', 'All', 'Approved Building Plan', 'All', 'Municipality approved architectural design and building plan', true, true),
    ('doc-r-boq-under-con', 'Immovable Properties', 'Building - Under Construction', 'Bill of Quantity', 'All', 'Certified engineering Bill of Quantities (BOQ) with remaining works schedule', true, true),
    ('doc-r-lease-agreement', 'Immovable Properties', 'Leased Property', 'Lease Agreement', 'All', 'Government lease contract / agreement', true, true),
    ('doc-r-lease-receipt', 'Immovable Properties', 'Leased Property', 'Lease Payment Receipt', 'All', 'Official lease fee payment clearance receipt', true, true),

    -- Movable Properties
    ('doc-r-vehicle-liber', 'Movable Properties', 'Vehicles', 'Vehicle Registration Certificate', 'All', 'Official Ministry of Transport vehicle ownership booklet / libre', true, true),
    ('doc-r-const-mach-cert', 'Movable Properties', 'Construction Machineries', 'Machinery Ownership Certificate', 'All', 'Official machinery ownership certificate / title', true, true),
    ('doc-r-const-mach-pur', 'Movable Properties', 'Construction Machineries', 'Purchase Document', 'All', 'Commercial purchase invoice or customs clearance declaration', true, true),

    -- Business Mortgages
    ('doc-r-ind-mach-cert', 'Business Mortgages', 'Industrial Machinery', 'Machinery Ownership Certificate', 'All', 'Machinery registration / ownership certificate', true, true),
    ('doc-r-ind-mach-pur', 'Business Mortgages', 'Industrial Machinery', 'Purchase Document', 'All', 'Purchase invoice / import declaration document', true, true),
    ('doc-r-off-mach-cert', 'Business Mortgages', 'Office Machinery', 'Machinery Ownership Certificate', 'All', 'Office equipment ownership certificate / invoice receipt', true, true),
    ('doc-r-off-mach-pur', 'Business Mortgages', 'Office Machinery', 'Purchase Document', 'All', 'Purchase document / commercial receipt', true, true),

    -- Third-Party Ownership
    ('doc-r-tp-pledge', 'All', 'All', 'Third-Party Consent and Pledge Agreement', 'Third-party-owned', 'Signed tripartite collateral pledge agreement', true, true),
    ('doc-r-tp-consent', 'All', 'All', 'Letter of Consent', 'Third-party-owned', 'Formal notarized letter of consent from legal asset owner', true, true),

    -- Agricultural / Other
    ('doc-r-livestock-tag', 'Agricultural/Other', 'Livestock', 'Registered Plastic Ear Tag evidence', 'All', 'Official veterinary ear-tag registration photo and record', true, true),
    ('doc-r-livestock-tin', 'Agricultural/Other', 'Livestock', 'TIN information/evidence', 'All', 'Owner taxpayer identification number certificate / evidence', true, true),
    ('doc-r-farm-land-cert', 'Agricultural/Other', 'Farm Products', 'Land Holding Certificate', 'All', 'Rural / urban land holding certificate for farm land', true, true),
    ('doc-r-farm-tin', 'Agricultural/Other', 'Farm Products', 'TIN information/evidence', 'All', 'Owner TIN evidence for agricultural producer', true, true),
    ('doc-r-whr-receipt', 'Agricultural/Other', 'Warehouse Receipts', 'Warehouse Receipt', 'All', 'Certified transferable warehouse commodity receipt', true, true),
    ('doc-r-ip-cert', 'Agricultural/Other', 'Intellectual Property Rights', 'Intellectual Property Rights Registration Certificate', 'All', 'Ethiopian Intellectual Property Authority registration certificate', true, true),

    -- Security Rank (External 1st Degree)
    ('doc-r-ext-confirmation', 'All', 'All', 'Confirmation letter from other financial institution', 'All', 'Formal bank clearance letter stating outstanding balance and charge', false, true),
    ('doc-r-ext-extract', 'All', 'All', 'Registration extract', 'All', 'Cadastral or registry official charge extract', false, true),

    -- Supporting Documents
    ('doc-r-valuation-rep', 'All', 'All', 'Valuation Report', 'All', 'Comprehensive appraisal report by bank approved valuation engineer', true, true),
    ('doc-r-mortgage-conf', 'All', 'All', 'Mortgage Registration Confirmation Letter', 'All', 'Registry stamped mortgage confirmation letter', false, true),
    ('doc-r-custody-agree', 'All', 'All', 'Collateral Custody Agreement', 'All', 'Signed tripartite collateral custody and warehousing agreement', false, true)
ON CONFLICT (id) DO UPDATE SET
    collateral_category = EXCLUDED.collateral_category,
    collateral_type = EXCLUDED.collateral_type,
    document_type = EXCLUDED.document_type,
    ownership_condition = EXCLUDED.ownership_condition,
    applicability_condition = EXCLUDED.applicability_condition,
    mandatory = EXCLUDED.mandatory,
    active = EXCLUDED.active;

