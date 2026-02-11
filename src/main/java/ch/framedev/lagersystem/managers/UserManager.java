package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public class UserManager {

    private final Logger logger = LogManager.getLogger(UserManager.class);

    private static UserManager instance;
    private final DatabaseManager databaseManager;

    private final String TABLE_NAME = DatabaseManager.TABLE_USERS;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, User> cache = new ConcurrentHashMap<>();
    private volatile List<String> allUsernamesCache = null;
    private volatile long allUsernamesCacheTime = 0L;
    private final long CACHE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes

    private UserManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "username TEXT," +
                "orders TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean insertUser(User user) {
        if(existsUser(user.getName())) {
            logger.warn("User with name '{}' already exists. Cannot insert.", user.getName());
            return false;
        }
        String sql = "INSERT INTO " + TABLE_NAME + " (username, orders) " +
                "VALUES (?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{user.getName(), String.join(",", user.getOrders())});
        if(result) {
            Main.logUtils.addLog("Inserted new user with name '" + user.getName() + "'");
            // update cache
            cache.put(user.getName(), user);
            allUsernamesCache = null;
            allUsernamesCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not insert new user with name '" + user.getName() + "'");
        }
        return result;
    }

    public boolean existsUser(String username) {
        if (username == null) return false;
        // prefer cache
        if (cache.containsKey(username)) return true;

        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE username = '" + username + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if user with name '{}'", username, e);
            return false;
        }
    }

    public boolean updateUser(User user) {
        if(!existsUser(user.getName())) {
            sendLogWarnExistsUser(user.getName());
            return false;
        }
        String sql = "UPDATE " + TABLE_NAME + " SET orders = ? WHERE username = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{String.join(",", user.getOrders()), user.getName()});
        if(result) {
            Main.logUtils.addLog("Updated user with name '" + user.getName() + "'");
            // update cache
            cache.put(user.getName(), user);
            allUsernamesCache = null;
            allUsernamesCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not update user with name '" + user.getName() + "'");
        }
        return result;
    }

    public boolean updateUser(String userName, List<String> orders) {
        if(!existsUser(userName)) {
            sendLogWarnExistsUser(userName);
            return false;
        }
        String sql = "UPDATE " + TABLE_NAME + " SET orders = ? WHERE username = ?;";
        boolean result =  databaseManager.executePreparedUpdate(sql, new Object[]{String.join(",", orders), userName});
        if(result) {
            Main.logUtils.addLog("Updated user with name '" + userName + "'");
            // update cache entry if present
            User u = cache.get(userName);
            if (u != null) {
                u.setOrders(orders);
                cache.put(userName, u);
            }
            allUsernamesCache = null;
            allUsernamesCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not update user with name '" + userName + "'");
        }
        return result;
    }

    private void sendLogWarnExistsUser(String username) {
        logger.warn("User with name '{}' does not exist. Cannot update.", username);
    }

    public boolean deleteUser(String username) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE username = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{username});
        if(result) {
            Main.logUtils.addLog("Deleted user with name '" + username + "'");
            // remove from cache
            cache.remove(username);
            allUsernamesCache = null;
            allUsernamesCacheTime = 0L;
        } else {
            Main.logUtils.addLog("Could not delete user with name '" + username + "'");
        }
        return result;
    }

    public User getUserByName(String username) {
        if (username == null) return null;
        // try cache first
        User cached = cache.get(username);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE username = '" + username + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            if (resultSet.next()) {
                String ordersStr = resultSet.getString("orders");
                List<String> orders = new ArrayList<>();
                if (ordersStr != null && !ordersStr.isEmpty()) {
                    String[] arr = ordersStr.split(",");
                    for (String s : arr) {
                        orders.add(s.trim());
                    }
                }
                User u = new User(username, orders);
                cache.put(username, u);
                return u;
            }
        } catch (Exception e) {
            logger.error("Error while retrieving user with name '{}'", username, e);
        }
        return null;
    }

    public List<String> getAllUsernames() {
        long now = System.currentTimeMillis();
        if (allUsernamesCache != null && (now - allUsernamesCacheTime) < CACHE_TTL_MILLIS) {
            return allUsernamesCache;
        }

        List<String> usernames = new ArrayList<>();
        String sql = "SELECT username FROM " + TABLE_NAME + ";";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                String username = resultSet.getString("username");
                usernames.add(username);
            }
            allUsernamesCache = Collections.unmodifiableList(usernames);
            allUsernamesCacheTime = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Error while retrieving all usernames", e);
        }
        return usernames;
    }

    /**
     * Clear both per-user and usernames list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        allUsernamesCache = null;
        allUsernamesCacheTime = 0L;
    }
}
