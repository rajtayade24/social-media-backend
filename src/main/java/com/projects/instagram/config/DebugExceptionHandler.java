package com.projects.instagram.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

@ControllerAdvice
public class DebugExceptionHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleAll(Exception ex) {
        log.error("Unhandled exception:", ex);
        String trace = Arrays.stream(ex.getStackTrace()).limit(7)
                .map(StackTraceElement::toString).collect(Collectors.joining("\n"));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(),
                        "trace", trace));
    }
}
