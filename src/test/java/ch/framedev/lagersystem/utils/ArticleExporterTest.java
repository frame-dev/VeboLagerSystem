package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Unit tests for {@link ArticleExporter}.
 * Covers public utility methods and private static helpers via reflection.
 */
@DisplayName("ArticleExporter")
class ArticleExporterTest {

    // -----------------------------------------------------------------------
    // sanitizeForWinAnsi (public static)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("sanitizeForWinAnsi: null returns empty string")
    void sanitizeForWinAnsi_null_returnsEmpty() {
        assertEquals("", ArticleExporter.sanitizeForWinAnsi(null));
    }

    @Test
    @DisplayName("sanitizeForWinAnsi: empty string returns empty string")
    void sanitizeForWinAnsi_empty_returnsEmpty() {
        assertEquals("", ArticleExporter.sanitizeForWinAnsi(""));
    }

    @Test
    @DisplayName("sanitizeForWinAnsi: pure ASCII is unchanged")
    void sanitizeForWinAnsi_asciiText_unchanged() {
        assertEquals("Hello World 123", ArticleExporter.sanitizeForWinAnsi("Hello World 123"));
    }

    @Test
    @DisplayName("sanitizeForWinAnsi: WinAnsi chars (≤255) like ä ö ü are preserved")
    void sanitizeForWinAnsi_winAnsiChars_preserved() {
        String input = "Lieferant: Müller & Söhne";
        assertEquals(input, ArticleExporter.sanitizeForWinAnsi(input));
    }

    @Test
    @DisplayName("sanitizeForWinAnsi: chars above 255 are stripped")
    void sanitizeForWinAnsi_charsAbove255_stripped() {
        // U+4E2D (中) and U+1F600 (😀) are both above 255 and should be removed
        String input = "Hello\u4E2DWorld\uD83D\uDE00";
        String result = ArticleExporter.sanitizeForWinAnsi(input);
        assertEquals("HelloWorld", result);
    }

    @Test
    @DisplayName("sanitizeForWinAnsi: mixed content strips only high codepoints")
    void sanitizeForWinAnsi_mixed_stripsOnlyHigh() {
        // 'é' is U+00E9 (233) – within 255, kept; '€' is U+20AC (8364) – above 255, stripped
        String input = "Preis: 5\u20AC (café)";
        String result = ArticleExporter.sanitizeForWinAnsi(input);
        assertEquals("Preis: 5 (caf\u00E9)", result);
    }

    // -----------------------------------------------------------------------
    // calculateColumnWidths (private static) – accessed via reflection
    // -----------------------------------------------------------------------

    private static float[] calculateColumnWidths(int numCols, int[] baseColumnWidths, float tableWidth)
            throws Exception {
        Method m = ArticleExporter.class.getDeclaredMethod(
                "calculateColumnWidths", int.class, int[].class, float.class);
        m.setAccessible(true);
        return (float[]) m.invoke(null, numCols, baseColumnWidths, tableWidth);
    }

    @Test
    @DisplayName("calculateColumnWidths: returns array of correct length")
    void calculateColumnWidths_returnsCorrectLength() throws Exception {
        float[] result = calculateColumnWidths(3, new int[]{100, 200, 300}, 600f);
        assertEquals(3, result.length);
    }

    @Test
    @DisplayName("calculateColumnWidths: widths sum to tableWidth")
    void calculateColumnWidths_sumEqualsTableWidth() throws Exception {
        float tableWidth = 500f;
        float[] result = calculateColumnWidths(4, new int[]{50, 100, 150, 200}, tableWidth);
        float sum = 0f;
        for (float w : result) sum += w;
        assertEquals(tableWidth, sum, 0.01f);
    }

    @Test
    @DisplayName("calculateColumnWidths: equal base widths produce equal output widths")
    void calculateColumnWidths_equalBases_equalOutput() throws Exception {
        float[] result = calculateColumnWidths(3, new int[]{100, 100, 100}, 300f);
        assertEquals(result[0], result[1], 0.01f);
        assertEquals(result[1], result[2], 0.01f);
    }

    @Test
    @DisplayName("calculateColumnWidths: columns without base entry receive default width 100")
    void calculateColumnWidths_missingBases_useDefault() throws Exception {
        // 2 base widths for 4 columns → last 2 should use default 100
        float tableWidth = 400f;
        float[] result = calculateColumnWidths(4, new int[]{50, 50}, tableWidth);
        assertEquals(4, result.length);
        float sum = 0f;
        for (float w : result) sum += w;
        assertEquals(tableWidth, sum, 0.01f);
    }

    // -----------------------------------------------------------------------
    // formatCellValue (private static) – accessed via reflection
    // -----------------------------------------------------------------------

