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
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/routing-edges/rebuild-and-export-pseudo-tags"
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/routing-edges/export"
Invoke-RestMethod -Method Post "http://localhost:8080/api/routes/options/refresh"
```

Check ingest result:

```powershell
Invoke-RestMethod "http://localhost:8080/api/geo/cache/status"
Invoke-RestMethod "http://localhost:8080/api/geo/cache/routing-audit"
Invoke-RestMethod "http://localhost:8080/api/geo/cache/routing-edges/status"
Get-Item "brouter/build-input/routing_edges.geojson"
```

Optional source connectivity checks:
- `GET /api/address/connectivity`
- `GET /api/osm/connectivity`
- `GET /api/geo/cache/routing-audit` (core routing readiness for merged OSM + Tallinn graph build)
- `POST /api/geo/cache/routing-edges/rebuild` (builds merged routing-edge cache from OSM + optional Tallinn overlap)
- `POST /api/geo/cache/routing-edges/rebuild-and-export-pseudo-tags` (core prep for BRouter map creation: rebuild + pseudo-tag export)
- `GET /api/geo/cache/routing-edges/status`
- `POST /api/geo/cache/routing-edges/export` (writes active merged edges to BRouter build input GeoJSON)
- `POST /api/geo/cache/routing-edges/export-pseudo-tags` (writes active merged-edge pseudo-tags for BRouter map creation)

Route option endpoints:
- `POST /api/routes/options/refresh` (builds OSM-first options and enriches with Tallinn overlap where available)
- `GET /api/routes/options?limit=100`
- `GET /api/routes/calculate?startLat=...&startLon=...&endLat=...&endLon=...`
  - optional `profile` query param: `fastbike|safety|gravel|trekking`
  - if `profile` is omitted (or unsupported), backend picks the best nearby persisted route-option profile and falls back to `fastbike`

Dev-only endpoints (`API_DEV_ENDPOINTS_ENABLED=true`):
- `POST /api/geo/cache/osm/ingest`
- `POST /api/geo/cache/tallinn/ingest?sourceLayer=...`
- `POST /api/ingest/run`
- `GET /api/ingest/snapshots?limit=...`

## BRouter (Routing Engine)

The backend uses [BRouter](https://github.com/abrensch/brouter) to plan
cycling routes. We run our own copy in a container so we do not depend on
the public brouter.de service. See `brouter/` for the Dockerfile,
entrypoint script, routing profiles, and the map data folder.

Production deployment is managed in the
[cyclerouteplanner-infra](https://github.com/Baisikkel/cyclerouteplanner-infra)
repo — this copy is for local development only.

### First-time setup (once per machine)

Map data files are too large to keep in git — download them with the
bundled script, run from the backend repo root:

```sh
./brouter/fetch-segments.sh
```

This downloads ~40 MB of routing data for Tallinn and Harjumaa into
`brouter/segments/` and skips files that are already there on repeat
runs.

### Merged Build Input Export

After rebuilding `geo.routing_edge_cache`, export merged OSM+Tallinn edges:

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/routing-edges/export"
```

By default the backend writes the file to:

`brouter/build-input/routing_edges.geojson`

Override path with:

`GEO_ROUTING_EDGES_EXPORT_OUTPUT_PATH`

Pseudo-tags output path can be overridden with:

`GEO_ROUTING_EDGES_EXPORT_PSEUDO_TAGS_OUTPUT_PATH`

### Segment Build With Pseudo-Tags

To rebuild local `.rd5` segments with merged-edge pseudo-tags:

1. Optional: place a base OSM extract at:
   `brouter/planet/estonia-latest.osm.pbf`
   (if missing, the build script auto-downloads from Geofabrik by default)
2. Rebuild merged routing edges and export pseudo-tags:

```powershell
Invoke-RestMethod -Method Post "http://localhost:8080/api/geo/cache/routing-edges/rebuild-and-export-pseudo-tags"
```

3. Run map creation inside the brouter container:

```powershell
docker compose run --rm --entrypoint /usr/local/bin/build-segments-with-pseudotags.sh -e PLANET_FILE=/planet/estonia-latest.osm.pbf brouter
```

The script writes generated `.rd5` files into `brouter/segments/`.
If `PLANET_FILE` is missing and auto-download fails, the script exits with an explicit error.

Auto-download settings:
- `BROUTER_PLANET_AUTO_DOWNLOAD` (default `true`)
- `BROUTER_PLANET_DOWNLOAD_URL` (default `https://download.geofabrik.de/europe/estonia-latest.osm.pbf`)

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

Four stock routing profiles ship in `brouter/profiles/`:
`trekking.brf`, `fastbike.brf`, `safety.brf`, `gravel.brf`.

## Tests
Run all tests:

```powershell
.\mvnw.cmd test
```

Notes:
- Unit/controller tests run without Docker.
- Integration tests use Testcontainers PostGIS.
