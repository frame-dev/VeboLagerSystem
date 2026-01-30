package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        String sql = "CREATE TABLE IF NOT EXISTS clients (" +
                "firstLastName TEXT PRIMARY KEY," +
                "department TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

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

    public boolean insertClient(String firstLastName, String department) {
        String sql = "INSERT INTO clients (firstLastName, department) VALUES (?, ?);";
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

    public boolean existsClient(String firstLastName) {
        if (departmentCache.containsKey(firstLastName)) {
            return true;
        }
        String sql = "SELECT 1 FROM clients WHERE firstLastName = ? LIMIT 1;";
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

    public boolean updateClient(String firstLastName, String newDepartment) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "UPDATE clients SET department = ? WHERE firstLastName = ?;";
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

    public boolean deleteClient(String firstLastName) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "DELETE FROM clients WHERE firstLastName = ?;";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName});
        if (success) {
            invalidateCaches(firstLastName);
            Main.logUtils.addLog(String.format("Deleted client with name '%s'", firstLastName));
        } else {
            Main.logUtils.addLog(String.format("Could not delete client with name '%s'", firstLastName));
        }
        return success;
    }

    public String getDepartmentByName(String firstLastName) {
        if (departmentCache.containsKey(firstLastName)) {
            return departmentCache.get(firstLastName);
        }
        String sql = "SELECT department FROM clients WHERE firstLastName = ?;";
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

    public List<Map<String, String>> getAllClients() {
        if (allClientsCache != null && System.currentTimeMillis() - allClientsCacheTime < CACHE_EXPIRY_MS) {
            return new ArrayList<>(allClientsCache);
        }

        String sql = "SELECT firstLastName, department FROM clients;";
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
