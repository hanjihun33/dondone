package com.workproofpay.backend.remittance.model;

public enum TransferStatus {
    REQUESTED,
    SIGNED,
    BROADCASTED,
    CONFIRMED,
    FAILED,
    TIMED_OUT
}
