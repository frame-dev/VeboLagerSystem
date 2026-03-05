package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Renders the article statistics dialog based on the current table model.
 */
public final class ArticleStatsDialog {

    private ArticleStatsDialog() {
    }

    // Table column indices
    private static final int COL_CATEGORY = 2;
    private static final int COL_STOCK = 4;
    private static final int COL_MIN_STOCK = 5;
    private static final int COL_SELL_PRICE = 6;
    private static final int COL_BUY_PRICE = 7;
    private static final int COL_VENDOR = 8;
    private static final int COL_NAME = 1;

    // Dialog dimensions
    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_HEIGHT = 750;
    private static final int DIALOG_MIN_WIDTH = 760;
    private static final int DIALOG_MIN_HEIGHT = 560;

    // Color scheme
    private static final Color COLOR_PRIMARY = new Color(52, 152, 219);
    private static final Color COLOR_SECONDARY = new Color(155, 89, 182);
    private static final Color COLOR_SUCCESS = new Color(46, 204, 113);
    private static final Color COLOR_WARNING = new Color(241, 196, 15);
    private static final Color COLOR_ERROR = new Color(231, 76, 60);
    private static final Color COLOR_DANGER = new Color(192, 57, 43);
    private static final Color GRADIENT_DARK_START = new Color(30, 58, 95);
    private static final Color GRADIENT_DARK_END = new Color(44, 62, 80);
    private static final Color GRADIENT_LIGHT_START = new Color(41, 128, 185);
    private static final Color GRADIENT_LIGHT_END = new Color(52, 152, 219);

    // Layout constants
    private static final int BORDER_RADIUS = 12;
    private static final int STAT_CARD_RADIUS = 14;
    private static final int HEADER_PADDING = 24;
    private static final int CONTENT_PADDING = 18;

    /**
     * Holds all calculated statistics for display.
     */
    private record Statistics(
            int totalArticles,
            int totalQuantity,
            double stockValue,
            double potentialRevenue,
            double profitMargin,
            int lowStockCount,
            int outOfStockCount,
            int categoriesCount,
            int vendorsCount,
            double avgArticleValue,
            String mostValuable,
            double stockHealth
    ) {
    }

