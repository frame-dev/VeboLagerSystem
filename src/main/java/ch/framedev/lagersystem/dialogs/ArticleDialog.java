package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;

import static ch.framedev.lagersystem.utils.ArticleUtils.getHeaderPanel;

/**
 * This class provides static methods to display dialogs for adding and updating articles. It includes methods to create and style dialog components such as headers, form fields, buttons, and scroll panes. The dialogs are designed with a modern look using gradients, shadows, and blue accents for required fields. The main methods are showUpdateArticleDialog() for editing existing articles and showAddArticleDialog() for creating new articles. Both methods return the entered data in a structured format or null if the dialog is canceled.
 */
@SuppressWarnings({"ReassignedVariable", "UnusedAssignment", "ClassEscapesDefinedScope"})
public class ArticleDialog {
    private static final Dimension DIALOG_DEFAULT_SIZE = new Dimension(720, 760);
    private static final Dimension DIALOG_MIN_SIZE = new Dimension(680, 620);

    /**
     * Private constructor to prevent instantiation since this class only contains static methods for displaying dialogs.
     */
    private ArticleDialog() {
    }

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
     * @return An array containing the updated data for the article:
     * [nummer, name, details, lagerbestand, mindestbestand, verkaufspreis, einkaufspreis, lieferant].
     * If the dialog is canceled, this method will return null.
     */
    public static Object[] showUpdateArticleDialog(JFrame frame, Object[] existingData) {
        if (frame == null) {
            throw new IllegalArgumentException("Frame must not be null");
        }
        return showArticleDialog(
                frame,
                "Artikel bearbeiten",
                UnicodeSymbols.EDIT,
                UnicodeSymbols.CHECKMARK + " Speichern",
                existingData
        );
    }

    private static Object[] showArticleDialog(JFrame frame, String title, String headerIcon, String okButtonText, Object[] existingData) {
        ThemeManager.applyUIDefaults();
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(frame, title, true);
        dialog.setUndecorated(true);

        ArticleDialogComponents components = createArticleDialogComponents(
                dialog,
                resultHolder,
                headerIcon,
                title,
                okButtonText,
                existingData
        );

        showDialog(frame, dialog, components.nummerField());
        return resultHolder[0];
    }

