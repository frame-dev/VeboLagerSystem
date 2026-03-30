package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import static ch.framedev.lagersystem.utils.JFrameUtils.*;

/**
 * GUI window that manages supplier reorders for articles.
 */
@SuppressWarnings("DuplicatedCode")
public class SupplierOrderGUI extends JFrame {

    private static final int ARTICLE_NUMBER_COLUMN_INDEX = 0;
    private static final int ARTICLE_NAME_COLUMN_INDEX = 1;
    private static final int VENDOR_COLUMN_INDEX = 2;
    private static final int QUANTITY_COLUMN_INDEX = 3;
    private static final int STOCK_COLUMN_INDEX = 4;
    private static final int ADDED_AT_COLUMN_INDEX = 5;

    private static final String FILE_SEPARATOR = "|";
    private static final Path ORDER_FILE = new File(Main.getAppDataDir(), "supplier_orders.txt").toPath();
    private static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final int[] BASE_COLUMN_WIDTHS = new int[]{150, 280, 180, 120, 120, 170};
    private static final String[] COLUMNS = {
            UnicodeSymbols.NUMBERS + " Artikelnummer",
            UnicodeSymbols.ARTICLE_NAME + " Name",
            UnicodeSymbols.TRUCK + " Lieferant",
            UnicodeSymbols.PACKAGE + " Bestellmenge",
            UnicodeSymbols.COL_LAGER_MENGE,
            UnicodeSymbols.CLOCK + " Hinzugefügt"
    };

    private static SupplierOrderGUI INSTANCE;

    private final Logger logger = LogManager.getLogger(SupplierOrderGUI.class);
    private final List<OrderItem> orderItems = new ArrayList<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JScrollPane tableScrollPane;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JLabel statusLabel = new JLabel();
    private final JLabel quantityLabel = new JLabel();
    private final JTextField searchField = new JTextField(26);
    private final JButton removeButton;
    private final JButton clearButton;
    private final JButton saveButton;

    public SupplierOrderGUI() {
        ThemeManager.getInstance().registerWindow(this);
        INSTANCE = this;

        setTitle("Lieferantenbestellungen");
        setMinimumSize(new Dimension(980, 600));
        setSize(1120, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JPanel headerWrapper = createHeaderPanel();
        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }

        JFrameUtils.RoundedPanel toolbarCard = createRoundedCard(18);
        toolbarCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        removeButton = createDangerButton(UnicodeSymbols.TRASH + " Entfernen");
        removeButton.setToolTipText("Ausgewählten Bestellposten entfernen");
        clearButton = createDangerButton(UnicodeSymbols.BROOM + " Alle löschen");
        clearButton.setToolTipText("Alle Lieferantenbestellungen löschen");
        saveButton = createSecondaryButton(UnicodeSymbols.FLOPPY + " Speichern");
        saveButton.setToolTipText("Bestellliste in Datei speichern");
        JButton refreshButton = createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.setToolTipText("Tabelle und Lagerwerte aktualisieren");

        removeButton.addActionListener(e -> removeSelected());
        clearButton.addActionListener(e -> clearAll());
        saveButton.addActionListener(e -> persistWithFeedback());
        refreshButton.addActionListener(e -> refreshTable());

        toolbarCard.add(removeButton);
        toolbarCard.add(clearButton);
        toolbarCard.add(saveButton);
        toolbarCard.add(refreshButton);
        topContainer.add(toolbarCard);

        mainContainer.add(topContainer, BorderLayout.NORTH);

        tableModel = createTableModel();
        table = createTable();
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder());
        tableScrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        tableScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JFrameUtils.RoundedPanel tableCard = createRoundedCard(18);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        tableCard.add(tableScrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(tableCard, gbc);
        mainContainer.add(centerWrapper, BorderLayout.CENTER);

        mainContainer.add(createSearchCard(), BorderLayout.SOUTH);
        setContentPane(mainContainer);

        configureInteractions();
        loadFromFile();
        refreshTable();
        installKeyboardShortcuts(refreshButton);
    }

    private record OrderItem(String articleNumber, String name, String vendor, int quantity, int stock, String addedAt) {
    }

