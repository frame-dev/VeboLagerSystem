package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.QRCodeUtils;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ch.framedev.lagersystem.main.Main.articleListGUI;

/**
 * TODO: Cache the article list in memory to avoid reloading from DB on every change
 */
@SuppressWarnings("unused")
public class ArticleGUI extends JFrame {

    private final JTable articleTable;
    // scroll pane reference so we can read viewport width on resize
    private final JScrollPane tableScrollPane;
    // base column widths (used as relative weights when resizing)
    private final int[] baseColumnWidths = new int[]{150, 260, 340, 110, 110, 150, 150, 200};
    private final JLabel countLabel;

    public ArticleGUI() {
        setTitle("Artikel Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Initialize table early so toolbar actions can reference it
        articleTable = new JTable();
        initializeTable();

        // Header (rounded white pill)
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        // soft neutral background that's easy on the eyes
        headerWrapper.setBackground(new Color(245, 247, 250));
        RoundedPanel headerPanel = new RoundedPanel(new Color(255, 255, 255), 20);
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setPreferredSize(new Dimension(760, 64));
        JLabel title = new JLabel("Artikel Verwaltung");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        // dark but soft title color
        title.setForeground(new Color(31, 45, 61));
        headerPanel.add(title);
        headerWrapper.add(headerPanel);

        // Toolbar with rounded buttons centered
        JPanel toolbarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 18));
        toolbarWrapper.setBackground(new Color(236, 239, 241));

