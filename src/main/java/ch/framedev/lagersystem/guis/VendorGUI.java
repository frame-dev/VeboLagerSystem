package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.VendorManager;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * TODO: Cache vendor list and refresh only on changes
 */
public class VendorGUI extends JFrame {

    private final JTable vendorTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{120, 260, 160, 140, 220, 300};

    public VendorGUI() {
        setTitle("Lieferant Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Header
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        RoundedPanel headerPanel = new RoundedPanel(new Color(255, 255, 255), 20);
        headerPanel.setPreferredSize(new Dimension(680, 64));
        JLabel titleLabel = new JLabel("Lieferant Verwaltung");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(31, 45, 61));
        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(new Color(245, 247, 250));
        JButton addVendorButton = createRoundedButton("Lieferant hinzufügen");
        JButton editVendorButton = createRoundedButton("Lieferant bearbeiten");
        JButton deleteVendorButton = createRoundedButton("Lieferant löschen");
        JButton refreshButton = createRoundedButton("🔄 Aktualisieren");
        toolbar.add(addVendorButton);
        toolbar.add(editVendorButton);
        toolbar.add(deleteVendorButton);
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
        vendorTable = new JTable();
        initializeVendorTable();
        tableScrollPane = new JScrollPane(vendorTable);
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
        JLabel searchLabel = new JLabel("Suche (Name oder Kontakt):");
        JTextField searchField = new JTextField(28);
        JButton searchBtn = new JButton("Suchen");
        JButton clearBtn = new JButton("Leeren");

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        add(searchPanel, BorderLayout.SOUTH);

        // load vendors
        loadVendors();

        // Setup interactions (context menu, double click)
        setupTableInteractions();

        // wiring: add/edit/delete
        addVendorButton.addActionListener(e -> {
            Object[] row = showAddVendorDialog();
            if (row != null) {
                Vendor v = new Vendor((String) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) row[5]).split("\\s*,\\s*")));
                if (VendorManager.getInstance().insertVendor(v)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich hinzugefügt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler: Lieferant bereits vorhanden oder Insert fehlgeschlagen", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });

        editVendorButton.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Lieferanten zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateVendorDialog(existing);
            if (updated != null) {
                Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                if (VendorManager.getInstance().updateVendor(v)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich aktualisiert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Lieferanten.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });

        deleteVendorButton.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Lieferanten zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.icon);
                return;
            }
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            String name = (String) vendorTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Lieferanten wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, Main.icon);
            if (confirm == JOptionPane.YES_OPTION) {
                if (VendorManager.getInstance().deleteVendor(name)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            loadVendors();
            JOptionPane.showMessageDialog(this, "Lieferantenliste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE);
        });

        // search logic using TableRowSorter on columns 0 (ID) and 1 (Name)
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) vendorTable.getModel());
        vendorTable.setRowSorter(sorter);

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

        // auto resize logic: adjust columns when visible or resized
        ComponentAdapter resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                adjustColumnWidths();
            }
        };
        this.addComponentListener(resizeListener);
        tableScrollPane.getViewport().addComponentListener(resizeListener);
        SwingUtilities.invokeLater(this::adjustColumnWidths);

        // show frame
        setVisible(true);
    }

    private void loadVendors() {
        VendorManager vm = VendorManager.getInstance();
        java.util.List<Vendor> vendors = vm.getVendors();
        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
        model.setRowCount(0);
        for (Vendor v : vendors) {
            model.addRow(new Object[]{v.getName(), v.getContactPerson(), v.getPhoneNumber(), v.getEmail(), v.getAddress(), String.join(",", v.getSuppliedArticles())});
        }
    }

    private Object[] showAddVendorDialog() {
        final Object[][] holder = new Object[1][];
        JDialog dialog = new JDialog(this, "Neuen Lieferanten hinzufügen", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Name:"), gbc);
        JTextField idField = new JTextField(20); gbc.gridx = 1; p.add(idField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Kontaktperson:"), gbc);
        JTextField contactField = new JTextField(20); gbc.gridx = 1; p.add(contactField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Telefon:"), gbc);
        JTextField phoneField = new JTextField(20); gbc.gridx = 1; p.add(phoneField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Email:"), gbc);
        JTextField emailField = new JTextField(20); gbc.gridx = 1; p.add(emailField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Adresse:"), gbc);
        JTextField addressField = new JTextField(20); gbc.gridx = 1; p.add(addressField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Gelieferte Artikel (kommagetrennt):"), gbc);
        JTextField articlesField = new JTextField(20); gbc.gridx = 1; p.add(articlesField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"); JButton cancel = new JButton("Abbrechen");
        buttons.add(cancel); buttons.add(ok);
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2; p.add(buttons, gbc);

        ok.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();
            if (id.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE,
                    Main.icon); return; }
            holder[0] = new Object[]{id, contact, phone, email, addr, arts};
            dialog.dispose();
        });
        cancel.addActionListener(ae -> { holder[0] = null; dialog.dispose(); });

        dialog.getContentPane().add(p);
        dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return holder[0];
    }

    private Object[] showUpdateVendorDialog(Object[] existing) {
        final Object[][] holder = new Object[1][];
        JDialog dialog = new JDialog(this, "Lieferant bearbeiten", true);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,6,6,6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Name:"), gbc);
        JTextField idField = new JTextField(existing[0]==null?"":existing[0].toString(),20); gbc.gridx = 1; p.add(idField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Kontaktperson:"), gbc);
        JTextField contactField = new JTextField(existing[1]==null?"":existing[1].toString(),20); gbc.gridx = 1; p.add(contactField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Telefon:"), gbc);
        JTextField phoneField = new JTextField(existing[2]==null?"":existing[2].toString(),20); gbc.gridx = 1; p.add(phoneField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Email:"), gbc);
        JTextField emailField = new JTextField(existing[3]==null?"":existing[3].toString(),20); gbc.gridx = 1; p.add(emailField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Adresse:"), gbc);
        JTextField addressField = new JTextField(existing[4]==null?"":existing[4].toString(),20); gbc.gridx = 1; p.add(addressField, gbc);
        row++; gbc.gridx = 0; gbc.gridy = row; p.add(new JLabel("Gelieferte Artikel (kommagetrennt):"), gbc);
        JTextField articlesField = new JTextField(existing[5]==null?"":existing[5].toString(),20); gbc.gridx = 1; p.add(articlesField, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton ok = new JButton("OK"); JButton cancel = new JButton("Abbrechen");
        buttons.add(cancel); buttons.add(ok);
        gbc.gridx = 0; gbc.gridy = ++row; gbc.gridwidth = 2; p.add(buttons, gbc);

        ok.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();
            if (id.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.icon); return; }
            holder[0] = new Object[]{id, contact, phone, email, addr, arts};
            dialog.dispose();
        });
        cancel.addActionListener(ae -> { holder[0] = null; dialog.dispose(); });

        dialog.getContentPane().add(p);
        dialog.pack(); dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return holder[0];
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem("Bearbeiten");
        JMenuItem del = new JMenuItem("Löschen");
        popup.add(edit); popup.add(del);

        edit.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow(); if (sel == -1) return;
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateVendorDialog(existing);
            if (updated != null) {
                DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
                for (int i=0;i<updated.length;i++) model.setValueAt(updated[i], modelRow, i);
                Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                VendorManager.getInstance().updateVendor(v);
            }
        });

        del.addActionListener(e -> deleteSelectedVendor());

        vendorTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = vendorTable.rowAtPoint(e.getPoint()); if (row==-1) return;
                    int modelRow = vendorTable.convertRowIndexToModel(row);
                    Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
                    Object[] updated = showUpdateVendorDialog(existing);
                    if (updated != null) {
                        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
                        for (int i=0;i<updated.length;i++) model.setValueAt(updated[i], modelRow, i);
                        Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                        v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                        VendorManager.getInstance().updateVendor(v);
                    }
                }
            }

            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
            private void maybeShow(MouseEvent e) { if (e.isPopupTrigger()) { int row = vendorTable.rowAtPoint(e.getPoint()); if (row!=-1) { vendorTable.setRowSelectionInterval(row,row); popup.show(e.getComponent(), e.getX(), e.getY()); } } }
        });
    }

    private void deleteSelectedVendor() {
        int sel = vendorTable.getSelectedRow(); if (sel==-1) return;
        int modelRow = vendorTable.convertRowIndexToModel(sel);
        String name = (String) vendorTable.getModel().getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Lieferanten wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, Main.icon);
        if (confirm == JOptionPane.YES_OPTION) {
            if (VendorManager.getInstance().deleteVendor(name)) {
                ((DefaultTableModel) vendorTable.getModel()).removeRow(modelRow);
            } else {
                JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE,
                        Main.icon);
            }
        }
    }

    private void initializeVendorTable() {
        // Column names must match the row data order used in loadVendors()
        String[] cols = new String[]{"Name", "Kontakt", "Telefon", "Email", "Adresse", "Gelieferte Artikel"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) { return String.class; }
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        vendorTable.setModel(model);

        // visuals
        vendorTable.setRowHeight(26);
        vendorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vendorTable.setShowGrid(true);
        vendorTable.setGridColor(new Color(226, 230, 233));
        vendorTable.setIntercellSpacing(new Dimension(1, 1));
        vendorTable.setFont(new Font("Arial", Font.PLAIN, 16));

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumnModel tcm = vendorTable.getColumnModel();
        if (tcm.getColumnCount()>0) tcm.getColumn(0).setCellRenderer(center);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);

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
        vendorTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Header styling
        JTableHeader header = vendorTable.getTableHeader();
        header.setBackground(new Color(62, 84, 98));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
    }

    private void adjustColumnWidths() {
        if (tableScrollPane==null || vendorTable.getColumnCount()==0) return;
        int available = tableScrollPane.getViewport().getWidth(); if (available<=0) available = tableScrollPane.getWidth(); if (available<=0) return;
        int totalBase = 0; for (int w: baseColumnWidths) totalBase+=w;
        TableColumnModel tcm = vendorTable.getColumnModel(); int colCount = tcm.getColumnCount();
        int used=0; int[] newW = new int[colCount];
        for (int i=0;i<colCount;i++) { int base = i<baseColumnWidths.length?baseColumnWidths[i]:100; int w = Math.max(60,(int)Math.round((base/(double)totalBase)*available)); newW[i]=w; used+=w; }
        int diff = available-used; int idx=0; while(diff!=0 && colCount>0){ newW[idx%colCount]+= (diff>0?1:-1); diff += (diff>0?-1:1); idx++; }
        for (int i=0;i<colCount;i++){ TableColumn c = tcm.getColumn(i); c.setPreferredWidth(newW[i]); }
        vendorTable.revalidate(); vendorTable.repaint(); tableScrollPane.revalidate(); tableScrollPane.repaint();
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

    // small rounded panel for card/header styling
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
