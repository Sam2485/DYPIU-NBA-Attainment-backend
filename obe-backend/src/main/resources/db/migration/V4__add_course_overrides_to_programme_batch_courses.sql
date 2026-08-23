-- V4: Add course code and course name override fields to programme_batch_courses table
ALTER TABLE programme_batch_courses ADD COLUMN IF NOT EXISTS course_code_override VARCHAR(50);
ALTER TABLE programme_batch_courses ADD COLUMN IF NOT EXISTS course_name_override VARCHAR(255);
