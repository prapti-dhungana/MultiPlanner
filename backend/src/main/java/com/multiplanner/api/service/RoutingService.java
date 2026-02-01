package com.multiplanner.api.service;

import com.multiplanner.api.client.TflClient;
import com.multiplanner.api.model.Station;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class RoutingService {

    private final TflClient tflClient;
    private final ObjectMapper objectMapper;

    public RoutingService(TflClient tflClient, ObjectMapper objectMapper) {
        this.tflClient = tflClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolve a Station -> TfL StopPoint id using TfL StopPoint search.
     * MVP heuristic: pick the first match.
     */
    public String resolveStopPointId(Station station) {
        try {
            String json = tflClient.searchStopPoints(station.getName());
            JsonNode root = objectMapper.readTree(json);

            JsonNode matches = root.get("matches");
            if (matches == null || !matches.isArray() || matches.size() == 0) {
                throw new IllegalArgumentException("No TfL StopPoint match for: " + station.getName());
            }

            // First match is good enough for MVP
            return matches.get(0).get("id").asText();

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve TfL StopPoint for " + station.getName(), e);
        }
    }

    public String routeStationToStation(Station from, Station to) {
        String fromId = resolveStopPointId(from);
        String toId = resolveStopPointId(to);
        return tflClient.journeyResults(fromId, toId);
    }
}
