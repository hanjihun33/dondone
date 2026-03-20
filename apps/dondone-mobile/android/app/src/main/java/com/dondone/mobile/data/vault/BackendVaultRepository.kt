package com.dondone.mobile.data.vault

import com.dondone.mobile.data.remote.BackendApiSupport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID

private const val SESSION_EXPIRED_MESSAGE = "세션이 만료되어 다시 로그인해 주세요."
private const val VAULT_LOGIN_MESSAGE = "로그인 후 예치 실연동 데이터를 불러옵니다."
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendVaultRepository(
    private val client: OkHttpClient = OkHttpClient()
) : VaultRepository {

    override suspend fun load(accessToken: String): VaultRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext VaultRemoteState.unauthenticated(VAULT_LOGIN_MESSAGE)
        }

        runCatching {
            val summary = fetchSummary(accessToken)
            val latestTransaction = runCatching {
                fetchLatestTransaction(accessToken)
            }.getOrNull()

            VaultRemoteState.content(
                VaultRemotePayload(
                    summary = summary,
                    latestTransaction = latestTransaction
                )
            )
        }.getOrElse { error ->
            when (error) {
                is VaultUnauthorizedException -> VaultRemoteState.unauthenticated(
                    error.message ?: SESSION_EXPIRED_MESSAGE
                )

                else -> VaultRemoteState.error(
                    error.message ?: "예치 실연동 데이터를 불러오지 못했어요."
                )
            }
        }
    }

    override suspend fun createDeposit(
        accessToken: String,
        amountAtomic: Long
    ): VaultCreateTransactionPayload = withContext(Dispatchers.IO) {
        createTransaction(
            accessToken = accessToken,
            amountAtomic = amountAtomic,
            path = "/api/vault/deposits",
            fallbackMessage = "예치 요청을 생성하지 못했어요.",
            idempotencyPrefix = "android-vault-deposit"
        )
    }

    override suspend fun createWithdrawal(
        accessToken: String,
        amountAtomic: Long
    ): VaultCreateTransactionPayload = withContext(Dispatchers.IO) {
        createTransaction(
            accessToken = accessToken,
            amountAtomic = amountAtomic,
            path = "/api/vault/withdrawals",
            fallbackMessage = "출금 요청을 생성하지 못했어요.",
            idempotencyPrefix = "android-vault-withdraw"
        )
    }

    override suspend fun getTransactionDetail(
        accessToken: String,
        requestId: String
    ): VaultTransactionDetailPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        fetchTransactionDetail(
            accessToken = accessToken,
            requestId = requestId,
            fallbackMessage = "예치 상태를 불러오지 못했어요."
        )
    }

    private fun fetchSummary(accessToken: String): VaultSummaryPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/vault/summary")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "예치 요약을 불러오지 못했어요.")
        return data.toSummaryPayload()
    }

    private fun fetchLatestTransaction(accessToken: String): VaultTransactionDetailPayload? {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/vault/transactions?limit=1")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "예치 상태를 불러오지 못했어요.")
        val latestItem = data.optJSONArray("transactions").toTransactionItemPayloads().firstOrNull()
            ?: return null
        return fetchTransactionDetail(
            accessToken = accessToken,
            requestId = latestItem.requestId,
            fallbackMessage = "예치 상태를 불러오지 못했어요."
        )
    }

    private fun fetchTransactionDetail(
        accessToken: String,
        requestId: String,
        fallbackMessage: String
    ): VaultTransactionDetailPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/vault/transactions/$requestId")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, fallbackMessage)
        return data.toTransactionDetailPayload()
    }

    private fun createTransaction(
        accessToken: String,
        amountAtomic: Long,
        path: String,
        fallbackMessage: String,
        idempotencyPrefix: String
    ): VaultCreateTransactionPayload {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("amountAtomic", amountAtomic)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}$path")
            .header("Authorization", "Bearer $accessToken")
            .header("Idempotency-Key", "$idempotencyPrefix-${UUID.randomUUID()}")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, fallbackMessage)
        return data.toCreateTransactionPayload()
    }

    private fun executeForData(request: Request, fallbackMessage: String): JSONObject {
        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val json = JSONObject(responseBody.ifBlank { "{}" })
            if (!response.isSuccessful) {
                throw parseError(
                    statusCode = response.code,
                    responseBody = json,
                    fallbackMessage = fallbackMessage
                )
            }
            return json.optJSONObject("data") ?: JSONObject()
        }
    }

    private fun parseError(
        statusCode: Int,
        responseBody: JSONObject,
        fallbackMessage: String
    ): IOException {
        val code = responseBody.optString("code").ifBlank { null }
        val message = responseBody.optString("message").ifBlank { fallbackMessage }
        return if (statusCode == 401 || statusCode == 403 || code == "UNAUTHORIZED" || code == "INVALID_TOKEN") {
            VaultUnauthorizedException(message)
        } else {
            VaultApiException(code = code, message = message)
        }
    }

    private fun requireAuthorized(accessToken: String) {
        if (accessToken.isBlank()) {
            throw VaultUnauthorizedException()
        }
    }
}

