package com.dondone.mobile.data.remittance

import com.dondone.mobile.data.auth.AuthUnauthorizedException
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
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID

private const val SESSION_EXPIRED_MESSAGE = "세션이 만료되어 다시 로그인해 주세요."
private const val REMITTANCE_LOGIN_MESSAGE = "로그인 후 송금 실연동 데이터를 불러옵니다."
private val JSON_MEDIA_TYPE = "application/json".toMediaType()

class BackendRemittanceRepository(
    private val client: OkHttpClient = OkHttpClient()
) : RemittanceRepository {

    override suspend fun load(accessToken: String): RemittanceRemoteState = withContext(Dispatchers.IO) {
        if (accessToken.isBlank()) {
            return@withContext RemittanceRemoteState.unauthenticated(REMITTANCE_LOGIN_MESSAGE)
        }

        runCatching {
            val wallet = ensureWallet(accessToken)
            val balance = if (wallet.fundingStatus == "FUNDED") {
                fetchWalletBalance(accessToken)
            } else {
                null
            }
            val recipients = fetchRecipients(accessToken)
            val ledgerItems = fetchLedgerItems(accessToken)
            val transfers = ledgerItems.toTransferSummaryPayloads()
            val activeTransfer = ledgerItems.firstOrNull { it.entryType == "REMITTANCE_TRANSFER" }
                ?.entryId
                ?.let { fetchTransferDetail(accessToken, it) }

            RemittanceRemoteState.content(
                RemittanceRemotePayload(
                    wallet = wallet,
                    balance = balance,
                    recipients = recipients,
                    ledgerItems = ledgerItems,
                    transfers = transfers,
                    activeTransfer = activeTransfer
                )
            )
        }.getOrElse { error ->
            when (error) {
                is RemittanceUnauthorizedException -> RemittanceRemoteState.unauthenticated(
                    error.message ?: SESSION_EXPIRED_MESSAGE
                )

                else -> RemittanceRemoteState.error(
                    error.message ?: "송금 실연동 데이터를 불러오지 못했어요."
                )
            }
        }
    }

    override suspend fun searchRecipientsByPhone(
        accessToken: String,
        phoneNumber: String
    ): List<RemittanceRecipientSearchPayload> = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("phoneNumber", phoneNumber)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/recipients/search")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, "휴대폰 번호 검색에 실패했어요.")
        return@withContext data.optJSONArray("candidates").toRecipientSearchPayloads()
    }

    override suspend fun createRecipient(
        accessToken: String,
        alias: String,
        relation: String,
        walletAddress: String,
        targetUserId: Long?
    ): RemittanceRecipientPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("alias", alias.trim())
            .put("relation", relation)
            .put("allowed", true)
        if (targetUserId != null) {
            body.put("targetUserId", targetUserId)
        } else {
            body.put("walletAddress", walletAddress.trim())
        }
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/recipients")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, "수신자를 등록하지 못했어요.")
        return@withContext data.toRecipientPayload()
    }

    override suspend fun updateRecipient(
        accessToken: String,
        recipientId: String,
        alias: String,
        relation: String,
        walletAddress: String
    ): RemittanceRecipientPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("alias", alias.trim())
            .put("relation", relation)
            .put("walletAddress", walletAddress.trim())
            .put("targetUserId", JSONObject.NULL)
            .put("allowed", true)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/recipients/$recipientId")
            .header("Authorization", "Bearer $accessToken")
            .put(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, "수신자를 수정하지 못했어요.")
        return@withContext data.toRecipientPayload()
    }

    override suspend fun precheck(
        accessToken: String,
        recipientId: String,
        amountAtomic: Long,
        highAmountConfirmed: Boolean,
        recentRecipientConfirmed: Boolean
    ): RemittanceTransferPrecheckPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("recipientId", recipientId)
            .put("amountAtomic", amountAtomic)
            .put("highAmountConfirmed", highAmountConfirmed)
            .put("recentRecipientConfirmed", recentRecipientConfirmed)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/transfers/precheck")
            .header("Authorization", "Bearer $accessToken")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, "송금 가능 여부를 확인하지 못했어요.")
        return@withContext data.toPrecheckPayload()
    }

    override suspend fun createTransfer(
        accessToken: String,
        recipientId: String,
        amountAtomic: Long,
        highAmountConfirmed: Boolean,
        recentRecipientConfirmed: Boolean
    ): RemittanceCreateTransferPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        val body = JSONObject()
            .put("recipientId", recipientId)
            .put("amountAtomic", amountAtomic)
            .put("highAmountConfirmed", highAmountConfirmed)
            .put("recentRecipientConfirmed", recentRecipientConfirmed)
            .toString()
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/transfers")
            .header("Authorization", "Bearer $accessToken")
            .header("Idempotency-Key", "android-remit-${UUID.randomUUID()}")
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val data = executeForData(request, "송금 요청을 생성하지 못했어요.")
        return@withContext data.toCreateTransferPayload()
    }

    override suspend fun getTransferDetail(
        accessToken: String,
        transferId: String
    ): RemittanceTransferDetailPayload = withContext(Dispatchers.IO) {
        requireAuthorized(accessToken)
        fetchTransferDetail(accessToken, transferId)
    }

    private fun ensureWallet(accessToken: String): RemittanceWalletPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/wallets/me")
            .header("Authorization", "Bearer $accessToken")
            .post("".toRequestBody(null))
            .build()

        return try {
            executeForData(request, "송금 지갑을 준비하지 못했어요.").toWalletPayload()
        } catch (error: RemittanceApiException) {
            if (error.code == "WALLET_FUNDING_FAILED") {
                fetchWallet(accessToken)
            } else {
                throw error
            }
        }
    }

    private fun fetchWallet(accessToken: String): RemittanceWalletPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/wallets/me")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "송금 지갑 상태를 확인하지 못했어요.")
        return data.toWalletPayload()
    }

    private fun fetchWalletBalance(accessToken: String): RemittanceWalletBalancePayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/wallets/me/balance")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "송금 지갑 잔액을 확인하지 못했어요.")
        return data.toWalletBalancePayload()
    }

    private fun fetchRecipients(accessToken: String): List<RemittanceRecipientPayload> {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/recipients")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "수신자 목록을 불러오지 못했어요.")
        return data.optJSONArray("recipients").toRecipientPayloads()
    }

    private fun fetchLedgerItems(accessToken: String): List<RemittanceLedgerItemPayload> {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/wallets/me/ledger")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "지갑 거래내역을 불러오지 못했어요.")
        return data.optJSONArray("entries").toLedgerItemPayloads()
    }

    private fun fetchTransferDetail(
        accessToken: String,
        transferId: String
    ): RemittanceTransferDetailPayload {
        val request = Request.Builder()
            .url("${BackendApiSupport.baseUrl}/api/remittance/transfers/$transferId")
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val data = executeForData(request, "송금 상태를 불러오지 못했어요.")
        return data.toTransferDetailPayload()
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
            RemittanceUnauthorizedException(message)
        } else {
            RemittanceApiException(code = code, message = message)
        }
    }

    private fun requireAuthorized(accessToken: String) {
        if (accessToken.isBlank()) {
            throw RemittanceUnauthorizedException()
        }
    }
}

