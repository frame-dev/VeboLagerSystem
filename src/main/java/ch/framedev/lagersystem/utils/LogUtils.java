package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.spi.StandardLevel;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * The LogUtils class provides utility methods for logging messages to a log file and using Log4j for logging. It ensures that the log file and its parent directories exist, and allows adding log entries with timestamps. The class is designed to be thread-safe, allowing multiple threads to log messages simultaneously without conflicts. It also integrates with a custom LogManager for additional logging functionality.
 * @author framedev
 */
public class LogUtils {

    public static final int DEFAULT_LOG_RETENTION_DAYS = 30;
    public static final String MAIN_LOG_FILE_PREFIX = "vebo_lager_system";
    public static final String MAIN_LOG_FILE_EXTENSION = ".log";

    private static final Logger LOGGER = LogManager.getLogger(LogUtils.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
    private static final ThreadLocal<Boolean> FORWARDING_TO_DB = ThreadLocal.withInitial(() -> false);
    private final Path logDirectoryPath;

    /**
     * Dedicated lock for file I/O to avoid blocking other logging work.
     * Log4j itself is thread-safe; we only need to serialize file appends.
     */
    private final Object fileLock = new Object();

    /**
     * Initializes the LogUtils class by setting up the log file path and ensuring that the log file and its parent directories exist. The log file is located in the "logs" directory within the application's data directory, and is named "vebo_lager_system.log". If the log file or its parent directories do not exist, they will be created. Any errors encountered during this process will be logged using Log4j.
     */
    public LogUtils() {
        this.logDirectoryPath = Main.getAppDataDir().toPath().resolve("logs");
        initializeLogFile();
    }

    public record LogCleanupResult(int deletedFileCount, int failedFileCount) {
    }

    /**
     * Ensures that the log directory exists.
     * The log file itself will be created lazily on first write (CREATE + APPEND).
     */
    private void initializeLogFile() {
        try {
            Files.createDirectories(logDirectoryPath);
        } catch (IOException | SecurityException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Erstellen des Log-Verzeichnisses: {}", logDirectoryPath, e);
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
        if (Main.databaseManager == null) {
            return;
        }
        if (Boolean.TRUE.equals(FORWARDING_TO_DB.get())) {
            return;
        }
        try {
            FORWARDING_TO_DB.set(true);
            ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
            if (!logManager.createLog(mapAppLogLevel(effectiveLevel), msg)) {
                LOGGER.log(Level.ERROR, "LogManager could not create log");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.ERROR, "Fehler beim Weiterleiten an den internen LogManager", ex);
        } finally {
            FORWARDING_TO_DB.remove();
        }
    }

    /**
     * Appends a single line to the configured log file.
     * Uses CREATE + APPEND so the file is created automatically if missing.
     */
    private void writeLogToFile(String logEntry) {
        synchronized (fileLock) {
            Path logFilePath = resolveLogFilePath(LocalDate.now());
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

    public Path getLogDirectoryPath() {
        return logDirectoryPath;
    }

    public static boolean isMainLogFile(Path filePath) {
        if (filePath == null) {
            return false;
        }

        String fileName = filePath.getFileName() == null ? "" : filePath.getFileName().toString();
        return fileName.equals(MAIN_LOG_FILE_PREFIX + MAIN_LOG_FILE_EXTENSION)
                || (fileName.startsWith(MAIN_LOG_FILE_PREFIX + "_") && fileName.endsWith(MAIN_LOG_FILE_EXTENSION));
    }

    private Path resolveLogFilePath(LocalDate date) {
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        return logDirectoryPath.resolve(MAIN_LOG_FILE_PREFIX + "_" + FILE_DATE_FORMAT.format(effectiveDate) + MAIN_LOG_FILE_EXTENSION);
    }

    public LogCleanupResult deleteLogsOlderThan(int retentionDays) {
        if (!Files.isDirectory(logDirectoryPath)) {
            return new LogCleanupResult(0, 0);
        }

        int effectiveRetentionDays = retentionDays > 0 ? retentionDays : DEFAULT_LOG_RETENTION_DAYS;
        Instant cutoff = Instant.now().minus(effectiveRetentionDays, ChronoUnit.DAYS);
        int deletedFiles = 0;
        int failedFiles = 0;

        synchronized (fileLock) {
            try (Stream<Path> fileStream = Files.list(logDirectoryPath)) {
                for (Path filePath : fileStream.filter(Files::isRegularFile).toList()) {
                    FileTime lastModifiedTime;
                    try {
                        lastModifiedTime = Files.getLastModifiedTime(filePath);
                    } catch (IOException | SecurityException e) {
                        failedFiles++;
                        LOGGER.log(Level.WARN, "Konnte Aenderungsdatum der Log-Datei nicht lesen: {}", filePath, e);
                        continue;
                    }

                    if (!lastModifiedTime.toInstant().isBefore(cutoff)) {
                        continue;
                    }

                    try {
                        if (Files.deleteIfExists(filePath)) {
                            deletedFiles++;
                        }
                    } catch (IOException | SecurityException e) {
                        failedFiles++;
                        LOGGER.log(Level.WARN, "Konnte alte Log-Datei nicht loeschen: {}", filePath, e);
                    }
                }
            } catch (IOException | SecurityException e) {
                LOGGER.log(Level.ERROR, "Fehler beim Lesen des Log-Verzeichnisses: {}", logDirectoryPath, e);
                return new LogCleanupResult(0, 1);
            }
        }

        return new LogCleanupResult(deletedFiles, failedFiles);
    }

    private ch.framedev.lagersystem.managers.LogManager.LogLevel mapAppLogLevel(Level level) {
        StandardLevel standardLevel = level == null ? StandardLevel.INFO : level.getStandardLevel();
        return switch (standardLevel) {
            case FATAL, ERROR -> ch.framedev.lagersystem.managers.LogManager.LogLevel.ERROR;
            case WARN -> ch.framedev.lagersystem.managers.LogManager.LogLevel.WARNING;
            case DEBUG -> ch.framedev.lagersystem.managers.LogManager.LogLevel.DEBUG;
            case TRACE -> ch.framedev.lagersystem.managers.LogManager.LogLevel.TRACE;
            default -> ch.framedev.lagersystem.managers.LogManager.LogLevel.INFO;
        };
    }
}
