package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.utils.ArticleUtils.FillingData;
import ch.framedev.lagersystem.utils.ArticleUtils.VolumeUnit;
import ch.framedev.lagersystem.classes.Article;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArticleUtils} — covers pure-logic methods only;
 * no file system, database, or Swing dependencies are exercised.
 */
class ArticleUtilsTest {

    // =========================================================================
    // normalizeFilling(String)
    // =========================================================================

    @Test
    @DisplayName("normalizeFilling: null returns empty string")
    void normalizeFilling_null_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeFilling((String) null));
    }

    @Test
    @DisplayName("normalizeFilling: blank string returns empty string")
    void normalizeFilling_blank_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeFilling("   "));
    }

    @Test
    @DisplayName("normalizeFilling: '500 ml' normalised to '500 ml'")
    void normalizeFilling_500ml_normalised() {
        assertEquals("500 ml", ArticleUtils.normalizeFilling("500 ml"));
    }

    @Test
    @DisplayName("normalizeFilling: '500ml' (no space) normalised to '500 ml'")
    void normalizeFilling_500mlNoSpace_normalised() {
        assertEquals("500 ml", ArticleUtils.normalizeFilling("500ml"));
    }

    @Test
    @DisplayName("normalizeFilling: '1.5 l' normalised — decimal dot becomes comma")
    void normalizeFilling_1_5l_decimalComma() {
        assertEquals("1,5 l", ArticleUtils.normalizeFilling("1.5 l"));
    }

    @Test
    @DisplayName("normalizeFilling: '1,5 l' (comma decimal) normalised correctly")
    void normalizeFilling_1comma5l_normalised() {
        assertEquals("1,5 l", ArticleUtils.normalizeFilling("1,5 l"));
    }

    @Test
    @DisplayName("normalizeFilling: amount 0 returns empty (invalid amount)")
    void normalizeFilling_zeroAmount_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeFilling("0 ml"));
    }

    @Test
    @DisplayName("normalizeFilling: colon-prefix is stripped")
    void normalizeFilling_colonPrefix_stripped() {
        assertEquals("500 ml", ArticleUtils.normalizeFilling("Füllung: 500 ml"));
    }

    @Test
    @DisplayName("normalizeFilling: brackets are stripped")
    void normalizeFilling_brackets_stripped() {
        assertEquals("500 ml", ArticleUtils.normalizeFilling("[500 ml]"));
    }

    @Test
    @DisplayName("normalizeFilling: parentheses are stripped")
    void normalizeFilling_parentheses_stripped() {
        assertEquals("500 ml", ArticleUtils.normalizeFilling("(500 ml)"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ml", "l", "g", "kg"})
    @DisplayName("normalizeFilling: unit-only string returned as-is (lower-cased)")
    void normalizeFilling_unitOnly_returnedLower(String unit) {
        assertEquals(unit.toLowerCase(), ArticleUtils.normalizeFilling(unit.toUpperCase()));
    }

    @Test
    @DisplayName("normalizeFilling: two-argument overload normalises correctly")
    void normalizeFilling_twoArgs_normalised() {
        assertEquals("250 ml", ArticleUtils.normalizeFilling("250", "ml"));
    }

    @Test
    @DisplayName("normalizeFilling: two-argument overload with null amount")
    void normalizeFilling_twoArgs_nullAmount() {
        // null amount → "" amount, so combined string is "ml" (unit only)
        assertEquals("ml", ArticleUtils.normalizeFilling(null, "ml"));
    }

    @Test
    @DisplayName("normalizeFilling: 250 g normalised")
    void normalizeFilling_250g_normalised() {
        assertEquals("250 g", ArticleUtils.normalizeFilling("250 g"));
    }

    @Test
    @DisplayName("normalizeFilling: 1 kg normalised")
    void normalizeFilling_1kg_normalised() {
        assertEquals("1 kg", ArticleUtils.normalizeFilling("1 kg"));
    }

    // =========================================================================
    // normalizeMetadataValue(String)
    // =========================================================================

    @Test
    @DisplayName("normalizeMetadataValue: null returns empty string")
    void normalizeMetadataValue_null_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeMetadataValue(null));
    }

    @Test
    @DisplayName("normalizeMetadataValue: empty string returns empty string")
    void normalizeMetadataValue_empty_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeMetadataValue(""));
    }

    @Test
    @DisplayName("normalizeMetadataValue: blank string returns empty string")
    void normalizeMetadataValue_blank_returnsEmpty() {
        assertEquals("", ArticleUtils.normalizeMetadataValue("   "));
    }

    @Test
    @DisplayName("normalizeMetadataValue: leading/trailing whitespace is trimmed")
    void normalizeMetadataValue_trimmingWhitespace() {
        assertEquals("test", ArticleUtils.normalizeMetadataValue("  test  "));
    }

    @Test
    @DisplayName("normalizeMetadataValue: multiple spaces are collapsed")
    void normalizeMetadataValue_multipleSpaces_collapsed() {
        assertEquals("hello world", ArticleUtils.normalizeMetadataValue("hello  world"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"n/a", "N/A", "null", "NULL", "-"})
    @DisplayName("normalizeMetadataValue: sentinel values return empty string")
    void normalizeMetadataValue_sentinelValues_returnsEmpty(String value) {
        assertEquals("", ArticleUtils.normalizeMetadataValue(value));
    }

    @Test
    @DisplayName("normalizeMetadataValue: normal value passes through")
    void normalizeMetadataValue_normalValue_passesThrough() {
        assertEquals("Blau", ArticleUtils.normalizeMetadataValue("Blau"));
    }

    // =========================================================================
    // buildOrderItemKey / isCompositeOrderItemKey / getOrderItem*
    // =========================================================================

    @Test
    @DisplayName("buildOrderItemKey: blank article number returns empty string")
    void buildOrderItemKey_blankArticleNumber_returnsEmpty() {
        assertEquals("", ArticleUtils.buildOrderItemKey("", "M", "Blau", "500 ml"));
    }

    @Test
    @DisplayName("buildOrderItemKey: null article number returns empty string")
    void buildOrderItemKey_nullArticleNumber_returnsEmpty() {
        assertEquals("", ArticleUtils.buildOrderItemKey(null, "M", "Blau", "500 ml"));
    }

    @Test
    @DisplayName("buildOrderItemKey: only article number (all extras blank) returns plain number")
    void buildOrderItemKey_onlyArticleNumber_returnsPlain() {
        assertEquals("A100", ArticleUtils.buildOrderItemKey("A100", null, null, null));
        assertEquals("A100", ArticleUtils.buildOrderItemKey("A100", "", "", ""));
    }

    @Test
    @DisplayName("buildOrderItemKey: with extras returns composite key starting with 'a='")
    void buildOrderItemKey_withExtras_returnsCompositeKey() {
        String key = ArticleUtils.buildOrderItemKey("A100", "M", "Blau", "500 ml");
        assertTrue(key.startsWith("a="), "Composite key must start with 'a='");
        assertTrue(key.contains("|s="), "Composite key must contain '|s='");
        assertTrue(key.contains("|c="), "Composite key must contain '|c='");
        assertTrue(key.contains("|f="), "Composite key must contain '|f='");
    }

    @Test
    @DisplayName("isCompositeOrderItemKey: null returns false")
    void isCompositeOrderItemKey_null_returnsFalse() {
        assertFalse(ArticleUtils.isCompositeOrderItemKey(null));
    }

    @Test
    @DisplayName("isCompositeOrderItemKey: blank returns false")
    void isCompositeOrderItemKey_blank_returnsFalse() {
        assertFalse(ArticleUtils.isCompositeOrderItemKey("  "));
    }

    @Test
    @DisplayName("isCompositeOrderItemKey: plain article number returns false")
    void isCompositeOrderItemKey_plainNumber_returnsFalse() {
        assertFalse(ArticleUtils.isCompositeOrderItemKey("A100"));
    }

    @Test
    @DisplayName("isCompositeOrderItemKey: composite key returns true")
    void isCompositeOrderItemKey_compositeKey_returnsTrue() {
        String key = ArticleUtils.buildOrderItemKey("A100", "M", "Blau", "500 ml");
        assertTrue(ArticleUtils.isCompositeOrderItemKey(key));
    }

    @Test
    @DisplayName("round-trip: getOrderItemArticleNumber extracts correct article number")
    void roundTrip_articleNumber() {
        String key = ArticleUtils.buildOrderItemKey("B200", "L", "Rot", "250 ml");
        assertEquals("B200", ArticleUtils.getOrderItemArticleNumber(key));
    }

    @Test
    @DisplayName("round-trip: getOrderItemSize extracts correct size")
    void roundTrip_size() {
        String key = ArticleUtils.buildOrderItemKey("B200", "L", "Rot", "250 ml");
        assertEquals("L", ArticleUtils.getOrderItemSize(key));
    }

    @Test
    @DisplayName("round-trip: getOrderItemColor extracts correct color")
    void roundTrip_color() {
        String key = ArticleUtils.buildOrderItemKey("B200", "L", "Rot", "250 ml");
        assertEquals("Rot", ArticleUtils.getOrderItemColor(key));
    }

    @Test
    @DisplayName("round-trip: getOrderItemFilling extracts and normalises filling")
    void roundTrip_filling() {
        String key = ArticleUtils.buildOrderItemKey("B200", "L", "Rot", "250 ml");
        assertEquals("250 ml", ArticleUtils.getOrderItemFilling(key));
    }

    @Test
    @DisplayName("getOrderItemArticleNumber: plain key returns the key value")
    void getOrderItemArticleNumber_plainKey_returnsSelf() {
        assertEquals("A100", ArticleUtils.getOrderItemArticleNumber("A100"));
    }

    @Test
    @DisplayName("getOrderItemSize: plain key returns empty string")
    void getOrderItemSize_plainKey_returnsEmpty() {
        assertEquals("", ArticleUtils.getOrderItemSize("A100"));
    }

    @Test
    @DisplayName("getOrderItemColor: plain key returns empty string")
    void getOrderItemColor_plainKey_returnsEmpty() {
        assertEquals("", ArticleUtils.getOrderItemColor("A100"));
    }

    @Test
    @DisplayName("getOrderItemFilling: plain key returns empty string")
    void getOrderItemFilling_plainKey_returnsEmpty() {
        assertEquals("", ArticleUtils.getOrderItemFilling("A100"));
    }

    @Test
    @DisplayName("getOrderItemArticleNumber: malformed composite value yields empty string")
    void getOrderItemArticleNumber_malformedComposite_returnsEmpty() {
        assertEquals("", ArticleUtils.getOrderItemArticleNumber("a=%%%|s=abc|c=abc|f=abc"));
    }

    @Test
    @DisplayName("getOrderItemSize/color/filling: malformed composite value yields empty strings")
    void getOrderItemMetadata_malformedComposite_returnsEmptyStrings() {
        String malformed = "a=QQ|s=%%%|c=%%%|f=%%%";

        assertEquals("", ArticleUtils.getOrderItemSize(malformed));
        assertEquals("", ArticleUtils.getOrderItemColor(malformed));
        assertEquals("", ArticleUtils.getOrderItemFilling(malformed));
    }

    // =========================================================================
    // isFillingValid(String)
    // =========================================================================

    @Test
    @DisplayName("isFillingValid: null is valid (treated as absent)")
    void isFillingValid_null_isValid() {
        assertTrue(ArticleUtils.isFillingValid(null));
    }

    @Test
    @DisplayName("isFillingValid: empty string is valid")
    void isFillingValid_empty_isValid() {
        assertTrue(ArticleUtils.isFillingValid(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"500 ml", "1,5 l", "250 g", "1 kg", "ml", "l", "g", "kg"})
    @DisplayName("isFillingValid: recognised filling values are valid")
    void isFillingValid_recognisedValues_valid(String filling) {
        assertTrue(ArticleUtils.isFillingValid(filling),
                "Expected valid for: " + filling);
    }

    @Test
    @DisplayName("isFillingValid: unrecognised text is invalid")
    void isFillingValid_unrecognisedText_invalid() {
        assertFalse(ArticleUtils.isFillingValid("random text"));
    }

    @Test
    @DisplayName("isFillingValid: zero amount normalises to blank → treated as valid")
    void isFillingValid_zeroAmount_treatedAsValid() {
        // normalizeFilling("0 ml") → "" → blank → valid
        assertTrue(ArticleUtils.isFillingValid("0 ml"));
    }

    // =========================================================================
    // parseFilling(String)
    // =========================================================================

    @Test
    @DisplayName("parseFilling: null returns null")
    void parseFilling_null_returnsNull() {
        assertNull(ArticleUtils.parseFilling(null));
    }

    @Test
    @DisplayName("parseFilling: empty string returns null")
    void parseFilling_empty_returnsNull() {
        assertNull(ArticleUtils.parseFilling(""));
    }

    @Test
    @DisplayName("parseFilling: '500 ml' returns FillingData(500, ml)")
    void parseFilling_500ml_returnsFillingData() {
        FillingData data = ArticleUtils.parseFilling("500 ml");
        assertNotNull(data);
        assertEquals(500.0, data.amount(), 1e-9);
        assertEquals("ml", data.normalizedUnit());
    }

    @Test
    @DisplayName("parseFilling: '1,5 l' returns FillingData(1.5, l)")
    void parseFilling_1comma5l_returnsFillingData() {
        FillingData data = ArticleUtils.parseFilling("1,5 l");
        assertNotNull(data);
        assertEquals(1.5, data.amount(), 1e-9);
        assertEquals("l", data.normalizedUnit());
    }

    @Test
    @DisplayName("parseFilling: '250 g' returns FillingData(250, g)")
    void parseFilling_250g_returnsFillingData() {
        FillingData data = ArticleUtils.parseFilling("250 g");
        assertNotNull(data);
        assertEquals(250.0, data.amount(), 1e-9);
        assertEquals("g", data.normalizedUnit());
    }

    @Test
    @DisplayName("parseFilling: zero amount returns null")
    void parseFilling_zeroAmount_returnsNull() {
        // normalizeFilling("0 ml") → "" → blank → parseFilling returns null
        assertNull(ArticleUtils.parseFilling("0 ml"));
    }

    @Test
    @DisplayName("parseFilling: unrecognised value returns null")
    void parseFilling_unrecognised_returnsNull() {
        assertNull(ArticleUtils.parseFilling("some text"));
    }

    @Test
    @DisplayName("resolveEffectiveSellPrice: null article returns 0")
    void resolveEffectiveSellPrice_nullArticle_returnsZero() {
        assertEquals(0.0, ArticleUtils.resolveEffectiveSellPrice(null, "500 ml"), 1e-9);
    }

    @Test
    @DisplayName("resolveEffectiveSellPrice: invalid filling returns article sell price")
    void resolveEffectiveSellPrice_invalidFilling_returnsSellPrice() {
        Article article = new Article("A1", "Artikel", "1 l", 0, 0, 19.9, 8.0, "Vendor");

        assertEquals(19.9, ArticleUtils.resolveEffectiveSellPrice(article, "invalid"), 1e-9);
    }

    @Test
    @DisplayName("resolveEffectiveSellPrice: non-volume filling returns article sell price")
    void resolveEffectiveSellPrice_nonVolumeFilling_returnsSellPrice() {
        Article article = new Article("A1", "Artikel", "1 l", 0, 0, 19.9, 8.0, "Vendor");

        assertEquals(19.9, ArticleUtils.resolveEffectiveSellPrice(article, "250 g"), 1e-9);
    }

    // =========================================================================
    // calculatePriceForFilling(double, double, double, VolumeUnit)
    // =========================================================================

    @Test
    @DisplayName("calculatePriceForFilling: 0.5 litre at 10/litre = 5.0")
    void calculatePriceForFilling_halfLitre() {
        double result = ArticleUtils.calculatePriceForFilling(10.0, 1.0, 0.5, VolumeUnit.LITER);
        assertEquals(5.0, result, 1e-9);
    }

    @Test
    @DisplayName("calculatePriceForFilling: 500 ml at 10/litre = 5.0")
    void calculatePriceForFilling_500ml() {
        double result = ArticleUtils.calculatePriceForFilling(10.0, 1.0, 500.0, VolumeUnit.MILLILITER);
        assertEquals(5.0, result, 1e-9);
    }

    @Test
    @DisplayName("calculatePriceForFilling: full litre equals sell price")
    void calculatePriceForFilling_fullLitre() {
        double result = ArticleUtils.calculatePriceForFilling(15.0, 1.0, 1.0, VolumeUnit.LITER);
        assertEquals(15.0, result, 1e-9);
    }

    @Test
    @DisplayName("calculatePriceForFilling: pro-rata for 2-litre container")
    void calculatePriceForFilling_proRata() {
        // 20.0 for 2 litres → price per litre = 10.0 → 1 litre = 10.0
        double result = ArticleUtils.calculatePriceForFilling(20.0, 2.0, 1.0, VolumeUnit.LITER);
        assertEquals(10.0, result, 1e-9);
    }

    @Test
    @DisplayName("calculatePriceForFilling: liter value <= 0 throws IllegalArgumentException")
    void calculatePriceForFilling_zeroLiter_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ArticleUtils.calculatePriceForFilling(10.0, 0.0, 1.0, VolumeUnit.LITER));
    }

    @Test
    @DisplayName("calculatePriceForFilling: negative liter throws IllegalArgumentException")
    void calculatePriceForFilling_negativeLiter_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ArticleUtils.calculatePriceForFilling(10.0, -1.0, 1.0, VolumeUnit.LITER));
    }

    @Test
    @DisplayName("calculatePriceForFilling: filling amount <= 0 throws IllegalArgumentException")
    void calculatePriceForFilling_zeroFilling_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ArticleUtils.calculatePriceForFilling(10.0, 1.0, 0.0, VolumeUnit.LITER));
    }

    @Test
    @DisplayName("calculatePriceForFilling: negative filling throws IllegalArgumentException")
    void calculatePriceForFilling_negativeFilling_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ArticleUtils.calculatePriceForFilling(10.0, 1.0, -1.0, VolumeUnit.LITER));
    }

    @Test
    @DisplayName("calculatePriceForFilling: null VolumeUnit throws IllegalArgumentException")
    void calculatePriceForFilling_nullUnit_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> ArticleUtils.calculatePriceForFilling(10.0, 1.0, 1.0, null));
    }

    // =========================================================================
    // getPartsFromDetails(String) / getPartsFromDetails(String, boolean)
    // =========================================================================

    @Test
    @DisplayName("getPartsFromDetails: null returns empty list")
    void getPartsFromDetails_null_returnsEmpty() {
        assertTrue(ArticleUtils.getPartsFromDetails(null).isEmpty());
    }

    @Test
    @DisplayName("getPartsFromDetails: blank string returns empty list")
    void getPartsFromDetails_blank_returnsEmpty() {
        assertTrue(ArticleUtils.getPartsFromDetails("   ").isEmpty());
    }

    @Test
    @DisplayName("getPartsFromDetails: single part without separator returns single element")
    void getPartsFromDetails_singlePart() {
        List<String> parts = ArticleUtils.getPartsFromDetails("Blau");
        assertEquals(1, parts.size());
        assertEquals("Blau", parts.get(0));
    }

    @Test
    @DisplayName("getPartsFromDetails: slash-separated parts are split correctly")
    void getPartsFromDetails_slashSeparated() {
        List<String> parts = ArticleUtils.getPartsFromDetails("S/M/L");
        assertEquals(3, parts.size());
        assertEquals("S", parts.get(0));
        assertEquals("M", parts.get(1));
        assertEquals("L", parts.get(2));
    }

    @Test
    @DisplayName("getPartsFromDetails: slash with spaces is also split")
    void getPartsFromDetails_slashWithSpaces() {
        List<String> parts = ArticleUtils.getPartsFromDetails("Rot / Blau / Grün");
        assertEquals(3, parts.size());
        assertEquals("Rot", parts.get(0));
        assertEquals("Blau", parts.get(1));
        assertEquals("Grün", parts.get(2));
    }

    @Test
    @DisplayName("getPartsFromDetails: slash preceded by digit is NOT split (price notation)")
    void getPartsFromDetails_slashAfterDigit_notSplit() {
        // "12.60/Einzel" → "/" is preceded by "0" (digit) → should NOT split
        List<String> parts = ArticleUtils.getPartsFromDetails("12.60/Einzel");
        assertEquals(1, parts.size());
        assertEquals("12.60/Einzel", parts.get(0));
    }

    @Test
    @DisplayName("getPartsFromDetails(cleanTokens=true): label prefix and quantity suffix stripped")
    void getPartsFromDetails_cleanTokens_stripsLabelAndQuantity() {
        // "Gr. S/M/L 100 Stk." → split by "/" → ["Gr. S", "M", "L 100 Stk."]
        // with cleanTokens: ["S", "M", "L"]
        List<String> parts = ArticleUtils.getPartsFromDetails("Gr. S/M/L 100 Stk.", true);
        assertEquals(3, parts.size());
        assertEquals("S", parts.get(0));
        assertEquals("M", parts.get(1));
        assertEquals("L", parts.get(2));
    }

    @Test
    @DisplayName("getPartsFromDetails(cleanTokens=true): parentheses stripped")
    void getPartsFromDetails_cleanTokens_stripsParentheses() {
        List<String> parts = ArticleUtils.getPartsFromDetails("(Rot)/Blau", true);
        assertEquals(2, parts.size());
        assertEquals("Rot", parts.get(0));
        assertEquals("Blau", parts.get(1));
    }

    @Test
    @DisplayName("getPartsFromDetails(cleanTokens=false): no label stripping")
    void getPartsFromDetails_noCleanTokens_noStripping() {
        // Label prefix is kept when cleanTokens=false
        List<String> parts = ArticleUtils.getPartsFromDetails("Gr. S/M", false);
        assertEquals(2, parts.size());
        assertEquals("Gr. S", parts.get(0));
        assertEquals("M", parts.get(1));
    }

    // =========================================================================
    // FillingData record
    // =========================================================================

    @Test
    @DisplayName("FillingData.normalizedUnit: lowercase conversion")
    void fillingData_normalizedUnit_lowercase() {
        FillingData data = new FillingData(500.0, "ML");
        assertEquals("ml", data.normalizedUnit());
    }

    @Test
    @DisplayName("FillingData.normalizedUnit: null unit returns empty string")
    void fillingData_normalizedUnit_null() {
        FillingData data = new FillingData(500.0, null);
        assertEquals("", data.normalizedUnit());
    }

    @Test
    @DisplayName("FillingData.normalizedUnit: trimmed")
    void fillingData_normalizedUnit_trimmed() {
        FillingData data = new FillingData(1.0, "  L  ");
        assertEquals("l", data.normalizedUnit());
    }

    @Test
    @DisplayName("FillingData.toVolumeUnit: 'l' maps to LITER")
    void fillingData_toVolumeUnit_liter() {
        FillingData data = new FillingData(1.5, "l");
        assertEquals(VolumeUnit.LITER, data.toVolumeUnit());
    }

    @Test
    @DisplayName("FillingData.toVolumeUnit: 'ml' maps to MILLILITER")
    void fillingData_toVolumeUnit_milliliter() {
        FillingData data = new FillingData(500.0, "ml");
        assertEquals(VolumeUnit.MILLILITER, data.toVolumeUnit());
    }

    @Test
    @DisplayName("FillingData.toVolumeUnit: 'g' maps to null (unsupported unit)")
    void fillingData_toVolumeUnit_gram_returnsNull() {
        FillingData data = new FillingData(250.0, "g");
        assertNull(data.toVolumeUnit());
    }

    @Test
    @DisplayName("FillingData.toVolumeUnit: 'kg' maps to null (unsupported unit)")
    void fillingData_toVolumeUnit_kg_returnsNull() {
        FillingData data = new FillingData(1.0, "kg");
        assertNull(data.toVolumeUnit());
    }

    @Test
    @DisplayName("FillingData.toDisplayString: 500 ml")
    void fillingData_toDisplayString_500ml() {
        FillingData data = new FillingData(500.0, "ml");
        assertEquals("500 ml", data.toDisplayString());
    }

    @Test
    @DisplayName("FillingData.toDisplayString: decimal point becomes comma")
    void fillingData_toDisplayString_decimalComma() {
        FillingData data = new FillingData(1.5, "l");
        assertEquals("1,5 l", data.toDisplayString());
    }

    @Test
    @DisplayName("FillingData.toDisplayString: trailing zeros stripped from integer amount")
    void fillingData_toDisplayString_integerAmount() {
        FillingData data = new FillingData(1000.0, "ml");
        assertEquals("1000 ml", data.toDisplayString());
    }

    // =========================================================================
    // formatArticleWithFilling
    // =========================================================================

    @Test
    @DisplayName("formatArticleWithFilling: null article returns empty string")
    void formatArticleWithFilling_nullArticle_returnsEmpty() {
        assertEquals("", ArticleUtils.formatArticleWithFilling(null, "500 ml"));
    }

    @Test
    @DisplayName("formatArticleWithFilling: null filling returns article name only")
    void formatArticleWithFilling_nullFilling_returnsNameOnly() {
        Article article = new Article("A1", "Testmittel", null, 0, 0, 10.0, 5.0, "Vendor");
        assertEquals("Testmittel", ArticleUtils.formatArticleWithFilling(article, null));
    }

    @Test
    @DisplayName("formatArticleWithFilling: blank filling returns article name only")
    void formatArticleWithFilling_blankFilling_returnsNameOnly() {
        Article article = new Article("A1", "Testmittel", null, 0, 0, 10.0, 5.0, "Vendor");
        assertEquals("Testmittel", ArticleUtils.formatArticleWithFilling(article, ""));
    }

    @Test
    @DisplayName("formatArticleWithFilling: valid filling appended in brackets")
    void formatArticleWithFilling_validFilling_appendedInBrackets() {
        Article article = new Article("A1", "Reinigungsmittel", null, 0, 0, 10.0, 5.0, "Vendor");
        assertEquals("Reinigungsmittel [500 ml]",
                ArticleUtils.formatArticleWithFilling(article, "500 ml"));
    }

    // =========================================================================
    // CategoryRange inner class
    // =========================================================================

    @Test
    @DisplayName("CategoryRange: fields are stored correctly")
    void categoryRange_fieldsStoredCorrectly() {
        ArticleUtils.CategoryRange range = new ArticleUtils.CategoryRange("Reinigungsmittel", 100, 199);
        assertEquals("Reinigungsmittel", range.category);
        assertEquals(100, range.rangeStart);
        assertEquals(199, range.rangeEnd);
    }
}

