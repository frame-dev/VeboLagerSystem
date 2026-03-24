package ch.framedev.lagersystem.guis;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.*;
import ch.framedev.lagersystem.managers.ThemeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The LogsGUI class provides a graphical user interface for viewing, filtering,
 * and managing logs related to orders, suppliers, and supplier orders in the
 * inventory system. It allows users to switch between different log categories,
 * apply text and date filters, export logs to PDF or CSV formats, and clear
 * logs. The GUI is designed with a modern look using custom styling and themes,
 * and it includes features such as auto-refreshing logs and a context menu for
 * copying log entries. The class interacts with various logging utilities to
 * retrieve and manage log data, and it uses Log4j for logging any errors that
 * occur within the GUI.
 * 
 * @author framedev
 */
public class LogsGUI extends JFrame {

    private final Logger logger = LogManager.getLogger(LogsGUI.class);

    private final List<String> orderLogsData = new ArrayList<>();
    private final List<String> supplierLogsData = new ArrayList<>();
    private final List<String> supplierOrderLogs = new ArrayList<>();
    private final JTextPane logTextPane = new JTextPane();
    private final JTextField searchField = new JTextField(18);
    private final JTextField fromDateField = new JTextField(8);
    private final JTextField toDateField = new JTextField(8);
    private final JLabel lastUpdatedLabel = new JLabel("Letzte Aktualisierung: -");
    private final JCheckBox autoRefreshCheckBox = new JCheckBox("Auto-Refresh");
    private final Timer autoRefreshTimer;
    private List<String> currentLogs = new ArrayList<>();
    private List<String> filteredLogs = new ArrayList<>();
    private LogCategory currentCategory = LogCategory.ORDER;

    private enum LogCategory {
        ORDER,
        SUPPLIER,
        SUPPLIER_ORDER
    }

