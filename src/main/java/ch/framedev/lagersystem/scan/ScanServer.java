package ch.framedev.lagersystem.scan;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.NetUtils;
import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.Executors;

public class ScanServer {

    // Single JSON file containing an array: [ {ts,data,quantity,ownUse}, ... ]
    private static final File STORE = new File(Main.getAppDataDir(), "scans.json");
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    @SuppressWarnings("HttpUrlsUsage")
    public static void main(String[] args) throws Exception {
        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/scan", exchange -> {
            try {
                if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    respond(exchange, 405, "Method Not Allowed");
                    return;
                }

                String query = exchange.getRequestURI().getRawQuery();

                // Always require data (scan payload) for both "show form" and "submit"
                String dataRaw = getQueryParam(query, "data");
                if (dataRaw == null || dataRaw.isBlank()) {
                    respond(exchange, 400, "Missing data");
                    return;
                }
                String decodedData = URLDecoder.decode(dataRaw, StandardCharsets.UTF_8);

                // If quantity is missing => show form
                String qtyRaw = getQueryParam(query, "quantity");
                if (qtyRaw == null || qtyRaw.isBlank()) {
                    String page = buildFormPage(decodedData);
                    respond(exchange, 200, page, "text/html; charset=utf-8");
                    return;
                }

                // Otherwise => treat as submission
                int quantity = 1;
                try {
                    quantity = Integer.parseInt(qtyRaw);
                    if (quantity < 1) quantity = 1;
                } catch (NumberFormatException ignored) {
                }

                // Checkbox: present only if checked
                boolean ownUse = getQueryParam(query, "ownUse") != null;
                // http://darryls-macbook-air.local:8080/scan?data=artikelNr%3A1244%3Bname%3ATestData%3Beinkaufspreis%3A12.5%3Bverkaufspreis%3A13.2%3Blieferant%3AZVG&ownUse=on&quantity=1&type=sell
                String type = getQueryParam(query, "type");
                boolean sellType = "sell".equalsIgnoreCase(type);
                boolean buyType = "buy".equalsIgnoreCase(type);

                JsonObject obj = new JsonObject();
                obj.addProperty("ts", Instant.now().toString());
                obj.addProperty("data", decodedData);
                obj.addProperty("quantity", quantity);
                obj.addProperty("ownUse", ownUse);
                if (sellType) {
                    obj.addProperty("type", "sell");
                } else if (buyType) {
                    obj.addProperty("type", "buy");
                } else {
                    obj.addProperty("type", "unknown");
                }
                appendToArrayFile(obj);

                System.out.println("Received: " + decodedData + " qty=" + quantity + " ownUse=" + ownUse
                        + " from " + NetUtils.getClientIp(exchange));

                // Success page (or redirect)
                respond(exchange, 200, buildSuccessPage(), "text/html; charset=utf-8");

            } catch (Exception e) {
                System.err.println("Error handling /scan: " + e);
                respond(exchange, 500, "Internal Server Error");
            }
        });

        server.createContext("/list", exchange -> {
            JsonArray arr = readArrayFile();
            // pretty optional; remove setPrettyPrinting() if you prefer compact
            String body = gson.toJson(arr) + "\n";
            respond(exchange, 200, body, "application/json; charset=utf-8");
        });

        server.createContext("/latest", exchange -> {
            JsonArray arr = readArrayFile();
            if (arr.isEmpty()) {
                respond(exchange, 200, "{}\n", "application/json; charset=utf-8");
                return;
            }
            JsonElement last = arr.get(arr.size() - 1);
            respond(exchange, 200, gson.toJson(last) + "\n", "application/json; charset=utf-8");
        });

        server.start();
        System.out.println("Server running:");
        System.out.println("  http://<host>:8080/scan?data=...");
        System.out.println("  http://<host>:8080/list");
        System.out.println("  http://<host>:8080/latest");
        System.out.println(NetUtils.getLocalIp());
    }

    private static String escHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String buildFormPage(String decodedData) {
        String safeData = escHtml(decodedData);

        return String.format("""
                <!doctype html>
                <html lang="de">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>Scan</title>
                </head>
                <body>
                    <h1>Bitte gebe die Menge an:</h1>
                
                    <form method="get" action="/scan">
                        <input type="hidden" name="data" value="%s">
                
                        <label>
                            <input type="checkbox" name="ownUse">
                            Eigenbedarf
                        </label>
                
                        <br><br>
                
                        <label for="quantity">Menge:</label>
                        <input type="number" id="quantity" name="quantity" min="1" required>
                        <br><br>
                        <label for="sell">Verkauf</label>
                        <input type="radio" id="sell" name="type" value="sell" checked>
                        <label for="buy">Einkauf</label>
                        <input type="radio" id="buy" name="type" value="buy">
                        <br><br>
                        <button type="submit">Absenden</button>
                    </form>
                </body>
                </html>
                """, safeData);
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
                    <a href="/list">Alle Scans</a>
                </body>
                </html>
                """;
    }


    // ---------- helpers ----------

    private static String getQueryParam(String query, String key) {
        if (query == null) return null;
        for (String part : query.split("&")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                String k = part.substring(0, eq);
                String v = part.substring(eq + 1);
                if (k.equals(key)) return v;
            }
        }
        return null;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int code, String body) throws IOException {
        respond(ex, code, body, "text/plain; charset=utf-8");
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int code, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(code, bytes.length);
        try (ex; OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    // All Gson: read/write the JSON array file
    private static synchronized void appendToArrayFile(JsonObject element) throws IOException {
        File parent = ScanServer.STORE.getParentFile();
        if (parent != null && !parent.exists())
            if (!parent.mkdirs())
                System.err.println("Konnte Verzeichnis nicht erstellen: " + parent.getAbsolutePath());

        JsonArray arr = readArrayFile();
        arr.add(element);

        try (Writer w = Files.newBufferedWriter(
                ScanServer.STORE.toPath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            gson.toJson(arr, w);
        }
    }

    private static JsonArray readArrayFile() throws IOException {
        if (!ScanServer.STORE.exists() || ScanServer.STORE.length() == 0) return new JsonArray();

        try (Reader r = Files.newBufferedReader(ScanServer.STORE.toPath(), StandardCharsets.UTF_8)) {
            JsonElement el = JsonParser.parseReader(r);
            if (el == null || el.isJsonNull()) return new JsonArray();
            if (!el.isJsonArray()) return new JsonArray(); // or throw
            return el.getAsJsonArray();
        } catch (JsonParseException e) {
            // corrupted file -> return empty (or throw to detect problem)
            return new JsonArray();
        }
    }
}