package com.dondone.mobile.data.wage

import com.dondone.mobile.data.remote.BackendApiException
import com.dondone.mobile.data.remote.BackendApiSupport
import com.dondone.mobile.data.remote.parseBackendErrorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

private const val SESSION_EXPIRED_MESSAGE = "세션이 만료되어 다시 로그인해 주세요."
private const val WAGE_LOGIN_MESSAGE = "로그인 후 급여 점검 데이터를 불러옵니다."
private const val ACTIVE_CONTRACT_NOT_FOUND_CODE = "ACTIVE_CONTRACT_NOT_FOUND"
private const val ACTIVE_CONTRACT_NOT_FOUND_BACKEND_MESSAGE = "Active contract not found"
private const val ACTIVE_CONTRACT_NOT_FOUND_USER_MESSAGE =
    "활성 근로계약이 확인되지 않았어요. 소속 관리자에게 근로계약 등록을 요청해 주세요."
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendWageRepository(
    private val client: OkHttpClient = OkHttpClient()
) : WageRepository {

    override suspend fun load(
        accessToken: String,
        month: YearMonth,
        asOf: LocalDate,
        paydayDay: Int
    ): WageRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext WageRemoteState.unauthenticated(WAGE_LOGIN_MESSAGE)
        }

        runCatching {
            val workplace = fetchPrimaryWorkplace(accessToken)
                ?: return@withContext WageRemoteState.empty("연결된 근무지가 없어 급여 점검을 불러올 수 없어요.")
            val monthlySummary = fetchMonthlySummary(accessToken, month, workplace.id)
            val estimate = fetchEstimate(accessToken, month, workplace.id)
            val summary = fetchLegacySummary(
                accessToken = accessToken,
                month = month,
                asOf = asOf,
                normalizedHourlyWage = monthlySummary.normalizedHourlyWage,
                paydayDay = paydayDay
            )

            WageRemoteState.content(
                WageRemotePayload(
                    workplaceName = workplace.name,
                    month = month,
                    workplaceId = workplace.id,
                    monthlySummary = monthlySummary,
                    estimate = estimate,
                    summary = summary
                )
            )
        }.getOrElse { error ->
            when (error) {
                is WageUnauthorizedException -> WageRemoteState.unauthenticated(
                    error.message ?: SESSION_EXPIRED_MESSAGE
                )

                else -> WageRemoteState.error(
                    error.message ?: "급여 점검 데이터를 불러오지 못했어요."
                )
            }
        }
    }

    override suspend fun createDeposit(
        accessToken: String,
        month: YearMonth,
        depositDate: LocalDate,
        actualDepositAmount: Long,
        deductionsKnown: Boolean,
        note: String?
    ): WageDepositRecordPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("yearMonth", month.toString())
            .put("depositDate", depositDate.toString())
            .put("actualDepositAmount", actualDepositAmount)
            .put("deductionsKnown", deductionsKnown)
            .put("note", note ?: JSONObject.NULL)
            .toString()
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/wage/deposits"
        )
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(
            request = request,
            fallbackMessage = "실제 입금액을 저장하지 못했어요."
        )
        return@withContext data.toDepositPayload()
    }

    override suspend fun createVerification(
        accessToken: String,
        month: YearMonth,
        workplaceId: Long,
        actualDepositAmount: Long,
        deductionsKnown: Boolean,
        memo: String?
    ): WageVerificationCreatedPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("month", month.toString())
            .put("workplaceId", workplaceId)
            .put("actualDepositAmount", actualDepositAmount)
            .put("deductionsKnown", deductionsKnown)
            .put("memo", memo ?: JSONObject.NULL)
            .toString()
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/wage/verifications"
        )
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(
            request = request,
            fallbackMessage = "급여 확인 결과를 생성하지 못했어요."
        )
        return@withContext data.toVerificationCreatedPayload()
    }

    override suspend fun getVerificationDetail(
        accessToken: String,
        verificationId: Long
    ): WageVerificationDetailPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/wage/verifications/$verificationId"
        )
            .get()
            .build()

        val data = executeForData(
            request = request,
            fallbackMessage = "급여 확인 상세를 불러오지 못했어요."
        )
        return@withContext data.toVerificationDetailPayload()
    }

    private fun fetchPrimaryWorkplace(accessToken: String): WorkplacePayload? {
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/workproof/workplaces"
        )
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw parseError(
                    statusCode = response.code,
                    responseBody = responseBody,
                    fallbackMessage = "근무지를 불러오지 못했어요."
                )
            }
            val workplaces = JSONObject(responseBody.ifBlank { "{}" })
                .optJSONObject("data")
                ?.optJSONArray("workplaces")
                ?: JSONArray()
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

    private fun fetchMonthlySummary(
        accessToken: String,
        month: YearMonth,
        workplaceId: Long
    ): WageMonthlySummaryPayload {
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/wage/monthly-summary?month=$month&workplaceId=$workplaceId"
        )
            .get()
            .build()
        val data = executeForData(
            request = request,
            fallbackMessage = "이번 달 반영 근무 요약을 불러오지 못했어요."
        )
        return data.toMonthlySummaryPayload()
    }

    private fun fetchEstimate(
        accessToken: String,
        month: YearMonth,
        workplaceId: Long
    ): WageEstimatePayload {
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url = "${BackendApiSupport.baseUrl}/api/wage/estimate?month=$month&workplaceId=$workplaceId"
        )
            .get()
            .build()
        val data = executeForData(
            request = request,
            fallbackMessage = "참고용 급여 추정을 불러오지 못했어요."
        )
        return data.toEstimatePayload()
    }

    private fun fetchLegacySummary(
        accessToken: String,
        month: YearMonth,
        asOf: LocalDate,
        normalizedHourlyWage: Long,
        paydayDay: Int
    ): WageLegacySummaryPayload {
        val request = authorizedRequestBuilder(
            accessToken = accessToken,
            url =
                "${BackendApiSupport.baseUrl}/api/wage/summary" +
                    "?yearMonth=$month" +
                    "&asOf=$asOf" +
                    "&normalizedHourlyWage=$normalizedHourlyWage" +
                    "&paydayDay=$paydayDay"
        )
            .get()
            .build()
        val data = executeForData(
            request = request,
            fallbackMessage = "입금 확인 요약을 불러오지 못했어요."
        )
        return data.toLegacySummaryPayload()
    }

    private fun executeForData(
        request: Request,
        fallbackMessage: String
    ): JSONObject {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (!response.isSuccessful) {
                throw parseError(
                    statusCode = response.code,
                    responseBody = responseBody,
                    fallbackMessage = fallbackMessage
                )
            }
            return json.optJSONObject("data") ?: JSONObject()
        }
    }

    private fun parseError(
        statusCode: Int,
        responseBody: String,
        fallbackMessage: String
    ): Exception {
        val message = resolveWageErrorMessage(responseBody, fallbackMessage)
        return if (statusCode == 401 || statusCode == 403) {
            WageUnauthorizedException(message)
        } else {
            BackendApiException(message)
        }
    }

    private fun resolveWageErrorMessage(
        responseBody: String,
        fallbackMessage: String
    ): String {
        val rawBody = responseBody.trim()
        if (
            rawBody.contains(ACTIVE_CONTRACT_NOT_FOUND_CODE, ignoreCase = true) ||
            rawBody.contains(ACTIVE_CONTRACT_NOT_FOUND_BACKEND_MESSAGE, ignoreCase = true)
        ) {
            return ACTIVE_CONTRACT_NOT_FOUND_USER_MESSAGE
        }

        val json = runCatching { JSONObject(responseBody.ifBlank { "{}" }) }.getOrNull()
        val backendCode = json?.optString("code").orEmpty()
        val backendMessage = json?.optString("message").orEmpty()

        if (
            backendCode == ACTIVE_CONTRACT_NOT_FOUND_CODE ||
            backendMessage.equals(ACTIVE_CONTRACT_NOT_FOUND_BACKEND_MESSAGE, ignoreCase = true)
        ) {
            return ACTIVE_CONTRACT_NOT_FOUND_USER_MESSAGE
        }

        val parsedMessage = parseBackendErrorMessage(responseBody, fallbackMessage)
        if (
            parsedMessage.contains(ACTIVE_CONTRACT_NOT_FOUND_CODE, ignoreCase = true) ||
            parsedMessage.contains(ACTIVE_CONTRACT_NOT_FOUND_BACKEND_MESSAGE, ignoreCase = true)
        ) {
            return ACTIVE_CONTRACT_NOT_FOUND_USER_MESSAGE
        }

        return parsedMessage
    }

    private fun requireAuthorized(accessToken: String) {
        if (accessToken.isBlank()) {
            throw WageUnauthorizedException()
        }
    }

    private fun authorizedRequestBuilder(
        accessToken: String,
        url: String
    ): Request.Builder = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer $accessToken")

    private fun JSONObject.toMonthlySummaryPayload(): WageMonthlySummaryPayload =
        WageMonthlySummaryPayload(
            month = YearMonth.parse(getString("month")),
            workplaceId = getLong("workplaceId"),
            contractId = getLong("contractId"),
            payUnit = getString("payUnit"),
            normalizedHourlyWage = getFlexibleLong("normalizedHourlyWage"),
            workDayCount = getInt("workDayCount"),
            verifiedWorkMinutes = getLong("verifiedWorkMinutes"),
            overtimeMinutes = getLong("overtimeMinutes"),
            nightMinutes = getLong("nightMinutes"),
            modifiedRecordCount = getInt("modifiedRecordCount"),
            includedRecordIds = optLongList(optJSONArray("includedRecordIds")),
            excludedPendingRecordCount = getInt("excludedPendingRecordCount")
        )

    private fun JSONObject.toEstimatePayload(): WageEstimatePayload {
        val estimate = getJSONObject("estimate")
        return WageEstimatePayload(
            month = YearMonth.parse(getString("month")),
            workplaceId = getLong("workplaceId"),
            ruleVersion = getString("ruleVersion"),
            disclaimer = getString("disclaimer"),
            baseEstimate = estimate.getLong("baseEstimate"),
            overtimePremium = estimate.getLong("overtimePremium"),
            nightPremium = estimate.getLong("nightPremium"),
            estimatedTotal = estimate.getLong("estimatedTotal")
        )
    }

    private fun JSONObject.toLegacySummaryPayload(): WageLegacySummaryPayload =
        WageLegacySummaryPayload(
            yearMonth = YearMonth.parse(getString("yearMonth")),
            asOf = optLocalDate("asOf"),
            workDays = getInt("workDays"),
            totalWorkedMinutes = getLong("totalWorkedMinutes"),
            overtimeMinutes = getLong("overtimeMinutes"),
            nightMinutes = getLong("nightMinutes"),
            normalizedHourlyWage = getFlexibleLong("normalizedHourlyWage"),
            estimatedBaseAmount = getLong("estimatedBaseAmount"),
            estimatedOvertimePremiumAmount = getLong("estimatedOvertimePremiumAmount"),
            estimatedNightPremiumAmount = getLong("estimatedNightPremiumAmount"),
            estimatedTotalAmount = getLong("estimatedTotalAmount"),
            actualDepositAmount = optLongValue("actualDepositAmount"),
            actualDepositRecordedDate = optLocalDate("actualDepositRecordedDate"),
            actualDepositRecordedDay = optIntValue("actualDepositRecordedDay"),
            deductionsKnown = optBoolean("deductionsKnown", false),
            paydayDay = getInt("paydayDay"),
            differenceAmount = optLongValue("differenceAmount"),
            anomalyTriggerAmount = getLong("anomalyTriggerAmount"),
            anomalyDetected = optBoolean("anomalyDetected", false),
            status = getString("status"),
            disclaimer = getString("disclaimer"),
            modifiedRecordCount = getInt("modifiedRecordCount"),
            reflectedRecordCount = getInt("reflectedRecordCount"),
            pendingRecordCount = getInt("pendingRecordCount"),
            relatedWorkProofIds = optLongList(optJSONArray("relatedWorkProofIds")),
            reasons = optReasonList(optJSONArray("reasons"))
        )

    private fun JSONObject.toDepositPayload(): WageDepositRecordPayload =
        WageDepositRecordPayload(
            id = getLong("id"),
            yearMonth = YearMonth.parse(getString("yearMonth")),
            depositDate = LocalDate.parse(getString("depositDate")),
            actualDepositAmount = getLong("actualDepositAmount"),
            deductionsKnown = getBoolean("deductionsKnown"),
            note = optNullableString("note")
        )

    private fun JSONObject.toVerificationCreatedPayload(): WageVerificationCreatedPayload =
        WageVerificationCreatedPayload(
            verificationId = getLong("verificationId"),
            status = getString("status"),
            resolutionStage = getString("resolutionStage"),
            estimatedTotal = getLong("estimatedTotal"),
            actualDepositAmount = getLong("actualDepositAmount"),
            differenceAmount = getLong("differenceAmount"),
            differenceRate = get("differenceRate").toString(),
            threshold = getJSONObject("threshold").toThresholdPayload(),
            possibleCauses = optPossibleCauseList(optJSONArray("possibleCauses")),
            evidence = getJSONObject("evidence").toEvidencePayload(),
            nextActions = optStringList(optJSONArray("nextActions"))
        )

    private fun JSONObject.toVerificationDetailPayload(): WageVerificationDetailPayload {
        val estimated = getJSONObject("estimated")
        val actual = getJSONObject("actual")
        val difference = getJSONObject("difference")
        return WageVerificationDetailPayload(
            verificationId = getLong("verificationId"),
            month = YearMonth.parse(getString("month")),
            workplaceId = getLong("workplaceId"),
            status = getString("status"),
            resolutionStage = getString("resolutionStage"),
            estimatedBaseAmount = estimated.getLong("baseEstimate"),
            estimatedOvertimePremium = estimated.getLong("overtimePremium"),
            estimatedNightPremium = estimated.getLong("nightPremium"),
            estimatedTotal = estimated.getLong("estimatedTotal"),
            actualDepositAmount = actual.getLong("actualDepositAmount"),
            deductionsKnown = actual.getBoolean("deductionsKnown"),
            submittedBy = actual.getString("submittedBy"),
            differenceAmount = difference.getLong("differenceAmount"),
            differenceRate = difference.get("differenceRate").toString(),
            threshold = getJSONObject("threshold").toThresholdPayload(),
            possibleCauses = optPossibleCauseList(optJSONArray("possibleCauses")),
            evidence = getJSONObject("evidence").toEvidencePayload(),
            employerSupport = getJSONObject("employerSupport").toEmployerSupportPayload(),
            relatedActions = getJSONObject("relatedActions").toRelatedActionsPayload()
        )
    }

    private fun JSONObject.toThresholdPayload(): WageThresholdPayload =
        WageThresholdPayload(
            absoluteWon = getLong("absoluteWon"),
            relativePercent = get("relativePercent").toString(),
            deductionRelaxed = getBoolean("deductionRelaxed")
        )

    private fun JSONObject.toEvidencePayload(): WageEvidencePayload =
        WageEvidencePayload(
            overtimeMinutes = getLong("overtimeMinutes"),
            nightMinutes = getLong("nightMinutes"),
            modifiedRecordCount = getInt("modifiedRecordCount"),
            recordIds = optLongList(optJSONArray("recordIds"))
        )

    private fun JSONObject.toEmployerSupportPayload(): WageEmployerSupportPayload =
        WageEmployerSupportPayload(
            available = getBoolean("available"),
            recommended = getBoolean("recommended"),
            status = getString("status")
        )

    private fun JSONObject.toRelatedActionsPayload(): WageRelatedActionsPayload =
        WageRelatedActionsPayload(
            proofPackReady = getBoolean("proofPackReady"),
            claimKitReady = getBoolean("claimKitReady"),
            instantClaimAvailable = getBoolean("instantClaimAvailable"),
            proofPackDocumentId = optLongValue("proofPackDocumentId"),
            claimKitDocumentId = optLongValue("claimKitDocumentId"),
            preparationId = optLongValue("preparationId")
        )

    private fun optPossibleCauseList(jsonArray: JSONArray?): List<WagePossibleCausePayload> {
        return jsonArray.mapObjects { item ->
            WagePossibleCausePayload(
                code = item.optString("code"),
                title = item.optString("title"),
                detail = item.optString("detail")
            )
        }
    }

    private fun optReasonList(jsonArray: JSONArray?): List<WageDifferenceReasonPayload> {
        return jsonArray.mapObjects { item ->
            WageDifferenceReasonPayload(
                code = item.optString("code"),
                title = item.optString("title"),
                description = item.optString("description"),
                relatedWorkProofIds = optLongList(item.optJSONArray("relatedWorkProofIds"))
            )
        }
    }

    private fun optStringList(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        return buildList {
            for (index in 0 until jsonArray.length()) {
                add(jsonArray.optString(index))
            }
        }
    }

    private fun optLongList(jsonArray: JSONArray?): List<Long> {
        if (jsonArray == null) return emptyList()
        return buildList {
            for (index in 0 until jsonArray.length()) {
                add(jsonArray.optLong(index))
            }
        }
    }

    private inline fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(transform(item))
            }
        }
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).trim().takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
    }

    private fun JSONObject.optLocalDate(key: String): LocalDate? =
        optNullableString(key)?.let(LocalDate::parse)

    private fun JSONObject.optLongValue(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return when (val value = get(key)) {
            is Number -> value.toLong()
            else -> value.toString().toBigDecimalOrNull()?.toLong()
        }
    }

    private fun JSONObject.optIntValue(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return when (val value = get(key)) {
            is Number -> value.toInt()
            else -> value.toString().toIntOrNull()
        }
    }

    private fun JSONObject.getFlexibleLong(key: String): Long {
        return when (val value = get(key)) {
            is Number -> value.toLong()
            else -> value.toString().toBigDecimalOrNull()?.toLong()
                ?: throw BackendApiException("숫자 응답을 파싱하지 못했어요: $key")
        }
    }

    private data class WorkplacePayload(
        val id: Long,
        val name: String
    )
}
