package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.WarningManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Locale;

/**
 * This class provides methods to display warnings in a modern, styled dialog. It can show individual warnings with details and actions, as well as a table overview of all warnings. It uses the WarningManager to retrieve and update warnings. The dialogs are designed to be visually appealing and user-friendly, with responsive layouts and consistent theming. The code is structured to separate UI construction from data handling, and includes helper methods for common UI components like headers, buttons, and status chips.
 */
public class DisplayWarningDialog {

    private static final Dimension WARNING_DIALOG_MIN_SIZE = new Dimension(700, 500);
    private static final Dimension WARNING_DIALOG_SIZE = new Dimension(820, 580);
    private static final Dimension WARNINGS_DIALOG_SIZE = new Dimension(1100, 760);
    private static final Dimension WARNINGS_EMPTY_DIALOG_SIZE = new Dimension(920, 620);
    private static final int MESSAGE_PREVIEW_LENGTH = 96;

    /**
     * Private constructor to prevent instantiation, as this class is intended to be used statically.
     */
    private DisplayWarningDialog() {
    }

    /**
     * Displays a warning in a modern, styled dialog.
     *
     * @param warning The warning object to display
     * @param frame   The parent frame for modality and centering
     */
    public static void displayWarning(JFrame frame, Warning warning) {
        if(frame == null) throw new IllegalArgumentException("Frame must not be null");

        ThemeManager.applyUIDefaults();
        if (warning == null) {
            new MessageDialog()
                    .setTitle("Keine Warnung")
                    .setMessage("Keine Warnung zum Anzeigen vorhanden.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame, "Warnung", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);
            dialog.setLayout(new BorderLayout());

            // Determine a palette based on the warning type
            WarningPalette palette = paletteFor(warning);

            JPanel chrome = createDialogChrome(ThemeManager.getBackgroundColor());
            chrome.setLayout(new BorderLayout());

            JPanel header = createGradientHeader(
                    palette.headerA(),
                    palette.headerB(),
                    palette.icon() + "  " + warning.getType().getDisplayName(),
                    UnicodeSymbols.INFO + " Details und Status",
                    dialog
            );
            chrome.add(header, BorderLayout.NORTH);

            JPanel contentCard = createCardPanel(new BorderLayout(0, 16), 22, 22, 18, 22);

            JLabel titleLabel = new JLabel(warning.getTitle());
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel dateLabel = createMetaLabel(UnicodeSymbols.CALENDAR + " Datum: " + warning.getDate());
            JLabel typeLabel = createMetaLabel(UnicodeSymbols.TAG + " Typ: " + warning.getType().getDisplayName());

            JPanel metaRow = new JPanel(new BorderLayout(12, 8));
            metaRow.setOpaque(false);
            metaRow.add(createStatusChip(warning.isResolved()), BorderLayout.EAST);

            JPanel metaLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            metaLeft.setOpaque(false);
            metaLeft.add(typeLabel);
            metaLeft.add(dateLabel);
            metaRow.add(metaLeft, BorderLayout.WEST);

            JPanel introPanel = new JPanel(new BorderLayout(0, 10));
            introPanel.setOpaque(false);
            introPanel.add(titleLabel, BorderLayout.NORTH);
            introPanel.add(metaRow, BorderLayout.SOUTH);
            contentCard.add(introPanel, BorderLayout.NORTH);

            JTextArea messageArea = createMessageArea(warning.getMessage(), 14);
            JScrollPane messageScroll = createMessageScrollPane(messageArea, 640, 260);
            contentCard.add(createMessageCard(messageScroll), BorderLayout.CENTER);

            JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonBar.setOpaque(false);

            if (!warning.isResolved()) {
                JButton resolveBtn = new JButton(UnicodeSymbols.CHECKMARK + " Als gelöst markieren");
                resolveBtn.setToolTipText("Markiert diese Warnung als gelöst");
                styleActionButton(resolveBtn, ThemeManager.getSuccessColor(), ThemeManager.getTextOnPrimaryColor());
                resolveBtn.addActionListener(e -> {
                    warning.setResolved(true);
                    warning.setDisplayed(true);
                    WarningManager.getInstance().resolveWarning(warning.getTitle());
                    new MessageDialog()
                            .setTitle("Erfolg")
                            .setMessage("Die Warnung wurde als gelöst markiert.")
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .setDuration(5000)
                            .display();
                    dialog.dispose();
                });
                buttonBar.add(resolveBtn);
            }

            JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
            closeBtn.setToolTipText("Schließt dieses Fenster");
            styleActionButton(closeBtn, palette.accent(), Color.WHITE);
            closeBtn.addActionListener(e -> dialog.dispose());
            buttonBar.add(closeBtn);

            contentCard.add(buttonBar, BorderLayout.SOUTH);

            chrome.add(contentCard, BorderLayout.CENTER);

            dialog.setContentPane(chrome);
            dialog.getRootPane().setDefaultButton(closeBtn);
            installEscToClose(dialog);

            // Mark as displayed
            warning.setDisplayed(true);

            dialog.setMinimumSize(WARNING_DIALOG_MIN_SIZE);
            dialog.setSize(WARNING_DIALOG_SIZE);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    /**
     * Display all warnings from WarningManager in a modern dialog.
     * @param frame The parent frame for modality and centering
     */
    public static void showAllWarnings(JFrame frame) {
        if(frame == null) throw new IllegalArgumentException("Frame must not be null");
        ThemeManager.applyUIDefaults();
        SwingUtilities.invokeLater(() -> {
        WarningManager warningManager = WarningManager.getInstance();
        List<Warning> warnings = warningManager.getAllWarnings();

        JDialog dialog = new JDialog(frame, UnicodeSymbols.WARNING + " Alle Warnungen", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());

        JPanel chrome = createDialogChrome(ThemeManager.getBackgroundColor());
        chrome.setLayout(new BorderLayout());

        // Header
        String subtitle = warnings.isEmpty() ? "Keine Warnungen" : (warnings.size() + " Warnung(en)");
        JPanel header = createGradientHeader(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.getHeaderGradientColor(),
                UnicodeSymbols.WARNING + " Warnungen Übersicht",
                subtitle,
                dialog
        );
        chrome.add(header, BorderLayout.NORTH);

        // Content
        if (warnings.isEmpty()) {
            JPanel emptyCard = createCardPanel(new GridBagLayout(), 34, 30, 34, 30);

            JLabel emptyLabel = new JLabel(UnicodeSymbols.CHECKMARK + " Keine Warnungen vorhanden");
            emptyLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 16));
            emptyLabel.setForeground(ThemeManager.getSuccessColor());
            emptyCard.add(emptyLabel);

            JPanel centerPad = new JPanel(new BorderLayout());
            centerPad.setOpaque(false);
            centerPad.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            centerPad.add(emptyCard, BorderLayout.CENTER);
            chrome.add(centerPad, BorderLayout.CENTER);

            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
            footer.setOpaque(false);
            footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

            JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
            styleActionButton(closeBtn, ThemeManager.getSurfaceColor(), ThemeManager.getTextPrimaryColor());
            closeBtn.addActionListener(e -> dialog.dispose());
            footer.add(closeBtn);

            chrome.add(footer, BorderLayout.SOUTH);

            dialog.setContentPane(chrome);
            dialog.getRootPane().setDefaultButton(closeBtn);
            installEscToClose(dialog);

            dialog.setSize(WARNINGS_EMPTY_DIALOG_SIZE);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            return;
        }

        // Table model
        DefaultTableModel tableModel = getModel();

        for (Warning w : warnings) {
            String status = w.isResolved() ? UnicodeSymbols.CHECKMARK + " Gelöst" : UnicodeSymbols.WARNING + " Offen";
            String type = switch (w.getType()) {
                case LOW_STOCK -> "Niedriger Bestand";
                case ORDER_NEEDED -> "Bestellung nötig";
                case CRITICAL_STOCK -> "Kritischer Bestand";
                default -> "Sonstiges";
            };

            tableModel.addRow(new Object[]{
                    status,
                    type,
                    w.getTitle(),
                    previewMessage(w.getMessage()),
                    w.getDate()
            });
        }

        JTable table = getWarningsTable(tableModel, warnings);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Render: alternating rows; status color only in column 0
        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                    c.setForeground(ThemeManager.getTextPrimaryColor());

                    if (column == 0 && value != null) {
                        String s = value.toString();
                        if (s.contains("Gelöst")) {
                            c.setForeground(ThemeManager.getSuccessColor());
                        } else {
                            c.setForeground(ThemeManager.getWarningForegroundColor());
                        }
                    }
                } else {
                    c.setBackground(ThemeManager.getSelectionBackgroundColor());
                    c.setForeground(ThemeManager.getSelectionForegroundColor());
                }

                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                setHorizontalAlignment(column == 3 ? SwingConstants.LEFT : SwingConstants.CENTER);
                return c;
            }
        };
        table.setDefaultRenderer(Object.class, defaultRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) defaultRenderer.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setText(createPreviewHtml(value == null ? "" : value.toString(), isSelected));
                return label;
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(120);
        table.getColumnModel().getColumn(1).setPreferredWidth(170);
        table.getColumnModel().getColumn(2).setPreferredWidth(220);
        table.getColumnModel().getColumn(3).setPreferredWidth(420);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);

