package com.dondone.mobile.data.documents

import com.dondone.mobile.data.remote.BackendApiException
import com.dondone.mobile.data.remote.BackendApiSupport
import com.dondone.mobile.data.remote.parseBackendErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate

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
}
