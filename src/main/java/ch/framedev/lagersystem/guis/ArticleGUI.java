package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.WarningManager;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ch.framedev.lagersystem.main.Main.articleListGUI;

/**
 * ArticleGUI with category support for better organization.
 * Categories are loaded from categories.json and mapped to articles based on article number ranges.
 */
@SuppressWarnings("unused")
public class ArticleGUI extends JFrame {

    private final JTable articleTable;
    // scroll pane reference so we can read viewport width on resize
    private final JScrollPane tableScrollPane;
    // base column widths - added category column between Name and Details
    private final int[] baseColumnWidths = new int[]{150, 200, 150, 290, 110, 110, 150, 150, 200};
    private final JLabel countLabel;

    // Category management
    private final JComboBox<String> categoryFilter;
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
        setTitle("Artikel Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Load categories from JSON
        loadCategories();

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
        JPanel toolbarWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 18));
        toolbarWrapper.setBackground(new Color(236, 239, 241));

        // Category filter
        JPanel categoryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        categoryPanel.setOpaque(false);
        JLabel categoryLabel = new JLabel("📁 Kategorie:");
        categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD));
        categoryFilter = new JComboBox<>();
        categoryFilter.addItem("Alle Kategorien");
        if (categories != null) {
            categories.keySet().stream().sorted().forEach(categoryFilter::addItem);
        }
        categoryFilter.setPreferredSize(new Dimension(200, 35));
        categoryFilter.addActionListener(e -> filterByCategory());
        categoryPanel.add(categoryLabel);
        categoryPanel.add(categoryFilter);
        toolbarWrapper.add(categoryPanel);

        // Add Article button
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
                } else {
                    JOptionPane.showMessageDialog(this, "Artikel erfolgreich hinzugefügt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // Edit Article button
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
                    JOptionPane.showMessageDialog(this, "Artikel erfolgreich aktualisiert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Artikels.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Delete Article button
        JButton deleteArticleButton = createRoundedButton("Artikel löschen");
        deleteArticleButton.addActionListener(e -> deleteSelectedArticle());

        // Retrieve QR-Code Data button
        JButton retrieveQrCodeDataButton = createRoundedButton("QR-Code Daten abrufen");
        retrieveQrCodeDataButton.addActionListener(e -> {
            List<Map<String, Object>> qrCodeData = QRCodeUtils.retrieveQrCodeDataFromWebsite();
            JOptionPane.showMessageDialog(this, "Es wurden " + qrCodeData.size() + " QR-Code Datensätze abgerufen.", "QR-Code Daten", JOptionPane.INFORMATION_MESSAGE);
        });

        toolbarWrapper.add(addArticleButton);
        toolbarWrapper.add(editArticleButton);
        toolbarWrapper.add(deleteArticleButton);
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

        // Add warnings button to bottom panel
        JButton showWarningsBottomBtn = new JButton("⚠️ Warnungen");
        showWarningsBottomBtn.setFocusPainted(false);
        showWarningsBottomBtn.setBorderPainted(false);
        showWarningsBottomBtn.setContentAreaFilled(false);
        showWarningsBottomBtn.setOpaque(true);
        showWarningsBottomBtn.setBackground(new Color(241, 196, 15));
        showWarningsBottomBtn.setForeground(Color.WHITE);
        showWarningsBottomBtn.setFont(showWarningsBottomBtn.getFont().deriveFont(Font.BOLD, 13f));
        showWarningsBottomBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(243, 156, 18), 2),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
        showWarningsBottomBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showWarningsBottomBtn.addActionListener(e -> showAllWarnings());
        showWarningsBottomBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showWarningsBottomBtn.setBackground(new Color(243, 156, 18));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showWarningsBottomBtn.setBackground(new Color(241, 196, 15));
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
        // listen to frame resizes
        this.addComponentListener(resizeListener);
        // and listen to viewport resizes (when card/scrollpane changes)
        tableScrollPane.getViewport().addComponentListener(resizeListener);

        // Ensure columns are adjusted once UI is visible
        SwingUtilities.invokeLater(this::adjustColumnWidths);
    }

    private Object[] showUpdateArticleDialog(Object[] existingData) {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(this, "Artikel Bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 247, 250));
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 190, 200), 2),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient-like effect
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185)); // Brighter blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("✏️  Artikel Bearbeiten");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Styled close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(41, 128, 185));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 24));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(new Color(231, 76, 60));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
            }
        });
        closeBtn.addActionListener(e -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        headerPanel.add(closeBtn, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Scrollable content card
        RoundedPanel contentCard = new RoundedPanel(Color.WHITE, 0);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Helper method for creating styled labels with icons
        java.util.function.Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setForeground(new Color(44, 62, 80));
            return label;
        };

        // Helper method for styling text fields with hover effect
        java.util.function.Consumer<JTextField> styleTextField = field -> {
            field.setFont(new Font("Arial", Font.PLAIN, 14));
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            field.setBackground(new Color(250, 251, 252));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                    ));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                    ));
                }
            });
        };

        int row = 0;

        // Artikelnummer with icon
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        JLabel nummerLabel = createLabel.apply("🔢  Artikelnummer *");
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
        JLabel nameLabel = createLabel.apply("📝  Name *");
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
        JLabel detailsLabel = createLabel.apply("📋  Details");
        contentCard.add(detailsLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 16, 6);
        JTextField detailsField = new JTextField(existingData[3] == null ? "" : existingData[3].toString(), 30);
        styleTextField.accept(detailsField);
        contentCard.add(detailsField, gbc);

        // Parse integer values robustly
        int existingLager = 0;
        int existingMindest = 0;
        try {
            Object o = existingData[4];
            if (o instanceof Number) existingLager = ((Number) o).intValue();
            else existingLager = Integer.parseInt(o.toString());
        } catch (Exception ignored) { }
        try {
            Object o = existingData[5];
            if (o instanceof Number) existingMindest = ((Number) o).intValue();
            else existingMindest = Integer.parseInt(o.toString());
        } catch (Exception ignored) { }

        row++;
        gbc.insets = new Insets(12, 6, 6, 6);
        // Stock fields with improved layout
        gbc.gridy = row;
        JLabel stockSectionLabel = createLabel.apply("📦  Lagerbestand");
        stockSectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentCard.add(stockSectionLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(8, 6, 16, 6);
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        stockPanel.setOpaque(false);

        JPanel lagerPanel = new JPanel(new BorderLayout(0, 6));
        lagerPanel.setOpaque(false);
        JLabel lagerLabel = new JLabel("Aktuell");
        lagerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        lagerLabel.setForeground(new Color(127, 140, 141));
        lagerPanel.add(lagerLabel, BorderLayout.NORTH);
        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(existingLager, 0, Integer.MAX_VALUE, 1));
        lagerSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) lagerSpinner.getEditor()).getTextField().setColumns(10);
        ((JSpinner.DefaultEditor) lagerSpinner.getEditor()).getTextField().setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            )
        );
        lagerPanel.add(lagerSpinner, BorderLayout.CENTER);

        JPanel mindestPanel = new JPanel(new BorderLayout(0, 6));
        mindestPanel.setOpaque(false);
        JLabel mindestLabel = new JLabel("Mindestbestand");
        mindestLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        mindestLabel.setForeground(new Color(127, 140, 141));
        mindestPanel.add(mindestLabel, BorderLayout.NORTH);
        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(existingMindest, 0, Integer.MAX_VALUE, 1));
        mindestSpinner.setFont(new Font("Arial", Font.PLAIN, 14));
        ((JSpinner.DefaultEditor) mindestSpinner.getEditor()).getTextField().setColumns(10);
        ((JSpinner.DefaultEditor) mindestSpinner.getEditor()).getTextField().setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
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
            Object o = existingData[6];
            if (o instanceof Number) existingVerkauf = ((Number) o).doubleValue();
            else existingVerkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) { }
        try {
            Object o = existingData[7];
            if (o instanceof Number) existingEinkauf = ((Number) o).doubleValue();
            else existingEinkauf = Double.parseDouble(o.toString());
        } catch (Exception ignored) { }

        row++;
        gbc.insets = new Insets(12, 6, 6, 6);
        // Price section
        gbc.gridy = row;
        JLabel priceSectionLabel = createLabel.apply("💰  Preise");
        priceSectionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        contentCard.add(priceSectionLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(8, 6, 16, 6);
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        pricePanel.setOpaque(false);

        JPanel verkaufPanel = new JPanel(new BorderLayout(0, 6));
        verkaufPanel.setOpaque(false);
        JLabel verkaufLabel = new JLabel("Verkaufspreis (CHF)");
        verkaufLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        verkaufLabel.setForeground(new Color(127, 140, 141));
        verkaufPanel.add(verkaufLabel, BorderLayout.NORTH);
        JFormattedTextField verkaufField = new JFormattedTextField(priceFormatter);
        verkaufField.setColumns(12);
        verkaufField.setValue(existingVerkauf);
        verkaufField.setFont(new Font("Arial", Font.PLAIN, 14));
        verkaufField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        verkaufField.setBackground(new Color(250, 251, 252));
        verkaufPanel.add(verkaufField, BorderLayout.CENTER);

        JPanel einkaufPanel = new JPanel(new BorderLayout(0, 6));
        einkaufPanel.setOpaque(false);
        JLabel einkaufLabel = new JLabel("Einkaufspreis (CHF)");
        einkaufLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        einkaufLabel.setForeground(new Color(127, 140, 141));
        einkaufPanel.add(einkaufLabel, BorderLayout.NORTH);
        JFormattedTextField einkaufField = new JFormattedTextField(priceFormatter);
        einkaufField.setColumns(12);
        einkaufField.setValue(existingEinkauf);
        einkaufField.setFont(new Font("Arial", Font.PLAIN, 14));
        einkaufField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        einkaufField.setBackground(new Color(250, 251, 252));
        einkaufPanel.add(einkaufField, BorderLayout.CENTER);

        pricePanel.add(verkaufPanel);
        pricePanel.add(einkaufPanel);
        contentCard.add(pricePanel, gbc);

        row++;
        gbc.insets = new Insets(6, 6, 6, 6);
        // Lieferant with icon
        gbc.gridy = row;
        JLabel lieferantLabel = createLabel.apply("🚚  Lieferant");
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
        buttonPanel.setBackground(new Color(250, 251, 252));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton cancelBtn = new JButton("✕  Abbrechen");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 13));
        cancelBtn.setForeground(new Color(52, 73, 94));
        cancelBtn.setBackground(new Color(255, 57, 57));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(new Color(220, 225, 230));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(new Color(236, 240, 241));
            }
        });

        JButton okBtn = new JButton("✓  Speichern");
        okBtn.setFont(new Font("Arial", Font.BOLD, 13));
        okBtn.setForeground(Color.BLACK);
        okBtn.setBackground(new Color(46, 204, 113));
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorderPainted(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(39, 174, 96), 1),
            BorderFactory.createEmptyBorder(10, 27, 10, 27)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(new Color(39, 174, 96));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(new Color(46, 204, 113));
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
                    JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.ERROR_MESSAGE);
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

    /**
     * Shows a polished modal dialog to enter a new article. Returns the table row or null if canceled.
     */
    private Object[] showAddArticleDialog() {
        final Object[][] resultHolder = new Object[1][];

        JDialog dialog = new JDialog(this, "Neuen Artikel hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with background
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 247, 250));
        mainContainer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(62, 84, 98));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        JLabel titleLabel = new JLabel("➕ Neuen Artikel hinzufügen");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 20f));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(62, 84, 98));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setFont(closeBtn.getFont().deriveFont(20f));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            resultHolder[0] = null;
            dialog.dispose();
        });
        headerPanel.add(closeBtn, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        RoundedPanel contentCard = new RoundedPanel(Color.WHITE, 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Helper method for creating styled labels
        java.util.function.Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
            label.setForeground(new Color(52, 73, 94));
            return label;
        };

        // Helper method for styling text fields
        java.util.function.Consumer<JTextField> styleTextField = field -> {
            field.setFont(field.getFont().deriveFont(14f));
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
        };

        int row = 0;

        // Artikelnummer
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        contentCard.add(createLabel.apply("Artikelnummer *"), gbc);
        row++;
        gbc.gridy = row;
        JTextField nummerField = new JTextField(25);
        styleTextField.accept(nummerField);
        contentCard.add(nummerField, gbc);

        row++;
        // Name
        gbc.gridy = row;
        contentCard.add(createLabel.apply("Name *"), gbc);
        row++;
        gbc.gridy = row;
        JTextField nameField = new JTextField(25);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        row++;
        // Details
        gbc.gridy = row;
        contentCard.add(createLabel.apply("Details"), gbc);
        row++;
        gbc.gridy = row;
        JTextField detailsField = new JTextField(25);
        styleTextField.accept(detailsField);
        contentCard.add(detailsField, gbc);

        row++;
        // Two columns for stock fields
        JPanel stockPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        stockPanel.setOpaque(false);

        JPanel lagerPanel = new JPanel(new BorderLayout(0, 4));
        lagerPanel.setOpaque(false);
        lagerPanel.add(createLabel.apply("Lagerbestand"), BorderLayout.NORTH);
        JSpinner lagerSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        lagerSpinner.setFont(lagerSpinner.getFont().deriveFont(14f));
        ((JSpinner.DefaultEditor) lagerSpinner.getEditor()).getTextField().setColumns(8);
        lagerPanel.add(lagerSpinner, BorderLayout.CENTER);

        JPanel mindestPanel = new JPanel(new BorderLayout(0, 4));
        mindestPanel.setOpaque(false);
        mindestPanel.add(createLabel.apply("Mindestbestand"), BorderLayout.NORTH);
        JSpinner mindestSpinner = new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        mindestSpinner.setFont(mindestSpinner.getFont().deriveFont(14f));
        ((JSpinner.DefaultEditor) mindestSpinner.getEditor()).getTextField().setColumns(8);
        mindestPanel.add(mindestSpinner, BorderLayout.CENTER);

        stockPanel.add(lagerPanel);
        stockPanel.add(mindestPanel);

        gbc.gridy = row;
        contentCard.add(stockPanel, gbc);

        // Price fields with formatter
        NumberFormat priceFormat = NumberFormat.getNumberInstance();
        priceFormat.setMinimumFractionDigits(2);
        priceFormat.setMaximumFractionDigits(2);
        NumberFormatter priceFormatter = new NumberFormatter(priceFormat);
        priceFormatter.setValueClass(Double.class);
        priceFormatter.setAllowsInvalid(false);
        priceFormatter.setMinimum(0.0);

        row++;
        // Two columns for price fields
        JPanel pricePanel = new JPanel(new GridLayout(1, 2, 16, 0));
        pricePanel.setOpaque(false);

        JPanel verkaufPanel = new JPanel(new BorderLayout(0, 4));
        verkaufPanel.setOpaque(false);
        verkaufPanel.add(createLabel.apply("Verkaufspreis (CHF)"), BorderLayout.NORTH);
        JFormattedTextField verkaufField = new JFormattedTextField(priceFormatter);
        verkaufField.setColumns(10);
        verkaufField.setValue(0.0);
        verkaufField.setFont(verkaufField.getFont().deriveFont(14f));
        verkaufField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        verkaufPanel.add(verkaufField, BorderLayout.CENTER);

        JPanel einkaufPanel = new JPanel(new BorderLayout(0, 4));
        einkaufPanel.setOpaque(false);
        einkaufPanel.add(createLabel.apply("Einkaufspreis (CHF)"), BorderLayout.NORTH);
        JFormattedTextField einkaufField = new JFormattedTextField(priceFormatter);
        einkaufField.setColumns(10);
        einkaufField.setValue(0.0);
        einkaufField.setFont(einkaufField.getFont().deriveFont(14f));
        einkaufField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        einkaufPanel.add(einkaufField, BorderLayout.CENTER);

        pricePanel.add(verkaufPanel);
        pricePanel.add(einkaufPanel);

        gbc.gridy = row;
        contentCard.add(pricePanel, gbc);

        row++;
        // Lieferant
        gbc.gridy = row;
        contentCard.add(createLabel.apply("Lieferant"), gbc);
        row++;
        gbc.gridy = row;
        JTextField lieferantField = new JTextField(25);
        styleTextField.accept(lieferantField);
        contentCard.add(lieferantField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(new Color(245, 247, 250));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setFont(cancelBtn.getFont().deriveFont(Font.BOLD, 13f));
        cancelBtn.setForeground(new Color(52, 73, 94));
        cancelBtn.setBackground(new Color(236, 240, 241));
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JButton okBtn = new JButton("➕ Hinzufügen");
        okBtn.setFont(okBtn.getFont().deriveFont(Font.BOLD, 13f));
        okBtn.setForeground(Color.BLACK);
        okBtn.setBackground(new Color(52, 152, 219));
        okBtn.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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
                    JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            resultHolder[0] = new Object[]{nummer, name, details, lagerbestand, mindestbestand,
                verkaufspreis, einkaufspreis, lieferant};
            dialog.dispose();
        });

        // Show dialog
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nummerField::requestFocusInWindow);
        dialog.setVisible(true);

        return resultHolder[0];
    }

    private void initializeTable() {
        // Provide a typed, non-editable table model so sorting & renderers behave correctly
        DefaultTableModel model = getDefaultTableModel();

        // Set model
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
        header.setBackground(new Color(62, 84, 98));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendTestWarning();
            }
        }, 500); // slight delay to ensure test warning shows after GUI is visible
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
                return false; // non-editable table (editing done via dialogs)
            }
        };

        model.addColumn("Artikelnummer");
        model.addColumn("Name");
        model.addColumn("Kategorie");  // New category column
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
        // Insert category after name (index 2)
        Object[] rowWithCategory = new Object[9];
        rowWithCategory[0] = rowData[0]; // Article number
        rowWithCategory[1] = rowData[1]; // Name
        rowWithCategory[2] = getCategoryForArticle((String) rowData[0]); // Category
        System.arraycopy(rowData, 2, rowWithCategory, 3, rowData.length - 2);
        model.addRow(rowWithCategory);
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

    private void sendTestWarning() {
        Warning warning = new Warning("Test warning", "Der Lagerbestand für Artikel A123 ist niedrig.", Warning.WarningType.LOW_STOCK, new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date()), false, false);
        displayWarning(warning);
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        // softer button background and darker text for contrast
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(0, 0, 0));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(205, 205, 207));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(237, 242, 247));
            }
        });
        // button.setPreferredSize(new Dimension(180, 40));
        return button;
    }

    private void loadArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        java.util.List<Article> articles = articleManager.getAllArticles();
        if (articles == null) return;
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();
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

    /**
     * Displays a warning in a modern, styled dialog
     * @param warning The warning object to display
     */
    public void displayWarning(Warning warning) {
        SwingUtilities.invokeLater(() -> {
            // Create custom dialog
            JDialog dialog = new JDialog(this, "Warnung", true);
            dialog.setUndecorated(true);
            dialog.setLayout(new BorderLayout());

            // Main panel with rounded corners
            RoundedPanel mainPanel = new RoundedPanel(new Color(245, 247, 250), 20);
            mainPanel.setLayout(new BorderLayout(0, 0));
            mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
            ));

            // Determine colors based on warning type
            Color headerColor;
            Color accentColor;
            String icon;
            switch (warning.getType()) {
                case LOW_STOCK:
                    headerColor = new Color(241, 196, 15); // Orange/Yellow
                    accentColor = new Color(243, 156, 18);
                    icon = "⚠️";
                    break;
                case ORDER_NEEDED:
                    headerColor = new Color(231, 76, 60); // Red
                    accentColor = new Color(192, 57, 43);
                    icon = "🔴";
                    break;
                default:
                    headerColor = new Color(52, 152, 219); // Blue
                    accentColor = new Color(41, 128, 185);
                    icon = "ℹ️";
                    break;
            }

            // Header with warning type
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(headerColor);
            header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

            JLabel typeLabel = new JLabel(icon + "  " + warning.getType().getDisplayName());
            typeLabel.setFont(typeLabel.getFont().deriveFont(Font.BOLD, 18f));
            typeLabel.setForeground(Color.WHITE);
            header.add(typeLabel, BorderLayout.WEST);

            // Close button
            JButton closeBtn = new JButton("✕");
            closeBtn.setFont(closeBtn.getFont().deriveFont(Font.BOLD, 16f));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setBackground(headerColor);
            closeBtn.setBorderPainted(false);
            closeBtn.setContentAreaFilled(false);
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> dialog.dispose());
            header.add(closeBtn, BorderLayout.EAST);

            mainPanel.add(header, BorderLayout.NORTH);

            // Content panel
            JPanel content = new JPanel(new GridBagLayout());
            content.setBackground(Color.WHITE);
            content.setBorder(BorderFactory.createEmptyBorder(24, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 12, 0);

            // Title
            JLabel titleLabel = new JLabel("<html><b>" + warning.getTitle() + "</b></html>");
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));
            titleLabel.setForeground(new Color(44, 62, 80));
            content.add(titleLabel, gbc);

            // Message
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 16, 0);
            JTextArea messageArea = new JTextArea(warning.getMessage());
            messageArea.setWrapStyleWord(true);
            messageArea.setLineWrap(true);
            messageArea.setEditable(false);
            messageArea.setFont(messageArea.getFont().deriveFont(14f));
            messageArea.setForeground(new Color(52, 73, 94));
            messageArea.setBackground(Color.WHITE);
            messageArea.setBorder(null);
            messageArea.setRows(4);
            messageArea.setColumns(35);
            content.add(messageArea, gbc);

            // Date
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 20, 0);
            JLabel dateLabel = new JLabel("📅 Datum: " + warning.getDate());
            dateLabel.setFont(dateLabel.getFont().deriveFont(12f));
            dateLabel.setForeground(new Color(127, 140, 141));
            content.add(dateLabel, gbc);

            // Status indicator
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 24, 0);
            JLabel statusLabel = new JLabel(warning.isResolved() ? "✅ Gelöst" : "❌ Ungelöst");
            statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));
            statusLabel.setForeground(warning.isResolved() ? new Color(39, 174, 96) : new Color(231, 76, 60));
            content.add(statusLabel, gbc);

            // Button panel
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 0, 0);
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setOpaque(false);

            if (!warning.isResolved()) {
                JButton resolveBtn = new JButton("✓ Als gelöst markieren");
                styleButton(resolveBtn, new Color(39, 174, 96), Color.WHITE);
                resolveBtn.addActionListener(e -> {
                    warning.setResolved(true);
                    JOptionPane.showMessageDialog(dialog,
                        "Die Warnung wurde als gelöst markiert.",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                });
                buttonPanel.add(resolveBtn);
            }

            JButton okBtn = new JButton("Schließen");
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
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));
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

    /**
     * Display all warnings from WarningManager in a modern dialog
     */
    private void showAllWarnings() {
        WarningManager warningManager = WarningManager.getInstance();
        List<Warning> warnings = warningManager.getAllWarnings();

        // Create modern dialog
        JDialog dialog = new JDialog(this, "Alle Warnungen", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 247, 250));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("⚠️ Warnungen Übersicht");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(31, 45, 61));

        JLabel countLabel = new JLabel(warnings.size() + " Warnung(en)");
        countLabel.setFont(countLabel.getFont().deriveFont(14f));
        countLabel.setForeground(new Color(100, 110, 120));

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(countLabel, BorderLayout.EAST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        // Main content with table
        if (warnings.isEmpty()) {
            JPanel emptyPanel = new JPanel(new GridBagLayout());
            emptyPanel.setBackground(Color.WHITE);
            JLabel emptyLabel = new JLabel("✓ Keine Warnungen vorhanden");
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(16f));
            emptyLabel.setForeground(new Color(100, 180, 100));
            emptyPanel.add(emptyLabel);
            dialog.add(emptyPanel, BorderLayout.CENTER);
        } else {
            // Create table model
            String[] columnNames = {"Status", "Typ", "Titel", "Nachricht", "Datum"};
            DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };

            // Populate table
            for (Warning warning : warnings) {
                String status = warning.isResolved() ? "✓ Gelöst" : "⚠ Offen";
                String type = switch (warning.getType()) {
                    case LOW_STOCK -> "Niedriger Bestand";
                    case ORDER_NEEDED -> "Bestellung nötig";
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

            JTable warningsTable = new JTable(tableModel);
            warningsTable.setRowHeight(32);
            warningsTable.setFont(warningsTable.getFont().deriveFont(14f));
            warningsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            warningsTable.setShowGrid(true);
            warningsTable.setGridColor(new Color(226, 230, 233));
            warningsTable.setIntercellSpacing(new Dimension(1, 1));

            // Custom renderer for status column
            warningsTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value,
                        boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        if (value.toString().contains("Gelöst")) {
                            setForeground(new Color(40, 150, 40));
                        } else {
                            setForeground(new Color(200, 100, 0));
                        }
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

            // Header styling
            JTableHeader header = warningsTable.getTableHeader();
            header.setBackground(new Color(62, 84, 98));
            header.setForeground(Color.WHITE);
            header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));

            JScrollPane scrollPane = new JScrollPane(warningsTable);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

            JPanel tablePanel = new JPanel(new BorderLayout());
            tablePanel.setBackground(new Color(245, 247, 250));
            tablePanel.add(scrollPane, BorderLayout.CENTER);

            dialog.add(tablePanel, BorderLayout.CENTER);

            // Action panel at bottom
            JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
            actionPanel.setBackground(new Color(245, 247, 250));
            actionPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

            JButton viewDetailsBtn = new JButton("Details anzeigen");
            styleButton(viewDetailsBtn, new Color(65, 105, 225), Color.WHITE);
            viewDetailsBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Warning selectedWarning = warnings.get(selectedRow);
                displayWarning(selectedWarning);
            });

            JButton resolveBtn = new JButton("Als gelöst markieren");
            styleButton(resolveBtn, new Color(40, 180, 99), Color.WHITE);
            resolveBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                Warning selectedWarning = warnings.get(selectedRow);
                if (selectedWarning.isResolved()) {
                    JOptionPane.showMessageDialog(dialog,
                        "Diese Warnung wurde bereits gelöst.",
                        "Bereits gelöst",
                        JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                if (warningManager.resolveWarning(selectedWarning.getTitle())) {
                    tableModel.setValueAt("✓ Gelöst", selectedRow, 0);
                    JOptionPane.showMessageDialog(dialog,
                        "Warnung wurde als gelöst markiert.",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            });

            JButton deleteBtn = new JButton("Löschen");
            styleButton(deleteBtn, new Color(231, 76, 60), Color.WHITE);
            deleteBtn.addActionListener(e -> {
                int selectedRow = warningsTable.getSelectedRow();
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(dialog,
                        "Bitte wählen Sie eine Warnung aus.",
                        "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Möchten Sie diese Warnung wirklich löschen?",
                    "Löschen bestätigen",
                    JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Warning selectedWarning = warnings.get(selectedRow);
                    if (warningManager.deleteWarning(selectedWarning.getTitle())) {
                        tableModel.removeRow(selectedRow);
                        warnings.remove(selectedRow);
                        countLabel.setText(warnings.size() + " Warnung(en)");
                        JOptionPane.showMessageDialog(dialog,
                            "Warnung wurde gelöscht.",
                            "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            });

            JButton refreshBtn = new JButton("🔄 Aktualisieren");
            styleButton(refreshBtn, new Color(237, 242, 247), new Color(20, 30, 40));
            refreshBtn.addActionListener(e -> {
                dialog.dispose();
                showAllWarnings();
            });

            JButton closeBtn = new JButton("Schließen");
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
            buttonPanel.setBackground(new Color(245, 247, 250));
            JButton closeBtn = new JButton("Schließen");
            styleButton(closeBtn, new Color(149, 165, 166), Color.WHITE);
            closeBtn.addActionListener(e -> dialog.dispose());
            buttonPanel.add(closeBtn);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
        }

        dialog.setVisible(true);
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
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>(){}.getType();
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
            JOptionPane.showMessageDialog(null, "Fehler beim Laden der Kategorien: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
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
        String selectedCategory = (String) categoryFilter.getSelectedItem();

        if (selectedCategory == null || selectedCategory.equals("Alle Kategorien")) {
            // Show all articles
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                ((TableRowSorter<?>) articleTable.getRowSorter()).setRowFilter(null);
            }
        } else {
            // Filter by category (column index 2)
            if (articleTable.getRowSorter() instanceof TableRowSorter) {
                @SuppressWarnings("unchecked")
                TableRowSorter<DefaultTableModel> sorter = (TableRowSorter<DefaultTableModel>) articleTable.getRowSorter();
                sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote(selectedCategory), 2));
            } else {
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) articleTable.getModel());
                articleTable.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote(selectedCategory), 2));
            }
        }
        updateCountLabel();
    }
}
