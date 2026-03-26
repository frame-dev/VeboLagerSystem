package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleExporter;
import ch.framedev.lagersystem.utils.JFrameUtils;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    private final JCheckBox autoRefreshCheckBox = new JCheckBox("Auto-Refresh");
    private final Timer autoRefreshTimer;

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
        setSize(980, 680);
        setMinimumSize(new Dimension(900, 580));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(mainPanel, BorderLayout.CENTER);

        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));

        JPanel headerWrapper = createHeaderPanel();
        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }

        orderLogsButton = createCategoryButton(UnicodeSymbols.PACKAGE + " Bestellungs-Protokolle",
                ThemeManager.getSuccessColor(),
                LogCategory.ORDER);
        supplierLogsButton = createCategoryButton(UnicodeSymbols.TRUCK + " Lieferanten-Protokolle",
                ThemeManager.getAccentColor(),
                LogCategory.SUPPLIER);
        supplierOrderLogsButton = createCategoryButton(UnicodeSymbols.DOCUMENT + " Lieferanten-Bestellungs-Protokolle",
                ThemeManager.getWarningColor(),
                LogCategory.SUPPLIER_ORDER);
        allLogsButton = createCategoryButton(UnicodeSymbols.CLIPBOARD + " Alle Protokolle",
                ThemeManager.getPrimaryColor(),
                LogCategory.ALL);

        JPanel categoryCard = createCardPanel(18, new BorderLayout(10, 10));
        categoryCard.add(buildCategoryPanel(), BorderLayout.CENTER);
        topContainer.add(categoryCard);
        topContainer.add(Box.createVerticalStrut(10));

        clearLogsButton = JFrameUtils.createThemeButton(UnicodeSymbols.TRASH + " Logs löschen", ThemeManager.getErrorColor());
        clearLogsButton.setToolTipText("Aktuelle Log-Kategorie löschen");
        clearLogsButton.addActionListener(e -> clearCurrentLogs());

        JButton refreshButton = JFrameUtils.createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.setToolTipText("Logs neu laden");
        refreshButton.addActionListener(e -> refreshCurrentLogs());

        JButton exportPdfButton = JFrameUtils.createSecondaryButton(UnicodeSymbols.DOCUMENT + " PDF Export");
        exportPdfButton.setToolTipText("Gefilterte Logs als PDF exportieren");
        exportPdfButton.addActionListener(e -> exportLogsToPdf(filteredLogs));

        JButton exportCsvButton = JFrameUtils.createSecondaryButton(UnicodeSymbols.DOWNLOAD + " CSV Export");
        exportCsvButton.setToolTipText("Gefilterte Logs als CSV exportieren");
        exportCsvButton.addActionListener(e -> exportLogsToCsv(filteredLogs));

        JButton clearFilterButton = JFrameUtils.createSecondaryButton(UnicodeSymbols.CLEAR + " Filter leeren");
        clearFilterButton.setToolTipText("Such- und Datumsfilter zurücksetzen");
        clearFilterButton.addActionListener(e -> clearFilters());

        JPanel controlsCard = createCardPanel(18, new BorderLayout(10, 10));
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
        viewerTitleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        viewerTitleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        setCategory(LogCategory.ORDER);
    }

    @Override
    public void dispose() {
        autoRefreshTimer.stop();
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
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
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Protokolle durchsuchen, filtern und exportieren");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(4));
        headerText.add(subtitleLabel);

        headerPanel.add(headerText, BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(headerPanel, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildCategoryPanel() {
        JPanel categoryPanel = new JPanel(new BorderLayout(10, 10));
        categoryPanel.setOpaque(false);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(orderLogsButton);
        buttonRow.add(supplierLogsButton);
        buttonRow.add(supplierOrderLogsButton);
        buttonRow.add(allLogsButton);

        JScrollPane buttonScrollPane = new JScrollPane(
                buttonRow,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        buttonScrollPane.setBorder(BorderFactory.createEmptyBorder());
        buttonScrollPane.setOpaque(false);
        buttonScrollPane.getViewport().setOpaque(false);
        buttonScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        badgeRow.setOpaque(false);
        badgeRow.add(categoryBadgeLabel);
        badgeRow.add(resultsBadgeLabel);

        categoryPanel.add(buttonScrollPane, BorderLayout.CENTER);
        categoryPanel.add(badgeRow, BorderLayout.EAST);
        return categoryPanel;
    }

    private JPanel buildFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        filterPanel.setOpaque(false);

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche:");
        styleLabel(searchLabel, Font.BOLD, ThemeManager.getTextPrimaryColor());

        JLabel fromLabel = new JLabel("Von:");
        styleLabel(fromLabel, Font.PLAIN, ThemeManager.getTextPrimaryColor());

        JLabel toLabel = new JLabel("Bis:");
        styleLabel(toLabel, Font.PLAIN, ThemeManager.getTextPrimaryColor());

        styleTextField(searchField);
        styleTextField(fromDateField);
        styleTextField(toDateField);

        searchField.setToolTipText("Textsuche in Logs");
        fromDateField.setToolTipText("Format: dd.MM.yyyy");
        toDateField.setToolTipText("Format: dd.MM.yyyy");

        filterPanel.add(searchLabel);
        filterPanel.add(searchField);
        filterPanel.add(fromLabel);
        filterPanel.add(fromDateField);
        filterPanel.add(toLabel);
        filterPanel.add(toDateField);
        return filterPanel;
    }

    private JPanel buildActionPanel(JCheckBox autoRefresh, JLabel updatedLabel, JButton refreshButton,
                                    JButton exportPdfButton, JButton exportCsvButton, JButton clearButton,
                                    JButton clearFilterButton) {
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        infoPanel.setOpaque(false);
        infoPanel.add(autoRefresh);
        infoPanel.add(updatedLabel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
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
        JPanel contentCard = createCardPanel(18, new BorderLayout());
        contentCard.add(buildViewerHeader(), BorderLayout.NORTH);

        logTextPane.setEditable(false);
        logTextPane.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        logTextPane.setBackground(ThemeManager.getInputBackgroundColor());
        logTextPane.setForeground(ThemeManager.getTextPrimaryColor());
        logTextPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

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
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JLabel helperLabel = new JLabel("Rechtsklick zum Kopieren • Datum: dd.MM.yyyy");
        helperLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        helperLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(viewerTitleLabel);
        leftPanel.add(Box.createVerticalStrut(3));
        leftPanel.add(helperLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(filterStateLabel);

        headerPanel.add(leftPanel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createCardPanel(int radius, LayoutManager layout) {
        JFrameUtils.RoundedPanel panel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), radius);
        panel.setLayout(layout);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        return panel;
    }

    private JButton createCategoryButton(String text, Color color, LogCategory category) {
        JButton button = JFrameUtils.createThemeButton(text, color);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setToolTipText(text);
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
        orderLogsButton.setEnabled(currentCategory != LogCategory.ORDER);
        supplierLogsButton.setEnabled(currentCategory != LogCategory.SUPPLIER);
        supplierOrderLogsButton.setEnabled(currentCategory != LogCategory.SUPPLIER_ORDER);
        allLogsButton.setEnabled(currentCategory != LogCategory.ALL);
        clearLogsButton.setEnabled(currentCategory != LogCategory.ALL);
        categoryBadgeLabel.setText("Kategorie: " + getCategoryDisplayName(currentCategory));
        viewerTitleLabel.setText(UnicodeSymbols.DOCUMENT + " " + getCategoryDisplayName(currentCategory));
    }

    private String getCategoryDisplayName(LogCategory category) {
        return switch (category) {
            case ORDER -> "Bestellungs-Protokolle";
            case SUPPLIER -> "Lieferanten-Protokolle";
            case SUPPLIER_ORDER -> "Lieferanten-Bestellungs-Protokolle";
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
        File supplierOrderFile = new File(Main.getAppDataDir(), "supplier_orders.txt");

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
        appendLogFileSection(allLogs, supplierOrderFile, "--- Lieferanten-Bestellungs-Protokolle ---");

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
        supplierOrderLogs.addAll(SupplierOrderGUI.getAllSupplierOrders());
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
        resultsBadgeLabel.setText("Treffer: " + filteredLogs.size() + " / " + currentLogs.size());
        filterStateLabel.setText(buildFilterSummary(query, fromDate, toDate));
        renderLogs(filteredLogs);
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

        switch (styleName) {
            case "info" -> StyleConstants.setForeground(style, ThemeManager.getInfoColor());
            case "warn" -> StyleConstants.setForeground(style, ThemeManager.getWarningColor());
            case "error" -> StyleConstants.setForeground(style, ThemeManager.getErrorColor());
            case "section" -> {
                StyleConstants.setBold(style, true);
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
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        JFrameUtils.styleTextField(field);
    }

    private void styleLabel(JLabel label, int style, Color color) {
        label.setFont(SettingsGUI.getFontByName(style, 12));
        label.setForeground(color);
    }

    private void styleBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }
}
