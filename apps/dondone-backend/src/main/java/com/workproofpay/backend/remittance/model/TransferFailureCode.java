package com.workproofpay.backend.remittance.model;

public enum TransferFailureCode {
    NETWORK_ERROR,
    CHAIN_REVERT,
    INSUFFICIENT_GAS,
    UNKNOWN
}
