package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"UnusedReturnValue", "DuplicatedCode"})
public class UserManager {

    private static final Logger logger = LogManager.getLogger(UserManager.class);

    private static volatile UserManager instance = null;
    private static final Object LOCK = new Object();
    private final DatabaseManager databaseManager;

    private static final String TABLE_NAME = DatabaseManager.TABLE_USERS;
    private static final long ALL_USERNAMES_CACHE_TTL_MILLIS = 5 * 60 * 1000;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
    private volatile List<String> allUsernamesCache = null;
    private volatile long allUsernamesCacheTime = 0L;

    private UserManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static UserManager getInstance() {
        UserManager local = instance;
        if (local == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new UserManager();
                }
                local = instance;
            }
        }
        return local;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "username TEXT UNIQUE," +
                "orders TEXT" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
    }

    private void invalidateAllUsernamesCache() {
        allUsernamesCache = null;
        allUsernamesCacheTime = 0L;
    }

    public boolean insertUser(User user) {
        if (user == null || user.getName() == null || user.getName().trim().isEmpty()) {
            logger.warn("insertUser called with null/blank user or username");
            return false;
        }
        String username = user.getName().trim();
        if (existsUser(username)) {
            logger.warn("User with name '{}' already exists. Cannot insert.", username);
            return false;
        }
        String sql = "INSERT INTO " + TABLE_NAME + " (username, orders) " +
                "VALUES (?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{username, String.join(",", user.getOrders())});
        if (result) {
            Main.logUtils.addLog("Inserted new user with name '" + username + "'");
            // update cache
            cache.put(username, user);
            invalidateAllUsernamesCache();
        } else {
            Main.logUtils.addLog("Could not insert new user with name '" + username + "'");
        }
        return result;
    }

    public boolean existsUser(String username) {
        if (username == null) return false;
        String normalized = username.trim();
        if (normalized.isEmpty()) return false;

        // prefer cache
        if (cache.containsKey(normalized)) return true;

        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE username = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{normalized})) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if user with name '{}'", normalized, e);
            return false;
        }
    }

    public boolean updateUser(User user) {
        if (user == null || user.getName() == null || user.getName().trim().isEmpty()) {
            logger.warn("updateUser called with null/blank user or username");
            return false;
        }
        String username = user.getName().trim();
        if (!existsUser(username)) {
            sendLogWarnExistsUser(username);
            return false;
        }
        String sql = "UPDATE " + TABLE_NAME + " SET orders = ? WHERE username = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{String.join(",", user.getOrders()), username});
        if (result) {
            Main.logUtils.addLog("Updated user with name '" + username + "'");
            // update cache
            cache.put(username, user);
            invalidateAllUsernamesCache();
        } else {
            Main.logUtils.addLog("Could not update user with name '" + username + "'");
        }
        return result;
    }

    public boolean updateUser(String userName, List<String> orders) {
        if (userName == null || userName.trim().isEmpty()) {
            logger.warn("updateUser(userName, orders) called with null/blank username");
            return false;
        }
        String username = userName.trim();
        List<String> safeOrders = (orders == null) ? List.of() : orders;
        if (!existsUser(username)) {
            sendLogWarnExistsUser(username);
            return false;
        }
        String sql = "UPDATE " + TABLE_NAME + " SET orders = ? WHERE username = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{String.join(",", safeOrders), username});
        if (result) {
            Main.logUtils.addLog("Updated user with name '" + username + "'");
            // update cache entry if present
            User u = cache.get(username);
            if (u != null) {
                u.setOrders(safeOrders);
                cache.put(username, u);
            }
            invalidateAllUsernamesCache();
        } else {
            Main.logUtils.addLog("Could not update user with name '" + username + "'");
        }
        return result;
    }

    private void sendLogWarnExistsUser(String username) {
        logger.warn("User with name '{}' does not exist. Cannot update.", username);
    }

    public boolean deleteUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String normalized = username.trim();
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE username = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{normalized});
        if (result) {
            Main.logUtils.addLog("Deleted user with name '" + normalized + "'");
            cache.remove(normalized);
            invalidateAllUsernamesCache();
        } else {
            Main.logUtils.addLog("Could not delete user with name '" + normalized + "'");
        }
        return result;
    }

    public User getUserByName(String username) {
        if (username == null) return null;
        String normalized = username.trim();
        if (normalized.isEmpty()) return null;

        // try cache first
        User cached = cache.get(normalized);
        if (cached != null) return cached;

        String sql = "SELECT username, orders FROM " + TABLE_NAME + " WHERE username = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{normalized})) {
            if (resultSet.next()) {
                String ordersStr = resultSet.getString("orders");
                List<String> orders = new ArrayList<>();
                if (ordersStr != null && !ordersStr.isEmpty()) {
                    String[] arr = ordersStr.split(",");
                    for (String s : arr) {
                        String trimmed = s.trim();
                        if (!trimmed.isEmpty()) orders.add(trimmed);
                    }
                }
                User u = new User(normalized, orders);
                cache.put(normalized, u);
                return u;
            }
        } catch (Exception e) {
            logger.error("Error while retrieving user with name '{}'", normalized, e);
        }
        return null;
    }

    public List<String> getAllUsernames() {
        long now = System.currentTimeMillis();
        if (allUsernamesCache != null && (now - allUsernamesCacheTime) < ALL_USERNAMES_CACHE_TTL_MILLIS) {
            return allUsernamesCache;
        }

        List<String> usernames = new ArrayList<>();
        String sql = "SELECT username FROM " + TABLE_NAME + ";";
        try (var resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                usernames.add(username);
            }
            allUsernamesCache = Collections.unmodifiableList(usernames);
            allUsernamesCacheTime = System.currentTimeMillis();
            return allUsernamesCache;
        } catch (Exception e) {
            logger.error("Error while retrieving all usernames", e);
        }
        if (allUsernamesCache != null) {
            return allUsernamesCache;
        } else if (!usernames.isEmpty()) {
            return Collections.unmodifiableList(usernames);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Clear both per-user and usernames list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        invalidateAllUsernamesCache();
    }
}
