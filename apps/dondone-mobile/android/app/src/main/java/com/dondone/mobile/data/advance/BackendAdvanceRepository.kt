package com.dondone.mobile.data.advance

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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

private const val SESSION_EXPIRED_MESSAGE = "세션이 만료되어 다시 로그인해 주세요."
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendAdvanceRepository(
    private val client: OkHttpClient = OkHttpClient()
) : AdvanceRepository {

    override suspend fun load(accessToken: String): AdvanceRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext AdvanceRemoteState.unauthenticated("로그인 후 실연동 데이터를 불러옵니다.")
        }
        runCatching {
            val workplace = fetchPrimaryWorkplace(accessToken) ?: return@withContext AdvanceRemoteState.empty(
                "연결된 근무지가 없어 미리받기 실연동을 표시할 수 없어요."
            )
            val eligibility = fetchEligibility(accessToken, workplace.id)
            val requests = fetchRequests(accessToken)
            AdvanceRemoteState.content(workplace.name, eligibility, requests)
        }.getOrElse { error ->
            when (error) {
                is AdvanceUnauthorizedException -> AdvanceRemoteState.unauthenticated(
                    error.message ?: SESSION_EXPIRED_MESSAGE
                )

                else -> AdvanceRemoteState.error(
                    error.message ?: "백엔드 연결에 실패해 데모 상태로 표시합니다."
                )
            }
        }
    }

    override suspend fun createRequest(
        accessToken: String,
        workplaceId: Long,
        requestedAmountAtomic: Long
    ): AdvanceCreateResult = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw AdvanceUnauthorizedException()
        }

        val body = JSONObject()
            .put("workplaceId", workplaceId)
            .put("requestedAmountAtomic", requestedAmountAtomic)
            .put("requestedAt", LocalDateTime.now().withNano(0).toString())
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/advance/requests")
            .header("Authorization", "Bearer $accessToken")
            .header("Idempotency-Key", "android-${UUID.randomUUID()}")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw AdvanceUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "미리받기 신청에 실패했어요. 잠시 후 다시 시도해 주세요."
                    )
                )
            }

            val data = json.getJSONObject("data")
            return@withContext AdvanceCreateResult(
                requestId = data.getLong("requestId"),
                assetSymbol = data.getString("assetSymbol"),
                assetDecimals = data.optInt("assetDecimals", 6),
                exchangeRateSnapshot = data.getBigDecimalCompat("exchangeRateSnapshot"),
                status = data.getString("status"),
                requestStatus = data.getString("requestStatus"),
                payoutStatus = data.optStringOrNull("payoutStatus"),
                approvedAmountAtomic = data.optLongOrNull("approvedAmountAtomic"),
                approvedDisplayKrwAmount = data.optLongOrNull("approvedDisplayKrwAmount"),
                feeAmountAtomic = data.getLong("feeAmountAtomic"),
                feeDisplayKrwAmount = data.getLong("feeDisplayKrwAmount"),
                settlementStatus = data.optStringOrNull("settlementStatus"),
                settlementDueDate = data.optStringOrNull("settlementDueDate"),
                repaymentDueDate = data.getString("repaymentDueDate"),
                eligibilitySnapshot = toEligibilitySnapshotPayload(data.getJSONObject("eligibilitySnapshot"))
            )
        }
    }

    override suspend fun getRequestDetail(
        accessToken: String,
        requestId: Long
    ): AdvanceRequestDetailPayload = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            throw AdvanceUnauthorizedException()
        }
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/advance/requests/$requestId")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw AdvanceUnauthorizedException()
                }
                throw BackendApiException(
                    parseBackendErrorMessage(
                        responseBody = responseBody,
                        fallbackMessage = "미리받기 상세를 불러오지 못했어요."
                    )
                )
            }
            return@withContext toDetailPayload(json.getJSONObject("data"))
        }
    }

    private fun fetchPrimaryWorkplace(token: String): WorkplacePayload? {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/workproof/workplaces")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw AdvanceUnauthorizedException()
                }
                throw BackendApiException("근무지 조회 실패: ${response.code}")
            }
            val workplaces = JSONObject(response.body?.string().orEmpty())
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
            return WorkplacePayload(
                id = selected.getLong("workplaceId"),
                name = selected.getString("name")
            )
        }
    }

    private fun fetchEligibility(token: String, workplaceId: Long): AdvanceEligibilityPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/advance/eligibility?workplaceId=$workplaceId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                if (response.code == 401 || response.code == 403) {
                    throw AdvanceUnauthorizedException()
                }
                throw BackendApiException("미리받기 적격성 조회 실패: ${response.code}")
            }
            val data = JSONObject(response.body?.string().orEmpty()).getJSONObject("data")
            val blockReasonsJson = data.getJSONArray("blockReasonCodes")
            val blockReasons = buildList {
                for (index in 0 until blockReasonsJson.length()) {
                    add(blockReasonsJson.getString(index))
                }
            }
            val noticeReasons = buildList {
                val noticeReasonsJson = data.optJSONArray("noticeReasonCodes")
                if (noticeReasonsJson != null) {
                    for (index in 0 until noticeReasonsJson.length()) {
                        add(noticeReasonsJson.getString(index))
                    }
                }
            }
            return AdvanceEligibilityPayload(
                workplaceId = data.getLong("workplaceId"),
                assetSymbol = data.getString("assetSymbol"),
                assetDecimals = data.optInt("assetDecimals", 6),
                exchangeRateSnapshot = data.getBigDecimalCompat("exchangeRateSnapshot"),
                availableAmountAtomic = data.getLong("availableAmountAtomic"),
                availableDisplayKrwAmount = data.getLong("availableDisplayKrwAmount"),
                maxCapAmountAtomic = data.getLong("maxCapAmountAtomic"),
                maxCapDisplayKrwAmount = data.getLong("maxCapDisplayKrwAmount"),
                repaymentTier = data.getString("repaymentTier"),
                blockReasonCodes = blockReasons,
                noticeReasonCodes = noticeReasons,
                estimatedFeeAmountAtomic = data.getLong("estimatedFeeAmountAtomic"),
                estimatedFeeDisplayKrwAmount = data.getLong("estimatedFeeDisplayKrwAmount"),
                estimatedRepaymentDate = data.getString("estimatedRepaymentDate"),
                settlementDueDate = data.optStringOrNull("settlementDueDate"),
                disclaimer = data.getString("disclaimer"),
                needsReviewRecordCount = data.getInt("needsReviewRecordCount")
            )
        }
    }

    private fun fetchRequests(token: String): List<AdvanceRequestItemPayload> {
        val months = listOf(
            YearMonth.now().minusMonths(1),
            YearMonth.now(),
            YearMonth.now().plusMonths(1)
        ).distinct()
        val collected = linkedMapOf<Long, AdvanceRequestItemPayload>()

        months.forEach { month ->
            val request = Request.Builder()
                .url("${BackendApiSupport.baseUrl}/api/advance/requests?month=$month")
                .header("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    if (response.code == 401 || response.code == 403) {
                        throw AdvanceUnauthorizedException()
                    }
                    throw BackendApiException("미리받기 이력 조회 실패: ${response.code}")
                }
                val requests = JSONObject(response.body?.string().orEmpty())
                    .getJSONObject("data")
                    .getJSONArray("requests")

                for (index in 0 until requests.length()) {
                    val item = requests.getJSONObject(index)
                    val payload = AdvanceRequestItemPayload(
                        requestId = item.getLong("requestId"),
                        workplaceId = item.getLong("workplaceId"),
                        assetSymbol = item.getString("assetSymbol"),
                        assetDecimals = item.optInt("assetDecimals", 6),
                        exchangeRateSnapshot = item.getBigDecimalCompat("exchangeRateSnapshot"),
                        requestedAmountAtomic = item.getLong("requestedAmountAtomic"),
                        requestedDisplayKrwAmount = item.getLong("requestedDisplayKrwAmount"),
                        approvedAmountAtomic = item.optLongOrNull("approvedAmountAtomic"),
                        approvedDisplayKrwAmount = item.optLongOrNull("approvedDisplayKrwAmount"),
                        status = item.getString("status"),
                        requestStatus = item.getString("requestStatus"),
                        payoutStatus = item.optStringOrNull("payoutStatus"),
                        payoutTxHash = item.optStringOrNull("payoutTxHash"),
                        settlementStatus = item.optStringOrNull("settlementStatus"),
                        settlementDueDate = item.optStringOrNull("settlementDueDate"),
                        repaymentDueDate = item.getString("repaymentDueDate"),
                        requestedAt = item.getString("requestedAt")
                    )
                    collected[payload.requestId] = payload
                }
            }
        }

        return collected.values.sortedByDescending { it.requestId }
    }

    private fun toDetailPayload(data: JSONObject): AdvanceRequestDetailPayload {
        return AdvanceRequestDetailPayload(
            requestId = data.getLong("requestId"),
            workplaceId = data.getLong("workplaceId"),
            assetSymbol = data.getString("assetSymbol"),
            assetDecimals = data.optInt("assetDecimals", 6),
            exchangeRateSnapshot = data.getBigDecimalCompat("exchangeRateSnapshot"),
            requestedAmountAtomic = data.getLong("requestedAmountAtomic"),
            requestedDisplayKrwAmount = data.getLong("requestedDisplayKrwAmount"),
            approvedAmountAtomic = data.optLongOrNull("approvedAmountAtomic"),
            approvedDisplayKrwAmount = data.optLongOrNull("approvedDisplayKrwAmount"),
            feeAmountAtomic = data.getLong("feeAmountAtomic"),
            feeDisplayKrwAmount = data.getLong("feeDisplayKrwAmount"),
            status = data.getString("status"),
            requestStatus = data.getString("requestStatus"),
            payoutStatus = data.optStringOrNull("payoutStatus"),
            payoutTxHash = data.optStringOrNull("payoutTxHash"),
            settlementStatus = data.optStringOrNull("settlementStatus"),
            settlementDueDate = data.optStringOrNull("settlementDueDate"),
            repaymentDueDate = data.getString("repaymentDueDate"),
            eligibilitySnapshot = toEligibilitySnapshotPayload(data.getJSONObject("eligibilitySnapshot")),
            createdAt = data.getString("createdAt")
        )
    }

    private fun toEligibilitySnapshotPayload(snapshot: JSONObject): AdvanceEligibilitySnapshotPayload {
        return AdvanceEligibilitySnapshotPayload(
            assetSymbol = snapshot.getString("assetSymbol"),
            assetDecimals = snapshot.optInt("assetDecimals", 6),
            exchangeRateSnapshot = snapshot.getBigDecimalCompat("exchangeRateSnapshot"),
            availableAmountAtomic = snapshot.getLong("availableAmountAtomic"),
            availableDisplayKrwAmount = snapshot.getLong("availableDisplayKrwAmount"),
            maxCapAmountAtomic = snapshot.getLong("maxCapAmountAtomic"),
            maxCapDisplayKrwAmount = snapshot.getLong("maxCapDisplayKrwAmount"),
            policyRate = snapshot.get("policyRate").toString(),
            reflectedWorkDays = snapshot.getInt("reflectedWorkDays"),
            reflectedWorkMinutes = snapshot.getLong("reflectedWorkMinutes"),
            needsReviewRecordCount = snapshot.getInt("needsReviewRecordCount")
        )
    }

    private data class WorkplacePayload(
        val id: Long,
        val name: String
    )
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    if (isNull(key)) {
        return null
    }
    return getLong(key)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) {
        return null
    }
    return getString(key)
}

private fun JSONObject.getBigDecimalCompat(key: String): BigDecimal =
    BigDecimal(get(key).toString())
