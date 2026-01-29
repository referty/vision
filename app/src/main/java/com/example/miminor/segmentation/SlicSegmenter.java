package com.example.miminor.segmentation;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Упрощенная быстрая сегментация для потокового режима.
 * Использует адаптивное квантование цветов + connected components.
 * 
 * Производительность: 50-150мс на 640×480
 * Сложность: O(N) - линейная
 */
public class SlicSegmenter extends BaseSegmenter {
    private static final String TAG = "SlicSegmenter";

    @Override
    protected String getAlgorithmName() {
        return "FAST_STREAMING";
    }

    @Override
    protected boolean usesContours() {
        return false;
    }

    @Override
    protected int getProcessingSize() {
        return 480;
    }

    @Override
    protected RegionData extractRegionByColor(Mat img, int targetColor, int x, int y, int sensitivity) {
        if (x < 0 || y < 0 || x >= img.cols() || y >= img.rows()) {
            return null;
        }

        Mat lab = new Mat();
        Imgproc.cvtColor(img, lab, Imgproc.COLOR_RGB2Lab);
        
        double[] seedColor = lab.get(y, x);
        if (seedColor == null) {
            lab.release();
            return null;
        }
        
        int tolerance = 20 + sensitivity * 2;
        Scalar loDiff = new Scalar(tolerance, tolerance, tolerance);
        Scalar upDiff = new Scalar(tolerance, tolerance, tolerance);
        
        Mat mask = new Mat(lab.rows() + 2, lab.cols() + 2, CvType.CV_8UC1, new Scalar(0));
        org.opencv.core.Rect rect = new org.opencv.core.Rect();
        
        int flags = 4 | (255 << 8) | Imgproc.FLOODFILL_FIXED_RANGE | Imgproc.FLOODFILL_MASK_ONLY;
        Imgproc.floodFill(lab, mask, new org.opencv.core.Point(x, y), 
            new Scalar(0, 0, 0), rect, loDiff, upDiff, flags);
        
        Mat maskCropped = mask.submat(1, mask.rows() - 1, 1, mask.cols() - 1);
        
        int area = Core.countNonZero(maskCropped);
        
        if (area < 50) {
            lab.release();
            mask.release();
            return null;
        }
        
        Mat roi = img.submat(rect);
        Scalar meanColor = Core.mean(roi, maskCropped.submat(rect));
        roi.release();
        
        RegionData region = new RegionData(rect, area);
        region.color = new int[]{
            (int) meanColor.val[0],
            (int) meanColor.val[1],
            (int) meanColor.val[2]
        };
        
        lab.release();
        mask.release();
        
        return region;
    }

    @Override
    protected List<RegionData> extractRegions(Mat img) {
        long start = System.currentTimeMillis();
        
        Mat downscaled = new Mat();
        Size targetSize = new Size(img.cols() / 2, img.rows() / 2);
        Imgproc.resize(img, downscaled, targetSize, 0, 0, Imgproc.INTER_LINEAR);
        
        Mat lab = new Mat();
        Imgproc.cvtColor(downscaled, lab, Imgproc.COLOR_RGB2Lab);
        
        Mat quantized = quantizeColors(lab, 32);
        
        Mat labels = new Mat(quantized.size(), CvType.CV_32S);
        int numLabels = Imgproc.connectedComponents(quantized, labels, 8, CvType.CV_32S);
        
        List<RegionData> regions = extractConnectedRegions(downscaled, labels, numLabels);
        
        for (RegionData region : regions) {
            region.bounds.x *= 2;
            region.bounds.y *= 2;
            region.bounds.width *= 2;
            region.bounds.height *= 2;
        }
        
        downscaled.release();
        lab.release();
        quantized.release();
        labels.release();
        
        long elapsed = System.currentTimeMillis() - start;
        Log.d(TAG, String.format("Fast segmentation: %d regions in %dms", regions.size(), elapsed));
        
        return regions;
    }

    private Mat quantizeColors(Mat lab, int levels) {
        Mat quantized = new Mat(lab.size(), CvType.CV_8UC1);
        
        byte[] labData = new byte[(int) (lab.total() * lab.channels())];
        lab.get(0, 0, labData);
        
        byte[] outData = new byte[(int) quantized.total()];
        
        for (int i = 0; i < labData.length; i += 3) {
            int L = labData[i] & 0xFF;
            int a = labData[i+1] & 0xFF;
            int b = labData[i+2] & 0xFF;
            
            int qL = (L / (256 / 4)) * (256 / 4);
            int qa = (a / (256 / 4)) * (256 / 4);
            int qb = (b / (256 / 4)) * (256 / 4);
            
            int hash = (qL / 4) * 64 + (qa / 4) * 8 + (qb / 4);
            outData[i / 3] = (byte) (hash % 256);
        }
        
        quantized.put(0, 0, outData);
        return quantized;
    }

    private List<RegionData> extractConnectedRegions(Mat img, Mat labels, int numLabels) {
        Map<Integer, ComponentStats> statsMap = new HashMap<>();
        
        int[] labelsData = new int[(int) labels.total()];
        labels.get(0, 0, labelsData);
        
        for (int y = 0; y < labels.rows(); y++) {
            for (int x = 0; x < labels.cols(); x++) {
                int idx = y * labels.cols() + x;
                int label = labelsData[idx];
                
                if (label == 0) continue;
                
                ComponentStats stats = statsMap.get(label);
                if (stats == null) {
                    stats = new ComponentStats();
                    statsMap.put(label, stats);
                }
                
                stats.minX = Math.min(stats.minX, x);
                stats.minY = Math.min(stats.minY, y);
                stats.maxX = Math.max(stats.maxX, x);
                stats.maxY = Math.max(stats.maxY, y);
                stats.area++;
            }
        }
        
        List<RegionData> regions = new ArrayList<>();
        
        for (Map.Entry<Integer, ComponentStats> entry : statsMap.entrySet()) {
            ComponentStats stats = entry.getValue();
            
            if (stats.area < 100) continue;
            
            int width = stats.maxX - stats.minX + 1;
            int height = stats.maxY - stats.minY + 1;
            
            if (width < 10 || height < 10) continue;
            
            org.opencv.core.Rect rect = new org.opencv.core.Rect(
                stats.minX, stats.minY, width, height
            );
            
            Mat roi = img.submat(rect);
            Scalar meanColor = Core.mean(roi);
            roi.release();
            
            RegionData region = new RegionData(rect, stats.area);
            region.color = new int[]{
                (int) meanColor.val[0],
                (int) meanColor.val[1],
                (int) meanColor.val[2]
            };
            
            regions.add(region);
        }
        
        Collections.sort(regions, new Comparator<RegionData>() {
            @Override
            public int compare(RegionData r1, RegionData r2) {
                return Integer.compare(r2.area, r1.area);
            }
        });
        
        if (regions.size() > 40) {
            regions = regions.subList(0, 40);
        }
        
        return regions;
    }

    private static class ComponentStats {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;
        int area = 0;
    }
}
