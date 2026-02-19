package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;

/**
 * Simple DatabaseManager for SQLite.
 *
 * Performance notes:
 * - Avoids ParameterMetaData() (slow/unreliable in SQLite)
 * - Adds pragmatic SQLite PRAGMAs (WAL, NORMAL sync, cache_size, mmap_size, busy_timeout, etc.)
 * - Uses a small LRU cache for PreparedStatements (huge win for repeated queries/updates)
 * - clearDatabase() uses a transaction and a single Statement for speed
 * - clearTable() uses a strict whitelist (safe + fast)
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class DatabaseManager {

    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);

    public static final String TABLE_ARTICLES = "articles";
    public static final String TABLE_VENDORS = "vendors";
    public static final String TABLE_ORDERS = "orders";
    public static final String TABLE_CLIENTS = "clients";
    public static final String TABLE_DEPARTMENTS = "departments";
    public static final String TABLE_USERS = "users";
    public static final String TABLE_WARNINGS = "warnings";
    public static final String TABLE_LOGS = "logs";
    public static final String TABLE_NOTES = "notes";

    private static final Set<String> ALLOWED_TABLES = Set.of(
            TABLE_ARTICLES,
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
        String prefix = (path != null && !path.isEmpty())
                ? (path.endsWith("/") ? path : path + "/")
                : "";

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
     * Execute a prepared query and return the ResultSet.
     * Caller must call closeQuery(rs).
     *
     * PreparedStatement is reused from an LRU cache.
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
     *
     * PreparedStatement is reused from an LRU cache.
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
        PreparedStatement ps = stmtCache.get(key);
        if (ps != null && !ps.isClosed()) {
            return ps;
        }

        // Create new and cache it
        ps = connection.prepareStatement(sql);
        stmtCache.put(key, ps);
        return ps;
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
            if (!rs.isClosed()) rs.close();
        } catch (SQLException ignored) {
        }

        // IMPORTANT:
        // If it's a cached PreparedStatement, we do NOT close it here (we keep it for reuse).
        if (stmt instanceof PreparedStatement) {
            // keep cached prepared statements alive
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

    /**
     * Execute update/DDL statement and return affected rows count (or -1 on error).
     */
    public int executeUpdateWithCount(String sql) {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            logger.error("SQL Exception during executeUpdateWithCount: ", e);
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
     *
     * IMPORTANT: You cannot bind table names with PreparedStatement placeholders.
     * Therefore we whitelist table names and build SQL directly.
     */
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

    @FunctionalInterface
    public interface ResultMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    public <T> List<T> queryList(String sql, Object[] params, ResultMapper<T> mapper) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            if (params != null) {
                for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<T> out = new ArrayList<>();
                while (rs.next()) out.add(mapper.map(rs));
                return out;
            }
        } catch (SQLException e) {
            logger.error("queryList failed: {}", sql, e);
            Main.logUtils.addLog("Fehler bei der Abfrage: " + e.getMessage());
            return List.of();
        }
    }

    public <T> T queryOne(String sql, Object[] params, ResultMapper<T> mapper) {
        List<T> list = queryList(sql, params, mapper);
        return list.isEmpty() ? null : list.getFirst();
    }

    public boolean inTransaction(Consumer<Connection> work) {
        boolean autoCommit = true;
        try {
            autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            work.accept(connection);
            connection.commit();
            return true;
        } catch (Exception e) {
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