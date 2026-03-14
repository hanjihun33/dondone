package com.dondone.mobile.data.demo

import com.dondone.mobile.domain.model.Account
import com.dondone.mobile.domain.model.AdvanceData
import com.dondone.mobile.domain.model.DemoInfo
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.DocumentItem
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.RemittanceData
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.VaultData
import com.dondone.mobile.domain.model.WageData
import com.dondone.mobile.domain.model.WorkAudit
import com.dondone.mobile.domain.model.WorkRecord
import com.dondone.mobile.domain.model.WorkproofData

object DemoSeedFactory {
    fun create(): DemoState {
        return DemoState(
            demo = DemoInfo(year = 2026, month = 3, monthLength = 31, asOfDay = 28),
            workproof = WorkproofData(
                workplaceName = "Green Farm",
                workplaceAddress = "경북 구미시",
                today = TodayWork(clockIn = null, clockOut = null),
                records = listOf(
                    WorkRecord("WP-0327-01", 27, "09:01", "18:03", false, 0),
                    WorkRecord("WP-0326-01", 26, "09:05", "18:15", false, 0),
                    WorkRecord("WP-0318-01", 18, "09:10", "18:10", true, 1)
                ),
                audit = listOf(
                    WorkAudit(
                        id = "WP-0318-01",
                        before = "08:57-18:10",
                        after = "09:10-18:10",
                        reason = "출근/퇴근 탭을 늦게 눌렀어요",
                        attachments = 1,
                        at = "2026-03-18 20:11"
                    )
                )
            ),
            wage = WageData(
                workDays = 18,
                totalHours = 142,
                overtimeHours = 12,
                nightHours = 6,
                hourly = 12_000,
                deductionsKnown = false,
                paydayDay = 27,
                actualDepositRecordedDay = 28,
                actualDeposit = 1_740_000
            ),
            remittance = RemittanceData(
                accounts = listOf(
                    Account("A-001", "주 계좌", "****-3124", 1_740_000),
                    Account("A-002", "생활비 계좌", "****-8271", 620_000)
                ),
                selectedAccountId = "A-001",
                recipients = listOf(
                    Recipient("R-001", "Minh Family", "가족", "0x2Aa3...17F9"),
                    Recipient("R-002", "Anh Brother", "형제", "0x5bC1...92De")
                ),
                selectedRecipientId = "R-001",
                draftAmountUsd = 360,
                txHash = "0x9f2e3d8b1c0a4e7f6d5c4b3a20e1f0d9c8b7a6f5e4d3c2b1a0",
                status = TransferStatus.IDLE,
                flowStep = TransferFlowStep.RECIPIENT,
                destinationMode = TransferDestinationMode.ACCOUNT,
                stepReturnTarget = null
            ),
            advance = AdvanceData(
                maxCap = 500_000,
                limitRate = 0.2,
                used = 120_000,
                flatFee = 1_500,
                selectedRequest = 100_000,
                previousRepaymentGood = true,
                bonusLimit = 100_000
            ),
            vault = VaultData(
                enabled = false,
                userDeposit = 0,
                apr = 0.043,
                accruedInterest = 0,
                totalPool = 42_000_000,
                advanceRatio = 0.3,
                advanceUtilization = 0.42,
                monthlyFeeRevenue = 285_000
            ),
            documents = listOf(
                DocumentItem("DOC-PROOF-2026-03", "증빙 리포트", "READY", "2026-03-28 14:21"),
                DocumentItem("DOC-CLAIM-2026-03", "근거 자료 묶음", "NOT_CREATED", null),
                DocumentItem("DOC-RECEIPT-0007", "송금 영수증", "READY", "2026-03-19 09:12")
            )
        )
    }
}
