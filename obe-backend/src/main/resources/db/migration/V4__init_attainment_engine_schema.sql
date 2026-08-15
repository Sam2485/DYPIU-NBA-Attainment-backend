-- V4__init_attainment_engine_schema.sql
-- Attainment Engine (Calculation Runs, Direct, Indirect, Overall CO & PO/PSO Attainment)

CREATE TABLE IF NOT EXISTS calculation_runs (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) REFERENCES courses(id) ON DELETE CASCADE,
    programme_id VARCHAR(50) REFERENCES programmes(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) REFERENCES batches(id) ON DELETE CASCADE,
    academic_year VARCHAR(20) NOT NULL,
    run_type VARCHAR(30) NOT NULL, -- COURSE_CO, PROGRAMME_PO_PSO
    run_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    executed_by VARCHAR(150)
);

CREATE TABLE IF NOT EXISTS direct_co_attainments (
    id VARCHAR(50) PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL REFERENCES calculation_runs(id) ON DELETE CASCADE,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    students_attempted INT NOT NULL DEFAULT 0,
    students_attained INT NOT NULL DEFAULT 0,
    percentage_attained NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    attainment_level INT NOT NULL DEFAULT 1, -- 1, 2, 3
    attainment_score NUMERIC(4,2) NOT NULL DEFAULT 1.00, -- 1.0 to 3.0
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_run_direct_co UNIQUE (run_id, co_code)
);

CREATE TABLE IF NOT EXISTS indirect_co_attainments (
    id VARCHAR(50) PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL REFERENCES calculation_runs(id) ON DELETE CASCADE,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    total_responses INT NOT NULL DEFAULT 0,
    avg_survey_score NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    percentage_attained NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    attainment_level INT NOT NULL DEFAULT 1,
    attainment_score NUMERIC(4,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_run_indirect_co UNIQUE (run_id, co_code)
);

CREATE TABLE IF NOT EXISTS overall_co_attainments (
    id VARCHAR(50) PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL REFERENCES calculation_runs(id) ON DELETE CASCADE,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    direct_score NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    indirect_score NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    overall_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00, -- Direct*0.8 + Indirect*0.2
    target_score NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    is_target_achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_run_overall_co UNIQUE (run_id, co_code)
);

CREATE TABLE IF NOT EXISTS po_attainments (
    id VARCHAR(50) PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL REFERENCES calculation_runs(id) ON DELETE CASCADE,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    po_code VARCHAR(20) NOT NULL,
    direct_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    indirect_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    final_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    target_attainment NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    is_target_achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_run_po UNIQUE (run_id, po_code)
);

CREATE TABLE IF NOT EXISTS pso_attainments (
    id VARCHAR(50) PRIMARY KEY,
    run_id VARCHAR(50) NOT NULL REFERENCES calculation_runs(id) ON DELETE CASCADE,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    pso_code VARCHAR(20) NOT NULL,
    direct_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    indirect_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    final_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,
    target_attainment NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    is_target_achieved BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_run_pso UNIQUE (run_id, pso_code)
);

CREATE INDEX IF NOT EXISTS idx_calc_runs_course ON calculation_runs(course_id);
CREATE INDEX IF NOT EXISTS idx_calc_runs_prog ON calculation_runs(programme_id);
