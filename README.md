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

## MVP Runbook
Use this sequence for first-time local setup and verification.

1. Start PostGIS:

```powershell
docker compose -f compose.yaml up -d
```

2. Start backend:

```powershell
.\mvnw.cmd spring-boot:run
```

3. Check source connectivity:

```powershell
Invoke-RestMethod "http://localhost:8080/api/address/connectivity"
Invoke-RestMethod "http://localhost:8080/api/osm/connectivity"
```

4. Run manual refreshes (same order each time):

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/address/cache/refresh?query=Tallinn&limit=100"
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/osm/refresh"
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/tallinn/refresh"
```

5. Check cache/status API:

```powershell
Invoke-RestMethod "http://localhost:8080/api/geo/cache/status"
```

6. Quick SQL verification:

```powershell
docker exec -it db psql -U ${env:POSTGRES_USER} -d ${env:POSTGRES_DB} -c "select count(*) as ads_count from address.ads_address_cache;"
docker exec -it db psql -U ${env:POSTGRES_USER} -d ${env:POSTGRES_DB} -c "select count(*) as osm_count from geo.osm_feature_cache;"
docker exec -it db psql -U ${env:POSTGRES_USER} -d ${env:POSTGRES_DB} -c "select count(*) as tallinn_count from geo.tallinn_layer_cache;"
docker exec -it db psql -U ${env:POSTGRES_USER} -d ${env:POSTGRES_DB} -c "select source, max(created_at) as last_ingested_at from meta.data_snapshot group by source order by source;"
```

If scheduled refresh is needed later, enable it in `.env`:
- `GEO_REFRESH_SCHEDULER_ENABLED=true`

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
- `REFRESH_RATE_LIMIT_MAX_REQUESTS`
- `REFRESH_RATE_LIMIT_WINDOW_SECONDS`
- `GEO_REFRESH_SCHEDULER_ENABLED`
- `GEO_REFRESH_SCHEDULER_CRON`
- `GEO_REFRESH_SCHEDULER_ADS_ENABLED`
- `GEO_REFRESH_SCHEDULER_OSM_ENABLED`
- `GEO_REFRESH_SCHEDULER_TALLINN_ENABLED`
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
