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
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_WARNINGS + " (" +
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
        String sql = "INSERT INTO " + DatabaseManager.TABLE_WARNINGS + " (title, message, type, date, isResolved, isDisplayed) " +
                "VALUES (?, ?, ?, ?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                warning.getTitle(),
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? "true" : "false",
                warning.isDisplayed() ? "true" : "false"
        });
        if (result) {
            Main.logUtils.addLog("Inserted new warning with title '" + warning.getTitle() + "'");
        } else {
            Main.logUtils.addLog("Could not insert new warning with title '" + warning.getTitle() + "'");
        }
        return result;
    }

    public boolean hasWarning(String title) {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{title})) {
            return resultSet.next();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isResolved(String title) {
        String sql = "SELECT isResolved FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
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
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS + " SET isResolved = 'true' WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title});
        if(result) {
            Main.logUtils.addLog("Resolved warning with title '" + title + "'");
        } else {
            Main.logUtils.addLog("Could not resolve warning with title '" + title + "'");
        }
        return result;
    }

    /**
     * Update a warning in the database
     *
     * @param warning Warning object to update
     * @return true if update was successful, false otherwise
     */
    public boolean updateWarning(Warning warning) {
        String sql = "UPDATE " + DatabaseManager.TABLE_WARNINGS + " SET message = ?, type = ?, date = ?, isResolved = ?, isDisplayed = ? " +
                "WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                warning.getMessage(),
                warning.getType().name(),
                warning.getDate(),
                warning.isResolved() ? "true" : "false",
                warning.isDisplayed() ? "true" : "false",
                warning.getTitle()
        });
        if (result) {
            Main.logUtils.addLog("Updated warning with title '" + warning.getTitle() + "'");
        } else {
            Main.logUtils.addLog("Could not update warning with title '" + warning.getTitle() + "'");
        }
        return result;
    }

    public boolean deleteWarning(String title) {
        String sql = "DELETE FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{title});
        if (result) {
            Main.logUtils.addLog("Deleted warning with title '" + title + "'");
        } else {
            Main.logUtils.addLog("Could not delete warning with title '" + title + "'");
        }
        return result;
    }

    public Warning getWarning(String title) {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
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
        String sql = "SELECT isDisplayed FROM " + DatabaseManager.TABLE_WARNINGS + " WHERE title = ?;";
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
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_WARNINGS + ";";
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
            System.err.println("[WarningManager] Fehler beim Laden aller Warnungen: " + e.getMessage());
            Main.logUtils.addLog("Fehler beim Laden aller Warnungen");
            e.printStackTrace();
        }
        return warnings;
    }
}
