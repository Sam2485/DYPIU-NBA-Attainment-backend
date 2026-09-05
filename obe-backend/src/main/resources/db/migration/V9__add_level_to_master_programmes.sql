-- Migration V9: Add level column (UG/PG) to master_programmes for degree level filtering

ALTER TABLE master_programmes ADD COLUMN IF NOT EXISTS level VARCHAR(20) DEFAULT 'UG';

-- Ensure all existing rows have default 'UG'
UPDATE master_programmes SET level = 'UG' WHERE level IS NULL;

-- Optional index for level-based filtering
CREATE INDEX IF NOT EXISTS idx_master_programmes_level ON master_programmes(level) WHERE deleted_at IS NULL;
