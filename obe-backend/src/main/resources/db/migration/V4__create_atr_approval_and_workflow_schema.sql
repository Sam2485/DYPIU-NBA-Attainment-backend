-- ============================================================
-- V4__create_atr_approval_and_workflow_schema.sql
--
-- Course ATR:
--   Course Coordinator creates
--   Programme Coordinator verifies
--
-- Programme ATR:
--   Programme Coordinator creates
--   HOD verifies
--
-- Also contains:
--   Approval workflow
--   Approval history
--   Setup progress tracking
--
-- NO INSERT statements.
-- NO UPDATE statements.
-- ============================================================


-- ============================================================
-- 1. COURSE ATR
-- ============================================================
--
-- One ATR row for each CO of a CourseOffering.
-- ============================================================

CREATE TABLE course_atrs (
                             id VARCHAR(50) PRIMARY KEY,

                             course_offering_id VARCHAR(50) NOT NULL
                                 REFERENCES course_offerings(id) ON DELETE CASCADE,

                             co_code VARCHAR(30) NOT NULL,

                             title VARCHAR(255),

                             target_score NUMERIC(4,2) NOT NULL,

                             actual_score NUMERIC(4,2) NOT NULL,

                             pct_achieved NUMERIC(5,2) NOT NULL,

                             status VARCHAR(50) NOT NULL,

                             statement TEXT,

                             actions_json TEXT,

                             submitted_by VARCHAR(150),

                             submitted_at TIMESTAMP WITH TIME ZONE,

                             verification_comments TEXT,

                             verified_at TIMESTAMP WITH TIME ZONE,

                             verified_by VARCHAR(150),

                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT uk_offering_co_atr
                                 UNIQUE (course_offering_id, co_code)
);


-- ============================================================
-- 2. PROGRAMME ATR
-- ============================================================
--
-- One Programme ATR per Programme + Batch.
--
-- Created by Programme Coordinator.
-- Verified by HOD.
-- ============================================================

CREATE TABLE programme_atrs (
                                id VARCHAR(50) PRIMARY KEY,

                                programme_id VARCHAR(50) NOT NULL
                                    REFERENCES programmes(id) ON DELETE CASCADE,

                                batch_id VARCHAR(50) NOT NULL
                                    REFERENCES batches(id) ON DELETE CASCADE,

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

                                CONSTRAINT uk_programme_batch_atr
                                    UNIQUE (programme_id, batch_id)
);


-- ============================================================
-- 3. APPROVAL REQUESTS
-- ============================================================
--
-- Generic approval mechanism.
--
-- resource_id identifies the resource being approved.
--
-- Examples:
--
-- COURSE_ATR
-- PROGRAMME_ATR
-- COURSE_CO_WEIGHTAGES
-- PROGRAMME_TARGETS
-- COURSE_ALLOCATION
-- ============================================================

