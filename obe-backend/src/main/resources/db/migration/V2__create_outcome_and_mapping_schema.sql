-- ============================================================
-- V2__create_outcome_and_mapping_schema.sql
--
-- PO / PSO / PEO / CO framework
-- CO-PO / CO-PSO mappings
-- Programme targets
-- Course CO targets
-- Mapping keywords
--
-- Outcome definitions are master-level.
-- Attainment remains batch/offering-specific and is handled
-- in V3.
-- ============================================================


-- ============================================================
-- 1. PROGRAMME OUTCOMES
-- ============================================================

CREATE TABLE programme_outcomes (
                                    id VARCHAR(50) PRIMARY KEY,

                                    programme_id VARCHAR(50) NOT NULL
                                        REFERENCES programmes(id) ON DELETE CASCADE,

                                    code VARCHAR(20) NOT NULL,

                                    statement TEXT NOT NULL,

                                    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT uq_programme_po
                                        UNIQUE (programme_id, code)
);


-- ============================================================
-- 2. PO COMPETENCIES
-- ============================================================

CREATE TABLE po_competencies (
                                 id VARCHAR(50) PRIMARY KEY,

                                 po_id VARCHAR(50) NOT NULL
                                     REFERENCES programme_outcomes(id) ON DELETE CASCADE,

                                 code VARCHAR(30) NOT NULL,

                                 statement TEXT NOT NULL,

                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_po_competency
                                     UNIQUE (po_id, code)
);


-- ============================================================
-- 3. PROGRAMME SPECIFIC OUTCOMES
-- ============================================================

CREATE TABLE programme_specific_outcomes (
                                             id VARCHAR(50) PRIMARY KEY,

                                             programme_id VARCHAR(50) NOT NULL
                                                 REFERENCES programmes(id) ON DELETE CASCADE,

                                             code VARCHAR(20) NOT NULL,

                                             statement TEXT NOT NULL,

                                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                             CONSTRAINT uq_programme_pso
                                                 UNIQUE (programme_id, code)
);


-- ============================================================
-- 4. PSO COMPETENCIES
-- ============================================================

CREATE TABLE pso_competencies (
                                  id VARCHAR(50) PRIMARY KEY,

                                  pso_id VARCHAR(50) NOT NULL
                                      REFERENCES programme_specific_outcomes(id) ON DELETE CASCADE,

                                  code VARCHAR(30) NOT NULL,

                                  statement TEXT NOT NULL,

                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_pso_competency
                                      UNIQUE (pso_id, code)
);


-- ============================================================
-- 5. PEO OUTCOMES
-- ============================================================

CREATE TABLE peo_outcomes (
                              id VARCHAR(50) PRIMARY KEY,

                              programme_id VARCHAR(50) NOT NULL
                                  REFERENCES programmes(id) ON DELETE CASCADE,

                              code VARCHAR(20) NOT NULL,

                              statement TEXT NOT NULL,

                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT uq_programme_peo
                                  UNIQUE (programme_id, code)
);


-- ============================================================
-- 6. COURSE OUTCOMES
-- ============================================================
--
-- CO definitions belong to CourseOffering.
-- ============================================================

CREATE TABLE course_outcomes (
                                 id VARCHAR(50) PRIMARY KEY,

                                 course_offering_id VARCHAR(50) NOT NULL
                                     REFERENCES course_offerings(id) ON DELETE CASCADE,

                                 code VARCHAR(30) NOT NULL,

                                 statement TEXT NOT NULL,

                                 target_level NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uk_offering_co_code
                                     UNIQUE (course_offering_id, code),

                                 CONSTRAINT chk_co_target_level
                                     CHECK (target_level >= 0 AND target_level <= 3)
);


-- ============================================================
-- 7. CO-PO MAPPINGS
-- ============================================================

CREATE TABLE co_po_mappings (
                                id VARCHAR(50) PRIMARY KEY,

                                course_outcome_id VARCHAR(50) NOT NULL
                                     REFERENCES course_outcomes(id) ON DELETE CASCADE,

                                po_code VARCHAR(20) NOT NULL,

                                mapping_level INTEGER NOT NULL DEFAULT 0,

                                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT uq_co_po
                                    UNIQUE (course_outcome_id, po_code),

                                CONSTRAINT chk_co_po_mapping_level
                                    CHECK (mapping_level BETWEEN 0 AND 3)
);


