package com.amir.shop.exception;

import jakarta.validation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse createErrorResponse(Map<String, String> errors) {
        return new ErrorResponse(400, "Ошибка валидации", errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(

            MethodArgumentNotValidException exception
    ) {

        Map<String, String> errors = new HashMap<>();
        List<FieldError> fieldErrors = exception.getBindingResult().getFieldErrors();

        for (FieldError error : fieldErrors) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(

            ConstraintViolationException exception
    ) {

        Map<String, String> errors = new HashMap<>();

        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            String message = violation.getMessage();
            errors.put(violation.getPropertyPath().toString(), message);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(createErrorResponse(errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(

            ResponseStatusException exception
    ) {
        int status = exception.getStatusCode().value();

        ErrorResponse response = new ErrorResponse(

                status,
                exception.getReason(),
                Map.of()
        );

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(response);
    }
}
