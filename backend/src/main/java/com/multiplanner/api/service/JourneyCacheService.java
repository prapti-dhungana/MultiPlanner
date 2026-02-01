package com.multiplanner.api.service;

import com.multiplanner.api.client.TflClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class JourneyCacheService {

    private final TflClient tflClient;

    public JourneyCacheService(TflClient tflClient) {
        this.tflClient = tflClient;
    }

    @Cacheable(
        cacheNames = "journeys",
        key = "'journey:from:' + #fromId + ':to:' + #toId + ':departAt:' + #departAtRounded5"
    )
    public String journeyResults(String fromId, String toId, String departAtRounded5) {
        // call TfL once; result is cached
        return tflClient.journeyResults(fromId, toId);
    }

    @Cacheable(
        cacheNames = "stopPoints",
        key = "'stopPoint:' + #stationName.toLowerCase()"
    )
    public String cachedStopPointSearch(String stationName) {
        return tflClient.searchStopPoints(stationName);
    }

}
