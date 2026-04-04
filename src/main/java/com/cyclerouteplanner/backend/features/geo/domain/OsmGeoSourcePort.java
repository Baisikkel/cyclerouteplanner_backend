package com.cyclerouteplanner.backend.features.geo.domain;

public interface OsmGeoSourcePort {

    String fetchCycleNetwork(double south, double west, double north, double east, int timeoutSeconds);
}
