-- V44: Add failed_requirement, rule_id, and exception_allowed columns to support requirement-level collateral exceptions
ALTER TABLE cims_exceptions ADD COLUMN IF NOT EXISTS failed_requirement VARCHAR(255);
ALTER TABLE cims_exceptions ADD COLUMN IF NOT EXISTS rule_id VARCHAR(64);
ALTER TABLE cims_mandatory_document_rules ADD COLUMN IF NOT EXISTS exception_allowed BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_cims_exceptions_col_req ON cims_exceptions(collateral_id, failed_requirement);

