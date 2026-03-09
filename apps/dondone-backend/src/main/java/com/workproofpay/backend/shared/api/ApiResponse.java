package com.workproofpay.backend.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workproofpay.backend.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        String code,
        String message,
        T data,
        Object details
) {

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(body(SuccessCode.SUCCESS.name(), null, data, null));
    }

    public static ResponseEntity<ApiResponse<Void>> success() {
        return ResponseEntity.ok(body(SuccessCode.SUCCESS.name(), null, null, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(body(SuccessCode.CREATED.name(), "Created", data, null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(T data) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(body(SuccessCode.ACCEPTED.name(), null, data, null));
    }

    public static ApiResponse<Void> errorBody(ErrorCode errorCode, String message, Object details) {
        return body(errorCode.getCode(), resolveMessage(errorCode, message), null, details);
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(errorBody(errorCode, null, null));
    }

    public static ResponseEntity<ApiResponse<Void>> error(ErrorCode errorCode, String message, Object details) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(errorBody(errorCode, message, details));
    }

    private static <T> ApiResponse<T> body(String code, String message, T data, Object details) {
        return new ApiResponse<>(code, message, data, details);
    }

    private static String resolveMessage(ErrorCode errorCode, String message) {
        return message != null ? message : errorCode.getMessage();
    }
}
