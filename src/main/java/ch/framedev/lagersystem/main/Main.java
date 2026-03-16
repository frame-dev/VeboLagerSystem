package ch.framedev.lagersystem.main;

import java.awt.Desktop;
import java.awt.HeadlessException;
import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.guis.SplashscreenGUI;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.SchedulerManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.managers.UpdateManager;
import ch.framedev.lagersystem.managers.UserManager;
import ch.framedev.lagersystem.managers.VendorManager;
import ch.framedev.lagersystem.managers.ThemeManager.LookAndFeelOption;
import ch.framedev.lagersystem.utils.ArticleUtils;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.simplejavautils.Settings;
import ch.framedev.simplejavautils.SimpleJavaUtils;

/**
 * Main entry point for VEBO Lagersystem application.
 * Handles initialization, data import, and GUI startup.
 *
 * @author FrameDev
 */
@SuppressWarnings("SameParameterValue")
public class Main {

    private Main() {
        // Private constructor to prevent instantiation of this utility class
    }

    /**
     * Logger instance for logging application events and errors. Configured to log
     * to both console and file, providing detailed information for debugging and
     * monitoring application behavior.
     */
    private static final Logger logger = LogManager.getLogger(Main.class);
    /**
     * Cached application data directory to avoid redundant lookups. Initialized on
     * first access and reused for subsequent calls to getAppDataDir().
     */
    private static volatile File appDataDirCache;
    /**
     * Flag to ensure that update checks are only performed once per application
     * run, preventing redundant checks and potential performance issues.
     */
    private static final AtomicBoolean updatesChecked = new AtomicBoolean(false);

    /**
     * Database manager instance, responsible for all database interactions.
     * Initialized at startup and used throughout the application.
     */
    public static DatabaseManager databaseManager;
    /**
     * Main GUI instance, responsible for displaying the main application window.
     * Initialized after loading settings and used to manage the main interface.
     */
    public static ArticleListGUI articleListGUI;
    /**
     * Log utility instance, responsible for managing application logs. Initialized
     * at startup and used for logging important events and errors throughout the
     * application.
     */
    public static final LogUtils logUtils = new LogUtils();
    /**
     * Application settings loaded from properties file, accessible throughout the
     * application.
     */
    public static Settings settings;
    /**
     * Application icons loaded from resources, used for GUI and dialogs.
     */
    public static ImageIcon icon;
    /**
     * Smaller icon variant for dialogs and taskbar, loaded from resources.
     */
    public static ImageIcon iconSmall;
    /**
     * Application version string, used for display in the GUI and logging. Should
     * be updated with each release to reflect the current version of the
     * application.
     */
    public static final String VERSION = "0.3-TESTING";

