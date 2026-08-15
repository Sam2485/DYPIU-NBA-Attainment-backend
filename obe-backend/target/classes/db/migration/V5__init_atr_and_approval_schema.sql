-- V5__init_atr_and_approval_schema.sql
-- Action Taken Reports (ATR) & Approval Workflow Schema

CREATE TABLE IF NOT EXISTS course_atrs (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    target_score NUMERIC(4,2) NOT NULL,
    actual_score NUMERIC(4,2) NOT NULL,
    pct_achieved NUMERIC(5,2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- "Target Achieved", "Target Not Achieved"
    statement TEXT,
    actions_json TEXT, -- JSON array of action items
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_atr_co UNIQUE (course_id, co_code)
);

CREATE TABLE IF NOT EXISTS programme_atrs (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) REFERENCES batches(id) ON DELETE CASCADE,
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    status VARCHAR(50) DEFAULT 'DRAFT', -- DRAFT, SUBMITTED_FOR_APPROVAL, APPROVED, NEEDS_REVISION
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    observations_json TEXT, -- JSON array of target/gap/actionPlan items
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_requests (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(50) NOT NULL, -- PO_PSO_FRAMEWORK, PROGRAMME_ATR, COURSE_CO_WEIGHTAGES, PROGRAMME_TARGETS, COURSE_ALLOCATION, COURSE_ATR
    title VARCHAR(255) NOT NULL,
    school_id VARCHAR(50) REFERENCES schools(id) ON DELETE CASCADE,
    programme_id VARCHAR(50) REFERENCES programmes(id) ON DELETE CASCADE,
    course_id VARCHAR(50) REFERENCES courses(id) ON DELETE CASCADE,
    submitted_by VARCHAR(150) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, NEEDS_REVISION
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    remarks TEXT,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS approval_history (
    id VARCHAR(50) PRIMARY KEY,
    approval_request_id VARCHAR(50) NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    actor_name VARCHAR(150) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL, -- SUBMITTED, APPROVED, REJECTED, REVISION_REQUESTED
    comments TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_course_atr_course ON course_atrs(course_id);
CREATE INDEX IF NOT EXISTS idx_programme_atr_prog ON programme_atrs(programme_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_status ON approval_requests(status);
