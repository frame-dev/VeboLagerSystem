package ch.framedev.lagersystem.managers;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.table.JTableHeader;

import ch.framedev.lagersystem.main.Main;

/**
 * Centralized theme manager for the Swing application.
 *
 * <p>This class provides:
 * <ul>
 *   <li>Light and dark color palettes</li>
 *   <li>Runtime theme switching</li>
 *   <li>Custom accent/header/button color support</li>
 *   <li>Automatic UI refresh for registered windows</li>
 *   <li>Convenience accessors for consistent styling</li>
 * </ul>
 *
 * <p>The manager is implemented as a singleton and integrates with
 * {@link UIManager} to apply consistent defaults across the entire UI.
 */
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings({ "SameReturnValue" })
public class ThemeManager {
        /**
         * Available LookAndFeel options for the application.
         */
        public enum LookAndFeelOption {
            SYSTEM("System", UIManager.getSystemLookAndFeelClassName()),
            METAL("Metal", "javax.swing.plaf.metal.MetalLookAndFeel"),
            NIMBUS("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"),
            MOTIF("Motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel"),
            GTK("GTK+", "com.sun.java.swing.plaf.gtk.GTKLookAndFeel"),
            FLAT_LIGHT("FlatLaf Light", FlatLightLaf.class.getName()),
            FLAT_DARK("FlatLaf Dark", FlatDarkLaf.class.getName()),
            FLAT_INTELLIJ("FlatLaf IntelliJ", FlatIntelliJLaf.class.getName()),
            FLAT_DARCULA("FlatLaf Darcula", FlatDarculaLaf.class.getName());

            public final String displayName;
            public final String className;
            LookAndFeelOption(String displayName, String className) {
                this.displayName = displayName;
                this.className = className;
            }
        }

        /**
         * Set the LookAndFeel by option. Falls back to system if not available.
         */
        public static void setLookAndFeel(LookAndFeelOption option) {
            registerAdditionalLookAndFeels();
            if (option == null) {
                setSystemLookAndFeelSafely();
                return;
            }
            try {
                configureWindowDecorations(option.className);
                UIManager.setLookAndFeel(option.className);
                logger.info("Set LookAndFeel: {} ({})", option.displayName, option.className);
            } catch (Exception e) {
                logger.warn("Could not set LookAndFeel: {} ({}), falling back to system.", option.displayName, option.className, e);
                setSystemLookAndFeelSafely();
            }
        }

