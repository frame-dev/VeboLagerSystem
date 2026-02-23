package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ClientManager class is responsible for managing client data in the database. It provides methods to create, read, update, and delete client records, as well as to retrieve client information. The class uses caching to improve performance for frequently accessed data, such as client departments and the list of all clients. It also logs all operations performed on clients for auditing purposes.
 * @author framedev
 */
@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "deprecation"})
public class ClientManager {

    private static ClientManager instance;
    private final DatabaseManager databaseManager;

    // Simple caches for fast lookups
    private static final long CACHE_EXPIRY_MS = 60_000; // 1 minute
    private final Map<String, String> departmentCache = Collections.synchronizedMap(new HashMap<>());
    private volatile List<Map<String, String>> allClientsCache;
    private volatile long allClientsCacheTime;

    private ClientManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_CLIENTS + " (" +
                "firstLastName TEXT PRIMARY KEY," +
                "department TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Returns the singleton instance of ClientManager. If the instance does not exist yet, it will be created.
     * @return The singleton instance of ClientManager.
     */
    public static ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    private void invalidateCaches(String firstLastName) {
        if (firstLastName != null) {
            departmentCache.remove(firstLastName);
        }
        allClientsCache = null;
    }

    /**
     * Inserts a new client into the database. If a client with the same name already exists, the method will return false and not perform the insertion.
     * @param firstLastName The name of the client to insert. This must be unique and not null.
     * @param department The department to assign to the client. This can be null if no department is assigned.
     * @return true if the client was successfully inserted, false if a client with the same name already exists or if the insertion failed.
     */
    public boolean insertClient(String firstLastName, String department) {
        String sql = "INSERT INTO " + DatabaseManager.TABLE_CLIENTS + " (firstLastName, department) VALUES (?, ?);";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName, department});
        if (success) {
            departmentCache.put(firstLastName, department);
            allClientsCache = null;
            Main.logUtils.addLog(String.format("Inserted new client with name '%s'", firstLastName));
        } else {
            Main.logUtils.addLog("Could not insert new client with name '" + firstLastName + "'");
        }
        return success;
    }

    /**
     * Checks if a client with the given name exists in the database. This method first checks the cache for a quick lookup before querying the database.
     * @param firstLastName The name of the client to check for existence. This must not be null.
     * @return true if a client with the given name exists, false if no such client exists or if an error occurs during the check.
     */
    public boolean existsClient(String firstLastName) {
        if (departmentCache.containsKey(firstLastName)) {
            return true;
        }
        String sql = "SELECT 1 FROM " + DatabaseManager.TABLE_CLIENTS + " WHERE firstLastName = ? LIMIT 1;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{firstLastName})) {
            boolean exists = resultSet.next();
            if (exists) {
                departmentCache.put(firstLastName, resultSet.getString(1));
            }
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Updates the department of an existing client.
     * @param firstLastName The name of the client to update.
     * @param newDepartment The new department to assign to the client.
     * @return true if the update was successful, false if the client does not exist or the update failed.
     */
    public boolean updateClient(String firstLastName, String newDepartment) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "UPDATE " + DatabaseManager.TABLE_CLIENTS + " SET department = ? WHERE firstLastName = ?;";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{newDepartment, firstLastName});
        if (success) {
            departmentCache.put(firstLastName, newDepartment);
            allClientsCache = null;
            Main.logUtils.addLog(String.format("Updated client with name '%s'", firstLastName));
        } else {
            Main.logUtils.addLog(String.format("Could not update client with name '%s'", firstLastName));
        }
        return success;
    }

    /**
     * Deletes a client from the database. If the client does not exist, the method will return false and not perform any deletion.
     * @param firstLastName The name of the client to delete.
     * @return true if the client was successfully deleted, false if the client does not exist or if the deletion failed.
     */
    public boolean deleteClient(String firstLastName) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "DELETE FROM " + DatabaseManager.TABLE_CLIENTS + " WHERE firstLastName = ?;";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName});
        if (success) {
            invalidateCaches(firstLastName);
            Main.logUtils.addLog(String.format("Deleted client with name '%s'", firstLastName));
        } else {
            Main.logUtils.addLog(String.format("Could not delete client with name '%s'", firstLastName));
        }
        return success;
    }

    /**
     * Retrieves the department of a client by their name. If the client does not exist, the method will return null.
     * @param firstLastName The name of the client whose department should be retrieved.
     * @return The department of the client, or null if the client does not exist or if an error occurs during retrieval.
     */
    public String getDepartmentByName(String firstLastName) {
        if (departmentCache.containsKey(firstLastName)) {
            return departmentCache.get(firstLastName);
        }
        String sql = "SELECT department FROM * " + DatabaseManager.TABLE_CLIENTS + " WHERE firstLastName = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{firstLastName})) {
            if (resultSet.next()) {
                String dept = resultSet.getString("department");
                departmentCache.put(firstLastName, dept);
                return dept;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

        /**
     * Retrieves a list of all clients in the database. Each client is represented as a map with keys "firstLastName" and "department". The method uses caching to improve performance, and the cache is refreshed if it is older than 1 minute.
     * @return A list of maps, where each map represents a client with keys "firstLastName" and "department". If an error occurs during retrieval, an empty list is returned.
     */
    public List<Map<String, String>> getAllClients() {
        if (allClientsCache != null && System.currentTimeMillis() - allClientsCacheTime < CACHE_EXPIRY_MS) {
            return new ArrayList<>(allClientsCache);
        }

        String sql = "SELECT firstLastName, department FROM " + DatabaseManager.TABLE_CLIENTS +";";
        List<Map<String, String>> clients = new ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                Map<String, String> client = new HashMap<>();
                String name = resultSet.getString("firstLastName");
                String dept = resultSet.getString("department");
                client.put("firstLastName", name);
                client.put("department", dept);
                clients.add(client);
                departmentCache.put(name, dept);
            }
            allClientsCache = clients;
            allClientsCacheTime = System.currentTimeMillis();
        } catch (Exception ignored) {
        }
        return clients;
    }
}
