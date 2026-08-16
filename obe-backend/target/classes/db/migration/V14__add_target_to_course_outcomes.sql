-- V14__add_target_to_course_outcomes.sql
-- Add target_level column to course_outcomes table for storing CO target benchmarks (1.00 - 3.00 scale)

ALTER TABLE course_outcomes ADD COLUMN IF NOT EXISTS target_level NUMERIC(4,2) DEFAULT 2.50;
