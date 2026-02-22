package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.guis.OrderGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class JFrameUtils {

    // small rounded panel for card styling
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

    public static void adjustColumnWidths(JTable table, JScrollPane tableScrollPane, int[] baseColumnWidths) {
        if (tableScrollPane == null || table.getColumnCount() == 0) return;

        int available = tableScrollPane.getViewport().getWidth();
        if (available <= 0) available = tableScrollPane.getWidth();
        if (available <= 0) return;

        int totalBase = 0;
        for (int w : baseColumnWidths) totalBase += w;

        TableColumnModel tcm = table.getColumnModel();
        int colCount = tcm.getColumnCount();

        int used = 0;
        int[] newW = new int[colCount];

        for (int i = 0; i < colCount; i++) {
            int base = i < baseColumnWidths.length ? baseColumnWidths[i] : 100;
            int w = Math.max(60, (int) Math.round((base / (double) totalBase) * available));
            newW[i] = w;
            used += w;
        }

        int diff = available - used;
        int idx = 0;
        while (diff != 0 && colCount > 0) {
            newW[idx % colCount] += (diff > 0 ? 1 : -1);
            diff += (diff > 0 ? -1 : 1);
            idx++;
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

        Color defaultBg = ThemeManager.getAccentColor();
        Color hoverBg = ThemeManager.getButtonHoverColor(defaultBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(defaultBg);

        button.setBackground(defaultBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(defaultBg.darker(), 1),
                BorderFactory.createEmptyBorder(9, 16, 9, 16)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2),
                        BorderFactory.createEmptyBorder(8, 15, 8, 15)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(defaultBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(defaultBg.darker(), 1),
                        BorderFactory.createEmptyBorder(9, 16, 9, 16)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : defaultBg);
            }
        });

        return button;
    }

    public static JButton createThemeButton(String text, Color baseBg) {
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

    public static void applyButtonPalette(JButton button, Color baseBg) {
        Color hoverBg = ThemeManager.getButtonHoverColor(baseBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(baseBg);

        button.setBackground(baseBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        // Remove previous palette listeners (avoid stacking)
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml instanceof MouseAdapter) {
                button.removeMouseListener(ml);
            }
        }

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (button.isEnabled()) button.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e)  { if (button.isEnabled()) button.setBackground(baseBg); }
            @Override public void mousePressed(MouseEvent e) { if (button.isEnabled()) button.setBackground(pressedBg); }
            @Override public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : baseBg);
            }
        });
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
}
