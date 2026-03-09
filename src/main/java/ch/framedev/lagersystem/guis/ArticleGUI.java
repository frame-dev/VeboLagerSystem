package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.*;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.*;
import ch.framedev.lagersystem.managers.ThemeManager;
import org.apache.logging.log4j.Level;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static ch.framedev.lagersystem.main.Main.articleListGUI;
import static ch.framedev.lagersystem.main.Main.logUtils;
import static ch.framedev.lagersystem.utils.ArticleUtils.*;
import static ch.framedev.lagersystem.utils.JFrameUtils.*;

/**
 * ArticleGUI with category support for better organization.
 * Categories are loaded from categories.json and mapped to articles based on
 * article number ranges.
 */
@SuppressWarnings({ "ALL" })
public class ArticleGUI extends JFrame {

    public final JTable articleTable;
    // scroll pane reference so we can read viewport width on resize
    private final JScrollPane tableScrollPane;
    // base column widths - added category column between Name and Details
    private final int[] baseColumnWidths = new int[] { 150, 200, 150, 290, 110, 110, 150, 150, 200 };
    private final JLabel countLabel;
    private boolean isUpdatingTable = false;
    private JTextField vendorFilterField;
    private JTextField stockMinField;
    private JTextField stockMaxField;
    private JTextField priceMinField;
    private JTextField priceMaxField;

    // Category management
    private JComboBox<String> categoryFilter;

