# Changelog

## 0.5.0
- Staff management API (`/api/admin/staff`): list, create admins/experts, rename, activate/deactivate (not yourself), reset password; `IdentityApi` for other modules.
- Expert assignment: admin assigns an active expert to a request (internal timeline note); request detail shows the assigned expert.
- Expert API (`/api/expert/tasks`): own tasks only (others get 404), no client contact details, start/resume, upload work (kept internal), notes, submit for review.
- Review gate: an admin publishes a reviewed internal file to make it a client deliverable.

## 0.4.0
- Production Docker image (multi-arch JDK/JRE 21 base, non-root user, files on `/data/files`), used by `research-office-deploy`.

## 0.3.0
- Catalog module: the office's services (academic + business) seeded by migration; public list and admin edit. Thesis/paper ghostwriting is deliberately not offered.
- Files module: `FilesApi` with local-disk storage (`RO_FILES_DIR`), 50 MB limit, allow-list of document/data/image types, owner-scoped access, UTF-8 download names.
- Orders module: request lifecycle `NEW → QUOTED → ACCEPTED → IN_PROGRESS → IN_REVIEW → DELIVERED → CLOSED` (+ `REVISION`, `CANCELLED`) enforced by one transition table; references `RO-YYYY-NNNN`; timeline of events with client visibility.
- Public client API by private tracking link: create request, upload files, view status/quote/timeline, accept quote, ask for revision, confirm delivery, download deliverables (internal files never exposed).
- Admin API: search/filter/page requests, detail with next allowed statuses and tracking URL, send quote, change status, notes, upload deliverables/internal files, stats per status.

## 0.2.0
- Identity module: staff users (ADMIN, EXPERT), BCrypt passwords, first admin created from `RO_ADMIN_*`.
- Auth API: `POST /api/auth/login`, `/refresh` (rotating refresh tokens, reuse revokes the whole family), `/logout`, `GET /api/auth/me`.
- Role-based access: `/api/admin/**` ADMIN, `/api/expert/**` EXPERT or ADMIN.
- OpenAPI: DTO properties are required unless marked nullable, so generated web types are exact.


## 0.1.0
- Project skeleton: Spring Boot 4.1, Java 21, modular monolith (Spring Modulith boundary test).
- Shared building blocks: `BaseEntity` (UUID, audit, optimistic lock), `PageResponse`, RFC 9457 errors with `code`.
- Stateless JWT (HS256) security, CORS, public `/api/public/**`.
- PostgreSQL + Flyway baseline, Actuator health/info, OpenAPI at `/v3/api-docs`.
- Testcontainers integration tests, GitHub Actions CI.
