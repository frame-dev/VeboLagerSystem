package ch.framedev.lagersystem.scan;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.NetUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("SameReturnValue")
public class ScanServer {

    private static final Logger logger = LogManager.getLogger(ScanServer.class);

    private static final File STORE = new File(Main.getAppDataDir(), "scans.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT);

    @SuppressWarnings("HttpUrlsUsage")
    public static void main(String[] args) throws Exception {
        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/scan", ScanServer::handleScanRequest);
        server.createContext("/scan.php", ScanServer::handleScanRequest);
        server.createContext("/list", ScanServer::handleListRequest);
        server.createContext("/scans.json", ScanServer::handleListRequest);
        server.createContext("/latest", ScanServer::handleLatestRequest);
        server.createContext("/latest.json", ScanServer::handleLatestRequest);

        server.start();
        logger.info("Server running:");
        logger.info("  http://<host>:8080/scan?data=...");
        logger.info("  http://<host>:8080/scan.php?data=...");
        logger.info("  http://<host>:8080/list");
        logger.info("  http://<host>:8080/scans.json");
        logger.info("  http://<host>:8080/latest");
        logger.info("  http://<host>:8080/latest.json");
        logger.info("  Local IP: {}", NetUtils.getLocalIp());
    }

    private static void handleScanRequest(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                respond(exchange, 405, "Method Not Allowed");
                return;
            }

            String query = exchange.getRequestURI().getRawQuery();
            String decodedData = getQueryParam(query, "data");
            if (decodedData == null || decodedData.isBlank()) {
                respond(exchange, 200, buildMissingDataPage(), "text/html; charset=utf-8");
                return;
            }

            String qtyRaw = getQueryParam(query, "quantity");
            if (qtyRaw == null || qtyRaw.isBlank()) {
                respond(exchange, 200, buildFormPage(
                        decodedData,
                        getQueryParam(query, "size"),
                        getQueryParam(query, "color"),
                        normalizeType(getQueryParam(query, "type")),
                        getQueryParam(query, "ownUse") != null), "text/html; charset=utf-8");
                return;
            }

            int quantity = parseQuantity(qtyRaw);
            boolean ownUse = getQueryParam(query, "ownUse") != null;
            String type = normalizeType(getQueryParam(query, "type"));
            String size = normalizeOptionalValue(getQueryParam(query, "size"));
            String color = normalizeOptionalValue(getQueryParam(query, "color"));

            JsonObject obj = new JsonObject();
            obj.addProperty("id", UUID.randomUUID().toString());
            obj.addProperty("timestamp", TIMESTAMP_FORMAT.format(LocalDateTime.now()));
            obj.addProperty("ts", Instant.now().toString());
            obj.addProperty("data", decodedData);
            obj.addProperty("quantity", quantity);
            obj.addProperty("type", type);
            obj.addProperty("ownUse", ownUse ? "Ja" : "Nein");
            obj.addProperty("size", size);
            obj.addProperty("color", color);
            appendToArrayFile(obj);

            logger.info("Received: {} qty={} ownUse={} type={}{}{} from {}",
                    decodedData,
                    quantity,
                    ownUse,
                    type,
                    size.isBlank() ? "" : " size=" + size,
                    color.isBlank() ? "" : " color=" + color,
                    NetUtils.getClientIp(exchange));

            respond(exchange, 200, buildSuccessPage(), "text/html; charset=utf-8");
        } catch (Exception e) {
            Main.logUtils.addLog("Fehler beim Verarbeiten des Scan-Requests: " + e.getMessage());
            logger.error("Fehler beim Verarbeiten des Scan-Requests", e);
            respond(exchange, 500, "Internal Server Error");
        }
    }

    private static void handleListRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "Method Not Allowed");
            return;
        }
        JsonArray arr = readArrayFile();
        respond(exchange, 200, GSON.toJson(arr) + "\n", "application/json; charset=utf-8");
    }

    private static void handleLatestRequest(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            respond(exchange, 405, "Method Not Allowed");
            return;
        }

        JsonArray arr = readArrayFile();
        if (arr.isEmpty()) {
            respond(exchange, 200, "{}\n", "application/json; charset=utf-8");
            return;
        }

        JsonElement last = arr.get(arr.size() - 1);
        respond(exchange, 200, GSON.toJson(last) + "\n", "application/json; charset=utf-8");
    }

    private static int parseQuantity(String qtyRaw) {
        int quantity = 1;
        try {
            quantity = Integer.parseInt(qtyRaw);
            if (quantity < 1) {
                quantity = 1;
            }
        } catch (NumberFormatException ignored) {
        }
        return quantity;
    }

    private static String normalizeType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "sell";
        }
        return switch (rawType.trim().toLowerCase(Locale.ROOT)) {
            case "buy" -> "buy";
            case "order" -> "order";
            default -> "sell";
        };
    }

    private static String normalizeOptionalValue(String value) {
        return value == null ? "" : value.trim();
    }

    private static String escHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String buildFormPage(String decodedData, String selectedSize, String selectedColor, String selectedType,
                                        boolean ownUse) {
        String safeData = escHtml(decodedData);
        String safeSize = escHtml(normalizeOptionalValue(selectedSize));
        String safeColor = escHtml(normalizeOptionalValue(selectedColor));
        String normalizedType = normalizeType(selectedType);
        VariantOptions options = detectVariantOptions(decodedData);

        return String.format("""
                <!doctype html>
                <html lang="de">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Scan</title>
                    <style>
                        :root {
                            --bg: #eef3fb;
                            --surface: #ffffff;
                            --border: #d7e0ee;
                            --text: #1f2b3d;
                            --muted: #607089;
                            --primary: #285ea8;
                            --primary-hover: #1f4f8f;
                            --shadow: 0 18px 40px rgba(24, 45, 78, 0.12);
                        }
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            font-family: "Segoe UI", Arial, sans-serif;
                            background: linear-gradient(180deg, #f4f8ff 0%%, var(--bg) 100%%);
                            color: var(--text);
                        }
                        .page {
                            min-height: 100vh;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                            padding: 24px;
                        }
                        .card {
                            width: min(100%%, 760px);
                            background: var(--surface);
                            border: 1px solid var(--border);
                            border-radius: 22px;
                            box-shadow: var(--shadow);
                            overflow: hidden;
                        }
                        .hero {
                            padding: 26px 28px 20px;
                            background: linear-gradient(135deg, #2d6fcc 0%%, #18386b 100%%);
                            color: white;
                        }
                        .hero h1 {
                            margin: 0 0 8px;
                            font-size: 30px;
                        }
                        .hero p {
                            margin: 0;
                            color: rgba(255,255,255,0.84);
                            font-size: 14px;
                        }
                        .payload {
                            margin-top: 16px;
                            padding: 12px 14px;
                            border-radius: 14px;
                            background: rgba(255,255,255,0.12);
                            font-size: 13px;
                            word-break: break-word;
                        }
                        form {
                            padding: 24px 28px 28px;
                        }
                        .grid {
                            display: grid;
                            grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                            gap: 16px;
                        }
                        .field {
                            display: flex;
                            flex-direction: column;
                            gap: 8px;
                        }
                        label {
                            font-weight: 600;
                            font-size: 14px;
                        }
                        .subtle {
                            color: var(--muted);
                            font-weight: 500;
                            font-size: 12px;
                        }
                        input[type="number"],
                        input[type="text"] {
                            width: 100%%;
                            padding: 12px 14px;
                            border-radius: 12px;
                            border: 1px solid var(--border);
                            font-size: 15px;
                            color: var(--text);
                            background: #fbfdff;
                        }
                        input:focus {
                            outline: 2px solid rgba(40, 94, 168, 0.18);
                            border-color: var(--primary);
                        }
                        .options {
                            margin-top: 20px;
                            display: flex;
                            flex-wrap: wrap;
                            gap: 16px;
                        }
                        .checkbox,
                        .radio {
                            display: inline-flex;
                            align-items: center;
                            gap: 8px;
                            padding: 10px 12px;
                            border: 1px solid var(--border);
                            border-radius: 12px;
                            background: #fbfdff;
                            font-weight: 500;
                        }
                        .radio-group {
                            margin-top: 20px;
                            display: flex;
                            flex-wrap: wrap;
                            gap: 10px;
                        }
                        .actions {
                            margin-top: 24px;
                            display: flex;
                            justify-content: flex-end;
                        }
                        button {
                            border: none;
                            border-radius: 12px;
                            padding: 13px 22px;
                            font-size: 15px;
                            font-weight: 700;
                            color: white;
                            background: var(--primary);
                            cursor: pointer;
                        }
                        button:hover {
                            background: var(--primary-hover);
                        }
                    </style>
                </head>
                <body>
                    <div class="page">
                        <div class="card">
                            <div class="hero">
                                <h1>Scan erfassen</h1>
                                <p>Menge, Typ und optionale Varianten für den gescannten Artikel speichern.</p>
                                <div class="payload">%s</div>
                            </div>
                            <form method="get" action="/scan.php">
                                <input type="hidden" name="data" value="%s">

                                <div class="grid">
                                    <div class="field">
                                        <label for="quantity">Menge</label>
                                        <input type="number" id="quantity" name="quantity" min="1" value="1" required>
                                    </div>
                                    <div class="field">
                                        <label for="size">Größe <span class="subtle">optional</span></label>
                                        <input list="size-options" id="size" name="size" value="%s" placeholder="z.B. M oder XL">
                                        %s
                                    </div>
                                    <div class="field">
                                        <label for="color">Farbe <span class="subtle">optional</span></label>
                                        <input list="color-options" id="color" name="color" value="%s" placeholder="z.B. Blau oder Rot">
                                        %s
                                    </div>
                                </div>

                                <div class="options">
                                    <label class="checkbox">
                                        <input type="checkbox" name="ownUse" %s>
                                        Eigenbedarf
                                    </label>
                                </div>

                                <div class="radio-group">
                                    <label class="radio">
                                        <input type="radio" name="type" value="sell" %s>
                                        Verkauf
                                    </label>
                                    <label class="radio">
                                        <input type="radio" name="type" value="buy" %s>
                                        Lagern
                                    </label>
                                    <label class="radio">
                                        <input type="radio" name="type" value="order" %s>
                                        Bestellen
                                    </label>
                                </div>

                                <div class="actions">
                                    <button type="submit">Absenden</button>
                                </div>
                            </form>
                        </div>
                    </div>
                </body>
                </html>
                """,
                safeData,
                safeData,
                safeSize,
                buildDataList("size-options", options.sizes()),
                safeColor,
                buildDataList("color-options", options.colors()),
                ownUse ? "checked" : "",
                "sell".equals(normalizedType) ? "checked" : "",
                "buy".equals(normalizedType) ? "checked" : "",
                "order".equals(normalizedType) ? "checked" : "");
    }

    private static VariantOptions detectVariantOptions(String data) {
        String details = extractDataField(data, "details");
        if (details.isBlank()) {
            return new VariantOptions(List.of(), List.of());
        }

        List<String> sizes = new ArrayList<>();
        for (String size : List.of("XS", "S", "M", "L", "XL", "XXL", "XXXL")) {
            if (containsStandaloneToken(details, size)) {
                sizes.add(size);
            }
        }

        List<String> colors = new ArrayList<>();
        for (String color : List.of("Gelb", "Blau", "Rot", "Grün", "Gruen", "Schwarz", "Weiss", "Weiß", "Orange")) {
            if (containsStandaloneToken(details, color)) {
                colors.add(color);
            }
        }

        return new VariantOptions(sizes, colors);
    }

    private static boolean containsStandaloneToken(String details, String token) {
        return Pattern.compile("(?<!\\p{L})" + Pattern.quote(token) + "(?!\\p{L})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(details)
                .find();
    }

    private static String extractDataField(String data, String key) {
        if (data == null || data.isBlank()) {
            return "";
        }
        for (String part : data.split(";")) {
            int idx = part.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String currentKey = part.substring(0, idx).trim();
            if (currentKey.equalsIgnoreCase(key)) {
                return part.substring(idx + 1).trim();
            }
        }
        return "";
    }

    private static String buildDataList(String id, List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("<datalist id=\"")
                .append(escHtml(id))
                .append("\">");
        for (String value : values) {
            builder.append("<option value=\"")
                    .append(escHtml(value))
                    .append("\"></option>");
        }
        builder.append("</datalist>");
        return builder.toString();
    }

    private record VariantOptions(List<String> sizes, List<String> colors) {
    }

    private static String buildMissingDataPage() {
        return """
                <!doctype html>
                <html lang="de">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Scan</title>
                    <style>
                        body {
                            margin: 0;
                            min-height: 100vh;
                            display: grid;
                            place-items: center;
                            background: linear-gradient(180deg, #f4f8ff 0%, #e9f0fb 100%);
                            font-family: "Segoe UI", Arial, sans-serif;
                            color: #1f2b3d;
                            padding: 24px;
                        }
                        .card {
                            width: min(100%, 520px);
                            background: white;
                            border: 1px solid #d7e0ee;
                            border-radius: 22px;
                            padding: 28px;
                            box-shadow: 0 18px 40px rgba(24, 45, 78, 0.12);
                        }
                        h1 {
                            margin: 0 0 12px;
                            font-size: 28px;
                        }
                        p {
                            margin: 0 0 14px;
                            line-height: 1.5;
                            color: #607089;
                        }
                        a {
                            color: #285ea8;
                            font-weight: 600;
                            text-decoration: none;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h1>Kein Scaninhalt vorhanden</h1>
                        <p>Diese Seite wird normalerweise ueber einen QR-Code mit dem Parameter <code>data</code> aufgerufen.</p>
                        <p><a href="/scans.json">Gespeicherte Scans ansehen</a></p>
                    </div>
                </body>
                </html>
                """;
    }

    private static String buildSuccessPage() {
        return """
                <!doctype html>
                <html lang="de">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Scan abgeschlossen</title>
                </head>
                <body>
                    <h1>Gespeichert</h1>
                    <p>Der Scan wurde gespeichert.</p>
                    <a href="/latest">Letzter Scan</a> |
                    <a href="/scans.json">Alle Scans</a> |
                    <a href="/scan.php">Neuer Scan</a>
                </body>
                </html>
                """;
    }

    private static String getQueryParam(String query, String key) {
        if (query == null) {
            return null;
        }
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String currentKey = URLDecoder.decode(part.substring(0, eq), StandardCharsets.UTF_8);
            if (!currentKey.equals(key)) {
                continue;
            }
            return URLDecoder.decode(part.substring(eq + 1), StandardCharsets.UTF_8);
        }
        return null;
    }

    private static void respond(HttpExchange ex, int code, String body) throws IOException {
        respond(ex, code, body, "text/plain; charset=utf-8");
    }

    private static void respond(HttpExchange ex, int code, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(code, bytes.length);
        try (ex; OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static synchronized void appendToArrayFile(JsonObject element) throws IOException {
        File parent = STORE.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logger.error("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());
        }

        JsonArray arr = readArrayFile();
        arr.add(element);

        try (Writer w = Files.newBufferedWriter(
                STORE.toPath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            GSON.toJson(arr, w);
        }
    }

    private static JsonArray readArrayFile() throws IOException {
        if (!STORE.exists() || STORE.length() == 0) {
            return new JsonArray();
        }

        try (Reader r = Files.newBufferedReader(STORE.toPath(), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(r);
            if (el == null || el.isJsonNull() || !el.isJsonArray()) {
                return new JsonArray();
            }
            return el.getAsJsonArray();
        } catch (JsonParseException e) {
            return new JsonArray();
        }
    }
}
