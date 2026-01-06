package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.classes.Vendor;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.managers.*;
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.simplejavautils.Settings;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {

    public static DatabaseManager databaseManager;
    public static ArticleListGUI articleListGUI;

    public static LogUtils logUtils = new LogUtils();

    public static Settings settings;

    public static final String VERSION = "0.1-TESTING";

    public static void main(String[] args) throws Exception {
        // Replace with Splash Screen
        loadSettings();
        System.out.println("Starte Vebo Lager System...");
        System.out.println("Version: " + VERSION);
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Version: " + System.getProperty("os.version"));

        if (!getAppDataDir().exists()) {
            if (!getAppDataDir().mkdirs()) {
                System.err.println("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
                logUtils.addLog("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
            }
        }


        databaseManager = new DatabaseManager(getAppDataDir().getAbsolutePath(), "vebo_lager_system.db");

        // This runs the first setup of the Program
        ArticleManager articleManager = ArticleManager.getInstance();
        List<Map<String, Object>> data = ImportUtils.getInstance().loadInventoryFile();
        System.out.println("Importiere " + data.size() + " Artikel aus der Inventar Datei...");
        for (Map<String, Object> itemData : data) {
            String number = (String) itemData.get("number");
            String name = (String) itemData.get("name");
            String details = (String) itemData.get("details");
            int stockQuantity = (int) itemData.get("stockQuantity");
            int minStockLevel = (int) itemData.get("minStockLevel");
            double sellPrice = (Double) itemData.get("sellPrice");
            double buyPrice = (Double) itemData.get("buyPrice");
            String vendorName = (String) itemData.get("vendorName");
            Article article = new Article(number, name, details, stockQuantity, minStockLevel, sellPrice, buyPrice,
                    vendorName);
            boolean inserted = articleManager.insertArticle(article);
            if (inserted) {
                System.out.println("Importierter Artikel: " + name + " (Artikelnummer: " + number + ")");
                logUtils.addLog("Importierter Artikel: " + name + " (Artikelnummer: " + number + ")");
            } else {
                System.out.println("Artikel bereits vorhanden, übersprungen: " + name + " (Artikelnummer: " + number + ")");
            }
        }

        VendorManager vendorManager = VendorManager.getInstance();
        List<Map<String, Object>> vendorData = ImportUtils.getInstance().loadVendorList();
        for (Map<String, Object> itemData : vendorData) {
            String vendorName = (String) itemData.get("name");
            String contactPerson = (String) itemData.get("contactPerson");
            String phoneNumber = (String) itemData.get("phoneNumber");
            String email = (String) itemData.get("email");
            String address = (String) itemData.get("address");
            String[] columns = {"contactPerson", "phoneNumber", "email", "address"};
            Object[] dataValues = {contactPerson, phoneNumber, email, address};
            boolean inserted = vendorManager.updateVendor(vendorName, columns, dataValues);
            if (!inserted) {
                System.out.println("Fehler beim Einfügen des Lieferanten: " + vendorName);
                logUtils.addLog("Fehler beim Einfügen des Lieferanten: " + vendorName);
            } else {
                System.out.println("Importierter Lieferant: " + vendorName);
                logUtils.addLog("Importierter Lieferant: " + vendorName);
            }
        }

        DepartmentManager departmentManager = DepartmentManager.getInstance();
        List<Map<String,Object>> departmentData = ImportUtils.getInstance().loadDepartmentsList();
        for(Map<String,Object> itemData : departmentData) {
            String departmentName = (String) itemData.get("department");
            String kontoNumber = (String) itemData.get("kontoNumber");
            if(!departmentManager.existsDepartment(departmentName)) {
                if(!departmentManager.insertDepartment(departmentName, kontoNumber)) {
                    System.out.println("Fehler beim Einfügen der Abteilung: " + departmentName);
                    logUtils.addLog("Fehler beim Einfügen der Abteilung: " + departmentName);
                    continue;
                }
                System.out.println("Importierte Abteilung: " + departmentName);
                logUtils.addLog("Importierte Abteilung: " + departmentName);
            }
        }


        User user = new User("marc", new ArrayList<>());
        UserManager userManager = UserManager.getInstance();
        if(!userManager.existsUser(user.getName()))
            userManager.insertUser(user);
        MainGUI mainGUI = new MainGUI();
        mainGUI.display();

        System.out.println(QRCodeUtils.retrieveQrCodeDataFromWebsite());
    }

    public static File getAppDataDir() {
        try {
            return UserDataDir.getAppPath("VeboLagerSystem").toFile();
        } catch (Exception e) {
            System.err.println("Konnte Anwendungsdatenverzeichnis nicht ermitteln, verwende aktuelles Verzeichnis.");
            return new File(".");
        }
    }

    private static void loadSettings() {
        File file = new File(getAppDataDir(), "settings.properties");
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    System.err.println("Konnte Einstellungsdatei nicht erstellen: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Erstellen der Einstellungsdatei: " + e.getMessage());
            }
        }
        settings = new Settings("settings.properties", Main.class, file);
    }
}