CREATE TABLE approval_requests (
                                   id VARCHAR(50) PRIMARY KEY,

                                   type VARCHAR(50) NOT NULL,

                                   title VARCHAR(255) NOT NULL,

                                   resource_id VARCHAR(50) NOT NULL,

                                   school_id VARCHAR(50)
                                       REFERENCES schools(id) ON DELETE CASCADE,

                                   department_id VARCHAR(50)
                                       REFERENCES departments(id) ON DELETE CASCADE,

                                   programme_id VARCHAR(50)
                                       REFERENCES programmes(id) ON DELETE CASCADE,

                                   batch_id VARCHAR(50)
                                       REFERENCES batches(id) ON DELETE CASCADE,

                                   course_id VARCHAR(50)
                                       REFERENCES courses(id) ON DELETE CASCADE,

                                   course_offering_id VARCHAR(50)
                                       REFERENCES course_offerings(id) ON DELETE CASCADE,

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
-- 4. APPROVAL HISTORY
-- ============================================================

CREATE TABLE approval_history (
                                  id VARCHAR(50) PRIMARY KEY,

                                  approval_request_id VARCHAR(50) NOT NULL
                                      REFERENCES approval_requests(id) ON DELETE CASCADE,

                                  actor_id BIGINT
                                                                  REFERENCES users(id) ON DELETE SET NULL,

                                  actor_name VARCHAR(150) NOT NULL,

                                  actor_role VARCHAR(50) NOT NULL,

                                  action VARCHAR(50) NOT NULL,

                                  comments TEXT,

                                  timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 5. DIRECTOR SETUP PROGRESS
-- ============================================================

CREATE TABLE director_setup_progress (
                                         id VARCHAR(50) PRIMARY KEY,

                                         school_id VARCHAR(50) NOT NULL UNIQUE
                                             REFERENCES schools(id) ON DELETE CASCADE,

                                         current_step INTEGER NOT NULL DEFAULT 1,

                                         current_step_enum VARCHAR(30) NOT NULL DEFAULT 'SCHOOL',

                                         overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',

                                         completed_steps VARCHAR(500) DEFAULT '',

                                         pending_steps VARCHAR(500)
                                             DEFAULT 'school,department,programme,review',

                                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 6. HOD SETUP PROGRESS
-- ============================================================

CREATE TABLE hod_setup_progress (
                                    id VARCHAR(50) PRIMARY KEY,

                                    department_id VARCHAR(50) NOT NULL UNIQUE
                                        REFERENCES departments(id) ON DELETE CASCADE,

                                    hod_email VARCHAR(150),

                                    current_step INTEGER NOT NULL DEFAULT 1,

                                    overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',

                                    completed_steps VARCHAR(500) DEFAULT '',

                                    pending_steps VARCHAR(500)
                                        DEFAULT 'batch,outcomes,coordinators,review',

                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 7. PROGRAMME COORDINATOR SETUP PROGRESS
-- ============================================================

CREATE TABLE pc_setup_progress (
                                   id VARCHAR(50) PRIMARY KEY,

                                   programme_id VARCHAR(50) NOT NULL
                                       REFERENCES programmes(id) ON DELETE CASCADE,

                                   batch_id VARCHAR(50) NOT NULL
                                       REFERENCES batches(id) ON DELETE CASCADE,

                                   coordinator_email VARCHAR(150),

                                   current_step INTEGER NOT NULL DEFAULT 1,

                                   overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',

                                   completed_steps VARCHAR(500) DEFAULT '',

                                   pending_steps VARCHAR(500)
                                       DEFAULT 'courses,targets,review',

                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uk_pc_setup_programme_batch
                                       UNIQUE (programme_id, batch_id)
);


-- ============================================================
-- 8. COURSE COORDINATOR SETUP PROGRESS
-- ============================================================
--
-- IMPORTANT:
-- This is CourseOffering-level, not Course-level.
--
-- Same course can have different setup progress for different
-- batches.
-- ============================================================

CREATE TABLE cc_setup_progress (
                                   id VARCHAR(50) PRIMARY KEY,

                                   course_offering_id VARCHAR(50) NOT NULL UNIQUE
                                       REFERENCES course_offerings(id) ON DELETE CASCADE,

                                   coordinator_email VARCHAR(150),

                                   current_step INTEGER NOT NULL DEFAULT 1,

                                   overall_status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',

                                   completed_steps VARCHAR(500) DEFAULT '',

                                   pending_steps VARCHAR(500)
                                       DEFAULT 'cos,co_targets,co_mapping,direct,indirect,attainment,course_atr',

                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_course_atr_offering
    ON course_atrs(course_offering_id);

CREATE INDEX idx_programme_atr_programme_batch
    ON programme_atrs(programme_id, batch_id);

CREATE INDEX idx_approval_requests_status
    ON approval_requests(status);

CREATE INDEX idx_approval_requests_resource
    ON approval_requests(resource_id);

CREATE INDEX idx_approval_requests_programme_batch
    ON approval_requests(programme_id, batch_id);

CREATE INDEX idx_approval_requests_offering
    ON approval_requests(course_offering_id);

CREATE INDEX idx_approval_history_request
    ON approval_history(approval_request_id);

CREATE INDEX idx_director_setup_school
    ON director_setup_progress(school_id);

CREATE INDEX idx_hod_setup_department
    ON hod_setup_progress(department_id);

CREATE INDEX idx_pc_setup_programme_batch
    ON pc_setup_progress(programme_id, batch_id);

CREATE INDEX idx_cc_setup_offering
    ON cc_setup_progress(course_offering_id);