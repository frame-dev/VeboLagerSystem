package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.OrderManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Map;

@SuppressWarnings("DuplicatedCode")
public class OrderExport {

    public static void createPDFExport(JFrame frame, Order order) {
        // Choose save location
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File(System.getProperty("user.home"), "order_" + order.getOrderId() + ".pdf"));
        int result = fc.showSaveDialog(frame);

        if (result != JFileChooser.APPROVE_OPTION) return;

        File outputFile = fc.getSelectedFile();

        String orderId = order.getOrderId();
        Map<String, Integer> orderedArticles = order.getOrderedArticles();
        String receiverName = order.getReceiverName();
        String receiverKontoNumber = order.getReceiverKontoNumber();
        String orderDate = order.getOrderDate();
        String senderName = order.getSenderName();
        String senderKontoNumber = order.getSenderKontoNumber();
        String department = order.getDepartment();
        String status = order.getStatus() != null ? order.getStatus() : "In Bearbeitung";

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // Fonts (macOS path first, then fallback)
            PDFont regularFont;
            PDFont boldFont;
            File regularTtf = new File("/Library/Fonts/Arial.ttf");
            File boldTtf = new File("/Library/Fonts/Arial Bold.ttf");

            if (regularTtf.exists()) {
                regularFont = PDType0Font.load(doc, regularTtf);
                boldFont = boldTtf.exists() ? PDType0Font.load(doc, boldTtf) : regularFont;
            } else {
                regularFont = PDType1Font.HELVETICA;
                boldFont = PDType1Font.HELVETICA_BOLD;
            }

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float pageWidth = page.getMediaBox().getWidth();
                float margin = 50;
                float yPosition = page.getMediaBox().getHeight() - margin;

                // Header
                cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
                cs.addRect(margin, yPosition - 60, pageWidth - 2 * margin, 60);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(boldFont, 24);
                cs.newLineAtOffset(margin + 10, yPosition - 35);
                cs.showText("VEBO BESTELLUNG");
                cs.endText();

                yPosition -= 80;

                // Order info
                cs.beginText();
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Bestelldatum:");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 11);
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(orderDate);
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
                cs.showText(orderId);
                cs.endText();

                yPosition -= 18;

                cs.beginText();
                cs.setFont(boldFont, 11);
                cs.newLineAtOffset(margin, yPosition);
                cs.showText("Status:");
                cs.endText();

                cs.beginText();
                cs.setFont(boldFont, 11);
                if ("Abgeschlossen".equals(status)) {
                    cs.setNonStrokingColor(27f / 255f, 94f / 255f, 32f / 255f);
                } else {
                    cs.setNonStrokingColor(230f / 255f, 81f / 255f, 0f);
                }
                cs.newLineAtOffset(margin + 100, yPosition);
                cs.showText(status);
                cs.endText();

                yPosition -= 30;

                // Sender / Receiver boxes
                float boxHeight = 85;
                cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f);
                cs.addRect(margin, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                cs.setNonStrokingColor(245f / 255f, 247f / 255f, 250f / 255f);
                cs.addRect(margin + (pageWidth - 2 * margin) / 2 + 5, yPosition - boxHeight, (pageWidth - 2 * margin - 10) / 2, boxHeight);
                cs.fill();

                // Sender
                cs.beginText();
                cs.setNonStrokingColor(0f, 0f, 0f);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(margin + 10, yPosition - 15);
                cs.showText("ABSENDER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(margin + 10, yPosition - 32);
                cs.showText(senderName);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(margin + 10, yPosition - 47);
                cs.showText("Konto: " + senderKontoNumber);
                cs.endText();

                // Receiver
                float rightBoxX = margin + (pageWidth - 2 * margin) / 2 + 15;
                cs.beginText();
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(rightBoxX, yPosition - 15);
                cs.showText("EMPFAENGER");
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 10);
                cs.newLineAtOffset(rightBoxX, yPosition - 32);
                cs.showText(receiverName);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 47);
                cs.showText("Konto: " + receiverKontoNumber);
                cs.endText();

                cs.beginText();
                cs.setFont(regularFont, 9);
                cs.newLineAtOffset(rightBoxX, yPosition - 62);
                cs.showText("Abteilung: " + department);
                cs.endText();

                yPosition -= boxHeight + 30;

                // Table header
                cs.setNonStrokingColor(62f / 255f, 84f / 255f, 98f / 255f);
                cs.addRect(margin, yPosition - 20, pageWidth - 2 * margin, 20);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f);
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

                // Rows
                List<Article> articles = OrderManager.getInstance().getOrderArticles(order);
                double total = 0.0;
                boolean alternateRow = false;

                for (Article article : articles) {
                    int qty = orderedArticles.getOrDefault(article.getArticleNumber(), 0);
                    if (qty == 0) qty = orderedArticles.getOrDefault(article.getName(), 0);

                    double unit = article.getSellPrice();
                    double line = unit * qty;
                    total += line;

                    if (alternateRow) {
                        cs.setNonStrokingColor(250f / 255f, 250f / 255f, 250f / 255f);
                        cs.addRect(margin, yPosition - 15, pageWidth - 2 * margin, 18);
                        cs.fill();
                    }

                    cs.beginText();
                    cs.setNonStrokingColor(0f, 0f, 0f);
                    cs.setFont(regularFont, 9);
                    cs.newLineAtOffset(margin + 5, yPosition - 10);

                    String articleName = article.getName();
                    if (articleName.length() > 35) articleName = articleName.substring(0, 32) + "...";

                    cs.showText(articleName + " (" + article.getArticleNumber() + ")");
                    cs.newLineAtOffset(200, 0);
                    cs.showText(String.valueOf(qty));
                    cs.newLineAtOffset(60, 0);
                    cs.showText(String.format("%.2f CHF", unit));
                    cs.newLineAtOffset(80, 0);
                    cs.showText(String.format("%.2f CHF", line));
                    cs.endText();

                    yPosition -= 18;
                    alternateRow = !alternateRow;

                    if (yPosition < 150) break;
                }

                // Totals
                yPosition -= 10;
                cs.setNonStrokingColor(200f / 255f, 200f / 255f, 200f / 255f);
                cs.setLineWidth(1);
                cs.moveTo(margin, yPosition);
                cs.lineTo(pageWidth - margin, yPosition);
                cs.stroke();

                yPosition -= 25;

                cs.setNonStrokingColor(30f / 255f, 58f / 255f, 95f / 255f);
                cs.addRect(pageWidth - margin - 150, yPosition - 25, 150, 30);
                cs.fill();

                cs.beginText();
                cs.setNonStrokingColor(1f, 1f, 1f);
                cs.setFont(boldFont, 12);
                cs.newLineAtOffset(pageWidth - margin - 140, yPosition - 15);
                cs.showText("TOTAL:");
                cs.newLineAtOffset(50, 0);
                cs.showText(String.format("%.2f CHF", total));
                cs.endText();

                // Footer
                cs.beginText();
                cs.setNonStrokingColor(150f / 255f, 150f / 255f, 150f / 255f);
                cs.setFont(regularFont, 8);
                cs.newLineAtOffset(margin, 30);
                cs.showText("VEBO Lagersystem - Generiert am " + new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date()));
                cs.endText();
            }

            doc.save(outputFile);

            JOptionPane.showMessageDialog(frame,
                    "PDF erfolgreich erstellt:\n" + outputFile.getAbsolutePath(),
                    "Erfolg",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
                    "Fehler beim Erstellen des PDF-Dokuments:\n" + ex.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }
}
