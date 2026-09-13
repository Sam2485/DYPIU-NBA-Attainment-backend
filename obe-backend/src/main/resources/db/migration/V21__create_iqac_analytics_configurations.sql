-- ============================================================
-- V21__create_iqac_analytics_configurations.sql
-- Add IQAC analytics configuration table for configurable thresholds
-- ============================================================

CREATE TABLE IF NOT EXISTS iqac_analytics_configurations (
    id VARCHAR(50) PRIMARY KEY,
    student_evidence_threshold NUMERIC(5, 2) NOT NULL DEFAULT 50.00,
    updated_by VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO iqac_analytics_configurations (id, student_evidence_threshold, updated_by)
VALUES ('GLOBAL', 50.00, 'SYSTEM')
ON CONFLICT (id) DO NOTHING;
