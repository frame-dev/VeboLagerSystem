package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ClientManager;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ClientGUI extends JFrame {

    private final JTable clientTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{200, 200, 200};
    private final List<Client> clients = new ArrayList<>();

    public ClientGUI() {
        setTitle("Kunden Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        RoundedPanel headerPanel = new RoundedPanel(new Color(255, 255, 255), 20);
        headerPanel.setPreferredSize(new Dimension(680, 64));
        JLabel titleLabel = new JLabel("Kunden Verwaltung");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(31, 45, 61));
        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(new Color(245, 247, 250));
        JButton addClientButton = createRoundedButton("Kunde hinzufügen");
        JButton editClientButton = createRoundedButton("Kunde bearbeiten");
        JButton deleteClientButton = createRoundedButton("Kunde löschen");
        toolbar.add(addClientButton);
        toolbar.add(editClientButton);
        toolbar.add(deleteClientButton);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(245, 247, 250));
        topPanel.add(toolbar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.PAGE_START);

        // Main card with table
        RoundedPanel card = new RoundedPanel(Color.WHITE, 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Table setup
        clientTable = new JTable();
        initializeClientTable();
        tableScrollPane = new JScrollPane(clientTable);
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
        JLabel searchLabel = new JLabel("Suche (Name oder Abteilung):");
        JTextField searchField = new JTextField(28);
        JButton searchBtn = new JButton("Suchen");
        JButton clearBtn = new JButton("Leeren");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        add(searchPanel, BorderLayout.SOUTH);

        // load clients
        loadClients();

        // Setup interactions
        setupTableInteractions();

        // Wire actions
        addClientButton.addActionListener(e -> {
            Object[] row = showAddClientDialog();
            if (row != null) {
                loadClients(); // Reload from database to ensure sync
            }
        });

        editClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[3];
            for (int i = 0; i < 3; i++) {
                existing[i] = clientTable.getModel().getValueAt(modelRow, i);
            }
            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null) {
                loadClients(); // Reload from database to ensure sync
            }
        });

        deleteClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);
            String konto = (String) clientTable.getModel().getValueAt(modelRow, 2);

            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name, konto)) {
                    clients.remove(modelRow);
                    ((DefaultTableModel) clientTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // search logic using TableRowSorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) clientTable.getModel());
        clientTable.setRowSorter(sorter);

        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
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

    private void loadClients() {
        ClientManager clientManager = ClientManager.getInstance();
        List<Map<String, String>> dbClients = clientManager.getAllClients();

        clients.clear();
        DefaultTableModel model = (DefaultTableModel) clientTable.getModel();
        model.setRowCount(0);

        for (Map<String, String> dbClient : dbClients) {
            String name = dbClient.get("firstLastName");
            String dept = dbClient.get("department");
            String konto = dbClient.get("kontoNumber");

            clients.add(new Client(name, dept, konto));
            model.addRow(new Object[]{name, dept, konto});
        }
    }

    private Object[] showAddClientDialog() {
        final Object[][] holder = new Object[1][];
        JDialog dialog = new JDialog(this, "Neuen Kunden hinzufügen", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Vorname und Nachname:"), gbc);
        JTextField nameField = new JTextField(20);
        gbc.gridx = 1;
        p.add(nameField, gbc);
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Abteilung:"), gbc);
        JTextField deptField = new JTextField(20);
        gbc.gridx = 1;
        p.add(deptField, gbc);
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Konto Nummer:"), gbc);
        JTextField kontoField = new JTextField(20);
        gbc.gridx = 1;
        p.add(kontoField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Abbrechen");
        buttons.add(cancel);
        buttons.add(ok);
        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.gridwidth = 2;
        p.add(buttons, gbc);

        ok.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String dept = deptField.getText().trim();
            String konto = kontoField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            holder[0] = new Object[]{name, dept, konto};
            dialog.dispose();
            ClientManager clientManager = ClientManager.getInstance();
            if(!clientManager.insertClient(name, konto, dept)) {
                JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        });
        cancel.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        dialog.getContentPane().add(p);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return holder[0];
    }

    private Object[] showUpdateClientDialog(Object[] existing) {
        final Object[][] holder = new Object[1][];
        JDialog dialog = new JDialog(this, "Kunde bearbeiten", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Vorname und Nachname:"), gbc);
        JTextField nameField = new JTextField(existing[0] == null ? "" : existing[0].toString(), 20);
        gbc.gridx = 1;
        p.add(nameField, gbc);
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Abteilung:"), gbc);
        JTextField deptField = new JTextField(existing[1] == null ? "" : existing[1].toString(), 20);
        gbc.gridx = 1;
        p.add(deptField, gbc);
        row++;
        gbc.gridx = 0;
        gbc.gridy = row;
        p.add(new JLabel("Konto Nummer:"), gbc);
        JTextField kontoField = new JTextField(existing[2] == null ? "" : existing[2].toString(), 20);
        gbc.gridx = 1;
        p.add(kontoField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Abbrechen");
        buttons.add(cancel);
        buttons.add(ok);
        gbc.gridx = 0;
        gbc.gridy = ++row;
        gbc.gridwidth = 2;
        p.add(buttons, gbc);

        ok.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String dept = deptField.getText().trim();
            String konto = kontoField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE);
                return;
            }
            holder[0] = new Object[]{name, dept, konto};
            dialog.dispose();
            ClientManager clientManager = ClientManager.getInstance();
            if(!clientManager.existsClient(name, konto)) {
                if(!clientManager.insertClient(name, konto, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                if(!clientManager.updateClient(name, konto, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Aktualisieren des Kunden in der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        cancel.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        dialog.getContentPane().add(p);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return holder[0];
    }

    /**
     * Setup table interactions: double-click to edit, right-click context menu for edit/delete
     */
    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Bearbeiten");
        JMenuItem del = new JMenuItem("Löschen");
        popup.add(edit);
        popup.add(del);

        edit.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[3];
            for (int i = 0; i < 3; i++) {
                existing[i] = clientTable.getModel().getValueAt(modelRow, i);
            }
            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null) {
                loadClients(); // Reload from database to ensure sync
            }
        });

        del.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);
            String konto = (String) clientTable.getModel().getValueAt(modelRow, 2);

            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name, konto)) {
                    clients.remove(modelRow);
                    ((DefaultTableModel) clientTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        clientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = clientTable.rowAtPoint(e.getPoint());
                    if (row == -1) return;
                    int modelRow = clientTable.convertRowIndexToModel(row);
                    Object[] existing = new Object[3];
                    for (int i = 0; i < 3; i++) {
                        existing[i] = clientTable.getModel().getValueAt(modelRow, i);
                    }
                    Object[] updated = showUpdateClientDialog(existing);
                    if (updated != null) {
                        loadClients(); // Reload from database to ensure sync
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
                    int row = clientTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        clientTable.setRowSelectionInterval(row, row);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void initializeClientTable() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public int getColumnCount() {
                return 3;
            }

            @Override
            public String getColumnName(int col) {
                return switch (col) {
                    case 0 -> "Name";
                    case 1 -> "Abteilung";
                    case 2 -> "Konto Nummer";
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
        clientTable.setModel(model);

        // visuals
        clientTable.setRowHeight(26);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientTable.setShowGrid(true);
        clientTable.setGridColor(new Color(226, 230, 233));
        clientTable.setIntercellSpacing(new Dimension(1, 1));
        clientTable.setSelectionBackground(new Color(184, 207, 229));
        clientTable.setSelectionForeground(Color.BLACK);

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumnModel tcm = clientTable.getColumnModel();
        if (tcm.getColumnCount() > 0) tcm.getColumn(2).setCellRenderer(center);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }
    }

    private void adjustColumnWidths() {
        if (tableScrollPane == null || clientTable.getColumnCount() == 0) return;
        int available = tableScrollPane.getViewport().getWidth();
        if (available <= 0) available = tableScrollPane.getWidth();
        if (available <= 0) return;
        int totalBase = 0;
        for (int w : baseColumnWidths) totalBase += w;
        TableColumnModel tcm = clientTable.getColumnModel();
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
        clientTable.revalidate();
        clientTable.repaint();
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

    // Simple Client data class
    private static class Client {
        String name;
        String department;
        String kontoNumber;

        Client(String name, String department, String kontoNumber) {
            this.name = name;
            this.department = department;
            this.kontoNumber = kontoNumber;
        }
    }

    // Rounded panel for card styling
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
