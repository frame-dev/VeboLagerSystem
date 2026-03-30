package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Locale;

/**
 * Displays a modern splash screen for the LagerSystem application.
 *
 * <p>
 * The UI uses a gradient background, subtle animated particles, and a
 * glass-like card that contains
 * the application logo, title/subtitle, a status message, and an animated
 * progress bar.
 * The layout is responsive and adapts its padding and typography when the
 * window is resized.
 *
 * <p>
 * Progress and status can be updated at runtime via
 * {@link #updateProgress(int, String)}.
 * The animation is driven by a {@link Timer} that periodically repaints the
 * relevant components.
 */
@SuppressWarnings("ALL")
public class SplashscreenGUI extends JFrame {

    private static final String WINDOWS_OS_TOKEN = "windows";

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
    private static volatile Icon cachedLogoIcon;

    private static final int ANIMATION_FRAME_DELAY_MS = 16;
    private static final int WINDOWS_ANIMATION_FRAME_DELAY_MS = 33;
    private static final double PHASE_SPEED = 5.2;
    private static final int DEFAULT_HEAVY_REPAINT_INTERVAL_FRAMES = 2;
    private static final int WINDOWS_MIN_HEAVY_REPAINT_INTERVAL_FRAMES = 4;
    private static final int WINDOWS_MIN_PARTICLE_COUNT = 4;
    private static final int BACKGROUND_CACHE_FRAME_COUNT = 24;
    private static final int REDUCED_BACKGROUND_CACHE_FRAME_COUNT = 12;
    private static final double BACKGROUND_CACHE_LOOP_PHASE = Math.PI * 40.0;
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
    private final GlassPanel cardPanel;

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
    private boolean stableSurfaceMode;
    private boolean reducedAnimationMode;
    private int lastPadX = -1;
    private int lastPadY = -1;
    private int lastTitleSize = -1;
    private int lastSubtitleSize = -1;
    private int lastStatusSize = -1;
    private int lastProgressBarWidth = -1;
    private int lastProgressBarHeight = -1;
    private volatile String lastStatusMessage = "";
    private volatile BufferedImage[] backgroundFrameCache;
    private volatile int cachedBackgroundWidth = -1;
    private volatile int cachedBackgroundHeight = -1;
    private volatile int cachedBackgroundFrameCount = 0;
    private volatile boolean cachedBackgroundDarkMode;
    private volatile boolean cachedBackgroundReducedMode;
    private volatile boolean backgroundCacheBuilding;
    private volatile long backgroundCacheGeneration;
    private volatile BufferedImage[] cardFrameCache;
    private volatile int cachedCardWidth = -1;
    private volatile int cachedCardHeight = -1;
    private volatile int cachedCardFrameCount = 0;
    private volatile boolean cachedCardDarkMode;
    private volatile boolean cachedCardReducedMode;
    private volatile boolean cardCacheBuilding;

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

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains(WINDOWS_OS_TOKEN);
    }

    private static Font uiFont(int style, int size) {
        if (isWindows()) {
            Font segoe = new Font("Segoe UI", style, size);
            if (!"Dialog".equalsIgnoreCase(segoe.getFamily())) {
                return segoe;
            }
        }
        return new Font("SansSerif", style, size);
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

    private Color backgroundTop() {
        return cachedBackgroundTop;
    }

    private Color backgroundMid() {
        return cachedBackgroundMid;
    }

    private Color backgroundBottom() {
        return cachedBackgroundBottom;
    }

    private Color glowBlue() {
        return cachedGlowBlue;
    }

    private Color textPrimary() {
        return cachedTextPrimary;
    }

    private Color textSecondary() {
        return cachedTextSecondary;
    }

    private Color cardTintTop() {
        return cachedCardTintTop;
    }

    private Color cardTintBottom() {
        return cachedCardTintBottom;
    }

    private record BackgroundFramePalette(
            Color backgroundTop,
            Color backgroundMid,
            Color backgroundBottom,
            Color glowBlue,
            Color accentCyan,
            Color streak1Color) {
    }

    private record CardFramePalette(
            Color glowBlue,
            Color cardTintTop,
            Color cardTintBottom,
            boolean darkTheme) {
    }

    /**
     * Creates and initializes the splash screen window.
     *
     * <p>
     * Builds the UI, installs a resize listener for responsive sizing, initializes
     * the particle system,
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
        configureWindowSurfaceForCurrentPlatform();

        GradientPanel root = new GradientPanel();
        root.setLayout(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(26, 26, 26, 26));

        cardPanel = new GlassPanel();
        cardPanel.setLayout(new BorderLayout());

        // Inner content panel
        contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        cardPanel.add(contentPanel, BorderLayout.CENTER);

        // Logo
        logoLabel = new AnimatedLogoLabel(loadLogo());
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(logoLabel);

        contentPanel.add(Box.createVerticalStrut(18));

        // Title
        titleLabel = new JLabel("Vebo Lagersystem");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(uiFont(Font.BOLD, 48));
        titleLabel.setForeground(accentBlue());
        contentPanel.add(titleLabel);

        // Subtitle
        subtitleLabel = new JLabel("Inventory Management");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(uiFont(Font.PLAIN, 19));
        subtitleLabel.setForeground(textSecondary());
        contentPanel.add(subtitleLabel);

        contentPanel.add(Box.createVerticalStrut(26));

        // Status
        statusLabel = new JLabel("Initialisiere Programm...");
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusLabel.setFont(uiFont(Font.PLAIN, 17));
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
        root.add(cardPanel, gbc);
        repaintTargets = new JComponent[] { root, cardPanel };

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
        requestAnimationCacheWarmup();
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
        if (stableSurfaceMode) {
            heavyRepaintIntervalFrames = Math.max(heavyRepaintIntervalFrames,
                    WINDOWS_MIN_HEAVY_REPAINT_INTERVAL_FRAMES);
            particleUpdateIntervalFrames = Math.max(particleUpdateIntervalFrames, 2);
        }
        if (reducedAnimationMode) {
            activeParticleCount = Math.min(activeParticleCount, WINDOWS_MIN_PARTICLE_COUNT);
            heavyRepaintIntervalFrames = Math.max(heavyRepaintIntervalFrames, 6);
            particleUpdateIntervalFrames = Math.max(particleUpdateIntervalFrames, 3);
        }
    }

    private void applyThemeColors() {
        refreshThemeCache();
        titleLabel.setForeground(cachedAccentBlue);
        subtitleLabel.setForeground(cachedTextSecondary);
        statusLabel.setForeground(cachedTextPrimary);
        progressBar.setForeground(cachedAccentBlue);
        retintParticles();
        invalidateAnimationCache();
        requestAnimationCacheWarmup();
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
     * Enables window dragging by mouse on a given component (for undecorated
     * windows).
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
                if (start[0] == null)
                    return;
                Point p = e.getLocationOnScreen();
                setLocation(p.x - start[0].x, p.y - start[0].y);
            }
        });
    }

    /**
     * Recomputes padding, typography, and progress bar size based on the current
     * window size.
     * This prevents clipping on small windows and keeps the layout visually
     * balanced.
     */
    private void updateResponsiveSizing() {
        int w = Math.max(getWidth(), 1);
        int h = Math.max(getHeight(), 1);
        boolean layoutChanged = false;

        // Inner padding scales with window size (prevents clipping on small windows)
        int padX = clamp((int) (w * 0.15), 52, 170);
        int padY = clamp((int) (h * 0.13), 44, 130);
        if (padX != lastPadX || padY != lastPadY) {
            contentPanel.setBorder(BorderFactory.createEmptyBorder(padY, padX, padY, padX));
            lastPadX = padX;
            lastPadY = padY;
            layoutChanged = true;
        }

        // Scale typography a bit
        int titleSize = clamp((int) (w * 0.052), 34, 52);
        int subSize = clamp((int) (w * 0.021), 16, 20);
        int statusSize = clamp((int) (w * 0.019), 14, 18);

        if (titleSize != lastTitleSize) {
            titleLabel.setFont(uiFont(Font.BOLD, titleSize));
            lastTitleSize = titleSize;
            layoutChanged = true;
        }
        if (subSize != lastSubtitleSize) {
            subtitleLabel.setFont(uiFont(Font.PLAIN, subSize));
            lastSubtitleSize = subSize;
            layoutChanged = true;
        }
        if (statusSize != lastStatusSize) {
            statusLabel.setFont(uiFont(Font.PLAIN, statusSize));
            lastStatusSize = statusSize;
            layoutChanged = true;
        }

        // Progress bar width adapts
        int pbW = clamp((int) (w * 0.50), 300, 600);
        int pbH = clamp((int) (h * 0.038), 20, 28);
        if (pbW != lastProgressBarWidth || pbH != lastProgressBarHeight) {
            Dimension progressSize = new Dimension(pbW, pbH);
            progressBar.setPreferredSize(progressSize);
            progressBar.setMaximumSize(progressSize);
            lastProgressBarWidth = pbW;
            lastProgressBarHeight = pbH;
            layoutChanged = true;
        }

        long area = (long) w * h;
        int baseHeavy = qualityPreset == null ? defaultQualityPreset.baseHeavyRepaintInterval
                : qualityPreset.baseHeavyRepaintInterval;
        int baseParticle = qualityPreset == null ? defaultQualityPreset.baseParticleUpdateInterval
                : qualityPreset.baseParticleUpdateInterval;
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

        if (reducedAnimationMode) {
            activeParticleCount = Math.min(activeParticleCount, WINDOWS_MIN_PARTICLE_COUNT);
            heavyRepaintIntervalFrames = Math.max(heavyRepaintIntervalFrames, 6);
            particleUpdateIntervalFrames = Math.max(particleUpdateIntervalFrames, 3);
        }

        if (layoutChanged) {
            invalidateAnimationCache();
            requestAnimationCacheWarmup();
            revalidate();
            repaint();
        }
    }

    private void configureWindowSurfaceForCurrentPlatform() {
        setUndecorated(true);

        // Some Windows setups do not support per-pixel translucency with undecorated
        // windows.
        GraphicsDevice graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        boolean supportsPerPixelTranslucency = graphicsDevice != null
                && graphicsDevice.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);

        // Transparent undecorated windows are visually unstable on many Windows +
        // driver
        // combinations, so keep a stable opaque surface there.
        stableSurfaceMode = isWindows() || !supportsPerPixelTranslucency;
        reducedAnimationMode = isWindows();
        if (!stableSurfaceMode) {
            setBackground(new Color(0, 0, 0, 0));
            return;
        }

        // Fallback to opaque background to avoid rendering artifacts (common on older
        // Windows drivers).
        setBackground(pick(LIGHT_BACKGROUND_BOTTOM, DARK_BACKGROUND_BOTTOM));
    }

    /**
     * Clamps {@code v} to the inclusive range {@code [min, max]}.
     *
     * @param v   value to clamp
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
            int clampedPercent = clamp(percent, 0, 100);
            boolean progressChanged = targetProgress != clampedPercent;
            boolean messageChanged = message != null && !message.isBlank() && !message.equals(lastStatusMessage);
            if (!progressChanged && !messageChanged) {
                return;
            }

            ensureAnimationRunning();
            targetProgress = clampedPercent;
            if (messageChanged) {
                lastStatusMessage = message;
                statusLabel.setText(message);
            }
        };
        if (SwingUtilities.isEventDispatchThread())
            r.run();
        else
            SwingUtilities.invokeLater(r);
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
     * Hides and disposes the splash screen on the Swing Event Dispatch Thread
     * (EDT).
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
        Icon cached = cachedLogoIcon;
        if (cached != null) {
            return cached;
        }

        URL logoUrl = SplashscreenGUI.class.getResource("/logo-small.png");
        Icon resolvedIcon;
        if (logoUrl == null) {
            resolvedIcon = new EmptyIcon(96, 96);
        } else {
            ImageIcon icon = new ImageIcon(logoUrl);
            Image scaled = icon.getImage().getScaledInstance(96, 96, Image.SCALE_SMOOTH);
            resolvedIcon = new ImageIcon(scaled);
        }

        cachedLogoIcon = resolvedIcon;
        return resolvedIcon;
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
                BufferedImage cachedFrame = getCachedBackgroundFrame();
                if (cachedFrame != null) {
                    g2.drawImage(cachedFrame, 0, 0, null);
                } else {
                    paintAnimatedBackgroundFrame(g2, getWidth(), getHeight(), getPhase(), reducedAnimationMode,
                            currentBackgroundFramePalette());
                }
                if (activeParticleCount > 0) {
                    SplashscreenGUI.this.paintParticles(g2);
                }
            } finally {
                g2.dispose();
            }
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
            if (width <= 0 || height <= 0)
                return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
                BufferedImage cachedFrame = getCachedCardFrame(width, height);
                if (cachedFrame != null) {
                    g2.drawImage(cachedFrame, 0, 0, null);
                } else {
                    paintAnimatedGlassCardFrame(g2, width, height, getPhase(), reducedAnimationMode,
                            currentCardFramePalette(), scale());
                }
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
            if (gc == null)
                return GlassPanel.STROKE;
            double sx = gc.getDefaultTransform().getScaleX();
            double sy = gc.getDefaultTransform().getScaleY();
            double s = (sx + sy) / 2.0;
            return (float) (GlassPanel.STROKE * s);
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

        requestAnimationCacheWarmup();
        lastFrameNanos = -1L;
        frameCounter = 0;

        animationTimer = new Timer(getAnimationFrameDelayMs(), event -> {
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
            if (!reducedAnimationMode || frameCounter % 2 == 0) {
                logoLabel.repaint();
            }
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

    private int getAnimationFrameDelayMs() {
        return stableSurfaceMode ? WINDOWS_ANIMATION_FRAME_DELAY_MS : ANIMATION_FRAME_DELAY_MS;
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
        requestAnimationCacheWarmup();
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

                int bob = reducedAnimationMode ? 0 : (int) (Math.sin(SplashscreenGUI.this.getPhase()) * 3);
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
                if (w <= 0 || h <= 0)
                    return;

                int arc = Math.min(16, h);

                // Track (with subtle depth)
                g2.setColor(cachedProgressTrack);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                GradientPaint trackShade = new GradientPaint(
                        x, y,
                        cachedProgressTrackShadeTop,
                        x, y + h,
                        cachedProgressTrackShadeBottom);
                g2.setPaint(trackShade);
                g2.fillRoundRect(x, y, w, h, arc, arc);

                // Fill
                double pct = getPercentComplete();
                int fillW = (int) Math.round(w * pct);
                if (fillW > 0) {
                    // base fill gradient
                    GradientPaint fillGrad = new GradientPaint(
                            x, y, cachedAccentBlue,
                            x + fillW, y + h, cachedAccentBlueDark);
                    g2.setPaint(fillGrad);
                    g2.fillRoundRect(x, y, fillW, h, arc, arc);

                    // specular sweep
                    if (!reducedAnimationMode) {
                        double ph = SplashscreenGUI.this.getPhase();
                        float sweepX = (float) (x + ((Math.sin(ph * 0.9) * 0.5 + 0.5) * fillW));
                        GradientPaint sweep = new GradientPaint(
                                sweepX - fillW * 0.55f, y, new Color(255, 255, 255, 0),
                                sweepX + fillW * 0.55f, y + h, new Color(255, 255, 255, 120));
                        Shape oldClip = g2.getClip();
                        g2.setClip(new RoundRectangle2D.Float(x, y, fillW, h, arc, arc));
                        g2.setPaint(sweep);
                        g2.fillRect(x, y, fillW, h);
                        g2.setClip(oldClip);
                    }
                }

                // Border
                g2.setColor(new Color(0, 0, 0, 28));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

                // Percent text (crisp + readable)
                if (isStringPainted()) {
                    String s = (int) Math.round(pct * 100) + "%";
                    g2.setFont(uiFont(Font.BOLD, 12));
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
                    color);
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
            if (x + radius < 0)
                x = width + radius;
            if (x - radius > width)
                x = -radius;
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

    private BackgroundFramePalette currentBackgroundFramePalette() {
        return new BackgroundFramePalette(
                backgroundTop(),
                backgroundMid(),
                backgroundBottom(),
                glowBlue(),
                cachedAccentCyan,
                cachedStreak1Color);
    }

    private CardFramePalette currentCardFramePalette() {
        return new CardFramePalette(
                glowBlue(),
                cardTintTop(),
                cardTintBottom(),
                isDarkTheme());
    }

    private void invalidateAnimationCache() {
        backgroundCacheGeneration++;
        backgroundFrameCache = null;
        cachedBackgroundWidth = -1;
        cachedBackgroundHeight = -1;
        cachedBackgroundFrameCount = 0;
        cachedBackgroundDarkMode = false;
        cachedBackgroundReducedMode = false;
        backgroundCacheBuilding = false;
        cardFrameCache = null;
        cachedCardWidth = -1;
        cachedCardHeight = -1;
        cachedCardFrameCount = 0;
        cachedCardDarkMode = false;
        cachedCardReducedMode = false;
        cardCacheBuilding = false;
    }

    private void requestAnimationCacheWarmup() {
        int width = Math.max(getWidth(), 0);
        int height = Math.max(getHeight(), 0);
        if (width <= 0 || height <= 0) {
            return;
        }

        boolean dark = isDarkTheme();
        boolean reduced = reducedAnimationMode;
        int frameCount = reduced ? REDUCED_BACKGROUND_CACHE_FRAME_COUNT : BACKGROUND_CACHE_FRAME_COUNT;
        BufferedImage[] cache = backgroundFrameCache;
        if (cache != null
                && cachedBackgroundWidth == width
                && cachedBackgroundHeight == height
                && cachedBackgroundFrameCount == frameCount
                && cachedBackgroundDarkMode == dark
                && cachedBackgroundReducedMode == reduced) {
            return;
        }

        synchronized (this) {
            cache = backgroundFrameCache;
            if (cache != null
                    && cachedBackgroundWidth == width
                    && cachedBackgroundHeight == height
                    && cachedBackgroundFrameCount == frameCount
                    && cachedBackgroundDarkMode == dark
                    && cachedBackgroundReducedMode == reduced) {
                return;
            }
            if (backgroundCacheBuilding) {
                return;
            }
            backgroundCacheBuilding = true;
        }

        long generation = backgroundCacheGeneration;
        BackgroundFramePalette palette = currentBackgroundFramePalette();
        Thread cacheThread = new Thread(() -> buildAnimationCache(width, height, frameCount, reduced, dark, generation, palette),
                "Splashscreen-background-cache");
        cacheThread.setDaemon(true);
        cacheThread.start();
        int cardWidth = Math.max(cardPanel.getWidth(), 0);
        int cardHeight = Math.max(cardPanel.getHeight(), 0);
        if (cardWidth > 0 && cardHeight > 0) {
            requestCardAnimationCacheWarmup(cardWidth, cardHeight, frameCount, reduced, dark, generation);
        }
    }

    private void buildAnimationCache(int width,
            int height,
            int frameCount,
            boolean reducedMode,
            boolean darkMode,
            long generation,
            BackgroundFramePalette palette) {
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            double phaseValue = (BACKGROUND_CACHE_LOOP_PHASE * i) / frameCount;
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = frame.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
                paintAnimatedBackgroundFrame(g2, width, height, phaseValue, reducedMode, palette);
            } finally {
                g2.dispose();
            }
            frames[i] = frame;
        }

        SwingUtilities.invokeLater(() -> {
            if (generation == backgroundCacheGeneration
                    && width == getWidth()
                    && height == getHeight()
                    && darkMode == isDarkTheme()
                    && reducedMode == reducedAnimationMode) {
                backgroundFrameCache = frames;
                cachedBackgroundWidth = width;
                cachedBackgroundHeight = height;
                cachedBackgroundFrameCount = frameCount;
                cachedBackgroundDarkMode = darkMode;
                cachedBackgroundReducedMode = reducedMode;
                repaint();
            }
            backgroundCacheBuilding = false;
        });
    }

    private BufferedImage getCachedBackgroundFrame() {
        BufferedImage[] cache = backgroundFrameCache;
        if (cache == null || cache.length == 0) {
            return null;
        }

        double normalizedPhase = getPhase() % BACKGROUND_CACHE_LOOP_PHASE;
        if (normalizedPhase < 0.0) {
            normalizedPhase += BACKGROUND_CACHE_LOOP_PHASE;
        }
        int index = (int) Math.floor((normalizedPhase / BACKGROUND_CACHE_LOOP_PHASE) * cache.length);
        return cache[Math.min(Math.max(index, 0), cache.length - 1)];
    }

    private void requestCardAnimationCacheWarmup(int width, int height, int frameCount, boolean reduced, boolean dark,
            long generation) {
        synchronized (this) {
            if (cardFrameCache != null
                    && cachedCardWidth == width
                    && cachedCardHeight == height
                    && cachedCardFrameCount == frameCount
                    && cachedCardDarkMode == dark
                    && cachedCardReducedMode == reduced) {
                return;
            }
            if (cardCacheBuilding) {
                return;
            }
            cardCacheBuilding = true;
        }

        CardFramePalette palette = currentCardFramePalette();
        Thread cacheThread = new Thread(
                () -> buildCardAnimationCache(width, height, frameCount, reduced, dark, generation, palette),
                "Splashscreen-card-cache");
        cacheThread.setDaemon(true);
        cacheThread.start();
    }

    private void buildCardAnimationCache(int width,
            int height,
            int frameCount,
            boolean reducedMode,
            boolean darkMode,
            long generation,
            CardFramePalette palette) {
        BufferedImage[] frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            double phaseValue = (BACKGROUND_CACHE_LOOP_PHASE * i) / frameCount;
            BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = frame.createGraphics();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
                paintAnimatedGlassCardFrame(g2, width, height, phaseValue, reducedMode, palette, GlassPanel.STROKE);
            } finally {
                g2.dispose();
            }
            frames[i] = frame;
        }

        SwingUtilities.invokeLater(() -> {
            if (generation == backgroundCacheGeneration
                    && width == getWidth()
                    && height == getHeight()
                    && darkMode == isDarkTheme()
                    && reducedMode == reducedAnimationMode) {
                cardFrameCache = frames;
                cachedCardWidth = width;
                cachedCardHeight = height;
                cachedCardFrameCount = frameCount;
                cachedCardDarkMode = darkMode;
                cachedCardReducedMode = reducedMode;
                repaint();
            }
            cardCacheBuilding = false;
        });
    }

    private BufferedImage getCachedCardFrame(int width, int height) {
        BufferedImage[] cache = cardFrameCache;
        if (cache == null || cache.length == 0 || width != cachedCardWidth || height != cachedCardHeight) {
            return null;
        }

        double normalizedPhase = getPhase() % BACKGROUND_CACHE_LOOP_PHASE;
        if (normalizedPhase < 0.0) {
            normalizedPhase += BACKGROUND_CACHE_LOOP_PHASE;
        }
        int index = (int) Math.floor((normalizedPhase / BACKGROUND_CACHE_LOOP_PHASE) * cache.length);
        return cache[Math.min(Math.max(index, 0), cache.length - 1)];
    }

    private void paintAnimatedBackgroundFrame(Graphics2D g2,
            int width,
            int height,
            double phaseValue,
            boolean reducedMode,
            BackgroundFramePalette palette) {
        if (width <= 0 || height <= 0 || palette == null) {
            return;
        }

        GradientPaint gradientTop = new GradientPaint(
                0, 0, palette.backgroundTop(),
                0, height * 0.65f, palette.backgroundMid());
        GradientPaint gradientBottom = new GradientPaint(
                0, height * 0.35f, palette.backgroundMid(),
                0, height, palette.backgroundBottom());

        g2.setPaint(gradientTop);
        g2.fillRect(0, 0, width, height);
        g2.setPaint(gradientBottom);
        g2.fillRect(0, 0, width, height);

        int drift = reducedMode ? 0 : (int) (Math.sin(phaseValue) * 7);
        int driftY = reducedMode ? 0 : (int) (Math.cos(phaseValue * 0.85) * 5);

        Color glow = palette.glowBlue();
        Color cyan = palette.accentCyan();
        g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 58));
        g2.fillOval(-240 + drift, -260 + driftY, 660, 660);

        g2.setColor(new Color(cyan.getRed(), cyan.getGreen(), cyan.getBlue(), 34));
        g2.fillOval(width - 520 - drift, height - 440 + driftY, 740, 740);

        g2.setColor(new Color(255, 255, 255, 110));
        g2.fillOval(width - 300 - drift, -60 + driftY, 380, 380);

        g2.setColor(new Color(255, 255, 255, 70));
        g2.fillOval(drift - 40, height - 310 + driftY, 360, 360);

        if (!reducedMode) {
            paintAuroraBands(g2, width, height, phaseValue, palette);
            paintLightStreaks(g2, width, height, phaseValue, palette);
        }
    }

    private void paintAnimatedGlassCardFrame(Graphics2D g2,
            int width,
            int height,
            double phaseValue,
            boolean reducedMode,
            CardFramePalette palette,
            float strokeScale) {
        if (width <= 0 || height <= 0 || palette == null) {
            return;
        }

        int x = GlassPanel.SHADOW;
        int y = GlassPanel.SHADOW;
        int w = width - GlassPanel.SHADOW * 2;
        int h = height - GlassPanel.SHADOW * 2;
        if (w <= 0 || h <= 0) {
            return;
        }

        int arc = Math.min(GlassPanel.ARC, Math.min(w, h));
        Shape glass = new RoundRectangle2D.Float(x, y, w, h, arc, arc);

        int shadowLayers = reducedMode ? 6 : GlassPanel.SHADOW;
        for (int i = 0; i < shadowLayers; i++) {
            float a = (float) (0.16 * (1.0 - (i / (double) GlassPanel.SHADOW)));
            g2.setColor(new Color(0f, 0f, 0f, a));
            g2.fillRoundRect(
                    x + i - GlassPanel.SHADOW / 2,
                    y + i - GlassPanel.SHADOW / 2,
                    w - i + GlassPanel.SHADOW,
                    h - i + GlassPanel.SHADOW,
                    arc + 2, arc + 2);
        }

        g2.setClip(glass);
        g2.setColor(palette.cardTintTop());
        g2.fill(glass);

        GradientPaint inner = new GradientPaint(
                x, y, palette.darkTheme() ? new Color(255, 255, 255, 34) : new Color(255, 255, 255, 128),
                x, y + h, palette.cardTintBottom());
        g2.setPaint(inner);
        g2.fill(glass);

        GradientPaint vignette = new GradientPaint(
                x, y, new Color(0, 0, 0, 0),
                x, y + h, new Color(0, 0, 0, 18));
        g2.setPaint(vignette);
        g2.fill(glass);

        if (!reducedMode) {
            float sweepX = (float) (x + ((Math.sin(phaseValue * 0.8) * 0.5 + 0.5) * w));
            GradientPaint sweep = new GradientPaint(
                    sweepX - w * 0.70f, y, new Color(255, 255, 255, 0),
                    sweepX + w * 0.70f, y + h, new Color(255, 255, 255, 60));
            g2.setPaint(sweep);
            g2.fill(glass);
        }

        g2.setClip(null);
        g2.setStroke(new BasicStroke(strokeScale));
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawRoundRect(x, y, w - 1, h - 1, arc, arc);

        g2.setColor(withAlpha(palette.glowBlue(), 70));
        g2.drawRoundRect(x + 2, y + 2, w - 5, h - 5, Math.max(arc - 2, 10), Math.max(arc - 2, 10));
    }

    private void paintAuroraBands(Graphics2D g2, int width, int height, double phaseValue, BackgroundFramePalette palette) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float cx = (float) ((Math.sin(phaseValue * 0.35) * 0.5 + 0.5) * width);

        GradientPaint aurora = new GradientPaint(
                cx - width * 0.55f, 0, new Color(255, 255, 255, 0),
                cx + width * 0.55f, height, withAlpha(palette.glowBlue(), 42));
        g2.setPaint(aurora);
        g2.fillRect(0, 0, width, height);

        GradientPaint aurora2 = new GradientPaint(
                cx - width * 0.35f, 0, withAlpha(palette.accentCyan(), 0),
                cx + width * 0.35f, height, withAlpha(palette.accentCyan(), 26));
        g2.setPaint(aurora2);
        g2.fillRect(0, 0, width, height);
    }

    private void paintLightStreaks(Graphics2D g2, int width, int height, double phaseValue, BackgroundFramePalette palette) {
        float x1 = (float) ((Math.sin(phaseValue * 0.6) * 0.5 + 0.5) * width);
        float x2 = (float) ((Math.cos(phaseValue * 0.5) * 0.5 + 0.5) * width);

        GradientPaint streak1 = new GradientPaint(
                x1 - width * 0.34f, 0, new Color(255, 255, 255, 0),
                x1 + width * 0.34f, height, palette.streak1Color());
        g2.setPaint(streak1);
        g2.fillRect(0, 0, width, height);

        GradientPaint streak2 = new GradientPaint(
                x2 - width * 0.38f, 0, withAlpha(palette.glowBlue(), 0),
                x2 + width * 0.38f, height, withAlpha(palette.glowBlue(), 46));
        g2.setPaint(streak2);
        g2.fillRect(0, 0, width, height);
    }

    private static Color withAlpha(Color base, int alpha) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), clamp(alpha, 0, 255));
    }
}
