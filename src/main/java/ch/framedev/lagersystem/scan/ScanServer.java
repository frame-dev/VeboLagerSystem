package ch.framedev.lagersystem.scan;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.utils.NetUtils;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executors;

public class ScanServer {

    private static final File STORE = new File(Main.getAppDataDir(), "scans.jsonl"); // eine Zeile = ein JSON

    public static void main(String[] args) throws Exception {
        int port = 8080;

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        // 1) Scan empfangen und speichern
        server.createContext("/scan", exchange -> {
            String query = exchange.getRequestURI().getRawQuery();
            String data = getQueryParam(query, "data");

            if (data == null || data.isBlank()) {
                respond(exchange, 400, "Missing data");
                return;
            }

            String decoded = URLDecoder.decode(data, StandardCharsets.UTF_8);

            // speichern (timestamp + data)
            String json = "{\"ts\":\"" + Instant.now() + "\",\"data\":\"" + escapeJson(decoded) + "\"}";
            appendLine(STORE, json);

            System.out.println("Received: " + decoded + " from " + NetUtils.getClientIp(exchange));
            respond(exchange, 200, "Data stored and available at /latest or in Program.\n");
        });

        // 2) Alle Scans anzeigen (letzte 200)
        server.createContext("/list", exchange -> {
            List<String> lines = readLastLines(STORE, 200);
            String body = "[\n" + String.join(",\n", lines) + "\n]\n";
            respond(exchange, 200, body, "application/json; charset=utf-8");
        });

        // 3) Letzten Scan anzeigen
        server.createContext("/latest", exchange -> {
            String last = readLastLine(STORE);
            if (last == null) {
                respond(exchange, 200, "{}\n", "application/json; charset=utf-8");
            } else {
                respond(exchange, 200, last + "\n", "application/json; charset=utf-8");
            }
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
        }
        ex.close();
    }

    private static synchronized void appendLine(File file, String line) throws IOException {
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(line);
            bw.newLine();
        }
    }

    private static List<String> readLastLines(File file, int max) throws IOException {
        if (!file.exists()) return List.of();
        List<String> all = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String s;
            while ((s = br.readLine()) != null) {
                if (!s.isBlank()) all.add(s);
            }
        }
        int from = Math.max(0, all.size() - max);
        return all.subList(from, all.size());
    }

    private static String readLastLine(File file) throws IOException {
        if (!file.exists()) return null;
        String last = null;
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String s;
            while ((s = br.readLine()) != null) {
                if (!s.isBlank()) last = s;
            }
        }
        return last;
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}