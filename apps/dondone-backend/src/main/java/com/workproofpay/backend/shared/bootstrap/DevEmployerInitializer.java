package com.workproofpay.backend.shared.bootstrap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.model.UserRole;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.correction.model.CorrectionDecisionAudit;
import com.workproofpay.backend.correction.model.CorrectionRequest;
import com.workproofpay.backend.correction.model.CorrectionRequestStatus;
import com.workproofpay.backend.correction.repo.CorrectionDecisionAuditRepository;
import com.workproofpay.backend.correction.repo.CorrectionRequestRepository;
import com.workproofpay.backend.employer.model.Company;
import com.workproofpay.backend.employer.model.EmployerProfile;
import com.workproofpay.backend.employer.model.EmploymentMembership;
import com.workproofpay.backend.employer.repo.CompanyRepository;
import com.workproofpay.backend.employer.repo.EmployerProfileRepository;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.employerauth.model.EmployerSignupCode;
import com.workproofpay.backend.employerauth.repo.EmployerSignupCodeRepository;
import com.workproofpay.backend.workproof.api.dto.request.WorkProofAttachmentMetadataRequest;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Profile("!test")
@Component
@RequiredArgsConstructor
public class DevEmployerInitializer implements CommandLineRunner {

    private static final String EMPLOYER_EMAIL = "manager@gmail.com";
    private static final String EMPLOYER_PASSWORD = "qweqwe123";
    private static final String EMPLOYER_DISPLAY_NAME = "돈던 관리자";
    private static final String COMPANY_NAME = "돈던 물류";
    private static final String COMPANY_CODE = "DN-SEOUL-2914";
    private static final String EMPLOYER_SIGNUP_CODE = "EMPLOYER-SEOUL-2026";
    private static final String WORKPLACE_NAME = "서울 허브";
    private static final String WORKPLACE_ADDRESS = "서울특별시 강남구 테헤란로 212";
    private static final String WORKPLACE_DETAIL = "1층 정문";
    private static final double WORKPLACE_LATITUDE = 37.501274;
    private static final double WORKPLACE_LONGITUDE = 127.039585;
    private static final int WORKPLACE_RADIUS_METERS = 300;
    private static final LocalDate MEMBERSHIP_START_DATE = LocalDate.of(2026, 1, 1);
    private static final List<String> SEEDED_WORKER_EMAILS = List.of(
            "worker.complete@acme.test",
            "worker.working@acme.test",
            "worker.review@acme.test",
            "worker.norecord@acme.test"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final WorkplaceRepository workplaceRepository;
    private final EmployerProfileRepository employerProfileRepository;
    private final EmploymentMembershipRepository employmentMembershipRepository;
    private final EmployerSignupCodeRepository employerSignupCodeRepository;
    private final WorkProofRepository workProofRepository;
    private final CorrectionRequestRepository correctionRequestRepository;
    private final CorrectionDecisionAuditRepository correctionDecisionAuditRepository;
    private final WorkProofAuditLogRepository workProofAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        User employerUser = userRepository.findByEmailIgnoreCase(EmailNormalizer.normalize(EMPLOYER_EMAIL))
                .map(existing -> {
                    if (existing.getRole() != UserRole.EMPLOYER) {
                        throw new IllegalStateException("Dev employer seed email is already used by a non-employer account.");
                    }
                    return existing;
                })
                .orElseGet(this::createEmployerUser);

        employerUser.updateProfile(EMPLOYER_DISPLAY_NAME, null);
        userRepository.save(employerUser);

        employerProfileRepository.findByAccountId(employerUser.getId())
                .ifPresent(profile -> resetExistingSeedScope(profile));

        seedEmployerWorkspace(employerUser);
    }

    private User createEmployerUser() {
        return userRepository.save(User.registerEmployer(
                EMPLOYER_EMAIL,
                passwordEncoder.encode(EMPLOYER_PASSWORD),
                EMPLOYER_DISPLAY_NAME
        ));
    }

