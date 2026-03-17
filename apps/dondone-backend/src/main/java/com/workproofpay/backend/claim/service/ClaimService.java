package com.workproofpay.backend.claim.service;

import com.workproofpay.backend.claim.api.dto.response.ClaimPreparationDetailResponse;
import com.workproofpay.backend.claim.api.dto.request.CreateClaimPreparationRequest;
import com.workproofpay.backend.claim.api.dto.response.ClaimPreparationResponse;
import com.workproofpay.backend.claim.model.ClaimPreparation;
import com.workproofpay.backend.claim.model.ClaimPreparationTone;
import com.workproofpay.backend.claim.repo.ClaimPreparationRepository;
import com.workproofpay.backend.documents.model.DocumentGenerationRequest;
import com.workproofpay.backend.documents.model.DocumentType;
import com.workproofpay.backend.documents.repo.DocumentGenerationRequestRepository;
import com.workproofpay.backend.shared.exception.ApiException;
import com.workproofpay.backend.shared.exception.ErrorCode;
import com.workproofpay.backend.wage.model.WageVerification;
import com.workproofpay.backend.wage.model.WageVerificationStatus;
import com.workproofpay.backend.wage.service.WageVerificationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Claim preparation은 verification snapshot을 사람 친화적인 준비 데이터로 재조합한다.
 */
@Service
@RequiredArgsConstructor
public class ClaimService {

    private static final String CHECKLIST_VERIFY_DEPOSIT = "VERIFY_DEPOSIT";
    private static final String CHECKLIST_REQUEST_EMPLOYER_CONFIRMATION = "REQUEST_EMPLOYER_CONFIRMATION";
    private static final String CHECKLIST_ATTACH_CLAIM_KIT = "ATTACH_CLAIM_KIT_IF_NEEDED";

    private final ClaimPreparationRepository claimPreparationRepository;
    private final DocumentGenerationRequestRepository documentGenerationRequestRepository;
    private final WageVerificationQueryService wageVerificationQueryService;

    /**
     * Claim preparation은 verification snapshot을 읽어
     * worker가 바로 복사/공유할 수 있는 summary와 checklist를 만든다.
     */
    @Transactional
    public ClaimPreparationResponse createPreparation(Long userId, CreateClaimPreparationRequest request) {
        WageVerification verification = wageVerificationQueryService.getOwnedVerification(userId, request.wageVerificationId());
        DocumentGenerationRequest claimKit = loadClaimKit(userId, verification.getId(), request.claimKitDocumentId());

        ClaimPreparation saved = claimPreparationRepository.save(ClaimPreparation.ready(
                verification.getUser(),
                verification.getId(),
                request.claimKitDocumentId(),
                request.locale(),
                request.tone(),
                buildSummaryText(verification, request.locale(), request.tone())
        ));

        return ClaimPreparationResponse.from(
                saved,
                buildChecklist(verification, claimKit),
                buildSuggestedRoutes(request.locale()),
                buildRelatedDocuments(claimKit)
        );
    }

    @Transactional(readOnly = true)
    public ClaimPreparationDetailResponse getPreparation(Long userId, Long preparationId) {
        ClaimPreparation preparation = claimPreparationRepository.findByIdAndUserId(preparationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.CLAIM_PREPARATION_NOT_FOUND));

        WageVerification verification = wageVerificationQueryService.getOwnedVerification(userId, preparation.getWageVerificationId());
        DocumentGenerationRequest claimKit = loadClaimKit(userId, verification.getId(), preparation.getClaimKitDocumentId());

