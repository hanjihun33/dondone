package com.workproofpay.backend.jobs.model;

public enum JobType {
    SUBMIT_TRANSFER(JobReferenceKind.TRANSFER),
    POLL_TRANSFER_RECEIPT(JobReferenceKind.TRANSFER),
    SUBMIT_VAULT_TRANSACTION(JobReferenceKind.VAULT),
    POLL_VAULT_TRANSACTION_RECEIPT(JobReferenceKind.VAULT),
    SUBMIT_ADVANCE_PAYOUT(JobReferenceKind.ADVANCE_PAYOUT);

    private final JobReferenceKind referenceKind;

    JobType(JobReferenceKind referenceKind) {
        this.referenceKind = referenceKind;
    }

    public JobReferenceKind getReferenceKind() {
        return referenceKind;
    }
}
