package ch.framedev.lagersystem.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.main.Main;

public class WarningManager {

    private static final Logger LOGGER = LogManager.getLogger(WarningManager.class);

    // Singleton Instance
    private static volatile WarningManager instance = null;
    // Database Manager
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    // Per-warning cache (keyed by title)
    private final ConcurrentHashMap<String, Warning> cache = new ConcurrentHashMap<>();
    // Cached list of all warnings with a simple TTL
    private volatile List<Warning> allWarningsCache = null;
    private volatile long allWarningsCacheTime = 0L;

    private static final long ALL_WARNINGS_CACHE_TTL_MILLIS = 5 * 60 * 1000L; // 5 minutes
    private static final String DB_TRUE = "true";
    private static final String DB_FALSE = "false";

    private void invalidateAllWarningsCache() {
        allWarningsCache = null;
        allWarningsCacheTime = 0L;
    }

    /**
     * Private constructor to enforce singleton pattern
     */
    private WarningManager() {
        databaseManager = Objects.requireNonNull(Main.databaseManager, "Main.databaseManager must not be null");
        createTable();
    }

    /**
     * Get the singleton instance of WarningManager
     *
     * @return WarningManager instance
     */
    public static WarningManager getInstance() {
        WarningManager local = instance;
        if (local == null) {
            synchronized (WarningManager.class) {
                if (instance == null) {
                    instance = new WarningManager();
                }
                local = instance;
            }
        }
        return local;
    }

