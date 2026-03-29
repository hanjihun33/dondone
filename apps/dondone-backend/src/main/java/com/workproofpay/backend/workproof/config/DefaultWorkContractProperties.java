package com.workproofpay.backend.workproof.config;

import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "workproof.default-contract")
public class DefaultWorkContractProperties {

    private boolean enabled = true;
    private WorkProofPayUnit payUnit = WorkProofPayUnit.HOURLY;
    private BigDecimal basePayAmount = BigDecimal.valueOf(12_000);
    private Integer dailyWorkMinutes = 480;
    private Integer monthlyWorkMinutes = 12_540;
    private Integer paydayDay = 31;
    private int effectiveFromOffsetDays = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public WorkProofPayUnit getPayUnit() {
        return payUnit;
    }

    public void setPayUnit(WorkProofPayUnit payUnit) {
        this.payUnit = payUnit;
    }

    public BigDecimal getBasePayAmount() {
        return basePayAmount;
    }

    public void setBasePayAmount(BigDecimal basePayAmount) {
        this.basePayAmount = basePayAmount;
    }

    public Integer getDailyWorkMinutes() {
        return dailyWorkMinutes;
    }

    public void setDailyWorkMinutes(Integer dailyWorkMinutes) {
        this.dailyWorkMinutes = dailyWorkMinutes;
    }

    public Integer getMonthlyWorkMinutes() {
        return monthlyWorkMinutes;
    }

    public void setMonthlyWorkMinutes(Integer monthlyWorkMinutes) {
        this.monthlyWorkMinutes = monthlyWorkMinutes;
    }

    public Integer getPaydayDay() {
        return paydayDay;
    }

    public void setPaydayDay(Integer paydayDay) {
        this.paydayDay = paydayDay;
    }

    public int getEffectiveFromOffsetDays() {
        return effectiveFromOffsetDays;
    }

    public void setEffectiveFromOffsetDays(int effectiveFromOffsetDays) {
        this.effectiveFromOffsetDays = effectiveFromOffsetDays;
    }
}
