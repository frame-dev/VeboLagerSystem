package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ArticleListGUI extends JFrame {

    private static Map<Article, Integer> articlesAndQuantity = new HashMap<>();
    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> articleJList;
    private final JTextField searchField;
    private final JLabel countLabel;

    public ArticleListGUI() {
        setTitle("Artikel Liste");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(420, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Alle ausgewählten Artikel", SwingConstants.LEFT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setForeground(new Color(34, 50, 72));
        header.add(title, BorderLayout.WEST);

        countLabel = new JLabel("", SwingConstants.RIGHT);
        countLabel.setFont(countLabel.getFont().deriveFont(12f));
        countLabel.setForeground(new Color(110, 120, 130));
        header.add(countLabel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Center: list inside card-like panel
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        initializeArticleList();
        JScrollPane scrollPane = new JScrollPane(articleJList);
        scrollPane.setBorder(null);
        card.add(scrollPane, BorderLayout.CENTER);

        add(card, BorderLayout.CENTER);

        // Bottom: search + buttons
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        bottom.setOpaque(false);

        JPanel searchPanel = new JPanel(new BorderLayout(6, 6));
        searchPanel.setOpaque(false);
        searchField = new JTextField();
        searchField.setColumns(20);
        searchField.setToolTipText("Search by name...");
        searchPanel.add(searchField, BorderLayout.CENTER);
        JButton clearSearch = new JButton("Clear");
        searchPanel.add(clearSearch, BorderLayout.EAST);
        bottom.add(searchPanel, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
        btnPanel.setOpaque(false);
        JButton removeBtn = new JButton("Remove Selected");
        JButton clearAllBtn = new JButton("Clear All");
        JButton closeBtn = new JButton("Close");
        btnPanel.add(removeBtn);
        btnPanel.add(clearAllBtn);
        btnPanel.add(closeBtn);
        bottom.add(btnPanel, BorderLayout.SOUTH);

        add(bottom, BorderLayout.SOUTH);

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

        removeBtn.addActionListener(e -> {
            int sel = articleJList.getSelectedIndex();
            if (sel == -1) return;
            String value = listModel.getElementAt(sel);
            String name = value.split("-")[0];
            Article toRemove = articlesAndQuantity.keySet().stream()
                    .filter(a -> {
                        try {
                            return a.getName().equals(name);
                        } catch (Exception ex) {
                            return a.toString().equals(name);
                        }
                    })
                    .findFirst().orElse(null);
            // remove matching first article by name
            if (toRemove != null) {
                articlesAndQuantity.remove(toRemove);
                refreshArticleList();
            }
        });

        clearAllBtn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Alle Artikel entfernen?", "Bestätigen", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
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
                        String value = listModel.getElementAt(idx);
                        JOptionPane.showMessageDialog(ArticleListGUI.this, value, "Artikel", JOptionPane.INFORMATION_MESSAGE);
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
        articleJList.setFixedCellHeight(36);
        articleJList.setCellRenderer(new ArticleCellRenderer());
    }

    private void filterList() {
        String q = searchField.getText().trim().toLowerCase();
        List<Article> filtered;
        if (q.isEmpty()) filtered = articlesAndQuantity.keySet().stream().toList();
        else filtered = articlesAndQuantity.keySet().stream()
                .filter(a -> displayFor(a).toLowerCase().contains(q))
                .collect(Collectors.toList());
        listModel.clear();
        for (Article a : filtered) listModel.addElement(displayFor(a));
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
        for (Article article : articlesAndQuantity.keySet()) listModel.addElement(displayFor(article));
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

    // custom renderer for nicer visuals
    private static class ArticleCellRenderer extends DefaultListCellRenderer {
        private final Border padding = BorderFactory.createEmptyBorder(6, 10, 6, 10);

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setBorder(padding);
            label.setFont(label.getFont().deriveFont(14f));
            label.setOpaque(true);
            if (isSelected) {
                label.setBackground(new Color(28, 120, 220));
                label.setForeground(Color.WHITE);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(new Color(34, 50, 72));
            }
            return label;
        }
    }
}