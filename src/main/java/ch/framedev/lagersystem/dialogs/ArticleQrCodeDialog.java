package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.SupplierOrderGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ch.framedev.lagersystem.utils.ArticleUtils.getHeaderPanel;
import static ch.framedev.lagersystem.main.Main.articleListGUI;
import static ch.framedev.lagersystem.main.Main.logUtils;

/**
 * Dialog for importing QR code scan data from the server.
 */
public final class ArticleQrCodeDialog {

    private static final Dimension DIALOG_SIZE = new Dimension(1150, 750);
    private static final Dimension DIALOG_MIN_SIZE = new Dimension(900, 600);

    private static final int COL_TIMESTAMP = 0;
    private static final int COL_DATA = 1;
    private static final int COL_QUANTITY = 2;
    private static final int COL_TYPE = 3;
    private static final int COL_OWN_USE = 4;
    private static final int COL_SIZE = 5;
    private static final int COL_COLOR = 6;
    private static final int COL_ID = 7;

    private ArticleQrCodeDialog() {
    }

    /**
     * Displays the Article QR Code Dialog, which allows users to view and manage QR code scan data retrieved from the server. The dialog includes a header with a title and subtitle, a content area with an info banner and a table displaying the QR code data, and a bottom action bar with buttons for refreshing data, importing selected or all entries, adding to the article list, removing from inventory, adding to supplier orders, deleting entries, and closing the dialog. The dialog is modal and will block interaction with the parent component until closed.
     *
     * @param parent the parent component relative to which the dialog will be displayed
     */
    public static void show(Component parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent component must not be null");
        }
        ThemeManager.applyUIDefaults();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "QR-Code Daten vom Server", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(DIALOG_SIZE);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setMinimumSize(DIALOG_MIN_SIZE);

        // ===== Header (gradient like other dialogs) =====
        JPanel headerPanel = getHeaderPanel();
        headerPanel.setLayout(new BorderLayout());

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setOpaque(false);

        JLabel titleLabel = new JLabel(UnicodeSymbols.PHONE + " QR-Code Daten von Server");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gescannte Artikel vom mobilen Scanner importieren");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        subtitleLabel.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerLeft.add(titleLabel);
        headerLeft.add(Box.createVerticalStrut(5));
        headerLeft.add(subtitleLabel);

