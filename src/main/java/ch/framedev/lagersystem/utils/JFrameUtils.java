package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility class for creating and styling Swing components with a consistent look and feel across the application.
 * Provides methods for creating styled buttons, panels, and form components, as well as a method for dynamically adjusting JTable column widths based on available space and base width proportions.
 * This class is designed to centralize all UI styling logic, making it easier to maintain a cohesive design and update styles in one place.
 * @author framedev
 */
@SuppressWarnings("DuplicatedCode")
public class JFrameUtils {

    /**
     * ClientProperty key used to store and replace our internal button palette MouseListener.
     * This prevents listener stacking without removing listeners added elsewhere.
     */
    private static final String CLIENT_PROP_PALETTE_LISTENER = "jframeutils.paletteListener";

    /**
     * Minimum column width used by {@link #adjustColumnWidths(JTable, JScrollPane, int[])}.
     */
    private static final int MIN_COLUMN_WIDTH = 60;

    /**
     * A custom JPanel that paints itself with rounded corners and a specified background color. This can be used to create visually appealing containers for other components, such as forms or sections within the application's windows.
     */
    @SuppressWarnings("DuplicatedCode")
    public static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        public RoundedPanel(Color bg, int radius) {
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

    /**
     * Dynamically adjusts the column widths of a JTable based on the available width of its enclosing JScrollPane and a set of base column widths.
     * @param table The JTable whose columns should be adjusted
     * @param tableScrollPane The JScrollPane that contains the JTable, used to determine available width
     * @param baseColumnWidths An array of integers representing the base widths for each column, used to calculate proportional widths. If fewer values are provided than columns, remaining columns will default to a base width of 100.
     */
    public static void adjustColumnWidths(JTable table, JScrollPane tableScrollPane, int[] baseColumnWidths) {
        if (table == null || tableScrollPane == null) return;
        if (table.getColumnCount() == 0) return;

        // Ensure preferred widths are respected.
        if (table.getAutoResizeMode() != JTable.AUTO_RESIZE_OFF) {
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        }

        int available = tableScrollPane.getViewport() != null ? tableScrollPane.getViewport().getWidth() : 0;
        if (available <= 0) available = tableScrollPane.getWidth();
        if (available <= 0) return;

        int[] bases = (baseColumnWidths == null) ? new int[0] : baseColumnWidths;

        TableColumnModel tcm = table.getColumnModel();
        int colCount = tcm.getColumnCount();

        // Compute total base width (falling back to 100 when not provided)
        int totalBase = 0;
        for (int i = 0; i < colCount; i++) {
            int base = i < bases.length ? bases[i] : 100;
            if (base < 1) base = 1;
            totalBase += base;
        }
        if (totalBase <= 0) return;

        int used = 0;
        int[] newW = new int[colCount];

        for (int i = 0; i < colCount; i++) {
            int base = i < bases.length ? bases[i] : 100;
            if (base < 1) base = 1;
            int w = Math.max(MIN_COLUMN_WIDTH, (int) Math.round((base / (double) totalBase) * available));
            newW[i] = w;
            used += w;
        }

        // Distribute rounding difference across columns so we match the available width.
        int diff = available - used;
        int idx = 0;
        while (diff != 0 && colCount > 0) {
            int col = idx % colCount;
            int next = newW[col] + (diff > 0 ? 1 : -1);
            // Keep columns from shrinking below the minimum width.
            if (next >= MIN_COLUMN_WIDTH) {
                newW[col] = next;
                diff += (diff > 0 ? -1 : 1);
            }
            idx++;
            // Safety to avoid pathological loops.
            if (idx > colCount * 1000) break;
        }

        for (int i = 0; i < colCount; i++) {
            tcm.getColumn(i).setPreferredWidth(newW[i]);
        }

        table.revalidate();
        table.repaint();
        tableScrollPane.revalidate();
        tableScrollPane.repaint();
    }

    public static void styleTextField(JTextField field) {
        if (field == null) return;
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
    }

    public static void styleComboBox(JComboBox<String> combo) {
        if (combo == null) return;
        Color bg = ThemeManager.getInputBackgroundColor();
        Color fg = ThemeManager.getTextPrimaryColor();
        Color border = ThemeManager.getInputBorderColor();
        Color selBg = ThemeManager.getSelectionBackgroundColor();
        Color selFg = ThemeManager.getSelectionForegroundColor();
        Color surface = ThemeManager.getSurfaceColor();

        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setOpaque(true);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // enforce popup list colors
                list.setBackground(bg);
                list.setForeground(fg);
                list.setSelectionBackground(selBg);
                list.setSelectionForeground(selFg);

                c.setOpaque(true);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    c.setBackground(selBg);
                    c.setForeground(selFg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(fg);
                }
                return c;
            }
        });

        // Theme arrow button + popup border (reliable across LAFs)
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton(UnicodeSymbols.ARROW_DOWN);
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { b.setBackground(surface); }
                    @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
                });
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof BasicComboPopup basic) {
                    basic.setBorder(BorderFactory.createLineBorder(border, 1));
                    basic.getList().setBackground(bg);
                    basic.getList().setForeground(fg);
                    basic.getList().setSelectionBackground(selBg);
                    basic.getList().setSelectionForeground(selFg);
                }
                return popup;
            }
        });

        if (combo.isEditable()) {
            Component editorComp = combo.getEditor().getEditorComponent();
            if (editorComp instanceof JTextField tf) {
                tf.setBackground(bg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBorder(null);
            }
        }
    }

    /**
     * A custom JPanel that paints itself with a horizontal gradient background and rounded corners. This can be used for visually distinct sections of the UI, such as headers or highlighted areas.
     */
    public static class GradientPanel extends JPanel {
        private final Color color1;
        private final Color color2;

        public GradientPanel(Color color1, Color color2) {
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

    public static JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

        // Apply default accent palette (hover/pressed) without stacking listeners.
        applyButtonPalette(button, ThemeManager.getAccentColor());
        return button;
    }

    public static JButton createThemeButton(String text, Color baseBg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

        applyButtonPalette(button, baseBg);
        return button;
    }

    /**
     * Applies a consistent color palette to a JButton, including hover and pressed states, based on a given base background color.
     * @param button The JButton to style
     * @param baseBg The base background color to use for the button (e.g., primary, accent, error)
     */
    public static void applyButtonPalette(JButton button, Color baseBg) {
        if (button == null || baseBg == null) return;

        Color hoverBg = ThemeManager.getButtonHoverColor(baseBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(baseBg);

        button.setBackground(baseBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        // Remove only the listener previously installed by this utility (avoid breaking external listeners).
        Object existing = button.getClientProperty(CLIENT_PROP_PALETTE_LISTENER);
        if (existing instanceof MouseListener ml) {
            button.removeMouseListener(ml);
        }

        MouseAdapter paletteListener = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (button.isEnabled()) button.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e)  { if (button.isEnabled()) button.setBackground(baseBg); }
            @Override public void mousePressed(MouseEvent e) { if (button.isEnabled()) button.setBackground(pressedBg); }
            @Override public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : baseBg);
            }
        };

        button.putClientProperty(CLIENT_PROP_PALETTE_LISTENER, paletteListener);
        button.addMouseListener(paletteListener);
    }

    public static JButton createPrimaryButton(String text) {
        JButton btn = createRoundedButton(text);
        applyButtonPalette(btn, ThemeManager.getAccentColor());
        return btn;
    }

    public static JButton createSecondaryButton(String text) {
        JButton btn = createRoundedButton(text);
        applyButtonPalette(btn, ThemeManager.getPrimaryColor());
        return btn;
    }

    /**
     * Creates a "danger" styled button, typically used for destructive actions like delete.
     * @param text The button text
     * @return A JButton with the danger color palette applied
     */
    public static JButton createDangerButton(String text) {
        JButton btn = createRoundedButton(text);
        applyButtonPalette(btn, ThemeManager.getErrorColor());
        return btn;
    }



    public static JButton createSecondaryButton(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setForeground(ThemeManager.getTextPrimaryColor());
        btn.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color fg = ThemeManager.getTextPrimaryColor();
        Color hover = ThemeManager.getButtonHoverColor(fg);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(hover); }
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(fg); }
        });
        return btn;
    }

    public static JButton createPrimaryButton(String text, Color color, ActionListener action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action);
        btn.setBackground(color);
        btn.setForeground(ThemeManager.getTextOnPrimaryColor());
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color hover = ThemeManager.getButtonHoverColor(color);
        btn.addChangeListener(e -> btn.setBackground(btn.getModel().isRollover() ? hover : color));
        return btn;
    }

    /**
     * Creates a header panel with a horizontal gradient background and rounded corners.
     * @return A styled JPanel suitable for use as a header in the application's windows
     */
    public static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, ThemeManager.getHeaderBackgroundColor(), getWidth(), 0, ThemeManager.getHeaderGradientColor());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(0,0,0,30));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));
        return headerPanel;
    }

    /**
     * This method retrieves the currently selected articles from the JTable, converting the selected rows into Article objects.
     *
     * @return A list of Article objects corresponding to the selected rows in the table. If no rows are selected, returns an empty list.
     */
    public static List<Article> getSelectedArticles(JTable articleTable) {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            return Collections.emptyList();
        }

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        List<Article> selectedArticles = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            Object articleNumber = model.getValueAt(modelRow, 0);
            Object name = model.getValueAt(modelRow, 1);
            Object details = model.getValueAt(modelRow, 3);
            Object stockQuantity = model.getValueAt(modelRow, 4);
            Object minStockLevel = model.getValueAt(modelRow, 5);
            Object sellPrice = model.getValueAt(modelRow, 6);
            if (sellPrice instanceof String str) {
                sellPrice = str.replace(" CHF", "").trim();
            }
            Object purchasePrice = model.getValueAt(modelRow, 7);
            if (purchasePrice instanceof String str) {
                purchasePrice = str.replace(" CHF", "").trim();
            }
            Object vendorName = model.getValueAt(modelRow, 8);

            Article article = new Article(
                    String.valueOf(articleNumber),
                    String.valueOf(name),
                    String.valueOf(details),
                    toInt(stockQuantity),
                    toInt(minStockLevel),
                    toDouble(sellPrice),
                    toDouble(purchasePrice),
                    String.valueOf(vendorName)
            );
            selectedArticles.add(article);
        }
        return selectedArticles;
    }

    public static void exportSelectedArticles(JFrame frame, JTable articleTable) {
        java.util.List<Article> selected = getSelectedArticles(articleTable);
        if (selected.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte wählen Sie mindestens einen Artikel aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        Object[] options = {"CSV", "PDF", "Abbrechen"};
        int choice = new MessageDialog()
                .setTitle("Export")
                .setMessage("Auswahl exportieren als:")
                .setOptions(options)
                .setIcon(Main.iconSmall)
                .setOptions(options)
                .displayWithOptions();
        if (choice == 0) {
            exportArticlesToCsv(selected);
        } else if (choice == 1) {
            exportArticlesToPdf(selected);
        }
    }

    private static void exportArticlesToCsv(java.util.List<Article> articles) {
        ArticleExporter.exportArticlesToCsv(articles);
    }

    private static void exportArticlesToPdf(List<Article> articles) {
        ArticleExporter.exportArticlesToPdf(articles);
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            str = str.replace(" CHF", "").trim();
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            str = str.replace(" CHF", "").trim();
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
