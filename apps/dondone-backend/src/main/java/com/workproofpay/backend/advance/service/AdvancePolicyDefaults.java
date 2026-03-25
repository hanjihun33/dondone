package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.model.AdvanceFeeType;
import com.workproofpay.backend.advance.model.AdvancePolicy;
import com.workproofpay.backend.advance.model.AdvanceSettlementMode;

import java.math.BigDecimal;

public final class AdvancePolicyDefaults {

    public static final int DEFAULT_PAYDAY_DAY = 25;
    public static final boolean SAME_DAY_ADVANCE_ALLOWED = false;
    public static final int REDUCED_CAP_DAYS_BEFORE_PAYDAY = 7;
    public static final String ASSET_SYMBOL = "dUSDC";
    public static final int ASSET_DECIMALS = 6;
    public static final BigDecimal REFERENCE_KRW_PER_ASSET = BigDecimal.valueOf(1_450L);
    public static final long MAX_CAP_DISPLAY_KRW_AMOUNT = 500_000L;
    public static final long NEAR_PAYDAY_MAX_CAP_DISPLAY_KRW_AMOUNT = 50_000L;
    public static final long FLAT_FEE_DISPLAY_KRW_AMOUNT = 5_000L;
    public static final String DISCLAIMER = "미리받기 금액은 반영된 근무 기록 기준의 데모 시뮬레이션입니다. 실제 금융 서비스 제공을 의미하지 않습니다.";

    private AdvancePolicyDefaults() {
    }

    public static AdvancePolicy createDefault() {
        return AdvancePolicy.global(
                true,
                DEFAULT_PAYDAY_DAY,
                SAME_DAY_ADVANCE_ALLOWED,
                REDUCED_CAP_DAYS_BEFORE_PAYDAY,
                ASSET_SYMBOL,
                ASSET_DECIMALS,
                REFERENCE_KRW_PER_ASSET,
                MAX_CAP_DISPLAY_KRW_AMOUNT,
                NEAR_PAYDAY_MAX_CAP_DISPLAY_KRW_AMOUNT,
                AdvanceFeeType.FLAT,
                FLAT_FEE_DISPLAY_KRW_AMOUNT,
                AdvanceSettlementMode.PAYDAY_AUTO_OFFSET,
                false,
                DISCLAIMER
        );
    }
}
