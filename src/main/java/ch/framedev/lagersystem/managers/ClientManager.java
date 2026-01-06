package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientManager {

    private static ClientManager instance;
    private final DatabaseManager databaseManager;

    private ClientManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS clients (" +
                "firstLastName TEXT," +
                "kontoNumber TEXT," +
                "department TEXT," +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public static ClientManager getInstance() {
        if (instance == null) {
            instance = new ClientManager();
        }
        return instance;
    }

    public boolean insertClient(String firstLastName, String kontoNumber, String department) {
        String sql = "INSERT INTO clients (firstLastName, kontoNumber, department) " +
                "VALUES (?, ?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName, kontoNumber, department});
    }

    public boolean existsClient(String firstLastName, String kontoNumber) {
        String sql = "SELECT * FROM clients WHERE firstLastName = '" + firstLastName + "' AND kontoNumber = '" + kontoNumber + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateClient(String firstLastName, String kontoNumber, String newDepartment) {
        if (!existsClient(firstLastName, kontoNumber)) {
            return false;
        }
        String sql = "UPDATE clients SET department = ? WHERE firstLastName = ? AND kontoNumber = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{newDepartment, firstLastName, kontoNumber});
    }

    public boolean deleteClient(String firstLastName, String kontoNumber) {
        if (!existsClient(firstLastName, kontoNumber)) {
            return false;
        }
        String sql = "DELETE FROM clients WHERE firstLastName = ? AND kontoNumber = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName, kontoNumber});
    }

    public List<Map<String, String>> getAllClients() {
        String sql = "SELECT * FROM clients;";
        List<Map<String, String>> clients = new ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                Map<String, String> client = new HashMap<>();
                client.put("firstLastName", resultSet.getString("firstLastName"));
                client.put("kontoNumber", resultSet.getString("kontoNumber"));
                client.put("department", resultSet.getString("department"));
                clients.add(client);
            }
        } catch (Exception e) {
            // Handle exception if needed
        }
        return clients;
    }
}
