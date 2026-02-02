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

    //Single-leg route (From -> To).
    @PostMapping("/route")
    public String route(@RequestBody RouteRequest request) {
        return routingService.routeStationToStation(request.from(), request.to());
    }

    //Multi-leg route (From -> Stop1 -> ... -> To).
    @PostMapping("/route/multi")
    public String routeMulti(@RequestBody MultiRouteRequest request) {
        System.out.println("routeMulti request = " + request);
        return routingService.routeMulti(
                request.stops(),
                request.preferences(),
                request.modes()
        );
    }

    // Request body for single-leg routing.
    public record RouteRequest(Station from, Station to) {}

    //Request body for multi-leg routing.
    public record MultiRouteRequest(
            List<Station> stops,
            Preferences preferences,
            Modes modes
    ) {}

    //Sorting preference for selecting the best journey option returned by TfL.
    public record Preferences(SortBy sortBy) {}

    //Transport mode toggles for filtering TfL journey options.
    public record Modes(Boolean includeBus, Boolean includeTram) {}

    public enum SortBy { FASTEST, FEWEST_TRANSFERS }
}
