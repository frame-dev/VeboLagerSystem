package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class VendorOrderLogging {

    private static final Logger LOGGER = LogManager.getLogger(VendorOrderLogging.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy | HH:mm:ss", Locale.ROOT);
    private static volatile VendorOrderLogging instance;
    private final Path vendorOrderFilePath;

    private VendorOrderLogging() {
        this.vendorOrderFilePath = new java.io.File(Main.getAppDataDir() + "/logs", "vendorOrder.log").toPath();
        ensureLogFileExists();
    }

    public static VendorOrderLogging getInstance() {
        if (instance == null) {
            synchronized (VendorOrderLogging.class) {
                if (instance == null) {
                    instance = new VendorOrderLogging();
                }
            }
        }
        return instance;
    }

    public synchronized void addLog(String message) {
        String formattedMessage = "[LieferantenLog] | " + DATE_FORMAT.format(LocalDateTime.now()) + " >> " + safe(message);
        save(formattedMessage);
    }

    private void save(String message) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                vendorOrderFilePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(message);
            writer.newLine();
        } catch (IOException ex) {
            LOGGER.error("Fehler beim Schreiben des Lieferanten-Logs: {}", vendorOrderFilePath, ex);
            Main.logUtils.addLog("VendorOrder.log konnte nicht geschrieben werden: " + vendorOrderFilePath);
        }
    }

    public List<String> getLogs() {
        if (!Files.exists(vendorOrderFilePath)) {
            return List.of();
        }
        try {
            return Files.readAllLines(vendorOrderFilePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error("Fehler beim Lesen des Lieferanten-Logs: {}", vendorOrderFilePath, e);
            Main.logUtils.addLog("VendorOrder.log konnte nicht gelesen werden: " + vendorOrderFilePath);
            return List.of();
        }
    }

    private void ensureLogFileExists() {
        Path parent = vendorOrderFilePath.getParent();
        if (parent == null) {
            LOGGER.error("Kein Log-Verzeichnis fuer Pfad: {}", vendorOrderFilePath);
            Main.logUtils.addLog("VendorOrder.log konnte nicht erstellt werden! Grund: Kein Log-Verzeichnis fuer Pfad: " + vendorOrderFilePath);
            return;
        }
        try {
            Files.createDirectories(parent);
            if (!Files.exists(vendorOrderFilePath)) {
                Files.createFile(vendorOrderFilePath);
            }
        } catch (IOException e) {
            LOGGER.error("Konnte VendorOrder.log nicht erstellen: {}", vendorOrderFilePath, e);
            Main.logUtils.addLog("VendorOrder.log konnte nicht erstellt werden! Grund: " + vendorOrderFilePath);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
