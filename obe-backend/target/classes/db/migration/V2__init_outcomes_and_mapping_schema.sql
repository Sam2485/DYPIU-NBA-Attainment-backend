-- V2__init_outcomes_and_mapping_schema.sql
-- Outcome Management (PO, PSO, PEO, CO) & CO-PO/PSO Mapping Schema

CREATE TABLE programme_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL, -- e.g. "PO1"
    statement TEXT NOT NULL,
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prog_po_year UNIQUE (programme_id, code, academic_year)
);

CREATE TABLE po_competencies (
    id VARCHAR(50) PRIMARY KEY,
    po_id VARCHAR(50) NOT NULL REFERENCES programme_outcomes(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL, -- e.g. "PO1.1"
    statement TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE programme_specific_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL, -- e.g. "PSO1"
    statement TEXT NOT NULL,
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prog_pso_year UNIQUE (programme_id, code, academic_year)
);

CREATE TABLE pso_competencies (
    id VARCHAR(50) PRIMARY KEY,
    pso_id VARCHAR(50) NOT NULL REFERENCES programme_specific_outcomes(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL, -- e.g. "PSO1.1"
    statement TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE peo_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL, -- e.g. "PEO1"
    statement TEXT NOT NULL,
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE course_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL, -- e.g. "C321.1"
    statement TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE co_po_mappings (
    id VARCHAR(50) PRIMARY KEY,
    course_outcome_id VARCHAR(50) NOT NULL REFERENCES course_outcomes(id) ON DELETE CASCADE,
    po_code VARCHAR(20) NOT NULL,
    mapping_level INT NOT NULL DEFAULT 0, -- 0 (no correlation), 1 (low), 2 (medium), 3 (high)
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_co_po UNIQUE (course_outcome_id, po_code)
);

CREATE TABLE co_pso_mappings (
    id VARCHAR(50) PRIMARY KEY,
    course_outcome_id VARCHAR(50) NOT NULL REFERENCES course_outcomes(id) ON DELETE CASCADE,
    pso_code VARCHAR(20) NOT NULL,
    mapping_level INT NOT NULL DEFAULT 0, -- 0, 1, 2, 3
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_co_pso UNIQUE (course_outcome_id, pso_code)
);

-- Target Level Stores
CREATE TABLE programme_targets (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    outcome_code VARCHAR(20) NOT NULL, -- PO1..12 or PSO1..3
    target_value NUMERIC(4,2) NOT NULL DEFAULT 2.50, -- 1.0 to 3.0 scale
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_prog_target UNIQUE (programme_id, outcome_code)
);

CREATE TABLE course_co_targets (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    target_value NUMERIC(4,2) NOT NULL DEFAULT 2.50, -- 1.0 to 3.0 scale
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_co_target UNIQUE (course_id, co_code)
);

CREATE INDEX idx_po_prog ON programme_outcomes(programme_id);
CREATE INDEX idx_pso_prog ON programme_specific_outcomes(programme_id);
CREATE INDEX idx_co_course ON course_outcomes(course_id);
CREATE INDEX idx_co_po_map ON co_po_mappings(course_outcome_id);
CREATE INDEX idx_co_pso_map ON co_pso_mappings(course_outcome_id);
