package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;

import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static ch.framedev.lagersystem.guis.SettingsGUI.DEFAULT_FONT_STYLE;

/**
 * Utility helper class for building and managing UI-related settings components.
 * <p>
 * This class centralizes reusable logic for:
 * <ul>
 *   <li>Opening configuration directories</li>
 *   <li>Creating styled settings sections</li>
 *   <li>Resolving application fonts in a platform-safe way</li>
 *   <li>Providing reusable rounded card panels</li>
 * </ul>
 * All methods are static and the class is not intended to be instantiated.
 */
public class SettingsUtils {

    private SettingsUtils() {
        // Utility class
    }

    /**
     * Opens the application settings directory in the system file explorer.
     * <p>
     * The directory is resolved via {@link Main#getAppDataDir()} and opened using
     * {@link java.awt.Desktop}. If the folder cannot be opened, an error dialog
     * is shown to the user and the failure is written to the application log.
     *
     * @param frame the parent frame used for positioning the error dialog
     */
    public static void openSettingsFolder(JFrame frame) {
        File settingsDir = Main.getAppDataDir();

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            new MessageDialog()
                    .setTitle("Nicht unterstützt")
                    .setMessage("Öffnen des Einstellungen-Ordners wird von diesem System nicht unterstützt.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Öffnen des Einstellungen-Ordners: Desktop OPEN nicht unterstützt");
            return;
        }

        try {
            Desktop.getDesktop().open(settingsDir);
        } catch (IOException e) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Öffnen des Einstellungen-Ordners:\n" + e.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Öffnen des Einstellungen-Ordners: " + e.getMessage());
        }
    }

    /**
     * Creates a styled settings section panel with a modern card-like appearance.
     * <p>
     * The panel includes a title, description, separator line and a container
     * for optional header actions. It also registers the panel in the supplied
     * searchable component list and stores searchable text as a client property.
     *
     * @param title               the section title displayed at the top of the card
     * @param description         a descriptive text explaining the purpose of the section
     * @param searchableSections  list used by the settings search feature to filter panels
     * @return a fully styled section panel ready to be populated with settings controls
     */
    public static JPanel createSectionPanel(String title, String description, List<JComponent> searchableSections) {
        String safeTitle = title == null ? "" : title;
        String safeDescription = description == null ? "" : description;

        RoundedPanel section = getRoundedPanel();
        section.putClientProperty("searchText", (safeTitle + " " + safeDescription).toLowerCase());
        if (searchableSections != null) searchableSections.add(section);

        // Header panel with better visual hierarchy
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerLeft.setOpaque(false);
        JLabel titleLabel = new JLabel(safeTitle);
        titleLabel.setFont(getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        headerLeft.add(titleLabel);
        headerPanel.add(headerLeft, BorderLayout.WEST);

        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerActions.setOpaque(false);
        headerPanel.add(headerActions, BorderLayout.EAST);
        section.putClientProperty("headerActions", headerActions);

        // Description with better formatting and theme color
        // Avoid hard-coded colors inside HTML so theme foreground is respected
        JLabel descLabel = new JLabel("<html><div style='line-height: 1.6;'>" + safeDescription + "</div></html>");
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

    /**
     * Resolves a font based on the configured application font style.
     * <p>
     * If no font is configured, the system UI font or the default application
     * font is used as a fallback. On Windows and macOS, the method also ensures
     * emoji compatibility by switching to a logical font if necessary.
     *
     * @param style    the {@link Font} style (e.g. {@link Font#PLAIN}, {@link Font#BOLD})
     * @param fontSize the desired font size in pixels
     * @return a platform-safe font instance that matches the requested style and size
     */
    @SuppressWarnings("MagicConstant")
    public static Font getFontByName(int style, int fontSize) {
        String fontName = null;
        if (Main.settings != null) {
            fontName = Main.settings.getProperty("font_style");
        }

        if (fontName == null || fontName.trim().isEmpty()) {
            Font uiFont = UIManager.getFont("Label.font");
            fontName = uiFont == null ? DEFAULT_FONT_STYLE : uiFont.getFamily();
        } else if (isWindows() && ("Arial".equalsIgnoreCase(fontName) || "Arial Unicode MS".equalsIgnoreCase(fontName))) {
            // Prefer system UI font on Windows to improve glyph coverage
            Font uiFont = UIManager.getFont("Label.font");
            fontName = uiFont == null ? DEFAULT_FONT_STYLE : uiFont.getFamily();
        }

        Font font = new Font(fontName, style, fontSize);

        // Emoji / symbol fallback on Windows + macOS
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
     * Creates a styled label with specified font and color
     */
    @SuppressWarnings("MagicConstant")
    public static JLabel createStyledLabel(Logger logger, JComboBox<?> fontComboBox, String text, int fontSize, int fontStyle, Color color) {
        JLabel label = new JLabel(text);
        String selectedItem = (String) fontComboBox.getSelectedItem();
        if (selectedItem == null) {
            logger.error("Font-ComboBox hat kein ausgewähltes Element, Standardwert wird verwendet");
            selectedItem = DEFAULT_FONT_STYLE;
        }
        label.setFont(new Font(selectedItem, fontStyle, fontSize));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Custom panel implementation that renders a rounded card surface.
     * <p>
     * The panel draws its own background with anti-aliasing and a subtle
     * drop shadow to achieve a modern card UI look used throughout the
     * settings interface.
     */
    public static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        /**
         * Creates a rounded panel used for modern card-style UI sections.
         * The panel is painted manually with anti-aliasing, a soft shadow,
         * and rounded corners to match the application's design language.
         *
         * @param bg     the background color of the card surface
         * @param radius the corner radius in pixels used for the rounded rectangle
         */
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
            g2.fillRoundRect(2, 2, Math.max(0, getWidth() - 4), Math.max(0, getHeight() - 4), radius, radius);

            // Draw main card
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, Math.max(0, getWidth() - 2), Math.max(0, getHeight() - 2), radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
