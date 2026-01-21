# Update Implementation Summary

## Overview
The VEBO Lagersystem now includes a comprehensive update management system that checks for new releases from GitHub across multiple release channels (Stable, Beta, Alpha, and Testing).

## Architecture

### Components

#### 1. UpdateManager (`UpdateManager.java`)
**Location**: `src/main/java/ch/framedev/lagersystem/managers/UpdateManager.java`

**Key Features**:
- Thread-safe singleton pattern
- Caching mechanism (5-minute cache duration)
- Support for multiple release channels
- GitHub API integration with authentication support
- Version comparison logic
- Release notes retrieval

**Release Channels**:
```java
public enum ReleaseChannel {
    STABLE,     // Production releases (e.g., v1.0.0)
    BETA,       // Beta releases (e.g., v1.0.0-beta.1)
    ALPHA,      // Alpha releases (e.g., v1.0.0-alpha.1)
    TESTING     // Testing/development versions (e.g., 0.2-TESTING)
}
```

**Main Methods**:
- `getLatestVersion()` - Gets latest stable version
- `getLatestVersion(ReleaseChannel)` - Gets latest version for specific channel
- `checkAllChannels()` - Checks all channels and returns results
- `isUpdateAvailable()` - Checks if update is available
- `compareWithLatest()` - Detailed version comparison
- `detectChannel(String)` - Auto-detects release channel from version string

#### 2. Main.java Integration
**Location**: `src/main/java/ch/framedev/lagersystem/main/Main.java`

**Update Check on Startup**:
```java
private static void checkForUpdates() {
    UpdateManager updateManager = UpdateManager.getInstance();
    UpdateManager.ChannelUpdateResult channelResult = updateManager.checkAllChannels();
    
    if (channelResult.hasStableUpdate() || 
        channelResult.hasBetaUpdate() || 
        channelResult.hasAlphaUpdate() || 
        channelResult.hasTestingUpdate()) {
        displayUpdateDialog(channelResult);
    }
}
```

**Features**:
- Automatic update check on application startup
- Console logging of available versions
- GUI dialog if updates are available
- Opens browser to GitHub releases page

#### 3. SettingsGUI Integration
**Location**: `src/main/java/ch/framedev/lagersystem/guis/SettingsGUI.java`

**Manual Update Checks**:
- Four buttons in the "About" tab for each release channel:
  - Stable (Blue) - Recommended for production
  - Beta (Orange) - Testing versions
  - Alpha (Red) - Experimental features
  - Testing (Purple) - Development versions

**Update Check Flow**:
1. User clicks update button for desired channel
2. Progress dialog appears
3. SwingWorker fetches version in background
4. Result dialog shows:
   - Current version
   - Latest version available
   - Channel type
   - Option to open download page

## Version Comparison Logic

### Version String Format
- **Stable**: `v1.0.0`, `v2.1.3`
- **Beta**: `v1.0.0-beta.1`, `v2.0.0-beta`
- **Alpha**: `v1.0.0-alpha.1`, `v2.0.0-alpha`
- **Testing**: `0.2-TESTING`, `0.3-TESTING`

### Comparison Algorithm
```java
private int compareVersions(String v1, String v2) {
    String[] parts1 = v1.split("[.-]");
    String[] parts2 = v2.split("[.-]");
    
    for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
        int num1 = parseVersionPart(parts1[i]);
        int num2 = parseVersionPart(parts2[i]);
        
        if (num1 != num2) {
            return Integer.compare(num1, num2);
        }
    }
    return 0;
}
```

**Special Handling**:
- Pre-release identifiers (testing, snapshot, alpha, beta, rc) return `-1`
- Ensures `0.2-TESTING` < `0.2` < `1.0`
- Text parts use hash for consistent comparison

## GitHub API Integration

### Endpoints
- **Latest Release**: `https://api.github.com/repos/frame-dev/VeboLagerSystem/releases/latest`
- **All Releases**: `https://api.github.com/repos/frame-dev/VeboLagerSystem/releases`

### Authentication
Optional personal access token for higher rate limits:
- Without token: 60 requests/hour
- With token: 5,000 requests/hour

**Configuration**:
```properties
# settings.properties
github-token=ghp_xxxxxxxxxxxx
```

### Rate Limiting
- 5-minute cache for stable releases
- No cache for beta/alpha/testing (fetched on demand)
- Returns cached version on API errors

## Update Dialog UI

### Startup Dialog (Main.java)
**Appearance**:
```html
🎉 Updates verfügbar!
Aktuelle Version: 0.2-TESTING (TESTING)
─────────────────────────
Verfügbare Versionen:
✓ Stable: v1.0.0 (empfohlen)
⚠ Beta: v1.1.0-beta.1 (testing)
⚡ Alpha: v1.2.0-alpha.1 (experimental)
🔧 Testing: 0.3-TESTING (für Entwickler)
```

**Actions**:
- "Ja" - Opens GitHub releases page
- "Nein" - Dismisses dialog

### Settings Dialog
**Individual Channel Checks**:
- Shows progress while checking
- Displays channel-specific results
- Direct link to filtered releases page

## Data Classes

### ChannelUpdateResult
```java
public static class ChannelUpdateResult {
    String currentVersion;
    ReleaseChannel currentChannel;
    String stableVersion;
    String betaVersion;
    String alphaVersion;
    String testingVersion;
    
    boolean hasStableUpdate();
    boolean hasBetaUpdate();
    boolean hasAlphaUpdate();
    boolean hasTestingUpdate();
}
```