    private void resetExistingSeedScope(EmployerProfile employerProfile) {
        Long companyId = employerProfile.getCompanyId();
        Long workplaceId = employerProfile.getDefaultWorkplaceId();

        List<CorrectionRequest> correctionRequests = correctionRequestRepository
                .findByCompanyIdAndWorkplaceIdOrderByCreatedAtDescIdDesc(companyId, workplaceId);
        for (CorrectionRequest correctionRequest : correctionRequests) {
            correctionDecisionAuditRepository.deleteAll(
                    correctionDecisionAuditRepository.findByCorrectionRequestIdOrderByCreatedAtDesc(correctionRequest.getId())
            );
        }
        correctionRequestRepository.deleteAll(correctionRequests);

        List<User> seededWorkers = loadSeededWorkers();
        List<Long> seededWorkerIds = seededWorkers.stream()
                .map(User::getId)
                .toList();
        if (!seededWorkerIds.isEmpty()) {
            List<WorkProof> workProofs = workProofRepository.findAll().stream()
                    .filter(workProof -> workProof.getWorkplace() != null)
                    .filter(workProof -> Objects.equals(workProof.getWorkplace().getId(), workplaceId))
                    .filter(workProof -> seededWorkerIds.contains(workProof.getUser().getId()))
                    .toList();
            if (!workProofs.isEmpty()) {
                workProofAuditLogRepository.deleteAll(
                        workProofAuditLogRepository.findByWorkProofIdInOrderByCreatedAtDesc(
                                workProofs.stream().map(WorkProof::getId).toList()
                        )
                );
                workProofRepository.deleteAll(workProofs);
            }
        }

        employmentMembershipRepository.deleteAll(
                employmentMembershipRepository.findByCompanyIdAndWorkplaceId(companyId, workplaceId)
        );
        employerSignupCodeRepository.deleteAll(
                employerSignupCodeRepository.findByCompanyIdAndDefaultWorkplaceId(companyId, workplaceId)
        );
        employerProfileRepository.delete(employerProfile);
        workplaceRepository.findById(workplaceId).ifPresent(workplaceRepository::delete);
        companyRepository.findById(companyId).ifPresent(companyRepository::delete);
        userRepository.deleteAll(seededWorkers);
    }

