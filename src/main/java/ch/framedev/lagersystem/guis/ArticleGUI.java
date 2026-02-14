package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.*;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.*;
import ch.framedev.lagersystem.managers.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ch.framedev.lagersystem.main.Main.articleListGUI;

/**
 * ArticleGUI with category support for better organization.
 * Categories are loaded from categories.json and mapped to articles based on article number ranges.
 * TODO: Performance Check
 */
public class ArticleGUI extends JFrame {

    private final JTable articleTable;
    // scroll pane reference so we can read viewport width on resize
    private final JScrollPane tableScrollPane;
    // base column widths - added category column between Name and Details
    private final int[] baseColumnWidths = new int[]{150, 200, 150, 290, 110, 110, 150, 150, 200};
    private final JLabel countLabel;
    private boolean isUpdatingTable = false;
    private JTextField vendorFilterField;
    private JTextField stockMinField;
    private JTextField stockMaxField;
    private JTextField priceMinField;
    private JTextField priceMaxField;

    // Category management
    private JComboBox<String> categoryFilter;
    private Map<String, CategoryRange> categories; // category name -> range

    // Inner class to hold category range data
    private static class CategoryRange {
        String category;
        int rangeStart;
        int rangeEnd;

        CategoryRange(String category, int start, int end) {
            this.category = category;
            this.rangeStart = start;
            this.rangeEnd = end;
        }
    }

    public ArticleGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle("Artikel Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Apply current theme defaults (important if you switched theme before opening)
        ThemeManager.applyUIDefaults();

        // Load categories from JSON
        loadCategories();

        // Initialize a table early so toolbar actions can reference it
        articleTable = new JTable();
        initializeTable();

        // =========================
        // Header
        // =========================
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());

        RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setLayout(new GridBagLayout());
        headerPanel.setPreferredSize(new Dimension(760, 64));

        /*JLabel title = new JLabel("Artikel Verwaltung");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 24));
        title.setForeground(ThemeManager.getTextPrimaryColor());
        headerPanel.add(title);
        headerWrapper.add(headerPanel);*/

        // =========================
        // Toolbar
        // =========================
        RoundedPanel toolbarCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        toolbarCard.setLayout(new BorderLayout());
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel toolbarWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        toolbarWrapper.setOpaque(false);

        // Category filter section
        JPanel categoryPanel = createCategoryFilterPanel();
        JPanel filterGroup = createToolbarGroup();
        filterGroup.add(categoryPanel);

        JPanel advancedFilterPanel = createAdvancedFilterPanel();
        filterGroup.add(advancedFilterPanel);
        toolbarWrapper.add(filterGroup);

