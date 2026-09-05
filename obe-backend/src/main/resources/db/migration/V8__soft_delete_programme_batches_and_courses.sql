-- Migration V8: Fix unique constraints on programme_batches and programme_batch_courses for soft delete support

-- 1. Programme Batches: Drop global unique constraint on start_year and replace with partial unique index (active only)
ALTER TABLE programme_batches DROP CONSTRAINT IF EXISTS uk_programme_batch_start_year;
CREATE UNIQUE INDEX IF NOT EXISTS idx_programme_batches_start_year_active ON programme_batches(master_programme_id, start_year) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_programme_batches_name_active ON programme_batches(master_programme_id, name) WHERE deleted_at IS NULL;

-- 2. Programme Batch Courses: Drop global unique constraint and replace with partial unique index (active only)
ALTER TABLE programme_batch_courses DROP CONSTRAINT IF EXISTS uk_batch_course_sem;
CREATE UNIQUE INDEX IF NOT EXISTS idx_batch_course_sem_active ON programme_batch_courses(programme_batch_id, master_course_id, semester) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_batch_course_active ON programme_batch_courses(programme_batch_id, master_course_id) WHERE deleted_at IS NULL;
