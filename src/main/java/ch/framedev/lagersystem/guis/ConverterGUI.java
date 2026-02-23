package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.JFrameUtils;
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
import java.text.DecimalFormat;

/**
 * A GUI for calculating the price of filling an article with a certain amount. The user can enter the desired
 * amount in ml or liters, and the GUI will calculate the price based on the article's sell price and volume.
 * @author framedev
 */
public class ConverterGUI extends JFrame {

    private final Article article;

    private JTextField amountField;
    private JComboBox<String> unitBox;
    private JLabel resultLabel;

    /**
     * Create a new converter GUI for the given article. The article's name and sell price will be displayed,
     * @param article The article to calculate prices for. If null, the GUI will show placeholders and disable calculation.
     */
    public ConverterGUI(Article article) {
        this.article = article;

        setTitle("Befüllungsrechner");
        setSize(1020, 640);
        setMinimumSize(new Dimension(1020, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(ThemeManager.getBackgroundColor());
        root.setBorder(new EmptyBorder(28,28,28,28));

        JFrameUtils.RoundedPanel card =
                new JFrameUtils.RoundedPanel(ThemeManager.getCardBackgroundColor(), 4);
        card.setLayout(new BorderLayout(0,16));
        card.setBorder(new EmptyBorder(15,15,15,15));

        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildContent(), BorderLayout.CENTER);
        card.add(buildBottom(), BorderLayout.SOUTH);

        root.add(card);
        setContentPane(root);

        amountField.addActionListener(e -> calculate());
    }

    // ---------- HEADER ------------------------------------------------------

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Befüllungsrechner");
        title.setFont(SettingsUtils.getFontByName(Font.BOLD, 26));
        title.setForeground(ThemeManager.getTextPrimaryColor());

        JLabel subtitle = new JLabel("Preis pro Abfüllmenge berechnen");
        subtitle.setFont(SettingsUtils.getFontByName(Font.PLAIN, 14));
        subtitle.setForeground(ThemeManager.getTextSecondaryColor());

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(3));
        text.add(subtitle);

        header.add(text, BorderLayout.WEST);
        return header;
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
        gc.insets = new Insets(0,0,14,0);

        String name = (article != null && article.getName() != null) ? article.getName() : "—";
        double sell = (article != null) ? article.getSellPrice() : 0.0;

        JLabel articleLabel = new JLabel("Artikel: " + name);
        articleLabel.setFont(SettingsUtils.getFontByName(Font.BOLD, 17));
        articleLabel.setForeground(ThemeManager.getTextPrimaryColor());
        content.add(articleLabel, gc);

        gc.gridy++;
        JLabel priceLabel = new JLabel("Verkaufspreis: " + formatCHF(sell));
        priceLabel.setFont(SettingsUtils.getFontByName(Font.PLAIN, 14));
        priceLabel.setForeground(ThemeManager.getTextSecondaryColor());
        content.add(priceLabel, gc);

        gc.gridy++;
        content.add(buildFormPanel(), gc);

        gc.gridy++;
        gc.weighty = 1;
        content.add(Box.createVerticalGlue(), gc);

