package ch.framedev.lagersystem.managers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Warning;
import static ch.framedev.lagersystem.dialogs.DisplayWarningDialog.displayWarning;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;

/**
 * Manages scheduled background tasks (stock checks, warning display, and optional QR-code auto import).
 *
 * <p>Design goals:
 * <ul>
 *   <li>Avoid GUI popups during scheduler runs (EDT-safe UI updates)</li>
 *   <li>Keep scheduling thread-safe (double-checked locking singleton)</li>
 *   <li>Fail safely on bad settings values</li>
 *   <li>Minimize expensive per-article DB checks where possible</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class SchedulerManager {

    private final Logger logger = LogManager.getLogger(SchedulerManager.class);

    private static volatile SchedulerManager instance;
    private static final Object lock = new Object();

    private ScheduledExecutorService executor;
    private final WarningManager warningManager = WarningManager.getInstance();

    /**
     * Ensures that scheduling calls are serialized.
     *
     * <p>Note: the singleton creation is already thread-safe, but the executor lifecycle can be
     * interacted with from different code paths (start/shutdown/restart).
     */
    private final Object scheduleLock = new Object();

    /** Default stock check interval in minutes when no setting is present or parsing fails. */
    private static final long DEFAULT_STOCK_CHECK_MINUTES = 30;

    /** Default warning display interval in hours when no setting is present or parsing fails. */
    private static final long DEFAULT_WARNING_DISPLAY_HOURS = 1;

    /**
     * Parses a long setting value safely.
     *
     * @param raw          raw string value from settings
     * @param defaultValue value to use when {@code raw} is null/blank/invalid
     * @return parsed long or {@code defaultValue}
     */
    private static long parseLongOrDefault(String raw, long defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Ensures the executor exists and is usable (recreates it after shutdown).
     */
    private void ensureExecutor() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newScheduledThreadPool(1);
        }
    }

    private SchedulerManager() {
        this.executor = Executors.newScheduledThreadPool(1);
    }

    /**
     * Returns the singleton instance using double-checked locking.
     *
     * @return scheduler manager instance
     */
    public static SchedulerManager getInstance() {
        SchedulerManager local = SchedulerManager.instance;
        if (local == null) {
            synchronized (lock) {
                local = SchedulerManager.instance;
                if (local == null) {
                    local = new SchedulerManager();
                    SchedulerManager.instance = local;
                }
            }
        }
        return local;
    }

    /**
     * Starts the scheduled stock check.
     *
     * <p>Default: every 30 minutes.
     */
    public void startScheduledStockCheck() {
        long period = parseLongOrDefault(Main.settings.getProperty("stock_check_interval"), DEFAULT_STOCK_CHECK_MINUTES);
        startScheduledStockCheck(period, TimeUnit.MINUTES);
    }

    /**
     * Starts the scheduled stock check with a custom interval.
     *
     * @param period interval amount (must be > 0)
     * @param unit   time unit for the interval
     */
    public void startScheduledStockCheck(long period, TimeUnit unit) {
        if (period <= 0 || unit == null) {
            logger.warn("Ignoring stock check scheduling due to invalid arguments: period={}, unit={}", period, unit);
            return;
        }
        synchronized (scheduleLock) {
            ensureExecutor();
            executor.scheduleAtFixedRate(
                    this::checkLowStock,
                    0, // start immediately
                    period,
                    unit
            );
        }
        logger.info("Stock check started (interval: {} {})", period, unit.toString().toLowerCase());
    }

    /**
     * Starts periodic warning display.
     *
     * <p>Default: every 1 hour.
     */
    public void startHourlyWarningDisplay() {
        long interval = parseLongOrDefault(Main.settings.getProperty("warning_display_interval"), DEFAULT_WARNING_DISPLAY_HOURS);
        executorScheduleWarningDisplay(interval, TimeUnit.HOURS);
    }

    /**
     * Starts warning display with a custom interval.
     *
     * @param interval interval amount (must be > 0)
     * @param unit     time unit for the interval
     */
    public void startWarningDisplay(long interval, TimeUnit unit) {
        executorScheduleWarningDisplay(interval, unit);
    }

    /**
     * Internal helper to schedule warning display safely.
     */
    private void executorScheduleWarningDisplay(long interval, TimeUnit unit) {
        if (interval <= 0 || unit == null) {
            logger.warn("Ignoring warning display scheduling due to invalid arguments: interval={}, unit={}", interval, unit);
            return;
        }
        synchronized (scheduleLock) {
            ensureExecutor();
            executor.scheduleAtFixedRate(
                    this::displayPendingWarnings,
                    interval, // first run after one interval
                    interval,
                    unit
            );
        }
        logger.info("Warning display started (interval: {} {})", interval, unit.toString().toLowerCase());
    }

    /**
     * Starts periodic QR-code auto import.
     *
     * @param interval interval amount (must be > 0)
     * @param unit     time unit for the interval
     */
    public void startAutoImportQrCodes(long interval, TimeUnit unit) {
        if (interval <= 0 || unit == null) {
            logger.warn("Ignoring QR auto import scheduling due to invalid arguments: interval={}, unit={}", interval, unit);
            return;
        }
        synchronized (scheduleLock) {
            ensureExecutor();
            executor.scheduleAtFixedRate(
                    this::autoImportQrCodes,
                    0,
                    interval,
                    unit
            );
        }
        logger.info("QR-code auto import started (interval: {} {})", interval, unit.toString().toLowerCase());
    }

    /**
     * Überprüft alle Artikel auf niedrigen Lagerbestand und erstellt Warnungen.
     * Diese Methode wird vom Scheduler aufgerufen und zeigt KEINE automatischen Popups an.
     */
    public void checkLowStock() {
        try {
            // Artikel neu laden für aktuelle Daten
            ArticleManager articleManager = ArticleManager.getInstance();
            List<Article> articles = articleManager.getAllArticles();

            if (articles == null || articles.isEmpty()) {
                logger.info("Keine Artikel zum Überprüfen vorhanden");
                return;
            }

            // Use a per-run formatter (thread-confined) and a single timestamp for all warnings in this run.
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String currentDateTime = dateFormat.format(new Date());
            int warningsCreated = 0;
            int warningsUpdated = 0;

            for (Article article : articles) {
                // Überspringe Artikel ohne Mindestbestand-Definition
                if (article.getMinStockLevel() <= 0) {
                    continue;
                }

                // Berechne kritische Schwelle (50% des Mindestbestands)
                int criticalThreshold = (int) Math.ceil(article.getMinStockLevel() * 0.5);

                // Prüfe auf kritischen Lagerbestand (50% oder weniger des Mindestbestands)
                if (article.getStockQuantity() > 0 && article.getStockQuantity() <= criticalThreshold) {
                    String titleCritical = "Kritischer Lagerbestand für Artikel " + article.getArticleNumber();

                    // Vermeide doppelte Warnungen
                    if (warningManager.hasNotWarning(titleCritical)) {
                        Warning warning = new Warning(
                                titleCritical,
                                String.format("KRITISCH: Der Lagerbestand für Artikel '%s' (Nr: %s) ist sehr niedrig! " +
                                                "Aktueller Bestand: %d (nur noch %d%% des Mindestbestands von %d)",
                                        article.getName(),
                                        article.getArticleNumber(),
                                        article.getStockQuantity(),
                                        (article.getStockQuantity() * 100) / article.getMinStockLevel(),
                                        article.getMinStockLevel()),
                                Warning.WarningType.CRITICAL_STOCK,
                                currentDateTime,
                                false,
                                false
                        );

                        if (warningManager.insertWarning(warning)) {
                            warningsCreated++;
                        } else {
                            logger.error("Fehler beim Speichern der kritischen Warnung für Artikel {}", article.getArticleNumber());
                            Main.logUtils.addLog("Fehler beim Speichern der kritischen Warnung für Artikel " + article.getArticleNumber());
                        }
                    }
                }
                // Prüfe, ob Lagerbestand unter oder gleich Mindestbestand (aber über kritischer Schwelle)
                else if (article.getStockQuantity() > criticalThreshold && article.getStockQuantity() <= article.getMinStockLevel()) {
                    String title = "Niedriger Lagerbestand für Artikel " + article.getArticleNumber();

                    // Vermeide doppelte Warnungen
                    if (warningManager.hasNotWarning(title)) {
                        Warning warning = new Warning(
                                title,
                                String.format("Der Lagerbestand für Artikel '%s' (Nr: %s) ist niedrig. " +
                                                "Aktueller Bestand: %d | Mindestbestand: %d",
                                        article.getName(),
                                        article.getArticleNumber(),
                                        article.getStockQuantity(),
                                        article.getMinStockLevel()),
                                Warning.WarningType.LOW_STOCK,
                                currentDateTime,
                                false, // Nicht gelöst
                                false  // NICHT automatisch anzeigen (kein Popup während Scheduler-Lauf)
                        );

                        // Warnung nur in Datenbank speichern, NICHT anzeigen
                        if (warningManager.insertWarning(warning)) {
                            warningsCreated++;
                        } else {
                            logger.error("Fehler beim Speichern der Warnung für Artikel {}", article.getArticleNumber());
                            Main.logUtils.addLog("Fehler beim Speichern der Warnung für Artikel " + article.getArticleNumber());
                        }
                    } else {
                        // Bestehende Warnung aktualisieren (falls Bestand sich geändert hat)
                        warningsUpdated++;
                    }
                }
            }

            // Log der Ergebnisse
            if (warningsCreated > 0 || warningsUpdated > 0) {
                logger.info("Lagerbestandsprüfung abgeschlossen: {} neue Warnung(en), {} aktualisiert",
                        warningsCreated, warningsUpdated);
            } else {
                logger.info("Lagerbestandsprüfung abgeschlossen: Keine neuen Warnungen");
            }
        } catch (Exception e) {
            logger.error("Fehler bei der Lagerbestandsprüfung: {}", e.getMessage(), e);
            Main.logUtils.addLog("Fehler bei der Lagerbestandsüberprüfung: " + e.getMessage());
        }
    }

    /**
     * Shuts down the scheduler gracefully.
     *
     * <p>If the executor is stopped, a fresh executor is created so scheduling can be started again
     * without constructing a new instance.
     */
    public void shutdown() {
        synchronized (scheduleLock) {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                try {
                    if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                        executor.shutdownNow();
                    }
                    logger.info("Scheduler shut down cleanly");
                } catch (InterruptedException e) {
                    executor.shutdownNow();
                    Thread.currentThread().interrupt();
                } finally {
                    // Recreate executor for restart capability
                    ensureExecutor();
                }
            } else {
                // Ensure we always end in a usable state
                ensureExecutor();
            }
        }
    }

    /**
     * Displays all warnings that are not yet displayed and not resolved.
     *
     * <p>This is called by the scheduler. UI operations are dispatched onto the Swing EDT.
     */
    private void displayPendingWarnings() {
        try {
            List<Warning> allWarnings = warningManager.getAllWarnings();

            if (allWarnings == null || allWarnings.isEmpty()) {
                logger.info("No pending warnings to display");
                return;
            }

            // Filtere unangezeigte und nicht gelöste Warnungen
            List<Warning> pendingWarnings = allWarnings.stream()
                    .filter(w -> !w.isDisplayed() && !w.isResolved())
                    .toList();

            if (pendingWarnings.isEmpty()) {
                logger.info("No pending warnings to display");
                return;
            }

            logger.info("Zeige {} unangezeigte Warnung(en) an", pendingWarnings.size());

            // Zeige Warnungen nacheinander an (nur wenn GUI verfügbar ist)
            for (Warning warning : pendingWarnings) {
                // Display dialog on the EDT to avoid Swing threading issues.
                SwingUtilities.invokeLater(() -> {
                    if (MainGUI.articleGUI != null) {
                        displayWarning(MainGUI.articleGUI, warning);
                    } else {
                        logger.warn("Cannot display warning dialog because MainGUI.articleGUI is null: {}", warning.getTitle());
                    }
                });

                // Mark as displayed and persist state (even if UI is currently unavailable).
                warning.setDisplayed(true);
                if (warningManager.updateWarning(warning)) {
                    logger.info("Warning displayed: {}", warning.getTitle());
                    Main.logUtils.addLog("Warning displayed: " + warning.getTitle());
                } else {
                    logger.error("Failed to update warning as displayed: {}", warning.getTitle());
                    Main.logUtils.addLog("Failed to update warning as displayed: " + warning.getTitle());
                }
            }

        } catch (Exception e) {
            logger.info("Fehler beim Anzeigen von Warnungen: {}", e.getMessage(), e);
            Main.logUtils.addLog("Fehler beim Anzeigen von Warnungen: " + e.getMessage());
        }
    }

    /**
     * Pulls QR-code data from the website and applies stock changes for non-imported "buy" entries.
     *
     * <p>Runs on the scheduler thread; keep it robust against malformed payloads.
     */
    private void autoImportQrCodes() {
        List<Map<String, Object>> qrData = QRCodeUtils.retrieveQrCodeDataFromWebsite();
        if (qrData.isEmpty()) {
            return;
        }

        ArticleManager articleManager = ArticleManager.getInstance();

        for (Map<String, Object> data : qrData) {
            if (data == null) {
                continue;
            }

            // Only process "buy" events (stock increase)
            Object type = data.get("type");
            if (!"buy".equals(type)) {
                continue;
            }

            Object rawPayload = data.get("data");
            if (!(rawPayload instanceof String payload)) {
                continue;
            }

            String[] parts = QRCodeUtils.getPartsFromData(payload);
            if (parts.length < 2) {
                continue;
            }

            String articleNumber = parts[0];

            int quantity;
            Object rawQty = data.get("quantity");
            if (rawQty instanceof Number n) {
                quantity = n.intValue();
            } else {
                continue;
            }

            Object rawId = data.get("id");
            if (rawId == null) {
                continue;
            }
            String id = rawId.toString();

            if (!ImportUtils.getImportedQrCodes().contains(id)) {
                if (!articleManager.addToStock(articleNumber, quantity)) {
                    logger.error("Failed to auto-import QR-code for article {}", articleNumber);
                    Main.logUtils.addLog("Failed to auto-import QR-code for article " + articleNumber);
                } else {
                    logger.info("Auto-imported {} units to article {}", quantity, articleNumber);
                    ImportUtils.addQrCodeImport(id);
                }
            }
        }
    }
}
