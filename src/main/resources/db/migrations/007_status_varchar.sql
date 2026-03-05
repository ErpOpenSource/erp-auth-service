-- 007_status_varchar.sql
-- Convert user_status custom PG enum to VARCHAR(16) so Hibernate can INSERT correctly.
-- PostgreSQL requires an explicit cast when using custom enum types via JDBC; VARCHAR avoids this.

BEGIN;

-- Must drop the DEFAULT first (it references the custom type)
ALTER TABLE users ALTER COLUMN status DROP DEFAULT;

-- Convert enum column to plain VARCHAR
ALTER TABLE users ALTER COLUMN status TYPE VARCHAR(16) USING status::text;

-- Restore default as plain string
ALTER TABLE users ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- Add CHECK constraint to enforce valid values
ALTER TABLE users ADD CONSTRAINT users_status_check
    CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED'));

-- Drop the now-unused custom enum type
DROP TYPE IF EXISTS user_status CASCADE;

COMMIT;
