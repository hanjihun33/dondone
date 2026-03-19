package com.workproofpay.backend.documents.pdf;

public record RenderedPdf(
        byte[] bytes,
        String fileName,
        String contentType,
        String sha256
) {
}
