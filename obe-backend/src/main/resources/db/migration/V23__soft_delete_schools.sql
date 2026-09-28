-- Migration V23: Add soft-delete columns to schools table and update unique constraint on code

ALTER TABLE schools ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255);

-- Drop unconditional unique constraint on code
ALTER TABLE schools DROP CONSTRAINT IF EXISTS schools_code_key;

-- Create partial unique index on code for active schools
CREATE UNIQUE INDEX IF NOT EXISTS idx_schools_code_active ON schools(code) WHERE deleted_at IS NULL;
