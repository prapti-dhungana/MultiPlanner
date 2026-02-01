package com.multiplanner.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multiplanner.api.model.Station;
import com.multiplanner.api.controller.RoutingController;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Service
public class RoutingService {

    private final ObjectMapper objectMapper;
    private final JourneyCacheService journeyCacheService;

    public RoutingService(ObjectMapper objectMapper, JourneyCacheService journeyCacheService) {
        this.objectMapper = objectMapper;
        this.journeyCacheService = journeyCacheService;
    }

    /**
     * Resolve a Station -> TfL StopPoint id using TfL StopPoint search.
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
         Single-leg TfL routing from -> to.
     */
    public String routeStationToStation(Station from, Station to) {
        String fromId = resolveStopPointId(from);
        String toId = resolveStopPointId(to);

        // Cache key includes a "departAt" bucket rounded to 5 minutes
        // so repeated calls within that window don't hammer TfL.
        String departAtRounded5 = roundNowTo5MinKey();

        // call TfL once; result is cached
        String modesCsv = buildModesCsv(true, true); // include everything for single-leg
        String journeyJson = journeyCacheService.journeyResults(fromId, toId, departAtRounded5, modesCsv);


        // Return a smaller, UI-friendly summary shape
        try {
            ObjectNode summary = buildLegSummary(
                from.getName(),
                to.getName(),
                fromId,
                toId,
                journeyJson,
                RoutingController.SortBy.FASTEST,
                true,
                true
            );

            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build route summary", e);
        }
    }

