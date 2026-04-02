package ch.framedev.lagersystem.managers;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.framedev.lagersystem.main.Main;

public class CategoryManager {

    public static final String UNKNOWN_CATEGORY = "Unbekannt";
    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*(?:-\\s*(\\d+))?\\s*$");

    private static CategoryManager instance;
    private final DatabaseManager databaseManager;

    private CategoryManager() {
        this.databaseManager = Main.databaseManager;
        createTable();
    }

    public static synchronized CategoryManager getInstance() {
        if (instance == null) {
            instance = new CategoryManager();
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_CATEGORIES + " ("
                + databaseManager.identityColumn("id") + ","
                + "category TEXT NOT NULL UNIQUE,"
                + "fromTo TEXT NOT NULL"
                + ");";
        databaseManager.executeTrustedUpdate(sql);
    }

    public boolean insertCategory(String category, String fromTo) {
        if (category == null || category.isBlank() || fromTo == null || fromTo.isBlank()) {
            return false;
        }
        if(exists(category))
            return false; // Prevent inserting duplicate category
        String sql = "INSERT INTO " + DatabaseManager.TABLE_CATEGORIES + " (category, fromTo) VALUES (?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{category, fromTo});
    }

    public boolean exists(String category) {
        String sql = "SELECT COUNT(*) FROM " + DatabaseManager.TABLE_CATEGORIES + " WHERE category = ?;";
        try(ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{category})) {
            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateCategory(String category, String newFromTo) {
        if (category == null || category.isBlank() || newFromTo == null || newFromTo.isBlank()) {
            return false;
        }
        if(!exists(category))
            return false; // Prevent updating non-existing category
        String sql = "UPDATE " + DatabaseManager.TABLE_CATEGORIES + " SET fromTo = ? WHERE category = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{newFromTo, category});
    }

    public boolean deleteCategory(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        if (!exists(category)) {
            return false;
        }
        String sql = "DELETE FROM " + DatabaseManager.TABLE_CATEGORIES + " WHERE category = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{category});
    }

    public List<Map<String, String>> getAllCategories() {
        List<Map<String, String>> categories = new ArrayList<>();
        String sql = "SELECT category, fromTo FROM " + DatabaseManager.TABLE_CATEGORIES + ";";
        try (ResultSet resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                Map<String, String> category = new HashMap<>();
                category.put("category", resultSet.getString("category"));
                category.put("fromTo", resultSet.getString("fromTo"));
                categories.add(category);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return categories;
    }

    public String getCategoryForArticle(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return UNKNOWN_CATEGORY;
        }
        String numericPart = articleNumber.replaceAll("[^0-9]", "");
        if (numericPart.isEmpty()) {
            return UNKNOWN_CATEGORY;
        }
        int articleNum;
        try {
            articleNum = Integer.parseInt(numericPart);
        } catch (NumberFormatException ignored) {
            return UNKNOWN_CATEGORY;
        }
        for (Map<String, String> entry : getAllCategories()) {
            String fromTo = entry.get("fromTo");
            if (fromTo == null) continue;
            Matcher m = RANGE_PATTERN.matcher(fromTo);
            if (!m.matches()) continue;
            try {
                int start = Integer.parseInt(m.group(1));
                int end = m.group(2) != null ? Integer.parseInt(m.group(2)) : start;
                if (articleNum >= start && articleNum <= end) {
                    return entry.get("category");
                }
            } catch (NumberFormatException ignored) {
                // skip malformed range entries
            }
        }
        return UNKNOWN_CATEGORY;
    }
}
