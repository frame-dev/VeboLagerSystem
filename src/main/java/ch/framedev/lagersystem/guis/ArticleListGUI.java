package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class ArticleListGUI extends JFrame {

    private static Map<Article, Integer> articlesAndQuantity = new HashMap<>();
    private final DefaultListModel<ArticleDisplay> listModel = new DefaultListModel<>();
    private JList<ArticleDisplay> articleJList;
    private final JTextField searchField;
    private final JLabel countLabel;

    // Helper class to track article and quantity together
    private record ArticleDisplay(Article article, int quantity) {
        @Override
        public String toString() {
            try {
                String name = article.getName();
                if (name.length() > 30) {
                    name = name.substring(0, 27) + "...";
                }
                return String.format("%-30s | Nr: %-8s | Qty: %d",
                        name,
                        article.getArticleNumber(),
                        quantity);
            } catch (Exception ex) {
                return article.toString() + " | Qty: " + quantity;
            }
        }
    }

    public ArticleListGUI() {
        setTitle("📋 Artikel Liste");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(650, 750);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Main wrapper with gradient-like background
        JPanel mainWrapper = new JPanel(new BorderLayout(0, 0));
        mainWrapper.setBackground(new Color(245, 247, 250));
        mainWrapper.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Header with rounded panel and shadow effect
        RoundedPanel headerPanel = new RoundedPanel(new Color(30, 58, 95), 20);
        headerPanel.setLayout(new BorderLayout(16, 0));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(20, 48, 85), 1),
            BorderFactory.createEmptyBorder(24, 28, 24, 28)
        ));

        // Icon and title section
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleSection.setOpaque(false);

        JLabel iconLabel = new JLabel("📋");
        iconLabel.setFont(iconLabel.getFont().deriveFont(28f));
        titleSection.add(iconLabel);

        JLabel title = new JLabel("Ausgewählte Artikel");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setForeground(Color.WHITE);
        titleSection.add(title);

        headerPanel.add(titleSection, BorderLayout.WEST);

        countLabel = new JLabel("", SwingConstants.RIGHT);
        countLabel.setFont(countLabel.getFont().deriveFont(Font.BOLD, 15f));
        countLabel.setForeground(new Color(180, 200, 240));
        headerPanel.add(countLabel, BorderLayout.EAST);

        mainWrapper.add(headerPanel, BorderLayout.NORTH);

        // Center: modern card with list and enhanced search
        RoundedPanel card = new RoundedPanel(Color.WHITE, 20);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 0, 0, 0),
                BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
                )
        ));

        // Search panel with modern styling at top of card
        JPanel searchPanel = new JPanel(new BorderLayout(10, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        // Search icon container
        JPanel searchIconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        searchIconPanel.setOpaque(false);
        searchIconPanel.setPreferredSize(new Dimension(40, 40));
        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(searchIcon.getFont().deriveFont(18f));
        searchIconPanel.add(searchIcon);
        searchPanel.add(searchIconPanel, BorderLayout.WEST);

        // Search field with modern styling
        searchField = new JTextField();
        searchField.setFont(searchField.getFont().deriveFont(15f));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        searchField.setToolTipText("Suche nach Artikelname oder Nummer...");
        searchField.setBackground(new Color(250, 252, 255));

        // Add focus effects to search field
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(52, 152, 219), 2),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                searchField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton clearSearch = createStyledButton("✕", new Color(231, 76, 60), Color.WHITE, 36, 36);
        clearSearch.setToolTipText("Suche löschen");
        clearSearch.setFont(clearSearch.getFont().deriveFont(Font.BOLD, 16f));
        searchPanel.add(clearSearch, BorderLayout.EAST);

        card.add(searchPanel, BorderLayout.NORTH);

        initializeArticleList();
        JScrollPane scrollPane = new JScrollPane(articleJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        card.add(scrollPane, BorderLayout.CENTER);

        mainWrapper.add(card, BorderLayout.CENTER);

        // Bottom: modern action buttons with enhanced styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JButton editQtyBtn = createStyledButton("✏️ Menge ändern", new Color(52, 152, 219), Color.WHITE, 0, 0);
        editQtyBtn.setPreferredSize(new Dimension(160, 40));

        JButton removeBtn = createStyledButton("🗑️ Entfernen", new Color(231, 76, 60), Color.WHITE, 0, 0);
        removeBtn.setPreferredSize(new Dimension(140, 40));

        JButton clearAllBtn = createStyledButton("🧹 Alle löschen", new Color(243, 156, 18), Color.WHITE, 0, 0);
        clearAllBtn.setPreferredSize(new Dimension(140, 40));

        JButton closeBtn = createStyledButton("Schließen", new Color(149, 165, 166), Color.WHITE, 0, 0);
        closeBtn.setPreferredSize(new Dimension(120, 40));

        buttonPanel.add(editQtyBtn);
        buttonPanel.add(removeBtn);
        buttonPanel.add(clearAllBtn);
        buttonPanel.add(closeBtn);

        mainWrapper.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainWrapper);

        // Wiring
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterList();
            }
        });
        clearSearch.addActionListener(e -> {
            searchField.setText("");
            filterList();
        });

        editQtyBtn.addActionListener(e -> {
            int sel = articleJList.getSelectedIndex();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            ArticleDisplay selected = listModel.getElementAt(sel);
            String input = JOptionPane.showInputDialog(this,
                "Neue Menge für \"" + selected.article.getName() + "\":",
                selected.quantity);
            if (input != null) {
                try {
                    int newQty = Integer.parseInt(input.trim());
                    if (newQty <= 0) {
                        JOptionPane.showMessageDialog(this, "Menge muss größer als 0 sein.", "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE,
                                Main.icon);
                        return;
                    }
                    articlesAndQuantity.put(selected.article, newQty);
                    refreshArticleList();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Ungültige Zahl: " + input, "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });

        removeBtn.addActionListener(e -> {
            int sel = articleJList.getSelectedIndex();
            if (sel == -1) return;
            ArticleDisplay selected = listModel.getElementAt(sel);
            articlesAndQuantity.remove(selected.article);
            refreshArticleList();
        });

        clearAllBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Alle Artikel entfernen?", "Bestätigen", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.icon) == JOptionPane.YES_OPTION) {
                articlesAndQuantity.clear();
                refreshArticleList();
            }
        });

        closeBtn.addActionListener(e -> dispose());

        // double click to show article details (if desired)
        articleJList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = articleJList.locationToIndex(e.getPoint());
                    if (idx != -1) {
                        ArticleDisplay ad = listModel.getElementAt(idx);
                        String details = String.format(
                            "<html><h3>%s</h3>" +
                            "<b>Article Number:</b> %s<br/>" +
                            "<b>Details:</b> %s<br/>" +
                            "<b>Stock:</b> %d<br/>" +
                            "<b>Min Stock:</b> %d<br/>" +
                            "<b>Selected Qty:</b> %d<br/>" +
                            "<b>Sell Price:</b> %.2f CHF<br/>" +
                            "<b>Purchase Price:</b> %.2f CHF</html>",
                            ad.article.getName(),
                            ad.article.getArticleNumber(),
                            ad.article.getDetails(),
                            ad.article.getStockQuantity(),
                            ad.article.getMinStockLevel(),
                            ad.quantity,
                            ad.article.getSellPrice(),
                            ad.article.getPurchasePrice()
                        );
                        JOptionPane.showMessageDialog(ArticleListGUI.this, details, "Artikel Details", JOptionPane.INFORMATION_MESSAGE,
                                Main.icon);
                    }
                }
            }
        });

        refreshArticleList();
        setVisible(true);
    }

    private void initializeArticleList() {
        articleJList = new JList<>(listModel);
        articleJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        articleJList.setFixedCellHeight(72); // Increased height for better display
        articleJList.setCellRenderer(new ArticleCellRenderer());
        articleJList.setBackground(Color.WHITE);
    }

    private void filterList() {
        String q = searchField.getText().trim().toLowerCase();
        List<Map.Entry<Article, Integer>> filtered;
        if (q.isEmpty()) {
            filtered = new ArrayList<>(articlesAndQuantity.entrySet());
        } else {
            filtered = articlesAndQuantity.entrySet().stream()
                    .filter(entry -> displayFor(entry.getKey()).toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }
        listModel.clear();
        for (Map.Entry<Article, Integer> entry : filtered) {
            listModel.addElement(new ArticleDisplay(entry.getKey(), entry.getValue()));
        }
        countLabel.setText(listModel.getSize() + " item(s)");
    }

    private String displayFor(Article a) {
        // prefer name, fallback to toString
        try {
            return a.getName() + "-" + a.getArticleNumber() + " (Stock: " + a.getStockQuantity() + ") \\ " + a.getDetails() + " | Qty: " + articlesAndQuantity.get(a);
        } catch (Exception ex) {
            return a.toString();
        }
    }

    public void refreshArticleList() {
        listModel.clear();
        for (Map.Entry<Article, Integer> entry : articlesAndQuantity.entrySet()) {
            listModel.addElement(new ArticleDisplay(entry.getKey(), entry.getValue()));
        }
        countLabel.setText(listModel.getSize() + " item(s)");
    }

    public static void setArticles(Map<Article, Integer> articleMap) {
        articlesAndQuantity = articleMap;
    }

    public static Map<Article, Integer> getArticles() {
        return articlesAndQuantity;
    }

    public static void addArticle(Article article, int quantity) {
        articlesAndQuantity.put(article, quantity);
    }

    public static void removeArticle(Article article) {
        articlesAndQuantity.remove(article);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> {
            if (!isVisible())
                setVisible(true);
            else
                toFront();
        });
    }

    public static Map<Article, Integer> getArticlesAndQuantity() {
        return articlesAndQuantity;
    }

    /**
     * Helper method to create styled buttons with consistent appearance
     */
    private JButton createStyledButton(String text, Color bgColor, Color fgColor, int width, int height) {
        JButton button = new JButton(text);
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (width > 0 && height > 0) {
            button.setPreferredSize(new Dimension(width, height));
            button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        } else {
            button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
            ));
        }

        // Enhanced hover effect with smooth transition
        button.addMouseListener(new MouseAdapter() {
            private final Color originalBg = bgColor;
            private final Color hoverBg = new Color(
                Math.min(255, bgColor.getRed() + 20),
                Math.min(255, bgColor.getGreen() + 20),
                Math.min(255, bgColor.getBlue() + 20)
            );

            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(originalBg.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(hoverBg);
            }
        });

        return button;
    }

    // Custom renderer for nicer visuals with modern card-like appearance
    private static class ArticleCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                     boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof ArticleDisplay)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            ArticleDisplay ad = (ArticleDisplay) value;

            JPanel panel = new JPanel(new BorderLayout(12, 0));
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 242, 245)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
            ));
            panel.setPreferredSize(new Dimension(list.getWidth() - 10, 72));

            if (isSelected) {
                panel.setBackground(new Color(41, 128, 185));
            } else {
                panel.setBackground(index % 2 == 0 ? Color.WHITE : new Color(249, 250, 251));
            }

            // Left side: Article info with better structure
            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(ad.article.getName());
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
            nameLabel.setForeground(isSelected ? Color.WHITE : new Color(31, 45, 61));
            nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel detailLabel = new JLabel(String.format("📦 Nr: %s  •  Stock: %d",
                ad.article.getArticleNumber(), ad.article.getStockQuantity()));
            detailLabel.setFont(detailLabel.getFont().deriveFont(12f));
            detailLabel.setForeground(isSelected ? new Color(220, 230, 240) : new Color(120, 130, 140));
            detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(nameLabel);
            infoPanel.add(Box.createVerticalStrut(4));
            infoPanel.add(detailLabel);

            panel.add(infoPanel, BorderLayout.CENTER);

            // Right side: Quantity badge with modern styling
            JPanel badgePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            badgePanel.setOpaque(false);

            JLabel qtyLabel = new JLabel("Qty: " + ad.quantity);
            qtyLabel.setFont(qtyLabel.getFont().deriveFont(Font.BOLD, 13f));
            qtyLabel.setForeground(isSelected ? Color.WHITE : new Color(62, 84, 98));
            qtyLabel.setOpaque(true);
            qtyLabel.setBackground(isSelected ? new Color(52, 152, 219) : new Color(230, 240, 245));
            qtyLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(isSelected ? new Color(52, 152, 219) : new Color(200, 215, 225), 1),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));
            qtyLabel.setHorizontalAlignment(SwingConstants.CENTER);

            badgePanel.add(qtyLabel);
            panel.add(badgePanel, BorderLayout.EAST);

            return panel;
        }
    }

    // Rounded panel for modern UI
    private static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
            this.backgroundColor = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}