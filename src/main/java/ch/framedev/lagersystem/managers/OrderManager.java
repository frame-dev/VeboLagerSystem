package ch.framedev.lagersystem.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.ArticleUtils;

@SuppressWarnings({"UnusedReturnValue", "DuplicatedCode"})
public class OrderManager {

    private static final Logger logger = LogManager.getLogger(OrderManager.class);
    private static final Gson GSON = new Gson();

    private static volatile OrderManager instance = null;
    private final DatabaseManager databaseManager;

    // ==================== Cache ====================
    private final ConcurrentHashMap<String, Order> cache = new ConcurrentHashMap<>();
    private volatile List<Order> allOrdersCache = null;
    private volatile long allOrdersCacheTime = 0L;

    private OrderManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static OrderManager getInstance() {
        OrderManager local = instance;
        if (local == null) {
            synchronized (OrderManager.class) {
                if (instance == null) {
                    instance = new OrderManager();
                }
                local = instance;
            }
        }
        return local;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + DatabaseManager.TABLE_ORDERS + " (" +
                "orderId TEXT UNIQUE," +
                "orderedArticles TEXT," +
                "articleSizes TEXT," +
                "articleColors TEXT," +
                "articleFillings TEXT," +
                "receiverName TEXT," +
                "receiverKontoNumber TEXT," +
                "orderDate TEXT," +
                "senderName TEXT," +
                "senderKontoNumber TEXT," +
                "department TEXT," +
                "status TEXT" +
                ");";
        databaseManager.executeTrustedUpdate(sql);
        ensureMetadataColumns();
    }

    private void ensureMetadataColumns() {
        List<String> existingColumns = new ArrayList<>(databaseManager.getTableColumns(DatabaseManager.TABLE_ORDERS));

        ensureColumnExists(existingColumns, "articleSizes");
        ensureColumnExists(existingColumns, "articleColors");
        ensureColumnExists(existingColumns, "articleFillings");
    }

    private void ensureColumnExists(List<String> existingColumns, String columnName) {
        boolean exists = existingColumns.stream().anyMatch(name -> columnName.equalsIgnoreCase(name));
        if (!exists) {
            databaseManager.executeTrustedUpdate("ALTER TABLE " + DatabaseManager.TABLE_ORDERS
                    + " ADD COLUMN " + columnName + " TEXT;");
        }
    }

    private static String normalizeId(String orderId) {
        if (orderId == null) return null;
        String id = orderId.trim();
        return id.isEmpty() ? null : id;
    }

    private static String serializeOrderedArticles(Map<String, Integer> orderedArticles) {
        if (orderedArticles == null || orderedArticles.isEmpty()) return "";
        return orderedArticles.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .map(OrderManager::serializeArticleEntry)
                .collect(Collectors.joining(","));
    }

    private static String serializeArticleEntry(Entry<String, Integer> e) {
        if(e.getValue() == null) {
            return e.getKey().trim() + ":0";
        }
        return e.getKey().trim() + ":" + Math.max(0, e.getValue());
    }

    private static Map<String, Integer> parseOrderedArticles(String orderedArticlesStr) {
        Map<String, Integer> orderedArticles = new HashMap<>();
        if (orderedArticlesStr == null || orderedArticlesStr.isBlank()) {
            return orderedArticles;
        }
        String[] articlesArray = orderedArticlesStr.split(",");
        for (String articleEntry : articlesArray) {
            if (articleEntry == null || articleEntry.isBlank()) continue;
            String[] parts = articleEntry.split(":", 2);
            if (parts.length != 2) continue;
            String articleNumber = parts[0].trim();
            if (articleNumber.isEmpty()) continue;
            try {
                int quantity = Integer.parseInt(parts[1].trim());
                orderedArticles.put(articleNumber, quantity);
            } catch (NumberFormatException ignored) {
                // skip malformed quantity
            }
        }
        return orderedArticles;
    }

    private static String serializeArticleMetadata(Map<String, String> articleMetadata) {
        if (articleMetadata == null || articleMetadata.isEmpty()) return "";
        Map<String, String> sanitized = new HashMap<>();
        for (Map.Entry<String, String> entry : articleMetadata.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            String filling = entry.getValue();
            if (filling == null || filling.isBlank()) continue;
            sanitized.put(entry.getKey().trim(), filling.trim());
        }
        return sanitized.isEmpty() ? "" : GSON.toJson(sanitized);
    }

