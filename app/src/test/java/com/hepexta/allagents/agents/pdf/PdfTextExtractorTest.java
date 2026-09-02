package com.hepexta.allagents.agents.pdf;

import com.hepexta.allagents.support.PdfFixtures;
import com.hepexta.allagents.support.TestProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

    @TempDir
    Path tempDir;

    private final PdfTextExtractor extractor = new PdfTextExtractor(TestProperties.of(10, 1000));

    @Test
    void extractsTextAndPageCountFromBytes() {
        var extracted = extractor.fromBytes(PdfFixtures.createPdf("Hello PDF world"));
        assertTrue(extracted.text().contains("Hello PDF world"));
        assertEquals(1, extracted.pages());
    }

    @Test
    void extractsFromBase64() {
        var extracted = extractor.fromBase64(PdfFixtures.createPdfBase64("Base64 content"));
        assertTrue(extracted.text().contains("Base64 content"));
    }

    @Test
    void truncatesTextToMaxChars() {
        PdfTextExtractor truncating = new PdfTextExtractor(TestProperties.of(10, 10));
        var extracted = truncating.fromBytes(PdfFixtures.createPdf("This is a longer text than ten characters"));
        assertEquals(10, extracted.text().length());
    }

    @Test
    void invalidBytesThrow() {
        assertThrows(IllegalArgumentException.class, () -> extractor.fromBytes(new byte[]{1, 2, 3}));
    }

    @Test
    void oversizedPdfIsRejectedBeforeParsing() {
        PdfTextExtractor small = new PdfTextExtractor(TestProperties.of(10, 1000, 10, tempDir.toString()));
        assertThrows(IllegalArgumentException.class, () -> small.fromBytes(PdfFixtures.createPdf("Hello PDF world")));
    }

    @Test
    void missingPathThrowsAndCreatesAllowedDirLazily() {
        PdfTextExtractor withAllowedDir = new PdfTextExtractor(
                TestProperties.of(10, 1000, Integer.MAX_VALUE, tempDir.resolve("not-yet-created").toString()));
        assertThrows(IllegalArgumentException.class, () -> withAllowedDir.fromPath("C:/definitely/not/here.pdf"));
        assertTrue(Files.isDirectory(tempDir.resolve("not-yet-created")));
    }

    @Test
    void extractsFromPathInsideAllowedDir() throws Exception {
        Path allowedDir = tempDir.resolve("pdfs");
        Files.createDirectories(allowedDir);
        Path pdf = allowedDir.resolve("invoice.pdf");
        Files.write(pdf, PdfFixtures.createPdf("Allowed dir content"));

        PdfTextExtractor withAllowedDir = new PdfTextExtractor(
                TestProperties.of(10, 1000, Integer.MAX_VALUE, allowedDir.toString()));
        var extracted = withAllowedDir.fromPath(pdf.toString());
        assertTrue(extracted.text().contains("Allowed dir content"));
    }

    @Test
    void pathOutsideAllowedDirIsRejected() throws Exception {
        Path allowedDir = tempDir.resolve("allowed");
        Files.createDirectories(allowedDir);
        Path outside = tempDir.resolve("outside.pdf");
        Files.write(outside, PdfFixtures.createPdf("Secret content"));

        PdfTextExtractor withAllowedDir = new PdfTextExtractor(
                TestProperties.of(10, 1000, Integer.MAX_VALUE, allowedDir.toString()));
        assertThrows(IllegalArgumentException.class, () -> withAllowedDir.fromPath(outside.toString()));
    }
}
