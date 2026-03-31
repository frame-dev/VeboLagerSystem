package ch.framedev.lagersystem.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningTest {

    @Test
    @DisplayName("constructor: stores all warning fields")
    void constructor_storesAllFields() {
        Warning warning = new Warning(
                "Kritisch",
                "Bestand zu niedrig",
                Warning.WarningType.CRITICAL_STOCK,
                "31.03.2026",
                false,
                true);

        assertEquals("Kritisch", warning.getTitle());
        assertEquals("Bestand zu niedrig", warning.getMessage());
        assertEquals(Warning.WarningType.CRITICAL_STOCK, warning.getType());
        assertEquals("31.03.2026", warning.getDate());
        assertFalse(warning.isResolved());
        assertTrue(warning.isDisplayed());
    }

    @Test
    @DisplayName("setters: update all warning fields")
    void setters_updateAllFields() {
        Warning warning = new Warning("Alt", "Alt", Warning.WarningType.OTHER, "Alt", false, false);

        warning.setTitle("Neu");
        warning.setMessage("Neue Nachricht");
        warning.setType(Warning.WarningType.LOW_STOCK);
        warning.setDate("01.04.2026");
        warning.setResolved(true);
        warning.setDisplayed(true);

        assertEquals("Neu", warning.getTitle());
        assertEquals("Neue Nachricht", warning.getMessage());
        assertEquals(Warning.WarningType.LOW_STOCK, warning.getType());
        assertEquals("01.04.2026", warning.getDate());
        assertTrue(warning.isResolved());
        assertTrue(warning.isDisplayed());
    }

    @Test
    @DisplayName("WarningType: exposes localized display names")
    void warningType_exposesDisplayNames() {
        assertEquals("Mindest Lagerbestand", Warning.WarningType.LOW_STOCK.getDisplayName());
        assertEquals("Kritischer Lagerbestand", Warning.WarningType.CRITICAL_STOCK.getDisplayName());
        assertEquals("Bestellung erforderlich", Warning.WarningType.ORDER_NEEDED.getDisplayName());
        assertEquals("Sonstiges", Warning.WarningType.OTHER.getDisplayName());
    }

    @Test
    @DisplayName("getQRCodeData: serializes all fields as labeled payload")
    void getQRCodeData_serializesAllFields() {
        Warning warning = new Warning(
                "Kritisch",
                "Bestand zu niedrig",
                Warning.WarningType.CRITICAL_STOCK,
                "31.03.2026",
                true,
                false);

        assertEquals(
                "title:Kritisch;message:Bestand zu niedrig;type:CRITICAL_STOCK;date:31.03.2026;isResolved:true;isDisplayed:false",
                warning.getQRCodeData());
    }
}
