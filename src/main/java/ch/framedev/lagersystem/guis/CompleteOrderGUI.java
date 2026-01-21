package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.UserManager;
import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

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
        ThemeManager.getInstance().registerWindow(this);

        setTitle("Bestellung Abschließen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        Color bg = ThemeManager.getBackgroundColor();
        Color card = ThemeManager.getCardBackgroundColor();
        Color text = ThemeManager.getTextPrimaryColor();
        Color muted = ThemeManager.getTextSecondaryColor();
        Color border = ThemeManager.getBorderColor();

        // Header with gradient
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(bg);

        GradientPanel headerPanel = new GradientPanel(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.getButtonHoverColor(ThemeManager.getHeaderBackgroundColor())
        );
        headerPanel.setPreferredSize(new Dimension(720, 80));
        headerPanel.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("✓");
        iconLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 32));
        iconLabel.setForeground(new Color(255, 255, 255, 180));

        JLabel title = new JLabel(UnicodeSymbols.CHECKMARK + " Bestellung Abschließen");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 26));
        title.setForeground(ThemeManager.getHeaderForegroundColor());

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
        splitPane.setBackground(bg);

        // Left panel - Orders list
        JPanel leftPanel = new JPanel(new BorderLayout(8, 8));
        leftPanel.setOpaque(false);

        RoundedPanel leftCard = new RoundedPanel(card, 16);
        leftCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        leftCard.setLayout(new BorderLayout(8, 8));

        JLabel listTitle = new JLabel(UnicodeSymbols.CLIPBOARD + " Offene Bestellungen");
        listTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        listTitle.setForeground(text);
        leftCard.add(listTitle, BorderLayout.NORTH);

        orderCountLabel = new JLabel("Anzahl Bestellungen: 0");
        orderCountLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        orderCountLabel.setForeground(muted);
        leftCard.add(orderCountLabel, BorderLayout.SOUTH);

        leftPanel.add(leftCard, BorderLayout.CENTER);
        splitPane.setLeftComponent(leftPanel);

        // Right panel - Details
        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        rightPanel.setOpaque(false);

        RoundedPanel rightCard = new RoundedPanel(card, 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel detailsTitle = new JLabel(UnicodeSymbols.EMPTY_PAGE + " Bestelldetails");
        detailsTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        detailsTitle.setForeground(text);
        rightCard.add(detailsTitle, BorderLayout.NORTH);

        // Details panel (initialize early)
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        statusIconLabel = new JLabel(UnicodeSymbols.INFO, SwingConstants.CENTER);
        statusIconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 48));

        JLabel placeholderText = new JLabel("<html><div style='text-align:center;'>" +
                "Wählen Sie eine Bestellung aus<br/>um Details anzuzeigen</div></html>", SwingConstants.CENTER);
        placeholderText.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        placeholderText.setForeground(muted);

        detailsPanel.add(Box.createVerticalGlue());
        detailsPanel.add(statusIconLabel);
        detailsPanel.add(Box.createVerticalStrut(10));
        detailsPanel.add(placeholderText);
        detailsPanel.add(Box.createVerticalGlue());

        JScrollPane detailsScroll = new JScrollPane(detailsPanel);
        detailsScroll.setBorder(null);
        detailsScroll.setBackground(card);
        detailsScroll.getViewport().setBackground(card);
        rightCard.add(detailsScroll, BorderLayout.CENTER);

        rightPanel.add(rightCard, BorderLayout.CENTER);
        splitPane.setRightComponent(rightPanel);

        // Orders list
        initializeOrdersList();
        JScrollPane scrollPane = new JScrollPane(ordersList);
        scrollPane.setBorder(BorderFactory.createLineBorder(border, 1));
        scrollPane.setBackground(card);
        scrollPane.getViewport().setBackground(card);
        leftCard.add(scrollPane, BorderLayout.CENTER);

        // Wrapper for split pane
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(bg);
        centerWrapper.add(splitPane, BorderLayout.CENTER);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom panel with styled buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 10));
        bottomPanel.setBackground(bg);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 20, 15, 20));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttons.setOpaque(false);

        refreshButton = createThemeButton(UnicodeSymbols.UPDATE + " Aktualisieren", ThemeManager.getSecondaryColor());
        refreshButton.setToolTipText("Aktualisiert die Liste der offenen Bestellungen");
        completeButton = createThemeButton(UnicodeSymbols.CHECKMARK + " Abschließen", ThemeManager.getSuccessColor());
        completeButton.setToolTipText("Schließt die ausgewählte Bestellung ab");
        completeButton.setEnabled(false);
        closeButton = createThemeButton(UnicodeSymbols.CLOSE + " Schließen", ThemeManager.getDangerColor());
        closeButton.setToolTipText("Schließt dieses Fenster");

        buttons.add(refreshButton);
        buttons.add(completeButton);
        buttons.add(closeButton);
        bottomPanel.add(buttons, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        attachListeners();
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
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
            JOptionPane.showMessageDialog(this, "Liste wurde aktualisiert.", "Aktualisiert",
                    JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
        });
        closeButton.addActionListener(e -> dispose());
    }

    private void showPlaceholder() {
        detailsPanel.removeAll();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));

        statusIconLabel.setText(UnicodeSymbols.INFO);
        statusIconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel placeholderText = new JLabel("<html><div style='text-align:center;'>" +
                "Wählen Sie eine Bestellung aus<br/>um Details anzuzeigen</div></html>");
        placeholderText.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        placeholderText.setForeground(ThemeManager.getTextSecondaryColor());
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

        addDetailRow(detailsPanel, gbc, "Bestell-ID:", safe(order.getOrderId()), ThemeManager.getAccentColor());
        addDetailRow(detailsPanel, gbc, "Empfänger:", safe(order.getReceiverName()), ThemeManager.getSuccessColor());
        addDetailRow(detailsPanel, gbc, "Abteilung:", safe(order.getDepartment()), new Color(155, 89, 182));
        addDetailRow(detailsPanel, gbc, "Datum:", safe(order.getOrderDate()), ThemeManager.getWarningColor());

        int articleCount = order.getOrderedArticles().size();
        addDetailRow(detailsPanel, gbc, "Artikel:", articleCount + " Position(en)", new Color(230, 126, 34));

        gbc.gridy++;
        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeManager.getDividerColor());
        detailsPanel.add(sep, gbc);

        gbc.gridy++;
        JLabel articlesTitle = new JLabel(UnicodeSymbols.PACKAGE + " Bestellte Artikel:");
        articlesTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        articlesTitle.setForeground(ThemeManager.getTextPrimaryColor());
        detailsPanel.add(articlesTitle, gbc);

        List<Article> articles = order.getOrderedArticles().keySet().stream()
                .map(s -> ArticleManager.getInstance().getArticleByNumber(s))
                .filter(Objects::nonNull)
                .toList();

        for (Article article : articles) {
            gbc.gridy++;
            int qty = order.getOrderedArticles().get(article.getArticleNumber());
            String articleInfo = String.format("  • %s (%s) - Menge: %d",
                    safe(article.getName()), safe(article.getArticleNumber()), qty);
            JLabel articleLabel = new JLabel(articleInfo);
            articleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            articleLabel.setForeground(ThemeManager.getTextSecondaryColor());
            detailsPanel.add(articleLabel, gbc);
        }

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
        labelComp.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        labelComp.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        valueComp.setForeground(accentColor);

        rowPanel.add(labelComp, BorderLayout.WEST);
        rowPanel.add(valueComp, BorderLayout.CENTER);

        panel.add(rowPanel, gbc);
        gbc.gridy++;
    }

    private void completeSelectedOrder() {
        int idx = ordersList.getSelectedIndex();
        if (idx < 0 || currentOrders == null || idx >= currentOrders.size()) return;

        Order selected = currentOrders.get(idx);

        // Final confirmation FIRST
        int res = JOptionPane.showConfirmDialog(this,
                "Möchten Sie die Bestellung " + safe(selected.getOrderId()) + " abschließen?\n" +
                        "Der Lagerbestand wird entsprechend reduziert.",
                "Bestätigung",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, Main.iconSmall);

        if (res != JOptionPane.YES_OPTION) {
            return;
        }

        // Resolve articles
        List<Article> articles = selected.getOrderedArticles().keySet().stream()
                .map(s -> ArticleManager.getInstance().getArticleByNumber(s))
                .filter(Objects::nonNull)
                .toList();

        // Validate stock availability first
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (Article article : articles) {
            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
            int currentStock = article.getStockQuantity();
            int newStock = currentStock - qty;

            if (currentStock < qty) {
                errors.add(String.format("• %s (%s): Lager=%d, Bestellt=%d (Fehlt: %d)",
                        safe(article.getName()), safe(article.getArticleNumber()),
                        currentStock, qty, qty - currentStock));
            } else if (newStock < article.getMinStockLevel()) {
                warnings.add(String.format("• %s (%s): Neuer Bestand=%d < Mindestbestand=%d",
                        safe(article.getName()), safe(article.getArticleNumber()),
                        newStock, article.getMinStockLevel()));
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>FEHLER: Nicht genügend Lagerbestand!</b><br/><br/>");
            errors.forEach(err -> msg.append(err).append("<br/>"));
            msg.append("<br/>Die Bestellung kann nicht abgeschlossen werden.</html>");
            JOptionPane.showMessageDialog(this, msg.toString(), "Lagerbestand unzureichend",
                    JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            return;
        }

        if (!warnings.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>WARNUNG: Mindestbestand wird unterschritten!</b><br/><br/>");
            warnings.forEach(warn -> msg.append(warn).append("<br/>"));
            msg.append("<br/>Möchten Sie trotzdem fortfahren?</html>");
            int warnRes = JOptionPane.showConfirmDialog(this, msg.toString(), "Warnung",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            if (warnRes != JOptionPane.YES_OPTION) return;
        }

        // Optional: partial fulfillment (if you want to keep it)
        // Here we only check again if something changed (usually not needed),
        // but you had the feature, so we keep it as a single controlled branch.
        List<String> insufficientStockErrors = new ArrayList<>();
        for (Article article : articles) {
            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
            int currentStock = article.getStockQuantity();
            if (currentStock < qty) {
                insufficientStockErrors.add(String.format(
                        "• %s (%s): Benötigt=%d, Verfügbar=%d (Fehlt: %d)",
                        safe(article.getName()),
                        safe(article.getArticleNumber()),
                        qty,
                        currentStock,
                        qty - currentStock
                ));
            }
        }

        if (!insufficientStockErrors.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("<html><b>WARNUNG: Nicht genügend Lagerbestand!</b><br/><br/>");
            errorMsg.append("Folgende Artikel haben nicht ausreichend Lagerbestand:<br/><br/>");
            insufficientStockErrors.forEach(err -> errorMsg.append(err).append("<br/>"));
            errorMsg.append("<br/><b>Optionen:</b><br/>");
            errorMsg.append("• <b>Ja</b>: Bestellung mit verfügbarem Bestand abschließen (Fehlmenge wird protokolliert)<br/>");
            errorMsg.append("• <b>Nein</b>: Vorgang abbrechen</html>");

            int choice = JOptionPane.showConfirmDialog(this,
                    errorMsg.toString(),
                    "Unzureichender Lagerbestand",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);

            if (choice != JOptionPane.YES_OPTION) {
                return;
            }

            completeOrderWithPartialStock(selected, articles, idx);
            return;
        }

        // Deduct stock & update order
        ArticleManager articleManager = ArticleManager.getInstance();

        boolean allStockUpdated = true;
        for (Article article : articles) {
            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
            if (!articleManager.removeFromStock(article.getArticleNumber(), qty)) {
                allStockUpdated = false;
                System.err.println("[CompleteOrderGUI] Fehler beim Reduzieren des Lagerbestands für Artikel: " + article.getArticleNumber());
            }
        }

        // Update user
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUserByName(selected.getSenderName().toLowerCase());
        if (user == null) user = createUser(selected.getSenderName().toLowerCase());
        if (!user.getOrders().contains(selected.getOrderId())) user.getOrders().add(selected.getOrderId());
        userManager.updateUser(user);
        OrderLoggingUtils.getInstance().addLogEntry(selected, user);

        // Update order
        selected.setStatus("Abgeschlossen");
        boolean updated = OrderManager.getInstance().updateOrder(selected);

        if (updated && allStockUpdated) {
            JOptionPane.showMessageDialog(this,
                    "Die Bestellung wurde erfolgreich abgeschlossen.",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } else if (!allStockUpdated) {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Warnung: Teilweise Fehler!</b><br/><br/>" +
                            "Einige Lagerbestände konnten nicht aktualisiert werden.<br/>" +
                            "Bitte überprüfen Sie die Logs und aktualisieren Sie manuell.</html>",
                    "Teilweise erfolgreich",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Abschließen der Bestellung.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }

        refreshList();
    }

    /**
     * Completes an order with partial stock fulfillment.
     * Deducts available stock and logs items that need to be ordered.
     */
    private void completeOrderWithPartialStock(Order selected, List<Article> articles, int idx) {
        ArticleManager articleManager = ArticleManager.getInstance();

        List<String> fulfilledItems = new ArrayList<>();
        List<String> partialItems = new ArrayList<>();
        List<String> unfulfillableItems = new ArrayList<>();

        StringBuilder orderNotes = new StringBuilder();
        orderNotes.append("Teilweise abgeschlossen - Fehlmengen:\n");

        boolean hasPartialFulfillment = false;

        for (Article article : articles) {
            int qtyOrdered = selected.getOrderedArticles().get(article.getArticleNumber());
            int currentStock = article.getStockQuantity();

            if (currentStock >= qtyOrdered) {
                if (articleManager.removeFromStock(article.getArticleNumber(), qtyOrdered)) {
                    fulfilledItems.add(String.format("%s: %d/%d", safe(article.getName()), qtyOrdered, qtyOrdered));
                } else {
                    System.err.println("[CompleteOrderGUI] Fehler beim Reduzieren des Lagerbestands für: " + article.getArticleNumber());
                }
            } else if (currentStock > 0) {
                int shortage = qtyOrdered - currentStock;
                if (articleManager.removeFromStock(article.getArticleNumber(), currentStock)) {
                    partialItems.add(String.format("%s: %d/%d (Fehlmenge: %d)",
                            safe(article.getName()), currentStock, qtyOrdered, shortage));
                    orderNotes.append(String.format("- %s (%s): %d Stück fehlen\n",
                            safe(article.getName()), safe(article.getArticleNumber()), shortage));
                    hasPartialFulfillment = true;
                }
            } else {
                unfulfillableItems.add(String.format("%s: 0/%d (Komplett fehlend)",
                        safe(article.getName()), qtyOrdered));
                orderNotes.append(String.format("- %s (%s): %d Stück fehlen (kein Bestand)\n",
                        safe(article.getName()), safe(article.getArticleNumber()), qtyOrdered));
                hasPartialFulfillment = true;
            }
        }

        Order order = currentOrders.get(idx);
        order.setStatus(hasPartialFulfillment ? "Teilweise abgeschlossen" : "Abgeschlossen");

        boolean updatedOrder = OrderManager.getInstance().updateOrder(order);

        StringBuilder resultMsg = new StringBuilder("<html><b>Bestellung verarbeitet!</b><br/><br/>");
        resultMsg.append("<b>Bestell-ID:</b> ").append(safe(selected.getOrderId())).append("<br/><br/>");

        if (!fulfilledItems.isEmpty()) {
            resultMsg.append("<b style='color:green;'>✓ Vollständig erfüllt:</b><br/>");
            fulfilledItems.forEach(item -> resultMsg.append("  ").append(item).append("<br/>"));
            resultMsg.append("<br/>");
        }

        if (!partialItems.isEmpty()) {
            resultMsg.append("<b style='color:orange;'>⚠ Teilweise erfüllt:</b><br/>");
            partialItems.forEach(item -> resultMsg.append("  ").append(item).append("<br/>"));
            resultMsg.append("<br/>");
        }

        if (!unfulfillableItems.isEmpty()) {
            resultMsg.append("<b style='color:red;'>✕ Nicht erfüllt:</b><br/>");
            unfulfillableItems.forEach(item -> resultMsg.append("  ").append(item).append("<br/>"));
            resultMsg.append("<br/>");
        }

        if (hasPartialFulfillment) {
            resultMsg.append("<br/><b style='color:red;'>WICHTIG:</b> Fehlende Artikel müssen nachbestellt werden!<br/>");
            resultMsg.append("<b>Status:</b> Teilweise abgeschlossen<br/>");
        } else {
            resultMsg.append("<b>Status:</b> Abgeschlossen<br/>");
        }

        resultMsg.append("</html>");

        if (updatedOrder) {
            JOptionPane.showMessageDialog(this,
                    resultMsg.toString(),
                    "Bestellung verarbeitet",
                    hasPartialFulfillment ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            if (hasPartialFulfillment) {
                System.out.println("[CompleteOrderGUI] Bestellung " + safe(selected.getOrderId()) + " teilweise abgeschlossen:");
                System.out.println(orderNotes);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "<html><b>Warnung:</b><br/><br/>" +
                            "Lagerbestände wurden teilweise aktualisiert, aber der Bestellstatus konnte nicht aktualisiert werden.<br/>" +
                            "Bitte überprüfen Sie die Bestellung manuell.</html>",
                    "Teilweise erfolgreich",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
        }

        refreshList();
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private User createUser(String userName) {
        User user = new User(userName, new ArrayList<>());
        UserManager.getInstance().insertUser(user);
        return user;
    }

    private void initializeOrdersList() {
        listModel = new DefaultListModel<>();
        ordersList = new JList<>(listModel);

        ordersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersList.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        ordersList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        ordersList.setFixedCellHeight(60);

        ordersList.setCellRenderer(new OrderListCellRenderer());

        ordersList.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        ordersList.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        ordersList.setBackground(ThemeManager.getCardBackgroundColor());
        ordersList.setForeground(ThemeManager.getTextPrimaryColor());

        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        try {
            List<Order> allOrders = OrderManager.getInstance().getOrders();
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
        if (completeButton != null) completeButton.setEnabled(false);
        updateOrderCount();
    }

    private void updateOrderCount() {
        int count = currentOrders != null ? currentOrders.size() : 0;
        orderCountLabel.setText("Anzahl Bestellungen: " + count);
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
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(hoverBg);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(baseBg);
            }
            public void mousePressed(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) button.setBackground(pressedBg);
            }
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

    private record OrderListItem(Order order) {

        @Override
            public String toString() {
                return order.getOrderId() + " - " + order.getReceiverName();
            }
        }

    private static class OrderListCellRenderer extends JPanel implements ListCellRenderer<OrderListItem> {
        private final JLabel idLabel;
        private final JLabel nameLabel;
        private final JLabel deptLabel;
        private final JLabel dateLabel;
        private final Border padding;

        OrderListCellRenderer() {
            setLayout(new BorderLayout(10, 5));
            setOpaque(true);

            padding = BorderFactory.createEmptyBorder(8, 12, 8, 12);

            idLabel = new JLabel();
            idLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

            JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 2));
            infoPanel.setOpaque(false);

            nameLabel = new JLabel();
            nameLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));

            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            bottomRow.setOpaque(false);

            deptLabel = new JLabel();
            deptLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));

            dateLabel = new JLabel();
            dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));

            JLabel sep = new JLabel("|");
            sep.setForeground(ThemeManager.getTextSecondaryColor());

            bottomRow.add(deptLabel);
            bottomRow.add(sep);
            bottomRow.add(dateLabel);

            infoPanel.add(nameLabel);
            infoPanel.add(bottomRow);

            add(idLabel, BorderLayout.NORTH);
            add(infoPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends OrderListItem> list,
                                                      OrderListItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            Order order = value.order;

            idLabel.setText(UnicodeSymbols.INFO + " " + safe(order.getOrderId()));
            nameLabel.setText("Empfänger: " + safe(order.getReceiverName()));
            deptLabel.setText(UnicodeSymbols.DEPARTMENT + " " + safe(order.getDepartment()));
            dateLabel.setText(UnicodeSymbols.CALENDAR + " " + safe(order.getOrderDate()));

            Color bg = ThemeManager.getCardBackgroundColor();
            Color fg = ThemeManager.getTextPrimaryColor();
            Color muted = ThemeManager.getTextSecondaryColor();
            Color divider = ThemeManager.getDividerColor();
            Color accent = ThemeManager.getAccentColor();

            idLabel.setForeground(accent);
            nameLabel.setForeground(fg);
            deptLabel.setForeground(muted);
            dateLabel.setForeground(muted);

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, divider),
                    padding
            ));

            if (isSelected) {
                setBackground(ThemeManager.getSelectionBackgroundColor());
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 3, 1, 0, accent),
                        padding
                ));
            } else {
                setBackground(bg);
            }

            return this;
        }
    }

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