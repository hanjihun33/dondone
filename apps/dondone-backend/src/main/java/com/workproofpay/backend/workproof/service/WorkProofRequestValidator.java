package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.UpdateWorkProofRequest;
import com.workproofpay.backend.workproof.model.WorkProof;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class WorkProofRequestValidator {

    public void validateForCreate(CreateWorkProofRequest request) {
        if (!request.workDate().equals(request.clockInAt().toLocalDate())) {
            throw new ApiException(ErrorCode.INVALID_WORK_DATE);
        }
        if (request.clockOutAt() != null) {
            validateTimeRange(request.clockInAt(), request.clockOutAt());
        }
        boolean hasAnyClockOutField = request.clockOutAt() != null
                || request.deviceClockOutAt() != null
                || request.clockOutLatitude() != null
                || request.clockOutLongitude() != null;
        boolean hasAllClockOutFields = request.clockOutAt() != null
                && request.deviceClockOutAt() != null
                && request.clockOutLatitude() != null
                && request.clockOutLongitude() != null;
        if (hasAnyClockOutField && !hasAllClockOutFields) {
            throw new ApiException(ErrorCode.INCOMPLETE_CLOCK_OUT);
        }
        if (request.deviceClockOutAt() != null && !request.deviceClockOutAt().isAfter(request.deviceClockInAt())) {
            throw new ApiException(ErrorCode.INVALID_DEVICE_TIME);
        }
    }

    public void validateForUpdate(WorkProof workProof, UpdateWorkProofRequest request) {
        if (!workProof.isReflected() && !workProof.isNeedsReview()) {
            throw new ApiException(ErrorCode.WORKPROOF_EDIT_NOT_ALLOWED);
        }
        if (!workProof.getWorkDate().equals(request.clockInAt().toLocalDate())) {
            throw new ApiException(ErrorCode.INVALID_WORK_DATE);
        }
        validateTimeRange(request.clockInAt(), request.clockOutAt());
        if (request.attachments() != null && request.attachmentCount() != null
                && request.attachmentCount().intValue() != request.attachments().size()) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "attachmentCount must match attachments size");
        }
    }

    private void validateTimeRange(LocalDateTime clockInAt, LocalDateTime clockOutAt) {
        if (!clockOutAt.isAfter(clockInAt)) {
            throw new ApiException(ErrorCode.INVALID_WORKPROOF_TIME);
        }
    }
}
