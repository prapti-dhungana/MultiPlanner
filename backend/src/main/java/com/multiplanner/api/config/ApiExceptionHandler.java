package com.multiplanner.api.config;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global API exception handler.
 * Keeps frontend errors clear and consistent.#
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleBadRequest(IllegalArgumentException ex, HttpServletRequest req) {
        return errorBody("bad_request", ex.getMessage(), req.getRequestURI());
    }

    // If someone opens a missing path, return 404 
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(NoResourceFoundException ex, HttpServletRequest req) {
        return errorBody("not_found", "Endpoint not found", req.getRequestURI());
    }

    // Catch-all for real server failures
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneric(Exception ex, HttpServletRequest req) {
        return errorBody(
            "internal_error",
            "Something went wrong on the server.",
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
}
