package com.workproofpay.backend.remittance.api.dto.response;

import java.util.List;

public record RemittanceOpsTransferListResponse(
        List<RemittanceOpsTransferItemResponse> transfers
) {
}
