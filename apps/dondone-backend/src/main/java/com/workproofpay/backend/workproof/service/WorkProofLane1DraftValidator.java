package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WorkProofLane1DraftValidator {

    public void validateCreateContract(CreateContractRequest request) {
        if (request.payUnit() != WorkProofPayUnit.DAILY && request.dailyWorkMinutes() != null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "dailyWorkMinutes is only allowed for DAILY payUnit");
        }
        if (request.payUnit() != WorkProofPayUnit.MONTHLY && request.monthlyWorkMinutes() != null) {
            throw new ApiException(ErrorCode.INVALID_INPUT_VALUE, "monthlyWorkMinutes is only allowed for MONTHLY payUnit");
        }
    }

    public void validateCheckOutSequence(LocalDateTime activeCheckInDeviceAt, CheckOutWorkProofRequest request) {
        if (activeCheckInDeviceAt != null && !request.deviceAt().isAfter(activeCheckInDeviceAt)) {
            throw new ApiException(ErrorCode.CHECK_OUT_BEFORE_CHECK_IN);
        }
    }
}
