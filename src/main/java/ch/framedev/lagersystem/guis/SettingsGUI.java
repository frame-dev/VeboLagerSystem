package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.SchedulerManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Moderne Einstellungen-GUI für das VEBO Lagersystem
 */
public class SettingsGUI extends JFrame {

    private final Logger logger = LogManager.getLogger(SettingsGUI.class);

    private final JSpinner stockCheckIntervalSpinner;
    private final JSpinner warningDisplayIntervalSpinner;
    private final JCheckBox enableWarningDisplayCheckbox;
    private final JCheckBox enableAutoStockCheckCheckbox;
    private final JTextField serverUrlField;

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

        JLabel warningInfoLabel = getWarningInfoLabel();

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

        // === SECTION 4: Datenbank-Einstellungen ===
        settingsPanel.add(Box.createVerticalStrut(25));
        JPanel databaseSection = createSectionPanel("💾 Datenbank-Einstellungen",
                "Konfiguration der Datenbank-Verbindung");
        JLabel databaseClearLabel = getDatabaseClearLabel();

        JPanel databaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        databaseButtonPanel.setOpaque(false);
        databaseButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        databaseButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton clearDatabaseButton = createStyledButton("🗑️ Datenbank Bereinigen", new Color(220, 53, 69));
        clearDatabaseButton.addActionListener(e -> clearDatabase());

        databaseButtonPanel.add(clearDatabaseButton);

        databaseSection.add(Box.createVerticalStrut(18));
        databaseSection.add(databaseClearLabel);
        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(databaseButtonPanel);

        JLabel selectedDatabaseClear = getSelectedDatabaseClear();
        databaseSection.add(Box.createVerticalStrut(10));
        databaseSection.add(selectedDatabaseClear);

        // Table selection panel
        JPanel tableSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tableSelectionPanel.setOpaque(false);
        tableSelectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableSelectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel tableLabel = new JLabel("Tabelle auswählen:");
        tableLabel.setFont(new Font("Arial", Font.BOLD, 13));
        tableLabel.setForeground(new Color(70, 80, 90));

        List<String> tableNames = new ArrayList<>(List.of("articles", "vendors", "orders", "clients", "departments", "users", "warnings"));
        JComboBox<String> tableComboBox = new JComboBox<>(tableNames.toArray(new String[0]));
        tableComboBox.setFont(new Font("Arial", Font.PLAIN, 13));
        tableComboBox.setPreferredSize(new Dimension(200, 35));
        styleComboBox(tableComboBox);

        tableSelectionPanel.add(tableLabel);
        tableSelectionPanel.add(Box.createHorizontalStrut(12));
        tableSelectionPanel.add(tableComboBox);

        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(tableSelectionPanel);

