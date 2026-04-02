package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.main.Main;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import com.google.zxing.WriterException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for QR code operations: generation, retrieval, and parsing.
 */
@SuppressWarnings("unused")
public class QRCodeUtils {

    private static final Logger logger = LogManager.getLogger(QRCodeUtils.class);
    private static final String DEFAULT_SCAN_SUBMIT_URL = "https://framedev.ch/vebo/scan.php";
    private static final String DEFAULT_SCAN_LIST_URL = "https://framedev.ch/vebo/scans.json";

    /** Shared Gson instance (thread-safe for read-only use). */
    private static final Gson GSON = new Gson();

    /** Local store for QR scan data (JSON array stored in scans.json). */
    private static final File STORE = Variables.STORE;

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
                Main.logUtils.addLog(Level.ERROR, "Konnte Verzeichnis für QR-Codes nicht erstellen: " + directory.getAbsolutePath());
            }
        }
        List<File> qrCodeFiles = new ArrayList<>();
        for (Article article : articles) {
            String data = article.getQrCodeData();
            if (data == null || data.isBlank()) {
                logger.warn("Skipping QR code generation for article {} because QR data is empty", article.getArticleNumber());
                Main.logUtils.addLog(Level.WARN, "QR-Daten für Artikel " + article.getArticleNumber() + " sind leer. QR-Code wird übersprungen.");
                continue;
            }
            // Encode data for URL safety
            String encodedData;
            try {
                encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
            } catch (RuntimeException ex) {
                logger.error("Error encoding QR data for article: {}", article.getArticleNumber(), ex);
                Main.logUtils.addLog(Level.ERROR, "Fehler beim Kodieren der QR-Daten für Artikel: " + article.getArticleNumber() + " - " + ex.getMessage());
                continue;
            }
            String url = resolveScanSubmitUrl(Main.settings.getProperty("server_url")) + "?data=" + encodedData;
            try {
                File qrCodeFile = QRCodeGenerator.generateQRCodeImage(url, 300, 300, Main.getAppDataDir() + "/qr_codes/" + article.getArticleNumber() + "_qrcode.png");
                qrCodeFiles.add(qrCodeFile);
            } catch (WriterException | IOException e) {
                logger.error("Error generating QR code for article: {}", article.getArticleNumber(), e);
                Main.logUtils.addLog(Level.ERROR, "Fehler beim Generieren des QR-Codes für Artikel: " + article.getArticleNumber() + " - " + e.getMessage());
            }
        }
        return qrCodeFiles;
    }

    /**
     * Ensures the local scans.json file exists.
     * If it doesn't exist, it will be created as an empty JSON array.
     */
    private static void ensureStoreExists() {
        try {
            File parent = STORE.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            if (!STORE.exists()) {
                Files.writeString(STORE.toPath(), "[]", StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("Could not ensure QR store exists at {}: {}", STORE.getAbsolutePath(), e.getMessage());
            Main.logUtils.addLog(Level.ERROR, "Konnte QR-Store nicht initialisieren: " + e.getMessage());
        }
    }

    /**
     * Reads the local store as a JSON array. If the file is missing or invalid, an empty array is returned.
     */
    private static JsonArray readStoreArray() {
        ensureStoreExists();
        try (Reader reader = new FileReader(STORE, StandardCharsets.UTF_8)) {
            JsonArray arr = GSON.fromJson(reader, JsonArray.class);
            return arr != null ? arr : new JsonArray();
        } catch (IOException e) {
            logger.warn("Failed to read QR store ({}): {}", STORE.getAbsolutePath(), e.getMessage());
            Main.logUtils.addLog(Level.ERROR, "Fehler beim Lesen der gespeicherten QR-Codes: " + e.getMessage());
            return new JsonArray();
        }
    }

    /**
     * Retrieves QR code scan data from the remote website as a list of maps.
     *
     * @return List of maps representing QR code scan data
     */
    public static List<Map<String, Object>> retrieveQrCodeDataFromWebsite() {
        String urlString = resolveScanListUrl(Main.settings.getProperty("server_url"));

        List<Map<String, Object>> mapList = new ArrayList<>();
        HttpURLConnection connection = null;
        try {
            connection = getConnection(urlString);
            getResponseCode(mapList, connection);
        } catch (IOException e) {
            logger.error("Error while fetching QR code data from website", e);
            Main.logUtils.addLog(Level.ERROR, "Fehler beim Abrufen der QR-Code-Daten von der Webseite: " + e.getMessage());
        } catch (URISyntaxException e) {
            logger.error("Invalid server URL syntax: {}", urlString, e);
            Main.logUtils.addLog(Level.ERROR, "Ungültige URL-Syntax: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return mapList;
    }

    @SuppressWarnings("unchecked")
    private static void getResponseCode(List<Map<String, Object>> mapList, HttpURLConnection connection)
            throws IOException {
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                JsonArray jsonArray = GSON.fromJson(reader, JsonArray.class);
                if (jsonArray != null) {
                    for (JsonElement e : jsonArray) {
                        JsonObject obj = e.getAsJsonObject();
                        Map<String, Object> map = GSON.fromJson(GSON.toJson(obj), Map.class);
                        mapList.add(map);
                        Main.logUtils.addLog(Level.INFO, "QR-Code-Daten von der Webseite abgerufen: " + map.toString());
                    }
                }
            }
        } else {
            logger.error("Failed to fetch QR code data. HTTP response code: {}", responseCode);
            Main.logUtils.addLog(Level.ERROR, "Fehler beim Abrufen der QR-Code-Daten. HTTP-Antwortcode: " + responseCode);
        }
    }

    private static HttpURLConnection getConnection(String urlString)
            throws MalformedURLException, URISyntaxException, IOException, ProtocolException {
        HttpURLConnection connection;
        URL url = new URI(urlString).toURL();
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000); // 10 seconds timeout
        connection.setReadTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "VeboLagerSystem/1.0");
        return connection;
    }

    public static String resolveScanSubmitUrl(String serverUrlSetting) {
        String normalized = normalizeServerUrl(serverUrlSetting);
        if (normalized == null) {
            return DEFAULT_SCAN_SUBMIT_URL;
        }
        return replaceScanEndpoint(normalized, "/scan.php");
    }

    public static String resolveScanListUrl(String serverUrlSetting) {
        String normalized = normalizeServerUrl(serverUrlSetting);
        if (normalized == null) {
            return DEFAULT_SCAN_LIST_URL;
        }
        return replaceScanEndpoint(normalized, "/scans.json");
    }

    private static String normalizeServerUrl(String serverUrlSetting) {
        if (serverUrlSetting == null) {
            return null;
        }

        String normalized = serverUrlSetting.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private static String replaceScanEndpoint(String normalizedUrl, String targetEndpoint) {
        String baseUrl = normalizedUrl;
        for (String endpoint : List.of("/scan.php", "/scan", "/scans.json", "/list", "/latest", "/latest.json")) {
            if (baseUrl.endsWith(endpoint)) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - endpoint.length());
                break;
            }
        }
        return baseUrl + targetEndpoint;
    }

    /**
     * Reads all QR code data strings from the local scans.json file.
     *
     * @return List of QR code data strings
     */
    public static List<String> getDataFromQRCode() {
        List<String> dataList = new ArrayList<>();
        JsonArray jsonArray = readStoreArray();
        for (JsonElement e : jsonArray) {
            if (e == null || !e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            if (obj.has("data") && !obj.get("data").isJsonNull()) {
                dataList.add(obj.get("data").getAsString());
            } else {
                Main.logUtils.addLog(Level.ERROR, "QR-Code hat keinen (data) Eintrag.");
            }
        }
        return dataList;
    }

    /**
     * Reads all QR code JSON objects as strings from the local scans.json file.
     *
     * @return List of QR code JSON strings
     */
    public static List<String> getQRCodes() {
        List<String> qrCodeList = new ArrayList<>();
        JsonArray jsonArray = readStoreArray();
        for (JsonElement e : jsonArray) {
            if (e == null || !e.isJsonObject()) continue;
            qrCodeList.add(GSON.toJson(e.getAsJsonObject()));
        }
        return qrCodeList;
    }

    /**
     * Gets the latest QR code data string from the local scans.json file.
     *
     * @return Latest QR code data string, or null if not found
     */
    public static String getLatestQRCodeData() {
        JsonArray jsonArray = readStoreArray();
        if (!jsonArray.isEmpty()) {
            JsonElement last = jsonArray.get(jsonArray.size() - 1);
            if (last != null && last.isJsonObject()) {
                JsonObject obj = last.getAsJsonObject();
                if (obj.has("data") && !obj.get("data").isJsonNull()) {
                    return obj.get("data").getAsString();
                } else {
                    Main.logUtils.addLog(Level.ERROR, "Der neueste QR-Code hat keinen (data) Eintrag.");
                }
            }
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
        List<Map<String, String>> mapList = new ArrayList<>();
        JsonArray jsonArray = readStoreArray();
        for (JsonElement e : jsonArray) {
            if (e == null || !e.isJsonObject()) continue;
            JsonObject obj = e.getAsJsonObject();
            Map<String, String> map = GSON.fromJson(GSON.toJson(obj), Map.class);
            mapList.add(map);
        }
        return mapList;
    }

    /**
     * Clears the local scans.json store by resetting it to an empty JSON array.
     * This avoids FileNotFound issues for callers that expect the store to exist.
     */
    public static void clearStoredQRCodes() {
        ensureStoreExists();
        try {
            Files.writeString(STORE.toPath(), "[]", StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Could not clear stored QR codes: {}", e.getMessage(), e);
            Main.logUtils.addLog(Level.ERROR, "Konnte gespeicherte QR-Codes nicht löschen: " + e.getMessage());
        }
    }
}
