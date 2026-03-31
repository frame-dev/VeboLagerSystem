package ch.framedev.lagersystem.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Settings} – comment-preserving properties handler.
 */
class SettingsTest {

    @TempDir
    Path tempDir;

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private File writeFile(String content) throws IOException {
        Path p = tempDir.resolve("test.properties");
        Files.writeString(p, content, StandardCharsets.UTF_8);
        return p.toFile();
    }

    private String readFile(File f) throws IOException {
        return Files.readString(f.toPath(), StandardCharsets.UTF_8);
    }

    // -------------------------------------------------------------------------
    // Basic load / get
    // -------------------------------------------------------------------------

    @Test
    void getProperty_returnsValue() throws IOException {
        File f = writeFile("key=value\n");
        Settings s = new Settings(f);
        assertEquals("value", s.getProperty("key"));
    }

    @Test
    void getProperty_unknownKey_returnsNull() throws IOException {
        File f = writeFile("key=value\n");
        Settings s = new Settings(f);
        assertNull(s.getProperty("missing"));
    }

    @Test
    void getProperty_withDefault_usesDefault() throws IOException {
        File f = writeFile("key=value\n");
        Settings s = new Settings(f);
        assertEquals("fallback", s.getProperty("missing", "fallback"));
    }

    @Test
    void getProperty_withDefault_usesActualValue() throws IOException {
        File f = writeFile("key=hello\n");
        Settings s = new Settings(f);
        assertEquals("hello", s.getProperty("key", "fallback"));
    }

    @Test
    void contains_existingKey_returnsTrue() throws IOException {
        File f = writeFile("key=value\n");
        Settings s = new Settings(f);
        assertTrue(s.contains("key"));
    }

    @Test
    void contains_missingKey_returnsFalse() throws IOException {
        File f = writeFile("key=value\n");
        Settings s = new Settings(f);
        assertFalse(s.contains("missing"));
    }

    // -------------------------------------------------------------------------
    // Comment preservation on save
    // -------------------------------------------------------------------------

    @Test
    void save_preservesLeadingComment() throws IOException {
        String original = "# This is a comment\nkey=value\n";
        File f = writeFile(original);
        Settings s = new Settings(f);
        s.save();
        String saved = readFile(f);
        assertTrue(saved.contains("# This is a comment"),
                "Lead comment must be preserved after save, got:\n" + saved);
    }

    @Test
    void save_preservesInlineComments() throws IOException {
        String original =
                "# DB settings\n" +
                "# Supported: sqlite, h2\n" +
                "database_type=h2\n" +
                "\n" +
                "# Scheduler\n" +
                "interval=30\n";
        File f = writeFile(original);
        Settings s = new Settings(f);
        s.save();
        String saved = readFile(f);
        assertTrue(saved.contains("# DB settings"), "Comment '# DB settings' must survive save");
        assertTrue(saved.contains("# Supported: sqlite, h2"), "Comment must survive save");
        assertTrue(saved.contains("# Scheduler"), "Comment '# Scheduler' must survive save");
    }

    @Test
    void save_preservesBlankLines() throws IOException {
        String original = "a=1\n\nb=2\n";
        File f = writeFile(original);
        Settings s = new Settings(f);
        s.save();
        String saved = readFile(f);
        assertTrue(saved.contains("\n\n") || saved.contains("\r\n\r\n"),
                "Blank line must be preserved, got:\n" + saved);
    }

    // -------------------------------------------------------------------------
    // setProperty – update existing key in place (comment stays)
    // -------------------------------------------------------------------------

    @Test
    void setProperty_updatesValueInPlace_commentPreserved() throws IOException {
        String original = "# DB type\ndatabase_type=sqlite\n";
        File f = writeFile(original);
        Settings s = new Settings(f);
        s.setProperty("database_type", "h2");
        s.save();
        String saved = readFile(f);
        assertTrue(saved.contains("# DB type"), "Comment must stay after update");
        assertTrue(saved.contains("database_type=h2"), "Value must be updated");
        assertFalse(saved.contains("sqlite"), "Old value must not appear");
    }

