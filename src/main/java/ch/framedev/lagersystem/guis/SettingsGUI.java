package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.SchedulerManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Moderne Einstellungen-GUI für das VEBO Lagersystem
 */
public class SettingsGUI extends JFrame {

    private JSpinner stockCheckIntervalSpinner;
    private JSpinner warningDisplayIntervalSpinner;
    private JCheckBox enableWarningDisplayCheckbox;
    private JCheckBox enableAutoStockCheckCheckbox;
    private JTextField serverUrlField;

    public SettingsGUI() {
        setTitle("Einstellungen");
        setSize(900, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Background color
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(new Color(245, 247, 250));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 58, 95));
        headerPanel.setPreferredSize(new Dimension(0, 90));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("⚙️  Einstellungen");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Konfiguration für das Lagersystem");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(200, 220, 240));

        JPanel headerTextPanel = new JPanel();
        headerTextPanel.setLayout(new BoxLayout(headerTextPanel, BoxLayout.Y_AXIS));
        headerTextPanel.setOpaque(false);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerTextPanel.add(titleLabel);
        headerTextPanel.add(Box.createVerticalStrut(5));
        headerTextPanel.add(subtitleLabel);

        headerPanel.add(headerTextPanel, BorderLayout.WEST);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Content Area
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(new Color(245, 247, 250));
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        RoundedPanel contentCard = new RoundedPanel(Color.WHITE, 12);
        contentCard.setLayout(new BorderLayout(0, 0));
        contentCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setOpaque(false);
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // === SECTION 1: Lagerbestandsprüfung ===
        JPanel stockCheckSection = createSectionPanel("📦 Lagerbestandsprüfung",
            "Automatische Überprüfung des Lagerbestands und Warnerstellung");

        enableAutoStockCheckCheckbox = new JCheckBox("Automatische Lagerbestandsprüfung aktivieren");
        styleCheckbox(enableAutoStockCheckCheckbox);
        enableAutoStockCheckCheckbox.setSelected(true);

        JPanel intervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        intervalPanel.setOpaque(false);
        intervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        intervalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel intervalLabel = new JLabel("Prüfintervall:");
        intervalLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        intervalLabel.setForeground(new Color(70, 80, 90));

        stockCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
        stockCheckIntervalSpinner.setFont(new Font("Arial", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) stockCheckIntervalSpinner.getEditor()).getTextField().setColumns(6);
        styleSpinner(stockCheckIntervalSpinner);

        JLabel minutesLabel = new JLabel("Minuten");
        minutesLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        minutesLabel.setForeground(new Color(100, 110, 120));

        intervalPanel.add(intervalLabel);
        intervalPanel.add(Box.createHorizontalStrut(12));
        intervalPanel.add(stockCheckIntervalSpinner);
        intervalPanel.add(Box.createHorizontalStrut(10));
        intervalPanel.add(minutesLabel);

        stockCheckSection.add(Box.createVerticalStrut(18));
        stockCheckSection.add(enableAutoStockCheckCheckbox);
        stockCheckSection.add(Box.createVerticalStrut(15));
        stockCheckSection.add(intervalPanel);

        settingsPanel.add(stockCheckSection);
        settingsPanel.add(Box.createVerticalStrut(25));

        // === SECTION 2: Warnungen ===
        JPanel warningSection = createSectionPanel("⚠️ Warnungsanzeige",
            "Konfiguration der automatischen Warnanzeige");

        enableWarningDisplayCheckbox = new JCheckBox("Automatische Anzeige ungelesener Warnungen aktivieren");
        styleCheckbox(enableWarningDisplayCheckbox);
        enableWarningDisplayCheckbox.setSelected(true);

        JPanel warningIntervalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        warningIntervalPanel.setOpaque(false);
        warningIntervalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningIntervalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel warningIntervalLabel = new JLabel("Anzeigeintervall:");
        warningIntervalLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        warningIntervalLabel.setForeground(new Color(70, 80, 90));

        warningDisplayIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        warningDisplayIntervalSpinner.setFont(new Font("Arial", Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) warningDisplayIntervalSpinner.getEditor()).getTextField().setColumns(6);
        styleSpinner(warningDisplayIntervalSpinner);

        JLabel hoursLabel = new JLabel("Stunde(n)");
        hoursLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        hoursLabel.setForeground(new Color(100, 110, 120));

