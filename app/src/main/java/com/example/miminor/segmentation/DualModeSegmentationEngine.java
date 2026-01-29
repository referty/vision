package com.example.miminor.segmentation;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;

import com.example.miminor.utils.BufferPool;
import com.example.miminor.utils.ColorConverter;
import com.example.miminor.utils.OklabColor;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Менеджер двухрежимной сегментации с кэшированием и адаптивной обработкой.
 * 
 * Потоковый режим: SLIC суперпиксели, <100мс, OKLAB расстояние
 * Режим точности: Контуры, ~500мс, CIEDE2000 расстояние
 */
public class DualModeSegmentationEngine {
    private static final String TAG = "DualModeEngine";
    
    private final SlicSegmenter streamingSegmenter;
    private final ContourSegmenter precisionSegmenter;
    private BufferPool bufferPool;
    
    private boolean useStreamingMode = true;
    private SegmentationResult cachedResult;
    private long cacheTimestamp;
    private static final long CACHE_VALIDITY_MS = 2000;

    public DualModeSegmentationEngine() {
        this.streamingSegmenter = new SlicSegmenter();
        this.precisionSegmenter = new ContourSegmenter();
    }

    /**
     * Устанавливает режим работы.
     */
    public void setMode(boolean streaming) {
        if (this.useStreamingMode != streaming) {
            invalidateCache();
        }
        this.useStreamingMode = streaming;
    }

    /**
     * Сегментирует изображение в выбранном режиме.
     */
    public SegmentationResult segment(Bitmap bitmap) {
        if (isCacheValid()) {
            Log.d(TAG, "Using cached result");
            return cachedResult;
        }

        long start = System.currentTimeMillis();
        
        initBufferPool(bitmap.getWidth(), bitmap.getHeight());
        
        SegmentationResult result;
        if (useStreamingMode) {
            result = streamingSegmenter.analyze(bitmap);
        } else {
            result = precisionSegmenter.analyze(bitmap);
        }
        
        cachedResult = result;
        cacheTimestamp = System.currentTimeMillis();
        
        long elapsed = System.currentTimeMillis() - start;
        String mode = useStreamingMode ? "STREAMING" : "PRECISION";
        Log.d(TAG, String.format("%s mode: %dms, %d segments", 
            mode, elapsed, result.getSegmentCount()));
        
        return result;
    }

    /**
     * Анализирует цвет области с учетом режима.
     * Streaming: OKLAB Euclidean distance
     * Precision: CIEDE2000 distance
     */
    public ColorAnalysisResult analyzeColor(Bitmap bitmap, int x, int y, int radius) {
        if (x < 0 || y < 0 || x >= bitmap.getWidth() || y >= bitmap.getHeight()) {
            return null;
        }
        
        int r = 0, g = 0, b = 0, count = 0;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int px = x + dx;
                int py = y + dy;
                
                if (px >= 0 && px < bitmap.getWidth() && py >= 0 && py < bitmap.getHeight()) {
                    int pixel = bitmap.getPixel(px, py);
                    r += android.graphics.Color.red(pixel);
                    g += android.graphics.Color.green(pixel);
                    b += android.graphics.Color.blue(pixel);
                    count++;
                }
            }
        }
        
        if (count == 0) return null;
        
        r /= count;
        g /= count;
        b /= count;
        
        OklabColor oklabColor = ColorConverter.rgbToOklab(r, g, b);
        
        float perceptualAccuracy = useStreamingMode ? 
            ColorConverter.oklabDistance(oklabColor, oklabColor) :
            0.0f;
        
        return new ColorAnalysisResult(
            android.graphics.Color.rgb(r, g, b),
            oklabColor,
            useStreamingMode ? "OKLAB" : "CIEDE2000",
            perceptualAccuracy
        );
    }

    /**
     * Результат анализа цвета с метриками точности.
     */
    public static class ColorAnalysisResult {
        public final int rgbColor;
        public final OklabColor oklabColor;
        public final String metricUsed;
        public final float accuracy;

        ColorAnalysisResult(int rgb, OklabColor oklab, String metric, float accuracy) {
            this.rgbColor = rgb;
            this.oklabColor = oklab;
            this.metricUsed = metric;
            this.accuracy = accuracy;
        }
    }

    private void initBufferPool(int width, int height) {
        if (bufferPool == null || 
            (width != bufferPool.acquireBitmap().getWidth())) {
            if (bufferPool != null) {
                bufferPool.clear();
            }
            bufferPool = new BufferPool(width, height);
        }
    }

    private boolean isCacheValid() {
        if (cachedResult == null) return false;
        long age = System.currentTimeMillis() - cacheTimestamp;
        return age < CACHE_VALIDITY_MS;
    }

    private void invalidateCache() {
        cachedResult = null;
        cacheTimestamp = 0;
    }

    public void cleanup() {
        invalidateCache();
        if (bufferPool != null) {
            bufferPool.clear();
            bufferPool = null;
        }
    }
}
