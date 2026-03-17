package com.workproofpay.remittance.service;

import com.workproofpay.remittance.config.AppProperties;
import com.workproofpay.remittance.domain.RecipientEntity;
import com.workproofpay.remittance.dto.RecipientItemResponse;
import com.workproofpay.remittance.dto.RecipientUpsertRequest;
import com.workproofpay.remittance.repo.RecipientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class RecipientService {
    private final RecipientRepository recipientRepository;
    private final AppProperties appProperties;

    public RecipientService(RecipientRepository recipientRepository, AppProperties appProperties) {
        this.recipientRepository = recipientRepository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public List<RecipientItemResponse> getRecipients(String userId) {
        return recipientRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RecipientItemResponse upsertRecipient(String userId, String recipientId, RecipientUpsertRequest request) {
        RecipientEntity entity = recipientRepository
                .findByRecipientIdAndUserId(recipientId, userId)
                .orElseGet(RecipientEntity::new);

        entity.setRecipientId(recipientId);
        entity.setUserId(userId);
        entity.setAlias(request.alias());
        entity.setWalletAddress(request.walletAddress());
        entity.setRelation(request.relation());
        entity.setAllowed(request.allowed());
        entity.setUpdatedAt(Instant.now());

        recipientRepository.save(entity);
        return toResponse(entity);
    }

    public RecipientEntity getRequiredRecipient(String userId, String recipientId) {
        return recipientRepository.findByRecipientIdAndUserId(recipientId, userId)
                .orElseThrow(() -> new ApiException(404, "RECIPIENT_NOT_FOUND", "Recipient not found", null));
    }

    private RecipientItemResponse toResponse(RecipientEntity entity) {
        Instant cooldownEndsAt = entity.getUpdatedAt().plusSeconds(appProperties.getRemittance().getCooldownSeconds());
        return new RecipientItemResponse(
                entity.getRecipientId(),
                entity.getAlias(),
                entity.getWalletAddress(),
                entity.getRelation(),
                entity.isAllowed(),
                entity.getUpdatedAt(),
                cooldownEndsAt
        );
    }
}
