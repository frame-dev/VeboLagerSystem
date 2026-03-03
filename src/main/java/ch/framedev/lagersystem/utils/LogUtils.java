package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The LogUtils class provides utility methods for logging messages to a log file and using Log4j for logging. It ensures that the log file and its parent directories exist, and allows adding log entries with timestamps. The class is designed to be thread-safe, allowing multiple threads to log messages simultaneously without conflicts. It also integrates with a custom LogManager for additional logging functionality.
 * @author framedev
 */
public class LogUtils {

    private static final Logger LOGGER = LogManager.getLogger(LogUtils.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private final Path logFilePath;

    /**
     * Dedicated lock for file I/O to avoid blocking other logging work.
     * Log4j itself is thread-safe; we only need to serialize file appends.
     */
    private final Object fileLock = new Object();

    /**
     * Initializes the LogUtils class by setting up the log file path and ensuring that the log file and its parent directories exist. The log file is located in the "logs" directory within the application's data directory, and is named "vebo_lager_system.log". If the log file or its parent directories do not exist, they will be created. Any errors encountered during this process will be logged using Log4j.
     */
    public LogUtils() {
        this.logFilePath = new File(Main.getAppDataDir() + "/logs", "vebo_lager_system.log").toPath();
        initializeLogFile();
    }

    /**
     * Ensures that the log directory exists.
     * The log file itself will be created lazily on first write (CREATE + APPEND).
     */
    private void initializeLogFile() {
        Path parentDir = logFilePath.getParent();
        if (parentDir == null) {
            LOGGER.log(Level.ERROR, "Kein Log-Verzeichnis fuer Pfad: {}", logFilePath);
            return;
        }
        try {
            Files.createDirectories(parentDir);
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Erstellen des Log-Verzeichnisses: {}", parentDir, e);
        }
    }

    /**
     * Convenience method that logs at INFO level.
     *
     * @param logEntry message to log (nullable; null becomes an empty string)
     */
    public void addLog(String logEntry) {
        addLog(Level.INFO, logEntry);
    }

    /**
     * Adds a log entry to the log file and logs it using Log4j.
     * The log entry is timestamped with the current date and time.
     *
     * <p>Thread-safety: file appends are serialized via {@link #fileLock}.
     * Log4j is thread-safe and does not need external synchronization.</p>
     *
     * @param level    Log4j level to use
     * @param logEntry message to log (nullable; null becomes an empty string)
     */
    public void addLog(Level level, String logEntry) {
        String msg = (logEntry == null) ? "" : logEntry;
        Level effectiveLevel = (level == null) ? Level.INFO : level;

        String timestampedEntry = "[" + DATE_FORMAT.format(LocalDateTime.now()) + "] >> " + msg;
        writeLogToFile(timestampedEntry);
        LOGGER.log(effectiveLevel, msg);

        // Forward to the application's own LogManager (if available)
        try {
            ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
            if (!logManager.createLog(ch.framedev.lagersystem.managers.LogManager.LogLevel.INFO, msg)) {
                LOGGER.log(Level.ERROR, "LogManager could not create log");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.ERROR, "Fehler beim Weiterleiten an den internen LogManager", ex);
        }
    }

    /**
     * Appends a single line to the configured log file.
     * Uses CREATE + APPEND so the file is created automatically if missing.
     */
    private void writeLogToFile(String logEntry) {
        synchronized (fileLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    logFilePath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            )) {
                writer.write(logEntry);
                writer.newLine();
            } catch (IOException | SecurityException e) {
                LOGGER.log(Level.ERROR, "Fehler beim Schreiben der Log-Datei: {}", logFilePath, e);
            }
        }
    }
}
