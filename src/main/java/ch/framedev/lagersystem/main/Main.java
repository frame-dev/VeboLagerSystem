package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.ThemeManager;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.simplejavautils.Settings;
import ch.framedev.simplejavautils.SimpleJavaUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for VEBO Lagersystem application.
 * Handles initialization, data import, and GUI startup.
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static DatabaseManager databaseManager;
    public static ArticleListGUI articleListGUI;
    public static LogUtils logUtils = new LogUtils();
    public static Settings settings;
    public static ImageIcon icon;
    public static ImageIcon iconSmall;

    public static final String VERSION = "0.2-TESTING";

    public static void main(String[] args) {
        try {
            // Initialize application
            printStartupInfo();
            initializeApplication();

            if (getBooleanSetting("load-from-files")) {
                // Import initial data
                importInitialData();
                // Initialize default user
                initializeDefaultUser();
                logger.info("Initial data import completed.");
            } else {
                logger.info("Initial data import skipped as per settings.");
            }

            // Launch GUI
            launchGUI();

            // Check for updates
            checkForUpdates();

        } catch (Exception e) {
            System.err.println("Kritischer Fehler beim Starten der Anwendung: " + e.getMessage());
            logUtils.addLog("Kritischer Fehler: " + e.getMessage());
            logUtils.addLog("Stack trace: " + getStackTraceAsString(e));
            System.exit(1);
        }
    }

    /**
     * Convert stack trace to string for logging
     */
    private static String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\n  at ").append(element.toString());
        }
        return sb.toString();
    }

    /**
     * Print system and application information at startup
     */
    private static void printStartupInfo() {
        printSeparatorLine();
        System.out.println("Starte VEBO Lager System...");
        System.out.println("Version: " + VERSION);
        printDashedLine();
        printSystemInfo();
        printSeparatorLine();
    }

    /**
     * Print system information (Java version, vendor, OS, memory, architecture)
     */
    private static void printSystemInfo() {
        // Java Information
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("Java Home: " + System.getProperty("java.home"));
        System.out.println("Java Runtime: " + System.getProperty("java.runtime.name") + " " +
                          System.getProperty("java.runtime.version"));

        // Operating System
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("OS Architecture: " + System.getProperty("os.arch"));

        // System Architecture
        System.out.println("Available Processors: " + Runtime.getRuntime().availableProcessors());

        // Memory Information
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory() / (1024 * 1024); // Convert to MB
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long usedMemory = totalMemory - freeMemory;

        System.out.println("Max Memory: " + maxMemory + " MB");
        System.out.println("Total Memory: " + totalMemory + " MB");
        System.out.println("Used Memory: " + usedMemory + " MB");
        System.out.println("Free Memory: " + freeMemory + " MB");

        // User and Directory Information
        System.out.println("User Name: " + System.getProperty("user.name"));
        System.out.println("User Home: " + System.getProperty("user.home"));
        System.out.println("Working Directory: " + System.getProperty("user.dir"));
        System.out.println("File Separator: '" + System.getProperty("file.separator") + "'");

        // Application Data Directory
        System.out.println("App Data Directory: " + getAppDataDir().getAbsolutePath());
    }

    /**
     * Print separator line
     */
    private static void printSeparatorLine() {
        System.out.println("=".repeat(60));
    }

    /**
     * Print dashed line
     */
    private static void printDashedLine() {
        System.out.println("-".repeat(60));
    }

    /**
     * Initialize application settings and database
     */
    private static void initializeApplication() {
        loadApplicationIcons();
        loadSettings();
        initializeTheme();
        ensureAppDataDirectory();
        initializeDatabase();
    }

    /**
     * Load application icons
     */
    private static void loadApplicationIcons() {
        try {
            SimpleJavaUtils utils = new SimpleJavaUtils();
            icon = createScaledIcon(utils, "logo.png", 128, 128);
            iconSmall = createScaledIcon(utils, "logo-small.png", 64, 64);
        } catch (MalformedURLException e) {
            String errorMsg = "Fehler beim Laden des Icons: " + e.getMessage();
            System.err.println(errorMsg);
            logUtils.addLog(errorMsg);
            throw new RuntimeException("Icon konnte nicht geladen werden", e);
        }
    }

    /**
     * Create a scaled ImageIcon from resource file
     */
    private static ImageIcon createScaledIcon(SimpleJavaUtils utils, String resourceName, int width, int height) throws MalformedURLException {
        try {
            ImageIcon originalIcon = new ImageIcon(utils.getFromResourceFile(resourceName).toURI().toURL());
            Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (Exception e) {
            throw new MalformedURLException("Failed to load resource: " + resourceName);
        }
    }

    /**
     * Initialize theme manager and apply theme settings
     */
    private static void initializeTheme() {
        ThemeManager.initialize();
        System.out.println("✓ Theme initialisiert - Dark mode: " + ThemeManager.isDarkMode());
    }

    /**
     * Ensure application data directory exists
     */
    private static void ensureAppDataDirectory() {
        File appDataDir = getAppDataDir();
        if (!appDataDir.exists() && !appDataDir.mkdirs()) {
            String msg = "Konnte Anwendungsdatenverzeichnis nicht erstellen: " + appDataDir.getAbsolutePath();
            System.err.println(msg);
            logUtils.addLog(msg);
            throw new RuntimeException(msg);
        }
    }

    /**
     * Initialize database connection
     */
    private static void initializeDatabase() {
        databaseManager = new DatabaseManager(getAppDataDir().getAbsolutePath(), "vebo_lager_system.db");
        System.out.println("✓ Datenbank initialisiert");
    }

    /**
     * Import all initial data (articles, vendors, departments, clients)
     */
    private static void importInitialData() {
        ImportUtils importUtils = ImportUtils.getInstance();

        importArticles(importUtils);
        importVendors(importUtils);
        importDepartments(importUtils);
        importClients(importUtils);

        System.out.println("✓ Alle Daten importiert");
    }

    /**
     * Import articles from inventory file
     */
    private static void importArticles(ImportUtils importUtils) {
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Map<String, Object>> data = importUtils.loadInventoryFile();

        System.out.println("\n📦 Importiere " + data.size() + " Artikel...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : data) {
            Article article = createArticleFromMap(itemData);
            processArticleImport(articleManager, article, result);
        }

        result.printSummary();
    }

    /**
     * Process single article import
     */
    private static void processArticleImport(ArticleManager articleManager, Article article, ImportResult result) {
        if (ImportUtils.getImportedItems().contains(article.getArticleNumber())) {
            result.incrementSkipped();
            return;
        }

        if (articleManager.insertArticle(article)) {
            result.incrementImported();
            if (!ImportUtils.getImportedItems().contains(article.getArticleNumber())) {
                ImportUtils.addToList(article.getArticleNumber());
            }
            logUtils.addLog("Importierter Artikel: " + article.getName() + " (" + article.getArticleNumber() + ")");
        } else {
            result.incrementSkipped();
        }
    }

    /**
     * Create Article object from map data
     */
    private static Article createArticleFromMap(Map<String, Object> data) {
        return new Article(
                (String) data.get("number"),
                (String) data.get("name"),
                (String) data.get("details"),
                (int) data.get("stockQuantity"),
                (int) data.get("minStockLevel"),
                (Double) data.get("sellPrice"),
                (Double) data.get("buyPrice"),
                (String) data.get("vendorName")
        );
    }

    /**
     * Import vendors from vendor file
     */
    private static void importVendors(ImportUtils importUtils) {
        VendorManager vendorManager = VendorManager.getInstance();
        List<Map<String, Object>> vendorData = importUtils.loadVendorList();

        System.out.println("\n🚚 Importiere " + vendorData.size() + " Lieferanten...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : vendorData) {
            processVendorImport(vendorManager, itemData, result);
        }

        result.printSummary();
    }

    /**
     * Process single vendor import
     */
    private static void processVendorImport(VendorManager vendorManager, Map<String, Object> itemData, ImportResult result) {
        String vendorName = (String) itemData.get("name");

        if (ImportUtils.getImportedItems().contains(vendorName)) {
            result.incrementSkipped();
            return;
        }

        String[] columns = {"contactPerson", "phoneNumber", "email", "address"};
        Object[] dataValues = {
                itemData.get("contactPerson"),
                itemData.get("phoneNumber"),
                itemData.get("email"),
                itemData.get("address")
        };

        if (vendorManager.updateVendor(vendorName, columns, dataValues)) {
            result.incrementImported();
            ImportUtils.addToList(vendorName);
            logUtils.addLog("Importierter Lieferant: " + vendorName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren des Lieferanten: " + vendorName);
        }
    }


    /**
     * Import departments from departments file
     */
    private static void importDepartments(ImportUtils importUtils) {
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        List<Map<String, Object>> departmentData = importUtils.loadDepartmentsList();

        System.out.println("\n🏢 Importiere " + departmentData.size() + " Abteilungen...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : departmentData) {
            processDepartmentImport(departmentManager, itemData, result);
        }

        result.printSummary();
    }

    /**
     * Process single department import
     */
    private static void processDepartmentImport(DepartmentManager departmentManager, Map<String, Object> itemData, ImportResult result) {
        String departmentName = (String) itemData.get("department");
        String kontoNumber = (String) itemData.get("kontoNumber");

        if (shouldSkipImport(departmentName, departmentManager.existsDepartment(departmentName))) {
            result.incrementSkipped();
            return;
        }

        if (departmentManager.insertDepartment(departmentName, kontoNumber)) {
            result.incrementImported();
            ImportUtils.addToList(departmentName);
            logUtils.addLog("Importierte Abteilung: " + departmentName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren der Abteilung: " + departmentName);
        }
    }

    /**
     * Import clients from clients file
     */
    private static void importClients(ImportUtils importUtils) {
        ClientManager clientManager = ClientManager.getInstance();
        List<Map<String, Object>> clientData = importUtils.loadClientsList();

        System.out.println("\n👥 Importiere " + clientData.size() + " Kunden...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : clientData) {
            processClientImport(clientManager, itemData, result);
        }

        result.printSummary();
    }

    /**
     * Process single client import
     */
    private static void processClientImport(ClientManager clientManager, Map<String, Object> itemData, ImportResult result) {
        String firstLastName = (String) itemData.get("firstLastName");
        String department = (String) itemData.get("department");

        if (shouldSkipImport(firstLastName, clientManager.existsClient(firstLastName))) {
            result.incrementSkipped();
            return;
        }

        if (clientManager.insertClient(firstLastName, department)) {
            result.incrementImported();
            ImportUtils.addToList(firstLastName);
            logUtils.addLog("Importierter Kunde: " + firstLastName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren des Kunden: " + firstLastName);
        }
    }

    /**
     * Check if import should be skipped
     */
    private static boolean shouldSkipImport(String itemName, boolean existsInManager) {
        return ImportUtils.getImportedItems().contains(itemName) || existsInManager;
    }

    /**
     * Initialize default user for the application
     */
    private static void initializeDefaultUser() {
        User user = new User("marc", new ArrayList<>());
        UserManager userManager = UserManager.getInstance();

        if (!userManager.existsUser(user.getName())) {
            userManager.insertUser(user);
            System.out.println("\n✓ Standard-Benutzer erstellt");
        }
    }

    /**
     * Launch the main GUI
     */
    private static void launchGUI() {
        System.out.println("\n🚀 Starte GUI...");
        MainGUI mainGUI = new MainGUI();
        mainGUI.display();
        System.out.println("✓ Anwendung gestartet");

        startScheduledTasks();
    }

    /**
     * Start all scheduled tasks based on settings
     */
    private static void startScheduledTasks() {
        SchedulerManager schedulerManager = SchedulerManager.getInstance();
        SchedulerConfig config = loadSchedulerConfig();

        startStockCheckScheduler(schedulerManager, config);
        startWarningDisplayScheduler(schedulerManager, config);
        startQRCodeImportScheduler(schedulerManager, config);
    }

    /**
     * Load scheduler configuration from settings
     */
    private static SchedulerConfig loadSchedulerConfig() {
        return new SchedulerConfig(
                getIntSetting("stock_check_interval", 30),
                getBooleanSetting("enable_auto_stock_check"),
                getBooleanSetting("enable_hourly_warnings"),
                getBooleanSetting("enable_automatic_import_qrcode"),
                getIntSetting("qrcode_import_interval", 10)
        );
    }

    /**
     * Get integer setting with default value
     */
    private static int getIntSetting(String key, int defaultValue) {
        String value = settings.getProperty(key);
        return (value != null) ? Integer.parseInt(value) : defaultValue;
    }

    /**
     * Get boolean setting with default value
     */
    private static boolean getBooleanSetting(String key) {
        String value = settings.getProperty(key);
        return value == null || Boolean.parseBoolean(value);
    }

    /**
     * Start stock check scheduler if enabled
     */
    private static void startStockCheckScheduler(SchedulerManager schedulerManager, SchedulerConfig config) {
        if (config.enableAutoCheck) {
            schedulerManager.startScheduledStockCheck(config.stockCheckInterval, java.util.concurrent.TimeUnit.MINUTES);
            System.out.println("✓ Lagerbestandsprüfung gestartet (Intervall: " + config.stockCheckInterval + " Min.)");
        }
    }

    /**
     * Start warning display scheduler if enabled
     */
    private static void startWarningDisplayScheduler(SchedulerManager schedulerManager, SchedulerConfig config) {
        if (config.enableWarnings) {
            schedulerManager.startHourlyWarningDisplay();
            System.out.println("✓ Stündliche Warnanzeige gestartet");
        }
    }

    /**
     * Start QR code import scheduler if enabled
     */
    private static void startQRCodeImportScheduler(SchedulerManager schedulerManager, SchedulerConfig config) {
        if (config.enableAutomaticImport) {
            schedulerManager.startAutoImportQrCodes(config.automaticImportInterval, java.util.concurrent.TimeUnit.MINUTES);
            System.out.println("✓ Automatischer QR-Code Import gestartet");
        }
    }

    /**
     * Get application data directory path
     *
     * @return File object representing the app data directory
     */
    public static File getAppDataDir() {
        try {
            return UserDataDir.getAppPath("VeboLagerSystem").toFile();
        } catch (Exception e) {
            System.err.println("Konnte Anwendungsdatenverzeichnis nicht ermitteln, verwende aktuelles Verzeichnis.");
            logUtils.addLog("Fehler beim Ermitteln des App-Datenverzeichnisses: " + e.getMessage());
            return new File(".");
        }
    }

    /**
     * Load application settings from properties file
     */
    private static void loadSettings() {
        File settingsFile = ensureSettingsFile();
        settings = new Settings("settings.properties", Main.class, settingsFile);
        System.out.println("✓ Einstellungen geladen");

        applyThemeSettings();
        applyTableFontSettings();
        loadGitHubToken();
    }

    /**
     * Ensure settings file exists
     */
    private static File ensureSettingsFile() {
        File settingsFile = new File(getAppDataDir(), "settings.properties");

        if (!settingsFile.exists()) {
            createSettingsFile(settingsFile);
        }

        return settingsFile;
    }

    /**
     * Create settings file if it doesn't exist
     */
    private static void createSettingsFile(File settingsFile) {
        try {
            if (!settingsFile.createNewFile()) {
                System.err.println("Konnte Einstellungsdatei nicht erstellen: " + settingsFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Fehler beim Erstellen der Einstellungsdatei: " + e.getMessage());
            logUtils.addLog("Fehler beim Erstellen der Einstellungsdatei: " + e.getMessage());
        }
    }

    /**
     * Apply theme settings from configuration
     */
    private static void applyThemeSettings() {
        String darkModeStr = settings.getProperty("dark_mode");
        boolean darkMode = Boolean.parseBoolean(darkModeStr);

        ThemeManager themeManager = ThemeManager.getInstance();
        themeManager.setTheme(darkMode ? ThemeManager.Theme.DARK : ThemeManager.Theme.LIGHT);

        System.out.println("✓ Theme gesetzt: " + (darkMode ? "Dark Mode" : "Light Mode"));
    }

    /**
     * Apply table font settings from configuration
     */
    private static void applyTableFontSettings() {
        String tableFontSizeStr = settings.getProperty("table_font_size");

        if (tableFontSizeStr == null || tableFontSizeStr.isEmpty()) {
            settings.setProperty("table_font_size", String.valueOf(SettingsGUI.TABLE_FONT_SIZE));
        }

        SettingsGUI.TABLE_FONT_SIZE = Integer.parseInt(settings.getProperty("table_font_size"));
    }

    /**
     * Load GitHub token for update manager
     */
    private static void loadGitHubToken() {
        String githubToken = settings.getProperty("github-token");

        if (githubToken != null && !githubToken.isEmpty()) {
            UpdateManager.getInstance().setPersonalToken(githubToken);
            logUtils.addLog("GitHub Token für Update-Manager gesetzt");
        }
    }

    /**
     * Check for application updates from GitHub
     */
    private static void checkForUpdates() {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            if (updateManager.isUpdateAvailable(VERSION)) {
                displayUpdateAvailable(updateManager.getLatestVersion());
            } else {
                System.out.println("✓ Anwendung ist auf dem neuesten Stand");
            }
        } catch (Exception e) {
            handleUpdateCheckError(e);
        }
    }

    /**
     * Display update available message
     */
    private static void displayUpdateAvailable(String latestVersion) {
        System.out.println("\n⚠️  Neue Version verfügbar: " + latestVersion);
        System.out.println("   Aktuelle Version: " + VERSION);
        System.out.println("   Download: https://github.com/frame-dev/VeboLagerSystem/releases/latest");
        logUtils.addLog("Update verfügbar: " + VERSION + " -> " + latestVersion);
    }

    /**
     * Handle update check error
     */
    private static void handleUpdateCheckError(Exception e) {
        System.out.println("⚠️  Konnte nicht auf Updates prüfen: " + e.getMessage());
        logUtils.addLog("Fehler beim Prüfen auf Updates: " + e.getMessage());
    }

    /**
     * Helper class to track import results
     */
    private static class ImportResult {
        private int imported = 0;
        private int skipped = 0;

        public void incrementImported() {
            imported++;
        }

        public void incrementSkipped() {
            skipped++;
        }

        public void printSummary() {
            System.out.println("  → " + imported + " importiert, " + skipped + " übersprungen");
        }
    }

    /**
     * Helper record to hold scheduler configuration
     */
    private record SchedulerConfig(
            int stockCheckInterval,
            boolean enableAutoCheck,
            boolean enableWarnings,
            boolean enableAutomaticImport,
            int automaticImportInterval
    ) {}
}
