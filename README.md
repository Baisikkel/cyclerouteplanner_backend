# Cycle Route Planner Backend

Backend service for cycling route planning integrations.

Current scope:
- OSM/Overpass connectivity and cycling-network ingest
- Maa-amet ADS connectivity and address cache refresh
- Tallinn open data GeoJSON ingest
- PostgreSQL + PostGIS with Flyway migrations

## Stack
- Java 25
- Spring Boot 4.x
- PostgreSQL/PostGIS
- Flyway
- Maven
- Docker Compose

## Local Setup
1. Create `.env` from `.env.sample`.
2. Start database only:

```powershell
docker compose up -d postgres
```

3. Run backend from IDE or terminal:

```powershell
.\mvnw.cmd spring-boot:run
```

## Full Containerized Setup
Start backend + database:

```powershell
docker compose up -d --build
```

Database only:

```powershell
docker compose up -d postgres
```

## API Docs
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Populate Database
Run these in this order after backend is up:

```powershell or from Swagger
Invoke-RestMethod -Method Post "http://localhost:8080/api/address/cache/refresh?query=Tallinn&limit=100"
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/osm/refresh"
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/tallinn/refresh"
```

Check ingest result:

```powershell
Invoke-RestMethod "http://localhost:8080/api/geo/cache/status"
```

Optional source connectivity checks:
- `GET /api/address/connectivity`
- `GET /api/osm/connectivity`

Dev-only endpoints (`API_DEV_ENDPOINTS_ENABLED=true`):
- `POST /api/geo/cache/osm/ingest`
- `POST /api/geo/cache/tallinn/ingest?sourceLayer=...`
- `POST /api/ingest/run`
- `GET /api/ingest/snapshots?limit=...`

## Tests
Run all tests:

```powershell
.\mvnw.cmd test
```

Notes:
- Unit/controller tests run without Docker.
- Integration tests use Testcontainers PostGIS.
