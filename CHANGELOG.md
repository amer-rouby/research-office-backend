# Changelog

## 0.2.0
- Identity module: staff users (ADMIN, EXPERT), BCrypt passwords, first admin created from `RO_ADMIN_*`.
- Auth API: `POST /api/auth/login`, `/refresh` (rotating refresh tokens, reuse revokes the whole family), `/logout`, `GET /api/auth/me`.
- Role-based access: `/api/admin/**` ADMIN, `/api/expert/**` EXPERT or ADMIN.


## 0.1.0
- Project skeleton: Spring Boot 4.1, Java 21, modular monolith (Spring Modulith boundary test).
- Shared building blocks: `BaseEntity` (UUID, audit, optimistic lock), `PageResponse`, RFC 9457 errors with `code`.
- Stateless JWT (HS256) security, CORS, public `/api/public/**`.
- PostgreSQL + Flyway baseline, Actuator health/info, OpenAPI at `/v3/api-docs`.
- Testcontainers integration tests, GitHub Actions CI.
