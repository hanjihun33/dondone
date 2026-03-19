package com.dondone.mobile.data.documents

import java.io.IOException
import java.time.LocalDate

interface WorkproofDocumentRepository {
    suspend fun preview(
        accessToken: String,
        workplaceId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): WorkproofDocumentPreviewPayload

    suspend fun create(
        accessToken: String,
        workplaceId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): WorkproofDocumentCreatePayload

    suspend fun getRequestStatus(
        accessToken: String,
        requestId: String
    ): WorkproofDocumentRequestStatusPayload
}

data class WorkproofDocumentPreviewPayload(
    val documentType: String,
    val workplaceId: Long,
    val workplaceName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRecordCount: Int,
    val reflectedCount: Int,
    val needsReviewCount: Int,
    val editedCount: Int,
    val attachmentCount: Int,
    val totalWorkedMinutes: Long,
    val totalWorkedHoursText: String
)

data class WorkproofDocumentCreatePayload(
    val requestId: String,
    val documentType: String,
    val status: String,
    val pollUrl: String
)

data class WorkproofDocumentRequestStatusPayload(
    val requestId: String,
    val documentId: Long?,
    val documentType: String,
    val status: String,
    val pollUrl: String,
    val documentUrl: String?
)

class WorkproofDocumentUnauthorizedException(
    message: String = "세션이 만료되어 다시 로그인해 주세요."
) : IOException(message)
