package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unused", "deprecation"})
public class WarningManager {

    private final Logger LOGGER = LogManager.getLogger(WarningManager.class);

    // Singleton Instance
    private static WarningManager instance;
    // Database Manager
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    // Per-warning cache (keyed by title)
    private final ConcurrentHashMap<String, Warning> cache = new ConcurrentHashMap<>();
    // Cached list of all warnings with a simple TTL
    private volatile List<Warning> allWarningsCache = null;
    private volatile long allWarningsCacheTime = 0L;

    /**
     * Private constructor to enforce singleton pattern
     */
    private WarningManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    /**
     * Get the singleton instance of WarningManager
     *
     * @return WarningManager instance
     */
    public static WarningManager getInstance() {
        if (instance == null) {
            instance = new WarningManager();
        }
        return instance;
    }

    /**
     * Create the warnings table if it does not exist
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_WARNINGS + " (" +
                "title TEXT," +
                "message TEXT," +
                "type TEXT," +
                "date TEXT," +
                "isResolved TEXT," +
                "isDisplayed TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Insert a new warning into the database
     *
     * @param warning Warning object to insert
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertWarning(Warning warning) {
        String sql = "INSERT INTO " + DatabaseManager.TABLE_WARNINGS + " (title, message, type, date, isResolved, isDisplayed) " +
                "VALUES (?, ?, ?, ?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                warning.getTitle(),
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? "true" : "false",
                warning.isDisplayed() ? "true" : "false"
        });
        if (result) {
            Main.logUtils.addLog("Inserted new warning with title '" + warning.getTitle() + "'");
            // Update cache
            cache.put(warning.getTitle(), warning);
            // Invalidate cached list
            allWarningsCache = null;
            allWarningsCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not insert new warning with title '" + warning.getTitle() + "'");
            LOGGER.error("Could not insert new warning with title '{}'", warning.getTitle());
        }
        return result;
    }

    public boolean hasNotWarning(String title) {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            return !resultSet.next();
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isResolved(String title) {
        // Prefer cache if available
        Warning cached = cache.get(title);
        if (cached != null) {
            return cached.isResolved();
        }

        String sql = "SELECT isResolved FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                return resultSet.getString("isResolved").equals("true");
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking if warning with title '{}' is resolved", title, e);
            return false;
        }
        return false;
    }

    public boolean resolveWarning(String title) {
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS + " SET isResolved = 'true' WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title});
        if (result) {
            Main.logUtils.addLog("Resolved warning with title '" + title + "'");
            // Update cache if present
            Warning w = cache.get(title);
            if (w != null) {
                w.setResolved(true);
                cache.put(title, w);
            }
            // Invalidate cached list
            allWarningsCache = null;
            allWarningsCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not resolve warning with title '" + title + "'");
            LOGGER.error("Could not resolve warning with title '{}'", title);
        }
        return result;
    }

    /**
     * Update a warning in the database
     *
     * @param warning Warning an object to update
     * @return true if the update was successful, false otherwise
     */
    public boolean updateWarning(Warning warning) {
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS + " SET message = ?, type = ?, date = ?, isResolved = ?, isDisplayed = ? " +
                "WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? "true" : "false",
                warning.isDisplayed() ? "true" : "false",
                warning.getTitle()
        });
        if (result) {
            Main.logUtils.addLog("Updated warning with title '" + warning.getTitle() + "'");
            // Update cache
            cache.put(warning.getTitle(), warning);
            // Invalidate cached list
            allWarningsCache = null;
            allWarningsCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not update warning with title '" + warning.getTitle() + "'");
            LOGGER.error("Could not update warning with title '{}'", warning.getTitle());
        }
        return result;
    }

    public boolean deleteWarning(String title) {
        String sql = "DELETE FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title});
        if (result) {
            Main.logUtils.addLog("Deleted warning with title '" + title + "'");
            // Remove from cache
            cache.remove(title);
            // Invalidate cached list
            allWarningsCache = null;
            allWarningsCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not delete warning with title '" + title + "'");
            LOGGER.error("Could not delete warning with title '{}'", title);
        }
        return result;
    }

    public Warning getWarning(String title) {
        if (title == null) return null;
        // Try cache first
        Warning cached = cache.get(title);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                Warning w = new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        resultSet.getString("isResolved").equals("true"),
                        resultSet.getString("isDisplayed").equals("true")
                );
                cache.put(title, w);
                return w;
            }
        } catch (Exception e) {
            LOGGER.error("Error while retrieving warning with title '{}'", title, e);
            return null;
        }
        return null;
    }

    public boolean isDisplayed(String title) {
        // Prefer cache if available
        Warning cached = cache.get(title);
        if (cached != null) {
            return cached.isDisplayed();
        }

        String sql = "SELECT isDisplayed FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                return resultSet.getString("isDisplayed").equals("true");
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking if warning with title '{}' is displayed", title, e);
            return false;
        }
        return false;
    }

    public List<Warning> getAllWarnings() {
        // Use cached list when within TTL
        long now = System.currentTimeMillis();
        // 5 minutes
        long CACHE_TTL_MILLIS = 5 * 60 * 1000;
        if (allWarningsCache != null && (now - allWarningsCacheTime) < CACHE_TTL_MILLIS) {
            return allWarningsCache;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + ";";
        List<Warning> warnings = new java.util.ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                Warning w = new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        resultSet.getString("isResolved").equals("true"),
                        resultSet.getString("isDisplayed").equals("true")
                );
                warnings.add(w);
                // refresh per-warning cache
                cache.put(w.getTitle(), w);
            }
            // cache the list (immutable view)
            allWarningsCache = Collections.unmodifiableList(warnings);
            allWarningsCacheTime = System.currentTimeMillis();
        } catch (Exception e) {
            System.err.println("[WarningManager] Fehler beim Laden aller Warnungen: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Laden aller Warnungen");
            LOGGER.error("Error while retrieving all warnings", e);
        }
        return warnings;
    }

    /**
     * Clear both per-warning and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        allWarningsCache = null;
        allWarningsCacheTime = 0L;
    }
}
