package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkProofRequest;
import org.springframework.stereotype.Component;

@Component
public class WorkProofRequestValidator {

    public void validateForCreate(CreateWorkProofRequest request) {
        if (!request.workDate().equals(request.clockInAt().toLocalDate())) {
            throw new ApiException(ErrorCode.INVALID_WORK_DATE);
        }
        if (request.clockOutAt() != null && !request.clockOutAt().isAfter(request.clockInAt())) {
            throw new ApiException(ErrorCode.INVALID_WORKPROOF_TIME);
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
}
