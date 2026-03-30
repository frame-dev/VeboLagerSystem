package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.JFrameUtils.RoundedPanel;
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.framedev.lagersystem.utils.JFrameUtils.createThemeButton;

/**
 * Modern edit order GUI with improved visual design and user experience.
 * Features: Split panel layout, gradient header, styled components, and article
 * caching.
 */
@SuppressWarnings({ "SwitchStatementWithTooFewBranches", "DuplicatedCode" })
public class EditOrderGUI extends JFrame {

    private static final Logger LOGGER = LogManager.getLogger(EditOrderGUI.class);

    private static final int PAD = 12;
    private static final int CARD_PAD = 14;
    private static final int RADIUS_HEADER = 20;
    private static final int RADIUS_CARD = 18;
    private static final int COL_ARTICLE_NUMBER = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_SIZE = 3;
    private static final int COL_FILLING = 4;
    private static final int COL_UNIT_PRICE = 5;

    private final Order order;
    private final JTextField receiverNameField;
    private final JTextField receiverKontoNumberField;
    private final JTextField orderDateField;
    private final JTextField senderNameField;
    private final JTextField senderKontoNumberField;
    private final JTextField departmentField;

    private final DefaultTableModel tableModel;
    private final JTable articlesTable;
    private final Map<String, Article> articleCache = new HashMap<>();
    private final List<String> rowOrderKeys = new ArrayList<>();
    private boolean updatingTableModel;

