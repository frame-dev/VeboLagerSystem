package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.simplejavautils.SimpleJavaUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportUtils {

    private final File INVENTORY_FILE;

    private static ImportUtils instance;

    private ImportUtils() {
        INVENTORY_FILE = new SimpleJavaUtils().getFromResourceFile("inventar.json", Main.class);
    }

    public static ImportUtils getInstance() {
        if (instance == null) {
            instance = new ImportUtils();
        }
        return instance;
    }

    public List<Map<String, Object>> loadInventoryFile() {
        List<Map<String, Object>> inventoryData = new ArrayList<>();
        if (INVENTORY_FILE == null || !INVENTORY_FILE.exists()) {
            return inventoryData;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(INVENTORY_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) return inventoryData;

            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) continue;
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = new HashMap<>();
                for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                    String key = entry.getKey();
                    JsonElement value = entry.getValue();

                    if (value.isJsonPrimitive()) {
                        if (value.getAsJsonPrimitive().isString()) {
                            itemData.put(key, value.getAsString());
                        } else if (value.getAsJsonPrimitive().isNumber()) {
                            if( value.getAsString().contains(".") ) {
                                itemData.put(key, value.getAsDouble());
                            } else {
                                itemData.put(key, value.getAsInt());
                            }
                        } else if (value.getAsJsonPrimitive().isBoolean()) {
                            itemData.put(key, value.getAsBoolean());
                        }
                    } else if (value.isJsonArray()) {
                        itemData.put(key, gson.fromJson(value, List.class));
                    } else if (value.isJsonObject()) {
                        itemData.put(key, gson.fromJson(value, Map.class));
                    } else if (value.isJsonNull()) {
                        itemData.put(key, null);
                    }
                }
                inventoryData.add(itemData);
            }
        } catch (FileNotFoundException e) {
            // file was checked earlier; return empty list
        } catch (Exception e) {
            throw new RuntimeException("Failed to load inventory file", e);
        }
        return inventoryData;
    }
}