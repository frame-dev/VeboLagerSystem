package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.managers.UpdateManager.ReleaseChannel;
import ch.framedev.lagersystem.managers.UpdateManager.ReleaseInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the pure-logic portions of {@link UpdateManager} that do not
 * require network access or a running database:
 * <ul>
 *   <li>{@code isUpdateAvailable(String, String)}</li>
 *   <li>{@code detectChannel(String)}</li>
 *   <li>{@link ReleaseInfo#hasDownloadUrl()}</li>
 * </ul>
 * No singleton setup is needed because {@code detectChannel} is static and
 * the two-argument {@code isUpdateAvailable} only performs string comparison.
 */
class UpdateManagerTest {

    // =========================================================================
    // detectChannel(String) — static, no side-effects
    // =========================================================================

    @Test
    @DisplayName("detectChannel: null version returns STABLE")
    void detectChannel_null_returnsStable() {
        assertEquals(ReleaseChannel.STABLE, UpdateManager.detectChannel(null));
    }

    @Test
    @DisplayName("detectChannel: plain version number returns STABLE")
    void detectChannel_plainVersion_returnsStable() {
        assertEquals(ReleaseChannel.STABLE, UpdateManager.detectChannel("v1.2.3"));
        assertEquals(ReleaseChannel.STABLE, UpdateManager.detectChannel("1.0.0"));
    }

    @Test
    @DisplayName("detectChannel: version containing 'beta' returns BETA")
    void detectChannel_betaVersion_returnsBeta() {
        assertEquals(ReleaseChannel.BETA, UpdateManager.detectChannel("v1.0.0-beta.1"));
        assertEquals(ReleaseChannel.BETA, UpdateManager.detectChannel("1.0.0-BETA"));
    }

    @Test
    @DisplayName("detectChannel: version containing 'alpha' returns ALPHA")
    void detectChannel_alphaVersion_returnsAlpha() {
        assertEquals(ReleaseChannel.ALPHA, UpdateManager.detectChannel("v2.0.0-alpha.3"));
        assertEquals(ReleaseChannel.ALPHA, UpdateManager.detectChannel("ALPHA-build"));
    }

    @Test
    @DisplayName("detectChannel: version containing 'testing' returns TESTING")
    void detectChannel_testingVersion_returnsTesting() {
        assertEquals(ReleaseChannel.TESTING, UpdateManager.detectChannel("0.2-TESTING"));
        assertEquals(ReleaseChannel.TESTING, UpdateManager.detectChannel("testing-snapshot"));
    }

    // =========================================================================
    // ReleaseInfo.hasDownloadUrl()
    // =========================================================================

    @Test
    @DisplayName("ReleaseInfo.hasDownloadUrl: non-blank URL returns true")
    void releaseInfo_hasDownloadUrl_nonBlank_returnsTrue() {
        ReleaseInfo info = new ReleaseInfo("v1.0.0", "notes", "https://example.com/app.jar");
        assertTrue(info.hasDownloadUrl());
    }

    @Test
    @DisplayName("ReleaseInfo.hasDownloadUrl: null URL returns false")
    void releaseInfo_hasDownloadUrl_null_returnsFalse() {
        ReleaseInfo info = new ReleaseInfo("v1.0.0", "notes", null);
        assertFalse(info.hasDownloadUrl());
    }

    @Test
    @DisplayName("ReleaseInfo.hasDownloadUrl: blank URL returns true (isEmpty check, not isBlank)")
    void releaseInfo_hasDownloadUrl_blank_returnsTrue() {
        ReleaseInfo info = new ReleaseInfo("v1.0.0", "notes", "  ");
        assertTrue(info.hasDownloadUrl());
    }

    // =========================================================================
    // isUpdateAvailable(String, String) — instance method, string comparison
    // =========================================================================

    @ParameterizedTest(name = "current={0}, latest={1} → update available")
    @DisplayName("isUpdateAvailable: newer latest version returns true")
    @CsvSource({
        "1.0.0, 1.0.1",
        "1.0.0, 1.1.0",
        "1.0.0, 2.0.0",
        "v1.0.0, v1.0.1",
        "v1.2.3, v2.0.0"
    })
    void isUpdateAvailable_newerLatest_returnsTrue(String current, String latest) {
        UpdateManager manager = buildUpdateManager();
        assertTrue(manager.isUpdateAvailable(current, latest));
    }

    @ParameterizedTest(name = "current={0}, latest={1} → no update")
    @DisplayName("isUpdateAvailable: same or older latest version returns false")
    @CsvSource({
        "1.0.0, 1.0.0",
        "1.1.0, 1.0.0",
        "2.0.0, 1.9.9",
        "v1.5.0, v1.4.0"
    })
    void isUpdateAvailable_sameOrOlderLatest_returnsFalse(String current, String latest) {
        UpdateManager manager = buildUpdateManager();
        assertFalse(manager.isUpdateAvailable(current, latest));
    }

    @Test
    @DisplayName("isUpdateAvailable: null latestVersion returns false")
    void isUpdateAvailable_nullLatest_returnsFalse() {
        assertFalse(buildUpdateManager().isUpdateAvailable("1.0.0", null));
    }

    @Test
    @DisplayName("isUpdateAvailable: v-prefix stripped before comparison")
    void isUpdateAvailable_vPrefix_handled() {
        UpdateManager manager = buildUpdateManager();
        // "v1.0.0" and "1.0.0" should be treated as equal → no update
        assertFalse(manager.isUpdateAvailable("v1.0.0", "1.0.0"));
        assertFalse(manager.isUpdateAvailable("1.0.0", "v1.0.0"));
        // Genuine newer release
        assertTrue(manager.isUpdateAvailable("v1.0.0", "v1.0.1"));
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * Builds an UpdateManager instance via reflection to avoid touching
     * {@code Main.settings} (which may be null in test environments).
     */
    private static UpdateManager buildUpdateManager() {
        try {
            // Reset the singleton first so we start clean
            java.lang.reflect.Field instanceField = UpdateManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);

            // Create instance bypassing the normal constructor (which reads Main.settings)
            java.lang.reflect.Constructor<UpdateManager> ctor =
                    UpdateManager.class.getDeclaredConstructor();
            ctor.setAccessible(true);

            // Temporarily stub Main.settings to prevent NPE in constructor
            ch.framedev.lagersystem.main.Main.settings =
                    new ch.framedev.lagersystem.utils.Settings(createEmptyTempFile());

            return ctor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Could not build UpdateManager for test", e);
        }
    }

    private static java.io.File createEmptyTempFile() {
        try {
            java.io.File f = java.io.File.createTempFile("update-manager-test-settings", ".properties");
            f.deleteOnExit();
            return f;
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }
}
