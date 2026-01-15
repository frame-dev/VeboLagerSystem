package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.utils.ThemeManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Modern new order GUI with improved visual design and user experience.
 * Features: Split panel layout, gradient header, styled components, and PDF export (PDFBox).
 */
public class NewOrderGUI extends JFrame {

    private final DefaultTableModel orderTableModel;
    // track Article -> qty so prices are available
    private final Map<Article, Integer> orderArticles = new LinkedHashMap<>();
    private final JLabel totalPriceLabel;

    private final JComboBox<String> receiverNameCombobox;
    private final JTextField receiverKontoField;
    private final JTextField senderNameField;
    private final JTextField senderKontoField;

    private final JComboBox<String> departmentList;

    private JTable orderTable;

    public NewOrderGUI() {
        ThemeManager tm = ThemeManager.getInstance();
        tm.registerWindow(this);

        setTitle("Neue Bestellung erstellen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Header with gradient
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(tm.getBackgroundColor());

        GradientPanel header = new GradientPanel(
                tm.getPrimaryColor(),
                tm.getAccentColor()
        );
        header.setPreferredSize(new Dimension(900, 80));
        header.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel("📝");
        iconLabel.setFont(iconLabel.getFont().deriveFont(Font.BOLD, 36f));
        iconLabel.setForeground(tm.getTextOnPrimaryColor());

        JLabel title = new JLabel("  Neue Bestellung Erstellen");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        title.setForeground(tm.getTextOnPrimaryColor());

        JPanel headerContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        headerContent.setOpaque(false);
        headerContent.add(iconLabel);
        headerContent.add(title);
        header.add(headerContent);

        headerWrapper.add(header);
        add(headerWrapper, BorderLayout.NORTH);

        // Main content with split pane
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(tm.getBackgroundColor());
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Left panel - Form details
        RoundedPanel leftCard = new RoundedPanel(tm.getCardBackgroundColor(), 16);
        leftCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftCard.setLayout(new BorderLayout(10, 10));

        JLabel formTitle = new JLabel("📋 Bestellinformationen");
        formTitle.setFont(formTitle.getFont().deriveFont(Font.BOLD, 17f));
        formTitle.setForeground(tm.getTextPrimaryColor());
        leftCard.add(formTitle, BorderLayout.NORTH);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(8, 8, 8, 8);
        r.fill = GridBagConstraints.HORIZONTAL;
        r.gridx = 0;
        r.gridy = 0;
        r.weightx = 1.0;

        // Comboboxes
        departmentList = new JComboBox<>();
        fillDepartmentList();

        receiverNameCombobox = new JComboBox<>();
        fillReceiverNameCombobox();

        // Apply theme styling to inputs
        styleComboBox(receiverNameCombobox);
        styleComboBox(departmentList);

        receiverNameCombobox.addActionListener(listener -> {
            String selected = (String) receiverNameCombobox.getSelectedItem();
            if (selected == null || selected.trim().isEmpty()) return;

            String department = ClientManager.getInstance().getDepartmentByName(selected);
            if (department != null && !department.trim().isEmpty()) {
                departmentList.setSelectedItem(department);
            }
        });

        // Receiver / Sender form
        receiverKontoField = new JTextField();
        senderNameField = new JTextField();
        senderKontoField = new JTextField();
        senderKontoField.setText("4250 - 431.689");

        styleTextField(receiverKontoField);
        styleTextField(senderNameField);
        styleTextField(senderKontoField);

        departmentList.addActionListener(event -> {
            String selectedDept = (String) departmentList.getSelectedItem();
            if (selectedDept != null && !selectedDept.trim().isEmpty()) {
                DepartmentManager departmentManager = DepartmentManager.getInstance();
                Map<String, Object> dept = departmentManager.getDepartment(selectedDept);
                if (dept != null && dept.get("kontoNumber") != null) {
                    receiverKontoField.setText(dept.get("kontoNumber").toString());
                }
            }
        });

        // When user types in the editable combobox editor
        Component editor = departmentList.getEditor().getEditorComponent();
        if (editor instanceof JTextField tf) {
            tf.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyReleased(java.awt.event.KeyEvent e) {
                    String typedDept = departmentList.getEditor().getItem().toString().trim();
                    if (!typedDept.isEmpty()) {
                        Map<String, Object> dept = DepartmentManager.getInstance().getDepartment(typedDept);
                        if (dept != null && dept.get("kontoNumber") != null) {
                            receiverKontoField.setText(dept.get("kontoNumber").toString());
                        }
                    }
                }
            });
        }