    private static Map<String, String> parseArticleMetadata(String articleMetadataStr) {
        if (articleMetadataStr == null || articleMetadataStr.isBlank()) {
            return new HashMap<>();
        }
        try {
            java.lang.reflect.Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            Map<String, String> parsed = GSON.fromJson(articleMetadataStr, mapType);
            return parsed == null ? new HashMap<>() : new HashMap<>(parsed);
        } catch (Exception ignored) {
            return new HashMap<>();
        }
    }

    private void invalidateAllOrdersCache() {
        allOrdersCache = null;
        allOrdersCacheTime = 0L;
    }

    public boolean existsOrder(String orderId) {
        String id = normalizeId(orderId);
        if (id == null) return false;
        // prefer cache
        if (cache.containsKey(id)) return true;

        String sql = "SELECT 1 FROM " + DatabaseManager.TABLE_ORDERS + " WHERE orderId = ? LIMIT 1;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{id})) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if order with id '{}'", id, e);
            Main.logUtils.addLog("Error while checking if order with id '" + id + "'");
            return false;
        }
    }

    public boolean insertOrder(Order order) {
        if (order == null) return false;
        String id = normalizeId(order.getOrderId());
        if (id == null) return false;

        if (existsOrder(id)) {
            return false;
        }

        String articlesStr = serializeOrderedArticles(order.getOrderedArticles());
        String sizesStr = serializeArticleMetadata(order.getArticleSizes());
        String colorsStr = serializeArticleMetadata(order.getArticleColors());
        String fillingsStr = serializeArticleMetadata(order.getArticleFillings());

        String sql = "INSERT INTO " + DatabaseManager.TABLE_ORDERS + " (orderId, orderedArticles, articleSizes, articleColors, articleFillings, receiverName, receiverKontoNumber, orderDate, senderName, senderKontoNumber, department, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                id,
                articlesStr,
                sizesStr,
                colorsStr,
                fillingsStr,
                order.getReceiverName(),
                order.getReceiverKontoNumber(),
                order.getOrderDate(),
                order.getSenderName(),
                order.getSenderKontoNumber(),
                order.getDepartment(),
                order.getStatus()
        });

        if (result) {
            Main.logUtils.addLog("Order with id '" + id + "' inserted");
            cache.put(id, order);
            invalidateAllOrdersCache();
            return true;
        }

        logger.error("Error while inserting order with id '{}'", id);
        Main.logUtils.addLog("Error while inserting order with id '" + id + "'");
        return false;
    }

    public boolean updateOrder(Order order) {
        if (order == null) return false;
        String id = normalizeId(order.getOrderId());
        if (id == null) return false;

        if (!existsOrder(id)) {
            Main.logUtils.addLog("Order with id '" + id + "' does not exist");
            return false;
        }

        String articlesStr = serializeOrderedArticles(order.getOrderedArticles());
        String sizesStr = serializeArticleMetadata(order.getArticleSizes());
        String colorsStr = serializeArticleMetadata(order.getArticleColors());
        String fillingsStr = serializeArticleMetadata(order.getArticleFillings());

        String sql = "UPDATE " + DatabaseManager.TABLE_ORDERS +
                " SET orderedArticles = ?, articleSizes = ?, articleColors = ?, articleFillings = ?, receiverName = ?, receiverKontoNumber = ?, orderDate = ?, senderName = ?, senderKontoNumber = ?, department = ?, status = ? " +
                "WHERE orderId = ?;";

        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                articlesStr,
                sizesStr,
                colorsStr,
                fillingsStr,
                order.getReceiverName(),
                order.getReceiverKontoNumber(),
                order.getOrderDate(),
                order.getSenderName(),
                order.getSenderKontoNumber(),
                order.getDepartment(),
                order.getStatus(),
                id
        });

        if (result) {
            Main.logUtils.addLog("Order with id '" + id + "' updated");
            cache.put(id, order);
            invalidateAllOrdersCache();
            return true;
        }

        logger.error("Error while updating order with id '{}'", id);
        Main.logUtils.addLog("Error while updating order with id '" + id + "'");
        return false;
    }

    public boolean deleteOrder(String orderId) {
        String id = normalizeId(orderId);
        if (id == null) return false;

        if (!existsOrder(id)) {
            return false;
        }

        String sql = "DELETE FROM " + DatabaseManager.TABLE_ORDERS + " WHERE orderId = ?;";
        boolean ok = databaseManager.executePreparedUpdate(sql, new Object[]{id});

        if (ok) {
            Main.logUtils.addLog("Order with id '" + id + "' deleted");
            cache.remove(id);
            invalidateAllOrdersCache();
            return true;
        }

        logger.error("Error while deleting order with id '{}'", id);
        Main.logUtils.addLog("Error while deleting order with id '" + id + "'");
        return false;
    }

    public Order getOrder(String orderId) {
        String id = normalizeId(orderId);
        if (id == null) return null;

        Order cached = cache.get(id);
        if (cached != null) return cached;

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ORDERS + " WHERE orderId = ?;";
        try (var resultSet = databaseManager.executePreparedQuery(sql, new Object[]{id})) {
            if (resultSet.next()) {
                String orderedArticlesStr = resultSet.getString("orderedArticles");
                Map<String, Integer> orderedArticles = parseOrderedArticles(orderedArticlesStr);
                Map<String, String> articleSizes = parseArticleMetadata(resultSet.getString("articleSizes"));
                Map<String, String> articleColors = parseArticleMetadata(resultSet.getString("articleColors"));
                Map<String, String> articleFillings = parseArticleMetadata(resultSet.getString("articleFillings"));
                Order o = new Order(
                        resultSet.getString("orderId"),
                        new java.util.HashMap<>(orderedArticles),
                        articleSizes,
                        articleColors,
                        articleFillings,
                        resultSet.getString("receiverName"),
                        resultSet.getString("receiverKontoNumber"),
                        resultSet.getString("orderDate"),
                        resultSet.getString("senderName"),
                        resultSet.getString("senderKontoNumber"),
                        resultSet.getString("department"),
                        resultSet.getString("status")
                );
                cache.put(id, o);
                return o;
            }
        } catch (Exception e) {
            logger.error("Error while retrieving order with id '{}'", id, e);
            Main.logUtils.addLog("Error while retrieving order with id '" + id + "'");
        }
        return null;
    }

    public List<Order> getOrders() {
        long now = System.currentTimeMillis();
        long CACHE_TTL_MILLIS = 5 * 60 * 1000;
        if (allOrdersCache != null && (now - allOrdersCacheTime) < CACHE_TTL_MILLIS) {
            return allOrdersCache;
        }

        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ORDERS + ";";
        List<Order> orders = new ArrayList<>();
        try (var resultSet = databaseManager.executeTrustedQuery(sql)) {
            while (resultSet.next()) {
                String orderedArticlesStr = resultSet.getString("orderedArticles");
                Map<String, Integer> orderedArticles = parseOrderedArticles(orderedArticlesStr);
                Map<String, String> articleSizes = parseArticleMetadata(resultSet.getString("articleSizes"));
                Map<String, String> articleColors = parseArticleMetadata(resultSet.getString("articleColors"));
                Map<String, String> articleFillings = parseArticleMetadata(resultSet.getString("articleFillings"));
                Order o = new Order(
                        resultSet.getString("orderId"),
                        orderedArticles,
                        articleSizes,
                        articleColors,
                        articleFillings,
                        resultSet.getString("receiverName"),
                        resultSet.getString("receiverKontoNumber"),
                        resultSet.getString("orderDate"),
                        resultSet.getString("senderName"),
                        resultSet.getString("senderKontoNumber"),
                        resultSet.getString("department"),
                        resultSet.getString("status")
                );
                orders.add(o);
                // refresh per-order cache
                cache.put(o.getOrderId(), o);
            }
            allOrdersCache = Collections.unmodifiableList(orders);
            allOrdersCacheTime = System.currentTimeMillis();
            return allOrdersCache;
        } catch (Exception e) {
            logger.error("Error while checking if orders in Database", e);
            Main.logUtils.addLog("Error while checking if orders in Database");
        }
        if (allOrdersCache != null) {
            return allOrdersCache;
        } else if (!orders.isEmpty()) {
            return Collections.unmodifiableList(orders);
        } else {
            return Collections.emptyList();
        }
    }

    public List<Article> getOrderArticles(Order order) {
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        List<Article> articles = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            String articleKey = entry.getKey();
            String articleNumber = ArticleUtils.getOrderItemArticleNumber(articleKey);
            Article article;
            article = ArticleManager.getInstance().getArticleByName(articleNumber);
            if(article == null) {
                article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
            }
            if (article != null) {
                articles.add(article);
            }
        }
        return articles;
    }

    /**
     * Clear both per-order and list caches immediately.
     */
    public void clearCache() {
        cache.clear();
        invalidateAllOrdersCache();
    }
}
