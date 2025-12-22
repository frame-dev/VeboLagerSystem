package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.guis.MainGUI;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.UserDataDir;

import java.io.File;

public class Main {

    public static DatabaseManager databaseManager;

    public static void main(String[] args) throws Exception {
        System.out.println("Starte Vebo Lager System...");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
        System.out.println("OS Name: " + System.getProperty("os.name"));
        System.out.println("OS Version: " + System.getProperty("os.version"));
        ScanServer.main(args);

        if(!getAppDataDir().exists()) {
            if(!getAppDataDir().mkdirs()) {
                System.err.println("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
            }
        }

        databaseManager = new DatabaseManager(getAppDataDir().getAbsolutePath(), "vebo_lager_system.db");
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
}
