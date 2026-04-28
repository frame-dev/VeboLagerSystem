package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.SeperateArticle;
import ch.framedev.lagersystem.dialogs.*;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.*;
import ch.framedev.lagersystem.managers.ThemeManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    private static final Logger LOGGER = LogManager.getLogger(ArticleGUI.class);
    private static final String FILTER_KEY_SEARCH = "ui.filter.articles.search";
    private static final String FILTER_KEY_CATEGORY = "ui.filter.articles.category";
    private static final String FILTER_KEY_VENDOR = "ui.filter.articles.vendor";
    private static final String FILTER_KEY_STOCK_MIN = "ui.filter.articles.stock.min";
    private static final String FILTER_KEY_STOCK_MAX = "ui.filter.articles.stock.max";
    private static final String FILTER_KEY_PRICE_MIN = "ui.filter.articles.price.min";
    private static final String FILTER_KEY_PRICE_MAX = "ui.filter.articles.price.max";

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
    private JTextField searchField;
    private SwingWorker<List<Article>, Void> articleLoadWorker;
    private int articleLoadGeneration = 0;
    private final Deque<UndoableArticleAction> undoStack = new ArrayDeque<>();
    private final Deque<UndoableArticleAction> redoStack = new ArrayDeque<>();
    private JButton undoButton;
    private JButton redoButton;

    // Category management
    private JComboBox<String> categoryFilter;

    /**
     * Initializes the ArticleGUI, sets up the layout, loads categories and
     * articles, and configures interactions.
     */
    public ArticleGUI() {
        ThemeManager.getInstance().registerWindow(this);
        LOGGER.info("Initializing ArticleGUI window");

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

            JPanel headerText = JFrameUtils.createHeaderTextPanel(titleLabel, subtitleLabel, 4);

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
                    LOGGER.warn("Failed to add article '{}' because the insert returned false", article.getArticleNumber());
                    MessageDialog messageDialog = new MessageDialog(
                            "Fehler beim Hinzufügen des Artikels. Möglicherweise existiert die Artikelnummer bereits.",
                            "Fehler", 5000);
                    messageDialog.display();
                } else {
                    LOGGER.info("Added article '{}' ({})", article.getArticleNumber(), article.getName());
                    logUtils.addLog(Level.INFO, "Artikel hinzugefügt: " + article.getArticleNumber() + " - " + article.getName());
                    loadArticles();
                    MessageDialog messageDialog = new MessageDialog(
                            "Artikel erfolgreich hinzugefügt.",
                            "Erfolg", 5000);
                    messageDialog.display();
                }
            }
        });

        // Edit Article button
        JButton editArticleButton = createRoundedButton(UnicodeSymbols.CODE + " Artikel bearbeiten");
        editArticleButton.setToolTipText("Bearbeitet den ausgewählten Artikel");
        editArticleButton.addActionListener(e -> {
            int selectedRow = articleTable.getSelectedRow();
            if (selectedRow == -1) {
                MessageDialog messageDialog = new MessageDialog("Bitte wählen Sie einen Artikel zum Bearbeiten aus.",
                        "Keine Auswahl", 5000);
                messageDialog.display();
                return;
            }
            editArticleAtModelRow(articleTable.convertRowIndexToModel(selectedRow));
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

        undoButton = createRoundedButton("↶ Rückgängig");
        undoButton.setToolTipText("Letzte Löschung oder Bestandsänderung rückgängig machen");
        undoButton.addActionListener(e -> undoLastArticleAction());

        redoButton = createRoundedButton("↷ Wiederholen");
        redoButton.setToolTipText("Zuletzt rückgängig gemachte Aktion erneut ausführen");
        redoButton.addActionListener(e -> redoLastArticleAction());

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

        JButton showSeparatedArticlesButton = createRoundedButton(UnicodeSymbols.CLIPBOARD + " Getrennte Artikel");
        showSeparatedArticlesButton.setToolTipText("Zeigt alle getrennten Artikel an");
        showSeparatedArticlesButton.addActionListener(e -> showSeparatedArticlesDialog());

        JPanel actionGroup = createToolbarGroup();
        actionGroup.add(addArticleButton);
        actionGroup.add(editArticleButton);
        actionGroup.add(deleteArticleButton);

        JPanel bulkGroup = createToolbarGroup();
        bulkGroup.add(bulkDeleteButton);
        bulkGroup.add(bulkAdjustStockButton);
        bulkGroup.add(bulkExportButton);

        JPanel historyGroup = createToolbarGroup();
        historyGroup.add(undoButton);
        historyGroup.add(redoButton);

        JPanel qrGroup = createToolbarGroup();
        qrGroup.add(generateQrCodesButton);
        qrGroup.add(qrPreviewPdfButton);
        qrGroup.add(retrieveQrCodeDataButton);

        JPanel infoGroup = createToolbarGroup();
        infoGroup.add(showDetailsButton);
        infoGroup.add(showSeparatedArticlesButton);

        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(actionGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(bulkGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(historyGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(qrGroup);
        toolbarWrapper.add(createToolbarDivider());
        toolbarWrapper.add(infoGroup);

        toolbarCard.add(toolbarWrapper, BorderLayout.CENTER);
        updateUndoRedoState();

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

        searchField = new JTextField(30);
        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JPanel searchInputRow = new JPanel(new BorderLayout(10, 0));
        searchInputRow.setOpaque(false);
        searchInputRow.add(searchLabel, BorderLayout.WEST);
        searchInputRow.add(searchField, BorderLayout.CENTER);

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

        JButton refreshBtn = createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshBtn.setToolTipText("Lädt die Artikeltabelle neu");
        refreshBtn.addActionListener(e -> loadArticles());

        JButton searchBtn = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Sucht nach Artikeln basierend auf dem eingegebenen Text");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.BROOM + " Leeren");
        clearBtn.setToolTipText("Löscht die Suchfilter und zeigt alle Artikel an");

        JPanel topRow = new JPanel(new BorderLayout(10, 8));
        topRow.setOpaque(false);
        topRow.add(searchInputRow, BorderLayout.CENTER);
        topRow.add(countLabel, BorderLayout.EAST);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setOpaque(false);
        actionRow.add(addToClientOrder);
        actionRow.add(exportTableAsPdfBtn);
        actionRow.add(showWarningsBottomBtn);
        actionRow.add(refreshBtn);
        actionRow.add(searchBtn);
        actionRow.add(clearBtn);

        JPanel searchContent = new JPanel();
        searchContent.setOpaque(false);
        searchContent.setLayout(new BoxLayout(searchContent, BoxLayout.Y_AXIS));
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchContent.add(topRow);
        searchContent.add(Box.createVerticalStrut(8));
        searchContent.add(actionRow);

        searchCard.add(searchContent, BorderLayout.CENTER);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(ThemeManager.getBackgroundColor());
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        searchWrapper.add(searchCard, BorderLayout.CENTER);

        add(searchWrapper, BorderLayout.SOUTH);

        // Live filter while typing
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyCombinedFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyCombinedFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyCombinedFilters();
            }
        });

        searchBtn.addActionListener(e -> applyCombinedFilters());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            applyCombinedFilters();
        });

        searchField.addActionListener(e -> applyCombinedFilters());
        // ESC clears
        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    applyCombinedFilters();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED);

        restoreTableFilters();

        // Auto-resize columns when the window or viewport changes size
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths(articleTable, tableScrollPane, baseColumnWidths);
            }
        };
        this.addComponentListener(resizeListener);
        tableScrollPane.getViewport().addComponentListener(resizeListener);

        installKeyboardShortcuts(addArticleButton, editArticleButton, deleteArticleButton, refreshBtn, clearBtn);
        SwingUtilities.invokeLater(() -> adjustColumnWidths(articleTable, tableScrollPane, baseColumnWidths));
    }

    @Override
    public void dispose() {
        if (articleLoadWorker != null && !articleLoadWorker.isDone()) {
            articleLoadWorker.cancel(true);
        }
        LOGGER.info("Disposing ArticleGUI window");
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void installKeyboardShortcuts(JButton addArticleButton, JButton editArticleButton,
                                          JButton deleteArticleButton, JButton refreshButton, JButton clearButton) {
        JRootPane rootPane = getRootPane();
        KeyboardShortcutUtils.addTooltipHint(searchField, KeyboardShortcutUtils.menuKey(KeyEvent.VK_F));
        KeyboardShortcutUtils.addTooltipHint(addArticleButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_N));
        KeyboardShortcutUtils.addTooltipHint(editArticleButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_E));
        KeyboardShortcutUtils.addTooltipHint(deleteArticleButton, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        KeyboardShortcutUtils.addTooltipHint(refreshButton, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        KeyboardShortcutUtils.addTooltipHint(clearButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_L));
        KeyboardShortcutUtils.addTooltipHint(undoButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_Z));
        KeyboardShortcutUtils.addTooltipHint(redoButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_Y));
        KeyboardShortcutUtils.registerClose(this);
        KeyboardShortcutUtils.registerFocus(rootPane, "articles.focusSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_F), searchField);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.add",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_N), addArticleButton);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.edit",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_E), editArticleButton);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.delete",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), deleteArticleButton, true);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.refresh",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshButton);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.clearSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_L), clearButton);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.undo",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_Z), undoButton);
        KeyboardShortcutUtils.registerButton(rootPane, "articles.redo",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_Y), redoButton);
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
        int loadGeneration = ++articleLoadGeneration;
        if (articleLoadWorker != null && !articleLoadWorker.isDone()) {
            articleLoadWorker.cancel(true);
        }
        if (countLabel != null) {
            countLabel.setText("Artikel werden geladen...");
        }
        articleLoadWorker = new SwingWorker<>() {
            @Override
            protected List<Article> doInBackground() {
                List<Article> articles = ArticleManager.getInstance().getAllArticles();
                return articles != null ? articles : java.util.Collections.emptyList();
            }

            @Override
            protected void done() {
                if (isCancelled() || loadGeneration != articleLoadGeneration) {
                    return;
                }
                try {
                    populateArticles(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("Article loading was interrupted");
                    updateCountLabel();
                } catch (java.util.concurrent.ExecutionException ex) {
                    LOGGER.error("Failed to load articles for ArticleGUI", ex);
                    updateCountLabel();
                }
            }
        };
        articleLoadWorker.execute();
    }

    private void populateArticles(List<Article> articles) {
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
        applyCombinedFilters();
        LOGGER.info("Loaded {} articles into ArticleGUI", articles.size());
    }

    private void updateCountLabel() {
        if (countLabel != null && articleTable != null) {
            int filtered = articleTable.getRowCount();
            int total = 0;
            if (articleTable.getModel() instanceof DefaultTableModel model) {
                total = model.getRowCount();
            }
            countLabel.setText("Artikel: " + filtered + " / " + total);
            JFrameUtils.updateTableEmptyState(
                    tableScrollPane,
                    articleTable,
                    total == 0,
                    "Willkommen im Artikellager",
                    "Noch sind keine Artikel erfasst. Fügen Sie den ersten Artikel hinzu oder importieren Sie Artikeldaten, um die Lagerverwaltung zu starten.");
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
        Article beforeArticle = ArticleManager.getInstance().getArticleByNumber(articleNumber);
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
        } else if (beforeArticle != null && beforeArticle.getStockQuantity() != article.getStockQuantity()) {
            pushUndoAction(new StockChangeUndoAction(
                    article.getArticleNumber(),
                    article.getName(),
                    beforeArticle.getStockQuantity(),
                    article.getStockQuantity()));
        }
    }

    private void showInlineEditError(String message, String articleNumber, int modelRow) {
        LOGGER.warn("Inline edit failed for article '{}': {}", articleNumber, message);
        MessageDialog messageDialog = new MessageDialog(message, "Ungültige Eingabe", 5000);
        messageDialog.display();
        reloadRowFromDb(articleNumber, modelRow);
    }

    private void editArticleAtModelRow(int modelRow) {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        Object[] existingData = model.getDataVector().elementAt(modelRow).toArray();
        Object[] updatedRow = showUpdateArticleDialog(existingData);
        if (updatedRow == null) {
            return;
        }
        if (updatedRow.length != 8) {
            throw new IllegalArgumentException("Invalid number of fields in update article dialog");
        }

        Article updatedArticle = buildArticleFromDialogRow(updatedRow);
        if (updatedArticle == null) {
            LOGGER.warn("Rejected update for article '{}' because the dialog returned invalid data", model.getValueAt(modelRow, 0));
            new MessageDialog("Ungültige Artikeldaten. Bitte Eingaben prüfen.", "Fehler", 5000).display();
            reloadRowFromDb(String.valueOf(model.getValueAt(modelRow, 0)), modelRow);
            return;
        }

        Article beforeArticle = ArticleManager.getInstance().getArticleByNumber(updatedArticle.getArticleNumber());
        if (ArticleManager.getInstance().updateArticle(updatedArticle)) {
            LOGGER.info("Updated article '{}' ({})", updatedArticle.getArticleNumber(), updatedArticle.getName());
            logUtils.addLog(Level.INFO,
                    "Artikel aktualisiert: " + updatedArticle.getArticleNumber() + " - " + updatedArticle.getName());
            applyArticleToTableRow(model, modelRow, updatedArticle);
            applyCombinedFilters();
            if (beforeArticle != null && beforeArticle.getStockQuantity() != updatedArticle.getStockQuantity()) {
                pushUndoAction(new StockChangeUndoAction(
                        updatedArticle.getArticleNumber(),
                        updatedArticle.getName(),
                        beforeArticle.getStockQuantity(),
                        updatedArticle.getStockQuantity()));
            }
            new MessageDialog("Artikel erfolgreich aktualisiert.", "Erfolg", 5000).display();
        } else {
            LOGGER.warn("Failed to update article '{}'", updatedArticle.getArticleNumber());
            new MessageDialog("Fehler beim Aktualisieren des Artikels.", "Fehler", 5000).display();
            reloadRowFromDb(String.valueOf(model.getValueAt(modelRow, 0)), modelRow);
        }
    }

    private Article buildArticleFromDialogRow(Object[] updatedRow) {
        Integer stockQuantity = parseInteger(updatedRow[3]);
        Integer minStock = parseInteger(updatedRow[4]);
        Double sellPrice = parseDouble(updatedRow[5]);
        Double purchasePrice = parseDouble(updatedRow[6]);
        if (stockQuantity == null || minStock == null || sellPrice == null || purchasePrice == null) {
            return null;
        }

        return new Article(
                String.valueOf(updatedRow[0]),
                String.valueOf(updatedRow[1]),
                String.valueOf(updatedRow[2]),
                stockQuantity,
                minStock,
                sellPrice,
                purchasePrice,
                String.valueOf(updatedRow[7]));
    }

    private void applyArticleToTableRow(DefaultTableModel model, int modelRow, Article article) {
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

    private void pushUndoAction(UndoableArticleAction action) {
        if (action == null) {
            return;
        }
        undoStack.push(action);
        redoStack.clear();
        updateUndoRedoState();
    }

    private void undoLastArticleAction() {
        if (undoStack.isEmpty()) {
            return;
        }
        UndoableArticleAction action = undoStack.pop();
        if (action.undo()) {
            redoStack.push(action);
            loadArticles();
            new MessageDialog("Rückgängig gemacht: " + action.getDescription(), "Rückgängig", 5000).display();
        } else {
            new MessageDialog("Die Aktion konnte nicht rückgängig gemacht werden.", "Rückgängig", 5000).display();
        }
        updateUndoRedoState();
    }

    private void redoLastArticleAction() {
        if (redoStack.isEmpty()) {
            return;
        }
        UndoableArticleAction action = redoStack.pop();
        if (action.redo()) {
            undoStack.push(action);
            loadArticles();
            new MessageDialog("Wiederholt: " + action.getDescription(), "Wiederholen", 5000).display();
        } else {
            new MessageDialog("Die Aktion konnte nicht wiederholt werden.", "Wiederholen", 5000).display();
        }
        updateUndoRedoState();
    }

    private void updateUndoRedoState() {
        if (undoButton != null) {
            undoButton.setEnabled(!undoStack.isEmpty());
        }
        if (redoButton != null) {
            redoButton.setEnabled(!redoStack.isEmpty());
        }
    }

    private Article copyArticle(Article article) {
        if (article == null) {
            return null;
        }
        return new Article(
                article.getArticleNumber(),
                article.getName(),
                article.getDetails(),
                article.getStockQuantity(),
                article.getMinStockLevel(),
                article.getSellPrice(),
                article.getPurchasePrice(),
                article.getVendorName());
    }

    private boolean restoreArticleSnapshot(Article snapshot) {
        ArticleManager articleManager = ArticleManager.getInstance();
        if (snapshot == null || snapshot.getArticleNumber() == null) {
            return false;
        }
        Article copy = copyArticle(snapshot);
        if (articleManager.existsArticle(copy.getArticleNumber())) {
            return articleManager.updateArticle(copy, "Undo/Redo");
        }
        return articleManager.insertArticle(copy);
    }

    private interface UndoableArticleAction {
        boolean undo();

        boolean redo();

        String getDescription();
    }

    private class StockChangeUndoAction implements UndoableArticleAction {
        private final String articleNumber;
        private final String articleName;
        private final int oldStock;
        private final int newStock;

        StockChangeUndoAction(String articleNumber, String articleName, int oldStock, int newStock) {
            this.articleNumber = articleNumber;
            this.articleName = articleName == null || articleName.isBlank() ? articleNumber : articleName;
            this.oldStock = oldStock;
            this.newStock = newStock;
        }

        @Override
        public boolean undo() {
            return setStock(oldStock);
        }

        @Override
        public boolean redo() {
            return setStock(newStock);
        }

        @Override
        public String getDescription() {
            return "Bestand " + articleName + " (" + oldStock + " ↔ " + newStock + ")";
        }

        private boolean setStock(int stock) {
            Article article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
            if (article == null) {
                return false;
            }
            Article updated = new Article(
                    article.getArticleNumber(),
                    article.getName(),
                    article.getDetails(),
                    stock,
                    article.getMinStockLevel(),
                    article.getSellPrice(),
                    article.getPurchasePrice(),
                    article.getVendorName());
            return ArticleManager.getInstance().updateArticle(updated, "Undo/Redo");
        }
    }

    private class DeleteArticlesUndoAction implements UndoableArticleAction {
        private final List<Article> articles;

        DeleteArticlesUndoAction(List<Article> articles) {
            this.articles = articles == null ? List.of() : articles.stream()
                    .map(ArticleGUI.this::copyArticle)
                    .filter(Objects::nonNull)
                    .toList();
        }

        @Override
        public boolean undo() {
            boolean success = true;
            for (Article article : articles) {
                success &= restoreArticleSnapshot(article);
            }
            return success && !articles.isEmpty();
        }

        @Override
        public boolean redo() {
            boolean success = true;
            ArticleManager articleManager = ArticleManager.getInstance();
            for (Article article : articles) {
                if (article == null || article.getArticleNumber() == null) {
                    success = false;
                    continue;
                }
                if (articleManager.existsArticle(article.getArticleNumber())) {
                    success &= articleManager.deleteArticleByNumber(article.getArticleNumber());
                }
            }
            return success && !articles.isEmpty();
        }

        @Override
        public String getDescription() {
            return articles.size() == 1
                    ? "Artikel wiederherstellen/löschen"
                    : articles.size() + " Artikel wiederherstellen/löschen";
        }
    }

    private class CompositeArticleUndoAction implements UndoableArticleAction {
        private final String description;
        private final List<UndoableArticleAction> actions;

        CompositeArticleUndoAction(String description, List<? extends UndoableArticleAction> actions) {
            this.description = description == null || description.isBlank() ? "Artikel-Aktion" : description;
            this.actions = actions == null ? List.of() : new ArrayList<>(actions);
        }

        @Override
        public boolean undo() {
            boolean success = true;
            ListIterator<UndoableArticleAction> iterator = actions.listIterator(actions.size());
            while (iterator.hasPrevious()) {
                success &= iterator.previous().undo();
            }
            return success && !actions.isEmpty();
        }

        @Override
        public boolean redo() {
            boolean success = true;
            for (UndoableArticleAction action : actions) {
                success &= action.redo();
            }
            return success && !actions.isEmpty();
        }

        @Override
        public String getDescription() {
            return description;
        }
    }

    private void deleteSelectedArticle() {
        int selectedRow = articleTable.getSelectedRow();
        if (selectedRow == -1) {
            MessageDialog messageDialog = new MessageDialog("Bitte wählen Sie einen Artikel zum Löschen aus.",
                    "Keine Auswahl", 5000);
            messageDialog.display();
            return;
        }

        int confirm = new MessageDialog("Möchten Sie diesen Artikel wirklich löschen?",
                "Artikel löschen")
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .displayWithOptions();
        if (confirm != JOptionPane.YES_OPTION)
            return;

        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        int modelRow = articleTable.convertRowIndexToModel(selectedRow);
        String artikelnummer = (String) model.getValueAt(modelRow, 0);

        // Remove from a database first
        ArticleManager articleManager = ArticleManager.getInstance();
        Article deletedArticle = copyArticle(articleManager.getArticleByNumber(artikelnummer));
        if (articleManager.deleteArticleByNumber(artikelnummer)) {
            // Only remove from the table if DB deletion succeeded
            model.removeRow(modelRow);
            updateCountLabel();
            LOGGER.info("Deleted article '{}'", artikelnummer);
            logUtils.addLog(Level.INFO, "Artikel gelöscht: " + artikelnummer);
            if (deletedArticle != null) {
                pushUndoAction(new DeleteArticlesUndoAction(List.of(deletedArticle)));
            }
            MessageDialog messageDialog = new MessageDialog("Artikel erfolgreich gelöscht.", "Erfolg", 5000);
            messageDialog.display();
        } else {
            LOGGER.warn("Failed to delete article '{}'", artikelnummer);
            MessageDialog messageDialog = new MessageDialog("Fehler beim Löschen des Artikels aus der Datenbank.",
                    "Fehler", 5000);
            messageDialog.display();
        }
    }

    private void deleteSelectedArticles() {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0) {
            MessageDialog messageDialog = new MessageDialog("Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl", 5000);
            messageDialog.display();
            return;
        }

        int confirm = new MessageDialog("Möchten Sie " + selectedRows.length + " Artikel wirklich löschen?",
                "Artikel löschen")
                .setOptionType(JOptionPane.YES_NO_OPTION).displayWithOptions();
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
        int deletedCount = 0;
        List<Article> deletedArticles = new ArrayList<>();
        for (int modelRow : modelRows) {
            String artikelnummer = String.valueOf(model.getValueAt(modelRow, 0));
            Article deletedArticle = copyArticle(articleManager.getArticleByNumber(artikelnummer));
            if (articleManager.deleteArticleByNumber(artikelnummer)) {
                model.removeRow(modelRow);
                deletedCount++;
                if (deletedArticle != null) {
                    deletedArticles.add(deletedArticle);
                }
            } else {
                LOGGER.warn("Failed to delete article '{}' during bulk delete", artikelnummer);
            }
        }
        isUpdatingTable = false;
        updateCountLabel();
        LOGGER.info("Bulk deleted {} of {} selected articles", deletedCount, modelRows.size());
        if (deletedCount > 0) {
            logUtils.addLog(Level.INFO, "Mehrfachlöschung von Artikeln abgeschlossen: " + deletedCount + " gelöscht");
            if (!deletedArticles.isEmpty()) {
                pushUndoAction(new DeleteArticlesUndoAction(deletedArticles));
            }
        }
    }

    private void adjustStockForSelectedArticles() {
        List<Article> selected = getSelectedArticles(articleTable);
        if (selected.isEmpty()) {
            new MessageDialog("Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl", 5000)
                    .display();
            return;
        }

        String input = new MessageDialog()
        .setTitle("Bestand anpassen")
        .setMessage("Bestandsänderung eingeben (z.B. 5 oder -3):")
        .displayWithStringInput();
        if (input == null) {
            return;
        }
        Integer delta = parseInteger(input.trim());
        if (delta == null) {
            new MessageDialog("Bitte eine gueltige Ganzzahl eingeben.",
                    "Ungueltige Eingabe", 5000)
                    .display();
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
            int confirm = new MessageDialog()
            .setTitle("Bestand anpassen")
            .setMessage("Einige Artikel wuerden negativ werden.\nBestand fuer diese Artikel auf 0 setzen?")
            .setOptionType(JOptionPane.YES_NO_OPTION)
            .displayWithOptions();
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }

        ArticleManager articleManager = ArticleManager.getInstance();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        isUpdatingTable = true;
        List<StockChangeUndoAction> stockChanges = new ArrayList<>();
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
                if (article.getStockQuantity() != newStock) {
                    stockChanges.add(new StockChangeUndoAction(
                            article.getArticleNumber(),
                            article.getName(),
                            article.getStockQuantity(),
                            newStock));
                }
            }
        }
        isUpdatingTable = false;
        if (!stockChanges.isEmpty()) {
            pushUndoAction(new CompositeArticleUndoAction(
                    "Bestandsänderung für " + stockChanges.size() + " Artikel",
                    stockChanges));
            updateCountLabel();
        }
    }

    /**
     * Generate QR codes for the selected articles and save them to disk, showing a
     * progress dialog during generation.
     * Uses a SwingWorker to keep the UI responsive and handles errors gracefully.
     */
    private void generateQrCodesForSelectedArticles() {
        List<Article> selectedArticles = getSelectedArticles(articleTable);
        if (selectedArticles.isEmpty()) {
            new MessageDialog("Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl", 5000)
                    .display();
            return;
        }

        File outputDir = new File(Main.getAppDataDir(), "qr_codes");
        int result = new MessageDialog()
                .setTitle("QR-Codes generieren")
                .setMessage("QR-Codes fuer " + selectedArticles.size() + " ausgewählte Artikel generieren?\nSpeicherort: "
                        + outputDir.getAbsolutePath())
                .setOptionType(JOptionPane.OK_CANCEL_OPTION)
                .displayWithOptions();
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
                        new MessageDialog("Es konnten keine QR-Codes erstellt werden.",
                                "QR-Codes generieren", 5000)
                                .display();
                        return;
                    }
                    new MessageDialog("QR-Codes wurden erstellt.\nAnzahl: " + files.size() + "\nOrdner: "
                            + outputDir.getAbsolutePath(),
                            "QR-Codes generieren", 5000)
                            .display();
                } catch (Exception ex) {
                    new MessageDialog("Fehler beim Erstellen der QR-Codes: " + ex.getMessage(),
                            "QR-Codes generieren", 5000)
                            .display();
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
                        new MessageDialog("Artikel konnte nicht gefunden werden.", "Fehler", 5000)
                                .display();
                        return;
                    }

                    String picked = null;

                    List<String> variants = ArticleUtils.getCurrentSeparatedDetails(article);
                    if(!variants.isEmpty()) {
                        String message = "Wählen Sie die Variante für \"" + article.getName() + "\":";
                        String[] options = variants.toArray(new String[0]);
                        int choice = new MessageDialog()
                                .setTitle("Variante auswählen")
                                .setMessage(message)
                                .setOptionType(JOptionPane.DEFAULT_OPTION)
                                .setOptions(options)
                                .displayWithOptions();
                        if (choice < 0 || choice >= options.length) {
                            return; // User cancelled or closed the dialog
                        }
                        picked = options[choice];
                    }

                    String input = new MessageDialog()
                            .setTitle("Zur Kundenbestellung hinzufügen")
                            .setMessage("Geben Sie die Menge für \"" + article.getName() + "\" ein:")
                            .displayWithStringInput();

                    if (input == null)
                        return; // User cancelled

                    try {
                        int quantity = Integer.parseInt(input.trim());
                        if (quantity <= 0) {
                            new MessageDialog("Menge muss größer als 0 sein.", "Ungültige Eingabe", 5000)
                                    .display();
                            return;
                        }

                        if (article.getStockQuantity() < quantity) {
                            int confirm = new MessageDialog()
                            .setTitle("Niedriger Lagerbestand")
                            .setMessage("Der Lagerbestand ist niedriger als die gewünschte Menge.\nMöchten Sie trotzdem fortfahren?")
                            .setOptionType(JOptionPane.YES_NO_OPTION)
                            .displayWithOptions();
                            if (confirm != JOptionPane.YES_OPTION) {
                                return;
                            }
                        }

                        ArticleListGUI.addArticle(article, quantity, picked, null);

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
                        new MessageDialog()
                                .setTitle("Ungültige Eingabe")
                                .setMessage("Bitte geben Sie eine gültige Zahl für die Menge ein.")
                                .display();
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
            editArticleAtModelRow(articleTable.convertRowIndexToModel(sel));
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

    private void showSeparatedArticlesDialog() {
        List<SeperateArticle> separatedArticles = getCurrentSeparatedArticles();
        if (separatedArticles == null || separatedArticles.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine getrennten Artikel")
                    .setMessage("Es sind aktuell keine getrennten Artikel vorhanden.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
            return;
        }

        ThemeManager.applyUIDefaults();

        JDialog dialog = createSeparatedArticlesDialog();
        JPanel root = createSeparatedDialogRoot();
        DefaultTableModel model = createSeparatedArticlesTableModel(separatedArticles);
        JTable table = new JTable(model);
        configureSeparatedArticlesTable(table);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        SeparatedDialogUi ui = createSeparatedDialogUi(table);

        root.add(createSeparatedHeaderCard(separatedArticles), BorderLayout.NORTH);
        root.add(createSeparatedContentSplit(table, ui), BorderLayout.CENTER);
        root.add(createSeparatedFooter(dialog, ui.closeButton()), BorderLayout.SOUTH);

        bindSeparatedDialogInteractions(table, sorter, ui);
        initializeSeparatedDialogSelection(table, ui);
        configureSeparatedDialog(dialog, root, ui.closeButton());
        dialog.setVisible(true);
    }

    private JDialog createSeparatedArticlesDialog() {
        JDialog dialog = new JDialog(this, UnicodeSymbols.CLIPBOARD + " Getrennte Artikel", true);
        dialog.setSize(980, 680);
        dialog.setMinimumSize(new Dimension(840, 560));
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        return dialog;
    }

    private JPanel createSeparatedDialogRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        return root;
    }

    private RoundedPanel createSeparatedHeaderCard(List<SeperateArticle> separatedArticles) {
        RoundedPanel headerCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        headerCard.setLayout(new BorderLayout(16, 8));
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Getrennte Artikel");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel("Varianten sauber durchsuchen, vergleichen und komplett lesen");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        headerCard.add(JFrameUtils.createHeaderTextPanel(titleLabel, subtitleLabel, 4), BorderLayout.CENTER);
        headerCard.add(createSeparatedStatsPanel(separatedArticles), BorderLayout.EAST);
        return headerCard;
    }

    private JPanel createSeparatedStatsPanel(List<SeperateArticle> separatedArticles) {
        Set<String> uniqueArticleNumbers = new LinkedHashSet<>();
        for (SeperateArticle separatedArticle : separatedArticles) {
            if (separatedArticle != null && separatedArticle.getArticleNumber() != null) {
                uniqueArticleNumbers.add(separatedArticle.getArticleNumber());
            }
        }

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        statsPanel.setOpaque(false);
        statsPanel.add(createSeparatedSummaryPill(String.valueOf(separatedArticles.size()), "Varianten"));
        statsPanel.add(createSeparatedSummaryPill(String.valueOf(uniqueArticleNumbers.size()), "Artikel"));
        return statsPanel;
    }

    private SeparatedDialogUi createSeparatedDialogUi(JTable table) {
        JTextField searchField = new JTextField(24);
        styleTextField(searchField);
        searchField.setToolTipText("Sucht nach Artikelnummer, Index oder Zusatzdetail");

        JLabel filteredCountLabel = new JLabel();
        filteredCountLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        filteredCountLabel.setForeground(ThemeManager.getTextSecondaryColor());
        updateSeparatedFilteredCount(filteredCountLabel, table);

        JTextArea detailsArea = new JTextArea();
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        detailsArea.setOpaque(false);
        detailsArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        detailsArea.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        detailsArea.setForeground(ThemeManager.getTextPrimaryColor());
        detailsArea.setText("Wählen Sie links eine Variante aus, um die vollständigen Zusatzdetails zu lesen.");

        JLabel selectedIndexValue = createSeparatedDetailValueLabel("Keine Auswahl");
        JLabel selectedArticleValue = createSeparatedDetailValueLabel("Keine Auswahl");
        JButton closeButton = createSecondaryButton(UnicodeSymbols.CLEAR + " Schließen");

        return new SeparatedDialogUi(searchField, filteredCountLabel, detailsArea, selectedIndexValue, selectedArticleValue, closeButton);
    }

    private JSplitPane createSeparatedContentSplit(JTable table, SeparatedDialogUi ui) {
        JSplitPane contentSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                createSeparatedTableCard(table, ui),
                createSeparatedDetailsCard(ui)
        );
        contentSplit.setResizeWeight(0.68);
        contentSplit.setBorder(BorderFactory.createEmptyBorder());
        contentSplit.setOpaque(false);
        contentSplit.setDividerSize(10);
        contentSplit.setContinuousLayout(true);
        return contentSplit;
    }

    private RoundedPanel createSeparatedTableCard(JTable table, SeparatedDialogUi ui) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());

        RoundedPanel tableCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        tableCard.add(createSeparatedSearchRow(ui.searchField(), ui.filteredCountLabel()), BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        return tableCard;
    }

    private JPanel createSeparatedSearchRow(JTextField searchField, JLabel filteredCountLabel) {
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);

        JPanel searchFieldPanel = new JPanel(new BorderLayout(8, 0));
        searchFieldPanel.setOpaque(false);

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche");
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        searchFieldPanel.add(searchLabel, BorderLayout.WEST);
        searchFieldPanel.add(searchField, BorderLayout.CENTER);

        searchRow.add(searchFieldPanel, BorderLayout.CENTER);
        searchRow.add(filteredCountLabel, BorderLayout.EAST);
        return searchRow;
    }

    private RoundedPanel createSeparatedDetailsCard(SeparatedDialogUi ui) {
        RoundedPanel detailsCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        detailsCard.setLayout(new BorderLayout(0, 12));
        detailsCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JScrollPane detailsScrollPane = new JScrollPane(ui.detailsArea());
        detailsScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        detailsScrollPane.getViewport().setOpaque(false);
        detailsScrollPane.setOpaque(false);

        detailsCard.add(createSeparatedDetailsTop(ui.selectedIndexValue(), ui.selectedArticleValue()), BorderLayout.NORTH);
        detailsCard.add(detailsScrollPane, BorderLayout.CENTER);
        return detailsCard;
    }

    private JPanel createSeparatedDetailsTop(JLabel selectedIndexValue, JLabel selectedArticleValue) {
        JLabel detailsTitle = new JLabel(UnicodeSymbols.INFO + " Variantendetails");
        detailsTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 15));
        detailsTitle.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel detailsSubtitle = new JLabel("Vollständige Zusatzinformation der ausgewählten getrennten Variante");
        detailsSubtitle.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        detailsSubtitle.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel metaPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        metaPanel.setOpaque(false);
        metaPanel.add(createSeparatedInfoBlock("Index", selectedIndexValue));
        metaPanel.add(createSeparatedInfoBlock("Artikelnummer", selectedArticleValue));

        JPanel detailsTop = new JPanel(new BorderLayout(0, 12));
        detailsTop.setOpaque(false);
        detailsTop.add(JFrameUtils.createHeaderTextPanel(detailsTitle, detailsSubtitle, 4), BorderLayout.NORTH);
        detailsTop.add(metaPanel, BorderLayout.CENTER);
        return detailsTop;
    }

    private JPanel createSeparatedFooter(JDialog dialog, JButton closeButton) {
        closeButton.addActionListener(e -> dialog.dispose());
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(closeButton);
        return footer;
    }

    private void bindSeparatedDialogInteractions(JTable table, TableRowSorter<DefaultTableModel> sorter, SeparatedDialogUi ui) {
        ui.searchField().getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySeparatedArticlesFilter(ui.searchField(), sorter, table, ui.filteredCountLabel());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySeparatedArticlesFilter(ui.searchField(), sorter, table, ui.filteredCountLabel());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySeparatedArticlesFilter(ui.searchField(), sorter, table, ui.filteredCountLabel());
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateSeparatedDetailsPanel(table, ui.selectedIndexValue(), ui.selectedArticleValue(), ui.detailsArea());
            }
        });
    }

    private void initializeSeparatedDialogSelection(JTable table, SeparatedDialogUi ui) {
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            updateSeparatedDetailsPanel(table, ui.selectedIndexValue(), ui.selectedArticleValue(), ui.detailsArea());
        }
    }

    private void configureSeparatedDialog(JDialog dialog, JPanel root, JButton closeButton) {
        dialog.setContentPane(root);
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private record SeparatedDialogUi(
            JTextField searchField,
            JLabel filteredCountLabel,
            JTextArea detailsArea,
            JLabel selectedIndexValue,
            JLabel selectedArticleValue,
            JButton closeButton
    ) {
    }

    private DefaultTableModel createSeparatedArticlesTableModel(List<SeperateArticle> separatedArticles) {
        DefaultTableModel model = new DefaultTableModel(new Object[]{"Nr.", "Artikelnummer", "Variante"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };

        for (SeperateArticle separatedArticle : separatedArticles) {
            if (separatedArticle == null) {
                continue;
            }
            model.addRow(new Object[]{
                    separatedArticle.getIndex(),
                    separatedArticle.getArticleNumber(),
                    separatedArticle.getOtherDetails()
            });
        }
        return model;
    }

    private void configureSeparatedArticlesTable(JTable table) {
        table.setRowHeight(42);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 6));
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        table.setGridColor(ThemeManager.getBorderColor());
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());

        JTableHeader header = table.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackground());
        header.setForeground(ThemeManager.getTableHeaderForeground());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(90);
        columnModel.getColumn(0).setMaxWidth(90);
        columnModel.getColumn(1).setPreferredWidth(180);
        columnModel.getColumn(2).setPreferredWidth(520);

        columnModel.getColumn(0).setCellRenderer(createSeparatedIndexRenderer());
        columnModel.getColumn(1).setCellRenderer(createSeparatedArticleRenderer());
        columnModel.getColumn(2).setCellRenderer(createSeparatedDetailsRenderer());
    }

    private DefaultTableCellRenderer createSeparatedIndexRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
                label.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(ThemeManager.getSelectionBackgroundColor());
                    label.setForeground(ThemeManager.getSelectionForegroundColor());
                } else {
                    label.setBackground(row % 2 == 0 ? ThemeManager.getCardBackgroundColor() : getSeparatedSurfaceColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                }
                return label;
            }
        };
    }

    private DefaultTableCellRenderer createSeparatedArticleRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(ThemeManager.getSelectionBackgroundColor());
                    label.setForeground(ThemeManager.getSelectionForegroundColor());
                } else {
                    label.setBackground(row % 2 == 0 ? ThemeManager.getCardBackgroundColor() : getSeparatedSurfaceColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                }
                return label;
            }
        };
    }

    private DefaultTableCellRenderer createSeparatedDetailsRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String details = value == null ? "" : value.toString().trim();
                String preview = details.length() > 90 ? details.substring(0, 87) + "..." : details;
                label.setText("<html><div style='padding:4px 0;'><span style='font-weight:600;'>Variante</span><br>" +
                        escapeSeparatedHtml(preview) + "</div></html>");
                label.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(ThemeManager.getSelectionBackgroundColor());
                    label.setForeground(ThemeManager.getSelectionForegroundColor());
                } else {
                    label.setBackground(row % 2 == 0 ? ThemeManager.getCardBackgroundColor() : getSeparatedSurfaceColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                }
                label.setToolTipText(details.isBlank() ? "Keine Zusatzdetails vorhanden" : details);
                return label;
            }
        };
    }

    private JPanel createSeparatedSummaryPill(String value, String label) {
        RoundedPanel pill = new RoundedPanel(getSeparatedSurfaceColor(), 14);
        pill.setLayout(new BoxLayout(pill, BoxLayout.Y_AXIS));
        pill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        valueLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel labelLabel = new JLabel(label);
        labelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        labelLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 11));
        labelLabel.setForeground(ThemeManager.getTextSecondaryColor());

        pill.add(valueLabel);
        pill.add(Box.createVerticalStrut(2));
        pill.add(labelLabel);
        return pill;
    }

    private JPanel createSeparatedInfoBlock(String title, JLabel valueLabel) {
        RoundedPanel panel = new RoundedPanel(getSeparatedSurfaceColor(), 14);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 11));
        titleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(valueLabel);
        return panel;
    }

    private JLabel createSeparatedDetailValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    private void applySeparatedArticlesFilter(JTextField searchField, TableRowSorter<DefaultTableModel> sorter,
                                              JTable table, JLabel filteredCountLabel) {
        String query = searchField == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(query), 0, 1, 2));
        }
        updateSeparatedFilteredCount(filteredCountLabel, table);
        if (table.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void updateSeparatedFilteredCount(JLabel filteredCountLabel, JTable table) {
        if (filteredCountLabel == null || table == null) {
            return;
        }
        filteredCountLabel.setText(table.getRowCount() + " sichtbar");
    }

    private void updateSeparatedDetailsPanel(JTable table, JLabel selectedIndexValue, JLabel selectedArticleValue,
                                             JTextArea detailsArea) {
        if (table == null || selectedIndexValue == null || selectedArticleValue == null || detailsArea == null) {
            return;
        }
        int selectedRow = table.getSelectedRow();
        if (selectedRow < 0) {
            selectedIndexValue.setText("Keine Auswahl");
            selectedArticleValue.setText("Keine Auswahl");
            detailsArea.setText("Wählen Sie links eine Variante aus, um die vollständigen Zusatzdetails zu lesen.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(selectedRow);
        Object indexValue = table.getModel().getValueAt(modelRow, 0);
        Object articleValue = table.getModel().getValueAt(modelRow, 1);
        Object detailsValue = table.getModel().getValueAt(modelRow, 2);

        selectedIndexValue.setText(String.valueOf(indexValue));
        selectedArticleValue.setText(String.valueOf(articleValue));
        detailsArea.setText(detailsValue == null || detailsValue.toString().isBlank()
                ? "Keine Zusatzdetails vorhanden."
                : detailsValue.toString().trim());
        detailsArea.setCaretPosition(0);
    }

    private String escapeSeparatedHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private Color getSeparatedSurfaceColor() {
        Color card = ThemeManager.getCardBackgroundColor();
        Color background = ThemeManager.getBackgroundColor();
        int red = Math.round(card.getRed() * 0.86f + background.getRed() * 0.14f);
        int green = Math.round(card.getGreen() * 0.86f + background.getGreen() * 0.14f);
        int blue = Math.round(card.getBlue() * 0.86f + background.getBlue() * 0.14f);
        return new Color(red, green, blue);
    }

    private List<SeperateArticle> getCurrentSeparatedArticles() {
        List<SeperateArticle> separatedArticles = new ArrayList<>();
        for (Article article : ArticleManager.getInstance().getAllArticles()) {
            if (article == null || article.getArticleNumber() == null || article.getArticleNumber().isBlank()) {
                continue;
            }
            if (!ArticleUtils.isArticleSeparated(article.getArticleNumber())) {
                continue;
            }
            separatedArticles.addAll(ArticleUtils.newSeperatedArticles(article.getArticleNumber()));
        }
        return separatedArticles;
    }

    private void applyAdvancedFilters() {
        applyCombinedFilters();
    }

    private void applyCombinedFilters() {
        persistTableFilters();
        String selectedCategory = categoryFilter == null ? null : (String) categoryFilter.getSelectedItem();
        String searchQuery = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String vendorQuery = vendorFilterField == null ? "" : vendorFilterField.getText().trim().toLowerCase(Locale.ROOT);
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
        boolean hasFilters = !searchQuery.isEmpty()
                || !"Alle Kategorien".equals(categoryFilterValue)
                || !vendorQuery.isEmpty()
                || stockMin != null
                || stockMax != null
                || priceMin != null
                || priceMax != null;

        if (!hasFilters) {
            sorter.setRowFilter(null);
            updateCountLabel();
            return;
        }

        RowFilter<DefaultTableModel, Integer> rowFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                if (!searchQuery.isEmpty() && !matchesSearchQuery(entry, searchQuery)) {
                    return false;
                }

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

    private void restoreTableFilters() {
        if (searchField == null || categoryFilter == null || vendorFilterField == null
                || stockMinField == null || stockMaxField == null
                || priceMinField == null || priceMaxField == null) {
            return;
        }
        searchField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_SEARCH, ""));
        String category = JFrameUtils.getPersistentUiValue(FILTER_KEY_CATEGORY, "Alle Kategorien");
        if (!JFrameUtils.selectComboBoxItem(categoryFilter, category)) {
            categoryFilter.setSelectedItem("Alle Kategorien");
        }
        vendorFilterField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_VENDOR, ""));
        stockMinField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_STOCK_MIN, ""));
        stockMaxField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_STOCK_MAX, ""));
        priceMinField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_PRICE_MIN, ""));
        priceMaxField.setText(JFrameUtils.getPersistentUiValue(FILTER_KEY_PRICE_MAX, ""));
        applyCombinedFilters();
    }

    private void persistTableFilters() {
        if (searchField == null || categoryFilter == null || vendorFilterField == null
                || stockMinField == null || stockMaxField == null
                || priceMinField == null || priceMaxField == null) {
            return;
        }
        JFrameUtils.setPersistentUiValue(FILTER_KEY_SEARCH, searchField.getText().trim());
        JFrameUtils.setPersistentUiValue(FILTER_KEY_CATEGORY, String.valueOf(categoryFilter.getSelectedItem()));
        JFrameUtils.setPersistentUiValue(FILTER_KEY_VENDOR, vendorFilterField.getText().trim());
        JFrameUtils.setPersistentUiValue(FILTER_KEY_STOCK_MIN, stockMinField.getText().trim());
        JFrameUtils.setPersistentUiValue(FILTER_KEY_STOCK_MAX, stockMaxField.getText().trim());
        JFrameUtils.setPersistentUiValue(FILTER_KEY_PRICE_MIN, priceMinField.getText().trim());
        JFrameUtils.setPersistentUiValue(FILTER_KEY_PRICE_MAX, priceMaxField.getText().trim());
    }

    private boolean matchesSearchQuery(RowFilter.Entry<? extends DefaultTableModel, ? extends Integer> entry,
            String searchQuery) {
        for (int i = 0; i < entry.getValueCount(); i++) {
            String value = entry.getStringValue(i);
            if (value != null && value.toLowerCase(Locale.ROOT).contains(searchQuery)) {
                return true;
            }
        }
        return false;
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
        if (combo == null)
            return;
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
}
