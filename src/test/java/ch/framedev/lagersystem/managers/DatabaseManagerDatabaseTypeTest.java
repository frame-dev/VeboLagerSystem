package ch.framedev.lagersystem.managers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DatabaseManager.DatabaseType} — fully pure, no DB connections.
 */
class DatabaseManagerDatabaseTypeTest {

    // =========================================================================
    // fromConfig(String)
    // =========================================================================

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "unknown", "postgres", "mysql"})
    @DisplayName("fromConfig: null, blank and unrecognised values default to SQLITE")
    void fromConfig_defaultsToSqlite(String value) {
        assertEquals(DatabaseManager.DatabaseType.SQLITE,
                DatabaseManager.DatabaseType.fromConfig(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"h2", "H2", "H2 "})
    @DisplayName("fromConfig: 'h2' variants (case-insensitive) return H2")
    void fromConfig_h2(String value) {
        assertEquals(DatabaseManager.DatabaseType.H2,
                DatabaseManager.DatabaseType.fromConfig(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "JSON", "Json"})
    @DisplayName("fromConfig: 'json' variants (case-insensitive) return JSON")
    void fromConfig_json(String value) {
        assertEquals(DatabaseManager.DatabaseType.JSON,
                DatabaseManager.DatabaseType.fromConfig(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "YAML", "Yaml", "yml", "YML"})
    @DisplayName("fromConfig: 'yaml'/'yml' variants (case-insensitive) return YAML")
    void fromConfig_yaml(String value) {
        assertEquals(DatabaseManager.DatabaseType.YAML,
                DatabaseManager.DatabaseType.fromConfig(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"sqlite", "SQLITE", "SQLite"})
    @DisplayName("fromConfig: 'sqlite' variants (case-insensitive) return SQLITE")
    void fromConfig_sqlite(String value) {
        assertEquals(DatabaseManager.DatabaseType.SQLITE,
                DatabaseManager.DatabaseType.fromConfig(value));
    }

    // =========================================================================
    // getConfigValue()
    // =========================================================================

    @Test
    @DisplayName("getConfigValue: SQLITE → 'sqlite'")
    void getConfigValue_sqlite() {
        assertEquals("sqlite", DatabaseManager.DatabaseType.SQLITE.getConfigValue());
    }

    @Test
    @DisplayName("getConfigValue: H2 → 'h2'")
    void getConfigValue_h2() {
        assertEquals("h2", DatabaseManager.DatabaseType.H2.getConfigValue());
    }

    @Test
    @DisplayName("getConfigValue: JSON → 'json'")
    void getConfigValue_json() {
        assertEquals("json", DatabaseManager.DatabaseType.JSON.getConfigValue());
    }

    @Test
    @DisplayName("getConfigValue: YAML → 'yaml'")
    void getConfigValue_yaml() {
        assertEquals("yaml", DatabaseManager.DatabaseType.YAML.getConfigValue());
    }

    // =========================================================================
    // getDisplayName()
    // =========================================================================

    @Test
    @DisplayName("getDisplayName: SQLITE → 'SQLite'")
    void getDisplayName_sqlite() {
        assertEquals("SQLite", DatabaseManager.DatabaseType.SQLITE.getDisplayName());
    }

    @Test
    @DisplayName("getDisplayName: H2 → 'H2'")
    void getDisplayName_h2() {
        assertEquals("H2", DatabaseManager.DatabaseType.H2.getDisplayName());
    }

    @Test
    @DisplayName("getDisplayName: JSON → 'JSON Filesystem'")
    void getDisplayName_json() {
        assertEquals("JSON Filesystem", DatabaseManager.DatabaseType.JSON.getDisplayName());
    }

    @Test
    @DisplayName("getDisplayName: YAML → 'YAML Filesystem'")
    void getDisplayName_yaml() {
        assertEquals("YAML Filesystem", DatabaseManager.DatabaseType.YAML.getDisplayName());
    }

    // =========================================================================
    // round-trip: fromConfig(getConfigValue()) == original
    // =========================================================================

    @Test
    @DisplayName("round-trip: fromConfig(getConfigValue()) returns the same type")
    void fromConfig_roundTrip() {
        for (DatabaseManager.DatabaseType type : DatabaseManager.DatabaseType.values()) {
            assertEquals(type,
                    DatabaseManager.DatabaseType.fromConfig(type.getConfigValue()),
                    "Round-trip failed for: " + type);
        }
    }

    // =========================================================================
    // ALLOWED_TABLES / TABLE constants
    // =========================================================================

    @Test
    @DisplayName("ALLOWED_TABLES contains all known application tables")
    void allowedTables_containsAllKnownTables() {
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_ARTICLES));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_VENDORS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_ORDERS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_CLIENTS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_DEPARTMENTS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_USERS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_WARNINGS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_LOGS));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_NOTES));
        assertTrue(DatabaseManager.ALLOWED_TABLES.contains(DatabaseManager.TABLE_SEPARATE_ARTICLES));
    }

    @Test
    @DisplayName("getDefaultFileName: SQLITE returns .db extension")
    void getDefaultFileName_sqlite_hasDbExtension() {
        String fileName = DatabaseManager.getDefaultFileName(DatabaseManager.DatabaseType.SQLITE);
        assertTrue(fileName.endsWith(".db"),
                "SQLITE default file name must end with .db, got: " + fileName);
    }

    @Test
    @DisplayName("getDefaultFileName: H2 does not need .db extension")
    void getDefaultFileName_h2_noDotDb() {
        String fileName = DatabaseManager.getDefaultFileName(DatabaseManager.DatabaseType.H2);
        assertFalse(fileName.endsWith(".db"),
                "H2 default file name should not end with .db, got: " + fileName);
    }
}

