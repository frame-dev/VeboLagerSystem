package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Utility for exporting selected articles to a simple PDF list.
 */
public final class ArticleSelectionPdfExporter {

    private ArticleSelectionPdfExporter() {
    }

    public static void exportSelectedArticlesPdf(Component parent, List<Article> articles, Icon icon) {
        if (articles == null || articles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Auswahl_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));
        int userSelection = fileChooser.showSaveDialog(parent);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".pdf")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".pdf");
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
                line = ArticlePdfExporter.sanitizeForWinAnsi(line);
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

            JOptionPane.showMessageDialog(parent,
                    "PDF erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "Export",
                    JOptionPane.INFORMATION_MESSAGE,
                    icon);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Fehler beim PDF-Export: " + ex.getMessage(),
                    "Export",
                    JOptionPane.ERROR_MESSAGE,
                    icon);
        }
    }

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
}
