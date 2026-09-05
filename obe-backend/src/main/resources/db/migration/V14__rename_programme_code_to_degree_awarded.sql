-- Migration V14: Rename code column in master_programmes to degree_awarded

ALTER TABLE master_programmes RENAME COLUMN code TO degree_awarded;
ALTER TABLE master_programmes ALTER COLUMN degree_awarded TYPE VARCHAR(100);
