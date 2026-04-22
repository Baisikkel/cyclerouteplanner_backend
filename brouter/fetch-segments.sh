#!/usr/bin/env bash
# Downloads BRouter routing data for Tallinn + Harjumaa (~40 MB, one-off).
# Safe to re-run — files that are already there get skipped.
#
# Run from the backend repo root:
#     ./brouter/fetch-segments.sh
#
# Background: BRouter cuts the world into square map tiles. Tallinn and
# Harjumaa fit into the two tiles listed below. The files are too big to
# store in git, so we download them on first setup and keep them locally.
set -euo pipefail

SEGMENT_DIR="$(cd "$(dirname "$0")" && pwd)/segments"
UPSTREAM_BASE="https://brouter.de/brouter/segments4"
TILES=(E20_N55.rd5 E25_N55.rd5)

mkdir -p "$SEGMENT_DIR"
cd "$SEGMENT_DIR"

for tile in "${TILES[@]}"; do
  if [ -f "$tile" ]; then
    echo "[fetch-segments] $tile already present — skipping"
    continue
  fi
  echo "[fetch-segments] downloading $tile ..."
  # --fail aborts on HTTP errors; -L follows redirects; write to .tmp and mv
  # on success so an interrupted download never leaves a half-written file.
  curl --fail -sSL -o "$tile.tmp" "$UPSTREAM_BASE/$tile"
  mv "$tile.tmp" "$tile"
  echo "[fetch-segments] $tile done"
done

echo "[fetch-segments] segment directory: $SEGMENT_DIR"
ls -lh "$SEGMENT_DIR"/*.rd5 2>/dev/null || true
