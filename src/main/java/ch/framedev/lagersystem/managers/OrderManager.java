package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnusedReturnValue")
public class OrderManager {

    private final Logger logger = LogManager.getLogger(OrderManager.class);

    private static OrderManager instance;
    private final DatabaseManager databaseManager;

    private OrderManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static OrderManager getInstance() {
        if (instance == null) {
            instance = new OrderManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS orders (" +
                "orderId TEXT," +
                "orderedArticles TEXT," +
                "receiverName TEXT," +
                "receiverKontoNumber TEXT," +
                "orderDate TEXT," +
                "senderName TEXT," +
                "senderKontoNumber TEXT," +
                "department TEXT," +
                "status TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean existsOrder(String orderId) {
        String sql = "SELECT * FROM orders WHERE orderId = '" + orderId + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if order with id '{}'", orderId, e);
            Main.logUtils.addLog("Error while checking if order with id '" + orderId + "'");
            return false;
        }
    }

    public boolean insertOrder(Order order) {
        if (existsOrder(order.getOrderId())) {
            return false;
        }
        StringBuilder articlesBuilder = new StringBuilder();
        order.getOrderedArticles().forEach((articleNumber, quantity) ->
                articlesBuilder.append(articleNumber).append(":").append(quantity).append(","));
        if (!articlesBuilder.isEmpty()) {
            articlesBuilder.setLength(articlesBuilder.length() - 1); // Remove trailing comma
        }
        String sql = "INSERT INTO orders (orderId, orderedArticles, receiverName, receiverKontoNumber, orderDate, senderName, senderKontoNumber, department, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{
                order.getOrderId(),
                articlesBuilder.toString(),
                order.getReceiverName(),
                order.getReceiverKontoNumber(),
                order.getOrderDate(),
                order.getSenderName(),
                order.getSenderKontoNumber(),
                order.getDepartment(),
                order.getStatus()
        });
        if(result) {
            Main.logUtils.addLog("Order with id '" + order.getOrderId() + "' inserted");
            return true;
        } else {
            logger.error("Error while inserting order with id '{}'", order.getOrderId());
            Main.logUtils.addLog("Error while inserting order with id '" + order.getOrderId() + "'");
            return false;
        }
    }

    public boolean updateOrder(Order order) {
        if (!existsOrder(order.getOrderId())) {
            Main.logUtils.addLog("Order with id '" + order.getOrderId() + "' does not exist");
            return false;
        }
        StringBuilder articlesBuilder = new StringBuilder();
        order.getOrderedArticles().forEach((articleNumber, quantity) ->
                articlesBuilder.append(articleNumber).append(":").append(quantity).append(","));
        if (!articlesBuilder.isEmpty()) {
            articlesBuilder.setLength(articlesBuilder.length() - 1); // Remove trailing comma
        }
        String sql = "UPDATE orders SET orderedArticles = ?, receiverName = ?, receiverKontoNumber = ?, orderDate = ?, senderName = ?, senderKontoNumber = ?, department = ?, status = ?" +
                "WHERE orderId = ?;";
        boolean result =  databaseManager.executePreparedUpdate(sql, new Object[]{
                articlesBuilder.toString(),
                order.getReceiverName(),
                order.getReceiverKontoNumber(),
                order.getOrderDate(),
                order.getSenderName(),
                order.getSenderKontoNumber(),
                order.getDepartment(),
                order.getStatus(),
                order.getOrderId()
        });
        if(result) {
            Main.logUtils.addLog("Order with id '" + order.getOrderId() + "' updated");
            return true;
        } else {
            logger.error("Error while updating order with id '{}'", order.getOrderId());
            Main.logUtils.addLog("Error while updating order with id '" + order.getOrderId() + "'");
            return false;
        }
    }

    public boolean deleteOrder(String orderId) {
        if (!existsOrder(orderId)) {
            return false;
        }
        String sql = "DELETE FROM orders WHERE orderId = ?;";
        if( databaseManager.executePreparedUpdate(sql, new Object[]{orderId})) {
            Main.logUtils.addLog("Order with id '" + orderId + "' deleted");
            return true;
        } else {
            logger.error("Error while deleting order with id '{}'", orderId);
            Main.logUtils.addLog("Error while deleting order with id '" + orderId + "'");
            return false;
        }
    }

    public Order getOrder(String orderId) {
        String sql = "SELECT * FROM " + DatabaseManager.TABLE_ORDERS + " WHERE orderId = '" + orderId + "';";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            if (resultSet.next()) {
                String orderedArticlesStr = resultSet.getString("orderedArticles");
                var orderedArticles = getOrderedArticles(orderedArticlesStr);
                return new Order(
                        resultSet.getString("orderId"),
                        orderedArticles,
                        resultSet.getString("receiverName"),
                        resultSet.getString("receiverKontoNumber"),
                        resultSet.getString("orderDate"),
                        resultSet.getString("senderName"),
                        resultSet.getString("senderKontoNumber"),
                        resultSet.getString("department"),
                        resultSet.getString("status")
                );
            }
        } catch (Exception e) {
            logger.error("Error while checking if order with id '{}'", orderId, e);
            Main.logUtils.addLog("Error while checking if order with id '" + orderId + "'");
        }
        return null;
    }

    private static HashMap<String, Integer> getOrderedArticles(String orderedArticlesStr) {
        var orderedArticles = new HashMap<String, Integer>();
        if (orderedArticlesStr != null && !orderedArticlesStr.isEmpty()) {
            String[] articlesArray = orderedArticlesStr.split(",");
            for (String articleEntry : articlesArray) {
                String[] parts = articleEntry.split(":");
                if (parts.length == 2) {
                    String articleNumber = parts[0].trim();
                    int quantity = Integer.parseInt(parts[1].trim());
                    orderedArticles.put(articleNumber, quantity);
                }
            }
        }
        return orderedArticles;
    }

    public List<Order> getOrders() {
        String sql = "SELECT * FROM orders;";
        try (var resultSet = databaseManager.executeQuery(sql)) {
            var orders = new ArrayList<Order>();
            while (resultSet.next()) {
                String orderedArticlesStr = resultSet.getString("orderedArticles");
                var orderedArticles = getOrderedArticles(orderedArticlesStr);
                orders.add(new Order(
                        resultSet.getString("orderId"),
                        orderedArticles,
                        resultSet.getString("receiverName"),
                        resultSet.getString("receiverKontoNumber"),
                        resultSet.getString("orderDate"),
                        resultSet.getString("senderName"),
                        resultSet.getString("senderKontoNumber"),
                        resultSet.getString("department"),
                        resultSet.getString("status")
                ));
            }
            return orders;
        } catch (Exception e) {
            logger.error("Error while checking if orders in Database", e);
            Main.logUtils.addLog("Error while checking if orders in Database");
            return new ArrayList<>();
        }
    }

    public List<Article> getOrderArticles(Order order) {
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        List<Article> articles = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : orderedArticles.entrySet()) {
            Article article;
            article = ArticleManager.getInstance().getArticleByName(entry.getKey());
            if(article == null) {
                article = ArticleManager.getInstance().getArticleByNumber(entry.getKey());
            }
            articles.add(article);
        }
        return articles;
    }
}
