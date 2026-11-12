-- Migration V41: Authentication security, password reset tokens, and system parameters
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS reset_token VARCHAR(255);
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS reset_token_expiry TIMESTAMP WITHOUT TIME ZONE;

-- Add security system parameters
INSERT INTO cims_system_parameters (param_key, param_value, description)
VALUES 
    ('account_lockout_duration_minutes', '15', 'User account lockout duration in minutes after exceeding maximum failed login attempts'),
    ('password_reset_token_validity_minutes', '15', 'Time-limited password reset token validity in minutes'),
    ('password_require_uppercase', 'true', 'Require at least one uppercase letter in password policy'),
    ('password_require_digit', 'true', 'Require at least one numerical digit in password policy')
ON CONFLICT (param_key) DO NOTHING;

