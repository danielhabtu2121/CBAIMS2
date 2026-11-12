-- ==============================================================================
-- Migration V46: Relationship-Specific Ownership Context & Decoupled Third-Party Pledges
-- Grounded in: Collateral master ownership is preserved; third-party status is relationship-specific.
-- ==============================================================================

ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS relationship_context VARCHAR(50) DEFAULT 'Borrower-owned';
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS third_party BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS relationship_to_borrower VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS pledge_agreement_ref VARCHAR(100);
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS consent_info TEXT;
ALTER TABLE cims_loan_collateral_links ADD COLUMN IF NOT EXISTS consent_document_ref VARCHAR(100);

ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS relationship_context VARCHAR(50);
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS third_party BOOLEAN DEFAULT FALSE;
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS relationship_to_borrower VARCHAR(100);
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS pledge_agreement_ref VARCHAR(100);
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS consent_info TEXT;
ALTER TABLE cims_loan_collateral_link_history ADD COLUMN IF NOT EXISTS consent_document_ref VARCHAR(100);

-- Backfill existing links based on whether legal owner CIF matches borrower CIF
UPDATE cims_loan_collateral_links l
SET third_party = CASE 
        WHEN l.legal_owner_customer_id IS NOT NULL AND l.borrower_customer_id IS NOT NULL 
             AND LOWER(TRIM(l.legal_owner_customer_id)) <> LOWER(TRIM(l.borrower_customer_id)) THEN TRUE 
        ELSE FALSE 
    END,
    relationship_context = CASE 
        WHEN l.legal_owner_customer_id IS NOT NULL AND l.borrower_customer_id IS NOT NULL 
             AND LOWER(TRIM(l.legal_owner_customer_id)) <> LOWER(TRIM(l.borrower_customer_id)) THEN 'Third-party' 
        ELSE 'Borrower-owned' 
    END;

CREATE INDEX IF NOT EXISTS idx_links_rel_context ON cims_loan_collateral_links (relationship_context);
CREATE INDEX IF NOT EXISTS idx_links_third_party ON cims_loan_collateral_links (third_party);

