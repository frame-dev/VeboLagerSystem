package ch.framedev.lagersystem.managers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.SeperateArticle;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;

/**
 * ArticleManager with intelligent caching for improved performance.
 * Features:
 * - LRU (Least Recently Used) cache for frequently accessed articles
 * - Thread-safe cache operations
 * - Automatic cache invalidation on updates/deletes
 * - Configurable cache size
 */
@SuppressWarnings({"unused", "deprecation"})

public class ArticleManager {
    private static final Logger logger = LogManager.getLogger(ArticleManager.class);

    private final DatabaseManager databaseManager;
    private static volatile ArticleManager instance;

    // Cache configuration
    private static final int MAX_CACHE_SIZE = 500; // Maximum number of cached articles
    private static final int NAME_CACHE_SIZE = 200; // Cache for name lookups

    // LRU Cache for article number lookups (most common)
    private final Map<String, Article> articleCache;

    // Cache for name lookups
    private final Map<String, Article> nameCache;

    // Cache for all articles (invalidated on any modification)
    private volatile List<Article> allArticlesCache;
    private volatile long allArticlesCacheTime;
    private static final long CACHE_EXPIRY_MS = 60000; // 1 minute

    private final ReentrantReadWriteLock allArticlesLock = new ReentrantReadWriteLock();
    // Fast membership lookup for article numbers (avoid linear scans)
    private final Set<String> articleNumberIndex = ConcurrentHashMap.newKeySet();

    private ArticleManager() {
        databaseManager = Main.databaseManager;

        // Initialize LRU caches with thread-safe access
        this.articleCache = Collections.synchronizedMap(new LinkedHashMap<>(
                MAX_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Article> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });

        this.nameCache = Collections.synchronizedMap(new LinkedHashMap<>(
                NAME_CACHE_SIZE + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Article> eldest) {
                return size() > NAME_CACHE_SIZE;
            }
        });

