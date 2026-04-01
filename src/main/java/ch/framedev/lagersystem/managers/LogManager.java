package ch.framedev.lagersystem.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.main.Main;
import static ch.framedev.lagersystem.managers.DatabaseManager.TABLE_LOGS;

/**
 * LogManager for managing application logs in the database. Provides
 * functionality to create, retrieve, filter, and delete logs.
 */
@SuppressWarnings({"unused"})
public class LogManager {

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);

    private static volatile LogManager instance = null;
    private final DatabaseManager databaseManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // ==================== Cache ====================
    // Per-log cache keyed by id
    private final ConcurrentHashMap<Integer, Log> cache = new ConcurrentHashMap<>();
    // Cached list of all logs with TTL
    private volatile List<Log> allLogsCache = null;
    private volatile long allLogsCacheTime = 0L;
    // Cached counts per level and total
    private final ConcurrentHashMap<LogLevel, Integer> countsCache = new ConcurrentHashMap<>();
    private volatile Integer totalCountCache = null;
    private volatile long countsCacheTime = 0L;
    private static final long CACHE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes for logs/counts

    /**
     * Log levels enum for better type safety
     */
    public enum LogLevel {
        /**
         * INFO: General informational messages about application operations.
         * WARNING: Indications of potential issues or important events that are
         * not errors. ERROR: Serious issues that have occurred, such as
         * exceptions or failed operations. DEBUG: Detailed information useful
         * for debugging purposes, typically not shown in production. TRACE:
         * Very detailed information about the application's execution, often
         * including method entry/exit and variable values, used for in-depth
         * debugging.
         */
        INFO, WARNING, ERROR, DEBUG, TRACE
    }

    private LogManager() {
        this.databaseManager = Main.databaseManager;
        if (this.databaseManager == null) {
            throw new IllegalStateException("DatabaseManager is not initialized");
        }
        createTable();
    }

    public static LogManager getInstance() {
        LogManager local = instance;
        if (local == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
                local = instance;
            }
        }
        return local;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " ("
                + databaseManager.identityColumn("id") + ","
                + "timestamp TEXT NOT NULL,"
                + "epochMillis BIGINT NOT NULL,"
                + "level TEXT NOT NULL,"
                + "message TEXT NOT NULL"
                + ");";
        databaseManager.executeTrustedUpdate(sql);
        ensureEpochMillisColumnType();
    }

    private void ensureEpochMillisColumnType() {
        if (databaseManager.isH2()) {
            databaseManager.executeTrustedUpdate(
                    "ALTER TABLE " + TABLE_LOGS + " ALTER COLUMN epochMillis BIGINT NOT NULL;");
        }
    }

    /**
     * Log record representing a log entry in the database.
     *
     * @param id The unique identifier for the log entry (auto-incremented by
     * the database).
     * @param timestamp The timestamp of when the log entry was created, stored
     * as a string in the format "dd.MM.yyyy HH:mm:ss".
     * @param level The log level (e.g., INFO, WARNING, ERROR, DEBUG, TRACE)
     * indicating the severity or type of the log entry.
     * @param message The log message content describing the event or
     * information being logged.
     */
    public record Log(int id, String timestamp, String level, String message) {

        public Log(String timestamp, String level, String message) {
            this(0, timestamp, level, message);
        }
    }

    private Log readLog(ResultSet rs) throws SQLException {
        return new Log(
                rs.getInt("id"),
                rs.getString("timestamp"),
                rs.getString("level"),
                rs.getString("message")
        );
    }

    private List<Log> collectLogs(ResultSet rs) throws SQLException {
        List<Log> logs = new ArrayList<>();
        while (rs.next()) {
            Log log = readLog(rs);
            logs.add(log);
            cache.put(log.id, log);
        }
        return logs;
    }

    /**
     * Creates a log entry in the database. After successful insertion, it
     * invalidates relevant caches to ensure that subsequent reads reflect the
     * new log entry.
     *
     * @param log The Log record containing the timestamp, level, and message to
     * be inserted into the database. The id field is not used for insertion as
     * it is auto-incremented by the database.
     * @return true if the log entry was successfully created in the database,
     * false otherwise.
     */
    public boolean createLog(Log log) {
        String sql = "INSERT INTO " + TABLE_LOGS + " (timestamp, epochMillis, level, message) VALUES (?, ?, ?, ?);";
        boolean ok = databaseManager.executePreparedUpdate(sql, new Object[]{log.timestamp, System.currentTimeMillis(), log.level, log.message});
        // Main.logUtils.addLog(log.message);
        if (ok) {
            invalidateCaches();
        }
        return ok;
    }

    /**
     * Convenience method to create a log entry with the current timestamp. This
     * method formats the current date and time according to the specified
     * formatter and then calls the createLog(Log log) method to insert the log
     * entry into the database.
     *
     * @param level The log level (e.g., INFO, WARNING, ERROR, DEBUG, TRACE)
     * indicating the severity or type of the log entry.
     * @param message The log message content describing the event or
     * information being logged.
     * @return true if the log entry was successfully created in the database,
     * false otherwise.
     */
    public boolean createLog(LogLevel level, String message) {
        String safeMessage = message == null ? "" : message;
        String timestamp = LocalDateTime.now().format(FORMATTER);
        Log log = new Log(timestamp, level.name(), safeMessage);
        return createLog(log);
    }

    /**
     * Retrieves all logs from the database, ordered by id in descending order
     * (most recent first). This method uses a caching mechanism to improve
     * performance for repeated calls. If the cache is valid (not expired), it
     * returns the cached list of logs. Otherwise, it queries the database,
     * updates the cache, and returns the fresh list of logs.
     *
     * @return A list of Log records representing all log entries in the
     * database, ordered by most recent first. If an error occurs during
     * retrieval, an empty list is returned and the error is logged.
     */
    public List<Log> getAllLogs() {
        long now = System.currentTimeMillis();
        if (allLogsCache != null && (now - allLogsCacheTime) < CACHE_TTL_MILLIS) {
            return allLogsCache;
        }

        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS + " ORDER BY id DESC;";
        List<Log> logs = new ArrayList<>();

        try (ResultSet rs = databaseManager.executeTrustedQuery(sql)) {
            logs = collectLogs(rs);
            allLogsCache = Collections.unmodifiableList(logs);
            allLogsCacheTime = System.currentTimeMillis();
        } catch (SQLException e) {
            logger.error("Error retrieving logs: {}", e.getMessage(), e);
        }
        if (allLogsCache != null) {
            return allLogsCache;
        } else if (!logs.isEmpty()) {
            return Collections.unmodifiableList(logs);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Retrieves logs filtered by the specified log level. This method queries
     * the database directly for logs matching the given level and does not use
     * caching for the list of logs, as counts are cached separately. The
     * retrieved logs are ordered by id in descending order (most recent first).
     * Each retrieved log is also added to the per-log cache for potential
     * future retrieval by id.
     *
     * @param level The log level (e.g., INFO, WARNING, ERROR, DEBUG, TRACE)
     * used to filter the logs. Only logs with this level will be retrieved from
     * the database.
     * @return A list of Log records representing the log entries that match the
     * specified log level, ordered by most recent first. If an error occurs
     * during retrieval, an empty list is returned and the error is logged.
     */
    public List<Log> getLogsByLevel(LogLevel level) {
        // For this case, query DB directly (counts are cached elsewhere)
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS
                + " WHERE level = ? ORDER BY id DESC;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{level.name()})) {
            return collectLogs(rs);
        } catch (Exception e) {
            logger.error("Error retrieving logs by level: {}", e.getMessage(), e);
        }

        return List.of();
    }

    /**
     * Retrieves logs created within the specified date range. The method
     * expects the startDate and endDate parameters to be in the same format as
     * the timestamp stored in the database (e.g., "dd.MM.yyyy HH:mm:ss"). It
     * queries the database for logs where the timestamp is between the provided
     * start and end dates, ordered by id in descending order (most recent
     * first). Each retrieved log is also added to the per-log cache for
     * potential future retrieval by id.
     *
     * @param startDate The start date of the range, formatted as a string in
     * the same format as the timestamp stored in the database (e.g.,
     * "dd.MM.yyyy HH:mm:ss"). Logs with a timestamp equal to or greater than
     * this date will be included in the results.
     * @param endDate The end date of the range, formatted as a string in the
     * same format as the timestamp stored in the database (e.g., "dd.MM.yyyy
     * HH:mm:ss"). Logs with a timestamp equal to or less than this date will be
     * included in the results.
     * @return A list of Log records representing the log entries that were
     * created within the specified date range, ordered by most recent first. If
     * an error occurs during retrieval, an empty list is returned and the error
     * is logged.
     */
    public List<Log> getLogsByDateRange(String startDate, String endDate) {
        // Prefer epochMillis comparisons when the inputs are parseable.
        try {
            long startMillis = LocalDateTime.parse(startDate, FORMATTER).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = LocalDateTime.parse(endDate, FORMATTER).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS
                    + " WHERE epochMillis BETWEEN ? AND ? ORDER BY id DESC;";
            try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{startMillis, endMillis})) {
                return collectLogs(rs);
            }
        } catch (SQLException ignored) {
            // Fall back to the legacy timestamp string comparison.
        }

        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS
                + " WHERE timestamp BETWEEN ? AND ? ORDER BY id DESC;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{startDate, endDate})) {
            return collectLogs(rs);
        } catch (Exception e) {
            logger.error("Error retrieving logs by date range: {}", e.getMessage(), e);
        }

        return List.of();
    }

    /**
     * Retrieves logs that contain the specified search term in their message.
     * The method performs a case-insensitive search for the search term within
     * the message field of the logs. It uses a SQL LIKE query to find matching
     * logs, ordered by id in descending order (most recent first). Each
     * retrieved log
     *
     * @return A list of Log records representing the log entries that contain
     * the specified search term in their message, ordered by most recent first.
     * If an error occurs during retrieval, an empty list is returned and the
     * error is logged.
     */
    public List<Log> searchLogs(String searchTerm) {
        if (searchTerm == null) {
            return List.of();
        }
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS
                + " WHERE message LIKE ? ORDER BY id DESC;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{"%" + searchTerm + "%"})) {
            return collectLogs(rs);
        } catch (Exception e) {
            logger.error("Error searching logs: {}", e.getMessage(), e);
        }

        return List.of();
    }

    /**
     * Retrieves a log entry by its unique identifier (id). The method first
     * checks the in-memory cache for the log entry with the specified id. If it
     * is found in the cache, it is returned immediately. If not found in the
     * cache, the method queries the database for the log entry with the given
     * id. If the log entry is found in the database, it is added to the cache
     * before being returned. If an error occurs during retrieval or if no log
     * entry with the specified id exists, null is returned and the error is
     * logged.
     *
     * @param id The unique identifier of the log entry to be retrieved. This id
     * corresponds to the primary key in the database and is auto-incremented
     * when new log entries are created.
     * @return A Log record representing the log entry with the specified id, or
     * null if no such log entry exists or if an error occurs during retrieval.
     * If the log entry is successfully retrieved from the database, it is also
     * stored in the in-memory cache for potential future retrieval by id.
     */
    public Log getLogById(int id) {
        // try cache first
        Log cached = cache.get(id);
        if (cached != null) {
            return cached;
        }

        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS + " WHERE id = ?;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{id})) {
            if (rs.next()) {
                Log l = readLog(rs);
                cache.put(id, l);
                return l;
            }
        } catch (Exception e) {
            logger.error("Error getting log by id {}: {}", id, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Deletes a log entry from the database by its unique identifier (id). The
     * method executes a DELETE SQL statement to remove the log entry with the
     * specified id from the database. If the deletion is successful, it also
     * removes the corresponding entry from the in-memory cache and invalidates
     * related caches to ensure that subsequent reads reflect the deletion. If
     * an error occurs during deletion, false is returned and the error is
     * logged.
     *
     * @param id The unique identifier of the log entry to be deleted. This id
     * corresponds to the primary key in the database and is auto-incremented
     * when new log entries are created.
     * @return true if the log entry was successfully deleted from the database,
     * false otherwise. If the deletion is successful, the corresponding entry
     * is also removed from the in-memory cache and related caches are
     * invalidated. If an error occurs during deletion, false is returned and
     * the error is logged.
     */
    public boolean deleteLog(int id) {
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE id = ?;";
        boolean ok = databaseManager.executePreparedUpdate(sql, new Object[]{id});
        if (ok) {
            cache.remove(id);
            allLogsCache = null;
            allLogsCacheTime = 0L;
            countsCache.clear();
            totalCountCache = null;
            countsCacheTime = 0L;
        }
        return ok;
    }

    /**
     * Deletes log entries that are older than the specified number of days. The
     * method calculates the cutoff date based on the current date and the
     * provided number of days, and then executes a DELETE SQL statement to
     * remove all log entries with a timestamp older than the cutoff date. If
     * any log entries are deleted, it also clears the in-memory cache and
     * invalidates related caches to ensure that subsequent reads reflect the
     * deletions. The method returns the number of log entries that were
     * deleted. If an error occurs during deletion, 0 is returned and the error
     * is logged.
     *
     * @param daysOld The number of days used to determine which log entries are
     * considered old. Log entries with a timestamp older than the current date
     * minus this number of days will be deleted from the database.
     * @return The number of log entries that were deleted from the database. If
     * any log entries are deleted, the in-memory cache and related caches are
     * also cleared to reflect the deletions. If an error occurs during
     * deletion, 0 is returned and the error is logged.
     */
    public int deleteOldLogs(int daysOld) {
        long cutoffMillis = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L);
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE epochMillis < ?;";
        try {
            int deletedCount = databaseManager.executeUpdateWithCount(sql, cutoffMillis);
            if (deletedCount >= 0) {
                invalidateCaches();
            }
            return Math.max(deletedCount, 0);
        } catch (Exception e) {
            logger.error("Error deleting old logs: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Deletes logs older than 30 days
     */
    private void deleteOldLogs() {
        // Deletes logs older than 30 days based on epochMillis
        long cutoffMillis = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE epochMillis < " + cutoffMillis;
        databaseManager.executeTrustedUpdate(sql);
        invalidateCaches();
    }

    /**
     * Clears all logs from the database
     *
     * @return true if the logs were successfully cleared, false otherwise. If
     * the logs are cleared, the in-memory cache and related caches are also
     * cleared to reflect the deletions. If an error occurs during clearing,
     * false is returned and the error is logged.
     */
    public boolean clearAllLogs() {
        String sql = "DELETE FROM " + TABLE_LOGS + ";";
        boolean ok = databaseManager.executeTrustedUpdate(sql);
        if (ok) {
            invalidateCaches();
        }
        return ok;
    }

    /**
     * Gets the count of logs for a specific log level. This method uses a
     * caching mechanism to improve performance for repeated calls with the same
     * log level. If the count for the specified log level is available in the
     * cache and is not expired, it returns the cached count. Otherwise, it
     * queries the database for the count of logs with the given log level,
     * updates the cache, and returns the fresh count. If an error occurs during
     * retrieval, 0 is returned and the error is logged.
     *
     * @param level The log level (e.g., INFO, WARNING, ERROR, DEBUG, TRACE) for
     * which the count of logs should be retrieved. The method will return the
     * number of log entries in the database that have this log level.
     * @return The count of log entries in the database that have the specified
     * log level. If the count is available in the cache and is not expired, the
     * cached count is returned. Otherwise, the method queries the database for
     * the count, updates the cache, and returns the fresh count. If an error
     * occurs during retrieval, 0 is returned and the error is logged.
     */
    public int getLogCountByLevel(LogLevel level) {
        long now = System.currentTimeMillis();
        if (countsCache.containsKey(level) && (now - countsCacheTime) < CACHE_TTL_MILLIS) {
            return countsCache.get(level);
        }

        String sql = "SELECT COUNT(*) as count FROM " + TABLE_LOGS + " WHERE level = ?;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{level.name()})) {
            if (rs.next()) {
                int count = rs.getInt("count");
                countsCache.put(level, count);
                countsCacheTime = System.currentTimeMillis();
                return count;
            }
        } catch (Exception e) {
            logger.error("Error getting log count by level: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Gets the total count of all logs (cached)
     *
     * @return The total count of all log entries in the database. If the total
     * count is available in the cache and is not expired, the cached count is
     * returned. Otherwise, the method queries the database for the total count,
     * updates the cache, and returns the fresh count. If an error occurs during
     * retrieval, 0 is returned and the error is logged.
     */
    public int getTotalLogCount() {
        long now = System.currentTimeMillis();
        if (totalCountCache != null && (now - countsCacheTime) < CACHE_TTL_MILLIS) {
            return totalCountCache;
        }
        String sql = "SELECT COUNT(*) as count FROM " + TABLE_LOGS + ";";
        try (ResultSet rs = databaseManager.executeTrustedQuery(sql)) {
            if (rs.next()) {
                int count = rs.getInt("count");
                totalCountCache = count;
                countsCacheTime = System.currentTimeMillis();
                return count;
            }
        } catch (Exception e) {
            logger.error("Error getting total log count: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Clear in-memory caches immediately.
     */
    public void clearCache() {
        invalidateCaches();
    }

    private void invalidateCaches() {
        cache.clear();
        allLogsCache = null;
        allLogsCacheTime = 0L;
        countsCache.clear();
        totalCountCache = null;
        countsCacheTime = 0L;
    }
}
