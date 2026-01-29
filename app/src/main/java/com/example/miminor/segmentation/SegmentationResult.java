package com.example.miminor.segmentation;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Image segmentation result
 */
public class SegmentationResult {
    private final List<ImageSegment> segments;
    private final long processingTimeMs;
    private final boolean success;
    private final String errorMessage;
    private final Bitmap resultBitmap;

    private SegmentationResult(List<ImageSegment> segments, long processingTimeMs,
                               boolean success, String errorMessage, Bitmap resultBitmap) {
        this.segments = segments != null ? segments : new ArrayList<>();
        this.processingTimeMs = processingTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.resultBitmap = resultBitmap;
    }

    public SegmentationResult(boolean success, List<ImageSegment> segments, Bitmap resultBitmap) {
        this(segments, 0, success, null, resultBitmap);
    }

    public static SegmentationResult success(List<ImageSegment> segments, long processingTimeMs) {
        return new SegmentationResult(segments, processingTimeMs, true, null, null);
    }

    public static SegmentationResult error(String errorMessage) {
        return new SegmentationResult(null, 0, false, errorMessage, null);
    }

    public Bitmap getResultBitmap() {
        return resultBitmap;
    }

    public List<ImageSegment> getSegments() {
        return segments;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Находит сегмент по координатам точки
     */
    public ImageSegment findSegmentAt(int x, int y) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            ImageSegment segment = segments.get(i);
            if (segment.containsPoint(x, y)) {
                return segment;
            }
        }
        return null;
    }

    public int getSegmentCount() {
        return segments.size();
    }

    @Override
    public String toString() {
        if (!success) {
            return "SegmentationResult{error=" + errorMessage + "}";
        }
        return String.format("SegmentationResult{segments=%d, timeMs=%d}",
            segments.size(), processingTimeMs);
    }
}
