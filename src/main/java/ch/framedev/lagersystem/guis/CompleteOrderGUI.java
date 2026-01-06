package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.UserManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompleteOrderGUI extends JFrame {

    private JList<String> ordersList;
    private DefaultListModel<String> listModel;
    private List<Order> currentOrders;
    private final JLabel detailsLabel;
    private final JButton completeButton;
    private final JButton refreshButton;
    private final JButton closeButton;
    private final JLabel orderCountLabel;

    public CompleteOrderGUI() {
        setTitle("Bestellung Abschließen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(750, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Header
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        RoundedPanel headerPanel = new RoundedPanel(new Color(255, 255, 255), 20);
        headerPanel.setPreferredSize(new Dimension(600, 64));
        JLabel title = new JLabel("Bestellung Abschließen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(31, 45, 61));
        headerPanel.add(title);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Main card
        RoundedPanel card = new RoundedPanel(Color.WHITE, 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Initialize detailsLabel early
        detailsLabel = new JLabel("<html><div style='text-align:center; padding:10px;'>" +
                "Wähle eine Bestellung aus, um Details zu sehen.</div></html>", SwingConstants.CENTER);
        detailsLabel.setFont(detailsLabel.getFont().deriveFont(14f));
        detailsLabel.setForeground(new Color(80, 90, 100));

        orderCountLabel = new JLabel("Anzahl Bestellungen: 0");

        // Initialize orders list
        initializeOrdersList();
        JScrollPane scrollPane = new JScrollPane(ordersList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230)));
        card.add(scrollPane, BorderLayout.CENTER);

        // Details panel
        JPanel detailsPanel = new JPanel(new BorderLayout(8, 8));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        detailsPanel.add(detailsLabel, BorderLayout.CENTER);
        card.add(detailsPanel, BorderLayout.SOUTH);

        // Place card in center
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom panel with count and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(new Color(245, 247, 250));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

        orderCountLabel.setFont(orderCountLabel.getFont().deriveFont(Font.BOLD));
        orderCountLabel.setForeground(new Color(31, 45, 61));
        bottomPanel.add(orderCountLabel, BorderLayout.WEST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttons.setOpaque(false);

        completeButton = createRoundedButton("Bestellung abschließen");
        completeButton.setEnabled(false);
        refreshButton = createRoundedButton("Aktualisieren");
        closeButton = createRoundedButton("Schließen");

        buttons.add(refreshButton);
        buttons.add(completeButton);
        buttons.add(closeButton);
        bottomPanel.add(buttons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        attachListeners();
    }

    private void attachListeners() {
        ordersList.addListSelectionListener(e -> {
            int idx = ordersList.getSelectedIndex();
            if (idx >= 0 && currentOrders != null && idx < currentOrders.size()) {
                Order o = currentOrders.get(idx);
                int articleCount = o.getOrderedArticles().size();
                detailsLabel.setText(String.format(
                        "<html><div style='padding:5px;'>" +
                                "<b>Bestell-ID:</b> %s<br/>" +
                                "<b>Empfänger:</b> %s<br/>" +
                                "<b>Abteilung:</b> %s<br/>" +
                                "<b>Datum:</b> %s<br/>" +
                                "<b>Artikel:</b> %d<br/>" +
                                "</div></html>",
                        o.getOrderId(), o.getReceiverName(), o.getDepartment(),
                        o.getOrderDate(), articleCount));
                completeButton.setEnabled(true);
            } else {
                detailsLabel.setText("<html><div style='text-align:center; padding:10px;'>" +
                        "Wähle eine Bestellung aus, um Details zu sehen.</div></html>");
                completeButton.setEnabled(false);
            }
        });

        completeButton.addActionListener(e -> completeSelectedOrder());
        refreshButton.addActionListener(e -> refreshList());
        closeButton.addActionListener(e -> dispose());
    }

    private void completeSelectedOrder() {
        int idx = ordersList.getSelectedIndex();
        if (idx < 0 || currentOrders == null || idx >= currentOrders.size()) {
            return;
        }

        Order selected = currentOrders.get(idx);

        // Validate stock availability first
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        List<Article> articles = selected.getOrderedArticles().keySet().stream()
                .map(s -> ArticleManager.getInstance().getArticleByNumber(s))
                .filter(Objects::nonNull)
                .toList();

        for (Article article : articles) {
            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
            int currentStock = article.getStockQuantity();
            int newStock = currentStock - qty;

            // Critical: Not enough stock
            if (currentStock < qty) {
                errors.add(String.format("• %s (%s): Lager=%d, Bestellt=%d (Fehlt: %d)",
                        article.getName(), article.getArticleNumber(),
                        currentStock, qty, qty - currentStock));
            }
            // Warning: Below minimum stock
            else if (newStock < article.getMinStockLevel()) {
                warnings.add(String.format("• %s (%s): Neuer Bestand=%d < Mindestbestand=%d",
                        article.getName(), article.getArticleNumber(),
                        newStock, article.getMinStockLevel()));
            }
        }

        // Show errors if any
        if (!errors.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>FEHLER: Nicht genügend Lagerbestand!</b><br/><br/>");
            errors.forEach(err -> msg.append(err).append("<br/>"));
            msg.append("<br/>Die Bestellung kann nicht abgeschlossen werden.</html>");
            JOptionPane.showMessageDialog(this, msg.toString(), "Lagerbestand unzureichend", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Show warnings if any
        if (!warnings.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>WARNUNG: Mindestbestand wird unterschritten!</b><br/><br/>");
            warnings.forEach(warn -> msg.append(warn).append("<br/>"));
            msg.append("<br/>Möchten Sie trotzdem fortfahren?</html>");
            int res = JOptionPane.showConfirmDialog(this, msg.toString(), "Warnung", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (res != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Final confirmation
        //noinspection unused
        int res = JOptionPane.showConfirmDialog(this,
                "Möchten Sie die Bestellung " + selected.getOrderId() + " abschließen?\n" +
                        "Der Lagerbestand wird entsprechend reduziert.",
                "Bestätigung",
                JOptionPane.YES_NO_OPTION);
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUserByName(selected.getSenderName());
        if (!user.getOrders().contains(selected.getOrderId()))
            user.getOrders().add(selected.getOrderId());
        userManager.updateUser(user);
/*
        if (res == JOptionPane.YES_OPTION) {
            // Update stock quantities
            for (Article article : articles) {
                int qty = selected.getOrderedArticles().get(article.getArticleNumber());
                article.setStockQuantity(article.getStockQuantity() - qty);
                ArticleManager.getInstance().updateArticle(article);
            }

            // Update order status
            Order order = currentOrders.get(idx);
            order.setStatus("Abgeschlossen");
            boolean updated = OrderManager.getInstance().updateOrder(order);

            if (updated) {
                JOptionPane.showMessageDialog(this,
                        String.format("<html><b>Bestellung erfolgreich abgeschlossen!</b><br/><br/>" +
                                "Bestell-ID: %s<br/>" +
                                "Lagerbestände wurden aktualisiert.<br/>" +
                                "Status: Abgeschlossen</html>",
                                selected.getOrderId()),
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Warnung: Lagerbestände wurden aktualisiert, aber der Bestellstatus konnte nicht aktualisiert werden.",
                        "Teilweise erfolgreich",
                        JOptionPane.WARNING_MESSAGE);
            }

            refreshList();
        }*/
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private void initializeOrdersList() {
        listModel = new DefaultListModel<>();
        ordersList = new JList<>(listModel);
        ordersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersList.setFont(ordersList.getFont().deriveFont(14f));
        ordersList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        try {
            List<Order> allOrders = OrderManager.getInstance().getOrders();
            // Filter to show only pending orders (not completed)
            currentOrders = allOrders.stream()
                    .filter(order -> !"Abgeschlossen".equals(order.getStatus()))
                    .toList();
        } catch (Exception ex) {
            currentOrders = null;
        }
        if (currentOrders != null) {
            for (Order order : currentOrders) {
                listModel.addElement(order.getOrderId() + " - " + order.getReceiverName() + " (" + order.getDepartment() + ")");
            }
        }
        if (detailsLabel != null) {
            detailsLabel.setText("<html><div style='text-align:center; padding:10px;'>" +
                    "Wähle eine Bestellung aus, um Details zu sehen.</div></html>");
        }
        if (completeButton != null) {
            completeButton.setEnabled(false);
        }
        updateOrderCount();
    }

    private void updateOrderCount() {
        int count = currentOrders != null ? currentOrders.size() : 0;
        orderCountLabel.setText("Anzahl Bestellungen: " + count);
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(20, 30, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
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