        JButton deleteSelectedTableButton = createStyledButton("🗑️ Ausgewählte Tabelle Löschen", new Color(230, 126, 34));
        deleteSelectedTableButton.addActionListener(e -> {
            String selectedTable = (String) tableComboBox.getSelectedItem();
            if (selectedTable == null || selectedTable.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie eine Tabelle aus.",
                    "Keine Tabelle ausgewählt",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                String.format("<html><b>WARNUNG: Tabelle '%s' wirklich löschen?</b><br/><br/>" +
                    "Diese Aktion kann <b>NICHT</b> rückgängig gemacht werden!<br/>" +
                    "Alle Daten in der Tabelle '%s' werden permanent gelöscht.<br/><br/>" +
                    "Möchten Sie fortfahren?</html>", selectedTable, selectedTable),
                "Tabelle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                // Second confirmation for safety
                String userInput = JOptionPane.showInputDialog(this,
                    String.format("<html>Bitte geben Sie den Tabellennamen '<b>%s</b>' ein,<br/>" +
                        "um die Löschung zu bestätigen:</html>", selectedTable),
                    "Löschung bestätigen",
                    JOptionPane.WARNING_MESSAGE);

                if (userInput != null && userInput.trim().equals(selectedTable)) {
                    deleteTable(selectedTable);
                } else if (userInput != null) {
                    JOptionPane.showMessageDialog(this,
                        "Der eingegebene Tabellenname stimmt nicht überein.\nLöschung abgebrochen.",
                        "Abgebrochen",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JPanel deleteTableButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        deleteTableButtonPanel.setOpaque(false);
        deleteTableButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        deleteTableButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        deleteTableButtonPanel.add(deleteSelectedTableButton);

        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(deleteTableButtonPanel);

        settingsPanel.add(databaseSection);

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

        JButton cancelButton = createStyledButton("Abbrechen", new Color(149, 165, 166));
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton("💾 Speichern", new Color(46, 204, 113));
        saveButton.addActionListener(e -> saveSettings());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(mainContainer);

        // Load current settings
        loadSettings();
    }

    private static JLabel getSelectedDatabaseClear() {
        JLabel selectedDatabaseClear = new JLabel("<html><div style='width: 700px; padding: 8px 0'><p>" +
                "<i>Hinweis: Diese Aktion löscht eine bestimmte Tabelle. Bitte seien Sie vorsichtig!</i>" +
                "</div></html>");
        selectedDatabaseClear.setFont(new Font("Arial", Font.PLAIN, 12));
        selectedDatabaseClear.setForeground(new Color(150, 160, 170));
        selectedDatabaseClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        selectedDatabaseClear.setMaximumSize(new Dimension(750, 50));
        return selectedDatabaseClear;
    }

    private static JLabel getDatabaseClearLabel() {
        JLabel databaseClearLabel = new JLabel("<html><div style='width: 700px; padding: 8px 0'><p>" +
                "Mit dem Knopf <b>Datenbank Bereinigen</b> werden alle Daten in der Datenbank gelöscht!" +
                "</div></html>");
        databaseClearLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        databaseClearLabel.setForeground(new Color(120, 130, 140));
        databaseClearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        databaseClearLabel.setMaximumSize(new Dimension(750, 60));
        return databaseClearLabel;
    }

    private static JLabel getWarningInfoLabel() {
        JLabel warningInfoLabel = new JLabel("<html><div style='width: 700px; padding: 8px 0;'>" +
                "<i>Warnungen werden automatisch in diesem Intervall angezeigt, wenn neue ungelesene Warnungen vorhanden sind.</i>" +
                "</div></html>");
        warningInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        warningInfoLabel.setForeground(new Color(120, 130, 140));
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        warningInfoLabel.setMaximumSize(new Dimension(750, 60));
        return warningInfoLabel;
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
     * Styles a combobox
     */
    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Arial", Font.PLAIN, 13));
        comboBox.setBackground(Color.WHITE);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 220), 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    /**
     * Creates a styled button
     */
    private JButton createStyledButton(String text, Color originalBg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(originalBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(originalBg.darker(), 1),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setFocusPainted(false);
        button.setOpaque(true); // Fixed: Changed to true so button background is visible
        button.setBorderPainted(true);
        button.setContentAreaFilled(true); // Ensure button fills with background color
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover effect
        Color hoverBg = originalBg.darker();
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
                boolean enableWarnings = enableWarningsStr == null || Boolean.parseBoolean(enableWarningsStr);
                enableWarningDisplayCheckbox.setSelected(enableWarnings);

                // Load warning display interval (default 1 hour)
                String warningIntervalStr = Main.settings.getProperty("warning_display_interval");
                int warningInterval = (warningIntervalStr != null) ? Integer.parseInt(warningIntervalStr) : 1;
                warningDisplayIntervalSpinner.setValue(warningInterval);

                // Load auto stock check setting (default true)
                String enableAutoCheckStr = Main.settings.getProperty("enable_auto_stock_check");
                boolean enableAutoCheck = enableAutoCheckStr == null || Boolean.parseBoolean(enableAutoCheckStr);
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
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.icon);

                dispose();
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Speichern der Einstellungen: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Speichern!</b><br/><br/>" +
                            "Die Einstellungen konnten nicht gespeichert werden:<br/>" +
                            e.getMessage() + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
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
            logger.error("Fehler beim Anwenden der Einstellungen", e);
        }
    }

    public void display() {
        setVisible(true);
    }

    /**
     * Deletes a specific table from the database
     */
    private void deleteTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return;
        }

        try {
            ch.framedev.lagersystem.managers.DatabaseManager dbManager =
                    ch.framedev.lagersystem.main.Main.databaseManager;

            if (dbManager != null) {
                // Execute DROP TABLE command
                String dropTableSQL = "DROP TABLE IF EXISTS " + tableName + ";";
                boolean success = dbManager.executeUpdate(dropTableSQL);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            String.format("<html><b>✓ Tabelle erfolgreich gelöscht</b><br/><br/>" +
                                    "Die Tabelle '<b>%s</b>' wurde aus der Datenbank entfernt.<br/><br/>" +
                                    "<i>Hinweis: Die zugehörigen Daten sind permanent gelöscht.</i></html>",
                                    tableName),
                            "Erfolgreich",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);

                    System.out.printf("[SettingsGUI] Tabelle '%s' wurde erfolgreich gelöscht%n", tableName);

                    // Show restart recommendation
                    int restart = JOptionPane.showConfirmDialog(this,
                            "<html>Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                    "um Inkonsistenzen zu vermeiden.<br/><br/>" +
                                    "Möchten Sie jetzt neu starten?</html>",
                            "Neustart empfohlen",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            Main.icon);

                    if (restart == JOptionPane.YES_OPTION) {
                        System.exit(0);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                            String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                    "Die Tabelle '<b>%s</b>' konnte nicht gelöscht werden.<br/>" +
                                    "Bitte überprüfen Sie die Logs für weitere Details.</html>",
                                    tableName),
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.icon);
            }
        } catch (Exception e) {
            System.err.printf("[SettingsGUI] Fehler beim Löschen der Tabelle '%s': %s%n",
                    tableName, e.getMessage());
            logger.error("Fehler beim Löschen der Tabelle '{}'", tableName, e);
            JOptionPane.showMessageDialog(this,
                    String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                            "Tabelle: %s<br/>" +
                            "Fehler: %s</html>",
                            tableName, e.getMessage()),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
        }
    }

    /**
     * Clears the database after user confirmation
     */
    private void clearDatabase() {
        // First confirmation
        int firstConfirm = JOptionPane.showConfirmDialog(this,
                "<html><b>⚠️ WARNUNG: Datenbank löschen</b><br/><br/>" +
                        "Möchten Sie wirklich <b>ALLE DATEN</b> aus der Datenbank löschen?<br/><br/>" +
                        "Dies umfasst:<br/>" +
                        "• Alle Artikel<br/>" +
                        "• Alle Lieferanten<br/>" +
                        "• Alle Bestellungen<br/>" +
                        "• Alle Kunden<br/>" +
                        "• Alle Abteilungen<br/>" +
                        "• Alle Benutzer<br/>" +
                        "• Alle Warnungen<br/><br/>" +
                        "<b>Diese Aktion kann NICHT rückgängig gemacht werden!</b></html>",
                "Datenbank löschen - Bestätigung 1/2",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                Main.icon);

        if (firstConfirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Second confirmation (extra safety)
        String confirmText = JOptionPane.showInputDialog(this,
                "<html><b>Zweite Bestätigung erforderlich</b><br/><br/>" +
                        "Bitte geben Sie <b>LÖSCHEN</b> ein, um fortzufahren:</html>",
                "Datenbank löschen - Bestätigung 2/2",
                JOptionPane.WARNING_MESSAGE);

        if (confirmText == null || !confirmText.trim().equalsIgnoreCase("LÖSCHEN")) {
            JOptionPane.showMessageDialog(this,
                    "Vorgang abgebrochen. Die Datenbank wurde nicht gelöscht.",
                    "Abgebrochen",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);
            return;
        }

        // Perform database clearing
        try {
            ch.framedev.lagersystem.managers.DatabaseManager dbManager =
                    ch.framedev.lagersystem.main.Main.databaseManager;

            if (dbManager != null) {
                dbManager.clearDatabase();
                File file = new File(Main.getAppDataDir(), "own_use_list.txt");
                if (!file.delete())
                    System.out.println("[SettingsGUI] own_use_list.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                File importQrcodesFile = new File(Main.getAppDataDir(), "imported_qrcodes.txt");
                if (!importQrcodesFile.delete())
                    System.out.println("[SettingsGUI] imported_qrcodes.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");
                File importedItemsFile = new File(Main.getAppDataDir(), "imported_items.txt");
                if (!importedItemsFile.delete())
                    System.out.println("[SettingsGUI] imported_items.txt konnte nicht gelöscht werden (Datei existiert möglicherweise nicht)");

                JOptionPane.showMessageDialog(this,
                        "<html><b>✓ Datenbank erfolgreich gelöscht</b><br/><br/>" +
                                "Alle Daten wurden aus der Datenbank entfernt.<br/><br/>" +
                                "Das Programm sollte nun neu gestartet werden.</html>",
                        "Erfolgreich",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.icon);

                System.out.println("[SettingsGUI] Datenbank wurde erfolgreich bereinigt");

                // Ask if user wants to restart
                int restart = JOptionPane.showConfirmDialog(this,
                        "Möchten Sie das Programm jetzt neu starten?",
                        "Neustart empfohlen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.icon);

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.icon);
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Löschen der Datenbank: " + e.getMessage());
            logger.error("Fehler beim Löschen der Datenbank", e);
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Löschen der Datenbank</b><br/><br/>" +
                            "Fehler: " + e.getMessage() + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
        }
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