private fun JSONObject.toWalletPayload(): RemittanceWalletPayload =
    RemittanceWalletPayload(
        walletAddress = getString("walletAddress"),
        fundingStatus = getString("fundingStatus"),
        fundingFailureReason = optNullableString("fundingFailureReason"),
        fundedAt = optDateTime("fundedAt"),
        createdAt = optDateTime("createdAt")
    )

private fun JSONObject.toWalletBalancePayload(): RemittanceWalletBalancePayload =
    RemittanceWalletBalancePayload(
        walletAddress = getString("walletAddress"),
        assetSymbol = getString("assetSymbol"),
        assetDecimals = optInt("assetDecimals", 6),
        tokenBalanceAtomic = getString("tokenBalanceAtomic"),
        nativeBalanceWei = getString("nativeBalanceWei")
    )

private fun JSONObject.toRecipientPayload(): RemittanceRecipientPayload =
    RemittanceRecipientPayload(
        recipientId = getString("recipientId"),
        alias = getString("alias"),
        relation = getString("relation"),
        walletAddress = getString("walletAddress"),
        allowed = optBoolean("allowed", true),
        recentlyUpdated = optBoolean("recentlyUpdated", false),
        updatedAt = optDateTime("updatedAt")
    )

private fun JSONObject.toPrecheckPayload(): RemittanceTransferPrecheckPayload =
    RemittanceTransferPrecheckPayload(
        allowed = optBoolean("allowed"),
        policyCode = optNullableString("policyCode"),
        assetSymbol = getString("assetSymbol"),
        highAmountThresholdAtomic = optLong("highAmountThresholdAtomic"),
        recentRecipientConfirmationRequired = optBoolean("recentRecipientConfirmationRequired"),
        recentRecipientUpdatedAt = optDateTime("recentRecipientUpdatedAt"),
        walletAddress = getString("walletAddress"),
        currentTokenBalanceAtomic = getString("currentTokenBalanceAtomic"),
        currentNativeBalanceWei = getString("currentNativeBalanceWei")
    )

private fun JSONObject.toCreateTransferPayload(): RemittanceCreateTransferPayload =
    RemittanceCreateTransferPayload(
        transferId = getString("transferId"),
        status = getString("status"),
        assetSymbol = getString("assetSymbol"),
        amountAtomic = getLong("amountAtomic"),
        recipientId = getString("recipientId"),
        createdAt = optDateTime("createdAt")
    )

