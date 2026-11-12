-- V47: Widen collateral_pct column in cims_loan_accounts and cbs_sim.facilities to allow high coverage percentages without numeric overflow
ALTER TABLE cims_loan_accounts ALTER COLUMN collateral_pct TYPE NUMERIC(12, 2);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables 
        WHERE table_schema = 'cbs_sim' AND table_name = 'facilities'
    ) THEN
        ALTER TABLE cbs_sim.facilities ALTER COLUMN collateral_pct TYPE NUMERIC(12, 2);
    END IF;
END $$;

