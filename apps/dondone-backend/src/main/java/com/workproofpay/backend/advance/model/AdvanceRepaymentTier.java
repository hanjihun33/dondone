package com.workproofpay.backend.advance.model;

import java.math.BigDecimal;

public enum AdvanceRepaymentTier {
    D("D", 0, BigDecimal.ZERO, 0L, 5),
    C("C", 5, BigDecimal.valueOf(0.10), 50_000L, 10),
    B("B", 10, BigDecimal.valueOf(0.20), 150_000L, 20),
    A("A", 20, BigDecimal.valueOf(0.30), 300_000L, null);

    private final String code;
    private final int minimumDays;
    private final BigDecimal ratio;
    private final long capAmount;
    private final Integer nextMinimumDays;

    AdvanceRepaymentTier(String code, int minimumDays, BigDecimal ratio, long capAmount, Integer nextMinimumDays) {
        this.code = code;
        this.minimumDays = minimumDays;
        this.ratio = ratio;
        this.capAmount = capAmount;
        this.nextMinimumDays = nextMinimumDays;
    }

    public String code() {
        return code;
    }

    public BigDecimal ratio() {
        return ratio;
    }

    public long capAmount() {
        return capAmount;
    }

    public Integer nextMinimumDays() {
        return nextMinimumDays;
    }

    public static AdvanceRepaymentTier fromReflectedDays(int reflectedDays) {
        if (reflectedDays >= A.minimumDays) {
            return A;
        }
        if (reflectedDays >= B.minimumDays) {
            return B;
        }
        if (reflectedDays >= C.minimumDays) {
            return C;
        }
        return D;
    }
}
