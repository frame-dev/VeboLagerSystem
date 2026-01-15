package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modern edit order GUI with improved visual design and user experience.
 * Features: Split panel layout, gradient header, styled components, and article caching.
 */
public class EditOrderGUI extends JFrame {

    private final Order order;
    private final JTextField receiverNameField;
    private final JTextField receiverKontoNumberField;
    private final JTextField orderDateField;
    private final JTextField senderNameField;
    private final JTextField senderKontoNumberField;
    private final JTextField departmentField;

    private final DefaultTableModel tableModel;
    private final Map<String, Article> articleCache = new HashMap<>();

    public EditOrderGUI(Order order) {
        this.order = order;

        setTitle("Bestellung Bearbeiten");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        setLayout(new BorderLayout(0, 0));

        // Gradient header with icon
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GradientPanel header = new GradientPanel(
            ThemeManager.getHeaderBackgroundColor(),
            ThemeManager.getHeaderBackgroundColor().brighter()
        );
        header.setPreferredSize(new Dimension(850, 80));
        header.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("✏️");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 36f));
        iconLabel.setForeground(Color.WHITE);

        JLabel title = new JLabel("  Bestellung Bearbeiten");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(Color.WHITE);

        JPanel headerContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        headerContent.setOpaque(false);
        headerContent.add(iconLabel);
        headerContent.add(title);
        header.add(headerContent);

        headerWrapper.add(header);
        add(headerWrapper, BorderLayout.NORTH);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(ThemeManager.getBackgroundColor());
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Left panel - Order details form
        RoundedPanel leftCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        leftCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftCard.setLayout(new BorderLayout(10, 10));

        JLabel formTitle = new JLabel("📋 Bestelldetails");
        formTitle.setFont(formTitle.getFont().deriveFont(Font.BOLD, 17f));
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
        JTextField orderIdField = new JTextField(order.getOrderId());
        orderIdField.setEditable(false);
        orderIdField.setBackground(ThemeManager.getInputBackgroundColor().darker());
        styleTextField(orderIdField);

        receiverNameField = new JTextField(order.getReceiverName());
        styleTextField(receiverNameField);

        receiverKontoNumberField = new JTextField(order.getReceiverKontoNumber());
        styleTextField(receiverKontoNumberField);

        orderDateField = new JTextField(order.getOrderDate());
        styleTextField(orderDateField);

        senderNameField = new JTextField(order.getSenderName());
        styleTextField(senderNameField);

        senderKontoNumberField = new JTextField(order.getSenderKontoNumber());
        styleTextField(senderKontoNumberField);

        departmentField = new JTextField(order.getDepartment() != null ? order.getDepartment() : "");
        styleTextField(departmentField);

        // Add form rows
        addStyledFormRow(formPanel, gbc, "🆔 Bestell-ID:", orderIdField);
        addStyledFormRow(formPanel, gbc, "👤 Empfänger Name:", receiverNameField);
        addStyledFormRow(formPanel, gbc, "💳 Empfänger Konto:", receiverKontoNumberField);
        addStyledFormRow(formPanel, gbc, "📅 Bestelldatum:", orderDateField);
        addStyledFormRow(formPanel, gbc, "👤 Absender Name:", senderNameField);
        addStyledFormRow(formPanel, gbc, "💳 Absender Konto:", senderKontoNumberField);
        addStyledFormRow(formPanel, gbc, "🏢 Abteilung:", departmentField);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        leftCard.add(formScroll, BorderLayout.CENTER);

        leftCard.add(formScroll, BorderLayout.CENTER);

        // Right panel - Articles table
        RoundedPanel rightCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel tableTitle = new JLabel("📦 Bestellte Artikel");
        tableTitle.setFont(tableTitle.getFont().deriveFont(Font.BOLD, 17f));
        tableTitle.setForeground(ThemeManager.getTextPrimaryColor());
        rightCard.add(tableTitle, BorderLayout.NORTH);

        // Articles Table with modern styling
        String[] columnNames = {"Artikel", "Menge", "Einzelpreis"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1; // Only quantity is editable
            }
        };
        JTable articlesTable = new JTable(tableModel);
        articlesTable.setRowHeight(28);
        articlesTable.setFont(articlesTable.getFont().deriveFont(13f));
        articlesTable.setShowGrid(true);
        articlesTable.setGridColor(ThemeManager.getBorderColor());
        articlesTable.setBackground(ThemeManager.getTableRowEvenColor());
        articlesTable.setForeground(ThemeManager.getTextPrimaryColor());
        articlesTable.getTableHeader().setBackground(ThemeManager.getTableHeaderBackground());
        articlesTable.getTableHeader().setForeground(ThemeManager.getTableHeaderForeground());
        articlesTable.getTableHeader().setFont(articlesTable.getTableHeader().getFont().deriveFont(Font.BOLD, 13f));
        articlesTable.setSelectionBackground(ThemeManager.getTableHeaderBackground());
        articlesTable.setSelectionForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.getViewport().setBackground(ThemeManager.getTableRowEvenColor());
        rightCard.add(scrollPane, BorderLayout.CENTER);

        // Bottom action panel
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Info label on left
        JLabel infoLabel = new JLabel("💡 Tipp: Doppelklick zum Bearbeiten der Menge");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        infoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        actionPanel.add(infoLabel, BorderLayout.WEST);

        // Action buttons on right
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = createStyledButton("✕ Abbrechen", new Color(220, 53, 69), Color.WHITE);
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton("💾 Speichern", new Color(40, 167, 69), Color.WHITE);
        saveButton.addActionListener(e -> saveChanges());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        actionPanel.add(buttonPanel, BorderLayout.EAST);

        rightCard.add(actionPanel, BorderLayout.SOUTH);

        // Add panels to split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCard, rightCard);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        mainContent.add(splitPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Load articles
        loadArticles();
    }

    private void styleTextField(JTextField field) {
        ThemeManager tm = ThemeManager.getInstance();
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
    }

    private void addStyledFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        panel.add(label, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 8, 12, 8);
        panel.add(field, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
    }

    private void loadArticles() {
        // Load articles from order into table with caching for performance
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        List<Article> articles = new ArrayList<>();

        // Use cache to avoid repeated database queries
        for(Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            String articleNumber = entry.getKey();
            Article article;

            // Check cache first
            if (articleCache.containsKey(articleNumber)) {
                article = articleCache.get(articleNumber);
            } else {
                article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
                if (article != null) {
                    articleCache.put(articleNumber, article);
                }
            }

            if (article != null) {
                articles.add(article);
            }
        }

        // Populate table
        for (Article article : articles) {
            int quantity = orderedArticles.get(article.getArticleNumber());
            Object[] rowData = {
                article.getName() + " (" + article.getArticleNumber() + ")",
                quantity,
                String.format("%.2f CHF", article.getSellPrice())
            };
            tableModel.addRow(rowData);
        }
    }

    private void saveChanges() {
        // Update order with form values
        order.setReceiverName(receiverNameField.getText().trim());
        order.setReceiverKontoNumber(receiverKontoNumberField.getText().trim());
        order.setOrderDate(orderDateField.getText().trim());
        order.setSenderName(senderNameField.getText().trim());
        order.setSenderKontoNumber(senderKontoNumberField.getText().trim());
        order.setDepartment(departmentField.getText().trim());

        // Get updated quantities from table
        Map<String, Integer> tableData = getTableData();
        order.setOrderedArticles(new HashMap<>(tableData));

        // Save to database
        OrderManager orderManager = OrderManager.getInstance();
        boolean success = orderManager.updateOrder(order);

        if (!success) {
            JOptionPane.showMessageDialog(this,
                "<html><b>Fehler beim Aktualisieren!</b><br/>Die Bestellung konnte nicht gespeichert werden.</html>",
                "Fehler",
                JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JOptionPane.showMessageDialog(this,
            "<html><b>✓ Erfolgreich gespeichert!</b><br/>Die Bestellung wurde aktualisiert.</html>",
            "Erfolg",
            JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
        dispose();
    }

    public Map<String, Integer> getTableData() {
        Map<String, Integer> data = new HashMap<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String articleNameWithNumber = (String) tableModel.getValueAt(i, 0);

            // Extract article number from "Name (Number)" format
            String articleNumber = null;
            int openParen = articleNameWithNumber.lastIndexOf('(');
            int closeParen = articleNameWithNumber.lastIndexOf(')');
            if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                articleNumber = articleNameWithNumber.substring(openParen + 1, closeParen).trim();
            }

            // Get quantity from table
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
                continue; // Skip this row
            }

            // Use article number if extracted, otherwise try to find by name
            if (articleNumber != null && !articleNumber.isEmpty()) {
                data.put(articleNumber, quantity);
            } else {
                // Fallback: extract just the name part and look it up
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

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    // Gradient panel for header
    private static class GradientPanel extends JPanel {
        private final Color color1;
        private final Color color2;

        GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Rounded panel for card styling
    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
            this.bg = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}