-- ============================================================
-- 8. CO-PSO MAPPINGS
-- ============================================================

CREATE TABLE co_pso_mappings (
                                 id VARCHAR(50) PRIMARY KEY,

                                 course_outcome_id VARCHAR(50) NOT NULL
                                     REFERENCES course_outcomes(id) ON DELETE CASCADE,

                                 pso_code VARCHAR(20) NOT NULL,

                                 mapping_level INTEGER NOT NULL DEFAULT 0,

                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                 CONSTRAINT uq_co_pso
                                     UNIQUE (course_outcome_id, pso_code),

                                 CONSTRAINT chk_co_pso_mapping_level
                                     CHECK (mapping_level BETWEEN 0 AND 3)
);


-- ============================================================
-- 9. PROGRAMME TARGETS
-- ============================================================
--
-- PO1..PO12 / PSO1..PSO3
--
-- Target is defined at batch level.
-- ============================================================

CREATE TABLE programme_targets (
                                   id VARCHAR(50) PRIMARY KEY,

                                   batch_id VARCHAR(50) NOT NULL
                                       REFERENCES batches(id) ON DELETE CASCADE,

                                   outcome_type VARCHAR(10) NOT NULL DEFAULT 'PO',

                                   outcome_code VARCHAR(20) NOT NULL,

                                   target_value NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uq_batch_outcome_target
                                       UNIQUE (batch_id, outcome_type, outcome_code),

                                   CONSTRAINT chk_programme_target
                                       CHECK (target_value >= 0 AND target_value <= 3)
);


-- ============================================================
-- 10. COURSE CO TARGETS
-- ============================================================

CREATE TABLE course_co_targets (
                                   id VARCHAR(50) PRIMARY KEY,

                                   course_id VARCHAR(50) NOT NULL
                                       REFERENCES courses(id) ON DELETE CASCADE,

                                   co_code VARCHAR(30) NOT NULL,

                                   target_value NUMERIC(4,2) NOT NULL DEFAULT 2.50,

                                   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uq_course_co_target
                                       UNIQUE (course_id, co_code),

                                   CONSTRAINT chk_course_co_target
                                       CHECK (target_value >= 0 AND target_value <= 3)
);


-- ============================================================
-- 11. COURSE MAPPING KEYWORDS
-- ============================================================

CREATE TABLE course_mapping_keywords (
                                         id VARCHAR(50) PRIMARY KEY,

                                         course_offering_id VARCHAR(50) NOT NULL
                                             REFERENCES course_offerings(id) ON DELETE CASCADE,

                                         keyword_type VARCHAR(20) NOT NULL,

                                         keywords_json TEXT NOT NULL,

                                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

                                         CONSTRAINT uq_offering_keyword_type
                                             UNIQUE (course_offering_id, keyword_type),

                                         CONSTRAINT chk_keyword_type
                                             CHECK (keyword_type IN ('PO', 'PSO'))
);

CREATE INDEX idx_mapping_keywords_offering
    ON course_mapping_keywords(course_offering_id);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_programme_outcomes_programme
    ON programme_outcomes(programme_id);

CREATE INDEX idx_po_competencies_po
    ON po_competencies(po_id);

CREATE INDEX idx_programme_pso_programme
    ON programme_specific_outcomes(programme_id);

CREATE INDEX idx_pso_competencies_pso
    ON pso_competencies(pso_id);

CREATE INDEX idx_peo_outcomes_programme
    ON peo_outcomes(programme_id);

CREATE INDEX idx_course_outcomes_offering
    ON course_outcomes(course_offering_id);

CREATE INDEX idx_co_po_mapping_co
    ON co_po_mappings(course_outcome_id);

CREATE INDEX idx_co_pso_mapping_co
    ON co_pso_mappings(course_outcome_id);

CREATE INDEX idx_programme_targets_batch
    ON programme_targets(batch_id);

CREATE INDEX idx_course_co_targets_course
    ON course_co_targets(course_id);