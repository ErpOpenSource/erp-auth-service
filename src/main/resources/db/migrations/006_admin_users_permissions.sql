-- 006_admin_users_permissions.sql
-- Add admin user management permissions and grant them to ADMIN role

BEGIN;

INSERT INTO permissions (id, code, name)
VALUES
  ('61111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'admin.users.read', 'Read users'),
  ('62222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'admin.users.create', 'Create users')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('admin.users.read', 'admin.users.create')
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

COMMIT;
