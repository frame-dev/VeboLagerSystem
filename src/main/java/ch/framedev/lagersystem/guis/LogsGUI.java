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

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

public class LogsGUI extends JFrame {

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

    public LogsGUI() {
        setTitle(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Main panel with padding and background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        add(mainPanel, BorderLayout.CENTER);

        // Header panel with gradient and shadow
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, ThemeManager.getHeaderBackgroundColor(), getWidth(), 0, ThemeManager.getHeaderGradientColor());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(0,0,0,30));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Logs Übersicht");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 30));
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Protokolle und Systemereignisse anzeigen");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 17));
        subtitleLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 230));
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);

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
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        filterPanel.add(searchLabel);
        searchField.setToolTipText("Textsuche in Logs");
        filterPanel.add(searchField);
        JLabel fromLabel = new JLabel("Von (dd.MM.yyyy):");
        fromLabel.setForeground(ThemeManager.getTextPrimaryColor());
        filterPanel.add(fromLabel);
        fromDateField.setToolTipText("z.B. 01.01.2024");
        filterPanel.add(fromDateField);
        JLabel toLabel = new JLabel("Bis (dd.MM.yyyy):");
        toLabel.setForeground(ThemeManager.getTextPrimaryColor());
        filterPanel.add(toLabel);
        toDateField.setToolTipText("z.B. 31.12.2024");
        filterPanel.add(toDateField);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        actionPanel.setOpaque(false);
        JButton exportPdfButton = new JButton("PDF Export");
        exportPdfButton.addActionListener(e -> exportLogsToPdf(filteredLogs));
        JButton exportCsvButton = new JButton("CSV Export");
        exportCsvButton.addActionListener(e -> exportLogsToCsv(filteredLogs));
        JButton clearButton = new JButton("Logs loeschen");
        clearButton.addActionListener(e -> clearCurrentLogs());
        autoRefreshCheckBox.setOpaque(false);
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

        topPanel.add(toolbarPanel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Card-like content panel for logs (fills all remaining space)
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.setColor(new Color(0,0,0,10));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        logTextPane.setEditable(false);
        logTextPane.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        logTextPane.setBackground(ThemeManager.getInputBackgroundColor());
        logTextPane.setForeground(ThemeManager.getTextPrimaryColor());
        logTextPane.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        JScrollPane scrollPane = new JScrollPane(logTextPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        initPopupMenu();

        searchField.getDocument().addDocumentListener(new DocumentListener() {
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
        });

        fromDateField.getDocument().addDocumentListener(new DocumentListener() {
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
        });

        toDateField.getDocument().addDocumentListener(new DocumentListener() {
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
        });

        // Button hover effects with smooth color transitions
        for(JButton button : new JButton[]{orderLogsButton, supplierLogsButton, supplierOrderLogsButton, allLogsButton}) {
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

    private void allLogs(ActionEvent actionEvent) {
        File allLogsFile = new File(new File(Main.getAppDataDir(), "logs"), "vebo_lager_system.log");
        File orderLogFile = new File(new File(Main.getAppDataDir(), "logs"), "bestellung.log");
        File vendorOrderFile = new File(new File(Main.getAppDataDir(), "logs"), "vendorOrder.log");
        File supplierOrderFile = new File(new File(Main.getAppDataDir(), "logs"), "supplier_orders.txt");
        List<String> allLogs = new ArrayList<>();
        if(allLogsFile.exists()) {
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
        lastUpdatedLabel.setText("Letzte Aktualisierung: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
    }

    /**
     * Helper method to style buttons consistently
     */
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)
        ));
        button.setPreferredSize(new Dimension(290, 48));
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
        lastUpdatedLabel.setText("Letzte Aktualisierung: " + new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()));
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
            JOptionPane.showMessageDialog(this,
                    "Keine Logs zum Export vorhanden.",
                    "CSV Export",
                    JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this,
                    "CSV erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "CSV Export",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim CSV-Export: " + ex.getMessage(),
                    "CSV Export",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportLogsToPdf(List<String> logs) {
        if (logs == null || logs.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Keine Logs zum Export vorhanden.",
                    "PDF Export",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("Logs_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont;
            File arial = new File("/Library/Fonts/Arial.ttf");
            if (arial.exists()) {
                regularFont = PDType0Font.load(doc, arial);
            } else {
                try {
                    Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                    Object helv = c.getField("HELVETICA").get(null);
                    regularFont = (PDFont) helv;
                } catch (Exception e) {
                    throw new IOException("Keine verwendbaren Schriftarten gefunden");
                }
            }

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40f;
            float fontSize = 10f;
            float lineHeight = 14f;
            float yStart = pageSize.getHeight() - margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.setFont(regularFont, fontSize);

            float y = yStart;
            for (String line : logs) {
                List<String> wrapped = wrapLine(line, regularFont, fontSize, pageSize.getWidth() - 2 * margin);
                for (String part : wrapped) {
                    if (y - lineHeight < margin) {
                        contentStream.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        contentStream = new PDPageContentStream(doc, page);
                        contentStream.setFont(regularFont, fontSize);
                        y = yStart;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText(part);
                    contentStream.endText();
                    y -= lineHeight;
                }
            }

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(this,
                    "PDF erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "PDF Export",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim PDF-Export: " + ex.getMessage(),
                    "PDF Export",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private List<String> wrapLine(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.length() == 0 ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width > maxWidth && current.length() > 0) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private void clearCurrentLogs() {
        String categoryName = switch (currentCategory) {
            case ORDER -> "Bestellungs-Protokolle";
            case SUPPLIER -> "Lieferanten-Protokolle";
            case SUPPLIER_ORDER -> "Lieferanten-Bestellungs-Protokolle";
        };
        int confirm = JOptionPane.showConfirmDialog(this,
                "Moechten Sie alle " + categoryName + " loeschen?",
                "Logs loeschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
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
                logFile.getParentFile().mkdirs();
            }
            Files.writeString(logFile.toPath(), "", StandardCharsets.UTF_8);
            refreshCurrentLogs();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Loeschen der Logs: " + ex.getMessage(),
                    "Logs loeschen",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
