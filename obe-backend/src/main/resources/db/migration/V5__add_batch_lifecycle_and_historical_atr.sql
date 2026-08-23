-- Add batch lifecycle editing window fields to programme_batches
ALTER TABLE programme_batches ADD COLUMN IF NOT EXISTS editing_window_until TIMESTAMP WITH TIME ZONE;
ALTER TABLE programme_batches ADD COLUMN IF NOT EXISTS editing_window_opened_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE programme_batches ADD COLUMN IF NOT EXISTS editing_window_opened_by VARCHAR(150);

