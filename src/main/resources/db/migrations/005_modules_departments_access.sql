-- 005_modules_departments_access.sql
-- Enterprise authorization model: modules, departments, assignments and module-bound permissions

BEGIN;

CREATE TABLE IF NOT EXISTS modules (
  id          UUID PRIMARY KEY,
  code        VARCHAR(64) NOT NULL UNIQUE,
  name        VARCHAR(128) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS departments (
  id          UUID PRIMARY KEY,
  code        VARCHAR(64) NOT NULL UNIQUE,
  name        VARCHAR(128) NOT NULL,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_modules (
  user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  module_id    UUID NOT NULL REFERENCES modules(id) ON DELETE CASCADE,
  assigned_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, module_id)
);

CREATE TABLE IF NOT EXISTS user_departments (
  user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  department_id  UUID NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
  assigned_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, department_id)
);

CREATE INDEX IF NOT EXISTS idx_user_modules_module_id ON user_modules(module_id);
CREATE INDEX IF NOT EXISTS idx_user_departments_department_id ON user_departments(department_id);

ALTER TABLE permissions
  ADD COLUMN IF NOT EXISTS module_id UUID REFERENCES modules(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_permissions_module_id ON permissions(module_id);

-- Seed baseline modules
INSERT INTO modules (id, code, name)
VALUES
  ('c1c1c1c1-1111-1111-1111-111111111111', 'SALES', 'Sales'),
  ('c2c2c2c2-2222-2222-2222-222222222222', 'INVENTORY', 'Inventory'),
  ('c3c3c3c3-3333-3333-3333-333333333333', 'PURCHASING', 'Purchasing')
ON CONFLICT (code) DO NOTHING;

-- Seed baseline departments
INSERT INTO departments (id, code, name)
VALUES
  ('d1d1d1d1-1111-1111-1111-111111111111', 'HQ', 'Headquarters'),
  ('d2d2d2d2-2222-2222-2222-222222222222', 'SALES_OPS', 'Sales Operations')
ON CONFLICT (code) DO NOTHING;

-- Seed module permissions
INSERT INTO permissions (id, code, name, module_id)
SELECT '51111111-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'sales.order.read', 'Read sales orders', m.id
FROM modules m WHERE m.code = 'SALES'
ON CONFLICT (code) DO UPDATE SET module_id = EXCLUDED.module_id;

INSERT INTO permissions (id, code, name, module_id)
SELECT '52222222-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'sales.order.create', 'Create sales orders', m.id
FROM modules m WHERE m.code = 'SALES'
ON CONFLICT (code) DO UPDATE SET module_id = EXCLUDED.module_id;

INSERT INTO permissions (id, code, name, module_id)
SELECT '53333333-cccc-cccc-cccc-cccccccccccc', 'inventory.stock.read', 'Read stock', m.id
FROM modules m WHERE m.code = 'INVENTORY'
ON CONFLICT (code) DO UPDATE SET module_id = EXCLUDED.module_id;

INSERT INTO permissions (id, code, name, module_id)
SELECT '54444444-dddd-dddd-dddd-dddddddddddd', 'inventory.stock.adjust', 'Adjust stock', m.id
FROM modules m WHERE m.code = 'INVENTORY'
ON CONFLICT (code) DO UPDATE SET module_id = EXCLUDED.module_id;

-- Ensure existing admin.* permissions are global (module_id = null)
UPDATE permissions
SET module_id = NULL
WHERE code LIKE 'admin.%';

-- Grant new permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('sales.order.read', 'sales.order.create', 'inventory.stock.read', 'inventory.stock.adjust')
WHERE r.code = 'ADMIN'
ON CONFLICT DO NOTHING;

-- Optional baseline for USER role (read-only sales)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'sales.order.read'
WHERE r.code = 'USER'
ON CONFLICT DO NOTHING;

-- Assign baseline modules/departments to seeded admin user if present
INSERT INTO user_modules (user_id, module_id)
SELECT u.id, m.id
FROM users u
JOIN modules m ON m.code IN ('SALES', 'INVENTORY', 'PURCHASING')
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

INSERT INTO user_departments (user_id, department_id)
SELECT u.id, d.id
FROM users u
JOIN departments d ON d.code = 'HQ'
WHERE u.username = 'admin'
ON CONFLICT DO NOTHING;

COMMIT;
