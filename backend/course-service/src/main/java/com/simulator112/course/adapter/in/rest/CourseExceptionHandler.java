package com.simulator112.course.adapter.in.rest;

import com.simulator112.course.domain.exception.AssignmentLockedException;
import com.simulator112.course.domain.exception.CourseAccessDeniedException;
import com.simulator112.course.domain.exception.CourseNotFoundException;
import com.simulator112.course.domain.exception.EnrollmentNotFoundException;
import com.simulator112.course.domain.exception.IncidentNotFoundException;
import com.simulator112.course.domain.exception.StudyGroupNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class CourseExceptionHandler {
    @ExceptionHandler({CourseNotFoundException.class, StudyGroupNotFoundException.class,
            EnrollmentNotFoundException.class, IncidentNotFoundException.class})
    ResponseEntity<ErrorResponse> notFound(RuntimeException exception) {
        return response(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(CourseAccessDeniedException.class)
    ResponseEntity<ErrorResponse> forbidden(RuntimeException exception) {
        return response(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AssignmentLockedException.class)
    ResponseEntity<ErrorResponse> conflict(RuntimeException exception) {
        return response(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(Instant.now(), status.value(), message));
    }

    record ErrorResponse(Instant timestamp, int status, String message) {
    }
}
