-- ====================================================================
-- V51: Notification Recipient Isolation, Portfolio Digest Templates,
-- and Granular Access Scoping
-- ====================================================================

-- 1. Enhance cims_notifications for multi-dimensional recipient isolation
ALTER TABLE cims_notifications ADD COLUMN IF NOT EXISTS recipient_user_id VARCHAR(100);
ALTER TABLE cims_notifications ADD COLUMN IF NOT EXISTS target_role VARCHAR(100);
ALTER TABLE cims_notifications ADD COLUMN IF NOT EXISTS target_branch VARCHAR(100);
ALTER TABLE cims_notifications ADD COLUMN IF NOT EXISTS target_segment VARCHAR(100);
ALTER TABLE cims_notifications ADD COLUMN IF NOT EXISTS is_bank_wide BOOLEAN DEFAULT FALSE;

-- Performance indexes for user inbox queries and isolation filters
CREATE INDEX IF NOT EXISTS idx_cims_notifications_recipient_user_id ON cims_notifications(recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_cims_notifications_target_role ON cims_notifications(target_role);
CREATE INDEX IF NOT EXISTS idx_cims_notifications_target_branch ON cims_notifications(target_branch);
CREATE INDEX IF NOT EXISTS idx_cims_notifications_target_segment ON cims_notifications(target_segment);
CREATE INDEX IF NOT EXISTS idx_cims_notifications_bank_wide ON cims_notifications(is_bank_wide);

-- 2. Seed dedicated Portfolio-level Expiry Digest Template (Separated from individual policy templates)
INSERT INTO cims_notification_templates (id, name, type, subject, body, trigger_days_before, active)
VALUES
    ('tmpl-portfolio-expiry-digest', 'PORTFOLIO_EXPIRY_DIGEST', 'Email',
     'CIMS Insurance Portfolio Alert: {Total Policies} Policies Requiring Renewal Action',
     'Dear {Staff Name},\n\nYou have {Total Policies} insurance policies in your assigned branch/portfolio requiring renewal tracking or follow-up action.\n\nSummary of Policies:\n{Policy Details}\n\nPlease review these records and coordinate with borrowers and underwriters to ensure uninterrupted coverage.\n\nBranch: {Branch} | Date: {Report Date}',
     0, true)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    subject = EXCLUDED.subject,
    body = EXCLUDED.body,
    active = EXCLUDED.active;

-- 3. Backfill existing notifications with appropriate recipient_user_id
UPDATE cims_notifications n
SET recipient_user_id = u.id
FROM cims_users u
WHERE n.recipient_user_id IS NULL
  AND (
      LOWER(n.recipient) = LOWER(u.email)
      OR LOWER(n.recipient) = LOWER(u.username)
      OR LOWER(n.recipient) = LOWER(u.username || '@cims-bank.et')
      OR LOWER(n.recipient) = LOWER(u.id)
  );

-- Backfill role-based targets for known role mailboxes
UPDATE cims_notifications
SET target_role = 'BRMGR'
WHERE target_role IS NULL AND recipient ILIKE '%brmgr%';

UPDATE cims_notifications
SET target_role = 'CRO'
WHERE target_role IS NULL AND recipient ILIKE '%cro%';

UPDATE cims_notifications
SET target_role = 'MGRCOLLDOC'
WHERE target_role IS NULL AND (recipient ILIKE '%colldoc%' OR recipient ILIKE '%mgrcolldoc%');

