package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Warning;
import ch.framedev.lagersystem.main.Main;

import java.util.List;

public class WarningManager {

    // Singleton Instance
    private static WarningManager instance;
    // Database Manager
    private final DatabaseManager databaseManager;

    /**
     * Private constructor to enforce singleton pattern
     */
    private WarningManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    /**
     * Get the singleton instance of WarningManager
     *
     * @return WarningManager instance
     */
    public static WarningManager getInstance() {
        if (instance == null) {
            instance = new WarningManager();
        }
        return instance;
    }

    /**
     * Create the warnings table if it does not exist
     */
    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS warnings (" +
                "title TEXT," +
                "message TEXT," +
                "type TEXT," +
                "date TEXT," +
                "isResolved TEXT," +
                "isDisplayed TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    /**
     * Insert a new warning into the database
     *
     * @param warning Warning object to insert
     * @return true if insertion was successful, false otherwise
     */
    public boolean insertWarning(Warning warning) {
        String sql = "INSERT INTO warnings (title, message, type, date, isResolved) " +
                "VALUES (?, ?, ?, ?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{
                warning.getTitle(),
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? "true" : "false",
                warning.isDisplayed() ? "true" : "false"
        });
    }

    public boolean hasWarning(String title) {
        String sql = "SELECT * FROM warnings WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isResolved(String title) {
        String sql = "SELECT isResolved FROM warnings WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                return resultSet.getString("isResolved").equals("true");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public boolean resolveWarning(String title) {
        String sql = "UPDATE warnings SET isResolved = 'true' WHERE title = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{title});
    }

    public boolean deleteWarning(String title) {
        String sql = "DELETE FROM warnings WHERE title = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{title});
    }

    public Warning getWarning(String title) {
        String sql = "SELECT * FROM warnings WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                return new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        resultSet.getString("isResolved").equals("true"),
                        resultSet.getString("isDisplayed").equals("true")
                );
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    public boolean isDisplayed(String title) {
        String sql = "SELECT isDisplayed FROM warnings WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            if (resultSet.next()) {
                return resultSet.getString("isDisplayed").equals("true");
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public List<Warning> getAllWarnings() {
        String sql = "SELECT * FROM warnings;";
        List<Warning> warnings = new java.util.ArrayList<>();
        try (var resultSet = databaseManager.executeQuery(sql)) {
            while (resultSet.next()) {
                warnings.add(new Warning(
                        resultSet.getString("title"),
                        resultSet.getString("message"),
                        Warning.WarningType.valueOf(resultSet.getString("type")),
                        resultSet.getString("date"),
                        resultSet.getString("isResolved").equals("true"),
                        resultSet.getString("isDisplayed").equals("true")
                ));
            }
        } catch (Exception e) {
            return warnings;
        }
        return warnings;
    }

    /**
     * Warnungen
     * - Zu wenig Lagerbestand
     * - Muss bestellt werden
     */
}
