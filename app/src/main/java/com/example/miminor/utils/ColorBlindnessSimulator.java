package com.example.miminor.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Симулятор различных типов дальтонизма (цветовой слепоты)
 * Использует матрицы трансформации цветов для имитации восприятия
 */
public class ColorBlindnessSimulator {

    public enum ColorBlindnessType {
        NORMAL("Обычное зрение"),
        PROTANOPIA("Протанопия (красный-зеленый)"),
        DEUTERANOPIA("Дейтеранопия (красный-зеленый)"),
        TRITANOPIA("Тританопия (сине-желтый)"),
        PROTANOMALY("Протаномалия (слабая красный-зеленый)"),
        DEUTERANOMALY("Дейтераномалия (слабая красный-зеленый)"),
        TRITANOMALY("Тританомалия (слабая сине-желтый)"),
        ACHROMATOPSIA("Ахроматопсия (полная цветовая слепота)");

        private final String displayName;

        ColorBlindnessType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Применяет симуляцию дальтонизма к изображению
     */
    public static Bitmap applyColorBlindnessFilter(Bitmap source, ColorBlindnessType type) {
        if (source == null || type == ColorBlindnessType.NORMAL) {
            return source;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        Bitmap result = Bitmap.createBitmap(width, height, source.getConfig());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int transformedPixel = transformColor(pixel, type);
                result.setPixel(x, y, transformedPixel);
            }
        }

        return result;
    }

    /**
     * Трансформирует цвет согласно типу дальтонизма
     */
    public static int transformColor(int color, ColorBlindnessType type) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);

        float[] rgb = new float[]{r / 255f, g / 255f, b / 255f};
        float[] transformed = applyColorMatrix(rgb, getTransformationMatrix(type));

        int newR = clamp((int) (transformed[0] * 255));
        int newG = clamp((int) (transformed[1] * 255));
        int newB = clamp((int) (transformed[2] * 255));

        return Color.argb(a, newR, newG, newB);
    }

    /**
     * Возвращает матрицу трансформации для типа дальтонизма
     */
    private static float[][] getTransformationMatrix(ColorBlindnessType type) {
        switch (type) {
            case PROTANOPIA:
                // Отсутствие L-колбочек (красные)
                return new float[][]{
                    {0.567f, 0.433f, 0.0f},
                    {0.558f, 0.442f, 0.0f},
                    {0.0f, 0.242f, 0.758f}
                };

            case DEUTERANOPIA:
                // Отсутствие M-колбочек (зеленые)
                return new float[][]{
                    {0.625f, 0.375f, 0.0f},
                    {0.7f, 0.3f, 0.0f},
                    {0.0f, 0.3f, 0.7f}
                };

            case TRITANOPIA:
                // Отсутствие S-колбочек (синие)
                return new float[][]{
                    {0.95f, 0.05f, 0.0f},
                    {0.0f, 0.433f, 0.567f},
                    {0.0f, 0.475f, 0.525f}
                };

            case PROTANOMALY:
                // Аномальные L-колбочки (слабая форма)
                return new float[][]{
                    {0.817f, 0.183f, 0.0f},
                    {0.333f, 0.667f, 0.0f},
                    {0.0f, 0.125f, 0.875f}
                };

            case DEUTERANOMALY:
                // Аномальные M-колбочки (слабая форма)
                return new float[][]{
                    {0.8f, 0.2f, 0.0f},
                    {0.258f, 0.742f, 0.0f},
                    {0.0f, 0.142f, 0.858f}
                };

            case TRITANOMALY:
                // Аномальные S-колбочки (слабая форма)
                return new float[][]{
                    {0.967f, 0.033f, 0.0f},
                    {0.0f, 0.733f, 0.267f},
                    {0.0f, 0.183f, 0.817f}
                };

            case ACHROMATOPSIA:
                // Полная цветовая слепота (монохромное зрение)
                return new float[][]{
                    {0.299f, 0.587f, 0.114f},
                    {0.299f, 0.587f, 0.114f},
                    {0.299f, 0.587f, 0.114f}
                };

            case NORMAL:
            default:
                // Единичная матрица (без изменений)
                return new float[][]{
                    {1.0f, 0.0f, 0.0f},
                    {0.0f, 1.0f, 0.0f},
                    {0.0f, 0.0f, 1.0f}
                };
        }
    }

    /**
     * Применяет матрицу трансформации к RGB значениям
     */
    private static float[] applyColorMatrix(float[] rgb, float[][] matrix) {
        float[] result = new float[3];

        for (int i = 0; i < 3; i++) {
            result[i] = matrix[i][0] * rgb[0] +
                       matrix[i][1] * rgb[1] +
                       matrix[i][2] * rgb[2];
        }

        return result;
    }

    /**
     * Ограничивает значение в диапазоне [0, 255]
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * Возвращает название цвета, адаптированное для типа дальтонизма
     */
    public static String getAdaptedColorName(int color, ColorBlindnessType type) {
        int transformedColor = transformColor(color, type);
        return ColorNameMapper.getColorName(transformedColor) +
               " (как воспринимается при " + type.getDisplayName().toLowerCase() + ")";
    }

    /**
     * Проверяет, насколько различимы два цвета при данном типе дальтонизма
     */
    public static boolean areDistinguishable(int color1, int color2, ColorBlindnessType type) {
        int transformed1 = transformColor(color1, type);
        int transformed2 = transformColor(color2, type);

        int dr = Color.red(transformed1) - Color.red(transformed2);
        int dg = Color.green(transformed1) - Color.green(transformed2);
        int db = Color.blue(transformed1) - Color.blue(transformed2);

        double distance = Math.sqrt(dr * dr + dg * dg + db * db);

        // Порог различимости (настраивается)
        return distance > 50;
    }
}
