package com.cyclerouteplanner.backend.features.routing.domain;

import java.util.List;

public interface RouteOptionPort {

    int rebuildFromGeoCaches();

    List<RouteOptionRecord> findActive(int limit);
}
