package com.workproofpay.backend.remittance.service;

import com.workproofpay.backend.remittance.api.dto.request.RecipientSearchRequest;
import com.workproofpay.backend.remittance.api.dto.request.UpsertRecipientRequest;
import com.workproofpay.backend.remittance.api.dto.response.RecipientItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientListResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientSearchItemResponse;
import com.workproofpay.backend.remittance.api.dto.response.RecipientSearchListResponse;
import com.workproofpay.backend.remittance.config.RemittanceProperties;
import com.workproofpay.backend.remittance.model.Recipient;
import com.workproofpay.backend.remittance.repo.RecipientRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.shared.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final UserWalletRepository userWalletRepository;
    private final RemittanceProperties properties;

    @Transactional(readOnly = true)
    public RecipientListResponse getRecipients(Long userId) {
        List<RecipientItemResponse> recipients = recipientRepository.findByUserIdOrderByUpdatedAtDescRecipientIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
        return new RecipientListResponse(recipients);
    }

    @Transactional(readOnly = true)
    public RecipientSearchListResponse searchRecipientsByPhoneNumber(Long userId, RecipientSearchRequest request) {
        String normalizedPhoneNumber = PhoneNumberUtils.normalizeOrThrow(request.phoneNumber());
        Set<String> existingWalletAddresses = recipientRepository.findByUserIdOrderByUpdatedAtDescRecipientIdDesc(userId)
                .stream()
                .map(Recipient::getWalletAddress)
                .collect(java.util.stream.Collectors.toSet());
        List<RecipientSearchItemResponse> candidates = userWalletRepository.findRecipientCandidatesByPhoneNumber(
                        normalizedPhoneNumber,
                        userId
                ).stream()
                .map(candidate -> new RecipientSearchItemResponse(
                        candidate.getUserId(),
                        candidate.getDisplayName(),
                        PhoneNumberUtils.mask(candidate.getPhoneNumber()),
                        maskWalletAddress(candidate.getWalletAddress()),
                        existingWalletAddresses.contains(candidate.getWalletAddress())
                ))
                .toList();
        return new RecipientSearchListResponse(candidates);
    }

    @Transactional
    public RecipientItemResponse createRecipient(Long userId, UpsertRecipientRequest request) {
        ResolvedRecipientTarget resolvedTarget = resolveRecipientTarget(request);
        ensureWalletAddressAvailable(userId, resolvedTarget.walletAddress(), null);
        Recipient recipient = Recipient.create(
                generateRecipientId(),
                userId,
                resolvedTarget.targetUserId(),
                request.alias().trim(),
                request.relation(),
                resolvedTarget.walletAddress(),
                request.allowed()
        );
        return saveRecipient(recipient);
    }

    @Transactional
    public RecipientItemResponse updateRecipient(Long userId, String recipientId, UpsertRecipientRequest request) {
        ResolvedRecipientTarget resolvedTarget = resolveRecipientTarget(request);
        ensureWalletAddressAvailable(userId, resolvedTarget.walletAddress(), recipientId);
        Recipient recipient = getRequiredRecipient(userId, recipientId);
        recipient.update(
                request.alias().trim(),
                request.relation(),
                resolvedTarget.walletAddress(),
                resolvedTarget.targetUserId(),
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

    private ResolvedRecipientTarget resolveRecipientTarget(UpsertRecipientRequest request) {
        if (request.targetUserId() != null) {
            return userWalletRepository.findByUserId(request.targetUserId())
                    .map(wallet -> new ResolvedRecipientTarget(wallet.getWalletAddress().toLowerCase(), request.targetUserId()))
                    .orElseThrow(() -> new ApiException(ErrorCode.RECIPIENT_NOT_FOUND));
        }

        String walletAddress = request.walletAddress();
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new ApiException(ErrorCode.INVALID_WALLET_ADDRESS);
        }
        if (!walletAddress.matches("^0x[a-fA-F0-9]{40}$")) {
            throw new ApiException(ErrorCode.INVALID_WALLET_ADDRESS);
        }
        return new ResolvedRecipientTarget(walletAddress.toLowerCase(), null);
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

    private String maskWalletAddress(String walletAddress) {
        if (walletAddress == null || walletAddress.length() < 12) {
            return walletAddress;
        }
        return walletAddress.substring(0, 6) + "..." + walletAddress.substring(walletAddress.length() - 4);
    }

    private record ResolvedRecipientTarget(String walletAddress, Long targetUserId) {
    }
}
