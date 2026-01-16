package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
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

    private JTable articlesTable;

    public EditOrderGUI(Order order) {
        this.order = order;

        ThemeManager.getInstance().registerWindow(this);

        setTitle("Bestellung Bearbeiten");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Gradient header with icon
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());

        GradientPanel header = new GradientPanel(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.getButtonHoverColor(ThemeManager.getHeaderBackgroundColor())
        );
        header.setPreferredSize(new Dimension(850, 80));
        header.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("✏️");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 36f));
        iconLabel.setForeground(ThemeManager.getTextOnPrimaryColor());

        JLabel title = new JLabel("  Bestellung Bearbeiten");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(ThemeManager.getTextOnPrimaryColor());

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
        addStyledFormRow(formPanel, gbc, "🆔 Bestell-ID:", orderIdField);
        addStyledFormRow(formPanel, gbc, "👤 Empfänger Name:", receiverNameField);
        addStyledFormRow(formPanel, gbc, "💳 Empfänger Konto:", receiverKontoNumberField);
        addStyledFormRow(formPanel, gbc, "📅 Bestelldatum:", orderDateField);
        addStyledFormRow(formPanel, gbc, "👤 Absender Name:", senderNameField);
        addStyledFormRow(formPanel, gbc, "💳 Absender Konto:", senderKontoNumberField);
        addStyledFormRow(formPanel, gbc, "🏢 Abteilung:", departmentField);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setBackground(ThemeManager.getCardBackgroundColor());
        formScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
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

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        articlesTable = new JTable(tableModel);
        applyTableTheme(articlesTable);

        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        rightCard.add(scrollPane, BorderLayout.CENTER);

        // Bottom action panel
        JPanel actionPanel = new JPanel(new BorderLayout(10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JLabel infoLabel = new JLabel("💡 Tipp: Doppelklick zum Bearbeiten der Menge");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC, 11f));
        infoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        actionPanel.add(infoLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        JButton cancelButton = createThemeButton("✕ Abbrechen", ThemeManager.getDangerColor());
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createThemeButton("💾 Speichern", ThemeManager.getSuccessColor());
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

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void applyTableTheme(JTable table) {
        table.setRowHeight(28);
        table.setFont(new Font("Arial", Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
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
        th.setFont(th.getFont().deriveFont(Font.BOLD, 13f));
        th.setReorderingAllowed(false);

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
                    c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
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
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        editorField.setBackground(ThemeManager.getInputBackgroundColor());
        editorField.setForeground(ThemeManager.getTextPrimaryColor());
        editorField.setCaretColor(ThemeManager.getTextPrimaryColor());
        table.setDefaultEditor(Integer.class, new DefaultCellEditor(editorField));
    }

    private void styleTextField(JTextField field) {
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

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private JButton createThemeButton(String text, Color baseBg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        Color hoverBg = ThemeManager.getButtonHoverColor(baseBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(baseBg);

        button.setBackground(baseBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(baseBg);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                if (!button.isEnabled()) return;
                button.setBackground(button.contains(evt.getPoint()) ? hoverBg : baseBg);
            }
        });

        return button;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
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