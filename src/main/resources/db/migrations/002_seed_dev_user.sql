-- DEV SEED USER (username: admin, password: admin1234)
INSERT INTO users (id, username, email, password_hash, status, created_at, updated_at)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'admin',
  NULL,
  '$2b$12$Ju5Q22chRCMFFz38c3SRlO4ZotSXItLyu3cZsERDT74WV9cTHzuPy',
  'ACTIVE',
  now(),
  now()
)
ON CONFLICT (username) DO NOTHING;