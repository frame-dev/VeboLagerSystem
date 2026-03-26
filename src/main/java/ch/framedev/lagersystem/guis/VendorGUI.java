package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.dialogs.VendorDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.VendorManager;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    private static final int NAME_COLUMN_INDEX = 0;
    private static final int CONTACT_COLUMN_INDEX = 1;
    private static final int EMAIL_COLUMN_INDEX = 3;
    private static final int ADDRESS_COLUMN_INDEX = 4;
    private static final int ARTICLES_COLUMN_INDEX = 5;
    private static final int MIN_ORDER_COLUMN_INDEX = 6;
    private static final int ARTICLE_PREVIEW_LIMIT = 2;

    private final JTable vendorTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{120, 260, 160, 140, 220, 300, 140};
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("de-CH"));

    // ---- Vendor cache (GUI-level) ------------------------------------------
    private static final long VENDOR_CACHE_TTL_MS = 30_000; // 30s GUI cache
    private volatile List<Vendor> vendorsCache = null;
    private volatile long vendorsCacheTime = 0L;
    private SwingWorker<List<Vendor>, Void> vendorLoadWorker;
    private int vendorLoadGeneration = 0;

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

        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }
        topContainer.add(toolbarWrapper);

        add(topContainer, BorderLayout.NORTH);

        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        vendorTable = new JTable() {
            @Override
            public String getToolTipText(MouseEvent event) {
                int viewRow = rowAtPoint(event.getPoint());
                int viewColumn = columnAtPoint(event.getPoint());
                if (viewRow < 0 || viewColumn < 0) {
                    return null;
                }

                int modelRow = convertRowIndexToModel(viewRow);
                int modelColumn = convertColumnIndexToModel(viewColumn);
                Vendor vendor = getVendorForModelRow(modelRow);
                if (vendor == null) {
                    Object value = getModel().getValueAt(modelRow, modelColumn);
                    String text = safeStr(value).trim();
                    return text.isEmpty() ? null : text;
                }

                return getVendorToolTip(vendor, modelColumn);
            }
        };
        initializeVendorTable();
        tableScrollPane = new JScrollPane(vendorTable);
        tableScrollPane.setBackground(ThemeManager.getCardBackgroundColor());
        tableScrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        card.add(tableScrollPane, BorderLayout.CENTER);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(card, gbc);
        add(centerWrapper, BorderLayout.CENTER);

        RoundedPanel searchCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Name, Kontakt oder Artikel):");
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

        loadVendors(false);
        setupTableInteractions();

        addVendorButton.addActionListener(e -> {
            Object[] row = showAddVendorDialog();
            if (row != null) {
                Vendor v = vendorFromDialogRow(row);
                if (v == null) {
                    return;
                }

                if (VendorManager.getInstance().insertVendor(v)) {
                    invalidateVendorsCache();
                    loadVendors(true);
                    new MessageDialog()
                            .setTitle("Erfolg")
                            .setMessage("Lieferant erfolgreich hinzugefügt.")
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .display();
                } else {
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Lieferant bereits vorhanden oder Insert fehlgeschlagen")
                            .setMessageType(JOptionPane.ERROR_MESSAGE)
                            .display();
                }
            }
        });

        editVendorButton.addActionListener(e -> editSelectedVendor(true));
        deleteVendorButton.addActionListener(e -> deleteSelectedVendor());

        refreshButton.addActionListener(e -> {
            invalidateVendorsCache();
            loadVendors(true);
        });

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) vendorTable.getModel());
        vendorTable.setRowSorter(sorter);

        Runnable doSearch = () -> {
            String text = searchField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                String regex = "(?i)" + Pattern.quote(text);
                sorter.setRowFilter(RowFilter.regexFilter(regex,
                        NAME_COLUMN_INDEX,
                        CONTACT_COLUMN_INDEX,
                        EMAIL_COLUMN_INDEX,
                        ADDRESS_COLUMN_INDEX,
                        ARTICLES_COLUMN_INDEX));
            }
        };

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
            sorter.setRowFilter(null);
        });
        searchField.addActionListener(e -> doSearch.run());

        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    sorter.setRowFilter(null);
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED
        );

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

    public void display() {
        setVisible(true);
    }

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

    private void styleTextField(JTextField tf) {
        if (tf == null) {
            throw new IllegalArgumentException("tf must not be null");
        }
        JFrameUtils.styleTextField(tf);
    }

    private void loadVendors(boolean forceReload) {
        int loadGeneration = ++vendorLoadGeneration;
        if (vendorLoadWorker != null && !vendorLoadWorker.isDone()) {
            vendorLoadWorker.cancel(true);
        }
        vendorLoadWorker = new SwingWorker<>() {
            @Override
            protected List<Vendor> doInBackground() {
                return getVendorsCached(forceReload);
            }

            @Override
            protected void done() {
                if (isCancelled() || loadGeneration != vendorLoadGeneration) {
                    return;
                }
                try {
                    populateVendors(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException ex) {
                    // Keep the current table contents if loading fails.
                }
            }
        };
        vendorLoadWorker.execute();
    }

    private void populateVendors(List<Vendor> vendors) {
        DefaultTableModel model = (DefaultTableModel) vendorTable.getModel();
        model.setRowCount(0);

        for (Vendor v : vendors) {
            model.addRow(new Object[]{
                    safeStr(v.getName()),
                    safeStr(v.getContactPerson()),
                    safeStr(v.getPhoneNumber()),
                    safeStr(v.getEmail()),
                    safeStr(v.getAddress()),
                    formatSuppliedArticlesPreview(v),
                    v.getMinOrderValue()
            });
        }
    }

    private Object[] showAddVendorDialog() {
        return VendorDialog.showAddVendorDialog(this);
    }

    private Object[] showUpdateVendorDialog(Object[] existing) {
        if (existing == null) {
            throw new IllegalArgumentException("existing must not be null");
        }
        return VendorDialog.showUpdateVendorDialog(this, existing);
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        JMenuItem del = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");
        popup.add(edit);
        popup.add(del);

        edit.setToolTipText("Ausgewählten Lieferanten bearbeiten");
        del.setToolTipText("Ausgewählten Lieferanten löschen");

        edit.addActionListener(e -> editSelectedVendor(false));
        del.addActionListener(e -> deleteSelectedVendor());

        vendorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = vendorTable.rowAtPoint(e.getPoint());
                    if (row == -1) {
                        return;
                    }
                    vendorTable.setRowSelectionInterval(row, row);
                    editSelectedVendor(false);
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
        if (vendorLoadWorker != null && !vendorLoadWorker.isDone()) {
            vendorLoadWorker.cancel(true);
        }
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void editSelectedVendor(boolean showSuccessDialog) {
        int sel = vendorTable.getSelectedRow();
        if (sel == -1) {
            if (showSuccessDialog) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie einen Lieferanten zum Bearbeiten aus.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
            }
            return;
        }

        int modelRow = vendorTable.convertRowIndexToModel(sel);
        Object[] existing = getVendorDialogDataForModelRow(modelRow);
        if (existing == null) {
            showVendorNotFoundMessage();
            return;
        }

        Object[] updated = showUpdateVendorDialog(existing);
        if (updated == null) {
            return;
        }

        Vendor vendor = vendorFromDialogRow(updated);
        if (vendor == null) {
            return;
        }

        if (VendorManager.getInstance().updateVendor(vendor)) {
            invalidateVendorsCache();
            loadVendors(true);
            if (showSuccessDialog) {
                new MessageDialog()
                        .setTitle("Erfolg")
                        .setMessage("Lieferant erfolgreich aktualisiert.")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
            }
        } else {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Aktualisieren des Lieferanten.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    private void deleteSelectedVendor() {
        int sel = vendorTable.getSelectedRow();
        if (sel == -1) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Wählen sie einen Lieferanten in der Tabelle zum löschen aus.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        int modelRow = vendorTable.convertRowIndexToModel(sel);
        String name = safeStr(vendorTable.getModel().getValueAt(modelRow, NAME_COLUMN_INDEX));

        int confirm = new MessageDialog()
                .setTitle("Löschen")
                .setMessage("Möchten Sie diesen Lieferanten wirklich löschen?")
                .setMessageType(JOptionPane.WARNING_MESSAGE)
                .displayWithOptions();

        if (confirm == JOptionPane.YES_OPTION) {
            if (VendorManager.getInstance().deleteVendor(name)) {
                invalidateVendorsCache();
                loadVendors(true);
                new MessageDialog()
                        .setTitle("Erfolg")
                        .setMessage("Lieferant erfolgreich gelöscht.")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
            } else {
                new MessageDialog()
                        .setTitle("Fehler")
                        .setMessage("Löschen fehlgeschlagen.")
                        .setMessageType(JOptionPane.ERROR_MESSAGE)
                        .display();
            }
        }
    }

    private void initializeVendorTable() {
        DefaultTableModel model = getModel();
        vendorTable.setModel(model);

        vendorTable.setRowHeight(26);
        vendorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        vendorTable.getTableHeader().setReorderingAllowed(false);
        vendorTable.setShowGrid(true);
        vendorTable.setGridColor(ThemeManager.getTableGridColor());
        vendorTable.setIntercellSpacing(new Dimension(1, 1));
        vendorTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
        vendorTable.setBackground(ThemeManager.getTableRowEvenColor());
        vendorTable.setForeground(ThemeManager.getTextPrimaryColor());
        vendorTable.setSelectionBackground(ThemeManager.getTableSelectionColor());
        vendorTable.setSelectionForeground(ThemeManager.getTextOnPrimaryColor());

        TableColumnModel tcm = vendorTable.getColumnModel();
        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }

        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                return label;
            }
        };
        vendorTable.setDefaultRenderer(Object.class, alternatingRenderer);

        DefaultTableCellRenderer nameRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setFont(SettingsGUI.getFontByName(Font.BOLD, SettingsGUI.TABLE_FONT_SIZE));
                return label;
            }
        };

        DefaultTableCellRenderer articlesRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                if (!isSelected) {
                    label.setForeground(ThemeManager.getTextSecondaryColor());
                }
                return label;
            }
        };

        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                applyTableColors(label, isSelected, row);
                label.setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof Number) {
                    Number number = (Number) value;
                    label.setText(currencyFormat.format(number.doubleValue()));
                } else {
                    label.setText(value == null ? "" : String.valueOf(value));
                }
                return label;
            }
        };

        if (tcm.getColumnCount() > NAME_COLUMN_INDEX) {
            tcm.getColumn(NAME_COLUMN_INDEX).setCellRenderer(nameRenderer);
        }
        if (tcm.getColumnCount() > ARTICLES_COLUMN_INDEX) {
            tcm.getColumn(ARTICLES_COLUMN_INDEX).setCellRenderer(articlesRenderer);
        }
        if (tcm.getColumnCount() > MIN_ORDER_COLUMN_INDEX) {
            tcm.getColumn(MIN_ORDER_COLUMN_INDEX).setCellRenderer(moneyRenderer);
        }

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
                if (columnIndex == MIN_ORDER_COLUMN_INDEX) {
                    return Double.class;
                }
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

    /**
     * Converts the VendorDialog Object[] into a Vendor instance.
     * Expected row format:
     * 0 name
     * 1 contactPerson
     * 2 phone
     * 3 email
     * 4 address
     * 5 suppliedArticles (comma separated)
     * 6 minOrderValue (double or string)
     */
    private Vendor vendorFromDialogRow(Object[] row) {
        if (row == null || row.length < 7) {
            return null;
        }

        String name = safeStr(row[0]);
        String contact = safeStr(row[1]);
        String phone = safeStr(row[2]);
        String email = safeStr(row[3]);
        String address = safeStr(row[4]);
        String supplied = safeStr(row[5]);
        double minOrder = safeDouble(row[6]);

        Vendor vendor = new Vendor(name, contact, phone, email, address, minOrder);
        vendor.setSuppliedArticles(parseSuppliedArticles(supplied));
        return vendor;
    }

    private void applyTableColors(Component component, boolean isSelected, int row) {
        if (!isSelected) {
            component.setBackground(row % 2 == 0
                    ? ThemeManager.getTableRowEvenColor()
                    : ThemeManager.getTableRowOddColor());
            component.setForeground(ThemeManager.getTextPrimaryColor());
        } else {
            component.setBackground(ThemeManager.getTableSelectionColor());
            component.setForeground(ThemeManager.getTextOnPrimaryColor());
        }
    }

    private Vendor getVendorForModelRow(int modelRow) {
        if (modelRow < 0 || modelRow >= vendorTable.getModel().getRowCount()) {
            return null;
        }

        String name = safeStr(vendorTable.getModel().getValueAt(modelRow, NAME_COLUMN_INDEX)).trim();
        if (name.isEmpty()) {
            return null;
        }

        Vendor vendor = vendorByNameCache.get(name);
        if (vendor != null) {
            return vendor;
        }

        Vendor loadedVendor = VendorManager.getInstance().getVendorByName(name);
        if (loadedVendor != null) {
            vendorByNameCache.put(name, loadedVendor);
        }
        return loadedVendor;
    }

    private Object[] getVendorDialogDataForModelRow(int modelRow) {
        Vendor vendor = getVendorForModelRow(modelRow);
        if (vendor == null) {
            return null;
        }

        return new Object[]{
                safeStr(vendor.getName()),
                safeStr(vendor.getContactPerson()),
                safeStr(vendor.getPhoneNumber()),
                safeStr(vendor.getEmail()),
                safeStr(vendor.getAddress()),
                String.join(",", normalizeArticles(vendor.getSuppliedArticles())),
                vendor.getMinOrderValue()
        };
    }

    private void showVendorNotFoundMessage() {
        new MessageDialog()
                .setTitle("Fehler")
                .setMessage("Der Lieferant konnte nicht geladen werden.")
                .setMessageType(JOptionPane.ERROR_MESSAGE)
                .display();
    }

    private String formatSuppliedArticlesPreview(Vendor vendor) {
        List<String> displayEntries = getSuppliedArticleDisplayEntries(vendor);
        if (displayEntries.isEmpty()) {
            return "Keine Artikel";
        }

        if (displayEntries.size() <= ARTICLE_PREVIEW_LIMIT) {
            return String.join(", ", displayEntries);
        }

        List<String> preview = new ArrayList<>();
        for (int i = 0; i < ARTICLE_PREVIEW_LIMIT; i++) {
            preview.add(displayEntries.get(i));
        }
        int remaining = displayEntries.size() - ARTICLE_PREVIEW_LIMIT;
        return String.join(", ", preview) + " +" + remaining + " weitere";
    }

    private String getVendorToolTip(Vendor vendor, int modelColumn) {
        switch (modelColumn) {
            case ARTICLES_COLUMN_INDEX:
                return buildArticlesToolTip(vendor);
            case MIN_ORDER_COLUMN_INDEX:
                return currencyFormat.format(vendor.getMinOrderValue());
            case NAME_COLUMN_INDEX:
                return toToolTipText(vendor.getName());
            case CONTACT_COLUMN_INDEX:
                return toToolTipText(vendor.getContactPerson());
            case EMAIL_COLUMN_INDEX:
                return toToolTipText(vendor.getEmail());
            case ADDRESS_COLUMN_INDEX:
                return toToolTipText(vendor.getAddress());
            default:
                return null;
        }
    }

    private String buildArticlesToolTip(Vendor vendor) {
        List<String> displayEntries = getSuppliedArticleDisplayEntries(vendor);
        if (displayEntries.isEmpty()) {
            return "Keine verknüpften Artikel";
        }

        StringBuilder builder = new StringBuilder("<html><b>Gelieferte Artikel</b><br>");
        for (String entry : displayEntries) {
            builder.append(escapeHtml(entry)).append("<br>");
        }
        builder.append("</html>");
        return builder.toString();
    }

    private List<String> getSuppliedArticleDisplayEntries(Vendor vendor) {
        List<String> normalizedArticles = normalizeArticles(vendor == null ? null : vendor.getSuppliedArticles());
        List<String> displayEntries = new ArrayList<>();

        for (String articleNumber : normalizedArticles) {
            Article article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
            if (article != null && article.getName() != null && !article.getName().isBlank()) {
                displayEntries.add(articleNumber + " (" + article.getName().trim() + ")");
            } else {
                displayEntries.add(articleNumber);
            }
        }

        return displayEntries;
    }

    private static List<String> parseSuppliedArticles(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return java.util.Collections.emptyList();
        }

        String[] parts = supplied.split("\\s*,\\s*");
        List<String> articles = new ArrayList<>();
        for (String part : parts) {
            String trimmed = safeStr(part).trim();
            if (!trimmed.isEmpty()) {
                articles.add(trimmed);
            }
        }
        return articles;
    }

    private static List<String> normalizeArticles(List<String> suppliedArticles) {
        if (suppliedArticles == null || suppliedArticles.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<String> normalized = new ArrayList<>();
        for (String article : suppliedArticles) {
            String trimmed = safeStr(article).trim();
            if (!trimmed.isEmpty()) {
                normalized.add(trimmed);
            }
        }
        return normalized;
    }

    private static String toToolTipText(String text) {
        String normalized = safeStr(text).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String safeStr(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static double safeDouble(Object o) {
        if (o == null) {
            return 0.0;
        }
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o).trim().replace(",", "."));
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private static String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
