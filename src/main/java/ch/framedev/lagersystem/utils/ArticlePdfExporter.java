package ch.framedev.lagersystem.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility to export a JTable with article data into a PDF file.
 */
public final class ArticlePdfExporter {

    private ArticlePdfExporter() {
    }

    public static void exportTableAsPdf(Component parent, JTable table, int[] baseColumnWidths, Icon icon) {
        if (table == null) {
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Export_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
        }

        try (PDDocument doc = new PDDocument()) {
            PDFont[] fonts = loadPdfFonts(doc);
            PDFont boldFont = fonts[0];
            PDFont regularFont = fonts[1];

            final boolean useWinAnsiFallback =
                    boldFont.getClass().getSimpleName().contains("PDType1Font") ||
                    regularFont.getClass().getSimpleName().contains("PDType1Font");

            // Page setup - Use A4 landscape for all columns to fit
            PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()); // Landscape
            float margin = 30;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            float tableWidth = pageWidth - 2 * margin;

            // Table configuration
            int numCols = table.getColumnCount();
            int numRows = table.getRowCount();
            float rowHeight = 16f;
            float headerHeight = 20f;
            float fontSize = 7f;
            float headerFontSize = 8f;
            float cellPadding = 3f;

            // Calculate column widths proportionally
            float[] columnWidths = new float[numCols];
            float totalWidth = 0;
            for (int i = 0; i < numCols; i++) {
                columnWidths[i] = i < baseColumnWidths.length ? baseColumnWidths[i] : 100;
                totalWidth += columnWidths[i];
            }
            for (int i = 0; i < numCols; i++) {
                columnWidths[i] = (columnWidths[i] / totalWidth) * tableWidth;
            }

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            float yPosition = pageHeight - margin;
            int currentPage = 1;
            boolean alternate = false;

            java.util.function.BiConsumer<PDPageContentStream, Float> drawHeader = (cs, yPos) -> {
                try {
                    cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
                    cs.addRect(margin, yPos - 45, tableWidth, 45);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.setFont(boldFont, 16);
                    cs.newLineAtOffset(margin + 10, yPos - 28);
                    cs.showText("VEBO Lagersystem - Artikelliste");
                    cs.endText();

                    cs.beginText();
                    cs.setFont(regularFont, 8);
                    cs.newLineAtOffset(pageWidth - margin - 120, yPos - 28);
                    cs.showText("Export: " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date()));
                    cs.endText();

                    float tableHeaderY = yPos - 55;
                    cs.setNonStrokingColor(62f / 255f, 84f / 255f, 98f / 255f);
                    cs.addRect(margin, tableHeaderY - headerHeight, tableWidth, headerHeight);
                    cs.fill();

                    cs.beginText();
                    cs.setNonStrokingColor(1f, 1f, 1f);
                    cs.setFont(boldFont, headerFontSize);

                    float xPos = margin;
                    for (int col = 0; col < numCols; col++) {
                        String header = table.getColumnName(col);
                        if (useWinAnsiFallback) {
                            header = sanitizeForWinAnsi(header);
                        }
                        float textWidth = boldFont.getStringWidth(header) / 1000f * headerFontSize;
                        if (textWidth > columnWidths[col] - 2 * cellPadding) {
                            while (textWidth > columnWidths[col] - 2 * cellPadding && header.length() > 2) {
                                header = header.substring(0, header.length() - 1);
                                textWidth = boldFont.getStringWidth(header + "..") / 1000f * headerFontSize;
                            }
                            header = header + "..";
                        }

                        cs.newLineAtOffset(xPos + cellPadding, tableHeaderY - headerHeight + cellPadding + 3);
                        cs.showText(header);
                        cs.newLineAtOffset(-(xPos + cellPadding), -(tableHeaderY - headerHeight + cellPadding + 3));
                        xPos += columnWidths[col];
                    }
                    cs.endText();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            java.util.function.BiConsumer<PDPageContentStream, Integer> drawFooter = (cs, pageNum) -> {
                try {
                    cs.beginText();
                    cs.setNonStrokingColor(0.4f, 0.4f, 0.4f);
                    cs.setFont(regularFont, 7);
                    cs.newLineAtOffset(margin, 15);
                    cs.showText("VEBO Lagersystem © 2026 | " + numRows + " Artikel");
                    cs.endText();

                    cs.beginText();
                    cs.newLineAtOffset(pageWidth - margin - 50, 15);
                    cs.showText("Seite " + pageNum);
                    cs.endText();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            };

            drawHeader.accept(contentStream, yPosition);
            yPosition -= (55 + headerHeight + 5);

            for (int row = 0; row < numRows; row++) {
                if (yPosition - rowHeight < margin + 30) {
                    drawFooter.accept(contentStream, currentPage);
                    contentStream.close();

                    currentPage++;
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    contentStream = new PDPageContentStream(doc, page);
                    yPosition = pageHeight - margin;

                    drawHeader.accept(contentStream, yPosition);
                    yPosition -= (55 + headerHeight + 5);
                }

                if (alternate) {
                    contentStream.setNonStrokingColor(247f / 255f, 250f / 255f, 253f / 255f);
                    contentStream.addRect(margin, yPosition - rowHeight, tableWidth, rowHeight);
                    contentStream.fill();
                }

                contentStream.setStrokingColor(220f / 255f, 225f / 255f, 230f / 255f);
                contentStream.setLineWidth(0.3f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(margin + tableWidth, yPosition);
                contentStream.stroke();

                contentStream.beginText();
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                contentStream.setFont(regularFont, fontSize);

                float xPos = margin;
                for (int col = 0; col < numCols; col++) {
                    Object value = table.getValueAt(row, col);
                    String text = "";

                    if (value != null) {
                        if (value instanceof Double) {
                            text = String.format("%.2f", (Double) value);
                        } else if (value instanceof Integer) {
                            text = String.format("%d", (Integer) value);
                        } else {
                            text = value.toString();
                        }
                    }

                    if (useWinAnsiFallback) {
                        text = sanitizeForWinAnsi(text);
                    }

                    float textWidth = regularFont.getStringWidth(text) / 1000f * fontSize;
                    if (textWidth > columnWidths[col] - 2 * cellPadding) {
                        while (textWidth > columnWidths[col] - 2 * cellPadding && text.length() > 2) {
                            text = text.substring(0, text.length() - 1);
                            textWidth = regularFont.getStringWidth(text + "..") / 1000f * fontSize;
                        }
                        text = text + "..";
                    }

                    contentStream.newLineAtOffset(xPos + cellPadding, yPosition - rowHeight + cellPadding + 2);
                    contentStream.showText(text);
                    contentStream.newLineAtOffset(-(xPos + cellPadding), -(yPosition - rowHeight + cellPadding + 2));
                    xPos += columnWidths[col];
                }
                contentStream.endText();

                yPosition -= rowHeight;
                alternate = !alternate;
            }

            contentStream.setStrokingColor(220f / 255f, 225f / 255f, 230f / 255f);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(margin + tableWidth, yPosition);
            contentStream.stroke();

            drawFooter.accept(contentStream, currentPage);

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(parent,
                    UnicodeSymbols.CHECKMARK + " PDF erfolgreich exportiert!\n\n" +
                            "Datei: " + fileToSave.getName() + "\n" +
                            "Pfad: " + fileToSave.getParent() + "\n" +
                            "Artikel: " + numRows + "\n" +
                            "Seiten: " + currentPage,
                    "Export erfolgreich",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    UnicodeSymbols.ERROR + " Fehler beim PDF-Export:\n\n" + ex.getMessage() +
                            "\n\nBitte überprüfen Sie die Schreibrechte und versuchen Sie es erneut.",
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    icon);
            ex.printStackTrace();
        }
    }

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

    private static PDFont[] loadPdfFonts(PDDocument doc) throws IOException {
        PDFont regular = null;
        PDFont bold = null;

        String[] regularCandidates = new String[] {
                "/Library/Fonts/Arial.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "C:/Windows/Fonts/arial.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf"
        };

        String[] boldCandidates = new String[] {
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
                    try (InputStream in = ArticlePdfExporter.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                        if (in != null) {
                            regular = PDType0Font.load(doc, in);
                        }
                    }
                }
                if (bold == null) {
                    try (InputStream in = ArticlePdfExporter.class.getResourceAsStream("/fonts/DejaVuSans-Bold.ttf")) {
                        if (in != null) {
                            bold = PDType0Font.load(doc, in);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (regular == null || bold == null) {
            try {
                Class<?> c = Class.forName("org.apache.pdfbox.pdmodel.font.PDType1Font");
                Object helvBold = c.getField("HELVETICA_BOLD").get(null);
                Object helv = c.getField("HELVETICA").get(null);
                bold = (PDFont) helvBold;
                regular = (PDFont) helv;
            } catch (Exception e) {
                throw new IOException("Keine verwendbaren Schriftarten gefunden");
            }
        }

        if (bold == null) {
            bold = regular;
        }
        if (regular == null) {
            regular = bold;
        }

        return new PDFont[] { bold, regular };
    }
}
