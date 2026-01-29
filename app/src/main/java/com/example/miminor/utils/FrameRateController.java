package com.example.miminor.utils;

import android.util.Log;

/**
 * Адаптивный контроллер частоты кадров для потокового режима.
 * Пропускает обработку кадров при перегрузке pipeline.
 */
public class FrameRateController {
    private static final String TAG = "FrameRateController";
    
    private final long targetFrameTimeMs;
    private long lastProcessTime = 0;
    private long lastProcessDuration = 0;
    private int droppedFrames = 0;
    private int processedFrames = 0;

    /**
     * @param targetFps целевая частота кадров (например, 10 для <100мс)
     */
    public FrameRateController(int targetFps) {
        this.targetFrameTimeMs = 1000 / targetFps;
    }

    /**
     * Проверяет, нужно ли обрабатывать текущий кадр.
     */
    public boolean shouldProcessFrame() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastProcessTime;
        
        if (elapsed < targetFrameTimeMs) {
            droppedFrames++;
            return false;
        }
        
        return true;
    }

    /**
     * Уведомляет о начале обработки кадра.
     */
    public void onFrameStart() {
        lastProcessTime = System.currentTimeMillis();
    }

    /**
     * Уведомляет о завершении обработки кадра.
     */
    public void onFrameEnd() {
        lastProcessDuration = System.currentTimeMillis() - lastProcessTime;
        processedFrames++;
        
        if (processedFrames % 30 == 0) {
            float dropRate = (float) droppedFrames / (droppedFrames + processedFrames) * 100;
            Log.d(TAG, String.format("FPS stats: avg=%dms, dropped=%.1f%%", 
                lastProcessDuration, dropRate));
        }
    }

    public long getLastFrameDuration() {
        return lastProcessDuration;
    }

    public void reset() {
        droppedFrames = 0;
        processedFrames = 0;
    }
}
