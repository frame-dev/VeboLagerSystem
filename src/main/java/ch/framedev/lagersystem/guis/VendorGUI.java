package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.dialogs.VendorDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.VendorManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import static ch.framedev.lagersystem.utils.JFrameUtils.RoundedPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
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
        ThemeManager.applyUIDefaults();
        setTitle("Lieferant Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Header
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());
        RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setPreferredSize(new Dimension(680, 64));
        JLabel titleLabel = new JLabel("Lieferant Verwaltung");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(ThemeManager.getBackgroundColor());
        JButton addVendorButton = createRoundedButton(UnicodeSymbols.HEAVY_PLUS + " Lieferant hinzufügen");
        addVendorButton.setToolTipText("Neuen Lieferanten hinzufügen");
        JButton editVendorButton = createRoundedButton(UnicodeSymbols.CODE + " Lieferant bearbeiten");
        editVendorButton.setToolTipText("Ausgewählten Lieferanten bearbeiten");
        JButton deleteVendorButton = createRoundedButton(UnicodeSymbols.TRASH + " Lieferant löschen");
        deleteVendorButton.setToolTipText("Ausgewählten Lieferanten löschen");
        JButton refreshButton = createRoundedButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.setToolTipText("Lieferantenliste aktualisieren");
        toolbar.add(addVendorButton);
        toolbar.add(editVendorButton);
        toolbar.add(deleteVendorButton);
        toolbar.add(refreshButton);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.add(toolbar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.PAGE_START);

        // Main card with table
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Table setup
        vendorTable = new JTable();
        initializeVendorTable();
        tableScrollPane = new JScrollPane(vendorTable);
        tableScrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        tableScrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
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

        // Bottom search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchPanel.setBackground(ThemeManager.getBackgroundColor());
        JLabel searchLabel = new JLabel("Suche (Name oder Kontakt):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JTextField searchField = new JTextField(28);
        styleTextField(searchField);

        JButton searchBtn = new JButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Nach Lieferanten suchen");
        JButton clearBtn = new JButton(UnicodeSymbols.CLEAR + " Leeren");
        clearBtn.setToolTipText("Suchfeld leeren");
        styleFlatActionButton(searchBtn);
        styleFlatActionButton(clearBtn);

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
                Vendor v = new Vendor((String) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4], (double) row[6]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) row[5]).split("\\s*,\\s*")));
                if (VendorManager.getInstance().insertVendor(v)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich hinzugefügt.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler: Lieferant bereits vorhanden oder Insert fehlgeschlagen", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });

        editVendorButton.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Lieferanten zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateVendorDialog(existing);
            if (updated != null) {
                Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4], (double) updated[6]);
                v.setSuppliedArticles(Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                if (VendorManager.getInstance().updateVendor(v)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich aktualisiert.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Lieferanten.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });

        deleteVendorButton.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Lieferanten zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            String name = (String) vendorTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Lieferanten wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                if (VendorManager.getInstance().deleteVendor(name)) {
                    loadVendors(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            loadVendors();
            JOptionPane.showMessageDialog(this, "Lieferantenliste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE);
        });

        // search logic using TableRowSorter on columns 0 (Name) and 1 (Kontakt)
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

    private void styleTextField(JTextField tf) {
        tf.setBackground(ThemeManager.getInputBackgroundColor());
        tf.setForeground(ThemeManager.getTextPrimaryColor());
        tf.setCaretColor(ThemeManager.getTextPrimaryColor());
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    /**
     * Makes small "action" buttons (Search/Clear) display consistently in dark/light.
     * (Your big buttons already use createRoundedButton().)
     */
    private void styleFlatActionButton(JButton btn) {
        Color fg = ThemeManager.getTextPrimaryColor();
        Color hover = ThemeManager.getButtonHoverColor(fg);

        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setForeground(fg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setForeground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                btn.setForeground(fg);
            }
        });
    }

    private void loadVendors() {
        VendorManager vm = VendorManager.getInstance();
        java.util.List<Vendor> vendors = vm.getVendors();
        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
        model.setRowCount(0);
        for (Vendor v : vendors) {
            model.addRow(new Object[]{
                    v.getName(),
                    v.getContactPerson(),
                    v.getPhoneNumber(),
                    v.getEmail(),
                    v.getAddress(),
                    String.join(",", v.getSuppliedArticles()),
                    v.getMinOrderValue()
            });
        }
    }

    private Object[] showAddVendorDialog() {
        return VendorDialog.showAddVendorDialog(this);
    }

    /**
     * Shows a dialog to update an existing vendor.
     *
     * @param existing The existing vendor data as an Object array.
     * @return The updated vendor data as an Object array, or null if canceled.
     */
    private Object[] showUpdateVendorDialog(Object[] existing) {
        return VendorDialog.showUpdateVendorDialog(this, existing);
    }

    /**
     * Adjusts column widths based on the current size of the table's viewport.
     * Distributes extra space proportionally based on baseColumnWidths.
     */
    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        JMenuItem del = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");
        popup.add(edit);
        popup.add(del);

        edit.setToolTipText("Ausgewählten Lieferanten bearbeiten");
        del.setToolTipText("Ausgewählten Lieferanten löschen");

        edit.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateVendorDialog(existing);
            if (updated != null) {
                DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
                for (int i = 0; i < updated.length; i++) model.setValueAt(updated[i], modelRow, i);
                Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                VendorManager.getInstance().updateVendor(v);
            }
        });

        del.addActionListener(e -> deleteSelectedVendor());

        vendorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = vendorTable.rowAtPoint(e.getPoint());
                    if (row == -1) return;
                    int modelRow = vendorTable.convertRowIndexToModel(row);
                    Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
                    Object[] updated = showUpdateVendorDialog(existing);
                    if (updated != null) {
                        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
                        for (int i = 0; i < updated.length; i++) model.setValueAt(updated[i], modelRow, i);
                        Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                        v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
                        VendorManager.getInstance().updateVendor(v);
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
                    int row = vendorTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        vendorTable.setRowSelectionInterval(row, row);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
    }

    private void deleteSelectedVendor() {
        int sel = vendorTable.getSelectedRow();
        if (sel == -1) return;
        int modelRow = vendorTable.convertRowIndexToModel(sel);
        String name = (String) vendorTable.getModel().getValueAt(modelRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Lieferanten wirklich löschen?", "Löschen",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, Main.iconSmall);
        if (confirm == JOptionPane.YES_OPTION) {
            if (VendorManager.getInstance().deleteVendor(name)) {
                ((DefaultTableModel) vendorTable.getModel()).removeRow(modelRow);
            } else {
                JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            }
        }
    }

    private void initializeVendorTable() {
        // Column names must match the row data order used in loadVendors()
        // Add Unicode symbols to the column headers
        String[] cols = new String[]{
                UnicodeSymbols.TRUCK + " Name",
                UnicodeSymbols.PERSON + " Kontakt",
                UnicodeSymbols.PHONE + " Telefon",
                UnicodeSymbols.EMAIL + " Email",
                UnicodeSymbols.ADDRESS + " Adresse",
                UnicodeSymbols.PACKAGE + " Gelieferte Artikel",
                UnicodeSymbols.MONEY + " Mindestbestellwert"
        };
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        vendorTable.setModel(model);

        // visuals
        vendorTable.setRowHeight(26);
        vendorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vendorTable.setShowGrid(true);
        vendorTable.setGridColor(ThemeManager.getTableGridColor());
        vendorTable.setIntercellSpacing(new Dimension(1, 1));
        vendorTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        vendorTable.setBackground(ThemeManager.getTableRowEvenColor());
        vendorTable.setForeground(ThemeManager.getTextPrimaryColor());
        vendorTable.setSelectionBackground(ThemeManager.getTableSelectionColor());
        vendorTable.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());

        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumnModel tcm = vendorTable.getColumnModel();
        if (tcm.getColumnCount() > 0) tcm.getColumn(0).setCellRenderer(center);

        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

        // Alternating row colors for readability (subtle)
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            private final Color EVEN = ThemeManager.getTableRowEvenColor();
            private final Color ODD = ThemeManager.getTableRowOddColor();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? EVEN : ODD);
                    c.setForeground(ThemeManager.getTextPrimaryColor());
                } else {
                    c.setBackground(ThemeManager.getTableSelectionColor());
                    c.setForeground(ThemeManager.getTextOnPrimaryColor());
                }
                return c;
            }
        };
        vendorTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Header styling
        JTableHeader header = vendorTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderColor());
        header.setForeground(ThemeManager.getTextOnPrimaryColor());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
    }

    private void adjustColumnWidths() {
        JFrameUtils.adjustColumnWidths(vendorTable, tableScrollPane, baseColumnWidths);
    }

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        Color defaultBg = ThemeManager.getAccentColor();
        Color hoverBg = ThemeManager.getButtonHoverColor(defaultBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(defaultBg);

        button.setBackground(defaultBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(defaultBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2),
                        BorderFactory.createEmptyBorder(9, 19, 9, 19)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(defaultBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(defaultBg.darker(), 1),
                        BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : defaultBg);
            }
        });

        return button;
    }
}
