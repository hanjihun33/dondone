package com.workproofpay.backend.advance.service;

import com.workproofpay.backend.advance.model.AdvancePayout;
import com.workproofpay.backend.advance.model.AdvanceRequest;
import com.workproofpay.backend.advance.repo.AdvancePayoutRepository;
import com.workproofpay.backend.jobs.model.JobReferenceKind;
import com.workproofpay.backend.jobs.model.JobType;
import com.workproofpay.backend.jobs.service.JobService;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvancePayoutService {

    private final AdvancePayoutRepository advancePayoutRepository;
    private final WalletService walletService;
    private final JobService jobService;

    @Transactional
    public AdvancePayout createRequestedPayout(AdvanceRequest advanceRequest) {
        UserWallet wallet = walletService.createWalletRecord(advanceRequest.getUser().getId()).wallet();

        AdvancePayout payout = advancePayoutRepository.saveAndFlush(AdvancePayout.request(
                generateAdvancePayoutId(),
                advanceRequest.getId(),
                advanceRequest.getUser().getId(),
                wallet.getWalletAddress(),
                advanceRequest.getApprovedAmountAtomic(),
                advanceRequest.getAssetSymbol(),
                buildIdempotencyKey(advanceRequest)
        ));

        jobService.enqueue(
                JobReferenceKind.ADVANCE_PAYOUT,
                JobType.SUBMIT_ADVANCE_PAYOUT,
                payout.getAdvancePayoutId(),
                LocalDateTime.now()
        );
        return payout;
    }

    private String generateAdvancePayoutId() {
        return "ap_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String buildIdempotencyKey(AdvanceRequest advanceRequest) {
        return "advance-payout:" + advanceRequest.getId();
    }
}
