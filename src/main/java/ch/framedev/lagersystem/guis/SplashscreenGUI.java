package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URL;

/**
 * Displays a modern splash screen for the LagerSystem application.
 *
 * <p>The UI uses a gradient background, subtle animated particles, and a glass-like card that contains
 * the application logo, title/subtitle, a status message, and an animated progress bar.
 * The layout is responsive and adapts its padding and typography when the window is resized.
 *
 * <p>Progress and status can be updated at runtime via {@link #updateProgress(int, String)}.
 * The animation is driven by a {@link Timer} that periodically repaints the relevant components.
 */
@SuppressWarnings("ALL")
public class SplashscreenGUI extends JFrame {

    public enum QualityPreset {
        HIGH(18, 2, 1),
        BALANCED(14, 3, 1),
        LOW(10, 4, 2);

        private final int particleCount;
        private final int baseHeavyRepaintInterval;
        private final int baseParticleUpdateInterval;

        QualityPreset(int particleCount, int baseHeavyRepaintInterval, int baseParticleUpdateInterval) {
            this.particleCount = particleCount;
            this.baseHeavyRepaintInterval = baseHeavyRepaintInterval;
            this.baseParticleUpdateInterval = baseParticleUpdateInterval;
        }
    }

    private static volatile QualityPreset defaultQualityPreset = QualityPreset.BALANCED;

    private static final int ANIMATION_FRAME_DELAY_MS = 16;
    private static final double PHASE_SPEED = 5.2;
    private static final int DEFAULT_HEAVY_REPAINT_INTERVAL_FRAMES = 2;
    private static final double BASE_FPS = 60.0;
    private static final double MAX_DELTA_SECONDS = 0.05;
    private static final double PROGRESS_LERP_RATE = 0.22;
    private static final int MAX_PARTICLES = 18;

    // Palette (theme-aware light + dark variants)
    private static final Color LIGHT_ACCENT_BLUE = new Color(23, 112, 238);
    private static final Color DARK_ACCENT_BLUE = new Color(95, 165, 255);

    private static final Color LIGHT_BACKGROUND_TOP = new Color(192, 224, 255);
    private static final Color LIGHT_BACKGROUND_MID = new Color(226, 242, 255);
    private static final Color LIGHT_BACKGROUND_BOTTOM = new Color(250, 252, 255);
    private static final Color DARK_BACKGROUND_TOP = new Color(18, 24, 38);
    private static final Color DARK_BACKGROUND_MID = new Color(24, 34, 52);
    private static final Color DARK_BACKGROUND_BOTTOM = new Color(12, 18, 30);

    private static final Color LIGHT_GLOW_BLUE = new Color(74, 168, 255);
    private static final Color DARK_GLOW_BLUE = new Color(84, 154, 250);

    private static final Color LIGHT_CARD_TINT_TOP = new Color(255, 255, 255, 168);
    private static final Color DARK_CARD_TINT_TOP = new Color(26, 34, 52, 198);
    private static final Color LIGHT_CARD_TINT_BOTTOM = new Color(74, 168, 255, 22);
    private static final Color DARK_CARD_TINT_BOTTOM = new Color(84, 154, 250, 36);

    private final AnimatedProgressBar progressBar;
    private final JLabel statusLabel;
    private final AnimatedLogoLabel logoLabel;

    private Timer animationTimer;
    private volatile double phase = 0.0;
    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final JComponent[] repaintTargets;
    private int frameCounter = 0;
    private long lastFrameNanos = -1L;
    private boolean lastKnownDarkMode;
    private volatile int targetProgress = 0;
    private double animatedProgress = 0.0;
    private int heavyRepaintIntervalFrames = DEFAULT_HEAVY_REPAINT_INTERVAL_FRAMES;
    private int particleUpdateIntervalFrames = 1;
    private int activeParticleCount = QualityPreset.BALANCED.particleCount;
    private QualityPreset qualityPreset;

