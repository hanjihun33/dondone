package com.workproofpay.backend.remittance.adapter;

import com.workproofpay.backend.remittance.model.TransferFailureCode;

public record ChainReceiptResult(
        boolean success,
        TransferFailureCode failureCode,
        String networkFeeWei
) {
}
