package com.cyclerouteplanner.backend.service;

import com.cyclerouteplanner.backend.dto.RouteDto;
import com.cyclerouteplanner.backend.repository.RouteRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<RouteDto> getAllRoutes() {
        // Placeholder implementation until real persistence is added.
        return List.of(
                new RouteDto(1L, "Campus", "City Center", 5.2),
                new RouteDto(2L, "City Center", "Beach", 8.7)
        );
    }
}