    private List<User> loadSeededWorkers() {
        List<User> workers = new ArrayList<>();
        for (String email : SEEDED_WORKER_EMAILS) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(workers::add);
        }
        return workers;
    }

    private void seedEmployerWorkspace(User employerUser) {
        Company company = companyRepository.save(Company.create(COMPANY_NAME, COMPANY_CODE));
        Workplace workplace = workplaceRepository.save(Workplace.create(
                employerUser,
                company.getId(),
                WORKPLACE_NAME,
                WORKPLACE_ADDRESS,
                WORKPLACE_DETAIL,
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                WORKPLACE_RADIUS_METERS
        ));
        employerSignupCodeRepository.save(EmployerSignupCode.create(
                EMPLOYER_SIGNUP_CODE,
                company.getId(),
                workplace.getId(),
                employerUser.getId()
        ));
        employerProfileRepository.save(EmployerProfile.create(
                employerUser.getId(),
                company.getId(),
                workplace.getId(),
                EMPLOYER_DISPLAY_NAME
        ));

        User completedWorker = createWorker("worker.complete@acme.test", "김민수", "01020000001");
        User workingWorker = createWorker("worker.working@acme.test", "이서연", "01020000002");
        User reviewWorker = createWorker("worker.review@acme.test", "박준호", "01020000003");
        User noRecordWorker = createWorker("worker.norecord@acme.test", "정하늘", "01020000004");

        createMembership(completedWorker, company, workplace);
        createMembership(workingWorker, company, workplace);
        createMembership(reviewWorker, company, workplace);
        createMembership(noRecordWorker, company, workplace);

        LocalDate today = LocalDate.now();

        createCompletedTodayWorkProof(completedWorker, workplace, today);
        createWorkingTodayWorkProof(workingWorker, workplace, today);
        createReviewTodayWorkProof(reviewWorker, workplace, today);

        createPendingCorrectionSeed(completedWorker, company, workplace, today.minusDays(1));
        createApprovedCorrectionSeed(workingWorker, employerUser, company, workplace, today.minusDays(2));
        createRejectedCorrectionSeed(reviewWorker, employerUser, company, workplace, today.minusDays(3));
    }

    private User createWorker(String email, String name, String phoneNumber) {
        return userRepository.save(User.register(
                email,
                passwordEncoder.encode("qweqwe123"),
                name,
                phoneNumber
        ));
    }

    private void createMembership(User worker, Company company, Workplace workplace) {
        employmentMembershipRepository.save(EmploymentMembership.create(
                worker.getId(),
                company.getId(),
                workplace.getId(),
                MEMBERSHIP_START_DATE
        ));
    }

    private void createCompletedTodayWorkProof(User worker, Workplace workplace, LocalDate date) {
        WorkProof workProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                date.atTime(9, 0),
                date.atTime(9, 1),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                "정문 게이트"
        );
        workProof.completeCheckOut(
                date.atTime(18, 0),
                date.atTime(18, 1),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                "정문 게이트",
                false
        );
        workProofRepository.save(workProof);
    }

    private void createWorkingTodayWorkProof(User worker, Workplace workplace, LocalDate date) {
        workProofRepository.save(WorkProof.checkIn(
                worker,
                workplace,
                null,
                date.atTime(9, 12),
                date.atTime(9, 13),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                "동문 게이트"
        ));
    }

    private void createReviewTodayWorkProof(User worker, Workplace workplace, LocalDate date) {
        WorkProof workProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                date.atTime(8, 55),
                date.atTime(8, 56),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                "주차장 출입구"
        );
        workProof.completeCheckOut(
                date.atTime(18, 7),
                date.atTime(18, 8),
                WORKPLACE_LATITUDE + 0.005,
                WORKPLACE_LONGITUDE + 0.005,
                "사업장 외부 도로",
                true
        );
        workProof.updateTimes(
                date.atTime(8, 55),
                date.atTime(18, 7),
                "GPS 보정 확인 요청",
                "퇴근 위치가 반경 밖으로 기록되어 검토가 필요해요.",
                1,
                toAttachmentMetadataJson(List.of(new WorkProofAttachmentMetadataRequest(
                        WorkProofAttachmentMetadataRequest.AttachmentType.PHOTO,
                        "검토증빙.jpg",
                        "seed://review-evidence"
                )))
        );
        workProofRepository.save(workProof);
    }

    private void createPendingCorrectionSeed(User worker, Company company, Workplace workplace, LocalDate date) {
        WorkProof workProof = createHistoricalReflectedWorkProof(
                worker,
                workplace,
                date,
                9,
                3,
                18,
                1,
                "정문 게이트"
        );

        correctionRequestRepository.save(CorrectionRequest.create(
                workProof,
                worker.getId(),
                worker.getId(),
                company.getId(),
                workplace.getId(),
                date,
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                date.atTime(8, 58),
                date.atTime(18, 12),
                "출근/퇴근 탭을 늦게 눌렀어요",
                "출근 직후 장비 점검 때문에 바로 기록하지 못했어요.",
                1,
                toAttachmentMetadataJson(List.of(new WorkProofAttachmentMetadataRequest(
                        WorkProofAttachmentMetadataRequest.AttachmentType.DOCUMENT,
                        "장비점검기록.pdf",
                        "seed://kiosk-log"
                )))
        ));
    }

    private void createApprovedCorrectionSeed(User worker, User employerUser, Company company, Workplace workplace, LocalDate date) {
        WorkProof workProof = createHistoricalReflectedWorkProof(
                worker,
                workplace,
                date,
                8,
                57,
                17,
                11,
                "동문 게이트"
        );

        CorrectionRequest correctionRequest = correctionRequestRepository.save(CorrectionRequest.create(
                workProof,
                worker.getId(),
                worker.getId(),
                company.getId(),
                workplace.getId(),
                date,
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                date.atTime(8, 57),
                date.atTime(18, 5),
                "회의 정리 때문에 퇴근 입력이 늦었어요",
                "팀 회의 마감 후 정리 때문에 늦게 입력했어요.",
                1,
                toAttachmentMetadataJson(List.of(new WorkProofAttachmentMetadataRequest(
                        WorkProofAttachmentMetadataRequest.AttachmentType.MEMO,
                        "운영메모.txt",
                        "seed://manager-note"
                )))
        ));

        LocalDateTime beforeClockInAt = workProof.getClockInAt();
        LocalDateTime beforeClockOutAt = workProof.getClockOutAt();
        String beforeEditReason = workProof.getEditReason();
        String beforeMemo = workProof.getMemo();
        int beforeAttachmentCount = workProof.getAttachmentCount();
        String beforeAttachmentMetadataJson = workProof.getAttachmentMetadataJson();

        workProof.updateTimes(
                correctionRequest.getRequestedClockInAt(),
                correctionRequest.getRequestedClockOutAt(),
                correctionRequest.getReason(),
                workProof.getMemo(),
                correctionRequest.getAttachmentCount(),
                correctionRequest.getAttachmentMetadataJson()
        );
        workProof = workProofRepository.save(workProof);
        workProofAuditLogRepository.save(WorkProofAuditLog.record(
                workProof,
                employerUser.getId(),
                beforeClockInAt,
                beforeClockOutAt,
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                beforeEditReason,
                workProof.getEditReason(),
                beforeMemo,
                workProof.getMemo(),
                beforeAttachmentCount,
                workProof.getAttachmentCount(),
                beforeAttachmentMetadataJson,
                workProof.getAttachmentMetadataJson()
        ));

        CorrectionRequestStatus beforeStatus = correctionRequest.getStatus();
        correctionRequest.approve(
                employerUser.getId(),
                "회의 종료 후 추가 확인한 내용과 증빙이 일치해 수락했어요.",
                LocalDateTime.now().minusHours(4)
        );
        correctionRequest = correctionRequestRepository.save(correctionRequest);
        correctionDecisionAuditRepository.save(CorrectionDecisionAudit.record(
                correctionRequest,
                employerUser.getId(),
                beforeStatus,
                correctionRequest.getStatus(),
                correctionRequest.getDecisionMemo(),
                null
        ));
    }

    private void createRejectedCorrectionSeed(User worker, User employerUser, Company company, Workplace workplace, LocalDate date) {
        WorkProof workProof = createHistoricalReflectedWorkProof(
                worker,
                workplace,
                date,
                9,
                14,
                18,
                9,
                "주차장 출입구"
        );

        CorrectionRequest correctionRequest = correctionRequestRepository.save(CorrectionRequest.create(
                workProof,
                worker.getId(),
                worker.getId(),
                company.getId(),
                workplace.getId(),
                date,
                workProof.getClockInAt(),
                workProof.getClockOutAt(),
                date.atTime(8, 45),
                date.atTime(18, 20),
                "수기 입력 시간이 실제와 달랐어요",
                "수기 입력으로 남긴 시간이라 정확하지 않을 수 있어요.",
                0,
                null
        ));

        CorrectionRequestStatus beforeStatus = correctionRequest.getStatus();
        correctionRequest.reject(
                employerUser.getId(),
                "입력 시간과 증빙이 맞지 않아 이번 요청은 반려했어요.",
                "MANUAL_ENTRY_MISMATCH",
                LocalDateTime.now().minusHours(2)
        );
        correctionRequest = correctionRequestRepository.save(correctionRequest);
        correctionDecisionAuditRepository.save(CorrectionDecisionAudit.record(
                correctionRequest,
                employerUser.getId(),
                beforeStatus,
                correctionRequest.getStatus(),
                correctionRequest.getDecisionMemo(),
                correctionRequest.getRejectReasonCode()
        ));
    }

    private WorkProof createHistoricalReflectedWorkProof(User worker,
                                                         Workplace workplace,
                                                         LocalDate date,
                                                         int checkInHour,
                                                         int checkInMinute,
                                                         int checkOutHour,
                                                         int checkOutMinute,
                                                         String locationLabel) {
        WorkProof workProof = WorkProof.checkIn(
                worker,
                workplace,
                null,
                date.atTime(checkInHour, checkInMinute),
                date.atTime(checkInHour, checkInMinute).plusMinutes(1),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                locationLabel
        );
        workProof.completeCheckOut(
                date.atTime(checkOutHour, checkOutMinute),
                date.atTime(checkOutHour, checkOutMinute).plusMinutes(1),
                WORKPLACE_LATITUDE,
                WORKPLACE_LONGITUDE,
                locationLabel,
                false
        );
        return workProofRepository.save(workProof);
    }

    private String toAttachmentMetadataJson(List<WorkProofAttachmentMetadataRequest> attachments) {
        try {
            return objectMapper.writeValueAsString(attachments);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize dev seed attachment metadata.", exception);
        }
    }
}
