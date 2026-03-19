package com.dondone.mobile.data.documents

import com.dondone.mobile.data.remote.BackendApiException
import com.dondone.mobile.data.remote.BackendApiSupport
import com.dondone.mobile.data.remote.parseBackendErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

private const val WORKPROOF_DOCUMENT_TYPE = "WORKPROOF_STATEMENT"
private val WORKPROOF_DOCUMENT_JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendWorkproofDocumentRepository(
    private val client: OkHttpClient = OkHttpClient()
) : WorkproofDocumentRepository {

    override suspend fun preview(
        accessToken: String,
        workplaceId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): WorkproofDocumentPreviewPayload = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw WorkproofDocumentUnauthorizedException()
        }

        val request = Request.Builder()
            .url(
                "${BackendApiSupport.baseUrl}/api/workproof/documents/preview" +
                    "?workplaceId=$workplaceId&startDate=$startDate&endDate=$endDate"
            )
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw WorkproofDocumentUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "문서 미리보기를 불러오지 못했어요."
                    )
                )
            }

            val data = JSONObject(responseBody.ifBlank { "{}" }).getJSONObject("data")
            return@withContext WorkproofDocumentPreviewPayload(
                documentType = data.getString("documentType"),
                workplaceId = data.getLong("workplaceId"),
                workplaceName = data.getString("workplaceName"),
                startDate = LocalDate.parse(data.getString("startDate")),
                endDate = LocalDate.parse(data.getString("endDate")),
                totalRecordCount = data.getInt("totalRecordCount"),
                reflectedCount = data.getInt("reflectedCount"),
                needsReviewCount = data.getInt("needsReviewCount"),
                editedCount = data.getInt("editedCount"),
                attachmentCount = data.getInt("attachmentCount"),
                totalWorkedMinutes = data.getLong("totalWorkedMinutes"),
                totalWorkedHoursText = data.getString("totalWorkedHoursText")
            )
        }
    }

    override suspend fun create(
        accessToken: String,
        workplaceId: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ): WorkproofDocumentCreatePayload = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw WorkproofDocumentUnauthorizedException()
        }

        val body = JSONObject()
            .put("documentType", WORKPROOF_DOCUMENT_TYPE)
            .put("workplaceId", workplaceId)
            .put("startDate", startDate.toString())
            .put("endDate", endDate.toString())
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/documents")
            .header("Authorization", "Bearer $accessToken")
            .header("Idempotency-Key", "android-workproof-${UUID.randomUUID()}")
            .post(body.toRequestBody(WORKPROOF_DOCUMENT_JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw WorkproofDocumentUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "근무 기록 문서 생성 요청에 실패했어요."
                    )
                )
            }

            val data = JSONObject(responseBody.ifBlank { "{}" }).getJSONObject("data")
            return@withContext WorkproofDocumentCreatePayload(
                requestId = data.getString("requestId"),
                documentType = data.getString("documentType"),
                status = data.getString("status"),
                pollUrl = data.getString("pollUrl")
            )
        }
    }

    override suspend fun getRequestStatus(
        accessToken: String,
        requestId: String
    ): WorkproofDocumentRequestStatusPayload = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw WorkproofDocumentUnauthorizedException()
        }

        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/documents/requests/$requestId")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw WorkproofDocumentUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "근무 기록 문서 상태를 확인하지 못했어요."
                    )
                )
            }

            val data = JSONObject(responseBody.ifBlank { "{}" }).getJSONObject("data")
            return@withContext WorkproofDocumentRequestStatusPayload(
                requestId = data.getString("requestId"),
                documentId = if (data.isNull("documentId")) null else data.getLong("documentId"),
                documentType = data.getString("documentType"),
                status = data.getString("status"),
                pollUrl = data.getString("pollUrl"),
                documentUrl = if (data.isNull("documentUrl")) null else data.getString("documentUrl")
            )
        }
    }
}
