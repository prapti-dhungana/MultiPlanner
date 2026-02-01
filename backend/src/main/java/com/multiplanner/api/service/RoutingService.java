package com.multiplanner.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multiplanner.api.client.TflClient;
import com.multiplanner.api.model.Station;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class RoutingService {

    private final TflClient tflClient;
    private final ObjectMapper objectMapper;
    private final JourneyCacheService journeyCacheService;

    public RoutingService(TflClient tflClient, ObjectMapper objectMapper, JourneyCacheService journeyCacheService) {
        this.tflClient = tflClient;
        this.objectMapper = objectMapper;
        this.journeyCacheService = journeyCacheService;
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
            String json = journeyCacheService.cachedStopPointSearch(station.getName());
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

        // Cache key includes a "departAt" bucket rounded to 5 minutes
        // so repeated calls within that window don't hammer TfL.
        String departAtRounded5 = roundNowTo5MinKey();

        // call TfL once; result is cached
        return journeyCacheService.journeyResults(fromId, toId, departAtRounded5);
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

        // Use one rounded departAt bucket for the whole multi-leg request
        // (keeps cache keys consistent across legs in the same button click).
        String departAtRounded5 = roundNowTo5MinKey();

        try {
            ArrayNode results = objectMapper.createArrayNode();

            for (int i = 0; i < stops.size() - 1; i++) {
                Station from = stops.get(i);
                Station to = stops.get(i + 1);

                String fromId = ids.get(i);
                String toId = ids.get(i + 1);

                // Use cached journey results per leg (fromId -> toId)
                String journeyJson = journeyCacheService.journeyResults(fromId, toId, departAtRounded5);

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

    /**
     * Helper: round "now" to a 5-minute bucket for cache keys.
     * Example output: 2026-02-01T08:25Z
     */
    private String roundNowTo5MinKey() {
        ZonedDateTime now = ZonedDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        int minute = now.getMinute();
        int rounded = (minute / 5) * 5;

        ZonedDateTime roundedTime = now
            .withMinute(rounded)
            .withSecond(0)
            .withNano(0);

        return roundedTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm'Z'"));
    }
}