    /**
     * Initializes the EditOrderGUI with the given order, setting up the layout,
     * components, and event handlers for editing the order details and articles.
     * 
     * @param order the Order object containing the details to be edited in the GUI.
     */
    public EditOrderGUI(Order order) {
        this.order = order;

        ThemeManager.getInstance().registerWindow(this);

        setTitle("Bestellung Bearbeiten");
        setSize(1000, 700);
        setMinimumSize(new Dimension(900, 620));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Top area (VendorGUI style: header card + toolbar card)
        JPanel topPanel = JFrameUtils.createVerticalContainer(
                ThemeManager.getBackgroundColor(),
                new Insets(PAD, PAD, 8, PAD));

        JPanel headerCard = JFrameUtils.createHeaderWrapper(
                "Bestellung Bearbeiten",
                "Empfänger/Absender, Abteilung sowie Menge, Größe und Füllung anpassen",
                UnicodeSymbols.EDIT + " ",
                UnicodeSymbols.INFO + " ",
                22,
                12,
                4,
                RADIUS_HEADER,
                new Insets(14, 18, 14, 18),
                null);

        // Toolbar card
        RoundedPanel toolbarCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), RADIUS_CARD);
        toolbarCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JButton saveButtonTop = createThemeButton(UnicodeSymbols.FLOPPY + " Speichern", ThemeManager.getSuccessColor());
        saveButtonTop.setToolTipText("Änderungen speichern");
        saveButtonTop.addActionListener(e -> saveChanges());

        JButton cancelButtonTop = createThemeButton(UnicodeSymbols.CLOSE + " Abbrechen", ThemeManager.getErrorColor());
        cancelButtonTop.setToolTipText("Abbrechen und ohne Änderungen schließen");
        cancelButtonTop.addActionListener(e -> dispose());

        toolbarCard.add(saveButtonTop);
        toolbarCard.add(cancelButtonTop);

        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        topPanel.add(headerCard);
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(toolbarCard);

        add(topPanel, BorderLayout.NORTH);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBackground(ThemeManager.getBackgroundColor());
        mainContent.setBorder(BorderFactory.createEmptyBorder(10, PAD, PAD, PAD));

        // Left panel - Order details form
        RoundedPanel leftCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), RADIUS_CARD);
        leftCard.setBorder(BorderFactory.createEmptyBorder(CARD_PAD, CARD_PAD, CARD_PAD, CARD_PAD));
        leftCard.setLayout(new BorderLayout(10, 10));

        JLabel formTitle = new JLabel(UnicodeSymbols.CLIPBOARD + " Bestelldetails");
        formTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        formTitle.setForeground(ThemeManager.getTextPrimaryColor());
        leftCard.add(formTitle, BorderLayout.NORTH);

        // Form panel with GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;

        // Initialize fields
        JTextField orderIdField = new JTextField(safe(order.getOrderId()));
        orderIdField.setEditable(false);
        styleTextField(orderIdField);
        orderIdField.setBackground(ThemeManager.getInputDisabledBackgroundColor());
        orderIdField.setForeground(ThemeManager.getInputDisabledForegroundColor());

        receiverNameField = new JTextField(safe(order.getReceiverName()));
        styleTextField(receiverNameField);

        receiverKontoNumberField = new JTextField(safe(order.getReceiverKontoNumber()));
        styleTextField(receiverKontoNumberField);

        orderDateField = new JTextField(safe(order.getOrderDate()));
        styleTextField(orderDateField);

        senderNameField = new JTextField(safe(order.getSenderName()));
        styleTextField(senderNameField);

        senderKontoNumberField = new JTextField(safe(order.getSenderKontoNumber()));
        styleTextField(senderKontoNumberField);

        departmentField = new JTextField(order.getDepartment() != null ? order.getDepartment() : "");
        styleTextField(departmentField);

        // Add form rows
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.ID + " Bestell-ID:", orderIdField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.PERSON + " Empfänger Name:", receiverNameField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.CREDIT_CARD + " Empfänger Konto:", receiverKontoNumberField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.CALENDAR + " Bestelldatum:", orderDateField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.PERSON + " Absender Name:", senderNameField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.CREDIT_CARD + " Absender Konto:", senderKontoNumberField);
        addStyledFormRow(formPanel, gbc, UnicodeSymbols.DEPARTMENT + " Abteilung:", departmentField);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setBackground(ThemeManager.getCardBackgroundColor());
        formScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        leftCard.add(formScroll, BorderLayout.CENTER);

        // Right panel - Articles table
        RoundedPanel rightCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), RADIUS_CARD);
        rightCard.setBorder(BorderFactory.createEmptyBorder(CARD_PAD, CARD_PAD, CARD_PAD, CARD_PAD));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel tableTitle = new JLabel(UnicodeSymbols.PACKAGE + " Bestellte Artikel");
        tableTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        tableTitle.setForeground(ThemeManager.getTextPrimaryColor());
        rightCard.add(tableTitle, BorderLayout.NORTH);

        // Articles Table with modern styling
        String[] columnNames = { "Artikel", "Artikel-Nr.", "Menge", "Größe", "Füllung", "Einzelpreis" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_QUANTITY || column == COL_SIZE || column == COL_FILLING;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case COL_QUANTITY -> Integer.class;
                    default -> String.class;
                };
            }
        };

        articlesTable = new JTable(tableModel);
        applyTableTheme(articlesTable);
        tableModel.addTableModelListener(e -> handleTableUpdate(e));

        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        rightCard.add(scrollPane, BorderLayout.CENTER);

        // Bottom action panel (tip label only)
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel infoLabel = new JLabel(UnicodeSymbols.BULB + " Tipp: Doppelklick zum Bearbeiten von Menge, Größe oder Füllung");
        infoLabel.setFont(SettingsGUI.getFontByName(Font.ITALIC, 11));
        infoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        actionPanel.add(infoLabel, BorderLayout.WEST);

        rightCard.add(actionPanel, BorderLayout.SOUTH);

        // Add panels to split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCard, rightCard);
        splitPane.setDividerLocation(420);
        splitPane.setDividerSize(6);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        mainContent.add(splitPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Load articles
        loadArticles();

        installKeyboardShortcuts(saveButtonTop);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void installKeyboardShortcuts(JButton saveButton) {
        KeyboardShortcutUtils.addTooltipHint(saveButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_S));
        KeyboardShortcutUtils.registerClose(this);
        KeyboardShortcutUtils.registerButton(getRootPane(), "editOrder.save",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_S), saveButton);
    }

    private void applyTableTheme(JTable table) {
        if (table == null)
            throw new NullPointerException("table must not be null");
        table.setRowHeight(28);
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        table.setShowGrid(true);
        table.setGridColor(ThemeManager.getTableGridColor());
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        table.setFillsViewportHeight(true);

        JTableHeader th = table.getTableHeader();
        th.setBackground(ThemeManager.getTableHeaderBackgroundColor());
        th.setForeground(ThemeManager.getTableHeaderForegroundColor());
        th.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        th.setReorderingAllowed(false);

        DefaultTableCellRenderer qtyRenderer = new DefaultTableCellRenderer();
        qtyRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        if (table.getColumnModel().getColumnCount() > 1) {
            table.getColumnModel().getColumn(COL_QUANTITY).setCellRenderer(qtyRenderer);
        }

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        if (table.getColumnModel().getColumnCount() > COL_UNIT_PRICE) {
            table.getColumnModel().getColumn(COL_UNIT_PRICE).setCellRenderer(rightRenderer);
        }

        // Alternating rows + proper selection + theme-safe colors
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(ThemeManager.getSelectionBackgroundColor());
                    c.setForeground(ThemeManager.getSelectionForegroundColor());
                } else {
                    c.setBackground(
                            row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                    c.setForeground(ThemeManager.getTextPrimaryColor());
                }

                if (c instanceof JComponent jc) {
                    jc.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                }

                return c;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);

        // Editor styling for quantity column
        JTextField editorField = new JTextField();
        editorField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputFocusBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        editorField.setBackground(ThemeManager.getInputBackgroundColor());
        editorField.setForeground(ThemeManager.getTextPrimaryColor());
        editorField.setCaretColor(ThemeManager.getTextPrimaryColor());
        table.setDefaultEditor(Integer.class, new DefaultCellEditor(editorField));
        table.setDefaultEditor(String.class, new DefaultCellEditor(editorField));
    }

    private void styleTextField(JTextField field) {
        JFrameUtils.styleTextField(field);
    }

    private void addStyledFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        if (panel == null)
            throw new NullPointerException("panel must not be null");
        if (gbc == null)
            throw new NullPointerException("gbc must not be null");
        if (labelText == null)
            throw new NullPointerException("labelText must not be null");
        if (field == null)
            throw new NullPointerException("field must not be null");
        JLabel label = new JLabel(labelText);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        panel.add(label, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 8, 12, 8);
        panel.add(field, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
    }

    private void loadArticles() {
        updatingTableModel = true;
        tableModel.setRowCount(0);
        rowOrderKeys.clear();

        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        for (Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            String orderItemKey = entry.getKey();
            String articleNumber = ArticleUtils.getOrderItemArticleNumber(orderItemKey);
            Article article = articleCache.get(articleNumber);
            if (article == null) {
                article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
                if (article != null) {
                    articleCache.put(articleNumber, article);
                }
            }
            if (article == null) {
                continue;
            }

            int quantity = entry.getValue();
            String filling = normalizeFillingValue(order.getArticleFilling(orderItemKey));
            Object[] rowData = {
                    safe(article.getName()),
                    articleNumber,
                    quantity,
                    ArticleUtils.normalizeMetadataValue(order.getArticleSize(orderItemKey)),
                    filling,
                    formatPrice(resolveUnitPrice(article, filling))
            };
            tableModel.addRow(rowData);
            rowOrderKeys.add(orderItemKey);
        }
        updatingTableModel = false;
        LOGGER.info("Loaded {} order rows for order {}", rowOrderKeys.size(), safe(order.getOrderId()));
    }

    private void saveChanges() {
        if (articlesTable.isEditing()) {
            articlesTable.getCellEditor().stopCellEditing();
        }

        order.setReceiverName(receiverNameField.getText().trim());
        order.setReceiverKontoNumber(receiverKontoNumberField.getText().trim());
        order.setOrderDate(orderDateField.getText().trim());
        order.setSenderName(senderNameField.getText().trim());
        order.setSenderKontoNumber(senderKontoNumberField.getText().trim());
        order.setDepartment(departmentField.getText().trim());

        OrderTableData tableData;
        try {
            tableData = getTableData();
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("Validation failed while saving order {}: {}", safe(order.getOrderId()), ex.getMessage());
            OrderLoggingUtils.getInstance().addWarn(safe(order.getOrderId()),
                    "Bestellung konnte nicht gespeichert werden: " + ex.getMessage());
            new MessageDialog()
                    .setTitle("Ungültige Eingabe")
                    .setMessage(ex.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        order.setOrderedArticles(new LinkedHashMap<>(tableData.quantities));
        order.setArticleSizes(new LinkedHashMap<>(tableData.sizes));
        order.setArticleColors(new LinkedHashMap<>(tableData.colors));
        order.setArticleFillings(new LinkedHashMap<>(tableData.fillings));

        OrderManager orderManager = OrderManager.getInstance();
        boolean success = orderManager.updateOrder(order);

        if (!success) {
            LOGGER.error("Failed to update order {}", safe(order.getOrderId()));
            OrderLoggingUtils.getInstance().addError(safe(order.getOrderId()),
                    "Fehler beim Speichern der Bestellung");
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage(
                            "<html><b>Fehler beim Aktualisieren!</b><br/>Die Bestellung konnte nicht gespeichert werden.</html>")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        new MessageDialog()
                .setTitle("Erfolg")
                .setMessage(
                        "<html><b>OK Erfolgreich gespeichert!</b><br/>Die Bestellung wurde aktualisiert.</html>")
                .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                .display();
        LOGGER.info("Updated order {} with {} line items", safe(order.getOrderId()), tableData.quantities.size());
        OrderLoggingUtils.getInstance().addInfo(safe(order.getOrderId()),
                "Bestellung aktualisiert (" + tableData.quantities.size() + " Positionen)");
        firePropertyChange("orderUpdated", null, order);
        dispose();
    }

    /**
     * Extracts article numbers and quantities from the table model.
     * 
     * @return a map of article numbers to their corresponding quantities based on
     *         the current table data.
     */
    public OrderTableData getTableData() {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        Map<String, String> sizes = new LinkedHashMap<>();
        Map<String, String> colors = new LinkedHashMap<>();
        Map<String, String> fillings = new LinkedHashMap<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String originalOrderKey = getOrderKeyForRow(i);
            String articleNumber = ArticleUtils.getOrderItemArticleNumber(originalOrderKey);
            if (articleNumber.isBlank()) {
                articleNumber = cellText(i, COL_ARTICLE_NUMBER);
            }
            if (articleNumber.isBlank()) {
                throw new IllegalArgumentException("Artikelnummer fehlt in Zeile " + (i + 1) + ".");
            }

            int quantity = parseQuantityForSave(i);
            String size = ArticleUtils.normalizeMetadataValue(cellText(i, COL_SIZE));
            String filling = normalizeFillingValue(cellText(i, COL_FILLING));
            if (!filling.isBlank()) {
                if (!ArticleUtils.isFillingValid(filling)) {
                    throw new IllegalArgumentException("Ungültige Füllung in Zeile " + (i + 1)
                            + ". Erlaubt sind z.B. \"500 ml\" oder \"1 l\".");
                }
            }

            String color = ArticleUtils.normalizeMetadataValue(order.getArticleColor(originalOrderKey));
            String orderItemKey = ArticleUtils.buildOrderItemKey(articleNumber, size, color, filling);
            if (orderItemKey.isBlank()) {
                throw new IllegalArgumentException("Artikelnummer fehlt in Zeile " + (i + 1) + ".");
            }

            quantities.merge(orderItemKey, quantity, Integer::sum);
            if (!size.isBlank()) {
                sizes.put(orderItemKey, size);
            }
            if (!color.isBlank()) {
                colors.put(orderItemKey, color);
            }
            if (!filling.isBlank()) {
                fillings.put(orderItemKey, filling);
            }
        }

        return new OrderTableData(quantities, sizes, colors, fillings);
    }

    private void handleTableUpdate(TableModelEvent event) {
        if (updatingTableModel || event.getType() != TableModelEvent.UPDATE) {
            return;
        }

        int firstRow = event.getFirstRow();
        int lastRow = event.getLastRow();
        if (firstRow < 0 || lastRow < firstRow) {
            refreshAllPrices();
            return;
        }

        for (int row = firstRow; row <= lastRow; row++) {
            refreshPriceForRow(row);
        }
    }

    private void refreshAllPrices() {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            refreshPriceForRow(row);
        }
    }

    private void refreshPriceForRow(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) {
            return;
        }

        Article article = getArticleForRow(row);
        if (article == null) {
            return;
        }

        String filling = normalizeFillingValue(cellText(row, COL_FILLING));
        updatingTableModel = true;
        tableModel.setValueAt(formatPrice(resolveUnitPrice(article, filling)), row, COL_UNIT_PRICE);
        updatingTableModel = false;
    }

    private Article getArticleForRow(int row) {
        String articleNumber = cellText(row, COL_ARTICLE_NUMBER);
        if (articleNumber.isBlank()) {
            return null;
        }
        Article article = articleCache.get(articleNumber);
        if (article == null) {
            article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
            if (article != null) {
                articleCache.put(articleNumber, article);
            }
        }
        return article;
    }

    private int parseQuantityForSave(int row) {
        Object qtyObj = tableModel.getValueAt(row, COL_QUANTITY);
        int quantity;
        try {
            if (qtyObj instanceof Integer integer) {
                quantity = integer;
            } else {
                quantity = Integer.parseInt(String.valueOf(qtyObj).trim());
            }
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Ungültige Menge in Zeile " + (row + 1) + ".");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("Die Menge muss in Zeile " + (row + 1) + " größer als 0 sein.");
        }
        return quantity;
    }

    private double resolveUnitPrice(Article article, String filling) {
        try {
            return ArticleUtils.resolveEffectiveSellPrice(article, filling);
        } catch (RuntimeException ex) {
            return article == null ? 0.0 : article.getSellPrice();
        }
    }

    private String formatPrice(double price) {
        return String.format("%.2f CHF", price);
    }

    private String cellText(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString().trim();
    }

    private String normalizeFillingValue(String filling) {
        String normalized = ArticleUtils.normalizeFilling(filling);
        return normalized == null ? "" : normalized;
    }

    private String getOrderKeyForRow(int row) {
        if (row < 0 || row >= rowOrderKeys.size()) {
            return "";
        }
        return rowOrderKeys.get(row);
    }

    /**
     * Displays the EditOrderGUI window on the Event Dispatch Thread to ensure
     * thread safety and proper GUI rendering.
     */
    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    /**
     * Utility method to safely handle null strings by converting them to empty
     * strings, preventing potential NullPointerExceptions when setting text fields.
     * 
     * @param s the input string that may be null
     * @return the original string if it's not null, or an empty string if the input
     *         is null
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public static final class OrderTableData {
        private final Map<String, Integer> quantities;
        private final Map<String, String> sizes;
        private final Map<String, String> colors;
        private final Map<String, String> fillings;

        private OrderTableData(Map<String, Integer> quantities, Map<String, String> sizes,
                               Map<String, String> colors, Map<String, String> fillings) {
            this.quantities = quantities;
            this.sizes = sizes;
            this.colors = colors;
            this.fillings = fillings;
        }
    }
}
