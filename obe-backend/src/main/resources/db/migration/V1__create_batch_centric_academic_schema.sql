-- ============================================================
-- V1__create_batch_centric_academic_schema.sql
--
-- Core academic hierarchy:
--
-- School
--   -> Department
--       -> Programme
--           -> Batch
--               -> Students
--               -> CourseOfferings
--                    -> Course
--
-- Course = master course definition
-- CourseOffering = batch/semester-specific course instance
--
-- IMPORTANT:
-- - Batch is the cohort identity.
-- - No academic_year is used as cohort identity.
-- - No seed INSERT statements.
-- - No UPDATE statements.
-- ============================================================


-- ============================================================
-- 1. SCHOOLS
-- ============================================================

CREATE TABLE schools (
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

CREATE TABLE departments (
                             id VARCHAR(50) PRIMARY KEY,

                             school_id VARCHAR(50) NOT NULL
                                 REFERENCES schools(id) ON DELETE CASCADE,

                             code VARCHAR(20) NOT NULL,
                             name VARCHAR(255) NOT NULL,

                             hod VARCHAR(150),
                             hod_email VARCHAR(150),

                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT uq_department_school_code
                                 UNIQUE (school_id, code)
);


-- ============================================================
-- 3. PROGRAMMES
-- ============================================================

CREATE TABLE programmes (
                            id VARCHAR(50) PRIMARY KEY,

                            department_id VARCHAR(50) NOT NULL
                                REFERENCES departments(id) ON DELETE CASCADE,

                            code VARCHAR(20) NOT NULL UNIQUE,
                            name VARCHAR(255) NOT NULL,

                            duration_years INTEGER NOT NULL DEFAULT 4,

                            department_name VARCHAR(255),

                            coordinator VARCHAR(150),
                            coordinator_email VARCHAR(150),

                            status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                            created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 9. USERS
-- ============================================================
--
-- Organizational scope:
--
-- IQAC              -> institution-wide
-- DIRECTOR          -> school
-- HOD               -> department
-- PROGRAMME_COORD.  -> programme
-- FACULTY           -> course offering(s)
-- ============================================================

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       username VARCHAR(100) NOT NULL UNIQUE,

                       email VARCHAR(150) NOT NULL UNIQUE,

                       password_hash VARCHAR(255) NOT NULL,

                       name VARCHAR(150) NOT NULL,

                       role VARCHAR(50) NOT NULL,

                       department VARCHAR(255),
                       programme VARCHAR(255),

                       school_id VARCHAR(50)
                           REFERENCES schools(id) ON DELETE SET NULL,

                       department_id VARCHAR(50)
                           REFERENCES departments(id) ON DELETE SET NULL,

                       programme_id VARCHAR(50)
                           REFERENCES programmes(id) ON DELETE SET NULL,

                       is_active BOOLEAN NOT NULL DEFAULT TRUE,

                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);



-- ============================================================
-- 4. BATCHES
-- ============================================================
--
-- Example:
--
-- start_year = 2025
-- end_year   = 2029
-- display name = Batch 2025-29
--
-- previous_batch_id provides historical continuity:
--
-- 2025-29 -> previous_batch_id -> 2024-28
-- ============================================================

CREATE TABLE batches (
                         id VARCHAR(50) PRIMARY KEY,

                         programme_id VARCHAR(50) NOT NULL
                             REFERENCES programmes(id) ON DELETE CASCADE,

                         programme_code VARCHAR(20),
                         programme_name VARCHAR(255),

                         duration_years INTEGER NOT NULL DEFAULT 4,

                         name VARCHAR(255) NOT NULL,

                         start_year INTEGER NOT NULL,
                         end_year INTEGER NOT NULL,

                         previous_batch_id VARCHAR(50)
                             REFERENCES batches(id) ON DELETE SET NULL,

                         year_level VARCHAR(100),

                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT chk_batch_year_range
                             CHECK (end_year > start_year),

                         CONSTRAINT chk_batch_duration
                             CHECK (end_year - start_year = duration_years)
);


-- ============================================================
-- 5. SEMESTERS
-- ============================================================
--
-- Semester belongs to a Batch.
--
-- No academic_year column.
--
-- Example:
-- Batch 2025-29
--   Semester 1
--   Semester 2
--   ...
--   Semester 8
-- ============================================================

CREATE TABLE semesters (
                           id VARCHAR(50) PRIMARY KEY,

                           batch_id VARCHAR(50) NOT NULL
                               REFERENCES batches(id) ON DELETE CASCADE,

                           semester_num INTEGER NOT NULL,

                           name VARCHAR(50) NOT NULL,

                           status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

                           created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT uq_batch_semester
                               UNIQUE (batch_id, semester_num),

                           CONSTRAINT chk_semester_number
                               CHECK (semester_num >= 1)
);


-- ============================================================
-- 6. COURSES
-- ============================================================
--
-- Course is the MASTER course definition.
--
-- Course does NOT contain:
--   - batch
--   - semester
--   - academic_year
--
-- Those belong to CourseOffering.
-- ============================================================
CREATE TABLE courses (
                         id VARCHAR(50) PRIMARY KEY,

                         code VARCHAR(50) NOT NULL,
                         name VARCHAR(255) NOT NULL,

                         programme_id VARCHAR(50) NOT NULL
                             REFERENCES programmes(id) ON DELETE CASCADE,

                         credits INTEGER NOT NULL DEFAULT 4,

                         course_type VARCHAR(50) NOT NULL DEFAULT 'CORE',

                         status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                         CONSTRAINT uq_course_programme_code
                             UNIQUE (programme_id, code)
);

-- ============================================================
-- 7. COURSE OFFERINGS
-- ============================================================
--
-- CourseOffering = Course + Batch + Semester
--
-- Example:
--
-- CS301
--   -> Batch 2024-28 / Semester 5
--   -> Batch 2025-29 / Semester 5
--
-- These are separate offerings.
-- ============================================================

CREATE TABLE course_offerings (
                                  id VARCHAR(50) PRIMARY KEY,

                                  course_id VARCHAR(50) NOT NULL
                                      REFERENCES courses(id) ON DELETE CASCADE,

                                  batch_id VARCHAR(50) NOT NULL
                                      REFERENCES batches(id) ON DELETE CASCADE,

                                  semester INTEGER NOT NULL,

                                  course_coordinator_id BIGINT
                                      REFERENCES users(id) ON DELETE SET NULL,

                                  course_coordinator_name VARCHAR(255),

                                  assigned_faculty TEXT,

                                  status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uk_batch_course_sem
                                      UNIQUE (batch_id, course_id, semester),

                                  CONSTRAINT chk_offering_semester
                                      CHECK (semester >= 1)
);

-- ============================================================
-- 8. STUDENTS
-- ============================================================

CREATE TABLE students (
                          id VARCHAR(50) PRIMARY KEY,

                          batch_id VARCHAR(50) NOT NULL
                              REFERENCES batches(id) ON DELETE CASCADE,

                          prn VARCHAR(50) NOT NULL UNIQUE,

                          name VARCHAR(150) NOT NULL,
                          email VARCHAR(150) NOT NULL,

                          status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',

                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);



-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_departments_school
    ON departments(school_id);

CREATE INDEX idx_programmes_department
    ON programmes(department_id);

CREATE INDEX idx_batches_programme
    ON batches(programme_id);

CREATE INDEX idx_batches_previous
    ON batches(previous_batch_id);

CREATE INDEX idx_semesters_batch
    ON semesters(batch_id);

CREATE INDEX idx_courses_programme
    ON courses(programme_id);

CREATE INDEX idx_course_offerings_course
    ON course_offerings(course_id);

CREATE INDEX idx_course_offerings_batch
    ON course_offerings(batch_id);

CREATE INDEX idx_course_offerings_coordinator
    ON course_offerings(course_coordinator_id);

CREATE INDEX idx_students_batch
    ON students(batch_id);

CREATE INDEX idx_users_username
    ON users(username);

CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_users_scope
    ON users(school_id, department_id, programme_id);