    @Test
    void setProperty_newKey_appendedAtEnd() throws IOException {
        File f = writeFile("existing=yes\n");
        Settings s = new Settings(f);
        s.setProperty("newKey", "newVal");
        s.save();
        String saved = readFile(f);
        assertTrue(saved.contains("newKey=newVal"), "New key must be written");
        assertTrue(saved.contains("existing=yes"), "Existing key must remain");
    }

    // -------------------------------------------------------------------------
    // Round-trip: load → modify → save → reload
    // -------------------------------------------------------------------------

    @Test
    void roundTrip_commentsAndValuesPreservedAfterReload() throws IOException {
        String original =
                "# Application settings\n" +
                "load-from-files=true\n" +
                "# Supported values: sqlite, h2, json, yaml\n" +
                "database_type=h2\n" +
                "\n" +
                "# Scheduler Settings\n" +
                "# Interval for stock level checking (in minutes)\n" +
                "stock_check_interval=30\n";
        File f = writeFile(original);
        Settings s = new Settings(f);

        // Modify one value
        s.setProperty("database_type", "sqlite");
        s.save();

        // Reload from file
        Settings s2 = new Settings(f);
        assertEquals("sqlite", s2.getProperty("database_type"));
        assertEquals("true", s2.getProperty("load-from-files"));
        assertEquals("30", s2.getProperty("stock_check_interval"));

        // Comments must still be in the file
        String saved = readFile(f);
        assertTrue(saved.contains("# Application settings"));
        assertTrue(saved.contains("# Supported values: sqlite, h2, json, yaml"));
        assertTrue(saved.contains("# Scheduler Settings"));
        assertTrue(saved.contains("# Interval for stock level checking (in minutes)"));
    }

    @Test
    void roundTrip_multipleModificationsPreserveAllComments() throws IOException {
        String original =
                "# First section\n" +
                "a=1\n" +
                "\n" +
                "# Second section\n" +
                "b=2\n";
        File f = writeFile(original);
        Settings s = new Settings(f);
        s.setProperty("a", "10");
        s.setProperty("b", "20");
        s.setProperty("c", "30");
        s.save();

        Settings s2 = new Settings(f);
        assertEquals("10", s2.getProperty("a"));
        assertEquals("20", s2.getProperty("b"));
        assertEquals("30", s2.getProperty("c"));

        String saved = readFile(f);
        assertTrue(saved.contains("# First section"));
        assertTrue(saved.contains("# Second section"));
    }

    // -------------------------------------------------------------------------
    // remove
    // -------------------------------------------------------------------------

    @Test
    void remove_keyNoLongerExists() throws IOException {
        File f = writeFile("keep=yes\nremove=me\n");
        Settings s = new Settings(f);
        s.remove("remove");
        assertFalse(s.contains("remove"));
        assertTrue(s.contains("keep"));
    }

    @Test
    void remove_unknownKey_noException() throws IOException {
        File f = writeFile("key=val\n");
        Settings s = new Settings(f);
        assertDoesNotThrow(() -> s.remove("ghost"));
    }

    // -------------------------------------------------------------------------
    // keySet
    // -------------------------------------------------------------------------

    @Test
    void keySet_containsAllKeys() throws IOException {
        File f = writeFile("a=1\nb=2\nc=3\n");
        Settings s = new Settings(f);
        assertTrue(s.keySet().containsAll(List.of("a", "b", "c")));
        assertEquals(3, s.keySet().size());
    }

    // -------------------------------------------------------------------------
    // Empty / non-existent file
    // -------------------------------------------------------------------------

    @Test
    void nonExistentFile_startsEmpty() {
        File f = tempDir.resolve("does-not-exist.properties").toFile();
        Settings s = new Settings(f);
        assertTrue(s.keySet().isEmpty());
    }

    @Test
    void emptyFile_startsEmpty() throws IOException {
        File f = writeFile("");
        Settings s = new Settings(f);
        assertTrue(s.keySet().isEmpty());
    }

