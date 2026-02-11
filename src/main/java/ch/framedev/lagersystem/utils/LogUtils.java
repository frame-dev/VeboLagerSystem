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

public class LogUtils {

    private static final Logger LOGGER = LogManager.getLogger(LogUtils.class);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss", Locale.ROOT);
    private final Path logFilePath;

    public LogUtils() {
        this.logFilePath = new File(Main.getAppDataDir() + "/logs", "vebo_lager_system.log").toPath();
        initializeLogFile();
    }

    private void initializeLogFile() {
        Path parentDir = logFilePath.getParent();
        if (parentDir == null) {
            LOGGER.log(Level.ERROR, "Kein Log-Verzeichnis fuer Pfad: {}", logFilePath);
            return;
        }
        try {
            Files.createDirectories(parentDir);
            if (!Files.exists(logFilePath)) {
                Files.createFile(logFilePath);
            }
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Erstellen der Log-Datei: {}", logFilePath, e);
        }
    }

    public synchronized void addLog(String logEntry) {
        String timestampedEntry = "[" + DATE_FORMAT.format(LocalDateTime.now()) + "] >> " + logEntry;
        writeLogToFile(timestampedEntry);
        LOGGER.log(Level.INFO, logEntry);
        ch.framedev.lagersystem.managers.LogManager logManager = ch.framedev.lagersystem.managers.LogManager.getInstance();
        if (!logManager.createLog(ch.framedev.lagersystem.managers.LogManager.LogLevel.INFO, logEntry)) {
            LOGGER.log(Level.ERROR, "LogManager could not create log");
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
            writer.newLine();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Schreiben der Log-Datei: {}", logFilePath, e);
        }
    }
}