    private void installKeyboardShortcuts(JButton refreshButton) {
        JRootPane rootPane = getRootPane();
        KeyboardShortcutUtils.addTooltipHint(searchField, KeyboardShortcutUtils.menuKey(KeyEvent.VK_F));
        KeyboardShortcutUtils.addTooltipHint(removeButton, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        KeyboardShortcutUtils.addTooltipHint(saveButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_S));
        KeyboardShortcutUtils.addTooltipHint(refreshButton, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        KeyboardShortcutUtils.registerClose(this);
        KeyboardShortcutUtils.registerFocus(rootPane, "supplierOrders.focusSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_F), searchField);
        KeyboardShortcutUtils.registerButton(rootPane, "supplierOrders.remove",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), removeButton, true);
        KeyboardShortcutUtils.registerButton(rootPane, "supplierOrders.save",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_S), saveButton);
        KeyboardShortcutUtils.registerButton(rootPane, "supplierOrders.refresh",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshButton);
        KeyboardShortcutUtils.register(rootPane, "supplierOrders.clearSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_L), this::clearSearch);
    }

    private JPanel createHeaderPanel() {
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (disableHeader) {
            return null;
        }

        JFrameUtils.RoundedPanel headerCard = createRoundedCard(20);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.TRUCK + " Lieferanten-Bestellungen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Nachbestellungen prüfen, filtern und speichern");
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

        headerCard.add(headerText, BorderLayout.WEST);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(headerCard, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSearchCard() {
        JFrameUtils.RoundedPanel searchCard = createRoundedCard(18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Artikelnummer, Name oder Lieferant):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);
        leftSearch.add(searchLabel);
        leftSearch.add(searchField);

        styleBadge(statusLabel);
        styleBadge(quantityLabel);

        JButton searchButton = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        JButton clearSearchButton = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");
        searchButton.addActionListener(e -> applySearchFilter());
        clearSearchButton.addActionListener(e -> clearSearch());

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(statusLabel);
        rightActions.add(quantityLabel);
        rightActions.add(Box.createHorizontalStrut(6));
        rightActions.add(searchButton);
        rightActions.add(clearSearchButton);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(ThemeManager.getBackgroundColor());
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));
        searchWrapper.add(searchCard, BorderLayout.CENTER);

