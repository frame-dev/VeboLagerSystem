package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.OrderManager;
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
        setTitle("Neue Bestellung erstellen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // Header
        RoundedPanel header = new RoundedPanel(new Color(255, 255, 255), 18);
        header.setPreferredSize(new Dimension(0, 72));
        header.setLayout(new GridBagLayout());
        JLabel title = new JLabel("Neue Bestellung");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setForeground(new Color(30, 40, 50));
        header.add(title);
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(new Color(245, 247, 250));
        headerWrapper.add(header);
        add(headerWrapper, BorderLayout.NORTH);

        // Main card
        RoundedPanel card = new RoundedPanel(Color.WHITE, 16);
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        card.setLayout(new BorderLayout(8, 8));

        // Order details panel
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(6, 6, 6, 6);
        r.fill = GridBagConstraints.HORIZONTAL;
        r.gridx = 0;
        r.gridy = 0;
        r.weightx = 1.0;

        departmentList = new JComboBox<>();
        fillDepartmentList();

        receiverNameCombobox = new JComboBox<>();
        fillReceiverNameCombobox();

        // Receiver / Sender form
        receiverKontoField = new JTextField();
        senderNameField = new JTextField();
        senderKontoField = new JTextField();
        senderKontoField.setText("4250 - 431.689");

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

        right.add(new JLabel("Empfänger Name:"), r);
        r.gridy++;
        right.add(receiverNameCombobox, r);
        r.gridy++;
        right.add(new JLabel("Empfänger Konto Nr.:"), r);
        r.gridy++;
        right.add(receiverKontoField, r);
        r.gridy++;
        right.add(new JLabel("Absender Name:"), r);
        r.gridy++;
        right.add(senderNameField, r);
        r.gridy++;
        right.add(new JLabel("Absender Konto Nr.:"), r);
        r.gridy++;
        right.add(senderKontoField, r);
        r.gridy++;
        right.add(new JLabel("Abteilung:"), r);
        r.gridy++;
        right.add(departmentList, r);

        // Order table (show unit price and line total)
        r.gridy++;
        r.fill = GridBagConstraints.BOTH;
        r.weighty = 0.5;
        orderTableModel = new DefaultTableModel(new String[]{"Artikel", "Menge", "Einzelpreis", "Gesamt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable orderTable = new JTable(orderTableModel);
        orderTable.setRowHeight(24);
        JScrollPane orderScroll = new JScrollPane(orderTable);
        right.add(orderScroll, r);

        // Bottom actions (create + total + pdf)
        r.gridy++;
        r.fill = GridBagConstraints.HORIZONTAL;
        r.weighty = 0.0;
        JPanel rightBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBottom.setOpaque(false);

        JButton addArticlesBtn = createRoundedButton("Artikel hinzufügen");
        addArticlesBtn.addActionListener(e -> addArticlesFromList());

        totalPriceLabel = new JLabel("Totalpreis: 0.00 CHF");
        JButton createOrderBtn = createRoundedButton("Bestellen");
        createOrderBtn.addActionListener(e -> onCreateOrder());
        JButton exportPdfBtn = createRoundedButton("Export PDF");
        exportPdfBtn.addActionListener(e -> {
            File file = chooseSaveFile();
            if (file != null) {
                try {
                    exportOrderToPDF(file);
                    JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(), "Erfolg", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        rightBottom.add(addArticlesBtn);
        rightBottom.add(totalPriceLabel);
        rightBottom.add(exportPdfBtn);
        rightBottom.add(createOrderBtn);
        right.add(rightBottom, r);

        card.add(right, BorderLayout.CENTER);

        // place card in center with padding background
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(new Color(245, 247, 250));
        centerWrapper.add(card);
        add(centerWrapper, BorderLayout.CENTER);

        // Menu
        setJMenuBar(createJMenu());

        // finalize
        pack();
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(null);
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
                JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Keine Artikel in der Bestellung.", "Fehler", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String receiver = receiverNameCombobox.getSelectedItem() != null ? receiverNameCombobox.getSelectedItem().toString().trim() : "";
        String rKonto = receiverKontoField.getText().trim();
        String sender = senderNameField.getText().trim();
        String sKonto = senderKontoField.getText().trim();
        String department = departmentList.getSelectedItem() != null ? departmentList.getSelectedItem().toString().trim() : "";
        if (receiver.isEmpty() || sender.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Empfänger und Absender Namen sind erforderlich.", "Fehler", JOptionPane.ERROR_MESSAGE);
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
                JOptionPane.showMessageDialog(this, "PDF erstellt: " + file.getAbsolutePath(), "Erfolg", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Fehler beim Erstellen des PDFs: " + ex.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
            }
        }
        JOptionPane.showMessageDialog(this, "Bestellung erstellt.", "Erfolgreich", JOptionPane.INFORMATION_MESSAGE);
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

    private JButton createRoundedButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBackground(new Color(237, 242, 247));
        button.setForeground(new Color(20, 30, 40));
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
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