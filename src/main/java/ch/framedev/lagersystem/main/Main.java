package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.simplejavautils.Settings;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main entry point for VEBO Lagersystem application.
 * Handles initialization, data import, and GUI startup.
 */
public class Main {

    public static DatabaseManager databaseManager;
    public static ArticleListGUI articleListGUI;
    public static LogUtils logUtils = new LogUtils();
    public static Settings settings;

    public static final String VERSION = "0.1-TESTING";

    public static void main(String[] args) {
        try {
            // Initialize application
            printStartupInfo();
            initializeApplication();

            // Import initial data
            importInitialData();

            // Initialize default user
            initializeDefaultUser();

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
        System.out.println("=".repeat(60));
        System.out.println("Starte VEBO Lager System...");
        System.out.println("Version: " + VERSION);
        System.out.println("-".repeat(60));
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("=".repeat(60));
    }

    /**
     * Initialize application settings and database
     */
    private static void initializeApplication() {
        loadSettings();
        ensureAppDataDirectory();
        initializeDatabase();
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
        int imported = 0, skipped = 0;

        for (Map<String, Object> itemData : data) {
            Article article = createArticleFromMap(itemData);

            if (ImportUtils.getImportedItems().contains(article.getArticleNumber())) {
                skipped++;
                continue;
            }
            if (articleManager.insertArticle(article)) {
                imported++;
                if (!ImportUtils.getImportedItems().contains(article.getArticleNumber())) {
                    ImportUtils.addToList(article.getArticleNumber());
                }
                logUtils.addLog("Importierter Artikel: " + article.getName() + " (" + article.getArticleNumber() + ")");
            } else {
                skipped++;
            }
        }

        System.out.println("  → " + imported + " importiert, " + skipped + " übersprungen");
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
        int imported = 0, skipped = 0;

        for (Map<String, Object> itemData : vendorData) {
            String vendorName = (String) itemData.get("name");

            if (ImportUtils.getImportedItems().contains(vendorName)) {
                skipped++;
                continue;
            }

            String[] columns = {"contactPerson", "phoneNumber", "email", "address"};
            Object[] dataValues = {
                itemData.get("contactPerson"),
                itemData.get("phoneNumber"),
                itemData.get("email"),
                itemData.get("address")
            };

            if (vendorManager.updateVendor(vendorName, columns, dataValues)) {
                imported++;
                ImportUtils.addToList(vendorName);
                logUtils.addLog("Importierter Lieferant: " + vendorName);
            } else {
                skipped++;
                logUtils.addLog("Fehler beim Importieren des Lieferanten: " + vendorName);
            }
        }

        System.out.println("  → " + imported + " importiert, " + skipped + " übersprungen");
    }

    /**
     * Import departments from departments file
     */
    private static void importDepartments(ImportUtils importUtils) {
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        List<Map<String, Object>> departmentData = importUtils.loadDepartmentsList();

        System.out.println("\n🏢 Importiere " + departmentData.size() + " Abteilungen...");
        int imported = 0, skipped = 0;

        for (Map<String, Object> itemData : departmentData) {
            String departmentName = (String) itemData.get("department");
            String kontoNumber = (String) itemData.get("kontoNumber");

            if (ImportUtils.getImportedItems().contains(departmentName) ||
                departmentManager.existsDepartment(departmentName)) {
                skipped++;
                continue;
            }

            if (departmentManager.insertDepartment(departmentName, kontoNumber)) {
                imported++;
                ImportUtils.addToList(departmentName);
                logUtils.addLog("Importierte Abteilung: " + departmentName);
            } else {
                skipped++;
                logUtils.addLog("Fehler beim Importieren der Abteilung: " + departmentName);
            }
        }

        System.out.println("  → " + imported + " importiert, " + skipped + " übersprungen");
    }

    /**
     * Import clients from clients file
     */
    private static void importClients(ImportUtils importUtils) {
        ClientManager clientManager = ClientManager.getInstance();
        List<Map<String, Object>> clientData = importUtils.loadClientsList();

        System.out.println("\n👥 Importiere " + clientData.size() + " Kunden...");
        int imported = 0, skipped = 0;

        for (Map<String, Object> itemData : clientData) {
            String firstLastName = (String) itemData.get("firstLastName");
            String department = (String) itemData.get("department");

            if (ImportUtils.getImportedItems().contains(firstLastName) ||
                clientManager.existsClient(firstLastName)) {
                skipped++;
                continue;
            }

            if (clientManager.insertClient(firstLastName, department)) {
                imported++;
                ImportUtils.addToList(firstLastName);
                logUtils.addLog("Importierter Kunde: " + firstLastName);
            } else {
                skipped++;
                logUtils.addLog("Fehler beim Importieren des Kunden: " + firstLastName);
            }
        }

        System.out.println("  → " + imported + " importiert, " + skipped + " übersprungen");
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

        // Starte Scheduler mit Einstellungen aus settings.properties
        SchedulerManager schedulerManager = SchedulerManager.getInstance();

        // Lade Einstellungen
        String intervalStr = settings.getProperty("stock_check_interval");
        int interval = (intervalStr != null) ? Integer.parseInt(intervalStr) : 30;

        String enableAutoCheckStr = settings.getProperty("enable_auto_stock_check");
        boolean enableAutoCheck = enableAutoCheckStr == null || Boolean.parseBoolean(enableAutoCheckStr);

        String enableWarningsStr = settings.getProperty("enable_hourly_warnings");
        boolean enableWarnings = enableWarningsStr == null || Boolean.parseBoolean(enableWarningsStr);

        // Starte Scheduler basierend auf Einstellungen
        if (enableAutoCheck) {
            schedulerManager.startScheduledStockCheck(interval, java.util.concurrent.TimeUnit.MINUTES);
            System.out.println("✓ Lagerbestandsprüfung gestartet (Intervall: " + interval + " Min.)");
        }

        if (enableWarnings) {
            schedulerManager.startHourlyWarningDisplay();
            System.out.println("✓ Stündliche Warnanzeige gestartet");
        }
    }

    /**
     * Get application data directory path
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
        File settingsFile = new File(getAppDataDir(), "settings.properties");

        if (!settingsFile.exists()) {
            try {
                if (!settingsFile.createNewFile()) {
                    System.err.println("Konnte Einstellungsdatei nicht erstellen: " + settingsFile.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Erstellen der Einstellungsdatei: " + e.getMessage());
                logUtils.addLog("Fehler beim Erstellen der Einstellungsdatei: " + e.getMessage());
            }
        }

        settings = new Settings("settings.properties", Main.class, settingsFile);
        System.out.println("✓ Einstellungen geladen");

        // Load GitHub token if configured
        String githubToken = settings.getProperty("github-token");
        if (githubToken != null && !githubToken.isEmpty()) {
            UpdateManager.getInstance().setPersonalToken(githubToken);
        }
    }

    /**
     * Check for application updates from GitHub
     */
    private static void checkForUpdates() {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            if (updateManager.isUpdateAvailable(VERSION)) {
                String latestVersion = updateManager.getLatestVersion();
                System.out.println("\n⚠️  Neue Version verfügbar: " + latestVersion);
                System.out.println("   Aktuelle Version: " + VERSION);
                System.out.println("   Download: https://github.com/frame-dev/VeboLagerSystem/releases/latest");
                logUtils.addLog("Update verfügbar: " + VERSION + " -> " + latestVersion);
            } else {
                System.out.println("✓ Anwendung ist auf dem neuesten Stand");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Konnte nicht auf Updates prüfen: " + e.getMessage());
            logUtils.addLog("Fehler beim Prüfen auf Updates: " + e.getMessage());
        }
    }
}
