package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.main.Main;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class UpdateManager {

    private static final Logger logger = LogManager.getLogger(UpdateManager.class);
    private static final String GITHUB_URL = "https://api.github.com/repos/frame-dev/VeboLagerSystem/releases/latest";

    private static UpdateManager instance;
    private String personalToken;

    private UpdateManager() {
        String githubToken = Main.settings.getProperty("github-token");
        if (githubToken != null && !githubToken.isEmpty()) {
            setPersonalToken(githubToken);
        }
    }

    public static UpdateManager getInstance() {
        if (instance == null) {
            instance = new UpdateManager();
        }
        return instance;
    }

    /**
     * Sets the GitHub personal token for authenticated requests.
     * This allows for higher rate limits (5,000 requests/hour vs 60 requests/hour).
     *
     * @param token GitHub personal access token (e.g., "ghp_xxxxxxxxxxxx")
     */
    public void setPersonalToken(String token) {
        this.personalToken = token;
        if (token != null) {
            logger.info("GitHub personal token configured");
        }
    }

    public String getLatestVersion() {
        try {
            // Create HTTP connection to GitHub API
            URL url = URI.create(GITHUB_URL).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorMessage = readErrorResponse(connection);
                if (responseCode == 404) {
                    logger.warn("GitHub API 404 Not Found: Repository '{}' not found or has no releases. Response: {}",
                            GITHUB_URL, errorMessage);
                } else {
                    logger.error("GitHub API error: HTTP {} - {}", responseCode, errorMessage);
                }
                return null;
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            // Parse JSON response
            JsonElement jsonElement = JsonParser.parseString(response.toString());
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Extract version tag (e.g., "v1.2.3")
            if (jsonObject.has("tag_name")) {
                String version = jsonObject.get("tag_name").getAsString();
                logger.info("Latest version from GitHub: {}", version);
                return version;
            }

            logger.warn("No tag_name found in GitHub response");
            return null;

        } catch (Exception e) {
            logger.error("Error fetching latest version from GitHub: {}", e.getMessage(), e);
            return null;
        }
    }

    private HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Configure the request
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "VeboLagerSystem");

        // Add authorization header if personal token is set
        if (personalToken != null && !personalToken.isEmpty()) {
            connection.setRequestProperty("Authorization", "token " + personalToken);
        }
        return connection;
    }

    /**
     * Reads the error response body from an HTTP connection.
     * This is useful for getting error messages from GitHub API.
     */
    private String readErrorResponse(HttpURLConnection connection) {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8)
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } catch (Exception e) {
            return "Could not read error response";
        }
    }

    /**
     * Verifies that the repository exists and is accessible.
     * Returns true if the repository is public and accessible.
     */
    public boolean verifyRepository() {
        try {
            URL url = URI.create("https://api.github.com/repos/frame-dev/VeboLagerSystem").toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "VeboLagerSystem");

            if (personalToken != null && !personalToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "token " + personalToken);
            }

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            if (responseCode == 200) {
                logger.info("Repository verified successfully");
                return true;
            } else {
                logger.warn("Repository verification failed: HTTP {}", responseCode);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error verifying repository: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a newer version is available compared to the current version.
     * @param currentVersion The current application version (e.g., "0.1-TESTING" or "v1.0.0")
     * @return true if a newer version is available, false otherwise
     */
    public boolean isUpdateAvailable(String currentVersion) {
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            logger.debug("No latest version available from GitHub");
            return false;
        }

        // Remove 'v' prefix if present for comparison
        String current = currentVersion.toLowerCase().replaceAll("^v", "");
        String latest = latestVersion.toLowerCase().replaceAll("^v", "");

        try {
            int comparison = compareVersions(current, latest);
            boolean updateAvailable = comparison < 0;

            if (updateAvailable) {
                logger.info("Update available! Current: {}, Latest: {}", currentVersion, latestVersion);
            } else {
                logger.debug("Application is up to date. Current: {}, Latest: {}", currentVersion, latestVersion);
            }

            return updateAvailable;
        } catch (Exception e) {
            logger.warn("Could not compare versions: {} vs {}", currentVersion, latestVersion, e);
            return false;
        }
    }

    /**
     * Compares two version strings.
     * Returns: negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = (i < parts1.length) ? parseVersionPart(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? parseVersionPart(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }

        return 0;
    }

    /**
     * Parse a version part to integer, handling both numbers and text
     */
    private int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            // For text parts like "TESTING", assign a high number
            return part.hashCode() % 1000;
        }
    }

    /**
     * Gets the download URL for the latest release JAR file.
     * Returns the direct download link to the first attached JAR file.
     *
     * @return Download URL if JAR file is attached to release, null otherwise
     */
    public String getLatestReleaseJarUrl() {
        try {
            // Create HTTP connection to GitHub API
            URL url = URI.create(GITHUB_URL).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Failed to get release info: HTTP {}", responseCode);
                return null;
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
            );

            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            connection.disconnect();

            // Parse JSON response
            JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();

            // Check if assets array exists and has files
            if (jsonObject.has("assets")) {
                var assetsArray = jsonObject.getAsJsonArray("assets");
                if (!assetsArray.isEmpty()) {
                    // Get the first asset (usually the JAR file)
                    JsonObject firstAsset = assetsArray.get(0).getAsJsonObject();
                    String downloadUrl = firstAsset.get("browser_download_url").getAsString();

                    logger.info("JAR download URL found: {}", downloadUrl);
                    return downloadUrl;
                }
            }

            logger.warn("No JAR file found in release assets");
            return null;

        } catch (Exception e) {
            logger.error("Error getting JAR download URL: {}", e.getMessage(), e);
            return null;
        }
    }
}
