## 📌 Description
<!-- What does this PR do? Why? -->

## 🔍 Type of change
- [ ] Feature
- [ ] Bugfix
- [ ] Refactor
- [ ] Performance
- [ ] Security
- [ ] Documentation
- [ ] Build/CI

## 🧪 How to test
<!-- Provide steps or evidence (logs, screenshots, curl commands) -->

## 🗃️ Database changes (Bytebase)
- [ ] No DB changes
- [ ] DB change included — migration script added in `db/migrations/`
  - Script name: `XXX_<name>.sql`
  - Bytebase rollout plan described

## 📎 Related Issues / Tickets
<!-- Link issue(s) if any -->

## ✅ Checklist
- [ ] Scope is small and focused
- [ ] No business logic inside controllers
- [ ] Error codes are consistent (e.g., SEAT_LIMIT_REACHED)
- [ ] Observability aligned: logs JSON + X-Request-Id propagation
- [ ] Security considerations reviewed (tokens, sessions, hashing)
- [ ] Docs updated if needed