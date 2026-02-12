package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.WarningManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class DisplayWarningDialog {

    /**
     * Displays a warning in a modern, styled dialog
     *
     * @param warning The warning object to display
     */
    public static void displayWarning(JFrame frame, Warning warning) {
        SwingUtilities.invokeLater(() -> {
            // Create custom dialog
            JDialog dialog = new JDialog(frame, "Warnung", true);
            dialog.setUndecorated(true);
            dialog.requestFocus();
            dialog.setAutoRequestFocus(true);
            dialog.setLayout(new BorderLayout());

            // Main panel with rounded corners
            ArticleGUI.RoundedPanel mainPanel = new ArticleGUI.RoundedPanel(ThemeManager.getBackgroundColor(), 20);
            mainPanel.setLayout(new BorderLayout(0, 0));
            mainPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                    BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));

            // Determine colors based on warning type
            Color headerColor;
            Color accentColor;
            String icon;
            switch (warning.getType()) {
                case LOW_STOCK:
                    headerColor = ThemeManager.getWarningColor();
                    accentColor = ThemeManager.adjustColor(headerColor, -20);
                    icon = UnicodeSymbols.WARNING;
                    break;
                case ORDER_NEEDED:
                    headerColor = ThemeManager.getErrorColor();
                    accentColor = ThemeManager.adjustColor(headerColor, -20);
                    icon = UnicodeSymbols.CIRCLE;
                    break;
                default:
                    headerColor = ThemeManager.getAccentColor();
                    accentColor = ThemeManager.adjustColor(headerColor, -20);
                    icon = UnicodeSymbols.INFO;
                    break;
            }

            // Header with warning type
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(headerColor);
            header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel typeLabel = new JLabel(icon + "  " + warning.getType().getDisplayName());
            typeLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
            typeLabel.setForeground(Color.WHITE);
            header.add(typeLabel, BorderLayout.WEST);

            // Close button
            JButton closeBtn = new JButton("✕");
            closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setBackground(headerColor);
            closeBtn.setBorderPainted(false);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.setPreferredSize(new Dimension(40, 40));
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
            closeBtn.addActionListener(e -> dialog.dispose());
            header.add(closeBtn, BorderLayout.EAST);

            mainPanel.add(header, BorderLayout.NORTH);

            // Content panel
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(ThemeManager.getCardBackgroundColor());
            content.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 12, 0);

            // Title
            JLabel titleLabel = new JLabel("<html><b>" + warning.getTitle() + "</b></html>");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
            content.add(titleLabel, gbc);

            // Message
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 16, 0);
            JTextArea messageArea = new JTextArea(warning.getMessage());
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setEditable(false);
            messageArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            messageArea.setForeground(ThemeManager.getTextPrimaryColor());
            messageArea.setBackground(ThemeManager.getCardBackgroundColor());
            messageArea.setBorder(null);
            messageArea.setRows(4);
            messageArea.setColumns(35);
            content.add(messageArea, gbc);

            // Date
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 20, 0);
            JLabel dateLabel = new JLabel(UnicodeSymbols.CALENDAR + " Datum: " + warning.getDate());
            dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            dateLabel.setForeground(ThemeManager.getTextSecondaryColor());
            content.add(dateLabel, gbc);

            // Status indicator
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 24, 0);
            JLabel statusLabel = new JLabel(warning.isResolved() ? UnicodeSymbols.CHECKMARK + " Gelöst" : UnicodeSymbols.ERROR + " Ungelöst");
            statusLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
            statusLabel.setForeground(warning.isResolved() ? ThemeManager.getSuccessColor() : ThemeManager.getErrorColor());
            content.add(statusLabel, gbc);

            // Button panel
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 0, 0);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setOpaque(false);

            if (!warning.isResolved()) {
                JButton resolveBtn = new JButton("OK Als gelöst markieren");
                styleButton(resolveBtn, ThemeManager.getSuccessColor(), ThemeManager.getTextOnPrimaryColor());
                resolveBtn.addActionListener(e -> {
                    warning.setResolved(true);
                    warning.setDisplayed(true);
                    WarningManager.getInstance().resolveWarning(warning.getTitle());
                    JOptionPane.showMessageDialog(dialog,
                            "Die Warnung wurde als gelöst markiert.",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                    dialog.dispose();
                });
                buttonPanel.add(resolveBtn);
            }

            JButton okBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
            okBtn.setToolTipText("Schließt dieses Fenster");
            styleButton(okBtn, accentColor, Color.WHITE);
            okBtn.addActionListener(e -> dialog.dispose());
            buttonPanel.add(okBtn);

            content.add(buttonPanel, gbc);

            mainPanel.add(content, BorderLayout.CENTER);

            dialog.add(mainPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(frame);

            // Mark as displayed
            warning.setDisplayed(true);

            dialog.requestFocus();
            dialog.setVisible(true);
        });
    }

    /**
     * Helper method to style buttons consistently
     */
    private static void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 2),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
    }

    /**
     * Display all warnings from WarningManager in a modern dialog
     */
    public static void showAllWarnings(JFrame frame) {
        WarningManager warningManager = WarningManager.getInstance();
        List<Warning> warnings = warningManager.getAllWarnings();

        // Create modern dialog with theme support
        JDialog dialog = new JDialog(frame, UnicodeSymbols.WARNING + " Alle Warnungen", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(frame);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(ThemeManager.getBackgroundColor());

        // Header panel with theme colors
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.getCardBackgroundColor());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel titleLabel = new JLabel(UnicodeSymbols.WARNING + " Warnungen Übersicht");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel countLabel = new JLabel(warnings.size() + " Warnung(en)");
        countLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        countLabel.setForeground(ThemeManager.getTextSecondaryColor());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(countLabel, BorderLayout.EAST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        // Main content with table
        if (warnings.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setBackground(ThemeManager.getCardBackgroundColor());
            JLabel emptyLabel = new JLabel(UnicodeSymbols.CHECKMARK + " Keine Warnungen vorhanden");
            emptyLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 16));
            emptyLabel.setForeground(ThemeManager.getSuccessColor());
            emptyPanel.add(emptyLabel);
            dialog.add(emptyPanel, BorderLayout.CENTER);
        } else {
            // Create table model
            String[] columnNames = {
                    UnicodeSymbols.STATUS + " Status",
                    UnicodeSymbols.TAG + " Typ",
                    UnicodeSymbols.TITLE + " Titel",
                    UnicodeSymbols.MEMO + " Nachricht",
                    UnicodeSymbols.CALENDAR + " Datum"
            };
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // Populate table
            for (Warning warning : warnings) {
                String status = warning.isResolved() ? UnicodeSymbols.CHECKMARK + " Gelöst" : UnicodeSymbols.WARNING + " Offen";
                String type = switch (warning.getType()) {
                    case LOW_STOCK -> "Niedriger Bestand";
                    case ORDER_NEEDED -> "Bestellung nötig";
                    case CRITICAL_STOCK -> "Kritischer Bestand";
                    default -> "Sonstiges";
                };

                tableModel.addRow(new Object[]{
                        status,
                        type,
                        warning.getTitle(),
                        warning.getMessage(),
                        warning.getDate()
                });
            }

            JTable warningsTable = getWarningsTable(tableModel);

            // Alternating row colors with theme support
            warningsTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                                                               boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        // Set background based on row index
                        c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());

                        // Set foreground based on status
                        if (value.toString().contains("Gelöst")) {
                            setForeground(ThemeManager.getSuccessColor());
                        } else {
                            setForeground(ThemeManager.getWarningForegroundColor());
                        }
                    } else {
                        setForeground(ThemeManager.getSelectionForegroundColor());
                    }
                    setHorizontalAlignment(SwingConstants.CENTER);
                    return c;
                }
            });

            // Column widths
            warningsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
            warningsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
            warningsTable.getColumnModel().getColumn(2).setPreferredWidth(200);
            warningsTable.getColumnModel().getColumn(3).setPreferredWidth(300);
            warningsTable.getColumnModel().getColumn(4).setPreferredWidth(120);

            // Header styling with theme support
            JTableHeader header = warningsTable.getTableHeader();
            header.setBackground(ThemeManager.getTableHeaderBackgroundColor());
            header.setForeground(ThemeManager.getTableHeaderForegroundColor());
            header.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));

            JScrollPane scrollPane = new JScrollPane(warningsTable);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
            scrollPane.setBackground(ThemeManager.getCardBackgroundColor());

            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.setBackground(ThemeManager.getBackgroundColor());
            tablePanel.add(scrollPane, BorderLayout.CENTER);

            dialog.add(tablePanel, BorderLayout.CENTER);

            // Action panel at bottom with theme support
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            actionPanel.setBackground(ThemeManager.getBackgroundColor());
            actionPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

            JButton viewDetailsBtn = new JButton(UnicodeSymbols.INFO + " Details anzeigen");
            viewDetailsBtn.setToolTipText("Zeigt die Details der ausgewählten Warnung an");
            styleButton(viewDetailsBtn, new Color(65, 105, 225), Color.WHITE);
            viewDetailsBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                            "Bitte wählen Sie eine Warnung aus.",
                            "Keine Auswahl",
                            JOptionPane.WARNING_MESSAGE,
                            Main.iconSmall);
                    return;
                }
                Warning selectedWarning = warnings.get(selectedRow);
                displayWarning(frame, selectedWarning);
            });
            warningsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        int selectedRow = warningsTable.getSelectedRow();
                        if (selectedRow != -1) {
                            Warning selectedWarning = warnings.get(selectedRow);
                            displayWarning(frame, selectedWarning);
                        }
                    }
                }
            });

            JButton resolveBtn = new JButton(UnicodeSymbols.CHECKMARK + " Als gelöst markieren");
            resolveBtn.setToolTipText("Markiert die ausgewählte Warnung als gelöst");
            styleButton(resolveBtn, new Color(40, 180, 99), Color.WHITE);
            resolveBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                            "Bitte wählen Sie eine Warnung aus.",
                            "Keine Auswahl",
                            JOptionPane.WARNING_MESSAGE,
                            Main.iconSmall);
                    return;
                }
                Warning selectedWarning = warnings.get(selectedRow);
                if (selectedWarning.isResolved()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Diese Warnung wurde bereits gelöst.",
                            "Bereits gelöst",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                    return;
                }
                if (warningManager.resolveWarning(selectedWarning.getTitle())) {
                    tableModel.setValueAt(UnicodeSymbols.CHECKMARK + " Gelöst", selectedRow, 0);
                    JOptionPane.showMessageDialog(dialog,
                            "Warnung wurde als gelöst markiert.",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                }
            });

            JButton deleteBtn = new JButton(UnicodeSymbols.TRASH + " Löschen");
            deleteBtn.setToolTipText("Löscht die ausgewählte Warnung");
            styleButton(deleteBtn, new Color(231, 76, 60), Color.WHITE);
            deleteBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                            "Bitte wählen Sie eine Warnung aus.",
                            "Keine Auswahl",
                            JOptionPane.WARNING_MESSAGE,
                            Main.iconSmall);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Möchten Sie diese Warnung wirklich löschen?",
                        "Löschen bestätigen",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.iconSmall);
                if (confirm == JOptionPane.YES_OPTION) {
                    Warning selectedWarning = warnings.get(selectedRow);
                    if (warningManager.deleteWarning(selectedWarning.getTitle())) {
                        tableModel.removeRow(selectedRow);
                        warnings.remove(selectedRow);
                        countLabel.setText(warnings.size() + " Warnung(en)");
                        JOptionPane.showMessageDialog(dialog,
                                "Warnung wurde gelöscht.",
                                "Erfolg",
                                JOptionPane.INFORMATION_MESSAGE,
                                Main.iconSmall);
                    }
                }
            });

            JButton refreshBtn = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
            refreshBtn.setToolTipText("Aktualisiert die Warnungsliste");
            styleButton(refreshBtn, new Color(237, 242, 247), new Color(20, 30, 40));
            refreshBtn.addActionListener(e -> {
                dialog.dispose();
                showAllWarnings(frame);
            });

            JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
            closeBtn.setToolTipText("Schließt dieses Fenster");
            styleButton(closeBtn, new Color(149, 165, 166), Color.WHITE);
            closeBtn.addActionListener(e -> dialog.dispose());

            actionPanel.add(viewDetailsBtn);
            actionPanel.add(resolveBtn);
            actionPanel.add(deleteBtn);
            actionPanel.add(refreshBtn);
            actionPanel.add(closeBtn);

            dialog.add(actionPanel, BorderLayout.SOUTH);
        }

        // If no warnings, just show close button
        if (warnings.isEmpty()) {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
            buttonPanel.setBackground(ThemeManager.getBackgroundColor());
            JButton closeBtn = new JButton("Schließen");
            closeBtn.setToolTipText("Schließt dieses Fenster");
            styleButton(closeBtn, new Color(149, 165, 166), Color.WHITE);
            closeBtn.addActionListener(e -> dialog.dispose());
            buttonPanel.add(closeBtn);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
        }

        dialog.setVisible(true);
    }

    private static JTable getWarningsTable(DefaultTableModel tableModel) {
        JTable warningsTable = new JTable(tableModel);
        warningsTable.setRowHeight(32);
        warningsTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        warningsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        warningsTable.setShowGrid(true);
        warningsTable.setGridColor(ThemeManager.getTableGridColor());
        warningsTable.setIntercellSpacing(new Dimension(1, 1));
        warningsTable.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        warningsTable.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        warningsTable.setBackground(ThemeManager.getCardBackgroundColor());
        warningsTable.setForeground(ThemeManager.getTextPrimaryColor());
        return warningsTable;
    }
}
