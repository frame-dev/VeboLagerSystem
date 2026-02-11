package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class DepartmentManager {

    private final Logger logger = LogManager.getLogger(DepartmentManager.class);

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
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_DEPARTMENTS + " (" +
                "departmentName TEXT," +
                "kontoNumber TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean insertDepartment(String departmentName, String kontoNumber) {
        if (existsDepartment(departmentName)) {
            return false;
        }
        String sql = "INSERT INTO " + DatabaseManager.TABLE_DEPARTMENTS + " (departmentName, kontoNumber) " +
                "VALUES (?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{departmentName, kontoNumber});
        if (result) {
            Main.logUtils.addLog(String.format("Inserted new department with name '%s'", departmentName));
        } else {
            Main.logUtils.addLog(String.format("Could not insert new department with name '%s'", departmentName));
        }
        return result;
    }

    public boolean existsDepartment(String departmentName) {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = '" + departmentName + "';";
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
        String sql = "DELETE FROM " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{departmentName});
        if (result) {
            Main.logUtils.addLog(String.format("Deleted department with name '%s'", departmentName));
        } else {
            Main.logUtils.addLog(String.format("Could not delete department with name '%s'", departmentName));
        }
        return result;
    }

    public boolean updateDepartment(String departmentName, String newKontoNumber) {
        if (!existsDepartment(departmentName)) {
            return false;
        }
        String sql = "UPDATE " + DatabaseManager.TABLE_DEPARTMENTS + " SET kontoNumber = ? WHERE departmentName = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{newKontoNumber, departmentName});
        if (result) {
            Main.logUtils.addLog(String.format("Updated department with name '%s'", departmentName));
        } else {
            Main.logUtils.addLog(String.format("Could not update department with name '%s'", departmentName));
        }
        return result;
    }

    public Map<String, Object> getDepartment(String departmentName) {
        String sql = "SELECT * from " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{departmentName})) {
            if (resultSet.next()) {
                return Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                );
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting department", e);
            Main.logUtils.addLog("Error getting department");
            return null;
        }
    }

    public List<Map<String, Object>> getAllDepartments() {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_DEPARTMENTS + ";";
        List<Map<String, Object>> departments = new ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                departments.add(Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                ));
            }
        } catch (Exception e) {
            logger.error("Error while getting all departments", e);
            Main.logUtils.addLog("Error while getting all departments");
        }
        return departments;
    }
}
