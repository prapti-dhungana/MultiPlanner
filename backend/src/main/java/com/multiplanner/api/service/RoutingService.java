package com.multiplanner.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.multiplanner.api.controller.RoutingController;
import com.multiplanner.api.model.Station;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Routing domain service.
 *  - convert Station names to TfL StopPoint IDs 
 *  - fetch TfL JourneyResults 
 *  - select the best journey based on sorting/mode filters.
 */
@Service
public class RoutingService {

    private final ObjectMapper objectMapper;
    private final JourneyCacheService journeyCacheService;

    public RoutingService(ObjectMapper objectMapper, JourneyCacheService journeyCacheService) {
        this.objectMapper = objectMapper;
        this.journeyCacheService = journeyCacheService;
    }
    
  //Resolve a Station into a TfL StopPoint ID.
    public String resolveStopPointId(Station station) {
        if (station == null) {
            throw new IllegalArgumentException("Station is required");
        }

        // Prefer DB-backed StopPoint 
        if (station.getCode() != null && !station.getCode().isBlank()) {
            return station.getCode();
        }

        //  No code, then fall back to TfL search by name
        if (station.getName() == null || station.getName().isBlank()) {
            throw new IllegalArgumentException("Station name is required");
        }

        try {
            String json = journeyCacheService.cachedStopPointSearch(station.getName());
            JsonNode root = objectMapper.readTree(json);

            JsonNode matches = root.get("matches");
            if (matches == null || !matches.isArray() || matches.isEmpty()) {
                throw new IllegalArgumentException(
                    "No TfL StopPoint match for " + station.getName()
                );
            }

            JsonNode id = matches.get(0).get("id");
            if (id == null || id.asText().isBlank()) {
                throw new IllegalArgumentException(
                    "TfL StopPoint match missing id for " + station.getName()
                );
            }

            return id.asText();

        } catch (IllegalArgumentException e) {
            throw e; 
        } catch (Exception e) {
            throw new RuntimeException(
                "Failed to resolve TfL StopPoint for " + station.getName(), e
            );
        }
    }


    //Single leg TfL routing (from) -> (to).
    public String routeStationToStation(Station from, Station to) {
        String fromId = resolveStopPointId(from);
        String toId = resolveStopPointId(to);

        // Use a 5-minute bucket for caching 
        String departAtRounded5 = roundNowTo5MinKey();

        //Default options for single-leg include everything + fastest
        RouteOptions options = new RouteOptions(RoutingController.SortBy.FASTEST, true, true);
        String modesCsv = buildModesCsv(options.includeBus(), options.includeTram());

        try {
            ObjectNode summary = routeLegSummary(from, to, fromId, toId, departAtRounded5, options, modesCsv);
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            // If the real cause is "bad input" (or TfL returned no journeys), surface it as 400
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            if (e.getCause() instanceof IllegalArgumentException) throw (IllegalArgumentException) e.getCause();

            throw new RuntimeException("Failed to build multi-route response", e);
        }
    }

