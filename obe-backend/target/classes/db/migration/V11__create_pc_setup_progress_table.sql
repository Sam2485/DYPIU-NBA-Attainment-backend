-- V11__create_pc_setup_progress_table.sql
-- Migration to track setup workflow progress for Programme Coordinators

CREATE TABLE IF NOT EXISTS pc_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL UNIQUE REFERENCES programmes(id) ON DELETE CASCADE,
    coordinator_email VARCHAR(150),
    current_step INT NOT NULL DEFAULT 1,
    overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'courses,targets,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
