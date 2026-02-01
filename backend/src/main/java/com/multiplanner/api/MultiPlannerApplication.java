package com.multiplanner.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
/**
 * Application entry point.
 * - Enables Spring Boot auto-configuration and application-wide caching
 */

@SpringBootApplication
@EnableCaching
public class MultiPlannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiPlannerApplication.class, args);
    }
}
