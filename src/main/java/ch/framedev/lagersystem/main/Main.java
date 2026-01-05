package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.ImportUtils;
import ch.framedev.lagersystem.utils.LogUtils;
import ch.framedev.lagersystem.utils.UserDataDir;
import ch.framedev.simplejavautils.Settings;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;

public class Main {

    public static DatabaseManager databaseManager;
    public static ArticleListGUI articleListGUI;

    public static LogUtils logUtils = new LogUtils();

    public static String userName = "Unbekannt";

    public static Settings settings;

    public static void main(String[] args) throws Exception {
        System.out.println("Starte Vebo Lager System...");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Version: " + System.getProperty("os.version"));
        ScanServer.main(args);

        if (!getAppDataDir().exists()) {
            if (!getAppDataDir().mkdirs()) {
                System.err.println("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
                logUtils.addLog("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
            }
        }

        loadSettings();

        databaseManager = new DatabaseManager(getAppDataDir().getAbsolutePath(), "vebo_lager_system.db");

        // This runs the first setup of the Program
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
            boolean inserted = ArticleManager.getInstance().insertArticle(article);
            if (inserted) {
                System.out.println("Importierter Artikel: " + name + " (Artikelnummer: " + number + ")");
                logUtils.addLog("Importierter Artikel: " + name + " (Artikelnummer: " + number + ")");
            } else {
                System.out.println("Artikel bereits vorhanden, übersprungen: " + name + " (Artikelnummer: " + number + ")");
            }
        }
        String input = JOptionPane.showInputDialog("Bitte geben Sie Ihren Benutzernamen ein:");
        if (input != null && !input.trim().isEmpty()) {
            userName = input.trim();
        } else {
            JOptionPane.showMessageDialog(null, "Benutzername ungültig, Standardname 'Unbekannt' wird verwendet.", "Warnung", JOptionPane.WARNING_MESSAGE);
            userName = "Unbekannt";
        }
        System.out.println("Benutzername gesetzt auf: " + userName);
        MainGUI mainGUI = new MainGUI();
        mainGUI.display();
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
