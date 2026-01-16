package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized theme management for the application.
 * Provides color schemes for both light and dark themes.
 * Supports both static utility methods and instance-based theme management.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private static Theme currentTheme = Theme.LIGHT;
    private final List<Window> registeredWindows = new ArrayList<>();

    /**
     * Theme enumeration
     */
    public enum Theme {
        LIGHT, DARK
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
        public static final Color TITLE_TEXT = new Color(26, 26, 26);
        public static final Color TITLE_TEXT_HIGHLIGHT = new Color(52, 152, 219);
        public static final Color TITLE_TEXT_HIGHLIGHT_DARK = new Color(41, 128, 185);

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

    private ThemeManager() {
        initialize();
        applyUIDefaults();
    }

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

    // =========================
    // Theme init / persistence
    // =========================

    public static void initialize() {
        if (Main.settings != null) {
            String darkModeStr = Main.settings.getProperty("dark_mode");
            boolean darkMode = Boolean.parseBoolean(darkModeStr);
            currentTheme = darkMode ? Theme.DARK : Theme.LIGHT;
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        currentTheme = theme;

        if (Main.settings != null) {
            Main.settings.setProperty("dark_mode", String.valueOf(theme == Theme.DARK));
        }

        applyUIDefaults();
        updateAllWindows();
    }

    public static boolean isDarkMode() {
        return currentTheme == Theme.DARK;
    }

    public static void setDarkMode(boolean enabled) {
        getInstance().setTheme(enabled ? Theme.DARK : Theme.LIGHT);
    }

    public static final class JOptionPaneTheme {

        private JOptionPaneTheme() {}

        public static void apply() {
            // Base colors
            Color bg = ThemeManager.getCardBackgroundColor();          // dialog panel background
            Color surfaceBg = ThemeManager.getBackgroundColor();       // surface/window background
            Color fg = ThemeManager.getTextPrimaryColor();             // main text color
            Color secondaryFg = ThemeManager.getTextSecondaryColor();  // secondary text
            Color border = ThemeManager.getBorderColor();

            // Inputs
            Color inputBg = ThemeManager.getInputBackgroundColor();
            Color inputFg = ThemeManager.getTextPrimaryColor();
            Color caret = ThemeManager.getTextPrimaryColor();

            // Buttons - use proper button colors with white text for visibility
            Color btnBg = ThemeManager.getButtonBackgroundColor();
            Color btnFg = Color.WHITE;  // Always white text on blue buttons for best contrast
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
            UIManager.put("Button.focus", new ColorUIResource(btnBg));  // Keep original on focus

            // Hover effect colors
            Color btnHoverBg = btnHover;
            UIManager.put("Button.highlight", new ColorUIResource(btnHoverBg));
            UIManager.put("Button.light", new ColorUIResource(btnHoverBg));
            UIManager.put("Button.shadow", new ColorUIResource(btnPressed));
            UIManager.put("Button.darkShadow", new ColorUIResource(btnPressed.darker()));

            // Create prettier button border with subtle shadow effect
            Color btnBorderColor = ThemeManager.isDarkMode()
                ? new Color(Math.max(0, btnBg.getRed() - 30), Math.max(0, btnBg.getGreen() - 30), Math.max(0, btnBg.getBlue() - 30))
                : new Color(Math.max(0, btnBg.getRed() - 20), Math.max(0, btnBg.getGreen() - 20), Math.max(0, btnBg.getBlue() - 20));

            UIManager.put("Button.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(btnBorderColor, 1, true),
                        BorderFactory.createEmptyBorder(1, 1, 2, 1)  // Subtle shadow for depth
                    ),
                    BorderFactory.createEmptyBorder(8, 20, 8, 20)
            ));
            UIManager.put("Button.margin", new Insets(8, 20, 8, 20));
            UIManager.put("Button.opaque", Boolean.TRUE);
            UIManager.put("Button.contentAreaFilled", Boolean.TRUE);

            // Enable mouse hover effects
            UIManager.put("Button.rolloverEnabled", Boolean.TRUE);
            UIManager.put("Button.rollover", new ColorUIResource(btnHoverBg));

            // Font styling for buttons
            UIManager.put("Button.font", new Font("Arial", Font.BOLD, 13));

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
            UIManager.put("Table.background", new ColorUIResource(inputBg));
            UIManager.put("Table.foreground", new ColorUIResource(fg));
            UIManager.put("Table.selectionBackground", new ColorUIResource(selBg));
            UIManager.put("Table.selectionForeground", new ColorUIResource(selFg));
            UIManager.put("Table.gridColor", new ColorUIResource(border));
            UIManager.put("TableHeader.background", new ColorUIResource(bg));
            UIManager.put("TableHeader.foreground", new ColorUIResource(fg));

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
        }
    }

    // =========================
    // UIManager defaults
    // =========================

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

        // ---------- FileChooser ----------
        d.put("FileChooser.background", new ColorUIResource(bg));
        d.put("FileChooser.foreground", new ColorUIResource(fg));

        // These vary by LAF but help a lot:
        d.put("FileChooser.listViewBackground", new ColorUIResource(card));
        d.put("FileChooser.listViewForeground", new ColorUIResource(fg));
        d.put("FileChooser.detailsViewBackground", new ColorUIResource(card));
        d.put("FileChooser.detailsViewForeground", new ColorUIResource(fg));

        d.put("FileChooser.readOnly", Boolean.FALSE);

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
    }

    // =========================
    // Window registration / refresh
    // =========================

    public void registerWindow(Window window) {
        if (window != null && !registeredWindows.contains(window)) {
            registeredWindows.add(window);
        }
    }

    public void unregisterWindow(Window window) {
        registeredWindows.remove(window);
    }

    public void updateAllWindows() {
        registeredWindows.removeIf(w -> w == null || !w.isDisplayable());

        for (Window window : registeredWindows) {
            SwingUtilities.invokeLater(() -> {
                SwingUtilities.updateComponentTreeUI(window);
                updateComponentTree(window);
                window.revalidate();
                window.repaint();
            });
        }
    }

    private void updateComponentTree(Container container) {
        applyThemeToComponent(container);
        for (Component comp : container.getComponents()) {
            applyThemeToComponent(comp);
            if (comp instanceof Container) {
                updateComponentTree((Container) comp);
            }
        }
    }

    private void applyThemeToComponent(Component comp) {

        // Panels
        if (comp instanceof JPanel panel) {
            panel.setBackground(getBackgroundColor());
        }

        // Scroll panes: set viewport background too
        if (comp instanceof JScrollPane sp) {
            sp.setBackground(getBackgroundColor());
            if (sp.getViewport() != null) sp.getViewport().setBackground(getBackgroundColor());
            if (sp.getColumnHeader() != null) sp.getColumnHeader().setBackground(getBackgroundColor());
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

            if (jc instanceof JLabel || jc instanceof JButton || jc instanceof JCheckBox || jc instanceof JRadioButton) {
                jc.setForeground(enabled ? getTextPrimaryColor() : getDisabledForeground());
            }

            if (jc instanceof JTextField field) {
                field.setBackground(enabled ? getInputBackgroundColor() : getInputDisabledBackgroundColor());
                field.setForeground(enabled ? getTextPrimaryColor() : getDisabledForeground());
                field.setCaretColor(getTextPrimaryColor());
                field.setSelectionColor(getSelectionBackgroundColor());
                field.setSelectedTextColor(getSelectionForegroundColor());

                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(enabled ? getInputBorderColor() : getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)
                ));
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
        return isDarkMode() ? Dark.BUTTON_BG : Light.BUTTON_BG;
    }

    public static Color getButtonForegroundColor() {
        return isDarkMode() ? Dark.BUTTON_FG : Light.BUTTON_FG;
    }

    public static Color getButtonHoverColor() {
        return isDarkMode() ? Dark.BUTTON_HOVER : Light.BUTTON_HOVER;
    }

    public static Color getButtonPressedColor() {
        return isDarkMode() ? Dark.BUTTON_PRESSED : Light.BUTTON_PRESSED;
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
        return isDarkMode() ? Dark.HEADER_BG : Light.HEADER_BG;
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
        return isDarkMode() ? Dark.HEADER_BG : Light.HEADER_BG;
    }

    public static Color getAccentColor() {
        return new Color(52, 152, 219);
    }

    public static Color getTableSelectionColor() {
        return new Color(52, 152, 219, 60);
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
                Math.min(255, baseColor.getBlue() + 20)
        );
    }

    public static Color getButtonPressedColor(Color baseColor) {
        return new Color(
                Math.max(0, baseColor.getRed() - 40),
                Math.max(0, baseColor.getGreen() - 40),
                Math.max(0, baseColor.getBlue() - 40)
        );
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

    public static Color getInfoBorderColor() {
        return isDarkMode() ? new Color(2, 136, 209) : new Color(23, 162, 184);
    }

    public static Color getTextPrimaryDarkerColor() {
        return isDarkMode() ? Dark.TEXT_PRIMARY_DARKER : Light.TEXT_PRIMARY_DARKER;
    }

    public static Color getSurfaceDarkerColor() {
        return isDarkMode() ? Dark.SURFACE_DARKER : Light.SURFACE_DARKER;
    }
}