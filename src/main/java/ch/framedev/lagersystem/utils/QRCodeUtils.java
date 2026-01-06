package ch.framedev.lagersystem.utils;

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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class QRCodeUtils {

    private static final Logger logger = LogManager.getLogger(QRCodeUtils.class);

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
            }
        } catch (IOException e) {
            logger.error("Error while fetching QR code data from website", e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return mapList;
    }

    private static final File STORE = new File(Main.getAppDataDir(), "scans.json"); // eine Zeile = ein JSON

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
            throw new RuntimeException(e);
        }
        return dataList;
    }

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
            throw new RuntimeException(e);
        }
        return qrCodeList;
    }

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
            throw new RuntimeException(e);
        }
        return null;
    }

    public static String[] getPartsFromData(String data) {
        if (data == null) return new String[0];
        return data.split(";");
    }

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
            throw new RuntimeException(e);
        }
        return mapList;
    }

    public static void clearStoredQRCodes() {
        if (STORE.exists()) {
            if(!STORE.delete())
                System.err.println("Konnte gespeicherte QR-Codes nicht löschen: " + STORE.getAbsolutePath());
        }
    }
}
