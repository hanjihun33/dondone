package com.workproofpay.backend.employer.api.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Scoped attendance status derived from today's workplace workproof")
public enum EmployerWorkerAttendanceStatus {
    WORKING,
    COMPLETED,
    NEEDS_REVIEW,
    NO_RECORD
}
