package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.actions.OrderActions;
import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.OrderManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * TODO: Cache orders for performance
 */
public class OrderGUI extends JFrame {

    private final JTable orderTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{150, 180, 150, 150, 120, 120, 120};
    private final JLabel orderCountLabel;

    public OrderGUI() {
        setTitle("Bestellungen Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Header
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        RoundedPanel headerPanel = new RoundedPanel(new Color(255, 255, 255), 20);
        headerPanel.setPreferredSize(new Dimension(680, 64));
        JLabel titleLabel = new JLabel("Bestellungen Verwaltung");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(31, 45, 61));
        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(new Color(245, 247, 250));
        JButton newOrderButton = createRoundedButton("Neue Bestellung erstellen");
        JButton editOrderButton = createRoundedButton("Bestellung bearbeiten");
        JButton deleteOrderButton = createRoundedButton("Bestellung löschen");
        JButton completeOrderButton = createRoundedButton("Bestellung abschließen");
        JButton refreshButton = createRoundedButton("🔄 Aktualisieren");
        JComboBox<String> filterComboBox = new JComboBox<>(new String[]{"Alle Bestellungen", "Abgeschlossene Bestellungen", "Offene Bestellungen"});
        toolbar.add(filterComboBox);
        toolbar.add(newOrderButton);
        toolbar.add(editOrderButton);
        toolbar.add(deleteOrderButton);
        toolbar.add(completeOrderButton);
        toolbar.add(refreshButton);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.add(toolbar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.PAGE_START);

        // Main card with table
        RoundedPanel card = new RoundedPanel(Color.WHITE, 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Table setup
        orderTable = new JTable();
        initializeOrderTable();
        tableScrollPane = new JScrollPane(orderTable);
        card.add(tableScrollPane, BorderLayout.CENTER);

        // place card in center with padding
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(card, gbc);
        add(centerWrapper, BorderLayout.CENTER);

        // Bottom search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchPanel.setBackground(new Color(245, 247, 250));

        orderCountLabel = new JLabel("Anzahl Bestellungen: " + orderTable.getRowCount());
        orderCountLabel.setFont(orderCountLabel.getFont().deriveFont(Font.BOLD));
        searchPanel.add(orderCountLabel);

        JLabel searchLabel = new JLabel("Suche (Bestell-ID, Empfänger oder Abteilung):");
        JTextField searchField = new JTextField(28);
        JButton searchBtn = new JButton("Suchen");
        JButton clearBtn = new JButton("Leeren");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        add(searchPanel, BorderLayout.SOUTH);

        // load orders
        loadOrders();

        // Setup interactions
        setupTableInteractions();

        // Wire actions
        newOrderButton.addActionListener(new OrderActions.CreateOrderAction());

        editOrderButton.addActionListener(e -> {
            Vector<Object> rowData = getSelectedOrderData();
            if (rowData == null) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
            if (order != null) {
                EditOrderGUI editGui = new EditOrderGUI(order);
                editGui.display();
                // Reload orders after edit window closes
                editGui.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        loadOrders();
                    }
                });
            }
        });

        deleteOrderButton.addActionListener(e -> {
            int sel = orderTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            int modelRow = orderTable.convertRowIndexToModel(sel);
            String orderId = (String) orderTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diese Bestellung wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, Main.icon);
            if (confirm == JOptionPane.YES_OPTION) {
                if (OrderManager.getInstance().deleteOrder(orderId)) {
                    loadOrders(); // Refresh table
                    updateOrderCount();
                    JOptionPane.showMessageDialog(this, "Bestellung erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });

        completeOrderButton.addActionListener(new OrderActions.CompleteOrderAction());

        refreshButton.addActionListener(e -> {
            loadOrders();
            updateOrderCount();
            JOptionPane.showMessageDialog(this, "Bestellungsliste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);
        });

        // search logic using TableRowSorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) orderTable.getModel());
        orderTable.setRowSorter(sorter);

        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                try {
                    String regex = "(?i)" + Pattern.quote(text);
                    sorter.setRowFilter(RowFilter.regexFilter(regex, 0, 1, 3));
                } catch (PatternSyntaxException ex) {
                    sorter.setRowFilter(RowFilter.regexFilter(Pattern.quote(text), 0, 1, 3));
                }
            }
        };

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
        });
        searchField.addActionListener(e -> doSearch.run());

        filterComboBox.addActionListener(e -> {
            String selected = (String) filterComboBox.getSelectedItem();
            if ("Alle Bestellungen".equals(selected)) {
                sorter.setRowFilter(null);
            } else if ("Abgeschlossene Bestellungen".equals(selected)) {
                sorter.setRowFilter(RowFilter.regexFilter("^Abgeschlossen$", 6));
            } else if ("Offene Bestellungen".equals(selected)) {
                sorter.setRowFilter(RowFilter.notFilter(RowFilter.regexFilter("^Abgeschlossen$", 6)));
            }
        });

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

        setVisible(true);
    }

    private void loadOrders() {
        OrderManager om = OrderManager.getInstance();
        List<Order> orders = om.getOrders();
        DefaultTableModel model = (DefaultTableModel) orderTable.getModel();
        model.setRowCount(0);
        for (Order o : orders) {
            model.addRow(new Object[]{
                    o.getOrderId(),
                    o.getReceiverName(),
                    o.getSenderName(),
                    o.getDepartment(),
                    o.getOrderDate(),
                    o.getOrderedArticles().size() + " Artikel",
                    o.getStatus() != null ? o.getStatus() : "In Bearbeitung"
            });
        }
        updateOrderCount();
    }

    private void updateOrderCount() {
        if (orderCountLabel != null) {
            orderCountLabel.setText("Anzahl Bestellungen: " + orderTable.getRowCount());
        }
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Bearbeiten");
        JMenuItem del = new JMenuItem("Löschen");
        JMenuItem complete = new JMenuItem("Abschließen");
        popup.add(edit);
        popup.add(del);
        popup.add(complete);

        edit.addActionListener(e -> {
            Vector<Object> rowData = getSelectedOrderData();
            if (rowData == null) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
            if (order != null) {
                EditOrderGUI editGui = new EditOrderGUI(order);
                editGui.display();
                // Reload orders after edit window closes
                editGui.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        loadOrders();
                    }
                });
            }
        });
        del.addActionListener(e -> {
            int sel = orderTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = orderTable.convertRowIndexToModel(sel);
            String orderId = (String) orderTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diese Bestellung wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, Main.icon);
            if (confirm == JOptionPane.YES_OPTION) {
                if (OrderManager.getInstance().deleteOrder(orderId)) {
                    ((DefaultTableModel) orderTable.getModel()).removeRow(modelRow);
                    updateOrderCount();
                    JOptionPane.showMessageDialog(this, "Bestellung erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });
        complete.addActionListener(e -> new OrderActions.CompleteOrderAction().actionPerformed(null));

        orderTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    Vector<Object> rowData = getSelectedOrderData();
                    if (rowData == null) {
                        JOptionPane.showMessageDialog(OrderGUI.this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                                Main.icon);
                        return;
                    }
                    Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
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
        if (sel == -1) return null;
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
                    case 0 -> "Bestell-ID";
                    case 1 -> "Empfänger";
                    case 2 -> "Absender";
                    case 3 -> "Abteilung";
                    case 4 -> "Datum";
                    case 5 -> "Artikel";
                    case 6 -> "Status";
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
        // Make grid lines visible and subtle (adds vertical+horizontal lines like in your mockup)
        orderTable.setShowGrid(true);
        orderTable.setShowHorizontalLines(true);
        orderTable.setShowVerticalLines(true);
        orderTable.setGridColor(new Color(226, 230, 233)); // soft light gray
        orderTable.setIntercellSpacing(new Dimension(1, 1));
        orderTable.setFont(new Font("Arial", Font.PLAIN, 16));

        // visuals
        orderTable.setRowHeight(26);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setSelectionBackground(new Color(184, 207, 229));
        orderTable.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);

        // Status column renderer with color coding
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (!isSelected && value != null) {
                    String status = value.toString();
                    if ("Abgeschlossen".equals(status)) {
                        c.setBackground(new Color(200, 230, 201)); // Light green
                        c.setForeground(new Color(27, 94, 32)); // Dark green
                    } else if ("In Bearbeitung".equals(status)) {
                        c.setBackground(new Color(255, 243, 224)); // Light orange
                        c.setForeground(new Color(230, 81, 0)); // Dark orange
                    } else {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        };

        TableColumnModel tcm = orderTable.getColumnModel();
        if (tcm.getColumnCount() > 0) tcm.getColumn(0).setCellRenderer(center);
        if (tcm.getColumnCount() > 6) tcm.getColumn(6).setCellRenderer(statusRenderer);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

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
        orderTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Header styling
        JTableHeader header = orderTable.getTableHeader();
        header.setBackground(new Color(62, 84, 98));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
    }

    private void adjustColumnWidths() {
        if (tableScrollPane == null || orderTable.getColumnCount() == 0) return;
        int available = tableScrollPane.getViewport().getWidth();
        if (available <= 0) available = tableScrollPane.getWidth();
        if (available <= 0) return;
        int totalBase = 0;
        for (int w : baseColumnWidths) totalBase += w;
        TableColumnModel tcm = orderTable.getColumnModel();
        int colCount = tcm.getColumnCount();
        int used = 0;
        int[] newW = new int[colCount];
        for (int i = 0; i < colCount; i++) {
            int base = i < baseColumnWidths.length ? baseColumnWidths[i] : 100;
            int w = Math.max(60, (int) Math.round((base / (double) totalBase) * available));
            newW[i] = w;
            used += w;
        }
        int diff = available - used;
        int idx = 0;
        while (diff != 0 && colCount > 0) {
            newW[idx % colCount] += (diff > 0 ? 1 : -1);
            diff += (diff > 0 ? -1 : 1);
            idx++;
        }
        for (int i = 0; i < colCount; i++) {
            tcm.getColumn(i).setPreferredWidth(newW[i]);
        }
        orderTable.revalidate();
        orderTable.repaint();
        tableScrollPane.revalidate();
        tableScrollPane.repaint();
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        // Modern color scheme
        Color defaultBg = new Color(52, 152, 219); // Blue
        Color hoverBg = new Color(41, 128, 185);   // Darker blue
        Color pressedBg = new Color(31, 97, 141);  // Even darker blue

        button.setBackground(defaultBg);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(41, 128, 185), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add hover effects
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(31, 97, 141), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                button.setBackground(defaultBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(41, 128, 185), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                button.setBackground(hoverBg);
            }
        });

        return button;
    }

    private void showOrderDetailsDialog(Order order) {
        JDialog detailsDialog = new JDialog(this, "Bestelldetails - " + order.getOrderId(), true);
        detailsDialog.setSize(600, 500);
        detailsDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        panel.setBackground(Color.WHITE);

        // Build HTML content
        List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
        double totalPrice = 0.0;
        StringBuilder articlesHtml = new StringBuilder();

        for (Article a : articles) {
            int quantity = order.getOrderedArticles().getOrDefault(a.getArticleNumber(), 0);
            if (quantity == 0) {
                quantity = order.getOrderedArticles().getOrDefault(a.getName(), 0);
            }
            double lineTotal = quantity * a.getSellPrice();
            totalPrice += lineTotal;
            articlesHtml.append("<tr>")
                    .append("<td>").append(a.getArticleNumber()).append("</td>")
                    .append("<td>").append(a.getName()).append("</td>")
                    .append("<td align='right'>").append(quantity).append("</td>")
                    .append("<td align='right'>").append(String.format("%.2f", a.getSellPrice())).append(" CHF</td>")
                    .append("<td align='right'><b>").append(String.format("%.2f", lineTotal)).append(" CHF</b></td>")
                    .append("</tr>");
        }

        JScrollPane scrollPane = getJScrollPane(order, articlesHtml, totalPrice);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        btnPanel.setBackground(Color.WHITE);

        JButton exportBtn = createRoundedButton("PDF Exportieren");
        exportBtn.addActionListener(ev -> {
            createPDFExport(order);
        });

        JButton closeBtn = createRoundedButton("Schließen");
        closeBtn.addActionListener(ev -> detailsDialog.dispose());

        btnPanel.add(exportBtn);
        btnPanel.add(closeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        detailsDialog.add(panel);
        detailsDialog.setVisible(true);
    }

    private void createPDFExport(Order order) {
        // Choose save location
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"), "order_" + order.getOrderId() + ".pdf"));
        int result = fc.showSaveDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return; // User cancelled
        }

        File outputFile = fc.getSelectedFile();

        // Extract order data
        String orderId = order.getOrderId();
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        String receiverName = order.getReceiverName();
        String receiverKontoNumber = order.getReceiverKontoNumber();
        String orderDate = order.getOrderDate();
        String senderName = order.getSenderName();
        String senderKontoNumber = order.getSenderKontoNumber();
        String department = order.getDepartment();
        String status = order.getStatus() != null ? order.getStatus() : "In Bearbeitung";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // Load fonts
            PDFont regularFont;
            PDFont boldFont;
            File regularTtf = new File("/Library/Fonts/Arial.ttf");
            File boldTtf = new File("/Library/Fonts/Arial Bold.ttf");

            if (regularTtf.exists()) {
                regularFont = PDType0Font.load(doc, regularTtf);
                boldFont = boldTtf.exists() ? PDType0Font.load(doc, boldTtf) : regularFont;
            } else {
                // Fallback to built-in Type1 fonts
                regularFont = PDType1Font.HELVETICA;
                boldFont = PDType1Font.HELVETICA_BOLD;
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;

                // === HEADER SECTION ===
                cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f); // Dark blue
                cs.addRect(margin, yPosition - 60, pageWidth - 2 * margin, 60);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f); // White
                cs.setFont(boldFont, 24);
                cs.newLineAtOffset(margin + 10, yPosition - 35);
                cs.showText("VEBO BESTELLUNG");
                cs.endText();

                yPosition -= 80;

                // === ORDER INFO SECTION ===
                cs.beginText();
                cs.setNonStrokingColor(0f, 0f, 0f); // Black
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestelldatum:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(orderDate);
                cs.endText();

                yPosition -= 18;

                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestell-ID:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(orderId);
                cs.endText();

                yPosition -= 18;

                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Status:");
                cs.endText();

                cs.beginText();
                cs.setFont(boldFont, 11);
                if ("Abgeschlossen".equals(status)) {
                    cs.setNonStrokingColor(27f / 255f, 94f / 255f, 32f / 255f); // Green
                } else {
                    cs.setNonStrokingColor(230f / 255f, 81f / 255f, 0f); // Orange
                }
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(status);
                cs.endText();

                yPosition -= 30;

                // === SENDER / RECEIVER SECTION ===
                float boxHeight = 85;
                cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f); // Light gray
                cs.addRect(margin, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f);
                cs.addRect(margin + (pageWidth - 2 * margin) / 2 + 5, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                // Sender (left box)
                cs.beginText();
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(margin + 10, yPosition - 15);
                cs.showText("ABSENDER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(margin + 10, yPosition - 32);
                cs.showText(senderName);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(margin + 10, yPosition - 47);
                cs.showText("Konto: " + senderKontoNumber);
                cs.endText();

                // Receiver (right box)
                float rightBoxX = margin + (pageWidth - 2 * margin) / 2 + 15;
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(rightBoxX, yPosition - 15);
                cs.showText("EMPFAENGER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(rightBoxX, yPosition - 32);
                cs.showText(receiverName);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 47);
                cs.showText("Konto: " + receiverKontoNumber);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 62);
                cs.showText("Abteilung: " + department);
                cs.endText();

                yPosition -= boxHeight + 30;

                // === ARTICLES TABLE ===
                cs.setNonStrokingColor(62f / 255f, 84f / 255f, 98f / 255f); // Dark blue-gray
                cs.addRect(margin, yPosition - 20, pageWidth - 2 * margin, 20);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(boldFont, 10);
                cs.newLineAtOffset(margin + 5, yPosition - 14);
                cs.showText("Artikel");
                cs.newLineAtOffset(200, 0);
                cs.showText("Menge");
                cs.newLineAtOffset(60, 0);
                cs.showText("Einzelpreis");
                cs.newLineAtOffset(80, 0);
                cs.showText("Gesamt");
                cs.endText();

                yPosition -= 25;

                // Get article details and calculate totals
                List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
                double total = 0.0;
                boolean alternateRow = false;

                for (Article article : articles) {
                    int qty = orderedArticles.getOrDefault(article.getArticleNumber(), 0);
                    if (qty == 0) {
                        qty = orderedArticles.getOrDefault(article.getName(), 0);
                    }

                    double unit = article.getSellPrice();
                    double line = unit * qty;
                    total += line;

                    // Alternate row background
                    if (alternateRow) {
                        cs.setNonStrokingColor(250f / 255f, 250f / 255f, 250f / 255f);
                        cs.addRect(margin, yPosition - 15, pageWidth - 2 * margin, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.setFont(regularFont, 9);
                    cs.newLineAtOffset(margin + 5, yPosition - 10);

                    // Truncate long article names
                    String articleName = article.getName();
                    if (articleName.length() > 35) {
                        articleName = articleName.substring(0, 32) + "...";
                    }
                    cs.showText(articleName + " (" + article.getArticleNumber() + ")");
                    cs.newLineAtOffset(200, 0);
                    cs.showText(String.valueOf(qty));
                    cs.newLineAtOffset(60, 0);
                    cs.showText(String.format("%.2f CHF", unit));
                    cs.newLineAtOffset(80, 0);
                    cs.showText(String.format("%.2f CHF", line));
                    cs.endText();

                    yPosition -= 18;
                    alternateRow = !alternateRow;

                    // Check if we need a new page
                    if (yPosition < 150) {
                        break; // Stop if running out of space
                    }
                }

                // === TOTALS SECTION ===
                yPosition -= 10;
                cs.setNonStrokingColor(200f / 255f, 200f / 255f, 200f / 255f);
                cs.setLineWidth(1);
                cs.moveTo(margin, yPosition);
                cs.lineTo(pageWidth - margin, yPosition);
                cs.stroke();

                yPosition -= 25;

                // Total box
                cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
                cs.addRect(pageWidth - margin - 150, yPosition - 25, 150, 30);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(pageWidth - margin - 140, yPosition - 15);
                cs.showText("TOTAL:");
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format("%.2f CHF", total));
                cs.endText();

                // === FOOTER ===
                cs.beginText();
                cs.setNonStrokingColor(150f / 255f, 150f / 255f, 150f / 255f);
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(margin, 30);
                cs.showText("VEBO Lagersystem - Generiert am " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date()));
                cs.endText();
            }

            doc.save(outputFile);

            JOptionPane.showMessageDialog(this,
                    "PDF erfolgreich erstellt:\n" + outputFile.getAbsolutePath(),
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Erstellen des PDF-Dokuments:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
        }
    }

    private static JScrollPane getJScrollPane(Order order, StringBuilder articlesHtml, double totalPrice) {
        String htmlContent = "<html><body style='font-family: Arial, sans-serif; padding: 10px;'>" +
                "<h2 style='color: #1e3a5f; margin-bottom: 20px;'>Bestelldetails</h2>" +
                "<table style='width: 100%; margin-bottom: 20px;'>" +
                "<tr><td style='width: 180px;'><b>Bestell-ID:</b></td><td>" + order.getOrderId() + "</td></tr>" +
                "<tr><td><b>Status:</b></td><td><span style='color: " +
                ("Abgeschlossen".equals(order.getStatus()) ? "#1b5e20" : "#e65100") + "; font-weight: bold;'>" +
                (order.getStatus() != null ? order.getStatus() : "In Bearbeitung") + "</span></td></tr>" +
                "<tr><td><b>Datum:</b></td><td>" + order.getOrderDate() + "</td></tr>" +
                "<tr><td><b>Abteilung:</b></td><td>" + order.getDepartment() + "</td></tr>" +
                "<tr><td><b>Empfänger:</b></td><td>" + order.getReceiverName() + "</td></tr>" +
                "<tr><td><b>Empfänger Konto:</b></td><td>" + order.getReceiverKontoNumber() + "</td></tr>" +
                "<tr><td><b>Absender:</b></td><td>" + order.getSenderName() + "</td></tr>" +
                "<tr><td><b>Absender Konto:</b></td><td>" + order.getSenderKontoNumber() + "</td></tr>" +
                "</table>" +
                "<h3 style='color: #1e3a5f; margin-top: 20px; margin-bottom: 10px;'>Bestellte Artikel</h3>" +
                "<table border='1' cellpadding='8' cellspacing='0' style='width: 100%; border-collapse: collapse; border-color: #ddd;'>" +
                "<tr style='background-color: #3e5462; color: white;'>" +
                "<th>Art.-Nr.</th><th>Name</th><th>Menge</th><th>Einzelpreis</th><th>Gesamt</th>" +
                "</tr>" +
                articlesHtml.toString() +
                "<tr style='background-color: #f0f0f0; font-weight: bold;'>" +
                "<td colspan='4' align='right'>Gesamtpreis:</td>" +
                "<td align='right' style='color: #1e3a5f; font-size: 16px;'>" + String.format("%.2f", totalPrice) + " CHF</td>" +
                "</tr>" +
                "</table>" +
                "</body></html>";

        JEditorPane editorPane = new JEditorPane("text/html", htmlContent);
        editorPane.setEditable(false);
        editorPane.setBackground(Color.WHITE);
        return new JScrollPane(editorPane);
    }

    // small rounded panel for card styling
    private static class RoundedPanel extends JPanel {
        private final Color bg;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
            this.bg = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}