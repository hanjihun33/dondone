package com.workproofpay.backend.remittance;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.remittance.adapter.ChainBalanceSnapshot;
import com.workproofpay.backend.remittance.adapter.RemittanceBlockchainGateway;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.UserWallet;
import com.workproofpay.backend.remittance.model.WalletFundingStatus;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.remittance.service.RemittanceMetrics;
import com.workproofpay.backend.remittance.service.WalletCryptoService;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.math.BigInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserWalletRepository userWalletRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletCryptoService walletCryptoService;

    @Mock
    private RemittanceBlockchainGateway blockchainGateway;

    @Mock
    private RemittanceMetrics remittanceMetrics;

    private WalletService walletService;
    private Map<Long, UserWallet> walletStore;

    @BeforeEach
    void setUp() {
        walletStore = new HashMap<>();

        RemittanceProperties properties = new RemittanceProperties();
        walletService = new WalletService(
                userWalletRepository,
                userRepository,
                walletCryptoService,
                blockchainGateway,
                properties,
                remittanceMetrics
        );

        lenient().when(userRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.of(User.register("wallet@test.com", "hashed", "Wallet User")));
        lenient().when(walletCryptoService.encrypt(any())).thenReturn("encrypted-private-key");
        when(userWalletRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(walletStore.get(invocation.getArgument(0))));
        lenient().when(blockchainGateway.getBalances(any()))
                .thenReturn(new ChainBalanceSnapshot(BigInteger.ZERO, BigInteger.ZERO));
        lenient().when(userWalletRepository.saveAndFlush(any(UserWallet.class)))
                .thenAnswer(invocation -> persist(invocation.getArgument(0), true));
        lenient().when(userWalletRepository.save(any(UserWallet.class)))
                .thenAnswer(invocation -> persist(invocation.getArgument(0), false));
    }

    @Test
    void retriesFundingForExistingFailedWalletInsteadOfReturningHalfReadyWallet() {
        long userId = 1L;
        doThrow(new IllegalStateException("rpc timeout"))
                .doNothing()
                .when(blockchainGateway)
                .fundWallet(anyString(), any(BigInteger.class), any(BigInteger.class));

        assertThatThrownBy(() -> walletService.createWalletIfAbsent(userId))
                .isInstanceOf(ApiException.class)
                .satisfies(throwable -> assertThat(((ApiException) throwable).getErrorCode())
                        .isEqualTo(ErrorCode.WALLET_FUNDING_FAILED));

        UserWallet failedWallet = walletStore.get(userId);
        assertThat(failedWallet).isNotNull();
        assertThat(failedWallet.getFundingStatus()).isEqualTo(WalletFundingStatus.FAILED);
        assertThat(failedWallet.getFundingFailureReason()).contains("rpc timeout");
        String walletAddress = failedWallet.getWalletAddress();

        WalletService.WalletCreateResult retried = walletService.createWalletIfAbsent(userId);

        assertThat(retried.created()).isFalse();
        assertThat(retried.response().walletAddress()).isEqualTo(walletAddress);
        assertThat(retried.response().fundingStatus()).isEqualTo(WalletFundingStatus.FUNDED);
        assertThat(retried.response().fundingFailureReason()).isNull();
        assertThat(retried.response().fundedAt()).isNotNull();
        assertThat(walletStore.get(userId).getWalletAddress()).isEqualTo(walletAddress);
        assertThat(walletStore.get(userId).getFundingStatus()).isEqualTo(WalletFundingStatus.FUNDED);
    }

    @Test
    void doesNotRefundWalletThatIsAlreadyPendingFunding() {
        long userId = 2L;
        UserWallet pendingWallet = UserWallet.create(
                userId,
                "0x1234567890123456789012345678901234567890",
                "encrypted-private-key"
        );
        pendingWallet.onCreate();
        walletStore.put(userId, pendingWallet);

        WalletService.WalletCreateResult result = walletService.createWalletIfAbsent(userId);

        assertThat(result.created()).isFalse();
        assertThat(result.response().fundingStatus()).isEqualTo(WalletFundingStatus.PENDING);
        verify(blockchainGateway, never()).fundWallet(anyString(), any(BigInteger.class), any(BigInteger.class));
    }

    @Test
    void marksPendingWalletFundedWhenSeedBalancesAlreadyExist() {
        long userId = 3L;
        UserWallet pendingWallet = UserWallet.create(
                userId,
                "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd",
                "encrypted-private-key"
        );
        pendingWallet.onCreate();
        walletStore.put(userId, pendingWallet);
        when(blockchainGateway.getBalances(pendingWallet.getWalletAddress()))
                .thenReturn(new ChainBalanceSnapshot(
                        BigInteger.valueOf(500_000_000L),
                        new BigInteger("10000000000000000")
                ));

        WalletService.WalletCreateResult result = walletService.createWalletIfAbsent(userId);

        assertThat(result.created()).isFalse();
        assertThat(result.response().fundingStatus()).isEqualTo(WalletFundingStatus.FUNDED);
        verify(blockchainGateway, never()).fundWallet(anyString(), any(BigInteger.class), any(BigInteger.class));
    }

    @Test
    void recoversFailedWalletByFundingOnlyMissingAssets() {
        long userId = 4L;
        UserWallet failedWallet = UserWallet.create(
                userId,
                "0x4444444444444444444444444444444444444444",
                "encrypted-private-key"
        );
        failedWallet.onCreate();
        failedWallet.markFundingFailed("partial funding");
        walletStore.put(userId, failedWallet);

        when(blockchainGateway.getBalances(failedWallet.getWalletAddress()))
                .thenReturn(new ChainBalanceSnapshot(
                        BigInteger.ZERO,
                        new BigInteger("10000000000000000")
                ));

        walletService.recoverWalletFunding(userId);

        verify(blockchainGateway).fundWallet(
                eq(failedWallet.getWalletAddress()),
                eq(BigInteger.valueOf(500_000_000L)),
                eq(BigInteger.ZERO)
        );
    }

    private UserWallet persist(UserWallet wallet, boolean isCreate) {
        if (isCreate && wallet.getCreatedAt() == null) {
            wallet.onCreate();
        } else {
            wallet.onUpdate();
        }
        walletStore.put(wallet.getUserId(), wallet);
        return wallet;
    }
}