    private static String formatCellValue(Object value) throws Exception {
        Method m = ArticleExporter.class.getDeclaredMethod("formatCellValue", Object.class);
        m.setAccessible(true);
        return (String) m.invoke(null, value);
    }

    @Test
    @DisplayName("formatCellValue: null returns empty string")
    void formatCellValue_null_returnsEmpty() throws Exception {
        assertEquals("", formatCellValue(null));
    }

    @Test
    @DisplayName("formatCellValue: Double is formatted to 2 decimal places")
    void formatCellValue_double_formattedWithTwoDecimals() throws Exception {
        assertEquals("12.50", formatCellValue(12.5));
    }

    @Test
    @DisplayName("formatCellValue: Float is formatted to 2 decimal places")
    void formatCellValue_float_formattedWithTwoDecimals() throws Exception {
        assertEquals("3.14", formatCellValue(3.14f));
    }

    @Test
    @DisplayName("formatCellValue: Integer is formatted without decimals")
    void formatCellValue_integer_formattedWithoutDecimals() throws Exception {
        assertEquals("42", formatCellValue(42));
    }

    @Test
    @DisplayName("formatCellValue: Long is formatted without decimals")
    void formatCellValue_long_formattedWithoutDecimals() throws Exception {
        assertEquals("1000000", formatCellValue(1_000_000L));
    }

    @Test
    @DisplayName("formatCellValue: String is returned as-is")
    void formatCellValue_string_returnedAsIs() throws Exception {
        assertEquals("Testartikel", formatCellValue("Testartikel"));
    }

    // -----------------------------------------------------------------------
    // isNumericValue (private static) – accessed via reflection
    // -----------------------------------------------------------------------

    private static boolean isNumericValue(Object value) throws Exception {
        Method m = ArticleExporter.class.getDeclaredMethod("isNumericValue", Object.class);
        m.setAccessible(true);
        return (Boolean) m.invoke(null, value);
    }

    @Test
    @DisplayName("isNumericValue: null returns false")
    void isNumericValue_null_returnsFalse() throws Exception {
        assertFalse(isNumericValue(null));
    }

    @Test
    @DisplayName("isNumericValue: Integer returns true")
    void isNumericValue_integer_returnsTrue() throws Exception {
        assertTrue(isNumericValue(5));
    }

    @Test
    @DisplayName("isNumericValue: Double returns true")
    void isNumericValue_double_returnsTrue() throws Exception {
        assertTrue(isNumericValue(3.14));
    }

    @Test
    @DisplayName("isNumericValue: String returns false")
    void isNumericValue_string_returnsFalse() throws Exception {
        assertFalse(isNumericValue("123"));
    }

    // -----------------------------------------------------------------------
    // escapeCsv (private static) – accessed via reflection
    // -----------------------------------------------------------------------

    private static String escapeCsv(String value) throws Exception {
        Method m = ArticleExporter.class.getDeclaredMethod("escapeCsv", String.class);
        m.setAccessible(true);
        return (String) m.invoke(null, value);
    }

    @Test
    @DisplayName("escapeCsv: null returns empty string")
    void escapeCsv_null_returnsEmpty() throws Exception {
        assertEquals("", escapeCsv(null));
    }

    @Test
    @DisplayName("escapeCsv: plain value is returned as-is")
    void escapeCsv_plain_returnsAsIs() throws Exception {
        assertEquals("Artikel123", escapeCsv("Artikel123"));
    }

    @Test
    @DisplayName("escapeCsv: value with comma is quoted")
    void escapeCsv_withComma_quoted() throws Exception {
        assertEquals("\"Müller, AG\"", escapeCsv("Müller, AG"));
    }

    @Test
    @DisplayName("escapeCsv: value with double-quote doubles the quote and wraps in quotes")
    void escapeCsv_withDoubleQuote_doubled() throws Exception {
        assertEquals("\"say \"\"hello\"\"\"", escapeCsv("say \"hello\""));
    }

    @Test
    @DisplayName("escapeCsv: value with newline is quoted")
    void escapeCsv_withNewline_quoted() throws Exception {
        String result = escapeCsv("line1\nline2");
        assertTrue(result.startsWith("\"") && result.endsWith("\""));
    }

    @Test
    @DisplayName("escapeCsv: empty string is returned as-is")
    void escapeCsv_empty_returnsEmpty() throws Exception {
        assertEquals("", escapeCsv(""));
    }

