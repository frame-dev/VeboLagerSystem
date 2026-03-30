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
import ch.framedev.lagersystem.utils.KeyboardShortcutUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
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

    private static final Logger LOGGER = LogManager.getLogger(VendorGUI.class);

    private static final int NAME_COLUMN_INDEX = 0;
    private static final int CONTACT_COLUMN_INDEX = 1;
    private static final int EMAIL_COLUMN_INDEX = 3;
    private static final int ADDRESS_COLUMN_INDEX = 4;
    private static final int ARTICLES_COLUMN_INDEX = 5;
    private static final int MIN_ORDER_COLUMN_INDEX = 6;
    private static final int ARTICLE_PREVIEW_LIMIT = 2;

    private final JTable vendorTable;
    private final JScrollPane tableScrollPane;
    private final JLabel vendorCountLabel = new JLabel();
    private final JLabel tableCountLabel = new JLabel();
    private final int[] baseColumnWidths = new int[]{120, 260, 160, 140, 220, 300, 140};
    private final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("de-CH"));
    private TableRowSorter<DefaultTableModel> sorter;

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
        LOGGER.info("Initializing VendorGUI window");
        setTitle("Lieferant Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setMinimumSize(new Dimension(860, 520));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel topContainer = new JPanel();
        topContainer.setBackground(ThemeManager.getBackgroundColor());
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.setBorder(BorderFactory.createEmptyBorder(14, 14, 10, 14));

        JButton addVendorButton = createRoundedButton(UnicodeSymbols.HEAVY_PLUS + " Lieferant hinzufügen");
        addVendorButton.setToolTipText("Neuen Lieferanten hinzufügen");
        JButton editVendorButton = createRoundedButton(UnicodeSymbols.CODE + " Lieferant bearbeiten");
        editVendorButton.setToolTipText("Ausgewählten Lieferanten bearbeiten");
        JButton deleteVendorButton = createRoundedButton(UnicodeSymbols.TRASH + " Lieferant löschen");
        deleteVendorButton.setToolTipText("Ausgewählten Lieferanten löschen");
        JButton refreshButton = createRoundedButton(UnicodeSymbols.REFRESH + " Aktualisieren");
        refreshButton.setToolTipText("Lieferantenliste aktualisieren");

        JPanel headerWrapper = createHeaderWrapper();
        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }

        topContainer.add(createToolbarCard(addVendorButton, editVendorButton, deleteVendorButton, refreshButton));
        topContainer.add(Box.createVerticalStrut(10));

        JTextField searchField = new JTextField(26);
        styleTextField(searchField);
        searchField.setToolTipText("Tippen zum Filtern – Enter zum Suchen, ESC zum Leeren");

        JButton searchBtn = createSecondaryButton(UnicodeSymbols.SEARCH + " Suchen");
        searchBtn.setToolTipText("Nach Lieferanten suchen");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");
        clearBtn.setToolTipText("Suchfeld leeren");

        topContainer.add(createSearchCard(searchField, searchBtn, clearBtn));

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
        card.add(createTableFooterCard(), BorderLayout.SOUTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        centerWrapper.add(card, gbc);
        add(centerWrapper, BorderLayout.CENTER);

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
                    LOGGER.info("Added vendor '{}'", v.getName());
                    Main.logUtils.addLog(Level.INFO, "Lieferant hinzugefügt: " + v.getName());
                    invalidateVendorsCache();
                    loadVendors(true);
                    new MessageDialog()
                            .setTitle("Erfolg")
                            .setMessage("Lieferant erfolgreich hinzugefügt.")
                            .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                            .display();
                } else {
                    LOGGER.warn("Failed to add vendor '{}'", v.getName());
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
            LOGGER.info("Refreshing vendor table");
            invalidateVendorsCache();
            loadVendors(true);
        });

        sorter = new TableRowSorter<>((DefaultTableModel) vendorTable.getModel());
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
            updateVendorCountLabel();
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
            updateVendorCountLabel();
        });
        searchField.addActionListener(e -> doSearch.run());

        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    sorter.setRowFilter(null);
                    updateVendorCountLabel();
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
        installKeyboardShortcuts(searchField, addVendorButton, editVendorButton, deleteVendorButton, refreshButton, clearBtn);
        SwingUtilities.invokeLater(this::adjustColumnWidths);
    }

    public void display() {
        setVisible(true);
    }

    private JPanel createHeaderWrapper() {
        boolean disableHeader = Main.settings.getProperty("disable_header") != null
                && Main.settings.getProperty("disable_header").equalsIgnoreCase("true");
        if (disableHeader) {
            return null;
        }

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

        styleCountBadge(vendorCountLabel);
        vendorCountLabel.setText("Lieferanten werden geladen...");

        headerPanel.add(headerText, BorderLayout.WEST);
        headerPanel.add(vendorCountLabel, BorderLayout.EAST);

        JPanel headerWrapper = new JPanel(new BorderLayout());
        headerWrapper.setOpaque(false);
        headerWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerWrapper.add(headerPanel, BorderLayout.CENTER);
        return headerWrapper;
    }

    private JPanel createToolbarCard(JButton addVendorButton, JButton editVendorButton, JButton deleteVendorButton,
                                     JButton refreshButton) {
        RoundedPanel toolbar = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        toolbar.add(addVendorButton);
        toolbar.add(editVendorButton);
        toolbar.add(deleteVendorButton);
        toolbar.add(refreshButton);

        JPanel toolbarWrapper = new JPanel(new BorderLayout());
        toolbarWrapper.setOpaque(false);
        toolbarWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarWrapper.add(toolbar, BorderLayout.CENTER);
        return toolbarWrapper;
    }

    private JPanel createSearchCard(JTextField searchField, JButton searchBtn, JButton clearBtn) {
        RoundedPanel searchCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Name, Kontakt oder Artikel):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));

        JPanel leftSearch = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSearch.setOpaque(false);
        leftSearch.add(searchLabel);
        leftSearch.add(searchField);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightActions.setOpaque(false);
        rightActions.add(searchBtn);
        rightActions.add(clearBtn);

        searchCard.add(leftSearch, BorderLayout.CENTER);
        searchCard.add(rightActions, BorderLayout.EAST);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setOpaque(false);
        searchWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchWrapper.add(searchCard, BorderLayout.CENTER);
        return searchWrapper;
    }

    private JPanel createTableFooterCard() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(6, 2, 0, 2));

        JLabel hintLabel = new JLabel("Doppelklick zum Bearbeiten • Artikelspalte zeigt alle Einträge");
        hintLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        hintLabel.setForeground(ThemeManager.getTextSecondaryColor());

        styleCountBadge(tableCountLabel);
        tableCountLabel.setText("Lieferanten werden geladen...");

        footer.add(hintLabel, BorderLayout.WEST);
        footer.add(tableCountLabel, BorderLayout.EAST);
        return footer;
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
        vendorCountLabel.setText("Lieferanten werden geladen...");
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
                    updateVendorCountLabel();
                } catch (java.util.concurrent.ExecutionException ex) {
                    LOGGER.error("Failed to load vendors for VendorGUI", ex);
                    vendorCountLabel.setText("Lieferanten konnten nicht geladen werden");
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
        LOGGER.info("Loaded {} vendors into VendorGUI", vendors.size());
        updateVendorCountLabel();
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
        JMenuItem showArticles = new JMenuItem(UnicodeSymbols.PACKAGE + " Artikel anzeigen");
        JMenuItem edit = new JMenuItem(UnicodeSymbols.CODE + " Bearbeiten");
        JMenuItem del = new JMenuItem(UnicodeSymbols.TRASH + " Löschen");
        popup.add(showArticles);
        popup.add(edit);
        popup.add(del);

        showArticles.setToolTipText("Alle verknüpften Artikel dieses Lieferanten anzeigen");
        edit.setToolTipText("Ausgewählten Lieferanten bearbeiten");
        del.setToolTipText("Ausgewählten Lieferanten löschen");

        showArticles.addActionListener(e -> showSelectedVendorArticles());
        edit.addActionListener(e -> editSelectedVendor(false));
        del.addActionListener(e -> deleteSelectedVendor());

        vendorTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = vendorTable.rowAtPoint(e.getPoint());
                    int column = vendorTable.columnAtPoint(e.getPoint());
                    if (row == -1) {
                        return;
                    }
                    vendorTable.setRowSelectionInterval(row, row);
                    if (column == ARTICLES_COLUMN_INDEX) {
                        showSelectedVendorArticles();
                    } else {
                        editSelectedVendor(false);
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
                        Vendor vendor = getVendorForModelRow(vendorTable.convertRowIndexToModel(row));
                        showArticles.setEnabled(vendor != null && !getSuppliedArticleDisplayEntries(vendor).isEmpty());
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
        LOGGER.info("Disposing VendorGUI window");
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void installKeyboardShortcuts(JTextField searchField, JButton addVendorButton, JButton editVendorButton,
                                          JButton deleteVendorButton, JButton refreshButton, JButton clearButton) {
        JRootPane rootPane = getRootPane();
        KeyboardShortcutUtils.addTooltipHint(searchField, KeyboardShortcutUtils.menuKey(KeyEvent.VK_F));
        KeyboardShortcutUtils.addTooltipHint(addVendorButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_N));
        KeyboardShortcutUtils.addTooltipHint(editVendorButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_E));
        KeyboardShortcutUtils.addTooltipHint(deleteVendorButton, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        KeyboardShortcutUtils.addTooltipHint(refreshButton, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        KeyboardShortcutUtils.addTooltipHint(clearButton, KeyboardShortcutUtils.menuKey(KeyEvent.VK_L));
        KeyboardShortcutUtils.registerClose(this);
        KeyboardShortcutUtils.registerFocus(rootPane, "vendors.focusSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_F), searchField);
        KeyboardShortcutUtils.registerButton(rootPane, "vendors.add",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_N), addVendorButton);
        KeyboardShortcutUtils.registerButton(rootPane, "vendors.edit",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_E), editVendorButton);
        KeyboardShortcutUtils.registerButton(rootPane, "vendors.delete",
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), deleteVendorButton, true);
        KeyboardShortcutUtils.registerButton(rootPane, "vendors.refresh",
                KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), refreshButton);
        KeyboardShortcutUtils.registerButton(rootPane, "vendors.clearSearch",
                KeyboardShortcutUtils.menuKey(KeyEvent.VK_L), clearButton);
        KeyboardShortcutUtils.register(rootPane, "vendors.showArticles",
                KeyboardShortcutUtils.menuShiftKey(KeyEvent.VK_A), this::showSelectedVendorArticles);
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
            LOGGER.info("Updated vendor '{}'", vendor.getName());
            Main.logUtils.addLog(Level.INFO, "Lieferant aktualisiert: " + vendor.getName());
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
            LOGGER.warn("Failed to update vendor '{}'", vendor.getName());
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
                LOGGER.info("Deleted vendor '{}'", name);
                Main.logUtils.addLog(Level.INFO, "Lieferant gelöscht: " + name);
                invalidateVendorsCache();
                loadVendors(true);
                new MessageDialog()
                        .setTitle("Erfolg")
                        .setMessage("Lieferant erfolgreich gelöscht.")
                        .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                        .display();
            } else {
                LOGGER.warn("Failed to delete vendor '{}'", name);
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

        vendorTable.setRowHeight(38);
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
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                label.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));
                int modelRow = table.convertRowIndexToModel(row);
                Vendor vendor = getVendorForModelRow(modelRow);
                label.setText(buildArticlesCellText(vendor, value, isSelected));
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

    private void styleCountBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    private void updateVendorCountLabel() {
        int total = vendorTable.getModel().getRowCount();
        int shown = sorter != null ? sorter.getViewRowCount() : total;
        String text = "Lieferanten: " + shown + " / " + total;
        vendorCountLabel.setText(text);
        tableCountLabel.setText(text);
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

    private void showSelectedVendorArticles() {
        int selectedRow = vendorTable.getSelectedRow();
        if (selectedRow < 0) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte wählen Sie einen Lieferanten aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        int modelRow = vendorTable.convertRowIndexToModel(selectedRow);
        Vendor vendor = getVendorForModelRow(modelRow);
        if (vendor == null) {
            showVendorNotFoundMessage();
            return;
        }

        showVendorArticlesDialog(vendor);
    }

    private void showVendorArticlesDialog(Vendor vendor) {
        List<String> entries = new ArrayList<>(getSuppliedArticleDisplayEntries(vendor));
        if (entries.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Artikel")
                    .setMessage("Für diesen Lieferanten sind keine Artikel hinterlegt.")
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
            return;
        }

        JDialog dialog = new JDialog(this, UnicodeSymbols.PACKAGE + " Gelieferte Artikel", true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getContentPane().setBackground(ThemeManager.getBackgroundColor());

        RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        headerPanel.setLayout(new BorderLayout(10, 6));
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLabel = new JLabel(UnicodeSymbols.PACKAGE + " " + safeStr(vendor.getName()));
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitleLabel = new JLabel(entries.size() + " verknüpfte Artikel");
        subtitleLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JLabel countBadge = new JLabel();
        styleCountBadge(countBadge);
        countBadge.setText("Angezeigt: " + entries.size() + " / " + entries.size());

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(titleLabel);
        headerText.add(Box.createVerticalStrut(3));
        headerText.add(subtitleLabel);

        headerPanel.add(headerText, BorderLayout.WEST);
        headerPanel.add(countBadge, BorderLayout.EAST);

        JTextField searchField = new JTextField(22);
        styleTextField(searchField);
        searchField.setToolTipText("Artikelnummer oder Namen filtern");

        JButton clearSearchButton = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");

        RoundedPanel filterCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        filterCard.setLayout(new BorderLayout(8, 0));
        filterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Artikel filtern");
        searchLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JPanel searchLeftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchLeftPanel.setOpaque(false);
        searchLeftPanel.add(searchLabel);
        searchLeftPanel.add(searchField);

        JPanel searchRightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchRightPanel.setOpaque(false);
        searchRightPanel.add(clearSearchButton);

        filterCard.add(searchLeftPanel, BorderLayout.WEST);
        filterCard.add(searchRightPanel, BorderLayout.EAST);

        DefaultListModel<String> listModel = new DefaultListModel<>();
        Runnable refreshList = () -> {
            listModel.clear();
            String query = safeStr(searchField.getText()).trim().toLowerCase();
            for (String entry : entries) {
                if (query.isEmpty() || entry.toLowerCase().contains(query)) {
                    listModel.addElement(entry);
                }
            }
            countBadge.setText("Angezeigt: " + listModel.size() + " / " + entries.size());
        };
        refreshList.run();

        JList<String> articleList = new JList<>(listModel);
        articleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        articleList.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        articleList.setBackground(ThemeManager.getCardBackgroundColor());
        articleList.setForeground(ThemeManager.getTextPrimaryColor());
        articleList.setFixedCellHeight(42);
        articleList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String text = safeStr(value);
                String number = text;
                String details = "";
                int splitIndex = text.indexOf(" (");
                if (splitIndex > 0 && text.endsWith(")")) {
                    number = text.substring(0, splitIndex).trim();
                    details = text.substring(splitIndex + 1).trim();
                }
                label.setText(details.isBlank()
                        ? "<html><b>" + escapeHtml(number) + "</b></html>"
                        : "<html><b>" + escapeHtml(number) + "</b><br><font color='" +
                        toHtmlColor(isSelected ? ThemeManager.getTextOnPrimaryColor() : ThemeManager.getTextSecondaryColor()) +
                        "'>" + escapeHtml(details) + "</font></html>");
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (!isSelected) {
                    label.setBackground(index % 2 == 0
                            ? ThemeManager.getTableRowEvenColor()
                            : ThemeManager.getTableRowOddColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                } else {
                    label.setBackground(ThemeManager.getTableSelectionColor());
                    label.setForeground(ThemeManager.getTextOnPrimaryColor());
                }
                return label;
            }
        });
        articleList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    copyArticleListSelection(articleList.getSelectedValue());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(articleList);
        scrollPane.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        scrollPane.getViewport().setBackground(ThemeManager.getCardBackgroundColor());

        JLabel helperLabel = new JLabel("Doppelklick kopiert einen Eintrag");
        helperLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        helperLabel.setForeground(ThemeManager.getTextSecondaryColor());

        JButton copySelectedButton = createSecondaryButton(UnicodeSymbols.COPY + " Auswahl kopieren");
        copySelectedButton.addActionListener(e -> copyArticleListSelection(articleList.getSelectedValue()));

        JButton copyAllButton = createSecondaryButton(UnicodeSymbols.DOCUMENT + " Alle kopieren");
        copyAllButton.addActionListener(e -> copyArticleListSelection(String.join(System.lineSeparator(), entries)));

        JButton closeButton = createSecondaryButton(UnicodeSymbols.CLOSE + " Schließen");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.add(helperLabel, BorderLayout.WEST);

        JPanel bottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomButtons.setOpaque(false);
        bottomButtons.add(copySelectedButton);
        bottomButtons.add(copyAllButton);
        bottomButtons.add(closeButton);
        bottomPanel.add(bottomButtons, BorderLayout.EAST);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refreshList.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refreshList.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refreshList.run();
            }
        });
        clearSearchButton.addActionListener(e -> searchField.setText(""));
        searchField.addActionListener(e -> refreshList.run());
        searchField.registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED
        );

        JPanel contentPanel = new JPanel(new BorderLayout(0, 10));
        contentPanel.setBackground(ThemeManager.getBackgroundColor());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(filterCard, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.setContentPane(contentPanel);
        dialog.getRootPane().setDefaultButton(closeButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.setSize(620, 500);
        dialog.setMinimumSize(new Dimension(500, 360));
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(searchField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private String formatSuppliedArticlesPreview(Vendor vendor) {
        List<String> displayEntries = getSuppliedArticleDisplayEntries(vendor);
        if (displayEntries.isEmpty()) {
            return "Keine Artikel";
        }

        String summary = displayEntries.size() + " Artikel";
        if (displayEntries.size() <= ARTICLE_PREVIEW_LIMIT) {
            return summary + " • " + String.join(", ", displayEntries);
        }

        List<String> preview = new ArrayList<>();
        for (int i = 0; i < ARTICLE_PREVIEW_LIMIT; i++) {
            preview.add(displayEntries.get(i));
        }
        int remaining = displayEntries.size() - ARTICLE_PREVIEW_LIMIT;
        return summary + " • " + String.join(", ", preview) + " +" + remaining;
    }

    private String buildArticlesCellText(Vendor vendor, Object value, boolean isSelected) {
        List<String> displayEntries = getSuppliedArticleDisplayEntries(vendor);
        if (displayEntries.isEmpty()) {
            String fallback = safeStr(value).trim();
            if (fallback.isEmpty()) {
                fallback = "Keine Artikel";
            }
            return fallback;
        }

        String summary = displayEntries.size() + " " + (displayEntries.size() == 1 ? "Artikel" : "Artikel");
        String preview;
        if (displayEntries.size() <= ARTICLE_PREVIEW_LIMIT) {
            preview = String.join(", ", displayEntries);
        } else {
            List<String> previewEntries = new ArrayList<>();
            for (int i = 0; i < ARTICLE_PREVIEW_LIMIT; i++) {
                previewEntries.add(displayEntries.get(i));
            }
            preview = String.join(", ", previewEntries) + " +" + (displayEntries.size() - ARTICLE_PREVIEW_LIMIT);
        }

        String previewColor = isSelected
                ? toHtmlColor(ThemeManager.getTextOnPrimaryColor())
                : toHtmlColor(ThemeManager.getTextSecondaryColor());
        return "<html><b>" + escapeHtml(summary) + "</b><br><font color='" + previewColor + "'>"
                + escapeHtml(preview) + "</font></html>";
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

        StringBuilder builder = new StringBuilder("<html><b>Gelieferte Artikel</b> (" + displayEntries.size() + ")<br><br>");
        for (String entry : displayEntries) {
            builder.append("&#8226; ").append(escapeHtml(entry)).append("<br>");
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

    private void copyArticleListSelection(String text) {
        String value = safeStr(text).trim();
        if (value.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Auswahl")
                    .setMessage("Bitte wählen Sie einen Artikeleintrag aus.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(value), null);
    }

    private static String toHtmlColor(Color color) {
        Color resolved = color == null ? Color.BLACK : color;
        return String.format("#%02x%02x%02x", resolved.getRed(), resolved.getGreen(), resolved.getBlue());
    }
}
