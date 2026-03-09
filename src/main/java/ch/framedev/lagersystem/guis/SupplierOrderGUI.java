package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ch.framedev.lagersystem.utils.JFrameUtils.*;

/**
 * GUI window that manages supplier reorders for articles.
 *
 * <p>
 * This view displays all pending supplier order items in a table and allows
 * users to add, remove, clear, and persist orders to a local text file.
 * The GUI integrates with {@link ArticleManager} to resolve stock values
 * and with {@link VendorOrderLogging} to record order actions.
 *
 * <p>
 * The window is theme-aware and automatically registers itself with
 * {@link ThemeManager} so colors update dynamically.
 */
@SuppressWarnings("DuplicatedCode")
public class SupplierOrderGUI extends JFrame {

    private static SupplierOrderGUI INSTANCE;

    private final Logger logger = LogManager.getLogger(SupplierOrderGUI.class);

    @SuppressWarnings("RegExpEmptyAlternationBranch")
    private static final String SEPARATOR = "|";
    private static final String[] COLUMNS = {
            UnicodeSymbols.NUMBERS + " Artikelnummer",
            UnicodeSymbols.ARTICLE_NAME + " Name",
            UnicodeSymbols.TRUCK + " Lieferant",
            UnicodeSymbols.PACKAGE + " Bestell Menge",
            UnicodeSymbols.COL_LAGER_MENGE,
            UnicodeSymbols.CLOCK + " Hinzugefügt"
    };

    private static final Path ORDER_FILE = new File(Main.getAppDataDir(), "supplier_orders.txt").toPath();
    private final List<OrderItem> orderItems = new ArrayList<>();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel statusLabel = new JLabel();

    /**
     * Creates and initializes the supplier order management window.
     *
     * <p>
     * Builds the UI layout, configures the table, loads stored orders from disk,
     * and refreshes the table contents.
     */
    public SupplierOrderGUI() {
        ThemeManager.getInstance().registerWindow(this);
        INSTANCE = this;

        // UI setup
        setTitle("Lieferantenbestellung");
        setSize(1100, 700);
        setMinimumSize(new Dimension(920, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Main container with background
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // ===== Top area (Header card – same style as VendorGUI) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JPanel headerWrapper = null;
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (!disableHeader) {
            // Header card
            JFrameUtils.RoundedPanel headerCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(),
                    20);
            headerCard.setLayout(new BorderLayout());
            headerCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel(UnicodeSymbols.TRUCK + " Lieferanten-Bestellungen");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Verwalten Sie Ihre Artikel-Nachbestellungen");
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

            headerWrapper = new JPanel(new BorderLayout());
            headerWrapper.setOpaque(false);
            headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            headerWrapper.add(headerCard, BorderLayout.CENTER);
        }

        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
        }
        mainContainer.add(topContainer, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        table.setFillsViewportHeight(true);
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());

        // Header styling
        table.getTableHeader().setBackground(ThemeManager.getTableHeaderBackground());
        table.getTableHeader().setForeground(ThemeManager.getTableHeaderForeground());
        table.getTableHeader().setFont(SettingsGUI.getFontByName(Font.BOLD, 18));

        // Table wrapper card
        JPanel tableCard = createCardPanel();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Bottom action bar card
        JPanel actionCard = createCardPanel();
        actionCard.setLayout(new BorderLayout());
        actionCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionPanel.setOpaque(false);

        styleStatusBadge(statusLabel);
        updateStatusLabel();

        JButton removeBtn = createPrimaryButton(UnicodeSymbols.TRASH + " Entfernen", ThemeManager.getErrorColor(),
                e -> removeSelected());
        JButton clearBtn = createPrimaryButton(UnicodeSymbols.BROOM + " Alle löschen", ThemeManager.getWarningColor(),
                e -> clearAll());
        JButton saveBtn = createPrimaryButton(UnicodeSymbols.FLOPPY + " Speichern", ThemeManager.getAccentColor(),
                e -> persist());
        JButton refreshBtn = createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren", e -> refreshTable());

        actionPanel.add(removeBtn);
        actionPanel.add(clearBtn);
        actionPanel.add(saveBtn);
        actionPanel.add(refreshBtn);

        actionCard.add(statusLabel, BorderLayout.WEST);
        actionCard.add(actionPanel, BorderLayout.EAST);

        JPanel centerWrapper = new JPanel(new BorderLayout(0, 12));
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(16, 18, 18, 18));
        centerWrapper.add(tableCard, BorderLayout.CENTER);
        centerWrapper.add(actionCard, BorderLayout.SOUTH);

