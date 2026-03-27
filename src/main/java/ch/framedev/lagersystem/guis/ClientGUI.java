package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.dialogs.ClientDialog;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static ch.framedev.lagersystem.utils.JFrameUtils.createSecondaryButton;

/**
 * The ClientGUI class provides a graphical user interface for managing clients
 * in the inventory system. It allows users to view, add, edit, delete, and
 * filter clients based on their names and associated departments. The GUI is
 * designed with a modern look and feel, utilizing theming from the ThemeManager
 * to ensure consistency across the application. It features a responsive layout
 * that adjusts to window resizing and includes interactive components such as
 * tables, buttons, combo boxes, and search fields for an efficient user
 * experience.
 */
@SuppressWarnings({ "MismatchedQueryAndUpdateOfCollection", "DuplicatedCode" })
public class ClientGUI extends JFrame {

    private static final Logger LOGGER = LogManager.getLogger(ClientGUI.class);

    private final JTable clientTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[] { 300, 300 };
    private final List<Client> clients = new ArrayList<>();
    private final JLabel clientCountLabel = new JLabel();

    private final JComboBox<String> filterDepartmentCombobox;
    private SwingWorker<List<Map<String, String>>, Void> clientLoadWorker;
    private int clientLoadGeneration = 0;

    private static final String ALL_DEPARTMENTS_LABEL = UnicodeSymbols.DEPARTMENT + " Alle Abteilungen";

