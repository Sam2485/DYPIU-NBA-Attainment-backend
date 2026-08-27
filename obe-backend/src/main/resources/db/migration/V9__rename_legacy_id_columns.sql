-- Rename legacy 'programme_id' column in 'users' table to the canonical 'master_programme_id'
ALTER TABLE users RENAME COLUMN programme_id TO master_programme_id;

-- Drop the old index that referenced the old column name
DROP INDEX IF EXISTS idx_users_scope;

-- Recreate the index using the new column name
CREATE INDEX IF NOT EXISTS idx_users_scope ON users(school_id, department_id, master_programme_id);
