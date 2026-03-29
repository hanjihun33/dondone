package com.workproofpay.backend.workproof.model;

import com.workproofpay.backend.shared.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_contracts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
/**
 * 근무지별 활성 급여 계약을 표현하며 WorkProof/Wage 계산의 공통 기준이 된다.
 */
public class WorkContract extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workplace_id", nullable = false)
    private Workplace workplace;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_unit", nullable = false, length = 20)
    private WorkProofPayUnit payUnit;

    @Column(name = "base_pay_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal basePayAmount;

    @Column(name = "daily_work_minutes")
    private Integer dailyWorkMinutes;

    @Column(name = "monthly_work_minutes")
    private Integer monthlyWorkMinutes;

    @Column(name = "normalized_hourly_wage", nullable = false, precision = 19, scale = 2)
    private BigDecimal normalizedHourlyWage;

    @Column(name = "payday_day", nullable = false)
    private Integer paydayDay;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    private WorkContract(Workplace workplace,
                         WorkProofPayUnit payUnit,
                         BigDecimal basePayAmount,
                         Integer dailyWorkMinutes,
                         Integer monthlyWorkMinutes,
                         BigDecimal normalizedHourlyWage,
                         Integer paydayDay,
                         LocalDate effectiveFrom,
                         LocalDate effectiveTo) {
        this.workplace = workplace;
        this.payUnit = payUnit;
        this.basePayAmount = basePayAmount;
        this.dailyWorkMinutes = dailyWorkMinutes;
        this.monthlyWorkMinutes = monthlyWorkMinutes;
        this.normalizedHourlyWage = normalizedHourlyWage;
        this.paydayDay = paydayDay;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
    }

    public static WorkContract activate(Workplace workplace,
                                        WorkProofPayUnit payUnit,
                                        BigDecimal basePayAmount,
                                        Integer dailyWorkMinutes,
                                        Integer monthlyWorkMinutes,
                                        BigDecimal normalizedHourlyWage,
                                        Integer paydayDay,
                                        LocalDate effectiveFrom) {
        return new WorkContract(
                workplace,
                payUnit,
                basePayAmount,
                dailyWorkMinutes,
                monthlyWorkMinutes,
                normalizedHourlyWage,
                paydayDay,
                effectiveFrom,
                null
        );
    }

    public static WorkContract activate(Workplace workplace,
                                        WorkProofPayUnit payUnit,
                                        BigDecimal basePayAmount,
                                        Integer dailyWorkMinutes,
                                        Integer monthlyWorkMinutes,
                                        BigDecimal normalizedHourlyWage,
                                        LocalDate effectiveFrom) {
        return activate(
                workplace,
                payUnit,
                basePayAmount,
                dailyWorkMinutes,
                monthlyWorkMinutes,
                normalizedHourlyWage,
                31,
                effectiveFrom
        );
    }

    public boolean isActive() {
        return effectiveTo == null;
    }
}
