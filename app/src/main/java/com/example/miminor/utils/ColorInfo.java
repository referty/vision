package com.example.miminor.utils;

import android.graphics.Color;

/**
 * Data class для хранения информации о цвете
 */
public class ColorInfo {
    private final int color;
    private final String colorName;
    private final String hexCode;
    private final int red;
    private final int green;
    private final int blue;
    private final double contrast;

    public ColorInfo(int color, String colorName) {
        this.color = color;
        this.colorName = colorName;
        this.red = Color.red(color);
        this.green = Color.green(color);
        this.blue = Color.blue(color);
        this.hexCode = String.format("#%02X%02X%02X", red, green, blue);
        this.contrast = calculateContrast(color);
    }

    /**
     * Вычисляет контрастность цвета относительно белого фона
     * @return значение контрастности (1.0 - 21.0)
     */
    private double calculateContrast(int color) {
        double luminance1 = calculateLuminance(color);
        double luminance2 = calculateLuminance(Color.WHITE);

        double lighter = Math.max(luminance1, luminance2);
        double darker = Math.min(luminance1, luminance2);

        return (lighter + 0.05) / (darker + 0.05);
    }

    /**
     * Вычисляет относительную яркость цвета по формуле WCAG
     */
    private double calculateLuminance(int color) {
        double r = Color.red(color) / 255.0;
        double g = Color.green(color) / 255.0;
        double b = Color.blue(color) / 255.0;

        r = (r <= 0.03928) ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = (g <= 0.03928) ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = (b <= 0.03928) ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);

        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    // Getters
    public int getColor() {
        return color;
    }

    public String getColorName() {
        return colorName;
    }

    public String getHexCode() {
        return hexCode;
    }

    public String getRgbString() {
        return String.format("RGB(%d, %d, %d)", red, green, blue);
    }

    public int getRed() {
        return red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public double getContrast() {
        return contrast;
    }

    public String getContrastRating() {
        if (contrast >= 7.0) {
            return "AAA (Отличная)";
        } else if (contrast >= 4.5) {
            return "AA (Хорошая)";
        } else if (contrast >= 3.0) {
            return "A (Удовлетворительная)";
        } else {
            return "Низкая";
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %s)", colorName, hexCode, getRgbString());
    }
}
