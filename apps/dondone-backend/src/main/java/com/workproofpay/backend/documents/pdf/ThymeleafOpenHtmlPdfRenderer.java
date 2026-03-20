package com.workproofpay.backend.documents.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Component
public class ThymeleafOpenHtmlPdfRenderer implements PdfRenderer {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PDF_FONT_FAMILY = "Noto Sans KR";
    private static final String PDF_FONT_RESOURCE_PATH = "fonts/NotoSansKR-VF.ttf";
    private static final List<SystemFontCandidate> SYSTEM_FONT_FALLBACKS = List.of(
            new SystemFontCandidate(Path.of("C:/Windows/Fonts/NotoSansKR-VF.ttf"), PDF_FONT_FAMILY),
            new SystemFontCandidate(Path.of("C:/Windows/Fonts/malgun.ttf"), "Malgun Gothic")
    );

    private final SpringTemplateEngine templateEngine;

    public ThymeleafOpenHtmlPdfRenderer(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public RenderedPdf render(String templateName, Object model) {
        Context context = new Context();
        if (model instanceof Map<?, ?> variables) {
            for (Map.Entry<?, ?> entry : variables.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    context.setVariable(key, entry.getValue());
                }
            }
        } else {
            context.setVariable("snapshot", model);
        }

        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            registerFonts(builder);
            builder.toStream(outputStream);
            builder.run();

            byte[] pdfBytes = outputStream.toByteArray();
            return new RenderedPdf(
                    pdfBytes,
                    buildFileName(templateName),
                    CONTENT_TYPE_PDF,
                    sha256(pdfBytes)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF for template " + templateName, e);
        }
    }

    private void registerFonts(PdfRendererBuilder builder) {
        if (registerBundledFont(builder)) {
            return;
        }

        if (registerSystemFallbackFonts(builder)) {
            return;
        }

        throw new IllegalStateException("No Korean-capable PDF font was found for OpenHTMLtoPDF rendering");
    }

    private boolean registerBundledFont(PdfRendererBuilder builder) {
        ClassPathResource resource = new ClassPathResource(PDF_FONT_RESOURCE_PATH);
        if (!resource.exists()) {
            return false;
        }

        builder.useFont(() -> openResourceStream(resource), PDF_FONT_FAMILY);
        return true;
    }

    private boolean registerSystemFallbackFonts(PdfRendererBuilder builder) {
        boolean registered = false;
        for (SystemFontCandidate candidate : SYSTEM_FONT_FALLBACKS) {
            if (Files.exists(candidate.path())) {
                builder.useFont(candidate.path().toFile(), candidate.familyName());
                registered = true;
            }
        }
        return registered;
    }

    private InputStream openResourceStream(ClassPathResource resource) {
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open bundled PDF font resource " + PDF_FONT_RESOURCE_PATH, e);
        }
    }

    private String buildFileName(String templateName) {
        String sanitizedTemplateName = templateName.replace('/', '-').replace('\\', '-');
        String timestamp = FILE_NAME_TIMESTAMP.format(LocalDateTime.now());
        return sanitizedTemplateName + "-" + timestamp + ".pdf";
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    private record SystemFontCandidate(Path path, String familyName) {
    }
}
