package com.cyclerouteplanner.backend.features.routing.domain;

import java.util.List;
import java.util.Optional;

public interface RouteOptionPort {

    int rebuildFromGeoCaches();

    List<RouteOptionRecord> findActive(int limit);

    Optional<String> findBestProfileHint(double startLat, double startLon, double endLat, double endLon);
}