        // Add Article button
        JButton addArticleButton = createRoundedButton(UnicodeSymbols.HEAVY_PLUS + " Artikel hinzufügen");
        addArticleButton.setToolTipText("Erstellt einen neuen Artikel im Lager");
        addArticleButton.addActionListener(e -> {
            Object[] row = showAddArticleDialog();
            if (row != null) {
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
                    JOptionPane.showMessageDialog(this,
                            "Fehler beim Hinzufügen des Artikels. Möglicherweise existiert die Artikelnummer bereits.",
                            "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                } else {
                    loadArticles();
                    JOptionPane.showMessageDialog(this,
                            "Artikel erfolgreich hinzugefügt.",
                            "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                }
            }
        });

        // Edit Article button
        JButton editArticleButton = createRoundedButton(UnicodeSymbols.CODE + " Artikel bearbeiten");
        editArticleButton.setToolTipText("Bearbeitet den ausgewählten Artikel");
        editArticleButton.addActionListener(e -> {
            int selectedRow = articleTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this,
                        "Bitte wählen Sie einen Artikel zum Bearbeiten aus.",
                        "Keine Auswahl", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
                return;
            }

            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            Object[] existingData = ((DefaultTableModel) articleTable.getModel())
                    .getDataVector().elementAt(modelRow).toArray();

            Object[] updatedRow = showUpdateArticleDialog(existingData);
            if (updatedRow != null) {
                // updatedRow: [artikelNr, name, details, lager, mindest, verkauf, einkauf, lieferant]
                DefaultTableModel model = (DefaultTableModel) articleTable.getModel();

                model.setValueAt(updatedRow[0], modelRow, 0); // Artikelnummer
                model.setValueAt(updatedRow[1], modelRow, 1); // Name
                String category = getCategoryForArticle((String) updatedRow[0]);
                model.setValueAt(category, modelRow, 2); // Kategorie (recalculated)
                model.setValueAt(updatedRow[2], modelRow, 3); // Details
                model.setValueAt(updatedRow[3], modelRow, 4); // Lagerbestand
                model.setValueAt(updatedRow[4], modelRow, 5); // Mindestbestand
                model.setValueAt(updatedRow[5], modelRow, 6); // Verkaufspreis
                model.setValueAt(updatedRow[6], modelRow, 7); // Einkaufspreis
                model.setValueAt(updatedRow[7], modelRow, 8); // Lieferant

                ArticleManager articleManager = ArticleManager.getInstance();
                Article article = new Article(
                        (String) updatedRow[0],
                        (String) updatedRow[1],
                        (String) updatedRow[2],
                        (Integer) updatedRow[3],
                        (Integer) updatedRow[4],
                        (Double) updatedRow[5],
                        (Double) updatedRow[6],
                        (String) updatedRow[7] // lieferant
                );

                if (articleManager.updateArticle(article)) {
                    loadArticles();
                    JOptionPane.showMessageDialog(this,
                            "Artikel erfolgreich aktualisiert.",
                            "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Fehler beim Aktualisieren des Artikels.",
                            "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        // Delete Article button
        JButton deleteArticleButton = createRoundedButton(UnicodeSymbols.TRASH + " Artikel löschen");
        deleteArticleButton.setToolTipText("Löscht den ausgewählten Artikel aus dem Lager");
        deleteArticleButton.addActionListener(e -> deleteSelectedArticle());

        // Generate QR-Codes for selected articles
        JButton generateQrCodesButton = createRoundedButton(UnicodeSymbols.PACKAGE + " QR-Codes erstellen");
        generateQrCodesButton.setToolTipText("Erstellt QR-Codes für die ausgewählten Artikel");
        generateQrCodesButton.addActionListener(e -> generateQrCodesForSelectedArticles());

        // QR preview + PDF export
        JButton qrPreviewPdfButton = createRoundedButton(UnicodeSymbols.CLIPBOARD + " QR-Code Vorschau/PDF");
        qrPreviewPdfButton.setToolTipText("Zeigt eine Vorschau und exportiert QR-Codes als PDF");
        qrPreviewPdfButton.addActionListener(e -> showQrCodePreviewDialog());

        JButton bulkDeleteButton = createRoundedButton(UnicodeSymbols.TRASH + " Mehrfach löschen");
        bulkDeleteButton.setToolTipText("Löscht alle ausgewählten Artikel");
        bulkDeleteButton.addActionListener(e -> deleteSelectedArticles());

        JButton bulkAdjustStockButton = createRoundedButton(UnicodeSymbols.PACKAGE + " Bestand anpassen");
        bulkAdjustStockButton.setToolTipText("Passt den Lagerbestand für alle ausgewählten Artikel an");
        bulkAdjustStockButton.addActionListener(e -> adjustStockForSelectedArticles());

        JButton bulkExportButton = createRoundedButton(UnicodeSymbols.DOWNLOAD + " Auswahl exportieren");
        bulkExportButton.setToolTipText("Exportiert die ausgewählten Artikel");
        bulkExportButton.addActionListener(e -> exportSelectedArticles());

        // Retrieve QR-Code Data button
        JButton retrieveQrCodeDataButton = createRoundedButton(UnicodeSymbols.PHONE + " QR-Code Daten abrufen");
        retrieveQrCodeDataButton.setToolTipText("Ruft Artikeldaten über einen QR-Code ab");
        retrieveQrCodeDataButton.addActionListener(e -> showQRCodeDataDialog());

        // Show Details button
        JButton showDetailsButton = createRoundedButton(UnicodeSymbols.CHART + " Lager Details");
        showDetailsButton.setToolTipText("Zeigt detaillierte Lagerinformationen an");
        showDetailsButton.addActionListener(e -> showDetails());

        JPanel actionGroup = createToolbarGroup();
        actionGroup.add(addArticleButton);
        actionGroup.add(editArticleButton);
        actionGroup.add(deleteArticleButton);

        JPanel bulkGroup = createToolbarGroup();
        bulkGroup.add(bulkDeleteButton);
        bulkGroup.add(bulkAdjustStockButton);
        bulkGroup.add(bulkExportButton);

        JPanel qrGroup = createToolbarGroup();
        qrGroup.add(generateQrCodesButton);
        qrGroup.add(qrPreviewPdfButton);
        qrGroup.add(retrieveQrCodeDataButton);

        JPanel infoGroup = createToolbarGroup();
        infoGroup.add(showDetailsButton);

        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(actionGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(bulkGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(qrGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(infoGroup);

        toolbarCard.add(toolbarWrapper, BorderLayout.CENTER);

        JScrollPane toolbarScrollPane = new JScrollPane(
                toolbarCard,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
        toolbarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        toolbarScrollPane.setOpaque(false);
        toolbarScrollPane.getViewport().setOpaque(false);
        toolbarScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // Top area: header + toolbar stacked
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.add(headerWrapper, BorderLayout.NORTH);
        topPanel.add(toolbarScrollPane, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // =========================
        // Main card/table area
        // =========================
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        articleTable.setPreferredScrollableViewportSize(new Dimension(920, 420));
        tableScrollPane = new JScrollPane(articleTable);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tableScrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        tableScrollPane.setBackground(ThemeManager.getScrollbarBackgroundColor());
        tableScrollPane.setForeground(ThemeManager.getScrollbarForegroundColor());
        card.add(tableScrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());

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

        // =========================
        // Bottom search bar
        // =========================
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 12));
        searchPanel.setBackground(ThemeManager.getBackgroundColor());

        JLabel searchLabel = new JLabel("Suche:");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JTextField searchField = new JTextField(30);
        searchField.setBackground(ThemeManager.getInputBackgroundColor());
        searchField.setForeground(ThemeManager.getTextPrimaryColor());
        searchField.setCaretColor(ThemeManager.getTextPrimaryColor());
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JButton searchBtn = new JButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Sucht nach Artikeln basierend auf dem eingegebenen Text");
        JButton clearBtn = new JButton(UnicodeSymbols.BROOM + " Leeren");
        clearBtn.setToolTipText("Löscht die Suchfilter und zeigt alle Artikel an");

// base colors
        Color normalFg = ThemeManager.getTextPrimaryColor();
        Color hoverFg = ThemeManager.getTextLinkColor();      // good hover text color
        Color border = ThemeManager.getBorderColor();

        for (JButton b : new JButton[]{searchBtn, clearBtn}) {
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBorderPainted(true);
            b.setFocusPainted(false);

            b.setForeground(normalFg);
            b.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(border, 1),
                    BorderFactory.createEmptyBorder(8, 16, 8, 16)
            ));

            b.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    b.setForeground(hoverFg);
                    // optional: slightly stronger border on hover
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getBorderFocusColor(), 1),
                            BorderFactory.createEmptyBorder(8, 16, 8, 16)
                    ));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    b.setForeground(normalFg);
                    b.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(border, 1),
                            BorderFactory.createEmptyBorder(8, 16, 8, 16)
                    ));
                }
            });
        }

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
            updateCountLabel();
        };

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                ((TableRowSorter<?>) articleTable.getRowSorter()).setRowFilter(null);
            }
            updateCountLabel();
        });

        searchField.addActionListener(e -> doSearch.run());
        searchField.setForeground(ThemeManager.getTextPrimaryColor());
        searchField.setBackground(ThemeManager.getInputBackgroundColor());

        JButton addToClientOrder = createRoundedButton(UnicodeSymbols.SHOPPING_CART + " Zur Kundenbestellung hinzufügen");
        addToClientOrder.setToolTipText("Fügt die ausgewählten Artikel zur aktuellen Kundenbestellung hinzu");
        addToClientOrder.addActionListener(e -> addSelectedArticlesToClientOrder());
        searchPanel.add(addToClientOrder);

        JButton exportTableAsPdfBtn = createRoundedButton(UnicodeSymbols.DOWNLOAD + " Tabelle als PDF exportieren");
        exportTableAsPdfBtn.addActionListener(e -> exportTableAsPDF());
        searchPanel.add(exportTableAsPdfBtn);

        countLabel = new JLabel("Anzahl Artikel: " + articleTable.getRowCount());
        countLabel.setForeground(ThemeManager.getTextPrimaryColor());
        countLabel.setBackground(ThemeManager.getTextSecondaryColor());
        searchPanel.add(countLabel);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);

        // Add warnings button to bottom panel
        JButton showWarningsBottomBtn = new JButton(UnicodeSymbols.WARNING + " Warnungen");
        showWarningsBottomBtn.setToolTipText("Zeigt alle Lagerwarnungen an");
        showWarningsBottomBtn.setFocusPainted(false);
        showWarningsBottomBtn.setBorderPainted(false);
        showWarningsBottomBtn.setContentAreaFilled(false);
        showWarningsBottomBtn.setOpaque(true);
        showWarningsBottomBtn.setBackground(ThemeManager.getWarningColor());
        showWarningsBottomBtn.setForeground(Color.WHITE);
        showWarningsBottomBtn.setFont(showWarningsBottomBtn.getFont().deriveFont(Font.BOLD, 13f));
        showWarningsBottomBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(showWarningsBottomBtn.getBackground().darker(), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        showWarningsBottomBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showWarningsBottomBtn.addActionListener(e -> showAllWarnings());
        showWarningsBottomBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showWarningsBottomBtn.setBackground(showWarningsBottomBtn.getBackground().darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showWarningsBottomBtn.setBackground(ThemeManager.getWarningColor());
            }
        });
        searchPanel.add(showWarningsBottomBtn);

        add(searchPanel, BorderLayout.SOUTH);

        // Auto-resize columns when the window or viewport changes size
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths();
            }
        };
        this.addComponentListener(resizeListener);
        tableScrollPane.getViewport().addComponentListener(resizeListener);

        SwingUtilities.invokeLater(this::adjustColumnWidths);
    }

    private void addSelectedArticlesToClientOrder() {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus, um ihn zur Kundenbestellung hinzuzufügen.",
                    "Keine Auswahl", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            return;
        }

        List<Article> articlesToAdd = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();

        for (int selectedRow : selectedRows) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            String artikelNr = (String) model.getValueAt(modelRow, 0);
            Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
            if (article != null) {
                String input = JOptionPane.showInputDialog("Menge für Artikel (" + artikelNr + ")" + article.getName() + " eingeben:", "1");
                if (input != null) {
                    try {
                        int menge = Integer.parseInt(input);
                        ArticleListGUI.addArticle(article, menge);
                        articlesToAdd.add(article);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Bitte geben Sie eine gültige Zahl für die Menge ein.",
                                "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    }
                }
            }
        }

        if (articlesToAdd.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Keine gültigen Artikel zum Hinzufügen gefunden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            return;
        }
        JOptionPane.showMessageDialog(null, "Artikel zur Kundenbestellung hinzugefügt.",
                "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private Object[] showUpdateArticleDialog(Object[] existingData) {
        return ArticleDialog.showUpdateArticleDialog(this, existingData);
    }

    /**
     * Shows a polished modal dialog to enter a new article. Returns the table row or null if canceled.
     */
    private Object[] showAddArticleDialog() {
        return ArticleDialog.showAddArticleDialog(this);
    }

    public static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient = getGradientPaint();
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }

            private GradientPaint getGradientPaint() {
                Color color1 = ThemeManager.getHeaderBackgroundColor();
                Color color2 = ThemeManager.getHeaderGradientColor();

                return new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }

    private void initializeTable() {
        // Provide a typed, non-editable table model so sorting & renderers behave correctly
        DefaultTableModel model = getDefaultTableModel();

        // Set model
        articleTable.setModel(model);
        registerInlineEditListener(model);

        // Visual tweaks
        articleTable.setRowHeight(26);
        articleTable.setAutoCreateRowSorter(true);
        articleTable.getTableHeader().setReorderingAllowed(false);
        articleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Let JTable resize all columns proportionally to fill the viewport when the window resizes
        articleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Make grid lines visible and subtle (adds vertical+horizontal lines like in your mockup)
        articleTable.setShowGrid(true);
        articleTable.setShowHorizontalLines(true);
        articleTable.setShowVerticalLines(true);
        articleTable.setGridColor(ThemeManager.getTableGridColor()); // soft light gray // soft light gray
        articleTable.setIntercellSpacing(new Dimension(1, 1));
        articleTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));


        // Alternating row colors for readability (subtle)
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected)
                    c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                return c;
            }
        };
        articleTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Column-specific renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);

        DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
            @Override
            public void setValue(Object value) {
                if (value instanceof Number) setText(value + " CHF");
                else setText(value == null ? "" : value.toString());
                setHorizontalAlignment(SwingConstants.RIGHT);
            }
        };

        // Make column widths reasonable (initial preferred widths)
        TableColumnModel tcm = articleTable.getColumnModel();
        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

        // Apply renderers where appropriate (column indices stable)
        // Artikelnummer centered
        tcm.getColumn(0).setCellRenderer(centerRenderer);
        // Category centered
        tcm.getColumn(2).setCellRenderer(centerRenderer);
        // Integer columns right aligned (shifted by 1 due to category column)
        tcm.getColumn(4).setCellRenderer(rightRenderer);
        tcm.getColumn(5).setCellRenderer(rightRenderer);
        // Currency columns use currency renderer (shifted by 1)
        tcm.getColumn(6).setCellRenderer(currencyRenderer);
        tcm.getColumn(7).setCellRenderer(currencyRenderer);

        // Header styling
        JTableHeader header = articleTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackground());
        header.setForeground(ThemeManager.getTableHeaderForeground());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));

        NumberFormatter intFormatter = new NumberFormatter(NumberFormat.getIntegerInstance());
        intFormatter.setAllowsInvalid(false);
        intFormatter.setMinimum(0);
        JFormattedTextField intField = new JFormattedTextField(intFormatter);
        intField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        DefaultCellEditor intEditor = new DefaultCellEditor(intField);

        NumberFormatter doubleFormatter = new NumberFormatter(NumberFormat.getNumberInstance());
        doubleFormatter.setAllowsInvalid(false);
        doubleFormatter.setMinimum(0.0);
        JFormattedTextField doubleField = new JFormattedTextField(doubleFormatter);
        doubleField.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        DefaultCellEditor doubleEditor = new DefaultCellEditor(doubleField);

        tcm.getColumn(4).setCellEditor(intEditor);
        tcm.getColumn(5).setCellEditor(intEditor);
        tcm.getColumn(6).setCellEditor(doubleEditor);
        tcm.getColumn(7).setCellEditor(doubleEditor);
    }

    /**
     * Export the article table as a PDF with all data on a single page.
     * Uses landscape A3 format for maximum space and dynamically scales content to fit.
     */
    private void exportTableAsPDF() {
        ArticlePdfExporter.exportTableAsPdf(this, articleTable, baseColumnWidths, Main.iconSmall);
    }

    private static DefaultTableModel getDefaultTableModel() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 4, 5 -> Integer.class;  // Lagerbestand, Mindestbestand (shifted by 1)
                    case 6, 7 -> Double.class;   // Verkaufspreis, Einkaufspreis (shifted by 1)
                    default -> String.class;
                };
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0 && column != 2; // allow inline edit except article number and category
            }
        };

        model.addColumn(UnicodeSymbols.NUMBERS + " Artikelnummer");
        model.addColumn(UnicodeSymbols.ARTICLE_NAME + " Name");
        model.addColumn(UnicodeSymbols.CATEGORY + " Kategorie");  // New category column
        model.addColumn(UnicodeSymbols.CLIPBOARD + " Details");
        model.addColumn(UnicodeSymbols.COL_LAGERBESTAND + " Lagerbestand");
        model.addColumn(UnicodeSymbols.COL_MINDESTBESTAND + " Mindestbestand");
        model.addColumn(UnicodeSymbols.MONEY + " Verkaufspreis");
        model.addColumn(UnicodeSymbols.MONEY + " Einkaufspreis");
        model.addColumn(UnicodeSymbols.TRUCK + " Lieferant");
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
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        // Use ThemeManager for consistent Light/Dark styling
        Color defaultBg = ThemeManager.getAccentColor();
        Color hoverBg = ThemeManager.getButtonHoverColor(defaultBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(defaultBg);

        button.setBackground(defaultBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(defaultBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2),
                        BorderFactory.createEmptyBorder(9, 19, 9, 19)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(defaultBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(defaultBg.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : defaultBg);
            }
        });

        return button;
    }

    private void loadArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Article> articles = articleManager.getAllArticles();
        if (articles == null) return;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        isUpdatingTable = true;
        model.setRowCount(0);
        for (Article a : articles) {
            String category = getCategoryForArticle(a.getArticleNumber());
            model.addRow(new Object[]{
                    a.getArticleNumber(),
                    a.getName(),
                    category,  // Category column
                    a.getDetails(),
                    a.getStockQuantity(),
                    a.getMinStockLevel(),
                    a.getSellPrice(),
                    a.getPurchasePrice(),
                    a.getVendorName()
            });
        }
        isUpdatingTable = false;
        updateCountLabel();
    }

    private void updateCountLabel() {
        if (countLabel != null) {
            countLabel.setText("Anzahl Artikel: " + articleTable.getRowCount());
        }
    }

    private void registerInlineEditListener(DefaultTableModel model) {
        model.addTableModelListener(e -> {
            if (isUpdatingTable || e.getType() != TableModelEvent.UPDATE) {
                return;
            }
            int row = e.getFirstRow();
            int column = e.getColumn();
            if (row < 0 || column < 0) {
                return;
            }
            handleInlineEdit(row);
        });
    }

    private void handleInlineEdit(int modelRow) {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        String articleNumber = String.valueOf(model.getValueAt(modelRow, 0));
        String name = String.valueOf(model.getValueAt(modelRow, 1)).trim();
        String details = String.valueOf(model.getValueAt(modelRow, 3));
        Integer stockQuantity = parseInteger(model.getValueAt(modelRow, 4));
        Integer minStock = parseInteger(model.getValueAt(modelRow, 5));
        Double sellPrice = parseDouble(model.getValueAt(modelRow, 6));
        Double purchasePrice = parseDouble(model.getValueAt(modelRow, 7));
        String vendor = String.valueOf(model.getValueAt(modelRow, 8)).trim();

        if (name.isEmpty()) {
            showInlineEditError("Name darf nicht leer sein.", articleNumber, modelRow);
            return;
        }
        if (stockQuantity == null || stockQuantity < 0) {
            showInlineEditError("Lagerbestand muss eine Zahl >= 0 sein.", articleNumber, modelRow);
            return;
        }
        if (minStock == null || minStock < 0) {
            showInlineEditError("Mindestbestand muss eine Zahl >= 0 sein.", articleNumber, modelRow);
            return;
        }
        if (sellPrice == null || sellPrice < 0) {
            showInlineEditError("Verkaufspreis muss eine Zahl >= 0 sein.", articleNumber, modelRow);
            return;
        }
        if (purchasePrice == null || purchasePrice < 0) {
            showInlineEditError("Einkaufspreis muss eine Zahl >= 0 sein.", articleNumber, modelRow);
            return;
        }

        Article article = new Article(
                articleNumber,
                name,
                details,
                stockQuantity,
                minStock,
                sellPrice,
                purchasePrice,
                vendor
        );

        if (!ArticleManager.getInstance().updateArticle(article)) {
            showInlineEditError("Fehler beim Speichern der Aenderung.", articleNumber, modelRow);
        }
    }

    private void showInlineEditError(String message, String articleNumber, int modelRow) {
        JOptionPane.showMessageDialog(this, message, "Ungültige Eingabe", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
        reloadRowFromDb(articleNumber, modelRow);
    }

    private void reloadRowFromDb(String articleNumber, int modelRow) {
        Article article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
        if (article == null) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        isUpdatingTable = true;
        model.setValueAt(article.getArticleNumber(), modelRow, 0);
        model.setValueAt(article.getName(), modelRow, 1);
        model.setValueAt(getCategoryForArticle(article.getArticleNumber()), modelRow, 2);
        model.setValueAt(article.getDetails(), modelRow, 3);
        model.setValueAt(article.getStockQuantity(), modelRow, 4);
        model.setValueAt(article.getMinStockLevel(), modelRow, 5);
        model.setValueAt(article.getSellPrice(), modelRow, 6);
        model.setValueAt(article.getPurchasePrice(), modelRow, 7);
        model.setValueAt(article.getVendorName(), modelRow, 8);
        isUpdatingTable = false;
    }

    private Integer parseInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double parseDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void deleteSelectedArticle() {
        int selectedRow = articleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Artikel wirklich löschen?", "Artikel löschen", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                Main.iconSmall);
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
            JOptionPane.showMessageDialog(this, "Artikel erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
        } else {
            JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Artikels aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
        }
    }

    private void deleteSelectedArticles() {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Moechten Sie " + selectedRows.length + " Artikel wirklich loeschen?",
                "Artikel loeschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                Main.iconSmall);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        List<Integer> modelRows = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            modelRows.add(articleTable.convertRowIndexToModel(selectedRow));
        }
        modelRows.sort(Comparator.reverseOrder());

        ArticleManager articleManager = ArticleManager.getInstance();
        isUpdatingTable = true;
        for (int modelRow : modelRows) {
            String artikelnummer = String.valueOf(model.getValueAt(modelRow, 0));
            if (articleManager.deleteArticleByNumber(artikelnummer)) {
                model.removeRow(modelRow);
            }
        }
        isUpdatingTable = false;
        updateCountLabel();
    }

    private void adjustStockForSelectedArticles() {
        List<Article> selected = getSelectedArticles();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        String input = JOptionPane.showInputDialog(this,
                "Bestandsaenderung eingeben (z.B. 5 oder -3):",
                "Bestand anpassen",
                JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return;
        }
        Integer delta = parseInteger(input.trim());
        if (delta == null) {
            JOptionPane.showMessageDialog(this,
                    "Bitte eine gueltige Ganzzahl eingeben.",
                    "Ungueltige Eingabe",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        boolean hasNegative = false;
        for (Article article : selected) {
            if (article.getStockQuantity() + delta < 0) {
                hasNegative = true;
                break;
            }
        }
        if (hasNegative) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Einige Artikel wuerden negativ werden.\nBestand fuer diese Artikel auf 0 setzen?",
                    "Bestand anpassen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        ArticleManager articleManager = ArticleManager.getInstance();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        isUpdatingTable = true;
        for (int selectedRow : articleTable.getSelectedRows()) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            String artikelnummer = String.valueOf(model.getValueAt(modelRow, 0));
            Article article = articleManager.getArticleByNumber(artikelnummer);
            if (article == null) {
                continue;
            }
            int newStock = Math.max(0, article.getStockQuantity() + delta);
            Article updated = new Article(
                    article.getArticleNumber(),
                    article.getName(),
                    article.getDetails(),
                    newStock,
                    article.getMinStockLevel(),
                    article.getSellPrice(),
                    article.getPurchasePrice(),
                    article.getVendorName()
            );
            if (articleManager.updateArticle(updated)) {
                model.setValueAt(newStock, modelRow, 4);
            }
        }
        isUpdatingTable = false;
    }

    private void exportSelectedArticles() {
        List<Article> selected = getSelectedArticles();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        Object[] options = {"CSV", "PDF", "Abbrechen"};
        int choice = JOptionPane.showOptionDialog(this,
                "Auswahl exportieren als:",
                "Export",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                Main.iconSmall,
                options,
                options[0]);
        if (choice == 0) {
            exportArticlesToCsv(selected);
        } else if (choice == 1) {
            exportArticlesToPdf(selected);
        }
    }

    private void exportArticlesToCsv(List<Article> articles) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Auswahl_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
        }

        try (var writer = Files.newBufferedWriter(fileToSave.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Artikelnummer,Name,Details,Lagerbestand,Mindestbestand,Verkaufspreis,Einkaufspreis,Lieferant");
            writer.write(System.lineSeparator());
            for (Article article : articles) {
                writer.write(escapeCsv(article.getArticleNumber()));
                writer.write(",");
                writer.write(escapeCsv(article.getName()));
                writer.write(",");
                writer.write(escapeCsv(article.getDetails()));
                writer.write(",");
                writer.write(String.valueOf(article.getStockQuantity()));
                writer.write(",");
                writer.write(String.valueOf(article.getMinStockLevel()));
                writer.write(",");
                writer.write(String.valueOf(article.getSellPrice()));
                writer.write(",");
                writer.write(String.valueOf(article.getPurchasePrice()));
                writer.write(",");
                writer.write(escapeCsv(article.getVendorName()));
                writer.write(System.lineSeparator());
            }
            JOptionPane.showMessageDialog(this,
                    "CSV erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "Export",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim CSV-Export: " + ex.getMessage(),
                    "Export",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    private void exportArticlesToPdf(List<Article> articles) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Auswahl_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont = PDType1Font.HELVETICA;

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40f;
            float fontSize = 10f;
            float lineHeight = 14f;
            float yStart = pageSize.getHeight() - margin;
            float maxWidth = pageSize.getWidth() - 2 * margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.setFont(regularFont, fontSize);

            float y = yStart;
            for (Article article : articles) {
                String line = article.getArticleNumber() + " | " + article.getName() + " | Lager: " +
                        article.getStockQuantity() + " | Min: " + article.getMinStockLevel() +
                        " | VK: " + article.getSellPrice() + " | EK: " + article.getPurchasePrice() +
                        " | Lieferant: " + article.getVendorName();
                List<String> wrapped = wrapLine(line, regularFont, fontSize, maxWidth);
                for (String part : wrapped) {
                    if (y - lineHeight < margin) {
                        contentStream.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        contentStream = new PDPageContentStream(doc, page);
                        contentStream.setFont(regularFont, fontSize);
                        y = yStart;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText(part);
                    contentStream.endText();
                    y -= lineHeight;
                }
            }

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(this,
                    "PDF erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "Export",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim PDF-Export: " + ex.getMessage(),
                    "Export",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    private List<String> wrapLine(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    public List<Article> getSelectedArticles() {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            return Collections.emptyList();
        }

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        List<Article> selectedArticles = new ArrayList<>();
        for (int selectedRow : selectedRows) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            Object articleNumber = model.getValueAt(modelRow, 0);
            Object name = model.getValueAt(modelRow, 1);
            Object details = model.getValueAt(modelRow, 3);
            Object stockQuantity = model.getValueAt(modelRow, 4);
            Object minStockLevel = model.getValueAt(modelRow, 5);
            Object sellPrice = model.getValueAt(modelRow, 6);
            Object purchasePrice = model.getValueAt(modelRow, 7);
            Object vendorName = model.getValueAt(modelRow, 8);

            Article article = new Article(
                    String.valueOf(articleNumber),
                    String.valueOf(name),
                    String.valueOf(details),
                    toInt(stockQuantity),
                    toInt(minStockLevel),
                    toDouble(sellPrice),
                    toDouble(purchasePrice),
                    String.valueOf(vendorName)
            );
            selectedArticles.add(article);
        }
        return selectedArticles;
    }

    private static int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private void generateQrCodesForSelectedArticles() {
        List<Article> selectedArticles = getSelectedArticles();
        if (selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        File outputDir = new File(Main.getAppDataDir(), "qr_codes");
        int result = JOptionPane.showConfirmDialog(
                this,
                "QR-Codes fuer " + selectedArticles.size() + " ausgewählte Artikel generieren?\nSpeicherort: " + outputDir.getAbsolutePath(),
                "QR-Codes generieren",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        JDialog progressDialog = new JDialog(this, "QR-Codes generieren", true);
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        progressDialog.setLayout(new BorderLayout());
        JLabel label = new JLabel("Bitte warten, QR-Codes werden erstellt...");
        label.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        progressDialog.add(label, BorderLayout.CENTER);
        progressDialog.pack();
        progressDialog.setLocationRelativeTo(this);

        SwingWorker<List<File>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<File> doInBackground() {
                return QRCodeUtils.createQrCodes(selectedArticles);
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                try {
                    List<File> files = get();
                    if (files == null || files.isEmpty()) {
                        JOptionPane.showMessageDialog(
                                ArticleGUI.this,
                                "Es konnten keine QR-Codes erstellt werden.",
                                "QR-Codes generieren",
                                JOptionPane.WARNING_MESSAGE,
                                Main.iconSmall
                        );
                        return;
                    }
                    JOptionPane.showMessageDialog(
                            ArticleGUI.this,
                            "QR-Codes wurden erstellt.\nAnzahl: " + files.size() + "\nOrdner: " + outputDir.getAbsolutePath(),
                            "QR-Codes generieren",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall
                    );
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            ArticleGUI.this,
                            "Fehler beim Erstellen der QR-Codes: " + ex.getMessage(),
                            "QR-Codes generieren",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall
                    );
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    private void showQrCodePreviewDialog() {
        ArticleQrPreviewDialog.show(this, getSelectedArticles());
    }

    // Simple rounded panel implementation
    public static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        public RoundedPanel(Color bg, int radius) {
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
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Artikel konnte nicht gefunden werden.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
                            JOptionPane.showMessageDialog(ArticleGUI.this, "Menge muss größer als 0 sein.", "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                            return;
                        }

                        if (article.getStockQuantity() < quantity) {
                            int confirm = JOptionPane.showConfirmDialog(ArticleGUI.this,
                                    "Der Lagerbestand ist niedriger als die gewünschte Menge.\nMöchten Sie trotzdem fortfahren?",
                                    "Niedriger Lagerbestand",
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE,
                                    Main.iconSmall);
                            if (confirm != JOptionPane.YES_OPTION) {
                                return;
                            }
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
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Ungültige Menge: " + input, "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
        JMenuItem editItem = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        JMenuItem deleteItem = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");

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

    /**
     * Display all warnings from WarningManager in a modern dialog
     */
    private void showAllWarnings() {
        DisplayWarningDialog.showAllWarnings(this);
    }

    /**
     * Load categories from categories.json resource file
     */
    private void loadCategories() {
        categories = new HashMap<>();
        try {
            InputStream is = getClass().getResourceAsStream("/categories.json");
            if (is == null) {
                System.err.println("categories.json not found in resources");
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();
            List<Map<String, String>> categoryList = gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    listType
            );

            for (Map<String, String> cat : categoryList) {
                String categoryName = cat.get("category");
                String fromTo = cat.get("fromTo");

                if (categoryName != null && fromTo != null) {
                    // Parse range like "1101 - 1116" or "1301"
                    String[] parts = fromTo.split("-");
                    int start, end;

                    if (parts.length == 2) {
                        start = Integer.parseInt(parts[0].trim());
                        end = Integer.parseInt(parts[1].trim());
                    } else {
                        start = end = Integer.parseInt(parts[0].trim());
                    }

                    categories.put(categoryName, new CategoryRange(categoryName, start, end));
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Fehler beim Laden der Kategorien: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
        }
    }

    /**
     * Get category for an article based on its article number
     */
    private String getCategoryForArticle(String articleNumber) {
        if (categories == null || articleNumber == null) {
            return "Unbekannt";
        }

        try {
            // Extract numeric part from article number (e.g., "1101" from "1101-ABC")
            String numericPart = articleNumber.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                return "Unbekannt";
            }

            int articleNum = Integer.parseInt(numericPart);

            // Find matching category range
            for (CategoryRange range : categories.values()) {
                if (articleNum >= range.rangeStart && articleNum <= range.rangeEnd) {
                    return range.category;
                }
            }
        } catch (NumberFormatException e) {
            // Article number doesn't contain valid number
        }

        return "Unbekannt";
    }

    /**
     * Filter table by selected category
     */
    private void filterByCategory() {
        applyAdvancedFilters();
    }

    private void applyAdvancedFilters() {
        String selectedCategory = categoryFilter == null ? null : (String) categoryFilter.getSelectedItem();
        String vendorQuery = vendorFilterField == null ? "" : vendorFilterField.getText().trim().toLowerCase();
        Integer stockMin = parseIntegerFilter(stockMinField);
        Integer stockMax = parseIntegerFilter(stockMaxField);
        Double priceMin = parseDoubleFilter(priceMinField);
        Double priceMax = parseDoubleFilter(priceMaxField);

        @SuppressWarnings("unchecked")
        TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) articleTable.getRowSorter();
        if (sorter == null) {
            sorter = new TableRowSorter<>((DefaultTableModel) articleTable.getModel());
            articleTable.setRowSorter(sorter);
        }

        String categoryFilterValue = selectedCategory == null ? "Alle Kategorien" : selectedCategory;

        RowFilter<DefaultTableModel, Integer> rowFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (!"Alle Kategorien".equals(categoryFilterValue)) {
                    String categoryValue = entry.getStringValue(2);
                    if (!categoryFilterValue.equals(categoryValue)) {
                        return false;
                    }
                }

                if (!vendorQuery.isEmpty()) {
                    String vendorValue = entry.getStringValue(8).toLowerCase();
                    if (!vendorValue.contains(vendorQuery)) {
                        return false;
                    }
                }

                if (stockMin != null || stockMax != null) {
                    Integer stockValue = parseIntegerFilter(entry.getValue(4));
                    if (stockValue == null) {
                        return false;
                    }
                    if (stockMin != null && stockValue < stockMin) {
                        return false;
                    }
                    if (stockMax != null && stockValue > stockMax) {
                        return false;
                    }
                }

                if (priceMin != null || priceMax != null) {
                    Double priceValue = parseDoubleFilter(entry.getValue(6));
                    if (priceValue == null) {
                        return false;
                    }
                    if (priceMin != null && priceValue < priceMin) {
                        return false;
                    }
                    return priceMax == null || priceValue <= priceMax;
                }

                return true;
            }
        };

        sorter.setRowFilter(rowFilter);
        updateCountLabel();
    }

    private Integer parseIntegerFilter(JTextField field) {
        if (field == null) {
            return null;
        }
        String text = field.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer parseIntegerFilter(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Double parseDoubleFilter(JTextField field) {
        if (field == null) {
            return null;
        }
        String text = field.getText();
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parseDoubleFilter(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Display QR-Code data in a modern, optimized dialog with import functionality
     * Features:
     * - Async data loading with progress feedback
     * - Multiple import modes (single, multiple, all)
     * - Eigenverbrauch (own use) support
     * - Order list integration
     * - Visual status indicators
     */
    private void showQRCodeDataDialog() {
        ArticleQrCodeDialog.show(this);
    }

    /**
     * Creates a styled category filter panel with combobox
     * Modern design with proper spacing, rounded borders, and theme support
     */
    private JPanel createCategoryFilterPanel() {
        // Create rounded panel with card-like appearance
        RoundedPanel categoryPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 8);
        categoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        categoryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Create and style label with icon
        JLabel categoryLabel = new JLabel(UnicodeSymbols.FOLDER + " Kategorie:");
        categoryLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        categoryLabel.setForeground(ThemeManager.getTextPrimaryColor());

        // Initialize combobox with categories
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("Alle Kategorien");
        if (categories != null) {
            categories.keySet().stream().sorted().forEach(categoryFilter::addItem);
        }
        categoryFilter.setPreferredSize(new Dimension(220, 38));
        categoryFilter.addActionListener(e -> applyAdvancedFilters());
        categoryFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Apply theme-aware styling
        styleComboBox(categoryFilter);

        // Assemble panel
        categoryPanel.add(categoryLabel);
        categoryPanel.add(categoryFilter);

        return categoryPanel;
    }

    private JPanel createToolbarGroup() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setOpaque(false);
        return panel;
    }

    private JComponent createToolbarDivider() {
        JLabel divider = new JLabel("\u2022");
        divider.setForeground(ThemeManager.getTextSecondaryColor());
        divider.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        return divider;
    }

    private JPanel createAdvancedFilterPanel() {
        RoundedPanel filterPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 8);
        filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        filterPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        JLabel vendorLabel = new JLabel(UnicodeSymbols.TRUCK + " Lieferant:");
        vendorLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        vendorLabel.setForeground(ThemeManager.getTextPrimaryColor());
        vendorFilterField = new JTextField(10);
        vendorFilterField.setToolTipText("Lieferant enthaelt...");
        styleFilterField(vendorFilterField);

        JLabel stockLabel = new JLabel(UnicodeSymbols.PACKAGE + " Lager:");
        stockLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        stockLabel.setForeground(ThemeManager.getTextPrimaryColor());
        stockMinField = new JTextField(4);
        stockMinField.setToolTipText("Min");
        styleFilterField(stockMinField);
        stockMaxField = new JTextField(4);
        stockMaxField.setToolTipText("Max");
        styleFilterField(stockMaxField);

        JLabel priceLabel = new JLabel(UnicodeSymbols.MONEY + " Preis:");
        priceLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        priceLabel.setForeground(ThemeManager.getTextPrimaryColor());
        priceMinField = new JTextField(5);
        priceMinField.setToolTipText("Min");
        styleFilterField(priceMinField);
        priceMaxField = new JTextField(5);
        priceMaxField.setToolTipText("Max");
        styleFilterField(priceMaxField);

        filterPanel.add(vendorLabel);
        filterPanel.add(vendorFilterField);
        filterPanel.add(stockLabel);
        filterPanel.add(stockMinField);
        filterPanel.add(new JLabel("-"));
        filterPanel.add(stockMaxField);
        filterPanel.add(priceLabel);
        filterPanel.add(priceMinField);
        filterPanel.add(new JLabel("-"));
        filterPanel.add(priceMaxField);

        addFilterListener(vendorFilterField);
        addFilterListener(stockMinField);
        addFilterListener(stockMaxField);
        addFilterListener(priceMinField);
        addFilterListener(priceMaxField);

        return filterPanel;
    }

    private void styleFilterField(JTextField field) {
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
    }

    private void addFilterListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyAdvancedFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyAdvancedFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyAdvancedFilters();
            }
        });
    }

    /**
     * Applies consistent theme-aware styling to the category combobox
     * Handles background, foreground, editor, popup list styling, and hover effects
     */
    private void styleComboBox(JComboBox<String> combo) {
        Color bg = ThemeManager.getInputBackgroundColor();
        Color fg = ThemeManager.getTextPrimaryColor();
        Color border = ThemeManager.getInputBorderColor();
        Color selBg = ThemeManager.getSelectionBackgroundColor();
        Color selFg = ThemeManager.getSelectionForegroundColor();
        Color surface = ThemeManager.getSurfaceColor();

        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setOpaque(true);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // enforce popup list colors
                list.setBackground(bg);
                list.setForeground(fg);
                list.setSelectionBackground(selBg);
                list.setSelectionForeground(selFg);

                c.setOpaque(true);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    c.setBackground(selBg);
                    c.setForeground(selFg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(fg);
                }
                return c;
            }
        });

        // Theme arrow button + popup border (reliable across LAFs)
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton(UnicodeSymbols.ARROW_DOWN);
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        b.setBackground(surface);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        b.setBackground(bg);
                    }
                });
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof BasicComboPopup basic) {
                    basic.setBorder(BorderFactory.createLineBorder(border, 1));
                    basic.getList().setBackground(bg);
                    basic.getList().setForeground(fg);
                    basic.getList().setSelectionBackground(selBg);
                    basic.getList().setSelectionForeground(selFg);
                }
                return popup;
            }
        });

        if (combo.isEditable()) {
            Component editorComp = combo.getEditor().getEditorComponent();
            if (editorComp instanceof JTextField tf) {
                tf.setBackground(bg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBorder(null);
            }
        }
    }

    /**
     * Display detailed stock statistics in a modern themed dialog
     */
    private void showDetails() {
        ArticleStatsDialog.show(this, articleTable);
    }
}
