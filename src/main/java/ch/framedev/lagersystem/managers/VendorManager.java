package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class VendorManager {

    private final Logger logger = LogManager.getLogger(VendorManager.class);

    private static VendorManager instance;
    private final DatabaseManager databaseManager;

    private VendorManager() {
        databaseManager = Main.databaseManager;
        createTable();
    }

    public static VendorManager getInstance() {
        if (instance == null) {
            instance = new VendorManager();
        }
        return instance;
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS vendors (" +
                "name TEXT," +
                "contactPerson TEXT," +
                "phoneNumber TEXT," +
                "email TEXT," +
                "address TEXT," +
                "suppliedArticles TEXT" +
                ");";
        databaseManager.executeUpdate(sql);
    }

    public boolean existsVendor(String name) {
        String sql = "SELECT * FROM vendors WHERE name = '" + name + "';";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            return resultSet.next();
        } catch (Exception e) {
            logger.error("Error while checking if vendor with name '{}'", name, e);
            return false;
        }
    }

    public boolean insertVendor(Vendor vendor) {
        if (existsVendor(vendor.getName())) {
            return false;
        }
        String sql = "INSERT INTO vendors (name, contactPerson, phoneNumber, email, address, suppliedArticles) " +
                "VALUES (?, ?, ?, ?, ?, ?);";
        return databaseManager.executePreparedUpdate(sql, new Object[]{vendor.getName(), vendor.getContactPerson(),
                vendor.getPhoneNumber(), vendor.getEmail(), vendor.getAddress(),
                String.join(",", vendor.getSuppliedArticles())});
    }

    public boolean updateVendor(Vendor vendor) {
        if (!existsVendor(vendor.getName())) {
            return false;
        }
        String sql = "UPDATE vendors SET contactPerson = ?, phoneNumber = ?, email = ?, address = ?, suppliedArticles = ? " +
                "WHERE name = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{vendor.getContactPerson(),
                vendor.getPhoneNumber(), vendor.getEmail(), vendor.getAddress(),
                String.join(",", vendor.getSuppliedArticles()), vendor.getName()});
    }

    public boolean deleteVendor(String name) {
        if (!existsVendor(name)) {
            return false;
        }
        String sql = "DELETE FROM vendors WHERE name = ?;";
        return databaseManager.executePreparedUpdate(sql, new Object[]{name});
    }

    public Vendor getVendorByName(String name) {
        String sql = "SELECT * FROM vendors WHERE name = '" + name + "';";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            if (resultSet.next()) {
                String contactPerson = resultSet.getString("contactPerson");
                String phoneNumber = resultSet.getString("phoneNumber");
                String email = resultSet.getString("email");
                String address = resultSet.getString("address");
                String suppliedArticlesStr = resultSet.getString("suppliedArticles");
                List<String> suppliedArticles = new ArrayList<>();
                if (suppliedArticlesStr != null && !suppliedArticlesStr.isEmpty()) {
                    String[] articlesArray = suppliedArticlesStr.split(",");
                    for (String article : articlesArray) {
                        suppliedArticles.add(article.trim());
                    }
                }
                return new Vendor(name, contactPerson, phoneNumber, email, address, suppliedArticles);
            } else {
                return null;
            }
        } catch (Exception e) {
            logger.error("Error getting vendor", e);
            return null;
        }
    }

    public List<Vendor> getVendors() {
        String sql = "SELECT * FROM vendors;";
        try (ResultSet resultSet = databaseManager.executeQuery(sql)) {
            List<Vendor> vendors = new ArrayList<>();
            while (resultSet.next()) {
                String name = resultSet.getString("name");
                String contactPerson = resultSet.getString("contactPerson");
                String phoneNumber = resultSet.getString("phoneNumber");
                String email = resultSet.getString("email");
                String address = resultSet.getString("address");
                String suppliedArticlesStr = resultSet.getString("suppliedArticles");
                List<String> suppliedArticles = new ArrayList<>();
                if (suppliedArticlesStr != null && !suppliedArticlesStr.isEmpty()) {
                    String[] articlesArray = suppliedArticlesStr.split(",");
                    for (String article : articlesArray) {
                        suppliedArticles.add(article.trim());
                    }
                }
                vendors.add(new Vendor(name, contactPerson, phoneNumber, email, address, suppliedArticles));
            }
            return vendors;
        } catch (Exception e) {
            logger.error("Error while getting vendors", e);
            return new ArrayList<>();
        }
    }
}
