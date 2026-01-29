package com.example.miminor.utils;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;

/**
 * Пул буферов для избежания аллокаций во время обработки.
 * Уменьшает GC-паузы в потоковом режиме.
 */
public class BufferPool {
    private final int width;
    private final int height;
    private final ArrayDeque<Mat> matPool;
    private final ArrayDeque<Bitmap> bitmapPool;
    private static final int MAX_POOL_SIZE = 5;

    public BufferPool(int width, int height) {
        this.width = width;
        this.height = height;
        this.matPool = new ArrayDeque<>();
        this.bitmapPool = new ArrayDeque<>();
    }

    public Mat acquireMat() {
        Mat mat = matPool.pollFirst();
        if (mat == null) {
            mat = new Mat();
        }
        return mat;
    }

    public void releaseMat(Mat mat) {
        if (mat != null && matPool.size() < MAX_POOL_SIZE) {
            matPool.add(mat);
        } else if (mat != null) {
            mat.release();
        }
    }

    public Bitmap acquireBitmap() {
        Bitmap bitmap = bitmapPool.pollFirst();
        if (bitmap == null) {
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        }
        return bitmap;
    }

    public void releaseBitmap(Bitmap bitmap) {
        if (bitmap != null && bitmapPool.size() < MAX_POOL_SIZE) {
            bitmapPool.add(bitmap);
        } else if (bitmap != null) {
            bitmap.recycle();
        }
    }

    public void clear() {
        for (Mat mat : matPool) {
            mat.release();
        }
        matPool.clear();

        for (Bitmap bitmap : bitmapPool) {
            bitmap.recycle();
        }
        bitmapPool.clear();
    }
}
