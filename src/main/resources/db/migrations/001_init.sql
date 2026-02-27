-- 001_init.sql
-- ERP Auth Service - V1 schema
-- PostgreSQL

BEGIN;

-- Optional: dedicated schema (si prefieres schema auth dentro del DB)
-- CREATE SCHEMA IF NOT EXISTS auth;
-- SET search_path TO auth;

-- ================
-- ENUMS / TYPES
-- ================

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
    CREATE TYPE user_status AS ENUM ('ACTIVE', 'LOCKED', 'DISABLED');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'enforce_mode') THEN
    CREATE TYPE enforce_mode AS ENUM ('HARD', 'SOFT');
  END IF;
END$$;

-- ================
-- USERS
-- ================
CREATE TABLE IF NOT EXISTS users (
  id                UUID PRIMARY KEY,
  username          VARCHAR(120) NOT NULL,
  email             VARCHAR(255),
  password_hash     TEXT NOT NULL,
  status            user_status NOT NULL DEFAULT 'ACTIVE',
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_users_username UNIQUE (username),
  CONSTRAINT uq_users_email UNIQUE (email)
);

-- ================
-- LICENSE SEATS (single row)
-- ================
CREATE TABLE IF NOT EXISTS license_seats (
  id                    VARCHAR(32) PRIMARY KEY,   -- e.g. 'MAIN'
  max_concurrent_seats   INTEGER NOT NULL CHECK (max_concurrent_seats >= 0),
  enforce_mode           enforce_mode NOT NULL DEFAULT 'HARD',
  updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
  version                BIGINT NOT NULL DEFAULT 0
);

-- Seed single row if not exists
INSERT INTO license_seats (id, max_concurrent_seats, enforce_mode)
VALUES ('MAIN', 10, 'HARD')
ON CONFLICT (id) DO NOTHING;

-- ================
-- SESSIONS
-- ================
CREATE TABLE IF NOT EXISTS sessions (
  id                       UUID PRIMARY KEY,
  user_id                   UUID NOT NULL REFERENCES users(id),
  device_id                 VARCHAR(128) NOT NULL,
  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_seen_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- Active rule: revoked_at IS NULL AND expires_at > now()
  expires_at                TIMESTAMPTZ NOT NULL,
  revoked_at                TIMESTAMPTZ,

  -- Refresh token rotation (store hashes only)
  refresh_token_hash        TEXT NOT NULL,
  prev_refresh_token_hash   TEXT,

  ip                        INET,
  user_agent                TEXT
);

-- Helpful indexes
CREATE INDEX IF NOT EXISTS idx_sessions_user_id ON sessions(user_id);

-- Fast count of actives (partial index)
CREATE INDEX IF NOT EXISTS idx_sessions_active
ON sessions (expires_at)
WHERE revoked_at IS NULL;

-- Lookup by refresh hash (used during refresh flow)
CREATE INDEX IF NOT EXISTS idx_sessions_refresh_hash
ON sessions (refresh_token_hash);

-- ================
-- AUDIT EVENTS
-- ================
CREATE TABLE IF NOT EXISTS audit_events (
  id              UUID PRIMARY KEY,
  type            VARCHAR(64) NOT NULL,
  actor_user_id   UUID REFERENCES users(id),
  target_user_id  UUID REFERENCES users(id),
  session_id      UUID REFERENCES sessions(id),
  timestamp       TIMESTAMPTZ NOT NULL DEFAULT now(),

  -- correlate with gateway: X-Request-Id (and/or trace id)
  trace_id        VARCHAR(128),

  metadata        JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_audit_events_timestamp ON audit_events(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_type ON audit_events(type);
CREATE INDEX IF NOT EXISTS idx_audit_events_actor ON audit_events(actor_user_id);

COMMIT;