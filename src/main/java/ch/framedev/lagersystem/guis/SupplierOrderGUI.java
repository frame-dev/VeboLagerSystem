package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import ch.framedev.lagersystem.utils.VendorOrderLogging;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SupplierOrderGUI extends JFrame {

    private static SupplierOrderGUI INSTANCE;

    private final Logger logger = LogManager.getLogger(SupplierOrderGUI.class);

    private static final String SEPARATOR = "|";
    private static final String[] COLUMNS = {
        UnicodeSymbols.NUMBERS + " Artikelnummer",
        UnicodeSymbols.ARTICLE_NAME + " Name",
        UnicodeSymbols.TRUCK + " Lieferant",
        UnicodeSymbols.PACKAGE + " Bestell Menge",
        UnicodeSymbols.COL_LAGER_MENGE + " Lager Menge",
        UnicodeSymbols.CLOCK + " Hinzugefügt"
    };

    private static final File orderFile = new File(Main.getAppDataDir(), "supplier_orders.txt");
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
        GradientPanel header = getGradientPanel();

        // Title section with icon
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
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
        Color subtitleColor = ThemeManager.isDarkMode()
                ? new Color(220, 230, 240, 230)
                : new Color(255, 255, 255, 240);
        subtitle.setForeground(subtitleColor);
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
        JTableHeader headerTable = table.getTableHeader();
        headerTable.setBackground(ThemeManager.getTableHeaderBackground());
        headerTable.setForeground(ThemeManager.getTableHeaderForeground());
        headerTable.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor()));
        add(scrollPane, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actionPanel.setBackground(ThemeManager.getBackgroundColor());

        JButton removeBtn = createButton(UnicodeSymbols.TRASH + " Entfernen", new Color(220, 53, 69), e -> removeSelected());
        JButton clearBtn = createButton(UnicodeSymbols.BROOM + " Alle löschen", new Color(243, 156, 18), e -> clearAll());
        JButton saveBtn = createButton(UnicodeSymbols.FLOPPY + " Speichern", new Color(52, 152, 219), e -> persist());
        JButton refreshBtn = createButton(UnicodeSymbols.REFRESH + " Aktualisieren", new Color(40, 167, 69), e -> refreshTable());

        actionPanel.add(removeBtn);
        actionPanel.add(clearBtn);
        actionPanel.add(saveBtn);
        actionPanel.add(refreshBtn);
        add(actionPanel, BorderLayout.SOUTH);

        loadFromFile();
        refreshTable();
    }

    private static GradientPanel getGradientPanel() {
        GradientPanel header = new GradientPanel(
                ThemeManager.getHeaderBackgroundColor(),
                ThemeManager.isDarkMode()
                        ? new Color(35, 47, 62)
                        : new Color(41, 128, 185)
        );
        header.setLayout(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 100));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0, 0, 0, 30)),
                BorderFactory.createEmptyBorder(25, 40, 25, 40)
        ));
        return header;
    }

    private JButton createButton(String text, Color color, ActionListener action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action);
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(color.darker()); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(color); }
        });
        return btn;
    }

    private record OrderItem(String articleNumber, String name, String vendor, int quantity, int stock, String addedAt) {
    }

    private void loadFromFile() {
        if (!orderFile.exists()) {
            try {
                if(!orderFile.createNewFile()) {
                    logger.error("Konnte die Lieferanten-Bestelldatei nicht erstellen: {}", orderFile.getAbsolutePath());
                    Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler: Konnte die Lieferanten-Bestelldatei nicht erstellen: " + orderFile.getAbsolutePath());
                }
            } catch (Exception ignored) {}
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(orderFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\" + SEPARATOR);
                if (parts.length >= 5) {
                    orderItems.add(new OrderItem(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), parts[5]));
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Lieferanten-Bestelldatei: {}", e.getMessage(), e);
            Main.logUtils.addLog("[SUPPLIER ORDER|ERROR] Fehler beim Laden der Lieferanten-Bestelldatei: " + e.getMessage());
        }
    }

    private void persist() {
        try (FileWriter writer = new FileWriter(orderFile, false)) {
            for (OrderItem item : orderItems) {
                writer.write(String.join(SEPARATOR,
                        item.articleNumber(),
                        item.name(),
                        item.vendor(),
                        String.valueOf(item.quantity()),
                        String.valueOf(ArticleManager.getInstance().getArticleByNumber(item.articleNumber()).getStockQuantity()),
                        item.addedAt()) + System.lineSeparator());
                VendorOrderLogging.getInstance().addLog("Bestellposten gespeichert: Artikelnummer " + item.articleNumber() + ", Menge: " + item.quantity());
            }
        } catch (Exception e) {
            logger.error("Fehler beim Laden der Lieferanten: {}", e.getMessage(), e);
            Main.logUtils.addLog("Fehler beim laden der Lieferanten: " + e.getMessage());

        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (OrderItem item : orderItems) {
            tableModel.addRow(new Object[]{item.articleNumber(), item.name(), item.vendor(), item.quantity(), ArticleManager.getInstance().getArticleByNumber(item.articleNumber()).getStockQuantity(), item.addedAt()});
        }
    }

    private void removeSelected() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) return;
        // remove from bottom to top to keep indices valid
        for (int i = rows.length - 1; i >= 0; i--) {
            orderItems.remove(rows[i]);
        }
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

    public static List<String> getAllSupplierOrders() {
        List<String> orders = new ArrayList<>();
        if (!orderFile.exists()) {
            return orders;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(orderFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                orders.add(line);
            }
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

    /**
     * Gradient panel for modern header design
     */
    private static class GradientPanel extends JPanel {
        private final Color color1;
        private final Color color2;

        GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
