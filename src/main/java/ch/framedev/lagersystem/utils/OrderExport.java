package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.managers.OrderManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Utility for exporting an {@link Order} as a PDF using Apache PDFBox.
 *
 * <p>Improvements compared to the previous implementation:
 * <ul>
 *   <li>Null-safety for inputs</li>
 *   <li>File chooser with PDF filter + automatic ".pdf" extension</li>
 *   <li>Overwrite confirmation if the output file already exists</li>
 *   <li>Font loading that works across macOS/Windows/Linux with a safe fallback</li>
 *   <li>Automatic page breaks when the table reaches the bottom of the page</li>
 * </ul>
 */
@SuppressWarnings("DuplicatedCode")
public class OrderExport {

    private static final Logger LOGGER = LogManager.getLogger(OrderExport.class);

    // Layout constants
    private static final float MARGIN = 50f;
    private static final float HEADER_HEIGHT = 60f;
    private static final float BOX_HEIGHT = 85f;
    private static final float TABLE_HEADER_HEIGHT = 20f;
    private static final float ROW_HEIGHT = 18f;
    private static final float FOOTER_Y = 30f;
    private static final float MIN_Y_BEFORE_PAGE_BREAK = 150f;
    private static final float TABLE_CELL_PADDING = 8f;

    // Visual palette
    private static final Color COLOR_PRIMARY = new Color(30, 58, 95);
    private static final Color COLOR_PRIMARY_SOFT = new Color(62, 84, 98);
    private static final Color COLOR_TEXT = new Color(23, 33, 45);
    private static final Color COLOR_MUTED = new Color(130, 139, 149);
    private static final Color COLOR_SURFACE = new Color(245, 247, 250);
    private static final Color COLOR_ROW_ALT = new Color(250, 250, 250);
    private static final Color COLOR_LINE = new Color(210, 214, 220);
    private static final Color COLOR_SUCCESS = new Color(27, 94, 32);
    private static final Color COLOR_WARNING = new Color(230, 81, 0);

    private static final Locale LOCALE_CH = Locale.forLanguageTag("de-CH");
    private static final DecimalFormat CHF = new DecimalFormat("0.00", new DecimalFormatSymbols(LOCALE_CH));
    private static final DateTimeFormatter FOOTER_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Utility class: prevent instantiation. */
    private OrderExport() {
    }

