package com.obar.web.config;

import com.obar.web.driver.DriverApiController;
import com.obar.web.maps.MapsApiController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = { MapsApiController.class, DriverApiController.class })
public class ApiExceptionHandler {

    private static final String DEFAULT_MESSAGE = "N\u00E3o foi poss\u00EDvel completar o pedido.";

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
        return ResponseEntity
                .status(isPinError(exception) ? HttpStatus.BAD_REQUEST : HttpStatus.CONFLICT)
                .body(new ApiError(messageOrDefault(exception.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected() {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(DEFAULT_MESSAGE));
    }

    private boolean isPinError(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && message.toLowerCase().contains("pin");
    }

    private String messageOrDefault(String message) {
        return message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
    }

    private record ApiError(String message) {
    }
}
