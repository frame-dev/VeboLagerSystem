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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO: Performance optimizations if needed for large files.
 */
public class ImportUtils {

    private static final Logger LOGGER = LogManager.getLogger(ImportUtils.class);
    private final File INVENTORY_FILE;
    private final File VENDOR_FILE;
    private final File DEPARTMENT_FILE;
    private final File CLIENTS_FILE;
    private static volatile ImportUtils instance;

    /**
     * Private constructor for the ImportUtils class to initialize and load
     * essential configuration files required by the application. This constructor
     * is intended for internal use and performs the following operations:
     * <p>
     * 1. Loads the inventory file 'inventar.json' from the application's resources.
     * Logs a warning if the file is not found or does not exist and an info
     * message if the file is successfully loaded.
     * <p>
     * 2. Loads the vendor file 'vendor.json'. Logs a warning if the file is not
     * found or does not exist and an info message if the file is successfully
     * loaded.
     * <p>
     * 3. Loads the department file 'departments.json'. Logs a warning if the file
     * is not found or does not exist and an info message if the file is
     * successfully loaded.
     * <p>
     * 4. Loads the clients file 'clients.json'. Logs a warning if the file is not
     * found or does not exist and an info message if the file is successfully
     * loaded.
     * <p>
     * The LOGGER is used to report file loading statuses, and messages are also
     * recorded using the logging utility provided by the Main class. If any of
     * the required files are not found, the respective warnings ensure visibility
     * to issues during the initialization phase.
     */
    private ImportUtils() {
        // Load inventory file
        INVENTORY_FILE = new SimpleJavaUtils().getFromResourceFile("inventar.json", Main.class);
        if (INVENTORY_FILE == null) {
            LOGGER.warn("Inventory file 'inventar.json' not found in resources");
            Main.logUtils.addLog("Inventory file 'inventar.json' not found in resources");
        } else if (!INVENTORY_FILE.exists()) {
            LOGGER.warn("Inventory file does not exist: {}", INVENTORY_FILE.getAbsolutePath());
            Main.logUtils.addLog(INVENTORY_FILE.getAbsolutePath() + " does not exist");
        } else {
            LOGGER.info("Inventory file loaded: {}", INVENTORY_FILE.getAbsolutePath());
            Main.logUtils.addLog(INVENTORY_FILE.getAbsolutePath() + " loaded successfully");
        }

        // Load vendor file
        VENDOR_FILE = new SimpleJavaUtils().getFromResourceFile("vendor.json", Main.class);
        if (VENDOR_FILE == null) {
            LOGGER.warn("Vendor file 'vendor.json' not found in resources");
            Main.logUtils.addLog("Vendor file 'vendor.json' not found in resources");
        } else if (!VENDOR_FILE.exists()) {
            LOGGER.warn("Vendor file does not exist: {}", VENDOR_FILE.getAbsolutePath());
            Main.logUtils.addLog(VENDOR_FILE.getAbsolutePath() + " does not exist");
        } else {
            LOGGER.info("Vendor file loaded: {}", VENDOR_FILE.getAbsolutePath());
            Main.logUtils.addLog(VENDOR_FILE.getAbsolutePath() + " loaded successfully");
        }

        // Load department file
        DEPARTMENT_FILE = new SimpleJavaUtils().getFromResourceFile("departments.json", Main.class);
        if (DEPARTMENT_FILE == null) {
            LOGGER.warn("Department file 'departments.json' not found in resources");
            Main.logUtils.addLog("Department file 'departments.json' not found in resources");
        } else if (!DEPARTMENT_FILE.exists()) {
            LOGGER.warn("Department file does not exist: {}", DEPARTMENT_FILE.getAbsolutePath());
            Main.logUtils.addLog(DEPARTMENT_FILE.getAbsolutePath() + " does not exist");
        } else {
            LOGGER.info("Department file loaded: {}", DEPARTMENT_FILE.getAbsolutePath());
            Main.logUtils.addLog(DEPARTMENT_FILE.getAbsolutePath() + " loaded successfully");
        }

        // Load clients file
        CLIENTS_FILE = new SimpleJavaUtils().getFromResourceFile("clients.json", Main.class);
        if (CLIENTS_FILE == null) {
            LOGGER.warn("Clients file 'clients.json' not found in resources");
            Main.logUtils.addLog("Clients file 'clients.json' not found in resources");
        } else if (!CLIENTS_FILE.exists()) {
            LOGGER.warn("Clients file does not exist: {}", CLIENTS_FILE.getAbsolutePath());
            Main.logUtils.addLog(CLIENTS_FILE.getAbsolutePath() + " does not exist");
        } else {
            LOGGER.info("Clients file loaded: {}", CLIENTS_FILE.getAbsolutePath());
            Main.logUtils.addLog(CLIENTS_FILE.getAbsolutePath() + " loaded successfully");
        }
    }

