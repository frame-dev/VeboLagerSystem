package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

final class SettingsMaintenanceService {

    private SettingsMaintenanceService() {
    }

    static void openCategorySettingsFile(Logger logger) {
        File file = new File(Main.getAppDataDir(), "categories.json");
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("Datei konnte nicht erstellt werden.");
                }
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("{}");
                }
            }
            Desktop.getDesktop().edit(file);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen der Kategorien-Datei: {}", e.getMessage());
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Öffnen der Kategorien-Datei:\n" + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Öffnen der Kategorien-Datei: " + e.getMessage());
        }
    }

    static void deleteOldLogs(Logger logger) {
        deleteOldLogs(logger, true);
    }

    static DeleteOldLogsResult deleteOldLogs(Logger logger, boolean showDialog) {
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager
                .getInstance();
        int deletedCount = logManager.deleteOldLogs(LogUtils.DEFAULT_LOG_RETENTION_DAYS);
        var fileCleanup = Main.logUtils.deleteLogsOlderThan(LogUtils.DEFAULT_LOG_RETENTION_DAYS);
        String message = String.format(
                "Es wurden %d Protokolle aus der Datenbank und %d Protokolldateien gelöscht, die älter als 30 Tage sind.",
                deletedCount, fileCleanup.deletedFileCount());
        if (fileCleanup.failedFileCount() > 0) {
            message += String.format("%n%nHinweis: %d Protokolldatei(en) konnten nicht gelöscht werden.",
                    fileCleanup.failedFileCount());
        }

        if (showDialog) {
            new MessageDialog()
                    .setTitle("Alte Protokolle gelöscht")
                    .setMessage(message)
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        }
        logger.info(
                "{}{}",
                message,
                showDialog ? "" : " (automatisch)");
        Main.logUtils.addLog(message + (showDialog ? "" : " (automatisch)"));
        return new DeleteOldLogsResult(deletedCount, fileCleanup.deletedFileCount(), fileCleanup.failedFileCount());
    }

    static void deleteAllLogs(Logger logger, Component parent) {
        java.io.File logsFolder = new java.io.File(Main.getAppDataDir(), "logs");
        int confirm = JOptionPane.showConfirmDialog(parent,
                "<html><b>Alle Protokolle wirklich löschen?</b><br/><br/>" +
                        "Diese Aktion löscht alle Protokolldateien im Protokolle-Ordner.<br/>" +
                        "Möchten Sie fortfahren?</html>",
                "Protokolle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            java.io.File[] logFiles = logsFolder.listFiles();
            if (logFiles != null) {
                for (java.io.File logFile : logFiles) {
                    if (logFile.isFile()) {
                        Files.delete(logFile.toPath());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Fehler beim Löschen der Protokolle: {}", e.getMessage());
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Löschen der Protokolle:\n" + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Löschen der Protokolle: " + e.getMessage());
            return;
        }

        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager
                .getInstance();
        if (logManager.clearAllLogs()) {
            logger.info("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
            Main.logUtils.addLog("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
            new MessageDialog()
                    .setTitle("Protokolle gelöscht")
                    .setMessage("Alle Protokolle wurden erfolgreich gelöscht.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } else {
            logger.error("Fehler beim Löschen der Protokolle aus der Datenbank.");
            Main.logUtils.addLog("Fehler beim Löschen der Protokolle aus der Datenbank.");
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Die Protokolldateien wurden gelöscht, aber die Datenbankprotokolle konnten nicht entfernt werden.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    static void openLogsFolder(Logger logger) {
        java.io.File logsDir = new java.io.File(Main.getAppDataDir(), "logs");
        try {
            Desktop.getDesktop().open(logsDir);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen des Protokolle-Ordners: {}", e.getMessage());
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Öffnen des Protokolle-Ordners:\n" + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Öffnen des Protokolle-Ordners: " + e.getMessage());
        }
    }

    static void openSettingsFolder(JFrame parent) {
        SettingsUtils.openSettingsFolder(parent);
    }

    record DeleteOldLogsResult(int deletedDatabaseLogs, int deletedFileLogs, int failedFileLogs) {
    }

    @SuppressWarnings("deprecation")
    static void deleteTable(Component parent, Logger logger, String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        try {
            DatabaseManager dbManager = Main.databaseManager;

            if (dbManager != null) {
                String dropTableSQL = "DROP TABLE IF EXISTS " + tableName + ";";
                boolean success = dbManager.executeUpdate(dropTableSQL);

                if (success) {
                    new MessageDialog()
                            .setTitle("Tabelle gelöscht")
                            .setMessage(String.format("<html><b>OK Tabelle erfolgreich gelöscht</b><br/><br/>" +
                                            "Die Tabelle '<b>%s</b>' wurde aus der Datenbank entfernt.<br/><br/>" +
                                            "<i>Hinweis: Die zugehörigen Daten sind permanent gelöscht.</i></html>",
                                    tableName))
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .display();

                    System.out.printf("[SettingsGUI] Tabelle '%s' wurde erfolgreich gelöscht%n", tableName);

                    int restart = new MessageDialog()
                            .setTitle("Neustart empfohlen")
                            .setMessage("<html>Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                    "um Inkonsistenzen zu vermeiden.<br/><br/>" +
                                    "Möchten Sie jetzt neu starten?</html>")
                            .setMessageType(JOptionPane.QUESTION_MESSAGE)
                            .setOptionType(JOptionPane.YES_NO_OPTION)
                            .displayWithOptions();

                    if (restart == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    new MessageDialog()
                            .setTitle("Fehler beim Löschen der Tabelle")
                            .setMessage(String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                            "Die Tabelle '<b>%s</b>' konnte nicht gelöscht werden.<br/>" +
                                            "Bitte überprüfen Sie die Logs für weitere Details.</html>",
                                    tableName))
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                    Main.logUtils.addLog(String.format(
                            "Fehler beim Löschen der Tabelle. Die Tabelle '%s' konnte nicht gelöscht werden.",
                            tableName));
                }
            } else {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Fehler: Datenbankverbindung nicht verfügbar.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
            }
        } catch (Exception e) {
            System.err.printf("[SettingsGUI] Fehler beim Löschen der Tabelle '%s': %s%n", tableName, e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Löschen der Tabelle '%s': %s", tableName, e.getMessage()));
            logger.error("Fehler beim Löschen der Tabelle '{}'", tableName, e);
            new MessageDialog()
                    .setTitle("Fehler beim Löschen der Tabelle")
                    .setMessage(String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                    "Tabelle: %s<br/>" +
                                    "Fehler: %s</html>",
                            tableName, e.getMessage()))
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    static void clearDatabase(Component parent, Logger logger) {
        int firstConfirm = new MessageDialog()
                .setTitle("Datenbank löschen - Bestätigung 1/2")
                .setMessage("<html><b>⚠ WARNUNG: Datenbank löschen</b><br/><br/>" +
                        "Möchten Sie wirklich <b>ALLE DATEN</b> aus der Datenbank löschen?<br/><br/>" +
                        "Dies umfasst:<br/>" +
                        "- Alle Artikel<br/>" +
                        "- Alle Lieferanten<br/>" +
                        "- Alle Bestellungen<br/>" +
                        "- Alle Kunden<br/>" +
                        "- Alle Abteilungen<br/>" +
                        "- Alle Benutzer<br/>" +
                        "- Alle Logs<br/>" +
                        "- Alle Benutzer</br>" +
                        "- Alle Notizen<br/>" +
                        "- Alle Warnungen<br/><br/>" +
                        "<b>Diese Aktion kann NICHT rückgängig gemacht werden!</b></html>")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();

        if (firstConfirm != JOptionPane.YES_OPTION) {
            return;
        }

        String confirmText = new MessageDialog()
                .setTitle("Datenbank löschen - Bestätigung 2/2")
                .setMessage("<html><b>Zweite Bestätigung erforderlich</b><br/><br/>" +
                        "Bitte geben Sie <b>LÖSCHEN</b> ein, um fortzufahren:</html>")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .displayWithStringInput();

        if (confirmText == null || !confirmText.trim().equalsIgnoreCase("LÖSCHEN")) {
            new MessageDialog()
                    .setTitle("Abgebrochen")
                    .setMessage("Vorgang abgebrochen. Die Datenbank wurde nicht gelöscht.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
            return;
        }

        try {
            DatabaseManager dbManager = Main.databaseManager;

            if (dbManager != null) {
                dbManager.clearDatabase();
                File file = new File(Main.getAppDataDir(), "own_use_list.txt");
                if (!file.delete()) {
                    System.out.println(
                            "[SettingsGUI] own_use_list.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                }
                File importQrcodesFile = new File(Main.getAppDataDir(), "imported_qrcodes.txt");
                if (!importQrcodesFile.delete()) {
                    System.out.println(
                            "[SettingsGUI] imported_qrcodes.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                }
                File importedItemsFile = new File(Main.getAppDataDir(), "imported_items.txt");
                if (!importedItemsFile.delete()) {
                    System.out.println(
                            "[SettingsGUI] imported_items.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                }

                new MessageDialog()
                        .setTitle("Erfolgreich")
                        .setMessage("<html><b>OK Datenbank erfolgreich gelöscht</b><br/><br/>" +
                                "Alle Daten wurden aus der Datenbank entfernt.<br/><br/>" +
                                "Das Programm sollte nun neu gestartet werden.</html>")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();

                System.out.println("[SettingsGUI] Datenbank wurde erfolgreich bereinigt");

                int restart = new MessageDialog()
                        .setTitle("Neustart empfohlen")
                        .setMessage("<html>Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                "um Inkonsistenzen zu vermeiden.<br/><br/>" +
                                "Möchten Sie jetzt neu starten?</html>")
                        .setMessageType(JOptionPane.QUESTION_MESSAGE)
                        .setOptionType(JOptionPane.YES_NO_OPTION)
                        .displayWithOptions();

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Fehler: Datenbankverbindung nicht verfügbar.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
                Main.logUtils.addLog("Fehler: Datenbankverbindung nicht verfügbar.");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Löschen der Datenbank: " + e.getMessage());
            logger.error("Fehler beim Löschen der Datenbank", e);
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("<html><b>Fehler beim Löschen der Datenbank</b><br/><br/>" +
                            "Fehler: " + e.getMessage() + "</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
        }
    }
}
