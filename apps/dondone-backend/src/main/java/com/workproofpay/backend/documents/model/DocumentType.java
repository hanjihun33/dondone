package com.workproofpay.backend.documents.model;

public enum DocumentType {
    WORKPROOF_STATEMENT,
    PROOF_PACK,
    CLAIM_KIT,
    TRANSFER_RECEIPT;

    public boolean usesPeriodRange() {
        return this == WORKPROOF_STATEMENT;
    }

    public boolean usesYearMonth() {
        return this == PROOF_PACK || this == CLAIM_KIT;
    }
}
