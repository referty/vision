package com.example.miminor.utils;

/**
 * Converter between RGB and OKLAB color spaces with CIEDE2000 distance metric support.
 * OKLAB provides 8x better perceptual uniformity than HSV for color difference calculations.
 */
public class ColorConverter {

    /**
     * Converts sRGB color to OKLAB perceptually uniform color space.
     * 
     * Process:
     * 1. sRGB → Linear RGB (gamma correction)
     * 2. Linear RGB → LMS cone space
     * 3. LMS → LMS' (cube root)
     * 4. LMS' → OKLAB
     * 
     * @param r Red component [0-255]
     * @param g Green component [0-255]
     * @param b Blue component [0-255]
     * @return OKLAB color representation
     */
    public static OklabColor rgbToOklab(int r, int g, int b) {
        float rLin = gammaToLinear(r / 255f);
        float gLin = gammaToLinear(g / 255f);
        float bLin = gammaToLinear(b / 255f);

        float l = 0.4122214708f * rLin + 0.5363325363f * gLin + 0.0514459929f * bLin;
        float m = 0.2119034982f * rLin + 0.6806995451f * gLin + 0.1073969566f * bLin;
        float s = 0.0883024619f * rLin + 0.2817188376f * gLin + 0.6299787005f * bLin;

        float lRoot = cbrt(l);
        float mRoot = cbrt(m);
        float sRoot = cbrt(s);

        return new OklabColor(
            0.2104542553f * lRoot + 0.7936177850f * mRoot - 0.0040720468f * sRoot,
            1.9779984951f * lRoot - 2.4285922050f * mRoot + 0.4505937099f * sRoot,
            0.0259040371f * lRoot + 0.7827717662f * mRoot - 0.8086757660f * sRoot
        );
    }

    /**
     * Converts OKLAB back to sRGB.
     * Inverse transformation of rgbToOklab.
     */
    public static int[] oklabToRgb(OklabColor oklab) {
        float lRoot = oklab.L * 0.9999999985f + 0.3963377774f * oklab.a + 0.2158037573f * oklab.b;
        float mRoot = oklab.L * 1.0000000089f - 0.1055613458f * oklab.a - 0.0638541728f * oklab.b;
        float sRoot = oklab.L * 1.0000000547f - 0.0894841775f * oklab.a - 1.2914855480f * oklab.b;

        float l = lRoot * lRoot * lRoot;
        float m = mRoot * mRoot * mRoot;
        float s = sRoot * sRoot * sRoot;

        float rLin = +4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        float gLin = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        float bLin = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;

        int r = clamp((int) (linearToGamma(rLin) * 255));
        int g = clamp((int) (linearToGamma(gLin) * 255));
        int b = clamp((int) (linearToGamma(bLin) * 255));

        return new int[] { r, g, b };
    }

    /**
     * Calculates Euclidean distance in OKLAB space.
     * ΔE < 1.0: barely noticeable
     * 1.0 < ΔE < 2.3: small difference
     * 2.3 < ΔE < 5.0: noticeable difference
     * ΔE > 5.0: strongly different colors
     * 
     * @return Perceptually uniform color distance
     */
    public static float oklabDistance(OklabColor c1, OklabColor c2) {
        float dL = c2.L - c1.L;
        float da = c2.a - c1.a;
        float db = c2.b - c1.b;
        return (float) Math.sqrt(dL * dL + da * da + db * db);
    }

    /**
     * CIEDE2000 color difference formula - most accurate perceptual metric.
     * Clinical thresholds:
     * ΔE00 = 0.8: just noticeable difference (JND)
     * ΔE00 = 1.8: acceptability threshold for critical applications
     * 
     * Note: More computationally expensive than Euclidean OKLAB distance.
     * Use for precision mode only.
     */
    public static float ciede2000Distance(OklabColor c1, OklabColor c2) {
        int[] rgb1 = oklabToRgb(c1);
        int[] rgb2 = oklabToRgb(c2);
        
        float[] lab1 = rgbToLab(rgb1[0], rgb1[1], rgb1[2]);
        float[] lab2 = rgbToLab(rgb2[0], rgb2[1], rgb2[2]);
        
        return calculateCIEDE2000(lab1, lab2);
    }

