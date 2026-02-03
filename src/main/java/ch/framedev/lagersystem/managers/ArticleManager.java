package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ArticleManager with intelligent caching for improved performance.
 * Features:
 * - LRU (Least Recently Used) cache for frequently accessed articles
 * - Thread-safe cache operations
 * - Automatic cache invalidation on updates/deletes
 * - Configurable cache size
 */
@SuppressWarnings("unused")
public class ArticleManager {

    private final Logger logger = LogManager.getLogger(ArticleManager.class);

    private final DatabaseManager databaseManager;
    private static ArticleManager instance;

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
     * Get singleton instance
     */
    public static ArticleManager getInstance() {
        if (instance == null) {
            instance = new ArticleManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS articles (" +
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
        nameCache.entrySet().removeIf(entry ->
                entry.getValue().getArticleNumber().equals(articleNumber));
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
        if (article != null) {
            articleCache.put(article.getArticleNumber(), article);
            nameCache.put(article.getName(), article);
            articleNumberIndex.add(article.getArticleNumber());
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

        String sql = "SELECT 1 FROM articles WHERE articleNumber = ? LIMIT 1;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber})) {
            boolean exists = resultSet.next();
            if (exists) {
                articleNumberIndex.add(articleNumber);
            }
            return exists;
        } catch (Exception e) {
            logger.error("Error while checking if article with number '{}' exists", articleNumber, e);
            Main.logUtils.addLog("Error while checking if article with number " + articleNumber + " exists");
            return false;
        }
    }

    /**
     * Insert a new article into the database
     *
     * @param article Article to insert
     * @return true if successful, false otherwise
     */
    public boolean insertArticle(Article article) {
        if (existsArticle(article.getArticleNumber())) {
            Main.logUtils.addLog("Article with the number " + article.getArticleNumber() + " already exists!");
            return false;
        }
        String sql = "INSERT INTO articles (articleNumber, name, details, stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
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

    /**
     * Update an existing article in the database
     *
     * @param article Article to update
     * @return true if successful, false otherwise
     */
    public boolean updateArticle(Article article) {
        if (!existsArticle(article.getArticleNumber())) {
            return false;
        }
        String sql = "UPDATE articles SET name = ?, details = ?, stockQuantity = ?, minStockLevel = ?, sellPrice = ?, purchasePrice = ?, vendorName = ? " +
                "WHERE articleNumber = ?;";
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
        if (!existsArticle(articleNumber)) {
            return false;
        }
        String sql = "DELETE FROM articles WHERE articleNumber = ?;";
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
        Article cached = articleCache.get(articleNumber);
        if (cached != null) {
            logger.debug("Article {} retrieved from cache", articleNumber);
            return cached;
        }

        String sql = "SELECT * FROM articles WHERE articleNumber = ? LIMIT 1;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber})) {
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
        } catch (Exception e) {
            logger.error("Error while getting article with number '{}'", articleNumber, e);
            Main.logUtils.addLog("Error while getting article with number '" + articleNumber + "'");
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
        Article cached = nameCache.get(name);
        if (cached != null) {
            logger.debug("Article '{}' retrieved from name cache", name);
            return cached;
        }

        String sql = "SELECT * FROM articles WHERE name = ? LIMIT 1;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{name})) {
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
        } catch (Exception e) {
            logger.error("Error while getting article with name '{}'", name, e);
            Main.logUtils.addLog("Error while getting article with name '" + name + "'");
            return null;
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

        String sql = "SELECT * FROM articles;";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
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

            allArticlesLock.writeLock().lock();
            try {
                allArticlesCache = articles;
                allArticlesCacheTime = System.currentTimeMillis();
                logger.debug("All articles cached ({} articles)", articles.size());
            } finally {
                allArticlesLock.writeLock().unlock();
            }

            // Ensure the index is synchronized with the freshly loaded data
            articleNumberIndex.clear();
            articles.forEach(a -> articleNumberIndex.add(a.getArticleNumber()));

            return new ArrayList<>(articles);
        } catch (Exception e) {
            logger.error("Error while getting all articles", e);
            Main.logUtils.addLog("Error while getting all articles");
            return null;
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
        Article article = getArticleByNumber(articleNumber);
        if (article == null) {
            return false;
        }
        int newQuantity = article.getStockQuantity() + quantity;
        article.setStockQuantity(newQuantity);
        return updateArticle(article);
    }

}
