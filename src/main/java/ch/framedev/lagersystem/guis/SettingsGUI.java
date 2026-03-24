package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.ArticleUtils.CategoryRange;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.net.URI;
import java.util.*;
import java.util.List;
import static ch.framedev.lagersystem.utils.ArticleUtils.categories;
import static ch.framedev.lagersystem.utils.ArticleUtils.loadCategories;

/**
 * Moderne Einstellungen-GUI für das VEBO Lagersystem.
 *
 * <p>
 * Diese Ansicht kapselt:
 * <ul>
 * <li>System-/Automatisierungs-Einstellungen (z.B. Lagerbestandsprüfung,
 * Warnungen, QR-Import)</li>
 * <li>Darstellungs-Einstellungen (Theme, Farben, Schriftart,
 * Schriftgrößen)</li>
 * <li>Such-/Filter-Funktion über Einstellungsbereiche</li>
 * </ul>
 *
 * <p>
 * Änderungen werden typischerweise über {@code saveSettings()} persistiert und
 * nach dem Speichern
 * in der Anwendung angewendet.
 */
public class SettingsGUI extends JFrame {
    /**
     * Logger für UI- und Einstellungsaktionen innerhalb dieser GUI.
     */
    private final Logger logger = LogManager.getLogger(SettingsGUI.class);

    private JSpinner stockCheckIntervalSpinner;
    private JSpinner warningDisplayIntervalSpinner;
    private JCheckBox enableWarningDisplayCheckbox;
    private JCheckBox enableAutoStockCheckCheckbox;
    private JCheckBox automaticImportCheckBox;
    private JCheckBox darkModeCheckbox;
    private JSpinner qrCodeImportIntervalSpinner;
    private JSpinner fontSizeTableSpinner;
    private JSpinner fontSizeTabSpinner;
    private JTextField serverUrlField;
    private JComboBox<String> themeComboBox;
    private static JComboBox<String> fontComboBox;
    /**
     * Suchfeld zum Filtern/Anzeigen der Einstellungen innerhalb der Tabs.
     */
    private final JTextField settingsSearchField = new JTextField(24);
    /**
     * Platzhaltertext für {@link #settingsSearchField}, solange kein echter
     * Suchtext eingegeben wurde.
     */
    private static final String SEARCH_PLACEHOLDER = "Suche nach Einstellungen…";
    /**
     * {@code true}, wenn im Suchfeld aktuell nur der Platzhalter angezeigt wird.
     * Wird u.a. in Focus-Listenern und beim globalen Shortcut verwendet.
     */
    private boolean searchPlaceholderActive = false;
    /**
     * Sammlung von UI-Sektionen, die über die Suche gefiltert/gehervorgehoben
     * werden können.
     */
    private final List<JComponent> searchableSections = new ArrayList<>();
    private final JLabel previewTitleLabel = new JLabel("Vorschau");
    private final JLabel previewBodyLabel = new JLabel("Schrift und Farben werden hier angezeigt.");
    private final JTable previewTable = new JTable(
            new Object[][] {
                    { "A-100", "Beispielartikel", 12, 5 },
                    { "B-200", "Musterteil", 4, 2 }
            },
            new Object[] { "Artikel", "Name", "Lager", "Min" });
    private final JButton previewButton = new JButton("Beispiel Button");
    private JPanel previewCardPanel;
    private JPanel previewTextPanel;
    private JScrollPane previewTableScroll;
    private final JSlider tableFontSlider = new JSlider(10, 44, 16);
    private final JSlider tabFontSlider = new JSlider(10, 40, 15);
    private final JLabel tableFontSample = new JLabel("Tabellenschrift Vorschau (16px)");
    private final JLabel tabFontSample = new JLabel("Tab-Schrift Vorschau (15px)");
    private final JLabel fontSample = new JLabel("Schriftart Vorschau");
    private JButton accentColorButton;
    private JButton headerColorButton;
    private JButton buttonColorButton;
    private JLabel accentHexLabel;
    private JLabel headerHexLabel;
    private JLabel buttonHexLabel;
    private Color selectedAccentColor;
    private Color selectedHeaderColor;
    private Color selectedButtonColor;

    private static final int DEFAULT_STOCK_CHECK_INTERVAL = 30;
    private static final int DEFAULT_WARNING_INTERVAL = 1;
    private static final int DEFAULT_QR_IMPORT_INTERVAL = 10;
    private static final int DEFAULT_TABLE_FONT_SIZE = 16;
    private static final int DEFAULT_TAB_FONT_SIZE = 15;
    private static final boolean DEFAULT_ENABLE_AUTO_STOCK_CHECK = true;
    private static final boolean DEFAULT_ENABLE_WARNINGS = true;
    private static final boolean DEFAULT_ENABLE_QR_IMPORT = true;
    private static final boolean DEFAULT_DARK_MODE = false;
    private enum GlassIntensity {
        SUBTLE,
        MEDIUM
    }

    // Change this to MEDIUM for a stronger frosted look.
    private static final GlassIntensity GLASS_INTENSITY = GlassIntensity.MEDIUM;
    /**
     * Standard-Schriftfamilie, falls keine Einstellung vorhanden ist.
     */
    public static final String DEFAULT_FONT_STYLE = "Dialog";
    private static final String DEFAULT_SERVER_URL = "https://framedev.ch/vebo";

    /**
     * Aktuell verwendete Tabellenschriftgröße (px). Wird u.a. für Vorschau und
     * Tabellen-UI genutzt.
     */
    public static int TABLE_FONT_SIZE = 16;

    /**
     * Schlüssel/Mapping der unterstützten Einstellungsvariablen.
     *
     * <p>
     * Jeder Enum-Wert enthält den Property-Key, der in der Settings-Property-Datei
     * verwendet wird.
     */
    public enum Variable {
        /**
         * Intervall (in Minuten), in dem die Anwendung den Lagerbestand überprüft und
         * Warnungen erstellt, wenn Artikel unter dem Mindestbestand liegen.
         * Beeinflusst, wie häufig die Anwendung den Lagerbestand überprüft und
         * automatisch Warnungen für Artikel erstellt, die unter dem definierten
         * Mindestbestand liegen. Standard ist 30 Minuten, was eine gute Balance
         * zwischen Aktualität und Systemressourcen bietet. Benutzer können hier ein
         * kürzeres Intervall angeben, um schneller auf Lagerbestandsänderungen zu
         * reagieren, oder ein längeres Intervall, um Ressourcen zu schonen.
         */
        STOCK_CHECK_INTERVAL("stock_check_interval"),
        /**
         * Ob die automatische Anzeige von ungelesenen Warnungen aktiviert ist.
         * Beeinflusst, ob die Anwendung regelmäßig auf ungelesene Warnungen prüft und
         * diese automatisch anzeigt. Standard ist {@code true}. Wenn aktiviert, prüft
         * die Anwendung alle Stunde (gemäß {@link #WARNING_DISPLAY_INTERVAL}) auf
         * ungelesene Warnungen und zeigt diese prominent an, um sicherzustellen, dass
         * Benutzer wichtige Informationen nicht verpassen. Benutzer können hier
         * entscheiden, ob sie diese proaktive Benachrichtigungsfunktion nutzen möchten
         * oder lieber manuell nach Warnungen suchen wollen.
         */
        ENABLE_WARNING_INTERVAL("enable_hourly_warnings"),
        /**
         * Intervall (in Stunden), in dem die Anwendung auf ungelesene Warnungen prüft
         * und diese automatisch anzeigt. Beeinflusst, wie häufig die Anwendung nach
         * ungelesenen Warnungen sucht und diese prominent anzeigt. Standard ist 1
         * Stunde, was eine gute Balance zwischen Aktualität und Systemressourcen
         * bietet. Benutzer können hier ein kürzeres Intervall angeben, um schneller auf
         * neue Warnungen zu reagieren, oder ein längeres Intervall, um Ressourcen zu
         * schonen.
         */
        WARNING_DISPLAY_INTERVAL("warning_display_interval"),
        /**
         * Ob die automatische Lagerbestandsprüfung aktiviert ist. Beeinflusst, ob die
         * Anwendung regelmäßig den Lagerbestand überprüft und Warnungen erstellt, wenn
         * Artikel unter dem Mindestbestand liegen. Standard ist {@code true}. Wenn
         * aktiviert, prüft die Anwendung alle 30 Minuten (gemäß
         * {@link #STOCK_CHECK_INTERVAL}) den Lagerbestand und erstellt automatisch
         * Warnungen für Artikel, die unter dem definierten Mindestbestand liegen.
         * Benutzer können hier entscheiden, ob sie diese proaktive Überprüfungsfunktion
         * nutzen möchten oder lieber manuell den Lagerbestand überwachen wollen.
         */
        ENABLE_AUTO_STOCK_CHECK("enable_auto_stock_check"),
        /**
         * Die URL, von der die Anwendung Warnungsdaten (z.B. Lagerwarnungen) abruft.
         * Beeinflusst, wo die Anwendung nach Warnungsinformationen sucht. Standard ist
         * "<a href="https://framedev.ch/vebo/scans.json">...</a>", was auf eine
         * Beispiel-URL verweist. Benutzer können hier eine benutzerdefinierte URL
         * angeben, z.B. die Adresse eines eigenen Servers oder einer API, von der die
         * Anwendung Warnungsdaten abrufen soll. Es ist wichtig, dass die angegebene URL
         * korrekt formatiert ist und auf eine gültige Ressource zeigt, damit die
         * Anwendung Warnungsinformationen erfolgreich abrufen kann.
         */
        SERVER_URL("server_url"),
        /**
         * Ob die automatische Anzeige von ungelesenen Warnungen aktiviert ist.
         * Beeinflusst, ob die Anwendung automatisch eine Warnungs-Karte anzeigt, wenn
         * ungelesene Warnungen vorhanden sind. Standard ist {@code true}. Wenn
         * aktiviert, prüft die Anwendung regelmäßig (gemäß
         * {@link #WARNING_DISPLAY_INTERVAL}) auf ungelesene Warnungen und zeigt diese
         * prominent an, um sicherzustellen, dass Benutzer wichtige Informationen nicht
         * verpassen. Benutzer können hier entscheiden, ob sie diese proaktive
         * Benachrichtigungsfunktion nutzen möchten oder lieber manuell nach Warnungen
         * suchen.
         */
        ENABLE_AUTOMATIC_IMPORT_QRCODE("enable_automatic_import_qrcode"),
        /**
         * Intervall (in Minuten) für den automatischen Import von QR-Code Scans.
         * Beeinflusst, wie häufig die Anwendung nach neuen QR-Code Scans sucht und
         * diese importiert. Standard ist 10 Minuten, was eine gute Balance zwischen
         * Aktualität und Systemressourcen bietet. Benutzer können hier ein kürzeres
         * Intervall angeben, um schneller auf neue Scans zu reagieren, oder ein
         * längeres Intervall, um Ressourcen zu schonen.
         */
        QRCODE_IMPORT_INTERVAL("qrcode_import_interval"),
        /**
         * Ob der Dunkelmodus aktiviert ist. Beeinflusst die Farbpalette der gesamten
         * Anwendung. Standard ist {@code false} (Hellmodus). Wenn aktiviert, verwendet
         * die Anwendung dunkle Hintergründe und helle Schriftfarben, um die Augen zu
         * schonen und die Lesbarkeit in dunklen Umgebungen zu verbessern. Benutzer
         * können hier zwischen Dunkel- und Hellmodus wechseln, je nach ihren
         * Präferenzen und Umgebungslichtbedingungen.
         */
        DARK_MODE("dark_mode"),
        /**
         * Die Schriftgröße, die in Tabellen (z.B. Artikelkarte, Warnungskarte)
         * verwendet wird. Beeinflusst die Lesbarkeit der Tabellen und die
         * Gesamtästhetik der Anwendung. Standard ist 16px, was eine gute Balance
         * zwischen Lesbarkeit und Platzbedarf bietet. Benutzer können hier eine größere
         * oder kleinere Schriftgröße angeben, je nach ihren Präferenzen und
         * Bildschirmauflösung.
         */
        TABLE_FONT_SIZE("table_font_size"),
        /**
         * Die Schriftgröße, die in Tab-Labels (z.B. im Hauptbereich) verwendet wird.
         * Beeinflusst die Lesbarkeit der Tabs und die Gesamtästhetik der Anwendung.
         * Standard ist 15px, was eine gute Balance zwischen Lesbarkeit und Platzbedarf
         * bietet. Benutzer können hier eine größere oder kleinere Schriftgröße angeben,
         * je nach ihren Präferenzen und Bildschirmauflösung.
         */
        TABLE_FONT_SIZE_TAB("table_font_size_tab"),
        /**
         * Die Schriftart, die in der gesamten Anwendung verwendet wird (z.B. für
         * Labels, Buttons, Tabellen). Standard ist "Dialog", was eine
         * plattformunabhängige Schriftfamilie ist, die auf den meisten Systemen
         * verfügbar ist. Benutzer können hier eine andere Schriftart angeben, z.B.
         * "Arial", "Verdana" oder "Times New Roman". Die Anwendung sollte versuchen,
         * die angegebene Schriftart zu laden und verwenden, und falls diese nicht
         * verfügbar ist, auf die Standardschriftart zurückfallen.
         */
        FONT_STYLE("font_style"),
        /**
         * Die Akzentfarbe der Anwendung, z.B. für Links, Icons und Hervorhebungen. Wird
         * in der gesamten Anwendung verwendet, z.B. für Link-Farben, Icons in Buttons
         * und Hervorhebungen in Karten-UI.
         */
        THEME_ACCENT_COLOR("theme_accent_color"),
        /**
         * Die Farbe des Headers in der Einstellungen-GUI. Wird auch in anderen
         * Bereichen der Anwendung verwendet, z.B. für die Header-Farbe in den Karten-UI
         * (Artikelkarte, Warnungskarte, etc.).
         */
        THEME_HEADER_COLOR("theme_header_color"),
        /**
         * Die Farbe von Buttons (z.B. Speichern, Abbrechen) in der Einstellungen-GUI.
         * Wird auch in anderen Bereichen der Anwendung verwendet, z.B. für die
         * "Hinzufügen"-Buttons in der Artikelverwaltung.
         */
        THEME_BUTTON_COLOR("theme_button_color");

        final String value;

        /**
         * Erstellt einen Settings-Key.
         *
         * @param value Property-Key (z.B. "dark_mode")
         */
        Variable(String value) {
            this.value = value;
        }

        /**
         * Liefert den Property-Key, der zu diesem Enum-Wert gehört.
         *
         * @return Property-Key (nicht {@code null})
         */
        public String getValue() {
            return value;
        }
    }

