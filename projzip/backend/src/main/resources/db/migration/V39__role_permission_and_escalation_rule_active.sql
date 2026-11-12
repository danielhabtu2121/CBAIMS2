-- =========================================================================
-- V39: Add active flags to role permissions, escalation rules, and district to users
-- Supports Audit Findings 71, 74, and 70
-- =========================================================================

ALTER TABLE cims_role_permissions ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE cims_escalation_rules ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS district VARCHAR(100);
ALTER TABLE cims_ownership_documents ADD COLUMN IF NOT EXISTS verified_by VARCHAR(64);
ALTER TABLE cims_ownership_documents ADD COLUMN IF NOT EXISTS verified_at VARCHAR(50);

