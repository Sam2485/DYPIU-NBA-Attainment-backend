-- V15__create_course_mapping_keywords_table.sql
-- Store PO and PSO Keyword Mapping Store per Course

CREATE TABLE IF NOT EXISTS course_mapping_keywords (
    id VARCHAR(50) PRIMARY KEY,
    course_id VARCHAR(50) NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    keyword_type VARCHAR(20) NOT NULL, -- 'PO' or 'PSO'
    keywords_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_keyword_type UNIQUE (course_id, keyword_type)
);

CREATE INDEX IF NOT EXISTS idx_mapping_keywords_course ON course_mapping_keywords(course_id);
