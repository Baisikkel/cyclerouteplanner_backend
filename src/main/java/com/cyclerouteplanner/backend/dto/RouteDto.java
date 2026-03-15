package com.cyclerouteplanner.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RouteDto {

    private final Long id;
    private final String startLocation;
    private final String endLocation;
    private final double distanceKm;
}
