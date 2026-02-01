package com.multiplanner.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Component
public class TflClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String appKey;

    public TflClient(
            @Value("${tfl.base-url}") String baseUrl,
            @Value("${tfl.app-key}") String appKey
    ) {
        this.baseUrl = baseUrl;
        this.appKey = appKey;
        this.restClient = RestClient.create();
    }

    /** Search TfL StopPoints by a station name */
    public String searchStopPoints(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/StopPoint/Search/{query}")
                .queryParam("app_key", appKey)
                .buildAndExpand(query)
                .toUriString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }

    /** Journey planner between two TfL stop ids (StopPoint ids). */
    public String journeyResults(String fromStopId, String toStopId) {
        // Restrict modes 
        List<String> allowedModes = List.of(
                "national-rail",
                "overground",
                "elizabeth-line",
                "tube",
                "dlr",
                "tram",
                "walking"
        );

        UriComponentsBuilder b = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/Journey/JourneyResults/{from}/to/{to}")
                .queryParam("app_key", appKey);

        // Important: add multiple mode params (?mode=a&mode=b&...)
        for (String m : allowedModes) {
            b.queryParam("mode", m);
        }

        String url = b.buildAndExpand(fromStopId, toStopId).toUriString();

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}
