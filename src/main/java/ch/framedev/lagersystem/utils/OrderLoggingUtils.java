package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
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

/**
 * The OrderLoggingUtils class provides utility methods for logging order processing activities to a log file. It implements a singleton pattern to ensure that only one instance of the logger exists throughout the application. The class allows adding log entries for processed orders, which include the date and time of processing, the user's name, and the order ID. The log entries are written to a file named "bestellung.log" located in the "logs" directory within the application's data directory. The class also provides a method to retrieve all log entries from the log file. Thread safety is ensured when adding log entries, allowing multiple threads to log simultaneously without conflicts.
 * @author framedev
 */
public class OrderLoggingUtils {

    private static final Logger LOGGER = LogManager.getLogger(OrderLoggingUtils.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private static volatile OrderLoggingUtils instance;
    private final Path logFilePath;

    private OrderLoggingUtils() {
        this.logFilePath = new java.io.File(Main.getAppDataDir() + java.io.File.separator + "logs" + java.io.File.separator + "bestellung.log").toPath();
        ensureLogFileExists();
    }

    public static OrderLoggingUtils getInstance() {
        if (instance == null) {
            synchronized (OrderLoggingUtils.class) {
                if (instance == null) {
                    instance = new OrderLoggingUtils();
                }
            }
        }
        return instance;
    }

    /**
     * Adds a log entry for the given order and user. The log entry includes the current date and time, the user's name, and the order ID. The method is synchronized to ensure thread safety when multiple threads are logging simultaneously. If either the order or user is null, a warning is logged and the method returns without adding a log entry.
     * @param order The order for which the log entry is being added.
     * @param user The user who processed the order, whose name will be included in the log entry.
     */
    public synchronized void addLogEntry(Order order, User user) {
        if (order == null || user == null) {
            LOGGER.warn("addLogEntry called with null order or user");
            return;
        }
        String dateString = DATE_FORMAT.format(LocalDateTime.now());
        String logEntry = String.format("Datum: %s, Benutzer '%s' hat eine Bestellung verarbeitet: %s%n",
                dateString,
                safe(user.getName()),
                safe(order.getOrderId()));
        writeLogToFile(logEntry);
        Main.logUtils.addLog("[ORDER LOG|INFO] " + logEntry.trim());
    }

    private void ensureLogFileExists() {
        Path parent = logFilePath.getParent();
        if (parent == null) {
            LOGGER.error("Kein Log-Verzeichnis fuer Pfad: {}", logFilePath);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler: Kein Log-Verzeichnis fuer Pfad: " + logFilePath);
            return;
        }
        try {
            Files.createDirectories(parent);
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }
        } catch (IOException e) {
            LOGGER.error("Konnte die Log-Datei nicht erstellen: {}", logFilePath, e);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler: Konnte die Log-Datei nicht erstellen: " + logFilePath);
        }
    }

    private void writeLogToFile(String logEntry) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                logFilePath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            writer.write(logEntry);
        } catch (IOException e) {
            LOGGER.error("Fehler beim Schreiben des Log-Eintrags in die Datei: {}", logFilePath, e);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler beim Schreiben des Log-Eintrags in die Datei: " + logFilePath);
        }
    }

    public List<String> getAllLogs() {
        if (!Files.exists(logFilePath)) {
            return List.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(logFilePath, StandardCharsets.UTF_8)) {
            return reader.lines().toList();
        } catch (IOException ex) {
            LOGGER.error("Fehler beim Lesen der Log-Datei: {}", logFilePath, ex);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Fehler beim Lesen der Log-Datei: " + logFilePath);
            return List.of();
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }
}
