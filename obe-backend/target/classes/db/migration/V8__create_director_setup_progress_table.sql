-- Migration V8: Create Director Setup Progress Tracking Table
CREATE TABLE IF NOT EXISTS director_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    school_id VARCHAR(50) NOT NULL UNIQUE,
    current_step INT NOT NULL DEFAULT 1,
    current_step_enum VARCHAR(30) NOT NULL DEFAULT 'SCHOOL',
    overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'school,department,programme,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Seed default setup progress for default school SET (sch-1)
INSERT INTO director_setup_progress (id, school_id, current_step, current_step_enum, overall_status, completed_steps, pending_steps)
VALUES ('progress-sch-1', 'sch-1', 1, 'SCHOOL', 'IN_PROGRESS', '', 'school,department,programme,review')
ON CONFLICT (school_id) DO NOTHING;
