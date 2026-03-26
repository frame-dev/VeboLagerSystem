package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.OrderExport;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static ch.framedev.lagersystem.utils.JFrameUtils.*;

/**
 * The OrderGUI class provides a graphical user interface for managing orders in
 * the inventory system. It allows users to view, filter, create, edit, delete,
 * and complete orders. The GUI displays a list of orders in a table format,
 * with options to search and filter the orders based on various criteria. Users
 * can also access detailed information about each order and perform actions
 * such as editing or deleting orders directly from the interface. The class
 * implements caching for order data to improve performance and reduce database
 * load, while ensuring that the displayed information is up-to-date when
 * necessary.
 *
 * @author framedev
 */
@SuppressWarnings("DuplicatedCode")
public class OrderGUI extends JFrame {

    private final JTable orderTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[] { 150, 180, 150, 150, 120, 120, 120 };
    private final JLabel orderCountLabel;
    private final JComboBox<String> filterComboBox;
    private final JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;
    private SwingWorker<List<Order>, Void> orderLoadWorker;
    private int orderLoadGeneration = 0;

    // ---- Order cache (GUI-level) ----------------------------------------------
    private static final long ORDER_CACHE_TTL_MS = 30_000; // 30s GUI cache

    private volatile List<Order> ordersCache = null;
    private volatile long ordersCacheTime = 0L;

    // Fast lookup by ID for edit/details without extra DB hits
    private final ConcurrentHashMap<String, Order> orderByIdCache = new ConcurrentHashMap<>();

    /**
     * Initializes the OrderGUI by setting up the main window, including the header,
     * toolbar, order table, and search bar. The GUI allows users to view, filter,
     * create, edit, delete, and complete orders. It also includes a search
     * functionality to filter orders based on various criteria. The order data is
     * loaded from the OrderManager and displayed in a JTable with custom styling
     * and interactions. The GUI is designed to be responsive and user-friendly,
     * with tooltips and confirmation dialogs for critical actions.
     */
    public OrderGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        setTitle("Bestellungen Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setMinimumSize(new Dimension(980, 560));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Top area (header + toolbar)
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        JFrameUtils.RoundedPanel headerCard = null;
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (!disableHeader) {

            // Header card
            headerCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
            headerCard.setLayout(new BorderLayout());
            headerCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));

            JLabel titleLabel = new JLabel(UnicodeSymbols.PACKAGE + " Bestellungen Verwaltung");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Bestellungen verwalten, filtern und exportieren");
            subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
            subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

            JPanel titleBox = new JPanel();
            titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
            titleBox.setOpaque(false);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            titleBox.add(titleLabel);
            titleBox.add(Box.createVerticalStrut(4));
            titleBox.add(subtitleLabel);

