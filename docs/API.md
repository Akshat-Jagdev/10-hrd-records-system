# API reference

Interactive docs: **Swagger UI** at `http://localhost:8080/swagger-ui.html`
(OpenAPI JSON at `/v3/api-docs`). All endpoints are prefixed `/api/v1`.
Errors are RFC 7807 `application/problem+json` with a human-readable `detail`.

## Authentication

`POST /auth/login` with `{"username", "password"}` returns
`{"token", "tokenType": "Bearer", "expiresIn", "user"}`. Send the token on every
call: `Authorization: Bearer <token>`. `GET /auth/me` returns the current user.

## RBAC matrix

| Endpoint | ADMIN | HR_MANAGER | VIEWER | AUDITOR |
|---|:--:|:--:|:--:|:--:|
| `POST /auth/login`, `GET /auth/me` | ✅ | ✅ | ✅ | ✅ |
| `GET /dashboard` | ✅ | ✅ | ✅ | ✅ |
| `GET /employees`, `/departments`, `/production`, `/incentives/*` (reads) | ✅ | ✅ | ✅ | ✅ |
| `POST/PUT/DELETE /employees`, `/departments`, `/production` | ✅ | ✅ | ❌ 403 | ❌ 403 |
| `POST/PUT/DELETE /incentives/schemes` | ✅ | ✅ | ❌ 403 | ❌ 403 |
| `POST /incentives/earnings/calculate`, `/approve`, `/reject` | ✅ | ✅ | ❌ 403 | ❌ 403 |
| `GET /audit` | ✅ | ✅ | ❌ 403 | ✅ |
| `GET/POST/PUT/DELETE /admin/users` | ✅ | ❌ 403 | ❌ 403 | ❌ 403 |

Unauthenticated calls get `401`; authenticated-but-not-allowed get `403`.
Business-rule violations (duplicate code, department in use, approving a non-draft,
touching the last enabled ADMIN…) get `409 Conflict` with the reason in `detail`.

## Endpoints

### Users (ADMIN) — `/admin/users`
- `GET /admin/users` — list
- `POST /admin/users` — `{username, password (>=8), fullName, role}` → 201
- `PUT /admin/users/{id}` — `{fullName, role, enabled}`
- `DELETE /admin/users/{id}` — 204; refuses self-delete and last enabled ADMIN

### Departments — `/departments`
- `GET /departments` — list (authenticated)
- `POST /departments` — `{code, name, type: PRODUCTION|SUPPORT}` (ADMIN/HR_MANAGER)
- `PUT /departments/{id}`, `DELETE /departments/{id}` — delete refuses departments
  with employees or production records

### Employees — `/employees`
- `GET /employees?empNo=&name=&departmentId=&status=&page=&size=` — paged search
  (`PageResponse` envelope: `content, page, size, totalElements, totalPages`)
- `POST /employees` — `{empNo, fullName, grade, designation, departmentId, doj,
  status, basicPay}`; `PUT /employees/{id}`; `DELETE /employees/{id}` (refuses
  employees with incentive records)

### Production — `/production`
- `GET /production?departmentId=&year=&month=&page=&size=` — paged; each row carries
  computed `achievementPct`
- `POST /production` — `{departmentId, year, month, plannedTonnes, actualTonnes}` —
  one record per (department, year, month); duplicates → 409
- `PUT /production/{id}`, `DELETE /production/{id}`

### Incentives — `/incentives`
- `GET /incentives/schemes` — list with grade rates
- `POST /incentives/schemes` — `{name, description, thresholdPct, ceilingPct,
  ratePerPoint, active, effectiveFrom, effectiveTo, gradeRates: [{grade, factor}]}`
  (ceiling >= threshold enforced)
- `PUT /incentives/schemes/{id}`, `DELETE /incentives/schemes/{id}` (refuses schemes
  with earnings)
- `GET /incentives/earnings?employeeId=&schemeId=&year=&month=&status=&page=&size=`
- `POST /incentives/earnings/calculate` — `{schemeId, departmentId, year, month}` —
  deletes existing DRAFT rows for that cell and regenerates them from the production
  record (404 if no production record; 409 if the scheme is not active for the period)
- `POST /incentives/earnings/{id}/approve` · `/reject` — DRAFT only

### Audit — `/audit` (ADMIN / HR_MANAGER / AUDITOR)
- `GET /audit?entity=&username=&action=CREATE|UPDATE|DELETE&from=&to=&page=&size=`
  — latest first; entries include jsonb `oldValue`/`newValue` snapshots

### Dashboard — `/dashboard` (any authenticated)
- `GET /dashboard` — active/total employees, department counts, pending incentive
  drafts, last-12-period production trend, 5 most recent audit entries

## curl quickstart

```bash
BASE=http://localhost:8080/api/v1

# login as HR manager (demo credentials)
TOKEN=$(curl -s $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"hr.manager","password":"HrManager@HRD-Demo"}' | jq -r .token)

# list employees
curl -s $BASE/employees -H "Authorization: Bearer $TOKEN" | jq .

# create a production record
curl -s $BASE/production -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"departmentId":1,"year":2026,"month":1,"plannedTonnes":42000,"actualTonnes":43500}' | jq .

# calculate + approve incentives
curl -s -X POST $BASE/incentives/earnings/calculate -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"schemeId":1,"departmentId":1,"year":2026,"month":1}' | jq .

# a VIEWER token on the same call → 403
```
