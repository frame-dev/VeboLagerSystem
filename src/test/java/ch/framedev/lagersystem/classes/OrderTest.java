package ch.framedev.lagersystem.classes;

import ch.framedev.lagersystem.utils.ArticleUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    @DisplayName("constructor: uses default status when none is provided")
    void constructor_usesDefaultStatus() {
        Order order = new Order(
                "ORD-1",
                orderedArticles("1001", 2),
                "Max Muster",
                "K-1",
                "31.03.2026",
                "Erika Muster",
                "S-1",
                "Lager");

        assertEquals("In Bearbeitung", order.getStatus());
    }

    @Test
    @DisplayName("constructor: copies null metadata maps to empty maps")
    void constructor_turnsNullMetadataMapsIntoEmptyMaps() {
        Order order = new Order(
                "ORD-2",
                orderedArticles("1001", 1),
                null,
                null,
                null,
                "Receiver",
                "RK",
                "31.03.2026",
                "Sender",
                "SK",
                "Dept",
                "Offen");

        assertNotNull(order.getArticleSizes());
        assertNotNull(order.getArticleColors());
        assertNotNull(order.getArticleFillings());
        assertTrue(order.getArticleSizes().isEmpty());
        assertTrue(order.getArticleColors().isEmpty());
        assertTrue(order.getArticleFillings().isEmpty());
    }

    @Test
    @DisplayName("default constructor: initializes metadata maps")
    void defaultConstructor_initializesMetadataMaps() {
        Order order = new Order();

        assertNotNull(order.getArticleSizes());
        assertNotNull(order.getArticleColors());
        assertNotNull(order.getArticleFillings());
        assertTrue(order.getArticleSizes().isEmpty());
        assertTrue(order.getArticleColors().isEmpty());
        assertTrue(order.getArticleFillings().isEmpty());
    }

    @Test
    @DisplayName("setArticleSizes/setArticleColors/setArticleFillings: defensively copy maps")
    void metadataSetters_copyIncomingMaps() {
        Order order = new Order();
        Map<String, String> sizes = new LinkedHashMap<>();
        Map<String, String> colors = new LinkedHashMap<>();
        Map<String, String> fillings = new LinkedHashMap<>();

        sizes.put("1001", "L");
        colors.put("1001", "Rot");
        fillings.put("1001", "500 ml");

        order.setArticleSizes(sizes);
        order.setArticleColors(colors);
        order.setArticleFillings(fillings);

        sizes.put("1001", "XL");
        colors.put("1001", "Blau");
        fillings.put("1001", "1 l");

        assertEquals("L", order.getArticleSize("1001"));
        assertEquals("Rot", order.getArticleColor("1001"));
        assertEquals("500 ml", order.getArticleFilling("1001"));
    }

    @Test
    @DisplayName("getArticle metadata accessors: return empty string for null or missing keys")
    void metadataAccessors_returnEmptyStringForMissingKeys() {
        Order order = new Order();

        assertEquals("", order.getArticleSize(null));
        assertEquals("", order.getArticleColor(null));
        assertEquals("", order.getArticleFilling(null));
        assertEquals("", order.getArticleSize("missing"));
        assertEquals("", order.getArticleColor("missing"));
        assertEquals("", order.getArticleFilling("missing"));
    }

    @Test
    @DisplayName("formatArticleLabel: appends filling, size and color")
    void formatArticleLabel_appendsMetadata() {
        String articleKey = ArticleUtils.buildOrderItemKey("1001", "XL", "Blau", "500 ml");
        Order order = new Order();
        order.setArticleSizes(Map.of(articleKey, "XL"));
        order.setArticleColors(Map.of(articleKey, "Blau"));
        order.setArticleFillings(Map.of(articleKey, "500 ml"));
        Article article = new Article("1001", "Handseife", "Details", 10, 2, 5.0, 2.0, "Vendor");

        String label = order.formatArticleLabel(article, articleKey);

        assertEquals("Handseife [500 ml] (XL) {Blau}", label);
    }

    @Test
    @DisplayName("formatArticleLabel: uses article number when no explicit key is provided")
    void formatArticleLabel_usesArticleNumberByDefault() {
        Order order = new Order();
        order.setArticleSizes(Map.of("1001", "M"));
        order.setArticleColors(Map.of("1001", "Gruen"));
        order.setArticleFillings(Map.of("1001", "250 ml"));
        Article article = new Article("1001", "Reiniger", "Details", 10, 2, 5.0, 2.0, "Vendor");

        String label = order.formatArticleLabel(article);

        assertEquals("Reiniger [250 ml] (M) {Gruen}", label);
    }

    @Test
    @DisplayName("formatArticleLabel: returns empty string for null article")
    void formatArticleLabel_returnsEmptyStringForNullArticle() {
        Order order = new Order();

        assertEquals("", order.formatArticleLabel(null));
        assertEquals("", order.formatArticleLabel(null, "1001"));
    }

    @Test
    @DisplayName("getQRCodeDataString: includes ordered articles and all metadata fields")
    void getQRCodeDataString_containsExpectedFields() {
        Map<String, Integer> articles = new LinkedHashMap<>();
        articles.put("1001", 2);
        articles.put("1002", 4);
        Order order = new Order(
                "ORD-QR",
                articles,
                "Receiver",
                "RK-1",
                "31.03.2026",
                "Sender",
                "SK-1",
                "Verkauf",
                "Abgeschlossen");

        String qrData = order.getQRCodeDataString();

        assertEquals("ORD-QR;1001,2|1002,4;Receiver;RK-1;31.03.2026;Sender;SK-1;Verkauf;Abgeschlossen", qrData);
    }

    @Test
    @DisplayName("getQRCodeDataString: handles empty ordered article map without trailing pipe")
    void getQRCodeDataString_handlesEmptyOrderedArticleMap() {
        Order order = new Order(
                "ORD-EMPTY",
                new LinkedHashMap<>(),
                "Receiver",
                "RK-1",
                "31.03.2026",
                "Sender",
                "SK-1",
                "Verkauf");

        String qrData = order.getQRCodeDataString();

        assertEquals("ORD-EMPTY;;Receiver;RK-1;31.03.2026;Sender;SK-1;Verkauf;In Bearbeitung", qrData);
    }

    private Map<String, Integer> orderedArticles(String articleNumber, int quantity) {
        Map<String, Integer> articles = new LinkedHashMap<>();
        articles.put(articleNumber, quantity);
        return articles;
    }
}
