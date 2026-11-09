# research-office-backend

Backend لمنصة مكتب الخدمات البحثية: Spring Boot 4.1 · Java 21 · PostgreSQL.

## التشغيل محليًا
```bash
# من فولدر research-office
docker compose up -d

# من research-office-backend (JDK 21)
./mvnw spring-boot:run
```
- API: http://localhost:8090
- OpenAPI: http://localhost:8090/v3/api-docs · Swagger: http://localhost:8090/swagger-ui.html
- Health: http://localhost:8090/actuator/health

## الاختبارات
```bash
./mvnw verify   # يحتاج Docker (Testcontainers)
```

## المعمارية
Modular monolith، كل موديول package تحت `com.researchoffice`:
`shared` (مفتوح للكل) · `identity` · `catalog` · `orders` · `experts` · `files` · `payments` · `notifications` · `messaging` · `ai`.
`ModularityTests` بيفشّل الـ build لو موديول دخل على internals موديول تاني.
كل تغيير في قاعدة البيانات = Flyway migration جديد في `src/main/resources/db/migration`.

## متغيرات البيئة (كلها `RO_*`)
| المتغير | الافتراضي |
|---|---|
| `RO_PORT` | `8090` |
| `RO_DB_URL` | `jdbc:postgresql://localhost:5434/research_office` |
| `RO_DB_USERNAME` / `RO_DB_PASSWORD` | `ro` / `ro` |
| `RO_JWT_SECRET` | قيمة dev فقط — لازم تتغير في الإنتاج (32 بايت على الأقل) |
| `RO_CORS_ORIGINS` | `http://localhost:4200` |
| `RO_FILES_DIR` | `data/files` (مكان حفظ الملفات) |
| `RO_WEB_URL` | `http://localhost:4200` (لتكوين لينك متابعة العميل) |
| `RO_ACCESS_TOKEN_TTL` / `RO_REFRESH_TOKEN_TTL` | `15m` / `14d` |
| `RO_ADMIN_EMAIL` | `admin@research-office.local` (أول أدمن بيتعمل لو مفيش مستخدمين) |
| `RO_ADMIN_PASSWORD` | `ChangeMe#2026` — للتطوير فقط، غيّره قبل الإنتاج |

## الإصدارات
كل تغيير على branch ← Pull Request ← تحديث الـ version في `pom.xml` و`CHANGELOG.md` ← merge ← tag `vX.Y.Z` ← GitHub Release.
