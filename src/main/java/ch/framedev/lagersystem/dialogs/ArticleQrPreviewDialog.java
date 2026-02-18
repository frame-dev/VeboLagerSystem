package ch.framedev.lagersystem.dialogs;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.SettingsGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ThemeManager;
import ch.framedev.lagersystem.utils.QRCodeGenerator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static ch.framedev.lagersystem.utils.ArticleExporter.sanitizeForWinAnsi;

/**
 * Dialog for previewing QR codes for selected articles and exporting them as a PDF.
 */
public final class ArticleQrPreviewDialog {

    private ArticleQrPreviewDialog() {
    }

    public static void show(Component parent, List<Article> selectedArticles) {
        if (selectedArticles == null || selectedArticles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte w\u00e4hlen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "QR-Code Vorschau", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel infoLabel = new JLabel("Vorschau f\u00fcr " + selectedArticles.size() + " Artikel wird geladen...");
        topPanel.add(infoLabel, BorderLayout.WEST);

        JButton exportPdfButton = new JButton("Als PDF exportieren");
        exportPdfButton.setEnabled(false);
        topPanel.add(exportPdfButton, BorderLayout.EAST);

        dialog.add(topPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 16, 16));
        gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gridPanel.setBackground(ThemeManager.getBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        List<QrPreviewItem> previewItems = new ArrayList<>();

        SwingWorker<List<QrPreviewItem>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<QrPreviewItem> doInBackground() {
                List<QrPreviewItem> items = new ArrayList<>();
                for (Article article : selectedArticles) {
                    try {
                        String url = buildQrCodeUrl(article);
                        BufferedImage image = QRCodeGenerator.generateQRCodeBufferedImage(url, 220, 220);
                        items.add(new QrPreviewItem(article, image));
                    } catch (Exception ex) {
                        // skip broken entries but continue the batch
                    }
                }
                return items;
            }

            @Override
            protected void done() {
                try {
                    List<QrPreviewItem> items = get();
                    previewItems.addAll(items);
                    gridPanel.removeAll();

                    for (QrPreviewItem item : items) {
                        JPanel cell = new JPanel(new BorderLayout());
                        cell.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 232), 1));
                        cell.setBackground(ThemeManager.getCardBackgroundColor());

                        Image scaled = item.image().getScaledInstance(160, 160, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        imageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
                        cell.add(imageLabel, BorderLayout.CENTER);

                        String labelText = item.article().getArticleNumber() + " - " + item.article().getName();
                        JLabel textLabel = new JLabel(labelText);
                        textLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
                        textLabel.setFont(SettingsGUI.getFontByName(Font.PLAIN, 12));
                        cell.add(textLabel, BorderLayout.SOUTH);

                        gridPanel.add(cell);
                    }

                    infoLabel.setText("Vorschau geladen: " + items.size() + " QR-Codes");
                    exportPdfButton.setEnabled(!items.isEmpty());
                    gridPanel.revalidate();
                    gridPanel.repaint();
                } catch (Exception ex) {
                    infoLabel.setText("Fehler beim Laden der Vorschau: " + ex.getMessage());
                }
            }
        };

        exportPdfButton.addActionListener(e -> exportQrCodesToPdf(parent, previewItems));

        worker.execute();

        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    private static String buildQrCodeUrl(Article article) {
        String data = article.getQrCodeData();
        String encodedData = URLEncoder.encode(data, StandardCharsets.UTF_8);
        String serverUrl = "https://framedev.ch/vebo/scan.php";
        return serverUrl + "?data=" + encodedData;
    }

    private static void exportQrCodesToPdf(Component parent, List<QrPreviewItem> items) {
        if (items == null || items.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Keine QR-Codes zum Export vorhanden.",
                    "QR-Codes exportieren",
                    JOptionPane.WARNING_MESSAGE,
                    Main.iconSmall);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("PDF Speichern");
        fileChooser.setSelectedFile(new File("QR_Codes_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".pdf"));

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
            float margin = 30f;
            float pageWidth = pageSize.getWidth();
            float pageHeight = pageSize.getHeight();
            int columns = 3;
            float cellWidth = (pageWidth - (2 * margin)) / columns;
            float cellHeight = cellWidth + 40;
            float imageSize = cellWidth - 20;
            float fontSize = 9f;

            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            float startY = pageHeight - margin;
            int col = 0;
            int row = 0;

            for (QrPreviewItem item : items) {
                float x = margin + (col * cellWidth);
                float yTop = startY - (row * cellHeight);

                if (yTop - cellHeight < margin) {
                    contentStream.close();
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    contentStream = new PDPageContentStream(doc, page);
                    col = 0;
                    row = 0;
                    x = margin;
                    yTop = startY;
                }

                PDImageXObject image = LosslessFactory.createFromImage(doc, item.image());
                float imageX = x + (cellWidth - imageSize) / 2f;
                float imageY = yTop - imageSize;
                contentStream.drawImage(image, imageX, imageY, imageSize, imageSize);

                String labelText = item.article().getArticleNumber() + " - " + item.article().getName();
                labelText = sanitizeForWinAnsi(labelText);
                labelText = trimTextToWidth(labelText, regularFont, fontSize, cellWidth - 12);
                contentStream.beginText();
                contentStream.setFont(regularFont, fontSize);
                contentStream.newLineAtOffset(x + 6, imageY - 14);
                contentStream.showText(labelText);
                contentStream.endText();

                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }

            contentStream.close();
            doc.save(fileToSave);

            JOptionPane.showMessageDialog(parent,
                    "PDF erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "QR-Codes exportieren",
                    JOptionPane.INFORMATION_MESSAGE,
                    Main.iconSmall);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Fehler beim PDF-Export: " + ex.getMessage(),
                    "QR-Codes exportieren",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall);
        }
    }

    private static String trimTextToWidth(String text, PDFont font, float fontSize, float maxWidth) throws IOException {
        String trimmed = text;
        float textWidth = font.getStringWidth(trimmed) / 1000f * fontSize;
        if (textWidth <= maxWidth) {
            return trimmed;
        }
        while (trimmed.length() > 2 && textWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
            textWidth = font.getStringWidth(trimmed + "..") / 1000f * fontSize;
        }
        return trimmed + "..";
    }

    private record QrPreviewItem(Article article, BufferedImage image) {
    }
}
