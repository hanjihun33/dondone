package com.workproofpay.backend.workproof.service;

import com.workproofpay.backend.workproof.config.DefaultWorkContractProperties;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DefaultWorkContractProvisioner implements WorkContractProvisioner {

    private final WorkContractRepository workContractRepository;
    private final DefaultWorkContractProperties properties;

    @Override
    public WorkContract ensureActiveContract(Workplace workplace, LocalDate effectiveFrom) {
        return workContractRepository.findFirstByWorkplaceIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplace.getId())
                .orElseGet(() -> {
                    if (!properties.isEnabled()) {
                        throw new IllegalStateException("Default work contract provisioning is disabled");
                    }

                    WorkProofPayUnit payUnit = properties.getPayUnit();
                    BigDecimal basePayAmount = properties.getBasePayAmount();
                    Integer dailyWorkMinutes = resolveDailyWorkMinutes(payUnit);
                    Integer monthlyWorkMinutes = resolveMonthlyWorkMinutes(payUnit);
                    BigDecimal normalizedHourlyWage = calculateNormalizedHourlyWage(
                            payUnit,
                            basePayAmount,
                            dailyWorkMinutes,
                            monthlyWorkMinutes
                    );

                    return workContractRepository.save(WorkContract.activate(
                            workplace,
                            payUnit,
                            basePayAmount,
                            dailyWorkMinutes,
                            monthlyWorkMinutes,
                            normalizedHourlyWage,
                            properties.getPaydayDay(),
                            effectiveFrom.plusDays(properties.getEffectiveFromOffsetDays())
                    ));
                });
    }

    private Integer resolveDailyWorkMinutes(WorkProofPayUnit payUnit) {
        if (payUnit != WorkProofPayUnit.DAILY) {
            return null;
        }
        return properties.getDailyWorkMinutes();
    }

    private Integer resolveMonthlyWorkMinutes(WorkProofPayUnit payUnit) {
        if (payUnit != WorkProofPayUnit.MONTHLY) {
            return null;
        }
        return properties.getMonthlyWorkMinutes();
    }

    private BigDecimal calculateNormalizedHourlyWage(WorkProofPayUnit payUnit,
                                                     BigDecimal basePayAmount,
                                                     Integer dailyWorkMinutes,
                                                     Integer monthlyWorkMinutes) {
        return switch (payUnit) {
            case HOURLY -> basePayAmount;
            case DAILY -> basePayAmount
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(dailyWorkMinutes), 2, RoundingMode.HALF_UP);
            case MONTHLY -> basePayAmount
                    .multiply(BigDecimal.valueOf(60))
                    .divide(BigDecimal.valueOf(monthlyWorkMinutes), 2, RoundingMode.HALF_UP);
        };
    }
}
