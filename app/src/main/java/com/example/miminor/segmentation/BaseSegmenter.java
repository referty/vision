package com.example.miminor.segmentation;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for image segmentation algorithms
 */
public abstract class BaseSegmenter {
    protected static final String TAG = "BaseSegmenter";

    /**
     * Сегментация объекта по цвету в точке клика
     */
    public ImageSegment segmentByColor(Bitmap originalBitmap, int targetColor, int x, int y, int sensitivity) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Starting color-based segmentation at (" + x + ", " + y + ") with sensitivity " + sensitivity);

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        Mat img = resizeImage(originalBitmap, getProcessingSize());

        float scaleX = (float) originalWidth / img.cols();
        float scaleY = (float) originalHeight / img.rows();

        int scaledX = (int) (x / scaleX);
        int scaledY = (int) (y / scaleY);

        RegionData region = extractRegionByColor(img, targetColor, scaledX, scaledY, sensitivity);
        
        img.release();

        if (region == null) {
            return null;
        }

        android.graphics.Rect bounds = new android.graphics.Rect(
            (int) (region.bounds.x * scaleX),
            (int) (region.bounds.y * scaleY),
            (int) ((region.bounds.x + region.bounds.width) * scaleX),
            (int) ((region.bounds.y + region.bounds.height) * scaleY)
        );

        int centerX = bounds.centerX();
        int centerY = bounds.centerY();
        int color = originalBitmap.getPixel(centerX, centerY);
        String colorName = com.example.miminor.utils.ColorNameMapper.getColorName(color);
        com.example.miminor.utils.ColorInfo colorInfo =
            new com.example.miminor.utils.ColorInfo(color, colorName);

        List<Point> contourPoints = new ArrayList<>();
        if (usesContours() && region.contourPoints != null) {
            for (org.opencv.core.Point p : region.contourPoints) {
                contourPoints.add(new Point((int) (p.x * scaleX), (int) (p.y * scaleY)));
            }
        }

        ImageSegment segment = new ImageSegment(0, bounds, null, colorInfo, 1.0f);
        segment.setContourPoints(contourPoints);

        long elapsed = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Segmentation completed in " + elapsed + "ms");

        return segment;
    }

    /**
     * Analyze image and extract segments
     */
    public SegmentationResult analyze(Bitmap originalBitmap) {
        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Starting analysis (" + getAlgorithmName() + "): "
            + originalBitmap.getWidth() + "x" + originalBitmap.getHeight());

        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();

        Mat img = resizeImage(originalBitmap, getProcessingSize());

        float scaleX = (float) originalWidth / img.cols();
        float scaleY = (float) originalHeight / img.rows();

        List<RegionData> regions = extractRegions(img);

        List<ImageSegment> segments = convertToSegments(regions, originalBitmap, scaleX, scaleY);

        img.release();

        long elapsed = System.currentTimeMillis() - startTime;
        Log.d(TAG, "Analysis completed in " + elapsed + "ms, found " + segments.size() + " regions");

        return new SegmentationResult(true, segments, null);
    }

    /**
     * Extract single region by color at specific point
     */
    protected abstract RegionData extractRegionByColor(Mat img, int targetColor, int x, int y, int sensitivity);

    /**
     * Extract regions from processed image - implemented by subclasses
     */
    protected abstract List<RegionData> extractRegions(Mat img);

    /**
     * Get algorithm name for logging
     */
    protected abstract String getAlgorithmName();

    /**
     * Get processing size (max dimension)
     */
    protected int getProcessingSize() {
        return 200;
    }

    /**
     * Whether this segmenter uses contours or boxes
     */
    protected abstract boolean usesContours();

    /**
     * Resize image to processing size
     */
    protected Mat resizeImage(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap resized = bitmap;
        if (Math.max(width, height) > maxSize) {
            float scale = (float) maxSize / Math.max(width, height);
            int newWidth = (int) (width * scale);
            int newHeight = (int) (height * scale);
            resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
        }

        Mat mat = new Mat();
        Utils.bitmapToMat(resized, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);

        if (resized != bitmap) {
            resized.recycle();
        }

        return mat;
    }

    /**
     * Convert OpenCV regions to ImageSegments with original image coordinates
     */
    protected List<ImageSegment> convertToSegments(List<RegionData> regions,
                                                   Bitmap originalBitmap,
                                                   float scaleX, float scaleY) {
        List<ImageSegment> segments = new ArrayList<>();
        int id = 0;

        for (RegionData region : regions) {
            android.graphics.Rect bounds = new android.graphics.Rect(
                (int) (region.bounds.x * scaleX),
                (int) (region.bounds.y * scaleY),
                (int) ((region.bounds.x + region.bounds.width) * scaleX),
                (int) ((region.bounds.y + region.bounds.height) * scaleY)
            );

            int centerX = bounds.centerX();
            int centerY = bounds.centerY();
            int color = originalBitmap.getPixel(centerX, centerY);
            String colorName = com.example.miminor.utils.ColorNameMapper.getColorName(color);
            com.example.miminor.utils.ColorInfo colorInfo =
                new com.example.miminor.utils.ColorInfo(color, colorName);

            List<Point> contourPoints = new ArrayList<>();
            if (usesContours() && region.contourPoints != null) {
                for (org.opencv.core.Point p : region.contourPoints) {
                    contourPoints.add(new Point((int) (p.x * scaleX), (int) (p.y * scaleY)));
                }
            }

            ImageSegment segment = new ImageSegment(id++, bounds, null, colorInfo, 1.0f);
            segment.setContourPoints(contourPoints);
            segments.add(segment);
        }

        return segments;
    }

    /**
     * Region data extracted from image
     */
    protected static class RegionData {
        public org.opencv.core.Rect bounds;
        public List<org.opencv.core.Point> contourPoints;
        public int area;
        public int[] color;

        public RegionData(org.opencv.core.Rect bounds, int area) {
            this.bounds = bounds;
            this.area = area;
            this.contourPoints = new ArrayList<>();
        }

        public RegionData(org.opencv.core.Rect bounds, int area, List<org.opencv.core.Point> contourPoints) {
            this.bounds = bounds;
            this.area = area;
            this.contourPoints = contourPoints;
        }
    }
}
