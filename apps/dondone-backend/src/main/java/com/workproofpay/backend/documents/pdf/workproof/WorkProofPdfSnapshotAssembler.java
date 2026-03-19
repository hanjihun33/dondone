package com.workproofpay.backend.documents.pdf.workproof;

public interface WorkProofPdfSnapshotAssembler {
    WorkProofPdfSnapshot assemble(WorkProofPdfAssembleCommand command);
}
