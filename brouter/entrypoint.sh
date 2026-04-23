#!/bin/sh
# Runs when the brouter container starts up.
#
# Two jobs:
#   1. Make sure /profiles/lookups.dat exists. This is a small data file
#      that brouter needs next to every .brf routing profile (it tells
#      brouter how to read OpenStreetMap tag data). We do not commit it
#      to git because it must match the exact brouter engine version -
#      the image ships with the right copy, and we put it in place here
#      on first startup.
#   2. Launch the brouter routing server.
#
# The java command at the bottom matches the one upstream uses:
#   https://github.com/abrensch/brouter/blob/master/misc/scripts/standalone/server.sh
set -eu

if [ ! -f /profiles/lookups.dat ]; then
  echo "[brouter-entrypoint] copying lookups.dat into /profiles (brouter ${BROUTER_VERSION})"
  cp "${BROUTER_HOME}/profiles2/lookups.dat" /profiles/lookups.dat
fi

# Safety check - if /profiles has no .brf files, someone forgot to
# attach the deploy/brouter/profiles/ folder to this container.
if ! ls /profiles/*.brf >/dev/null 2>&1; then
  echo "[brouter-entrypoint] ERROR: no .brf files found in /profiles" >&2
  echo "[brouter-entrypoint] attach deploy/brouter/profiles/ to /profiles and try again" >&2
  exit 1
fi

# Arguments expected by RouteServer, in order:
#   segments folder, profiles folder, custom-profiles folder,
#   port number, max number of parallel requests.
exec java $JAVA_OPTS \
  -cp "${BROUTER_HOME}/brouter.jar" \
  btools.server.RouteServer \
  /segments /profiles /customprofiles \
  "${BROUTER_PORT}" "${BROUTER_MAX_THREADS}"
