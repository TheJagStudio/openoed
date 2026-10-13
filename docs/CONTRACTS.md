# OpenOED API, storage and event contract

Base URL `http://localhost:8087`. Authenticated routes take `Authorization: Bearer <jwt>`.
Errors are RFC 9457 problem details. The platform stand-in listens on `http://localhost:8090`.

## Auth

| Method | Path | Notes |
|---|---|---|
| POST | `/api/v1/auth/register` | Always creates an APPLICANT. Returns access + refresh tokens. |
| POST | `/api/v1/auth/login` | Locks the account for 15 minutes after 5 failures. |
| POST | `/api/v1/auth/refresh` | Rotates the refresh token. Reuse of a revoked token revokes the family. |
| POST | `/api/v1/auth/logout` | Revokes the presented refresh token. |
| GET | `/api/v1/auth/me` | Current user. |

Access tokens are RS256, 15 minutes, claims `sub`, `email`, `name`, `role`, optional `artUnit`.

## Filings

`POST /api/v1/applications` is multipart: part `application` (JSON) and part `specimen` (PNG or JPEG, max 8 MB).
The response is the application detail, including the rules score. Bands: LOW 0–24, MEDIUM 25–49, HIGH 50–74, CRITICAL 75–100.
HIGH and CRITICAL open a priority review.

Applicants only see their own rows. Everyone else can read the docket. An examiner whose art unit differs from the filing is still allowed to open it; the audit item is flagged `ART_UNIT_MISMATCH`.

## Reviews, office actions, deadlines

| Method | Path | Who |
|---|---|---|
| GET | `/api/v1/reviews?status=OPEN` | OED, admin |
| POST | `/api/v1/reviews/{applicationId}` | body `{decision: CONFIRM\|CLEAR\|ASSIGN, notes}` |
| POST | `/api/v1/applications/{id}/office-actions` | examiner, admin. Body `{type, body}` |
| POST | `/api/v1/office-actions/{id}/responses` | owning applicant |
| POST | `/api/v1/office-actions/{id}/simulate-deadline` | staff, and only when `app.deadlines.simulation-enabled` |
| GET | `/api/v1/deadlines` | open office actions the caller may see |
| GET | `/api/v1/notices` | SES notices mirrored into Postgres |

Issuing an office action writes a `domain_outbox` row in the same transaction. A relay publishes it to SQS queue `office-actions`. The platform bridge creates an EventBridge one-shot named `oa-{id}` at `fireAt`. The statutory due date is always issued-at plus 6 months; `fireAt` is the local scheduler instant (`PT30M` in the dev profile, `PT2M` by default).

When the schedule fires, the Lambda calls:

* `POST /internal/v1/deadlines/evaluate` with header `X-Internal-Token`
* if the outcome is `ABANDONED`, SES send, then `POST /internal/v1/notices`

A response that arrived first yields `ALREADY_RESPONDED` and no mail. A second fire yields `ALREADY_CLOSED`.

## Audit ledger

Annotated controller methods are intercepted by `AuditAspect`. The item is appended to DynamoDB table `audit-ledger` (`pk = AUDIT#yyyy-MM-dd`, `sk = instant#uuid`). If the platform is down the same JSON is stored in `audit_outbox` and retried. `DELETE /dynamodb/tables/audit-ledger/items` returns 405.

`GET /api/v1/audit` is limited to IG, OED and admin.

## Platform stand-in

| Method | Path |
|---|---|
| POST | `/sqs/queues/{name}/messages` body `{body}` |
| GET | `/sqs/queues/{name}/messages?max=` |
| DELETE | `/sqs/queues/{name}/messages/{receipt}` |
| POST | `/schedules` `{name, fireAt, payload}` |
| POST | `/schedules/{name}/fire` |
| DELETE | `/schedules/{name}` |
| POST | `/ses/emails` |
| GET | `/ses/emails` |
| PUT | `/dynamodb/tables/audit-ledger/items` |
| POST | `/dynamodb/tables/audit-ledger/query` |
| GET | `/status` |
