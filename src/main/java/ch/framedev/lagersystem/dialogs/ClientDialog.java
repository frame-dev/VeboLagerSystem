package ch.framedev.lagersystem.dialogs;

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

/**
 * The ClientDialog class provides static methods to display dialogs for adding and updating client information. It uses Swing components to create a user-friendly interface that allows users to input client details such as name and department. The dialogs are designed with a consistent theme and include validation to ensure that required fields are filled out correctly. The class interacts with the ClientManager to perform database operations for inserting and updating client records.
 * @author framedev
 */
@SuppressWarnings({"DuplicatedCode", "ReassignedVariable", "UnusedAssignment"})
public class ClientDialog {

    private ClientDialog() {
    }

    /**
     * Shows a dialog to add a new client. The method returns an array containing the name and department of the newly added client if the user clicks "Add", or null if the user cancels the operation.
     * @param frame The parent frame for the dialog.
     * @return An array with the new client's details [name, department] if added, or null if cancelled.
     */
    public static Object[] showAddClientDialog(JFrame frame) {
        if(frame == null) throw new NullPointerException("Frame must not be null");
        ThemeManager.applyUIDefaults();
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.CLIENT + " Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        JPanel mainContainer = createDialogChrome();

        JPanel headerPanel = createGradientHeader(
                UnicodeSymbols.HEAVY_PLUS,
                "Neuen Kunden hinzufügen",
                dialog,
                () -> {
                    holder[0] = null;
                    dialog.dispose();
                }
        );
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor());
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        int row = 0;

        // Name
        gbc.gridy = row++;
        contentCard.add(createRequiredLabel(UnicodeSymbols.PERSON + " Vorname und Nachname *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 18, 8);
        JTextField nameField = createTextField();
        contentCard.add(nameField, gbc);

        // Department
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel(UnicodeSymbols.DEPARTMENT + " Abteilung"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 8, 8);
        JComboBox<String> departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox);
        departmentCombobox.setPreferredSize(new Dimension(420, 40));
        styleComboBox(departmentCombobox);
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = createFooterBar();

        JButton cancelBtn = createDangerButton(UnicodeSymbols.CLOSE + " Abbrechen", "Das Hinzufügen des neuen Kunden abbrechen");
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        JButton okBtn = createPrimaryButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");
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
            if (!clientManager.insertClient(name, dept)) {
                JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                holder[0] = null;
                return;
            }

            holder[0] = new Object[]{name, dept};
            dialog.dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        dialog.setSize(720, 460);
        dialog.setMinimumSize(new Dimension(640, 420));
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    /**
     * Shows a dialog to edit an existing client's details. The existing array should contain the current name at index 0 and the current department at index 1 (both can be null or empty). The method returns an array with the updated name and department if the user clicks "Save", or null if the user cancels the operation.
     * @param frame The parent frame for the dialog.
     * @param existing An array containing the existing client details: [name, department]. Both can be null or empty if not available.
     * @return An array with the updated client details [name, department] if saved, or null if cancelled.
     */
    public static Object[] showUpdateClientDialog(JFrame frame, Object[] existing) {
        if(frame == null) throw new IllegalArgumentException("Frame must not be null");
        if(existing == null) throw new IllegalArgumentException("Existing must not be null");
        ThemeManager.applyUIDefaults();
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.EDIT + " Kunde bearbeiten", true);
        dialog.setUndecorated(true);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        JPanel mainContainer = createDialogChrome();

        JPanel headerPanel = createGradientHeader(
                UnicodeSymbols.EDIT,
                "Kunde bearbeiten",
                dialog,
                () -> {
                    holder[0] = null;
                    dialog.dispose();
                }
        );
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor());
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 24, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        int row = 0;

        // Name
        gbc.gridy = row++;
        contentCard.add(createRequiredLabel(UnicodeSymbols.PERSON + " Vorname und Nachname *"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 18, 8);
        JTextField nameField = createTextField();
        nameField.setText(existing.length > 0 && existing[0] != null ? existing[0].toString() : "");
        final String originalName = nameField.getText().trim();
        contentCard.add(nameField, gbc);

        // Department
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel(UnicodeSymbols.DEPARTMENT + " Abteilung"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 8, 8);
        JComboBox<String> departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox);
        departmentCombobox.setPreferredSize(new Dimension(420, 40));
        styleComboBox(departmentCombobox);

