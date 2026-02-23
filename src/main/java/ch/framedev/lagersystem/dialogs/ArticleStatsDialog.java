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
@SuppressWarnings("DuplicatedCode")
public final class ArticleStatsDialog {

    private ArticleStatsDialog() {
    }

    /**
     * Displays a modal dialog with various statistics about the articles in stock, calculated from the provided JTable. The dialog includes an overview of total articles, total quantity, categories, vendors, financial metrics like stock value and potential revenue, and stock health indicators. It also features a modern design with hover effects and a responsive layout.
     * @param parent the parent component to center the dialog on
     * @param table the JTable containing the article data to analyze for statistics
     */
    public static void show(Component parent, JTable table) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent),
                UnicodeSymbols.PACKAGE + " Lager Details & Statistiken", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setSize(900, 750);
        dialog.setLocationRelativeTo(parent);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        // Subtle dialog shadow / outline
        Color shadow = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 45);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(shadow, 1, true),
                new EmptyBorder(0, 0, 0, 0)
        ));

        JPanel headerPanel = getStatsHeaderPanel(dialog);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(18, 22, 22, 22));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

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

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel overviewLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Bestandsübersicht");
        overviewLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        overviewLabel.setForeground(ThemeManager.getTextPrimaryColor());
        overviewLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        contentCard.add(overviewLabel, gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.PACKAGE + " Gesamtanzahl Artikel",
                String.valueOf(totalArticles),
                new Color(52, 152, 219)
        ), gbc);

        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.CHART + " Gesamtmenge (Stück)",
                String.format("%,d", totalQuantity),
                new Color(52, 152, 219)
        ), gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.FOLDER + " Kategorien",
                String.valueOf(categoriesCount),
                new Color(155, 89, 182)
        ), gbc);

        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.TRUCK + " Lieferanten",
                String.valueOf(vendorsCount),
                new Color(155, 89, 182)
        ), gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 10, 8);
        JLabel financialLabel = new JLabel(UnicodeSymbols.MONEY + " Finanzübersicht");
        financialLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        financialLabel.setForeground(ThemeManager.getTextPrimaryColor());
        financialLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(financialLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.MONEY + " Lagerwert (Einkauf)",
                String.format("%.2f CHF", stockValue),
                new Color(46, 204, 113)
        ), gbc);

        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.MIN_STOCK + " Potenzielle Einnahmen",
                String.format("%.2f CHF", potentialRevenue),
                new Color(52, 152, 219)
        ), gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        contentCard.add(createStatPanel(
                UnicodeSymbols.CHEVRON_UP + " Gewinnspanne",
                String.format("%.2f CHF", profitMargin),
                new Color(241, 196, 15)
        ), gbc);

        gbc.gridwidth = 1;

        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.BULLET + " Ø Wert pro Artikel",
                String.format("%.2f CHF", avgArticleValue),
                new Color(155, 89, 182)
        ), gbc);

        gbc.gridx = 1;
        String displayValue = mostValuable.length() > 20 ? mostValuable.substring(0, 17) + "..." : mostValuable;
        contentCard.add(createStatPanel(
                UnicodeSymbols.STAR_FILLED + " Wertvollster Artikel",
                displayValue,
                new Color(241, 196, 15)
        ), gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 10, 8);
        JLabel healthLabel = new JLabel(UnicodeSymbols.HEALTH + " Lagerstatus");
        healthLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        healthLabel.setForeground(ThemeManager.getTextPrimaryColor());
        healthLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(healthLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);

        gbc.gridx = 0;
        gbc.gridy = row;
        Color lowStockColor = lowStockCount > 0 ? new Color(231, 76, 60) : new Color(46, 204, 113);
        contentCard.add(createStatPanel(
                UnicodeSymbols.WARNING + " Niedriger Bestand",
                String.valueOf(lowStockCount),
                lowStockColor
        ), gbc);

        gbc.gridx = 1;
        Color outOfStockColor = outOfStockCount > 0 ? new Color(192, 57, 43) : new Color(46, 204, 113);
        contentCard.add(createStatPanel(
                UnicodeSymbols.WARNING + " Nicht vorrätig",
                String.valueOf(outOfStockCount),
                outOfStockColor
        ), gbc);

        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        Color healthColor = stockHealth >= 80 ? new Color(46, 204, 113)
                : stockHealth >= 50 ? new Color(241, 196, 15)
                : new Color(231, 76, 60);
        contentCard.add(createStatPanel(
                UnicodeSymbols.HEALTH + " Lagergesundheit",
                String.format("%.1f%%", stockHealth),
                healthColor
        ), gbc);

        JScrollPane scrollPane = new JScrollPane(contentCard);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        Color okBtnColor = ThemeManager.getAccentColor();
        JButton okBtn = new JButton(UnicodeSymbols.CHECK + " Schließen");
        okBtn.setToolTipText("Schliesst dieses Fenster");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okBtnColor);
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 120 : 60), 1, true),
                BorderFactory.createEmptyBorder(12, 26, 12, 26)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                okBtn.setBackground(ThemeManager.getButtonHoverColor(okBtnColor));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                okBtn.setBackground(okBtnColor);
            }
        });
        okBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);
        dialog.setVisible(true);
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
        closeBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 100));
                closeBtn.setContentAreaFilled(true);
                closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
                closeBtn.setContentAreaFilled(false);
                closeBtn.setBorder(null);
            }
        });
        closeBtn.addActionListener(e -> dialog.dispose());
        headerPanel.add(closeBtn, BorderLayout.EAST);
        return headerPanel;
    }

    private static JPanel getPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.isDarkMode()
                        ? new Color(30, 58, 95)
                        : new Color(41, 128, 185);
                Color color2 = ThemeManager.isDarkMode()
                        ? new Color(44, 62, 80)
                        : new Color(52, 152, 219);

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }

    private static JPanel createStatPanel(String label, String value, Color accentColor) {
        // Base + hover borders (subtle shadow via alpha outline)
        Color outline = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 70 : 35);
        Color outlineHover = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 120 : 65);

        Border baseBorder = new CompoundBorder(
                new LineBorder(outline, 1, true),
                new EmptyBorder(14, 14, 14, 16)
        );
        Border hoverBorder = new CompoundBorder(
                new LineBorder(outlineHover, 1, true),
                // Top a touch smaller + bottom a touch larger -> optical "lift"
                new EmptyBorder(12, 14, 16, 16)
        );

        ArticleGUI.RoundedPanel tile = new ArticleGUI.RoundedPanel(ThemeManager.getSurfaceColor(), 14);
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

        // Hover lift effect must work over children too
        MouseAdapter hover = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                tile.setBorder(hoverBorder);
                tile.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Avoid flicker when moving between child components inside the tile
                Point p = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(p, tile);
                if (p.x >= 0 && p.y >= 0 && p.x < tile.getWidth() && p.y < tile.getHeight()) {
                    return;
                }
                tile.setBorder(baseBorder);
                tile.repaint();
            }
        };

        tile.addMouseListener(hover);
        stripe.addMouseListener(hover);
        labelComp.addMouseListener(hover);
        valueComp.addMouseListener(hover);
        text.addMouseListener(hover);

        return tile;
    }

    private static double calculateValueInStock(JTable table) {
        double totalValue = 0.0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            double einkaufspreis = (double) model.getValueAt(i, 7);
            totalValue += lagerbestand * einkaufspreis;
        }
        return totalValue;
    }

    private static double calculatePotentialRevenue(JTable table) {
        double totalRevenue = 0.0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            double verkaufspreis = (double) model.getValueAt(i, 6);
            totalRevenue += lagerbestand * verkaufspreis;
        }
        return totalRevenue;
    }

    private static int countLowStockArticles(JTable table) {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            int mindestbestand = (int) model.getValueAt(i, 5);
            if (lagerbestand <= mindestbestand && mindestbestand > 0) {
                count++;
            }
        }
        return count;
    }

    private static int countOutOfStockArticles(JTable table) {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            if (lagerbestand == 0) {
                count++;
            }
        }
        return count;
    }

    private static int calculateTotalQuantity(JTable table) {
        int total = 0;
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            total += lagerbestand;
        }
        return total;
    }

    private static int countCategories(JTable table) {
        Set<String> categories = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String category = (String) model.getValueAt(i, 2);
            if (category != null && !category.trim().isEmpty() && !category.equals("Unbekannt")) {
                categories.add(category);
            }
        }
        return categories.size();
    }

    private static int countVendors(JTable table) {
        Set<String> vendors = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String vendor = (String) model.getValueAt(i, 8);
            if (vendor != null && !vendor.trim().isEmpty()) {
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
            int lagerbestand = (int) model.getValueAt(i, 4);
            double einkaufspreis = (double) model.getValueAt(i, 7);
            double value = lagerbestand * einkaufspreis;

            if (value > maxValue) {
                maxValue = value;
                articleName = (String) model.getValueAt(i, 1);
            }
        }

        return articleName;
    }

    private static double calculateStockHealthPercentage(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int articlesAboveMin = 0;
        int articlesWithMinSet = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            int mindestbestand = (int) model.getValueAt(i, 5);

            if (mindestbestand > 0) {
                articlesWithMinSet++;
                if (lagerbestand > mindestbestand) {
                    articlesAboveMin++;
                }
            }
        }

        if (articlesWithMinSet == 0) {
            return 100.0;
        }
        return (articlesAboveMin * 100.0) / articlesWithMinSet;
    }
}