    /**
     * Create the warnings table if it does not exist
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_WARNINGS + " (" +
                "title TEXT UNIQUE," +
                "message TEXT," +
                "type TEXT," +
                "date TEXT," +
                "isResolved TEXT," +
                "isDisplayed TEXT" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
    }

    /**
     * Insert a new warning into the database
     *
     * @param warning Warning object to insert
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertWarning(Warning warning) {
        if (warning == null || warning.getTitle() == null || warning.getTitle().isEmpty())
            return false;
        String sql = "INSERT INTO " + DatabaseManager.TABLE_WARNINGS
                + " (title, message, type, date, isResolved, isDisplayed) " +
                "VALUES (?, ?, ?, ?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[] {
                warning.getTitle(),
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? DB_TRUE : DB_FALSE,
                warning.isDisplayed() ? DB_TRUE : DB_FALSE
        });
        if (result) {
            Main.logUtils.addLog("Inserted new warning with title '" + warning.getTitle() + "'");
            // Update cache
            cache.put(warning.getTitle(), warning);
            // Invalidate cached list
            invalidateAllWarningsCache();
        } else {
            Main.logUtils.addLog("Could not insert new warning with title '" + warning.getTitle() + "'");
            LOGGER.error("Could not insert new warning with title '{}'", warning.getTitle());
        }
        return result;
    }

    public boolean hasNotWarning(String title) {
        if (title == null)
            return true;
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[] { title })) {
            return !resultSet.next();
        } catch (Exception e) {
            LOGGER.error("Error while checking if warning with title '{}' exists", title, e);
            // fail-open: allow creation if DB check fails
            return true;
        }
    }

    public boolean isResolved(String title) {
        if (title == null)
            return false;
        // Prefer cache if available
        Warning cached = cache.get(title);
        if (cached != null) {
            return cached.isResolved();
        }

        String sql = "SELECT isResolved FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[] { title })) {
            if (resultSet.next()) {
                return parseDbBoolean(resultSet.getString("isResolved"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking if warning with title '{}' is resolved", title, e);
            return false;
        }
        return false;
    }

    public boolean resolveWarning(String title) {
        if (title == null)
            return false;
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS + " SET isResolved = 'true' WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[] { title });
        if (result) {
            Main.logUtils.addLog("Resolved warning with title '" + title + "'");
            // Update cache if present
            Warning w = cache.get(title);
            if (w != null) {
                w.setResolved(true);
                cache.put(title, w);
            }
            // Invalidate cached list
            invalidateAllWarningsCache();
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
        if (warning == null || warning.getTitle() == null || warning.getTitle().isEmpty())
            return false;
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS
                + " SET message = ?, type = ?, date = ?, isResolved = ?, isDisplayed = ? " +
                "WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[] {
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? DB_TRUE : DB_FALSE,
                warning.isDisplayed() ? DB_TRUE : DB_FALSE,
                warning.getTitle()
        });
        if (result) {
            Main.logUtils.addLog("Updated warning with title '" + warning.getTitle() + "'");
            // Update cache
            cache.put(warning.getTitle(), warning);
            // Invalidate cached list
            invalidateAllWarningsCache();
        } else {
            Main.logUtils.addLog("Could not update warning with title '" + warning.getTitle() + "'");
            LOGGER.error("Could not update warning with title '{}'", warning.getTitle());
        }
        return result;
    }

    public boolean deleteWarning(String title) {
        if (title == null)
            return false;
        String sql = "DELETE FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[] { title });
        if (result) {
            Main.logUtils.addLog("Deleted warning with title '" + title + "'");
            // Remove from cache
            cache.remove(title);
            // Invalidate cached list
            invalidateAllWarningsCache();
        } else {
            Main.logUtils.addLog("Could not delete warning with title '" + title + "'");
            LOGGER.error("Could not delete warning with title '{}'", title);
        }
        return result;
    }

    /**
     * Retrieve a warning by its title
     * 
     * @param title The title of the warning
     * @return The Warning object if found, null otherwise
     */
    public Warning getWarning(String title) {
        if (title == null)
            return null;
        // Try cache first
        Warning cached = cache.get(title);
        if (cached != null)
            return cached;

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[] { title })) {
            if (resultSet.next()) {
                Warning w = new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        parseDbBoolean(resultSet.getString("isResolved")),
                        parseDbBoolean(resultSet.getString("isDisplayed")));
                cache.put(title, w);
                return w;
            }
        } catch (Exception e) {
            LOGGER.error("Error while retrieving warning with title '{}'", title, e);
            return null;
        }
        return null;
    }

    /**
     * Check if a warning with the given title is currently displayed
     * 
     * @param title The title of the warning to check
     * @return true if the warning is displayed, false otherwise
     */
    public boolean isDisplayed(String title) {
        if (title == null)
            return false;
        // Prefer cache if available
        Warning cached = cache.get(title);
        if (cached != null) {
            return cached.isDisplayed();
        }

        String sql = "SELECT isDisplayed FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[] { title })) {
            if (resultSet.next()) {
                return parseDbBoolean(resultSet.getString("isDisplayed"));
            }
        } catch (Exception e) {
            LOGGER.error("Error while checking if warning with title '{}' is displayed", title, e);
            return false;
        }
        return false;
    }

    /**
     * Retrieve a list of all warnings from the database. This method uses a simple
     * caching mechanism to improve performance.
     * 
     * @return List of all Warning objects
     */
    public List<Warning> getAllWarnings() {
        // Use cached list when within TTL
        long now = System.currentTimeMillis();
        if (allWarningsCache != null && (now - allWarningsCacheTime) < ALL_WARNINGS_CACHE_TTL_MILLIS) {
            return allWarningsCache;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + ";";
        List<Warning> warnings = new ArrayList<>();
        try (var resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                Warning w = new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        parseDbBoolean(resultSet.getString("isResolved")),
                        parseDbBoolean(resultSet.getString("isDisplayed")));
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
        if (allWarningsCache != null) {
            return allWarningsCache;
        } else if (!warnings.isEmpty()) {
            return Collections.unmodifiableList(warnings);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Clear both per-warning and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        invalidateAllWarningsCache();
    }

    /**
     * Clears cached warning data and eagerly reloads the current warnings from the
     * database.
     */
    public void forceReloadWarnings() {
        clearCache();
        getAllWarnings();
    }

    private static boolean parseDbBoolean(String value) {
        return Boolean.parseBoolean(value);
    }
}