        JButton addArticleButton = createRoundedButton("Artikel hinzufügen");
        addArticleButton.addActionListener(e -> {
            Object[] row = showAddArticleDialog();
            if (row != null) {
                addArticleRow(row);
                ArticleManager articleManager = ArticleManager.getInstance();
                Article article = new Article(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (Integer) row[3],
                        (Integer) row[4],
                        (Double) row[5],
                        (Double) row[6],
                        (String) row[7]
                );
                if (!articleManager.insertArticle(article)) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Hinzufügen des Artikels. Möglicherweise existiert die Artikelnummer bereits.", "Fehler", JOptionPane.ERROR_MESSAGE);
                    loadArticles(); // Refresh to remove the local addition
                } else {
                    loadArticles(); // Refresh to ensure data consistency
                    JOptionPane.showMessageDialog(this, "Artikel erfolgreich hinzugefügt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JButton editArticleButton = createRoundedButton("Artikel bearbeiten");
        editArticleButton.addActionListener(e -> {
            int selectedRow = articleTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object[] existingData = ((DefaultTableModel) articleTable.getModel()).getDataVector().elementAt(selectedRow).toArray();
            Object[] updatedRow = showUpdateArticleDialog(existingData);
            if (updatedRow != null) {
                DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
                for (int i = 0; i < updatedRow.length; i++) {
                    model.setValueAt(updatedRow[i], selectedRow, i);
                }
                ArticleManager articleManager = ArticleManager.getInstance();
                Article article = new Article(
                        (String) updatedRow[0],
                        (String) updatedRow[1],
                        (String) updatedRow[2],
                        (Integer) updatedRow[3],
                        (Integer) updatedRow[4],
                        (Double) updatedRow[5],
                        (Double) updatedRow[6],
                        (String) updatedRow[7]
                );
                if (articleManager.updateArticle(article)) {
                    loadArticles(); // Refresh the table
                    JOptionPane.showMessageDialog(this, "Artikel erfolgreich aktualisiert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Artikels.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton deleteArticleButton = createRoundedButton("Artikel löschen");
        deleteArticleButton.addActionListener(e -> deleteSelectedArticle());

        JButton refreshButton = createRoundedButton("🔄 Aktualisieren");
        refreshButton.addActionListener(e -> {
            loadArticles();
            JOptionPane.showMessageDialog(this, "Artikelliste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton retrieveQrCodeDataButton = createRoundedButton("QR-Code Daten abrufen");
        retrieveQrCodeDataButton.addActionListener(e -> {
            List<Map<String, Object>> qrCodeData = QRCodeUtils.retrieveQrCodeDataFromWebsite();
            JOptionPane.showMessageDialog(this, "Es wurden " + qrCodeData.size() + " QR-Code Datensätze abgerufen.", "QR-Code Daten", JOptionPane.INFORMATION_MESSAGE);
        });

        toolbarWrapper.add(addArticleButton);
        toolbarWrapper.add(editArticleButton);
        toolbarWrapper.add(deleteArticleButton);
        toolbarWrapper.add(refreshButton);
        toolbarWrapper.add(retrieveQrCodeDataButton);

        // top area: header + toolbar stacked
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.add(headerWrapper, BorderLayout.NORTH);
        topPanel.add(toolbarWrapper, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // Main card area (rounded white background with padding)
        RoundedPanel card = new RoundedPanel(Color.WHITE, 20);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        // make the table viewport wider and taller so it looks more like the design
        articleTable.setPreferredScrollableViewportSize(new Dimension(920, 420));
        tableScrollPane = new JScrollPane(articleTable);
        // allow the scroll pane to grow/shrink with the window (no fixed preferredSize)
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.add(tableScrollPane, BorderLayout.CENTER);

        // add surrounding shadow background and place in center
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbcCenter = new GridBagConstraints();
        gbcCenter.gridx = 0;
        gbcCenter.gridy = 0;
        gbcCenter.weightx = 1.0;
        gbcCenter.weighty = 1.0;
        gbcCenter.fill = GridBagConstraints.BOTH;
        centerWrapper.add(card, gbcCenter);
        add(centerWrapper, BorderLayout.CENTER);

        // Load existing articles from DB
        loadArticles();

        // Setup table interactions (context menu, double-click)
        setupTableInteractions();

        // Bottom search bar (search by article number or name)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        searchPanel.setBackground(new Color(245, 247, 250));
        JLabel searchLabel = new JLabel("Suche:");
        searchLabel.setForeground(new Color(31, 45, 61));
        JTextField searchField = new JTextField(30);
        JButton searchBtn = new JButton("Suchen");
        JButton clearBtn = new JButton("Leeren");

        // perform search using a RowFilter on columns 0 (Artikelnummer) and 1 (Name)
        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
            TableRowSorter<DefaultTableModel> sorter;
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                //noinspection unchecked
                sorter = (TableRowSorter<DefaultTableModel>) articleTable.getRowSorter();
            } else {
                sorter = new TableRowSorter<>((DefaultTableModel) articleTable.getModel());
                articleTable.setRowSorter(sorter);
            }
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                try {
                    String regex = "(?i)" + Pattern.quote(text);
                    sorter.setRowFilter(RowFilter.regexFilter(regex, 0, 1));
                } catch (PatternSyntaxException ex) {
                    sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote(text), 0, 1));
                }
            }
        };

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                ((TableRowSorter<?>) articleTable.getRowSorter()).setRowFilter(null);
            }
        });

        // Enter key triggers search
        searchField.addActionListener(e -> doSearch.run());

        countLabel = new JLabel("Anzahl Artikel: " + articleTable.getRowCount());
        searchPanel.add(countLabel);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        add(searchPanel, BorderLayout.SOUTH);

        // Auto-resize columns when the window or viewport changes size
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths();
            }
        };
        // listen to frame resizes
        this.addComponentListener(resizeListener);
        // and listen to viewport resizes (when card/scrollpane changes)
        tableScrollPane.getViewport().addComponentListener(resizeListener);

        // Ensure columns are adjusted once UI is visible
        SwingUtilities.invokeLater(this::adjustColumnWidths);
    }

    private Object[] showUpdateArticleDialog(Object[] existingData) {
        // For simplicity, reuse the add article dialog and pre-fill fields
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(this, "Artikel Bearbeiten", true);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Artikelnummer:"), gbc);
        JTextField nummerField = new JTextField(existingData[0] == null ? "" : existingData[0].toString(), 20);
        gbc.gridx = 1;
        panel.add(nummerField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Name:"), gbc);
        JTextField nameField = new JTextField(existingData[1] == null ? "" : existingData[1].toString(), 20);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Details:"), gbc);
        JTextField detailsField = new JTextField(existingData[2] == null ? "" : existingData[2].toString(), 20);
        gbc.gridx = 1;
        panel.add(detailsField, gbc);

        // parse integer values robustly
        int existingLager = 0;
        int existingMindest = 0;
        try {
            Object o = existingData[3];
            if (o instanceof Number) existingLager = ((Number) o).intValue();
            else existingLager = Integer.parseInt(o.toString());
        } catch (Exception ignored) {
        }
        try {
            Object o = existingData[4];
            if (o instanceof Number) existingMindest = ((Number) o).intValue();
            else existingMindest = Integer.parseInt(o.toString());
        } catch (Exception ignored) {
        }

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lagerbestand:"), gbc);
        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(existingLager, 0, Integer.MAX_VALUE, 1));
        gbc.gridx = 1;
        panel.add(lagerSpinner, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Mindestbestand:"), gbc);
        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(existingMindest, 0, Integer.MAX_VALUE, 1));
        gbc.gridx = 1;
        panel.add(mindestSpinner, gbc);

        // price fields with formatter
        NumberFormat priceFormat = NumberFormat.getNumberInstance();
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);
        NumberFormatter priceFormatter = new NumberFormatter(priceFormat);
        priceFormatter.setValueClass(Double.class);
        priceFormatter.setAllowsInvalid(false);
        priceFormatter.setMinimum(0.0);

        double existingVerkauf = 0.0;
        double existingEinkauf = 0.0;
        try {
            Object o = existingData[5];
            if (o instanceof Number) existingVerkauf = ((Number) o).doubleValue();
            else existingVerkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {
        }
        try {
            Object o = existingData[6];
            if (o instanceof Number) existingEinkauf = ((Number) o).doubleValue();
            else existingEinkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {
        }

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Verkaufspreis:"), gbc);
        JFormattedTextField verkaufField = new JFormattedTextField(priceFormatter);
        verkaufField.setColumns(10);
        verkaufField.setValue(existingVerkauf);
        gbc.gridx = 1;
        panel.add(verkaufField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Einkaufspreis:"), gbc);
        JFormattedTextField einkaufField = new JFormattedTextField(priceFormatter);
        einkaufField.setColumns(10);
        einkaufField.setValue(existingEinkauf);
        gbc.gridx = 1;
        panel.add(einkaufField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lieferant:"), gbc);
        JTextField lieferantField = new JTextField(existingData[7] == null ? "" : existingData[7].toString(), 20);
        gbc.gridx = 1;
        panel.add(lieferantField, gbc);

        // buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Abbrechen");
        buttons.add(cancelBtn);
        buttons.add(okBtn);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        dialog.getContentPane().add(panel);
        dialog.getRootPane().setDefaultButton(okBtn);

        // actions
        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        okBtn.addActionListener(ae -> {
            String nummer = nummerField.getText().trim();
            String name = nameField.getText().trim();
            String details = detailsField.getText().trim();
            String lieferant = lieferantField.getText().trim();

            if (nummer.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Artikelnummer und Name sind Pflichtfelder.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int lagerbestand = ((Number) lagerSpinner.getValue()).intValue();
            int mindestbestand = ((Number) mindestSpinner.getValue()).intValue();
            double verkaufspreis;
            double einkaufspreis;
            try {
                verkaufField.commitEdit();
                einkaufField.commitEdit();
                verkaufspreis = ((Number) verkaufField.getValue()).doubleValue();
                einkaufspreis = ((Number) einkaufField.getValue()).doubleValue();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Bitte gültige Preise eingeben.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand, verkaufspreis, einkaufspreis, lieferant};
            dialog.dispose();
        });

        // show dialog centered
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nummerField::requestFocusInWindow);
        dialog.setVisible(true);

        return resultHolder[0];
    }

    /**
     * Shows a polished modal dialog to enter a new article. Returns the table row or null if canceled.
     */
    private Object[] showAddArticleDialog() {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(this, "Neuen Artikel hinzufügen", true);
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Artikelnummer:"), gbc);
        JTextField nummerField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(nummerField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Name:"), gbc);
        JTextField nameField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Details:"), gbc);
        JTextField detailsField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(detailsField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lagerbestand:"), gbc);
        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        gbc.gridx = 1;
        panel.add(lagerSpinner, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Mindestbestand:"), gbc);
        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        gbc.gridx = 1;
        panel.add(mindestSpinner, gbc);

        // price fields with formatter
        NumberFormat priceFormat = NumberFormat.getNumberInstance();
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);
        NumberFormatter priceFormatter = new NumberFormatter(priceFormat);
        priceFormatter.setValueClass(Double.class);
        priceFormatter.setAllowsInvalid(false);
        priceFormatter.setMinimum(0.0);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Verkaufspreis:"), gbc);
        JFormattedTextField verkaufField = new JFormattedTextField(priceFormatter);
        verkaufField.setColumns(10);
        verkaufField.setValue(0.0);
        gbc.gridx = 1;
        panel.add(verkaufField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Einkaufspreis:"), gbc);
        JFormattedTextField einkaufField = new JFormattedTextField(priceFormatter);
        einkaufField.setColumns(10);
        einkaufField.setValue(0.0);
        gbc.gridx = 1;
        panel.add(einkaufField, gbc);

        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Lieferant:"), gbc);
        JTextField lieferantField = new JTextField(20);
        gbc.gridx = 1;
        panel.add(lieferantField, gbc);

        // buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Abbrechen");
        buttons.add(cancelBtn);
        buttons.add(okBtn);

        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.gridwidth = 2;
        panel.add(buttons, gbc);

        dialog.getContentPane().add(panel);
        dialog.getRootPane().setDefaultButton(okBtn);

        // actions
        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        okBtn.addActionListener(ae -> {
            String nummer = nummerField.getText().trim();
            String name = nameField.getText().trim();
            String details = detailsField.getText().trim();
            String lieferant = lieferantField.getText().trim();

            if (nummer.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Artikelnummer und Name sind Pflichtfelder.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int lagerbestand = ((Number) lagerSpinner.getValue()).intValue();
            int mindestbestand = ((Number) mindestSpinner.getValue()).intValue();
            double verkaufspreis;
            double einkaufspreis;
            try {
                verkaufField.commitEdit();
                einkaufField.commitEdit();
                verkaufspreis = ((Number) verkaufField.getValue()).doubleValue();
                einkaufspreis = ((Number) einkaufField.getValue()).doubleValue();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Bitte gültige Preise eingeben.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand, verkaufspreis, einkaufspreis, lieferant};
            dialog.dispose();
        });

        // show dialog centered
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nummerField::requestFocusInWindow);
        dialog.setVisible(true);

        return resultHolder[0];
    }

    private void initializeTable() {
        // Provide a typed, non-editable table model so sorting & renderers behave correctly
        DefaultTableModel model = getDefaultTableModel();

        articleTable.setModel(model);

        // Visual tweaks
        articleTable.setRowHeight(26);
        articleTable.setAutoCreateRowSorter(true);
        articleTable.getTableHeader().setReorderingAllowed(false);
        articleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Let JTable resize all columns proportionally to fill the viewport when the window resizes
        articleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Make grid lines visible and subtle (adds vertical+horizontal lines like in your mockup)
        articleTable.setShowGrid(true);
        articleTable.setShowHorizontalLines(true);
        articleTable.setShowVerticalLines(true);
        articleTable.setGridColor(new Color(226, 230, 233)); // soft light gray
        articleTable.setIntercellSpacing(new Dimension(1, 1));
        articleTable.setFont(new Font("Arial", Font.PLAIN, 16));


        // Alternating row colors for readability (subtle)
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            private final Color EVEN = new Color(255, 255, 255);
            private final Color ODD = new Color(247, 250, 253);

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) c.setBackground(row % 2 == 0 ? EVEN : ODD);
                return c;
            }
        };
        articleTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Column-specific renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
        DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Number) setText(currencyFormat.format(((Number) value).doubleValue()));
                else setText(value == null ? "" : value.toString());
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };

        // Make column widths reasonable (initial preferred widths)
        javax.swing.table.TableColumnModel tcm = articleTable.getColumnModel();
        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

        // Apply renderers where appropriate (column indices stable)
        // Artikelnummer centered
        tcm.getColumn(0).setCellRenderer(centerRenderer);
        // Integer columns right aligned
        tcm.getColumn(3).setCellRenderer(rightRenderer);
        tcm.getColumn(4).setCellRenderer(rightRenderer);
        // Currency columns use currency renderer
        tcm.getColumn(5).setCellRenderer(currencyRenderer);
        tcm.getColumn(6).setCellRenderer(currencyRenderer);

        // Header styling
        JTableHeader header = articleTable.getTableHeader();
        header.setBackground(new Color(62, 84, 98));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
    }

    private static DefaultTableModel getDefaultTableModel() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 3, 4 -> Integer.class;
                    case 5, 6 -> Double.class;
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // non-editable table (editing done via dialogs)
            }
        };

        model.addColumn("Artikelnummer");
        model.addColumn("Name");
        model.addColumn("Details");
        model.addColumn("Lagerbestand");
        model.addColumn("Mindestbestand");
        model.addColumn("Verkaufspreis");
        model.addColumn("Einkaufspreis");
        model.addColumn("Lieferant");
        return model;
    }

