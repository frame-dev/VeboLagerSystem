package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class QRCodeUtils {

    private static final File STORE = new File(Main.getAppDataDir(), "scans.jsonl"); // eine Zeile = ein JSON

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
}