    /**
     * Create a instance of this class if not already created and return it.
     *
     * @return Instance of ImportUtils
     */
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

    /**
     * Load the clients list from the 'clients.json' file. Each client is represented as a Map<String, Object>.
     *
     * @return List of clients loaded from the file, or an empty list if the file cannot be loaded or is invalid.
     */
    public List<Map<String, Object>> loadClientsList() {
        List<Map<String, Object>> clients = new ArrayList<>();

        if (CLIENTS_FILE == null || !CLIENTS_FILE.exists()) {
            LOGGER.warn("Cannot load file 'clients.json' from resources.");
            Main.logUtils.addLog("Cannot load file 'clients.json' from resources.");
            return clients;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(CLIENTS_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) {
                LOGGER.info("Loaded {} clients from file 'clients.json'.", 0);
                Main.logUtils.addLog("Loaded 0 clients from file 'clients.json'.");
                return clients;
            }
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in clients array");
                    Main.logUtils.addLog("Skipping non-object element in clients array");
                    continue;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                clients.add(itemData);
            }
            LOGGER.info("Loaded {} clients from file 'clients.json'.", clients.size());
            Main.logUtils.addLog(String.format("Loaded %d clients from file 'clients.json'.", clients.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to load clients from file 'clients.json'", e);
            Main.logUtils.addLog("Failed to load clients from file 'clients.json'.");
        }
        return clients;
    }

    /**
     * Load the department list from the 'departments.json' file. Each department is represented as a Map<String, Object>.
     *
     * @return List of departments loaded from the file, or an empty list if the file cannot be loaded or is invalid.
     */
    public List<Map<String, Object>> loadDepartmentsList() {
        List<Map<String, Object>> list = new ArrayList<>();

        if (DEPARTMENT_FILE == null || !DEPARTMENT_FILE.exists()) {
            LOGGER.warn("Cannot load file 'departments.json' from resources.");
            Main.logUtils.addLog("Cannot load file 'departments.json' from resources.");
            return list;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(DEPARTMENT_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) {
                LOGGER.info("Loaded {} departments from file 'departments.json'.", 0);
                Main.logUtils.addLog("Loaded 0 departments from file 'departments.json'.");
                return list;
            }
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in departments array");
                    Main.logUtils.addLog("Skipping non-object element in departments array");
                    continue;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                list.add(itemData);
            }
            LOGGER.info("Loaded {} departments from file 'departments.json'.", list.size());
            Main.logUtils.addLog(String.format("Loaded %d departments from file 'departments.json'.", list.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to load departments from file 'departments.json'", e);
            Main.logUtils.addLog("Failed to load departments from file 'departments.json'.");
        }
        return list;
    }

    /**
     * Load the vendor list from the 'vendor.json' file. Each vendor is represented as a Map<String, Object>.
     *
     * @return List of vendors loaded from the file, or an empty list if the file cannot be loaded or is invalid.
     */
    public List<Map<String, Object>> loadVendorList() {
        List<Map<String, Object>> list = new ArrayList<>();

        if (VENDOR_FILE == null || !VENDOR_FILE.exists()) {
            LOGGER.warn("Cannot load Vendor list from file 'vendor.json'.");
            Main.logUtils.addLog("Cannot load Vendor list from file 'vendor.json'.");
            return list;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(VENDOR_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
            if (jsonArray == null) {
                LOGGER.info("Loaded {} vendors from file 'vendor.json'.", 0);
                Main.logUtils.addLog("Loaded 0 vendors from file 'vendor.json'.");
                return list;
            }
            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in vendor array");
                    Main.logUtils.addLog("Skipping non-object element in vendor array");
                    continue;
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                list.add(itemData);
            }

            LOGGER.info("Loaded {} vendors from file 'vendor.json'.", list.size());
            Main.logUtils.addLog(String.format("Loaded %d vendors from file 'vendor.json'.", list.size()));
        } catch (IOException e) {
            LOGGER.error("Failed to load vendors from file 'vendor.json'", e);
            Main.logUtils.addLog("Failed to load vendors from file 'vendor.json'.");
        }
        return list;
    }

    /**
     * Load the inventory data from the 'inventar.json' file. Each inventory item is represented as a Map<String, Object>.
     *
     * @return List of inventory items loaded from the file, or an empty list if the file cannot be loaded or is invalid.
     */
    public List<Map<String, Object>> loadInventoryFile() {
        List<Map<String, Object>> inventoryData = new ArrayList<>();

        if (INVENTORY_FILE == null || !INVENTORY_FILE.exists()) {
            LOGGER.warn("Cannot load inventory: file is null or does not exist");
            Main.logUtils.addLog("Cannot load inventory: file is null or does not exist");
            return inventoryData;
        }

        Gson gson = new Gson();
        try (FileReader reader = new FileReader(INVENTORY_FILE)) {
            JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);

            if (jsonArray == null) {
                LOGGER.warn("Inventory file is empty or invalid JSON");
                Main.logUtils.addLog("Inventory file is empty or invalid JSON");
                return inventoryData;
            }

            for (JsonElement jsonElement : jsonArray) {
                if (!jsonElement.isJsonObject()) {
                    LOGGER.debug("Skipping non-object element in inventory array");
                    Main.logUtils.addLog("Skipping non-object element in inventory array");
                    continue;
                }

                JsonObject jsonObject = jsonElement.getAsJsonObject();
                Map<String, Object> itemData = parseJsonObject(gson, jsonObject);
                inventoryData.add(itemData);
            }

            LOGGER.info("Successfully loaded {} items from inventory file", inventoryData.size());
            Main.logUtils.addLog(String.format("Loaded %d items from inventory file.", inventoryData.size()));

        } catch (IOException e) {
            LOGGER.error("Failed to read inventory file: {}", INVENTORY_FILE.getAbsolutePath(), e);
            Main.logUtils.addLog("Failed to read inventory file: " + INVENTORY_FILE.getAbsolutePath());
            throw new RuntimeException("Failed to load inventory file", e);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while parsing inventory file", e);
            Main.logUtils.addLog(String.format("Unexpected error while parsing inventory file: %s", INVENTORY_FILE.getAbsolutePath()));
            throw new RuntimeException("Failed to load inventory file", e);
        }

        return inventoryData;
    }

    /**
     * Parse a JsonObject into a Map<String, Object>.
     *
     * @param gson       Gson instance for parsing
     * @param jsonObject JsonObject to parse
     * @return Map representation of the JsonObject
     */
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

    /**
     * Parse a JsonPrimitive into the appropriate Java type.
     *
     * @param value JsonElement representing a primitive value
     * @return Parsed Java Object (String, Boolean, Integer, Long, Double)
     */
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

    /**
     * Add an item name to the list of imported items. This method appends the given item name to a text file named 'imported_items.txt' located in the application's data directory. Each item name is written on a new line. If the file does not exist, it will be created. If an error occurs during writing, an error message is logged using the application's logging utility and the logger.
     *
     * @param itemName the itemName to add
     */
    public static void addToList(String itemName) {
        File file = new File(Main.getAppDataDir(), "imported_items.txt");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(itemName + System.lineSeparator());
        } catch (IOException e) {
            Main.logUtils.addLog("Failed to write item to imported_items.txt");
            LOGGER.error("Failed to write item to imported_items.txt", e);
        }
    }

