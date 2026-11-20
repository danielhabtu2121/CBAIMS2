-- Migration V50: Real Notification Integrations, Templates and Reminder Scheduling
-- Add policy reminder tracking columns
ALTER TABLE cims_insurance_policies ADD COLUMN IF NOT EXISTS last_reminder_date VARCHAR(50);
ALTER TABLE cims_insurance_policies ADD COLUMN IF NOT EXISTS last_reminder_type VARCHAR(100);

-- Indexes for reminder tracking
CREATE INDEX IF NOT EXISTS idx_cims_insurance_policies_reminder ON cims_insurance_policies (status, last_reminder_date);

-- Configure Office 365 SMTP and Dashen DataPower SMS in system parameters
INSERT INTO cims_system_parameters (param_key, param_value, description, param_type)
VALUES
  ('notification_outlook_enabled', 'true', 'Enable Office 365 / Microsoft Outlook Email integration', 'boolean'),
  ('notification_outlook_provider', 'OFFICE365_SMTP', 'Email provider mode: OFFICE365_SMTP, MICROSOFT_GRAPH, or SIMULATOR', 'string'),
  ('notification_outlook_sender_email', 'CBAIMS@dashenbanksc.com', 'Authoritative Bank mailbox sender address', 'string'),
  ('notification_outlook_smtp_host', 'smtp.office365.com', 'Office 365 SMTP relay host', 'string'),
  ('notification_outlook_smtp_port', '587', 'Office 365 SMTP port (587 for STARTTLS)', 'numeric'),
  ('notification_outlook_smtp_starttls', 'true', 'Require STARTTLS encryption for Office 365 SMTP', 'boolean'),
  ('notification_outlook_smtp_username', 'CBAIMS@dashenbanksc.com', 'Office 365 SMTP authentication username (or use CBAIMS_SMTP_USERNAME env var)', 'string'),
  ('notification_outlook_smtp_password', '', 'Office 365 SMTP authentication password (or use CBAIMS_SMTP_PASSWORD env var)', 'password'),
  ('notification_outlook_connection_timeout_ms', '10000', 'SMTP TCP connection timeout in milliseconds', 'numeric'),
  ('notification_outlook_read_timeout_ms', '15000', 'SMTP socket read timeout in milliseconds', 'numeric'),
  ('notification_outlook_write_timeout_ms', '15000', 'SMTP socket write timeout in milliseconds', 'numeric'),
  ('notification_sms_enabled', 'true', 'Enable Enterprise SMS integration', 'boolean'),
  ('notification_sms_provider', 'DASHEN_DATAPOWER', 'SMS provider mode: DASHEN_DATAPOWER, ETHIO_TELECOM_REST, TWILIO_REST, or SIMULATOR', 'string'),
  ('notification_sms_token_url', 'https://datapower-cp4iuat.dashenbanksc.com:9443/dashen-bank/sandbox/oauth19/oauth2/token', 'Dashen DataPower OAuth 2.0 token endpoint', 'string'),
  ('notification_sms_api_url', 'https://datapower-cp4iuat.dashenbanksc.com:9443/dashen-bank/sandbox/SMS/send', 'Dashen DataPower SMS dispatch endpoint', 'string'),
  ('notification_sms_scope', 'DASHEN', 'OAuth 2.0 Scope for Dashen DataPower API', 'string'),
  ('notification_sms_send_for', 'CBAIMS', 'Application identifier tag for Dashen DataPower SMS payload', 'string'),
  ('notification_sms_client_id', '', 'Dashen DataPower OAuth 2.0 Client ID (or use CBAIMS_SMS_CLIENT_ID env var)', 'string'),
  ('notification_sms_client_secret', '', 'Dashen DataPower OAuth 2.0 Client Secret (or use CBAIMS_SMS_CLIENT_SECRET env var)', 'password'),
  ('notification_sms_challenge_algo', 'S256', 'OAuth challenge algorithm as specified in Postman specification', 'string'),
  ('notification_sms_phone_format', '+251', 'Standard phone format: +251, 251, or 09', 'string'),
  ('notification_sms_timeout_ms', '15000', 'DataPower HTTP connection and read timeout in milliseconds', 'numeric')
ON CONFLICT (param_key) DO UPDATE
SET description = EXCLUDED.description, param_type = EXCLUDED.param_type;

-- Seed / Update Configured 6-Stage Reminder Schedules
-- 1) 30 working days before expiry
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-30w', 30, 'Both', 'Before Expiry', 30, 'Working Day', 'One-time', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM","BRMGR"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

-- 2) 25 working days before expiry
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-25w', 25, 'Both', 'Before Expiry', 25, 'Working Day', 'One-time', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

-- 3) 20 working days before expiry
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-20w', 20, 'Both', 'Before Expiry', 20, 'Working Day', 'One-time', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

-- 4) 10 calendar days before expiry
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-10c', 10, 'Both', 'Before Expiry', 10, 'Calendar Day', 'One-time', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM","BRMGR"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

-- 5) 5 calendar days after expiry
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-5c-post', 5, 'Both', 'After Expiry', 5, 'Calendar Day', 'One-time', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM","BRMGR"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

-- 6) More than 10 calendar days overdue: repeat every 15 days
INSERT INTO cims_reminder_schedules (id, days_before_expiry, audience, trigger_basis, offset_days, day_type, recurrence, continue_until, channels_json, recipient_roles_json, active)
VALUES ('rem-15c-repeat', 10, 'Both', 'After Expiry', 10, 'Calendar Day', 'Repeat Every 15 Days', 'Renewed', '["Email","SMS"]', '["CRO","BRO","SRM","BRM","BRMGR","DISTDIR"]', true)
ON CONFLICT (id) DO UPDATE SET
  days_before_expiry = EXCLUDED.days_before_expiry,
  audience = EXCLUDED.audience,
  trigger_basis = EXCLUDED.trigger_basis,
  offset_days = EXCLUDED.offset_days,
  day_type = EXCLUDED.day_type,
  recurrence = EXCLUDED.recurrence,
  continue_until = EXCLUDED.continue_until,
  channels_json = EXCLUDED.channels_json,
  recipient_roles_json = EXCLUDED.recipient_roles_json,
  active = EXCLUDED.active;

