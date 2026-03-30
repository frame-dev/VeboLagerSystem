package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.utils.UnicodeSymbols;

final class SettingsCsvFeedbackService {

    private SettingsCsvFeedbackService() {
    }

    static boolean ensureCsvHasHeader(String headerLine) {
        if (headerLine != null) {
            return true;
        }
        SettingsDialogService.showError("Fehler", "Die CSV-Datei ist leer.");
        return false;
    }

    static void showImportResult(String entityLabel, int imported, int errors) {
        SettingsDialogService.showInfo(
                "Import Ergebnis",
                String.format("<html><b>%s-Import abgeschlossen</b><br/><br/>" +
                                UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                                (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                                "</html>",
                        entityLabel, imported, errors));
    }

    static void showImportError(String entityLabel, String errorMessage) {
        SettingsDialogService.showError(
                "Fehler",
                "Fehler beim Importieren der " + entityLabel + ": " + errorMessage);
    }

    static void showImportUnavailable(String title, String message) {
        SettingsDialogService.showInfo(title, message);
    }
}
