package ch.framedev.lagersystem.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Rounded button that respects its actual size and follows the ThemeManager.
 */
public class RoundButton extends JButton {

    private Shape shape;
    private final int arc;

    public RoundButton(String label) {
        this(label, 18);
    }

    public RoundButton(String label, int arc) {
        super(label);
        this.arc = arc;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setOpaque(false);

        setBackground(ThemeManager.getAccentColor());
        setForeground(ThemeManager.getTextOnPrimaryColor());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bg = getBackground();
        if (!isEnabled()) {
            bg = ThemeManager.getDisabledBackgroundColor();
        } else if (getModel().isPressed()) {
            bg = ThemeManager.getButtonPressedColor(bg);
        } else if (getModel().isRollover()) {
            bg = ThemeManager.getButtonHoverColor(bg);
        }

        int w = getWidth();
        int h = getHeight();
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        ThemeManager tm = ThemeManager.getInstance();
        Color border = ThemeManager.getBorderColor();
        if (isEnabled()) {
            border = getBackground().darker();
        }

        g2.setColor(border);
        g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        g2.dispose();
    }

    @Override
    public boolean contains(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        if (shape == null || !shape.getBounds().equals(getBounds())) {
            shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);
        }
        return shape.contains(x, y);
    }

}