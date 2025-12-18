package ch.framedev.lagersystem.main;

import ch.framedev.lagersystem.guis.Splashscreen;
import ch.framedev.lagersystem.scan.ScanServer;
import ch.framedev.lagersystem.utils.UserDataDir;

import java.io.File;

public class Main {

    public static void main(String[] args) throws Exception {
        ScanServer.main(args);

        if(!getAppDataDir().exists()) {
            if(!getAppDataDir().mkdirs()) {
                System.err.println("Konnte Anwendungsdatenverzeichnis nicht erstellen: " + getAppDataDir().getAbsolutePath());
            }
        }

        Splashscreen splashscreen = new Splashscreen();
        splashscreen.showSplash(5000); // Zeige Splashscreen für 3 Sekunden

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
