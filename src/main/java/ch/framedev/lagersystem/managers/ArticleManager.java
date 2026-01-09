package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.*;

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
     * Invalidate a specific article from cache
     */
    private void invalidateArticleCache(String articleNumber) {
        articleCache.remove(articleNumber);
        // Also need to remove from name cache if present
        nameCache.entrySet().removeIf(entry ->
            entry.getValue().getArticleNumber().equals(articleNumber));
        // Invalidate all articles cache
        allArticlesCache = null;
    }

    /**
     * Invalidate all caches (called on bulk operations)
     */
    private void invalidateAllCaches() {
        articleCache.clear();
        nameCache.clear();
        allArticlesCache = null;
        logger.debug("All article caches invalidated");
    }

    /**
     * Add article to cache
     */
    private void cacheArticle(Article article) {
        if (article != null) {
            articleCache.put(article.getArticleNumber(), article);
            nameCache.put(article.getName(), article);
        }
    }

    /**
     * Check if all articles cache is still valid
     */
    private boolean isAllArticlesCacheValid() {
        return allArticlesCache != null &&
               (System.currentTimeMillis() - allArticlesCacheTime) < CACHE_EXPIRY_MS;
    }

    /**
     * Get cache statistics for monitoring
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("articleCacheSize", articleCache.size());
        stats.put("nameCacheSize", nameCache.size());
        stats.put("allArticlesCached", allArticlesCache != null);
        stats.put("maxCacheSize", MAX_CACHE_SIZE);
        return stats;
    }

    public boolean existsArticle(String articleNumber) {
        String sql = "SELECT * FROM articles WHERE articleNumber = '" + articleNumber + "';";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if vendor with name '{}'", articleNumber, e);
            return false;
        }
    }

    public boolean insertArticle(Article article) {
        if (existsArticle(article.getArticleNumber())) {
            return false;
        }
        String sql = "INSERT INTO articles (articleNumber, name, details, stockQuantity, minStockLevel, sellPrice, purchasePrice, vendorName) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        VendorManager vendorManager = VendorManager.getInstance();
        if (!vendorManager.existsVendor(article.getVendorName())) {
            Vendor vendor = new Vendor(article.getVendorName(), "", "", "", "", new ArrayList<>());
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
            // Add to cache and invalidate all articles cache
            cacheArticle(article);
            allArticlesCache = null;
            logger.debug("Article {} added to cache", article.getArticleNumber());
        }

        return success;
    }

    public boolean updateArticle(Article article) {
        if (!existsArticle(article.getArticleNumber())) {
            return false;
        }
        String sql = "UPDATE articles SET name = ?, details = ?, stockQuantity = ?, minStockLevel = ?, sellPrice = ?, purchasePrice = ?, vendorName = ? " +
                "WHERE articleNumber = ?;";
        VendorManager vendorManager = VendorManager.getInstance();
        if (!vendorManager.existsVendor(article.getVendorName())) {
            Vendor vendor = new Vendor(article.getVendorName(), "", "", "", "", new ArrayList<>());
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
            // Update cache with new data
            invalidateArticleCache(article.getArticleNumber());
            cacheArticle(article);
            logger.debug("Article {} updated in cache", article.getArticleNumber());
        }

        return success;
    }

    public boolean deleteArticleByNumber(String articleNumber) {
        if (!existsArticle(articleNumber)) {
            return false;
        }
        String sql = "DELETE FROM articles WHERE articleNumber = ?;";
        boolean success = databaseManager.executePreparedUpdate(sql, new Object[]{articleNumber});

        if (success) {
            // Remove from cache
            invalidateArticleCache(articleNumber);
            logger.debug("Article {} removed from cache", articleNumber);
        }

        return success;
    }

    public boolean deleteArticle(Article article) {
        if (existsArticle(article.getArticleNumber())) {
            return deleteArticleByNumber(article.getArticleNumber());
        }
        return false;
    }

    public Article getArticleByNumber(String articleNumber) {
        // Check cache first
        Article cached = articleCache.get(articleNumber);
        if (cached != null) {
            logger.debug("Article {} retrieved from cache", articleNumber);
            return cached;
        }

        // Not in cache, query database
        String sql = "SELECT * FROM articles WHERE articleNumber = ?;";
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
                // Add to cache for future use
                cacheArticle(article);
                return article;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error while getting article with number '{}'", articleNumber, e);
            return null;
        }
    }

    public Article getArticleByName(String name) {
        // Check name cache first
        Article cached = nameCache.get(name);
        if (cached != null) {
            logger.debug("Article '{}' retrieved from name cache", name);
            return cached;
        }

        // Not in cache, query database
        String sql = "SELECT * FROM articles WHERE name = ?;";
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
                // Add to cache for future use
                cacheArticle(article);
                return article;
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error while getting article with name '{}'", name, e);
            return null;
        }
    }

    public List<Article> getAllArticles() {
        // Check if cached list is still valid (not older than 1 minute)
        if (isAllArticlesCacheValid()) {
            logger.debug("All articles retrieved from cache");
            return new ArrayList<>(allArticlesCache); // Return copy to prevent modification
        }

        // Cache expired or not present, query database
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
                // Also add to individual caches
                cacheArticle(article);
            }

            // Cache the result
            allArticlesCache = articles;
            allArticlesCacheTime = System.currentTimeMillis();
            logger.debug("All articles cached ({} articles)", articles.size());

            return new ArrayList<>(articles); // Return copy
        } catch (Exception e) {
            logger.error("Error while getting all articles", e);
            return null;
        }
    }

    public boolean removeFromStock(String articleNumber, int quantity) {
        Article article = getArticleByNumber(articleNumber);
        if(article == null) {
            return false;
        }
        int newQuantity = article.getStockQuantity() - quantity;
        if(newQuantity < 0) {
            newQuantity = 0;
        }
        article.setStockQuantity(newQuantity);
        return updateArticle(article);
    }

    public boolean addToStock(String articleNumber, int quantity) {
        Article article = getArticleByNumber(articleNumber);
        if(article == null) {
            return false;
        }
        int newQuantity = article.getStockQuantity() + quantity;
        article.setStockQuantity(newQuantity);
        return updateArticle(article);
    }

}
