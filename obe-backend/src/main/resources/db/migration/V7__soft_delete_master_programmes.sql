-- Migration to add soft-delete to master_programmes and fix unique constraints

ALTER TABLE master_programmes ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE master_programmes ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);

-- Drop existing non-partial unique constraints
ALTER TABLE master_programmes DROP CONSTRAINT IF EXISTS master_programmes_code_key;
ALTER TABLE master_programmes DROP CONSTRAINT IF EXISTS uk_department_programme_code;

-- Create partial unique indexes (active records only)
CREATE UNIQUE INDEX IF NOT EXISTS idx_master_programmes_code_active ON master_programmes(code) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_master_programmes_dept_code_active ON master_programmes(department_id, code) WHERE deleted_at IS NULL;
