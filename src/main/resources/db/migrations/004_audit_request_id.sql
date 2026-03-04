-- 004_audit_request_id.sql
-- Add request_id correlation field to audit events

BEGIN;

ALTER TABLE audit_events
  ADD COLUMN IF NOT EXISTS request_id VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_audit_events_request_id
  ON audit_events(request_id);

COMMIT;
