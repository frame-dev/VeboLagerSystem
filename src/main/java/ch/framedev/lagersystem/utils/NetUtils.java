package ch.framedev.lagersystem.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;

public final class NetUtils {

    private NetUtils() {
    }

    // 1) Client-IP aus HttpExchange (prüft X-Forwarded-For / X-Real-IP / Forwarded)
    public static String getClientIp(HttpExchange exchange) {
        if (exchange == null) return null;
        String[] headersToCheck = {"X-Forwarded-For", "X-Real-IP", "Forwarded"};
        for (String h : headersToCheck) {
            String val = exchange.getRequestHeaders().getFirst(h);
            if (val != null && !val.isBlank()) {
                if ("Forwarded".equalsIgnoreCase(h)) {
                    // Forwarded: for=192.0.2.60;proto=http;by=203.0.113.43
                    int idx = val.indexOf("for=");
                    if (idx >= 0) {
                        String part = val.substring(idx + 4).split("[; ,]")[0];
                        return stripQuotes(part);
                    }
                } else {
                    // X-Forwarded-For can contain comma separated list -> first is original client
                    return stripQuotes(val.split(",")[0].trim());
                }
            }
        }
        InetSocketAddress remote = exchange.getRemoteAddress();
        if (remote != null && remote.getAddress() != null) {
            return remote.getAddress().getHostAddress();
        }
        return null;
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    // 2) Erste erreichbare nicht-loopback IPv4-Adresse des Hosts
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface nif = ifs.nextElement();
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) continue;
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                        return a.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        // Fallback
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return null;
        }
    }

    // 3) Public/external IP via external service (kann fehlschlagen wenn kein Internet)
    public static String getPublicIp() {
        String service = "https://api.ipify.org";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(service).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String ip = br.readLine();
                return (ip == null || ip.isBlank()) ? null : ip.trim();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
