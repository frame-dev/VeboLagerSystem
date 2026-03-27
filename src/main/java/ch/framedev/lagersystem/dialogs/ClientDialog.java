package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The ClientDialog class provides static methods to display dialogs for adding and updating client information. It uses Swing components to create a user-friendly interface that allows users to input client details such as name and department. The dialogs are designed with a consistent theme and include validation to ensure that required fields are filled out correctly. The class interacts with the ClientManager to perform database operations for inserting and updating client records.
 * @author framedev
 */
@SuppressWarnings({"ReassignedVariable", "UnusedAssignment"})
public class ClientDialog {

    private static final Logger LOGGER = LogManager.getLogger(ClientDialog.class);

    /**
     * Shared dialog UI builder for add/update client dialogs.
     * Returns an array: [mainContainer, nameField, departmentCombobox]
     */
    private static Object[] buildClientDialogUI(JDialog dialog, String headerIcon, String headerTitle, String initialName, String initialDept) {
        JPanel mainContainer = createDialogChrome();
        mainContainer.add(createGradientHeader(
                headerIcon,
                headerTitle,
                dialog,
                dialog::dispose
        ), BorderLayout.NORTH);

        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor());
        contentCard.setBorder(BorderFactory.createEmptyBorder(CARD_BORDER_TOP, CARD_BORDER_LEFT, CARD_BORDER_BOTTOM, CARD_BORDER_RIGHT));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = baseGbc();
        int row = 0;

        // Name
        gbc.gridy = row++;
        contentCard.add(createRequiredLabel(UnicodeSymbols.PERSON + " Vorname und Nachname *"), gbc);

        gbc.gridy = row++;
        gbc.insets = FIELD_INSETS;
        JTextField nameField = createTextField();
        if (initialName != null) nameField.setText(initialName);
        contentCard.add(nameField, gbc);

        // Department
        gbc.insets = LABEL_INSETS;
        gbc.gridy = row++;
        contentCard.add(createLabel(UnicodeSymbols.DEPARTMENT + " Abteilung"), gbc);

