package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.application.exception.ReviewCommentForbiddenException;
import com.simulator112.review_service.application.exception.ReviewNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ReviewExceptionHandler {
    @ExceptionHandler(ReviewNotFoundException.class)
    ResponseEntity<Map<String, Object>> handleNotFound(ReviewNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "timestamp", Instant.now().toString(), "message", exception.getMessage()));
    }

    @ExceptionHandler(ReviewCommentForbiddenException.class)
    ResponseEntity<Map<String, Object>> handleForbidden(ReviewCommentForbiddenException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "timestamp", Instant.now().toString(), "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> handle(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "timestamp", Instant.now().toString(), "message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, Object>> handleState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "timestamp", Instant.now().toString(), "message", exception.getMessage()));
    }
}
