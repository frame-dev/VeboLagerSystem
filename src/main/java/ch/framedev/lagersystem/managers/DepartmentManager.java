package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DepartmentManager {

    private static DepartmentManager instance;
    private final DatabaseManager databaseManager;

    private DepartmentManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static DepartmentManager getInstance() {
        if (instance == null) {
            instance = new DepartmentManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS departments (" +
                "departmentName TEXT," +
                "kontoNumber TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean insertDepartment(String departmentName, String kontoNumber) {
        if (existsDepartment(departmentName)) {
            return false;
        }
        String sql = "INSERT INTO departments (departmentName, kontoNumber) " +
                "VALUES (?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{departmentName, kontoNumber});
    }

    public boolean existsDepartment(String departmentName) {
        String sql = "SELECT * FROM departments WHERE departmentName = '" + departmentName + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteDepartment(String departmentName) {
        if (!existsDepartment(departmentName)) {
            return false;
        }
        String sql = "DELETE FROM departments WHERE departmentName = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{departmentName});
    }

    public boolean updateDepartment(String departmentName, String newKontoNumber) {
        if (!existsDepartment(departmentName)) {
            return false;
        }
        String sql = "UPDATE departments SET kontoNumber = ? WHERE departmentName = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{newKontoNumber, departmentName});
    }

    public Map<String, Object> getDepartment(String departmentName) {
        String sql = "SELECT * from DEPARTMENTS WHERE departmentName = ?;";
        try(var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{departmentName})) {
            if (resultSet.next()) {
                return Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                );
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Map<String, Object>> getAllDepartments() {
        String sql = "SELECT * FROM departments;";
        List<Map<String, Object>> departments = new ArrayList<>();
        try(var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                departments.add(Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return departments;
    }
}
