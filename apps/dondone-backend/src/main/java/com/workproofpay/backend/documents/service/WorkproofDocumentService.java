package com.workproofpay.backend.documents.service;

public interface WorkproofDocumentService {

    WorkproofDocumentPreviewResult preview(Long userId, WorkproofDocumentPreviewQuery query);
}
