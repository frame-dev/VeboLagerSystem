package ch.framedev.lagersystem.managers;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.Variables;

public class CategoryManager {

    public static final String UNKNOWN_CATEGORY = "Unbekannt";
    private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(\\d+)\\s*(?:-\\s*(\\d+))?\\s*$");
    private static final long ALL_CATEGORIES_CACHE_TTL_MILLIS = Variables.CACHE_TTL_MILLIS;

    private volatile List<Map<String, String>> allCategoriesCache = null;
    private volatile long allCategoriesCacheTime = 0L;

    private static volatile CategoryManager instance;
    private final DatabaseManager databaseManager;

    private CategoryManager() {
        this.databaseManager = Main.databaseManager;
        createTable();
    }

    public static CategoryManager getInstance() {
        if (instance == null) {
            synchronized (CategoryManager.class) {
                if (instance == null) {
                    instance = new CategoryManager();
                }
            }
        }
        return instance;
    }

    public static void resetInstance() {
        instance = null;
    }

    private void invalidateAllCategoriesCache() {
        allCategoriesCache = null;
        allCategoriesCacheTime = 0L;
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
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{category, fromTo});
        if (result) invalidateAllCategoriesCache();
        return result;
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
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{newFromTo, category});
        if (result) invalidateAllCategoriesCache();
        return result;
    }

    public boolean deleteCategory(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        if (!exists(category)) {
            return false;
        }
        String sql = "DELETE FROM " + DatabaseManager.TABLE_CATEGORIES + " WHERE category = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{category});
        if (result) invalidateAllCategoriesCache();
        return result;
    }

    public List<Map<String, String>> getAllCategories() {
        long now = System.currentTimeMillis();
        if (allCategoriesCache != null && (now - allCategoriesCacheTime) < ALL_CATEGORIES_CACHE_TTL_MILLIS) {
            return allCategoriesCache;
        }
        List<Map<String, String>> categories = new ArrayList<>();
        String sql = "SELECT category, fromTo FROM " + DatabaseManager.TABLE_CATEGORIES + ";";
        try (ResultSet resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                Map<String, String> category = new HashMap<>();
                category.put("category", resultSet.getString("category"));
                category.put("fromTo", resultSet.getString("fromTo"));
                categories.add(category);
            }
            allCategoriesCache = Collections.unmodifiableList(categories);
            allCategoriesCacheTime = System.currentTimeMillis();
            return allCategoriesCache;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return allCategoriesCache != null ? allCategoriesCache : Collections.emptyList();
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
