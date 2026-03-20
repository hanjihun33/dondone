package com.workproofpay.backend.shared.bootstrap;

import com.workproofpay.backend.auth.model.User;
import com.workproofpay.backend.auth.repo.UserRepository;
import com.workproofpay.backend.auth.support.EmailNormalizer;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@Profile("demo")
@RequiredArgsConstructor
public class DevUserInitializer implements CommandLineRunner {

    private static final String DEFAULT_USER_EMAIL = "test@gmail.com";
    private static final String DEFAULT_USER_PASSWORD = "qweqwe123";
    private static final String PDF_DEMO_EMAIL = "demo@test.com";
    private static final String PDF_DEMO_PASSWORD = "qweqwe123";
    private static final String PDF_DEMO_WORKPLACE_NAME = "SSAFY";
    private static final String PDF_DEMO_WORKPLACE_ADDRESS = "광주광역시 광산구 하남산단 6번로 107";
    private static final String PDF_DEMO_WORKPLACE_LABEL = "광주 SSAFY";
    private static final double PDF_DEMO_LATITUDE = 35.2031092d;
    private static final double PDF_DEMO_LONGITUDE = 126.8083831d;
    private static final int PDF_DEMO_ALLOWED_RADIUS_METERS = 1_000;
    private static final String PDF_DEMO_ATTACHMENT_METADATA_JSON = "{\"attachments\":[{\"name\":\"clock-correction.jpg\"}]}";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofAuditLogRepository workProofAuditLogRepository;

    @Override
    @Transactional
    public void run(String... args) {
        ensureDefaultUser();
        ensurePdfDemoUser();
    }

