package ch.framedev.lagersystem.guis;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

@SuppressWarnings("unused")
public class SplashscreenGUI extends JFrame {

    private static final Color ACCENT_BLUE = new Color(30, 136, 229);
    private static final Color BACKGROUND_TOP = new Color(210, 232, 255);
    private static final Color BACKGROUND_MID = new Color(232, 242, 255);
    private static final Color BACKGROUND_BOTTOM = new Color(248, 250, 255);
    private static final Color GLOW_BLUE = new Color(60, 155, 245);

    private final AnimatedProgressBar progressBar;
    private final JLabel statusLabel;
    private final AnimatedLogoLabel logoLabel;
    private Timer animationTimer;
    private double phase = 0.0;
    private final Particle[] particles = new Particle[32];

    public SplashscreenGUI() {
        setTitle("Lagersystem - Splashscreen");
        setSize(760, 440);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);

        GradientPanel root = new GradientPanel();
        root.setLayout(new GridBagLayout());

        GlassPanel card = new GlassPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(36, 52, 40, 52));

        logoLabel = new AnimatedLogoLabel(loadLogo());
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logoLabel);

        card.add(Box.createVerticalStrut(18));

        JLabel title = new JLabel("Vebo Lager System");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(new Font("SansSerif", Font.BOLD, 48));
        title.setForeground(ACCENT_BLUE);
        card.add(title);

        JLabel subtitle = new JLabel("Inventory Management");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 19));
        subtitle.setForeground(new Color(80, 105, 130));
        card.add(subtitle);

        card.add(Box.createVerticalStrut(26));

        statusLabel = new JLabel("Initialisiere Programm...");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));
        statusLabel.setForeground(new Color(55, 75, 95));
        card.add(statusLabel);

        card.add(Box.createVerticalStrut(14));

        progressBar = new AnimatedProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT_BLUE);
        progressBar.setBackground(new Color(255, 255, 255, 200));
        progressBar.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        progressBar.setPreferredSize(new Dimension(420, 22));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(progressBar);

        root.add(card, new GridBagConstraints());
        setContentPane(root);
        initParticles();
        startAnimation(root, card);
    }

    public void updateProgress(int percent, String message) {
        progressBar.setValue(percent);
        if (message != null && !message.isBlank()) {
            statusLabel.setText(message);
        }
    }

    public void display() {
        setVisible(true);
    }

    public void close() {
        setVisible(false);
        stopAnimation();
        dispose();
    }

    private static Icon loadLogo() {
        URL logoUrl = SplashscreenGUI.class.getResource("/logo-small.png");
        if (logoUrl == null) {
            return new EmptyIcon(96, 96);
        }
        ImageIcon icon = new ImageIcon(logoUrl);
        Image scaled = icon.getImage().getScaledInstance(96, 96, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            GradientPaint gradientTop = new GradientPaint(
                0, 0, BACKGROUND_TOP,
                0, getHeight() * 0.6f, BACKGROUND_MID
            );
            GradientPaint gradientBottom = new GradientPaint(
                0, getHeight() * 0.4f, BACKGROUND_MID,
                0, getHeight(), BACKGROUND_BOTTOM
            );
            g2.setPaint(gradientTop);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setPaint(gradientBottom);
            g2.fillRect(0, 0, getWidth(), getHeight());

            double phase = SplashscreenGUI.this.getPhase();
            int drift = (int) (Math.sin(phase) * 6);
            int driftY = (int) (Math.cos(phase * 0.8) * 4);

            g2.setColor(new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 70));
            g2.fillOval(-140 + drift, -160 + driftY, 420, 420);
            g2.setColor(new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 45));
            g2.fillOval(getWidth() - 330 - drift, getHeight() - 290 + driftY, 460, 460);
            g2.setColor(new Color(255, 255, 255, 130));
            g2.fillOval(getWidth() - 190 - drift, driftY, 250, 250);
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillOval(30 + drift, getHeight() - 210 + driftY, 220, 220);

            SplashscreenGUI.this.paintLightStreaks(g2);
            SplashscreenGUI.this.paintParticles(g2);
            g2.dispose();
        }
    }

    private class GlassPanel extends JPanel {
        GlassPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = 26;
            int shadow = 8;
            int w = getWidth() - shadow * 2;
            int h = getHeight() - shadow * 2;

            g2.setColor(new Color(0, 0, 0, 38));
            g2.fillRoundRect(shadow, shadow, w, h, arc, arc);

            g2.setColor(new Color(255, 255, 255, 155));
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.drawRoundRect(0, 0, w, h, arc, arc);

            g2.setColor(new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 85));
            g2.drawRoundRect(2, 2, w - 4, h - 4, arc - 2, arc - 2);

            double phase = SplashscreenGUI.this.getPhase();
            float sweepX = (float) ((Math.sin(phase * 0.8) * 0.5 + 0.5) * w);
            GradientPaint sweep = new GradientPaint(
                sweepX - w * 0.6f, 0, new Color(255, 255, 255, 0),
                sweepX + w * 0.6f, h, new Color(255, 255, 255, 60)
            );
            g2.setPaint(sweep);
            g2.fillRoundRect(0, 0, w, h, arc, arc);

            GradientPaint innerGlow = new GradientPaint(
                0, 0, new Color(255, 255, 255, 90),
                0, h, new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 28)
            );
            g2.setPaint(innerGlow);
            g2.fillRoundRect(0, 0, w, h, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Insets getInsets() {
            return new Insets(8, 8, 8, 8);
        }
    }

    private record EmptyIcon(int width, int height) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    private void startAnimation(JComponent... components) {
        animationTimer = new Timer(15, event -> {
            phase += 0.035;
            advanceParticles();
            logoLabel.repaint();
            statusLabel.repaint();
            progressBar.repaint();
            for (JComponent component : components) {
                component.repaint();
            }
        });
        animationTimer.start();
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    private double getPhase() {
        return phase;
    }

    private void initParticles() {
        for (int i = 0; i < particles.length; i++) {
            particles[i] = Particle.random();
        }
    }

    private void advanceParticles() {
        for (Particle particle : particles) {
            particle.advance(getWidth(), getHeight());
        }
    }

    private void paintParticles(Graphics2D g2) {
        for (Particle particle : particles) {
            particle.paint(g2);
        }
    }

    private class AnimatedLogoLabel extends JLabel {
        AnimatedLogoLabel(Icon icon) {
            super(icon);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int bob = (int) (Math.sin(SplashscreenGUI.this.getPhase()) * 3);
            g2.translate(0, bob);
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    private class AnimatedProgressBar extends JProgressBar {
        AnimatedProgressBar(int min, int max) {
            super(min, max);
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Double percent = getPercentComplete();
            if (percent == null || percent <= 0.0) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Insets insets = getInsets();
            int w = getWidth() - insets.left - insets.right;
            int h = getHeight() - insets.top - insets.bottom;
            int fill = Math.max(1, (int) Math.round(w * percent));
            int x = insets.left;
            int y = insets.top;

            double phase = SplashscreenGUI.this.getPhase();
            float sweepX = (float) ((Math.sin(phase * 0.9) * 0.5 + 0.5) * fill);
            GradientPaint sweep = new GradientPaint(
                x + sweepX - fill * 0.5f, y, new Color(255, 255, 255, 0),
                x + sweepX + fill * 0.5f, y + h, new Color(255, 255, 255, 120)
            );
            Shape oldClip = g2.getClip();
            g2.setClip(x, y, fill, h);
            g2.setPaint(sweep);
            g2.fillRect(x, y, fill, h);
            g2.setClip(oldClip);
            g2.dispose();
        }
    }

    private static class Particle {
        private double x;
        private double y;
        private final double radius;
        private final double speed;
        private final double drift;
        private final int alpha;
        private final Color color;

        private Particle(double x, double y, double radius, double speed, double drift, int alpha, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
            this.drift = drift;
            this.alpha = alpha;
            this.color = color;
        }

        static Particle random() {
            double radius = 6 + Math.random() * 16;
            double speed = 0.12 + Math.random() * 0.35;
            double drift = (Math.random() * 2.0) - 1.0;
            int alpha = 30 + (int) (Math.random() * 70);
            int tint = 200 + (int) (Math.random() * 55);
            Color color = new Color(tint, tint, 255);
            return new Particle(
                Math.random() * 800,
                Math.random() * 500,
                radius,
                speed,
                drift,
                alpha,
                color
            );
        }

        void advance(int width, int height) {
            y -= speed;
            x += drift * 0.15;
            if (y + radius < 0) {
                y = height + radius;
                x = Math.random() * Math.max(width, 1);
            }
            if (x + radius < 0) {
                x = width + radius;
            }
            if (x - radius > width) {
                x = -radius;
            }
        }

        void paint(Graphics2D g2) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillOval((int) x, (int) y, (int) radius, (int) radius);
        }
    }

    private void paintLightStreaks(Graphics2D g2) {
        double phase = getPhase();
        int w = getWidth();
        int h = getHeight();
        float x1 = (float) ((Math.sin(phase * 0.6) * 0.5 + 0.5) * w);
        float x2 = (float) ((Math.cos(phase * 0.5) * 0.5 + 0.5) * w);

        GradientPaint streak1 = new GradientPaint(
            x1 - w * 0.3f, 0, new Color(255, 255, 255, 0),
            x1 + w * 0.3f, h, new Color(255, 255, 255, 80)
        );
        g2.setPaint(streak1);
        g2.fillRect(0, 0, w, h);

        GradientPaint streak2 = new GradientPaint(
            x2 - w * 0.35f, 0, new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 0),
            x2 + w * 0.35f, h, new Color(GLOW_BLUE.getRed(), GLOW_BLUE.getGreen(), GLOW_BLUE.getBlue(), 55)
        );
        g2.setPaint(streak2);
        g2.fillRect(0, 0, w, h);
    }
}