        return content;
    }

    private JComponent buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(),1),
                new EmptyBorder(16,16,16,16)
        ));

        GridBagConstraints fc = new GridBagConstraints();
        fc.gridy = 0;
        fc.insets = new Insets(0,0,12,12);
        fc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        fc.gridx = 0;
        JLabel amountLabel = new JLabel("Abfüllmenge:");
        amountLabel.setFont(SettingsUtils.getFontByName(Font.PLAIN,14));
        amountLabel.setForeground(ThemeManager.getTextPrimaryColor());
        form.add(amountLabel, fc);

        // Field
        fc.gridx = 1;
        fc.weightx = 1;
        amountField = new JTextField();
        amountField.setFont(SettingsUtils.getFontByName(Font.PLAIN,15));
        amountField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor()),
                new EmptyBorder(8,12,8,12)
        ));

        // Hover + focus effect for a text field
        installFieldHoverAndFocus(amountField);

        form.add(amountField, fc);

        // Unit
        fc.gridx = 2;
        fc.weightx = 0;
        fc.insets = new Insets(0,0,12,0);
        unitBox = createStyledComboBox(new String[]{"ml","l"});
        form.add(unitBox, fc);

        // Result label
        fc.gridy++;
        fc.gridx = 0;
        fc.gridwidth = 3;
        fc.insets = new Insets(6,0,0,0);

        resultLabel = new JLabel("Preis: —");
        resultLabel.setFont(SettingsUtils.getFontByName(Font.BOLD,18));
        resultLabel.setForeground(ThemeManager.getTextPrimaryColor());
        form.add(resultLabel, fc);

        return form;
    }

    // ---------- BOTTOM ------------------------------------------------------

    private JComponent buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);

        JLabel tip = new JLabel("Menge eingeben und Berechnen klicken (Enter funktioniert auch)");
        tip.setFont(SettingsUtils.getFontByName(Font.PLAIN,12));
        tip.setForeground(ThemeManager.getTextSecondaryColor());
        bottom.add(tip, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,14,0));
        actions.setOpaque(false);

        JButton reset = createBigButton("Zurücksetzen", false);
        JButton calc  = createBigButton("Berechnen", true);
        JButton close = createBigButton("Schließen", false);

        calc.addActionListener(e -> calculate());
        reset.addActionListener(e -> {
            amountField.setText("");
            resultLabel.setText("Preis: —");
        });
        close.addActionListener(e -> dispose());

        actions.add(reset);
        actions.add(calc);
        actions.add(close);

        bottom.add(actions, BorderLayout.EAST);
        return bottom;
    }

    // ---------- LOGIC -------------------------------------------------------

    private void calculate() {
        if (article == null) {
            JOptionPane.showMessageDialog(this, "Kein Artikel ausgewählt.", "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String txt = amountField.getText().trim().replace(",", ".");
        if (txt.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Bitte eine Abfüllmenge eingeben.", "Hinweis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double amount = Double.parseDouble(txt);
            if (amount <= 0) throw new NumberFormatException();

            boolean liters = "l".equals(unitBox.getSelectedItem());

            double price = ArticleUtils.calculatePriceForFilling(
                    article,
                    amount,
                    liters ? ArticleUtils.VolumeUnit.LITER : ArticleUtils.VolumeUnit.MILLILITER
            );

            resultLabel.setText("Preis: " + formatCHF(price));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ungültige Eingabe.", "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------- HELPERS -----------------------------------------------------

    private JButton createBigButton(String text, boolean primary){
        JButton b = new JButton(text);
        b.setFont(SettingsUtils.getFontByName(Font.BOLD,15));
        b.setPreferredSize(new Dimension(160,46));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor()));
        b.setBackground(primary ? ThemeManager.getPrimaryColor() : ThemeManager.getCardBackgroundColor());
        b.setForeground(ThemeManager.getTextPrimaryColor());
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect (simple brighten/darken without hardcoding colors)
        final Color normalBg = b.getBackground();
        final Color hoverBg = brighten(normalBg, 0.08f);

        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(hoverBg); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(normalBg); }
        });

        return b;
    }

    private static String formatCHF(double v){
        return new DecimalFormat("0.00").format(v)+" CHF";
    }

    // ---------- COMBOBOX (WITH HOVER) --------------------------------------

    private JComboBox<String> createStyledComboBox(String[] values) {
        JComboBox<String> box = new JComboBox<>(values);

        box.setFont(SettingsUtils.getFontByName(Font.PLAIN, 15));
        box.setForeground(ThemeManager.getTextPrimaryColor());
        box.setBackground(ThemeManager.getCardBackgroundColor());
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
                    label.setBackground(ThemeManager.getPrimaryColor());
                    label.setForeground(ThemeManager.getTextPrimaryColor());
                } else {
                    label.setBackground(ThemeManager.getCardBackgroundColor());
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
        box.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { box.setBorder(compoundFieldBorder(true)); }
            @Override public void mouseExited(MouseEvent e)  { box.setBorder(compoundFieldBorder(false)); }
        });
    }

    // ---------- TEXT FIELD HOVER/FOCUS -------------------------------------

    private void installFieldHoverAndFocus(JTextField field) {
        field.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!field.isFocusOwner()) field.setBorder(compoundFieldBorder(true));
            }
            @Override public void mouseExited(MouseEvent e) {
                if (!field.isFocusOwner()) field.setBorder(compoundFieldBorder(false));
            }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { field.setBorder(compoundFieldBorder(true)); }
            @Override public void focusLost(FocusEvent e)   { field.setBorder(compoundFieldBorder(false)); }
        });
    }

    /**
     * Create a compound border for fields that brightens on hover/focus
     */
    private Border compoundFieldBorder(boolean active) {
        Color line = active ? brighten(ThemeManager.getBorderColor(), 0.20f) : ThemeManager.getBorderColor();
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(line, 1),
                new EmptyBorder(8, 12, 8, 12)
        );
    }

    // ---------- COLOR MATH --------------------------------------------------

    /**
     * Brighten a color by blending it with white. Amount is between 0 (no change) and 1 (full white).
     * @param c The original color
     * @param amount How much to brighten (0-1)
     * @return A new brightened color
     */
    private static Color brighten(Color c, float amount) {
        amount = Math.max(0f, Math.min(1f, amount));
        int r = c.getRed() + Math.round((255 - c.getRed()) * amount);
        int g = c.getGreen() + Math.round((255 - c.getGreen()) * amount);
        int b = c.getBlue() + Math.round((255 - c.getBlue()) * amount);
        return new Color(clamp(r), clamp(g), clamp(b), c.getAlpha());
    }

    /**
     * Clamp a color component to the 0-255 range
     */
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }
}