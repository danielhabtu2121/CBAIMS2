-- V43: Allow null collateral_id in cims_collateral_owners to support Hibernate orphan-removal & collection merge lifecycle
ALTER TABLE cims_collateral_owners ALTER COLUMN collateral_id DROP NOT NULL;

