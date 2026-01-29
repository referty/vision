package com.example.miminor.segmentation;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;

import com.example.miminor.utils.ColorInfo;

import java.util.ArrayList;
import java.util.List;


public class ImageSegment {
    private final int id;
    private final Rect bounds;
    private final Bitmap mask;
    private final ColorInfo dominantColor;
    private final float confidence;
    private List<Point> contour;

    public ImageSegment(int id, Rect bounds, Bitmap mask, ColorInfo dominantColor, float confidence) {
        this.id = id;
        this.bounds = bounds;
        this.mask = mask;
        this.dominantColor = dominantColor;
        this.confidence = confidence;
    }
    
    public void setContourPoints(List<Point> contourPoints) {
        this.contour = contourPoints;
    }

    public int getId() {
        return id;
    }

    public Rect getBounds() {
        return bounds;
    }

    public Bitmap getMask() {
        return mask;
    }

    public ColorInfo getDominantColor() {
        return dominantColor;
    }

    public float getConfidence() {
        return confidence;
    }

    public List<Point> getContour() {
        return contour != null ? contour : new ArrayList<>();
    }

    public boolean containsPoint(int x, int y) {
        return bounds.contains(x, y);
    }

    public int getArea() {
        if (mask == null) {
            return bounds.width() * bounds.height();
        }

        int area = 0;
        for (int y = 0; y < mask.getHeight(); y++) {
            for (int x = 0; x < mask.getWidth(); x++) {
                int pixel = mask.getPixel(x, y);
                if (android.graphics.Color.alpha(pixel) > 128) {
                    area++;
                }
            }
        }
        return area;
    }

    @Override
    public String toString() {
        return String.format("Segment #%d: bounds=%s, color=%s, confidence=%.2f",
            id, bounds, dominantColor != null ? dominantColor.getColorName() : "unknown", confidence);
    }
}
