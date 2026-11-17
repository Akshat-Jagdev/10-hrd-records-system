# Decision log (ADR-lite)

Short records of the choices that shaped this project and why.

## D1 — Java 17 + Spring Boot 3.5, no Lombok
Lombok annotation processing is a recurring source of JDK-version friction; records
give immutable DTOs natively and entity accessors are explicit. Java 17 is the
current LTS sweet spot for Spring Boot 3.x.

## D2 — Stateless JWT (HS256), single secret
A demo-scale system doesn't need refresh-token rotation or an external IdP. The
token carries `role` and `name` claims so most requests need no DB lookup for
authorization decisions (roles are re-checked per request from the token; the
`enabled` flag is checked at authentication time — documented trade-off: disabling
a user does not revoke already-issued tokens until they expire, 8h by default).

## D3 — Roles: ADMIN / HR_MANAGER / VIEWER / AUDITOR
Mirrors real plant-HR separation of duties: only HR stewards write business data,
auditors get read-everything **plus the audit trail**, viewers read business data
only, and ADMIN exists for user administration. Invariants roles can't express
(last enabled ADMIN, self-delete, referential "still in use" guards) are enforced
in services as 409s with clear messages.

## D4 — Flyway owns the schema; Hibernate validates
`ddl-auto=validate` + hand-written `V1` means the SQL in migrations is the source
of truth (reviewable in PRs), while the entity mappings stay honest — boot fails
loudly on drift. Synthetic data is *generated* by a committed, seeded script
(`scripts/generate_synthetic_data.py`) so the dataset is reproducible and
obviously-not-real.

## D5 — JPA Specifications instead of `:param is null` queries
Optional-filter search written as `where (:x is null or e.x = :x)` breaks with
Hibernate 6 parameter typing on PostgreSQL (null typed as `bytea`). Each module's
filtered endpoint builds a `Specification` with null-skipping predicates — correct,
composable, and index-friendly.

## D6 — Audit as jsonb snapshots, written by services
`audit_log` stores the acting username (denormalized), the action, and the full
before/after DTO snapshots as `jsonb`. That keeps the trail queryable in SQL and
renderable in the UI without entity-specific audit tables. Services call
`AuditService.record(...)` inside their transaction — the trail can't be forgotten
by a controller path, and an audited mutation that rolls back leaves no row.

## D7 — Incentive calculation deletes + regenerates DRAFT rows only
Recalculation for a (scheme, department, period) cell wipes existing `DRAFT` rows
and inserts fresh ones; `APPROVED`/`REJECTED` decisions are immutable history.
`IncentiveCalculator` is a pure static class so the money math is exhaustively
unit-testable without Spring.

## D8 — `PageResponse` envelope
Spring Data's `Page` JSON shape has churned across Boot versions (and warns since
3.3). A five-field envelope (`content, page, size, totalElements, totalPages`)
stabilizes the API contract and the frontend types.

## D9 — React without a UI kit; hand-rolled CSS design system
For a portfolio project the interesting part is data flow and API design, not
which component library was installed. One `styles.css` (tokens for ink/surface/
series/status) keeps the visual language consistent; the dashboard chart is a
hand-built inline SVG with a fixed two-series palette, legend and hover crosshair
— no chart dependency.

## D10 — JWT in localStorage (demo trade-off)
Vulnerable to XSS script exfiltration where httpOnly cookies are not; acceptable
here because there is nothing sensitive to steal (synthetic data) and it keeps the
backend a pure stateless API. A production deployment should move the token to an
httpOnly cookie with CSRF protection.

## D11 — Testcontainers ITs, CI-run when Docker is absent locally
Integration tests (`*IT.java`, failsafe plugin) boot the app against a real
disposable PostgreSQL container — the same engine, JSONB included, as production.
Machines without a Docker daemon skip them (`disabledWithoutDocker = true`);
GitHub Actions runs the full `mvn verify`.
