# Architecture Guide

## Goal

Keep a clean layered architecture where `obar-core` is shared by desktop and web, with a single database.

## Module Boundaries

- `obar-core`: shared business module (`model`, `dal`, `bll`, migrations, config internals).
- `obar-desktop`: JavaFX app for company/internal users.
- `obar-web`: Spring Web MVC + Thymeleaf app for client users.

Both apps use the same database schema and business logic from `obar-core`.

## Layer Model

### Core Layers

- `model`: domain entities and enums.
- `dal`: persistence repositories and Hibernate data access.
- `bll`: business rules, use-cases, service orchestration, DTOs.

### UI Layers

- Desktop UI: JavaFX controllers and views.
- Web UI: Spring controllers + Thymeleaf views.

## Dependency Direction (must follow)

- UI -> BLL
- BLL -> DAL + model
- DAL -> model + infra config
- model -> no dependency on DAL/BLL/UI

## Hard Rules

1. UI modules (`obar-desktop`, `obar-web`) must not import `com.obar.dal.*`.
2. UI modules must not import `com.obar.config.*`.
3. UI should consume BLL DTOs for read models, not ORM entities.
4. Hibernate and Flyway details stay inside `obar-core` internals.
5. Schema changes are done only by new Flyway migrations.

## Practical Pattern Used In This Project

- Startup lifecycle exposed by BLL facade (`CoreLifecycleService`) so UI does not call infra classes directly.

## Folder Intent

- `obar-core/src/main/java/com/obar/model`: entities, enums.
- `obar-core/src/main/java/com/obar/dal`: repositories.
- `obar-core/src/main/java/com/obar/bll`: business services, DTOs, command objects.
- `obar-core/src/main/resources/db/migration`: Flyway SQL migrations.

## Architecture Review Checklist

Before merging, confirm:

1. No UI import of DAL/config classes.
2. New business rules are in BLL, not in controllers.
3. New SQL schema change is a new migration file.
4. DTO naming is consistent (`*DTO`, `*Command`).
5. Desktop and web can both reuse the change through `obar-core`.
