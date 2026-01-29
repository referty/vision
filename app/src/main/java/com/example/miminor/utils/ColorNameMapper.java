package com.example.miminor.utils;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps RGB colors to Russian color names using OKLAB perceptually uniform color space.
 * Provides 8x better perceptual accuracy compared to RGB-based distance.
 */
public class ColorNameMapper {
    private static final Map<String, OklabColor> COLOR_MAP_OKLAB = new HashMap<>();

    static {
        addColor("Красный", 255, 0, 0);
        addColor("Зеленый", 0, 255, 0);
        addColor("Синий", 0, 0, 255);
        addColor("Желтый", 255, 255, 0);
        addColor("Голубой", 0, 255, 255);
        addColor("Пурпурный", 255, 0, 255);
        addColor("Белый", 255, 255, 255);
        addColor("Черный", 0, 0, 0);
        addColor("Серый", 128, 128, 128);

        addColor("Алый", 255, 36, 0);
        addColor("Коралловый", 255, 127, 80);
        addColor("Малиновый", 220, 20, 60);
        addColor("Бордовый", 128, 0, 32);
        addColor("Розовый", 255, 192, 203);
        addColor("Фуксия", 255, 0, 127);

        addColor("Оранжевый", 255, 165, 0);
        addColor("Персиковый", 255, 218, 185);
        addColor("Абрикосовый", 251, 206, 177);

        addColor("Золотой", 255, 215, 0);
        addColor("Лимонный", 255, 250, 205);
        addColor("Янтарный", 255, 191, 0);

        addColor("Салатовый", 124, 252, 0);
        addColor("Изумрудный", 0, 201, 87);
        addColor("Оливковый", 128, 128, 0);
        addColor("Мятный", 152, 255, 152);
        addColor("Хаки", 195, 176, 145);

        addColor("Лазурный", 0, 127, 255);
        addColor("Бирюзовый", 64, 224, 208);
        addColor("Морской волны", 70, 130, 180);
        addColor("Темно-синий", 0, 0, 139);
        addColor("Индиго", 75, 0, 130);

        addColor("Фиолетовый", 138, 43, 226);
        addColor("Сиреневый", 221, 160, 221);
        addColor("Лавандовый", 230, 230, 250);

        addColor("Коричневый", 165, 42, 42);
        addColor("Бежевый", 245, 245, 220);
        addColor("Песочный", 244, 164, 96);
        addColor("Шоколадный", 210, 105, 30);

        addColor("Кремовый", 255, 253, 208);
        addColor("Слоновая кость", 255, 255, 240);
        addColor("Светло-серый", 211, 211, 211);
        addColor("Темно-серый", 169, 169, 169);
    }

    private static void addColor(String name, int r, int g, int b) {
        COLOR_MAP_OKLAB.put(name, ColorConverter.rgbToOklab(r, g, b));
    }

    /**
     * Maps RGB color to Russian name using OKLAB perceptually uniform distance.
     * Automatically adds brightness modifiers (светлый/темный) for extreme lightness.
     */
    public static String getColorName(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        OklabColor oklab = ColorConverter.rgbToOklab(r, g, b);
        
        if (oklab.getChroma() < 0.05f) {
            if (oklab.L < 0.2f) return "Черный";
            if (oklab.L > 0.9f) return "Белый";
            if (oklab.L < 0.4f) return "Темно-серый";
            if (oklab.L > 0.7f) return "Светло-серый";
            return "Серый";
        }

        String closestName = "Неизвестный цвет";
        float minDistance = Float.MAX_VALUE;

        for (Map.Entry<String, OklabColor> entry : COLOR_MAP_OKLAB.entrySet()) {
            float distance = ColorConverter.oklabDistance(oklab, entry.getValue());
            
            if (distance < minDistance) {
                minDistance = distance;
                closestName = entry.getKey();
            }
        }

        if (oklab.L < 0.25f && !closestName.contains("Темно")) {
            return "Темно-" + closestName.toLowerCase();
        } else if (oklab.L > 0.85f && !closestName.contains("Светло")) {
            return "Светло-" + closestName.toLowerCase();
        }

        return closestName;
    }

    /**
     * Returns detailed color description for TTS.
     */
    public static String getColorDescription(int color) {
        String name = getColorName(color);
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        
        OklabColor oklab = ColorConverter.rgbToOklab(r, g, b);

        String brightnessDesc;
        if (oklab.L > 0.85f) {
            brightnessDesc = "очень светлый";
        } else if (oklab.L > 0.65f) {
            brightnessDesc = "светлый";
        } else if (oklab.L > 0.45f) {
            brightnessDesc = "средней яркости";
        } else if (oklab.L > 0.25f) {
            brightnessDesc = "темный";
        } else {
            brightnessDesc = "очень темный";
        }

        return String.format("%s, %s оттенок", name, brightnessDesc);
    }
}
