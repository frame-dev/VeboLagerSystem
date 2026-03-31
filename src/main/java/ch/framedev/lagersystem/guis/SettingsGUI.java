package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.ArticleUtils.CategoryRange;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.text.NumberFormat;
import java.io.File;
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
    private record MaintenanceActionSpec(
            String text,
            String tooltip,
            Color color,
            Runnable action,
            String successTitle,
            String successMessage) {
    }

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
    private JCheckBox deleteOldLogsCheckBox;
    private JSpinner qrCodeImportIntervalSpinner;
    private JSpinner fontSizeTableSpinner;
    private JSpinner fontSizeTabSpinner;
    private JTextField serverUrlField;
    private JComboBox<String> themeComboBox;
    private static JComboBox<String> fontComboBox;
    private JComboBox<DatabaseManager.DatabaseType> databaseTypeComboBox;
    private JComboBox<DatabaseManager.DatabaseType> migrationSourceComboBox;
    private JLabel databaseBackendHintLabel;
    private JLabel databaseMigrationHintLabel;
    private JButton migrateDatabaseButton;
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
    private Point dragStartPoint;
    private int activeMaintenanceTasks;

    private static final int DEFAULT_STOCK_CHECK_INTERVAL = 30;
    private static final int DEFAULT_WARNING_INTERVAL = 1;
    private static final int DEFAULT_QR_IMPORT_INTERVAL = 10;
    private static final int DEFAULT_TABLE_FONT_SIZE = 16;
    private static final int DEFAULT_TAB_FONT_SIZE = 15;
    private static final boolean DEFAULT_ENABLE_AUTO_STOCK_CHECK = true;
    private static final boolean DEFAULT_ENABLE_WARNINGS = true;
    private static final boolean DEFAULT_ENABLE_QR_IMPORT = true;
    private static final boolean DEFAULT_DARK_MODE = false;
    private static final boolean DEFAULT_DELETE_OLD_LOGS_ON_STARTUP = false;
    private static final DatabaseManager.DatabaseType DEFAULT_DATABASE_TYPE = DatabaseManager.DatabaseType.SQLITE;
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
        THEME_BUTTON_COLOR("theme_button_color"),
        /**
         * Welches Speicher-Backend verwendet werden soll. Unterstützt sqlite, h2,
         * json und yaml.
         */
        DATABASE_TYPE("database_type"),
        /**
         * Ob alte Protokolle beim Start automatisch bereinigt werden sollen.
         */
        DELETE_OLD_LOGS_ON_STARTUP("delete_old_logs_on_startup");

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
        setUndecorated(true);
        setTitle("Einstellungen");
        setSize(1200, 850);
        setMinimumSize(new Dimension(1100, 760));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(getSoftGlassBorder(), 1, true));

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
        headerCard.add(createHeaderActionsPanel(), BorderLayout.EAST);
        installWindowDrag(headerCard);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(headerCard, BorderLayout.CENTER);
        // allow full-width under BoxLayout/BorderLayout parents
        // (removed setMaximumSize line)

        return wrapper;
    }

    private JPanel createHeaderActionsPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionsPanel.setOpaque(false);

        JButton closeButton = createHeaderCloseButton();
        closeButton.addActionListener(e -> dispose());
        actionsPanel.add(closeButton);

        return actionsPanel;
    }

    private JButton createHeaderCloseButton() {
        JButton button = new JButton(UnicodeSymbols.CLOSE + " Schließen");
        button.setFont(getFontByName(Font.BOLD, 13));
        button.setForeground(getReadableTextColor(ThemeManager.getDangerColor()));
        button.setBackground(ThemeManager.getDangerColor());
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(adjustColor(ThemeManager.getDangerColor(), -0.18f), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Schließt das Einstellungsfenster");

        Color baseBg = ThemeManager.getDangerColor();
        Color hoverBg = adjustColor(baseBg, -0.08f);
        Color pressedBg = adjustColor(baseBg, -0.16f);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseBg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : baseBg);
            }
        });

        return button;
    }

    private void installWindowDrag(JComponent target) {
        if (target == null) {
            return;
        }

        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStartPoint = e.getPoint();
            }
        });
        target.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartPoint == null) {
                    return;
                }
                Point screenPoint = e.getLocationOnScreen();
                setLocation(screenPoint.x - dragStartPoint.x, screenPoint.y - dragStartPoint.y);
            }
        });
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

        JButton clearSearchButton = new JButton(UnicodeSymbols.CLEAR);
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
        JPanel buttonPanel = createWrapButtonPanel(FlowLayout.RIGHT, 12, 12);
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
        settingsSearchField.setToolTipText(KeyboardShortcutUtils.withShortcutHint(
                settingsSearchField.getToolTipText(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())));

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeSettings");
        am.put("closeSettings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "saveSettings");
        am.put("saveSettings", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveSettings();
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

    private JPanel createTabContentPanel() {
        JPanel panel = createCategoryPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private JPanel createStandardSectionCard(String title, String description, Runnable resetAction) {
        JPanel card = SettingsUtils.createSectionPanel(title, description, null);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        card.setBackground(ThemeManager.getCardBackgroundColor());
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (resetAction != null) {
            addSectionResetButton(card, resetAction);
        }
        return card;
    }

    private void addTabSection(JPanel panel, JComponent section, int spacingAfter) {
        panel.add(section);
        if (spacingAfter > 0) {
            panel.add(Box.createVerticalStrut(spacingAfter));
        }
    }

    private void addScrollableTab(JTabbedPane tabbedPane, String title, JPanel panel) {
        JScrollPane scrollPane = createScrollablePanel(panel);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabbedPane.addTab(title, scrollPane);
    }

    private void buildSystemAutomaticTab(JTabbedPane tabbedPane) {
        JPanel systemPanel = createTabContentPanel();
        addTabSection(systemPanel, createStockCheckCard(), 22);
        addTabSection(systemPanel, createWarningDisplayCard(), 22);
        addTabSection(systemPanel, createQrCodeImportCard(), 22);
        addTabSection(systemPanel, createCategoryManagementCard(), 22);
        addTabSection(systemPanel, createOtherSettingsCard(), 18);
        addTabSection(systemPanel, createMaintenanceReloadPanel(), 0);
        systemPanel.add(Box.createVerticalGlue());
        addScrollableTab(tabbedPane, UnicodeSymbols.WRENCH + " System", systemPanel);
    }

    private JPanel createStockCheckCard() {
        JPanel card = createStandardSectionCard(
                UnicodeSymbols.PACKAGE + " Lagerbestandsprüfung",
                "Automatische Überprüfung des Lagerbestands und Warnerstellung",
                this::resetStockCheckDefaults);

        enableAutoStockCheckCheckbox = new JCheckBox("Automatische Lagerbestandsprüfung aktivieren");
        styleCheckbox(enableAutoStockCheckCheckbox);
        enableAutoStockCheckCheckbox.setSelected(true);
        enableAutoStockCheckCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        stockCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
        JPanel intervalPanel = createLabeledSpinnerPanel("Prüfintervall:", stockCheckIntervalSpinner, "Minuten", 6);
        intervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(Box.createVerticalStrut(18));
        card.add(enableAutoStockCheckCheckbox);
        card.add(Box.createVerticalStrut(15));
        card.add(intervalPanel);
        return card;
    }

    private JPanel createWarningDisplayCard() {
        JPanel card = createStandardSectionCard(
                UnicodeSymbols.WARNING + " Warnungsanzeige",
                "Konfiguration der automatischen Warnanzeige",
                this::resetWarningDefaults);

        enableWarningDisplayCheckbox = new JCheckBox("Automatische Anzeige ungelesener Warnungen aktivieren");
        styleCheckbox(enableWarningDisplayCheckbox);
        enableWarningDisplayCheckbox.setSelected(true);
        enableWarningDisplayCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        warningDisplayIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        JPanel intervalPanel = createLabeledSpinnerPanel("Anzeigeintervall:", warningDisplayIntervalSpinner, "Stunde(n)", 6);
        intervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoLabel = getWarningInfoLabel();
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(Box.createVerticalStrut(18));
        card.add(enableWarningDisplayCheckbox);
        card.add(Box.createVerticalStrut(15));
        card.add(intervalPanel);
        card.add(Box.createVerticalStrut(10));
        card.add(infoLabel);
        return card;
    }

    private JPanel createQrCodeImportCard() {
        JPanel card = createStandardSectionCard(
                UnicodeSymbols.PHONE + " QR-Code Import",
                "Automatischer Import von QR-Code Scans",
                this::resetQrCodeDefaults);

        automaticImportCheckBox = new JCheckBox("Automatisches Importieren von QR-Codes aktivieren");
        styleCheckbox(automaticImportCheckBox);
        automaticImportCheckBox.setSelected(false);
        automaticImportCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        qrCodeImportIntervalSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 60, 5));
        JPanel intervalPanel = createLabeledSpinnerPanel("Import-Intervall:", qrCodeImportIntervalSpinner, "Minuten", 6);
        intervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(Box.createVerticalStrut(18));
        card.add(automaticImportCheckBox);
        card.add(Box.createVerticalStrut(15));
        card.add(intervalPanel);
        return card;
    }

    private JPanel createCategoryManagementCard() {
        JList<String> categoryList = createCategoryList();
        JPanel categoryCard = createStandardSectionCard(
                UnicodeSymbols.FOLDER + " Kategorien",
                "Einstellungen zu verschiedenen Kategorien",
                null);

        categoryCard.add(createInfoLabel("Hier können Sie Kategorien hinzufügen oder die Datei direkt bearbeiten. Optional können Sie einen Nummernbereich (Von/Bis) angeben."));
        categoryCard.add(Box.createVerticalStrut(10));
        categoryCard.add(createCategoryActionsRow());
        categoryCard.add(Box.createVerticalStrut(14));
        categoryCard.add(createCategorySeparator());
        categoryCard.add(Box.createVerticalStrut(14));
        categoryCard.add(createAddCategoryPanel(categoryList));
        categoryCard.add(Box.createVerticalStrut(18));
        categoryCard.add(createCategoryListTitle());
        categoryCard.add(Box.createVerticalStrut(10));
        categoryCard.add(createCategoryListScroll(categoryList));
        categoryCard.add(Box.createVerticalStrut(14));
        return categoryCard;
    }

    private JPanel createCategoryActionsRow() {
        JPanel actions = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);

        JButton settingsFileButton = createStyledButton(
                UnicodeSymbols.FOLDER + " Kategorien-Datei öffnen",
                ThemeManager.getSecondaryColor());
        settingsFileButton.setToolTipText("Öffnet categories.json im Standard-Editor");
        settingsFileButton.addActionListener(e -> openCategorySettingsFile());
        actions.add(settingsFileButton);

        JButton reloadButton = createStyledButton(
                UnicodeSymbols.REFRESH + " Kategorien neu laden",
                ThemeManager.getAccentColor());
        reloadButton.setToolTipText("Lädt categories.json neu ein (ohne Neustart)");
        reloadButton.addActionListener(e -> {
            loadCategories();
            showTimedInfo("Kategorien", "Kategorien wurden neu geladen.");
        });
        actions.add(reloadButton);
        return actions;
    }

    private JSeparator createCategorySeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(ThemeManager.getBorderColor());
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);
        return separator;
    }

    private JPanel createAddCategoryPanel(JList<String> categoryList) {
        JLabel titleLabel = new JLabel("Neue Kategorie hinzufügen");
        titleLabel.setFont(getFontByName(Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hintLabel = new JLabel("<html><div style='padding-top:2px;'><span style='font-size:11px;'>Tipp: Drücken Sie Enter im Namen-Feld, um hinzuzufügen. Z.b von 1000 bis 1999</span></div></html>");
        hintLabel.setFont(getFontByName(Font.PLAIN, 12));
        hintLabel.setForeground(ThemeManager.getTextSecondaryColor());
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField categoryNameField = createCategoryNameField();
        NumberFormatter formatter = createIntegerFormatter();
        JFormattedTextField fromRange = createCategoryRangeField(formatter, "Von (z.B. 1000)");
        JFormattedTextField toRange = createCategoryRangeField(formatter, "Bis (z.B. 1999)");
        JButton addButton = createCategoryAddButton();
        Runnable addAction = createCategoryAddAction(categoryList, categoryNameField, fromRange, toRange, addButton);

        installCategoryAddButtonState(categoryNameField, fromRange, toRange, addButton);
        addButton.addActionListener(e -> addAction.run());
        categoryNameField.addActionListener(e -> {
            if (addButton.isEnabled()) {
                addAction.run();
            }
        });

        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));
        outerPanel.setOpaque(false);
        outerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        outerPanel.add(titleLabel);
        outerPanel.add(Box.createVerticalStrut(4));
        outerPanel.add(hintLabel);
        outerPanel.add(Box.createVerticalStrut(10));
        outerPanel.add(createCategoryAddFormPanel(categoryNameField, fromRange, toRange, addButton));
        return outerPanel;
    }

    private JTextField createCategoryNameField() {
        JTextField field = new JTextField();
        styleInputField(field);
        field.setToolTipText("Kategoriename (z.B. 'Elektronik')");
        return field;
    }

    private NumberFormatter createIntegerFormatter() {
        NumberFormat intFormat = NumberFormat.getIntegerInstance();
        intFormat.setGroupingUsed(false);
        NumberFormatter formatter = new NumberFormatter(intFormat);
        formatter.setValueClass(Integer.class);
        formatter.setAllowsInvalid(true);
        formatter.setCommitsOnValidEdit(true);
        formatter.setMinimum(Integer.MIN_VALUE);
        formatter.setMaximum(Integer.MAX_VALUE);
        return formatter;
    }

    private JFormattedTextField createCategoryRangeField(NumberFormatter formatter, String tooltip) {
        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(6);
        field.setToolTipText(tooltip);
        field.setFont(getFontByName(Font.PLAIN, 13));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    private JButton createCategoryAddButton() {
        JButton button = createStyledButton(UnicodeSymbols.PLUS + " Hinzufügen", ThemeManager.getSuccessColor());
        button.setToolTipText("Fügt die Kategorie hinzu");
        button.setEnabled(false);
        button.setPreferredSize(new Dimension(160, 36));
        return button;
    }

    private Runnable createCategoryAddAction(JList<String> categoryList, JTextField categoryNameField,
                                             JFormattedTextField fromRange, JFormattedTextField toRange,
                                             JButton addCategoryButton) {
        return () -> handleCategoryAdd(categoryList, categoryNameField, fromRange, toRange, addCategoryButton);
    }

    private void handleCategoryAdd(JList<String> categoryList, JTextField categoryNameField,
                                   JFormattedTextField fromRange, JFormattedTextField toRange,
                                   JButton addCategoryButton) {
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

        if (!applyCategoryRange(newCategory, fromTxt, toTxt, categoryList)) {
            return;
        }

        categoryNameField.setText("");
        fromRange.setValue(null);
        toRange.setValue(null);
        addCategoryButton.setEnabled(false);
        categoryNameField.requestFocusInWindow();
    }

    private boolean applyCategoryRange(String categoryName, String fromTxt, String toTxt, JList<String> categoryList) {
        if (fromTxt.isEmpty() && toTxt.isEmpty()) {
            addNewCategory(categoryName, Integer.MIN_VALUE, Integer.MAX_VALUE);
            loadCategoriesIntoList(categoryList);
            return true;
        }

        try {
            int from = fromTxt.isEmpty() ? Integer.MIN_VALUE : Integer.parseInt(fromTxt);
            int to = toTxt.isEmpty() ? Integer.MAX_VALUE : Integer.parseInt(toTxt);
            if (from > to) {
                throw new NumberFormatException();
            }
            addNewCategory(categoryName, from, to);
            loadCategoriesIntoList(categoryList);
            return true;
        } catch (NumberFormatException ex) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Ungültiger Bereich (Von/Bis). Bitte ganze Zahlen eingeben und Von ≤ Bis.")
                    .setDuration(5000)
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return false;
        }
    }

    private void installCategoryAddButtonState(JTextField categoryNameField, JFormattedTextField fromRange,
                                               JFormattedTextField toRange, JButton addCategoryButton) {
        categoryNameField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateState() {
                addCategoryButton.setEnabled(hasValidCategoryInput(categoryNameField, fromRange, toRange));
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateState();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateState();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateState();
            }
        });
    }

    private boolean hasValidCategoryInput(JTextField categoryNameField, JFormattedTextField fromRange,
                                          JFormattedTextField toRange) {
        String name = categoryNameField.getText() == null ? "" : categoryNameField.getText().trim();
        if (name.isEmpty()) {
            return false;
        }
        Object fromObj = fromRange.getValue();
        Object toObj = toRange.getValue();
        if (fromObj instanceof Number from && toObj instanceof Number to) {
            return from.intValue() <= to.intValue();
        }
        return true;
    }

    private JPanel createCategoryAddFormPanel(JTextField categoryNameField, JFormattedTextField fromRange,
                                              JFormattedTextField toRange, JButton addCategoryButton) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(createCategoryAddLabelsPanel());
        panel.add(Box.createHorizontalStrut(12));
        panel.add(createCategoryAddFieldsPanel(categoryNameField, fromRange, toRange));
        panel.add(Box.createHorizontalStrut(12));
        panel.add(createCategoryAddButtonPanel(addCategoryButton));
        return panel;
    }

    private JPanel createCategoryAddLabelsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        panel.setMaximumSize(new Dimension(140, Integer.MAX_VALUE));
        panel.add(createCategoryFieldLabel("Kategoriename", Font.BOLD, 13, ThemeManager.getTextPrimaryColor()));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createCategoryFieldLabel("Von", Font.PLAIN, 11, ThemeManager.getTextSecondaryColor()));
        panel.add(Box.createVerticalStrut(8));
        panel.add(createCategoryFieldLabel("Bis", Font.PLAIN, 11, ThemeManager.getTextSecondaryColor()));
        return panel;
    }

    private JLabel createCategoryFieldLabel(String text, int style, int size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(getFontByName(style, size));
        label.setForeground(color);
        return label;
    }

    private JPanel createCategoryAddFieldsPanel(JTextField categoryNameField, JFormattedTextField fromRange,
                                                JFormattedTextField toRange) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        panel.setMaximumSize(new Dimension(220, Integer.MAX_VALUE));
        panel.add(categoryNameField);
        panel.add(Box.createVerticalStrut(8));
        panel.add(fromRange);
        panel.add(Box.createVerticalStrut(8));
        panel.add(toRange);
        return panel;
    }

    private JPanel createCategoryAddButtonPanel(JButton addCategoryButton) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentY(Component.TOP_ALIGNMENT);
        panel.setMaximumSize(new Dimension(180, Integer.MAX_VALUE));
        panel.add(addCategoryButton);
        return panel;
    }

    private JLabel createCategoryListTitle() {
        JLabel label = new JLabel("Aktuelle Kategorien");
        label.setFont(getFontByName(Font.BOLD, 18));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JList<String> createCategoryList() {
        JList<String> categoryList = new JList<>();
        categoryList.setFont(getFontByName(Font.PLAIN, 15));
        categoryList.setBackground(ThemeManager.getInputBackgroundColor());
        categoryList.setForeground(ThemeManager.getTextPrimaryColor());
        categoryList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        categoryList.setAlignmentX(Component.LEFT_ALIGNMENT);
        loadCategoriesIntoList(categoryList);
        return categoryList;
    }

    private JScrollPane createCategoryListScroll(JList<String> categoryList) {
        JScrollPane scrollPane = new JScrollPane(categoryList);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
    }

    private JPanel createOtherSettingsCard() {
        JPanel card = createStandardSectionCard("Sonstiges", "Allgemeine Einstellungen", null);
        card.add(Box.createVerticalStrut(18));
        addOtherSettingsEntry(card,
                "Öffnet den Ordner, in dem die Einstellungsdateien gespeichert sind.",
                createStyledButton(UnicodeSymbols.FOLDER + " Einstellungen-Ordner öffnen", ThemeManager.getSecondaryColor()),
                this::openSettingsFolder);
        addOtherSettingsEntry(card,
                "Öffnet den Ordner, in dem die Anwendungsprotokolle gespeichert sind.",
                createStyledButton(UnicodeSymbols.FOLDER + " Protokolle-Ordner öffnen", ThemeManager.getSecondaryColor()),
                this::openLogsFolder);
        addOtherSettingsEntry(card,
                "Löscht alle Anwendungsprotokolle aus dem Protokolle-Ordner. So wie in der Datenbank.",
                createStyledButton(UnicodeSymbols.TRASH + " Alle Protokolle löschen", ThemeManager.getDangerColor()),
                this::deleteAllLogs);

        JLabel deleteOldLogsInfo = createInfoLabel("Löscht alle Anwendungsprotokolle, die älter als 30 Tage sind, aus dem Protokolle-Ordner. Per Option kann die Bereinigung auch automatisch beim Start ausgeführt werden. So wie in der Datenbank.");
        deleteOldLogsInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(15));
        card.add(deleteOldLogsInfo);

        deleteOldLogsCheckBox = new JCheckBox("Beim Start alte Protokolle (älter als 30 Tage) automatisch löschen");
        styleCheckbox(deleteOldLogsCheckBox);
        deleteOldLogsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(10));
        card.add(deleteOldLogsCheckBox);

        JButton deleteOldLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alte Protokolle löschen", ThemeManager.getDangerColor());
        deleteOldLogsButton.addActionListener(e -> deleteOldLogs());
        deleteOldLogsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(Box.createVerticalStrut(10));
        card.add(deleteOldLogsButton);
        return card;
    }

    private void addOtherSettingsEntry(JPanel card, String infoText, JButton button, Runnable action) {
        JLabel infoLabel = createInfoLabel(infoText);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(infoLabel);
        card.add(Box.createVerticalStrut(10));
        button.addActionListener(e -> action.run());
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(button);
        card.add(Box.createVerticalStrut(15));
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
        JPanel appearancePanel = createTabContentPanel();
        addTabSection(appearancePanel, createFontSettingsCard(), 22);
        addTabSection(appearancePanel, createTableSettingsCard(), 22);
        addTabSection(appearancePanel, createThemeSettingsCard(), 22);
        addTabSection(appearancePanel, createColorSettingsCard(), 22);
        addTabSection(appearancePanel, createHeaderSettingsCard(), 12);
        addTabSection(appearancePanel, createPreviewSettingsCard(), 0);
        addScrollableTab(tabbedPane, UnicodeSymbols.COLOR_PALETTE + " Darstellung", appearancePanel);
    }

    private JPanel createFontSettingsCard() {
        JPanel fontCard = createStandardSectionCard(
                UnicodeSymbols.ABC + " Schriftart",
                "Schriftart für die Anwendung festlegen",
                this::resetFontDefaults);

        JLabel introLabel = new JLabel("<html><div style='padding:2px 0;'><b>Darstellung</b><br/><span style='font-size:11px;'>Änderungen werden nach dem Speichern übernommen. Die Vorschau aktualisiert sich automatisch.</span></div></html>");
        introLabel.setFont(getFontByName(Font.PLAIN, 12));
        introLabel.setForeground(ThemeManager.getTextSecondaryColor());
        introLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel infoLabel = new JLabel("<html><div><span style='font-size: 11px;'>Wählen Sie eine Schriftart. Die Vorschau unten zeigt das Ergebnis.</span></div></html>");
        infoLabel.setFont(getFontByName(Font.PLAIN, 12));
        infoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        fontComboBox = new JComboBox<>();
        getAllFonts().forEach(fontComboBox::addItem);
        styleComboBox(fontComboBox);
        fontComboBox.addActionListener(e -> updatePreview());

        JPanel selectionPanel = createLabeledComboBoxPanel("Schriftart auswählen:", fontComboBox);
        selectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSample.setFont(getFontByName(Font.PLAIN, 16));
        fontSample.setForeground(ThemeManager.getTextPrimaryColor());
        fontSample.setAlignmentX(Component.LEFT_ALIGNMENT);

        fontCard.add(Box.createVerticalStrut(12));
        fontCard.add(introLabel);
        fontCard.add(Box.createVerticalStrut(12));
        fontCard.add(infoLabel);
        fontCard.add(Box.createVerticalStrut(10));
        fontCard.add(selectionPanel);
        fontCard.add(Box.createVerticalStrut(12));
        fontCard.add(fontSample);
        return fontCard;
    }

    private JPanel createTableSettingsCard() {
        JPanel tableCard = createStandardSectionCard(
                UnicodeSymbols.CLIPBOARD + " Tabellen & Tabs",
                "Schriftgröße für Tabellen und Tabs anpassen",
                this::resetTableDefaults);

        fontSizeTableSpinner = new JSpinner(new SpinnerNumberModel(16, 10, 44, 1));
        JPanel tableSizePanel = createLabeledSpinnerPanel("Tabellen-Schriftgröße:", fontSizeTableSpinner, "px", 4);
        tableSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        configureSlider(tableFontSlider, fontSizeTableSpinner, tableFontSample);
        tableFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);

        fontSizeTabSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 40, 1));
        JPanel tabSizePanel = createLabeledSpinnerPanel("Tabs-Schriftgröße:", fontSizeTabSpinner, "px", 4);
        tabSizePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        configureSlider(tabFontSlider, fontSizeTabSpinner, tabFontSample);
        tabFontSample.setAlignmentX(Component.LEFT_ALIGNMENT);

        fontSizeTableSpinner.addChangeListener(e -> updatePreview());
        fontSizeTabSpinner.addChangeListener(e -> updatePreview());

        tableCard.add(Box.createVerticalStrut(12));
        addSliderSetting(tableCard, tableSizePanel, tableFontSlider, tableFontSample);
        tableCard.add(Box.createVerticalStrut(14));
        addSliderSetting(tableCard, tabSizePanel, tabFontSlider, tabFontSample);
        return tableCard;
    }

    private void addSliderSetting(JPanel card, JPanel labelPanel, JSlider slider, JLabel sampleLabel) {
        labelPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        sampleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(labelPanel);
        card.add(Box.createVerticalStrut(8));
        card.add(slider);
        card.add(Box.createVerticalStrut(6));
        card.add(sampleLabel);
    }

    private JPanel createThemeSettingsCard() {
        JPanel themeCard = createStandardSectionCard(
                UnicodeSymbols.COLOR_PALETTE + " Design & Darstellung",
                "Passen Sie das Erscheinungsbild der Anwendung an",
                this::resetThemeDefaults);

        darkModeCheckbox = new JCheckBox("Dark Mode aktivieren");
        styleCheckbox(darkModeCheckbox);
        darkModeCheckbox.setSelected(ThemeManager.isDarkMode());
        darkModeCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] themes = { "Light", "Dark" };
        themeComboBox = new JComboBox<>(themes);
        styleComboBox(themeComboBox);
        themeComboBox.setSelectedItem(ThemeManager.isDarkMode() ? "Dark" : "Light");
        JPanel themeSelectionPanel = createLabeledComboBoxPanel("Design-Schema:", themeComboBox);
        themeSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        installThemeControlBindings();

        JLabel lookAndFeelInfo = new JLabel("Look & Feel: " + ThemeManager.getCurrentLookAndFeelName());
        lookAndFeelInfo.setFont(getFontByName(Font.PLAIN, 12));
        lookAndFeelInfo.setForeground(ThemeManager.getTextSecondaryColor());
        lookAndFeelInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JList<String> lafList = createLookAndFeelList();
        JScrollPane lafScroll = new JScrollPane(lafList);
        lafScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton applyLafButton = createApplyLookAndFeelButton(lafList);
        JLabel themeInfoLabel = new JLabel("<html><div style='padding: 6px 0;'><i>Hinweis: Nach dem Speichern werden alle Fenster mit dem neuen Design aktualisiert.<br/>Der Dark Mode schont die Augen bei Arbeiten in dunkler Umgebung.</i></div></html>");
        themeInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        themeInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        themeInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        themeCard.add(Box.createVerticalStrut(16));
        themeCard.add(darkModeCheckbox);
        themeCard.add(Box.createVerticalStrut(12));
        themeCard.add(themeSelectionPanel);
        themeCard.add(Box.createVerticalStrut(12));
        themeCard.add(lookAndFeelInfo);
        themeCard.add(Box.createVerticalStrut(10));
        themeCard.add(lafScroll);
        themeCard.add(Box.createVerticalStrut(10));
        themeCard.add(applyLafButton);
        themeCard.add(themeInfoLabel);
        return themeCard;
    }

    private void installThemeControlBindings() {
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
    }

    private JList<String> createLookAndFeelList() {
        JList<String> lafList = new JList<>();
        lafList.setListData(ThemeManager.getAllLookAndFeels().toArray(new String[0]));
        lafList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lafList.setSelectedValue(ThemeManager.getCurrentLookAndFeelName(), false);
        lafList.setFont(getFontByName(Font.PLAIN, 13));
        lafList.setBackground(ThemeManager.getInputBackgroundColor());
        lafList.setForeground(ThemeManager.getTextPrimaryColor());
        lafList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        lafList.setAlignmentX(Component.LEFT_ALIGNMENT);
        lafList.setVisibleRowCount(5);
        return lafList;
    }

    private JButton createApplyLookAndFeelButton(JList<String> lafList) {
        JButton button = createStyledButton(UnicodeSymbols.CHECK + " Look & Feel anwenden", ThemeManager.getSuccessColor());
        button.setToolTipText("Wendet das ausgewählte Look & Feel an (nur in der Vorschau)");
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.addActionListener(e -> applySelectedLookAndFeel(lafList));
        return button;
    }

    private void applySelectedLookAndFeel(JList<String> lafList) {
        String selectedLaf = lafList.getSelectedValue();
        if (selectedLaf == null) {
            new MessageDialog()
                    .setTitle("Hinweis")
                    .setMessage("Bitte wählen Sie ein Look & Feel aus der Liste aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }
        ThemeManager.setLookAndFeel(selectedLaf);
        updatePreview();
        Main.settings.setProperty("look_and_feel", selectedLaf);
        Main.settings.save();
    }

    private JPanel createColorSettingsCard() {
        JPanel colorCard = createStandardSectionCard(
                UnicodeSymbols.COLOR_PALETTE + " Farbthemen",
                "Akzent-, Header- und Buttonfarbe anpassen",
                this::resetColorDefaults);
        colorCard.add(Box.createVerticalStrut(12));
        colorCard.add(createColorRow("Akzentfarbe:", "theme_accent_color"));
        colorCard.add(Box.createVerticalStrut(10));
        colorCard.add(createColorRow("Headerfarbe:", "theme_header_color"));
        colorCard.add(Box.createVerticalStrut(10));
        colorCard.add(createColorRow("Buttonfarbe:", "theme_button_color"));
        return colorCard;
    }

    private JPanel createPreviewSettingsCard() {
        JPanel previewCard = createStandardSectionCard(
                UnicodeSymbols.BULB + " Live Vorschau",
                "Vorschau auf Schrift und Farben basierend auf Ihren Einstellungen",
                null);
        previewCard.add(Box.createVerticalStrut(12));
        JComponent previewPanelComponent = createPreviewPanel();
        previewPanelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
        previewCard.add(previewPanelComponent);
        return previewCard;
    }

    private JPanel createHeaderSettingsCard() {
        JPanel headerCard = createStandardSectionCard(
                UnicodeSymbols.INFO + " Header-Einstellungen",
                "Optionen zur Anpassung der Header-Panels",
                null);
        headerCard.setOpaque(true);
        headerCard.add(createInfoLabel("Deaktivieren der Header-Panels"));
        headerCard.add(Box.createVerticalStrut(8));
        headerCard.add(createDisableHeaderCheckbox());
        return headerCard;
    }

    private JCheckBox createDisableHeaderCheckbox() {
        boolean disable = "true".equalsIgnoreCase(Main.settings.getProperty("disable_header"));
        JCheckBox checkbox = new JCheckBox("Header-Panels deaktivieren (entfernt die farbigen Header-Balken) (" + (disable ? "Deaktiviert" : "Aktiviert") + ")");
        styleCheckbox(checkbox);
        checkbox.setSelected(disable);
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.addActionListener(e -> {
            boolean disableHeader = checkbox.isSelected();
            Main.settings.setProperty("disable_header", Boolean.toString(disableHeader));
            Main.settings.save();
            ThemeManager.getInstance().updateAllWindows();
        });
        return checkbox;
    }

    private void buildConnectionTab(JTabbedPane tabbedPane) {
        JPanel connectionPanel = createTabContentPanel();
        addTabSection(connectionPanel, createServerConnectionCard(), 0);
        connectionPanel.add(Box.createVerticalGlue());
        addScrollableTab(tabbedPane, UnicodeSymbols.GLOBE + " Verbindung", connectionPanel);
    }

    private void buildDatabaseTab(JTabbedPane tabbedPane) {
        JPanel databasePanel = createTabContentPanel();
        addTabSection(databasePanel, createDatabaseBackendCard(), 22);
        addTabSection(databasePanel, createDatabaseMigrationCard(), 18);
        addTabSection(databasePanel, createDatabaseManagementCard(), 0);
        databasePanel.add(Box.createVerticalGlue());
        addScrollableTab(tabbedPane, UnicodeSymbols.FLOPPY + " Datenbank", databasePanel);
    }

    private JPanel createDatabaseBackendCard() {
        JPanel backendCard = createStandardSectionCard(
                UnicodeSymbols.FLOPPY + " Speicher-Backend",
                "Wählen Sie aus, ob die App SQLite, H2, JSON oder YAML verwenden soll",
                this::resetDatabaseDefaults);

        backendCard.add(createInfoLabel(
                "<html><div style='width:650px;'>Das ausgewählte Backend wird beim nächsten Start verwendet. " +
                        "<b>SQLite</b> und <b>H2</b> bleiben dateibasierte Datenbanken. " +
                        "<b>JSON</b> und <b>YAML</b> speichern Tabellen als Dateien im App-Datenordner. " +
                        "Ein Backend-Wechsel migriert vorhandene Daten nicht automatisch.</div></html>"));
        backendCard.add(Box.createVerticalStrut(12));

        databaseTypeComboBox = new JComboBox<>(DatabaseManager.DatabaseType.values());
        styleComboBox(databaseTypeComboBox);
        databaseTypeComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = "";
                if (value instanceof DatabaseManager.DatabaseType type) {
                    text = type.getDisplayName() + " (" + type.getConfigValue() + ")";
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        databaseTypeComboBox.addActionListener(e -> {
            updateDatabaseBackendHint();
            updateDatabaseMigrationHint();
        });

        JPanel selectionPanel = createLabeledComboBoxPanel("Speicher-Backend:", databaseTypeComboBox);
        selectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        backendCard.add(selectionPanel);
        backendCard.add(Box.createVerticalStrut(10));

        databaseBackendHintLabel = createInfoLabel("");
        databaseBackendHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        backendCard.add(databaseBackendHintLabel);
        updateDatabaseBackendHint();
        return backendCard;
    }

    private void updateDatabaseBackendHint() {
        if (databaseBackendHintLabel == null) {
            return;
        }

        DatabaseManager.DatabaseType selectedType = getSelectedDatabaseType();
        String storageMode = switch (selectedType) {
            case SQLITE, H2 -> "Dateibasierte Datenbank";
            case JSON, YAML -> "Dateisystem-Speicher";
        };
        databaseBackendHintLabel.setText(
                "<html><div style='width:650px;'><b>" + selectedType.getDisplayName() + "</b> - " + storageMode +
                        "<br/>Ablage: <code>" + describeDatabaseStorage(selectedType) + "</code>" +
                        "<br/><i>Hinweis:</i> Bereits gespeicherte Daten bleiben im bisherigen Backend, bis sie separat migriert werden.</div></html>");
    }

    private DatabaseManager.DatabaseType getSelectedDatabaseType() {
        if (databaseTypeComboBox == null || databaseTypeComboBox.getSelectedItem() == null) {
            return DEFAULT_DATABASE_TYPE;
        }
        return (DatabaseManager.DatabaseType) databaseTypeComboBox.getSelectedItem();
    }

    private JPanel createDatabaseMigrationCard() {
        JPanel migrationCard = createStandardSectionCard(
                UnicodeSymbols.REFRESH + " Daten migrieren",
                "Kopiert den Datenbestand von einem Quell-Backend in das aktuell ausgewählte Ziel-Backend",
                null);

        migrationCard.add(createInfoLabel(
                "<html><div style='width:650px;'>Die Migration kopiert alle bekannten Anwendungstabellen "
                        + "in das oben ausgewählte Ziel-Backend und <b>überschreibt</b> den dortigen Datenbestand. "
                        + "Die Einstellung <b>Speicher-Backend</b> wird dadurch nicht automatisch gespeichert.</div></html>"));
        migrationCard.add(Box.createVerticalStrut(12));

        migrationSourceComboBox = new JComboBox<>(DatabaseManager.DatabaseType.values());
        styleComboBox(migrationSourceComboBox);
        migrationSourceComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                String text = "";
                if (value instanceof DatabaseManager.DatabaseType type) {
                    text = type.getDisplayName() + " (" + type.getConfigValue() + ")";
                }
                return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
            }
        });
        DatabaseManager.DatabaseType activeType = Main.databaseManager != null
                ? Main.databaseManager.getDatabaseType()
                : DEFAULT_DATABASE_TYPE;
        migrationSourceComboBox.setSelectedItem(activeType);
        migrationSourceComboBox.addActionListener(e -> updateDatabaseMigrationHint());

        JPanel sourcePanel = createLabeledComboBoxPanel("Quelle:", migrationSourceComboBox);
        sourcePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        migrationCard.add(sourcePanel);
        migrationCard.add(Box.createVerticalStrut(10));

        databaseMigrationHintLabel = createInfoLabel("");
        databaseMigrationHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        migrationCard.add(databaseMigrationHintLabel);
        migrationCard.add(Box.createVerticalStrut(12));

        JPanel actionRow = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        migrateDatabaseButton = createStyledButton(UnicodeSymbols.REFRESH + " Daten migrieren", ThemeManager.getSuccessColor());
        migrateDatabaseButton.setToolTipText("Kopiert alle Anwendungstabellen in das ausgewählte Ziel-Backend");
        migrateDatabaseButton.addActionListener(e -> startDatabaseMigration());
        actionRow.add(migrateDatabaseButton);
        migrationCard.add(actionRow);

        updateDatabaseMigrationHint();
        return migrationCard;
    }

    private void updateDatabaseMigrationHint() {
        if (databaseMigrationHintLabel == null) {
            return;
        }

        DatabaseManager.DatabaseType sourceType = getSelectedMigrationSourceType();
        DatabaseManager.DatabaseType targetType = getSelectedDatabaseType();
        boolean identical = sourceType == targetType;
        boolean targetIsActive = Main.databaseManager != null && Main.databaseManager.getDatabaseType() == targetType;

        if (migrateDatabaseButton != null) {
            migrateDatabaseButton.setEnabled(!identical);
        }

        String targetNote = targetIsActive
                ? "Das Ziel ist aktuell aktiv. Ein Neustart wird nach der Migration empfohlen."
                : "Nach der Migration bitte das Ziel-Backend speichern und die App neu starten.";

        databaseMigrationHintLabel.setText(
                "<html><div style='width:650px;'><b>Quelle:</b> " + sourceType.getDisplayName()
                        + "<br/><code>" + describeDatabaseStorage(sourceType) + "</code>"
                        + "<br/><b>Ziel:</b> " + targetType.getDisplayName()
                        + "<br/><code>" + describeDatabaseStorage(targetType) + "</code>"
                        + "<br/><i>Hinweis:</i> "
                        + (identical
                        ? "Bitte unterschiedliche Backends für Quelle und Ziel auswählen."
                        : targetNote)
                        + "</div></html>");
    }

    private DatabaseManager.DatabaseType getSelectedMigrationSourceType() {
        if (migrationSourceComboBox == null || migrationSourceComboBox.getSelectedItem() == null) {
            return DEFAULT_DATABASE_TYPE;
        }
        return (DatabaseManager.DatabaseType) migrationSourceComboBox.getSelectedItem();
    }

    private String describeDatabaseStorage(DatabaseManager.DatabaseType databaseType) {
        String configuredFileName = resolveDatabaseFileNameFor(databaseType);
        File appDataDir = Main.getAppDataDir();
        if (databaseType == DatabaseManager.DatabaseType.SQLITE) {
            return new File(appDataDir, configuredFileName).getAbsolutePath();
        }

        String normalizedName = normalizeDatabaseBaseName(configuredFileName);
        if (databaseType == DatabaseManager.DatabaseType.H2) {
            return new File(appDataDir, normalizedName + ".mv.db").getAbsolutePath();
        }

        String suffix = databaseType == DatabaseManager.DatabaseType.JSON ? "_json" : "_yaml";
        String extension = databaseType == DatabaseManager.DatabaseType.JSON ? "*.json" : "*.yaml";
        return new File(new File(appDataDir, normalizedName + suffix), "tables").getAbsolutePath()
                + File.separator + extension;
    }

    private String resolveDatabaseFileNameFor(DatabaseManager.DatabaseType databaseType) {
        String configured = Main.settings.getProperty("database_file");
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        return DatabaseManager.getDefaultFileName(databaseType);
    }

    private String normalizeDatabaseBaseName(String fileName) {
        String normalized = (fileName == null || fileName.isBlank())
                ? DatabaseManager.getDefaultFileName(DatabaseManager.DatabaseType.H2)
                : fileName.trim();
        normalized = stripSuffixIgnoreCase(normalized, ".mv.db");
        normalized = stripSuffixIgnoreCase(normalized, ".db");
        normalized = stripSuffixIgnoreCase(normalized, ".json");
        normalized = stripSuffixIgnoreCase(normalized, ".yaml");
        normalized = stripSuffixIgnoreCase(normalized, ".yml");
        return normalized;
    }

    private String stripSuffixIgnoreCase(String value, String suffix) {
        if (value == null || suffix == null) {
            return value;
        }
        return value.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))
                ? value.substring(0, value.length() - suffix.length())
                : value;
    }

    private void startDatabaseMigration() {
        DatabaseManager.DatabaseType sourceType = getSelectedMigrationSourceType();
        DatabaseManager.DatabaseType targetType = getSelectedDatabaseType();
        if (sourceType == targetType) {
            new MessageDialog()
                    .setTitle("Migration nicht möglich")
                    .setMessage("Quelle und Ziel müssen unterschiedlich sein.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        int confirm = new MessageDialog()
                .setTitle("Migration bestätigen")
                .setMessage("<html><b>Daten wirklich migrieren?</b><br/><br/>"
                        + "Quelle: <b>" + sourceType.getDisplayName() + "</b><br/>"
                        + describeDatabaseStorage(sourceType) + "<br/><br/>"
                        + "Ziel: <b>" + targetType.getDisplayName() + "</b><br/>"
                        + describeDatabaseStorage(targetType) + "<br/><br/>"
                        + "Der aktuelle Datenbestand im Ziel wird ersetzt.</html>")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (migrateDatabaseButton != null) {
            migrateDatabaseButton.setEnabled(false);
        }
        if (migrationSourceComboBox != null) {
            migrationSourceComboBox.setEnabled(false);
        }
        String originalText = migrateDatabaseButton == null ? "" : migrateDatabaseButton.getText();
        if (migrateDatabaseButton != null) {
            migrateDatabaseButton.setText(UnicodeSymbols.REFRESH + " Migriert...");
        }
        setMaintenanceBusy(true);

        SwingWorker<DatabaseManager.MigrationSummary, Void> worker = new SwingWorker<>() {
            @Override
            protected DatabaseManager.MigrationSummary doInBackground() {
                return performDatabaseMigration(sourceType, targetType);
            }

            @Override
            protected void done() {
                if (migrateDatabaseButton != null) {
                    migrateDatabaseButton.setEnabled(sourceType != targetType);
                    migrateDatabaseButton.setText(originalText);
                }
                if (migrationSourceComboBox != null) {
                    migrationSourceComboBox.setEnabled(true);
                }
                setMaintenanceBusy(false);
                try {
                    DatabaseManager.MigrationSummary summary = get();
                    new MessageDialog()
                            .setTitle("Migration erfolgreich")
                            .setMessage(buildDatabaseMigrationSuccessMessage(summary))
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .display();
                } catch (Exception ex) {
                    logger.error("Database migration failed", ex);
                    new MessageDialog()
                            .setTitle("Migration fehlgeschlagen")
                            .setMessage("Die Datenmigration konnte nicht abgeschlossen werden: "
                                    + getReadableExceptionMessage(ex))
                            .setDuration(7000)
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                } finally {
                    updateDatabaseMigrationHint();
                }
            }
        };
        worker.execute();
    }

    private DatabaseManager.MigrationSummary performDatabaseMigration(DatabaseManager.DatabaseType sourceType,
                                                                      DatabaseManager.DatabaseType targetType) {
        List<DatabaseManager> temporaryManagers = new ArrayList<>();
        try {
            DatabaseManager sourceManager = resolveMigrationManager(sourceType, temporaryManagers);
            DatabaseManager targetManager = resolveMigrationManager(targetType, temporaryManagers);
            return sourceManager.migrateDataTo(targetManager);
        } finally {
            for (DatabaseManager manager : temporaryManagers) {
                try {
                    manager.close();
                } catch (Exception closeException) {
                    logger.warn("Temporary migration manager could not be closed cleanly", closeException);
                }
            }
        }
    }

    private DatabaseManager resolveMigrationManager(DatabaseManager.DatabaseType databaseType,
                                                    List<DatabaseManager> temporaryManagers) {
        if (Main.databaseManager != null && Main.databaseManager.getDatabaseType() == databaseType) {
            return Main.databaseManager;
        }

        DatabaseManager manager = new DatabaseManager(
                databaseType,
                Main.getAppDataDir().getAbsolutePath(),
                resolveDatabaseFileNameFor(databaseType));
        temporaryManagers.add(manager);
        return manager;
    }

    private String buildDatabaseMigrationSuccessMessage(DatabaseManager.MigrationSummary summary) {
        StringBuilder builder = new StringBuilder("<html><b>")
                .append(UnicodeSymbols.CHECKMARK)
                .append(" Migration erfolgreich!</b><br/><br/>")
                .append("Quelle: ")
                .append(summary.getSourceType().getDisplayName())
                .append("<br/>Ziel: ")
                .append(summary.getTargetType().getDisplayName())
                .append("<br/>Tabellen verarbeitet: ")
                .append(summary.getTableCount())
                .append("<br/>Zeilen kopiert: ")
                .append(summary.getRowCount());

        if (!summary.getMigratedRowsPerTable().isEmpty()) {
            builder.append("<br/><br/>Tabellen: ");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : summary.getMigratedRowsPerTable().entrySet()) {
                if (!first) {
                    builder.append(", ");
                }
                builder.append(entry.getKey()).append(" (").append(entry.getValue()).append(')');
                first = false;
            }
        }

        boolean targetIsActive = Main.databaseManager != null
                && Main.databaseManager.getDatabaseType() == summary.getTargetType();
        builder.append("<br/><br/><i>")
                .append(targetIsActive
                        ? "Das Ziel-Backend ist aktuell aktiv. Bitte starten Sie die App neu, damit alle Ansichten neu laden."
                        : "Wenn dieses Backend verwendet werden soll, speichern Sie die Einstellung und starten Sie die App neu.")
                .append("</i></html>");
        return builder.toString();
    }

    private void buildImportExportTab(JTabbedPane tabbedPane) {
        JPanel importExportPanel = createTabContentPanel();
        addTabSection(importExportPanel, createDataImportExportCard(), 25);
        addTabSection(importExportPanel, createSettingsProfileCard(), 0);
        importExportPanel.add(Box.createVerticalGlue());
        addScrollableTab(tabbedPane, UnicodeSymbols.DOWNLOAD + " Import/Export", importExportPanel);
    }

    private JPanel createServerConnectionCard() {
        JPanel serverCard = createStandardSectionCard(
                UnicodeSymbols.GLOBE + " Server-Verbindung",
                "URL des QR-Code Scan-Servers",
                null);

        serverCard.add(createServerConnectionTitle());
        serverCard.add(Box.createVerticalStrut(8));
        serverCard.add(createServerConnectionDescription());
        serverCard.add(Box.createVerticalStrut(14));
        serverCard.add(createInfoLabel("Tipp: Nutzen Sie https (wenn möglich). Mit den Aktionen können Sie die URL testen oder kopieren."));
        serverCard.add(Box.createVerticalStrut(10));

        JLabel urlStatusLabel = createServerUrlStatusLabel();
        Runnable validateUrl = () -> updateServerUrlStatus(urlStatusLabel);

        serverCard.add(createServerActionRow(validateUrl));
        serverCard.add(Box.createVerticalStrut(14));
        serverCard.add(createServerUrlPanel());
        serverCard.add(Box.createVerticalStrut(8));
        serverCard.add(urlStatusLabel);

        installServerUrlValidation(validateUrl);
        validateUrl.run();
        return serverCard;
    }

    private JLabel createServerConnectionTitle() {
        JLabel label = new JLabel(UnicodeSymbols.GLOBE + " Server-Verbindung");
        label.setFont(getFontByName(Font.BOLD, 22));
        label.setForeground(ThemeManager.getTitleTextColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createServerConnectionDescription() {
        JLabel label = new JLabel("Konfigurieren Sie die Server-URL für den QR-Code Scan-Service. Nutzen Sie https, wenn möglich.");
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createServerActionRow(Runnable validateUrl) {
        JPanel actions = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        JButton openUrlButton = createStyledButton(UnicodeSymbols.GLOBE + " Öffnen", ThemeManager.getSecondaryColor());
        openUrlButton.setToolTipText("Öffnet die URL im Browser");
        openUrlButton.addActionListener(e -> openServerUrl(validateUrl));

        JButton copyUrlButton = createStyledButton(UnicodeSymbols.CLIPBOARD + " Kopieren", ThemeManager.getAccentColor());
        copyUrlButton.setToolTipText("Kopiert die URL in die Zwischenablage");
        copyUrlButton.addActionListener(e -> copyServerUrl());

        JButton testUrlButton = createStyledButton(UnicodeSymbols.BULB + " Prüfen", ThemeManager.getSuccessColor());
        testUrlButton.setToolTipText("Prüft, ob die URL syntaktisch gültig ist");
        testUrlButton.addActionListener(e -> validateUrl.run());

        actions.add(openUrlButton);
        actions.add(copyUrlButton);
        actions.add(testUrlButton);
        return actions;
    }

    private JPanel createServerUrlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel("Server URL:");
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        serverUrlField = new JTextField(DEFAULT_SERVER_URL);
        styleInputField(serverUrlField);
        serverUrlField.setToolTipText("z.B. https://example.com/scans.json");
        serverUrlField.setPreferredSize(new Dimension(600, 40));
        serverUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        serverUrlField.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(serverUrlField);
        return panel;
    }

    private JLabel createServerUrlStatusLabel() {
        JLabel label = new JLabel(" ");
        label.setFont(getFontByName(Font.PLAIN, 12));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private void installServerUrlValidation(Runnable validateUrl) {
        serverUrlField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateStatus() {
                validateUrl.run();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateStatus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateStatus();
            }
        });
    }

    private void updateServerUrlStatus(JLabel urlStatusLabel) {
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
    }

    private void openServerUrl(Runnable validateUrl) {
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
    }

    private void copyServerUrl() {
        String raw = serverUrlField.getText() == null ? "" : serverUrlField.getText().trim();
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(raw), null);
        new MessageDialog()
                .setTitle("Zwischenablage")
                .setMessage("URL wurde kopiert.")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .setIcon(Main.iconSmall)
                .setDuration(1250)
                .display();
    }

    private JPanel createDatabaseManagementCard() {
        JPanel dbCard = createStandardSectionCard(
                UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung",
                "Datenbank bereinigen und Tabellen verwalten",
                null);
        dbCard.add(createDatabaseTitleLabel());
        dbCard.add(Box.createVerticalStrut(8));
        dbCard.add(createDatabaseDescriptionLabel());
        dbCard.add(Box.createVerticalStrut(18));
        dbCard.add(getDatabaseClearLabel());
        dbCard.add(Box.createVerticalStrut(15));
        dbCard.add(createDatabaseClearButtonRow());
        dbCard.add(Box.createVerticalStrut(18));
        dbCard.add(getSelectedDatabaseClear());
        dbCard.add(Box.createVerticalStrut(12));
        dbCard.add(createDeleteTableSection(dbCard));
        return dbCard;
    }

    private JLabel createDatabaseTitleLabel() {
        JLabel label = new JLabel(UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung");
        label.setFont(getFontByName(Font.BOLD, 22));
        label.setForeground(ThemeManager.getTitleTextColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JLabel createDatabaseDescriptionLabel() {
        JLabel label = new JLabel("Verwalten und bereinigen Sie Ihre Datenbank. Löschen Sie gezielt Tabellen oder führen Sie eine Komplettbereinigung durch.");
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createDatabaseClearButtonRow() {
        JPanel row = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        JButton clearButton = createStyledButton(UnicodeSymbols.TRASH + " Datenbank Bereinigen", ThemeManager.getDangerColor());
        clearButton.addActionListener(e -> clearDatabase());
        clearButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(clearButton);
        return row;
    }

    private JPanel createDeleteTableSection(JPanel parent) {
        List<String> tableNames = new ArrayList<>(DatabaseManager.ALLOWED_TABLES);
        JComboBox<String> tableComboBox = new JComboBox<>(tableNames.toArray(new String[0]));
        styleComboBox(tableComboBox);

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel selectionPanel = createLabeledComboBoxPanel("Tabelle auswählen:", tableComboBox);
        selectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(selectionPanel);
        container.add(Box.createVerticalStrut(14));

        JPanel actionRow = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        JButton deleteButton = createStyledButton(UnicodeSymbols.TRASH + " Ausgewählte Tabelle Löschen", ThemeManager.getWarningColor());
        deleteButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteButton.addActionListener(e -> confirmDeleteSelectedTable(tableComboBox));
        actionRow.add(deleteButton);
        container.add(actionRow);
        return container;
    }

    private void confirmDeleteSelectedTable(JComboBox<String> tableComboBox) {
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
        if (confirmDeleteTable(selectedTable)) {
            requestDeleteTableConfirmationInput(selectedTable);
        }
    }

    private boolean confirmDeleteTable(String selectedTable) {
        int confirm = JOptionPane.showConfirmDialog(this,
                String.format("<html><b>WARNUNG: Tabelle '%s' wirklich löschen?</b><br/><br/>" +
                        "Diese Aktion kann <b>NICHT</b> rückgängig gemacht werden!<br/>" +
                        "Alle Daten in der Tabelle '%s' werden permanent gelöscht.<br/><br/>" +
                        "Möchten Sie fortfahren?</html>", selectedTable, selectedTable),
                "Tabelle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return confirm == JOptionPane.YES_OPTION;
    }

    private void requestDeleteTableConfirmationInput(String selectedTable) {
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

    private JPanel createDataImportExportCard() {
        JPanel card = createStandardSectionCard(
                UnicodeSymbols.ARROW_UP + " " + UnicodeSymbols.ARROW_DOWN + " Import/Export",
                "Datenimport und -export Funktionen",
                null);
        card.add(Box.createVerticalStrut(15));
        addImportExportSection(card,
                SettingsUtils.createStyledLabel(logger, fontComboBox, "Import:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()),
                createInfoLabel("<html><div style='width:650px;'>Importieren Sie Artikel, Lieferanten, Kunden oder Abteilungen aus CSV-Dateien.<br/>Stellen Sie sicher, dass die CSV-Dateien das richtige Format haben.</div></html>"),
                createImportDatabaseButton());
        card.add(Box.createVerticalStrut(20));
        addImportExportSection(card,
                SettingsUtils.createStyledLabel(logger, fontComboBox, "Export:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()),
                createInfoLabel("<html><div style='width:650px;'>Exportieren Sie Ihre Datenbankinhalte in CSV-Dateien zur Sicherung oder Weiterverarbeitung.</div></html>"),
                createExportDatabaseButton());
        return card;
    }

    private void addImportExportSection(JPanel card, JLabel titleLabel, JLabel infoLabel, JButton actionButton) {
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(infoLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(actionButton);
    }

    private JButton createImportDatabaseButton() {
        JButton button = createStyledButton(UnicodeSymbols.ARROW_UP + " Datenbank Importieren", ThemeManager.getSuccessColor());
        button.addActionListener(e -> importFromCsv());
        return button;
    }

    private JButton createExportDatabaseButton() {
        JButton button = createStyledButton(UnicodeSymbols.ARROW_DOWN + " Datenbank Exportieren", ThemeManager.getSecondaryColor());
        button.addActionListener(e -> exportDatabaseWithConfirmation());
        return button;
    }

    private void exportDatabaseWithConfirmation() {
        String message = "<html><b>Datenbank nach CSV exportieren?</b><br/><br/>" +
                "Dies erstellt CSV-Dateien für:<br/>" +
                "- Artikel (articles_export.csv)<br/>" +
                "- Lieferanten (vendors_export.csv)<br/>" +
                "- Kunden (clients_export.csv)<br/>" +
                "- Bestellungen (orders_export.csv)<br/>" +
                "- Abteilungen (departments_export.csv)<br/><br/>" +
                "Speicherort: " + Main.getAppDataDir().getAbsolutePath() + "</html>";
        int confirm = new MessageDialog()
                .setTitle("Export bestätigen")
                .setMessage(message)
                .setMessageType(JOptionPane.QUESTION_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
        if (confirm == JOptionPane.YES_OPTION) {
            performCsvExport();
        }
    }

    private void performCsvExport() {
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
                return;
            }
            new MessageDialog()
                    .setTitle("Export erfolgreich")
                    .setMessage("<html><b>" + UnicodeSymbols.CHECKMARK + " Export erfolgreich!</b><br/><br/>" +
                            "Alle Tabellen wurden erfolgreich exportiert.<br/>" +
                            "Speicherort: <br/>" +
                            Main.getAppDataDir().getAbsolutePath() + "</html>")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } catch (Exception ex) {
            new MessageDialog()
                    .setTitle("Fehler beim Export")
                    .setMessage("<html><b>" + UnicodeSymbols.ERROR + " Fehler beim Export!</b><br/><br/>" +
                            ex.getMessage() + "</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    private JPanel createSettingsProfileCard() {
        JPanel card = createStandardSectionCard(
                UnicodeSymbols.FLOPPY + " Einstellungen Profil",
                "Importieren oder exportieren Sie Ihre Einstellungen als Profil-Datei",
                null);
        card.add(Box.createVerticalStrut(15));

        JButton exportButton = createStyledButton(UnicodeSymbols.DOWNLOAD + " Einstellungen exportieren", ThemeManager.getSecondaryColor());
        exportButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportButton.addActionListener(e -> exportSettingsProfile());
        card.add(exportButton);
        card.add(Box.createVerticalStrut(10));

        JButton importButton = createStyledButton(UnicodeSymbols.UPLOAD + " Einstellungen importieren", ThemeManager.getSuccessColor());
        importButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        importButton.addActionListener(e -> importSettingsProfile());
        card.add(importButton);
        return card;
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
                UnicodeSymbols.TEST_TUBE + " Testing-Updates",
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
                UnicodeSymbols.COPYRIGHT + " 2026 VEBO Oensingen. Alle Rechte vorbehalten.",
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
        return createWrapButtonPanel(FlowLayout.LEFT, 10, 8);
    }

    private JPanel createMaintenanceReloadPanel() {
        JPanel panel = SettingsUtils.createSectionPanel(
                UnicodeSymbols.REFRESH + " Daten neu laden / Import erzwingen",
                "Aktualisiert zwischengespeicherte Daten nach Imports oder manuellen Änderungen an Dateien und Einstellungen.",
                null);
        configureMaintenancePanel(panel);
        panel.add(createMaintenanceHintLabel());
        panel.add(Box.createVerticalStrut(12));
        panel.add(createMaintenanceReloadButtonRow());
        panel.add(Box.createVerticalStrut(12));
        panel.add(createMaintenanceImportRow());
        return panel;
    }

    private void configureMaintenancePanel(JPanel panel) {
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)));
        panel.setBackground(ThemeManager.getCardBackgroundColor());
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    private JLabel createMaintenanceHintLabel() {
        return createInfoLabel(
                "<html><div style='width:650px;'>Nutzen Sie diese Aktionen, wenn Artikel, Warnungen oder Kategorien nach manuellen Dateiänderungen sofort neu eingelesen werden sollen. " +
                        "Der erzwungene Import startet den vollständigen Initialimport erneut.</div></html>");
    }

    private JPanel createMaintenanceReloadButtonRow() {
        JPanel reloadButtonRow = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        for (MaintenanceActionSpec spec : getMaintenanceReloadActions()) {
            reloadButtonRow.add(createMaintenanceActionButton(spec));
        }
        return reloadButtonRow;
    }

    private List<MaintenanceActionSpec> getMaintenanceReloadActions() {
        return List.of(
                new MaintenanceActionSpec(
                        UnicodeSymbols.REFRESH + " Artikel neu laden",
                        "Liest alle Artikel erneut aus der Datenquelle ein.",
                        ThemeManager.getAccentColor(),
                        () -> ArticleManager.getInstance().forceReloadArticles(),
                        "Artikel neu geladen",
                        "Alle Artikel wurden neu geladen."),
                new MaintenanceActionSpec(
                        UnicodeSymbols.REFRESH + " Warnungen neu laden",
                        "Liest alle Warnungen erneut aus der Datenbank ein.",
                        ThemeManager.getAccentColor(),
                        () -> WarningManager.getInstance().forceReloadWarnings(),
                        "Warnungen neu geladen",
                        "Alle Warnungen wurden neu geladen."),
                new MaintenanceActionSpec(
                        UnicodeSymbols.REFRESH + " Kategorien neu laden",
                        "Lädt die Kategorien aus den Einstellungsdateien neu.",
                        ThemeManager.getAccentColor(),
                        ArticleUtils::loadCategories,
                        "Kategorien neu geladen",
                        "Alle Kategorien wurden neu geladen."));
    }

    private JPanel createMaintenanceImportRow() {
        JPanel importRow = createWrapButtonPanel(FlowLayout.LEFT, 8, 8);
        JButton forceImportDataButton = createStyledButton(
                UnicodeSymbols.REFRESH + " Gesamten Datenimport erzwingen",
                ThemeManager.getSuccessColor());
        forceImportDataButton.setToolTipText("Startet den vollständigen Initialimport erneut.");
        forceImportDataButton.addActionListener(e -> triggerForcedDataImport(forceImportDataButton));
        importRow.add(forceImportDataButton);
        return importRow;
    }

    private JButton createUpdateChannelButton(String text, Color color, String tooltip, String channel) {
        JButton button = createStyledButton(text, color);
        button.setToolTipText(tooltip);
        Dimension preferred = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(190, preferred.width), Math.max(46, preferred.height)));
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

    private JButton createMaintenanceActionButton(String text, String tooltip, Color color, Runnable action,
                                                  String successTitle, String successMessage) {
        JButton button = createStyledButton(text, color);
        button.setToolTipText(tooltip);
        button.addActionListener(e -> runMaintenanceActionAsync(
                button,
                UnicodeSymbols.REFRESH + " Lädt...",
                action,
                successTitle,
                successMessage,
                "Neu laden fehlgeschlagen",
                "Die Aktion konnte nicht abgeschlossen werden: "));
        return button;
    }

    private JButton createMaintenanceActionButton(MaintenanceActionSpec spec) {
        return createMaintenanceActionButton(
                spec.text(),
                spec.tooltip(),
                spec.color(),
                spec.action(),
                spec.successTitle(),
                spec.successMessage());
    }

    private void triggerForcedDataImport(JButton triggerButton) {
        if (triggerButton == null) {
            Main.forceImportData();
            return;
        }
        runMaintenanceActionAsync(
                triggerButton,
                UnicodeSymbols.REFRESH + " Import läuft...",
                Main::forceImportData,
                "Datenimport abgeschlossen",
                "Der vollständige Datenimport wurde erfolgreich erneut ausgeführt.",
                "Import fehlgeschlagen",
                "Der erzwungene Datenimport konnte nicht abgeschlossen werden: ");
    }

    private void showTimedInfo(String title, String message) {
        new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setDuration(5000)
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .display();
    }

    private void runMaintenanceActionAsync(JButton triggerButton, String runningText, Runnable action,
                                           String successTitle, String successMessage,
                                           String errorTitle, String errorPrefix) {
        if (triggerButton == null || action == null) {
            return;
        }

        triggerButton.setEnabled(false);
        String originalText = triggerButton.getText();
        if (runningText != null && !runningText.isBlank()) {
            triggerButton.setText(runningText);
        }
        setMaintenanceBusy(true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                action.run();
                return null;
            }

            @Override
            protected void done() {
                triggerButton.setEnabled(true);
                triggerButton.setText(originalText);
                setMaintenanceBusy(false);
                try {
                    get();
                    showTimedInfo(successTitle, successMessage);
                } catch (Exception ex) {
                    logger.error(errorTitle, ex);
                    new MessageDialog()
                            .setTitle(errorTitle)
                            .setMessage(errorPrefix + getReadableExceptionMessage(ex))
                            .setDuration(7000)
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                }
            }
        };
        worker.execute();
    }

    private void setMaintenanceBusy(boolean busy) {
        activeMaintenanceTasks += busy ? 1 : -1;
        if (activeMaintenanceTasks < 0) {
            activeMaintenanceTasks = 0;
        }
        setCursor(Cursor.getPredefinedCursor(
                activeMaintenanceTasks > 0 ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private String getReadableExceptionMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return (message == null || message.isBlank()) ? cause.getClass().getSimpleName() : message;
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

    private JPanel createWrapButtonPanel(int alignment, int hgap, int vgap) {
        return SettingsUiFactory.createWrapButtonPanel(alignment, hgap, vgap);
    }

    /**
     * Creates a scrollable panel wrapper with optimized settings
     */
    private JScrollPane createScrollablePanel(JPanel panel) {
        return SettingsUiFactory.createScrollablePanel(panel);
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
        JButton button = new JButton(UnicodeSymbols.REFRESH + " Zurücksetzen");
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
     * Creates a modern styled button with smooth hover effects and professional
     * appearance
     */
    private JButton createStyledButton(String text, Color originalBg) {
        return SettingsUiFactory.createStyledButton(text, originalBg);
    }

    private Color adjustColor(Color color, float factor) {
        return SettingsColorService.adjustColor(color, factor);
    }

    private Color getSoftGlassSurface(Color base) {
        return SettingsColorService.getSoftGlassSurface(base, GLASS_INTENSITY == GlassIntensity.MEDIUM);
    }

    private Color getSoftGlassBorder() {
        return SettingsColorService.getSoftGlassBorder(GLASS_INTENSITY == GlassIntensity.MEDIUM);
    }

    private Color getSoftGlassHighlight() {
        return SettingsColorService.getSoftGlassHighlight(GLASS_INTENSITY == GlassIntensity.MEDIUM);
    }

    private Color getReadableTextColor(Color bg) {
        return SettingsColorService.getReadableTextColor(bg);
    }

    private Color getStatusErrorColor() {
        return SettingsColorService.getStatusErrorColor();
    }

    private Color getStatusSuccessColor() {
        return SettingsColorService.getStatusSuccessColor();
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
                String previousDatabaseType = Main.settings.getProperty("database_type");
                if (previousDatabaseType == null || previousDatabaseType.isBlank()) {
                    previousDatabaseType = DEFAULT_DATABASE_TYPE.getConfigValue();
                }
                String selectedDatabaseType = getSelectedDatabaseType().getConfigValue();
                boolean databaseTypeChanged = !selectedDatabaseType.equalsIgnoreCase(previousDatabaseType);

                SettingsRuntimeService.persistSettings(collectSettingsProperties());

                System.out.println("[SettingsGUI] Einstellungen gespeichert");

                applySettings(interval, enableWarnings, warningInterval, enableAutoCheck, darkMode);
                ThemeManager.setCustomColors(selectedAccentColor, selectedHeaderColor, selectedButtonColor);

                new MessageDialog()
                        .setTitle("Einstellungen gespeichert")
                        .setMessage(buildSaveSuccessMessage(databaseTypeChanged))
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

    private String buildSaveSuccessMessage(boolean databaseTypeChanged) {
        if (!databaseTypeChanged) {
            return "<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                    "Die neuen Einstellungen wurden erfolgreich übernommen.</html>";
        }
        return "<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                "Die neuen Einstellungen wurden erfolgreich übernommen.<br/><br/>" +
                "<b>Wichtig:</b> Das neue Speicher-Backend wird erst nach einem Neustart der Anwendung verwendet.</html>";
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

    private void resetMaintenanceDefaults() {
        if (deleteOldLogsCheckBox != null) {
            deleteOldLogsCheckBox.setSelected(DEFAULT_DELETE_OLD_LOGS_ON_STARTUP);
        }
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

    private void resetDatabaseDefaults() {
        if (databaseTypeComboBox != null) {
            databaseTypeComboBox.setSelectedItem(DEFAULT_DATABASE_TYPE);
            updateDatabaseBackendHint();
            updateDatabaseMigrationHint();
        }
        if (migrationSourceComboBox != null) {
            DatabaseManager.DatabaseType activeType = Main.databaseManager != null
                    ? Main.databaseManager.getDatabaseType()
                    : DEFAULT_DATABASE_TYPE;
            migrationSourceComboBox.setSelectedItem(activeType);
            updateDatabaseMigrationHint();
        }
    }

    private void resetAllDefaults() {
        resetStockCheckDefaults();
        resetWarningDefaults();
        resetQrCodeDefaults();
        resetFontDefaults();
        resetTableDefaults();
        resetThemeDefaults();
        resetServerDefaults();
        resetDatabaseDefaults();
        resetColorDefaults();
        resetMaintenanceDefaults();
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
        props.setProperty("database_type", getSelectedDatabaseType().getConfigValue());
        props.setProperty("table_font_size", String.valueOf(fontSizeTableSpinner.getValue()));
        props.setProperty("table_font_size_tab", String.valueOf(fontSizeTabSpinner.getValue()));
        Object fontStyle = fontComboBox.getSelectedItem();
        props.setProperty("font_style", fontStyle == null ? DEFAULT_FONT_STYLE : fontStyle.toString());
        props.setProperty("theme_accent_color", SettingsProfileService.colorToSetting(selectedAccentColor));
        props.setProperty("theme_header_color", SettingsProfileService.colorToSetting(selectedHeaderColor));
        props.setProperty("theme_button_color", SettingsProfileService.colorToSetting(selectedButtonColor));
        props.setProperty("delete_old_logs_on_startup", String.valueOf(deleteOldLogsCheckBox.isSelected()));
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
        if (databaseTypeComboBox != null) {
            databaseTypeComboBox.setSelectedItem(
                    DatabaseManager.DatabaseType.fromConfig(props.getProperty("database_type")));
        }
        if (migrationSourceComboBox != null) {
            DatabaseManager.DatabaseType activeType = Main.databaseManager != null
                    ? Main.databaseManager.getDatabaseType()
                    : DEFAULT_DATABASE_TYPE;
            migrationSourceComboBox.setSelectedItem(activeType);
        }
        fontSizeTableSpinner.setValue(SettingsProfileService.parseIntProperty(props, "table_font_size", DEFAULT_TABLE_FONT_SIZE));
        fontSizeTabSpinner.setValue(SettingsProfileService.parseIntProperty(props, "table_font_size_tab", DEFAULT_TAB_FONT_SIZE));
        fontComboBox.setSelectedItem(props.getProperty("font_style", DEFAULT_FONT_STYLE));
        selectedAccentColor = SettingsProfileService.parseColor(props.getProperty("theme_accent_color"));
        selectedHeaderColor = SettingsProfileService.parseColor(props.getProperty("theme_header_color"));
        selectedButtonColor = SettingsProfileService.parseColor(props.getProperty("theme_button_color"));
        deleteOldLogsCheckBox.setSelected(Boolean.parseBoolean(
                props.getProperty("delete_old_logs_on_startup", String.valueOf(DEFAULT_DELETE_OLD_LOGS_ON_STARTUP))));
        updateColorControls();
        updateDatabaseBackendHint();
        updateDatabaseMigrationHint();
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
        return SettingsUiFactory.createLabeledSpinnerPanel(labelText, spinner, unitText, columns);
    }

    /**
     * Creates a panel with a label and combo box
     */
    private JPanel createLabeledComboBoxPanel(String labelText, JComboBox<?> comboBox) {
        return SettingsUiFactory.createLabeledComboBoxPanel(labelText, comboBox);
    }

    /**
     * Creates an info label with HTML content
     */
    private JLabel createInfoLabel(String htmlContent) {
        return SettingsUiFactory.createInfoLabel(htmlContent);
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

    private void styleComboBox(JComboBox<?> combo) {
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