        warningIntervalPanel.add(warningIntervalLabel);
        warningIntervalPanel.add(Box.createHorizontalStrut(12));
        warningIntervalPanel.add(warningDisplayIntervalSpinner);
        warningIntervalPanel.add(Box.createHorizontalStrut(10));
        warningIntervalPanel.add(hoursLabel);

        JLabel warningInfoLabel = new JLabel("<html><div style='width: 700px; padding: 8px 0;'>" +
            "<i>Warnungen werden automatisch in diesem Intervall angezeigt, wenn neue ungelesene Warnungen vorhanden sind.</i>" +
            "</div></html>");
        warningInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        warningInfoLabel.setForeground(new Color(120, 130, 140));
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningInfoLabel.setMaximumSize(new Dimension(750, 60));

        warningSection.add(Box.createVerticalStrut(18));
        warningSection.add(enableWarningDisplayCheckbox);
        warningSection.add(Box.createVerticalStrut(15));
        warningSection.add(warningIntervalPanel);
        warningSection.add(Box.createVerticalStrut(10));
        warningSection.add(warningInfoLabel);

        settingsPanel.add(warningSection);
        settingsPanel.add(Box.createVerticalStrut(25));

        // === SECTION 3: Server-Einstellungen ===
        JPanel serverSection = createSectionPanel("🌐 Server-Verbindung",
            "URL des QR-Code Scan-Servers");

        JPanel serverUrlPanel = new JPanel(new BorderLayout(0, 10));
        serverUrlPanel.setOpaque(false);
        serverUrlPanel.setMaximumSize(new Dimension(750, 80));

        JLabel serverUrlLabel = new JLabel("Server URL:");
        serverUrlLabel.setFont(new Font("Arial", Font.BOLD, 13));
        serverUrlLabel.setForeground(new Color(70, 80, 90));

        serverUrlField = new JTextField("http://localhost/scan/list.php");
        serverUrlField.setFont(new Font("Arial", Font.PLAIN, 13));
        serverUrlField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        serverUrlField.setPreferredSize(new Dimension(700, 40));

        serverUrlPanel.add(serverUrlLabel, BorderLayout.NORTH);
        serverUrlPanel.add(serverUrlField, BorderLayout.CENTER);

        serverSection.add(Box.createVerticalStrut(18));
        serverSection.add(serverUrlPanel);

        settingsPanel.add(serverSection);

