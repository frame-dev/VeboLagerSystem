package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Modern Main Dashboard for VEBO Lagersystem with Tabbed Interface
 */
public class MainGUI extends JFrame {

    private final JTabbedPane tabbedPane;

    // Keep references so content stays alive and doesn't get disposed accidentally
    public static ArticleGUI articleGUI;
    private VendorGUI vendorGUI;
    private OrderGUI orderGUI;
    private ClientGUI clientGUI;

    // Tab wrappers
    private final JPanel articleWrapper = createTabWrapper();
    private final JPanel vendorWrapper  = createTabWrapper();
    private final JPanel orderWrapper   = createTabWrapper();
    private final JPanel clientWrapper  = createTabWrapper();

    public MainGUI() {
        ThemeManager.getInstance().registerWindow(this);
        ThemeManager.applyUIDefaults();

        setTitle("VEBO Lagersystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setIconImage(Main.icon.getImage());
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeManager.getBackgroundColor());

        // ===== HEADER =====
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(ThemeManager.getHeaderBackgroundColor());
        headerPanel.setPreferredSize(new Dimension(0, 100));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));

        JLabel titleLabel = new JLabel("VEBO Lagersystem");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 42));
        titleLabel.setForeground(ThemeManager.getHeaderForegroundColor());
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JLabel subtitleLabel = new JLabel("Zentrale Verwaltung für Artikel, Bestellungen und Lieferanten");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(ThemeManager.getTextSecondaryColor());
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        headerTextPanel.add(subtitleLabel);

        headerPanel.add(headerTextPanel, BorderLayout.WEST);

        // Right panel with settings + date
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        JButton settingsButton = new JButton("⚙️ Einstellungen");
        styleHeaderButton(settingsButton);
        settingsButton.addActionListener(e -> {
            SettingsGUI settingsGUI = new SettingsGUI();
            settingsGUI.display();
        });

        JLabel dateLabel = new JLabel(new SimpleDateFormat("dd. MMMM yyyy").format(new Date()));
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dateLabel.setForeground(ThemeManager.getTextSecondaryColor());

        rightPanel.add(settingsButton);
        rightPanel.add(dateLabel);

        headerPanel.add(rightPanel, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // ===== TABS =====
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 15));
        tabbedPane.setBackground(ThemeManager.getBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());

        // Tabs
        tabbedPane.addTab("  📦 Artikel  ", null, articleWrapper, "Artikelverwaltung");
        tabbedPane.addTab("  🚚 Lieferanten  ", null, vendorWrapper, "Lieferantenverwaltung");
        tabbedPane.addTab("  📋 Bestellungen  ", null, orderWrapper, "Bestellungsverwaltung");
        tabbedPane.addTab("  👥 Kunden  ", null, clientWrapper, "Kundenverwaltung");

        // Theme-safe subtle tab backgrounds (works in light + dark)
        Color tabTint = ThemeManager.isDarkMode()
                ? new Color(255, 255, 255, 16)
                : new Color(0, 0, 0, 12);

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setBackgroundAt(i, tabTint);
        }

        // Lazy loading
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            JPanel wrapper = (JPanel) tabbedPane.getComponentAt(idx);
            if (wrapper.getClientProperty("loaded") == null) {
                loadTabContent(idx, wrapper);
                wrapper.putClientProperty("loaded", Boolean.TRUE);
            }
        });

        // Load first tab immediately
        loadTabContent(0, articleWrapper);
        articleWrapper.putClientProperty("loaded", Boolean.TRUE);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // ===== FOOTER =====
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(ThemeManager.getBackgroundColor());
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel footerLabel = new JLabel("© 2026 VEBO Lagersystem | Entwickelt von Darryl Huber");
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        footerLabel.setForeground(ThemeManager.getTextSecondaryColor());
        footerPanel.add(footerLabel);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    @Override
    public void dispose() {
        ThemeManager.getInstance().unregisterWindow(this);
        super.dispose();
    }

    /**
     * Creates a wrapper panel with padding for tab content
     */
    private static JPanel createTabWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ThemeManager.getBackgroundColor());
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return wrapper;
    }

    /**
     * Loads the content for a specific tab into the wrapper.
     * IMPORTANT: We do NOT dispose the child frames. Disposing kills listeners/resources.
     */
    private void loadTabContent(int tabIndex, JPanel wrapper) {
        wrapper.removeAll();

        JFrame frame = switch (tabIndex) {
            case 0 -> {
                if (articleGUI == null) articleGUI = new ArticleGUI();
                yield articleGUI;
            }
            case 1 -> {
                if (vendorGUI == null) vendorGUI = new VendorGUI();
                yield vendorGUI;
            }
            case 2 -> {
                if (orderGUI == null) orderGUI = new OrderGUI();
                yield orderGUI;
            }
            case 3 -> {
                if (clientGUI == null) clientGUI = new ClientGUI();
                yield clientGUI;
            }
            default -> null;
        };

        if (frame == null) return;

        // Keep it from behaving like an independent window
        frame.setVisible(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        // Extract content panel (safe)
        Container cp = frame.getContentPane();
        if (cp instanceof JPanel panel) {
            wrapper.add(panel, BorderLayout.CENTER);
        } else {
            JPanel fallback = new JPanel(new BorderLayout());
            fallback.setOpaque(false);
            fallback.add(cp, BorderLayout.CENTER);
            wrapper.add(fallback, BorderLayout.CENTER);
        }

        wrapper.revalidate();
        wrapper.repaint();
    }

    private void styleHeaderButton(JButton button) {
        Color base = ThemeManager.getAccentColor();
        Color hover = ThemeManager.getButtonHoverColor(base);
        Color pressed = ThemeManager.getButtonPressedColor(base);

        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setForeground(ThemeManager.getTextOnPrimaryColor());
        button.setBackground(base);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(base.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        ));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(base);
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(pressed);
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(button.contains(evt.getPoint()) ? hover : base);
            }
        });
    }

    public void display() {
        setVisible(true);
    }
}