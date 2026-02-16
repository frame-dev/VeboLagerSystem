package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.ClientGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClientDialog {

    public static Object[] showAddClientDialog(JFrame frame, JComboBox<String> departmentCombobox) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.CLIENT + " Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with subtle shadow
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        Color shadowColor = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 80 : 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadowColor, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Modern gradient header
        JPanel headerPanel = getHeaderPanel();

        // Icon and title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.HEAVY_PLUS);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Neuen Kunden hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Modern close button with hover effect
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setToolTipText("Schließt das Fenster");
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
                closeBtn.setForeground(Color.WHITE);
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

        // Content card with better spacing
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Label creation with blue accent for required fields
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

        // Text field styling with blue focus effect
        Consumer<JTextField> styleTextField = field -> {
            Color normalBorder = ThemeManager.getInputBorderColor();
            Color focusBorder = ThemeManager.getAccentColor();

            field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            field.setBackground(ThemeManager.getInputBackgroundColor());
            field.setForeground(ThemeManager.getTextPrimaryColor());
            field.setCaretColor(ThemeManager.getAccentColor());
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(normalBorder, 1, true),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
            ));

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(focusBorder, 2, true),
                            BorderFactory.createEmptyBorder(11, 13, 11, 13)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(normalBorder, 1, true),
                            BorderFactory.createEmptyBorder(12, 14, 12, 14)
                    ));
                }
            });
        };

        int row = 0;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel nameLabel = createLabel.apply(UnicodeSymbols.PERSON + " Vorname und Nachname *");
        contentCard.add(nameLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 20, 8);
        JTextField nameField = new JTextField(35);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        // Department field
        row++;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row;
        JLabel deptLabel = createLabel.apply(UnicodeSymbols.DEPARTMENT + " Abteilung");
        contentCard.add(deptLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 8, 8);
        departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox);
        departmentCombobox.setPreferredSize(new Dimension(400, 40));
        styleComboBox(departmentCombobox);
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Modern button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        // Cancel button with hover
        Color cancelColor = ThemeManager.getErrorColor();
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + " Abbrechen");
        cancelBtn.setToolTipText("Das Hinzufügen des neuen Kunden abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(cancelColor);
        cancelBtn.setOpaque(true);
        cancelBtn.setContentAreaFilled(true);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cancelColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(ThemeManager.getButtonHoverColor(cancelColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelColor);
            }
        });

        // OK button with blue gradient colors and hover
        Color okColor = ThemeManager.getButtonBackgroundColor();
        Color okHover = ThemeManager.getButtonHoverColor(okColor);

        JButton okBtn = new JButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");
        okBtn.setToolTipText("Den neuen Kunden zur Datenbank hinzufügen");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okColor);
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(okHover);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(okHover.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(okColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Event handlers
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        JComboBox<String> finalDepartmentCombobox = departmentCombobox;
        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) finalDepartmentCombobox.getSelectedItem();
            if (selectedDept == null) selectedDept = "";
            String dept = selectedDept.trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            if (!clientManager.insertClient(name, dept)) {
                JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                holder[0] = null;
                return;
            }

            holder[0] = new Object[]{name, dept};
            dialog.dispose();
        });

        dialog.setSize(700, 450);
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    public static Object[] showUpdateClientDialog(JFrame frame, Object[] existing, JComboBox<String> departmentCombobox) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.EDIT + " Kunde bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with subtle shadow
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        Color shadowColor = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 80 : 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadowColor, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header with gradient-like effect
        JPanel headerPanel = createModernHeaderPanel(dialog, holder);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card with form fields
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Name field with enhanced styling
        int row = 0;
        gbc.gridy = row++;
        JLabel nameLabel = createStyledLabel(UnicodeSymbols.PERSON + "  Vorname und Nachname *");
        contentCard.add(nameLabel, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 20, 8);
        JTextField nameField = createStyledTextField(existing[0] == null ? "" : existing[0].toString());
        contentCard.add(nameField, gbc);

        // Department field with enhanced styling
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        JLabel deptLabel = createStyledLabel(UnicodeSymbols.DEPARTMENT + "  Abteilung");
        contentCard.add(deptLabel, gbc);

        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 8, 8);
        departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox);
        departmentCombobox.setPreferredSize(new Dimension(340, 38));
        styleComboBox(departmentCombobox);

        // Pre-select existing department
        if (existing[1] != null) {
            String existingDept = existing[1].toString();
            for (int i = 0; i < departmentCombobox.getItemCount(); i++) {
                if (existingDept.equals(departmentCombobox.getItemAt(i))) {
                    departmentCombobox.setSelectedIndex(i);
                    break;
                }
            }
        }

        contentCard.add(departmentCombobox, gbc);
        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel with enhanced styling
        JPanel buttonPanel = createUpdateDialogButtonPanel(dialog, holder, nameField, departmentCombobox);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton((JButton) buttonPanel.getComponent(1)); // OK button

        dialog.setSize(680, 480);
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    /**
     * Create styled label for dialog forms
     */
    private static JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    /**
     * Create styled text field for dialog forms with focus effects
     */
    private static JTextField createStyledTextField(String initialValue) {
        JTextField field = new JTextField(initialValue, 30);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());

        Color normalBorder = ThemeManager.getInputBorderColor();
        Color focusBorder = ThemeManager.getInputFocusBorderColor();

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(11, 13, 11, 13)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }
        });

        return field;
    }

    /**
     * Create button panel for update client dialog with enhanced buttons
     */
    private static JPanel createUpdateDialogButtonPanel(JDialog dialog, Object[][] holder, JTextField nameField, JComboBox<String> departmentCombobox) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 16));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        // Cancel button with hover effect
        Color cancelColor = ThemeManager.getErrorColor();
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + "  Abbrechen");
        cancelBtn.setToolTipText("Das Bearbeiten des Kunden abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(cancelColor);
        cancelBtn.setOpaque(true);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cancelColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(ThemeManager.getButtonHoverColor(cancelColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelColor);
            }
        });

        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        // OK button with hover effect
        Color okColor = ThemeManager.getSuccessColor();
        JButton okBtn = new JButton(UnicodeSymbols.FLOPPY + "  Speichern");
        okBtn.setToolTipText("Die Änderungen am Kunden speichern");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okColor);
        okBtn.setOpaque(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(ThemeManager.getButtonHoverColor(okColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okColor);
            }
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) departmentCombobox.getSelectedItem();
            if (selectedDept == null) selectedDept = "";
            String dept = selectedDept.trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            if (!clientManager.existsClient(name)) {
                if (!clientManager.insertClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    return;
                }
            } else {
                if (!clientManager.updateClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Aktualisieren des Kunden in der Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    return;
                }
            }

            holder[0] = new Object[]{name, dept};
            dialog.dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        return buttonPanel;
    }

    private static JPanel getPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.getAccentColor();
                Color color2 = ThemeManager.isDarkMode()
                        ? ThemeManager.getButtonHoverColor(color1)
                        : color1.brighter();

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
     * Create modern header panel for update client dialog with gradient effect
     */
    private static JPanel createModernHeaderPanel(JDialog dialog, Object[][] holder) {
        JPanel headerPanel = getPanel();

        // Title section with icon
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleSection.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.EDIT);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 22));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Kunde bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titleSection.add(iconLabel);
        titleSection.add(titleLabel);
        headerPanel.add(titleSection, BorderLayout.WEST);

        // Close button with hover effect
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 100));
                closeBtn.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
                closeBtn.setContentAreaFilled(false);
            }
        });

        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });

        headerPanel.add(closeBtn, BorderLayout.EAST);
        return headerPanel;
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

    private static void fillDepartmentList(JComboBox<String> departmentCombobox) {
        departmentCombobox.removeAllItems();
        departmentCombobox.addItem("");

        DepartmentManager.getInstance()
                .getAllDepartments()
                .stream()
                .map(dept -> dept.get("department"))
                .filter(dept -> dept != null && !dept.toString().trim().isEmpty())
                .map(dept -> dept.toString().trim())
                .distinct()
                .sorted()
                .forEach(departmentCombobox::addItem);

        departmentCombobox.setEditable(true);

        // Ensure editor is themed too
        Component editor = departmentCombobox.getEditor().getEditorComponent();
        if (editor instanceof JTextField tf) {
            tf.setBackground(ThemeManager.getInputBackgroundColor());
            tf.setForeground(ThemeManager.getTextPrimaryColor());
            tf.setCaretColor(ThemeManager.getTextPrimaryColor());
            tf.setBorder(null);
        }
    }

    private static void styleComboBox(JComboBox<String> combo) {
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

    // Rounded panel for card styling
    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
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
}
