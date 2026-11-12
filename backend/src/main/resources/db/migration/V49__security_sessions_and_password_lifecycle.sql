-- =========================================================================
-- V49: Security Hardening: Server Sessions, Password Lifecycle & History
-- =========================================================================

-- 1. Persistent Server-Enforced User Sessions (SEC-02, SEC-03)
CREATE TABLE IF NOT EXISTS cims_user_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    role VARCHAR(64) NOT NULL,
    client_ip VARCHAR(64),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idle_timeout_minutes INT NOT NULL DEFAULT 15,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revocation_reason VARCHAR(255),
    logged_out_at TIMESTAMP,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES cims_users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON cims_user_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_sessions_revoked ON cims_user_sessions(revoked);
CREATE INDEX IF NOT EXISTS idx_sessions_last_activity ON cims_user_sessions(last_activity_at);

-- 2. User Password History for Reuse Prevention (SEC-06)
CREATE TABLE IF NOT EXISTS cims_user_password_history (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pwd_hist_user FOREIGN KEY (user_id) REFERENCES cims_users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pwd_hist_user ON cims_user_password_history(user_id, created_at DESC);

-- 3. Password Lifecycle Fields on cims_users (SEC-05, SEC-08)
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP;
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS password_expiry_date TIMESTAMP;
ALTER TABLE cims_users ADD COLUMN IF NOT EXISTS password_expired BOOLEAN DEFAULT FALSE;

-- Initialize password_changed_at for existing users to current timestamp
UPDATE cims_users SET password_changed_at = CURRENT_TIMESTAMP WHERE password_changed_at IS NULL;

-- 4. Seed Security System Parameters (SEC-05, SEC-06, SEC-16)
INSERT INTO cims_system_parameters (param_key, param_value, description, param_type)
VALUES
    ('password_expiry_days', '90', 'Password validity period in days before mandatory change (Awaiting Bank Policy confirmation)', 'INTEGER'),
    ('password_history_count', '5', 'Number of historical passwords remembered to prevent reuse (Awaiting Bank Policy confirmation)', 'INTEGER'),
    ('trusted_proxies', '127.0.0.1,::1,localhost', 'Comma-separated IP addresses of trusted reverse proxies for X-Forwarded-For validation', 'STRING')
ON CONFLICT (param_key) DO NOTHING;

