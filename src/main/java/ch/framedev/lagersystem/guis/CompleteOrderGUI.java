package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.UserManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.OrderLoggingUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static ch.framedev.lagersystem.utils.JFrameUtils.createThemeButton;

/**
 * GUI for completing orders. Displays a list of open orders and allows the user to view details and complete them.
 * @author framedev
 */
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

    /**
     * Initializes the CompleteOrderGUI with a modern design and sets up all components and listeners.
     */
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

        JFrameUtils.GradientPanel headerPanel = new JFrameUtils.GradientPanel(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.getButtonHoverColor(ThemeManager.getHeaderBackgroundColor())
        );
        headerPanel.setPreferredSize(new Dimension(720, 80));
        headerPanel.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("OK");
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

        JFrameUtils.RoundedPanel leftCard = new JFrameUtils.RoundedPanel(card, 16);
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

        JFrameUtils.RoundedPanel rightCard = new JFrameUtils.RoundedPanel(card, 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel detailsTitle = new JLabel(UnicodeSymbols.FILE + " Bestelldetails");
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
        if(ordersList == null) return;
        ordersList.addListSelectionListener(e -> {
            OrderListItem item = ordersList.getSelectedValue();
            if (item != null && item.order != null) {
                showOrderDetails(item.order);
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
        if(order == null) return;
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
            String articleInfo = String.format("  - %s (%s) - Menge: %d",
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
        if(panel == null || gbc == null || label == null || value == null || accentColor == null) return;
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
        OrderListItem item = ordersList.getSelectedValue();
        if (item == null || item.order == null) return;

        Order selected = item.order;

        int res = JOptionPane.showConfirmDialog(this,
                "Möchten Sie die Bestellung " + safe(selected.getOrderId()) + " abschließen?\n" +
                        "Der Lagerbestand wird entsprechend reduziert.",
                "Bestätigung",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, Main.iconSmall);

        if (res != JOptionPane.YES_OPTION) return;

        List<Article> articles = selected.getOrderedArticles().keySet().stream()
                .map(s -> ArticleManager.getInstance().getArticleByNumber(s))
                .filter(Objects::nonNull)
                .toList();

        List<String> warnings = new ArrayList<>();
        List<String> shortages = new ArrayList<>();

        for (Article article : articles) {
            if(article == null) continue;
            int ordered = selected.getOrderedArticles().get(article.getArticleNumber());
            int stock = article.getStockQuantity();

            int willFulfill = Math.min(stock, ordered);
            int missing = Math.max(0, ordered - stock);

            if (missing > 0) {
                shortages.add(String.format(
                        "- %s (%s): Bestellt=%d, Lager=%d → Wird geliefert=%d, Fehlmenge=%d",
                        safe(article.getName()),
                        safe(article.getArticleNumber()),
                        ordered,
                        stock,
                        willFulfill,
                        missing
                ));
            } else {
                int newStockAfterFull = stock - ordered;
                if (newStockAfterFull < article.getMinStockLevel()) {
                    warnings.add(String.format(
                            "- %s (%s): Neuer Bestand=%d < Mindestbestand=%d",
                            safe(article.getName()),
                            safe(article.getArticleNumber()),
                            newStockAfterFull,
                            article.getMinStockLevel()
                    ));
                }
            }
        }

        // ---------- NEW: open PartialOrderGUI instead of immediate partial completion ----------
        if (!shortages.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>WARNUNG: Nicht genügend Lagerbestand!</b><br/><br/>");
            msg.append("Folgende Artikel können nicht vollständig geliefert werden:<br/><br/>");
            shortages.forEach(s -> msg.append(s).append("<br/>"));
            msg.append("<br/>Möchten Sie die Teilbestellung jetzt vervollständigen?</html>");

            int choice = JOptionPane.showConfirmDialog(this,
                    msg.toString(),
                    "Teilbestellung",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);

            if (choice != JOptionPane.YES_OPTION) return;

            // Open the new GUI
            PartialOrderGUI gui = new PartialOrderGUI(selected);
            gui.setVisible(true);

            // After closing partial GUI, refresh list (window listener)
            gui.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent e) {
                    refreshList();
                }
            });

            return;
        }
        // -------------------------------------------------------------------------------------

        if (!warnings.isEmpty()) {
            StringBuilder msg = new StringBuilder("<html><b>WARNUNG: Mindestbestand wird unterschritten!</b><br/><br/>");
            warnings.forEach(warn -> msg.append(warn).append("<br/>"));
            msg.append("<br/>Möchten Sie trotzdem fortfahren?</html>");
            int warnRes = JOptionPane.showConfirmDialog(this, msg.toString(), "Warnung",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            if (warnRes != JOptionPane.YES_OPTION) return;
        }

        ArticleManager articleManager = ArticleManager.getInstance();
        boolean allStockUpdated = true;

        for (Article article : articles) {
            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
            if (!articleManager.removeFromStock(article.getArticleNumber(), qty)) {
                allStockUpdated = false;
                System.err.println("[CompleteOrderGUI] Fehler beim Reduzieren des Lagerbestands für Artikel: " + article.getArticleNumber());
            }
        }

        updateUserAndLog(selected);

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

    private void updateUserAndLog(Order selected) {
        if(selected == null) return;
        UserManager userManager = UserManager.getInstance();
        String sender = safe(selected.getSenderName()).toLowerCase();

        User user = userManager.getUserByName(sender);
        if (user == null) user = createUser(sender);

        if (!user.getOrders().contains(selected.getOrderId())) {
            user.getOrders().add(selected.getOrderId());
        }

        userManager.updateUser(user);
        OrderLoggingUtils.getInstance().addLogEntry(selected, user);
    }

    /**
     * Displays this GUI on the Event Dispatch Thread (EDT). Should be called after creating an instance.
     */
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
            setLayout(new BorderLayout(10, 6));
            setOpaque(true);

            // a bit more breathing room on left/right
            padding = BorderFactory.createEmptyBorder(10, 12, 10, 12);

            idLabel = new JLabel();
            idLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

            nameLabel = new JLabel();
            nameLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));

            deptLabel = new JLabel();
            deptLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));

            dateLabel = new JLabel();
            dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));

            // --- Center stack (name + meta row) -----------------------------------
            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            center.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

            // keep labels aligned nicely
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel metaRow = new JPanel();
            metaRow.setOpaque(false);
            metaRow.setLayout(new BoxLayout(metaRow, BoxLayout.X_AXIS));
            metaRow.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel sep = new JLabel("•"); // cleaner than "|"
            sep.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
            sep.setForeground(ThemeManager.getTextSecondaryColor());

            deptLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            // ensure meta labels don't collapse in BoxLayout
            deptLabel.setMinimumSize(deptLabel.getPreferredSize());
            dateLabel.setMinimumSize(dateLabel.getPreferredSize());

            metaRow.add(deptLabel);
            metaRow.add(Box.createHorizontalStrut(8));
            metaRow.add(sep);
            metaRow.add(Box.createHorizontalStrut(8));
            metaRow.add(dateLabel);
            metaRow.add(Box.createHorizontalGlue()); // pushes content left

            center.add(nameLabel);
            center.add(Box.createVerticalStrut(4));
            center.add(metaRow);

            // --- Assemble ----------------------------------------------------------
            add(idLabel, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends OrderListItem> list,
                                                      OrderListItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            Order order = value.order;

            Color bg = ThemeManager.getCardBackgroundColor();
            Color fg = ThemeManager.getTextPrimaryColor();
            Color muted = ThemeManager.getTextSecondaryColor();
            Color divider = ThemeManager.getDividerColor();
            Color accent = ThemeManager.getAccentColor();

            idLabel.setText(UnicodeSymbols.INFO + " " + safe(order.getOrderId()));

            String receiver = safe(order.getReceiverName());
            nameLabel.setText("<html><b>Empfänger:</b> " + escapeHtml(receiver) + "</html>");

            deptLabel.setText(UnicodeSymbols.DEPARTMENT + " " + safe(order.getDepartment()));
            dateLabel.setText(UnicodeSymbols.CALENDAR + " " + safe(order.getOrderDate()));

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

        private static String escapeHtml(String s) {
            return s == null ? "" : s
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;");
        }
    }
}