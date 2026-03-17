package com.workproofpay.remittance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.remittance.domain.JobEntity;
import com.workproofpay.remittance.domain.JobStatus;
import com.workproofpay.remittance.domain.JobType;
import com.workproofpay.remittance.domain.RecipientEntity;
import com.workproofpay.remittance.domain.TransferEntity;
import com.workproofpay.remittance.domain.TransferStatus;
import com.workproofpay.remittance.domain.UserWalletEntity;
import com.workproofpay.remittance.dto.DemoSeedResponse;
import com.workproofpay.remittance.repo.JobRepository;
import com.workproofpay.remittance.repo.RecipientRepository;
import com.workproofpay.remittance.repo.TransferRepository;
import com.workproofpay.remittance.repo.UserWalletRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

@Service
public class DemoSeedService {
    private final ObjectMapper objectMapper;
    private final WalletCryptoService walletCryptoService;
    private final UserWalletRepository userWalletRepository;
    private final RecipientRepository recipientRepository;
    private final TransferRepository transferRepository;
    private final JobRepository jobRepository;

    public DemoSeedService(
            ObjectMapper objectMapper,
            WalletCryptoService walletCryptoService,
            UserWalletRepository userWalletRepository,
            RecipientRepository recipientRepository,
            TransferRepository transferRepository,
            JobRepository jobRepository
    ) {
        this.objectMapper = objectMapper;
        this.walletCryptoService = walletCryptoService;
        this.userWalletRepository = userWalletRepository;
        this.recipientRepository = recipientRepository;
        this.transferRepository = transferRepository;
        this.jobRepository = jobRepository;
    }

    @Transactional
    public DemoSeedResponse seedDefault() {
        DemoSeedPayload payload = loadPayload("demo/remittance-seed-normal-case.json");

        jobRepository.deleteAll();
        transferRepository.deleteAll();
        recipientRepository.deleteAll();
        userWalletRepository.deleteAll();

        Credentials credentials = Credentials.create(payload.wallet().privateKey());

        UserWalletEntity wallet = new UserWalletEntity();
        wallet.setUserId(payload.user().userId());
        wallet.setWalletAddress(credentials.getAddress());
        wallet.setEncryptedPrivateKey(walletCryptoService.encrypt(payload.wallet().privateKey()));
        wallet.setCreatedAt(Instant.parse(payload.wallet().createdAt()));
        userWalletRepository.save(wallet);

        for (RecipientSeed recipientSeed : payload.recipients()) {
            RecipientEntity recipient = new RecipientEntity();
            recipient.setRecipientId(recipientSeed.recipientId());
            recipient.setUserId(payload.user().userId());
            recipient.setAlias(recipientSeed.alias());
            recipient.setWalletAddress(recipientSeed.walletAddress());
            recipient.setRelation(recipientSeed.relation());
            recipient.setAllowed(recipientSeed.allowed());
            recipient.setUpdatedAt(Instant.parse(recipientSeed.updatedAt()));
            recipientRepository.save(recipient);
        }

        for (TransferSeed transferSeed : payload.transfers()) {
            TransferEntity transfer = new TransferEntity();
            transfer.setTransferId(transferSeed.transferId());
            transfer.setUserId(payload.user().userId());
            transfer.setRecipientId(transferSeed.recipientId());
            transfer.setAsset(transferSeed.asset());
            transfer.setAmountAtomic(transferSeed.amountAtomic());
            transfer.setSenderAddress(credentials.getAddress());
            transfer.setRecipientAddress(transferSeed.recipientAddress());
            transfer.setStatus(TransferStatus.valueOf(transferSeed.status()));
            transfer.setTxHash(transferSeed.txHash());
            transfer.setFailureCode(transferSeed.failureCode());
            transfer.setIdempotencyKey(transferSeed.idempotencyKey());
            transfer.setHighAmountConfirmed(transferSeed.highAmountConfirmed());
            transfer.setCreatedAt(Instant.parse(transferSeed.createdAt()));
            transfer.setUpdatedAt(Instant.parse(transferSeed.updatedAt()));
            transferRepository.save(transfer);
        }

        for (JobSeed jobSeed : payload.jobs()) {
            JobEntity job = new JobEntity();
            job.setTransferId(jobSeed.transferId());
            job.setJobType(JobType.valueOf(jobSeed.jobType()));
            job.setStatus(JobStatus.valueOf(jobSeed.status()));
            job.setAttemptCount(jobSeed.attemptCount());
            job.setRunAt(Instant.parse(jobSeed.runAt()));
            job.setLastError(jobSeed.lastError());
            jobRepository.save(job);
        }

        return new DemoSeedResponse(
                payload.seedName(),
                payload.user().userId(),
                credentials.getAddress(),
                payload.recipients().size(),
                payload.transfers().size()
        );
    }

    private DemoSeedPayload loadPayload(String resourcePath) {
        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            try (InputStream inputStream = resource.getInputStream()) {
                return objectMapper.readValue(inputStream, DemoSeedPayload.class);
            }
        } catch (Exception e) {
            throw new IllegalStateException("failed to load demo seed: " + resourcePath, e);
        }
    }

    private record DemoSeedPayload(
            String seedName,
            UserSeed user,
            WalletSeed wallet,
            List<RecipientSeed> recipients,
            List<TransferSeed> transfers,
            List<JobSeed> jobs
    ) {}

    private record UserSeed(
            String userId,
            String displayName
    ) {}

    private record WalletSeed(
            String privateKey,
            String createdAt
    ) {}

    private record RecipientSeed(
            String recipientId,
            String alias,
            String walletAddress,
            String relation,
            boolean allowed,
            String updatedAt
    ) {}

    private record TransferSeed(
            String transferId,
            String recipientId,
            String recipientAddress,
            String asset,
            long amountAtomic,
            String status,
            String txHash,
            String failureCode,
            String idempotencyKey,
            boolean highAmountConfirmed,
            String createdAt,
            String updatedAt
    ) {}

    private record JobSeed(
            String transferId,
            String jobType,
            String status,
            int attemptCount,
            String runAt,
            String lastError
    ) {}
}
