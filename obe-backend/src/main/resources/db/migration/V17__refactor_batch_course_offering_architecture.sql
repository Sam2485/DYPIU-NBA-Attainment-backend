-- V17__refactor_batch_course_offering_architecture.sql
-- Refactor Batch, Course Master, Course Offering, Course ATR, and Programme ATR Schema

-- 1. Update batches table: add previous_batch_id for sequential batch continuity
ALTER TABLE batches ADD COLUMN IF NOT EXISTS previous_batch_id VARCHAR(50) REFERENCES batches(id) ON DELETE SET NULL;

-- 2. Create course_offerings table to separate Course Master definitions from Batch-specific Offerings
CREATE TABLE IF NOT EXISTS course_offerings (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    batch_id VARCHAR(50) NOT NULL REFERENCES batches(id) ON DELETE CASCADE,
    semester INT NOT NULL DEFAULT 1,
    academic_year VARCHAR(20) NOT NULL DEFAULT '2025-26',
    course_coordinator_id VARCHAR(150),
    course_coordinator_name VARCHAR(150),
    assigned_faculty TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_course_sem UNIQUE (batch_id, course_id, semester)
);

-- 3. Update course_atrs table: add course_offering_id column
ALTER TABLE course_atrs ADD COLUMN IF NOT EXISTS course_offering_id VARCHAR(50) REFERENCES course_offerings(id) ON DELETE CASCADE;

-- 4. Update courses table: make semester and academic_year optional for master course definitions
ALTER TABLE courses ALTER COLUMN semester DROP NOT NULL;
ALTER TABLE courses ALTER COLUMN academic_year DROP NOT NULL;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS credits INT DEFAULT 4;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS course_type VARCHAR(50) DEFAULT 'CORE';

-- 5. Migrate existing courses to course_offerings if needed
INSERT INTO course_offerings (id, course_id, batch_id, semester, academic_year, course_coordinator_name, assigned_faculty, status)
SELECT 
    'offering-' || c.id || '-' || b.id,
    c.id,
    b.id,
    CASE 
        WHEN c.semester ~ '^\d+$' THEN CAST(c.semester AS INT)
        ELSE 1
    END,
    COALESCE(c.academic_year, '2025-26'),
    c.coordinator,
    c.assigned_faculty,
    'ACTIVE'
FROM courses c
CROSS JOIN batches b
ON CONFLICT (batch_id, course_id, semester) DO NOTHING;

-- 6. Add unique constraint for programme_atrs per batch
ALTER TABLE programme_atrs DROP CONSTRAINT IF EXISTS uk_programme_atr_batch;
ALTER TABLE programme_atrs ADD CONSTRAINT uk_programme_atr_batch UNIQUE (programme_id, batch_id);
