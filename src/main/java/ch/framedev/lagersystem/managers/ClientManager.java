package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    public boolean insertClient(String firstLastName, String department) {
        String sql = "INSERT INTO clients (firstLastName, department) " +
                "VALUES (?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName, department});
    }

    public boolean existsClient(String firstLastName) {
        String sql = "SELECT * FROM clients WHERE firstLastName = '" + firstLastName + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean updateClient(String firstLastName, String newDepartment) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "UPDATE clients SET department = ? WHERE firstLastName = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{newDepartment, firstLastName});
    }

    public boolean deleteClient(String firstLastName) {
        if (!existsClient(firstLastName)) {
            return false;
        }
        String sql = "DELETE FROM clients WHERE firstLastName = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{firstLastName});
    }

    public List<Map<String, String>> getAllClients() {
        String sql = "SELECT * FROM clients;";
        List<Map<String, String>> clients = new ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                Map<String, String> client = new HashMap<>();
                client.put("firstLastName", resultSet.getString("firstLastName"));
                client.put("department", resultSet.getString("department"));
                clients.add(client);
            }
        } catch (Exception e) {
            // Handle exception if needed
        }
        return clients;
    }
}
