package ch.framedev.lagersystem.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves article numbers to category names based on range definitions.
 */
public final class CategoryResolver {

    private final Map<String, CategoryRange> ranges;

    private record CategoryRange(String category, int rangeStart, int rangeEnd) {
    }

    private CategoryResolver(Map<String, CategoryRange> ranges) {
        this.ranges = ranges;
    }

    public static CategoryResolver fromResource(String resourcePath) {
        Map<String, CategoryRange> result = new HashMap<>();
        try (InputStream is = CategoryResolver.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                return new CategoryResolver(result);
            }

            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<Map<String, String>>>() {
            }.getType();
            List<Map<String, String>> categoryList = gson.fromJson(
                    new InputStreamReader(is, StandardCharsets.UTF_8),
                    listType
            );

            if (categoryList == null) {
                return new CategoryResolver(result);
            }

            for (Map<String, String> cat : categoryList) {
                String categoryName = cat.get("category");
                String fromTo = cat.get("fromTo");

                if (categoryName == null || fromTo == null) {
                    continue;
                }

                String[] parts = fromTo.split("-");
                int start;
                int end;

                if (parts.length == 2) {
                    start = Integer.parseInt(parts[0].trim());
                    end = Integer.parseInt(parts[1].trim());
                } else {
                    start = end = Integer.parseInt(parts[0].trim());
                }

                result.put(categoryName, new CategoryRange(categoryName, start, end));
            }
        } catch (Exception ignored) {
            return new CategoryResolver(result);
        }

        return new CategoryResolver(result);
    }

    public Map<String, String> getCategoryNames() {
        Map<String, String> names = new HashMap<>();
        for (String key : ranges.keySet()) {
            names.put(key, key);
        }
        return names;
    }

    public String resolveCategory(String articleNumber) {
        if (articleNumber == null) {
            return "Unbekannt";
        }

        try {
            String numericPart = articleNumber.replaceAll("[^0-9]", "");
            if (numericPart.isEmpty()) {
                return "Unbekannt";
            }

            int articleNum = Integer.parseInt(numericPart);
            for (CategoryRange range : ranges.values()) {
                if (articleNum >= range.rangeStart && articleNum <= range.rangeEnd) {
                    return range.category;
                }
            }
        } catch (NumberFormatException ignored) {
        }

        return "Unbekannt";
    }
}
