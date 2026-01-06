package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ArticleManager {

    private final Logger logger = LogManager.getLogger(ArticleManager.class);

    private final DatabaseManager databaseManager;
    private static ArticleManager instance;

    private ArticleManager() {
        databaseManager = Main.databaseManager;
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
            Vendor vendor = vendor = new Vendor(article.getVendorName(), "", "", "", "", new ArrayList<>());
            vendor.getSuppliedArticles().add(article.getArticleNumber());
            vendorManager.insertVendor(vendor);
        } else {
            Vendor vendor = vendorManager.getVendorByName(article.getVendorName());
            if (vendor != null && !vendor.getSuppliedArticles().contains(article.getArticleNumber())) {
                vendor.getSuppliedArticles().add(article.getArticleNumber());
                vendorManager.updateVendor(vendor);
            }
        }
        return databaseManager.executePreparedUpdate(sql, new Object[]{article.getArticleNumber(), article.getName(), article.getDetails(),
                article.getStockQuantity(), article.getMinStockLevel(), article.getSellPrice(),
                article.getPurchasePrice(), article.getVendorName()});
    }

    public boolean updateArticle(Article article) {
        if (!existsArticle(article.getArticleNumber())) {
            return false;
        }
        String sql = "UPDATE articles SET name = ?, details = ?, stockQuantity = ?, minStockLevel = ?, sellPrice = ?, purchasePrice = ?, vendorName = ? " +
                "WHERE articleNumber = ?;";
        VendorManager vendorManager = VendorManager.getInstance();
        if (!vendorManager.existsVendor(article.getVendorName())) {
            Vendor vendor = vendor = new Vendor(article.getVendorName(), "", "", "", "", new ArrayList<>());
            vendor.getSuppliedArticles().add(article.getArticleNumber());
            vendorManager.insertVendor(vendor);
        } else {
            Vendor vendor = vendorManager.getVendorByName(article.getVendorName());
            if (vendor != null && !vendor.getSuppliedArticles().contains(article.getArticleNumber())) {
                vendor.getSuppliedArticles().add(article.getArticleNumber());
                vendorManager.updateVendor(vendor);
            }
        }
        return databaseManager.executePreparedUpdate(sql, new Object[]{article.getName(), article.getDetails(),
                article.getStockQuantity(), article.getMinStockLevel(), article.getSellPrice(),
                article.getPurchasePrice(), article.getVendorName(), article.getArticleNumber()});
    }

    public boolean deleteArticleByNumber(String articleNumber) {
        if (!existsArticle(articleNumber)) {
            return false;
        }
        String sql = "DELETE FROM articles WHERE articleNumber = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{articleNumber});
    }

    public boolean deleteArticle(Article article) {
        if (existsArticle(article.getArticleNumber())) {
            return deleteArticleByNumber(article.getArticleNumber());
        }
        return false;
    }

    public Article getArticleByNumber(String articleNumber) {
        String sql = "SELECT * FROM articles WHERE articleNumber = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{articleNumber})) {
            if (resultSet.next()) {
                return new Article(
                        resultSet.getString("articleNumber"),
                        resultSet.getString("name"),
                        resultSet.getString("details"),
                        resultSet.getInt("stockQuantity"),
                        resultSet.getInt("minStockLevel"),
                        resultSet.getDouble("sellPrice"),
                        resultSet.getDouble("purchasePrice"),
                        resultSet.getString("vendorName")
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error while checking if vendor with name '{}'", articleNumber, e);
            return null;
        }
    }

    public Article getArticleByName(String name) {
        String sql = "SELECT * FROM articles WHERE name = ?;";
        try (ResultSet resultSet = databaseManager.executePreparedQuery(sql, new Object[]{name})) {
            if (resultSet.next()) {
                return new Article(
                        resultSet.getString("articleNumber"),
                        resultSet.getString("name"),
                        resultSet.getString("details"),
                        resultSet.getInt("stockQuantity"),
                        resultSet.getInt("minStockLevel"),
                        resultSet.getDouble("sellPrice"),
                        resultSet.getDouble("purchasePrice"),
                        resultSet.getString("vendorName")
                );
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error while checking if vendor with name '{}'", name, e);
            return null;
        }
    }

    public List<Article> getAllArticles() {
        String sql = "SELECT * FROM articles;";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            List<Article> articles = new ArrayList<>();
            while (resultSet.next()) {
                articles.add(new Article(
                        resultSet.getString("articleNumber"),
                        resultSet.getString("name"),
                        resultSet.getString("details"),
                        resultSet.getInt("stockQuantity"),
                        resultSet.getInt("minStockLevel"),
                        resultSet.getDouble("sellPrice"),
                        resultSet.getDouble("purchasePrice"),
                        resultSet.getString("vendorName")
                ));
            }
            return articles;
        } catch (Exception e) {
            logger.error("Error while getting all articles", e);
            return null;
        }
    }

}
