package com.example.miminor.utils;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

/**
 * LRU-кэш для результатов сегментации.
 * Уменьшает повторные вычисления для одинаковых изображений.
 */
public class SegmentationCache {
    private static final String TAG = "SegmentationCache";
    private static final int MAX_CACHE_SIZE_MB = 10;
    
    private final LruCache<String, CachedResult> cache;
    private int hits = 0;
    private int misses = 0;

    public SegmentationCache() {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = Math.min(MAX_CACHE_SIZE_MB * 1024, maxMemory / 8);
        
        cache = new LruCache<String, CachedResult>(cacheSize) {
            @Override
            protected int sizeOf(String key, CachedResult result) {
                return result.sizeKb;
            }
        };
        
        Log.d(TAG, "Cache initialized with " + cacheSize + "KB");
    }

    /**
     * Генерирует ключ кэша для изображения.
     */
    public String generateKey(Bitmap bitmap, String mode, int sensitivity) {
        return String.format("%dx%d_%s_%d_%d", 
            bitmap.getWidth(), 
            bitmap.getHeight(),
            mode,
            sensitivity,
            bitmap.hashCode()
        );
    }

    /**
     * Получает результат из кэша.
     */
    public <T> T get(String key) {
        CachedResult result = cache.get(key);
        if (result != null && !result.isExpired()) {
            hits++;
            logStats();
            return (T) result.data;
        }
        misses++;
        return null;
    }

    /**
     * Сохраняет результат в кэш.
     */
    public void put(String key, Object data, int sizeKb) {
        cache.put(key, new CachedResult(data, sizeKb));
    }

    /**
     * Очищает кэш.
     */
    public void clear() {
        cache.evictAll();
        hits = 0;
        misses = 0;
    }

    private void logStats() {
        if ((hits + misses) % 20 == 0) {
            float hitRate = (float) hits / (hits + misses) * 100;
            Log.d(TAG, String.format("Cache: hits=%d, misses=%d, rate=%.1f%%", 
                hits, misses, hitRate));
        }
    }

    private static class CachedResult {
        final Object data;
        final int sizeKb;
        final long timestamp;
        static final long VALIDITY_MS = 60000;

        CachedResult(Object data, int sizeKb) {
            this.data = data;
            this.sizeKb = sizeKb;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > VALIDITY_MS;
        }
    }
}
