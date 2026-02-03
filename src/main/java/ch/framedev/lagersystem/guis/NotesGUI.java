package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.NotesManager;
import ch.framedev.lagersystem.utils.Note;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;

public class NotesGUI extends JFrame {

    private final NotesManager notesManager = NotesManager.getInstance();

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> notesList = new JList<>(listModel);
    private final JTextArea noteContentArea = new JTextArea(20, 30);

    public NotesGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle(UnicodeSymbols.MEMO + " Notizen");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(780, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        root.add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel contentPanel = createContentPanel();
        JScrollPane contentScroll = new JScrollPane(contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        contentScroll.setBackground(ThemeManager.getBackgroundColor());
        contentScroll.getViewport().setBackground(ThemeManager.getBackgroundColor());
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(contentScroll, BorderLayout.CENTER);

        setContentPane(root);
        setupList();
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
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

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
        titleBox.add(Box.createVerticalStrut(2));
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

        JButton refreshButton = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.addActionListener(e -> setupList());

        for (JButton button : new JButton[]{createNoteButton, updateDialogButton, refreshButton}) {
            styleToolbarButton(button);
            toolbar.add(button);
        }

        return toolbar;
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

        notesList.addListSelectionListener(e -> {
            String selectedTitle = notesList.getSelectedValue();
            if (selectedTitle == null) return;
            Note note = notesManager.getNoteByTitle(selectedTitle);
            if (note == null) return;
            String contentText = "Titel: " + note.getTitle() + "\n" +
                    "Datum: " + note.getDate() + "\n\n" +
                    note.getContent();
            noteContentArea.setText(contentText);
            noteContentArea.setCaretPosition(0);
        });

        noteContentArea.setEditable(false);
        noteContentArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        noteContentArea.setBackground(ThemeManager.getInputBackgroundColor());
        noteContentArea.setForeground(ThemeManager.getTextPrimaryColor());
        noteContentArea.setMargin(new Insets(10, 12, 10, 12));
        noteContentArea.setLineWrap(true);
        noteContentArea.setWrapStyleWord(true);

        JScrollPane listScroll = new JScrollPane(notesList);
        listScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        listScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());

        JScrollPane contentScroll = new JScrollPane(noteContentArea);
        contentScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        contentScroll.getViewport().setBackground(ThemeManager.getInputBackgroundColor());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScroll, contentScroll);
        splitPane.setDividerLocation(250);
        splitPane.setDividerSize(6);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        content.add(splitPane, BorderLayout.CENTER);
        return content;
    }

    private JPanel createFooterSpacer() {
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        return spacer;
    }

    private void styleToolbarButton(JButton button) {
        Color base = ThemeManager.getAccentColor();
        Color hover = ThemeManager.getButtonHoverColor(base);

        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBackground(base);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addChangeListener(e -> {
            if (button.getModel().isRollover()) {
                button.setBackground(hover);
            } else {
                button.setBackground(base);
            }
        });
    }

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
        JButton saveButton = createDialogButton(UnicodeSymbols.CHECKMARK + " Speichern", ThemeManager.getSuccessColor());

        saveButton.addActionListener(ev -> {
            String newContent = contentArea.getText().trim();
            if (newContent.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Titel und Inhalt dürfen nicht leer sein.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            boolean success = notesManager.updateNote(note.getTitle(), newContent);
            if (success) {
                dialog.dispose();
                setupList();
            } else {
                JOptionPane.showMessageDialog(dialog, "Notiz mit diesem Titel existiert bereits.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(createDialogLabel("Inhalt:"), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(createDialogScroll(contentArea), gbc);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void createNoteDialog() {
        JDialog dialog = createNoteDialogShell(UnicodeSymbols.HEAVY_PLUS + " Notiz erstellen");
        JTextField titleField = createDialogTextField();
        titleField.setPreferredSize(new Dimension(0, 32));
        titleField.setMinimumSize(new Dimension(0, 32));
        JTextArea contentArea = createDialogTextArea("");
        JButton saveButton = createDialogButton(UnicodeSymbols.CHECKMARK + " Speichern", ThemeManager.getSuccessColor());

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
            } else {
                JOptionPane.showMessageDialog(dialog, "Notiz mit diesem Titel existiert bereits.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(createDialogLabel("Titel:"), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        form.add(titleField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(createDialogLabel("Inhalt:"), gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(createDialogScroll(contentArea), gbc);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JDialog createNoteDialogShell(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(460, 520);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel container = new JPanel(new BorderLayout(10, 10));
        container.setBackground(ThemeManager.getBackgroundColor());
        container.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        dialog.setContentPane(container);

        return dialog;
    }

    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    private JTextField createDialogTextField() {
        JTextField field = new JTextField();
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        return field;
    }

    private JTextArea createDialogTextArea(String content) {
        JTextArea area = new JTextArea(content);
        area.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        area.setBackground(ThemeManager.getInputBackgroundColor());
        area.setForeground(ThemeManager.getTextPrimaryColor());
        area.setCaretColor(ThemeManager.getTextPrimaryColor());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private JScrollPane createDialogScroll(JTextArea area) {
        JScrollPane scrollPane = new JScrollPane(area,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.getViewport().setBackground(ThemeManager.getInputBackgroundColor());
        return scrollPane;
    }

    private JButton createDialogButton(String text, Color bg) {
        JButton button = new JButton(text);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBackground(bg);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bg.darker(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createDialogButtonPanel(JButton primaryButton, JDialog dialog) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        panel.setOpaque(false);

        JButton cancel = createDialogButton(UnicodeSymbols.CLOSE + " Abbrechen", ThemeManager.getErrorColor());
        cancel.addActionListener(e -> dialog.dispose());

        panel.add(cancel);
        panel.add(primaryButton);
        return panel;
    }

    private void setupList() {
        listModel.clear();
        listModel.addAll(notesManager.getAllNotes().stream().map(Note::getTitle).toList());
        if (!listModel.isEmpty() && notesList.getSelectedIndex() == -1) {
            notesList.setSelectedIndex(0);
        }
    }

    public void display() {
        setVisible(true);
    }
}

