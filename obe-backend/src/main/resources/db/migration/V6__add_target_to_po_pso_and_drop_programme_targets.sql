-- ============================================================
-- V6__add_target_to_po_pso_and_drop_programme_targets.sql
-- ============================================================
-- Add direct 'target' column to programme_outcomes & programme_specific_outcomes
-- Drop separate programme_targets table to simplify schema
-- ============================================================

ALTER TABLE programme_outcomes 
    ADD COLUMN IF NOT EXISTS target NUMERIC(4,2);

ALTER TABLE programme_specific_outcomes 
    ADD COLUMN IF NOT EXISTS target NUMERIC(4,2);

DROP TABLE IF EXISTS programme_targets CASCADE;
