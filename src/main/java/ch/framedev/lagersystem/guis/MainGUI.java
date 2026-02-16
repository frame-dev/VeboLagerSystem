package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

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
    private SupplierOrderGUI supplierOrderGUI;
    private LogsGUI logsGUI;
    private SettingsGUI settingsGUI;
    private NotesGUI notesGUI;

    // Tab wrappers
    private final JPanel articleWrapper = createTabWrapper();
    private final JPanel vendorWrapper  = createTabWrapper();
    private final JPanel orderWrapper   = createTabWrapper();
    private final JPanel clientWrapper  = createTabWrapper();
    private final JPanel supplierOrderWrapper = createTabWrapper();
    private final JPanel logsWrapper = createTabWrapper();

    public MainGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        ThemeManager.JOptionPaneTheme.apply();

        setTitle("VEBO Lagersystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        if (Main.icon != null) {
            setIconImage(Main.icon.getImage());
        }
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
            case 4 -> {
                if( supplierOrderGUI == null) supplierOrderGUI = new SupplierOrderGUI();
                yield supplierOrderGUI;
            }
            case 5 -> {
                if (logsGUI == null) logsGUI = new LogsGUI();
                yield logsGUI;
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
        // Create gradient background panel
        JFrameUtils.GradientPanel headerPanel = new JFrameUtils.GradientPanel(
            ThemeManager.getHeaderBackgroundColor(),
            ThemeManager.getHeaderGradientColor()
        );
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 180));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 0, 0, 30)),
                BorderFactory.createEmptyBorder(30, 60, 30, 60)
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
        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);

        // Add icon + title in horizontal layout
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);

        Font headerIconFont = SettingsGUI.getFontByName(Font.BOLD, 42);
        JLabel iconLabel = new JLabel(UnicodeSymbols.safeSymbol(UnicodeSymbols.PACKAGE, "PKG", headerIconFont));
        iconLabel.setFont(getEmojiCapableFont(headerIconFont));
        iconLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        titleRow.add(iconLabel);

        JLabel titleLabel = new JLabel("VEBO Lagersystem");
        Font titleFont = SettingsGUI.getFontByName(Font.BOLD, 52);
        titleLabel.setFont(titleFont);
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        titleRow.add(titleLabel);

        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerTextPanel.add(titleRow);

        headerTextPanel.add(Box.createVerticalStrut(2));

        // Subtitle aligned with the "V" in "VEBO" (after icon)
        JLabel subtitleLabel = new JLabel("Zentrale Verwaltung für Artikel, Bestellungen und Lieferanten");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        subtitleLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 230));

        // Create wrapper with FlowLayout matching the title row
        JPanel subtitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        subtitleRow.setOpaque(false);
        subtitleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add invisible spacer matching icon width (approximately 42px + 10px gap = 52px)
        subtitleRow.add(Box.createHorizontalStrut(12));
        subtitleRow.add(subtitleLabel);

        headerTextPanel.add(subtitleRow);

        return headerTextPanel;
    }

    /**
     * Creates the right side of the header with settings button and date
     */
    private JPanel createHeaderRightPanel() {
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.X_AXIS));
        rightPanel.setOpaque(false);

        Font headerButtonFont = SettingsGUI.getFontByName(Font.BOLD, 12);
        JButton settingsButton = new JButton(UnicodeSymbols.safeSymbol(UnicodeSymbols.BETTER_GEAR, "CFG", headerButtonFont) + " Einstellungen");
        styleHeaderButton(settingsButton);
        settingsButton.setToolTipText("Einstellungen des Programms öffnen");
        settingsButton.addActionListener(e -> {
            settingsGUI = showOrCreateWindow(settingsGUI, SettingsGUI::new);
        });
        settingsButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
        JButton notesButton = new JButton(UnicodeSymbols.safeSymbol(UnicodeSymbols.CLIPBOARD, "CLIP", headerButtonFont) + " Notizen");
        styleHeaderButton(notesButton);
        notesButton.setToolTipText("Persönliche Notizen verwalten");
        notesButton.addActionListener(e -> {
            notesGUI = showOrCreateWindow(notesGUI, NotesGUI::new);
        });
        notesButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

        JLabel dateLabel = new JLabel(new SimpleDateFormat("EEEE, dd. MMMM yyyy").format(new Date()));
        dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        dateLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        dateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        rightPanel.add(notesButton);
        rightPanel.add(Box.createHorizontalStrut(8));
        rightPanel.add(settingsButton);
        rightPanel.add(Box.createHorizontalStrut(10));
        rightPanel.add(dateLabel);

        return rightPanel;
    }

    /**
     * Initializes the tabbed pane with all tabs and lazy loading
     */
    private void initializeTabbedPane() {
        int fontSizeTab = getTabFontSize();

        tabbedPane = new JTabbedPane(JTabbedPane.TOP) {
            @Override
            public void updateUI() {
                UIManager.put("TabbedPane.tabAreaBackground", ThemeManager.getBackgroundColor());
                UIManager.put("TabbedPane.selected", ThemeManager.getAccentColor());
                UIManager.put("TabbedPane.focus", ThemeManager.getAccentColor().darker());
                UIManager.put("TabbedPane.borderHightlightColor", ThemeManager.getBorderColor()); // note: typo in key, see below
                UIManager.put("TabbedPane.lightHighlight", ThemeManager.getBorderColor().brighter());
                UIManager.put("TabbedPane.shadow", ThemeManager.getBorderColor().darker());
                UIManager.put("TabbedPane.darkShadow", ThemeManager.getBorderColor().darker().darker());

                // This affects height, but only if the current LAF/UI delegate honors it
                // UIManager.put("TabbedPane.tabInsets", new Insets(25, 25, 25, 25));

                super.updateUI();

                // Force layout recalc
                revalidate();
                repaint();
                this.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.BOLD, fontSizeTab + 3)));
            }
        };
        /**tabbedPane.setUI(new BasicTabbedPaneUI() {

            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                // your desired tab height
                return 30;
            }
        });*/

        // Trigger UI update
        tabbedPane.updateUI();

        // Use larger font for bigger tabs
        //tabbedPane.setFont(SettingsGUI.getFontByName(Font.BOLD, fontSizeTab + 2));
        tabbedPane.setBackground(ThemeManager.getBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());

        // Enhanced padding and border for modern look with bigger tabs
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 15, 8, 15),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                )
        ));

        // Add tabs
        addTabs();

        // Apply theme-safe tab backgrounds and styling
        applyTabBackgrounds();
        applyTabStyling();

        // Setup lazy loading
        setupLazyLoading();

        // Load the first tab immediately
        loadTabContent(0, articleWrapper);
        articleWrapper.putClientProperty("loaded", Boolean.TRUE);
        setJMenuBar(createMenuBar());
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
        Font tabFont = tabbedPane.getFont();
        String tabPackage = UnicodeSymbols.safeSymbol(UnicodeSymbols.PACKAGE, "PKG", tabFont);
        String tabTruck = UnicodeSymbols.safeSymbol(UnicodeSymbols.TRUCK, "TRK", tabFont);
        String tabClipboard = UnicodeSymbols.safeSymbol(UnicodeSymbols.CLIPBOARD, "CLIP", tabFont);
        String tabPeople = UnicodeSymbols.safeSymbol(UnicodeSymbols.PEOPLE, "USERS", tabFont);
        // Add extra spacing for bigger, more prominent tabs
        tabbedPane.addTab("<html>     " + tabPackage + "  Artikel     </html>", null, articleWrapper, "Artikelverwaltung");
        tabbedPane.addTab("<html>     " + tabTruck + "  Lieferanten     </html>", null, vendorWrapper, "Lieferantenverwaltung");
        tabbedPane.addTab("<html>     " + tabClipboard + "  Bestellungen     </html>", null, orderWrapper, "Bestellungsverwaltung");
        tabbedPane.addTab("<html>     " + tabPeople + "  Kunden     </html>", null, clientWrapper, "Kundenverwaltung");
        tabbedPane.addTab("<html>     " + tabTruck + tabPackage + "  Lieferantenbestellungen     </html>", null, supplierOrderWrapper, "Lieferantenbestellungen verwalten");
        tabbedPane.addTab("<html>     " + tabClipboard + " Protokolle     </html>", null, logsWrapper, "Systemprotokolle anzeigen");
    }

    /**
     * Applies theme-safe subtle backgrounds to tabs
     */
    private void applyTabBackgrounds() {
        // More visible and attractive tab backgrounds with better contrast
        Color unselectedBg = ThemeManager.getSurfaceColor();
        Color selectedBg = ThemeManager.getCardBackgroundColor();

        // Set initial background colors
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (i == tabbedPane.getSelectedIndex()) {
                tabbedPane.setBackgroundAt(i, selectedBg);
                tabbedPane.setForegroundAt(i, ThemeManager.getTextPrimaryColor());
            } else {
                tabbedPane.setBackgroundAt(i, unselectedBg);
                tabbedPane.setForegroundAt(i, ThemeManager.getTextSecondaryColor());
            }
        }
    }

    /**
     * Applies modern styling to tab UI (padding, borders, shadows)
     */
    private void applyTabStyling() {
        // Set component background for better visual separation
        tabbedPane.setOpaque(true);

        // Enhanced border with a rounded appearance
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(4, 4, 4, 4)
                ),
                BorderFactory.createLineBorder(
                        ThemeManager.adjustColor(ThemeManager.getBorderColor(), ThemeManager.isDarkMode() ? 20 : -10), 1)
        ));
    }

    /**
     * Sets up lazy loading for tabs
     */
    private void setupLazyLoading() {
        // Define colors for tab backgrounds
        Color unselectedBg = ThemeManager.getSurfaceColor();
        Color selectedBg = ThemeManager.getCardBackgroundColor();

        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();

            // Update tab backgrounds when switching
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (i == idx) {
                    tabbedPane.setBackgroundAt(i, selectedBg);
                    tabbedPane.setForegroundAt(i, ThemeManager.getTextPrimaryColor());
                } else {
                    tabbedPane.setBackgroundAt(i, unselectedBg);
                    tabbedPane.setForegroundAt(i, ThemeManager.getTextSecondaryColor());
                }
            }

            // Lazy load content
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

        button.setFont(getEmojiCapableFont(SettingsGUI.getFontByName(Font.BOLD, 12)));
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
                        BorderFactory.createEmptyBorder(8, 18, 8, 18)
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

    private static Font getEmojiCapableFont(Font baseFont) {
        if (baseFont == null) {
            return null;
        }
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            // Use logical font to allow emoji fallback on macOS.
            return new Font("Dialog", baseFont.getStyle(), baseFont.getSize());
        }
        return baseFont;
    }

    /**
     * Displays the main GUI window by making it visible.
     * This method sets the visibility of the {@code MainGUI} frame to {@code true},
     * allowing the user to interact with the application's graphical interface.
     */
    public void display() {
        setVisible(true);
    }

    private <T extends JFrame> T showOrCreateWindow(T existing, Supplier<T> factory) {
        T window = existing;
        if (window == null || !window.isDisplayable()) {
            window = factory.get();
        }
        if (!window.isVisible()) {
            window.setVisible(true);
        }
        window.toFront();
        window.requestFocus();
        return window;
    }

    /**
     * Creates and configures the menu bar for the application.
     * The menu bar includes a "Werkzeuge" menu with options for generating QR codes
     * for all articles or for selected articles. Each menu item is associated with
     * its respective action.
     *
     * @return the configured {@code JMenuBar} instance
     */
    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu toolsMenu = new JMenu("Werkzeuge");
        JMenuItem qrCodesGeneratorMenuItem = new JMenuItem("QR-Codes generieren");
        qrCodesGeneratorMenuItem.addActionListener(e -> generateQrCodesForArticles());
        JMenuItem qrCodesSelectedMenuItem = new JMenuItem("QR-Codes fuer Auswahl");
        qrCodesSelectedMenuItem.addActionListener(e -> generateQrCodesForSelectedArticles());
        toolsMenu.add(qrCodesGeneratorMenuItem);
        toolsMenu.add(qrCodesSelectedMenuItem);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    /**
     * Generates QR codes for all articles in the application.
     *
     * This method retrieves the full list of articles using the article manager.
     * If no articles are available, it displays a warning dialog to inform the user.
     * Otherwise, it invokes the {@code generateQrCodesForList} method to generate
     * QR codes for the retrieved list of articles with a confirmation prompt.
     *
     * The confirmation prompt text is localized and asks the user whether they want
     * to generate QR codes for all articles.
     */
    private void generateQrCodesForArticles() {
        List<Article> articles = ArticleManager.getInstance().getAllArticles();
        if (articles == null || articles.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Keine Artikel vorhanden.",
                    "QR-Codes generieren",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        generateQrCodesForList(articles, "QR-Codes fuer alle Artikel generieren?");
    }

    /**
     * Generates QR codes for the selected articles within the application.
     *
     * This method retrieves a list of selected articles from the article GUI and
     * initiates QR code generation if a valid selection is made. If the article
     * GUI is not initialized or no articles are selected, it displays a warning
     * dialog to notify the user of the missing selection.
     *
     * The confirmation prompt includes the total count of selected articles, asking
     * the user for confirmation before proceeding with the QR code generation.
     *
     * Preconditions:
     * - The article GUI tab must be opened, and at least one article should be selected.
     *
     * Postconditions:
     * - Invokes the {@code generateQrCodesForList} method to generate QR codes
     *   for the list of selected articles, following confirmation from the user.
     *
     * Warning messages:
     * - If the article GUI is not initialized, a dialog indicates that the article
     *   tab must be opened.
     * - If no articles are selected, a dialog indicates that at least one article
     *   must be chosen.
     */
    private void generateQrCodesForSelectedArticles() {
        if (articleGUI == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bitte zuerst den Artikel-Tab oeffnen und eine Auswahl treffen.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        List<Article> selectedArticles = articleGUI.getSelectedArticles();
        if (selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bitte waehlen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        generateQrCodesForList(selectedArticles, "QR-Codes fuer " + selectedArticles.size() + " ausgewählte Artikel generieren?");
    }

    /**
     * Generates QR codes for a given list of articles after displaying a confirmation dialog.
     *
     * This method allows the user to confirm whether they want to proceed with generating QR codes
     * for the provided list of articles. A progress dialog is displayed during the generation process.
     * Upon completion, the user is notified about the result, including the number of QR codes
     * generated and the output directory.
     *
     * @param articles the list of articles for which QR codes will be generated
     * @param promptText the text to display in the confirmation dialog prompt
     */
    private void generateQrCodesForList(List<Article> articles, String promptText) {
        File outputDir = new File(Main.getAppDataDir(), "qr_codes");
        int result = JOptionPane.showConfirmDialog(
                this,
                promptText + "\nSpeicherort: " + outputDir.getAbsolutePath(),
                "QR-Codes generieren",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        JDialog progressDialog = new JDialog(this, "QR-Codes generieren", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout());
        JLabel label = new JLabel("Bitte warten, QR-Codes werden erstellt...");
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(label, BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<List<File>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<File> doInBackground() {
                return QRCodeUtils.createQrCodes(articles);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<File> files = get();
                    if (files == null || files.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                MainGUI.this,
                                "Es konnten keine QR-Codes erstellt werden.",
                                "QR-Codes generieren",
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }
                    JOptionPane.showMessageDialog(
                            MainGUI.this,
                            "QR-Codes wurden erstellt.\nAnzahl: " + files.size() + "\nOrdner: " + outputDir.getAbsolutePath(),
                            "QR-Codes generieren",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            MainGUI.this,
                            "Fehler beim Erstellen der QR-Codes: " + ex.getMessage(),
                            "QR-Codes generieren",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }
}
