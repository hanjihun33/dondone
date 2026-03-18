package com.dondone.mobile.data.workproof

import com.dondone.mobile.data.remote.BackendApiException
import com.dondone.mobile.data.remote.BackendApiSupport
import com.dondone.mobile.data.remote.parseBackendErrorMessage
import com.dondone.mobile.domain.model.WorkproofData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private const val SESSION_EXPIRED_MESSAGE = "세션이 만료되어 다시 로그인해 주세요."
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendWorkproofRepository(
    private val client: OkHttpClient = OkHttpClient()
) : WorkproofRepository {

    override suspend fun load(accessToken: String): WorkproofRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext WorkproofRemoteState.unauthenticated("로그인 후 출퇴근 실연동을 불러옵니다.")
        }
        runCatching {
            val workplace = fetchPrimaryWorkplace(accessToken)
                ?: return@withContext WorkproofRemoteState.empty("연결된 근무지가 없어 출퇴근 기록을 불러올 수 없어요.")
            ensureActiveContract(accessToken, workplace.workplaceId)
            val records = fetchRecords(accessToken, workplace.workplaceId, YearMonth.now())
            WorkproofRemoteState.content(
                WorkproofRemotePayload(
                    workplace = workplace,
                    records = records
                )
            )
        }.getOrElse { error ->
            when (error) {
                is WorkproofUnauthorizedException -> WorkproofRemoteState.unauthenticated(
                    error.message ?: SESSION_EXPIRED_MESSAGE
                )

                else -> WorkproofRemoteState.error(
                    error.message ?: "출퇴근 실연동 데이터를 불러오지 못했어요."
                )
            }
        }
    }

    override suspend fun clockIn(
        accessToken: String,
        workproof: WorkproofData
    ): WorkproofRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw WorkproofUnauthorizedException()
        }
        val workplaceId = workproof.workplaceId ?: throw BackendApiException("연결된 근무지 정보를 다시 불러와 주세요.")
        ensureActiveContract(accessToken, workplaceId)
        val body = JSONObject()
            .put("workplaceId", workplaceId)
            .put("deviceAt", LocalDateTime.now().withNano(0).toString())
            .put("latitude", workproof.currentLatitude)
            .put("longitude", workproof.currentLongitude)
            .put("locationLabel", workproof.workplaceName)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/records/check-in")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        executeMutation(
            request = request,
            failureMessage = "출근 기록을 저장하지 못했어요."
        )
        load(accessToken)
    }

    override suspend fun clockOut(
        accessToken: String,
        workproof: WorkproofData
    ): WorkproofRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw WorkproofUnauthorizedException()
        }
        val body = JSONObject()
            .put("deviceAt", LocalDateTime.now().withNano(0).toString())
            .put("latitude", workproof.currentLatitude)
            .put("longitude", workproof.currentLongitude)
            .put("locationLabel", workproof.workplaceName)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/records/check-out")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        executeMutation(
            request = request,
            failureMessage = "퇴근 기록을 저장하지 못했어요."
        )
        load(accessToken)
    }

    private fun fetchPrimaryWorkplace(token: String): WorkproofWorkplacePayload? {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/workplaces")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw WorkproofUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "근무지 조회에 실패했어요."
                    )
                )
            }
            val workplaces = JSONObject(responseBody.ifBlank { "{}" })
                .getJSONObject("data")
                .getJSONArray("workplaces")
            if (workplaces.length() == 0) return null

            var selected: JSONObject? = null
            for (index in 0 until workplaces.length()) {
                val item = workplaces.getJSONObject(index)
                if (item.optBoolean("hasActiveContract")) {
                    selected = item
                    break
                }
                if (selected == null) {
                    selected = item
                }
            }
            selected ?: return null

            return WorkproofWorkplacePayload(
                workplaceId = selected.getLong("workplaceId"),
                name = selected.getString("name"),
                address = selected.getString("address"),
                latitude = selected.getDouble("latitude"),
                longitude = selected.getDouble("longitude"),
                allowedRadiusMeters = selected.optInt("allowedRadiusMeters", 1000)
            )
        }
    }

    private fun ensureActiveContract(token: String, workplaceId: Long) {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/contracts/current?workplaceId=$workplaceId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                return
            }
            if (response.code == 401 || response.code == 403) {
                throw WorkproofUnauthorizedException()
            }
            if (response.code == 404) {
                throw BackendApiException("활성 근로 계약이 없어 출퇴근을 진행할 수 없어요.")
            }
            throw BackendApiException(
                parseBackendErrorMessage(
                    responseBody = responseBody,
                    fallbackMessage = "활성 계약을 확인하지 못했어요."
                )
            )
        }
    }

    private fun fetchRecords(
        token: String,
        workplaceId: Long,
        month: YearMonth
    ): List<WorkproofRecordPayload> {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/records?month=$month&workplaceId=$workplaceId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw WorkproofUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "출퇴근 기록을 불러오지 못했어요."
                    )
                )
            }

            val recordsJson = JSONObject(responseBody.ifBlank { "{}" })
                .getJSONObject("data")
                .getJSONArray("records")
            return buildList {
                for (index in 0 until recordsJson.length()) {
                    add(recordsJson.getJSONObject(index).toRecordPayload())
                }
            }
        }
    }

    private fun executeMutation(
        request: Request,
        failureMessage: String
    ) {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                return
            }
            if (response.code == 401 || response.code == 403) {
                throw WorkproofUnauthorizedException()
            }
            throw BackendApiException(
                parseBackendErrorMessage(
                    responseBody = responseBody,
                    fallbackMessage = failureMessage
                )
            )
        }
    }

    private fun JSONObject.toRecordPayload(): WorkproofRecordPayload {
        return WorkproofRecordPayload(
            recordId = getLong("recordId"),
            workDate = LocalDate.parse(getString("workDate")),
            status = getString("status"),
            checkInDeviceAt = LocalDateTime.parse(getString("checkInDeviceAt")),
            checkOutDeviceAt = optLocalDateTime("checkOutDeviceAt"),
            workedMinutes = optLongValue("workedMinutes"),
            modified = getBoolean("modified"),
            reflectionStatus = getString("reflectionStatus"),
            riskFlags = optStringList(getJSONArray("riskFlags"))
        )
    }

    private fun JSONObject.optLongValue(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return getLong(key)
    }

    private fun JSONObject.optLocalDateTime(key: String): LocalDateTime? {
        if (!has(key) || isNull(key)) return null
        val value = optString(key)
        if (value.isBlank() || value == "null") return null
        return LocalDateTime.parse(value)
    }

    private fun optStringList(jsonArray: JSONArray): List<String> {
        return buildList {
            for (index in 0 until jsonArray.length()) {
                add(jsonArray.getString(index))
            }
        }
    }
}
