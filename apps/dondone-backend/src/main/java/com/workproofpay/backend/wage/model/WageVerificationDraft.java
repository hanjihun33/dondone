package com.workproofpay.backend.wage.model;

import com.workproofpay.backend.workproof.model.WorkProofPayUnit;

import java.math.BigDecimal;
import java.util.List;

/**
 * WageVerification aggregate를 만들기 전 필요한 계산/계약/evidence snapshot을 한 번에 담는 초안 객체다.
 * service가 upstream 결과를 조합한 뒤 entity에는 이 draft만 넘겨 책임 경계를 유지한다.
 */
public record WageVerificationDraft(
        String month,
        Long workplaceId,
        Long contractId,
        WorkProofPayUnit payUnit,
        BigDecimal basePayAmount,
        Integer dailyWorkMinutes,
        Integer monthlyWorkMinutes,
        BigDecimal normalizedHourlyWage,
        int workDayCount,
        long verifiedWorkMinutes,
        long overtimeMinutes,
        long nightMinutes,
        int modifiedRecordCount,
        int excludedPendingRecordCount,
        long actualDepositAmount,
        boolean deductionsKnown,
        String memo,
        WageVerificationStatus status,
        WageVerificationResolutionStage resolutionStage,
        long baseEstimate,
        long overtimePremium,
        long nightPremium,
        long estimatedTotal,
        long differenceAmount,
        BigDecimal differenceRate,
        long thresholdAbsoluteWon,
        BigDecimal thresholdRelativePercent,
        boolean thresholdDeductionRelaxed,
        List<Long> evidenceRecordIds,
        List<WageVerificationPossibleCauseSnapshot> possibleCauses
) {
}
