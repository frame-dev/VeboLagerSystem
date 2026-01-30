package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnusedReturnValue")
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
                "suppliedArticles TEXT," +
                "minOrderValue DOUBLE" +
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
        String sql = "INSERT INTO vendors (name, contactPerson, phoneNumber, email, address, suppliedArticles, minOrderValue) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?);";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{vendor.getName(), vendor.getContactPerson(),
                vendor.getPhoneNumber(), vendor.getEmail(), vendor.getAddress(),
                String.join(",", vendor.getSuppliedArticles()), vendor.getMinOrderValue()});
        if(result) {
            Main.logUtils.addLog("Inserted new vendor with name '" + vendor.getName() + "'");
        } else {
            Main.logUtils.addLog("Could not insert new vendor with name '" + vendor.getName() + "'");
        }
        return result;
    }

    public boolean updateVendor(Vendor vendor) {
        if (!existsVendor(vendor.getName())) {
            return false;
        }
        String sql = "UPDATE vendors SET contactPerson = ?, phoneNumber = ?, email = ?, address = ?, suppliedArticles = ?, minOrderValue=? " +
                "WHERE name = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{vendor.getContactPerson(),
                vendor.getPhoneNumber(), vendor.getEmail(), vendor.getAddress(),
                String.join(",", vendor.getSuppliedArticles()), vendor.getMinOrderValue(), vendor.getName()});
        if(result) {
            Main.logUtils.addLog("Updated vendor with name '" + vendor.getName() + "'");
        } else {
            Main.logUtils.addLog("Could not update vendor with name '" + vendor.getName() + "'");
        }
        return result;
    }

    public boolean updateVendor(String vendorName, String[] columns, Object[] data) {
        if(!existsVendor(vendorName)) {
            return false;
        }
        if(data.length != columns.length) {
            return false;
        }

        // Build dynamic UPDATE statement
        StringBuilder sql = new StringBuilder("UPDATE vendors SET ");
        for (int i = 0; i < columns.length; i++) {
            sql.append(columns[i]).append(" = ?");
            if (i < columns.length - 1) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE name = ?;");

        // Combine data array with vendorName for WHERE clause
        Object[] params = new Object[data.length + 1];
        System.arraycopy(data, 0, params, 0, data.length);
        params[data.length] = vendorName;

        boolean result = databaseManager.executePreparedUpdate(sql.toString(), params);
        if(result) {
            Main.logUtils.addLog("Updated vendor with name '" + vendorName + "'");
        } else {
            Main.logUtils.addLog("Could not update vendor with name '" + vendorName + "'");
        }
        return result;
    }

    public boolean deleteVendor(String name) {
        if (!existsVendor(name)) {
            return false;
        }
        String sql = "DELETE FROM vendors WHERE name = ?;";
        boolean result = databaseManager.executePreparedUpdate(sql, new Object[]{name});
        if (result) {
            Main.logUtils.addLog("Deleted vendor with name '" + name + "'");
        } else {
            Main.logUtils.addLog("Could not delete vendor with name '" + name + "'");
        }
        return result;
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
                double minOrderValue = resultSet.getDouble("minOrderValue");
                List<String> suppliedArticles = new ArrayList<>();
                if (suppliedArticlesStr != null && !suppliedArticlesStr.isEmpty()) {
                    String[] articlesArray = suppliedArticlesStr.split(",");
                    for (String article : articlesArray) {
                        suppliedArticles.add(article.trim());
                    }
                }
                return new Vendor(name, contactPerson, phoneNumber, email, address, suppliedArticles, minOrderValue);
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
                double minOrderValue = resultSet.getDouble("minOrderValue");
                List<String> suppliedArticles = new ArrayList<>();
                if (suppliedArticlesStr != null && !suppliedArticlesStr.isEmpty()) {
                    String[] articlesArray = suppliedArticlesStr.split(",");
                    for (String article : articlesArray) {
                        suppliedArticles.add(article.trim());
                    }
                }
                vendors.add(new Vendor(name, contactPerson, phoneNumber, email, address, suppliedArticles, minOrderValue));
            }
            return vendors;
        } catch (Exception e) {
            logger.error("Error while getting vendors", e);
            Main.logUtils.addLog("Error while getting vendors");
            return new ArrayList<>();
        }
    }
}
