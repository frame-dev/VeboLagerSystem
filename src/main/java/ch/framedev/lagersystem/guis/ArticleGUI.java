package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.WarningManager;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.QRCodeGenerator;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

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
import java.net.URLEncoder;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(this, "Artikel Bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with theme colors
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.getPrimaryColor());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel(UnicodeSymbols.EDIT + "  Artikel Bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Styled close button
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schließen");
        closeBtn.setForeground(ThemeManager.getTextOnPrimaryColor());
        closeBtn.setBackground(ThemeManager.getPrimaryColor());
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(true);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 24));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getErrorColor());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(ThemeManager.getTextOnPrimaryColor());
            }
        });
        closeBtn.addActionListener(e -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        headerPanel.add(closeBtn, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Scrollable content card
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Helper method for creating styled labels with icons
        Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        // Helper method for styling text fields with hover effect
        Consumer<JTextField> styleTextField = field -> {
            field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            field.setBackground(ThemeManager.getInputBackgroundColor());
            field.setForeground(ThemeManager.getTextPrimaryColor());
            field.setCaretColor(ThemeManager.getTextPrimaryColor());
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getInputFocusBorderColor(), 2, true),
                            BorderFactory.createEmptyBorder(9, 11, 9, 11)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1, true),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)
                    ));
                }
            });
        };

        int row = 0;

        // Artikelnummer with icon
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        JLabel nummerLabel = createLabel.apply(UnicodeSymbols.NUMBERS + " Artikelnummer *");
        contentCard.add(nummerLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 16, 6);
        JTextField nummerField = new JTextField(existingData[0] == null ? "" : existingData[0].toString(), 30);
        styleTextField.accept(nummerField);
        contentCard.add(nummerField, gbc);

        row++;
        gbc.insets = new Insets(6, 6, 6, 6);
        // Name with icon
        gbc.gridy = row;
        JLabel nameLabel = createLabel.apply(UnicodeSymbols.PENCIL + " Name *");
        contentCard.add(nameLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 16, 6);
        JTextField nameField = new JTextField(existingData[1] == null ? "" : existingData[1].toString(), 30);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        row++;
        gbc.insets = new Insets(6, 6, 6, 6);
        // Details with icon
        gbc.gridy = row;
        JLabel detailsLabel = createLabel.apply(UnicodeSymbols.CLIPBOARD + " Details");
        contentCard.add(detailsLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 16, 6);
        JTextField detailsField = new JTextField(existingData[3] == null ? "" : existingData[3].toString(), 30);
        styleTextField.accept(detailsField);
        contentCard.add(detailsField, gbc);

        // Parse integer values robustly (account for category at index 2)
        // existingData structure: [0:artikelNr, 1:name, 2:category, 3:details, 4:lager, 5:mindest, 6:verkauf, 7:einkauf, 8:lieferant]
        int existingLager = 0;
        int existingMindest = 0;
        try {
            Object o = existingData[4]; // Lagerbestand at index 4
            if (o instanceof Number) existingLager = ((Number) o).intValue();
            else existingLager = Integer.parseInt(o.toString());
        } catch (Exception ignored) {
        }
        try {
            Object o = existingData[5]; // Mindestbestand at index 5
            if (o instanceof Number) existingMindest = ((Number) o).intValue();
            else existingMindest = Integer.parseInt(o.toString());
        } catch (Exception ignored) {
        }

        row++;
        gbc.insets = new Insets(12, 6, 6, 6);
        // Stock fields with improved layout
        gbc.gridy = row;
        JLabel stockSectionLabel = createLabel.apply(UnicodeSymbols.PACKAGE + " Lagerbestand");
        stockSectionLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(stockSectionLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(8, 6, 16, 6);
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        stockPanel.setOpaque(false);

        JPanel lagerPanel = new JPanel(new BorderLayout(0, 6));
        lagerPanel.setOpaque(false);
        JLabel lagerLabel = new JLabel("Aktuell");
        lagerLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        lagerLabel.setForeground(ThemeManager.getTextSecondaryColor());
        lagerPanel.add(lagerLabel, BorderLayout.NORTH);
        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(existingLager, 0, Integer.MAX_VALUE, 1));
        lagerSpinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) lagerSpinner.getEditor()).getTextField().setColumns(10);
        JTextField lagerField = ((JSpinner.DefaultEditor) lagerSpinner.getEditor()).getTextField();
        lagerField.setBackground(ThemeManager.getInputBackgroundColor());
        lagerField.setForeground(ThemeManager.getTextPrimaryColor());
        lagerField.setCaretColor(ThemeManager.getTextPrimaryColor());
        lagerField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                )
        );
        lagerPanel.add(lagerSpinner, BorderLayout.CENTER);

        JPanel mindestPanel = new JPanel(new BorderLayout(0, 6));
        mindestPanel.setOpaque(false);
        JLabel mindestLabel = new JLabel("Mindestbestand");
        mindestLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        mindestLabel.setForeground(ThemeManager.getTextSecondaryColor());
        mindestPanel.add(mindestLabel, BorderLayout.NORTH);
        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(existingMindest, 0, Integer.MAX_VALUE, 1));
        mindestSpinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) mindestSpinner.getEditor()).getTextField().setColumns(10);
        JTextField mindestField = ((JSpinner.DefaultEditor) mindestSpinner.getEditor()).getTextField();
        mindestField.setBackground(ThemeManager.getInputBackgroundColor());
        mindestField.setForeground(ThemeManager.getTextPrimaryColor());
        mindestField.setCaretColor(ThemeManager.getTextPrimaryColor());
        mindestField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                )
        );
        mindestPanel.add(mindestSpinner, BorderLayout.CENTER);

        stockPanel.add(lagerPanel);
        stockPanel.add(mindestPanel);
        contentCard.add(stockPanel, gbc);

        // Price fields with formatter
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
            Object o = existingData[6]; // Verkaufspreis at index 6
            if (o instanceof Number) existingVerkauf = ((Number) o).doubleValue();
            else existingVerkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {
        }
        try {
            Object o = existingData[7]; // Einkaufspreis at index 7
            if (o instanceof Number) existingEinkauf = ((Number) o).doubleValue();
            else existingEinkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) {
        }

        row++;
        gbc.insets = new Insets(12, 6, 6, 6);
        // Price section
        gbc.gridy = row;
        JLabel priceSectionLabel = createLabel.apply(UnicodeSymbols.MONEY + " Preise");
        priceSectionLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        contentCard.add(priceSectionLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(8, 6, 16, 6);
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        pricePanel.setOpaque(false);

        JPanel verkaufPanel = new JPanel(new BorderLayout(0, 6));
        verkaufPanel.setOpaque(false);
        JLabel verkaufLabel = new JLabel("Verkaufspreis (CHF)");
        verkaufLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        verkaufLabel.setForeground(ThemeManager.getTextSecondaryColor());
        verkaufPanel.add(verkaufLabel, BorderLayout.NORTH);
        JFormattedTextField verkaufField = getVerkaufField(priceFormatter, existingVerkauf);
        verkaufPanel.add(verkaufField, BorderLayout.CENTER);

        JPanel einkaufPanel = new JPanel(new BorderLayout(0, 6));
        einkaufPanel.setOpaque(false);
        JLabel einkaufLabel = new JLabel("Einkaufspreis (CHF)");
        einkaufLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        einkaufLabel.setForeground(ThemeManager.getTextSecondaryColor());
        einkaufPanel.add(einkaufLabel, BorderLayout.NORTH);
        JFormattedTextField einkaufField = getVerkaufField(priceFormatter, existingEinkauf);
        einkaufPanel.add(einkaufField, BorderLayout.CENTER);

        pricePanel.add(verkaufPanel);
        pricePanel.add(einkaufPanel);
        contentCard.add(pricePanel, gbc);

        row++;
        gbc.insets = new Insets(6, 6, 6, 6);
        // Lieferant with icon
        gbc.gridy = row;
        JLabel lieferantLabel = createLabel.apply(UnicodeSymbols.TRUCK + " Lieferant");
        contentCard.add(lieferantLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 6, 6);
        JTextField lieferantField = new JTextField(existingData[8] == null ? "" : existingData[8].toString(), 30);
        styleTextField.accept(lieferantField);
        contentCard.add(lieferantField, gbc);

        // Wrap content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentCard);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        // Button panel with improved styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 16));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        Color cancelBtnColor = ThemeManager.getErrorColor();
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + "  Abbrechen");
        cancelBtn.setToolTipText("Abbrechen und Änderungen verwerfen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(cancelBtnColor);
        cancelBtn.setOpaque(true);
        cancelBtn.setContentAreaFilled(true);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cancelBtnColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(ThemeManager.getButtonHoverColor(cancelBtnColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelBtnColor);
            }
        });

        Color okBtnColor = ThemeManager.getSuccessColor();
        JButton okBtn = new JButton(UnicodeSymbols.CHECK + "  Speichern");
        okBtn.setToolTipText("Änderungen speichern");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okBtnColor);
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorderPainted(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okBtnColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 27, 10, 27)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(ThemeManager.getButtonHoverColor(okBtnColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okBtnColor);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Actions
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
                JOptionPane.showMessageDialog(dialog,
                        "Artikelnummer und Name sind Pflichtfelder.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
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
                JOptionPane.showMessageDialog(dialog,
                        "Bitte gültige Preise eingeben.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                return;
            }

            resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                    verkaufspreis, einkaufspreis, lieferant};
            dialog.dispose();
        });

        // Show dialog
        dialog.setSize(700, 750);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nummerField::requestFocusInWindow);
        dialog.setVisible(true);

        return resultHolder[0];
    }

    private static JFormattedTextField getVerkaufField(NumberFormatter priceFormatter, double existingVerkauf) {
        JFormattedTextField verkaufField = new JFormattedTextField(priceFormatter);
        verkaufField.setColumns(12);
        verkaufField.setValue(existingVerkauf);
        verkaufField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        verkaufField.setBackground(ThemeManager.getInputBackgroundColor());
        verkaufField.setForeground(ThemeManager.getTextPrimaryColor());
        verkaufField.setCaretColor(ThemeManager.getTextPrimaryColor());
        verkaufField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        return verkaufField;
    }

    /**
     * Shows a polished modal dialog to enter a new article. Returns the table row or null if canceled.
     */
    private Object[] showAddArticleDialog() {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = createAddArticleDialog();
        ArticleDialogComponents components = createArticleDialogComponents(dialog, resultHolder);

        showDialog(dialog, components.nummerField());

        return resultHolder[0];
    }

    /**
     * Creates and configures the main dialog
     */
    private JDialog createAddArticleDialog() {
        JDialog dialog = new JDialog(this, "Neuen Artikel hinzufügen", true);
        dialog.setUndecorated(true);
        return dialog;
    }

    /**
     * Creates all dialog components
     */
    private ArticleDialogComponents createArticleDialogComponents(JDialog dialog, Object[][] resultHolder) {
        JPanel mainContainer = createDialogMainContainer();

        // Header
        JPanel headerPanel = createDialogHeader(dialog, resultHolder);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content with form fields
        ArticleFormFields formFields = createArticleFormFields();
        mainContainer.add(formFields.contentCard(), BorderLayout.CENTER);

        // Buttons
        DialogButtons dialogButtons = createDialogButtons(dialog, resultHolder, formFields);
        mainContainer.add(dialogButtons.buttonPanel(), BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(dialogButtons.okButton());

        return new ArticleDialogComponents(
                formFields.nummerField(),
                formFields.nameField(),
                formFields.detailsField(),
                formFields.lagerSpinner(),
                formFields.mindestSpinner(),
                formFields.verkaufField(),
                formFields.einkaufField(),
                formFields.lieferantField()
        );
    }

    /**
     * Creates the main container panel with shadow effect
     */
    private JPanel createDialogMainContainer() {
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // Add subtle shadow border
        Color shadowColor = ThemeManager.withAlpha(Color.BLACK, ThemeManager.isDarkMode() ? 80 : 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadowColor, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        return mainContainer;
    }

    /**
     * Creates the dialog header with title and close button - modern blue gradient design
     */
    private JPanel createDialogHeader(JDialog dialog, Object[][] resultHolder) {
        // Create gradient panel for modern look
        JPanel headerPanel = getHeaderPanel();

        // Icon and title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.HEAVY_PLUS);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel(UnicodeSymbols.CHECKMARK + " Neuen Artikel hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = createDialogCloseButton(dialog, resultHolder);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        return headerPanel;
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

    /**
     * Creates the close button for the dialog header with modern styling
     */
    private JButton createDialogCloseButton(JDialog dialog, Object[][] resultHolder) {
        JButton closeBtn = new JButton("X");
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));

        // Hover effect
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

        closeBtn.addActionListener(e -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        return closeBtn;
    }

    /**
     * Creates all form fields for the article dialog with modern card design
     */
    private ArticleFormFields createArticleFormFields() {
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = createDefaultGridBagConstraints();
        int row = 0;

        // Create form fields
        JTextField nummerField = addLabeledTextField(contentCard, gbc, row, "Artikelnummer *");
        row += 2;

        JTextField nameField = addLabeledTextField(contentCard, gbc, row, "Name *");
        row += 2;

        JTextField detailsField = addLabeledTextField(contentCard, gbc, row, "Details");
        row += 2;

        // Stock spinners
        gbc.gridy = row++;
        JPanel stockPanel = createStockSpinnersPanel();
        contentCard.add(stockPanel, gbc);
        JSpinner lagerSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(0)).getComponent(1);
        JSpinner mindestSpinner = (JSpinner) ((JPanel) stockPanel.getComponent(1)).getComponent(1);

        // Price fields
        gbc.gridy = row++;
        JPanel pricePanel = createPriceFieldsPanel();
        contentCard.add(pricePanel, gbc);
        JFormattedTextField verkaufField = (JFormattedTextField) ((JPanel) pricePanel.getComponent(0)).getComponent(1);
        JFormattedTextField einkaufField = (JFormattedTextField) ((JPanel) pricePanel.getComponent(1)).getComponent(1);

        // Supplier field
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridy = row++;
        contentCard.add(createDialogLabel(UnicodeSymbols.TRUCK + " Lieferant"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 6, 6);
        JTextField lieferantField = createDialogTextField();
        contentCard.add(lieferantField, gbc);

        return new ArticleFormFields(contentCard, nummerField, nameField, detailsField,
                lagerSpinner, mindestSpinner, verkaufField, einkaufField, lieferantField);
    }

    /**
     * Creates default GridBagConstraints for dialog layout
     */
    private GridBagConstraints createDefaultGridBagConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        return gbc;
    }

    /**
     * Adds a labeled text field to the content panel
     */
    private JTextField addLabeledTextField(JPanel panel, GridBagConstraints gbc, int row, String labelText) {
        gbc.gridy = row;
        panel.add(createDialogLabel(labelText), gbc);
        gbc.gridy = row + 1;
        JTextField textField = createDialogTextField();
        panel.add(textField, gbc);
        return textField;
    }

    /**
     * Creates a styled label for dialog forms with blue accent for required fields
     */
    private JLabel createDialogLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

        // Blue accent for required fields (marked with *)
        if (text.contains("*")) {
            Color blueAccent = ThemeManager.getAccentColor();

            // Split text to style the asterisk differently
            String labelText = text.replace("*", "").trim();
            label.setText("<html>" + labelText + " <span style='color: rgb(" +
                    blueAccent.getRed() + "," + blueAccent.getGreen() + "," + blueAccent.getBlue() +
                    ");'>*</span></html>");
        }

        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    /**
     * Creates a styled text field for dialog forms with blue focus effect
     */
    private JTextField createDialogTextField() {
        JTextField field = new JTextField(25);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        // Blue focus effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    /**
     * Creates the panel with stock quantity spinners
     */
    private JPanel createStockSpinnersPanel() {
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        stockPanel.setOpaque(false);

        stockPanel.add(createLabeledSpinnerPanel("Lagerbestand"));
        stockPanel.add(createLabeledSpinnerPanel("Mindestbestand"));

        return stockPanel;
    }

    /**
     * Creates a panel with label and spinner
     */
    private JPanel createLabeledSpinnerPanel(String labelText) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        spinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(8);
        styleDialogSpinner(spinner);
        panel.add(spinner, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Styles a spinner for dialog forms with blue focus effect
     */
    private void styleDialogSpinner(JSpinner spinner) {
        spinner.setBackground(ThemeManager.getInputBackgroundColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField textField = defaultEditor.getTextField();
            textField.setBackground(ThemeManager.getInputBackgroundColor());
            textField.setForeground(ThemeManager.getTextPrimaryColor());
            textField.setCaretColor(ThemeManager.getAccentColor());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(normalBorder, 1, true),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));

            // Blue focus effect
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(focusBorder, 2, true),
                            BorderFactory.createEmptyBorder(5, 7, 5, 7)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(normalBorder, 1, true),
                            BorderFactory.createEmptyBorder(6, 8, 6, 8)
                    ));
                }
            });
        }
    }

    /**
     * Creates the panel with price input fields
     */
    private JPanel createPriceFieldsPanel() {
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        pricePanel.setOpaque(false);

        NumberFormatter priceFormatter = createPriceFormatter();

        pricePanel.add(createLabeledPriceField("Verkaufspreis (CHF)", priceFormatter));
        pricePanel.add(createLabeledPriceField("Einkaufspreis (CHF)", priceFormatter));

        return pricePanel;
    }

    /**
     * Creates a number formatter for price fields
     */
    private NumberFormatter createPriceFormatter() {
        NumberFormat priceFormat = NumberFormat.getNumberInstance();
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);

        NumberFormatter priceFormatter = new NumberFormatter(priceFormat);
        priceFormatter.setValueClass(Double.class);
        priceFormatter.setAllowsInvalid(false);
        priceFormatter.setMinimum(0.0);

        return priceFormatter;
    }

    /**
     * Creates a panel with label and price field with blue focus effect
     */
    private JPanel createLabeledPriceField(String labelText, NumberFormatter formatter) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(createDialogLabel(labelText), BorderLayout.NORTH);

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.getAccentColor();

        JFormattedTextField field = new JFormattedTextField(formatter);
        field.setColumns(10);
        field.setValue(0.0);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        // Blue focus effect
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        panel.add(field, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates the button panel for the dialog
     */
    private DialogButtons createDialogButtons(JDialog dialog, Object[][] resultHolder, ArticleFormFields formFields) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());

        JButton cancelBtn = createDialogCancelButton(dialog, resultHolder);
        JButton okBtn = createDialogOkButton(dialog, resultHolder, formFields);

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        return new DialogButtons(buttonPanel, okBtn);
    }

    /**
     * Creates the cancel button with modern styling and hover effects
     */
    private JButton createDialogCancelButton(JDialog dialog, Object[][] resultHolder) {
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + " Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(ThemeManager.getTextPrimaryColor());

        Color baseColor = ThemeManager.getSurfaceColor();
        Color hoverColor = ThemeManager.getButtonHoverColor(baseColor);

        cancelBtn.setBackground(baseColor);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(baseColor);
            }
        });

        cancelBtn.addActionListener(ae -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        return cancelBtn;
    }

    /**
     * Creates the OK button with modern blue styling and smooth hover effects
     */
    private JButton createDialogOkButton(JDialog dialog, Object[][] resultHolder, ArticleFormFields formFields) {
        JButton okBtn = new JButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);

        // Blue gradient colors
        Color baseColor = ThemeManager.getButtonBackgroundColor();
        Color hoverColor = ThemeManager.getButtonHoverColor(baseColor);
        Color pressedColor = ThemeManager.getButtonPressedColor(baseColor);

        okBtn.setBackground(baseColor);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Smooth hover effects
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(hoverColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(baseColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(baseColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                okBtn.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                okBtn.setBackground(okBtn.contains(e.getPoint()) ? hoverColor : baseColor);
            }
        });

        okBtn.addActionListener(ae -> handleAddArticleConfirm(dialog, resultHolder, formFields));
        return okBtn;
    }

    /**
     * Handles the confirmation of adding a new article
     */
    private void handleAddArticleConfirm(JDialog dialog, Object[][] resultHolder, ArticleFormFields fields) {
        String nummer = fields.nummerField().getText().trim();
        String name = fields.nameField().getText().trim();
        String details = fields.detailsField().getText().trim();
        String lieferant = fields.lieferantField().getText().trim();

        if (nummer.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(dialog,
                    "Artikelnummer und Name sind Pflichtfelder.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        int lagerbestand = ((Number) fields.lagerSpinner().getValue()).intValue();
        int mindestbestand = ((Number) fields.mindestSpinner().getValue()).intValue();

        double verkaufspreis;
        double einkaufspreis;
        try {
            fields.verkaufField().commitEdit();
            fields.einkaufField().commitEdit();
            verkaufspreis = ((Number) fields.verkaufField().getValue()).doubleValue();
            einkaufspreis = ((Number) fields.einkaufField().getValue()).doubleValue();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialog,
                    "Bitte gültige Preise eingeben.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                verkaufspreis, einkaufspreis, lieferant};
        dialog.dispose();
    }


    /**
     * Shows the dialog and sets initial focus
     */
    private void showDialog(JDialog dialog, JTextField initialFocusField) {
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(initialFocusField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    /**
     * Record to hold dialog component references
     */
    private record ArticleDialogComponents(
            JTextField nummerField,
            JTextField nameField,
            JTextField detailsField,
            JSpinner lagerSpinner,
            JSpinner mindestSpinner,
            JFormattedTextField verkaufField,
            JFormattedTextField einkaufField,
            JTextField lieferantField
    ) {
    }

    /**
     * Record to hold form fields during creation
     */
    private record ArticleFormFields(
            JPanel contentCard,
            JTextField nummerField,
            JTextField nameField,
            JTextField detailsField,
            JSpinner lagerSpinner,
            JSpinner mindestSpinner,
            JFormattedTextField verkaufField,
            JFormattedTextField einkaufField,
            JTextField lieferantField
    ) {
    }

    /**
     * Record to hold dialog buttons
     */
    private record DialogButtons(
            JPanel buttonPanel,
            JButton okButton
    ) {
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
        // File chooser for save location
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
        }

        JTable table = articleTable;
        try (PDDocument doc = new PDDocument()) {
            // Load fonts
            PDFont boldFont;
            PDFont regularFont;
            File arialBold = new File("/Library/Fonts/Arial Bold.ttf");
            File arial = new File("/Library/Fonts/Arial.ttf");

            if (arialBold.exists() && arial.exists()) {
                boldFont = PDType0Font.load(doc, arialBold);
                regularFont = PDType0Font.load(doc, arial);
            } else {
                // Fallback to Type1 fonts
                try {
                    Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                    Object helvBold = c.getField("HELVETICA_BOLD").get(null);
                    Object helv = c.getField("HELVETICA").get(null);
                    boldFont = (PDFont) helvBold;
                    regularFont = (PDFont) helv;
                } catch (Exception e) {
                    throw new IOException("Keine verwendbaren Schriftarten gefunden");
                }
            }

            final boolean useWinAnsiFallback =
                    boldFont.getClass().getSimpleName().contains("PDType1Font") ||
                    regularFont.getClass().getSimpleName().contains("PDType1Font");

            // Page setup - Use A4 landscape for all columns to fit
            PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()); // Landscape
            float margin = 30;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float tableWidth = pageWidth - 2 * margin;

            // Table configuration
            int numCols = table.getColumnCount();
            int numRows = table.getRowCount();
            float rowHeight = 16f;
            float headerHeight = 20f;
            float fontSize = 7f;
            float headerFontSize = 8f;
            float cellPadding = 3f;

            // Calculate column widths proportionally
            float[] columnWidths = new float[numCols];
            float totalWidth = 0;
            for (int i = 0; i < numCols; i++) {
                columnWidths[i] = i < baseColumnWidths.length ? baseColumnWidths[i] : 100;
                totalWidth += columnWidths[i];
            }
            for (int i = 0; i < numCols; i++) {
                columnWidths[i] = (columnWidths[i] / totalWidth) * tableWidth;
            }

            // Create first page
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            float yPosition = pageHeight - margin;
            int currentPage = 1;
            boolean alternate = false;

            // Helper method to draw header
            java.util.function.BiConsumer<PDPageContentStream, Float> drawHeader = (cs, yPos) -> {
                try {
                    // Document header
                    cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
                    cs.addRect(margin, yPos - 45, tableWidth, 45);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.setFont(boldFont, 16);
                    cs.newLineAtOffset(margin + 10, yPos - 28);
                    cs.showText("VEBO Lagersystem - Artikelliste");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(regularFont, 8);
                    cs.newLineAtOffset(pageWidth - margin - 120, yPos - 28);
                    cs.showText("Export: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
                    cs.endText();

                    // Table header
                    float tableHeaderY = yPos - 55;
                    cs.setNonStrokingColor(62f / 255f, 84f / 255f, 98f / 255f);
                    cs.addRect(margin, tableHeaderY - headerHeight, tableWidth, headerHeight);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.setFont(boldFont, headerFontSize);

                    float xPos = margin;
                    for (int col = 0; col < numCols; col++) {
                        String header = table.getColumnName(col);
                        if (useWinAnsiFallback) {
                            header = sanitizeForWinAnsi(header);
                        }
                        // Truncate header if too long
                        float textWidth = boldFont.getStringWidth(header) / 1000f * headerFontSize;
                        if (textWidth > columnWidths[col] - 2 * cellPadding) {
                            while (textWidth > columnWidths[col] - 2 * cellPadding && header.length() > 2) {
                                header = header.substring(0, header.length() - 1);
                                textWidth = boldFont.getStringWidth(header + "..") / 1000f * headerFontSize;
                            }
                            header = header + "..";
                        }

                        cs.newLineAtOffset(xPos + cellPadding, tableHeaderY - headerHeight + cellPadding + 3);
                        cs.showText(header);
                        cs.newLineAtOffset(-(xPos + cellPadding), -(tableHeaderY - headerHeight + cellPadding + 3));
                        xPos += columnWidths[col];
                    }
                    cs.endText();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            // Helper method to draw footer
            java.util.function.BiConsumer<PDPageContentStream, Integer> drawFooter = (cs, pageNum) -> {
                try {
                    cs.beginText();
                    cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                    cs.setFont(regularFont, 7);
                    cs.newLineAtOffset(margin, 15);
                    cs.showText("VEBO Lagersystem © 2026 | " + numRows + " Artikel");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(pageWidth - margin - 50, 15);
                    cs.showText("Seite " + pageNum);
                    cs.endText();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            // Draw header on first page
            drawHeader.accept(contentStream, yPosition);
            yPosition -= (55 + headerHeight + 5);

            // Draw rows
            for (int row = 0; row < numRows; row++) {
                // Check if we need a new page
                if (yPosition - rowHeight < margin + 30) {
                    // Draw footer on current page
                    drawFooter.accept(contentStream, currentPage);
                    contentStream.close();

                    // Create new page
                    currentPage++;
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    contentStream = new PDPageContentStream(doc, page);
                    yPosition = pageHeight - margin;

                    // Draw header on new page
                    drawHeader.accept(contentStream, yPosition);
                    yPosition -= (55 + headerHeight + 5);
                }

                // Alternate row background
                if (alternate) {
                    contentStream.setNonStrokingColor(247f / 255f, 250f / 255f, 253f / 255f);
                    contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
                    contentStream.fill();
                }

                // Draw horizontal line
                contentStream.setStrokingColor(220f / 255f, 225f / 255f, 230f / 255f);
                contentStream.setLineWidth(0.3f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(margin + tableWidth, yPosition);
                contentStream.stroke();

                // Draw cell text
                contentStream.beginText();
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                contentStream.setFont(regularFont, fontSize);

                float xPos = margin;
                for (int col = 0; col < numCols; col++) {
                    Object value = table.getValueAt(row, col);
                    String text = "";

                    if (value != null) {
                        // Format numbers
                        if (value instanceof Double) {
                            text = String.format("%.2f", (Double) value);
                        } else if (value instanceof Integer) {
                            text = String.format("%d", (Integer) value);
                        } else {
                            text = value.toString();
                        }
                    }

                    if (useWinAnsiFallback) {
                        text = sanitizeForWinAnsi(text);
                    }

                    // Truncate if too long
                    float textWidth = regularFont.getStringWidth(text) / 1000f * fontSize;
                    if (textWidth > columnWidths[col] - 2 * cellPadding) {
                        while (textWidth > columnWidths[col] - 2 * cellPadding && text.length() > 2) {
                            text = text.substring(0, text.length() - 1);
                            textWidth = regularFont.getStringWidth(text + "..") / 1000f * fontSize;
                        }
                        text = text + "..";
                    }

                    contentStream.newLineAtOffset(xPos + cellPadding, yPosition - rowHeight + cellPadding + 2);
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(-(xPos + cellPadding), -(yPosition - rowHeight + cellPadding + 2));
                    xPos += columnWidths[col];
                }
                contentStream.endText();

                yPosition -= rowHeight;
                alternate = !alternate;
            }

            // Draw bottom border
            contentStream.setStrokingColor(220f / 255f, 225f / 255f, 230f / 255f);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(margin + tableWidth, yPosition);
            contentStream.stroke();

            // Draw footer on last page
            drawFooter.accept(contentStream, currentPage);

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(this,
                    UnicodeSymbols.CHECKMARK + " PDF erfolgreich exportiert!\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Artikel: " + numRows + "\n" +
                            "Seiten: " + currentPage,
                    "Export erfolgreich",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    UnicodeSymbols.ERROR + " Fehler beim PDF-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            ex.printStackTrace();
        }
    }

    private static String sanitizeForWinAnsi(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp <= 255) {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
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
        List<Article> selectedArticles = getSelectedArticles();
        if (selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JDialog dialog = new JDialog(this, "QR-Code Vorschau", true);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel infoLabel = new JLabel("Vorschau für " + selectedArticles.size() + " Artikel wird geladen...");
        topPanel.add(infoLabel, BorderLayout.WEST);

        JButton exportPdfButton = new JButton("Als PDF exportieren");
        exportPdfButton.setEnabled(false);
        topPanel.add(exportPdfButton, BorderLayout.EAST);

        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gridPanel.setBackground(ThemeManager.getBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        List<QrPreviewItem> previewItems = new ArrayList<>();

        SwingWorker<List<QrPreviewItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<QrPreviewItem> doInBackground() {
                List<QrPreviewItem> items = new ArrayList<>();
                for (Article article : selectedArticles) {
                    try {
                        String url = buildQrCodeUrl(article);
                        BufferedImage image = QRCodeGenerator.generateQRCodeBufferedImage(url, 220, 220);
                        items.add(new QrPreviewItem(article, image));
                    } catch (Exception ex) {
                        // skip broken entries but continue the batch
                    }
                }
                return items;
            }

            @Override
            protected void done() {
                try {
                    List<QrPreviewItem> items = get();
                    previewItems.addAll(items);
                    gridPanel.removeAll();

                    for (QrPreviewItem item : items) {
                        JPanel cell = new JPanel(new BorderLayout());
                        cell.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 232), 1));
                        cell.setBackground(ThemeManager.getCardBackgroundColor());

                        Image scaled = item.image().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
                        cell.add(imageLabel, BorderLayout.CENTER);

                        String labelText = item.article().getArticleNumber() + " - " + item.article().getName();
                        JLabel textLabel = new JLabel(labelText);
                        textLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
                        textLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
                        cell.add(textLabel, BorderLayout.SOUTH);

                        gridPanel.add(cell);
                    }

                    infoLabel.setText("Vorschau geladen: " + items.size() + " QR-Codes");
                    exportPdfButton.setEnabled(!items.isEmpty());
                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (Exception ex) {
                    infoLabel.setText("Fehler beim Laden der Vorschau: " + ex.getMessage());
                }
            }
        };

        exportPdfButton.addActionListener(e -> exportQrCodesToPdf(previewItems));

        worker.execute();

        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private record QrPreviewItem(Article article, BufferedImage image) {
    }

    private String buildQrCodeUrl(Article article) {
        String data = article.getQrCodeData();
        String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
        String serverUrl = "https://framedev.ch/vebo/scan.php";
        return serverUrl + "?data=" + encodedData;
    }

    private void exportQrCodesToPdf(List<QrPreviewItem> items) {
        if (items == null || items.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Keine QR-Codes zum Export vorhanden.",
                    "QR-Codes exportieren",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("QR_Codes_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

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
            float margin = 30f;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            int columns = 3;
            float cellWidth = (pageWidth - (2 * margin)) / columns;
            float cellHeight = cellWidth + 40;
            float imageSize = cellWidth - 20;
            float fontSize = 9f;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            float startY = pageHeight - margin;
            int col = 0;
            int row = 0;

            for (QrPreviewItem item : items) {
                float x = margin + (col * cellWidth);
                float yTop = startY - (row * cellHeight);

                if (yTop - cellHeight < margin) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    contentStream = new PDPageContentStream(doc, page);
                    col = 0;
                    row = 0;
                    x = margin;
                    yTop = startY;
                }

                PDImageXObject image = LosslessFactory.createFromImage(doc, item.image());
                float imageX = x + (cellWidth - imageSize) / 2f;
                float imageY = yTop - imageSize;
                contentStream.drawImage(image, imageX, imageY, imageSize, imageSize);

                String labelText = item.article().getArticleNumber() + " - " + item.article().getName();
                labelText = trimTextToWidth(labelText, regularFont, fontSize, cellWidth - 12);
                contentStream.beginText();
                contentStream.setFont(regularFont, fontSize);
                contentStream.newLineAtOffset(x + 6, imageY - 14);
                contentStream.showText(labelText);
                contentStream.endText();

                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(this,
                    "PDF erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "QR-Codes exportieren",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim PDF-Export: " + ex.getMessage(),
                    "QR-Codes exportieren",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    private static String trimTextToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String trimmed = text;
        float textWidth = font.getStringWidth(trimmed) / 1000f * fontSize;
        if (textWidth <= maxWidth) {
            return trimmed;
        }
        while (trimmed.length() > 2 && textWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            textWidth = font.getStringWidth(trimmed + "..") / 1000f * fontSize;
        }
        return trimmed + "..";
    }

    /**
     * Displays a warning in a modern, styled dialog
     *
     * @param warning The warning object to display
     */
    public void displayWarning(Warning warning) {
        SwingUtilities.invokeLater(() -> {
            // Create custom dialog
            JDialog dialog = new JDialog(this, "Warnung", true);
            dialog.setUndecorated(true);
            dialog.requestFocus();
            dialog.setAutoRequestFocus(true);
            dialog.setLayout(new BorderLayout());

            // Main panel with rounded corners
            RoundedPanel mainPanel = new RoundedPanel(ThemeManager.getBackgroundColor(), 20);
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
            dialog.setLocationRelativeTo(this);

            // Mark as displayed
            warning.setDisplayed(true);

            dialog.requestFocus();
            dialog.setVisible(true);
        });
    }

    /**
     * Helper method to style buttons consistently
     */
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
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
        WarningManager warningManager = WarningManager.getInstance();
        List<Warning> warnings = warningManager.getAllWarnings();

        // Create modern dialog with theme support
        JDialog dialog = new JDialog(this, UnicodeSymbols.WARNING + " Alle Warnungen", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
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
            DefaultTableModel tableModel = getTableModel();

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
                displayWarning(selectedWarning);
            });
            warningsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        int selectedRow = warningsTable.getSelectedRow();
                        if (selectedRow != -1) {
                            Warning selectedWarning = warnings.get(selectedRow);
                            displayWarning(selectedWarning);
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
                showAllWarnings();
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

    private static DefaultTableModel getTableModel() {
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
        return tableModel;
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
        // Create modern dialog with improved styling
        JDialog dialog = new JDialog(this, "QR-Code Daten vom Server", true);
        dialog.setSize(1150, 750);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setMinimumSize(new Dimension(900, 600));

        // Header panel with gradient-like appearance
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

        // Main content area with improved styling
        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(new Color(248, 249, 250));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info panel with better visual feedback
        RoundedPanel infoPanel = new RoundedPanel(new Color(240, 248, 255), 8);
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

        // Table for displaying QR data with enhanced styling
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

        // Alternating row colors
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

        // Column widths
        qrTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        qrTable.getColumnModel().getColumn(1).setPreferredWidth(280);
        qrTable.getColumnModel().getColumn(2).setPreferredWidth(90);
        qrTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        qrTable.getColumnModel().getColumn(4).setPreferredWidth(140);
        qrTable.getColumnModel().getColumn(5).setPreferredWidth(220);

        // Header styling
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

        // Bottom action panel with improved layout
        JPanel actionPanel = new JPanel(new BorderLayout(10, 0));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        // Status area with icon
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

        // Button panel with better spacing
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

        // Load data in background with improved feedback
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

                    // Populate table
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

                } catch (Exception e) {
                    infoIcon.setText(UnicodeSymbols.CLOSE);
                    infoLabel.setText("Fehler beim Laden: " + e.getMessage());
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

        // Enable/disable buttons based on selection with improved logic
        qrTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;

            int selectedCount = qrTable.getSelectedRowCount();
            boolean hasSelection = selectedCount > 0;
            boolean ownUse = false;
            boolean hasSellType = false;
            boolean hasBuyType = false;
            boolean hasOrderType = false;

            // Check types for all selected rows
            if (selectedCount == 1) {
                int selectedRow = qrTable.getSelectedRow();
                String ownUseValue = qrTable.getValueAt(selectedRow, 4).toString();
                ownUse = ownUseValue.equalsIgnoreCase("Ja") || ownUseValue.equalsIgnoreCase("true");
                String type = qrTable.getValueAt(selectedRow, 3).toString();
                hasSellType = type.equalsIgnoreCase("sell");
                hasBuyType = type.equalsIgnoreCase("buy");
                hasOrderType = type.equalsIgnoreCase("order");
            } else if (selectedCount > 1) {
                // Check if all selected rows are same type
                int[] selectedRows = qrTable.getSelectedRows();
                boolean allSell = true;
                boolean allBuy = true;
                for (int row : selectedRows) {
                    String type = qrTable.getValueAt(row, 3).toString();
                    if (!type.equalsIgnoreCase("sell")) allSell = false;
                    if (!type.equalsIgnoreCase("buy")) allBuy = false;
                }
                hasSellType = allSell;
                hasBuyType = allBuy;
            }

            if (selectedCount == 1 && ownUse) {
                // Eigenverbrauch mode (only for single selection)
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
                // Normal import mode (can be multiple)
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
                // Sell mode (can be multiple)
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
                // Order mode (can be multiple)
                removeFromInventoryBtn.setEnabled(false);
                removeFromInventoryBtn.setVisible(false);
                addToOrderBtn.setVisible(true);
                addToOrderBtn.setEnabled(true);
                addToArticleListBtn.setVisible(false);
                importBtn.setVisible(false);
                importAllBtn.setVisible(false);
                statusLabel.setText(selectedCount + " Bestell-Datensatz " + selectedCount + " ausgewählt");
            } else {
                // No selection or mixed types
                importBtn.setEnabled(false);
                removeFromInventoryBtn.setEnabled(false);
                importAllBtn.setEnabled(tableModel.getRowCount() > 0);
                addToArticleListBtn.setVisible(true);
                addToOrderBtn.setEnabled(false);
                statusLabel.setText(hasSelection ? "Gemischte Auswahl" : "Bereit zum Importieren");
            }
        });

        // Action listeners with improved feedback
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

            // Create new worker for refresh
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
                            // Already imported - skip
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

        // Start loading data
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
        JDialog dialog = new JDialog(this, UnicodeSymbols.PACKAGE + " Lager Details & Statistiken", true);
        dialog.setUndecorated(true);
        dialog.setSize(900, 750);
        dialog.setLocationRelativeTo(this);

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient
        JPanel headerPanel = getStatsHeaderPanel(dialog);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Scrollable content panel with statistics
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Calculate all statistics
        int totalArticles = articleTable.getRowCount();
        int totalQuantity = calculateTotalQuantity();
        double stockValue = calculateValueInStock();
        double potentialRevenue = calculatePotentialRevenue();
        double profitMargin = calculateProfitMargin();
        int lowStockCount = countLowStockArticles();
        int outOfStockCount = countOutOfStockArticles();
        int categoriesCount = countCategories();
        int vendorsCount = countVendors();
        double avgArticleValue = calculateAverageArticleValue();
        String mostValuable = getMostValuableArticle();
        double stockHealth = calculateStockHealthPercentage();

        // Create two-column layout for better organization
        int row = 0;

        // === SECTION 1: INVENTORY OVERVIEW ===
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        JLabel overviewLabel = new JLabel(UnicodeSymbols.CLIPBOARD + " Bestandsübersicht");
        overviewLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        overviewLabel.setForeground(ThemeManager.getTextPrimaryColor());
        overviewLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(overviewLabel, gbc);

        gbc.gridwidth = 1;

        // Total articles
        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.PACKAGE + " Gesamtanzahl Artikel",
                String.valueOf(totalArticles),
                new Color(52, 152, 219)
        ), gbc);

        // Total quantity
        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.CHART + " Gesamtmenge (Stück)",
                String.format("%,d", totalQuantity),
                new Color(52, 152, 219)
        ), gbc);

        row++;

        // Categories
        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.FOLDER + " Kategorien",
                String.valueOf(categoriesCount),
                new Color(155, 89, 182)
        ), gbc);

        // Vendors
        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.TRUCK + " Lieferanten",
                String.valueOf(vendorsCount),
                new Color(155, 89, 182)
        ), gbc);

        row++;

        // === SECTION 2: FINANCIAL OVERVIEW ===
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 10, 8);
        JLabel financialLabel = new JLabel(UnicodeSymbols.MONEY + " Finanzübersicht");
        financialLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        financialLabel.setForeground(ThemeManager.getTextPrimaryColor());
        financialLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(financialLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Stock value
        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.MONEY + " Lagerwert (Einkauf)",
                String.format("%.2f CHF", stockValue),
                new Color(46, 204, 113)
        ), gbc);

        // Potential revenue
        gbc.gridx = 1;
        contentCard.add(createStatPanel(
                UnicodeSymbols.MIN_STOCK + " Potenzielle Einnahmen",
                String.format("%.2f CHF", potentialRevenue),
                new Color(52, 152, 219)
        ), gbc);

        row++;

        // Profit margin
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        contentCard.add(createStatPanel(
                UnicodeSymbols.CHEVRON_UP + " Gewinnspanne",
                String.format("%.2f CHF", profitMargin),
                new Color(241, 196, 15)
        ), gbc);

        gbc.gridwidth = 1;

        // Average value per article
        gbc.gridx = 0;
        gbc.gridy = row;
        contentCard.add(createStatPanel(
                UnicodeSymbols.BULLET + " Ø Wert pro Artikel",
                String.format("%.2f CHF", avgArticleValue),
                new Color(155, 89, 182)
        ), gbc);

        // Most valuable article (display truncated if too long)
        gbc.gridx = 1;
        String displayValue = mostValuable.length() > 20 ? mostValuable.substring(0, 17) + "..." : mostValuable;
        contentCard.add(createStatPanel(
                UnicodeSymbols.STAR_FILLED + " Wertvollster Artikel",
                displayValue,
                new Color(241, 196, 15)
        ), gbc);

        row++;

        // === SECTION 3: STOCK HEALTH ===
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 8, 10, 8);
        JLabel healthLabel = new JLabel(UnicodeSymbols.HEALTH + " Lagerstatus");
        healthLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        healthLabel.setForeground(ThemeManager.getTextPrimaryColor());
        healthLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        contentCard.add(healthLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Low stock count
        gbc.gridx = 0;
        gbc.gridy = row;
        Color lowStockColor = lowStockCount > 0 ? new Color(231, 76, 60) : new Color(46, 204, 113);
        contentCard.add(createStatPanel(
                UnicodeSymbols.WARNING + " Niedriger Bestand",
                String.valueOf(lowStockCount),
                lowStockColor
        ), gbc);

        // Out of stock count
        gbc.gridx = 1;
        Color outOfStockColor = outOfStockCount > 0 ? new Color(192, 57, 43) : new Color(46, 204, 113);
        contentCard.add(createStatPanel(
                UnicodeSymbols.WARNING + " Nicht vorrätig",
                String.valueOf(outOfStockCount),
                outOfStockColor
        ), gbc);

        row++;

        // Stock health percentage
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        Color healthColor = stockHealth >= 80 ? new Color(46, 204, 113) :
                stockHealth >= 50 ? new Color(241, 196, 15) :
                        new Color(231, 76, 60);
        contentCard.add(createStatPanel(
                UnicodeSymbols.HEALTH + " Lagergesundheit",
                String.format("%.1f%%", stockHealth),
                healthColor
        ), gbc);

        // Wrap content in scroll pane
        JScrollPane scrollPane = new JScrollPane(contentCard);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 16));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        Color okBtnColor = ThemeManager.getAccentColor();
        JButton okBtn = new JButton(UnicodeSymbols.CHECK + " Schließen");
        okBtn.setToolTipText("Schliesst dieses Fenster");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okBtnColor);
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okBtnColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(ThemeManager.getButtonHoverColor(okBtnColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okBtnColor);
            }
        });
        okBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);
        dialog.setVisible(true);
    }

    /**
     * Create header panel for statistics dialog with gradient background
     */
    private static JPanel getStatsHeaderPanel(JDialog dialog) {
        JPanel headerPanel = getPanel();

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.CHART);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Lager Statistiken");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Schliesst dieses Fenster");
        closeBtn.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 200));
        closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(ThemeManager.withAlpha(ThemeManager.getErrorColor(), 100));
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
        headerPanel.add(closeBtn, BorderLayout.EAST);
        return headerPanel;
    }

    private static JPanel getPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.isDarkMode()
                        ? new Color(30, 58, 95)
                        : new Color(41, 128, 185);
                Color color2 = ThemeManager.isDarkMode()
                        ? new Color(44, 62, 80)
                        : new Color(52, 152, 219);

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }

    /**
     * Create a styled statistic panel with icon, label, and value
     */
    private JPanel createStatPanel(String label, String value, Color accentColor) {
        RoundedPanel panel = new RoundedPanel(ThemeManager.getSurfaceColor(), 12);
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accentColor.brighter(), 2),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)
        ));

        // Left side with label
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        labelComp.setForeground(ThemeManager.getTextPrimaryColor());
        panel.add(labelComp, BorderLayout.WEST);

        // Right side with value
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        valueComp.setForeground(accentColor);
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(valueComp, BorderLayout.EAST);

        return panel;
    }

    /**
     * Calculate total value of stock based on purchase prices
     */
    private double calculateValueInStock() {
        double totalValue = 0.0;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4); // Lagerbestand column
            double einkaufspreis = (double) model.getValueAt(i, 7); // Einkaufspreis column
            totalValue += lagerbestand * einkaufspreis;
        }
        return totalValue;
    }

    /**
     * Calculate total potential revenue based on selling prices
     */
    private double calculatePotentialRevenue() {
        double totalRevenue = 0.0;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4); // Lagerbestand column
            double verkaufspreis = (double) model.getValueAt(i, 6); // Verkaufspreis column
            totalRevenue += lagerbestand * verkaufspreis;
        }
        return totalRevenue;
    }

    /**
     * Calculate total profit margin (potential revenue - stock value)
     */
    private double calculateProfitMargin() {
        return calculatePotentialRevenue() - calculateValueInStock();
    }

    /**
     * Count articles below minimum stock level
     */
    private int countLowStockArticles() {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            int mindestbestand = (int) model.getValueAt(i, 5);
            if (lagerbestand <= mindestbestand && mindestbestand > 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count articles with zero stock
     */
    private int countOutOfStockArticles() {
        int count = 0;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            if (lagerbestand == 0) {
                count++;
            }
        }
        return count;
    }

    /**
     * Calculate total quantity of all articles in stock
     */
    private int calculateTotalQuantity() {
        int total = 0;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            total += lagerbestand;
        }
        return total;
    }

    /**
     * Get count of unique categories in current view
     */
    private int countCategories() {
        Set<String> categories = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String category = (String) model.getValueAt(i, 2);
            if (category != null && !category.trim().isEmpty() && !category.equals("Unbekannt")) {
                categories.add(category);
            }
        }
        return categories.size();
    }

    /**
     * Get count of unique vendors in current view
     */
    private int countVendors() {
        Set<String> vendors = new HashSet<>();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            String vendor = (String) model.getValueAt(i, 8);
            if (vendor != null && !vendor.trim().isEmpty()) {
                vendors.add(vendor);
            }
        }
        return vendors.size();
    }

    /**
     * Calculate average article value (stock value / total quantity)
     */
    private double calculateAverageArticleValue() {
        int totalQty = calculateTotalQuantity();
        if (totalQty == 0) return 0.0;
        return calculateValueInStock() / totalQty;
    }

    /**
     * Get the most valuable article by total stock value
     */
    private String getMostValuableArticle() {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        double maxValue = 0.0;
        String articleName = "N/A";

        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            double einkaufspreis = (double) model.getValueAt(i, 7);
            double value = lagerbestand * einkaufspreis;

            if (value > maxValue) {
                maxValue = value;
                articleName = (String) model.getValueAt(i, 1); // Name
            }
        }

        return articleName;
    }

    /**
     * Calculate inventory turnover rate (articles in stock vs minimum required)
     */
    private double calculateStockHealthPercentage() {
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
        int articlesAboveMin = 0;
        int articlesWithMinSet = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            int lagerbestand = (int) model.getValueAt(i, 4);
            int mindestbestand = (int) model.getValueAt(i, 5);

            if (mindestbestand > 0) {
                articlesWithMinSet++;
                if (lagerbestand > mindestbestand) {
                    articlesAboveMin++;
                }
            }
        }

        if (articlesWithMinSet == 0) return 100.0;
        return (articlesAboveMin * 100.0) / articlesWithMinSet;
    }
}
