package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/**
 * The ArticleListGUI class provides a graphical user interface for displaying and managing a list of articles along with their quantities. It allows users to view article details, search/filter the list, edit quantities, remove articles, and clear the entire list. The GUI is designed with a modern look using rounded panels and styled buttons, and it integrates with the ThemeManager for dynamic theming. The class also includes static methods to set and retrieve the articles and their quantities, making it easy to integrate with other parts of the application.
 * @author framedev
 */
@SuppressWarnings("unused")
public class ArticleListGUI extends JFrame {

    private static Map<Article, Integer> articlesAndQuantity = new HashMap<>();

    // UI state
    private final DefaultListModel<ArticleDisplay> listModel = new DefaultListModel<>();
    private final List<ArticleDisplay> displayCache = new ArrayList<>();

    private final JTextField searchField = new JTextField();
    private final JLabel countLabel = new JLabel("", SwingConstants.RIGHT);
    private final JLabel subtitleLabel = new JLabel("", SwingConstants.LEFT);

    private JList<ArticleDisplay> articleJList;

    private final Timer searchDebounce = new Timer(180, e -> filterList());

    // Empty-state overlay
    private JLabel emptyStateLabel;

    // Helper class to track article and quantity together
    private record ArticleDisplay(Article article, int quantity, String searchableText) {
        @Override
        public String toString() {
            // Fallback (not used for renderer, but helpful for debugging)
            try {
                String name = article.getName();
                if (name != null && name.length() > 34) name = name.substring(0, 31) + "...";
                return String.format("%s | Nr: %s | Qty: %d", name, article.getArticleNumber(), quantity);
            } catch (Exception ex) {
                return article + " | Qty: " + quantity;
            }
        }
    }

