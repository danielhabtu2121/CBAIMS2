@echo off
set PGPASSWORD=cims_password
"C:\Program Files\PostgreSQL\17\bin\psql.exe" -h 127.0.0.1 -p 5432 -U cims_user -d cims_db_dev -c "ALTER TABLE cims_exceptions ADD COLUMN IF NOT EXISTS failed_requirement VARCHAR(255); ALTER TABLE cims_exceptions ADD COLUMN IF NOT EXISTS rule_id VARCHAR(64); ALTER TABLE cims_mandatory_document_rules ADD COLUMN IF NOT EXISTS exception_allowed BOOLEAN DEFAULT TRUE; CREATE INDEX IF NOT EXISTS idx_cims_exceptions_col_req ON cims_exceptions(collateral_id, failed_requirement);"