private fun JSONObject.toSummaryPayload(): VaultSummaryPayload =
    VaultSummaryPayload(
        walletAddress = getString("walletAddress"),
        vaultAddress = getString("vaultAddress"),
        network = getString("network"),
        assetSymbol = getString("assetSymbol"),
        assetDecimals = optInt("assetDecimals", 6),
        storedAmountAtomic = getString("storedAmountAtomic"),
        accruedYieldAtomic = getString("accruedYieldAtomic"),
        walletTokenBalanceAtomic = getString("walletTokenBalanceAtomic"),
        availableToStoreAmountAtomic = getString("availableToStoreAmountAtomic"),
        shareBalance = getString("shareBalance"),
        interestPreview = optJSONObject("interestPreview")?.toInterestPreviewPayload()
            ?: VaultInterestPreviewPayload(
                dailyEstimatedYieldAtomic = "0",
                monthlyEstimatedYieldAtomic = "0",
                yearlyEstimatedYieldAtomic = "0",
                apyBps = 0
            ),
        disclaimer = optNullableString("disclaimer").orEmpty()
    )

private fun JSONObject.toInterestPreviewPayload(): VaultInterestPreviewPayload =
    VaultInterestPreviewPayload(
        dailyEstimatedYieldAtomic = getString("dailyEstimatedYieldAtomic"),
        monthlyEstimatedYieldAtomic = getString("monthlyEstimatedYieldAtomic"),
        yearlyEstimatedYieldAtomic = getString("yearlyEstimatedYieldAtomic"),
        apyBps = optInt("apyBps", 0)
    )

private fun JSONObject.toCreateTransactionPayload(): VaultCreateTransactionPayload =
    VaultCreateTransactionPayload(
        requestId = getString("requestId"),
        txType = getString("txType"),
        status = getString("status"),
        detailPath = optNullableString("detailPath"),
        createdAt = optDateTime("createdAt")
    )

private fun JSONObject.toTransactionItemPayload(): VaultTransactionItemPayload =
    VaultTransactionItemPayload(
        requestId = getString("requestId"),
        txType = getString("txType"),
        status = getString("status"),
        amountAtomic = getString("amountAtomic"),
        shareDelta = optNullableString("shareDelta"),
        txHash = optNullableString("txHash"),
        failureCode = optNullableString("failureCode"),
        updatedAt = optDateTime("updatedAt")
    )

private fun JSONObject.toTransactionDetailPayload(): VaultTransactionDetailPayload =
    VaultTransactionDetailPayload(
        requestId = getString("requestId"),
        txType = getString("txType"),
        status = getString("status"),
        walletAddress = getString("walletAddress"),
        vaultAddress = getString("vaultAddress"),
        assetSymbol = getString("assetSymbol"),
        amountAtomic = getString("amountAtomic"),
        shareDelta = optNullableString("shareDelta"),
        txHash = optNullableString("txHash"),
        failureCode = optNullableString("failureCode"),
        createdAt = optDateTime("createdAt"),
        updatedAt = optDateTime("updatedAt"),
        confirmedAt = optDateTime("confirmedAt")
    )

private fun JSONArray?.toTransactionItemPayloads(): List<VaultTransactionItemPayload> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(item.toTransactionItemPayload())
        }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key)
        .trim()
        .takeUnless { value -> value.isEmpty() || value.equals("null", ignoreCase = true) }
}

private fun JSONObject.optDateTime(key: String): LocalDateTime? =
    optNullableString(key)?.let(LocalDateTime::parse)

class VaultUnauthorizedException(
    message: String = SESSION_EXPIRED_MESSAGE
) : IOException(message)

class VaultApiException(
    val code: String? = null,
    message: String
) : IOException(message)
