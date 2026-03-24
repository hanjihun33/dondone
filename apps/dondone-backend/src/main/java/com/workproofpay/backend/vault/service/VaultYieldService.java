package com.workproofpay.backend.vault.service;

import com.workproofpay.backend.vault.api.dto.response.VaultInterestPreviewResponse;
import com.workproofpay.backend.vault.model.VaultPosition;
import com.workproofpay.backend.vault.model.VaultYieldLog;
import com.workproofpay.backend.vault.repo.VaultYieldLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VaultYieldService {

    private static final long SECONDS_PER_DAY = 86_400L;
    private static final long SECONDS_PER_YEAR = 365L * SECONDS_PER_DAY;

    private final VaultYieldLogRepository vaultYieldLogRepository;

    @Transactional
    public void accrueIfNeeded(VaultPosition position) {
        LocalDateTime fromAt = position.getLastAccruedAt();
        LocalDateTime toAt = LocalDateTime.now();
        if (fromAt == null) {
            position.accrue(BigInteger.ZERO, toAt);
            return;
        }
        if (!toAt.isAfter(fromAt) || position.getPrincipalAmountAtomic().signum() <= 0) {
            position.accrue(BigInteger.ZERO, toAt);
            return;
        }

        BigInteger yieldAmountAtomic = estimate(position.getPrincipalAmountAtomic(), position.getApyBps(), Duration.between(fromAt, toAt).getSeconds());
        position.accrue(yieldAmountAtomic, toAt);
        if (yieldAmountAtomic.signum() > 0) {
            vaultYieldLogRepository.save(VaultYieldLog.create(
                    position.getId(),
                    position.getUserId(),
                    fromAt,
                    toAt,
                    position.getApyBps(),
                    position.getPrincipalAmountAtomic(),
                    yieldAmountAtomic,
                    "SECONDLY_SIMPLE"
            ));
        }
    }

    @Transactional(readOnly = true)
    public VaultInterestPreviewResponse preview(VaultPosition position) {
        BigInteger principal = position == null ? BigInteger.ZERO : position.getPrincipalAmountAtomic();
        int apyBps = position == null ? 500 : position.getApyBps();
        return new VaultInterestPreviewResponse(
                estimate(principal, apyBps, SECONDS_PER_DAY).toString(),
                estimate(principal, apyBps, 30L * SECONDS_PER_DAY).toString(),
                estimate(principal, apyBps, SECONDS_PER_YEAR).toString(),
                apyBps
        );
    }

    private BigInteger estimate(BigInteger principalAmountAtomic, int apyBps, long seconds) {
        if (principalAmountAtomic.signum() <= 0 || apyBps <= 0 || seconds <= 0) {
            return BigInteger.ZERO;
        }
        return principalAmountAtomic
                .multiply(BigInteger.valueOf(apyBps))
                .multiply(BigInteger.valueOf(seconds))
                .divide(BigInteger.valueOf(10_000L))
                .divide(BigInteger.valueOf(SECONDS_PER_YEAR));
    }
}
