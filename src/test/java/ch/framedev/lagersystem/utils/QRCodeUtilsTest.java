package ch.framedev.lagersystem.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QRCodeUtilsTest {

    @Test
    @DisplayName("extractArticleNumberFromData: removes article label")
    void extractArticleNumberFromData_removesArticleLabel() {
        String data = "artikelNr:4711;name:Test;details:1 l";

        assertEquals("4711", QRCodeUtils.extractArticleNumberFromData(data));
    }

    @Test
    @DisplayName("buildQrImportFailureMessage: includes article number, id and QR value")
    void buildQrImportFailureMessage_includesDiagnosticValues() {
        String message = QRCodeUtils.buildQrImportFailureMessage(
                "4711",
                "artikelNr:4711;name:Problem",
                "QR-42",
                "Unbekannte Artikel-Nr.");

        assertTrue(message.contains("Unbekannte Artikel-Nr."));
        assertTrue(message.contains("Artikel-Nr.: 4711"));
        assertTrue(message.contains("QR-ID: QR-42"));
        assertTrue(message.contains("QR-Wert: artikelNr:4711;name:Problem"));
    }
}
