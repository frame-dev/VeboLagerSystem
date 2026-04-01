package ch.framedev.lagersystem.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link LogUtils} — tests only the pure static
 * {@link LogUtils#isMainLogFile(Path)} helper which has no I/O or Swing dependencies.
 */
class LogUtilsTest {

    // =========================================================================
    // isMainLogFile(Path)
    // =========================================================================

    @Test
    @DisplayName("isMainLogFile: null path returns false")
    void isMainLogFile_null_returnsFalse() {
        assertFalse(LogUtils.isMainLogFile(null));
    }

    @Test
    @DisplayName("isMainLogFile: exact main log file name returns true")
    void isMainLogFile_exactMainLogFile_returnsTrue() {
        // "vebo_lager_system.log" is the exact non-dated main log file
        Path path = Path.of(LogUtils.MAIN_LOG_FILE_PREFIX + LogUtils.MAIN_LOG_FILE_EXTENSION);
        assertTrue(LogUtils.isMainLogFile(path));
    }

    @Test
    @DisplayName("isMainLogFile: dated log file name returns true")
    void isMainLogFile_datedLogFile_returnsTrue() {
        // e.g. "vebo_lager_system_2024-01-15.log"
        Path path = Path.of(LogUtils.MAIN_LOG_FILE_PREFIX + "_2024-01-15" + LogUtils.MAIN_LOG_FILE_EXTENSION);
        assertTrue(LogUtils.isMainLogFile(path));
    }

    @Test
    @DisplayName("isMainLogFile: today dated log file returns true")
    void isMainLogFile_todayDatedLogFile_returnsTrue() {
        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path path = Path.of(LogUtils.MAIN_LOG_FILE_PREFIX + "_" + today + LogUtils.MAIN_LOG_FILE_EXTENSION);
        assertTrue(LogUtils.isMainLogFile(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "other.log",
            "application.log",
            "vebo_lager_system2.log",     // wrong suffix (2 is not underscore)
            "vebo_lager_system_.txt",      // wrong extension
            "vebo_lager_system_2024.txt"   // wrong extension
    })
    @DisplayName("isMainLogFile: unrelated filenames return false")
    void isMainLogFile_unrelatedFiles_returnsFalse(String fileName) {
        assertFalse(LogUtils.isMainLogFile(Path.of(fileName)),
                "Expected false for file name: " + fileName);
    }

    @Test
    @DisplayName("isMainLogFile: path in subdirectory uses only the file name part")
    void isMainLogFile_inSubdirectory_usesFileNameOnly() {
        // A path like /some/dir/vebo_lager_system.log — should still be true
        Path path = Path.of("some", "dir",
                LogUtils.MAIN_LOG_FILE_PREFIX + LogUtils.MAIN_LOG_FILE_EXTENSION);
        assertTrue(LogUtils.isMainLogFile(path));
    }

    @Test
    @DisplayName("isMainLogFile: path without filename component returns false")
    void isMainLogFile_pathWithoutFileName_returnsFalse() {
        // Root path has no file-name component
        assertFalse(LogUtils.isMainLogFile(Path.of("/")));
    }

    // =========================================================================
    // Constants
    // =========================================================================

    @Test
    @DisplayName("MAIN_LOG_FILE_PREFIX is not blank")
    void mainLogFilePrefix_notBlank() {
        assertFalse(LogUtils.MAIN_LOG_FILE_PREFIX.isBlank());
    }

    @Test
    @DisplayName("MAIN_LOG_FILE_EXTENSION starts with a dot")
    void mainLogFileExtension_startWithDot() {
        assertTrue(LogUtils.MAIN_LOG_FILE_EXTENSION.startsWith("."));
    }

    @Test
    @DisplayName("DEFAULT_LOG_RETENTION_DAYS is positive")
    void defaultLogRetentionDays_isPositive() {
        assertTrue(LogUtils.DEFAULT_LOG_RETENTION_DAYS > 0);
    }
}