    //Chains TfL JourneyResults for each adjacent pair.
    public String routeMulti(
            List<Station> stops,
            RoutingController.Preferences preferences,
            RoutingController.Modes modes
    ) {
        if (stops == null || stops.size() < 2) {
            throw new IllegalArgumentException("Need at least 2 stops");
        }

        // Ensures no null values
        for (int i = 0; i < stops.size(); i++) {
            if (stops.get(i) == null) {
                throw new IllegalArgumentException("Stop at index " + i + " is null");
            }
        }

        // Sorting default = fastest
        RoutingController.SortBy sortBy = (preferences != null && preferences.sortBy() != null)
                ? preferences.sortBy()
                : RoutingController.SortBy.FASTEST;

        // Mode defaults= include everything until UI provides toggles
        boolean includeBus = (modes == null || modes.includeBus() == null) ? true : modes.includeBus();
        boolean includeTram = (modes == null || modes.includeTram() == null) ? true : modes.includeTram();

        RouteOptions options = new RouteOptions(sortBy, includeBus, includeTram);
        String modesCsv = buildModesCsv(includeBus, includeTram);

        // Resolve all ids at once 
        List<String> ids = new ArrayList<>();
        for (Station s : stops) {
            ids.add(resolveStopPointId(s));
        }

        // keeps cache keys consistent across legs in the same button click
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

                ObjectNode legSummary = routeLegSummary(from, to, fromId, toId, departAtRounded5, options, modesCsv);

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
            // If the real cause is "bad input" (or TfL returned no journeys), surface it as 400
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            if (e.getCause() instanceof IllegalArgumentException) throw (IllegalArgumentException) e.getCause();

            throw new RuntimeException("Failed to build multi-route response", e);
        }

    }

    // HELPERS
    // Options used to choose the best journey from the TfL API response.
    private record RouteOptions(
            RoutingController.SortBy sortBy,
            boolean includeBus,
            boolean includeTram
    ) {}

    //Core helper used to build a singular leg
    private ObjectNode routeLegSummary(
            Station from,
            Station to,
            String fromId,
            String toId,
            String departAtRounded5,
            RouteOptions options,
            String modesCsv
    ) throws Exception {
        String journeyJson = journeyCacheService.journeyResults(fromId, toId, departAtRounded5, modesCsv);

        return buildLegSummary(
                from.getName(),
                to.getName(),
                fromId,
                toId,
                journeyJson,
                options.sortBy(),
                options.includeBus(),
                options.includeTram()
        );
    }

    //Builds a smaller leg summary from the raw TfL JourneyResults JSON
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


     //Estimate interchanges by counting non-walking legs and subtracting 1.
    private int estimateInterchanges(JsonNode legs) {
        if (legs == null || !legs.isArray() || legs.size() == 0) return 0;

        int nonWalk = 0;
        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("");
            if (!"walking".equalsIgnoreCase(mode)) {
                nonWalk++;
            }
        }

        if (nonWalk <= 1) return 0;
        return nonWalk - 1;
    }

     //Build a simple readable summary from the legs.
    private String buildSummaryFromLegs(JsonNode legs) {
        if (legs == null || !legs.isArray() || legs.size() == 0) return "Journey";

        for (JsonNode leg : legs) {
            String mode = leg.path("mode").path("id").asText("");
            if ("walking".equalsIgnoreCase(mode)) continue;

            JsonNode routeOptions = leg.path("routeOptions");
            if (routeOptions.isArray() && routeOptions.size() > 0) {
                String name = routeOptions.get(0).path("name").asText("");
                if (!name.isBlank()) return name;
            }
        }

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

    // Round "now" to a 5-minute bucket for cache keys.
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

    /**
     * Select the "best" journey from TfL options according to:
     *  - allowed modes 
     *  - sorting preference 
     */
    private JsonNode pickBestJourney(
            ArrayNode journeys,
            RoutingController.SortBy sortBy,
            boolean includeBus,
            boolean includeTram
    ) {
        JsonNode best = null;

        for (JsonNode candidate : journeys) {
            if (!journeyAllowedByModes(candidate, includeBus, includeTram)) {
                continue;
            }

            if (best == null) {
                best = candidate;
                continue;
            }

            int durCandidate = candidate.path("duration").asInt(Integer.MAX_VALUE);
            int durBest = best.path("duration").asInt(Integer.MAX_VALUE);

            int transfersCandidate = estimateInterchanges(candidate.get("legs"));
            int transfersBest = estimateInterchanges(best.get("legs"));

            if (sortBy == RoutingController.SortBy.FEWEST_TRANSFERS) {
                if (transfersCandidate < transfersBest
                        || (transfersCandidate == transfersBest && durCandidate < durBest)) {
                    best = candidate;
                }
            } else {
                if (durCandidate < durBest
                        || (durCandidate == durBest && transfersCandidate < transfersBest)) {
                    best = candidate;
                }
            }
        }

        if (best == null) {
            throw new IllegalArgumentException("No journeys matched mode filters (bus/tram).");
        }

        return best;
    }


     //Returns false if a journey contains a leg with a mode that is currently excluded.
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

     //Build the TfL mode query parameter.
    private String buildModesCsv(boolean includeBus, boolean includeTram) {
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