        // Add form fields
        addStyledFormRow(formPanel, r, "👤 Empfänger Name:", receiverNameCombobox);
        addStyledFormRow(formPanel, r, "💳 Empfänger Konto Nr.:", receiverKontoField);
        addStyledFormRow(formPanel, r, "👤 Absender Name:", senderNameField);
        addStyledFormRow(formPanel, r, "💳 Absender Konto Nr.:", senderKontoField);
        addStyledFormRow(formPanel, r, "🏢 Abteilung:", departmentList);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setBackground(tm.getCardBackgroundColor());
        formScroll.getViewport().setBackground(tm.getCardBackgroundColor());
        leftCard.add(formScroll, BorderLayout.CENTER);

        // Right panel - Order items
        RoundedPanel rightCard = new RoundedPanel(tm.getCardBackgroundColor(), 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel tableTitle = new JLabel("🛒 Bestellte Artikel");
        tableTitle.setFont(tableTitle.getFont().deriveFont(Font.BOLD, 17f));
        tableTitle.setForeground(tm.getTextPrimaryColor());
        rightCard.add(tableTitle, BorderLayout.NORTH);

        // Order table (show unit price and line total)
        orderTableModel = new DefaultTableModel(new String[]{"Artikel", "Menge", "Einzelpreis", "Gesamt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return switch (columnIndex) {
                    case 1 -> Integer.class;
                    default -> String.class;
                };
            }
        };

        orderTable = new JTable(orderTableModel);
        applyOrderTableTheme(orderTable);

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.setBorder(BorderFactory.createLineBorder(tm.getBorderColor(), 1));
        orderScroll.setBackground(tm.getCardBackgroundColor());
        orderScroll.getViewport().setBackground(tm.getCardBackgroundColor());
        rightCard.add(orderScroll, BorderLayout.CENTER);

