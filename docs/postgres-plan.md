# PostgreSQL and backend release plan

## Prerequisites

Install Docker Desktop (or another Docker Engine) and verify:

```powershell
docker --version
docker compose version
docker info
```

The compose file uses `postgres:16-alpine`, a persistent volume, environment
driven credentials, and a `pg_isready` healthcheck. `POSTGRES_PASSWORD` is
required and must not be committed. The default database/user are `atom` and
`atom`; override them with `POSTGRES_DB` and `POSTGRES_USER`.

## Start and verify the database

```powershell
Set-Location C:\Users\인포비10\platdorm
$env:POSTGRES_PASSWORD='a-local-password'
docker compose config
docker compose up -d postgres
docker compose ps
docker compose exec postgres pg_isready -U atom -d atom
```

`docker compose up` must report the database as healthy before using the API.
If Docker Desktop is not running, `docker compose config` still validates the
rendered configuration, but PostgreSQL integration has not been tested.

## Run the backend

```powershell
Set-Location C:\Users\인포비10\platdorm\backend-java
$env:PROFILE='postgres'
$env:DB_URL='jdbc:postgresql://localhost:5432/atom'
$env:DB_USERNAME='atom'
$env:DB_PASSWORD='a-local-password'
$env:DB_SCHEMA='public'
$env:JWT_SECRET='generate-and-store-a-random-secret-of-at-least-32-bytes'
C:\Users\인포비10\maven\bin\mvn.cmd test
C:\Users\인포비10\maven\bin\mvn.cmd package
java -jar target\atom-backend-0.1.0-SNAPSHOT.jar
```

Flyway migrations `V1` through `V4` create and validate the schema. JPA is
configured with `spring.jpa.hibernate.ddl-auto=validate`; schema changes must
be added as new migrations. New migrations use guarded DDL where appropriate,
foreign keys, status checks, and indexes.

## Roles and endpoints

- Public: health, signup, login.
- USER: own ATOM/CPSR CRUD, submit/cancel, comments, and history.
- ADMIN: all USER operations plus all request visibility, review/approve/reject,
  user enable/disable, and existing department/role/menu CRUD.
- Request statuses: `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED`,
  `CANCELLED`. Invalid transitions return JSON `409`; unauthenticated and
  unauthorized calls return JSON `401`/`403`.

The backend records manually entered `evaluation_result` values and transition
history. No AI prediction or AI-service integration is performed in this
release; that integration is explicitly deferred.
