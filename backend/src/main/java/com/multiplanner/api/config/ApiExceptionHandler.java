package com.multiplanner.api.config;

import java.time.Instant;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralised API exception handling.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    //Client errors (bad input, no journeys, invalid station, etc.)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "error", "bad_request",
                        "message", ex.getMessage()
                ));
    }

    //Anything unexpected is a server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleServerError(
            Exception ex
    ) {
        ex.printStackTrace(); // IMPORTANT during development

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "error", "internal_error",
                        "message", "Something went wrong on the server."
                ));
    }
}
