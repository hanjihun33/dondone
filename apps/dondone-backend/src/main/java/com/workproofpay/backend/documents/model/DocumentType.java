package com.workproofpay.backend.documents.model;

public enum DocumentType {
    WORKPROOF_STATEMENT(true, false, DocumentGenerationStrategy.ON_DEMAND_DOWNLOAD),
    PROOF_PACK(false, true, DocumentGenerationStrategy.REQUEST_STATUS_WORKFLOW),
    CLAIM_KIT(false, true, DocumentGenerationStrategy.REQUEST_STATUS_WORKFLOW),
    TRANSFER_RECEIPT(false, false, DocumentGenerationStrategy.REQUEST_STATUS_WORKFLOW);

    private final boolean usesPeriodRange;
    private final boolean usesYearMonth;
    private final DocumentGenerationStrategy generationStrategy;

    DocumentType(boolean usesPeriodRange,
                 boolean usesYearMonth,
                 DocumentGenerationStrategy generationStrategy) {
        this.usesPeriodRange = usesPeriodRange;
        this.usesYearMonth = usesYearMonth;
        this.generationStrategy = generationStrategy;
    }

    public boolean usesPeriodRange() {
        return usesPeriodRange;
    }

    public boolean usesYearMonth() {
        return usesYearMonth;
    }

    public DocumentGenerationStrategy generationStrategy() {
        return generationStrategy;
    }

    public boolean usesOnDemandDownload() {
        return generationStrategy == DocumentGenerationStrategy.ON_DEMAND_DOWNLOAD;
    }

    public boolean usesRequestStatusWorkflow() {
        return generationStrategy == DocumentGenerationStrategy.REQUEST_STATUS_WORKFLOW;
    }
}
