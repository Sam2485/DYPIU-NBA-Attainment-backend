-- Phase 11: Create Reports Module Schema (Reports, Artifacts, Templates, Assets)

CREATE TABLE IF NOT EXISTS report_templates (
    id VARCHAR(50) PRIMARY KEY,
    template_name VARCHAR(150) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    template_version INT NOT NULL DEFAULT 1,
    is_default BOOLEAN NOT NULL DEFAULT TRUE,
    institution_id VARCHAR(50),
    header_config_json TEXT,
    body_definition_json TEXT,
    footer_config_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS report_assets (
    id VARCHAR(50) PRIMARY KEY,
    institution_id VARCHAR(50),
    asset_type VARCHAR(50) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_by VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS reports (
    id VARCHAR(50) PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    institution_id VARCHAR(50),
    master_programme_id VARCHAR(50),
    programme_batch_id VARCHAR(50),
    programme_batch_course_id VARCHAR(50),
    master_course_id VARCHAR(50),
    template_id VARCHAR(50),
    template_version INT DEFAULT 1,
    generated_by VARCHAR(150),
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    snapshot_json TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'GENERATED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS report_artifacts (
    id VARCHAR(50) PRIMARY KEY,
    report_id VARCHAR(50) NOT NULL,
    artifact_type VARCHAR(30) NOT NULL,
    file_reference VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    sha256_checksum VARCHAR(64) NOT NULL,
    hmac_signature VARCHAR(128) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_artifact_report FOREIGN KEY (report_id) REFERENCES reports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_reports_type ON reports(report_type);
CREATE INDEX IF NOT EXISTS idx_reports_batch ON reports(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_reports_offering ON reports(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_reports_prog ON reports(master_programme_id);
CREATE INDEX IF NOT EXISTS idx_report_artifacts_report ON report_artifacts(report_id);
CREATE INDEX IF NOT EXISTS idx_report_templates_type ON report_templates(report_type);
CREATE INDEX IF NOT EXISTS idx_report_assets_inst ON report_assets(institution_id);
