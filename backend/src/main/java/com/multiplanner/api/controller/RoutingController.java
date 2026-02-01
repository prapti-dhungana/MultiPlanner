package com.multiplanner.api.controller;

import com.multiplanner.api.model.Station;
import com.multiplanner.api.service.RoutingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
/**
 * REST controller displaying routing endpoints
 *  - Validates request shape using records
 *  - leaves all routing, filtering, and sorting logic to RoutingService
 */
@RestController
@RequestMapping("/api")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    //Single-leg (From -> To)
    @PostMapping("/route")
    public String route(@RequestBody RouteRequest request) {
        return routingService.routeStationToStation(request.from(), request.to());
    }

    //Multi-leg (From -> Stop1 -> Stop2 -> To)
    @PostMapping("/route/multi")
    public String routeMulti(@RequestBody MultiRouteRequest request) {
        return routingService.routeMulti(
            request.stops(),
            request.preferences(),
            request.modes()
        );
    }

    //Request body for single leg route
    public record RouteRequest(Station from, Station to) {}

    //Request body for multi-leg route
    public record MultiRouteRequest(
        List<Station> stops,
        Preferences preferences,
        Modes modes
    ) {}

    public record Preferences(SortBy sortBy) {} 

    public record Modes(Boolean includeBus, Boolean includeTram) {} 

    public enum SortBy { FASTEST, FEWEST_TRANSFERS } 
}
