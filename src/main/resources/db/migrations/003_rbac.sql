-- 003_rbac.sql
-- Base RBAC model for admin/user authorization

BEGIN;

CREATE TABLE IF NOT EXISTS roles (
  id          UUID PRIMARY KEY,
  code        VARCHAR(64) NOT NULL UNIQUE,
  name        VARCHAR(128) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS permissions (
  id          UUID PRIMARY KEY,
  code        VARCHAR(128) NOT NULL UNIQUE,
  name        VARCHAR(128) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_id      UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
  permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
  assigned_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (role_id, permission_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission_id ON role_permissions(permission_id);

-- Seed baseline roles
INSERT INTO roles (id, code, name)
VALUES
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN', 'Administrator'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'USER', 'Standard User')
ON CONFLICT (code) DO NOTHING;

-- Seed baseline permissions (starter set)
INSERT INTO permissions (id, code, name)
VALUES
  ('11111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'admin.sessions.read', 'Read active sessions'),
  ('22222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'admin.sessions.revoke', 'Revoke sessions'),
  ('33333333-cccc-cccc-cccc-cccccccccccc', 'admin.seats.read', 'Read license seats')
ON CONFLICT (code) DO NOTHING;

-- Map baseline permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Grant ADMIN to seeded dev admin user if present
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.code = 'ADMIN'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

COMMIT;