        /**
         * Set the LookAndFeel by option. Falls back to system if not available.
         */
        public static void setLookAndFeel(String name) {
            try {
                registerAdditionalLookAndFeels();
                String normalized = name == null ? "" : name.trim();
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (info.getName().equalsIgnoreCase(normalized)) {
                        configureWindowDecorations(info.getClassName());
                        UIManager.setLookAndFeel(info.getClassName());
                        logger.info("Set LookAndFeel: {} ({})", info.getName(), info.getClassName());
                        return;
                    }
                }
                logger.warn("LookAndFeel not found: {}, falling back to system.", name);
                setSystemLookAndFeelSafely();
            } catch (Exception e) {
                logger.error("Could not set LookAndFeel: {}", name, e);
                setSystemLookAndFeelSafely();
            }
        }

        private static void setSystemLookAndFeelSafely() {
            try {
                configureWindowDecorations(UIManager.getSystemLookAndFeelClassName());
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                logger.error("Could not set system LookAndFeel", ex);
            }
        }

        private static void configureWindowDecorations(String lookAndFeelClassName) {
            boolean flatLaf = lookAndFeelClassName != null
                    && lookAndFeelClassName.startsWith("com.formdev.flatlaf.");
            boolean useNativeWindowDecorations = flatLaf && OS_NAME.contains("win")
                    && FlatLaf.supportsNativeWindowDecorations();
            JFrame.setDefaultLookAndFeelDecorated(flatLaf && !OS_NAME.contains("win"));
            JDialog.setDefaultLookAndFeelDecorated(flatLaf && !OS_NAME.contains("win"));
            FlatLaf.setUseNativeWindowDecorations(useNativeWindowDecorations);
        }

        /**
         * Log all installed LookAndFeels for debugging.
         */
        public static void logAvailableLookAndFeels() {
            registerAdditionalLookAndFeels();
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                logger.info("Available LookAndFeel: {} ({})", info.getName(), info.getClassName());
            }
        }
    private static final Logger logger = LogManager.getLogger(ThemeManager.class);
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
    private static volatile boolean additionalLookAndFeelsRegistered;
    // Set the system LookAndFeel for native appearance, but keep custom theming
    static {
        try {
            registerAdditionalLookAndFeels();
            // Use system L&F for native controls (Windows, macOS, Linux)
            setLookAndFeel(LookAndFeelOption.METAL);
        } catch (Exception e) {
            // Fallback: log and ignore, will use default L&F
            logger.warn("Could not set system LookAndFeel", e);
        }
    }

    public static String getCurrentLookAndFeelName() {
        return UIManager.getLookAndFeel().getName();
    }

    public static List<String> getAllLookAndFeels() {
        registerAdditionalLookAndFeels();
        LinkedHashSet<String> lafNames = new LinkedHashSet<>();
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            lafNames.add(info.getName());
        }
        return new ArrayList<>(lafNames);
    }

    private static void registerAdditionalLookAndFeels() {
        if (additionalLookAndFeelsRegistered) {
            return;
        }
        synchronized (ThemeManager.class) {
            if (additionalLookAndFeelsRegistered) {
                return;
            }

            installLookAndFeelIfMissing(LookAndFeelOption.FLAT_LIGHT.displayName, LookAndFeelOption.FLAT_LIGHT.className);
            installLookAndFeelIfMissing(LookAndFeelOption.FLAT_DARK.displayName, LookAndFeelOption.FLAT_DARK.className);
            installLookAndFeelIfMissing(LookAndFeelOption.FLAT_INTELLIJ.displayName, LookAndFeelOption.FLAT_INTELLIJ.className);
            installLookAndFeelIfMissing(LookAndFeelOption.FLAT_DARCULA.displayName, LookAndFeelOption.FLAT_DARCULA.className);

            additionalLookAndFeelsRegistered = true;
        }
    }

    private static void installLookAndFeelIfMissing(String displayName, String className) {
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if (info.getName().equalsIgnoreCase(displayName) || info.getClassName().equals(className)) {
                return;
            }
        }
        UIManager.installLookAndFeel(new UIManager.LookAndFeelInfo(displayName, className));
        logger.info("Registered LookAndFeel: {} ({})", displayName, className);
    }

    private static volatile ThemeManager instance;
    private static Theme currentTheme = Theme.LIGHT;
    private static Color customAccentColor;
    private static Color customHeaderColor;
    private static Color customButtonColor;
    // Use WeakHashMap to avoid memory leaks if unregisterWindow is forgotten.
    // Windows are automatically removed when they become unreachable.
    private final Map<Window, Boolean> registeredWindows = new WeakHashMap<>();

    /**
     * Available application themes.
     */
    public enum Theme {
        /** Light theme with bright surfaces and dark text. */
        LIGHT,
        /** Dark theme with dark surfaces and light text. */
        DARK
    }

    // =========================
    // Theme color palettes
    // =========================

    public static class Light {

        // Base surfaces
        public static final Color BACKGROUND = new Color(245, 247, 250);
        public static final Color CARD_BACKGROUND = Color.WHITE;
        public static final Color SURFACE = new Color(252, 253, 254);
        public static final Color SURFACE_DARKER = SURFACE.darker();

        // Text
        public static final Color TEXT_PRIMARY = new Color(31, 45, 61);
        public static final Color TEXT_PRIMARY_DARKER = TEXT_PRIMARY.darker();
        public static final Color TEXT_SECONDARY = new Color(108, 117, 125);
        public static final Color TEXT_DISABLED = new Color(160, 170, 180);
        public static final Color TEXT_LINK = new Color(52, 152, 219);
        public static final Color TITLE_TEXT = new Color(255, 255, 255);
        public static final Color TITLE_TEXT_HIGHLIGHT = new Color(52, 152, 219);
        public static final Color TITLE_TEXT_HIGHLIGHT_DARK = new Color(41, 128, 185);
        public static final Color SUB_TITLE_TEXT = new Color(220, 220, 220);

        // Borders & dividers
        public static final Color BORDER = new Color(220, 225, 230);
        public static final Color BORDER_FOCUS = new Color(52, 152, 219);
        public static final Color DIVIDER = new Color(230, 234, 238);

        // Header & navigation
        public static final Color HEADER_BG = new Color(41, 128, 185); // Professional blue
        public static final Color HEADER_FG = Color.WHITE;
        public static final Color MENU_BG = new Color(235, 238, 242);
        public static final Color MENU_HOVER = new Color(220, 226, 232);
        public static final Color MENU_SELECTED = new Color(210, 220, 230);

        // Tables
        public static final Color TABLE_HEADER_BG = new Color(62, 84, 98);
        public static final Color TABLE_HEADER_FG = Color.WHITE;
        public static final Color TABLE_GRID = new Color(226, 230, 233);
        public static final Color TABLE_ROW_EVEN = Color.WHITE;
        public static final Color TABLE_ROW_ODD = new Color(247, 250, 253);
        public static final Color TABLE_ROW_HOVER = new Color(235, 242, 250);
        public static final Color TABLE_ROW_SELECTED = new Color(210, 228, 245);

        // Inputs
        public static final Color INPUT_BG = new Color(250, 251, 252);
        public static final Color INPUT_BORDER = new Color(189, 195, 199);
        public static final Color INPUT_FOCUS_BORDER = new Color(52, 152, 219);
        public static final Color INPUT_DISABLED_BG = new Color(235, 238, 240);

        // Disabled
        public static final Color DISABLED_BG = new Color(220, 220, 220);
        public static final Color DISABLED_FG = new Color(160, 170, 180);

        // Buttons
        public static final Color BUTTON_BG = new Color(52, 152, 219);
        public static final Color BUTTON_FG = Color.WHITE;
        public static final Color BUTTON_HOVER = new Color(41, 128, 185);
        public static final Color BUTTON_PRESSED = new Color(31, 97, 141);
        public static final Color BUTTON_DISABLED_BG = new Color(200, 205, 210);
        public static final Color BUTTON_DISABLED_FG = new Color(150, 155, 160);

        // Feedback colors
        public static final Color SUCCESS = new Color(46, 204, 113);
        public static final Color WARNING = new Color(241, 196, 15);
        public static final Color ERROR = new Color(231, 76, 60);
        public static final Color INFO = new Color(52, 152, 219);

        // Selection & highlight
        public static final Color SELECTION_BG = new Color(210, 228, 245);
        public static final Color SELECTION_FG = TEXT_PRIMARY;
        public static final Color TEXT_HIGHLIGHT_BG = new Color(255, 235, 150);

        // Tooltips
        public static final Color TOOLTIP_BG = new Color(50, 50, 50);
        public static final Color TOOLTIP_FG = Color.WHITE;

        // Scrollbars
        public static final Color SCROLLBAR_BG = new Color(235, 238, 242);
        public static final Color SCROLLBAR_FG = new Color(180, 185, 190);
        public static final Color SCROLLBAR_THUMB = new Color(150, 155, 160);
    }

    /**
     * Dark Theme
     */
    public static class Dark {

        // Base surfaces
        public static final Color BACKGROUND = new Color(26, 26, 26, 255);
        public static final Color CARD_BACKGROUND = new Color(45, 45, 45);
        public static final Color SURFACE = new Color(50, 50, 50);
        public static final Color SURFACE_DARKER = SURFACE.darker();

        // Text
        public static final Color TEXT_PRIMARY = new Color(240, 240, 240);
        public static final Color TEXT_PRIMARY_DARKER = TEXT_PRIMARY.darker();
        public static final Color TEXT_SECONDARY = new Color(180, 180, 180);
        public static final Color TEXT_DISABLED = new Color(120, 120, 120);
        public static final Color TEXT_LINK = new Color(100, 170, 255);
        public static final Color TITLE_TEXT = new Color(255, 255, 255);
        public static final Color TITLE_TEXT_HIGHLIGHT = new Color(170, 186, 255);
        public static final Color TITLE_TEXT_HIGHLIGHT_DARK = new Color(130, 150, 255);
        public static final Color SUB_TITLE_TEXT = new Color(200, 200, 200);

        // Borders & dividers
        public static final Color BORDER = new Color(60, 60, 60);
        public static final Color BORDER_FOCUS = new Color(52, 152, 219);
        public static final Color DIVIDER = new Color(70, 70, 70);

        // Header & navigation
        public static final Color HEADER_BG = new Color(30, 58, 95); // Darker professional blue for dark mode
        public static final Color HEADER_FG = TEXT_PRIMARY;
        public static final Color MENU_BG = new Color(40, 40, 40);
        public static final Color MENU_HOVER = new Color(55, 55, 55);
        public static final Color MENU_SELECTED = new Color(65, 65, 65);

        // Tables
        public static final Color TABLE_HEADER_BG = new Color(50, 50, 50);
        public static final Color TABLE_HEADER_FG = TEXT_PRIMARY;
        public static final Color TABLE_GRID = new Color(60, 60, 60);
        public static final Color TABLE_ROW_EVEN = new Color(45, 45, 45);
        public static final Color TABLE_ROW_ODD = new Color(40, 40, 40);
        public static final Color TABLE_ROW_HOVER = new Color(60, 60, 60);
        public static final Color TABLE_ROW_SELECTED = new Color(75, 110, 175);

        // Inputs
        public static final Color INPUT_BG = new Color(50, 50, 50);
        public static final Color INPUT_BORDER = new Color(70, 70, 70);
        public static final Color INPUT_FOCUS_BORDER = new Color(52, 152, 219);
        public static final Color INPUT_DISABLED_BG = new Color(65, 65, 65);

        // Buttons
        public static final Color BUTTON_BG = new Color(52, 152, 219);
        public static final Color BUTTON_FG = Color.WHITE;
        public static final Color BUTTON_HOVER = new Color(72, 170, 240);
        public static final Color BUTTON_PRESSED = new Color(32, 120, 200);
        public static final Color BUTTON_DISABLED_BG = new Color(80, 80, 80);
        public static final Color BUTTON_DISABLED_FG = new Color(120, 120, 120);

        // Disabled
        public static final Color DISABLED_BG = new Color(80, 80, 80);
        public static final Color DISABLED_FG = new Color(120, 120, 120);

        // Feedback colors
        public static final Color SUCCESS = new Color(80, 200, 120);
        public static final Color WARNING = new Color(240, 200, 90);
        public static final Color ERROR = new Color(230, 90, 90);
        public static final Color INFO = new Color(100, 170, 255);

        // Selection & highlight
        public static final Color SELECTION_BG = new Color(75, 110, 175);
        public static final Color SELECTION_FG = TEXT_PRIMARY;
        public static final Color TEXT_HIGHLIGHT_BG = new Color(75, 110, 175);

        // Tooltips
        public static final Color TOOLTIP_BG = new Color(60, 60, 60);
        public static final Color TOOLTIP_FG = TEXT_PRIMARY;

        // Scrollbars
        public static final Color SCROLLBAR_BG = new Color(60, 60, 60);
        public static final Color SCROLLBAR_FG = new Color(120, 120, 120);
        public static final Color SCROLLBAR_THUMB = new Color(90, 90, 90);
    }

    // =========================
    // Singleton
    // =========================

    /**
     * Creates the theme manager and applies initial UI defaults.
     */
    private ThemeManager() {
        initialize();
        applyUIDefaults();
    }

    /**
     * Returns the singleton instance of the theme manager.
     *
     * @return theme manager instance
     */
    public static ThemeManager getInstance() {
        if (instance == null) {
            synchronized (ThemeManager.class) {
                if (instance == null) {
                    instance = new ThemeManager();
                }
            }
        }
        return instance;
    }

    /**
     * For testing or reinitialization: resets the singleton instance (use with
     * caution).
     */
    public static void resetInstance() {
        synchronized (ThemeManager.class) {
            if (instance != null) {
                instance = null;
            }
        }
    }

    // =========================
    // Theme init / persistence
    // =========================

    /**
     * Initializes the theme from persisted settings and loads custom colors.
     */
    public static void initialize() {
        if (Main.settings != null) {
            String darkModeStr = Main.settings.getProperty("dark_mode");
            boolean darkMode = Boolean.parseBoolean(darkModeStr);
            currentTheme = darkMode ? Theme.DARK : Theme.LIGHT;
            loadCustomColorsFromSettings();
        }
    }

    /**
     * Returns the currently active theme.
     *
     * @return active theme
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Changes the current theme, persists the setting, and refreshes all registered
     * windows.
     *
     * @param theme new theme to apply
     */
    public void setTheme(Theme theme) {
        Runnable apply = () -> {
            currentTheme = theme;

            if (Main.settings != null) {
                Main.settings.setProperty("dark_mode", String.valueOf(theme == Theme.DARK));
            }

            applyUIDefaults();
            updateAllWindows();
        };
        runOnEdt(apply);
    }

    /**
     * Sets custom accent, header, and button colors and refreshes the UI.
     *
     * @param accent custom accent color (nullable)
     * @param header custom header color (nullable)
     * @param button custom button color (nullable)
     */
    public static void setCustomColors(Color accent, Color header, Color button) {
        Runnable apply = () -> {
            customAccentColor = accent;
            customHeaderColor = header;
            customButtonColor = button;
            applyUIDefaults();
            getInstance().updateAllWindows();
        };
        runOnEdt(apply);
    }

    public static Color getCustomAccentColor() {
        return customAccentColor;
    }

    public static Color getCustomHeaderColor() {
        return customHeaderColor;
    }

    public static Color getCustomButtonColor() {
        return customButtonColor;
    }

    /**
     * Loads user-defined colors from persisted settings.
     */
    private static void loadCustomColorsFromSettings() {
        if (Main.settings == null) {
            return;
        }
        customAccentColor = parseHexColor(Main.settings.getProperty("theme_accent_color"));
        customHeaderColor = parseHexColor(Main.settings.getProperty("theme_header_color"));
        customButtonColor = parseHexColor(Main.settings.getProperty("theme_button_color"));
    }

    /**
     * Parses a hex color string into a {@link Color} instance.
     *
     * @param value hex string such as "#RRGGBB"
     * @return parsed color or {@code null} if invalid
     */
    private static Color parseHexColor(String value) {
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

    /**
     * Returns whether the current theme is dark.
     *
     * @return {@code true} if dark mode is active
     */
    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    /**
     * Convenience method to toggle dark mode.
     *
     * @param enabled {@code true} to enable dark theme
     */
    public static void setDarkMode(boolean enabled) {
        getInstance().setTheme(enabled ? Theme.DARK : Theme.LIGHT);
    }

    /**
     * Applies theme-aware UI defaults specifically for JOptionPane dialogs.
     */
    public static final class JOptionPaneTheme {

        private JOptionPaneTheme() {
        }

        /**
         * Applies theme colors and fonts to {@link UIManager} defaults.
         * This ensures newly created components automatically use the current theme.
         */
        public static void apply() {
            // Base colors
            Color bg = ThemeManager.getCardBackgroundColor(); // dialog panel background
            Color surfaceBg = ThemeManager.getBackgroundColor(); // surface/window background
            Color fg = ThemeManager.getTextPrimaryColor(); // main text color
            Color secondaryFg = ThemeManager.getTextSecondaryColor(); // secondary text
            Color border = ThemeManager.getBorderColor();

            // Inputs
            Color inputBg = ThemeManager.getInputBackgroundColor();
            Color inputFg = ThemeManager.getTextPrimaryColor();
            Color caret = ThemeManager.getTextPrimaryColor();

            // Buttons - use proper button colors with white text for visibility
            Color btnBg = ThemeManager.getButtonBackgroundColor();
            Color btnFg = ThemeManager.getButtonForegroundColor();
            Color btnHover = ThemeManager.getButtonHoverColor();
            Color btnPressed = ThemeManager.getButtonPressedColor();

            // Selection
            Color selBg = ThemeManager.getSelectionBackgroundColor();
            Color selFg = ThemeManager.getSelectionForegroundColor();

            // ---- JOptionPane / OptionPane ----
            UIManager.put("OptionPane.background", new ColorUIResource(bg));
            UIManager.put("OptionPane.messageForeground", new ColorUIResource(fg));
            UIManager.put("OptionPane.foreground", new ColorUIResource(fg));
            UIManager.put("OptionPane.messageAreaBorder", BorderFactory.createEmptyBorder(12, 15, 12, 15));
            UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(12, 15, 0, 15));

            // ---- Panels ----
            UIManager.put("Panel.background", new ColorUIResource(bg));
            UIManager.put("Panel.foreground", new ColorUIResource(fg));

            // ---- Labels ----
            UIManager.put("Label.foreground", new ColorUIResource(fg));
            UIManager.put("Label.background", new ColorUIResource(bg));
            UIManager.put("Label.disabledForeground", new ColorUIResource(secondaryFg));

            // ---- Buttons - Modern styling with shadows and hover effects ----
            UIManager.put("Button.background", new ColorUIResource(btnBg));
            UIManager.put("Button.foreground", new ColorUIResource(btnFg));
            UIManager.put("Button.select", new ColorUIResource(btnPressed));
            UIManager.put("Button.focus", new ColorUIResource(btnBg)); // Keep original on focus

            // Hover effect colors
            UIManager.put("Button.highlight", new ColorUIResource(btnHover));
            UIManager.put("Button.light", new ColorUIResource(btnHover));
            UIManager.put("Button.shadow", new ColorUIResource(btnPressed));
            UIManager.put("Button.darkShadow", new ColorUIResource(btnPressed.darker()));

            // Create prettier button border with subtle shadow effect
            Color btnBorderColor = ThemeManager.isDarkMode()
                    ? new Color(Math.max(0, btnBg.getRed() - 30), Math.max(0, btnBg.getGreen() - 30),
                            Math.max(0, btnBg.getBlue() - 30))
                    : new Color(Math.max(0, btnBg.getRed() - 20), Math.max(0, btnBg.getGreen() - 20),
                            Math.max(0, btnBg.getBlue() - 20));

            UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(btnBorderColor, 1, true),
                            BorderFactory.createEmptyBorder(1, 1, 2, 1) // Subtle shadow for depth
                    ),
                    BorderFactory.createEmptyBorder(8, 20, 8, 20)));
            UIManager.put("Button.margin", new Insets(8, 20, 8, 20));
            UIManager.put("Button.opaque", Boolean.TRUE);
            UIManager.put("Button.contentAreaFilled", Boolean.TRUE);

            // Enable mouse hover effects
            UIManager.put("Button.rolloverEnabled", Boolean.TRUE);

            // Font styling for buttons (use logical font for emoji fallback on Windows)
            Font buttonFont = UIManager.getFont("Button.font");
            if (buttonFont == null) {
                buttonFont = new Font("Dialog", Font.PLAIN, 13);
            }
            UIManager.put("Button.font", buttonFont.deriveFont(Font.BOLD, 13f));

            // ---- TextField / PasswordField ----
            UIManager.put("TextField.background", new ColorUIResource(inputBg));
            UIManager.put("TextField.foreground", new ColorUIResource(inputFg));
            UIManager.put("TextField.caretForeground", new ColorUIResource(caret));
            UIManager.put("TextField.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("TextField.selectionForeground", new ColorUIResource(selFg));
            UIManager.put("TextField.inactiveForeground", new ColorUIResource(secondaryFg));

            UIManager.put("PasswordField.background", new ColorUIResource(inputBg));
            UIManager.put("PasswordField.foreground", new ColorUIResource(inputFg));
            UIManager.put("PasswordField.caretForeground", new ColorUIResource(caret));
            UIManager.put("PasswordField.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("PasswordField.selectionForeground", new ColorUIResource(selFg));

            // ---- TextArea ----
            UIManager.put("TextArea.background", new ColorUIResource(inputBg));
            UIManager.put("TextArea.foreground", new ColorUIResource(inputFg));
            UIManager.put("TextArea.caretForeground", new ColorUIResource(caret));
            UIManager.put("TextArea.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("TextArea.selectionForeground", new ColorUIResource(selFg));

            // ---- FormattedTextField ----
            UIManager.put("FormattedTextField.background", new ColorUIResource(inputBg));
            UIManager.put("FormattedTextField.foreground", new ColorUIResource(inputFg));
            UIManager.put("FormattedTextField.caretForeground", new ColorUIResource(caret));
            UIManager.put("FormattedTextField.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("FormattedTextField.selectionForeground", new ColorUIResource(selFg));

            // ---- ScrollPane / Viewport ----
            UIManager.put("ScrollPane.background", new ColorUIResource(bg));
            UIManager.put("ScrollPane.foreground", new ColorUIResource(fg));
            UIManager.put("Viewport.background", new ColorUIResource(bg));
            UIManager.put("Viewport.foreground", new ColorUIResource(fg));

            // ---- List ----
            UIManager.put("List.background", new ColorUIResource(inputBg));
            UIManager.put("List.foreground", new ColorUIResource(fg));
            UIManager.put("List.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("List.selectionForeground", new ColorUIResource(selFg));
            UIManager.put("List.dropLineColor", new ColorUIResource(border));

            // ---- ComboBox ----
            UIManager.put("ComboBox.background", new ColorUIResource(inputBg));
            UIManager.put("ComboBox.foreground", new ColorUIResource(fg));
            UIManager.put("ComboBox.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("ComboBox.selectionForeground", new ColorUIResource(selFg));
            UIManager.put("ComboBox.buttonBackground", new ColorUIResource(inputBg));
            UIManager.put("ComboBox.buttonForeground", new ColorUIResource(fg));

            // ---- Tree ----
            UIManager.put("Tree.background", new ColorUIResource(inputBg));
            UIManager.put("Tree.foreground", new ColorUIResource(fg));
            UIManager.put("Tree.textBackground", new ColorUIResource(inputBg));
            UIManager.put("Tree.textForeground", new ColorUIResource(fg));
            UIManager.put("Tree.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("Tree.selectionForeground", new ColorUIResource(selFg));

            // ---- Table ----
            UIManager.put("Table.background", new ColorUIResource(ThemeManager.getCardBackgroundColor()));
            UIManager.put("Table.foreground", new ColorUIResource(fg));
            UIManager.put("Table.selectionBackground", new ColorUIResource(ThemeManager.getTableRowSelectedColor()));
            UIManager.put("Table.selectionForeground", new ColorUIResource(selFg));
            UIManager.put("Table.gridColor", new ColorUIResource(ThemeManager.getTableGridColor()));
            UIManager.put("TableHeader.background", new ColorUIResource(ThemeManager.getTableHeaderBackgroundColor()));
            UIManager.put("TableHeader.foreground", new ColorUIResource(ThemeManager.getTableHeaderForegroundColor()));

            // ---- TabbedPane ----
            UIManager.put("TabbedPane.background", new ColorUIResource(bg));
            UIManager.put("TabbedPane.foreground", new ColorUIResource(fg));
            UIManager.put("TabbedPane.selected", new ColorUIResource(selBg));
            UIManager.put("TabbedPane.selectedForeground", new ColorUIResource(fg));

            // ---- ToolTip ----
            UIManager.put("ToolTip.background", new ColorUIResource(ThemeManager.getTooltipBackgroundColor()));
            UIManager.put("ToolTip.foreground", new ColorUIResource(ThemeManager.getTooltipForegroundColor()));

            // ---- Menu & MenuBar ----
            UIManager.put("Menu.background", new ColorUIResource(bg));
            UIManager.put("Menu.foreground", new ColorUIResource(fg));
            UIManager.put("Menu.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("Menu.selectionForeground", new ColorUIResource(selFg));

            UIManager.put("MenuBar.background", new ColorUIResource(bg));
            UIManager.put("MenuBar.foreground", new ColorUIResource(fg));

            UIManager.put("MenuItem.background", new ColorUIResource(bg));
            UIManager.put("MenuItem.foreground", new ColorUIResource(fg));
            UIManager.put("MenuItem.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("MenuItem.selectionForeground", new ColorUIResource(selFg));

            UIManager.put("PopupMenu.background", new ColorUIResource(bg));
            UIManager.put("PopupMenu.foreground", new ColorUIResource(fg));

            // ---- CheckBox & RadioButton ----
            UIManager.put("CheckBox.background", new ColorUIResource(bg));
            UIManager.put("CheckBox.foreground", new ColorUIResource(fg));

            UIManager.put("RadioButton.background", new ColorUIResource(bg));
            UIManager.put("RadioButton.foreground", new ColorUIResource(fg));

            // ---- Spinner ----
            UIManager.put("Spinner.background", new ColorUIResource(inputBg));
            UIManager.put("Spinner.foreground", new ColorUIResource(fg));

            // ---- Slider ----
            UIManager.put("Slider.background", new ColorUIResource(bg));
            UIManager.put("Slider.foreground", new ColorUIResource(fg));

            // ---- ProgressBar ----
            UIManager.put("ProgressBar.background", new ColorUIResource(bg));
            UIManager.put("ProgressBar.foreground", new ColorUIResource(btnBg));
            UIManager.put("ProgressBar.selectionBackground", new ColorUIResource(fg));
            UIManager.put("ProgressBar.selectionForeground", new ColorUIResource(fg));

            // ---- Separator ----
            UIManager.put("Separator.foreground", new ColorUIResource(border));
            UIManager.put("Separator.background", new ColorUIResource(bg));

            // ---- ToolBar ----
            UIManager.put("ToolBar.background", new ColorUIResource(bg));
            UIManager.put("ToolBar.foreground", new ColorUIResource(fg));

            // ---- SplitPane ----
            UIManager.put("SplitPane.background", new ColorUIResource(bg));

            // ---- Desktop / InternalFrame ----
            UIManager.put("Desktop.background", new ColorUIResource(surfaceBg));
            UIManager.put("InternalFrame.background", new ColorUIResource(bg));

            // ---- ColorChooser ----
            UIManager.put("ColorChooser.background", new ColorUIResource(bg));
            UIManager.put("ColorChooser.foreground", new ColorUIResource(fg));

            // ========================================
            // ---- FILE CHOOSER - COMPREHENSIVE ----
            // ========================================

            // Main FileChooser colors
            UIManager.put("FileChooser.background", new ColorUIResource(bg));
            UIManager.put("FileChooser.foreground", new ColorUIResource(fg));

            // File list
            UIManager.put("FileChooser.listViewBackground", new ColorUIResource(inputBg));
            UIManager.put("FileChooser.listViewForeground", new ColorUIResource(fg));
            UIManager.put("FileChooser.detailsViewBackground", new ColorUIResource(inputBg));
            UIManager.put("FileChooser.detailsViewForeground", new ColorUIResource(fg));

            // Selection in file list
            UIManager.put("FileChooser.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("FileChooser.selectionForeground", new ColorUIResource(selFg));

            // Text fields (file name, path, etc.)
            UIManager.put("FileChooser.textForeground", new ColorUIResource(fg));
            UIManager.put("FileChooser.textBackground", new ColorUIResource(inputBg));

            // ComboBox in FileChooser (directory dropdown)
            UIManager.put("FileChooser.comboBoxBackground", new ColorUIResource(inputBg));
            UIManager.put("FileChooser.comboBoxForeground", new ColorUIResource(fg));

            // Buttons in FileChooser
            UIManager.put("FileChooser.buttonBackground", new ColorUIResource(btnBg));
            UIManager.put("FileChooser.buttonForeground", new ColorUIResource(btnFg));

            // File view specific
            UIManager.put("FileView.computerIcon", null);
            UIManager.put("FileView.directoryIcon", null);
            UIManager.put("FileView.fileIcon", null);
            UIManager.put("FileView.floppyDriveIcon", null);
            UIManager.put("FileView.hardDriveIcon", null);

            Font toolTipFont = UIManager.getFont("ToolTip.font");
            if (toolTipFont == null) {
                toolTipFont = new Font("Dialog", Font.PLAIN, 16);
            }
            UIManager.put("ToolTip.font", toolTipFont.deriveFont(Font.BOLD, 16f));
            UIManager.put("TabbedPane.tabInsets", new Insets(18, 25, 18, 25));
            UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 2));
            // ---- ScrollBar (modern look) ----
            UIManager.put("ScrollBar.background", new ColorUIResource(ThemeManager.getScrollbarBackgroundColor()));
            UIManager.put("ScrollBar.foreground", new ColorUIResource(ThemeManager.getScrollbarForegroundColor()));
            UIManager.put("ScrollBar.thumb", new ColorUIResource(ThemeManager.getScrollbarThumbColor()));
            UIManager.put("ScrollBar.thumbHighlight",
                    new ColorUIResource(ThemeManager.withAlpha(ThemeManager.getScrollbarThumbColor(), 180)));
            UIManager.put("ScrollBar.thumbDarkShadow",
                    new ColorUIResource(ThemeManager.withAlpha(ThemeManager.getScrollbarThumbColor(), 120)));
            UIManager.put("ScrollBar.thumbShadow",
                    new ColorUIResource(ThemeManager.withAlpha(ThemeManager.getScrollbarThumbColor(), 150)));
            UIManager.put("ScrollBar.track", new ColorUIResource(ThemeManager.getScrollbarBackgroundColor()));
            UIManager.put("ScrollBar.trackHighlight", new ColorUIResource(ThemeManager.getAccentColor()));
            UIManager.put("ScrollBar.border", BorderFactory.createLineBorder(ThemeManager.getDividerColor(), 1, true));
            // Use a slightly thinner scrollbar on macOS for native feel
            UIManager.put("ScrollBar.width", isMac() ? 10 : 14);
            UIManager.put("ScrollBar.minimumThumbSize", new java.awt.Dimension(30, 30));
            UIManager.put("ScrollBar.maximumThumbSize", new java.awt.Dimension(100, 100));
            UIManager.put("ScrollBar.opaque", Boolean.TRUE);
        }
    }

    // =========================
    // UIManager defaults
    // =========================

    /**
     * Applies theme colors and fonts to {@link UIManager} defaults.
     * This ensures newly created components automatically use the current theme.
     */
    public static void applyUIDefaults() {
        UIDefaults d = UIManager.getDefaults();

        Color bg = getBackgroundColor();
        Color card = getCardBackgroundColor();
        Color fg = getTextPrimaryColor();
        Color fg2 = getTextSecondaryColor();
        Color border = getBorderColor();

        Color inputBg = getInputBackgroundColor();
        Color inputFg = getTextPrimaryColor();

        Color selBg = getSelectionBackgroundColor();
        Color selFg = getSelectionForegroundColor();

        Color btnBg = getButtonBackgroundColor();
        Color btnFg = getButtonForegroundColor();

        // ---------- General surfaces ----------
        d.put("control", new ColorUIResource(bg));
        d.put("Panel.background", new ColorUIResource(bg));
        d.put("Viewport.background", new ColorUIResource(bg));
        d.put("ScrollPane.background", new ColorUIResource(bg));

        // ---------- Text ----------
        d.put("Label.foreground", new ColorUIResource(fg));
        d.put("TitledBorder.titleColor", new ColorUIResource(fg));
        d.put("ToolTip.background", new ColorUIResource(getTooltipBackgroundColor()));
        d.put("ToolTip.foreground", new ColorUIResource(getTooltipForegroundColor()));

        // ---------- Buttons (Cancel/OK in dialogs are plain buttons) ----------
        d.put("Button.background", new ColorUIResource(btnBg));
        d.put("Button.foreground", new ColorUIResource(btnFg));
        d.put("Button.disabledText", new ColorUIResource(getDisabledForeground()));

        // ---------- Inputs ----------
        d.put("TextField.background", new ColorUIResource(inputBg));
        d.put("TextField.foreground", new ColorUIResource(inputFg));
        d.put("TextField.caretForeground", new ColorUIResource(inputFg));
        d.put("TextField.selectionBackground", new ColorUIResource(selBg));
        d.put("TextField.selectionForeground", new ColorUIResource(selFg));

        d.put("PasswordField.background", new ColorUIResource(inputBg));
        d.put("PasswordField.foreground", new ColorUIResource(inputFg));
        d.put("PasswordField.caretForeground", new ColorUIResource(inputFg));
        d.put("PasswordField.selectionBackground", new ColorUIResource(selBg));
        d.put("PasswordField.selectionForeground", new ColorUIResource(selFg));

        d.put("TextArea.background", new ColorUIResource(inputBg));
        d.put("TextArea.foreground", new ColorUIResource(inputFg));
        d.put("TextArea.caretForeground", new ColorUIResource(inputFg));
        d.put("TextArea.selectionBackground", new ColorUIResource(selBg));
        d.put("TextArea.selectionForeground", new ColorUIResource(selFg));

        // ---------- Lists/Combos/Menus (FileChooser uses these a lot) ----------
        d.put("List.background", new ColorUIResource(card));
        d.put("List.foreground", new ColorUIResource(fg));
        d.put("List.selectionBackground", new ColorUIResource(selBg));
        d.put("List.selectionForeground", new ColorUIResource(selFg));

        d.put("ComboBox.background", new ColorUIResource(inputBg));
        d.put("ComboBox.foreground", new ColorUIResource(fg));
        d.put("ComboBox.selectionBackground", new ColorUIResource(selBg));
        d.put("ComboBox.selectionForeground", new ColorUIResource(selFg));

        d.put("Menu.background", new ColorUIResource(bg));
        d.put("Menu.foreground", new ColorUIResource(fg));
        d.put("MenuItem.background", new ColorUIResource(bg));
        d.put("MenuItem.foreground", new ColorUIResource(fg));
        d.put("MenuItem.selectionBackground", new ColorUIResource(selBg));
        d.put("MenuItem.selectionForeground", new ColorUIResource(selFg));

        // ---------- OptionPane (JOptionPane) ----------
        d.put("OptionPane.background", new ColorUIResource(bg));
        d.put("OptionPane.messageForeground", new ColorUIResource(fg));
        d.put("OptionPane.foreground", new ColorUIResource(fg));

        applyWindowsTitleBarDefaults(d);

        // ---------- Fonts (emoji-safe on Windows/macOS) ----------
        if (isWindowsOrMac()) {
            String[] fontKeys = {
                    "defaultFont",
                    "Label.font",
                    "Button.font",
                    "ToggleButton.font",
                    "Menu.font",
                    "MenuItem.font",
                    "CheckBox.font",
                    "RadioButton.font",
                    "TextField.font",
                    "PasswordField.font",
                    "TextArea.font",
                    "TextPane.font",
                    "EditorPane.font",
                    "Table.font",
                    "TableHeader.font",
                    "TabbedPane.font",
                    "ComboBox.font",
                    "List.font",
                    "ToolTip.font",
                    "TitledBorder.font",
                    "OptionPane.messageFont",
                    "OptionPane.buttonFont"
            };
            for (String key : fontKeys) {
                Font font = d.getFont(key);
                Font safeFont = withEmojiFallback(font);
                if (safeFont != null) {
                    d.put(key, new FontUIResource(safeFont));
                }
            }
        }

        // ---------- FileChooser ----------
        d.put("FileChooser.background", new ColorUIResource(bg));
        d.put("FileChooser.foreground", new ColorUIResource(fg));

        // These vary by LAF but help a lot:
        d.put("FileChooser.listViewBackground", new ColorUIResource(card));
        d.put("FileChooser.listViewForeground", new ColorUIResource(fg));
        d.put("FileChooser.detailsViewBackground", new ColorUIResource(card));
        d.put("FileChooser.detailsViewForeground", new ColorUIResource(fg));

        // Text field inside chooser
        d.put("FileChooser.textFieldBackground", new ColorUIResource(inputBg));
        d.put("FileChooser.textFieldForeground", new ColorUIResource(fg));

        // IMPORTANT: correct selection colors
        d.put("FileChooser.textSelectionBackground", new ColorUIResource(selBg));
        d.put("FileChooser.textSelectionForeground", new ColorUIResource(selFg));

        // ---------- Tables (chooser "Details" view uses JTable) ----------
        d.put("Table.background", new ColorUIResource(card));
        d.put("Table.foreground", new ColorUIResource(fg));
        d.put("Table.gridColor", new ColorUIResource(getTableGridColor()));
        d.put("Table.selectionBackground", new ColorUIResource(selBg));
        d.put("Table.selectionForeground", new ColorUIResource(selFg));

        d.put("TableHeader.background", new ColorUIResource(getTableHeaderBackgroundColor()));
        d.put("TableHeader.foreground", new ColorUIResource(getTableHeaderForegroundColor()));

        // ---------- Borders ----------
        d.put("Separator.foreground", new ColorUIResource(border));
        d.put("Separator.background", new ColorUIResource(border));

        // ---------- TabbedPane (modern look) ----------
        d.put("TabbedPane.background", new ColorUIResource(bg));
        d.put("TabbedPane.foreground", new ColorUIResource(fg));
        d.put("TabbedPane.selected", new ColorUIResource(selBg));
        d.put("TabbedPane.selectedForeground", new ColorUIResource(fg));
        d.put("TabbedPane.contentAreaColor", new ColorUIResource(bg));
        d.put("TabbedPane.focus", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.borderHightlightColor", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.light", new ColorUIResource(getBorderLightColor()));
        d.put("TabbedPane.shadow", new ColorUIResource(border));
        d.put("TabbedPane.darkShadow", new ColorUIResource(border.darker()));
        d.put("TabbedPane.unselectedBackground", new ColorUIResource(bg));
        d.put("TabbedPane.unselectedForeground", new ColorUIResource(fg2));
        d.put("TabbedPane.tabAreaBackground", new ColorUIResource(bg));
        d.put("TabbedPane.selectedTabPadInsets", new Insets(4, 12, 4, 12));
        d.put("TabbedPane.tabInsets", new Insets(8, 18, 8, 18));
        d.put("TabbedPane.tabAreaInsets", new Insets(2, 2, 0, 2));
        d.put("TabbedPane.contentBorderInsets", new Insets(2, 2, 2, 2));
        d.put("TabbedPane.focusHighlight", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.selectedTabHighlight", new ColorUIResource(getHeaderGradientColor()));
        d.put("TabbedPane.selectedTabBackground", new ColorUIResource(selBg));
        d.put("TabbedPane.selectedTabForeground", new ColorUIResource(fg));
        d.put("TabbedPane.borderColor", new ColorUIResource(border));
        d.put("TabbedPane.selectedTabBorderColor", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.underlineColor", new ColorUIResource(getAccentColor()));
        d.put("TabbedPane.underlineHeight", 3);
        d.put("TabbedPane.underlineAtTop", Boolean.FALSE);
        d.put("TabbedPane.opaque", Boolean.TRUE);
        d.put("TabbedPane.showContentSeparator", Boolean.TRUE);
        d.put("TabbedPane.tabRunOverlay", 2);
        d.put("TabbedPane.tabRunIndent", 8);
        d.put("TabbedPane.tabSelectionHeight", 4);
        d.put("TabbedPane.tabSelectionColor", new ColorUIResource(getAccentColor()));
        d.put("TabbedPane.tabSelectionUnderlineColor", new ColorUIResource(getAccentColor()));
        d.put("TabbedPane.tabSelectionUnderlineHeight", 3);
        d.put("TabbedPane.tabSelectionUnderlineAtTop", Boolean.FALSE);
        d.put("TabbedPane.tabSelectionUnderlineInsets", new Insets(0, 8, 0, 8));
        d.put("TabbedPane.tabFocusColor", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.tabFocusHighlightColor", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.tabFocusHighlightThickness", 2);
        d.put("TabbedPane.tabFocusHighlightInsets", new Insets(2, 8, 2, 8));
        d.put("TabbedPane.tabBackground", new ColorUIResource(bg));
        d.put("TabbedPane.tabForeground", new ColorUIResource(fg));
        d.put("TabbedPane.tabDisabledBackground", new ColorUIResource(getDisabledBackgroundColor()));
        d.put("TabbedPane.tabDisabledForeground", new ColorUIResource(getDisabledForegroundColor()));
        d.put("TabbedPane.tabBorderColor", new ColorUIResource(border));
        d.put("TabbedPane.tabSelectedBorderColor", new ColorUIResource(getBorderFocusColor()));
        d.put("TabbedPane.tabHighlight", new ColorUIResource(getHeaderGradientColor()));
        d.put("TabbedPane.tabShadow", new ColorUIResource(border.darker()));
        d.put("TabbedPane.tabAreaBorderColor", new ColorUIResource(border));
        d.put("TabbedPane.tabAreaHighlightColor", new ColorUIResource(getHeaderGradientColor()));
        d.put("TabbedPane.tabAreaShadowColor", new ColorUIResource(border.darker()));
        d.put("TabbedPane.tabAreaOpaque", Boolean.TRUE);
        d.put("TabbedPane.tabOpaque", Boolean.TRUE);
        d.put("TabbedPane.tabSelectedOpaque", Boolean.TRUE);
        d.put("TabbedPane.tabDisabledOpaque", Boolean.TRUE);
        // ---------- ScrollBar (modern look) ----------
        d.put("ScrollBar.background", new ColorUIResource(getScrollbarBackgroundColor()));
        d.put("ScrollBar.foreground", new ColorUIResource(getScrollbarForegroundColor()));
        d.put("ScrollBar.thumb", new ColorUIResource(getScrollbarThumbColor()));
        d.put("ScrollBar.thumbHighlight", new ColorUIResource(withAlpha(getScrollbarThumbColor(), 180)));
        d.put("ScrollBar.thumbDarkShadow", new ColorUIResource(withAlpha(getScrollbarThumbColor(), 120)));
        d.put("ScrollBar.thumbShadow", new ColorUIResource(withAlpha(getScrollbarThumbColor(), 150)));
        d.put("ScrollBar.track", new ColorUIResource(getScrollbarBackgroundColor()));
        d.put("ScrollBar.trackHighlight", new ColorUIResource(getAccentColor()));
        d.put("ScrollBar.border", BorderFactory.createLineBorder(getDividerColor(), 1, true));
        // Use a slightly thinner scrollbar on macOS for native feel
        d.put("ScrollBar.width", isMac() ? 10 : 14);
        d.put("ScrollBar.minimumThumbSize", new java.awt.Dimension(30, 30));
        d.put("ScrollBar.maximumThumbSize", new java.awt.Dimension(100, 100));
        d.put("ScrollBar.opaque", Boolean.TRUE);
    }

    private static void applyWindowsTitleBarDefaults(UIDefaults defaults) {
        if (!(UIManager.getLookAndFeel() instanceof FlatLaf) || !isWindows()) {
            return;
        }

        // Prefer native Windows caption buttons and keep FlatLaf's fallback title pane compact.
        defaults.put("TitlePane.useWindowDecorations", Boolean.FALSE);
        defaults.put("TitlePane.buttonSize", new java.awt.Dimension(34, 24));
        defaults.put("TitlePane.buttonMinimumWidth", 24);
        defaults.put("TitlePane.buttonMaximizedHeight", 20);
        defaults.put("TitlePane.buttonSymbolHeight", 8);
        defaults.put("TitlePane.buttonsGap", 0);
        defaults.put("TitlePane.buttonsMargins", new Insets(0, 0, 0, 0));
        defaults.put("TitlePane.small.buttonSize", new java.awt.Dimension(28, 18));
        defaults.put("TitlePane.small.buttonSymbolHeight", 7);
    }

    private static Font withEmojiFallback(Font font) {
        if (font == null) {
            return null;
        }
        // On macOS, use the system font for best compatibility
        if (isMac()) {
            return new Font("San Francisco", font.getStyle(), font.getSize());
        }
        // On Windows, fallback to Segoe UI for emoji support
        if (isWindows()) {
            return new Font("Segoe UI Emoji", font.getStyle(), font.getSize());
        }
        // On Linux, fallback to DejaVu Sans if available
        if (OS_NAME.contains("nux") || OS_NAME.contains("nix")) {
            return new Font("DejaVu Sans", font.getStyle(), font.getSize());
        }
        // Default fallback
        if (font.canDisplayUpTo("\uD83D\uDCC1") != -1) {
            return new Font("Dialog", font.getStyle(), font.getSize());
        }
        return font;
    }

    private static boolean isWindowsOrMac() {
        return isWindows() || isMac();
    }

    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    private static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    private static void runOnEdt(Runnable action) {
        if (action == null) {
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    // =========================
    // Window registration / refresh
    // =========================

    /**
     * Registers a window so it can be refreshed when the theme changes.
     *
     * @param window window to track
     */
    public void registerWindow(Window window) {
        if (window != null) {
            registeredWindows.put(window, Boolean.TRUE);
            // Run once after the current EDT cycle so components built in constructors
            // receive the same visual baseline across all windows.
            SwingUtilities.invokeLater(() -> {
                updateComponentTree(window);
                window.revalidate();
                window.repaint();
            });
        }
    }

    /**
     * Removes a window from theme tracking.
     *
     * @param window window to remove
     */
    public void unregisterWindow(Window window) {
        registeredWindows.remove(window);
    }

    /**
     * Refreshes all registered windows so the new theme is applied immediately.
     */
    public void updateAllWindows() {
        Runnable refresh = () -> {
            // Snapshot to avoid concurrent modification while iterating
            for (Window window : java.util.List.copyOf(registeredWindows.keySet())) {
                if (!window.isDisplayable()) {
                    continue;
                }
                SwingUtilities.updateComponentTreeUI(window);
                updateComponentTree(window);
                window.revalidate();
                window.repaint();
            }
        };
        runOnEdt(refresh);
    }

    /**
     * Recursively applies theme styling to all components in a container.
     */
    private void updateComponentTree(Container container) {
        applyThemeToComponent(container);
        for (Component comp : container.getComponents()) {
            applyThemeToComponent(comp);
            if (comp instanceof Container container1) {
                updateComponentTree(container1);
            }
        }
    }

    /**
     * Applies theme colors to a single Swing component depending on its type.
     */
    private void applyThemeToComponent(Component comp) {

        // Panels
        if (comp instanceof JPanel panel) {
            if (panel.isOpaque()) {
                panel.setBackground(getBackgroundColor());
            }
        }

        // Scroll panes: set a viewport background too
        if (comp instanceof JScrollPane sp) {
            sp.setBackground(getBackgroundColor());
            if (sp.getViewport() != null)
                sp.getViewport().setBackground(getCardBackgroundColor());
            if (sp.getColumnHeader() != null)
                sp.getColumnHeader().setBackground(getBackgroundColor());
        }

        if (comp instanceof JSplitPane splitPane) {
            splitPane.setBackground(getBackgroundColor());
            splitPane.setForeground(getBorderColor());
            if (splitPane.getDividerSize() < 8) {
                splitPane.setDividerSize(8);
            }
        }

        if (comp instanceof JTabbedPane tabbedPane) {
            tabbedPane.setBackground(getBackgroundColor());
            tabbedPane.setForeground(getTextPrimaryColor());
            tabbedPane.setOpaque(true);
            Object refreshTabs = tabbedPane.getClientProperty("settings.tab.refresh");
            if (refreshTabs instanceof Runnable runnable) {
                runnable.run();
            }
        }

        if (comp instanceof JList<?> list) {
            list.setBackground(getCardBackgroundColor());
            list.setForeground(getTextPrimaryColor());
            list.setSelectionBackground(getSelectionBackgroundColor());
            list.setSelectionForeground(getSelectionForegroundColor());
        }

        if (comp instanceof JComboBox<?> comboBox) {
            comboBox.setBackground(getInputBackgroundColor());
            comboBox.setForeground(getTextPrimaryColor());
        }

        // Tables
        if (comp instanceof JTable table) {
            table.setBackground(getCardBackgroundColor());
            table.setForeground(getTextPrimaryColor());
            table.setGridColor(getTableGridColor());
            table.setSelectionBackground(getTableRowSelectedColor());
            table.setSelectionForeground(getTextPrimaryColor());

            JTableHeader header = table.getTableHeader();
            if (header != null) {
                header.setBackground(getTableHeaderBackgroundColor());
                header.setForeground(getTableHeaderForegroundColor());
            }
        }

        // Generic JComponent handling
        if (comp instanceof JComponent jc) {
            boolean enabled = jc.isEnabled();

            if (jc instanceof JLabel || jc instanceof JCheckBox || jc instanceof JRadioButton) {
                // Preserve custom foreground colors (e.g. header/title labels).
                Color fg = jc.getForeground();
                if (fg == null || fg instanceof UIResource) {
                    jc.setForeground(enabled ? getTextPrimaryColor() : getDisabledForeground());
                }
            }

            if (jc instanceof JButton button) {
                boolean customButtonStyle = Boolean.TRUE.equals(button.getClientProperty("theme.button.custom"));
                // Preserve explicit button text colors when they are custom.
                Color buttonFg = button.getForeground();
                boolean useDefaultButtonFg = buttonFg == null || buttonFg instanceof UIResource;
                Color buttonBg = button.getBackground();
                boolean useDefaultButtonBg = buttonBg == null || buttonBg instanceof UIResource;
                if (!customButtonStyle && useDefaultButtonBg) {
                    button.setBackground(enabled ? getButtonBackgroundColor() : getDisabledBackgroundColor());
                }
                if (!customButtonStyle && useDefaultButtonFg) {
                    button.setForeground(enabled ? getButtonForegroundColor() : getDisabledForeground());
                }
                button.setFocusPainted(false);
                if (!customButtonStyle) {
                    installConsistentButtonStyle(button);
                }
            }

            if (jc instanceof JTextField field) {
                field.setBackground(enabled ? getInputBackgroundColor() : getInputDisabledBackgroundColor());
                field.setForeground(enabled ? getTextPrimaryColor() : getDisabledForeground());
                field.setCaretColor(getTextPrimaryColor());
                field.setSelectionColor(getSelectionBackgroundColor());
                field.setSelectedTextColor(getSelectionForegroundColor());

                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(enabled ? getInputBorderColor() : getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            }

            if (jc instanceof JTextArea area) {
                area.setBackground(enabled ? getInputBackgroundColor() : getInputDisabledBackgroundColor());
                area.setForeground(enabled ? getTextPrimaryColor() : getDisabledForeground());
                area.setCaretColor(getTextPrimaryColor());
                area.setSelectionColor(getSelectionBackgroundColor());
                area.setSelectedTextColor(getSelectionForegroundColor());
            }
        }
    }

    private void installConsistentButtonStyle(JButton button) {
        if (button == null) {
            return;
        }

        Color base = button.isEnabled() ? button.getBackground() : getDisabledBackgroundColor();
        if (base == null) {
            base = getButtonBackgroundColor();
            button.setBackground(base);
        }
        final Color buttonBase = base;

        // Keep custom colors (danger/success/etc.) but normalize spacing and border style.
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getButtonPressedColor(buttonBase), 1, true),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        Object marker = button.getClientProperty("theme.button.hover.installed");
        if (Boolean.TRUE.equals(marker)) {
            return;
        }

        button.putClientProperty("theme.button.hover.installed", Boolean.TRUE);
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(getButtonHoverColor(buttonBase));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(buttonBase);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) {
                    button.setBackground(getButtonPressedColor(buttonBase));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) {
                    return;
                }
                button.setBackground(button.contains(e.getPoint()) ? getButtonHoverColor(buttonBase) : buttonBase);
            }
        });
    }

    // =========================
    // Accessor methods (kept close to your original API)
    // =========================

    public static Color getDisabledBackgroundColor() {
        return isDarkMode() ? new Color(80, 80, 80) : new Color(220, 220, 220);
    }

    public static Color getTableHeaderBackground() {
        return isDarkMode() ? Dark.TABLE_HEADER_BG : Light.TABLE_HEADER_BG;
    }

    public static Color getTableHeaderForeground() {
        return isDarkMode() ? Dark.TABLE_HEADER_FG : Light.TABLE_HEADER_FG;
    }

    public static Color getDisabledForegroundColor() {
        return isDarkMode() ? Dark.TEXT_DISABLED : Light.TEXT_DISABLED;
    }

    public static Color getTextLinkColor() {
        return isDarkMode() ? Dark.TEXT_LINK : Light.TEXT_LINK;
    }

    public static Color getSurfaceColor() {
        return isDarkMode() ? Dark.SURFACE : Light.SURFACE;
    }

    public static Color getDividerColor() {
        return isDarkMode() ? Dark.DIVIDER : Light.DIVIDER;
    }

    public static Color getHeaderForegroundColor() {
        return isDarkMode() ? Dark.HEADER_FG : Light.HEADER_FG;
    }

    public static Color getMenuBackgroundColor() {
        return isDarkMode() ? Dark.MENU_BG : Light.MENU_BG;
    }

    public static Color getMenuHoverColor() {
        return isDarkMode() ? Dark.MENU_HOVER : Light.MENU_HOVER;
    }

    public static Color getMenuSelectedColor() {
        return isDarkMode() ? Dark.MENU_SELECTED : Light.MENU_SELECTED;
    }

    public static Color getTableRowHoverColor() {
        return isDarkMode() ? Dark.TABLE_ROW_HOVER : Light.TABLE_ROW_HOVER;
    }

    public static Color getTableRowSelectedColor() {
        return isDarkMode() ? Dark.TABLE_ROW_SELECTED : Light.TABLE_ROW_SELECTED;
    }

    public static Color getDisabledBackground() {
        return isDarkMode() ? Dark.DISABLED_BG : Light.DISABLED_BG;
    }

    public static Color getDisabledForeground() {
        return isDarkMode() ? Dark.DISABLED_FG : Light.DISABLED_FG;
    }

    public static Color getButtonBackgroundColor() {
        return customButtonColor != null ? customButtonColor : (isDarkMode() ? Dark.BUTTON_BG : Light.BUTTON_BG);
    }

    public static Color getButtonForegroundColor() {
        // If user overrides the button background, ensure readable text.
        if (customButtonColor != null) {
            return getContrastingTextColor(customButtonColor);
        }
        return isDarkMode() ? Dark.BUTTON_FG : Light.BUTTON_FG;
    }

    public static Color getButtonHoverColor() {
        return getButtonHoverColor(getButtonBackgroundColor());
    }

    public static Color getButtonPressedColor() {
        return getButtonPressedColor(getButtonBackgroundColor());
    }

    public static Color getButtonDisabledBackgroundColor() {
        return isDarkMode() ? Dark.BUTTON_DISABLED_BG : Light.BUTTON_DISABLED_BG;
    }

    public static Color getButtonDisabledForegroundColor() {
        return isDarkMode() ? Dark.BUTTON_DISABLED_FG : Light.BUTTON_DISABLED_FG;
    }

    public static Color getSuccessColor() {
        return isDarkMode() ? Dark.SUCCESS : Light.SUCCESS;
    }

    public static Color getErrorColor() {
        return isDarkMode() ? Dark.ERROR : Light.ERROR;
    }

    public static Color getInfoColor() {
        return isDarkMode() ? Dark.INFO : Light.INFO;
    }

    public static Color getTextHighlightBackgroundColor() {
        return isDarkMode() ? Dark.TEXT_HIGHLIGHT_BG : Light.TEXT_HIGHLIGHT_BG;
    }

    public static Color getTooltipBackgroundColor() {
        return isDarkMode() ? Dark.TOOLTIP_BG : Light.TOOLTIP_BG;
    }

    public static Color getTooltipForegroundColor() {
        return isDarkMode() ? Dark.TOOLTIP_FG : Light.TOOLTIP_FG;
    }

    public static Color getScrollbarBackgroundColor() {
        return isDarkMode() ? Dark.SCROLLBAR_BG : Light.SCROLLBAR_BG;
    }

    public static Color getScrollbarForegroundColor() {
        return isDarkMode() ? Dark.SCROLLBAR_FG : Light.SCROLLBAR_FG;
    }

    public static Color getScrollbarThumbColor() {
        return isDarkMode() ? Dark.SCROLLBAR_THUMB : Light.SCROLLBAR_THUMB;
    }

    public static Color getSuccessBackgroundColor() {
        return isDarkMode() ? new Color(46, 64, 54) : new Color(212, 237, 218);
    }

    public static Color getErrorBackgroundColor() {
        return isDarkMode() ? new Color(78, 36, 36) : new Color(248, 215, 218);
    }

    public static Color getInfoBackgroundColor() {
        return isDarkMode() ? new Color(36, 64, 78) : new Color(209, 236, 241);
    }

    public static Color getWarningBackgroundColor() {
        return isDarkMode() ? new Color(78, 64, 36) : new Color(255, 243, 205);
    }

    public static Color getSelectionBackgroundColor() {
        return isDarkMode() ? Dark.SELECTION_BG : Light.SELECTION_BG;
    }

    public static Color getSelectionForegroundColor() {
        return isDarkMode() ? Dark.SELECTION_FG : Light.SELECTION_FG;
    }

    public static Color getInputDisabledBackgroundColor() {
        return isDarkMode() ? Dark.INPUT_DISABLED_BG : Light.INPUT_DISABLED_BG;
    }

    public static Color getInputDisabledForegroundColor() {
        return isDarkMode() ? Dark.TEXT_DISABLED : Light.TEXT_DISABLED;
    }

    public static Color getBorderFocusColor() {
        return isDarkMode() ? Dark.BORDER_FOCUS : Light.BORDER_FOCUS;
    }

    public static Color getSuccessForegroundColor() {
        return isDarkMode() ? Dark.SUCCESS : Light.SUCCESS;
    }

    public static Color getWarningForegroundColor() {
        return isDarkMode() ? Dark.WARNING : Light.WARNING;
    }

    public static Color getErrorForegroundColor() {
        return isDarkMode() ? Dark.ERROR : Light.ERROR;
    }

    public static Color getInfoForegroundColor() {
        return isDarkMode() ? Dark.INFO : Light.INFO;
    }

    public static Color getSuccessBorderColor() {
        return isDarkMode() ? new Color(56, 142, 60) : new Color(40, 167, 69);
    }

    public static Color getBackgroundColor() {
        return isDarkMode() ? Dark.BACKGROUND : Light.BACKGROUND;
    }

    public Color getBackground() {
        return getBackgroundColor();
    }

    public static Color getCardBackgroundColor() {
        return isDarkMode() ? Dark.CARD_BACKGROUND : Light.CARD_BACKGROUND;
    }

    public static Color getTextPrimaryColor() {
        return isDarkMode() ? Dark.TEXT_PRIMARY : Light.TEXT_PRIMARY;
    }

    public static Color getTextSecondaryColor() {
        return isDarkMode() ? Dark.TEXT_SECONDARY : Light.TEXT_SECONDARY;
    }

    public static Color getBorderColor() {
        return isDarkMode() ? Dark.BORDER : Light.BORDER;
    }

    public static Color getHeaderBackgroundColor() {
        return customHeaderColor != null ? customHeaderColor : (isDarkMode() ? Dark.HEADER_BG : Light.HEADER_BG);
    }

    public static Color getTableHeaderBackgroundColor() {
        return isDarkMode() ? Dark.TABLE_HEADER_BG : Light.TABLE_HEADER_BG;
    }

    public static Color getTableHeaderForegroundColor() {
        return isDarkMode() ? Dark.TABLE_HEADER_FG : Light.TABLE_HEADER_FG;
    }

    public static Color getTableGridColor() {
        return isDarkMode() ? Dark.TABLE_GRID : Light.TABLE_GRID;
    }

    public static Color getTableRowEvenColor() {
        return isDarkMode() ? Dark.TABLE_ROW_EVEN : Light.TABLE_ROW_EVEN;
    }

    public static Color getTableRowOddColor() {
        return isDarkMode() ? Dark.TABLE_ROW_ODD : Light.TABLE_ROW_ODD;
    }

    public static Color getInputBackgroundColor() {
        return isDarkMode() ? Dark.INPUT_BG : Light.INPUT_BG;
    }

    public static Color getInputBorderColor() {
        return isDarkMode() ? Dark.INPUT_BORDER : Light.INPUT_BORDER;
    }

    public static Color getInputFocusBorderColor() {
        return isDarkMode() ? Dark.INPUT_FOCUS_BORDER : Light.INPUT_FOCUS_BORDER;
    }

    public static Color getPrimaryColor() {
        return getHeaderBackgroundColor();
    }

    public static Color getAccentColor() {
        return customAccentColor != null ? customAccentColor : (isDarkMode() ? Dark.TEXT_LINK : Light.TEXT_LINK);
    }

    public static Color getTableSelectionColor() {
        Color accent = getAccentColor();
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60);
    }

    public static Color getTextOnPrimaryColor() {
        return Color.WHITE;
    }

    public static Color getTableHeaderColor() {
        return getTableHeaderBackgroundColor();
    }

    public static Color getButtonHoverColor(Color baseColor) {
        return new Color(
                Math.min(255, baseColor.getRed() + 20),
                Math.min(255, baseColor.getGreen() + 20),
                Math.min(255, baseColor.getBlue() + 20));
    }

    public static Color getButtonPressedColor(Color baseColor) {
        return new Color(
                Math.max(0, baseColor.getRed() - 40),
                Math.max(0, baseColor.getGreen() - 40),
                Math.max(0, baseColor.getBlue() - 40));
    }

    public static Color getBorderLightColor() {
        return isDarkMode() ? new Color(70, 70, 70) : new Color(233, 236, 239);
    }

    public static Color getInputBorderFocusColor() {
        return getInputFocusBorderColor();
    }

    public static Color getDangerColor() {
        return new Color(220, 53, 69);
    }

    public static Color getWarningColor() {
        return new Color(255, 193, 7);
    }

    public static Color getSecondaryColor() {
        return new Color(108, 117, 125);
    }

    public static Color getTitleTextColor() {
        return isDarkMode() ? Dark.TITLE_TEXT : Light.TITLE_TEXT;
    }

    public static Color getTitleTextHighlightColor() {
        return isDarkMode() ? Dark.TITLE_TEXT_HIGHLIGHT : Light.TITLE_TEXT_HIGHLIGHT;
    }

    public static Color getTitleTextHighlightDarkColor() {
        return isDarkMode() ? Dark.TITLE_TEXT_HIGHLIGHT_DARK : Light.TITLE_TEXT_HIGHLIGHT_DARK;
    }

    public static Color getSubTitleTextColor() {
        return isDarkMode() ? Dark.SUB_TITLE_TEXT : Light.SUB_TITLE_TEXT;
    }

    public static Color withAlpha(Color base, int alpha) {
        if (base == null) {
            return new Color(0, 0, 0, alpha);
        }
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
    }

    public static Color getHeaderGradientColor() {
        Color base = getHeaderBackgroundColor();
        int delta = isDarkMode() ? 20 : -20;
        return adjustColor(base, delta);
    }

    public static Color adjustColor(Color base, int delta) {
        if (base == null) {
            return null;
        }
        return new Color(
                clamp(base.getRed() + delta),
                clamp(base.getGreen() + delta),
                clamp(base.getBlue() + delta));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public static Color getInfoBorderColor() {
        return isDarkMode() ? new Color(2, 136, 209) : new Color(23, 162, 184);
    }

    public static Color getTextPrimaryDarkerColor() {
        return isDarkMode() ? Dark.TEXT_PRIMARY_DARKER : Light.TEXT_PRIMARY_DARKER;
    }

    public static Color getSurfaceDarkerColor() {
        return isDarkMode() ? Dark.SURFACE_DARKER : Light.SURFACE_DARKER;
    }

    private static Color getContrastingTextColor(Color bg) {
        if (bg == null)
            return Color.WHITE;

        // Relative luminance (sRGB)
        double r = bg.getRed() / 255.0;
        double g = bg.getGreen() / 255.0;
        double b = bg.getBlue() / 255.0;

        r = (r <= 0.03928) ? (r / 12.92) : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? (g / 12.92) : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? (b / 12.92) : Math.pow((b + 0.055) / 1.055, 2.4);

        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance > 0.5 ? Color.BLACK : Color.WHITE;
    }
}
