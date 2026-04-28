package ch.framedev.lagersystem.managers;

import static ch.framedev.lagersystem.managers.DatabaseManager.TABLE_HISTORIES;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.History;
import ch.framedev.lagersystem.main.Main;

public class HistoryManager {

    private static final Logger logger = LogManager.getLogger(HistoryManager.class);
    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

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

    public static void resetInstance() {
        synchronized (HistoryManager.class) {
            instance = null;
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_HISTORIES + " (" +
                databaseManager.identityColumn("id") + ","
                + "info TEXT(255)" + ","
                + "date TEXT(255)" + ","
                + "articleNumber TEXT" + ","
                + "userName TEXT" + ","
                + "oldStock INTEGER" + ","
                + "newStock INTEGER" + ","
                + "changeAmount INTEGER" + ","
                + "action TEXT" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
        ensureColumn("articleNumber", "TEXT");
        ensureColumn("userName", "TEXT");
        ensureColumn("oldStock", "INTEGER");
        ensureColumn("newStock", "INTEGER");
        ensureColumn("changeAmount", "INTEGER");
        ensureColumn("action", "TEXT");
        databaseManager.executeTrustedUpdate("CREATE INDEX IF NOT EXISTS idx_histories_article_number " +
                "ON " + DatabaseManager.TABLE_HISTORIES + " (articleNumber);");
    }

    private void ensureColumn(String columnName, String sqlType) {
        boolean exists = databaseManager.getTableColumns(TABLE_HISTORIES).stream()
                .anyMatch(columnName::equalsIgnoreCase);
        if (!exists) {
            databaseManager.executeTrustedUpdate("ALTER TABLE " + TABLE_HISTORIES +
                    " ADD COLUMN " + columnName + " " + sqlType + ";");
        }
    }

    public boolean insertHistory(History history) {
        if (history == null) {
            logger.warn("Could not insert history: history is null");
            Main.logUtils.addLog("Could not insert history: history is null");
            return false;
        }

        Object[] values = new Object[] {
            history.getInfo(),
            history.getDate(),
            history.getArticleNumber(),
            history.getUserName(),
            history.getOldStock(),
            history.getNewStock(),
            history.getChangeAmount(),
            history.getAction()
        };
        String sql = "INSERT INTO " + TABLE_HISTORIES +
                " (info, date, articleNumber, userName, oldStock, newStock, changeAmount, action) " +
                "VALUES (?,?,?,?,?,?,?,?);";
        return databaseManager.executePreparedUpdate(sql, values);
    }

    public boolean insertStockChange(String articleNumber, String articleName, int oldStock, int newStock,
                                     String userName) {
        int changeAmount = newStock - oldStock;
        String safeArticleName = articleName == null || articleName.isBlank() ? "-" : articleName.trim();
        String safeUserName = userName == null || userName.isBlank() ? "System" : userName.trim();
        String action = changeAmount > 0 ? "STOCK_INCREASED" : "STOCK_DECREASED";
        String info = String.format(
                "Artikelbestand geändert: %s - %s | alter Bestand: %d | neuer Bestand: %d | Änderung: %+d | Benutzer: %s",
                articleNumber, safeArticleName, oldStock, newStock, changeAmount, safeUserName);
        return insertHistory(new History(
                info,
                LocalDateTime.now().format(HISTORY_TIMESTAMP_FORMAT),
                articleNumber,
                safeUserName,
                oldStock,
                newStock,
                changeAmount,
                action));
    }

    public boolean insertArticleEvent(String articleNumber, String articleName, String action, String message,
                                      String userName) {
        String safeArticleNumber = articleNumber == null || articleNumber.isBlank() ? "<unbekannt>" : articleNumber.trim();
        String safeArticleName = articleName == null || articleName.isBlank() ? "-" : articleName.trim();
        String safeAction = action == null || action.isBlank() ? "ARTICLE_EVENT" : action.trim();
        String safeUserName = userName == null || userName.isBlank() ? "System" : userName.trim();
        String safeMessage = message == null || message.isBlank() ? "Artikel-Ereignis" : message.trim();
        String info = "Artikel-Ereignis: " + safeArticleNumber + " - " + safeArticleName
                + " | Aktion: " + safeAction
                + " | " + safeMessage
                + " | Benutzer: " + safeUserName;
        return insertHistory(new History(
                info,
                LocalDateTime.now().format(HISTORY_TIMESTAMP_FORMAT),
                safeArticleNumber,
                safeUserName,
                null,
                null,
                null,
                safeAction));
    }

    public List<History> getHistories() {
        List<History> histories = new ArrayList<>();
        String sql = "SELECT info, date, articleNumber, userName, oldStock, newStock, changeAmount, action " +
                "FROM " + TABLE_HISTORIES + " ORDER BY id DESC;";
        try (ResultSet resultSet = databaseManager.executeTrustedQuery(sql)) {
            if (resultSet == null) {
                logger.error("Error fetching histories: query returned null ResultSet");
                Main.logUtils.addLog("Error fetching histories: query returned null ResultSet");
                return histories;
            }

            while (resultSet.next()) {
                histories.add(mapHistory(resultSet));
            }
        } catch (SQLException ex) {
            logger.error("Error fetching histories: {}", ex.getMessage(), ex);
            Main.logUtils.addLog("Error fetching histories: " + ex.getMessage());
        }
        return histories;
    }

    public List<History> getHistoriesForArticle(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return List.of();
        }

        List<History> histories = new ArrayList<>();
        String sql = "SELECT info, date, articleNumber, userName, oldStock, newStock, changeAmount, action " +
                "FROM " + TABLE_HISTORIES + " WHERE articleNumber = ? ORDER BY id DESC;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber})) {
            while (resultSet.next()) {
                histories.add(mapHistory(resultSet));
            }
        } catch (SQLException ex) {
            logger.error("Error fetching histories for article {}: {}", articleNumber, ex.getMessage(), ex);
            Main.logUtils.addLog("Error fetching histories for article " + articleNumber + ": " + ex.getMessage());
        }
        return histories;
    }

    private History mapHistory(ResultSet resultSet) throws SQLException {
        return new History(
                resultSet.getString("info"),
                resultSet.getString("date"),
                resultSet.getString("articleNumber"),
                resultSet.getString("userName"),
                getNullableInt(resultSet, "oldStock"),
                getNullableInt(resultSet, "newStock"),
                getNullableInt(resultSet, "changeAmount"),
                resultSet.getString("action"));
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }
}
