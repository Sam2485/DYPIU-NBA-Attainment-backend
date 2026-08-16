-- V13__create_uploaded_documents_table.sql
CREATE TABLE IF NOT EXISTS uploaded_documents (
    id VARCHAR(255) PRIMARY KEY,
    course_id VARCHAR(255) NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    saved_file_name VARCHAR(255) NOT NULL,
    saved_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    records_processed INT,
    threshold_percentage DECIMAL(5,2),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_uploaded_docs_course_type ON uploaded_documents(course_id, document_type);
