package com.workproofpay.backend.documents.pdf;

public interface PdfRenderer {
    RenderedPdf render(String templateName, Object model);
}