    // --- Deduplication helpers ---
    private static ArticleGUI.RoundedPanel createCardPanel(int radius, int borderPadding) {
        ArticleGUI.RoundedPanel card = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), radius);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(borderPadding, borderPadding, borderPadding, borderPadding)
        ));
        return card;
    }

    private static ArticleGUI.RoundedPanel getButtonPanel() {
        ArticleGUI.RoundedPanel buttonPanel = createCardPanel(12, 10);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        return buttonPanel;
    }

    private static JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        btn.setForeground(fgColor);
        Color hover = ThemeManager.getButtonHoverColor(bgColor);
        Color pressed = ThemeManager.getButtonPressedColor(bgColor);
        btn.setBackground(bgColor);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(true);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1, true),
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
                btn.setBackground(bgColor);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 24, 12, 24)
                ));
            }
            @Override
            public void mousePressed(MouseEvent e) {
                btn.setBackground(pressed);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                btn.setBackground(btn.contains(e.getPoint()) ? hover : bgColor);
            }
        });
        return btn;
    }

    private static JButton createDialogPrimaryActionButton(String text) {
        return createStyledButton(text, ThemeManager.getButtonBackgroundColor(), Color.WHITE);
    }

    private static JButton createDialogDangerActionButton(String text) {
        return createStyledButton(text, ThemeManager.getErrorColor(), Color.WHITE);
    }

    private static JScrollPane createDialogScrollPane(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
        if (frame == null) {
            throw new IllegalArgumentException("Frame must not be null");
        }
        return showArticleDialog(
                frame,
                "Neuen Artikel hinzufügen",
                UnicodeSymbols.HEAVY_PLUS,
                UnicodeSymbols.CHECKMARK + " Hinzufügen",
                null
        );
    }

    private static ArticleDialogComponents createArticleDialogComponents(
            JDialog dialog,
            Object[][] resultHolder,
            String headerIcon,
            String headerTitle,
            String okButtonText,
            Object[] existingData
    ) {
        JPanel mainContainer = createDialogMainContainer();

        // Header
        JPanel headerPanel = createDialogHeader(dialog, resultHolder, headerIcon, headerTitle);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content with form fields
        ArticleFormFields formFields = createArticleFormFields(existingData);
        mainContainer.add(createDialogScrollPane(formFields.contentCard()), BorderLayout.CENTER);

        // Buttons
        DialogButtons dialogButtons = createDialogButtons(dialog, resultHolder, formFields, okButtonText);
        mainContainer.add(dialogButtons.buttonPanel(), BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(dialogButtons.okButton());
        installDialogKeyboardShortcuts(dialog, resultHolder, dialogButtons.okButton());

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

    private static JPanel createDialogHeader(JDialog dialog, Object[][] resultHolder, String icon, String title) {
        // Create a gradient panel for modern look
        JPanel headerPanel = getHeaderPanel();

        // Icon and title panel
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

        JButton closeBtn = createDialogCloseButton(dialog, resultHolder);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        return headerPanel;
    }

    /**
     * Creates the close button for the dialog header with modern styling
     *
     * @param dialog       The dialog to close when the button is clicked
     * @param resultHolder A reference to hold the result (set to null on close) to signal cancellation to the caller
     * @return A styled JButton that closes the dialog when clicked
     */
    public static JButton createDialogCloseButton(JDialog dialog, Object[][] resultHolder) {
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schliesst den Dialog");
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

    private static void installDialogKeyboardShortcuts(JDialog dialog, Object[][] resultHolder, JButton okButton) {
        JRootPane rootPane = dialog.getRootPane();
        rootPane.registerKeyboardAction(
                e -> {
                    resultHolder[0] = null;
                    dialog.dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        rootPane.registerKeyboardAction(
                e -> okButton.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
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

    private static ArticleFormFields createArticleFormFields(Object[] existingData) {
        String existingNummer = getStringValue(existingData, 0);
        String existingName = getStringValue(existingData, 1);
        String existingDetails = getStringValue(existingData, 3);
        int existingLager = getIntValue(existingData, 4);
        int existingMindest = getIntValue(existingData, 5);
        double existingVerkauf = getDoubleValue(existingData, 6);
        double existingEinkauf = getDoubleValue(existingData, 7);
        String existingLieferant = getStringValue(existingData, 8);

        ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = createDefaultGridBagConstraints();
        int row = 0;

        gbc.insets = new Insets(2, 8, 14, 8);
        gbc.gridy = row++;
        contentCard.add(createDialogHintLabel("* Pflichtfelder  ·  Esc = Abbrechen  ·  Ctrl+Enter = Speichern"), gbc);

        gbc.insets = new Insets(8, 8, 8, 8);
        // Create form fields
        JTextField nummerField = addLabeledTextField(contentCard, gbc, row, UnicodeSymbols.NUMBERS + " Artikelnummer *", existingNummer);
        row += 2;

        JTextField nameField = addLabeledTextField(contentCard, gbc, row, UnicodeSymbols.PENCIL + " Name *", existingName);
        row += 2;

        JTextField detailsField = addLabeledTextField(contentCard, gbc, row, UnicodeSymbols.CLIPBOARD + " Details", existingDetails);
        row += 2;

        // Stock spinners
        gbc.insets = new Insets(10, 8, 6, 8);
        gbc.gridy = row++;
        JLabel stockSection = createDialogLabel(UnicodeSymbols.PACKAGE + " Lagerbestand");
        stockSection.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(stockSection, gbc);

        gbc.insets = new Insets(4, 8, 8, 8);
        gbc.gridy = row++;
        JPanel stockPanel = createStockSpinnersPanel(existingLager, existingMindest);
        contentCard.add(stockPanel, gbc);
        JSpinner lagerSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(0)).getComponent(1);
        JSpinner mindestSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(1)).getComponent(1);

        // Price fields
        gbc.insets = new Insets(10, 8, 6, 8);
        gbc.gridy = row++;
        JLabel priceSection = createDialogLabel(UnicodeSymbols.MONEY + " Preise");
        priceSection.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(priceSection, gbc);

        gbc.insets = new Insets(4, 8, 8, 8);
        gbc.gridy = row++;
        JPanel pricePanel = createPriceFieldsPanel(existingVerkauf, existingEinkauf);
        contentCard.add(pricePanel, gbc);
        JPanel verkaufPanel = (JPanel) pricePanel.getComponent(0);
        JPanel einkaufPanel = (JPanel) pricePanel.getComponent(1);

        JFormattedTextField verkaufField = findFormattedField(verkaufPanel);
        JFormattedTextField einkaufField = findFormattedField(einkaufPanel);

        if (verkaufField == null) {
            verkaufField = new JFormattedTextField(createPriceFormatter());
            verkaufField.setColumns(10);
            verkaufPanel.add(verkaufField, BorderLayout.CENTER);
        }
        verkaufField.setValue(existingVerkauf);
        if (einkaufField == null) {
            einkaufField = new JFormattedTextField(createPriceFormatter());
            einkaufField.setColumns(10);
            einkaufPanel.add(einkaufField, BorderLayout.CENTER);
        }
        einkaufField.setValue(existingEinkauf);

        // Supplier field
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createDialogLabel(UnicodeSymbols.TRUCK + " Lieferant"), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 6, 8);
        JTextField lieferantField = createDialogTextField();
        lieferantField.setText(existingLieferant);
        contentCard.add(lieferantField, gbc);

        return new ArticleFormFields(contentCard, nummerField, nameField, detailsField,
                lagerSpinner, mindestSpinner, verkaufField, einkaufField, lieferantField);
    }

    /**
     * Adds a labeled text field to the content panel
     */
    private static JTextField addLabeledTextField(JPanel panel, GridBagConstraints gbc, int row, String labelText, String initialValue) {
        gbc.gridy = row;
        panel.add(createDialogLabel(labelText), gbc);
        gbc.gridy = row + 1;
        gbc.insets = new Insets(2, 8, 14, 8);
        JTextField textField = createDialogTextField();
        textField.setText(initialValue == null ? "" : initialValue);
        panel.add(textField, gbc);
        gbc.insets = new Insets(8, 8, 8, 8);
        return textField;
    }

    /**
     * Creates a styled label for dialog forms with blue accent for required fields
     */
    private static JLabel createDialogLabel(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
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

    private static JLabel createDialogHintLabel(String text) {
        JLabel hintLabel = new JLabel(text);
        hintLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        hintLabel.setForeground(ThemeManager.getTextSecondaryColor());
        return hintLabel;
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

    private static JPanel createStockSpinnersPanel(int lagerbestand, int mindestbestand) {
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        stockPanel.setOpaque(false);

        stockPanel.add(createLabeledSpinnerPanel("Aktuell", lagerbestand));
        stockPanel.add(createLabeledSpinnerPanel("Mindestbestand", mindestbestand));

        return stockPanel;
    }

    /**
     * Creates a panel with label and spinner
     */
    private static JPanel createLabeledSpinnerPanel(String labelText, int initialValue) {
        if (labelText == null) {
            throw new IllegalArgumentException("Label text cannot be null");
        }
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(0, initialValue), 0, Integer.MAX_VALUE, 1));
        spinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(8);
        styleDialogSpinner(spinner);
        panel.add(spinner, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Styles a spinner for dialog forms with a blue focus effect
     */
    private static void styleDialogSpinner(JSpinner spinner) {
        if (spinner == null) {
            throw new IllegalArgumentException("Spinner cannot be null.");
        }
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
            switch (c) {
                case null -> {
                    continue;
                }
                case JFormattedTextField f -> {
                    return f;
                }
                case Container child -> {
                    JFormattedTextField nested = findFormattedField(child);
                    if (nested != null) return nested;
                }
                default -> {
                }
            }
        }
        return null;
    }

    private static JPanel createPriceFieldsPanel(double verkaufspreis, double einkaufspreis) {
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        pricePanel.setOpaque(false);

        NumberFormatter priceFormatter = createPriceFormatter();

        pricePanel.add(createLabeledPriceField("Verkaufspreis (CHF)", priceFormatter, verkaufspreis));
        pricePanel.add(createLabeledPriceField("Einkaufspreis (CHF)", priceFormatter, einkaufspreis));

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
    private static JPanel createLabeledPriceField(String labelText, NumberFormatter formatter, double initialValue) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(10);
        field.setValue(Math.max(0.0, initialValue));
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

    private static DialogButtons createDialogButtons(
            JDialog dialog,
            Object[][] resultHolder,
            ArticleFormFields formFields,
            String okButtonText
    ) {
        ArticleGUI.RoundedPanel buttonPanel = getButtonPanel();

        JButton cancelBtn = createDialogDangerActionButton(UnicodeSymbols.CLOSE + " Abbrechen");
        cancelBtn.setToolTipText("Dialog ohne Speichern schliessen");
        JButton okBtn = createDialogPrimaryActionButton(okButtonText);
        okBtn.setToolTipText("Artikeldaten übernehmen");

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> handleArticleConfirm(dialog, resultHolder, formFields));

        return new DialogButtons(buttonPanel, okBtn);
    }

    private static void handleArticleConfirm(JDialog dialog, Object[][] resultHolder, ArticleFormFields fields) {
        String nummer = fields.nummerField().getText().trim();
        String name = fields.nameField().getText().trim();
        String details = fields.detailsField().getText().trim();
        String lieferant = fields.lieferantField().getText().trim();

        if (nummer.isEmpty()) {
            showValidationError(dialog, "Artikelnummer ist ein Pflichtfeld.", fields.nummerField());
            return;
        }
        if (name.isEmpty()) {
            showValidationError(dialog, "Name ist ein Pflichtfeld.", fields.nameField());
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
            new MessageDialog()
                .setTitle("Fehler")
                .setMessage("Bitte gültige Preise eingeben.")
                .setMessageType(JOptionPane.ERROR_MESSAGE)
                .display();
            return;
        }

        resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                verkaufspreis, einkaufspreis, lieferant};
        dialog.dispose();
    }

    private static void showValidationError(JDialog dialog, String message, JTextField fieldToFocus) {
        new MessageDialog()
            .setTitle("Fehler")
            .setMessage(message)
            .setMessageType(JOptionPane.ERROR_MESSAGE)
            .display();
        fieldToFocus.requestFocusInWindow();
        fieldToFocus.selectAll();
    }

    private static String getStringValue(Object[] existingData, int index) {
        if (existingData == null || index < 0 || index >= existingData.length) {
            return "";
        }
        Object value = existingData[index];
        return value == null ? "" : value.toString().trim();
    }

    private static int getIntValue(Object[] existingData, int index) {
        if (existingData == null || index < 0 || index >= existingData.length) {
            return 0;
        }
        Object value = existingData[index];
        if (value instanceof Number number) {
            return Math.max(0, number.intValue());
        }
        try {
            return Math.max(0, Integer.parseInt(value == null ? "0" : value.toString().trim()));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static double getDoubleValue(Object[] existingData, int index) {
        if (existingData == null || index < 0 || index >= existingData.length) {
            return 0.0;
        }
        Object value = existingData[index];
        if (value instanceof Number number) {
            return Math.max(0.0, number.doubleValue());
        }
        try {
            return Math.max(0.0, Double.parseDouble(value == null ? "0" : value.toString().trim()));
        } catch (Exception ignored) {
            return 0.0;
        }
    }


    /**
     * Shows the dialog and sets the initial focus
     */
    private static void showDialog(JFrame frame, JDialog dialog, JTextField initialFocusField) {
        dialog.pack();
        dialog.setMinimumSize(DIALOG_MIN_SIZE);
        if (dialog.getWidth() < DIALOG_DEFAULT_SIZE.width || dialog.getHeight() < DIALOG_DEFAULT_SIZE.height) {
            dialog.setSize(
                    Math.max(dialog.getWidth(), DIALOG_DEFAULT_SIZE.width),
                    Math.max(dialog.getHeight(), DIALOG_DEFAULT_SIZE.height)
            );
        }
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