        // Bottom panel with total and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        totalPanel.setOpaque(false);
        totalPriceLabel = new JLabel("Totalpreis: 0.00 CHF");
        totalPriceLabel.setFont(totalPriceLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalPriceLabel.setForeground(tm.getSuccessColor());
        totalPanel.add(totalPriceLabel);
        bottomPanel.add(totalPanel, BorderLayout.WEST);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        JButton addArticlesBtn = createThemeButton("📦 Artikel hinzufügen", tm.getPrimaryColor());
        addArticlesBtn.addActionListener(e -> addArticlesFromList());

        JButton exportPdfBtn = createThemeButton("📄 Export PDF", tm.getWarningColor());
        exportPdfBtn.addActionListener(e -> {
            File file = chooseSaveFile();
            if (file != null) {
                try {
                    exportOrderToPDF(file);
                    JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(),
                            "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                }
            }
        });

        JButton createOrderBtn = createThemeButton("✓ Bestellen", tm.getSuccessColor());
        createOrderBtn.addActionListener(e -> onCreateOrder());

        actionButtons.add(addArticlesBtn);
        actionButtons.add(exportPdfBtn);
        actionButtons.add(createOrderBtn);
        bottomPanel.add(actionButtons, BorderLayout.EAST);

        rightCard.add(bottomPanel, BorderLayout.SOUTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCard, rightCard);
        splitPane.setDividerLocation(450);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        mainContent.add(splitPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        setJMenuBar(createJMenu());

        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void styleTextField(JTextField field) {
        ThemeManager tm = ThemeManager.getInstance();
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tm.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(tm.getInputBackgroundColor());
        field.setForeground(tm.getTextPrimaryColor());
        field.setCaretColor(tm.getTextPrimaryColor());
    }

    private void styleComboBox(JComboBox<String> combo) {
        ThemeManager tm = ThemeManager.getInstance();

        Color bg = tm.getInputBackgroundColor();
        Color fg = tm.getTextPrimaryColor();
        Color border = tm.getInputBorderColor();
        Color selBg = tm.getSelectionBackgroundColor();
        Color selFg = tm.getSelectionForegroundColor();
        Color surface = tm.getSurfaceColor();

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
                JButton b = new JButton("▾");
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

    private void applyOrderTableTheme(JTable table) {
        ThemeManager tm = ThemeManager.getInstance();

        table.setRowHeight(28);
        table.setFont(table.getFont().deriveFont(13f));
        table.setShowGrid(true);
        table.setGridColor(tm.getTableGridColor());
        table.setBackground(tm.getCardBackgroundColor());
        table.setForeground(tm.getTextPrimaryColor());
        table.setSelectionBackground(tm.getSelectionBackgroundColor());
        table.setSelectionForeground(tm.getSelectionForegroundColor());
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(tm.getTableHeaderBackgroundColor());
        header.setForeground(tm.getTableHeaderForegroundColor());
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(tm.getSelectionBackgroundColor());
                    c.setForeground(tm.getSelectionForegroundColor());
                } else {
                    c.setBackground(row % 2 == 0 ? tm.getTableRowEvenColor() : tm.getTableRowOddColor());
                    c.setForeground(tm.getTextPrimaryColor());
                }

                if (c instanceof JComponent jc) {
                    jc.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                }
                return c;
            }
        };

        table.setDefaultRenderer(Object.class, renderer);
        table.setDefaultRenderer(String.class, renderer);
        table.setDefaultRenderer(Integer.class, renderer);
    }

    private void addStyledFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        ThemeManager tm = ThemeManager.getInstance();
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(tm.getTextPrimaryColor());
        panel.add(label, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 8, 12, 8);
        panel.add(field, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
    }

    private void fillReceiverNameCombobox() {
        receiverNameCombobox.removeAllItems();
        receiverNameCombobox.addItem("");

        Set<String> clientNames = new LinkedHashSet<>();
        ClientManager clientManager = ClientManager.getInstance();
        for (var client : clientManager.getAllClients()) {
            String name = client.get("firstLastName");
            if (name != null && !name.trim().isEmpty()) {
                clientNames.add(name.trim());
            }
        }

        clientNames.stream().sorted().forEach(receiverNameCombobox::addItem);
        receiverNameCombobox.setEditable(true);
    }

    private void fillDepartmentList() {
        departmentList.removeAllItems();
        departmentList.addItem("");

        Set<String> departments = new LinkedHashSet<>();
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        for (var department : departmentManager.getAllDepartments()) {
            String dept = (String) department.get("department");
            if (dept != null && !dept.trim().isEmpty()) {
                departments.add(dept.trim());
            }
        }

        departments.stream().sorted().forEach(departmentList::addItem);
        departmentList.setEditable(true);
    }

    private File chooseSaveFile() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"),
                "order_" + System.currentTimeMillis() + ".pdf"));
        int res = fc.showSaveDialog(this);
        return res == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    private void addArticlesFromList() {
        Map<Article, Integer> articlesWithQty = ArticleListGUI.getArticlesAndQuantity();

        if (articlesWithQty.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Keine Artikel in der Artikelliste. Bitte fügen Sie zuerst Artikel hinzu.",
                    "Keine Artikel",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        for (Map.Entry<Article, Integer> entry : articlesWithQty.entrySet()) {
            Article a = entry.getKey();
            Integer qty = entry.getValue();

            if (qty != null && qty > 0) {
                orderArticles.put(a, qty);
            }
        }

        rebuildOrderTable();
        updateTotalPrice();

        JOptionPane.showMessageDialog(this,
                articlesWithQty.size() + " Artikel zur Bestellung hinzugefügt.",
                "Erfolgreich",
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
    }

    private void rebuildOrderTable() {
        orderTableModel.setRowCount(0);
        for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
            Article a = e.getKey();
            int qty = e.getValue();
            double unit = safePrice(a);
            double line = unit * qty;
            orderTableModel.addRow(new Object[]{
                    a.getName(),
                    qty,
                    String.format("%.2f CHF", unit),
                    String.format("%.2f CHF", line)
            });
        }
    }

    private double safePrice(Article a) {
        try {
            return a.getSellPrice();
        } catch (NoSuchMethodError | AbstractMethodError | RuntimeException ex) {
            return 0.0;
        }
    }

    private void updateTotalPrice() {
        double total = 0.0;
        for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
            total += safePrice(e.getKey()) * e.getValue();
        }
        totalPriceLabel.setText(String.format("Totalpreis: %.2f CHF", total));
    }

    private void onCreateOrder() {
        if (orderArticles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keine Artikel in der Bestellung.",
                    "Fehler", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            return;
        }

        String receiver = receiverNameCombobox.getSelectedItem() != null
                ? receiverNameCombobox.getSelectedItem().toString().trim()
                : "";
        String rKonto = receiverKontoField.getText().trim();
        String sender = senderNameField.getText().trim();
        String sKonto = senderKontoField.getText().trim();
        String department = departmentList.getSelectedItem() != null
                ? departmentList.getSelectedItem().toString().trim()
                : "";

        if (receiver.isEmpty() || sender.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Empfänger und Absender Namen sind erforderlich.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            return;
        }

        Map<String, Integer> payload = new LinkedHashMap<>();
        for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
            payload.put(e.getKey().getArticleNumber(), e.getValue());
        }

        createOrder(payload, receiver, rKonto, sender, sKonto, department);

        File file = chooseSaveFile();
        if (file != null) {
            try {
                exportOrderToPDF(file);
                JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(),
                        "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(),
                        "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            }
        }

        JOptionPane.showMessageDialog(this, "Bestellung erstellt.",
                "Erfolgreich", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);

        orderArticles.clear();
        rebuildOrderTable();
        updateTotalPrice();
    }

    public void display() {
        setVisible(true);
        if (Main.articleListGUI != null) Main.articleListGUI.display();
    }

    @SuppressWarnings("deprecation")
    private void exportOrderToPDF(File file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDFont regularFont = null;
            PDFont boldFont = null;
            File regularTtf = new File("/Library/Fonts/Arial.ttf");
            File boldTtf = new File("/Library/Fonts/Arial Bold.ttf");

            if (regularTtf.exists()) {
                regularFont = PDType0Font.load(doc, regularTtf);
                if (boldTtf.exists()) {
                    boldFont = PDType0Font.load(doc, boldTtf);
                } else {
                    boldFont = regularFont;
                }
            } else {
                try {
                    Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                    Object helv = c.getField("HELVETICA").get(null);
                    Object helvBold = c.getField("HELVETICA_BOLD").get(null);
                    if (helv instanceof PDFont) regularFont = (PDFont) helv;
                    if (helvBold instanceof PDFont) boldFont = (PDFont) helvBold;
                    if (regularFont != null && boldFont == null) boldFont = regularFont;
                } catch (Exception ignored) {
                }
            }

            if (regularFont == null) {
                throw new IOException("No usable font found (install a system TTF or add PDFBox 2.x).");
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float margin = 50;
                float yPosition = 750;

                cs.setNonStrokingColor(30, 58, 95);
                cs.addRect(margin, yPosition, pageWidth - 2 * margin, 60);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 24);
                cs.newLineAtOffset(margin + 10, yPosition + 25);
                cs.showText("VEBO BESTELLUNG");
                cs.endText();

                yPosition -= 80;

                cs.beginText();
                cs.setNonStrokingColor(0, 0, 0);
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestelldatum:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
                cs.endText();

                yPosition -= 18;

                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestell-ID:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText("ORD" + System.currentTimeMillis());
                cs.endText();

                yPosition -= 30;

                float boxHeight = 85;
                cs.setNonStrokingColor(245, 247, 250);
                cs.addRect(margin, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.setNonStrokingColor(245, 247, 250);
                cs.addRect(margin + (pageWidth - 2 * margin) / 2 + 5, yPosition - boxHeight,
                        (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(0, 0, 0);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(margin + 10, yPosition - 15);
                cs.showText("ABSENDER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(margin + 10, yPosition - 32);
                cs.showText(senderNameField.getText().trim());
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(margin + 10, yPosition - 47);
                cs.showText("Konto: " + senderKontoField.getText().trim());
                cs.endText();

                float rightBoxX = margin + (pageWidth - 2 * margin) / 2 + 15;
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(rightBoxX, yPosition - 15);
                cs.showText("EMPFÄNGER");
                cs.endText();

                String receiver = receiverNameCombobox.getSelectedItem() != null
                        ? receiverNameCombobox.getSelectedItem().toString().trim()
                        : "";
                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(rightBoxX, yPosition - 32);
                cs.showText(receiver);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 47);
                cs.showText("Konto: " + receiverKontoField.getText().trim());
                cs.endText();

                String dept = departmentList.getSelectedItem() != null ? departmentList.getSelectedItem().toString().trim() : "";
                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 62);
                cs.showText("Abteilung: " + dept);
                cs.endText();

                yPosition -= boxHeight + 30;

                cs.setNonStrokingColor(62, 84, 98);
                cs.addRect(margin, yPosition - 20, pageWidth - 2 * margin, 20);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 10);
                cs.newLineAtOffset(margin + 5, yPosition - 14);
                cs.showText("Artikel");
                cs.newLineAtOffset(200, 0);
                cs.showText("Menge");
                cs.newLineAtOffset(60, 0);
                cs.showText("Einzelpreis");
                cs.newLineAtOffset(80, 0);
                cs.showText("Gesamt");
                cs.endText();

                yPosition -= 25;

                double total = 0.0;
                boolean alternateRow = false;
                for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
                    Article a = e.getKey();
                    int qty = e.getValue();
                    double unit = safePrice(a);
                    double line = unit * qty;
                    total += line;

                    if (alternateRow) {
                        cs.setNonStrokingColor(250, 250, 250);
                        cs.addRect(margin, yPosition - 15, pageWidth - 2 * margin, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.setFont(regularFont, 9);
                    cs.newLineAtOffset(margin + 5, yPosition - 10);

                    String articleName = a.getName();
                    if (articleName.length() > 35) articleName = articleName.substring(0, 32) + "...";

                    cs.showText(articleName + " (" + a.getArticleNumber() + ")");
                    cs.newLineAtOffset(200, 0);
                    cs.showText(String.valueOf(qty));
                    cs.newLineAtOffset(60, 0);
                    cs.showText(String.format("%.2f CHF", unit));
                    cs.newLineAtOffset(80, 0);
                    cs.showText(String.format("%.2f CHF", line));
                    cs.endText();

                    yPosition -= 18;
                    alternateRow = !alternateRow;

                    if (yPosition < 150) break;
                }

                yPosition -= 10;
                cs.setNonStrokingColor(200, 200, 200);
                cs.setLineWidth(1);
                cs.moveTo(margin, yPosition);
                cs.lineTo(pageWidth - margin, yPosition);
                cs.stroke();

                yPosition -= 25;

                cs.setNonStrokingColor(30, 58, 95);
                cs.addRect(pageWidth - margin - 150, yPosition - 25, 150, 30);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(pageWidth - margin - 140, yPosition - 15);
                cs.showText("TOTAL:");
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format("%.2f CHF", total));
                cs.endText();

                cs.beginText();
                cs.setNonStrokingColor(150, 150, 150);
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(margin, 30);
                cs.showText("VEBO Lagersystem - Generiert am " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
                cs.endText();
            }

            doc.save(file);
        }
    }

    private JMenuBar createJMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Bestellung");
        JMenuItem help = new JMenuItem("Hilfe");
        help.addActionListener(e -> JOptionPane.showMessageDialog(this, getHelpText(),
                "Hilfe - Neue Bestellung erstellen", JOptionPane.INFORMATION_MESSAGE));
        menu.add(help);
        menuBar.add(menu);
        return menuBar;
    }

    private String getHelpText() {
        return "<html><h2>Hilfe - Neue Bestellung erstellen</h2>" +
                "<p>In diesem Fenster können Sie eine neue Bestellung zusammenstellen.</p>" +
                "<ul>" +
                "<li>Wählen Sie Artikel aus der Liste und fügen Sie Mengen hinzu.</li>" +
                "<li>Geben Sie Empfänger und Absender an.</li>" +
                "<li>Klicken Sie auf \"Bestellen\" um die Bestellung zu speichern.</li>" +
                "<li>Mit \"Export PDF\" erzeugen Sie ein PDF mit allen Daten.</li>" +
                "</ul>";
    }

    private JButton createThemeButton(String text, Color baseBg) {
        ThemeManager tm = ThemeManager.getInstance();

        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        Color hoverBg = tm.getButtonHoverColor(baseBg);
        Color pressedBg = tm.getButtonPressedColor(baseBg);

        button.setBackground(baseBg);
        button.setForeground(tm.getTextOnPrimaryColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(baseBg);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (button.isEnabled()) button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!button.isEnabled()) return;
                button.setBackground(button.contains(e.getPoint()) ? hoverBg : baseBg);
            }
        });

        return button;
    }

    private void createOrder(Map<String, Integer> orderArticles, String receiverName, String receiverKontoNumber,
                             String senderName, String senderKontoNumber, String department) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String date = dateFormat.format(new Date());
        Order order = new Order(
                "ORD" + System.currentTimeMillis(),
                orderArticles,
                receiverName,
                receiverKontoNumber,
                date,
                senderName,
                senderKontoNumber,
                department
        );
        OrderManager.getInstance().insertOrder(order);
    }

    // Gradient panel for header
    private static class GradientPanel extends JPanel {
        private final Color color1;
        private final Color color2;

        GradientPanel(Color color1, Color color2) {
            this.color1 = color1;
            this.color2 = color2;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
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