package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ThemeManager;
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

    public SupplierOrderGUI() {
        ThemeManager.getInstance().registerWindow(this);
        INSTANCE = this;

        // UI setup
        setTitle("Lieferantenbestellung");
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        // Main container with background
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Modern gradient header
        JPanel header = getHeaderPanel();

        // Title section with icon
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
        titleSection.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.TRUCK);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 38));
        iconLabel.setForeground(ThemeManager.getTextOnPrimaryColor());
        titleSection.add(iconLabel);

        JPanel titleTextPanel = new JPanel();
        titleTextPanel.setLayout(new BoxLayout(titleTextPanel, BoxLayout.Y_AXIS));
        titleTextPanel.setOpaque(false);

        JLabel title = new JLabel("Lieferanten-Bestellungen");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 28));
        title.setForeground(ThemeManager.getTextOnPrimaryColor());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Verwalten Sie Ihre Artikel-Nachbestellungen");
        subtitle.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        subtitle.setForeground(ThemeManager.withAlpha(ThemeManager.getTextOnPrimaryColor(), 230));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleTextPanel.add(title);
        titleTextPanel.add(Box.createVerticalStrut(5));
        titleTextPanel.add(subtitle);

        titleSection.add(titleTextPanel);
        header.add(titleSection, BorderLayout.WEST);

        mainContainer.add(header, BorderLayout.NORTH);

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

        JButton removeBtn = createPrimaryButton(UnicodeSymbols.TRASH + " Entfernen", ThemeManager.getErrorColor(), e -> removeSelected());
        JButton clearBtn = createPrimaryButton(UnicodeSymbols.BROOM + " Alle löschen", ThemeManager.getWarningColor(), e -> clearAll());
        JButton saveBtn = createPrimaryButton(UnicodeSymbols.FLOPPY + " Speichern", ThemeManager.getAccentColor(), e -> persist());
        JButton refreshBtn = createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren", e -> refreshTable());

        actionPanel.add(removeBtn);
        actionPanel.add(clearBtn);
        actionPanel.add(saveBtn);
        actionPanel.add(refreshBtn);

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

        loadFromFile();
        refreshTable();
    }

    private record OrderItem(String articleNumber, String name, String vendor, int quantity, int stock, String addedAt) {
    }

    private void loadFromFile() {
        ensureOrderFileExists();
        orderItems.clear();
        try {
            List<String> lines = Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                String[] parts = line.split("\\" + SEPARATOR, -1);
                if (parts.length >= 6) {
                    orderItems.add(new OrderItem(
                            parts[0],
                            parts[1],
                            parts[2],
                            safeInt(parts[3]),
                            safeInt(parts[4]),
                            parts[5]
                    ));
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Lieferanten-Bestelldatei: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Laden der Lieferanten-Bestelldatei: " + e.getMessage());
        }
    }

    private void persist() {
        ensureOrderFileExists();
        List<String> lines = new ArrayList<>();

        for (OrderItem item : orderItems) {
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
                    item.addedAt()
            ));

            VendorOrderLogging.getInstance().addLog(
                    "Bestellposten gespeichert: Artikelnummer " + item.articleNumber() + ", Menge: " + item.quantity()
            );
        }

        try {
            Files.write(ORDER_FILE, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.error("Fehler beim Speichern der Lieferanten-Bestellungen: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Speichern der Lieferanten-Bestellungen: " + e.getMessage());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        orderItems.stream()
                .sorted(Comparator.comparing(OrderItem::addedAt).reversed())
                .forEach(item -> tableModel.addRow(new Object[]{
                        item.articleNumber(),
                        item.name(),
                        item.vendor(),
                        item.quantity(),
                        item.stock(),
                        item.addedAt()
                }));
    }

    private void removeSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= orderItems.size()) return;

        orderItems.remove(modelRow);
        persist();
        refreshTable();
    }

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

    private void upsertItem(Article article, int quantity) {
        String articleNumber = article.getArticleNumber();
        Optional<OrderItem> existing = orderItems.stream()
                .filter(it -> Objects.equals(it.articleNumber(), articleNumber))
                .findFirst();
        if (existing.isPresent()) {
            OrderItem prev = existing.get();
            int idx = orderItems.indexOf(prev);
            orderItems.set(idx, new OrderItem(prev.articleNumber(), prev.name(), prev.vendor(), prev.quantity() + quantity, article.getStockQuantity(), prev.addedAt()));
        } else {
            String added = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            orderItems.add(new OrderItem(
                    articleNumber,
                    article.getName(),
                    article.getVendorName(),
                    quantity,
                    article.getStockQuantity(),
                    added
            ));
        }
        persist();
        refreshTable();
    }

    public static void addArticleToSupplierOrder(Article article, int quantity) {
        if (article == null || quantity <= 0) return;
        SwingUtilities.invokeLater(() -> {
            SupplierOrderGUI gui = getInstance();
            gui.upsertItem(article, quantity);
            VendorOrderLogging.getInstance().addLog("Neuer Artikel zur Bestellliste hinzugefügt.");
        });
    }

    private static void ensureOrderFileExists() {
        try {
            File parent = ORDER_FILE.toFile().getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            if (!Files.exists(ORDER_FILE)) {
                Files.createFile(ORDER_FILE);
            }
        } catch (Exception ignored) {
        }
    }

    private static int safeInt(String s) {
        try {
            return Integer.parseInt(s == null ? "0" : s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private JPanel createCardPanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(ThemeManager.getCardBackgroundColor());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return card;
    }

    public static List<String> getAllSupplierOrders() {
        List<String> orders = new ArrayList<>();
        ensureOrderFileExists();
        try {
            orders.addAll(Files.readAllLines(ORDER_FILE, StandardCharsets.UTF_8));
        } catch (Exception e) {
            LogManager.getLogger(SupplierOrderGUI.class).error("Fehler beim Lesen der Lieferanten-Bestelldatei: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Lesen der Lieferanten-Bestelldatei: " + e.getMessage());
        }
        return orders;
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
        super.dispose();
    }

    private static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, ThemeManager.getHeaderBackgroundColor(), getWidth(), 0, ThemeManager.getHeaderGradientColor());
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g2.setColor(new Color(0,0,0,30));
                g2.fillRoundRect(4, getHeight()-8, getWidth()-8, 8, 8, 8); // subtle shadow
                g2.dispose();
                super.paintComponent(g);
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 32, 18, 32));
        return headerPanel;
    }
}
