package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.guis.ArticleListGUI;
import ch.framedev.lagersystem.main.Main;
import ch.framedev.lagersystem.managers.ArticleManager;
import ch.framedev.lagersystem.managers.ThemeManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The ArticleUtils class provides utility methods for handling articles, including loading and managing article categories from a JSON file, calculating prices for filling based on article details, and determining the category of an article based on its number. It also defines an enum for volume units (liters and milliliters) to assist with price calculations.
 * @author framedev
 */
public class ArticleUtils {

    private static final Logger LOGGER = LogManager.getLogger(ArticleUtils.class);

    /**
     * Guard for category loading. Categories are loaded lazily and cached to avoid
     * repeated disk reads on every lookup.
     */
    private static final Object CATEGORY_LOCK = new Object();

    /** Regex to extract a numeric litre value from details like "10 lt.", "0.75 l", "1 liter" (case-insensitive). */
    private static final Pattern LITER_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:lt\\.|lt\\b|l\\.|l\\b|liter\\b)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Private constructor to prevent instantiation of this utility class, as all methods are static and it is not meant to be instantiated.
     */
    private ArticleUtils() {
        // Private constructor to prevent instantiation
    }


    /**
     * Helper class to represent a category and its associated article number range. Loaded from categories.json.
     */
    public static class CategoryRange {
        /**
         * The name of the category (e.g., "Getränke")
         */
        public String category;
        /**
         * The starting article number for this category (inclusive)
         */
        public int rangeStart;
        /**
         * The ending article number for this category (inclusive)
         */
        public int rangeEnd;

        /**
         * Creates a new CategoryRange with the specified category name and article number range.
         * @param category The name of the category (e.g., "Getränke")
         * @param start The starting article number for this category (inclusive)
         * @param end The ending article number for this category (inclusive)
         */
        public CategoryRange(String category, int start, int end) {
            this.category = category;
            this.rangeStart = start;
            this.rangeEnd = end;
        }
    }

    /**
     * Enum representing volume units for price calculation, such as liters and milliliters.
     */
    public enum VolumeUnit {
        /**
         * Represents liters (e.g., 1 lt. = 1 liter)
         */
        LITER,
        /**
         * Represents milliliters (e.g., 750 ml = 0.75 liters)
         */
        MILLILITER
    }

    /**
     * Category names mapped to their corresponding article number ranges.
     * <p>
     * This map is loaded from the application's categories.json file and cached in memory.
     * It is never null (falls back to an empty map).
     */
    public static Map<String, CategoryRange> categories = new HashMap<>(); // category name -> range

    /**
     * Loads categories from the application's categories.json file.
     * <p>
     * The result is cached in {@link #categories}. If the file is missing or invalid,
     * the method falls back to an empty category map.
     */
    public static void loadCategories() {
        synchronized (CATEGORY_LOCK) {
            Map<String, CategoryRange> loaded = new HashMap<>();
            File file = new File(Main.getAppDataDir(), "categories.json");

            if (!file.exists()) {
                // File not found: keep categories empty but do not treat as fatal.
                categories = loaded;
                LOGGER.warn("categories.json not found at {}", file.getAbsolutePath());
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();

            try (InputStream is = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                List<Map<String, String>> categoryList = gson.fromJson(reader, listType);
                if (categoryList == null) {
                    categories = loaded;
                    return;
                }

                for (Map<String, String> cat : categoryList) {
                    if (cat == null) continue;

                    String categoryName = cat.get("category");
                    String fromTo = cat.get("fromTo");

                    if (categoryName == null || categoryName.trim().isEmpty() || fromTo == null || fromTo.trim().isEmpty()) {
                        continue;
                    }

                    // Parse range like "1101 - 1116" or "1301"
                    String[] parts = fromTo.split("-");
                    int start;
                    int end;

                    try {
                        if (parts.length == 2) {
                            start = Integer.parseInt(parts[0].trim());
                            end = Integer.parseInt(parts[1].trim());
                        } else {
                            start = end = Integer.parseInt(parts[0].trim());
                        }
                    } catch (NumberFormatException nfe) {
                        LOGGER.warn("Invalid category range '{}' for category '{}'", fromTo, categoryName);
                        continue;
                    }

                    loaded.put(categoryName, new CategoryRange(categoryName, start, end));
                }

                categories = loaded;

            } catch (Exception e) {
                categories = loaded;
                LOGGER.error("Error loading categories from {}", file.getAbsolutePath(), e);
                JOptionPane.showMessageDialog(
                        null,
                        "Fehler beim Laden der Kategorien: " + e.getMessage(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall
                );
            }
        }
    }

    /** Ensures categories are loaded once before access. */
    private static void ensureCategoriesLoaded() {
        // Fast path: already loaded (non-null and not empty is enough for a stable cache)
        if (categories != null && !categories.isEmpty()) {
            return;
        }
        // Slow path: load under lock
        loadCategories();
        if (categories == null) {
            categories = new HashMap<>();
        }
    }

    /**
     * Add a new category with a given name and range, then save it to the categories.json file
     * @param categoryName Name of the category to add (e.g., "Reinigungsmittel")
     * @param rangeStart Start of the article number range for this category (e.g., 1101)
     * @param rangeEnd End of the article number range for this category (e.g., 1116). Can be the same as rangeStart for single numbers.
     */
    public static void addNewCategory(String categoryName, int rangeStart, int rangeEnd) {
        if(categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be null or blank");
        }
        if (categories == null) {
            loadCategories();
        }
        categories.put(categoryName, new CategoryRange(categoryName, rangeStart, rangeEnd));
        saveCategories();
    }

    /**
     * Persists the in-memory categories to categorize.json.
     */
    private static void saveCategories() {
        if (categories == null) {
            return;
        }
        try {
            List<Map<String, String>> categoryList = categories.values().stream().map(range -> {
                Map<String, String> map = new HashMap<>();
                map.put("category", range.category);
                map.put("fromTo", range.rangeStart == range.rangeEnd
                        ? String.valueOf(range.rangeStart)
                        : range.rangeStart + " - " + range.rangeEnd);
                return map;
            }).toList();

            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            String json = gson.toJson(categoryList);

            File outFile = new File(Main.getAppDataDir(), "categories.json");
            Files.writeString(outFile.toPath(), json, StandardCharsets.UTF_8);

        } catch (Exception e) {
            LOGGER.error("Error saving categories", e);
            JOptionPane.showMessageDialog(
                    null,
                    "Fehler beim Speichern der Kategorien: " + e.getMessage(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall
            );
        }
    }

    /**
     * Resolves the category name for an article number by matching the numeric part against
     * loaded category ranges.
     *
     * @param articleNumber article number to categorize (e.g. "1101-ABC")
     * @return category name if found, otherwise "Unbekannt"
     */
    public static String getCategoryForArticle(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return "Unbekannt";
        }

        ensureCategoriesLoaded();
        if (categories == null || categories.isEmpty()) {
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
            for (CategoryRange range : categories.values()) {
                if (range == null) continue;
                if (articleNum >= range.rangeStart && articleNum <= range.rangeEnd) {
                    return range.category;
                }
            }
        } catch (NumberFormatException ignored) {
            // Article number doesn't contain valid number
        }

        return "Unbekannt";
    }

    /**
     * Calculate price for filling based on sell price, liter information and filling amount
     * @param sellPrice Sell price of the article (e.g., 25.54 CHF)
     * @param liter Liter information from article details (e.g., 10 for "10 lt.")
     * @param fillingAmount Amount to fill (e.g., 750 for 750ml)
     * @param unit Unit of filling amount (LITER or MILLILITER)
     * @return Calculated price for filling
     */
    public static double calculatePriceForFilling(double sellPrice, double liter, double fillingAmount, VolumeUnit unit) {
        if (liter <= 0) {
            throw new IllegalArgumentException("Liter value must be > 0, but was: " + liter);
        }
        if (unit == null) {
            throw new IllegalArgumentException("VolumeUnit must not be null");
        }
        double pricePerLitre = sellPrice / liter;
        double fillingAmountInLitres = (unit == VolumeUnit.LITER) ? fillingAmount : fillingAmount / 1000.0;
        return pricePerLitre * fillingAmountInLitres;
    }

    /**
     * Calculates a filling price for an article by extracting the litre value from {@link Article#getDetails()}.
     *
     * <p>Supported detail formats include for example:
     * <ul>
     *   <li>"10 lt."</li>
     *   <li>"0.75 l"</li>
     *   <li>"1 liter"</li>
     * </ul>
     *
     * @param article        article to calculate price for
     * @param fillingAmount  amount to fill (e.g. 750 for 750ml)
     * @param unit           unit of filling amount
     * @return calculated price for filling
     */
    public static double calculatePriceForFilling(Article article, double fillingAmount, VolumeUnit unit) {
        if (article == null) {
            throw new IllegalArgumentException("Article must not be null");
        }
        if (unit == null) {
            throw new IllegalArgumentException("VolumeUnit must not be null");
        }

        String details = article.getDetails();
        String category = getCategoryForArticle(article.getArticleNumber());

        // Allow list of categories where filling-price calculation makes sense
        String categoryLower = category == null ? "" : category.toLowerCase(Locale.ROOT);
        boolean allowed =
                categoryLower.contains("reinigungsmittel") ||
                        categoryLower.contains("geschirrreiniger") ||
                        categoryLower.contains("seife");

        if (!allowed) {
            JOptionPane.showMessageDialog(
                    null,
                    "Artikelkategorie '" + category + "' ist nicht für die Preisberechnung geeignet. Artikel: " + article.getName(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall
            );
            throw new IllegalArgumentException(
                    "Article category must be Reinigungsmittel, Geschirrreiniger or Seife for filling price calculation. Article category: " + category
            );
        }

        if (details == null || details.isBlank()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Artikeldetails fehlen für Artikel: " + article.getName(),
                    "Fehler",
                    JOptionPane.ERROR_MESSAGE,
                    Main.iconSmall
            );
            throw new IllegalArgumentException("Article details cannot be null or empty");
        }

        Matcher m = LITER_PATTERN.matcher(details);
        if (m.find()) {
            String raw = m.group(1);
            // Support decimals with comma (e.g. "0,75 l")
            raw = raw.replace(',', '.');
            try {
                double liter = Double.parseDouble(raw.trim());
                return calculatePriceForFilling(article.getSellPrice(), liter, fillingAmount, unit);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Konnte Literangabe aus Artikeldetails nicht parsen für Artikel: " + article.getName(),
                        "Fehler",
                        JOptionPane.ERROR_MESSAGE,
                        Main.iconSmall
                );
                throw new IllegalArgumentException("Could not parse liter value from article details: " + details);
            }
        }

        JOptionPane.showMessageDialog(
                null,
                "Artikeldetails enthalten keine gültige Literangabe für Artikel: " + article.getName(),
                "Fehler",
                JOptionPane.ERROR_MESSAGE,
                Main.iconSmall
        );
        throw new IllegalArgumentException("Article details do not contain valid liter information: " + details);
    }

    public static void addSelectedArticlesToClientOrder(JFrame frame, JTable articleTable) {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(frame,
                    "Bitte wählen Sie mindestens einen Artikel aus, um ihn zur Kundenbestellung hinzuzufügen.",
                    "Keine Auswahl", JOptionPane.WARNING_MESSAGE, Main.iconSmall);
            return;
        }

        List<Article> articlesToAdd = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();

        for (int selectedRow : selectedRows) {
            int modelRow = articleTable.convertRowIndexToModel(selectedRow);
            String artikelNr = (String) model.getValueAt(modelRow, 0);
            Article article = ArticleManager.getInstance().getArticleByNumber(artikelNr);
            if (article != null) {
                String input = JOptionPane.showInputDialog(frame, "Menge für Artikel (" + artikelNr + ")" + article.getName() + " eingeben:", "1");
                if (input != null) {
                    try {
                        int menge = Integer.parseInt(input);
                        ArticleListGUI.addArticle(article, menge);
                        articlesToAdd.add(article);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame,
                                "Bitte geben Sie eine gültige Zahl für die Menge ein.",
                                "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    }
                }
            }
        }

        if (articlesToAdd.isEmpty()) {
            JOptionPane.showMessageDialog(frame,
                    "Keine gültigen Artikel zum Hinzufügen gefunden.",
                    "Fehler", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
            return;
        }
        JOptionPane.showMessageDialog(frame, "Artikel zur Kundenbestellung hinzugefügt.",
                "Erfolg", JOptionPane.INFORMATION_MESSAGE, Main.iconSmall);
    }

    /**
     * Creates a custom header panel with a horizontal gradient background for the article management section.
     *
     * @return A JPanel with a custom gradient background that can be used as a header for the article management section.
     */
    public static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                GradientPaint gradient = getGradientPaint();
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }

            private GradientPaint getGradientPaint() {
                Color color1 = ThemeManager.getHeaderBackgroundColor();
                Color color2 = ThemeManager.getHeaderGradientColor();

                return new GradientPaint(
                        0, 0, color1,
                        getWidth(), 0, color2
                );
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }
}
