package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.History;
import ch.framedev.lagersystem.classes.SeperateArticle;
import ch.framedev.lagersystem.classes.Vendor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleManagerTest extends ManagerTestSupport {

    @Test
    @DisplayName("insert/get/update/delete: article CRUD works")
    void articleCrud_works() {
        ArticleManager manager = ArticleManager.getInstance();
        Article article = new Article("1001", "Reiniger", "1 l", 10, 2, 9.9, 5.0, "VendorA");

        assertTrue(manager.insertArticle(article));
        assertTrue(manager.existsArticle("1001"));
        assertEquals("Reiniger", manager.getArticleByNumber("1001").getName());
        assertEquals("1001", manager.getArticleByName("Reiniger").getArticleNumber());

        article.setName("Reiniger Neu");
        article.setStockQuantity(25);
        assertTrue(manager.updateArticle(article));
        assertEquals("Reiniger Neu", manager.getArticleByNumber("1001").getName());
        assertEquals(25, manager.getArticleByNumber("1001").getStockQuantity());

        assertTrue(manager.deleteArticleByNumber("1001"));
        assertFalse(manager.existsArticle("1001"));
        assertNull(manager.getArticleByNumber("1001"));
    }

    @Test
    @DisplayName("insertArticle: auto-creates vendor relation")
    void insertArticle_autoCreatesVendorRelation() {
        ArticleManager articleManager = ArticleManager.getInstance();
        VendorManager vendorManager = VendorManager.getInstance();

        assertTrue(articleManager.insertArticle(new Article("2201", "Handseife", "500 ml", 12, 2, 7.5, 3.2, "VendorX")));

        Vendor vendor = vendorManager.getVendorByName("VendorX");
        assertNotNull(vendor);
        assertTrue(vendor.getSuppliedArticles().contains("2201"));
    }

    @Test
    @DisplayName("stock methods: addToStock and removeFromStock update quantity")
    void stockMethods_updateQuantity() {
        ArticleManager manager = ArticleManager.getInstance();
        manager.insertArticle(new Article("3301", "Spray", "250 ml", 10, 1, 4.0, 2.0, "VendorY"));

        assertTrue(manager.addToStock("3301", 5, "anna"));
        assertEquals(15, manager.getArticleByNumber("3301").getStockQuantity());

        assertTrue(manager.removeFromStock("3301", 20, "bernd"));
        assertEquals(0, manager.getArticleByNumber("3301").getStockQuantity());

        List<History> histories = HistoryManager.getInstance().getHistoriesForArticle("3301").stream()
                .filter(history -> history.getAction() != null && history.getAction().startsWith("STOCK_"))
                .toList();
        assertEquals(2, histories.size());
        assertEquals("bernd", histories.get(0).getUserName());
        assertEquals(15, histories.get(0).getOldStock());
        assertEquals(0, histories.get(0).getNewStock());
        assertEquals(-15, histories.get(0).getChangeAmount());
        assertEquals("STOCK_DECREASED", histories.get(0).getAction());
        assertTrue(histories.get(0).getInfo().contains("Benutzer: bernd"));

        assertEquals("anna", histories.get(1).getUserName());
        assertEquals(10, histories.get(1).getOldStock());
        assertEquals(15, histories.get(1).getNewStock());
        assertEquals(5, histories.get(1).getChangeAmount());
        assertEquals("STOCK_INCREASED", histories.get(1).getAction());
    }

    @Test
    @DisplayName("separate article CRUD: insert query update delete works")
    void separateArticleCrud_works() {
        ArticleManager manager = ArticleManager.getInstance();
        SeperateArticle article = new SeperateArticle(1, "4401", "Blau");

        assertTrue(manager.insertSeperateArticle(article));
        assertTrue(manager.existsSeperateArticle(1, "4401"));
        assertTrue(manager.existsSeperateArticleByDetail("4401", "Blau"));
        assertTrue(manager.hasSeperateArticles("4401"));
        assertEquals("Blau", manager.getSeperateArticle(1, "4401").getOtherDetails());
        assertEquals("Blau", manager.getArticleByNumberAndDetail("4401", "Blau").getOtherDetails());
        assertEquals(List.of(1), manager.getAllIndexesForArticleNumber("4401"));
        assertEquals(List.of("Blau"), manager.getAllDetailsForArticleNumber("4401"));

        SeperateArticle updated = new SeperateArticle(1, "4401", "Rot");
        assertTrue(manager.updateSeperateArticle(updated));
        assertEquals("Rot", manager.getSeperateArticle(1, "4401").getOtherDetails());

        assertTrue(manager.deleteSeperateArticle(1, "4401"));
        assertFalse(manager.existsSeperateArticle(1, "4401"));
    }

    @Test
    @DisplayName("cache stats: reflect inserted article and index")
    void cacheStats_reflectInsertedArticle() {
        ArticleManager manager = ArticleManager.getInstance();
        manager.insertArticle(new Article("5501", "Tuch", "Details", 3, 1, 2.0, 1.0, "VendorZ"));
        manager.getArticleByNumber("5501");

        Map<String, Object> stats = manager.getCacheStats();

        assertTrue(((Integer) stats.get("articleCacheSize")) >= 1);
        assertTrue(((Integer) stats.get("articleNumberIndexSize")) >= 1);
        assertEquals(500, stats.get("maxCacheSize"));
    }
}
