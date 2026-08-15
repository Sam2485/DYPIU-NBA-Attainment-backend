-- V9__rename_dean_to_director_and_add_director_email.sql
DO $$ 
BEGIN 
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='schools' AND column_name='dean') THEN
    ALTER TABLE schools RENAME COLUMN dean TO director;
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='schools' AND column_name='email') THEN
    ALTER TABLE schools RENAME COLUMN email TO director_email;
  END IF;
END $$;
