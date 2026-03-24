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
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
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

    private static final int PAD = 12;
    private static final int CARD_PAD = 14;
    private static final int RADIUS_HEADER = 20;
    private static final int RADIUS_CARD = 18;

    private final Order order;
    private final JTextField receiverNameField;
    private final JTextField receiverKontoNumberField;
    private final JTextField orderDateField;
    private final JTextField senderNameField;
    private final JTextField senderKontoNumberField;
    private final JTextField departmentField;

    private final DefaultTableModel tableModel;
    private final Map<String, Article> articleCache = new HashMap<>();

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
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.setBorder(BorderFactory.createEmptyBorder(PAD, PAD, 8, PAD));

        // Header card
        RoundedPanel headerCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), RADIUS_HEADER);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel titleLabel = new JLabel(UnicodeSymbols.EDIT + " Bestellung Bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(
                UnicodeSymbols.INFO + " Empfänger/Absender, Abteilung und Artikelmengen anpassen");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(titleLabel);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitleLabel);

        headerCard.add(titleBox, BorderLayout.WEST);

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
        String[] columnNames = { "Artikel", "Menge", "Einzelpreis" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only quantity is editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        JTable articlesTable = new JTable(tableModel);
        applyTableTheme(articlesTable);

        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        rightCard.add(scrollPane, BorderLayout.CENTER);

        // Bottom action panel (tip label only)
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel infoLabel = new JLabel(UnicodeSymbols.BULB + " Tipp: Doppelklick zum Bearbeiten der Menge");
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

        getRootPane().registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
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
            table.getColumnModel().getColumn(1).setCellRenderer(qtyRenderer);
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
        tableModel.setRowCount(0);

        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        List<Article> articles = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            String articleNumber = entry.getKey();
            Article article = articleCache.get(articleNumber);
            if (article == null) {
                article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
                if (article != null) {
                    articleCache.put(articleNumber, article);
                }
            }
            if (article != null) {
                articles.add(article);
            }
        }

        for (Article article : articles) {
            int quantity = orderedArticles.getOrDefault(article.getArticleNumber(), 0);
            String articleLabel = ArticleUtils.formatArticleWithFilling(article, order.getArticleFilling(article.getArticleNumber()));
            Object[] rowData = {
                    articleLabel + " (" + article.getArticleNumber() + ")",
                    quantity,
                    String.format("%.2f CHF", ArticleUtils.resolveEffectiveSellPrice(article, order.getArticleFilling(article.getArticleNumber())))
            };
            tableModel.addRow(rowData);
        }
    }

    private void saveChanges() {
        order.setReceiverName(receiverNameField.getText().trim());
        order.setReceiverKontoNumber(receiverKontoNumberField.getText().trim());
        order.setOrderDate(orderDateField.getText().trim());
        order.setSenderName(senderNameField.getText().trim());
        order.setSenderKontoNumber(senderKontoNumberField.getText().trim());
        order.setDepartment(departmentField.getText().trim());

        Map<String, Integer> tableData = getTableData();
        order.setOrderedArticles(new HashMap<>(tableData));

        OrderManager orderManager = OrderManager.getInstance();
        boolean success = orderManager.updateOrder(order);

        if (!success) {
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
        dispose();
    }

    /**
     * Extracts article numbers and quantities from the table model.
     * 
     * @return a map of article numbers to their corresponding quantities based on
     *         the current table data.
     */
    public Map<String, Integer> getTableData() {
        Map<String, Integer> data = new HashMap<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String articleNameWithNumber = (String) tableModel.getValueAt(i, 0);

            String articleNumber = null;
            int openParen = articleNameWithNumber.lastIndexOf('(');
            int closeParen = articleNameWithNumber.lastIndexOf(')');
            if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                articleNumber = articleNameWithNumber.substring(openParen + 1, closeParen).trim();
            }

            int quantity;
            try {
                Object qtyObj = tableModel.getValueAt(i, 1);
                if (qtyObj instanceof Integer) {
                    quantity = (Integer) qtyObj;
                } else {
                    quantity = Integer.parseInt(qtyObj.toString());
                }
            } catch (NumberFormatException | ClassCastException e) {
                System.err.println("Invalid quantity at row " + i + ": " + tableModel.getValueAt(i, 1));
                continue;
            }

            if (quantity <= 0) {
                continue;
            }

            if (articleNumber != null && !articleNumber.isEmpty()) {
                data.put(articleNumber, quantity);
            } else {
                String articleName = articleNameWithNumber;
                if (openParen != -1) {
                    articleName = articleNameWithNumber.substring(0, openParen).trim();
                }
                Article article = ArticleManager.getInstance().getArticleByName(articleName);
                if (article != null) {
                    data.put(article.getArticleNumber(), quantity);
                }
            }
        }

        return data;
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
}
