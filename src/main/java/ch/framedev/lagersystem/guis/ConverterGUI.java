package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.JFrameUtils;
import ch.framedev.lagersystem.utils.SettingsUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
        setSize(980, 620);
        setMinimumSize(new Dimension(920, 540));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(ThemeManager.getBackgroundColor());
        topPanel.add(buildHeaderCard());
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(buildToolbarCard());
        root.add(topPanel, BorderLayout.NORTH);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(ThemeManager.getBackgroundColor());
        GridBagConstraints centerConstraints = new GridBagConstraints();
        centerConstraints.gridx = 0;
        centerConstraints.gridy = 0;
        centerConstraints.weightx = 1.0;
        centerConstraints.weighty = 1.0;
        centerConstraints.fill = GridBagConstraints.BOTH;
        centerWrapper.add(buildContentCard(), centerConstraints);
        root.add(centerWrapper, BorderLayout.CENTER);

        JPanel bottomWrapper = new JPanel(new BorderLayout());
        bottomWrapper.setBackground(ThemeManager.getBackgroundColor());
        bottomWrapper.setBorder(new EmptyBorder(10, 0, 0, 0));
        bottomWrapper.add(buildBottomCard(), BorderLayout.CENTER);
        root.add(bottomWrapper, BorderLayout.SOUTH);

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

    private JComponent buildHeaderCard() {
        JFrameUtils.RoundedPanel headerCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 20);
        headerCard.setLayout(new BorderLayout());
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Befüllungsrechner");
        title.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        title.setForeground(ThemeManager.getTextPrimaryColor());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Preis pro Abfüllmenge berechnen und zur Bestellung übernehmen");
        subtitle.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        subtitle.setForeground(ThemeManager.getTextSecondaryColor());
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subtitle);

        headerCard.add(textPanel, BorderLayout.CENTER);
        headerCard.add(buildArticleBadge(), BorderLayout.EAST);
        return headerCard;
    }

    private JPanel buildArticleBadge() {
        String badgeName  = (article != null && article.getName() != null) ? article.getName() : "Kein Artikel";
        String badgePrice = (article != null) ? formatCHF(article.getSellPrice()) : "—";

        JFrameUtils.RoundedPanel badge = new JFrameUtils.RoundedPanel(ThemeManager.getSurfaceColor(), 14);
        badge.setLayout(new BoxLayout(badge, BoxLayout.Y_AXIS));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                new EmptyBorder(10, 16, 10, 16)));

        JLabel nameLabel = new JLabel(badgeName);
        nameLabel.setFont(SettingsUtils.getFontByName(Font.BOLD, 13));
        nameLabel.setForeground(ThemeManager.getTextPrimaryColor());
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel(badgePrice);
        priceLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 16));
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

    private JComponent buildToolbarCard() {
        JFrameUtils.RoundedPanel toolbarCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        toolbarCard.setLayout(new BorderLayout(12, 0));
        toolbarCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(buildFormPanel(), BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton reset = createActionButton("Zurücksetzen", ThemeManager.getPrimaryColor());
        calcButton = createActionButton("Berechnen", ThemeManager.getAccentColor());
        reset.addActionListener(e -> resetCalculator());
        calcButton.addActionListener(e -> calculate());

        right.add(reset);
        right.add(calcButton);

        toolbarCard.add(left, BorderLayout.CENTER);
        toolbarCard.add(right, BorderLayout.EAST);
        return toolbarCard;
    }

    private JComponent buildContentCard() {
        JFrameUtils.RoundedPanel contentCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        contentCard.setLayout(new BorderLayout(0, 12));
        contentCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(2, 2, 12, 2);
        content.add(buildHintRow(), gc);

        gc.gridy++;
        gc.insets = new Insets(0, 0, 14, 0);
        content.add(buildResultPanel(), gc);

        gc.gridy++;
        gc.weighty = 1;
        content.add(Box.createVerticalGlue(), gc);

        contentCard.add(content, BorderLayout.CENTER);
        return contentCard;
    }

    private JComponent buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridy = 0;
        fc.insets = new Insets(0, 0, 0, 12);
        fc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        fc.gridx = 0;
        fc.weightx = 0;
        JLabel amountLabel = new JLabel("Abfüllmenge:");
        amountLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 12));
        amountLabel.setForeground(ThemeManager.getTextPrimaryColor());
        form.add(amountLabel, fc);

        // Field
        fc.gridx = 1;
        fc.weightx = 1;
        amountField = new JTextField();
        JFrameUtils.styleTextField(amountField);
        amountField.setFont(SettingsGUI.getFontByName(Font.PLAIN, 14));
        amountField.setPreferredSize(new Dimension(160, 38));
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
        resultPanel = new JFrameUtils.RoundedPanel(ThemeManager.getSurfaceColor(), 16) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color ac = ThemeManager.getAccentColor();
                g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), clamp(resultPanelTintAlpha)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(ThemeManager.getBorderColor());
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
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
        caption.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        caption.setForeground(ThemeManager.getTextSecondaryColor());
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultLabel = new JLabel("Preis: —");
        resultLabel.setFont(SettingsGUI.getFontByName(Font.BOLD, 22));
        resultLabel.setForeground(ThemeManager.getTextPrimaryColor());
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        resultMetaLabel = new JLabel("Menge: —");
        resultMetaLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
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

    private JComponent buildBottomCard() {
        JFrameUtils.RoundedPanel bottomCard = new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 18);
        bottomCard.setLayout(new BorderLayout(10, 0));
        bottomCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel tip = new JLabel("Tipp: Menge eingeben, berechnen und danach zur Bestellung übernehmen");
        tip.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
        tip.setForeground(ThemeManager.getTextSecondaryColor());
        bottomCard.add(tip, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);

        JButton close = createActionButton("Schließen", ThemeManager.getPrimaryColor());
        addToOrder = createActionButton(
                fillingConsumer == null ? "Zur Bestellung Hinzufügen" : "Füllung Übernehmen",
                fillingConsumer == null ? ThemeManager.getSuccessColor() : ThemeManager.getAccentColor());

        addToOrder.setEnabled(false);
        addToOrder.addActionListener(e -> addToOrder());
        close.addActionListener(e -> dispose());

        actions.add(addToOrder);
        actions.add(close);
        bottomCard.add(actions, BorderLayout.EAST);
        return bottomCard;
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

    private JButton createActionButton(String text, Color color) {
        if (text == null)
            throw new NullPointerException("text must not be null");
        JButton b = JFrameUtils.createThemeButton(text, color);
        b.setPreferredSize(new Dimension(176, 40));
        return b;
    }

    private void resetCalculator() {
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
        JFrameUtils.styleComboBox(box);
        box.setFont(SettingsUtils.getFontByName(Font.PLAIN, 15));
        box.setPreferredSize(new Dimension(100, 40));
        box.setFocusable(false);
        return box;
    }

    // ---------- TEXT FIELD HOVER/FOCUS -------------------------------------

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
