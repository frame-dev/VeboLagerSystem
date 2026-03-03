package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;

import static ch.framedev.lagersystem.main.Main.databaseManager;
import static ch.framedev.lagersystem.main.Main.settings;
import static ch.framedev.lagersystem.utils.ArticleUtils.categories;
import static ch.framedev.lagersystem.utils.ArticleUtils.loadCategories;

/**
 * Moderne Einstellungen-GUI für das VEBO Lagersystem.
 *
 * <p>Diese Ansicht kapselt:
 * <ul>
 *   <li>System-/Automatisierungs-Einstellungen (z.B. Lagerbestandsprüfung, Warnungen, QR-Import)</li>
 *   <li>Darstellungs-Einstellungen (Theme, Farben, Schriftart, Schriftgrößen)</li>
 *   <li>Such-/Filter-Funktion über Einstellungsbereiche</li>
 * </ul>
 *
 * <p>Änderungen werden typischerweise über {@code saveSettings()} persistiert und nach dem Speichern
 * in der Anwendung angewendet.
 */
@SuppressWarnings("deprecation")
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
     * Platzhaltertext für {@link #settingsSearchField}, solange kein echter Suchtext eingegeben wurde.
     */
    private static final String SEARCH_PLACEHOLDER = "Suche nach Einstellungen…";
    /**
     * {@code true}, wenn im Suchfeld aktuell nur der Platzhalter angezeigt wird.
     * Wird u.a. in Focus-Listenern und beim globalen Shortcut verwendet.
     */
    private boolean searchPlaceholderActive = false;
    /**
     * Sammlung von UI-Sektionen, die über die Suche gefiltert/gehervorgehoben werden können.
     */
    private final List<JComponent> searchableSections = new ArrayList<>();
    private final JLabel previewTitleLabel = new JLabel("Vorschau");
    private final JLabel previewBodyLabel = new JLabel("Schrift und Farben werden hier angezeigt.");
    private final JTable previewTable = new JTable(
            new Object[][]{
                    {"A-100", "Beispielartikel", 12, 5},
                    {"B-200", "Musterteil", 4, 2}
            },
            new Object[]{"Artikel", "Name", "Lager", "Min"}
    );
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
    /**
     * Standard-Schriftfamilie, falls keine Einstellung vorhanden ist.
     */
    public static final String DEFAULT_FONT_STYLE = "Dialog";
    private static final String DEFAULT_SERVER_URL = "https://framedev.ch/vebo";

    /**
     * Aktuell verwendete Tabellenschriftgröße (px). Wird u.a. für Vorschau und Tabellen-UI genutzt.
     */
    public static int TABLE_FONT_SIZE = 16;

    /**
     * Schlüssel/Mapping der unterstützten Einstellungsvariablen.
     *
     * <p>Jeder Enum-Wert enthält den Property-Key, der in der Settings-Property-Datei verwendet wird.
     */
    public enum Variable {
        /**
         * Intervall (in Minuten), in dem die Anwendung den Lagerbestand überprüft und Warnungen erstellt, wenn Artikel unter dem Mindestbestand liegen. Beeinflusst, wie häufig die Anwendung den Lagerbestand überprüft und automatisch Warnungen für Artikel erstellt, die unter dem definierten Mindestbestand liegen. Standard ist 30 Minuten, was eine gute Balance zwischen Aktualität und Systemressourcen bietet. Benutzer können hier ein kürzeres Intervall angeben, um schneller auf Lagerbestandsänderungen zu reagieren, oder ein längeres Intervall, um Ressourcen zu schonen.
         */
        STOCK_CHECK_INTERVAL(settings.getProperty("stock_check_interval")),
        /**
         * Ob die automatische Anzeige von ungelesenen Warnungen aktiviert ist. Beeinflusst, ob die Anwendung regelmäßig auf ungelesene Warnungen prüft und diese automatisch anzeigt. Standard ist {@code true}. Wenn aktiviert, prüft die Anwendung alle Stunde (gemäß {@link #WARNING_DISPLAY_INTERVAL}) auf ungelesene Warnungen und zeigt diese prominent an, um sicherzustellen, dass Benutzer wichtige Informationen nicht verpassen. Benutzer können hier entscheiden, ob sie diese proaktive Benachrichtigungsfunktion nutzen möchten oder lieber manuell nach Warnungen suchen wollen.
         */
        ENABLE_WARNING_INTERVAL(settings.getProperty("enable_houtly_warnings")),
        /**
         * Intervall (in Stunden), in dem die Anwendung auf ungelesene Warnungen prüft und diese automatisch anzeigt. Beeinflusst, wie häufig die Anwendung nach ungelesenen Warnungen sucht und diese prominent anzeigt. Standard ist 1 Stunde, was eine gute Balance zwischen Aktualität und Systemressourcen bietet. Benutzer können hier ein kürzeres Intervall angeben, um schneller auf neue Warnungen zu reagieren, oder ein längeres Intervall, um Ressourcen zu schonen.
         */
        WARNING_DISPLAY_INTERVAL(settings.getProperty("warning_display_interval")),
        /**
         * Ob die automatische Lagerbestandsprüfung aktiviert ist. Beeinflusst, ob die Anwendung regelmäßig den Lagerbestand überprüft und Warnungen erstellt, wenn Artikel unter dem Mindestbestand liegen. Standard ist {@code true}. Wenn aktiviert, prüft die Anwendung alle 30 Minuten (gemäß {@link #STOCK_CHECK_INTERVAL}) den Lagerbestand und erstellt automatisch Warnungen für Artikel, die unter dem definierten Mindestbestand liegen. Benutzer können hier entscheiden, ob sie diese proaktive Überprüfungsfunktion nutzen möchten oder lieber manuell den Lagerbestand überwachen wollen.
         */
        ENABLE_AUTO_STOCK_CHECK(settings.getProperty("enable_auto_stock_check")),
        /**
         * Die URL, von der die Anwendung Warnungsdaten (z.B. Lagerwarnungen) abruft. Beeinflusst, wo die Anwendung nach Warnungsinformationen sucht. Standard ist "<a href="https://framedev.ch/vebo/scans.json">...</a>", was auf eine Beispiel-URL verweist. Benutzer können hier eine benutzerdefinierte URL angeben, z.B. die Adresse eines eigenen Servers oder einer API, von der die Anwendung Warnungsdaten abrufen soll. Es ist wichtig, dass die angegebene URL korrekt formatiert ist und auf eine gültige Ressource zeigt, damit die Anwendung Warnungsinformationen erfolgreich abrufen kann.
         */
        SERVER_URL(settings.getProperty("server_url")),
        /**
         * Ob die automatische Anzeige von ungelesenen Warnungen aktiviert ist. Beeinflusst, ob die Anwendung automatisch eine Warnungs-Karte anzeigt, wenn ungelesene Warnungen vorhanden sind. Standard ist {@code true}. Wenn aktiviert, prüft die Anwendung regelmäßig (gemäß {@link #WARNING_DISPLAY_INTERVAL}) auf ungelesene Warnungen und zeigt diese prominent an, um sicherzustellen, dass Benutzer wichtige Informationen nicht verpassen. Benutzer können hier entscheiden, ob sie diese proaktive Benachrichtigungsfunktion nutzen möchten oder lieber manuell nach Warnungen suchen.
         */
        ENABLE_AUTOMATIC_IMPORT_QRCODE(settings.getProperty("enable_automatic_import_qrcode")),
        /**
         * Intervall (in Minuten) für den automatischen Import von QR-Code Scans. Beeinflusst, wie häufig die Anwendung nach neuen QR-Code Scans sucht und diese importiert. Standard ist 10 Minuten, was eine gute Balance zwischen Aktualität und Systemressourcen bietet. Benutzer können hier ein kürzeres Intervall angeben, um schneller auf neue Scans zu reagieren, oder ein längeres Intervall, um Ressourcen zu schonen.
         */
        QRCODE_IMPORT_INTERVAL(settings.getProperty("qrcode_import_interval")),
        /**
         * Ob der Dunkelmodus aktiviert ist. Beeinflusst die Farbpalette der gesamten Anwendung. Standard ist {@code false} (Hellmodus). Wenn aktiviert, verwendet die Anwendung dunkle Hintergründe und helle Schriftfarben, um die Augen zu schonen und die Lesbarkeit in dunklen Umgebungen zu verbessern. Benutzer können hier zwischen Dunkel- und Hellmodus wechseln, je nach ihren Präferenzen und Umgebungslichtbedingungen.
         */
        DARK_MODE(settings.getProperty("dark_mode")),
        /**
         * Die Schriftgröße, die in Tabellen (z.B. Artikelkarte, Warnungskarte) verwendet wird. Beeinflusst die Lesbarkeit der Tabellen und die Gesamtästhetik der Anwendung. Standard ist 16px, was eine gute Balance zwischen Lesbarkeit und Platzbedarf bietet. Benutzer können hier eine größere oder kleinere Schriftgröße angeben, je nach ihren Präferenzen und Bildschirmauflösung.
         */
        TABLE_FONT_SIZE(settings.getProperty("table_font_size")),
        /**
         * Die Schriftgröße, die in Tab-Labels (z.B. im Hauptbereich) verwendet wird. Beeinflusst die Lesbarkeit der Tabs und die Gesamtästhetik der Anwendung. Standard ist 15px, was eine gute Balance zwischen Lesbarkeit und Platzbedarf bietet. Benutzer können hier eine größere oder kleinere Schriftgröße angeben, je nach ihren Präferenzen und Bildschirmauflösung.
         */
        TABLE_FONT_SIZE_TAB(settings.getProperty("table_font_size_tab")),
        /**
         * Die Schriftart, die in der gesamten Anwendung verwendet wird (z.B. für Labels, Buttons, Tabellen). Standard ist "Dialog", was eine plattformunabhängige Schriftfamilie ist, die auf den meisten Systemen verfügbar ist. Benutzer können hier eine andere Schriftart angeben, z.B. "Arial", "Verdana" oder "Times New Roman". Die Anwendung sollte versuchen, die angegebene Schriftart zu laden und verwenden, und falls diese nicht verfügbar ist, auf die Standardschriftart zurückfallen.
         */
        FONT_STYLE(settings.getProperty("font_style")),
        /**
         * Die Akzentfarbe der Anwendung, z.B. für Links, Icons und Hervorhebungen. Wird in der gesamten Anwendung verwendet, z.B. für Link-Farben, Icons in Buttons und Hervorhebungen in Karten-UI.
         */
        THEME_ACCENT_COLOR(settings.getProperty("theme_accent_color")),
        /**
         * Die Farbe des Headers in der Einstellungen-GUI. Wird auch in anderen Bereichen der Anwendung verwendet, z.B. für die Header-Farbe in den Karten-UI (Artikelkarte, Warnungskarte, etc.).
         */
        THEME_HEADER_COLOR(settings.getProperty("theme_header_color")),
        /**
         * Die Farbe von Buttons (z.B. Speichern, Abbrechen) in der Einstellungen-GUI. Wird auch in anderen Bereichen der Anwendung verwendet, z.B. für die "Hinzufügen"-Buttons in der Artikelverwaltung.
         */
        THEME_BUTTON_COLOR(settings.getProperty("theme_button_color"));

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
     * <p>Initialisiert das Frame, baut die UI (Header, Tabs, Bottom-Bar), registriert Shortcuts/
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
    }

    private void initFrame() {
        setTitle("Einstellungen");
        setSize(1200, 850);
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
        JFrameUtils.RoundedPanel headerCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
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
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        contentCard.setLayout(new BorderLayout(0, 10));
        contentCard.setOpaque(false);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Tabs (TOP placement) + Search bar below the tab strip (visual hierarchy)
        JTabbedPane tabbedPane = createTabbedPane();
        JPanel searchPanel = createSearchPanel();

        // Put the tabs above the search (search belongs to the tab contents area)
        JPanel tabsAndSearch = new JPanel();
        tabsAndSearch.setOpaque(false);
        tabsAndSearch.setLayout(new BoxLayout(tabsAndSearch, BoxLayout.Y_AXIS));
        tabbedPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabsAndSearch.add(tabbedPane);
        tabsAndSearch.add(Box.createVerticalStrut(10));
        tabsAndSearch.add(searchPanel);

        contentCard.add(tabsAndSearch, BorderLayout.NORTH);

        // The tabbed pane itself contains the scrollable content; we only need to ensure it expands.
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
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)
                ));
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
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
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
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
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
        return tabbedPane;
    }

    private JPanel createBottomButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 18));
        buttonPanel.setBackground(ThemeManager.getCardBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton resetAllButton = createStyledButton(UnicodeSymbols.REFRESH + " Alles zurücksetzen", new Color(231, 76, 60));
        resetAllButton.addActionListener(e -> resetAllDefaults());

        JButton cancelButton = createStyledButton(UnicodeSymbols.CLOSE + " Abbrechen", new Color(155, 89, 182));
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton(UnicodeSymbols.FLOPPY + " Speichern", new Color(46, 204, 113));
        saveButton.addActionListener(e -> saveSettings());

        buttonPanel.add(resetAllButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        return buttonPanel;
    }

    private void installGlobalShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "focusSearch");
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
        if(tabbedPane == null) return;
        buildSystemAutomaticTab(tabbedPane);
        buildDesignTab(tabbedPane);
        buildConnectionTab(tabbedPane);
        buildDatabaseTab(tabbedPane);
        buildImportExportTab(tabbedPane);
        buildAboutTab(tabbedPane);
    }

    private void buildSystemAutomaticTab(JTabbedPane tabbedPane) {
        // === CATEGORY 1: System & Automatisierung ===
        JPanel systemPanel = createCategoryPanel();
        JScrollPane systemScroll = createScrollablePanel(systemPanel);
        // Stock Check Section
        JPanel stockCheckSection = createSectionPanel(UnicodeSymbols.PACKAGE + " Lagerbestandsprüfung",
                "Automatische Überprüfung des Lagerbestands und Warnerstellung");
        addSectionResetButton(stockCheckSection, this::resetStockCheckDefaults);

        enableAutoStockCheckCheckbox = new JCheckBox("Automatische Lagerbestandsprüfung aktivieren");
        styleCheckbox(enableAutoStockCheckCheckbox);
        enableAutoStockCheckCheckbox.setSelected(true);

        stockCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
        JPanel intervalPanel = createLabeledSpinnerPanel("Prüfintervall:", stockCheckIntervalSpinner, "Minuten", 6);

        stockCheckSection.add(Box.createVerticalStrut(18));
        stockCheckSection.add(enableAutoStockCheckCheckbox);
        stockCheckSection.add(Box.createVerticalStrut(15));
        stockCheckSection.add(intervalPanel);

        systemPanel.add(stockCheckSection);
        systemPanel.add(Box.createVerticalStrut(25));

        // === SECTION 2: Warnungen ===
        JPanel warningSection = createSectionPanel(UnicodeSymbols.WARNING + " Warnungsanzeige",
                "Konfiguration der automatischen Warnanzeige");
        addSectionResetButton(warningSection, this::resetWarningDefaults);

        enableWarningDisplayCheckbox = new JCheckBox("Automatische Anzeige ungelesener Warnungen aktivieren");
        styleCheckbox(enableWarningDisplayCheckbox);
        enableWarningDisplayCheckbox.setSelected(true);

        warningDisplayIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        JPanel warningIntervalPanel = createLabeledSpinnerPanel("Anzeigeintervall:", warningDisplayIntervalSpinner, "Stunde(n)", 6);

        JLabel warningInfoLabel = getWarningInfoLabel();

        warningSection.add(Box.createVerticalStrut(18));
        warningSection.add(enableWarningDisplayCheckbox);
        warningSection.add(Box.createVerticalStrut(15));
        warningSection.add(warningIntervalPanel);
        warningSection.add(Box.createVerticalStrut(10));
        warningSection.add(warningInfoLabel);

        systemPanel.add(warningSection);

        systemPanel.add(Box.createVerticalStrut(25));

        // QR-Code Section
        JPanel qrCodeOptionsPanel = createSectionPanel(UnicodeSymbols.PHONE + " QR-Code Import",
                "Automatischer Import von QR-Code Scans");
        addSectionResetButton(qrCodeOptionsPanel, this::resetQrCodeDefaults);

        automaticImportCheckBox = new JCheckBox("Automatisches Importieren von QR-Codes aktivieren");
        styleCheckbox(automaticImportCheckBox);
        automaticImportCheckBox.setSelected(false);

        qrCodeOptionsPanel.add(Box.createVerticalStrut(18));
        qrCodeOptionsPanel.add(automaticImportCheckBox);
        qrCodeOptionsPanel.add(Box.createVerticalStrut(15));

        qrCodeImportIntervalSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 60, 5));
        JPanel qrIntervalPanel = createLabeledSpinnerPanel("Import-Intervall:", qrCodeImportIntervalSpinner, "Minuten", 6);

        qrCodeOptionsPanel.add(qrIntervalPanel);

        systemPanel.add(qrCodeOptionsPanel);
        systemPanel.add(Box.createVerticalStrut(25));

        JPanel categoryPanel = createSectionPanel(UnicodeSymbols.FOLDER + " Kategorien", "Einstellungen zu verschiedenen Kategorien");

        JLabel categoryInfoLabel = createInfoLabel(
                "Hier können Sie Kategorien hinzufügen oder die Datei direkt bearbeiten. " +
                        "Optional können Sie einen Nummernbereich (Von/Bis) angeben."
        );

        // Top actions row
        JPanel categoryActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        categoryActions.setOpaque(false);
        categoryActions.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton categorySettingsFileButton = createStyledButton(
                UnicodeSymbols.FOLDER + " Kategorien-Datei öffnen",
                new Color(52, 152, 219)
        );
        categorySettingsFileButton.setToolTipText("Öffnet categories.json im Standard-Editor");
        categorySettingsFileButton.addActionListener(e -> openCategorySettingsFile());

        JButton reloadCategoriesButton = createStyledButton(
                UnicodeSymbols.REFRESH + " Kategorien neu laden",
                new Color(155, 89, 182)
        );
        reloadCategoriesButton.setToolTipText("Lädt categories.json neu ein (ohne Neustart)");
        reloadCategoriesButton.addActionListener(e -> {
            loadCategories();
            JOptionPane.showMessageDialog(this,
                    "Kategorien wurden neu geladen.",
                    "Kategorien",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        });

        categoryActions.add(categorySettingsFileButton);
        categoryActions.add(reloadCategoriesButton);

        categoryPanel.add(categoryInfoLabel);
        categoryPanel.add(Box.createVerticalStrut(10));
        categoryPanel.add(categoryActions);
        categoryPanel.add(Box.createVerticalStrut(14));

        JSeparator catSep = new JSeparator();
        catSep.setForeground(ThemeManager.getBorderColor());
        catSep.setAlignmentX(Component.LEFT_ALIGNMENT);
        categoryPanel.add(catSep);
        categoryPanel.add(Box.createVerticalStrut(14));

        // ---- Add Category Form (improved layout) ---------------------------------

        JLabel categoryFormTitle = new JLabel("Neue Kategorie hinzufügen");
        categoryFormTitle.setFont(getFontByName(Font.BOLD, 14));
        categoryFormTitle.setForeground(ThemeManager.getTextPrimaryColor());
        categoryFormTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel categoryFormHint = new JLabel(
                "<html><div style='padding-top:2px;'>" +
                        "<span style='font-size:11px;'>Tipp: Drücken Sie Enter im Namen-Feld, um hinzuzufügen. Z.b von 1000 bis 1999</span>" +
                        "</div></html>"
        );
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
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JFormattedTextField toRange = new JFormattedTextField(intFormatter);
        toRange.setColumns(6);
        toRange.setToolTipText("Bis (z.B. 1999)");
        toRange.setFont(getFontByName(Font.PLAIN, 13));
        toRange.setBackground(ThemeManager.getInputBackgroundColor());
        toRange.setForeground(ThemeManager.getTextPrimaryColor());
        toRange.setCaretColor(ThemeManager.getTextPrimaryColor());
        toRange.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton addCategoryButton = createStyledButton(
                UnicodeSymbols.PLUS + " Hinzufügen",
                new Color(46, 204, 113)
        );
        addCategoryButton.setToolTipText("Fügt die Kategorie hinzu");
        addCategoryButton.setEnabled(false);

        Runnable doAddCategory = () -> {
            String newCategory = categoryNameField.getText() == null ? "" : categoryNameField.getText().trim();
            String fromTxt = fromRange.getText() == null ? "" : fromRange.getText().trim();
            String toTxt = toRange.getText() == null ? "" : toRange.getText().trim();

            if (newCategory.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte einen Kategorienamen eingeben.",
                        "Hinweis",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                categoryNameField.requestFocusInWindow();
                return;
            }

            // Only validate if either range is present
            if (!fromTxt.isEmpty() || !toTxt.isEmpty()) {
                try {
                    int f = fromTxt.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(fromTxt);
                    int t = toTxt.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(toTxt);
                    if (f > t) throw new NumberFormatException();
                    addNewCategory(newCategory, f, t);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Ungültiger Bereich (Von/Bis). Bitte ganze Zahlen eingeben und Von ≤ Bis.",
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                    return;
                }
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

        // Layout panel
        JPanel addCategoryPanel = new JPanel(new GridBagLayout());
        addCategoryPanel.setOpaque(false);
        addCategoryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;

        // Row 0: Title + hint (full width)
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 4, 0);
        addCategoryPanel.add(categoryFormTitle, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        addCategoryPanel.add(categoryFormHint, gbc);

        // Row 2: Labels
        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 12);

        JLabel categoryNameLabel = new JLabel("Kategoriename");
        categoryNameLabel.setFont(getFontByName(Font.BOLD, 13));
        categoryNameLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel fromLabel = new JLabel("Von");
        fromLabel.setFont(getFontByName(Font.PLAIN, 11));
        fromLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JLabel toLabel = new JLabel("Bis");
        toLabel.setFont(getFontByName(Font.PLAIN, 11));
        toLabel.setForeground(ThemeManager.getTextSecondaryColor());

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        addCategoryPanel.add(categoryNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        addCategoryPanel.add(fromLabel, gbc);

        gbc.gridx = 2;
        addCategoryPanel.add(toLabel, gbc);

        // Row 3: Fields + button
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 0, 12);

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        addCategoryPanel.add(categoryNameField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        fromRange.setPreferredSize(new Dimension(120, 36));
        addCategoryPanel.add(fromRange, gbc);

        gbc.gridx = 2;
        toRange.setPreferredSize(new Dimension(120, 36));
        addCategoryPanel.add(toRange, gbc);

        gbc.gridx = 3;
        gbc.insets = new Insets(0, 0, 0, 0);
        addCategoryButton.setPreferredSize(new Dimension(160, 36));
        addCategoryPanel.add(addCategoryButton, gbc);

        categoryPanel.add(addCategoryPanel);

        // finally add to system panel
        systemPanel.add(categoryPanel);
        systemPanel.add(Box.createVerticalGlue());
        systemPanel.add(Box.createVerticalStrut(25));
        JPanel otherSection = createSectionPanel("Sonstiges", "Allgemeine Einstellungen");
        otherSection.add(Box.createVerticalStrut(18));
        JLabel openSettingsLabel = createInfoLabel("Öffnet den Ordner, in dem die Einstellungsdateien gespeichert sind.");
        otherSection.add(openSettingsLabel);
        otherSection.add(Box.createVerticalStrut(10));
        JButton openSettingsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Einstellungen-Ordner öffnen", new Color(52, 152, 219));
        openSettingsFolderButton.addActionListener(e -> openSettingsFolder());
        otherSection.add(openSettingsFolderButton);
        JLabel logsLabel = createInfoLabel("Öffnet den Ordner, in dem die Anwendungsprotokolle gespeichert sind.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsLabel);
        otherSection.add(Box.createVerticalStrut(10));
        JButton openLogsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Protokolle-Ordner öffnen", new Color(52, 152, 219));
        openLogsFolderButton.addActionListener(e -> openLogsFolder());
        otherSection.add(openLogsFolderButton);
        otherSection.add(Box.createVerticalStrut(15));
        JLabel logsDelete = createInfoLabel("Löscht alle Anwendungsprotokolle aus dem Protokolle-Ordner. So wie in der Datenbank.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsDelete);
        JButton deleteLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alle Protokolle löschen", new Color(220, 53, 69));
        deleteLogsButton.addActionListener(e -> deleteAllLogs());
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteLogsButton);
        otherSection.add(Box.createVerticalStrut(15));
        JLabel logsDeleteTime = createInfoLabel("Löscht alle Anwendungsprotokolle, die älter als 30 Tage sind, aus dem Protokolle-Ordner. So wie in der Datenbank.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsDeleteTime);
        JCheckBox deleteOldLogsCheckBox = new JCheckBox("Alte Protokolle (älter als 30 Tage) löschen");
        styleCheckbox(deleteOldLogsCheckBox);
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteOldLogsCheckBox);
        JButton deleteOldLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alte Protokolle löschen", new Color(220, 53, 69));
        deleteOldLogsButton.addActionListener(e -> deleteOldLogs());
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteOldLogsButton);
        systemPanel.add(otherSection);
        // Add system panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.WRENCH + " System", systemScroll);
    }

    private void buildDesignTab(JTabbedPane tabbedPane) {
        // === CATEGORY 1b: Darstellung ===
        // One-page layout (no right-side sticky panel). Everything scrolls together.
        JPanel appearancePanel = createCategoryPanel();
        JScrollPane appearanceScroll = createScrollablePanel(appearancePanel);
        JLabel appearanceIntro = new JLabel(
                "<html><div style='padding:2px 0;'><b>Darstellung</b><br/>" +
                        "<span style='font-size:11px;'>Änderungen werden nach dem Speichern übernommen. " +
                        "Die Vorschau aktualisiert sich automatisch.</span></div></html>"
        );
        appearanceIntro.setFont(getFontByName(Font.PLAIN, 12));
        appearanceIntro.setForeground(ThemeManager.getTextSecondaryColor());
        appearanceIntro.setAlignmentX(Component.LEFT_ALIGNMENT);
        // ---- Schriftart --------------------------------------------------------
        JPanel fontSettingsPanel = createSectionPanel(UnicodeSymbols.ABC + " Schriftart",
                "Schriftart für die Anwendung festlegen");
        addSectionResetButton(fontSettingsPanel, this::resetFontDefaults);
        fontSettingsPanel.add(Box.createVerticalStrut(12));
        fontSettingsPanel.add(appearanceIntro);
        fontSettingsPanel.add(Box.createVerticalStrut(12));
        JLabel fontInfoLabel = new JLabel(
                "<html><div><span style='font-size: 11px;'>" +
                        "Wählen Sie eine Schriftart. Die Vorschau unten zeigt das Ergebnis.</span></div></html>"
        );
        fontInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        fontInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        fontInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSettingsPanel.add(fontInfoLabel);
        fontSettingsPanel.add(Box.createVerticalStrut(10));
        fontComboBox = new JComboBox<>();
        getAllFonts().forEach(fontComboBox::addItem);
        styleComboBox(fontComboBox);
        fontComboBox.addActionListener(e -> updatePreview());
        JPanel fontSelectionPanel = createLabeledComboBoxPanel("Schriftart auswählen:", fontComboBox);
        fontSettingsPanel.add(fontSelectionPanel);
        fontSample.setFont(getFontByName(Font.PLAIN, 16));
        fontSample.setForeground(ThemeManager.getTextPrimaryColor());
        fontSample.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSettingsPanel.add(Box.createVerticalStrut(12));
        fontSettingsPanel.add(fontSample);
        // ---- Tabellen & Tabs ---------------------------------------------------
        JPanel tableSettingsPanel = createSectionPanel(UnicodeSymbols.CLIPBOARD + " Tabellen & Tabs",
                "Schriftgröße für Tabellen und Tabs anpassen");
        addSectionResetButton(tableSettingsPanel, this::resetTableDefaults);
        fontSizeTableSpinner = new JSpinner(new SpinnerNumberModel(16, 10, 44, 1));
        JPanel fontSizePanel = createLabeledSpinnerPanel("Tabellen-Schriftgröße:", fontSizeTableSpinner, "px", 4);
        tableSettingsPanel.add(Box.createVerticalStrut(12));
        tableSettingsPanel.add(fontSizePanel);
        tableSettingsPanel.add(Box.createVerticalStrut(8));
        configureSlider(tableFontSlider, fontSizeTableSpinner, tableFontSample);
        tableSettingsPanel.add(tableFontSlider);
        tableSettingsPanel.add(Box.createVerticalStrut(6));
        tableSettingsPanel.add(tableFontSample);
        fontSizeTabSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 40, 1));
        JPanel fontTabSizePanel = createLabeledSpinnerPanel("Tabs-Schriftgröße:", fontSizeTabSpinner, "px", 4);
        tableSettingsPanel.add(Box.createVerticalStrut(14));
        tableSettingsPanel.add(fontTabSizePanel);
        tableSettingsPanel.add(Box.createVerticalStrut(8));
        configureSlider(tabFontSlider, fontSizeTabSpinner, tabFontSample);
        tableSettingsPanel.add(tabFontSlider);
        tableSettingsPanel.add(Box.createVerticalStrut(6));
        tableSettingsPanel.add(tabFontSample);
        // Keep preview in sync even if user types into spinners
        fontSizeTableSpinner.addChangeListener(e -> updatePreview());
        fontSizeTabSpinner.addChangeListener(e -> updatePreview());
        // ---- Design / Theme ----------------------------------------------------
        JPanel themeSection = createSectionPanel(UnicodeSymbols.COLOR_PALETTE + " Design & Darstellung",
                "Passen Sie das Erscheinungsbild der Anwendung an");
        addSectionResetButton(themeSection, this::resetThemeDefaults);
        darkModeCheckbox = new JCheckBox("Dark Mode aktivieren");
        styleCheckbox(darkModeCheckbox);
        darkModeCheckbox.setSelected(ThemeManager.isDarkMode());
        themeSection.add(Box.createVerticalStrut(16));
        themeSection.add(darkModeCheckbox);
        themeSection.add(Box.createVerticalStrut(12));
        String[] themes = {"Light", "Dark"};
        themeComboBox = new JComboBox<>(themes);
        styleComboBox(themeComboBox);
        themeComboBox.setSelectedItem(ThemeManager.isDarkMode() ? "Dark" : "Light");
        JPanel themeSelectionPanel = createLabeledComboBoxPanel("Design-Schema:", themeComboBox);
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
        themeSection.add(themeSelectionPanel);
        themeSection.add(Box.createVerticalStrut(12));
        JLabel themeInfoLabel = new JLabel("<html><div style='padding: 6px 0;'>" +
                "<i>Hinweis: Nach dem Speichern werden alle Fenster mit dem neuen Design aktualisiert.<br/>" +
                "Der Dark Mode schont die Augen bei Arbeiten in dunkler Umgebung.</i>" +
                "</div></html>");
        themeInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        themeInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        themeInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeSection.add(themeInfoLabel);
        // ---- Farben ------------------------------------------------------------
        JPanel colorSection = createSectionPanel(UnicodeSymbols.COLOR_PALETTE + " Farbthemen",
                "Akzent-, Header- und Buttonfarbe anpassen");
        addSectionResetButton(colorSection, this::resetColorDefaults);
        colorSection.add(Box.createVerticalStrut(12));
        colorSection.add(createColorRow("Akzentfarbe:", "theme_accent_color"));
        colorSection.add(Box.createVerticalStrut(10));
        colorSection.add(createColorRow("Headerfarbe:", "theme_header_color"));
        colorSection.add(Box.createVerticalStrut(10));
        colorSection.add(createColorRow("Buttonfarbe:", "theme_button_color"));
        // ---- Vorschau ----------------------------------------------------------
        JPanel previewSection = createSectionPanel(UnicodeSymbols.BULB + " Live Vorschau",
                "Vorschau auf Schrift und Farben basierend auf Ihren Einstellungen");
        previewSection.add(Box.createVerticalStrut(12));
        previewSection.add(createPreviewPanel());
        // ---- Assemble ----------------------------------------------------------
        appearancePanel.add(fontSettingsPanel);
        appearancePanel.add(Box.createVerticalStrut(22));
        appearancePanel.add(tableSettingsPanel);
        appearancePanel.add(Box.createVerticalStrut(22));
        appearancePanel.add(themeSection);
        appearancePanel.add(Box.createVerticalStrut(22));
        appearancePanel.add(colorSection);
        appearancePanel.add(Box.createVerticalStrut(22));
        appearancePanel.add(previewSection);
        appearancePanel.add(Box.createVerticalGlue());
        // Add appearance panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.COLOR_PALETTE + " Darstellung", appearanceScroll);
    }

    private void buildConnectionTab(JTabbedPane tabbedPane) {
        // === CATEGORY 2: Verbindung ===
        JPanel connectionPanel = createCategoryPanel();
        JScrollPane connectionScroll = createScrollablePanel(connectionPanel);
        // Server-Einstellungen
        JPanel serverSection = createSectionPanel(UnicodeSymbols.GLOBE + " Server-Verbindung",
                "URL des QR-Code Scan-Servers");
        addSectionResetButton(serverSection, this::resetServerDefaults);
        JLabel serverHint = createInfoLabel(
                "Tipp: Nutzen Sie https (wenn möglich). Mit den Aktionen können Sie die URL testen oder kopieren."
        );
        serverHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        // Actions row
        JPanel serverActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        serverActions.setOpaque(false);
        serverActions.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton openUrlButton = createStyledButton(UnicodeSymbols.GLOBE + " Öffnen", new Color(52, 152, 219));
        openUrlButton.setToolTipText("Öffnet die URL im Browser");
        JButton copyUrlButton = createStyledButton(UnicodeSymbols.CLIPBOARD + " Kopieren", new Color(155, 89, 182));
        copyUrlButton.setToolTipText("Kopiert die URL in die Zwischenablage");
        JButton testUrlButton = createStyledButton(UnicodeSymbols.BULB + " Prüfen", new Color(46, 204, 113));
        testUrlButton.setToolTipText("Prüft, ob die URL syntaktisch gültig ist");
        serverActions.add(openUrlButton);
        serverActions.add(copyUrlButton);
        serverActions.add(testUrlButton);
        // URL field
        JPanel serverUrlPanel = new JPanel(new BorderLayout(0, 10));
        serverUrlPanel.setOpaque(false);
        serverUrlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        serverUrlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel serverUrlLabel = new JLabel("Server URL:");
        serverUrlLabel.setFont(getFontByName(Font.BOLD, 13));
        serverUrlLabel.setForeground(ThemeManager.getTextPrimaryColor());
        serverUrlField = new JTextField(DEFAULT_SERVER_URL);
        styleInputField(serverUrlField);
        serverUrlField.setToolTipText("z.B. https://example.com/scans.json");
        serverUrlField.setPreferredSize(new Dimension(600, 40));
        serverUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        // Small validation badge
        JLabel urlStatusLabel = new JLabel(" ");
        urlStatusLabel.setFont(getFontByName(Font.PLAIN, 12));
        urlStatusLabel.setForeground(ThemeManager.getTextSecondaryColor());
        urlStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        Runnable validateUrl = () -> {
            String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
            if (raw.isEmpty()) {
                urlStatusLabel.setText("Bitte eine URL eingeben.");
                urlStatusLabel.setForeground(new Color(231, 76, 60));
                return;
            }
            try {
                URI uri = new URI(raw);
                String scheme = uri.getScheme();
                if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                    urlStatusLabel.setText("Ungültiges Schema (nur http/https).");
                    urlStatusLabel.setForeground(new Color(231, 76, 60));
                    return;
                }
                if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                    urlStatusLabel.setText("Ungültige URL (Host fehlt).");
                    urlStatusLabel.setForeground(new Color(231, 76, 60));
                    return;
                }
                urlStatusLabel.setText("URL sieht gültig aus.");
                urlStatusLabel.setForeground(new Color(46, 204, 113));
            } catch (Exception ex) {
                urlStatusLabel.setText("Ungültige URL (Syntaxfehler).");
                urlStatusLabel.setForeground(new Color(231, 76, 60));
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
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Öffnen der URL:\n" + ex.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
            }
        });
        copyUrlButton.addActionListener(e -> {
            String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(raw), null);
            JOptionPane.showMessageDialog(this,
                    "URL wurde kopiert.",
                    "Zwischenablage",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
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
        serverUrlPanel.add(serverUrlLabel, BorderLayout.NORTH);
        serverUrlPanel.add(serverUrlField, BorderLayout.CENTER);
        serverSection.add(Box.createVerticalStrut(14));
        serverSection.add(serverHint);
        serverSection.add(Box.createVerticalStrut(10));
        serverSection.add(serverActions);
        serverSection.add(Box.createVerticalStrut(14));
        serverSection.add(serverUrlPanel);
        serverSection.add(Box.createVerticalStrut(8));
        serverSection.add(urlStatusLabel);
        // initial status
        validateUrl.run();
        connectionPanel.add(serverSection);
        connectionPanel.add(Box.createVerticalGlue());
        // Add connection panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.GLOBE + " Verbindung", connectionScroll);
    }

    private void buildDatabaseTab(JTabbedPane tabbedPane) {
        // === CATEGORY 3: Datenbank ===
        JPanel databasePanel = createCategoryPanel();
        JScrollPane databaseScroll = createScrollablePanel(databasePanel);
        // Datenbank-Bereinigung Section
        JPanel databaseSection = createSectionPanel(UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung",
                "Datenbank bereinigen und Tabellen verwalten");
        JLabel databaseClearLabel = getDatabaseClearLabel();
        JPanel databaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        databaseButtonPanel.setOpaque(false);
        databaseButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        databaseButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        JButton clearDatabaseButton = createStyledButton(UnicodeSymbols.TRASH + " Datenbank Bereinigen", new Color(220, 53, 69));
        clearDatabaseButton.addActionListener(e -> clearDatabase());
        databaseButtonPanel.add(clearDatabaseButton);
        databaseSection.add(Box.createVerticalStrut(18));
        databaseSection.add(databaseClearLabel);
        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(databaseButtonPanel);
        JLabel selectedDatabaseClear = getSelectedDatabaseClear();
        databaseSection.add(Box.createVerticalStrut(10));
        databaseSection.add(selectedDatabaseClear);
        List<String> tableNames = new ArrayList<>(DatabaseManager.ALLOWED_TABLES);
        JComboBox<String> tableComboBox = new JComboBox<>(tableNames.toArray(new String[0]));
        styleComboBox(tableComboBox);
        JPanel tableSelectionPanel = createLabeledComboBoxPanel("Tabelle auswählen:", tableComboBox);
        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(tableSelectionPanel);
        JButton deleteSelectedTableButton = createStyledButton(UnicodeSymbols.TRASH + " Ausgewählte Tabelle Löschen", new Color(230, 126, 34));
        deleteSelectedTableButton.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable == null || selectedTable.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Bitte wählen Sie eine Tabelle aus.",
                        "Keine Tabelle ausgewählt",
                        JOptionPane.WARNING_MESSAGE);
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
                String userInput = JOptionPane.showInputDialog(this,
                        String.format("<html>Bitte geben Sie den Tabellennamen '<b>%s</b>' ein,<br/>" +
                                "um die Löschung zu bestätigen:</html>", selectedTable),
                        "Löschung bestätigen",
                        JOptionPane.WARNING_MESSAGE);
                if (userInput != null && userInput.trim().equals(selectedTable)) {
                    deleteTable(selectedTable);
                } else if (userInput != null) {
                    JOptionPane.showMessageDialog(this,
                            "Der eingegebene Tabellenname stimmt nicht überein.\nLöschung abgebrochen.",
                            "Abgebrochen",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        JPanel deleteTableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deleteTableButtonPanel.setOpaque(false);
        deleteTableButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteTableButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        deleteTableButtonPanel.add(deleteSelectedTableButton);
        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(deleteTableButtonPanel);
        databasePanel.add(databaseSection);
        databasePanel.add(Box.createVerticalGlue());
        // Add database panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.FLOPPY + " Datenbank", databaseScroll);
    }

    private void buildImportExportTab(JTabbedPane tabbedPane) {
        // === CATEGORY 4: Import & Export ===
        JPanel importExportPanel = createCategoryPanel();
        JScrollPane importExportScroll = createScrollablePanel(importExportPanel);
        JPanel importExportSection = createSectionPanel(UnicodeSymbols.ARROW_UP + UnicodeSymbols.ARROW_DOWN + "️ Import & Export",
                "Datenimport und -export Funktionen");
        importExportSection.add(Box.createVerticalStrut(15));
        importExportSection.add(createStyledLabel("Import:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        importExportSection.add(Box.createVerticalStrut(5));
        importExportSection.add(createInfoLabel("<html><div style='width:650px;'>" +
                "Importieren Sie Artikel, Lieferanten oder andere Daten aus CSV-Dateien.<br/>" +
                "Stellen Sie sicher, dass die CSV-Dateien das richtige Format haben.</div></html>"));
        importExportSection.add(Box.createVerticalStrut(10));
        JButton importButton = createStyledButton(UnicodeSymbols.ARROW_UP + " Datenbank Importieren", new Color(39, 174, 96));
        importButton.addActionListener(e -> importFromCsv());
        importExportSection.add(importButton);
        importExportSection.add(Box.createVerticalStrut(20));
        importExportSection.add(Box.createVerticalStrut(15));
        importExportSection.add(createStyledLabel("Export:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        importExportSection.add(Box.createVerticalStrut(5));
        importExportSection.add(createInfoLabel("<html><div style='width:650px;'>" +
                "Exportieren Sie Ihre Datenbankinhalte in CSV-Dateien zur Sicherung oder Weiterverarbeitung.</div></html>"));
        importExportSection.add(Box.createVerticalStrut(10));
        JButton exportButton = createStyledButton(UnicodeSymbols.ARROW_DOWN + " Datenbank Exportieren", new Color(52, 152, 219));
        exportButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "<html><b>Datenbank nach CSV exportieren?</b><br/><br/>" +
                            "Dies erstellt CSV-Dateien für:<br/>" +
                            "- Artikel (articles_export.csv)<br/>" +
                            "- Lieferanten (vendors_export.csv)<br/>" +
                            "- Kunden (clients_export.csv)<br/>" +
                            "- Bestellungen (orders_export.csv)<br/><br/>" +
                            "Speicherort: " + Main.getAppDataDir().getAbsolutePath() + "</html>",
                    "Exportieren bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    exportToCsv();
                    JOptionPane.showMessageDialog(this,
                            "<html><b>OK Export erfolgreich!</b><br/><br/>" +
                                    "Alle Tabellen wurden erfolgreich exportiert.<br/>" +
                                    "Speicherort: <br/>" +
                                    Main.getAppDataDir().getAbsolutePath() + "</html>",
                            "Export erfolgreich",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "<html><b>X Fehler beim Export!</b><br/><br/>" +
                                    ex.getMessage() + "</html>",
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });
        importExportSection.add(exportButton);
        importExportPanel.add(importExportSection);
        importExportPanel.add(Box.createVerticalStrut(25));
        JPanel settingsProfileSection = createSectionPanel(UnicodeSymbols.FLOPPY + " Einstellungen Profil",
                "Importieren oder exportieren Sie Ihre Einstellungen als Profil-Datei");
        settingsProfileSection.add(Box.createVerticalStrut(15));
        JButton exportSettingsButton = createStyledButton(UnicodeSymbols.DOWNLOAD + " Einstellungen exportieren", new Color(52, 152, 219));
        exportSettingsButton.addActionListener(e -> exportSettingsProfile());
        JButton importSettingsButton = createStyledButton(UnicodeSymbols.UPLOAD + " Einstellungen importieren", new Color(39, 174, 96));
        importSettingsButton.addActionListener(e -> importSettingsProfile());
        settingsProfileSection.add(exportSettingsButton);
        settingsProfileSection.add(Box.createVerticalStrut(10));
        settingsProfileSection.add(importSettingsButton);
        importExportPanel.add(settingsProfileSection);
        importExportPanel.add(Box.createVerticalGlue());
        // Add import/export panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.DOWNLOAD + " Import/Export", importExportScroll);
    }

    private void buildAboutTab(JTabbedPane tabbedPane) {
        // === CATEGORY 5: About ===
        JPanel aboutPanel = createCategoryPanel();
        JScrollPane aboutScroll = createScrollablePanel(aboutPanel);
        JPanel aboutSection = createSectionPanel(UnicodeSymbols.INFO + " Über VEBO Lagersystem",
                "Informationen über die Anwendung");
        // Application Info
        JPanel appInfoPanel = new JPanel();
        appInfoPanel.setLayout(new BoxLayout(appInfoPanel, BoxLayout.Y_AXIS));
        appInfoPanel.setOpaque(false);
        appInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appInfoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        // Logo/Title
        JLabel appNameLabel = new JLabel("VEBO Lagersystem");
        appNameLabel.setFont(getFontByName(Font.BOLD, 28));
        appNameLabel.setForeground(ThemeManager.getTitleTextColor());
        appNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
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
                appNameLabel.setForeground(ThemeManager.getTitleTextHighlightColor());
                appNameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                appNameLabel.setForeground(ThemeManager.getTitleTextColor());
                appNameLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        appNameLabel.setToolTipText("Besuchen Sie unsere Website: https://vebo.ch");
        JLabel versionLabel = new JLabel("Version " + Main.VERSION);
        versionLabel.setFont(getFontByName(Font.PLAIN, 14));
        versionLabel.setForeground(ThemeManager.getTextSecondaryColor());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appInfoPanel.add(appNameLabel);
        appInfoPanel.add(Box.createVerticalStrut(5));
        appInfoPanel.add(versionLabel);
        aboutSection.add(Box.createVerticalStrut(18));
        aboutSection.add(appInfoPanel);
        // Description
        JPanel descriptionPanel = new JPanel(new BorderLayout(0, 10));
        descriptionPanel.setOpaque(false);
        descriptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        descriptionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descTitle = new JLabel("Beschreibung:");
        descTitle.setFont(getFontByName(Font.PLAIN, 14));
        descTitle.setForeground(ThemeManager.getTextPrimaryColor());
        JTextArea descriptionArea = new JTextArea(
                "VEBO Lagersystem ist eine moderne Lagerverwaltungssoftware für die effiziente " +
                        "Verwaltung von Artikeln, Bestellungen, Lieferanten und Kunden. Die Anwendung " +
                        "bietet umfangreiche Funktionen für die Bestandsverwaltung, automatische Warnungen " +
                        "bei niedrigem Lagerbestand und QR-Code-basierte Artikelerfassung."
        );
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFont(getFontByName(Font.PLAIN, 13));
        descriptionArea.setForeground(ThemeManager.getTextPrimaryColor());
        descriptionArea.setBackground(ThemeManager.getInputBackgroundColor());
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        descriptionArea.setRows(4);
        descriptionPanel.add(descTitle, BorderLayout.NORTH);
        descriptionPanel.add(descriptionArea, BorderLayout.CENTER);
        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(descriptionPanel);
        // Developer Info
        JPanel developerPanel = new JPanel();
        developerPanel.setLayout(new BoxLayout(developerPanel, BoxLayout.Y_AXIS));
        developerPanel.setOpaque(false);
        developerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        developerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        developerPanel.add(createStyledLabel(UnicodeSymbols.DEVELOPER + " Entwickler:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        developerPanel.add(Box.createVerticalStrut(8));
        developerPanel.add(createStyledLabel("Darryl Huber", 13, Font.PLAIN, ThemeManager.getTextPrimaryColor()));
        developerPanel.add(Box.createVerticalStrut(5));
        developerPanel.add(createStyledLabel("Organisation: VEBO Oensingen", 13, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(developerPanel);
        // System Info
        JPanel systemInfoPanel = new JPanel();
        systemInfoPanel.setLayout(new BoxLayout(systemInfoPanel, BoxLayout.Y_AXIS));
        systemInfoPanel.setOpaque(false);
        systemInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        systemInfoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        systemInfoPanel.add(createStyledLabel(UnicodeSymbols.DEVELOPER + " System-Information:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(8));
        systemInfoPanel.add(createStyledLabel("Java Version: " + System.getProperty("java.version"), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(createStyledLabel("Betriebssystem: " + System.getProperty("os.name") + " " + System.getProperty("os.version"), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(createStyledLabel("Datenverzeichnis: " + Main.getAppDataDir().getAbsolutePath(), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(systemInfoPanel);
        // Update Manager Section
        JPanel updatePanel = new JPanel();
        updatePanel.setLayout(new BoxLayout(updatePanel, BoxLayout.Y_AXIS));
        updatePanel.setOpaque(false);
        updatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updatePanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        updatePanel.add(createStyledLabel(UnicodeSymbols.DOWNLOAD + " Update-Verwaltung:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        updatePanel.add(Box.createVerticalStrut(12));
        // Create update button rows with better layout
        JPanel updateRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        updateRow1.setOpaque(false);
        updateRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        updateRow1.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton checkStableUpdateBtn = createStyledButton("Stable-Updates", new Color(52, 152, 219));
        checkStableUpdateBtn.setToolTipText("Prüft auf neue stabile Versionen");
        checkStableUpdateBtn.addActionListener(e -> checkForUpdates("stable"));
        checkStableUpdateBtn.setPreferredSize(new Dimension(180, 40));
        JButton checkBetaUpdateBtn = createStyledButton("Beta-Updates", new Color(243, 156, 18));
        checkBetaUpdateBtn.setToolTipText("Prüft auf neue Beta-Versionen");
        checkBetaUpdateBtn.addActionListener(e -> checkForUpdates("beta"));
        checkBetaUpdateBtn.setPreferredSize(new Dimension(180, 40));
        updateRow1.add(checkStableUpdateBtn);
        updateRow1.add(checkBetaUpdateBtn);
        updatePanel.add(updateRow1);
        updatePanel.add(Box.createVerticalStrut(8));
        JPanel updateRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        updateRow2.setOpaque(false);
        updateRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        updateRow2.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton checkAlphaUpdateBtn = createStyledButton("Alpha-Updates", new Color(231, 76, 60));
        checkAlphaUpdateBtn.setToolTipText("Prüft auf neue Alpha-Versionen (experimentell)");
        checkAlphaUpdateBtn.addActionListener(e -> checkForUpdates("alpha"));
        checkAlphaUpdateBtn.setPreferredSize(new Dimension(180, 40));
        JButton checkTestingUpdateBtn = createStyledButton("Testing-Updates", new Color(155, 89, 182));
        checkTestingUpdateBtn.setToolTipText("Prüft auf neue Testing-Versionen (Entwicklung)");
        checkTestingUpdateBtn.addActionListener(e -> checkForUpdates("testing"));
        checkTestingUpdateBtn.setPreferredSize(new Dimension(180, 40));
        updateRow2.add(checkAlphaUpdateBtn);
        updateRow2.add(checkTestingUpdateBtn);
        updatePanel.add(updateRow2);
        updatePanel.add(Box.createVerticalStrut(12));
        // Add info text about version
        JLabel versionInfoLabel = new JLabel("<html><div style='padding: 8px; background: " + toHexColor(ThemeManager.getInputBackgroundColor()) + "; border-radius: 6px;'>" +
                "<b>Aktuelle Version:</b> " + Main.VERSION + "<br/>" +
                "<span style='font-size: 11px; color: #666;'>Kanal: " + UpdateManager.detectChannel(Main.VERSION) + "</span>" +
                "</div></html>");
        versionInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        versionInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updatePanel.add(versionInfoLabel);
        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(updatePanel);
        // Copyright
        JPanel copyrightPanel = new JPanel();
        copyrightPanel.setLayout(new BoxLayout(copyrightPanel, BoxLayout.Y_AXIS));
        copyrightPanel.setOpaque(false);
        copyrightPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyrightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(15, 0, 0, 0)
        ));
        copyrightPanel.add(createStyledLabel("© 2026 VEBO Oensingen. Alle Rechte vorbehalten.", 11, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        copyrightPanel.add(Box.createVerticalStrut(5));
        JLabel licenseLabel = createStyledLabel("Diese Software wird bereitgestellt \"wie sie ist\", ohne jegliche Garantie.", 11, Font.ITALIC, ThemeManager.getTextSecondaryColor());
        copyrightPanel.add(licenseLabel);
        aboutSection.add(Box.createVerticalStrut(20));
        aboutSection.add(copyrightPanel);
        aboutPanel.add(aboutSection);
        aboutPanel.add(Box.createVerticalGlue());
        // Add about panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.INFO + " Über", aboutScroll);
    }

    private void styleInputField(JTextField field) {
        if(field == null) return;
        field.setFont(getFontByName(Font.PLAIN, 13));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
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
        File file = new File(Main.getAppDataDir(), "categories.json");
        try {
            if (!file.exists()) {
                if (!file.createNewFile())
                    throw new IOException("Datei konnte nicht erstellt werden.");
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write("{}");
                }
            }
            Desktop.getDesktop().edit(file);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen der Kategorien-Datei: {}", e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Öffnen der Kategorien-Datei:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Öffnen der Kategorien-Datei: " + e.getMessage());
        }
    }

    private void deleteOldLogs() {
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
        int deletedCount = logManager.deleteOldLogs(30);
        File logsFolder = new File(Main.getAppDataDir(), "logs");
        int fileDeletedCount = 0;
        try {
            File[] logFiles = logsFolder.listFiles();
            if (logFiles != null) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
                for (File logFile : logFiles) {
                    if (logFile.isFile()) {
                        BasicFileAttributes attrs = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
                        LocalDateTime fileTime = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
                        if (fileTime.isBefore(cutoffDate)) {
                            Files.delete(logFile.toPath());
                            fileDeletedCount++;
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this,
                    String.format("Es wurden %d Protokolle aus der Datenbank und %d Protokolldateien gelöscht, die älter als 30 Tage sind.", deletedCount, fileDeletedCount),
                    "Alte Protokolle gelöscht",
                    JOptionPane.INFORMATION_MESSAGE);
            logger.info("Es wurden {} Protokolle aus der Datenbank und {} Protokolldateien gelöscht, die älter als 30 Tage sind.", deletedCount, fileDeletedCount);
            Main.logUtils.addLog("Es wurden " + deletedCount + " Protokolle aus der Datenbank und " + fileDeletedCount + " Protokolldateien gelöscht, die älter als 30 Tage sind.");
        } catch (IOException ex) {
            logger.error("Fehler beim Löschen alter Protokolle: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Löschen alter Protokolle:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Löschen alter Protokolle: " + ex.getMessage());
        }
    }

    private void deleteAllLogs() {
        File logsFolder = new File(Main.getAppDataDir(), "logs");
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Alle Protokolle wirklich löschen?</b><br/><br/>" +
                        "Diese Aktion löscht alle Protokolldateien im Protokolle-Ordner.<br/>" +
                        "Möchten Sie fortfahren?</html>",
                "Protokolle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                File[] logFiles = logsFolder.listFiles();
                if (logFiles != null) {
                    for (File logFile : logFiles) {
                        if (logFile.isFile()) {
                            Files.delete(logFile.toPath());
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Fehler beim Löschen der Protokolle: {}", e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Löschen der Protokolle:\n" + e.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                Main.logUtils.addLog("Fehler beim Löschen der Protokolle: " + e.getMessage());
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Alle Protokolle wurden erfolgreich gelöscht.",
                    "Protokolle gelöscht",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
        if (logManager.clearAllLogs()) {
            logger.info("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
            Main.logUtils.addLog("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
        } else {
            logger.error("Fehler beim Löschen der Protokolle aus der Datenbank.");
            Main.logUtils.addLog("Fehler beim Löschen der Protokolle aus der Datenbank.");
        }
    }

    private void openLogsFolder() {
        File logsDir = new File(Main.getAppDataDir(), "logs");
        try {
            Desktop.getDesktop().open(logsDir);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen des Protokolle-Ordners: {}", e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Öffnen des Protokolle-Ordners:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Öffnen des Protokolle-Ordners: " + e.getMessage());
        }
    }

    private void openSettingsFolder() {
        SettingsUtils.openSettingsFolder(this);
    }

    /**
     * Creates a category panel for tabbed interface with proper width management
     */
    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeManager.getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        return panel;
    }

    /**
     * Creates a scrollable panel wrapper with optimized settings
     */
    private JScrollPane createScrollablePanel(JPanel panel) {
        if(panel == null) throw new NullPointerException("Panel cannot be null");
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
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
                "<i>Warnungen werden automatisch in diesem Intervall angezeigt, wenn neue ungelesene Warnungen vorhanden sind.</i>" +
                "</div></html>");
        warningInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        warningInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return warningInfoLabel;
    }

    /**
     * Creates a modern styled section panel with card-like appearance
     */
    private JPanel createSectionPanel(String title, String description) {
        if(title == null || description == null) throw new NullPointerException("Title and description cannot be null");
        return SettingsUtils.createSectionPanel(title, description, searchableSections);
    }

    private void addSectionResetButton(JPanel section, Runnable action) {
        if(section == null || action == null) throw new NullPointerException("Section and action cannot be null");
        Object actions = section.getClientProperty("headerActions");
        if (actions instanceof JPanel panel) {
            JButton resetButton = createHeaderActionButton();
            resetButton.addActionListener(e -> action.run());
            panel.add(resetButton);
        }
    }

    private JPanel createColorRow(String labelText, String key) {
        if(labelText == null || key == null) throw new NullPointerException("Label text and key cannot be null");
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
        button.setPreferredSize(new Dimension(36, 22));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private Color getSelectedColorForKey(String key) {
        if(key == null) throw new NullPointerException("Key cannot be null");
        return switch (key) {
            case "theme_accent_color" -> selectedAccentColor;
            case "theme_header_color" -> selectedHeaderColor;
            case "theme_button_color" -> selectedButtonColor;
            default -> null;
        };
    }

    private void setSelectedColorForKey(String key, Color color) {
        if(key == null) throw new NullPointerException("Key cannot be null");
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
            label.setText(toHex(color));
        }
    }

    private JButton createHeaderActionButton() {
        JButton button = new JButton("Zuruecksetzen");
        button.setFont(getFontByName(Font.PLAIN, 12));
        button.setForeground(ThemeManager.getTextPrimaryColor());
        button.setBackground(ThemeManager.getSurfaceColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createPreviewPanel() {
        SettingsUtils.RoundedPanel previewCard = new SettingsUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        previewCardPanel = previewCard;
        previewCard.setLayout(new BorderLayout(12, 12));
        previewCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

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
        Color header = selectedHeaderColor != null ? selectedHeaderColor : (darkMode ? ThemeManager.Dark.HEADER_BG : ThemeManager.Light.HEADER_BG);
        Color accent = selectedAccentColor != null ? selectedAccentColor : ThemeManager.getAccentColor();
        Color button = selectedButtonColor != null ? selectedButtonColor : (darkMode ? ThemeManager.Dark.BUTTON_BG : ThemeManager.Light.BUTTON_BG);

        String fontName = fontComboBox == null ? DEFAULT_FONT_STYLE : (String) fontComboBox.getSelectedItem();
        int tableSize = fontSizeTableSpinner == null ? 16 : (Integer) fontSizeTableSpinner.getValue();
        int tabSize = fontSizeTabSpinner == null ? 15 : (Integer) fontSizeTabSpinner.getValue();

        previewTitleLabel.setFont(new Font(fontName, Font.BOLD, 16));
        previewTitleLabel.setForeground(Color.WHITE);
        previewTitleLabel.setOpaque(true);
        previewTitleLabel.setBackground(header);
        previewTitleLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        previewBodyLabel.setFont(new Font(fontName, Font.PLAIN, 13));
        previewBodyLabel.setForeground(secondary);
        previewButton.setFont(new Font(fontName, Font.BOLD, 12));
        previewButton.setBackground(button);
        previewButton.setForeground(Color.WHITE);

        previewTable.setFont(new Font(fontName, Font.PLAIN, Math.max(11, tableSize - 2)));
        previewTable.setForeground(text);
        previewTable.setBackground(card);
        previewTable.setSelectionBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 80));
        previewTable.getTableHeader().setFont(new Font(fontName, Font.BOLD, Math.max(10, tableSize - 3)));
        previewTable.getTableHeader().setBackground(darkMode ? ThemeManager.Dark.TABLE_HEADER_BG : ThemeManager.Light.TABLE_HEADER_BG);
        previewTable.getTableHeader().setForeground(darkMode ? ThemeManager.Dark.TABLE_HEADER_FG : ThemeManager.Light.TABLE_HEADER_FG);

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
        if(slider == null || spinner == null || sampleLabel == null) throw new IllegalArgumentException("slider, spinner and sampleLabel must not be null");
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
        if(spinner == null) throw new IllegalArgumentException("spinner must not be null");
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
        if(checkbox == null) throw new IllegalArgumentException("checkbox must not be null");
        checkbox.setFont(getFontByName(Font.PLAIN, 14));
        checkbox.setForeground(ThemeManager.getTextPrimaryColor());
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        checkbox.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));

        // Add hover effect for better UX
        checkbox.addMouseListener(new MouseAdapter() {
            private final Color originalForeground = checkbox.getForeground();
            private final Color hoverForeground = ThemeManager.getPrimaryColor();

            @Override
            public void mouseEntered(MouseEvent e) {
                checkbox.setForeground(hoverForeground);
                checkbox.setFont(checkbox.getFont().deriveFont(Font.BOLD));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkbox.setForeground(originalForeground);
                checkbox.setFont(checkbox.getFont().deriveFont(Font.PLAIN));
            }
        });
    }

    /**
     * Styles a spinner with modern, rounded appearance and focus effects
     */
    private void styleSpinner(JSpinner spinner) {
        if(spinner == null) throw new IllegalArgumentException("spinner must not be null");
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(getFontByName(Font.PLAIN, 14));
            textField.setBackground(ThemeManager.getInputBackgroundColor());
            textField.setForeground(ThemeManager.getTextPrimaryColor());
            textField.setCaretColor(ThemeManager.getTextPrimaryColor());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));

            // Add focus listener for better UX
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getPrimaryColor(), 2, true),
                            BorderFactory.createEmptyBorder(9, 13, 9, 13)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                            BorderFactory.createEmptyBorder(10, 14, 10, 14)
                    ));
                }
            });
        }

        spinner.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true));
        spinner.setBackground(ThemeManager.getInputBackgroundColor());
    }

    /**
     * Creates a modern styled button with smooth hover effects and professional appearance
     */
    private JButton createStyledButton(String text, Color originalBg) {
        if(text == null) throw new IllegalArgumentException("text must not be null");
        JButton button = new JButton(text);
        button.setFont(getFontByName(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(originalBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                BorderFactory.createEmptyBorder(14, 28, 14, 28)
        ));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Set consistent height
        button.setPreferredSize(new Dimension(
                button.getPreferredSize().width,
                48
        ));

        // Enhanced hover effect with smooth color transitions
        Color hoverBg = new Color(
                Math.max(0, originalBg.getRed() - 25),
                Math.max(0, originalBg.getGreen() - 25),
                Math.max(0, originalBg.getBlue() - 25)
        );
        Color pressedBg = new Color(
                Math.max(0, originalBg.getRed() - 50),
                Math.max(0, originalBg.getGreen() - 50),
                Math.max(0, originalBg.getBlue() - 50)
        );

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2, true),
                        BorderFactory.createEmptyBorder(13, 27, 13, 27)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                        BorderFactory.createEmptyBorder(14, 28, 14, 28)
                ));
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
                    button.setBackground(originalBg);
                }
            }
        });

        return button;
    }

    /**
     * Loads settings from the system
     */
    private void loadSettings() {
        try {
            if (Main.settings != null) {
                // Load stock check interval (default 30 minutes)
                String intervalStr = Main.settings.getProperty("stock_check_interval");
                int interval = (intervalStr != null) ? Integer.parseInt(intervalStr) : 30;
                stockCheckIntervalSpinner.setValue(interval);

                // Load warning display setting (default true)
                String enableWarningsStr = Main.settings.getProperty("enable_hourly_warnings");
                boolean enableWarnings = enableWarningsStr == null || Boolean.parseBoolean(enableWarningsStr);
                enableWarningDisplayCheckbox.setSelected(enableWarnings);

                // Load warning display interval (default 1 hour)
                String warningIntervalStr = Main.settings.getProperty("warning_display_interval");
                int warningInterval = (warningIntervalStr != null) ? Integer.parseInt(warningIntervalStr) : 1;
                warningDisplayIntervalSpinner.setValue(warningInterval);

                // Load auto stock check setting (default true)
                String enableAutoCheckStr = Main.settings.getProperty("enable_auto_stock_check");
                boolean enableAutoCheck = enableAutoCheckStr == null || Boolean.parseBoolean(enableAutoCheckStr);
                enableAutoStockCheckCheckbox.setSelected(enableAutoCheck);

                // Load server URL
                String serverUrl = Main.settings.getProperty("server_url");
                if (serverUrl == null || serverUrl.trim().isEmpty()) {
                    serverUrl = "https://framedev.ch/vebo/scans.json";
                }
                serverUrlField.setText(serverUrl);

                String enableAutomaticImportStr = Main.settings.getProperty("enable_automatic_import_qrcode");
                boolean enableAutomaticImport = enableAutomaticImportStr == null || Boolean.parseBoolean(enableAutomaticImportStr);
                automaticImportCheckBox.setSelected(enableAutomaticImport);

                String qrCodeImportIntervalStr = Main.settings.getProperty("qrcode_import_interval");
                int qrCodeImportInterval = (qrCodeImportIntervalStr != null) ? Integer.parseInt(qrCodeImportIntervalStr) : 10;
                qrCodeImportIntervalSpinner.setValue(qrCodeImportInterval);

                // Load dark mode setting (default false)
                String darkModeStr = Main.settings.getProperty("dark_mode");
                boolean darkMode = Boolean.parseBoolean(darkModeStr);
                darkModeCheckbox.setSelected(darkMode);
                themeComboBox.setSelectedItem(darkMode ? "Dark" : "Light");
                themeComboBox.setEnabled(!darkMode);

                // Load font size setting (default 14)
                String fontSizeStr = Main.settings.getProperty("table_font_size");
                int fontSize = (fontSizeStr != null) ? Integer.parseInt(fontSizeStr) : 16;
                fontSizeTableSpinner.setValue(fontSize);
                tableFontSlider.setValue(fontSize);

                String fontSizeTabStr = Main.settings.getProperty("table_font_size_tab");
                int fontSizeTab = (fontSizeTabStr != null) ? Integer.parseInt(fontSizeTabStr) : 15;
                fontSizeTabSpinner.setValue(fontSizeTab);
                tabFontSlider.setValue(fontSizeTab);

                String fontStyle = Main.settings.getProperty("font_style");
                if (fontStyle != null) {
                    fontComboBox.setSelectedItem(fontStyle);
                }
                selectedAccentColor = parseColor(Main.settings.getProperty("theme_accent_color"));
                selectedHeaderColor = parseColor(Main.settings.getProperty("theme_header_color"));
                selectedButtonColor = parseColor(Main.settings.getProperty("theme_button_color"));
                updateColorControls();
                updatePreview();

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
                // Save stock check interval
                int interval = (Integer) stockCheckIntervalSpinner.getValue();
                Main.settings.setProperty("stock_check_interval", String.valueOf(interval));

                // Save warning display setting
                boolean enableWarnings = enableWarningDisplayCheckbox.isSelected();
                Main.settings.setProperty("enable_hourly_warnings", String.valueOf(enableWarnings));

                // Save warning display interval
                int warningInterval = (Integer) warningDisplayIntervalSpinner.getValue();
                Main.settings.setProperty("warning_display_interval", String.valueOf(warningInterval));

                // Save auto stock check setting
                boolean enableAutoCheck = enableAutoStockCheckCheckbox.isSelected();
                Main.settings.setProperty("enable_auto_stock_check", String.valueOf(enableAutoCheck));

                // Save server URL
                String serverUrl = serverUrlField.getText().trim();
                Main.settings.setProperty("server_url", serverUrl);

                // Save QR-Code settings
                boolean enableAutomaticImport = automaticImportCheckBox.isSelected();
                Main.settings.setProperty("enable_automatic_import_qrcode", String.valueOf(enableAutomaticImport));

                // Save QR-Code import interval
                int qrCodeImportInterval = (Integer) qrCodeImportIntervalSpinner.getValue();
                Main.settings.setProperty("qrcode_import_interval", String.valueOf(qrCodeImportInterval));

                // Save dark mode setting
                boolean darkMode = darkModeCheckbox.isSelected();
                Main.settings.setProperty("dark_mode", String.valueOf(darkMode));

                Main.settings.setProperty("theme_accent_color", colorToSetting(selectedAccentColor));
                Main.settings.setProperty("theme_header_color", colorToSetting(selectedHeaderColor));
                Main.settings.setProperty("theme_button_color", colorToSetting(selectedButtonColor));

                // Save font size setting
                int fontSize = (Integer) fontSizeTableSpinner.getValue();
                Main.settings.setProperty("table_font_size", String.valueOf(fontSize));
                int fontSizeTab = (Integer) fontSizeTabSpinner.getValue();
                Main.settings.setProperty("table_font_size_tab", String.valueOf(fontSizeTab));
                String fontStyle = (String) fontComboBox.getSelectedItem();
                Main.settings.setProperty("font_style", fontStyle);

                Main.settings.save();

                System.out.println("[SettingsGUI] Einstellungen gespeichert");

                // Apply settings immediately
                applySettings(interval, enableWarnings, warningInterval, enableAutoCheck, darkMode);
                ThemeManager.setCustomColors(selectedAccentColor, selectedHeaderColor, selectedButtonColor);

                JOptionPane.showMessageDialog(this,
                        "<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                                "Die neuen Einstellungen wurden erfolgreich übernommen.</html>",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

                dispose();
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Speichern der Einstellungen: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Speichern!</b><br/><br/>" +
                            "Die Einstellungen konnten nicht gespeichert werden:<br/>" +
                            e.getMessage() + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
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
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Einstellungen exportieren");
        fileChooser.setSelectedFile(new File("vebo_settings.properties"));
        int choice = fileChooser.showSaveDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            Properties props = collectSettingsProperties();
            props.store(out, "VEBO Lagersystem Einstellungen");
            JOptionPane.showMessageDialog(this,
                    "Einstellungen exportiert:\n" + file.getAbsolutePath(),
                    "Export erfolgreich",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Export: " + ex.getMessage(),
                    "Export fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    private void importSettingsProfile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Einstellungen importieren");
        int choice = fileChooser.showOpenDialog(this);
        if (choice != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        try (FileInputStream in = new FileInputStream(file)) {
            Properties props = new Properties();
            props.load(in);
            applySettingsProperties(props);
            JOptionPane.showMessageDialog(this,
                    "Einstellungen importiert.\nBitte speichern, um sie zu uebernehmen.",
                    "Import erfolgreich",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Import: " + ex.getMessage(),
                    "Import fehlgeschlagen",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
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
        props.setProperty("theme_accent_color", colorToSetting(selectedAccentColor));
        props.setProperty("theme_header_color", colorToSetting(selectedHeaderColor));
        props.setProperty("theme_button_color", colorToSetting(selectedButtonColor));
        return props;
    }

    private void applySettingsProperties(Properties props) {
        if(props == null) throw new IllegalArgumentException("props must not be null");
        stockCheckIntervalSpinner.setValue(parseIntProperty(props, "stock_check_interval", DEFAULT_STOCK_CHECK_INTERVAL));
        enableWarningDisplayCheckbox.setSelected(Boolean.parseBoolean(props.getProperty("enable_hourly_warnings", String.valueOf(DEFAULT_ENABLE_WARNINGS))));
        warningDisplayIntervalSpinner.setValue(parseIntProperty(props, "warning_display_interval", DEFAULT_WARNING_INTERVAL));
        enableAutoStockCheckCheckbox.setSelected(Boolean.parseBoolean(props.getProperty("enable_auto_stock_check", String.valueOf(DEFAULT_ENABLE_AUTO_STOCK_CHECK))));
        serverUrlField.setText(props.getProperty("server_url", DEFAULT_SERVER_URL));
        automaticImportCheckBox.setSelected(Boolean.parseBoolean(props.getProperty("enable_automatic_import_qrcode", String.valueOf(DEFAULT_ENABLE_QR_IMPORT))));
        qrCodeImportIntervalSpinner.setValue(parseIntProperty(props, "qrcode_import_interval", DEFAULT_QR_IMPORT_INTERVAL));
        boolean darkMode = Boolean.parseBoolean(props.getProperty("dark_mode", String.valueOf(DEFAULT_DARK_MODE)));
        darkModeCheckbox.setSelected(darkMode);
        themeComboBox.setSelectedItem(darkMode ? "Dark" : "Light");
        themeComboBox.setEnabled(!darkMode);
        fontSizeTableSpinner.setValue(parseIntProperty(props, "table_font_size", DEFAULT_TABLE_FONT_SIZE));
        fontSizeTabSpinner.setValue(parseIntProperty(props, "table_font_size_tab", DEFAULT_TAB_FONT_SIZE));
        fontComboBox.setSelectedItem(props.getProperty("font_style", DEFAULT_FONT_STYLE));
        selectedAccentColor = parseColor(props.getProperty("theme_accent_color"));
        selectedHeaderColor = parseColor(props.getProperty("theme_header_color"));
        selectedButtonColor = parseColor(props.getProperty("theme_button_color"));
        updateColorControls();
        updatePreview();
    }

    private int parseIntProperty(Properties props, String key, int fallback) {
        if(props == null) throw new IllegalArgumentException("props must not be null");
        if(key == null) throw new IllegalArgumentException("key must not be null");
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String colorToSetting(Color color) {
        return color == null ? "" : toHex(color);
    }

    private Color parseColor(String value) {
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

    private String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    @SuppressWarnings("MagicConstant")
    public static Font getFontByName(int style, int fontSize) {
        String fontName = Main.settings.getProperty("font_style");
        if (fontName == null || fontName.trim().isEmpty()) {
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
     * Applies settings to the running system
     */
    private void applySettings(int interval, boolean enableWarnings, int warningInterval, boolean enableAutoCheck, boolean darkMode) {
        try {
            // Apply theme changes
            boolean currentDarkMode = ThemeManager.isDarkMode();
            if (currentDarkMode != darkMode) {
                ThemeManager.setDarkMode(darkMode);
                System.out.println("[SettingsGUI] Theme geändert zu: " + (darkMode ? "Dark Mode" : "Light Mode"));

                // Show restart recommendation for full theme change
                int restart = JOptionPane.showConfirmDialog(this,
                        "<html>Das Theme wurde geändert.<br/><br/>" +
                                "Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                "damit das Theme vollständig angewendet wird.<br/><br/>" +
                                "Möchten Sie jetzt neu starten?</html>",
                        "Neustart empfohlen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        Main.iconSmall);

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                    return;
                }
            }

            SchedulerManager scheduler = SchedulerManager.getInstance();

            // Shutdown existing scheduler
            scheduler.shutdown();

            // Wait a moment for clean shutdown
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Restart scheduler with new settings
            if (enableAutoCheck) {
                scheduler.startScheduledStockCheck(interval, java.util.concurrent.TimeUnit.MINUTES);
                System.out.println("[SettingsGUI] Automatische Lagerbestandsprüfung neu gestartet (Intervall: " + interval + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatische Lagerbestandsprüfung deaktiviert");
            }

            if (enableWarnings) {
                scheduler.startWarningDisplay(warningInterval, java.util.concurrent.TimeUnit.HOURS);
                System.out.println("[SettingsGUI] Automatische Warnanzeige aktiviert (Intervall: " + warningInterval + " Stunde(n))");
            } else {
                System.out.println("[SettingsGUI] Automatische Warnanzeige deaktiviert");
            }

            if (automaticImportCheckBox.isSelected()) {
                scheduler.startAutoImportQrCodes(
                        (Integer) qrCodeImportIntervalSpinner.getValue(),
                        java.util.concurrent.TimeUnit.MINUTES
                );
                System.out.println("[SettingsGUI] Automatischer QR-Code Import aktiviert (Intervall: " + qrCodeImportIntervalSpinner.getValue() + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatischer QR-Code Import deaktiviert");
            }

        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Anwenden der Einstellungen: " + e.getMessage());
            logger.error("Fehler beim Anwenden der Einstellungen", e);
            Main.logUtils.addLog("Fehler beim Anwenden der Einstellungen: " + e.getMessage());
        }
    }

    /**
     * Checks for updates based on the selected release channel (stable, beta, alpha, testing)
     * Shows a dialog with update information and download link if available
     */
    private void checkForUpdates(String channel) {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            // Convert string to ReleaseChannel enum
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

            // Create MODAL progress dialog
            JDialog progressDialog = new JDialog(this, "Nach Updates suchen...", true);
            progressDialog.setLayout(new BorderLayout());
            progressDialog.setSize(400, 150);
            progressDialog.setLocationRelativeTo(this);
            progressDialog.setUndecorated(true);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.setResizable(false);

            JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
            progressPanel.setBackground(ThemeManager.getCardBackgroundColor());
            progressPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            JLabel progressLabel = new JLabel("<html><center>Prüfe auf " + channelDisplay + "-Updates...<br/>Bitte warten...</center></html>");
            progressLabel.setFont(getFontByName(Font.PLAIN, 14));
            progressLabel.setForeground(ThemeManager.getTextPrimaryColor());
            progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

            progressPanel.add(progressLabel, BorderLayout.CENTER);
            progressDialog.add(progressPanel);

            // Check for updates in background
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return updateManager.getLatestVersion(releaseChannel);
                }

                @Override
                protected void done() {
                    try {
                        String latestVersion = get();
                        handleUpdateResult(latestVersion, channel);
                    } catch (Exception e) {
                        logger.error("Fehler beim Prüfen auf Updates: {}", e.getMessage(), e);
                        showUpdateError(e.getMessage());
                    } finally {
                        // Always close the progress dialog
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                    }
                }
            };

            // Start worker
            worker.execute();
            // Show modal dialog - this will block until disposed
            progressDialog.setVisible(true);

        } catch (Exception e) {
            logger.error("Fehler beim Starten der Update-Prüfung: {}", e.getMessage(), e);
            showUpdateError(e.getMessage());
            Main.logUtils.addLog("Fehler beim Starten der Update-Prüfung: " + e.getMessage());
        }
    }

    /**
     * Handles the update check result and displays appropriate dialog
     */
    private void handleUpdateResult(String latestVersion, String channel) {
        if (latestVersion == null) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Keine Update-Informationen verfügbar</b><br/><br/>" +
                            "Die Update-Informationen konnten nicht abgerufen werden.<br/>" +
                            "Bitte überprüfen Sie Ihre Internetverbindung.</html>",
                    "Update-Prüfung",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        String currentVersion = Main.VERSION;
        UpdateManager updateManager = UpdateManager.getInstance();

        if (updateManager.isUpdateAvailable(currentVersion)) {
            showUpdateAvailableDialog(latestVersion, currentVersion, channel);
        } else {
            showNoUpdateDialog(currentVersion, channel);
        }
    }

    /**
     * Shows dialog when an update is available
     */
    private void showUpdateAvailableDialog(String latestVersion, String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        String downloadUrl = getDownloadUrl(channel);

        Object[] options = {"Download-Seite öffnen", "Später"};
        int result = JOptionPane.showOptionDialog(this,
                "<html><b>" + UnicodeSymbols.DOWNLOAD + " Update verfügbar!</b><br/><br/>" +
                        "Eine neue Version ist verfügbar:<br/><br/>" +
                        "Aktuelle Version: <b>" + currentVersion + "</b><br/>" +
                        "Neue Version: <b>" + latestVersion + channelDisplay + "</b><br/><br/>" +
                        "Möchten Sie die Download-Seite öffnen?</html>",
                "Update verfügbar",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall,
                options,
                options[0]);

        if (result == 0) { // Download-Seite öffnen
            openDownloadPage(downloadUrl);
        }
    }

    /**
     * Shows dialog when no update is available
     */
    private void showNoUpdateDialog(String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        JOptionPane.showMessageDialog(this,
                "<html><b>" + UnicodeSymbols.CHECKMARK + " Keine Updates verfügbar</b><br/><br/>" +
                        "Sie verwenden bereits die neueste Version" + channelDisplay + ":<br/>" +
                        "<b>Version " + currentVersion + "</b></html>",
                "Update-Prüfung",
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
    }

    /**
     * Shows error dialog when update check fails
     */
    private void showUpdateError(String errorMessage) {
        if(errorMessage == null) throw new IllegalArgumentException("errorMessage must not be null");
        JOptionPane.showMessageDialog(this,
                "<html><b>" + UnicodeSymbols.WARNING + " Fehler bei Update-Prüfung</b><br/><br/>" +
                        "Die Update-Prüfung ist fehlgeschlagen:<br/>" +
                        errorMessage + "</html>",
                "Fehler",
                JOptionPane.ERROR_MESSAGE,
                Main.iconSmall);
        Main.logUtils.addLog("Fehler bei Update-Prüfung");
    }

    /**
     * Gets the download URL based on the release channel
     */
    private String getDownloadUrl(String channel) {
        if(channel == null) throw new IllegalArgumentException("channel must not be null");
        return switch (channel.toLowerCase()) {
            case "beta" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=beta";
            case "alpha" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=alpha";
            case "testing" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=testing";
            default -> "https://github.com/frame-dev/VeboLagerSystem/releases/latest";
        };
    }

    /**
     * Opens the download page in the default browser
     */
    private void openDownloadPage(String url) {
        if(url == null) throw new IllegalArgumentException("url must not be null");
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new java.net.URI(url));
                logger.info("Download-Seite geöffnet: {}", url);
            } else {
                // Fallback: show URL in dialog
                JOptionPane.showMessageDialog(this,
                        "<html><b>Download-Link:</b><br/><br/>" +
                                "<a href='" + url + "'>" + url + "</a><br/><br/>" +
                                "Bitte kopieren Sie den Link und öffnen Sie ihn in Ihrem Browser.</html>",
                        "Download-Link",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Öffnen der Download-Seite: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Öffnen des Browsers</b><br/><br/>" +
                            "Bitte öffnen Sie den folgenden Link manuell:<br/>" +
                            url + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    public void display() {
        setVisible(true);
    }

    /**
     * Deletes a specific table from the database
     */
    private void deleteTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        try {
            DatabaseManager dbManager = databaseManager;

            if (dbManager != null) {
                // Execute DROP TABLE command
                String dropTableSQL = "DROP TABLE IF EXISTS " + tableName + ";";
                boolean success = dbManager.executeUpdate(dropTableSQL);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            String.format("<html><b>OK Tabelle erfolgreich gelöscht</b><br/><br/>" +
                                            "Die Tabelle '<b>%s</b>' wurde aus der Datenbank entfernt.<br/><br/>" +
                                            "<i>Hinweis: Die zugehörigen Daten sind permanent gelöscht.</i></html>",
                                    tableName),
                            "Erfolgreich",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);

                    System.out.printf("[SettingsGUI] Tabelle '%s' wurde erfolgreich gelöscht%n", tableName);

                    // Show restart recommendation
                    int restart = JOptionPane.showConfirmDialog(this,
                            "<html>Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                    "um Inkonsistenzen zu vermeiden.<br/><br/>" +
                                    "Möchten Sie jetzt neu starten?</html>",
                            "Neustart empfohlen",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            Main.iconSmall);

                    if (restart == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                            "Die Tabelle '<b>%s</b>' konnte nicht gelöscht werden.<br/>" +
                                            "Bitte überprüfen Sie die Logs für weitere Details.</html>",
                                    tableName),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                    Main.logUtils.addLog(String.format("Fehler beim Löschen der Tabelle. Die Tabelle '%s' konnte nicht gelöscht werden.", tableName));
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
            }
        } catch (Exception e) {
            System.err.printf("[SettingsGUI] Fehler beim Löschen der Tabelle '%s': %s%n",
                    tableName, e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Löschen der Tabelle '%s': %s", tableName, e.getMessage()));
            logger.error("Fehler beim Löschen der Tabelle '{}'", tableName, e);
            JOptionPane.showMessageDialog(this,
                    String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                    "Tabelle: %s<br/>" +
                                    "Fehler: %s</html>",
                            tableName, e.getMessage()),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    /**
     * Clears the database after user confirmation
     */
    private void clearDatabase() {
        // First confirmation
        int firstConfirm = JOptionPane.showConfirmDialog(this,
                "<html><b>⚠ WARNUNG: Datenbank löschen</b><br/><br/>" +
                        "Möchten Sie wirklich <b>ALLE DATEN</b> aus der Datenbank löschen?<br/><br/>" +
                        "Dies umfasst:<br/>" +
                        "- Alle Artikel<br/>" +
                        "- Alle Lieferanten<br/>" +
                        "- Alle Bestellungen<br/>" +
                        "- Alle Kunden<br/>" +
                        "- Alle Abteilungen<br/>" +
                        "- Alle Benutzer<br/>" +
                        "- Alle Logs<br/>" +
                        "- Alle Benutzer</br>" +
                        "- Alle Notizen<br/>" +
                        "- Alle Warnungen<br/><br/>" +
                        "<b>Diese Aktion kann NICHT rückgängig gemacht werden!</b></html>",
                "Datenbank löschen - Bestätigung 1/2",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                Main.iconSmall);

        if (firstConfirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Second confirmation (extra safety)
        String confirmText = JOptionPane.showInputDialog(this,
                "<html><b>Zweite Bestätigung erforderlich</b><br/><br/>" +
                        "Bitte geben Sie <b>LÖSCHEN</b> ein, um fortzufahren:</html>",
                "Datenbank löschen - Bestätigung 2/2",
                JOptionPane.WARNING_MESSAGE);

        if (confirmText == null || !confirmText.trim().equalsIgnoreCase("LÖSCHEN")) {
            JOptionPane.showMessageDialog(this,
                    "Vorgang abgebrochen. Die Datenbank wurde nicht gelöscht.",
                    "Abgebrochen",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
            return;
        }

        // Perform database clearing
        try {
            DatabaseManager dbManager =
                    databaseManager;

            if (dbManager != null) {
                dbManager.clearDatabase();
                File file = new File(Main.getAppDataDir(), "own_use_list.txt");
                if (!file.delete())
                    System.out.println("[SettingsGUI] own_use_list.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                File importQrcodesFile = new File(Main.getAppDataDir(), "imported_qrcodes.txt");
                if (!importQrcodesFile.delete())
                    System.out.println("[SettingsGUI] imported_qrcodes.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                File importedItemsFile = new File(Main.getAppDataDir(), "imported_items.txt");
                if (!importedItemsFile.delete())
                    System.out.println("[SettingsGUI] imported_items.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");

                JOptionPane.showMessageDialog(this,
                        "<html><b>OK Datenbank erfolgreich gelöscht</b><br/><br/>" +
                                "Alle Daten wurden aus der Datenbank entfernt.<br/><br/>" +
                                "Das Programm sollte nun neu gestartet werden.</html>",
                        "Erfolgreich",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

                System.out.println("[SettingsGUI] Datenbank wurde erfolgreich bereinigt");

                // Ask if user wants to restart
                int restart = JOptionPane.showConfirmDialog(this,
                        "Möchten Sie das Programm jetzt neu starten?",
                        "Neustart empfohlen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                Main.logUtils.addLog("Fehler: Datenbankverbindung nicht verfügbar.");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Löschen der Datenbank: " + e.getMessage());
            logger.error("Fehler beim Löschen der Datenbank", e);
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Löschen der Datenbank</b><br/><br/>" +
                            "Fehler: " + e.getMessage() + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
        }
    }

    /**
     * Creates a panel with a label, spinner, and unit label
     */
    private JPanel createLabeledSpinnerPanel(String labelText, JSpinner spinner, String unitText, int columns) {
        if(labelText == null) throw new IllegalArgumentException("labelText must not be null");
        if(spinner == null) throw new IllegalArgumentException("spinner must not be null");
        if(unitText == null) throw new IllegalArgumentException("unitText must not be null");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        spinner.setFont(getFontByName(Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
        styleSpinner(spinner);

        JLabel unitLabel = new JLabel(unitText);
        unitLabel.setFont(getFontByName(Font.PLAIN, 13));
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
        if(labelText == null) throw new IllegalArgumentException("labelText must not be null");
        if(comboBox == null) throw new IllegalArgumentException("comboBox must not be null");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        comboBox.setPreferredSize(new Dimension(200, 35));

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(comboBox);

        return panel;
    }

    /**
     * Creates a styled label with specified font and color
     */
    @SuppressWarnings("MagicConstant")
    private JLabel createStyledLabel(String text, int fontSize, int fontStyle, Color color) {
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
     * Creates an info label with HTML content
     */
    private JLabel createInfoLabel(String htmlContent) {
        JLabel label = new JLabel(htmlContent);
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static void importFromCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV-Datei zum Importieren auswählen");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV-Dateien", "csv"));
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setCurrentDirectory(Main.getAppDataDir());

        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("[SettingsGUI] Import abgebrochen");
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String fileName = selectedFile.getName().toLowerCase();

        // Determine file type based on filename
        if (fileName.contains("article")) {
            importArticlesFromCsv(selectedFile);
        } else if (fileName.contains("vendor") || fileName.contains("supplier")) {
            importVendorsFromCsv(selectedFile);
        } else if (fileName.contains("client") || fileName.contains("customer")) {
            importClientsFromCsv(selectedFile);
        } else if (fileName.contains("order")) {
            importOrdersFromCsv();
        } else {
            // Ask user what type of data this is
            String[] options = {"Artikel", "Lieferanten", "Kunden", "Bestellungen", "Abbrechen"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Welche Art von Daten möchten Sie importieren?",
                    "Datentyp auswählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall,
                    options,
                    options[0]);

            switch (choice) {
                case 0 -> importArticlesFromCsv(selectedFile);
                case 1 -> importVendorsFromCsv(selectedFile);
                case 2 -> importClientsFromCsv(selectedFile);
                case 3 -> importOrdersFromCsv();
                default -> System.out.println("[SettingsGUI] Import abgebrochen");
            }
        }
    }

    private static void importArticlesFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            ArticleManager articleManager = ArticleManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 8) {
                        errors++;
                        continue;
                    }

                    String articleNumber = parts[0];
                    Article article = getArticle(parts, articleNumber);

                    if (articleManager.existsArticle(articleNumber)) {
                        if (articleManager.updateArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (articleManager.insertArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Artikel-Import abgeschlossen</b><br/><br/>" +
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Artikel-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Artikel: " + e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Importieren der Artikel: %s", e.getMessage()));
        }
    }

    private static Article getArticle(String[] parts, String articleNumber) {
        String name = parts[1];
        String details = parts[2];
        int stockQuantity = Integer.parseInt(parts[3]);
        int minStockLevel = Integer.parseInt(parts[4]);
        double sellPrice = Double.parseDouble(parts[5]);
        double purchasePrice = Double.parseDouble(parts[6]);
        String vendorName = parts[7];

        return new Article(articleNumber, name, details,
                stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName);
    }

    private static void importVendorsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            VendorManager vendorManager = VendorManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 5) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String contactPerson = parts[1];
                    String phoneNumber = parts[2];
                    String email = parts[3];
                    String address = parts[4];
                    double minOrderValue = Double.parseDouble(parts[5]);

                    Vendor vendor = new Vendor(name, contactPerson, phoneNumber, email, address, new ArrayList<>(), minOrderValue);

                    if (vendorManager.existsVendor(name)) {
                        String[] columns = {"contactPerson", "phoneNumber", "email", "address"};
                        Object[] values = {contactPerson, phoneNumber, email, address};
                        if (vendorManager.updateVendor(name, columns, values)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (vendorManager.insertVendor(vendor)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Lieferanten-Import abgeschlossen</b><br/><br/>" +
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Lieferanten-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Lieferanten-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Lieferanten: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Lieferanten: " + e.getMessage());
        }
    }

    private static void importClientsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                Main.logUtils.addLog("Die CSV-Datei ist leer.");
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String department = parts[1];

                    if (clientManager.existsClient(name)) {
                        if (clientManager.updateClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (clientManager.insertClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Kunden-Import abgeschlossen</b><br/><br/>" +
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Kunden-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Kunden-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Kunden: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Kunden: " + e.getMessage());
        }
    }

    private static void importOrdersFromCsv() {
        JOptionPane.showMessageDialog(null,
                "<html><b>Bestellungs-Import nicht verfügbar</b><br/><br/>" +
                        "Der Import von Bestellungen ist aus Sicherheitsgründen deaktiviert.<br/>" +
                        "Bestellungen sollten nur über die normale Bestellfunktion erstellt werden.</html>",
                "Nicht verfügbar",
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
        System.out.println("[SettingsGUI] Bestellungs-Import wurde übersprungen (nicht implementiert aus Sicherheitsgründen)");
    }

    /**
     * Parses a CSV line, handling quoted fields with commas
     */
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append("©");
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Exports all database tables to CSV files in the application data directory.
     * Creates separate CSV files for articles, vendors, clients, and orders.
     */
    private static void exportToCsv() {
        int successCount = 0;
        int totalTables = 4;

        // Export Articles
        List<Article> articles = ArticleManager.getInstance().getAllArticles();
        File csvFile = new File(Main.getAppDataDir(), "articles_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant");

            for (Article article : articles) {
                writer.format(Locale.ROOT,
                        "\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,\"%s\"%n",
                        escapeCSV(article.getArticleNumber()),
                        escapeCSV(article.getName()),
                        escapeCSV(article.getDetails()),
                        article.getStockQuantity(),
                        article.getMinStockLevel(),
                        article.getSellPrice(),
                        article.getPurchasePrice(),
                        escapeCSV(article.getVendorName())
                );
            }
            System.out.println("[SettingsGUI] Artikel erfolgreich nach " + csvFile.getAbsolutePath() + " exportiert (" + articles.size() + " Einträge)");
            successCount++;
        } catch (Exception e) {
            String errorMsg = "Fehler beim Exportieren der Artikel: " + e.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, e);
            Main.logUtils.addLog(errorMsg);
        }

        // Export Vendors
        List<Vendor> vendors = VendorManager.getInstance().getVendors();
        File vendorCsvFile = new File(Main.getAppDataDir(), "vendors_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(vendorCsvFile))) {
            writer.println("Name,Kontaktperson,Telefon,E-Mail,Adresse,MinBestellwert");
            for (Vendor vendor : vendors) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(vendor.getName()),
                        escapeCSV(vendor.getContactPerson()),
                        escapeCSV(vendor.getPhoneNumber()),
                        escapeCSV(vendor.getEmail()),
                        escapeCSV(vendor.getAddress()),
                        escapeCSV(String.valueOf(vendor.getMinOrderValue())));
            }
            System.out.println("[SettingsGUI] Lieferanten erfolgreich nach " + vendorCsvFile.getAbsolutePath() + " exportiert (" + vendors.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Lieferanten: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        // Export Clients
        List<Map<String, String>> clients = ClientManager.getInstance().getAllClients();
        File clientCsvFile = new File(Main.getAppDataDir(), "clients_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(clientCsvFile))) {
            writer.println("Name,Abteilung");
            for (Map<String, String> clientMap : clients) {
                String name = clientMap.getOrDefault("firstLastName", "");
                String department = clientMap.getOrDefault("department", "");
                writer.printf("\"%s\",\"%s\"%n", escapeCSV(name), escapeCSV(department));
            }
            System.out.println("[SettingsGUI] Kunden erfolgreich nach " + clientCsvFile.getAbsolutePath() + " exportiert (" + clients.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Kunden: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        // Export Orders
        List<Order> orders = OrderManager.getInstance().getOrders();
        File orderCsvFile = new File(Main.getAppDataDir(), "orders_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(orderCsvFile))) {
            writer.println("Bestell-ID,Empfängername,EmpfängerKontoNummer,SenderName,SenderKontoNummer,Artikel,Bestelldatum,Status,Abteilung");
            for (Order order : orders) {
                // Format ordered articles as "ArticleNum1:Qty1;ArticleNum2:Qty2;..."
                String articlesStr = order.getOrderedArticles().entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(java.util.stream.Collectors.joining(";"));

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(order.getOrderId()),
                        escapeCSV(order.getReceiverName()),
                        escapeCSV(order.getReceiverKontoNumber()),
                        escapeCSV(order.getSenderName()),
                        escapeCSV(order.getSenderKontoNumber()),
                        escapeCSV(articlesStr),
                        escapeCSV(order.getOrderDate()),
                        escapeCSV(order.getStatus()),
                        escapeCSV(order.getDepartment()));
            }
            System.out.println("[SettingsGUI] Bestellungen erfolgreich nach " + orderCsvFile.getAbsolutePath() + " exportiert (" + orders.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Bestellungen: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
        }

        // Show summary
        System.out.println("[SettingsGUI] CSV-Export abgeschlossen: " + successCount + "/" + totalTables + " Tabellen erfolgreich exportiert");
        System.out.println("[SettingsGUI] Dateien gespeichert in: " + Main.getAppDataDir().getAbsolutePath());
    }

    /**
     * Helper method to escape special characters in CSV values (quotes, commas, newlines)
     */
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes by doubling them and remove any existing outer quotes
        return value.replace("\"", "\"\"");
    }

    private void styleComboBox(JComboBox<String> combo) {
        if(combo == null) throw new IllegalArgumentException("combo must not be null");
        Color bg = ThemeManager.getInputBackgroundColor();
        Color fg = ThemeManager.getTextPrimaryColor();
        Color border = ThemeManager.getInputBorderColor();
        Color selBg = ThemeManager.getSelectionBackgroundColor();
        Color selFg = ThemeManager.getSelectionForegroundColor();

        combo.setOpaque(true);
        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // IMPORTANT: force popup list colors via renderer AND list defaults
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel c = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                // enforce list base colors too (popup JList)
                list.setBackground(bg);
                list.setForeground(fg);
                list.setSelectionBackground(selBg);
                list.setSelectionForeground(selFg);

                c.setOpaque(true);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    c.setBackground(selBg);
                    c.setForeground(selFg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(fg);
                }
                return c;
            }
        });

        // Theme arrow button + popup border using a small UI override (most reliable)
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("▾");
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        b.setBackground(ThemeManager.getSurfaceColor());
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        b.setBackground(bg);
                    }
                });
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof BasicComboPopup basic) {
                    basic.setBorder(BorderFactory.createLineBorder(border, 1));
                    basic.getList().setBackground(bg);
                    basic.getList().setForeground(fg);
                    basic.getList().setSelectionBackground(selBg);
                    basic.getList().setSelectionForeground(selFg);
                }
                return popup;
            }
        });

        // If editable: theme the editor field
        if (combo.isEditable()) {
            Component editorComp = combo.getEditor().getEditorComponent();
            if (editorComp instanceof JTextField tf) {
                tf.setBackground(bg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBorder(null);
            }
        }
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
