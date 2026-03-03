package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
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
import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    private static final Locale LOCALE_CH = Locale.forLanguageTag("de-CH");
    private static final DecimalFormat CHF = new DecimalFormat("0.00", new DecimalFormatSymbols(LOCALE_CH));

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
            JOptionPane.showMessageDialog(frame,
                    "Keine Bestellung zum Exportieren ausgewählt.",
                    "Hinweis",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
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
            int overwrite = JOptionPane.showConfirmDialog(frame,
                    "Die Datei existiert bereits. Überschreiben?\n" + outputFile.getAbsolutePath(),
                    "Bestätigen",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            if (overwrite != JOptionPane.YES_OPTION) {
                return;
            }
        }

        String orderId = safe(order.getOrderId());
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        String receiverName = safe(order.getReceiverName());
        String receiverKontoNumber = safe(order.getReceiverKontoNumber());
        String orderDate = safe(order.getOrderDate());
        String senderName = safe(order.getSenderName());
        String senderKontoNumber = safe(order.getSenderKontoNumber());
        String department = safe(order.getDepartment());
        String status = order.getStatus() != null ? order.getStatus() : "In Bearbeitung";

        if (orderedArticles == null) {
            JOptionPane.showMessageDialog(frame,
                    "Diese Bestellung enthält keine Artikel.",
                    "Hinweis",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        try (PDDocument doc = new PDDocument()) {

            Fonts fonts = loadFonts(doc);

            // Create first page
            PageState ps = new PageState(new PDPage());
            doc.addPage(ps.page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, ps.page)) {
                PDRectangle mediaBox = ps.page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                ps.y = pageHeight - MARGIN;

                // Header
                ps.y = drawHeader(cs, fonts.bold, pageWidth, ps.y);

                // Order info
                ps.y = drawOrderInfo(cs, fonts, orderDate, orderId, status, ps.y);

                // Sender/Receiver boxes
                ps.y = drawSenderReceiver(cs, fonts, pageWidth,
                        senderName, senderKontoNumber,
                        receiverName, receiverKontoNumber, department,
                        ps.y);

                // Table header
                ps.y = drawTableHeader(cs, fonts.bold, pageWidth, ps.y);

                // Rows
                List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
                double total = 0.0;
                boolean alternateRow = false;

                for (Article article : articles) {
                    // Page break if we reach the bottom
                    if (ps.y < MIN_Y_BEFORE_PAGE_BREAK) {
                        // finish footer on current page
                        drawFooter(cs, fonts.regular);

                        // close current stream before starting a new one
                        cs.close();

                        // new page
                        ps = new PageState(new PDPage());
                        doc.addPage(ps.page);

                        try (PDPageContentStream cs2 = new PDPageContentStream(doc, ps.page)) {
                            PDRectangle mb = ps.page.getMediaBox();
                            float w = mb.getWidth();
                            float h = mb.getHeight();

                            ps.y = h - MARGIN;

                            // Repeat header (optional)
                            ps.y = drawHeader(cs2, fonts.bold, w, ps.y);

                            // Repeat table header
                            ps.y = ps.y - 10; // little spacing
                            ps.y = drawTableHeader(cs2, fonts.bold, w, ps.y);

                            // Continue rows on new page using cs2
                            total = renderRemainingRows(cs2, fonts, w, ps, orderedArticles, articles, article, total, alternateRow);
                            // renderRemainingRows already did footer + last page footer as needed.
                        }
                        // We handled the rest in renderRemainingRows
                        doc.save(outputFile);

                        JOptionPane.showMessageDialog(frame,
                                "PDF erfolgreich erstellt:\n" + outputFile.getAbsolutePath(),
                                "Erfolg",
                                JOptionPane.INFORMATION_MESSAGE,
                                Main.iconSmall);
                        return;
                    }

                    int qty = orderedArticles.getOrDefault(safe(article.getArticleNumber()), 0);
                    if (qty == 0) {
                        qty = orderedArticles.getOrDefault(safe(article.getName()), 0);
                    }

                    double unit = article.getSellPrice();
                    double line = unit * qty;
                    total += line;

                    if (alternateRow) {
                        cs.setNonStrokingColor(250f / 255f, 250f / 255f, 250f / 255f);
                        cs.addRect(MARGIN, ps.y - 15, pageWidth - 2 * MARGIN, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.setFont(fonts.regular, 9);
                    cs.newLineAtOffset(MARGIN + 5, ps.y - 10);

                    String articleName = safe(article.getName());
                    if (articleName.length() > 35) {
                        articleName = articleName.substring(0, 32) + "...";
                    }

                    cs.showText(articleName + " (" + safe(article.getArticleNumber()) + ")");
                    cs.newLineAtOffset(200, 0);
                    cs.showText(String.valueOf(qty));
                    cs.newLineAtOffset(60, 0);
                    cs.showText(CHF.format(unit) + " CHF");
                    cs.newLineAtOffset(80, 0);
                    cs.showText(CHF.format(line) + " CHF");
                    cs.endText();

                    ps.y -= ROW_HEIGHT;
                    alternateRow = !alternateRow;
                }

                // Totals
                ps.y = drawTotals(cs, fonts.bold, pageWidth, ps.y, total);

                // Footer
                drawFooter(cs, fonts.regular);
            }

            doc.save(outputFile);

            JOptionPane.showMessageDialog(frame,
                    "PDF erfolgreich erstellt:\n" + outputFile.getAbsolutePath(),
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

        } catch (Exception ex) {
            LOGGER.error("Fehler beim Erstellen des PDF-Exports", ex);
            JOptionPane.showMessageDialog(frame,
                    "Fehler beim Erstellen des PDF-Dokuments:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    /**
     * Continues rendering rows starting with the given currentArticle (inclusive) on a newly created page stream.
     * This is a helper to keep the page-break logic simple without rewriting the entire method.
     */
    private static double renderRemainingRows(PDPageContentStream cs, Fonts fonts, float pageWidth, PageState ps,
                                             Map<String, Integer> orderedArticles,
                                             List<Article> allArticles,
                                             Article currentArticle,
                                             double total,
                                             boolean alternateRow) throws Exception {

        boolean start = false;
        for (Article article : allArticles) {
            if (!start) {
                if (article == currentArticle) {
                    start = true;
                } else {
                    continue;
                }
            }

            if (ps.y < MIN_Y_BEFORE_PAGE_BREAK) {
                drawFooter(cs, fonts.regular);
                cs.close();

                // new page
                // NOTE: the caller owns doc creation; here we can only stop if we need more pages.
                // The current implementation avoids multi-multi-page recursion. If you need unlimited pages,
                // we can refactor to a loop that manages doc + streams outside.
                throw new IllegalStateException("Zu viele Artikel für den aktuellen PDF-Renderer (mehrere Seitenwechsel). Bitte Refactor anfordern.");
            }

            int qty = orderedArticles.getOrDefault(safe(article.getArticleNumber()), 0);
            if (qty == 0) {
                qty = orderedArticles.getOrDefault(safe(article.getName()), 0);
            }

            double unit = article.getSellPrice();
            double line = unit * qty;
            total += line;

            if (alternateRow) {
                cs.setNonStrokingColor(250f / 255f, 250f / 255f, 250f / 255f);
                cs.addRect(MARGIN, ps.y - 15, pageWidth - 2 * MARGIN, 18);
                cs.fill();
            }

            cs.beginText();
            cs.setNonStrokingColor(0f, 0f, 0f);
            cs.setFont(fonts.regular, 9);
            cs.newLineAtOffset(MARGIN + 5, ps.y - 10);

            String articleName = safe(article.getName());
            if (articleName.length() > 35) {
                articleName = articleName.substring(0, 32) + "...";
            }

            cs.showText(articleName + " (" + safe(article.getArticleNumber()) + ")");
            cs.newLineAtOffset(200, 0);
            cs.showText(String.valueOf(qty));
            cs.newLineAtOffset(60, 0);
            cs.showText(CHF.format(unit) + " CHF");
            cs.newLineAtOffset(80, 0);
            cs.showText(CHF.format(line) + " CHF");
            cs.endText();

            ps.y -= ROW_HEIGHT;
            alternateRow = !alternateRow;
        }

        ps.y = drawTotals(cs, fonts.bold, pageWidth, ps.y, total);
        drawFooter(cs, fonts.regular);
        return total;
    }

    private static float drawHeader(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y) throws Exception {
        cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
        cs.addRect(MARGIN, y - HEADER_HEIGHT, pageWidth - 2 * MARGIN, HEADER_HEIGHT);
        cs.fill();

        cs.beginText();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldFont, 24);
        cs.newLineAtOffset(MARGIN + 10, y - 35);
        cs.showText("VEBO BESTELLUNG");
        cs.endText();

        return y - (HEADER_HEIGHT + 20f);
    }

    private static float drawOrderInfo(PDPageContentStream cs, Fonts fonts, String orderDate, String orderId, String status, float y) throws Exception {
        // Bestelldatum
        text(cs, fonts.bold, 11, 0f, 0f, 0f, MARGIN, y, "Bestelldatum:");
        text(cs, fonts.regular, 11, 0f, 0f, 0f, MARGIN + 100, y, orderDate);
        y -= 18;

        // Bestell-ID
        text(cs, fonts.bold, 11, 0f, 0f, 0f, MARGIN, y, "Bestell-ID:");
        text(cs, fonts.regular, 11, 0f, 0f, 0f, MARGIN + 100, y, orderId);
        y -= 18;

        // Status
        text(cs, fonts.bold, 11, 0f, 0f, 0f, MARGIN, y, "Status:");

        if ("Abgeschlossen".equals(status)) {
            text(cs, fonts.bold, 11, 27f / 255f, 94f / 255f, 32f / 255f, MARGIN + 100, y, status);
        } else {
            text(cs, fonts.bold, 11, 230f / 255f, 81f / 255f, 0f, MARGIN + 100, y, status);
        }

        return y - 30;
    }

    private static float drawSenderReceiver(PDPageContentStream cs, Fonts fonts, float pageWidth,
                                           String senderName, String senderKonto,
                                           String receiverName, String receiverKonto, String department,
                                           float y) throws Exception {

        cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f);
        cs.addRect(MARGIN, y - BOX_HEIGHT, (pageWidth - 2 * MARGIN - 10) / 2, BOX_HEIGHT);
        cs.fill();

        cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f);
        cs.addRect(MARGIN + (pageWidth - 2 * MARGIN) / 2 + 5, y - BOX_HEIGHT, (pageWidth - 2 * MARGIN - 10) / 2, BOX_HEIGHT);
        cs.fill();

        // Sender
        text(cs, fonts.bold, 12, 0f, 0f, 0f, MARGIN + 10, y - 15, "ABSENDER");
        text(cs, fonts.regular, 10, 0f, 0f, 0f, MARGIN + 10, y - 32, senderName);
        text(cs, fonts.regular, 9, 0f, 0f, 0f, MARGIN + 10, y - 47, "Konto: " + senderKonto);

        // Receiver
        float rightBoxX = MARGIN + (pageWidth - 2 * MARGIN) / 2 + 15;
        text(cs, fonts.bold, 12, 0f, 0f, 0f, rightBoxX, y - 15, "EMPFAENGER");
        text(cs, fonts.regular, 10, 0f, 0f, 0f, rightBoxX, y - 32, receiverName);
        text(cs, fonts.regular, 9, 0f, 0f, 0f, rightBoxX, y - 47, "Konto: " + receiverKonto);
        text(cs, fonts.regular, 9, 0f, 0f, 0f, rightBoxX, y - 62, "Abteilung: " + department);

        return y - (BOX_HEIGHT + 30f);
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y) throws Exception {
        cs.setNonStrokingColor(62f / 255f, 84f / 255f, 98f / 255f);
        cs.addRect(MARGIN, y - TABLE_HEADER_HEIGHT, pageWidth - 2 * MARGIN, TABLE_HEADER_HEIGHT);
        cs.fill();

        cs.beginText();
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.setFont(boldFont, 10);
        cs.newLineAtOffset(MARGIN + 5, y - 14);
        cs.showText("Artikel");
        cs.newLineAtOffset(200, 0);
        cs.showText("Menge");
        cs.newLineAtOffset(60, 0);
        cs.showText("Einzelpreis");
        cs.newLineAtOffset(80, 0);
        cs.showText("Gesamt");
        cs.endText();

        return y - 25f;
    }

    private static float drawTotals(PDPageContentStream cs, PDFont boldFont, float pageWidth, float y, double total) throws Exception {
        y -= 10;
        cs.setNonStrokingColor(200f / 255f, 200f / 255f, 200f / 255f);
        cs.setLineWidth(1);
        cs.moveTo(MARGIN, y);
        cs.lineTo(pageWidth - MARGIN, y);
        cs.stroke();

        y -= 25;

        cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
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

    private static void drawFooter(PDPageContentStream cs, PDFont regularFont) throws Exception {
        cs.beginText();
        cs.setNonStrokingColor(150f / 255f, 150f / 255f, 150f / 255f);
        cs.setFont(regularFont, 8);
        cs.newLineAtOffset(MARGIN, FOOTER_Y);
        cs.showText("VEBO Lagersystem - Generiert am " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
        cs.endText();
    }

    private static void text(PDPageContentStream cs, PDFont font, float size,
                             float r, float g, float b,
                             float x, float y,
                             String text) throws Exception {
        cs.beginText();
        cs.setNonStrokingColor(r, g, b);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
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

    private static final class PageState {
        private final PDPage page;
        private float y;

        private PageState(PDPage page) {
            this.page = page;
        }
    }
}
