# ATOM Frontend

Next.js 16 + React 18 + JavaScript frontend for the existing Spring Boot API.

## Run

```powershell
Set-Location frontend
npm install
$env:NEXT_PUBLIC_API_BASE_URL = "http://localhost:8080" # optional
npm run dev
```

`NEXT_PUBLIC_API_BASE_URL` defaults to `http://localhost:8080`. The frontend
does not call the AI service; ATOM and CPSR workflows are backed by the Java
API only.

## Tests

```powershell
npm test
npm run test:run
```

The API client tests run in Vitest with a jsdom environment and mock `fetch`
and browser storage, so they do not make network requests.

The browser authenticates with backend HttpOnly cookies; JWTs are never
returned to browser JavaScript, stored in localStorage, rendered, or logged.
The API client sends `credentials: 'include'` and echoes the readable CSRF
cookie in `X-CSRF-TOKEN` for state-changing requests. Session restore calls
`GET /api/auth/me`.
If the backend uses a non-default CSRF cookie name, set
`NEXT_PUBLIC_CSRF_COOKIE_NAME` to the same value.

## Routes

- `/` login
- `/signup` public USER signup
- `/user` authenticated workspace
- `/admin` ADMIN-only users, departments, roles, and menus CRUD
- `/atom` and `/cpsr` paginated request lists and create forms
- `/atom/:id` and `/cpsr/:id` request details, editing, transitions, comments, and history

Only safe user metadata may be cached locally; JWTs are never returned to
browser JavaScript, stored, rendered, or logged. API requests use
`credentials: 'include'`, add the CSRF header for state-changing calls, and
normalize JSON errors into `ApiError`. The backend uses zero-based pagination
and returns
`{ content, page, size, totalElements, totalPages }`.

Sign out calls the backend `POST /api/auth/logout` endpoint before clearing the
client metadata. Do not commit credentials, prefilled passwords, or production
API URLs. Configure the backend JWT secret and bounded CORS origins externally.
