package com.workproofpay.backend.vault.model;

public enum VaultTransactionStatus {
    REQUESTED,
    SIGNED,
    BROADCASTED,
    CONFIRMED,
    FAILED,
    TIMED_OUT
}
