package com.multiplanner.api.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Health-check endpoint.
 * Used by:
 * - frontend to verify backend availability
 * - local/dev debugging
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "service", "multiplanner-api",
                "timestamp", Instant.now().toString()
        );
    }
}
