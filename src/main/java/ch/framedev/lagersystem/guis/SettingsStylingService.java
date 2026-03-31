package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;

final class SettingsStylingService {

    private SettingsStylingService() {
    }

    static void applySettingsVisualPolish(Container root,
                                          Color accentColor,
                                          Color softGlassHighlight,
                                          Color softGlassBorder,
                                          Color softGlassSurface) {
        if (root == null) {
            return;
        }
        styleContainerRecursively(root, accentColor, softGlassHighlight, softGlassBorder, softGlassSurface);
    }

    private static void styleContainerRecursively(Container container,
                                                  Color accentColor,
                                                  Color softGlassHighlight,
                                                  Color softGlassBorder,
                                                  Color softGlassSurface) {
        for (Component component : container.getComponents()) {
            if (component instanceof JComponent jc) {
                if (jc.getClientProperty("searchText") != null) {
                    styleSettingsSectionCard(jc, accentColor, softGlassHighlight, softGlassBorder, softGlassSurface);
                }
                if (jc instanceof JScrollPane scrollPane) {
                    styleScrollPane(scrollPane);
                }
                if (jc instanceof JSeparator separator) {
                    separator.setForeground(ThemeManager.getBorderColor());
                }
                if (jc instanceof JTextArea area && !area.isEditable()) {
                    area.setLineWrap(true);
                    area.setWrapStyleWord(true);
                    area.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                }
            }
            if (component instanceof Container child) {
                styleContainerRecursively(child, accentColor, softGlassHighlight, softGlassBorder, softGlassSurface);
            }
        }
    }

    static void styleSettingsSectionCard(JComponent sectionCard,
                                         Color accentColor,
                                         Color softGlassHighlight,
                                         Color softGlassBorder,
                                         Color softGlassSurface) {
        if (sectionCard == null) {
            return;
        }
        Color accent = accentColor == null ? ThemeManager.getAccentColor() : accentColor;
        Color highlight = softGlassHighlight == null ? ThemeManager.getBorderColor() : softGlassHighlight;
        Color border = softGlassBorder == null ? ThemeManager.getBorderColor() : softGlassBorder;
        Color surface = softGlassSurface == null ? ThemeManager.getCardBackgroundColor() : softGlassSurface;

        sectionCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(3, 0, 0, 0, accent),
                        BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(highlight, 1, true),
                                BorderFactory.createLineBorder(border, 1, true))),
                BorderFactory.createEmptyBorder(22, 26, 20, 26)));
        sectionCard.setBackground(surface);
    }

    static void styleScrollPane(JScrollPane scrollPane) {
        if (scrollPane == null) {
            return;
        }
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        scrollPane.getViewport().setBackground(ThemeManager.getBackgroundColor());
        if (scrollPane.getVerticalScrollBar() != null) {
            scrollPane.getVerticalScrollBar().setUnitIncrement(14);
            scrollPane.getVerticalScrollBar().setUI(createModernScrollBarUI());
        }
        if (scrollPane.getHorizontalScrollBar() != null) {
            scrollPane.getHorizontalScrollBar().setUI(createModernScrollBarUI());
        }
    }

    static void styleSecondaryActionButton(JButton button) {
        if (button == null) {
            return;
        }
        button.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        button.setForeground(ThemeManager.getTextPrimaryColor());
        button.setBackground(ThemeManager.getSurfaceColor());
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(adjustColor(ThemeManager.getSurfaceColor(), -0.08f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ThemeManager.getSurfaceColor());
            }
        });
    }

    static void styleInputField(JTextField field) {
        if (field == null) {
            return;
        }
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getPrimaryColor(), 2, true),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)));
            }
        });
    }

    static void styleCheckbox(JCheckBox checkbox) {
        if (checkbox == null) {
            throw new IllegalArgumentException("checkbox must not be null");
        }
        checkbox.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        checkbox.setForeground(ThemeManager.getTextPrimaryColor());
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        checkbox.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));

        checkbox.addMouseListener(new MouseAdapter() {
            private final Color originalForeground = checkbox.getForeground();
            private final Color hoverForeground = adjustColor(ThemeManager.getPrimaryColor(), 1f);

            @Override
            public void mouseEntered(MouseEvent e) {
                checkbox.setForeground(hoverForeground);
                checkbox.setBorder(BorderFactory.createEmptyBorder(8, 6, 8, 0));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkbox.setForeground(originalForeground);
                checkbox.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));
            }
        });
    }

    static void styleSpinner(JSpinner spinner) {
        if (spinner == null) {
            throw new IllegalArgumentException("spinner must not be null");
        }
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            textField.setBackground(ThemeManager.getInputBackgroundColor());
            textField.setForeground(ThemeManager.getTextPrimaryColor());
            textField.setCaretColor(ThemeManager.getTextPrimaryColor());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getPrimaryColor(), 2, true),
                            BorderFactory.createEmptyBorder(9, 13, 9, 13)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
                }
            });
        }

        spinner.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true));
        spinner.setBackground(ThemeManager.getInputBackgroundColor());
    }

    static void styleComboBox(JComboBox<?> combo) {
        if (combo == null) {
            throw new IllegalArgumentException("combo must not be null");
        }
        JFrameUtils.styleComboBox(combo);
    }

    private static BasicScrollBarUI createModernScrollBarUI() {
        Color track = ThemeManager.getInputBackgroundColor();
        Color thumb = adjustColor(ThemeManager.getBorderColor(), -0.08f);
        Color thumbHover = adjustColor(ThemeManager.getAccentColor(), -0.12f);

        return new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.trackColor = track;
                this.thumbColor = thumb;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (!scrollbar.isEnabled() || thumbBounds.width <= 0 || thumbBounds.height <= 0) {
                    return;
                }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? thumbHover : thumb);
                int arc = 10;
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, arc, arc);
                g2.dispose();
            }
        };
    }

    private static Color adjustColor(Color color, float factor) {
        if (color == null) {
            return null;
        }
        int r = clampColor(Math.round(color.getRed() * (1.0f + factor)));
        int g = clampColor(Math.round(color.getGreen() * (1.0f + factor)));
        int b = clampColor(Math.round(color.getBlue() * (1.0f + factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
