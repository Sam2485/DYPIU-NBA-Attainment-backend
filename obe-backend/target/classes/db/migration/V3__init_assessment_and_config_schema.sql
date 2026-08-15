-- V3__init_assessment_and_config_schema.sql
-- Assessment Data Management (Marks, Surveys) & Configuration Schema

CREATE TABLE IF NOT EXISTS attainment_configurations (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(255) NOT NULL,
    direct_weight NUMERIC(5,2) NOT NULL DEFAULT 80.00, -- 80%
    indirect_weight NUMERIC(5,2) NOT NULL DEFAULT 20.00, -- 20%
    direct_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00, -- e.g. 60%
    indirect_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00,
    status VARCHAR(20) DEFAULT 'DRAFT', -- DRAFT, SUBMITTED, VERIFIED
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attainment_config_course UNIQUE (course_id)
);

CREATE TABLE IF NOT EXISTS attainment_levels (
    id VARCHAR(50) PRIMARY KEY,
    config_id VARCHAR(50) NOT NULL REFERENCES attainment_configurations(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL, -- DIRECT, INDIRECT
    level_val INT NOT NULL, -- 1, 2, 3
    min_percentage NUMERIC(5,2) NOT NULL, -- 0, 50, 70
    max_percentage NUMERIC(5,2) NOT NULL, -- 50, 70, 100
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS end_sem_marks_uploads (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    record_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'COMPLETED'
);

CREATE TABLE IF NOT EXISTS student_co_marks (
    id VARCHAR(50) PRIMARY KEY,
    upload_id VARCHAR(50) REFERENCES end_sem_marks_uploads(id) ON DELETE CASCADE,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    student_id VARCHAR(50) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    prn VARCHAR(50) NOT NULL,
    student_name VARCHAR(150),
    co_code VARCHAR(30) NOT NULL,
    marks_obtained NUMERIC(5,2) NOT NULL,
    max_marks NUMERIC(5,2) NOT NULL DEFAULT 100.00,
    percentage NUMERIC(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_co_mark UNIQUE (course_id, student_id, co_code)
);

CREATE TABLE IF NOT EXISTS course_end_surveys (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    academic_year VARCHAR(20) NOT NULL,
    total_respondents INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS survey_responses (
    id VARCHAR(50) PRIMARY KEY,
    survey_id VARCHAR(50) NOT NULL REFERENCES course_end_surveys(id) ON DELETE CASCADE,
    student_id VARCHAR(50) REFERENCES students(id) ON DELETE SET NULL,
    prn VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS survey_response_details (
    id VARCHAR(50) PRIMARY KEY,
    response_id VARCHAR(50) NOT NULL REFERENCES survey_responses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 3), -- 1, 2, 3 scale
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS programme_exit_surveys (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    total_respondents INT DEFAULT 0,
    avg_exit_score NUMERIC(4,2) DEFAULT 2.50,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attainment_config_course ON attainment_configurations(course_id);
CREATE INDEX IF NOT EXISTS idx_student_co_marks_course ON student_co_marks(course_id);
CREATE INDEX IF NOT EXISTS idx_student_co_marks_student ON student_co_marks(student_id);
