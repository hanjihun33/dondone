package com.workproofpay.backend.remittance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.RemittanceJobWorker;
import com.workproofpay.backend.remittance.api.dto.request.CreateTransferRequest;
import com.workproofpay.backend.remittance.api.dto.request.UpsertRecipientRequest;
import com.workproofpay.backend.remittance.model.RecipientRelation;
import com.workproofpay.backend.remittance.repo.RecipientRepository;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.remittance.service.RecipientService;
import com.workproofpay.backend.remittance.service.TransferService;
import com.workproofpay.backend.remittance.service.WalletService;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RemittanceIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserWalletRepository userWalletRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RemittanceJobWorker remittanceJobWorker;

    @Autowired
    private WalletService walletService;

    @Autowired
    private RecipientService recipientService;

    @Autowired
    private TransferService transferService;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        transferRepository.deleteAll();
        recipientRepository.deleteAll();
        userWalletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsWalletRecipientTransferAndConfirmsViaJobWorker() throws Exception {
        User user = userRepository.save(User.register("remit@test.com", "hashed", "Remit"));
        String token = tokenFor(user);

        String walletBody = mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.walletAddress").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String walletAddress = readText(walletBody, "walletAddress");

        mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletAddress").value(walletAddress));

        mockMvc.perform(get("/api/remittance/wallets/me/balance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletAddress").value(walletAddress))
                .andExpect(jsonPath("$.data.assetSymbol").value("dUSDC"))
                .andExpect(jsonPath("$.data.tokenBalanceAtomic").value("500000000"));

        String recipientJson = """
                {
                  "alias": "Mom",
                  "relation": "FAMILY",
                  "walletAddress": "0x1111111111111111111111111111111111111111",
                  "allowed": true
                }
                """;

        String recipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recipientJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.recentlyUpdated").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String recipientId = readText(recipientBody, "recipientId");

        String precheckBlockedJson = """
                {
                  "recipientId": "%s",
                  "amountAtomic": 50000000,
                  "highAmountConfirmed": false,
                  "recentRecipientConfirmed": false
                }
                """.formatted(recipientId);

        mockMvc.perform(post("/api/remittance/transfers/precheck")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(precheckBlockedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.policyCode").value("RECENT_RECIPIENT_CONFIRMATION_REQUIRED"));

        String createTransferJson = """
                {
                  "recipientId": "%s",
                  "amountAtomic": 50000000,
                  "highAmountConfirmed": false,
                  "recentRecipientConfirmed": true
                }
                """.formatted(recipientId);

        String createdBody = mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "remit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransferJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transferId = readText(createdBody, "transferId");

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "remit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTransferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value(transferId));

        String mismatchedReplayJson = """
                {
                  "recipientId": "%s",
                  "amountAtomic": 60000000,
                  "highAmountConfirmed": false,
                  "recentRecipientConfirmed": true
                }
                """.formatted(recipientId);

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "remit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchedReplayJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED_WITH_DIFFERENT_PAYLOAD"));

        mockMvc.perform(get("/api/remittance/transfers")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transfers.length()").value(1))
                .andExpect(jsonPath("$.data.transfers[0].transferId").value(transferId))
                .andExpect(jsonPath("$.data.transfers[0].status").value("REQUESTED"));

        remittanceJobWorker.run();
        remittanceJobWorker.run();

        mockMvc.perform(get("/api/remittance/transfers/{transferId}", transferId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transferId").value(transferId))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.txHash").isString());
    }

    @Test
    void enforcesAuthAndHidesForeignTransferAndPolicyBlocks() throws Exception {
        User owner = userRepository.save(User.register("owner@test.com", "hashed", "Owner"));
        User other = userRepository.save(User.register("other@test.com", "hashed", "Other"));
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(other);

        mockMvc.perform(get("/api/remittance/transfers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/remittance/transfers/precheck")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "missing",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": false
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));

        mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isCreated());

        String blockedRecipientJson = """
                {
                  "alias": "Dad",
                  "relation": "FAMILY",
                  "walletAddress": "0x2222222222222222222222222222222222222222",
                  "allowed": false
                }
                """;

        String blockedRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedRecipientJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String blockedRecipientId = readText(blockedRecipientBody, "recipientId");

        String blockedTransferJson = """
                {
                  "recipientId": "%s",
                  "amountAtomic": 50000000,
                  "highAmountConfirmed": false,
                  "recentRecipientConfirmed": true
                }
                """.formatted(blockedRecipientId);

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "blocked-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockedTransferJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_ALLOWED"));

        String allowedRecipientJson = """
                {
                  "alias": "Sister",
                  "relation": "FAMILY",
                  "walletAddress": "0x3333333333333333333333333333333333333333",
                  "allowed": true
                }
                """;

        String allowedRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allowedRecipientJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String allowedRecipientId = readText(allowedRecipientBody, "recipientId");

        String highAmountJson = """
                {
                  "recipientId": "%s",
                  "amountAtomic": 100000001,
                  "highAmountConfirmed": false,
                  "recentRecipientConfirmed": true
                }
                """.formatted(allowedRecipientId);

        mockMvc.perform(post("/api/remittance/transfers/precheck")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(highAmountJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(false))
                .andExpect(jsonPath("$.data.policyCode").value("HIGH_AMOUNT_CONFIRMATION_REQUIRED"));

        String createdBody = mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(ownerToken))
                        .header("Idempotency-Key", "owner-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(allowedRecipientId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String transferId = readText(createdBody, "transferId");

        mockMvc.perform(get("/api/remittance/transfers/{transferId}", transferId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSFER_NOT_FOUND"));
    }

    @Test
    void precheckRequiresExistingWalletAndPutOnlyUpdatesExistingRecipient() throws Exception {
        User user = userRepository.save(User.register("guard@test.com", "hashed", "Guard"));
        String token = tokenFor(user);

        String recipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Brother",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x4444444444444444444444444444444444444444",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String recipientId = readText(recipientBody, "recipientId");

        mockMvc.perform(post("/api/remittance/transfers/precheck")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": false
                                }
                                """.formatted(recipientId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));

        mockMvc.perform(put("/api/remittance/recipients/{recipientId}", "new")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Nope",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x5555555555555555555555555555555555555555",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECIPIENT_NOT_FOUND"));
    }

    @Test
    void rejectsDuplicateRecipientWalletAddressWithConflict() throws Exception {
        User user = userRepository.save(User.register("dup@test.com", "hashed", "Dup"));
        String token = tokenFor(user);

        mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Mom",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x1212121212121212121212121212121212121212",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Mom Again",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x1212121212121212121212121212121212121212",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RECIPIENT_WALLET_ALREADY_EXISTS"));
    }

    @Test
    void searchesRecipientsByPhoneNumberAndMarksAlreadyRegistered() throws Exception {
        User owner = userRepository.save(User.register("search-owner@test.com", "hashed", "Owner", "01022223333"));
        User target = userRepository.save(User.register("search-target@test.com", "hashed", "Target", "01099998888"));
        String ownerToken = tokenFor(owner);

        walletService.createWalletIfAbsent(target.getId());
        String targetWalletAddress = userWalletRepository.findById(target.getId())
                .orElseThrow()
                .getWalletAddress();
        recipientService.createRecipient(
                owner.getId(),
                new UpsertRecipientRequest("Target", RecipientRelation.FRIEND, targetWalletAddress, null, true)
        );

        mockMvc.perform(post("/api/remittance/recipients/search")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-9999-8888"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates.length()").value(1))
                .andExpect(jsonPath("$.data.candidates[0].displayName").value("Target"))
                .andExpect(jsonPath("$.data.candidates[0].maskedPhoneNumber").value("010-****-8888"))
                .andExpect(jsonPath("$.data.candidates[0].walletAddressMasked").value("0x" + targetWalletAddress.substring(2, 6) + "..." + targetWalletAddress.substring(targetWalletAddress.length() - 4)))
                .andExpect(jsonPath("$.data.candidates[0].alreadyRegistered").value(true));
    }

    @Test
    void excludesSelfAndUsersWithoutWalletFromPhoneSearch() throws Exception {
        User owner = userRepository.save(User.register("self-owner@test.com", "hashed", "Owner", "01077776666"));
        userRepository.save(User.register("no-wallet@test.com", "hashed", "No Wallet", "01044445555"));
        String ownerToken = tokenFor(owner);

        mockMvc.perform(post("/api/remittance/recipients/search")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-7777-6666"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates.length()").value(0));

        mockMvc.perform(post("/api/remittance/recipients/search")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "010-4444-5555"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates.length()").value(0));
    }

    @Test
    void createsRecipientFromTargetUserIdWithoutReceivingRawWalletAddressFromSearch() throws Exception {
        User owner = userRepository.save(User.register("target-owner@test.com", "hashed", "Owner", "01012121212"));
        User target = userRepository.save(User.register("target-user@test.com", "hashed", "Target", "01034343434"));
        String ownerToken = tokenFor(owner);

        walletService.createWalletIfAbsent(target.getId());
        String targetWalletAddress = userWalletRepository.findByUserId(target.getId())
                .orElseThrow()
                .getWalletAddress();

        mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Target",
                                  "relation": "FRIEND",
                                  "targetUserId": %d,
                                  "allowed": true
                                }
                                """.formatted(target.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.walletAddress").value(targetWalletAddress));
    }

    @Test
    void returnsBadRequestForInvalidPhoneSearchPayload() throws Exception {
        User owner = userRepository.save(User.register("invalid-search@test.com", "hashed", "Owner", "01077776666"));
        String ownerToken = tokenFor(owner);

        mockMvc.perform(post("/api/remittance/recipients/search")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phoneNumber": "01012"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blocksSelfTransferAndInsufficientBalanceAndKeepsBalanceOnFailedReceipt() throws Exception {
        User user = userRepository.save(User.register("edge@test.com", "hashed", "Edge"));
        String token = tokenFor(user);

        String walletBody = mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String walletAddress = readText(walletBody, "walletAddress");

        String selfRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Me",
                                  "relation": "OTHER",
                                  "walletAddress": "%s",
                                  "allowed": true
                                }
                                """.formatted(walletAddress)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String selfRecipientId = readText(selfRecipientBody, "recipientId");

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "self-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(selfRecipientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_TRANSFER_NOT_ALLOWED"));

        String externalRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Cousin",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x6666666666666666666666666666666666666666",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String externalRecipientId = readText(externalRecipientBody, "recipientId");

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "too-much-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 600000000,
                                  "highAmountConfirmed": true,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(externalRecipientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_WALLET_BALANCE"));

        String failedTransferBody = mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "fail-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 51000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(externalRecipientId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String failedTransferId = readText(failedTransferBody, "transferId");

        remittanceJobWorker.run();
        remittanceJobWorker.run();

        mockMvc.perform(get("/api/remittance/transfers/{transferId}", failedTransferId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureCode").value("NETWORK_ERROR"))
                .andExpect(jsonPath("$.data.txHash").isString());

        mockMvc.perform(get("/api/remittance/wallets/me/balance")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenBalanceAtomic").value("500000000"));
    }

    @Test
    void blocksSecondTransferWhileFirstIsStillInProgress() throws Exception {
        User user = userRepository.save(User.register("busy@test.com", "hashed", "Busy"));
        String token = tokenFor(user);

        mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isCreated());

        String firstRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Mom",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x7777777777777777777777777777777777777777",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String firstRecipientId = readText(firstRecipientBody, "recipientId");

        String secondRecipientBody = mockMvc.perform(post("/api/remittance/recipients")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "alias": "Dad",
                                  "relation": "FAMILY",
                                  "walletAddress": "0x8888888888888888888888888888888888888888",
                                  "allowed": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String secondRecipientId = readText(secondRecipientBody, "recipientId");

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "first-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(firstRecipientId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/remittance/transfers")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "second-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "recipientId": "%s",
                                  "amountAtomic": 50000000,
                                  "highAmountConfirmed": false,
                                  "recentRecipientConfirmed": true
                                }
                                """.formatted(secondRecipientId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TRANSFER_ALREADY_IN_PROGRESS"));
    }

    @Test
    void serializesConcurrentTransferCreationPerUser() throws Exception {
        User user = userRepository.save(User.register("parallel@test.com", "hashed", "Parallel"));
        walletService.createWalletIfAbsent(user.getId());

        String firstRecipientId = recipientService.createRecipient(
                user.getId(),
                new UpsertRecipientRequest("A", RecipientRelation.FAMILY, "0x9999999999999999999999999999999999999999", null, true)
        ).recipientId();
        String secondRecipientId = recipientService.createRecipient(
                user.getId(),
                new UpsertRecipientRequest("B", RecipientRelation.FAMILY, "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", null, true)
        ).recipientId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        try {
            List<Callable<Object>> tasks = List.of(
                    concurrentTransferTask(startLatch, user.getId(), "parallel-1", firstRecipientId),
                    concurrentTransferTask(startLatch, user.getId(), "parallel-2", secondRecipientId)
            );

            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(task));
            }
            startLatch.countDown();

            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> future : futures) {
                outcomes.add(future.get());
            }

            long successCount = outcomes.stream().filter(result -> !(result instanceof Throwable)).count();
            long conflictCount = outcomes.stream()
                    .filter(ApiException.class::isInstance)
                    .map(ApiException.class::cast)
                    .filter(exception -> exception.getErrorCode().name().equals("TRANSFER_ALREADY_IN_PROGRESS"))
                    .count();

            assertThat(successCount).isEqualTo(1);
            assertThat(conflictCount).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Callable<Object> concurrentTransferTask(
            CountDownLatch startLatch,
            Long userId,
            String idempotencyKey,
            String recipientId
    ) {
        return () -> {
            startLatch.await();
            Throwable thrown = catchThrowable(() -> transferService.createTransfer(
                    userId,
                    idempotencyKey,
                    new CreateTransferRequest(recipientId, 50000000L, false, true)
            ));
            return thrown == null ? "created" : thrown;
        };
    }

    private String readText(String body, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path(fieldName).asText();
    }
}