        // Wrap settings panel in a scroll pane for proper scrolling
        JScrollPane scrollPane = new JScrollPane(settingsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        contentCard.add(scrollPane, BorderLayout.CENTER);
        contentWrapper.add(contentCard, BorderLayout.CENTER);

        mainContainer.add(contentWrapper, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 225, 230)));

        JButton cancelButton = createStyledButton("Abbrechen", new Color(149, 165, 166), Color.BLACK);
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton("💾 Speichern", new Color(46, 204, 113), Color.BLACK);
        saveButton.addActionListener(e -> saveSettings());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(mainContainer);

        // Load current settings
        loadSettings();
    }

    /**
     * Creates a styled section panel
     */
    private JPanel createSectionPanel(String title, String description) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(true);
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 230), 1, true),
            BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));
        section.setBackground(new Color(248, 250, 252));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(30, 40, 50));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><div style='width: 750px;'>" + description + "</div></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 110, 120));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setMaximumSize(new Dimension(750, 50));

        section.add(titleLabel);
        section.add(Box.createVerticalStrut(8));
        section.add(descLabel);

        return section;
    }

    /**
     * Styles a checkbox
     */
    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(new Font("Arial", Font.PLAIN, 13));
        checkbox.setForeground(new Color(50, 60, 70));
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    }

    /**
     * Styles a spinner
     */
    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
        }
    }

    /**
     * Creates a styled button
     */
    private JButton createStyledButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(fgColor);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(bgColor.darker(), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setBorderPainted(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        Color originalBg = bgColor;
        Color hoverBg = bgColor.darker();
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
            }
        });

        return button;
    }

    /**
     * Loads settings from the system
     */
    private void loadSettings() {
        try {
            if (Main.settings != null) {
                // Load stock check interval (default 30 minutes)
                String intervalStr = Main.settings.getProperty("stock_check_interval");
                int interval = (intervalStr != null) ? Integer.parseInt(intervalStr) : 30;
                stockCheckIntervalSpinner.setValue(interval);

                // Load warning display setting (default true)
                String enableWarningsStr = Main.settings.getProperty("enable_hourly_warnings");
                boolean enableWarnings = (enableWarningsStr != null) ? Boolean.parseBoolean(enableWarningsStr) : true;
                enableWarningDisplayCheckbox.setSelected(enableWarnings);

                // Load warning display interval (default 1 hour)
                String warningIntervalStr = Main.settings.getProperty("warning_display_interval");
                int warningInterval = (warningIntervalStr != null) ? Integer.parseInt(warningIntervalStr) : 1;
                warningDisplayIntervalSpinner.setValue(warningInterval);

                // Load auto stock check setting (default true)
                String enableAutoCheckStr = Main.settings.getProperty("enable_auto_stock_check");
                boolean enableAutoCheck = (enableAutoCheckStr != null) ? Boolean.parseBoolean(enableAutoCheckStr) : true;
                enableAutoStockCheckCheckbox.setSelected(enableAutoCheck);

                // Load server URL
                String serverUrl = Main.settings.getProperty("server_url");
                if (serverUrl == null || serverUrl.trim().isEmpty()) {
                    serverUrl = "http://localhost/scan/list.php";
                }
                serverUrlField.setText(serverUrl);

                System.out.println("[SettingsGUI] Einstellungen geladen");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Laden der Einstellungen: " + e.getMessage());
        }
    }

    /**
     * Saves settings and applies them
     */
    private void saveSettings() {
        try {
            if (Main.settings != null) {
                // Save stock check interval
                int interval = (Integer) stockCheckIntervalSpinner.getValue();
                Main.settings.setProperty("stock_check_interval", String.valueOf(interval));

                // Save warning display setting
                boolean enableWarnings = enableWarningDisplayCheckbox.isSelected();
                Main.settings.setProperty("enable_hourly_warnings", String.valueOf(enableWarnings));

                // Save warning display interval
                int warningInterval = (Integer) warningDisplayIntervalSpinner.getValue();
                Main.settings.setProperty("warning_display_interval", String.valueOf(warningInterval));

                // Save auto stock check setting
                boolean enableAutoCheck = enableAutoStockCheckCheckbox.isSelected();
                Main.settings.setProperty("enable_auto_stock_check", String.valueOf(enableAutoCheck));

                // Save server URL
                String serverUrl = serverUrlField.getText().trim();
                Main.settings.setProperty("server_url", serverUrl);

                Main.settings.save();

                System.out.println("[SettingsGUI] Einstellungen gespeichert");

                // Apply settings immediately
                applySettings(interval, enableWarnings, warningInterval, enableAutoCheck);

                JOptionPane.showMessageDialog(this,
                    "<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                    "Die neuen Einstellungen wurden erfolgreich übernommen.</html>",
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE);

                dispose();
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Speichern der Einstellungen: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                "<html><b>Fehler beim Speichern!</b><br/><br/>" +
                "Die Einstellungen konnten nicht gespeichert werden:<br/>" +
                e.getMessage() + "</html>",
                "Fehler",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Applies settings to the running system
     */
    private void applySettings(int interval, boolean enableWarnings, int warningInterval, boolean enableAutoCheck) {
        try {
            SchedulerManager scheduler = SchedulerManager.getInstance();

            // Shutdown existing scheduler
            scheduler.shutdown();

            // Wait a moment for clean shutdown
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Restart scheduler with new settings
            if (enableAutoCheck) {
                scheduler.startScheduledStockCheck(interval, java.util.concurrent.TimeUnit.MINUTES);
                System.out.println("[SettingsGUI] Automatische Lagerbestandsprüfung neu gestartet (Intervall: " + interval + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatische Lagerbestandsprüfung deaktiviert");
            }

            if (enableWarnings) {
                scheduler.startWarningDisplay(warningInterval, java.util.concurrent.TimeUnit.HOURS);
                System.out.println("[SettingsGUI] Automatische Warnanzeige aktiviert (Intervall: " + warningInterval + " Stunde(n))");
            } else {
                System.out.println("[SettingsGUI] Automatische Warnanzeige deaktiviert");
            }

        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Anwenden der Einstellungen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void display() {
        setVisible(true);
    }

    /**
     * Rounded panel for modern card design
     */
    private static class RoundedPanel extends JPanel {
        private final Color backgroundColor;
        private final int radius;

        RoundedPanel(Color bg, int radius) {
            this.backgroundColor = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