    private static float[] rgbToLab(int r, int g, int b) {
        float rLin = gammaToLinear(r / 255f);
        float gLin = gammaToLinear(g / 255f);
        float bLin = gammaToLinear(b / 255f);

        float x = rLin * 0.4124564f + gLin * 0.3575761f + bLin * 0.1804375f;
        float y = rLin * 0.2126729f + gLin * 0.7151522f + bLin * 0.0721750f;
        float z = rLin * 0.0193339f + gLin * 0.1191920f + bLin * 0.9503041f;

        x = x / 0.95047f;
        y = y / 1.00000f;
        z = z / 1.08883f;

        x = x > 0.008856f ? (float) Math.pow(x, 1f/3f) : (7.787f * x + 16f/116f);
        y = y > 0.008856f ? (float) Math.pow(y, 1f/3f) : (7.787f * y + 16f/116f);
        z = z > 0.008856f ? (float) Math.pow(z, 1f/3f) : (7.787f * z + 16f/116f);

        float L = 116f * y - 16f;
        float a = 500f * (x - y);
        float bVal = 200f * (y - z);

        return new float[] { L, a, bVal };
    }

    private static float calculateCIEDE2000(float[] lab1, float[] lab2) {
        float L1 = lab1[0], a1 = lab1[1], b1 = lab1[2];
        float L2 = lab2[0], a2 = lab2[1], b2 = lab2[2];

        float C1 = (float) Math.sqrt(a1 * a1 + b1 * b1);
        float C2 = (float) Math.sqrt(a2 * a2 + b2 * b2);
        float Cbar = (C1 + C2) / 2f;

        float G = 0.5f * (1f - (float) Math.sqrt(Math.pow(Cbar, 7) / (Math.pow(Cbar, 7) + Math.pow(25, 7))));
        float a1p = a1 * (1f + G);
        float a2p = a2 * (1f + G);

        float C1p = (float) Math.sqrt(a1p * a1p + b1 * b1);
        float C2p = (float) Math.sqrt(a2p * a2p + b2 * b2);

        float h1p = Math.abs(a1p) + Math.abs(b1) == 0 ? 0 : (float) Math.atan2(b1, a1p);
        float h2p = Math.abs(a2p) + Math.abs(b2) == 0 ? 0 : (float) Math.atan2(b2, a2p);

        if (h1p < 0) h1p += 2 * Math.PI;
        if (h2p < 0) h2p += 2 * Math.PI;

        float dLp = L2 - L1;
        float dCp = C2p - C1p;

        float dhp;
        if (C1p * C2p == 0) {
            dhp = 0;
        } else {
            dhp = h2p - h1p;
            if (dhp > Math.PI) dhp -= 2 * Math.PI;
            if (dhp < -Math.PI) dhp += 2 * Math.PI;
        }

        float dHp = 2 * (float) Math.sqrt(C1p * C2p) * (float) Math.sin(dhp / 2);

        float Lbar = (L1 + L2) / 2f;
        float Cpbar = (C1p + C2p) / 2f;

        float hpbar;
        if (C1p * C2p == 0) {
            hpbar = h1p + h2p;
        } else {
            hpbar = (h1p + h2p) / 2f;
            if (Math.abs(h1p - h2p) > Math.PI) {
                if (hpbar < Math.PI) hpbar += Math.PI;
                else hpbar -= Math.PI;
            }
        }

        float T = 1 - 0.17f * (float) Math.cos(hpbar - Math.PI/6) +
                  0.24f * (float) Math.cos(2 * hpbar) +
                  0.32f * (float) Math.cos(3 * hpbar + Math.PI/30) -
                  0.20f * (float) Math.cos(4 * hpbar - 63 * Math.PI/180);

        float SL = 1 + (0.015f * (Lbar - 50) * (Lbar - 50)) / (float) Math.sqrt(20 + (Lbar - 50) * (Lbar - 50));
        float SC = 1 + 0.045f * Cpbar;
        float SH = 1 + 0.015f * Cpbar * T;

        float RT = -2 * (float) Math.sqrt(Math.pow(Cpbar, 7) / (Math.pow(Cpbar, 7) + Math.pow(25, 7))) *
                   (float) Math.sin(60 * Math.PI/180 * (float) Math.exp(-Math.pow((hpbar - 275 * Math.PI/180) / (25 * Math.PI/180), 2)));

        return (float) Math.sqrt(
            Math.pow(dLp / SL, 2) +
            Math.pow(dCp / SC, 2) +
            Math.pow(dHp / SH, 2) +
            RT * (dCp / SC) * (dHp / SH)
        );
    }

    private static float gammaToLinear(float value) {
        if (value <= 0.04045f) {
            return value / 12.92f;
        } else {
            return (float) Math.pow((value + 0.055f) / 1.055f, 2.4f);
        }
    }

    private static float linearToGamma(float value) {
        if (value <= 0.0031308f) {
            return value * 12.92f;
        } else {
            return (float) (1.055f * Math.pow(value, 1f/2.4f) - 0.055f);
        }
    }

    private static float cbrt(float x) {
        if (x >= 0) {
            return (float) Math.pow(x, 1f/3f);
        } else {
            return -(float) Math.pow(-x, 1f/3f);
        }
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