        mainContainer.add(centerWrapper, BorderLayout.CENTER);
        setContentPane(mainContainer);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateStatusLabel();
            }
        });

        loadFromFile();
        refreshTable();
    }

    /**
     * Immutable data holder representing a single supplier order entry.
     *
     * @param articleNumber article identifier
     * @param name          article display name
     * @param vendor        supplier name
     * @param quantity      requested quantity
     * @param stock         current stock level
     * @param addedAt       timestamp when the item was added
     */
    private record OrderItem(String articleNumber, String name, String vendor, int quantity, int stock,
            String addedAt) {
    }

    /**
     * Loads supplier order entries from the persistent order file into memory.
     * Invalid or malformed lines are ignored.
     */
    private void loadFromFile() {
        ensureOrderFileExists();
        orderItems.clear();
        try {
            List<String> lines = Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank())
                    continue;
                String[] parts = line.split("\\" + SEPARATOR, -1);
                if (parts.length >= 6) {
                    orderItems.add(new OrderItem(
                            parts[0],
                            parts[1],
                            parts[2],
                            safeInt(parts[3]),
                            safeInt(parts[4]),
                            parts[5]));
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Lieferanten-Bestelldatei: {}", e.getMessage(), e);
            Main.logUtils
                    .addLog("[SUPPLIER ORDER|ERROR] Fehler beim Laden der Lieferanten-Bestelldatei: " + e.getMessage());
        }
    }

    /**
     * Writes all current supplier order entries to disk.
     * Stock values are refreshed from {@link ArticleManager} before saving.
     */
    private void persist() {
        ensureOrderFileExists();
        List<String> lines = new ArrayList<>();

        for (OrderItem item : orderItems) {
            if (item == null)
                throw new IllegalArgumentException("item must not be null");
            int stock = item.stock();
            Article a = ArticleManager.getInstance().getArticleByNumber(item.articleNumber());
            if (a != null) {
                stock = a.getStockQuantity();
            }

            lines.add(String.join(SEPARATOR,
                    item.articleNumber(),
                    item.name(),
                    item.vendor(),
                    String.valueOf(item.quantity()),
                    String.valueOf(stock),
                    item.addedAt()));

            VendorOrderLogging.getInstance().addLog(
                    "Bestellposten gespeichert: Artikelnummer " + item.articleNumber() + ", Menge: " + item.quantity());
        }

        try {
            Files.write(ORDER_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Fehler beim Speichern der Lieferanten-Bestellungen: {}", e.getMessage(), e);
            Main.logUtils.addLog(
                    "[SUPPLIER ORDER|ERROR] Fehler beim Speichern der Lieferanten-Bestellungen: " + e.getMessage());
        }
    }

    /**
     * Rebuilds the table model from the in-memory order list and sorts
     * entries by newest first.
     */
    private void refreshTable() {
        tableModel.setRowCount(0);
        orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::addedAt).reversed())
                .forEach(item -> tableModel.addRow(new Object[] {
                        item.articleNumber(),
                        item.name(),
                        item.vendor(),
                        item.quantity(),
                        item.stock(),
                        item.addedAt()
                }));
        updateStatusLabel();
    }

    private void updateStatusLabel() {
        int total = tableModel.getRowCount();
        int selected = table.getSelectedRow() >= 0 ? 1 : 0;
        statusLabel.setText("Einträge: " + total + (selected > 0 ? " • Auswahl: 1" : ""));
    }

    private void styleStatusBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    /**
     * Removes the currently selected order row from the list and persists the
     * change.
     */
    private void removeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0)
            return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= orderItems.size())
            return;

        orderItems.remove(modelRow);
        persist();
        refreshTable();
    }

    /**
     * Clears all supplier orders after user confirmation and persists the result.
     */
    private void clearAll() {
        if (JOptionPane.showConfirmDialog(this,
                "Alle Lieferanten-Bestellposten löschen?",
                "Bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                Main.iconSmall) == JOptionPane.YES_OPTION) {
            orderItems.clear();
            persist();
            refreshTable();
        }
    }

    /**
     * Adds a new supplier order entry or increases the quantity if it already
     * exists.
     *
     * @param article  article to order
     * @param quantity amount to add
     */
    private void upsertItem(Article article, int quantity) {
        if (article == null)
            throw new IllegalArgumentException("article must not be null");
        String articleNumber = article.getArticleNumber();
        Optional<OrderItem> existing = orderItems.stream()
                .filter(it -> Objects.equals(it.articleNumber(), articleNumber))
                .findFirst();
        if (existing.isPresent()) {
            OrderItem prev = existing.get();
            int idx = orderItems.indexOf(prev);
            orderItems.set(idx, new OrderItem(prev.articleNumber(), prev.name(), prev.vendor(),
                    prev.quantity() + quantity, article.getStockQuantity(), prev.addedAt()));
        } else {
            String added = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            orderItems.add(new OrderItem(
                    articleNumber,
                    article.getName(),
                    article.getVendorName(),
                    quantity,
                    article.getStockQuantity(),
                    added));
        }
        persist();
        refreshTable();
    }

    /**
     * Adds an article to the supplier order list using the singleton GUI instance.
     * Executed on the Swing Event Dispatch Thread.
     *
     * @param article  article to add
     * @param quantity quantity to order
     */
    public static void addArticleToSupplierOrder(Article article, int quantity) {
        if (article == null || quantity <= 0)
            return;
        SwingUtilities.invokeLater(() -> {
            SupplierOrderGUI gui = getInstance();
            gui.upsertItem(article, quantity);
            VendorOrderLogging.getInstance().addLog("Neuer Artikel zur Bestellliste hinzugefügt.");
        });
    }

    /**
     * Ensures the supplier order file and its parent directory exist.
     */
    private static void ensureOrderFileExists() {
        try {
            File parent = ORDER_FILE.toFile().getParentFile();
            if (parent != null && !parent.exists()) {
                // noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            if (!Files.exists(ORDER_FILE)) {
                Files.createFile(ORDER_FILE);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Parses an integer safely.
     *
     * @param s string to parse
     * @return parsed value or {@code 0} if parsing fails
     */
    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s == null ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Creates a themed card-style panel used for UI sections.
     *
     * @return styled panel container
     */
    private JPanel createCardPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(ThemeManager.getCardBackgroundColor());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        return card;
    }

    /**
     * Returns all persisted supplier order lines.
     *
     * @return list of stored order entries
     */
    public static List<String> getAllSupplierOrders() {
        List<String> orders = new ArrayList<>();
        ensureOrderFileExists();
        try {
            orders.addAll(Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LogManager.getLogger(SupplierOrderGUI.class).error("Fehler beim Lesen der Lieferanten-Bestelldatei: {}",
                    e.getMessage(), e);
            Main.logUtils
                    .addLog("[SUPPLIER ORDER|ERROR] Fehler beim Lesen der Lieferanten-Bestelldatei: " + e.getMessage());
        }
        return orders;
    }

    /**
     * Returns the singleton instance of the supplier order window.
     * Creates it if necessary.
     *
     * @return GUI instance
     */
    public static SupplierOrderGUI getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SupplierOrderGUI();
        }
        return INSTANCE;
    }

    /**
     * Unregisters the window from the theme manager and disposes it.
     */
    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        INSTANCE = null;
        super.dispose();
    }

}
