package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

final class DialogFieldStylingService {

    private DialogFieldStylingService() {
    }

    static JTextField createTextField(int columns, Color normalBorder, Color focusBorder, Insets padding) {
        JTextField field = new JTextField(columns);
        styleTextComponent(field, normalBorder, focusBorder, padding);
        return field;
    }

    static void styleTextComponent(JTextField field, Color normalBorder, Color focusBorder, Insets padding) {
        if (field == null) {
            return;
        }
        Insets safePadding = padding == null ? new Insets(10, 12, 10, 12) : padding;
        Color safeNormalBorder = normalBorder == null ? ThemeManager.getBorderColor() : normalBorder;
        Color safeFocusBorder = focusBorder == null ? ThemeManager.getAccentColor() : focusBorder;

        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());
        applyTextBorder(field, safeNormalBorder, 1, safePadding);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                applyTextBorder(field, safeFocusBorder, 2, shrinkPadding(safePadding, 1));
            }

            @Override
            public void focusLost(FocusEvent e) {
                applyTextBorder(field, safeNormalBorder, 1, safePadding);
            }
        });
    }

    private static Insets shrinkPadding(Insets padding, int amount) {
        return new Insets(
                Math.max(0, padding.top - amount),
                Math.max(0, padding.left - amount),
                Math.max(0, padding.bottom - amount),
                Math.max(0, padding.right - amount));
    }

    private static void applyTextBorder(JTextField field, Color color, int thickness, Insets padding) {
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color, thickness, true),
                BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
        ));
    }
}