        JTableHeader th = table.getTableHeader();
        th.setBackground(ThemeManager.getTableHeaderBackgroundColor());
        th.setForeground(ThemeManager.getTableHeaderForegroundColor());
        th.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        th.setPreferredSize(new Dimension(th.getWidth(), 40));

        JTextField searchField = createSearchField();
        JLabel resultLabel = createMetaLabel(UnicodeSymbols.SEARCH + " Suche in Titel, Nachricht, Typ und Datum");
        JLabel openCountChip = createSummaryChip(
                UnicodeSymbols.WARNING + " Offen: 0",
                ThemeManager.getWarningColor(), ThemeManager.withAlpha(ThemeManager.getWarningColor(), 30));
        JLabel resolvedCountChip = createSummaryChip(
                UnicodeSymbols.CHECKMARK + " Gelöst: 0",
                ThemeManager.getSuccessColor(), ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 28));
        JLabel totalCountChip = createSummaryChip(
                UnicodeSymbols.CLIPBOARD + " Sichtbar: 0 / " + warnings.size(),
                ThemeManager.getAccentColor(), ThemeManager.withAlpha(ThemeManager.getAccentColor(), 24));
        final WarningFilterMode[] filterMode = {WarningFilterMode.ALL};

        JPanel filterRow = new JPanel(new BorderLayout(10, 0));
        filterRow.setOpaque(false);
        filterRow.add(searchField, BorderLayout.CENTER);
        filterRow.add(resultLabel, BorderLayout.EAST);

        JPanel filterButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterButtons.setOpaque(false);
        JToggleButton allButton = createFilterToggleButton("Alle");
        JToggleButton openButton = createFilterToggleButton("Nur offen");
        JToggleButton resolvedButton = createFilterToggleButton("Nur gelöst");
        ButtonGroup filterGroup = new ButtonGroup();
        filterGroup.add(allButton);
        filterGroup.add(openButton);
        filterGroup.add(resolvedButton);
        allButton.setSelected(true);
        filterButtons.add(allButton);
        filterButtons.add(openButton);
        filterButtons.add(resolvedButton);

        JPanel summaryRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        summaryRow.setOpaque(false);
        summaryRow.add(openCountChip);
        summaryRow.add(resolvedCountChip);
        summaryRow.add(totalCountChip);
        summaryRow.add(filterButtons);

        JPanel contentTop = createCardPanel(new BorderLayout(0, 10), 16, 16, 10, 16);
        contentTop.add(filterRow, BorderLayout.NORTH);
        contentTop.add(summaryRow, BorderLayout.SOUTH);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true));
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.setBackground(ThemeManager.getBackgroundColor());

        JTextArea detailsArea = createMessageArea("", 15);
        detailsArea.setText("Wählen Sie eine Warnung aus,\num alle Details vollständig zu sehen.");
        JLabel detailTitle = new JLabel(UnicodeSymbols.INFO + " Warnungsdetails");
        detailTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        detailTitle.setForeground(ThemeManager.getTextPrimaryColor());
        JLabel detailMeta = createMetaLabel("Noch keine Warnung ausgewählt");
        JPanel detailStatusHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        detailStatusHolder.setOpaque(false);

        JPanel detailHeader = new JPanel(new BorderLayout(10, 8));
        detailHeader.setOpaque(false);
        JPanel detailHeaderLeft = new JPanel(new BorderLayout(0, 6));
        detailHeaderLeft.setOpaque(false);
        detailHeaderLeft.add(detailTitle, BorderLayout.NORTH);
        detailHeaderLeft.add(detailMeta, BorderLayout.SOUTH);
        detailHeader.add(detailHeaderLeft, BorderLayout.CENTER);
        detailHeader.add(detailStatusHolder, BorderLayout.EAST);

        JPanel detailsCard = createCardPanel(new BorderLayout(0, 14), 18, 18, 18, 18);
        detailsCard.add(detailHeader, BorderLayout.NORTH);
        detailsCard.add(createMessageCard(
                createMessageScrollPane(detailsArea, 900, 180),
                "Vollständige Nachricht der ausgewählten Warnung",
                "DETAILANSICHT"), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, detailsCard);
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder(6, 16, 10, 16));
        splitPane.setResizeWeight(0.62);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerSize(10);
        splitPane.setBackground(ThemeManager.getBackgroundColor());

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.add(contentTop, BorderLayout.NORTH);
        centerPanel.add(splitPane, BorderLayout.CENTER);

        chrome.add(centerPanel, BorderLayout.CENTER);

        // Footer actions
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JPanel footerStatus = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        footerStatus.setOpaque(false);
        footerStatus.add(createMetaLabel("Doppelklick zeigt die ausgewählte Warnung im Detaildialog."));

        JPanel footerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footerButtons.setOpaque(false);

        JButton viewDetailsBtn = new JButton(UnicodeSymbols.INFO + " Details");
        viewDetailsBtn.setToolTipText("Zeigt die Details der ausgewählten Warnung an");
        styleActionButton(viewDetailsBtn, ThemeManager.getPrimaryColor(), ThemeManager.getTextOnPrimaryColor());

        JButton resolveBtn = new JButton(UnicodeSymbols.CHECKMARK + " Als gelöst");
        resolveBtn.setToolTipText("Markiert die ausgewählte Warnung als gelöst");
        styleActionButton(resolveBtn, ThemeManager.getSuccessColor(), ThemeManager.getTextOnPrimaryColor());

        JButton deleteBtn = new JButton(UnicodeSymbols.TRASH + " Löschen");
        deleteBtn.setToolTipText("Löscht die ausgewählte Warnung");
        styleActionButton(deleteBtn, ThemeManager.getErrorColor(), ThemeManager.getTextOnPrimaryColor());

        JButton refreshBtn = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshBtn.setToolTipText("Aktualisiert die Warnungsliste");
        styleActionButton(refreshBtn, ThemeManager.getSurfaceColor(), ThemeManager.getTextPrimaryColor());

        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
        closeBtn.setToolTipText("Schließt dieses Fenster");
        styleActionButton(closeBtn, ThemeManager.getSurfaceColor(), ThemeManager.getTextPrimaryColor());

        Runnable refreshOverviewState = () -> applyFilters(
                sorter, warnings, table, searchField, filterMode[0], resultLabel,
                openCountChip, resolvedCountChip, totalCountChip,
                detailTitle, detailMeta, detailsArea, detailStatusHolder);

        allButton.addActionListener(e -> {
            filterMode[0] = WarningFilterMode.ALL;
            refreshOverviewState.run();
        });
        openButton.addActionListener(e -> {
            filterMode[0] = WarningFilterMode.OPEN;
            refreshOverviewState.run();
        });
        resolvedButton.addActionListener(e -> {
            filterMode[0] = WarningFilterMode.RESOLVED;
            refreshOverviewState.run();
        });
        installWarningSearch(searchField, refreshOverviewState);

        viewDetailsBtn.addActionListener(e -> {
            Warning selected = getSelectedWarning(table, warnings);
            if (selected == null) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie eine Warnung aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            displayWarning(frame, selected);
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int viewRow = table.getSelectedRow();
                    if (viewRow != -1) {
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        displayWarning(frame, warnings.get(modelRow));
                    }
                }
            }
        });

        resolveBtn.addActionListener(e -> {
            Warning selected = getSelectedWarning(table, warnings);
            if (selected == null) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = warnings.indexOf(selected);

            if (selected.isResolved()) {
                new MessageDialog()
                        .setTitle("Bereits gelöst")
                        .setMessage("Diese Warnung wurde bereits gelöst.")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .setDuration(5000)
                        .display();
                return;
            }

            if (warningManager.resolveWarning(selected.getTitle())) {
                selected.setResolved(true);
                tableModel.setValueAt(UnicodeSymbols.CHECKMARK + " Gelöst", modelRow, 0);
                refreshOverviewState.run();
                new MessageDialog()
                        .setTitle("Erfolg")
                        .setMessage("Warnung wurde als gelöst markiert.")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .setDuration(5000)
                        .display();
            }
        });

        deleteBtn.addActionListener(e -> {
            Warning selected = getSelectedWarning(table, warnings);
            if (selected == null) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie eine Warnung aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            int modelRow = warnings.indexOf(selected);

            int confirm = new MessageDialog()
                .setTitle("Löschen bestätigen")
                .setMessage("Möchten Sie diese Warnung wirklich löschen?")
                .setMessageType(JOptionPane.QUESTION_MESSAGE)
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
            if (confirm != JOptionPane.YES_OPTION) return;

            if (warningManager.deleteWarning(selected.getTitle())) {
                tableModel.removeRow(modelRow);
                warnings.remove(modelRow);
                refreshOverviewState.run();
            }
        });

        refreshBtn.addActionListener(e -> {
            dialog.dispose();
            showAllWarnings(frame);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateWarningDetails(getSelectedWarning(table, warnings), detailTitle, detailMeta, detailsArea, detailStatusHolder);
            }
        });

        refreshOverviewState.run();

        footerButtons.add(viewDetailsBtn);
        footerButtons.add(resolveBtn);
        footerButtons.add(deleteBtn);
        footerButtons.add(refreshBtn);
        footerButtons.add(closeBtn);

        footer.add(footerStatus, BorderLayout.WEST);
        footer.add(footerButtons, BorderLayout.EAST);

        chrome.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(chrome);
        dialog.getRootPane().setDefaultButton(viewDetailsBtn);
        installEscToClose(dialog);

        dialog.setSize(WARNINGS_DIALOG_SIZE);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
        });
    }

    private static DefaultTableModel getModel() {
        String[] columnNames = {
                UnicodeSymbols.STATUS + " Status",
                UnicodeSymbols.TAG + " Typ",
                UnicodeSymbols.TITLE + " Titel",
                UnicodeSymbols.MEMO + " Nachricht",
                UnicodeSymbols.CALENDAR + " Datum"
        };
        return new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static JTable getWarningsTable(DefaultTableModel tableModel, List<Warning> warnings) {
        if(tableModel == null) throw new IllegalArgumentException("Table model must not be null");
        JTable table = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int viewRow = rowAtPoint(event.getPoint());
                int viewColumn = columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return null;
                }
                if (viewColumn == 3) {
                    int modelRow = convertRowIndexToModel(viewRow);
                    if (modelRow >= 0 && modelRow < warnings.size()) {
                        return toHtmlTooltip(warnings.get(modelRow).getMessage());
                    }
                }
                Object value = getValueAt(viewRow, viewColumn);
                return value == null ? null : value.toString();
            }
        };
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(44);
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setShowVerticalLines(false);
        table.setRowMargin(0);
        table.setGridColor(ThemeManager.getTableGridColor());
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        return table;
    }

    private static JTextField createSearchField() {
        JTextField field = new JTextField();
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)
        ));
        field.setToolTipText("Suche nach Titel, Nachricht, Typ oder Datum");
        return field;
    }

    private static void installWarningSearch(JTextField searchField, Runnable onFilterChange) {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onFilterChange.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onFilterChange.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                onFilterChange.run();
            }
        };
        searchField.getDocument().addDocumentListener(listener);
    }

    private static void applyFilters(TableRowSorter<DefaultTableModel> sorter, List<Warning> warnings, JTable table,
                                     JTextField searchField, WarningFilterMode filterMode, JLabel resultLabel,
                                     JLabel openCountChip, JLabel resolvedCountChip, JLabel totalCountChip,
                                     JLabel detailTitle, JLabel detailMeta, JTextArea detailsArea, JPanel detailStatusHolder) {
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Warning warning = warnings.get(entry.getIdentifier());
                if (filterMode == WarningFilterMode.OPEN && warning.isResolved()) {
                    return false;
                }
                if (filterMode == WarningFilterMode.RESOLVED && !warning.isResolved()) {
                    return false;
                }
                if (query.isEmpty()) {
                    return true;
                }
                return containsIgnoreCase(warning.getTitle(), query)
                        || containsIgnoreCase(warning.getMessage(), query)
                        || containsIgnoreCase(warning.getDate(), query)
                        || containsIgnoreCase(warning.getType().getDisplayName(), query);
            }
        });
        updateOverviewStats(table, warnings.size(), openCountChip, resolvedCountChip, totalCountChip, resultLabel);
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            updateWarningDetails(getSelectedWarning(table, warnings), detailTitle, detailMeta, detailsArea, detailStatusHolder);
        } else {
            table.clearSelection();
            updateWarningDetails(null, detailTitle, detailMeta, detailsArea, detailStatusHolder);
        }
    }

    private static void updateOverviewStats(JTable table, int totalWarnings, JLabel openCountChip,
                                            JLabel resolvedCountChip, JLabel totalCountChip, JLabel resultLabel) {
        int visibleOpen = 0;
        int visibleResolved = 0;
        for (int i = 0; i < table.getRowCount(); i++) {
            Object statusValue = table.getValueAt(i, 0);
            if (statusValue != null && statusValue.toString().contains("Gelöst")) {
                visibleResolved++;
            } else {
                visibleOpen++;
            }
        }
        openCountChip.setText(UnicodeSymbols.WARNING + " Offen: " + visibleOpen);
        resolvedCountChip.setText(UnicodeSymbols.CHECKMARK + " Gelöst: " + visibleResolved);
        totalCountChip.setText(UnicodeSymbols.CLIPBOARD + " Sichtbar: " + table.getRowCount() + " / " + totalWarnings);
        resultLabel.setText(UnicodeSymbols.SEARCH + " Treffer: " + table.getRowCount());
    }

    private static boolean containsIgnoreCase(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private static String previewMessage(String message) {
        if (message == null) {
            return "";
        }
        String compact = message.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (compact.length() <= MESSAGE_PREVIEW_LENGTH) {
            return compact;
        }
        return compact.substring(0, MESSAGE_PREVIEW_LENGTH - 1) + "…";
    }

    private static String createPreviewHtml(String preview, boolean selected) {
        String fg = toHex(selected ? ThemeManager.getSelectionForegroundColor() : ThemeManager.getTextPrimaryColor());
        String muted = toHex(selected
                ? ThemeManager.withAlpha(ThemeManager.getSelectionForegroundColor(), 215)
                : ThemeManager.getTextSecondaryColor());
        return "<html><div style='line-height:1.2;padding-top:2px;padding-bottom:2px;'>"
                + "<span style='color:" + muted + ";font-size:9px;font-weight:700;'>VORSCHAU</span><br/>"
                + "<span style='color:" + fg + ";font-size:11px;'>"
                + escapeHtml(preview)
                + "</span></div></html>";
    }

    private static String toHtmlTooltip(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return "<html><div style='width:420px;'>" + escapeHtml(text).replace("\n", "<br/>") + "</div></html>";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static Warning getSelectedWarning(JTable table, List<Warning> warnings) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= warnings.size()) {
            return null;
        }
        return warnings.get(modelRow);
    }

    private static void updateWarningDetails(Warning warning, JLabel titleLabel, JLabel metaLabel,
                                             JTextArea messageArea, JPanel statusHolder) {
        if (warning == null) {
            titleLabel.setText(UnicodeSymbols.INFO + " Warnungsdetails");
            metaLabel.setText("Noch keine Warnung ausgewählt");
            messageArea.setText("Wählen Sie eine Warnung aus,\num alle Details vollständig zu sehen.");
            statusHolder.removeAll();
            statusHolder.revalidate();
            statusHolder.repaint();
            return;
        }

        titleLabel.setText(warning.getTitle());
        metaLabel.setText(UnicodeSymbols.TAG + " " + warning.getType().getDisplayName()
                + "   •   " + UnicodeSymbols.CALENDAR + " " + warning.getDate());
        messageArea.setText(warning.getMessage());
        messageArea.setCaretPosition(0);

        statusHolder.removeAll();
        statusHolder.add(createStatusChip(warning.isResolved()));
        statusHolder.revalidate();
        statusHolder.repaint();
    }

    // ------------------------ UI helpers ------------------------

    private static void installEscToClose(JDialog dialog) {
        if(dialog == null) throw new IllegalArgumentException("Dialog must not be null");
        JRootPane root = dialog.getRootPane();
        root.registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private static JPanel createDialogChrome(Color bg) {
        JPanel chrome = createCardPanel(new BorderLayout(), 0, 0, 0, 0, bg, 18);
        Color shadow = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 90 : 35);
        Color border = ThemeManager.getBorderColor();
        chrome.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadow, 1, true),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(border, 1, true),
                        BorderFactory.createEmptyBorder(0, 0, 0, 0)
                )
        ));
        return chrome;
    }

    // Unified card panel creation
    private static JPanel createCardPanel(LayoutManager layout, int top, int left, int bottom, int right) {
        return createCardPanel(layout, top, left, bottom, right, ThemeManager.getCardBackgroundColor(), 14);
    }
    private static JPanel createCardPanel(LayoutManager layout, int top, int left, int bottom, int right, Color bg, int radius) {
        JPanel panel = new ArticleGUI.RoundedPanel(bg, radius);
        panel.setLayout(layout);
        panel.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        panel.setOpaque(false);
        return panel;
    }

    private static JTextArea createMessageArea(String text, int fontSize) {
        JTextArea messageArea = new JTextArea(text);
        messageArea.setWrapStyleWord(true);
        messageArea.setLineWrap(true);
        messageArea.setEditable(false);
        messageArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, fontSize));
        messageArea.setForeground(ThemeManager.getTextPrimaryColor());
        messageArea.setBackground(ThemeManager.getSurfaceColor());
        messageArea.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        messageArea.setMargin(new Insets(6, 6, 6, 6));
        messageArea.setCaretPosition(0);
        messageArea.setSelectionColor(ThemeManager.getSelectionBackgroundColor());
        messageArea.setSelectedTextColor(ThemeManager.getSelectionForegroundColor());
        return messageArea;
    }

    private static JScrollPane createMessageScrollPane(JTextArea messageArea, int preferredWidth, int preferredHeight) {
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        messageScroll.getViewport().setBackground(ThemeManager.getSurfaceColor());
        messageScroll.setBackground(ThemeManager.getSurfaceColor());
        messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        messageScroll.getVerticalScrollBar().setUnitIncrement(14);
        messageScroll.setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        return messageScroll;
    }

    private static JPanel createMessageCard(JScrollPane messageScroll) {
        return createMessageCard(messageScroll, "Ausführlicher Nachrichtentext", "NACHRICHT");
    }

    private static JPanel createMessageCard(JScrollPane messageScroll, String helperText, String badgeText) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setOpaque(false);
        JLabel label = new JLabel(UnicodeSymbols.MEMO + " Nachricht");
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel helperLabel = createMetaLabel(helperText);
        JComponent badge = createPreviewBadge(badgeText);

        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setOpaque(false);
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerLeft.setOpaque(false);
        headerLeft.add(label);
        headerLeft.add(badge);
        header.add(headerLeft, BorderLayout.WEST);
        header.add(helperLabel, BorderLayout.EAST);

        JPanel body = new ArticleGUI.RoundedPanel(ThemeManager.withAlpha(ThemeManager.getSurfaceColor(), 248), 16);
        body.setLayout(new BorderLayout(0, 0));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getBorderColor(), 105), 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        body.setOpaque(false);

        JPanel accentRail = new JPanel();
        accentRail.setPreferredSize(new Dimension(8, 0));
        accentRail.setBackground(ThemeManager.withAlpha(ThemeManager.getAccentColor(), 185));
        body.add(accentRail, BorderLayout.WEST);

        JPanel scrollWrap = new JPanel(new BorderLayout());
        scrollWrap.setOpaque(false);
        scrollWrap.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        scrollWrap.add(messageScroll, BorderLayout.CENTER);
        body.add(scrollWrap, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel createMetaLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        return label;
    }

    private static JComponent createPreviewBadge(String text) {
        JLabel badge = new JLabel(text);
        badge.setFont(SettingsGUI.getFontByName(Font.BOLD, 10));
        badge.setForeground(ThemeManager.getAccentColor());
        badge.setOpaque(true);
        badge.setBackground(ThemeManager.withAlpha(ThemeManager.getAccentColor(), 28));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getAccentColor(), 110), 1, true),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        return badge;
    }

    private static JToggleButton createFilterToggleButton(String text) {
        JToggleButton button = new JToggleButton(text);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextPrimaryColor());
        button.setBackground(ThemeManager.getSurfaceColor());
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)
        ));
        button.addItemListener(e -> {
            boolean selected = button.isSelected();
            Color bg = selected ? ThemeManager.withAlpha(ThemeManager.getAccentColor(), 36) : ThemeManager.getSurfaceColor();
            Color fg = selected ? ThemeManager.getAccentColor() : ThemeManager.getTextPrimaryColor();
            Color border = selected ? ThemeManager.getAccentColor() : ThemeManager.getBorderColor();
            button.setBackground(bg);
            button.setForeground(fg);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1, true),
                    BorderFactory.createEmptyBorder(7, 12, 7, 12)
            ));
        });
        return button;
    }

    private static JPanel createGradientHeader(Color a, Color b, String title, String subtitle, JDialog dialog) {
        JPanel header = getHeader(a, b);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.withAlpha(Color.WHITE, 210));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textStack.add(titleLabel);
        textStack.add(Box.createVerticalStrut(4));
        textStack.add(subtitleLabel);

        header.add(textStack, BorderLayout.WEST);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        meta.setOpaque(false);
        meta.add(createHeaderBadge(UnicodeSymbols.INFO + " Fokusmodus"));
        meta.add(createHeaderBadge(UnicodeSymbols.CALENDAR + " Heute"));

        JButton closeBtn = createHeaderCloseButton(dialog);
        meta.add(closeBtn);
        header.add(meta, BorderLayout.EAST);

        return header;
    }

    private static JPanel getHeader(Color a, Color b) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, a, getWidth(), 0, b);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 18));
        return header;
    }

    private static JButton createHeaderCloseButton(JDialog dialog) {
        if(dialog == null) throw new IllegalArgumentException("Dialog must not be null");
        JButton close = new JButton(UnicodeSymbols.CLOSE);
        close.setToolTipText("Schließen");
        close.setForeground(ThemeManager.withAlpha(Color.WHITE, 220));
        close.setBackground(ThemeManager.withAlpha(Color.WHITE, 0));
        close.setBorderPainted(false);
        close.setFocusPainted(false);
        close.setContentAreaFilled(false);
        close.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setPreferredSize(new Dimension(42, 42));
        close.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        close.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                close.setForeground(Color.WHITE);
                close.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 110));
                close.setContentAreaFilled(true);
                close.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                close.setForeground(ThemeManager.withAlpha(Color.WHITE, 220));
                close.setBackground(ThemeManager.withAlpha(Color.WHITE, 0));
                close.setContentAreaFilled(false);
                close.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }
        });

        close.addActionListener(e -> dialog.dispose());
        return close;
    }

    private static JLabel createStatusChip(boolean resolved) {
        String text = resolved ? (UnicodeSymbols.CHECKMARK + " Gelöst") : (UnicodeSymbols.ERROR + " Ungelöst");
        Color bg = resolved ? ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 28)
                : ThemeManager.withAlpha(ThemeManager.getErrorColor(), 28);
        Color fg = resolved ? ThemeManager.getSuccessColor() : ThemeManager.getErrorColor();

        JLabel chip = new JLabel(text);
        chip.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        chip.setForeground(fg);
        chip.setOpaque(true);
        chip.setBackground(bg);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(fg, 120), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return chip;
    }

    private static void styleActionButton(JButton button, Color base, Color fg) {
        if(button == null) throw new IllegalArgumentException("Button must not be null");
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);

        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setForeground(fg);
        button.setBackground(base);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1, true),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));

        // Remove only listeners previously added by this helper (keep LAF/UI listeners intact)
        for (MouseListener ml : button.getMouseListeners()) {
            String cn = ml.getClass().getName();
            if (cn.contains("DisplayWarningDialog")) {
                button.removeMouseListener(ml);
            }
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hover.darker(), 1, true),
                        BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(base);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(base.darker(), 1, true),
                        BorderFactory.createEmptyBorder(10, 18, 10, 18)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hover : base);
            }
        });
    }

    private static WarningPalette paletteFor(Warning warning) {
        if(warning == null) throw new IllegalArgumentException("Warning must not be null");
        // Two-tone header gradients + accent for CTA
        return switch (warning.getType()) {
            case LOW_STOCK -> new WarningPalette(
                    ThemeManager.getWarningColor(),
                    ThemeManager.adjustColor(ThemeManager.getWarningColor(), -35),
                    ThemeManager.adjustColor(ThemeManager.getWarningColor(), -20),
                    UnicodeSymbols.WARNING
            );
            case ORDER_NEEDED -> new WarningPalette(
                    ThemeManager.getErrorColor(),
                    ThemeManager.adjustColor(ThemeManager.getErrorColor(), -35),
                    ThemeManager.adjustColor(ThemeManager.getErrorColor(), -20),
                    UnicodeSymbols.CIRCLE
            );
            default -> new WarningPalette(
                    ThemeManager.getAccentColor(),
                    ThemeManager.adjustColor(ThemeManager.getAccentColor(), -35),
                    ThemeManager.adjustColor(ThemeManager.getAccentColor(), -20),
                    UnicodeSymbols.INFO
            );
        };
    }

    private record WarningPalette(Color headerA, Color headerB, Color accent, String icon) {
    }

    private static JComponent createHeaderBadge(String text) {
        JLabel badge = new JLabel(text);
        badge.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        badge.setForeground(ThemeManager.withAlpha(Color.WHITE, 220));
        badge.setOpaque(true);
        badge.setBackground(ThemeManager.withAlpha(Color.WHITE, 24));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(Color.WHITE, 70), 1, true),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)
        ));
        return badge;
    }

    private static JLabel createSummaryChip(String text, Color fg, Color bg) {
        JLabel chip = new JLabel(text);
        chip.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        chip.setForeground(fg);
        chip.setOpaque(true);
        chip.setBackground(bg);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.withAlpha(fg, 120), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        return chip;
    }

    private enum WarningFilterMode {
        ALL,
        OPEN,
        RESOLVED
    }
}