    /**
     * Erstellt und initialisiert die Einstellungen-GUI.
     *
     * <p>
     * Initialisiert das Frame, baut die UI (Header, Tabs, Bottom-Bar), registriert
     * Shortcuts/
     * Listener und lädt anschließend die aktuellen Settings.
     */
    public SettingsGUI() {
        initFrame();

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // ===== Top area (Header) – VendorGUI-style card =====
        JPanel topPaddingWrapper = new JPanel(new BorderLayout());
        topPaddingWrapper.setOpaque(false);
        // single consistent outer padding
        topPaddingWrapper.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));
        topPaddingWrapper.add(createHeaderPanel(), BorderLayout.CENTER);
        mainContainer.add(topPaddingWrapper, BorderLayout.NORTH);
        mainContainer.add(createContentWrapper(), BorderLayout.CENTER);
        mainContainer.add(createBottomButtonPanel(), BorderLayout.SOUTH);

        setContentPane(mainContainer);

        installGlobalShortcuts();
        wireSearchFiltering();

        // Load current settings
        loadSettings();
        applySettingsVisualPolish(getContentPane());
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void initFrame() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        setTitle("Einstellungen");
        setSize(1200, 850);
        setMinimumSize(new Dimension(1100, 760));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set window icon if available
        if (Main.iconSmall != null) {
            setIconImage(Main.iconSmall.getImage());
        }
    }

    private JPanel createHeaderPanel() {
        // VendorGUI-style header card (consistent with other screens)
        JFrameUtils.RoundedPanel headerCard = new JFrameUtils.RoundedPanel(
            getSoftGlassSurface(ThemeManager.getCardBackgroundColor()),
            20);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getSoftGlassHighlight(), 1, true),
                BorderFactory.createLineBorder(getSoftGlassBorder(), 1, true)),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.BETTER_GEAR + " Einstellungen");
        titleLabel.setFont(getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Konfiguration und Verwaltung des Lagersystems");
        subtitleLabel.setFont(getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitleLabel);

        headerCard.add(headerText, BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(headerCard, BorderLayout.CENTER);
        // allow full-width under BoxLayout/BorderLayout parents
        // (removed setMaximumSize line)

        return wrapper;
    }

    private JPanel createContentWrapper() {
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(ThemeManager.getBackgroundColor());
        // keep same outer padding as other screens (VendorGUI/LogsGUI)
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // Content card (tabs + search) for a cleaner, consistent design
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(
            getSoftGlassSurface(ThemeManager.getCardBackgroundColor()),
            18);
        contentCard.setLayout(new BorderLayout(0, 10));
        contentCard.setOpaque(true);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getSoftGlassHighlight(), 1, true),
                BorderFactory.createLineBorder(getSoftGlassBorder(), 1, true)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        // Search bar + tabs (tabs stay in center to use full available height)
        JTabbedPane tabbedPane = createTabbedPane();
        JPanel searchPanel = createSearchPanel();

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setOpaque(false);
        searchWrapper.add(searchPanel, BorderLayout.CENTER);
        contentCard.add(searchWrapper, BorderLayout.NORTH);

        // The tabbed pane itself contains the scrollable content; we only need to
        // ensure it expands.
        // Wrap it so it takes all remaining space and stays aligned.
        JPanel tabFill = new JPanel(new BorderLayout());
        tabFill.setOpaque(false);
        tabFill.add(tabbedPane, BorderLayout.CENTER);
        contentCard.add(tabFill, BorderLayout.CENTER);

        contentWrapper.add(contentCard, BorderLayout.CENTER);
        return contentWrapper;
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new BorderLayout(12, 0));
        searchPanel.setOpaque(false);

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Einstellungen durchsuchen:");
        searchLabel.setFont(getFontByName(Font.BOLD, 13));
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        // Search input (with placeholder + clear button)
        settingsSearchField.setToolTipText("Suche nach Einstellungen...");
        settingsSearchField.setFont(getFontByName(Font.PLAIN, 13));
        settingsSearchField.setBackground(ThemeManager.getInputBackgroundColor());
        settingsSearchField.setForeground(ThemeManager.getTextSecondaryColor());
        settingsSearchField.setCaretColor(ThemeManager.getTextPrimaryColor());
        settingsSearchField.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        // Placeholder handling
        searchPlaceholderActive = true;
        settingsSearchField.setText(SEARCH_PLACEHOLDER);
        settingsSearchField.setFont(settingsSearchField.getFont().deriveFont(Font.ITALIC));
        settingsSearchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchPlaceholderActive) {
                    searchPlaceholderActive = false;
                    settingsSearchField.setText("");
                    settingsSearchField.setForeground(ThemeManager.getTextPrimaryColor());
                    settingsSearchField.setFont(getFontByName(Font.PLAIN, 13));
                }
                settingsSearchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getPrimaryColor(), 2, true),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                String txt = settingsSearchField.getText() == null ? "" : settingsSearchField.getText().trim();
                if (txt.isEmpty()) {
                    searchPlaceholderActive = true;
                    settingsSearchField.setText(SEARCH_PLACEHOLDER);
                    settingsSearchField.setForeground(ThemeManager.getTextSecondaryColor());
                    settingsSearchField.setFont(getFontByName(Font.PLAIN, 13).deriveFont(Font.ITALIC));
                }
                settingsSearchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });

        JButton clearSearchButton = new JButton("✕");
        clearSearchButton.setToolTipText("Suche löschen");
        clearSearchButton.setFont(getFontByName(Font.BOLD, 12));
        clearSearchButton.setForeground(ThemeManager.getTextSecondaryColor());
        clearSearchButton.setBackground(ThemeManager.getInputBackgroundColor());
        clearSearchButton.setFocusPainted(false);
        clearSearchButton.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        clearSearchButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearSearchButton.addActionListener(e -> {
            searchPlaceholderActive = false;
            settingsSearchField.setText("");
            settingsSearchField.requestFocusInWindow();
            applySearchFilter();
        });
        clearSearchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                clearSearchButton.setForeground(ThemeManager.getTextPrimaryColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                clearSearchButton.setForeground(ThemeManager.getTextSecondaryColor());
            }
        });

        JPanel searchFieldWrapper = new JPanel(new BorderLayout());
        searchFieldWrapper.setOpaque(true);
        searchFieldWrapper.setBackground(ThemeManager.getInputBackgroundColor());
        searchFieldWrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        searchFieldWrapper.add(settingsSearchField, BorderLayout.CENTER);
        searchFieldWrapper.add(clearSearchButton, BorderLayout.EAST);

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchFieldWrapper, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        return searchPanel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setOpaque(false);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPane.setFont(getFontByName(Font.BOLD, 14));
        tabbedPane.setBackground(ThemeManager.getCardBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());
        tabbedPane.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorderColor()));

        buildTabs(tabbedPane);
        applyTabVisualStyle(tabbedPane);
        return tabbedPane;
    }

    private void applyTabVisualStyle(JTabbedPane tabbedPane) {
        if (tabbedPane == null) {
            return;
        }
        if (Boolean.TRUE.equals(tabbedPane.getClientProperty("settings.tab.style.installed"))) {
            refreshTabPills(tabbedPane);
            return;
        }

        tabbedPane.putClientProperty("settings.tab.style.installed", Boolean.TRUE);
        tabbedPane.putClientProperty("settings.tab.refresh", (Runnable) () -> refreshTabPills(tabbedPane));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new javax.swing.plaf.basic.BasicTabbedPaneUI() {
            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                return 42;
            }

            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 14;
            }
        });

        tabbedPane.addChangeListener(e -> refreshTabPills(tabbedPane));
        refreshTabPills(tabbedPane);
    }

    private void refreshTabPills(JTabbedPane tabbedPane) {
        if (tabbedPane == null) {
            return;
        }
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String title = tabbedPane.getTitleAt(i);
            boolean selected = i == tabbedPane.getSelectedIndex();
            tabbedPane.setTabComponentAt(i, createTabPill(title, selected));
        }
    }

    private JComponent createTabPill(String title, boolean selected) {
        Color selectedBg = ThemeManager.getAccentColor();
        Color unselectedBg = ThemeManager.getInputBackgroundColor();
        Color borderColor = selected ? adjustColor(selectedBg, -0.20f) : ThemeManager.getBorderColor();

        JFrameUtils.RoundedPanel pill = new JFrameUtils.RoundedPanel(selected ? selectedBg : unselectedBg, 16);
        pill.setLayout(new BorderLayout());
        pill.setOpaque(false);
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));

        JLabel label = new JLabel(title == null ? "" : title);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(selected ? getReadableTextColor(selectedBg) : ThemeManager.getTextPrimaryColor());
        pill.add(label, BorderLayout.CENTER);
        return pill;
    }

    private void applySettingsVisualPolish(Container root) {
        SettingsStylingService.applySettingsVisualPolish(
                root,
                ThemeManager.getAccentColor(),
                getSoftGlassHighlight(),
                getSoftGlassBorder(),
                getSoftGlassSurface(ThemeManager.getCardBackgroundColor()));
    }

    private JPanel createBottomButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 18));
        buttonPanel.setBackground(ThemeManager.getCardBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton resetAllButton = createStyledButton(UnicodeSymbols.REFRESH + " Alles zurücksetzen",
            ThemeManager.getWarningColor());
        resetAllButton.addActionListener(e -> resetAllDefaults());

        JButton cancelButton = createStyledButton(UnicodeSymbols.CLOSE + " Abbrechen", ThemeManager.getSecondaryColor());
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton(UnicodeSymbols.FLOPPY + " Speichern", ThemeManager.getSuccessColor());
        saveButton.addActionListener(e -> saveSettings());

        buttonPanel.add(resetAllButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void installGlobalShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchPlaceholderActive) {
                    searchPlaceholderActive = false;
                    settingsSearchField.setText("");
                    settingsSearchField.setForeground(ThemeManager.getTextPrimaryColor());
                    settingsSearchField.setFont(getFontByName(Font.PLAIN, 13));
                }
                settingsSearchField.requestFocusInWindow();
                settingsSearchField.selectAll();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSettings");
        am.put("closeSettings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void wireSearchFiltering() {
        settingsSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearchFilter();
            }
        });
    }

    private void buildTabs(JTabbedPane tabbedPane) {
        if (tabbedPane == null)
            return;
        buildSystemAutomaticTab(tabbedPane);
        buildDesignTab(tabbedPane);
        buildConnectionTab(tabbedPane);
        buildDatabaseTab(tabbedPane);
        buildImportExportTab(tabbedPane);
        buildAboutTab(tabbedPane);
    }

    private void buildSystemAutomaticTab(JTabbedPane tabbedPane) {
        // === CATEGORY 1: System & Automatisierung (Prettier, Left-Aligned) ===
        JPanel systemPanel = createCategoryPanel();
        systemPanel.setLayout(new BoxLayout(systemPanel, BoxLayout.Y_AXIS));
        JScrollPane systemScroll = createScrollablePanel(systemPanel);

        // Card: Stock Check
        JPanel stockCheckCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.PACKAGE + " Lagerbestandsprüfung",
            "Automatische Überprüfung des Lagerbestands und Warnerstellung",
            null);
        stockCheckCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        stockCheckCard.setBackground(ThemeManager.getCardBackgroundColor());
        stockCheckCard.setLayout(new BoxLayout(stockCheckCard, BoxLayout.Y_AXIS));
        stockCheckCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(stockCheckCard, this::resetStockCheckDefaults);

        enableAutoStockCheckCheckbox = new JCheckBox("Automatische Lagerbestandsprüfung aktivieren");
        styleCheckbox(enableAutoStockCheckCheckbox);
        enableAutoStockCheckCheckbox.setSelected(true);
        enableAutoStockCheckCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        stockCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
        JPanel intervalPanel = createLabeledSpinnerPanel("Prüfintervall:", stockCheckIntervalSpinner, "Minuten", 6);
        intervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        stockCheckCard.add(Box.createVerticalStrut(18));
        stockCheckCard.add(enableAutoStockCheckCheckbox);
        stockCheckCard.add(Box.createVerticalStrut(15));
        stockCheckCard.add(intervalPanel);

        systemPanel.add(stockCheckCard);
        systemPanel.add(Box.createVerticalStrut(22));

        // Card: Warnungen
        JPanel warningCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.WARNING + " Warnungsanzeige",
            "Konfiguration der automatischen Warnanzeige",
            null);
        warningCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        warningCard.setBackground(ThemeManager.getCardBackgroundColor());
        warningCard.setLayout(new BoxLayout(warningCard, BoxLayout.Y_AXIS));
        warningCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(warningCard, this::resetWarningDefaults);

        enableWarningDisplayCheckbox = new JCheckBox("Automatische Anzeige ungelesener Warnungen aktivieren");
        styleCheckbox(enableWarningDisplayCheckbox);
        enableWarningDisplayCheckbox.setSelected(true);
        enableWarningDisplayCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        warningDisplayIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        JPanel warningIntervalPanel = createLabeledSpinnerPanel("Anzeigeintervall:", warningDisplayIntervalSpinner, "Stunde(n)", 6);
        warningIntervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel warningInfoLabel = getWarningInfoLabel();
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        warningCard.add(Box.createVerticalStrut(18));
        warningCard.add(enableWarningDisplayCheckbox);
        warningCard.add(Box.createVerticalStrut(15));
        warningCard.add(warningIntervalPanel);
        warningCard.add(Box.createVerticalStrut(10));
        warningCard.add(warningInfoLabel);

        systemPanel.add(warningCard);
        systemPanel.add(Box.createVerticalStrut(22));

        // Card: QR-Code Import
        JPanel qrCodeCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.PHONE + " QR-Code Import",
            "Automatischer Import von QR-Code Scans",
            null);
        qrCodeCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        qrCodeCard.setBackground(ThemeManager.getCardBackgroundColor());
        qrCodeCard.setLayout(new BoxLayout(qrCodeCard, BoxLayout.Y_AXIS));
        qrCodeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(qrCodeCard, this::resetQrCodeDefaults);

        automaticImportCheckBox = new JCheckBox("Automatisches Importieren von QR-Codes aktivieren");
        styleCheckbox(automaticImportCheckBox);
        automaticImportCheckBox.setSelected(false);
        automaticImportCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        qrCodeCard.add(Box.createVerticalStrut(18));
        qrCodeCard.add(automaticImportCheckBox);
        qrCodeCard.add(Box.createVerticalStrut(15));

        qrCodeImportIntervalSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 60, 5));
        JPanel qrIntervalPanel = createLabeledSpinnerPanel("Import-Intervall:", qrCodeImportIntervalSpinner, "Minuten", 6);
        qrIntervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qrCodeCard.add(qrIntervalPanel);

        systemPanel.add(qrCodeCard);
        systemPanel.add(Box.createVerticalStrut(22));

        // Card: Kategorien
        JList<String> categoryList = new JList<>();
        JPanel categoryCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.FOLDER + " Kategorien",
            "Einstellungen zu verschiedenen Kategorien",
            null);
        categoryCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        categoryCard.setBackground(ThemeManager.getCardBackgroundColor());
        categoryCard.setLayout(new BoxLayout(categoryCard, BoxLayout.Y_AXIS));
        categoryCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryInfoLabel = createInfoLabel("Hier können Sie Kategorien hinzufügen oder die Datei direkt bearbeiten. Optional können Sie einen Nummernbereich (Von/Bis) angeben.");
        categoryInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top actions row
        JPanel categoryActions = new JPanel();
        categoryActions.setLayout(new BoxLayout(categoryActions, BoxLayout.X_AXIS));
        categoryActions.setOpaque(false);
        categoryActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton categorySettingsFileButton = createStyledButton(UnicodeSymbols.FOLDER + " Kategorien-Datei öffnen", ThemeManager.getSecondaryColor());
        categorySettingsFileButton.setToolTipText("Öffnet categories.json im Standard-Editor");
        categorySettingsFileButton.addActionListener(e -> openCategorySettingsFile());
        JButton reloadCategoriesButton = createStyledButton(UnicodeSymbols.REFRESH + " Kategorien neu laden", ThemeManager.getAccentColor());
        reloadCategoriesButton.setToolTipText("Lädt categories.json neu ein (ohne Neustart)");
        reloadCategoriesButton.addActionListener(e -> {
            loadCategories();
            new MessageDialog()
                    .setTitle("Kategorien")
                    .setMessage("Kategorien wurden neu geladen.")
                    .setDuration(5000)
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        });
        categoryActions.add(categorySettingsFileButton);
        categoryActions.add(Box.createHorizontalStrut(8));
        categoryActions.add(reloadCategoriesButton);
        categoryCard.add(categoryInfoLabel);
        categoryCard.add(Box.createVerticalStrut(10));
        categoryCard.add(categoryActions);
        categoryCard.add(Box.createVerticalStrut(14));

        JSeparator catSep = new JSeparator();
        catSep.setForeground(ThemeManager.getBorderColor());
        catSep.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryCard.add(catSep);
        categoryCard.add(Box.createVerticalStrut(14));

        // ---- Add Category Form (improved layout, left-aligned) ----
        JLabel categoryFormTitle = new JLabel("Neue Kategorie hinzufügen");
        categoryFormTitle.setFont(getFontByName(Font.BOLD, 14));
        categoryFormTitle.setForeground(ThemeManager.getTextPrimaryColor());
        categoryFormTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryFormHint = new JLabel("<html><div style='padding-top:2px;'><span style='font-size:11px;'>Tipp: Drücken Sie Enter im Namen-Feld, um hinzuzufügen. Z.b von 1000 bis 1999</span></div></html>");
        categoryFormHint.setFont(getFontByName(Font.PLAIN, 12));
        categoryFormHint.setForeground(ThemeManager.getTextSecondaryColor());
        categoryFormHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField categoryNameField = new JTextField();
        styleInputField(categoryNameField);
        categoryNameField.setToolTipText("Kategoriename (z.B. 'Elektronik')");

        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(false);
        NumberFormatter intFormatter = new NumberFormatter(intFormat);
        intFormatter.setValueClass(Integer.class);
        intFormatter.setAllowsInvalid(true);
        intFormatter.setCommitsOnValidEdit(true);
        intFormatter.setMinimum(Integer.MIN_VALUE);
        intFormatter.setMaximum(Integer.MAX_VALUE);

        JFormattedTextField fromRange = new JFormattedTextField(intFormatter);
        fromRange.setColumns(6);
        fromRange.setToolTipText("Von (z.B. 1000)");
        fromRange.setFont(getFontByName(Font.PLAIN, 13));
        fromRange.setBackground(ThemeManager.getInputBackgroundColor());
        fromRange.setForeground(ThemeManager.getTextPrimaryColor());
        fromRange.setCaretColor(ThemeManager.getTextPrimaryColor());
        fromRange.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JFormattedTextField toRange = new JFormattedTextField(intFormatter);
        toRange.setColumns(6);
        toRange.setToolTipText("Bis (z.B. 1999)");
        toRange.setFont(getFontByName(Font.PLAIN, 13));
        toRange.setBackground(ThemeManager.getInputBackgroundColor());
        toRange.setForeground(ThemeManager.getTextPrimaryColor());
        toRange.setCaretColor(ThemeManager.getTextPrimaryColor());
        toRange.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JButton addCategoryButton = createStyledButton(UnicodeSymbols.PLUS + " Hinzufügen", ThemeManager.getSuccessColor());
        addCategoryButton.setToolTipText("Fügt die Kategorie hinzu");
        addCategoryButton.setEnabled(false);

        Runnable doAddCategory = () -> {
            String newCategory = categoryNameField.getText() == null ? "" : categoryNameField.getText().trim();
            String fromTxt = fromRange.getText() == null ? "" : fromRange.getText().trim();
            String toTxt = toRange.getText() == null ? "" : toRange.getText().trim();

            if (newCategory.isEmpty()) {
                new MessageDialog()
                .setTitle("Hinweis")
                .setMessage("Bitte einen Kategorienamen eingeben.")
                .setDuration(5000)
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .display();
                categoryNameField.requestFocusInWindow();
                return;
            }

            // Only validate if either range is present
            if (!fromTxt.isEmpty() || !toTxt.isEmpty()) {
                try {
                    int f = fromTxt.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(fromTxt);
                    int t = toTxt.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(toTxt);
                    if (f > t)
                        throw new NumberFormatException();
                    addNewCategory(newCategory, f, t);
                    loadCategoriesIntoList(categoryList);
                } catch (NumberFormatException ex) {
                    new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Ungültiger Bereich (Von/Bis). Bitte ganze Zahlen eingeben und Von ≤ Bis.")
                    .setDuration(5000)
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
                    return;
                }
            } else {
                addNewCategory(newCategory, Integer.MIN_VALUE, Integer.MAX_VALUE);
                loadCategoriesIntoList(categoryList);
            }

            categoryNameField.setText("");
            fromRange.setValue(null);
            toRange.setValue(null);
            addCategoryButton.setEnabled(false);
            categoryNameField.requestFocusInWindow();
        };

        addCategoryButton.addActionListener(e -> doAddCategory.run());

        // Enable button only when name is not empty
        categoryNameField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                String name = categoryNameField.getText() == null
                        ? ""
                        : categoryNameField.getText().trim();

                Object fromObj = fromRange.getValue();
                Object toObj = toRange.getValue();

                boolean validRange = true;

                // Only validate range if BOTH values are present
                if (fromObj instanceof Number && toObj instanceof Number) {
                    int from = ((Number) fromObj).intValue();
                    int to = ((Number) toObj).intValue();
                    validRange = from <= to;
                }

                boolean validName = !name.isEmpty();
                addCategoryButton.setEnabled(validName && validRange);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }
        });

        // Enter in name field triggers add
        categoryNameField.addActionListener(e -> {
            if (addCategoryButton.isEnabled()) {
                doAddCategory.run();
            }
        });

        // Layout panel (left-aligned)
        JPanel addCategoryPanel = new JPanel();
        addCategoryPanel.setLayout(new BoxLayout(addCategoryPanel, BoxLayout.X_AXIS));
        addCategoryPanel.setOpaque(false);
        addCategoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel addCategoryLabels = new JPanel();
        addCategoryLabels.setLayout(new BoxLayout(addCategoryLabels, BoxLayout.Y_AXIS));
        addCategoryLabels.setOpaque(false);
        addCategoryLabels.setAlignmentY(Component.TOP_ALIGNMENT);
        addCategoryLabels.setMaximumSize(new Dimension(140, Integer.MAX_VALUE));
        JLabel categoryNameLabel = new JLabel("Kategoriename");
        categoryNameLabel.setFont(getFontByName(Font.BOLD, 13));
        categoryNameLabel.setForeground(ThemeManager.getTextPrimaryColor());
        JLabel fromLabel = new JLabel("Von");
        fromLabel.setFont(getFontByName(Font.PLAIN, 11));
        fromLabel.setForeground(ThemeManager.getTextSecondaryColor());
        JLabel toLabel = new JLabel("Bis");
        toLabel.setFont(getFontByName(Font.PLAIN, 11));
        toLabel.setForeground(ThemeManager.getTextSecondaryColor());
        addCategoryLabels.add(categoryNameLabel);
        addCategoryLabels.add(Box.createVerticalStrut(8));
        addCategoryLabels.add(fromLabel);
        addCategoryLabels.add(Box.createVerticalStrut(8));
        addCategoryLabels.add(toLabel);

        JPanel addCategoryFields = new JPanel();
        addCategoryFields.setLayout(new BoxLayout(addCategoryFields, BoxLayout.Y_AXIS));
        addCategoryFields.setOpaque(false);
        addCategoryFields.setAlignmentY(Component.TOP_ALIGNMENT);
        addCategoryFields.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        addCategoryFields.add(categoryNameField);
        addCategoryFields.add(Box.createVerticalStrut(8));
        addCategoryFields.add(fromRange);
        addCategoryFields.add(Box.createVerticalStrut(8));
        addCategoryFields.add(toRange);

        JPanel addCategoryButtonPanel = new JPanel();
        addCategoryButtonPanel.setLayout(new BoxLayout(addCategoryButtonPanel, BoxLayout.Y_AXIS));
        addCategoryButtonPanel.setOpaque(false);
        addCategoryButtonPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        addCategoryButtonPanel.setMaximumSize(new Dimension(180, Integer.MAX_VALUE));
        addCategoryButton.setPreferredSize(new Dimension(160, 36));
        addCategoryButtonPanel.add(addCategoryButton);

        JPanel addCategoryFormPanel = new JPanel();
        addCategoryFormPanel.setLayout(new BoxLayout(addCategoryFormPanel, BoxLayout.X_AXIS));
        addCategoryFormPanel.setOpaque(false);
        addCategoryFormPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCategoryFormPanel.add(addCategoryLabels);
        addCategoryFormPanel.add(Box.createHorizontalStrut(12));
        addCategoryFormPanel.add(addCategoryFields);
        addCategoryFormPanel.add(Box.createHorizontalStrut(12));
        addCategoryFormPanel.add(addCategoryButtonPanel);

        JPanel addCategoryOuterPanel = new JPanel();
        addCategoryOuterPanel.setLayout(new BoxLayout(addCategoryOuterPanel, BoxLayout.Y_AXIS));
        addCategoryOuterPanel.setOpaque(false);
        addCategoryOuterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addCategoryOuterPanel.add(categoryFormTitle);
        addCategoryOuterPanel.add(Box.createVerticalStrut(4));
        addCategoryOuterPanel.add(categoryFormHint);
        addCategoryOuterPanel.add(Box.createVerticalStrut(10));
        addCategoryOuterPanel.add(addCategoryFormPanel);

        categoryCard.add(addCategoryOuterPanel);
        categoryCard.add(Box.createVerticalStrut(18));

        JLabel categoryListLabel = new JLabel("Aktuelle Kategorien");
        categoryListLabel.setFont(getFontByName(Font.BOLD, 18));
        categoryListLabel.setForeground(ThemeManager.getTextPrimaryColor());
        categoryListLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryCard.add(categoryListLabel);
        categoryCard.add(Box.createVerticalStrut(10));
        categoryList.setFont(getFontByName(Font.PLAIN, 15));
        categoryList.setBackground(ThemeManager.getInputBackgroundColor());
        categoryList.setForeground(ThemeManager.getTextPrimaryColor());
        categoryList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        categoryList.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadCategoriesIntoList(categoryList);
        JScrollPane categoryListScroll = new JScrollPane(categoryList);
        categoryListScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryCard.add(categoryListScroll);
        categoryCard.add(Box.createVerticalStrut(14));

        systemPanel.add(categoryCard);
        systemPanel.add(Box.createVerticalStrut(22));

        // Card: Sonstiges
        JPanel otherCard = SettingsUtils.createSectionPanel(
            "Sonstiges",
            "Allgemeine Einstellungen",
            null);
        otherCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        otherCard.setBackground(ThemeManager.getCardBackgroundColor());
        otherCard.setLayout(new BoxLayout(otherCard, BoxLayout.Y_AXIS));
        otherCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        otherCard.add(Box.createVerticalStrut(18));
        JLabel openSettingsLabel = createInfoLabel("Öffnet den Ordner, in dem die Einstellungsdateien gespeichert sind.");
        openSettingsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(openSettingsLabel);
        otherCard.add(Box.createVerticalStrut(10));
        JButton openSettingsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Einstellungen-Ordner öffnen", ThemeManager.getSecondaryColor());
        openSettingsFolderButton.addActionListener(e -> openSettingsFolder());
        openSettingsFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(openSettingsFolderButton);
        JLabel logsLabel = createInfoLabel("Öffnet den Ordner, in dem die Anwendungsprotokolle gespeichert sind.");
        logsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(15));
        otherCard.add(logsLabel);
        otherCard.add(Box.createVerticalStrut(10));
        JButton openLogsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Protokolle-Ordner öffnen", ThemeManager.getSecondaryColor());
        openLogsFolderButton.addActionListener(e -> openLogsFolder());
        openLogsFolderButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(openLogsFolderButton);
        otherCard.add(Box.createVerticalStrut(15));
        JLabel logsDelete = createInfoLabel("Löscht alle Anwendungsprotokolle aus dem Protokolle-Ordner. So wie in der Datenbank.");
        logsDelete.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(15));
        otherCard.add(logsDelete);
        JButton deleteLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alle Protokolle löschen", ThemeManager.getDangerColor());
        deleteLogsButton.addActionListener(e -> deleteAllLogs());
        deleteLogsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(10));
        otherCard.add(deleteLogsButton);
        otherCard.add(Box.createVerticalStrut(15));
        JLabel logsDeleteTime = createInfoLabel("Löscht alle Anwendungsprotokolle, die älter als 30 Tage sind, aus dem Protokolle-Ordner. So wie in der Datenbank.");
        logsDeleteTime.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(15));
        otherCard.add(logsDeleteTime);
        JCheckBox deleteOldLogsCheckBox = new JCheckBox("Alte Protokolle (älter als 30 Tage) löschen");
        styleCheckbox(deleteOldLogsCheckBox);
        deleteOldLogsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(10));
        otherCard.add(deleteOldLogsCheckBox);
        JButton deleteOldLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alte Protokolle löschen", ThemeManager.getDangerColor());
        deleteOldLogsButton.addActionListener(e -> deleteOldLogs());
        deleteOldLogsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        otherCard.add(Box.createVerticalStrut(10));
        otherCard.add(deleteOldLogsButton);

        systemPanel.add(otherCard);
        systemPanel.add(Box.createVerticalGlue());

        // Add system panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.WRENCH + " System", systemScroll);
    }

    private void loadCategoriesIntoList(JList<String> categoryList) {
        Objects.requireNonNull(categoryList, "categoryList must not be null");

        // Count articles per category once (instead of re-scanning for every category).
        Map<String, Integer> articleCountsByCategory = new HashMap<>();
        for (Article article : ArticleManager.getInstance().getAllArticles()) {
            if (article == null || article.getCategory() == null || article.getCategory().isBlank()) {
                continue;
            }
            articleCountsByCategory.merge(normalizeCategoryKey(article.getCategory()), 1, Integer::sum);
        }

        List<String> sortedCategoryNames = new ArrayList<>(ArticleUtils.categories.keySet());
        sortedCategoryNames.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> displayEntries = new ArrayList<>(sortedCategoryNames.size());
        for (String categoryName : sortedCategoryNames) {
            CategoryRange categoryRange = ArticleUtils.categories.get(categoryName);
            int count = articleCountsByCategory.getOrDefault(normalizeCategoryKey(categoryName), 0);
            displayEntries.add(buildCategoryDisplayLabel(categoryName, categoryRange, count));
        }

        categoryList.setListData(displayEntries.toArray(new String[0]));
    }

    private String buildCategoryDisplayLabel(String categoryName, CategoryRange categoryRange, int articleCount) {
        String baseLabel = categoryName + formatCategoryRange(categoryRange);
        return baseLabel + formatArticleCountSuffix(articleCount);
    }

    private String formatArticleCountSuffix(int articleCount) {
        if (articleCount <= 0) {
            return "";
        }
        String articleText = articleCount == 1 ? "1 Artikel" : articleCount + " Artikel";
        return " | " + articleText + " in dieser Kategorie";
    }

    private String formatCategoryRange(CategoryRange categoryRange) {
        if (categoryRange == null) {
            return "";
        }

        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        int from = categoryRange.rangeStart;
        int to = categoryRange.rangeEnd;

        if (from == min && to == max) {
            return "";
        }

        String fromStr = from == min ? "" : String.valueOf(from);
        String toStr = to == max ? "" : String.valueOf(to);
        return " (" + fromStr + " - " + toStr + ")";
    }

    private String normalizeCategoryKey(String category) {
        return category == null ? "" : category.trim().toLowerCase(Locale.ROOT);
    }

    private void buildDesignTab(JTabbedPane tabbedPane) {
        // === CATEGORY 1b: Darstellung (Prettier, Left-Aligned) ===
        JPanel appearancePanel = createCategoryPanel();
        appearancePanel.setLayout(new BoxLayout(appearancePanel, BoxLayout.Y_AXIS));
        // Removed unused appearanceScroll variable

        // Card: Schriftart
        JPanel fontCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.ABC + " Schriftart",
            "Schriftart für die Anwendung festlegen",
            null);
        fontCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        fontCard.setBackground(ThemeManager.getCardBackgroundColor());
        fontCard.setLayout(new BoxLayout(fontCard, BoxLayout.Y_AXIS));
        fontCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(fontCard, this::resetFontDefaults);

        JLabel appearanceIntro = new JLabel("<html><div style='padding:2px 0;'><b>Darstellung</b><br/><span style='font-size:11px;'>Änderungen werden nach dem Speichern übernommen. Die Vorschau aktualisiert sich automatisch.</span></div></html>");
        appearanceIntro.setFont(getFontByName(Font.PLAIN, 12));
        appearanceIntro.setForeground(ThemeManager.getTextSecondaryColor());
        appearanceIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontCard.add(Box.createVerticalStrut(12));
        fontCard.add(appearanceIntro);
        fontCard.add(Box.createVerticalStrut(12));

        JLabel fontInfoLabel = new JLabel("<html><div><span style='font-size: 11px;'>Wählen Sie eine Schriftart. Die Vorschau unten zeigt das Ergebnis.</span></div></html>");
        fontInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        fontInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        fontInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontCard.add(fontInfoLabel);
        fontCard.add(Box.createVerticalStrut(10));

        fontComboBox = new JComboBox<>();
        getAllFonts().forEach(fontComboBox::addItem);
        styleComboBox(fontComboBox);
        fontComboBox.addActionListener(e -> updatePreview());
        JPanel fontSelectionPanel = createLabeledComboBoxPanel("Schriftart auswählen:", fontComboBox);
        fontSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontCard.add(fontSelectionPanel);
        fontSample.setFont(getFontByName(Font.PLAIN, 16));
        fontSample.setForeground(ThemeManager.getTextPrimaryColor());
        fontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontCard.add(Box.createVerticalStrut(12));
        fontCard.add(fontSample);

        appearancePanel.add(fontCard);
        appearancePanel.add(Box.createVerticalStrut(22));

        // Card: Tabellen & Tabs
        JPanel tableCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.CLIPBOARD + " Tabellen & Tabs",
            "Schriftgröße für Tabellen und Tabs anpassen",
            null);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        tableCard.setBackground(ThemeManager.getCardBackgroundColor());
        tableCard.setLayout(new BoxLayout(tableCard, BoxLayout.Y_AXIS));
        tableCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(tableCard, this::resetTableDefaults);

        fontSizeTableSpinner = new JSpinner(new SpinnerNumberModel(16, 10, 44, 1));
        JPanel fontSizePanel = createLabeledSpinnerPanel("Tabellen-Schriftgröße:", fontSizeTableSpinner, "px", 4);
        fontSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(Box.createVerticalStrut(12));
        tableCard.add(fontSizePanel);
        tableCard.add(Box.createVerticalStrut(8));
        configureSlider(tableFontSlider, fontSizeTableSpinner, tableFontSample);
        tableCard.add(tableFontSlider);
        tableCard.add(Box.createVerticalStrut(6));
        tableFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(tableFontSample);

        fontSizeTabSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 40, 1));
        JPanel fontTabSizePanel = createLabeledSpinnerPanel("Tabs-Schriftgröße:", fontSizeTabSpinner, "px", 4);
        fontTabSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(Box.createVerticalStrut(14));
        tableCard.add(fontTabSizePanel);
        tableCard.add(Box.createVerticalStrut(8));
        configureSlider(tabFontSlider, fontSizeTabSpinner, tabFontSample);
        tableCard.add(tabFontSlider);
        tableCard.add(Box.createVerticalStrut(6));
        tabFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableCard.add(tabFontSample);

        fontSizeTableSpinner.addChangeListener(e -> updatePreview());
        fontSizeTabSpinner.addChangeListener(e -> updatePreview());

        appearancePanel.add(tableCard);
        appearancePanel.add(Box.createVerticalStrut(22));

        // Card: Design / Theme
        JPanel themeCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.COLOR_PALETTE + " Design & Darstellung",
            "Passen Sie das Erscheinungsbild der Anwendung an",
            null);
        fontSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        themeCard.setBackground(ThemeManager.getCardBackgroundColor());
        tableFontSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.setLayout(new BoxLayout(themeCard, BoxLayout.Y_AXIS));
        themeCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(themeCard, this::resetThemeDefaults);

        darkModeCheckbox = new JCheckBox("Dark Mode aktivieren");
        styleCheckbox(darkModeCheckbox);
        fontTabSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        darkModeCheckbox.setSelected(ThemeManager.isDarkMode());
        darkModeCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(Box.createVerticalStrut(16));
        themeCard.add(darkModeCheckbox);
        tabFontSlider.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(Box.createVerticalStrut(12));
        String[] themes = { "Light", "Dark" };
        tabFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeComboBox = new JComboBox<>(themes);
        styleComboBox(themeComboBox);
        themeComboBox.setSelectedItem(ThemeManager.isDarkMode() ? "Dark" : "Light");
        JPanel themeSelectionPanel = createLabeledComboBoxPanel("Design-Schema:", themeComboBox);
        themeSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        darkModeCheckbox.addActionListener(e -> {
            boolean isDark = darkModeCheckbox.isSelected();
            themeComboBox.setEnabled(!isDark);
            themeComboBox.setToolTipText(isDark
                    ? "Deaktiviert, weil Dark Mode aktiviert ist"
                    : "Wählen Sie ein Design-Schema");
            if (isDark) {
                themeComboBox.setSelectedItem("Dark");
            }
            updatePreview();
        });
        themeComboBox.addActionListener(e -> {
            String selected = (String) themeComboBox.getSelectedItem();
            darkModeCheckbox.setSelected("Dark".equals(selected));
            updatePreview();
        });
        themeCard.add(themeSelectionPanel);
        themeCard.add(Box.createVerticalStrut(12));

        darkModeCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel themeLookAndFeelInfo = new JLabel("Look & Feel: " + ThemeManager.getCurrentLookAndFeelName());
        themeLookAndFeelInfo.setFont(getFontByName(Font.PLAIN, 12));
        themeLookAndFeelInfo.setForeground(ThemeManager.getTextSecondaryColor());
        themeLookAndFeelInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(themeLookAndFeelInfo);
        themeCard.add(Box.createHorizontalStrut(10));
        JList<String> lafList = new JList<>();
        lafList.setListData(ThemeManager.getAllLookAndFeels().toArray(new String[0]));
        lafList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lafList.setSelectedValue(ThemeManager.getCurrentLookAndFeelName(), false);
        themeSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        lafList.setFont(getFontByName(Font.PLAIN, 13));
        lafList.setBackground(ThemeManager.getInputBackgroundColor());
        lafList.setForeground(ThemeManager.getTextPrimaryColor());
        lafList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        lafList.setAlignmentX(Component.LEFT_ALIGNMENT);
        lafList.setVisibleRowCount(5);
        JScrollPane lafScroll = new JScrollPane(lafList);
        lafScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(lafScroll);
        JButton applyLafButton = createStyledButton(UnicodeSymbols.CHECK + " Look & Feel anwenden", ThemeManager.getSuccessColor());
        applyLafButton.setToolTipText("Wendet das ausgewählte Look & Feel an (nur in der Vorschau)");
        applyLafButton.addActionListener(e -> {
            String selectedLaf = lafList.getSelectedValue();
            if (selectedLaf != null) {
                ThemeManager.setLookAndFeel(selectedLaf);
                updatePreview();
                Main.settings.setProperty("look_and_feel", selectedLaf);
                Main.settings.save();
            } else {
                new MessageDialog()
                .setTitle("Hinweis")
                .setMessage("Bitte wählen Sie ein Look & Feel aus der Liste aus.")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .display();
            }
        });
        themeLookAndFeelInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(Box.createVerticalStrut(10));
        applyLafButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(applyLafButton);

        JLabel themeInfoLabel = new JLabel("<html><div style='padding: 6px 0;'><i>Hinweis: Nach dem Speichern werden alle Fenster mit dem neuen Design aktualisiert.<br/>Der Dark Mode schont die Augen bei Arbeiten in dunkler Umgebung.</i></div></html>");
        themeInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        themeInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        lafScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeCard.add(themeInfoLabel);

        applyLafButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        appearancePanel.add(themeCard);
        appearancePanel.add(Box.createVerticalStrut(22));
        appearancePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Card: Farben
        JPanel colorCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.COLOR_PALETTE + " Farbthemen",
            "Akzent-, Header- und Buttonfarbe anpassen",
            null);
        colorCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        colorCard.setBackground(ThemeManager.getCardBackgroundColor());
        colorCard.setLayout(new BoxLayout(colorCard, BoxLayout.Y_AXIS));
        colorCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        addSectionResetButton(colorCard, this::resetColorDefaults);
        colorCard.add(Box.createVerticalStrut(12));
        colorCard.add(createColorRow("Akzentfarbe:", "theme_accent_color"));
        colorCard.add(Box.createVerticalStrut(10));
        colorCard.add(createColorRow("Headerfarbe:", "theme_header_color"));
        colorCard.add(Box.createVerticalStrut(10));
        colorCard.add(createColorRow("Buttonfarbe:", "theme_button_color"));
        themeInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        appearancePanel.add(colorCard);
        appearancePanel.add(Box.createVerticalStrut(22));

        // Card: Vorschau
        JPanel previewCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.BULB + " Live Vorschau",
            "Vorschau auf Schrift und Farben basierend auf Ihren Einstellungen",
            null);
        previewCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        previewCard.setBackground(ThemeManager.getCardBackgroundColor());
        previewCard.setLayout(new BoxLayout(previewCard, BoxLayout.Y_AXIS));
        previewCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(Box.createVerticalStrut(12));
        JComponent previewPanelComponent = createPreviewPanel();
        previewPanelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(previewPanelComponent);

        JPanel headerCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.INFO + " Header-Einstellungen",
            "Optionen zur Anpassung der Header-Panels",
            null);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        headerCard.setBackground(ThemeManager.getCardBackgroundColor());
        headerCard.setLayout(new BoxLayout(headerCard, BoxLayout.Y_AXIS));
        headerCard.setOpaque(true);
        JLabel disableHeaderPanels = createInfoLabel("Deaktivieren der Header-Panels");
        disableHeaderPanels.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerCard.add(disableHeaderPanels);
        boolean disable = Main.settings.getProperty("disable_header") != null && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        JCheckBox disableHeaderColorCheckbox = new JCheckBox("Header-Panels deaktivieren (entfernt die farbigen Header-Balken) ("+ (disable ? "Deaktiviert" : "Aktiviert") + ")");
        styleCheckbox(disableHeaderColorCheckbox);
        disableHeaderColorCheckbox.setSelected(disable);
        disableHeaderColorCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        disableHeaderColorCheckbox.addActionListener(e -> {
            boolean disableHeader = disableHeaderColorCheckbox.isSelected();
            Main.settings.setProperty("disable_header", Boolean.toString(disableHeader));
            Main.settings.save();
            ThemeManager.getInstance().updateAllWindows();
        });
        headerCard.add(Box.createVerticalStrut(8));
        headerCard.add(disableHeaderColorCheckbox);
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        appearancePanel.add(headerCard);
        appearancePanel.add(Box.createVerticalStrut(12));

        appearancePanel.add(previewCard);

        // Remove duplicate color rows (already added above)
        // Add appearance panel to tabbed pane with scroll
        JScrollPane appearanceScroll = createScrollablePanel(appearancePanel);
        appearanceScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabbedPane.addTab(UnicodeSymbols.COLOR_PALETTE + " Darstellung", appearanceScroll);
    }

    private void buildConnectionTab(JTabbedPane tabbedPane) {
        // === CATEGORY 2: Verbindung (Prettier, Left-Aligned) ===
        JPanel connectionPanel = createCategoryPanel();
        connectionPanel.setLayout(new BoxLayout(connectionPanel, BoxLayout.Y_AXIS));
        JScrollPane connectionScroll = createScrollablePanel(connectionPanel);

        // Card: Server Connection
        JPanel serverCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.GLOBE + " Server-Verbindung",
            "URL des QR-Code Scan-Servers",
            null);
        serverCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        // Removed stray previewPanel/previewCard lines (not relevant to connection tab)
        serverCard.setLayout(new BoxLayout(serverCard, BoxLayout.Y_AXIS));
        serverCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel serverTitle = new JLabel(UnicodeSymbols.GLOBE + " Server-Verbindung");
        serverTitle.setFont(getFontByName(Font.BOLD, 22));
        serverTitle.setForeground(ThemeManager.getTitleTextColor());
        serverTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverCard.add(serverTitle);
        serverCard.add(Box.createVerticalStrut(8));

        JLabel serverDesc = new JLabel("Konfigurieren Sie die Server-URL für den QR-Code Scan-Service. Nutzen Sie https, wenn möglich.");
        serverDesc.setFont(getFontByName(Font.PLAIN, 13));
        serverDesc.setForeground(ThemeManager.getTextSecondaryColor());
        serverDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverCard.add(serverDesc);
        serverCard.add(Box.createVerticalStrut(14));

        JLabel serverHint = createInfoLabel("Tipp: Nutzen Sie https (wenn möglich). Mit den Aktionen können Sie die URL testen oder kopieren.");
        serverHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverCard.add(serverHint);
        serverCard.add(Box.createVerticalStrut(10));

        // Actions row
        JPanel serverActions = new JPanel();
        serverActions.setLayout(new BoxLayout(serverActions, BoxLayout.X_AXIS));
        serverActions.setOpaque(false);
        serverActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton openUrlButton = createStyledButton(UnicodeSymbols.GLOBE + " Öffnen", ThemeManager.getSecondaryColor());
        openUrlButton.setToolTipText("Öffnet die URL im Browser");
        JButton copyUrlButton = createStyledButton(UnicodeSymbols.CLIPBOARD + " Kopieren", ThemeManager.getAccentColor());
        copyUrlButton.setToolTipText("Kopiert die URL in die Zwischenablage");
        JButton testUrlButton = createStyledButton(UnicodeSymbols.BULB + " Prüfen", ThemeManager.getSuccessColor());
        testUrlButton.setToolTipText("Prüft, ob die URL syntaktisch gültig ist");
        serverActions.add(openUrlButton);
        serverActions.add(Box.createHorizontalStrut(8));
        serverActions.add(copyUrlButton);
        serverActions.add(Box.createHorizontalStrut(8));
        serverActions.add(testUrlButton);
        serverCard.add(serverActions);
        serverCard.add(Box.createVerticalStrut(14));

        // URL field
        JPanel serverUrlPanel = new JPanel();
        serverUrlPanel.setLayout(new BoxLayout(serverUrlPanel, BoxLayout.Y_AXIS));
        serverUrlPanel.setOpaque(false);
        serverUrlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        serverUrlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel serverUrlLabel = new JLabel("Server URL:");
        serverUrlLabel.setFont(getFontByName(Font.BOLD, 13));
        serverUrlLabel.setForeground(ThemeManager.getTextPrimaryColor());
        serverUrlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverUrlPanel.add(serverUrlLabel);
        serverUrlField = new JTextField(DEFAULT_SERVER_URL);
        styleInputField(serverUrlField);
        serverUrlField.setToolTipText("z.B. https://example.com/scans.json");
        serverUrlField.setPreferredSize(new Dimension(600, 40));
        serverUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        serverUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverUrlPanel.add(serverUrlField);
        serverCard.add(serverUrlPanel);
        serverCard.add(Box.createVerticalStrut(8));

        // Small validation badge
        JLabel urlStatusLabel = new JLabel(" ");
        urlStatusLabel.setFont(getFontByName(Font.PLAIN, 12));
        urlStatusLabel.setForeground(ThemeManager.getTextSecondaryColor());
        urlStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        serverCard.add(urlStatusLabel);

        Runnable validateUrl = () -> {
            String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
            if (raw.isEmpty()) {
                urlStatusLabel.setText("Bitte eine URL eingeben.");
                urlStatusLabel.setForeground(getStatusErrorColor());
                return;
            }
            try {
                URI uri = new URI(raw);
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    urlStatusLabel.setText("Ungültiges Schema (nur http/https).");
                    urlStatusLabel.setForeground(getStatusErrorColor());
                    return;
                }
                if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                    urlStatusLabel.setText("Ungültige URL (Host fehlt).");
                    urlStatusLabel.setForeground(getStatusErrorColor());
                    return;
                }
                urlStatusLabel.setText("URL sieht gültig aus.");
                urlStatusLabel.setForeground(getStatusSuccessColor());
            } catch (Exception ex) {
                urlStatusLabel.setText("Ungültige URL (Syntaxfehler).");
                urlStatusLabel.setForeground(getStatusErrorColor());
            }
        };

        // Wire actions
        openUrlButton.addActionListener(e -> {
            validateUrl.run();
            try {
                String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
                if (!raw.isEmpty()) {
                    Desktop.getDesktop().browse(new URI(raw));
                }
            } catch (Exception ex) {
                new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Öffnen der URL:\n" + ex.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            }
        });
        copyUrlButton.addActionListener(e -> {
            String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(raw), null);
            new MessageDialog()
                .setTitle("Zwischenablage")
                .setMessage("URL wurde kopiert.")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .setIcon(Main.iconSmall)
                .setDuration(1250)
                .display();
        });
        testUrlButton.addActionListener(e -> {
            validateUrl.run();
            // Keep it lightweight: only syntax check here.
            // A real network ping would be added in a dedicated background worker.
        });
        // Live validate while typing
        serverUrlField.getDocument().addDocumentListener(new DocumentListener() {
            private void u() {
                validateUrl.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                u();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                u();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                u();
            }
        });
        // initial status
        validateUrl.run();

        // Add card to main panel
        connectionPanel.add(serverCard);
        connectionPanel.add(Box.createVerticalGlue());

        // Add connection panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.GLOBE + " Verbindung", connectionScroll);
    }

    private void buildDatabaseTab(JTabbedPane tabbedPane) {
        // === CATEGORY 3: Datenbank (Prettier, Left-Aligned) ===
        JPanel databasePanel = createCategoryPanel();
        databasePanel.setLayout(new BoxLayout(databasePanel, BoxLayout.Y_AXIS));
        JScrollPane databaseScroll = createScrollablePanel(databasePanel);

        // Card: Database Management
        JPanel dbCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung",
            "Datenbank bereinigen und Tabellen verwalten",
            null);
        dbCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        dbCard.setBackground(ThemeManager.getCardBackgroundColor());
        dbCard.setLayout(new BoxLayout(dbCard, BoxLayout.Y_AXIS));
        dbCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dbTitle = new JLabel(UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung");
        dbTitle.setFont(getFontByName(Font.BOLD, 22));
        dbTitle.setForeground(ThemeManager.getTitleTextColor());
        dbTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbCard.add(dbTitle);
        dbCard.add(Box.createVerticalStrut(8));

        JLabel dbDesc = new JLabel("Verwalten und bereinigen Sie Ihre Datenbank. Löschen Sie gezielt Tabellen oder führen Sie eine Komplettbereinigung durch.");
        dbDesc.setFont(getFontByName(Font.PLAIN, 13));
        dbDesc.setForeground(ThemeManager.getTextSecondaryColor());
        dbDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbCard.add(dbDesc);
        dbCard.add(Box.createVerticalStrut(18));

        // Database clear info
        JLabel databaseClearLabel = getDatabaseClearLabel();
        databaseClearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbCard.add(databaseClearLabel);
        dbCard.add(Box.createVerticalStrut(15));

        // Button row: Clear database
        JPanel dbButtonRow = new JPanel();
        dbButtonRow.setLayout(new BoxLayout(dbButtonRow, BoxLayout.X_AXIS));
        dbButtonRow.setOpaque(false);
        dbButtonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton clearDatabaseButton = createStyledButton(UnicodeSymbols.TRASH + " Datenbank Bereinigen",
            ThemeManager.getDangerColor());
        clearDatabaseButton.addActionListener(e -> clearDatabase());
        clearDatabaseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbButtonRow.add(clearDatabaseButton);
        dbCard.add(dbButtonRow);
        dbCard.add(Box.createVerticalStrut(18));

        // Selected table clear info
        JLabel selectedDatabaseClear = getSelectedDatabaseClear();
        selectedDatabaseClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbCard.add(selectedDatabaseClear);
        dbCard.add(Box.createVerticalStrut(12));

        // Table selection row
        List<String> tableNames = new ArrayList<>(DatabaseManager.ALLOWED_TABLES);
        JComboBox<String> tableComboBox = new JComboBox<>(tableNames.toArray(new String[0]));
        styleComboBox(tableComboBox);
        JPanel tableSelectionPanel = createLabeledComboBoxPanel("Tabelle auswählen:", tableComboBox);
        tableSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        dbCard.add(tableSelectionPanel);
        dbCard.add(Box.createVerticalStrut(14));

        // Button row: Delete selected table
        JPanel deleteTableButtonPanel = new JPanel();
        deleteTableButtonPanel.setLayout(new BoxLayout(deleteTableButtonPanel, BoxLayout.X_AXIS));
        deleteTableButtonPanel.setOpaque(false);
        deleteTableButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton deleteSelectedTableButton = createStyledButton(UnicodeSymbols.TRASH + " Ausgewählte Tabelle Löschen",
            ThemeManager.getWarningColor());
        deleteSelectedTableButton.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable == null || selectedTable.trim().isEmpty()) {
                new MessageDialog()
                .setTitle("Keine Tabelle ausgewählt")
                .setMessage("Bitte wählen Sie eine Tabelle aus der Dropdown-Liste aus, bevor Sie fortfahren.")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setIcon(Main.iconSmall)
                .display();
            return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("<html><b>WARNUNG: Tabelle '%s' wirklich löschen?</b><br/><br/>" +
                    "Diese Aktion kann <b>NICHT</b> rückgängig gemacht werden!<br/>" +
                    "Alle Daten in der Tabelle '%s' werden permanent gelöscht.<br/><br/>" +
                    "Möchten Sie fortfahren?</html>", selectedTable, selectedTable),
                "Tabelle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
            // Second confirmation for safety
            String userInput = new MessageDialog()
            .setTitle("Löschung bestätigen")
            .setMessage(String.format("<html>Bitte geben Sie den Tabellennamen '<b>%s</b>' ein,<br/>" +
                    "um die Löschung zu bestätigen:</html>", selectedTable))
            .setMessageType(JOptionPane.WARNING_MESSAGE)
            .setIcon(Main.iconSmall)
            .displayWithStringInput();
            if (userInput != null && userInput.trim().equals(selectedTable)) {
                deleteTable(selectedTable);
            } else if (userInput != null) {
                new MessageDialog()
                    .setTitle("Abgebrochen")
                    .setMessage("Der eingegebene Tabellenname stimmt nicht überein.\nLöschung abgebrochen.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
            }
            }
        });
        deleteSelectedTableButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteTableButtonPanel.add(deleteSelectedTableButton);
        dbCard.add(deleteTableButtonPanel);

        // Add card to main panel
        databasePanel.add(dbCard);
        databasePanel.add(Box.createVerticalGlue());

        // Add database panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.FLOPPY + " Datenbank", databaseScroll);
    }

    private void buildImportExportTab(JTabbedPane tabbedPane) {
        // === CATEGORY 4: Import & Export (Prettier, Left-Aligned) ===
        JPanel importExportPanel = createCategoryPanel();
        importExportPanel.setLayout(new BoxLayout(importExportPanel, BoxLayout.Y_AXIS));
        JScrollPane importExportScroll = createScrollablePanel(importExportPanel);

        // Card: Import & Export
        JPanel importExportCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.ARROW_UP + UnicodeSymbols.ARROW_DOWN + "️ Import & Export",
            "Datenimport und -export Funktionen",
            null);
        importExportCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        importExportCard.setBackground(ThemeManager.getCardBackgroundColor());
        importExportCard.setLayout(new BoxLayout(importExportCard, BoxLayout.Y_AXIS));
        importExportCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        importExportCard.add(Box.createVerticalStrut(15));
        JLabel importLabel = SettingsUtils.createStyledLabel(logger, fontComboBox, "Import:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor());
        importLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportCard.add(importLabel);
        importExportCard.add(Box.createVerticalStrut(5));
        JLabel importInfo = createInfoLabel("<html><div style='width:650px;'>Importieren Sie Artikel, Lieferanten oder andere Daten aus CSV-Dateien.<br/>Stellen Sie sicher, dass die CSV-Dateien das richtige Format haben.</div></html>");
        importInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportCard.add(importInfo);
        importExportCard.add(Box.createVerticalStrut(10));
        JButton importButton = createStyledButton(UnicodeSymbols.ARROW_UP + " Datenbank Importieren", ThemeManager.getSuccessColor());
        importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        importButton.addActionListener(e -> importFromCsv());
        importExportCard.add(importButton);
        importExportCard.add(Box.createVerticalStrut(20));

        JLabel exportLabel = SettingsUtils.createStyledLabel(logger, fontComboBox, "Export:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor());
        exportLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportCard.add(exportLabel);
        importExportCard.add(Box.createVerticalStrut(5));
        JLabel exportInfo = createInfoLabel("<html><div style='width:650px;'>Exportieren Sie Ihre Datenbankinhalte in CSV-Dateien zur Sicherung oder Weiterverarbeitung.</div></html>");
        exportInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportCard.add(exportInfo);
        importExportCard.add(Box.createVerticalStrut(10));
        JButton exportButton = createStyledButton(UnicodeSymbols.ARROW_DOWN + " Datenbank Exportieren", ThemeManager.getSecondaryColor());
        exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportButton.addActionListener(e -> {
            String message = "<html><b>Datenbank nach CSV exportieren?</b><br/><br/>" +
                    "Dies erstellt CSV-Dateien für:<br/>" +
                    "- Artikel (articles_export.csv)<br/>" +
                    "- Lieferanten (vendors_export.csv)<br/>" +
                    "- Kunden (clients_export.csv)<br/>" +
                    "- Bestellungen (orders_export.csv)<br/><br/>" +
                    "Speicherort: " + Main.getAppDataDir().getAbsolutePath() + "</html>";
            int confirm = new MessageDialog()
                .setTitle("Export bestätigen")
                .setMessage(message)
                .setMessageType(JOptionPane.QUESTION_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
            if (confirm == JOptionPane.YES_OPTION) {
            try {
                SettingsDataTransferService.ExportSummary summary = exportToCsv();
                if (summary.hasFailures()) {
                    new MessageDialog()
                        .setTitle("Export teilweise erfolgreich")
                        .setMessage("<html><b>Export teilweise erfolgreich</b><br/><br/>" +
                            "Erfolgreich exportiert: " + summary.getSuccessCount() + " von " + summary.getTotalTables() + " Tabellen.<br/>" +
                            "Bitte prüfen Sie die Protokolle für Details.<br/><br/>" +
                            "Speicherort:<br/>" +
                            Main.getAppDataDir().getAbsolutePath() + "</html>")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                } else {
                    new MessageDialog()
                        .setTitle("Export erfolgreich")
                        .setMessage("<html><b>OK Export erfolgreich!</b><br/><br/>" +
                            "Alle Tabellen wurden erfolgreich exportiert.<br/>" +
                            "Speicherort: <br/>" +
                            Main.getAppDataDir().getAbsolutePath() + "</html>")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
                }
            } catch (Exception ex) {
                new MessageDialog()
                    .setTitle("Fehler beim Export")
                    .setMessage("<html><b>X Fehler beim Export!</b><br/><br/>" +
                        ex.getMessage() + "</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            }
            }
        });
        importExportCard.add(exportButton);

        importExportPanel.add(importExportCard);
        importExportPanel.add(Box.createVerticalStrut(25));

        // Card: Einstellungen Profil
        JPanel settingsProfileCard = SettingsUtils.createSectionPanel(
            UnicodeSymbols.FLOPPY + " Einstellungen Profil",
            "Importieren oder exportieren Sie Ihre Einstellungen als Profil-Datei",
            null);
        settingsProfileCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
            BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        settingsProfileCard.setBackground(ThemeManager.getCardBackgroundColor());
        settingsProfileCard.setLayout(new BoxLayout(settingsProfileCard, BoxLayout.Y_AXIS));
        settingsProfileCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsProfileCard.add(Box.createVerticalStrut(15));
        JButton exportSettingsButton = createStyledButton(UnicodeSymbols.DOWNLOAD + " Einstellungen exportieren", ThemeManager.getSecondaryColor());
        exportSettingsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportSettingsButton.addActionListener(e -> exportSettingsProfile());
        JButton importSettingsButton = createStyledButton(UnicodeSymbols.UPLOAD + " Einstellungen importieren", ThemeManager.getSuccessColor());
        importSettingsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        importSettingsButton.addActionListener(e -> importSettingsProfile());
        settingsProfileCard.add(exportSettingsButton);
        settingsProfileCard.add(Box.createVerticalStrut(10));
        settingsProfileCard.add(importSettingsButton);

        importExportPanel.add(settingsProfileCard);
        importExportPanel.add(Box.createVerticalGlue());

        // Add import/export panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.DOWNLOAD + " Import/Export", importExportScroll);
    }

    private void buildAboutTab(JTabbedPane tabbedPane) {
        JPanel aboutPanel = createCategoryPanel();
        aboutPanel.setLayout(new BoxLayout(aboutPanel, BoxLayout.Y_AXIS));
        aboutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JScrollPane aboutScroll = createScrollablePanel(aboutPanel);

        JPanel appInfoCard = createAboutCard(
                UnicodeSymbols.INFO + " Über VEBO Lagersystem",
                "Informationen über die Anwendung",
                ThemeManager.getCardBackgroundColor(),
                new Insets(18, 24, 18, 24));

        JLabel appNameLabel = new JLabel(UnicodeSymbols.BOX + " VEBO Lagersystem");
        appNameLabel.setFont(getFontByName(Font.BOLD, 30));
        appNameLabel.setForeground(ThemeManager.getTextPrimaryColor());
        appNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        appNameLabel.setToolTipText("Besuchen Sie unsere Website: https://vebo.ch");
        appNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://vebo.ch"));
                } catch (Exception ex) {
                    logger.error("Fehler beim Öffnen der VEBO-Website: {}", ex.getMessage());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                appNameLabel.setForeground(adjustColor(ThemeManager.getAccentColor(), -0.20f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                appNameLabel.setForeground(ThemeManager.getTextPrimaryColor());
            }
        });

        JLabel versionLabel = new JLabel(UnicodeSymbols.TAG + " Version " + Main.VERSION);
        versionLabel.setFont(getFontByName(Font.PLAIN, 15));
        versionLabel.setForeground(ThemeManager.getTextSecondaryColor());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel aboutLeadLabel = new JLabel("Moderne Lagerverwaltung für den täglichen Einsatz.");
        aboutLeadLabel.setFont(getFontByName(Font.PLAIN, 13));
        aboutLeadLabel.setForeground(ThemeManager.getTextSecondaryColor());
        aboutLeadLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel aboutMetaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        aboutMetaRow.setOpaque(false);
        aboutMetaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutMetaRow.add(createAboutMetaBadge(
                "Kanal: " + UpdateManager.detectChannel(Main.VERSION),
                ThemeManager.getInputBackgroundColor(),
                ThemeManager.getTextSecondaryColor()));
        aboutMetaRow.add(createAboutMetaBadge(
                "Java " + System.getProperty("java.version"),
                ThemeManager.getInputBackgroundColor(),
                ThemeManager.getTextSecondaryColor()));

        JPanel aboutActionsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        aboutActionsRow.setOpaque(false);
        aboutActionsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        aboutActionsRow.add(createAboutMiniActionButton(
                UnicodeSymbols.GLOBE + " Website",
                "Öffnet https://vebo.ch",
                () -> {
                    try {
                        Desktop.getDesktop().browse(new URI("https://vebo.ch"));
                    } catch (Exception ex) {
                        logger.error("Fehler beim Öffnen der VEBO-Website: {}", ex.getMessage());
                    }
                }));
        aboutActionsRow.add(createAboutMiniActionButton(
                UnicodeSymbols.FOLDER + " Datenordner",
                "Öffnet den VEBO-Datenordner",
                () -> {
                    try {
                        Desktop.getDesktop().open(Main.getAppDataDir());
                    } catch (Exception ex) {
                        logger.error("Fehler beim Öffnen des Datenordners: {}", ex.getMessage());
                    }
                }));

        appInfoCard.add(appNameLabel);
        appInfoCard.add(Box.createVerticalStrut(8));
        appInfoCard.add(versionLabel);
        appInfoCard.add(Box.createVerticalStrut(6));
        appInfoCard.add(aboutLeadLabel);
        appInfoCard.add(Box.createVerticalStrut(10));
        appInfoCard.add(aboutMetaRow);
        appInfoCard.add(Box.createVerticalStrut(10));
        appInfoCard.add(aboutActionsRow);

        JPanel descCard = createAboutCard(
                UnicodeSymbols.INFO + " Beschreibung",
                "Was macht das VEBO Lagersystem?",
                ThemeManager.getInputBackgroundColor(),
                new Insets(16, 22, 16, 22));
        JLabel descTitle = new JLabel("Beschreibung:");
        descTitle.setFont(getFontByName(Font.BOLD, 15));
        descTitle.setForeground(ThemeManager.getTextPrimaryColor());
        descTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea descriptionArea = new JTextArea(
                "VEBO Lagersystem ist eine moderne Lagerverwaltungssoftware für die effiziente Verwaltung von Artikeln, Bestellungen, Lieferanten und Kunden. Die Anwendung bietet umfangreiche Funktionen für die Bestandsverwaltung, automatische Warnungen bei niedrigem Lagerbestand und QR-Code-basierte Artikelerfassung.");
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFont(getFontByName(Font.PLAIN, 13));
        descriptionArea.setForeground(ThemeManager.getTextPrimaryColor());
        descriptionArea.setBackground(ThemeManager.getInputBackgroundColor());
        descriptionArea.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        descriptionArea.setRows(5);
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        descCard.add(descTitle);
        descCard.add(Box.createVerticalStrut(6));
        descCard.add(descriptionArea);

        JPanel devCard = createAboutCard(
                UnicodeSymbols.DEVELOPER + " Entwickler",
                "Wer hat das System entwickelt?",
                ThemeManager.getCardBackgroundColor(),
                new Insets(16, 22, 16, 22));
        JLabel devName = SettingsUtils.createStyledLabel(logger, fontComboBox, UnicodeSymbols.USER + " Darryl Huber", 14, Font.BOLD,
                ThemeManager.getTextPrimaryColor());
        JLabel devOrg = SettingsUtils.createStyledLabel(logger, fontComboBox, UnicodeSymbols.BUILDING + " Organisation: VEBO Oensingen", 13, Font.PLAIN,
                ThemeManager.getTextSecondaryColor());
        devName.setAlignmentX(Component.LEFT_ALIGNMENT);
        devOrg.setAlignmentX(Component.LEFT_ALIGNMENT);
        devCard.add(devName);
        devCard.add(Box.createVerticalStrut(5));
        devCard.add(devOrg);

        JPanel sysCard = createAboutCard(
                UnicodeSymbols.DEVELOPER + " System-Information",
                "Technische Details zur Laufzeitumgebung",
                ThemeManager.getInputBackgroundColor(),
                new Insets(16, 22, 16, 22));
        JLabel sysJava = SettingsUtils.createStyledLabel(
                logger,
                fontComboBox,
                UnicodeSymbols.LAPTOP + " Java Version: " + System.getProperty("java.version"),
                12,
                Font.PLAIN,
                ThemeManager.getTextSecondaryColor());
        JLabel sysOs = SettingsUtils.createStyledLabel(
                logger,
                fontComboBox,
                UnicodeSymbols.MONITOR + " Betriebssystem: " + System.getProperty("os.name") + " "
                        + System.getProperty("os.version"),
                12,
                Font.PLAIN,
                ThemeManager.getTextSecondaryColor());
        JLabel sysDir = SettingsUtils.createStyledLabel(
                logger,
                fontComboBox,
                UnicodeSymbols.FOLDER + " Datenverzeichnis: " + Main.getAppDataDir().getAbsolutePath(),
                12,
                Font.PLAIN,
                ThemeManager.getTextSecondaryColor());
        sysJava.setAlignmentX(Component.LEFT_ALIGNMENT);
        sysOs.setAlignmentX(Component.LEFT_ALIGNMENT);
        sysDir.setAlignmentX(Component.LEFT_ALIGNMENT);
        sysCard.add(sysJava);
        sysCard.add(Box.createVerticalStrut(5));
        sysCard.add(sysOs);
        sysCard.add(Box.createVerticalStrut(5));
        sysCard.add(sysDir);

        JPanel updateCard = createAboutCard(
                UnicodeSymbols.DOWNLOAD + " Update-Verwaltung",
                "Prüfen Sie auf neue Versionen und Kanäle",
                ThemeManager.getCardBackgroundColor(),
                new Insets(16, 22, 16, 22));
        JPanel updateRow1 = createAboutButtonRow();
        updateRow1.add(createUpdateChannelButton(
                UnicodeSymbols.CHECK + " Stable-Updates",
                ThemeManager.getSecondaryColor(),
                "Prüft auf neue stabile Versionen",
                "stable"));
        updateRow1.add(createUpdateChannelButton(
                UnicodeSymbols.BETA + " Beta-Updates",
                ThemeManager.getWarningColor(),
                "Prüft auf neue Beta-Versionen",
                "beta"));
        updateCard.add(updateRow1);
        updateCard.add(Box.createVerticalStrut(8));

        JPanel updateRow2 = createAboutButtonRow();
        updateRow2.add(createUpdateChannelButton(
                UnicodeSymbols.EXPERIMENT + " Alpha-Updates",
                ThemeManager.getDangerColor(),
                "Prüft auf neue Alpha-Versionen (experimentell)",
                "alpha"));
        updateRow2.add(createUpdateChannelButton(
                "🧪 Testing-Updates",
                ThemeManager.getAccentColor(),
                "Prüft auf neue Testing-Versionen (Entwicklung)",
                "testing"));
        updateCard.add(updateRow2);
        updateCard.add(Box.createVerticalStrut(12));

        JLabel versionInfoLabel = new JLabel("<html><div style='padding: 8px; background: "
                + toHexColor(ThemeManager.getInputBackgroundColor())
                + "; border-radius: 6px; margin-top: 8px; margin-bottom: 4px;'><b>Aktuelle Version:</b> "
                + Main.VERSION + "<br/>"
                + "<span style='font-size: 11px; color: " + toHexColor(ThemeManager.getTextSecondaryColor())
                + ";'>Kanal: " + UpdateManager.detectChannel(Main.VERSION) + "</span></div></html>");
        versionInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        versionInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updateCard.add(versionInfoLabel);

        JPanel copyrightCard = createAboutCard(
                UnicodeSymbols.COPYRIGHT + " Copyright",
                "Rechtliche Hinweise zur Software",
                ThemeManager.getInputBackgroundColor(),
                new Insets(14, 20, 14, 20));
        JLabel copyrightLabel = SettingsUtils.createStyledLabel(
                logger,
                fontComboBox,
                "© 2026 VEBO Oensingen. Alle Rechte vorbehalten.",
                11,
                Font.PLAIN,
                ThemeManager.getTextSecondaryColor());
        JLabel licenseLabel = SettingsUtils.createStyledLabel(logger, fontComboBox,
                "Diese Software wird bereitgestellt \"wie sie ist\", ohne jegliche Garantie.",
                11,
                Font.ITALIC,
                ThemeManager.getTextSecondaryColor());
        copyrightLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        licenseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyrightCard.add(copyrightLabel);
        copyrightCard.add(Box.createVerticalStrut(5));
        copyrightCard.add(licenseLabel);

        aboutPanel.add(appInfoCard);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(descCard);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(devCard);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(sysCard);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(updateCard);
        aboutPanel.add(Box.createVerticalStrut(18));
        aboutPanel.add(copyrightCard);
        aboutPanel.add(Box.createVerticalGlue());

        tabbedPane.addTab(UnicodeSymbols.INFO + " Über", aboutScroll);
    }

    private JPanel createAboutCard(String title, String description, Color background, Insets padding) {
        JPanel card = SettingsUtils.createSectionPanel(title, description, null);
        int top = padding == null ? 16 : padding.top;
        int left = padding == null ? 22 : padding.left;
        int bottom = padding == null ? 16 : padding.bottom;
        int right = padding == null ? 22 : padding.right;
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(getSoftGlassHighlight(), 1, true),
                        BorderFactory.createLineBorder(getSoftGlassBorder(), 1, true)),
                BorderFactory.createEmptyBorder(top, left, bottom, right)));
        Color base = background == null ? ThemeManager.getCardBackgroundColor() : background;
        card.setBackground(getSoftGlassSurface(base));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        return card;
    }

    private JPanel createAboutButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private JButton createUpdateChannelButton(String text, Color color, String tooltip, String channel) {
        JButton button = createStyledButton(text, color);
        button.setToolTipText(tooltip);
        button.setPreferredSize(new Dimension(180, 40));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> checkForUpdates(channel));
        return button;
    }

    private JLabel createAboutMetaBadge(String text, Color background, Color foreground) {
        JLabel badge = new JLabel(text == null ? "" : text);
        badge.setFont(getFontByName(Font.PLAIN, 11));
        badge.setForeground(foreground == null ? ThemeManager.getTextSecondaryColor() : foreground);
        badge.setOpaque(true);
        badge.setBackground(background == null ? ThemeManager.getInputBackgroundColor() : background);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return badge;
    }

    private JButton createAboutMiniActionButton(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text == null ? "" : text);
        styleSecondaryActionButton(button);
        button.setToolTipText(tooltip);
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
        return button;
    }

    private void styleSecondaryActionButton(JButton button) {
        SettingsStylingService.styleSecondaryActionButton(button);
    }

    private void styleInputField(JTextField field) {
        SettingsStylingService.styleInputField(field);
    }

    private void addNewCategory(String newCategory, int from, int to) {
        loadCategories();
        if (newCategory == null || newCategory.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Der Kategoriename darf nicht leer sein.",
                    "Ungültiger Name",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (categories.containsKey(newCategory)) {
            JOptionPane.showMessageDialog(this,
                    "Diese Kategorie existiert bereits.",
                    "Kategorie existiert",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        ArticleUtils.addNewCategory(newCategory, from, to);
        loadCategories();
        JOptionPane.showMessageDialog(this,
                "Kategorie '" + newCategory + "' wurde erfolgreich hinzugefügt.",
                "Kategorie hinzugefügt",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void openCategorySettingsFile() {
        SettingsMaintenanceService.openCategorySettingsFile(logger);
    }

    private void deleteOldLogs() {
        SettingsMaintenanceService.deleteOldLogs(logger);
    }

    private void deleteAllLogs() {
        SettingsMaintenanceService.deleteAllLogs(logger, this);
    }

    private void openLogsFolder() {
        SettingsMaintenanceService.openLogsFolder(logger);
    }

    private void openSettingsFolder() {
        SettingsMaintenanceService.openSettingsFolder(this);
    }

    /**
     * Creates a category panel for tabbed interface with proper width management
     */
    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeManager.getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        return panel;
    }

    /**
     * Creates a scrollable panel wrapper with optimized settings
     */
    private JScrollPane createScrollablePanel(JPanel panel) {
        if (panel == null)
            throw new NullPointerException("Panel cannot be null");
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(ThemeManager.getBackgroundColor());

        return scrollPane;
    }

    private static JLabel getSelectedDatabaseClear() {
        JLabel selectedDatabaseClear = new JLabel("<html><div style='padding: 8px 0'><p>" +
                "<i>Hinweis: Diese Aktion löscht eine bestimmte Tabelle. Bitte seien Sie vorsichtig!</i>" +
                "</div></html>");
        selectedDatabaseClear.setFont(getFontByName(Font.PLAIN, 12));
        selectedDatabaseClear.setForeground(ThemeManager.getTextSecondaryColor());
        selectedDatabaseClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        return selectedDatabaseClear;
    }

    private static JLabel getDatabaseClearLabel() {
        JLabel databaseClearLabel = new JLabel("<html><div style='padding: 8px 0'><p>" +
                "Mit dem Knopf <b>Datenbank Bereinigen</b> werden alle Daten in der Datenbank gelöscht!" +
                "</div></html>");
        databaseClearLabel.setFont(getFontByName(Font.PLAIN, 13));
        databaseClearLabel.setForeground(ThemeManager.getTextSecondaryColor());
        databaseClearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return databaseClearLabel;
    }

    private static JLabel getWarningInfoLabel() {
        JLabel warningInfoLabel = new JLabel("<html><div style='padding: 8px 0;'>" +
                "<i>Warnungen werden automatisch in diesem Intervall angezeigt, wenn neue ungelesene Warnungen vorhanden sind.</i>"
                +
                "</div></html>");
        warningInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        warningInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return warningInfoLabel;
    }

    private void addSectionResetButton(JPanel section, Runnable action) {
        if (section == null || action == null)
            throw new NullPointerException("Section and action cannot be null");
        Object actions = section.getClientProperty("headerActions");
        if (actions instanceof JPanel panel) {
            JButton resetButton = createHeaderActionButton();
            resetButton.addActionListener(e -> action.run());
            panel.add(resetButton);
        }
    }

    private JPanel createColorRow(String labelText, String key) {
        if (labelText == null || key == null)
            throw new NullPointerException("Label text and key cannot be null");
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        row.add(label);

        JButton colorButton = createColorButton();
        JLabel hexLabel = new JLabel("Standard");
        hexLabel.setFont(getFontByName(Font.PLAIN, 12));
        hexLabel.setForeground(ThemeManager.getTextSecondaryColor());

        switch (key) {
            case "theme_accent_color" -> {
                accentColorButton = colorButton;
                accentHexLabel = hexLabel;
            }
            case "theme_header_color" -> {
                headerColorButton = colorButton;
                headerHexLabel = hexLabel;
            }
            case "theme_button_color" -> {
                buttonColorButton = colorButton;
                buttonHexLabel = hexLabel;
            }
        }

        colorButton.addActionListener(e -> {
            Color current = getSelectedColorForKey(key);
            Color chosen = JColorChooser.showDialog(this, "Farbe waehlen", current);
            if (chosen != null) {
                setSelectedColorForKey(key, chosen);
                updateColorControls();
                updatePreview();
            }
        });

        row.add(colorButton);
        row.add(hexLabel);
        return row;
    }

    private JButton createColorButton() {
        JButton button = new JButton(" ");
        button.setPreferredSize(new Dimension(42, 24));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(adjustColor(ThemeManager.getAccentColor(), -0.15f), 2, true),
                        BorderFactory.createEmptyBorder(1, 1, 1, 1)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)));
            }
        });
        return button;
    }

    private Color getSelectedColorForKey(String key) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        return switch (key) {
            case "theme_accent_color" -> selectedAccentColor;
            case "theme_header_color" -> selectedHeaderColor;
            case "theme_button_color" -> selectedButtonColor;
            default -> null;
        };
    }

    private void setSelectedColorForKey(String key, Color color) {
        if (key == null)
            throw new NullPointerException("Key cannot be null");
        switch (key) {
            case "theme_accent_color" -> selectedAccentColor = color;
            case "theme_header_color" -> selectedHeaderColor = color;
            case "theme_button_color" -> selectedButtonColor = color;
        }
    }

    private void updateColorControls() {
        updateColorButton(accentColorButton, accentHexLabel, selectedAccentColor);
        updateColorButton(headerColorButton, headerHexLabel, selectedHeaderColor);
        updateColorButton(buttonColorButton, buttonHexLabel, selectedButtonColor);
    }

    private void updateColorButton(JButton button, JLabel label, Color color) {
        if (button == null || label == null) {
            return;
        }
        if (color == null) {
            button.setBackground(ThemeManager.getInputBackgroundColor());
            label.setText("Standard");
        } else {
            button.setBackground(color);
            label.setText(SettingsProfileService.colorToSetting(color));
        }
    }

    private JButton createHeaderActionButton() {
        JButton button = new JButton("Zuruecksetzen");
        styleSecondaryActionButton(button);
        return button;
    }

    private JPanel createPreviewPanel() {
        SettingsUtils.RoundedPanel previewCard = new SettingsUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(),
                12);
        previewCardPanel = previewCard;
        previewCard.setLayout(new BorderLayout(12, 12));
        previewCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel textPanel = new JPanel();
        previewTextPanel = textPanel;
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(true);
        previewTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewBodyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(previewTitleLabel);
        textPanel.add(Box.createVerticalStrut(6));
        textPanel.add(previewBodyLabel);
        textPanel.add(Box.createVerticalStrut(10));
        previewButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(previewButton);

        previewTable.setRowHeight(22);
        previewTable.setEnabled(false);
        JScrollPane tableScroll = new JScrollPane(previewTable);
        previewTableScroll = tableScroll;
        tableScroll.setPreferredSize(new Dimension(320, 90));
        tableScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        tableScroll.getViewport().setOpaque(true);

        previewCard.add(textPanel, BorderLayout.WEST);
        previewCard.add(tableScroll, BorderLayout.EAST);

        updatePreview();
        return previewCard;
    }

    private void updatePreview() {
        boolean darkMode = darkModeCheckbox != null && darkModeCheckbox.isSelected();
        Color card = darkMode ? ThemeManager.Dark.CARD_BACKGROUND : ThemeManager.Light.CARD_BACKGROUND;
        Color text = darkMode ? ThemeManager.Dark.TEXT_PRIMARY : ThemeManager.Light.TEXT_PRIMARY;
        Color secondary = darkMode ? ThemeManager.Dark.TEXT_SECONDARY : ThemeManager.Light.TEXT_SECONDARY;
        Color border = darkMode ? ThemeManager.Dark.BORDER : ThemeManager.Light.BORDER;
        Color header = selectedHeaderColor != null ? selectedHeaderColor
                : (darkMode ? ThemeManager.Dark.HEADER_BG : ThemeManager.Light.HEADER_BG);
        Color accent = selectedAccentColor != null ? selectedAccentColor : ThemeManager.getAccentColor();
        Color button = selectedButtonColor != null ? selectedButtonColor
                : (darkMode ? ThemeManager.Dark.BUTTON_BG : ThemeManager.Light.BUTTON_BG);

        String fontName = fontComboBox == null ? DEFAULT_FONT_STYLE : (String) fontComboBox.getSelectedItem();
        int tableSize = fontSizeTableSpinner == null ? 16 : (Integer) fontSizeTableSpinner.getValue();
        int tabSize = fontSizeTabSpinner == null ? 15 : (Integer) fontSizeTabSpinner.getValue();

        previewTitleLabel.setFont(new Font(fontName, Font.BOLD, 16));
        previewTitleLabel.setForeground(getReadableTextColor(header));
        previewTitleLabel.setOpaque(true);
        previewTitleLabel.setBackground(header);
        previewTitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        previewBodyLabel.setFont(new Font(fontName, Font.PLAIN, 13));
        previewBodyLabel.setForeground(secondary);
        previewButton.setFont(new Font(fontName, Font.BOLD, 12));
        previewButton.setBackground(button);
        previewButton.setForeground(getReadableTextColor(button));

        previewTable.setFont(new Font(fontName, Font.PLAIN, Math.max(11, tableSize - 2)));
        previewTable.setForeground(text);
        previewTable.setBackground(card);
        previewTable.setSelectionBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
        previewTable.getTableHeader().setFont(new Font(fontName, Font.BOLD, Math.max(10, tableSize - 3)));
        previewTable.getTableHeader()
                .setBackground(darkMode ? ThemeManager.Dark.TABLE_HEADER_BG : ThemeManager.Light.TABLE_HEADER_BG);
        previewTable.getTableHeader()
                .setForeground(darkMode ? ThemeManager.Dark.TABLE_HEADER_FG : ThemeManager.Light.TABLE_HEADER_FG);

        tableFontSample.setFont(new Font(fontName, Font.PLAIN, tableSize));
        tabFontSample.setFont(new Font(fontName, Font.PLAIN, tabSize));
        fontSample.setFont(new Font(fontName, Font.PLAIN, 16));
        tableFontSample.setForeground(text);
        tabFontSample.setForeground(text);
        fontSample.setForeground(text);

        if (previewCardPanel != null) {
            previewCardPanel.setBackground(card);
        }
        if (previewTextPanel != null) {
            previewTextPanel.setBackground(card);
        }
        if (previewTableScroll != null) {
            previewTableScroll.getViewport().setBackground(card);
            previewTableScroll.setBackground(card);
            previewTableScroll.setBorder(BorderFactory.createLineBorder(border, 1));
        }
        previewTable.setGridColor(border);
    }

    private void configureSlider(JSlider slider, JSpinner spinner, JLabel sampleLabel) {
        if (slider == null || spinner == null || sampleLabel == null)
            throw new IllegalArgumentException("slider, spinner and sampleLabel must not be null");
        final String baseText = sampleLabel.getText().replaceAll("\\s*\\(\\d+px\\)\\s*$", "");

        slider.setPaintTicks(true);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setOpaque(false);

        // Initialize label
        Object sv = spinner.getValue();
        int initial = sv instanceof Integer ? (Integer) sv : slider.getValue();
        sampleLabel.setText(baseText + " (" + initial + "px)");

        slider.addChangeListener(e -> {
            int value = slider.getValue();
            if (!valueEqualsSpinner(value, spinner)) {
                spinner.setValue(value);
            }
            sampleLabel.setText(baseText + " (" + value + "px)");
            updatePreview();
        });

        spinner.addChangeListener(e -> {
            int value = (Integer) spinner.getValue();
            if (slider.getValue() != value) {
                slider.setValue(value);
            }
            sampleLabel.setText(baseText + " (" + value + "px)");
            updatePreview();
        });
    }

    private boolean valueEqualsSpinner(int value, JSpinner spinner) {
        if (spinner == null)
            throw new IllegalArgumentException("spinner must not be null");
        Object spinnerValue = spinner.getValue();
        return spinnerValue instanceof Integer && (Integer) spinnerValue == value;
    }

    private void applySearchFilter() {
        String query;
        if (searchPlaceholderActive) {
            query = "";
        } else {
            query = settingsSearchField.getText() == null ? "" : settingsSearchField.getText().trim().toLowerCase();
        }
        for (JComponent section : searchableSections) {
            Object searchText = section.getClientProperty("searchText");
            boolean visible = query.isEmpty() || (searchText instanceof String text && text.contains(query));
            section.setVisible(visible);
        }
        revalidate();
        repaint();
    }

    /**
     * Styles a checkbox with a modern appearance and enhanced click area
     */
    private void styleCheckbox(JCheckBox checkbox) {
        SettingsStylingService.styleCheckbox(checkbox);
    }

    /**
     * Styles a spinner with modern, rounded appearance and focus effects
     */
    private void styleSpinner(JSpinner spinner) {
        SettingsStylingService.styleSpinner(spinner);
    }

    /**
     * Creates a modern styled button with smooth hover effects and professional
     * appearance
     */
    private JButton createStyledButton(String text, Color originalBg) {
        if (text == null)
            throw new IllegalArgumentException("text must not be null");
        Color baseBg = originalBg != null ? originalBg : ThemeManager.getButtonBackgroundColor();
        JButton button = new JButton(text);
        button.setFont(getFontByName(Font.BOLD, 14));
        button.setForeground(getReadableTextColor(baseBg));
        button.setBackground(baseBg);
        Border normalBorder = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(adjustColor(baseBg, -0.18f), 1, true),
            BorderFactory.createEmptyBorder(14, 28, 14, 28));
        button.setBorder(normalBorder);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("theme.button.custom", Boolean.TRUE);

        // Set consistent height
        button.setPreferredSize(new Dimension(
                button.getPreferredSize().width,
                48));

        Color hoverBg = adjustColor(baseBg, -0.08f);
        Color pressedBg = adjustColor(baseBg, -0.16f);
        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(adjustColor(hoverBg, -0.12f), 1, true),
                BorderFactory.createEmptyBorder(14, 28, 14, 28));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseBg);
                button.setBorder(normalBorder);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.contains(e.getPoint())) {
                    button.setBackground(hoverBg);
                } else {
                    button.setBackground(baseBg);
                }
            }
        });

        return button;
    }

    private Color adjustColor(Color color, float factor) {
        if (color == null) {
            return ThemeManager.getButtonBackgroundColor();
        }
        int r = clampColor(Math.round(color.getRed() * (1.0f + factor)));
        int g = clampColor(Math.round(color.getGreen() * (1.0f + factor)));
        int b = clampColor(Math.round(color.getBlue() * (1.0f + factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private Color getSoftGlassSurface(Color base) {
        Color fallback = base == null ? ThemeManager.getCardBackgroundColor() : base;
        int alpha = switch (GLASS_INTENSITY) {
            case MEDIUM -> ThemeManager.isDarkMode() ? 186 : 236;
            case SUBTLE -> ThemeManager.isDarkMode() ? 170 : 228;
        };
        return new Color(fallback.getRed(), fallback.getGreen(), fallback.getBlue(), alpha);
    }

    private Color getSoftGlassBorder() {
        Color border = ThemeManager.getBorderColor();
        int alpha = switch (GLASS_INTENSITY) {
            case MEDIUM -> ThemeManager.isDarkMode() ? 188 : 196;
            case SUBTLE -> ThemeManager.isDarkMode() ? 165 : 175;
        };
        return new Color(border.getRed(), border.getGreen(), border.getBlue(), alpha);
    }

    private Color getSoftGlassHighlight() {
        Color light = ThemeManager.isDarkMode()
                ? adjustColor(ThemeManager.getTextPrimaryColor(), -0.35f)
                : ThemeManager.getTextPrimaryColor();
        int alpha = switch (GLASS_INTENSITY) {
            case MEDIUM -> ThemeManager.isDarkMode() ? 96 : 120;
            case SUBTLE -> ThemeManager.isDarkMode() ? 70 : 92;
        };
        return new Color(light.getRed(), light.getGreen(), light.getBlue(), alpha);
    }

    private Color getReadableTextColor(Color bg) {
        if (bg == null) {
            return ThemeManager.getTextPrimaryColor();
        }
        double luminance = (0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue()) / 255.0;
        return luminance > 0.6 ? new Color(20, 20, 20) : Color.WHITE;
    }

    private Color getStatusErrorColor() {
        return ThemeManager.getDangerColor();
    }

    private Color getStatusSuccessColor() {
        return ThemeManager.getSuccessColor();
    }

    /**
     * Loads settings from the system
     */
    private void loadSettings() {
        try {
            Properties props = SettingsRuntimeService.readSettingsSnapshot();
            if (props != null) {
                applySettingsProperties(props);
                tableFontSlider.setValue((Integer) fontSizeTableSpinner.getValue());
                tabFontSlider.setValue((Integer) fontSizeTabSpinner.getValue());

                System.out.println("[SettingsGUI] Einstellungen geladen");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Laden der Einstellungen: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Laden der Einstellungen: " + e.getMessage());
        }
    }

    /**
     * Saves settings and applies them
     */
    private void saveSettings() {
        try {
            if (Main.settings != null) {
                int interval = (Integer) stockCheckIntervalSpinner.getValue();
                boolean enableWarnings = enableWarningDisplayCheckbox.isSelected();
                int warningInterval = (Integer) warningDisplayIntervalSpinner.getValue();
                boolean enableAutoCheck = enableAutoStockCheckCheckbox.isSelected();
                boolean darkMode = darkModeCheckbox.isSelected();

                SettingsRuntimeService.persistSettings(collectSettingsProperties());

                System.out.println("[SettingsGUI] Einstellungen gespeichert");

                applySettings(interval, enableWarnings, warningInterval, enableAutoCheck, darkMode);
                ThemeManager.setCustomColors(selectedAccentColor, selectedHeaderColor, selectedButtonColor);

                new MessageDialog()
                        .setTitle("Einstellungen gespeichert")
                        .setMessage("<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                                "Die neuen Einstellungen wurden erfolgreich übernommen.</html>")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
                dispose();
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Speichern der Einstellungen: " + e.getMessage());
            new MessageDialog()
                    .setTitle("Fehler beim Speichern")
                    .setMessage("<html><b>Fehler beim Speichern!</b><br/><br/>" +
                            "Die Einstellungen konnten nicht gespeichert werden:<br/>" +
                            e.getMessage() + "</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            Main.logUtils.addLog("Fehler beim Speichern der Einstellungen: " + e.getMessage());
        }
    }

    private void resetStockCheckDefaults() {
        enableAutoStockCheckCheckbox.setSelected(DEFAULT_ENABLE_AUTO_STOCK_CHECK);
        stockCheckIntervalSpinner.setValue(DEFAULT_STOCK_CHECK_INTERVAL);
    }

    private void resetWarningDefaults() {
        enableWarningDisplayCheckbox.setSelected(DEFAULT_ENABLE_WARNINGS);
        warningDisplayIntervalSpinner.setValue(DEFAULT_WARNING_INTERVAL);
    }

    private void resetQrCodeDefaults() {
        automaticImportCheckBox.setSelected(DEFAULT_ENABLE_QR_IMPORT);
        qrCodeImportIntervalSpinner.setValue(DEFAULT_QR_IMPORT_INTERVAL);
    }

    private void resetFontDefaults() {
        fontComboBox.setSelectedItem(DEFAULT_FONT_STYLE);
        updatePreview();
    }

    private void resetTableDefaults() {
        fontSizeTableSpinner.setValue(DEFAULT_TABLE_FONT_SIZE);
        fontSizeTabSpinner.setValue(DEFAULT_TAB_FONT_SIZE);
        updatePreview();
    }

    private void resetThemeDefaults() {
        darkModeCheckbox.setSelected(DEFAULT_DARK_MODE);
        themeComboBox.setSelectedItem(DEFAULT_DARK_MODE ? "Dark" : "Light");
        themeComboBox.setEnabled(!DEFAULT_DARK_MODE);
        updatePreview();
    }

    private void resetColorDefaults() {
        selectedAccentColor = null;
        selectedHeaderColor = null;
        selectedButtonColor = null;
        updateColorControls();
        updatePreview();
    }

    private void resetServerDefaults() {
        serverUrlField.setText(DEFAULT_SERVER_URL);
    }

    private void resetAllDefaults() {
        resetStockCheckDefaults();
        resetWarningDefaults();
        resetQrCodeDefaults();
        resetFontDefaults();
        resetTableDefaults();
        resetThemeDefaults();
        resetServerDefaults();
        resetColorDefaults();
    }

    private void exportSettingsProfile() {
        SettingsProfileService.exportSettingsProfile(this, this::collectSettingsProperties);
    }

    private void importSettingsProfile() {
        SettingsProfileService.importSettingsProfile(this, this::applySettingsProperties);
    }

    private Properties collectSettingsProperties() {
        Properties props = new Properties();
        props.setProperty("stock_check_interval", String.valueOf(stockCheckIntervalSpinner.getValue()));
        props.setProperty("enable_hourly_warnings", String.valueOf(enableWarningDisplayCheckbox.isSelected()));
        props.setProperty("warning_display_interval", String.valueOf(warningDisplayIntervalSpinner.getValue()));
        props.setProperty("enable_auto_stock_check", String.valueOf(enableAutoStockCheckCheckbox.isSelected()));
        props.setProperty("server_url", serverUrlField.getText().trim());
        props.setProperty("enable_automatic_import_qrcode", String.valueOf(automaticImportCheckBox.isSelected()));
        props.setProperty("qrcode_import_interval", String.valueOf(qrCodeImportIntervalSpinner.getValue()));
        props.setProperty("dark_mode", String.valueOf(darkModeCheckbox.isSelected()));
        props.setProperty("table_font_size", String.valueOf(fontSizeTableSpinner.getValue()));
        props.setProperty("table_font_size_tab", String.valueOf(fontSizeTabSpinner.getValue()));
        Object fontStyle = fontComboBox.getSelectedItem();
        props.setProperty("font_style", fontStyle == null ? DEFAULT_FONT_STYLE : fontStyle.toString());
        props.setProperty("theme_accent_color", SettingsProfileService.colorToSetting(selectedAccentColor));
        props.setProperty("theme_header_color", SettingsProfileService.colorToSetting(selectedHeaderColor));
        props.setProperty("theme_button_color", SettingsProfileService.colorToSetting(selectedButtonColor));
        return props;
    }

    private void applySettingsProperties(Properties props) {
        if (props == null)
            throw new IllegalArgumentException("props must not be null");
        stockCheckIntervalSpinner
                .setValue(SettingsProfileService.parseIntProperty(props, "stock_check_interval", DEFAULT_STOCK_CHECK_INTERVAL));
        enableWarningDisplayCheckbox.setSelected(Boolean
                .parseBoolean(props.getProperty("enable_hourly_warnings", String.valueOf(DEFAULT_ENABLE_WARNINGS))));
        warningDisplayIntervalSpinner
                .setValue(SettingsProfileService.parseIntProperty(props, "warning_display_interval", DEFAULT_WARNING_INTERVAL));
        enableAutoStockCheckCheckbox.setSelected(Boolean.parseBoolean(
                props.getProperty("enable_auto_stock_check", String.valueOf(DEFAULT_ENABLE_AUTO_STOCK_CHECK))));
        serverUrlField.setText(props.getProperty("server_url", DEFAULT_SERVER_URL));
        automaticImportCheckBox.setSelected(Boolean.parseBoolean(
                props.getProperty("enable_automatic_import_qrcode", String.valueOf(DEFAULT_ENABLE_QR_IMPORT))));
        qrCodeImportIntervalSpinner
                .setValue(SettingsProfileService.parseIntProperty(props, "qrcode_import_interval", DEFAULT_QR_IMPORT_INTERVAL));
        boolean darkMode = Boolean.parseBoolean(props.getProperty("dark_mode", String.valueOf(DEFAULT_DARK_MODE)));
        darkModeCheckbox.setSelected(darkMode);
        themeComboBox.setSelectedItem(darkMode ? "Dark" : "Light");
        themeComboBox.setEnabled(!darkMode);
        fontSizeTableSpinner.setValue(SettingsProfileService.parseIntProperty(props, "table_font_size", DEFAULT_TABLE_FONT_SIZE));
        fontSizeTabSpinner.setValue(SettingsProfileService.parseIntProperty(props, "table_font_size_tab", DEFAULT_TAB_FONT_SIZE));
        fontComboBox.setSelectedItem(props.getProperty("font_style", DEFAULT_FONT_STYLE));
        selectedAccentColor = SettingsProfileService.parseColor(props.getProperty("theme_accent_color"));
        selectedHeaderColor = SettingsProfileService.parseColor(props.getProperty("theme_header_color"));
        selectedButtonColor = SettingsProfileService.parseColor(props.getProperty("theme_button_color"));
        updateColorControls();
        updatePreview();
    }

    @SuppressWarnings("MagicConstant")
    public static Font getFontByName(int style, int fontSize) {
        String fontName = Main.settings.getProperty("font_style");
        if (fontName == null || fontName.trim().isEmpty()) {
            Font uiFont = UIManager.getFont("Label.font");
            fontName = uiFont == null ? DEFAULT_FONT_STYLE : uiFont.getFamily();
        } else if (isWindows()
                && ("Arial".equalsIgnoreCase(fontName) || "Arial Unicode MS".equalsIgnoreCase(fontName))) {
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
     * Applies settings to the running system
     */
    private void applySettings(int interval, boolean enableWarnings, int warningInterval, boolean enableAutoCheck,
            boolean darkMode) {
        SettingsRuntimeService.applyRuntimeSettings(
                logger,
                interval,
                enableWarnings,
                warningInterval,
                enableAutoCheck,
                darkMode,
                automaticImportCheckBox.isSelected(),
                (Integer) qrCodeImportIntervalSpinner.getValue());
    }

    /**
     * Checks for updates based on the selected release channel (stable, beta,
     * alpha, testing)
     * Shows a dialog with update information and download link if available
     */
    private void checkForUpdates(String channel) {
        SettingsUpdateService.checkForUpdates(this, logger, channel);
    }

    public void display() {
        setVisible(true);
    }

    /**
     * Deletes a specific table from the database
     */
    private void deleteTable(String tableName) {
        SettingsMaintenanceService.deleteTable(this, logger, tableName);
    }

    /**
     * Clears the database after user confirmation
     */
    private void clearDatabase() {
        SettingsMaintenanceService.clearDatabase(this, logger);
    }

    /**
     * Creates a panel with a label, spinner, and unit label
     */
    private JPanel createLabeledSpinnerPanel(String labelText, JSpinner spinner, String unitText, int columns) {
        if (labelText == null)
            throw new IllegalArgumentException("labelText must not be null");
        if (spinner == null)
            throw new IllegalArgumentException("spinner must not be null");
        if (unitText == null)
            throw new IllegalArgumentException("unitText must not be null");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        spinner.setFont(getFontByName(Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
        styleSpinner(spinner);

        JLabel unitLabel = new JLabel(unitText);
        unitLabel.setFont(getFontByName(Font.PLAIN, 12));
        unitLabel.setForeground(ThemeManager.getTextSecondaryColor());

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(spinner);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(unitLabel);

        return panel;
    }

    /**
     * Creates a panel with a label and combo box
     */
    private JPanel createLabeledComboBoxPanel(String labelText, JComboBox<?> comboBox) {
        if (labelText == null)
            throw new IllegalArgumentException("labelText must not be null");
        if (comboBox == null)
            throw new IllegalArgumentException("comboBox must not be null");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        comboBox.setPreferredSize(new Dimension(230, 36));

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(comboBox);

        return panel;
    }

    /**
     * Creates an info label with HTML content
     */
    private JLabel createInfoLabel(String htmlContent) {
        String content = htmlContent == null ? "" : htmlContent.trim();
        if (!content.toLowerCase(Locale.ROOT).startsWith("<html>")) {
            content = "<html><div style='line-height:1.45;'>" + content + "</div></html>";
        }
        JLabel label = new JLabel(content);
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void importFromCsv() {
        SettingsDataTransferService.importFromCsv();
    }

    /**
     * Exports all database tables to CSV files in the application data directory.
     * Creates separate CSV files for articles, vendors, clients, and orders.
     */
    private static SettingsDataTransferService.ExportSummary exportToCsv() {
        return SettingsDataTransferService.exportToCsv();
    }

    private void styleComboBox(JComboBox<String> combo) {
        SettingsStylingService.styleComboBox(combo);
    }

    private static List<String> getAllFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        return Arrays.asList(fontNames);
    }

    /**
     * Convert Color to hex string format for HTML
     */
    private static String toHexColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

}