    // -----------------------------------------------------------------------
    // exportOrderToPDF – null argument validation (no Swing needed)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportOrderToPDF: null file throws IllegalArgumentException")
    void exportOrderToPDF_nullFile_throws(@TempDir Path tempDir) {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipped in headless environment (Swing components cannot be created)");
        JComboBox<String> receiver = new JComboBox<>();
        JTextField receiverKonto = new JTextField();
        JComboBox<String> sender = new JComboBox<>();
        JTextField senderKonto = new JTextField();
        JComboBox<String> dept = new JComboBox<>();
        Article a = new Article("A001", "TestArtikel", "", 10, 2, 5.0, 3.0, "Lieferant");
        Map<Article, Integer> articles = new HashMap<>();
        articles.put(a, 1);
        assertThrows(IllegalArgumentException.class, () ->
                ArticleExporter.exportOrderToPDF(null, receiver, receiverKonto, sender, senderKonto,
                        dept, articles, null, null, null));
    }

    @Test
    @DisplayName("exportOrderToPDF: null receiver combobox throws IllegalArgumentException")
    void exportOrderToPDF_nullReceiverCombobox_throws(@TempDir Path tempDir) {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipped in headless environment (Swing components cannot be created)");
        File pdfFile = tempDir.resolve("order.pdf").toFile();
        JTextField receiverKonto = new JTextField();
        JComboBox<String> sender = new JComboBox<>();
        JTextField senderKonto = new JTextField();
        JComboBox<String> dept = new JComboBox<>();
        Article a = new Article("A001", "TestArtikel", "", 10, 2, 5.0, 3.0, "Lieferant");
        Map<Article, Integer> articles = new HashMap<>();
        articles.put(a, 1);
        assertThrows(IllegalArgumentException.class, () ->
                ArticleExporter.exportOrderToPDF(pdfFile, null, receiverKonto, sender, senderKonto,
                        dept, articles, null, null, null));
    }

    @Test
    @DisplayName("exportOrderToPDF: empty order articles map throws IllegalArgumentException")
    void exportOrderToPDF_emptyArticles_throws(@TempDir Path tempDir) {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipped in headless environment (Swing components cannot be created)");
        File pdfFile = tempDir.resolve("order.pdf").toFile();
        JComboBox<String> receiver = new JComboBox<>();
        JTextField receiverKonto = new JTextField();
        JComboBox<String> sender = new JComboBox<>();
        JTextField senderKonto = new JTextField();
        JComboBox<String> dept = new JComboBox<>();
        assertThrows(IllegalArgumentException.class, () ->
                ArticleExporter.exportOrderToPDF(pdfFile, receiver, receiverKonto, sender, senderKonto,
                        dept, new HashMap<>(), null, null, null));
    }

    // -----------------------------------------------------------------------
    // exportOrderToPDF – produces a valid PDF file
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportOrderToPDF: creates a readable PDF with one article")
    void exportOrderToPDF_createsValidPdf(@TempDir Path tempDir) throws IOException {
        assumeFalse(GraphicsEnvironment.isHeadless(),
                "Skipped in headless environment (Swing components cannot be created)");

        File pdfFile = tempDir.resolve("order.pdf").toFile();

        JComboBox<String> receiver = new JComboBox<>(new String[]{"Max Muster"});
        receiver.setSelectedIndex(0);
        JTextField receiverKonto = new JTextField("CH00 0000 0000 0000 0001");
        JComboBox<String> sender = new JComboBox<>(new String[]{"VEBO"});
        sender.setSelectedIndex(0);
        JTextField senderKonto = new JTextField("CH00 0000 0000 0000 0002");
        JComboBox<String> dept = new JComboBox<>(new String[]{"Lager"});
        dept.setSelectedIndex(0);

        Article a = new Article("A001", "Testartikel", "Details", 50, 5, 9.90, 4.50, "Lieferant AG");
        Map<Article, Integer> articles = new HashMap<>();
        articles.put(a, 3);

        ArticleExporter.exportOrderToPDF(pdfFile, receiver, receiverKonto, sender, senderKonto,
                dept, articles, null, null, null);

        assertTrue(pdfFile.exists(), "PDF file should have been created");
        assertTrue(pdfFile.length() > 0, "PDF file should not be empty");

        // Verify file is a valid PDF that PDFBox can open
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertTrue(doc.getNumberOfPages() >= 1, "PDF should have at least one page");
        }
    }

    // -----------------------------------------------------------------------
    // exportArticlesToCsv – null argument validation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportArticlesToCsv: null articles throws IllegalArgumentException")
    void exportArticlesToCsv_null_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ArticleExporter.exportArticlesToCsv(null));
    }

    // -----------------------------------------------------------------------
    // exportArticlesToPdf – null argument validation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("exportArticlesToPdf: null articles throws IllegalArgumentException")
    void exportArticlesToPdf_null_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ArticleExporter.exportArticlesToPdf(null));
    }
}
