-- ============================================================
-- V5__add_blooms_level_and_verification_enhancements.sql
--
-- Adds blooms_level to course_outcomes
-- Adds level JSON columns to attainment_configurations
-- ============================================================

ALTER TABLE course_outcomes 
    ADD COLUMN IF NOT EXISTS blooms_level VARCHAR(50) DEFAULT 'L3 - Apply';

ALTER TABLE attainment_configurations 
    ADD COLUMN IF NOT EXISTS direct_levels_json TEXT,
    ADD COLUMN IF NOT EXISTS indirect_levels_json TEXT;
