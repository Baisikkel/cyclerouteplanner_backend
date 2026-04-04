# Cycle Route Planner Backend

Backend service for cycling route planning integrations. Current scope includes:
- OSM/Overpass thin-slice connectivity check
- Maa-amet ADS thin-slice connectivity and search check
- Ingest skeleton with snapshot tracking for planned geo data providers
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
- `POST /api/address/cache/refresh?query=...&limit=...`
- `GET /api/geo/cache/status`
- `POST /api/geo/cache/osm/ingest`
- `POST /api/geo/cache/osm/refresh`
- `POST /api/geo/cache/tallinn/ingest?sourceLayer=...`
- `POST /api/geo/cache/tallinn/refresh`
- `POST /api/ingest/run`
- `GET /api/ingest/snapshots?limit=...`

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
- `ADS_STATUS_PATH`
- `ADS_SEARCH_PATH`
- `ADS_SEARCH_QUERY_PARAM`
- `ADS_SEARCH_LIMIT_PARAM`
- `ADS_CACHE_REFRESH_DEFAULT_QUERY`
- `ADS_CACHE_REFRESH_DEFAULT_LIMIT`
- `GEO_DEFAULT_BBOX_SOUTH`
- `GEO_DEFAULT_BBOX_WEST`
- `GEO_DEFAULT_BBOX_NORTH`
- `GEO_DEFAULT_BBOX_EAST`
- `GEO_OVERPASS_TIMEOUT_SECONDS`
- `GEO_TALLINN_SOURCE_URL`
- `GEO_TALLINN_SOURCE_LAYER`
- `GEO_TALLINN_FEATURE_ID_PROPERTY`
- `GEO_TALLINN_FEATURE_NAME_PROPERTY`
- `OSM_OVERPASS_BASE_URL`
- `INGEST_SCHEDULER_ENABLED`
- `INGEST_SCHEDULER_CRON`

## Tests
Run all tests:

```powershell
.\mvnw.cmd test
```

Notes:
- Unit/controller tests run without Docker.
- Integration test (`DatabaseMigrationIntegrationTest`) starts PostGIS via Testcontainers and validates Flyway migrations.
- Docker Desktop must be reachable by the test JVM for integration tests.
