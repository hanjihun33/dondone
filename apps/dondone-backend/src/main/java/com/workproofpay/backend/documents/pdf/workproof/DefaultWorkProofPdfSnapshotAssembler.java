package com.workproofpay.backend.documents.pdf.workproof;

import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.employer.model.EmploymentMembershipStatus;
import com.workproofpay.backend.employer.repo.EmploymentMembershipRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.workproof.model.WorkProof;
import com.workproofpay.backend.workproof.model.WorkProofAuditLog;
import com.workproofpay.backend.workproof.model.Workplace;
import com.workproofpay.backend.workproof.repo.WorkProofAuditLogRepository;
import com.workproofpay.backend.workproof.repo.WorkProofRepository;
import com.workproofpay.backend.workproof.repo.WorkplaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

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
    private final WorkProofRepository workProofRepository;
    private final WorkProofAuditLogRepository workProofAuditLogRepository;
    private final EmploymentMembershipRepository employmentMembershipRepository;

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

        Workplace workplace = getAccessibleWorkplace(command.userId(), workplaceId);

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

        int totalWorkDayCount = (int) records.stream()
                .map(WorkProof::getWorkDate)
                .distinct()
                .count();
        int editedCount = (int) records.stream().filter(WorkProof::isEdited).count();
        int issueCount = (int) records.stream().filter(this::hasSpecialIssue).count();
        long totalWorkedMinutes = records.stream().mapToLong(WorkProof::workedMinutes).sum();

        List<WorkProofPdfSnapshot.WorkProofRecordItem> recordItems = records.stream()
                .map(record -> toRecordItem(record, locale))
                .toList();

        List<WorkProofPdfSnapshot.WorkProofAuditItem> auditItems = audits.stream()
                .map(audit -> toAuditItem(audit, locale))
                .toList();

        WorkProof latestRecord = records.get(0);
        return new WorkProofPdfSnapshot(
                buildMeta(request, yearMonth, zoneId, locale),
                buildStatementInfo(locale),
                new WorkProofPdfSnapshot.WorkerInfo(
                        nullToDash(request.getUser().getName()),
                        nullToDash(request.getUser().getEmail()),
                        nullToDash(request.getUser().getPhoneNumber())
                ),
                new WorkProofPdfSnapshot.WorkplaceInfo(
                        nullToDash(latestRecord.resolveWorkplaceName()),
                        nullToDash(latestRecord.resolveWorkplaceAddress())
                ),
                new WorkProofPdfSnapshot.PeriodInfo(
                        formatDate(startDate),
                        formatDate(endDate),
                        request.getMonth() == null ? formatYearMonth(startDate, endDate) : request.getMonth(),
                        formatPeriodLabel(startDate, endDate, locale)
                ),
                new WorkProofPdfSnapshot.SummaryInfo(
                        totalWorkDayCount,
                        editedCount,
                        issueCount,
                        totalWorkedMinutes,
                        formatMinutesLabel(totalWorkedMinutes, locale),
                        formatWorkDayCountLabel(totalWorkDayCount, locale),
                        formatIssueCountLabel(issueCount, locale)
                ),
                recordItems,
                auditItems
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

    private Workplace getAccessibleWorkplace(Long userId, Long workplaceId) {
        Workplace workplace = workplaceRepository.findById(workplaceId)
                .orElseThrow(() -> new ApiException(ErrorCode.WORKPLACE_NOT_FOUND));
        if (Objects.equals(workplace.getUser().getId(), userId) || hasActiveMembership(userId, workplace)) {
            return workplace;
        }
        throw new ApiException(ErrorCode.WORKPLACE_NOT_FOUND);
    }

    private boolean hasActiveMembership(Long userId, Workplace workplace) {
        if (workplace.getCompanyId() == null) {
            return false;
        }
        return !employmentMembershipRepository.findActiveWorkerMembershipByScope(
                userId,
                workplace.getCompanyId(),
                workplace.getId(),
                EmploymentMembershipStatus.ACTIVE,
                LocalDate.now()
        ).isEmpty();
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

    private WorkProofPdfSnapshot.StatementInfo buildStatementInfo(Locale locale) {
        return new WorkProofPdfSnapshot.StatementInfo(
                isKorean(locale) ? "근무 기록 문서" : "WorkProof Statement",
                isKorean(locale)
                        ? "선택한 기간의 출퇴근 기록과 변경 이력을 정리한 문서"
                        : "A statement of attendance records and change history for the selected period"
        );
    }

    private WorkProofPdfSnapshot.WorkProofRecordItem toRecordItem(WorkProof record, Locale locale) {
        long workedMinutes = record.workedMinutes();
        return new WorkProofPdfSnapshot.WorkProofRecordItem(
                record.getId(),
                formatDate(record.getWorkDate()),
                formatTime(record.resolveRecognizedClockInAt()),
                formatTime(record.resolveRecognizedClockOutAt()),
                workedMinutes,
                formatMinutesLabel(workedMinutes, locale),
                buildRemarks(record, locale)
        );
    }

    private WorkProofPdfSnapshot.WorkProofAuditItem toAuditItem(WorkProofAuditLog audit, Locale locale) {
        return new WorkProofPdfSnapshot.WorkProofAuditItem(
                audit.getId(),
                audit.getWorkProof().getId(),
                formatDate(audit.getWorkProof().getWorkDate()),
                formatDateTime(audit.getCreatedAt()),
                buildAuditChangeSummary(audit, locale),
                blankToDash(audit.getAfterEditReason())
        );
    }

    private Locale resolveLocale(Locale locale) {
        return locale == null ? DEFAULT_LOCALE : locale;
    }

    private ZoneId resolveZoneId(ZoneId zoneId) {
        return zoneId == null ? DEFAULT_ZONE_ID : zoneId;
    }

    private String formatMinutesLabel(long minutes, Locale locale) {
        long hours = minutes / 60;
        long remainMinutes = minutes % 60;
        return isKorean(locale)
                ? "%d시간 %02d분".formatted(hours, remainMinutes)
                : "%dh %02dm".formatted(hours, remainMinutes);
    }

    private String formatWorkDayCountLabel(int totalWorkDayCount, Locale locale) {
        return isKorean(locale)
                ? "%d일".formatted(totalWorkDayCount)
                : "%d days".formatted(totalWorkDayCount);
    }

    private String formatIssueCountLabel(int issueCount, Locale locale) {
        return isKorean(locale)
                ? "%d건".formatted(issueCount)
                : "%d issues".formatted(issueCount);
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

    private String formatPeriodLabel(LocalDate startDate, LocalDate endDate, Locale locale) {
        String range = "%s ~ %s".formatted(formatDate(startDate), formatDate(endDate));
        return isKorean(locale) ? "대상 기간: " + range : "Period: " + range;
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

    private String buildAuditChangeSummary(WorkProofAuditLog audit, Locale locale) {
        String timeSummary = isKorean(locale)
                ? "출근 %s→%s / 퇴근 %s→%s".formatted(
                formatTime(audit.getBeforeClockInAt()),
                formatTime(audit.getAfterClockInAt()),
                formatTime(audit.getBeforeClockOutAt()),
                formatTime(audit.getAfterClockOutAt())
        )
                : "In %s→%s / Out %s→%s".formatted(
                formatTime(audit.getBeforeClockInAt()),
                formatTime(audit.getAfterClockInAt()),
                formatTime(audit.getBeforeClockOutAt()),
                formatTime(audit.getAfterClockOutAt())
        );

        if (!isBlank(audit.getAfterEditReason())) {
            return isKorean(locale)
                    ? "%s / 사유: %s".formatted(timeSummary, audit.getAfterEditReason())
                    : "%s / Reason: %s".formatted(timeSummary, audit.getAfterEditReason());
        }

        return timeSummary;
    }

    private String buildRemarks(WorkProof record, Locale locale) {
        List<String> remarks = new ArrayList<>();
        if (record.isEdited()) {
            remarks.add(isKorean(locale) ? "수정 기록 있음" : "Edited");
        }
        if (record.isNeedsReview()) {
            remarks.add(isKorean(locale) ? "기록 확인 필요" : "Needs review");
        } else if (record.isClockOutOutsideAllowedRadius()) {
            remarks.add(isKorean(locale) ? "위치 확인 필요" : "Location check required");
        } else if (record.getClockOutAt() == null) {
            remarks.add(isKorean(locale) ? "퇴근 기록 확인 필요" : "Checkout missing");
        }
        if (record.getAttachmentCount() > 0) {
            remarks.add(isKorean(locale) ? "첨부 있음" : "Attachment included");
        }
        return remarks.isEmpty() ? "-" : String.join(" / ", remarks);
    }

    private boolean hasSpecialIssue(WorkProof record) {
        return record.isNeedsReview()
                || record.isClockOutOutsideAllowedRadius()
                || record.getClockOutAt() == null;
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
