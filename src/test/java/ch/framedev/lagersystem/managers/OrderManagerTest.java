package ch.framedev.lagersystem.managers;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderManagerTest extends ManagerTestSupport {

    private Order buildOrder(String id) {
        return new Order(id, Map.of("1001", 2), "Max Mustermann", "KT-001",
                "2026-01-15", "Lager", "KT-999", "Einkauf");
    }

    // =========================================================================
    // CRUD
    // =========================================================================

    @Test
    @DisplayName("insert/exists/get/update/delete: order CRUD works")
    void orderCrud_works() {
        OrderManager manager = OrderManager.getInstance();
        Order order = buildOrder("ORD-001");

        assertTrue(manager.insertOrder(order));
        assertTrue(manager.existsOrder("ORD-001"));

        Order fetched = manager.getOrder("ORD-001");
        assertNotNull(fetched);
        assertEquals("ORD-001", fetched.getOrderId());
        assertEquals("Max Mustermann", fetched.getReceiverName());
        assertEquals("Einkauf", fetched.getDepartment());
        assertEquals(2, fetched.getOrderedArticles().get("1001"));

        Order updated = new Order("ORD-001", Map.of("1001", 5), "Max Mustermann", "KT-001",
                "2026-01-15", "Lager", "KT-999", "Einkauf", "Geliefert");
        assertTrue(manager.updateOrder(updated));
        assertEquals("Geliefert", manager.getOrder("ORD-001").getStatus());
        assertEquals(5, manager.getOrder("ORD-001").getOrderedArticles().get("1001"));

        assertTrue(manager.deleteOrder("ORD-001"));
        assertFalse(manager.existsOrder("ORD-001"));
        assertNull(manager.getOrder("ORD-001"));
    }

    @Test
    @DisplayName("insertOrder: duplicate id returns false")
    void insertOrder_duplicate_returnsFalse() {
        OrderManager manager = OrderManager.getInstance();
        assertTrue(manager.insertOrder(buildOrder("ORD-DUP")));
        assertFalse(manager.insertOrder(buildOrder("ORD-DUP")));
    }

    @Test
    @DisplayName("insertOrder: null order returns false")
    void insertOrder_null_returnsFalse() {
        assertFalse(OrderManager.getInstance().insertOrder(null));
    }

    @Test
    @DisplayName("deleteOrder: non-existent id returns false")
    void deleteOrder_nonExistent_returnsFalse() {
        assertFalse(OrderManager.getInstance().deleteOrder("ORD-GHOST"));
    }

    @Test
    @DisplayName("updateOrder: non-existent id returns false")
    void updateOrder_nonExistent_returnsFalse() {
        assertFalse(OrderManager.getInstance().updateOrder(buildOrder("ORD-MISSING")));
    }

    @Test
    @DisplayName("getOrders: returns all inserted orders")
    void getOrders_returnsAllInserted() {
        OrderManager manager = OrderManager.getInstance();
        manager.insertOrder(buildOrder("ORD-A1"));
        manager.insertOrder(buildOrder("ORD-A2"));
        manager.insertOrder(buildOrder("ORD-A3"));

        List<Order> orders = manager.getOrders();

        assertEquals(3, orders.size());
        assertTrue(orders.stream().anyMatch(o -> "ORD-A1".equals(o.getOrderId())));
        assertTrue(orders.stream().anyMatch(o -> "ORD-A2".equals(o.getOrderId())));
        assertTrue(orders.stream().anyMatch(o -> "ORD-A3".equals(o.getOrderId())));
    }

    @Test
    @DisplayName("getOrders: empty table returns empty list")
    void getOrders_emptyTable_returnsEmptyList() {
        assertTrue(OrderManager.getInstance().getOrders().isEmpty());
    }

    @Test
    @DisplayName("getOrderArticles: returns articles present in article table")
    void getOrderArticles_returnsMatchingArticles() {
        ArticleManager articleManager = ArticleManager.getInstance();
        articleManager.insertArticle(new Article("2001", "Seife", "500 ml", 10, 2, 5.0, 2.5, "VendorA"));

        OrderManager orderManager = OrderManager.getInstance();
        Order order = new Order("ORD-ART", Map.of("2001", 1), "Empf.", "KT-1",
                "2026-01-20", "Sender", "KT-2", "IT");
        orderManager.insertOrder(order);

        List<Article> articles = orderManager.getOrderArticles(orderManager.getOrder("ORD-ART"));

        assertEquals(1, articles.size());
        assertEquals("2001", articles.get(0).getArticleNumber());
    }

    @Test
    @DisplayName("getOrderArticles: unknown article number yields empty list")
    void getOrderArticles_unknownArticle_yieldsEmptyList() {
        OrderManager manager = OrderManager.getInstance();
        Order order = new Order("ORD-NOART", Map.of("UNKNOWN-99", 1), "R", "K",
                "2026-01-01", "S", "K2", "Dept");
        manager.insertOrder(order);

        List<Article> articles = manager.getOrderArticles(manager.getOrder("ORD-NOART"));
        assertTrue(articles.isEmpty());
    }

    @Test
    @DisplayName("clearCache: does not break subsequent reads")
    void clearCache_doesNotBreakSubsequentReads() {
        OrderManager manager = OrderManager.getInstance();
        manager.insertOrder(buildOrder("ORD-CC1"));
        manager.clearCache();

        assertTrue(manager.existsOrder("ORD-CC1"));
        assertNotNull(manager.getOrder("ORD-CC1"));
    }

    @Test
    @DisplayName("order with article metadata: sizes, colors, fillings roundtrip")
    void orderWithMetadata_roundtrip() {
        OrderManager manager = OrderManager.getInstance();
        Order order = new Order(
                "ORD-META",
                Map.of("3001", 3),
                Map.of("3001", "500 ml"),
                Map.of("3001", "Rot"),
                Map.of("3001", "500 ml"),
                "Receiver", "KT-X",
                "2026-02-01",
                "Sender", "KT-Y",
                "Sales",
                "In Bearbeitung"
        );
        assertTrue(manager.insertOrder(order));

        Order loaded = manager.getOrder("ORD-META");
        assertNotNull(loaded);
        assertEquals("500 ml", loaded.getArticleSizes().get("3001"));
        assertEquals("Rot", loaded.getArticleColors().get("3001"));
        assertEquals("500 ml", loaded.getArticleFillings().get("3001"));
    }
}
