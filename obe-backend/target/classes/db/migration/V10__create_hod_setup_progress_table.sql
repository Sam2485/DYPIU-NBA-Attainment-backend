-- Migration V10: Create HOD Setup Progress Tracking Table
CREATE TABLE IF NOT EXISTS hod_setup_progress (
    id VARCHAR(50) PRIMARY KEY,
    department_id VARCHAR(50) NOT NULL UNIQUE,
    hod_email VARCHAR(100),
    current_step INT NOT NULL DEFAULT 1,
    overall_status VARCHAR(30) NOT NULL DEFAULT 'IN_PROGRESS',
    completed_steps VARCHAR(500) DEFAULT '',
    pending_steps VARCHAR(500) DEFAULT 'batch,outcomes,coordinators,review',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
