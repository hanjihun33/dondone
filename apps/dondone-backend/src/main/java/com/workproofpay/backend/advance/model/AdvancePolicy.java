package com.workproofpay.backend.advance.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "advance_policies",
        indexes = {
                @Index(name = "idx_advance_policies_enabled", columnList = "enabled, advance_policy_id DESC")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdvancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "advance_policy_id")
    private Long id;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "payday_day", nullable = false)
    private int paydayDay;

    @Column(name = "same_day_advance_allowed", nullable = false)
    private boolean sameDayAdvanceAllowed;

    @Column(name = "reduced_cap_days_before_payday", nullable = false)
    private int reducedCapDaysBeforePayday;

    @Column(name = "asset_symbol", nullable = false, length = 20)
    private String assetSymbol;

    @Column(name = "asset_decimals", nullable = false)
    private int assetDecimals;

    @Column(name = "reference_krw_per_asset", nullable = false, precision = 12, scale = 2)
    private BigDecimal referenceKrwPerAsset;

    @Column(name = "max_cap_display_krw_amount", nullable = false)
    private long maxCapDisplayKrwAmount;

    @Column(name = "near_payday_max_cap_display_krw_amount", nullable = false)
    private long nearPaydayMaxCapDisplayKrwAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private AdvanceFeeType feeType;

    @Column(name = "flat_fee_display_krw_amount", nullable = false)
    private long flatFeeDisplayKrwAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_mode", nullable = false, length = 40)
    private AdvanceSettlementMode settlementMode;

    @Column(name = "manual_repayment_enabled", nullable = false)
    private boolean manualRepaymentEnabled;

    @Column(name = "disclaimer", nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private AdvancePolicy(
            boolean enabled,
            int paydayDay,
            boolean sameDayAdvanceAllowed,
            int reducedCapDaysBeforePayday,
            String assetSymbol,
            int assetDecimals,
            BigDecimal referenceKrwPerAsset,
            long maxCapDisplayKrwAmount,
            long nearPaydayMaxCapDisplayKrwAmount,
            AdvanceFeeType feeType,
            long flatFeeDisplayKrwAmount,
            AdvanceSettlementMode settlementMode,
            boolean manualRepaymentEnabled,
            String disclaimer
    ) {
        this.enabled = enabled;
        this.paydayDay = paydayDay;
        this.sameDayAdvanceAllowed = sameDayAdvanceAllowed;
        this.reducedCapDaysBeforePayday = reducedCapDaysBeforePayday;
        this.assetSymbol = assetSymbol;
        this.assetDecimals = assetDecimals;
        this.referenceKrwPerAsset = referenceKrwPerAsset;
        this.maxCapDisplayKrwAmount = maxCapDisplayKrwAmount;
        this.nearPaydayMaxCapDisplayKrwAmount = nearPaydayMaxCapDisplayKrwAmount;
        this.feeType = feeType;
        this.flatFeeDisplayKrwAmount = flatFeeDisplayKrwAmount;
        this.settlementMode = settlementMode;
        this.manualRepaymentEnabled = manualRepaymentEnabled;
        this.disclaimer = disclaimer;
    }

    public static AdvancePolicy global(
            boolean enabled,
            int paydayDay,
            boolean sameDayAdvanceAllowed,
            int reducedCapDaysBeforePayday,
            String assetSymbol,
            int assetDecimals,
            BigDecimal referenceKrwPerAsset,
            long maxCapDisplayKrwAmount,
            long nearPaydayMaxCapDisplayKrwAmount,
            AdvanceFeeType feeType,
            long flatFeeDisplayKrwAmount,
            AdvanceSettlementMode settlementMode,
            boolean manualRepaymentEnabled,
            String disclaimer
    ) {
        return new AdvancePolicy(
                enabled,
                paydayDay,
                sameDayAdvanceAllowed,
                reducedCapDaysBeforePayday,
                assetSymbol,
                assetDecimals,
                referenceKrwPerAsset,
                maxCapDisplayKrwAmount,
                nearPaydayMaxCapDisplayKrwAmount,
                feeType,
                flatFeeDisplayKrwAmount,
                settlementMode,
                manualRepaymentEnabled,
                disclaimer
        );
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
