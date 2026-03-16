package com.workproofpay.backend.workproof;

import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.GetWorkProofMonthlySummaryQuery;
import com.workproofpay.backend.workproof.api.dto.request.ListWorkProofRecordsQuery;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.service.WorkProofLane1DraftValidator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkProofLane1DraftValidatorTest {

    private Validator validator;
    private WorkProofLane1DraftValidator draftValidator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        draftValidator = new WorkProofLane1DraftValidator();
    }

    @Test
    void rejectsOutOfRangeCheckInCoordinatesWithBeanValidation() {
        CheckInWorkProofRequest request = new CheckInWorkProofRequest(
                1L,
                LocalDateTime.of(2026, 3, 13, 9, 0),
                91.0,
                127.0,
                "Lobby"
        );

        Set<ConstraintViolation<CheckInWorkProofRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(violation -> violation.getMessage().equals("latitude must be 90 or less")));
    }

    @Test
    void rejectsInvalidMonthQueryWithBeanValidation() {
        ListWorkProofRecordsQuery query = new ListWorkProofRecordsQuery("2026/03", 2L);

        Set<ConstraintViolation<ListWorkProofRecordsQuery>> violations = validator.validate(query);

        assertTrue(violations.stream().anyMatch(violation -> violation.getMessage().equals("month must follow YYYY-MM")));
    }

    @Test
    void rejectsTooLongMonthQueryWithBeanValidation() {
        ListWorkProofRecordsQuery recordsQuery = new ListWorkProofRecordsQuery("2026-031", 2L);
        GetWorkProofMonthlySummaryQuery summaryQuery = new GetWorkProofMonthlySummaryQuery("2026-031", 2L);

        Set<ConstraintViolation<ListWorkProofRecordsQuery>> recordViolations = validator.validate(recordsQuery);
        Set<ConstraintViolation<GetWorkProofMonthlySummaryQuery>> summaryViolations = validator.validate(summaryQuery);

        assertTrue(recordViolations.stream().anyMatch(violation -> violation.getMessage().equals("month must be exactly 7 characters")));
        assertTrue(summaryViolations.stream().anyMatch(violation -> violation.getMessage().equals("month must be exactly 7 characters")));
    }

    @Test
    void rejectsContractMinuteFieldsThatDoNotMatchPayUnit() {
        CreateContractRequest request = new CreateContractRequest(
                3L,
                WorkProofPayUnit.HOURLY,
                BigDecimal.valueOf(12000),
                480,
                null,
                null
        );

        ApiException exception = assertThrows(ApiException.class, () -> draftValidator.validateCreateContract(request));

        assertEquals(ErrorCode.INVALID_INPUT_VALUE, exception.getErrorCode());
        assertEquals("dailyWorkMinutes is only allowed for DAILY payUnit", exception.getMessage());
    }

    @Test
    void rejectsCheckOutBeforeActiveCheckIn() {
        CheckOutWorkProofRequest request = new CheckOutWorkProofRequest(
                LocalDateTime.of(2026, 3, 13, 8, 59),
                37.5,
                127.0,
                "Front door"
        );

        ApiException exception = assertThrows(ApiException.class, () ->
                draftValidator.validateCheckOutSequence(LocalDateTime.of(2026, 3, 13, 9, 0), request)
        );

        assertEquals(ErrorCode.CHECK_OUT_BEFORE_CHECK_IN, exception.getErrorCode());
    }
}
