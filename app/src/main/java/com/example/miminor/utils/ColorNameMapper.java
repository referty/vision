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
        // Основные цвета
        addColor("Красный", 255, 0, 0);
        addColor("Зеленый", 0, 255, 0);
        addColor("Синий", 0, 0, 255);
        addColor("Желтый", 255, 255, 0);
        addColor("Голубой", 0, 255, 255);
        addColor("Пурпурный", 255, 0, 255);
        addColor("Белый", 255, 255, 255);
        addColor("Черный", 0, 0, 0);
        addColor("Серый", 128, 128, 128);

        // Красные тона
        addColor("Индийский красный", 205, 92, 92);      // IndianRed
        addColor("Светло-коралловый", 240, 128, 128);    // LightCoral
        addColor("Лососевый", 250, 128, 114);            // Salmon
        addColor("Темно-лососевый", 233, 150, 122);      // DarkSalmon
        addColor("Светло-лососевый", 255, 160, 122);     // LightSalmon
        addColor("Малиновый", 220, 20, 60);              // Crimson
        addColor("Красный", 255, 0, 0);                  // Red (дубль)
        addColor("Кирпичный", 178, 34, 34);               // FireBrick
        addColor("Темно-бордовый", 139, 0, 0);            // DarkRed

        // Розовые тона
        addColor("Розовый", 255, 192, 203);              // Pink
        addColor("Светло-розовый", 255, 182, 193);       // LightPink
        addColor("Ярко-розовый", 255, 105, 180);         // HotPink
        addColor("Темно-розовый", 255, 20, 147);         // DeepPink
        addColor("Средний фиолетово-красный", 199, 21, 133); // MediumVioletRed
        addColor("Бледный фиолетово-красный", 219, 112, 147); // PaleVioletRed

        // Оранжевые тона
        addColor("Коралловый", 255, 127, 80);            // Coral
        addColor("Томатный", 255, 99, 71);               // Tomato
        addColor("Красно-оранжевый", 255, 69, 0);        // OrangeRed
        addColor("Темно-оранжевый", 255, 140, 0);        // DarkOrange
        addColor("Оранжевый", 255, 165, 0);              // Orange

        // Жёлтые тона
        addColor("Золотой", 255, 215, 0);                // Gold
        addColor("Желтый", 255, 255, 0);                 // Yellow (дубль)
        addColor("Светло-желтый", 255, 255, 224);        // LightYellow
        addColor("Лимонный", 255, 250, 205);             // LemonChiffon
        addColor("Светлый золотисто-желтый", 250, 250, 210); // LightGoldenrodYellow
        addColor("Папайя", 255, 239, 213);               // PapayaWhip
        addColor("Мокасин", 255, 228, 181);              // Moccasin
        addColor("Персиковый", 255, 218, 185);           // PeachPuff
        addColor("Бледно-золотистый", 238, 232, 170);    // PaleGoldenrod
        addColor("Хаки", 240, 230, 140);                 // Khaki
        addColor("Темно-хаки", 189, 183, 107);           // DarkKhaki

        // Фиолетовые тона
        addColor("Лавандовый", 230, 230, 250);           // Lavender
        addColor("Чертополох", 216, 191, 216);           // Thistle
        addColor("Сиреневый", 221, 160, 221);            // Plum
        addColor("Фиолетовый", 238, 130, 238);           // Violet
        addColor("Орхидея", 218, 112, 214);              // Orchid
        addColor("Фуксия", 255, 0, 255);                 // Fuchsia
        addColor("Пурпурный", 255, 0, 255);              // Magenta (дубль Fuchsia)
        addColor("Средняя орхидея", 186, 85, 211);       // MediumOrchid
        addColor("Средний пурпурный", 147, 112, 219);    // MediumPurple
        addColor("Сине-фиолетовый", 138, 43, 226);       // BlueViolet
        addColor("Темно-фиолетовый", 148, 0, 211);       // DarkViolet
        addColor("Темная орхидея", 153, 50, 204);        // DarkOrchid
        addColor("Темно-пурпурный", 139, 0, 139);        // DarkMagenta
        addColor("Пурпурный", 128, 0, 128);              // Purple
        addColor("Индиго", 75, 0, 130);                  // Indigo
        addColor("Шиферно-синий", 106, 90, 205);         // SlateBlue
        addColor("Темный шиферно-синий", 72, 61, 139);   // DarkSlateBlue

        // Коричневые тона
        addColor("Кукурузные рыльца", 255, 248, 220);    // Cornsilk
        addColor("Миндаль", 255, 235, 205);              // BlanchedAlmond
        addColor("Бисквит", 255, 228, 196);              // Bisque
        addColor("Навахо", 255, 222, 173);               // NavajoWhite
        addColor("Пшеничный", 245, 222, 179);            // Wheat
        addColor("Древесный", 222, 184, 135);            // BurlyWood
        addColor("Тан", 210, 180, 140);                  // Tan
        addColor("Розово-коричневый", 188, 143, 143);    // RosyBrown
        addColor("Песочный", 244, 164, 96);              // SandyBrown
        addColor("Золотарник", 218, 165, 32);            // Goldenrod
        addColor("Темный золотарник", 184, 134, 11);     // DarkGoldenRod
        addColor("Перу", 205, 133, 63);                  // Peru
        addColor("Шоколадный", 210, 105, 30);            // Chocolate
        addColor("Коричневое седло", 139, 69, 19);       // SaddleBrown
        addColor("Сиенна", 160, 82, 45);                 // Sienna
        addColor("Коричневый", 165, 42, 42);             // Brown
        addColor("Темно-бордовый", 128, 0, 0);           // Maroon

        // Зелёные тона
        addColor("Зеленовато-желтый", 173, 255, 47);     // GreenYellow
        addColor("Шартрез", 127, 255, 0);                // Chartreuse
        addColor("Газонная зелень", 124, 252, 0);        // LawnGreen
        addColor("Лайм", 0, 255, 0);                     // Lime
        addColor("Лаймово-зеленый", 50, 205, 50);        // LimeGreen
        addColor("Бледно-зеленый", 152, 251, 152);       // PaleGreen
        addColor("Светло-зеленый", 144, 238, 144);       // LightGreen
        addColor("Средняя весенняя зелень", 0, 250, 154); // MediumSpringGreen
        addColor("Весенняя зелень", 0, 255, 127);        // SpringGreen
        addColor("Средний морской зеленый", 60, 179, 113); // MediumSeaGreen
        addColor("Морской зеленый", 46, 139, 87);        // SeaGreen
        addColor("Лесной зеленый", 34, 139, 34);         // ForestGreen
        addColor("Зеленый", 0, 128, 0);                  // Green (дубль)
        addColor("Темно-зеленый", 0, 100, 0);            // DarkGreen
        addColor("Желто-зеленый", 154, 205, 50);         // YellowGreen
        addColor("Оливково-зеленый", 107, 142, 35);      // OliveDrab
        addColor("Оливковый", 128, 128, 0);              // Olive
        addColor("Темный оливково-зеленый", 85, 107, 47); // DarkOliveGreen
        addColor("Средний аквамарин", 102, 205, 170);    // MediumAquamarine
        addColor("Темный морской зеленый", 143, 188, 143); // DarkSeaGreen
        addColor("Светлый морской зеленый", 32, 178, 170); // LightSeaGreen
        addColor("Темно-циановый", 0, 139, 139);         // DarkCyan
        addColor("Темно-бирюзовый", 0, 128, 128);        // Teal

        // Синие тона
        addColor("Циан", 0, 255, 255);                   // Aqua / Cyan
        addColor("Светло-циановый", 224, 255, 255);      // LightCyan
        addColor("Бледно-бирюзовый", 175, 238, 238);     // PaleTurquoise
        addColor("Аквамарин", 127, 255, 212);            // Aquamarine
        addColor("Бирюзовый", 64, 224, 208);             // Turquoise
        addColor("Средний бирюзовый", 72, 209, 204);     // MediumTurquoise
        addColor("Темный бирюзовый", 0, 206, 209);       // DarkTurquoise
        addColor("Синий кадет", 95, 158, 160);           // CadetBlue
        addColor("Стальной синий", 70, 130, 180);        // SteelBlue
        addColor("Светлый стальной синий", 176, 196, 222); // LightSteelBlue
        addColor("Пороховой синий", 176, 224, 230);      // PowderBlue
        addColor("Светло-синий", 173, 216, 230);         // LightBlue
        addColor("Небесно-голубой", 135, 206, 235);      // SkyBlue
        addColor("Светлый небесно-голубой", 135, 206, 250); // LightSkyBlue
        addColor("Темный небесно-голубой", 0, 191, 255); // DeepSkyBlue
        addColor("Яркий синий", 30, 144, 255);           // DodgerBlue
        addColor("Васильковый", 100, 149, 237);          // CornflowerBlue
        addColor("Средний шиферно-синий", 123, 104, 238); // MediumSlateBlue
        addColor("Королевский синий", 65, 105, 225);     // RoyalBlue
        addColor("Синий", 0, 0, 255);                    // Blue (дубль)
        addColor("Средне-синий", 0, 0, 205);             // MediumBlue
        addColor("Темно-синий", 0, 0, 139);              // DarkBlue
        addColor("Темно-синий (нави)", 0, 0, 128);       // Navy
        addColor("Полуночно-синий", 25, 25, 112);        // MidnightBlue

        // Белые тона
        addColor("Белый", 255, 255, 255);                // White (дубль)
        addColor("Белоснежный", 255, 250, 250);          // Snow
        addColor("Медвяная роса", 240, 255, 240);        // Honeydew
        addColor("Мятно-кремовый", 245, 255, 250);       // MintCream
        addColor("Лазурный", 240, 255, 255);             // Azure
        addColor("Алиса-синий", 240, 248, 255);          // AliceBlue
        addColor("Призрачно-белый", 248, 248, 255);      // GhostWhite
        addColor("Белый дым", 245, 245, 245);            // WhiteSmoke
        addColor("Морская раковина", 255, 245, 238);     // Seashell
        addColor("Бежевый", 245, 245, 220);              // Beige
        addColor("Старое кружево", 253, 245, 230);       // OldLace
        addColor("Цветочный белый", 255, 250, 240);      // FloralWhite
        addColor("Слоновая кость", 255, 255, 240);       // Ivory
        addColor("Античный белый", 250, 235, 215);       // AntiqueWhite
        addColor("Льняной", 250, 240, 230);              // Linen
        addColor("Лавандовый румянец", 255, 240, 245);   // LavenderBlush
        addColor("Туманная роза", 255, 228, 225);        // MistyRose

        // Серые тона
        addColor("Гейнсборо", 220, 220, 220);            // Gainsboro
        addColor("Светло-серый", 211, 211, 211);         // LightGrey
        addColor("Серебристый", 192, 192, 192);          // Silver
        addColor("Темно-серый", 169, 169, 169);          // DarkGray
        addColor("Серый", 128, 128, 128);                // Gray
        addColor("Тускло-серый", 105, 105, 105);         // DimGray
        addColor("Светлый шиферно-серый", 119, 136, 153); // LightSlateGray
        addColor("Шиферно-серый", 112, 128, 144);        // SlateGray
        addColor("Темный шиферно-серый", 47, 79, 79);    // DarkSlateGray
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
