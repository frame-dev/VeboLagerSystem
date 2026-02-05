package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.NotesManager;
import ch.framedev.lagersystem.utils.Note;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import java.awt.*;

/**
 * Notes manager window for viewing, creating, and updating personal notes.
 * Provides a split list/detail layout, toolbar actions, and themed dialogs.
 * Dialogs use rounded card styling with a header strip to match the app theme.
 */
public class NotesGUI extends JFrame {

    private final NotesManager notesManager = NotesManager.getInstance();

    private final DefaultListModel<String> listModel = new DefaultListModel<>();
    private final JList<String> notesList = new JList<>(listModel);
    private final JTextArea noteContentArea = new JTextArea(20, 30);
    private float fontScaleDelta = 0f;

    public NotesGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle(UnicodeSymbols.MEMO + " Notizen");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(860, 660);
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

        JPanel bottomPanel = createFooterToolbar();
        root.add(bottomPanel, BorderLayout.SOUTH);

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

        JButton deleteDialogButton = new JButton(UnicodeSymbols.TRASH + " Notiz löschen");
        deleteDialogButton.addActionListener(e -> deleteNote());

        JButton refreshButton = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.addActionListener(e -> setupList());

        for (JButton button : new JButton[]{createNoteButton, updateDialogButton, deleteDialogButton, refreshButton}) {
            styleToolbarButton(button);
            toolbar.add(button);
        }

        return toolbar;
    }

    private JPanel createFooterToolbar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolBar.setOpaque(false);

        JButton fontSizeBiggerButton = new JButton("Increase Font");
        fontSizeBiggerButton.addActionListener(listener -> bumpFontSize(2f));

        JButton fontSizeSmallerButton = new JButton("Decrease Font");
        fontSizeSmallerButton.addActionListener(listener -> bumpFontSize(-2f));

        for (JButton button : new JButton[]{fontSizeBiggerButton, fontSizeSmallerButton}) {
            styleToolbarButton(button);
            toolBar.add(button);
        }

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

        // Scroll panes for list and content
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
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 8, 0);
        form.add(createDialogLabel("Inhalt:"), gbc);

        gbc.gridy = 1;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(createDialogScroll(contentArea), gbc);

        JPanel body = getDialogBodyPanel(dialog);
        body.add(form, BorderLayout.CENTER);
        body.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

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
            } else {
                JOptionPane.showMessageDialog(this, "Fehler beim Löschen der Notiz.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
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
        gbc.weighty = 0.0;
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

        JPanel body = getDialogBodyPanel(dialog);
        body.add(form, BorderLayout.CENTER);
        body.add(createDialogButtonPanel(saveButton, dialog), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JDialog createNoteDialogShell(String title) {
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
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

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
                BorderFactory.createEmptyBorder(16, 16, 16, 16)
        ));

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

    private JPanel getDialogBodyPanel(JDialog dialog) {
        Object body = dialog.getRootPane().getClientProperty("notes.dialog.body");
        return body instanceof JPanel panel ? panel : (JPanel) dialog.getContentPane();
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
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
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

        Color hover = ThemeManager.getButtonHoverColor(bg);
        button.addChangeListener(e -> button.setBackground(button.getModel().isRollover() ? hover : bg));
        return button;
    }

    private JPanel createDialogButtonPanel(JButton primaryButton, JDialog dialog) {
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
                BorderFactory.createEmptyBorder(10, 0, 0, 0)
        ));
        wrapper.add(panel, BorderLayout.EAST);
        return wrapper;
    }

    private void setupList() {
        listModel.clear();
        listModel.addAll(notesManager.getAllNotes().stream().map(Note::getTitle).toList());
        if (!listModel.isEmpty() && notesList.getSelectedIndex() == -1) {
            notesList.setSelectedIndex(0);
        }
    }

    private void bumpFontSize(float delta) {
        fontScaleDelta += delta;
        applyFontDelta(getContentPane(), delta);
        revalidate();
        repaint();
    }

    private void applyFontDelta(Component component, float delta) {
        Font font = component.getFont();
        if (font != null) {
            component.setFont(font.deriveFont(font.getSize2D() + delta));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyFontDelta(child, delta);
            }
        }
    }

    public void display() {
        setVisible(true);
    }

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
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
