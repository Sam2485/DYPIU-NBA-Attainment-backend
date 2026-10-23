-- ============================================================
-- Migration V25: Add employee_id to users table
-- ============================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id VARCHAR(50);
CREATE INDEX IF NOT EXISTS idx_users_employee_id ON users(employee_id);
