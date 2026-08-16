-- V16__add_metadata_to_uploaded_documents.sql
-- Add audit tracking metadata columns to uploaded_documents table

ALTER TABLE uploaded_documents 
ADD COLUMN IF NOT EXISTS programme_id VARCHAR(255),
ADD COLUMN IF NOT EXISTS batch_name VARCHAR(100),
ADD COLUMN IF NOT EXISTS uploaded_by VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_uploaded_docs_prog_batch ON uploaded_documents(programme_id, batch_name);