    /**
     * Get the list of imported items. This method reads the 'imported_items.txt' file located in the application's data directory and returns a list of item names that have been imported. Each line in the file represents a single item name. If the file does not exist, an empty list is returned. If an error occurs during reading, an error message is logged using the application's logging utility and the logger, and an empty list is returned.
     * @return List of imported item names, or an empty list if the file does not exist or an error occurs during reading.
     */
    public static List<String> getImportedItems() {
        List<String> importedItems = new ArrayList<>();
        File file = new File(Main.getAppDataDir(), "imported_items.txt");
        if (!file.exists()) {
            return importedItems;
        }
        try {
            importedItems = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            Main.logUtils.addLog("Failed to read items from imported_items.txt");
            LOGGER.error("Failed to read imported items from imported_items.txt", e);
        }
        return importedItems;
    }

    /**
     * Add a QR code ID to the list of imported QR codes. This method appends the given QR code ID to a text file named 'imported_qrcodes.txt' located in the application's data directory. Each QR code ID is written on a new line. If the file does not exist, it will be created. If an error occurs during writing, an error message is logged using the application's logging utility and the logger.
     *
     * @param qrId The QR code ID to be added to the list of imported QR codes.
     */
    public static void addQrCodeImport(String qrId) {
        File file = new File(Main.getAppDataDir(), "imported_qrcodes.txt");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(qrId + System.lineSeparator());
            writer.flush();
        } catch (IOException e) {
            Main.logUtils.addLog("Failed to write imported qrcodes.txt to imported_qrcodes.txt");
            LOGGER.error("Failed to write QR code ID to imported_qrcodes.txt", e);
        }
    }

    /**
     * Get the list of imported QR code IDs. This method reads the 'imported_qrcodes.txt' file located in the application's data directory and returns a list of QR code IDs that have been imported. Each line in the file represents a single QR code ID. If the file does not exist, an empty list is returned. If an error occurs during reading, an error message is logged using the application's logging utility and the logger, and an empty list is returned.
     * @return List of imported QR code IDs, or an empty list if the file does not exist or an error occurs during reading.
     */
    public static List<String> getImportedQrCodes() {
        List<String> importedQrCodes = new ArrayList<>();
        File file = new File(Main.getAppDataDir(), "imported_qrcodes.txt");
        if (!file.exists()) {
            return importedQrCodes;
        }
        try {
            importedQrCodes = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            Main.logUtils.addLog("Failed to read imported qrcodes.txt from imported_qrcodes.txt");
            LOGGER.error("Failed to read imported QR codes from imported_qrcodes.txt", e);
        }
        return importedQrCodes;
    }

    /**
     * Add a data entry to the own use list. This method appends the given data string to a text file named 'own_use_list.txt' located in the application's data directory. Each data entry is written on a new line. If the file does not exist, it will be created. If an error occurs during writing, an error message is logged using the application's logging utility and the logger.
     * @param data The data string to be added to the own use list.
     */
    public static void addToOwnUseList(String data) {
        File file = new File(Main.getAppDataDir(), "own_use_list.txt");
        try (FileWriter writer = new FileWriter(file, true)) {
            writer.write(data + System.lineSeparator());
        } catch (IOException e) {
            Main.logUtils.addLog("Failed to write own_use_list.txt");
            LOGGER.error("Failed to write data to own_use_list.txt", e);
        }
    }
}