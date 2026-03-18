package com.workproofpay.remittance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.remittance.domain.TransferEntity;
import com.workproofpay.remittance.dto.IntegrityHashResponse;
import com.workproofpay.remittance.repo.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TransferIntegrityService {
    private final TransferRepository transferRepository;
    private final ObjectMapper objectMapper;

    public TransferIntegrityService(
            TransferRepository transferRepository,
            ObjectMapper objectMapper
    ) {
        this.transferRepository = transferRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public IntegrityHashResponse generateForTransfer(Long userId, String transferId) {
        TransferEntity transfer = transferRepository.findByTransferIdAndUserId(transferId, userId)
                .orElseThrow(() -> new ApiException(404, "TRANSFER_NOT_FOUND", "Transfer not found", null));

        String docType = "TRANSFER_RECEIPT";
        String sourceRef = transfer.getTransferId();
        int itemCount = 1;
        String normalizedPayload = buildNormalizedPayload(transfer, docType, sourceRef, itemCount);
        String payloadHash = Hash.sha3String(normalizedPayload);
        String proofId = Hash.sha3String(docType + ":" + sourceRef + ":" + payloadHash);

        return new IntegrityHashResponse(
                docType,
                sourceRef,
                itemCount,
                normalizedPayload,
                payloadHash,
                proofId
        );
    }

    private String buildNormalizedPayload(TransferEntity transfer, String docType, String sourceRef, int itemCount) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("schemaVersion", "transfer-integrity-v1");
        payload.put("docType", docType);
        payload.put("sourceRef", sourceRef);
        payload.put("itemCount", itemCount);
        payload.put("transferId", transfer.getTransferId());
        payload.put("userId", transfer.getUserId());
        payload.put("senderAddress", nullSafe(transfer.getSenderAddress()));
        payload.put("recipientAddress", nullSafe(transfer.getRecipientAddress()));
        payload.put("asset", nullSafe(transfer.getAsset()));
        payload.put("amountAtomic", transfer.getAmountAtomic());
        payload.put("status", transfer.getStatus() == null ? "" : transfer.getStatus().name());
        payload.put("txHash", nullSafe(transfer.getTxHash()));
        payload.put("failureCode", nullSafe(transfer.getFailureCode()));
        payload.put("updatedAt", transfer.getUpdatedAt() == null ? "" : transfer.getUpdatedAt().toString());

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to build normalized payload", e);
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
