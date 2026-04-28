package ch.framedev.lagersystem.classes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ch.framedev.lagersystem.main.Main;

public class History {

    private static final List<String> SUPPORTED_DATE_FORMATS = Arrays.asList(
            "dd.MM.yyyy-HH:mm:ss",
            "dd.MM.yyyy HH:mm:ss",
            "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss"
    );

    private String info;
    private String date;

    public History(String info, String date) {
        this.info = info;
        this.date = date;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Date stringToDate() {
        if (date == null || date.trim().isEmpty()) {
            Main.logUtils.addLog("Could not format Date: date is null or empty");
            return null;
        }

        String normalizedDate = date.trim();
        for (String pattern : SUPPORTED_DATE_FORMATS) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
                format.setLenient(false);
                return format.parse(normalizedDate);
            } catch (ParseException ignored) {
                // Try the next supported format.
            }
        }

        Main.logUtils.addLog("Could not format Date: unsupported date format '" + normalizedDate + "'");
        return null;
    }
    
}