    /**
     * Initializes the ArticleGUI, sets up the layout, loads categories and
     * articles, and configures interactions.
     */
    @SuppressWarnings("unchecked")
    public ArticleGUI() {
        ThemeManager.getInstance().registerWindow(this);

        setTitle("Artikel Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setMinimumSize(new Dimension(1080, 680));
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

        // ===== Top area (Header + Toolbar) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JPanel headerWrapper = null;
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (!disableHeader) {

            // Header card (no fixed height -> prevents clipping)
            RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
            headerPanel.setLayout(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel(UnicodeSymbols.ARTICLE_NAME + " Artikel Verwaltung");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Artikel verwalten, filtern und durchsuchen");
            subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

            JPanel headerText = new JPanel();
            headerText.setOpaque(false);
            headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerText.add(titleLabel);
            headerText.add(Box.createVerticalStrut(4));
            headerText.add(subtitleLabel);

            headerPanel.add(headerText, BorderLayout.WEST);

            headerWrapper = new JPanel(new BorderLayout());
            headerWrapper.setOpaque(false);
            headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerWrapper.add(headerPanel, BorderLayout.CENTER);
        }

        // =========================
        // Toolbar
        // =========================
        RoundedPanel toolbarCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        toolbarCard.setLayout(new BorderLayout());
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

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
                if (row.length != 8)
                    throw new IllegalArgumentException("Invalid number of fields in add article dialog");
                ArticleManager articleManager = ArticleManager.getInstance();
                Article article = new Article(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        (Integer) row[3],
                        (Integer) row[4],
                        (Double) row[5],
                        (Double) row[6],
                        (String) row[7]);
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
                if (updatedRow.length != 8)
                    throw new IllegalArgumentException("Invalid number of fields in update article dialog");
                // updatedRow: [artikelNr, name, details, lager, mindest, verkauf, einkauf,
                // lieferant]
                DefaultTableModel model = (DefaultTableModel) articleTable.getModel();

                model.setValueAt(updatedRow[0], modelRow, 0); // Artikelnummer
                model.setValueAt(updatedRow[1], modelRow, 1); // Name
                String category = getCategoryForArticle((String) updatedRow[0]);
                model.setValueAt(category, modelRow, 2); // Kategorie (recalculated)
                model.setValueAt(updatedRow[2], modelRow, 3); // Details
                model.setValueAt(updatedRow[3], modelRow, 4); // Lagerbestand
                model.setValueAt(updatedRow[4], modelRow, 5); // Mindestbestand
                model.setValueAt(parseDouble(updatedRow[5]), modelRow, 6); // Verkaufspreis
                model.setValueAt(parseDouble(updatedRow[6]), modelRow, 7); // Einkaufspreis
                model.setValueAt(updatedRow[7], modelRow, 8); // Lieferant

                ArticleManager articleManager = ArticleManager.getInstance();
                Article article = new Article(
                        (String) updatedRow[0], // Artikelnummer
                        (String) updatedRow[1], // Name
                        (String) updatedRow[2], // Details
                        (Integer) updatedRow[3], // Lagerbestand
                        (Integer) updatedRow[4], // Mindestbestand
                        parseDouble(updatedRow[5]), // Verkaufspreis
                        parseDouble(updatedRow[6]), // Einkaufspreis
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
        bulkExportButton.addActionListener(e -> exportSelectedArticles(this, articleTable));

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
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toolbarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        toolbarScrollPane.setOpaque(false);
        toolbarScrollPane.getViewport().setOpaque(false);
        toolbarScrollPane.getHorizontalScrollBar().setUnitIncrement(16);

        // Top area: header + toolbar stacked
        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }
        topContainer.add(toolbarScrollPane);
        add(topContainer, BorderLayout.NORTH);

        // =========================
        // Main card/table area
        // =========================
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        articleTable.setPreferredScrollableViewportSize(new Dimension(920, 420));
        tableScrollPane = new JScrollPane(articleTable);
        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        tableScrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        tableScrollPane.setBackground(ThemeManager.getCardBackgroundColor());
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

        // ===== Bottom search bar (card – like VendorGUI/ClientGUI) =====
        RoundedPanel searchCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche:");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        JTextField searchField = new JTextField(30);
        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);
        leftSearch.add(searchLabel);
        leftSearch.add(searchField);

        // Right-side actions (prettier grouping)
        JPanel rightActions = new JPanel();
        rightActions.setOpaque(false);
        rightActions.setLayout(new BoxLayout(rightActions, BoxLayout.X_AXIS));

        // Count badge
        countLabel = new JLabel();
        styleCountBadge(countLabel);
        updateCountLabel();

        // Actions (use secondary palette to keep the bottom bar light)
        JButton addToClientOrder = createSecondaryButton(UnicodeSymbols.SHOPPING_CART + " Zur Kundenbestellung");
        addToClientOrder.setToolTipText("Fügt die ausgewählten Artikel zur aktuellen Kundenbestellung hinzu");
        addToClientOrder.addActionListener(e -> addSelectedArticlesToClientOrder(this, articleTable));

        JButton exportTableAsPdfBtn = createSecondaryButton(UnicodeSymbols.DOWNLOAD + " Tabelle als PDF");
        exportTableAsPdfBtn.setToolTipText("Exportiert die Tabelle als PDF");
        exportTableAsPdfBtn.addActionListener(e -> exportTableAsPDF());

        JButton showWarningsBottomBtn = createSecondaryButton(UnicodeSymbols.WARNING + " Warnungen");
        applyButtonPalette(showWarningsBottomBtn, ThemeManager.getWarningColor());
        showWarningsBottomBtn.setToolTipText("Zeigt alle Lagerwarnungen an");
        showWarningsBottomBtn.addActionListener(e -> showAllWarnings());

        JButton searchBtn = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Sucht nach Artikeln basierend auf dem eingegebenen Text");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.BROOM + " Leeren");
        clearBtn.setToolTipText("Löscht die Suchfilter und zeigt alle Artikel an");

        // Assemble right side with spacing + subtle separators
        rightActions.add(countLabel);
        rightActions.add(Box.createHorizontalStrut(10));
        rightActions.add(createMiniDivider());
        rightActions.add(Box.createHorizontalStrut(10));

        rightActions.add(addToClientOrder);
        rightActions.add(Box.createHorizontalStrut(8));
        rightActions.add(exportTableAsPdfBtn);
        rightActions.add(Box.createHorizontalStrut(8));
        rightActions.add(showWarningsBottomBtn);

        rightActions.add(Box.createHorizontalStrut(10));
        rightActions.add(createMiniDivider());
        rightActions.add(Box.createHorizontalStrut(10));

        rightActions.add(searchBtn);
        rightActions.add(Box.createHorizontalStrut(8));
        rightActions.add(clearBtn);

        searchCard.add(leftSearch, BorderLayout.CENTER);
        searchCard.add(rightActions, BorderLayout.EAST);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(ThemeManager.getBackgroundColor());
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        searchWrapper.add(searchCard, BorderLayout.CENTER);

        add(searchWrapper, BorderLayout.SOUTH);

        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
            TableRowSorter<DefaultTableModel> sorter;
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                // noinspection unchecked
                sorter = (TableRowSorter<DefaultTableModel>) articleTable.getRowSorter();
            } else {
                sorter = new TableRowSorter<>((DefaultTableModel) articleTable.getModel());
                articleTable.setRowSorter(sorter);
            }
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                String regex = "(?i)" + Pattern.quote(text);
                sorter.setRowFilter(RowFilter.regexFilter(regex));
            }
            updateCountLabel();
        };

        // Live filter while typing
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                doSearch.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                doSearch.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                doSearch.run();
            }
        });

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                ((TableRowSorter<?>) articleTable.getRowSorter()).setRowFilter(null);
            }
            updateCountLabel();
        });

        searchField.addActionListener(e -> doSearch.run());
        // ESC clears
        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    if (articleTable.getRowSorter() instanceof TableRowSorter) {
                        ((TableRowSorter<?>) articleTable.getRowSorter()).setRowFilter(null);
                    }
                    updateCountLabel();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED);

        // Auto-resize columns when the window or viewport changes size
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths(articleTable, tableScrollPane, baseColumnWidths);
            }
        };
        this.addComponentListener(resizeListener);
        tableScrollPane.getViewport().addComponentListener(resizeListener);

        SwingUtilities.invokeLater(() -> adjustColumnWidths(articleTable, tableScrollPane, baseColumnWidths));
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
     * Shows a polished modal dialog to enter a new article. Returns the table row
     * or null if canceled.
     */
    private Object[] showAddArticleDialog() {
        return ArticleDialog.showAddArticleDialog(this);
    }

    private void initializeTable() {
        if (articleTable == null) {
            throw new IllegalStateException("Article table has not been initialized");
        }
        // Provide a typed, non-editable table model so sorting & renderers behave
        // correctly
        DefaultTableModel model = getDefaultTableModel();

        // Set model
        articleTable.setModel(model);
        registerInlineEditListener(model);

        // Visual tweaks
        articleTable.setRowHeight(26);
        articleTable.setAutoCreateRowSorter(true);
        articleTable.getTableHeader().setReorderingAllowed(false);
        articleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        // Let JTable resize all columns proportionally to fill the viewport when the
        // window resizes
        articleTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Make grid lines visible and subtle (adds vertical+horizontal lines like in
        // your mockup)
        articleTable.setShowGrid(true);
        articleTable.setShowHorizontalLines(true);
        articleTable.setShowVerticalLines(true);
        articleTable.setGridColor(ThemeManager.getTableGridColor()); // soft light gray // soft light gray
        articleTable.setIntercellSpacing(new Dimension(1, 1));
        articleTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));

        // Alternating row colors for readability (subtle)
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected)
                    c.setBackground(
                            row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
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
                if (value instanceof Number)
                    setText(value + " CHF");
                else
                    setText(value == null ? "" : value.toString());
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
     * Uses a landscape A3 format for maximum space and dynamically scales content
     * to fit.
     */
    private void exportTableAsPDF() {
        ArticleExporter.exportTableAsPdf(this, articleTable, baseColumnWidths, Main.iconSmall);
    }

    private static DefaultTableModel getDefaultTableModel() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 4, 5 -> Integer.class; // Lagerbestand, Mindestbestand (shifted by 1)
                    case 6, 7 -> Double.class; // Verkaufspreis, Einkaufspreis (shifted by 1)
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
        model.addColumn(UnicodeSymbols.CATEGORY + " Kategorie"); // New category column
        model.addColumn(UnicodeSymbols.CLIPBOARD + " Details");
        model.addColumn(UnicodeSymbols.COL_LAGERBESTAND + " Lagerbestand");
        model.addColumn(UnicodeSymbols.COL_MINDESTBESTAND + " Mindestbestand");
        model.addColumn(UnicodeSymbols.MONEY + " Verkaufspreis");
        model.addColumn(UnicodeSymbols.MONEY + " Einkaufspreis");
        model.addColumn(UnicodeSymbols.TRUCK + " Lieferant");
        return model;
    }

    /**
     * Displays the ArticleGUI window. This method can be called from the main menu
     * or other parts of the application to show the article management interface.
     */
    public void display() {
        setVisible(true);
    }

    private JButton createRoundedButton(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text must not be null");
        }
        return JFrameUtils.createRoundedButton(text);
    }

    private void styleTextField(JTextField tf) {
        if (tf == null) {
            throw new IllegalArgumentException("Textfield cannot be null");
        }
        JFrameUtils.styleTextField(tf);
    }

    private void loadArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Article> articles = articleManager.getAllArticles();
        if (articles == null)
            return;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        isUpdatingTable = true;
        model.setRowCount(0);
        for (Article a : articles) {
            if (a == null)
                throw new NullPointerException("Article is null");
            String category = getCategoryForArticle(a.getArticleNumber());
            model.addRow(new Object[] {
                    a.getArticleNumber(),
                    a.getName(),
                    category, // Category column
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
        if (countLabel != null && articleTable != null) {
            int filtered = articleTable.getRowCount();
            int total = 0;
            if (articleTable.getModel() instanceof DefaultTableModel model) {
                total = model.getRowCount();
            }
            countLabel.setText("Artikel: " + filtered + " / " + total);
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
                vendor);

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
            logUtils.addLog(Level.ERROR, "Article not found in database: " + articleNumber);
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
            str = str.replace(" CHF", "").trim();
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
            str = str.replace(" CHF", "").trim();
            try {
                return Double.parseDouble(str.trim().replace(" CHF", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void deleteSelectedArticle() {
        int selectedRow = articleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Artikel zum Löschen aus.", "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Artikel wirklich löschen?",
                "Artikel löschen", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                Main.iconSmall);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        int modelRow = articleTable.convertRowIndexToModel(selectedRow);
        String artikelnummer = (String) model.getValueAt(modelRow, 0);

        // Remove from a database first
        ArticleManager articleManager = ArticleManager.getInstance();
        if (articleManager.deleteArticleByNumber(artikelnummer)) {
            // Only remove from the table if DB deletion succeeded
            model.removeRow(modelRow);
            updateCountLabel();
            JOptionPane.showMessageDialog(this, "Artikel erfolgreich gelöscht.", "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
        } else {
            JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Artikels aus der Datenbank.", "Fehler",
                    JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
        List<Article> selected = getSelectedArticles(articleTable);
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
            if (article == null) {
                throw new IllegalArgumentException("Artikel ist null.");
            }
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
                    article.getVendorName());
            if (articleManager.updateArticle(updated)) {
                model.setValueAt(newStock, modelRow, 4);
            }
        }
        isUpdatingTable = false;
    }

    /**
     * Generate QR codes for the selected articles and save them to disk, showing a
     * progress dialog during generation.
     * Uses a SwingWorker to keep the UI responsive and handles errors gracefully.
     */
    private void generateQrCodesForSelectedArticles() {
        List<Article> selectedArticles = getSelectedArticles(articleTable);
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
                "QR-Codes fuer " + selectedArticles.size() + " ausgewählte Artikel generieren?\nSpeicherort: "
                        + outputDir.getAbsolutePath(),
                "QR-Codes generieren",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
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
                                Main.iconSmall);
                        return;
                    }
                    JOptionPane.showMessageDialog(
                            ArticleGUI.this,
                            "QR-Codes wurden erstellt.\nAnzahl: " + files.size() + "\nOrdner: "
                                    + outputDir.getAbsolutePath(),
                            "QR-Codes generieren",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            ArticleGUI.this,
                            "Fehler beim Erstellen der QR-Codes: " + ex.getMessage(),
                            "QR-Codes generieren",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        };

        worker.execute();
        progressDialog.setVisible(true);
    }

    /**
     * Show a preview dialog for the QR codes of the selected articles, allowing
     * users to see and print them without saving to disk.
     */
    private void showQrCodePreviewDialog() {
        ArticleQrPreviewDialog.show(this, getSelectedArticles(articleTable));
    }

    /**
     * A custom JPanel with rounded corners and a specified background color. Used
     * for the header section of the ArticleGUI to create a visually appealing
     * design that stands out from the rest of the interface.
     */
    public static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        /**
         * Create a panel with rounded corners and a custom background color.
         *
         * @param bg     The background color of the panel
         * @param radius The radius of the rounded corners in pixels
         */
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

    /**
     * Set up mouse interactions for the article table, including double-click to
     * add to order list and right-click context menu
     */
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
                    if (row == -1)
                        return;

                    int modelRow = articleTable.convertRowIndexToModel(row);
                    Object[] existingData = ((DefaultTableModel) articleTable.getModel()).getDataVector()
                            .elementAt(modelRow).toArray();
                    String artikelNr = (String) existingData[0];

                    Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
                    if (article == null) {
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Artikel konnte nicht gefunden werden.",
                                "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                        return;
                    }

                    String input = JOptionPane.showInputDialog(ArticleGUI.this,
                            "Geben Sie die Menge für \"" + article.getName() + "\" ein:",
                            "Zur Bestellung hinzufügen",
                            JOptionPane.PLAIN_MESSAGE);

                    if (input == null)
                        return; // User cancelled

                    try {
                        int quantity = Integer.parseInt(input.trim());
                        if (quantity <= 0) {
                            JOptionPane.showMessageDialog(ArticleGUI.this, "Menge muss größer als 0 sein.",
                                    "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
                        JOptionPane.showMessageDialog(ArticleGUI.this, "Ungültige Menge: " + input, "Fehler",
                                JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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

    /**
     * Create a context menu for the article table with options to edit or delete
     * the selected article
     */
    private JPopupMenu createTablePopup() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem editItem = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        JMenuItem deleteItem = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");

        editItem.addActionListener(e -> {
            int sel = articleTable.getSelectedRow();
            if (sel == -1)
                return;
            int modelRow = articleTable.convertRowIndexToModel(sel);
            Object[] existingData = ((DefaultTableModel) articleTable.getModel()).getDataVector().elementAt(modelRow)
                    .toArray();
            Object[] updated = showUpdateArticleDialog(existingData);
            if (updated != null) {
                DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
                for (int i = 0; i < updated.length; i++)
                    model.setValueAt(updated[i], modelRow, i);
                ArticleManager.getInstance().updateArticle(new Article(
                        (String) updated[0], (String) updated[1], (String) updated[2], (Integer) updated[3],
                        (Integer) updated[4], (Double) updated[5], (Double) updated[6], (String) updated[7]));
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
        loadCategories();
        // Create a rounded panel with a card-like appearance
        RoundedPanel categoryPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 8);
        categoryPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        categoryPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        // Create and style a label with an icon
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

    @SuppressWarnings("UnnecessaryUnicodeEscape")
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
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

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
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
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
        if (combo == null) {
            throw new IllegalArgumentException("ComboBox cannot be null");
        }
        JFrameUtils.styleComboBox(combo);
    }

    /**
     * Display detailed stock statistics in a modern themed dialog
     */
    private void showDetails() {
        ArticleStatsDialog.show(this, articleTable);
    }

    private void styleCountBadge(JLabel label) {
        if (label == null) {
            throw new IllegalArgumentException("Label cannot be null");
        }
        label.setOpaque(true);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private JComponent createMiniDivider() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(ThemeManager.getBorderColor());
        p.setPreferredSize(new Dimension(1, 22));
        p.setMinimumSize(new Dimension(1, 22));
        p.setMaximumSize(new Dimension(1, 22));
        return p;
    }
}