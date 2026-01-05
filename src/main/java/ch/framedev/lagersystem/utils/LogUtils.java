package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.simplejavautils.SimpleJavaUtils;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.List;

public class LogUtils {

    private List<String> logs;
    private final File LOG_FILE;

    public LogUtils() {
        this.LOG_FILE = new File(Main.getAppDataDir(), "vebo_lager_system.log");
        if(!LOG_FILE.getParentFile().exists()) {
            if(!LOG_FILE.getParentFile().mkdirs()) {
                System.err.println("Konnte Log-Verzeichnis nicht erstellen: " + LOG_FILE.getParentFile().getAbsolutePath());
            }
            if(!LOG_FILE.exists()) {
                try {
                    if(!LOG_FILE.createNewFile()) {
                        System.err.println("Konnte Log-Datei nicht erstellen: " + LOG_FILE.getAbsolutePath());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void addLog(String logEntry) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        logEntry = "[" + dateFormat.format(System.currentTimeMillis()) + "] " + logEntry;
        logs.add(logEntry);
        saveLogsToFile();
    }

    private void saveLogsToFile() {
        try(FileWriter writer = new FileWriter(LOG_FILE, true)) {
            for(String log : logs) {
                writer.write(log + System.lineSeparator());
            }
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
