-- V1__init_academic_and_user_schema.sql
-- Academic Management & User/Access Control Schema

CREATE TABLE IF NOT EXISTS schools (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    director VARCHAR(150),
    est_year VARCHAR(10),
    director_email VARCHAR(150),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS departments (
    id VARCHAR(50) PRIMARY KEY,
    school_id VARCHAR(50) NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    hod VARCHAR(150),
    hod_email VARCHAR(150),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_school_code UNIQUE (school_id, code)
);

CREATE TABLE IF NOT EXISTS programmes (
    id VARCHAR(50) PRIMARY KEY,
    department_id VARCHAR(50) NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    duration_years INT NOT NULL DEFAULT 4,
    department_name VARCHAR(255),
    coordinator VARCHAR(150),
    coordinator_email VARCHAR(150),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS academic_years (
    id VARCHAR(50) PRIMARY KEY,
    year_name VARCHAR(20) NOT NULL UNIQUE, -- e.g. "2025-26"
    start_date DATE,
    end_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS batches (
    id VARCHAR(50) PRIMARY KEY,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    programme_code VARCHAR(20) NOT NULL,
    programme_name VARCHAR(255) NOT NULL,
    duration_years INT NOT NULL DEFAULT 4,
    name VARCHAR(255) NOT NULL, -- e.g. "Batch 2025-29 (BE-COMP)"
    start_year VARCHAR(20) NOT NULL, -- "2025-26"
    end_year VARCHAR(20) NOT NULL, -- "2028-29"
    year_level VARCHAR(100), -- "Year 1 (Freshmen)"
    status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INITIALIZED, GRADUATED
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS semesters (
    id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    semester_num INT NOT NULL, -- 1 to 8
    name VARCHAR(50) NOT NULL, -- "Sem I", "Sem II"
    academic_year VARCHAR(20) NOT NULL, -- "2025-26"
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS courses (
    id VARCHAR(50) PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    programme_id VARCHAR(50) NOT NULL REFERENCES programmes(id) ON DELETE CASCADE,
    semester VARCHAR(50) NOT NULL,
    coordinator VARCHAR(150),
    faculty VARCHAR(255),
    assigned_faculty TEXT, -- Comma-separated or JSON array of assigned faculty
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(50) PRIMARY KEY,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    prn VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    status VARCHAR(20) DEFAULT 'ENROLLED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL, -- IQAC, DIRECTOR, HOD, PROGRAMME_COORDINATOR, FACULTY
    department VARCHAR(255),
    programme VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for fast query resolution
CREATE INDEX IF NOT EXISTS idx_departments_school ON departments(school_id);
CREATE INDEX IF NOT EXISTS idx_programmes_dept ON programmes(department_id);
CREATE INDEX IF NOT EXISTS idx_batches_prog ON batches(programme_id);
CREATE INDEX IF NOT EXISTS idx_semesters_batch ON semesters(batch_id);
CREATE INDEX IF NOT EXISTS idx_courses_prog ON courses(programme_id);
CREATE INDEX IF NOT EXISTS idx_students_batch ON students(batch_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
