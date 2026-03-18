package com.workproofpay.remittance.service;

import com.workproofpay.remittance.domain.UserWalletEntity;
import com.workproofpay.remittance.dto.UserWalletResponse;
import com.workproofpay.remittance.repo.AppUserRepository;
import com.workproofpay.remittance.repo.UserWalletRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.time.Instant;

@Service
public class UserWalletService {
    private final AppUserRepository appUserRepository;
    private final UserWalletRepository userWalletRepository;
    private final WalletCryptoService walletCryptoService;
    private final ApplicationEventPublisher applicationEventPublisher;

    public UserWalletService(
            AppUserRepository appUserRepository,
            UserWalletRepository userWalletRepository,
            WalletCryptoService walletCryptoService,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.appUserRepository = appUserRepository;
        this.userWalletRepository = userWalletRepository;
        this.walletCryptoService = walletCryptoService;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public UserWalletResponse createWalletIfAbsent(Long userId) {
        requireExistingUser(userId);
        UserWalletEntity existing = userWalletRepository.findById(userId).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        try {
            ECKeyPair keyPair = Keys.createEcKeyPair();
            String privateKey = toHexPrivateKey(keyPair.getPrivateKey());
            Credentials credentials = Credentials.create(privateKey);

            UserWalletEntity wallet = new UserWalletEntity();
            wallet.setUserId(userId);
            wallet.setWalletAddress(credentials.getAddress());
            wallet.setEncryptedPrivateKey(walletCryptoService.encrypt(privateKey));
            wallet.setCreatedAt(Instant.now());

            userWalletRepository.save(wallet);
            applicationEventPublisher.publishEvent(new WalletCreatedEvent(wallet.getUserId(), wallet.getWalletAddress()));
            return toResponse(wallet);
        } catch (Exception e) {
            throw new IllegalStateException("wallet generation failed", e);
        }
    }

    @Transactional(readOnly = true)
    public UserWalletResponse getWallet(Long userId) {
        UserWalletEntity wallet = getRequiredEntity(userId);
        return toResponse(wallet);
    }

    @Transactional(readOnly = true)
    public UserWalletEntity getRequiredEntity(Long userId) {
        return userWalletRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "WALLET_NOT_FOUND", "Wallet not found", null));
    }

    @Transactional(readOnly = true)
    public String getDecryptedPrivateKey(Long userId) {
        UserWalletEntity wallet = getRequiredEntity(userId);
        return walletCryptoService.decrypt(wallet.getEncryptedPrivateKey());
    }

    private void requireExistingUser(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw new ApiException(404, "USER_NOT_FOUND", "User not found in test.users", null);
        }
    }

    private UserWalletResponse toResponse(UserWalletEntity wallet) {
        return new UserWalletResponse(
                wallet.getUserId(),
                wallet.getWalletAddress(),
                wallet.getCreatedAt()
        );
    }

    private String toHexPrivateKey(BigInteger privateKey) {
        return String.format("%064x", privateKey);
    }
}
