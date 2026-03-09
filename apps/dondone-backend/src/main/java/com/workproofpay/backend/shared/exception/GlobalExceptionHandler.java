package com.workproofpay.backend.shared.exception;

import com.workproofpay.backend.shared.api.ApiResponse;
import com.workproofpay.backend.shared.api.ValidationErrorDetail;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException e) {
        return ApiResponse.error(e.getErrorCode(), e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<ValidationErrorDetail> details = e.getBindingResult().getAllErrors().stream()
                .map(this::toValidationDetail)
                .toList();
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, null, details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException e) {
        List<ValidationErrorDetail> details = e.getConstraintViolations().stream()
                .map(violation -> new ValidationErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, null, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        return ApiResponse.error(ErrorCode.INTERNAL_ERROR);
    }

    private ValidationErrorDetail toValidationDetail(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return new ValidationErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return new ValidationErrorDetail(error.getObjectName(), error.getDefaultMessage());
    }
}
