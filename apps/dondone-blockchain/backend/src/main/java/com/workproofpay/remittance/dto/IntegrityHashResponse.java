package com.workproofpay.remittance.dto;

public record IntegrityHashResponse(
        String docType,
        String sourceRef,
        int itemCount,
        String normalizedPayload,
        String payloadHash,
        String proofId
) {}