    /**
     * Creates a PDF export of the given order and allows the user to choose the save location.
     *
     * @param frame parent JFrame for dialogs
     * @param order order to export
     */
    public static void createPDFExport(JFrame frame, Order order) {
        if (order == null) {
            new MessageDialog()
                    .setTitle("Keine Bestellung")
                    .setMessage("Keine Bestellung zum Exportieren ausgewählt.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        // Choose save location
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Bestellung als PDF speichern");
        fc.setFileFilter(new FileNameExtensionFilter("PDF (*.pdf)", "pdf"));
        fc.setAcceptAllFileFilterUsed(true);
        fc.setSelectedFile(new File(System.getProperty("user.home"), "order_" + safe(order.getOrderId()) + ".pdf"));

        int result = fc.showSaveDialog(frame);
        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File chosen = fc.getSelectedFile();
        File outputFile = ensurePdfExtension(chosen);

        // Confirm overwrite
        if (outputFile.exists()) {
            int overwrite = new MessageDialog()
                    .setTitle("Bestätigen")
                    .setMessage("Die Datei existiert bereits. Überschreiben?\n" + outputFile.getAbsolutePath())
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .setOptions(new String[]{"Ja", "Nein"})
                    .displayWithOptions();

            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        exportOrderToFile(outputFile, order);
    }

    public static void exportOrderToFile(File outputFile, Order order) {
        if (order == null) {
            new MessageDialog()
                    .setTitle("Keine Bestellung")
                    .setMessage("Keine Bestellung zum Exportieren vorhanden.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file cannot be null");
        }

        outputFile = ensurePdfExtension(outputFile);

        String orderId = safe(order.getOrderId());
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        String receiverName = safe(order.getReceiverName());
        String receiverKontoNumber = safe(order.getReceiverKontoNumber());
        String orderDate = safe(order.getOrderDate());
        String senderName = safe(order.getSenderName());
        String senderKontoNumber = safe(order.getSenderKontoNumber());
        String department = safe(order.getDepartment());
        String status = order.getStatus() != null ? order.getStatus() : "In Bearbeitung";

        if (orderedArticles == null || orderedArticles.isEmpty()) {
            new MessageDialog()
                    .setTitle("Keine Artikel")
                    .setMessage("Diese Bestellung enthält keine Artikel.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            return;
        }

        try (PDDocument doc = new PDDocument()) {

            Fonts fonts = loadFonts(doc);
            List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
            if (articles == null || articles.isEmpty()) {
                new MessageDialog()
                        .setTitle("Keine exportierbaren Artikel")
                        .setMessage("Diese Bestellung enthält keine exportierbaren Artikel.")
                        .setMessageType(JOptionPane.WARNING_MESSAGE)
                        .display();
                return;
            }

            double total = 0.0;
            boolean alternateRow = false;
            int articleIndex = 0;
            int pageNumber = 1;
            boolean firstPage = true;

            while (articleIndex < articles.size()) {
                PageState ps = new PageState(new PDPage());
                doc.addPage(ps.page);

                try (PDPageContentStream cs = new PDPageContentStream(doc, ps.page)) {
                    PDRectangle mediaBox = ps.page.getMediaBox();
                    float pageWidth = mediaBox.getWidth();
                    float pageHeight = mediaBox.getHeight();

                    ps.y = pageHeight - MARGIN;
                    ps.y = drawHeader(cs, fonts.bold, pageWidth, ps.y);

                    if (firstPage) {
                        ps.y = drawOrderInfo(cs, fonts, orderDate, orderId, status, ps.y);
                        ps.y = drawSenderReceiver(cs, fonts, pageWidth,
                                senderName, senderKontoNumber,
                                receiverName, receiverKontoNumber, department,
                                ps.y);
                    } else {
                        ps.y -= 10f;
                    }

                    TableColumns columns = createTableColumns(pageWidth);
                    ps.y = drawTableHeader(cs, fonts.bold, pageWidth, ps.y, columns);

                    while (articleIndex < articles.size() && ps.y >= MIN_Y_BEFORE_PAGE_BREAK) {
                        Article article = articles.get(articleIndex);

                        int qty = orderedArticles.getOrDefault(safe(article.getArticleNumber()), 0);
                        if (qty == 0) {
                            qty = orderedArticles.getOrDefault(safe(article.getName()), 0);
                        }

                        String filling = order.getArticleFilling(article.getArticleNumber());
                        double unit = ArticleUtils.resolveEffectiveSellPrice(article, filling);
                        double line = unit * qty;
                        total += line;

                        if (alternateRow) {
                            setFillColor(cs, COLOR_ROW_ALT);
                            cs.addRect(MARGIN, ps.y - 15, pageWidth - 2 * MARGIN, 18);
                            cs.fill();
                        }

                        drawArticleRow(cs, fonts, order, article, qty, unit, line, ps.y, columns);

                        ps.y -= ROW_HEIGHT;
                        alternateRow = !alternateRow;
                        articleIndex++;
                    }

                    if (articleIndex >= articles.size()) {
                        ps.y = drawTotals(cs, fonts.bold, pageWidth, ps.y, total);
                    }

                    drawFooter(cs, fonts.regular, pageNumber, pageWidth);
                }

                firstPage = false;
                pageNumber++;
            }

            doc.save(outputFile);

            new MessageDialog()
                    .setTitle("Erfolg")
                    .setMessage("PDF erfolgreich erstellt:\n" + outputFile.getAbsolutePath())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();

        } catch (Exception ex) {
            LOGGER.error("Fehler beim Erstellen des PDF-Exports", ex);
            new MessageDialog()
                    .setTitle("Fehler")
                    .setMessage("Fehler beim Erstellen des PDF-Dokuments:\n" + ex.getMessage())
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    private static float drawHeader(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y) throws Exception {
        setFillColor(cs, COLOR_PRIMARY);
        cs.addRect(MARGIN, y - HEADER_HEIGHT, pageWidth - 2 * MARGIN, HEADER_HEIGHT);
        cs.fill();

        cs.beginText();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldFont, 24);
        cs.newLineAtOffset(MARGIN + 10, y - 35);
        cs.showText("VEBO BESTELLUNG");
        cs.endText();

        cs.beginText();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldFont, 10);
        cs.newLineAtOffset(pageWidth - MARGIN - 165, y - 18);
        cs.showText("Bestelluebersicht");
        cs.endText();

        return y - (HEADER_HEIGHT + 20f);
    }

    private static float drawOrderInfo(PDPageContentStream cs, Fonts fonts, String orderDate, String orderId, String status, float y) throws Exception {
        // Bestelldatum
        text(cs, fonts.bold, 11, COLOR_TEXT, MARGIN, y, "Bestelldatum:");
        text(cs, fonts.regular, 11, COLOR_TEXT, MARGIN + 100, y, orderDate);
        y -= 18;

        // Bestell-ID
        text(cs, fonts.bold, 11, COLOR_TEXT, MARGIN, y, "Bestell-ID:");
        text(cs, fonts.regular, 11, COLOR_TEXT, MARGIN + 100, y, orderId);
        y -= 18;

        // Status
        text(cs, fonts.bold, 11, COLOR_TEXT, MARGIN, y, "Status:");

        if ("Abgeschlossen".equals(status)) {
            text(cs, fonts.bold, 11, COLOR_SUCCESS, MARGIN + 100, y, status);
        } else {
            text(cs, fonts.bold, 11, COLOR_WARNING, MARGIN + 100, y, status);
        }

        return y - 30;
    }

    private static float drawSenderReceiver(PDPageContentStream cs, Fonts fonts, float pageWidth,
                                           String senderName, String senderKonto,
                                           String receiverName, String receiverKonto, String department,
                                           float y) throws Exception {

        setFillColor(cs, COLOR_SURFACE);
        cs.addRect(MARGIN, y - BOX_HEIGHT, (pageWidth - 2 * MARGIN - 10) / 2, BOX_HEIGHT);
        cs.fill();

        setFillColor(cs, COLOR_SURFACE);
        cs.addRect(MARGIN + (pageWidth - 2 * MARGIN) / 2 + 5, y - BOX_HEIGHT, (pageWidth - 2 * MARGIN - 10) / 2, BOX_HEIGHT);
        cs.fill();

        // Sender
        text(cs, fonts.bold, 12, COLOR_TEXT, MARGIN + 10, y - 15, "ABSENDER");
        text(cs, fonts.regular, 10, COLOR_TEXT, MARGIN + 10, y - 32, senderName);
        text(cs, fonts.regular, 9, COLOR_TEXT, MARGIN + 10, y - 47, "Konto: " + senderKonto);

        // Receiver
        float rightBoxX = MARGIN + (pageWidth - 2 * MARGIN) / 2 + 15;
        text(cs, fonts.bold, 12, COLOR_TEXT, rightBoxX, y - 15, "EMPFAENGER");
        text(cs, fonts.regular, 10, COLOR_TEXT, rightBoxX, y - 32, receiverName);
        text(cs, fonts.regular, 9, COLOR_TEXT, rightBoxX, y - 47, "Konto: " + receiverKonto);
        text(cs, fonts.regular, 9, COLOR_TEXT, rightBoxX, y - 62, "Abteilung: " + department);

        return y - (BOX_HEIGHT + 30f);
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y,
                                         TableColumns columns) throws Exception {
        setFillColor(cs, COLOR_PRIMARY_SOFT);
        cs.addRect(MARGIN, y - TABLE_HEADER_HEIGHT, pageWidth - 2 * MARGIN, TABLE_HEADER_HEIGHT);
        cs.fill();

        text(cs, boldFont, 10, Color.WHITE, columns.articleX(), y - 14, "Artikel");
        text(cs, boldFont, 10, Color.WHITE, columns.qtyX(), y - 14, "Menge");
        text(cs, boldFont, 10, Color.WHITE, columns.unitX(), y - 14, "Einzelpreis");
        text(cs, boldFont, 10, Color.WHITE, columns.totalX(), y - 14, "Gesamt");

        return y - 25f;
    }

    private static void drawArticleRow(PDPageContentStream cs, Fonts fonts, Order order, Article article, int qty,
                       double unitPrice, double lineTotal, float y,
                       TableColumns columns) throws Exception {
    String articleLabel = truncate(order.formatArticleLabel(article), 34)
            + " (" + safe(article.getArticleNumber()) + ")";

    text(cs, fonts.regular, 9, COLOR_TEXT, columns.articleX(), y - 10, articleLabel);
    textRight(cs, fonts.regular, 9, COLOR_TEXT, columns.unitX() - TABLE_CELL_PADDING,
        y - 10, String.valueOf(qty));
    textRight(cs, fonts.regular, 9, COLOR_TEXT, columns.totalX() - TABLE_CELL_PADDING,
        y - 10, CHF.format(unitPrice) + " CHF");
    textRight(cs, fonts.regular, 9, COLOR_TEXT, columns.tableRightX() - TABLE_CELL_PADDING,
        y - 10, CHF.format(lineTotal) + " CHF");
    }

    private static float drawTotals(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y, double total) throws Exception {
        y -= 10;
    setStrokeColor(cs, COLOR_LINE);
        cs.setLineWidth(1);
        cs.moveTo(MARGIN, y);
        cs.lineTo(pageWidth - MARGIN, y);
        cs.stroke();

        y -= 25;

        setFillColor(cs, COLOR_PRIMARY);
        cs.addRect(pageWidth - MARGIN - 150, y - 25, 150, 30);
        cs.fill();

        cs.beginText();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldFont, 12);
        cs.newLineAtOffset(pageWidth - MARGIN - 140, y - 15);
        cs.showText("TOTAL:");
        cs.newLineAtOffset(50, 0);
        cs.showText(CHF.format(total) + " CHF");
        cs.endText();

        return y - 10;
    }

    private static void drawFooter(PDPageContentStream cs, PDFont regularFont, int pageNumber, float pageWidth) throws Exception {
        text(cs, regularFont, 8, COLOR_MUTED, MARGIN, FOOTER_Y,
                "VEBO Lagersystem - Generiert am " + LocalDateTime.now().format(FOOTER_TIMESTAMP_FORMATTER));
        textRight(cs, regularFont, 8, COLOR_MUTED, pageWidth - MARGIN, FOOTER_Y,
                "Seite " + pageNumber);
    }

    private static void text(PDPageContentStream cs, PDFont font, float size,
                             Color color,
                             float x, float y,
                             String text) throws Exception {
        cs.beginText();
        setFillColor(cs, color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private static void textRight(PDPageContentStream cs, PDFont font, float size, Color color,
                                  float rightX, float y, String value) throws Exception {
        String content = value == null ? "" : value;
        float textWidth = font.getStringWidth(content) / 1000f * size;
        text(cs, font, size, color, rightX - textWidth, y, content);
    }

    private static void setFillColor(PDPageContentStream cs, Color color) throws Exception {
        cs.setNonStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
    }

    private static void setStrokeColor(PDPageContentStream cs, Color color) throws Exception {
        cs.setStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
    }

    private static TableColumns createTableColumns(float pageWidth) {
        float tableLeftX = MARGIN;
        float tableRightX = pageWidth - MARGIN;
        float tableWidth = tableRightX - tableLeftX;

        float articleX = tableLeftX + TABLE_CELL_PADDING;
        float qtyX = tableLeftX + tableWidth * 0.62f;
        float unitX = tableLeftX + tableWidth * 0.75f;
        float totalX = tableLeftX + tableWidth * 0.89f;

        return new TableColumns(articleX, qtyX, unitX, totalX, tableRightX);
    }

    private static String truncate(String text, int maxLength) {
        String safeText = safe(text);
        if (safeText.length() <= maxLength) {
            return safeText;
        }
        return safeText.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private static String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    private static File ensurePdfExtension(File file) {
        String name = file.getName();
        if (!name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            return new File(file.getParentFile(), name + ".pdf");
        }
        return file;
    }

    /**
     * Font loading that tries OS-specific Arial paths first, then bundled resource, then PDFBox defaults.
     */
    private static Fonts loadFonts(PDDocument doc) {
        try {
            // macOS
            File macRegular = new File("/Library/Fonts/Arial.ttf");
            File macBold = new File("/Library/Fonts/Arial Bold.ttf");
            if (macRegular.exists()) {
                PDFont reg = PDType0Font.load(doc, macRegular);
                PDFont bold = macBold.exists() ? PDType0Font.load(doc, macBold) : reg;
                return new Fonts(reg, bold);
            }

            // Windows (common locations)
            File winRegular = new File("C:/Windows/Fonts/arial.ttf");
            File winBold = new File("C:/Windows/Fonts/arialbd.ttf");
            if (winRegular.exists()) {
                PDFont reg = PDType0Font.load(doc, winRegular);
                PDFont bold = winBold.exists() ? PDType0Font.load(doc, winBold) : reg;
                return new Fonts(reg, bold);
            }

            // Optional: bundled font inside resources (put e.g. /fonts/Arial.ttf in your jar)
            try (InputStream is = OrderExport.class.getResourceAsStream("/fonts/Arial.ttf")) {
                if (is != null) {
                    PDFont reg = PDType0Font.load(doc, is);
                    return new Fonts(reg, reg);
                }
            }

        } catch (Exception e) {
            LOGGER.warn("Konnte TrueType-Font nicht laden, nutze PDFBox-Fallback.", e);
        }

        return new Fonts(PDType1Font.HELVETICA, PDType1Font.HELVETICA_BOLD);
    }

    private record Fonts(PDFont regular, PDFont bold) {
    }

    private record TableColumns(float articleX, float qtyX, float unitX, float totalX, float tableRightX) {
    }

    private static final class PageState {
        private final PDPage page;
        private float y;

        private PageState(PDPage page) {
            this.page = page;
        }
    }
}
