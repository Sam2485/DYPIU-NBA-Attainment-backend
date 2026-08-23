-- Phase 10: Create Attainment Reports Persistence Schema

CREATE TABLE IF NOT EXISTS course_attainment_reports (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    overall_co_attainment NUMERIC(5,2),
    direct_attainment NUMERIC(5,2),
    indirect_attainment NUMERIC(5,2),
    table1_mapping_json TEXT,
    table2_direct_json TEXT,
    table3_co_attainment_json TEXT,
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(150),
    verified_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_attainment_report UNIQUE (programme_batch_course_id),
    CONSTRAINT fk_course_attainment_report_offering FOREIGN KEY (programme_batch_course_id) REFERENCES programme_batch_courses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS programme_batch_attainment_reports (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    overall_programme_attainment NUMERIC(5,2),
    average_mapping_report_json TEXT,
    direct_attainment_report_json TEXT,
    indirect_attainment_report_json TEXT,
    overall_attainment_report_json TEXT,
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_programme_batch_attainment_report UNIQUE (programme_batch_id),
    CONSTRAINT fk_programme_batch_attainment_report_batch FOREIGN KEY (programme_batch_id) REFERENCES programme_batches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_course_attainment_report_offering ON course_attainment_reports(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_programme_batch_attainment_report_batch ON programme_batch_attainment_reports(programme_batch_id);
