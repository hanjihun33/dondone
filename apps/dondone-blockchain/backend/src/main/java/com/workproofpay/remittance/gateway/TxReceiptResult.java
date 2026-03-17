package com.workproofpay.remittance.gateway;

public record TxReceiptResult(boolean success, String failureCode) {}
