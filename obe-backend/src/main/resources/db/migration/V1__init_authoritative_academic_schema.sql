-- ============================================================
-- V1__init_authoritative_academic_schema.sql
--
-- Authoritative Academic Schema Baseline for DYPIU NBA Attainment Backend
--
-- Authoritative Academic Hierarchy:
--
-- School
--   └── Department
--        └── MasterProgramme [master_programmes]
--             ├── MasterCourse [master_courses]
--             │
--             └── ProgrammeBatch [programme_batches]
--                  ├── Programme Coordinator (Assignment)
--                  ├── Programme Outcomes (PO1-PO12 + target + status) [programme_outcomes]
--                  │    └── PO Competencies [po_competencies]
--                  ├── Programme Specific Outcomes (PSO1-PSO3 + target + status) [programme_specific_outcomes]
--                  │    └── PSO Competencies [pso_competencies]
--                  ├── PEO Outcomes [peo_outcomes]
--                  ├── Programme Exit Surveys [programme_exit_surveys]
--                  ├── Programme ATR [programme_atrs]
--                  ├── Students [students]
--                  │
--                  └── ProgrammeBatchCourse [programme_batch_courses]
--                       ├── Course Coordinator (Assignment)
--                       ├── Course Outcomes (CO1-CO6 + target + blooms + status) [course_outcomes]
--                       │    ├── CO-PO Mapping [co_po_mappings]
--                       │    └── CO-PSO Mapping [co_pso_mappings]
--                       ├── Course Mapping Keywords [course_mapping_keywords]
--                       ├── Attainment Configuration [attainment_configurations]
--                       │    └── Attainment Levels [attainment_levels]
--                       ├── End-Sem Marks Uploads [end_sem_marks_uploads]
--                       ├── Student CO Marks [student_co_marks]
--                       ├── Course End Surveys & Responses [course_end_surveys, survey_responses, survey_response_details]
--                       ├── Uploaded Documents [uploaded_documents]
--                       └── Course ATR [course_atrs]
--
-- Also contains:
--   - Users & Security Scope [users]
--   - Approval Workflow Engine [approval_requests, approval_history]
--   - Role Setup Wizard Tracking [director_setup_progress, hod_setup_progress, pc_setup_progress, cc_setup_progress]
--
-- ============================================================


-- ============================================================
-- 1. SCHOOLS
-- ============================================================
CREATE TABLE IF NOT EXISTS schools (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    director_id BIGINT UNIQUE,
    director_name VARCHAR(255),
    director VARCHAR(150),
    director_email VARCHAR(150) UNIQUE,
    est_year VARCHAR(10),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 2. DEPARTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(50) PRIMARY KEY,
    school_id VARCHAR(50) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    hod VARCHAR(150),
    hod_email VARCHAR(150),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_school_code UNIQUE (school_id, code)
);


