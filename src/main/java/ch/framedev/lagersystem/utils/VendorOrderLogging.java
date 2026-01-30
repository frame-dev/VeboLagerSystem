package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class VendorOrderLogging {

    private final File vendorOrderFile;
    private static VendorOrderLogging instance;

    private VendorOrderLogging() {
        this.vendorOrderFile = new File(Main.getAppDataDir() + "/logs", "vendorOrder.log");
        if(!vendorOrderFile.exists()) {
            if(!vendorOrderFile.getParentFile().exists()) {
                if(!vendorOrderFile.getParentFile().mkdirs())
                    Main.logUtils.addLog("VendorOrder.log konnte nicht erstellt werden! Grund: Verzeichnis konnte nicht erstellt werden: " + vendorOrderFile.getParentFile().getAbsolutePath());
            }
        }
    }

    public static VendorOrderLogging getInstance() {
        if(instance == null) {
            instance = new VendorOrderLogging();
        }
        return instance;
    }

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy | HH:mm:ss");
    public void addLog(String message) {
        String formattedMessage = "[LieferantenLog] | " + dateFormat.format(new Date()) + " >> " + message;
        save(formattedMessage);
    }

    private void save(String message) {
        try (FileWriter writer = new FileWriter(vendorOrderFile)) {
            writer.write(message + System.lineSeparator());
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public List<String> getLogs() {
        if(!vendorOrderFile.exists()) {
            return List.of();
        }
        try {
            return Files.readAllLines(vendorOrderFile.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
