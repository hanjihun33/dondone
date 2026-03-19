package com.workproofpay.backend.remittance;

import com.workproofpay.backend.remittance.adapter.ChainBalanceSnapshot;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.model.RecipientRelation;
import com.workproofpay.backend.remittance.model.RemittancePolicyCode;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.service.RecipientService;
import com.workproofpay.backend.remittance.service.RemittancePolicyService;
import com.workproofpay.backend.remittance.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemittancePolicyServiceTest {

    @Mock
    private RecipientService recipientService;

    @Mock
    private WalletService walletService;

    @Mock
    private TransferRepository transferRepository;

    private RemittancePolicyService remittancePolicyService;

    @BeforeEach
    void setUp() {
        remittancePolicyService = new RemittancePolicyService(
                recipientService,
                walletService,
                transferRepository,
                new RemittanceProperties()
        );
    }

    @Test
    void blocksTransferWhenNativeBalanceIsBelowEstimatedGasCost() {
        Recipient recipient = Recipient.create(
                "rcp_1",
                1L,
                "수신자",
                RecipientRelation.FRIEND,
                "0x2222222222222222222222222222222222222222",
                true
        );
        recipient.onCreate();
        UserWallet wallet = UserWallet.create(
                1L,
                "0x1111111111111111111111111111111111111111",
                "encrypted"
        );
        wallet.onCreate();

        when(recipientService.getRequiredRecipient(1L, "rcp_1")).thenReturn(recipient);
        when(walletService.getRequiredWallet(1L)).thenReturn(wallet);
        when(walletService.getBalances(wallet.getWalletAddress())).thenReturn(
                new ChainBalanceSnapshot(
                        BigInteger.valueOf(500_000_000L),
                        BigInteger.valueOf(10L)
                )
        );
        when(walletService.estimateTransferGasCostWei()).thenReturn(BigInteger.valueOf(11L));
        when(transferRepository.existsByUserIdAndStatusIn(eq(1L), anyList())).thenReturn(false);

        Object decision = remittancePolicyService.evaluate(
                1L,
                "rcp_1",
                50_000_000L,
                false,
                true
        );

        assertThat((Boolean) ReflectionTestUtils.invokeMethod(decision, "allowed")).isFalse();
        assertThat((RemittancePolicyCode) ReflectionTestUtils.invokeMethod(decision, "policyCode"))
                .isEqualTo(RemittancePolicyCode.INSUFFICIENT_WALLET_BALANCE);
    }
}