            headerCard.add(titleBox, BorderLayout.WEST);
        }

        // Toolbar card
        JFrameUtils.RoundedPanel toolbarCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbarCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        filterComboBox = new JComboBox<>(new String[] {
                "Alle Bestellungen", "Abgeschlossene Bestellungen", "Offene Bestellungen"
        });
        styleComboBox(filterComboBox);

        JButton newOrderButton = createPrimaryButton(UnicodeSymbols.HEAVY_PLUS + " Neue Bestellung");
        newOrderButton.setToolTipText("Erstellt eine neue Bestellung");
        JButton editOrderButton = createSecondaryButton(UnicodeSymbols.CODE + " Bearbeiten");
        editOrderButton.setToolTipText("Bearbeitet die ausgewählte Bestellung");
        JButton deleteOrderButton = createDangerButton(UnicodeSymbols.TRASH + " Löschen");
        deleteOrderButton.setToolTipText("Löscht die ausgewählte Bestellung");
        JButton completeOrderButton = createSecondaryButton(UnicodeSymbols.CHECKMARK + " Abschließen");
        completeOrderButton.setToolTipText("Markiert die ausgewählte Bestellung als abgeschlossen");
        JButton refreshButton = createSecondaryButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.setToolTipText("Bestellungsliste aktualisieren");

        toolbarCard.add(filterComboBox);
        toolbarCard.add(newOrderButton);
        toolbarCard.add(editOrderButton);
        toolbarCard.add(deleteOrderButton);
        toolbarCard.add(completeOrderButton);
        toolbarCard.add(refreshButton);

        if (headerCard != null) {
            headerCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            toolbarCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            topPanel.add(headerCard);
            topPanel.add(Box.createVerticalStrut(10));
        }
        topPanel.add(toolbarCard);

        add(topPanel, BorderLayout.NORTH);

        // Main card with table
        JFrameUtils.RoundedPanel card = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Table setup
        orderTable = new JTable();
        initializeOrderTable();
        tableScrollPane = new JScrollPane(orderTable);
        tableScrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        tableScrollPane.getViewport().setBackground(ThemeManager.getTableRowEvenColor());
        card.add(tableScrollPane, BorderLayout.CENTER);

        // place card in center with padding
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(card, gbc);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom search bar (card)
        JFrameUtils.RoundedPanel searchCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        orderCountLabel = new JLabel("Anzahl Bestellungen: 0");
        orderCountLabel.setForeground(ThemeManager.getTextPrimaryColor());
        orderCountLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Bestell-ID, Empfänger oder Abteilung):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        searchField = new JTextField(26);
        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JPanel searchInputRow = new JPanel(new BorderLayout(10, 0));
        searchInputRow.setOpaque(false);
        searchInputRow.add(searchLabel, BorderLayout.WEST);
        searchInputRow.add(searchField, BorderLayout.CENTER);

        JButton searchBtn = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Bestellungen filtern");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");
        clearBtn.setToolTipText("Suchfeld leeren");

        JPanel topRow = new JPanel(new BorderLayout(12, 8));
        topRow.setOpaque(false);
        topRow.add(orderCountLabel, BorderLayout.WEST);
        topRow.add(searchInputRow, BorderLayout.CENTER);

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionRow.setOpaque(false);
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

        // load orders
        loadOrders();

        // Setup interactions
        setupTableInteractions();

        // Wire actions
        newOrderButton.addActionListener(e -> openCreateOrderGui());

        editOrderButton.addActionListener(e -> {
            Vector<Object> rowData = getSelectedOrderData();
            if (rowData == null) {
                new MessageDialog()
                        .setTitle("Keine Bestellung ausgewählt")
                        .setMessage("Bitte wählen Sie eine Bestellung zum Bearbeiten aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            String orderId = (String) rowData.getFirst();
            Order order = getOrderCached(orderId);
            if (order != null) {
                openEditOrderGui(order);
            }
        });

        deleteOrderButton.addActionListener(e -> deleteSelectedOrder());

        completeOrderButton.addActionListener(e -> openCompleteOrderGui());

        refreshButton.addActionListener(e -> {
            invalidateOrdersCache();
            loadOrders(true);
            updateOrderCount();
        });

        // search logic using TableRowSorter
        sorter = new TableRowSorter<>((DefaultTableModel) orderTable.getModel());
        orderTable.setRowSorter(sorter);

        Runnable doSearch = this::applyCombinedFilter;

        // Live filter while typing
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                doSearch.run();
            }
        });

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            applyCombinedFilter();
        });
        // ESC clears
        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    applyCombinedFilter();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED);

        filterComboBox.addActionListener(e -> applyCombinedFilter());

        // auto resize logic
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

    @Override
    public void dispose() {
        if (orderLoadWorker != null && !orderLoadWorker.isDone()) {
            orderLoadWorker.cancel(true);
        }
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void loadOrders() {
        loadOrders(false);
    }

    @SuppressWarnings("SameParameterValue")
    private void loadOrders(boolean forceReload) {
        String selectedOrderId = getSelectedOrderId();
        int loadGeneration = ++orderLoadGeneration;
        if (orderLoadWorker != null && !orderLoadWorker.isDone()) {
            orderLoadWorker.cancel(true);
        }
        orderCountLabel.setText("Bestellungen werden geladen...");
        orderLoadWorker = new SwingWorker<>() {
            @Override
            protected List<Order> doInBackground() {
                return getOrdersCached(forceReload);
            }

            @Override
            protected void done() {
                if (isCancelled() || loadGeneration != orderLoadGeneration) {
                    return;
                }
                try {
                    rebuildTable(get(), selectedOrderId);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    updateOrderCount();
                } catch (java.util.concurrent.ExecutionException ex) {
                    updateOrderCount();
                }
            }
        };
        orderLoadWorker.execute();
    }

    private void updateOrderCount() {
        if (orderCountLabel == null)
            return;

        int count;
        if (orderTable.getRowSorter() != null) {
            count = orderTable.getRowSorter().getViewRowCount();
        } else {
            count = orderTable.getRowCount();
        }
        orderCountLabel.setText("Anzahl Bestellungen: " + count);
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        edit.setToolTipText("Bearbeitet die ausgewählte Bestellung");
        JMenuItem del = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");
        del.setToolTipText("Löscht die ausgewählte Bestellung");
        JMenuItem complete = new JMenuItem(UnicodeSymbols.CHECKMARK + " Abschließen");
        complete.setToolTipText("Markiert die ausgewählte Bestellung als abgeschlossen");
        popup.add(edit);
        popup.add(del);
        popup.add(complete);

        edit.addActionListener(e -> {
            Vector<Object> rowData = getSelectedOrderData();
            if (rowData == null) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie eine Bestellung zum Bearbeiten aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }
            Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
            if (order != null) {
                openEditOrderGui(order);
            }
        });

        del.addActionListener(e -> deleteSelectedOrder());

        complete.addActionListener(e -> openCompleteOrderGui());

        orderTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Vector<Object> rowData = getSelectedOrderData();
                    if (rowData == null) {
                        new MessageDialog()
                                .setTitle("Keine Auswahl")
                                .setMessage("Bitte wählen Sie eine Bestellung zum Anzeigen aus.")
                                .setMessageType(JOptionPane.WARNING_MESSAGE)
                                .display();
                        return;
                    }
                    String orderId = (String) rowData.getFirst();
                    Order order = getOrderCached(orderId);
                    if (order != null) {
                        showOrderDetailsDialog(order);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShow(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShow(e);
            }

            private void maybeShow(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = orderTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        orderTable.setRowSelectionInterval(row, row);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private Vector<Object> getSelectedOrderData() {
        int sel = orderTable.getSelectedRow();
        if (sel == -1)
            return null;
        int modelRow = orderTable.convertRowIndexToModel(sel);

        Vector<Object> rowData = new Vector<>();
        for (int i = 0; i < orderTable.getColumnCount(); i++) {
            rowData.add(orderTable.getModel().getValueAt(modelRow, i));
        }
        return rowData;
    }

    private void initializeOrderTable() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public int getColumnCount() {
                return 7;
            }

            @Override
            public String getColumnName(int col) {
                return switch (col) {
                    case 0 -> UnicodeSymbols.ID + " Bestell-ID";
                    case 1 -> UnicodeSymbols.PERSON + " Empfänger";
                    case 2 -> UnicodeSymbols.PERSON + " Absender";
                    case 3 -> UnicodeSymbols.DEPARTMENT + " Abteilung";
                    case 4 -> UnicodeSymbols.CALENDAR + " Datum";
                    case 5 -> UnicodeSymbols.PACKAGE + " Artikel";
                    case 6 -> UnicodeSymbols.STAR_FILLED + " Status";
                    default -> "";
                };
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        orderTable.setModel(model);

        // grid
        orderTable.setShowGrid(true);
        orderTable.setShowHorizontalLines(true);
        orderTable.setShowVerticalLines(true);
        orderTable.setGridColor(ThemeManager.getBorderColor());
        orderTable.setIntercellSpacing(new Dimension(1, 1));
        orderTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));

        // visuals
        orderTable.setRowHeight(28);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        orderTable.setSelectionForeground(ThemeManager.getSelectionForegroundColor());

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        // Status column renderer with color coding (keep theme-aware background)
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                        column);
                c.setHorizontalAlignment(SwingConstants.CENTER);

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                    return c;
                }

                // base alternating background
                Color baseBg = (row % 2 == 0) ? ThemeManager.getTableRowEvenColor()
                        : ThemeManager.getTableRowOddColor();
                c.setBackground(baseBg);
                c.setForeground(ThemeManager.getTextPrimaryColor());

                if (value != null) {
                    String status = value.toString();
                    if ("Abgeschlossen".equals(status)) {
                        c.setBackground(ThemeManager.withAlpha(ThemeManager.getSuccessColor(), 60));
                        c.setForeground(ThemeManager.adjustColor(ThemeManager.getSuccessColor(), -80));
                    } else if ("In Bearbeitung".equals(status)) {
                        c.setBackground(ThemeManager.withAlpha(ThemeManager.getWarningColor(), 60));
                        c.setForeground(ThemeManager.adjustColor(ThemeManager.getWarningColor(), -80));
                    }
                }
                return c;
            }
        };

        TableColumnModel tcm = orderTable.getColumnModel();
        if (tcm.getColumnCount() > 0)
            tcm.getColumn(0).setCellRenderer(center);
        if (tcm.getColumnCount() > 6)
            tcm.getColumn(6).setCellRenderer(statusRenderer);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

        // Alternating row colors for readability (subtle) - DO NOT override status
        // renderer
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(
                            row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                    c.setForeground(ThemeManager.getTextPrimaryColor());
                }
                return c;
            }
        };
        orderTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Header styling
        JTableHeader header = orderTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackground());
        header.setForeground(ThemeManager.getTableHeaderForeground());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        header.setReorderingAllowed(true);

        // keep consistent header height
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 34));
    }

    private void adjustColumnWidths() {
        JFrameUtils.adjustColumnWidths(orderTable, tableScrollPane, baseColumnWidths);
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    // -------------------- UI helpers --------------------

    private void styleTextField(JTextField tf) {
        JFrameUtils.styleTextField(tf);
    }

    /**
     * Adds hover and press effects to a button by changing its background color.
     * The button's original background color should be set to 'base' for this to
     * work properly.
     *
     * @param button  The JButton to which the hover effects should be added.
     * @param base    The base background color of the button when it is in its
     *                normal state.
     * @param hover   The background color of the button when the mouse is hovering
     *                over it.
     * @param pressed The background color of the button when it is being pressed
     *                (mouse click).
     */
    public static void addHoverEffects(JButton button, Color base, Color hover, Color pressed) {
        if (button == null)
            return;
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hover : base);
            }
        });
    }

    private void showOrderDetailsDialog(Order order) {
        if (order == null)
            return;
        JDialog detailsDialog = new JDialog(this, "Bestelldetails - " + order.getOrderId(), true);
        detailsDialog.setSize(600, 500);
        detailsDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(ThemeManager.getCardBackgroundColor());

        // Build HTML content
        List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
        double totalPrice = 0.0;
        StringBuilder articlesHtml = new StringBuilder();

        for (Article a : articles) {
            if (a == null)
                continue;
            int quantity = order.getOrderedArticles().getOrDefault(a.getArticleNumber(), 0);
            if (quantity == 0) {
                quantity = order.getOrderedArticles().getOrDefault(a.getName(), 0);
            }
            String filling = order.getArticleFilling(a.getArticleNumber());
            double unitPrice = ArticleUtils.resolveEffectiveSellPrice(a, filling);
            double lineTotal = quantity * unitPrice;
            totalPrice += lineTotal;
            String articleName = order.formatArticleLabel(a);

            articlesHtml.append("<tr>")
                    .append("<td>").append(escapeHtml(a.getArticleNumber())).append("</td>")
                    .append("<td>").append(escapeHtml(articleName)).append("</td>")
                    .append("<td align='right'>").append(quantity).append("</td>")
                    .append("<td align='right'>").append(String.format(java.util.Locale.ROOT, "%.2f", unitPrice)).append(" CHF</td>")
                    .append("<td align='right'><b>").append(String.format(java.util.Locale.ROOT, "%.2f", lineTotal)).append(" CHF</b></td>")
                    .append("</tr>");
        }

        JScrollPane scrollPane = getJScrollPane(order, articlesHtml, totalPrice);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnPanel.setOpaque(false);

        JButton exportBtn = createRoundedButton(UnicodeSymbols.DOWNLOAD + " PDF Exportieren");
        exportBtn.setToolTipText("Exportiert die Bestelldetails als PDF-Datei");
        exportBtn.addActionListener(ev -> createPDFExport(order));

        JButton closeBtn = createRoundedButton(UnicodeSymbols.CLOSE + " Schließen");
        closeBtn.setToolTipText("Schließt das Fenster");
        closeBtn.addActionListener(ev -> detailsDialog.dispose());

        btnPanel.add(exportBtn);
        btnPanel.add(closeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        detailsDialog.add(panel);
        detailsDialog.setVisible(true);
    }

    private void createPDFExport(Order order) {
        if (order == null)
            return;
        OrderExport.createPDFExport(this, order);
    }

    private static JScrollPane getJScrollPane(Order order, StringBuilder articlesHtml, double totalPrice) {
        String htmlContent = "<html><body style='font-family: Arial, sans-serif; padding: 10px;'>" +
                "<h2 style='color: #1e3a5f; margin-bottom: 20px;'>Bestelldetails</h2>" +
                "<table style='width: 100%; margin-bottom: 20px;'>" +
                "<tr><td style='width: 180px;'><b>Bestell-ID:</b></td><td>" + escapeHtml(order.getOrderId()) + "</td></tr>" +
                "<tr><td><b>Status:</b></td><td><span style='color: " +
                ("Abgeschlossen".equals(order.getStatus()) ? "#1b5e20" : "#e65100") + "; font-weight: bold;'>" +
                escapeHtml(order.getStatus() != null ? order.getStatus() : "In Bearbeitung") + "</span></td></tr>" +
                "<tr><td><b>Datum:</b></td><td>" + escapeHtml(order.getOrderDate()) + "</td></tr>" +
                "<tr><td><b>Abteilung:</b></td><td>" + escapeHtml(order.getDepartment()) + "</td></tr>" +
                "<tr><td><b>Empfänger:</b></td><td>" + escapeHtml(order.getReceiverName()) + "</td></tr>" +
                "<tr><td><b>Empfänger Konto:</b></td><td>" + escapeHtml(order.getReceiverKontoNumber()) + "</td></tr>" +
                "<tr><td><b>Absender:</b></td><td>" + escapeHtml(order.getSenderName()) + "</td></tr>" +
                "<tr><td><b>Absender Konto:</b></td><td>" + escapeHtml(order.getSenderKontoNumber()) + "</td></tr>" +
                "</table>" +
                "<h3 style='color: #1e3a5f; margin-top: 20px; margin-bottom: 10px;'>Bestellte Artikel</h3>" +
                "<table border='1' cellpadding='8' cellspacing='0' style='width: 100%; border-collapse: collapse; border-color: #ddd;'>"
                +
                "<tr style='background-color: #3e5462; color: white;'>" +
                "<th>Art.-Nr.</th><th>Name</th><th>Menge</th><th>Einzelpreis</th><th>Gesamt</th>" +
                "</tr>" +
                articlesHtml +
                "<tr style='background-color: #f0f0f0; font-weight: bold;'>" +
                "<td colspan='4' align='right'>Gesamtpreis:</td>" +
                "<td align='right' style='color: #1e3a5f; font-size: 16px;'>" + String.format(java.util.Locale.ROOT, "%.2f", totalPrice)
                + " CHF</td>" +
                "</tr>" +
                "</table>" +
                "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(Color.WHITE);
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        return new JScrollPane(editorPane);
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private boolean isOrdersCacheValid() {
        return ordersCache != null && (System.currentTimeMillis() - ordersCacheTime) < ORDER_CACHE_TTL_MS;
    }

    private void invalidateOrdersCache() {
        ordersCache = null;
        ordersCacheTime = 0L;
        orderByIdCache.clear();
    }

    private List<Order> getOrdersCached(boolean forceReload) {
        if (!forceReload && isOrdersCacheValid()) {
            return ordersCache;
        }

        List<Order> fresh = OrderManager.getInstance().getOrders();
        if (fresh == null) {
            // Keep old cache if DB fails (optional behavior)
            return ordersCache != null ? ordersCache : java.util.Collections.emptyList();
        }

        ordersCache = new ArrayList<>(fresh);
        ordersCacheTime = System.currentTimeMillis();

        orderByIdCache.clear();
        for (Order o : ordersCache) {
            if (o != null && o.getOrderId() != null) {
                orderByIdCache.put(o.getOrderId(), o);
            }
        }
        return ordersCache;
    }

    private Order getOrderCached(String orderId) {
        if (orderId == null)
            return null;

        Order cached = orderByIdCache.get(orderId);
        if (cached != null)
            return cached;

        // If list cache is valid but map entry missing, rebuild map from list (cheap)
        if (isOrdersCacheValid() && ordersCache != null) {
            for (Order o : ordersCache) {
                if (o != null && orderId.equals(o.getOrderId())) {
                    orderByIdCache.put(orderId, o);
                    return o;
                }
            }
        }

        // Fallback: fetch single order from manager and cache it
        Order fresh = OrderManager.getInstance().getOrder(orderId);
        if (fresh != null)
            orderByIdCache.put(orderId, fresh);
        return fresh;
    }

    private void openCreateOrderGui() {
        NewOrderGUI newOrderGUI = new NewOrderGUI();
        newOrderGUI.addPropertyChangeListener("orderCreated", evt -> {
            if (evt.getNewValue() instanceof Order createdOrder) {
                upsertOrderRow(createdOrder, true);
            }
        });
        newOrderGUI.display();
    }

    private void openEditOrderGui(Order order) {
        if (order == null) {
            return;
        }
        EditOrderGUI editGui = new EditOrderGUI(order);
        editGui.addPropertyChangeListener("orderUpdated", evt -> {
            if (evt.getNewValue() instanceof Order updatedOrder) {
                upsertOrderRow(updatedOrder, true);
            }
        });
        editGui.display();
    }

    private void openCompleteOrderGui() {
        CompleteOrderGUI completeOrderGUI = new CompleteOrderGUI();
        completeOrderGUI.addPropertyChangeListener("orderCompleted", evt -> {
            if (evt.getNewValue() instanceof Order completedOrder) {
                upsertOrderRow(completedOrder, false);
            }
        });
        completeOrderGUI.display();
    }

    private void deleteSelectedOrder() {
        int sel = orderTable.getSelectedRow();
        if (sel == -1) {
            new MessageDialog()
                    .setTitle("Keine Bestellung ausgewählt")
                    .setMessage("Bitte wählen Sie eine Bestellung zum Löschen aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        int modelRow = orderTable.convertRowIndexToModel(sel);
        String orderId = (String) orderTable.getModel().getValueAt(modelRow, 0);

        int confirm = new MessageDialog().setTitle("Löschen")
                .setMessage("Möchten Sie diese Bestellung wirklich löschen?")
                .setOptionType(JOptionPane.YES_NO_OPTION)
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .displayWithOptions();

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (OrderManager.getInstance().deleteOrder(orderId)) {
            removeOrderRow(orderId);
            new MessageDialog()
                    .setTitle("Erfolg")
                    .setMessage("Bestellung erfolgreich gelöscht.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
            Main.logUtils.addLog(orderId + " wurde gelöscht.");
            return;
        }

        new MessageDialog()
                .setTitle("Fehler")
                .setMessage("Löschen fehlgeschlagen.")
                .setMessageType(JOptionPane.ERROR_MESSAGE)
                .display();
        Main.logUtils.addLog(orderId + " konnte nicht gelöscht werden.");
    }

    private void rebuildTable(List<Order> orders, String selectedOrderId) {
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        model.setRowCount(0);

        for (Order order : orders) {
            if (order == null) {
                continue;
            }
            model.addRow(toRowData(order));
        }

        applyCombinedFilter();
        selectOrderById(selectedOrderId);
    }

    private Object[] toRowData(Order order) {
        return new Object[] {
                order.getOrderId(),
                order.getReceiverName(),
                order.getSenderName(),
                order.getDepartment(),
                order.getOrderDate(),
                order.getOrderedArticles().size() + " Artikel",
                order.getStatus() != null ? order.getStatus() : "In Bearbeitung"
        };
    }

    private void upsertOrderRow(Order order, boolean selectRow) {
        if (order == null || order.getOrderId() == null) {
            return;
        }

        upsertOrderInCache(order);

        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        int modelRow = findModelRowByOrderId(order.getOrderId());
        Object[] rowData = toRowData(order);

        if (modelRow >= 0) {
            for (int column = 0; column < rowData.length; column++) {
                model.setValueAt(rowData[column], modelRow, column);
            }
        } else {
            model.addRow(rowData);
        }

        applyCombinedFilter();
        if (selectRow) {
            selectOrderById(order.getOrderId());
        }
    }

    private void removeOrderRow(String orderId) {
        if (orderId == null) {
            return;
        }

        int modelRow = findModelRowByOrderId(orderId);
        if (modelRow >= 0) {
            ((DefaultTableModel) orderTable.getModel()).removeRow(modelRow);
        }

        removeOrderFromCache(orderId);
        applyCombinedFilter();
    }

    private int findModelRowByOrderId(String orderId) {
        if (orderId == null) {
            return -1;
        }

        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            if (orderId.equals(model.getValueAt(row, 0))) {
                return row;
            }
        }
        return -1;
    }

    private void selectOrderById(String orderId) {
        if (orderId == null) {
            return;
        }

        int modelRow = findModelRowByOrderId(orderId);
        if (modelRow < 0) {
            return;
        }

        int viewRow = sorter != null ? orderTable.convertRowIndexToView(modelRow) : modelRow;
        if (viewRow < 0) {
            orderTable.clearSelection();
            return;
        }

        orderTable.setRowSelectionInterval(viewRow, viewRow);
        orderTable.scrollRectToVisible(orderTable.getCellRect(viewRow, 0, true));
    }

    private String getSelectedOrderId() {
        int selectedRow = orderTable.getSelectedRow();
        if (selectedRow < 0) {
            return null;
        }

        int modelRow = orderTable.convertRowIndexToModel(selectedRow);
        Object value = orderTable.getModel().getValueAt(modelRow, 0);
        return value instanceof String orderId ? orderId : null;
    }

    private void upsertOrderInCache(Order order) {
        orderByIdCache.put(order.getOrderId(), order);

        if (ordersCache == null) {
            ordersCache = new ArrayList<>();
        }

        for (int i = 0; i < ordersCache.size(); i++) {
            Order current = ordersCache.get(i);
            if (current != null && order.getOrderId().equals(current.getOrderId())) {
                ordersCache.set(i, order);
                ordersCacheTime = System.currentTimeMillis();
                return;
            }
        }

        ordersCache.add(order);
        ordersCacheTime = System.currentTimeMillis();
    }

    private void removeOrderFromCache(String orderId) {
        orderByIdCache.remove(orderId);

        if (ordersCache != null) {
            ordersCache.removeIf(order -> order != null && orderId.equals(order.getOrderId()));
        }

        ordersCacheTime = System.currentTimeMillis();
    }

    private void applyCombinedFilter() {
        if (sorter == null) {
            updateOrderCount();
            return;
        }

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        String text = searchField.getText().trim();
        if (!text.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0, 1, 3));
        }

        String selected = (String) filterComboBox.getSelectedItem();
        if ("Abgeschlossene Bestellungen".equals(selected)) {
            filters.add(RowFilter.regexFilter("^Abgeschlossen$", 6));
        } else if ("Offene Bestellungen".equals(selected)) {
            filters.add(RowFilter.notFilter(RowFilter.regexFilter("^Abgeschlossen$", 6)));
        }

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        updateOrderCount();
    }
}
