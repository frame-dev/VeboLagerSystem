package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.classes.User;
import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    /** Absolute path to the order log file (created on demand). */
    private final Path logFilePath;

    /** Dedicated lock to serialize file writes without blocking other logic. */
    private final Object fileLock = new Object();

    private OrderLoggingUtils() {
        // Build paths using NIO to stay cross-platform and avoid manual separators.
        this.logFilePath = Main.getAppDataDir().toPath()
                .resolve("logs")
                .resolve("bestellung.log");
        ensureLogDirExists();
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
     * Adds a log entry for the given order and user.
     *
     * <p>The entry is appended to {@code bestellung.log} and also forwarded to the in-app log.
     * File IO is synchronized via {@link #fileLock} to keep the critical section small.
     *
     * @param order the processed order (must not be {@code null})
     * @param user  the user who processed the order (must not be {@code null})
     */
    public void addLogEntry(Order order, User user) {
        if (order == null || user == null) {
            LOGGER.warn("addLogEntry called with null order or user");
            return;
        }

        String logEntry = formatEntry(order, user);
        appendLogEntry(logEntry, Level.INFO, "[ORDER LOG|INFO] " + logEntry.trim());
    }

    public void addOrderEvent(String orderId, Level level, String message) {
        String sanitizedMessage = safe(message).trim();
        if (sanitizedMessage.isEmpty()) {
            return;
        }

        Level effectiveLevel = level == null ? Level.INFO : level;
        String logEntry = formatEventEntry(orderId, effectiveLevel, sanitizedMessage);
        StringBuilder appMessage = new StringBuilder("[ORDER LOG|")
                .append(effectiveLevel)
                .append("] ");
        if (orderId != null && !orderId.isBlank()) {
            appMessage.append("[").append(orderId).append("] ");
        }
        appMessage.append(sanitizedMessage);
        appendLogEntry(logEntry, effectiveLevel, appMessage.toString());
    }

    public void addInfo(String orderId, String message) {
        addOrderEvent(orderId, Level.INFO, message);
    }

    public void addWarn(String orderId, String message) {
        addOrderEvent(orderId, Level.WARN, message);
    }

    public void addError(String orderId, String message) {
        addOrderEvent(orderId, Level.ERROR, message);
    }

    /**
     * Builds a single, human-readable log line.
     */
    private String formatEntry(Order order, User user) {
        String dateString = DATE_FORMAT.format(LocalDateTime.now());
        return String.format(
                "Datum: %s, Benutzer '%s' hat eine Bestellung verarbeitet: %s%n",
                dateString,
                safe(user.getName()),
                safe(order.getOrderId())
        );
    }

    private String formatEventEntry(String orderId, Level level, String message) {
        String dateString = DATE_FORMAT.format(LocalDateTime.now());
        StringBuilder builder = new StringBuilder("Datum: ")
                .append(dateString)
                .append(", Level: ")
                .append(level == null ? Level.INFO : level)
                .append(", ");
        if (orderId != null && !orderId.isBlank()) {
            builder.append("Bestellung: ").append(orderId).append(", ");
        }
        builder.append("Ereignis: ").append(message).append(System.lineSeparator());
        return builder.toString();
    }

    /**
     * Ensures the log directory exists. The log file itself is created on demand when writing.
     */
    private void ensureLogDirExists() {
        Path parent = logFilePath.getParent();
        if (parent == null) {
            LOGGER.error("No log directory for path: {}", logFilePath);
            Main.logUtils.addLog("[ORDER LOG|ERROR] No log directory for path: " + logFilePath);
            return;
        }
        try {
            Files.createDirectories(parent);
        } catch (IOException e) {
            LOGGER.error("Could not create log directory: {}", parent, e);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Could not create log directory: " + parent + " (" + e.getMessage() + ")");
        }
    }

    /**
     * Appends a single line to the order log file.
     */
    private void writeLogToFile(String logEntry) {
        try {
            Files.writeString(
                    logFilePath,
                    logEntry,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            LOGGER.error("Failed to write order log entry to: {}", logFilePath, e);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Failed to write order log entry to: " + logFilePath + " (" + e.getMessage() + ")");
        }
    }

    private void appendLogEntry(String logEntry, Level level, String appMessage) {
        synchronized (fileLock) {
            writeLogToFile(logEntry);
        }
        Main.logUtils.addLog(level, appMessage);
    }

    /**
     * Reads all log lines from the order log file.
     *
     * @return immutable list of log lines (empty if missing or unreadable)
     */
    public List<String> getAllLogs() {
        if (!Files.exists(logFilePath)) {
            return List.of();
        }
        try {
            return List.copyOf(Files.readAllLines(logFilePath, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            LOGGER.error("Failed to read order log file: {}", logFilePath, ex);
            Main.logUtils.addLog("[ORDER LOG|ERROR] Failed to read order log file: " + logFilePath + " (" + ex.getMessage() + ")");
            return List.of();
        }
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }
}
