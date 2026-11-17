# Architecture

HRD Records System is a small, deliberately boring, production-shaped platform for
managing steel-plant HR and production records — the kind of system a plant's HR and
production departments actually run on, rebuilt here as a portfolio project with
**synthetic data only** (inspired by, and not affiliated with, a steel-plant internship).

## Stack

| Layer | Choice |
|---|---|
| Backend | Java 17, Spring Boot 3.5 (Web, Data JPA, Security, Validation, Actuator) |
| Database | PostgreSQL 16 (Flyway migrations, `ddl-auto=validate`) |
| Auth | Stateless JWT (HS256, jjwt 0.12), BCrypt passwords |
| API docs | springdoc-openapi (Swagger UI at `/swagger-ui.html`) |
| Tests | JUnit 5 + Mockito (unit), Testcontainers PostgreSQL (integration) |
| Frontend | React 19 + TypeScript, Vite, axios, react-router-dom, hand-rolled CSS |

## Modules (`com.hrd.records`)

```
auth/        login + token issuing, /api/v1/auth
user/        user administration (ADMIN only), /api/v1/admin/users
department/  departments (PRODUCTION/SUPPORT), /api/v1/departments
employee/    employee master data + search, /api/v1/employees
production/  monthly planned/actual tonnes per department, /api/v1/production
incentive/   schemes, grade rates, draft calculation, approve/reject, /api/v1/incentives
audit/       append-only mutation trail (jsonb before/after), /api/v1/audit
dashboard/   read-only aggregations, /api/v1/dashboard
security/    JWT issue/verify filter, UserPrincipal, error handlers
common/      RFC 7807 problem+json handler, PageResponse envelope, exceptions
config/      Security, CORS, OpenAPI
```

## Request flow

```
Browser ──JWT──▶ JwtAuthFilter ──▶ SecurityFilterChain (route rules)
                                   ──▶ @PreAuthorize (role rules on controllers)
                                        ──▶ Service (@Transactional, guards)
                                             ├─▶ Repository (JPA + Specifications)
                                             └─▶ AuditService.record(before, after)
                                                  └─▶ audit_log (jsonb snapshots)
```

- **Route-level rules** (`SecurityConfig`): everything requires authentication except
  `/api/v1/auth/login`, Swagger, and health/info endpoints.
- **Method-level rules** (`@PreAuthorize`): business writes require
  `ADMIN` or `HR_MANAGER`; the audit trail additionally serves `AUDITOR`; user
  administration is `ADMIN`-only. See [API.md](API.md) for the full RBAC matrix.
- **Guards in services** protect invariants the roles can't express: no self-delete,
  never demote/disable/delete the last enabled `ADMIN`, no deleting departments or
  employees that still have dependent records (409 with a clear message).

## The incentive engine

`IncentiveCalculator` is pure, side-effect-free logic — the one part of the domain
worth unit-testing to death:

```
achievementPct = actual / planned * 100
effectivePoints = min(achievementPct, ceilingPct) - thresholdPct
amount = effectivePoints * ratePerPoint * gradeFactor   (0 when effectivePoints <= 0)
```

Eligibility: `ACTIVE` employees whose date of joining is on or before the period's
month end, in the department the production record belongs to. The *Calculate*
endpoint recomputes a (scheme, department, year, month) cell: existing `DRAFT` rows
for that cell are deleted and regenerated; `APPROVED`/`REJECTED` history is never
touched. Approval (`HR_MANAGER`/`ADMIN`) stamps `decidedBy`/`decidedAt`.

## Audit trail

Every service mutation calls `AuditService.record(action, entity, id, beforeDto, afterDto)`.
Snapshots are serialized to `jsonb` (old value null on CREATE, new value null on DELETE),
the acting username is denormalized so entries survive user deletion, and rows are
append-only. The trail is queryable by entity, username, action and time range via a
JPA `Specification` (never `:param is null` query tricks — see
[DECISIONS.md](DECISIONS.md)).

## Data

Flyway owns the schema; Hibernate only validates it:

- `V1__schema.sql` — all tables, constraints, FKs
- `V2__demo_users_and_departments.sql` — 4 demo users (BCrypt) + 8 departments
- `V3__synthetic_data.sql` — **generated** by `scripts/generate_synthetic_data.py`
  (seeded, deterministic): ~150 employees, 24 months of production (2024–2025),
  3 schemes, seeded incentive earnings (approved history + a pending draft month)

Regenerate V3 with `python3 scripts/generate_synthetic_data.py`. Never put real plant
data anywhere in this repository.

## Frontend

Vite SPA, no UI framework: axios instance with a token interceptor (401 → back to
login), an auth context with role guards (`RequireRole`), a role-filtered app shell,
and pages per module. The dashboard renders stat tiles and an inline-SVG trend chart
(fixed two-series palette, legend, hover crosshair — no chart library). Tokens live in
`localStorage` for demo simplicity; see [DECISIONS.md](DECISIONS.md) for the trade-off.
