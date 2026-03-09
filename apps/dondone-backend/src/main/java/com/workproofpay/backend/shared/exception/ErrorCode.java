package com.workproofpay.backend.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request"),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "Invalid input value"),
    INVALID_WORK_DATE(HttpStatus.BAD_REQUEST, "workDate must match clockInAt date"),
    INVALID_WORKPROOF_TIME(HttpStatus.BAD_REQUEST, "clockOutAt must be after clockInAt"),
    INCOMPLETE_CLOCK_OUT(HttpStatus.BAD_REQUEST, "clockOutAt, deviceClockOutAt, clockOutLatitude, and clockOutLongitude must be provided together"),
    INVALID_DEVICE_TIME(HttpStatus.BAD_REQUEST, "deviceClockOutAt must be after deviceClockInAt"),
    INVALID_YEAR_MONTH(HttpStatus.BAD_REQUEST, "yearMonth must follow YYYY-MM"),
    INVALID_DEPOSIT_DATE(HttpStatus.BAD_REQUEST, "depositDate must be inside yearMonth"),
    INVALID_HOURLY_WAGE(HttpStatus.BAD_REQUEST, "normalizedHourlyWage must be greater than 0"),
    INVALID_PAYDAY(HttpStatus.BAD_REQUEST, "paydayDay must be between 1 and 31"),
    AS_OF_REQUIRED(HttpStatus.BAD_REQUEST, "asOf is required"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token is invalid or expired"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    WORKPROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "WorkProof not found"),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getCode() {
        return name();
    }
}
