package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.OrderGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

@SuppressWarnings("DuplicatedCode")
public class VendorDialog {
    public static Object[] showAddVendorDialog(JFrame frame) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, "Neuen Lieferanten hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient
        JPanel headerPanel = getHeaderPanel();

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.TRUCK);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Neuen Lieferanten hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Close button
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
        headerPanel.add(closeBtn, BorderLayout.EAST);

        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Helper for creating labels
        Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            if (text.contains("*")) {
                Color blueAccent = ThemeManager.getAccentColor();
                String labelText = text.replace("*", "").trim();
                label.setText("<html>" + labelText + " <span style='color: rgb(" +
                        blueAccent.getRed() + "," + blueAccent.getGreen() + "," + blueAccent.getBlue() +
                        ");'>*</span></html>");
            }
            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        int row = 0;

        // Name field
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + "  Name *"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField idField = createStyledTextField();
        contentCard.add(idField, gbc);

        // Contact person
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PERSON + "  Kontaktperson"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField contactField = createStyledTextField();
        contentCard.add(contactField, gbc);

        // Phone
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PHONE + "  Telefon"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField phoneField = createStyledTextField();
        contentCard.add(phoneField, gbc);

        // Email
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.MEMO + "  Email"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField emailField = createStyledTextField();
        contentCard.add(emailField, gbc);

        // Address
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.BUILDING + "  Adresse"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField addressField = createStyledTextField();
        contentCard.add(addressField, gbc);

        // Articles
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PACKAGE + "  Gelieferte Artikel (kommagetrennt)"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 8, 8, 8);
        JTextField articlesField = createStyledTextField();
        contentCard.add(articlesField, gbc);

        // Min Order Value
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = ++row;
        contentCard.add(createLabel.apply(UnicodeSymbols.MONEY + "  Mindestbestellwert"), gbc);
        gbc.gridy = ++row;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField minOrderField = createStyledTextField();
        minOrderField.setText("0.0");
        contentCard.add(minOrderField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel
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

        JButton okBtn = new JButton(UnicodeSymbols.HEAVY_PLUS + " Hinzufügen");
        okBtn.setToolTipText("Neuen Lieferanten hinzufügen");
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

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Action listeners
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();
            double minOrder = 0.0;
            try {
                minOrder = Double.parseDouble(minOrderField.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            holder[0] = new Object[]{id, contact, phone, email, addr, arts, minOrder};
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(idField::requestFocusInWindow);
        dialog.setVisible(true);
        return holder[0];
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

    /**
     * Shows a dialog to update an existing vendor.
     * @param existing The existing vendor data as an Object array.
     * @return The updated vendor data as an Object array, or null if canceled.
     */
    public static Object[] showUpdateVendorDialog(JFrame frame, Object[] existing) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, "Lieferant bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient
        JPanel headerPanel = getHeaderPanel();

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.EDIT);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Lieferant bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Close button
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
        headerPanel.add(closeBtn, BorderLayout.EAST);

        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Helper for creating labels
        Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        int row = 0;

        // Name field
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + "  Name"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField idField = createStyledTextField();
        idField.setText(existing[0] == null ? "" : existing[0].toString());
        contentCard.add(idField, gbc);

        // Contact person
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PERSON + "  Kontaktperson"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField contactField = createStyledTextField();
        contactField.setText(existing[1] == null ? "" : existing[1].toString());
        contentCard.add(contactField, gbc);

        // Phone
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PHONE + "  Telefon"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField phoneField = createStyledTextField();
        phoneField.setText(existing[2] == null ? "" : existing[2].toString());
        contentCard.add(phoneField, gbc);

        // Email
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.MEMO + "  Email"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField emailField = createStyledTextField();
        emailField.setText(existing[3] == null ? "" : existing[3].toString());
        contentCard.add(emailField, gbc);

        // Address
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.BUILDING + "  Adresse"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField addressField = createStyledTextField();
        addressField.setText(existing[4] == null ? "" : existing[4].toString());
        contentCard.add(addressField, gbc);

        // Articles
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PACKAGE + "  Gelieferte Artikel (kommagetrennt)"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 8, 8, 8);
        JTextField articlesField = createStyledTextField();
        articlesField.setText(existing[5] == null ? "" : existing[5].toString());
        contentCard.add(articlesField, gbc);

        // Min Order Value
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = ++row;
        contentCard.add(createLabel.apply(UnicodeSymbols.MONEY + "  Mindestbestellwert"), gbc);
        gbc.gridy = ++row;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField minOrderField = createStyledTextField();
        minOrderField.setText(existing.length > 6 && existing[6] != null ? existing[6].toString() : "0.0");
        contentCard.add(minOrderField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setToolTipText("Abbrechen des Vorgangs");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
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

        JButton okBtn = new JButton(UnicodeSymbols.FLOPPY + " Speichern");
        okBtn.setToolTipText("Änderungen speichern");
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

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Action listeners
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();
            double minOrder = 0.0;
            try {
                minOrder = Double.parseDouble(minOrderField.getText().trim());
            } catch (NumberFormatException ignored) {
            }

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            holder[0] = new Object[]{id, contact, phone, email, addr, arts, minOrder};
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(idField::requestFocusInWindow);
        dialog.setVisible(true);
        return holder[0];
    }

}
