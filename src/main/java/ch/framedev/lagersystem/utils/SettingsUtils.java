package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static ch.framedev.lagersystem.guis.SettingsGUI.DEFAULT_FONT_STYLE;

public class SettingsUtils {

    public static void openSettingsFolder(JFrame frame) {
        File settingsDir = Main.getAppDataDir();
        try {
            Desktop.getDesktop().open(settingsDir);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame,
                    "Fehler beim Öffnen des Einstellungen-Ordners:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Öffnen des Einstellungen-Ordners: " + e.getMessage());
        }
    }

    /**
     * Creates a modern styled section panel with card-like appearance
     */
    public static JPanel createSectionPanel(String title, String description, List<JComponent> searchableSections) {
        RoundedPanel section = getRoundedPanel();
        section.putClientProperty("searchText", (title + " " + description).toLowerCase());
        searchableSections.add(section);

        // Header panel with better visual hierarchy
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        headerLeft.add(titleLabel);
        headerPanel.add(headerLeft, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerPanel.add(headerActions, BorderLayout.EAST);
        section.putClientProperty("headerActions", headerActions);

        // Description with better formatting
        JLabel descLabel = new JLabel("<html><div style='line-height: 1.8; color: #6c757d; font-weight: 300;'>" +
                description + "</div></html>");
        descLabel.setFont(getFontByName(Font.PLAIN, 13));
        descLabel.setForeground(ThemeManager.getTextSecondaryColor());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Add a subtle separator line
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setForeground(ThemeManager.getBorderColor());
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(headerPanel);
        section.add(Box.createVerticalStrut(12));
        section.add(descLabel);
        section.add(Box.createVerticalStrut(20));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));

        return section;
    }

    private static RoundedPanel getRoundedPanel() {
        RoundedPanel section = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        // Enhanced shadow and border with depth effect
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(0, 0, 0, 10), 3),
                        BorderFactory.createEmptyBorder(1, 1, 3, 1)
                ),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(28, 32, 28, 32)
                )
        ));

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    @SuppressWarnings("MagicConstant")
    public static Font getFontByName(int style, int fontSize) {
        String fontName = Main.settings.getProperty("font_style");
        if(fontName == null || fontName.trim().isEmpty()) {
            Font uiFont = UIManager.getFont("Label.font");
            fontName = uiFont == null ? DEFAULT_FONT_STYLE : uiFont.getFamily();
        } else if (isWindows() && ("Arial".equalsIgnoreCase(fontName) || "Arial Unicode MS".equalsIgnoreCase(fontName))) {
            Font uiFont = UIManager.getFont("Label.font");
            fontName = uiFont == null ? DEFAULT_FONT_STYLE : uiFont.getFamily();
        }
        Font font = new Font(fontName, style, fontSize);
        if ((isWindows() || isMac()) && font.canDisplayUpTo("\uD83D\uDCC1") != -1) {
            font = new Font("Dialog", style, fontSize);
        }
        return font;
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isMac() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase(Locale.ROOT).contains("mac");
    }

    /**
     * Rounded panel for modern card design with enhanced shadow
     */
    public static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        public RoundedPanel(Color bg, int radius) {
            this.backgroundColor = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw subtle shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, radius, radius);

            // Draw main card
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
