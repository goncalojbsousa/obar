package com.obar.web.maps;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = MapsApiController.class)
public class MapsApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        String message = exception.getReason() == null || exception.getReason().isBlank()
                ? "Não foi possível completar o pedido."
                : exception.getReason();
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ApiError(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Não foi possível completar o pedido."));
    }

    private record ApiError(String message) {
    }
}