    /**
     * Adjust table column widths proportionally to the available viewport width using baseColumnWidths as weights.
     */
    private void adjustColumnWidths() {
        if (tableScrollPane == null || articleTable.getColumnCount() == 0) return;
        int available = tableScrollPane.getViewport().getWidth();
        if (available <= 0) {
            // try the scrollpane size if viewport isn't ready
            available = tableScrollPane.getWidth();
        }
        if (available <= 0) return;

        int totalBase = 0;
        for (int w : baseColumnWidths) totalBase += w;

        TableColumnModel tcm = articleTable.getColumnModel();
        int colCount = tcm.getColumnCount();

        // Compute scaled widths
        int used = 0;
        int[] newWidths = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            int base = i < baseColumnWidths.length ? baseColumnWidths[i] : 100;
            int w = Math.max(50, (int) Math.round((base / (double) totalBase) * available));
            newWidths[i] = w;
            used += w;
        }

        // adjust rounding error: distribute remaining pixels
        int diff = available - used;
        int idx = 0;
        while (diff != 0 && colCount > 0) {
            newWidths[idx % colCount] += (diff > 0) ? 1 : -1;
            diff += (diff > 0) ? -1 : 1;
            idx++;
        }

        // Apply widths
        for (int i = 0; i < colCount; i++) {
            TableColumn col = tcm.getColumn(i);
            col.setPreferredWidth(newWidths[i]);
        }
        articleTable.revalidate();
    }

    public void refreshTableData(Object[][] data) {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        model.setRowCount(0); // Clear existing data
        for (Object[] row : data) {
            model.addRow(row);
        }
    }

    public void addArticleRow(Object[] rowData) {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        model.addRow(rowData);
        updateCountLabel();
    }

    public void removeArticleRow(int rowIndex) {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        model.removeRow(rowIndex);
        updateCountLabel();
    }

    public JTable getArticleTable() {
        return articleTable;
    }

    public void display() {
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ArticleGUI gui = new ArticleGUI();
            gui.display();
        });
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        // softer button background and darker text for contrast
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(20, 30, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(180, 40));
        return button;
    }

    private void loadArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        java.util.List<Article> articles = articleManager.getAllArticles();
        if (articles == null) return;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        model.setRowCount(0);
        for (Article a : articles) {
            model.addRow(new Object[]{
                    a.getArticleNumber(),
                    a.getName(),
                    a.getDetails(),
                    a.getStockQuantity(),
                    a.getMinStockLevel(),
                    a.getSellPrice(),
                    a.getPurchasePrice(),
                    a.getVendorName()
            });
        }
        updateCountLabel();
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText("Anzahl Artikel: " + articleTable.getRowCount());
        }
    }

    private void deleteSelectedArticle() {
        int selectedRow = articleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Artikel wirklich löschen?", "Artikel löschen", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        int modelRow = articleTable.convertRowIndexToModel(selectedRow);
        String artikelnummer = (String) model.getValueAt(modelRow, 0);

        // Remove from database first
        ArticleManager articleManager = ArticleManager.getInstance();
        if (articleManager.deleteArticleByNumber(artikelnummer)) {
            // Only remove from table if DB deletion succeeded
            model.removeRow(modelRow);
            updateCountLabel();
            JOptionPane.showMessageDialog(this, "Artikel erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Artikels aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Simple rounded panel implementation
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

    private void setupTableInteractions() {
        JPopupMenu popup = createTablePopup();

        articleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Double left-click: Add to order list
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = articleTable.rowAtPoint(e.getPoint());
                    if (row == -1) return;

                    int modelRow = articleTable.convertRowIndexToModel(row);
                    Object[] existingData = ((DefaultTableModel) articleTable.getModel()).getDataVector().elementAt(modelRow).toArray();
                    String artikelNr = (String) existingData[0];

                    Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                    if (article == null) {
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Artikel konnte nicht gefunden werden.", "Fehler", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    String input = JOptionPane.showInputDialog(ArticleGUI.this,
                        "Geben Sie die Menge für \"" + article.getName() + "\" ein:",
                        "Zur Bestellung hinzufügen",
                        JOptionPane.PLAIN_MESSAGE);

                    if (input == null) return; // User cancelled

                    try {
                        int quantity = Integer.parseInt(input.trim());
                        if (quantity <= 0) {
                            JOptionPane.showMessageDialog(ArticleGUI.this, "Menge muss größer als 0 sein.", "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        ArticleListGUI.addArticle(article, quantity);

                        if (articleListGUI == null) {
                            articleListGUI = new ArticleListGUI();
                        } else {
                            articleListGUI.refreshArticleList();
                        }

                        if (!articleListGUI.isVisible()) {
                            articleListGUI.display();
                        } else {
                            articleListGUI.toFront();
                        }

                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Ungültige Menge: " + input, "Fehler", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = articleTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        articleTable.setRowSelectionInterval(row, row);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private JPopupMenu createTablePopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem("Bearbeiten");
        JMenuItem deleteItem = new JMenuItem("Löschen");

        editItem.addActionListener(e -> {
            int sel = articleTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = articleTable.convertRowIndexToModel(sel);
            Object[] existingData = ((DefaultTableModel) articleTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateArticleDialog(existingData);
            if (updated != null) {
                DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
                for (int i = 0; i < updated.length; i++) model.setValueAt(updated[i], modelRow, i);
                ArticleManager.getInstance().updateArticle(new Article(
                        (String) updated[0], (String) updated[1], (String) updated[2], (Integer) updated[3], (Integer) updated[4], (Double) updated[5], (Double) updated[6], (String) updated[7]
                ));
            }
        });

        deleteItem.addActionListener(e -> deleteSelectedArticle());

        popup.add(editItem);
        popup.add(deleteItem);
        return popup;
    }
}
