package com.workproofpay.backend.jobs.model;

public enum JobType {
    SUBMIT_TRANSFER(JobReferenceKind.TRANSFER),
    POLL_TRANSFER_RECEIPT(JobReferenceKind.TRANSFER);

    private final JobReferenceKind referenceKind;

    JobType(JobReferenceKind referenceKind) {
        this.referenceKind = referenceKind;
    }

    public JobReferenceKind getReferenceKind() {
        return referenceKind;
    }
}
