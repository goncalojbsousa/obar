# Obar

---

## Project Structure

```
obar/
├── obar-core/        # Shared module — Model, DAL, BLL (compiled as .jar)
├── obar-desktop/     # JavaFX desktop application (company-facing)
├── obar-web/         # Spring Web MVC application (client-facing)
├── docker/
│   └── docker-compose.yml
└── pom.xml           # Parent Maven POM
```

`obar-core` is the heart of the project. It is compiled as a `.jar` and used by both `obar-desktop` and `obar-web`. Both applications share the same database.

---

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/goncalojbsousa/obar.git
cd obar
```

### 2. Start the database

Create your local environment file from the template:

```bash
copy .env.example .env
```

Then start PostgreSQL:

```bash
docker compose --env-file .env -f docker/docker-compose.yml up -d
```

This starts a PostgreSQL 16 instance on `localhost:5432`.

> The database schema is managed by **Flyway** and is applied automatically when the application starts for the first time.

### 3. Open in IntelliJ

- Open IntelliJ IDEA
- **File → Open** → select the `obar` root folder
- Wait for Maven to download dependencies (watch the bottom progress bar)
- Click **Load Maven Changes** if prompted

### 4. Build the core module

In the Maven panel (right side), run:

```
obar → obar-core → Lifecycle → install
```

This compiles `obar-core` and installs it in your local Maven repository so `obar-desktop` and `obar-web` can use it.

### 5. Run the application

- **Desktop:** Run the main class in `obar-desktop`
- **Web:** Run the main class in `obar-web`

---

## Database

Database configuration is read from environment variables (`.env` in local development).

| Property | Value |
|---|---|
| Host | `DB_HOST` (default: `localhost`) |
| Port | `DB_PORT` (default: `5432`) |
| Database | `DB_NAME` (default: `obar`) |
| Username | `DB_USER` (default: `obar_user`) |
| Password | `DB_PASSWORD` (default: `obar_pass`) |

Connect with any PostgreSQL client using the credentials above.

### Migrations

Database migrations are handled by **Flyway**. Migration files are located at:

```
obar-core/src/main/resources/db/migration/
```

Naming convention:
```
V1__create_schema.sql
V2__add_some_feature.sql
V3__insert_seed_data.sql
```

**Never edit an existing migration file.** Always create a new one.

---

## Stopping the database

```bash
docker compose --env-file .env -f docker/docker-compose.yml down
```

To stop and **delete all data**:

```bash
docker compose --env-file .env -f docker/docker-compose.yml down -v
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Database | PostgreSQL 16 |
| ORM | Hibernate 6 |
| Migrations | Flyway 10 |
| Desktop UI | JavaFX |
| Web | Spring Web MVC + Thymeleaf |
| Build | Maven (multi-module) |
| Language | Java 21 |