    @SuppressWarnings("null")
    private void ensureDefaultUser() {
        String normalizedEmail = EmailNormalizer.normalize(DEFAULT_USER_EMAIL);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }
        User user = User.register(
                normalizedEmail,
                passwordEncoder.encode(DEFAULT_USER_PASSWORD),
                "Test User",
                "01012345678");
        userRepository.save(user);
    }

    @SuppressWarnings("null")
    private void ensurePdfDemoUser() {
        String normalizedEmail = EmailNormalizer.normalize(PDF_DEMO_EMAIL);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> Objects.requireNonNull(userRepository.save(User.register(
                        normalizedEmail,
                        passwordEncoder.encode(PDF_DEMO_PASSWORD),
                        "PDF Demo User",
                        "01098765432"))));

        Workplace workplace = ensurePdfDemoWorkplace(user);
        WorkContract contract = ensurePdfDemoContract(user, workplace);
        ensurePdfDemoWorkProofs(user, workplace, contract);
    }

    @SuppressWarnings("null")
    private Workplace ensurePdfDemoWorkplace(User user) {
        return workplaceRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(workplace -> PDF_DEMO_WORKPLACE_NAME.equals(workplace.getName()))
                .findFirst()
                .orElseGet(() -> Objects.requireNonNull(workplaceRepository.save(Workplace.create(
                        user,
                        PDF_DEMO_WORKPLACE_NAME,
                        PDF_DEMO_WORKPLACE_ADDRESS,
                        PDF_DEMO_WORKPLACE_LABEL,
                        PDF_DEMO_LATITUDE,
                        PDF_DEMO_LONGITUDE,
                        PDF_DEMO_ALLOWED_RADIUS_METERS))));
    }

    @SuppressWarnings("null")
    private WorkContract ensurePdfDemoContract(User user, Workplace workplace) {
        return workContractRepository
                .findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(workplace.getId(),
                        user.getId())
                .orElseGet(() -> Objects.requireNonNull(workContractRepository.save(WorkContract.activate(
                        workplace,
                        WorkProofPayUnit.HOURLY,
                        BigDecimal.valueOf(12_000),
                        480,
                        10_560,
                        BigDecimal.valueOf(12_000),
                        LocalDate.of(2026, 1, 1)))));
    }

    @SuppressWarnings("null")
    private void ensurePdfDemoWorkProofs(User user, Workplace workplace, WorkContract contract) {
        if (!workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                user.getId(),
                workplace.getId(),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 3, 31)).isEmpty()) {
            return;
        }

        List<WorkProof> seededRecords = List.of(
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 2, 3), 9, 0, 18, 0, false),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 2, 5), 9, 5, 18, 5, false),
                createEditedRecord(user, workplace, contract, LocalDate.of(2026, 2, 9), 9, 0, 18, 0, 9, 15, 18, 10,
                        "출근 시간 정정", "버스 지연으로 앱 입력이 늦었어요.", 0, null),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 2, 12), 9, 0, 18, 0, false),
                createReviewRecord(user, workplace, contract, LocalDate.of(2026, 2, 16), 9, 0, 18, 0, "광주 SSAFY 후문"),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 2, 19), 9, 0, 17, 45, false),
                createEditedRecord(user, workplace, contract, LocalDate.of(2026, 2, 24), 8, 55, 18, 5, 9, 0, 18, 20,
                        "퇴근 누락 보정", "관리자 확인 후 퇴근 시간을 수정했어요.", 1, PDF_DEMO_ATTACHMENT_METADATA_JSON),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 2, 27), 9, 0, 18, 0, false),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 3), 9, 0, 18, 0, false),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 5), 9, 0, 18, 30, false),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 9), 8, 50, 18, 0, false),
                createReviewRecord(user, workplace, contract, LocalDate.of(2026, 3, 11), 9, 0, 18, 0, "광주 SSAFY 주차장"),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 13), 9, 0, 18, 0, false),
                createEditedRecord(user, workplace, contract, LocalDate.of(2026, 3, 17), 9, 0, 17, 50, 9, 10, 18, 10,
                        "지문 인식 오류 정정", "시스템 오작동으로 수동 보정했어요.", 2, PDF_DEMO_ATTACHMENT_METADATA_JSON),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 18), 9, 0, 18, 0, false),
                createReflectedRecord(user, workplace, contract, LocalDate.of(2026, 3, 19), 9, 0, 18, 0, false));

        Objects.requireNonNull(workProofRepository.saveAll(seededRecords));
    }

    private WorkProof createReflectedRecord(User user,
            Workplace workplace,
            WorkContract contract,
            LocalDate workDate,
            int checkInHour,
            int checkInMinute,
            int checkOutHour,
            int checkOutMinute,
            boolean withAttachment) {
        LocalDateTime checkInAt = workDate.atTime(checkInHour, checkInMinute);
        LocalDateTime checkOutAt = workDate.atTime(checkOutHour, checkOutMinute);
        WorkProof record = WorkProof.checkIn(
                user,
                workplace,
                contract,
                checkInAt,
                checkInAt.plusMinutes(1),
                PDF_DEMO_LATITUDE,
                PDF_DEMO_LONGITUDE,
                PDF_DEMO_WORKPLACE_LABEL);
        record.completeCheckOut(
                checkOutAt,
                checkOutAt.plusMinutes(1),
                PDF_DEMO_LATITUDE,
                PDF_DEMO_LONGITUDE,
                PDF_DEMO_WORKPLACE_LABEL,
                false);
        if (withAttachment) {
            record.updateTimes(
                    checkInAt,
                    checkOutAt,
                    "근무 사진 첨부",
                    "근무 현장 확인용 사진을 첨부했어요.",
                    1,
                    PDF_DEMO_ATTACHMENT_METADATA_JSON);
        }
        return record;
    }

    private WorkProof createReviewRecord(User user,
            Workplace workplace,
            WorkContract contract,
            LocalDate workDate,
            int checkInHour,
            int checkInMinute,
            int checkOutHour,
            int checkOutMinute,
            String clockOutLabel) {
        LocalDateTime checkInAt = workDate.atTime(checkInHour, checkInMinute);
        LocalDateTime checkOutAt = workDate.atTime(checkOutHour, checkOutMinute);
        WorkProof record = WorkProof.checkIn(
                user,
                workplace,
                contract,
                checkInAt,
                checkInAt.plusMinutes(1),
                PDF_DEMO_LATITUDE,
                PDF_DEMO_LONGITUDE,
                PDF_DEMO_WORKPLACE_LABEL);
        record.completeCheckOut(
                checkOutAt,
                checkOutAt.plusMinutes(1),
                PDF_DEMO_LATITUDE + 0.02d,
                PDF_DEMO_LONGITUDE + 0.02d,
                clockOutLabel,
                true);
        return record;
    }

    @SuppressWarnings("null")
    private WorkProof createEditedRecord(User user,
            Workplace workplace,
            WorkContract contract,
            LocalDate workDate,
            int originalInHour,
            int originalInMinute,
            int originalOutHour,
            int originalOutMinute,
            int updatedInHour,
            int updatedInMinute,
            int updatedOutHour,
            int updatedOutMinute,
            String editReason,
            String memo,
            int attachmentCount,
            String attachmentMetadataJson) {
        LocalDateTime originalIn = workDate.atTime(originalInHour, originalInMinute);
        LocalDateTime originalOut = workDate.atTime(originalOutHour, originalOutMinute);
        LocalDateTime updatedIn = workDate.atTime(updatedInHour, updatedInMinute);
        LocalDateTime updatedOut = workDate.atTime(updatedOutHour, updatedOutMinute);

        WorkProof record = WorkProof.checkIn(
                user,
                workplace,
                contract,
                originalIn,
                originalIn.plusMinutes(1),
                PDF_DEMO_LATITUDE,
                PDF_DEMO_LONGITUDE,
                PDF_DEMO_WORKPLACE_LABEL);
        record.completeCheckOut(
                originalOut,
                originalOut.plusMinutes(1),
                PDF_DEMO_LATITUDE,
                PDF_DEMO_LONGITUDE,
                PDF_DEMO_WORKPLACE_LABEL,
                false);
        record.updateTimes(
                updatedIn,
                updatedOut,
                editReason,
                memo,
                attachmentCount,
                attachmentMetadataJson);
        WorkProof saved = Objects.requireNonNull(workProofRepository.save(record));
        Objects.requireNonNull(workProofAuditLogRepository.save(WorkProofAuditLog.record(
                saved,
                user.getId(),
                originalIn,
                originalOut,
                updatedIn,
                updatedOut,
                null,
                editReason,
                null,
                memo,
                0,
                attachmentCount,
                null,
                attachmentMetadataJson)));
        return saved;
    }
}
