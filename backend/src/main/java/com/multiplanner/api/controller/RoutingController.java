package com.multiplanner.api.controller;

import com.multiplanner.api.model.Station;
import com.multiplanner.api.service.RoutingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/route")
    public String route(@RequestBody RouteRequest request) {
        return routingService.routeStationToStation(request.from(), request.to());
    }

    public record RouteRequest(Station from, Station to) {}
}
