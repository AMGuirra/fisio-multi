# Copilot instructions for entity-crud-12

This file gives AI coding agents immediate, actionable knowledge to be productive in this repository.

Summary
- Java 17 Spring Boot 3 web app (Thymeleaf + Bootstrap). See [README.md](README.md).
- Single-tenant-per-database SaaS with multi-CNPJ inside each tenant (code expects an active `codigo_cnpj` in session).

Quick commands
- Run locally (dev): `./mvnw spring-boot:run` (project root).
- Build jar: `./mvnw -DskipTests package` then `java -jar target/*.jar`.
- Run tests: `./mvnw test` (tests use H2 in test scope).

Where to look (key paths)
- App entry: `src/main/java/com/fourcatsdev/entitycrud/EntityCrudApplication.java`
- Config / tenant handling: `src/main/java/com/fourcatsdev/entitycrud/config/` (TenantContext, filters)
- Domain entities: `src/main/java/com/fourcatsdev/entitycrud/domain/` (see `base` and `enums` subpackages)
- Repositories: `src/main/java/com/fourcatsdev/entitycrud/repository/` (Spring Data JPA)
- Services: `src/main/java/com/fourcatsdev/entitycrud/service/`
- Controllers (Thymeleaf-based): `src/main/java/com/fourcatsdev/entitycrud/controller/`
- Flyway migrations: `src/main/resources/db/migration/` (V1..V12 — V10 seeds dev company/user)
- Templates: `src/main/resources/templates/` (use Thymeleaf + layout dialect)
- Static assets: `src/main/resources/static/` (bootstrap + jquery included)

Important project-specific patterns
- Multi-tenancy: every DB operation is filtered by `codigo_cnpj`. Agents should avoid modifying persistence code without preserving tenant filtering. Check `config/*` for how the tenant is propagated.
- Single-tenant-per-db: provisioning or DB migration changes affect all customers; migrations are managed by Flyway in `db/migration`.
- UI: controllers return Thymeleaf templates under `templates/*` (many features implemented as traditional MVC controllers, not REST endpoints).
- Security: app uses Spring Security; templates for auth live in `templates/auth/` (login, reset-password, etc.). Be mindful of authentication flow when changing controllers/templates.
- Project uses Lombok for DTOs/entities — annotation processing is configured in `pom.xml`.

DB / environment notes
- Default dev DB settings are in `src/main/resources/application.properties` (postgres://localhost:5432/locadora, user/password `postgres`).
- Run `CREATE DATABASE locadora;` before starting, or update `application.properties` to match your DB.
- Flyway runs on startup; migration V10 seeds the dev company and user (CNPJ `00000000000191`).

Build & test tips
- `spring.jpa.hibernate.ddl-auto=validate` in `application.properties` — schema changes must be applied via Flyway migrations.
- Unit/integration tests use H2 (see `pom.xml` test dependencies). Prefer adding Flyway test migrations when adding schema changes for tests.

Code conventions to follow
- Package layout: `config`, `domain`, `repository`, `service`, `controller` — keep new code in the appropriate package.
- Controllers: keep logic thin; put business rules in `service` classes.
- Repositories: prefer Spring Data interfaces (extend `JpaRepository`).
- Templates: use the existing `layout.html` and fragments; follow existing CSS classes and Bootstrap 5 usage.

If you modify DB schema
- Add a new Flyway script in `src/main/resources/db/migration/` and update any affected JPA entities. Do NOT change `spring.jpa.hibernate.ddl-auto` to `update` locally to bypass migrations.

Examples (where to change for common tasks)
- Add a new entity `Product`: create `domain/Product.java`, `repository/ProductRepository`, `service/ProductService`, controller and templates under `templates/product/`.
- Change tenant resolution: edit `config/TenantFilter` and `config/TenantContext` to maintain session-scoped `codigo_cnpj` propagation.

Safety / gotchas
- Don't assume REST API patterns — many features are server-side rendered (Thymeleaf). API-like controllers are rare.
- Changing global Spring Security configuration impacts many templates; search for `templates/auth` and `security` beans before making changes.

When in doubt
- Inspect `src/main/resources/db/migration` and `src/main/java/com/fourcatsdev/entitycrud/config` first.
- Refer to [README.md](README.md) for high-level architecture and the dev CNPJ seed (`00000000000191`).

Ask for feedback: If any part of these instructions is unclear or missing examples you'd like, tell me which area to expand (DB migrations, tenant model, templates, or security).
