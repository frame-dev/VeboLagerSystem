package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.dialogs.VendorDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.VendorManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.JFrameUtils.RoundedPanel;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static ch.framedev.lagersystem.utils.JFrameUtils.*;

/**
 * VendorGUI with GUI-level caching.
 * - Caches vendor list for faster reloads (TTL-based)
 * - Invalidates cache on add/edit/delete/refresh
 */
@SuppressWarnings("DuplicatedCode")
public class VendorGUI extends JFrame {

    private final JTable vendorTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{120, 260, 160, 140, 220, 300, 140}; // include min order value col

    // ---- Vendor cache (GUI-level) ------------------------------------------
    private static final long VENDOR_CACHE_TTL_MS = 30_000; // 30s GUI cache
    private volatile List<Vendor> vendorsCache = null;
    private volatile long vendorsCacheTime = 0L;

    // Fast lookup by vendor name (optional but useful)
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    private final ConcurrentHashMap<String, Vendor> vendorByNameCache = new ConcurrentHashMap<>();

    public VendorGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();
        setTitle("Lieferant Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== Top area (Header + Toolbar) =====
        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JPanel headerWrapper = null;
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (!disableHeader) {
        // Header card (no fixed height -> prevents clipping)
        RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setLayout(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)
        ));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(UnicodeSymbols.TRUCK + " Lieferant Verwaltung");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Lieferanten verwalten, suchen und bearbeiten");
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

        headerPanel.add(headerText, BorderLayout.WEST);

        headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);
    }

        // Toolbar card
        RoundedPanel toolbar = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

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

        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.setOpaque(false);
        toolbarWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarWrapper.add(toolbar, BorderLayout.SOUTH);

        if(headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }
        topContainer.add(toolbarWrapper);

        add(topContainer, BorderLayout.NORTH);

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

        // ===== Bottom search bar (card – like ClientGUI) =====
        RoundedPanel searchCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Name oder Kontakt):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        JTextField searchField = new JTextField(26);
        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);
        leftSearch.add(searchLabel);
        leftSearch.add(searchField);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);

        JButton searchBtn = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Nach Lieferanten suchen");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");
        clearBtn.setToolTipText("Suchfeld leeren");

        rightActions.add(searchBtn);
        rightActions.add(clearBtn);

        searchCard.add(leftSearch, BorderLayout.CENTER);
        searchCard.add(rightActions, BorderLayout.EAST);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(ThemeManager.getBackgroundColor());
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        searchWrapper.add(searchCard, BorderLayout.CENTER);

        add(searchWrapper, BorderLayout.SOUTH);

        // load vendors (cached)
        loadVendors(false);

        // Setup interactions (context menu, double click)
        setupTableInteractions();

        // wiring: add/edit/delete
        addVendorButton.addActionListener(e -> {
            Object[] row = showAddVendorDialog();
            if (row != null) {
                Vendor v = vendorFromDialogRow(row);
                if (v == null) return;

                if (VendorManager.getInstance().insertVendor(v)) {
                    invalidateVendorsCache();
                    loadVendors(true);
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich hinzugefügt.", "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler: Lieferant bereits vorhanden oder Insert fehlgeschlagen",
                            "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        editVendorButton.addActionListener(e -> {
            int sel = vendorTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Lieferanten zum Bearbeiten aus.",
                        "Keine Auswahl", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
                return;
            }
            int modelRow = vendorTable.convertRowIndexToModel(sel);
            Object[] existing = ((DefaultTableModel) vendorTable.getModel()).getDataVector().elementAt(modelRow).toArray();
            Object[] updated = showUpdateVendorDialog(existing);
            if (updated != null) {
                Vendor v = vendorFromDialogRow(updated);
                if (v == null) return;

                if (VendorManager.getInstance().updateVendor(v)) {
                    invalidateVendorsCache();
                    loadVendors(true);
                    JOptionPane.showMessageDialog(this, "Lieferant erfolgreich aktualisiert.", "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Lieferanten.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        deleteVendorButton.addActionListener(e -> deleteSelectedVendor());

        refreshButton.addActionListener(e -> {
            invalidateVendorsCache();
            loadVendors(true);
            JOptionPane.showMessageDialog(this, "Lieferantenliste wurde aktualisiert.", "Aktualisiert",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        // ===== Sorting/Filtering (Vendor search) =====
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) vendorTable.getModel());
        vendorTable.setRowSorter(sorter);

        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                String regex = "(?i)" + Pattern.quote(text);
                sorter.setRowFilter(RowFilter.regexFilter(regex, 0, 1));
            }
        };

        // Live filter while typing
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { doSearch.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { doSearch.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { doSearch.run(); }
        });

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
        });
        searchField.addActionListener(e -> doSearch.run());

        // ESC clears
        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    sorter.setRowFilter(null);
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED
        );

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

    // -------------------- Cache helpers --------------------

    private boolean isVendorsCacheValid() {
        return vendorsCache != null && (System.currentTimeMillis() - vendorsCacheTime) < VENDOR_CACHE_TTL_MS;
    }

    private void invalidateVendorsCache() {
        vendorsCache = null;
        vendorsCacheTime = 0L;
        vendorByNameCache.clear();
    }

    private List<Vendor> getVendorsCached(boolean forceReload) {
        if (!forceReload && isVendorsCacheValid()) {
            return vendorsCache;
        }

        List<Vendor> fresh = VendorManager.getInstance().getVendors();
        if (fresh == null) {
            return vendorsCache != null ? vendorsCache : java.util.Collections.emptyList();
        }

        vendorsCache = fresh;
        vendorsCacheTime = System.currentTimeMillis();

        vendorByNameCache.clear();
        for (Vendor v : fresh) {
            if (v != null && v.getName() != null) {
                vendorByNameCache.put(v.getName(), v);
            }
        }

        return fresh;
    }

    // -------------------- UI helpers --------------------

    private void styleTextField(JTextField tf) {
        if(tf == null) throw new IllegalArgumentException("tf must not be null");
        JFrameUtils.styleTextField(tf);
    }

    // -------------------- Data loading --------------------

    private void loadVendors(boolean forceReload) {
        List<Vendor> vendors = getVendorsCached(forceReload);

        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
        model.setRowCount(0);

        for (Vendor v : vendors) {
            model.addRow(new Object[]{
                    v.getName(),
                    v.getContactPerson(),
                    v.getPhoneNumber(),
                    v.getEmail(),
                    v.getAddress(),
                    v.getSuppliedArticles() != null ? String.join(",", v.getSuppliedArticles()) : "",
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
        if (existing == null) throw new IllegalArgumentException("existing must not be null");
        return VendorDialog.showUpdateVendorDialog(this, existing);
    }

    // -------------------- Interactions --------------------

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
                Vendor v = vendorFromDialogRow(updated);
                if (v == null) return;

                if (VendorManager.getInstance().updateVendor(v)) {
                    invalidateVendorsCache();
                    loadVendors(true);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Aktualisieren des Lieferanten.",
                            "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
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
                        Vendor v = vendorFromDialogRow(updated);
                        if (v == null) return;

                        if (VendorManager.getInstance().updateVendor(v)) {
                            invalidateVendorsCache();
                            loadVendors(true);
                        } else {
                            JOptionPane.showMessageDialog(VendorGUI.this, "Fehler beim Aktualisieren des Lieferanten.",
                                    "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                        }
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

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void deleteSelectedVendor() {
        int sel = vendorTable.getSelectedRow();
        if (sel == -1) {
            JOptionPane.showMessageDialog(null, "Wählen sie einen Lieferanten in der Tabelle zum löschen aus.");
            return;
        }

        int modelRow = vendorTable.convertRowIndexToModel(sel);
        String name = (String) vendorTable.getModel().getValueAt(modelRow, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Lieferanten wirklich löschen?",
                "Löschen", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, Main.iconSmall);

        if (confirm == JOptionPane.YES_OPTION) {
            if (VendorManager.getInstance().deleteVendor(name)) {
                invalidateVendorsCache();
                loadVendors(true);
                JOptionPane.showMessageDialog(this, "Lieferant erfolgreich gelöscht.", "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
            } else {
                JOptionPane.showMessageDialog(this, "Löschen fehlgeschlagen.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            }
        }
    }

    // -------------------- Table setup --------------------

    private void initializeVendorTable() {
        // Column names must match the row data order used in loadVendors()
        DefaultTableModel model = getModel();
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
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
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

    private static DefaultTableModel getModel() {
        String[] cols = new String[]{
                UnicodeSymbols.TRUCK + " Name",
                UnicodeSymbols.PERSON + " Kontakt",
                UnicodeSymbols.PHONE + " Telefon",
                UnicodeSymbols.EMAIL + " Email",
                UnicodeSymbols.ADDRESS + " Adresse",
                UnicodeSymbols.PACKAGE + " Gelieferte Artikel",
                UnicodeSymbols.MONEY + " Mindestbestellwert"
        };

        return new DefaultTableModel(cols, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                // keep String for sorter simplicity; min order value is still displayed fine
                return String.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private void adjustColumnWidths() {
        JFrameUtils.adjustColumnWidths(vendorTable, tableScrollPane, baseColumnWidths);
    }

    // -------------------- Vendor mapping --------------------

    /**
     * Converts the VendorDialog Object[] into a Vendor instance.
     * Expected row format (based on your usage):
     * 0 name
     * 1 contactPerson
     * 2 phone
     * 3 email
     * 4 address
     * 5 suppliedArticles (comma separated)
     * 6 minOrderValue (double or string)
     */
    private Vendor vendorFromDialogRow(Object[] row) {
        if (row == null || row.length < 7) return null;

        String name = safeStr(row[0]);
        String contact = safeStr(row[1]);
        String phone = safeStr(row[2]);
        String email = safeStr(row[3]);
        String address = safeStr(row[4]);
        String supplied = safeStr(row[5]);
        double minOrder = safeDouble(row[6]);

        Vendor v = new Vendor(name, contact, phone, email, address, minOrder);
        v.setSuppliedArticles(Arrays.asList(supplied.split("\\s*,\\s*")));
        return v;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double safeDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(o).trim().replace(",", "."));
        } catch (Exception ex) {
            return 0.0;
        }
    }
}