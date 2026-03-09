package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.LogManager;
import ch.framedev.lagersystem.managers.NotesManager;
import ch.framedev.lagersystem.classes.Note;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Notes manager window for viewing, creating, and updating personal notes.
 * Provides a split list/detail layout, toolbar actions, and themed dialogs.
 * Dialogs use rounded card styling with a header strip to match the app theme.
 */
@SuppressWarnings("DuplicatedCode")
public class NotesGUI extends JFrame {
    
    /**
     * Creates a reusable form panel for note dialogs (create/update).
     * 
     * @param titleField  optional title field (null for update)
     * @param contentArea content area (required)
     * @return JPanel containing the form
     */
    private JPanel buildNoteDialogForm(JTextField titleField, JTextArea contentArea) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 0);
        int row = 0;
        if (titleField != null) {
            gbc.gridy = row++;
            form.add(createDialogLabel("Titel:"), gbc);
            gbc.gridy = row++;
            gbc.insets = new Insets(0, 0, 12, 0);
            form.add(titleField, gbc);
            gbc.insets = new Insets(0, 0, 6, 0);
        }
        gbc.gridy = row++;
        form.add(createDialogLabel("Inhalt:"), gbc);
        gbc.gridy = row++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(createDialogScroll(contentArea), gbc);
        return form;
    }

    // NotesManager access to Database
    private final NotesManager notesManager = NotesManager.getInstance();

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> notesList = new JList<>(listModel);
    private final JTextArea noteContentArea = new JTextArea(20, 30);
    private final JTextField searchField = new JTextField();
    private final JLabel statusLabel = new JLabel();
    private List<String> allTitles = List.of();
    private float fontScaleDelta = 0f;
    private static final float MIN_FONT_SIZE = 10f;
    private static final float MAX_FONT_SIZE = 30f;

    /**
     * Initializes the NotesGUI window, sets up the layout, components, and event
     * handlers.
     */
    public NotesGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle(UnicodeSymbols.MEMO + " Notizen");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1060, 860);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        // Top: header and search bar in a card with shadow and more roundness
        JPanel topCard = new ShadowRoundedPanel(ThemeManager.getCardBackgroundColor(), 28, 12);
        topCard.setLayout(new BorderLayout(0, 0));
        topCard.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JPanel headerPanel = createHeaderPanel();
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel searchBar = createSearchBar();
        searchBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel topPanel = new JPanel();
        topPanel.setOpaque(false);
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(headerPanel);
        topPanel.add(Box.createVerticalStrut(16));
        topPanel.add(searchBar);

        topCard.add(topPanel, BorderLayout.CENTER);
        root.add(topCard, BorderLayout.NORTH);

        // Center: content split in a card with shadow and more roundness
        JPanel contentCard = new ShadowRoundedPanel(ThemeManager.getCardBackgroundColor(), 28, 16);
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        contentCard.add(createContentPanel(), BorderLayout.CENTER);
        root.add(contentCard, BorderLayout.CENTER);

        // Bottom: toolbar in a card with shadow and more roundness
        JPanel bottomCard = new ShadowRoundedPanel(ThemeManager.getCardBackgroundColor(), 28, 10);
        bottomCard.setLayout(new BorderLayout());
        bottomCard.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        bottomCard.add(createFooterToolbar(), BorderLayout.CENTER);
        root.add(bottomCard, BorderLayout.SOUTH);

        setContentPane(root);
        setupList();
        setupKeyBindings();
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.getHeaderBackgroundColor());
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JLabel title = new JLabel(UnicodeSymbols.MEMO + " Notizen");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        title.setForeground(ThemeManager.getTextOnPrimaryColor());

        JLabel subtitle = new JLabel(UnicodeSymbols.INFO + " Persönliche Notizen verwalten und durchsuchen");
        subtitle.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitle.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(10));
        titleBox.add(subtitle);

        header.add(titleBox, BorderLayout.WEST);
        header.add(createToolbar(), BorderLayout.EAST);
        return header;
    }

    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);

        JButton createNoteButton = new JButton(UnicodeSymbols.HEAVY_PLUS + " Notiz erstellen");
        createNoteButton.addActionListener(e -> createNoteDialog());

        JButton updateDialogButton = new JButton(UnicodeSymbols.EDIT + " Notiz bearbeiten");
        updateDialogButton.addActionListener(e -> openUpdateDialog());

        JButton deleteDialogButton = new JButton(UnicodeSymbols.TRASH + " Notiz löschen");
        deleteDialogButton.addActionListener(e -> deleteNote());

        JButton refreshButton = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.addActionListener(e -> setupList());

        for (JButton button : new JButton[] { createNoteButton, updateDialogButton, deleteDialogButton,
                refreshButton }) {
            styleToolbarButton(button);
            toolbar.add(button);
        }

        return toolbar;
    }

    private JPanel createFooterToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);
        toolBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        statusLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        statusLabel.setForeground(ThemeManager.getTextSecondaryColor());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(statusLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton fontSizeBiggerButton = new JButton(UnicodeSymbols.PLUS + " Zoom");
        fontSizeBiggerButton.setToolTipText("Schriftgröße erhöhen");
        fontSizeBiggerButton.addActionListener(listener -> bumpFontSize(2f));

        JButton fontSizeSmallerButton = new JButton(UnicodeSymbols.MINUS + " Zoom");
        fontSizeSmallerButton.setToolTipText("Schriftgröße verringern");
        fontSizeSmallerButton.addActionListener(listener -> bumpFontSize(-2f));

        for (JButton button : new JButton[] { fontSizeSmallerButton, fontSizeBiggerButton }) {
            styleToolbarButton(button);
            right.add(button);
        }

        toolBar.add(left, BorderLayout.WEST);
        toolBar.add(right, BorderLayout.EAST);
        return toolBar;
    }

    private JPanel createContentPanel() {
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

        notesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        notesList.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        notesList.setBackground(ThemeManager.getCardBackgroundColor());
        notesList.setForeground(ThemeManager.getTextPrimaryColor());
        notesList.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        notesList.setSelectionForeground(ThemeManager.getSelectionForegroundColor());

        // Selection for Selected Note
        notesList.addListSelectionListener(e -> {
            String selectedTitle = notesList.getSelectedValue();
            if (selectedTitle == null)
                return;
            Note note = notesManager.getNoteByTitle(selectedTitle);
            if (note == null)
                return;
            String contentText = "Titel: " + note.getTitle() + "\n" +
                    "Datum: " + note.getDate() + "\n\n" +
                    note.getContent();
            noteContentArea.setText(contentText);
            noteContentArea.setCaretPosition(0);
            updateStatus();
        });

        noteContentArea.setEditable(false);
        noteContentArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        noteContentArea.setBackground(ThemeManager.getInputBackgroundColor());
        noteContentArea.setForeground(ThemeManager.getTextPrimaryColor());
        noteContentArea.setMargin(new Insets(10, 12, 10, 12));
        noteContentArea.setLineWrap(true);
        noteContentArea.setWrapStyleWord(true);

        // Scroll panes for list and content, wrapped in rounded cards
        JScrollPane listScroll = new JScrollPane(notesList);
        listScroll.setBorder(BorderFactory.createEmptyBorder());
        listScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        JScrollPane contentScroll = new JScrollPane(noteContentArea);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.getViewport().setBackground(ThemeManager.getInputBackgroundColor());
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);

        RoundedPanel listCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        listCard.add(listScroll, BorderLayout.CENTER);

        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        contentCard.setLayout(new BorderLayout());
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        contentCard.add(contentScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listCard, contentCard);
        splitPane.setDividerLocation(260);
        splitPane.setDividerSize(8);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setOpaque(false);

        content.add(splitPane, BorderLayout.CENTER);
        return content;
    }

    private void styleToolbarButton(JButton button) {
        if (button == null) return;
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        JFrameUtils.applyButtonPalette(button, ThemeManager.getAccentColor());
    }

    /**
     * Opens a dialog to update the content of the selected note.
     * <p>
     * This method performs the following steps:
     * 1. Retrieves the title of the currently selected note from the list.
     * If no note is selected, a warning message is displayed to prompt the user to
     * make a selection.
     * 2. Uses the retrieved title to fetch the note from the notes manager.
     * If the note is not found, the method exits without further action.
     * 3. Creates and displays a dialog prepopulated with the note's current content
     * to allow the user to edit the note.
     * 4. Handles the save action within the dialog:
     * a. Validates that the updated content is not empty.
     * Displays an error message if validation fails.
     * b. Attempts to save the changes through the notes manager.
     * If the update is successful, closes the dialog and refreshes the notes list.
     * If the update fails (e.g., due to duplicate titles), displays an error
     * message.
     * <p>
     * The dialog consists of a text area for editing the content and a save button.
     * The layout and components are dynamically set up within this method.
     * <p>
     * Note:
     * - This method interacts with several helper methods and components for
     * creating the dialog UI.
     * - The notes manager is used for fetching and updating the note.
     * - The notes list is refreshed after a successful update.
     */
    private void openUpdateDialog() {
        String selectedTitle = notesList.getSelectedValue();
        if (selectedTitle == null) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Notiz zum Bearbeiten aus.",
                    "Keine Notiz ausgewählt", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Note note = notesManager.getNoteByTitle(selectedTitle);
        if (note == null) {
            return;
        }

        JDialog dialog = createNoteDialogShell(UnicodeSymbols.EDIT + " Notiz aktualisieren");
        JTextArea contentArea = createDialogTextArea(note.getContent());
        JButton saveButton = createDialogButton(UnicodeSymbols.CHECKMARK + " Speichern",
                ThemeManager.getSuccessColor());
        saveButton.addActionListener(ev -> {
            String newContent = contentArea.getText().trim();
            if (newContent.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Titel und Inhalt dürfen nicht leer sein.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean success = notesManager.updateNote(note.getTitle(), newContent);
            if (success) {
                LogManager.getInstance().createLog(LogManager.LogLevel.INFO,
                        "Notiz '" + note.getTitle() + "' aktualisiert.");
                dialog.dispose();
                setupList();
            } else {
                JOptionPane.showMessageDialog(dialog, "Notiz mit diesem Titel existiert bereits.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        JPanel body = getDialogBodyPanel(dialog);
        body.add(buildNoteDialogForm(null, contentArea), BorderLayout.CENTER);
        body.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Deletes the currently selected note from the notes list.
     * <p>
     * This method performs the following steps:
     * 1. Retrieves the title of the selected note from the list. If no note is
     * selected,
     * a warning message is displayed to prompt the user to make a selection.
     * 2. Confirms with the user whether they want to delete the selected note
     * through a confirmation dialog.
     * 3. If the user confirms deletion, attempts to delete the note using the notes
     * manager.
     * - If the deletion is successful:
     * - Updates the notes list to reflect the change.
     * - Clears the content area of the note editor.
     * - If the deletion fails, displays an error message to indicate the failure.
     */
    private void deleteNote() {
        String selectedTitle = notesList.getSelectedValue();
        if (selectedTitle == null) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Notiz zum Löschen aus.",
                    "Keine Notiz ausgewählt", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Möchten Sie die Notiz \"" + selectedTitle + "\" wirklich löschen?",
                "Notiz löschen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            boolean success = notesManager.deleteNote(selectedTitle);
            if (success) {
                setupList();
                noteContentArea.setText("");
                LogManager.getInstance().createLog(LogManager.LogLevel.INFO,
                        "Notiz '" + selectedTitle + "' geloescht.");
            } else {
                JOptionPane.showMessageDialog(this, "Fehler beim Löschen der Notiz.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Opens a dialog to create a new note.
     * <p>
     * This method initializes and displays a modal dialog for creating a new note
     * with the
     * following UI components:
     * 1. A text field for entering the note's title.
     * 2. A text area for entering the note's content.
     * 3. A save button to confirm the creation of the note.
     * <p>
     * Detailed Behavior:
     * - Validates that both the title and content fields are not empty before
     * allowing the note to be created.
     * If either field is empty, an error message is displayed and the creation
     * process is halted.
     * - Attempts to add the note to the underlying notes manager. On success:
     * a. Closes the dialog.
     * b. Refreshes the notes list displayed in the GUI.
     * On failure (e.g., duplicate title), displays an error message indicating the
     * issue.
     * <p>
     * Error Handling:
     * If the title or content is empty, or a note with the same title already
     * exists, appropriate
     * error dialogs are shown to the user.
     * <p>
     * Layout Structure:
     * - The UI layout is dynamically constructed using GridBagLayout for proper
     * alignment.
     * - The main dialog body includes labels, input fields, and a scrollable text
     * area.
     * - Button panel includes the save button for submitting the note and is
     * displayed
     * at the bottom of the dialog.
     * <p>
     * Dependencies:
     * - Relies on helper methods such as `createNoteDialogShell`,
     * `createDialogTextField`,
     * `createDialogTextArea`, and `createDialogButton` for constructing individual
     * dialog components.
     * - Uses `notesManager` for managing the notes and `setupList` for refreshing
     * the GUI.
     * <p>
     * Post-condition:
     * - If the note is successfully added, the dialog is closed and the notes list
     * is updated.
     * - If the note creation fails or is canceled, no changes are made to the list.
     */
    private void createNoteDialog() {
        JDialog dialog = createNoteDialogShell(UnicodeSymbols.HEAVY_PLUS + " Notiz erstellen");
        JTextField titleField = createDialogTextField();
        titleField.setPreferredSize(new Dimension(0, 32));
        titleField.setMinimumSize(new Dimension(0, 32));
        JTextArea contentArea = createDialogTextArea("");
        JButton saveButton = createDialogButton(UnicodeSymbols.CHECKMARK + " Speichern",
                ThemeManager.getSuccessColor());

        saveButton.addActionListener(e -> {
            String title = titleField.getText().trim();
            String content = contentArea.getText().trim();
            if (title.isEmpty() || content.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Titel und Inhalt dürfen nicht leer sein.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean success = notesManager.addNote(title, content);
            if (success) {
                dialog.dispose();
                setupList();
                LogManager.getInstance().createLog(LogManager.LogLevel.INFO, "Notiz '" + title + "' erstellt.");
            } else {
                JOptionPane.showMessageDialog(dialog, "Notiz mit diesem Titel existiert bereits.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel body = getDialogBodyPanel(dialog);
        body.add(buildNoteDialogForm(titleField, contentArea), BorderLayout.CENTER);
        body.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * Creates and returns a modal dialog shell for displaying or editing a note.
     * The dialog has a customizable title, a structured layout, and theme-compliant
     * styling.
     * It includes a header with a close button and a body panel for additional
     * components or content.
     *
     * @param title the title of the dialog, typically the name of the note.
     * @return a configured JDialog instance with the specified title and default
     *         layout.
     */
    private JDialog createNoteDialogShell(String title) {
        if (title == null)
            throw new IllegalArgumentException("Title cannot be null");
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(480, 540);
        dialog.setMinimumSize(new Dimension(440, 440));
        dialog.setLayout(new BorderLayout());
        dialog.setLocationRelativeTo(this);
        dialog.setUndecorated(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Card wrapper to match other dialogs
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        // Header strip
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ThemeManager.getHeaderBackgroundColor());
        header.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel headerTitle = new JLabel(title);
        headerTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        headerTitle.setForeground(ThemeManager.getTextOnPrimaryColor());
        header.add(headerTitle, BorderLayout.WEST);

        JButton close = new JButton(UnicodeSymbols.CLOSE);
        close.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        close.setForeground(ThemeManager.getTextOnPrimaryColor());
        close.setContentAreaFilled(false);
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dialog.dispose());
        header.add(close, BorderLayout.EAST);

        // Body card
        RoundedPanel body = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        body.setLayout(new BorderLayout(10, 10));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        root.add(card, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.getRootPane().putClientProperty("notes.dialog.body", body);
        if (fontScaleDelta != 0f) {
            applyFontDelta(dialog.getContentPane(), fontScaleDelta);
        }
        return dialog;
    }

    /**
     * Retrieves the body panel of a specified dialog, if available, falling back to
     * the dialog's content pane if no specific body panel is set.
     * <p>
     * The method checks for a custom body panel associated with the dialog via the
     * "notes.dialog.body" client property.
     * If such a property exists and is of type JPanel, it returns that panel.
     * Otherwise, it defaults to returning the
     * content pane of the dialog.
     *
     * @param dialog the JDialog instance from which the body panel is to be
     *               retrieved
     * @return the JPanel representing the body of the dialog, or the dialog's
     *         content pane if no custom body panel is set
     */
    private JPanel getDialogBodyPanel(JDialog dialog) {
        if (dialog == null)
            throw new IllegalArgumentException("Dialog cannot be null");
        Object body = dialog.getRootPane().getClientProperty("notes.dialog.body");
        return body instanceof JPanel panel ? panel : (JPanel) dialog.getContentPane();
    }

    /**
     * Creates and returns a styled {@code JLabel} to be used within dialog
     * components.
     * The label is initialized with the given text and styled according to the
     * application's
     * font and theme settings.
     *
     * @param text the text to be displayed on the label
     * @return a configured {@code JLabel} instance with the specified text and
     *         styling
     */
    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    /**
     * Creates and returns a styled {@code JTextField} for use within dialog
     * components.
     * <p>
     * The text field is configured with the following properties:
     * - Font: Set to a predefined font with plain style and size 12.
     * - Background color: Matches the input background color from the theme
     * manager.
     * - Foreground color: Matches the primary text color from the theme manager.
     * - Caret color: Matches the primary text color from the theme manager.
     * - Border: A combination of a line border using the input border color from
     * the theme manager
     * and an empty border for padding.
     * <p>
     * This text field is ready for immediate integration in themed dialog
     * interfaces.
     *
     * @return a {@code JTextField} with custom styling and theme-compliant
     *         configurations
     */
    private JTextField createDialogTextField() {
        JTextField field = new JTextField();
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return field;
    }

    /**
     * Creates and configures a JTextArea for use in a dialog. The JTextArea is
     * initialized
     * with the specified content and styled using the application's theme and font
     * settings.
     *
     * @param content the initial text to be displayed in the JTextArea
     * @return a configured JTextArea instance with specified text and appearance
     *         settings
     */
    private JTextArea createDialogTextArea(String content) {
        if (content == null)
            throw new IllegalArgumentException("Content cannot be null");
        JTextArea area = new JTextArea(content);
        area.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        area.setBackground(ThemeManager.getInputBackgroundColor());
        area.setForeground(ThemeManager.getTextPrimaryColor());
        area.setCaretColor(ThemeManager.getTextPrimaryColor());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    /**
     * Creates a JScrollPane configured for the given JTextArea with customized
     * appearance and behavior.
     *
     * @param area the JTextArea to wrap within the JScrollPane
     * @return a JScrollPane containing the specified JTextArea with vertical
     *         scrolling enabled and horizontal scrolling disabled
     */
    private JScrollPane createDialogScroll(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.getViewport().setBackground(ThemeManager.getInputBackgroundColor());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    /**
     * Creates and configures a customizable dialog button with the specified text
     * and background color.
     *
     * @param text the text to display on the button.
     * @param bg   the background color of the button.
     * @return a fully configured {@code JButton} with the specified properties.
     */
    private JButton createDialogButton(String text, Color bg) {
        if (text == null)
            throw new IllegalArgumentException("Text cannot be null");
        JButton button = new JButton(text);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        JFrameUtils.applyButtonPalette(button, bg);
        return button;
    }

    /**
     * Creates a panel containing dialog buttons, including a primary button and a
     * cancel button.
     * The panel is styled with a right-aligned layout and an appropriate border.
     *
     * @param primaryButton the primary action button to be added to the panel
     * @param dialog        the dialog that the cancel button will close when
     *                      clicked
     * @return a JPanel containing the styled button panel
     */
    private JPanel createDialogButtonPanel(JButton primaryButton, JDialog dialog) {
        if (primaryButton == null)
            throw new IllegalArgumentException("Primary button cannot be null");
        if (dialog == null)
            throw new IllegalArgumentException("Dialog cannot be null");
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton cancel = createDialogButton(UnicodeSymbols.CLOSE + " Abbrechen", ThemeManager.getErrorColor());
        cancel.addActionListener(e -> dialog.dispose());

        panel.add(cancel);
        panel.add(primaryButton);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(10, 0, 0, 0)));
        wrapper.add(panel, BorderLayout.EAST);
        return wrapper;
    }

    /**
     * Configures and populates the list model with note titles.
     * <p>
     * Caches all titles, applies search filter, updates status label, and shows
     * placeholder text if needed.
     */
    private void setupList() {
        allTitles = notesManager.getAllNotes().stream().map(Note::getTitle).toList();
        applySearchFilter();

        if (!listModel.isEmpty() && notesList.getSelectedIndex() == -1) {
            notesList.setSelectedIndex(0);
        }

        if (listModel.isEmpty()) {
            noteContentArea.setText("""
                    Keine Notizen vorhanden.

                    • Klicken Sie auf 'Notiz erstellen' um zu starten.
                    • Nutzen Sie die Suche, um Notizen schnell zu finden.""");
            noteContentArea.setCaretPosition(0);
        }

        updateStatus();
    }

    private JPanel createSearchBar() {
        JPanel wrap = new JPanel(new BorderLayout(10, 0));
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JLabel label = new JLabel(UnicodeSymbols.SEARCH + " Suchen:");
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        searchField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        searchField.setBackground(ThemeManager.getInputBackgroundColor());
        searchField.setForeground(ThemeManager.getTextPrimaryColor());
        searchField.setCaretColor(ThemeManager.getTextPrimaryColor());
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        searchField.setToolTipText("Titel durchsuchen (Strg/Cmd+F)");

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearchFilter();
            }
        });

        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.add(label, BorderLayout.WEST);
        card.add(searchField, BorderLayout.CENTER);

        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void applySearchFilter() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        listModel.clear();
        if (q.isEmpty()) {
            listModel.addAll(allTitles);
        } else {
            for (String t : allTitles) {
                if (t != null && t.toLowerCase().contains(q)) {
                    listModel.addElement(t);
                }
            }
        }

        if (!listModel.isEmpty() && notesList.getSelectedIndex() == -1) {
            notesList.setSelectedIndex(0);
        } else if (listModel.isEmpty()) {
            noteContentArea.setText(q.isEmpty()
                    ? "Keine Notizen vorhanden."
                    : "Keine Notizen für den aktuellen Filter gefunden.");
            noteContentArea.setCaretPosition(0);
        }
        updateStatus();
    }

    private void updateStatus() {
        int total = allTitles == null ? 0 : allTitles.size();
        int shown = listModel.getSize();
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        if (q.isEmpty()) {
            statusLabel.setText(shown + " Notiz(en)");
        } else {
            statusLabel.setText(shown + " von " + total + " • Filter: \"" + q + "\"");
        }
    }

    private void setupKeyBindings() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();

        // New note (Ctrl/Cmd+N)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "notes.new");
        am.put("notes.new", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                createNoteDialog();
            }
        });

        // Focus search (Ctrl/Cmd+F)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "notes.find");
        am.put("notes.find", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        // Delete selected note (Delete)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "notes.delete");
        am.put("notes.delete", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deleteNote();
            }
        });

        // Refresh (F5)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "notes.refresh");
        am.put("notes.refresh", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                setupList();
            }
        });
    }

    /**
     * Adjusts the font size of the UI components by a specified delta value.
     * This method modifies the scaling factor for fonts and applies the change
     * recursively to all components within the content pane. It then triggers
     * a revalidation and repaint of the UI to reflect the updated font sizes.
     *
     * @param delta the amount to adjust the font size scaling factor. Positive
     *              values
     *              increase the font size, while negative values decrease it.
     */
    private void bumpFontSize(float delta) {
        fontScaleDelta += delta;
        applyFontDelta(getContentPane(), delta);
        revalidate();
        repaint();
        updateStatus();
    }

    /**
     * Adjusts the font size of the specified component and all its child components
     * by applying the given delta value.
     *
     * @param component the component whose font size is to be adjusted
     * @param delta     the value to adjust the font size by; positive values
     *                  increase the size,
     *                  while negative values decrease the size
     */
    private void applyFontDelta(Component component, float delta) {
        if (component == null)
            throw new IllegalArgumentException("Component cannot be null");
        Font font = component.getFont();
        if (font != null) {
            float newSize = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, font.getSize2D() + delta));
            component.setFont(font.deriveFont(newSize));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyFontDelta(child, delta);
            }
        }
    }

    /**
     * Makes the current component visible by setting its visibility state to true.
     * This method ensures that the component is displayed on the screen.
     */
    public void display() {
        setVisible(true);
    }

    /**
     * A custom JPanel with rounded corners and customizable background color.
     * This panel allows rendering with anti-aliasing for smoother edges.
     * <p>
     * The corner radius and background color can be specified during the
     * instantiation of the panel. The panel is non-opaque by default to ensure
     * the rounded corners are displayed properly.
     * <p>
     * Overrides the {@code paintComponent} method to implement custom rendering
     * behavior with rounded corners.
     */
    private static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;
        RoundedPanel(Color bg, int radius) {
            this.backgroundColor = bg;
            this.radius = radius;
            setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintRoundedBackground(g2, backgroundColor, 0, 0, getWidth(), getHeight(), radius);
            g2.dispose();
            super.paintComponent(g);
        }

        protected static void paintRoundedBackground(Graphics2D g2, Color color,
                                                     int x, int y, int width, int height, int arc) {
            if (width <= 0 || height <= 0 || color == null) {
                return;
            }
            g2.setColor(color);
            g2.fillRoundRect(x, y, width, height, arc, arc);
        }
    }

    // Adds a drop shadow to the rounded panel for a modern, elevated look
    private static class ShadowRoundedPanel extends RoundedPanel {
        private final int shadowSize;
        private final int localRadius;
        private final Color localBackground;
        ShadowRoundedPanel(Color bg, int radius, int shadowSize) {
            super(bg, radius + 4); // slightly rounder
            this.shadowSize = shadowSize;
            this.localRadius = radius + 4;
            this.localBackground = bg;
            setBorder(BorderFactory.createEmptyBorder(0, 0, shadowSize, shadowSize));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int r = localRadius;
            // Draw soft shadow
            for (int i = shadowSize; i > 0; i--) {
                int alpha = (int)(18.0 * i/shadowSize);
                g2.setColor(new Color(0,0,0,alpha));
                g2.fillRoundRect(i, i, w-i*2, h-i*2, r, r);
            }
            // Draw subtle gradient background
            GradientPaint gp = new GradientPaint(0, 0, localBackground.brighter(), 0, h, localBackground.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w-shadowSize, h-shadowSize, r, r);
            g2.dispose();
        }
    }
}
