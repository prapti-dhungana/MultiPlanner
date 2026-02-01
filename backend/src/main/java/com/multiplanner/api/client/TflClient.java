package com.multiplanner.api.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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
        this.baseUrl = baseUrl;
        this.appKey = appKey;
        this.restClient = RestClient.create();
    }

    /** Search TfL StopPoints by a station name (e.g., "Lewisham"). */
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

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
        }

}
