package com.dondone.mobile.data.demo

import com.dondone.mobile.domain.model.Account
import com.dondone.mobile.domain.model.AdvanceData
import com.dondone.mobile.domain.model.DemoInfo
import com.dondone.mobile.domain.model.DemoState
import com.dondone.mobile.domain.model.DocumentItem
import com.dondone.mobile.domain.model.DEFAULT_CONTRACT_PAYDAY_DAY
import com.dondone.mobile.domain.model.Recipient
import com.dondone.mobile.domain.model.RemittanceData
import com.dondone.mobile.domain.model.TodayWork
import com.dondone.mobile.domain.model.TransactionCategory
import com.dondone.mobile.domain.model.TransactionDirection
import com.dondone.mobile.domain.model.TransactionRecord
import com.dondone.mobile.domain.model.TransferDestinationMode
import com.dondone.mobile.domain.model.TransferFlowStep
import com.dondone.mobile.domain.model.TransferStatus
import com.dondone.mobile.domain.model.VaultData
import com.dondone.mobile.domain.model.WageData
import com.dondone.mobile.domain.model.WorkAudit
import com.dondone.mobile.domain.model.WorkRecord
import com.dondone.mobile.domain.model.WorkproofData
import java.time.LocalDate
import java.time.LocalDateTime

private const val DEMO_WORKPLACE_NAME = "Green Farm"
private const val DEMO_WORKPLACE_ADDRESS = "경상북도 구미시 농장로 18"
private const val DEMO_WORKPLACE_LATITUDE = 36.1195
private const val DEMO_WORKPLACE_LONGITUDE = 128.3446
private const val DEMO_CURRENT_LATITUDE = 36.1211
private const val DEMO_CURRENT_LONGITUDE = 128.3418
private const val DEMO_ALLOWED_RADIUS_METERS = 1000

object DemoSeedFactory {
    fun create(): DemoState {
        return DemoState(
            demo = DemoInfo(year = 2026, month = 3, monthLength = 31, asOfDay = 28),
            workproof = WorkproofData(
                workplaceName = DEMO_WORKPLACE_NAME,
                workplaceAddress = DEMO_WORKPLACE_ADDRESS,
                workplaceLatitude = DEMO_WORKPLACE_LATITUDE,
                workplaceLongitude = DEMO_WORKPLACE_LONGITUDE,
                currentLatitude = DEMO_CURRENT_LATITUDE,
                currentLongitude = DEMO_CURRENT_LONGITUDE,
                today = TodayWork(clockIn = null, clockOut = null),
                records = listOf(
                    WorkRecord("WP-0327-01", LocalDate.of(2026, 3, 27), 27, "09:01", "18:03", false, 0),
                    WorkRecord("WP-0326-01", LocalDate.of(2026, 3, 26), 26, "09:05", "18:15", false, 0),
                    WorkRecord("WP-0318-01", LocalDate.of(2026, 3, 18), 18, "09:10", "18:10", true, 1)
                ),
                audit = listOf(
                    WorkAudit(
                        id = "WP-0318-01",
                        before = "08:57-18:10",
                        after = "09:10-18:10",
                        reason = "출근 기록 수정을 위해 증빙을 첨부했습니다.",
                        attachments = 1,
                        at = "2026-03-18 20:11"
                    )
                ),
                workplaceId = null,
                allowedRadiusMeters = DEMO_ALLOWED_RADIUS_METERS
            ),
            wage = WageData(
                workDays = 18,
                totalHours = 142,
                overtimeHours = 12,
                nightHours = 6,
                hourly = 12_000,
                deductionsKnown = false,
                paydayDay = DEFAULT_CONTRACT_PAYDAY_DAY,
                actualDepositRecordedDay = 28,
                actualDeposit = 1_740_000
            ),
            remittance = RemittanceData(
                accounts = listOf(
                    Account("A-001", "생활비 계좌", "****-3124", 1_740_000),
                    Account("A-002", "예비 지갑", "****-8271", 620_000)
                ),
                selectedAccountId = "A-001",
                recipients = listOf(
                    Recipient("R-001", "Minh Family", "가족", "0x2Aa3...17F9"),
                    Recipient("R-002", "Anh Brother", "형제", "0x5bC1...92De")
                ),
                transactions = listOf(
                    TransactionRecord(
                        id = "TX-A001-0321-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 21, 9, 12),
                        amount = 1_740_000,
                        direction = TransactionDirection.INCOME,
                        counterpartyName = "DonDone Payroll",
                        category = TransactionCategory.SALARY,
                        memo = "3월 급여 입금",
                        methodLabel = "급여 이체"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0321-2",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 21, 12, 28),
                        amount = 8_500,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "샐러드랩",
                        category = TransactionCategory.FOOD,
                        memo = "점심",
                        methodLabel = "계좌 이체"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0320-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 20, 18, 42),
                        amount = 4_800,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "카페온",
                        category = TransactionCategory.CAFE,
                        memo = "퇴근 후 커피",
                        methodLabel = "카드 결제"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0319-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 19, 21, 8),
                        amount = 75_000,
                        direction = TransactionDirection.INCOME,
                        counterpartyName = "엄마",
                        category = TransactionCategory.TRANSFER,
                        memo = "생활비 보조",
                        methodLabel = "계좌 이체"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0318-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 18, 20, 10),
                        amount = 32_000,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "한길마트",
                        category = TransactionCategory.LIVING,
                        memo = "생필품",
                        methodLabel = "카드 결제"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0308-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 3, 8, 11, 3),
                        amount = 55_000,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "모아스토어",
                        category = TransactionCategory.SHOPPING,
                        memo = "작업복 추가 구매",
                        methodLabel = "간편 결제"
                    ),
                    TransactionRecord(
                        id = "TX-A001-0228-1",
                        walletId = "A-001",
                        occurredAt = LocalDateTime.of(2026, 2, 28, 15, 44),
                        amount = 28_000,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "한빛교통",
                        category = TransactionCategory.TRANSPORT,
                        memo = "주간 교통비",
                        methodLabel = "교통 충전"
                    ),
                    TransactionRecord(
                        id = "TX-A002-0322-1",
                        walletId = "A-002",
                        occurredAt = LocalDateTime.of(2026, 3, 22, 8, 5),
                        amount = 120_000,
                        direction = TransactionDirection.INCOME,
                        counterpartyName = "엄마",
                        category = TransactionCategory.TRANSFER,
                        memo = "생활비",
                        methodLabel = "계좌 이체"
                    ),
                    TransactionRecord(
                        id = "TX-A002-0320-1",
                        walletId = "A-002",
                        occurredAt = LocalDateTime.of(2026, 3, 20, 7, 32),
                        amount = 3_200,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "동네버스",
                        category = TransactionCategory.TRANSPORT,
                        memo = "출근",
                        methodLabel = "교통 결제"
                    ),
                    TransactionRecord(
                        id = "TX-A002-0319-1",
                        walletId = "A-002",
                        occurredAt = LocalDateTime.of(2026, 3, 19, 19, 5),
                        amount = 18_900,
                        direction = TransactionDirection.EXPENSE,
                        counterpartyName = "편의점",
                        category = TransactionCategory.FOOD,
                        memo = "간식",
                        methodLabel = "카드 결제"
                    )
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
                DocumentItem("DOC-PROOF-2026-03", "근로 증빙 묶음", "READY", null),
                DocumentItem("DOC-CLAIM-2026-03", "체불 대응 서류", "NOT_CREATED", null),
                DocumentItem("DOC-RECEIPT-0007", "송금 영수증", "READY", null)
            )
        )
    }
}
