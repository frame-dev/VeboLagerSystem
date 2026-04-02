package ch.framedev.lagersystem.managers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ch.framedev.lagersystem.managers.CategoryManager.UNKNOWN_CATEGORY;
import static org.junit.jupiter.api.Assertions.*;

class CategoryManagerTest extends ManagerTestSupport {

    // -------------------------------------------------------------------------
    // insertCategory / exists
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("insertCategory: new category is persisted and exists returns true")
    void insertCategory_persistsCategory() {
        CategoryManager manager = CategoryManager.getInstance();

        assertTrue(manager.insertCategory("Lebensmittel", "100 - 199"));
        assertTrue(manager.exists("Lebensmittel"));
    }

    @Test
    @DisplayName("insertCategory: duplicate category returns false")
    void insertCategory_duplicateReturnsFalse() {
        CategoryManager manager = CategoryManager.getInstance();

        manager.insertCategory("Getränke", "200 - 299");
        assertFalse(manager.insertCategory("Getränke", "200 - 299"));
    }

    @Test
    @DisplayName("insertCategory: blank name or fromTo returns false")
    void insertCategory_blankArgumentsReturnFalse() {
        CategoryManager manager = CategoryManager.getInstance();

        assertFalse(manager.insertCategory("", "100 - 199"));
        assertFalse(manager.insertCategory("Kategorie", "  "));
        assertFalse(manager.insertCategory(null, "100 - 199"));
        assertFalse(manager.insertCategory("Kategorie", null));
    }

    // -------------------------------------------------------------------------
    // deleteCategory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deleteCategory: removes existing category")
    void deleteCategory_removesCategory() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Reinigung", "800 - 899");

        assertTrue(manager.deleteCategory("Reinigung"));
        assertFalse(manager.exists("Reinigung"));
    }

    @Test
    @DisplayName("deleteCategory: returns false for non-existing category")
    void deleteCategory_nonExistingReturnsFalse() {
        assertFalse(CategoryManager.getInstance().deleteCategory("NichtVorhanden"));
    }

    @Test
    @DisplayName("deleteCategory: returns false for null or blank")
    void deleteCategory_nullOrBlankReturnsFalse() {
        CategoryManager manager = CategoryManager.getInstance();
        assertFalse(manager.deleteCategory(null));
        assertFalse(manager.deleteCategory("  "));
    }

    // -------------------------------------------------------------------------
    // updateCategory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("updateCategory: changes fromTo for existing category")
    void updateCategory_changesFromTo() {
        CategoryManager manager = CategoryManager.getInstance();

        manager.insertCategory("Haushalt", "300 - 399");
        assertTrue(manager.updateCategory("Haushalt", "300 - 499"));

        List<Map<String, String>> all = manager.getAllCategories();
        String fromTo = all.stream()
                .filter(e -> "Haushalt".equals(e.get("category")))
                .map(e -> e.get("fromTo"))
                .findFirst()
                .orElse(null);
        assertEquals("300 - 499", fromTo);
    }

    @Test
    @DisplayName("updateCategory: returns false for non-existing category")
    void updateCategory_nonExistingReturnsFalse() {
        CategoryManager manager = CategoryManager.getInstance();

        assertFalse(manager.updateCategory("Unbekannt", "1 - 9"));
    }

    // -------------------------------------------------------------------------
    // getAllCategories
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAllCategories: returns all inserted categories with correct keys")
    void getAllCategories_returnsAllCategories() {
        CategoryManager manager = CategoryManager.getInstance();

        manager.insertCategory("Büro", "400 - 499");
        manager.insertCategory("Technik", "500 - 599");

        List<Map<String, String>> all = manager.getAllCategories();
        assertEquals(2, all.size());
        assertTrue(all.stream().anyMatch(e -> "Büro".equals(e.get("category")) && "400 - 499".equals(e.get("fromTo"))));
        assertTrue(all.stream().anyMatch(e -> "Technik".equals(e.get("category")) && "500 - 599".equals(e.get("fromTo"))));
    }

    @Test
    @DisplayName("getAllCategories: empty table returns empty list")
    void getAllCategories_emptyTable() {
        assertTrue(CategoryManager.getInstance().getAllCategories().isEmpty());
    }

    // -------------------------------------------------------------------------
    // getCategoryForArticle
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getCategoryForArticle: matches article number inside range")
    void getCategoryForArticle_matchesRange() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Lebensmittel", "100 - 199");

        assertEquals("Lebensmittel", manager.getCategoryForArticle("150"));
    }

    @Test
    @DisplayName("getCategoryForArticle: matches article number at range boundaries")
    void getCategoryForArticle_matchesBoundaries() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Grenze", "200 - 299");

        assertEquals("Grenze", manager.getCategoryForArticle("200"));
        assertEquals("Grenze", manager.getCategoryForArticle("299"));
    }

    @Test
    @DisplayName("getCategoryForArticle: article number outside all ranges returns UNKNOWN_CATEGORY")
    void getCategoryForArticle_noMatch() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Elektro", "600 - 699");

        assertEquals(UNKNOWN_CATEGORY, manager.getCategoryForArticle("500"));
    }

    @Test
    @DisplayName("getCategoryForArticle: strips non-numeric prefix from article number")
    void getCategoryForArticle_stripsPrefix() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Werkzeug", "700 - 799");

        assertEquals("Werkzeug", manager.getCategoryForArticle("ART750"));
    }

    @Test
    @DisplayName("getCategoryForArticle: null or blank returns UNKNOWN_CATEGORY")
    void getCategoryForArticle_nullOrBlank() {
        CategoryManager manager = CategoryManager.getInstance();

        assertEquals(UNKNOWN_CATEGORY, manager.getCategoryForArticle(null));
        assertEquals(UNKNOWN_CATEGORY, manager.getCategoryForArticle("   "));
    }

    @Test
    @DisplayName("getCategoryForArticle: purely non-numeric string returns UNKNOWN_CATEGORY")
    void getCategoryForArticle_noDigits() {
        CategoryManager manager = CategoryManager.getInstance();

        assertEquals(UNKNOWN_CATEGORY, manager.getCategoryForArticle("ABC"));
    }

    @Test
    @DisplayName("getCategoryForArticle: single-value range (no dash) is supported")
    void getCategoryForArticle_singleValueRange() {
        CategoryManager manager = CategoryManager.getInstance();
        manager.insertCategory("Sonderposten", "42");

        assertEquals("Sonderposten", manager.getCategoryForArticle("42"));
        assertEquals(UNKNOWN_CATEGORY, manager.getCategoryForArticle("43"));
    }
}
