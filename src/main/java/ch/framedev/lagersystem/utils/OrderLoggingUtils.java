package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class OrderLoggingUtils {

    private final Logger logger = LogManager.getLogger(OrderLoggingUtils.class);

    private final File logFile;
    private static OrderLoggingUtils instance;

    private OrderLoggingUtils() {
        instance = this;
        logFile = new File(Main.getAppDataDir() + File.separator + "logs" + File.separator + "bestellung.log");
        if (!logFile.getParentFile().exists()) {
            if(!logFile.getParentFile().mkdirs()) {
                logger.error("Konnte das Verzeichnis für die Log-Datei nicht erstellen: {}", logFile.getParentFile().getAbsolutePath());
                Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler: Konnte das Verzeichnis für die Log-Datei nicht erstellen: " + logFile.getParentFile().getAbsolutePath());
            }
        }
    }

    public static OrderLoggingUtils getInstance() {
        if (instance == null) {
            instance = new OrderLoggingUtils();
        }
        return instance;
    }

    public void addLogEntry(Order order, User user) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String dateString = dateFormat.format(new Date());
        String logEntry = String.format("Datum: %s, Benutzer '%s' hat eine Bestellung verarbeitet: %s%n",
                dateString,
                user.getName(),
                order.getOrderId());
        writeLogToFile(logEntry);
        Main.logUtils.addLog("[ORDER LOG|INFO] " + logEntry.trim());
    }

    private void writeLogToFile(String logEntry) {
        try (var writer = new FileWriter(logFile, true)) {
            writer.write(logEntry);
            writer.flush();
        } catch (Exception e) {
            logger.error("Fehler beim Schreiben des Log-Eintrags in die Datei: {}", logFile.getAbsolutePath(), e);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler beim Schreiben des Log-Eintrags in die Datei: " + logFile.getAbsolutePath());
        }
    }

    public List<String> getAllLogs() {
        if(!logFile.exists()) {
            return List.of();
        }
        try(BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            return reader.lines().toList();
        } catch (Exception ex) {
            logger.error("Fehler beim Lesen der Log-Datei: {}", logFile.getAbsolutePath(), ex);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler beim Lesen der Log-Datei: " + logFile.getAbsolutePath());
            return List.of();
        }
    }
}
