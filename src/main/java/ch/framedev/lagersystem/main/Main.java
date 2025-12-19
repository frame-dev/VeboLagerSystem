package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.guis.Splashscreen;
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.QRCodeUtils;
import ch.framedev.lagersystem.utils.UserDataDir;

import java.io.File;
import java.util.Arrays;

public class Main {

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

        Splashscreen splashscreen = new Splashscreen();
        splashscreen.showSplash(5000); // Zeige Splashscreen für 3 Sekunden
        System.out.println("Anwendungsdatenverzeichnis: " + getAppDataDir().getAbsolutePath());
        System.out.println(QRCodeUtils.getMapFromJsonQRCode());
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
