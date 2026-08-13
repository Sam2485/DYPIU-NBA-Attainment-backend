-- V9__rename_dean_to_director_and_add_director_email.sql
ALTER TABLE schools RENAME COLUMN dean TO director;
ALTER TABLE schools RENAME COLUMN email TO director_email;
