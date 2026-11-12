@echo off
set PGPASSWORD=cims_password
"C:\Program Files\PostgreSQL\17\bin\psql.exe" -h 127.0.0.1 -p 5432 -U cims_user -d cims_db_dev -c "ALTER TABLE cims_collateral_owners ALTER COLUMN collateral_id DROP NOT NULL;"

