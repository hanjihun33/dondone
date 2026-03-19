package com.workproofpay.backend.documents.pdf;

import com.workproofpay.backend.documents.pdf.workproof.WorkProofPdfSnapshot;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class ThymeleafOpenHtmlPdfRendererTest {

    @Test
    void rendersWorkProofSnapshotToPdfBytes() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver());
        ThymeleafOpenHtmlPdfRenderer renderer = new ThymeleafOpenHtmlPdfRenderer(templateEngine);

        RenderedPdf renderedPdf = renderer.render("pdf/workproof-statement", sampleSnapshot());

        assertThat(renderedPdf.contentType()).isEqualTo("application/pdf");
        assertThat(renderedPdf.fileName()).endsWith(".pdf");
        assertThat(renderedPdf.sha256()).hasSize(64);
        assertThat(renderedPdf.bytes()).isNotEmpty();
        assertThat(new String(renderedPdf.bytes(), 0, Math.min(4, renderedPdf.bytes().length), StandardCharsets.ISO_8859_1))
                .startsWith("%PDF");
    }

    private ClassLoaderTemplateResolver templateResolver() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        return resolver;
    }

    private WorkProofPdfSnapshot sampleSnapshot() {
        return new WorkProofPdfSnapshot(
                new WorkProofPdfSnapshot.DocumentMeta("PROOF_PACK", "PP-202603-000001", "workproof-statement-v1", "2026-03-19 10:00:00", "Asia/Seoul", Locale.KOREA.toLanguageTag()),
                new WorkProofPdfSnapshot.WorkerInfo(1L, "Test User", "test@example.com"),
                new WorkProofPdfSnapshot.WorkplaceInfo(7L, "DunDone Factory", "Seoul", "Main Gate"),
                new WorkProofPdfSnapshot.ContractInfo("시급", "10,030원", "10,030원", 480, 10560, "2026-03-01", "현재"),
                new WorkProofPdfSnapshot.PeriodInfo("2026-03-01", "2026-03-31", "2026-03"),
                new WorkProofPdfSnapshot.SummaryInfo(2, 1, 1, 1, 2, 1081L, "18시간 01분"),
                List.of(
                        new WorkProofPdfSnapshot.WorkProofRecordItem(
                                100L, "2026-03-10", "09:00", "18:00", 540L, "9시간 00분",
                                "Main Gate", "Main Gate", "REFLECTED", "반영 완료", "success",
                                true, false, "-", "Correction", "memo", 2,
                                "2026-03-10 09:00:00", "2026-03-10 18:00:00"
                        ),
                        new WorkProofPdfSnapshot.WorkProofRecordItem(
                                101L, "2026-03-11", "09:02", "18:03", 541L, "9시간 01분",
                                "Main Gate", "Parking", "NEEDS_REVIEW", "검토 필요", "warning",
                                false, true, "반경 외 퇴근", "-", "-", 0,
                                "2026-03-11 09:02:00", "2026-03-11 18:03:00"
                        )
                ),
                List.of(new WorkProofPdfSnapshot.WorkProofAuditItem(
                        1L, 100L, "2026-03-12 10:00:00",
                        "09:00", "18:00", "09:10", "18:10",
                        "-", "Updated memo", "-", "Correction"
                )),
                List.of(
                        "검토 필요 상태의 기록이 포함되어 있어 정산 전 수동 확인이 필요합니다.",
                        "수정된 기록은 하단 수정 이력 섹션에서 변경 전후 내용을 함께 확인할 수 있습니다."
                )
        );
    }
}
