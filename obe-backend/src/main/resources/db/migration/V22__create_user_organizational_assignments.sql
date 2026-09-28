-- ============================================================
-- V22__create_user_organizational_assignments.sql
-- Create normalized organizational assignments table to support
-- 1:N user-to-school and user-to-department assignments while
-- preserving existing users table fields for backward compatibility.
-- ============================================================

-- 1. Create normalized user_organizational_assignments table
CREATE TABLE IF NOT EXISTS user_organizational_assignments (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    school_id VARCHAR(50) REFERENCES schools(id) ON DELETE CASCADE,
    department_id VARCHAR(50) REFERENCES departments(id) ON DELETE CASCADE,
    master_programme_id VARCHAR(50) REFERENCES master_programmes(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_org_assignment UNIQUE NULLS NOT DISTINCT (
        user_id,
        role,
        school_id,
        department_id,
        master_programme_id
    )
);

-- 2. Performance indexes for scope verification and user directory lookups
CREATE INDEX IF NOT EXISTS idx_uoa_user_id ON user_organizational_assignments(user_id);
CREATE INDEX IF NOT EXISTS idx_uoa_school_id ON user_organizational_assignments(school_id);
CREATE INDEX IF NOT EXISTS idx_uoa_dept_id ON user_organizational_assignments(department_id);
CREATE INDEX IF NOT EXISTS idx_uoa_prog_id ON user_organizational_assignments(master_programme_id);
CREATE INDEX IF NOT EXISTS idx_uoa_role ON user_organizational_assignments(role);

-- 3. Backfill existing non-IQAC users with valid organizational context
INSERT INTO user_organizational_assignments (user_id, role, school_id, department_id, master_programme_id)
SELECT id, role, school_id, department_id, master_programme_id
FROM users
WHERE role NOT IN ('IQAC', 'ADMIN') 
  AND (school_id IS NOT NULL OR department_id IS NOT NULL OR master_programme_id IS NOT NULL)
ON CONFLICT (user_id, role, school_id, department_id, master_programme_id) DO NOTHING;

-- 4. Backfill Director assignments from schools table by director_email
INSERT INTO user_organizational_assignments (user_id, role, school_id, department_id, master_programme_id)
SELECT u.id, 'DIRECTOR', s.id, NULL, NULL
FROM schools s
JOIN users u ON LOWER(TRIM(u.email)) = LOWER(TRIM(s.director_email))
WHERE s.director_email IS NOT NULL AND TRIM(s.director_email) != ''
ON CONFLICT (user_id, role, school_id, department_id, master_programme_id) DO NOTHING;

-- 5. Backfill Director assignments from schools table by director_id
INSERT INTO user_organizational_assignments (user_id, role, school_id, department_id, master_programme_id)
SELECT u.id, 'DIRECTOR', s.id, NULL, NULL
FROM schools s
JOIN users u ON u.id = s.director_id
WHERE s.director_id IS NOT NULL
ON CONFLICT (user_id, role, school_id, department_id, master_programme_id) DO NOTHING;

-- 6. Backfill HOD assignments from departments table by hod_email
INSERT INTO user_organizational_assignments (user_id, role, school_id, department_id, master_programme_id)
SELECT u.id, 'HOD', d.school_id, d.id, NULL
FROM departments d
JOIN users u ON LOWER(TRIM(u.email)) = LOWER(TRIM(d.hod_email))
WHERE d.hod_email IS NOT NULL AND TRIM(d.hod_email) != ''
ON CONFLICT (user_id, role, school_id, department_id, master_programme_id) DO NOTHING;
