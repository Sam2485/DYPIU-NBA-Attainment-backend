-- V12__create_cc_setup_progress_table.sql
-- Migration to track setup workflow progress for Course Coordinators

CREATE TABLE IF NOT EXISTS cc_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL UNIQUE REFERENCES courses(id) ON DELETE CASCADE,
    coordinator_email VARCHAR(150),
    current_step INT NOT NULL DEFAULT 1,
    overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'cos,co_targets,co_mapping,direct,indirect,attainment,course_atr',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
