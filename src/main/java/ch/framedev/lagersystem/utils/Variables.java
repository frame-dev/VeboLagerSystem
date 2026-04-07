package ch.framedev.lagersystem.utils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ch.framedev.lagersystem.main.Main;

public class Variables {

    /**
     * Time in milliseconds that cached data (e.g. article details) is considered valid before it should be refreshed.
     * Set to 5 minutes (5 * 60 * 1000 ms) by default, but can be adjusted based on expected data change frequency and performance needs.
     */
    public static final long CACHE_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes

    /**
     * Application version string, used for display in the GUI and logging. Should
     * be updated with each release to reflect the current version of the
     * application.
     */
    public static final String VERSION = "0.3-TESTING";

    public static File getScanStore() {
        return new File(Main.getAppDataDir(), "scans.json");
    }

    public static Map<String,String> settingsDefaults() {
        Map<String,String> defaults = new HashMap<>();
        defaults.put("load-from-files", "true");
        defaults.put("database_type", "sqlite");
        defaults.put("stock_check_interval", "30");
        defaults.put("warning_display_interval", "1");
        defaults.put("github-token", "your_github_token_here");
        defaults.put("first-time", "false");
        defaults.put("table_font_size", "16");
        defaults.put("dark_mode", "false");
        defaults.put("table_font_size_tab", "15");
        defaults.put("font_style", "Dialog");
        defaults.put("theme_header_color", "");
        defaults.put("theme_accent_color", "");
        defaults.put("theme_button_color", "");
        defaults.put("enable_hourly_warnings", "true");
        defaults.put("enable_automatic_import_qrcode", "true");
        defaults.put("qrcode_import_interval", "10");
        defaults.put("enable_auto_stock_check", "true");
        defaults.put("server_url", "https://framedev.ch/vebo");
        defaults.put("delete_old_logs_on_startup", "false");
        defaults.put("look_and_feel", "");
        defaults.put("disable_header", "false");
        defaults.put("scan_server_port", "8080");
        return defaults;
    }
    
}
