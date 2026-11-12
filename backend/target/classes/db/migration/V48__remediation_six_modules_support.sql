-- V48: Database schema enhancements for Six Modules Remediation (RPT, DEG, ESC, REL, AUTH, CFG)

-- 1. Scheduled Reports execution and artifact tracking columns (RPT-06)
ALTER TABLE cims_scheduled_reports ADD COLUMN IF NOT EXISTS last_execution_status VARCHAR(50);
ALTER TABLE cims_scheduled_reports ADD COLUMN IF NOT EXISTS last_delivery_status VARCHAR(50);
ALTER TABLE cims_scheduled_reports ADD COLUMN IF NOT EXISTS last_artifact_path TEXT;
ALTER TABLE cims_scheduled_reports ADD COLUMN IF NOT EXISTS last_artifact_ref VARCHAR(100);
ALTER TABLE cims_scheduled_reports ADD COLUMN IF NOT EXISTS last_error_message TEXT;

-- 2. Decouple Collateral Release Margin from Insurance Adequacy (REL-03 / CFG-02)
INSERT INTO cims_system_parameters (param_key, param_value, description, param_type)
VALUES (
    'COLLATERAL_RELEASE_MARGIN_PCT',
    '100.0',
    'Collateral release required remaining security margin percentage (Baseline subject to credit policy clarification)',
    'number'
)
ON CONFLICT (param_key) DO UPDATE
SET description = 'Collateral release required remaining security margin percentage (Baseline subject to credit policy clarification)';

-- 3. Remove unsupported bank-placed coverage instruction from notification templates (ESC-08)
UPDATE cims_notification_templates
SET body = REPLACE(body, 'and avoid bank-placed coverage at your expense.', 'and ensure policy continuity.')
WHERE body LIKE '%bank-placed coverage%';

UPDATE cims_notification_templates
SET body = REPLACE(body, 'and initiate bank-placed coverage if renewal is not filed within 5 business days.', 'and secure prompt renewal submission.')
WHERE body LIKE '%bank-placed coverage%';
