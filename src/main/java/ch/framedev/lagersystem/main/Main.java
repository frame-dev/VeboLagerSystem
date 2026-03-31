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
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
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
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.lagersystem.utils.Settings;
import ch.framedev.simplejavautils.SimpleJavaUtils;

/**
 * Main entry point for VEBO Lagersystem application.
 * Handles initialization, data import, and GUI startup or Server starting based
 * on command-line arguments. Provides utility methods for accessing application
 * data directory and loading settings.
 * GUI is only supported on desktop platforms. For server mode, run with
 * "server" argument to start ScanServer (can run in CLI).
 * 
 * @author FrameDev
 */
@SuppressWarnings("SameParameterValue")
public class Main {

    private enum CommandMode {
        DESKTOP,
        SERVER,
        FORCE_IMPORT,
        FORCE_CREATE_QR
    }

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
        if (runCommandMode(args)) {
            return;
        }

        startDesktopApplication();
    }

    private static boolean runCommandMode(String[] args) {
        CommandMode commandMode = resolveCommandMode(args);
        switch (commandMode) {
            case SERVER -> runServerMode(args);
            case FORCE_IMPORT -> forceImportData();
            case FORCE_CREATE_QR -> createQrCodesForAllArticles();
            case DESKTOP -> {
                return false;
            }
        }
        return true;
    }

    private static CommandMode resolveCommandMode(String[] args) {
        if (args.length == 0) {
            return CommandMode.DESKTOP;
        }

        String mode = args[0];
        if ("server".equalsIgnoreCase(mode)) {
            return CommandMode.SERVER;
        }
        if ("force-import".equalsIgnoreCase(mode)) {
            return CommandMode.FORCE_IMPORT;
        }
        if ("force-create-qr".equalsIgnoreCase(mode)) {
            return CommandMode.FORCE_CREATE_QR;
        }
        return CommandMode.DESKTOP;
    }

    private static void createQrCodesForAllArticles() {
        runCliOperation(
                "Starte erzwungenes Erstellen von QR-Codes für alle Artikel...",
                "Fehler beim erzwungenen Erstellen der QR-Codes",
                () -> {
            initializeCliDataAccess();
                    generateQrCodesAndLog();
            System.out.println("QR-Codes für alle Artikel erstellt.");
                });
    }

    public static void forceImportData() {
        runCliOperation(
                "Starte erzwungenen Datenimport...",
                "Fehler beim erzwungenen Datenimport",
                () -> {
            initializeCliDataAccess();
            deleteImportedItemsList();
            importInitialData(null);
            initializeDefaultUser();
            System.out.println("Datenimport abgeschlossen.");
                });
    }

    private static void deleteImportedItemsList() {
        if (!ImportUtils.deleteImportFile()) {
            logger.warn("Konnte die Liste der importierten Items nicht löschen. Möglicherweise existiert sie nicht oder es gibt Berechtigungsprobleme.");
        } else {
            logger.info("Liste der importierten Items erfolgreich gelöscht.");
        }
        resetFirstStartSettingIfNeeded();
    }

    private static void initializeCliDataAccess() {
        printStartupInfo();
        ensureAppDataDirectory();
        loadSettings();
        initializeDatabase();
    }

    private static void runCliOperation(String startMessage, String errorMessage, Runnable action) {
        System.out.println(startMessage);
        try {
            action.run();
        } catch (Exception e) {
            handleCriticalStartupError(errorMessage, e);
            System.exit(1);
        }
    }

    private static void runServerMode(String[] args) {
        System.out.println("Starte Server-Modus...");
        try {
            ScanServer.main(args);
        } catch (Exception e) {
            logger.error("Fehler im Server-Modus: {}", e.getMessage(), e);
            logUtils.addLog("Fehler im Server-Modus: " + e.getMessage());
        }
    }

    private static void startDesktopApplication() {
        try {
            printStartupInfo();
            SplashscreenGUI splashscreen = createAndShowSplashscreen();
            startInitializationWithSplashscreen(splashscreen);
        } catch (Exception e) {
            handleCriticalStartupError("Fehler beim Starten der Anwendung", e);
            System.exit(1);
        }
    }

    /**
     * Convert stack trace to string for logging
     */
    private static String getStackTraceAsString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable current = e;
        boolean first = true;
        while (current != null) {
            if (!first) {
                sb.append("\nCaused by: ").append(current.getClass().getName());
                if (current.getMessage() != null && !current.getMessage().isBlank()) {
                    sb.append(": ").append(current.getMessage());
                }
            } else if (current.getClass() != null) {
                sb.append("\n").append(current.getClass().getName());
                if (current.getMessage() != null && !current.getMessage().isBlank()) {
                    sb.append(": ").append(current.getMessage());
                }
            }
            for (StackTraceElement element : current.getStackTrace()) {
                sb.append("\n  at ").append(element);
            }
            current = current.getCause();
            first = false;
        }
        return sb.toString();
    }

    /**
     * Print system and application information at startup
     */
    private static void printStartupInfo() {
        printSeparatorLine();
        System.out.println("Starte VEBO Lagersystem...");
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
        updateProgress(progressListener, 3, "Starte Initialisierung...");
        runProgressStep(progressListener, 4, "Prüfe Datenverzeichnis...", Main::ensureAppDataDirectory,
                5, "Datenverzeichnis bereit...");
        runProgressStep(progressListener, 6, "Lade Einstellungen...", Main::loadSettings,
                7, "Einstellungen geladen...");
        runProgressStep(progressListener, 8, "Initialisiere Theme...", Main::initializeTheme,
                9, "Theme gesetzt...");
        updateProgress(progressListener, 10, "Überprüfen ob mit Internet verbunden...");
        runProgressStep(progressListener, 11, "Initialisiere Datenbank...", () -> {
            initializeDatabase();
            applyLookAndFeelFromSettings();
        }, 12, "Datenbank initialisiert...");
        runProgressStep(progressListener, 17, "Prüfe auf Updates (asynchron)...", Main::startUpdateCheckAsync,
                19, "Update-Prüfung gestartet...");
        runProgressStep(progressListener, 20, "Lade Icons...", Main::loadApplicationIcons,
                25, "Icons geladen...");
        handleFirstStartIfNeeded(progressListener);
        updateProgress(progressListener, 96, "Abschluss der Initialisierung...");
    }

    private static void runProgressStep(ProgressListener progressListener,
            int beforePercent,
            String beforeMessage,
            Runnable action,
            int afterPercent,
            String afterMessage) {
        updateProgress(progressListener, beforePercent, beforeMessage);
        action.run();
        updateProgress(progressListener, afterPercent, afterMessage);
    }

    /**
     * Handle first start logic, including asking user about initial data import and
     * performing the import if needed. This method checks the "first-time" setting
     * to determine if it's the first run of the application, and if so, it prompts
     * the user to import initial data. If the user agrees, it proceeds to import
     * articles, vendors, departments, clients, and optionally creates QR codes. It
     * also initializes a default user for the application. Progress updates are
     * sent to the provided listener throughout the process.
     * 
     * @param progressListener Listener to receive progress updates during
     *                         initialization
     */
    private static void handleFirstStartIfNeeded(ProgressListener progressListener) {
        if (isFirstStartAlreadyHandled()) {
            return;
        }

        markFirstStartHandled();
        updateProgress(progressListener, 54, "Erster Start...");

        boolean loadFromFiles = askForInitialDataImport();
        settings.setProperty("load-from-files", String.valueOf(loadFromFiles));
        settings.save();

        if (!loadFromFiles) {
            logger.info("Initial data import skipped as per settings.");
            return;
        }

        updateProgress(progressListener, 72, "Importiere Startdaten...");
        importInitialData(progressListener);
        updateProgress(progressListener, 80, "Startdaten importiert...");
        updateProgress(progressListener, 81, "QR-Code Abfrage...");
        importQrData(progressListener);
        updateProgress(progressListener, 85, "QR-Code Abfrage abgeschlossen.");
        updateProgress(progressListener, 88, "Erstelle Standard-Benutzer...");
        initializeDefaultUser();
        updateProgress(progressListener, 90, "Standard-Benutzer erstellt...");
        logger.info("Initial data import completed.");
    }

    private static boolean isFirstStartAlreadyHandled() {
        String firstTimeSetting = settings.getProperty("first-time");
        return firstTimeSetting != null && firstTimeSetting.equalsIgnoreCase("true");
    }

    private static void markFirstStartHandled() {
        settings.setProperty("first-time", "true");
    }

    private static void resetFirstStartSettingIfNeeded() {
        if (!isFirstStartAlreadyHandled()) {
            return;
        }

        settings.setProperty("first-time", "false");
        settings.save();
        logger.info("Einstellung 'first-time' zurückgesetzt auf false.");
    }

    /**
     * Import QR code data by asking the user if they want to create QR codes for
     * all articles. If the user agrees, it generates QR codes for each article and
     * logs the results. Progress updates are sent to the provided listener
     * throughout the process.
     * 
     * @param progressListener Listener to receive progress updates during QR code
     *                         creation
     */
    private static void importQrData(ProgressListener progressListener) {
        if (showConfirmDialogOnEdt(
                "QR-Codes Erstellen?",
                "QR-Codes",
                null) != JOptionPane.YES_OPTION) {
            return;
        }

        updateProgress(progressListener, 82, "Erstelle QR-Codes...");
        logger.info("QR-Codes werden erstellt...");
        generateQrCodesAndLog();
        logger.info("QR-Codes erstellt.");
    }

    private static void generateQrCodesAndLog() {
        List<File> qrCodeFiles = QRCodeUtils.createQrCodes(ArticleManager.getInstance().getAllArticles());
        for (File qrCodeFile : qrCodeFiles) {
            logger.info("QR-Code erstellt: {}", qrCodeFile.getAbsolutePath());
        }
    }

    /**
     * Show a confirmation dialog on the Event Dispatch Thread (EDT) and return the
     * user's response. This method ensures that the dialog is displayed on the EDT,
     * which is necessary for thread safety in Swing applications. It takes the
     * message, title, and icon for the dialog as parameters and returns the result
     * of the user's choice.
     * 
     * @param progressListener Listener to receive progress updates during the
     *                         confirmation dialog
     * @return true if the user chooses to import initial data, false otherwise
     */
    private static boolean askForInitialDataImport() {
        return showConfirmDialogOnEdt(
                "Willkommen zum VEBO Lagersystem!\nMöchten Sie die anfänglichen Daten jetzt importieren?",
                "Erster Start",
                iconSmall) == JOptionPane.YES_OPTION;
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
            logUtils.addLog("Icons successfully loaded!");
        } catch (MalformedURLException e) {
            String errorMsg = "Fehler beim Laden des Icons: " + e.getMessage();
            logger.error(errorMsg, e);
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
            logger.error(msg);
            logUtils.addLog(msg);
            throw new RuntimeException(msg);
        }
        logger.info("Anwendungsdatenverzeichnis bereit: {}", appDataDir.getAbsolutePath());
    }

    /**
     * Initialize database connection
     */
    private static void initializeDatabase() {
        DatabaseManager.DatabaseType databaseType = resolveDatabaseTypeSetting();
        String databaseFileName = resolveDatabaseFileName(databaseType);
        databaseManager = new DatabaseManager(databaseType, getAppDataDir().getAbsolutePath(), databaseFileName);
        databaseManager.initializeApplicationSchema();
        System.out.println("✓ Datenbank initialisiert (" + databaseType.getDisplayName() + ")");
        logger.info("Database initialized with type {} at {}", databaseType.getConfigValue(),
                databaseManager.getDatabaseUrl());
        cleanupOldLogsIfEnabled();
        logUtils.addLog("Datenbank initialisiert.");
    }

    /**
     * Import all initial data (articles, vendors, departments, clients)
     */
    private static void importInitialData(ProgressListener progressListener) {
        ImportUtils importUtils = ImportUtils.getInstance();
        HashSet<String> importedItems = new HashSet<>(ImportUtils.getImportedItems());
        List<String> newlyImportedItems = new ArrayList<>();

        updateProgress(progressListener, 76, "Importiere Artikel...");
        importArticles(importUtils, importedItems, newlyImportedItems);
        updateProgress(progressListener, 80, "Importiere Lieferanten...");
        importVendors(importUtils, importedItems, newlyImportedItems);
        updateProgress(progressListener, 83, "Importiere Abteilungen...");
        importDepartments(importUtils, importedItems, newlyImportedItems);
        updateProgress(progressListener, 86, "Importiere Kunden...");
        importClients(importUtils, importedItems, newlyImportedItems);
        persistImportedItems(newlyImportedItems);

        System.out.println("✓ Alle Daten importiert");
    }

    /**
     * Import articles from inventory file
     */
    private static void importArticles(ImportUtils importUtils,
            java.util.Set<String> importedItems,
            List<String> newlyImportedItems) {
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Map<String, Object>> data = importUtils.loadInventoryFile();

        System.out.println("\nImportiere " + data.size() + " Artikel...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : data) {
            Article article = createArticleFromMap(itemData);
            processArticleImport(articleManager, article, result, importedItems, newlyImportedItems);
        }

        result.printSummary();
    }

    /**
     * Process single article import
     */
    private static void processArticleImport(ArticleManager articleManager,
            Article article,
            ImportResult result,
            java.util.Set<String> importedItems,
            List<String> newlyImportedItems) {
        if (importedItems.contains(article.getArticleNumber())) {
            result.incrementSkipped();
            return;
        }

        if (articleManager.insertArticle(article)) {
            result.incrementImported();
            importedItems.add(article.getArticleNumber());
            newlyImportedItems.add(article.getArticleNumber());
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
    private static void importVendors(ImportUtils importUtils,
            java.util.Set<String> importedItems,
            List<String> newlyImportedItems) {
        VendorManager vendorManager = VendorManager.getInstance();
        List<Map<String, Object>> vendorData = importUtils.loadVendorList();

        System.out.println("\n🚚 Importiere " + vendorData.size() + " Lieferanten...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : vendorData) {
            processVendorImport(vendorManager, itemData, result, importedItems, newlyImportedItems);
        }

        result.printSummary();
    }

    /**
     * Process single vendor import
     */
    private static void processVendorImport(VendorManager vendorManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems, List<String> newlyImportedItems) {
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
            newlyImportedItems.add(vendorName);
            logUtils.addLog("Importierter Lieferant: " + vendorName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren des Lieferanten: " + vendorName);
        }
    }

    /**
     * Import departments from departments file
     */
    private static void importDepartments(ImportUtils importUtils,
            java.util.Set<String> importedItems,
            List<String> newlyImportedItems) {
        DepartmentManager departmentManager = DepartmentManager.getInstance();
        List<Map<String, Object>> departmentData = importUtils.loadDepartmentsList();

        System.out.println("\nImportiere " + departmentData.size() + " Abteilungen...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : departmentData) {
            processDepartmentImport(departmentManager, itemData, result, importedItems, newlyImportedItems);
        }

        result.printSummary();
    }

    /**
     * Process single department import
     */
    private static void processDepartmentImport(DepartmentManager departmentManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems, List<String> newlyImportedItems) {
        String departmentName = getString(itemData, "department");
        String kontoNumber = getString(itemData, "kontoNumber");

        if (shouldSkipImport(departmentName, departmentManager.existsDepartment(departmentName), importedItems)) {
            result.incrementSkipped();
            return;
        }

        if (departmentManager.insertDepartment(departmentName, kontoNumber)) {
            result.incrementImported();
            importedItems.add(departmentName);
            newlyImportedItems.add(departmentName);
            logUtils.addLog("Importierte Abteilung: " + departmentName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren der Abteilung: " + departmentName);
        }
    }

    /**
     * Import clients from clients file
     */
    private static void importClients(ImportUtils importUtils,
            java.util.Set<String> importedItems,
            List<String> newlyImportedItems) {
        ClientManager clientManager = ClientManager.getInstance();
        List<Map<String, Object>> clientData = importUtils.loadClientsList();

        System.out.println("\n👥 Importiere " + clientData.size() + " Kunden...");
        ImportResult result = new ImportResult();

        for (Map<String, Object> itemData : clientData) {
            processClientImport(clientManager, itemData, result, importedItems, newlyImportedItems);
        }

        result.printSummary();
    }

    /**
     * Process single client import
     */
    private static void processClientImport(ClientManager clientManager, Map<String, Object> itemData,
            ImportResult result, java.util.Set<String> importedItems, List<String> newlyImportedItems) {
        String firstLastName = getString(itemData, "firstLastName");
        String department = getString(itemData, "department");

        if (shouldSkipImport(firstLastName, clientManager.existsClient(firstLastName), importedItems)) {
            result.incrementSkipped();
            return;
        }

        if (clientManager.insertClient(firstLastName, department)) {
            result.incrementImported();
            importedItems.add(firstLastName);
            newlyImportedItems.add(firstLastName);
            logUtils.addLog("Importierter Kunde: " + firstLastName);
        } else {
            result.incrementSkipped();
            logUtils.addLog("Fehler beim Importieren des Kunden: " + firstLastName);
        }
    }

    private static void persistImportedItems(List<String> newlyImportedItems) {
        if (newlyImportedItems == null || newlyImportedItems.isEmpty()) {
            return;
        }

        Path importFile = Path.of(getAppDataDir().getAbsolutePath(), "imported_items.txt");
        try {
            Files.write(
                    importFile,
                    newlyImportedItems,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            logUtils.addLog("Fehler beim Schreiben von imported_items.txt: " + e.getMessage());
            logger.error("Failed to persist imported items", e);
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
        if (settings == null) {
            return defaultValue;
        }
        String value = settings.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    /**
     * Get boolean setting with support for legacy key aliases.
     */
    private static boolean getBooleanSetting(String key, boolean defaultValue, String... aliases) {
        if (settings == null) {
            return defaultValue;
        }
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

    private static void cleanupOldLogsIfEnabled() {
        if (!getBooleanSetting("delete_old_logs_on_startup", false)) {
            return;
        }

        int deletedDatabaseLogs = ch.framedev.lagersystem.managers.LogManager.getInstance()
                .deleteOldLogs(LogUtils.DEFAULT_LOG_RETENTION_DAYS);
        LogUtils.LogCleanupResult fileCleanup = logUtils.deleteLogsOlderThan(LogUtils.DEFAULT_LOG_RETENTION_DAYS);

        String cleanupMessage = String.format(
                "Automatische Log-Bereinigung abgeschlossen: %d Datenbankeintraege und %d Protokolldateien aelter als %d Tage geloescht.",
                deletedDatabaseLogs,
                fileCleanup.deletedFileCount(),
                LogUtils.DEFAULT_LOG_RETENTION_DAYS);

        if (fileCleanup.failedFileCount() > 0) {
            cleanupMessage += " " + fileCleanup.failedFileCount()
                    + " Protokolldatei(en) konnten nicht geloescht werden.";
        }

        logger.info(cleanupMessage);
        logUtils.addLog(cleanupMessage);
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
            if (logUtils != null) {
                logUtils.addLog("Fehler beim Ermitteln des App-Datenverzeichnisses: " + e.getMessage());
            }
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
        initializeDefaultSettings();
        System.out.println("✓ Einstellungen geladen");

        applyThemeSettings();
        applyTableFontSettings();
        loadGitHubToken();
        ensureCategoriesFileExists();
    }

    private static void initializeDefaultSettings() {
        boolean changed = false;
        if (!settings.contains("first-time")) {
            settings.setProperty("first-time", "false");
            changed = true;
        }
        if (!settings.contains("database_type")) {
            settings.setProperty("database_type", DatabaseManager.DatabaseType.SQLITE.getConfigValue());
            changed = true;
        }
        if (changed) {
            settings.save();
        }
    }

    private static DatabaseManager.DatabaseType resolveDatabaseTypeSetting() {
        String configured = settings != null ? settings.getProperty("database_type") : null;
        DatabaseManager.DatabaseType databaseType = DatabaseManager.DatabaseType.fromConfig(configured);

        if (settings != null) {
            String normalized = databaseType.getConfigValue();
            if (!normalized.equalsIgnoreCase(String.valueOf(configured))) {
                settings.setProperty("database_type", normalized);
                settings.save();
            }
        }

        return databaseType;
    }

    private static String resolveDatabaseFileName(DatabaseManager.DatabaseType databaseType) {
        String configured = settings != null ? settings.getProperty("database_file") : null;
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }
        if (databaseType == DatabaseManager.DatabaseType.H2
                || databaseType == DatabaseManager.DatabaseType.JSON
                || databaseType == DatabaseManager.DatabaseType.YAML) {
            return "vebo_lager_system";
        }
        return "vebo_lager_system.db";
    }

    private static void ensureCategoriesFileExists() {
        File targetFile = new File(getAppDataDir(), "categories.json");
        if (targetFile.exists()) {
            return;
        }

        SimpleJavaUtils utils = new SimpleJavaUtils();
        try {
            Files.copy(utils.getFromResourceFile("categories.json", Main.class).toPath(),
                    targetFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Konnte categories.json nicht initialisieren", e);
        }
    }

    /**
     * Returns the settings file path and ensures its parent directory exists.
     * The file itself is intentionally NOT pre-created so that {@link ch.framedev.lagersystem.utils.Settings}
     * can fall back to the classpath template (which carries comments and default values)
     * when the file does not yet exist.  The first call to {@code Settings.save()} will
     * create the file.
     */
    private static File ensureSettingsFile() {
        File settingsFile = new File(getAppDataDir(), "settings.properties");

        if (settingsFile.exists()) {
            logger.info("Settings file already exists: {}", settingsFile.getAbsolutePath());
        } else {
            // Ensure parent directory is present; Settings.save() will create the file.
            File parent = settingsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            logger.info("Settings file will be created on first save: {}", settingsFile.getAbsolutePath());
            logUtils.addLog("Einstellungsdatei wird beim ersten Speichern erstellt: " + settingsFile.getAbsolutePath());
        }

        return settingsFile;
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
        // Keep progress pacing sleep, but never block the EDT so splash animations stay
        // fluid.
        if (!SwingUtilities.isEventDispatchThread()) {
            sleepQuietly();
        }
    }

    private static void sleepQuietly() {
        try {
            Thread.sleep((long) 100);
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
        createInitializationWorker(splashscreen).execute();
    }

    private static SwingWorker<Void, ProgressUpdate> createInitializationWorker(SplashscreenGUI splashscreen) {
        return new SwingWorker<>() {
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
                updateSplashscreenProgress(splashscreen, chunks);
            }

            @Override
            protected void done() {
                if (initException != null) {
                    handleInitializationFailure(splashscreen, initException);
                    return;
                }

                finishInitialization(splashscreen);
            }
        };
    }

    private static void updateSplashscreenProgress(SplashscreenGUI splashscreen, List<ProgressUpdate> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        ProgressUpdate latestUpdate = chunks.getLast();
        splashscreen.updateProgress(latestUpdate.percent(), latestUpdate.message());
    }

    private static void handleInitializationFailure(SplashscreenGUI splashscreen, Exception initException) {
        handleCriticalStartupError("Fehler beim Starten der Anwendung", initException);
        splashscreen.close();
        JOptionPane.showMessageDialog(
                null,
                "Die Anwendung konnte nicht gestartet werden.\nDetails: " + initException.getMessage(),
                "Startfehler",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    private static void finishInitialization(SplashscreenGUI splashscreen) {
        splashscreen.updateProgress(100, "Starte Hauptfenster...");
        splashscreen.close();
        ImportUtils.loadSeparatedArticles();
        launchGUI();
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

    private static void handleCriticalStartupError(String message, Exception e) {
        logger.error("{}: {}", message, e.getMessage(), e);
        logUtils.addLog(message + ": " + e.getMessage());
        logUtils.addLog("Stack trace: " + getStackTraceAsString(e));
    }
}