        gbc.gridy = row++;
        gbc.insets = COMBOBOX_INSETS;
        JComboBox<String> departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox);
        departmentCombobox.setPreferredSize(new Dimension(COMBOBOX_WIDTH, COMBOBOX_HEIGHT));
        styleComboBox(departmentCombobox);
        if (initialDept != null && !initialDept.isEmpty()) {
            for (int i = 0; i < departmentCombobox.getItemCount(); i++) {
                if (initialDept.equals(departmentCombobox.getItemAt(i))) {
                    departmentCombobox.setSelectedIndex(i);
                    break;
                }
            }
        }
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);
        return new Object[]{mainContainer, nameField, departmentCombobox};
    }

    private ClientDialog() {}

    // Layout and style constants
    private static final int DIALOG_WIDTH = 720;
    private static final int DIALOG_HEIGHT_ADD = 460;
    private static final int DIALOG_MIN_WIDTH = 640;
    private static final int DIALOG_MIN_HEIGHT_ADD = 420;
    private static final int CARD_BORDER_TOP = 28;
    private static final int CARD_BORDER_LEFT = 32;
    private static final int CARD_BORDER_BOTTOM = 24;
    private static final int CARD_BORDER_RIGHT = 32;
    private static final int COMBOBOX_WIDTH = 420;
    private static final int COMBOBOX_HEIGHT = 40;
    private static final Insets FIELD_INSETS = new Insets(4, 8, 18, 8);
    private static final Insets LABEL_INSETS = new Insets(8, 8, 8, 8);
    private static final Insets COMBOBOX_INSETS = new Insets(4, 8, 8, 8);

    /**
     * Shows a dialog to add a new client. The method returns an array containing the name and department of the newly added client if the user clicks "Add", or null if the user cancels the operation.
     * @param frame The parent frame for the dialog.
     * @return An array with the new client's details [name, department] if added, or null if cancelled.
     */
    public static Object[] showAddClientDialog(JFrame frame) {
        if(frame == null) throw new NullPointerException("Frame must not be null");
        LOGGER.info("Opening add-client dialog");
        ThemeManager.applyUIDefaults();
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.CLIENT + " Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        Object[] ui = buildClientDialogUI(dialog, UnicodeSymbols.HEAVY_PLUS, "Neuen Kunden hinzufügen", null, null);
        JPanel mainContainer = (JPanel) ui[0];
        JTextField nameField = (JTextField) ui[1];
        @SuppressWarnings("unchecked")
        JComboBox<String> departmentCombobox = (JComboBox<String>) ui[2];

        // Buttons
        JPanel buttonPanel = createFooterBar();
        buttonPanel.add(createDialogButton(
            UnicodeSymbols.CLOSE + " Abbrechen",
            "Das Hinzufügen des neuen Kunden abbrechen",
            ThemeManager.getErrorColor(),
            ae -> {
                holder[0] = null;
                dialog.dispose();
            }
        ));
        buttonPanel.add(createDialogButton(
            UnicodeSymbols.CHECKMARK + " Hinzufügen",
            "Den neuen Kunden zur Datenbank hinzufügen",
            ThemeManager.getButtonBackgroundColor(),
            ae -> {
                String name = nameField.getText().trim();
                String selectedDept = (String) departmentCombobox.getSelectedItem();
                if (selectedDept == null) selectedDept = "";
                String dept = selectedDept.trim();

                if (name.isEmpty()) {
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Name ist erforderlich.")
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                return;
                }

                ClientManager clientManager = ClientManager.getInstance();
                if (!clientManager.insertClient(name, dept)) {
                    LOGGER.warn("Failed to insert client '{}' with department '{}'", name, dept);
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Fehler beim Hinzufügen des Kunden zur Datenbank.")
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                holder[0] = null;
                return;
                }

                LOGGER.info("Inserted client '{}' with department '{}'", name, dept);
                Main.logUtils.addLog(Level.INFO, "Kunde hinzugefügt: " + name + (dept.isBlank() ? "" : " (" + dept + ")"));
                holder[0] = new Object[]{name, dept};
                dialog.dispose();
            }
        ));
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        // Set default button to the 'Add' button (second button in panel)
        if (buttonPanel.getComponentCount() > 1 && buttonPanel.getComponent(1) instanceof JButton addBtn) {
            dialog.getRootPane().setDefaultButton(addBtn);
        }

        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT_ADD);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT_ADD));
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    public static Object[] showAddClientDialog(JFrame frame, String receiver) {
        if(frame == null) throw new NullPointerException("Frame must not be null");
        LOGGER.info("Opening add-client dialog with suggested receiver '{}'", receiver);
        ThemeManager.applyUIDefaults();
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.CLIENT + " Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);
        dialog.getRootPane().registerKeyboardAction(
            e -> dialog.dispose(),
            KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        Object[] ui = buildClientDialogUI(dialog, UnicodeSymbols.HEAVY_PLUS, "Neuen Kunden hinzufügen", null, null);
        JPanel mainContainer = (JPanel) ui[0];
        JTextField nameField = (JTextField) ui[1];
        nameField.setText(receiver);
        @SuppressWarnings("unchecked")
        JComboBox<String> departmentCombobox = (JComboBox<String>) ui[2];

        // Buttons
        JPanel buttonPanel = createFooterBar();
        buttonPanel.add(createDialogButton(
            UnicodeSymbols.CLOSE + " Abbrechen",
            "Das Hinzufügen des neuen Kunden abbrechen",
            ThemeManager.getErrorColor(),
            ae -> {
                holder[0] = null;
                dialog.dispose();
            }
        ));
        buttonPanel.add(createDialogButton(
            UnicodeSymbols.CHECKMARK + " Hinzufügen",
            "Den neuen Kunden zur Datenbank hinzufügen",
            ThemeManager.getButtonBackgroundColor(),
            ae -> {
                String name = nameField.getText().trim();
                String selectedDept = (String) departmentCombobox.getSelectedItem();
                if (selectedDept == null) selectedDept = "";
                String dept = selectedDept.trim();

                if (name.isEmpty()) {
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Name ist erforderlich.")
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                return;
                }

                ClientManager clientManager = ClientManager.getInstance();
                if (!clientManager.insertClient(name, dept)) {
                    LOGGER.warn("Failed to insert client '{}' with department '{}'", name, dept);
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Fehler beim Hinzufügen des Kunden zur Datenbank.")
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                holder[0] = null;
                return;
                }

                LOGGER.info("Inserted client '{}' with department '{}'", name, dept);
                Main.logUtils.addLog(Level.INFO, "Kunde hinzugefügt: " + name + (dept.isBlank() ? "" : " (" + dept + ")"));
                holder[0] = new Object[]{name, dept};
                dialog.dispose();
            }
        ));
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        // Set default button to the 'Add' button (second button in panel)
        if (buttonPanel.getComponentCount() > 1 && buttonPanel.getComponent(1) instanceof JButton addBtn) {
            dialog.getRootPane().setDefaultButton(addBtn);
        }

        dialog.setSize(DIALOG_WIDTH, DIALOG_HEIGHT_ADD);
        dialog.setMinimumSize(new Dimension(DIALOG_MIN_WIDTH, DIALOG_MIN_HEIGHT_ADD));
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
        LOGGER.info("Opening update-client dialog for '{}'", existing.length > 0 ? existing[0] : "");
        ThemeManager.applyUIDefaults();
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(frame, UnicodeSymbols.EDIT + " Kunde bearbeiten", true);
        dialog.setUndecorated(true);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        String initialName = (existing.length > 0 && existing[0] != null) ? existing[0].toString() : "";
        String initialDept = (existing.length > 1 && existing[1] != null) ? existing[1].toString() : "";
        Object[] ui = buildClientDialogUI(dialog, UnicodeSymbols.EDIT, "Kunde bearbeiten", initialName, initialDept);
        JPanel mainContainer = (JPanel) ui[0];
        JTextField nameField = (JTextField) ui[1];
        @SuppressWarnings("unchecked")
        JComboBox<String> departmentCombobox = (JComboBox<String>) ui[2];
        final String originalName = nameField.getText().trim();

        JPanel buttonPanel = createFooterBar();
        buttonPanel.add(createDialogButton(
                UnicodeSymbols.CLOSE + "  Abbrechen",
                "Das Bearbeiten des Kunden abbrechen",
                ThemeManager.getErrorColor(),
                ae -> {
                    holder[0] = null;
                    dialog.dispose();
                }
        ));
        buttonPanel.add(createDialogButton(
                UnicodeSymbols.FLOPPY + "  Speichern",
                "Die Änderungen am Kunden speichern",
                ThemeManager.getSuccessColor(),
                ae -> {
                    String name = nameField.getText().trim();
                    if (!originalName.isEmpty() && !name.equals(originalName)) {
                        LOGGER.warn("Rejected client rename from '{}' to '{}'", originalName, name);
                        new MessageDialog()
                                .setTitle("Hinweis")
                                .setMessage("Der Kundenname kann aktuell nicht geändert werden.\nBitte erstellen Sie einen neuen Kunden oder lassen Sie den Namen unverändert.")
                                .setMessageType(JOptionPane.WARNING_MESSAGE)
                                .display();
                        return;
                    }
                    String selectedDept = (String) departmentCombobox.getSelectedItem();
                    if (selectedDept == null) selectedDept = "";
                    String dept = selectedDept.trim();

                    if (name.isEmpty()) {
                        new MessageDialog()
                                .setTitle("Fehler")
                                .setMessage("Name ist erforderlich.")
                                .setMessageType(JOptionPane.ERROR_MESSAGE)
                                .display();
                        return;
                    }

                    ClientManager clientManager = ClientManager.getInstance();
                    if (!clientManager.existsClient(name)) {
                        if (!clientManager.insertClient(name, dept)) {
                            LOGGER.warn("Failed to insert missing client '{}' with department '{}' during update dialog", name, dept);
                            new MessageDialog()
                                    .setTitle("Fehler")
                                    .setMessage("Fehler beim Hinzufügen des Kunden zur Datenbank.")
                                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                                    .display();
                            return;
                        }
                        LOGGER.info("Inserted missing client '{}' with department '{}' from update dialog", name, dept);
                        Main.logUtils.addLog(Level.INFO,
                                "Kunde hinzugefügt: " + name + (dept.isBlank() ? "" : " (" + dept + ")"));
                    } else {
                        if (!clientManager.updateClient(name, dept)) {
                            LOGGER.warn("Failed to update client '{}' with department '{}'", name, dept);
                            new MessageDialog()
                                    .setTitle("Fehler")
                                    .setMessage("Fehler beim Aktualisieren des Kunden in der Datenbank.")
                                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                                    .display();
                            return;
                        }
                        LOGGER.info("Updated client '{}' with department '{}'", name, dept);
                        Main.logUtils.addLog(Level.INFO,
                                "Kunde aktualisiert: " + name + (dept.isBlank() ? "" : " (" + dept + ")"));
                    }

                    holder[0] = new Object[]{name, dept};
                    dialog.dispose();
                }
        ));
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        // Set default button to the 'Speichern' button (second button in panel)
        if (buttonPanel.getComponentCount() > 1 && buttonPanel.getComponent(1) instanceof JButton saveBtn) {
            dialog.getRootPane().setDefaultButton(saveBtn);
        }

        dialog.setSize(720, 480);
        dialog.setMinimumSize(new Dimension(640, 440));
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    // ------------------------- UI building blocks -------------------------
    private static JButton createDialogButton(String text, String tooltip, Color base, java.awt.event.ActionListener action) {
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);
        Color fg = ThemeManager.getTextOnPrimaryColor();
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
        btn.addActionListener(action);
        return btn;
    }

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

    // Removed unused button creation methods

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
        JFrameUtils.styleComboBox(combo);
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
