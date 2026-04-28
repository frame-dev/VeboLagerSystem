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
    private String articleNumber;
    private String userName;
    private Integer oldStock;
    private Integer newStock;
    private Integer changeAmount;
    private String action;

    public History(String info, String date) {
        this.info = info;
        this.date = date;
    }

    public History(String info, String date, String articleNumber, String userName,
                   Integer oldStock, Integer newStock, Integer changeAmount, String action) {
        this.info = info;
        this.date = date;
        this.articleNumber = articleNumber;
        this.userName = userName;
        this.oldStock = oldStock;
        this.newStock = newStock;
        this.changeAmount = changeAmount;
        this.action = action;
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

    public String getArticleNumber() {
        return articleNumber;
    }

    public void setArticleNumber(String articleNumber) {
        this.articleNumber = articleNumber;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getOldStock() {
        return oldStock;
    }

    public void setOldStock(Integer oldStock) {
        this.oldStock = oldStock;
    }

    public Integer getNewStock() {
        return newStock;
    }

    public void setNewStock(Integer newStock) {
        this.newStock = newStock;
    }

    public Integer getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Integer changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
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
