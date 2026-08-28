# ATOM Backend

Spring Boot 3.3.2 REST API using PostgreSQL (or H2 for local tests), Flyway,
BCrypt passwords, JWT authentication, and role-based authorization. AI service
integration is intentionally deferred; evaluation fields are manual placeholders.

## Profiles and environment

`PROFILE=local` is the default and uses an in-memory H2 database. Tests provide
their own isolated seed data. Production and PostgreSQL deployments do not ship
with a usable administrator account.

The `postgres` profile requires:

- `DB_URL` (default `jdbc:postgresql://localhost:5432/atom`)
- `DB_USERNAME` (default `atom`)
- `DB_PASSWORD` (required; use the same value as `POSTGRES_PASSWORD`)
- `DB_SCHEMA` (default `public`)
- `JWT_SECRET` (required, at least 32 bytes, generated and stored outside git)
- `JWT_EXPIRATION_MS` (default `3600000`)
- `ADMIN_BOOTSTRAP_USERNAME` and `ADMIN_BOOTSTRAP_PASSWORD` (optional,
  externally supplied one-time bootstrap; remove them after first startup)

The PostgreSQL profile removes the historical demo users with a new Flyway
migration. To create the first administrator, set both bootstrap variables in
the process environment. The password is BCrypt-hashed before storage and is
never returned by the API. Do not put either value in source control.

## API

Public: `GET /api/health`, `POST /api/auth/signup`, `POST /api/auth/login`.
Signup creates a regular enabled `USER`; it never accepts a role.

Authenticated users can create, list, edit, submit, cancel, comment on, and view
history for `/api/atom-requests` and `/api/cpsr-requests`. Lists are paginated
with `page` and `size` (maximum size 100). Supported sort fields are `id`,
`title`, `status`, `createdAt`, and `updatedAt`; invalid pageable values return
a JSON 400 error, and users see only their own requests, comments, and history.
Transitions are `submit`, `review`, `approve`, `reject`, and `cancel`; only ADMIN
can review or approve/reject. ADMIN sees all requests and can manage users,
including `PATCH /api/admin/users/{id}/enabled?enabled=false`, departments,
roles, and menus.

Send `Authorization: Bearer <token>`. Disabled users cannot log in or use
previously issued tokens. Errors are JSON with `status`, `error`, `message`,
and (for validation) `validationErrors`; malformed input, conflicts, and
optimistic-lock failures are controlled API errors. Password hashes are never
returned. CORS allows origins from `CORS_ALLOWED_ORIGINS` (default
`http://localhost:3000`) without wildcard credentials.

Browser login sets an HttpOnly JWT cookie (`ATOM_AUTH` by default) and a
readable `ATOM_CSRF` cookie. State-changing browser requests must echo the
CSRF cookie in `X-CSRF-TOKEN`; safe GET/HEAD/OPTIONS requests are exempt.
`COOKIE_SECURE`, `COOKIE_SAME_SITE`, `AUTH_COOKIE_NAME`, and `CSRF_COOKIE_NAME`
configure cookie behavior. `/api/auth/me` restores the current browser session.
Non-browser clients may continue to send bearer authorization and are not
subject to cookie CSRF checks.

## OpenAPI documentation

In local development, OpenAPI JSON and Swagger UI are public by default. In
production they require an ADMIN token unless `API_DOCS_PUBLIC` is explicitly
enabled. Use the Authorize button for authenticated operations. The H2 console
is disabled unless `H2_CONSOLE_ENABLED=true` is explicitly set for local
development; it is never enabled by the PostgreSQL profile.
Credential fields and JWT/database secrets are not included in the published
schemas or responses.

### Admin list endpoints

All four ADMIN-only list endpoints keep their existing paths and return the
stable shape `{ content, page, size, totalElements, totalPages }`:

- `GET /api/admin/users`
- `GET /api/admin/departments`
- `GET /api/admin/roles`
- `GET /api/admin/menus`

Each accepts `page` (zero-based, default `0`), `size` (default `20`; values
above `100` are capped at `100`), `sort` (default `id,asc`, in the form
`field[,asc|desc]`), and `search` (default empty, case-insensitive, maximum
100 characters). Supported sort fields are `id,username,fullName,role,enabled`
for users; `id,name` for departments and roles; and `id,label,path` for menus.
Negative pages, non-positive sizes, unsupported sort fields or directions, and
overlong or malformed searches return the standard JSON 400 error.

Examples:

```text
GET /api/admin/users?page=0&size=20&sort=username,asc&search=atom
GET /api/admin/departments?page=1&size=10&sort=name,desc&search=engineering
GET /api/admin/roles?search=admin
GET /api/admin/menus?sort=label,asc&search=report
```

## Verify locally

```powershell
Set-Location C:\Users\인포비10\platdorm\backend-java
C:\Users\인포비10\maven\bin\mvn.cmd test
C:\Users\인포비10\maven\bin\mvn.cmd package
```

## Verify PostgreSQL

From the repository root, set a non-secret password and JWT secret in the
process environment (or a local, untracked `.env`), then:

```powershell
docker compose config
docker compose up -d postgres
docker compose ps
docker compose exec postgres pg_isready -U atom -d atom
Set-Location C:\Users\인포비10\platdorm\backend-java
$env:PROFILE='postgres'
$env:DB_PASSWORD='the-local-password'
$env:JWT_SECRET='generate-a-random-secret-at-least-32-bytes'
C:\Users\인포비10\maven\bin\mvn.cmd package
java -jar target\atom-backend-0.1.0-SNAPSHOT.jar
```

The exact container name can differ; use `docker compose ps` and substitute the
reported name. Flyway runs migrations at startup and JPA uses `ddl-auto=validate`.
If Docker is unavailable, run `docker compose config` and report that PostgreSQL
could not be started rather than treating H2 as a PostgreSQL verification.

## Backend release checklist

- [ ] Run `C:\Users\인포비10\maven\bin\mvn.cmd test` from `backend-java`.
- [ ] Run `C:\Users\인포비10\maven\bin\mvn.cmd package` from `backend-java`.
- [ ] Verify API docs are protected or disabled in production, while `/api/**`
      remains protected as documented.
- [ ] Confirm H2 console access is disabled outside explicitly local development.
- [ ] Confirm the OpenAPI document contains the JWT bearer scheme and no
      password hashes, passwords, `JWT_SECRET`, or database credentials.
- [ ] For PostgreSQL release candidates, set non-secret process environment
      values, run Flyway with `ddl-auto=validate`, and review migration output.
- [ ] Review admin role requirements, request ownership, validation bounds,
      CORS origins, and disabled-user token behavior before deployment.
