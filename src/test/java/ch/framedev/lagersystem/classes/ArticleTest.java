package ch.framedev.lagersystem.classes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Article} — exercises getters, setters and
 * {@link Article#getQrCodeData()}.  The {@link Article#getCategory()}
 * method is intentionally excluded because it relies on a file-system
 * category mapping that is not available in the test environment.
 */
class ArticleTest {

    // =========================================================================
    // Construction / Getters
    // =========================================================================

    @Test
    @DisplayName("constructor: all fields are stored and retrievable via getters")
    void constructor_storesAllFields() {
        Article article = new Article("A001", "Reiniger", "10 lt.", 50, 5, 12.50, 8.00, "LieferantAG");

        assertEquals("A001", article.getArticleNumber());
        assertEquals("Reiniger", article.getName());
        assertEquals("10 lt.", article.getDetails());
        assertEquals(50, article.getStockQuantity());
        assertEquals(5, article.getMinStockLevel());
        assertEquals(12.50, article.getSellPrice(), 1e-9);
        assertEquals(8.00, article.getPurchasePrice(), 1e-9);
        assertEquals("LieferantAG", article.getVendorName());
    }

    @Test
    @DisplayName("constructor: null fields are accepted")
    void constructor_nullFieldsAccepted() {
        Article article = new Article(null, null, null, 0, 0, 0.0, 0.0, null);

        assertNull(article.getArticleNumber());
        assertNull(article.getName());
        assertNull(article.getDetails());
        assertNull(article.getVendorName());
    }

    // =========================================================================
    // Setters
    // =========================================================================

    @Test
    @DisplayName("setArticleNumber: updates the article number")
    void setArticleNumber_updatesValue() {
        Article article = buildTestArticle();
        article.setArticleNumber("B999");
        assertEquals("B999", article.getArticleNumber());
    }

    @Test
    @DisplayName("setName: updates the name")
    void setName_updatesValue() {
        Article article = buildTestArticle();
        article.setName("Neuer Name");
        assertEquals("Neuer Name", article.getName());
    }

    @Test
    @DisplayName("setDetails: updates the details")
    void setDetails_updatesValue() {
        Article article = buildTestArticle();
        article.setDetails("5 lt.");
        assertEquals("5 lt.", article.getDetails());
    }

    @Test
    @DisplayName("setStockQuantity: updates the stock quantity")
    void setStockQuantity_updatesValue() {
        Article article = buildTestArticle();
        article.setStockQuantity(100);
        assertEquals(100, article.getStockQuantity());
    }

    @Test
    @DisplayName("setMinStockLevel: updates the min stock level")
    void setMinStockLevel_updatesValue() {
        Article article = buildTestArticle();
        article.setMinStockLevel(10);
        assertEquals(10, article.getMinStockLevel());
    }

    @Test
    @DisplayName("setSellPrice: updates the sell price")
    void setSellPrice_updatesValue() {
        Article article = buildTestArticle();
        article.setSellPrice(99.99);
        assertEquals(99.99, article.getSellPrice(), 1e-9);
    }

    @Test
    @DisplayName("setPurchasePrice: updates the purchase price")
    void setPurchasePrice_updatesValue() {
        Article article = buildTestArticle();
        article.setPurchasePrice(49.99);
        assertEquals(49.99, article.getPurchasePrice(), 1e-9);
    }

    @Test
    @DisplayName("setVendorName: updates the vendor name")
    void setVendorName_updatesValue() {
        Article article = buildTestArticle();
        article.setVendorName("NeuerLieferant");
        assertEquals("NeuerLieferant", article.getVendorName());
    }

    // =========================================================================
    // getQrCodeData()
    // =========================================================================

    @Test
    @DisplayName("getQrCodeData: contains all expected fields")
    void getQrCodeData_containsAllFields() {
        Article article = new Article("A001", "Reiniger", "10 lt.", 50, 5, 12.50, 8.00, "LieferantAG");
        String qr = article.getQrCodeData();

        assertNotNull(qr);
        assertTrue(qr.contains("artikelNr:A001"), "QR must include article number");
        assertTrue(qr.contains("name:Reiniger"), "QR must include name");
        assertTrue(qr.contains("details:10 lt."), "QR must include details");
        assertTrue(qr.contains("einkaufspreis:8.0"), "QR must include purchase price");
        assertTrue(qr.contains("verkaufspreis:12.5"), "QR must include sell price");
        assertTrue(qr.contains("lieferant:LieferantAG"), "QR must include vendor name");
    }

    @Test
    @DisplayName("getQrCodeData: fields are semicolon-separated")
    void getQrCodeData_semiColonSeparated() {
        Article article = new Article("A001", "Reiniger", "10 lt.", 50, 5, 12.50, 8.00, "LieferantAG");
        String qr = article.getQrCodeData();
        // Expect exactly 5 semicolons (6 fields)
        long count = qr.chars().filter(c -> c == ';').count();
        assertEquals(5, count, "QR data must have exactly 5 semicolons separating 6 fields");
    }

    @Test
    @DisplayName("getQrCodeData: null article number throws NullPointerException")
    void getQrCodeData_nullArticleNumber_throwsNPE() {
        Article article = new Article(null, "Name", "Details", 0, 0, 0.0, 0.0, null);
        assertThrows(NullPointerException.class, article::getQrCodeData);
    }

    @Test
    @DisplayName("getQrCodeData: null details and vendor name included as 'null'")
    void getQrCodeData_nullOptionalFields_appearsAsNull() {
        Article article = new Article("A001", "Name", null, 0, 0, 0.0, 0.0, null);
        String qr = article.getQrCodeData();
        assertTrue(qr.contains("details:null"), "null details must appear as 'null'");
        assertTrue(qr.contains("lieferant:null"), "null vendor must appear as 'null'");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Article buildTestArticle() {
        return new Article("X001", "TestArtikel", "Testdetails", 10, 2, 9.99, 5.50, "TestVendor");
    }
}

