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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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

    public NewOrderGUI() {
        ThemeManager tm = ThemeManager.getInstance();

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
        formTitle.setFont(formTitle.getFont().deriveFont(Font.BOLD, 17f));
        formTitle.setForeground(new Color(31, 45, 61));
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

        departmentList = new JComboBox<>();
        fillDepartmentList();

        receiverNameCombobox = new JComboBox<>();
        fillReceiverNameCombobox();
        receiverNameCombobox.addActionListener(listener -> {
            String selected = (String) receiverNameCombobox.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(NewOrderGUI.this, "Kein Empfänger Name gefunden",
                        "Fehler", JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
            }

            String department = ClientManager.getInstance().getDepartmentByName(selected);
            if (department != null) {
                departmentList.setSelectedItem(department);
            }
        });

        // Receiver / Sender form
        receiverKontoField = new JTextField();
        senderNameField = new JTextField();
        senderKontoField = new JTextField();
        senderKontoField.setText("4250 - 431.689");

        // Style text fields
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

        // Add key listener for when user types in the editable combobox
        departmentList.getEditor().getEditorComponent().addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String typedDept = departmentList.getEditor().getItem().toString().trim();
                if (!typedDept.isEmpty()) {
                    DepartmentManager departmentManager = DepartmentManager.getInstance();
                    Map<String, Object> dept = departmentManager.getDepartment(typedDept);
                    if (dept != null && dept.get("kontoNumber") != null) {
                        receiverKontoField.setText(dept.get("kontoNumber").toString());
                    }
                }
            }
        });

        // Add form fields with styled labels
        addStyledFormRow(formPanel, r, "👤 Empfänger Name:", receiverNameCombobox);
        addStyledFormRow(formPanel, r, "💳 Empfänger Konto Nr.:", receiverKontoField);
        addStyledFormRow(formPanel, r, "👤 Absender Name:", senderNameField);
        addStyledFormRow(formPanel, r, "💳 Absender Konto Nr.:", senderKontoField);
        addStyledFormRow(formPanel, r, "🏢 Abteilung:", departmentList);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        leftCard.add(formScroll, BorderLayout.CENTER);

        // Right panel - Order items
        RoundedPanel rightCard = new RoundedPanel(Color.WHITE, 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel tableTitle = new JLabel("🛒 Bestellte Artikel");
        tableTitle.setFont(tableTitle.getFont().deriveFont(Font.BOLD, 17f));
        tableTitle.setForeground(new Color(31, 45, 61));
        rightCard.add(tableTitle, BorderLayout.NORTH);

        rightCard.add(tableTitle, BorderLayout.NORTH);

        // Order table (show unit price and line total)
        orderTableModel = new DefaultTableModel(new String[]{"Artikel", "Menge", "Einzelpreis", "Gesamt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable orderTable = new JTable(orderTableModel);
        orderTable.setRowHeight(28);
        orderTable.setFont(orderTable.getFont().deriveFont(13f));
        orderTable.setShowGrid(true);
        orderTable.setGridColor(new Color(230, 236, 240));
        orderTable.getTableHeader().setBackground(new Color(52, 152, 219));
        orderTable.getTableHeader().setForeground(Color.WHITE);
        orderTable.getTableHeader().setFont(orderTable.getTableHeader().getFont().deriveFont(Font.BOLD, 13f));
        orderTable.setSelectionBackground(new Color(52, 152, 219, 30));

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 230), 1));
        rightCard.add(orderScroll, BorderLayout.CENTER);

        // Bottom panel with total and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Total price with styled label
        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        totalPanel.setOpaque(false);
        totalPriceLabel = new JLabel("Totalpreis: 0.00 CHF");
        totalPriceLabel.setFont(totalPriceLabel.getFont().deriveFont(Font.BOLD, 16f));
        totalPriceLabel.setForeground(new Color(46, 204, 113));
        totalPanel.add(totalPriceLabel);
        bottomPanel.add(totalPanel, BorderLayout.WEST);

        // Action buttons
        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        JButton addArticlesBtn = createStyledButton("📦 Artikel hinzufügen", new Color(52, 152, 219));
        addArticlesBtn.addActionListener(e -> addArticlesFromList());

        JButton exportPdfBtn = createStyledButton("📄 Export PDF", new Color(241, 196, 15));
        exportPdfBtn.addActionListener(e -> {
            File file = chooseSaveFile();
            if (file != null) {
                try {
                    exportOrderToPDF(file);
                    JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(), "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                            Main.iconSmall);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });

        JButton createOrderBtn = createStyledButton("✓ Bestellen", new Color(46, 204, 113));
        createOrderBtn.addActionListener(e -> onCreateOrder());

        actionButtons.add(addArticlesBtn);
        actionButtons.add(exportPdfBtn);
        actionButtons.add(createOrderBtn);
        bottomPanel.add(actionButtons, BorderLayout.EAST);

        rightCard.add(bottomPanel, BorderLayout.SOUTH);

        // Add cards to split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftCard, rightCard);
        splitPane.setDividerLocation(450);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        mainContent.add(splitPane, BorderLayout.CENTER);
        add(mainContent, BorderLayout.CENTER);

        // Menu
        setJMenuBar(createJMenu());

        // finalize
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
    }

    private void styleTextField(JTextField field) {
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private void addStyledFormRow(JPanel panel, GridBagConstraints gbc, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(new Color(52, 73, 94));
        panel.add(label, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(2, 8, 12, 8);
        panel.add(field, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(8, 8, 8, 8);
    }

    private void fillReceiverNameCombobox() {
        receiverNameCombobox.removeAllItems();
        receiverNameCombobox.addItem(""); // Empty option

        Set<String> clientNames = new LinkedHashSet<>();
        ClientManager clientManager = ClientManager.getInstance();
        for (var client : clientManager.getAllClients()) {
            String name = client.get("firstLastName");
            if (name != null && !name.trim().isEmpty()) {
                clientNames.add(name.trim());
            }
        }

        // Add sorted client names to combo box
        clientNames.stream().sorted().forEach(receiverNameCombobox::addItem);

        // Make it editable so users can enter custom names
        receiverNameCombobox.setEditable(true);
    }

    private void fillDepartmentList() {
        departmentList.removeAllItems();
        departmentList.addItem(""); // Empty option

        Set<String> departments = new LinkedHashSet<>();
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        for (var department : departmentManager.getAllDepartments()) {
            String dept = (String) department.get("department");
            if (dept != null && !dept.trim().isEmpty()) {
                departments.add(dept.trim());
            }
        }

        // Add sorted departments to combo box
        departments.stream().sorted().forEach(departmentList::addItem);

        // Make it editable so users can enter custom departments
        departmentList.setEditable(true);
    }

    private File chooseSaveFile() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"), "order_" + System.currentTimeMillis() + ".pdf"));
        int res = fc.showSaveDialog(this);
        return res == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile() : null;
    }

    private void addArticlesFromList() {
        // Get all articles from ArticleListGUI
        Map<Article, Integer> articlesWithQty = ArticleListGUI.getArticlesAndQuantity();

        if (articlesWithQty.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Keine Artikel in der Artikelliste. Bitte fügen Sie zuerst Artikel hinzu.",
                    "Keine Artikel",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        // Add all articles to the order
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
            orderTableModel.addRow(new Object[]{a.getName(), qty, String.format("%.2f CHF", unit), String.format("%.2f CHF", line)});
        }
    }

    private double safePrice(Article a) {
        try {
            // assumes Article has getPrice()
            return a.getSellPrice();
        } catch (NoSuchMethodError | AbstractMethodError | RuntimeException ex) {
            // Article has no price or error: treat as 0
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
            JOptionPane.showMessageDialog(this, "Keine Artikel in der Bestellung.", "Fehler", JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }
        String receiver = receiverNameCombobox.getSelectedItem() != null ? receiverNameCombobox.getSelectedItem().toString().trim() : "";
        String rKonto = receiverKontoField.getText().trim();
        String sender = senderNameField.getText().trim();
        String sKonto = senderKontoField.getText().trim();
        String department = departmentList.getSelectedItem() != null ? departmentList.getSelectedItem().toString().trim() : "";
        if (receiver.isEmpty() || sender.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empfänger und Absender Namen sind erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE
                    , Main.iconSmall);
            return;
        }
        // convert to Map\<String,Integer\> expected by Order (use article number as key)
        Map<String, Integer> payload = new LinkedHashMap<>();
        for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
            payload.put(e.getKey().getArticleNumber(), e.getValue());
        }
        createOrder(payload, receiver, rKonto, sender, sKonto, department);
        File file = chooseSaveFile();
        if (file != null) {
            try {
                exportOrderToPDF(file);
                JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(), "Erfolg", JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
            }
        }
        JOptionPane.showMessageDialog(this, "Bestellung erstellt.", "Erfolgreich", JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
        orderArticles.clear();
        rebuildOrderTable();
        updateTotalPrice();
    }

    public void display() {
        setVisible(true);
        if (Main.articleListGUI != null) Main.articleListGUI.display();
    }

    public void createOrder(Map<String, Integer> orderArticles, String receiverName, String receiverKontoNumber,
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
        OrderManager orderManager = OrderManager.getInstance();
        orderManager.insertOrder(order);
    }

    @SuppressWarnings("deprecation")
    private void exportOrderToPDF(File file) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // Load fonts
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

                // === HEADER SECTION ===
                // Company name / logo area (top right)
                cs.setNonStrokingColor(30, 58, 95); // Dark blue
                cs.addRect(margin, yPosition, pageWidth - 2 * margin, 60);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255); // White
                cs.setFont(boldFont, 24);
                cs.newLineAtOffset(margin + 10, yPosition + 25);
                cs.showText("VEBO BESTELLUNG");
                cs.endText();

                yPosition -= 80;

                // Date and Order ID section
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

                // === SENDER / RECEIVER SECTION ===
                // Draw background boxes
                float boxHeight = 85;
                cs.setNonStrokingColor(245, 247, 250); // Light gray
                cs.addRect(margin, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.setNonStrokingColor(245, 247, 250);
                cs.addRect(margin + (pageWidth - 2 * margin) / 2 + 5, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                // Sender (left box)
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

                // Receiver (right box)
                float rightBoxX = margin + (pageWidth - 2 * margin) / 2 + 15;
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(rightBoxX, yPosition - 15);
                cs.showText("EMPFÄNGER");
                cs.endText();

                String receiver = receiverNameCombobox.getSelectedItem() != null ? receiverNameCombobox.getSelectedItem().toString().trim() : "";
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

                // === ARTICLES TABLE ===
                // Table header
                cs.setNonStrokingColor(62, 84, 98); // Dark blue-gray
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

                // Table rows
                double total = 0.0;
                boolean alternateRow = false;
                for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
                    Article a = e.getKey();
                    int qty = e.getValue();
                    double unit = safePrice(a);
                    double line = unit * qty;
                    total += line;

                    // Alternate row background
                    if (alternateRow) {
                        cs.setNonStrokingColor(250, 250, 250);
                        cs.addRect(margin, yPosition - 15, pageWidth - 2 * margin, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.setFont(regularFont, 9);
                    cs.newLineAtOffset(margin + 5, yPosition - 10);
                    // Truncate long article names
                    String articleName = a.getName();
                    if (articleName.length() > 35) {
                        articleName = articleName.substring(0, 32) + "...";
                    }
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

                    // Check if we need a new page
                    if (yPosition < 150) {
                        PDPage newPage = new PDPage();
                        doc.addPage(newPage);
                        cs.close();
                        // Continue on new page (simplified - in production you'd handle this better)
                        break;
                    }
                }

                // === TOTALS SECTION ===
                yPosition -= 10;
                cs.setNonStrokingColor(200, 200, 200);
                cs.setLineWidth(1);
                cs.moveTo(margin, yPosition);
                cs.lineTo(pageWidth - margin, yPosition);
                cs.stroke();

                yPosition -= 25;

                // Total box
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

                // === FOOTER ===
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
        help.addActionListener(e -> JOptionPane.showMessageDialog(this, getHelpText(), "Hilfe - Neue Bestellung erstellen", JOptionPane.INFORMATION_MESSAGE));
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

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
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