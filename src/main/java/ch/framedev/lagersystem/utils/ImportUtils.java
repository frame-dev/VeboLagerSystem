package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.simplejavautils.SimpleJavaUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImportUtils {

    private static final Logger LOGGER = LogManager.getLogger(ImportUtils.class);
    private final File INVENTORY_FILE;
    private final File VENDOR_FILE;
    private final File DEPARTMENT_FILE;
    private static volatile ImportUtils instance;

    private ImportUtils() {
        INVENTORY_FILE = new SimpleJavaUtils().getFromResourceFile("inventar.json", Main.class);
        if (INVENTORY_FILE == null) {
            LOGGER.warn("Inventory file 'inventar.json' not found in resources");
        } else if (!INVENTORY_FILE.exists()) {
            LOGGER.warn("Inventory file does not exist: {}", INVENTORY_FILE.getAbsolutePath());
        } else {
            LOGGER.info("Inventory file loaded: {}", INVENTORY_FILE.getAbsolutePath());
        }

        VENDOR_FILE = new SimpleJavaUtils().getFromResourceFile("vendor.json", Main.class);
        if (VENDOR_FILE == null) {
            LOGGER.warn("Vendor file 'vendor.json' not found in resources");
        } else if (!VENDOR_FILE.exists()) {
            LOGGER.warn("Vendor file does not exist: {}", VENDOR_FILE.getAbsolutePath());
        } else {
            LOGGER.info("Vendor file loaded: {}", VENDOR_FILE.getAbsolutePath());
        }

        DEPARTMENT_FILE = new SimpleJavaUtils().getFromResourceFile("departments.json", Main.class);
        if (DEPARTMENT_FILE == null) {
            LOGGER.warn("Department file 'departments.json' not found in resources");
        } else if (!DEPARTMENT_FILE.exists()) {
            LOGGER.warn("Department file does not exist: {}", DEPARTMENT_FILE.getAbsolutePath());
        } else {
            LOGGER.info("Department file loaded: {}", DEPARTMENT_FILE.getAbsolutePath());
        }
    }

    public static ImportUtils getInstance() {
        if (instance == null) {
            synchronized (ImportUtils.class) {
                if (instance == null) {
                    instance = new ImportUtils();
                }
            }
        }
        return instance;
    }

    public List<Map<String, Object>> loadDepartmentsList() {
        List<Map<String, Object>> list = new ArrayList<>();

        if(DEPARTMENT_FILE == null || !DEPARTMENT_FILE.exists()) {
            LOGGER.warn("Cannot load file 'departments.json' from resources.");
            return list;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(DEPARTMENT_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) {
                LOGGER.info("Loaded {} departments from file 'departments.json'.", 0);
                return list;
            }
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in departments array");
                    continue;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                list.add(itemData);
            }
            LOGGER.info("Loaded {} departments from file 'departments.json'.", list.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load departments from file 'departments.json'", e);
        }
        return list;
    }

    public List<Map<String, Object>> loadVendorList() {
        List<Map<String, Object>> list = new ArrayList<>();

        if(VENDOR_FILE == null || !VENDOR_FILE.exists()) {
            LOGGER.warn("Cannot load Vendor list from file 'vendor.json'.");
            return list;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(VENDOR_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if(jsonArray == null) {
                LOGGER.info("Loaded {} vendors from file 'vendor.json'.", 0);
                return list;
            }
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in vendor array");
                    continue;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                list.add(itemData);
            }

            LOGGER.info("Loaded {} vendors from file 'vendor.json'.", list.size());
        } catch (IOException e) {
            LOGGER.error("Failed to load vendors from file 'vendor.json'", e);
        }
        return list;
    }

    public List<Map<String, Object>> loadInventoryFile() {
        List<Map<String, Object>> inventoryData = new ArrayList<>();

        if (INVENTORY_FILE == null || !INVENTORY_FILE.exists()) {
            LOGGER.warn("Cannot load inventory: file is null or does not exist");
            return inventoryData;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(INVENTORY_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            if (jsonArray == null) {
                LOGGER.warn("Inventory file is empty or invalid JSON");
                return inventoryData;
            }

            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in inventory array");
                    continue;
                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                inventoryData.add(itemData);
            }

            LOGGER.info("Successfully loaded {} items from inventory file", inventoryData.size());

        } catch (IOException e) {
            LOGGER.error("Failed to read inventory file: {}", INVENTORY_FILE.getAbsolutePath(), e);
            throw new RuntimeException("Failed to load inventory file", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while parsing inventory file", e);
            throw new RuntimeException("Failed to load inventory file", e);
        }

        return inventoryData;
    }

    private Map<String, Object> parseJsonObject(Gson gson, JsonObject jsonObject) {
        Map<String, Object> itemData = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            if (value.isJsonNull()) {
                itemData.put(key, null);
            } else if (value.isJsonPrimitive()) {
                itemData.put(key, parsePrimitive(value));
            } else if (value.isJsonArray()) {
                itemData.put(key, gson.fromJson(value, List.class));
            } else if (value.isJsonObject()) {
                itemData.put(key, gson.fromJson(value, Map.class));
            }
        }

        return itemData;
    }

    private Object parsePrimitive(JsonElement value) {
        if (value.getAsJsonPrimitive().isString()) {
            return value.getAsString();
        } else if (value.getAsJsonPrimitive().isBoolean()) {
            return value.getAsBoolean();
        } else if (value.getAsJsonPrimitive().isNumber()) {
            // Check if the number has a decimal point
            String numStr = value.getAsString();
            if (numStr.contains(".")) {
                return value.getAsDouble();
            } else {
                // Try to fit in Integer first, fall back to Long if needed
                try {
                    return value.getAsInt();
                } catch (NumberFormatException e) {
                    return value.getAsLong();
                }
            }
        }
        return value.getAsString(); // fallback
    }
}