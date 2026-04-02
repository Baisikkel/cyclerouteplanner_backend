# Cycle Route Planner Backend

Backend service for cycling route planning integrations. Current scope includes:
- OSM/Overpass thin-slice connectivity check
- Maa-amet ADS thin-slice connectivity and search check
- PostgreSQL + PostGIS with Flyway migrations

## Stack
- Java 25
- Spring Boot 4.x
- PostgreSQL/PostGIS
- Flyway
- Maven
- Docker / Docker Compose

## Local Development
1. Create `.env` from `.env.sample` and set credentials.
2. Start only infrastructure (DB):

```powershell
docker compose -f compose.yaml up -d
```

3. Run backend from IDE or terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

### Full Containerized Run

```powershell
docker compose -f compose.full.yaml up -d
```

## API Docs (Swagger / OpenAPI)
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Current API Endpoints
- `GET /api/osm/connectivity`
- `GET /api/address/connectivity`
- `GET /api/address/search?query=...&limit=...`

## Configuration
Profiles:
- `local` (default): app on host + local/compose DB
- `docker`: app inside container
- `test`: lightweight test profile (no datasource)
- `integration`: real DB integration tests via Testcontainers

Important env vars:
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `POSTGRES_PORT`
- `ADS_BASE_URL`
- `OSM_OVERPASS_BASE_URL`

## Tests
Run all tests:

```powershell
.\mvnw.cmd test
```

Notes:
- Unit/controller tests run without Docker.
- Integration test (`DatabaseMigrationIntegrationTest`) starts PostGIS via Testcontainers and validates Flyway migrations.
- Docker Desktop must be reachable by the test JVM for integration tests.
