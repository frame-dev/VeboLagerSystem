package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UnicodeSymbols;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ch.framedev.lagersystem.main.Main.databaseManager;

/**
 * Moderne Einstellungen-GUI für das VEBO Lagersystem
 */
public class SettingsGUI extends JFrame {

    private final Logger logger = LogManager.getLogger(SettingsGUI.class);

    private final JSpinner stockCheckIntervalSpinner;
    private final JSpinner warningDisplayIntervalSpinner;
    private final JCheckBox enableWarningDisplayCheckbox;
    private final JCheckBox enableAutoStockCheckCheckbox;
    private final JCheckBox automaticImportCheckBox;
    private final JCheckBox darkModeCheckbox;
    private final JSpinner qrCodeImportIntervalSpinner;
    private final JSpinner fontSizeTableSpinner;
    private final JSpinner fontSizeTabSpinner;
    private final JTextField serverUrlField;
    private final JComboBox<String> themeComboBox;
    private static JComboBox<String> fontComboBox;

    public static int TABLE_FONT_SIZE = 16;

    public SettingsGUI() {
        setTitle("Einstellungen");
        setSize(1200, 850);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Set window icon if available
        if (Main.iconSmall != null) {
            setIconImage(Main.iconSmall.getImage());
        }

        // Background color
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(ThemeManager.getBackgroundColor());

        // Header Panel with enhanced gradient effect
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (g instanceof Graphics2D g2d) {
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    Color color1 = ThemeManager.getPrimaryColor();
                    Color color2 = new Color(
                            Math.max(0, color1.getRed() - 10),
                            Math.max(0, color1.getGreen() - 10),
                            Math.max(0, color1.getBlue() - 10)
                    );
                    GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setPreferredSize(new Dimension(0, 100));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        JLabel titleLabel = new JLabel(UnicodeSymbols.BETTER_GEAR + "  Einstellungen");
        titleLabel.setFont(getFontByName(Font.BOLD, 28));
        titleLabel.setForeground(ThemeManager.getTextOnPrimaryColor());

        JLabel subtitleLabel = new JLabel("Konfiguration und Verwaltung des Lagersystems");
        subtitleLabel.setFont(getFontByName(Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(255, 255, 255, 200));

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

        // Content Area with Tabbed Categories
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.setBackground(ThemeManager.getBackgroundColor());
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));

        // Create tabbed pane for categories
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.LEFT);
        tabbedPane.setFont(getFontByName(Font.BOLD, 14));
        tabbedPane.setBackground(ThemeManager.getCardBackgroundColor());
        tabbedPane.setForeground(ThemeManager.getTextPrimaryColor());
        tabbedPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // === CATEGORY 1: System & Automatisierung ===
        JPanel systemPanel = createCategoryPanel();
        JScrollPane systemScroll = createScrollablePanel(systemPanel);

        // Stock Check Section
        JPanel stockCheckSection = createSectionPanel(UnicodeSymbols.PACKAGE + " Lagerbestandsprüfung",
                "Automatische Überprüfung des Lagerbestands und Warnerstellung");

        enableAutoStockCheckCheckbox = new JCheckBox("Automatische Lagerbestandsprüfung aktivieren");
        styleCheckbox(enableAutoStockCheckCheckbox);
        enableAutoStockCheckCheckbox.setSelected(true);

        stockCheckIntervalSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 1440, 5));
        JPanel intervalPanel = createLabeledSpinnerPanel("Prüfintervall:", stockCheckIntervalSpinner, "Minuten", 6);

        stockCheckSection.add(Box.createVerticalStrut(18));
        stockCheckSection.add(enableAutoStockCheckCheckbox);
        stockCheckSection.add(Box.createVerticalStrut(15));
        stockCheckSection.add(intervalPanel);

        systemPanel.add(stockCheckSection);
        systemPanel.add(Box.createVerticalStrut(25));

        // === SECTION 2: Warnungen ===
        JPanel warningSection = createSectionPanel(UnicodeSymbols.WARNING + " Warnungsanzeige",
                "Konfiguration der automatischen Warnanzeige");

        enableWarningDisplayCheckbox = new JCheckBox("Automatische Anzeige ungelesener Warnungen aktivieren");
        styleCheckbox(enableWarningDisplayCheckbox);
        enableWarningDisplayCheckbox.setSelected(true);

        warningDisplayIntervalSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        JPanel warningIntervalPanel = createLabeledSpinnerPanel("Anzeigeintervall:", warningDisplayIntervalSpinner, "Stunde(n)", 6);

        JLabel warningInfoLabel = getWarningInfoLabel();

        warningSection.add(Box.createVerticalStrut(18));
        warningSection.add(enableWarningDisplayCheckbox);
        warningSection.add(Box.createVerticalStrut(15));
        warningSection.add(warningIntervalPanel);
        warningSection.add(Box.createVerticalStrut(10));
        warningSection.add(warningInfoLabel);

        systemPanel.add(warningSection);

        systemPanel.add(Box.createVerticalStrut(25));

        // QR-Code Section
        JPanel qrCodeOptionsPanel = createSectionPanel(UnicodeSymbols.PHONE + " QR-Code Import",
                "Automatischer Import von QR-Code Scans");

        automaticImportCheckBox = new JCheckBox("Automatisches Importieren von QR-Codes aktivieren");
        styleCheckbox(automaticImportCheckBox);
        automaticImportCheckBox.setSelected(false);

        qrCodeOptionsPanel.add(Box.createVerticalStrut(18));
        qrCodeOptionsPanel.add(automaticImportCheckBox);
        qrCodeOptionsPanel.add(Box.createVerticalStrut(15));

        qrCodeImportIntervalSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 60, 5));
        JPanel qrIntervalPanel = createLabeledSpinnerPanel("Import-Intervall:", qrCodeImportIntervalSpinner, "Minuten", 6);

        qrCodeOptionsPanel.add(qrIntervalPanel);

        systemPanel.add(qrCodeOptionsPanel);

        systemPanel.add(Box.createVerticalStrut(25));


        JPanel fontSettingsPanel = createSectionPanel(UnicodeSymbols.ABC + " Schriftart Einstellungen",
                "Passen Sie die Schriftart und -größe der Anwendung an");

        JLabel fontTableInfoLabel = new JLabel(
                "<html><p style='font-size: 12px;'>Diese Einstellung wird verwendet um die Schriftgrösse in den Tabellen zu ändern!</p>"
        );
        fontTableInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        fontTableInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        fontTableInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSettingsPanel.add(Box.createVerticalStrut(18));
        fontSettingsPanel.add(fontTableInfoLabel);
        fontSettingsPanel.add(Box.createVerticalStrut(10));

        fontSizeTableSpinner = new JSpinner(new SpinnerNumberModel(16, 10, 44, 1));
        JPanel fontSizePanel = createLabeledSpinnerPanel("Tabellen-Schriftgröße:", fontSizeTableSpinner, "px", 4);

        fontSettingsPanel.add(fontSizePanel);
        fontSettingsPanel.add(Box.createVerticalStrut(15));

        JLabel fontTabInfoLabel = new JLabel(
                "<html><p style='font-size: 12px;'>Diese Einstellung wird verwendet um die Schriftgrösse in den Tabs zu ändern!</p></html>"
        );
        fontTabInfoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        fontTabInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        fontTabInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSettingsPanel.add(Box.createVerticalStrut(18));
        fontSettingsPanel.add(fontTabInfoLabel);
        fontSettingsPanel.add(Box.createVerticalStrut(10));

        fontSizeTabSpinner = new JSpinner(new SpinnerNumberModel(15, 10, 40, 1));
        JPanel fontTabSizePanel = createLabeledSpinnerPanel("Tabs-Schriftgröße:", fontSizeTabSpinner, "px", 4);
        fontSettingsPanel.add(fontTabSizePanel);

        JLabel fontInfoLabel = new JLabel(
                "<html><p style='font-size: 12px;'>Diese Einstellung wird verwendet um die Schriftart zu ändern!</p></html>"
        );
        fontInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        fontInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        fontInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fontSettingsPanel.add(Box.createVerticalStrut(15));
        fontSettingsPanel.add(fontInfoLabel);
        fontSettingsPanel.add(Box.createVerticalStrut(10));

        fontComboBox = new JComboBox<>();
        getAllFonts().forEach(fontComboBox::addItem);
        styleComboBox(fontComboBox);
        JPanel fontSelectionPanel = createLabeledComboBoxPanel("Schriftart auswählen:", fontComboBox);
        fontSettingsPanel.add(fontSelectionPanel);

        systemPanel.add(fontSettingsPanel);

        systemPanel.add(Box.createVerticalStrut(25));

        // Dark Mode / Theme Section
        JPanel themeSection = createSectionPanel(UnicodeSymbols.COLOR_PALETTE + " Design & Darstellung",
                "Passen Sie das Erscheinungsbild der Anwendung an");

        darkModeCheckbox = new JCheckBox("Dark Mode aktivieren");
        styleCheckbox(darkModeCheckbox);
        darkModeCheckbox.setSelected(ThemeManager.isDarkMode());

        themeSection.add(Box.createVerticalStrut(18));
        themeSection.add(darkModeCheckbox);
        themeSection.add(Box.createVerticalStrut(15));

        String[] themes = {"Light", "Dark"};
        themeComboBox = new JComboBox<>(themes);
        styleComboBox(themeComboBox);
        themeComboBox.setSelectedItem(ThemeManager.isDarkMode() ? "Dark" : "Light");

        JPanel themeSelectionPanel = createLabeledComboBoxPanel("Design-Schema:", themeComboBox);

        // Add listeners after both components are initialized
        darkModeCheckbox.addActionListener(e -> {
            boolean isDark = darkModeCheckbox.isSelected();
            themeComboBox.setEnabled(!isDark);
            if (isDark) {
                themeComboBox.setSelectedItem("Dark");
            }
        });

        themeComboBox.addActionListener(e -> {
            String selected = (String) themeComboBox.getSelectedItem();
            darkModeCheckbox.setSelected("Dark".equals(selected));
        });


        themeSection.add(themeSelectionPanel);
        themeSection.add(Box.createVerticalStrut(15));

        // Theme Preview/Info
        JLabel themeInfoLabel = new JLabel("<html><div style='padding: 8px 0;'>" +
                "<i>Hinweis: Nach dem Speichern werden alle Fenster mit dem neuen Design aktualisiert.<br/>" +
                "Der Dark Mode schont die Augen bei Arbeiten in dunkler Umgebung.</i>" +
                "</div></html>");
        themeInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        themeInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        themeInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        themeSection.add(themeInfoLabel);

        systemPanel.add(themeSection);

        systemPanel.add(Box.createVerticalGlue());
        systemPanel.add(Box.createVerticalStrut(25));

        JPanel otherSection = createSectionPanel("Sonstiges", "Allgemeine Einstellungen");
        otherSection.add(Box.createVerticalStrut(18));

        JLabel openSettingsLabel = createInfoLabel("Öffnet den Ordner, in dem die Einstellungsdateien gespeichert sind.");
        otherSection.add(openSettingsLabel);
        otherSection.add(Box.createVerticalStrut(10));
        JButton openSettingsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Einstellungen-Ordner öffnen", new Color(52, 152, 219));
        openSettingsFolderButton.addActionListener(e -> openSettingsFolder());
        otherSection.add(openSettingsFolderButton);

        JLabel logsLabel = createInfoLabel("Öffnet den Ordner, in dem die Anwendungsprotokolle gespeichert sind.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsLabel);
        otherSection.add(Box.createVerticalStrut(10));
        JButton openLogsFolderButton = createStyledButton(UnicodeSymbols.FOLDER + " Protokolle-Ordner öffnen", new Color(52, 152, 219));
        openLogsFolderButton.addActionListener(e -> openLogsFolder());
        otherSection.add(openLogsFolderButton);
        otherSection.add(Box.createVerticalStrut(15));

        JLabel logsDelete = createInfoLabel("Löscht alle Anwendungsprotokolle aus dem Protokolle-Ordner. So wie in der Datenbank.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsDelete);
        JButton deleteLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alle Protokolle löschen", new Color(220, 53, 69));
        deleteLogsButton.addActionListener(e -> deleteAllLogs());
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteLogsButton);

        otherSection.add(Box.createVerticalStrut(15));
        JLabel logsDeleteTime = createInfoLabel("Löscht alle Anwendungsprotokolle, die älter als 30 Tage sind, aus dem Protokolle-Ordner. So wie in der Datenbank.");
        otherSection.add(Box.createVerticalStrut(15));
        otherSection.add(logsDeleteTime);
        JCheckBox deleteOldLogsCheckBox = new JCheckBox("Alte Protokolle (älter als 30 Tage) löschen");
        styleCheckbox(deleteOldLogsCheckBox);
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteOldLogsCheckBox);
        JButton deleteOldLogsButton = createStyledButton(UnicodeSymbols.TRASH + " Alte Protokolle löschen", new Color(220, 53, 69));
        deleteOldLogsButton.addActionListener(e -> deleteOldLogs());
        otherSection.add(Box.createVerticalStrut(10));
        otherSection.add(deleteOldLogsButton);



        systemPanel.add(otherSection);


        // Add system panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.WRENCH + " System", systemScroll);

        // === CATEGORY 2: Verbindung ===
        JPanel connectionPanel = createCategoryPanel();
        JScrollPane connectionScroll = createScrollablePanel(connectionPanel);

        // Server-Einstellungen
        JPanel serverSection = createSectionPanel(UnicodeSymbols.GLOBE + " Server-Verbindung",
                "URL des QR-Code Scan-Servers");

        JPanel serverUrlPanel = new JPanel(new BorderLayout(0, 10));
        serverUrlPanel.setOpaque(false);
        serverUrlPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        serverUrlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel serverUrlLabel = new JLabel("Server URL:");
        serverUrlLabel.setFont(getFontByName(Font.BOLD, 13));
        serverUrlLabel.setForeground(ThemeManager.getTextPrimaryColor());

        serverUrlField = new JTextField("http://localhost/scan/list.php");
        serverUrlField.setFont(getFontByName(Font.PLAIN, 13));
        serverUrlField.setBackground(ThemeManager.getInputBackgroundColor());
        serverUrlField.setForeground(ThemeManager.getTextPrimaryColor());
        serverUrlField.setCaretColor(ThemeManager.getTextPrimaryColor());
        serverUrlField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        serverUrlField.setPreferredSize(new Dimension(600, 40));
        serverUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        serverUrlPanel.add(serverUrlLabel, BorderLayout.NORTH);
        serverUrlPanel.add(serverUrlField, BorderLayout.CENTER);

        serverSection.add(Box.createVerticalStrut(18));
        serverSection.add(serverUrlPanel);

        connectionPanel.add(serverSection);
        connectionPanel.add(Box.createVerticalGlue());

        // Add connection panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.GLOBE + " Verbindung", connectionScroll);

        // === CATEGORY 3: Datenbank ===
        JPanel databasePanel = createCategoryPanel();
        JScrollPane databaseScroll = createScrollablePanel(databasePanel);
        // Datenbank-Bereinigung Section
        JPanel databaseSection = createSectionPanel(UnicodeSymbols.FLOPPY + " Datenbank-Verwaltung",
                "Datenbank bereinigen und Tabellen verwalten");
        JLabel databaseClearLabel = getDatabaseClearLabel();

        JPanel databaseButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        databaseButtonPanel.setOpaque(false);
        databaseButtonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        databaseButtonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton clearDatabaseButton = createStyledButton(UnicodeSymbols.TRASH + " Datenbank Bereinigen", new Color(220, 53, 69));
        clearDatabaseButton.addActionListener(e -> clearDatabase());

        databaseButtonPanel.add(clearDatabaseButton);

        databaseSection.add(Box.createVerticalStrut(18));
        databaseSection.add(databaseClearLabel);
        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(databaseButtonPanel);

        JLabel selectedDatabaseClear = getSelectedDatabaseClear();
        databaseSection.add(Box.createVerticalStrut(10));
        databaseSection.add(selectedDatabaseClear);

        List<String> tableNames = new ArrayList<>(List.of("articles", "vendors", "orders", "clients", "departments", "users", "warnings"));
        JComboBox<String> tableComboBox = new JComboBox<>(tableNames.toArray(new String[0]));
        styleComboBox(tableComboBox);

        JPanel tableSelectionPanel = createLabeledComboBoxPanel("Tabelle auswählen:", tableComboBox);

        databaseSection.add(Box.createVerticalStrut(15));
        databaseSection.add(tableSelectionPanel);

        JButton deleteSelectedTableButton = createStyledButton(UnicodeSymbols.TRASH + " Ausgewählte Tabelle Löschen", new Color(230, 126, 34));
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

        databasePanel.add(databaseSection);
        databasePanel.add(Box.createVerticalGlue());

        // Add database panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.FLOPPY + " Datenbank", databaseScroll);

        // === CATEGORY 4: Import & Export ===
        JPanel importExportPanel = createCategoryPanel();
        JScrollPane importExportScroll = createScrollablePanel(importExportPanel);

        JPanel importExportSection = createSectionPanel(UnicodeSymbols.ARROW_UP + UnicodeSymbols.ARROW_DOWN + "️ Import & Export",
                "Datenimport und -export Funktionen");
        importExportSection.add(Box.createVerticalStrut(15));

        importExportSection.add(createStyledLabel("Import:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        importExportSection.add(Box.createVerticalStrut(5));

        importExportSection.add(createInfoLabel("<html><div style='width:650px;'>" +
                "Importieren Sie Artikel, Lieferanten oder andere Daten aus CSV-Dateien.<br/>" +
                "Stellen Sie sicher, dass die CSV-Dateien das richtige Format haben.</div></html>"));
        importExportSection.add(Box.createVerticalStrut(10));

        JButton importButton = createStyledButton(UnicodeSymbols.ARROW_UP + " Datenbank Importieren", new Color(39, 174, 96));
        importButton.addActionListener(e -> importFromCsv());
        importExportSection.add(importButton);
        importExportSection.add(Box.createVerticalStrut(20));

        importExportSection.add(Box.createVerticalStrut(15));
        importExportSection.add(createStyledLabel("Export:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        importExportSection.add(Box.createVerticalStrut(5));

        importExportSection.add(createInfoLabel("<html><div style='width:650px;'>" +
                "Exportieren Sie Ihre Datenbankinhalte in CSV-Dateien zur Sicherung oder Weiterverarbeitung.</div></html>"));
        importExportSection.add(Box.createVerticalStrut(10));
        JButton exportButton = createStyledButton(UnicodeSymbols.ARROW_DOWN + " Datenbank Exportieren", new Color(52, 152, 219));
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
                    Main.iconSmall);

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
                            Main.iconSmall);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "<html><b>✗ Fehler beim Export!</b><br/><br/>" +
                                    ex.getMessage() + "</html>",
                            "Fehler",
                            JOptionPane.ERROR_MESSAGE,
                            Main.iconSmall);
                }
            }
        });
        importExportSection.add(exportButton);

        importExportPanel.add(importExportSection);
        importExportPanel.add(Box.createVerticalGlue());

        // Add import/export panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.DOWNLOAD + " Import/Export", importExportScroll);

        // === CATEGORY 5: About ===
        JPanel aboutPanel = createCategoryPanel();
        JScrollPane aboutScroll = createScrollablePanel(aboutPanel);

        JPanel aboutSection = createSectionPanel(UnicodeSymbols.INFO + " Über VEBO Lagersystem",
                "Informationen über die Anwendung");

        // Application Info
        JPanel appInfoPanel = new JPanel();
        appInfoPanel.setLayout(new BoxLayout(appInfoPanel, BoxLayout.Y_AXIS));
        appInfoPanel.setOpaque(false);
        appInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appInfoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Logo/Title
        JLabel appNameLabel = new JLabel("VEBO Lagersystem");
        appNameLabel.setFont(getFontByName(Font.BOLD, 28));
        appNameLabel.setForeground(ThemeManager.getTitleTextColor());
        appNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        appNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://vebo.ch"));
                } catch (Exception ex) {
                    logger.error("Fehler beim Öffnen der VEBO-Website: {}", ex.getMessage());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                appNameLabel.setForeground(ThemeManager.getTitleTextHighlightColor());
                appNameLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                appNameLabel.setForeground(ThemeManager.getTitleTextColor());
                appNameLabel.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        appNameLabel.setToolTipText("Besuchen Sie unsere Website: https://vebo.ch");

        JLabel versionLabel = new JLabel("Version " + Main.VERSION);
        versionLabel.setFont(getFontByName(Font.PLAIN, 14));
        versionLabel.setForeground(ThemeManager.getTextSecondaryColor());
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        appInfoPanel.add(appNameLabel);
        appInfoPanel.add(Box.createVerticalStrut(5));
        appInfoPanel.add(versionLabel);

        aboutSection.add(Box.createVerticalStrut(18));
        aboutSection.add(appInfoPanel);

        // Description
        JPanel descriptionPanel = new JPanel(new BorderLayout(0, 10));
        descriptionPanel.setOpaque(false);
        descriptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        descriptionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descTitle = new JLabel("Beschreibung:");
        descTitle.setFont(getFontByName(Font.PLAIN, 14));
        descTitle.setForeground(ThemeManager.getTextPrimaryColor());

        JTextArea descriptionArea = new JTextArea(
                "VEBO Lagersystem ist eine moderne Lagerverwaltungssoftware für die effiziente " +
                "Verwaltung von Artikeln, Bestellungen, Lieferanten und Kunden. Die Anwendung " +
                "bietet umfangreiche Funktionen für die Bestandsverwaltung, automatische Warnungen " +
                "bei niedrigem Lagerbestand und QR-Code-basierte Artikelerfassung."
        );
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setLineWrap(true);
        descriptionArea.setEditable(false);
        descriptionArea.setFont(getFontByName(Font.PLAIN, 13));
        descriptionArea.setForeground(ThemeManager.getTextPrimaryColor());
        descriptionArea.setBackground(ThemeManager.getInputBackgroundColor());
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        descriptionArea.setRows(4);

        descriptionPanel.add(descTitle, BorderLayout.NORTH);
        descriptionPanel.add(descriptionArea, BorderLayout.CENTER);

        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(descriptionPanel);

        // Developer Info
        JPanel developerPanel = new JPanel();
        developerPanel.setLayout(new BoxLayout(developerPanel, BoxLayout.Y_AXIS));
        developerPanel.setOpaque(false);
        developerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        developerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        developerPanel.add(createStyledLabel(UnicodeSymbols.DEVELOPER + " Entwickler:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        developerPanel.add(Box.createVerticalStrut(8));
        developerPanel.add(createStyledLabel("Darryl Huber", 13, Font.PLAIN, ThemeManager.getTextPrimaryColor()));
        developerPanel.add(Box.createVerticalStrut(5));
        developerPanel.add(createStyledLabel("Organisation: VEBO Oensingen", 13, Font.PLAIN, ThemeManager.getTextSecondaryColor()));

        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(developerPanel);

        // System Info
        JPanel systemInfoPanel = new JPanel();
        systemInfoPanel.setLayout(new BoxLayout(systemInfoPanel, BoxLayout.Y_AXIS));
        systemInfoPanel.setOpaque(false);
        systemInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        systemInfoPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        systemInfoPanel.add(createStyledLabel(UnicodeSymbols.DEVELOPER + " System-Information:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(8));
        systemInfoPanel.add(createStyledLabel("Java Version: " + System.getProperty("java.version"), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(createStyledLabel("Betriebssystem: " + System.getProperty("os.name") + " " + System.getProperty("os.version"), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        systemInfoPanel.add(Box.createVerticalStrut(5));
        systemInfoPanel.add(createStyledLabel("Datenverzeichnis: " + Main.getAppDataDir().getAbsolutePath(), 12, Font.PLAIN, ThemeManager.getTextSecondaryColor()));

        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(systemInfoPanel);

        // Update Manager Section
        JPanel updatePanel = new JPanel();
        updatePanel.setLayout(new BoxLayout(updatePanel, BoxLayout.Y_AXIS));
        updatePanel.setOpaque(false);
        updatePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updatePanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        updatePanel.add(createStyledLabel(UnicodeSymbols.DOWNLOAD + " Update-Verwaltung:", 14, Font.BOLD, ThemeManager.getTextPrimaryColor()));
        updatePanel.add(Box.createVerticalStrut(12));

        // Create update button rows with better layout
        JPanel updateRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        updateRow1.setOpaque(false);
        updateRow1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        updateRow1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton checkStableUpdateBtn = createStyledButton("Stable-Updates", new Color(52, 152, 219));
        checkStableUpdateBtn.setToolTipText("Prüft auf neue stabile Versionen");
        checkStableUpdateBtn.addActionListener(e -> checkForUpdates("stable"));
        checkStableUpdateBtn.setPreferredSize(new Dimension(180, 40));

        JButton checkBetaUpdateBtn = createStyledButton("Beta-Updates", new Color(243, 156, 18));
        checkBetaUpdateBtn.setToolTipText("Prüft auf neue Beta-Versionen");
        checkBetaUpdateBtn.addActionListener(e -> checkForUpdates("beta"));
        checkBetaUpdateBtn.setPreferredSize(new Dimension(180, 40));

        updateRow1.add(checkStableUpdateBtn);
        updateRow1.add(checkBetaUpdateBtn);

        updatePanel.add(updateRow1);
        updatePanel.add(Box.createVerticalStrut(8));

        JPanel updateRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        updateRow2.setOpaque(false);
        updateRow2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));
        updateRow2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton checkAlphaUpdateBtn = createStyledButton("Alpha-Updates", new Color(231, 76, 60));
        checkAlphaUpdateBtn.setToolTipText("Prüft auf neue Alpha-Versionen (experimentell)");
        checkAlphaUpdateBtn.addActionListener(e -> checkForUpdates("alpha"));
        checkAlphaUpdateBtn.setPreferredSize(new Dimension(180, 40));

        JButton checkTestingUpdateBtn = createStyledButton("Testing-Updates", new Color(155, 89, 182));
        checkTestingUpdateBtn.setToolTipText("Prüft auf neue Testing-Versionen (Entwicklung)");
        checkTestingUpdateBtn.addActionListener(e -> checkForUpdates("testing"));
        checkTestingUpdateBtn.setPreferredSize(new Dimension(180, 40));

        updateRow2.add(checkAlphaUpdateBtn);
        updateRow2.add(checkTestingUpdateBtn);

        updatePanel.add(updateRow2);
        updatePanel.add(Box.createVerticalStrut(12));

        // Add info text about version
        JLabel versionInfoLabel = new JLabel("<html><div style='padding: 8px; background: " + toHexColor(ThemeManager.getInputBackgroundColor()) + "; border-radius: 6px;'>" +
                "<b>Aktuelle Version:</b> " + Main.VERSION + "<br/>" +
                "<span style='font-size: 11px; color: #666;'>Kanal: " + UpdateManager.detectChannel(Main.VERSION) + "</span>" +
                "</div></html>");
        versionInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        versionInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        updatePanel.add(versionInfoLabel);

        aboutSection.add(Box.createVerticalStrut(15));
        aboutSection.add(updatePanel);

        // Copyright
        JPanel copyrightPanel = new JPanel();
        copyrightPanel.setLayout(new BoxLayout(copyrightPanel, BoxLayout.Y_AXIS));
        copyrightPanel.setOpaque(false);
        copyrightPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyrightPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()),
                BorderFactory.createEmptyBorder(15, 0, 0, 0)
        ));

        copyrightPanel.add(createStyledLabel("© 2026 VEBO Oensingen. Alle Rechte vorbehalten.", 11, Font.PLAIN, ThemeManager.getTextSecondaryColor()));
        copyrightPanel.add(Box.createVerticalStrut(5));

        JLabel licenseLabel = createStyledLabel("Diese Software wird bereitgestellt \"wie sie ist\", ohne jegliche Garantie.", 11, Font.ITALIC, ThemeManager.getTextSecondaryColor());
        copyrightPanel.add(licenseLabel);

        aboutSection.add(Box.createVerticalStrut(20));
        aboutSection.add(copyrightPanel);

        aboutPanel.add(aboutSection);
        aboutPanel.add(Box.createVerticalGlue());

        // Add about panel to tabbed pane
        tabbedPane.addTab(UnicodeSymbols.INFO + " Über", aboutScroll);

        // Add tabbed pane to content wrapper
        contentWrapper.add(tabbedPane, BorderLayout.CENTER);

        mainContainer.add(contentWrapper, BorderLayout.CENTER);

        // Button Panel with enhanced styling
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 18));
        buttonPanel.setBackground(ThemeManager.getCardBackgroundColor());
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getBorderColor()));

        JButton cancelButton = createStyledButton(UnicodeSymbols.CLOSE + " Abbrechen", new Color(155, 89, 182));
        cancelButton.addActionListener(e -> dispose());

        JButton saveButton = createStyledButton(UnicodeSymbols.FLOPPY + " Speichern", new Color(46, 204, 113));
        saveButton.addActionListener(e -> saveSettings());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);

        mainContainer.add(buttonPanel, BorderLayout.SOUTH);

        add(mainContainer);

        // Load current settings
        loadSettings();
    }

    private void deleteOldLogs() {
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
        int deletedCount = logManager.deleteOldLogs(30);
        File logsFolder = new File(Main.getAppDataDir(), "logs");
        int fileDeletedCount = 0;
        try {
            File[] logFiles = logsFolder.listFiles();
            if (logFiles != null) {
                LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
                for (File logFile : logFiles) {
                    if (logFile.isFile()) {
                        BasicFileAttributes attrs = Files.readAttributes(logFile.toPath(), BasicFileAttributes.class);
                        LocalDateTime fileTime = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
                        if (fileTime.isBefore(cutoffDate)) {
                            Files.delete(logFile.toPath());
                            fileDeletedCount++;
                        }
                    }
                }
            }
            JOptionPane.showMessageDialog(this,
                    String.format("Es wurden %d Protokolle aus der Datenbank und %d Protokolldateien gelöscht, die älter als 30 Tage sind.", deletedCount, fileDeletedCount),
                    "Alte Protokolle gelöscht",
                    JOptionPane.INFORMATION_MESSAGE);
            logger.info("Es wurden {} Protokolle aus der Datenbank und {} Protokolldateien gelöscht, die älter als 30 Tage sind.", deletedCount, fileDeletedCount);
            Main.logUtils.addLog("Es wurden " + deletedCount + " Protokolle aus der Datenbank und " + fileDeletedCount + " Protokolldateien gelöscht, die älter als 30 Tage sind.");
        } catch (IOException ex) {
            logger.error("Fehler beim Löschen alter Protokolle: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Löschen alter Protokolle:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Löschen alter Protokolle: " + ex.getMessage());
            return;
        }
    }

    private void deleteAllLogs() {
        File logsFolder = new File(Main.getAppDataDir(), "logs");
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html><b>Alle Protokolle wirklich löschen?</b><br/><br/>" +
                        "Diese Aktion löscht alle Protokolldateien im Protokolle-Ordner.<br/>" +
                        "Möchten Sie fortfahren?</html>",
                "Protokolle löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                File[] logFiles = logsFolder.listFiles();
                if (logFiles != null) {
                    for (File logFile : logFiles) {
                        if (logFile.isFile()) {
                            Files.delete(logFile.toPath());
                        }
                    }
                }
            } catch (IOException e) {
                logger.error("Fehler beim Löschen der Protokolle: {}", e.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Fehler beim Löschen der Protokolle:\n" + e.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE);
                Main.logUtils.addLog("Fehler beim Löschen der Protokolle: " + e.getMessage());
                return;
            }
            JOptionPane.showMessageDialog(this,
                    "Alle Protokolle wurden erfolgreich gelöscht.",
                    "Protokolle gelöscht",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
        if(logManager.clearAllLogs()) {
            logger.info("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
            Main.logUtils.addLog("Alle Protokolle wurden erfolgreich aus der Datenbank gelöscht.");
        } else {
            logger.error("Fehler beim Löschen der Protokolle aus der Datenbank.");
            Main.logUtils.addLog("Fehler beim Löschen der Protokolle aus der Datenbank.");
        }
    }

    private void openLogsFolder() {
        File logsDir = new File(Main.getAppDataDir(), "logs");
        try {
            Desktop.getDesktop().open(logsDir);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen des Protokolle-Ordners: {}", e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Öffnen des Protokolle-Ordners:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Öffnen des Protokolle-Ordners: " + e.getMessage());
        }
    }

    private void openSettingsFolder() {
        File settingsDir = Main.getAppDataDir();
        try {
            Desktop.getDesktop().open(settingsDir);
        } catch (IOException e) {
            logger.error("Fehler beim Öffnen des Einstellungen-Ordners: {}", e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Fehler beim Öffnen des Einstellungen-Ordners:\n" + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE);
            Main.logUtils.addLog("Fehler beim Öffnen des Einstellungen-Ordners: " + e.getMessage());
        }
    }

    /**
     * Creates a category panel for tabbed interface with proper width management
     */
    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ThemeManager.getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        return panel;
    }

    /**
     * Creates a scrollable panel wrapper with optimized settings
     */
    private JScrollPane createScrollablePanel(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(ThemeManager.getBackgroundColor());


        return scrollPane;
    }

    private static JLabel getSelectedDatabaseClear() {
        JLabel selectedDatabaseClear = new JLabel("<html><div style='padding: 8px 0'><p>" +
                "<i>Hinweis: Diese Aktion löscht eine bestimmte Tabelle. Bitte seien Sie vorsichtig!</i>" +
                "</div></html>");
        selectedDatabaseClear.setFont(getFontByName(Font.PLAIN, 12));
        selectedDatabaseClear.setForeground(ThemeManager.getTextSecondaryColor());
        selectedDatabaseClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        return selectedDatabaseClear;
    }

    private static JLabel getDatabaseClearLabel() {
        JLabel databaseClearLabel = new JLabel("<html><div style='padding: 8px 0'><p>" +
                "Mit dem Knopf <b>Datenbank Bereinigen</b> werden alle Daten in der Datenbank gelöscht!" +
                "</div></html>");
        databaseClearLabel.setFont(getFontByName(Font.PLAIN, 13));
        databaseClearLabel.setForeground(ThemeManager.getTextSecondaryColor());
        databaseClearLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return databaseClearLabel;
    }

    private static JLabel getWarningInfoLabel() {
        JLabel warningInfoLabel = new JLabel("<html><div style='padding: 8px 0;'>" +
                "<i>Warnungen werden automatisch in diesem Intervall angezeigt, wenn neue ungelesene Warnungen vorhanden sind.</i>" +
                "</div></html>");
        warningInfoLabel.setFont(getFontByName(Font.PLAIN, 12));
        warningInfoLabel.setForeground(ThemeManager.getTextSecondaryColor());
        warningInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return warningInfoLabel;
    }

    /**
     * Creates a modern styled section panel with card-like appearance
     */
    private JPanel createSectionPanel(String title, String description) {
        RoundedPanel section = getRoundedPanel();

        // Header panel with better visual hierarchy
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(getFontByName(Font.BOLD, 20));
        titleLabel.setForeground(ThemeManager.getTextPrimaryColor());

        headerPanel.add(titleLabel);

        // Description with better formatting
        JLabel descLabel = new JLabel("<html><div style='line-height: 1.8; color: #6c757d; font-weight: 300;'>" +
                description + "</div></html>");
        descLabel.setFont(getFontByName(Font.PLAIN, 13));
        descLabel.setForeground(ThemeManager.getTextSecondaryColor());
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        descLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        // Add a subtle separator line
        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setForeground(ThemeManager.getBorderColor());
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        section.add(headerPanel);
        section.add(Box.createVerticalStrut(12));
        section.add(descLabel);
        section.add(Box.createVerticalStrut(20));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));

        return section;
    }

    private static RoundedPanel getRoundedPanel() {
        RoundedPanel section = new RoundedPanel(ThemeManager.getCardBackgroundColor(), 16);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));

        // Enhanced shadow and border with depth effect
        section.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                new Color(0, 0, 0, 10), 3),
                        BorderFactory.createEmptyBorder(1, 1, 3, 1)
                ),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1),
                        BorderFactory.createEmptyBorder(28, 32, 28, 32)
                )
        ));

        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 520));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        return section;
    }

    /**
     * Styles a checkbox with modern appearance and enhanced click area
     */
    private void styleCheckbox(JCheckBox checkbox) {
        checkbox.setFont(getFontByName(Font.PLAIN, 14));
        checkbox.setForeground(ThemeManager.getTextPrimaryColor());
        checkbox.setOpaque(false);
        checkbox.setFocusPainted(false);
        checkbox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        checkbox.setBorder(BorderFactory.createEmptyBorder(8, 2, 8, 0));

        // Add hover effect for better UX
        checkbox.addMouseListener(new MouseAdapter() {
            private final Color originalForeground = checkbox.getForeground();
            private final Color hoverForeground = ThemeManager.getPrimaryColor();

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
            textField.setFont(getFontByName(Font.PLAIN, 14));
            textField.setBackground(ThemeManager.getInputBackgroundColor());
            textField.setForeground(ThemeManager.getTextPrimaryColor());
            textField.setCaretColor(ThemeManager.getTextPrimaryColor());
            textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));

            // Add focus listener for better UX
            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getPrimaryColor(), 2, true),
                            BorderFactory.createEmptyBorder(9, 13, 9, 13)
                    ));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true),
                            BorderFactory.createEmptyBorder(10, 14, 10, 14)
                    ));
                }
            });
        }

        spinner.setBorder(BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 1, true));
        spinner.setBackground(ThemeManager.getInputBackgroundColor());
    }

    /**
     * Creates a modern styled button with smooth hover effects and professional appearance
     */
    private JButton createStyledButton(String text, Color originalBg) {
        JButton button = new JButton(text);
        button.setFont(getFontByName(Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(originalBg);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                BorderFactory.createEmptyBorder(14, 28, 14, 28)
        ));
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(true);
        button.setContentAreaFilled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Set consistent height
        button.setPreferredSize(new Dimension(
                button.getPreferredSize().width,
                48
        ));

        // Enhanced hover effect with smooth color transitions
        Color hoverBg = new Color(
                Math.max(0, originalBg.getRed() - 25),
                Math.max(0, originalBg.getGreen() - 25),
                Math.max(0, originalBg.getBlue() - 25)
        );
        Color pressedBg = new Color(
                Math.max(0, originalBg.getRed() - 50),
                Math.max(0, originalBg.getGreen() - 50),
                Math.max(0, originalBg.getBlue() - 50)
        );

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(hoverBg.darker(), 2, true),
                        BorderFactory.createEmptyBorder(13, 27, 13, 27)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(originalBg);
                button.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(originalBg.darker(), 1, true),
                        BorderFactory.createEmptyBorder(14, 28, 14, 28)
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

                String enableAutomaticImportStr = Main.settings.getProperty("enable_automatic_import_qrcode");
                boolean enableAutomaticImport = enableAutomaticImportStr == null || Boolean.parseBoolean(enableAutomaticImportStr);
                automaticImportCheckBox.setSelected(enableAutomaticImport);

                String qrCodeImportIntervalStr = Main.settings.getProperty("qrcode_import_interval");
                int qrCodeImportInterval = (qrCodeImportIntervalStr != null) ? Integer.parseInt(qrCodeImportIntervalStr) : 10;
                qrCodeImportIntervalSpinner.setValue(qrCodeImportInterval);

                // Load dark mode setting (default false)
                String darkModeStr = Main.settings.getProperty("dark_mode");
                boolean darkMode = Boolean.parseBoolean(darkModeStr);
                darkModeCheckbox.setSelected(darkMode);
                themeComboBox.setSelectedItem(darkMode ? "Dark" : "Light");

                // Load font size setting (default 14)
                String fontSizeStr = Main.settings.getProperty("table_font_size");
                int fontSize = (fontSizeStr != null) ? Integer.parseInt(fontSizeStr) : 16;
                fontSizeTableSpinner.setValue(fontSize);

                String fontSizeTabStr = Main.settings.getProperty("table_font_size_tab");
                int fontSizeTab = (fontSizeTabStr != null) ? Integer.parseInt(fontSizeTabStr) : 15;
                fontSizeTabSpinner.setValue(fontSizeTab);

                String fontStyle = Main.settings.getProperty("font_style");
                if(fontStyle != null) {
                    fontComboBox.setSelectedItem(fontStyle);
                }

                System.out.println("[SettingsGUI] Einstellungen geladen");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Laden der Einstellungen: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Laden der Einstellungen: " + e.getMessage());
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

                // Save QR-Code settings
                boolean enableAutomaticImport = automaticImportCheckBox.isSelected();
                Main.settings.setProperty("enable_automatic_import_qrcode", String.valueOf(enableAutomaticImport));

                // Save QR-Code import interval
                int qrCodeImportInterval = (Integer) qrCodeImportIntervalSpinner.getValue();
                Main.settings.setProperty("qrcode_import_interval", String.valueOf(qrCodeImportInterval));

                // Save dark mode setting
                boolean darkMode = darkModeCheckbox.isSelected();
                Main.settings.setProperty("dark_mode", String.valueOf(darkMode));

                // Save font size setting
                int fontSize = (Integer) fontSizeTableSpinner.getValue();
                Main.settings.setProperty("table_font_size", String.valueOf(fontSize));
                int fontSizeTab = (Integer) fontSizeTabSpinner.getValue();
                Main.settings.setProperty("table_font_size_tab", String.valueOf(fontSizeTab));
                String fontStyle = (String) fontComboBox.getSelectedItem();
                Main.settings.setProperty("font_style", fontStyle);

                Main.settings.save();

                System.out.println("[SettingsGUI] Einstellungen gespeichert");

                // Apply settings immediately
                applySettings(interval, enableWarnings, warningInterval, enableAutoCheck, darkMode);

                JOptionPane.showMessageDialog(this,
                        "<html><b>Einstellungen gespeichert!</b><br/><br/>" +
                                "Die neuen Einstellungen wurden erfolgreich übernommen.</html>",
                        "Erfolg",
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

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
                    Main.iconSmall);
            Main.logUtils.addLog("Fehler beim Speichern der Einstellungen: " + e.getMessage());
        }
    }

    @SuppressWarnings("MagicConstant")
    public static Font getFontByName(int style, int fontSize) {
        String fontName = Main.settings.getProperty("font_style");
        if(fontName == null || fontName.trim().isEmpty()) {
            fontName = "Arial";
        }
        return new Font(fontName, style, fontSize);
    }

    /**
     * Applies settings to the running system
     */
    private void applySettings(int interval, boolean enableWarnings, int warningInterval, boolean enableAutoCheck, boolean darkMode) {
        try {
            // Apply theme changes
            boolean currentDarkMode = ThemeManager.isDarkMode();
            if (currentDarkMode != darkMode) {
                ThemeManager.setDarkMode(darkMode);
                System.out.println("[SettingsGUI] Theme geändert zu: " + (darkMode ? "Dark Mode" : "Light Mode"));

                // Show restart recommendation for full theme change
                int restart = JOptionPane.showConfirmDialog(this,
                        "<html>Das Theme wurde geändert.<br/><br/>" +
                        "Es wird empfohlen, das Programm neu zu starten,<br/>" +
                        "damit das Theme vollständig angewendet wird.<br/><br/>" +
                        "Möchten Sie jetzt neu starten?</html>",
                        "Neustart empfohlen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        Main.iconSmall);

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                    return;
                }
            }

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

            if(automaticImportCheckBox.isSelected()) {
                scheduler.startAutoImportQrCodes(
                        (Integer) qrCodeImportIntervalSpinner.getValue(),
                        java.util.concurrent.TimeUnit.MINUTES
                );
                System.out.println("[SettingsGUI] Automatischer QR-Code Import aktiviert (Intervall: " + qrCodeImportIntervalSpinner.getValue() + " Min.)");
            } else {
                System.out.println("[SettingsGUI] Automatischer QR-Code Import deaktiviert");
            }

        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Anwenden der Einstellungen: " + e.getMessage());
            logger.error("Fehler beim Anwenden der Einstellungen", e);
            Main.logUtils.addLog("Fehler beim Anwenden der Einstellungen: " + e.getMessage());
        }
    }

    /**
     * Checks for updates based on the selected release channel (stable, beta, alpha, testing)
     * Shows a dialog with update information and download link if available
     */
    private void checkForUpdates(String channel) {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            // Convert string to ReleaseChannel enum
            UpdateManager.ReleaseChannel releaseChannel = switch (channel.toLowerCase()) {
                case "beta" -> UpdateManager.ReleaseChannel.BETA;
                case "alpha" -> UpdateManager.ReleaseChannel.ALPHA;
                case "testing" -> UpdateManager.ReleaseChannel.TESTING;
                default -> UpdateManager.ReleaseChannel.STABLE;
            };

            String channelDisplay = switch (channel.toLowerCase()) {
                case "beta" -> "Beta";
                case "alpha" -> "Alpha";
                case "testing" -> "Testing";
                default -> "Stable";
            };

            // Create MODAL progress dialog
            JDialog progressDialog = new JDialog(this, "Nach Updates suchen...", true);
            progressDialog.setLayout(new BorderLayout());
            progressDialog.setSize(400, 150);
            progressDialog.setLocationRelativeTo(this);
            progressDialog.setUndecorated(true);
            progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            progressDialog.setResizable(false);

            JPanel progressPanel = new JPanel(new BorderLayout(10, 10));
            progressPanel.setBackground(ThemeManager.getCardBackgroundColor());
            progressPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getBorderColor(), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            JLabel progressLabel = new JLabel("<html><center>Prüfe auf " + channelDisplay + "-Updates...<br/>Bitte warten...</center></html>");
            progressLabel.setFont(getFontByName(Font.PLAIN, 14));
            progressLabel.setForeground(ThemeManager.getTextPrimaryColor());
            progressLabel.setHorizontalAlignment(SwingConstants.CENTER);

            progressPanel.add(progressLabel, BorderLayout.CENTER);
            progressDialog.add(progressPanel);

            // Check for updates in background
            SwingWorker<String, Void> worker = new SwingWorker<>() {
                @Override
                protected String doInBackground() {
                    return updateManager.getLatestVersion(releaseChannel);
                }

                @Override
                protected void done() {
                    try {
                        String latestVersion = get();
                        handleUpdateResult(latestVersion, channel);
                    } catch (Exception e) {
                        logger.error("Fehler beim Prüfen auf Updates: {}", e.getMessage(), e);
                        showUpdateError(e.getMessage());
                    } finally {
                        // Always close the progress dialog
                        progressDialog.setVisible(false);
                        progressDialog.dispose();
                    }
                }
            };

            // Start worker
            worker.execute();
            // Show modal dialog - this will block until disposed
            progressDialog.setVisible(true);

        } catch (Exception e) {
            logger.error("Fehler beim Starten der Update-Prüfung: {}", e.getMessage(), e);
            showUpdateError(e.getMessage());
            Main.logUtils.addLog("Fehler beim Starten der Update-Prüfung: " + e.getMessage());
        }
    }

    /**
     * Handles the update check result and displays appropriate dialog
     */
    private void handleUpdateResult(String latestVersion, String channel) {
        if (latestVersion == null) {
            JOptionPane.showMessageDialog(this,
                "<html><b>Keine Update-Informationen verfügbar</b><br/><br/>" +
                "Die Update-Informationen konnten nicht abgerufen werden.<br/>" +
                "Bitte überprüfen Sie Ihre Internetverbindung.</html>",
                "Update-Prüfung",
                JOptionPane.WARNING_MESSAGE,
                Main.iconSmall);
            return;
        }

        String currentVersion = Main.VERSION;
        UpdateManager updateManager = UpdateManager.getInstance();

        if (updateManager.isUpdateAvailable(currentVersion)) {
            showUpdateAvailableDialog(latestVersion, currentVersion, channel);
        } else {
            showNoUpdateDialog(currentVersion, channel);
        }
    }

    /**
     * Shows dialog when an update is available
     */
    private void showUpdateAvailableDialog(String latestVersion, String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        String downloadUrl = getDownloadUrl(channel);

        Object[] options = {"Download-Seite öffnen", "Später"};
        int result = JOptionPane.showOptionDialog(this,
            "<html><b>" + UnicodeSymbols.DOWNLOAD + " Update verfügbar!</b><br/><br/>" +
            "Eine neue Version ist verfügbar:<br/><br/>" +
            "Aktuelle Version: <b>" + currentVersion + "</b><br/>" +
            "Neue Version: <b>" + latestVersion + channelDisplay + "</b><br/><br/>" +
            "Möchten Sie die Download-Seite öffnen?</html>",
            "Update verfügbar",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            Main.iconSmall,
            options,
            options[0]);

        if (result == 0) { // Download-Seite öffnen
            openDownloadPage(downloadUrl);
        }
    }

    /**
     * Shows dialog when no update is available
     */
    private void showNoUpdateDialog(String currentVersion, String channel) {
        String channelDisplay = switch (channel.toLowerCase()) {
            case "beta" -> " (Beta)";
            case "alpha" -> " (Alpha)";
            case "testing" -> " (Testing)";
            default -> "";
        };

        JOptionPane.showMessageDialog(this,
            "<html><b>" + UnicodeSymbols.CHECKMARK + " Keine Updates verfügbar</b><br/><br/>" +
            "Sie verwenden bereits die neueste Version" + channelDisplay + ":<br/>" +
            "<b>Version " + currentVersion + "</b></html>",
            "Update-Prüfung",
            JOptionPane.INFORMATION_MESSAGE,
            Main.iconSmall);
    }

    /**
     * Shows error dialog when update check fails
     */
    private void showUpdateError(String errorMessage) {
        JOptionPane.showMessageDialog(this,
            "<html><b>" + UnicodeSymbols.WARNING + " Fehler bei Update-Prüfung</b><br/><br/>" +
            "Die Update-Prüfung ist fehlgeschlagen:<br/>" +
            errorMessage + "</html>",
            "Fehler",
            JOptionPane.ERROR_MESSAGE,
            Main.iconSmall);
        Main.logUtils.addLog("Fehler bei Update-Prüfung");
    }

    /**
     * Gets the download URL based on the release channel
     */
    private String getDownloadUrl(String channel) {
        return switch (channel.toLowerCase()) {
            case "beta" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=beta";
            case "alpha" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=alpha";
            case "testing" -> "https://github.com/frame-dev/VeboLagerSystem/releases?q=testing";
            default -> "https://github.com/frame-dev/VeboLagerSystem/releases/latest";
        };
    }

    /**
     * Opens the download page in the default browser
     */
    private void openDownloadPage(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new java.net.URI(url));
                logger.info("Download-Seite geöffnet: {}", url);
            } else {
                // Fallback: show URL in dialog
                JOptionPane.showMessageDialog(this,
                    "<html><b>Download-Link:</b><br/><br/>" +
                    "<a href='" + url + "'>" + url + "</a><br/><br/>" +
                    "Bitte kopieren Sie den Link und öffnen Sie ihn in Ihrem Browser.</html>",
                    "Download-Link",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Öffnen der Download-Seite: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(this,
                "<html><b>Fehler beim Öffnen des Browsers</b><br/><br/>" +
                "Bitte öffnen Sie den folgenden Link manuell:<br/>" +
                url + "</html>",
                "Fehler",
                JOptionPane.ERROR_MESSAGE,
                Main.iconSmall);
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
            DatabaseManager dbManager = databaseManager;

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
                            Main.iconSmall);

                    System.out.printf("[SettingsGUI] Tabelle '%s' wurde erfolgreich gelöscht%n", tableName);

                    // Show restart recommendation
                    int restart = JOptionPane.showConfirmDialog(this,
                            "<html>Es wird empfohlen, das Programm neu zu starten,<br/>" +
                                    "um Inkonsistenzen zu vermeiden.<br/><br/>" +
                                    "Möchten Sie jetzt neu starten?</html>",
                            "Neustart empfohlen",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            Main.iconSmall);

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
                            Main.iconSmall);
                    Main.logUtils.addLog(String.format("Fehler beim Löschen der Tabelle. Die Tabelle '%s' konnte nicht gelöscht werden.", tableName));
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
            }
        } catch (Exception e) {
            System.err.printf("[SettingsGUI] Fehler beim Löschen der Tabelle '%s': %s%n",
                    tableName, e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Löschen der Tabelle '%s': %s", tableName, e.getMessage()));
            logger.error("Fehler beim Löschen der Tabelle '{}'", tableName, e);
            JOptionPane.showMessageDialog(this,
                    String.format("<html><b>Fehler beim Löschen der Tabelle</b><br/><br/>" +
                                    "Tabelle: %s<br/>" +
                                    "Fehler: %s</html>",
                            tableName, e.getMessage()),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    /**
     * Clears the database after user confirmation
     */
    private void clearDatabase() {
        // First confirmation
        int firstConfirm = JOptionPane.showConfirmDialog(this,
                "<html><b>⚠ WARNUNG: Datenbank löschen</b><br/><br/>" +
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
                Main.iconSmall);

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
                    Main.iconSmall);
            return;
        }

        // Perform database clearing
        try {
            DatabaseManager dbManager =
                    databaseManager;

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
                        Main.iconSmall);

                System.out.println("[SettingsGUI] Datenbank wurde erfolgreich bereinigt");

                // Ask if user wants to restart
                int restart = JOptionPane.showConfirmDialog(this,
                        "Möchten Sie das Programm jetzt neu starten?",
                        "Neustart empfohlen",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        Main.iconSmall);

                if (restart == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                        "Fehler: Datenbankverbindung nicht verfügbar.",
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall);
                Main.logUtils.addLog("Fehler: Datenbankverbindung nicht verfügbar.");
            }
        } catch (Exception e) {
            System.err.println("[SettingsGUI] Fehler beim Löschen der Datenbank: " + e.getMessage());
            logger.error("Fehler beim Löschen der Datenbank", e);
            JOptionPane.showMessageDialog(this,
                    "<html><b>Fehler beim Löschen der Datenbank</b><br/><br/>" +
                            "Fehler: " + e.getMessage() + "</html>",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
        }
    }

    /**
     * Creates a panel with a label, spinner, and unit label
     */
    private JPanel createLabeledSpinnerPanel(String labelText, JSpinner spinner, String unitText, int columns) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        spinner.setFont(getFontByName(Font.PLAIN, 13));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(columns);
        styleSpinner(spinner);

        JLabel unitLabel = new JLabel(unitText);
        unitLabel.setFont(getFontByName(Font.PLAIN, 13));
        unitLabel.setForeground(ThemeManager.getTextSecondaryColor());

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(spinner);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(unitLabel);

        return panel;
    }

    /**
     * Creates a panel with a label and combo box
     */
    private JPanel createLabeledComboBoxPanel(String labelText, JComboBox<?> comboBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel label = new JLabel(labelText);
        label.setFont(getFontByName(Font.BOLD, 13));
        label.setForeground(ThemeManager.getTextPrimaryColor());

        comboBox.setPreferredSize(new Dimension(200, 35));

        panel.add(label);
        panel.add(Box.createHorizontalStrut(12));
        panel.add(comboBox);

        return panel;
    }

    /**
     * Creates a styled label with specified font and color
     */
    @SuppressWarnings("MagicConstant")
    private JLabel createStyledLabel(String text, int fontSize, int fontStyle, Color color) {
        JLabel label = new JLabel(text);
        String selectedItem = (String) fontComboBox.getSelectedItem();
        if(selectedItem == null) {
            logger.error("Font-ComboBox hat kein ausgewähltes Element, Standardwert wird verwendet");
            selectedItem = "Arial";
        }
        label.setFont(new Font(selectedItem, fontStyle, fontSize));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Creates an info label with HTML content
     */
    private JLabel createInfoLabel(String htmlContent) {
        JLabel label = new JLabel(htmlContent);
        label.setFont(getFontByName(Font.PLAIN, 13));
        label.setForeground(ThemeManager.getTextSecondaryColor());
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Rounded panel for modern card design with enhanced shadow
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
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Draw subtle shadow
            g2.setColor(new Color(0, 0, 0, 15));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, radius, radius);

            // Draw main card
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, radius, radius);

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
            importOrdersFromCsv();
        } else {
            // Ask user what type of data this is
            String[] options = {"Artikel", "Lieferanten", "Kunden", "Bestellungen", "Abbrechen"};
            int choice = JOptionPane.showOptionDialog(null,
                    "Welche Art von Daten möchten Sie importieren?",
                    "Datentyp auswählen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    Main.iconSmall,
                    options,
                    options[0]);

            switch (choice) {
                case 0 -> importArticlesFromCsv(selectedFile);
                case 1 -> importVendorsFromCsv(selectedFile);
                case 2 -> importClientsFromCsv(selectedFile);
                case 3 -> importOrdersFromCsv();
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
                        Main.iconSmall);
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
                    Article article = getArticle(parts, articleNumber);

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
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Artikel-Import: %d erfolgreich, %d Fehler%n", imported, errors);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Artikel: " + e.getMessage());
            Main.logUtils.addLog(String.format("Fehler beim Importieren der Artikel: %s", e.getMessage()));
        }
    }

    private static Article getArticle(String[] parts, String articleNumber) {
        String name = parts[1];
        String details = parts[2];
        int stockQuantity = Integer.parseInt(parts[3]);
        int minStockLevel = Integer.parseInt(parts[4]);
        double sellPrice = Double.parseDouble(parts[5]);
        double purchasePrice = Double.parseDouble(parts[6]);
        String vendorName = parts[7];

        return new Article(articleNumber, name, details,
                stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName);
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
                        Main.iconSmall);
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
                    double minOrderValue = Double.parseDouble(parts[5]);

                    Vendor vendor = new Vendor(name, contactPerson, phoneNumber, email, address, new ArrayList<>(), minOrderValue);

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
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Lieferanten-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Lieferanten-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Lieferanten: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Lieferanten: " + e.getMessage());
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
                        Main.iconSmall);
                Main.logUtils.addLog("Die CSV-Datei ist leer.");
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
                            UnicodeSymbols.CHECKMARK + " Importiert/Aktualisiert: %d<br/>" +
                            (errors > 0 ? UnicodeSymbols.ERROR + " Fehler: %d<br/>" : "") +
                            "</html>", imported, errors),
                    "Import Ergebnis",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

            System.out.printf("[SettingsGUI] Kunden-Import: %d erfolgreich, %d Fehler%n", imported, errors);
            String logMessage = String.format("Kunden-Import: %d erfolgreich, %d Fehler", imported, errors);
            Main.logUtils.addLog(logMessage);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Fehler beim Importieren: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
            System.err.println("[SettingsGUI] Fehler beim Importieren der Kunden: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Importieren der Kunden: " + e.getMessage());
        }
    }

    private static void importOrdersFromCsv() {
        JOptionPane.showMessageDialog(null,
                "<html><b>Bestellungs-Import nicht verfügbar</b><br/><br/>" +
                        "Der Import von Bestellungen ist aus Sicherheitsgründen deaktiviert.<br/>" +
                        "Bestellungen sollten nur über die normale Bestellfunktion erstellt werden.</html>",
                "Nicht verfügbar",
                JOptionPane.INFORMATION_MESSAGE,
                Main.iconSmall);
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
            Main.logUtils.addLog(errorMsg);
        }

        // Export Vendors
        List<Vendor> vendors = VendorManager.getInstance().getVendors();
        File vendorCsvFile = new File(Main.getAppDataDir(), "vendors_export.csv");
        try (PrintWriter writer = new PrintWriter(new FileWriter(vendorCsvFile))) {
            writer.println("Name,Kontaktperson,Telefon,E-Mail,Adresse,MinBestellwert");
            for (Vendor vendor : vendors) {
                writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        escapeCSV(vendor.getName()),
                        escapeCSV(vendor.getContactPerson()),
                        escapeCSV(vendor.getPhoneNumber()),
                        escapeCSV(vendor.getEmail()),
                        escapeCSV(vendor.getAddress()),
                        escapeCSV(String.valueOf(vendor.getMinOrderValue())));
            }
            System.out.println("[SettingsGUI] Lieferanten erfolgreich nach " + vendorCsvFile.getAbsolutePath() + " exportiert (" + vendors.size() + " Einträge)");
            successCount++;
        } catch (Exception ex) {
            String errorMsg = "Fehler beim Exportieren der Lieferanten: " + ex.getMessage();
            System.err.println("[SettingsGUI] " + errorMsg);
            LogManager.getLogger(SettingsGUI.class).error(errorMsg, ex);
            Main.logUtils.addLog(errorMsg);
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
            Main.logUtils.addLog(errorMsg);
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
            Main.logUtils.addLog(errorMsg);
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

    private void styleComboBox(JComboBox<String> combo) {
        Color bg     = ThemeManager.getInputBackgroundColor();
        Color fg     = ThemeManager.getTextPrimaryColor();
        Color border = ThemeManager.getInputBorderColor();
        Color selBg  = ThemeManager.getSelectionBackgroundColor();
        Color selFg  = ThemeManager.getSelectionForegroundColor();

        combo.setOpaque(true);
        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // IMPORTANT: force popup list colors via renderer AND list defaults
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel c = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                // enforce list base colors too (popup JList)
                list.setBackground(bg);
                list.setForeground(fg);
                list.setSelectionBackground(selBg);
                list.setSelectionForeground(selFg);

                c.setOpaque(true);
                c.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    c.setBackground(selBg);
                    c.setForeground(selFg);
                } else {
                    c.setBackground(bg);
                    c.setForeground(fg);
                }
                return c;
            }
        });

        // Theme arrow button + popup border using a small UI override (most reliable)
        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton b = new JButton("▾");
                b.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                b.setFocusPainted(false);
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(bg);
                b.setForeground(fg);
                b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                b.addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) { b.setBackground(ThemeManager.getSurfaceColor()); }
                    @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
                });
                return b;
            }

            @Override
            protected ComboPopup createPopup() {
                ComboPopup popup = super.createPopup();
                if (popup instanceof BasicComboPopup basic) {
                    basic.setBorder(BorderFactory.createLineBorder(border, 1));
                    basic.getList().setBackground(bg);
                    basic.getList().setForeground(fg);
                    basic.getList().setSelectionBackground(selBg);
                    basic.getList().setSelectionForeground(selFg);
                }
                return popup;
            }
        });

        // If editable: theme the editor field
        if (combo.isEditable()) {
            Component editorComp = combo.getEditor().getEditorComponent();
            if (editorComp instanceof JTextField tf) {
                tf.setBackground(bg);
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBorder(null);
            }
        }
    }

    private static List<String> getAllFonts() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        return Arrays.asList(fontNames);
    }

    /**
     * Convert Color to hex string format for HTML
     */
    private static String toHexColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

}
