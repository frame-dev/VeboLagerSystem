package ch.framedev.lagersystem.guis;

import javax.swing.*;
import java.awt.*;

/**
 * Modern Main Dashboard for VEBO Lagersystem with Tabbed Interface
 */
public class MainGUI extends JFrame {

    private final JTabbedPane tabbedPane;
    public static ArticleGUI articleGUI;

    public MainGUI() {
        setTitle("VEBO Lagersystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Background color
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 247, 250));

        // === HEADER SECTION ===
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 58, 95)); // Dark blue
        headerPanel.setPreferredSize(new Dimension(0, 90));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 40, 15, 40));

        // Title
        JLabel titleLabel = new JLabel("VEBO Lagersystem");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        // Subtitle
        JLabel subtitleLabel = new JLabel("Zentrale Verwaltung für Artikel, Bestellungen und Lieferanten");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(200, 220, 240));
        subtitleLabel.setHorizontalAlignment(SwingConstants.LEFT);

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerTextPanel.add(subtitleLabel);

        headerPanel.add(headerTextPanel, BorderLayout.WEST);

        // Add date/time label on the right
        JLabel dateLabel = new JLabel("07. Januar 2026");
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dateLabel.setForeground(new Color(180, 200, 220));
        headerPanel.add(dateLabel, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // === TABBED PANE SECTION ===
        tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(new Color(31, 45, 61));

        // Customize tab appearance
        UIManager.put("TabbedPane.selected", new Color(30, 58, 95));
        UIManager.put("TabbedPane.contentAreaColor", new Color(245, 247, 250));

        // Create wrapper panels for each GUI to add padding
        JPanel articleWrapper = createTabWrapper();
        JPanel vendorWrapper = createTabWrapper();
        JPanel orderWrapper = createTabWrapper();
        JPanel clientWrapper = createTabWrapper();

        // Add the actual GUI content to each wrapper
        // Note: We'll initialize these lazily when the tab is first selected

        // Add tabs with icons (using emoji as simple icons)
        tabbedPane.addTab("  📦 Artikel  ", null, articleWrapper, "Artikelverwaltung");
        tabbedPane.addTab("  🚚 Lieferanten  ", null, vendorWrapper, "Lieferantenverwaltung");
        tabbedPane.addTab("  📋 Bestellungen  ", null, orderWrapper, "Bestellungsverwaltung");
        tabbedPane.addTab("  👥 Kunden  ", null, clientWrapper, "Kundenverwaltung");

        // Set custom colors for tabs
        tabbedPane.setBackgroundAt(0, new Color(66, 133, 244, 30));
        tabbedPane.setBackgroundAt(1, new Color(52, 168, 83, 30));
        tabbedPane.setBackgroundAt(2, new Color(251, 188, 5, 30));
        tabbedPane.setBackgroundAt(3, new Color(234, 67, 53, 30));

        // Lazy loading: Initialize content when tab is selected
        tabbedPane.addChangeListener(e -> {
            int selectedIndex = tabbedPane.getSelectedIndex();
            JPanel selectedWrapper = (JPanel) tabbedPane.getComponentAt(selectedIndex);

            // Check if content is already loaded
            if (selectedWrapper.getComponentCount() == 0) {
                loadTabContent(selectedIndex, selectedWrapper);
            }
        });

        // Load the first tab immediately
        loadTabContent(0, articleWrapper);

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        // === FOOTER ===
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(new Color(240, 242, 245));
        footerPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        JLabel footerLabel = new JLabel("© 2026 VEBO Lagersystem | Entwickelt von Darryl Huber");
        footerLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(120, 120, 120));
        footerPanel.add(footerLabel);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    /**
     * Creates a wrapper panel with padding for tab content
     */
    private JPanel createTabWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(245, 247, 250));
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        return wrapper;
    }

    /**
     * Lazy loads the content for a specific tab
     */
    private void loadTabContent(int tabIndex, JPanel wrapper) {
        wrapper.removeAll(); // Clear any existing content

        JPanel contentPanel = switch (tabIndex) {
            case 0 -> {
                articleGUI = new ArticleGUI();
                yield (JPanel) articleGUI.getContentPane();
            }
            case 1 -> {
                VendorGUI vendorGUI = new VendorGUI();
                yield (JPanel) vendorGUI.getContentPane();
            }
            case 2 -> {
                OrderGUI orderGUI = new OrderGUI();
                yield (JPanel) orderGUI.getContentPane();
            }
            case 3 -> {
                ClientGUI clientGUI = new ClientGUI();
                yield (JPanel) clientGUI.getContentPane();
            }
            default -> null;
        };

        if (contentPanel != null) {
            wrapper.add(contentPanel, BorderLayout.CENTER);
            wrapper.revalidate();
            wrapper.repaint();
        }
    }

    public void display() {
        setVisible(true);
    }
}
