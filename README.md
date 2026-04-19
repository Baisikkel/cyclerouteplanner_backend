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

## BRouter (Routing Engine)

The backend uses [BRouter](https://github.com/abrensch/brouter) to plan
cycling routes. We run our own copy in a container so we do not depend on
the public brouter.de service. See `deploy/brouter/` for the Dockerfile,
entrypoint script, routing profiles, and the map data folder.

### First-time setup (once per machine)

Map data files are too large to keep in git — download them with the
bundled script, run from the backend repo root:

```sh
./deploy/brouter/fetch-segments.sh
```

This downloads ~40 MB of routing data for Tallinn and Harjumaa into
`deploy/brouter/segments/` and skips files that are already there on repeat
runs.

### Running

Start the full stack (brouter + postgres + backend):

```sh
docker compose up -d --build
```

Or just brouter on its own (useful when running the backend from the IDE):

```sh
docker compose up -d brouter
```

Once running, you can reach the engine at `http://localhost:17777/brouter`.
Quick sanity check (a short route through central Tallinn):

```sh
curl 'http://localhost:17777/brouter?lonlats=24.753,59.436|24.757,59.455&profile=trekking&alternativeidx=0&format=geojson'
```

### Profiles

Four stock routing profiles ship in `deploy/brouter/profiles/`:
`trekking.brf`, `fastbike.brf`, `safety.brf`, `gravel.brf`. These are
unmodified copies from BRouter v1.7.8. A follow-up ticket will customise
them for Tallinn conditions.

## Tests
Run all tests:

```powershell
.\mvnw.cmd test
```

Notes:
- Unit/controller tests run without Docker.
- Integration tests use Testcontainers PostGIS.
