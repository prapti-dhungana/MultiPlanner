package com.multiplanner.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class TflClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final String appKey;

    public TflClient(
            @Value("${tfl.base-url}") String baseUrl,
            @Value("${tfl.app-key}") String appKey
    ) {

        if (baseUrl == null || baseUrl.isBlank() || appKey == null || appKey.isBlank()) {
            throw new IllegalStateException("TfL base URL and app key must be configured");
        }

        this.baseUrl = baseUrl;
        this.appKey = appKey;
        this.restClient = RestClient.create();
    }

    //Search TfL StopPoints by a station name
    public String searchStopPoints(String query) {
        String url = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/StopPoint/Search/{query}")
                .queryParam("app_key", appKey)
                .buildAndExpand(query)
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    // Convert TfL 4xx into a readable IllegalArgumentException 
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException("TfL StopPoint search rejected the request");
                    })
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // keep message readable for the API client
            throw new IllegalArgumentException("TfL StopPoint search failed: HTTP " + e.getStatusCode(), e);
        }
    }

    // Journey planner between two stop ids
    public String journeyResults(String fromStopId, String toStopId, String modesCsv) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(baseUrl)
                .path("/Journey/JourneyResults/{from}/to/{to}")
                .queryParam("app_key", appKey);

        if (modesCsv != null && !modesCsv.isBlank()) {
            builder.queryParam("mode", modesCsv);
        }

        String url = builder
                .buildAndExpand(fromStopId, toStopId)
                .toUriString();

        try {
            return restClient.get()
                    .uri(url)
                    .retrieve()
                    // Convert TfL 4xx into a readable IllegalArgumentException 
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new IllegalArgumentException("TfL JourneyResults rejected the request");
                    })
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // keep message readable for the API client
            throw new IllegalArgumentException(
                    "TfL JourneyResults failed: HTTP " + e.getStatusCode()
                            + " (from=" + fromStopId + ", to=" + toStopId + ")",
                    e
            );
        }
    }

}
