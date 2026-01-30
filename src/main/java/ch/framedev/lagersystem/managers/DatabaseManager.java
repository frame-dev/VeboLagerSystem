package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Simple DatabaseManager for SQLite.
 * <p>
 * Note: executeQuery returns a ResultSet and keeps the creating Statement tracked.
 * Callers must call closeQuery(ResultSet) when finished to close both ResultSet and Statement.
 * TODO: Performance optimizations if needed.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class DatabaseManager {

    private final Logger logger = LogManager.getLogger(DatabaseManager.class);

    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_VENDORS = "vendors";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_CLIENTS = "clients";
    public static final String TABLE_DEPARTMENTS = "departments";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_WARNINGS = "warnings";
    public static final String TABLE_LOGS = "logs";

    private final Connection connection;

    /**
     * Tracks open ResultSet -> Statement pairs so both can be closed together later.
     * WeakHashMap avoids preventing GC if callers drop references without closing.
     */
    private final Map<ResultSet, Statement> openStatements =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Open (or create) the SQLite database at path/fileName.
     *
     * @param path     directory path (maybe null or empty)
     * @param fileName database file name
     */
    public DatabaseManager(String path, String fileName) {
        String prefix = (path != null && !path.isEmpty()) ? (path.endsWith("/") ? path : path + "/") : "";
        File dir = new File(prefix);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Main.logUtils.addLog("Fehler beim Erstellen des Datenbank-Verzeichnisses: " + dir.getAbsolutePath());
                throw new RuntimeException("Failed to create database directory: " + dir.getAbsolutePath());
            }
        }
        String url = "jdbc:sqlite:" + prefix + fileName;
        try {
            this.connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
            Main.logUtils.addLog("Fehler beim Öffnen der Datenbank: " + e.getMessage());
            throw new RuntimeException("Failed to open database: " + url, e);
        }
    }

    /**
     * Execute a query and return the ResultSet. Caller must call closeQuery(rs).
     *
     * @param sql SQL select/query
     * @return ResultSet or null on error
     */
    public ResultSet executeQuery(String sql) {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            openStatements.put(rs, stmt);
            return rs;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeQuery: ", e);
            Main.logUtils.addLog("Fehler bei der Abfrage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Close the given ResultSet and its creating Statement (if tracked).
     *
     * @param rs ResultSet to close (may be null)
     */
    public void closeQuery(ResultSet rs) {
        if (rs == null) return;
        Statement stmt = null;
        try {
            stmt = openStatements.remove(rs);
        } catch (Exception ignored) {
        }
        try {
            if (!rs.isClosed()) {
                rs.close();
            }
        } catch (SQLException ignored) {
        }
        if (stmt != null) {
            try {
                if (!stmt.isClosed()) {
                    stmt.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Close all currently tracked open queries (ResultSets and their Statements).
     */
    public void closeAllOpenQueries() {
        List<ResultSet> keys;
        synchronized (openStatements) {
            keys = new ArrayList<>(openStatements.keySet());
        }
        for (ResultSet rs : keys) {
            try {
                closeQuery(rs);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Execute update/DDL statement.
     *
     * @param sql SQL update/DDL
     * @return true if succeeded
     */
    public boolean executeUpdate(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdate: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return false;
        }
    }

    public int executeUpdateWithCount(String sql) {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdateWithCount: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return -1;
        }
    }

    public boolean executePreparedUpdate(String sql, Object[] values) {
        try (var pstmt = connection.prepareStatement(sql)) {
            // No parameters to bind
            if (values == null || values.length == 0) {
                pstmt.executeUpdate();
                return true;
            }

            try {
                int bindCount = pstmt.getParameterMetaData().getParameterCount();
                if (values.length < bindCount) {
                    Main.logUtils.addLog("Fehler bei der vorbereiteten Aktualisierung: Nicht genügend Parameter: erwartet " + bindCount + " aber erhalten " + values.length);
                    throw new IllegalArgumentException("Not enough parameters: expected " + bindCount + " but got " + values.length);
                }
                for (int i = 0; i < bindCount; i++) {
                    pstmt.setObject(i + 1, values[i]);
                }
            } catch (SQLException metaEx) {
                // Parameter metadata unavailable — fallback to binding all provided values.
                for (int i = 0; i < values.length; i++) {
                    pstmt.setObject(i + 1, values[i]);
                }
            }

            pstmt.executeUpdate();
            return true;
        } catch (SQLException | IllegalArgumentException e) {
            logger.error("SQL Exception during executeUpdate: ", e);
            Main.logUtils.addLog("Fehler bei der vorbereiteten Aktualisierung: " + e.getMessage());
            return false;
        }
    }

    /**
     * Expose raw Connection if needed.
     *
     * @return JDBC Connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Close manager: close tracked queries and connection.
     */
    public void close() {
        closeAllOpenQueries();
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    public ResultSet executePreparedQuery(String sql, Object[] objects) {
        try {
            var pstmt = connection.prepareStatement(sql);

            // No parameters to bind
            if (objects != null && objects.length > 0) {
                try {
                    int paramCount = pstmt.getParameterMetaData().getParameterCount();
                    if (objects.length < paramCount) {
                        Main.logUtils.addLog("Fehler bei der vorbereiteten Abfrage: Nicht genügend Parameter: erwartet " + paramCount + " aber erhalten " + objects.length);
                        throw new IllegalArgumentException("Not enough parameters: expected " + paramCount + " but got " + objects.length);
                    }
                    for (int i = 0; i < paramCount; i++) {
                        pstmt.setObject(i + 1, objects[i]);
                    }
                } catch (SQLException metaEx) {
                    // Parameter metadata unavailable — fallback to binding all provided values.
                    for (int i = 0; i < objects.length; i++) {
                        pstmt.setObject(i + 1, objects[i]);
                    }
                }
            }

            ResultSet rs = pstmt.executeQuery();
            openStatements.put(rs, pstmt);
            return rs;
        } catch (SQLException | IllegalArgumentException e) {
            logger.error("SQL Exception during executePreparedQuery: ", e);
            Main.logUtils.addLog("Fehler bei der vorbereiteten Abfrage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Clear all data from the database.
     * WARNING: This is a destructive operation that removes ALL data from ALL tables.
     * This operation uses transactions to ensure atomicity (all or nothing).
     *
     * @return true if all tables were successfully cleared, false otherwise
     */
    public boolean clearDatabase() {
        logger.warn("clearDatabase() called - this will delete all data!");

        // Define all tables to clear (in dependency order to avoid foreign key issues)
        String[] tablesToClear = {
            TABLE_WARNINGS,      // No dependencies
            TABLE_ORDERS,        // References users, articles
            TABLE_USERS,         // References orders (but we clear orders first)
            TABLE_CLIENTS,       // No dependencies
            TABLE_DEPARTMENTS,   // No dependencies
            TABLE_ARTICLES,      // References vendors
            TABLE_VENDORS        // No dependencies
        };

        boolean autoCommit = true;
        try {
            // Start transaction
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            int clearedCount = 0;

            // Clear each table
            for (String table : tablesToClear) {
                String sql = "DELETE FROM " + table;
                try (Statement stmt = connection.createStatement()) {
                    int rowsDeleted = stmt.executeUpdate(sql);
                    logger.info("Cleared {} rows from table: {}", rowsDeleted, table);
                    Main.logUtils.addLog("Tabelle " + table + " gelöscht, " + rowsDeleted + " Zeilen entfernt.");
                    clearedCount++;
                } catch (SQLException e) {
                    logger.error("Failed to clear table: {}", table, e);
                    // Rollback on any error
                    connection.rollback();
                    logger.warn("Transaction rolled back - no data was deleted");
                    Main.logUtils.addLog("Fehler beim Löschen der Tabelle " + table + ": " + e.getMessage());
                    return false;
                }
            }

            // Commit transaction
            connection.commit();
            logger.info("Successfully cleared all {} tables", clearedCount);
            return true;

        } catch (SQLException e) {
            logger.error("Error during clearDatabase transaction", e);
            Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
            try {
                connection.rollback();
                logger.warn("Transaction rolled back due to error");
                Main.logUtils.addLog("Transaktion zurückgesetzt aufgrund eines Fehlers.");
            } catch (SQLException rollbackEx) {
                logger.error("Failed to rollback transaction", rollbackEx);
                Main.logUtils.addLog("Fehler beim Zurücksetzen der Transaktion: " + rollbackEx.getMessage());
            }
            Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
            return false;
        } finally {
            // Restore auto-commit mode
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                logger.error("Failed to restore auto-commit mode", e);
                Main.logUtils.addLog("Failed to restore auto-commit mode");
            }
        }
    }

    /**
     * Clear a specific table from the database.
     *
     * @param tableName The name of the table to clear
     * @return true if the table was successfully cleared, false otherwise
     */
    public boolean clearTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            logger.error("clearTable called with invalid table name");
            Main.logUtils.addLog("Invalid table name: " + tableName);
            return false;
        }

        String sql = "DELETE FROM " + tableName;
        try (Statement stmt = connection.createStatement()) {
            int rowsDeleted = stmt.executeUpdate(sql);
            logger.info("Cleared {} rows from table: {}", rowsDeleted, tableName);
            Main.logUtils.addLog("Tabelle " + tableName + " gelöscht, " + rowsDeleted + " Zeilen entfernt.");
            return true;
        } catch (SQLException e) {
            logger.error("Failed to clear table: {}", tableName, e);
            Main.logUtils.addLog("Fehler beim Löschen der Tabelle " + tableName + ": " + e.getMessage());
            return false;
        }
    }
}