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

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for article handling: category loading/mapping,
 * fill-price calculations, and shared UI helpers.
 */
public final class ArticleUtils {

    private static final Logger LOGGER = LogManager.getLogger(ArticleUtils.class);

    private static final String UNKNOWN_CATEGORY = "Unbekannt";
    private static final Object CATEGORY_LOCK = new Object();
    private static volatile boolean categoriesLoaded;

    /**
     * Regex to extract litre values from strings such as "10 lt.", "0.75 l", "1 liter".
     */
    private static final Pattern LITER_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:lt\\.|lt\\b|l\\.|l\\b|liter\\b)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> FILLING_ALLOWED_CATEGORY_KEYWORDS = Set.of(
            "reinigungsmittel",
            "geschirrreiniger",
            "seife"
    );

    /**
     * Category names mapped to their corresponding article number ranges.
     *
     * <p>Kept public for compatibility with existing static imports/usages.
     */
    public static Map<String, CategoryRange> categories = new HashMap<>();

    private ArticleUtils() {
    }

    /**
     * Helper class to represent category and article-number range.
     */
    public static class CategoryRange {
        public final String category;
        public final int rangeStart;
        public final int rangeEnd;

        public CategoryRange(String category, int start, int end) {
            this.category = category;
            this.rangeStart = start;
            this.rangeEnd = end;
        }
    }

    /**
     * Supported volume units for filling-price calculations.
     */
    public enum VolumeUnit {
        LITER,
        MILLILITER
    }

    /**
     * Loads categories from categories.json and caches them in-memory.
     */
    public static void loadCategories() {
        synchronized (CATEGORY_LOCK) {
            Map<String, CategoryRange> loaded = new HashMap<>();
            File file = new File(Main.getAppDataDir(), "categories.json");

            if (!file.exists()) {
                categories = loaded;
                categoriesLoaded = true;
                LOGGER.warn("categories.json not found at {}", file.getAbsolutePath());
                return;
            }

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();

            try (InputStream is = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

                List<Map<String, String>> categoryList = gson.fromJson(reader, listType);
                if (categoryList != null) {
                    for (Map<String, String> cat : categoryList) {
                        if (cat == null) {
                            continue;
                        }

                        String categoryName = cat.get("category");
                        String fromTo = cat.get("fromTo");

                        if (categoryName == null || categoryName.isBlank() || fromTo == null || fromTo.isBlank()) {
                            continue;
                        }

                        int[] range = parseRange(fromTo);
                        if (range == null) {
                            LOGGER.warn("Invalid category range '{}' for category '{}'", fromTo, categoryName);
                            continue;
                        }

                        loaded.put(categoryName, new CategoryRange(categoryName, range[0], range[1]));
                    }
                }

                categories = loaded;
                categoriesLoaded = true;
            } catch (Exception e) {
                categories = loaded;
                categoriesLoaded = true;
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

    private static int[] parseRange(String fromTo) {
        if (fromTo == null || fromTo.isBlank()) {
            return null;
        }

        String[] parts = fromTo.split("-");
        try {
            int start;
            int end;
            if (parts.length == 2) {
                start = Integer.parseInt(parts[0].trim());
                end = Integer.parseInt(parts[1].trim());
            } else {
                start = Integer.parseInt(parts[0].trim());
                end = start;
            }

            if (start > end) {
                return null;
            }
            return new int[]{start, end};
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static void ensureCategoriesLoaded() {
        if (categoriesLoaded) {
            return;
        }

        loadCategories();
        if (categories == null) {
            categories = new HashMap<>();
        }
    }

    /**
     * Add a new category and persist it to categories.json.
     */
    public static void addNewCategory(String categoryName, int rangeStart, int rangeEnd) {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category name cannot be null or blank");
        }
        if (rangeStart > rangeEnd) {
            throw new IllegalArgumentException("Range start must be <= range end");
        }

        synchronized (CATEGORY_LOCK) {
            ensureCategoriesLoaded();
            categories.put(categoryName.trim(), new CategoryRange(categoryName.trim(), rangeStart, rangeEnd));
            saveCategories();
        }
    }

    /**
     * Persists categories to categories.json.
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
                }).sorted((left, right) -> compareCategoryNames(
                    left.getOrDefault("category", ""),
                    right.getOrDefault("category", "")
                )).toList();

            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            String json = gson.toJson(categoryList);

            File outFile = new File(Main.getAppDataDir(), "categories.json");
            Files.writeString(outFile.toPath(), json, StandardCharsets.UTF_8);
            categoriesLoaded = true;
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
     * Compares category names using numeric-aware ordering for prefixed categories
     * (e.g. "1.2" before "1.10"), then falls back to case-insensitive text order.
     */
    private static int compareCategoryNames(String left, String right) {
        List<Integer> leftOrder = extractCategoryOrder(left);
        List<Integer> rightOrder = extractCategoryOrder(right);

        int max = Math.max(leftOrder.size(), rightOrder.size());
        for (int i = 0; i < max; i++) {
            int leftPart = i < leftOrder.size() ? leftOrder.get(i) : -1;
            int rightPart = i < rightOrder.size() ? rightOrder.get(i) : -1;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }

        String leftSafe = left == null ? "" : left;
        String rightSafe = right == null ? "" : right;
        return String.CASE_INSENSITIVE_ORDER.compare(leftSafe, rightSafe);
    }

    /**
     * Extracts leading numeric order tokens from categories such as "1.2 Something".
     */
    private static List<Integer> extractCategoryOrder(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return List.of();
        }

        Matcher matcher = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)").matcher(categoryName);
        if (!matcher.find()) {
            return List.of();
        }

        String[] parts = matcher.group(1).split("\\.");
        List<Integer> orderParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                orderParts.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                // Ignore malformed token and stop numeric extraction.
                break;
            }
        }
        return orderParts;
    }

    /**
     * Resolves a category name for an article number.
     */
    public static String getCategoryForArticle(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return UNKNOWN_CATEGORY;
        }

        ensureCategoriesLoaded();
        if (categories == null || categories.isEmpty()) {
            return UNKNOWN_CATEGORY;
        }

        try {
            String numericPart = articleNumber.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                return UNKNOWN_CATEGORY;
            }

            int articleNum = Integer.parseInt(numericPart);
            for (CategoryRange range : categories.values()) {
                if (range == null) {
                    continue;
                }
                if (articleNum >= range.rangeStart && articleNum <= range.rangeEnd) {
                    return range.category;
                }
            }
        } catch (NumberFormatException ignored) {
            // ignore and return unknown
        }

        return UNKNOWN_CATEGORY;
    }

    /**
     * Calculates filling price from base values.
     */
    public static double calculatePriceForFilling(double sellPrice, double liter, double fillingAmount, VolumeUnit unit) {
        if (liter <= 0) {
            throw new IllegalArgumentException("Liter value must be > 0, but was: " + liter);
        }
        if (fillingAmount <= 0) {
            throw new IllegalArgumentException("Filling amount must be > 0, but was: " + fillingAmount);
        }
        if (unit == null) {
            throw new IllegalArgumentException("VolumeUnit must not be null");
        }

        double pricePerLitre = sellPrice / liter;
        double fillingAmountInLitres = (unit == VolumeUnit.LITER) ? fillingAmount : fillingAmount / 1000.0;
        return pricePerLitre * fillingAmountInLitres;
    }

    /**
     * Calculates filling price for an article by parsing litre values from details.
     */
    public static double calculatePriceForFilling(Article article, double fillingAmount, VolumeUnit unit) {
        if (article == null) {
            throw new IllegalArgumentException("Article must not be null");
        }
        if (fillingAmount <= 0) {
            throw new IllegalArgumentException("Filling amount must be > 0, but was: " + fillingAmount);
        }
        if (unit == null) {
            throw new IllegalArgumentException("VolumeUnit must not be null");
        }

        String details = article.getDetails();
        String category = getCategoryForArticle(article.getArticleNumber());

        String categoryLower = category == null ? "" : category.toLowerCase(Locale.ROOT);
        boolean allowed = FILLING_ALLOWED_CATEGORY_KEYWORDS.stream().anyMatch(categoryLower::contains);

        if (!allowed) {
            showErrorDialog("Artikelkategorie '" + category + "' ist nicht für die Preisberechnung geeignet. Artikel: " + article.getName());
            throw new IllegalArgumentException(
                    "Article category must be Reinigungsmittel, Geschirrreiniger or Seife for filling price calculation. Article category: " + category
            );
        }

        if (details == null || details.isBlank()) {
            showErrorDialog("Artikeldetails fehlen für Artikel: " + article.getName());
            throw new IllegalArgumentException("Article details cannot be null or empty");
        }

        Matcher matcher = LITER_PATTERN.matcher(details);
        if (matcher.find()) {
            String raw = matcher.group(1).replace(',', '.');
            try {
                double liter = Double.parseDouble(raw.trim());
                return calculatePriceForFilling(article.getSellPrice(), liter, fillingAmount, unit);
            } catch (NumberFormatException e) {
                showErrorDialog("Konnte Literangabe aus Artikeldetails nicht parsen für Artikel: " + article.getName());
                throw new IllegalArgumentException("Could not parse liter value from article details: " + details);
            }
        }

        showErrorDialog("Artikeldetails enthalten keine gültige Literangabe für Artikel: " + article.getName());
        throw new IllegalArgumentException("Article details do not contain valid liter information: " + details);
    }

    private static void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Fehler",
                JOptionPane.ERROR_MESSAGE,
                Main.iconSmall
        );
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
            if (article == null) {
                continue;
            }

            String input = JOptionPane.showInputDialog(
                    frame,
                    "Menge für Artikel (" + artikelNr + ") " + article.getName() + " eingeben:",
                    "1"
            );
            if (input == null) {
                continue;
            }

            try {
                int menge = Integer.parseInt(input.trim());
                if (menge <= 0) {
                    JOptionPane.showMessageDialog(frame,
                            "Bitte geben Sie eine Menge größer als 0 ein.",
                            "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
                    continue;
                }

                ArticleListGUI.addArticle(article, menge);
                articlesToAdd.add(article);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame,
                        "Bitte geben Sie eine gültige Zahl für die Menge ein.",
                        "Ungültige Eingabe", JOptionPane.ERROR_MESSAGE, Main.iconSmall);
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
     * Creates a gradient header panel used by article-related UIs.
     */
    public static JPanel getHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                Color color1 = ThemeManager.getHeaderBackgroundColor();
                Color color2 = ThemeManager.getHeaderGradientColor();
                GradientPaint gradient = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        return headerPanel;
    }
}