    // Cached theme colors reduce allocations inside paint loops.
    private Color cachedAccentBlue;
    private Color cachedAccentBlueDark;
    private Color cachedAccentCyan;
    private Color cachedBackgroundTop;
    private Color cachedBackgroundMid;
    private Color cachedBackgroundBottom;
    private Color cachedGlowBlue;
    private Color cachedTextPrimary;
    private Color cachedTextSecondary;
    private Color cachedCardTintTop;
    private Color cachedCardTintBottom;
    private Color cachedProgressTrack;
    private Color cachedProgressTrackShadeTop;
    private Color cachedProgressTrackShadeBottom;
    private Color cachedProgressText;
    private Color cachedStreak1Color;

    // Responsive padding + content references
    private final JPanel contentPanel;
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    private static boolean isDarkTheme() {
        return ThemeManager.isDarkMode();
    }

    private static Color pick(Color light, Color dark) {
        return isDarkTheme() ? dark : light;
    }

    private Color accentBlue() {
        Color accent = ThemeManager.getAccentColor();
        return accent != null ? accent : pick(LIGHT_ACCENT_BLUE, DARK_ACCENT_BLUE);
    }

    private Color accentBlueDark() {
        Color base = accentBlue();
        return ThemeManager.adjustColor(base, isDarkTheme() ? -26 : -30);
    }

    private Color accentCyan() {
        Color base = accentBlue();
        return ThemeManager.adjustColor(base, isDarkTheme() ? 32 : 52);
    }

    private Color backgroundTop() { return cachedBackgroundTop; }
    private Color backgroundMid() { return cachedBackgroundMid; }
    private Color backgroundBottom() { return cachedBackgroundBottom; }
    private Color glowBlue() { return cachedGlowBlue; }
    private Color textPrimary() { return cachedTextPrimary; }
    private Color textSecondary() { return cachedTextSecondary; }
    private Color cardTintTop() { return cachedCardTintTop; }
    private Color cardTintBottom() { return cachedCardTintBottom; }

    /**
     * Creates and initializes the splash screen window.
     *
     * <p>Builds the UI, installs a resize listener for responsive sizing, initializes the particle system,
     * and starts the animation timer.
     */
    public SplashscreenGUI() {
        this(defaultQualityPreset);
    }

