package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.text.NumberFormatter;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.function.Consumer;
import java.util.function.Function;

import static ch.framedev.lagersystem.guis.ArticleGUI.getHeaderPanel;

public class ArticleDialog {

    /**
     * Displays a dialog for updating an article. The dialog allows users to modify article properties
     * such as article number, name, details, stock levels, and minimum stock levels.
     *
     * @param existingData An array containing the existing data of the article to be updated.
     *                     This array should be structured as follows:
     *                     [0: artikelNr (String/Number), 1: name (String), 2: category (Object),
     *                     3: details (String), 4: lagerbestand (Number), 5: mindestbestand (Number),
     *                     6: verkaufspreis (Number), 7: einkaufspreis (Number), 8: lieferant (Object)].
     *                     Null or invalid indices in the array will be handled gracefully.
     * @return An array containing the updated data for the article, in the same format as the input array.
     *         If the dialog is canceled, this method will return null.
     */
    public static Object[] showUpdateArticleDialog(JFrame frame, Object[] existingData) {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(frame, "Artikel Bearbeiten", true);
        dialog.setUndecorated(true);

        JPanel mainContainer = createDialogMainContainer();

        // Header (same look as Add-Dialog)
        JPanel headerPanel = getHeaderPanel();
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.EDIT);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(UnicodeSymbols.CHECKMARK + "  Artikel Bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = createDialogCloseButton(dialog, resultHolder);
        headerPanel.add(closeBtn, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;

        // Helpers
        Function<String, JLabel> createLabel = ArticleDialog::createDialogLabel;
        Consumer<JTextField> styleTextField = field -> {
            // Reuse the same look as createDialogTextField(), but allow custom columns/value
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
        };

        int row = 0;

        // Artikelnummer
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.NUMBERS + " Artikelnummer *"), gbc);
        gbc.gridy = row++;
        JTextField nummerField = new JTextField(existingData != null && existingData.length > 0 && existingData[0] != null ? existingData[0].toString() : "", 25);
        styleTextField.accept(nummerField);
        contentCard.add(nummerField, gbc);

