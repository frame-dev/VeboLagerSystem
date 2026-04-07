package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.DatabaseManager;
import ch.framedev.lagersystem.managers.DatabaseManager.DatabaseType;
import ch.framedev.lagersystem.main.Main;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link OrderExport}, focusing on the file-based
 * {@link OrderExport#exportOrderToFile(File, Order)} entry point.
 */
@DisplayName("OrderExport")
class OrderExportTest {

    @TempDir
    Path tempDir;

    private DatabaseManager db;

    @BeforeEach
    void setUp() throws Exception {
        // Redirect appDataDirCache so OrderLoggingUtils writes its log under the temp dir
        setAppDataDirCache(tempDir.toFile());

        // Reset all relevant singletons before creating a fresh DB
        resetSingleton(OrderLoggingUtils.class);
        ArticleManager.resetInstance();

        db = new DatabaseManager(DatabaseType.H2, tempDir.toString(), "order-export-testdb");
        db.initializeApplicationSchema();
        Main.databaseManager = db;
    }

    @AfterEach
    void tearDown() throws IOException {
        resetSingleton(OrderLoggingUtils.class);
        ArticleManager.resetInstance();

        if (db != null) {
            db.close();
        }
        Main.databaseManager = null;

        // Clear appDataDirCache
        try {
            setAppDataDirCache(null);
        } catch (Exception ignored) {
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

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
        } catch (Exception ignored) {
        }
    }

    private Article buildArticle(String number) {
        return new Article(number, "Testartikel " + number, "Details", 100, 5, 9.50, 4.00, "Lieferant AG");
    }

    private Order buildOrder(String id, String articleNumber, int qty) {
        Map<String, Integer> articles = new HashMap<>();
        articles.put(articleNumber, qty);
        return new Order(id, articles, "Max Muster", "CH00 0001",
                "07.04.2026", "VEBO", "CH00 0002", "Lager");
    }

    // -----------------------------------------------------------------------
    // Null / empty argument validation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportOrderToFile: null outputFile throws IllegalArgumentException")
    void exportOrderToFile_nullFile_throws() {
        Order order = buildOrder("ORD-001", "A001", 2);
        assertThrows(IllegalArgumentException.class,
                () -> OrderExport.exportOrderToFile(null, order));
    }

    @Test
    @DisplayName("exportOrderToFile: null order returns early without creating a file")
    void exportOrderToFile_nullOrder_doesNotCreateFile() {
        File output = tempDir.resolve("null_order.pdf").toFile();
        OrderExport.exportOrderToFile(output, null);
        // Should return early – file must not have been created
        assertFalse(output.exists(), "No file should be created for a null order");
    }

    @Test
    @DisplayName("exportOrderToFile: order with no articles returns early without creating a file")
    void exportOrderToFile_emptyArticles_doesNotCreateFile() {
        File output = tempDir.resolve("empty_articles.pdf").toFile();
        Order order = new Order("ORD-EMPTY", new HashMap<>(),
                "Max", "KT-0", "07.04.2026", "VEBO", "KT-1", "Lager");
        OrderExport.exportOrderToFile(output, order);
        assertFalse(output.exists(), "No file should be created when the order has no articles");
    }

    // -----------------------------------------------------------------------
    // Successful PDF generation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportOrderToFile: creates a non-empty PDF for a valid order")
    void exportOrderToFile_validOrder_createsPdf() throws IOException {
        // Register an article so ArticleManager can look it up
        Article article = buildArticle("A001");
        ArticleManager.getInstance().insertArticle(article);

        File output = tempDir.resolve("order.pdf").toFile();
        Order order = buildOrder("ORD-001", "A001", 3);

        OrderExport.exportOrderToFile(output, order);

        assertTrue(output.exists(), "PDF file should have been created");
        assertTrue(output.length() > 0, "PDF file should not be empty");
        try (PDDocument doc = PDDocument.load(output)) {
            assertTrue(doc.getNumberOfPages() >= 1, "PDF must have at least one page");
        }
    }

    @Test
    @DisplayName("exportOrderToFile: PDF has .pdf extension even when file has none")
    void exportOrderToFile_addsExtension() throws IOException {
        Article article = buildArticle("A002");
        ArticleManager.getInstance().insertArticle(article);

        // Pass a file without extension – ExportDialogUtils.ensureExtension should add .pdf
        File outputNoExt = tempDir.resolve("order_no_ext").toFile();
        Order order = buildOrder("ORD-002", "A002", 1);

        OrderExport.exportOrderToFile(outputNoExt, order);

        File expectedFile = tempDir.resolve("order_no_ext.pdf").toFile();
        assertTrue(expectedFile.exists(), "PDF with .pdf extension should have been created");
    }

    @Test
    @DisplayName("exportOrderToFile: order with multiple articles produces a valid PDF")
    void exportOrderToFile_multipleArticles_createsValidPdf() throws IOException {
        Article a1 = buildArticle("B001");
        Article a2 = buildArticle("B002");
        Article a3 = buildArticle("B003");
        ArticleManager.getInstance().insertArticle(a1);
        ArticleManager.getInstance().insertArticle(a2);
        ArticleManager.getInstance().insertArticle(a3);

        Map<String, Integer> articles = new HashMap<>();
        articles.put("B001", 2);
        articles.put("B002", 5);
        articles.put("B003", 1);
        Order order = new Order("ORD-MULTI", articles,
                "Anna Beispiel", "CH00 9999",
                "07.04.2026", "VEBO Zentral", "CH00 8888", "Produktion");

        File output = tempDir.resolve("multi_order.pdf").toFile();
        OrderExport.exportOrderToFile(output, order);

        assertTrue(output.exists());
        try (PDDocument doc = PDDocument.load(output)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }

    @Test
    @DisplayName("exportOrderToFile: works when article is not in ArticleManager (skipped silently)")
    void exportOrderToFile_unknownArticle_skippedSilently() throws IOException {
        // No article inserted – ArticleManager will return null for "UNKNOWN"
        // The row is skipped; but the order still has 1 entry, so the early-exit check
        // (orderedArticles.isEmpty()) does NOT trigger. The PDF should still be created
        // because PDFBox generates the page even if all rows are skipped.
        Map<String, Integer> articles = new HashMap<>();
        articles.put("UNKNOWN", 3);
        Order order = new Order("ORD-UNKNOWN", articles,
                "Ghost", "KT-0", "07.04.2026", "VEBO", "KT-1", "Lager");

        File output = tempDir.resolve("unknown_article_order.pdf").toFile();
        // Should complete without throwing – unknown articles are skipped
        assertDoesNotThrow(() -> OrderExport.exportOrderToFile(output, order));
    }

    @Test
    @DisplayName("exportOrderToFile: order with 'Abgeschlossen' status is exported correctly")
    void exportOrderToFile_finishedStatus_createsPdf() throws IOException {
        Article article = buildArticle("C001");
        ArticleManager.getInstance().insertArticle(article);

        Map<String, Integer> articles = new HashMap<>();
        articles.put("C001", 4);
        Order order = new Order("ORD-DONE", articles,
                "Anna Fertig", "CH00 0003",
                "07.04.2026", "VEBO", "CH00 0004", "Einkauf",
                "Abgeschlossen");

        File output = tempDir.resolve("done_order.pdf").toFile();
        OrderExport.exportOrderToFile(output, order);

        assertTrue(output.exists());
        try (PDDocument doc = PDDocument.load(output)) {
            assertTrue(doc.getNumberOfPages() >= 1);
        }
    }
}
