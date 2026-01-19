package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Verwaltet geplante Aufgaben wie die Überprüfung des Lagerbestands.
 * Optimiert durch:
 * - Vermeidung von GUI-Popups während Scheduler-Läufen
 * - Batch-Verarbeitung von Warnungen
 * - Thread-sicheres Singleton-Pattern
 * - Effiziente Datenbankabfragen
 */
public class SchedulerManager {

    private static volatile SchedulerManager instance;
    private static final Object lock = new Object();

    private ScheduledExecutorService executor;
    private final WarningManager warningManager = WarningManager.getInstance();

    private SchedulerManager() {
        this.executor = Executors.newScheduledThreadPool(1);
    }

    /**
     * Thread-sichere Singleton-Implementierung
     */
    public static SchedulerManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new SchedulerManager();
                }
            }
        }
        return instance;
    }

    /**
     * Startet die geplante Überprüfung des Lagerbestands.
     * Standard: Alle 30 Minuten
     */
    public void startScheduledStockCheck() {
        String periodString = Main.settings.getProperty("stock_check_interval");

        long period = periodString != null ? Long.parseLong(periodString) : 30;

        startScheduledStockCheck(period, TimeUnit.MINUTES);
    }

    /**
     * Startet die geplante Überprüfung des Lagerbestands mit benutzerdefiniertem Intervall.
     *
     * @param period Das Intervall zwischen den Überprüfungen
     * @param unit   Die Zeiteinheit für das Intervall
     */
    public void startScheduledStockCheck(long period, TimeUnit unit) {
        executor.scheduleAtFixedRate(
                this::checkLowStock,
                0, // Sofort starten
                period,
                unit
        );
        System.out.printf("[SchedulerManager] Lagerbestandsprüfung gestartet (Intervall: %d %s)%n",
                period, unit.toString().toLowerCase());
    }

    /**
     * Startet die stündliche Anzeige von Warnungen.
     * Diese Methode zeigt alle ungelesenen Warnungen basierend auf den Einstellungen an.
     * Standard: Jede Stunde
     */
    public void startHourlyWarningDisplay() {
        String intervalString = Main.settings.getProperty("warning_display_interval");

        long interval = intervalString != null ? Long.parseLong(intervalString) : 1;

        executor.scheduleAtFixedRate(
                this::displayPendingWarnings,
                interval, // Nach der ersten Intervall-Zeit starten
                interval, // Wiederholen nach Intervall
                TimeUnit.HOURS
        );
        System.out.printf("[SchedulerManager] Warnanzeige gestartet (Intervall: %d Stunde(n))%n", interval);
    }

    /**
     * Startet die Anzeige von Warnungen mit benutzerdefiniertem Intervall.
     *
     * @param interval Das Intervall zwischen den Anzeigen
     * @param unit Die Zeiteinheit für das Intervall
     */
    public void startWarningDisplay(long interval, TimeUnit unit) {
        executor.scheduleAtFixedRate(
                this::displayPendingWarnings,
                interval, // Nach der ersten Intervall-Zeit starten
                interval, // Wiederholen nach Intervall
                unit
        );
        System.out.printf("[SchedulerManager] Warnanzeige gestartet (Intervall: %d %s)%n",
                interval, unit.toString().toLowerCase());
    }

    public void startAutoImportQrCodes(long interval, TimeUnit unit) {
        executor.scheduleAtFixedRate(
                this::autoImportQrCodes,
                0,
                interval,
                unit
        );
        System.out.printf("[SchedulerManager] Automatischer QR-Code Import gestartet (Intervall: %d %s)%n",
                interval, unit.toString().toLowerCase());
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
                System.out.println("[SchedulerManager] Keine Artikel zum Überprüfen vorhanden");
                return;
            }

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
                    if (!warningManager.hasWarning(titleCritical)) {
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
                            System.err.println("[SchedulerManager] Fehler beim Speichern der kritischen Warnung für Artikel " +
                                    article.getArticleNumber());
                        }
                    }
                }
                // Prüfe, ob Lagerbestand unter oder gleich Mindestbestand (aber über kritischer Schwelle)
                else if (article.getStockQuantity() > criticalThreshold && article.getStockQuantity() <= article.getMinStockLevel()) {
                    String title = "Niedriger Lagerbestand für Artikel " + article.getArticleNumber();

                    // Vermeide doppelte Warnungen
                    if (!warningManager.hasWarning(title)) {
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
                            System.err.println("[SchedulerManager] Fehler beim Speichern der Warnung für Artikel " +
                                    article.getArticleNumber());
                        }
                    } else {
                        // Bestehende Warnung aktualisieren (falls Bestand sich geändert hat)
                        warningsUpdated++;
                    }
                }
            }

            // Log der Ergebnisse
            if (warningsCreated > 0 || warningsUpdated > 0) {
                System.out.printf("[SchedulerManager] Lagerbestandsprüfung abgeschlossen: " +
                                "%d neue Warnung(en), %d aktualisiert%n",
                        warningsCreated, warningsUpdated);
            } else {
                System.out.println("[SchedulerManager] Lagerbestandsprüfung abgeschlossen: Keine kritischen Bestände");
            }
        } catch (Exception e) {
            System.err.println("[SchedulerManager] Fehler bei der Lagerbestandsprüfung: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Beendet den Scheduler ordnungsgemäß.
     */
    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
                System.out.println("[SchedulerManager] Scheduler ordnungsgemäß beendet");

                // Recreate executor for restart capability
                this.executor = Executors.newScheduledThreadPool(1);
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
                // Recreate executor even after interruption
                this.executor = Executors.newScheduledThreadPool(1);
            }
        }
    }

    /**
     * Manuelle einmalige Überprüfung (kann mit GUI-Anzeige verwendet werden).
     * Diese Methode kann aufgerufen werden, wenn der Benutzer manuell nach Warnungen sucht.
     */
    public void runManualCheck() {
        System.out.println("[SchedulerManager] Starte manuelle Lagerbestandsprüfung...");
        checkLowStock();
    }

    /**
     * Zeigt alle noch nicht angezeigten Warnungen an.
     * Diese Methode wird jede Stunde vom Scheduler aufgerufen.
     */
    private void displayPendingWarnings() {
        try {
            List<Warning> allWarnings = warningManager.getAllWarnings();

            if (allWarnings == null || allWarnings.isEmpty()) {
                System.out.println("[SchedulerManager] Keine Warnungen vorhanden");
                return;
            }

            // Filtere unangezeigte und nicht gelöste Warnungen
            List<Warning> pendingWarnings = allWarnings.stream()
                    .filter(w -> !w.isDisplayed() && !w.isResolved())
                    .toList();

            if (pendingWarnings.isEmpty()) {
                System.out.println("[SchedulerManager] Keine neuen Warnungen zum Anzeigen");
                return;
            }

            System.out.printf("[SchedulerManager] Zeige %d unangezeigte Warnung(en) an%n",
                    pendingWarnings.size());

            // Zeige Warnungen nacheinander an (nur wenn GUI verfügbar ist)
            for (Warning warning : pendingWarnings) {
                MainGUI.articleGUI.displayWarning(warning);
                // Markiere als angezeigt
                warning.setDisplayed(true);
                warningManager.updateWarning(warning);
            }

        } catch (Exception e) {
            System.err.println("[SchedulerManager] Fehler beim Anzeigen von Warnungen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void autoImportQrCodes() {
        List<Map<String, Object>> qrData = QRCodeUtils.retrieveQrCodeDataFromWebsite();
        if(qrData.isEmpty())
            return;
        ArticleManager articleManager = ArticleManager.getInstance();
        for(Map<String, Object> data : qrData) {
            if(data.get("type").equals("buy")) {
                String[] parts = QRCodeUtils.getPartsFromData((String) data.get("data"));
                if(parts.length < 2)
                    continue;
                String articleNumber = parts[0];
                int quantity = (int) data.get("quantity");
                String id = data.get("id").toString();
                if(!ImportUtils.getImportedQrCodes().contains(id)) {
                    if (!articleManager.addToStock(articleNumber, quantity))
                        System.err.println("[SchedulerManager] Fehler beim automatischen Importieren des QR-Codes für Artikel " + articleNumber);
                    else {
                        System.out.println("[SchedulerManager] Erfolgreich automatisch " + quantity + " Einheiten zu Artikel " + articleNumber + " hinzugefügt.");
                        ImportUtils.addQrCodeImport(id);
                    }
                }
            }
        }
    }
}