-- ============================================================
-- 3. MASTER PROGRAMMES
-- ============================================================
CREATE TABLE IF NOT EXISTS master_programmes (
    id VARCHAR(50) PRIMARY KEY,
    department_id VARCHAR(50) NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    duration_years INTEGER NOT NULL DEFAULT 4,
    department_name VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. USERS (Security Principal & Organizational Scope)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL,
    department VARCHAR(255),
    programme VARCHAR(255),
    school_id VARCHAR(50) REFERENCES schools(id) ON DELETE SET NULL,
    department_id VARCHAR(50) REFERENCES departments(id) ON DELETE SET NULL,
    programme_id VARCHAR(50) REFERENCES master_programmes(id) ON DELETE SET NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. PROGRAMME BATCHES (Student Cohort Instances)
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_batches (
    id VARCHAR(50) PRIMARY KEY,
    master_programme_id VARCHAR(50) NOT NULL REFERENCES master_programmes(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    start_year INTEGER NOT NULL,
    end_year INTEGER NOT NULL,
    duration_years INTEGER NOT NULL DEFAULT 4,
    coordinator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    coordinator_name VARCHAR(150),
    coordinator_email VARCHAR(150),
    year_level VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_programme_batch_start_year UNIQUE (master_programme_id, start_year),
    CONSTRAINT chk_batch_year_range CHECK (end_year > start_year),
    CONSTRAINT chk_batch_duration CHECK (end_year - start_year = duration_years)
);


-- ============================================================
-- 6. SEMESTERS
-- ============================================================
CREATE TABLE IF NOT EXISTS semesters (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    semester_num INTEGER NOT NULL,
    name VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_batch_semester UNIQUE (programme_batch_id, semester_num),
    CONSTRAINT chk_semester_number CHECK (semester_num >= 1)
);


-- ============================================================
-- 7. MASTER COURSES (Reusable Master Course Catalog)
-- ============================================================
CREATE TABLE IF NOT EXISTS master_courses (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    master_programme_id VARCHAR(50) NOT NULL REFERENCES master_programmes(id) ON DELETE CASCADE,
    credits INTEGER NOT NULL DEFAULT 4,
    course_type VARCHAR(50) NOT NULL DEFAULT 'CORE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_programme_code UNIQUE (master_programme_id, code)
);


-- ============================================================
-- 8. PROGRAMME BATCH COURSES (Batch-Specific Course Instances)
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_batch_courses (
    id VARCHAR(50) PRIMARY KEY,
    master_course_id VARCHAR(50) NOT NULL REFERENCES master_courses(id) ON DELETE CASCADE,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    semester INTEGER NOT NULL,
    course_coordinator_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    course_coordinator_name VARCHAR(255),
    assigned_faculty TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_course_sem UNIQUE (programme_batch_id, master_course_id, semester),
    CONSTRAINT chk_offering_semester CHECK (semester >= 1)
);


-- ============================================================
-- 9. STUDENTS (Cohort Enrolled Students)
-- ============================================================
CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    prn VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 10. PROGRAMME OUTCOMES (Batch-Scoped PO1 - PO12)
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    statement TEXT NOT NULL,
    target NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_po_code UNIQUE (programme_batch_id, code),
    CONSTRAINT chk_po_target CHECK (target >= 0 AND target <= 3)
);


-- ============================================================
-- 11. PO COMPETENCIES
-- ============================================================
CREATE TABLE IF NOT EXISTS po_competencies (
    id VARCHAR(50) PRIMARY KEY,
    po_id VARCHAR(50) NOT NULL REFERENCES programme_outcomes(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    statement TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_po_competency UNIQUE (po_id, code)
);


-- ============================================================
-- 12. PROGRAMME SPECIFIC OUTCOMES (Batch-Scoped PSO1 - PSO3)
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_specific_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    statement TEXT NOT NULL,
    target NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_pso_code UNIQUE (programme_batch_id, code),
    CONSTRAINT chk_pso_target CHECK (target >= 0 AND target <= 3)
);


-- ============================================================
-- 13. PSO COMPETENCIES
-- ============================================================
CREATE TABLE IF NOT EXISTS pso_competencies (
    id VARCHAR(50) PRIMARY KEY,
    pso_id VARCHAR(50) NOT NULL REFERENCES programme_specific_outcomes(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    statement TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pso_competency UNIQUE (pso_id, code)
);


-- ============================================================
-- 14. PEO OUTCOMES (Batch-Scoped Program Educational Objectives)
-- ============================================================
CREATE TABLE IF NOT EXISTS peo_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    statement TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_peo_code UNIQUE (programme_batch_id, code)
);


-- ============================================================
-- 15. COURSE OUTCOMES (Batch-Course-Scoped CO1 - CO6)
-- ============================================================
CREATE TABLE IF NOT EXISTS course_outcomes (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    code VARCHAR(30) NOT NULL,
    statement TEXT NOT NULL,
    target_level NUMERIC(4,2) NOT NULL DEFAULT 2.50,
    blooms_level VARCHAR(50) DEFAULT 'L3 - Apply',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_course_co_code UNIQUE (programme_batch_course_id, code),
    CONSTRAINT chk_co_target_level CHECK (target_level >= 0 AND target_level <= 3)
);


-- ============================================================
-- 16. CO-PO MAPPINGS (Correlation Matrix 0 - 3)
-- ============================================================
CREATE TABLE IF NOT EXISTS co_po_mappings (
    id VARCHAR(50) PRIMARY KEY,
    course_outcome_id VARCHAR(50) NOT NULL REFERENCES course_outcomes(id) ON DELETE CASCADE,
    po_code VARCHAR(20) NOT NULL,
    mapping_level INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_co_po UNIQUE (course_outcome_id, po_code),
    CONSTRAINT chk_co_po_mapping_level CHECK (mapping_level BETWEEN 0 AND 3)
);


-- ============================================================
-- 17. CO-PSO MAPPINGS (Correlation Matrix 0 - 3)
-- ============================================================
CREATE TABLE IF NOT EXISTS co_pso_mappings (
    id VARCHAR(50) PRIMARY KEY,
    course_outcome_id VARCHAR(50) NOT NULL REFERENCES course_outcomes(id) ON DELETE CASCADE,
    pso_code VARCHAR(20) NOT NULL,
    mapping_level INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_co_pso UNIQUE (course_outcome_id, pso_code),
    CONSTRAINT chk_co_pso_mapping_level CHECK (mapping_level BETWEEN 0 AND 3)
);


-- ============================================================
-- 18. COURSE MAPPING KEYWORDS (Justifications)
-- ============================================================
CREATE TABLE IF NOT EXISTS course_mapping_keywords (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    keyword_type VARCHAR(20) NOT NULL,
    keywords_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_batch_course_keyword_type UNIQUE (programme_batch_course_id, keyword_type),
    CONSTRAINT chk_keyword_type CHECK (keyword_type IN ('PO', 'PSO'))
);


-- ============================================================
-- 19. ATTAINMENT CONFIGURATIONS (Direct 80% / Indirect 20%)
-- ============================================================
CREATE TABLE IF NOT EXISTS attainment_configurations (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    direct_weight NUMERIC(5,2) NOT NULL DEFAULT 80.00,
    indirect_weight NUMERIC(5,2) NOT NULL DEFAULT 20.00,
    direct_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00,
    indirect_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    direct_levels_json TEXT,
    indirect_levels_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attainment_config_batch_course UNIQUE (programme_batch_course_id)
);


-- ============================================================
-- 20. ATTAINMENT LEVELS
-- ============================================================
CREATE TABLE IF NOT EXISTS attainment_levels (
    id VARCHAR(50) PRIMARY KEY,
    config_id VARCHAR(50) NOT NULL REFERENCES attainment_configurations(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    level_val INTEGER NOT NULL,
    min_percentage NUMERIC(5,2) NOT NULL,
    max_percentage NUMERIC(5,2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attainment_level_type CHECK (type IN ('DIRECT', 'INDIRECT')),
    CONSTRAINT chk_attainment_level_value CHECK (level_val BETWEEN 1 AND 3),
    CONSTRAINT chk_attainment_percentage_range CHECK (
        min_percentage >= 0
        AND max_percentage <= 100
        AND max_percentage >= min_percentage
    ),
    CONSTRAINT uq_attainment_level UNIQUE (config_id, type, level_val)
);


-- ============================================================
-- 21. END-SEM MARKS UPLOAD METADATA
-- ============================================================
CREATE TABLE IF NOT EXISTS end_sem_marks_uploads (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    uploaded_by VARCHAR(150) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    record_count INTEGER DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED'
);


-- ============================================================
-- 22. STUDENT CO MARKS
-- ============================================================
CREATE TABLE IF NOT EXISTS student_co_marks (
    id VARCHAR(50) PRIMARY KEY,
    upload_id VARCHAR(50) REFERENCES end_sem_marks_uploads(id) ON DELETE CASCADE,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    student_id VARCHAR(50) NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    prn VARCHAR(50) NOT NULL,
    student_name VARCHAR(150),
    co_code VARCHAR(30) NOT NULL,
    marks_obtained NUMERIC(8,2) NOT NULL,
    max_marks NUMERIC(8,2) NOT NULL DEFAULT 100.00,
    percentage NUMERIC(5,2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_co_mark UNIQUE (programme_batch_course_id, student_id, co_code)
);


-- ============================================================
-- 23. COURSE END SURVEYS
-- ============================================================
CREATE TABLE IF NOT EXISTS course_end_surveys (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    total_respondents INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_end_survey UNIQUE (programme_batch_course_id)
);


-- ============================================================
-- 24. SURVEY RESPONSES
-- ============================================================
CREATE TABLE IF NOT EXISTS survey_responses (
    id VARCHAR(50) PRIMARY KEY,
    survey_id VARCHAR(50) NOT NULL REFERENCES course_end_surveys(id) ON DELETE CASCADE,
    student_id VARCHAR(50) REFERENCES students(id) ON DELETE SET NULL,
    prn VARCHAR(50),
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 25. SURVEY RESPONSE DETAILS
-- ============================================================
CREATE TABLE IF NOT EXISTS survey_response_details (
    id VARCHAR(50) PRIMARY KEY,
    response_id VARCHAR(50) NOT NULL REFERENCES survey_responses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    rating INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_survey_rating CHECK (rating BETWEEN 1 AND 3)
);


-- ============================================================
-- 26. PROGRAMME EXIT SURVEYS
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_exit_surveys (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    total_respondents INTEGER DEFAULT 0,
    avg_exit_score NUMERIC(4,2) DEFAULT 2.50,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_programme_exit_survey UNIQUE (programme_batch_id)
);


-- ============================================================
-- 27. UPLOADED DOCUMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS uploaded_documents (
    id VARCHAR(255) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    programme_batch_course_id VARCHAR(50) REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    saved_file_name VARCHAR(255) NOT NULL,
    saved_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    records_processed INTEGER,
    threshold_percentage NUMERIC(5,2),
    uploaded_by VARCHAR(150),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 28. COURSE ACTION TAKEN REPORTS (Course ATR)
-- ============================================================
CREATE TABLE IF NOT EXISTS course_atrs (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    co_code VARCHAR(30) NOT NULL,
    title VARCHAR(255),
    target_score NUMERIC(4,2) NOT NULL,
    actual_score NUMERIC(4,2) NOT NULL,
    pct_achieved NUMERIC(5,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    statement TEXT,
    actions_json TEXT,
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    verification_comments TEXT,
    verified_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_course_co_atr UNIQUE (programme_batch_course_id, co_code)
);


-- ============================================================
-- 29. PROGRAMME ACTION TAKEN REPORTS (Programme ATR)
-- ============================================================
CREATE TABLE IF NOT EXISTS programme_atrs (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL REFERENCES programme_batches(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    submitted_by VARCHAR(150),
    submitted_at TIMESTAMP WITH TIME ZONE,
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    verified_by VARCHAR(150),
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_comments TEXT,
    observations_json TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_programme_batch_atr UNIQUE (programme_batch_id)
);


-- ============================================================
-- 30. APPROVAL REQUESTS (Centralized Workflow Engine)
-- ============================================================
CREATE TABLE IF NOT EXISTS approval_requests (
    id VARCHAR(50) PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    resource_id VARCHAR(50) NOT NULL,
    school_id VARCHAR(50) REFERENCES schools(id) ON DELETE CASCADE,
    department_id VARCHAR(50) REFERENCES departments(id) ON DELETE CASCADE,
    master_programme_id VARCHAR(50) REFERENCES master_programmes(id) ON DELETE CASCADE,
    programme_batch_id VARCHAR(50) REFERENCES programme_batches(id) ON DELETE CASCADE,
    master_course_id VARCHAR(50) REFERENCES master_courses(id) ON DELETE CASCADE,
    programme_batch_course_id VARCHAR(50) REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    submitted_by VARCHAR(150) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    approved_by VARCHAR(150),
    approved_at TIMESTAMP WITH TIME ZONE,
    remarks TEXT,
    details TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 31. APPROVAL HISTORY (Immutable Review Audit Log)
-- ============================================================
CREATE TABLE IF NOT EXISTS approval_history (
    id VARCHAR(50) PRIMARY KEY,
    approval_request_id VARCHAR(50) NOT NULL REFERENCES approval_requests(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_name VARCHAR(150) NOT NULL,
    actor_role VARCHAR(50) NOT NULL,
    action VARCHAR(50) NOT NULL,
    comments TEXT,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 32. DIRECTOR SETUP PROGRESS
-- ============================================================
CREATE TABLE IF NOT EXISTS director_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    school_id VARCHAR(50) NOT NULL UNIQUE REFERENCES schools(id) ON DELETE CASCADE,
    current_step INTEGER NOT NULL DEFAULT 1,
    current_step_enum VARCHAR(30) NOT NULL DEFAULT 'SCHOOL',
    overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'school,department,programme,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 33. HOD SETUP PROGRESS
-- ============================================================
CREATE TABLE IF NOT EXISTS hod_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    department_id VARCHAR(50) NOT NULL UNIQUE REFERENCES departments(id) ON DELETE CASCADE,
    hod_email VARCHAR(150),
    current_step INTEGER NOT NULL DEFAULT 1,
    overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'batch,outcomes,coordinators,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 34. PROGRAMME COORDINATOR SETUP PROGRESS
-- ============================================================
CREATE TABLE IF NOT EXISTS pc_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_id VARCHAR(50) NOT NULL UNIQUE REFERENCES programme_batches(id) ON DELETE CASCADE,
    coordinator_email VARCHAR(150),
    current_step INTEGER NOT NULL DEFAULT 1,
    overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'courses,targets,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 35. COURSE COORDINATOR SETUP PROGRESS
-- ============================================================
CREATE TABLE IF NOT EXISTS cc_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    programme_batch_course_id VARCHAR(50) NOT NULL UNIQUE REFERENCES programme_batch_courses(id) ON DELETE CASCADE,
    coordinator_email VARCHAR(150),
    current_step INTEGER NOT NULL DEFAULT 1,
    overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'cos,co_targets,co_mapping,direct,indirect,attainment,course_atr',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- INDEXES FOR PERFORMANCE AND SCOPE RESOLUTION
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_departments_school ON departments(school_id);
CREATE INDEX IF NOT EXISTS idx_master_programmes_department ON master_programmes(department_id);
CREATE INDEX IF NOT EXISTS idx_programme_batches_programme ON programme_batches(master_programme_id);
CREATE INDEX IF NOT EXISTS idx_programme_batches_coordinator ON programme_batches(coordinator_id);
CREATE INDEX IF NOT EXISTS idx_semesters_batch ON semesters(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_master_courses_programme ON master_courses(master_programme_id);
CREATE INDEX IF NOT EXISTS idx_programme_batch_courses_batch ON programme_batch_courses(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_programme_batch_courses_course ON programme_batch_courses(master_course_id);
CREATE INDEX IF NOT EXISTS idx_programme_batch_courses_coordinator ON programme_batch_courses(course_coordinator_id);
CREATE INDEX IF NOT EXISTS idx_students_batch ON students(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_programme_outcomes_batch ON programme_outcomes(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_po_competencies_po ON po_competencies(po_id);
CREATE INDEX IF NOT EXISTS idx_programme_pso_batch ON programme_specific_outcomes(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_pso_competencies_pso ON pso_competencies(pso_id);
CREATE INDEX IF NOT EXISTS idx_peo_outcomes_batch ON peo_outcomes(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_course_outcomes_batch_course ON course_outcomes(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_co_po_mapping_co ON co_po_mappings(course_outcome_id);
CREATE INDEX IF NOT EXISTS idx_co_pso_mapping_co ON co_pso_mappings(course_outcome_id);
CREATE INDEX IF NOT EXISTS idx_mapping_keywords_batch_course ON course_mapping_keywords(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_attainment_config_batch_course ON attainment_configurations(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_marks_upload_batch_course ON end_sem_marks_uploads(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_student_co_marks_batch_course ON student_co_marks(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_student_co_marks_student ON student_co_marks(student_id);
CREATE INDEX IF NOT EXISTS idx_course_surveys_batch_course ON course_end_surveys(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_programme_exit_survey_batch ON programme_exit_surveys(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_docs_batch ON uploaded_documents(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_docs_batch_course ON uploaded_documents(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_course_atr_batch_course ON course_atrs(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_programme_atr_batch ON programme_atrs(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_status ON approval_requests(status);
CREATE INDEX IF NOT EXISTS idx_approval_requests_resource ON approval_requests(resource_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_batch ON approval_requests(programme_batch_id);
CREATE INDEX IF NOT EXISTS idx_approval_requests_batch_course ON approval_requests(programme_batch_course_id);
CREATE INDEX IF NOT EXISTS idx_approval_history_request ON approval_history(approval_request_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_scope ON users(school_id, department_id, programme_id);