private fun JSONObject.toTransferDetailPayload(): RemittanceTransferDetailPayload =
    RemittanceTransferDetailPayload(
        transferId = getString("transferId"),
        direction = getString("direction"),
        status = getString("status"),
        assetSymbol = getString("assetSymbol"),
        amountAtomic = getLong("amountAtomic"),
        senderAddress = getString("senderAddress"),
        senderName = optNullableString("senderName"),
        recipientId = getString("recipientId"),
        recipientAlias = optNullableString("recipientAlias"),
        recipientAddress = getString("recipientAddress"),
        txHash = optNullableString("txHash"),
        networkFeeWei = optNullableString("networkFeeWei"),
        networkFeeAssetSymbol = optNullableString("networkFeeAssetSymbol"),
        failureCode = optNullableString("failureCode"),
        createdAt = optDateTime("createdAt"),
        updatedAt = optDateTime("updatedAt")
    )

private fun JSONArray?.toRecipientPayloads(): List<RemittanceRecipientPayload> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            add(getJSONObject(index).toRecipientPayload())
        }
    }
}

private fun JSONArray?.toRecipientSearchPayloads(): List<RemittanceRecipientSearchPayload> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                RemittanceRecipientSearchPayload(
                    candidateUserId = item.optLong("candidateUserId"),
                    displayName = item.optString("displayName"),
                    maskedPhoneNumber = item.optString("maskedPhoneNumber"),
                    walletAddressMasked = item.optString("walletAddressMasked"),
                    alreadyRegistered = item.optBoolean("alreadyRegistered", false)
                )
            )
        }
    }
}

private fun JSONArray?.toTransferSummaryPayloads(): List<RemittanceTransferSummaryPayload> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                RemittanceTransferSummaryPayload(
                    transferId = item.getString("transferId"),
                    direction = item.getString("direction"),
                    status = item.getString("status"),
                    assetSymbol = item.getString("assetSymbol"),
                    amountAtomic = item.getLong("amountAtomic"),
                    senderAddress = item.getString("senderAddress"),
                    senderName = item.optNullableString("senderName"),
                    recipientId = item.getString("recipientId"),
                    recipientAlias = item.optNullableString("recipientAlias"),
                    recipientAddress = item.getString("recipientAddress"),
                    txHash = item.optNullableString("txHash"),
                    networkFeeWei = item.optNullableString("networkFeeWei"),
                    networkFeeAssetSymbol = item.optNullableString("networkFeeAssetSymbol"),
                    updatedAt = item.optDateTime("updatedAt")
                )
            )
        }
    }
}

private fun JSONArray?.toLedgerItemPayloads(): List<RemittanceLedgerItemPayload> {
    if (this == null) return emptyList()
    return buildList(length()) {
        for (index in 0 until length()) {
            val item = optJSONObject(index) ?: continue
            add(
                RemittanceLedgerItemPayload(
                    entryId = item.getString("entryId"),
                    entryType = item.getString("entryType"),
                    direction = item.getString("direction"),
                    status = item.getString("status"),
                    assetSymbol = item.getString("assetSymbol"),
                    amountAtomic = item.getLong("amountAtomic"),
                    txHash = item.optNullableString("txHash"),
                    occurredAt = item.optDateTime("occurredAt"),
                    counterpartyLabel = item.optNullableString("counterpartyLabel"),
                    memo = item.optNullableString("memo")
                )
            )
        }
    }
}

private fun List<RemittanceLedgerItemPayload>.toTransferSummaryPayloads(): List<RemittanceTransferSummaryPayload> =
    mapNotNull { item ->
        if (item.entryType != "REMITTANCE_TRANSFER") {
            return@mapNotNull null
        }

        RemittanceTransferSummaryPayload(
            transferId = item.entryId,
            direction = when (item.direction.uppercase()) {
                "INBOUND" -> "INCOME"
                "OUTBOUND" -> "EXPENSE"
                else -> item.direction
            },
            status = item.status,
            assetSymbol = item.assetSymbol,
            amountAtomic = item.amountAtomic,
            senderAddress = "",
            senderName = if (item.direction.equals("INBOUND", ignoreCase = true)) {
                item.counterpartyLabel
            } else {
                null
            },
            recipientId = item.entryId,
            recipientAlias = if (item.direction.equals("OUTBOUND", ignoreCase = true)) {
                item.counterpartyLabel
            } else {
                null
            },
            recipientAddress = "",
            txHash = item.txHash,
            networkFeeWei = null,
            networkFeeAssetSymbol = null,
            updatedAt = item.occurredAt
        )
    }

internal fun JSONObject.optNullableString(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return normalizeApiString(optString(key))
}

internal fun JSONObject.optDateTime(key: String): LocalDateTime? =
    parseOptionalDateTime(optNullableString(key))

internal fun normalizeApiString(value: String?): String? =
    value
        ?.trim()
        ?.takeUnless { it.isEmpty() || it.equals("null", ignoreCase = true) }

internal fun parseOptionalDateTime(value: String?): LocalDateTime? =
    normalizeApiString(value)?.let(LocalDateTime::parse)

class RemittanceUnauthorizedException(
    message: String = SESSION_EXPIRED_MESSAGE
) : IOException(message)

class RemittanceApiException(
    val code: String? = null,
    message: String
) : IOException(message)
