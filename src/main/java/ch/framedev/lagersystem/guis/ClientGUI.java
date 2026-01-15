package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ClientGUI extends JFrame {

    private final JTable clientTable;
    private final JScrollPane tableScrollPane;
    private final int[] baseColumnWidths = new int[]{300, 300};
    private final List<Client> clients = new ArrayList<>();

    private JComboBox<String> departmentCombobox;

    private final JComboBox<String> filterDepartmentCombobox;

    public ClientGUI() {
        setTitle("Kunden Verwaltung");
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
        JLabel titleLabel = new JLabel("Kunden Verwaltung");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 22f));
        titleLabel.setForeground(new Color(31, 45, 61));
        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(new Color(245, 247, 250));
        filterDepartmentCombobox = new JComboBox<>();
        filterDepartmentCombobox.setPreferredSize(new Dimension(200, 30));
        filterDepartmentCombobox.addItem("Alle Abteilungen");
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        for (var department : departmentManager.getAllDepartments()) {
            String dept = (String) department.get("department");
            if (dept != null && !dept.trim().isEmpty()) {
                filterDepartmentCombobox.addItem(dept.trim());
            }
        }
        JLabel filterLabel = new JLabel("Nach Abteilung filtern:");
        filterLabel.setFont(filterLabel.getFont().deriveFont(Font.BOLD));
        toolbar.add(filterLabel);
        toolbar.add(filterDepartmentCombobox);
        JButton addClientButton = createRoundedButton("Kunde hinzufügen");
        JButton editClientButton = createRoundedButton("Kunde bearbeiten");
        JButton deleteClientButton = createRoundedButton("Kunde löschen");
        JButton refreshButton = createRoundedButton("🔄 Aktualisieren");
        toolbar.add(addClientButton);
        toolbar.add(editClientButton);
        toolbar.add(deleteClientButton);
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
        clientTable = new JTable();
        // initialize the Client Table
        initializeClientTable();
        tableScrollPane = new JScrollPane(clientTable);
        card.add(tableScrollPane, BorderLayout.CENTER);

        filterDepartmentCombobox.addActionListener(listener -> {
            String selected = (String) filterDepartmentCombobox.getSelectedItem();
            if (selected == null || selected.equals("Alle Abteilungen")) {
                ((TableRowSorter<?>) clientTable.getRowSorter()).setRowFilter(null);
            } else {
                ((TableRowSorter<?>) clientTable.getRowSorter()).setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(selected) + "$", 1));
            }
        });

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
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Bearbeiten aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[2];
            for (int i = 0; i < 2; i++) {
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
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Löschen aus.", "Keine Auswahl", JOptionPane.WARNING_MESSAGE,
                        Main.iconSmall);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    loadClients(); // Refresh table
                    JOptionPane.showMessageDialog(this, "Kunde erfolgreich gelöscht.", "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            loadClients();
            JOptionPane.showMessageDialog(this, "Kundenliste wurde aktualisiert.", "Aktualisiert", JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
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

        // setVisible(true);
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

            clients.add(new Client(name, dept));
            model.addRow(new Object[]{name, dept});
        }
    }

    private Object[] showAddClientDialog() {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(this, "Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with background
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 247, 250));
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 190, 200), 2),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient-like effect
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185)); // Blue
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("➕  Neuen Kunden hinzufügen");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Styled close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(192, 57, 43)); // Darker red background
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(true);
        closeBtn.setOpaque(true);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(45, 45));
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(169, 50, 38), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(231, 76, 60));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(new Color(192, 57, 43));
            }
        });
        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });

        JPanel closeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeBtnPanel.setOpaque(false);
        closeBtnPanel.add(closeBtn);
        headerPanel.add(closeBtnPanel, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        RoundedPanel contentCard = new RoundedPanel(Color.WHITE, 0);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Helper for labels
        java.util.function.Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(new Font("Arial", Font.BOLD, 13));
            label.setForeground(new Color(44, 62, 80));
            return label;
        };

        // Helper for text fields
        java.util.function.Consumer<JTextField> styleTextField = field -> {
            field.setFont(new Font("Arial", Font.PLAIN, 14));
            field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            field.setBackground(new Color(250, 251, 252));
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                    ));
                }
                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                    ));
                }
            });
        };

        int row = 0;

        // Name field with icon
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        JLabel nameLabel = createLabel.apply("👤  Vorname und Nachname *");
        contentCard.add(nameLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 16, 6);
        JTextField nameField = new JTextField(30);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        row++;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Department field with icon
        gbc.gridy = row;
        JLabel deptLabel = createLabel.apply("🏢  Abteilung");
        contentCard.add(deptLabel, gbc);
        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(2, 6, 6, 6);
        departmentCombobox = new JComboBox<>();
        fillDepartmentList();
        departmentCombobox.setFont(new Font("Arial", Font.PLAIN, 14));
        departmentCombobox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        departmentCombobox.setBackground(new Color(250, 251, 252));
        departmentCombobox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel with improved styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 16));
        buttonPanel.setBackground(new Color(250, 251, 252));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton cancelBtn = new JButton("✕  Abbrechen");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 13));
        cancelBtn.setForeground(new Color(52, 73, 94));
        cancelBtn.setBackground(new Color(236, 240, 241));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(new Color(220, 225, 230));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(new Color(236, 240, 241));
            }
        });

        JButton okBtn = new JButton("✓  Hinzufügen");
        okBtn.setFont(new Font("Arial", Font.BOLD, 13));
        okBtn.setForeground(Color.BLACK);
        okBtn.setBackground(new Color(52, 152, 219));
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorderPainted(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(41, 128, 185), 1),
            BorderFactory.createEmptyBorder(10, 27, 10, 27)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(new Color(41, 128, 185));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(new Color(52, 152, 219));
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Actions
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) departmentCombobox.getSelectedItem();
            if (selectedDept == null) {
                selectedDept = "";
            }
            String dept = selectedDept.trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Name ist erforderlich.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            holder[0] = new Object[]{name, dept};

            ClientManager clientManager = ClientManager.getInstance();
            if (!clientManager.insertClient(name, dept)) {
                JOptionPane.showMessageDialog(dialog,
                    "Fehler beim Hinzufügen des Kunden zur Datenbank.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
                holder[0] = null;
            }

            dialog.dispose();
        });

        // Show dialog
        dialog.setSize(650, 400);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    private Object[] showUpdateClientDialog(Object[] existing) {
        final Object[][] holder = new Object[1][];
        JDialog dialog = new JDialog(this, "Kunde bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 247, 250));
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 200), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient-like effect
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titleLabel = new JLabel("✏️  Kunde bearbeiten");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Styled close button
        JButton closeBtn = new JButton("✕");
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(192, 57, 43)); // Darker red background
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(true);
        closeBtn.setOpaque(true);
        closeBtn.setFont(new Font("Arial", Font.BOLD, 18));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(45, 45));
        closeBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(169, 50, 38), 1),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setBackground(new Color(231, 76, 60));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setBackground(new Color(192, 57, 43));
            }
        });
        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });

        JPanel closeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeBtnPanel.setOpaque(false);
        closeBtnPanel.add(closeBtn);
        headerPanel.add(closeBtnPanel, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card
        RoundedPanel contentCard = new RoundedPanel(Color.WHITE, 0);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Name field with icon
        JLabel nameLabel = new JLabel("👤  Vorname und Nachname *");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        nameLabel.setForeground(new Color(44, 62, 80));
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentCard.add(nameLabel, gbc);

        JTextField nameField = new JTextField(existing[0] == null ? "" : existing[0].toString(), 25);
        nameField.setFont(new Font("Arial", Font.PLAIN, 14));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        nameField.setBackground(new Color(250, 251, 252));
        nameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                nameField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                nameField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });
        gbc.gridy = 1;
        gbc.insets = new Insets(2, 8, 20, 8);
        contentCard.add(nameField, gbc);

        // Department field with icon
        JLabel deptLabel = new JLabel("🏢  Abteilung");
        deptLabel.setFont(new Font("Arial", Font.BOLD, 13));
        deptLabel.setForeground(new Color(44, 62, 80));
        gbc.gridy = 2;
        gbc.insets = new Insets(8, 8, 8, 8);
        contentCard.add(deptLabel, gbc);

        departmentCombobox = new JComboBox<>();
        departmentCombobox.setFont(new Font("Arial", Font.PLAIN, 14));
        departmentCombobox.setBackground(new Color(250, 251, 252));
        departmentCombobox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        fillDepartmentList();

        // Pre-select existing department
        if (existing[1] != null) {
            String existingDept = existing[1].toString();
            for (int i = 0; i < departmentCombobox.getItemCount(); i++) {
                if (existingDept.equals(departmentCombobox.getItemAt(i))) {
                    departmentCombobox.setSelectedIndex(i);
                    break;
                }
            }
        }

        gbc.gridy = 3;
        gbc.insets = new Insets(2, 8, 8, 8);
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel with improved styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(new Color(250, 251, 252));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setFont(new Font("Arial", Font.BOLD, 13));
        cancelBtn.setForeground(new Color(52, 73, 94));
        cancelBtn.setBackground(new Color(236, 240, 241));
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(new Color(220, 225, 230));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(new Color(236, 240, 241));
            }
        });

        JButton okBtn = new JButton("💾  Speichern");
        okBtn.setFont(new Font("Arial", Font.BOLD, 13));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(new Color(46, 204, 113));
        okBtn.setOpaque(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(39, 174, 96), 1),
                BorderFactory.createEmptyBorder(10, 24, 10, 24)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(new Color(39, 174, 96));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(new Color(46, 204, 113));
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Actions
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) departmentCombobox.getSelectedItem();
            if(selectedDept == null) {
                selectedDept = "";
            }
            String dept = selectedDept.trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }
            holder[0] = new Object[]{name, dept};
            dialog.dispose();
            ClientManager clientManager = ClientManager.getInstance();
            if(!clientManager.existsClient(name)) {
                if(!clientManager.insertClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            } else {
                if(!clientManager.updateClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Aktualisieren des Kunden in der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        // Show dialog
        dialog.setSize(650, 450);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);
        return holder[0];
    }

    /**
     * Optimized method to fill department combobox.
     * Uses stream processing for better performance and cleaner code.
     */
    private void fillDepartmentList() {
        departmentCombobox.removeAllItems();
        departmentCombobox.addItem(""); // Empty option

        // Stream-based processing: filter, map, sort, and add in one pipeline
        DepartmentManager.getInstance()
            .getAllDepartments()
            .stream()
            .map(dept -> dept.get("department"))
            .filter(dept -> dept != null && !dept.toString().trim().isEmpty())
            .map(dept -> dept.toString().trim())
            .distinct()  // Remove duplicates
            .sorted()    // Sort alphabetically
            .forEach(departmentCombobox::addItem);

        // Make it editable so users can enter custom departments
        departmentCombobox.setEditable(true);
    }

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
            Object[] existing = new Object[2];
            for (int i = 0; i < 2; i++) {
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

            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE, Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    clients.remove(modelRow);
                    ((DefaultTableModel) clientTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
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
                    Object[] existing = new Object[2];
                    for (int i = 0; i < 2; i++) {
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
                return 2;
            }

            @Override
            public String getColumnName(int col) {
                return switch (col) {
                    case 0 -> "Name";
                    case 1 -> "Abteilung";
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
        clientTable.setFont(new Font("Arial", Font.PLAIN, 16));

        TableColumnModel tcm = clientTable.getColumnModel();

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
        clientTable.setDefaultRenderer(Object.class, alternatingRenderer);

        // Header styling
        JTableHeader header = clientTable.getTableHeader();
        header.setBackground(new Color(62, 84, 98));
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 18f));
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
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        Color defaultBg = ThemeManager.getAccentColor();
        Color hoverBg = ThemeManager.getButtonHoverColor(defaultBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(defaultBg);

        button.setBackground(defaultBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
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
                if (button.contains(e.getPoint())) {
                    button.setBackground(hoverBg);
                } else {
                    button.setBackground(defaultBg);
                }
            }
        });

        return button;
    }

    // Simple Client data class
    private static class Client {
        String name;
        String department;

        Client(String name, String department) {
            this.name = name;
            this.department = department;
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
