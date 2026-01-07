package ch.framedev.lagersystem.managers;

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
                throw new RuntimeException("Failed to create database directory: " + dir.getAbsolutePath());
            }
        }
        String url = "jdbc:sqlite:" + prefix + fileName;
        try {
            this.connection = DriverManager.getConnection(url);
        } catch (SQLException e) {
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
            return false;
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
            return null;
        }
    }
}