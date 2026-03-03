package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.OrderGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public class VendorDialog {

    private VendorDialog() {
    }

    public static Object[] showAddVendorDialog(JFrame frame) {
        if(frame == null) throw new IllegalArgumentException("Frame must not be null");
        return showVendorDialog(
                frame,
                "Neuen Lieferanten hinzufügen",
                UnicodeSymbols.TRUCK,
                UnicodeSymbols.HEAVY_PLUS + " Hinzufügen",
                null
        );
    }

    /**
     * Shows a dialog to update an existing vendor.
     * @param existing The existing vendor data as an Object array.
     *                 Expected: [0]=name, [1]=contact, [2]=phone, [3]=email, [4]=address, [5]=articles, [6]=minOrder
     * @return The updated vendor data as an Object array, or null if canceled.
     */
    public static Object[] showUpdateVendorDialog(JFrame frame, Object[] existing) {
        if(frame == null) throw new IllegalArgumentException("Frame must not be null");
        if(existing == null) throw new IllegalArgumentException("Existing vendor data must not be null");
        return showVendorDialog(
                frame,
                "Lieferant bearbeiten",
                UnicodeSymbols.EDIT,
                UnicodeSymbols.FLOPPY + " Speichern",
                existing
        );
    }

    // ------------------------- Shared dialog builder -------------------------

    private static Object[] showVendorDialog(JFrame frame,
                                            String title,
                                            String headerIcon,
                                            String okText,
                                            Object[] existing) {
        final Object[][] holder = new Object[1][];

        ThemeManager.applyUIDefaults();

        JDialog dialog = new JDialog(frame, title, true);
        dialog.setUndecorated(true);

        JPanel mainContainer = createDialogChrome();

        // Header
        JPanel headerPanel = getHeaderPanel();
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(headerIcon);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = createHeaderCloseButton(dialog, holder);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();

        Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            if (text.contains("*")) {
                Color accent = ThemeManager.getAccentColor();
                String labelText = text.replace("*", "").trim();
                label.setText("<html>" + labelText + " <span style='color: rgb(" +
                        accent.getRed() + "," + accent.getGreen() + "," + accent.getBlue() +
                        ");'>*</span></html>");
            }
            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        int row = 0;

        // Name (required)
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + "  Name *"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField nameField = createStyledTextField();
        nameField.setText(safeString(existing, 0));
        contentCard.add(nameField, gbc);

        // Contact person
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PERSON + "  Kontaktperson"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField contactField = createStyledTextField();
        contactField.setText(safeString(existing, 1));
        contentCard.add(contactField, gbc);

        // Phone
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PHONE + "  Telefon"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField phoneField = createStyledTextField();
        phoneField.setText(safeString(existing, 2));
        contentCard.add(phoneField, gbc);

        // Email
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.MEMO + "  Email"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField emailField = createStyledTextField();
        emailField.setText(safeString(existing, 3));
        contentCard.add(emailField, gbc);

        // Address
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.BUILDING + "  Adresse"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField addressField = createStyledTextField();
        addressField.setText(safeString(existing, 4));
        contentCard.add(addressField, gbc);

        // Articles
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PACKAGE + "  Gelieferte Artikel (kommagetrennt)"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 8, 8, 8);
        JTextField articlesField = createStyledTextField();
        articlesField.setText(safeString(existing, 5));
        contentCard.add(articlesField, gbc);

        // Min Order Value (formatted)
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = ++row;
        contentCard.add(createLabel.apply(UnicodeSymbols.MONEY + "  Mindestbestellwert"), gbc);
        gbc.gridy = ++row;
        gbc.insets = new Insets(2, 8, 16, 8);
        JFormattedTextField minOrderField = createMoneyField();
        minOrderField.setValue(safeDouble(existing));
        contentCard.add(minOrderField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Footer buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setToolTipText("Abbrechen des Vorgangs");
        cancelBtn.setForeground(ThemeManager.getTextPrimaryColor());
        Color cancelBaseColor = ThemeManager.getSurfaceColor();
        Color cancelHoverColor = ThemeManager.getButtonHoverColor(cancelBaseColor);
        cancelBtn.setBackground(cancelBaseColor);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(cancelHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelBaseColor);
            }
        });

        JButton okBtn = new JButton(okText);
        okBtn.setToolTipText(title);
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        Color okBaseColor = ThemeManager.getButtonBackgroundColor();
        Color okHoverColor = ThemeManager.getButtonHoverColor(okBaseColor);
        Color okPressedColor = ThemeManager.getButtonPressedColor(okBaseColor);
        okBtn.setBackground(okBaseColor);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okBaseColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setOpaque(true);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        OrderGUI.addHoverEffects(okBtn, okBaseColor, okHoverColor, okPressedColor);

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // ESC closes
        installEscToClose(dialog, holder);

        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();

            double minOrder;
            try {
                minOrderField.commitEdit();
                Object v = minOrderField.getValue();
                minOrder = (v instanceof Number) ? ((Number) v).doubleValue() : 0.0;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte einen gültigen Mindestbestellwert eingeben.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            holder[0] = new Object[]{name, contact, phone, email, addr, arts, minOrder};
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    private static JPanel createDialogChrome() {
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // subtle shadow/outline look
        Color shadow = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadow, 1, true),
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true)
        ));

        return mainContainer;
    }

    private static GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        return gbc;
    }

    private static JButton createHeaderCloseButton(JDialog dialog, Object[][] holder) {
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Fenster schliessen");
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
                closeBtn.setForeground(ThemeManager.getTextOnPrimaryColor());
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
        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });
        return closeBtn;
    }

    private static void installEscToClose(JDialog dialog, Object[][] holder) {
        JRootPane root = dialog.getRootPane();
        root.registerKeyboardAction(
                e -> {
                    holder[0] = null;
                    dialog.dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.getHeaderBackgroundColor();
                Color color2 = ThemeManager.getHeaderGradientColor();

                GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }

    /**
     * Creates a styled text field for vendor dialogs with blue focus effect
     */
    private static JTextField createStyledTextField() {
        JTextField field = new JTextField(30);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    private static JFormattedTextField createMoneyField() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        NumberFormatter formatter = new NumberFormatter(nf);
        formatter.setValueClass(Double.class);
        formatter.setAllowsInvalid(false);
        formatter.setMinimum(0.0);

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(10);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    private static String safeString(Object[] existing, int idx) {
        if (existing == null || idx < 0 || idx >= existing.length) return "";
        Object v = existing[idx];
        return v == null ? "" : v.toString();
    }

    private static double safeDouble(Object[] existing) {
        if (existing == null || 6 < 0 || 6 >= existing.length) return 0.0;
        Object v = existing[6];
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString().trim());
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