    /**
     * Initializes the LogsGUI by setting up the user interface components,
     * including the header, buttons for log categories, filter fields, action
     * buttons, and the main content area for displaying logs. It also attaches
     * listeners for filtering logs based on user input and sets up a timer for
     * auto-refreshing logs if enabled. The constructor loads the initial set of
     * logs for the default category (ORDER) and applies the necessary styles and
     * themes to the components.
     */
    public LogsGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        setTitle(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        setSize(900, 650);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Main panel with padding and background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        add(mainPanel, BorderLayout.CENTER);

        // ===== Top area (Header + Toolbar) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        // mainPanel already provides outer padding; keep only vertical spacing here
        topContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel headerWrapper = null;
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (!disableHeader) {

            // Header card (VendorGUI-style)
            JFrameUtils.RoundedPanel headerPanel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(),
                    20);
            headerPanel.setLayout(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Protokolle und Systemereignisse anzeigen");
            subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

            JPanel headerText = new JPanel();
            headerText.setOpaque(false);
            headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerText.add(titleLabel);
            headerText.add(Box.createVerticalStrut(4));
            headerText.add(subtitleLabel);

            headerPanel.add(headerText, BorderLayout.WEST);

            // BoxLayout: use a standard wrapper so the header stretches to the left
            // edge/full width
            headerWrapper = new JPanel(new BorderLayout());
            headerWrapper.setOpaque(false);
            headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerWrapper.add(headerPanel, BorderLayout.CENTER);

            Dimension hpPref = headerPanel.getPreferredSize();
            headerWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, hpPref.height));
        }

        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }

        // Button panel for log categories (floating card look)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 12));
        buttonPanel.setOpaque(false);
        JButton orderLogsButton = new JButton(UnicodeSymbols.PACKAGE + " Bestellungs-Protokolle");
        styleButton(orderLogsButton, ThemeManager.getSuccessColor(), ThemeManager.getTextOnPrimaryColor());
        orderLogsButton.addActionListener(e -> setCategory(LogCategory.ORDER));
        JButton supplierLogsButton = new JButton(UnicodeSymbols.TRUCK + " Lieferanten-Protokolle");
        styleButton(supplierLogsButton, ThemeManager.getAccentColor(), ThemeManager.getTextOnPrimaryColor());
        supplierLogsButton.addActionListener(e -> setCategory(LogCategory.SUPPLIER));
        JButton supplierOrderLogsButton = new JButton(UnicodeSymbols.DOCUMENT + " Lieferanten-Bestellungs-Protokolle");
        styleButton(supplierOrderLogsButton, ThemeManager.getWarningColor(), ThemeManager.getTextOnPrimaryColor());
        supplierOrderLogsButton.addActionListener(e -> setCategory(LogCategory.SUPPLIER_ORDER));
        // All Logs Button
        JButton allLogsButton = new JButton(UnicodeSymbols.CLIPBOARD + " Alle Protokolle");
        styleButton(allLogsButton, ThemeManager.getPrimaryColor(), ThemeManager.getTextOnPrimaryColor());
        allLogsButton.addActionListener(this::allLogs);
        buttonPanel.add(supplierOrderLogsButton);
        buttonPanel.add(orderLogsButton);
        buttonPanel.add(supplierLogsButton);
        buttonPanel.add(allLogsButton);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        filterPanel.setOpaque(false);
        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche:");
        styleLabel(searchLabel, Font.BOLD, ThemeManager.getTextPrimaryColor());
        filterPanel.add(searchLabel);
        searchField.setToolTipText("Textsuche in Logs");
        styleTextField(searchField);
        filterPanel.add(searchField);
        JLabel fromLabel = new JLabel("Von (dd.MM.yyyy):");
        styleLabel(fromLabel, Font.PLAIN, ThemeManager.getTextPrimaryColor());
        filterPanel.add(fromLabel);
        fromDateField.setToolTipText("z.B. 01.01.2024");
        styleTextField(fromDateField);
        filterPanel.add(fromDateField);
        JLabel toLabel = new JLabel("Bis (dd.MM.yyyy):");
        styleLabel(toLabel, Font.PLAIN, ThemeManager.getTextPrimaryColor());
        filterPanel.add(toLabel);
        toDateField.setToolTipText("z.B. 31.12.2024");
        styleTextField(toDateField);
        filterPanel.add(toDateField);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actionPanel.setOpaque(false);
        JButton exportPdfButton = new JButton(UnicodeSymbols.DOCUMENT + " PDF Export");
        styleActionButton(exportPdfButton, ThemeManager.getAccentColor());
        exportPdfButton.addActionListener(e -> exportLogsToPdf(filteredLogs));

        JButton exportCsvButton = new JButton(UnicodeSymbols.DOWNLOAD + " CSV Export");
        styleActionButton(exportCsvButton, ThemeManager.getPrimaryColor());
        exportCsvButton.addActionListener(e -> exportLogsToCsv(filteredLogs));

        JButton clearButton = new JButton(UnicodeSymbols.TRASH + " Logs löschen");
        styleActionButton(clearButton, ThemeManager.getErrorColor());
        clearButton.addActionListener(e -> clearCurrentLogs());

        autoRefreshCheckBox.setOpaque(false);
        autoRefreshCheckBox.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        autoRefreshCheckBox.setForeground(ThemeManager.getTextPrimaryColor());
        lastUpdatedLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        lastUpdatedLabel.setForeground(ThemeManager.getTextSecondaryColor());
        actionPanel.add(autoRefreshCheckBox);
        actionPanel.add(lastUpdatedLabel);
        actionPanel.add(exportPdfButton);
        actionPanel.add(exportCsvButton);
        actionPanel.add(clearButton);

        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setOpaque(false);
        toolbarPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        toolbarPanel.add(buttonPanel, BorderLayout.NORTH);
        toolbarPanel.add(filterPanel, BorderLayout.CENTER);
        toolbarPanel.add(actionPanel, BorderLayout.SOUTH);

        JPanel toolbarCard = createCardWrapper(toolbarPanel);
        toolbarCard.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        toolbarCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension tbPref = toolbarCard.getPreferredSize();
        toolbarCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, tbPref.height));

        topContainer.add(toolbarCard);
        mainPanel.add(topContainer, BorderLayout.NORTH);

        // Card-like content panel for logs (fills all remaining space)
        JPanel contentPanel = getContentPanel();
        logTextPane.setEditable(false);
        logTextPane.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        logTextPane.setBackground(ThemeManager.getInputBackgroundColor());
        logTextPane.setForeground(ThemeManager.getTextPrimaryColor());
        logTextPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JScrollPane scrollPane = new JScrollPane(logTextPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(ThemeManager.getInputBackgroundColor());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        initPopupMenu();

        attachFilterListeners();

        // Button hover effects with smooth color transitions
        for (JButton button : new JButton[] { orderLogsButton, supplierLogsButton, supplierOrderLogsButton,
                allLogsButton }) {
            if (button == null)
                continue;
            button.addMouseListener(new MouseAdapter() {
                final Color orig = button.getBackground();
                final Color hover = orig.brighter();
                final Color pressed = orig.darker();

                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(hover);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(orig);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    button.setBackground(pressed);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    button.setBackground(button.contains(e.getPoint()) ? hover : orig);
                }
            });
        }

        autoRefreshTimer = new Timer(30000, e -> refreshCurrentLogs());
        autoRefreshTimer.setRepeats(true);
        autoRefreshCheckBox.addActionListener(e -> {
            if (autoRefreshCheckBox.isSelected()) {
                autoRefreshTimer.start();
            } else {
                autoRefreshTimer.stop();
            }
        });

        setCategory(LogCategory.ORDER);
    }

    @Override
    public void dispose() {
        autoRefreshTimer.stop();
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private static JPanel getContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getCardBackgroundColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(4, getHeight() - 8, getWidth() - 8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return contentPanel;
    }

    private void allLogs(ActionEvent actionEvent) {
        File allLogsFile = new File(new File(Main.getAppDataDir(), "logs"), "vebo_lager_system.log");
        File orderLogFile = new File(new File(Main.getAppDataDir(), "logs"), "bestellung.log");
        File vendorOrderFile = new File(new File(Main.getAppDataDir(), "logs"), "vendorOrder.log");
        File supplierOrderFile = new File(new File(Main.getAppDataDir(), "logs"), "supplier_orders.txt");
        List<String> allLogs = new ArrayList<>();
        if (allLogsFile.exists()) {
            try {
                allLogs.add("--- Haupt-Protokolle ---");
                allLogs.addAll(Files.readAllLines(allLogsFile.toPath(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        if (orderLogFile.exists()) {
            try {
                allLogs.add("--- Bestellungs-Protokolle ---");
                allLogs.addAll(Files.readAllLines(orderLogFile.toPath(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        if (vendorOrderFile.exists()) {
            try {
                allLogs.add("--- Lieferanten-Protokolle ---");
                allLogs.addAll(Files.readAllLines(vendorOrderFile.toPath(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        if (supplierOrderFile.exists()) {
            try {
                allLogs.add("--- Lieferanten-Bestellungs-Protokolle ---");
                allLogs.addAll(Files.readAllLines(supplierOrderFile.toPath(), StandardCharsets.UTF_8));
            } catch (IOException ignored) {
            }
        }
        currentLogs = allLogs;
        applyFilters();
        lastUpdatedLabel
                .setText("Letzte Aktualisierung: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
    }

    /**
     * Helper method to style buttons consistently
     */
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        if (button == null)
            return;
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setForeground(fgColor);
        button.setPreferredSize(new Dimension(260, 46));
        JFrameUtils.applyButtonPalette(button, bgColor);
    }

    // ---- ADDED HELPER METHODS ----
    private void styleTextField(JTextField field) {
        if (field == null)
            return;
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        JFrameUtils.styleTextField(field);
    }

    private void styleLabel(JLabel label, int style, Color color) {
        if (label == null)
            return;
        label.setFont(SettingsGUI.getFontByName(style, 12));
        label.setForeground(color);
    }

    private void styleActionButton(JButton button, Color bg) {
        if (button == null)
            return;
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        JFrameUtils.applyButtonPalette(button, bg);
    }

    private JPanel createCardWrapper(JComponent inner) {
        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        JPanel painted = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getCardBackgroundColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(ThemeManager.withAlpha(Color.BLACK, 12));
                g2.fillRoundRect(4, getHeight() - 8, getWidth() - 8, 8, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        painted.setOpaque(false);
        painted.add(inner, BorderLayout.CENTER);
        card.add(painted, BorderLayout.CENTER);
        return card;
    }

    private void attachFilterListeners() {
        DocumentListener dl = new DocumentListener() {
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
        searchField.getDocument().addDocumentListener(dl);
        fromDateField.getDocument().addDocumentListener(dl);
        toDateField.getDocument().addDocumentListener(dl);
    }

    private void setCategory(LogCategory category) {
        currentCategory = category;
        refreshCurrentLogs();
    }

    private void refreshCurrentLogs() {
        currentLogs = switch (currentCategory) {
            case ORDER -> loadOrderLogs();
            case SUPPLIER -> loadSupplierLogs();
            case SUPPLIER_ORDER -> loadSupplierOrderLogs();
        };
        applyFilters();
        lastUpdatedLabel
                .setText("Letzte Aktualisierung: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
    }

    private List<String> loadOrderLogs() {
        OrderLoggingUtils orderLoggingUtils = OrderLoggingUtils.getInstance();
        orderLogsData.clear();
        orderLogsData.addAll(orderLoggingUtils.getAllLogs());
        return new ArrayList<>(orderLogsData);
    }

    private List<String> loadSupplierLogs() {
        VendorOrderLogging vendorOrderLogging = VendorOrderLogging.getInstance();
        supplierLogsData.clear();
        supplierLogsData.addAll(vendorOrderLogging.getLogs());
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
        renderLogs(filteredLogs);
    }

    private void renderLogs(List<String> logs) {
        StyledDocument doc = logTextPane.getStyledDocument();
        doc.putProperty("filterNewlines", Boolean.TRUE);
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ignored) {
        }

        if (logs == null || logs.isEmpty()) {
            appendStyledLine(doc, "Keine Logs vorhanden.", "default");
            return;
        }

        for (String line : logs) {
            appendStyledLine(doc, line, resolveStyleName(line));
        }
    }

    private void appendStyledLine(StyledDocument doc, String line, String styleName) {
        Style style = getStyle(styleName);
        try {
            doc.insertString(doc.getLength(), line + System.lineSeparator(), style);
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

        if ("info".equals(styleName)) {
            StyleConstants.setForeground(style, ThemeManager.getInfoColor());
        } else if ("warn".equals(styleName)) {
            StyleConstants.setForeground(style, ThemeManager.getWarningColor());
        } else if ("error".equals(styleName)) {
            StyleConstants.setForeground(style, ThemeManager.getErrorColor());
        }
        return style;
    }

    private String resolveStyleName(String line) {
        if (line == null)
            return "default";
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
        DateTimeFormatter[] formats = new DateTimeFormatter[] {
                DateTimeFormatter.ofPattern("dd.MM.yyyy"),
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
        Pattern pattern = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})|(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = pattern.matcher(logEntry);
        if (!matcher.find()) {
            return null;
        }
        String dateText = matcher.group();
        return parseDateInput(dateText);
    }

    private void initPopupMenu() {
        JPopupMenu popupMenu = getPopupMenu();

        logTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private JPopupMenu getPopupMenu() {
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
        return popupMenu;
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
        fileChooser.setSelectedFile(
                new File("Logs_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));
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
        String categoryName = switch (currentCategory) {
            case ORDER -> "Bestellungs-Protokolle";
            case SUPPLIER -> "Lieferanten-Protokolle";
            case SUPPLIER_ORDER -> "Lieferanten-Bestellungs-Protokolle";
        };
        int confirm = new MessageDialog()
                .setTitle("Logs löschen")
                .setMessage("Möchten Sie alle " + categoryName + " löschen?")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .setOptions(new String[] { "Ja, löschen", "Abbrechen" })
                .displayWithOptions();
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        File logFile = switch (currentCategory) {
            case ORDER -> new File(new File(Main.getAppDataDir(), "logs"), "bestellung.log");
            case SUPPLIER -> new File(new File(Main.getAppDataDir(), "logs"), "vendorOrder.log");
            case SUPPLIER_ORDER -> new File(Main.getAppDataDir(), "supplier_orders.txt");
        };

        try {
            if (logFile.getParentFile() != null && !logFile.getParentFile().exists()) {
                if (!logFile.getParentFile().mkdirs()) {
                    logger.error("Could not create Parent file: {}", logFile.getParentFile().getAbsolutePath());
                    throw new IOException("Fehler beim Erstellen der Verzeichnisse");
                }
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
}
