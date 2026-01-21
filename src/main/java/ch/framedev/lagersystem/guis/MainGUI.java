package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Modern Main Dashboard for VEBO Lagersystem with Tabbed Interface
 */
public class MainGUI extends JFrame {

    private JTabbedPane tabbedPane;

    // Keep references so content stays alive and doesn't get disposed accidentally
    public static ArticleGUI articleGUI;
    private VendorGUI vendorGUI;
    private OrderGUI orderGUI;
    private ClientGUI clientGUI;

    // Tab wrappers
    private final JPanel articleWrapper = createTabWrapper();
    private final JPanel vendorWrapper  = createTabWrapper();
    private final JPanel orderWrapper   = createTabWrapper();
    private final JPanel clientWrapper  = createTabWrapper();

    public MainGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        ThemeManager.JOptionPaneTheme.apply();

        setTitle("VEBO Lagersystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(Main.icon.getImage());
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeManager.getBackgroundColor());

        // ===== HEADER =====
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ===== TABS =====
        initializeTabbedPane();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // ===== FOOTER =====
        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    /**
     * Creates a wrapper panel with padding for tab content
     */
    private static JPanel createTabWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeManager.getBackgroundColor());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return wrapper;
    }

    /**
     * Loads the content for a specific tab into the wrapper.
     * IMPORTANT: We do NOT dispose the child frames. Disposing kills listeners/resources.
     */
    private void loadTabContent(int tabIndex, JPanel wrapper) {
        wrapper.removeAll();

        JFrame frame = switch (tabIndex) {
            case 0 -> {
                if (articleGUI == null) articleGUI = new ArticleGUI();
                yield articleGUI;
            }
            case 1 -> {
                if (vendorGUI == null) vendorGUI = new VendorGUI();
                yield vendorGUI;
            }
            case 2 -> {
                if (orderGUI == null) orderGUI = new OrderGUI();
                yield orderGUI;
            }
            case 3 -> {
                if (clientGUI == null) clientGUI = new ClientGUI();
                yield clientGUI;
            }
            default -> null;
        };

        if (frame == null) return;

        // Keep it from behaving like an independent window
        frame.setVisible(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Extract content panel (safe)
        Container cp = frame.getContentPane();
        if (cp instanceof JPanel panel) {
            wrapper.add(panel, BorderLayout.CENTER);
        } else {
            JPanel fallback = new JPanel(new BorderLayout());
            fallback.setOpaque(false);
            fallback.add(cp, BorderLayout.CENTER);
            wrapper.add(fallback, BorderLayout.CENTER);
        }

        wrapper.revalidate();
        wrapper.repaint();
    }

    /**
     * Creates the header panel with title, subtitle, settings button, and date
     */
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.getHeaderBackgroundColor());
        headerPanel.setPreferredSize(new Dimension(0, 130));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(25, 50, 25, 50)
        ));

        // Left side: Title and subtitle
        JPanel headerTextPanel = createHeaderTextPanel();
        headerPanel.add(headerTextPanel, BorderLayout.WEST);

        // Right side: Settings button and date
        JPanel rightPanel = createHeaderRightPanel();
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the left side of the header with title and subtitle
     */
    private JPanel createHeaderTextPanel() {
        JLabel titleLabel = new JLabel("VEBO Lagersystem");
        Font titleFont = SettingsGUI.getFontByName(Font.BOLD, 48);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(ThemeManager.getHeaderForegroundColor());
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subtitleLabel = new JLabel("Zentrale Verwaltung für Artikel, Bestellungen und Lieferanten");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        Color subtitleColor = ThemeManager.isDarkMode()
            ? new Color(200, 220, 240)
            : new Color(255, 255, 255, 220);
        subtitleLabel.setForeground(subtitleColor);
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(Box.createVerticalStrut(6));
        headerTextPanel.add(subtitleLabel);

        return headerTextPanel;
    }

    /**
     * Creates the right side of the header with settings button and date
     */
    private JPanel createHeaderRightPanel() {
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setOpaque(false);

        JButton settingsButton = new JButton(UnicodeSymbols.BETTER_GEAR + " Einstellungen");
        styleHeaderButton(settingsButton);
        settingsButton.addActionListener(e -> {
            SettingsGUI settingsGUI = new SettingsGUI();
            settingsGUI.display();
        });

        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, dd. MMMM yyyy").format(new Date()));
        dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        Color dateColor = ThemeManager.isDarkMode()
            ? new Color(200, 220, 240, 200)
            : new Color(255, 255, 255, 200);
        dateLabel.setForeground(dateColor);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        rightPanel.add(settingsButton);
        rightPanel.add(dateLabel);

        return rightPanel;
    }

    /**
     * Initializes the tabbed pane with all tabs and lazy loading
     */
    private void initializeTabbedPane() {
        int fontSizeTab = getTabFontSize();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(SettingsGUI.getFontByName(Font.BOLD, fontSizeTab));
        tabbedPane.setBackground(ThemeManager.getBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Add tabs
        addTabs();

        // Apply theme-safe tab backgrounds and styling
        applyTabBackgrounds();
        applyTabStyling();

        // Setup lazy loading
        setupLazyLoading();

        // Load first tab immediately
        loadTabContent(0, articleWrapper);
        articleWrapper.putClientProperty("loaded", Boolean.TRUE);
    }

    /**
     * Gets the tab font size from settings
     */
    private int getTabFontSize() {
        String fontSizeTabStr = Main.settings.getProperty("table_font_size_tab");
        int fontSizeTab = 15;
        try {
            fontSizeTab = Integer.parseInt(fontSizeTabStr);
        } catch (NumberFormatException ignored) {
        }
        return fontSizeTab;
    }

    /**
     * Adds all tabs to the tabbed pane
     */
    private void addTabs() {
        tabbedPane.addTab("   " + UnicodeSymbols.PACKAGE + " Artikel   ", null, articleWrapper, "Artikelverwaltung");
        tabbedPane.addTab("   " + UnicodeSymbols.TRUCK + " Lieferanten   ", null, vendorWrapper, "Lieferantenverwaltung");
        tabbedPane.addTab("   " + UnicodeSymbols.CLIPBOARD + " Bestellungen   ", null, orderWrapper, "Bestellungsverwaltung");
        tabbedPane.addTab("   " + UnicodeSymbols.PEOPLE + " Kunden   ", null, clientWrapper, "Kundenverwaltung");
    }

    /**
     * Applies theme-safe subtle backgrounds to tabs
     */
    private void applyTabBackgrounds() {
        Color tabTint = ThemeManager.isDarkMode()
                ? new Color(255, 255, 255, 16)
                : new Color(0, 0, 0, 12);

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setBackgroundAt(i, tabTint);
        }
    }

    /**
     * Applies modern styling to tab UI (padding, borders, shadows)
     */
    private void applyTabStyling() {
        // Set component background for better visual separation
        tabbedPane.setOpaque(true);

        // Style the tab area with subtle borders
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
    }

    /**
     * Sets up lazy loading for tabs
     */
    private void setupLazyLoading() {
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            JPanel wrapper = (JPanel) tabbedPane.getComponentAt(idx);
            if (wrapper.getClientProperty("loaded") == null) {
                loadTabContent(idx, wrapper);
                wrapper.putClientProperty("loaded", Boolean.TRUE);
            }
        });
    }

    /**
     * Creates the footer panel with copyright information
     */
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(ThemeManager.getBackgroundColor());
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(12, 0, 12, 0)
        ));

        JLabel footerLabel = new JLabel("© 2026 VEBO Lagersystem | Entwickelt von Darryl Huber | Version " + Main.VERSION);
        footerLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        footerLabel.setForeground(ThemeManager.getTextSecondaryColor());
        footerPanel.add(footerLabel);

        return footerPanel;
    }

    /**
     * Styles a header button with theme colors and hover effects
     */
    private void styleHeaderButton(JButton button) {
        Color base = ThemeManager.getAccentColor();
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);

        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBackground(base);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(hover);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hover.darker(), 2),
                        BorderFactory.createEmptyBorder(9, 19, 9, 19)
                ));
            }

            @Override
            public void mouseExited(MouseEvent evt) {
                button.setBackground(base);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(base.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mousePressed(MouseEvent evt) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent evt) {
                button.setBackground(button.contains(evt.getPoint()) ? hover : base);
            }
        });
    }

    public void display() {
        setVisible(true);
    }
}