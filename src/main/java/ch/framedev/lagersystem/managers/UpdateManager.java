package ch.framedev.lagersystem.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import ch.framedev.lagersystem.main.Main;

/**
 * Manages application updates by checking GitHub releases.
 * Features:
 * - Checks for new versions from GitHub releases
 * - Compares version strings intelligently
 * - Supports authenticated requests for higher rate limits
 * - Provides JAR download URLs
 * - Thread-safe singleton pattern
 */
public final class UpdateManager {

    private static final Logger logger = LogManager.getLogger(UpdateManager.class);
    private static final String GITHUB_API_BASE = "https://api.github.com/repos/frame-dev/VeboLagerSystem";
    private static final String GITHUB_RELEASES_URL = GITHUB_API_BASE + "/releases/latest";
    private static final String GITHUB_ALL_RELEASES_URL = GITHUB_API_BASE + "/releases";
    private static final int CONNECT_TIMEOUT = 10000; // 10 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds

    private static volatile UpdateManager instance;
    private static final Object lock = new Object();

    private String personalToken;
    private String cachedLatestVersion;
    private long lastCheckTime = 0;
    private static final long CACHE_DURATION = TimeUnit.MINUTES.toMillis(5); // Cache for 5 minutes

    /**
     * Enum representing different release channels/branches
     */
    public enum ReleaseChannel {
        STABLE,     // Production releases (e.g., v1.0.0)
        BETA,       // Beta releases (e.g., v1.0.0-beta.1)
        ALPHA,      // Alpha releases (e.g., v1.0.0-alpha.1)
        TESTING     // Testing/development versions (e.g., 0.1-TESTING)
    }

    private UpdateManager() {
        // Load token from settings if available
        String githubToken = Main.settings.getProperty("github-token");
        if (githubToken != null && !githubToken.isEmpty()) {
            if(!githubToken.equalsIgnoreCase("your_github_token_here")) {
                setPersonalToken(githubToken);
                logger.debug("GitHub token loaded from settings");
            } else {
                logger.warn("GitHub token in settings is a placeholder. Please set a valid token for authenticated API requests.");
            }
        }
    }

