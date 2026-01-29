package com.example.miminor.utils;

/**
 * Represents a color in the OKLAB perceptually uniform color space.
 * OKLAB provides significantly better perceptual uniformity than HSV (RMS error 0.20 vs 11.59).
 * 
 * @see <a href="https://bottosson.github.io/posts/oklab/">OKLAB specification</a>
 */
public class OklabColor {
    public final float L;
    public final float a;
    public final float b;

    public OklabColor(float L, float a, float b) {
        this.L = L;
        this.a = a;
        this.b = b;
    }

    /**
     * Calculates chroma (saturation) in OKLAB space.
     * Chroma = sqrt(a² + b²)
     */
    public float getChroma() {
        return (float) Math.sqrt(a * a + b * b);
    }

    /**
     * Calculates hue angle in degrees [0, 360).
     * Uses atan2 to handle all quadrants correctly.
     */
    public float getHue() {
        float hue = (float) Math.toDegrees(Math.atan2(b, a));
        return hue < 0 ? hue + 360 : hue;
    }

    @Override
    public String toString() {
        return String.format("OKLAB(L=%.3f, a=%.3f, b=%.3f)", L, a, b);
    }
}
