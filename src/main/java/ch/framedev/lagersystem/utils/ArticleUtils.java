package ch.framedev.lagersystem.utils;

import ch.framedev.lagersystem.classes.Article;
import ch.framedev.lagersystem.classes.SeperateArticle;
import ch.framedev.lagersystem.dialogs.MessageDialog;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for article handling: category loading/mapping,
 * fill-price calculations, and shared UI helpers.
 */
public final class ArticleUtils {

    private static final Logger LOGGER = LogManager.getLogger(ArticleUtils.class);
    private static final Gson GSON = new Gson();
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
    private static final java.lang.reflect.Type CATEGORY_LIST_TYPE = new TypeToken<List<Map<String, String>>>() {
    }.getType();

    private static final String UNKNOWN_CATEGORY = "Unbekannt";
    private static final String CATEGORIES_FILE_NAME = "categories.json";
    private static final Object CATEGORY_LOCK = new Object();
    private static volatile boolean categoriesLoaded;

    /**
     * Regex to extract litre values from strings such as "10 lt.", "0.75 l", "1
     * liter".
     */
    private static final Pattern LITER_PATTERN = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:lt\\.|lt\\b|l\\.|l\\b|liter\\b)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FILLING_INPUT_PATTERN = Pattern.compile(
            "^(\\d+(?:[.,]\\d+)?)\\s*(ml|l|g|kg)$",
            Pattern.CASE_INSENSITIVE);

    private static final Set<String> FILLING_ALLOWED_CATEGORY_KEYWORDS = Set.of(
            "reinigungsmittel",
            "geschirrreiniger",
            "seife");

    /**
     * Splits on slash with optional surrounding whitespace, but not when the slash
     * is preceded by a digit (e.g. price notation "12.60/Einzel").
     */
    private static final Pattern DETAILS_SPLIT_PATTERN = Pattern.compile(
            "(?<!\\d)\\s*/\\s*");

    /**
     * Strips leading abbreviated label prefix, e.g. {@code "Gr. "}, {@code "Nr. "}.
     */
    private static final Pattern PART_LABEL_PREFIX = Pattern.compile(
            "^[A-Za-z\u00C0-\u024F]{1,5}\\.\\s+");

    /**
     * Strips trailing quantity/unit suffix, e.g. {@code " 100 Stk. Box"},
     * {@code " 50 ml"}.
     */
    private static final Pattern PART_QUANTITY_SUFFIX = Pattern.compile(
            "\\s+\\d+[\\d.,]*\\s*.*$");

    /**
     * Strips leading open-parentheses/brackets, e.g. {@code "(Rot"} →
     * {@code "Rot"}.
     */
    private static final Pattern PART_LEADING_PUNCT = Pattern.compile(
            "^[\\s()\\[\\]]+");

    /**
     * Strips trailing close-parentheses/brackets and lone prepositions, e.g.
     * {@code "Gelb)"} → {@code "Gelb"}, {@code "Pack \u00e0"} → {@code "Pack"}.
     */
    private static final Pattern PART_TRAILING_PUNCT = Pattern.compile(
            "[\\s()\\[\\]\\u00e0]+$");

    /**
     * Compiled pattern for extracting leading numeric order tokens like
     * {@code "1.2"}.
     */
    private static final Pattern CATEGORY_ORDER_PATTERN = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)*)");

    /**
     * Compiled pattern for parsing a category range such as {@code "100-199"} or
     * {@code "100 - 199"}.
     */
    private static final Pattern RANGE_PATTERN = Pattern.compile(
            "^\\s*(\\d+)\\s*(?:-\\s*(\\d+))?\\s*$");

    private static final String ORDER_ITEM_ARTICLE_PREFIX = "a=";
    private static final String ORDER_ITEM_SIZE_PREFIX = "|s=";
    private static final String ORDER_ITEM_COLOR_PREFIX = "|c=";
    private static final String ORDER_ITEM_FILLING_PREFIX = "|f=";

    /**
     * Category names mapped to their corresponding article number ranges.
     * Thread-safe; written under {@code CATEGORY_LOCK} but read freely from any
     * thread.
     */
    public static volatile Map<String, CategoryRange> categories = new ConcurrentHashMap<>();

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

    public record FillingData(double amount, String unit) {
        public String normalizedUnit() {
            return unit == null ? "" : unit.trim().toLowerCase(Locale.ROOT);
        }

        public VolumeUnit toVolumeUnit() {
            return switch (normalizedUnit()) {
                case "l" -> VolumeUnit.LITER;
                case "ml" -> VolumeUnit.MILLILITER;
                default -> null;
            };
        }

        public String toDisplayString() {
            return formatFillingAmount(amount) + " " + normalizedUnit();
        }
    }

    public static String normalizeFilling(String fillingValue) {
        if (fillingValue == null) {
            return "";
        }

        String candidate = sanitizeFillingCandidate(fillingValue);
        if (candidate.isEmpty()) {
            return "";
        }

        Matcher matcher = FILLING_INPUT_PATTERN.matcher(candidate);
        if (matcher.matches()) {
            try {
                double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
                if (amount <= 0) {
                    return "";
                }
                return new FillingData(amount, matcher.group(2)).toDisplayString();
            } catch (NumberFormatException ignored) {
                return "";
            }
        }

        String normalizedUnit = candidate.toLowerCase(Locale.ROOT);
        return switch (normalizedUnit) {
            case "ml", "l", "g", "kg" -> normalizedUnit;
            default -> candidate;
        };
    }

    public static String normalizeFilling(String amount, String unit) {
        String safeAmount = amount == null ? "" : amount.trim();
        String safeUnit = unit == null ? "" : unit.trim();
        return normalizeFilling((safeAmount + " " + safeUnit).trim());
    }

    public static String normalizeMetadataValue(String value) {
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

    public static String buildOrderItemKey(String articleNumber, String size, String color, String filling) {
        String normalizedArticleNumber = normalizeMetadataValue(articleNumber);
        if (normalizedArticleNumber.isBlank()) {
            return "";
        }

        String normalizedSize = normalizeMetadataValue(size);
        String normalizedColor = normalizeMetadataValue(color);
        String normalizedFilling = normalizeFilling(filling);

        if (normalizedSize.isBlank() && normalizedColor.isBlank() && normalizedFilling.isBlank()) {
            return normalizedArticleNumber;
        }

        return ORDER_ITEM_ARTICLE_PREFIX + encodeOrderItemComponent(normalizedArticleNumber)
                + ORDER_ITEM_SIZE_PREFIX + encodeOrderItemComponent(normalizedSize)
                + ORDER_ITEM_COLOR_PREFIX + encodeOrderItemComponent(normalizedColor)
                + ORDER_ITEM_FILLING_PREFIX + encodeOrderItemComponent(normalizedFilling);
    }

    public static boolean isCompositeOrderItemKey(String orderItemKey) {
        if (orderItemKey == null || orderItemKey.isBlank()) {
            return false;
        }
        return orderItemKey.startsWith(ORDER_ITEM_ARTICLE_PREFIX)
                && orderItemKey.contains(ORDER_ITEM_SIZE_PREFIX)
                && orderItemKey.contains(ORDER_ITEM_COLOR_PREFIX)
                && orderItemKey.contains(ORDER_ITEM_FILLING_PREFIX);
    }

    public static String getOrderItemArticleNumber(String orderItemKey) {
        if (!isCompositeOrderItemKey(orderItemKey)) {
            return normalizeMetadataValue(orderItemKey);
        }
        return decodeOrderItemComponent(extractOrderItemComponent(orderItemKey, ORDER_ITEM_ARTICLE_PREFIX, ORDER_ITEM_SIZE_PREFIX));
    }

    public static String getOrderItemSize(String orderItemKey) {
        if (!isCompositeOrderItemKey(orderItemKey)) {
            return "";
        }
        return normalizeMetadataValue(
                decodeOrderItemComponent(extractOrderItemComponent(orderItemKey, ORDER_ITEM_SIZE_PREFIX, ORDER_ITEM_COLOR_PREFIX)));
    }

    public static String getOrderItemColor(String orderItemKey) {
        if (!isCompositeOrderItemKey(orderItemKey)) {
            return "";
        }
        return normalizeMetadataValue(
                decodeOrderItemComponent(extractOrderItemComponent(orderItemKey, ORDER_ITEM_COLOR_PREFIX, ORDER_ITEM_FILLING_PREFIX)));
    }

    public static String getOrderItemFilling(String orderItemKey) {
        if (!isCompositeOrderItemKey(orderItemKey)) {
            return "";
        }
        return normalizeFilling(
                decodeOrderItemComponent(extractOrderItemComponent(orderItemKey, ORDER_ITEM_FILLING_PREFIX, null)));
    }

    private static String extractOrderItemComponent(String orderItemKey, String prefix, String nextPrefix) {
        int start = orderItemKey.indexOf(prefix);
        if (start < 0) {
            return "";
        }
        start += prefix.length();
        int end = nextPrefix == null ? orderItemKey.length() : orderItemKey.indexOf(nextPrefix, start);
        if (end < 0) {
            end = orderItemKey.length();
        }
        return orderItemKey.substring(start, end);
    }

    private static String encodeOrderItemComponent(String value) {
        String normalized = value == null ? "" : value;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeOrderItemComponent(String encodedValue) {
        if (encodedValue == null || encodedValue.isBlank()) {
            return "";
        }
        try {
            return new String(Base64.getUrlDecoder().decode(encodedValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ignored) {
            return "";
        }
    }

    public static boolean isFillingValid(String fillingValue) {
        if (fillingValue == null) {
            return true;
        }

        String normalized = normalizeFilling(fillingValue);
        if (normalized.isBlank()) {
            return true;
        }

        return FILLING_INPUT_PATTERN.matcher(normalized).matches()
                || normalized.equals("ml")
                || normalized.equals("l")
                || normalized.equals("g")
                || normalized.equals("kg");
    }

    public static FillingData parseFilling(String fillingValue) {
        String normalized = normalizeFilling(fillingValue);
        if (normalized.isBlank()) {
            return null;
        }

        Matcher matcher = FILLING_INPUT_PATTERN.matcher(normalized);
        if (!matcher.matches()) {
            return null;
        }

        try {
            double amount = Double.parseDouble(matcher.group(1).replace(',', '.'));
            if (amount <= 0) {
                return null;
            }
            return new FillingData(amount, matcher.group(2));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static double resolveEffectiveSellPrice(Article article, String fillingValue) {
        if (article == null) {
            return 0.0;
        }

        FillingData filling = parseFilling(fillingValue);
        if (filling == null) {
            return article.getSellPrice();
        }

        VolumeUnit volumeUnit = filling.toVolumeUnit();
        if (volumeUnit == null) {
            return article.getSellPrice();
        }

        return calculatePriceForFilling(article, filling.amount(), volumeUnit);
    }

    public static String formatArticleWithFilling(Article article, String fillingValue) {
        if (article == null) {
            return "";
        }

        String articleName = article.getName() == null ? "" : article.getName();
        String normalized = normalizeFilling(fillingValue);
        if (normalized.isBlank()) {
            return articleName;
        }
        return articleName + " [" + normalized + "]";
    }

    private static String sanitizeFillingCandidate(String fillingValue) {
        String candidate = fillingValue.trim();
        if (candidate.isEmpty()) {
            return "";
        }

        int colonIndex = candidate.indexOf(':');
        if (colonIndex >= 0 && colonIndex < candidate.length() - 1) {
            candidate = candidate.substring(colonIndex + 1).trim();
        }

        return candidate
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('(', ' ')
                .replace(')', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String formatFillingAmount(double amount) {
        BigDecimal decimal = BigDecimal.valueOf(amount).stripTrailingZeros();
        return decimal.toPlainString().replace('.', ',');
    }

    private static File getCategoriesFile() {
        return new File(Main.getAppDataDir(), CATEGORIES_FILE_NAME);
    }

    private static void setLoadedCategories(Map<String, CategoryRange> loadedCategories) {
        categories = loadedCategories;
        categoriesLoaded = true;
    }

    private static List<Map<String, String>> readCategoryEntries(File file) throws IOException {
        try (InputStream is = new FileInputStream(file);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            List<Map<String, String>> categoryList = GSON.fromJson(reader, CATEGORY_LIST_TYPE);
            return categoryList == null ? List.of() : categoryList;
        }
    }

    private static void addCategoryRange(Map<String, CategoryRange> loadedCategories, Map<String, String> categoryEntry) {
        if (categoryEntry == null) {
            return;
        }

        String categoryName = normalizeMetadataValue(categoryEntry.get("category"));
        String fromTo = normalizeMetadataValue(categoryEntry.get("fromTo"));
        if (categoryName.isBlank() || fromTo.isBlank()) {
            return;
        }

        int[] range = parseRange(fromTo);
        if (range == null) {
            LOGGER.warn("Invalid category range '{}' for category '{}'", fromTo, categoryName);
            return;
        }

        loadedCategories.put(categoryName, new CategoryRange(categoryName, range[0], range[1]));
    }

    private static Map<String, String> toCategoryEntry(CategoryRange range) {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put("category", range.category);
        map.put("fromTo", range.rangeStart == range.rangeEnd
                ? String.valueOf(range.rangeStart)
                : range.rangeStart + " - " + range.rangeEnd);
        return map;
    }

    /**
     * Loads categories from categories.json and caches them in-memory.
     */
    public static void loadCategories() {
        synchronized (CATEGORY_LOCK) {
            Map<String, CategoryRange> loaded = new HashMap<>();
            File file = getCategoriesFile();

            if (!file.exists()) {
                setLoadedCategories(loaded);
                LOGGER.warn("categories.json not found at {}", file.getAbsolutePath());
                return;
            }

            try {
                for (Map<String, String> categoryEntry : readCategoryEntries(file)) {
                    addCategoryRange(loaded, categoryEntry);
                }
                setLoadedCategories(loaded);
            } catch (IOException e) {
                setLoadedCategories(loaded);
                LOGGER.error("Error loading categories from {}", file.getAbsolutePath(), e);
                showTimedMessage("Fehler", "Fehler beim Laden der Kategorien: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static int[] parseRange(String fromTo) {
        if (fromTo == null || fromTo.isBlank()) {
            return null;
        }

        Matcher m = RANGE_PATTERN.matcher(fromTo);
        if (!m.matches()) {
            return null;
        }

        try {
            int start = Integer.parseInt(m.group(1));
            int end = m.group(2) != null ? Integer.parseInt(m.group(2)) : start;
            return start <= end ? new int[] { start, end } : null;
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
            categories = new ConcurrentHashMap<>();
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
        if (categories == null || categories.isEmpty()) {
            return;
        }

        try {
            List<Map<String, String>> categoryList = categories.values().stream()
                    .map(ArticleUtils::toCategoryEntry)
                    .sorted((left, right) -> compareCategoryNames(
                    left.getOrDefault("category", ""),
                    right.getOrDefault("category", "")))
                    .toList();

            Files.writeString(getCategoriesFile().toPath(), PRETTY_GSON.toJson(categoryList), StandardCharsets.UTF_8);
            categoriesLoaded = true;
        } catch (IOException e) {
            LOGGER.error("Error saving categories", e);
            showTimedMessage("Fehler", "Fehler beim Speichern der Kategorien: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean isFillingCategoryAllowed(String category) {
        String categoryLower = category == null ? "" : category.toLowerCase(Locale.ROOT);
        return FILLING_ALLOWED_CATEGORY_KEYWORDS.stream().anyMatch(categoryLower::contains);
    }

    private static Double extractLiterValue(String details) {
        if (details == null || details.isBlank()) {
            return null;
        }
        Matcher matcher = LITER_PATTERN.matcher(details);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Double.parseDouble(matcher.group(1).replace(',', '.').trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public static boolean canCalculateFillingPrice(Article article) {
        if (article == null) {
            return false;
        }

        if (!isFillingCategoryAllowed(getCategoryForArticle(article.getArticleNumber()))) {
            return false;
        }

        return extractLiterValue(article.getDetails()) != null;
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
     * Extracts leading numeric order tokens from categories such as "1.2
     * Something".
     */
    private static List<Integer> extractCategoryOrder(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) {
            return List.of();
        }

        Matcher matcher = CATEGORY_ORDER_PATTERN.matcher(categoryName);
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

        String numericPart = articleNumber.replaceAll("[^0-9]", "");
        if (numericPart.isEmpty()) {
            return UNKNOWN_CATEGORY;
        }

        try {
            int articleNum = Integer.parseInt(numericPart);
            Optional<CategoryRange> match = categories.values().stream()
                    .filter(r -> r != null && articleNum >= r.rangeStart && articleNum <= r.rangeEnd)
                    .findFirst();
            return match.map(r -> r.category).orElse(UNKNOWN_CATEGORY);
        } catch (NumberFormatException ignored) {
            return UNKNOWN_CATEGORY;
        }
    }

    /**
     * Calculates filling price from base values.
     */
    public static double calculatePriceForFilling(double sellPrice, double liter, double fillingAmount,
            VolumeUnit unit) {
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
        if (!isFillingCategoryAllowed(category)) {
            showErrorDialog("Artikelkategorie '" + category + "' ist nicht für die Preisberechnung geeignet. Artikel: "
                    + article.getName());
            throw new IllegalArgumentException(
                    "Article category must be Reinigungsmittel, Geschirrreiniger or Seife for filling price calculation. Article category: "
                            + category);
        }

        if (details == null || details.isBlank()) {
            showErrorDialog("Artikeldetails fehlen für Artikel: " + article.getName());
            throw new IllegalArgumentException("Article details cannot be null or empty");
        }

        Double liter = extractLiterValue(details);
        if (liter != null) {
            return calculatePriceForFilling(article.getSellPrice(), liter, fillingAmount, unit);
        }

        showErrorDialog("Artikeldetails enthalten keine gültige Literangabe für Artikel: " + article.getName());
        throw new IllegalArgumentException("Article details do not contain valid liter information: " + details);
    }

    private static void showErrorDialog(String message) {
        showTimedMessage("Fehler", message, JOptionPane.ERROR_MESSAGE);
    }

    private static void showTimedMessage(String title, String message, int messageType) {
        new MessageDialog()
                .setTitle(title)
                .setMessage(message)
                .setDuration(5000)
                .setMessageType(messageType)
                .display();
    }

    private static String promptForSeparatedVariant(Article article) {
        List<String> variants = getCurrentSeparatedDetails(article);
        if (variants.isEmpty()) {
            return null;
        }

        String[] options = variants.toArray(new String[0]);
        int choice = new MessageDialog()
                .setTitle("Variante auswählen")
                .setMessage("Wählen Sie die Variante für \"" + article.getName() + "\":")
                .setOptionType(JOptionPane.DEFAULT_OPTION)
                .setOptions(options)
                .displayWithOptions();
        if (choice < 0 || choice >= options.length) {
            return null;
        }
        return options[choice];
    }

    private static Integer promptForQuantity(String articleNumber, Article article) {
        String input = new MessageDialog()
                .setTitle("Menge eingeben")
                .setMessage("Menge für Artikel (" + articleNumber + ") " + article.getName() + " eingeben:")
                .setInitialInputValue("1")
                .setMessageType(JOptionPane.QUESTION_MESSAGE)
                .displayWithStringInput();
        if (input == null) {
            return null;
        }

        try {
            int quantity = Integer.parseInt(input.trim());
            if (quantity <= 0) {
                showTimedMessage("Ungültige Eingabe", "Bitte geben Sie eine Menge größer als 0 ein.", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return quantity;
        } catch (NumberFormatException ex) {
            showTimedMessage("Ungültige Eingabe", "Bitte geben Sie eine gültige Zahl für die Menge ein.", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    private static boolean addSelectedArticleToClientOrder(JTable articleTable, DefaultTableModel model, int selectedRow,
                                                           List<Article> articlesToAdd) {
        int modelRow = articleTable.convertRowIndexToModel(selectedRow);
        String articleNumber = (String) model.getValueAt(modelRow, 0);
        Article article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
        if (article == null) {
            return true;
        }

        String pickedVariant = promptForSeparatedVariant(article);
        if (!getCurrentSeparatedDetails(article).isEmpty() && pickedVariant == null) {
            return false;
        }

        Integer quantity = promptForQuantity(articleNumber, article);
        if (quantity == null) {
            return true;
        }

        ArticleListGUI.addArticle(article, quantity, pickedVariant, null);
        articlesToAdd.add(article);
        return true;
    }

    public static void addSelectedArticlesToClientOrder(JFrame frame, JTable articleTable) {
        int[] selectedRows = articleTable.getSelectedRows();
        if (selectedRows.length == 0) {
            showTimedMessage(
                    "Keine Auswahl",
                    "Bitte wählen Sie mindestens einen Artikel aus, um ihn zur Kundenbestellung hinzuzufügen.",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Article> articlesToAdd = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) articleTable.getModel();

        for (int selectedRow : selectedRows) {
            if (!addSelectedArticleToClientOrder(articleTable, model, selectedRow, articlesToAdd)) {
                return;
            }
        }

        if (articlesToAdd.isEmpty()) {
            showTimedMessage("Fehler", "Keine gültigen Artikel zum Hinzufügen gefunden.", JOptionPane.ERROR_MESSAGE);
            return;
        }

        showTimedMessage("Erfolg", "Artikel zur Kundenbestellung hinzugefügt.", JOptionPane.INFORMATION_MESSAGE);
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

    public static List<String> getPartsFromDetails(String details) {
        return getPartsFromDetails(details, false);
    }

    /**
     * Splits details by any common separator and optionally cleans each part by
     * removing
     * leading abbreviated label prefixes (e.g. {@code "Gr. "}) and trailing
     * quantity/unit
     * suffixes (e.g. {@code " 100 Stk. Box"}), so that
     * {@code "Gr. S, M, L, XL 100 Stk. Box"}
     * yields {@code ["S", "M", "L", "XL"]}.
     *
     * @param details     the raw details string
     * @param cleanTokens if {@code true}, strip label prefixes and quantity
     *                    suffixes from each part
     */
    public static List<String> getPartsFromDetails(String details, boolean cleanTokens) {
        if (details == null || details.isBlank()) {
            return List.of();
        }

        String[] parts = DETAILS_SPLIT_PATTERN.split(details);
        List<String> trimmedParts = new ArrayList<>(parts.length);
        for (String part : parts) {
            String token = part.trim();
            if (token.isBlank()) {
                continue;
            }
            if (cleanTokens) {
                token = PART_LABEL_PREFIX.matcher(token).replaceFirst("");
                token = PART_QUANTITY_SUFFIX.matcher(token).replaceFirst("").trim();
                token = PART_LEADING_PUNCT.matcher(token).replaceFirst("");
                token = PART_TRAILING_PUNCT.matcher(token).replaceFirst("").trim();
            }
            if (!token.isBlank()) {
                trimmedParts.add(token);
            }
        }

        return trimmedParts.isEmpty() ? List.of() : trimmedParts;
    }

    public static List<String> getExactDetailOptions(Article article) {
        if (article == null) {
            return List.of();
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        ArticleManager articleManager = ArticleManager.getInstance();
        String articleNumber = article.getArticleNumber();

        if (articleNumber != null && !articleNumber.isBlank()
                && articleManager.hasSeperateArticles(articleNumber)) {
            for (String detail : articleManager.getAllDetailsForArticleNumber(articleNumber)) {
                String normalized = normalizeMetadataValue(detail);
                if (!normalized.isBlank()) {
                    values.add(normalized);
                }
            }
        }

        for (String part : getPartsFromDetails(article.getDetails(), true)) {
            String normalized = normalizeMetadataValue(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }

        return values.isEmpty() ? List.of() : new ArrayList<>(values);
    }

    public static List<String> getCurrentSeparatedDetails(Article article) {
        if (article == null || article.getArticleNumber() == null || article.getArticleNumber().isBlank()) {
            return List.of();
        }
        if (!isArticleSeparated(article.getArticleNumber())) {
            return List.of();
        }

        List<String> details = new ArrayList<>();
        for (SeperateArticle separatedArticle : newSeperatedArticles(article.getArticleNumber())) {
            if (separatedArticle == null || separatedArticle.getOtherDetails() == null
                    || separatedArticle.getOtherDetails().isBlank()) {
                continue;
            }
            details.add(separatedArticle.getOtherDetails());
        }
        return details.isEmpty() ? List.of() : details;
    }

    private static boolean isSeparatedVariantPart(String part) {
        return part != null && part.matches("[\\p{L}]+");
    }

    private static LinkedHashSet<String> getSeparatedParts(Article article) {
        if (article == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(getPartsFromDetails(article.getDetails(), true));
    }

    public static boolean isArticleSeparated(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return false;
        }

        Article article = ArticleManager.getInstance().getArticleByNumber(articleNumber);
        if (article == null) {
            return false;
        }
        List<String> parts = new ArrayList<>(getSeparatedParts(article));
        if (parts.size() <= 1) {
            return false;
        }
        // Only treat as variants if every part is purely alphabetic (e.g. S/M/L/XL,
        // Blau/Gelb/Grün)
        // Parts containing digits (e.g. "50mm", "25m Rolle") are dimension specs, not
        // variants.
        return parts.stream().allMatch(ArticleUtils::isSeparatedVariantPart);
    }

    public static List<SeperateArticle> newSeperatedArticles(String articleNumber) {
        if (articleNumber == null || articleNumber.isBlank()) {
            return List.of();
        }
        ArticleManager articleManager = ArticleManager.getInstance();
        Article article = articleManager.getArticleByNumber(articleNumber);
        if (article == null) {
            return List.of();
        }

        List<SeperateArticle> allExistingArticles = articleManager.getAllSeperateArticles();
        Set<Integer> existingIds = new HashSet<>(allExistingArticles.stream()
                .map(SeperateArticle::getIndex)
                .toList());
        Map<String, SeperateArticle> existingByDetail = new HashMap<>();
        for (SeperateArticle existing : articleManager.getAllFromArticleNumber(articleNumber)) {
            if (existing == null || existing.getOtherDetails() == null || existing.getOtherDetails().isBlank()) {
                continue;
            }
            existingByDetail.putIfAbsent(existing.getOtherDetails(), existing);
        }

        int nextIndex = allExistingArticles.stream()
                .mapToInt(SeperateArticle::getIndex)
                .max()
                .orElse(0) + 1;

        LinkedHashSet<String> parts = getSeparatedParts(article);
        List<SeperateArticle> result = new ArrayList<>(parts.size());
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }

            SeperateArticle existing = existingByDetail.get(part);
            if (existing != null) {
                result.add(new SeperateArticle(existing.getIndex(), articleNumber, part));
                continue;
            }

            while (existingIds.contains(nextIndex)) {
                nextIndex++;
            }
            existingIds.add(nextIndex);
            result.add(new SeperateArticle(nextIndex, articleNumber, part));
            nextIndex++;
        }
        return result;
    }
}
