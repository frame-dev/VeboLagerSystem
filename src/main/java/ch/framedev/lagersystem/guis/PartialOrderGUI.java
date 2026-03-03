package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class PartialOrderGUI extends JFrame {

    private final Order order;
    private final Map<Article, JTextField> quantityFields = new HashMap<>();

    public PartialOrderGUI(@NotNull Order order) {
        this.order = order;

        setTitle("Teilbestellung vervollständigen");
        setSize(520, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        Color bg = ThemeManager.getBackgroundColor();
        Color card = ThemeManager.getCardBackgroundColor();
        Color border = ThemeManager.getBorderColor();

        JPanel root = new JPanel(new BorderLayout(10,10));
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(15,15,15,15));
        setContentPane(root);

        // HEADER
        JLabel title = new JLabel("Teilbestellung vervollständigen");
        title.setFont(new Font("Dialog", Font.BOLD, 18));
        title.setForeground(ThemeManager.getTextPrimaryColor());
        root.add(title, BorderLayout.NORTH);

        // CENTER CARD
        JPanel cardPanel = new JPanel(new BorderLayout(10,10));
        cardPanel.setBackground(card);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(15,15,15,15)
        ));

        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(card);

        // build list of missing articles
        order.getOrderedArticles().forEach((articleNr, orderedQty) -> {

            Article article = ArticleManager.getInstance().getArticleByNumber(articleNr);
            if(article == null) return;

            int stock = article.getStockQuantity();

            if(stock >= orderedQty) return; // skip already fulfilled

            int missing = orderedQty - stock;

            JPanel row = new JPanel(new BorderLayout(8,0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4,0,4,0));

            JLabel label = new JLabel(
                    article.getName() + "  (Fehlmenge: " + missing + ")"
            );
            label.setForeground(ThemeManager.getTextPrimaryColor());

            JTextField field = new JTextField();
            field.setPreferredSize(new Dimension(60,28));
            field.setBorder(BorderFactory.createLineBorder(border));

            quantityFields.put(article, field);

            row.add(label, BorderLayout.CENTER);
            row.add(field, BorderLayout.EAST);
            listPanel.add(row);
        });

        cardPanel.add(scroll, BorderLayout.CENTER);
        root.add(cardPanel, BorderLayout.CENTER);

        // BUTTONS
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);

        JButton cancel = new JButton("Abbrechen");
        JButton finish = new JButton("Bestellung abschließen");

        cancel.addActionListener(e -> dispose());
        finish.addActionListener(e -> applyCompletion());

        buttons.add(cancel);
        buttons.add(finish);

        root.add(buttons, BorderLayout.SOUTH);
    }

    private void applyCompletion() {

        ArticleManager articleManager = ArticleManager.getInstance();

        quantityFields.forEach((article, field) -> {

            String txt = field.getText().trim();
            if(txt.isEmpty()) return;

            try {
                int addQty = Integer.parseInt(txt);
                if(addQty <= 0) return;

                articleManager.addToStock(article.getArticleNumber(), addQty);

            } catch (NumberFormatException ignored) {}
        });

        order.setStatus("Abgeschlossen");
        OrderManager.getInstance().updateOrder(order);

        JOptionPane.showMessageDialog(
                this,
                "Bestellung erfolgreich vervollständigt.",
                "Fertig",
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall
        );

        dispose();
    }
}