package ch.framedev.lagersystem.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserDataDirTest {

    @Test
    @DisplayName("getAppPath: returns an existing directory ending with the app name")
    void getAppPath_returnsExistingDirectory() throws FileNotFoundException {
        Path path = UserDataDir.getAppPath("vebo-lagersystem-test");

        assertEquals("vebo-lagersystem-test", path.getFileName().toString());
        assertTrue(path.toFile().exists());
        assertTrue(path.toFile().isDirectory());
    }
}