        // Close button (top-right)
        JButton closeHeaderBtn = new JButton(UnicodeSymbols.CLOSE);
        closeHeaderBtn.setToolTipText("Schließen");
        closeHeaderBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        closeHeaderBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeHeaderBtn.setBorderPainted(false);
        closeHeaderBtn.setFocusPainted(false);
        closeHeaderBtn.setContentAreaFilled(false);
        closeHeaderBtn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        closeHeaderBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        closeHeaderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeHeaderBtn.setPreferredSize(new Dimension(44, 44));
        closeHeaderBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                closeHeaderBtn.setForeground(ThemeManager.getTextOnPrimaryColor());
                closeHeaderBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 120));
                closeHeaderBtn.setContentAreaFilled(true);
                closeHeaderBtn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                closeHeaderBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
                closeHeaderBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
                closeHeaderBtn.setContentAreaFilled(false);
                closeHeaderBtn.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }
        });
        closeHeaderBtn.addActionListener(e -> dialog.dispose());

        JPanel headerMeta = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerMeta.setOpaque(false);
        headerMeta.add(createHeaderBadge(UnicodeSymbols.CLOUD + " Live Sync", ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 70)));
        headerMeta.add(createHeaderBadge(UnicodeSymbols.REFRESH + " F5 Refresh", ThemeManager.withAlpha(ThemeManager.getAccentColor(), 70)));
        headerMeta.add(closeHeaderBtn);

        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(headerMeta, BorderLayout.EAST);
        dialog.add(headerPanel, BorderLayout.NORTH);

        // ===== Content area =====
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(ThemeManager.getBackgroundColor());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // Info banner (rounded card)
        ArticleGUI.RoundedPanel infoPanel = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 4));

        JLabel infoIcon = new JLabel(UnicodeSymbols.INFO);
        infoIcon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 18));
        infoIcon.setForeground(ThemeManager.getAccentColor());

        JLabel infoLabel = new JLabel("Lädt QR-Code Scan-Daten vom Server. Bitte warten...");
        infoLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        infoLabel.setForeground(ThemeManager.getTextPrimaryColor());

        infoPanel.add(infoIcon);
        infoPanel.add(infoLabel);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        String[] columnNames = {UnicodeSymbols.CLOCK + " Timestamp", UnicodeSymbols.PACKAGE + " Artikel", UnicodeSymbols.CHART + " Menge", UnicodeSymbols.TAG + " Typ", UnicodeSymbols.PERSON + " Eigenverbrauch", UnicodeSymbols.SORT + " Größe", UnicodeSymbols.COLOR_PALETTE + " Farbe", UnicodeSymbols.ID + " ID"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable qrTable = new JTable(tableModel);
        qrTable.setRowHeight(32);
        qrTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        qrTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        qrTable.setShowGrid(true);
        qrTable.setShowVerticalLines(false);
        qrTable.setRowMargin(0);
        qrTable.setGridColor(ThemeManager.getTableGridColor());
        qrTable.setIntercellSpacing(new Dimension(1, 1));
        qrTable.setFillsViewportHeight(true);
        qrTable.setSelectionBackground(ThemeManager.getTableSelectionColor());
        qrTable.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());
        qrTable.setAutoCreateRowSorter(true);

        qrTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color EVEN = ThemeManager.getTableRowEvenColor();
            private final Color ODD = ThemeManager.getTableRowOddColor();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? EVEN : ODD);
                    c.setForeground(ThemeManager.getTextPrimaryColor());
                } else {
                    c.setBackground(ThemeManager.getTableSelectionColor());
                    c.setForeground(ThemeManager.getTextOnPrimaryColor());
                }
                setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                return c;
            }
        });

        qrTable.getColumnModel().getColumn(COL_TIMESTAMP).setPreferredWidth(170);
        qrTable.getColumnModel().getColumn(COL_DATA).setPreferredWidth(280);
        qrTable.getColumnModel().getColumn(COL_QUANTITY).setPreferredWidth(90);
        qrTable.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(110);
        qrTable.getColumnModel().getColumn(COL_OWN_USE).setPreferredWidth(140);
        qrTable.getColumnModel().getColumn(COL_SIZE).setPreferredWidth(100);
        qrTable.getColumnModel().getColumn(COL_COLOR).setPreferredWidth(100);
        qrTable.getColumnModel().getColumn(COL_ID).setPreferredWidth(220);

        JTableHeader header = qrTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderColor());
        header.setForeground(ThemeManager.getTextOnPrimaryColor());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.withAlpha(ThemeManager.getBorderColor(), 180)));

        ArticleGUI.RoundedPanel tableCard = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        tableCard.setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(qrTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        tableCard.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(tableCard, BorderLayout.CENTER);
        dialog.add(mainPanel, BorderLayout.CENTER);

        // ===== Bottom action bar (rounded card) =====
        ArticleGUI.RoundedPanel actionPanel = new ArticleGUI.RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        actionPanel.setLayout(new BorderLayout(10, 0));
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JPanel statusArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusArea.setOpaque(true);
        statusArea.setBackground(ThemeManager.withAlpha(ThemeManager.getAccentColor(), 18));
        statusArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ThemeManager.withAlpha(ThemeManager.getAccentColor(), 80), 1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JLabel statusIcon = new JLabel(UnicodeSymbols.CIRCLE);
        statusIcon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 16));
        statusIcon.setForeground(ThemeManager.getSuccessColor());

        JLabel statusLabel = new JLabel("Bereit zum Importieren");
        statusLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        statusLabel.setForeground(ThemeManager.getTextPrimaryColor());

        statusArea.add(statusIcon);
        statusArea.add(statusLabel);
        actionPanel.add(statusArea, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        Color refreshColor = new Color(52, 152, 219);
        Color stockInColor = new Color(46, 204, 113);
        Color importAllColor = new Color(241, 196, 15);
        Color listColor = new Color(155, 89, 182);
        Color removeColor = new Color(251, 163, 153);
        Color orderColor = new Color(52, 152, 219);
        Color deleteColor = new Color(231, 76, 60);
        Color closeColor = new Color(149, 165, 166);

        JButton refreshBtn = createActionButton(UnicodeSymbols.REFRESH + " Aktualisieren",
            "Lädt die QR-Code Daten erneut vom Server",
            refreshColor,
            Color.WHITE);
        refreshBtn.setToolTipText("Lädt die QR-Code Daten erneut vom Server");

        JButton importBtn = createActionButton(UnicodeSymbols.DOWNLOAD + " Ausgewählte einlagern",
            "Lagert die ausgewählten QR-Codes ein",
            stockInColor,
            Color.WHITE);
        importBtn.setEnabled(false);
        importBtn.setToolTipText("Lagert die ausgewählten QR-Codes ein");

        JButton importAllBtn = createActionButton(UnicodeSymbols.DOWNLOAD + " Alle einlagern",
            "Lagert alle angezeigten QR-Codes ein",
            importAllColor,
            new Color(50, 50, 50));
        importAllBtn.setToolTipText("Lagert alle angezeigten QR-Codes ein");

        JButton addToArticleListBtn = createActionButton(UnicodeSymbols.HEAVY_PLUS + " Zur Bestellliste",
            "Fügt die ausgewählten Artikel der Bestellliste hinzu",
            listColor,
            Color.WHITE);
        addToArticleListBtn.setToolTipText("Fügt die ausgewählten Artikel der Bestellliste hinzu");
        addToArticleListBtn.addActionListener(e -> {
            int[] selectedRows = getSelectedModelRows(qrTable);
            if (selectedRows.length == 0) {
                new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte wählen Sie mindestens einen Datensatz aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
                return;
            }

            int added = 0;
            for (int row : selectedRows) {
                String data = (String) tableModel.getValueAt(row, COL_DATA);
                String artikelNr = extractArticleNumber(data);
                int menge = Integer.parseInt(tableModel.getValueAt(row, COL_QUANTITY).toString());

                Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                if (article != null) {
                    String picked = tableModel.getValueAt(row, COL_SIZE).toString();
                    String color = tableModel.getValueAt(row, COL_COLOR).toString();
                    ArticleListGUI.addArticle(article, menge, picked, color, null);
                    added++;
                }
            }

            if (added > 0) {
                if (articleListGUI == null) {
                    articleListGUI = new ArticleListGUI();
                    articleListGUI.setVisible(true);
                    articleListGUI.requestFocus();
                } else {
                    articleListGUI.refreshArticleList();
                    articleListGUI.toFront();
                }

                statusLabel.setText(added + " Artikel zur Bestellliste hinzugefügt");
                statusIcon.setForeground(ThemeManager.getAccentColor());
            }
        });

        JButton removeFromInventoryBtn = createActionButton(UnicodeSymbols.TRASH + " Aus Inventar",
            "Entfernt den ausgewählten Artikel aus dem Inventar",
            removeColor,
            Color.WHITE);
        removeFromInventoryBtn.setToolTipText("Entfernt den ausgewählten Artikel aus dem Inventar");
        removeFromInventoryBtn.setEnabled(false);
        removeFromInventoryBtn.addActionListener(e -> {
            int selectedViewRow = qrTable.getSelectedRow();
            if (selectedViewRow == -1) return;

            int selectedRow = qrTable.convertRowIndexToModel(selectedViewRow);

            String data = (String) tableModel.getValueAt(selectedRow, COL_DATA);
            String artikelNr = extractArticleNumber(data);
            int menge = Integer.parseInt(tableModel.getValueAt(selectedRow, COL_QUANTITY).toString());
            String id = tableModel.getValueAt(selectedRow, COL_ID).toString();
            Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);

            if (article != null) {
                if (ImportUtils.getImportedQrCodes().contains(id)) {
                    new MessageDialog()
                            .setTitle("Bereits verarbeitet")
                            .setMessage("Dieser Datensatz wurde bereits verarbeitet und kann nicht erneut entfernt werden.")
                            .setMessageType(JOptionPane.WARNING_MESSAGE)
                            .display();
                    return;
                }

                int confirm = new MessageDialog()
                        .setTitle("Entfernen bestätigen")
                        .setMessage(String.format("<html><b>%d Stück</b> von <b>\"%s\"</b><br/>aus dem Inventar entfernen?</html>",
                                menge, article.getName()))
                        .setMessageType(JOptionPane.QUESTION_MESSAGE)
                        .setOptionType(JOptionPane.YES_NO_OPTION)
                        .displayWithOptions();

                if (confirm == JOptionPane.YES_OPTION) {
                    boolean removed = ArticleManager.getInstance().removeFromStock(artikelNr, menge);
                    if (!removed) {
                        new MessageDialog()
                                .setTitle("Fehler")
                                .setMessage("Der Artikel konnte nicht aus dem Inventar entfernt werden.")
                                .setMessageType(JOptionPane.ERROR_MESSAGE)
                                .display();
                        return;
                    }

                    ImportUtils.addQrCodeImport(id);
                    ImportUtils.addToOwnUseList(data);
                    tableModel.removeRow(selectedRow);
                    statusLabel.setText(menge + " Stück von \"" + article.getName() + "\" entfernt");
                    statusIcon.setForeground(ThemeManager.getErrorColor());
                    Main.logUtils.addLog("Artikel " + artikelNr + " wurde aus dem Inventar enfernt. Menge: " + menge);
                }
            }
        });

        JButton addToOrderBtn = createActionButton(UnicodeSymbols.PACKAGE + " Lieferantenbestellung",
            "Fügt die ausgewählten Artikel der Bestellung beim Lieferant hinzu",
            orderColor,
            Color.WHITE);
        addToOrderBtn.setToolTipText("Fügt die ausgewählten Artikel der Bestellung beim Lieferant hinzu");
        addToOrderBtn.setEnabled(false);
        addToOrderBtn.addActionListener(e -> {
            int[] selectedRows = getSelectedModelRows(qrTable);
            if (selectedRows.length == 0) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie mindestens einen Datensatz aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            int added = 0;
            int notFound = 0;
            for (int row : selectedRows) {
                String id = tableModel.getValueAt(row, COL_ID).toString();
                String data = (String) tableModel.getValueAt(row, COL_DATA);
                String artikelNr = extractArticleNumber(data);
                int menge = Integer.parseInt(tableModel.getValueAt(row, COL_QUANTITY).toString());
                Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                if (article != null) {
                    SupplierOrderGUI.addArticleToSupplierOrder(article, menge);
                    ImportUtils.addQrCodeImport(id);
                    added++;
                } else {
                    notFound++;
                }
            }
            if (added > 0) {
                statusLabel.setText(added + " Artikel zur Lieferantenbestellung hinzugefügt");
                statusIcon.setForeground(ThemeManager.getAccentColor());
                refreshBtn.doClick();
                logUtils.addLog(added + " Artikel zur Lieferantenbestellung hinzugefügt");
            }

            if (added > 0 || notFound > 0) {
                StringBuilder result = new StringBuilder("<html><b>Bestellung aktualisiert</b><br/><br/>")
                        .append(UnicodeSymbols.CHECKMARK)
                        .append(" Hinzugefügt: ")
                        .append(added)
                        .append("<br/>");
                if (notFound > 0) {
                    result.append(UnicodeSymbols.WARNING)
                            .append(" Nicht gefunden: ")
                            .append(notFound)
                            .append("<br/>");
                }
                result.append("</html>");

                new MessageDialog()
                        .setTitle("Ergebnis")
                        .setMessage(result.toString())
                        .setMessageType(notFound > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE)
                        .display();
            }
        });

        JButton deleteBtn = createActionButton(UnicodeSymbols.TRASH + " Datensätze löschen",
            "Löscht die ausgewählten QR-Code Datensätze vom Server",
            deleteColor,
            Color.WHITE);
        deleteBtn.setToolTipText("Löscht die ausgewählten QR-Code Datensätze vom Server");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> {
            int[] selectedRows = getSelectedModelRows(qrTable);
            if (selectedRows.length == 0) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie mindestens einen Datensatz aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            int confirm = new MessageDialog()
                    .setTitle("Löschen bestätigen")
                    .setMessage("Möchten Sie die ausgewählten Datensätze wirklich vom Server löschen?")
                    .setMessageType(JOptionPane.QUESTION_MESSAGE)
                    .setOptionType(JOptionPane.YES_NO_OPTION)
                    .displayWithOptions();
            if (confirm == JOptionPane.YES_OPTION) {
                int deleted = 0;
                Arrays.sort(selectedRows);
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int row = selectedRows[i];
                    String id = tableModel.getValueAt(row, COL_ID).toString();
                    ImportUtils.addQrCodeImport(id);
                    tableModel.removeRow(row);
                    deleted++;
                }
                statusLabel.setText(deleted + " Datensätze gelöscht");
                statusIcon.setForeground(ThemeManager.getErrorColor());
                logUtils.addLog(deleted + " Datensätze gelöscht");
            }
        });

        JButton closeBtn = createActionButton(UnicodeSymbols.CLOSE + " Schließen",
            "Schließt dieses Fenster",
            closeColor,
            Color.WHITE);
        closeBtn.setToolTipText("Schließt dieses Fenster");
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(importBtn);
        importBtn.setVisible(false);
        buttonPanel.add(importAllBtn);
        importAllBtn.setVisible(true);
        buttonPanel.add(addToArticleListBtn);
        addToArticleListBtn.setVisible(false);
        buttonPanel.add(removeFromInventoryBtn);
        removeFromInventoryBtn.setVisible(false);
        buttonPanel.add(addToOrderBtn);
        addToOrderBtn.setVisible(false);
        buttonPanel.add(deleteBtn);
        deleteBtn.setVisible(false);
        buttonPanel.add(closeBtn);

        actionPanel.add(buttonPanel, BorderLayout.EAST);
        dialog.add(actionPanel, BorderLayout.SOUTH);

        // Keyboard shortcuts for faster operator workflow
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.getRootPane().registerKeyboardAction(
                e -> refreshBtn.doClick(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return QRCodeUtils.retrieveQrCodeDataFromWebsite();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> qrCodeDataList = get();
                    applyQrDataLoadResult(
                            tableModel,
                            qrCodeDataList,
                            infoPanel,
                            infoIcon,
                            infoLabel,
                            statusLabel,
                            statusIcon,
                            importAllBtn,
                            "QR-Code Datensätze erfolgreich geladen");

                } catch (Exception ex) {
                    applyQrDataLoadFailure(
                            infoPanel,
                            infoIcon,
                            infoLabel,
                            statusLabel,
                            statusIcon,
                            importAllBtn,
                            "Fehler beim Laden: " + ex.getMessage());
                }
            }
        };

        qrTable.getSelectionModel().addListSelectionListener(e -> {
            int[] selectedRows = getSelectedModelRows(qrTable);
            int selectedCount = selectedRows.length;
            boolean hasSelection = selectedCount > 0;

            // Reset all buttons to a safe baseline
            importBtn.setEnabled(false);
            importBtn.setVisible(false);

            importAllBtn.setEnabled(tableModel.getRowCount() > 0);
            importAllBtn.setVisible(true);

            addToArticleListBtn.setEnabled(false);
            addToArticleListBtn.setVisible(false);

            removeFromInventoryBtn.setEnabled(false);
            removeFromInventoryBtn.setVisible(false);

            addToOrderBtn.setEnabled(false);
            addToOrderBtn.setVisible(false);

            deleteBtn.setEnabled(false);
            deleteBtn.setVisible(false);

            if (!hasSelection) {
                statusLabel.setText("Bereit zum Importieren");
                statusIcon.setForeground(ThemeManager.getSuccessColor());
                return;
            }

            // Determine selection composition
            boolean allSell = true;
            boolean allBuy = true;
            boolean allOrder = true;
            boolean allOwnUseTrue = true;
            boolean allOwnUseFalse = true;

            for (int row : selectedRows) {
                Object typeObj = tableModel.getValueAt(row, COL_TYPE);
                String type = typeObj == null ? "" : typeObj.toString().trim();

                if (!type.equalsIgnoreCase("sell")) allSell = false;
                if (!type.equalsIgnoreCase("buy")) allBuy = false;
                if (!type.equalsIgnoreCase("order")) allOrder = false;

                Object ownUseObj = tableModel.getValueAt(row, COL_OWN_USE);
                boolean ownUseVal = parseOwnUse(ownUseObj);
                if (!ownUseVal) allOwnUseTrue = false;
                if (ownUseVal) allOwnUseFalse = false;
            }

            boolean hasSellType = allSell;
            boolean hasBuyType = allBuy;
            boolean hasOrderType = allOrder;

            // Rules:
            // 1) Remove-from-inventory only makes sense for EXACTLY ONE row, type=sell, ownUse=true
            if (selectedCount == 1 && hasSellType && allOwnUseTrue) {
                removeFromInventoryBtn.setEnabled(true);
                removeFromInventoryBtn.setVisible(true);

                deleteBtn.setEnabled(true);
                deleteBtn.setVisible(true);

                importAllBtn.setEnabled(false);
                importAllBtn.setVisible(true);

                statusLabel.setText("1 Eigenverbrauch-Datensatz ausgewählt");
                statusIcon.setForeground(ThemeManager.getErrorColor());
                return;
            }

            // 2) BUY: allow import selected, import all, supplier order, delete
            if (hasBuyType) {
                importBtn.setEnabled(true);
                importBtn.setVisible(true);

                importAllBtn.setEnabled(tableModel.getRowCount() > 0);
                importAllBtn.setVisible(true);

                addToOrderBtn.setEnabled(true);
                addToOrderBtn.setVisible(true);

                deleteBtn.setEnabled(true);
                deleteBtn.setVisible(true);

                statusLabel.setText(selectedCount + " Einlagern-Datensatz" + (selectedCount > 1 ? "e" : "") + " ausgewählt");
                statusIcon.setForeground(ThemeManager.getSuccessColor());
                return;
            }

            // 3) SELL (non-own-use): allow add-to-orderlist + delete
            // Own-use sell rows are handled by rule (1) (single selection) or considered mixed for safety.
            if (hasSellType && allOwnUseFalse) {
                addToArticleListBtn.setEnabled(true);
                addToArticleListBtn.setVisible(true);

                importAllBtn.setVisible(false);
                importAllBtn.setEnabled(false);
                importBtn.setVisible(false);
                importBtn.setEnabled(false);

                deleteBtn.setEnabled(true);
                deleteBtn.setVisible(true);

                statusLabel.setText(selectedCount + " Verkauf-Datensatz" + (selectedCount > 1 ? "e" : "") + " ausgewählt");
                statusIcon.setForeground(ThemeManager.getAccentColor());
                return;
            }

            // 4) ORDER: allow supplier order + delete
            if (hasOrderType) {
                addToOrderBtn.setEnabled(true);
                addToOrderBtn.setVisible(true);

                deleteBtn.setEnabled(true);
                deleteBtn.setVisible(true);

                statusLabel.setText(selectedCount + " Bestell-Datensatz" + (selectedCount > 1 ? "e" : "") + " ausgewählt");
                statusIcon.setForeground(ThemeManager.getAccentColor());
                return;
            }

            // 5) Mixed selection: only deletion is safe
            deleteBtn.setEnabled(true);
            deleteBtn.setVisible(true);
            statusLabel.setText("Gemischte Auswahl");
            statusIcon.setForeground(ThemeManager.getWarningColor());
        });

        refreshBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            infoIcon.setText(UnicodeSymbols.INFO);
            infoLabel.setText("Lädt QR-Code Scan-Daten vom Server. Bitte warten...");
            infoPanel.setBackground(ThemeManager.getCardBackgroundColor());
            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getAccentColor(), 2),
                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
            ));
            statusLabel.setText("Aktualisiere Daten...");
            statusIcon.setForeground(ThemeManager.getAccentColor());
            refreshBtn.setEnabled(false);
            importBtn.setEnabled(false);
            importAllBtn.setEnabled(false);
            removeFromInventoryBtn.setEnabled(false);
            addToOrderBtn.setEnabled(false);

            SwingWorker<List<Map<String, Object>>, Void> refreshWorker = new SwingWorker<>() {
                @Override
                protected List<Map<String, Object>> doInBackground() {
                    return QRCodeUtils.retrieveQrCodeDataFromWebsite();
                }

                @Override
                protected void done() {
                    try {
                        List<Map<String, Object>> qrCodeDataList = get();
                        applyQrDataLoadResult(
                                tableModel,
                                qrCodeDataList,
                                infoPanel,
                                infoIcon,
                                infoLabel,
                                statusLabel,
                                statusIcon,
                                importAllBtn,
                                "QR-Code Datensätze aktualisiert");
                    } catch (Exception ex) {
                        applyQrDataLoadFailure(
                                infoPanel,
                                infoIcon,
                                infoLabel,
                                statusLabel,
                                statusIcon,
                                importAllBtn,
                                "Fehler beim Aktualisieren: " + ex.getMessage());
                    } finally {
                        refreshBtn.setEnabled(true);
                    }
                }
            };
            refreshWorker.execute();
        });

        importBtn.addActionListener(e -> {
            int[] selectedRows = getSelectedModelRows(qrTable);
            if (selectedRows.length == 0) return;

            int confirm = new MessageDialog()
                    .setTitle("Import bestätigen")
                    .setMessage(String.format("<html><b>%d Datensätze</b> importieren?<br/><small>Artikel werden ins Lager eingebucht.</small></html>",
                            selectedRows.length))
                    .setMessageType(JOptionPane.QUESTION_MESSAGE)
                    .setOptionType(JOptionPane.YES_NO_OPTION)
                    .displayWithOptions();

            if (confirm == JOptionPane.YES_OPTION) {
                int imported = 0;
                int errors = 0;
                List<String> errorDetails = new ArrayList<>();
                ArticleManager articleManager = ArticleManager.getInstance();

                for (int row : selectedRows) {
                    String id = "";
                    String data = "";
                    String artikelNr = "";
                    try {
                        id = (String) tableModel.getValueAt(row, COL_ID);
                        data = (String) tableModel.getValueAt(row, COL_DATA);
                        int quantity = Integer.parseInt(tableModel.getValueAt(row, COL_QUANTITY).toString());

                        String[] parts = QRCodeUtils.getPartsFromData(data);
                        artikelNr = extractArticleNumber(parts);
                        Article article = articleManager.getArticleByNumber(artikelNr);

                        if (article != null && !ImportUtils.getImportedQrCodes().contains(id)) {
                            if (articleManager.addToStock(artikelNr, quantity)) {
                                ImportUtils.addQrCodeImport(id);
                                imported++;
                            } else {
                                errors++;
                                recordQrImportFailure(errorDetails, id, artikelNr, data,
                                        "Bestand konnte nicht aktualisiert werden", null);
                            }
                        } else if (article == null) {
                            try {
                                Article newArticle = retrieveParts(parts);
                                if (articleManager.insertArticle(newArticle)) {
                                    if (articleManager.addToStock(artikelNr, quantity)) {
                                        ImportUtils.addQrCodeImport(id);
                                        imported++;
                                    } else {
                                        errors++;
                                        recordQrImportFailure(errorDetails, id, artikelNr, data,
                                                "Unbekannte Artikel-Nr.; Artikel wurde angelegt, Bestand konnte aber nicht aktualisiert werden", null);
                                    }
                                } else {
                                    errors++;
                                    recordQrImportFailure(errorDetails, id, artikelNr, data,
                                            "Unbekannte Artikel-Nr.; Artikel konnte nicht automatisch angelegt werden", null);
                                }
                            } catch (Exception ex) {
                                errors++;
                                recordQrImportFailure(errorDetails, id, artikelNr, data,
                                        "Unbekannte Artikel-Nr.; QR-Daten konnten nicht als neuer Artikel gelesen werden", ex);
                            }
                        } else {
                            errors++;
                            recordQrImportFailure(errorDetails, id, artikelNr, data,
                                    "Datensatz wurde bereits verarbeitet", null);
                        }
                    } catch (Exception ex) {
                        errors++;
                        recordQrImportFailure(errorDetails, id, artikelNr, data,
                                "Datensatz konnte nicht importiert werden", ex);
                    }
                }

                String message = buildImportResultMessage(imported, 0, errors, errorDetails);

                new MessageDialog()
                        .setTitle("Import Ergebnis")
                        .setMessage(message)
                        .setMessageType(errors > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE)
                        .displayWithOptions();

                statusLabel.setText(imported + " Datensätze importiert" + (errors > 0 ? " (" + errors + " Fehler)" : ""));
                statusIcon.setForeground(errors > 0 ? ThemeManager.getWarningColor() : ThemeManager.getSuccessColor());
                refreshBtn.doClick();
                logUtils.addLog(imported + " Datensätze importiert" + (errors > 0 ? " (" + errors + " Fehler)" : ""));
            }
        });

        importAllBtn.addActionListener(e -> {
            int rowCount = tableModel.getRowCount();
            if (rowCount == 0) return;

            int confirm = new MessageDialog()
                    .setTitle("Import bestätigen")
                    .setMessage(String.format("<html><b>Alle %d Datensätze</b> importieren?<br/><small>Dies kann einige Sekunden dauern.</small></html>",
                            rowCount))
                    .setMessageType(JOptionPane.QUESTION_MESSAGE)
                    .setOptionType(JOptionPane.YES_NO_OPTION)
                    .displayWithOptions();

            if (confirm == JOptionPane.YES_OPTION) {
                statusLabel.setText("Importiere " + rowCount + " Datensätze...");
                statusIcon.setForeground(ThemeManager.getAccentColor());
                importAllBtn.setEnabled(false);
                importBtn.setEnabled(false);
                refreshBtn.setEnabled(false);
                removeFromInventoryBtn.setEnabled(false);
                addToOrderBtn.setEnabled(false);

                SwingWorker<Void, Integer> importWorker = new SwingWorker<>() {
                    int imported = 0;
                    int errors = 0;
                    int skipped = 0;
                    final List<String> errorDetails = new ArrayList<>();

                    @Override
                    protected Void doInBackground() {
                        ArticleManager articleManager = ArticleManager.getInstance();

                        for (int i = 0; i < rowCount; i++) {
                            String data = "";
                            String id = "";
                            String artikelNr = "";
                            try {
                                data = (String) tableModel.getValueAt(i, COL_DATA);
                                int quantity = Integer.parseInt(tableModel.getValueAt(i, COL_QUANTITY).toString());
                                id = (String) tableModel.getValueAt(i, COL_ID);

                                if (ImportUtils.getImportedQrCodes().contains(id)) {
                                    skipped++;
                                    continue;
                                }

                                String[] parts = QRCodeUtils.getPartsFromData(data);
                                artikelNr = extractArticleNumber(parts);
                                Article article = articleManager.getArticleByNumber(artikelNr);

                                if (article != null) {
                                    if (articleManager.addToStock(artikelNr, quantity)) {
                                        ImportUtils.addQrCodeImport(id);
                                        imported++;
                                        publish(imported);
                                    } else {
                                        errors++;
                                        recordQrImportFailure(errorDetails, id, artikelNr, data,
                                                "Bestand konnte nicht aktualisiert werden", null);
                                    }
                                } else {
                                    try {
                                        Article newArticle = retrieveParts(parts);
                                        if (articleManager.insertArticle(newArticle)) {
                                            if (articleManager.addToStock(artikelNr, quantity)) {
                                                ImportUtils.addQrCodeImport(id);
                                                imported++;
                                            } else {
                                                errors++;
                                                recordQrImportFailure(errorDetails, id, artikelNr, data,
                                                        "Unbekannte Artikel-Nr.; Artikel wurde angelegt, Bestand konnte aber nicht aktualisiert werden", null);
                                            }
                                        } else {
                                            errors++;
                                            recordQrImportFailure(errorDetails, id, artikelNr, data,
                                                    "Unbekannte Artikel-Nr.; Artikel konnte nicht automatisch angelegt werden", null);
                                        }
                                    } catch (Exception ex) {
                                        errors++;
                                        recordQrImportFailure(errorDetails, id, artikelNr, data,
                                                "Unbekannte Artikel-Nr.; QR-Daten konnten nicht als neuer Artikel gelesen werden", ex);
                                    }
                                }
                            } catch (Exception ex) {
                                errors++;
                                recordQrImportFailure(errorDetails, id, artikelNr, data,
                                        "Datensatz konnte nicht importiert werden", ex);
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void process(List<Integer> chunks) {
                        if (!chunks.isEmpty()) {
                            int latest = chunks.getLast();
                            statusLabel.setText("Importiert: " + latest + " / " + rowCount);
                        }
                    }

                    @Override
                    protected void done() {
                        String message = buildImportResultMessage(imported, skipped, errors, errorDetails);

                        new MessageDialog()
                                .setTitle("Import Ergebnis")
                                .setMessage(message)
                                .setMessageType(errors > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE)
                                .displayWithOptions();

                        statusLabel.setText(String.format("%d importiert%s",
                                imported,
                                (errors > 0 ? " (" + errors + " Fehler)" : "")));
                        statusIcon.setForeground(errors > 0 ? ThemeManager.getWarningColor() : ThemeManager.getSuccessColor());
                        Main.logUtils.addLog(String.format("%d importiert%s",
                                imported,
                                (errors > 0 ? " (" + errors + " Fehler)" : "")));

                        importAllBtn.setEnabled(true);
                        importBtn.setEnabled(false);
                        refreshBtn.setEnabled(true);
                        refreshBtn.doClick();
                    }
                };

                importWorker.execute();
            }
        });

        worker.execute();

        dialog.setVisible(true);
    }

    private static void setInfoPanelState(
            JPanel infoPanel,
            JLabel infoIcon,
            JLabel infoLabel,
            String iconText,
            String message,
            Color accentColor
    ) {
        infoIcon.setText(iconText);
        infoLabel.setText(message);
        infoPanel.setBackground(ThemeManager.withAlpha(accentColor, 30));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor, 2),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
    }

    private static void applyQrDataLoadResult(
            DefaultTableModel tableModel,
            List<Map<String, Object>> qrCodeDataList,
            JPanel infoPanel,
            JLabel infoIcon,
            JLabel infoLabel,
            JLabel statusLabel,
            JLabel statusIcon,
            JButton importAllBtn,
            String successMessageSuffix
    ) {
        if (qrCodeDataList == null || qrCodeDataList.isEmpty()) {
            setInfoPanelState(infoPanel, infoIcon, infoLabel, UnicodeSymbols.WARNING,
                    "Keine QR-Code Daten vom Server erhalten", ThemeManager.getWarningColor());
            statusLabel.setText("Keine Daten verfügbar");
            statusIcon.setForeground(ThemeManager.getWarningColor());
            importAllBtn.setEnabled(false);
            return;
        }

        int availableCount = populateTableWithAvailableRows(tableModel, qrCodeDataList);
        setInfoPanelState(infoPanel, infoIcon, infoLabel, UnicodeSymbols.CHECKMARK,
                availableCount + " " + successMessageSuffix, ThemeManager.getSuccessColor());
        statusLabel.setText(availableCount + " Datensätze bereit");
        statusIcon.setForeground(ThemeManager.getSuccessColor());
        importAllBtn.setEnabled(availableCount > 0);
    }

    private static void applyQrDataLoadFailure(
            JPanel infoPanel,
            JLabel infoIcon,
            JLabel infoLabel,
            JLabel statusLabel,
            JLabel statusIcon,
            JButton importAllBtn,
            String errorMessage
    ) {
        setInfoPanelState(infoPanel, infoIcon, infoLabel, UnicodeSymbols.CLOSE,
                errorMessage, ThemeManager.getErrorColor());
        statusLabel.setText("Fehler beim Laden");
        statusIcon.setForeground(ThemeManager.getErrorColor());
        importAllBtn.setEnabled(false);
    }

    private static int populateTableWithAvailableRows(DefaultTableModel tableModel, List<Map<String, Object>> qrCodeDataList) {
        if (tableModel == null || qrCodeDataList == null || qrCodeDataList.isEmpty()) {
            return 0;
        }

        Set<String> importedIds = new HashSet<>(ImportUtils.getImportedQrCodes());
        int availableCount = 0;

        for (Map<String, Object> dataMap : qrCodeDataList) {
            if (dataMap == null || dataMap.isEmpty()) {
                continue;
            }
            Object[] rowData = createRowData(dataMap);
            String rowId = rowData[COL_ID].toString();
            if (!importedIds.contains(rowId)) {
                tableModel.addRow(rowData);
                availableCount++;
            }
        }

        return availableCount;
    }

    private static Object[] createRowData(Map<String, Object> dataMap) {
        Object[] rowData = new Object[8];
        rowData[COL_TIMESTAMP] = dataMap.getOrDefault("timestamp", "N/A");
        rowData[COL_DATA] = dataMap.getOrDefault("data", "N/A");
        rowData[COL_QUANTITY] = dataMap.getOrDefault("quantity", "N/A");
        rowData[COL_TYPE] = dataMap.getOrDefault("type", "N/A");
        rowData[COL_OWN_USE] = dataMap.getOrDefault("ownUse", "N/A");
        rowData[COL_SIZE] = dataMap.getOrDefault("size", "N/A");
        rowData[COL_COLOR] = dataMap.getOrDefault("color", "N/A");
        rowData[COL_ID] = dataMap.getOrDefault("id", "N/A");
        return rowData;
    }

    private static boolean parseOwnUse(Object ownUseObj) {
        switch (ownUseObj) {
            case null -> {
                return false;
            }
            case Boolean b -> {
                return b;
            }
            case Number n -> {
                return n.intValue() != 0;
            }
            default -> {
            }
        }

        String s = ownUseObj.toString().trim();
        if (s.isEmpty()) return false;

        // Truthy Werte (Backend/Locale)
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("yes") || s.equalsIgnoreCase("y")) return true;
        if (s.equalsIgnoreCase("ja")) return true;
        return s.equals("1");
    }

    private static Article retrieveParts(String[] parts) {
        String artikelNr = extractArticleNumber(parts);
        String name = parts[1].replace("name:", "");
        String details = parts[2].replace("details:", "");
        String sellPrice = parts[3].replace("sellPrice:", "");
        String buyPrice = parts[4].replace("buyPrice:", "");
        String vendor = parts[5].replace("vendor:", "");
        return new Article(artikelNr, name, details, 0, 0,
                Double.parseDouble(sellPrice), Double.parseDouble(buyPrice), vendor);
    }

    private static void recordQrImportFailure(List<String> errorDetails, String qrId, String articleNumber,
                                              String qrValue, String reason, Exception exception) {
        String detail = QRCodeUtils.buildQrImportFailureMessage(
                articleNumber,
                qrValue,
                qrId,
                exception == null ? reason : reason + " (" + exception.getMessage() + ")");
        Main.logUtils.addLog(Level.ERROR, detail);
        if (errorDetails != null) {
            errorDetails.add(detail);
        }
    }

    private static String buildImportResultMessage(int imported, int skipped, int errors, List<String> errorDetails) {
        StringBuilder message = new StringBuilder("<html><b>Import abgeschlossen</b><br/><br/>")
                .append(UnicodeSymbols.CHECKMARK)
                .append(" Erfolgreich: ")
                .append(imported)
                .append("<br/>");
        if (skipped > 0) {
            message.append(UnicodeSymbols.FAST_FORWARD)
                    .append(" Übersprungen: ")
                    .append(skipped)
                    .append(" (bereits importiert)<br/>");
        }
        if (errors > 0) {
            message.append(UnicodeSymbols.CLOSE)
                    .append(" Fehler: ")
                    .append(errors)
                    .append("<br/>");
        }
        if (errorDetails != null && !errorDetails.isEmpty()) {
            message.append("<br/><b>Fehlerdetails</b><br/>");
            int maxDetails = Math.min(5, errorDetails.size());
            for (int i = 0; i < maxDetails; i++) {
                message.append("<small>")
                        .append(escapeHtml(errorDetails.get(i)))
                        .append("</small><br/>");
            }
            if (errorDetails.size() > maxDetails) {
                message.append("<small>Weitere Fehler wurden im Log gespeichert.</small><br/>");
            }
        }
        message.append("</html>");
        return message.toString();
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void styleButton(JButton button, Color bgColor, Color fgColor) {
        if (button == null) return;
        Color hoverBg = ThemeManager.getButtonHoverColor(bgColor);
        Color pressedBg = ThemeManager.getButtonPressedColor(bgColor);

        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        // remove previous listeners to avoid stacking styles
        for (MouseListener ml : button.getMouseListeners()) {
            if (ml.getClass().getName().contains("ArticleQrCodeDialog")) {
                button.removeMouseListener(ml);
            }
        }

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 16, 10, 16)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(bgColor.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 16, 10, 16)
                ));
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : bgColor);
            }
        });
    }

    private static JButton createActionButton(String text, String tooltip, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        styleButton(button, bgColor, fgColor);
        return button;
    }

    private static JComponent createHeaderBadge(String text, Color borderColor) {
        JLabel badge = new JLabel(text);
        badge.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        badge.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 220));
        badge.setOpaque(true);
        badge.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 26));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 1),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)
        ));
        return badge;
    }

    private static int[] getSelectedModelRows(JTable table) {
        if (table == null) {
            return new int[0];
        }
        int[] selectedViewRows = table.getSelectedRows();
        int[] selectedModelRows = new int[selectedViewRows.length];
        for (int i = 0; i < selectedViewRows.length; i++) {
            selectedModelRows[i] = table.convertRowIndexToModel(selectedViewRows[i]);
        }
        return selectedModelRows;
    }

    private static String extractArticleNumber(String data) {
        if (data == null || data.isBlank()) {
            return "";
        }
        return extractArticleNumber(QRCodeUtils.getPartsFromData(data));
    }

    private static String extractArticleNumber(String[] parts) {
        if (parts == null || parts.length == 0 || parts[0] == null) {
            return "";
        }
        return parts[0].replace("artikelNr:", "");
    }
}