    public SplashscreenGUI(QualityPreset preset) {
        qualityPreset = preset == null ? defaultQualityPreset : preset;
        ThemeManager.getInstance().registerWindow(this);
        lastKnownDarkMode = isDarkTheme();
        setTitle("Lagersystem - Splashscreen");
        setSize(920, 620);
        setMinimumSize(new Dimension(760, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);
        // Nicer window behavior for undecorated splash
        setBackground(new Color(0, 0, 0, 0));

        GradientPanel root = new GradientPanel();
        root.setLayout(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        GlassPanel card = new GlassPanel();
        card.setLayout(new BorderLayout());

        // Inner content panel
        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        card.add(contentPanel, BorderLayout.CENTER);

        // Logo
        logoLabel = new AnimatedLogoLabel(loadLogo());
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(logoLabel);

        contentPanel.add(Box.createVerticalStrut(18));

        // Title
        titleLabel = new JLabel("Vebo Lagersystem");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 48));
        titleLabel.setForeground(accentBlue());
        contentPanel.add(titleLabel);

        // Subtitle
        subtitleLabel = new JLabel("Inventory Management");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 19));
        subtitleLabel.setForeground(textSecondary());
        contentPanel.add(subtitleLabel);

        contentPanel.add(Box.createVerticalStrut(26));

        // Status
        statusLabel = new JLabel("Initialisiere Programm...");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 17));
        statusLabel.setForeground(textPrimary());
        contentPanel.add(statusLabel);

        contentPanel.add(Box.createVerticalStrut(14));

        // Progress
        progressBar = new AnimatedProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setForeground(accentBlue());
        progressBar.setPreferredSize(new Dimension(420, 22));
        progressBar.setMaximumSize(new Dimension(560, 26));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(progressBar);

        // Center card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        root.add(card, gbc);
        repaintTargets = new JComponent[]{root, card};

        setContentPane(root);

        // ESC closes the splash (useful for debugging and UX)
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        root.getActionMap().put("close", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        // Drag anywhere to move (undecorated window)
        installWindowDrag(root);

        // Responsive tuning so everything always fits
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateResponsiveSizing();
            }
        });
        applyQualityPresetInternal();
        applyThemeColors();
        updateResponsiveSizing();

        initParticles();
        startAnimation();
    }

    public static void setDefaultQualityPreset(QualityPreset preset) {
        defaultQualityPreset = preset == null ? QualityPreset.BALANCED : preset;
    }

    public static QualityPreset getDefaultQualityPreset() {
        return defaultQualityPreset;
    }

    public QualityPreset getQualityPreset() {
        return qualityPreset;
    }

    public void setQualityPreset(QualityPreset preset) {
        QualityPreset next = preset == null ? defaultQualityPreset : preset;
        Runnable r = () -> {
            qualityPreset = next;
            applyQualityPresetInternal();
            retintParticles();
            repaint();
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    private void applyQualityPresetInternal() {
        QualityPreset effective = qualityPreset == null ? defaultQualityPreset : qualityPreset;
        activeParticleCount = clamp(effective.particleCount, 4, particles.length);
        heavyRepaintIntervalFrames = effective.baseHeavyRepaintInterval;
        particleUpdateIntervalFrames = effective.baseParticleUpdateInterval;
    }

    private void applyThemeColors() {
        refreshThemeCache();
        titleLabel.setForeground(cachedAccentBlue);
        subtitleLabel.setForeground(cachedTextSecondary);
        statusLabel.setForeground(cachedTextPrimary);
        progressBar.setForeground(cachedAccentBlue);
        retintParticles();
        repaint();
    }

    private void refreshThemeCache() {
        boolean dark = isDarkTheme();
        cachedAccentBlue = accentBlue();
        cachedAccentBlueDark = accentBlueDark();
        cachedAccentCyan = accentCyan();
        cachedBackgroundTop = pick(LIGHT_BACKGROUND_TOP, DARK_BACKGROUND_TOP);
        cachedBackgroundMid = pick(LIGHT_BACKGROUND_MID, DARK_BACKGROUND_MID);
        cachedBackgroundBottom = pick(LIGHT_BACKGROUND_BOTTOM, DARK_BACKGROUND_BOTTOM);
        cachedGlowBlue = pick(LIGHT_GLOW_BLUE, DARK_GLOW_BLUE);
        cachedTextPrimary = ThemeManager.getTextPrimaryColor();
        cachedTextSecondary = ThemeManager.getTextSecondaryColor();
        cachedCardTintTop = pick(LIGHT_CARD_TINT_TOP, DARK_CARD_TINT_TOP);
        cachedCardTintBottom = pick(LIGHT_CARD_TINT_BOTTOM, DARK_CARD_TINT_BOTTOM);
        cachedProgressTrack = dark ? new Color(44, 54, 72, 210) : new Color(255, 255, 255, 185);
        cachedProgressTrackShadeTop = dark ? new Color(255, 255, 255, 28) : new Color(255, 255, 255, 140);
        cachedProgressTrackShadeBottom = dark ? new Color(0, 0, 0, 66) : new Color(0, 0, 0, 18);
        cachedProgressText = dark ? new Color(236, 244, 255, 242) : new Color(255, 255, 255, 235);
        cachedStreak1Color = dark ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 66);
    }

    /**
     * Enables window dragging by mouse on a given component (for undecorated windows).
     */
    private void installWindowDrag(JComponent target) {
        final Point[] start = new Point[1];
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                start[0] = e.getPoint();
            }
        });
        target.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (start[0] == null) return;
                Point p = e.getLocationOnScreen();
                setLocation(p.x - start[0].x, p.y - start[0].y);
            }
        });
    }

    /**
     * Recomputes padding, typography, and progress bar size based on the current window size.
     * This prevents clipping on small windows and keeps the layout visually balanced.
     */
    private void updateResponsiveSizing() {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);

        // Inner padding scales with window size (prevents clipping on small windows)
        int padX = clamp((int) (w * 0.15), 52, 170);
        int padY = clamp((int) (h * 0.13), 44, 130);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));

        // Scale typography a bit
        int titleSize = clamp((int) (w * 0.052), 34, 52);
        int subSize = clamp((int) (w * 0.021), 16, 20);
        int statusSize = clamp((int) (w * 0.019), 14, 18);

        titleLabel.setFont(new Font("SansSerif", Font.BOLD, titleSize));
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, subSize));
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, statusSize));

        // Progress bar width adapts
        int pbW = clamp((int) (w * 0.50), 300, 600);
        int pbH = clamp((int) (h * 0.038), 20, 28);
        progressBar.setPreferredSize(new Dimension(pbW, pbH));
        progressBar.setMaximumSize(new Dimension(pbW, pbH));

        long area = (long) w * h;
        int baseHeavy = qualityPreset == null ? defaultQualityPreset.baseHeavyRepaintInterval : qualityPreset.baseHeavyRepaintInterval;
        int baseParticle = qualityPreset == null ? defaultQualityPreset.baseParticleUpdateInterval : qualityPreset.baseParticleUpdateInterval;
        if (area > 1_000_000L) {
            heavyRepaintIntervalFrames = Math.max(baseHeavy, baseHeavy + 2);
            particleUpdateIntervalFrames = Math.max(baseParticle, baseParticle + 1);
        } else if (area > 600_000L) {
            heavyRepaintIntervalFrames = Math.max(baseHeavy, baseHeavy + 1);
            particleUpdateIntervalFrames = Math.max(baseParticle, baseParticle + 1);
        } else {
            heavyRepaintIntervalFrames = baseHeavy;
            particleUpdateIntervalFrames = baseParticle;
        }

        revalidate();
        repaint();
    }

    /**
     * Clamps {@code v} to the inclusive range {@code [min, max]}.
     *
     * @param v value to clamp
     * @param min minimum allowed value
     * @param max maximum allowed value
     * @return clamped value
     */
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Updates the progress bar value and optionally updates the status message.
     *
     * @param percent progress value (typically 0–100)
     * @param message status text to display; ignored if {@code null} or blank
     */
    public void updateProgress(int percent, String message) {
        Runnable r = () -> {
            ensureAnimationRunning();
            int clampedPercent = clamp(percent, 0, 100);
            targetProgress = clampedPercent;
            if (message != null && !message.isBlank()) statusLabel.setText(message);
        };
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    /**
     * Shows the splash screen on the Swing Event Dispatch Thread (EDT).
     */
    public void display() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            ensureAnimationRunning();
        });
    }

    /**
     * Hides and disposes the splash screen on the Swing Event Dispatch Thread (EDT).
     * Also stops the animation timer.
     */
    public void close() {
        SwingUtilities.invokeLater(() -> {
            stopAnimation();
            setVisible(false);
            dispose();
        });
    }

    @Override
    public void dispose() {
        stopAnimation();
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    /**
     * Loads and scales the application logo used on the splash screen.
     *
     * @return the logo icon; returns a placeholder icon if the resource is missing
     */
    private static Icon loadLogo() {
        URL logoUrl = SplashscreenGUI.class.getResource("/logo-small.png");
        if (logoUrl == null) return new EmptyIcon(96, 96);

        ImageIcon icon = new ImageIcon(logoUrl);
        Image scaled = icon.getImage().getScaledInstance(96, 96, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // ---------------------------------------------------------------------------------------------
    // Background
    // ---------------------------------------------------------------------------------------------

    private class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

                GradientPaint gradientTop = new GradientPaint(
                        0, 0, backgroundTop(),
                        0, getHeight() * 0.65f, backgroundMid()
                );
                GradientPaint gradientBottom = new GradientPaint(
                        0, getHeight() * 0.35f, backgroundMid(),
                        0, getHeight(), backgroundBottom()
                );

                g2.setPaint(gradientTop);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setPaint(gradientBottom);
                g2.fillRect(0, 0, getWidth(), getHeight());

                double ph = SplashscreenGUI.this.getPhase();
                int drift = (int) (Math.sin(ph) * 7);
                int driftY = (int) (Math.cos(ph * 0.85) * 5);

                // Big soft blobs for depth (premium, less "flat")
                Color glow = glowBlue();
                Color cyan = cachedAccentCyan;
                g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 58));
                g2.fillOval(-240 + drift, -260 + driftY, 660, 660);

                g2.setColor(new Color(cyan.getRed(), cyan.getGreen(), cyan.getBlue(), 34));
                g2.fillOval(getWidth() - 520 - drift, getHeight() - 440 + driftY, 740, 740);

                // White haze highlights
                g2.setColor(new Color(255, 255, 255, 110));
                g2.fillOval(getWidth() - 300 - drift, -60 + driftY, 380, 380);

                g2.setColor(new Color(255, 255, 255, 70));
                g2.fillOval(drift - 40, getHeight() - 310 + driftY, 360, 360);

                paintAuroraBands(g2);
                SplashscreenGUI.this.paintLightStreaks(g2);
                SplashscreenGUI.this.paintParticles(g2);
            } finally {
                g2.dispose();
            }
        }

        private void paintAuroraBands(Graphics2D g2) {
            int w = getWidth();
            int h = getHeight();
            if (w <= 0 || h <= 0) return;

            double ph = SplashscreenGUI.this.getPhase();
            float cx = (float) ((Math.sin(ph * 0.35) * 0.5 + 0.5) * w);

            // Soft "aurora" band (two-pass for richer gradient)
            GradientPaint aurora = new GradientPaint(
                    cx - w * 0.55f, 0, new Color(255, 255, 255, 0),
                    cx + w * 0.55f, h, withAlpha(glowBlue(), 42)
            );
            g2.setPaint(aurora);
            g2.fillRect(0, 0, w, h);

            Color cyan = cachedAccentCyan;
            GradientPaint aurora2 = new GradientPaint(
                    cx - w * 0.35f, 0, withAlpha(cyan, 0),
                    cx + w * 0.35f, h, withAlpha(cyan, 26)
            );
            g2.setPaint(aurora2);
            g2.fillRect(0, 0, w, h);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Glass card (prettier + ensures content fits)
    // ---------------------------------------------------------------------------------------------

    private class GlassPanel extends JPanel {
        private static final int SHADOW = 14;
        private static final int ARC = 30;
        private static final float STROKE = 1.25f;

        GlassPanel() {
            setOpaque(false);
            setDoubleBuffered(true);
            setBorder(BorderFactory.createEmptyBorder(SHADOW, SHADOW, SHADOW, SHADOW));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            int width = getWidth();
            int height = getHeight();
            if (width <= 0 || height <= 0) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);

                int x = SHADOW;
                int y = SHADOW;
                int w = width - SHADOW * 2;
                int h = height - SHADOW * 2;
                if (w <= 0 || h <= 0) return;

                int arc = Math.min(ARC, Math.min(w, h));
                Shape glass = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

                // Shadow (layered, smoother)
                for (int i = 0; i < SHADOW; i++) {
                    float a = (float) (0.16 * (1.0 - (i / (double) SHADOW)));
                    g2.setColor(new Color(0f, 0f, 0f, a));
                    g2.fillRoundRect(
                            x + i - SHADOW / 2,
                            y + i - SHADOW / 2,
                            w - i + SHADOW,
                            h - i + SHADOW,
                            arc + 2, arc + 2
                    );
                }

                // Clip for fills
                g2.setClip(glass);

                // Base frosted glass
                g2.setColor(cardTintTop());
                g2.fill(glass);

                // Inner gradient tint (top brighter, bottom slightly blue)
                GradientPaint inner = new GradientPaint(
                        x, y, isDarkTheme() ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 128),
                        x, y + h, cardTintBottom()
                );
                g2.setPaint(inner);
                g2.fill(glass);

                // Subtle vignette for depth
                GradientPaint vignette = new GradientPaint(
                        x, y, new Color(0, 0, 0, 0),
                        x, y + h, new Color(0, 0, 0, 18)
                );
                g2.setPaint(vignette);
                g2.fill(glass);

                // Animated sweep highlight
                double ph = SplashscreenGUI.this.getPhase();
                float sweepX = (float) (x + ((Math.sin(ph * 0.8) * 0.5 + 0.5) * w));
                GradientPaint sweep = new GradientPaint(
                        sweepX - w * 0.70f, y, new Color(255, 255, 255, 0),
                        sweepX + w * 0.70f, y + h, new Color(255, 255, 255, 60)
                );
                g2.setPaint(sweep);
                g2.fill(glass);

                // Reset clip for strokes
                g2.setClip(null);

                // Border
                g2.setStroke(new BasicStroke(scale()));
                g2.setColor(new Color(255, 255, 255, 220));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                // Inner glow border
                g2.setColor(withAlpha(glowBlue(), 70));
                g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, Math.max(arc - 2, 10), Math.max(arc - 2, 10));

            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getInsets() {
            return new Insets(SHADOW, SHADOW, SHADOW, SHADOW);
        }

        @Override
        public Insets getInsets(Insets insets) {
            Insets i = getInsets();
            insets.top = i.top;
            insets.left = i.left;
            insets.bottom = i.bottom;
            insets.right = i.right;
            return insets;
        }

        private float scale() {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            if (gc == null) return GlassPanel.STROKE;
            double sx = gc.getDefaultTransform().getScaleX();
            double sy = gc.getDefaultTransform().getScaleY();
            double s = (sx + sy) / 2.0;
            return (float) (GlassPanel.STROKE * s);
        }
    }

    private record EmptyIcon(int width, int height) implements Icon {
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {}
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
    }

    // ---------------------------------------------------------------------------------------------
    // Animation
    // ---------------------------------------------------------------------------------------------

    /**
    * Starts the animation timer and repaints animated components regularly.
     */
    private void startAnimation() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::startAnimation);
            return;
        }

        if (animationTimer != null && animationTimer.isRunning()) {
            return;
        }

        lastFrameNanos = -1L;
        frameCounter = 0;

        animationTimer = new Timer(ANIMATION_FRAME_DELAY_MS, event -> {
            if (!isDisplayable()) {
                stopAnimation();
                return;
            }
            if (!isShowing()) {
                return;
            }

            long now = System.nanoTime();
            if (lastFrameNanos < 0L) {
                lastFrameNanos = now;
            }
            double deltaSeconds = (now - lastFrameNanos) / 1_000_000_000.0;
            if (deltaSeconds > MAX_DELTA_SECONDS) {
                deltaSeconds = MAX_DELTA_SECONDS;
            }
            lastFrameNanos = now;

            phase += PHASE_SPEED * deltaSeconds;
            if (frameCounter % particleUpdateIntervalFrames == 0) {
                advanceParticles(deltaSeconds * BASE_FPS);
            }
            animateProgress();
            frameCounter++;
            boolean dark = isDarkTheme();
            if (dark != lastKnownDarkMode) {
                lastKnownDarkMode = dark;
                applyThemeColors();
            }

            // Repaint only animated layers/components.
            logoLabel.repaint();
            progressBar.repaint();
            if (frameCounter % heavyRepaintIntervalFrames == 0) {
                for (JComponent component : repaintTargets) {
                    component.repaint();
                }
            }
        });
        animationTimer.setCoalesce(true);
        animationTimer.setRepeats(true);
        animationTimer.setInitialDelay(0);
        animationTimer.start();
    }

    /**
     * Stops the animation timer if it is running.
     */
    private void stopAnimation() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::stopAnimation);
            return;
        }

        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        lastFrameNanos = -1L;
    }

    private void ensureAnimationRunning() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::ensureAnimationRunning);
            return;
        }
        if (!isDisplayable()) {
            return;
        }
        if (animationTimer == null || !animationTimer.isRunning()) {
            startAnimation();
        }
    }

    /**
     * Returns the current animation phase value used to drive smooth oscillations.
     *
     * @return animation phase
     */
    private double getPhase() {
        return phase;
    }

    private void initParticles() {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);
        for (int i = 0; i < activeParticleCount; i++) {
            particles[i] = Particle.random(w, h);
        }
        for (int i = activeParticleCount; i < particles.length; i++) {
            particles[i] = null;
        }
    }

    private void retintParticles() {
        boolean dark = isDarkTheme();
        for (int i = 0; i < activeParticleCount; i++) {
            Particle particle = particles[i];
            if (particle != null) {
                particle.recolor(dark);
            }
        }
    }

    private void animateProgress() {
        int target = targetProgress;
        animatedProgress += (target - animatedProgress) * PROGRESS_LERP_RATE;

        if (Math.abs(target - animatedProgress) < 0.06) {
            animatedProgress = target;
        }

        int displayValue = clamp((int) Math.round(animatedProgress), 0, 100);
        if (progressBar.getValue() != displayValue) {
            progressBar.setValue(displayValue);
        }
    }

    private void advanceParticles(double deltaScale) {
        for (int i = 0; i < activeParticleCount; i++) {
            Particle particle = particles[i];
            if (particle != null) {
                particle.advance(getWidth(), getHeight(), deltaScale);
            }
        }
    }

    private void paintParticles(Graphics2D g2) {
        for (int i = 0; i < activeParticleCount; i++) {
            Particle particle = particles[i];
            if (particle != null) {
                particle.paint(g2);
            }
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
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int bob = (int) (Math.sin(SplashscreenGUI.this.getPhase()) * 3);
                g2.translate(0, bob);

                // Softer glow behind logo (less "disky")
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                int r = Math.max(getWidth(), getHeight());

                g2.setColor(withAlpha(glowBlue(), 30));
                g2.fillOval(cx - r / 2, cy - r / 2, r, r);

                g2.setColor(withAlpha(cachedAccentCyan, 18));
                g2.fillOval(cx - r / 3, cy - r / 3, (r * 2) / 3, (r * 2) / 3);

                g2.setColor(new Color(255, 255, 255, 62));
                g2.fillOval(cx - r / 4, cy - r / 4, r / 2, r / 2);

                super.paintComponent(g2);
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Rounded, glassy progress bar with animated specular highlight.
     */
    private class AnimatedProgressBar extends JProgressBar {
        AnimatedProgressBar(int min, int max) {
            super(min, max);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            setString(""); // we draw our own % text
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Insets in = getInsets();
                int x = in.left;
                int y = in.top;
                int w = getWidth() - in.left - in.right;
                int h = getHeight() - in.top - in.bottom;
                if (w <= 0 || h <= 0) return;

                int arc = Math.min(16, h);

                // Track (with subtle depth)
                g2.setColor(cachedProgressTrack);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                GradientPaint trackShade = new GradientPaint(
                        x, y,
                    cachedProgressTrackShadeTop,
                        x, y + h,
                    cachedProgressTrackShadeBottom
                );
                g2.setPaint(trackShade);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                // Fill
                double pct = getPercentComplete();
                int fillW = (int) Math.round(w * pct);
                if (fillW > 0) {
                    // base fill gradient
                    GradientPaint fillGrad = new GradientPaint(
                            x, y, cachedAccentBlue,
                            x + fillW, y + h, cachedAccentBlueDark
                    );
                    g2.setPaint(fillGrad);
                    g2.fillRoundRect(x, y, fillW, h, arc, arc);

                    // specular sweep
                    double ph = SplashscreenGUI.this.getPhase();
                    float sweepX = (float) (x + ((Math.sin(ph * 0.9) * 0.5 + 0.5) * fillW));
                    GradientPaint sweep = new GradientPaint(
                            sweepX - fillW * 0.55f, y, new Color(255, 255, 255, 0),
                            sweepX + fillW * 0.55f, y + h, new Color(255, 255, 255, 120)
                    );
                    Shape oldClip = g2.getClip();
                    g2.setClip(new RoundRectangle2D.Float(x, y, fillW, h, arc, arc));
                    g2.setPaint(sweep);
                    g2.fillRect(x, y, fillW, h);
                    g2.setClip(oldClip);
                }

                // Border
                g2.setColor(new Color(0, 0, 0, 28));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                // Percent text (crisp + readable)
                if (isStringPainted()) {
                    String s = (int) Math.round(pct * 100) + "%";
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = x + (w - fm.stringWidth(s)) / 2;
                    int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

                    g2.setColor(new Color(0, 0, 0, 55));
                    g2.drawString(s, tx + 1, ty + 1);

                    g2.setColor(cachedProgressText);
                    g2.drawString(s, tx, ty);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Particles
    // ---------------------------------------------------------------------------------------------

    private static class Particle {
        private double x;
        private double y;
        private final double radius;
        private final double speed;
        private final double drift;
        private final int alpha;
        private Color color;

        private Particle(double x, double y, double radius, double speed, double drift, int alpha, Color color) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.speed = speed;
            this.drift = drift;
            this.alpha = alpha;
            this.color = color;
        }

        static Particle random(int width, int height) {
            double radius = 5 + Math.random() * 15;
            double speed = 0.10 + Math.random() * 0.30;
            double drift = (Math.random() * 2.0) - 1.0;
            int alpha = 16 + (int) (Math.random() * 55);
            boolean dark = isDarkTheme();
            int tint = dark ? 110 + (int) (Math.random() * 80) : 205 + (int) (Math.random() * 50);
            Color color = dark ? new Color(120, 160, Math.min(255, tint + 70)) : new Color(tint, tint, 248);

            int w = Math.max(width, 1);
            int h = Math.max(height, 1);

            return new Particle(
                    Math.random() * w,
                    Math.random() * h,
                    radius,
                    speed,
                    drift,
                    alpha,
                    color
            );
        }

        void advance(int width, int height, double deltaScale) {
            y -= speed * deltaScale;
            x += drift * 0.12 * deltaScale;

            if (y + radius < 0) {
                int w = Math.max(width, 1);
                int h = Math.max(height, 1);
                y = h + radius;
                x = Math.random() * w;
            }
            if (x + radius < 0) x = width + radius;
            if (x - radius > width) x = -radius;
        }

        void paint(Graphics2D g2) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillOval((int) x, (int) y, (int) radius, (int) radius);
        }

        void recolor(boolean dark) {
            int tint = dark ? 110 + (int) (Math.random() * 80) : 205 + (int) (Math.random() * 50);
            this.color = dark ? new Color(120, 160, Math.min(255, tint + 70)) : new Color(tint, tint, 248);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Light streaks
    // ---------------------------------------------------------------------------------------------

    private void paintLightStreaks(Graphics2D g2) {
        double ph = getPhase();
        int w = getWidth();
        int h = getHeight();

        float x1 = (float) ((Math.sin(ph * 0.6) * 0.5 + 0.5) * w);
        float x2 = (float) ((Math.cos(ph * 0.5) * 0.5 + 0.5) * w);

        GradientPaint streak1 = new GradientPaint(
                x1 - w * 0.34f, 0, new Color(255, 255, 255, 0),
                x1 + w * 0.34f, h, cachedStreak1Color
        );
        g2.setPaint(streak1);
        g2.fillRect(0, 0, w, h);

        GradientPaint streak2 = new GradientPaint(
                x2 - w * 0.38f, 0, withAlpha(glowBlue(), 0),
                x2 + w * 0.38f, h, withAlpha(glowBlue(), 46)
        );
        g2.setPaint(streak2);
        g2.fillRect(0, 0, w, h);
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamp(alpha, 0, 255));
    }
}