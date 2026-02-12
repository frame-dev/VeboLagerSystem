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

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.List;
import java.util.Map;

import static ch.framedev.lagersystem.main.Main.articleListGUI;

/**
 * Dialog for importing QR code scan data from the server.
 */
public final class ArticleQrCodeDialog {

    private ArticleQrCodeDialog() {
    }

    public static void show(Component parent) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "QR-Code Daten vom Server", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(1150, 750);
        dialog.setLocationRelativeTo(parent);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setMinimumSize(new Dimension(900, 600));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 58, 95));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 30, 24, 30));

        JPanel headerLeft = new JPanel();
        headerLeft.setLayout(new BoxLayout(headerLeft, BoxLayout.Y_AXIS));
        headerLeft.setOpaque(false);

        JLabel titleLabel = new JLabel(UnicodeSymbols.PHONE + " QR-Code Daten von Server");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Gescannte Artikel vom mobilen Scanner importieren");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(200, 220, 240));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        headerLeft.add(titleLabel);
        headerLeft.add(Box.createVerticalStrut(5));
        headerLeft.add(subtitleLabel);

        headerPanel.add(headerLeft, BorderLayout.WEST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        ArticleGUI.RoundedPanel infoPanel = new ArticleGUI.RoundedPanel(new Color(240, 248, 255), 8);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 181, 246), 2),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)
        ));
        infoPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JLabel infoIcon = new JLabel("ℹ️");
        infoIcon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 18));

        JLabel infoLabel = new JLabel("Lädt QR-Code Scan-Daten vom Server. Bitte warten...");
        infoLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        infoLabel.setForeground(new Color(52, 73, 94));

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
        qrTable.setGridColor(new Color(230, 235, 240));
        qrTable.setIntercellSpacing(new Dimension(1, 1));
        qrTable.setSelectionBackground(new Color(100, 181, 246));
        qrTable.setSelectionForeground(Color.WHITE);

        qrTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                    setForeground(new Color(52, 73, 94));
                }
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
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
        header.setBackground(new Color(30, 58, 95));
        header.setForeground(Color.WHITE);
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        header.setPreferredSize(new Dimension(header.getWidth(), 40));

        JScrollPane scrollPane = new JScrollPane(qrTable);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        scrollPane.getViewport().setBackground(Color.WHITE);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new BorderLayout(10, 0));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JPanel statusArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusArea.setOpaque(false);

        JLabel statusIcon = new JLabel(UnicodeSymbols.CIRCLE);
        statusIcon.setFont(SettingsGUI.getFontByName(Font.PLAIN, 16));
        statusIcon.setForeground(new Color(100, 200, 100));

        JLabel statusLabel = new JLabel("Bereit zum Importieren");
        statusLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        statusLabel.setForeground(new Color(70, 80, 90));

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
                statusIcon.setForeground(new Color(52, 152, 219));
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
                    statusIcon.setForeground(new Color(231, 76, 60));

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
                statusIcon.setForeground(new Color(52, 152, 219));
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
                statusIcon.setForeground(new Color(231, 76, 60));
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
                        infoPanel.setBackground(new Color(255, 245, 230));
                        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(255, 193, 7), 2),
                                BorderFactory.createEmptyBorder(12, 18, 12, 18)
                        ));
                        statusLabel.setText("Keine Daten verfügbar");
                        statusIcon.setForeground(new Color(255, 193, 7));
                        return;
                    }

                    for (Map<String, Object> dataMap : qrCodeDataList) {
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
                    infoPanel.setBackground(new Color(230, 255, 240));
                    infoPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                            BorderFactory.createEmptyBorder(12, 18, 12, 18)
                    ));
                    statusLabel.setText(availableCount + " Datensätze bereit");
                    statusIcon.setForeground(new Color(46, 204, 113));
                    importAllBtn.setEnabled(true);

                } catch (Exception ex) {
                    infoIcon.setText(UnicodeSymbols.CLOSE);
                    infoLabel.setText("Fehler beim Laden: " + ex.getMessage());
                    infoPanel.setBackground(new Color(255, 235, 238));
                    infoPanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                            BorderFactory.createEmptyBorder(12, 18, 12, 18)
                    ));
                    statusLabel.setText("Fehler beim Laden");
                    statusIcon.setForeground(new Color(231, 76, 60));
                }
            }
        };

        qrTable.getSelectionModel().addListSelectionListener(e -> {
            int[] selectedRows = qrTable.getSelectedRows();
            int selectedCount = selectedRows.length;
            boolean hasSelection = selectedCount > 0;

            importBtn.setEnabled(false);
            removeFromInventoryBtn.setEnabled(false);
            importAllBtn.setEnabled(tableModel.getRowCount() > 0);
            addToArticleListBtn.setVisible(false);
            addToOrderBtn.setEnabled(false);
            addToOrderBtn.setVisible(false);

            boolean hasSellType = false;
            boolean hasBuyType = false;
            boolean hasOrderType = false;
            boolean ownUse = false;

            if (hasSelection) {
                ownUse = Boolean.parseBoolean(String.valueOf(qrTable.getValueAt(selectedRows[0], 4)));
                boolean allSell = true;
                boolean allBuy = true;
                boolean allOrder = true;

                for (int row : selectedRows) {
                    String type = qrTable.getValueAt(row, 3).toString();
                    if (!type.equalsIgnoreCase("sell")) allSell = false;
                    if (!type.equalsIgnoreCase("buy")) allBuy = false;
                    if (!type.equalsIgnoreCase("order")) allOrder = false;
                }
                hasSellType = allSell;
                hasBuyType = allBuy;
                hasOrderType = allOrder;
            }

            if (selectedCount == 1 && ownUse) {
                removeFromInventoryBtn.setEnabled(true);
                removeFromInventoryBtn.setVisible(true);
                importBtn.setEnabled(false);
                importBtn.setVisible(false);
                importAllBtn.setEnabled(false);
                importAllBtn.setVisible(false);
                addToArticleListBtn.setVisible(false);
                addToOrderBtn.setEnabled(false);
                statusLabel.setText("1 Eigenverbrauch-Datensatz ausgewählt");
            } else if (hasSelection && hasBuyType) {
                importBtn.setEnabled(true);
                importBtn.setVisible(true);
                removeFromInventoryBtn.setEnabled(false);
                removeFromInventoryBtn.setVisible(false);
                importAllBtn.setEnabled(true);
                importAllBtn.setVisible(true);
                addToArticleListBtn.setVisible(false);
                addToOrderBtn.setEnabled(true);
                statusLabel.setText(selectedCount + " Einlagern-Datensatz" + (selectedCount > 1 ? "e" : "") + " ausgewählt");
            } else if (hasSelection && hasSellType) {
                removeFromInventoryBtn.setEnabled(false);
                removeFromInventoryBtn.setVisible(false);
                addToArticleListBtn.setVisible(true);
                importBtn.setEnabled(false);
                importBtn.setVisible(false);
                importAllBtn.setEnabled(false);
                importAllBtn.setVisible(false);
                addToOrderBtn.setEnabled(false);
                statusLabel.setText(selectedCount + " Verkauf-Datensatz" + (selectedCount > 1 ? "e" : "") + " ausgewählt");
            } else if (hasSelection && hasOrderType) {
                removeFromInventoryBtn.setEnabled(false);
                removeFromInventoryBtn.setVisible(false);
                addToOrderBtn.setVisible(true);
                addToOrderBtn.setEnabled(true);
                addToArticleListBtn.setVisible(false);
                importBtn.setVisible(false);
                importAllBtn.setVisible(false);
                statusLabel.setText(selectedCount + " Bestell-Datensatz " + selectedCount + " ausgewählt");
            } else {
                importBtn.setEnabled(false);
                removeFromInventoryBtn.setEnabled(false);
                importAllBtn.setEnabled(tableModel.getRowCount() > 0);
                addToArticleListBtn.setVisible(true);
                addToOrderBtn.setEnabled(false);
                statusLabel.setText(hasSelection ? "Gemischte Auswahl" : "Bereit zum Importieren");
            }
        });

        refreshBtn.addActionListener(e -> {
            tableModel.setRowCount(0);
            infoIcon.setText(UnicodeSymbols.INFO);
            infoLabel.setText("Lädt QR-Code Scan-Daten vom Server. Bitte warten...");
            infoPanel.setBackground(new Color(240, 248, 255));
            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(100, 181, 246), 2),
                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
            ));
            statusLabel.setText("Aktualisiere Daten...");
            statusIcon.setForeground(new Color(100, 181, 246));
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
                            infoPanel.setBackground(new Color(230, 255, 240));
                            infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(46, 204, 113), 2),
                                    BorderFactory.createEmptyBorder(12, 18, 12, 18)
                            ));
                            statusLabel.setText(availableCount + " Datensätze bereit");
                            statusIcon.setForeground(new Color(46, 204, 113));
                            importAllBtn.setEnabled(true);
                        }
                    } catch (Exception ex) {
                        infoIcon.setText(UnicodeSymbols.CLOSE);
                        infoLabel.setText("Fehler beim Aktualisieren: " + ex.getMessage());
                        infoPanel.setBackground(new Color(255, 235, 238));
                        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(231, 76, 60), 2),
                                BorderFactory.createEmptyBorder(12, 18, 12, 18)
                        ));
                        statusLabel.setText("Fehler beim Laden");
                        statusIcon.setForeground(new Color(231, 76, 60));
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
                statusIcon.setForeground(errors > 0 ? new Color(255, 193, 7) : new Color(46, 204, 113));
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
                statusIcon.setForeground(new Color(100, 181, 246));
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
                        statusIcon.setForeground(errors > 0 ? new Color(255, 193, 7) : new Color(46, 204, 113));

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

    private static Article retrieveParts(String[] parts) {
        String artikelNr = parts[0].replace("artikelNr:", "");
        String name = parts[1].replace("name:", "");
        String details = parts[2].replace("details:", "");
        String sellPrice = parts[3].replace("sellPrice:", "");
        String buyPrice = parts[4].replace("buyPrice:", "");
        String vendor = parts[5].replace("vendor:", "");
        return new Article(artikelNr, name, details, 0, 0,
                Double.parseDouble(buyPrice), Double.parseDouble(sellPrice), vendor);
    }

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
}
