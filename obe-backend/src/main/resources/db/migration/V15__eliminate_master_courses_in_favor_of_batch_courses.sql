-- Migration V15: Transition ProgrammeBatchCourses to autonomous entity and decouple from master_courses

-- 1. Add direct course definition columns
ALTER TABLE programme_batch_courses 
    ADD COLUMN IF NOT EXISTS code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS credits INTEGER DEFAULT 3,
    ADD COLUMN IF NOT EXISTS course_type VARCHAR(50) DEFAULT 'THEORY';

-- 2. Populate code, name, credits, course_type from existing master_courses and overrides
UPDATE programme_batch_courses pbc
SET 
    code = COALESCE(pbc.course_code_override, mc.code, 'COURSE-' || pbc.id),
    name = COALESCE(pbc.course_name_override, mc.name, 'Course ' || pbc.id),
    credits = COALESCE(mc.credits, 3),
    course_type = COALESCE(mc.course_type, 'THEORY')
FROM master_courses mc
WHERE pbc.master_course_id = mc.id;

-- Fallback for any records without master_courses match
UPDATE programme_batch_courses
SET 
    code = COALESCE(code, course_code_override, 'COURSE-' || id),
    name = COALESCE(name, course_name_override, 'Course ' || id),
    credits = COALESCE(credits, 3),
    course_type = COALESCE(course_type, 'THEORY')
WHERE code IS NULL OR name IS NULL;

-- 3. Make master_course_id nullable
ALTER TABLE programme_batch_courses ALTER COLUMN master_course_id DROP NOT NULL;

-- 4. Drop legacy partial indexes on master_course_id
DROP INDEX IF EXISTS idx_batch_course_sem_active;
DROP INDEX IF EXISTS idx_batch_course_active;

-- 5. Create new unique active index on (programme_batch_id, code)
CREATE UNIQUE INDEX IF NOT EXISTS idx_pbc_batch_code_active 
    ON programme_batch_courses(programme_batch_id, code) 
    WHERE deleted_at IS NULL;
