package com.workproofpay.remittance.controller;

import com.workproofpay.remittance.dto.ErrorResponse;
import com.workproofpay.remittance.service.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        ErrorResponse body = new ErrorResponse(
                new ErrorResponse.ErrorBody(e.getCode(), e.getMessage(), requestId, e.getDetails())
        );
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        Map<String, Object> details = new HashMap<>();
        details.put("fieldErrors", e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ":" + err.getDefaultMessage())
                .toList());

        ErrorResponse body = new ErrorResponse(
                new ErrorResponse.ErrorBody("VALIDATION_ERROR", "Invalid request", requestId, details)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        ErrorResponse body = new ErrorResponse(
                new ErrorResponse.ErrorBody("INVALID_ARGUMENT", e.getMessage(), requestId, null)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception e, HttpServletRequest request) {
        String requestId = resolveRequestId(request);
        ErrorResponse body = new ErrorResponse(
                new ErrorResponse.ErrorBody("UNKNOWN", "Unexpected server error", requestId, null)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String header = request.getHeader("X-Request-Id");
        if (header == null || header.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return header;
    }
}
