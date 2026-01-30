package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for QR code operations: generation, retrieval, and parsing.
 */
@SuppressWarnings("unused")
public class QRCodeUtils {

    private static final Logger logger = LogManager.getLogger(QRCodeUtils.class);
    private static final File STORE = new File(Main.getAppDataDir(), "scans.json"); // eine Zeile = ein JSON

    /**
     * Generates QR code images for a list of articles.
     * Each QR code encodes a URL with the article's QR data.
     *
     * @param articles List of Article objects
     * @return List of generated QR code image files
     */
    public static List<File> createQrCodes(List<Article> articles) {
        File directory = new File(Main.getAppDataDir(), "qr_codes");
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                logger.error("Could not create directory for QR codes: {}", directory.getAbsolutePath());
                Main.logUtils.addLog("Konnte Verzeichnis für QR-Codes nicht erstellen: " + directory.getAbsolutePath());
            }
        }
        List<File> qrCodeFiles = new ArrayList<>();
        for (Article article : articles) {
            String data = article.getQrCodeData();
            // Encode data for URL safety
            String encodedData;
            try {
                // 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
                encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("Error encoding QR data for article: {}", article.getArticleNumber(), ex);
                Main.logUtils.addLog("Fehler beim Kodieren der QR-Daten für Artikel: " + article.getArticleNumber() + " - " + ex.getMessage());
                continue;
            }
            String url = "https://framedev.ch/vebo/scan.php?data=" + encodedData;
            try {
                File qrCodeFile = QRCodeGenerator.generateQRCodeImage(url, 300, 300, Main.getAppDataDir() + "/qr_codes/" + article.getArticleNumber() + "_qrcode.png");
                qrCodeFiles.add(qrCodeFile);
            } catch (Exception e) {
                logger.error("Error generating QR code for article: {}", article.getArticleNumber(), e);
                Main.logUtils.addLog("Fehler beim Generieren des QR-Codes für Artikel: " + article.getArticleNumber() + " - " + e.getMessage());
            }
        }
        return qrCodeFiles;
    }

    /**
     * Retrieves QR code scan data from the remote website as a list of maps.
     *
     * @return List of maps representing QR code scan data
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> retrieveQrCodeDataFromWebsite() {
        String urlString = "https://framedev.ch/vebo/scans.json";
        Gson gson = new Gson();
        List<Map<String, Object>> mapList = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            URL url = new URI(urlString).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 seconds timeout
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "VeboLagerSystem/1.0");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonArray yamlData = gson.fromJson(reader, JsonArray.class);
                    if (yamlData != null) {
                        for (JsonElement e : yamlData) {
                            JsonObject obj = e.getAsJsonObject();
                            Map<String, Object> map = gson.fromJson(gson.toJson(obj), Map.class);
                            mapList.add(map);
                        }
                    }
                }
            } else {
                logger.error("Failed to fetch QR code data. HTTP response code: {}", responseCode);
                Main.logUtils.addLog("Fehler beim Abrufen der QR-Code-Daten. HTTP-Antwortcode: " + responseCode);
            }
        } catch (IOException e) {
            logger.error("Error while fetching QR code data from website", e);
            Main.logUtils.addLog("Fehler beim Abrufen der QR-Code-Daten von der Webseite: " + e.getMessage());
        } catch (URISyntaxException e) {
            Main.logUtils.addLog("Ungültige URL-Syntax: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return mapList;
    }

    /**
     * Reads all QR code data strings from the local scans.json file.
     *
     * @return List of QR code data strings
     */
    public static List<String> getDataFromQRCode() {
        Gson gson = new Gson();
        List<String> dataList = new ArrayList<>();
        try {
            JsonArray yamlData = gson.fromJson(new FileReader(STORE), JsonArray.class);
            for (JsonElement e : yamlData) {
                JsonObject obj = e.getAsJsonObject();
                if (obj.has("data")) {
                    String data = obj.get("data").getAsString();
                    dataList.add(data);
                }
            }
        } catch (FileNotFoundException e) {
            Main.logUtils.addLog("Fehler beim Lesen der gespeicherten QR-Codes: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return dataList;
    }

    /**
     * Reads all QR code JSON objects as strings from the local scans.json file.
     *
     * @return List of QR code JSON strings
     */
    public static List<String> getQRCodes() {
        Gson gson = new Gson();
        List<String> qrCodeList = new ArrayList<>();
        try {
            JsonArray yamlData = gson.fromJson(new FileReader(STORE), JsonArray.class);
            for (JsonElement e : yamlData) {
                JsonObject obj = e.getAsJsonObject();
                qrCodeList.add(gson.toJson(obj));
            }
        } catch (FileNotFoundException e) {
            Main.logUtils.addLog("Fehler beim Lesen der gespeicherten QR-Codes: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return qrCodeList;
    }

    /**
     * Gets the latest QR code data string from the local scans.json file.
     *
     * @return Latest QR code data string, or null if not found
     */
    public static String getLatestQRCodeData() {
        Gson gson = new Gson();
        try {
            JsonArray yamlData = gson.fromJson(new FileReader(STORE), JsonArray.class);
            if (!yamlData.isEmpty()) {
                JsonObject obj = yamlData.get(yamlData.size() - 1).getAsJsonObject();
                if (obj.has("data")) {
                    return obj.get("data").getAsString();
                }
            }
        } catch (FileNotFoundException e) {
            Main.logUtils.addLog("Fehler beim Lesen der gespeicherten QR-Codes: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Splits a QR code data string into its parts using ';' as separator.
     *
     * @param data QR code data string
     * @return Array of parts
     */
    public static String[] getPartsFromData(String data) {
        if (data == null) return new String[0];
        return data.split(";");
    }

    /**
     * Reads all QR code JSON objects as maps from the local scans.json file.
     *
     * @return List of maps representing QR code data
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, String>> getListMapFromJsonQRCode() {
        Gson gson = new Gson();
        List<Map<String, String>> mapList = new ArrayList<>();
        try {
            JsonArray yamlData = gson.fromJson(new FileReader(STORE), JsonArray.class);
            for (JsonElement e : yamlData) {
                JsonObject obj = e.getAsJsonObject();
                Map<String, String> map = gson.fromJson(gson.toJson(obj), Map.class);
                mapList.add(map);
            }
        } catch (FileNotFoundException e) {
            Main.logUtils.addLog("Fehler beim Lesen der gespeicherten QR-Codes: " + e.getMessage());
            throw new RuntimeException(e);
        }
        return mapList;
    }

    /**
     * Deletes the local scans.json file containing stored QR codes.
     */
    public static void clearStoredQRCodes() {
        if (STORE.exists()) {
            if(!STORE.delete()) {
                System.err.println("Konnte gespeicherte QR-Codes nicht löschen: " + STORE.getAbsolutePath());
                Main.logUtils.addLog("Konnte gespeicherte QR-Codes nicht löschen: " + STORE.getAbsolutePath());
            }
        }
    }
}
