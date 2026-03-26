package com.dondone.mobile.feature.finance.presentation

import com.dondone.mobile.data.demo.DemoSeedFactory
import com.dondone.mobile.data.remittance.RemittanceLedgerItemPayload
import com.dondone.mobile.data.remittance.RemittanceRemotePayload
import com.dondone.mobile.data.remittance.RemittanceRemoteState
import com.dondone.mobile.data.remittance.RemittanceTransferSummaryPayload
import com.dondone.mobile.data.remittance.RemittanceWalletBalancePayload
import com.dondone.mobile.data.remittance.RemittanceWalletPayload
import com.dondone.mobile.domain.model.TransactionDirection
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionHistoryUiModelTest {

    @Test
    fun `remote transfer only ledger renders wallet transfer item`() {
        val uiModel = DemoSeedFactory.create().toTransactionHistoryMainUiModel(
            accountId = "remote-wallet",
            remittanceRemoteState = remoteState(
                ledgerItems = listOf(
                    ledgerEntry(
                        entryId = "tx-1",
                        entryType = "REMITTANCE_TRANSFER",
                        direction = "OUTBOUND",
                        amountAtomic = 12_500_000L,
                        occurredAt = "2026-03-25T10:00:00",
                        counterpartyLabel = "찬호"
                    )
                ),
                transfers = listOf(
                    transferSummary(
                        transferId = "tx-1",
                        direction = "EXPENSE",
                        amountAtomic = 12_500_000L,
                        updatedAt = "2026-03-25T10:00:00",
                        recipientAlias = "찬호",
                        recipientAddress = "0x2222222222222222222222222222222222222222"
                    )
                )
            ),
            isAuthenticated = true,
            overrides = emptyMap()
        )

        assertEquals(1, uiModel.items.size)
        assertEquals("지갑 송금", uiModel.items.single().methodLabel)
        assertEquals(TransactionDirection.EXPENSE, uiModel.items.single().direction)
        assertEquals("-12.5 dUSDC", uiModel.items.single().amountText)
        assertEquals("찬호", uiModel.items.single().counterpartyName)
    }

    @Test
    fun `confirmed advance payout only ledger renders inbound advance item`() {
        val uiModel = DemoSeedFactory.create().toTransactionHistoryMainUiModel(
            accountId = "remote-wallet",
            remittanceRemoteState = remoteState(
                ledgerItems = listOf(
                    ledgerEntry(
                        entryId = "payout-1",
                        entryType = "ADVANCE_PAYOUT",
                        direction = "INBOUND",
                        amountAtomic = 34_000_000L,
                        occurredAt = "2026-03-25T11:00:00",
                        counterpartyLabel = "미리받기 지급",
                        memo = "약 30,000원 상당"
                    )
                )
            ),
            isAuthenticated = true,
            overrides = emptyMap()
        )

        assertEquals(1, uiModel.items.size)
        assertEquals("미리받기", uiModel.items.single().methodLabel)
        assertEquals(TransactionDirection.INCOME, uiModel.items.single().direction)
        assertEquals("+34 dUSDC", uiModel.items.single().amountText)
        assertEquals("미리받기 지급", uiModel.items.single().counterpartyName)
        assertEquals("약 30,000원 상당", uiModel.items.single().memo)
    }

    @Test
    fun `combined ledger keeps latest sort and income expense filter compatibility`() {
        val uiModel = DemoSeedFactory.create().toTransactionHistoryMainUiModel(
            accountId = "remote-wallet",
            remittanceRemoteState = remoteState(
                ledgerItems = listOf(
                    ledgerEntry(
                        entryId = "tx-older",
                        entryType = "REMITTANCE_TRANSFER",
                        direction = "OUTBOUND",
                        amountAtomic = 12_500_000L,
                        occurredAt = "2026-03-25T10:00:00",
                        counterpartyLabel = "찬호"
                    ),
                    ledgerEntry(
                        entryId = "payout-newer",
                        entryType = "ADVANCE_PAYOUT",
                        direction = "INBOUND",
                        amountAtomic = 34_000_000L,
                        occurredAt = "2026-03-25T11:00:00",
                        counterpartyLabel = "미리받기 지급"
                    )
                ),
                transfers = listOf(
                    transferSummary(
                        transferId = "tx-older",
                        direction = "EXPENSE",
                        amountAtomic = 12_500_000L,
                        updatedAt = "2026-03-25T10:00:00",
                        recipientAlias = "찬호",
                        recipientAddress = "0x2222222222222222222222222222222222222222"
                    )
                )
            ),
            isAuthenticated = true,
            overrides = emptyMap()
        )

        assertEquals(listOf("payout-newer", "tx-older"), uiModel.items.map { it.id })
        assertEquals(1, uiModel.items.count { it.direction == TransactionDirection.INCOME })
        assertEquals(1, uiModel.items.count { it.direction == TransactionDirection.EXPENSE })
        assertTrue(uiModel.items.any { it.methodLabel == "미리받기" })
        assertTrue(uiModel.items.any { it.methodLabel == "지갑 송금" })
    }

    private fun remoteState(
        ledgerItems: List<RemittanceLedgerItemPayload>,
        transfers: List<RemittanceTransferSummaryPayload> = emptyList()
    ): RemittanceRemoteState {
        return RemittanceRemoteState.content(
            RemittanceRemotePayload(
                wallet = RemittanceWalletPayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    fundingStatus = "FUNDED",
                    fundingFailureReason = null,
                    fundedAt = LocalDateTime.parse("2026-03-25T09:00:00"),
                    createdAt = LocalDateTime.parse("2026-03-25T08:59:00")
                ),
                balance = RemittanceWalletBalancePayload(
                    walletAddress = "0x1111111111111111111111111111111111111111",
                    assetSymbol = "dUSDC",
                    assetDecimals = 6,
                    tokenBalanceAtomic = "85000000",
                    nativeBalanceWei = "10000000000000000"
                ),
                recipients = emptyList(),
                ledgerItems = ledgerItems,
                transfers = transfers,
                activeTransfer = null
            )
        )
    }

    private fun ledgerEntry(
        entryId: String,
        entryType: String,
        direction: String,
        amountAtomic: Long,
        occurredAt: String,
        counterpartyLabel: String,
        memo: String? = null
    ): RemittanceLedgerItemPayload {
        return RemittanceLedgerItemPayload(
            entryId = entryId,
            entryType = entryType,
            direction = direction,
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = amountAtomic,
            txHash = "0xabc$entryId",
            occurredAt = LocalDateTime.parse(occurredAt),
            counterpartyLabel = counterpartyLabel,
            memo = memo
        )
    }

    private fun transferSummary(
        transferId: String,
        direction: String,
        amountAtomic: Long,
        updatedAt: String,
        recipientAlias: String?,
        recipientAddress: String
    ): RemittanceTransferSummaryPayload {
        return RemittanceTransferSummaryPayload(
            transferId = transferId,
            direction = direction,
            status = "CONFIRMED",
            assetSymbol = "dUSDC",
            amountAtomic = amountAtomic,
            senderAddress = "0x1111111111111111111111111111111111111111",
            senderName = "보낸 지갑",
            recipientId = "recipient-$transferId",
            recipientAlias = recipientAlias,
            recipientAddress = recipientAddress,
            txHash = "0xdef$transferId",
            networkFeeWei = "100000000000000",
            networkFeeAssetSymbol = "ETH",
            updatedAt = LocalDateTime.parse(updatedAt)
        )
    }
}
