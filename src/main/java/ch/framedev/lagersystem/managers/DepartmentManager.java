package ch.framedev.lagersystem.managers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.Variables;

/**
 * This class manages the departments in the database. It provides methods to create, read, update and delete departments.
 * It also implements a simple caching mechanism to improve performance when retrieving departments.
 *
 * @author framedev
 */
public class DepartmentManager {

    private static final Logger logger = LogManager.getLogger(DepartmentManager.class);

    private static volatile DepartmentManager instance = null;
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, Map<String, Object>> cache = new ConcurrentHashMap<>();
    private volatile List<Map<String, Object>> allDepartmentsCache = null;
    private volatile long allDepartmentsCacheTime = 0L;
    private static final long CACHE_TTL_MILLIS = Variables.CACHE_TTL_MILLIS;

    private void invalidateCaches() {
        allDepartmentsCache = null;
        allDepartmentsCacheTime = 0L;
    }

    private DepartmentManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    /**
     * Returns the singleton instance of DepartmentManager.
     *
     * @return the singleton instance of DepartmentManager.
     */
    public static DepartmentManager getInstance() {
        if (instance == null) {
            synchronized (DepartmentManager.class) {
                if (instance == null) {
                    instance = new DepartmentManager();
                }
            }
        }
        return instance;
    }

    /**
     * This method creates the table if it does not exist yet.
     * With columns departmentName and kontoNumber.
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_DEPARTMENTS + " (" +
                "departmentName TEXT PRIMARY KEY," +
                "kontoNumber TEXT" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
    }

    /**
     * Inserts a new department into the database.
     *
     * @param departmentName The name of the department.
     * @param kontoNumber    The konto number of the department.
     * @return true if successful, false otherwise.
     */
    public boolean insertDepartment(String departmentName, String kontoNumber) {
        if (departmentName == null || departmentName.trim().isEmpty()) return false;
        departmentName = departmentName.trim();
        if (kontoNumber != null) kontoNumber = kontoNumber.trim();

        if (existsDepartment(departmentName)) {
            return false;
        }
        String sql = "INSERT INTO " + DatabaseManager.TABLE_DEPARTMENTS + " (departmentName, kontoNumber) " +
                "VALUES (?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{departmentName, kontoNumber});
        if (result) {
            Main.logUtils.addLog(String.format("Inserted new department with name '%s'", departmentName));
            // update cache
            Map<String, Object> entry = Map.of("department", departmentName, "kontoNumber", kontoNumber);
            cache.put(departmentName, entry);
            invalidateCaches();
        } else {
            Main.logUtils.addLog(String.format("Could not insert new department with name '%s'", departmentName));
        }
        return result;
    }

    /**
     * Checks if a department with the given name exists.
     *
     * @param departmentName The name of the department.
     * @return true if the department exists, false otherwise.
     */
    public boolean existsDepartment(String departmentName) {
        if (departmentName == null) return false;
        departmentName = departmentName.trim();
        if (departmentName.isEmpty()) return false;

        // prefer cache
        if (cache.containsKey(departmentName)) return true;

        String sql = "SELECT 1 FROM " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = ? LIMIT 1;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{departmentName})) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if department exists: {}", departmentName, e);
            return false;
        }
    }

    /**
     * Deletes the department with the given name from the database.
     *
     * @param departmentName The name of the department to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    public boolean deleteDepartment(String departmentName) {
        if (departmentName == null) return false;
        departmentName = departmentName.trim();
        if (departmentName.isEmpty()) return false;

        if (!existsDepartment(departmentName)) {
            return false;
        }
        String sql = "DELETE FROM " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{departmentName});
        if (result) {
            Main.logUtils.addLog(String.format("Deleted department with name '%s'", departmentName));
            // invalidate cache
            cache.remove(departmentName);
            invalidateCaches();
        } else {
            Main.logUtils.addLog(String.format("Could not delete department with name '%s'", departmentName));
        }
        return result;
    }

    /**
     * Updates the konto number of a department with the given name.
     *
     * @param departmentName The name of the department to update.
     * @param newKontoNumber The new konto number to set.
     * @return true if the update was successful, false otherwise.
     */
    public boolean updateDepartment(String departmentName, String newKontoNumber) {
        if (departmentName == null) return false;
        departmentName = departmentName.trim();
        if (departmentName.isEmpty()) return false;
        if (newKontoNumber != null) newKontoNumber = newKontoNumber.trim();

        if (!existsDepartment(departmentName)) {
            return false;
        }
        String sql = "UPDATE " + DatabaseManager.TABLE_DEPARTMENTS + " SET kontoNumber = ? WHERE departmentName = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{newKontoNumber, departmentName});
        if (result) {
            Main.logUtils.addLog(String.format("Updated department with name '%s'", departmentName));
            // update cache
            Map<String, Object> entry = Map.of("department", departmentName, "kontoNumber", newKontoNumber);
            cache.put(departmentName, entry);
            invalidateCaches();
        } else {
            Main.logUtils.addLog(String.format("Could not update department with name '%s'", departmentName));
        }
        return result;
    }

    /**
     * Retrieves the department with the given name from the database.
     *
     * @param departmentName The name of the department to retrieve.
     * @return a map containing the department's name and konto number, or null if not found.
     */
    public Map<String, Object> getDepartment(String departmentName) {
        if (departmentName == null) return null;
        departmentName = departmentName.trim();
        if (departmentName.isEmpty()) return null;
        // try cache first
        Map<String, Object> cached = cache.get(departmentName);
        if (cached != null) return cached;

        String sql = "SELECT * from " + DatabaseManager.TABLE_DEPARTMENTS + " WHERE departmentName = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{departmentName})) {
            if (resultSet.next()) {
                Map<String, Object> entry = Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                );
                cache.put(departmentName, entry);
                return entry;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error getting department", e);
            Main.logUtils.addLog("Error getting department");
            return null;
        }
    }

    /**
     * Retrieves all departments from the database.
     *
     * @return a list map containing all departments with their names and konto numbers.
     */
    public List<Map<String, Object>> getAllDepartments() {
        long now = System.currentTimeMillis();
        if (allDepartmentsCache != null && (now - allDepartmentsCacheTime) < CACHE_TTL_MILLIS) {
            return allDepartmentsCache;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_DEPARTMENTS + ";";
        List<Map<String, Object>> departments = new ArrayList<>();
        try (var resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                Map<String, Object> entry = Map.of(
                        "department", resultSet.getString("departmentName"),
                        "kontoNumber", resultSet.getString("kontoNumber")
                );
                departments.add(entry);
                // refresh per-department cache
                cache.put((String) entry.get("department"), entry);
            }
            allDepartmentsCache = Collections.unmodifiableList(departments);
            allDepartmentsCacheTime = System.currentTimeMillis();
        } catch (Exception e) {
            logger.error("Error while getting all departments", e);
            Main.logUtils.addLog("Error while getting all departments");
        }
        if (allDepartmentsCache != null) {
            return allDepartmentsCache;
        } else if (!departments.isEmpty()) {
            return Collections.unmodifiableList(departments);
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Clear both per-department and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        invalidateCaches();
    }
}
