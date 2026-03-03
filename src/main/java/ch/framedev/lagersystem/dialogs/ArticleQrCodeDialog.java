package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.guis.SupplierOrderGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.Map;

import static ch.framedev.lagersystem.main.Main.articleListGUI;
import static ch.framedev.lagersystem.utils.ArticleUtils.getHeaderPanel;

/**
 * Dialog for importing QR code scan data from the server.
 */
@SuppressWarnings("DuplicatedCode")
public final class ArticleQrCodeDialog {

    private ArticleQrCodeDialog() {
    }

    /**
     * Displays the Article QR Code Dialog, which allows users to view and manage QR code scan data retrieved from the server. The dialog includes a header with a title and subtitle, a content area with an info banner and a table displaying the QR code data, and a bottom action bar with buttons for refreshing data, importing selected or all entries, adding to the article list, removing from inventory, adding to supplier orders, deleting entries, and closing the dialog. The dialog is modal and will block interaction with the parent component until closed.
     *
     * @param parent the parent component relative to which the dialog will be displayed
     */
    public static void show(Component parent) {
        ThemeManager.applyUIDefaults();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "QR-Code Daten vom Server", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1150, 750);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setMinimumSize(new Dimension(900, 600));

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
                closeHeaderBtn.setBorder(null);
            }
        });
        closeHeaderBtn.addActionListener(e -> dialog.dispose());

        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(closeHeaderBtn, BorderLayout.EAST);
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

        JLabel infoLabel = new JLabel("Lädt QR-Code Scan-Daten vom Server. Bitte warten...");
        infoLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        infoLabel.setForeground(ThemeManager.getTextPrimaryColor());

        infoPanel.add(infoIcon);
        infoPanel.add(infoLabel);
        mainPanel.add(infoPanel, BorderLayout.NORTH);

        String[] columnNames = {UnicodeSymbols.CLOCK + " Timestamp", UnicodeSymbols.PACKAGE + " Artikel", UnicodeSymbols.CHART + " Menge", UnicodeSymbols.TAG + " Typ", UnicodeSymbols.PERSON + " Eigenverbrauch", UnicodeSymbols.ID + " ID"};
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
        qrTable.setGridColor(ThemeManager.getTableGridColor());
        qrTable.setIntercellSpacing(new Dimension(1, 1));
        qrTable.setSelectionBackground(ThemeManager.getTableSelectionColor());
        qrTable.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());

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

        qrTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        qrTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        qrTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        qrTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        qrTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        qrTable.getColumnModel().getColumn(5).setPreferredWidth(220);

        JTableHeader header = qrTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderColor());
        header.setForeground(ThemeManager.getTextOnPrimaryColor());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

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
        statusArea.setOpaque(false);

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

        JButton refreshBtn = new JButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        styleButton(refreshBtn, new Color(52, 152, 219), Color.WHITE);
        refreshBtn.setToolTipText("Lädt die QR-Code Daten erneut vom Server");

        JButton importBtn = new JButton(UnicodeSymbols.DOWNLOAD + " Ausgewählte einlagern");
        styleButton(importBtn, new Color(46, 204, 113), Color.WHITE);
        importBtn.setEnabled(false);
        importBtn.setToolTipText("Lagert die ausgewählten QR-Codes ein");

        JButton importAllBtn = new JButton(UnicodeSymbols.DOWNLOAD + " Alle einlagern");
        styleButton(importAllBtn, new Color(241, 196, 15), new Color(50, 50, 50));
        importAllBtn.setToolTipText("Lagert alle angezeigten QR-Codes ein");

        JButton addToArticleListBtn = new JButton(UnicodeSymbols.HEAVY_PLUS + " Zur Bestellliste");
        addToArticleListBtn.setToolTipText("Fügt die ausgewählten Artikel der Bestellliste hinzu");
        styleButton(addToArticleListBtn, new Color(155, 89, 182), Color.WHITE);
        addToArticleListBtn.addActionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie mindestens einen Datensatz aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }

            int added = 0;
            for (int row : selectedRows) {
                String data = (String) tableModel.getValueAt(row, 1);
                String artikelNr = QRCodeUtils.getPartsFromData(data)[0];
                artikelNr = artikelNr.replace("artikelNr:", "");
                int menge = Integer.parseInt(tableModel.getValueAt(row, 2).toString());

                Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                if (article != null) {
                    ArticleListGUI.addArticle(article, menge);
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

        JButton removeFromInventoryBtn = new JButton(UnicodeSymbols.TRASH + " Aus Inventar");
        removeFromInventoryBtn.setToolTipText("Entfernt den ausgewählten Artikel aus dem Inventar");
        styleButton(removeFromInventoryBtn, new Color(251, 163, 153), Color.WHITE);
        removeFromInventoryBtn.setEnabled(false);
        removeFromInventoryBtn.addActionListener(e -> {
            int selectedRow = qrTable.getSelectedRow();
            if (selectedRow == -1) return;

            String data = (String) tableModel.getValueAt(selectedRow, 1);
            String artikelNr = QRCodeUtils.getPartsFromData(data)[0];
            artikelNr = artikelNr.replace("artikelNr:", "");
            int menge = Integer.parseInt(tableModel.getValueAt(selectedRow, 2).toString());
            Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);

            if (article != null) {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        String.format("<html><b>%d Stück</b> von <b>\"%s\"</b><br/>aus dem Inventar entfernen?</html>",
                                menge, article.getName()),
                        "Entfernen bestätigen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        Main.iconSmall);

                if (confirm == JOptionPane.YES_OPTION) {
                    ArticleManager.getInstance().removeFromStock(artikelNr, menge);
                    String id = tableModel.getValueAt(selectedRow, 5).toString();
                    if (!ImportUtils.getImportedQrCodes().contains(id)) {
                        ImportUtils.addQrCodeImport(id);
                    }

                    statusLabel.setText(menge + " Stück von \"" + article.getName() + "\" entfernt");
                    statusIcon.setForeground(ThemeManager.getErrorColor());

                    ImportUtils.addToOwnUseList(data);
                }
            }
        });

        JButton addToOrderBtn = new JButton("Bestellung beim Lieferant hinzufügen");
        addToOrderBtn.setToolTipText("Fügt die ausgewählten Artikel der Bestellung beim Lieferant hinzu");
        styleButton(addToOrderBtn, new Color(52, 152, 219), Color.WHITE);
        addToOrderBtn.setEnabled(false);
        addToOrderBtn.addActionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie mindestens einen Datensatz aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int added = 0;
            for (int row : selectedRows) {
                String id = tableModel.getValueAt(row, 5).toString();
                String data = (String) tableModel.getValueAt(row, 1);
                String artikelNr = QRCodeUtils.getPartsFromData(data)[0];
                artikelNr = artikelNr.replace("artikelNr:", "");
                int menge = Integer.parseInt(tableModel.getValueAt(row, 2).toString());
                Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                if (article != null) {
                    SupplierOrderGUI.addArticleToSupplierOrder(article, menge);
                    ImportUtils.addQrCodeImport(id);
                    JOptionPane.showMessageDialog(dialog,
                            menge + " Stück von \"" + article.getName() + "\" zur Lieferantenbestellung hinzugefügt.",
                            "Artikel hinzugefügt",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                    added++;
                    refreshBtn.doClick();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Artikel mit Nummer \"" + artikelNr + "\" nicht im Inventar gefunden.",
                            "Artikel nicht gefunden",
                            JOptionPane.WARNING_MESSAGE,
                            Main.iconSmall);
                }
            }
            if (added > 0) {
                statusLabel.setText(added + " Artikel zur Lieferantenbestellung hinzugefügt");
                statusIcon.setForeground(ThemeManager.getAccentColor());
            }
        });

        JButton deleteBtn = new JButton(UnicodeSymbols.TRASH + " Datensätze löschen");
        deleteBtn.setToolTipText("Löscht die ausgewählten QR-Code Datensätze vom Server");
        styleButton(deleteBtn, new Color(231, 76, 60), Color.WHITE);
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie mindestens einen Datensatz aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Möchten Sie die ausgewählten Datensätze wirklich vom Server löschen?",
                    "Löschen bestätigen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                int deleted = 0;
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    int row = selectedRows[i];
                    String id = tableModel.getValueAt(row, 5).toString();
                    ImportUtils.addQrCodeImport(id);
                    tableModel.removeRow(row);
                    deleted++;
                }
                statusLabel.setText(deleted + " Datensätze gelöscht");
                statusIcon.setForeground(ThemeManager.getErrorColor());
            }
        });

        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE + " Schließen");
        closeBtn.setToolTipText("Schließt dieses Fenster");
        styleButton(closeBtn, new Color(149, 165, 166), Color.WHITE);
        closeBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(importBtn);
        importBtn.setVisible(false);
        buttonPanel.add(importAllBtn);
        importAllBtn.setVisible(false);
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

        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return QRCodeUtils.retrieveQrCodeDataFromWebsite();
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> qrCodeDataList = get();
                    int availableCount = 0;

                    if (qrCodeDataList == null || qrCodeDataList.isEmpty()) {
                        infoIcon.setText(UnicodeSymbols.WARNING);
                        infoLabel.setText("Keine QR-Code Daten vom Server erhalten");
                        infoPanel.setBackground(ThemeManager.withAlpha(ThemeManager.getWarningColor(), 30));
                        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(ThemeManager.getWarningColor(), 2),
                                BorderFactory.createEmptyBorder(12, 18, 12, 18)
                        ));
                        statusLabel.setText("Keine Daten verfügbar");
                        statusIcon.setForeground(ThemeManager.getWarningColor());
                        return;
                    }

                    for (Map<String, Object> dataMap : qrCodeDataList) {
                        if (dataMap.isEmpty()) continue;
                        Object[] rowData = new Object[6];
                        rowData[0] = dataMap.getOrDefault("timestamp", "N/A");
                        rowData[1] = dataMap.getOrDefault("data", "N/A");
                        rowData[2] = dataMap.getOrDefault("quantity", "N/A");
                        rowData[3] = dataMap.getOrDefault("type", "N/A");
                        rowData[4] = dataMap.getOrDefault("ownUse", "N/A");
                        rowData[5] = dataMap.getOrDefault("id", "N/A");
                        if (!ImportUtils.getImportedQrCodes().contains(rowData[5].toString())) {
                            tableModel.addRow(rowData);
                            availableCount++;
                        }
                    }

                    infoIcon.setText(UnicodeSymbols.CHECKMARK);
                    infoLabel.setText(availableCount + " QR-Code Datensätze erfolgreich geladen");
                    infoPanel.setBackground(ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 30));
                    infoPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getSuccessColor(), 2),
                            BorderFactory.createEmptyBorder(12, 18, 12, 18)
                    ));
                    statusLabel.setText(availableCount + " Datensätze bereit");
                    statusIcon.setForeground(ThemeManager.getSuccessColor());
                    importAllBtn.setEnabled(true);

                } catch (Exception ex) {
                    infoIcon.setText(UnicodeSymbols.CLOSE);
                    infoLabel.setText("Fehler beim Laden: " + ex.getMessage());
                    infoPanel.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 30));
                    infoPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getErrorColor(), 2),
                            BorderFactory.createEmptyBorder(12, 18, 12, 18)
                    ));
                    statusLabel.setText("Fehler beim Laden");
                    statusIcon.setForeground(ThemeManager.getErrorColor());
                }
            }
        };

        qrTable.getSelectionModel().addListSelectionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            int selectedCount = selectedRows.length;
            boolean hasSelection = selectedCount > 0;

            // Reset all buttons to a safe baseline
            importBtn.setEnabled(false);
            importBtn.setVisible(false);

            importAllBtn.setEnabled(tableModel.getRowCount() > 0);
            importAllBtn.setVisible(false);

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
                Object typeObj = qrTable.getValueAt(row, 3);
                String type = typeObj == null ? "" : typeObj.toString().trim();

                if (!type.equalsIgnoreCase("sell")) allSell = false;
                if (!type.equalsIgnoreCase("buy")) allBuy = false;
                if (!type.equalsIgnoreCase("order")) allOrder = false;

                Object ownUseObj = qrTable.getValueAt(row, 4);
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
                importAllBtn.setVisible(false);

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
                        int availableCount = 0;
                        if (qrCodeDataList != null && !qrCodeDataList.isEmpty()) {
                            for (Map<String, Object> dataMap : qrCodeDataList) {
                                if (dataMap.isEmpty()) {
                                    throw new IllegalArgumentException("dataMap is empty");
                                }
                                Object[] rowData = new Object[6];
                                rowData[0] = dataMap.getOrDefault("timestamp", "N/A");
                                rowData[1] = dataMap.getOrDefault("data", "N/A");
                                rowData[2] = dataMap.getOrDefault("quantity", "N/A");
                                rowData[3] = dataMap.getOrDefault("type", "N/A");
                                rowData[4] = dataMap.getOrDefault("ownUse", "N/A");
                                rowData[5] = dataMap.getOrDefault("id", "N/A");
                                if (!ImportUtils.getImportedQrCodes().contains(rowData[5].toString())) {
                                    tableModel.addRow(rowData);
                                    availableCount++;
                                }
                            }
                            infoIcon.setText(UnicodeSymbols.CHECKMARK);
                            infoLabel.setText(availableCount + " QR-Code Datensätze aktualisiert");
                            infoPanel.setBackground(ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 30));
                            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(ThemeManager.getSuccessColor(), 2),
                                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
                            ));
                            statusLabel.setText(availableCount + " Datensätze bereit");
                            statusIcon.setForeground(ThemeManager.getSuccessColor());
                            importAllBtn.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        infoIcon.setText(UnicodeSymbols.CLOSE);
                        infoLabel.setText("Fehler beim Aktualisieren: " + ex.getMessage());
                        infoPanel.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 30));
                        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(ThemeManager.getErrorColor(), 2),
                                BorderFactory.createEmptyBorder(12, 18, 12, 18)
                        ));
                        statusLabel.setText("Fehler beim Laden");
                        statusIcon.setForeground(ThemeManager.getErrorColor());
                    }
                }
            };
            refreshWorker.execute();
        });

        importBtn.addActionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            if (selectedRows.length == 0) return;

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    String.format("<html><b>%d Datensätze</b> importieren?<br/><small>Artikel werden ins Lager eingebucht.</small></html>",
                            selectedRows.length),
                    "Import bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall);

            if (confirm == JOptionPane.YES_OPTION) {
                int imported = 0;
                int errors = 0;
                ArticleManager articleManager = ArticleManager.getInstance();

                for (int row : selectedRows) {
                    try {
                        String id = (String) tableModel.getValueAt(row, 5);
                        String data = (String) tableModel.getValueAt(row, 1);
                        int quantity = Integer.parseInt(tableModel.getValueAt(row, 2).toString());

                        String[] parts = QRCodeUtils.getPartsFromData(data);
                        String artikelNr = parts[0].replace("artikelNr:", "");
                        Article article = articleManager.getArticleByNumber(artikelNr);

                        if (article != null && !ImportUtils.getImportedQrCodes().contains(id)) {
                            if (articleManager.addToStock(artikelNr, quantity)) {
                                ImportUtils.addQrCodeImport(id);
                                imported++;
                            } else {
                                errors++;
                            }
                        } else if (article == null) {
                            Article newArticle = retrieveParts(parts);
                            if (articleManager.insertArticle(newArticle)) {
                                if (articleManager.addToStock(artikelNr, quantity)) {
                                    ImportUtils.addQrCodeImport(id);
                                    imported++;
                                } else {
                                    errors++;
                                }
                            } else {
                                errors++;
                            }
                        } else {
                            errors++;
                        }
                    } catch (Exception ex) {
                        errors++;
                    }
                }

                String message = String.format("<html><b>Import abgeschlossen</b><br/><br/>" +
                                UnicodeSymbols.CHECKMARK + " Erfolgreich: %d<br/>" +
                                (errors > 0 ? UnicodeSymbols.CLOSE + " Fehler: %d<br/>" : ""),
                        imported, errors);

                JOptionPane.showMessageDialog(dialog,
                        message,
                        "Import Ergebnis",
                        errors > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

                statusLabel.setText(imported + " Datensätze importiert" + (errors > 0 ? " (" + errors + " Fehler)" : ""));
                statusIcon.setForeground(errors > 0 ? ThemeManager.getWarningColor() : ThemeManager.getSuccessColor());
            }
        });

        importAllBtn.addActionListener(e -> {
            int rowCount = tableModel.getRowCount();
            if (rowCount == 0) return;

            int confirm = JOptionPane.showConfirmDialog(dialog,
                    String.format("<html><b>Alle %d Datensätze</b> importieren?<br/><small>Dies kann einige Sekunden dauern.</small></html>",
                            rowCount),
                    "Import bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall);

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

                    @Override
                    protected Void doInBackground() {
                        ArticleManager articleManager = ArticleManager.getInstance();

                        for (int i = 0; i < rowCount; i++) {
                            try {
                                String data = (String) tableModel.getValueAt(i, 1);
                                int quantity = Integer.parseInt(tableModel.getValueAt(i, 2).toString());
                                String id = (String) tableModel.getValueAt(i, 5);

                                if (ImportUtils.getImportedQrCodes().contains(id)) {
                                    skipped++;
                                    continue;
                                }

                                String[] parts = QRCodeUtils.getPartsFromData(data);
                                String artikelNr = parts[0].replace("artikelNr:", "");
                                Article article = articleManager.getArticleByNumber(artikelNr);

                                if (article != null) {
                                    if (articleManager.addToStock(artikelNr, quantity)) {
                                        ImportUtils.addQrCodeImport(id);
                                        imported++;
                                        publish(imported);
                                    } else {
                                        errors++;
                                    }
                                } else {
                                    Article newArticle = retrieveParts(parts);
                                    if (articleManager.insertArticle(newArticle)) {
                                        if (articleManager.addToStock(artikelNr, quantity)) {
                                            ImportUtils.addQrCodeImport(id);
                                            imported++;
                                        } else {
                                            errors++;
                                        }
                                    } else {
                                        errors++;
                                    }
                                }
                            } catch (Exception ex) {
                                errors++;
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
                        String message = String.format(
                                "<html><b>Import abgeschlossen</b><br/><br/>" +
                                        UnicodeSymbols.CHECKMARK + " Erfolgreich: %d<br/>" +
                                        (skipped > 0 ? UnicodeSymbols.FAST_FORWARD + "️ Übersprungen: %d (bereits importiert)<br/>" : "") +
                                        (errors > 0 ? UnicodeSymbols.CLOSE + " Fehler: %d<br/>" : ""),
                                imported, skipped, errors);

                        JOptionPane.showMessageDialog(dialog,
                                message,
                                "Import Ergebnis",
                                errors > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE,
                                Main.iconSmall);

                        statusLabel.setText(String.format("%d importiert%s",
                                imported,
                                (errors > 0 ? " (" + errors + " Fehler)" : "")));
                        statusIcon.setForeground(errors > 0 ? ThemeManager.getWarningColor() : ThemeManager.getSuccessColor());

                        importAllBtn.setEnabled(true);
                        refreshBtn.setEnabled(true);
                    }
                };

                importWorker.execute();
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        worker.execute();

        dialog.setVisible(true);
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
        String artikelNr = parts[0].replace("artikelNr:", "");
        String name = parts[1].replace("name:", "");
        String details = parts[2].replace("details:", "");
        String sellPrice = parts[3].replace("sellPrice:", "");
        String buyPrice = parts[4].replace("buyPrice:", "");
        String vendor = parts[5].replace("vendor:", "");
        return new Article(artikelNr, name, details, 0, 0,
                Double.parseDouble(sellPrice), Double.parseDouble(buyPrice), vendor);
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
}
