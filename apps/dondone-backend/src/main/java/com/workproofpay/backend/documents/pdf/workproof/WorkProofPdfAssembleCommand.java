package com.workproofpay.backend.documents.pdf.workproof;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;

public record WorkProofPdfAssembleCommand(
        Long userId,
        Long documentRequestId,
        String requestId,
        Long workplaceId,
        LocalDate startDate,
        LocalDate endDate,
        ZoneId zoneId,
        Locale locale
) {
}
