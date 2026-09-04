-- ============================================================
-- V10__add_admin_user_sunil_dambhare.sql
--
-- Adds Admin User Sunil Dambhare with Bcrypt Password Hash
-- ============================================================

INSERT INTO users (username, email, password_hash, name, role, is_active, created_at, updated_at)
VALUES (
    'sunil.dambhare',
    'sunil.dambhare@dypiu.ac.in',
    '$2a$12$fSqLVm2Hhc12iCPcphiOyegDu0reQDr2f1QID23fHhIY6biNXHyfi',
    'Sunil Dambhare',
    'IQAC',
    TRUE,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    name = EXCLUDED.name,
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP;
