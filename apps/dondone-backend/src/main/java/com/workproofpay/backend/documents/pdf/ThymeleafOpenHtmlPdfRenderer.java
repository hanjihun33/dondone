package com.workproofpay.backend.documents.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;

@Component
public class ThymeleafOpenHtmlPdfRenderer implements PdfRenderer {

    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final DateTimeFormatter FILE_NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

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
}
