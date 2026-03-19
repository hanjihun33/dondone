package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.api.dto.request.UpsertRecipientRequest;
import com.workproofpay.backend.remittance.api.dto.response.RecipientItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientListResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.repo.RecipientRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final RemittanceProperties properties;

    @Transactional(readOnly = true)
    public RecipientListResponse getRecipients(Long userId) {
        List<RecipientItemResponse> recipients = recipientRepository.findByUserIdOrderByUpdatedAtDescRecipientIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new RecipientListResponse(recipients);
    }

    @Transactional
    public RecipientItemResponse createRecipient(Long userId, UpsertRecipientRequest request) {
        String normalizedWalletAddress = request.walletAddress().toLowerCase();
        ensureWalletAddressAvailable(userId, normalizedWalletAddress, null);
        Recipient recipient = Recipient.create(
                generateRecipientId(),
                userId,
                request.alias().trim(),
                request.relation(),
                normalizedWalletAddress,
                request.allowed()
        );
        return saveRecipient(recipient);
    }

    @Transactional
    public RecipientItemResponse updateRecipient(Long userId, String recipientId, UpsertRecipientRequest request) {
        String normalizedWalletAddress = request.walletAddress().toLowerCase();
        ensureWalletAddressAvailable(userId, normalizedWalletAddress, recipientId);
        Recipient recipient = getRequiredRecipient(userId, recipientId);
        recipient.update(
                request.alias().trim(),
                request.relation(),
                normalizedWalletAddress,
                request.allowed()
        );
        return saveRecipient(recipient);
    }

    @Transactional(readOnly = true)
    public Recipient getRequiredRecipient(Long userId, String recipientId) {
        return recipientRepository.findByRecipientIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.RECIPIENT_NOT_FOUND));
    }

    public String generateRecipientId() {
        return "rcp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private RecipientItemResponse saveRecipient(Recipient recipient) {
        try {
            return toResponse(recipientRepository.save(recipient));
        } catch (DataIntegrityViolationException e) {
            throw duplicateWalletAddress();
        }
    }

    private void ensureWalletAddressAvailable(Long userId, String walletAddress, String recipientId) {
        boolean exists = recipientId == null
                ? recipientRepository.existsByUserIdAndWalletAddress(userId, walletAddress)
                : recipientRepository.existsByUserIdAndWalletAddressAndRecipientIdNot(userId, walletAddress, recipientId);
        if (exists) {
            throw duplicateWalletAddress();
        }
    }

    private ApiException duplicateWalletAddress() {
        return new ApiException(ErrorCode.RECIPIENT_WALLET_ALREADY_EXISTS);
    }

    private RecipientItemResponse toResponse(Recipient recipient) {
        return new RecipientItemResponse(
                recipient.getRecipientId(),
                recipient.getAlias(),
                recipient.getRelation(),
                recipient.getWalletAddress(),
                recipient.isAllowed(),
                isRecentlyUpdated(recipient),
                recipient.getUpdatedAt()
        );
    }

    private boolean isRecentlyUpdated(Recipient recipient) {
        return recipient.getUpdatedAt().plusSeconds(properties.getPolicy().getRecentRecipientWindowSeconds())
                .isAfter(LocalDateTime.now());
    }
}