        return ClaimPreparationDetailResponse.from(
                preparation,
                buildChecklist(verification, claimKit),
                buildSuggestedRoutes(preparation.getLocale()),
                buildRelatedDocuments(claimKit)
        );
    }

    /**
     * claim kit를 선택 입력으로 두되, 다른 verification 문맥의 문서는 섞이지 않게 막는다.
     */
    private DocumentGenerationRequest loadClaimKit(Long userId, Long verificationId, Long claimKitDocumentId) {
        if (claimKitDocumentId == null) {
            return null;
        }

        return documentGenerationRequestRepository.findByIdAndUserIdAndDocumentType(
                        claimKitDocumentId,
                        userId,
                        DocumentType.CLAIM_KIT
                )
                .filter(document -> verificationId.equals(document.getWageVerificationId()))
                .orElseThrow(() -> new ApiException(ErrorCode.CLAIM_KIT_NOT_FOUND));
    }

    /**
     * P0에서는 verification snapshot 숫자만 재사용하고, 문구 톤만 locale/tone에 맞춰 바꾼다.
     */
    private String buildSummaryText(WageVerification verification, String localeTag, ClaimPreparationTone tone) {
        Locale locale = Locale.forLanguageTag(localeTag);
        if (Locale.ROOT.equals(locale)) {
            locale = Locale.KOREA;
        }

        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        String estimated = numberFormat.format(verification.getEstimatedTotal());
        String actual = numberFormat.format(verification.getActualDepositAmount());
        String difference = numberFormat.format(verification.getDifferenceAmount());
        boolean korean = localeTag.toLowerCase(Locale.ROOT).startsWith("ko");

        if (korean) {
            return switch (tone) {
                case POLITE ->
                        "%s 급여 확인 결과를 기준으로 참고용 예상 금액 %s원과 실제 확인 금액 %s원 사이에 %s원 차이가 있습니다. 연장·야간 반영 여부와 공제 내역을 먼저 확인해 주세요."
                                .formatted(verification.getMonth(), estimated, actual, difference);
                case SHORT ->
                        "%s 기준 참고용 예상 %s원, 실제 확인 %s원, 차이 %s원입니다. 공제와 연장·야간 반영 여부를 먼저 확인하세요."
                                .formatted(verification.getMonth(), estimated, actual, difference);
                case DEFAULT ->
                        "%s 급여 확인 결과, 참고용 예상 금액 %s원 대비 실제 확인 금액은 %s원으로 %s원 차이가 있습니다. 회사와의 1차 확인 전에 공제 내역과 근무 반영 근거를 다시 살펴보세요."
                                .formatted(verification.getMonth(), estimated, actual, difference);
            };
        }

        return switch (tone) {
            case POLITE ->
                    "For %s, there is a %s KRW difference between the reference estimate of %s KRW and the worker-confirmed amount of %s KRW. Please review deductions and overtime or night work first."
                            .formatted(verification.getMonth(), difference, estimated, actual);
            case SHORT ->
                    "For %s, the reference estimate is %s KRW, the confirmed amount is %s KRW, and the difference is %s KRW."
                            .formatted(verification.getMonth(), estimated, actual, difference);
            case DEFAULT ->
                    "For %s, the worker-confirmed payment is %s KRW while the reference estimate is %s KRW, leaving a %s KRW difference. Review deductions and reflected work records before the next step."
                            .formatted(verification.getMonth(), actual, estimated, difference);
        };
    }

    private List<ClaimPreparationResponse.ChecklistItem> buildChecklist(WageVerification verification,
                                                                        DocumentGenerationRequest claimKit) {
        List<ClaimPreparationResponse.ChecklistItem> items = new ArrayList<>();
        items.add(new ClaimPreparationResponse.ChecklistItem(
                CHECKLIST_VERIFY_DEPOSIT,
                "실제 입금액과 메모를 다시 확인하기",
                true
        ));
        if (verification.getStatus() == WageVerificationStatus.CHECK_REQUIRED) {
            items.add(new ClaimPreparationResponse.ChecklistItem(
                    CHECKLIST_REQUEST_EMPLOYER_CONFIRMATION,
                    "회사와 1차 확인을 진행하기",
                    true
            ));
        }
        if (claimKit == null) {
            items.add(new ClaimPreparationResponse.ChecklistItem(
                    CHECKLIST_ATTACH_CLAIM_KIT,
                    "필요하면 Claim Kit를 추가로 준비하기",
                    false
            ));
        }
        return List.copyOf(items);
    }

    private List<ClaimPreparationResponse.SuggestedRoute> buildSuggestedRoutes(String localeTag) {
        boolean korean = localeTag.toLowerCase(Locale.ROOT).startsWith("ko");
        if (korean) {
            return List.of(
                    new ClaimPreparationResponse.SuggestedRoute(
                            "ONLINE",
                            "고용노동부 온라인 민원 안내",
                            "제출 전 필요한 기본 안내와 민원 경로를 확인합니다.",
                            null,
                            "https://www.moel.go.kr"
                    ),
                    new ClaimPreparationResponse.SuggestedRoute(
                            "PHONE",
                            "고용노동 상담센터 1350",
                            "언어 지원 가능 여부를 먼저 확인하고 상담을 준비합니다.",
                            "1350",
                            null
                    ),
                    new ClaimPreparationResponse.SuggestedRoute(
                            "VISIT",
                            "가까운 고용노동관서 방문",
                            "문서와 메모를 들고 직접 상담 또는 접수를 준비합니다.",
                            null,
                            null
                    )
            );
        }

        return List.of(
                new ClaimPreparationResponse.SuggestedRoute(
                        "ONLINE",
                        "Labor support website",
                        "Review the basic filing guidance before sharing your documents.",
                        null,
                        "https://www.moel.go.kr"
                ),
                new ClaimPreparationResponse.SuggestedRoute(
                        "PHONE",
                        "Labor hotline 1350",
                        "Call first to confirm the available language support.",
                        "1350",
                        null
                ),
                new ClaimPreparationResponse.SuggestedRoute(
                        "VISIT",
                        "Visit a local labor office",
                        "Bring your notes and related documents for an in-person consultation.",
                        null,
                        null
                )
        );
    }

    private List<ClaimPreparationResponse.RelatedDocument> buildRelatedDocuments(DocumentGenerationRequest claimKit) {
        if (claimKit == null) {
            return List.of();
        }

        return List.of(new ClaimPreparationResponse.RelatedDocument(
                claimKit.getId(),
                claimKit.getDocumentType(),
                claimKit.getStatus()
        ));
    }
}
