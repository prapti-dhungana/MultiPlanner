package com.multiplanner.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multiplanner.api.client.TflClient;
import com.multiplanner.api.model.Station;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        if (station == null || station.getName() == null || station.getName().isBlank()) {
            throw new IllegalArgumentException("Station name is required to resolve StopPoint id");
        }

        try {
            String json = tflClient.searchStopPoints(station.getName());
            JsonNode root = objectMapper.readTree(json);

            JsonNode matches = root.get("matches");
            if (matches == null || !matches.isArray() || matches.size() == 0) {
                throw new IllegalArgumentException("No TfL StopPoint match for: " + station.getName());
            }

            JsonNode first = matches.get(0);
            JsonNode id = first.get("id");
            if (id == null || id.asText().isBlank()) {
                throw new IllegalArgumentException("TfL StopPoint match missing id for: " + station.getName());
            }

            return id.asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve TfL StopPoint for " + station.getName(), e);
        }
    }

    /**
     * Single-leg TfL routing from -> to.
     */
    public String routeStationToStation(Station from, Station to) {
        String fromId = resolveStopPointId(from);
        String toId = resolveStopPointId(to);
        return tflClient.journeyResults(fromId, toId);
    }

    /**
     * Multi-stop routing: chains TfL JourneyResults for each adjacent pair.
     * Input: [A, B, C, D]
     * Output: legs A->B, B->C, C->D
     */
    public String routeMulti(List<Station> stops) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 stops");
        }

        // Ensure no nulls sneak in
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i) == null) {
                throw new IllegalArgumentException("Stop at index " + i + " is null");
            }
        }

        // Resolve all ids once (saves repeated searches if a stop is reused)
        List<String> ids = new ArrayList<>();
        for (Station s : stops) {
            ids.add(resolveStopPointId(s));
        }

        try {
            ArrayNode results = objectMapper.createArrayNode();

            for (int i = 0; i < stops.size() - 1; i++) {
                Station from = stops.get(i);
                Station to = stops.get(i + 1);

                String fromId = ids.get(i);
                String toId = ids.get(i + 1);

                String journeyJson = tflClient.journeyResults(fromId, toId);

                ObjectNode leg = objectMapper.createObjectNode();
                leg.put("fromName", from.getName());
                leg.put("toName", to.getName());
                leg.put("fromStopPointId", fromId);
                leg.put("toStopPointId", toId);
                leg.set("journey", objectMapper.readTree(journeyJson));

                results.add(leg);
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("mode", "multi");
            response.put("legs", stops.size() - 1);
            response.set("results", results);

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build multi-route response", e);
        }
    }
}
