package com.cyclerouteplanner.backend.features.address.domain;

public interface AdsGatewayPort {

    String fetchStatus();

    String search(String query, int limit);
}
