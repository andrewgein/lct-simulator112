package com.simulator112.incident.adapter.in.rest;

import com.simulator112.incident.domain.common.exception.ClassifierEntryNotFoundException;
import com.simulator112.incident.domain.common.exception.IncidentNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class IncidentExceptionHandler {
    @ExceptionHandler({IncidentNotFoundException.class, ClassifierEntryNotFoundException.class})
    ResponseEntity<ErrorResponse> notFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(Instant.now(), status.value(), message));
    }

    record ErrorResponse(Instant timestamp, int status, String message) {
    }
}
