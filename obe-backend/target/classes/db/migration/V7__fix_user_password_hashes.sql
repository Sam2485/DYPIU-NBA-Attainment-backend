-- V7__fix_user_password_hashes.sql
-- Synchronize BCrypt password hashes for default seeded accounts to match "password123"

UPDATE users
SET password_hash = (SELECT password_hash FROM users WHERE username = 'testuser' LIMIT 1)
WHERE username IN ('director', 'hod_cse', 'pc_cse', 'faculty_raj', 'iqac_admin')
  AND (SELECT COUNT(*) FROM users WHERE username = 'testuser') > 0;
