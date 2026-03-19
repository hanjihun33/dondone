package com.workproofpay.backend.documents.pdf.workproof;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkContract;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.WorkProofFinancialStatus;
import com.workproofpay.backend.workproof.model.WorkProofPayUnit;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkContractRepository;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultWorkProofPdfSnapshotAssembler implements WorkProofPdfSnapshotAssembler {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Locale DEFAULT_LOCALE = Locale.KOREA;
    private static final String TEMPLATE_VERSION = "workproof-statement-v1";

    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final WorkplaceRepository workplaceRepository;
    private final WorkContractRepository workContractRepository;
    private final WorkProofRepository workProofRepository;
    private final WorkProofAuditLogRepository workProofAuditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkProofPdfSnapshot assemble(WorkProofPdfAssembleCommand command) {
        if (command.documentRequestId() == null && isBlank(command.requestId())) {
            throw new ApiException(ErrorCode.INVALID_REQUEST, "documentRequestId or requestId is required");
        }

        Locale locale = resolveLocale(command.locale());
        ZoneId zoneId = resolveZoneId(command.zoneId());

        DocumentGenerationRequest request = loadDocumentRequest(command);
        LocalDate requestStartDate = request.getStartDate();
        LocalDate requestEndDate = request.getEndDate();
        YearMonth yearMonth = request.getMonth() == null ? null : YearMonth.parse(request.getMonth());
        LocalDate startDate = command.startDate() != null
                ? command.startDate()
                : requestStartDate != null
                ? requestStartDate
                : yearMonth.atDay(1);
        LocalDate endDate = command.endDate() != null
                ? command.endDate()
                : requestEndDate != null
                ? requestEndDate
                : yearMonth.atEndOfMonth();
        Long workplaceId = command.workplaceId() != null ? command.workplaceId() : request.getWorkplaceId();

        Workplace workplace = workplaceRepository.findByIdAndUserId(workplaceId, command.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));

        List<WorkProof> records = workProofRepository.findByUserIdAndWorkplaceIdAndWorkDateBetweenOrderByWorkDateDescClockInAtDesc(
                command.userId(),
                workplaceId,
                startDate,
                endDate
        );
        if (records.isEmpty()) {
            throw new ApiException(ErrorCode.WORKPROOF_NOT_FOUND);
        }

        List<Long> recordIds = records.stream()
                .map(WorkProof::getId)
                .toList();

        List<WorkProofAuditLog> audits = workProofAuditLogRepository.findByWorkProofIdInOrderByCreatedAtDesc(recordIds);
        Optional<WorkContract> contract = resolveContract(command.userId(), workplaceId, records);

        int totalRecordCount = records.size();
        int reflectedCount = countByStatus(records, WorkProofFinancialStatus.REFLECTED);
        int needsReviewCount = countByStatus(records, WorkProofFinancialStatus.NEEDS_REVIEW);
        int editedCount = (int) records.stream().filter(WorkProof::isEdited).count();
        int totalAttachmentCount = records.stream().mapToInt(WorkProof::getAttachmentCount).sum();
        long totalWorkedMinutes = records.stream().mapToLong(WorkProof::workedMinutes).sum();

        List<WorkProofPdfSnapshot.WorkProofRecordItem> recordItems = records.stream()
                .map(record -> toRecordItem(record, locale))
                .toList();

        List<WorkProofPdfSnapshot.WorkProofAuditItem> auditItems = audits.stream()
                .map(this::toAuditItem)
                .toList();

        List<String> notices = buildNotices(records, contract.isEmpty(), locale);

        return new WorkProofPdfSnapshot(
                buildMeta(request, yearMonth, zoneId, locale),
                new WorkProofPdfSnapshot.WorkerInfo(
                        request.getUser().getId(),
                        nullToDash(request.getUser().getName()),
                        nullToDash(request.getUser().getEmail())
                ),
                new WorkProofPdfSnapshot.WorkplaceInfo(
                        workplace.getId(),
                        nullToDash(workplace.getName()),
                        nullToDash(workplace.getAddress()),
                        blankToDash(workplace.getMapLabel())
                ),
                buildContractInfo(contract.orElse(null), locale),
                new WorkProofPdfSnapshot.PeriodInfo(
                        formatDate(startDate),
                        formatDate(endDate),
                        request.getMonth() == null ? formatYearMonth(startDate, endDate) : request.getMonth()
                ),
                new WorkProofPdfSnapshot.SummaryInfo(
                        totalRecordCount,
                        reflectedCount,
                        needsReviewCount,
                        editedCount,
                        totalAttachmentCount,
                        totalWorkedMinutes,
                        formatMinutesLabel(totalWorkedMinutes, locale)
                ),
                recordItems,
                auditItems,
                List.copyOf(notices)
        );
    }

    private DocumentGenerationRequest loadDocumentRequest(WorkProofPdfAssembleCommand command) {
        if (command.documentRequestId() != null) {
            return documentGenerationRequestRepository.findByIdAndUserIdAndDocumentType(
                            command.documentRequestId(),
                            command.userId(),
                            resolveSupportedDocumentType(command.documentRequestId())
                    )
                    .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
        }

        return documentGenerationRequestRepository.findByRequestIdAndUserId(command.requestId(), command.userId())
                .filter(request -> isSupportedDocumentType(request.getDocumentType()))
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private DocumentType resolveSupportedDocumentType(Long documentRequestId) {
        return documentGenerationRequestRepository.findById(documentRequestId)
                .map(DocumentGenerationRequest::getDocumentType)
                .filter(this::isSupportedDocumentType)
                .orElseThrow(() -> new ApiException(ErrorCode.DOCUMENT_NOT_FOUND));
    }

    private boolean isSupportedDocumentType(DocumentType documentType) {
        return documentType == DocumentType.PROOF_PACK || documentType == DocumentType.WORKPROOF_STATEMENT;
    }

    private Optional<WorkContract> resolveContract(Long userId, Long workplaceId, List<WorkProof> records) {
        Optional<WorkContract> latestRecordContract = records.stream()
                .map(WorkProof::getContract)
                .filter(contract -> contract != null)
                .findFirst();
        if (latestRecordContract.isPresent()) {
            return latestRecordContract;
        }
        return workContractRepository.findFirstByWorkplaceIdAndWorkplaceUserIdAndEffectiveToIsNullOrderByEffectiveFromDesc(
                workplaceId,
                userId
        );
    }

    private WorkProofPdfSnapshot.DocumentMeta buildMeta(DocumentGenerationRequest request,
                                                        YearMonth yearMonth,
                                                        ZoneId zoneId,
                                                        Locale locale) {
        String periodToken = yearMonth != null
                ? yearMonth.toString().replace("-", "")
                : request.getStartDate() != null
                ? request.getStartDate().toString().replace("-", "")
                : "RANGE";
        String documentPrefix = request.getDocumentType() == DocumentType.WORKPROOF_STATEMENT ? "WS" : "PP";
        String documentNumber = "%s-%s-%06d".formatted(documentPrefix, periodToken, request.getId());
        return new WorkProofPdfSnapshot.DocumentMeta(
                request.getDocumentType().name(),
                documentNumber,
                TEMPLATE_VERSION,
                formatDateTime(ZonedDateTime.now(zoneId).toLocalDateTime()),
                zoneId.getId(),
                locale.toLanguageTag()
        );
    }

    private WorkProofPdfSnapshot.ContractInfo buildContractInfo(WorkContract contract, Locale locale) {
        if (contract == null) {
            return new WorkProofPdfSnapshot.ContractInfo(
                    localizedMissing(locale),
                    localizedMissing(locale),
                    localizedMissing(locale),
                    null,
                    null,
                    localizedMissing(locale),
                    localizedMissing(locale)
            );
        }

        return new WorkProofPdfSnapshot.ContractInfo(
                payUnitLabel(contract.getPayUnit(), locale),
                formatAmount(contract.getBasePayAmount(), locale),
                formatAmount(contract.getNormalizedHourlyWage(), locale),
                contract.getDailyWorkMinutes(),
                contract.getMonthlyWorkMinutes(),
                formatDate(contract.getEffectiveFrom()),
                contract.getEffectiveTo() == null ? localizedCurrent(locale) : formatDate(contract.getEffectiveTo())
        );
    }

    private WorkProofPdfSnapshot.WorkProofRecordItem toRecordItem(WorkProof record, Locale locale) {
        long workedMinutes = record.workedMinutes();
        return new WorkProofPdfSnapshot.WorkProofRecordItem(
                record.getId(),
                formatDate(record.getWorkDate()),
                formatTime(record.getClockInAt()),
                formatTime(record.getClockOutAt()),
                workedMinutes,
                formatMinutesLabel(workedMinutes, locale),
                blankToDash(record.getClockInLocationLabel()),
                blankToDash(record.getClockOutLocationLabel()),
                record.getFinancialStatus().name(),
                financialStatusLabel(record.getFinancialStatus(), locale),
                financialStatusTone(record.getFinancialStatus()),
                record.isEdited(),
                record.isClockOutOutsideAllowedRadius(),
                record.isClockOutOutsideAllowedRadius() ? outsideAllowedRadiusLabel(locale) : localizedNotApplicable(locale),
                blankToDash(record.getEditReason()),
                blankToDash(record.getMemo()),
                record.getAttachmentCount(),
                formatDateTime(record.getCreatedAt()),
                formatDateTime(record.getUpdatedAt())
        );
    }

    private WorkProofPdfSnapshot.WorkProofAuditItem toAuditItem(WorkProofAuditLog audit) {
        return new WorkProofPdfSnapshot.WorkProofAuditItem(
                audit.getId(),
                audit.getWorkProof().getId(),
                formatDateTime(audit.getCreatedAt()),
                formatTime(audit.getBeforeClockInAt()),
                formatTime(audit.getBeforeClockOutAt()),
                formatTime(audit.getAfterClockInAt()),
                formatTime(audit.getAfterClockOutAt()),
                blankToDash(audit.getBeforeMemo()),
                blankToDash(audit.getAfterMemo()),
                blankToDash(audit.getBeforeEditReason()),
                blankToDash(audit.getAfterEditReason())
        );
    }

    private List<String> buildNotices(List<WorkProof> records, boolean missingContract, Locale locale) {
        List<String> notices = new ArrayList<>();

        if (records.stream().anyMatch(WorkProof::isNeedsReview)) {
            notices.add(localizedNeedsReviewNotice(locale));
        }
        if (records.stream().anyMatch(WorkProof::isClockOutOutsideAllowedRadius)) {
            notices.add(localizedOutsideRadiusNotice(locale));
        }
        if (records.stream().anyMatch(WorkProof::isEdited)) {
            notices.add(localizedEditedNotice(locale));
        }
        if (records.stream().anyMatch(record -> record.getAttachmentCount() > 0)) {
            notices.add(localizedAttachmentNotice(locale));
        }
        if (missingContract) {
            notices.add(localizedMissingContractNotice(locale));
        }

        return notices;
    }

    private int countByStatus(List<WorkProof> records, WorkProofFinancialStatus status) {
        return (int) records.stream()
                .filter(record -> record.getFinancialStatus() == status)
                .count();
    }

    private Locale resolveLocale(Locale locale) {
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    private ZoneId resolveZoneId(ZoneId zoneId) {
        return zoneId == null ? DEFAULT_ZONE_ID : zoneId;
    }

    private String payUnitLabel(WorkProofPayUnit payUnit, Locale locale) {
        boolean korean = isKorean(locale);
        return switch (payUnit) {
            case HOURLY -> korean ? "시급" : "Hourly";
            case DAILY -> korean ? "일급" : "Daily";
            case MONTHLY -> korean ? "월급" : "Monthly";
        };
    }

    private String financialStatusLabel(WorkProofFinancialStatus status, Locale locale) {
        boolean korean = isKorean(locale);
        return switch (status) {
            case PENDING -> korean ? "대기" : "Pending";
            case NEEDS_REVIEW -> korean ? "검토 필요" : "Needs Review";
            case REFLECTED -> korean ? "반영 완료" : "Reflected";
        };
    }

    private String financialStatusTone(WorkProofFinancialStatus status) {
        return switch (status) {
            case PENDING -> "neutral";
            case NEEDS_REVIEW -> "warning";
            case REFLECTED -> "success";
        };
    }

    private String formatAmount(BigDecimal amount, Locale locale) {
        if (amount == null) {
            return localizedMissing(locale);
        }

        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        formatter.setMinimumFractionDigits(0);
        formatter.setMaximumFractionDigits(amount.stripTrailingZeros().scale() > 0 ? 2 : 0);
        String formatted = formatter.format(amount);
        return isKorean(locale) ? formatted + "원" : formatted + " KRW";
    }

    private String formatMinutesLabel(long minutes, Locale locale) {
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        return isKorean(locale)
                ? "%d시간 %02d분".formatted(hours, remainMinutes)
                : "%dh %02dm".formatted(hours, remainMinutes);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatYearMonth(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return "-";
        }
        if (startDate.getYear() == endDate.getYear() && startDate.getMonth() == endDate.getMonth()) {
            return "%04d-%02d".formatted(startDate.getYear(), startDate.getMonthValue());
        }
        return "%s ~ %s".formatted(formatDate(startDate), formatDate(endDate));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : TIME_FORMATTER.format(dateTime);
    }

    private boolean isKorean(Locale locale) {
        return locale.getLanguage().equalsIgnoreCase(Locale.KOREAN.getLanguage());
    }

    private String outsideAllowedRadiusLabel(Locale locale) {
        return isKorean(locale) ? "반경 외 퇴근" : "Outside Radius";
    }

    private String localizedNotApplicable(Locale locale) {
        return isKorean(locale) ? "-" : "-";
    }

    private String localizedCurrent(Locale locale) {
        return isKorean(locale) ? "현재" : "Current";
    }

    private String localizedMissing(Locale locale) {
        return isKorean(locale) ? "정보 없음" : "N/A";
    }

    private String localizedNeedsReviewNotice(Locale locale) {
        return isKorean(locale)
                ? "검토 필요 상태의 기록이 포함되어 있어 정산 전 수동 확인이 필요합니다."
                : "Some records require manual review before financial use.";
    }

    private String localizedOutsideRadiusNotice(Locale locale) {
        return isKorean(locale)
                ? "허용 반경 밖에서 퇴근 처리된 기록이 포함되어 있습니다."
                : "Some checkout records were captured outside the allowed radius.";
    }

    private String localizedEditedNotice(Locale locale) {
        return isKorean(locale)
                ? "수정된 기록은 하단 수정 이력 섹션에서 변경 전후 내용을 함께 확인할 수 있습니다."
                : "Edited records are listed with change history below.";
    }

    private String localizedAttachmentNotice(Locale locale) {
        return isKorean(locale)
                ? "첨부 수는 포함되지만 첨부 원본 파일 본문은 이 PDF에 직접 포함되지 않습니다."
                : "Attachment counts are included, but raw attachment bodies are not embedded in this PDF.";
    }

    private String localizedMissingContractNotice(Locale locale) {
        return isKorean(locale)
                ? "계약 정보가 확인되지 않아 계약 요약 일부가 비어 있습니다."
                : "Contract summary is partially unavailable because no contract snapshot was found.";
    }

    private String nullToDash(String value) {
        return value == null ? "-" : value;
    }

    private String blankToDash(String value) {
        return isBlank(value) ? "-" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
