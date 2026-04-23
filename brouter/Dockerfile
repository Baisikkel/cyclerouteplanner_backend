# BRouter bicycle routing engine — we run our own copy so we do not depend
# on the public brouter.de service, and so we can customise routing later.
#
# Source: https://github.com/abrensch/brouter (MIT license, free to use)
# Listens on port 17777 (the BRouter default).
#
# To use a newer BRouter release: change the BROUTER_VERSION number below
# and rebuild the image. After an upgrade, compare the four .brf files in
# deploy/brouter/profiles/ against the upstream ones for the new version —
# the next ticket will customise those files, and an upgrade must not
# silently overwrite our customisations.
FROM eclipse-temurin:21-jre-alpine

ARG BROUTER_VERSION=1.7.8

ENV BROUTER_VERSION=${BROUTER_VERSION} \
    BROUTER_HOME=/opt/brouter \
    BROUTER_PORT=17777 \
    BROUTER_MAX_THREADS=1 \
    JAVA_OPTS="-Xmx256m -Xms64m -DmaxRunningTime=300 -DuseRFCMimeType=false"

WORKDIR /opt

# Runtime dependency: nc (netcat) is used by the docker-compose healthcheck
# to confirm the port is open. Alpine's busybox ships a minimal nc, but
# installing netcat-openbsd explicitly makes the dependency visible in the
# Dockerfile and survives any future change of the base image.
RUN apk add --no-cache netcat-openbsd

# Download the release zip from GitHub and extract it to /opt/brouter.
# The zip contains a JAR file whose filename includes the version
# number; we add a short-name link (symlink) "brouter.jar" pointing at
# it so entrypoint.sh does not have to know the current version.
RUN apk add --no-cache --virtual .build-deps curl unzip \
 && curl -fsSL -o /tmp/brouter.zip \
      "https://github.com/abrensch/brouter/releases/download/v${BROUTER_VERSION}/brouter-${BROUTER_VERSION}.zip" \
 && unzip -q /tmp/brouter.zip -d /tmp \
 && mv "/tmp/brouter-${BROUTER_VERSION}" "${BROUTER_HOME}" \
 && ln -s "${BROUTER_HOME}/brouter-${BROUTER_VERSION}-all.jar" "${BROUTER_HOME}/brouter.jar" \
 && rm /tmp/brouter.zip \
 && apk del .build-deps

# Three folders where the container expects to find data at runtime. The
# docker-compose file attaches host folders (or empty ones) to these
# paths so brouter can read them:
#   /segments       -> map data files (.rd5) from fetch-segments.sh
#   /profiles       -> routing profiles (.brf) from deploy/brouter/profiles/
#   /build-input    -> exported merged OSM+Tallinn edges from backend
#   /customprofiles -> empty for now; reserved for a future feature where
#                      the frontend sends custom routing rules per request
RUN mkdir -p /segments /profiles /build-input /customprofiles

COPY entrypoint.sh /usr/local/bin/entrypoint.sh
COPY build-segments-with-pseudotags.sh /usr/local/bin/build-segments-with-pseudotags.sh
RUN chmod +x /usr/local/bin/entrypoint.sh /usr/local/bin/build-segments-with-pseudotags.sh

EXPOSE 17777
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
