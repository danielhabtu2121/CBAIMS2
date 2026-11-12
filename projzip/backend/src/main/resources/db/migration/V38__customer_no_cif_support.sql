-- =========================================================================
-- V38: Support legitimate customer records without a CIF (Audit Finding 95)
-- Enables manual registration of third-party collateral pledgors / guarantors
-- who do not yet hold a CBS Core Banking CIF, without fabricating fake CIFs.
-- =========================================================================

-- 1. Make cif column nullable on cims_customers
ALTER TABLE cims_customers ALTER COLUMN cif DROP NOT NULL;

-- 2. Add identification_type column
ALTER TABLE cims_customers ADD COLUMN IF NOT EXISTS identification_type VARCHAR(50) DEFAULT 'CBS_CIF';

-- 3. Populate identification_type for existing records
UPDATE cims_customers 
SET identification_type = 'CBS_CIF' 
WHERE cif IS NOT NULL AND (identification_type IS NULL OR identification_type = '');

UPDATE cims_customers 
SET identification_type = 'UNREGISTERED_THIRD_PARTY' 
WHERE cif IS NULL;

-- 4. Create unique partial index on CIF so non-null CIFs remain strictly unique
CREATE UNIQUE INDEX IF NOT EXISTS idx_cims_customers_cif_unique ON cims_customers (cif) WHERE cif IS NOT NULL;
