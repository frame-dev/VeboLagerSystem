package ch.framedev.lagersystem.managers;

import static ch.framedev.lagersystem.managers.DatabaseManager.TABLE_HISTORIES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.History;
import ch.framedev.lagersystem.main.Main;

public class HistoryManager {

    private static final Logger logger = LogManager.getLogger(HistoryManager.class);

    private static volatile HistoryManager instance;

    private final DatabaseManager databaseManager;

    protected HistoryManager() {
        this.databaseManager = Main.databaseManager;
        createTable();
    }

    public static HistoryManager getInstance() {
        if (instance == null) {
            synchronized (HistoryManager.class) {
                if (instance == null) {
                    instance = new HistoryManager();
                }
            }
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_HISTORIES + " (" +
                databaseManager.identityColumn("id") + ","
                + "info TEXT(255)" + ","
                + "date TEXT(255)" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
    }

    public boolean insertHistory(History history) {
        if (history == null) {
            logger.warn("Could not insert history: history is null");
            Main.logUtils.addLog("Could not insert history: history is null");
            return false;
        }

        Object[] values = new Object[] {
            history.getInfo(),
            history.getDate()
        };
        String sql = "INSERT INTO " + TABLE_HISTORIES + " (info, date) VALUES (?,?);";
        return databaseManager.executePreparedUpdate(sql, values);
    }

    public List<History> getHistories() {
        List<History> histories = new ArrayList<>();
        String sql = "SELECT info, date FROM " + TABLE_HISTORIES + " ORDER BY id DESC;";
        try (ResultSet resultSet = databaseManager.executeTrustedQuery(sql)) {
            if (resultSet == null) {
                logger.error("Error fetching histories: query returned null ResultSet");
                Main.logUtils.addLog("Error fetching histories: query returned null ResultSet");
                return histories;
            }

            while (resultSet.next()) {
                histories.add(new History(resultSet.getString("info"), resultSet.getString("date")));
            }
        } catch (SQLException ex) {
            logger.error("Error fetching histories: {}", ex.getMessage(), ex);
            Main.logUtils.addLog("Error fetching histories: " + ex.getMessage());
        }
        return histories;
    }
}
