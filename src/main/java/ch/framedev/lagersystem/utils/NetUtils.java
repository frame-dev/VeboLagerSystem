 package ch.framedev.lagersystem.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Enumeration;

/**
 * The NetUtils class provides utility methods for network-related operations, such as checking for active network interfaces, retrieving the client's IP address from an HttpExchange object, obtaining the local IP address of the host, and fetching the public IP address using an external service. The class is designed to be a final utility class with static methods, and it includes error handling to ensure that exceptions do not propagate and instead return null or false as appropriate. The methods in this class can be used in various parts of the application where network information is needed.
 * @author framedev
 */
public final class NetUtils {

    private NetUtils() {
    }

    /**
     * Checks if the host has any active network interfaces (excluding loopback and virtual). This is a basic check to determine if the host is likely connected to a network, but it does not guarantee Internet connectivity.
     * @return true if at least one active non-loopback, non-virtual network interface is found; false otherwise.
     */
    public static boolean hasNetwork() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface nif = ifs.nextElement();
                if (nif.isUp() && !nif.isLoopback() && !nif.isVirtual()) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * Retrieves the client's IP address from the HttpExchange object. It first checks common headers that may contain the client's IP (e.g., "X-Forwarded-For", "X-Real-IP", "Forwarded") to account for cases where the server is behind a proxy. If these headers are not present or do not contain a valid IP, it falls back to using the remote address from the HttpExchange. The method also handles potential formatting issues, such as stripping quotes from header values.
     * @param exchange the HttpExchange object containing the request and connection information.
     * @return the client's IP address as a string if found; null if no valid IP address is found or if the exchange is null.
     */
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

    /**
     * Retrieves the local IP address of the host by iterating through all network interfaces and their associated IP addresses. The method checks for active, non-loopback, and non-virtual interfaces, and returns the first valid IPv4 address found. If no valid IP address is found or if an exception occurs during the process, it falls back to using InetAddress.getLocalHost() to retrieve the local IP address. If that also fails, it returns null.
     * @return the local IP address as a string if found; null if no valid IP address is found or if an error occurs.
     */
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

    /**
     * Retrieves the public IP address of the host by making an HTTP GET request to a public IP service (https://api.ipify.org). The method sets a connection and read timeout of 2000 milliseconds to prevent hanging. If the request is successful and a valid IP address is returned, it is trimmed and returned as a string. If any exception occurs during the process (e.g., network issues, service unavailability), the method returns null.
     * @return the public IP address as a string if successful; null if an error occurs or if the response is invalid.
     */
    public static String getPublicIp() {
        String service = "https://api.ipify.org";
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(service).toURL().openConnection();
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
