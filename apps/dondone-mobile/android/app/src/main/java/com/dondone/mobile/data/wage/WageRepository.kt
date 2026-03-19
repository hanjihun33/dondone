package com.dondone.mobile.data.wage

import java.time.LocalDate
import java.time.YearMonth

interface WageRepository {
    suspend fun load(
        accessToken: String,
        month: YearMonth,
        asOf: LocalDate,
        paydayDay: Int
    ): WageRemoteState

    suspend fun createDeposit(
        accessToken: String,
        month: YearMonth,
        depositDate: LocalDate,
        actualDepositAmount: Long,
        deductionsKnown: Boolean,
        note: String? = null
    ): WageDepositRecordPayload

    suspend fun createVerification(
        accessToken: String,
        month: YearMonth,
        workplaceId: Long,
        actualDepositAmount: Long,
        deductionsKnown: Boolean,
        memo: String? = null
    ): WageVerificationCreatedPayload

    suspend fun getVerificationDetail(
        accessToken: String,
        verificationId: Long
    ): WageVerificationDetailPayload
}