    /**
     * Displays a modal dialog with various statistics about the articles in stock, calculated from the provided JTable. The dialog includes an overview of total articles, total quantity, categories, vendors, financial metrics like stock value and potential revenue, and stock health indicators. It also features a modern design with hover effects and a responsive layout.
     *
     * @param parent the parent component to center the dialog on
     * @param table  the JTable containing the article data to analyze for statistics
     */
    public static void show(Component parent, JTable table) {
        if (parent == null || table == null) {
            JOptionPane.showMessageDialog(null, "Fehler beim öffnen des Dialoges.");
            return;
        }
        ThemeManager.applyUIDefaults();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                UnicodeSymbols.PACKAGE + " Lager Details & Statistiken", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT));
        dialog.setLocationRelativeTo(parent);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(createDarkOutlineBorder());

        mainContainer.add(getStatsHeaderPanel(dialog), BorderLayout.NORTH);

        ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), BORDER_RADIUS);
        contentCard.setBorder(BorderFactory.createEmptyBorder(CONTENT_PADDING, 22, 22, 22));
        contentCard.setLayout(new GridBagLayout());

        Statistics stats = calculateAllStatistics(table);

        fillContentCard(contentCard, stats);

        JScrollPane scrollPane = new JScrollPane(contentCard);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        mainContainer.add(createBottomButtonPanel(dialog), BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(createCloseButton(dialog));
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.setVisible(true);
    }

    private static Border createDarkOutlineBorder() {
        Color shadow = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 45);
        return BorderFactory.createCompoundBorder(
                new LineBorder(shadow, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        );
    }

    private static Statistics calculateAllStatistics(JTable table) {
        int totalArticles = table.getRowCount();
        int totalQuantity = calculateTotalQuantity(table);
        double stockValue = calculateValueInStock(table);
        double potentialRevenue = calculatePotentialRevenue(table);
        double profitMargin = potentialRevenue - stockValue;
        int lowStockCount = countLowStockArticles(table);
        int outOfStockCount = countOutOfStockArticles(table);
        int categoriesCount = countCategories(table);
        int vendorsCount = countVendors(table);
        double avgArticleValue = totalQuantity == 0 ? 0.0 : stockValue / totalQuantity;
        String mostValuable = getMostValuableArticle(table);
        double stockHealth = calculateStockHealthPercentage(table);

        return new Statistics(totalArticles, totalQuantity, stockValue, potentialRevenue, profitMargin,
                lowStockCount, outOfStockCount, categoriesCount, vendorsCount, avgArticleValue, mostValuable, stockHealth);
    }

    private static void fillContentCard(ArticleGUI.RoundedPanel contentCard, Statistics stats) {
        GridBagConstraints gbc = createDefaultGBC();

        addSectionHeader(contentCard, gbc, UnicodeSymbols.CLIPBOARD + " Bestandsübersicht", 0);

        gbc.gridwidth = 1;
        addStatRow(contentCard, gbc, 1,
                UnicodeSymbols.PACKAGE + " Gesamtanzahl Artikel", String.valueOf(stats.totalArticles()), COLOR_PRIMARY,
                UnicodeSymbols.CHART + " Gesamtmenge (Stück)", String.format("%,d", stats.totalQuantity()), COLOR_PRIMARY
        );

        addStatRow(contentCard, gbc, 2,
                UnicodeSymbols.FOLDER + " Kategorien", String.valueOf(stats.categoriesCount()), COLOR_SECONDARY,
                UnicodeSymbols.TRUCK + " Lieferanten", String.valueOf(stats.vendorsCount()), COLOR_SECONDARY
        );

        addSectionHeader(contentCard, gbc, UnicodeSymbols.MONEY + " Finanzübersicht", 3);

        addStatRow(contentCard, gbc, 4,
                UnicodeSymbols.MONEY + " Lagerwert (Einkauf)", String.format("%.2f CHF", stats.stockValue()), COLOR_SUCCESS,
                UnicodeSymbols.MIN_STOCK + " Potenzielle Einnahmen", String.format("%.2f CHF", stats.potentialRevenue()), COLOR_PRIMARY
        );

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        contentCard.add(createStatPanel(
                UnicodeSymbols.CHEVRON_UP + " Gewinnspanne",
                String.format("%.2f CHF", stats.profitMargin()),
                COLOR_WARNING
        ), gbc);

        gbc.gridwidth = 1;
        addStatRow(contentCard, gbc, 6,
                UnicodeSymbols.BULLET + " Ø Wert pro Artikel", String.format("%.2f CHF", stats.avgArticleValue()), COLOR_SECONDARY,
                UnicodeSymbols.STAR_FILLED + " Wertvollster Artikel",
                truncateString(stats.mostValuable(), 20),
                COLOR_WARNING
        );

        addSectionHeader(contentCard, gbc, UnicodeSymbols.HEALTH + " Lagerstatus", 7);

        Color lowStockColor = stats.lowStockCount() > 0 ? COLOR_ERROR : COLOR_SUCCESS;
        Color outOfStockColor = stats.outOfStockCount() > 0 ? COLOR_DANGER : COLOR_SUCCESS;

        addStatRow(contentCard, gbc, 8,
                UnicodeSymbols.WARNING + " Niedriger Bestand", String.valueOf(stats.lowStockCount()), lowStockColor,
                UnicodeSymbols.WARNING + " Nicht vorrätig", String.valueOf(stats.outOfStockCount()), outOfStockColor
        );

        Color healthColor = getHealthColor(stats.stockHealth());
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(8, 8, 8, 8);
        contentCard.add(createStatPanel(
                UnicodeSymbols.HEALTH + " Lagergesundheit",
                String.format("%.1f%%", stats.stockHealth()),
                healthColor
        ), gbc);
    }

    private static void addSectionHeader(ArticleGUI.RoundedPanel contentCard, GridBagConstraints gbc, String text, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 10, 8);
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(label, gbc);
    }

    private static void addStatRow(ArticleGUI.RoundedPanel contentCard, GridBagConstraints gbc, int row,
                                    String label1, String value1, Color color1,
                                    String label2, String value2, Color color2) {
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);
        
        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(label1, value1, color1), gbc);

        gbc.gridx = 1;
        contentCard.add(createStatPanel(label2, value2, color2), gbc);
    }

    private static GridBagConstraints createDefaultGBC() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        return gbc;
    }

    private static Color getHealthColor(double stockHealth) {
        if (stockHealth >= 80) return COLOR_SUCCESS;
        if (stockHealth >= 50) return COLOR_WARNING;
        return COLOR_ERROR;
    }

    private static String truncateString(String str, int maxLength) {
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }

    private static JPanel getStatsHeaderPanel(JDialog dialog) {
        JPanel headerPanel = getPanel();

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.CHART);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Lager Statistiken");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        headerPanel.add(createHeaderCloseButton(dialog), BorderLayout.EAST);
        return headerPanel;
    }

    private static JButton createHeaderCloseButton(JDialog dialog) {
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schliesst dieses Fenster");
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));
        
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 100));
                closeBtn.setContentAreaFilled(true);
                closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
                closeBtn.setContentAreaFilled(false);
                closeBtn.setBorder(null);
            }
        });
        closeBtn.addActionListener(e -> dialog.dispose());
        return closeBtn;
    }

    private static JButton createCloseButton(JDialog dialog) {
        Color btnColor = ThemeManager.getAccentColor();
        JButton btn = new JButton(UnicodeSymbols.CHECK + " Schließen");
        btn.setToolTipText("Schliesst dieses Fenster");
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(btnColor);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 120 : 60), 1, true),
                BorderFactory.createEmptyBorder(12, 26, 12, 26)
        ));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(ThemeManager.getButtonHoverColor(btnColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(btnColor);
            }
        });
        btn.addActionListener(e -> dialog.dispose());
        return btn;
    }

    private static JPanel createBottomButtonPanel(JDialog dialog) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        buttonPanel.add(createCloseButton(dialog));
        return buttonPanel;
    }

    private static JPanel getPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.isDarkMode() ? GRADIENT_DARK_START : GRADIENT_LIGHT_START;
                Color color2 = ThemeManager.isDarkMode() ? GRADIENT_DARK_END : GRADIENT_LIGHT_END;

                GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(HEADER_PADDING, 28, HEADER_PADDING, 28));
        return headerPanel;
    }

    private static JPanel createStatPanel(String label, String value, Color accentColor) {
        Color outline = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 70 : 35);
        Color outlineHover = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 120 : 65);

        Border baseBorder = new CompoundBorder(
                new LineBorder(outline, 1, true),
                new EmptyBorder(14, 14, 14, 16)
        );
        Border hoverBorder = new CompoundBorder(
                new LineBorder(outlineHover, 1, true),
                new EmptyBorder(12, 14, 16, 16)
        );

        ArticleGUI.RoundedPanel tile = new ArticleGUI.RoundedPanel(ThemeManager.getSurfaceColor(), STAT_CARD_RADIUS);
        tile.setLayout(new BorderLayout(12, 0));
        tile.setBorder(baseBorder);
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Accent stripe
        JPanel stripe = new JPanel();
        stripe.setOpaque(true);
        stripe.setBackground(accentColor);
        stripe.setPreferredSize(new Dimension(6, 1));
        stripe.setBorder(new EmptyBorder(0, 0, 0, 0));
        tile.add(stripe, BorderLayout.WEST);

        // Text stack
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        labelComp.setForeground(ThemeManager.getTextSecondaryColor());
        labelComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        valueComp.setForeground(ThemeManager.isDarkMode() ? accentColor.brighter() : accentColor.darker());
        valueComp.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(labelComp);
        text.add(Box.createVerticalStrut(6));
        text.add(valueComp);

        tile.add(text, BorderLayout.CENTER);

        // Hover effect for all child components
        MouseAdapter hover = createHoverAdapter(tile, baseBorder, hoverBorder);
        tile.addMouseListener(hover);
        stripe.addMouseListener(hover);
        labelComp.addMouseListener(hover);
        valueComp.addMouseListener(hover);
        text.addMouseListener(hover);

        return tile;
    }

    private static MouseAdapter createHoverAdapter(ArticleGUI.RoundedPanel tile, Border baseBorder, Border hoverBorder) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tile.setBorder(hoverBorder);
                tile.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), tile);
                if (!tile.contains(p)) {
                    tile.setBorder(baseBorder);
                    tile.repaint();
                }
            }
        };
    }

    private static double calculateValueInStock(JTable table) {
        double totalValue = 0.0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            double buyPrice = readDouble(model, i, COL_BUY_PRICE);
            totalValue += stock * buyPrice;
        }
        return totalValue;
    }

    private static double calculatePotentialRevenue(JTable table) {
        double totalRevenue = 0.0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            double sellPrice = readDouble(model, i, COL_SELL_PRICE);
            totalRevenue += stock * sellPrice;
        }
        return totalRevenue;
    }

    private static int countLowStockArticles(JTable table) {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            int minStock = readInt(model, i, COL_MIN_STOCK);
            if (stock <= minStock && minStock > 0) {
                count++;
            }
        }
        return count;
    }

    private static int countOutOfStockArticles(JTable table) {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            if (stock == 0) {
                count++;
            }
        }
        return count;
    }

    private static int calculateTotalQuantity(JTable table) {
        int total = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            total += stock;
        }
        return total;
    }

    private static int countCategories(JTable table) {
        Set<String> categories = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String category = readString(model, i, COL_CATEGORY);
            if (isValidCategory(category)) {
                categories.add(category);
            }
        }
        return categories.size();
    }

    private static int countVendors(JTable table) {
        Set<String> vendors = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String vendor = readString(model, i, COL_VENDOR);
            if (!vendor.trim().isEmpty()) {
                vendors.add(vendor);
            }
        }
        return vendors.size();
    }

    private static String getMostValuableArticle(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        double maxValue = 0.0;
        String articleName = "N/A";

        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            double buyPrice = readDouble(model, i, COL_BUY_PRICE);
            double value = stock * buyPrice;

            if (value > maxValue) {
                maxValue = value;
                articleName = readString(model, i, COL_NAME);
            }
        }

        return articleName;
    }

    private static double calculateStockHealthPercentage(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int articlesAboveMin = 0;
        int articlesWithMinSet = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            int stock = readInt(model, i, COL_STOCK);
            int minStock = readInt(model, i, COL_MIN_STOCK);

            if (minStock > 0) {
                articlesWithMinSet++;
                if (stock > minStock) {
                    articlesAboveMin++;
                }
            }
        }

        return articlesWithMinSet == 0 ? 100.0 : (articlesAboveMin * 100.0) / articlesWithMinSet;
    }

    private static boolean isValidCategory(String category) {
        return category != null && !category.trim().isEmpty() && !category.equals("Unbekannt");
    }

    private static int readInt(DefaultTableModel model, int row, int col) {
        Object v = model.getValueAt(row, col);
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try {
            String s = v.toString().trim().replace("'", "");
            if (s.isEmpty()) return 0;
            return (int) Math.round(Double.parseDouble(s.replace(",", ".")));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double readDouble(DefaultTableModel model, int row, int col) {
        Object v = model.getValueAt(row, col);
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            String s = v.toString().trim().replace("'", "");
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private static String readString(DefaultTableModel model, int row, int col) {
        Object v = model.getValueAt(row, col);
        return v == null ? "" : v.toString();
    }
}
