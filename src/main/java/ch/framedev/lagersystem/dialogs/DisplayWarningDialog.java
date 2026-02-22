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
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

public class DisplayWarningDialog {

    private DisplayWarningDialog() {
    }

    /**
     * Displays a warning in a modern, styled dialog.
     *
     * @param warning The warning object to display
     */
    public static void displayWarning(JFrame frame, Warning warning) {
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(frame, "Warnung", Dialog.ModalityType.APPLICATION_MODAL);
            dialog.setUndecorated(true);
            dialog.setLayout(new BorderLayout());

            // Determine palette based on warning type
            WarningPalette palette = paletteFor(warning);

            JPanel chrome = createDialogChrome(ThemeManager.getBackgroundColor(), 18);
            chrome.setLayout(new BorderLayout());

            JPanel header = createGradientHeader(
                    palette.headerA(),
                    palette.headerB(),
                    palette.icon() + "  " + warning.getType().getDisplayName(),
                    UnicodeSymbols.INFO + " Details",
                    dialog
            );
            chrome.add(header, BorderLayout.NORTH);

            ArticleGUI.RoundedPanel contentCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
            contentCard.setLayout(new GridBagLayout());
            contentCard.setBorder(BorderFactory.createEmptyBorder(22, 22, 18, 22));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets = new Insets(0, 0, 12, 0);

            // Title
            JLabel titleLabel = new JLabel(warning.getTitle());
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
            contentCard.add(titleLabel, gbc);

            // Message (scrollable when large)
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 14, 0);
            JTextArea messageArea = new JTextArea(warning.getMessage());
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setEditable(false);
            messageArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            messageArea.setForeground(ThemeManager.getTextPrimaryColor());
            messageArea.setBackground(ThemeManager.getCardBackgroundColor());
            messageArea.setBorder(null);

            JScrollPane messageScroll = new JScrollPane(messageArea);
            messageScroll.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            messageScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
            messageScroll.setBackground(ThemeManager.getCardBackgroundColor());
            messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            messageScroll.getVerticalScrollBar().setUnitIncrement(14);
            messageScroll.setPreferredSize(new Dimension(520, 130));
            contentCard.add(messageScroll, gbc);

            // Meta row
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 10, 0);
            JPanel metaRow = new JPanel(new BorderLayout(10, 0));
            metaRow.setOpaque(false);

            JLabel dateLabel = new JLabel(UnicodeSymbols.CALENDAR + " Datum: " + warning.getDate());
            dateLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            dateLabel.setForeground(ThemeManager.getTextSecondaryColor());
            metaRow.add(dateLabel, BorderLayout.WEST);

            JLabel statusChip = createStatusChip(warning.isResolved());
            metaRow.add(statusChip, BorderLayout.EAST);

            contentCard.add(metaRow, gbc);

            // Buttons
            gbc.gridy++;
            gbc.insets = new Insets(8, 0, 0, 0);
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
                    JOptionPane.showMessageDialog(dialog,
                            "Die Warnung wurde als gelöst markiert.",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                    dialog.dispose();
                });
                buttonBar.add(resolveBtn);
            }

            JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
            closeBtn.setToolTipText("Schließt dieses Fenster");
            styleActionButton(closeBtn, palette.accent(), Color.WHITE);
            closeBtn.addActionListener(e -> dialog.dispose());
            buttonBar.add(closeBtn);

            contentCard.add(buttonBar, gbc);

            chrome.add(contentCard, BorderLayout.CENTER);

            dialog.setContentPane(chrome);
            dialog.getRootPane().setDefaultButton(closeBtn);
            installEscToClose(dialog);

            // Mark as displayed
            warning.setDisplayed(true);

            dialog.pack();
            dialog.setMinimumSize(new Dimension(620, 420));
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
        });
    }

    /**
     * Display all warnings from WarningManager in a modern dialog.
     */
    public static void showAllWarnings(JFrame frame) {
        WarningManager warningManager = WarningManager.getInstance();
        List<Warning> warnings = warningManager.getAllWarnings();

        JDialog dialog = new JDialog(frame, UnicodeSymbols.WARNING + " Alle Warnungen", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setLayout(new BorderLayout());

        JPanel chrome = createDialogChrome(ThemeManager.getBackgroundColor(), 18);
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
            ArticleGUI.RoundedPanel emptyCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
            emptyCard.setBorder(BorderFactory.createEmptyBorder(34, 30, 34, 30));
            emptyCard.setLayout(new GridBagLayout());

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

            dialog.setSize(920, 620);
            dialog.setLocationRelativeTo(frame);
            dialog.setVisible(true);
            return;
        }

        // Table model
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
                    w.getMessage(),
                    w.getDate()
            });
        }

        JTable table = getWarningsTable(tableModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        // Render: alternating rows; status color only in column 0
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
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

                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                setHorizontalAlignment(column == 3 ? SwingConstants.LEFT : SwingConstants.CENTER);
                return c;
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

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(16, 16, 10, 16),
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true)
        ));
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.setBackground(ThemeManager.getBackgroundColor());

        chrome.add(scrollPane, BorderLayout.CENTER);

        // Footer actions
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

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

        viewDetailsBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Warning selected = warnings.get(modelRow);
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
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Warning selected = warnings.get(modelRow);

            if (selected.isResolved()) {
                JOptionPane.showMessageDialog(dialog,
                        "Diese Warnung wurde bereits gelöst.",
                        "Bereits gelöst",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);
                return;
            }

            if (warningManager.resolveWarning(selected.getTitle())) {
                selected.setResolved(true);
                tableModel.setValueAt(UnicodeSymbols.CHECKMARK + " Gelöst", modelRow, 0);
                JOptionPane.showMessageDialog(dialog,
                        "Warnung wurde als gelöst markiert.",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);
            }
        });

        deleteBtn.addActionListener(e -> {
            int viewRow = table.getSelectedRow();
            if (viewRow == -1) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Möchten Sie diese Warnung wirklich löschen?",
                    "Löschen bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall);
            if (confirm != JOptionPane.YES_OPTION) return;

            Warning selected = warnings.get(modelRow);
            if (warningManager.deleteWarning(selected.getTitle())) {
                tableModel.removeRow(modelRow);
                warnings.remove(modelRow);
                dialog.dispose();
                showAllWarnings(frame);
            }
        });

        refreshBtn.addActionListener(e -> {
            dialog.dispose();
            showAllWarnings(frame);
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        footer.add(viewDetailsBtn);
        footer.add(resolveBtn);
        footer.add(deleteBtn);
        footer.add(refreshBtn);
        footer.add(closeBtn);

        chrome.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(chrome);
        dialog.getRootPane().setDefaultButton(viewDetailsBtn);
        installEscToClose(dialog);

        dialog.setSize(980, 650);
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static JTable getWarningsTable(DefaultTableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(ThemeManager.getTableGridColor());
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        return table;
    }

    // ------------------------ UI helpers ------------------------

    private static void installEscToClose(JDialog dialog) {
        JRootPane root = dialog.getRootPane();
        root.registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private static JPanel createDialogChrome(Color bg, int radius) {
        ArticleGUI.RoundedPanel chrome = new ArticleGUI.RoundedPanel(bg, radius);
        chrome.setOpaque(false);

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

    private static JPanel createGradientHeader(Color a, Color b, String title, String subtitle, JDialog dialog) {
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

        JButton closeBtn = createHeaderCloseButton(dialog);
        header.add(closeBtn, BorderLayout.EAST);

        return header;
    }

    private static JButton createHeaderCloseButton(JDialog dialog) {
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
                close.setBorder(null);
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
}
