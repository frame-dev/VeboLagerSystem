package ch.framedev.lagersystem.utils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.InputEvent;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Shared helper methods for consistent keyboard shortcuts across Swing windows.
 */
public final class KeyboardShortcutUtils {

    private KeyboardShortcutUtils() {
    }

    public static int getMenuShortcutMask() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    public static KeyStroke menuKey(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, getMenuShortcutMask());
    }

    public static KeyStroke menuShiftKey(int keyCode) {
        return KeyStroke.getKeyStroke(keyCode, getMenuShortcutMask() | KeyEvent.SHIFT_DOWN_MASK);
    }

    public static void register(JRootPane rootPane, String actionId, KeyStroke keyStroke, Runnable action) {
        register(rootPane, actionId, keyStroke, action, () -> true);
    }

    public static void register(JRootPane rootPane, String actionId, KeyStroke keyStroke, Runnable action,
                                BooleanSupplier enabledSupplier) {
        Objects.requireNonNull(rootPane, "rootPane");
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(keyStroke, "keyStroke");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(enabledSupplier, "enabledSupplier");

        rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionId);
        rootPane.getActionMap().put(actionId, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (enabledSupplier.getAsBoolean()) {
                    action.run();
                }
            }
        });
    }

    public static void registerButton(JRootPane rootPane, String actionId, KeyStroke keyStroke, AbstractButton button) {
        register(rootPane, actionId, keyStroke, () -> clickButton(button));
    }

    public static void registerButton(JRootPane rootPane, String actionId, KeyStroke keyStroke, AbstractButton button,
                                      boolean ignoreWhenTextInputFocused) {
        register(rootPane, actionId, keyStroke, () -> clickButton(button),
                () -> !ignoreWhenTextInputFocused || !isTextInputFocused());
    }

    public static void registerFocus(JRootPane rootPane, String actionId, KeyStroke keyStroke, JComponent component) {
        register(rootPane, actionId, keyStroke, () -> {
            component.requestFocusInWindow();
            if (component instanceof JTextComponent textComponent) {
                textComponent.selectAll();
            }
        });
    }

    public static void registerClose(JFrame frame) {
        register(frame.getRootPane(), "shortcut.closeWindow", KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), frame::dispose);
    }

    public static void addTooltipHint(JComponent component, KeyStroke keyStroke) {
        if (component == null || keyStroke == null) {
            return;
        }
        component.setToolTipText(withShortcutHint(component.getToolTipText(), keyStroke));
    }

    public static String withShortcutHint(String baseText, KeyStroke keyStroke) {
        if (keyStroke == null) {
            return baseText;
        }
        String shortcutText = formatKeyStroke(keyStroke);
        if (shortcutText.isBlank()) {
            return baseText;
        }

        String base = baseText == null ? "" : baseText.trim();
        String hint = "Shortcut: " + shortcutText;
        if (base.isEmpty()) {
            return hint;
        }
        if (base.contains(hint)) {
            return base;
        }
        return base + " • " + hint;
    }

    public static String formatKeyStroke(KeyStroke keyStroke) {
        if (keyStroke == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int modifiers = keyStroke.getModifiers();
        if ((modifiers & getMenuShortcutMask()) != 0) {
            builder.append("Ctrl/Cmd+");
        }
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0) {
            builder.append("Shift+");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0) {
            builder.append("Alt+");
        }

        String keyText = KeyEvent.getKeyText(keyStroke.getKeyCode());
        if (keyText == null || keyText.isBlank()) {
            return builder.toString();
        }
        builder.append(keyText);
        return builder.toString();
    }

    public static boolean isTextInputFocused() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null) {
            return false;
        }
        if (focusOwner instanceof JTextComponent) {
            return true;
        }
        if (focusOwner instanceof JComboBox<?> comboBox && comboBox.isEditable()) {
            return true;
        }
        return SwingUtilities.getAncestorOfClass(JTextComponent.class, focusOwner) != null;
    }

    private static void clickButton(AbstractButton button) {
        if (button != null && button.isEnabled() && button.isShowing()) {
            button.doClick();
        }
    }
}
