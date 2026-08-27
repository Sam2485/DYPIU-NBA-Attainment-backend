-- Drop the globally unique partial index on master_programmes(code)
DROP INDEX IF EXISTS idx_master_programmes_code_active;

-- Drop the department-scoped partial index since uniqueness is school-scoped
DROP INDEX IF EXISTS idx_master_programmes_dept_code_active;

-- Optional: We do not necessarily need to create a school-scoped index at DB level 
-- if we rely on the service-level join validation, but creating one requires denormalization.
-- Since the user stated "service-level validation using a join to departments if changing the schema is not currently possible",
-- dropping the restrictive indexes allows the service validation to work without DB-level conflicts.
