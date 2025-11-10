// -----------------------------------------------------------------------------
// File: ApiExceptionHandler.java
// -----------------------------------------------------------------------------
package com.projects.instagram.config;


import com.projects.instagram.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;


import java.io.IOException;
import java.util.Map;


@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {


    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", ex.getMessage()));
    }


    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", ex.getMessage()));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", "Conflict: " + ex.getMostSpecificCause().getMessage()));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAll(Exception ex, HttpServletRequest req) {
        // Unwrap causes to detect client-abort / async-unusable
        Throwable cause = ex;
        while (cause != null) {
            // Tomcat-specific client abort
            if (cause instanceof org.apache.catalina.connector.ClientAbortException
                    || cause instanceof AsyncRequestNotUsableException
                    || (cause instanceof IOException && cause.getMessage() != null && (
                    cause.getMessage().contains("An established connection was aborted")
                            || cause.getMessage().toLowerCase().contains("broken pipe")))) {

                // client disconnected while server was writing.
                log.debug("Client aborted connection while serving {} : {}", req.getRequestURI(), cause.toString());
                // Nothing to send — return empty 204 (or simply empty ResponseEntity)
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
            cause = cause.getCause();
        }

        // For other exceptions, return a JSON body and explicitly set JSON content type so
        // the framework doesn't attempt to use the earlier content-type (e.g. video/mp4).
        log.error("Unhandled exception for request {}: {}", req.getRequestURI(), ex.toString(), ex);
        Map<String,Object> body = Map.of(
                "error", ex.getMessage() == null ? "unexpected_error" : ex.getMessage(),
                "path", req.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }
}