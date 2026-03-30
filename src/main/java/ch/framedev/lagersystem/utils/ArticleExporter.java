package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.dialogs.MessageDialog;
import ch.framedev.lagersystem.guis.MainGUI;
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
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Locale;

/**
 * Utility to export a JTable with article data into a PDF file.
 */
@SuppressWarnings("DuplicatedCode")
public final class ArticleExporter {

    private static final Logger LOGGER = LogManager.getLogger(ArticleExporter.class);
    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String PDF_EXTENSION = "pdf";
    private static final Color PDF_PRIMARY = new Color(30, 58, 95);
    private static final Color PDF_PRIMARY_SOFT = new Color(62, 84, 98);
    private static final Color PDF_ROW_ALT = new Color(247, 250, 253);
    private static final Color PDF_LINE = new Color(220, 225, 230);
    private static final Color PDF_TEXT = new Color(0, 0, 0);
    private static final Color PDF_TEXT_MUTED = new Color(102, 102, 102);

    private ArticleExporter() {
    }

    private static String nowFileTimestamp() {
        return LocalDateTime.now().format(FILE_TIMESTAMP_FORMATTER);
    }

    private static String nowDisplayTimestamp() {
        return LocalDateTime.now().format(DISPLAY_TIMESTAMP_FORMATTER);
    }

    private static String nowDisplayDate() {
        return LocalDateTime.now().format(DISPLAY_DATE_FORMATTER);
    }

