-- ============================================================
-- V3__create_assessment_and_attainment_schema.sql
--
-- Assessment data
-- Document uploads
-- Course surveys
-- Programme exit survey
-- Attainment configuration
-- Calculation runs
-- CO / PO / PSO attainment
--
-- All cohort-specific course data uses course_offering_id.
-- ============================================================


-- ============================================================
-- 1. ATTAINMENT CONFIGURATION
-- ============================================================

CREATE TABLE attainment_configurations (
                                           id VARCHAR(50) PRIMARY KEY,

                                           course_offering_id VARCHAR(50) NOT NULL
                                               REFERENCES course_offerings(id) ON DELETE CASCADE,

                                           direct_weight NUMERIC(5,2) NOT NULL DEFAULT 80.00,

                                           indirect_weight NUMERIC(5,2) NOT NULL DEFAULT 20.00,

                                           direct_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00,

                                           indirect_threshold NUMERIC(5,2) NOT NULL DEFAULT 60.00,

                                           status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

                                           submitted_by VARCHAR(150),
                                           submitted_at TIMESTAMP WITH TIME ZONE,

                                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                           CONSTRAINT uq_attainment_config_offering
                                               UNIQUE (course_offering_id)
);


-- ============================================================
-- 2. ATTAINMENT LEVELS
-- ============================================================

CREATE TABLE attainment_levels (
                                   id VARCHAR(50) PRIMARY KEY,

                                   config_id VARCHAR(50) NOT NULL
                                       REFERENCES attainment_configurations(id) ON DELETE CASCADE,

                                   type VARCHAR(20) NOT NULL,

                                   level_val INTEGER NOT NULL,

                                   min_percentage NUMERIC(5,2) NOT NULL,

                                   max_percentage NUMERIC(5,2) NOT NULL,

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT chk_attainment_level_type
                                       CHECK (type IN ('DIRECT', 'INDIRECT')),

                                   CONSTRAINT chk_attainment_level_value
                                       CHECK (level_val BETWEEN 1 AND 3),

                                   CONSTRAINT chk_attainment_percentage_range
                                       CHECK (
                                           min_percentage >= 0
                                               AND max_percentage <= 100
                                               AND max_percentage >= min_percentage
                                           ),

                                   CONSTRAINT uq_attainment_level
                                       UNIQUE (config_id, type, level_val)
);


-- ============================================================
-- 3. END-SEM MARKS UPLOAD
-- ============================================================

CREATE TABLE end_sem_marks_uploads (
                                       id VARCHAR(50) PRIMARY KEY,

                                       course_offering_id VARCHAR(50) NOT NULL
                                           REFERENCES course_offerings(id) ON DELETE CASCADE,

                                       file_name VARCHAR(255) NOT NULL,

                                       file_path VARCHAR(500) NOT NULL,

                                       uploaded_by VARCHAR(150) NOT NULL,

                                       uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                       record_count INTEGER DEFAULT 0,

                                       status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED'
);


-- ============================================================
-- 4. STUDENT CO MARKS
-- ============================================================

CREATE TABLE student_co_marks (
                                  id VARCHAR(50) PRIMARY KEY,

                                  upload_id VARCHAR(50)
                                      REFERENCES end_sem_marks_uploads(id) ON DELETE CASCADE,

                                  course_offering_id VARCHAR(50) NOT NULL
                                      REFERENCES course_offerings(id) ON DELETE CASCADE,

                                  student_id VARCHAR(50) NOT NULL
                                      REFERENCES students(id) ON DELETE CASCADE,

                                  prn VARCHAR(50) NOT NULL,

                                  student_name VARCHAR(150),

                                  co_code VARCHAR(30) NOT NULL,

                                  marks_obtained NUMERIC(8,2) NOT NULL,

                                  max_marks NUMERIC(8,2) NOT NULL DEFAULT 100.00,

                                  percentage NUMERIC(5,2),

                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_student_co_mark
                                      UNIQUE (course_offering_id, student_id, co_code)
);


-- ============================================================
-- 5. COURSE END SURVEY
-- ============================================================

CREATE TABLE course_end_surveys (
                                    id VARCHAR(50) PRIMARY KEY,

                                    course_offering_id VARCHAR(50) NOT NULL
                                        REFERENCES course_offerings(id) ON DELETE CASCADE,

                                    total_respondents INTEGER DEFAULT 0,

                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT uq_course_end_survey
                                        UNIQUE (course_offering_id)
);


-- ============================================================
-- 6. SURVEY RESPONSES
-- ============================================================

