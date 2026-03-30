package ch.framedev.lagersystem.guis;

import ch.framedev.lagersystem.managers.ThemeManager;

import java.awt.*;

final class SettingsColorService {

    private SettingsColorService() {
    }

    static Color adjustColor(Color color, float factor) {
        if (color == null) {
            return ThemeManager.getButtonBackgroundColor();
        }
        int r = clampColor(Math.round(color.getRed() * (1.0f + factor)));
        int g = clampColor(Math.round(color.getGreen() * (1.0f + factor)));
        int b = clampColor(Math.round(color.getBlue() * (1.0f + factor)));
        return new Color(r, g, b, color.getAlpha());
    }

    static Color getSoftGlassSurface(Color base, boolean mediumIntensity) {
        Color fallback = base == null ? ThemeManager.getCardBackgroundColor() : base;
        int alpha = mediumIntensity
                ? (ThemeManager.isDarkMode() ? 186 : 236)
                : (ThemeManager.isDarkMode() ? 170 : 228);
        return new Color(fallback.getRed(), fallback.getGreen(), fallback.getBlue(), alpha);
    }

    static Color getSoftGlassBorder(boolean mediumIntensity) {
        Color border = ThemeManager.getBorderColor();
        int alpha = mediumIntensity
                ? (ThemeManager.isDarkMode() ? 188 : 196)
                : (ThemeManager.isDarkMode() ? 165 : 175);
        return new Color(border.getRed(), border.getGreen(), border.getBlue(), alpha);
    }

    static Color getSoftGlassHighlight(boolean mediumIntensity) {
        Color light = ThemeManager.isDarkMode()
                ? adjustColor(ThemeManager.getTextPrimaryColor(), -0.35f)
                : ThemeManager.getTextPrimaryColor();
        int alpha = mediumIntensity
                ? (ThemeManager.isDarkMode() ? 96 : 120)
                : (ThemeManager.isDarkMode() ? 70 : 92);
        return new Color(light.getRed(), light.getGreen(), light.getBlue(), alpha);
    }

    static Color getReadableTextColor(Color background) {
        if (background == null) {
            return ThemeManager.getTextPrimaryColor();
        }
        double luminance = (0.2126 * background.getRed()
                + 0.7152 * background.getGreen()
                + 0.0722 * background.getBlue()) / 255.0;
        return luminance > 0.6 ? new Color(20, 20, 20) : Color.WHITE;
    }

    static Color getStatusErrorColor() {
        return ThemeManager.getDangerColor();
    }

    static Color getStatusSuccessColor() {
        return ThemeManager.getSuccessColor();
    }

    private static int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