        createTable();
    }

    /**
     * Get a singleton instance
     *
     * @return ArticleManager instance
     */
    public static ArticleManager getInstance() {
        if (instance == null) {
            synchronized (ArticleManager.class) {
                if (instance == null) {
                    instance = new ArticleManager();
                }
            }
        }
        return instance;
    }

    /**
     * For testing or reinitialization: resets the singleton instance (use with caution).
     */
    public static void resetInstance() {
        synchronized (ArticleManager.class) {
            if (instance != null) {
                instance = null;
            }
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_ARTICLES + " (" +
                "articleNumber TEXT," +
                "name TEXT," +
                "details TEXT," +
                "stockQuantity INTEGER," +
                "minStockLevel INTEGER," +
                "sellPrice DOUBLE," +
                "purchasePrice DOUBLE," +
                "vendorName TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
        String sqlSecond = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " (" +
            "\"index\" INTEGER," +
                "articleNumber TEXT," +
                "otherDetails TEXT" +
                ");";
        databaseManager.executeUpdate(sqlSecond);
    }

    /**
     * Invalidate caches related to a specific article
     *
     * @param articleNumber Article number to invalidate
     */
    private void invalidateArticleCache(String articleNumber) {
        articleCache.remove(articleNumber);
        articleNumberIndex.remove(articleNumber);
        // Also need to remove from name cache if present
        synchronized (nameCache) {
            nameCache.entrySet().removeIf(entry -> {
                Article a = entry.getValue();
                String num = a == null ? null : a.getArticleNumber();
                return articleNumber != null && articleNumber.equals(num);
            });
        }
        invalidateAllArticlesList();
    }

    /**
     * Invalidate only the all-articles list cache.
     */
    private void invalidateAllArticlesList() {
        allArticlesLock.writeLock().lock();
        try {
            allArticlesCache = null;
            allArticlesCacheTime = 0;
        } finally {
            allArticlesLock.writeLock().unlock();
        }
    }

    /**
     * Invalidate all caches (called on bulk operations)
     */
    private void invalidateAllCaches() {
        articleCache.clear();
        nameCache.clear();
        articleNumberIndex.clear();
        invalidateAllArticlesList();
        logger.debug("All article caches invalidated");
    }

    /**
     * Add article to the cache
     *
     * @param article Article to cache
     */
    private void cacheArticle(Article article) {
        if (article == null) return;
        String num = article.getArticleNumber();
        String name = article.getName();
        if (num != null && !num.isBlank()) {
            articleCache.put(num, article);
            articleNumberIndex.add(num);
        }
        if (name != null && !name.isBlank()) {
            nameCache.put(name, article);
        }
    }

    /**
     * Check if all articles cache is still valid
     *
     * @return true if valid, false if expired or null
     */
    private boolean isAllArticlesCacheValid() {
        allArticlesLock.readLock().lock();
        try {
            return allArticlesCache != null &&
                    (System.currentTimeMillis() - allArticlesCacheTime) < CACHE_EXPIRY_MS;
        } finally {
            allArticlesLock.readLock().unlock();
        }
    }

    /**
     * Get cache statistics for monitoring
     *
     * @return Map of cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("articleCacheSize", articleCache.size());
        stats.put("nameCacheSize", nameCache.size());
        stats.put("allArticlesCached", allArticlesCache != null);
        stats.put("maxCacheSize", MAX_CACHE_SIZE);
        stats.put("articleNumberIndexSize", articleNumberIndex.size());
        return stats;
    }

    /**
     * Check if an article exists by its article number
     *
     * @param articleNumber Article number to check
     * @return true if exists, false otherwise
     */
    public boolean existsArticle(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) return false;
        // Fast-path: cache check
        if (articleCache.containsKey(articleNumber) || articleNumberIndex.contains(articleNumber)) {
            return true;
        }
        if (isAllArticlesCacheValid()) {
            allArticlesLock.readLock().lock();
            try {
                if (allArticlesCache != null) {
                    return allArticlesCache.stream()
                            .anyMatch(a -> a.getArticleNumber().equals(articleNumber));
                }
            } finally {
                allArticlesLock.readLock().unlock();
            }
        }

        String sql = "SELECT 1 FROM " + DatabaseManager.TABLE_ARTICLES + " WHERE articleNumber = ? LIMIT 1;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber});
            if (resultSet == null) return false;
            boolean exists = resultSet.next();
            if (exists) {
                articleNumberIndex.add(articleNumber);
            }
            return exists;
        } catch (SQLException e) {
            logger.error("Error while checking if article with number '{}' exists", articleNumber, e);
            Main.logUtils.addLog("Error while checking if article with number " + articleNumber + " exists");
            return false;
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    /**
     * Insert a new article into the database
     *
     * @param article Article to insert
     * @return true if successful, false otherwise
     */
    public boolean insertArticle(Article article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        if (existsArticle(article.getArticleNumber())) {
            Main.logUtils.addLog("Article with the number " + article.getArticleNumber() + " already exists!");
            return false;
        }
        String sql = "INSERT INTO " + DatabaseManager.TABLE_ARTICLES + " (articleNumber, name, details, stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        insertVendorForArticle(article);
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{article.getArticleNumber(), article.getName(), article.getDetails(),
                article.getStockQuantity(), article.getMinStockLevel(), article.getSellPrice(),
                article.getPurchasePrice(), article.getVendorName()});

        if (success) {
            cacheArticle(article);
            invalidateAllArticlesList();
            logger.debug("Article {} added to cache", article.getArticleNumber());
            Main.logUtils.addLog("Article with number '" + article.getArticleNumber() + "' inserted");
        } else {
            Main.logUtils.addLog("Error while inserting article with number: " + article.getArticleNumber());
        }

        return success;
    }

    private void insertVendorForArticle(Article article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        VendorManager vendorManager = VendorManager.getInstance();
        if (!vendorManager.existsVendor(article.getVendorName())) {
            Vendor vendor = new Vendor(article.getVendorName(), "", "", "", "", new ArrayList<>(), 0.0);
            vendor.getSuppliedArticles().add(article.getArticleNumber());
            vendorManager.insertVendor(vendor);
        } else {
            Vendor vendor = vendorManager.getVendorByName(article.getVendorName());
            if (vendor != null && !vendor.getSuppliedArticles().contains(article.getArticleNumber())) {
                vendor.getSuppliedArticles().add(article.getArticleNumber());
                vendorManager.updateVendor(vendor);
            }
        }
    }

    /**
     * Update an existing article in the database
     *
     * @param article Article to update
     * @return true if successful, false otherwise
     */
    public boolean updateArticle(Article article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        if (!existsArticle(article.getArticleNumber())) {
            return false;
        }
        String sql = "UPDATE " + DatabaseManager.TABLE_ARTICLES + " SET name = ?, details = ?, stockQuantity = ?, minStockLevel = ?, sellPrice = ?, purchasePrice = ?, vendorName = ? " +
                "WHERE articleNumber = ?;";
        insertVendorForArticle(article);
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{article.getName(), article.getDetails(),
                article.getStockQuantity(), article.getMinStockLevel(), article.getSellPrice(),
                article.getPurchasePrice(), article.getVendorName(), article.getArticleNumber()});

        if (success) {
            invalidateArticleCache(article.getArticleNumber());
            cacheArticle(article);
            logger.debug("Article {} updated in cache", article.getArticleNumber());
            Main.logUtils.addLog("Article with number '" + article.getArticleNumber() + "' updated");
        } else {
            Main.logUtils.addLog("Error while updating article with number: " + article.getArticleNumber());
        }

        return success;
    }

    /**
     * Delete an article from the database by its article number
     *
     * @param articleNumber Article number to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteArticleByNumber(String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        if (!existsArticle(articleNumber)) {
            return false;
        }
        String sql = "DELETE FROM " + DatabaseManager.TABLE_ARTICLES + " WHERE articleNumber = ?;";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{articleNumber});

        if (success) {
            invalidateArticleCache(articleNumber);
            logger.debug("Article {} removed from cache", articleNumber);
            Main.logUtils.addLog("Article with number '" + articleNumber + "' deleted");
        } else {
            Main.logUtils.addLog("Error while deleting article with number: " + articleNumber);
        }

        return success;
    }

    /**
     * Delete an article from the database
     *
     * @param article Article to delete
     * @return true if successful, false otherwise
     */
    public boolean deleteArticle(Article article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        if (existsArticle(article.getArticleNumber())) {
            return deleteArticleByNumber(article.getArticleNumber());
        }
        return false;
    }

    /**
     * Retrieve an article by its article number
     *
     * @param articleNumber Article number to retrieve
     * @return Article object or null if not found
     */
    public Article getArticleByNumber(String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        Article cached = articleCache.get(articleNumber);
        if (cached != null) {
            logger.debug("Article {} retrieved from cache", articleNumber);
            return cached;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ARTICLES + " WHERE articleNumber = ? LIMIT 1;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber});
            if (resultSet == null) return null;
            return getArticle(resultSet);
        } catch (SQLException e) {
            logger.error("Error while getting article with number '{}'", articleNumber, e);
            Main.logUtils.addLog("Error while getting article with number '" + articleNumber + "'");
            return null;
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    private Article getArticle(ResultSet resultSet) throws SQLException {
        if(resultSet == null) return null;
        if (resultSet.next()) {
            Article article = new Article(
                    resultSet.getString("articleNumber"),
                    resultSet.getString("name"),
                    resultSet.getString("details"),
                    resultSet.getInt("stockQuantity"),
                    resultSet.getInt("minStockLevel"),
                    resultSet.getDouble("sellPrice"),
                    resultSet.getDouble("purchasePrice"),
                    resultSet.getString("vendorName")
            );
            cacheArticle(article);
            return article;
        } else {
            return null;
        }
    }

    /**
     * Retrieve an article by its name
     *
     * @param name Name of the article to retrieve
     * @return Article object or null if not found
     */
    public Article getArticleByName(String name) {
        if(name == null) throw new IllegalArgumentException("Article name cannot be null");
        Article cached = nameCache.get(name);
        if (cached != null) {
            logger.debug("Article '{}' retrieved from name cache", name);
            return cached;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ARTICLES + " WHERE name = ? LIMIT 1;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{name});
            if (resultSet == null) return null;
            return getArticle(resultSet);
        } catch (SQLException e) {
            logger.error("Error while getting article with name '{}'", name, e);
            Main.logUtils.addLog("Error while getting article with name '" + name + "'");
            return null;
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    /**
     * Retrieve all articles from the database
     *
     * @return List of all Article objects
     */
    public List<Article> getAllArticles() {
        if (isAllArticlesCacheValid()) {
            allArticlesLock.readLock().lock();
            try {
                if (allArticlesCache != null) {
                    logger.debug("All articles retrieved from cache");
                    return new ArrayList<>(allArticlesCache);
                }
            } finally {
                allArticlesLock.readLock().unlock();
            }
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ARTICLES + ";";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executeQuery(sql, new Object[]{});
            if (resultSet == null) {
                allArticlesLock.writeLock().lock();
                try {
                    allArticlesCache = List.of();
                    allArticlesCacheTime = System.currentTimeMillis();
                } finally {
                    allArticlesLock.writeLock().unlock();
                }
                return List.of();
            }

            List<Article> articles = new ArrayList<>();
            while (resultSet.next()) {
                Article article = new Article(
                        resultSet.getString("articleNumber"),
                        resultSet.getString("name"),
                        resultSet.getString("details"),
                        resultSet.getInt("stockQuantity"),
                        resultSet.getInt("minStockLevel"),
                        resultSet.getDouble("sellPrice"),
                        resultSet.getDouble("purchasePrice"),
                        resultSet.getString("vendorName")
                );
                articles.add(article);
                cacheArticle(article);
            }

            // Ensure the index is synchronized with the freshly loaded data
            articleNumberIndex.clear();
            for (Article a : articles) {
                String num = a == null ? null : a.getArticleNumber();
                if (num != null && !num.isBlank()) articleNumberIndex.add(num);
            }

            allArticlesLock.writeLock().lock();
            try {
                allArticlesCache = articles.isEmpty() ? List.of() : Collections.unmodifiableList(new ArrayList<>(articles));
                allArticlesCacheTime = System.currentTimeMillis();
                logger.debug("All articles cached ({} articles)", articles.size());
            } finally {
                allArticlesLock.writeLock().unlock();
            }

            return new ArrayList<>(articles);
        } catch (SQLException e) {
            logger.error("Error while getting all articles", e);
            Main.logUtils.addLog("Error while getting all articles");
            allArticlesLock.writeLock().lock();
            try {
                allArticlesCache = List.of();
                allArticlesCacheTime = System.currentTimeMillis();
            } finally {
                allArticlesLock.writeLock().unlock();
            }
            return List.of();
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    /**
     * Remove a specified quantity from an article's stock
     *
     * @param articleNumber Article number to update
     * @param quantity      Quantity to remove
     * @return true if successful, false otherwise
     */
    public boolean removeFromStock(String articleNumber, int quantity) {
        if(articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        Article article = getArticleByNumber(articleNumber);
        if (article == null) {
            return false;
        }
        int newQuantity = article.getStockQuantity() - quantity;
        if (newQuantity < 0) {
            newQuantity = 0;
        }
        article.setStockQuantity(newQuantity);
        return updateArticle(article);
    }

    /**
     * Add a specified quantity to an article's stock
     *
     * @param articleNumber Article number to update
     * @param quantity      Quantity to add
     * @return true if successful, false otherwise
     */
    public boolean addToStock(String articleNumber, int quantity) {
        if(articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        Article article = getArticleByNumber(articleNumber);
        if (article == null) {
            return false;
        }
        int newQuantity = article.getStockQuantity() + quantity;
        article.setStockQuantity(newQuantity);
        return updateArticle(article);
    }

    public boolean insertSeperateArticle(SeperateArticle article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        String sql = "INSERT INTO " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " (\"index\", articleNumber, otherDetails) " +
                "VALUES (?, ?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{article.getIndex(), article.getArticleNumber(), article.getOtherDetails()});
    }

    public boolean updateSeperateArticle(SeperateArticle article) {
        if (article == null) throw new IllegalArgumentException("Article cannot be null");
        String sql = "UPDATE " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " SET otherDetails = ? " +
                "WHERE \"index\" = ? AND articleNumber = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{article.getOtherDetails(), article.getIndex(), article.getArticleNumber()});
    }

    public boolean deleteSeperateArticle(int index, String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        String sql = "DELETE FROM " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " WHERE \"index\" = ? AND articleNumber = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{index, articleNumber});
    }

    public boolean existsSeperateArticle(int index, String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        String sql = "SELECT 1 FROM " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " WHERE \"index\" = ? AND articleNumber = ? LIMIT 1;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{index, articleNumber});
            if (resultSet == null) return false;
            return resultSet.next();
        } catch (SQLException e) {
            logger.error("Error while checking if seperate article with number '{}' exists", articleNumber, e);
            Main.logUtils.addLog("Error while checking if seperate article with number '" + articleNumber + "' exists");
            return false;
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    public SeperateArticle getSeperateArticle(int index, String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " WHERE \"index\" = ? AND articleNumber = ? LIMIT 1;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{index, articleNumber});
            if (resultSet == null) return null;
            if (resultSet.next()) {
                return new SeperateArticle(
                        resultSet.getInt("index"),
                        resultSet.getString("articleNumber"),
                        resultSet.getString("otherDetails")
                );
            } else {
                return null;
            }
        } catch (SQLException e) {
            logger.error("Error while getting seperate article with number '{}'", articleNumber, e);
            Main.logUtils.addLog("Error while getting seperate article with number '" + articleNumber + "'");
            return null;
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    public List<SeperateArticle> getAllFromArticleNumber(String articleNumber) {
        if (articleNumber == null) throw new IllegalArgumentException("Article number cannot be null");
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_SEPERATE_ARTICLES + " WHERE articleNumber = ?;";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber});
            if (resultSet == null) return List.of();
            List<SeperateArticle> articles = new ArrayList<>();
            while (resultSet.next()) {
                SeperateArticle article = new SeperateArticle(
                        resultSet.getInt("index"),
                        resultSet.getString("articleNumber"),
                        resultSet.getString("otherDetails")
                );
                articles.add(article);
            }
            return articles;
        } catch (SQLException e) {
            logger.error("Error while getting seperate articles with number '{}'", articleNumber, e);
            Main.logUtils.addLog("Error while getting seperate articles with number '" + articleNumber + "'");
            return List.of();
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

    public List<SeperateArticle> getAllSeperateArticles() {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_SEPERATE_ARTICLES + ";";
        ResultSet resultSet = null;
        try {
            resultSet = databaseManager.executePreparedQuery(sql, new Object[]{});
            if (resultSet == null) return List.of();
            List<SeperateArticle> articles = new ArrayList<>();
            while (resultSet.next()) {
                SeperateArticle article = new SeperateArticle(
                        resultSet.getInt("index"),
                        resultSet.getString("articleNumber"),
                        resultSet.getString("otherDetails")
                );
                articles.add(article);
            }
            return articles;
        } catch (SQLException e) {
            logger.error("Error while getting all seperate articles", e);
            Main.logUtils.addLog("Error while getting all seperate articles");
            return List.of();
        } finally {
            databaseManager.closeQuery(resultSet);
        }
    }

}
