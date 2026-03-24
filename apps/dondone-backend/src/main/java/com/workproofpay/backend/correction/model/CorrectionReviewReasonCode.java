package com.workproofpay.backend.correction.model;

public enum CorrectionReviewReasonCode {
    LATE_CLOCK_IN_AFTER_SCHEDULE,
    EARLY_CLOCK_OUT_BEFORE_SCHEDULE,
    LATE_CLOCK_OUT_AFTER_GRACE,
    OUTSIDE_ALLOWED_RADIUS,
    OTHER
}
