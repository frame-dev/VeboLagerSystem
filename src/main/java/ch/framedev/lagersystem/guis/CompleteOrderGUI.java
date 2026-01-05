package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.List;

public class CompleteOrderGUI extends JFrame {

    private JList<String> ordersList;
    private DefaultListModel<String> listModel;
    private List<Order> currentOrders;
    private final JLabel detailsLabel;
    private final JButton completeButton;
    private final JButton refreshButton;
    private final JButton closeButton;

    public CompleteOrderGUI() {
        setTitle("Bestellung Abschließen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        setContentPane(content);

        JLabel title = new JLabel("Bestellung Abschließen", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(24.0f));
        content.add(title, BorderLayout.NORTH);

        // initialize detailsLabel early so refreshList() (called by initializeOrdersList) won't NPE
        detailsLabel = new JLabel("Wähle eine Bestellung aus, um Details zu sehen.");


        // Details and controls
        JPanel south = new JPanel(new BorderLayout(8, 8));
        south.add(detailsLabel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        completeButton = new JButton("Bestellung abschließen");
        completeButton.setEnabled(false);
        refreshButton = new JButton("Aktualisieren");
        closeButton = new JButton("Schließen");

        buttons.add(refreshButton);
        buttons.add(completeButton);
        buttons.add(closeButton);
        south.add(buttons, BorderLayout.SOUTH);

        content.add(south, BorderLayout.SOUTH);


        initializeOrdersList();
        JScrollPane scrollPane = new JScrollPane(ordersList);
        scrollPane.setPreferredSize(new Dimension(600, 300));
        content.add(scrollPane, BorderLayout.CENTER);

        attachListeners();

        pack();
        setLocationRelativeTo(null);
    }

    private void attachListeners() {
        ordersList.addListSelectionListener(e -> {
            int idx = ordersList.getSelectedIndex();
            if (idx >= 0 && currentOrders != null && idx < currentOrders.size()) {
                Order o = currentOrders.get(idx);
                detailsLabel.setText(String.format("<html>Order ID: %s<br/>Empfänger: %s</html>",
                        o.getOrderId(), o.getReceiverName()));
                completeButton.setEnabled(true);
            } else {
                detailsLabel.setText("Wähle eine Bestellung aus, um Details zu sehen.");
                completeButton.setEnabled(false);
            }
        });

        completeButton.addActionListener(e -> {
            int idx = ordersList.getSelectedIndex();
            if (idx < 0 || currentOrders == null || idx >= currentOrders.size()) {
                return;
            }
            Order selected = currentOrders.get(idx);
            int res = JOptionPane.showConfirmDialog(
                    CompleteOrderGUI.this,
                    "Möchten Sie die Bestellung " + selected.getOrderId() + " abschließen?",
                    "Bestätigung",
                    JOptionPane.YES_NO_OPTION
            );
            if (res == JOptionPane.YES_OPTION) {
                boolean removed = false;
                try {
                    List<Order> managerOrders = OrderManager.getInstance().getOrders();
                    if (managerOrders != null) {
                        List<Article> articles = selected.getOrderedArticles().keySet().stream().map(s -> ArticleManager.getInstance().getArticleByNumber(s)).toList();
                        articles.forEach(article -> {
                            // add warning if article is less than min stock
                            int qty = selected.getOrderedArticles().get(article.getArticleNumber());
                            int currentStock = article.getStockQuantity();
                            int newStock = currentStock - qty;
                            if (newStock < article.getMinStockLevel()) {
                                JOptionPane.showMessageDialog(CompleteOrderGUI.this,
                                        "Warnung: Der Lagerbestand des Artikels '" + article.getName() + "' (" + article.getArticleNumber() + ") " +
                                                "fällt unter das Mindestlagerlevel (" + article.getMinStockLevel() + ") nach Abschluss dieser Bestellung.");
                            }
                            if (currentStock < qty) {
                                // not enough stock to fulfill order
                                JOptionPane.showMessageDialog(CompleteOrderGUI.this, "Warnung: Zu wenig im Lager für Artikel '" + article.getName() + "' (" + article.getArticleNumber() + "). " +
                                        "Aktueller Lagerbestand: " + currentStock + ", Bestellte Menge: " + qty);
                                return;
                            }
                            article.setStockQuantity(article.getStockQuantity() - qty);
                            ArticleManager.getInstance().updateArticle(article);
                        });
                        removed = managerOrders.remove(selected);
                    }
                } catch (Exception ignore) {
                }
                if (!removed && currentOrders != null) {
                    removed = currentOrders.remove(selected);
                }
                if (removed) {
                    JOptionPane.showMessageDialog(CompleteOrderGUI.this, "Bestellung abgeschlossen: " + selected.getOrderId());
                } else {
                    JOptionPane.showMessageDialog(CompleteOrderGUI.this, "Bestellung lokal markiert als abgeschlossen: " + selected.getOrderId());
                }
                refreshList();
            }
        });

        refreshButton.addActionListener(e -> refreshList());

        closeButton.addActionListener(e -> dispose());
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    private void initializeOrdersList() {
        listModel = new DefaultListModel<>();
        ordersList = new JList<>(listModel);
        ordersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshList();
    }

    private void refreshList() {
        listModel.clear();
        try {
            currentOrders = OrderManager.getInstance().getOrders();
        } catch (Exception ex) {
            currentOrders = null;
        }
        if (currentOrders != null) {
            for (Order order : currentOrders) {
                listModel.addElement(order.getOrderId() + " - " + order.getReceiverName());
            }
        }
        if (detailsLabel != null) {
            detailsLabel.setText("Wähle eine Bestellung aus, um Details zu sehen.");
        }
        completeButton.setEnabled(false);
    }
}