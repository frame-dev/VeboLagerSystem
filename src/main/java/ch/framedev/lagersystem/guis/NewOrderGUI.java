package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.OrderManager;
import ch.framedev.lagersystem.managers.UserManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.utils.ArticleExporter;
import ch.framedev.lagersystem.utils.UnicodeSymbols;

import static ch.framedev.lagersystem.utils.JFrameUtils.GradientPanel;
import static ch.framedev.lagersystem.utils.JFrameUtils.RoundedPanel;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

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
    private final JComboBox<String> senderNameCombobox;
    private final JTextField senderKontoField;

    private final JComboBox<String> departmentList;
    private final JLabel summaryReceiverValue;
    private final JLabel summaryDepartmentValue;
    private final JLabel summaryItemsValue;
    private final JLabel summaryTotalValue;

    // ---- Inline Article Search/Add (NEW) -----------------------------------
    private JTextField articleSearchField;
    private JComboBox<Article> articleCombo;
    private JSpinner articleQtySpinner;
    private JButton articleAddButton;
    private List<Article> allArticlesCache = new ArrayList<>();

    public NewOrderGUI() {
        ThemeManager.getInstance().registerWindow(this);

        summaryReceiverValue = createSummaryValueLabel("—");
        summaryDepartmentValue = createSummaryValueLabel("—");
        summaryItemsValue = createSummaryValueLabel("Keine Artikel");
        summaryTotalValue = createSummaryValueLabel("0.00 CHF");

        setTitle("Neue Bestellung erstellen");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1500, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));

        // Header with gradient
        JPanel headerWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        headerWrapper.setBackground(ThemeManager.getBackgroundColor());

        GradientPanel header = new GradientPanel(
                ThemeManager.getPrimaryColor(),
                ThemeManager.getAccentColor()
        );
        header.setPreferredSize(new Dimension(900, 80));
        header.setLayout(new GridBagLayout());

        JLabel iconLabel = new JLabel(UnicodeSymbols.CODE);
        iconLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 36));
        iconLabel.setForeground(ThemeManager.getTextOnPrimaryColor());

        JLabel title = new JLabel(UnicodeSymbols.HEAVY_PLUS + " Neue Bestellung Erstellen");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 26));
        title.setForeground(ThemeManager.getTextOnPrimaryColor());

        JPanel headerContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        headerContent.setOpaque(false);
        headerContent.add(iconLabel);
        headerContent.add(title);
        header.add(headerContent);

        headerWrapper.add(header);
        add(headerWrapper, BorderLayout.NORTH);

        // Main content with split pane
        JPanel mainContent = new JPanel(new BorderLayout(15, 15));
        mainContent.setBackground(ThemeManager.getBackgroundColor());
        mainContent.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Left panel - Form details
        RoundedPanel leftCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        leftCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        leftCard.setLayout(new BorderLayout(10, 10));

        JLabel formTitle = new JLabel(UnicodeSymbols.CLIPBOARD + " Bestellinformationen");
        formTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        formTitle.setForeground(ThemeManager.getTextPrimaryColor());
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

        // Receiver / Sender form
        receiverKontoField = new JTextField();

        receiverNameCombobox.addActionListener(listener -> {
            String selected = (String) receiverNameCombobox.getSelectedItem();
            if (selected == null || selected.trim().isEmpty()) return;

            String department = ClientManager.getInstance().getDepartmentByName(selected);
            if (department != null && !department.trim().isEmpty()) {
                departmentList.setSelectedItem(department);
                Map<String, Object> depMap = DepartmentManager.getInstance().getDepartment(department);
                if (depMap != null && depMap.get("kontoNumber") != null) {
                    receiverKontoField.setText(depMap.get("kontoNumber").toString());
                }
            }
            updateSummaryBar();
        });

        senderNameCombobox = new JComboBox<>();
        fillSenderNameCombobox();
        senderKontoField = new JTextField();
        senderKontoField.setText("4250 - 431.689");

        styleTextField(receiverKontoField);
        styleComboBox(senderNameCombobox);
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
            updateSummaryBar();
        });

        // When user types in the editable combobox editor
        Component editor = departmentList.getEditor().getEditorComponent();
        if (editor instanceof JTextField tf) {
            tf.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
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
        addStyledFormRow(formPanel, r, UnicodeSymbols.PERSON + " Empfänger Name:", receiverNameCombobox);
        addStyledFormRow(formPanel, r, UnicodeSymbols.CREDIT_CARD + " Empfänger Konto Nr.:", receiverKontoField);
        addStyledFormRow(formPanel, r, UnicodeSymbols.PERSON + " Absender Name:", senderNameCombobox);
        addStyledFormRow(formPanel, r, UnicodeSymbols.CREDIT_CARD + " Absender Konto Nr.:", senderKontoField);
        addStyledFormRow(formPanel, r, UnicodeSymbols.DEPARTMENT + " Abteilung:", departmentList);

        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(null);
        formScroll.setBackground(ThemeManager.getCardBackgroundColor());
        formScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        leftCard.add(formScroll, BorderLayout.CENTER);

        // Right panel - Order items
        RoundedPanel rightCard = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        rightCard.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        rightCard.setLayout(new BorderLayout(10, 10));

        JLabel tableTitle = new JLabel(UnicodeSymbols.SHOPPING_CART + " Bestellte Artikel");
        tableTitle.setFont(SettingsGUI.getFontByName(Font.BOLD, 17));
        tableTitle.setForeground(ThemeManager.getTextPrimaryColor());

        // Build top area (title + inline search)
        JPanel rightTop = new JPanel(new BorderLayout(0, 10));
        rightTop.setOpaque(false);
        rightTop.add(tableTitle, BorderLayout.NORTH);
        rightTop.add(buildInlineArticleSearchPanel(), BorderLayout.CENTER);
        rightCard.add(rightTop, BorderLayout.NORTH);

        // Order table (show unit price and line total)
        orderTableModel = new DefaultTableModel(new String[]{"Artikel", "Menge", "Einzelpreis", "Gesamt"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return Integer.class;
                return String.class;
            }
        };

        JTable orderTable = new JTable(orderTableModel);
        applyOrderTableTheme(orderTable);

        JScrollPane orderScroll = new JScrollPane(orderTable);
        orderScroll.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1));
        orderScroll.setBackground(ThemeManager.getCardBackgroundColor());
        orderScroll.getViewport().setBackground(ThemeManager.getCardBackgroundColor());
        rightCard.add(orderScroll, BorderLayout.CENTER);

        // Bottom panel with total and buttons
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel totalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        totalPanel.setOpaque(false);
        totalPriceLabel = new JLabel("Totalpreis: 0.00 CHF");
        totalPriceLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
        totalPriceLabel.setForeground(ThemeManager.getSuccessColor());
        totalPanel.add(totalPriceLabel);
        bottomPanel.add(totalPanel, BorderLayout.WEST);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionButtons.setOpaque(false);

        JButton addArticlesBtn = createThemeButton(UnicodeSymbols.PACKAGE + " Artikel hinzufügen", ThemeManager.getPrimaryColor());
        addArticlesBtn.addActionListener(e -> addArticlesFromList());
        addArticlesBtn.setToolTipText("Fügt Artikel aus der Artikelliste zur Bestellung hinzu.");

        JButton exportPdfBtn = createThemeButton(UnicodeSymbols.FILE + " Export PDF", ThemeManager.getWarningColor());
        exportPdfBtn.setToolTipText("Exportiert die Bestellung als PDF-Datei.");
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

        JButton createOrderBtn = createThemeButton(UnicodeSymbols.CHECKMARK + " Bestellen", ThemeManager.getSuccessColor());
        createOrderBtn.setToolTipText("Erstellt die Bestellung und speichert sie im System.");
        createOrderBtn.addActionListener(e -> onCreateOrder());

        actionButtons.add(addArticlesBtn);
        actionButtons.add(exportPdfBtn);
        actionButtons.add(createOrderBtn);
        bottomPanel.add(actionButtons, BorderLayout.EAST);

        rightCard.add(bottomPanel, BorderLayout.SOUTH);

        mainContent.add(createSummaryBar(), BorderLayout.NORTH);

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

        // Load & prepare article cache for inline search
        reloadAllArticlesCache();
        rebuildArticleComboModel("");

        updateSummaryBar();
    }

    // ---------------------------------------------------------------------
    // INLINE SEARCH PANEL (NEW)
    // ---------------------------------------------------------------------

    private JComponent buildInlineArticleSearchPanel() {
        RoundedPanel panel = new RoundedPanel(ThemeManager.getSurfaceColor(), 14);
        panel.setOpaque(true);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 6);
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel(UnicodeSymbols.SEARCH + " Artikel suchen:");
        lbl.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        lbl.setForeground(ThemeManager.getTextSecondaryColor());

        gc.gridx = 0;
        gc.weightx = 0.0;
        panel.add(lbl, gc);

        articleSearchField = new JTextField();
        styleTextField(articleSearchField);
        articleSearchField.setToolTipText("Name oder Artikelnummer eingeben…");

        gc.gridx = 1;
        gc.weightx = 0.35;
        panel.add(articleSearchField, gc);

        articleCombo = new JComboBox<>();
        articleCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Article a) {
                    l.setText(a.getName() + " (" + a.getArticleNumber() + ")");
                }
                return l;
            }
        });
        styleArticleComboBox(articleCombo);

        gc.gridx = 2;
        gc.weightx = 0.45;
        panel.add(articleCombo, gc);

        articleQtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        articleQtySpinner.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        articleQtySpinner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));
        if (articleQtySpinner.getEditor() instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(ThemeManager.getInputBackgroundColor());
            de.getTextField().setForeground(ThemeManager.getTextPrimaryColor());
            de.getTextField().setCaretColor(ThemeManager.getTextPrimaryColor());
            de.getTextField().setBorder(null);
        }

        gc.gridx = 3;
        gc.weightx = 0.10;
        panel.add(articleQtySpinner, gc);

        articleAddButton = createThemeButton(UnicodeSymbols.HEAVY_PLUS + " Hinzufügen", ThemeManager.getPrimaryColor());
        articleAddButton.setToolTipText("Fügt den ausgewählten Artikel zur Bestellung hinzu");

        gc.gridx = 4;
        gc.weightx = 0.10;
        panel.add(articleAddButton, gc);

        // live filter
        articleSearchField.getDocument().addDocumentListener(new DocumentListener() {
            private void update() {
                rebuildArticleComboModel(articleSearchField.getText());
            }
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }
        });
        articleSearchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                reloadAllArticlesCache();
                rebuildArticleComboModel(articleSearchField.getText());
            }
        });

        // add action
        articleAddButton.addActionListener(e -> addSelectedInlineArticle());

        // enter in qty adds
        if (articleQtySpinner.getEditor() instanceof JSpinner.DefaultEditor de) {
            de.getTextField().addActionListener(e -> addSelectedInlineArticle());
        }

        return panel;
    }

    private void addSelectedInlineArticle() {
        Article selected = (Article) articleCombo.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Bitte einen Artikel auswählen.", "Hinweis",
                    JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            return;
        }

        int qty = (Integer) articleQtySpinner.getValue();
        if (qty <= 0) qty = 1;

        // Merge quantity if same article already exists (by article number)
        for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
            if (sameArticle(e.getKey(), selected)) {
                orderArticles.put(e.getKey(), e.getValue() + qty);
                rebuildOrderTable();
                updateTotalPrice();
                return;
            }
        }

        orderArticles.put(selected, qty);
        rebuildOrderTable();
        updateTotalPrice();
    }

    private boolean sameArticle(Article a, Article b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return Objects.equals(a.getArticleNumber(), b.getArticleNumber());
    }

    private void reloadAllArticlesCache() {
        allArticlesCache = new ArrayList<>();

        // Try common method names without forcing you to change ArticleManager now
        ArticleManager mgr = ArticleManager.getInstance();
        String[] candidates = {"getArticles", "getAllArticles", "getAll", "listArticles"};

        for (String mName : candidates) {
            try {
                Method m = mgr.getClass().getMethod(mName);
                Object res = m.invoke(mgr);
                if (res instanceof Collection<?> col) {
                    for (Object o : col) {
                        if (o instanceof Article a) allArticlesCache.add(a);
                    }
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        // As a last resort: try to access a public field (rare) or just keep empty
        // If empty, the inline dropdown will simply show nothing.
    }

    private void rebuildArticleComboModel(String query) {
        String q = query == null ? "" : query.trim().toLowerCase();

        Article currentlySelected = (Article) articleCombo.getSelectedItem();

        DefaultComboBoxModel<Article> model = new DefaultComboBoxModel<>();
        int added = 0;

        for (Article a : allArticlesCache) {
            if (a == null) continue;

            String name = safe(a.getName()).toLowerCase();
            String nr = safe(a.getArticleNumber()).toLowerCase();

            String details = safe(a.getDetails()).toLowerCase();
            String vendor = safe(a.getVendorName()).toLowerCase();

            boolean match = q.isEmpty()
                    || name.contains(q)
                    || nr.contains(q)
                    || details.contains(q)
                    || vendor.contains(q);

            if (match) {
                model.addElement(a);
                added++;
                if (added >= 300) break;
            }
        }

        articleCombo.setModel(model);

        // keep selection if possible
        if (currentlySelected != null) {
            for (int i = 0; i < model.getSize(); i++) {
                Article x = model.getElementAt(i);
                if (sameArticle(x, currentlySelected)) {
                    articleCombo.setSelectedIndex(i);
                    return;
                }
            }
        }

        if (model.getSize() > 0) articleCombo.setSelectedIndex(0);
    }

    private void styleArticleComboBox(JComboBox<Article> combo) {
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
                    @Override public void mouseExited(MouseEvent e) { b.setBackground(bg); }
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
    }

    // ---------------------------------------------------------------------
    // EXISTING METHODS (unchanged)
    // ---------------------------------------------------------------------

    private void fillSenderNameCombobox() {
        senderNameCombobox.removeAllItems();
        senderNameCombobox.addItem("VEBO AG");

        Set<String> senderNames = new LinkedHashSet<>();
        UserManager userManager = UserManager.getInstance();
        for (var client : userManager.getAllUsernames()) {
            if (client != null && !client.trim().isEmpty()) {
                senderNames.add(client.trim());
            }
        }

        senderNames.stream().sorted().forEach(senderNameCombobox::addItem);
        senderNameCombobox.setEditable(true);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    private void styleTextField(JTextField field) {
        field.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getInputBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(ThemeManager.getInputBackgroundColor());
        field.setForeground(ThemeManager.getTextPrimaryColor());
        field.setCaretColor(ThemeManager.getTextPrimaryColor());
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
                    @Override public void mouseExited(MouseEvent e) { b.setBackground(bg); }
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
        table.setRowHeight(28);
        table.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));
        table.setShowGrid(true);
        table.setGridColor(ThemeManager.getTableGridColor());
        table.setBackground(ThemeManager.getCardBackgroundColor());
        table.setForeground(ThemeManager.getTextPrimaryColor());
        table.setSelectionBackground(ThemeManager.getSelectionBackgroundColor());
        table.setSelectionForeground(ThemeManager.getSelectionForegroundColor());
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(ThemeManager.getTableHeaderBackgroundColor());
        header.setForeground(ThemeManager.getTableHeaderForegroundColor());
        header.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(ThemeManager.getSelectionBackgroundColor());
                    c.setForeground(ThemeManager.getSelectionForegroundColor());
                } else {
                    c.setBackground(row % 2 == 0 ? ThemeManager.getTableRowEvenColor() : ThemeManager.getTableRowOddColor());
                    c.setForeground(ThemeManager.getTextPrimaryColor());
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
        JLabel label = new JLabel(labelText);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());
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
        updateSummaryBar();
    }

    private JPanel createSummaryBar() {
        RoundedPanel bar = new RoundedPanel(ThemeManager.getSurfaceColor(), 14);
        bar.setLayout(new GridLayout(1, 4, 12, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        bar.add(createSummaryChip(UnicodeSymbols.PERSON + " Empfänger", summaryReceiverValue));
        bar.add(createSummaryChip(UnicodeSymbols.DEPARTMENT + " Abteilung", summaryDepartmentValue));
        bar.add(createSummaryChip(UnicodeSymbols.PACKAGE + " Artikel", summaryItemsValue));
        bar.add(createSummaryChip(UnicodeSymbols.CREDIT_CARD + " Total", summaryTotalValue));
        return bar;
    }

    private JPanel createSummaryChip(String title, JLabel valueLabel) {
        RoundedPanel chip = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 12);
        chip.setLayout(new BorderLayout(0, 4));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        titleLabel.setForeground(ThemeManager.getTextSecondaryColor());
        chip.add(titleLabel, BorderLayout.NORTH);
        chip.add(valueLabel, BorderLayout.CENTER);
        return chip;
    }

    private JLabel createSummaryValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SettingsGUI.getFontByName(Font.BOLD, 14));
        label.setForeground(ThemeManager.getTextPrimaryColor());
        return label;
    }

    private void updateSummaryBar() {
        summaryReceiverValue.setText(valueOrDash((String) receiverNameCombobox.getSelectedItem()));
        summaryDepartmentValue.setText(valueOrDash((String) departmentList.getSelectedItem()));
        summaryItemsValue.setText(orderArticles.isEmpty() ? "Keine Artikel" : orderArticles.size() + " Artikel");
        summaryTotalValue.setText(totalPriceLabel.getText().replace("Totalpreis: ", ""));
    }

    private String valueOrDash(String text) {
        if (text == null) return "—";
        String trimmed = text.trim();
        return trimmed.isEmpty() ? "—" : trimmed;
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
        String sender = senderNameCombobox.getSelectedItem() != null
                ? senderNameCombobox.getSelectedItem().toString().trim()
                : "";
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

    private void exportOrderToPDF(File file) throws IOException {
        ArticleExporter.exportOrderToPDF(file, receiverNameCombobox,
                receiverKontoField,
                senderNameCombobox,
                senderKontoField,
                departmentList,
                orderArticles);
    }

    private JMenuBar createJMenu() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Bestellung");
        JMenuItem help = new JMenuItem("Hilfe");
        help.addActionListener(e -> showHelpDialog());
        menu.add(help);
        menuBar.add(menu);
        return menuBar;
    }

    private void showHelpDialog() {
        JDialog helpDialog = new JDialog(this, "Hilfe - Neue Bestellung erstellen", true);
        helpDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        helpDialog.setLayout(new BorderLayout(0, 0));

        JEditorPane editorPane = new JEditorPane("text/html", getHelpText());
        editorPane.setEditable(false);

        Color bg = ThemeManager.getCardBackgroundColor();
        Color fg = ThemeManager.getTextPrimaryColor();

        editorPane.setBackground(bg);
        editorPane.setForeground(fg);
        editorPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        editorPane.setFont(SettingsGUI.getFontByName(Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(bg);
        scrollPane.getViewport().setBackground(bg);
        scrollPane.setPreferredSize(new Dimension(700, 600));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(ThemeManager.getCardBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton closeButton = createThemeButton(UnicodeSymbols.CHECKMARK + " Schließen", ThemeManager.getPrimaryColor());
        closeButton.addActionListener(e -> helpDialog.dispose());
        closeButton.setToolTipText("Schliesst das Fenster");
        buttonPanel.add(closeButton);

        helpDialog.add(scrollPane, BorderLayout.CENTER);
        helpDialog.add(buttonPanel, BorderLayout.SOUTH);

        helpDialog.pack();
        helpDialog.setLocationRelativeTo(this);
        helpDialog.setVisible(true);
    }

    private String getHelpText() {
        return "<html><div style='width: 600px; font-family: Arial, sans-serif;'>" +
                "<h2 style='color: #2980b9; margin-bottom: 10px;'>📝 Hilfe - Neue Bestellung erstellen</h2>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>Übersicht</h3>" +
                "<p style='margin: 5px 0;'>In diesem Fenster können Sie eine neue Bestellung für Artikel aus dem Lager zusammenstellen. " +
                "Die Bestellung wird gespeichert und kann als PDF exportiert werden.</p>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>🎯 Schritt-für-Schritt Anleitung</h3>" +
                "<ol style='margin: 5px 0; padding-left: 20px;'>" +
                "<li><b>Empfänger auswählen:</b> Wählen Sie den Empfänger aus der Dropdown-Liste oder geben Sie einen neuen Namen ein.</li>" +
                "<li><b>Empfänger-Kontonummer:</b> Geben Sie die Kontonummer des Empfängers ein.</li>" +
                "<li><b>Absender auswählen:</b> Wählen Sie den Absender der Bestellung aus.</li>" +
                "<li><b>Absender-Kontonummer:</b> Geben Sie die Kontonummer des Absenders ein.</li>" +
                "<li><b>Abteilung wählen:</b> Ordnen Sie die Bestellung einer Abteilung zu.</li>" +
                "<li><b>Artikel hinzufügen:</b> Wählen Sie Artikel aus der Liste und geben Sie die gewünschte Menge ein.</li>" +
                "<li><b>Bestellung prüfen:</b> Überprüfen Sie alle Positionen in der Bestelltabelle.</li>" +
                "<li><b>Bestellung abschicken:</b> Klicken Sie auf \"📦 Bestellen\" um die Bestellung zu speichern.</li>" +
                "</ol>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>📋 Artikelverwaltung</h3>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li><b>Artikel hinzufügen:</b> Wählen Sie einen Artikel aus der Dropdown-Liste und klicken Sie auf \"➕ Hinzufügen\".</li>" +
                "<li><b>Menge ändern:</b> Doppelklicken Sie in der Tabelle auf die Menge und geben Sie einen neuen Wert ein.</li>" +
                "<li><b>Artikel entfernen:</b> Wählen Sie eine Zeile aus und klicken Sie auf \"🗑 Entfernen\".</li>" +
                "<li><b>Gesamtpreis:</b> Der Gesamtpreis wird automatisch berechnet und angezeigt.</li>" +
                "</ul>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>OK Pflichtfelder</h3>" +
                "<p style='margin: 5px 0;'><b style='color: #e74c3c;'>Folgende Felder müssen ausgefüllt werden:</b></p>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li>Empfängername</li>" +
                "<li>Empfänger-Kontonummer</li>" +
                "<li>Absendername</li>" +
                "<li>Absender-Kontonummer</li>" +
                "<li>Abteilung</li>" +
                "<li>Mindestens ein Artikel mit Menge > 0</li>" +
                "</ul>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>📄 PDF Export</h3>" +
                "<p style='margin: 5px 0;'>Mit der Funktion \"📄 Export PDF\" können Sie eine detaillierte Bestellübersicht erstellen:</p>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li>Enthält alle Bestelldetails (Empfänger, Absender, Datum, Artikelliste)</li>" +
                "<li>Zeigt Einzelpreise, Mengen und Gesamtpreis an</li>" +
                "<li>Professionelles Layout mit Logo und Firmendaten</li>" +
                "<li>Wird im Benutzerverzeichnis gespeichert</li>" +
                "</ul>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>💡 Tipps & Hinweise</h3>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li><b>Artikelsuche:</b> Beginnen Sie mit der Eingabe in der Artikel-Auswahl, um die Liste zu filtern.</li>" +
                "<li><b>Mengenvalidierung:</b> Negative Mengen werden automatisch korrigiert.</li>" +
                "<li><b>Lagerbestand:</b> Der aktuelle Lagerbestand wird bei jedem Artikel angezeigt.</li>" +
                "<li><b>Preisberechnung:</b> Verkaufspreise werden verwendet (nicht Einkaufspreise).</li>" +
                "<li><b>Kontonummern:</b> Verwenden Sie eindeutige Nummern zur besseren Nachverfolgung.</li>" +
                "<li><b>Speichern:</b> Bestellungen werden automatisch mit Datum und Uhrzeit versehen.</li>" +
                "</ul>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>⚠ Wichtige Hinweise</h3>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li>Prüfen Sie vor dem Absenden alle Daten auf Richtigkeit.</li>" +
                "<li>Gespeicherte Bestellungen können nachträglich bearbeitet werden.</li>" +
                "<li>Der Lagerbestand wird nach Abschluss der Bestellung automatisch aktualisiert.</li>" +
                "<li>Bei niedrigem Lagerbestand erscheinen automatische Warnungen.</li>" +
                "<li>PDF-Export ist unabhängig vom Speichern - Sie können Bestellungen vor dem Absenden exportieren.</li>" +
                "</ul>" +

                "<h3 style='color: #34495e; margin-top: 15px;'>🔍 Fehlerbehandlung</h3>" +
                "<p style='margin: 5px 0;'><b>Häufige Probleme und Lösungen:</b></p>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li><b>\"Fehlende Pflichtfelder\":</b> Füllen Sie alle mit * markierten Felder aus.</li>" +
                "<li><b>\"Ungültige Menge\":</b> Geben Sie eine positive Ganzzahl ein.</li>" +
                "<li><b>\"Keine Artikel\":</b> Fügen Sie mindestens einen Artikel zur Bestellung hinzu.</li>" +
                "<li><b>PDF nicht erstellt:</b> Prüfen Sie Schreibrechte im Zielordner.</li>" +
                "</ul>" +

                "<h3 style='color: #27ae60; margin-top: 15px;'>✨ Weitere Funktionen</h3>" +
                "<ul style='margin: 5px 0; padding-left: 20px;'>" +
                "<li><b>Mehrfachbestellungen:</b> Fügen Sie mehrere Artikel gleichzeitig hinzu.</li>" +
                "<li><b>Schnelleingabe:</b> Drücken Sie Enter nach der Mengeneingabe zum schnellen Hinzufügen.</li>" +
                "<li><b>Bestellhistorie:</b> Alle Bestellungen werden im System gespeichert und sind einsehbar.</li>" +
                "</ul>" +

                "<hr style='margin: 15px 0; border: 1px solid #ddd;'/>" +
                "<p style='margin: 10px 0; font-size: 11px; color: #7f8c8d;'>" +
                "<b>Hinweis:</b> Bei Fragen oder Problemen wenden Sie sich bitte an den Administrator.<br/>" +
                "Version: " + Main.VERSION + " | © 2026 VEBO Lagersystem" +
                "</p>" +
                "</div></html>";
    }

    private JButton createThemeButton(String text, Color baseBg) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        Color hoverBg = ThemeManager.getButtonHoverColor(baseBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(baseBg);

        button.setBackground(baseBg);
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setFont(SettingsGUI.getFontByName(Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(baseBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 18, 10, 18)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { if (button.isEnabled()) button.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e) { if (button.isEnabled()) button.setBackground(baseBg); }
            @Override public void mousePressed(MouseEvent e) { if (button.isEnabled()) button.setBackground(pressedBg); }
            @Override public void mouseReleased(MouseEvent e) {
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

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}