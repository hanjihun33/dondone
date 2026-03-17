package com.workproofpay.backend.workproof;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.shared.security.JwtTokenProvider;
import com.workproofpay.backend.support.PostgresIntegrationTestSupport;
import com.workproofpay.backend.workproof.api.dto.request.CheckInWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CheckOutWorkProofRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateContractRequest;
import com.workproofpay.backend.workproof.api.dto.request.CreateWorkplaceRequest;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WorkProofLane1IntegrationTest extends PostgresIntegrationTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkProofRepository workProofRepository;

    @Autowired
    private WorkProofAuditLogRepository workProofAuditLogRepository;

    @Autowired
    private WorkContractRepository workContractRepository;

    @Autowired
    private WorkplaceRepository workplaceRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        workProofAuditLogRepository.deleteAll();
        workProofRepository.deleteAll();
        workContractRepository.deleteAll();
        workplaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void providesTemporarySsafyWorkplaceWhenUserHasNoWorkplace() throws Exception {
        User user = userRepository.save(User.register("ssafy-temp@test.com", "hashed", "Temp"));
        String token = tokenFor(user);

        mockMvc.perform(get("/api/workproof/workplaces")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaces.length()").value(1))
                .andExpect(jsonPath("$.data.workplaces[0].name").value("SSAFY (임시)"))
                .andExpect(jsonPath("$.data.workplaces[0].address").value("광주광역시 광산구 하남산단 6번로 107"))
                .andExpect(jsonPath("$.data.workplaces[0].latitude").value(35.2031092))
                .andExpect(jsonPath("$.data.workplaces[0].longitude").value(126.8083831))
                .andExpect(jsonPath("$.data.workplaces[0].allowedRadiusMeters").value(1000))
                .andExpect(jsonPath("$.data.workplaces[0].hasActiveContract").value(false));
    }

    @Test
    void createsLane1WorkplaceContractAndRecordFlow() throws Exception {
        // 근무지 등록 -> 활성 계약 생성 -> 출근/퇴근 -> 목록/상세/월간 요약까지 lane 1 happy path를 검증한다.
        User user = userRepository.save(User.register("lane1@test.com", "hashed", "Lane1"));
        String token = tokenFor(user);

        // 1. 근무지 등록
        CreateWorkplaceRequest workplaceRequest = new CreateWorkplaceRequest(
                "DonDone Cafe",
                "Seoul Somewhere 1",
                "Front door",
                37.5,
                127.0
        );

        String workplaceBody = mockMvc.perform(post("/api/workproof/workplaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workplaceRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("DonDone Cafe"))
                .andExpect(jsonPath("$.data.allowedRadiusMeters").value(1000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long workplaceId = readId(workplaceBody, "workplaceId");

        // 2. 계약 생성 전 근무지 목록 상태 확인
        mockMvc.perform(get("/api/workproof/workplaces")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaces.length()").value(1))
                .andExpect(jsonPath("$.data.workplaces[0].allowedRadiusMeters").value(1000))
                .andExpect(jsonPath("$.data.workplaces[0].hasActiveContract").value(false));

        // 3. 활성 계약 생성
        CreateContractRequest contractRequest = new CreateContractRequest(
                workplaceId,
                WorkProofPayUnit.DAILY,
                BigDecimal.valueOf(96_000),
                null,
                null,
                LocalDate.of(2026, 3, 1)
        );

        String contractBody = mockMvc.perform(post("/api/workproof/contracts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(contractRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.payUnit").value("DAILY"))
                .andExpect(jsonPath("$.data.dailyWorkMinutes").value(480))
                .andExpect(jsonPath("$.data.normalizedHourlyWage").value(12000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long contractId = readId(contractBody, "contractId");

        // 4. 계약 생성 후 근무지/현재 계약 조회 검증
        mockMvc.perform(get("/api/workproof/workplaces")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaces[0].hasActiveContract").value(true));

        mockMvc.perform(get("/api/workproof/contracts/current")
                        .header("Authorization", bearer(token))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.contractId").value(contractId))
                        .andExpect(jsonPath("$.data.isActive").value(true));

        // 5. 출근 기록 생성
        CheckInWorkProofRequest checkInRequest = new CheckInWorkProofRequest(
                workplaceId,
                LocalDateTime.of(2026, 3, 13, 9, 0),
                37.5,
                127.0,
                "Front door"
        );

        String checkInBody = mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CHECKED_IN"))
                .andExpect(jsonPath("$.data.reflectionStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.workplace.workplaceId").value(workplaceId))
                .andExpect(jsonPath("$.data.contract.contractId").value(contractId))
                .andExpect(jsonPath("$.data.checkOut").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recordId = readId(checkInBody, "recordId");

        // 6. 퇴근 기록 확정
        CheckOutWorkProofRequest checkOutRequest = new CheckOutWorkProofRequest(
                LocalDateTime.of(2026, 3, 13, 18, 0),
                37.5001,
                127.0001,
                "Front door"
        );

        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkOutRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.reflectionStatus").value("REFLECTED"))
                .andExpect(jsonPath("$.data.workedMinutes").value(540));

        // 7. 월별 목록과 상세 조회가 같은 근무 기록을 반환하는지 검증
        mockMvc.perform(get("/api/workproof/records")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].recordId").value(recordId))
                .andExpect(jsonPath("$.data.records[0].reflectionStatus").value("REFLECTED"))
                .andExpect(jsonPath("$.data.records[0].workedMinutes").value(540));

        mockMvc.perform(get("/api/workproof/records/{recordId}", recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.reflectionStatus").value("REFLECTED"))
                .andExpect(jsonPath("$.data.riskFlags.length()").value(0))
                .andExpect(jsonPath("$.data.modifications.length()").value(0))
                .andExpect(jsonPath("$.data.attachments.length()").value(0));

        // 8. Wage/Advance 입력용 월간 요약 shape 검증
        mockMvc.perform(get("/api/workproof/monthly-summary")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workDayCount").value(1))
                .andExpect(jsonPath("$.data.totalWorkMinutes").value(540))
                .andExpect(jsonPath("$.data.overtimeMinutes").value(60))
                .andExpect(jsonPath("$.data.nightMinutes").value(0))
                .andExpect(jsonPath("$.data.integrity.recordedWorkDays").value(1))
                .andExpect(jsonPath("$.data.integrity.verifiedMinutes").value(540))
                .andExpect(jsonPath("$.data.financeReadiness.advanceEligibleWorkDays").value(1));
    }

    @Test
    void rejectsUnauthorizedAndLane1Conflicts() throws Exception {
        // 인증 누락, 활성 계약 중복, 활성 출근 중복, 잘못된 퇴근 시각 같은 lane 1 충돌 규칙을 검증한다.
        User user = userRepository.save(User.register("conflict@test.com", "hashed", "Conflict"));
        String token = tokenFor(user);
        Long workplaceId = createWorkplaceAndContract(token);

        // 1. 인증 없이 보호 endpoint에 접근하면 401이 반환되어야 한다.
        mockMvc.perform(post("/api/workproof/workplaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkplaceRequest(
                                "Unauthorized",
                                "Nowhere",
                                null,
                                37.0,
                                127.0
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        // 2. query validation 실패 시 구조화된 입력 오류가 반환되어야 한다.
        mockMvc.perform(get("/api/workproof/records")
                        .header("Authorization", bearer(token))
                        .param("month", "2026/03")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"));

        mockMvc.perform(get("/api/workproof/records")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-031")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.details[*].message").value(hasItem("month must be exactly 7 characters")));

        // 3. 같은 근무지에 활성 계약이 이미 있으면 중복 생성이 막혀야 한다.
        mockMvc.perform(post("/api/workproof/contracts")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateContractRequest(
                                workplaceId,
                                WorkProofPayUnit.HOURLY,
                                BigDecimal.valueOf(11_000),
                                null,
                                null,
                                null
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_CONTRACT_EXISTS"));

        // 4. 활성 출근 기록이 있는 동안에는 중복 체크인이 막혀야 한다.
        mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 14, 9, 0),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 14, 9, 5),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVE_WORKPROOF_EXISTS"));

        // 5. 체크인보다 이른 퇴근 시각은 충돌 오류로 거절되어야 한다.
        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutWorkProofRequest(
                                LocalDateTime.of(2026, 3, 14, 8, 59),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECK_OUT_BEFORE_CHECK_IN"));
    }

    @Test
    void rejectsClockInOutsideAllowedRadiusAndMarksOutsideCheckOutForReview() throws Exception {
        User user = userRepository.save(User.register("geofence@test.com", "hashed", "Geofence"));
        String token = tokenFor(user);
        Long workplaceId = createWorkplaceAndContract(token);

        mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 16, 9, 0),
                                37.52,
                                127.02,
                                "Outside radius"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKPLACE_RADIUS_EXCEEDED"));

        String checkInBody = mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 16, 9, 5),
                                37.5004,
                                127.0004,
                                "Front door"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CHECKED_IN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recordId = readId(checkInBody, "recordId");

        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutWorkProofRequest(
                                LocalDateTime.of(2026, 3, 16, 18, 0),
                                37.52,
                                127.02,
                                "Outside radius"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.status").value("CHECKED_OUT"))
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workedMinutes").value(535))
                .andExpect(jsonPath("$.data.riskFlags[0]").value("CHECK_OUT_OUTSIDE_RADIUS"));

        mockMvc.perform(get("/api/workproof/records")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].recordId").value(recordId))
                .andExpect(jsonPath("$.data.records[0].reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.records[0].riskFlags[0]").value("CHECK_OUT_OUTSIDE_RADIUS"));

        mockMvc.perform(get("/api/workproof/records/{recordId}", recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recordId").value(recordId))
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.riskFlags[0]").value("CHECK_OUT_OUTSIDE_RADIUS"));

        mockMvc.perform(get("/api/workproof/monthly-summary")
                        .header("Authorization", bearer(token))
                        .param("month", "2026-03")
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integrity.pendingMinutes").value(535))
                .andExpect(jsonPath("$.data.integrity.workproofRiskFlags").value(hasItem("CHECK_OUT_OUTSIDE_RADIUS_PRESENT")));
    }

    @Test
    void keepsNeedsReviewAfterEditingOutsideRadiusCheckOutRecord() throws Exception {
        User user = userRepository.save(User.register("review-edit@test.com", "hashed", "ReviewEdit"));
        String token = tokenFor(user);
        Long workplaceId = createWorkplaceAndContract(token);

        String checkInBody = mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 17, 9, 0),
                                37.5004,
                                127.0004,
                                "Front door"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recordId = readId(checkInBody, "recordId");

        mockMvc.perform(post("/api/workproof/records/check-out")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckOutWorkProofRequest(
                                LocalDateTime.of(2026, 3, 17, 18, 0),
                                37.52,
                                127.02,
                                "Outside radius"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"));

        String updateRequest = """
                {
                  "clockInAt": "2026-03-17T09:10:00",
                  "clockOutAt": "2026-03-17T18:10:00",
                  "editReason": "반경 밖에서 퇴근해 시간을 정정합니다.",
                  "memo": "집 도착 후 퇴근 처리",
                  "attachmentCount": 0
                }
                """;

        mockMvc.perform(patch("/api/workproof/{workProofId}", recordId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(recordId))
                .andExpect(jsonPath("$.data.financialStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.workedMinutes").doesNotExist());

        mockMvc.perform(get("/api/workproof/records/{recordId}", recordId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reflectionStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.riskFlags[0]").value("CHECK_OUT_OUTSIDE_RADIUS"));
    }

    @Test
    void hidesForeignWorkplaceAndRecordOwnership() throws Exception {
        // 타인 소유 근무지와 근무 기록은 404로 은닉되는지 확인한다.
        User owner = userRepository.save(User.register("owner-l1@test.com", "hashed", "Owner"));
        User other = userRepository.save(User.register("other-l1@test.com", "hashed", "Other"));
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(other);

        // 1. 소유자가 근무지/계약/출근 기록을 먼저 만든다.
        Long workplaceId = createWorkplaceAndContract(ownerToken);

        String checkInBody = mockMvc.perform(post("/api/workproof/records/check-in")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInWorkProofRequest(
                                workplaceId,
                                LocalDateTime.of(2026, 3, 15, 9, 0),
                                37.5,
                                127.0,
                                "Front door"
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long recordId = readId(checkInBody, "recordId");

        // 2. 다른 사용자는 소유하지 않은 근무지와 기록을 조회할 수 없어야 한다.
        mockMvc.perform(get("/api/workproof/contracts/current")
                        .header("Authorization", bearer(otherToken))
                        .param("workplaceId", workplaceId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKPLACE_NOT_FOUND"));

        mockMvc.perform(get("/api/workproof/records/{recordId}", recordId)
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("WORKPROOF_NOT_FOUND"));
    }

    private Long createWorkplaceAndContract(String token) throws Exception {
        String workplaceBody = mockMvc.perform(post("/api/workproof/workplaces")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkplaceRequest(
                                "Shared Store",
                                "Seoul Somewhere 2",
                                null,
                                37.5,
                                127.0
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.allowedRadiusMeters").value(1000))
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
                                null
                        ))))
                .andExpect(status().isCreated());

        return workplaceId;
    }

    private Long readId(String json, String fieldName) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        return root.path("data").path(fieldName).asLong();
    }

    private String tokenFor(User user) {
        return jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole().name());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
