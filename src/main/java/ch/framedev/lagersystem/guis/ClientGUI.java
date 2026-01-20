package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
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
        ThemeManager.getInstance().registerWindow(this);

        setTitle("Kunden Verwaltung");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // ===== Header =====
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());

        RoundedPanel headerPanel = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerPanel.setPreferredSize(new Dimension(680, 64));
        headerPanel.setLayout(new GridBagLayout());

        JLabel titleLabel = new JLabel(UnicodeSymbols.CLIENT + "Kunden Verwaltung");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        headerPanel.add(titleLabel);
        headerWrapper.add(headerPanel);
        add(headerWrapper, BorderLayout.NORTH);

        // ===== Toolbar =====
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 12));
        toolbar.setBackground(ThemeManager.getBackgroundColor());

        JLabel filterLabel = new JLabel(UnicodeSymbols.SEARCH + " Nach Abteilung filtern:");
        filterLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        filterLabel.setForeground(ThemeManager.getTextPrimaryColor());

        filterDepartmentCombobox = new JComboBox<>();
        filterDepartmentCombobox.setPreferredSize(new Dimension(240, 40));
        filterDepartmentCombobox.addItem(UnicodeSymbols.DEPARTMENT + " Alle Abteilungen");
        fillFilterDepartmentList();
        styleComboBox(filterDepartmentCombobox);

        JButton addClientButton = createRoundedButton(UnicodeSymbols.HEAVY_PLUS + " Kunde hinzufügen");
        JButton editClientButton = createRoundedButton(UnicodeSymbols.BETTER_EDIT + " Kunde bearbeiten");
        JButton deleteClientButton = createRoundedButton(UnicodeSymbols.TRASH + " Kunde löschen");
        JButton refreshButton = createRoundedButton(UnicodeSymbols.UPDATE + " Aktualisieren");

        toolbar.add(filterLabel);
        toolbar.add(filterDepartmentCombobox);
        toolbar.add(addClientButton);
        toolbar.add(editClientButton);
        toolbar.add(deleteClientButton);
        toolbar.add(refreshButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.add(toolbar, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.PAGE_START);

        // ===== Main card with table =====
        RoundedPanel card = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        clientTable = new JTable();
        initializeClientTable();

        tableScrollPane = new JScrollPane(clientTable);
        tableScrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
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

        // ===== Bottom search bar =====
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        searchPanel.setBackground(ThemeManager.getBackgroundColor());

        JLabel searchLabel = new JLabel(UnicodeSymbols.SEARCH + " Suche (Name oder Abteilung):");
        searchLabel.setForeground(ThemeManager.getTextPrimaryColor());

        JTextField searchField = new JTextField(28);
        styleSearchField(searchField);

        JButton searchBtn = new JButton(UnicodeSymbols.SEARCH + " Suchen");
        JButton clearBtn = new JButton(UnicodeSymbols.CLEAR + " Leeren");
        styleFlatActionButton(searchBtn);
        styleFlatActionButton(clearBtn);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(clearBtn);
        add(searchPanel, BorderLayout.SOUTH);

        // ===== Load clients =====
        loadClients();

        // ===== Sorting/Filtering =====
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) clientTable.getModel());
        clientTable.setRowSorter(sorter);

        filterDepartmentCombobox.addActionListener(listener -> {
            String selected = (String) filterDepartmentCombobox.getSelectedItem();
            if (selected == null || selected.equals("Alle Abteilungen")) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("^" + Pattern.quote(selected) + "$", 1));
            }
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
        };

        searchBtn.addActionListener(e -> doSearch.run());
        clearBtn.addActionListener(e -> {
            searchField.setText("");
            sorter.setRowFilter(null);
        });
        searchField.addActionListener(e -> doSearch.run());

        // ===== Table interactions =====
        setupTableInteractions();

        // ===== Wire actions =====
        addClientButton.addActionListener(e -> {
            Object[] row = showAddClientDialog();
            if (row != null) loadClients();
        });

        editClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Bearbeiten aus.", "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE, Main.iconSmall);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[]{
                    clientTable.getModel().getValueAt(modelRow, 0),
                    clientTable.getModel().getValueAt(modelRow, 1)
            };

            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null) loadClients();
        });

        deleteClientButton.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(this, "Bitte wählen Sie einen Kunden zum Löschen aus.", "Keine Auswahl",
                        JOptionPane.WARNING_MESSAGE, Main.iconSmall);
                return;
            }
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.iconSmall);

            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    loadClients();
                    JOptionPane.showMessageDialog(this, "Kunde erfolgreich gelöscht.", "Erfolg",
                            JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        refreshButton.addActionListener(e -> {
            loadClients();
            JOptionPane.showMessageDialog(this, "Kundenliste wurde aktualisiert.", "Aktualisiert",
                    JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
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
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void fillFilterDepartmentList() {
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        for (var department : departmentManager.getAllDepartments()) {
            String dept = (String) department.get("department");
            if (dept != null && !dept.trim().isEmpty()) {
                filterDepartmentCombobox.addItem(dept.trim());
            }
        }
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

        JDialog dialog = new JDialog(this, UnicodeSymbols.CLIENT + " Neuen Kunden hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with subtle shadow
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        Color shadowColor = ThemeManager.isDarkMode()
                ? new Color(0, 0, 0, 80)
                : new Color(0, 0, 0, 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadowColor, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Modern gradient header
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.isDarkMode()
                        ? new Color(30, 58, 95)
                        : new Color(41, 128, 185);
                Color color2 = ThemeManager.isDarkMode()
                        ? new Color(44, 62, 80)
                        : new Color(52, 152, 219);

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Icon and title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.HEAVY_PLUS);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Neuen Kunden hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Modern close button with hover effect
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setForeground(new Color(255, 255, 255, 200));
        closeBtn.setBackground(new Color(255, 255, 255, 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(new Color(231, 76, 60, 100));
                closeBtn.setContentAreaFilled(true);
                closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 255, 255, 200));
                closeBtn.setBackground(new Color(255, 255, 255, 0));
                closeBtn.setContentAreaFilled(false);
                closeBtn.setBorder(null);
            }
        });

        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });

        headerPanel.add(closeBtn, BorderLayout.EAST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card with better spacing
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Label creation with blue accent for required fields
        java.util.function.Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));

            if (text.contains("*")) {
                Color blueAccent = ThemeManager.isDarkMode()
                        ? new Color(100, 170, 255)
                        : new Color(52, 152, 219);
                String labelText = text.replace("*", "").trim();
                label.setText("<html>" + labelText + " <span style='color: rgb(" +
                        blueAccent.getRed() + "," + blueAccent.getGreen() + "," + blueAccent.getBlue() +
                        ");'>*</span></html>");
            }

            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        // Text field styling with blue focus effect
        java.util.function.Consumer<JTextField> styleTextField = field -> {
            Color normalBorder = ThemeManager.getInputBorderColor();
            Color focusBorder = ThemeManager.isDarkMode()
                    ? new Color(100, 170, 255)
                    : new Color(52, 152, 219);

            field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
            field.setBackground(ThemeManager.getInputBackgroundColor());
            field.setForeground(ThemeManager.getTextPrimaryColor());
            field.setCaretColor(ThemeManager.getAccentColor());
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(normalBorder, 1, true),
                    BorderFactory.createEmptyBorder(12, 14, 12, 14)
            ));

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(focusBorder, 2, true),
                            BorderFactory.createEmptyBorder(11, 13, 11, 13)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    field.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(normalBorder, 1, true),
                            BorderFactory.createEmptyBorder(12, 14, 12, 14)
                    ));
                }
            });
        };

        int row = 0;

        // Name field
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel nameLabel = createLabel.apply(UnicodeSymbols.PERSON + " Vorname und Nachname *");
        contentCard.add(nameLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 20, 8);
        JTextField nameField = new JTextField(35);
        styleTextField.accept(nameField);
        contentCard.add(nameField, gbc);

        // Department field
        row++;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row;
        JLabel deptLabel = createLabel.apply(UnicodeSymbols.DEPARTMENT + " Abteilung");
        contentCard.add(deptLabel, gbc);

        row++;
        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 8, 8);
        departmentCombobox = new JComboBox<>();
        fillDepartmentList();
        departmentCombobox.setPreferredSize(new Dimension(400, 40));
        styleComboBox(departmentCombobox);
        contentCard.add(departmentCombobox, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Modern button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        // Cancel button with hover
        Color cancelColor = ThemeManager.getErrorColor();
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + " Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(cancelColor);
        cancelBtn.setOpaque(true);
        cancelBtn.setContentAreaFilled(true);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cancelColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(ThemeManager.getButtonHoverColor(cancelColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelColor);
            }
        });

        // OK button with blue gradient colors and hover
        Color okColor = ThemeManager.isDarkMode()
                ? new Color(52, 152, 219)
                : new Color(41, 128, 185);
        Color okHover = ThemeManager.isDarkMode()
                ? new Color(72, 170, 240)
                : new Color(52, 152, 219);

        JButton okBtn = new JButton(UnicodeSymbols.CHECKMARK + " Hinzufügen");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okColor);
        okBtn.setOpaque(true);
        okBtn.setContentAreaFilled(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(okHover);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(okHover.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okColor);
                okBtn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(okColor.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 28, 12, 28)
                ));
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Event handlers
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) departmentCombobox.getSelectedItem();
            if (selectedDept == null) selectedDept = "";
            String dept = selectedDept.trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            if (!clientManager.insertClient(name, dept)) {
                JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                holder[0] = null;
                return;
            }

            holder[0] = new Object[]{name, dept};
            dialog.dispose();
        });

        dialog.setSize(700, 450);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    private Object[] showUpdateClientDialog(Object[] existing) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(this, UnicodeSymbols.EDIT + " Kunde bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with subtle shadow
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        Color shadowColor = ThemeManager.isDarkMode()
                ? new Color(0, 0, 0, 80)
                : new Color(0, 0, 0, 30);
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(shadowColor, 1, true),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header with gradient-like effect
        JPanel headerPanel = createModernHeaderPanel(dialog, holder);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content card with form fields
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 14);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Name field with enhanced styling
        int row = 0;
        gbc.gridy = row++;
        JLabel nameLabel = createStyledLabel(UnicodeSymbols.PERSON + "  Vorname und Nachname *");
        contentCard.add(nameLabel, gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(4, 8, 20, 8);
        JTextField nameField = createStyledTextField(existing[0] == null ? "" : existing[0].toString(), 30);
        contentCard.add(nameField, gbc);

        // Department field with enhanced styling
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        JLabel deptLabel = createStyledLabel(UnicodeSymbols.DEPARTMENT + "  Abteilung");
        contentCard.add(deptLabel, gbc);

        gbc.gridy = row;
        gbc.insets = new Insets(4, 8, 8, 8);
        departmentCombobox = new JComboBox<>();
        fillDepartmentList();
        departmentCombobox.setPreferredSize(new Dimension(340, 38));
        styleComboBox(departmentCombobox);

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

        contentCard.add(departmentCombobox, gbc);
        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel with enhanced styling
        JPanel buttonPanel = createUpdateDialogButtonPanel(dialog, holder, nameField);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton((JButton) buttonPanel.getComponent(1)); // OK button

        dialog.setSize(680, 480);
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);

        return holder[0];
    }

    /**
     * Create modern header panel for update client dialog with gradient effect
     */
    private JPanel createModernHeaderPanel(JDialog dialog, Object[][] holder) {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.getAccentColor();
                Color color2 = ThemeManager.isDarkMode()
                        ? ThemeManager.getButtonHoverColor(color1)
                        : color1.brighter();

                GradientPaint gradient = new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // Title section with icon
        JPanel titleSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleSection.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.EDIT);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 22));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Kunde bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titleSection.add(iconLabel);
        titleSection.add(titleLabel);
        headerPanel.add(titleSection, BorderLayout.WEST);

        // Close button with hover effect
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setForeground(new Color(255, 255, 255, 200));
        closeBtn.setBackground(new Color(255, 255, 255, 0));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 20));
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(40, 40));

        closeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                closeBtn.setForeground(Color.WHITE);
                closeBtn.setBackground(new Color(231, 76, 60, 100));
                closeBtn.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeBtn.setForeground(new Color(255, 255, 255, 200));
                closeBtn.setBackground(new Color(255, 255, 255, 0));
                closeBtn.setContentAreaFilled(false);
            }
        });

        closeBtn.addActionListener(e -> {
            holder[0] = null;
            dialog.dispose();
        });

        headerPanel.add(closeBtn, BorderLayout.EAST);
        return headerPanel;
    }

    /**
     * Create styled label for dialog forms
     */
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    /**
     * Create styled text field for dialog forms with focus effects
     */
    private JTextField createStyledTextField(String initialValue, int columns) {
        JTextField field = new JTextField(initialValue, columns);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());

        Color normalBorder = ThemeManager.getInputBorderColor();
        Color focusBorder = ThemeManager.getInputFocusBorderColor();

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(11, 13, 11, 13)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(12, 14, 12, 14)
                ));
            }
        });

        return field;
    }

    /**
     * Create button panel for update client dialog with enhanced buttons
     */
    private JPanel createUpdateDialogButtonPanel(JDialog dialog, Object[][] holder, JTextField nameField) {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 16));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        // Cancel button with hover effect
        Color cancelColor = ThemeManager.getErrorColor();
        JButton cancelBtn = new JButton(UnicodeSymbols.CLOSE + "  Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setBackground(cancelColor);
        cancelBtn.setOpaque(true);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cancelColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(ThemeManager.getButtonHoverColor(cancelColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelColor);
            }
        });

        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        // OK button with hover effect
        Color okColor = ThemeManager.getSuccessColor();
        JButton okBtn = new JButton(UnicodeSymbols.FLOPPY + "  Speichern");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        okBtn.setBackground(okColor);
        okBtn.setOpaque(true);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(ThemeManager.getButtonHoverColor(okColor));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okColor);
            }
        });

        okBtn.addActionListener(ae -> {
            String name = nameField.getText().trim();
            String selectedDept = (String) departmentCombobox.getSelectedItem();
            if (selectedDept == null) selectedDept = "";
            String dept = selectedDept.trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            if (!clientManager.existsClient(name)) {
                if (!clientManager.insertClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Hinzufügen des Kunden zur Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    return;
                }
            } else {
                if (!clientManager.updateClient(name, dept)) {
                    JOptionPane.showMessageDialog(dialog, "Fehler beim Aktualisieren des Kunden in der Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    return;
                }
            }

            holder[0] = new Object[]{name, dept};
            dialog.dispose();
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);

        return buttonPanel;
    }

    private void fillDepartmentList() {
        departmentCombobox.removeAllItems();
        departmentCombobox.addItem("");

        DepartmentManager.getInstance()
                .getAllDepartments()
                .stream()
                .map(dept -> dept.get("department"))
                .filter(dept -> dept != null && !dept.toString().trim().isEmpty())
                .map(dept -> dept.toString().trim())
                .distinct()
                .sorted()
                .forEach(departmentCombobox::addItem);

        departmentCombobox.setEditable(true);

        // Ensure editor is themed too
        Component editor = departmentCombobox.getEditor().getEditorComponent();
        if (editor instanceof JTextField tf) {
            tf.setBackground(ThemeManager.getInputBackgroundColor());
            tf.setForeground(ThemeManager.getTextPrimaryColor());
            tf.setCaretColor(ThemeManager.getTextPrimaryColor());
            tf.setBorder(null);
        }
    }

    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.EDIT + " Bearbeiten");
        JMenuItem del = new JMenuItem(UnicodeSymbols.CLEAR + " Löschen");
        popup.add(edit);
        popup.add(del);

        edit.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            Object[] existing = new Object[]{
                    clientTable.getModel().getValueAt(modelRow, 0),
                    clientTable.getModel().getValueAt(modelRow, 1)
            };
            Object[] updated = showUpdateClientDialog(existing);
            if (updated != null) loadClients();
        });

        del.addActionListener(e -> {
            int sel = clientTable.getSelectedRow();
            if (sel == -1) return;
            int modelRow = clientTable.convertRowIndexToModel(sel);
            String name = (String) clientTable.getModel().getValueAt(modelRow, 0);

            int confirm = JOptionPane.showConfirmDialog(this, "Möchten Sie diesen Kunden wirklich löschen?", "Löschen",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, Main.iconSmall);
            if (confirm == JOptionPane.YES_OPTION) {
                if (ClientManager.getInstance().deleteClient(name)) {
                    clients.remove(modelRow);
                    ((DefaultTableModel) clientTable.getModel()).removeRow(modelRow);
                } else {
                    JOptionPane.showMessageDialog(this, "Fehler beim Löschen des Kunden aus der Datenbank.", "Fehler",
                            JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
                    Object[] existing = new Object[]{
                            clientTable.getModel().getValueAt(modelRow, 0),
                            clientTable.getModel().getValueAt(modelRow, 1)
                    };
                    Object[] updated = showUpdateClientDialog(existing);
                    if (updated != null) loadClients();
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
        DefaultTableModel model = new DefaultTableModel(new String[]{"Name", "Abteilung"}, 0) {
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
        DefaultTableCellRenderer alternatingRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                    c.setForeground(ThemeManager.getTextPrimaryColor());
                }
                return c;
            }
        };
        clientTable.setDefaultRenderer(Object.class, alternatingRenderer);

        JTableHeader header = clientTable.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackgroundColor());
        header.setForeground(ThemeManager.getTableHeaderForegroundColor());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 18));

        TableColumnModel tcm = clientTable.getColumnModel();
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

    private void styleSearchField(JTextField field) {
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getInputFocusBorderColor(), 2),
                        BorderFactory.createEmptyBorder(7, 9, 7, 9)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                ));
            }
        });
    }

    private void styleFlatActionButton(JButton b) {
        Color normalFg = ThemeManager.getTextPrimaryColor();
        Color hoverFg = ThemeManager.getTextLinkColor();
        Color border = ThemeManager.getBorderColor();

        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setForeground(normalFg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setForeground(hoverFg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderFocusColor(), 1),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setForeground(normalFg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(border, 1),
                        BorderFactory.createEmptyBorder(8, 16, 8, 16)
                ));
            }
        });
    }

    private void styleComboBox(JComboBox<String> combo) {
        Color bg = ThemeManager.getInputBackgroundColor();
        Color fg = ThemeManager.getTextPrimaryColor();
        Color border = ThemeManager.getInputBorderColor();
        Color selBg = ThemeManager.getSelectionBackgroundColor();
        Color selFg = ThemeManager.getSelectionForegroundColor();
        Color surface = ThemeManager.getSurfaceColor();

        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setOpaque(true);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                // enforce popup list colors
                list.setBackground(bg);
                list.setForeground(fg);
                list.setSelectionBackground(selBg);
                list.setSelectionForeground(selFg);

                c.setOpaque(true);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    c.setBackground(selBg);
                    c.setForeground(selFg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(fg);
                }
                return c;
            }
        });

        // Theme arrow button + popup border (reliable across LAFs)
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton(UnicodeSymbols.ARROW_DOWN);
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { b.setBackground(surface); }
                    @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
                });
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof BasicComboPopup basic) {
                    basic.setBorder(BorderFactory.createLineBorder(border, 1));
                    basic.getList().setBackground(bg);
                    basic.getList().setForeground(fg);
                    basic.getList().setSelectionBackground(selBg);
                    basic.getList().setSelectionForeground(selFg);
                }
                return popup;
            }
        });

        if (combo.isEditable()) {
            Component editorComp = combo.getEditor().getEditorComponent();
            if (editorComp instanceof JTextField tf) {
                tf.setBackground(bg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBorder(null);
            }
        }
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