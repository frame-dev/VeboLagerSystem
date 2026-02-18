package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.ArticleGUI;
import ch.framedev.lagersystem.main.Main;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArticleUtils {

    public static enum VolumeUnit {
        LITER, MILLILITER
    }

    private static Map<String, ArticleGUI.CategoryRange> categories; // category name -> range

    /**
     * Load categories from categories.json resource file
     */
    private static void loadCategories() {
        categories = new HashMap<>();
        try {
            InputStream is = ArticleUtils.class.getResourceAsStream("/categories.json");
            if (is == null) {
                System.err.println("categories.json not found in resources");
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();
            List<Map<String, String>> categoryList = gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    listType
            );

            for (Map<String, String> cat : categoryList) {
                String categoryName = cat.get("category");
                String fromTo = cat.get("fromTo");

                if (categoryName != null && fromTo != null) {
                    // Parse range like "1101 - 1116" or "1301"
                    String[] parts = fromTo.split("-");
                    int start, end;

                    if (parts.length == 2) {
                        start = Integer.parseInt(parts[0].trim());
                        end = Integer.parseInt(parts[1].trim());
                    } else {
                        start = end = Integer.parseInt(parts[0].trim());
                    }

                    categories.put(categoryName, new ArticleGUI.CategoryRange(categoryName, start, end));
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading categories: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Fehler beim Laden der Kategorien: " + e.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
        }
    }

    /**
     * Get category for an article based on its article number
     */
    public static String getCategoryForArticle(String articleNumber) {
        loadCategories();
        if (categories == null || articleNumber == null) {
            return "Unbekannt";
        }

        try {
            // Extract the numeric part from the article number (e.g., "1101" from "1101-ABC")
            String numericPart = articleNumber.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                return "Unbekannt";
            }

            int articleNum = Integer.parseInt(numericPart);

            // Find matching category range
            for (ArticleGUI.CategoryRange range : categories.values()) {
                if (articleNum >= range.rangeStart && articleNum <= range.rangeEnd) {
                    return range.category;
                }
            }
        } catch (NumberFormatException e) {
            // Article number doesn't contain valid number
        }

        return "Unbekannt";
    }

    // 10l = 25.54 CHF fill to 750ml
    public static double calculatePriceForFilling(double sellPrice, double liter, double fillingAmount, VolumeUnit unit) {
        double pricePerLitre = sellPrice / liter;
        double fillingAmountInLitres = (unit == VolumeUnit.LITER) ? fillingAmount : fillingAmount / 1000.0;
        return pricePerLitre * fillingAmountInLitres;
    }

    public static double calculatePriceForFilling(Article article, double fillingAmount, VolumeUnit unit) {
        String details = article.getDetails();
        String category = getCategoryForArticle(article.getArticleNumber()); // Get main category (e.g., "Reinigungsmittel" from "Reinigungsmittel - Geschirrreiniger")
        boolean allowed =
                category.contains("Reinigungsmittel")
                        || category.contains("Geschirrreiniger")
                        || category.contains("Seife");

        if (!allowed) {
            JOptionPane.showMessageDialog(null, "Artikelkategorie '" + category + "' ist nicht für die Preisberechnung geeignet. Artikel: " + article.getName(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            throw new IllegalArgumentException(
                    "Article category must be Reinigungsmittel, Geschirrreiniger or Seife for filling price calculation. Article category: " + category
            );
        }
        if(details == null || details.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Artikeldetails fehlen für Artikel: " + article.getName(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            throw new IllegalArgumentException("Article details cannot be null or empty");
        }
        if(details.contains("lt.") || details.contains("l.") || details.contains("liter") || details.contains("l ")) {
            String[] parts = details.split("lt\\.|l\\.|liter|l |liter\\. |lt ");
            if(parts.length > 0) {
                try {
                    double liter = Double.parseDouble(parts[0].trim());
                    return calculatePriceForFilling(article.getSellPrice(), liter, fillingAmount, unit);
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Konnte Literangabe aus Artikeldetails nicht parsen für Artikel: " + article.getName(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    throw new IllegalArgumentException("Could not parse liter value from article details: " + details);
                }
            }
        }
        JOptionPane.showMessageDialog(null, "Artikeldetails enthalten keine gültige Literangabe für Artikel: " + article.getName(), "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
        throw new IllegalArgumentException("Article details do not contain valid liter information: " + details);
    }

    public static void main(String[] args) {
        double price = calculatePriceForFilling(25.54, 10, 750, VolumeUnit.MILLILITER);
        System.out.println("Price for filling: " + price + " CHF");

        Article article = new Article("3301", "Cola", "10 lt.", 1, 0, 25.54, 0, "ZVG");
        double priceFromArticle = calculatePriceForFilling(article, 750, VolumeUnit.MILLILITER);
        System.out.println("Price for filling from article: " + priceFromArticle + " CHF");
    }
}
