package com.workproofpay.backend.workproof.api.dto.response;

import java.util.List;

public record WorkplaceListResponse(
        List<WorkplaceResponse> workplaces
) {
}
