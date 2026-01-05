package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LogUtils {

    private final Logger LOGGER = LogManager.getLogger(LogUtils.class);
    private final File LOG_FILE;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public LogUtils() {
        this.LOG_FILE = new File(Main.getAppDataDir(), "vebo_lager_system.log");
        initializeLogFile();
    }

    private void initializeLogFile() {
        // Ensure parent directory exists
        File parentDir = LOG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                System.err.println("Konnte Log-Verzeichnis nicht erstellen: " + parentDir.getAbsolutePath());
                return;
            }
        }

        // Ensure log file exists
        if (!LOG_FILE.exists()) {
            try {
                if (!LOG_FILE.createNewFile()) {
                    System.err.println("Konnte Log-Datei nicht erstellen: " + LOG_FILE.getAbsolutePath());
                }
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "Fehler beim Erstellen der Log-Datei: {}", LOG_FILE.getAbsolutePath(), e);
            }
        }
    }

    public synchronized void addLog(String logEntry) {
        String timestampedEntry = "[" + dateFormat.format(new Date()) + "] " + logEntry;
        writeLogToFile(timestampedEntry);
        LOGGER.log(Level.INFO, logEntry);
    }

    private void writeLogToFile(String logEntry) {
        try (FileWriter writer = new FileWriter(LOG_FILE, true)) {
            writer.write(logEntry + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            LOGGER.log(Level.ERROR, "Fehler beim Schreiben der Log-Datei: {}", LOG_FILE.getAbsolutePath(), e);
        }
    }
}
