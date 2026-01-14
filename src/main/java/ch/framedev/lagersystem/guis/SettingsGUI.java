package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        settingsPanel.add(Box.createVerticalStrut(15));

        JPanel importExportSection = createSectionPanel("⬆️⬇️ Import & Export",
                "Datenimport und -export Funktionen");
        importExportSection.add(Box.createVerticalStrut(15));

        JLabel importLabel = new JLabel("Import:");
        importLabel.setFont(new Font("Arial", Font.BOLD, 14));
        importLabel.setForeground(new Color(30, 40, 50));
        importLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportSection.add(importLabel);
        importExportSection.add(Box.createVerticalStrut(5));
        JLabel importDesc = new JLabel("<html><div style='width: 750px;'>" +
                "Importieren Sie Artikel, Lieferanten oder andere Daten aus CSV-Dateien.<br/>" +
                "Stellen Sie sicher, dass die CSV-Dateien das richtige Format haben.</div></html>");
        importDesc.setFont(new Font("Arial", Font.PLAIN, 12));
        importDesc.setForeground(new Color(100, 110, 120));
        importDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        importDesc.setMaximumSize(new Dimension(750, 50));
        importExportSection.add(importDesc);
        importExportSection.add(Box.createVerticalStrut(10));
        JButton importButton = new JButton("Importieren von CSV-Datei");
        importButton = createStyledButton("⬆️ Datenbank Importieren", new Color(39, 174, 96));
        importButton.addActionListener(e -> {
            importFromCsv();
        });
        importExportSection.add(importButton);
        importExportSection.add(Box.createVerticalStrut(20));


        JLabel exportLabel = new JLabel("Export:");
        exportLabel.setFont(new Font("Arial", Font.BOLD, 14));
        exportLabel.setForeground(new Color(30, 40, 50));
        exportLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        importExportSection.add(Box.createVerticalStrut(15));
        importExportSection.add(exportLabel);
        importExportSection.add(Box.createVerticalStrut(5));

        JLabel exportDesc = new JLabel("<html><div style='width: 750px;'>" +
                "Exportieren Sie Ihre Datenbankinhalte in CSV-Dateien zur Sicherung oder Weiterverarbeitung.</div></html>");
        exportDesc.setFont(new Font("Arial", Font.PLAIN, 12));
        exportDesc.setForeground(new Color(100, 110, 120));
        exportDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportDesc.setMaximumSize(new Dimension(750, 50));
        importExportSection.add(exportDesc);
        importExportSection.add(Box.createVerticalStrut(10));
        JButton exportButton = createStyledButton("⬇️ Datenbank Exportieren", new Color(52, 152, 219));
        exportButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "<html><b>Datenbank nach CSV exportieren?</b><br/><br/>" +
                            "Dies erstellt CSV-Dateien für:<br/>" +
                            "• Artikel (articles_export.csv)<br/>" +
                            "• Lieferanten (vendors_export.csv)<br/>" +
                            "• Kunden (clients_export.csv)<br/>" +
                            "• Bestellungen (orders_export.csv)<br/><br/>" +
                            "Speicherort: " + Main.getAppDataDir().getAbsolutePath() + "</html>",
                    "Exportieren bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.icon);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    exportToCsv();
                    JOptionPane.showMessageDialog(this,
                            "<html><b>✓ Export erfolgreich!</b><br/><br/>" +
                                    "Alle Tabellen wurden erfolgreich exportiert.<br/>" +
                                    "Speicherort: <br/>" +
                                    Main.getAppDataDir().getAbsolutePath() + "</html>",
                            "Export erfolgreich",
                            JOptionPane.INFORMATION_MESSAGE,
                            Main.icon);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "<html><b>✗ Fehler beim Export!</b><br/><br/>" +
                                    ex.getMessage() + "</html>",
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.icon);
                }
            }
        });
        importExportSection.add(exportButton);

        settingsPanel.add(importExportSection);

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
     * Creates a modern styled section panel with card-like appearance
     */
    private JPanel createSectionPanel(String title, String description) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setOpaque(true);
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(220, 225, 230), 1, true),
                        // Subtle inner shadow effect
                        BorderFactory.createMatteBorder(1, 1, 0, 0, new Color(255, 255, 255, 180))
                ),
                BorderFactory.createEmptyBorder(24, 26, 24, 26)
        ));
        section.setBackground(new Color(248, 250, 252));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));

        // Title with icon
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 17));
        titleLabel.setForeground(new Color(30, 40, 50));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Description with better formatting
        JLabel descLabel = new JLabel("<html><div style='width: 750px; line-height: 1.5;'>" +
                description + "</div></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(new Color(100, 110, 120));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setMaximumSize(new Dimension(750, 60));

        section.add(titleLabel);
        section.add(Box.createVerticalStrut(10));
        section.add(descLabel);

        return section;
    }

    /**
     * Styles a checkbox with modern appearance and enhanced click area
     */
    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(new Font("Arial", Font.PLAIN, 14));
        checkbox.setForeground(new Color(50, 60, 70));
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        checkbox.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        // Add hover effect for better UX
        checkbox.addMouseListener(new MouseAdapter() {
            private final Color originalForeground = checkbox.getForeground();
            private final Color hoverForeground = new Color(30, 40, 50);

            @Override
            public void mouseEntered(MouseEvent e) {
                checkbox.setForeground(hoverForeground);
                checkbox.setFont(checkbox.getFont().deriveFont(Font.BOLD));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkbox.setForeground(originalForeground);
                checkbox.setFont(checkbox.getFont().deriveFont(Font.PLAIN));
            }
        });
    }

    /**
     * Styles a spinner with modern, rounded appearance and focus effects
     */
    private void styleSpinner(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setFont(new Font("Arial", Font.PLAIN, 13));
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                    BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            textField.setBackground(new Color(250, 251, 252));

            // Add focus listener for better UX
            textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true),
                            BorderFactory.createEmptyBorder(7, 11, 7, 11)
                    ));
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                            BorderFactory.createEmptyBorder(8, 12, 8, 12)
                    ));
                }
            });
        }

        // Style the spinner itself
        spinner.setBorder(BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true));
        spinner.setBackground(new Color(250, 251, 252));
    }

    /**
     * Styles a combobox with modern appearance and rounded borders
     */
    private void styleComboBox(JComboBox<?> comboBox) {
        comboBox.setFont(new Font("Arial", Font.PLAIN, 13));
        comboBox.setBackground(new Color(250, 251, 252));
        comboBox.setForeground(new Color(50, 60, 70));
        comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        comboBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Set preferred height for consistency
        comboBox.setPreferredSize(new Dimension(
                comboBox.getPreferredSize().width,
                40
        ));
    }

    /**
     * Creates a modern styled button with smooth hover effects and professional appearance
     */
    private JButton createStyledButton(String text, Color originalBg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(Color.WHITE);
        button.setBackground(originalBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)
        ));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Set consistent height
        button.setPreferredSize(new Dimension(
                button.getPreferredSize().width,
                44
        ));

        // Enhanced hover effect with smooth color transitions
        Color hoverBg = new Color(
                Math.max(0, originalBg.getRed() - 20),
                Math.max(0, originalBg.getGreen() - 20),
                Math.max(0, originalBg.getBlue() - 20)
        );
        Color pressedBg = new Color(
                Math.max(0, originalBg.getRed() - 40),
                Math.max(0, originalBg.getGreen() - 40),
                Math.max(0, originalBg.getBlue() - 40)
        );

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2, true),
                        BorderFactory.createEmptyBorder(11, 23, 11, 23)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                        BorderFactory.createEmptyBorder(12, 24, 12, 24)
                ));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(pressedBg);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (button.contains(e.getPoint())) {
                    button.setBackground(hoverBg);
                } else {
                    button.setBackground(originalBg);
                }
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

    private static void importFromCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV-Datei zum Importieren auswählen");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV-Dateien", "csv"));
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setCurrentDirectory(Main.getAppDataDir());

        int result = fileChooser.showOpenDialog(null);
        if (result != JFileChooser.APPROVE_OPTION) {
            System.out.println("[SettingsGUI] Import abgebrochen");
            return;
        }

        File selectedFile = fileChooser.getSelectedFile();
        String fileName = selectedFile.getName().toLowerCase();

        // Determine file type based on filename
        if (fileName.contains("article")) {
            importArticlesFromCsv(selectedFile);
        } else if (fileName.contains("vendor") || fileName.contains("supplier")) {
            importVendorsFromCsv(selectedFile);
        } else if (fileName.contains("client") || fileName.contains("customer")) {
            importClientsFromCsv(selectedFile);
        } else if (fileName.contains("order")) {
            importOrdersFromCsv(selectedFile);
        } else {
            // Ask user what type of data this is
            String[] options = {"Artikel", "Lieferanten", "Kunden", "Bestellungen", "Abbrechen"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Welche Art von Daten möchten Sie importieren?",
                    "Datentyp auswählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.icon,
                    options,
                    options[0]);

            switch (choice) {
                case 0 -> importArticlesFromCsv(selectedFile);
                case 1 -> importVendorsFromCsv(selectedFile);
                case 2 -> importClientsFromCsv(selectedFile);
                case 3 -> importOrdersFromCsv(selectedFile);
                default -> System.out.println("[SettingsGUI] Import abgebrochen");
            }
        }
    }

    private static void importArticlesFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.icon);
                return;
            }

            ArticleManager articleManager = ArticleManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 8) {
                        errors++;
                        continue;
                    }

                    String articleNumber = parts[0];
                    String name = parts[1];
                    String details = parts[2];
                    int stockQuantity = Integer.parseInt(parts[3]);
                    int minStockLevel = Integer.parseInt(parts[4]);
                    double sellPrice = Double.parseDouble(parts[5]);
                    double purchasePrice = Double.parseDouble(parts[6]);
                    String vendorName = parts[7];

                    Article article = new Article(articleNumber, name, details,
                            stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName);

                    if (articleManager.existsArticle(articleNumber)) {
                        if (articleManager.updateArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (articleManager.insertArticle(article)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Artikel-Import abgeschlossen</b><br/><br/>" +
                            "✅ Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? "❌ Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);

            System.out.printf("[SettingsGUI] Artikel-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Artikel: " + e.getMessage());
        }
    }

    private static void importVendorsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.icon);
                return;
            }

            VendorManager vendorManager = VendorManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 5) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String contactPerson = parts[1];
                    String phoneNumber = parts[2];
                    String email = parts[3];
                    String address = parts[4];

                    Vendor vendor = new Vendor(name, contactPerson, phoneNumber, email, address, new ArrayList<>());

                    if (vendorManager.existsVendor(name)) {
                        String[] columns = {"contactPerson", "phoneNumber", "email", "address"};
                        Object[] values = {contactPerson, phoneNumber, email, address};
                        if (vendorManager.updateVendor(name, columns, values)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (vendorManager.insertVendor(vendor)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Lieferanten-Import abgeschlossen</b><br/><br/>" +
                            "✅ Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? "❌ Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);

            System.out.printf("[SettingsGUI] Lieferanten-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Lieferanten: " + e.getMessage());
        }
    }

    private static void importClientsFromCsv(File csvFile) {
        int imported = 0;
        int errors = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine(); // Skip header
            if (headerLine == null) {
                JOptionPane.showMessageDialog(null,
                        "Die CSV-Datei ist leer.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.icon);
                return;
            }

            ClientManager clientManager = ClientManager.getInstance();
            String line;

            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 2) {
                        errors++;
                        continue;
                    }

                    String name = parts[0];
                    String department = parts[1];

                    if (clientManager.existsClient(name)) {
                        if (clientManager.updateClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    } else {
                        if (clientManager.insertClient(name, department)) {
                            imported++;
                        } else {
                            errors++;
                        }
                    }
                } catch (Exception e) {
                    errors++;
                    System.err.println("[SettingsGUI] Fehler beim Importieren einer Zeile: " + e.getMessage());
                }
            }

            JOptionPane.showMessageDialog(null,
                    String.format("<html><b>Kunden-Import abgeschlossen</b><br/><br/>" +
                            "✅ Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? "❌ Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.icon);

            System.out.printf("[SettingsGUI] Kunden-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.icon);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Kunden: " + e.getMessage());
        }
    }

    private static void importOrdersFromCsv(File csvFile) {
        JOptionPane.showMessageDialog(null,
                "<html><b>Bestellungs-Import nicht verfügbar</b><br/><br/>" +
                        "Der Import von Bestellungen ist aus Sicherheitsgründen deaktiviert.<br/>" +
                        "Bestellungen sollten nur über die normale Bestellfunktion erstellt werden.</html>",
                "Nicht verfügbar",
                JOptionPane.INFORMATION_MESSAGE,
                Main.icon);
        System.out.println("[SettingsGUI] Bestellungs-Import wurde übersprungen (nicht implementiert aus Sicherheitsgründen)");
    }

    /**
     * Parses a CSV line, handling quoted fields with commas
     */
    private static String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Exports all database tables to CSV files in the application data directory.
     * Creates separate CSV files for articles, vendors, clients, and orders.
     */
    private static void exportToCsv() {
        int successCount = 0;
        int totalTables = 4;

        // Export Articles
        List<Article> articles = ArticleManager.getInstance().getAllArticles();
        File csvFile = new File(Main.getAppDataDir(), "articles_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFile))) {
            writer.println("Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant");
            for (Article article : articles) {
                writer.printf("\"%s\",\"%s\",\"%s\",%d,%d,%.2f,%.2f,\"%s\"%n",
                        escapeCSV(article.getArticleNumber()),
                        escapeCSV(article.getName()),
                        escapeCSV(article.getDetails()),
                        article.getStockQuantity(),
                        article.getMinStockLevel(),
                        article.getSellPrice(),
                        article.getPurchasePrice(),
                        escapeCSV(article.getVendorName()));
            }
            System.out.println("[SettingsGUI] Artikel erfolgreich nach " + csvFile.getAbsolutePath() + " exportiert (" + articles.size() + " Einträge)");
            successCount++;
        } catch (Exception e) {
            String errorMsg = "Fehler beim Exportieren der Artikel: " + e.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, e);
        }

        // Export Vendors
        List<Vendor> vendors = VendorManager.getInstance().getVendors();
        File vendorCsvFile = new File(Main.getAppDataDir(), "vendors_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(vendorCsvFile))) {
            writer.println("Name,Kontaktperson,Telefon,E-Mail,Adresse");
            for (Vendor vendor : vendors) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(vendor.getName()),
                        escapeCSV(vendor.getContactPerson()),
                        escapeCSV(vendor.getPhoneNumber()),
                        escapeCSV(vendor.getEmail()),
                        escapeCSV(vendor.getAddress()));
            }
            System.out.println("[SettingsGUI] Lieferanten erfolgreich nach " + vendorCsvFile.getAbsolutePath() + " exportiert (" + vendors.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Lieferanten: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
        }

        // Export Clients
        List<Map<String, String>> clients = ClientManager.getInstance().getAllClients();
        File clientCsvFile = new File(Main.getAppDataDir(), "clients_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(clientCsvFile))) {
            writer.println("Name,Abteilung");
            for (Map<String, String> clientMap : clients) {
                String name = clientMap.getOrDefault("firstLastName", "");
                String department = clientMap.getOrDefault("department", "");
                writer.printf("\"%s\",\"%s\"%n", escapeCSV(name), escapeCSV(department));
            }
            System.out.println("[SettingsGUI] Kunden erfolgreich nach " + clientCsvFile.getAbsolutePath() + " exportiert (" + clients.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Kunden: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
        }

        // Export Orders
        List<Order> orders = OrderManager.getInstance().getOrders();
        File orderCsvFile = new File(Main.getAppDataDir(), "orders_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(orderCsvFile))) {
            writer.println("Bestell-ID,Empfängername,EmpfängerKontoNummer,SenderName,SenderKontoNummer,Artikel,Bestelldatum,Status,Abteilung");
            for (Order order : orders) {
                // Format ordered articles as "ArticleNum1:Qty1;ArticleNum2:Qty2;..."
                String articlesStr = order.getOrderedArticles().entrySet().stream()
                        .map(e -> e.getKey() + ":" + e.getValue())
                        .collect(java.util.stream.Collectors.joining(";"));

                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(order.getOrderId()),
                        escapeCSV(order.getReceiverName()),
                        escapeCSV(order.getReceiverKontoNumber()),
                        escapeCSV(order.getSenderName()),
                        escapeCSV(order.getSenderKontoNumber()),
                        escapeCSV(articlesStr),
                        escapeCSV(order.getOrderDate()),
                        escapeCSV(order.getStatus()),
                        escapeCSV(order.getDepartment()));
            }
            System.out.println("[SettingsGUI] Bestellungen erfolgreich nach " + orderCsvFile.getAbsolutePath() + " exportiert (" + orders.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Bestellungen: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
        }

        // Show summary
        System.out.println("[SettingsGUI] CSV-Export abgeschlossen: " + successCount + "/" + totalTables + " Tabellen erfolgreich exportiert");
        System.out.println("[SettingsGUI] Dateien gespeichert in: " + Main.getAppDataDir().getAbsolutePath());
    }

    /**
     * Helper method to escape special characters in CSV values (quotes, commas, newlines)
     */
    private static String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        // Escape quotes by doubling them and remove any existing outer quotes
        return value.replace("\"", "\"\"");
    }

    private class Client {
        private final String firstLastName, department;

        public Client(String firstLastName, String department) {
            this.firstLastName = firstLastName;
            this.department = department;
        }

        public String getFirstLastName() {
            return firstLastName;
        }

        public String getDepartment() {
            return department;
        }
    }
}
