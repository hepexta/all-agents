package com.hepexta.allagents.agents.pdf;

import com.hepexta.allagents.config.AppProperties;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Component
public class PdfTextExtractor {

    private final AppProperties properties;

    public PdfTextExtractor(AppProperties properties) {
        this.properties = properties;
    }

    public ExtractedText fromBase64(String base64) {
        return fromBytes(Base64.getDecoder().decode(base64));
    }

    public ExtractedText fromPath(String path) {
        try {
            Path allowedRoot = Path.of(properties.pdf().allowedDir()).toAbsolutePath().normalize();
            if (!Files.isDirectory(allowedRoot)) {
                Files.createDirectories(allowedRoot);
            }
            Path realRoot = allowedRoot.toRealPath();
            Path resolved = Path.of(path).toAbsolutePath().normalize().toRealPath();
            if (!resolved.startsWith(realRoot)) {
                throw new IllegalArgumentException("pdf path is outside the allowed directory: " + realRoot);
            }
            return fromBytes(Files.readAllBytes(resolved));
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read pdf at path: " + path, e);
        }
    }

    public ExtractedText fromBytes(byte[] bytes) {
        int maxBytes = properties.pdf().maxBytes();
        if (maxBytes > 0 && bytes.length > maxBytes) {
            throw new IllegalArgumentException("pdf too large: " + bytes.length + " bytes (max " + maxBytes + ")");
        }
        try (PDDocument document = Loader.loadPDF(bytes)) {
            int totalPages = document.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            int maxPages = properties.pdf().maxPages();
            if (maxPages > 0) {
                stripper.setEndPage(Math.min(totalPages, maxPages));
            }
            String text = stripper.getText(document).replace("\r\n", "\n");
            int maxChars = properties.pdf().maxChars();
            if (maxChars > 0 && text.length() > maxChars) {
                text = text.substring(0, maxChars);
            }
            return new ExtractedText(text, totalPages);
        } catch (IOException e) {
            throw new IllegalArgumentException("invalid pdf content", e);
        }
    }

    public record ExtractedText(String text, int pages) {
    }
}
