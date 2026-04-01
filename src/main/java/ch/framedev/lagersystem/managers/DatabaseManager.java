package ch.framedev.lagersystem.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.main.Main;

/**
 * Simple DatabaseManager for SQLite and H2.
 * <p>
 * Performance notes:
 * - Avoids ParameterMetaData() (slow/unreliable in SQLite)
 * - Adds pragmatic SQLite Pragmas (WAL, NORMAL sync, cache_size, mmap_size, busy_timeout, etc.)
 * - Uses a small LRU cache for PreparedStatements (huge win for repeated queries/updates)
 * - clearDatabase() uses a transaction and a single Statement for speed
 * - clearTable() uses a strict whitelist (safe + fast)
 */
@SuppressWarnings({"UnusedReturnValue", "SqlWithoutWhere"})

public class DatabaseManager {
    private static final Logger logger = LogManager.getLogger(DatabaseManager.class);
    private static final Gson FILE_GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?\"?([A-Za-z_][A-Za-z0-9_]*)\"?",
            Pattern.CASE_INSENSITIVE);

    public enum DatabaseType {
        SQLITE("sqlite", "SQLite"),
        H2("h2", "H2"),
        JSON("json", "JSON Filesystem"),
        YAML("yaml", "YAML Filesystem");

        private final String configValue;
        private final String displayName;

        DatabaseType(String configValue, String displayName) {
            this.configValue = configValue;
            this.displayName = displayName;
        }

        public String getConfigValue() {
            return configValue;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DatabaseType fromConfig(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return SQLITE;
            }

            String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
            if ("h2".equals(normalized)) {
                return H2;
            }
            if ("json".equals(normalized)) {
                return JSON;
            }
            if ("yaml".equals(normalized) || "yml".equals(normalized)) {
                return YAML;
            }
            return SQLITE;
        }
    }

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
    public static final String TABLE_SEPARATE_ARTICLES = "separate_articles";
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
            TABLE_SEPARATE_ARTICLES,
            TABLE_VENDORS,
            TABLE_ORDERS,
            TABLE_CLIENTS,
            TABLE_DEPARTMENTS,
            TABLE_USERS,
            TABLE_WARNINGS,
            TABLE_LOGS,
            TABLE_NOTES
    );

    private static final List<String> APPLICATION_TABLES = List.of(
            TABLE_WARNINGS,
            TABLE_ORDERS,
            TABLE_USERS,
            TABLE_CLIENTS,
            TABLE_DEPARTMENTS,
            TABLE_ARTICLES,
            TABLE_SEPARATE_ARTICLES,
            TABLE_VENDORS,
            TABLE_LOGS,
            TABLE_NOTES
    );

    private static final Set<String> IDENTITY_MANAGED_TABLES = Set.of(
            TABLE_LOGS,
            TABLE_NOTES
    );

    public static final class MigrationSummary {
        private final DatabaseType sourceType;
        private final DatabaseType targetType;
        private final int tableCount;
        private final int rowCount;
        private final Map<String, Integer> migratedRowsPerTable;

        private MigrationSummary(DatabaseType sourceType,
                                 DatabaseType targetType,
                                 int tableCount,
                                 int rowCount,
                                 Map<String, Integer> migratedRowsPerTable) {
            this.sourceType = sourceType;
            this.targetType = targetType;
            this.tableCount = tableCount;
            this.rowCount = rowCount;
            this.migratedRowsPerTable = migratedRowsPerTable == null
                    ? Collections.emptyMap()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(migratedRowsPerTable));
        }

        public DatabaseType getSourceType() {
            return sourceType;
        }

        public DatabaseType getTargetType() {
            return targetType;
        }

        public int getTableCount() {
            return tableCount;
        }

        public int getRowCount() {
            return rowCount;
        }

        public Map<String, Integer> getMigratedRowsPerTable() {
            return migratedRowsPerTable;
        }
    }

    private final Connection connection;
    private final DatabaseType databaseType;
    private final String databaseUrl;
    private final File filesystemStorageDir;
    private final Set<String> filesystemLoadedTables = Collections.synchronizedSet(new LinkedHashSet<>());
    private final Object filesystemLock = new Object();
    private int filesystemSyncSuspendDepth;

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
        this(DatabaseType.SQLITE, path, fileName);
    }

    /**
     * Open (or create) a SQLite or H2 database at path/fileName.
     *
     * @param databaseType database engine to use
     * @param path         directory path (maybe null or empty)
     * @param fileName     database file name or base name
     */
    public DatabaseManager(DatabaseType databaseType, String path, String fileName) {
        this.databaseType = databaseType == null ? DatabaseType.SQLITE : databaseType;
        String normalizedPath = (path == null) ? "" : path.trim();
        File dir = normalizedPath.isEmpty() ? null : new File(normalizedPath);
        if (dir != null && !dir.exists()) {
            if (!dir.mkdirs()) {
                Main.logUtils.addLog("Fehler beim Erstellen des Datenbank-Verzeichnisses: " + dir.getAbsolutePath());
                throw new RuntimeException("Failed to create database directory: " + dir.getAbsolutePath());
            }
        }

        this.filesystemStorageDir = resolveFilesystemStorageDir(this.databaseType, dir, fileName);
        ensureFilesystemStorageDirectory();
        String url = buildJdbcUrl(this.databaseType, dir, fileName);
        this.databaseUrl = url;
        try {
            loadJdbcDriver(this.databaseType);
            this.connection = DriverManager.getConnection(url);
            applyDatabaseSettings();
        } catch (SQLException | ClassNotFoundException e) {
            Main.logUtils.addLog("Fehler beim Öffnen der Datenbank: " + e.getMessage());
            throw new RuntimeException(buildOpenDatabaseErrorMessage(url, e), e);
        }
    }

    private void loadJdbcDriver(DatabaseType type) throws ClassNotFoundException {
        switch (type) {
            case H2, JSON, YAML -> Class.forName("org.h2.Driver");
            case SQLITE -> Class.forName("org.sqlite.JDBC");
        }
    }

    private String buildOpenDatabaseErrorMessage(String url, Exception cause) {
        if (cause instanceof ClassNotFoundException) {
            return "Failed to open database: " + url
                    + " (JDBC driver class not found at runtime: " + cause.getMessage()
                    + ". Reload the Maven project / runtime classpath and try again.)";
        }
        if (cause instanceof SQLException && cause.getMessage() != null
                && cause.getMessage().contains("No suitable driver")) {
            return "Failed to open database: " + url
                    + " (no suitable JDBC driver registered at runtime. The dependency may not be on the active runtime classpath.)";
        }
        return "Failed to open database: " + url;
    }

    private String buildJdbcUrl(DatabaseType type, File dir, String fileName) {
        return switch (type) {
            case H2 -> "jdbc:h2:file:" + resolveH2DatabasePath(dir, fileName);
            case JSON, YAML -> "jdbc:h2:mem:" + resolveFilesystemDatabaseName(fileName);
            case SQLITE -> {
                File dbFile = (dir == null) ? new File(resolveSqliteFileName(fileName))
                        : new File(dir, resolveSqliteFileName(fileName));
                yield "jdbc:sqlite:" + dbFile.getAbsolutePath();
            }
        };
    }

    private String resolveH2DatabasePath(File dir, String fileName) {
        String normalizedName = (fileName == null || fileName.isBlank()) ? "vebo_lager_system" : fileName.trim();
        normalizedName = stripSuffix(normalizedName, ".mv.db");
        normalizedName = stripSuffix(normalizedName, ".h2.db");
        normalizedName = stripSuffix(normalizedName, ".db");
        if (normalizedName.isBlank()) {
            normalizedName = "vebo_lager_system";
        }

        File dbFile = (dir == null) ? new File(normalizedName) : new File(dir, normalizedName);
        return dbFile.getAbsolutePath().replace('\\', '/');
    }

    private String resolveSqliteFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "vebo_lager_system.db";
        }
        return fileName.trim();
    }

    private File resolveFilesystemStorageDir(DatabaseType type, File dir, String fileName) {
        if (!isFilesystemType(type)) {
            return null;
        }

        String normalizedName = resolveFilesystemDatabaseName(fileName);
        String suffix = type == DatabaseType.JSON ? "_json" : "_yaml";
        File baseDir = dir == null ? new File(normalizedName + suffix) : new File(dir, normalizedName + suffix);
        return new File(baseDir, "tables");
    }

    private String resolveFilesystemDatabaseName(String fileName) {
        String normalizedName = (fileName == null || fileName.isBlank()) ? "vebo_lager_system" : fileName.trim();
        normalizedName = stripSuffix(normalizedName, ".json");
        normalizedName = stripSuffix(normalizedName, ".yaml");
        normalizedName = stripSuffix(normalizedName, ".yml");
        normalizedName = stripSuffix(normalizedName, ".mv.db");
        normalizedName = stripSuffix(normalizedName, ".h2.db");
        normalizedName = stripSuffix(normalizedName, ".db");
        normalizedName = normalizedName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (normalizedName.isBlank()) {
            return "vebo_lager_system";
        }
        return normalizedName;
    }

    private String stripSuffix(String value, String suffix) {
        if (value.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            return value.substring(0, value.length() - suffix.length());
        }
        return value;
    }

    private void applyDatabaseSettings() {
        if (databaseType == DatabaseType.SQLITE) {
            applySQLitePragmas();
            return;
        }
        applyH2Settings();
    }

    private boolean isFilesystemType(DatabaseType type) {
        return type == DatabaseType.JSON || type == DatabaseType.YAML;
    }

    public boolean isFilesystemBackend() {
        return isFilesystemType(databaseType);
    }

    private void ensureFilesystemStorageDirectory() {
        if (filesystemStorageDir == null) {
            return;
        }
        if (!filesystemStorageDir.exists() && !filesystemStorageDir.mkdirs()) {
            throw new RuntimeException("Failed to create filesystem storage directory: "
                    + filesystemStorageDir.getAbsolutePath());
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

    private void applyH2Settings() {
        execStatementSafe("SET DEFAULT_LOCK_TIMEOUT 5000");
    }

    private void execStatementSafe(String sql) {
        try (Statement s = connection.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            logger.debug("Database-specific statement failed (ignored): {}", sql, e);
        }
    }

    /**
     * Executes trusted internal raw SQL query text.
     *
     * <p>This is intended for application-owned schema/setup statements and fixed
     * internal queries where prepared statements are not practical.
     *
     * @param sql trusted SQL query string
     * @return ResultSet or null on error; caller must call {@link #closeQuery(ResultSet)} when done.
     */
    public ResultSet executeTrustedQuery(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Abfrage: SQL ist leer");
            return null;
        }
        try {
            Statement stmt = connection.createStatement();
            @SuppressWarnings("SqlSourceToSinkFlow") ResultSet rs = stmt.executeQuery(sql);
            openStatements.put(rs, stmt);
            return rs;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeTrustedQuery: ", e);
            Main.logUtils.addLog("Fehler bei der Abfrage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Executes trusted internal raw SQL update/DDL text.
     *
     * <p>This is intended for application-owned schema/setup statements and fixed
     * internal maintenance queries where prepared statements are not practical.
     *
     * @param sql trusted SQL update/DDL string
     * @return true if update succeeded, false on error
     */
    public boolean executeTrustedUpdate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Aktualisierung: SQL ist leer");
            return false;
        }
        try (Statement stmt = connection.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            stmt.executeUpdate(sql);
            onTrustedUpdateSuccess(sql);
            return true;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeTrustedUpdate: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return false;
        }
    }

    /**
     * Executes trusted internal raw SQL update/DDL text and returns affected rows count.
     *
     * @param sql trusted SQL update/DDL string
     * @return affected rows count, or -1 if an error occurs
     */
    public int executeTrustedUpdateWithCount(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            Main.logUtils.addLog("Fehler bei der Aktualisierung: SQL ist leer");
            return -1;
        }
        try (Statement stmt = connection.createStatement()) {
            //noinspection SqlSourceToSinkFlow
            int count = stmt.executeUpdate(sql);
            onTrustedUpdateSuccess(sql);
            return count;
        } catch (SQLException e) {
            logger.error("SQL Exception during executeTrustedUpdateWithCount: ", e);
            Main.logUtils.addLog("Fehler bei der Aktualisierung: " + e.getMessage());
            return -1;
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
        return executeTrustedQuery(sql);
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
            onPreparedMutationSuccess();
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
        return executeTrustedUpdate(sql);
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
        return executeTrustedUpdateWithCount(sql);
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
            int count = pstmt.executeUpdate();
            onPreparedMutationSuccess();
            return count;
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

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public boolean isSQLite() {
        return databaseType == DatabaseType.SQLITE;
    }

    public boolean isH2() {
        return databaseType == DatabaseType.H2;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public static String getDefaultFileName(DatabaseType databaseType) {
        return databaseType == DatabaseType.SQLITE ? "vebo_lager_system.db" : "vebo_lager_system";
    }

    public void initializeApplicationSchema() {
        boolean deferFilesystemSync = isFilesystemBackend();
        if (deferFilesystemSync) {
            suspendFilesystemSync();
        }

        try {
            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_ARTICLES + " ("
                    + "articleNumber TEXT,"
                    + "name TEXT,"
                    + "details TEXT,"
                    + "stockQuantity INTEGER,"
                    + "minStockLevel INTEGER,"
                    + "sellPrice DOUBLE,"
                    + "purchasePrice DOUBLE,"
                    + "vendorName TEXT"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_SEPARATE_ARTICLES + " ("
                    + "\"index\" INTEGER,"
                    + "articleNumber TEXT,"
                    + "otherDetails TEXT"
                    + ");");
            executeTrustedUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_seperate_articles_article_detail "
                    + "ON " + TABLE_SEPARATE_ARTICLES + " (articleNumber, otherDetails);");
            executeTrustedUpdate("CREATE INDEX IF NOT EXISTS idx_seperate_articles_article_number "
                    + "ON " + TABLE_SEPARATE_ARTICLES + " (articleNumber);");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_VENDORS + " ("
                    + "name TEXT UNIQUE,"
                    + "contactPerson TEXT,"
                    + "phoneNumber TEXT,"
                    + "email TEXT,"
                    + "address TEXT,"
                    + "suppliedArticles TEXT,"
                    + "minOrderValue DOUBLE"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS + " ("
                    + "orderId TEXT UNIQUE,"
                    + "orderedArticles TEXT,"
                    + "articleSizes TEXT,"
                    + "articleColors TEXT,"
                    + "articleFillings TEXT,"
                    + "receiverName TEXT,"
                    + "receiverKontoNumber TEXT,"
                    + "orderDate TEXT,"
                    + "senderName TEXT,"
                    + "senderKontoNumber TEXT,"
                    + "department TEXT,"
                    + "status TEXT"
                    + ");");
            ensureColumnExists(TABLE_ORDERS, "articleSizes", "TEXT");
            ensureColumnExists(TABLE_ORDERS, "articleColors", "TEXT");
            ensureColumnExists(TABLE_ORDERS, "articleFillings", "TEXT");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_CLIENTS + " ("
                    + "firstLastName TEXT PRIMARY KEY,"
                    + "department TEXT"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_DEPARTMENTS + " ("
                    + "departmentName TEXT PRIMARY KEY,"
                    + "kontoNumber TEXT"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                    + "username TEXT UNIQUE,"
                    + "orders TEXT"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_WARNINGS + " ("
                    + "title TEXT UNIQUE,"
                    + "message TEXT,"
                    + "type TEXT,"
                    + "date TEXT,"
                    + "isResolved TEXT,"
                    + "isDisplayed TEXT"
                    + ");");

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_LOGS + " ("
                    + identityColumn("id") + ","
                    + "timestamp TEXT NOT NULL,"
                    + "epochMillis BIGINT NOT NULL,"
                    + "level TEXT NOT NULL,"
                    + "message TEXT NOT NULL"
                    + ");");
            if (isH2() || isFilesystemBackend()) {
                executeTrustedUpdate("ALTER TABLE " + TABLE_LOGS + " ALTER COLUMN epochMillis BIGINT NOT NULL;");
            }

            executeTrustedUpdate("CREATE TABLE IF NOT EXISTS " + TABLE_NOTES + " ("
                    + identityColumn("id") + ","
                    + "title TEXT NOT NULL UNIQUE,"
                    + "content VARCHAR(2555),"
                    + "date TEXT"
                    + ");");
        } finally {
            if (deferFilesystemSync) {
                restoreFilesystemTablesIfNeeded();
                resumeFilesystemSync();
                syncFilesystemStorage();
            }
        }
    }

    public List<String> listUserTables() {
        LinkedHashSet<String> tables = new LinkedHashSet<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (resultSet.next()) {
                    String schema = resultSet.getString("TABLE_SCHEM");
                    String tableName = resultSet.getString("TABLE_NAME");
                    if (tableName == null || tableName.isBlank()) {
                        continue;
                    }
                    if ("INFORMATION_SCHEMA".equalsIgnoreCase(schema)) {
                        continue;
                    }
                    if (tableName.toLowerCase(Locale.ROOT).startsWith("sqlite_")) {
                        continue;
                    }
                    tables.add(tableName);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to list user tables", e);
            Main.logUtils.addLog("Fehler beim Lesen der Tabellenliste: " + e.getMessage());
        }
        return tables.isEmpty() ? Collections.emptyList() : new ArrayList<>(tables);
    }

    public MigrationSummary migrateDataTo(DatabaseManager target) {
        if (target == null) {
            throw new IllegalArgumentException("Target database must not be null.");
        }
        if (this == target || Objects.equals(databaseUrl, target.databaseUrl)) {
            throw new IllegalArgumentException("Source and target dataset must be different.");
        }

        initializeApplicationSchema();
        target.initializeApplicationSchema();

        Map<String, String> sourceTableLookup = new LinkedHashMap<>();
        for (String sourceTable : listUserTables()) {
            sourceTableLookup.put(sourceTable.toLowerCase(Locale.ROOT), sourceTable);
        }

        LinkedHashSet<String> orderedTables = new LinkedHashSet<>();
        for (String applicationTable : APPLICATION_TABLES) {
            String matchingTable = sourceTableLookup.get(applicationTable.toLowerCase(Locale.ROOT));
            if (matchingTable != null) {
                orderedTables.add(matchingTable);
            }
        }
        orderedTables.addAll(sourceTableLookup.values());

        Map<String, Integer> migratedRowsPerTable = new LinkedHashMap<>();
        boolean autoCommit = true;
        target.suspendFilesystemSync();
        try {
            autoCommit = target.connection.getAutoCommit();
            target.connection.setAutoCommit(false);
            target.clearTablesInternal(target.listUserTables());

            for (String tableName : orderedTables) {
                List<Map<String, Object>> rows = readAllRows(tableName);
                List<String> actualColumns = target.getTableColumns(tableName);
                for (Map<String, Object> row : rows) {
                    target.insertRow(tableName, actualColumns, row);
                }
                target.realignIdentityColumnIfNeeded(tableName);
                migratedRowsPerTable.put(tableName, rows.size());
            }

            target.connection.commit();
        } catch (Exception e) {
            try {
                target.connection.rollback();
            } catch (SQLException rollbackException) {
                logger.error("Failed to rollback migration transaction", rollbackException);
            }
            throw new RuntimeException("Migration failed: " + e.getMessage(), e);
        } finally {
            try {
                target.connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                logger.error("Failed to restore auto-commit after migration", e);
            }
            target.resumeFilesystemSync();
            if (target.isFilesystemBackend()) {
                target.syncFilesystemStorage();
            }
        }

        int totalRows = migratedRowsPerTable.values().stream().mapToInt(Integer::intValue).sum();
        return new MigrationSummary(
                databaseType,
                target.databaseType,
                migratedRowsPerTable.size(),
                totalRows,
                migratedRowsPerTable);
    }

    public String identityColumn(String columnName) {
        String normalized = (columnName == null || columnName.isBlank()) ? "id" : columnName.trim();
        if (databaseType == DatabaseType.H2 || isFilesystemBackend()) {
            return normalized + " INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY";
        }
        return normalized + " INTEGER PRIMARY KEY AUTOINCREMENT";
    }

    public List<String> getTableColumns(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> columns = new LinkedHashSet<>();
        String normalized = tableName.trim();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            collectTableColumns(metaData, normalized, columns);
            if (columns.isEmpty()) {
                collectTableColumns(metaData, normalized.toUpperCase(Locale.ROOT), columns);
            }
            if (columns.isEmpty()) {
                collectTableColumns(metaData, normalized.toLowerCase(Locale.ROOT), columns);
            }
        } catch (SQLException e) {
            logger.error("Failed to read metadata for table: {}", normalized, e);
            Main.logUtils.addLog("Fehler beim Lesen der Tabellenspalten: " + e.getMessage());
        }
        return columns.isEmpty() ? Collections.emptyList() : new ArrayList<>(columns);
    }

    private void collectTableColumns(DatabaseMetaData metaData, String tablePattern, Set<String> columns)
            throws SQLException {
        try (ResultSet resultSet = metaData.getColumns(connection.getCatalog(), null, tablePattern, null)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
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
                TABLE_SEPARATE_ARTICLES,
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
                syncFilesystemStorage();
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

    /**
     * Drops a table when the name is a safe SQL identifier.
     *
     * @param tableName table name to drop
     * @return true if the statement succeeded, false otherwise
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    public boolean dropTableIfExists(String tableName) {
        if (tableName == null || tableName.trim().isEmpty()) {
            logger.error("dropTableIfExists called with invalid table name");
            Main.logUtils.addLog("Invalid table name: " + tableName);
            return false;
        }

        String normalized = tableName.trim();
        if (!normalized.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            logger.error("dropTableIfExists called with unsafe table name: {}", normalized);
            Main.logUtils.addLog("Unsafe table name rejected: " + normalized);
            return false;
        }

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS " + normalized);
            logger.info("Dropped table if exists: {}", normalized);
            Main.logUtils.addLog("Tabelle gelöscht: " + normalized);
            return true;
        } catch (SQLException e) {
            logger.error("Failed to drop table: {}", normalized, e);
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

    private void onTrustedUpdateSuccess(String sql) {
        if (!isFilesystemBackend()) {
            return;
        }
        if (initializeFilesystemTableIfNeeded(sql)) {
            return;
        }
        syncFilesystemStorage();
    }

    private void onPreparedMutationSuccess() {
        if (!isFilesystemBackend()) {
            return;
        }
        syncFilesystemStorage();
    }

    private boolean initializeFilesystemTableIfNeeded(String sql) {
        if (!isFilesystemBackend() || sql == null) {
            return false;
        }

        Matcher matcher = CREATE_TABLE_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return false;
        }

        loadFilesystemTable(matcher.group(1));
        return true;
    }

    private void ensureColumnExists(String tableName, String columnName, String sqlType) {
        if (tableName == null || tableName.isBlank() || columnName == null || columnName.isBlank()
                || sqlType == null || sqlType.isBlank()) {
            return;
        }

        boolean exists = getTableColumns(tableName).stream().anyMatch(columnName::equalsIgnoreCase);
        if (!exists) {
            executeTrustedUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + sqlType + ";");
        }
    }

    private void realignIdentityColumnIfNeeded(String tableName) {
        if ((!isH2() && !isFilesystemBackend()) || tableName == null || tableName.isBlank()) {
            return;
        }

        String normalized = tableName.trim().toLowerCase(Locale.ROOT);
        if (!IDENTITY_MANAGED_TABLES.contains(normalized)) {
            return;
        }

        String actualTableName = resolveActualTableName(tableName);
        if (actualTableName == null) {
            return;
        }

        List<String> columns = getTableColumns(actualTableName);
        String idColumn = columns.stream()
                .filter(column -> "id".equalsIgnoreCase(column))
                .findFirst()
                .orElse(null);
        if (idColumn == null) {
            return;
        }

        String quotedTableName = quoteIdentifier(actualTableName);
        String quotedIdColumn = quoteIdentifier(idColumn);
        String sql = "SELECT COALESCE(MAX(" + quotedIdColumn + "), 0) FROM " + quotedTableName;

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            long nextValue = 1L;
            if (resultSet.next()) {
                nextValue = Math.max(1L, resultSet.getLong(1) + 1L);
            }

            statement.executeUpdate("ALTER TABLE " + quotedTableName
                    + " ALTER COLUMN " + quotedIdColumn
                    + " RESTART WITH " + nextValue);
        } catch (SQLException e) {
            logger.error("Failed to realign identity column for table {}", tableName, e);
            Main.logUtils.addLog("Fehler beim Neujustieren der ID-Spalte für Tabelle "
                    + tableName + ": " + e.getMessage());
        }
    }

    private String resolveActualTableName(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return null;
        }

        String trimmed = tableName.trim();
        for (String existingTable : listUserTables()) {
            if (existingTable != null && existingTable.equalsIgnoreCase(trimmed)) {
                return existingTable;
            }
        }
        return null;
    }

    private void clearTablesInternal(List<String> tables) throws SQLException {
        if (tables == null || tables.isEmpty()) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            for (String tableName : tables) {
                if (tableName == null || tableName.isBlank()) {
                    continue;
                }
                statement.executeUpdate("DELETE FROM " + quoteIdentifier(tableName));
            }
        }
    }

    private static String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void loadFilesystemTable(String tableName) {
        if (!isFilesystemBackend() || tableName == null || tableName.isBlank()) {
            return;
        }

        synchronized (filesystemLock) {
            String normalized = tableName.trim().toLowerCase(Locale.ROOT);
            if (filesystemLoadedTables.contains(normalized)) {
                return;
            }

            String actualTableName = resolveActualTableName(tableName);
            if (actualTableName == null) {
                return;
            }

            filesystemLoadedTables.add(normalized);
            File tableFile = getFilesystemTableFile(normalized);
            if (!tableFile.exists()) {
                return;
            }

            List<Map<String, Object>> rows = readFilesystemRows(tableFile);
            if (rows.isEmpty()) {
                return;
            }

            suspendFilesystemSync();
            try {
                List<String> actualColumns = getTableColumns(actualTableName);
                for (Map<String, Object> row : rows) {
                    insertRow(actualTableName, actualColumns, row);
                }
                realignIdentityColumnIfNeeded(actualTableName);
            } finally {
                resumeFilesystemSync();
            }
        }
    }

    private void insertRow(String tableName, List<String> actualColumns, Map<String, Object> row) {
        if (row == null || row.isEmpty() || actualColumns == null || actualColumns.isEmpty()) {
            return;
        }

        Map<String, String> columnLookup = new HashMap<>();
        for (String column : actualColumns) {
            columnLookup.put(column.toLowerCase(Locale.ROOT), column);
        }

        List<String> insertColumns = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String actualColumn = columnLookup.get(entry.getKey().toLowerCase(Locale.ROOT));
            if (actualColumn == null) {
                continue;
            }
            insertColumns.add(actualColumn);
            values.add(entry.getValue());
        }

        if (insertColumns.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        for (int i = 0; i < insertColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('"').append(insertColumns.get(i)).append('"');
        }
        sql.append(") VALUES (");
        for (int i = 0; i < insertColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append('?');
        }
        sql.append(')');

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bindParams(statement, values.toArray());
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to restore filesystem row for table {}", tableName, e);
            Main.logUtils.addLog("Fehler beim Laden der Dateispeicher-Daten für Tabelle " + tableName + ": "
                    + e.getMessage());
        }
    }

    private void syncFilesystemStorage() {
        if (!isFilesystemBackend() || filesystemStorageDir == null || filesystemSyncSuspendDepth > 0) {
            return;
        }

        synchronized (filesystemLock) {
            ensureFilesystemStorageDirectory();
            Set<String> currentTables = new LinkedHashSet<>();
            try {
                DatabaseMetaData metaData = connection.getMetaData();
                try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                    while (tables.next()) {
                        String schema = tables.getString("TABLE_SCHEM");
                        String tableName = tables.getString("TABLE_NAME");
                        if (tableName == null || tableName.isBlank()) {
                            continue;
                        }
                        if ("INFORMATION_SCHEMA".equalsIgnoreCase(schema)) {
                            continue;
                        }
                        currentTables.add(tableName.toLowerCase(Locale.ROOT));
                        writeFilesystemRows(tableName, readAllRows(tableName));
                    }
                }
                deleteStaleFilesystemTableFiles(currentTables);
            } catch (SQLException e) {
                logger.error("Failed to synchronize filesystem-backed tables", e);
                Main.logUtils.addLog("Fehler beim Synchronisieren der Dateispeicher-Tabellen: " + e.getMessage());
            }
        }
    }

    private void restoreFilesystemTablesIfNeeded() {
        if (!isFilesystemBackend() || filesystemStorageDir == null || !filesystemStorageDir.exists()) {
            return;
        }

        File[] files = filesystemStorageDir.listFiles((dir, name) -> name.endsWith(getFilesystemFileExtension()));
        if (files == null || files.length == 0) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();
            String tableName = fileName.substring(0, fileName.length() - getFilesystemFileExtension().length());
            String actualTableName = resolveActualTableName(tableName);
            if (actualTableName == null || getRowCount(actualTableName) > 0) {
                continue;
            }

            filesystemLoadedTables.remove(tableName.toLowerCase(Locale.ROOT));
            loadFilesystemTable(actualTableName);
        }
    }

    private List<Map<String, Object>> readAllRows(String tableName) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM " + tableName)) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            logger.error("Failed to read rows for filesystem persistence from {}", tableName, e);
            Main.logUtils.addLog("Fehler beim Lesen der Tabelle " + tableName + " für Dateispeicher: "
                    + e.getMessage());
        }
        return rows;
    }

    private int getRowCount(String tableName) {
        if (tableName == null || tableName.isBlank()) {
            return 0;
        }

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Failed to count rows for filesystem restore in table {}", tableName, e);
        }
        return 0;
    }

    private void writeFilesystemRows(String tableName, List<Map<String, Object>> rows) {
        File tableFile = getFilesystemTableFile(tableName.toLowerCase(Locale.ROOT));
        try (FileWriter writer = new FileWriter(tableFile)) {
            if (databaseType == DatabaseType.JSON) {
                FILE_GSON.toJson(rows, writer);
            } else {
                new Yaml().dump(rows, writer);
            }
        } catch (IOException e) {
            logger.error("Failed to write filesystem table {}", tableName, e);
            Main.logUtils.addLog("Fehler beim Schreiben der Dateispeicher-Tabelle " + tableName + ": "
                    + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> readFilesystemRows(File tableFile) {
        try (FileReader reader = new FileReader(tableFile)) {
            Object raw = databaseType == DatabaseType.JSON
                    ? FILE_GSON.fromJson(reader, List.class)
                    : new Yaml().load(reader);
            if (!(raw instanceof List<?> list)) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() != null) {
                            row.put(String.valueOf(entry.getKey()), entry.getValue());
                        }
                    }
                    rows.add(row);
                }
            }
            return rows;
        } catch (IOException e) {
            logger.error("Failed to read filesystem table {}", tableFile.getAbsolutePath(), e);
            Main.logUtils.addLog("Fehler beim Lesen der Dateispeicher-Datei " + tableFile.getName() + ": "
                    + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void deleteStaleFilesystemTableFiles(Set<String> currentTables) {
        File[] files = filesystemStorageDir.listFiles((dir, name) ->
                name.endsWith(getFilesystemFileExtension()));
        if (files == null) {
            return;
        }

        for (File file : files) {
            String name = file.getName();
            String tableName = name.substring(0, name.length() - getFilesystemFileExtension().length());
            String normalizedTableName = tableName.toLowerCase(Locale.ROOT);
            boolean isApplicationTable = APPLICATION_TABLES.stream()
                    .anyMatch(applicationTable -> applicationTable.equalsIgnoreCase(normalizedTableName));
            if (isApplicationTable) {
                continue;
            }
            if (currentTables.contains(normalizedTableName)) {
                continue;
            }
            if (!file.delete()) {
                logger.warn("Failed to delete stale filesystem table file {}", file.getAbsolutePath());
            }
        }
    }

    private File getFilesystemTableFile(String tableName) {
        return new File(filesystemStorageDir, tableName + getFilesystemFileExtension());
    }

    private String getFilesystemFileExtension() {
        return databaseType == DatabaseType.YAML ? ".yaml" : ".json";
    }

    private void suspendFilesystemSync() {
        filesystemSyncSuspendDepth++;
    }

    private void resumeFilesystemSync() {
        if (filesystemSyncSuspendDepth > 0) {
            filesystemSyncSuspendDepth--;
        }
    }
}
