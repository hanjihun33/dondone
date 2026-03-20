package com.workproofpay.backend.documents.pdf;

import com.workproofpay.backend.documents.pdf.workproof.WorkProofPdfSnapshot;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
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
        assertThat(normalizeExtractedText(extractText(renderedPdf.bytes())))
                .contains("근무 기록 문서", "반영 완료", "검토 필요");
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
                new WorkProofPdfSnapshot.StatementInfo("근무 기록 문서", "선택한 기간의 출퇴근 기록과 변경 이력을 정리한 문서", "근로자 확인, 고용주 검토, 진정/상담 첨부용으로 활용할 수 있습니다."),
                new WorkProofPdfSnapshot.WorkerInfo(1L, "Test User", "test@example.com"),
                new WorkProofPdfSnapshot.WorkplaceInfo(7L, "DunDone Factory", "Seoul", "Main Gate"),
                new WorkProofPdfSnapshot.ContractInfo("시급", "10,030원", "10,030원", 480, 10560, "2026-03-01", "현재"),
                new WorkProofPdfSnapshot.PeriodInfo("2026-03-01", "2026-03-31", "2026-03", "대상 기간: 2026-03-01 ~ 2026-03-31"),
                new WorkProofPdfSnapshot.SummaryInfo(2, 2, 1, 1, 1, 2, 1081L, "18시간 01분", "2일"),
                List.of(
                        new WorkProofPdfSnapshot.WorkProofRecordItem(
                                100L, "2026-03-10", "09:00", "18:00", 540L, "9시간 00분",
                                "Main Gate", "Main Gate", "REFLECTED", "반영 완료", "success",
                                true, false, "-", "출근 Main Gate / 퇴근 Main Gate", "Correction", "memo", "사유: Correction / 메모: memo", 2,
                                "2026-03-10 09:00:00", "2026-03-10 18:00:00"
                        ),
                        new WorkProofPdfSnapshot.WorkProofRecordItem(
                                101L, "2026-03-11", "09:02", "18:03", 541L, "9시간 01분",
                                "Main Gate", "Parking", "NEEDS_REVIEW", "검토 필요", "warning",
                                false, true, "반경 외 퇴근", "출근 Main Gate / 퇴근 Parking", "-", "-", "-", 0,
                                "2026-03-11 09:02:00", "2026-03-11 18:03:00"
                        )
                ),
                List.of(new WorkProofPdfSnapshot.WorkProofAuditItem(
                        1L, 100L, "2026-03-12 10:00:00",
                        "09:00", "18:00", "09:10", "18:10",
                        "-", "Updated memo", "-", "Correction",
                        "출근 09:00→09:10 / 퇴근 18:00→18:10 / 사유: Correction"
                )),
                List.of(
                        "검토 필요 상태의 기록이 포함되어 있어 정산 전 수동 확인이 필요합니다.",
                        "수정된 기록은 하단 수정 이력 섹션에서 변경 전후 내용을 함께 확인할 수 있습니다."
                )
        );
    }

    private String extractText(byte[] pdfBytes) {
        try (PDDocument document = PDDocument.load(pdfBytes)) {
            return new PDFTextStripper().getText(document);
        } catch (Exception e) {
            throw new AssertionError("Failed to extract PDF text for verification", e);
        }
    }

    private String normalizeExtractedText(String text) {
        return text
                .replace('\u00A0', ' ')
                .replace('\u2011', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-');
    }
}