    /**
     * Constructs the ArticleListGUI window, initializes the UI components, and sets up event listeners. The window displays a list of articles with their quantities, allows searching/filtering, and provides actions to edit quantities, remove articles, or clear the list. It also integrates with the ThemeManager for dynamic theming and applies a modern design with rounded panels and styled buttons.
     */
    public ArticleListGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle("📋 Artikel Liste");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 820);
        setLocationRelativeTo(null);

        // Timer config
        searchDebounce.setRepeats(false);

        // Build UI once
        initializeList();
        initializeComponents();
        wireGlobalShortcuts();
        wireSearch();

        // Icon
        if (Main.iconSmall != null) setIconImage(Main.iconSmall.getImage());

        refreshArticleList();
        setVisible(true);
    }

    // ---------- UI construction ----------

    private void initializeList() {
        articleJList = new JList<>(listModel);
        articleJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        articleJList.setFixedCellHeight(76);
        articleJList.setCellRenderer(new ArticleCellRenderer());
        articleJList.setBackground(ThemeManager.getCardBackgroundColor());
        articleJList.setForeground(ThemeManager.getTextPrimaryColor());
        articleJList.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        articleJList.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());

        articleJList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) showSelectedDetails();
            }
        });
    }

    private void initializeComponents() {
        // Base
        getContentPane().removeAll();
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(ThemeManager.getBackgroundColor());

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(true);
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        // Header
        RoundedPanel header = new RoundedPanel(ThemeManager.getHeaderBackgroundColor(), 22);
        header.setLayout(new BorderLayout(16, 0));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel icon = new JLabel("📋");
        icon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 28));
        left.add(icon);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Ausgewählte Artikel");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(255, 255, 255, 210));

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitleLabel);

        left.add(titleBox);

        header.add(left, BorderLayout.WEST);

        countLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        countLabel.setForeground(Color.WHITE);
        header.add(countLabel, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // Card
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 22);
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 0, 0, 0),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(16, 16, 16, 16)
                )
        ));

        // Search bar
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setOpaque(false);
        searchRow.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));

        JLabel searchIcon = new JLabel("🔎");
        searchIcon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 18));
        searchIcon.setPreferredSize(new Dimension(34, 34));
        searchIcon.setHorizontalAlignment(SwingConstants.CENTER);
        searchRow.add(searchIcon, BorderLayout.WEST);

        configureSearchField();
        searchRow.add(searchField, BorderLayout.CENTER);

        JButton clearBtn = createIconButton(ThemeManager.getErrorColor());
        clearBtn.setToolTipText("Suche löschen (Esc)");
        clearBtn.addActionListener(e -> clearSearch());
        searchRow.add(clearBtn, BorderLayout.EAST);

        card.add(searchRow, BorderLayout.NORTH);

        // List area + empty state overlay
        JScrollPane scrollPane = new JScrollPane(articleJList);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());

        // Put scrollPane into a layered pane to show empty state nicely
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new OverlayLayout(layered));
        layered.add(scrollPane, Integer.valueOf(0));

        emptyStateLabel = new JLabel("Keine Artikel in der Liste", SwingConstants.CENTER);
        emptyStateLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        emptyStateLabel.setForeground(ThemeManager.getTextSecondaryColor());
        emptyStateLabel.setOpaque(false);
        emptyStateLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        layered.add(emptyStateLabel, Integer.valueOf(1));

        card.add(layered, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 18));
        buttons.setOpaque(false);

        JButton editQtyBtn = createStyledButton("✏️ Menge ändern", ThemeManager.getAccentColor());
        editQtyBtn.setPreferredSize(new Dimension(170, 42));
        editQtyBtn.setToolTipText("Menge des ausgewählten Artikels ändern (Enter)");
        editQtyBtn.addActionListener(this::handleEditQuantity);

        JButton removeBtn = createStyledButton("🗑 Entfernen", ThemeManager.getErrorColor());
        removeBtn.setPreferredSize(new Dimension(145, 42));
        removeBtn.setToolTipText("Ausgewählten Artikel entfernen (Entf)");
        removeBtn.addActionListener(this::handleRemoveArticle);

        JButton clearAllBtn = createStyledButton("🧹 Alle löschen", ThemeManager.getWarningColor());
        clearAllBtn.setPreferredSize(new Dimension(145, 42));
        clearAllBtn.setToolTipText("Alle Artikel aus der Liste entfernen");
        clearAllBtn.addActionListener(this::handleClearAll);

        JButton closeBtn = createStyledButton("Schließen", ThemeManager.getSecondaryColor());
        closeBtn.setPreferredSize(new Dimension(120, 42));
        closeBtn.setToolTipText("Fenster schließen");
        closeBtn.addActionListener(e -> dispose());

        buttons.add(editQtyBtn);
        buttons.add(removeBtn);
        buttons.add(clearAllBtn);
        buttons.add(closeBtn);

        root.add(buttons, BorderLayout.SOUTH);

        setContentPane(root);
        revalidate();
        repaint();
    }

    private void configureSearchField() {
        searchField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 15));
        searchField.setToolTipText("Suche nach Artikelname, Nummer oder Details… (Ctrl+F)");
        searchField.setBackground(ThemeManager.getInputBackgroundColor());
        searchField.setForeground(ThemeManager.getTextPrimaryColor());
        searchField.setCaretColor(ThemeManager.getTextPrimaryColor());

        setSearchBorder(false);

        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { setSearchBorder(true); }
            @Override public void focusLost(FocusEvent e) { setSearchBorder(false); }
        });
    }

    private void setSearchBorder(boolean focused) {
        Color border = focused ? ThemeManager.getInputFocusBorderColor() : ThemeManager.getInputBorderColor();
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 2),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
    }

    private JButton createIconButton(Color bg) {
        JButton b = new JButton("✕");
        b.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        b.setPreferredSize(new Dimension(38, 38));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        b.addMouseListener(new HoverMouseAdapter(b, bg));
        return b;
    }

    private JButton createStyledButton(String text, Color bg) {
        if(text == null) throw new IllegalArgumentException("text must not be null");
        JButton b = new JButton(text);
        b.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        b.addMouseListener(new HoverMouseAdapter(b, bg));
        return b;
    }

    private static class HoverMouseAdapter extends MouseAdapter {
        private final JButton button;
        private final Color original;
        private final Color hover;
        private final Color pressed;

        HoverMouseAdapter(JButton button, Color original) {
            this.button = button;
            this.original = original;
            this.hover = ThemeManager.getButtonHoverColor(original);
            this.pressed = ThemeManager.getButtonPressedColor(original);
        }

        @Override public void mouseEntered(MouseEvent e) { button.setBackground(hover); }
        @Override public void mouseExited(MouseEvent e) { button.setBackground(original); }
        @Override public void mousePressed(MouseEvent e) { button.setBackground(pressed); }
        @Override public void mouseReleased(MouseEvent e) { button.setBackground(button.contains(e.getPoint()) ? hover : original); }
    }

    // ---------- Wiring / shortcuts ----------

    private void wireSearch() {
        // Debounced search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { searchDebounce.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { searchDebounce.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { searchDebounce.restart(); }
        });

        // ESC clears search when field focused
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { clearSearch(); }
        });
    }

    private void wireGlobalShortcuts() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // Ctrl+F => focus search
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        am.put("focusSearch", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        // Enter => edit qty (if list focused)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "editQty");
        am.put("editQty", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (articleJList.isFocusOwner() || articleJList.getSelectedIndex() != -1) {
                    handleEditQuantity(e);
                }
            }
        });

        // Delete => remove
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove");
        am.put("remove", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleRemoveArticle(e); }
        });

        // Ctrl+L => clear all
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "clearAll");
        am.put("clearAll", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { handleClearAll(e); }
        });
    }

    private void clearSearch() {
        searchField.setText("");
        filterList();
    }

    // ---------- Actions ----------

    private void handleEditQuantity(ActionEvent e) {
        int sel = articleJList.getSelectedIndex();
        if (sel == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel aus.", "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            return;
        }

        ArticleDisplay selected = listModel.getElementAt(sel);

        String input = JOptionPane.showInputDialog(this,
                "Neue Menge für \"" + safe(selected.article::getName) + "\":",
                selected.quantity);

        if (input == null) return;

        try {
            int newQty = Integer.parseInt(input.trim());
            if (newQty <= 0) {
                JOptionPane.showMessageDialog(this, "Menge muss größer als 0 sein.", "Ungültige Eingabe",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }
            articlesAndQuantity.put(selected.article, newQty);
            refreshArticleListPreservingQuery();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Zahl: " + input, "Fehler",
                    JOptionPane.ERROR_MESSAGE, Main.iconSmall);
        }
    }

    private void handleRemoveArticle(ActionEvent e) {
        int sel = articleJList.getSelectedIndex();
        if (sel == -1) return;

        ArticleDisplay selected = listModel.getElementAt(sel);
        articlesAndQuantity.remove(selected.article);
        refreshArticleListPreservingQuery();
    }

    private void handleClearAll(ActionEvent e) {
        if (articlesAndQuantity.isEmpty()) return;

        int res = JOptionPane.showConfirmDialog(this,
                "Alle Artikel entfernen?", "Bestätigen",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.iconSmall);

        if (res == JOptionPane.YES_OPTION) {
            articlesAndQuantity.clear();
            refreshArticleListPreservingQuery();
        }
    }

    private void showSelectedDetails() {
        int idx = articleJList.getSelectedIndex();
        if (idx == -1) return;

        ArticleDisplay ad = listModel.getElementAt(idx);

        String details = String.format(
                "<html><div style='width:420px;'>" +
                        "<h2 style='margin:0 0 8px 0;'>%s</h2>" +
                        "<b>Article Number:</b> %s<br/>" +
                        "<b>Details:</b> %s<br/>" +
                        "<b>Stock:</b> %d<br/>" +
                        "<b>Min Stock:</b> %d<br/>" +
                        "<b>Selected Qty:</b> %d<br/>" +
                        "<b>Sell Price:</b> %.2f CHF<br/>" +
                        "<b>Purchase Price:</b> %.2f CHF" +
                        "</div></html>",
                safe(ad.article::getName),
                safe(ad.article::getArticleNumber),
                safe(ad.article::getDetails),
                safeInt(ad.article::getStockQuantity),
                safeInt(ad.article::getMinStockLevel),
                ad.quantity,
                safeDouble(ad.article::getSellPrice),
                safeDouble(ad.article::getPurchasePrice)
        );

        JOptionPane.showMessageDialog(this, details, "Artikel Details",
                JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
    }

    // ---------- Data / filtering ----------

    private void filterList() {
        String q = searchField.getText().trim().toLowerCase(Locale.ROOT);

        if (displayCache.isEmpty() && !articlesAndQuantity.isEmpty()) {
            refreshArticleList(); // builds cache
        }

        List<ArticleDisplay> filtered = displayCache.stream()
                .filter(d -> q.isEmpty() || d.searchableText().contains(q))
                .toList();

        listModel.clear();
        for (ArticleDisplay d : filtered) listModel.addElement(d);

        updateCountersAndEmptyState(filtered.size(), q);
    }

    /**
     * Rebuild the display cache and refresh the JList. This should be called whenever the underlying articlesAndQuantity map changes in a way that might affect the display (e.g., article name changes, new article added, etc.). It preserves the current search query and selection state as much as possible.
     */
    public void refreshArticleList() {
        displayCache.clear();

        // Stable, pretty ordering: name -> article number
        List<Map.Entry<Article, Integer>> entries = new ArrayList<>(articlesAndQuantity.entrySet());
        entries.sort(Comparator
                .comparing((Map.Entry<Article, Integer> e) -> safe(e.getKey()::getName).toLowerCase(Locale.ROOT))
                .thenComparing(e -> safe(e.getKey()::getArticleNumber).toLowerCase(Locale.ROOT)));

        for (Map.Entry<Article, Integer> e : entries) {
            displayCache.add(createDisplay(e.getKey(), e.getValue()));
        }

        listModel.clear();
        for (ArticleDisplay d : displayCache) listModel.addElement(d);

        updateCountersAndEmptyState(listModel.getSize(), searchField.getText().trim());
    }

    private void refreshArticleListPreservingQuery() {
        // rebuild cache, then apply current query without losing it
        refreshArticleList();
        filterList();
    }

    private void updateCountersAndEmptyState(int shownCount, String query) {
        int total = articlesAndQuantity.size();

        if (query != null && !query.isBlank()) {
            countLabel.setText(shownCount + " / " + total);
            subtitleLabel.setText("Gefiltert nach: \"" + query + "\"");
        } else {
            countLabel.setText(total + " item(s)");
            subtitleLabel.setText(total == 0 ? "—" : "Tipp: Ctrl+F zum Suchen · Enter Menge ändern · Entf Entfernen");
        }

        boolean empty = (shownCount == 0);
        emptyStateLabel.setVisible(empty);

        if (empty) {
            if (query != null && !query.isBlank()) {
                emptyStateLabel.setText("Keine Treffer für \"" + query + "\"");
            } else {
                emptyStateLabel.setText("Keine Artikel in der Liste");
            }
        }
    }

    private ArticleDisplay createDisplay(Article article, int quantity) {
        if(article == null) throw new IllegalArgumentException("article must not be null");
        String name = safe(article::getName);
        String number = safe(article::getArticleNumber);
        String details = safe(article::getDetails);

        String searchable = (name + " " + number + " " + details + " qty:" + quantity)
                .toLowerCase(Locale.ROOT);

        return new ArticleDisplay(article, quantity, searchable);
    }

    private String safe(Supplier<String> supplier) {
        try {
            String v = supplier.get();
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }

    private int safeInt(IntSupplierEx supplier) {
        try { return supplier.getAsInt(); } catch (Exception e) { return 0; }
    }

    private double safeDouble(DoubleSupplierEx supplier) {
        try { return supplier.getAsDouble(); } catch (Exception e) { return 0.0; }
    }

    @FunctionalInterface private interface IntSupplierEx { int getAsInt() throws Exception; }
    @FunctionalInterface private interface DoubleSupplierEx { double getAsDouble() throws Exception; }

    // ---------- Theme / lifecycle ----------

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    /**
     * Called by ThemeManager when the theme changes
     */
    public void updateTheme() {
        SwingUtilities.invokeLater(() -> {
            // Rebuild UI to apply new colors cleanly
            initializeComponents();

            // Re-apply list colors (renderer uses ThemeManager dynamically, but base colors matter)
            articleJList.setBackground(ThemeManager.getCardBackgroundColor());
            articleJList.setForeground(ThemeManager.getTextPrimaryColor());
            articleJList.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
            articleJList.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());

            // Search field colors/border
            configureSearchField();

            refreshArticleListPreservingQuery();
        });
    }

    /**
     * Brings the window to the front if it's already visible, or makes it visible if it's not. This is useful for reusing the same instance of ArticleListGUI without creating multiple windows. If the window is already open, it will be focused and brought to the front; if it's not open, it will be shown.
     */
    public void display() {
        SwingUtilities.invokeLater(() -> {
            if (!isVisible()) setVisible(true);
            else toFront();
        });
    }

    // ---------- Static API ----------

    /**
     * Set the articles and their quantities to display in the list. This replaces any existing data.
     * @param articleMap Map of Article to quantity. If null, it will be treated as an empty map.
     */
    public static void setArticles(Map<Article, Integer> articleMap) {
        articlesAndQuantity = (articleMap == null) ? new HashMap<>() : articleMap;
    }

    /**
     * Get the current map of articles and their quantities. Modifying this map will affect the list directly, so be cautious when using it. If you want to modify the list, it's safer to use addArticle/removeArticle methods or create a copy of the map before making changes.
     * @return The current map of Article to quantity. Never null, but may be empty.
     */
    public static Map<Article, Integer> getArticles() {
        return articlesAndQuantity;
    }

    /**
     * Get the current map of articles and their quantities. This is the same as getArticles() but named to clarify that it includes quantities. Modifying this map will affect the list directly, so be cautious when using it. If you want to modify the list, it's safer to use addArticle/removeArticle methods or create a copy of the map before making changes.
     * @return The current map of Article to quantity. Never null, but may be empty.
     */
    public static Map<Article, Integer> getArticlesAndQuantity() {
        return articlesAndQuantity;
    }

    /**
     * Add an article with a specified quantity to the list. If the article already exists, its quantity will be updated. Quantity must be greater than 0; if not, it defaults to 1.
     * @param article The Article to add or update. If null, the method does nothing.
     * @param quantity The quantity for the article. Must be greater than 0; if not, it defaults to 1.
     */
    public static void addArticle(Article article, int quantity) {
        if (article == null) return;
        if (quantity <= 0) quantity = 1;
        articlesAndQuantity.put(article, quantity);
    }

    /**
     * Remove an article from the list. If the article does not exist, the method does nothing.
     * @param article The Article to remove. If null, the method does nothing.
     */
    public static void removeArticle(Article article) {
        if (article == null) return;
        articlesAndQuantity.remove(article);
    }

    // ---------- Renderer / visuals ----------

    private static class ArticleCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (!(value instanceof ArticleDisplay ad)) {
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            Article article = ad.article;
            int quantity = ad.quantity;

            JPanel panel = getJPanel(index, isSelected);

            // Left info
            JPanel info = new JPanel();
            info.setOpaque(false);
            info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

            String name = safeStatic(article::getName);
            String number = safeStatic(article::getArticleNumber);
            int stock = safeIntStatic(article::getStockQuantity);

            JLabel nameLabel = new JLabel(name);
            nameLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
            nameLabel.setForeground(isSelected ? ThemeManager.getTextOnPrimaryColor() : ThemeManager.getTextPrimaryColor());

            JLabel meta = new JLabel(String.format("📦 Nr: %s   ·   Stock: %d", number, stock));
            meta.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            meta.setForeground(isSelected ? ThemeManager.getTextOnPrimaryColor() : ThemeManager.getTextSecondaryColor());

            info.add(nameLabel);
            info.add(Box.createVerticalStrut(5));
            info.add(meta);

            panel.add(info, BorderLayout.CENTER);

            // Quantity badge (right)
            JLabel qty = new JLabel("Qty " + quantity, SwingConstants.CENTER);
            qty.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            qty.setOpaque(true);

            Color badgeBg = isSelected ? ThemeManager.getAccentColor() : ThemeManager.getInputBackgroundColor();
            Color badgeFg = isSelected ? ThemeManager.getTextOnPrimaryColor() : ThemeManager.getTextPrimaryColor();

            qty.setBackground(badgeBg);
            qty.setForeground(badgeFg);
            qty.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isSelected ? ThemeManager.getAccentColor() : ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(6, 14, 6, 14)
            ));

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(qty);
            panel.add(right, BorderLayout.EAST);

            return panel;
        }

        private static JPanel getJPanel(int index, boolean isSelected) {
            JPanel panel = new JPanel(new BorderLayout(12, 0));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorderColor().brighter()),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
            ));
            panel.setOpaque(true);

            Color bg = isSelected
                    ? ThemeManager.getSelectionBackgroundColor()
                    : (index % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
            panel.setBackground(bg);
            return panel;
        }

        private static String safeStatic(Supplier<String> s) {
            try {
                String v = s.get();
                return (v == null || v.isBlank()) ? "—" : v;
            } catch (Exception e) {
                return "—";
            }
        }

        private static int safeIntStatic(IntSupplierEx s) {
            try { return s.getAsInt(); } catch (Exception e) { return 0; }
        }
    }

    // Rounded panel for modern UI
    @SuppressWarnings("DuplicatedCode")
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