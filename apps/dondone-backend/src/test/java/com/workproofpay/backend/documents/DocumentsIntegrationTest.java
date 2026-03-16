package com.workproofpay.backend.documents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.documents.api.dto.request.CreateProofPackRequest;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.wage.api.dto.request.CreateWageVerificationRequest;
import com.workproofpay.backend.wage.repo.WageDepositRepository;
import com.workproofpay.backend.wage.repo.WageVerificationRepository;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentsIntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WageDepositRepository wageDepositRepository;

    @Autowired
    private WageVerificationRepository wageVerificationRepository;

    @Autowired
    private DocumentGenerationRequestRepository documentGenerationRequestRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        documentGenerationRequestRepository.deleteAll();
        wageVerificationRepository.deleteAll();
        wageDepositRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createsProofPackRequestFromVerificationAnchor() throws Exception {
        // verification 생성 -> proof pack 요청까지 Documents 입력축이 verificationId 하나로 닫히는지 검증한다.
        User user = userRepository.save(User.register("documents@test.com", "hashed", "Documents"));
        String token = tokenFor(user);
        Long verificationId = createVerification(token);

        mockMvc.perform(post("/api/documents/proof-packs")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "proof-pack-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProofPackRequest(verificationId))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.requestId").isNotEmpty())
                .andExpect(jsonPath("$.data.documentType").value("PROOF_PACK"))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.pollUrl").exists());
    }

    @Test
    void rejectsDuplicateIdempotencyKeyForProofPack() throws Exception {
        // 같은 verification이라도 동일 idempotency key 재사용은 문서 생성 중복으로 막아야 한다.
        User user = userRepository.save(User.register("duplicate@test.com", "hashed", "Documents"));
        String token = tokenFor(user);
        Long verificationId = createVerification(token);
        String body = objectMapper.writeValueAsString(new CreateProofPackRequest(verificationId));

        mockMvc.perform(post("/api/documents/proof-packs")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "duplicate-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/documents/proof-packs")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", "duplicate-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DOCUMENT_DUPLICATE_REQUEST"));
    }

    @Test
    void hidesOtherUsersVerificationWhenCreatingProofPack() throws Exception {
        // proof pack도 verification ownership을 그대로 따라가므로 타인 verification은 404로 숨긴다.
        User owner = userRepository.save(User.register("owner-doc@test.com", "hashed", "Owner"));
        String ownerToken = tokenFor(owner);
        Long verificationId = createVerification(ownerToken);

        User other = userRepository.save(User.register("other-doc@test.com", "hashed", "Other"));
        String otherToken = tokenFor(other);

        mockMvc.perform(post("/api/documents/proof-packs")
                        .header("Authorization", bearer(otherToken))
                        .header("Idempotency-Key", "other-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProofPackRequest(verificationId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WAGE_VERIFICATION_NOT_FOUND"));
    }

    private Long createVerification(String token) throws Exception {
        // 근무지 등록 -> 계약 생성 -> 출퇴근 1건 -> verification 생성까지 Documents fixture를 만든다.
        String workplaceBody = mockMvc.perform(post("/api/workproof/workplaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkplaceRequest(
                                "Proof Pack Cafe",
                                "Seoul Somewhere 1",
                                "Front door",
                                37.5,
                                127.0
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workplaceId = readId(workplaceBody, "workplaceId");

        mockMvc.perform(post("/api/workproof/contracts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateContractRequest(
                                workplaceId,
                                WorkProofPayUnit.HOURLY,
                                BigDecimal.valueOf(12_000),
                                null,
                                null,
                                LocalDate.of(2026, 3, 1)
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 10, 9, 0),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutWorkProofRequest(
                                LocalDateTime.of(2026, 3, 10, 18, 0),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isOk());

        String verificationBody = mockMvc.perform(post("/api/wage/verifications")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWageVerificationRequest(
                                "2026-03",
                                workplaceId,
                                90_000L,
                                true,
                                "Need a proof pack"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return readId(verificationBody, "verificationId");
    }

    private Long readId(String json, String fieldName) throws Exception {
        return objectMapper.readTree(json).path("data").path(fieldName).asLong();
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