CREATE TABLE survey_responses (
                                  id VARCHAR(50) PRIMARY KEY,

                                  survey_id VARCHAR(50) NOT NULL
                                      REFERENCES course_end_surveys(id) ON DELETE CASCADE,

                                  student_id VARCHAR(50)
                                                        REFERENCES students(id) ON DELETE SET NULL,

                                  prn VARCHAR(50),

                                  submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 7. SURVEY RESPONSE DETAILS
-- ============================================================

CREATE TABLE survey_response_details (
                                         id VARCHAR(50) PRIMARY KEY,

                                         response_id VARCHAR(50) NOT NULL
                                             REFERENCES survey_responses(id) ON DELETE CASCADE,

                                         co_code VARCHAR(30) NOT NULL,

                                         rating INTEGER NOT NULL,

                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT chk_survey_rating
                                             CHECK (rating BETWEEN 1 AND 3)
);


-- ============================================================
-- 8. PROGRAMME EXIT SURVEY
-- ============================================================

CREATE TABLE programme_exit_surveys (
                                        id VARCHAR(50) PRIMARY KEY,

                                        programme_id VARCHAR(50) NOT NULL
                                            REFERENCES programmes(id) ON DELETE CASCADE,

                                        batch_id VARCHAR(50) NOT NULL
                                            REFERENCES batches(id) ON DELETE CASCADE,

                                        total_respondents INTEGER DEFAULT 0,

                                        avg_exit_score NUMERIC(4,2) DEFAULT 2.50,

                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT uq_programme_exit_survey
                                            UNIQUE (programme_id, batch_id)
);


-- ============================================================
-- 9. UPLOADED DOCUMENTS
-- ============================================================

CREATE TABLE uploaded_documents (
                                    id VARCHAR(255) PRIMARY KEY,

                                    batch_id VARCHAR(50) NOT NULL
                                        REFERENCES batches(id) ON DELETE CASCADE,

                                    course_offering_id VARCHAR(50)
                                        REFERENCES course_offerings(id) ON DELETE CASCADE,

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
-- 10. CALCULATION RUNS
-- ============================================================

CREATE TABLE calculation_runs (
                                  id VARCHAR(50) PRIMARY KEY,

                                  course_offering_id VARCHAR(50)
                                      REFERENCES course_offerings(id) ON DELETE CASCADE,

                                  programme_id VARCHAR(50)
                                      REFERENCES programmes(id) ON DELETE CASCADE,

                                  batch_id VARCHAR(50)
                                      REFERENCES batches(id) ON DELETE CASCADE,

                                  run_type VARCHAR(30) NOT NULL,

                                  run_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                  status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',

                                  executed_by VARCHAR(150),

                                  CONSTRAINT chk_calculation_run_type
                                      CHECK (run_type IN ('COURSE_CO', 'PROGRAMME_PO_PSO')),

                                  CONSTRAINT chk_calculation_run_scope
                                      CHECK (
                                          (
                                              run_type = 'COURSE_CO'
                                                  AND course_offering_id IS NOT NULL
                                              )
                                              OR
                                          (
                                              run_type = 'PROGRAMME_PO_PSO'
                                                  AND programme_id IS NOT NULL
                                                  AND batch_id IS NOT NULL
                                              )
                                          )
);


-- ============================================================
-- 11. DIRECT CO ATTAINMENT
-- ============================================================

CREATE TABLE direct_co_attainments (
                                       id VARCHAR(50) PRIMARY KEY,

                                       run_id VARCHAR(50) NOT NULL
                                           REFERENCES calculation_runs(id) ON DELETE CASCADE,

                                       course_offering_id VARCHAR(50) NOT NULL
                                           REFERENCES course_offerings(id) ON DELETE CASCADE,

                                       co_code VARCHAR(30) NOT NULL,

                                       students_attempted INTEGER NOT NULL DEFAULT 0,

                                       students_attained INTEGER NOT NULL DEFAULT 0,

                                       percentage_attained NUMERIC(5,2) NOT NULL DEFAULT 0.00,

                                       attainment_level INTEGER NOT NULL DEFAULT 1,

                                       attainment_score NUMERIC(4,2) NOT NULL DEFAULT 1.00,

                                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                       CONSTRAINT uq_run_direct_co
                                           UNIQUE (run_id, co_code)
);


-- ============================================================
-- 12. INDIRECT CO ATTAINMENT
-- ============================================================

CREATE TABLE indirect_co_attainments (
                                         id VARCHAR(50) PRIMARY KEY,

                                         run_id VARCHAR(50) NOT NULL
                                             REFERENCES calculation_runs(id) ON DELETE CASCADE,

                                         course_offering_id VARCHAR(50) NOT NULL
                                             REFERENCES course_offerings(id) ON DELETE CASCADE,

                                         co_code VARCHAR(30) NOT NULL,

                                         total_responses INTEGER NOT NULL DEFAULT 0,

                                         avg_survey_score NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                         percentage_attained NUMERIC(5,2) NOT NULL DEFAULT 0.00,

                                         attainment_level INTEGER NOT NULL DEFAULT 1,

                                         attainment_score NUMERIC(4,2) NOT NULL DEFAULT 1.00,

                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT uq_run_indirect_co
                                             UNIQUE (run_id, co_code)
);


-- ============================================================
-- 13. OVERALL CO ATTAINMENT
-- ============================================================

CREATE TABLE overall_co_attainments (
                                        id VARCHAR(50) PRIMARY KEY,

                                        run_id VARCHAR(50) NOT NULL
                                            REFERENCES calculation_runs(id) ON DELETE CASCADE,

                                        course_offering_id VARCHAR(50) NOT NULL
                                            REFERENCES course_offerings(id) ON DELETE CASCADE,

                                        co_code VARCHAR(30) NOT NULL,

                                        direct_score NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                        indirect_score NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                        overall_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                        target_score NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                        is_target_achieved BOOLEAN NOT NULL DEFAULT FALSE,

                                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                        CONSTRAINT uq_run_overall_co
                                            UNIQUE (run_id, co_code)
);


-- ============================================================
-- 14. PO ATTAINMENT
-- ============================================================

CREATE TABLE po_attainments (
                                id VARCHAR(50) PRIMARY KEY,

                                run_id VARCHAR(50) NOT NULL
                                    REFERENCES calculation_runs(id) ON DELETE CASCADE,

                                programme_id VARCHAR(50) NOT NULL
                                    REFERENCES programmes(id) ON DELETE CASCADE,

                                batch_id VARCHAR(50) NOT NULL
                                    REFERENCES batches(id) ON DELETE CASCADE,

                                po_code VARCHAR(20) NOT NULL,

                                direct_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                indirect_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                final_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                target_attainment NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                is_target_achieved BOOLEAN NOT NULL DEFAULT FALSE,

                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT uq_run_po
                                    UNIQUE (run_id, po_code)
);


-- ============================================================
-- 15. PSO ATTAINMENT
-- ============================================================

CREATE TABLE pso_attainments (
                                 id VARCHAR(50) PRIMARY KEY,

                                 run_id VARCHAR(50) NOT NULL
                                     REFERENCES calculation_runs(id) ON DELETE CASCADE,

                                 programme_id VARCHAR(50) NOT NULL
                                     REFERENCES programmes(id) ON DELETE CASCADE,

                                 batch_id VARCHAR(50) NOT NULL
                                     REFERENCES batches(id) ON DELETE CASCADE,

                                 pso_code VARCHAR(20) NOT NULL,

                                 direct_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                 indirect_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                 final_attainment NUMERIC(4,2) NOT NULL DEFAULT 0.00,

                                 target_attainment NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                 is_target_achieved BOOLEAN NOT NULL DEFAULT FALSE,

                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_run_pso
                                     UNIQUE (run_id, pso_code)
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_attainment_config_offering
    ON attainment_configurations(course_offering_id);

CREATE INDEX idx_marks_upload_offering
    ON end_sem_marks_uploads(course_offering_id);

CREATE INDEX idx_student_co_marks_offering
    ON student_co_marks(course_offering_id);

CREATE INDEX idx_student_co_marks_student
    ON student_co_marks(student_id);

CREATE INDEX idx_course_surveys_offering
    ON course_end_surveys(course_offering_id);

CREATE INDEX idx_programme_exit_survey_batch
    ON programme_exit_surveys(programme_id, batch_id);

CREATE INDEX idx_uploaded_docs_offering
    ON uploaded_documents(course_offering_id);

CREATE INDEX idx_uploaded_docs_batch
    ON uploaded_documents(batch_id);

CREATE INDEX idx_calc_runs_offering
    ON calculation_runs(course_offering_id);

CREATE INDEX idx_calc_runs_programme_batch
    ON calculation_runs(programme_id, batch_id);

CREATE INDEX idx_direct_co_attainment_offering
    ON direct_co_attainments(course_offering_id);

CREATE INDEX idx_indirect_co_attainment_offering
    ON indirect_co_attainments(course_offering_id);

CREATE INDEX idx_overall_co_attainment_offering
    ON overall_co_attainments(course_offering_id);

CREATE INDEX idx_po_attainment_programme_batch
    ON po_attainments(programme_id, batch_id);

CREATE INDEX idx_pso_attainment_programme_batch
    ON pso_attainments(programme_id, batch_id);