-- Flyway Migration V54: Bank Organization Hierarchy, Normalized Assignments, Pilot Branches, and Test Roster

-- 1. Create normalized organizational units table
CREATE TABLE IF NOT EXISTS cims_organizational_units (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    unit_type VARCHAR(32) NOT NULL, -- HEAD_OFFICE, DISTRICT, AREA_OFFICE, AREA, OPERATING_UNIT
    parent_id VARCHAR(64) REFERENCES cims_organizational_units(id),
    sort_order INT DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Alter cims_branches and cims_district_hierarchies for canonical organizational support
ALTER TABLE cims_branches ADD COLUMN IF NOT EXISTS area VARCHAR(150);
ALTER TABLE cims_branches ADD COLUMN IF NOT EXISTS canonical_district_id VARCHAR(64);
ALTER TABLE cims_branches ADD COLUMN IF NOT EXISTS cbs_branch_code VARCHAR(50);
ALTER TABLE cims_branches ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE cims_district_hierarchies ADD COLUMN IF NOT EXISTS area VARCHAR(100);
ALTER TABLE cims_district_hierarchies ADD COLUMN IF NOT EXISTS district_id VARCHAR(64);

-- 3. Normalized Branch-to-Segment Assignments
CREATE TABLE IF NOT EXISTS cims_branch_segment_assignments (
    id VARCHAR(64) PRIMARY KEY,
    branch_id VARCHAR(64) NOT NULL,
    segment_id VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_branch_segment UNIQUE (branch_id, segment_id)
);

-- 4. CBS Branch Mappings
CREATE TABLE IF NOT EXISTS cims_cbs_branch_mappings (
    id VARCHAR(64) PRIMARY KEY,
    source_branch_code VARCHAR(50) NOT NULL UNIQUE,
    cbaims_branch_id VARCHAR(64) NOT NULL,
    official_name VARCHAR(150) NOT NULL,
    aliases TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PENDING_REVIEW, INACTIVE
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. CBS Segment Mappings
CREATE TABLE IF NOT EXISTS cims_cbs_segment_mappings (
    id VARCHAR(64) PRIMARY KEY,
    source_system VARCHAR(32) NOT NULL DEFAULT 'FLEXCUBE_CBS',
    source_value VARCHAR(64) NOT NULL UNIQUE,
    canonical_segment VARCHAR(100),
    mapping_status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, PENDING_REVIEW, REJECTED
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. User Organizational Assignments (multi-branch, multi-segment)
CREATE TABLE IF NOT EXISTS cims_user_org_assignments (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES cims_users(id),
    unit_id VARCHAR(64),
    branch_id VARCHAR(64),
    segment_id VARCHAR(64),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 7. Portfolio Assignments & History
CREATE TABLE IF NOT EXISTS cims_portfolio_assignments (
    id VARCHAR(64) PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL, -- CUSTOMER, FACILITY, COLLATERAL
    entity_id VARCHAR(64) NOT NULL,
    segment_id VARCHAR(64),
    branch_id VARCHAR(64),
    district_id VARCHAR(64),
    area_id VARCHAR(64),
    maker_user_id VARCHAR(64),
    checker_user_id VARCHAR(64),
    assignment_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL', -- CBS_IMPORT, AUTOMATIC_ROUTING, MANUAL, REASSIGNED
    status VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED', -- ASSIGNED, UNASSIGNED, PENDING_REVIEW
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_portfolio_entity UNIQUE (entity_type, entity_id)
);

CREATE TABLE IF NOT EXISTS cims_portfolio_assignment_history (
    id VARCHAR(64) PRIMARY KEY,
    portfolio_assignment_id VARCHAR(64),
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    previous_maker_id VARCHAR(64),
    new_maker_id VARCHAR(64),
    previous_checker_id VARCHAR(64),
    new_checker_id VARCHAR(64),
    assignment_source VARCHAR(32),
    reason TEXT,
    changed_by VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 8. Unassigned Records Queue
CREATE TABLE IF NOT EXISTS cims_unassigned_records (
    id VARCHAR(64) PRIMARY KEY,
    entity_type VARCHAR(32) NOT NULL,
    entity_id VARCHAR(64) NOT NULL,
    source_branch_code VARCHAR(50),
    source_segment VARCHAR(50),
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNASSIGNED', -- UNASSIGNED, RESOLVED, IGNORED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Seed Head Office, Corporate Units, and 14 Canonical Districts
INSERT INTO cims_organizational_units (id, code, name, unit_type, parent_id, sort_order, is_active) VALUES
('org-ho', 'HO', 'Head Office', 'HEAD_OFFICE', NULL, 1, true),

-- Corporate Operating Units under Head Office
('org-corp-center', 'CORP-CTR', 'Corporate Center', 'OPERATING_UNIT', 'org-ho', 10, true),
('org-corp-inst', 'CORP-INST', 'Institutional Banking', 'OPERATING_UNIT', 'org-ho', 11, true),
('org-corp-trade', 'CORP-TRADE', 'Domestic and Trade Service', 'OPERATING_UNIT', 'org-ho', 12, true),
('org-corp-impexp', 'CORP-IMPEXP', 'Import & Export', 'OPERATING_UNIT', 'org-ho', 13, true),
('org-corp-agrimfg', 'CORP-AGRIMFG', 'Agriculture and Manufacturing', 'OPERATING_UNIT', 'org-ho', 14, true),

-- 14 Canonical Districts (Addis Ababa: 1-4, Upcountry: 5-14)
('org-dist-aa-west', 'DIST-AA-WEST', 'Addis Ababa – West', 'DISTRICT', 'org-ho', 20, true),
('org-dist-aa-east', 'DIST-AA-EAST', 'Addis Ababa – East', 'DISTRICT', 'org-ho', 21, true),
('org-dist-aa-south', 'DIST-AA-SOUTH', 'Addis Ababa – South', 'DISTRICT', 'org-ho', 22, true),
('org-dist-aa-north', 'DIST-AA-NORTH', 'Addis Ababa – North', 'DISTRICT', 'org-ho', 23, true),
('org-dist-adama', 'DIST-ADAMA', 'Adama', 'DISTRICT', 'org-ho', 24, true),
('org-dist-bahirdar', 'DIST-BAHIR-DAR', 'Bahirdar', 'DISTRICT', 'org-ho', 25, true),
('org-dist-hawassa', 'DIST-HAWASSA', 'Hawassa', 'DISTRICT', 'org-ho', 26, true),
('org-dist-jimma', 'DIST-JIMMA', 'Jimma', 'DISTRICT', 'org-ho', 27, true),
('org-dist-dessie', 'DIST-DESSIE', 'Dessie', 'DISTRICT', 'org-ho', 28, true),
('org-dist-wolaita', 'DIST-WOLAITA', 'Wolaita', 'DISTRICT', 'org-ho', 29, true),
('org-dist-south-west', 'DIST-SOUTH-WEST', 'South West', 'DISTRICT', 'org-ho', 30, true),
('org-dist-nekemte', 'DIST-NEKEMTE', 'Nekemte', 'DISTRICT', 'org-ho', 31, true),
('org-dist-mekelle', 'DIST-MEKELLE', 'Mekelle', 'DISTRICT', 'org-ho', 32, true),
('org-dist-diredawa', 'DIST-DIRE-DAWA', 'Diredawa', 'DISTRICT', 'org-ho', 33, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    unit_type = EXCLUDED.unit_type,
    parent_id = EXCLUDED.parent_id,
    is_active = EXCLUDED.is_active;

-- 10. Seed Area Offices and Areas for all 14 Districts
INSERT INTO cims_organizational_units (id, code, name, unit_type, parent_id, sort_order, is_active) VALUES
-- Addis Ababa – West
('org-ao-aa-west', 'AO-AA-WEST', 'West Area Office', 'AREA_OFFICE', 'org-dist-aa-west', 100, true),
('org-area-kolfe', 'AREA-KOLFE', 'Kolfe Keraniyo Area', 'AREA', 'org-ao-aa-west', 101, true),
('org-area-merkato', 'AREA-MERKATO', 'Merkato Area', 'AREA', 'org-ao-aa-west', 102, true),
('org-area-west-surr', 'AREA-WEST-SURR', 'West Addis Surrounding', 'AREA', 'org-ao-aa-west', 103, true),

-- Addis Ababa – East
('org-ao-aa-east', 'AO-AA-EAST', 'East Area Office', 'AREA_OFFICE', 'org-dist-aa-east', 110, true),
('org-area-bole', 'AREA-BOLE', 'Bole Area', 'AREA', 'org-ao-aa-east', 111, true),
('org-area-gerji', 'AREA-GERJI', 'Gerji Area', 'AREA', 'org-ao-aa-east', 112, true),
('org-area-lemi-kura', 'AREA-LEMI-KURA', 'Lemi Kura Area', 'AREA', 'org-ao-aa-east', 113, true),

-- Addis Ababa – South
('org-ao-aa-south', 'AO-AA-SOUTH', 'South Area Office', 'AREA_OFFICE', 'org-dist-aa-south', 120, true),
('org-area-akaki', 'AREA-AKAKI', 'Akaki Kality–Dukem Area', 'AREA', 'org-ao-aa-south', 121, true),
('org-area-kirkos', 'AREA-KIRKOS', 'Kirkos Area', 'AREA', 'org-ao-aa-south', 122, true),
('org-area-nifas-silk', 'AREA-NIFAS-SILK', 'Nifas Silk Area', 'AREA', 'org-ao-aa-south', 123, true),

-- Addis Ababa – North
('org-ao-aa-north', 'AO-AA-NORTH', 'North Area Office', 'AREA_OFFICE', 'org-dist-aa-north', 130, true),
('org-area-arada-yeka', 'AREA-ARADA-YEKA', 'Arada Yeka Area', 'AREA', 'org-ao-aa-north', 131, true),
('org-area-gullele', 'AREA-GULLELE', 'Gullele Area', 'AREA', 'org-ao-aa-north', 132, true),

-- Adama
('org-ao-adama', 'AO-ADAMA', 'Adama Area Office', 'AREA_OFFICE', 'org-dist-adama', 140, true),
('org-area-adama', 'AREA-ADAMA', 'Adama Area', 'AREA', 'org-ao-adama', 141, true),
('org-area-assela', 'AREA-ASSELA', 'Assela Area', 'AREA', 'org-ao-adama', 142, true),

-- Bahirdar
('org-ao-bahirdar', 'AO-BAHIR-DAR', 'Bahirdar Area Office', 'AREA_OFFICE', 'org-dist-bahirdar', 150, true),
('org-area-bahir-dar', 'AREA-BAHIR-DAR', 'Bahir Dar Area', 'AREA', 'org-ao-bahirdar', 151, true),
('org-area-debre-markos', 'AREA-DEBRE-MARKOS', 'Debre Markos Area', 'AREA', 'org-ao-bahirdar', 152, true),
('org-area-gondar', 'AREA-GONDAR', 'Gondar Area', 'AREA', 'org-ao-bahirdar', 153, true),

-- Hawassa
('org-ao-hawassa', 'AO-HAWASSA', 'Hawassa Area Office', 'AREA_OFFICE', 'org-dist-hawassa', 160, true),
('org-area-arsi-robe', 'AREA-ARSI-ROBE', 'Arsi-Robe Area', 'AREA', 'org-ao-hawassa', 161, true),
('org-area-hawassa-dilla', 'AREA-HAWASSA-DILLA', 'Hawassa-Dilla Area', 'AREA', 'org-ao-hawassa', 162, true),

-- Jimma
('org-ao-jimma', 'AO-JIMMA', 'Jimma Area Office', 'AREA_OFFICE', 'org-dist-jimma', 170, true),
('org-area-jimma', 'AREA-JIMMA', 'Jimma Area', 'AREA', 'org-ao-jimma', 171, true),

-- Dessie
('org-ao-dessie', 'AO-DESSIE', 'Dessie Area Office', 'AREA_OFFICE', 'org-dist-dessie', 180, true),
('org-area-debrebrhan', 'AREA-DEBREBRHAN', 'Debrebrhan Area', 'AREA', 'org-ao-dessie', 181, true),
('org-area-dessie', 'AREA-DESSIE', 'Dessie Area', 'AREA', 'org-ao-dessie', 182, true),

-- Wolaita
('org-ao-wolaita', 'AO-WOLAITA', 'Wolaita Area Office', 'AREA_OFFICE', 'org-dist-wolaita', 190, true),
('org-area-arbaminch', 'AREA-ARBAMINCH', 'Arbaminch Area', 'AREA', 'org-ao-wolaita', 191, true),
('org-area-hossana', 'AREA-HOSSANA', 'Hossana Area', 'AREA', 'org-ao-wolaita', 192, true),

-- South West
('org-ao-south-west', 'AO-SOUTH-WEST', 'South West Area Office', 'AREA_OFFICE', 'org-dist-south-west', 200, true),
('org-area-bonga', 'AREA-BONGA', 'Bonga Area', 'AREA', 'org-ao-south-west', 201, true),

-- Nekemte
('org-ao-nekemte', 'AO-NEKEMTE', 'Nekemte Area Office', 'AREA_OFFICE', 'org-dist-nekemte', 210, true),
('org-area-nekemte', 'AREA-NEKEMTE', 'Nekemte Area', 'AREA', 'org-ao-nekemte', 211, true),

-- Mekelle
('org-ao-mekelle', 'AO-MEKELLE', 'Mekelle Area Office', 'AREA_OFFICE', 'org-dist-mekelle', 220, true),
('org-area-mekelle', 'AREA-MEKELLE', 'Mekelle Area', 'AREA', 'org-ao-mekelle', 221, true),
('org-area-shire', 'AREA-SHIRE', 'Shire Area', 'AREA', 'org-ao-mekelle', 222, true),

-- Diredawa
('org-ao-diredawa', 'AO-DIRE-DAWA', 'Diredawa Area Office', 'AREA_OFFICE', 'org-dist-diredawa', 230, true),
('org-area-dire-dawa', 'AREA-DIRE-DAWA', 'Dire Dawa Area', 'AREA', 'org-ao-diredawa', 231, true),
('org-area-harar', 'AREA-HARAR', 'Harar Area', 'AREA', 'org-ao-diredawa', 232, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    code = EXCLUDED.code,
    unit_type = EXCLUDED.unit_type,
    parent_id = EXCLUDED.parent_id,
    is_active = EXCLUDED.is_active;

-- 11. Reconcile Legacy Hierarchy & Seed Canonical Pilot Branches
-- Ensure the 8 pilot branches exist and map to the exact canonical districts and areas:
-- 1. Bole Special Branch: Addis Ababa – East, East Area Office, Bole Area
-- 2. Merkato Branch: Addis Ababa – West, West Area Office, Merkato Area
-- 3. Arada Branch: Addis Ababa – North, North Area Office, Arada Yeka Area
-- 4. Kirkos Branch: Addis Ababa – South, South Area Office, Kirkos Area
-- 5. Adama Branch: Adama, Adama Area Office, Adama Area
-- 6. Bahir Dar Branch: Bahirdar, Bahirdar Area Office, Bahir Dar Area
-- 7. Hawassa Branch: Hawassa, Hawassa Area Office, Hawassa-Dilla Area
-- 8. Dire Dawa Main Branch: Diredawa, Diredawa Area Office, Dire Dawa Area

-- Reconcile / Alias canonical branches
INSERT INTO cims_branches (id, code, name, type, parent_district_id, canonical_district_id, area_office, area, cbs_branch_code, segment_ids, is_active) VALUES
('brn-bole', 'BR0101', 'Bole Special Branch', 'CorporateCenter', 'org-dist-aa-east', 'org-dist-aa-east', 'East Area Office', 'Bole Area', '0101', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-merkato', 'BR0102', 'Merkato Branch', 'Branch', 'org-dist-aa-west', 'org-dist-aa-west', 'West Area Office', 'Merkato Area', '0102', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-arada', 'BR0103', 'Arada Branch', 'Branch', 'org-dist-aa-north', 'org-dist-aa-north', 'North Area Office', 'Arada Yeka Area', '0103', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-kirkos', 'BR0104', 'Kirkos Branch', 'Branch', 'org-dist-aa-south', 'org-dist-aa-south', 'South Area Office', 'Kirkos Area', '0104', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-adama', 'BR0201', 'Adama Branch', 'Branch', 'org-dist-adama', 'org-dist-adama', 'Adama Area Office', 'Adama Area', '0201', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-bahirdar', 'BR0202', 'Bahir Dar Branch', 'Branch', 'org-dist-bahirdar', 'org-dist-bahirdar', 'Bahirdar Area Office', 'Bahir Dar Area', '0202', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-hawassa', 'BR0203', 'Hawassa Branch', 'Branch', 'org-dist-hawassa', 'org-dist-hawassa', 'Hawassa Area Office', 'Hawassa-Dilla Area', '0203', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true),
('brn-diredawa', 'BR0204', 'Dire Dawa Main Branch', 'Branch', 'org-dist-diredawa', 'org-dist-diredawa', 'Diredawa Area Office', 'Dire Dawa Area', '0204', '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]', true)
ON CONFLICT (id) DO UPDATE SET
    code = EXCLUDED.code,
    name = EXCLUDED.name,
    canonical_district_id = EXCLUDED.canonical_district_id,
    parent_district_id = EXCLUDED.parent_district_id,
    area_office = EXCLUDED.area_office,
    area = EXCLUDED.area,
    cbs_branch_code = EXCLUDED.cbs_branch_code,
    segment_ids = EXCLUDED.segment_ids,
    is_active = true;

-- Also harmonize duplicate seed branch rows from V36 (preserving historical IDs if referenced)
UPDATE cims_branches SET canonical_district_id = 'org-dist-aa-east', area_office = 'East Area Office', area = 'Bole Area', cbs_branch_code = '0101' WHERE id = 'brn-addis-bole';
UPDATE cims_branches SET canonical_district_id = 'org-dist-aa-west', area_office = 'West Area Office', area = 'Merkato Area', cbs_branch_code = '0102' WHERE id = 'brn-addis-merkato';
UPDATE cims_branches SET canonical_district_id = 'org-dist-aa-north', area_office = 'North Area Office', area = 'Arada Yeka Area', cbs_branch_code = '0103' WHERE id = 'brn-addis-arada';
UPDATE cims_branches SET canonical_district_id = 'org-dist-aa-south', area_office = 'South Area Office', area = 'Kirkos Area', cbs_branch_code = '0104' WHERE id = 'brn-addis-kirkos';
UPDATE cims_branches SET canonical_district_id = 'org-dist-adama', area_office = 'Adama Area Office', area = 'Adama Area', cbs_branch_code = '0201' WHERE id = 'brn-oromia-adama';
UPDATE cims_branches SET canonical_district_id = 'org-dist-bahirdar', area_office = 'Bahirdar Area Office', area = 'Bahir Dar Area', cbs_branch_code = '0202' WHERE id = 'brn-amhara-bahirdar';
UPDATE cims_branches SET canonical_district_id = 'org-dist-hawassa', area_office = 'Hawassa Area Office', area = 'Hawassa-Dilla Area', cbs_branch_code = '0203' WHERE id = 'brn-snnpr-hawassa';
UPDATE cims_branches SET canonical_district_id = 'org-dist-diredawa', area_office = 'Diredawa Area Office', area = 'Dire Dawa Area', cbs_branch_code = '0204' WHERE id = 'brn-diredawa-main';

-- Safely reconcile IFB branch and legacy IFB district without deleting FK dependencies:
-- brn-ifb-sharika belongs geographically to Addis Ababa – West, Merkato Area, with IFB segment overlay
UPDATE cims_branches SET canonical_district_id = 'org-dist-aa-west', parent_district_id = 'org-dist-aa-west', area_office = 'West Area Office', area = 'Merkato Area' WHERE id = 'brn-ifb-sharika';

-- Ensure cims_district_hierarchies has canonical entries
INSERT INTO cims_district_hierarchies (id, district_name, area_office, branch_name, area, district_id) VALUES
('dh-1', 'Addis Ababa – East', 'East Area Office', 'Bole Special Branch', 'Bole Area', 'org-dist-aa-east'),
('dh-2', 'Addis Ababa – West', 'West Area Office', 'Merkato Branch', 'Merkato Area', 'org-dist-aa-west'),
('dh-3', 'Addis Ababa – North', 'North Area Office', 'Arada Branch', 'Arada Yeka Area', 'org-dist-aa-north'),
('dh-4', 'Addis Ababa – South', 'South Area Office', 'Kirkos Branch', 'Kirkos Area', 'org-dist-aa-south'),
('dh-5', 'Adama', 'Adama Area Office', 'Adama Branch', 'Adama Area', 'org-dist-adama'),
('dh-6', 'Bahirdar', 'Bahirdar Area Office', 'Bahir Dar Branch', 'Bahir Dar Area', 'org-dist-bahirdar'),
('dh-7', 'Hawassa', 'Hawassa Area Office', 'Hawassa Branch', 'Hawassa-Dilla Area', 'org-dist-hawassa'),
('dh-8', 'Diredawa', 'Diredawa Area Office', 'Dire Dawa Main Branch', 'Dire Dawa Area', 'org-dist-diredawa')
ON CONFLICT (id) DO UPDATE SET
    district_name = EXCLUDED.district_name,
    area_office = EXCLUDED.area_office,
    branch_name = EXCLUDED.branch_name,
    area = EXCLUDED.area,
    district_id = EXCLUDED.district_id;

-- 12. Seed Normalized Branch-Segment Assignments
INSERT INTO cims_branch_segment_assignments (id, branch_id, segment_id, is_active) VALUES
-- Bole Special Branch
('bsa-bole-corp', 'brn-bole', 'Corporate Banking', true),
('bsa-bole-ret', 'brn-bole', 'Retail Banking', true),
('bsa-bole-msme', 'brn-bole', 'MSME Banking', true),
('bsa-bole-ifb', 'brn-bole', 'Interest-Free Banking (IFB)', true),
-- Merkato Branch
('bsa-merk-corp', 'brn-merkato', 'Corporate Banking', true),
('bsa-merk-ret', 'brn-merkato', 'Retail Banking', true),
('bsa-merk-msme', 'brn-merkato', 'MSME Banking', true),
('bsa-merk-ifb', 'brn-merkato', 'Interest-Free Banking (IFB)', true),
-- Arada Branch
('bsa-arada-corp', 'brn-arada', 'Corporate Banking', true),
('bsa-arada-ret', 'brn-arada', 'Retail Banking', true),
('bsa-arada-msme', 'brn-arada', 'MSME Banking', true),
('bsa-arada-ifb', 'brn-arada', 'Interest-Free Banking (IFB)', true),
-- Kirkos Branch
('bsa-kirk-corp', 'brn-kirkos', 'Corporate Banking', true),
('bsa-kirk-ret', 'brn-kirkos', 'Retail Banking', true),
('bsa-kirk-msme', 'brn-kirkos', 'MSME Banking', true),
('bsa-kirk-ifb', 'brn-kirkos', 'Interest-Free Banking (IFB)', true),
-- Adama Branch
('bsa-adama-corp', 'brn-adama', 'Corporate Banking', true),
('bsa-adama-ret', 'brn-adama', 'Retail Banking', true),
('bsa-adama-msme', 'brn-adama', 'MSME Banking', true),
('bsa-adama-ifb', 'brn-adama', 'Interest-Free Banking (IFB)', true),
-- Bahir Dar Branch
('bsa-bd-corp', 'brn-bahirdar', 'Corporate Banking', true),
('bsa-bd-ret', 'brn-bahirdar', 'Retail Banking', true),
('bsa-bd-msme', 'brn-bahirdar', 'MSME Banking', true),
('bsa-bd-ifb', 'brn-bahirdar', 'Interest-Free Banking (IFB)', true),
-- Hawassa Branch
('bsa-haw-corp', 'brn-hawassa', 'Corporate Banking', true),
('bsa-haw-ret', 'brn-hawassa', 'Retail Banking', true),
('bsa-haw-msme', 'brn-hawassa', 'MSME Banking', true),
('bsa-haw-ifb', 'brn-hawassa', 'Interest-Free Banking (IFB)', true),
-- Dire Dawa Main Branch
('bsa-dd-corp', 'brn-diredawa', 'Corporate Banking', true),
('bsa-dd-ret', 'brn-diredawa', 'Retail Banking', true),
('bsa-dd-msme', 'brn-diredawa', 'MSME Banking', true),
('bsa-dd-ifb', 'brn-diredawa', 'Interest-Free Banking (IFB)', true)
ON CONFLICT (id) DO NOTHING;

-- 13. Seed CBS Branch Mappings
INSERT INTO cims_cbs_branch_mappings (id, source_branch_code, cbaims_branch_id, official_name, aliases, status, is_active) VALUES
('cbm-0101', '0101', 'brn-bole', 'Bole Special Branch', 'BR001,BRN-001,BOLE', 'ACTIVE', true),
('cbm-0102', '0102', 'brn-merkato', 'Merkato Branch', 'BR002,BRN-002,MERKATO', 'ACTIVE', true),
('cbm-0103', '0103', 'brn-arada', 'Arada Branch', 'BR003,BRN-003,BRN-006,ARADA', 'ACTIVE', true),
('cbm-0104', '0104', 'brn-kirkos', 'Kirkos Branch', 'BR004,BRN-004,KIRKOS', 'ACTIVE', true),
('cbm-0201', '0201', 'brn-adama', 'Adama Branch', 'BR005,BRN-005,ADAMA', 'ACTIVE', true),
('cbm-0202', '0202', 'brn-bahirdar', 'Bahir Dar Branch', 'BR006,BRN-006,BAHIR_DAR', 'ACTIVE', true),
('cbm-0203', '0203', 'brn-hawassa', 'Hawassa Branch', 'BR007,BRN-007,HAWASSA', 'ACTIVE', true),
('cbm-0204', '0204', 'brn-diredawa', 'Dire Dawa Main Branch', 'BR008,BRN-008,DIRE_DAWA', 'ACTIVE', true)
ON CONFLICT (id) DO UPDATE SET
    cbaims_branch_id = EXCLUDED.cbaims_branch_id,
    official_name = EXCLUDED.official_name,
    aliases = EXCLUDED.aliases,
    status = EXCLUDED.status,
    is_active = EXCLUDED.is_active;

-- 14. Seed CBS Segment Mappings
INSERT INTO cims_cbs_segment_mappings (id, source_system, source_value, canonical_segment, mapping_status, is_active, reason) VALUES
('csm-corp', 'FLEXCUBE_CBS', 'CORPORATE', 'Corporate Banking', 'ACTIVE', true, 'Canonical Corporate Banking mapping'),
('csm-msme', 'FLEXCUBE_CBS', 'MSME', 'MSME Banking', 'ACTIVE', true, 'Canonical MSME Banking mapping'),
('csm-ret', 'FLEXCUBE_CBS', 'RETAIL', 'Retail Banking', 'ACTIVE', true, 'Canonical Retail Banking mapping'),
('csm-ifb', 'FLEXCUBE_CBS', 'IFB', 'Interest-Free Banking (IFB)', 'ACTIVE', true, 'Canonical IFB Banking mapping'),
-- MRETAIL is explicitly held for review per specifications
('csm-mretail', 'FLEXCUBE_CBS', 'MRETAIL', NULL, 'PENDING_REVIEW', true, 'Unconfirmed source value requiring official business clarification')
ON CONFLICT (id) DO UPDATE SET
    canonical_segment = EXCLUDED.canonical_segment,
    mapping_status = EXCLUDED.mapping_status,
    reason = EXCLUDED.reason;

-- 15. Seed Test Roster Users
INSERT INTO cims_users (id, username, password, full_name, email, phone, role, branch, segment, district, active, segment_ids) VALUES
-- Corporate Maker & Checker
('usr-test-corp-ro-bole', 'TEST_CORP_RO_BOLE', 'password123', 'Abebe Tadesse (Test Corporate RO)', 'test_corp_ro@bank.com', '+251911100001', 'CRO', 'brn-bole', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking"]'),
('usr-test-corp-rm-bole', 'TEST_CORP_RM_BOLE', 'password123', 'Chaltu Gemechu (Test Corporate RM)', 'test_corp_rm@bank.com', '+251911100002', 'SRM', 'brn-bole', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking"]'),

-- Retail Maker & Checker
('usr-test-ret-ro-merk', 'TEST_RETAIL_RO_MERKATO', 'password123', 'Dawit Haile (Test Retail RO)', 'test_ret_ro@bank.com', '+251911100003', 'BRO', 'brn-merkato', 'Retail Banking', 'org-dist-aa-west', true, '["Retail Banking"]'),
('usr-test-ret-rm-merk', 'TEST_RETAIL_RM_MERKATO', 'password123', 'Hiwot Girma (Test Retail BM)', 'test_ret_rm@bank.com', '+251911100004', 'BRMGR', 'brn-merkato', 'Retail Banking', 'org-dist-aa-west', true, '["Retail Banking"]'),

-- MSME Maker & Checker
('usr-test-msme-ro-adama', 'TEST_MSME_RO_ADAMA', 'password123', 'Kassahun Bekele (Test MSME RO)', 'test_msme_ro@bank.com', '+251911100005', 'BRO', 'brn-adama', 'MSME Banking', 'org-dist-adama', true, '["MSME Banking"]'),
('usr-test-msme-rm-adama', 'TEST_MSME_RM_ADAMA', 'password123', 'Marta Zewdu (Test MSME BM)', 'test_msme_rm@bank.com', '+251911100006', 'BRMGR', 'brn-adama', 'MSME Banking', 'org-dist-adama', true, '["MSME Banking"]'),

-- IFB Maker & Checker
('usr-test-ifb-ro-merk', 'TEST_IFB_RO_MERKATO', 'password123', 'Mohammed Nur (Test IFB RO)', 'test_ifb_ro@bank.com', '+251911100007', 'BRO', 'brn-merkato', 'Interest-Free Banking (IFB)', 'org-dist-aa-west', true, '["Interest-Free Banking (IFB)"]'),
('usr-test-ifb-checker', 'TEST_IFB_CHECKER', 'password123', 'Fatima Ahmed (Test IFB Checker)', 'test_ifb_chk@bank.com', '+251911100008', 'BRMGR', 'brn-merkato', 'Interest-Free Banking (IFB)', 'org-dist-aa-west', true, '["Interest-Free Banking (IFB)"]'),

-- District Oversight & Area Oversight
('usr-test-dist-west', 'TEST_DISTRICT_WEST', 'password123', 'Tewodros Assefa (Test West District Dir)', 'test_dist_west@bank.com', '+251911100009', 'DISTDIR', 'brn-merkato', 'Corporate Banking', 'org-dist-aa-west', true, '["Corporate Banking","Retail Banking","MSME Banking"]'),
('usr-test-ifb-area-west', 'TEST_IFB_AREA_WEST', 'password123', 'Mustafa Kemal (Test IFB Area Mgr)', 'test_ifb_west@bank.com', '+251911100010', 'DISTDIR', 'brn-merkato', 'Interest-Free Banking (IFB)', 'org-dist-aa-west', true, '["Interest-Free Banking (IFB)"]'),

-- Head Office Oversight
('usr-test-ho-corp', 'TEST_HO_CORPORATE', 'password123', 'Rahel Tesfaye (Test HO Corporate Head)', 'test_ho_corp@bank.com', '+251911100011', 'HODEPT', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking"]'),

-- Read-Only & Governance Roles
('usr-test-risk-view', 'TEST_RISK_VIEW', 'password123', 'Samson Wolde (Test Risk Officer)', 'test_risk@bank.com', '+251911100012', 'RISK', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]'),
('usr-test-comp-view', 'TEST_COMPLIANCE_VIEW', 'password123', 'Senait Alemu (Test Compliance Officer)', 'test_compliance@bank.com', '+251911100013', 'COMPLIANCE', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]'),
('usr-test-aud-view', 'TEST_AUDIT_VIEW', 'password123', 'Yared Desta (Test Internal Auditor)', 'test_audit@bank.com', '+251911100014', 'AUDITOR', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]'),
('usr-test-sysadmin', 'TEST_SYSADMIN', 'password123', 'Admin Tester (Test Sysadmin)', 'test_sysadmin@bank.com', '+251911100015', 'SYSADMIN', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking","Retail Banking","MSME Banking","Interest-Free Banking (IFB)"]'),

-- Multi-Branch, Unassigned, and Deactivated test accounts
('usr-test-multi-branch', 'TEST_OFFICER_MULTI_BRANCH', 'password123', 'Tariku Lemma (Multi-Branch Officer)', 'test_multibrn@bank.com', '+251911100016', 'CRO', 'brn-bole', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking"]'),
('usr-test-unassigned', 'TEST_OFFICER_UNASSIGNED', 'password123', 'Zelalem Mengistu (Unassigned Officer)', 'test_unassigned@bank.com', '+251911100017', 'CRO', 'brn-ho', 'Corporate Banking', 'org-dist-aa-east', true, '["Corporate Banking"]'),
('usr-test-deactivated', 'TEST_OFFICER_DEACTIVATED', 'password123', 'Birtukan Mamo (Deactivated Officer)', 'test_deact@bank.com', '+251911100018', 'CRO', 'brn-bole', 'Corporate Banking', 'org-dist-aa-east', false, '["Corporate Banking"]')
ON CONFLICT (id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    role = EXCLUDED.role,
    branch = EXCLUDED.branch,
    segment = EXCLUDED.segment,
    district = EXCLUDED.district,
    active = EXCLUDED.active,
    segment_ids = EXCLUDED.segment_ids;

-- 16. Seed User Organizational Assignments
INSERT INTO cims_user_org_assignments (id, user_id, unit_id, branch_id, segment_id, is_primary, is_active) VALUES
('uoa-1', 'usr-test-corp-ro-bole', 'org-dist-aa-east', 'brn-bole', 'Corporate Banking', true, true),
('uoa-2', 'usr-test-corp-rm-bole', 'org-dist-aa-east', 'brn-bole', 'Corporate Banking', true, true),
('uoa-3', 'usr-test-ret-ro-merk', 'org-dist-aa-west', 'brn-merkato', 'Retail Banking', true, true),
('uoa-4', 'usr-test-ret-rm-merk', 'org-dist-aa-west', 'brn-merkato', 'Retail Banking', true, true),
('uoa-5', 'usr-test-msme-ro-adama', 'org-dist-adama', 'brn-adama', 'MSME Banking', true, true),
('uoa-6', 'usr-test-msme-rm-adama', 'org-dist-adama', 'brn-adama', 'MSME Banking', true, true),
('uoa-7', 'usr-test-ifb-ro-merk', 'org-dist-aa-west', 'brn-merkato', 'Interest-Free Banking (IFB)', true, true),
('uoa-8', 'usr-test-ifb-checker', 'org-dist-aa-west', 'brn-merkato', 'Interest-Free Banking (IFB)', true, true),
-- Multi-branch assignment for TEST_OFFICER_MULTI_BRANCH: Bole (primary) + Merkato (secondary)
('uoa-9', 'usr-test-multi-branch', 'org-dist-aa-east', 'brn-bole', 'Corporate Banking', true, true),
('uoa-10', 'usr-test-multi-branch', 'org-dist-aa-west', 'brn-merkato', 'Corporate Banking', false, true)
ON CONFLICT (id) DO NOTHING;
