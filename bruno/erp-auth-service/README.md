# Bruno Collection - ERP Auth Service

## Included Requests
- `Auth/Login`
- `Auth/Refresh`
- `Auth/Logout`
- `Auth/LogoutAll`
- `Admin/GetSeats`
- `Admin/UpdateSeats`
- `Admin/GetActiveSessions`

## Usage
1. Open Bruno and import folder: `bruno/erp-auth-service`.
2. Select environment: `local`.
3. Run in this order:
   - `Auth/Login`
   - `Auth/Refresh` (optional)
   - `Admin/*` requests (requires `ROLE_ADMIN` in DB)
   - `Auth/Logout` or `Auth/LogoutAll`

The collection auto-saves `accessToken`, `refreshToken` and `sessionId` from `Login/Refresh`.

## Correlation Header
Every request sends `X-Request-Id` (generated in pre-request script).  
This aligns with service correlation (`requestId` in logs and `audit_events.request_id`).

## Error Contract (reference)
When a request fails, API returns JSON:
- `code`
- `message`
- `requestId`
- `traceId`
- `details` (optional)

Common examples:
- `UNAUTHORIZED` (401)
- `ACCESS_DENIED` (403)
- `SEAT_LIMIT_REACHED` (409)
- `VALIDATION_ERROR` (400)
- `INTERNAL_ERROR` (500)
