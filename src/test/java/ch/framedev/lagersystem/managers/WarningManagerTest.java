package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Warning;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarningManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/resolve/update/delete: warning CRUD works")
    void warningCrud_works() {
        WarningManager manager = WarningManager.getInstance();
        Warning warning = new Warning("Warnung 1", "Text", Warning.WarningType.LOW_STOCK, "31.03.2026", false, false);

        assertTrue(manager.insertWarning(warning));
        assertFalse(manager.hasNotWarning("Warnung 1"));
        assertFalse(manager.isResolved("Warnung 1"));
        assertFalse(manager.isDisplayed("Warnung 1"));

        assertTrue(manager.resolveWarning("Warnung 1"));
        assertTrue(manager.isResolved("Warnung 1"));

        warning.setMessage("Neu");
        warning.setDisplayed(true);
        warning.setResolved(true);
        assertTrue(manager.updateWarning(warning));

        Warning loaded = manager.getWarning("Warnung 1");
        assertEquals("Neu", loaded.getMessage());
        assertTrue(loaded.isDisplayed());
        assertTrue(loaded.isResolved());

        assertTrue(manager.deleteWarning("Warnung 1"));
        assertNull(manager.getWarning("Warnung 1"));
    }

    @Test
    @DisplayName("getAllWarnings: returns inserted warnings")
    void getAllWarnings_returnsInsertedWarnings() {
        WarningManager manager = WarningManager.getInstance();
        manager.insertWarning(new Warning("W1", "M1", Warning.WarningType.LOW_STOCK, "31.03.2026", false, false));
        manager.insertWarning(new Warning("W2", "M2", Warning.WarningType.CRITICAL_STOCK, "31.03.2026", true, true));

        List<Warning> warnings = manager.getAllWarnings();

        assertEquals(2, warnings.size());
        assertTrue(warnings.stream().anyMatch(w -> "W1".equals(w.getTitle())));
        assertTrue(warnings.stream().anyMatch(w -> "W2".equals(w.getTitle()) && w.isResolved() && w.isDisplayed()));
    }

    @Test
    @DisplayName("hasNotWarning: null title returns true")
    void hasNotWarning_nullTitle_returnsTrue() {
        WarningManager manager = WarningManager.getInstance();

        assertTrue(manager.hasNotWarning(null));
    }
}
