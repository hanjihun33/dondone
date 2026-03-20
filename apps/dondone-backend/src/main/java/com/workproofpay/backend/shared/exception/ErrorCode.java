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
    MODIFICATION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "reasonCode is required"),
    INVALID_MISSING_RECORD_TIME(HttpStatus.BAD_REQUEST, "missing record times must be in chronological order"),
    INVALID_MODIFICATION_TIME(HttpStatus.BAD_REQUEST, "modification times must be in chronological order"),
    UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported file type"),
    REQUEST_AMOUNT_EXCEEDS_LIMIT(HttpStatus.BAD_REQUEST, "Requested amount exceeds the available advance limit"),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key header is required"),
    INVALID_WALLET_ADDRESS(HttpStatus.BAD_REQUEST, "walletAddress must be a valid EVM address"),
    INVALID_VAULT_AMOUNT(HttpStatus.BAD_REQUEST, "Vault amount must be greater than 0"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication is required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You do not have permission to access this resource"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Token is invalid or expired"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid credentials"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),
    WORKPROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "WorkProof not found"),
    WORKPROOF_EDIT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Only reflected WorkProof can be edited"),
    WORKPLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workplace not found"),
    ACTIVE_CONTRACT_NOT_FOUND(HttpStatus.NOT_FOUND, "Active contract not found"),
    ACTIVE_WORKPROOF_NOT_FOUND(HttpStatus.NOT_FOUND, "Active workproof not found"),
    WAGE_VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Wage verification not found"),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Document not found"),
    CLAIM_KIT_NOT_FOUND(HttpStatus.NOT_FOUND, "Claim kit not found"),
    CLAIM_PREPARATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Claim preparation not found"),
    ADVANCE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "Advance request not found"),
    RECIPIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Recipient not found"),
    TRANSFER_NOT_FOUND(HttpStatus.NOT_FOUND, "Transfer not found"),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "Wallet not found"),
    VAULT_TRANSACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Vault transaction not found"),

    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already exists"),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "Phone number already exists"),
    ACTIVE_CONTRACT_REQUIRED(HttpStatus.CONFLICT, "Active contract is required"),
    ACTIVE_CONTRACT_EXISTS(HttpStatus.CONFLICT, "Active contract already exists"),
    ACTIVE_WORKPROOF_EXISTS(HttpStatus.CONFLICT, "Active workproof already exists"),
    WORK_DATE_ALREADY_EXISTS(HttpStatus.CONFLICT, "A workproof already exists for the work date"),
    WORKPLACE_RADIUS_EXCEEDED(HttpStatus.CONFLICT, "Current location is outside the allowed workplace radius"),
    CHECK_OUT_BEFORE_CHECK_IN(HttpStatus.CONFLICT, "checkOut device time must be after the active checkIn time"),
    DOCUMENT_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "Document request already exists for the given idempotency key"),
    ADVANCE_DUPLICATE_REQUEST(HttpStatus.CONFLICT, "Advance request already exists for the idempotency key"),
    ADVANCE_NOT_ELIGIBLE(HttpStatus.CONFLICT, "Advance is not eligible"),
    IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD(HttpStatus.CONFLICT, "Idempotency key was reused with a different payload"),
    RECIPIENT_WALLET_ALREADY_EXISTS(HttpStatus.CONFLICT, "Recipient wallet is already registered"),
    RECIPIENT_NOT_ALLOWED(HttpStatus.CONFLICT, "Recipient is not allowed"),
    RECENT_RECIPIENT_CONFIRMATION_REQUIRED(HttpStatus.CONFLICT, "Recently updated recipient requires explicit confirmation"),
    HIGH_AMOUNT_CONFIRMATION_REQUIRED(HttpStatus.CONFLICT, "High amount transfer requires explicit confirmation"),
    SELF_TRANSFER_NOT_ALLOWED(HttpStatus.CONFLICT, "Sending to your own wallet is not allowed"),
    INSUFFICIENT_WALLET_BALANCE(HttpStatus.CONFLICT, "Wallet balance is insufficient for this transfer"),
    TRANSFER_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "Another transfer is already in progress"),
    VAULT_INSUFFICIENT_AVAILABLE_BALANCE(HttpStatus.CONFLICT, "Available balance is insufficient for this vault deposit"),
    VAULT_INSUFFICIENT_STORED_BALANCE(HttpStatus.CONFLICT, "Stored balance is insufficient for this vault withdrawal"),
    VAULT_ALLOWANCE_REQUIRED(HttpStatus.CONFLICT, "Vault token approval failed"),
    VAULT_TRANSACTION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "Another vault transaction is already in progress"),
    VAULT_CONFIG_MISSING(HttpStatus.CONFLICT, "Vault configuration is missing"),
    WALLET_FUNDING_FAILED(HttpStatus.CONFLICT, "Wallet funding failed"),
    RECOVERY_ACTION_NOT_ALLOWED(HttpStatus.CONFLICT, "Recovery action is not allowed for the current state"),
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "File is too large"),

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
