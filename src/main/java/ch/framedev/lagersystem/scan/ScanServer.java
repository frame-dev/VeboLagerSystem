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

    // Single JSON file containing an array: [ {ts,data}, ... ]
    private static final File STORE = new File(Main.getAppDataDir(), "scans.json");
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();

    public static void main(String[] args) throws Exception {
        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/scan", exchange -> {
            String text = """
                    <!doctype html>
                    <html lang="de">
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width,initial-scale=1">
                        <title>Scan abgeschlossen</title>
                        <style>
                            :root{--accent:#10b981;--muted:#94a3b8}
                            *{box-sizing:border-box}
                            html,body{height:100%;margin:0;font-family:Inter,system-ui,-apple-system,Segoe UI,Roboto,"Helvetica Neue",Arial;color:#e6eef8;background:linear-gradient(180deg,#071028 0%,#0b1220 100%)}
                            .container{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
                            .card{background:linear-gradient(180deg,rgba(255,255,255,0.03),rgba(255,255,255,0.02));border:1px solid rgba(255,255,255,0.04);padding:24px;border-radius:12px;display:flex;align-items:center;gap:18px;max-width:640px;width:100%;box-shadow:0 10px 30px rgba(2,6,23,0.6)}
                            .icon{width:64px;height:64px;flex:0 0 64px;display:grid;place-items:center;border-radius:12px;background:linear-gradient(135deg,rgba(16,185,129,0.12),rgba(16,185,129,0.06));color:var(--accent)}
                            h1{margin:0;font-size:1.25rem}
                            p{margin:6px 0 0;color:var(--muted)}
                            .actions{margin-left:auto;display:flex;gap:8px}
                            a.btn{display:inline-block;padding:8px 14px;border-radius:8px;text-decoration:none;color:inherit;font-weight:600;border:1px solid rgba(255,255,255,0.04);background:transparent}
                            a.btn.primary{background:linear-gradient(90deg,var(--accent),#06b6d4);color:#07202a;border:none}
                            .small{font-size:0.9rem;color:var(--muted);margin-top:6px}
                            @media(max-width:520px){.card{flex-direction:column;align-items:flex-start}.actions{margin-left:0;width:100%;justify-content:space-between}}
                        </style>
                    </head>
                    <body>
                    <div class="container">
                        <div class="card" role="status" aria-live="polite">
                            <div class="icon" aria-hidden="true">
                                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
                                     aria-hidden="true">
                                    <path d="M20 6L9 17l-5-5" stroke="currentColor" stroke-width="2" stroke-linecap="round"
                                          stroke-linejoin="round"/>
                                </svg>
                            </div>
                            <div>
                                <h1>Bitte gebe die Menge an:</h1>
                                <form>
                                    <label for="Eigenbedarf">Eigenbedarf:</label>
                                    <input type="checkbox" name="Eigenbedarf" id="ownUse">
                                    <label for="quantity">Menge:</label>
                                    <input type="number" id="quantity" name="Menge" min="1" required>
                                    <button type="submit">Absenden</button>
                                </form>
                            </div>
                            <div class="actions" aria-hidden="false">
                                <a class="btn" href="/latest">Letzter Scan</a>
                                <a class="btn primary" href="/list">Alle Scans</a>
                            </div>
                        </div>
                    </div>
                    <script>
                        document.getElementById('time').textContent = new Date().toLocaleString('de-DE');
                        // Optional: attempt to close the window after a short delay
                        setTimeout(function(){ try { window.close(); } catch(e){} }, 8000);
                    </script>
                    </body>
                    </html>""";

            respond(exchange, 200, text, "text/html; charset=utf-8");
            String query = exchange.getRequestURI().getRawQuery();
            String data = getQueryParam(query, "quantity");
            String ownUse = getQueryParam(query, "ownUse");
            if( ownUse != null ) {
                int quantity = 1;
                if (data != null) {
                    try {
                        quantity = Integer.parseInt(data);
                    } catch (NumberFormatException ignored) {
                    }
                }
                data = getQueryParam(query, "data");

                if (data == null || data.isBlank()) {
                    respond(exchange, 400, "Missing data");
                    return;
                }

                String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);

                JsonObject obj = new JsonObject();
                obj.addProperty("ts", Instant.now().toString());
                obj.addProperty("data", decoded);
                obj.addProperty("quantity", quantity);

                appendToArrayFile(STORE, obj);

                System.out.println("Received: " + decoded + " from " + NetUtils.getClientIp(exchange));

            } else {
                respond(exchange, 400, "Missing ownUse parameter");
            }
        });

        server.createContext("/list", exchange -> {
            JsonArray arr = readArrayFile(STORE);
            // pretty optional; remove setPrettyPrinting() if you prefer compact
            String body = gson.toJson(arr) + "\n";
            respond(exchange, 200, body, "application/json; charset=utf-8");
        });

        server.createContext("/latest", exchange -> {
            JsonArray arr = readArrayFile(STORE);
            if (arr.size() == 0) {
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
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        } finally {
            ex.close();
        }
    }

    // All Gson: read/write the JSON array file
    private static synchronized void appendToArrayFile(File file, JsonObject element) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();

        JsonArray arr = readArrayFile(file);
        arr.add(element);

        try (Writer w = Files.newBufferedWriter(
                file.toPath(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            gson.toJson(arr, w);
        }
    }

    private static JsonArray readArrayFile(File file) throws IOException {
        if (!file.exists() || file.length() == 0) return new JsonArray();

        try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
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