package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.VendorManager;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
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
        JButton editVendorButton = createRoundedButton(UnicodeSymbols.BETTER_EDIT + " Lieferant bearbeiten");
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
                Vendor v = new Vendor((String) row[0], (String) row[1], (String) row[2], (String) row[3], (String) row[4]);
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
                Vendor v = new Vendor((String) updated[0], (String) updated[1], (String) updated[2], (String) updated[3], (String) updated[4]);
                v.setSuppliedArticles(java.util.Arrays.asList(((String) updated[5]).split("\\s*,\\s*")));
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
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(hover); }
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(fg); }
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
                    String.join(",", v.getSuppliedArticles())
            });
        }
    }

    private Object[] showAddVendorDialog() {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(this, "Neuen Lieferanten hinzufügen", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient
        JPanel headerPanel = getHeaderPanel();

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.TRUCK);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Neuen Lieferanten hinzufügen");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Close button
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Fenster schliessen");
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

        // Content card
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Helper for creating labels
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

        int row = 0;

        // Name field
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + "  Name *"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField idField = createStyledTextField();
        contentCard.add(idField, gbc);

        // Contact person
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PERSON + "  Kontaktperson"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField contactField = createStyledTextField();
        contentCard.add(contactField, gbc);

        // Phone
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PHONE + "  Telefon"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField phoneField = createStyledTextField();
        contentCard.add(phoneField, gbc);

        // Email
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.MEMO + "  Email"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField emailField = createStyledTextField();
        contentCard.add(emailField, gbc);

        // Address
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.BUILDING + "  Adresse"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField addressField = createStyledTextField();
        contentCard.add(addressField, gbc);

        // Articles
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PACKAGE + "  Gelieferte Artikel (kommagetrennt)"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 8, 8, 8);
        JTextField articlesField = createStyledTextField();
        contentCard.add(articlesField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setToolTipText("Abbrechen des Vorgangs");
        cancelBtn.setForeground(ThemeManager.getTextPrimaryColor());
        Color cancelBaseColor = ThemeManager.getSurfaceColor();
        Color cancelHoverColor = ThemeManager.isDarkMode()
                ? new Color(70, 70, 70)
                : new Color(220, 225, 230);
        cancelBtn.setBackground(cancelBaseColor);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(cancelHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelBaseColor);
            }
        });

        JButton okBtn = new JButton(UnicodeSymbols.HEAVY_PLUS + " Hinzufügen");
        okBtn.setToolTipText("Neuen Lieferanten hinzufügen");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        Color okBaseColor = ThemeManager.isDarkMode()
                ? new Color(52, 152, 219)
                : new Color(41, 128, 185);
        Color okHoverColor = ThemeManager.isDarkMode()
                ? new Color(72, 170, 240)
                : new Color(52, 152, 219);
        Color okPressedColor = ThemeManager.isDarkMode()
                ? new Color(32, 120, 200)
                : new Color(31, 97, 141);
        okBtn.setBackground(okBaseColor);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okBaseColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setOpaque(true);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(okHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okBaseColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                okBtn.setBackground(okPressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                okBtn.setBackground(okBtn.contains(e.getPoint()) ? okHoverColor : okBaseColor);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Action listeners
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            holder[0] = new Object[]{id, contact, phone, email, addr, arts};
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(idField::requestFocusInWindow);
        dialog.setVisible(true);
        return holder[0];
    }

    private static JPanel getHeaderPanel() {
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
        return headerPanel;
    }

    /**
     * Creates a styled text field for vendor dialogs with blue focus effect
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField(30);
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getAccentColor());

        Color normalBorder = ThemeManager.getBorderColor();
        Color focusBorder = ThemeManager.isDarkMode()
                ? new Color(100, 170, 255)
                : new Color(52, 152, 219);

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder, 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(focusBorder, 2, true),
                        BorderFactory.createEmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(normalBorder, 1, true),
                        BorderFactory.createEmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    /**
     * Shows a dialog to update an existing vendor.
     * @param existing The existing vendor data as an Object array.
     * @return The updated vendor data as an Object array, or null if canceled.
     */
    private Object[] showUpdateVendorDialog(Object[] existing) {
        final Object[][] holder = new Object[1][];

        JDialog dialog = new JDialog(this, "Lieferant bearbeiten", true);
        dialog.setUndecorated(true);

        // Main container with shadow effect
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());
        mainContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header panel with gradient
        JPanel headerPanel = getHeaderPanel();

        // Title with icon
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel(UnicodeSymbols.EDIT);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 24));
        iconLabel.setForeground(Color.WHITE);

        JLabel titleLabel = new JLabel("Lieferant bearbeiten");
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);

        titlePanel.add(iconLabel);
        titlePanel.add(titleLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // Close button
        JButton closeBtn = new JButton(UnicodeSymbols.CLOSE);
        closeBtn.setToolTipText("Fenster schliessen");
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

        // Content card
        RoundedPanel contentCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        contentCard.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        contentCard.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        // Helper for creating labels
        java.util.function.Function<String, JLabel> createLabel = text -> {
            JLabel label = new JLabel(text);
            label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
            label.setForeground(ThemeManager.getTextPrimaryColor());
            return label;
        };

        int row = 0;

        // Name field
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.TRUCK + "  Name"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField idField = createStyledTextField();
        idField.setText(existing[0] == null ? "" : existing[0].toString());
        contentCard.add(idField, gbc);

        // Contact person
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PERSON + "  Kontaktperson"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField contactField = createStyledTextField();
        contactField.setText(existing[1] == null ? "" : existing[1].toString());
        contentCard.add(contactField, gbc);

        // Phone
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PHONE + "  Telefon"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField phoneField = createStyledTextField();
        phoneField.setText(existing[2] == null ? "" : existing[2].toString());
        contentCard.add(phoneField, gbc);

        // Email
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.MEMO + "  Email"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField emailField = createStyledTextField();
        emailField.setText(existing[3] == null ? "" : existing[3].toString());
        contentCard.add(emailField, gbc);

        // Address
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.BUILDING + "  Adresse"), gbc);
        gbc.gridy = row++;
        gbc.insets = new Insets(2, 8, 16, 8);
        JTextField addressField = createStyledTextField();
        addressField.setText(existing[4] == null ? "" : existing[4].toString());
        contentCard.add(addressField, gbc);

        // Articles
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridy = row++;
        contentCard.add(createLabel.apply(UnicodeSymbols.PACKAGE + "  Gelieferte Artikel (kommagetrennt)"), gbc);
        gbc.gridy = row;
        gbc.insets = new Insets(2, 8, 8, 8);
        JTextField articlesField = createStyledTextField();
        articlesField.setText(existing[5] == null ? "" : existing[5].toString());
        contentCard.add(articlesField, gbc);

        mainContainer.add(contentCard, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBackground(ThemeManager.getBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton cancelBtn = new JButton("Abbrechen");
        cancelBtn.setToolTipText("Abbrechen des Vorgangs");
        cancelBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        cancelBtn.setForeground(ThemeManager.getTextPrimaryColor());
        Color cancelBaseColor = ThemeManager.getSurfaceColor();
        Color cancelHoverColor = ThemeManager.isDarkMode()
                ? new Color(70, 70, 70)
                : new Color(220, 225, 230);
        cancelBtn.setBackground(cancelBaseColor);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setOpaque(true);
        cancelBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                cancelBtn.setBackground(cancelHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                cancelBtn.setBackground(cancelBaseColor);
            }
        });

        JButton okBtn = new JButton(UnicodeSymbols.FLOPPY + " Speichern");
        okBtn.setToolTipText("Änderungen speichern");
        okBtn.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        okBtn.setForeground(Color.WHITE);
        Color okBaseColor = ThemeManager.isDarkMode()
                ? new Color(52, 152, 219)
                : new Color(41, 128, 185);
        Color okHoverColor = ThemeManager.isDarkMode()
                ? new Color(72, 170, 240)
                : new Color(52, 152, 219);
        Color okPressedColor = ThemeManager.isDarkMode()
                ? new Color(32, 120, 200)
                : new Color(31, 97, 141);
        okBtn.setBackground(okBaseColor);
        okBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(okBaseColor.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)
        ));
        okBtn.setFocusPainted(false);
        okBtn.setOpaque(true);
        okBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        okBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                okBtn.setBackground(okHoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                okBtn.setBackground(okBaseColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                okBtn.setBackground(okPressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                okBtn.setBackground(okBtn.contains(e.getPoint()) ? okHoverColor : okBaseColor);
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(okBtn);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mainContainer);
        dialog.getRootPane().setDefaultButton(okBtn);

        // Action listeners
        cancelBtn.addActionListener(ae -> {
            holder[0] = null;
            dialog.dispose();
        });

        okBtn.addActionListener(ae -> {
            String id = idField.getText().trim();
            String contact = contactField.getText().trim();
            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String addr = addressField.getText().trim();
            String arts = articlesField.getText().trim();

            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name ist erforderlich.", "Fehler",
                        JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                return;
            }

            holder[0] = new Object[]{id, contact, phone, email, addr, arts};
            dialog.dispose();
        });

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        SwingUtilities.invokeLater(idField::requestFocusInWindow);
        dialog.setVisible(true);
        return holder[0];
    }

    /**
     * Adjusts column widths based on the current size of the table's viewport.
     * Distributes extra space proportionally based on baseColumnWidths.
     */
    private void setupTableInteractions() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem edit = new JMenuItem(UnicodeSymbols.BETTER_EDIT + " Bearbeiten");
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
            @Override public void mouseClicked(MouseEvent e) {
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

            @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }

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
        if (tableScrollPane == null || vendorTable.getColumnCount() == 0) return;
        int available = tableScrollPane.getViewport().getWidth();
        if (available <= 0) available = tableScrollPane.getWidth();
        if (available <= 0) return;

        int totalBase = 0;
        for (int w : baseColumnWidths) totalBase += w;

        TableColumnModel tcm = vendorTable.getColumnModel();
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
            TableColumn c = tcm.getColumn(i);
            c.setPreferredWidth(newW[i]);
        }

        vendorTable.revalidate();
        vendorTable.repaint();
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