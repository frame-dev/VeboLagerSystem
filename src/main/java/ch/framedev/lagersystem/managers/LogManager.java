package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static ch.framedev.lagersystem.managers.DatabaseManager.TABLE_LOGS;

/**
 * LogManager for managing application logs in the database.
 * Provides functionality to create, retrieve, filter, and delete logs.
 */
public class LogManager {

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(LogManager.class);
    private static LogManager instance;
    private final DatabaseManager databaseManager;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /**
     * Log levels enum for better type safety
     */
    public enum LogLevel {
        INFO, WARNING, ERROR, DEBUG, TRACE
    }

    private LogManager() {
        this.databaseManager = Main.databaseManager;
        createTable();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            instance = new LogManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "timestamp TEXT NOT NULL," +
                "level TEXT NOT NULL," +
                "message TEXT NOT NULL" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Log record
     */
    public record Log(int id, String timestamp, String level, String message) {
        public Log(String timestamp, String level, String message) {
            this(0, timestamp, level, message);
        }
    }

    /**
     * Creates a log entry in the database
     */
    public boolean createLog(Log log) {
        String sql = "INSERT INTO " + TABLE_LOGS + " (timestamp, level, message) VALUES (?, ?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[] {log.timestamp, log.level, log.message});
    }

    /**
     * Creates a log entry with current timestamp
     */
    public boolean createLog(LogLevel level, String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        Log log = new Log(timestamp, level.name(), message);
        return createLog(log);
    }

    /**
     * Retrieves all logs from the database
     */
    public List<Log> getAllLogs() {
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS + " ORDER BY id DESC;";
        List<Log> logs = new ArrayList<>();

        try (ResultSet rs = databaseManager.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("level"),
                    rs.getString("message")
                ));
            }
        } catch (Exception e) {
            logger.error("Error retrieving logs: {}", e.getMessage(), e);
        }

        return logs;
    }

    /**
     * Retrieves logs filtered by level
     */
    public List<Log> getLogsByLevel(LogLevel level) {
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS +
                     " WHERE level = ? ORDER BY id DESC;";
        List<Log> logs = new ArrayList<>();

        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{level.name()})) {
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("level"),
                    rs.getString("message")
                ));
            }
        } catch (Exception e) {
            logger.error("Error retrieving logs by level: {}", e.getMessage(), e);
        }

        return logs;
    }

    /**
     * Retrieves logs within a date range
     */
    public List<Log> getLogsByDateRange(String startDate, String endDate) {
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS +
                     " WHERE timestamp BETWEEN ? AND ? ORDER BY id DESC;";
        List<Log> logs = new ArrayList<>();

        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{startDate, endDate})) {
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("level"),
                    rs.getString("message")
                ));
            }
        } catch (Exception e) {
            logger.error("Error retrieving logs by date range: {}", e.getMessage(), e);
        }

        return logs;
    }

    /**
     * Searches logs by message content
     */
    public List<Log> searchLogs(String searchTerm) {
        String sql = "SELECT id, timestamp, level, message FROM " + TABLE_LOGS +
                     " WHERE message LIKE ? ORDER BY id DESC;";
        List<Log> logs = new ArrayList<>();

        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{"%" + searchTerm + "%"})) {
            while (rs.next()) {
                logs.add(new Log(
                    rs.getInt("id"),
                    rs.getString("timestamp"),
                    rs.getString("level"),
                    rs.getString("message")
                ));
            }
        } catch (Exception e) {
            logger.error("Error searching logs: {}", e.getMessage(), e);
        }

        return logs;
    }

    /**
     * Deletes a log entry by ID
     */
    public boolean deleteLog(int id) {
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE id = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{id});
    }

    /**
     * Deletes logs older than specified days
     */
    public int deleteOldLogs(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        String cutoffTimestamp = cutoffDate.format(FORMATTER);
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE timestamp < '" + cutoffTimestamp + "';";
        try {
            return databaseManager.executeUpdateWithCount(sql);
        } catch (Exception e) {
            logger.error("Error deleting old logs: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Deletes logs older than 30 days
     */
    private void deleteOldLogs() {
        // Deletes logs older than 30 days based on the timestamp column (assumed ISO 8601 or yyyy-MM-dd HH:mm:ss format)
        String sql = "DELETE FROM " + TABLE_LOGS + " WHERE date(timestamp) < date('now', '-30 days')";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Clears all logs from the database
     */
    public boolean clearAllLogs() {
        String sql = "DELETE FROM " + TABLE_LOGS + ";";
        return databaseManager.executeUpdate(sql);
    }

    /**
     * Gets the count of logs by level
     */
    public int getLogCountByLevel(LogLevel level) {
        String sql = "SELECT COUNT(*) as count FROM " + TABLE_LOGS + " WHERE level = ?;";
        try (ResultSet rs = databaseManager.executePreparedQuery(sql, new Object[]{level.name()})) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error getting log count by level: {}", e.getMessage(), e);
        }
        return 0;
    }

    /**
     * Gets the total count of all logs
     */
    public int getTotalLogCount() {
        String sql = "SELECT COUNT(*) as count FROM " + TABLE_LOGS + ";";
        try (ResultSet rs = databaseManager.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (Exception e) {
            logger.error("Error getting total log count: {}", e.getMessage(), e);
        }
        return 0;
    }
}
