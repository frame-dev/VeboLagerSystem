package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Utility for exporting article lists to CSV.
 */
public final class ArticleCsvExporter {

    private ArticleCsvExporter() {
    }

    public static void exportArticlesToCsv(Component parent, List<Article> articles, Icon icon) {
        if (articles == null || articles.isEmpty()) {
            JOptionPane.showMessageDialog(parent,
                    "Bitte wählen Sie mindestens einen Artikel aus.",
                    "Keine Auswahl",
                    JOptionPane.WARNING_MESSAGE,
                    icon);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("CSV Speichern");
        fileChooser.setSelectedFile(new File("Artikel_Auswahl_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv"));
        int userSelection = fileChooser.showSaveDialog(parent);
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
            JOptionPane.showMessageDialog(parent,
                    "CSV erfolgreich exportiert:\n" + fileToSave.getAbsolutePath(),
                    "Export",
                    JOptionPane.INFORMATION_MESSAGE,
                    icon);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent,
                    "Fehler beim CSV-Export: " + ex.getMessage(),
                    "Export",
                    JOptionPane.ERROR_MESSAGE,
                    icon);
        }
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
