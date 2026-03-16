package ch.framedev.lagersystem.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.main.Main;

/**
 * Simple DatabaseManager for SQLite.
 * <p>
 * Performance notes:
 * - Avoids ParameterMetaData() (slow/unreliable in SQLite)
 * - Adds pragmatic SQLite Pragmas (WAL, NORMAL sync, cache_size, mmap_size, busy_timeout, etc.)
 * - Uses a small LRU cache for PreparedStatements (huge win for repeated queries/updates)
 * - clearDatabase() uses a transaction and a single Statement for speed
 * - clearTable() uses a strict whitelist (safe + fast)
 */
@SuppressWarnings({"UnusedReturnValue", "DeprecatedIsStillUsed", "SqlWithoutWhere"})

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);

    // Singleton instance
    private static volatile DatabaseManager instance;

    /**
     * Get the singleton instance of DatabaseManager.
     * @param path Directory path (may be null or empty)
     * @param fileName Database file name
     * @return DatabaseManager instance
     */
    public static DatabaseManager getInstance(String path, String fileName) {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(path, fileName);
                }
            }
        }
        return instance;
    }

    /**
     * For testing or reinitialization: resets the singleton instance (use with caution).
     */
    public static void resetInstance() {
        synchronized (DatabaseManager.class) {
            if (instance != null) {
                instance.close();
                instance = null;
            }
        }
    }



    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_SEPERATE_ARTICLES = "seperate_articles";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_VENDORS = "vendors";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_ORDERS = "orders";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_CLIENTS = "clients";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_DEPARTMENTS = "departments";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_USERS = "users";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_WARNINGS = "warnings";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_LOGS = "logs";
    /**
     * IMPORTANT: These table names are used in clearTable() and must be kept in sync with your actual schema.
     */
    public static final String TABLE_NOTES = "notes";

    /**
     * Whitelist of allowed table names for clearTable() to prevent SQL injection. Must be kept in sync with actual schema and clearTable() usage.
     */
    public static final Set<String> ALLOWED_TABLES = Set.of(
            TABLE_ARTICLES,
            TABLE_SEPERATE_ARTICLES,
            TABLE_VENDORS,
            TABLE_ORDERS,
            TABLE_CLIENTS,
            TABLE_DEPARTMENTS,
            TABLE_USERS,
            TABLE_WARNINGS,
            TABLE_LOGS,
            TABLE_NOTES
    );

    private final Connection connection;

    /**
     * Tracks open ResultSet -> Statement pairs so both can be closed together later.
     * WeakHashMap avoids preventing GC if callers drop references without closing.
     */
    private final Map<ResultSet, Statement> openStatements =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * PreparedStatement LRU cache.
     * Key includes SQL + "Q"/"U" so query/update statements don't collide.
     */
    private static final int STMT_CACHE_SIZE = 64;

    private final Map<String, PreparedStatement> stmtCache =
            Collections.synchronizedMap(new LinkedHashMap<>(STMT_CACHE_SIZE + 1, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, PreparedStatement> eldest) {
                    if (size() <= STMT_CACHE_SIZE) return false;
                    PreparedStatement ps = eldest.getValue();
                    try {
                        if (ps != null && !ps.isClosed()) ps.close();
                    } catch (SQLException ignored) {
                    }
                    return true;
                }
            });

    /**
     * Open (or create) the SQLite database at path/fileName.
     *
     * @param path     directory path (maybe null or empty)
     * @param fileName database file name
     */
    public DatabaseManager(String path, String fileName) {
        String normalizedPath = (path == null) ? "" : path.trim();
        File dir = normalizedPath.isEmpty() ? null : new File(normalizedPath);
        if (dir != null && !dir.exists()) {
            if (!dir.mkdirs()) {
                Main.logUtils.addLog("Fehler beim Erstellen des Datenbank-Verzeichnisses: " + dir.getAbsolutePath());
                throw new RuntimeException("Failed to create database directory: " + dir.getAbsolutePath());
            }
        }

        File dbFile = (dir == null) ? new File(fileName) : new File(dir, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        try {
            this.connection = DriverManager.getConnection(url);
            applySQLitePragmas(); // performance + safety defaults
        } catch (SQLException e) {
            Main.logUtils.addLog("Fehler beim Öffnen der Datenbank: " + e.getMessage());
            throw new RuntimeException("Failed to open database: " + url, e);
        }
    }

    /**
     * Recommended SQLite pragmas for typical desktop apps.
     * WAL improves concurrency and write throughput. NORMAL is a good perf/safety tradeoff.
     */
    private void applySQLitePragmas() {
        execPragmaSafe("PRAGMA foreign_keys = ON;");
        execPragmaSafe("PRAGMA journal_mode = WAL;");
        execPragmaSafe("PRAGMA synchronous = NORMAL;");
        execPragmaSafe("PRAGMA temp_store = MEMORY;");
        // cache_size: negative means KB, so -20000 ~= 20MB cache
        execPragmaSafe("PRAGMA cache_size = -20000;");

        // Helps prevent "database is locked" under concurrent UI actions
        execPragmaSafe("PRAGMA busy_timeout = 5000;"); // ms

        // Memory map I/O can help on larger DBs / SSDs
        // 256MB mapping (adjust if needed)
        execPragmaSafe("PRAGMA mmap_size = 268435456;");

        // Control WAL checkpointing frequency (pages); adjust to your workload
        execPragmaSafe("PRAGMA wal_autocheckpoint = 1000;");

        // Light maintenance hint (SQLite 3.18+)
        execPragmaSafe("PRAGMA optimize;");
    }

    private void execPragmaSafe(String sql) {
        try (Statement s = connection.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            logger.debug("Pragma failed (ignored): {}", sql, e);
        }
    }

    /**
     * Executes a raw SQL query string.
     *
     * <p><b>SECURITY WARNING:</b> Passing user-controlled input into this method can lead to SQL injection.
     * Prefer {@link #executeQuery(String, Object...)} (prepared statements) or the safe mapping helpers
     * {@link #queryList(String, Object[], ResultMapper)} / {@link #queryOne(String, Object[], ResultMapper)}.
     *
     * @param sql raw SQL query string (e.g. "SELECT * FROM users WHERE id = 1")
     * @return ResultSet or null on error; caller must call {@link #closeQuery(ResultSet)} when done.
     */
    @Deprecated
    public ResultSet executeQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Abfrage: SQL ist leer");
            return null;
        }

        // Very small guardrail: reject obvious multi-statement / comment injection patterns.
        // This is NOT a complete SQL injection defense; prepared statements are the correct fix.
        if (looksLikeMultiStatementOrComment(sql)) {
            logger.warn("Rejected potentially unsafe raw SQL in executeQuery(): {}", sql);
            Main.logUtils.addLog("Unsichere SQL-Abfrage abgelehnt (verwende PreparedStatement)");
            return null;
        }

        try {
            Statement stmt = connection.createStatement();
            @SuppressWarnings("SqlSourceToSinkFlow") ResultSet rs = stmt.executeQuery(sql);
            openStatements.put(rs, stmt);
            return rs;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeQuery: ", e);
            Main.logUtils.addLog("Fehler bei der Abfrage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Executes a parameterized query using a cached PreparedStatement.
     *
     * <p>Use SQL placeholders ("?") and pass parameters via varargs:
     * <pre>
     *     executeQuery("SELECT * FROM users WHERE username = ?", username);
     * </pre>
     * <p>
     * Caller must call {@link #closeQuery(ResultSet)}.
     *
     * @param sql    SQL string with "?" placeholders (e.g. "SELECT * FROM users WHERE username = ?")
     * @param params parameters to bind to the placeholders (e.g. username)
     * @return ResultSet or null on error; caller must call {@link #closeQuery(ResultSet)} when done.
     */
    public ResultSet executeQuery(String sql, Object... params) {
        return executePreparedQuery(sql, params);
    }

    private boolean looksLikeMultiStatementOrComment(String sql) {
        // Disallow semicolons (multiple statements) and SQL comments.
        // SQLite (JDBC) typically executes only one statement in executeQuery(), but this helps prevent
        // accidental concatenation and common injection payloads.
        // allow trailing whitespace
        int semi = sql.indexOf(';');
        if (semi >= 0) {
            // If semicolon appears anywhere before the end (ignoring whitespace), reject
            for (int i = semi + 1; i < sql.length(); i++) {
                if (!Character.isWhitespace(sql.charAt(i))) return true;
            }
        }
        // Basic comment tokens
        return sql.contains("--") || sql.contains("/*") || sql.contains("*/");
    }

    /**
     * Execute a prepared query and return the ResultSet.
     * Caller must call closeQuery(rs).
     * <p>
     * PreparedStatement is reused from an LRU cache.
     *
     * @param sql    SQL string with "?" placeholders (e.g. "SELECT * FROM users WHERE username = ?")
     * @param params parameters to bind to the placeholders (e.g. username)
     * @return ResultSet or null on error; caller must call {@link #closeQuery(ResultSet)} when done.
     */
    public ResultSet executePreparedQuery(String sql, Object[] params) {
        try {
            PreparedStatement pstmt = getCachedPreparedStatement("Q", sql);

            bindParams(pstmt, params);

            ResultSet rs = pstmt.executeQuery();
            // track rs->pstmt so closeQuery closes both if needed
            openStatements.put(rs, pstmt);
            return rs;
        } catch (SQLException e) {
            logger.error("SQL Exception during executePreparedQuery: ", e);
            Main.logUtils.addLog("Fehler bei der vorbereiteten Abfrage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Execute prepared update statement.
     * <p>
     * PreparedStatement is reused from an LRU cache.
     *
     * @param sql    SQL string with "?" placeholders (e.g. "UPDATE users SET last_login = ? WHERE id = ?")
     * @param params parameters to bind to the placeholders (e.g. lastLogin, userId)
     * @return true if update succeeded, false on error
     */
    public boolean executePreparedUpdate(String sql, Object[] params) {
        try {
            PreparedStatement pstmt = getCachedPreparedStatement("U", sql);

            bindParams(pstmt, params);

            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("SQL Exception during executePreparedUpdate: ", e);
            Main.logUtils.addLog("Fehler bei der vorbereiteten Aktualisierung: " + e.getMessage());
            return false;
        }
    }

    private void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        ps.clearParameters();
        if (params == null) return;
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }

    private PreparedStatement getCachedPreparedStatement(String type, String sql) throws SQLException {
        String key = type + ":" + sql;
        synchronized (stmtCache) {
            PreparedStatement ps = stmtCache.get(key);
            if (ps != null && !ps.isClosed()) {
                return ps;
            }

            // Create new and cache it
            ps = connection.prepareStatement(sql);
            stmtCache.put(key, ps);
            return ps;
        }
    }

    /**
     * Checks whether a PreparedStatement instance is currently managed by the LRU cache.
     * This lets {@link #closeQuery(ResultSet)} safely close non-cached PreparedStatements
     * while keeping cached ones alive for reuse.
     */
    private boolean isCachedPreparedStatement(PreparedStatement ps) {
        if (ps == null) return false;
        synchronized (stmtCache) {
            for (PreparedStatement cached : stmtCache.values()) {
                if (cached == ps) return true;
            }
        }
        return false;
    }

    /**
     * Close the given ResultSet and its creating Statement (if tracked).
     *
     * @param rs ResultSet to close (maybe null)
     */
    public void closeQuery(ResultSet rs) {
        if (rs == null) return;

        Statement stmt = null;
        try {
            stmt = openStatements.remove(rs);
        } catch (Exception ignored) {
        }

        try {
            if (!rs.isClosed()) rs.close();
        } catch (SQLException ignored) {
        }

        // IMPORTANT:
        // If it's a cached PreparedStatement, we do NOT close it here (we keep it for reuse).
        if (stmt instanceof PreparedStatement ps) {
            if (isCachedPreparedStatement(ps)) {
                return; // keep cached prepared statements alive
            }
            // Not cached -> close to avoid leaks
            try {
                if (!ps.isClosed()) ps.close();
            } catch (SQLException ignored) {
            }
            return;
        }

        if (stmt != null) {
            try {
                if (!stmt.isClosed()) stmt.close();
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
     * Executes a raw SQL update/DDL string.
     *
     * <p><b>SECURITY WARNING:</b> Passing user-controlled input into this method can lead to SQL injection.
     * Prefer {@link #executeUpdate(String, Object...)} (prepared statements).
     * @param sql raw SQL update/DDL string (e.g. "UPDATE users SET last_login = '2024-01-01' WHERE id = 1")
     * @return true if update succeeded, false on error
     */
    @Deprecated
    public boolean executeUpdate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Aktualisierung: SQL ist leer");
            return false;
        }

        // Very small guardrail: reject obvious multi-statement / comment injection patterns.
        // This is NOT a complete SQL injection defense; prepared statements are the correct fix.
        if (looksLikeMultiStatementOrComment(sql)) {
            logger.warn("Rejected potentially unsafe raw SQL in executeUpdate(): {}", sql);
            Main.logUtils.addLog("Unsichere SQL-Aktualisierung abgelehnt (verwende PreparedStatement)");
            return false;
        }

        try (Statement stmt = connection.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            stmt.executeUpdate(sql);
            return true;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdate: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes a parameterized update/DDL using a cached PreparedStatement.
     *
     * <p>Use SQL placeholders ("?") and pass parameters via varargs:
     * <pre>
     *     executeUpdate("UPDATE users SET last_login = ? WHERE id = ?", lastLogin, userId);
     * </pre>
     *
     * @param params parameters to bind to the placeholders (e.g. lastLogin, userId)
     * @param sql    SQL string with "?" placeholders (e.g. "UPDATE users SET last_login = ? WHERE id = ?")
     * @return true if update succeeded, false on error
     */
    public boolean executeUpdate(String sql, Object... params) {
        return executePreparedUpdate(sql, params);
    }

    /**
     * Executes a raw SQL update/DDL string and returns affected rows count (or -1 on error).
     *
     * <p><b>SECURITY WARNING:</b> Passing user-controlled input into this method can lead to SQL injection.
     * Prefer {@link #executeUpdateWithCount(String, Object...)} (prepared statements).
     *
     * @return affected rows count, or -1 if an error occurs
     */
    @Deprecated
    public int executeUpdateWithCount(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Aktualisierung: SQL ist leer");
            return -1;
        }

        // Very small guardrail: reject obvious multi-statement / comment injection patterns.
        // This is NOT a complete SQL injection defense; prepared statements are the correct fix.
        if (looksLikeMultiStatementOrComment(sql)) {
            logger.warn("Rejected potentially unsafe raw SQL in executeUpdateWithCount(): {}", sql);
            Main.logUtils.addLog("Unsichere SQL-Aktualisierung abgelehnt (verwende PreparedStatement)");
            return -1;
        }

        try (Statement stmt = connection.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdateWithCount: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Executes a parameterized update/DDL and returns affected rows count (or -1 on error).
     *
     * @param sql    SQL string with "?" placeholders (e.g. "UPDATE users SET last_login = ? WHERE id = ?")
     * @param params parameters to bind to the placeholders (e.g. lastLogin, user, Id)
     * @return affected rows count, or -1 if an error occurs
     */
    public int executeUpdateWithCount(String sql, Object... params) {
        try {
            PreparedStatement pstmt = getCachedPreparedStatement("U", sql);
            bindParams(pstmt, params);
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdateWithCount (prepared): ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return -1;
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
     * Close manager: close tracked queries, close cached statements, close connection.
     */
    public void close() {
        closeAllOpenQueries();

        synchronized (stmtCache) {
            for (PreparedStatement ps : stmtCache.values()) {
                try {
                    if (ps != null && !ps.isClosed()) ps.close();
                } catch (SQLException ignored) {
                }
            }
            stmtCache.clear();
        }

        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {
        }
    }

    /**
     * Clear all data from the database.
     * WARNING: destructive operation that removes ALL data from ALL tables.
     * Uses a transaction to ensure atomicity (all or nothing).
     *
     * @return true if all tables were cleared successfully, false if any error occurs (in which case no data will be deleted due to rollback).
     */
    public boolean clearDatabase() {
        logger.warn("clearDatabase() called - this will delete all data!");

        String[] tablesToClear = {
                TABLE_WARNINGS,
                TABLE_ORDERS,
                TABLE_USERS,
                TABLE_CLIENTS,
                TABLE_DEPARTMENTS,
                TABLE_ARTICLES,
                TABLE_VENDORS,
                TABLE_LOGS,
                TABLE_NOTES
        };

        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try (Statement stmt = connection.createStatement()) {
                int clearedCount = 0;
                for (String table : tablesToClear) {
                    int rowsDeleted = stmt.executeUpdate("DELETE FROM " + table);
                    getClearedInfo(table, rowsDeleted);
                    Main.logUtils.addLog("Tabelle " + table + " gelöscht, " + rowsDeleted + " Zeilen entfernt.");
                    clearedCount++;
                }
                connection.commit();
                logger.info("Successfully cleared all {} tables", clearedCount);
                return true;
            } catch (SQLException e) {
                logger.error("Failed during clearDatabase() statement loop", e);
                connection.rollback();
                logger.warn("Transaction rolled back - no data was deleted");
                Main.logUtils.addLog("Fehler beim Löschen der Datenbank: " + e.getMessage());
                return false;
            }

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
            return false;
        } finally {
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
     * <p>
     * IMPORTANT: You cannot bind table names with PreparedStatement placeholders.
     * Therefore we whitelist table names and build SQL directly.
     *
     * @param tableName name of the table to clear (must be in ALLOWED_TABLES)
     * @return true if the table was cleared successfully, false if the table name is invalid or an error occurs.
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public boolean clearTable(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            logger.error("clearTable called with invalid table name");
            Main.logUtils.addLog("Invalid table name: " + tableName);
            return false;
        }

        String normalized = tableName.trim();
        if (!ALLOWED_TABLES.contains(normalized)) {
            logger.error("clearTable called with invalid/unauthorized table name: {}", normalized);
            Main.logUtils.addLog("Invalid/unauthorized table name: " + normalized);
            return false;
        }

        try (Statement stmt = connection.createStatement()) {
            int rowsDeleted = stmt.executeUpdate("DELETE FROM " + normalized);
            getClearedInfo(normalized, rowsDeleted);
            Main.logUtils.addLog("Tabelle " + normalized + " gelöscht, " + rowsDeleted + " Zeilen entfernt.");
            return true;
        } catch (SQLException e) {
            logger.error("Failed to clear table: {}", normalized, e);
            Main.logUtils.addLog("Fehler beim Löschen der Tabelle " + normalized + ": " + e.getMessage());
            return false;
        }
    }

    private void getClearedInfo(String tableName, int rowsDeleted) {
        logger.info("Cleared {} rows from table: {}", rowsDeleted, tableName);
    }

    // ---------------------------------------------------------------------------------------------
    // Optional helper API (safe mapping) - prefer this over returning ResultSet long-term
    // ---------------------------------------------------------------------------------------------

    /**
     * Functional interface for mapping a ResultSet row to an object of type T.
     *
     * @param <T> the type of the mapped object
     */
    @FunctionalInterface
    public interface ResultMapper<T> {
        /**
         * Map the current row of the ResultSet to an object of type T.
         *
         * @param rs ResultSet positioned at the current row; caller is responsible for closing it.
         * @return mapped object of type T
         */
        T map(ResultSet rs);
    }

    /**
     * Helper for list-result queries. Returns a list of mapped objects, or an empty list on error.
     *
     * @param sql    SQL string with "?" placeholders (e.g. "SELECT * FROM users WHERE department = ?")
     * @param params parameters to bind to the placeholders (e.g. departmentName)
     * @param mapper function to map each ResultSet row to an object of type T
     * @param <T>    the type of the mapped objects
     * @return list of mapped objects of type T, or an empty list if an error occurs
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public <T> List<T> queryList(String sql, Object[] params, ResultMapper<T> mapper) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out.isEmpty() ? Collections.emptyList() : out;
            }
        } catch (SQLException e) {
            logger.error("queryList failed: {}", sql, e);
            Main.logUtils.addLog("Fehler bei der Abfrage: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Helper for single-result queries. Returns the mapped object or null if no results.
     *
     * @param sql    SQL string with "?" placeholders (e.g. "SELECT * FROM users WHERE id = ?")
     * @param params parameters to bind to the placeholders (e.g. userId)
     * @param mapper function to map the ResultSet row to an object of type T
     * @param <T>    the type of the mapped object
     * @return mapped object of type T or null if no results
     */
    public <T> T queryOne(String sql, Object[] params, ResultMapper<T> mapper) {
        List<T> list = queryList(sql, params, mapper);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * Helper for executing multiple operations in a transaction. If any exception occurs, the transaction is rolled back.
     *
     * @param work a Consumer that accepts a Connection and performs the desired operations; it can throw exceptions to trigger rollback
     * @return true if the transaction was committed successfully, false if it was rolled back due to an exception
     */
    public boolean inTransaction(Consumer<Connection> work) {
        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            work.accept(connection);
            connection.commit();
            return true;
        } catch (SQLException e) {
            logger.error("Transaction failed", e);
            try {
                connection.rollback();
            } catch (SQLException ignored) {
            }
            return false;
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
            }
        }
    }
}