package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.UserManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CompleteOrderGUI extends JFrame {

    private JList<OrderListItem> ordersList;
    private DefaultListModel<OrderListItem> listModel;
    private List<Order> currentOrders;
    private final JPanel detailsPanel;
    private final JButton completeButton;
    private final JButton refreshButton;
    private final JButton closeButton;
    private final JLabel orderCountLabel;
    private final JLabel statusIconLabel;

    public CompleteOrderGUI() {
        setTitle("Bestellung Abschließen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Header with gradient
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        GradientPanel headerPanel = new GradientPanel(
            new Color(30, 58, 95),
            new Color(52, 84, 122)
        );
        headerPanel.setPreferredSize(new Dimension(720, 80));
        headerPanel.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("✓");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 32f));
        iconLabel.setForeground(new Color(255, 255, 255, 180));

        JLabel title = new JLabel("  Bestellung Abschließen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(Color.WHITE);

        JPanel headerContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        headerContent.setOpaque(false);
        headerContent.add(iconLabel);
        headerContent.add(title);
        headerPanel.add(headerContent);

        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Main split panel
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);
        splitPane.setDividerSize(4);
        splitPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        splitPane.setBackground(new Color(245, 247, 250));

        // Left panel - Orders list with search
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);

        RoundedPanel leftCard = new RoundedPanel(Color.WHITE, 16);
        leftCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        leftCard.setLayout(new BorderLayout(8, 8));

        JLabel listTitle = new JLabel("📋 Offene Bestellungen");
        listTitle.setFont(listTitle.getFont().deriveFont(Font.BOLD, 16f));
        listTitle.setForeground(new Color(31, 45, 61));
        leftCard.add(listTitle, BorderLayout.NORTH);

        // Initialize orderCountLabel early
        orderCountLabel = new JLabel("Anzahl Bestellungen: 0");
        orderCountLabel.setFont(orderCountLabel.getFont().deriveFont(Font.PLAIN, 12f));
        orderCountLabel.setForeground(new Color(100, 110, 120));
        leftCard.add(orderCountLabel, BorderLayout.SOUTH);

        leftPanel.add(leftCard, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // Right panel - Details (MUST be created before initializeOrdersList)
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setOpaque(false);

        RoundedPanel rightCard = new RoundedPanel(Color.WHITE, 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel detailsTitle = new JLabel("📄 Bestelldetails");
        detailsTitle.setFont(detailsTitle.getFont().deriveFont(Font.BOLD, 16f));
        detailsTitle.setForeground(new Color(31, 45, 61));
        rightCard.add(detailsTitle, BorderLayout.NORTH);

        // Details panel with better styling (initialize EARLY)
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        statusIconLabel = new JLabel("ℹ️", SwingConstants.CENTER);
        statusIconLabel.setFont(statusIconLabel.getFont().deriveFont(48f));

        JLabel placeholderText = new JLabel("<html><div style='text-align:center; color:#6c757d;'>" +
            "Wählen Sie eine Bestellung aus<br/>um Details anzuzeigen</div></html>", SwingConstants.CENTER);
        placeholderText.setFont(placeholderText.getFont().deriveFont(14f));

        detailsPanel.add(Box.createVerticalGlue());
        detailsPanel.add(statusIconLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        detailsPanel.add(placeholderText);
        detailsPanel.add(Box.createVerticalGlue());

        JScrollPane detailsScroll = new JScrollPane(detailsPanel);
        detailsScroll.setBorder(null);
        detailsScroll.setBackground(Color.WHITE);
        rightCard.add(detailsScroll, BorderLayout.CENTER);

        rightPanel.add(rightCard, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        // NOW initialize orders list after detailsPanel is ready
        initializeOrdersList();
        JScrollPane scrollPane = new JScrollPane(ordersList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
        scrollPane.setBackground(Color.WHITE);
        leftCard.add(scrollPane, BorderLayout.CENTER);

        // Wrapper for split pane
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(splitPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom panel with styled buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 10));
        bottomPanel.setBackground(new Color(245, 247, 250));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 15, 20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);

        refreshButton = createStyledButton("🔄 Aktualisieren", new Color(108, 117, 125), Color.WHITE);
        completeButton = createStyledButton("✓ Abschließen", new Color(40, 167, 69), Color.WHITE);
        completeButton.setEnabled(false);
        closeButton = createStyledButton("✕ Schließen", new Color(220, 53, 69), Color.WHITE);

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
                showOrderDetails(o);
                completeButton.setEnabled(true);
            } else {
                showPlaceholder();
                completeButton.setEnabled(false);
            }
        });

        completeButton.addActionListener(e -> completeSelectedOrder());
        refreshButton.addActionListener(e -> {
            refreshList();
            JOptionPane.showMessageDialog(this, "Liste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE);
        });
        closeButton.addActionListener(e -> dispose());
    }

    private void showPlaceholder() {
        detailsPanel.removeAll();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        statusIconLabel.setText("ℹ️");
        statusIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel placeholderText = new JLabel("<html><div style='text-align:center; color:#6c757d;'>" +
            "Wählen Sie eine Bestellung aus<br/>um Details anzuzeigen</div></html>");
        placeholderText.setFont(placeholderText.getFont().deriveFont(14f));
        placeholderText.setAlignmentX(Component.CENTER_ALIGNMENT);

        detailsPanel.add(Box.createVerticalGlue());
        detailsPanel.add(statusIconLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        detailsPanel.add(placeholderText);
        detailsPanel.add(Box.createVerticalGlue());

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private void showOrderDetails(Order order) {
        detailsPanel.removeAll();
        detailsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Order info fields
        addDetailRow(detailsPanel, gbc, "Bestell-ID:", order.getOrderId(), new Color(52, 152, 219));
        addDetailRow(detailsPanel, gbc, "Empfänger:", order.getReceiverName(), new Color(46, 204, 113));
        addDetailRow(detailsPanel, gbc, "Abteilung:", order.getDepartment(), new Color(155, 89, 182));
        addDetailRow(detailsPanel, gbc, "Datum:", order.getOrderDate(), new Color(241, 196, 15));

        // Article count with icon
        int articleCount = order.getOrderedArticles().size();
        addDetailRow(detailsPanel, gbc, "Artikel:", articleCount + " Position(en)", new Color(230, 126, 34));

        // Separator
        gbc.gridy++;
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 225, 230));
        detailsPanel.add(sep, gbc);

        // Article details
        gbc.gridy++;
        JLabel articlesTitle = new JLabel("📦 Bestellte Artikel:");
        articlesTitle.setFont(articlesTitle.getFont().deriveFont(Font.BOLD, 14f));
        articlesTitle.setForeground(new Color(52, 73, 94));
        detailsPanel.add(articlesTitle, gbc);

        List<Article> articles = order.getOrderedArticles().keySet().stream()
                .map(s -> ArticleManager.getInstance().getArticleByNumber(s))
                .filter(Objects::nonNull)
                .toList();

        for (Article article : articles) {
            gbc.gridy++;
            int qty = order.getOrderedArticles().get(article.getArticleNumber());
            String articleInfo = String.format("  • %s (%s) - Menge: %d",
                article.getName(), article.getArticleNumber(), qty);
            JLabel articleLabel = new JLabel(articleInfo);
            articleLabel.setFont(articleLabel.getFont().deriveFont(12f));
            articleLabel.setForeground(new Color(73, 80, 87));
            detailsPanel.add(articleLabel, gbc);
        }

        // Add glue to push content to top
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        detailsPanel.add(Box.createVerticalGlue(), gbc);

        detailsPanel.revalidate();
        detailsPanel.repaint();
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gbc, String label, String value, Color accentColor) {
        JPanel rowPanel = new JPanel(new BorderLayout(10, 0));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(labelComp.getFont().deriveFont(Font.BOLD, 13f));
        labelComp.setForeground(new Color(52, 73, 94));

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(valueComp.getFont().deriveFont(Font.PLAIN, 13f));
        valueComp.setForeground(accentColor);

        rowPanel.add(labelComp, BorderLayout.WEST);
        rowPanel.add(valueComp, BorderLayout.CENTER);

        panel.add(rowPanel, gbc);
        gbc.gridy++;
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
        User user = userManager.getUserByName(selected.getSenderName().toLowerCase());
        if (!user.getOrders().contains(selected.getOrderId()))
            user.getOrders().add(selected.getOrderId());
        userManager.updateUser(user);

        selected.setStatus("Abgeschlossen");
        boolean updated = OrderManager.getInstance().updateOrder(selected);
        if (updated)
            JOptionPane.showMessageDialog(null, "Die Bestellung wurde erfolgreich abgeschlossen.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        else
            JOptionPane.showMessageDialog(null, "Fehler beim Abschließen der Bestellung.", "Fehler", JOptionPane.ERROR_MESSAGE);
        refreshList();
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
        ordersList.setFont(ordersList.getFont().deriveFont(13f));
        ordersList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ordersList.setFixedCellHeight(60);
        ordersList.setCellRenderer(new OrderListCellRenderer());
        ordersList.setSelectionBackground(new Color(52, 152, 219, 30));
        ordersList.setSelectionForeground(Color.BLACK);
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
                listModel.addElement(new OrderListItem(order));
            }
        }
        showPlaceholder();
        if (completeButton != null) {
            completeButton.setEnabled(false);
        }
        updateOrderCount();
    }

    private void updateOrderCount() {
        int count = currentOrders != null ? currentOrders.size() : 0;
        orderCountLabel.setText("Anzahl Bestellungen: " + count);
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
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
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

    // Order list item wrapper class
    private static class OrderListItem {
        final Order order;

        OrderListItem(Order order) {
            this.order = order;
        }

        @Override
        public String toString() {
            return order.getOrderId() + " - " + order.getReceiverName();
        }
    }

    // Custom cell renderer for prettier list items
    private static class OrderListCellRenderer extends JPanel implements ListCellRenderer<OrderListItem> {
        private final JLabel idLabel;
        private final JLabel nameLabel;
        private final JLabel deptLabel;
        private final JLabel dateLabel;
        private final Border padding;

        OrderListCellRenderer() {
            setLayout(new BorderLayout(10, 5));
            padding = BorderFactory.createEmptyBorder(8, 12, 8, 12);

            idLabel = new JLabel();
            idLabel.setFont(idLabel.getFont().deriveFont(Font.BOLD, 13f));
            idLabel.setForeground(new Color(52, 152, 219));

            JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
            infoPanel.setOpaque(false);

            nameLabel = new JLabel();
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.PLAIN, 12f));
            nameLabel.setForeground(new Color(52, 73, 94));

            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            bottomRow.setOpaque(false);

            deptLabel = new JLabel();
            deptLabel.setFont(deptLabel.getFont().deriveFont(Font.PLAIN, 11f));
            deptLabel.setForeground(new Color(108, 117, 125));

            dateLabel = new JLabel();
            dateLabel.setFont(dateLabel.getFont().deriveFont(Font.PLAIN, 11f));
            dateLabel.setForeground(new Color(108, 117, 125));

            bottomRow.add(deptLabel);
            bottomRow.add(new JLabel("|"));
            bottomRow.add(dateLabel);

            infoPanel.add(nameLabel);
            infoPanel.add(bottomRow);

            add(idLabel, BorderLayout.NORTH);
            add(infoPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends OrderListItem> list,
                OrderListItem value, int index, boolean isSelected, boolean cellHasFocus) {

            Order order = value.order;
            idLabel.setText("📋 " + order.getOrderId());
            nameLabel.setText("Empfänger: " + order.getReceiverName());
            deptLabel.setText("🏢 " + order.getDepartment());
            dateLabel.setText("📅 " + order.getOrderDate());

            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                padding
            ));

            if (isSelected) {
                setBackground(new Color(52, 152, 219, 30));
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 3, 1, 0, new Color(52, 152, 219)),
                    padding
                ));
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }
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