    /**
     * Initializes the ClientGUI, setting up the layout, components, and event
     * handlers for managing clients. The GUI includes a header, a toolbar with
     * filtering options and action buttons, a main area with a table displaying
     * clients, and a search bar at the bottom. It also loads the initial client
     * data from the database and applies theming to all components.
     */
    public ClientGUI() {
        ThemeManager.getInstance().registerWindow(this);
        LOGGER.info("Initializing ClientGUI window");

        setTitle("Kunden Verwaltung");
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
            JFrameUtils.RoundedPanel headerPanel = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(),
                    20);
            headerPanel.setLayout(new BorderLayout());
            headerPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                    BorderFactory.createEmptyBorder(14, 18, 14, 18)));
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel titleLabel = new JLabel(UnicodeSymbols.CLIENT + " Kunden Verwaltung");
            titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
            titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

            JLabel subtitleLabel = new JLabel(UnicodeSymbols.INFO + " Kunden verwalten, filtern und durchsuchen");
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
        JFrameUtils.RoundedPanel toolbar = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        JLabel filterLabel = new JLabel(UnicodeSymbols.SEARCH + " Nach Abteilung:");
        filterLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        filterLabel.setForeground(ThemeManager.getTextPrimaryColor());

        // Combo used in dialogs (create/edit)
        JComboBox<String> departmentCombobox = new JComboBox<>();
        fillDepartmentList(departmentCombobox, false);
        styleComboBox(departmentCombobox);

        // Combo used as filter
        filterDepartmentCombobox = new JComboBox<>();
        filterDepartmentCombobox.setPreferredSize(new Dimension(240, 40));
        filterDepartmentCombobox.addItem(ALL_DEPARTMENTS_LABEL);
        fillDepartmentList(filterDepartmentCombobox, true);
        styleComboBox(filterDepartmentCombobox);

        JButton addClientButton = createRoundedButton(UnicodeSymbols.HEAVY_PLUS + " Kunde hinzufügen");
        addClientButton.setToolTipText("Einen neuen Kunden zur Datenbank hinzufügen");
        JButton editClientButton = createRoundedButton(UnicodeSymbols.CODE + " Kunde bearbeiten");
        editClientButton.setToolTipText("Den ausgewählten Kunden bearbeiten");
        JButton deleteClientButton = createRoundedButton(UnicodeSymbols.TRASH + " Kunde löschen");
        deleteClientButton.setToolTipText("Den ausgewählten Kunden löschen");
        JButton refreshButton = createRoundedButton(UnicodeSymbols.UPDATE + " Aktualisieren");
        refreshButton.setToolTipText("Die Kundenliste aktualisieren");

        toolbar.add(filterLabel);
        toolbar.add(filterDepartmentCombobox);
        toolbar.add(Box.createHorizontalStrut(6));
        toolbar.add(addClientButton);
        toolbar.add(editClientButton);
        toolbar.add(deleteClientButton);
        toolbar.add(refreshButton);

        JScrollPane toolbarScrollPane = new JScrollPane(
                toolbar,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toolbarScrollPane.setBorder(BorderFactory.createEmptyBorder());
        toolbarScrollPane.setOpaque(false);
        toolbarScrollPane.getViewport().setOpaque(false);
        toolbarScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        toolbarScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (headerWrapper != null) {
            topContainer.add(headerWrapper);
            topContainer.add(Box.createVerticalStrut(10));
        }
        topContainer.add(toolbarScrollPane);

        add(topContainer, BorderLayout.NORTH);

        // ===== Main card with table =====
        JFrameUtils.RoundedPanel card = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        clientTable = new JTable();
        initializeClientTable();

        tableScrollPane = new JScrollPane(clientTable);
        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
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

        // ===== Bottom search bar (card – like VendorGUI) =====
        JFrameUtils.RoundedPanel searchCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        searchCard.setLayout(new BorderLayout(10, 0));
        searchCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Name oder Abteilung):");
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
        searchBtn.setToolTipText("Nach Kunden suchen");
        JButton clearBtn = createSecondaryButton(UnicodeSymbols.CLEAR + " Leeren");
        clearBtn.setToolTipText("Suchfeld leeren");

        styleCountBadge(clientCountLabel);

        rightActions.add(clientCountLabel);
        rightActions.add(Box.createHorizontalStrut(8));
        rightActions.add(searchBtn);
        rightActions.add(clearBtn);

        searchCard.add(leftSearch, BorderLayout.CENTER);
        searchCard.add(rightActions, BorderLayout.EAST);

        JPanel searchWrapper = new JPanel(new BorderLayout());
        searchWrapper.setBackground(ThemeManager.getBackgroundColor());
        searchWrapper.setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
        searchWrapper.add(searchCard, BorderLayout.CENTER);

        add(searchWrapper, BorderLayout.SOUTH);

        // ===== Load clients =====
        loadClients();

        // ===== Sorting/Filtering =====
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) clientTable.getModel());
        clientTable.setRowSorter(sorter);

        filterDepartmentCombobox.addActionListener(listener -> {
            String selected = (String) filterDepartmentCombobox.getSelectedItem();
            if (selected == null || ALL_DEPARTMENTS_LABEL.equals(selected)) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(selected) + "$", 1));
            }
            updateClientCountLabel(sorter);
        });

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
            updateClientCountLabel(sorter);
        };

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
            filterDepartmentCombobox.setSelectedItem(ALL_DEPARTMENTS_LABEL);
            sorter.setRowFilter(null);
            updateClientCountLabel(sorter);
        });
        searchField.addActionListener(e -> doSearch.run());
        // ESC clears
        searchField.registerKeyboardAction(
                e -> {
                    searchField.setText("");
                    filterDepartmentCombobox.setSelectedItem(ALL_DEPARTMENTS_LABEL);
                    sorter.setRowFilter(null);
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_FOCUSED);
        updateClientCountLabel(sorter);

        // ===== Table interactions =====
        setupTableInteractions();

        updateClientCountLabel(sorter);
        // ===== Wire actions =====
        addClientButton.addActionListener(e -> {
            Object[] row = showAddClientDialog();
            if (row != null)
                loadClients();
        });

        editClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie einen Kunden zum Bearbeiten aus.")
                        .display();
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[] {
                    clientTable.getModel().getValueAt(modelRow, 0),
                    clientTable.getModel().getValueAt(modelRow, 1)
            };

            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null)
                loadClients();
        });

        deleteClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                new MessageDialog()
                        .setTitle("Keine Auswahl")
                        .setMessage("Bitte wählen Sie einen Kunden zum Löschen aus.")
                        .display();
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);

            int confirm = new MessageDialog()
                    .setTitle("Löschen bestätigen")
                    .setMessage("Möchten Sie diesen Kunden wirklich löschen?")
                    .setOptionType(JOptionPane.YES_NO_OPTION)
                    .displayWithOptions();

            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    LOGGER.info("Deleted client '{}'", name);
                    Main.logUtils.addLog(Level.INFO, "Kunde gelöscht: " + name);
                    loadClients();
                    new MessageDialog()
                            .setTitle("Erfolg")
                            .setMessage("Kunde erfolgreich gelöscht.")
                            .display();
                } else {
                    LOGGER.warn("Failed to delete client '{}'", name);
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Fehler beim Löschen des Kunden aus der Datenbank.")
                            .display();
                }
            }
        });

        refreshButton.addActionListener(e -> {
            LOGGER.info("Refreshing client table");
            loadClients();
        });

        // ===== Auto resize columns =====
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
        if (clientLoadWorker != null && !clientLoadWorker.isDone()) {
            clientLoadWorker.cancel(true);
        }
        LOGGER.info("Disposing ClientGUI window");
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void fillDepartmentList(JComboBox<String> target, boolean skipFirstItem) {
        if (target == null)
            return;
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        for (var department : departmentManager.getAllDepartments()) {
            String dept = (String) department.get("department");
            if (dept == null)
                continue;
            String trimmed = dept.trim();
            if (trimmed.isEmpty())
                continue;

            // Avoid duplicates (especially when called multiple times)
            boolean exists = false;
            int start = skipFirstItem ? 1 : 0;
            for (int i = start; i < target.getItemCount(); i++) {
                if (trimmed.equals(target.getItemAt(i))) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                target.addItem(trimmed);
            }
        }
    }

    private void loadClients() {
        int loadGeneration = ++clientLoadGeneration;
        if (clientLoadWorker != null && !clientLoadWorker.isDone()) {
            clientLoadWorker.cancel(true);
        }
        clientCountLabel.setText("Kunden werden geladen...");
        clientLoadWorker = new SwingWorker<>() {
            @Override
            protected List<Map<String, String>> doInBackground() {
                List<Map<String, String>> dbClients = ClientManager.getInstance().getAllClients();
                return dbClients != null ? dbClients : java.util.Collections.emptyList();
            }

            @Override
            protected void done() {
                if (isCancelled() || loadGeneration != clientLoadGeneration) {
                    return;
                }
                try {
                    populateClients(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("Client loading was interrupted");
                    updateClientCountLabel((TableRowSorter<?>) clientTable.getRowSorter());
                } catch (java.util.concurrent.ExecutionException ex) {
                    LOGGER.error("Failed to load clients for ClientGUI", ex);
                    updateClientCountLabel((TableRowSorter<?>) clientTable.getRowSorter());
                }
            }
        };
        clientLoadWorker.execute();
    }

    private void populateClients(List<Map<String, String>> dbClients) {
        clients.clear();
        DefaultTableModel model = (DefaultTableModel) clientTable.getModel();
        model.setRowCount(0);

        for (Map<String, String> dbClient : dbClients) {
            String name = dbClient.get("firstLastName");
            String dept = dbClient.get("department");

            clients.add(new Client(name, dept));
            model.addRow(new Object[] { name, dept });
        }

        updateClientCountLabel((TableRowSorter<?>) clientTable.getRowSorter());
        LOGGER.info("Loaded {} clients into ClientGUI", dbClients.size());
    }

    private void updateClientCountLabel(TableRowSorter<?> sorter) {
        int total = clientTable.getModel().getRowCount();
        int shown = sorter != null ? sorter.getViewRowCount() : total;
        clientCountLabel.setText("Kunden: " + shown + " / " + total);
    }

    private void styleCountBadge(JLabel label) {
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        label.setOpaque(true);
        label.setBackground(ThemeManager.getSurfaceColor());
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
    }

    private Object[] showAddClientDialog() {
        return ClientDialog.showAddClientDialog(this);
    }

    private Object[] showUpdateClientDialog(Object[] existing) {
        if (existing == null)
            return null;
        return ClientDialog.showUpdateClientDialog(this, existing);
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.EDIT + " Bearbeiten");
        JMenuItem del = new JMenuItem(UnicodeSymbols.CLEAR + " Löschen");
        popup.add(edit);
        popup.add(del);

        edit.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1)
                return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[] {
                    clientTable.getModel().getValueAt(modelRow, 0),
                    clientTable.getModel().getValueAt(modelRow, 1)
            };
            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null)
                loadClients();
        });

        del.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1)
                return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);

            int confirm = new MessageDialog()
                    .setTitle("Löschen bestätigen")
                    .setMessage("Möchten Sie diesen Kunden wirklich löschen?")
                    .setOptionType(JOptionPane.YES_NO_OPTION)
                    .displayWithOptions();
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    LOGGER.info("Deleted client '{}' from context menu", name);
                    Main.logUtils.addLog(Level.INFO, "Kunde gelöscht: " + name);
                    clients.remove(modelRow);
                    ((DefaultTableModel) clientTable.getModel()).removeRow(modelRow);
                } else {
                    LOGGER.warn("Failed to delete client '{}' from context menu", name);
                    new MessageDialog()
                            .setTitle("Fehler")
                            .setMessage("Fehler beim Löschen des Kunden aus der Datenbank.")
                            .display();
                }
            }
        });

        clientTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = clientTable.rowAtPoint(e.getPoint());
                    if (row == -1)
                        return;
                    int modelRow = clientTable.convertRowIndexToModel(row);
                    Object[] existing = new Object[] {
                            clientTable.getModel().getValueAt(modelRow, 0),
                            clientTable.getModel().getValueAt(modelRow, 1)
                    };
                    Object[] updated = showUpdateClientDialog(existing);
                    if (updated != null) {
                        loadClients();
                        new MessageDialog()
                                .setTitle("Aktualisiert")
                                .setMessage("Kundenliste wurde aktualisiert.")
                                .display();
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
        DefaultTableModel model = new DefaultTableModel(new String[] {
                UnicodeSymbols.PERSON + " Name",
                UnicodeSymbols.DEPARTMENT + " Abteilung"
        }, 0) {
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

        clientTable.setRowHeight(26);
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientTable.setShowGrid(true);
        clientTable.setGridColor(ThemeManager.getTableGridColor());
        clientTable.setIntercellSpacing(new Dimension(1, 1));
        clientTable.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        clientTable.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        clientTable.setFont(SettingsGUI.getFontByName(Font.PLAIN, SettingsGUI.TABLE_FONT_SIZE));

        // Alternating row colors
        JTableHeader header = getJTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackgroundColor());
        header.setForeground(ThemeManager.getTableHeaderForegroundColor());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));

        TableColumnModel tcm = clientTable.getColumnModel();
        for (int i = 0; i < baseColumnWidths.length && i < tcm.getColumnCount(); i++) {
            tcm.getColumn(i).setPreferredWidth(baseColumnWidths[i]);
        }
    }

    private JTableHeader getJTableHeader() {
        DefaultTableCellRenderer alternatingRenderer = getDefaultTableCellRenderer();
        clientTable.setDefaultRenderer(Object.class, alternatingRenderer);

        return clientTable.getTableHeader();
    }

    private static DefaultTableCellRenderer getDefaultTableCellRenderer() {
        return new DefaultTableCellRenderer() {
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
    }

    private void adjustColumnWidths() {
        JFrameUtils.adjustColumnWidths(clientTable, tableScrollPane, baseColumnWidths);
    }

    private JButton createRoundedButton(String text) {
        return JFrameUtils.createRoundedButton(text);
    }

    private void styleTextField(JTextField tf) {
        if (tf == null)
            return;
        JFrameUtils.styleTextField(tf);
    }

    private void styleComboBox(JComboBox<String> combo) {
        if (combo == null)
            return;
        JFrameUtils.styleComboBox(combo);
    }

    // Simple Client data class
    private static class Client {
        @SuppressWarnings("unused")
        final String name;
        @SuppressWarnings("unused")
        final String department;

        Client(String name, String department) {
            this.name = name;
            this.department = department;
        }
    }

    public static void openForNewClient(JFrame frame, String receiver) {
        LOGGER.info("Opening add-client dialog for suggested receiver '{}'", receiver);
        ClientDialog.showAddClientDialog(frame, receiver);
    }
}
