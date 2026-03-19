package com.dondone.mobile.data.wage

import java.time.LocalDate
import java.time.YearMonth

data class WagePossibleCausePayload(
    val code: String,
    val title: String,
    val detail: String
)

data class WageThresholdPayload(
    val absoluteWon: Long,
    val relativePercent: String,
    val deductionRelaxed: Boolean
)

data class WageEvidencePayload(
    val overtimeMinutes: Long,
    val nightMinutes: Long,
    val modifiedRecordCount: Int,
    val recordIds: List<Long>
)

data class WageEmployerSupportPayload(
    val available: Boolean,
    val recommended: Boolean,
    val status: String
)

data class WageRelatedActionsPayload(
    val proofPackReady: Boolean,
    val claimKitReady: Boolean,
    val instantClaimAvailable: Boolean,
    val proofPackDocumentId: Long?,
    val claimKitDocumentId: Long?,
    val preparationId: Long?
)

data class WageMonthlySummaryPayload(
    val month: YearMonth,
    val workplaceId: Long,
    val contractId: Long,
    val payUnit: String,
    val normalizedHourlyWage: Long,
    val workDayCount: Int,
    val verifiedWorkMinutes: Long,
    val overtimeMinutes: Long,
    val nightMinutes: Long,
    val modifiedRecordCount: Int,
    val includedRecordIds: List<Long>,
    val excludedPendingRecordCount: Int
)

data class WageEstimatePayload(
    val month: YearMonth,
    val workplaceId: Long,
    val ruleVersion: String,
    val disclaimer: String,
    val baseEstimate: Long,
    val overtimePremium: Long,
    val nightPremium: Long,
    val estimatedTotal: Long
)

data class WageDifferenceReasonPayload(
    val code: String,
    val title: String,
    val description: String,
    val relatedWorkProofIds: List<Long>
)

data class WageLegacySummaryPayload(
    val yearMonth: YearMonth,
    val asOf: LocalDate?,
    val workDays: Int,
    val totalWorkedMinutes: Long,
    val overtimeMinutes: Long,
    val nightMinutes: Long,
    val normalizedHourlyWage: Long,
    val estimatedBaseAmount: Long,
    val estimatedOvertimePremiumAmount: Long,
    val estimatedNightPremiumAmount: Long,
    val estimatedTotalAmount: Long,
    val actualDepositAmount: Long?,
    val actualDepositRecordedDate: LocalDate?,
    val actualDepositRecordedDay: Int?,
    val deductionsKnown: Boolean,
    val paydayDay: Int,
    val differenceAmount: Long?,
    val anomalyTriggerAmount: Long,
    val anomalyDetected: Boolean,
    val status: String,
    val disclaimer: String,
    val modifiedRecordCount: Int,
    val reflectedRecordCount: Int,
    val pendingRecordCount: Int,
    val relatedWorkProofIds: List<Long>,
    val reasons: List<WageDifferenceReasonPayload>
)

data class WageDepositRecordPayload(
    val id: Long,
    val yearMonth: YearMonth,
    val depositDate: LocalDate,
    val actualDepositAmount: Long,
    val deductionsKnown: Boolean,
    val note: String?
)

data class WageVerificationCreatedPayload(
    val verificationId: Long,
    val status: String,
    val resolutionStage: String,
    val estimatedTotal: Long,
    val actualDepositAmount: Long,
    val differenceAmount: Long,
    val differenceRate: String,
    val threshold: WageThresholdPayload,
    val possibleCauses: List<WagePossibleCausePayload>,
    val evidence: WageEvidencePayload,
    val nextActions: List<String>
)

data class WageVerificationDetailPayload(
    val verificationId: Long,
    val month: YearMonth,
    val workplaceId: Long,
    val status: String,
    val resolutionStage: String,
    val estimatedBaseAmount: Long,
    val estimatedOvertimePremium: Long,
    val estimatedNightPremium: Long,
    val estimatedTotal: Long,
    val actualDepositAmount: Long,
    val deductionsKnown: Boolean,
    val submittedBy: String,
    val differenceAmount: Long,
    val differenceRate: String,
    val threshold: WageThresholdPayload,
    val possibleCauses: List<WagePossibleCausePayload>,
    val evidence: WageEvidencePayload,
    val employerSupport: WageEmployerSupportPayload,
    val relatedActions: WageRelatedActionsPayload
)

data class WageRemotePayload(
    val workplaceName: String,
    val month: YearMonth,
    val workplaceId: Long,
    val monthlySummary: WageMonthlySummaryPayload,
    val estimate: WageEstimatePayload,
    val summary: WageLegacySummaryPayload,
    val latestVerification: WageVerificationDetailPayload? = null
)
