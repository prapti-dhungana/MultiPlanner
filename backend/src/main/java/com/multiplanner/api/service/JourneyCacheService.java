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
        key = "'journey:from:' + #fromId + ':to:' + #toId + ':departAt:' + #departAtRounded5 + ':modes:' + (#modesCsv == null ? '' : #modesCsv)"
    )
    public String journeyResults(String fromId, String toId, String departAtRounded5, String modesCsv) {
        // departAtRounded5 is used only for caching bucketing
        return tflClient.journeyResults(fromId, toId, modesCsv);
    }

    @Cacheable(
        cacheNames = "stopPoints",
        key = "'stopPoint:' + #stationName.toLowerCase()"
    )
    public String cachedStopPointSearch(String stationName) {
        return tflClient.searchStopPoints(stationName);
    }
}