        // Pre-select existing department
        if (existing.length > 1 && existing[1] != null) {
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

        JPanel buttonPanel = createFooterBar();

        JButton cancelBtn = createDangerButton(UnicodeSymbols.CLOSE + "  Abbrechen", "Das Bearbeiten des Kunden abbrechen");
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        JButton okBtn = createSuccessButton(UnicodeSymbols.FLOPPY + "  Speichern");
        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            if (!originalName.isEmpty() && !name.equals(originalName)) {
                JOptionPane.showMessageDialog(dialog,
                        "Der Kundenname kann aktuell nicht geändert werden.\nBitte erstellen Sie einen neuen Kunden oder lassen Sie den Namen unverändert.",
                        "Hinweis",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
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
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        dialog.setSize(720, 480);
        dialog.setMinimumSize(new Dimension(640, 440));
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    // ------------------------- UI building blocks -------------------------

    private static JPanel createDialogChrome() {
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // Outer subtle shadow + inner border (looks crisp in light + dark)
        Color shadow = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 30);
        Color inner = ThemeManager.getBorderColor();
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadow, 1, true),
                BorderFactory.createLineBorder(inner, 1, true)
        ));

        return mainContainer;
    }

    private static JPanel createFooterBar() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));
        return buttonPanel;
    }

    private static GridBagConstraints baseGbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        return gbc;
    }

    private static JPanel createGradientHeader(String icon, String title, JDialog dialog, Runnable onClose) {
        JPanel headerPanel = getHeaderPanel();

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schließt das Fenster");
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 210));
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
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 110));
                closeBtn.setContentAreaFilled(true);
                closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 210));
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
                closeBtn.setContentAreaFilled(false);
                closeBtn.setBorder(null);
            }
        });

        closeBtn.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            } else {
                dialog.dispose();
            }
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
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color color1 = ThemeManager.getHeaderBackgroundColor();
                Color color2 = ThemeManager.getHeaderGradientColor();

                GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        return headerPanel;
    }

    private static JLabel createLabel(String text) {
        if(text == null) throw new IllegalArgumentException("Text must not be null");
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    private static JLabel createRequiredLabel(String text) {
        if(text == null) throw new IllegalArgumentException("Text must not be null");
        JLabel label = createLabel(text);
        if (text.contains("*")) {
            Color accent = ThemeManager.getAccentColor();
            String labelText = text.replace("*", "").trim();
            label.setText("<html>" + labelText + " <span style='color: rgb(" +
                    accent.getRed() + "," + accent.getGreen() + "," + accent.getBlue() +
                    ");'>*</span></html>");
        }
        return label;
    }

    private static JTextField createTextField() {
        JTextField field = new JTextField(35);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());

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

    private static JButton createPrimaryButton(String text) {
        if(text == null) throw new IllegalArgumentException("Text must not be null");
        Color base = ThemeManager.getButtonBackgroundColor();
        return createFilledButton(text, "Den neuen Kunden zur Datenbank hinzufügen", base, ThemeManager.getTextOnPrimaryColor());
    }

    private static JButton createSuccessButton(String text) {
        if(text == null) throw new IllegalArgumentException("Text must not be null");
        Color base = ThemeManager.getSuccessColor();
        return createFilledButton(text, "Die Änderungen am Kunden speichern", base, ThemeManager.getTextOnPrimaryColor());
    }

    private static JButton createDangerButton(String text, String tooltip) {
        if(text == null) throw new IllegalArgumentException("Text must not be null");
        Color base = ThemeManager.getErrorColor();
        return createFilledButton(text, tooltip, base, ThemeManager.getTextOnPrimaryColor());
    }

    private static JButton createFilledButton(String text, String tooltip, Color base, Color fg) {
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);

        JButton btn = new JButton(text);
        btn.setToolTipText(tooltip);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setBackground(base);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 26, 12, 26)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hover.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 26, 12, 26)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(base);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(base.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 26, 12, 26)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(btn.contains(e.getPoint()) ? hover : base);
            }
        });

        return btn;
    }

    // ------------------------- Existing helpers -------------------------

    private static void fillDepartmentList(JComboBox<String> departmentCombobox) {
        if(departmentCombobox == null) throw new IllegalArgumentException("Department combobox must not be null");
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
            tf.setCaretColor(ThemeManager.getAccentColor());
            tf.setBorder(null);
        }
    }

    private static void styleComboBox(JComboBox<String> combo) {
        if(combo == null) throw new IllegalArgumentException("Combobox must not be null");
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
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        b.setBackground(surface);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        b.setBackground(bg);
                    }
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
                tf.setCaretColor(ThemeManager.getAccentColor());
                tf.setBorder(null);
            }
        }
    }

    // Rounded panel for card styling
    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundedPanel(Color bg) {
            this.bg = bg;
            this.radius = 14;
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
