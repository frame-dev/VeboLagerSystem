package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class EditOrderGUI extends JFrame {

    private final Order order;
    private JTextField orderIdField;
    private JTextField receiverNameField;
    private JTextField receiverKontoNumberField;
    private JTextField orderDateField;
    private JTextField senderNameField;
    private JTextField senderKontoNumberField;

    private JTable articlesTable;
    private DefaultTableModel tableModel;

    public EditOrderGUI(Order order) {
        this.order = order;
        setTitle("Edit Order");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Order ID
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(new JLabel("Order ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        orderIdField = new JTextField(order.getOrderId());
        orderIdField.setEditable(false);
        mainPanel.add(orderIdField, gbc);

        // Receiver Name
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Empfänger Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        receiverNameField = new JTextField(order.getReceiverName());
        mainPanel.add(receiverNameField, gbc);

        // Receiver Konto Number
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Empfänger Konto Nummer:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        receiverKontoNumberField = new JTextField(order.getReceiverKontoNumber());
        mainPanel.add(receiverKontoNumberField, gbc);

        // Order Date
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Bestelldatum:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        orderDateField = new JTextField(order.getOrderDate());
        mainPanel.add(orderDateField, gbc);

        // Sender Name
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Absender Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        senderNameField = new JTextField(order.getSenderName());
        mainPanel.add(senderNameField, gbc);

        // Sender Konto Number
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("Absender Konto Nummer:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        senderKontoNumberField = new JTextField(order.getSenderKontoNumber());
        mainPanel.add(senderKontoNumberField, gbc);

        // Articles Table
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        String[] columnNames = {"Artikel", "Menge", "Preis"};
        tableModel = new DefaultTableModel(columnNames, 0);
        articlesTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(articlesTable);
        scrollPane.setPreferredSize(new Dimension(700, 200));
        mainPanel.add(scrollPane, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("Speichern");
        JButton cancelButton = new JButton("Abbrechen");

        saveButton.addActionListener(e -> saveChanges());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.EAST;
        mainPanel.add(buttonPanel, gbc);

        add(mainPanel);

        loadArticles();
    }

    private void loadArticles() {
        // Load articles from order into table
        // This depends on your Order class structure
        // Example implementation:
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        List<Article> articles = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            articles.add(ArticleManager.getInstance().getArticleByNumber(entry.getKey()));
        }
        for (Article article : articles) {
            int quantity = orderedArticles.get(article.getArticleNumber());
            Object[] rowData = {article.getName(), quantity, article.getSellPrice()};
            tableModel.addRow(rowData);
        }
    }

    private void saveChanges() {
        order.setReceiverName(receiverNameField.getText());
        order.setReceiverKontoNumber(receiverKontoNumberField.getText());
        order.setOrderDate(orderDateField.getText());
        order.setSenderName(senderNameField.getText());
        order.setSenderKontoNumber(senderKontoNumberField.getText());

        Map<String, Integer> tableData = getTableData();
        Map<String, Integer> orderedArticles = new java.util.HashMap<>();
        for (Map.Entry<String, Integer> entry : tableData.entrySet()) {
            orderedArticles.put(entry.getKey(), (Integer) entry.getValue());
        }
        order.setOrderedArticles(orderedArticles);

        OrderManager orderManager = OrderManager.getInstance();
        boolean success = orderManager.updateOrder(order);
        if (!success) {
            JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren der Bestellung!", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Bestellung erfolgreich aktualisiert!");
        dispose();
    }

    public Map<String, Integer> getTableData() {
        Map<String, Integer> data = new java.util.HashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String articleName = (String) tableModel.getValueAt(i, 0);
            int quantity = 0;
            try {
                quantity = Integer.parseInt((String) tableModel.getValueAt(i, 1));
            } catch (NumberFormatException | ClassCastException e) {
                quantity = (int) tableModel.getValueAt(i, 1);
            }
            // Integer quantity = Integer.parseInt((String) tableModel.getValueAt(i, 1));

            // Get article number from article name
            Article article = ArticleManager.getInstance().getArticleByName(articleName);
            if (article != null) {
                data.put(article.getArticleNumber(), quantity);
            }
        }
        return data;
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}