        // Name
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PENCIL + " Name *"), gbc);
        gbc.gridy = row++;
        JTextField nameField = new JTextField(existingData != null && existingData.length > 1 && existingData[1] != null ? existingData[1].toString() : "", 25);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        // Details
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.CLIPBOARD + " Details"), gbc);
        gbc.gridy = row++;
        JTextField detailsField = new JTextField(existingData != null && existingData.length > 3 && existingData[3] != null ? existingData[3].toString() : "", 25);
        styleTextField.accept(detailsField);
        contentCard.add(detailsField, gbc);

        // Existing numeric values (account for category at index 2)
        int existingLager = 0;
        int existingMindest = 0;
        double existingVerkauf = 0.0;
        double existingEinkauf = 0.0;

        try {
            Object o = existingData != null && existingData.length > 4 ? existingData[4] : null;
            if (o instanceof Number) existingLager = ((Number) o).intValue();
            else if (o != null) existingLager = Integer.parseInt(o.toString());
        } catch (Exception ignored) {}

        try {
            Object o = existingData != null && existingData.length > 5 ? existingData[5] : null;
            if (o instanceof Number) existingMindest = ((Number) o).intValue();
            else if (o != null) existingMindest = Integer.parseInt(o.toString());
        } catch (Exception ignored) {}

        try {
            Object o = existingData != null && existingData.length > 6 ? existingData[6] : null;
            if (o instanceof Number) existingVerkauf = ((Number) o).doubleValue();
            else if (o != null) existingVerkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {}

        try {
            Object o = existingData != null && existingData.length > 7 ? existingData[7] : null;
            if (o instanceof Number) existingEinkauf = ((Number) o).doubleValue();
            else if (o != null) existingEinkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {}

        // Stock section
        gbc.gridy = row++;
        JLabel stockSection = createLabel.apply(UnicodeSymbols.PACKAGE + " Lagerbestand");
        stockSection.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(stockSection, gbc);

        gbc.gridy = row++;
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        stockPanel.setOpaque(false);

        JPanel lagerPanel = new JPanel(new BorderLayout(0, 4));
        lagerPanel.setOpaque(false);
        JLabel lagerLabel = new JLabel("Aktuell");
        lagerLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        lagerLabel.setForeground(ThemeManager.getTextSecondaryColor());
        lagerPanel.add(lagerLabel, BorderLayout.NORTH);

        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(existingLager, 0, Integer.MAX_VALUE, 1));
        lagerSpinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        styleDialogSpinner(lagerSpinner);
        lagerPanel.add(lagerSpinner, BorderLayout.CENTER);

        JPanel mindestPanel = new JPanel(new BorderLayout(0, 4));
        mindestPanel.setOpaque(false);
        JLabel mindestLabel = new JLabel("Mindestbestand");
        mindestLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        mindestLabel.setForeground(ThemeManager.getTextSecondaryColor());
        mindestPanel.add(mindestLabel, BorderLayout.NORTH);

        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(existingMindest, 0, Integer.MAX_VALUE, 1));
        mindestSpinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        styleDialogSpinner(mindestSpinner);
        mindestPanel.add(mindestSpinner, BorderLayout.CENTER);

        stockPanel.add(lagerPanel);
        stockPanel.add(mindestPanel);
        contentCard.add(stockPanel, gbc);

        // Price section
        gbc.gridy = row++;
        JLabel priceSection = createLabel.apply(UnicodeSymbols.MONEY + " Preise");
        priceSection.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(priceSection, gbc);

        gbc.gridy = row++;
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        pricePanel.setOpaque(false);

        NumberFormatter priceFormatter = createPriceFormatter();

        JPanel verkaufPanel = createLabeledPriceField("Verkaufspreis (CHF)", priceFormatter);
        JFormattedTextField verkaufField = findFormattedField(verkaufPanel);
        if (verkaufField == null) {
            verkaufField = new JFormattedTextField(priceFormatter);
            verkaufField.setColumns(10);
            verkaufPanel.add(verkaufField, BorderLayout.CENTER);
        }
        verkaufField.setValue(existingVerkauf);

        JPanel einkaufPanel = createLabeledPriceField("Einkaufspreis (CHF)", priceFormatter);
        JFormattedTextField einkaufField = findFormattedField(einkaufPanel);
        if (einkaufField == null) {
            einkaufField = new JFormattedTextField(priceFormatter);
            einkaufField.setColumns(10);
            einkaufPanel.add(einkaufField, BorderLayout.CENTER);
        }
        einkaufField.setValue(existingEinkauf);

        pricePanel.add(verkaufPanel);
        pricePanel.add(einkaufPanel);
        contentCard.add(pricePanel, gbc);

        // Lieferant
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + " Lieferant"), gbc);
        gbc.gridy = row++;
        JTextField lieferantField = new JTextField(existingData != null && existingData.length > 8 && existingData[8] != null ? existingData[8].toString() : "", 25);
        styleTextField.accept(lieferantField);
        contentCard.add(lieferantField, gbc);

        JScrollPane scrollPane = createDialogScrollPane(contentCard);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        // Buttons (rounded card bar)
        ArticleGUI.RoundedPanel buttonPanel = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JButton cancelBtn = createDialogDangerActionButton(UnicodeSymbols.CLOSE + " Abbrechen");
        JButton okBtn = createDialogPrimaryActionButton(UnicodeSymbols.CHECKMARK + " Speichern");
        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });

        JFormattedTextField finalVerkaufField = verkaufField;
        JFormattedTextField finalEinkaufField = einkaufField;
        okBtn.addActionListener(ae -> {
            String nummer = nummerField.getText().trim();
            String name = nameField.getText().trim();
            String details = detailsField.getText().trim();
            String lieferant = lieferantField.getText().trim();

            if (nummer.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Artikelnummer und Name sind Pflichtfelder.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            int lagerbestand = ((Number) lagerSpinner.getValue()).intValue();
            int mindestbestand = ((Number) mindestSpinner.getValue()).intValue();

            double verkaufspreis;
            double einkaufspreis;
            try {
                finalVerkaufField.commitEdit();
                finalEinkaufField.commitEdit();
                verkaufspreis = ((Number) finalVerkaufField.getValue()).doubleValue();
                einkaufspreis = ((Number) finalEinkaufField.getValue()).doubleValue();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte gültige Preise eingeben.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            // IMPORTANT: return format expected by ArticleGUI update flow (no category)
            resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                    verkaufspreis, einkaufspreis, lieferant};
            dialog.dispose();
        });

        dialog.setSize(720, 760);
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(nummerField::requestFocusInWindow);
        dialog.setVisible(true);

        return resultHolder[0];
    }
    private static JButton createDialogPrimaryActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        btn.setForeground(Color.WHITE);

        Color base = ThemeManager.getButtonBackgroundColor();
        applyDialogActionButtonPalette(btn, base, ThemeManager.getButtonHoverColor(base), ThemeManager.getButtonPressedColor(base));
        return btn;
    }

    private static JButton createDialogDangerActionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        btn.setForeground(Color.WHITE);

        Color base = ThemeManager.getErrorColor();
        applyDialogActionButtonPalette(btn, base, ThemeManager.getButtonHoverColor(base), ThemeManager.getButtonPressedColor(base));
        return btn;
    }

    private static void applyDialogActionButtonPalette(JButton btn, Color base, Color hover, Color pressed) {
        btn.setBackground(base);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hover);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hover.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 24, 12, 24)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(base);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(base.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 24, 12, 24)
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
    }

    private static JScrollPane createDialogScrollPane(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);

        // Optional: make scrollbars feel less harsh on light theme
        sp.getVerticalScrollBar().setBackground(ThemeManager.getBackgroundColor());
        sp.getHorizontalScrollBar().setBackground(ThemeManager.getBackgroundColor());
        return sp;
    }

    /**
     * Shows a polished modal dialog to enter a new article. Returns the table row or null if canceled.
     */
    public static Object[] showAddArticleDialog(JFrame frame) {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = createAddArticleDialog(frame);
        ArticleDialogComponents components = createArticleDialogComponents(dialog, resultHolder);

        showDialog(frame, dialog, components.nummerField());

        return resultHolder[0];
    }

    /**
     * Creates and configures the main dialog
     */
    public static JDialog createAddArticleDialog(JFrame frame) {
        JDialog dialog = new JDialog(frame, "Neuen Artikel hinzufügen", true);
        dialog.setUndecorated(true);
        return dialog;
    }

    /**
     * Creates all dialog components
     */
    public static ArticleDialogComponents createArticleDialogComponents(JDialog dialog, Object[][] resultHolder) {
        JPanel mainContainer = createDialogMainContainer();

        // Header
        JPanel headerPanel = createDialogHeader(dialog, resultHolder);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content with form fields
        ArticleFormFields formFields = createArticleFormFields();
        mainContainer.add(formFields.contentCard(), BorderLayout.CENTER);

        // Buttons
        DialogButtons dialogButtons = createDialogButtons(dialog, resultHolder, formFields);
        mainContainer.add(dialogButtons.buttonPanel(), BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(dialogButtons.okButton());

        return new ArticleDialogComponents(
                formFields.nummerField(),
                formFields.nameField(),
                formFields.detailsField(),
                formFields.lagerSpinner(),
                formFields.mindestSpinner(),
                formFields.verkaufField(),
                formFields.einkaufField(),
                formFields.lieferantField()
        );
    }

    /**
     * Creates the main container panel with shadow effect
     */
    public static JPanel createDialogMainContainer() {
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // Outer padding so the dialog doesn't hug the screen edge
        Border outerPadding = BorderFactory.createEmptyBorder(12, 12, 12, 12);

        // Subtle shadow + crisp border for a modern floating-card look
        Color shadowColor = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 35);
        Border shadow = BorderFactory.createLineBorder(shadowColor, 1, true);
        Border crisp = BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true);

        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                outerPadding,
                BorderFactory.createCompoundBorder(shadow, crisp)
        ));

        return mainContainer;
    }

    /**
     * Creates the dialog header with title and close button - modern blue gradient design
     */
    public static JPanel createDialogHeader(JDialog dialog, Object[][] resultHolder) {
        // Create gradient panel for modern look
        JPanel headerPanel = getHeaderPanel();

        // Icon and title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.HEAVY_PLUS);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(UnicodeSymbols.CHECKMARK + " Neuen Artikel hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = createDialogCloseButton(dialog, resultHolder);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the close button for the dialog header with modern styling
     */
    public static JButton createDialogCloseButton(JDialog dialog, Object[][] resultHolder) {
        JButton closeBtn = new JButton("X");
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));

        // Hover effect
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 100)); // Red tint on hover
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
            resultHolder[0] = null;
            dialog.dispose();
        });
        return closeBtn;
    }

    /**
     * Creates default GridBagConstraints for dialog layout
     */
    private static GridBagConstraints createDefaultGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        return gbc;
    }

    /**
     * Creates all form fields for the article dialog with modern card design
     */
    public static ArticleFormFields createArticleFormFields() {
        ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = createDefaultGridBagConstraints();
        int row = 0;

        // Create form fields
        JTextField nummerField = addLabeledTextField(contentCard, gbc, row, "Artikelnummer *");
        row += 2;

        JTextField nameField = addLabeledTextField(contentCard, gbc, row, "Name *");
        row += 2;

        JTextField detailsField = addLabeledTextField(contentCard, gbc, row, "Details");
        row += 2;

        // Stock spinners
        gbc.gridy = row++;
        JPanel stockPanel = createStockSpinnersPanel();
        contentCard.add(stockPanel, gbc);
        JSpinner lagerSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(0)).getComponent(1);
        JSpinner mindestSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(1)).getComponent(1);

        // Price fields
        gbc.gridy = row++;
        JPanel pricePanel = createPriceFieldsPanel();
        contentCard.add(pricePanel, gbc);
        JFormattedTextField verkaufField = findFormattedField((JPanel) pricePanel.getComponent(0));
        JFormattedTextField einkaufField = findFormattedField((JPanel) pricePanel.getComponent(1));
        if (verkaufField == null) {
            verkaufField = new JFormattedTextField(createPriceFormatter());
            verkaufField.setColumns(10);
        }
        if (einkaufField == null) {
            einkaufField = new JFormattedTextField(createPriceFormatter());
            einkaufField.setColumns(10);
        }

        // Supplier field
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridy = row++;
        contentCard.add(createDialogLabel(UnicodeSymbols.TRUCK + " Lieferant"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 6, 6);
        JTextField lieferantField = createDialogTextField();
        contentCard.add(lieferantField, gbc);

        return new ArticleFormFields(contentCard, nummerField, nameField, detailsField,
                lagerSpinner, mindestSpinner, verkaufField, einkaufField, lieferantField);
    }

    /**
     * Adds a labeled text field to the content panel
     */
    private static JTextField addLabeledTextField(JPanel panel, GridBagConstraints gbc, int row, String labelText) {
        gbc.gridy = row;
        panel.add(createDialogLabel(labelText), gbc);
        gbc.gridy = row + 1;
        JTextField textField = createDialogTextField();
        panel.add(textField, gbc);
        return textField;
    }

    /**
     * Creates a styled label for dialog forms with blue accent for required fields
     */
    private static JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

        // Blue accent for required fields (marked with *)
        if (text.contains("*")) {
            Color blueAccent = ThemeManager.getAccentColor();

            // Split text to style the asterisk differently
            String labelText = text.replace("*", "").trim();
            label.setText("<html>" + labelText + " <span style='color: rgb(" +
                    blueAccent.getRed() + "," + blueAccent.getGreen() + "," + blueAccent.getBlue() +
                    ");'>*</span></html>");
        }

        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    /**
     * Creates a styled text field for dialog forms with blue focus effect
     */
    private static JTextField createDialogTextField() {
        JTextField field = new JTextField(25);
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

        // Blue focus effect
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
     * Creates the panel with stock quantity spinners
     */
    private static JPanel createStockSpinnersPanel() {
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        stockPanel.setOpaque(false);

        stockPanel.add(createLabeledSpinnerPanel("Lagerbestand"));
        stockPanel.add(createLabeledSpinnerPanel("Mindestbestand"));

        return stockPanel;
    }

    /**
     * Creates a panel with label and spinner
     */
    private static JPanel createLabeledSpinnerPanel(String labelText) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        spinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(8);
        styleDialogSpinner(spinner);
        panel.add(spinner, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Styles a spinner for dialog forms with blue focus effect
     */
    private static void styleDialogSpinner(JSpinner spinner) {
        spinner.setBackground(ThemeManager.getInputBackgroundColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField textField = defaultEditor.getTextField();
            textField.setBackground(ThemeManager.getInputBackgroundColor());
            textField.setForeground(ThemeManager.getTextPrimaryColor());
            textField.setCaretColor(ThemeManager.getAccentColor());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(normalBorder, 1, true),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            // Blue focus effect
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(focusBorder, 2, true),
                            BorderFactory.createEmptyBorder(5, 7, 5, 7)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(normalBorder, 1, true),
                            BorderFactory.createEmptyBorder(6, 8, 6, 8)
                    ));
                }
            });
        }
    }

    /**
     * Helper to recursively find a JFormattedTextField in a container (for price field extraction)
     */
    private static JFormattedTextField findFormattedField(Container root) {
        if (root == null) return null;
        for (Component c : root.getComponents()) {
            if (c instanceof JFormattedTextField f) {
                return f;
            }
            if (c instanceof Container child) {
                JFormattedTextField nested = findFormattedField(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    /**
     * Creates the panel with price input fields
     */
    private static JPanel createPriceFieldsPanel() {
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        pricePanel.setOpaque(false);

        NumberFormatter priceFormatter = createPriceFormatter();

        pricePanel.add(createLabeledPriceField("Verkaufspreis (CHF)", priceFormatter));
        pricePanel.add(createLabeledPriceField("Einkaufspreis (CHF)", priceFormatter));

        return pricePanel;
    }

    /**
     * Creates a number formatter for price fields
     */
    private static NumberFormatter createPriceFormatter() {
        NumberFormat priceFormat = NumberFormat.getNumberInstance();
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);

        NumberFormatter priceFormatter = new NumberFormatter(priceFormat);
        priceFormatter.setValueClass(Double.class);
        priceFormatter.setAllowsInvalid(false);
        priceFormatter.setMinimum(0.0);

        return priceFormatter;
    }

    /**
     * Creates a panel with label and price field with blue focus effect
     */
    private static JPanel createLabeledPriceField(String labelText, NumberFormatter formatter) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(10);
        field.setValue(0.0);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        // Blue focus effect
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

        panel.add(field, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the button panel for the dialog
     */
    private static DialogButtons createDialogButtons(JDialog dialog, Object[][] resultHolder, ArticleFormFields formFields) {
        ArticleGUI.RoundedPanel buttonPanel = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JButton cancelBtn = createDialogDangerActionButton(UnicodeSymbols.CLOSE + " Abbrechen");
        JButton okBtn = createDialogPrimaryActionButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> handleAddArticleConfirm(dialog, resultHolder, formFields));

        return new DialogButtons(buttonPanel, okBtn);
    }

    /**
     * Creates the cancel button with modern styling and hover effects
     */
    private static JButton createDialogCancelButton(JDialog dialog, Object[][] resultHolder) {
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + " Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(ThemeManager.getTextPrimaryColor());

        Color baseColor = ThemeManager.getSurfaceColor();
        Color hoverColor = ThemeManager.getButtonHoverColor(baseColor);

        cancelBtn.setBackground(baseColor);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(baseColor);
            }
        });

        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        return cancelBtn;
    }

    /**
     * Creates the OK button with modern blue styling and smooth hover effects
     */
    private static JButton createDialogOkButton(JDialog dialog, Object[][] resultHolder, ArticleFormFields formFields) {
        JButton okBtn = new JButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);

        // Blue gradient colors
        Color baseColor = ThemeManager.getButtonBackgroundColor();
        Color hoverColor = ThemeManager.getButtonHoverColor(baseColor);
        Color pressedColor = ThemeManager.getButtonPressedColor(baseColor);

        okBtn.setBackground(baseColor);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Smooth hover effects
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(hoverColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(baseColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(baseColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                okBtn.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                okBtn.setBackground(okBtn.contains(e.getPoint()) ? hoverColor : baseColor);
            }
        });

        okBtn.addActionListener(ae -> handleAddArticleConfirm(dialog, resultHolder, formFields));
        return okBtn;
    }

    private static void handleAddArticleConfirm(JDialog dialog, Object[][] resultHolder, ArticleFormFields fields) {
        String nummer = fields.nummerField().getText().trim();
        String name = fields.nameField().getText().trim();
        String details = fields.detailsField().getText().trim();
        String lieferant = fields.lieferantField().getText().trim();

        if (nummer.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(dialog,
                    "Artikelnummer und Name sind Pflichtfelder.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        int lagerbestand = ((Number) fields.lagerSpinner().getValue()).intValue();
        int mindestbestand = ((Number) fields.mindestSpinner().getValue()).intValue();

        double verkaufspreis;
        double einkaufspreis;
        try {
            fields.verkaufField().commitEdit();
            fields.einkaufField().commitEdit();
            verkaufspreis = ((Number) fields.verkaufField().getValue()).doubleValue();
            einkaufspreis = ((Number) fields.einkaufField().getValue()).doubleValue();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog,
                    "Bitte gültige Preise eingeben.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                verkaufspreis, einkaufspreis, lieferant};
        dialog.dispose();
    }


    /**
     * Shows the dialog and sets initial focus
     */
    private static void showDialog(JFrame frame, JDialog dialog, JTextField initialFocusField) {
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(initialFocusField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    /**
     * Record to hold dialog component references
     */
    private record ArticleDialogComponents(
            JTextField nummerField,
            JTextField nameField,
            JTextField detailsField,
            JSpinner lagerSpinner,
            JSpinner mindestSpinner,
            JFormattedTextField verkaufField,
            JFormattedTextField einkaufField,
            JTextField lieferantField
    ) {
    }

    /**
     * Record to hold form fields during creation
     */
    private record ArticleFormFields(
            JPanel contentCard,
            JTextField nummerField,
            JTextField nameField,
            JTextField detailsField,
            JSpinner lagerSpinner,
            JSpinner mindestSpinner,
            JFormattedTextField verkaufField,
            JFormattedTextField einkaufField,
            JTextField lieferantField
    ) {
    }

    /**
     * Record to hold dialog buttons
     */
    private record DialogButtons(
            JPanel buttonPanel,
            JButton okButton
    ) {
    }
}
