package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.ImageIcon;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageDialog {

    private static final int DEFAULT_DURATION_MS = 0;
    private static final String DEFAULT_TITLE = "Hinweis";
    private static final Dimension MIN_DIALOG_SIZE = new Dimension(420, 160);
    private static final int LOGO_SIZE = 30;
    private static final int MESSAGE_WIDTH_PX = 380;
    private static final int CORNER_RADIUS = 14;
    private static final int HEADER_HEIGHT = 64;
    private static final Dimension ACTION_BUTTON_MIN_SIZE = new Dimension(108, 36);

    private String message;
    private String title;
    private int duration;
    private int optionType = JOptionPane.DEFAULT_OPTION;
    private int messageType = JOptionPane.INFORMATION_MESSAGE;
    private Icon icon;
    private Object[] options;
    private Object initialValue;
    private String initialInputValue = "";

    private JDialog jDialog;
    private Timer autoCloseTimer;

    public MessageDialog() {
        this("", DEFAULT_TITLE, DEFAULT_DURATION_MS);
    }

    public MessageDialog(String message, String title) {
        this(message, title, DEFAULT_DURATION_MS);
    }

    public MessageDialog(String message, String title, int duration) {
        this.message = normalizeMessage(message);
        this.title = normalizeTitle(title);
        this.duration = normalizeDuration(duration);
    }

    public MessageDialog(String message, String title, int optionType, int messageType) {
        this(message, title, DEFAULT_DURATION_MS);
        this.optionType = optionType;
        this.messageType = messageType;
    }

    public MessageDialog(String message, String title, int optionType, int messageType, Icon icon,
            Object[] options, Object initialValue) {
        this(message, title, DEFAULT_DURATION_MS);
        this.optionType = optionType;
        this.messageType = messageType;
        this.icon = icon;
        this.options = options;
        this.initialValue = initialValue;
    }

    public MessageDialog setMessage(String message) {
        this.message = normalizeMessage(message);
        return this;
    }

    public MessageDialog setTitle(String title) {
        this.title = normalizeTitle(title);
        return this;
    }

    public MessageDialog setDuration(int duration) {
        this.duration = normalizeDuration(duration);
        return this;
    }

    public MessageDialog setOptionType(int optionType) {
        this.optionType = optionType;
        return this;
    }

    public MessageDialog setMessageType(int messageType) {
        this.messageType = messageType;
        return this;
    }

    public MessageDialog setIcon(Icon icon) {
        this.icon = icon;
        return this;
    }

    public MessageDialog setOptions(Object[] options) {
        this.options = options;
        return this;
    }

    public MessageDialog setInitialValue(Object initialValue) {
        this.initialValue = initialValue;
        return this;
    }

    public MessageDialog setInitialInputValue(String initialInputValue) {
        this.initialInputValue = initialInputValue == null ? "" : initialInputValue;
        return this;
    }

    // JOptionPane-compatible APIs ---------------------------------------------------------------

    public static int showConfirmDialog(Component parentComponent, Object message) {
        return showConfirmDialog(parentComponent, message, "Bestätigung", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE, null);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
        return showConfirmDialog(parentComponent, message, title, optionType, JOptionPane.QUESTION_MESSAGE, null);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType) {
        return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
    }

    public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType, Icon icon) {
        String normalizedTitle = title == null || title.isBlank() ? "Bestätigung" : title;
        String normalizedMessage = message == null ? "" : message.toString();
        return showStyledOptionDialog(parentComponent, normalizedMessage, normalizedTitle, optionType, messageType,
                icon, null, null);
    }

    public static int showOptionDialog(Component parentComponent, Object message, String title, int optionType,
            int messageType, Icon icon, Object[] options, Object initialValue) {
        String normalizedTitle = title == null || title.isBlank() ? "Auswahl" : title;
        String normalizedMessage = message == null ? "" : message.toString();
        return showStyledOptionDialog(parentComponent, normalizedMessage, normalizedTitle, optionType, messageType,
                icon, options, initialValue);
    }

    public static String showInputDialog(Component parentComponent, Object message) {
        return showInputDialog(parentComponent, message, "Eingabe", JOptionPane.QUESTION_MESSAGE, null, "");
    }

    public static String showInputDialog(Component parentComponent, Object message, String title, int messageType) {
        return showInputDialog(parentComponent, message, title, messageType, null, "");
    }

    public static String showInputDialog(Component parentComponent, Object message, String title, int messageType,
            Icon icon, String initialSelectionValue) {
        String normalizedTitle = title == null || title.isBlank() ? "Eingabe" : title;
        String normalizedMessage = message == null ? "" : message.toString();

        OptionDialogResult result = showStyledOptionDialogInternal(parentComponent, normalizedMessage,
                normalizedTitle, JOptionPane.OK_CANCEL_OPTION, messageType, icon, null, null, true,
                initialSelectionValue);

        if (result.optionResult == JOptionPane.OK_OPTION || result.optionResult == JOptionPane.YES_OPTION) {
            return result.inputValue;
        }
        return null;
    }

    public void display() {
        Runnable showTask = () -> {
            if (jDialog != null && jDialog.isDisplayable()) {
                jDialog.toFront();
                return;
            }

            ThemeManager.applyUIDefaults();
            Window owner = findActiveWindow();
            jDialog = new JDialog(owner, this.title, Dialog.ModalityType.MODELESS);
            jDialog.setLayout(new BorderLayout());
            jDialog.setResizable(false);
            jDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            jDialog.setUndecorated(true);
            jDialog.setBackground(new Color(0, 0, 0, 0));

            JPanel root = createRoundedRoot();

            JPanel header = createHeaderPanel(this.title);
            makeDraggable(jDialog, header);
            root.add(createTopSection(header), BorderLayout.NORTH);

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));

            JPanel body = new JPanel(new BorderLayout(10, 0));
            body.setOpaque(false);

            Icon effectiveIcon = icon != null ? icon : getDefaultMessageIcon(messageType);
            if (effectiveIcon != null) {
                JLabel iconLabel = new JLabel(effectiveIcon);
                iconLabel.setOpaque(false);
                iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                iconLabel.setVerticalAlignment(JLabel.TOP);
                body.add(iconLabel, BorderLayout.WEST);
            }

            JLabel messageLabel = new JLabel(toHtmlMultiline(this.message));
            messageLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
            messageLabel.setForeground(ThemeManager.getTextPrimaryColor());
            messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JScrollPane messageScroll = new JScrollPane(messageLabel);
            messageScroll.setBorder(BorderFactory.createEmptyBorder());
            messageScroll.setOpaque(false);
            messageScroll.getViewport().setOpaque(false);
            messageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            messageScroll.getVerticalScrollBar().setUnitIncrement(14);
            body.add(messageScroll, BorderLayout.CENTER);

            content.add(body);

            root.add(content, BorderLayout.CENTER);

            JPanel footer = createFooterPanel();

            JButton closeBtn = new JButton("Schließen");
            styleCloseButton(closeBtn);
            closeBtn.addActionListener(e -> close());

            footer.add(closeBtn);
            root.add(footer, BorderLayout.SOUTH);

            jDialog.setContentPane(root);
            jDialog.getRootPane().setDefaultButton(closeBtn);
            jDialog.getRootPane().registerKeyboardAction(
                    e -> close(),
                    javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
            );

            jDialog.pack();
            jDialog.setMinimumSize(MIN_DIALOG_SIZE);
            applyRoundedShape(jDialog);
            jDialog.setLocationRelativeTo(owner);
            jDialog.setVisible(true);

            if (duration > 0) {
                autoCloseTimer = new Timer(duration, e -> close());
                autoCloseTimer.setRepeats(false);
                autoCloseTimer.start();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            showTask.run();
        } else {
            SwingUtilities.invokeLater(showTask);
        }
    }

    public int displayWithOptions() {
        return showStyledOptionDialog(null, this.message, this.title, this.optionType, this.messageType, this.icon,
                this.options, this.initialValue);
    }

    public String displayWithStringInput() {
        OptionDialogResult result = showStyledOptionDialogInternal(null, this.message, this.title, this.optionType,
                this.messageType, this.icon, this.options, this.initialValue, true, this.initialInputValue);
        if (result.optionResult == JOptionPane.OK_OPTION || result.optionResult == JOptionPane.YES_OPTION) {
            return result.inputValue;
        }
        return null;
    }

    public void close() {
        Runnable closeTask = () -> {
            if (autoCloseTimer != null) {
                autoCloseTimer.stop();
                autoCloseTimer = null;
            }
            if (jDialog != null) {
                jDialog.dispose();
                jDialog = null;
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            closeTask.run();
        } else {
            SwingUtilities.invokeLater(closeTask);
        }
    }

    private static Window findActiveWindow() {
        Window active = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (active != null) {
            return active;
        }
        return null;
    }

    private static String normalizeMessage(String message) {
        return message == null ? "" : message;
    }

    private static String normalizeTitle(String title) {
        return title == null || title.isBlank() ? DEFAULT_TITLE : title;
    }

    private static int normalizeDuration(int duration) {
        return Math.max(0, duration);
    }

    private static String toHtmlMultiline(String text) {
        String value = text == null ? "" : text.trim();
        if (value.regionMatches(true, 0, "<html>", 0, 6)) {
            return value;
        }

        String safe = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br/>");
        return "<html><div style='width: " + MESSAGE_WIDTH_PX + "px; text-align:left; line-height:1.35;'>"
                + safe + "</div></html>";
    }

    private static JPanel createRoundedRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getBackgroundColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS * 2, CORNER_RADIUS * 2);
                g2.setColor(ThemeManager.getBorderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CORNER_RADIUS * 2, CORNER_RADIUS * 2);
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return root;
    }

    private static JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ThemeManager.getBorderColor());
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        return footer;
    }

    private static JPanel createDivider() {
        JPanel divider = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ThemeManager.getBorderColor());
                g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
            }
        };
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(0, 1));
        return divider;
    }

    private static JPanel createTopSection(JPanel header) {
        JPanel topSection = new JPanel(new BorderLayout(0, 0));
        topSection.setOpaque(false);
        topSection.add(header, BorderLayout.CENTER);
        topSection.add(createDivider(), BorderLayout.SOUTH);
        return topSection;
    }

    private static void applyRoundedShape(Window window) {
        try {
            window.setShape(new RoundRectangle2D.Double(
                    0, 0, window.getWidth(), window.getHeight(), CORNER_RADIUS * 2, CORNER_RADIUS * 2));
        } catch (UnsupportedOperationException ignored) {
            // Platform does not support shaped windows — fall back silently
        }
    }

    private static JPanel createHeaderPanel(String title) {
        JPanel header = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(
                        0, 0, ThemeManager.getHeaderBackgroundColor(),
                        getWidth(), 0, ThemeManager.getHeaderGradientColor()
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

        JLabel logoLabel = new JLabel(loadLogoIcon(LOGO_SIZE));
        logoLabel.setOpaque(false);
        header.add(logoLabel, BorderLayout.WEST);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        titleLabel.setVerticalAlignment(JLabel.CENTER);
        header.add(titleLabel, BorderLayout.CENTER);

        return header;
    }

    private static ImageIcon loadLogoIcon(int size) {
        URL logoUrl = MessageDialog.class.getResource("/logo-small.png");
        if (logoUrl == null) {
            return new ImageIcon(new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB));
        }
        ImageIcon icon = new ImageIcon(logoUrl);
        Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private static void makeDraggable(Window window, JPanel dragHandle) {
        final int[] dragOrigin = new int[2];
        dragHandle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                dragOrigin[0] = e.getXOnScreen() - window.getX();
                dragOrigin[1] = e.getYOnScreen() - window.getY();
            }
        });
        dragHandle.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                window.setLocation(e.getXOnScreen() - dragOrigin[0], e.getYOnScreen() - dragOrigin[1]);
                applyRoundedShape(window);
            }
        });
    }

    private static void styleCloseButton(JButton button) {
        styleActionButton(button, ThemeManager.getAccentColor(), ThemeManager.getTextOnPrimaryColor());
    }

    private static void styleSecondaryButton(JButton button) {
        styleActionButton(button, ThemeManager.getSurfaceColor(), ThemeManager.getTextPrimaryColor());
    }

    private static void styleActionButton(JButton button, Color base, Color fg) {
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);

        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setForeground(fg);
        button.setBackground(base);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMinimumSize(ACTION_BUTTON_MIN_SIZE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1, true),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hover : base);
            }
        });
    }

    private static int showStyledOptionDialog(Component parentComponent, String message, String title, int optionType,
            int messageType, Icon icon, Object[] options, Object initialValue) {
        return showStyledOptionDialogInternal(parentComponent, message, title, optionType, messageType, icon,
                options, initialValue, false, null).optionResult;
    }

    private static OptionDialogResult showStyledOptionDialogInternal(Component parentComponent, String message,
            String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue,
            boolean includeStringInput, String initialInputValue) {
        return invokeOnEdtAndWait(() -> {
            ThemeManager.applyUIDefaults();

            Window owner = parentComponent == null ? findActiveWindow() : SwingUtilities.getWindowAncestor(parentComponent);
            JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setLayout(new BorderLayout());
            dialog.setResizable(false);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setUndecorated(true);
            dialog.setBackground(new Color(0, 0, 0, 0));

            final int[] result = { JOptionPane.CLOSED_OPTION };
            final String[] inputResult = { initialInputValue == null ? "" : initialInputValue };

            JPanel root = createRoundedRoot();

            JPanel header = createHeaderPanel(title);
            makeDraggable(dialog, header);
            root.add(createTopSection(header), BorderLayout.NORTH);

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.setBorder(BorderFactory.createEmptyBorder(14, 18, 12, 18));

            JPanel body = new JPanel(new BorderLayout(10, 0));
            body.setOpaque(false);

            Icon effectiveIcon = icon != null ? icon : getDefaultMessageIcon(messageType);
            if (effectiveIcon != null) {
                JLabel iconLabel = new JLabel(effectiveIcon);
                iconLabel.setOpaque(false);
                iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                iconLabel.setVerticalAlignment(JLabel.TOP);
                body.add(iconLabel, BorderLayout.WEST);
            }

            JLabel messageLabel = new JLabel(toHtmlMultiline(message));
            messageLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
            messageLabel.setForeground(ThemeManager.getTextPrimaryColor());
            messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JScrollPane messageScroll = new JScrollPane(messageLabel);
            messageScroll.setBorder(BorderFactory.createEmptyBorder());
            messageScroll.setOpaque(false);
            messageScroll.getViewport().setOpaque(false);
            messageScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            messageScroll.getVerticalScrollBar().setUnitIncrement(14);
            body.add(messageScroll, BorderLayout.CENTER);
            content.add(body);

            final JTextField inputField;
            if (includeStringInput) {
                inputField = new JTextField(inputResult[0], 28);
                inputField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
                inputField.setForeground(ThemeManager.getTextPrimaryColor());
                inputField.setBackground(ThemeManager.getSurfaceColor());
                inputField.setCaretColor(ThemeManager.getTextPrimaryColor());
                Color normalBorder = ThemeManager.getBorderColor();
                Color focusBorder = ThemeManager.getAccentColor();
                inputField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
                inputField.addFocusListener(new FocusAdapter() {
                    @Override public void focusGained(FocusEvent e) {
                        inputField.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(focusBorder, 2, true),
                                BorderFactory.createEmptyBorder(7, 9, 7, 9)));
                    }
                    @Override public void focusLost(FocusEvent e) {
                        inputField.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(normalBorder, 1, true),
                                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
                    }
                });
                inputField.setAlignmentX(Component.LEFT_ALIGNMENT);
                inputField.setMaximumSize(new Dimension(Integer.MAX_VALUE, inputField.getPreferredSize().height + 4));
                content.add(javax.swing.Box.createVerticalStrut(10));
                content.add(inputField);
            } else {
                inputField = null;
            }

            root.add(content, BorderLayout.CENTER);

            JPanel footer = createFooterPanel();

            Map<Object, Integer> optionToReturn = createOptionMap(optionType, options);
            Object defaultOption = determineDefaultOption(optionType, options, initialValue);
            JButton defaultButton = null;

            for (Map.Entry<Object, Integer> entry : optionToReturn.entrySet()) {
                String text = String.valueOf(entry.getKey());
                JButton button = new JButton(text);
                if (isAffirmativeResult(entry.getValue(), text)) {
                    styleCloseButton(button);
                } else {
                    styleSecondaryButton(button);
                }

                int value = entry.getValue();
                button.addActionListener(e -> {
                    result[0] = value;
                    if (inputField != null) {
                        inputResult[0] = inputField.getText();
                    }
                    dialog.dispose();
                });

                if (defaultButton == null && String.valueOf(defaultOption).equals(text)) {
                    defaultButton = button;
                }
                footer.add(button);
            }

            root.add(footer, BorderLayout.SOUTH);
            dialog.setContentPane(root);
            if (defaultButton != null) {
                dialog.getRootPane().setDefaultButton(defaultButton);
            }
            if (inputField != null) {
                SwingUtilities.invokeLater(inputField::requestFocusInWindow);
            }
            dialog.getRootPane().registerKeyboardAction(
                    e -> {
                        result[0] = JOptionPane.CLOSED_OPTION;
                        if (inputField != null) {
                            inputResult[0] = inputField.getText();
                        }
                        dialog.dispose();
                    },
                    javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
            );

            dialog.pack();
            dialog.setMinimumSize(MIN_DIALOG_SIZE);
            applyRoundedShape(dialog);
            dialog.setLocationRelativeTo(owner);
            dialog.setVisible(true);

            return new OptionDialogResult(result[0], inputResult[0]);
        }, new OptionDialogResult(JOptionPane.CLOSED_OPTION, null));
    }

    private static Map<Object, Integer> createOptionMap(int optionType, Object[] options) {
        LinkedHashMap<Object, Integer> map = new LinkedHashMap<>();
        if (options != null && options.length > 0) {
            for (int i = 0; i < options.length; i++) {
                map.put(options[i], i);
            }
            return map;
        }

        switch (optionType) {
            case JOptionPane.YES_NO_OPTION -> {
                map.put("Ja", JOptionPane.YES_OPTION);
                map.put("Nein", JOptionPane.NO_OPTION);
            }
            case JOptionPane.YES_NO_CANCEL_OPTION -> {
                map.put("Ja", JOptionPane.YES_OPTION);
                map.put("Nein", JOptionPane.NO_OPTION);
                map.put("Abbrechen", JOptionPane.CANCEL_OPTION);
            }
            case JOptionPane.OK_CANCEL_OPTION -> {
                map.put("OK", JOptionPane.OK_OPTION);
                map.put("Abbrechen", JOptionPane.CANCEL_OPTION);
            }
            default -> map.put("OK", JOptionPane.OK_OPTION);
        }
        return map;
    }

    private static Object determineDefaultOption(int optionType, Object[] options, Object initialValue) {
        if (initialValue != null) {
            return initialValue;
        }
        if (options != null && options.length > 0) {
            return options[0];
        }
        return switch (optionType) {
            case JOptionPane.YES_NO_OPTION, JOptionPane.YES_NO_CANCEL_OPTION -> "Ja";
            case JOptionPane.OK_CANCEL_OPTION -> "OK";
            default -> "OK";
        };
    }

    private static boolean isAffirmativeResult(int returnValue, String text) {
        if (returnValue == JOptionPane.YES_OPTION || returnValue == JOptionPane.OK_OPTION) {
            return true;
        }
        String normalized = text == null ? "" : text.trim().toLowerCase();
        return "yes".equals(normalized) || "ja".equals(normalized) || "ok".equals(normalized);
    }

    private static Icon getDefaultMessageIcon(int messageType) {
        return switch (messageType) {
            case JOptionPane.ERROR_MESSAGE -> UIManager.getIcon("OptionPane.errorIcon");
            case JOptionPane.INFORMATION_MESSAGE -> UIManager.getIcon("OptionPane.informationIcon");
            case JOptionPane.WARNING_MESSAGE -> UIManager.getIcon("OptionPane.warningIcon");
            case JOptionPane.QUESTION_MESSAGE -> UIManager.getIcon("OptionPane.questionIcon");
            default -> null;
        };
    }

    private static final class OptionDialogResult {
        private final int optionResult;
        private final String inputValue;

        private OptionDialogResult(int optionResult, String inputValue) {
            this.optionResult = optionResult;
            this.inputValue = inputValue;
        }
    }

    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    private static <T> T invokeOnEdtAndWait(SupplierWithException<T> supplier, T fallbackValue) {
        if (SwingUtilities.isEventDispatchThread()) {
            try {
                return supplier.get();
            } catch (Exception e) {
                return fallbackValue;
            }
        }

        final Object[] result = new Object[] { fallbackValue };
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = supplier.get();
                } catch (Exception ignored) {
                    result[0] = fallbackValue;
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (InvocationTargetException ignored) {
        }
        @SuppressWarnings("unchecked")
        T typedResult = (T) result[0];
        return typedResult;
    }
}