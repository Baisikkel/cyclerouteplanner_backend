package com.cyclerouteplanner.backend.features.geo.domain;

public interface TallinnGeoSourcePort {

    String fetchGeoJson(String sourceUrl);
}
