# Development guide

## Prerequisites

- Java 17 (any JDK 17; on macOS: `brew install openjdk@17`)
- Maven 3.9+ (`brew install maven`)
- Node 20+ / npm
- PostgreSQL 16 — or just use Docker / docker-compose
- Python 3 (only to regenerate synthetic data)

## One-command demo (Docker)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080 (Swagger UI at `/swagger-ui.html`)
- Postgres: localhost:5432 (`hrd`/`hrd`)

Flyway migrates the database on first boot. Demo credentials are in the
[README](../README.md) (synthetic data only).

## Manual setup

### Backend

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)   # macOS, if the default JDK is newer

# point at your Postgres (defaults shown)
export DB_HOST=localhost DB_PORT=5432 DB_NAME=hrd DB_USER=hrd DB_PASSWORD=hrd
export JWT_SECRET=local-dev-secret-at-least-32-chars!!

mvn spring-boot:run
```

| Env var | Default | Notes |
|---|---|---|
| `DB_HOST/PORT/NAME/USER/PASSWORD` | localhost/5432/hrd/hrd/hrd | datasource |
| `JWT_SECRET` | – (required at boot) | HS256 key, **>= 32 chars** |
| `app.jwt.ttl-minutes` (application.yml) | 480 | token lifetime |

### Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /api → localhost:8080
```

## Tests

```bash
cd backend
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

mvn test           # unit tests (IncentiveCalculator, JwtService, UserService) — no DB needed
mvn verify         # + Testcontainers integration tests — REQUIRES a Docker daemon
```

Integration tests (`*IT.java`) boot the real app against a disposable
`postgres:16-alpine` container (`@ServiceConnection`) and exercise auth, RBAC,
audit and the incentive flow end to end.

**No local Docker?** On macOS without Docker Desktop, `brew install colima docker`
and start `colima` — or push and let the [GitHub Actions workflow](../.github/workflows/ci.yml)
run `mvn verify` for you (its runners have Docker).

## Regenerating synthetic data

`V3__synthetic_data.sql` is generated, never hand-edited:

```bash
python3 scripts/generate_synthetic_data.py
```

The script is seeded and deterministic (same input → same SQL). Changes to employee
counts, months or schemes belong in the script, not the SQL.

## Conventions

- Java 17, no Lombok — records for DTOs, manual accessors on entities.
- DTOs live as nested records in each module's `XxxDtos` class.
- Every service mutation goes through `AuditService.record(...)`.
- Filtered queries use JPA `Specification`s — never `:param is null` patterns.
- Paged endpoints return the `PageResponse` envelope.
- Frontend: no UI framework; one `styles.css` design system; typed API calls in
  `src/api.ts`.
