package com.multiplanner.api.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global API exception handler.
 * Keeps frontend errors clear and consistent.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return errorBody("bad_request", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneric(Exception ex, HttpServletRequest req) {
        return errorBody(
            "internal_error",
            "Something went wrong while planning the route.",
            req.getRequestURI()
        );
    }

    private Map<String, Object> errorBody(String code, String message, String path) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("error", code);
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntime(RuntimeException ex, HttpServletRequest req) {
        if (ex.getCause() instanceof IllegalArgumentException iae) {
            return errorBody("bad_request", iae.getMessage(), req.getRequestURI());
        }
        // otherwise let it fall through as 500:
        return errorBody("internal_error", "Something went wrong while planning the route.", req.getRequestURI());
    }

}
