package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.SchedulerManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

final class SettingsRuntimeService {

    private SettingsRuntimeService() {
    }

    static Properties readSettingsSnapshot() {
        if (Main.settings == null) {
            return null;
        }

        Properties props = new Properties();
        for (SettingsGUI.Variable variable : SettingsGUI.Variable.values()) {
            String key = variable.getValue();
            String value = Main.settings.getProperty(key);
            if (value != null) {
                props.setProperty(key, value);
            }
        }
        return props;
    }

    static void persistSettings(Properties props) {
        if (Main.settings == null || props == null) {
            return;
        }

        for (String key : props.stringPropertyNames()) {
            Main.settings.setProperty(key, props.getProperty(key));
        }
        Main.settings.save();
    }

    static void applyRuntimeSettings(Logger logger,
                                     int interval,
                                     boolean enableWarnings,
                                     int warningInterval,
                                     boolean enableAutoCheck,
                                     boolean darkMode,
                                     boolean enableQrImport,
                                     int qrCodeImportInterval) {
        try {
            boolean currentDarkMode = ThemeManager.isDarkMode();
            if (currentDarkMode != darkMode) {
                ThemeManager.setDarkMode(darkMode);
                System.out.println("[SettingsGUI] Theme geaendert zu: " + (darkMode ? "Dark Mode" : "Light Mode"));

                int restart = new MessageDialog()
                        .setTitle("Neustart empfohlen")
                        .setMessage("<html>Das Theme wurde geändert.<br/><br/>" +
                                "Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                "damit das Theme vollständig angewendet wird.<br/><br/>" +
                                "Möchten Sie jetzt neu starten?</html>")
                        .setMessageType(JOptionPane.QUESTION_MESSAGE)
                        .setOptionType(JOptionPane.YES_NO_OPTION)
                        .displayWithOptions();

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                    return;
                }
            }

            SchedulerManager scheduler = SchedulerManager.getInstance();
            scheduler.shutdown();

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (enableAutoCheck) {
                scheduler.startScheduledStockCheck(interval, TimeUnit.MINUTES);
                System.out.println("[SettingsGUI] Automatische Lagerbestandspruefung neu gestartet (Intervall: "
                        + interval + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatische Lagerbestandspruefung deaktiviert");
            }

            if (enableWarnings) {
                scheduler.startWarningDisplay(warningInterval, TimeUnit.HOURS);
                System.out.println("[SettingsGUI] Automatische Warnanzeige aktiviert (Intervall: " + warningInterval
                        + " Stunde(n))");
            } else {
                System.out.println("[SettingsGUI] Automatische Warnanzeige deaktiviert");
            }

            if (enableQrImport) {
                scheduler.startAutoImportQrCodes(qrCodeImportInterval, TimeUnit.MINUTES);
                System.out.println("[SettingsGUI] Automatischer QR-Code Import aktiviert (Intervall: "
                        + qrCodeImportInterval + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatischer QR-Code Import deaktiviert");
            }

        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Anwenden der Einstellungen: " + e.getMessage());
            logger.error("Fehler beim Anwenden der Einstellungen", e);
            Main.logUtils.addLog("Fehler beim Anwenden der Einstellungen: " + e.getMessage());
        }
    }
}
