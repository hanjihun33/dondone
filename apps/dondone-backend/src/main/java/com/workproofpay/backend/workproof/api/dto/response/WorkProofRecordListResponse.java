package com.workproofpay.backend.workproof.api.dto.response;

import java.util.List;

public record WorkProofRecordListResponse(
        String month,
        Long workplaceId,
        List<WorkProofRecordListItemResponse> records
) {
}
