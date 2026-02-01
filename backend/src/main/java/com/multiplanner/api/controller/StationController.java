package com.multiplanner.api.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.multiplanner.api.model.Station;
import com.multiplanner.api.service.StationService;

@RestController
public class StationController{
    private final StationService stationService;

    public StationController(StationService stationService){
        this.stationService = stationService;
    }

    @GetMapping("/api/stations")
    public List<Station> searchStations(@RequestParam(required = false) String query){
        return stationService.searchStations(query);
    }

}