    @Test
    void emptyFile_setAndSave_works() throws IOException {
        File f = writeFile("");
        Settings s = new Settings(f);
        s.setProperty("hello", "world");
        s.save();
        Settings s2 = new Settings(f);
        assertEquals("world", s2.getProperty("hello"));
    }

    // -------------------------------------------------------------------------
    // Empty file → classpath fallback (3-arg constructor)
    // -------------------------------------------------------------------------

    @Test
    void emptyFile_withClasspathFallback_usesClasspath() throws IOException {
        // An empty file (0 bytes) must be treated as non-existent so that the
        // classpath template (with comments + default values) is used instead.
        File emptyFile = writeFile("");
        // "settings.properties" on the classpath contains database_type=h2
        Settings s = new Settings("settings.properties", getClass(), emptyFile);
        assertEquals("h2", s.getProperty("database_type"),
                "Empty file must fall back to classpath resource");
    }

    @Test
    void emptyFile_withClasspathFallback_savePreservesComments() throws IOException {
        File emptyFile = writeFile("");
        Settings s = new Settings("settings.properties", getClass(), emptyFile);
        // Add a new key and save – classpath comments must end up in the file
        s.setProperty("first-time", "false");
        s.save();

        String saved = readFile(emptyFile);
        assertTrue(saved.contains("database_type=h2"),     "Classpath value must be written");
        assertTrue(saved.contains("first-time=false"),     "New key must be appended");
        assertTrue(saved.contains("# Supported values:"),  "Classpath comment must be preserved");
    }

    // -------------------------------------------------------------------------
    // Classpath fallback constructor
    // -------------------------------------------------------------------------

    @Test
    void classpathFallback_loadsDefaultResource() {
        File nonExistent = tempDir.resolve("missing.properties").toFile();
        // "settings.properties" exists on the classpath in src/main/resources
        Settings s = new Settings("settings.properties", getClass(), nonExistent);
        // The classpath resource contains "database_type=h2"
        assertEquals("h2", s.getProperty("database_type"),
                "Classpath fallback must load default settings.properties");
    }

    @Test
    void classpathFallback_saveWritesToFile() {
        File target = tempDir.resolve("new-settings.properties").toFile();
        Settings s = new Settings("settings.properties", getClass(), target);
        s.setProperty("myKey", "myVal");
        s.save();
        assertTrue(target.exists(), "save() must create the file");
        Settings s2 = new Settings(target);
        assertEquals("myVal", s2.getProperty("myKey"));
    }

    // -------------------------------------------------------------------------
    // Separator styles
    // -------------------------------------------------------------------------

    @Test
    void colonSeparator_parsed() throws IOException {
        File f = writeFile("key:value\n");
        Settings s = new Settings(f);
        assertEquals("value", s.getProperty("key"));
    }

    @Test
    void spaceSeparator_parsed() throws IOException {
        File f = writeFile("key value\n");
        Settings s = new Settings(f);
        assertEquals("value", s.getProperty("key"));
    }

    // -------------------------------------------------------------------------
    // Escape sequences
    // -------------------------------------------------------------------------

    @Test
    void getProperty_unescapesNewline() throws IOException {
        File f = writeFile("msg=line1\\nline2\n");
        Settings s = new Settings(f);
        assertEquals("line1\nline2", s.getProperty("msg"));
    }

    @Test
    void setProperty_escapesNewlineOnSave() throws IOException {
        File f = writeFile("");
        Settings s = new Settings(f);
        s.setProperty("msg", "line1\nline2");
        s.save();
        String raw = readFile(f);
        assertTrue(raw.contains("\\n"), "Newline in value must be escaped in file");
    }

    // -------------------------------------------------------------------------
    // toString
    // -------------------------------------------------------------------------

    @Test
    void toString_containsKeyValuePairs() throws IOException {
        File f = writeFile("a=1\nb=2\n");
        Settings s = new Settings(f);
        String str = s.toString();
        assertTrue(str.contains("a=1"));
        assertTrue(str.contains("b=2"));
    }
}

