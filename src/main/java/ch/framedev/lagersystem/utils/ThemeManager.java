package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;

import javax.swing.*;
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

        // Text
        public static final Color TEXT_PRIMARY = new Color(31, 45, 61);
        public static final Color TEXT_SECONDARY = new Color(108, 117, 125);
        public static final Color TEXT_DISABLED = new Color(160, 170, 180);
        public static final Color TEXT_LINK = new Color(52, 152, 219);

        // Borders & dividers
        public static final Color BORDER = new Color(220, 225, 230);
        public static final Color BORDER_FOCUS = new Color(52, 152, 219);
        public static final Color DIVIDER = new Color(230, 234, 238);

        // Header & navigation
        public static final Color HEADER_BG = new Color(30, 58, 95);
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

        // Text
        public static final Color TEXT_PRIMARY = new Color(240, 240, 240);
        public static final Color TEXT_SECONDARY = new Color(180, 180, 180);
        public static final Color TEXT_DISABLED = new Color(120, 120, 120);
        public static final Color TEXT_LINK = new Color(100, 170, 255);

        // Borders & dividers
        public static final Color BORDER = new Color(60, 60, 60);
        public static final Color BORDER_FOCUS = new Color(52, 152, 219);
        public static final Color DIVIDER = new Color(70, 70, 70);

        // Header & navigation
        public static final Color HEADER_BG = new Color(25, 25, 25);
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
            boolean darkMode = darkModeStr != null && Boolean.parseBoolean(darkModeStr);
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

    // =========================
    // UIManager defaults
    // =========================

    public static void applyUIDefaults() {
        UIDefaults d = UIManager.getDefaults();

        // General
        d.put("control", getBackgroundColor());
        d.put("Panel.background", getBackgroundColor());
        d.put("Viewport.background", getBackgroundColor());

        // Labels
        d.put("Label.foreground", getTextPrimaryColor());

        // Buttons
        d.put("Button.background", getButtonBackgroundColor());
        d.put("Button.foreground", getButtonForegroundColor());
        d.put("Button.disabledText", getDisabledForeground());

        // Text components
        d.put("TextField.background", getInputBackgroundColor());
        d.put("TextField.foreground", getTextPrimaryColor());
        d.put("TextField.inactiveBackground", getInputDisabledBackgroundColor());
        d.put("TextField.inactiveForeground", getInputDisabledForegroundColor());

        d.put("TextArea.background", getInputBackgroundColor());
        d.put("TextArea.foreground", getTextPrimaryColor());
        d.put("TextArea.inactiveBackground", getInputDisabledBackgroundColor());
        d.put("TextArea.inactiveForeground", getInputDisabledForegroundColor());

        // Selection
        d.put("TextField.selectionBackground", getSelectionBackgroundColor());
        d.put("TextField.selectionForeground", getSelectionForegroundColor());
        d.put("TextArea.selectionBackground", getSelectionBackgroundColor());
        d.put("TextArea.selectionForeground", getSelectionForegroundColor());

        // Tables
        d.put("Table.background", getCardBackgroundColor());
        d.put("Table.foreground", getTextPrimaryColor());
        d.put("Table.gridColor", getTableGridColor());
        d.put("Table.selectionBackground", getTableRowSelectedColor());
        d.put("Table.selectionForeground", getTextPrimaryColor());

        d.put("TableHeader.background", getTableHeaderBackgroundColor());
        d.put("TableHeader.foreground", getTableHeaderForegroundColor());

        // Tooltips
        d.put("ToolTip.background", getTooltipBackgroundColor());
        d.put("ToolTip.foreground", getTooltipForegroundColor());

        // ScrollPane + ScrollBar (best-effort; LAF-dependent)
        d.put("ScrollPane.background", getBackgroundColor());
        d.put("ScrollBar.background", getScrollbarBackgroundColor());
        d.put("ScrollBar.foreground", getScrollbarForegroundColor());

        // Some LAFs use these keys
        d.put("ScrollBar.thumb", getScrollbarThumbColor());
        d.put("ScrollBar.thumbHighlight", getScrollbarThumbColor());
        d.put("ScrollBar.thumbDarkShadow", getScrollbarThumbColor());
        d.put("ScrollBar.track", getScrollbarBackgroundColor());
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
}