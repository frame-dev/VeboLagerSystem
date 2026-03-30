package ch.framedev.lagersystem.guis;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class SettingsProfileService {

    private SettingsProfileService() {
    }

    static void exportSettingsProfile(Component parent, Supplier<Properties> collector) {
        if (collector == null) {
            throw new IllegalArgumentException("collector must not be null");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Einstellungen exportieren");
        fileChooser.setSelectedFile(new File("vebo_settings.properties"));
        int choice = fileChooser.showSaveDialog(parent);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            Properties props = collector.get();
            props.store(out, "VEBO Lagersystem Einstellungen");
            SettingsDialogService.showInfo(
                    "Export erfolgreich",
                    "<html><b>Einstellungen exportiert!</b><br/><br/>" +
                            "Die Einstellungen wurden erfolgreich exportiert:<br/>" +
                            file.getAbsolutePath() + "</html>");
        } catch (IOException ex) {
            SettingsDialogService.showError(
                    "Export fehlgeschlagen",
                    "<html><b>Fehler beim Export!</b><br/><br/>" +
                            "Die Einstellungen konnten nicht exportiert werden:<br/>" +
                            ex.getMessage() + "</html>");
        }
    }

    static void importSettingsProfile(Component parent, Consumer<Properties> applier) {
        if (applier == null) {
            throw new IllegalArgumentException("applier must not be null");
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Einstellungen importieren");
        int choice = fileChooser.showOpenDialog(parent);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();
        try (FileInputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            applier.accept(props);
            SettingsDialogService.showInfo(
                    "Import erfolgreich",
                    "<html><b>Einstellungen importiert!</b><br/><br/>" +
                            "Die Einstellungen wurden erfolgreich importiert.<br/>" +
                            "Bitte speichern, um sie zu übernehmen.</html>");
        } catch (IOException ex) {
            SettingsDialogService.showError(
                    "Import fehlgeschlagen",
                    "<html><b>Fehler beim Import!</b><br/><br/>" +
                            "Die Einstellungen konnten nicht importiert werden:<br/>" +
                            ex.getMessage() + "</html>");
        }
    }

    static int parseIntProperty(Properties props, String key, int fallback) {
        if (props == null) {
            throw new IllegalArgumentException("props must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static String colorToSetting(Color color) {
        return color == null ? "" : toHex(color);
    }

    static Color parseColor(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String hex = value.trim();
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        if (hex.length() != 6) {
            return null;
        }
        try {
            int rgb = Integer.parseInt(hex, 16);
            return new Color(rgb);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }
}