    /**
     * Multi-stop routing: chains TfL JourneyResults for each adjacent pair.
     * Input: [A, B, C, D]
     * Output: legs A->B, B->C, C->D
     */
    public String routeMulti(
        List<Station> stops,
        RoutingController.Preferences preferences,
        RoutingController.Modes modes
    ) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 stops");
        }

        // Ensure no nulls sneak in
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i) == null) {
                throw new IllegalArgumentException("Stop at index " + i + " is null");
            }
        }
        // defaults
        RoutingController.SortBy sortBy = (preferences != null && preferences.sortBy() != null)
            ? preferences.sortBy()
            : RoutingController.SortBy.FASTEST;

        boolean includeBus = (modes != null && modes.includeBus() != null) && modes.includeBus();
        boolean includeTram = (modes != null && modes.includeTram() != null) && modes.includeTram();
        String modesCsv = buildModesCsv(includeBus, includeTram);


        // Resolve all ids once 
        List<String> ids = new ArrayList<>();
        for (Station s : stops) {
            ids.add(resolveStopPointId(s));
        }

        // Use one rounded departAt bucket for the whole multi-leg request
        // (keeps cache keys consistent across legs in the same button click).
        String departAtRounded5 = roundNowTo5MinKey();

        try {
            ArrayNode legSummaries = objectMapper.createArrayNode();

            int totalDuration = 0;
            int totalInterchanges = 0;

            for (int i = 0; i < stops.size() - 1; i++) {
                Station from = stops.get(i);
                Station to = stops.get(i + 1);

                String fromId = ids.get(i);
                String toId = ids.get(i + 1);

                // Use cached journey results per leg (fromId -> toId)
                String journeyJson = journeyCacheService.journeyResults(fromId, toId, departAtRounded5, modesCsv);

                ObjectNode legSummary = buildLegSummary(from.getName(), to.getName(), fromId, toId, journeyJson, sortBy, includeBus, includeTram);

                totalDuration += legSummary.path("durationMinutes").asInt(0);
                totalInterchanges += legSummary.path("interchanges").asInt(0);

                legSummaries.add(legSummary);
            }

            ObjectNode response = objectMapper.createObjectNode();
            response.put("mode", "multi");
            response.put("legs", stops.size() - 1);
            response.put("totalDurationMinutes", totalDuration);
            response.put("totalInterchanges", totalInterchanges);
            response.set("results", legSummaries);

            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build multi-route response", e);
        }
    }

    /**
     * Build a smaller, UI-friendly leg summary from the raw TfL JourneyResults JSON.
     */
    private ObjectNode buildLegSummary(
        String fromName,
        String toName,
        String fromId,
        String toId,
        String journeyJson,
        RoutingController.SortBy sortBy,
        boolean includeBus,
        boolean includeTram
    ) throws Exception {
        JsonNode root = objectMapper.readTree(journeyJson);

        JsonNode journeys = root.get("journeys");
        if (journeys == null || !journeys.isArray() || journeys.size() == 0) {
            throw new IllegalArgumentException("TfL returned no journeys");
        }

        JsonNode best = pickBestJourney((ArrayNode) journeys, sortBy, includeBus, includeTram);

        int duration = best.path("duration").asInt(0);
        String start = best.path("startDateTime").asText(null);
        String arrive = best.path("arrivalDateTime").asText(null);

        JsonNode legs = best.get("legs");
        int interchanges = estimateInterchanges(legs);

        String summary = buildSummaryFromLegs(legs);

        ArrayNode segments = objectMapper.createArrayNode();
        if (legs != null && legs.isArray()) {
            for (JsonNode leg : legs) {
                ObjectNode seg = objectMapper.createObjectNode();

                String mode = leg.path("mode").path("id").asText(null);

                String line = leg.path("routeOptions").isArray() && leg.path("routeOptions").size() > 0
                    ? leg.path("routeOptions").get(0).path("name").asText(null)
                    : null;

                String direction = leg.path("instruction").path("detailed").asText(null);

                String dep = leg.path("departurePoint").path("commonName").asText(null);
                String arr = leg.path("arrivalPoint").path("commonName").asText(null);

                int segDuration = leg.path("duration").asInt(0);

                seg.put("mode", mode);
                if (line != null && !line.isBlank()) seg.put("line", line);
                if (direction != null && !direction.isBlank()) seg.put("direction", direction);
                if (dep != null && !dep.isBlank()) seg.put("from", dep);
                if (arr != null && !arr.isBlank()) seg.put("to", arr);
                seg.put("durationMinutes", segDuration);

                segments.add(seg);
            }
        }

        ObjectNode out = objectMapper.createObjectNode();
        out.put("fromName", fromName);
        out.put("toName", toName);
        out.put("fromStopPointId", fromId);
        out.put("toStopPointId", toId);
        out.put("durationMinutes", duration);
        if (start != null) out.put("startDateTime", start);
        if (arrive != null) out.put("arrivalDateTime", arrive);
        out.put("interchanges", interchanges);
        out.put("summary", summary);
        out.set("segments", segments);

        return out;
    }


    /**
     * Estimate interchanges.
     * count non-walking legs and subtract 1.
     */
    private int estimateInterchanges(JsonNode legs) {
        if (legs == null || !legs.isArray() || legs.size() == 0) return 0;

        int nonWalk = 0;
        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("");
            if (!"walking".equalsIgnoreCase(mode)) {
                nonWalk++;
            }
        }

        // If only walked, interchanges should be 0.
        if (nonWalk <= 1) return 0;

        return nonWalk - 1;
    }

    /**
     * Build a simple readable summary from the legs.
     */
    private String buildSummaryFromLegs(JsonNode legs) {
        if (legs == null || !legs.isArray() || legs.size() == 0) return "Journey";

        // Prefer the first non-walking leg's route name if available
        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("");
            if ("walking".equalsIgnoreCase(mode)) continue;

            JsonNode routeOptions = leg.path("routeOptions");
            if (routeOptions.isArray() && routeOptions.size() > 0) {
                String name = routeOptions.get(0).path("name").asText("");
                if (!name.isBlank()) return name;
            }
        }

        // Fallback: list modes seen (e.g., "train + tube")
        List<String> modes = new ArrayList<>();
        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("");
            if (mode.isBlank()) continue;
            if (!modes.contains(mode)) modes.add(mode);
        }

        if (modes.isEmpty()) return "Journey";
        if (modes.size() == 1) return modes.get(0);
        return String.join(" + ", modes);
    }

    /**
     * Helper: round "now" to a 5-minute bucket for cache keys.
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

    private JsonNode pickBestJourney(
        ArrayNode journeys,
        RoutingController.SortBy sortBy,
        boolean includeBus,
        boolean includeTram
    ) {
        JsonNode best = null;

        for (JsonNode j : journeys) {
            if (!journeyAllowedByModes(j, includeBus, includeTram)) {
                continue;
            }

            if (best == null) {
                best = j;
                continue;
            }

            int durJ = j.path("duration").asInt(Integer.MAX_VALUE);
            int durBest = best.path("duration").asInt(Integer.MAX_VALUE);

            int intJ = estimateInterchanges(j.get("legs"));
            int intBest = estimateInterchanges(best.get("legs"));

            if (sortBy == RoutingController.SortBy.FEWEST_TRANSFERS) {
                // primary: fewest interchanges, tie-break: shortest duration
                if (intJ < intBest || (intJ == intBest && durJ < durBest)) {
                    best = j;
                }
            } else {
                // FASTEST default: shortest duration, tie-break: fewest interchanges
                if (durJ < durBest || (durJ == durBest && intJ < intBest)) {
                    best = j;
                }
            }
        }

        if (best == null) {
            throw new IllegalArgumentException("No journeys matched mode filters (bus/tram).");
        }

        return best;
    }

    private boolean journeyAllowedByModes(JsonNode journey, boolean includeBus, boolean includeTram) {
        JsonNode legs = journey.get("legs");
        if (legs == null || !legs.isArray()) return true;

        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("").toLowerCase();
            if (!includeBus && "bus".equals(mode)) return false;
            if (!includeTram && "tram".equals(mode)) return false;
        }
        return true;
    }

    private String buildModesCsv(boolean includeBus, boolean includeTram) {
        // "include everything" baseline (rail + walking + bus + tram based on toggles)
        // You can tweak this list later, but this is a sensible TfL set for London.
        List<String> modes = new ArrayList<>();
        modes.add("walking");
        modes.add("tube");
        modes.add("dlr");
        modes.add("overground");
        modes.add("national-rail");
        modes.add("elizabeth-line");

        if (includeBus) modes.add("bus");
        if (includeTram) modes.add("tram");

        return String.join(",", modes);
    }


}
