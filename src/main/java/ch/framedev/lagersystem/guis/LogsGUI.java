package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleExporter;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GUI for viewing and filtering application logs.
 */
@SuppressWarnings("DuplicatedCode")
public class LogsGUI extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private final Logger logger = LogManager.getLogger(LogsGUI.class);

    private final List<String> orderLogsData = new ArrayList<>();
    private final List<String> supplierLogsData = new ArrayList<>();
    private final List<String> supplierOrderLogs = new ArrayList<>();

    private final JTextPane logTextPane = new JTextPane();
    private final JTextField searchField = new JTextField(20);
    private final JTextField fromDateField = new JTextField(8);
    private final JTextField toDateField = new JTextField(8);
    private final JLabel lastUpdatedLabel = new JLabel("Letzte Aktualisierung: -");
    private final JLabel categoryBadgeLabel = new JLabel();
    private final JLabel resultsBadgeLabel = new JLabel();
    private final JLabel filterStateLabel = new JLabel("Keine Filter aktiv");
    private final JLabel viewerTitleLabel = new JLabel(UnicodeSymbols.DOCUMENT + " Log-Ausgabe");
    private final JLabel totalStatsLabel = new JLabel();
    private final JLabel infoStatsLabel = new JLabel();
    private final JLabel warnStatsLabel = new JLabel();
    private final JLabel errorStatsLabel = new JLabel();
    private final JCheckBox autoRefreshCheckBox = new JCheckBox("Auto-Refresh");
    private final Timer autoRefreshTimer;
    private Point dragStartPoint;

    private final JButton orderLogsButton;
    private final JButton supplierLogsButton;
    private final JButton supplierOrderLogsButton;
    private final JButton allLogsButton;
    private final JButton clearLogsButton;

    private List<String> currentLogs = new ArrayList<>();
    private List<String> filteredLogs = new ArrayList<>();
    private LogCategory currentCategory = LogCategory.ORDER;

    private enum LogCategory {
        ORDER,
        SUPPLIER,
        SUPPLIER_ORDER,
        ALL
    }

    public LogsGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();

        setTitle(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        setUndecorated(true);
        setSize(940, 640);
        setMinimumSize(new Dimension(860, 540));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getRootPane().setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(mainPanel, BorderLayout.CENTER);

        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        JPanel headerWrapper = createHeaderPanel();
        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(8));
        }

        orderLogsButton = createCategoryButton(UnicodeSymbols.PACKAGE + " Bestellungs-Protokolle",
                ThemeManager.getSuccessColor(),
                LogCategory.ORDER);
        supplierLogsButton = createCategoryButton(UnicodeSymbols.TRUCK + " Lieferanten-Protokolle",
                ThemeManager.getAccentColor(),
                LogCategory.SUPPLIER);
        supplierOrderLogsButton = createCategoryButton(UnicodeSymbols.DOCUMENT + " Lieferanten-Bestellungen",
                ThemeManager.getWarningColor(),
                LogCategory.SUPPLIER_ORDER);
        allLogsButton = createCategoryButton(UnicodeSymbols.CLIPBOARD + " Alle Protokolle",
                ThemeManager.getPrimaryColor(),
                LogCategory.ALL);

        JPanel categoryCard = createCardPanel(16, new BorderLayout(8, 8));
        categoryCard.add(buildCategoryPanel(), BorderLayout.CENTER);
        topContainer.add(categoryCard);
        topContainer.add(Box.createVerticalStrut(8));

        clearLogsButton = prepareActionButton(
                JFrameUtils.createThemeButton(UnicodeSymbols.TRASH + " Logs löschen", ThemeManager.getErrorColor()),
                140,
                38
        );
        clearLogsButton.setToolTipText("Aktuelle Log-Kategorie löschen");
        clearLogsButton.addActionListener(e -> clearCurrentLogs());

        JButton refreshButton = prepareActionButton(
                JFrameUtils.createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren"),
                136,
                38
        );
        refreshButton.setToolTipText("Logs neu laden");
        refreshButton.addActionListener(e -> refreshCurrentLogs());

        JButton exportPdfButton = prepareActionButton(
                JFrameUtils.createSecondaryButton(UnicodeSymbols.DOCUMENT + " PDF"),
                112,
                38
        );
        exportPdfButton.setToolTipText("Gefilterte Logs als PDF exportieren");
        exportPdfButton.addActionListener(e -> exportLogsToPdf(filteredLogs));

        JButton exportCsvButton = prepareActionButton(
                JFrameUtils.createSecondaryButton(UnicodeSymbols.DOWNLOAD + " CSV"),
                112,
                38
        );
        exportCsvButton.setToolTipText("Gefilterte Logs als CSV exportieren");
        exportCsvButton.addActionListener(e -> exportLogsToCsv(filteredLogs));

        JButton clearFilterButton = prepareActionButton(
                JFrameUtils.createSecondaryButton(UnicodeSymbols.CLEAR + " Filter leeren"),
                136,
                38
        );
        clearFilterButton.setToolTipText("Such- und Datumsfilter zurücksetzen");
        clearFilterButton.addActionListener(e -> clearFilters());

        JPanel controlsCard = createCardPanel(16, new BorderLayout(8, 8));
        controlsCard.add(buildFilterPanel(), BorderLayout.CENTER);
        controlsCard.add(buildActionPanel(autoRefreshCheckBox, lastUpdatedLabel, refreshButton, exportPdfButton, exportCsvButton, clearLogsButton, clearFilterButton), BorderLayout.SOUTH);
        topContainer.add(controlsCard);

        mainPanel.add(topContainer, BorderLayout.NORTH);
        mainPanel.add(buildContentCard(), BorderLayout.CENTER);

        initPopupMenu();
        attachFilterListeners();

        autoRefreshTimer = new Timer(30_000, e -> refreshCurrentLogs());
        autoRefreshTimer.setRepeats(true);
        autoRefreshCheckBox.setOpaque(false);
        autoRefreshCheckBox.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        autoRefreshCheckBox.setForeground(ThemeManager.getTextPrimaryColor());
        autoRefreshCheckBox.addActionListener(e -> {
            if (autoRefreshCheckBox.isSelected()) {
                autoRefreshTimer.start();
            } else {
                autoRefreshTimer.stop();
            }
        });

        styleBadge(categoryBadgeLabel);
        styleBadge(resultsBadgeLabel);
        styleBadge(lastUpdatedLabel);
        styleBadge(filterStateLabel);
        filterStateLabel.setBackground(ThemeManager.getInputBackgroundColor());
        styleStatBadge(totalStatsLabel, ThemeManager.getPrimaryColor());
        styleStatBadge(infoStatsLabel, ThemeManager.getInfoColor());
        styleStatBadge(warnStatsLabel, ThemeManager.getWarningColor());
        styleStatBadge(errorStatsLabel, ThemeManager.getErrorColor());
        viewerTitleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        viewerTitleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchField.putClientProperty("JTextField.placeholderText", "Logs durchsuchen");
        fromDateField.putClientProperty("JTextField.placeholderText", "dd.MM.yyyy");
        toDateField.putClientProperty("JTextField.placeholderText", "dd.MM.yyyy");

        installKeyboardShortcuts(refreshButton, exportPdfButton, exportCsvButton, clearFilterButton);
        setCategory(LogCategory.ORDER);
    }

    @Override
    public void dispose() {
        autoRefreshTimer.stop();
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void installKeyboardShortcuts(JButton refreshButton, JButton exportPdfButton,
                                          JButton exportCsvButton, JButton clearFilterButton) {
        JRootPane rootPane = getRootPane();
        KeyboardShortcutUtils.addTooltipHint(searchField, KeyboardShortcutUtils.menuKey(KeyEvent.VK_F));
        KeyboardShortcutUtils.addTooltipHint(refreshButton, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        KeyboardShortcutUtils.addTooltipHint(clearFilterButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_L));
        KeyboardShortcutUtils.addTooltipHint(exportPdfButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_P));
        KeyboardShortcutUtils.addTooltipHint(exportCsvButton, KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_C));
        KeyboardShortcutUtils.registerClose(this);
        KeyboardShortcutUtils.registerFocus(rootPane, "logs.focusSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_F), searchField);
        KeyboardShortcutUtils.registerButton(rootPane, "logs.refresh",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshButton);
        KeyboardShortcutUtils.registerButton(rootPane, "logs.clearFilters",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_L), clearFilterButton);
        KeyboardShortcutUtils.registerButton(rootPane, "logs.exportPdf",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_P), exportPdfButton);
        KeyboardShortcutUtils.registerButton(rootPane, "logs.exportCsv",
                KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_C), exportCsvButton);
        KeyboardShortcutUtils.register(rootPane, "logs.categoryOrder",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_1), () -> setCategory(LogCategory.ORDER));
        KeyboardShortcutUtils.register(rootPane, "logs.categorySupplier",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_2), () -> setCategory(LogCategory.SUPPLIER));
        KeyboardShortcutUtils.register(rootPane, "logs.categorySupplierOrders",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_3), () -> setCategory(LogCategory.SUPPLIER_ORDER));
        KeyboardShortcutUtils.register(rootPane, "logs.categoryAll",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_4), () -> setCategory(LogCategory.ALL));
    }

    private JPanel createHeaderPanel() {
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (disableHeader) {
            return null;
        }

        JFrameUtils.RoundedPanel headerPanel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)
        ));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Protokolle durchsuchen, filtern und exportieren");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(3));
        headerText.add(subtitleLabel);

        headerPanel.add(headerText, BorderLayout.WEST);
        headerPanel.add(buildHeaderActionsPanel(), BorderLayout.EAST);
        installWindowDrag(headerPanel);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(headerPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildHeaderActionsPanel() {
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actionsPanel.setOpaque(false);
        actionsPanel.add(createHeaderChip(UnicodeSymbols.BULB + " Filter & Export", ThemeManager.getSurfaceColor()));
        actionsPanel.add(createHeaderCloseButton());
        return actionsPanel;
    }

    private JPanel buildCategoryPanel() {
        JPanel categoryPanel = new JPanel(new BorderLayout(10, 8));
        categoryPanel.setOpaque(false);

        JLabel categoryTitle = new JLabel(UnicodeSymbols.CLIPBOARD + " Protokollquellen");
        categoryTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        categoryTitle.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel categoryHint = new JLabel("Zwischen Quellen wechseln, um Bestellungen, Lieferanten oder alle Dateien gesammelt zu prüfen.");
        categoryHint.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        categoryHint.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.add(categoryTitle);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(categoryHint);

        JPanel buttonRow = createWrapPanel(FlowLayout.LEFT, 8, 6);
        buttonRow.add(orderLogsButton);
        buttonRow.add(supplierLogsButton);
        buttonRow.add(supplierOrderLogsButton);
        buttonRow.add(allLogsButton);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(categoryBadgeLabel);
        badgeRow.add(resultsBadgeLabel);

        categoryPanel.add(textPanel, BorderLayout.WEST);
        categoryPanel.add(buttonRow, BorderLayout.CENTER);
        categoryPanel.add(badgeRow, BorderLayout.EAST);
        return categoryPanel;
    }

    private JPanel buildFilterPanel() {
        JPanel filterPanel = new JPanel(new BorderLayout(10, 8));
        filterPanel.setOpaque(false);

        styleTextField(searchField);
        styleTextField(fromDateField);
        styleTextField(toDateField);
        searchField.setColumns(18);
        fromDateField.setColumns(9);
        toDateField.setColumns(9);

        searchField.setToolTipText("Textsuche in Logs");
        fromDateField.setToolTipText("Format: dd.MM.yyyy");
        toDateField.setToolTipText("Format: dd.MM.yyyy");

        JLabel filterTitle = new JLabel(UnicodeSymbols.FILTER + " Filter");
        filterTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        filterTitle.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel filterHint = new JLabel("Suche nach Begriffen und schränke Ergebnisse optional per Datum ein.");
        filterHint.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        filterHint.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel introPanel = new JPanel();
        introPanel.setOpaque(false);
        introPanel.setLayout(new BoxLayout(introPanel, BoxLayout.Y_AXIS));
        introPanel.add(filterTitle);
        introPanel.add(Box.createVerticalStrut(3));
        introPanel.add(filterHint);

        JPanel fieldsPanel = createWrapPanel(FlowLayout.LEFT, 8, 6);
        fieldsPanel.add(buildInputGroup(UnicodeSymbols.SEARCH + " Suche", searchField));
        fieldsPanel.add(buildInputGroup(UnicodeSymbols.CALENDAR + " Von", fromDateField));
        fieldsPanel.add(buildInputGroup(UnicodeSymbols.CALENDAR + " Bis", toDateField));

        filterPanel.add(introPanel, BorderLayout.WEST);
        filterPanel.add(fieldsPanel, BorderLayout.CENTER);
        return filterPanel;
    }

    private JPanel buildActionPanel(JCheckBox autoRefresh, JLabel updatedLabel, JButton refreshButton,
                                    JButton exportPdfButton, JButton exportCsvButton, JButton clearButton,
                                    JButton clearFilterButton) {
        JPanel actionPanel = new JPanel(new BorderLayout(8, 8));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        infoPanel.setOpaque(false);
        infoPanel.add(autoRefresh);
        infoPanel.add(updatedLabel);

        JPanel buttonPanel = createWrapPanel(FlowLayout.RIGHT, 6, 6);
        buttonPanel.add(refreshButton);
        buttonPanel.add(clearFilterButton);
        buttonPanel.add(exportPdfButton);
        buttonPanel.add(exportCsvButton);
        buttonPanel.add(clearButton);

        actionPanel.add(infoPanel, BorderLayout.WEST);
        actionPanel.add(buttonPanel, BorderLayout.EAST);
        return actionPanel;
    }

    private JPanel buildContentCard() {
        JPanel contentCard = createCardPanel(16, new BorderLayout());
        contentCard.add(buildViewerHeader(), BorderLayout.NORTH);

        logTextPane.setEditable(false);
        logTextPane.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logTextPane.setBackground(ThemeManager.getInputBackgroundColor());
        logTextPane.setForeground(ThemeManager.getTextPrimaryColor());
        logTextPane.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        JScrollPane scrollPane = new JScrollPane(logTextPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(ThemeManager.getInputBackgroundColor());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        contentCard.add(scrollPane, BorderLayout.CENTER);
        return contentCard;
    }

    private JPanel buildViewerHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JLabel helperLabel = new JLabel("Rechtsklick zum Kopieren • Datum: dd.MM.yyyy");
        helperLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        helperLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(viewerTitleLabel);
        leftPanel.add(Box.createVerticalStrut(2));
        leftPanel.add(helperLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(filterStateLabel);

        JPanel statsPanel = createWrapPanel(FlowLayout.LEFT, 6, 6);
        statsPanel.add(totalStatsLabel);
        statsPanel.add(infoStatsLabel);
        statsPanel.add(warnStatsLabel);
        statsPanel.add(errorStatsLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        headerPanel.add(statsPanel, BorderLayout.SOUTH);
        return headerPanel;
    }

    private JPanel buildInputGroup(String title, JTextField field) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        group.add(titleLabel);
        group.add(Box.createVerticalStrut(3));
        group.add(field);
        return group;
    }

    private JPanel createCardPanel(int radius, LayoutManager layout) {
        JFrameUtils.RoundedPanel panel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), radius);
        panel.setLayout(layout);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return panel;
    }

    private JButton createCategoryButton(String text, Color color, LogCategory category) {
        JButton button = JFrameUtils.createThemeButton(text, color);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setToolTipText(text);
        button.setPreferredSize(new Dimension(Math.max(180, button.getPreferredSize().width), 38));
        button.putClientProperty("logs.baseColor", color);
        button.putClientProperty("logs.category", category);
        button.addActionListener(e -> setCategory(category));
        return button;
    }

    private void attachFilterListeners() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        };
        searchField.getDocument().addDocumentListener(listener);
        fromDateField.getDocument().addDocumentListener(listener);
        toDateField.getDocument().addDocumentListener(listener);
        registerClearShortcut(searchField);
        registerClearShortcut(fromDateField);
        registerClearShortcut(toDateField);
    }

    private void registerClearShortcut(JTextField field) {
        field.registerKeyboardAction(
                e -> clearFilters(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_FOCUSED
        );
    }

    private void setCategory(LogCategory category) {
        currentCategory = category;
        updateCategoryButtons();
        refreshCurrentLogs();
    }

    private void updateCategoryButtons() {
        updateCategoryButtonState(orderLogsButton, LogCategory.ORDER);
        updateCategoryButtonState(supplierLogsButton, LogCategory.SUPPLIER);
        updateCategoryButtonState(supplierOrderLogsButton, LogCategory.SUPPLIER_ORDER);
        updateCategoryButtonState(allLogsButton, LogCategory.ALL);
        clearLogsButton.setEnabled(currentCategory != LogCategory.ALL);
        categoryBadgeLabel.setText(UnicodeSymbols.TAG + " " + getCategoryDisplayName(currentCategory));
        viewerTitleLabel.setText(UnicodeSymbols.DOCUMENT + " " + getCategoryDisplayName(currentCategory));
    }

    private void updateCategoryButtonState(JButton button, LogCategory category) {
        if (button == null || category == null) {
            return;
        }

        Color baseColor = (Color) button.getClientProperty("logs.baseColor");
        if (baseColor == null) {
            baseColor = ThemeManager.getAccentColor();
        }

        boolean selected = currentCategory == category;
        button.setEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(selected ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
        JFrameUtils.applyButtonPalette(button, selected ? baseColor : mixColors(baseColor, ThemeManager.getSurfaceColor(), 0.18f));
        button.setForeground(selected
                ? getReadableTextColor(baseColor)
                : ThemeManager.getTextPrimaryColor());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(selected ? ThemeManager.getAccentColor() : mixColors(baseColor, Color.BLACK, 0.15f),
                        selected ? 2 : 1, true),
                BorderFactory.createEmptyBorder(selected ? 7 : 8, 14, selected ? 7 : 8, 14)));
    }

    private String getCategoryDisplayName(LogCategory category) {
        return switch (category) {
            case ORDER -> "Bestellungs-Protokolle";
            case SUPPLIER -> "Lieferanten-Protokolle";
            case SUPPLIER_ORDER -> "Lieferanten-Bestellungen";
            case ALL -> "Alle Protokolle";
        };
    }

    private void refreshCurrentLogs() {
        currentLogs = switch (currentCategory) {
            case ORDER -> loadOrderLogs();
            case SUPPLIER -> loadSupplierLogs();
            case SUPPLIER_ORDER -> loadSupplierOrderLogs();
            case ALL -> loadAllLogs();
        };
        applyFilters();
        lastUpdatedLabel.setText("Letzte Aktualisierung: " + TIMESTAMP_FORMAT.format(new Date()));
    }

    private List<String> loadAllLogs() {
        File orderLogFile = new File(new File(Main.getAppDataDir(), "logs"), "bestellung.log");
        File vendorOrderFile = new File(new File(Main.getAppDataDir(), "logs"), "vendorOrder.log");

        List<String> allLogs = new ArrayList<>();
        List<Path> mainLogFiles = getMainApplicationLogFiles();
        if (!mainLogFiles.isEmpty()) {
            allLogs.add("--- Haupt-Protokolle ---");
            for (Path logPath : mainLogFiles) {
                allLogs.add("[Datei] " + logPath.getFileName());
                try {
                    allLogs.addAll(Files.readAllLines(logPath, StandardCharsets.UTF_8));
                } catch (IOException ignored) {
                }
            }
        }

        appendLogFileSection(allLogs, orderLogFile, "--- Bestellungs-Protokolle ---");
        appendLogFileSection(allLogs, vendorOrderFile, "--- Lieferanten-Protokolle ---");
        appendLogSection(allLogs, SupplierOrderGUI.getAllSupplierOrdersForDisplay(), "--- Lieferanten-Bestellungen ---");

        return allLogs;
    }

    private void appendLogFileSection(List<String> target, File logFile, String title) {
        if (!logFile.exists()) {
            return;
        }
        try {
            target.add(title);
            target.addAll(Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8));
        } catch (IOException ignored) {
        }
    }

    private void appendLogSection(List<String> target, List<String> sectionLines, String title) {
        if (sectionLines == null || sectionLines.isEmpty()) {
            return;
        }
        target.add(title);
        target.addAll(sectionLines);
    }

    private List<Path> getMainApplicationLogFiles() {
        Path logDirectory = Main.logUtils.getLogDirectoryPath();
        if (!Files.isDirectory(logDirectory)) {
            return List.of();
        }

        try (var fileStream = Files.list(logDirectory)) {
            return fileStream
                    .filter(Files::isRegularFile)
                    .filter(LogUtils::isMainLogFile)
                    .sorted()
                    .toList();
        } catch (IOException ex) {
            logger.error("Fehler beim Laden der Haupt-Protokolle: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    private List<String> loadOrderLogs() {
        orderLogsData.clear();
        orderLogsData.addAll(OrderLoggingUtils.getInstance().getAllLogs());
        return new ArrayList<>(orderLogsData);
    }

    private List<String> loadSupplierLogs() {
        supplierLogsData.clear();
        supplierLogsData.addAll(VendorOrderLogging.getInstance().getLogs());
        return new ArrayList<>(supplierLogsData);
    }

    private List<String> loadSupplierOrderLogs() {
        supplierOrderLogs.clear();
        supplierOrderLogs.addAll(SupplierOrderGUI.getAllSupplierOrdersForDisplay());
        return new ArrayList<>(supplierOrderLogs);
    }

    private void applyFilters() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        LocalDate fromDate = parseDateInput(fromDateField.getText());
        LocalDate toDate = parseDateInput(toDateField.getText());

        List<String> result = new ArrayList<>();
        for (String logEntry : currentLogs) {
            if (logEntry == null || logEntry.isBlank()) {
                continue;
            }
            if (!query.isEmpty() && !logEntry.toLowerCase().contains(query)) {
                continue;
            }
            if (fromDate != null || toDate != null) {
                LocalDate logDate = extractDateFromLog(logEntry);
                if (logDate == null) {
                    continue;
                }
                if (fromDate != null && logDate.isBefore(fromDate)) {
                    continue;
                }
                if (toDate != null && logDate.isAfter(toDate)) {
                    continue;
                }
            }
            result.add(logEntry);
        }

        filteredLogs = result;
        resultsBadgeLabel.setText(UnicodeSymbols.SEARCH + " Treffer: " + filteredLogs.size() + " / " + currentLogs.size());
        filterStateLabel.setText(buildFilterSummary(query, fromDate, toDate));
        updateStats(filteredLogs);
        renderLogs(filteredLogs);
    }

    private void updateStats(List<String> logs) {
        int total = logs == null ? 0 : logs.size();
        int info = 0;
        int warn = 0;
        int error = 0;

        if (logs != null) {
            for (String line : logs) {
                String styleName = resolveStyleName(line);
                if ("info".equals(styleName)) {
                    info++;
                } else if ("warn".equals(styleName)) {
                    warn++;
                } else if ("error".equals(styleName)) {
                    error++;
                }
            }
        }

        totalStatsLabel.setText(UnicodeSymbols.CLIPBOARD + " Gesamt: " + total);
        infoStatsLabel.setText(UnicodeSymbols.INFO + " Info: " + info);
        warnStatsLabel.setText(UnicodeSymbols.WARNING + " Warnungen: " + warn);
        errorStatsLabel.setText(UnicodeSymbols.ERROR + " Fehler: " + error);
    }

    private String buildFilterSummary(String query, LocalDate fromDate, LocalDate toDate) {
        List<String> parts = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            parts.add("Suche: " + query);
        }
        if (fromDate != null) {
            parts.add("Von: " + DATE_FORMAT.format(fromDate));
        }
        if (toDate != null) {
            parts.add("Bis: " + DATE_FORMAT.format(toDate));
        }
        if (parts.isEmpty()) {
            return "Keine Filter aktiv";
        }
        return String.join(" • ", parts);
    }

    private void clearFilters() {
        searchField.setText("");
        fromDateField.setText("");
        toDateField.setText("");
        applyFilters();
    }

    private void renderLogs(List<String> logs) {
        StyledDocument document = logTextPane.getStyledDocument();
        try {
            document.remove(0, document.getLength());
        } catch (BadLocationException ignored) {
        }

        if (logs == null || logs.isEmpty()) {
            appendStyledLine(document, "Keine Logs vorhanden.", "muted");
            logTextPane.setCaretPosition(0);
            return;
        }

        for (String line : logs) {
            appendStyledLine(document, line, resolveStyleName(line));
        }
        logTextPane.setCaretPosition(0);
    }

    private void appendStyledLine(StyledDocument document, String line, String styleName) {
        Style style = getStyle(styleName);
        try {
            document.insertString(document.getLength(), line + System.lineSeparator(), style);
        } catch (BadLocationException ignored) {
        }
    }

    private Style getStyle(String styleName) {
        Style style = logTextPane.getStyle(styleName);
        if (style != null) {
            return style;
        }

        style = logTextPane.addStyle(styleName, null);
        StyleConstants.setFontFamily(style, logTextPane.getFont().getFamily());
        StyleConstants.setFontSize(style, logTextPane.getFont().getSize());
        StyleConstants.setForeground(style, ThemeManager.getTextPrimaryColor());
        StyleConstants.setSpaceAbove(style, 1.5f);
        StyleConstants.setSpaceBelow(style, 1.5f);

        switch (styleName) {
            case "info" -> StyleConstants.setForeground(style, ThemeManager.getInfoColor());
            case "warn" -> StyleConstants.setForeground(style, ThemeManager.getWarningColor());
            case "error" -> StyleConstants.setForeground(style, ThemeManager.getErrorColor());
            case "section" -> {
                StyleConstants.setBold(style, true);
                StyleConstants.setFontSize(style, logTextPane.getFont().getSize() + 1);
                StyleConstants.setForeground(style, ThemeManager.getAccentColor());
            }
            case "muted" -> StyleConstants.setForeground(style, ThemeManager.getTextSecondaryColor());
            default -> {
            }
        }
        return style;
    }

    private String resolveStyleName(String line) {
        if (line == null) {
            return "default";
        }
        if (line.startsWith("---")) {
            return "section";
        }
        if (line.startsWith("[Datei]")) {
            return "muted";
        }
        if (line.startsWith("Lieferant:") || line.startsWith("Hinzugefügt:")) {
            return "muted";
        }

        String upper = line.toUpperCase();
        if (upper.contains("ERROR") || upper.contains("FEHLER") || upper.contains("EXCEPTION")) {
            return "error";
        }
        if (upper.contains("WARN") || upper.contains("WARNUNG")) {
            return "warn";
        }
        if (upper.contains("INFO") || upper.contains("OK")) {
            return "info";
        }
        return "default";
    }

    private LocalDate parseDateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        String trimmed = input.trim();
        DateTimeFormatter[] formats = new DateTimeFormatter[]{
                DATE_FORMAT,
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : formats) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private LocalDate extractDateFromLog(String logEntry) {
        Matcher matcher = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})|(\\d{4}-\\d{2}-\\d{2})").matcher(logEntry);
        if (!matcher.find()) {
            return null;
        }
        return parseDateInput(matcher.group());
    }

    private void initPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem copySelected = new JMenuItem("Auswahl kopieren");
        copySelected.addActionListener(e -> copyText(logTextPane.getSelectedText()));

        JMenuItem copyLine = new JMenuItem("Zeile kopieren");
        copyLine.addActionListener(e -> copyText(getCurrentLineText()));

        JMenuItem copyAll = new JMenuItem("Alles kopieren");
        copyAll.addActionListener(e -> copyText(logTextPane.getText()));

        popupMenu.add(copySelected);
        popupMenu.add(copyLine);
        popupMenu.add(copyAll);

        logTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private String getCurrentLineText() {
        try {
            int caret = logTextPane.getCaretPosition();
            int line = logTextPane.getDocument().getDefaultRootElement().getElementIndex(caret);
            int start = logTextPane.getDocument().getDefaultRootElement().getElement(line).getStartOffset();
            int end = logTextPane.getDocument().getDefaultRootElement().getElement(line).getEndOffset();
            return logTextPane.getDocument().getText(start, end - start).trim();
        } catch (BadLocationException ex) {
            return "";
        }
    }

    private void copyText(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    private void exportLogsToCsv(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            new MessageDialog()
                    .setTitle("CSV Export")
                    .setMessage("Keine Logs zum Export vorhanden.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV Speichern");
        fileChooser.setSelectedFile(new File("Logs_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
        }

        try (var writer = Files.newBufferedWriter(fileToSave.toPath(), StandardCharsets.UTF_8)) {
            for (String line : logs) {
                String escaped = line.replace("\"", "\"\"");
                writer.write("\"" + escaped + "\"");
                writer.write(System.lineSeparator());
            }

            new MessageDialog()
                    .setTitle("CSV Export")
                    .setMessage("CSV erfolgreich exportiert:\n" + fileToSave.getAbsolutePath())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } catch (IOException ex) {
            new MessageDialog()
                    .setTitle("CSV Export")
                    .setMessage("Fehler beim CSV-Export: " + ex.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            logger.error("Could not export CSV {}", ex.getMessage(), ex);
        }
    }

    private void exportLogsToPdf(List<String> logs) {
        ArticleExporter.exportLogsToPdf(logs, this);
    }

    private void clearCurrentLogs() {
        if (currentCategory == LogCategory.ALL) {
            new MessageDialog()
                    .setTitle("Logs löschen")
                    .setMessage("Bitte wählen Sie eine konkrete Log-Kategorie zum Löschen aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        String categoryName = getCategoryDisplayName(currentCategory);
        int confirm = new MessageDialog()
                .setTitle("Logs löschen")
                .setMessage("Möchten Sie alle " + categoryName + " löschen?")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setOptions(new String[]{"Ja, löschen", "Abbrechen"})
                .displayWithOptions();
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        File logFile = switch (currentCategory) {
            case ORDER -> new File(new File(Main.getAppDataDir(), "logs"), "bestellung.log");
            case SUPPLIER -> new File(new File(Main.getAppDataDir(), "logs"), "vendorOrder.log");
            case SUPPLIER_ORDER -> new File(Main.getAppDataDir(), "supplier_orders.txt");
            case ALL -> null;
        };

        if (logFile == null) {
            return;
        }

        try {
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
                throw new IOException("Fehler beim Erstellen der Verzeichnisse");
            }
            Files.writeString(logFile.toPath(), "", StandardCharsets.UTF_8);
            refreshCurrentLogs();
        } catch (IOException ex) {
            new MessageDialog()
                    .setTitle("Logs löschen")
                    .setMessage("Fehler beim Löschen der Logs: " + ex.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            logger.error("Fehler beim Löschen der Logs: {}", ex.getMessage(), ex);
        }
    }

    private void styleTextField(JTextField field) {
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        JFrameUtils.styleTextField(field);
    }

    private void styleBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }

    private void styleStatBadge(JLabel label, Color baseColor) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        label.setForeground(getReadableTextColor(baseColor));
        label.setOpaque(true);
        label.setBackground(mixColors(baseColor, ThemeManager.getCardBackgroundColor(), 0.18f));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(mixColors(baseColor, Color.BLACK, 0.18f), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
    }

    private JPanel createWrapPanel(int alignment, int hgap, int vgap) {
        JPanel panel = new JPanel(new WrapLayout(alignment, hgap, vgap));
        panel.setOpaque(false);
        return panel;
    }

    private JLabel createHeaderChip(String text, Color background) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(background);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        return label;
    }

    private JButton prepareActionButton(JButton button, int minWidth, int height) {
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        Dimension preferredSize = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(minWidth, preferredSize.width), height));
        return button;
    }

    private Color getReadableTextColor(Color bg) {
        if (bg == null) {
            return ThemeManager.getTextPrimaryColor();
        }
        double luminance = (0.2126 * bg.getRed() + 0.7152 * bg.getGreen() + 0.0722 * bg.getBlue()) / 255.0;
        return luminance > 0.6 ? new Color(20, 20, 20) : Color.WHITE;
    }

    private Color mixColors(Color base, Color mix, double ratio) {
        if (base == null) {
            return mix == null ? ThemeManager.getCardBackgroundColor() : mix;
        }
        if (mix == null) {
            return base;
        }
        double clamped = Math.max(0d, Math.min(1d, ratio));
        int red = (int) Math.round(base.getRed() * (1d - clamped) + mix.getRed() * clamped);
        int green = (int) Math.round(base.getGreen() * (1d - clamped) + mix.getGreen() * clamped);
        int blue = (int) Math.round(base.getBlue() * (1d - clamped) + mix.getBlue() * clamped);
        return new Color(red, green, blue);
    }

    private JButton createHeaderCloseButton() {
        JButton button = JFrameUtils.createThemeButton(UnicodeSymbols.CLOSE + " Schließen", ThemeManager.getErrorColor());
        prepareActionButton(button, 128, 38);
        button.setToolTipText("Schließt das Logfenster");
        button.addActionListener(e -> dispose());
        return button;
    }

    private void installWindowDrag(JComponent target) {
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

    private static final class WrapLayout extends FlowLayout {

        private WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= getHgap() + 1;
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                Container container = target;

                while (targetWidth == 0 && container.getParent() != null) {
                    container = container.getParent();
                    targetWidth = container.getWidth();
                }

                if (targetWidth <= 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                Insets insets = target.getInsets();
                int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);

                Dimension dimension = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                for (Component component : target.getComponents()) {
                    if (!component.isVisible()) {
                        continue;
                    }

                    Dimension componentSize = preferred ? component.getPreferredSize() : component.getMinimumSize();
                    if (rowWidth + componentSize.width > maxWidth) {
                        addRow(dimension, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += getHgap();
                    }
                    rowWidth += componentSize.width;
                    rowHeight = Math.max(rowHeight, componentSize.height);
                }

                addRow(dimension, rowWidth, rowHeight);
                dimension.width += insets.left + insets.right + getHgap() * 2;
                dimension.height += insets.top + insets.bottom + getVgap() * 2;
                return dimension;
            }
        }

        private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
            dimension.width = Math.max(dimension.width, rowWidth);
            if (dimension.height > 0) {
                dimension.height += getVgap();
            }
            dimension.height += rowHeight;
        }
    }
}
