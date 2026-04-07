package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ClientManager;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.managers.DatabaseManager.DatabaseType;
import ch.framedev.lagersystem.managers.DepartmentManager;
import ch.framedev.lagersystem.managers.VendorManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ImportUtilsTest {

    @TempDir
    Path tempDir;

    private DatabaseManager db;

    // -------------------------------------------------------------------------
    // Setup / teardown
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        // Point Main.getAppDataDir() to
        // our temp directory so file-based methods write there
        setAppDataDirCache(tempDir.toFile());

        // Minimal DB for QR-code table – use SQLite because addQrCodeImport uses
        // "INSERT OR IGNORE" which is SQLite-specific syntax
        db = new DatabaseManager(DatabaseType.SQLITE, tempDir.toString(), "import-testdb");
        db.initializeApplicationSchema();
        Main.databaseManager = db;

        // Reset managers that may carry state from previous tests
        ArticleManager.resetInstance();
        resetSingleton(VendorManager.class);
        resetSingleton(ClientManager.class);
        resetSingleton(DepartmentManager.class);

        // Reset ImportUtils singleton so it doesn't hold stale file references
        resetSingleton(ImportUtils.class);
    }

    @AfterEach
    void tearDown() throws IOException {
        ArticleManager.resetInstance();
        resetSingleton(VendorManager.class);
        resetSingleton(ClientManager.class);
        resetSingleton(DepartmentManager.class);
        resetSingleton(ImportUtils.class);

        if (db != null) {
            db.close();
        }
        Main.databaseManager = null;
        try {
            setAppDataDirCache(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ignored) {
                // best-effort cleanup
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setAppDataDirCache(File dir) throws Exception {
        Field f = Main.class.getDeclaredField("appDataDirCache");
        f.setAccessible(true);
        f.set(null, dir);
    }

    private void resetSingleton(Class<?> type) {
        try {
            Field f = type.getDeclaredField("instance");
            f.setAccessible(true);
            f.set(null, null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Could not reset singleton for " + type.getSimpleName(), e);
        }
    }

    /** Invokes the private static {@code parseCsvLine(String)} on SettingsDataTransferService. */
    private String[] parseCsvLine(String line) throws Exception {
        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("parseCsvLine", String.class);
        m.setAccessible(true);
        return (String[]) m.invoke(null, line);
    }

    private File writeTempCsv(String name, String content) throws IOException {
        Path f = tempDir.resolve(name);
        Files.writeString(f, content, StandardCharsets.UTF_8);
        return f.toFile();
    }

    // =========================================================================
    // parseCsvLine – plain values
    // =========================================================================

    @Test
    @DisplayName("parseCsvLine: simple comma-separated values without quotes")
    void parseCsvLine_simple_noQuotes() throws Exception {
        String[] parts = parseCsvLine("a,b,c");
        assertArrayEquals(new String[]{"a", "b", "c"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: quoted fields are unquoted")
    void parseCsvLine_quotedFields_unquoted() throws Exception {
        String[] parts = parseCsvLine("\"hello\",\"world\"");
        assertArrayEquals(new String[]{"hello", "world"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: quoted field with embedded comma is treated as one value")
    void parseCsvLine_quotedFieldWithComma_singleValue() throws Exception {
        String[] parts = parseCsvLine("\"Müller, Hans\",Lager");
        assertArrayEquals(new String[]{"Müller, Hans", "Lager"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: doubled quote inside quoted field is unescaped to single quote")
    void parseCsvLine_doubledQuote_unescaped() throws Exception {
        String[] parts = parseCsvLine("\"say \"\"hello\"\"\",ok");
        assertArrayEquals(new String[]{"say \"hello\"", "ok"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: mixed quoted and plain fields")
    void parseCsvLine_mixedFields() throws Exception {
        String[] parts = parseCsvLine("\"1001\",Reiniger,\"1 l\",10,2,9.9,5.0,VendorA");
        assertArrayEquals(new String[]{"1001", "Reiniger", "1 l", "10", "2", "9.9", "5.0", "VendorA"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: empty fields between commas produce empty strings")
    void parseCsvLine_emptyFields_emptyStrings() throws Exception {
        String[] parts = parseCsvLine("a,,c");
        assertArrayEquals(new String[]{"a", "", "c"}, parts);
    }

    @Test
    @DisplayName("parseCsvLine: unclosed quote throws IllegalArgumentException")
    void parseCsvLine_unclosedQuote_throwsIllegalArgument() {
        assertThrows(Exception.class, () -> parseCsvLine("\"unclosed,field"));
    }

    @Test
    @DisplayName("parseCsvLine: single value without commas returns one-element array")
    void parseCsvLine_singleValue_oneElement() throws Exception {
        String[] parts = parseCsvLine("only");
        assertArrayEquals(new String[]{"only"}, parts);
    }

    // =========================================================================
    // addToList / getImportedItems
    // =========================================================================

    @Test
    @DisplayName("getImportedItems: returns empty list when file does not exist")
    void getImportedItems_noFile_returnsEmpty() {
        assertTrue(ImportUtils.getImportedItems().isEmpty());
    }

    @Test
    @DisplayName("addToList: appended items are returned by getImportedItems")
    void addToList_itemsAreRetrievable() {
        ImportUtils.addToList("ITEM-001");
        ImportUtils.addToList("ITEM-002");

        List<String> items = ImportUtils.getImportedItems();
        assertTrue(items.contains("ITEM-001"));
        assertTrue(items.contains("ITEM-002"));
    }

    @Test
    @DisplayName("addToList: calling multiple times appends (does not overwrite)")
    void addToList_multipleCallsAppend() {
        ImportUtils.addToList("A");
        ImportUtils.addToList("B");
        ImportUtils.addToList("C");

        assertEquals(3, ImportUtils.getImportedItems().size());
    }

    // =========================================================================
    // deleteImportFile
    // =========================================================================

    @Test
    @DisplayName("deleteImportFile: returns true when file does not exist")
    void deleteImportFile_noFile_returnsTrue() {
        assertTrue(ImportUtils.deleteImportFile());
    }

    @Test
    @DisplayName("deleteImportFile: deletes existing file and returns true")
    void deleteImportFile_existingFile_deletedAndReturnsTrue() {
        ImportUtils.addToList("X");
        assertFalse(ImportUtils.getImportedItems().isEmpty());

        assertTrue(ImportUtils.deleteImportFile());
        assertTrue(ImportUtils.getImportedItems().isEmpty());
    }

    // =========================================================================
    // addQrCodeImport / getImportedQrCodes (DB-backed)
    // =========================================================================

    @Test
    @DisplayName("getImportedQrCodes: returns empty list initially")
    void getImportedQrCodes_empty_initially() {
        assertTrue(ImportUtils.getImportedQrCodes().isEmpty());
    }

    @Test
    @DisplayName("addQrCodeImport: stored ID is returned by getImportedQrCodes")
    void addQrCodeImport_idIsRetrievable() {
        ImportUtils.addQrCodeImport("QR-001");

        assertTrue(ImportUtils.getImportedQrCodes().contains("QR-001"));
    }

    @Test
    @DisplayName("addQrCodeImport: duplicate ID is silently ignored (INSERT OR IGNORE)")
    void addQrCodeImport_duplicate_silentlyIgnored() {
        ImportUtils.addQrCodeImport("QR-DUP");
        ImportUtils.addQrCodeImport("QR-DUP");

        List<String> qrCodes = ImportUtils.getImportedQrCodes();
        assertEquals(1, qrCodes.stream().filter("QR-DUP"::equals).count());
    }

    @Test
    @DisplayName("addQrCodeImport: multiple distinct IDs are all stored")
    void addQrCodeImport_multipleIds_allStored() {
        ImportUtils.addQrCodeImport("QR-A");
        ImportUtils.addQrCodeImport("QR-B");
        ImportUtils.addQrCodeImport("QR-C");

        List<String> qrCodes = ImportUtils.getImportedQrCodes();
        assertTrue(qrCodes.contains("QR-A"));
        assertTrue(qrCodes.contains("QR-B"));
        assertTrue(qrCodes.contains("QR-C"));
    }

    // =========================================================================
    // migrateQrCodesFileIfNeeded (implicit via addQrCodeImport / getImportedQrCodes)
    // =========================================================================

    @Test
    @DisplayName("getImportedQrCodes: migrates legacy imported_qrcodes.txt into DB on first call")
    void getImportedQrCodes_migratesLegacyFile() throws IOException {
        // Write a legacy text file with QR IDs
        Path legacyFile = tempDir.resolve("imported_qrcodes.txt");
        Files.writeString(legacyFile, "LEGACY-1\nLEGACY-2\n", StandardCharsets.UTF_8);

        List<String> qrCodes = ImportUtils.getImportedQrCodes();

        assertTrue(qrCodes.contains("LEGACY-1"));
        assertTrue(qrCodes.contains("LEGACY-2"));
        // Legacy file should be deleted after migration
        assertFalse(legacyFile.toFile().exists(), "Legacy file should be deleted after migration");
    }

    // =========================================================================
    // addToOwnUseList
    // =========================================================================

    @Test
    @DisplayName("addToOwnUseList: writes data to own_use_list.txt in app data dir")
    void addToOwnUseList_writesFile() throws IOException {
        ImportUtils.addToOwnUseList("USE-001");

        Path ownUseFile = tempDir.resolve("own_use_list.txt");
        assertTrue(ownUseFile.toFile().exists(), "own_use_list.txt should be created");
        String content = Files.readString(ownUseFile, StandardCharsets.UTF_8);
        assertTrue(content.contains("USE-001"));
    }

    // =========================================================================
    // Article CSV import end-to-end (via SettingsDataTransferService)
    // =========================================================================

    @Test
    @DisplayName("importArticlesFromCsv: valid CSV file inserts articles into the DB")
    void importArticlesFromCsv_validCsv_insertsArticles() throws Exception {
        // CSV content matching the exported format:
        // Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant
        String csv = "Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant\n"
                + "\"1001\",\"Reiniger\",\"1 l\",10,2,9.9,5.0,\"VendorA\"\n"
                + "\"1002\",\"Spachtel\",\"Metall\",5,1,14.5,8.0,\"VendorB\"\n";

        File csvFile = writeTempCsv("articles_import.csv", csv);

        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("importArticlesFromCsv", File.class);
        m.setAccessible(true);
        m.invoke(null, csvFile);

        ArticleManager am = ArticleManager.getInstance();
        assertTrue(am.existsArticle("1001"), "Article 1001 should have been imported");
        assertEquals("Reiniger", am.getArticleByNumber("1001").getName());

        assertTrue(am.existsArticle("1002"), "Article 1002 should have been imported");
        assertEquals("Spachtel", am.getArticleByNumber("1002").getName());
    }

    @Test
    @DisplayName("importArticlesFromCsv: row with too few fields is skipped, valid rows still imported")
    void importArticlesFromCsv_invalidRow_skipped() throws Exception {
        String csv = "Artikelnummer,Name,Details,Lagerbestand,Mindestlagerbestand,Verkaufspreis,Einkaufspreis,Lieferant\n"
                + "TOO,FEW,FIELDS\n"                             // only 3 columns – skipped
                + "\"2001\",\"Hammer\",\"Stahl\",3,1,19.9,10.0,\"VendorC\"\n";

        File csvFile = writeTempCsv("articles_partialerror.csv", csv);

        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("importArticlesFromCsv", File.class);
        m.setAccessible(true);
        m.invoke(null, csvFile);

        ArticleManager am = ArticleManager.getInstance();
        assertTrue(am.existsArticle("2001"), "Valid row should still be imported");
    }

    @Test
    @DisplayName("importVendorsFromCsv: valid CSV file inserts vendors into the DB")
    void importVendorsFromCsv_validCsv_insertsVendors() throws Exception {
        // Name,Kontaktperson,Telefon,E-Mail,Adresse,MinBestellwert
        String csv = "Name,Kontaktperson,Telefon,E-Mail,Adresse,MinBestellwert\n"
                + "\"TestVendor\",\"Hans Muster\",\"0791234567\",\"info@test.ch\",\"Musterstrasse 1\",50.0\n";

        File csvFile = writeTempCsv("vendors_import.csv", csv);

        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("importVendorsFromCsv", File.class);
        m.setAccessible(true);
        m.invoke(null, csvFile);

        VendorManager vm = VendorManager.getInstance();
        assertTrue(vm.existsVendor("TestVendor"), "Vendor should have been imported");
    }

    @Test
    @DisplayName("importClientsFromCsv: valid CSV file inserts clients into the DB")
    void importClientsFromCsv_validCsv_insertsClients() throws Exception {
        // Name,Abteilung
        String csv = "Name,Abteilung\n"
                + "\"Max Mustermann\",\"Lager\"\n";

        File csvFile = writeTempCsv("clients_import.csv", csv);

        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("importClientsFromCsv", File.class);
        m.setAccessible(true);
        m.invoke(null, csvFile);

        ClientManager cm = ClientManager.getInstance();
        assertTrue(cm.existsClient("Max Mustermann"), "Client should have been imported");
    }

    @Test
    @DisplayName("importDepartmentsFromCsv: valid CSV file inserts departments into the DB")
    void importDepartmentsFromCsv_validCsv_insertsDepartments() throws Exception {
        // Abteilung,KontoNummer
        String csv = "Abteilung,KontoNummer\n"
                + "\"Lager\",\"1000\"\n";

        File csvFile = writeTempCsv("departments_import.csv", csv);

        Class<?> svc = Class.forName("ch.framedev.lagersystem.guis.SettingsDataTransferService");
        Method m = svc.getDeclaredMethod("importDepartmentsFromCsv", File.class);
        m.setAccessible(true);
        m.invoke(null, csvFile);

        DepartmentManager dm = DepartmentManager.getInstance();
        assertTrue(dm.existsDepartment("Lager"), "Department should have been imported");
    }
}