    /**
     * Thread-safe singleton instance retrieval
     */
    @SuppressWarnings("DoubleCheckedLocking")
    public static UpdateManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new UpdateManager();
                }
            }
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

    /**
     * Gets the latest version from GitHub releases.
     * Uses caching to avoid excessive API calls (5-minute cache).
     *
     * @return Latest version string (e.g., "v1.0.0") or null if unable to fetch
     */
    public String getLatestVersion() {
        return getLatestVersion(ReleaseChannel.STABLE);
    }

    /**
     * Gets the latest version for a specific release channel.
     * Uses caching to avoid excessive API calls (5-minute cache).
     *
     * @param channel The release channel to check (STABLE, BETA, or ALPHA)
     * @return Latest version string for the channel (e.g., "v1.0.0", "v1.0.0-beta.1") or null if unable to fetch
     */
    public String getLatestVersion(ReleaseChannel channel) {
        // For stable releases, use the /releases/latest endpoint (cached)
        if (channel == ReleaseChannel.STABLE) {
            return getLatestStableVersion();
        }

        // For beta/alpha, fetch all releases and find the latest matching one
        return getLatestVersionForChannel(channel);
    }

    /**
     * Gets the latest stable version (cached).
     */
    private String getLatestStableVersion() {
        // Check cache first
        long currentTime = System.currentTimeMillis();
        if (cachedLatestVersion != null && (currentTime - lastCheckTime) < CACHE_DURATION) {
            logger.debug("Returning cached version: {}", cachedLatestVersion);
            return cachedLatestVersion;
        }

        try {
            // Create HTTP connection to GitHub API
            URL url = URI.create(GITHUB_RELEASES_URL).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            // Check response code
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                String errorMessage = readErrorResponse(connection);
                switch (responseCode) {
                    case 404 -> {
                        logger.warn("GitHub API 404 Not Found: Repository '{}' not found or has no releases. Response: {}",
                                GITHUB_RELEASES_URL, errorMessage);
                        Main.logUtils.addLog("GitHub API 404 Not Found: Repository not found or has no releases.");
                    }
                    case 403 -> {
                        logger.warn("GitHub API rate limit exceeded (HTTP 403). Consider adding a personal token.");
                        Main.logUtils.addLog("GitHub API rate limit exceeded (HTTP 403). Consider adding a personal token.");
                    }
                    default -> {
                        logger.error("GitHub API error: HTTP {} - {}", responseCode, errorMessage);
                        Main.logUtils.addLog("GitHub API error: HTTP " + responseCode + " - " + errorMessage);
                    }
                }
                connection.disconnect();
                return cachedLatestVersion; // Return the cached version if available
            }

            // Read response
            String responseBody = readResponse(connection);
            connection.disconnect();

            // Parse JSON response
            JsonElement jsonElement = JsonParser.parseString(responseBody);
            JsonObject jsonObject = jsonElement.getAsJsonObject();

            // Extract version tag (e.g., "v1.2.3")
            if (jsonObject.has("tag_name")) {
                String version = jsonObject.get("tag_name").getAsString();
                logger.info("Latest stable version from GitHub: {}", version);
                Main.logUtils.addLog("Latest stable version from GitHub: " + version);

                // Update cache
                cachedLatestVersion = version;
                lastCheckTime = currentTime;

                return version;
            }

            logger.warn("No tag_name found in GitHub response");
            Main.logUtils.addLog("No tag_name found in GitHub response");
            return cachedLatestVersion;

        } catch (JsonSyntaxException | IOException e) {
            logger.error("Error fetching latest version from GitHub: {}", e.getMessage(), e);
            Main.logUtils.addLog("Error fetching latest version from GitHub: " + e.getMessage());
            return cachedLatestVersion; // Return cached version on error
        }
    }

    /**
     * Gets the latest version for beta, alpha, or testing channel from all releases.
     */
    private String getLatestVersionForChannel(ReleaseChannel channel) {
        try {
            URL url = URI.create(GITHUB_ALL_RELEASES_URL).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Failed to get all releases: HTTP {}", responseCode);
                Main.logUtils.addLog("Failed to get all releases: HTTP " + responseCode);
                connection.disconnect();
                return null;
            }

            String responseBody = readResponse(connection);
            connection.disconnect();

            JsonArray releasesArray = JsonParser.parseString(responseBody).getAsJsonArray();

            // Find the latest version matching the channel
            String channelPrefix;
            switch (channel) {
                case BETA -> channelPrefix = "beta";
                case ALPHA -> channelPrefix = "alpha";
                case TESTING -> channelPrefix = "testing";
                default -> {
                    logger.warn("Unsupported channel for this method: {}", channel);
                    Main.logUtils.addLog("Unsupported channel for this method: " + channel);
                    return null;
                }
            }

            for (JsonElement element : releasesArray) {
                JsonObject release = element.getAsJsonObject();
                if (release.has("tag_name")) {
                    String tag = release.get("tag_name").getAsString().toLowerCase();

                    // Check if this release matches the channel
                    if (tag.contains(channelPrefix)) {
                        logger.info("Latest {} version from GitHub: {}", channel, release.get("tag_name").getAsString());
                        Main.logUtils.addLog(String.format("Latest version from GitHub: %s", release.get("tag_name").getAsString()));
                        return release.get("tag_name").getAsString();
                    }
                }
            }

            logger.info("No {} releases found", channel);
            Main.logUtils.addLog(String.format("No %s releases found", channel));
            return null;

        } catch (JsonSyntaxException | IOException e) {
            logger.error("Error fetching {} version: {}", channel, e.getMessage(), e);
            Main.logUtils.addLog(String.format("Error fetching %s version: %s", channel, e.getMessage()));
            return null;
        }
    }

    /**
     * Reads the response body from an HTTP connection.
     */
    private String readResponse(HttpURLConnection connection) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }

    /**
     * Creates and configures an HTTP connection for GitHub API requests.
     */
    private HttpURLConnection getHttpURLConnection(URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // Configure the request
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT);
        connection.setReadTimeout(READ_TIMEOUT);
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
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } catch (Exception e) {
            Main.logUtils.addLog("Could not read error response: " + e.getMessage());
            return "Could not read error response";
        }
    }

    /**
     * Verifies that the repository exists and is accessible.
     * Returns true if the repository is public and accessible.
     */
    public boolean verifyRepository() {
        try {
            URL url = URI.create(GITHUB_API_BASE).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            int responseCode = connection.getResponseCode();
            connection.disconnect();

            if (responseCode == 200) {
                logger.info("Repository verified successfully");
                Main.logUtils.addLog("Repository verified successfully");
                return true;
            } else {
                logger.warn("Repository verification failed: HTTP {}", responseCode);
                Main.logUtils.addLog(String.format("Repository verification failed: HTTP %d", responseCode));
                return false;
            }
        } catch (IOException e) {
            logger.error("Error verifying repository: {}", e.getMessage());
            Main.logUtils.addLog("Error verifying repository: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clears the cached version information.
     * Forces the next call to getLatestVersion() to fetch fresh data from GitHub.
     */
    public void clearCache() {
        cachedLatestVersion = null;
        lastCheckTime = 0;
        logger.debug("Version cache cleared");
    }

    /**
     * Gets the release notes for the latest version.
     *
     * @return Release notes as a string, or null if unable to fetch
     */
    public String getLatestReleaseNotes() {
        try {
            URL url = URI.create(GITHUB_RELEASES_URL).toURL();
            HttpURLConnection connection = getHttpURLConnection(url);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.warn("Failed to get release notes: HTTP {}", responseCode);
                Main.logUtils.addLog(String.format("Failed to get release notes: HTTP %d", responseCode));
                connection.disconnect();
                return null;
            }

            String responseBody = readResponse(connection);
            connection.disconnect();

            JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();

            if (jsonObject.has("body")) {
                String notes = jsonObject.get("body").getAsString();
                logger.debug("Release notes retrieved successfully");
                Main.logUtils.addLog(String.format("Release notes retrieved successfully: %s", notes));
                return notes;
            }

            logger.warn("No release notes found");
            Main.logUtils.addLog("No release notes found");
            return null;

        } catch (JsonSyntaxException | IOException e) {
            logger.error("Error fetching release notes: {}", e.getMessage(), e);
            Main.logUtils.addLog(String.format("Error fetching release notes: %s", e.getMessage()));
            return null;
        }
    }

    /**
     * Gets detailed information about the latest release.
     *
     * @return ReleaseInfo object with version, notes, and download URL, or null if unable to fetch
     */
    public ReleaseInfo getLatestReleaseInfo() {
        try {
            JsonObject jsonObject = getJsonObject();
            if (jsonObject == null) return null;

            String version = jsonObject.has("tag_name") ? jsonObject.get("tag_name").getAsString() : null;
            String notes = jsonObject.has("body") ? jsonObject.get("body").getAsString() : null;
            String downloadUrl = null;

            if (jsonObject.has("assets")) {
                JsonArray assetsArray = jsonObject.getAsJsonArray("assets");
                if (!assetsArray.isEmpty()) {
                    JsonObject firstAsset = assetsArray.get(0).getAsJsonObject();
                    downloadUrl = firstAsset.get("browser_download_url").getAsString();
                }
            }

            if (version != null) {
                logger.debug("Release info retrieved: version={}, hasNotes={}, hasDownload={}",
                        version, notes != null, downloadUrl != null);
                return new ReleaseInfo(version, notes, downloadUrl);
            }

            return null;

        } catch (IOException e) {
            logger.error("Error fetching release info: {}", e.getMessage(), e);
            Main.logUtils.addLog(String.format("Error fetching release info: %s", e.getMessage()));
            return null;
        }
    }

    private JsonObject getJsonObject() throws IOException {
        URL url = URI.create(GITHUB_RELEASES_URL).toURL();
        HttpURLConnection connection = getHttpURLConnection(url);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            getWarmMessageReleaseInfo(responseCode);
            Main.logUtils.addLog(String.format("Failed to get release info: HTTP %d", responseCode));
            connection.disconnect();
            return null;
        }

        String responseBody = readResponse(connection);
        connection.disconnect();

        return JsonParser.parseString(responseBody).getAsJsonObject();
    }

    /**
     * Container class for release information.
     */
    public record ReleaseInfo(String version, String releaseNotes, String downloadUrl) {

        public boolean hasDownloadUrl() {
            return downloadUrl != null && !downloadUrl.isEmpty();
        }

        @Override
        public String toString() {
            return String.format("ReleaseInfo{version='%s', hasNotes=%s, hasDownload=%s}",
                    version, releaseNotes != null, hasDownloadUrl());
        }
    }

    /**
     * Checks if a newer version is available compared to the current version.
     * Uses Main.VERSION as the current version.
     *
     * @return true if a newer version is available, false otherwise
     */
    public boolean isUpdateAvailable() {
        return isUpdateAvailable(Main.VERSION);
    }

    /**
     * Checks if a newer version is available compared to the specified version.
     *
     * @param currentVersion The current application version (e.g., "0.1-TESTING" or "v1.0.0")
     * @return true if a newer version is available, false otherwise
     */
    public boolean isUpdateAvailable(String currentVersion) {
        return isUpdateAvailable(currentVersion, getLatestVersion());
    }

    /**
     * Checks if a newer version is available compared to the specified version and
     * release tag.
     *
     * @param currentVersion The current application version
     * @param latestVersion The latest version to compare against
     * @return true if a newer version is available, false otherwise
     */
    public boolean isUpdateAvailable(String currentVersion, String latestVersion) {

        if (latestVersion == null) {
            logger.debug("No latest version available from GitHub");
            Main.logUtils.addLog("No latest version available from GitHub");
            return false;
        }

        // Normalize versions for comparison - remove all 'v' or 'V' characters
        String current = normalizeVersion(currentVersion);
        String latest = normalizeVersion(latestVersion);

        try {
            int comparison = compareVersions(current, latest);
            boolean updateAvailable = comparison < 0;

            if (updateAvailable) {
                logger.info("Update available! Current: {} (normalized: {}), Latest: {} (normalized: {}), comparison: {}",
                        currentVersion, current, latestVersion, latest, comparison);
                Main.logUtils.addLog(String.format("Update available! Current: %s, Latest: %s", currentVersion, latestVersion));
            } else if (comparison == 0) {
                logger.debug("Application is up to date. Current: {}, Latest: {} (versions are equal)",
                        currentVersion, latestVersion);
                Main.logUtils.addLog(String.format("Application is up to date! Current: %s, Latest: %s", currentVersion, latestVersion));
            } else {
                logger.debug("Current version is newer than latest release. Current: {}, Latest: {} (comparison: {})",
                        currentVersion, latestVersion, comparison);
                Main.logUtils.addLog(String.format("Current version is newer than latest release. Current: %s, Latest: %s", currentVersion, latestVersion));
            }
            return updateAvailable;
        } catch (Exception e) {
            logger.warn("Could not compare versions: {} vs {}", currentVersion, latestVersion, e);
            Main.logUtils.addLog(String.format("Could not compare versions: %s vs %s", currentVersion, latestVersion));
            return false;
        }
    }

    /**
     * Normalizes a version string by removing all 'v' or 'V' characters and converting to lowercase.
     *
     * @param version Version string to normalize
     * @return Normalized version string
     */
    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }
        return version.toLowerCase().replaceAll("[vV]", "");
    }

    /**
     * Detects the release channel from a version string.
     *
     * @param version Version string (e.g., "v1.0.0", "v1.0.0-beta.1", "0.2-TESTING")
     * @return Detected ReleaseChannel
     */
    public static ReleaseChannel detectChannel(String version) {
        if (version == null) {
            return ReleaseChannel.STABLE;
        }

        String lower = version.toLowerCase();

        if (lower.contains("alpha")) {
            return ReleaseChannel.ALPHA;
        } else if (lower.contains("beta")) {
            return ReleaseChannel.BETA;
        } else if (lower.contains("testing")) {
            return ReleaseChannel.TESTING;
        } else {
            return ReleaseChannel.STABLE;
        }
    }

    /**
     * Checks for updates in all channels and returns the best available update.
     *
     * @return ChannelUpdateResult with information about all available updates
     */
    public ChannelUpdateResult checkAllChannels() {
        String currentVersion = Main.VERSION;
        ReleaseChannel currentChannel = detectChannel(currentVersion);

        String stableVersion = getLatestVersion(ReleaseChannel.STABLE);
        String betaVersion = getLatestVersion(ReleaseChannel.BETA);
        String alphaVersion = getLatestVersion(ReleaseChannel.ALPHA);
        String testingVersion = getLatestVersion(ReleaseChannel.TESTING);

        return new ChannelUpdateResult(
                currentVersion,
                currentChannel,
                stableVersion,
                betaVersion,
                alphaVersion,
                testingVersion
        );
    }

    /**
     * Result of checking all release channels
     */
    public static record ChannelUpdateResult(String currentVersion, ReleaseChannel currentChannel, String stableVersion,
                                      String betaVersion, String alphaVersion, String testingVersion) {

        public boolean hasStableUpdate() {
            return stableVersion != null && isNewer(currentVersion, stableVersion);
        }

        public boolean hasBetaUpdate() {
            return betaVersion != null && isNewer(currentVersion, betaVersion);
        }

        public boolean hasAlphaUpdate() {
            return alphaVersion != null && isNewer(currentVersion, alphaVersion);
        }

        public boolean hasTestingUpdate() {
            return testingVersion != null && isNewer(currentVersion, testingVersion);
        }

        private boolean isNewer(String current, String latest) {
            try {
                UpdateManager manager = UpdateManager.getInstance();
                String c = manager.normalizeVersion(current);
                String l = manager.normalizeVersion(latest);

                return manager.compareVersions(c, l) < 0;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public String toString() {
            return String.format("ChannelUpdateResult{current='%s' (%s), stable='%s', beta='%s', alpha='%s', testing='%s'}",
                    currentVersion, currentChannel, stableVersion, betaVersion, alphaVersion, testingVersion);
        }
    }

    /**
     * Gets a detailed comparison between the current version and the latest version.
     *
     * @return A VersionComparisonResult with details, or null if unable to fetch latest version
     */
    public VersionComparisonResult compareWithLatest() {
        String currentVersion = Main.VERSION;
        String latestVersion = getLatestVersion();

        if (latestVersion == null) {
            return null;
        }

        String current = normalizeVersion(currentVersion);
        String latest = normalizeVersion(latestVersion);

        int comparison = compareVersions(current, latest);

        return new VersionComparisonResult(
                currentVersion,
                latestVersion,
                comparison < 0,  // isUpdateAvailable
                comparison == 0, // isCurrent
                comparison > 0   // isNewer
        );
    }

    /**
     * Result of version comparison
     */
    public static record VersionComparisonResult(String currentVersion, String latestVersion, boolean updateAvailable,
                                          boolean isCurrent, boolean isNewer) {

        @Override
        public String toString() {
            String status;
            if (updateAvailable) {
                status = "Update available";
            } else if (isCurrent) {
                status = "Up to date";
            } else {
                status = "Development version";
            }
            return String.format("VersionComparison{current='%s', latest='%s', status='%s'}",
                    currentVersion, latestVersion, status);
        }
    }


    /**
     * Compares two version strings.
     * Returns: negative if v1 < v2, 0 if equal, positive if v1 > v2
     * <p>
     * Examples:
     * - "0.2-testing" vs "0.2" -> -1 (testing is older)
     * - "0.2" vs "1.0" -> -1 (0.2 is older)
     * - "1.0" vs "1.0" -> 0 (equal)
     */
    private int compareVersions(String v1, String v2) {
        logger.debug("Comparing versions: '{}' vs '{}'", v1, v2);

        String[] parts1 = v1.split("[.-]");
        String[] parts2 = v2.split("[.-]");

        logger.debug("Version 1 parts: {}", java.util.Arrays.toString(parts1));
        logger.debug("Version 2 parts: {}", java.util.Arrays.toString(parts2));

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int num1 = (i < parts1.length) ? parseVersionPart(parts1[i]) : 0;
            int num2 = (i < parts2.length) ? parseVersionPart(parts2[i]) : 0;

            logger.debug("Comparing part {}: {} ({}) vs {} ({})",
                    i,
                    (i < parts1.length ? parts1[i] : "missing"), num1,
                    (i < parts2.length ? parts2[i] : "missing"), num2);

            if (num1 != num2) {
                int result = Integer.compare(num1, num2);
                logger.debug("Versions differ at position {}: result = {}", i, result);
                return result;
            }
        }

        logger.debug("Versions are equal");
        return 0;
    }

    /**
     * Parse a version part to integer, handling both numbers and text.
     * For text parts like "TESTING", "SNAPSHOT", etc., we treat them as pre-release versions
     * by assigning them a negative value, so they're always considered older than numeric versions.
     * <p>
     * Examples:
     * - "2" -> 2
     * - "testing" -> -1
     * - "beta" -> -1
     * - "alpha" -> -1
     * - "rc" -> -1
     */
    private int parseVersionPart(String part) {
        try {
            int result = Integer.parseInt(part);
            logger.debug("Parsed '{}' as numeric: {}", part, result);
            return result;
        } catch (NumberFormatException e) {
            String lowerPart = part.toLowerCase();

            // For known pre-release identifiers, treat as pre-release (negative value)
            // This ensures "0.2-TESTING" < "0.2" < "1.0"
            if (lowerPart.equals("testing") ||
                    lowerPart.equals("snapshot") ||
                    lowerPart.equals("alpha") ||
                    lowerPart.equals("beta") ||
                    lowerPart.equals("rc")) {
                logger.debug("Parsed '{}' as pre-release: -1", part);
                return -1;
            }

            // For other text, use a small positive hash
            int hash = Math.abs(part.hashCode() % 100);
            logger.debug("Parsed '{}' as hash: {}", part, hash);
            return hash;
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
            JsonObject jsonObject = getJsonObject();
            if (jsonObject == null) return null;

            // Check if the assets array exists and has files
            if (jsonObject.has("assets")) {
                JsonArray assetsArray = jsonObject.getAsJsonArray("assets");
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

        } catch (IOException e) {
            logger.error("Error getting JAR download URL: {}", e.getMessage(), e);
            Main.logUtils.addLog(String.format("Error getting JAR download URL: %s", e.getMessage()));
            return null;
        }
    }

    private static void getWarmMessageReleaseInfo(int responseCode) {
        logger.warn("Failed to get release info: HTTP {}", responseCode);
    }

    /**
     * Test method to verify version comparison logic.
     * Logs comparison results for debugging purposes.
     *
     * @param v1 First version string
     * @param v2 Second version string
     * @return Negative or positive integer based on comparison, 0 if equal
     */
    public int testVersionComparison(String v1, String v2) {
        logger.info("=== Testing Version Comparison ===");
        logger.info("Input: '{}' vs '{}'", v1, v2);

        String n1 = normalizeVersion(v1);
        String n2 = normalizeVersion(v2);
        logger.info("Normalized: '{}' vs '{}'", n1, n2);

        int result = compareVersions(n1, n2);
        logger.info("Result: {}", result);

        String interpretation;
        if (result < 0) {
            interpretation = String.format("'%s' is OLDER than '%s'", v1, v2);
        } else if (result > 0) {
            interpretation = String.format("'%s' is NEWER than '%s'", v1, v2);
        } else {
            interpretation = String.format("'%s' EQUALS '%s'", v1, v2);
        }
        logger.info(interpretation);
        logger.info("=================================");

        return result;
    }
}
