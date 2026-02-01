package com.multiplanner.api.controller;

import com.multiplanner.api.model.Station;
import com.multiplanner.api.service.RoutingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    // Single-leg (From -> To)
    @PostMapping("/route")
    public String route(@RequestBody RouteRequest request) {
        return routingService.routeStationToStation(request.from(), request.to());
    }

    // Multi-leg (From -> Stop1 -> Stop2 -> To)
    @PostMapping("/route/multi")
    public String routeMulti(@RequestBody MultiRouteRequest request) {
        return routingService.routeMulti(request.stops());
    }

    public record RouteRequest(Station from, Station to) {}

    public record MultiRouteRequest(List<Station> stops) {}
}