### VersionComparisonResult
```java
public static class VersionComparisonResult {
    String currentVersion;
    String latestVersion;
    boolean updateAvailable;
    boolean isCurrent;
    boolean isNewer;
}
```

### ReleaseInfo
```java
public static class ReleaseInfo {
    String version;
    String releaseNotes;
    String downloadUrl;
    
    boolean hasDownloadUrl();
}
```

## Configuration

### Settings Properties
```properties
# Enable/disable update checks
enable_update_check=true

# GitHub personal access token (optional)
github-token=

# Update check interval (for future scheduled checks)
update_check_interval=24
```

## Error Handling

### Network Errors
- Returns cached version if available
- Displays user-friendly error messages
- Logs detailed errors for debugging

### API Errors
- **404**: Repository not found or no releases
- **403**: Rate limit exceeded (suggests adding token)
- **Other**: Generic error handling with detailed logging

### Version Parsing Errors
- Graceful degradation
- Defaults to string comparison
- Logs warnings for investigation

## Testing Checklist

### Manual Testing
- [ ] Startup update check shows dialog when updates available
- [ ] Startup update check silent when up-to-date
- [ ] Settings > About > Stable update check works
- [ ] Settings > About > Beta update check works
- [ ] Settings > About > Alpha update check works
- [ ] Settings > About > Testing update check works
- [ ] Progress dialog appears and dismisses correctly
- [ ] Download page opens in browser
- [ ] Error handling for no internet connection
- [ ] Error handling for invalid GitHub token
- [ ] Version comparison logic (0.2-TESTING < v1.0.0)

### Automated Testing
- [ ] Version comparison unit tests
- [ ] Channel detection tests
- [ ] Mock API response tests
- [ ] Cache expiration tests

## Future Enhancements

### Planned Features
1. **Automatic Downloads**: Download JAR files directly
2. **In-App Updates**: Apply updates without browser
3. **Update Notifications**: Background checks with notifications
4. **Release Notes Display**: Show changelog in-app
5. **Rollback Support**: Revert to previous version
6. **Delta Updates**: Download only changed files

### Improvements
1. **Better Caching**: Persistent cache across restarts
2. **Retry Logic**: Automatic retry on network failures
3. **Offline Mode**: Better handling when offline
4. **Update Scheduling**: Configurable check intervals
5. **Silent Mode**: Option to suppress dialogs

## Troubleshooting

### Common Issues

**Issue**: "No updates available" but new version exists
- **Solution**: Clear cache using `updateManager.clearCache()`
- **Solution**: Check GitHub releases page manually
- **Solution**: Verify network connectivity

**Issue**: API rate limit exceeded
- **Solution**: Add GitHub personal access token
- **Solution**: Wait for rate limit reset (1 hour)

**Issue**: Wrong version detected
- **Solution**: Check version string format
- **Solution**: Verify Main.VERSION constant
- **Solution**: Check release tags on GitHub

**Issue**: Update dialog doesn't appear
- **Solution**: Check console logs for errors
- **Solution**: Verify internet connection
- **Solution**: Check GitHub repository access

## Console Output Examples

### Successful Check (No Updates)
```
✓ Aktuelle Version: 0.2-TESTING (TESTING)
✓ Anwendung ist auf dem neuesten Stand
  → Stable: v1.0.0
  → Beta: v1.1.0-beta.1
  → Alpha: v1.2.0-alpha.1
  → Testing: 0.2-TESTING
```

### Update Available
```
✓ Aktuelle Version: 0.2-TESTING (TESTING)
⚠️  Update verfügbar in deinem Channel: 0.3-TESTING
  → Stable: v1.0.0
  → Beta: v1.1.0-beta.1
  → Alpha: v1.2.0-alpha.1
  → Testing: 0.3-TESTING
```

### Development Version
```
✓ Aktuelle Version: 99.0.0 (STABLE)
✓ Entwicklungsversion (neuer als letzte Release: v1.0.0)
  → Stable: v1.0.0
```

## Code Locations

### Key Files
- `Main.java:470-520` - Startup update check
- `Main.java:530-610` - Update dialog display
- `UpdateManager.java:80-250` - Version fetching logic
- `UpdateManager.java:420-580` - Channel checking
- `UpdateManager.java:650-750` - Version comparison
- `SettingsGUI.java:600-640` - Update UI buttons
- `SettingsGUI.java:1190-1350` - Update check methods

## Dependencies

### Required Libraries
- `com.google.gson` - JSON parsing for GitHub API
- Java 11+ - URI, HttpURLConnection
- Swing - GUI dialogs

### External Services
- GitHub API (api.github.com)
- GitHub Releases (github.com/frame-dev/VeboLagerSystem/releases)

## Version History

### v0.2-TESTING (Current)
- ✅ Multi-channel update checking
- ✅ Startup update detection
- ✅ Manual update checks from Settings
- ✅ GitHub API integration
- ✅ Version comparison logic
- ✅ Caching mechanism

### Future Versions
- v0.3-TESTING: Add release notes display
- v1.0.0: Stable release with auto-update
- v1.1.0: Background update checks with notifications

---

**Last Updated**: January 20, 2026  
**Maintainer**: VEBO Oensingen Development Team  
**Contact**: See application About section
