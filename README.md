# erp-auth-service
Enterprise Authentication and Licensing service for ERP (single-tenant / on-prem).

## Stack
- Java 21
- Spring Boot 3.5
- PostgreSQL 16
- Docker / Docker Compose
- OpenTelemetry + JSON logs

## Quick Start (Docker)
1. Copy env template:
```bash
cp .env.example .env
```
2. Ensure external network exists (shared with your infra stack):
```bash
docker network create erp-platform
```
3. Build and run:
```bash
docker compose -f docker/docker-compose.yml up -d --build
```
4. Check health:
```bash
curl http://localhost:8081/actuator/health/readiness
```

## Environment Variables
Use `.env` (see `.env.example`):
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `AUTH_JWT_SECRET`
- `AUTH_JWT_ACCESS_TOKEN_EXPIRATION_MINUTES`
- `AUTH_REFRESH_EXPIRATION_DAYS_DEFAULT`
- `AUTH_REFRESH_EXPIRATION_DAYS_REMEMBER_ME`
- `AUTH_REFRESH_PEPPER`

## Migrations (Bytebase flow)
Migrations are SQL files under `src/main/resources/db/migrations`:
- `001_init.sql`
- `002_seed_dev_user.sql`
- `003_rbac.sql`
- `004_audit_request_id.sql`
- `005_modules_departments_access.sql`

Recommended process:
1. Apply migration in order (001 -> 005) using Bytebase.
2. Validate schema in DB.
3. Deploy service with `spring.jpa.hibernate.ddl-auto=validate`.

## API Summary
### Public
- `POST /login`
- `POST /refresh`

### Authenticated
- `POST /logout`
- `POST /logout/all` (current user from JWT `sub`)

### Admin (`ROLE_ADMIN`)
- `GET /admin/licenses/seats`
- `PUT /admin/licenses/seats`
- `GET /admin/sessions/active`
- `POST /admin/sessions/{id}/revoke`
- `POST /admin/users`
- `POST /admin/users/{id}/lock`
- `POST /admin/users/{id}/disable`
- `POST /admin/users/{id}/enable`
- `POST /admin/users/{id}/password/reset`
- `GET /admin/users/{id}/access`
- `POST /admin/modules`
- `POST /admin/departments`
- `POST /admin/permissions`
- `PUT /admin/users/{id}/modules`
- `PUT /admin/users/{id}/departments`
- `PUT /admin/roles/{code}/permissions`

### Observability
- `GET /actuator/health`
- `GET /actuator/health/readiness`
- `GET /actuator/health/liveness`
- `GET /actuator/metrics`
- `GET /actuator/prometheus`

## RBAC Notes
- `/admin/**` is protected with `ROLE_ADMIN`.
- JWT claims are loaded from DB, not from hardcoded username checks.
- Access token now includes: `roles`, `modules`, `departments`, `permissions`.
- Module-scoped permissions are filtered by user module assignment.
- Seeded dev `admin` gets `ADMIN` via migration `003_rbac.sql`.
- Migration `005` seeds baseline modules/departments and module permissions.

## Error Contract
All errors return JSON with:
- `code`
- `message`
- `requestId`
- `traceId`
- `details` (optional)

Security errors (`401`/`403`) also use the same JSON contract.

## Request Correlation
- Incoming `X-Request-Id` is propagated; if absent, service generates one.
- `X-Request-Id` is always returned in response.
- `requestId` and `traceId` are written to logs and persisted in `audit_events`.

## Local Testing
Run compile:
```bash
./mvnw -DskipTests compile -q
```

Run tests:
```bash
./mvnw test -q
```

Includes:
- refresh rotation/reuse tests
- security tests (`401/403`)
- RBAC admin access integration tests
- seat-enforcement concurrency integration test

## Troubleshooting
### `SCRAM-based authentication, but no password was provided`
- Check `.env` values and DB credentials.
- Ensure `POSTGRES_USER/POSTGRES_PASSWORD` match DB container.

### `Unable to determine Dialect`
- Validate datasource URL/user/password.
- Confirm DB container is reachable from `erp-auth-service`.

### `column ip is inet but expression is varchar`
- Ensure current entity mapping is deployed (`@ColumnTransformer(write = "?::inet")`).
- Re-apply schema/migrations if DB drift exists.

### `column metadata is jsonb but expression is varchar`
- Ensure audit metadata writes with JSON cast (`@ColumnTransformer(write = "?::jsonb")`).
- Confirm migration `001_init.sql` created `audit_events.metadata` as `jsonb`.

### `/admin/**` returns `403`
- Verify user has `ADMIN` role in `user_roles`.
- Re-login to issue new access token with updated `roles` claim.