    /**
     * Exports the contents of a JTable to a PDF file, allowing the user to choose the save location and file name. The PDF will include a header with the title and export date, a table with the article data, and a footer with page numbers and total article count.
     *
     * @param parent           The parent component for the file chooser dialog
     * @param table            The JTable containing the article data to be exported
     * @param baseColumnWidths An array of integers representing the base widths for each column, which will be used to calculate proportional column widths in the PDF. If the array has fewer entries than the number of columns in the table, default widths will be used for the remaining columns.
     * @param icon             An Icon to be used in error messages if the export process fails. This can be null if no icon is desired.
     */
    public static void exportTableAsPdf(Component parent, JTable table, int[] baseColumnWidths, Icon icon) {
        if(parent == null) return;
        if (table == null) return;
        if (baseColumnWidths == null) baseColumnWidths = new int[0];

        File fileToSave = chooseSaveFile(parent, "Artikel_Export_" + nowFileTimestamp() + "." + PDF_EXTENSION);
        if (fileToSave == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDFont[] fonts = loadPdfFonts(doc);
            PDFont boldFont = fonts[0];
            PDFont regularFont = fonts[1];
            boolean useWinAnsiFallback = (boldFont instanceof PDType1Font) || (regularFont instanceof PDType1Font);

            PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            float margin = 30f;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float tableWidth = pageWidth - 2 * margin;

            int numCols = table.getColumnCount();
            int numRows = table.getRowCount();
            float rowHeight = 16f;
            float tableHeaderHeight = 20f;
            float fontSize = 7f;
            float headerFontSize = 8f;
            float cellPadding = 3f;
            float[] columnWidths = calculateColumnWidths(numCols, baseColumnWidths, tableWidth);

            int currentPage = 1;
            int rowIndex = 0;
            boolean alternate = false;

            while (rowIndex < numRows) {
                PDPage page = new PDPage(pageSize);
                doc.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(doc, page)) {
                    float yPosition = pageHeight - margin;
                    yPosition = drawTableExportHeader(contentStream, table, boldFont, regularFont,
                            pageWidth, margin, tableWidth, yPosition, tableHeaderHeight,
                            headerFontSize, cellPadding, columnWidths, numCols, useWinAnsiFallback);

                    while (rowIndex < numRows && yPosition - rowHeight >= margin + 30) {
                        if (alternate) {
                            setFillColor(contentStream, PDF_ROW_ALT);
                            contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
                            contentStream.fill();
                        }

                        setStrokeColor(contentStream, PDF_LINE);
                        contentStream.setLineWidth(0.3f);
                        contentStream.moveTo(margin, yPosition);
                        contentStream.lineTo(margin + tableWidth, yPosition);
                        contentStream.stroke();

                        float xPos = margin;
                        for (int col = 0; col < numCols; col++) {
                            String cellText = formatCellValue(table.getValueAt(rowIndex, col));
                            if (useWinAnsiFallback) {
                                cellText = sanitizeForWinAnsi(cellText);
                            }
                            cellText = fitText(cellText, regularFont, fontSize, columnWidths[col] - 2 * cellPadding);

                            float textY = yPosition - rowHeight + cellPadding + 2;
                            if (isNumericValue(table.getValueAt(rowIndex, col))) {
                                drawRightAlignedText(contentStream, regularFont, fontSize, PDF_TEXT,
                                        xPos + columnWidths[col] - cellPadding, textY, cellText);
                            } else {
                                drawText(contentStream, regularFont, fontSize, PDF_TEXT,
                                        xPos + cellPadding, textY, cellText);
                            }
                            xPos += columnWidths[col];
                        }

                        yPosition -= rowHeight;
                        alternate = !alternate;
                        rowIndex++;
                    }

                    setStrokeColor(contentStream, PDF_LINE);
                    contentStream.setLineWidth(0.5f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(margin + tableWidth, yPosition);
                    contentStream.stroke();

                    drawTableExportFooter(contentStream, regularFont, pageWidth, margin, numRows, currentPage);
                }

                currentPage++;
            }

            doc.save(fileToSave);

            new MessageDialog()
                    .setTitle("Export erfolgreich")
                    .setMessage(UnicodeSymbols.CHECKMARK + " PDF erfolgreich exportiert!\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Artikel: " + numRows + "\n" +
                            "Seiten: " + currentPage)
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();

        } catch (Exception ex) {
            new MessageDialog()
                    .setTitle("Fehler beim PDF-Export")
                    .setMessage(UnicodeSymbols.ERROR + " Fehler beim PDF-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            LOGGER.error(ex.getMessage(), ex);
        }
    }

    private static float[] calculateColumnWidths(int numCols, int[] baseColumnWidths, float tableWidth) {
        float[] columnWidths = new float[numCols];
        float totalWidth = 0f;
        for (int i = 0; i < numCols; i++) {
            columnWidths[i] = i < baseColumnWidths.length ? Math.max(20, baseColumnWidths[i]) : 100;
            totalWidth += columnWidths[i];
        }
        if (totalWidth <= 0f) {
            float equal = tableWidth / Math.max(1, numCols);
            for (int i = 0; i < numCols; i++) columnWidths[i] = equal;
            return columnWidths;
        }
        for (int i = 0; i < numCols; i++) {
            columnWidths[i] = (columnWidths[i] / totalWidth) * tableWidth;
        }
        return columnWidths;
    }

    private static float drawTableExportHeader(PDPageContentStream cs, JTable table, PDFont boldFont, PDFont regularFont,
                                               float pageWidth, float margin, float tableWidth, float yPos,
                                               float tableHeaderHeight, float headerFontSize, float cellPadding,
                                               float[] columnWidths, int numCols, boolean useWinAnsiFallback) throws IOException {
        setFillColor(cs, PDF_PRIMARY);
        cs.addRect(margin, yPos - 45, tableWidth, 45);
        cs.fill();

        drawText(cs, boldFont, 16, Color.WHITE, margin + 10, yPos - 28, "VEBO Lagersystem - Artikelliste");
        drawText(cs, regularFont, 8, Color.WHITE, pageWidth - margin - 125, yPos - 28, "Export: " + nowDisplayTimestamp());

        float tableHeaderY = yPos - 55;
        setFillColor(cs, PDF_PRIMARY_SOFT);
        cs.addRect(margin, tableHeaderY - tableHeaderHeight, tableWidth, tableHeaderHeight);
        cs.fill();

        float xPos = margin;
        for (int col = 0; col < numCols; col++) {
            String header = table.getColumnName(col);
            if (useWinAnsiFallback) {
                header = sanitizeForWinAnsi(header);
            }
            header = fitText(header, boldFont, headerFontSize, columnWidths[col] - 2 * cellPadding);
            drawText(cs, boldFont, headerFontSize, Color.WHITE,
                    xPos + cellPadding, tableHeaderY - tableHeaderHeight + cellPadding + 3, header);
            xPos += columnWidths[col];
        }

        return yPos - (55 + tableHeaderHeight + 5);
    }

    private static void drawTableExportFooter(PDPageContentStream cs, PDFont regularFont, float pageWidth,
                                              float margin, int numRows, int pageNum) throws IOException {
        drawText(cs, regularFont, 7, PDF_TEXT_MUTED, margin, 15,
                "VEBO Lagersystem © 2026 | " + numRows + " Artikel");
        drawRightAlignedText(cs, regularFont, 7, PDF_TEXT_MUTED, pageWidth - margin, 15,
                "Seite " + pageNum);
    }

    private static void drawText(PDPageContentStream cs, PDFont font, float fontSize, Color color,
                                 float x, float y, String text) throws IOException {
        String value = sanitizeForFont(text, font);
        cs.beginText();
        setFillColor(cs, color);
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(value);
        cs.endText();
    }

    private static void drawRightAlignedText(PDPageContentStream cs, PDFont font, float fontSize, Color color,
                                             float rightX, float y, String text) throws IOException {
        String value = sanitizeForFont(text, font);
        float textWidth = font.getStringWidth(value) / 1000f * fontSize;
        drawText(cs, font, fontSize, color, rightX - textWidth, y, value);
    }

    private static String fitText(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String value = sanitizeForFont(text, font);
        if (maxWidth <= 0f) {
            return "";
        }
        float textWidth = font.getStringWidth(value) / 1000f * fontSize;
        if (textWidth <= maxWidth) {
            return value;
        }
        String shortened = value;
        while (shortened.length() > 2) {
            shortened = shortened.substring(0, shortened.length() - 1);
            float shortenedWidth = font.getStringWidth(shortened + "..") / 1000f * fontSize;
            if (shortenedWidth <= maxWidth) {
                return shortened + "..";
            }
        }
        return "..";
    }

    private static String formatCellValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double d) {
            return String.format(Locale.ROOT, "%.2f", d);
        }
        if (value instanceof Float f) {
            return String.format(Locale.ROOT, "%.2f", f);
        }
        if (value instanceof Integer i) {
            return String.format(Locale.ROOT, "%d", i);
        }
        if (value instanceof Long l) {
            return String.format(Locale.ROOT, "%d", l);
        }
        return String.valueOf(value);
    }

    private static boolean isNumericValue(Object value) {
        return value instanceof Number;
    }

    private static void setFillColor(PDPageContentStream cs, Color color) throws IOException {
        cs.setNonStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
    }

    private static void setStrokeColor(PDPageContentStream cs, Color color) throws IOException {
        cs.setStrokingColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);
    }

    /**
     * Sanitizes a string to ensure it only contains characters that can be represented in WinAnsi encoding.
     *
     * @param text The input text to sanitize
     * @return A sanitized version of the input text, where characters that cannot be represented in WinAnsi are removed. If the input text is null or empty, an empty string is returned.
     */
    public static String sanitizeForWinAnsi(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (cp <= 255) {
                sb.appendCodePoint(cp);
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static String sanitizeForFont(String text, PDFont font) {
        if (text == null || text.isEmpty() || font == null) {
            return text == null ? "" : text;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            String glyph = new String(Character.toChars(cp));
            try {
                font.encode(glyph);
                sb.append(glyph);
            } catch (IllegalArgumentException | IOException ignored) {
                // Skip glyphs the active PDF font cannot encode.
            }
            i += Character.charCount(cp);
        }
        return sb.toString();
    }

    private static PDFont[] loadPdfFonts(PDDocument doc) throws IOException {
        if(doc == null) throw new IllegalArgumentException("Document cannot be null");
        PDFont regular = null;
        PDFont bold = null;

        String[] regularCandidates = new String[]{
                "/Library/Fonts/Arial.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
        };

        String[] boldCandidates = new String[]{
                "/Library/Fonts/Arial Bold.ttf",
                "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
                "C:/Windows/Fonts/arialbd.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf"
        };

        for (String path : regularCandidates) {
            File file = new File(path);
            if (file.exists()) {
                regular = PDType0Font.load(doc, file);
                break;
            }
        }

        for (String path : boldCandidates) {
            File file = new File(path);
            if (file.exists()) {
                bold = PDType0Font.load(doc, file);
                break;
            }
        }

        if (regular == null || bold == null) {
            try {
                if (regular == null) {
                    try (InputStream in = ArticleExporter.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                        if (in != null) {
                            regular = PDType0Font.load(doc, in);
                        }
                    }
                }
                if (bold == null) {
                    try (InputStream in = ArticleExporter.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf")) {
                        if (in != null) {
                            bold = PDType0Font.load(doc, in);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (regular == null || bold == null) {
            // Last-resort fallback (limited glyph support)
            regular = PDType1Font.HELVETICA;
            bold = PDType1Font.HELVETICA_BOLD;
        }

        if (bold == null) {
            bold = regular;
        }
        if (regular == null) {
            regular = bold;
        }

        return new PDFont[]{bold, regular};
    }

    /**
     * Generates a PDF file representing an order, including sender and receiver information, department, and a list of ordered articles with their quantities and prices.
     *
     * @param file                 The file to which the PDF will be saved
     * @param senderNameCombobox   A JComboBox containing the sender's name (selected item will be used)
     * @param senderKontoField     A JTextField containing the sender's account information
     * @param receiverNameCombobox A JComboBox containing the receiver's name (selected item will be used)
     * @param receiverKontoField   A JTextField containing the receiver's account information
     * @param departmentList       A JComboBox containing the department information (selected item will be used)
     * @param orderArticles        A Map of Article objects to their ordered quantities, representing the articles included in the order
     * @throws IOException If an error occurs during PDF generation or saving the file
     */
    @SuppressWarnings("deprecation")
    public static void exportOrderToPDF(File file, JComboBox<String> receiverNameCombobox, JTextField receiverKontoField,
                                        JComboBox<String> senderNameCombobox, JTextField senderKontoField,
                                        JComboBox<String> departmentList, Map<Article, Integer> orderArticles,
                                        Map<String, String> orderArticleSizes,
                                        Map<String, String> orderArticleColors,
                                        Map<String, String> orderArticleFillings) throws IOException {
        if(file == null) throw new IllegalArgumentException("File cannot be null");
        if(receiverNameCombobox == null) throw new IllegalArgumentException("Receiver name combobox cannot be null");
        if(receiverKontoField == null) throw new IllegalArgumentException("Receiver konto field cannot be null");
        if(senderNameCombobox == null) throw new IllegalArgumentException("Sender name combobox cannot be null");
        if(senderKontoField == null) throw new IllegalArgumentException("Sender konto field cannot be null");
        if(departmentList == null) throw new IllegalArgumentException("Department list cannot be null");
        if(orderArticles == null) throw new IllegalArgumentException("Order articles cannot be null");
        if(orderArticles.isEmpty()) throw new IllegalArgumentException("Order articles cannot be empty");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            PDFont regularFont = null;
            PDFont boldFont = null;
            File regularTtf = new File("/Library/Fonts/Arial.ttf");
            File boldTtf = new File("/Library/Fonts/Arial Bold.ttf");

            if (regularTtf.exists()) {
                regularFont = PDType0Font.load(doc, regularTtf);
                if (boldTtf.exists()) {
                    boldFont = PDType0Font.load(doc, boldTtf);
                } else {
                    boldFont = regularFont;
                }
            } else {
                try {
                    Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                    Object helv = c.getField("HELVETICA").get(null);
                    Object helvBold = c.getField("HELVETICA_BOLD").get(null);
                    if (helv instanceof PDFont) regularFont = (PDFont) helv;
                    if (helvBold instanceof PDFont) boldFont = (PDFont) helvBold;
                    if (regularFont != null && boldFont == null) boldFont = regularFont;
                } catch (Exception ignored) {
                }
            }

            if (regularFont == null) {
                throw new IOException("No usable font found (install a system TTF or add PDFBox 2.x).");
            }

            final boolean useWinAnsiFallback =
                    regularFont.getClass().getSimpleName().contains("PDType1Font") ||
                            boldFont.getClass().getSimpleName().contains("PDType1Font");

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float margin = 50;
                float yPosition = 750;

                cs.setNonStrokingColor(30, 58, 95);
                cs.addRect(margin, yPosition, pageWidth - 2 * margin, 60);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 24);
                cs.newLineAtOffset(margin + 10, yPosition + 25);
                cs.showText("VEBO BESTELLUNG");
                cs.endText();

                yPosition -= 80;

                cs.beginText();
                cs.setNonStrokingColor(0, 0, 0);
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestelldatum:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(nowDisplayDate());
                cs.endText();

                yPosition -= 18;

                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestell-ID:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText("ORD" + System.currentTimeMillis());
                cs.endText();

                yPosition -= 30;

                float boxHeight = 85;
                cs.setNonStrokingColor(245, 247, 250);
                cs.addRect(margin, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.setNonStrokingColor(245, 247, 250);
                cs.addRect(margin + (pageWidth - 2 * margin) / 2 + 5, yPosition - boxHeight,
                        (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(0, 0, 0);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(margin + 10, yPosition - 15);
                cs.showText("ABSENDER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(margin + 10, yPosition - 32);
                cs.showText(sanitizeForWinAnsi(Objects.requireNonNull(senderNameCombobox.getSelectedItem()).toString(), useWinAnsiFallback));
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(margin + 10, yPosition - 47);
                cs.showText(sanitizeForWinAnsi("Konto: " + senderKontoField.getText().trim(), useWinAnsiFallback));
                cs.endText();

                float rightBoxX = margin + (pageWidth - 2 * margin) / 2 + 15;
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(rightBoxX, yPosition - 15);
                cs.showText("EMPFÄNGER");
                cs.endText();

                String receiver = receiverNameCombobox.getSelectedItem() != null
                        ? receiverNameCombobox.getSelectedItem().toString().trim()
                        : "";
                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(rightBoxX, yPosition - 32);
                cs.showText(sanitizeForWinAnsi(receiver, useWinAnsiFallback));
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 47);
                cs.showText(sanitizeForWinAnsi("Konto: " + receiverKontoField.getText().trim(), useWinAnsiFallback));
                cs.endText();

                String dept = departmentList.getSelectedItem() != null ? departmentList.getSelectedItem().toString().trim() : "";
                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 62);
                cs.showText(sanitizeForWinAnsi("Abteilung: " + dept, useWinAnsiFallback));
                cs.endText();

                yPosition -= boxHeight + 30;

                cs.setNonStrokingColor(62, 84, 98);
                cs.addRect(margin, yPosition - 20, pageWidth - 2 * margin, 20);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 10);
                cs.newLineAtOffset(margin + 5, yPosition - 14);
                cs.showText("Artikel");
                cs.newLineAtOffset(200, 0);
                cs.showText("Menge");
                cs.newLineAtOffset(60, 0);
                cs.showText("Einzelpreis");
                cs.newLineAtOffset(80, 0);
                cs.showText("Gesamt");
                cs.endText();

                yPosition -= 25;

                double total = 0.0;
                boolean alternateRow = false;
                for (Map.Entry<Article, Integer> e : orderArticles.entrySet()) {
                    Article a = e.getKey();
                    if (a == null) {
                        continue;
                    }
                    int qty = e.getValue();
                    String filling = (orderArticleFillings == null || a.getArticleNumber() == null)
                            ? ""
                            : ArticleUtils.normalizeFilling(orderArticleFillings.get(a.getArticleNumber()));
                    String size = getNormalizedMetadata(orderArticleSizes, a);
                    String color = getNormalizedMetadata(orderArticleColors, a);
                    double unit = safePrice(a, filling);
                    double line = unit * qty;
                    total += line;

                    if (alternateRow) {
                        cs.setNonStrokingColor(250, 250, 250);
                        cs.addRect(margin, yPosition - 15, pageWidth - 2 * margin, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0, 0, 0);
                    cs.setFont(regularFont, 9);
                    cs.newLineAtOffset(margin + 5, yPosition - 10);

                    String articleName = ArticleUtils.formatArticleWithFilling(a, filling);
                    if (!size.isBlank()) {
                        articleName += " (" + size + ")";
                    }
                    if (!color.isBlank()) {
                        articleName += " {" + color + "}";
                    }
                    if (articleName.length() > 35) articleName = articleName.substring(0, 32) + "...";
                    String articleLabel = articleName + " (" + a.getArticleNumber() + ")";

                    cs.showText(sanitizeForWinAnsi(articleLabel, useWinAnsiFallback));
                    cs.newLineAtOffset(200, 0);
                    cs.showText(String.valueOf(qty));
                    cs.newLineAtOffset(60, 0);
                    cs.showText(String.format(Locale.ROOT, "%.2f CHF", unit));
                    cs.newLineAtOffset(80, 0);
                    cs.showText(String.format(Locale.ROOT, "%.2f CHF", line));
                    cs.endText();

                    yPosition -= 18;
                    alternateRow = !alternateRow;

                    if (yPosition < 150) break;
                }

                yPosition -= 10;
                cs.setNonStrokingColor(200, 200, 200);
                cs.setLineWidth(1);
                cs.moveTo(margin, yPosition);
                cs.lineTo(pageWidth - margin, yPosition);
                cs.stroke();

                yPosition -= 25;

                cs.setNonStrokingColor(30, 58, 95);
                cs.addRect(pageWidth - margin - 150, yPosition - 25, 150, 30);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(255, 255, 255);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(pageWidth - margin - 140, yPosition - 15);
                cs.showText("TOTAL:");
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format(Locale.ROOT, "%.2f CHF", total));
                cs.endText();

                cs.beginText();
                cs.setNonStrokingColor(150, 150, 150);
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(margin, 30);
                cs.showText("VEBO Lagersystem - Generiert am " + nowDisplayTimestamp());
                cs.endText();
            }

            doc.save(file);
        }
    }

    /**
     * Safely retrieves the selling price of an article, returning 0.0 if any exceptions occur (e.g., method not found).
     *
     * @param a The article for which to retrieve the selling price
     * @return The selling price of the article, or 0.0 if it cannot be retrieved due to an exception
     */
    private static double safePrice(Article a, String filling) {
        if(a == null) throw new IllegalArgumentException("Article cannot be null");
        try {
            return ArticleUtils.resolveEffectiveSellPrice(a, filling);
        } catch (NoSuchMethodError | AbstractMethodError | RuntimeException ex) {
            return a.getSellPrice();
        }
    }

    private static String getNormalizedMetadata(Map<String, String> metadataMap, Article article) {
        if (metadataMap == null || article == null || article.getArticleNumber() == null) {
            return "";
        }

        String value = metadataMap.get(article.getArticleNumber());
        if (value == null) {
            return "";
        }

        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()
                || normalized.equalsIgnoreCase("n/a")
                || normalized.equalsIgnoreCase("null")
                || normalized.equals("-")) {
            return "";
        }

        return normalized;
    }

    /**
     * Sanitizes a string to ensure it only contains characters that can be represented in WinAnsi encoding.
     *
     * @param text               The input text to sanitize
     * @param useWinAnsiFallback Whether to perform the sanitization. If false, the original text is returned without modification.
     * @return A sanitized version of the input text, where characters that cannot be represented in WinAnsi are removed if useWinAnsiFallback is true. If useWinAnsiFallback is false, the original text is returned unchanged.
     */
    private static String sanitizeForWinAnsi(String text, boolean useWinAnsiFallback) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return useWinAnsiFallback ? sanitizeForWinAnsi(text) : text;
    }

    /**
     * Exports the given list of articles to a PDF file, prompting the user for the save location.
     *
     * @param articles Die Liste der Artikel, die exportiert werden sollen
     */
    public static void exportArticlesToPdf(java.util.List<Article> articles) {
        if(articles == null) throw new IllegalArgumentException("Articles cannot be null");
        File fileToSave = chooseSaveFile(MainGUI.articleGUI,
            "Artikel_Auswahl_" + nowFileTimestamp() + "." + PDF_EXTENSION
        );
        if (fileToSave == null) {
            return;
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont = PDType1Font.HELVETICA;

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40f;
            float fontSize = 10f;
            float lineHeight = 14f;
            float yStart = pageSize.getHeight() - margin;
            float maxWidth = pageSize.getWidth() - 2 * margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.setFont(regularFont, fontSize);

            float y = yStart;
            for (Article article : articles) {
                String line = article.getArticleNumber() + " | " + article.getName() + " | Lager: " +
                        article.getStockQuantity() + " | Min: " + article.getMinStockLevel() +
                        " | VK: " + article.getSellPrice() + " | EK: " + article.getPurchasePrice() +
                        " | Lieferant: " + article.getVendorName();
                List<String> wrapped = wrapLine(line, regularFont, fontSize, maxWidth);
                for (String part : wrapped) {
                    if (y - lineHeight < margin) {
                        contentStream.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        contentStream = new PDPageContentStream(doc, page);
                        contentStream.setFont(regularFont, fontSize);
                        y = yStart;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText(part);
                    contentStream.endText();
                    y -= lineHeight;
                }
            }

            contentStream.close();
            doc.save(fileToSave);
            new MessageDialog()
                    .setTitle("Export erfolgreich")
                    .setMessage(UnicodeSymbols.CHECKMARK + " PDF erfolgreich exportiert!\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Artikel: " + articles.size())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } catch (Exception ex) {
            new MessageDialog()
                    .setTitle("Fehler beim PDF-Export")
                    .setMessage(UnicodeSymbols.ERROR + " Fehler beim PDF-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    /**
     * Wraps a single line of text into multiple lines based on the specified font, font size, and maximum width.
     *
     * @param text     The text to wrap
     * @param font     The PDFont used to measure text width
     * @param fontSize The font size used for the text, needed to calculate the width in user space units
     * @param maxWidth The maximum width in user space units that a line can occupy before wrapping
     * @return A list of strings, each representing a line of wrapped text that fits within the specified width
     * @throws IOException If an error occurs while measuring text width with the PDFont
     */
    private static List<String> wrapLine(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String candidate = current.isEmpty() ? word : current + " " + word;
            float width = font.getStringWidth(candidate) / 1000f * fontSize;
            if (width > maxWidth && !current.isEmpty()) {
                lines.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            lines.add(current.toString());
        }
        return lines;
    }

    /**
     * Exports the given list of articles to a CSV file, prompting the user for the save location.
     *
     * @param articles Die Liste der Artikel, die exportiert werden sollen
     */
    public static void exportArticlesToCsv(List<Article> articles) {
        if(articles == null) throw new IllegalArgumentException("Articles cannot be null");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Auswahl_" + nowFileTimestamp() + ".csv"));
        int userSelection = fileChooser.showSaveDialog(MainGUI.articleGUI);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
        }

        try (var writer = Files.newBufferedWriter(fileToSave.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Artikelnummer,Name,Details,Lagerbestand,Mindestbestand,Verkaufspreis,Einkaufspreis,Lieferant");
            writer.write(System.lineSeparator());
            for (Article article : articles) {
                writer.write(escapeCsv(article.getArticleNumber()));
                writer.write(",");
                writer.write(escapeCsv(article.getName()));
                writer.write(",");
                writer.write(escapeCsv(article.getDetails()));
                writer.write(",");
                writer.write(String.valueOf(article.getStockQuantity()));
                writer.write(",");
                writer.write(String.valueOf(article.getMinStockLevel()));
                writer.write(",");
                writer.write(String.valueOf(article.getSellPrice()));
                writer.write(",");
                writer.write(String.valueOf(article.getPurchasePrice()));
                writer.write(",");
                writer.write(escapeCsv(article.getVendorName()));
                writer.write(System.lineSeparator());
            }
            new MessageDialog()
                    .setTitle("Export erfolgreich")
                    .setMessage(UnicodeSymbols.CHECKMARK + " CSV erfolgreich exportiert:\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Artikel: " + articles.size())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } catch (IOException ex) {
            new MessageDialog()
                    .setTitle("Fehler beim CSV-Export")
                    .setMessage(UnicodeSymbols.ERROR + " Fehler beim CSV-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
        }
    }

    /**
     * Escapes a value for CSV output, adding quotes if necessary and doubling internal quotes.
     *
     * @param value The value to escape
     * @return The escaped value, ready for CSV output
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Exports the given list of log entries to a PDF file, prompting the user for the save location. Each log entry is wrapped to fit within the page width, and multiple pages are created if necessary.
     *
     * @param logs  Die Liste der Log-Einträge, die exportiert werden sollen. Jeder Eintrag wird als separate Zeile im PDF dargestellt.
     * @param frame Das übergeordnete JFrame, das als Kontext für die Dateiauswahl und Fehlermeldungen verwendet wird. Kann null sein, wenn kein Kontext benötigt wird.
     */
    public static void exportLogsToPdf(List<String> logs, JFrame frame) {
        if(frame == null) throw new IllegalArgumentException("Frame cannot be null");
        if (logs == null || logs.isEmpty()) {
            new MessageDialog()
                    .setTitle("PDF Export")
                    .setMessage(UnicodeSymbols.WARNING + " Keine Logs zum Export vorhanden.")
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .display();
            LOGGER.warn("No Logs found to export!");
            return;
        }

        File fileToSave = chooseSaveFile(frame,
            "Logs_Export_" + nowFileTimestamp() + "." + PDF_EXTENSION
        );
        if (fileToSave == null) {
            return;
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont;
            File arial = new File("/Library/Fonts/Arial.ttf");
            if (arial.exists()) {
                regularFont = PDType0Font.load(doc, arial);
            } else {
                try {
                    Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                    Object helv = c.getField("HELVETICA").get(null);
                    regularFont = (PDFont) helv;
                } catch (Exception e) {
                    LOGGER.error("No usable Font found!", e);
                    throw new IOException("Keine verwendbaren Schriftarten gefunden");
                }
            }

            PDRectangle pageSize = PDRectangle.A4;
            float margin = 40f;
            float fontSize = 10f;
            float lineHeight = 14f;
            float yStart = pageSize.getHeight() - margin;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);
            contentStream.setFont(regularFont, fontSize);

            float y = yStart;
            for (String line : logs) {
                List<String> wrapped = wrapLine(line, regularFont, fontSize, pageSize.getWidth() - 2 * margin);
                for (String part : wrapped) {
                    if (y - lineHeight < margin) {
                        contentStream.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        contentStream = new PDPageContentStream(doc, page);
                        contentStream.setFont(regularFont, fontSize);
                        y = yStart;
                    }
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText(part);
                    contentStream.endText();
                    y -= lineHeight;
                }
            }

            contentStream.close();
            doc.save(fileToSave);

            new MessageDialog()
                    .setTitle("PDF Export")
                    .setMessage(UnicodeSymbols.CHECKMARK + " PDF erfolgreich exportiert:\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Logs: " + logs.size())
                    .setMessageType(JOptionPane.INFORMATION_MESSAGE)
                    .display();
        } catch (Exception ex) {
            new MessageDialog()
                    .setTitle("Fehler beim PDF-Export")
                    .setMessage(UnicodeSymbols.ERROR + " Fehler beim PDF-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.")
                    .setMessageType(JOptionPane.ERROR_MESSAGE)
                    .display();
            LOGGER.error("Could not create PDF-Export {}", ex.getMessage(), ex);
        }
    }

    // Helper to choose a save file with extension filter and overwrite confirmation
    private static File chooseSaveFile(Component parent, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File(defaultFileName));
        fileChooser.setFileFilter(new FileNameExtensionFilter(
            PDF_EXTENSION.toUpperCase(Locale.ROOT) + " Dateien (*." + PDF_EXTENSION + ")",
            PDF_EXTENSION
        ));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File selected = fileChooser.getSelectedFile();
        if (selected == null) {
            return null;
        }

        String nameLower = selected.getName().toLowerCase(Locale.ROOT);
        if (!nameLower.endsWith("." + PDF_EXTENSION)) {
            selected = new File(selected.getAbsolutePath() + "." + PDF_EXTENSION);
        }

        if (selected.exists()) {
            int overwrite = new MessageDialog()
                    .setTitle("Bestätigen")
                    .setMessage(UnicodeSymbols.WARNING + " Die Datei existiert bereits. Überschreiben?\n\n" + selected.getName())
                    .setMessageType(JOptionPane.WARNING_MESSAGE)
                    .displayWithOptions();
            if (overwrite != JOptionPane.YES_OPTION) {
                return null;
            }
        }

        return selected;
    }
}
