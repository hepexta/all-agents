package com.hepexta.allagents.agents.pdf;

import com.hepexta.allagents.support.PdfFixtures;
import com.hepexta.allagents.support.TestProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

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
    void missingPathThrows() {
        assertThrows(IllegalArgumentException.class, () -> extractor.fromPath("C:/definitely/not/here.pdf"));
    }
}