        searchCard.add(leftSearch, BorderLayout.CENTER);
        searchCard.add(rightActions, BorderLayout.EAST);
        return searchWrapper;
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == QUANTITY_COLUMN_INDEX || columnIndex == STOCK_COLUMN_INDEX) {
                    return Integer.class;
                }
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createTable() {
        JTable orderTable = new JTable(tableModel) {
            @Override
            public String getToolTipText(MouseEvent event) {
                int viewRow = rowAtPoint(event.getPoint());
                int viewColumn = columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return null;
                }

                int modelRow = convertRowIndexToModel(viewRow);
                int modelColumn = convertColumnIndexToModel(viewColumn);
                return buildCellToolTip(modelRow, modelColumn);
            }
        };

        orderTable.setRowHeight(28);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setShowGrid(true);
        orderTable.setGridColor(ThemeManager.getTableGridColor());
        orderTable.setIntercellSpacing(new Dimension(1, 1));
        orderTable.setFillsViewportHeight(true);
        orderTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        orderTable.setBackground(ThemeManager.getTableRowEvenColor());
        orderTable.setForeground(ThemeManager.getTextPrimaryColor());
        orderTable.setSelectionBackground(ThemeManager.getTableSelectionColor());
        orderTable.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());
        orderTable.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                return label;
            }
        };
        orderTable.setDefaultRenderer(Object.class, defaultRenderer);

        DefaultTableCellRenderer articleNumberRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setFont(SettingsGUI.getFontByName(Font.BOLD, SettingsGUI.TABLE_FONT_SIZE));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        };

        DefaultTableCellRenderer quantityRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(SettingsGUI.getFontByName(Font.BOLD, SettingsGUI.TABLE_FONT_SIZE));
                return label;
            }
        };

        DefaultTableCellRenderer stockRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    int stock = value instanceof Number ? ((Number) value).intValue() : safeInt(String.valueOf(value));
                    int quantity = safeInt(String.valueOf(table.getValueAt(row, QUANTITY_COLUMN_INDEX)));
                    if (stock <= 0) {
                        label.setForeground(ThemeManager.getErrorColor());
                    } else if (stock <= quantity) {
                        label.setForeground(ThemeManager.getWarningColor());
                    }
                }
                return label;
            }
        };

        DefaultTableCellRenderer addedAtRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    label.setForeground(ThemeManager.getTextSecondaryColor());
                }
                return label;
            }
        };

        TableColumnModel columnModel = orderTable.getColumnModel();
        for (int i = 0; i < BASE_COLUMN_WIDTHS.length && i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setPreferredWidth(BASE_COLUMN_WIDTHS[i]);
        }
        columnModel.getColumn(ARTICLE_NUMBER_COLUMN_INDEX).setCellRenderer(articleNumberRenderer);
        columnModel.getColumn(QUANTITY_COLUMN_INDEX).setCellRenderer(quantityRenderer);
        columnModel.getColumn(STOCK_COLUMN_INDEX).setCellRenderer(stockRenderer);
        columnModel.getColumn(ADDED_AT_COLUMN_INDEX).setCellRenderer(addedAtRenderer);

        JTableHeader header = orderTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackground());
        header.setForeground(ThemeManager.getTableHeaderForeground());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));

        return orderTable;
    }

    private void configureInteractions() {
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applySearchFilter();
            }
        });

        searchField.addActionListener(e -> applySearchFilter());
        searchField.registerKeyboardAction(
                e -> clearSearch(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED
        );

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateStatusLabels();
                updateActionState();
            }
        });

        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths(table, tableScrollPane, BASE_COLUMN_WIDTHS);
            }
        };
        addComponentListener(resizeListener);
        tableScrollPane.getViewport().addComponentListener(resizeListener);
        SwingUtilities.invokeLater(() -> adjustColumnWidths(table, tableScrollPane, BASE_COLUMN_WIDTHS));
    }

    private void loadFromFile() {
        ensureOrderFileExists();
        orderItems.clear();

        try {
            List<String> lines = Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(Pattern.quote(FILE_SEPARATOR), -1);
                if (parts.length < 6) {
                    continue;
                }

                orderItems.add(new OrderItem(
                        parts[0],
                        parts[1],
                        parts[2],
                        safeInt(parts[3]),
                        safeInt(parts[4]),
                        parts[5]
                ));
            }
            syncStockLevels();
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Lieferanten-Bestelldatei: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Laden der Lieferanten-Bestelldatei: " + e.getMessage());
        }
    }

    private void persistWithFeedback() {
        if (persist()) {
            new MessageDialog()
                    .setTitle("Gespeichert")
                    .setMessage("Lieferantenbestellungen wurden gespeichert.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } else {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Die Lieferantenbestellungen konnten nicht gespeichert werden.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    private boolean persist() {
        ensureOrderFileExists();
        syncStockLevels();

        List<String> lines = new ArrayList<>();
        for (OrderItem item : orderItems) {
            if (item == null) {
                continue;
            }

            lines.add(String.join(FILE_SEPARATOR,
                    safeText(item.articleNumber()),
                    safeText(item.name()),
                    safeText(item.vendor()),
                    String.valueOf(item.quantity()),
                    String.valueOf(item.stock()),
                    safeText(item.addedAt())));

            VendorOrderLogging.getInstance().addLog(
                    "Bestellposten gespeichert: Artikelnummer " + item.articleNumber() + ", Menge: " + item.quantity());
        }

        try {
            Files.write(ORDER_FILE, lines, StandardCharsets.UTF_8);
            refreshTable();
            return true;
        } catch (Exception e) {
            logger.error("Fehler beim Speichern der Lieferanten-Bestellungen: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Speichern der Lieferanten-Bestellungen: " + e.getMessage());
            return false;
        }
    }

    private void refreshTable() {
        syncStockLevels();
        tableModel.setRowCount(0);

        List<OrderItem> sortedItems = new ArrayList<>(orderItems);
        sortedItems.sort((first, second) -> parseAddedAt(second.addedAt()).compareTo(parseAddedAt(first.addedAt())));

        for (OrderItem item : sortedItems) {
            tableModel.addRow(new Object[]{
                    item.articleNumber(),
                    item.name(),
                    item.vendor(),
                    item.quantity(),
                    item.stock(),
                    item.addedAt()
            });
        }

        updateStatusLabels();
        updateActionState();
        SwingUtilities.invokeLater(() -> adjustColumnWidths(table, tableScrollPane, BASE_COLUMN_WIDTHS));
    }

    private void updateStatusLabels() {
        int total = orderItems.size();
        int visible = table.getRowCount();
        int selected = table.getSelectedRow() >= 0 ? 1 : 0;
        int visibleQuantity = 0;

        for (int viewRow = 0; viewRow < table.getRowCount(); viewRow++) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            Object value = tableModel.getValueAt(modelRow, QUANTITY_COLUMN_INDEX);
            visibleQuantity += value instanceof Number ? ((Number) value).intValue() : safeInt(String.valueOf(value));
        }

        statusLabel.setText("Einträge: " + visible + " / " + total + (selected > 0 ? " • Auswahl: 1" : ""));
        quantityLabel.setText("Bestellmenge: " + visibleQuantity);
    }

    private void updateActionState() {
        boolean hasItems = !orderItems.isEmpty();
        removeButton.setEnabled(table.getSelectedRow() >= 0);
        clearButton.setEnabled(hasItems);
        saveButton.setEnabled(hasItems);
    }

    private void applySearchFilter() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            String regex = "(?i)" + Pattern.quote(text);
            sorter.setRowFilter(RowFilter.regexFilter(regex,
                    ARTICLE_NUMBER_COLUMN_INDEX,
                    ARTICLE_NAME_COLUMN_INDEX,
                    VENDOR_COLUMN_INDEX));
        }
        updateStatusLabels();
    }

    private void clearSearch() {
        searchField.setText("");
        sorter.setRowFilter(null);
        updateStatusLabels();
    }

    private void removeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte wählen Sie einen Bestellposten zum Entfernen aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        int modelRow = table.convertRowIndexToModel(viewRow);
        String articleNumber = safeText(tableModel.getValueAt(modelRow, ARTICLE_NUMBER_COLUMN_INDEX));
        String articleName = safeText(tableModel.getValueAt(modelRow, ARTICLE_NAME_COLUMN_INDEX));

        orderItems.removeIf(item -> Objects.equals(item.articleNumber(), articleNumber));
        if (persist()) {
            VendorOrderLogging.getInstance().addLog(
                    "Bestellposten entfernt: Artikelnummer " + articleNumber + " (" + articleName + ")");
        }
    }

    private void clearAll() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Alle Lieferanten-Bestellposten löschen?",
                "Bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                Main.iconSmall
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        orderItems.clear();
        if (persist()) {
            VendorOrderLogging.getInstance().addLog("Alle Lieferanten-Bestellposten wurden gelöscht.");
        }
    }

    private void upsertItem(Article article, int quantity) {
        if (article == null) {
            throw new IllegalArgumentException("article must not be null");
        }

        String articleNumber = article.getArticleNumber();
        Optional<OrderItem> existing = orderItems.stream()
                .filter(item -> Objects.equals(item.articleNumber(), articleNumber))
                .findFirst();

        if (existing.isPresent()) {
            OrderItem previous = existing.get();
            int index = orderItems.indexOf(previous);
            orderItems.set(index, new OrderItem(
                    previous.articleNumber(),
                    defaultIfBlank(previous.name(), article.getName()),
                    defaultIfBlank(previous.vendor(), article.getVendorName()),
                    previous.quantity() + quantity,
                    article.getStockQuantity(),
                    previous.addedAt()
            ));
        } else {
            orderItems.add(new OrderItem(
                    articleNumber,
                    defaultIfBlank(article.getName(), "Unbenannter Artikel"),
                    defaultIfBlank(article.getVendorName(), "Kein Lieferant"),
                    quantity,
                    article.getStockQuantity(),
                    LocalDateTime.now().format(DISPLAY_DATE_FORMAT)
            ));
        }

        persist();
    }

    public static void addArticleToSupplierOrder(Article article, int quantity) {
        if (article == null || quantity <= 0) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            SupplierOrderGUI gui = getInstance();
            gui.upsertItem(article, quantity);
            VendorOrderLogging.getInstance().addLog(
                    "Artikel zur Bestellliste hinzugefügt: " + article.getArticleNumber() + " x" + quantity);
        });
    }

    private void syncStockLevels() {
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            Article article = ArticleManager.getInstance().getArticleByNumber(item.articleNumber());
            if (article == null) {
                continue;
            }

            orderItems.set(i, new OrderItem(
                    item.articleNumber(),
                    defaultIfBlank(item.name(), article.getName()),
                    defaultIfBlank(item.vendor(), article.getVendorName()),
                    item.quantity(),
                    article.getStockQuantity(),
                    item.addedAt()
            ));
        }
    }

    private static LocalDateTime parseAddedAt(String addedAt) {
        if (addedAt == null || addedAt.isBlank()) {
            return LocalDateTime.MIN;
        }
        try {
            return LocalDateTime.parse(addedAt, DISPLAY_DATE_FORMAT);
        } catch (DateTimeParseException ignored) {
            return LocalDateTime.MIN;
        }
    }

    private String buildCellToolTip(int modelRow, int modelColumn) {
        String articleNumber = safeText(tableModel.getValueAt(modelRow, ARTICLE_NUMBER_COLUMN_INDEX));
        String articleName = safeText(tableModel.getValueAt(modelRow, ARTICLE_NAME_COLUMN_INDEX));
        String vendor = safeText(tableModel.getValueAt(modelRow, VENDOR_COLUMN_INDEX));
        int quantity = safeInt(String.valueOf(tableModel.getValueAt(modelRow, QUANTITY_COLUMN_INDEX)));
        int stock = safeInt(String.valueOf(tableModel.getValueAt(modelRow, STOCK_COLUMN_INDEX)));
        String addedAt = safeText(tableModel.getValueAt(modelRow, ADDED_AT_COLUMN_INDEX));

        if (modelColumn == ARTICLE_NAME_COLUMN_INDEX || modelColumn == VENDOR_COLUMN_INDEX) {
            String text = safeText(tableModel.getValueAt(modelRow, modelColumn));
            if (!text.isEmpty()) {
                return text;
            }
        }

        return "<html><b>" + escapeHtml(articleNumber) + " - " + escapeHtml(articleName) + "</b><br>"
                + "Lieferant: " + escapeHtml(vendor) + "<br>"
                + "Bestellmenge: " + quantity + "<br>"
                + "Lager: " + stock + "<br>"
                + "Hinzugefügt: " + escapeHtml(addedAt) + "</html>";
    }

    private void styleBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void applyTableColors(Component component, boolean isSelected, int row) {
        if (!isSelected) {
            component.setBackground(row % 2 == 0
                    ? ThemeManager.getTableRowEvenColor()
                    : ThemeManager.getTableRowOddColor());
            component.setForeground(ThemeManager.getTextPrimaryColor());
        } else {
            component.setBackground(ThemeManager.getTableSelectionColor());
            component.setForeground(ThemeManager.getTextOnPrimaryColor());
        }
    }

    private JFrameUtils.RoundedPanel createRoundedCard(int radius) {
        return new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), radius);
    }

    private JButton createDangerButton(String text) {
        return JFrameUtils.createThemeButton(text, ThemeManager.getErrorColor());
    }

    private static void ensureOrderFileExists() {
        try {
            File parent = ORDER_FILE.toFile().getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!Files.exists(ORDER_FILE)) {
                Files.createFile(ORDER_FILE);
            }
        } catch (Exception ignored) {
        }
    }

    private static int safeInt(String text) {
        try {
            return Integer.parseInt(text == null ? "0" : text.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? safeText(fallback) : value;
    }

    private static String escapeHtml(String text) {
        return safeText(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static OrderItem parseOrderLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        String[] parts = line.split(Pattern.quote(FILE_SEPARATOR), -1);
        if (parts.length < 6) {
            return null;
        }

        return new OrderItem(
                parts[0],
                parts[1],
                parts[2],
                safeInt(parts[3]),
                safeInt(parts[4]),
                parts[5]
        );
    }

    private static String formatAddedAtForDisplay(String addedAt) {
        LocalDateTime parsed = parseAddedAt(addedAt);
        if (parsed.equals(LocalDateTime.MIN)) {
            return safeText(addedAt).isBlank() ? "-" : safeText(addedAt);
        }
        return DISPLAY_DATE_FORMAT.format(parsed);
    }

    public static List<String> getAllSupplierOrders() {
        List<String> orders = new ArrayList<>();
        ensureOrderFileExists();
        try {
            orders.addAll(Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LogManager.getLogger(SupplierOrderGUI.class).error("Fehler beim Lesen der Lieferanten-Bestelldatei: {}",
                    e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Lesen der Lieferanten-Bestelldatei: " + e.getMessage());
        }
        return orders;
    }

    public static List<String> getAllSupplierOrdersForDisplay() {
        List<OrderItem> items = new ArrayList<>();
        for (String line : getAllSupplierOrders()) {
            OrderItem item = parseOrderLine(line);
            if (item != null) {
                items.add(item);
            }
        }

        items.sort((first, second) -> parseAddedAt(second.addedAt()).compareTo(parseAddedAt(first.addedAt())));

        List<String> displayLines = new ArrayList<>();
        if (items.isEmpty()) {
            displayLines.add("Keine Lieferanten-Bestellungen vorhanden.");
            return displayLines;
        }

        for (OrderItem item : items) {
            displayLines.add("--- " + safeText(item.articleNumber()) + " • " + defaultIfBlank(item.name(), "Unbenannter Artikel") + " ---");
            displayLines.add("Lieferant: " + defaultIfBlank(item.vendor(), "Kein Lieferant"));
            displayLines.add("Bestellmenge: " + item.quantity() + " | Lagerbestand: " + item.stock());
            displayLines.add("Hinzugefügt: " + formatAddedAtForDisplay(item.addedAt()));
        }

        return displayLines;
    }

    public static SupplierOrderGUI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SupplierOrderGUI();
        }
        return INSTANCE;
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        INSTANCE = null;
        super.dispose();
    }
}
