package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

final class SettingsUiFactory {

    private SettingsUiFactory() {
    }

    static JScrollPane createScrollablePanel(JPanel panel) {
        if (panel == null) {
            throw new NullPointerException("Panel cannot be null");
        }
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(true);
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(ThemeManager.getBackgroundColor());
        return scrollPane;
    }

    static JPanel createWrapButtonPanel(int alignment, int hgap, int vgap) {
        JPanel panel = new JPanel(new WrapLayout(alignment, hgap, vgap));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return panel;
    }

    static JPanel createLabeledSpinnerPanel(String labelText, JSpinner spinner, String unitText, int columns) {
        if (labelText == null) {
            throw new IllegalArgumentException("labelText must not be null");
        }
        if (spinner == null) {
            throw new IllegalArgumentException("spinner must not be null");
        }
        if (unitText == null) {
            throw new IllegalArgumentException("unitText must not be null");
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JLabel label = new JLabel(labelText);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        spinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
        SettingsStylingService.styleSpinner(spinner);

        JLabel unitLabel = new JLabel(unitText);
        unitLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        unitLabel.setForeground(ThemeManager.getTextSecondaryColor());

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(spinner);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(unitLabel);
        return panel;
    }

    static JPanel createLabeledComboBoxPanel(String labelText, JComboBox<?> comboBox) {
        if (labelText == null) {
            throw new IllegalArgumentException("labelText must not be null");
        }
        if (comboBox == null) {
            throw new IllegalArgumentException("comboBox must not be null");
        }

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));

        JLabel label = new JLabel(labelText);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        comboBox.setPreferredSize(new Dimension(230, 36));

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(comboBox);
        return panel;
    }

    static JLabel createInfoLabel(String htmlContent) {
        String content = htmlContent == null ? "" : htmlContent.trim();
        if (!content.toLowerCase(Locale.ROOT).startsWith("<html>")) {
            content = "<html><div style='line-height:1.45;'>" + content + "</div></html>";
        }
        JLabel label = new JLabel(content);
        label.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    static JButton createStyledButton(String text, Color originalBg) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }

        Color baseBg = originalBg != null ? originalBg : ThemeManager.getButtonBackgroundColor();
        JButton button = new JButton(text);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setForeground(SettingsColorService.getReadableTextColor(baseBg));
        button.setBackground(baseBg);
        button.setIconTextGap(8);

        Border normalBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SettingsColorService.adjustColor(baseBg, -0.18f), 1, true),
                BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setBorder(normalBorder);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty("theme.button.custom", Boolean.TRUE);
        button.setMargin(new Insets(0, 0, 0, 0));

        Dimension preferredSize = button.getPreferredSize();
        button.setPreferredSize(new Dimension(Math.max(160, preferredSize.width), 46));
        button.setMinimumSize(new Dimension(120, 46));

        Color hoverBg = SettingsColorService.adjustColor(baseBg, -0.08f);
        Color pressedBg = SettingsColorService.adjustColor(baseBg, -0.16f);
        Border hoverBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SettingsColorService.adjustColor(hoverBg, -0.12f), 1, true),
                BorderFactory.createEmptyBorder(12, 20, 12, 20));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(hoverBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseBg);
                button.setBorder(normalBorder);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : baseBg);
            }
        });

        return button;
    }
}
