package com.multiplanner.api.service;

import com.multiplanner.api.model.Station;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public List<Station> searchStations(String query) {
        return stationRepository.search(query);
    }
}
