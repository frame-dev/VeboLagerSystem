package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.UpdateManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

final class SettingsUpdateService {

    private SettingsUpdateService() {
    }

    static void checkForUpdates(SettingsGUI parent, Logger logger, String channel) {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            UpdateManager.ReleaseChannel releaseChannel = switch (channel.toLowerCase()) {
                case "beta" -> UpdateManager.ReleaseChannel.BETA;
                case "alpha" -> UpdateManager.ReleaseChannel.ALPHA;
                case "testing" -> UpdateManager.ReleaseChannel.TESTING;
                default -> UpdateManager.ReleaseChannel.STABLE;
            };

            String channelDisplay = switch (channel.toLowerCase()) {
                case "beta" -> "Beta";
                case "alpha" -> "Alpha";
                case "testing" -> "Testing";
                default -> "Stable";
            };

            JDialog progressDialog = new JDialog(parent, "Nach Updates suchen...", true);
            progressDialog.setLayout(new BorderLayout());
            progressDialog.setSize(400, 150);
            progressDialog.setLocationRelativeTo(parent);
            progressDialog.setUndecorated(true);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.setResizable(false);

            JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
            progressPanel.setBackground(ThemeManager.getCardBackgroundColor());
            progressPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)));

            JLabel progressLabel = new JLabel(
                    "<html><center>Prüfe auf " + channelDisplay + "-Updates...<br/>Bitte warten...</center></html>");
            progressLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            progressLabel.setForeground(ThemeManager.getTextPrimaryColor());
            progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

            progressPanel.add(progressLabel, BorderLayout.CENTER);
            progressDialog.add(progressPanel);

            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return updateManager.getLatestVersion(releaseChannel);
                }

                @Override
                protected void done() {
                    try {
                        String latestVersion = get();
                        handleUpdateResult(parent, logger, latestVersion, channel);
                    } catch (Exception e) {
                        logger.error("Fehler beim Prüfen auf Updates: {}", e.getMessage(), e);
                        showUpdateError(e.getMessage());
                    } finally {
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                    }
                }
            };

            worker.execute();
            progressDialog.setVisible(true);

        } catch (Exception e) {
            logger.error("Fehler beim Starten der Update-Prüfung: {}", e.getMessage(), e);
            showUpdateError(e.getMessage());
            Main.logUtils.addLog("Fehler beim Starten der Update-Prüfung: " + e.getMessage());
        }
    }

    private static void handleUpdateResult(SettingsGUI parent, Logger logger, String latestVersion, String channel) {
        if (latestVersion == null) {
            new MessageDialog()
                    .setTitle("Update-Prüfung")
                    .setMessage("<html><b>Keine Update-Informationen verfügbar</b><br/><br/>" +
                            "Die Update-Informationen konnten nicht abgerufen werden.<br/>" +
                            "Bitte überprüfen Sie Ihre Internetverbindung.</html>")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        String currentVersion = Main.VERSION;
        UpdateManager updateManager = UpdateManager.getInstance();

        if (updateManager.isUpdateAvailable(currentVersion, latestVersion)) {
            showUpdateAvailableDialog(logger, latestVersion, currentVersion, channel);
        } else {
            showNoUpdateDialog(currentVersion, channel);
        }
    }

    private static void showUpdateAvailableDialog(Logger logger, String latestVersion, String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        String downloadUrl = getDownloadUrl(channel);

        Object[] options = { "Download-Seite öffnen", "Später" };
        int result = new MessageDialog()
                .setTitle("Update verfügbar")
                .setMessage("<html><b>" + UnicodeSymbols.DOWNLOAD + " Update verfügbar!</b><br/><br/>" +
                        "Eine neue Version ist verfügbar:<br/><br/>" +
                        "Aktuelle Version: <b>" + currentVersion + "</b><br/>" +
                        "Neue Version: <b>" + latestVersion + channelDisplay + "</b><br/><br/>" +
                        "Möchten Sie die Download-Seite öffnen?</html>")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .setOptions(options)
                .displayWithOptions();

        if (result == 0) {
            openDownloadPage(logger, downloadUrl);
        }
    }

    private static void showNoUpdateDialog(String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        new MessageDialog()
                .setTitle("Keine Updates verfügbar")
                .setMessage("<html><b>" + UnicodeSymbols.CHECKMARK + " Keine Updates verfügbar</b><br/><br/>" +
                        "Sie verwenden bereits die neueste Version" + channelDisplay + ":<br/>" +
                        "<b>Version " + currentVersion + "</b></html>")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .display();
    }

    private static void showUpdateError(String errorMessage) {
        if (errorMessage == null) {
            throw new IllegalArgumentException("errorMessage must not be null");
        }
        new MessageDialog()
                .setTitle("Fehler bei Update-Prüfung")
                .setMessage("<html><b>" + UnicodeSymbols.WARNING + " Fehler bei Update-Prüfung</b><br/><br/>" +
                        "Die Update-Prüfung ist fehlgeschlagen:<br/>" +
                        errorMessage + "</html>")
                .setMessageType(JOptionPane.ERROR_MESSAGE)
                .display();
        Main.logUtils.addLog("Fehler bei Update-Prüfung");
    }

    private static String getDownloadUrl(String channel) {
        if (channel == null) {
            throw new IllegalArgumentException("channel must not be null");
        }
        return switch (channel.toLowerCase()) {
            case "beta" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=beta";
            case "alpha" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=alpha";
            case "testing" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=testing";
            default -> "https://github.com/frame-dev/VeboLagerSystem/releases/latest";
        };
    }

    private static void openDownloadPage(Logger logger, String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                logger.info("Download-Seite geöffnet: {}", url);
            } else {
                new MessageDialog()
                        .setTitle("Browser nicht unterstützt")
                        .setMessage("<html><b>Ihr System unterstützt das automatische Öffnen von Browsern nicht.</b><br/><br/>" +
                                "Bitte kopieren Sie den folgenden Link und öffnen Sie ihn manuell in Ihrem Browser:<br/><br/>" +
                                "<a href='" + url + "'>" + url + "</a></html>")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
            }
        } catch (Exception e) {
            logger.error("Fehler beim Öffnen der Download-Seite: {}", e.getMessage(), e);
            new MessageDialog()
                    .setTitle("Fehler beim Öffnen des Browsers")
                    .setMessage("<html><b>Fehler beim Öffnen des Browsers</b><br/><br/>" +
                            "Bitte öffnen Sie den folgenden Link manuell:<br/>" +
                            url + "</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }
}