    /**
     * Main method - entry point of the application. Initializes the application,
     * shows the splash screen, and starts the main GUI. Handles any exceptions that
     * occur during startup and logs them appropriately.
     * 
     * @param args command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        try {
            // Initialize application
            printStartupInfo();
            // Start the Splashscreen and initialization in the background
            SplashscreenGUI splashscreen = createAndShowSplashscreen();
            startInitializationWithSplashscreen(splashscreen);

        } catch (Exception e) {
            logger.error("Fehler beim Starten der Anwendung: {}", e.getMessage(), e);
            logUtils.addLog("Kritischer Fehler: " + e.getMessage());
            logUtils.addLog("Stack trace: " + getStackTraceAsString(e));
            System.exit(1);
        }
    }

    /**
     * Convert stack trace to string for logging
     */
    private static String getStackTraceAsString(Throwable e) {
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
     * Print system information (Java version, vendor, OS)
     */
    private static void printSystemInfo() {
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS: " + System.getProperty("os.name") + " | " + System.getProperty("os.version"));
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
    private static void initializeApplication(ProgressListener progressListener) {
        // Setup for initialization steps with progress updates
        updateProgress(progressListener, 3, "Starte Initialisierung...");
        updateProgress(progressListener, 4, "Überprüfen ob mit Internet verbunden...");
        updateProgress(progressListener, 5, "Prüfe Datenverzeichnis...");
        ensureAppDataDirectory();
        updateProgress(progressListener, 6, "Datenverzeichnis bereit...");
        updateProgress(progressListener, 7, "Initialisiere Datenbank...");
        initializeDatabase();
        updateProgress(progressListener, 8, "Datenbank initialisiert...");
        updateProgress(progressListener, 10, "Lade Einstellungen...");
        loadSettings();
        updateProgress(progressListener, 15, "Einstellungen geladen...");
        updateProgress(progressListener, 17, "Prüfe auf Updates (asynchron)...");
        startUpdateCheckAsync();
        updateProgress(progressListener, 19, "Update-Prüfung gestartet...");
        updateProgress(progressListener, 20, "Lade Icons...");
        loadApplicationIcons();
        updateProgress(progressListener, 25, "Icons geladen...");
        updateProgress(progressListener, 34, "Initialisiere Theme...");
        initializeTheme();
        applyLookAndFeelFromSettings();
        updateProgress(progressListener, 35, "Theme gesetzt...");
        handleFirstStartIfNeeded(progressListener);
        updateProgress(progressListener, 96, "Abschluss der Initialisierung...");
        System.out.println(ArticleUtils.getPartsFromDetails(ArticleManager.getInstance().getArticleByNumber("1213").getDetails(), true));
    }

    private static void handleFirstStartIfNeeded(ProgressListener progressListener) {
        String firstTimeSetting = settings.getProperty("first-time");
        if (firstTimeSetting != null && firstTimeSetting.equalsIgnoreCase("true")) {
            return;
        }

        settings.setProperty("first-time", "true");
        updateProgress(progressListener, 54, "Erster Start...");

        boolean loadFromFiles = askForInitialDataImport(progressListener);
        settings.setProperty("load-from-files", String.valueOf(loadFromFiles));
        settings.save();

        if (!loadFromFiles) {
            logger.info("Initial data import skipped as per settings.");
            return;
        }

        updateProgress(progressListener, 72, "Importiere Startdaten...");
        importInitialData(progressListener);
        updateProgress(progressListener, 80, "Startdaten importiert...");
        updateProgress(progressListener, 88, "Erstelle Standard-Benutzer...");
        initializeDefaultUser();
        updateProgress(progressListener, 90, "Standard-Benutzer erstellt...");
        logger.info("Initial data import completed.");
    }

    private static boolean askForInitialDataImport(ProgressListener progressListener) {
        int result = showConfirmDialogOnEdt(
                "Willkommen zum VEBO Lagersystem!\nMöchten Sie die anfänglichen Daten jetzt importieren?",
                "Erster Start",
                iconSmall);

        if (result != JOptionPane.YES_OPTION) {
            return false;
        }

        updateProgress(progressListener, 62, "QR-Code Abfrage...");
        int resultQr = showConfirmDialogOnEdt(
                "QR-Codes Erstellen?",
                "QR-Codes",
                null);
        if (resultQr == JOptionPane.YES_OPTION) {
            updateProgress(progressListener, 68, "Erstelle QR-Codes...");
            logger.info("QR-Codes werden erstellt...");
            List<File> qrCodeFiles = QRCodeUtils.createQrCodes(ArticleManager.getInstance().getAllArticles());
            for (File qrCodeFile : qrCodeFiles) {
                logger.info("QR-Code erstellt: {}", qrCodeFile.getAbsolutePath());
            }
            logger.info("QR-Codes erstellt.");
        }
        updateProgress(progressListener, 63, "QR-Code Abfrage abgeschlossen.");
        return true;
    }

    private static void startUpdateCheckAsync() {
        CompletableFuture.runAsync(Main::checkForUpdatesOnce)
                .exceptionally(e -> {
                    logger.warn("Asynchrone Update-Prüfung fehlgeschlagen: {}", e.getMessage(), e);
                    return null;
                });
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
     * Create a scaled ImageIcon from a resource file
     */
    private static ImageIcon createScaledIcon(SimpleJavaUtils utils, String resourceName, int width, int height)
            throws MalformedURLException {
        try {
            ImageIcon originalIcon = new ImageIcon(utils.getFromResourceFile(resourceName).toURI().toURL());
            Image scaledImage = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);
        } catch (MalformedURLException e) {
            logger.error("Fehler beim Laden des Icons: {}", e.getMessage(), e);
            logUtils.addLog("Fehler beim Laden des Icons: " + e.getMessage());
            throw new MalformedURLException("Failed to load resource: " + resourceName);
        }
    }

    /**
     * Initialize the theme manager and apply theme settings
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
        } else {
            logger.info("Anwendungsdatenverzeichnis bereit: {}", appDataDir.getAbsolutePath());
        }
    }

    /**
     * Initialize database connection
     */
    private static void initializeDatabase() {
        databaseManager = new DatabaseManager(getAppDataDir().getAbsolutePath(), "vebo_lager_system.db");
        System.out.println("✓ Datenbank initialisiert");
        logger.info("Database initialized.");
        logUtils.addLog("Datenbank initialisiert.");
    }

    /**
     * Import all initial data (articles, vendors, departments, clients)
     */
    private static void importInitialData(ProgressListener progressListener) {
        ImportUtils importUtils = ImportUtils.getInstance();
        java.util.Set<String> importedItems = new java.util.HashSet<>(ImportUtils.getImportedItems());

        updateProgress(progressListener, 76, "Importiere Artikel...");
        importArticles(importUtils, importedItems);
        updateProgress(progressListener, 80, "Importiere Lieferanten...");
        importVendors(importUtils, importedItems);
        updateProgress(progressListener, 83, "Importiere Abteilungen...");
        importDepartments(importUtils, importedItems);
        updateProgress(progressListener, 86, "Importiere Kunden...");
        importClients(importUtils, importedItems);

        System.out.println("✓ Alle Daten importiert");
    }

    /**
     * Import articles from inventory file
     */
    private static void importArticles(ImportUtils importUtils, java.util.Set<String> importedItems) {
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Map<String, Object>> data = importUtils.loadInventoryFile();

        System.out.println("\nImportiere " + data.size() + " Artikel...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : data) {
            Article article = createArticleFromMap(itemData);
            processArticleImport(articleManager, article, result, importedItems);
        }

        result.printSummary();
    }

    /**
     * Process single article import
     */
    private static void processArticleImport(ArticleManager articleManager, Article article, ImportResult result,
            java.util.Set<String> importedItems) {
        if (importedItems.contains(article.getArticleNumber())) {
            result.incrementSkipped();
            return;
        }

        if (articleManager.insertArticle(article)) {
            result.incrementImported();
            importedItems.add(article.getArticleNumber());
            ImportUtils.addToList(article.getArticleNumber());
            logUtils.addLog("Importierter Artikel: " + article.getName() + " (" + article.getArticleNumber() + ")");
        } else {
            result.incrementSkipped();
        }
    }

    /**
     * Create Article object from map data
     */
    private static Article createArticleFromMap(Map<String, Object> data) {
        String number = getString(data, "number");
        String name = getString(data, "name");
        String details = getString(data, "details");
        int stockQuantity = getInt(data, "stockQuantity");
        int minStockLevel = getInt(data, "minStockLevel");
        double sellPrice = getDouble(data, "sellPrice");
        double buyPrice = getDouble(data, "buyPrice");
        String vendorName = getString(data, "vendorName");

        return new Article(
                number,
                name,
                details,
                stockQuantity,
                minStockLevel,
                sellPrice,
                buyPrice,
                vendorName);
    }

    private static String getString(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        return value == null ? "" : value.toString();
    }

    private static int getInt(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private static double getDouble(Map<String, Object> data, String key) {
        Object value = data == null ? null : data.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return 0.0;
    }

    /**
     * Import vendors from vendor file
     */
    private static void importVendors(ImportUtils importUtils, java.util.Set<String> importedItems) {
        VendorManager vendorManager = VendorManager.getInstance();
        List<Map<String, Object>> vendorData = importUtils.loadVendorList();

        System.out.println("\n🚚 Importiere " + vendorData.size() + " Lieferanten...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : vendorData) {
            processVendorImport(vendorManager, itemData, result, importedItems);
        }

        result.printSummary();
    }

    /**
     * Process single vendor import
     */
    private static void processVendorImport(VendorManager vendorManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems) {
        String vendorName = getString(itemData, "name");

        if (vendorName.isBlank()) {
            result.incrementSkipped();
            logUtils.addLog("Lieferant ohne Namen beim Import übersprungen");
            return;
        }

        if (importedItems.contains(vendorName)) {
            result.incrementSkipped();
            return;
        }
        String contactPerson = getString(itemData, "contactPerson");
        String phoneNumber = getString(itemData, "phoneNumber");
        String email = getString(itemData, "email");
        String address = getString(itemData, "address");

        boolean success;
        if (vendorManager.existsVendor(vendorName)) {
            String[] columns = { "contactPerson", "phoneNumber", "email", "address" };
            Object[] dataValues = {
                    contactPerson, phoneNumber, email, address
            };
            success = vendorManager.updateVendor(vendorName, columns, dataValues);
        } else {
            Vendor vendor = new Vendor(vendorName, contactPerson, phoneNumber, email, address, new ArrayList<>(), 0.0);
            success = vendorManager.insertVendor(vendor);
        }

        if (success) {
            result.incrementImported();
            importedItems.add(vendorName);
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
    private static void importDepartments(ImportUtils importUtils, java.util.Set<String> importedItems) {
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        List<Map<String, Object>> departmentData = importUtils.loadDepartmentsList();

        System.out.println("\nImportiere " + departmentData.size() + " Abteilungen...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : departmentData) {
            processDepartmentImport(departmentManager, itemData, result, importedItems);
        }

        result.printSummary();
    }

    /**
     * Process single department import
     */
    private static void processDepartmentImport(DepartmentManager departmentManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems) {
        String departmentName = getString(itemData, "department");
        String kontoNumber = getString(itemData, "kontoNumber");

        if (shouldSkipImport(departmentName, departmentManager.existsDepartment(departmentName), importedItems)) {
            result.incrementSkipped();
            return;
        }

        if (departmentManager.insertDepartment(departmentName, kontoNumber)) {
            result.incrementImported();
            importedItems.add(departmentName);
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
    private static void importClients(ImportUtils importUtils, java.util.Set<String> importedItems) {
        ClientManager clientManager = ClientManager.getInstance();
        List<Map<String, Object>> clientData = importUtils.loadClientsList();

        System.out.println("\n👥 Importiere " + clientData.size() + " Kunden...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : clientData) {
            processClientImport(clientManager, itemData, result, importedItems);
        }

        result.printSummary();
    }

    /**
     * Process single client import
     */
    private static void processClientImport(ClientManager clientManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems) {
        String firstLastName = getString(itemData, "firstLastName");
        String department = getString(itemData, "department");

        if (shouldSkipImport(firstLastName, clientManager.existsClient(firstLastName), importedItems)) {
            result.incrementSkipped();
            return;
        }

        if (clientManager.insertClient(firstLastName, department)) {
            result.incrementImported();
            importedItems.add(firstLastName);
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
    private static boolean shouldSkipImport(String itemName, boolean existsInManager,
            java.util.Set<String> importedItems) {
        return importedItems.contains(itemName) || existsInManager;
    }

    /**
     * Initialize the default user for the application
     */
    private static void initializeDefaultUser() {
        User user = new User("marc", new ArrayList<>());
        UserManager userManager = UserManager.getInstance();

        if (!userManager.existsUser(user.getName())) {
            userManager.insertUser(user);
            System.out.println("\nOK Standard-Benutzer erstellt");
        }
    }

    /**
     * Launch the main GUI
     */
    private static void launchGUI() {
        Runnable r = () -> {
            System.out.println("\n🚀 Starte GUI...");
            MainGUI mainGUI = new MainGUI();
            mainGUI.display();
            System.out.println("✓ Anwendung gestartet");

            startScheduledTasks();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
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
                getPositiveIntSetting("stock_check_interval", 30),
                getBooleanSetting("enable_auto_stock_check", true),
                getBooleanSetting("enable_hourly_warnings", true, "enable_houtly_warnings"),
                getBooleanSetting("enable_automatic_import_qrcode", true),
                getPositiveIntSetting("qrcode_import_interval", 10));
    }

    /**
     * Get integer setting with default value
     */
    private static int getIntSetting(String key, int defaultValue) {
        String value = settings.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get positive integer setting with default value fallback.
     */
    private static int getPositiveIntSetting(String key, int defaultValue) {
        int value = getIntSetting(key, defaultValue);
        return value > 0 ? value : defaultValue;
    }

    /**
     * Get boolean setting with a default value
     */
    private static boolean getBooleanSetting(String key, boolean defaultValue) {
        String value = settings.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * Get boolean setting with support for legacy key aliases.
     */
    private static boolean getBooleanSetting(String key, boolean defaultValue, String... aliases) {
        String value = settings.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }

        if (aliases != null) {
            for (String alias : aliases) {
                if (alias == null || alias.isBlank()) {
                    continue;
                }

                String aliasValue = settings.getProperty(alias);
                if (aliasValue != null) {
                    logger.info("Using legacy setting key '{}' for '{}'.", alias, key);
                    return Boolean.parseBoolean(aliasValue);
                }
            }
        }

        return defaultValue;
    }

    /**
     * Applies look-and-feel from settings with a safe fallback.
     */
    private static void applyLookAndFeelFromSettings() {
        String lookAndFeel = settings.getProperty("look_and_feel");
        if (lookAndFeel != null && !lookAndFeel.isBlank()) {
            ThemeManager.setLookAndFeel(lookAndFeel.trim());
        } else {
            ThemeManager.setLookAndFeel(LookAndFeelOption.SYSTEM);
        }
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
            schedulerManager.startAutoImportQrCodes(config.automaticImportInterval,
                    java.util.concurrent.TimeUnit.MINUTES);
            System.out.println("✓ Automatischer QR-Code Import gestartet");
        }
    }

    /**
     * Get application data directory path
     *
     * @return File object representing the app data directory
     */
    public static File getAppDataDir() {
        File cached = appDataDirCache;
        if (cached != null) {
            return cached;
        }
        try {
            File resolved = UserDataDir.getAppPath("VeboLagerSystem").toFile();
            appDataDirCache = resolved;
            return resolved;
        } catch (FileNotFoundException e) {
            logger.error("Could not get VeboLagerSystem directory.", e);
            logUtils.addLog("Fehler beim Ermitteln des App-Datenverzeichnisses: " + e.getMessage());
            File fallback = new File(".");
            appDataDirCache = fallback;
            return fallback;
        }
    }

    /**
     * Load application settings from a properties file
     */
    private static void loadSettings() {
        File settingsFile = ensureSettingsFile();
        settings = new Settings("settings.properties", Main.class, settingsFile);
        if (!settings.contains("first-time")) {
            settings.setProperty("first-time", "false");
            settings.save();
        }
        System.out.println("✓ Einstellungen geladen");

        applyThemeSettings();
        applyTableFontSettings();
        loadGitHubToken();
        SimpleJavaUtils utils = new SimpleJavaUtils();
        try {
            if (!new File(getAppDataDir(), "categories.json").exists()) {
                Files.copy(utils.getFromResourceFile("categories.json", Main.class).toPath(),
                        new File(getAppDataDir(), "categories.json").toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensure a settings file exists
     */
    private static File ensureSettingsFile() {
        File settingsFile = new File(getAppDataDir(), "settings.properties");

        if (!settingsFile.exists()) {
            createSettingsFile(settingsFile);
        } else {
            logger.info("Settings file already exists: {}", settingsFile.getAbsolutePath());
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
            } else {
                logger.info("Settings file created: {}", settingsFile.getAbsolutePath());
                logUtils.addLog("Einstellungsdatei erstellt: " + settingsFile.getAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Fehler beim Erstellen der Einstellungsdatei: {}", e.getMessage(), e);
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
        int fontSize = getPositiveIntSetting("table_font_size", SettingsGUI.TABLE_FONT_SIZE);
        SettingsGUI.TABLE_FONT_SIZE = fontSize;

        // Persist normalized value so invalid/empty entries don't reappear on restart.
        String normalized = String.valueOf(fontSize);
        String current = settings.getProperty("table_font_size");
        if (!normalized.equals(current)) {
            settings.setProperty("table_font_size", normalized);
            settings.save();
        }
    }

    /**
     * Load GitHub token for update manager
     */
    private static void loadGitHubToken() {
        String githubToken = settings.getProperty("github-token");

        if (githubToken != null && !githubToken.isEmpty()) {
            UpdateManager.getInstance().setPersonalToken(githubToken);
            logUtils.addLog("GitHub Token für Update-Manager gesetzt");
        } else {
            logUtils.addLog("Kein GitHub Token für Update-Manager gesetzt");
        }
    }

    /**
     * Check for application updates from GitHub across all release channels
     */
    private static void checkForUpdates() {
        try {
            UpdateManager updateManager = UpdateManager.getInstance();

            // Check all channels
            UpdateManager.ChannelUpdateResult channelResult = updateManager.checkAllChannels();

            if (channelResult == null) {
                System.out.println("⚠  Konnte nicht auf Updates prüfen (keine Verbindung zu GitHub)");
                return;
            }

            // Log current version and channel
            UpdateManager.ReleaseChannel currentChannel = UpdateManager.detectChannel(VERSION);
            System.out.println("✓ Aktuelle Version: " + VERSION + " (" + currentChannel + ")");

            // Check for updates in the current channel
            UpdateManager.VersionComparisonResult comparison = updateManager.compareWithLatest();

            if (comparison == null) {
                System.out.println("⚠  Konnte nicht auf Updates prüfen (keine Verbindung zu GitHub)");
                return;
            }

            // Display console output
            if (comparison.updateAvailable()) {
                System.out.println("⚠  Update verfügbar in deinem Channel: " + comparison.latestVersion());
            } else if (comparison.isCurrent()) {
                System.out.println("✓ Anwendung ist auf dem neuesten Stand");
            } else if (comparison.isNewer()) {
                System.out.println(
                        "✓ Entwicklungsversion (neuer als letzte Release: " + comparison.latestVersion() + ")");
            }

            // Log all available channels
            if (channelResult.stableVersion() != null) {
                System.out.println("  → Stable: " + channelResult.stableVersion());
            }
            if (channelResult.betaVersion() != null) {
                System.out.println("  → Beta: " + channelResult.betaVersion());
            }
            if (channelResult.alphaVersion() != null) {
                System.out.println("  → Alpha: " + channelResult.alphaVersion());
            }
            if (channelResult.testingVersion() != null) {
                System.out.println("  → Testing: " + channelResult.testingVersion());
            }

            // Show GUI dialog if any update is available
            if (channelResult.hasStableUpdate() || channelResult.hasBetaUpdate() || channelResult.hasAlphaUpdate()
                    || channelResult.hasTestingUpdate()) {
                displayUpdateDialog(channelResult);
            }

        } catch (Exception e) {
            handleUpdateCheckError(e);
        }
    }

    /**
     * Display comprehensive update dialog showing all available channels
     */
    private static void displayUpdateDialog(UpdateManager.ChannelUpdateResult channelResult) {
        SwingUtilities.invokeLater(() -> {
            StringBuilder messageBuilder = new StringBuilder();
            messageBuilder.append("<html><body style='width: 450px; padding: 10px;'>");
            messageBuilder.append("<h2 style='color: #2c3e50; margin-bottom: 10px;'>🎉 Updates verfügbar!</h2>");
            messageBuilder.append("<p style='margin: 10px 0;'><b>Aktuelle Version:</b> ")
                    .append(VERSION)
                    .append(" (")
                    .append(channelResult.currentChannel())
                    .append(")</p>");

            messageBuilder.append("<hr style='margin: 15px 0; border: none; border-top: 1px solid #ccc;'>");
            messageBuilder.append("<h3 style='color: #34495e; margin: 10px 0;'>Verfügbare Versionen:</h3>");

            // Build list of available updates
            boolean hasUpdates = false;

            if (channelResult.hasStableUpdate()) {
                messageBuilder.append("<p style='margin: 8px 0; padding-left: 10px;'>")
                        .append("<b style='color: #27ae60;'>✓ Stable:</b> ")
                        .append(channelResult.stableVersion())
                        .append(" <i style='color: #7f8c8d;'>(empfohlen)</i></p>");
                hasUpdates = true;
            }

            if (channelResult.hasBetaUpdate()) {
                messageBuilder.append("<p style='margin: 8px 0; padding-left: 10px;'>")
                        .append("<b style='color: #f39c12;'>⚠ Beta:</b> ")
                        .append(channelResult.betaVersion())
                        .append(" <i style='color: #7f8c8d;'>(testing)</i></p>");
                hasUpdates = true;
            }

            if (channelResult.hasAlphaUpdate()) {
                messageBuilder.append("<p style='margin: 8px 0; padding-left: 10px;'>")
                        .append("<b style='color: #e74c3c;'>⚡ Alpha:</b> ")
                        .append(channelResult.alphaVersion())
                        .append(" <i style='color: #7f8c8d;'>(experimental)</i></p>");
                hasUpdates = true;
            }

            if (channelResult.hasTestingUpdate()) {
                messageBuilder.append("<p style='margin: 8px 0; padding-left: 10px;'>")
                        .append("<b style='color: #8e44ad;'>🔧 Testing:</b> ")
                        .append(channelResult.testingVersion())
                        .append(" <i style='color: #7f8c8d;'>(für Entwickler)</i></p>");
                hasUpdates = true;
            }

            if (!hasUpdates) {
                return; // No updates to show
            }

            messageBuilder.append("<hr style='margin: 15px 0; border: none; border-top: 1px solid #ccc;'>");
            messageBuilder.append("<p style='margin-top: 15px; color: #555;'>")
                    .append("Möchten Sie die Download-Seite öffnen, um die gewünschte Version herunterzuladen?</p>");
            messageBuilder.append("</body></html>");

            int option = JOptionPane.showConfirmDialog(
                    null,
                    messageBuilder.toString(),
                    "Updates verfügbar",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    iconSmall);

            if (option == JOptionPane.YES_OPTION) {
                openDownloadPage();
            }
        });
    }

    /**
     * Open the GitHub releases page in the default browser
     */
    private static void openDownloadPage() {
        try {
            String downloadUrl = "https://github.com/frame-dev/VeboLagerSystem/releases";
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(java.net.URI.create(downloadUrl));
                logger.info("Opened releases page in browser: {}", downloadUrl);
            } else {
                // Fallback: show URL in a dialog
                JOptionPane.showMessageDialog(
                        null,
                        "Bitte öffnen Sie diesen Link in Ihrem Browser:\n" + downloadUrl,
                        "Download-Link",
                        JOptionPane.INFORMATION_MESSAGE,
                        iconSmall);
                logger.warn("Browser not supported, showed URL in dialog instead");
            }
        } catch (HeadlessException | IOException e) {
            logger.error("Failed to open browser: {}", e.getMessage(), e);
            JOptionPane.showMessageDialog(
                    null,
                    """
                            Fehler beim Öffnen des Browsers.
                            Bitte besuchen Sie manuell:
                            https://github.com/frame-dev/VeboLagerSystem/releases""",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    iconSmall);
        }
    }

    /**
     * Handle update check error
     */
    private static void handleUpdateCheckError(Exception e) {
        logger.error("Failed to check for updates: {}", e.getMessage(), e);
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
            int automaticImportInterval) {
    }

    private interface ProgressListener {
        void onProgress(int percent, String message);
    }

    private record ProgressUpdate(int percent, String message) {
    }

    private static void updateProgress(ProgressListener progressListener, int percent, String message) {
        if (progressListener != null) {
            progressListener.onProgress(percent, message);
        }
        // Keep progress pacing sleep, but never block the EDT so splash animations stay fluid.
        if (!SwingUtilities.isEventDispatchThread()) {
            sleepQuietly();
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep((long) 200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static SplashscreenGUI createAndShowSplashscreen() {
        try {
            final SplashscreenGUI[] splashRef = new SplashscreenGUI[1];
            SwingUtilities.invokeAndWait(() -> {
                splashRef[0] = new SplashscreenGUI();
                splashRef[0].display();
            });
            return splashRef[0];
        } catch (InterruptedException | InvocationTargetException e) {
            throw new RuntimeException("Konnte Splashscreen nicht starten", e);
        }
    }

    private static void startInitializationWithSplashscreen(SplashscreenGUI splashscreen) {
        SwingWorker<Void, ProgressUpdate> worker = new SwingWorker<>() {
            private Exception initException;

            @Override
            protected Void doInBackground() {
                try {
                    initializeApplication((percent, message) -> {
                        logSplashProgress(percent, message);
                        publish(new ProgressUpdate(percent, message));
                    });
                } catch (Exception e) {
                    initException = e;
                }
                return null;
            }

            @Override
            protected void process(List<ProgressUpdate> chunks) {
                if (chunks.isEmpty()) {
                    return;
                }
                for (ProgressUpdate update : chunks) {
                    splashscreen.updateProgress(update.percent(), update.message());
                }
            }

            @Override
            protected void done() {
                if (initException != null) {
                    logger.error("Fehler beim Starten der Anwendung: {}", initException.getMessage(), initException);
                    logUtils.addLog("Kritischer Fehler: " + initException.getMessage());
                    logUtils.addLog("Stack trace: " + getStackTraceAsString(initException));
                    splashscreen.close();
                    JOptionPane.showMessageDialog(
                            null,
                            "Die Anwendung konnte nicht gestartet werden.\nDetails: " + initException.getMessage(),
                            "Startfehler",
                            JOptionPane.ERROR_MESSAGE);
                    System.exit(1);
                    return;
                }

                splashscreen.updateProgress(100, "Starte Hauptfenster...");
                splashscreen.close();
                ImportUtils.loadSeparatedArticles();

                // Launch GUI
                launchGUI();
            }
        };
        worker.execute();
    }

    private static void checkForUpdatesOnce() {
        if (!updatesChecked.compareAndSet(false, true)) {
            return;
        }
        checkForUpdates();
    }

    private static int showConfirmDialogOnEdt(Object message, String title, Icon icon) {
        if (SwingUtilities.isEventDispatchThread()) {
            return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE, icon);
        }
        final int[] result = new int[1];
        try {
            SwingUtilities.invokeAndWait(() -> result[0] = JOptionPane.showConfirmDialog(
                    null,
                    message,
                    title,
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    icon));
        } catch (InterruptedException | InvocationTargetException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Dialog konnte nicht angezeigt werden", e);
        }
        return result[0];
    }

    private static void logSplashProgress(int percent, String message) {
        String safeMessage = message == null ? "" : message;
        System.out.println("[Splash] " + percent + "% - " + safeMessage);
    }
}
