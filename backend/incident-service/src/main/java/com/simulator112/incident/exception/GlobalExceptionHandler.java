package com.simulator112.incident.exception;

import com.simulator112.incident.exception.classifier.ClassifierEntryNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            IncidentNotFoundException.class,
            ClassifierEntryNotFoundException.class,
            ResourceNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, WebRequest request) {
        log.warn("Ресурс не найден: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> fieldErrors.put(err.getField(), err.getDefaultMessage()));
        log.warn("Ошибка валидации: {}", fieldErrors);
        return build(HttpStatus.BAD_REQUEST, "Ошибка валидации", request, fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, WebRequest request) {
        log.error("Необработанное исключение", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера", request, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, WebRequest request,
                                                 Map<String, String> fieldErrors) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", ""),
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}
