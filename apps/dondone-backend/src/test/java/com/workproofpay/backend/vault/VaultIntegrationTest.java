package com.workproofpay.backend.vault;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.jobs.repo.JobRepository;
import com.workproofpay.backend.jobs.service.VaultJobWorker;
import com.workproofpay.backend.remittance.repo.RecipientRepository;
import com.workproofpay.backend.remittance.repo.TransferRepository;
import com.workproofpay.backend.remittance.repo.UserWalletRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.vault.repo.VaultPositionRepository;
import com.workproofpay.backend.vault.repo.VaultTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class VaultIntegrationTest extends PostgresIntegrationTestSupport {

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
    private VaultPositionRepository vaultPositionRepository;

    @Autowired
    private VaultTransactionRepository vaultTransactionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private VaultJobWorker vaultJobWorker;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        transferRepository.deleteAll();
        recipientRepository.deleteAll();
        vaultTransactionRepository.deleteAll();
        vaultPositionRepository.deleteAll();
        userWalletRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsAndConfirmsDepositAndWithdrawalTransactions() throws Exception {
        User user = userRepository.save(User.register("vault@test.com", "hashed", "Vault User"));
        String token = bearer(tokenFor(user));

        mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", token))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/vault/summary")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletTokenBalanceAtomic").value("500000000"))
                .andExpect(jsonPath("$.data.availableToStoreAmountAtomic").value("500000000"))
                .andExpect(jsonPath("$.data.storedAmountAtomic").value("0"))
                .andExpect(jsonPath("$.data.shareBalance").value("0"))
                .andExpect(jsonPath("$.data.interestPreview.monthlyEstimatedYieldAtomic").value("0"));

        String firstDepositResponse = mockMvc.perform(post("/api/vault/deposits")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 100000000
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.txType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String depositRequestId = readText(firstDepositResponse, "requestId");

        mockMvc.perform(post("/api/vault/deposits")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-deposit-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 100000000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(depositRequestId))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        vaultJobWorker.run();
        vaultJobWorker.run();

        mockMvc.perform(get("/api/vault/transactions/{vaultTransactionId}", depositRequestId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.requestId").value(depositRequestId))
                .andExpect(jsonPath("$.data.txType").value("DEPOSIT"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.amountAtomic").value("100000000"))
                .andExpect(jsonPath("$.data.shareDelta").value("100000000"))
                .andExpect(jsonPath("$.data.txHash").isString())
                .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty());

        mockMvc.perform(get("/api/vault/summary")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletTokenBalanceAtomic").value("400000000"))
                .andExpect(jsonPath("$.data.availableToStoreAmountAtomic").value("400000000"))
                .andExpect(jsonPath("$.data.storedAmountAtomic").value("100000000"))
                .andExpect(jsonPath("$.data.shareBalance").value("100000000"))
                .andExpect(jsonPath("$.data.interestPreview.monthlyEstimatedYieldAtomic").value("416666"));

        String withdrawalResponse = mockMvc.perform(post("/api/vault/withdrawals")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-withdraw-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 50000000
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.txType").value("WITHDRAW"))
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String withdrawalRequestId = readText(withdrawalResponse, "requestId");

        vaultJobWorker.run();
        vaultJobWorker.run();

        mockMvc.perform(get("/api/vault/transactions")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.transactions", hasSize(2)))
                .andExpect(jsonPath("$.data.transactions[0].requestId").value(withdrawalRequestId))
                .andExpect(jsonPath("$.data.transactions[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.transactions[1].requestId").value(depositRequestId))
                .andExpect(jsonPath("$.data.transactions[1].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/vault/summary")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.walletTokenBalanceAtomic").value("450000000"))
                .andExpect(jsonPath("$.data.availableToStoreAmountAtomic").value("450000000"))
                .andExpect(jsonPath("$.data.storedAmountAtomic").value("50000000"))
                .andExpect(jsonPath("$.data.shareBalance").value("50000000"));
    }

    @Test
    void enforcesAuthWalletIdempotencyAndBalanceGuards() throws Exception {
        User user = userRepository.save(User.register("vault-guard@test.com", "hashed", "Guard"));
        String token = bearer(tokenFor(user));

        mockMvc.perform(get("/api/vault/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/vault/summary")
                        .header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WALLET_NOT_FOUND"));

        mockMvc.perform(post("/api/remittance/wallets/me")
                        .header("Authorization", token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/vault/deposits")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 1000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));

        mockMvc.perform(post("/api/vault/deposits")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-invalid-amount")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        mockMvc.perform(post("/api/vault/withdrawals")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-withdraw-too-much")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 1000
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VAULT_INSUFFICIENT_STORED_BALANCE"));

        mockMvc.perform(post("/api/vault/deposits")
                        .header("Authorization", token)
                        .header("Idempotency-Key", "vault-oversized-deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "amountAtomic": 600000000
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VAULT_INSUFFICIENT_AVAILABLE_BALANCE"));
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String readText(String body, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        return root.path("data").path(fieldName).asText();
    }
}
