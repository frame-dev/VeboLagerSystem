package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.actions.OrderActions;
import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.managers.OrderManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class OrderGUI extends JFrame {

    private final JTable orderTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{150, 180, 150, 150, 120, 120};

    public OrderGUI() {
        setTitle("Bestellungen Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

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
        toolbar.add(newOrderButton);
        toolbar.add(editOrderButton);
        toolbar.add(deleteOrderButton);
        toolbar.add(completeOrderButton);
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
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
            if (order != null) {
                EditOrderGUI editGui = new EditOrderGUI(order);
                editGui.display();
            }
        });

        deleteOrderButton.addActionListener(e -> {
            int sel = orderTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = orderTable.convertRowIndexToModel(sel);
            String orderId = (String) orderTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diese Bestellung wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (OrderManager.getInstance().deleteOrder(orderId)) {
                    ((DefaultTableModel) orderTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        completeOrderButton.addActionListener(new OrderActions.CompleteOrderAction());

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
                    o.getOrderedArticles().size() + " Artikel"
            });
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
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
            if (order != null) {
                EditOrderGUI editGui = new EditOrderGUI(order);
                editGui.display();
            }
        });
        del.addActionListener(e -> {
            int sel = orderTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = orderTable.convertRowIndexToModel(sel);
            String orderId = (String) orderTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diese Bestellung wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (OrderManager.getInstance().deleteOrder(orderId)) {
                    ((DefaultTableModel) orderTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE);
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
                        JOptionPane.showMessageDialog(OrderGUI.this, "Bitte wählen Sie eine Bestellung zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    Order order = OrderManager.getInstance().getOrder((String) rowData.getFirst());
                    if (order != null) {
                        JFrame detailsGUI = new JFrame("Details");
                        JPanel panel = new JPanel(new BorderLayout(10, 10));
                        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                        JTextArea detailsArea = new JTextArea();
                        detailsArea.setEditable(false);
                        StringBuilder detailsText = new StringBuilder();
                        detailsText.append("Bestell-ID: ").append(order.getOrderId()).append("\n");
                        detailsText.append("Department: ").append(order.getDepartment()).append("\n");
                        detailsText.append("Empfänger: ").append(order.getReceiverName()).append("\n");
                        detailsText.append("Absender: ").append(order.getSenderName()).append("\n");
                        detailsText.append("Datum: ").append(order.getOrderDate()).append("\n");
                        List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
                        detailsText.append("Artikel:\n");
                        for (Article a : articles) {
                            int quantity = order.getOrderedArticles().getOrDefault(a.getArticleNumber(), 0);
                            if (quantity == 0) {
                                quantity = order.getOrderedArticles().get(a.getName());
                            }
                            detailsText.append(" - ").append(a.getArticleNumber()).append(": ").append(a.getName()).append(" :: Menge: ").append(quantity).append(" | Einzelpreis: ").append(a.getSellPrice()).append(" CHF").append(" || Preis: ")
                                    .append(quantity * a.getSellPrice()).append(" CHF").append("\n");
                        }
                        detailsText.append("\nGesamtpreis: ");
                        double totalPrice = 0.0;
                        for (Article a : articles) {
                            int quantity = order.getOrderedArticles().getOrDefault(a.getArticleNumber(), 0);
                            if (quantity == 0) {
                                quantity = order.getOrderedArticles().get(a.getName());
                            }
                            totalPrice += quantity * a.getSellPrice();
                        }
                        detailsText.append(totalPrice).append(" CHF\n");
                        detailsArea.setText(detailsText.toString());
                        panel.add(new JScrollPane(detailsArea), BorderLayout.CENTER);
                        JButton closeBtn = new JButton("Schließen");
                        closeBtn.addActionListener(ev -> detailsGUI.dispose());
                        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        btnPanel.add(closeBtn);
                        panel.add(btnPanel, BorderLayout.SOUTH);
                        detailsGUI.add(panel);
                        detailsGUI.setSize(400, 400);
                        detailsGUI.setLocationRelativeTo(OrderGUI.this);
                        detailsGUI.setVisible(true);
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
                return 6;
            }

            @Override
            public String getColumnName(int col) {
                switch (col) {
                    case 0:
                        return "Bestell-ID";
                    case 1:
                        return "Empfänger";
                    case 2:
                        return "Absender";
                    case 3:
                        return "Abteilung";
                    case 4:
                        return "Datum";
                    case 5:
                        return "Artikel";
                    default:
                        return "";
                }
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

        // visuals
        orderTable.setRowHeight(26);
        orderTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        orderTable.setShowGrid(true);
        orderTable.setGridColor(new Color(226, 230, 233));
        orderTable.setIntercellSpacing(new Dimension(1, 1));
        orderTable.setSelectionBackground(new Color(184, 207, 229));
        orderTable.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumnModel tcm = orderTable.getColumnModel();
        if (tcm.getColumnCount() > 0) tcm.getColumn(0).setCellRenderer(center);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }
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
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(20, 30, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 2),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
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