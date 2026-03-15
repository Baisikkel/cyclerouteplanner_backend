package com.cyclerouteplanner.backend.controller;

import com.cyclerouteplanner.backend.dto.RouteDto;
import com.cyclerouteplanner.backend.service.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<RouteDto> getAllRoutes() {
        return routeService.getAllRoutes();
    }
}
