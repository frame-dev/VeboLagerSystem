package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * A GUI for calculating the price of filling an article with a certain amount.
 * The user can enter the desired
 * amount in ml or liters, and the GUI will calculate the price based on the
 * article's sell price and volume.
 * 
 * @author framedev
 */
public class ConverterGUI extends JFrame {

    private final Article article;

    private JTextField amountField;
    private JComboBox<String> unitBox;
    private JLabel resultLabel;
    private JLabel resultMetaLabel;
    private JPanel resultPanel;
    private Timer resultPulseTimer;
    private int resultPanelTintAlpha = 14;
    private JButton calcButton;
    private JButton addToOrder;
    private final Consumer<String> fillingConsumer;

    /**
     * Create a new converter GUI for the given article. The article's name and sell
     * price will be displayed,
     * 
     * @param article The article to calculate prices for. If null, the GUI will
     *                show placeholders and disable calculation.
     */
    public ConverterGUI(Article article) {
        this(article, null);
    }

    public ConverterGUI(Article article, Consumer<String> fillingConsumer) {
        this.article = article;
        this.fillingConsumer = fillingConsumer;
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();

        setTitle("Befüllungsrechner");
        setSize(1020, 640);
        setMinimumSize(new Dimension(1020, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                Color bg = ThemeManager.getBackgroundColor();
                Color top = blendColor(bg, ThemeManager.getSurfaceColor(), 0.25f);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bg);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                Color ac = ThemeManager.getAccentColor();
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 26));
                g2.fillOval(-120, -90, 380, 260);
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 16));
                g2.fillOval(w - 280, h - 220, 360, 260);

                g2.dispose();
                super.paintComponent(g);
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(36, 36, 36, 36));

        ShadowRoundedPanel card = new ShadowRoundedPanel(ThemeManager.getCardBackgroundColor(), 22, 12);
        card.setLayout(new BorderLayout(0, 22));
        card.setBorder(new EmptyBorder(28, 32, 28, 32));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildContent(), BorderLayout.CENTER);
        card.add(buildBottom(), BorderLayout.SOUTH);

        root.add(card);
        setContentPane(root);

        // Make Enter trigger the primary action when possible
        if (getRootPane() != null) {
            getRootPane().setDefaultButton(calcButton);

            // ESC closes the window
            getRootPane().registerKeyboardAction(
                    e -> dispose(),
                    KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        // Disable interaction when no article is provided
        boolean enabled = (this.article != null && ArticleUtils.canCalculateFillingPrice(article));
        amountField.setEnabled(enabled);
        unitBox.setEnabled(enabled);
        if (calcButton != null)
            calcButton.setEnabled(enabled);
        if (addToOrder != null)
            addToOrder.setEnabled(enabled);

        amountField.addActionListener(e -> calculate());

        // Improve flow: start typing immediately without extra clicks.
        SwingUtilities.invokeLater(() -> {
            if (amountField != null && amountField.isEnabled()) {
                amountField.requestFocusInWindow();
                amountField.selectAll();
            }
        });
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    // ---------- HEADER ------------------------------------------------------

    private JComponent buildHeader() {
        JPanel outer = new JPanel(new BorderLayout(0, 10));
        outer.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(0, 0));
        header.setOpaque(false);

        // Left accent stripe
        JPanel stripe = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getAccentColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        stripe.setOpaque(false);
        stripe.setPreferredSize(new Dimension(4, 0));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setBorder(new EmptyBorder(0, 12, 0, 0));

        JLabel title = new JLabel("Befüllungsrechner");
        title.setFont(SettingsUtils.getFontByName(Font.BOLD, 26));
        title.setForeground(ThemeManager.getTextPrimaryColor());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Preis pro Abfüllmenge berechnen");
        subtitle.setFont(SettingsUtils.getFontByName(Font.PLAIN, 14));
        subtitle.setForeground(ThemeManager.getTextSecondaryColor());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(subtitle);

        header.add(stripe, BorderLayout.WEST);
        header.add(textPanel, BorderLayout.CENTER);
        header.add(buildArticleBadge(), BorderLayout.EAST);

        JSeparator divider = new JSeparator();
        divider.setForeground(ThemeManager.getDividerColor());

        outer.add(header, BorderLayout.CENTER);
        outer.add(divider, BorderLayout.SOUTH);
        return outer;
    }

    private JPanel buildArticleBadge() {
        String badgeName  = (article != null && article.getName() != null) ? article.getName() : "Kein Artikel";
        String badgePrice = (article != null) ? formatCHF(article.getSellPrice()) : "—";

        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ac = ThemeManager.getAccentColor();
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 70));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(new EmptyBorder(8, 16, 8, 16));

        JLabel nameLabel = new JLabel(badgeName);
        nameLabel.setFont(SettingsUtils.getFontByName(Font.BOLD, 13));
        nameLabel.setForeground(ThemeManager.getTextPrimaryColor());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel(badgePrice);
        priceLabel.setFont(SettingsUtils.getFontByName(Font.BOLD, 16));
        priceLabel.setForeground(article != null
            ? ensureContrast(ThemeManager.getAccentColor(), ThemeManager.getCardBackgroundColor(), 3.2f)
            : ThemeManager.getTextSecondaryColor());
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        badge.add(nameLabel);
        badge.add(Box.createVerticalStrut(3));
        badge.add(priceLabel);
        return badge;
    }

    // ---------- CONTENT -----------------------------------------------------

    private JComponent buildContent() {
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 14, 0);

        content.add(buildFormPanel(), gc);

        gc.gridy++;
        gc.insets = new Insets(2, 2, 12, 2);
        content.add(buildHintRow(), gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 14, 0);
        content.add(buildResultPanel(), gc);

        gc.gridy++;
        gc.weighty = 1;
        content.add(Box.createVerticalGlue(), gc);

        return content;
    }

    private JComponent buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ThemeManager.getSurfaceColor());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ThemeManager.getBorderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(18, 18, 18, 18));

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridy = 0;
        fc.insets = new Insets(0, 0, 0, 12);
        fc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        fc.gridx = 0;
        fc.weightx = 0;
        JLabel amountLabel = new JLabel("Abfüllmenge:");
        amountLabel.setFont(SettingsUtils.getFontByName(Font.PLAIN, 14));
        amountLabel.setForeground(ThemeManager.getTextPrimaryColor());
        form.add(amountLabel, fc);

        // Field
        fc.gridx = 1;
        fc.weightx = 1;
        amountField = new JTextField();
        amountField.setFont(SettingsUtils.getFontByName(Font.PLAIN, 15));
        amountField.setBackground(ThemeManager.getInputBackgroundColor());
        amountField.setForeground(ThemeManager.getTextPrimaryColor());
        amountField.setCaretColor(ThemeManager.getAccentColor());
        amountField.setBorder(compoundFieldBorder(false));
        installFieldHoverAndFocus(amountField);
        form.add(amountField, fc);

        // Unit
        fc.gridx = 2;
        fc.weightx = 0;
        fc.insets = new Insets(0, 0, 0, 0);
        unitBox = createStyledComboBox(new String[] { "ml", "l" });
        form.add(unitBox, fc);

        return form;
    }

    private JComponent buildHintRow() {
        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hint.setOpaque(false);

        JLabel dot = new JLabel("•");
        dot.setFont(SettingsUtils.getFontByName(Font.BOLD, 18));
        dot.setForeground(ThemeManager.getAccentColor());

        JLabel text = new JLabel("Beispiel: 250 ml oder 1.5 l");
        text.setFont(SettingsUtils.getFontByName(Font.PLAIN, 12));
        text.setForeground(ThemeManager.getTextSecondaryColor());

        hint.add(dot);
        hint.add(text);
        return hint;
    }

    private JComponent buildResultPanel() {
        resultPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ac = ThemeManager.getAccentColor();
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), clamp(resultPanelTintAlpha)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 55));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                // Left accent stripe
                g2.setColor(ac);
                g2.fillRoundRect(0, 0, 4, getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        resultPanel.setOpaque(false);
        resultPanel.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel caption = new JLabel("Berechneter Gesamtpreis");
        caption.setFont(SettingsUtils.getFontByName(Font.PLAIN, 12));
        caption.setForeground(ThemeManager.getTextSecondaryColor());
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultLabel = new JLabel("Preis: —");
        resultLabel.setFont(SettingsUtils.getFontByName(Font.BOLD, 22));
        resultLabel.setForeground(ThemeManager.getTextPrimaryColor());
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultMetaLabel = new JLabel("Menge: —");
        resultMetaLabel.setFont(SettingsUtils.getFontByName(Font.PLAIN, 12));
        resultMetaLabel.setForeground(ThemeManager.getTextSecondaryColor());
        resultMetaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(caption);
        text.add(Box.createVerticalStrut(2));
        text.add(resultLabel);
        text.add(Box.createVerticalStrut(2));
        text.add(resultMetaLabel);
        resultPanel.add(text, BorderLayout.CENTER);

        return resultPanel;
    }

    // ---------- BOTTOM ------------------------------------------------------

    private JComponent buildBottom() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);

        JSeparator sep = new JSeparator();
        sep.setForeground(ThemeManager.getDividerColor());
        wrapper.add(sep, BorderLayout.NORTH);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JLabel tip = new JLabel("Menge eingeben und Berechnen klicken (Enter funktioniert auch)");
        tip.setFont(SettingsUtils.getFontByName(Font.PLAIN, 12));
        tip.setForeground(ThemeManager.getTextSecondaryColor());
        bottom.add(tip, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        actions.setOpaque(false);

        JButton reset = createBigButton("Zurücksetzen", false);
        calcButton = createBigButton("Berechnen", true);
        JButton close = createBigButton("Schließen", false);

        addToOrder = createBigButton(fillingConsumer == null ? "Zur Bestellung Hinzufügen" : "Füllung Übernehmen", false);

        addToOrder.setEnabled(false);
        addToOrder.addActionListener(e -> addToOrder());

        calcButton.addActionListener(e -> calculate());
        reset.addActionListener(e -> {
            amountField.setText("");
            resultLabel.setText("Preis: —");
            resultLabel.setForeground(ThemeManager.getTextPrimaryColor());
            if (resultMetaLabel != null) {
                resultMetaLabel.setText("Menge: —");
            }
            stopResultPulse();
            resultPanelTintAlpha = 14;
            if (resultPanel != null) {
                resultPanel.repaint();
            }
        });
        close.addActionListener(e -> dispose());



        actions.add(reset);
        actions.add(calcButton);
        actions.add(addToOrder);
        actions.add(close);

        bottom.add(actions, BorderLayout.EAST);
        wrapper.add(bottom, BorderLayout.CENTER);
        return wrapper;
    }

    // ---------- LOGIC -------------------------------------------------------

    private void addToOrder() {
        if (article == null) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Kein Artikel ausgewählt.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        String amount = amountField.getText() == null ? "" : amountField.getText().trim();
        String unit = unitBox.getSelectedItem() == null ? "" : unitBox.getSelectedItem().toString().trim();
        String filling = ArticleUtils.normalizeFilling(amount, unit);

        if (!ArticleUtils.isFillingValid(filling) || filling.isBlank()) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Bitte zuerst eine gültige Befüllmenge eingeben.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        if (fillingConsumer != null) {
            fillingConsumer.accept(filling);
            dispose();
            return;
        }

        ArticleListGUI.addArticle(article, 1, null, filling);
        if (Main.articleListGUI != null) {
            Main.articleListGUI.refreshArticleList();
            Main.articleListGUI.display();
        }
    }

    private void calculate() {
        if (article == null) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Kein Artikel ausgewählt.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            return;
        }

        String txt = amountField.getText().trim().replace(",", ".");
        if (txt.isEmpty()) {
            new MessageDialog()
                    .setTitle("Hinweis")
                    .setMessage("Bitte eine Abfüllmenge eingeben.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        try {
            java.math.BigDecimal amountBd = new java.math.BigDecimal(txt);
            if (amountBd.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }

            double amount = amountBd.doubleValue();
            boolean liters = "l".equals(unitBox.getSelectedItem());

            double price = ArticleUtils.calculatePriceForFilling(
                    article,
                    amount,
                    liters ? ArticleUtils.VolumeUnit.LITER : ArticleUtils.VolumeUnit.MILLILITER);

            // Normalize to 2 decimals for display
            java.math.BigDecimal priceBd = java.math.BigDecimal.valueOf(price)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            resultLabel.setText("Preis: " + formatCHF(priceBd.doubleValue()));
            if (resultMetaLabel != null) {
                String unit = liters ? "l" : "ml";
                resultMetaLabel.setText("Menge: " + txt + " " + unit);
            }

            resultLabel.setForeground(ensureContrast(
                    ThemeManager.getAccentColor(),
                    ThemeManager.getCardBackgroundColor(),
                    3.2f));
                pulseResultPanel();

        } catch (NumberFormatException ex) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Ungültige Eingabe.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        } catch (Exception ex) {
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Berechnen.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    // ---------- HELPERS -----------------------------------------------------

    private JButton createBigButton(String text, boolean primary) {
        if (text == null)
            throw new NullPointerException("text must not be null");
        JButton b = new JButton(text);
        b.setFont(SettingsUtils.getFontByName(Font.BOLD, 15));
        b.setPreferredSize(new Dimension(148, 40));
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setContentAreaFilled(true);

        Color normalBg = primary ? ThemeManager.getAccentColor() : ThemeManager.getSurfaceColor();
        Color normalFg = primary
            ? ensureContrast(ThemeManager.getTextOnPrimaryColor(), normalBg, 4.5f)
            : ThemeManager.getTextPrimaryColor();
        Color borderNormal = primary ? ThemeManager.getAccentColor().darker() : ThemeManager.getBorderColor();
        Color hoverBg = ThemeManager.getButtonHoverColor(normalBg);
        Color pressedBg = ThemeManager.getButtonPressedColor(normalBg);
        Color borderHover = blendColor(borderNormal, Color.BLACK, 0.08f);

        b.setBackground(normalBg);
        b.setForeground(normalFg);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderNormal, 1, true),
                new EmptyBorder(10, 22, 10, 22)));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setBackground(hoverBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderHover, 1, true),
                        new EmptyBorder(10, 22, 10, 22)));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(normalBg);
                b.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(borderNormal, 1, true),
                        new EmptyBorder(10, 22, 10, 22)));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                b.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                b.setBackground(b.contains(e.getPoint()) ? hoverBg : normalBg);
            }
        });

        return b;
    }

    // Modern shadowed rounded panel for card look
    private static class ShadowRoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;
        private final int shadowSize;

        ShadowRoundedPanel(Color bg, int radius, int shadowSize) {
            this.backgroundColor = bg;
            this.radius = radius;
            this.shadowSize = shadowSize;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            // Draw soft shadow
            for (int i = shadowSize; i > 0; i--) {
                int alpha = (int) (18.0 * i / shadowSize);
                g2.setColor(new Color(0, 0, 0, alpha));
                g2.fillRoundRect(i, i, w - i * 2, h - i * 2, radius, radius);
            }
            // Draw subtle gradient background
            Color top = blendColor(backgroundColor, Color.WHITE, ThemeManager.isDarkMode() ? 0.08f : 0.14f);
            Color bottom = blendColor(backgroundColor, Color.BLACK, ThemeManager.isDarkMode() ? 0.12f : 0.06f);
            GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w - shadowSize, h - shadowSize, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static String formatCHF(double v) {
        java.text.DecimalFormatSymbols symbols = new java.text.DecimalFormatSymbols(java.util.Locale.ROOT);
        symbols.setDecimalSeparator('.');
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.00", symbols);
        return df.format(v) + " CHF";
    }

    // ---------- COMBOBOX (WITH HOVER) --------------------------------------

    private JComboBox<String> createStyledComboBox(String[] values) {
        if (values == null)
            throw new NullPointerException("values must not be null");
        JComboBox<String> box = new JComboBox<>(values);

        box.setFont(SettingsUtils.getFontByName(Font.PLAIN, 15));
        box.setForeground(ThemeManager.getTextPrimaryColor());
        box.setBackground(ThemeManager.getInputBackgroundColor());
        box.setBorder(compoundFieldBorder(false));
        box.setPreferredSize(new Dimension(100, 40));
        box.setFocusable(false);

        // Hover + focus-like border effect
        installComboHover(box);

        // Renderer for consistent dropdown look
        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setFont(SettingsUtils.getFontByName(Font.PLAIN, 15));
                label.setBorder(new EmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    label.setBackground(ThemeManager.getAccentColor());
                    label.setForeground(ensureContrast(
                            ThemeManager.getTextOnPrimaryColor(),
                            ThemeManager.getAccentColor(),
                            4.5f));
                } else {
                    label.setBackground(ThemeManager.getInputBackgroundColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                }
                return label;
            }
        });

        // Minimal arrow button
        box.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("▾");
                button.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setForeground(ThemeManager.getTextPrimaryColor());
                button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return button;
            }
        });

        return box;
    }

    private void installComboHover(JComboBox<?> box) {
        if (box == null)
            throw new NullPointerException("box must not be null");
        box.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                box.setBorder(compoundFieldBorder(true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                box.setBorder(compoundFieldBorder(false));
            }
        });
    }

    // ---------- TEXT FIELD HOVER/FOCUS -------------------------------------

    private void installFieldHoverAndFocus(JTextField field) {
        if (field == null)
            throw new NullPointerException("field must not be null");
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!field.isFocusOwner())
                    field.setBorder(compoundFieldBorder(true));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!field.isFocusOwner())
                    field.setBorder(compoundFieldBorder(false));
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(compoundFieldBorder(true));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(compoundFieldBorder(false));
            }
        });
    }

    /**
     * Create a compound border for fields that becomes stronger on hover/focus.
     */
    private Border compoundFieldBorder(boolean active) {
        Color line = active ? ThemeManager.getInputFocusBorderColor() : ThemeManager.getInputBorderColor();
        int width = active ? 2 : 1;
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line, width),
                new EmptyBorder(8, 12, 8, 12));
    }

    /**
     * Ensure text color has at least a minimal contrast against a background.
     */
    private static Color ensureContrast(Color preferred, Color background, float minRatio) {
        if (preferred == null) {
            throw new NullPointerException("preferred must not be null");
        }
        if (background == null) {
            throw new NullPointerException("background must not be null");
        }

        Color best = preferred;
        double ratio = contrastRatio(preferred, background);
        if (ratio >= minRatio) {
            return preferred;
        }

        Color fallback = getReadableTextColor(background);
        double fallbackRatio = contrastRatio(fallback, background);
        if (fallbackRatio > ratio) {
            best = fallback;
            ratio = fallbackRatio;
        }

        // Gradually blend toward a highly readable fallback to preserve hue when possible.
        for (int i = 1; i <= 8 && ratio < minRatio; i++) {
            float amount = i / 8f;
            Color candidate = blendColor(preferred, fallback, amount);
            double candidateRatio = contrastRatio(candidate, background);
            if (candidateRatio > ratio) {
                best = candidate;
                ratio = candidateRatio;
            }
        }
        return best;
    }

    private static Color getReadableTextColor(Color background) {
        return relativeLuminance(background) > 0.45 ? Color.BLACK : Color.WHITE;
    }

    private static double contrastRatio(Color c1, Color c2) {
        double l1 = relativeLuminance(c1);
        double l2 = relativeLuminance(c2);
        double lighter = Math.max(l1, l2);
        double darker = Math.min(l1, l2);
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double relativeLuminance(Color c) {
        double r = channelToLinear(c.getRed() / 255.0);
        double g = channelToLinear(c.getGreen() / 255.0);
        double b = channelToLinear(c.getBlue() / 255.0);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channelToLinear(double v) {
        return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
    }

    // ---------- COLOR MATH --------------------------------------------------

    /**
     * Blends a color toward a target (white for brightening, black for darkening).
     * 
     * @param c      source color
     * @param target Color.WHITE or Color.BLACK
     * @param amount blend fraction 0–1
     */
    private static Color blendColor(Color c, Color target, float amount) {
        if (c == null)
            throw new NullPointerException("c must not be null");
        amount = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(c.getRed() + (target.getRed() - c.getRed()) * amount);
        int g = Math.round(c.getGreen() + (target.getGreen() - c.getGreen()) * amount);
        int b = Math.round(c.getBlue() + (target.getBlue() - c.getBlue()) * amount);
        return new Color(clamp(r), clamp(g), clamp(b), c.getAlpha());
    }

    /**
     * Clamp a color component to the 0-255 range
     */
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private void pulseResultPanel() {
        stopResultPulse();
        final int base = 14;
        final int peak = 52;
        final int totalSteps = 12;
        final int[] step = {0};

        resultPulseTimer = new Timer(24, e -> {
            step[0]++;
            float phase = step[0] / (float) totalSteps;
            float wave = phase <= 0.5f
                    ? (phase / 0.5f)
                    : ((1f - phase) / 0.5f);

            resultPanelTintAlpha = Math.round(base + (peak - base) * Math.max(0f, wave));
            if (resultPanel != null) {
                resultPanel.repaint();
            }

            if (step[0] >= totalSteps) {
                stopResultPulse();
                resultPanelTintAlpha = base;
                if (resultPanel != null) {
                    resultPanel.repaint();
                }
            }
        });
        resultPulseTimer.setRepeats(true);
        resultPulseTimer.start();
    }

    private void stopResultPulse() {
        if (resultPulseTimer != null) {
            resultPulseTimer.stop();
            resultPulseTimer = null;
        }
    }
}
