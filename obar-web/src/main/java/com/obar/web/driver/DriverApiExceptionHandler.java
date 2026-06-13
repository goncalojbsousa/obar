package com.obar.web.driver;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = DriverApiController.class)
public class DriverApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiError(messageOrDefault(exception.getReason())));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity
                .badRequest()
                .body(new ApiError(messageOrDefault(exception.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleStateConflict(IllegalStateException exception) {
        HttpStatus status = isPinError(exception) ? HttpStatus.BAD_REQUEST : HttpStatus.CONFLICT;
        return ResponseEntity
                .status(status)
                .body(new ApiError(messageOrDefault(exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Não foi possível completar o pedido."));
    }

    private boolean isPinError(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("pin");
    }

    private String messageOrDefault(String message) {
        return message == null || message.isBlank()
                ? "Não foi possível completar o pedido."
                : message;
    }

    private record ApiError(String message) {
    }
}
