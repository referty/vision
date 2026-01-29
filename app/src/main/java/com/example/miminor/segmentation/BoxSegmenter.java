package com.example.miminor.segmentation;

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
import java.util.List;

/**
 * Box-based segmentation with Phase 1 optimizations:
 * - Processing at 0.75x resolution for better speed/quality balance
 * - Optimized color quantization in HSV space
 * - Phase 2 will migrate to OKLAB for 8x better perceptual accuracy
 */
public class BoxSegmenter extends BaseSegmenter {

    @Override
    protected String getAlgorithmName() {
        return "BOX_MODE_OPTIMIZED";
    }

    @Override
    protected boolean usesContours() {
        return false;
    }

    @Override
    protected int getProcessingSize() {
        return 400;
    }

    // ---------- geometry helpers ----------

    private boolean isRectInside(org.opencv.core.Rect inner, org.opencv.core.Rect outer, double tol) {
        return inner.x >= outer.x + tol &&
                inner.y >= outer.y + tol &&
                inner.x + inner.width <= outer.x + outer.width - tol &&
                inner.y + inner.height <= outer.y + outer.height - tol;
    }

    private double rectIoU(org.opencv.core.Rect r1, org.opencv.core.Rect r2) {
        int x1 = Math.max(r1.x, r2.x);
        int y1 = Math.max(r1.y, r2.y);
        int x2 = Math.min(r1.x + r1.width, r2.x + r2.width);
        int y2 = Math.min(r1.y + r1.height, r2.y + r2.height);

        int interW = Math.max(0, x2 - x1);
        int interH = Math.max(0, y2 - y1);
        double interArea = interW * interH;

        double area1 = r1.width * r1.height;
        double area2 = r2.width * r2.height;
        double unionArea = area1 + area2 - interArea;
        if (unionArea <= 0) return 0.0;
        return interArea / unionArea;
    }

    private boolean isRectAlmostSame(org.opencv.core.Rect r1, org.opencv.core.Rect r2, double iouThresh) {
        double iou = rectIoU(r1, r2);
        return iou > iouThresh;
    }

    private void addRegionWithHierarchyFilter(List<ColorRegion> colorRegions, ColorRegion newRegion) {
        // 1. если новый почти совпадает с существующим — игнорируем
        for (ColorRegion existing : colorRegions) {
            if (isRectAlmostSame(newRegion.bounds, existing.bounds, 0.9)) {
                return;
            }
        }

        // 2. если существующий полностью внутри нового и намного меньше -- убираем существующий
        colorRegions.removeIf(existing ->
                isRectInside(existing.bounds, newRegion.bounds, 1.0) &&
                        existing.area < newRegion.area * 0.7
        );

        colorRegions.add(newRegion);
    }

    // ---------- main segmentation ----------

    @Override
    protected RegionData extractRegionByColor(Mat img, int targetColor, int x, int y, int sensitivity) {
        return BoxSegmenterColor.extractByColor(img, targetColor, x, y, sensitivity);
    }

    @Override
    protected List<RegionData> extractRegions(Mat img) {
        Mat segmented = new Mat();
        Imgproc.pyrMeanShiftFiltering(img, segmented, 8, 16);
        Imgproc.GaussianBlur(segmented, segmented, new Size(3, 3), 0);


        Mat edges = new Mat();
        Mat gray = new Mat();
        Imgproc.cvtColor(segmented, gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(gray, edges, 40, 120);
        Mat dilatedEdges = new Mat();
        Mat edgeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
        Imgproc.dilate(edges, dilatedEdges, edgeKernel);
        edgeKernel.release();
        edges.release();
        gray.release();

        Mat hsv = new Mat();
        Imgproc.cvtColor(segmented, hsv, Imgproc.COLOR_RGB2HSV);

        int imageArea = img.rows() * img.cols();
        int minArea = (int) (imageArea * 0.0015); // чуть мягче
        int maxRegions = 35;

        List<Mat> hsvChannels = new ArrayList<>();
        Core.split(hsv, hsvChannels);
        Mat hChannel = hsvChannels.get(0);
        Mat sChannel = hsvChannels.get(1);
        Mat vChannel = hsvChannels.get(2);

        Scalar vMean = Core.mean(vChannel);
        Scalar sMean = Core.mean(sChannel);

        int vThresh = Math.max(20, Math.min((int) (vMean.val[0] * 0.32), 55));
        int sThresh = Math.max(20, Math.min((int) (sMean.val[0] * 0.42), 65));

        List<ColorRegion> colorRegions = new ArrayList<>();

        // dark
        Mat darkMask = new Mat();
        Core.compare(vChannel, new Scalar(vThresh), darkMask, Core.CMP_LT);
        if (Core.countNonZero(darkMask) > minArea) {
            processColorRegion(darkMask, segmented, dilatedEdges, colorRegions, minArea);
        }
        darkMask.release();

        // gray
        Mat grayMask = new Mat();
        Mat highV = new Mat();
        Core.compare(vChannel, new Scalar(vThresh), highV, Core.CMP_GE);
        Core.compare(sChannel, new Scalar(sThresh), grayMask, Core.CMP_LT);
        Core.bitwise_and(highV, grayMask, grayMask);
        highV.release();

        if (Core.countNonZero(grayMask) > minArea) {
            processColorRegion(grayMask, segmented, dilatedEdges, colorRegions, minArea);
        }
        grayMask.release();

        // colored
        Mat coloredMask = new Mat();
        Mat highS = new Mat();
        Mat highV2 = new Mat();
        Core.compare(sChannel, new Scalar(sThresh), highS, Core.CMP_GE);
        Core.compare(vChannel, new Scalar(vThresh), highV2, Core.CMP_GE);
        Core.bitwise_and(highS, highV2, coloredMask);
        highS.release();
        highV2.release();

        int hueRanges = 24;
        int hueStep = 180 / hueRanges;

        for (int i = 0; i < hueRanges; i++) {
            Mat hueMask = new Mat();
            int hMin = i * hueStep;
            int hMax = (i + 1) * hueStep;

            Core.inRange(hChannel, new Scalar(hMin), new Scalar(hMax), hueMask);
            Core.bitwise_and(hueMask, coloredMask, hueMask);

            if (Core.countNonZero(hueMask) > minArea) {
                processColorRegion(hueMask, segmented, dilatedEdges, colorRegions, minArea);
            }
            hueMask.release();
        }

        coloredMask.release();
        hChannel.release();
        sChannel.release();
        vChannel.release();
        hsv.release();
        dilatedEdges.release();
        segmented.release();

        mergeNearbyRegions(colorRegions);

        List<RegionData> regions = new ArrayList<>();
        for (ColorRegion cr : colorRegions) {
            RegionData rd = new RegionData(cr.bounds, cr.area);
            rd.color = cr.color;
            regions.add(rd);
        }

        Collections.sort(regions, new Comparator<RegionData>() {
            @Override
            public int compare(RegionData r1, RegionData r2) {
                return Integer.compare(r2.area, r1.area);
            }
        });

        if (regions.size() > maxRegions) {
            regions = regions.subList(0, maxRegions);
        }

        return regions;
    }

    /**
     * Process a single color mask and extract regions with watershed separation
     */
    private void processColorRegion(Mat mask, Mat img, Mat edges, List<ColorRegion> colorRegions, int minArea) {
        Mat cleanMask = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.morphologyEx(mask, cleanMask, Imgproc.MORPH_OPEN, kernel);
        Imgproc.morphologyEx(cleanMask, cleanMask, Imgproc.MORPH_CLOSE, kernel);

        Mat edgeMask = new Mat();
        Core.bitwise_not(edges, edgeMask);
        Core.bitwise_and(cleanMask, edgeMask, cleanMask);
        edgeMask.release();

        Mat dist = new Mat();
        Imgproc.distanceTransform(cleanMask, dist, Imgproc.DIST_L2, 3);
        Core.normalize(dist, dist, 0, 1.0, Core.NORM_MINMAX);

        Mat peaks = new Mat();
        Imgproc.threshold(dist, peaks, 0.4, 1.0, Imgproc.THRESH_BINARY);
        peaks.convertTo(peaks, CvType.CV_8U, 255);

        Mat largePeaks = new Mat();
        Mat kernelLarge = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        Imgproc.dilate(peaks, largePeaks, kernelLarge);
        kernel.release();
        kernelLarge.release();

        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = Imgproc.connectedComponentsWithStats(largePeaks, labels, stats, centroids);

        if (numLabels > 1 && numLabels <= 60) {
            Mat markers = new Mat();
            labels.convertTo(markers, CvType.CV_32S);

            Mat imgCopy = new Mat();
            img.copyTo(imgCopy);
            Imgproc.watershed(imgCopy, markers);
            imgCopy.release();

            for (int label = 1; label < numLabels; label++) {
                Mat labelMask = new Mat();
                Core.compare(markers, new Scalar(label), labelMask, Core.CMP_EQ);

                int nonZero = Core.countNonZero(labelMask);
                if (nonZero >= minArea) {
                    List<MatOfPoint> contours = new ArrayList<>();
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(labelMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                    if (!contours.isEmpty()) {
                        MatOfPoint largestContour = contours.get(0);
                        for (MatOfPoint c : contours) {
                            if (Imgproc.contourArea(c) > Imgproc.contourArea(largestContour)) {
                                largestContour = c;
                            }
                        }

                        double area = Imgproc.contourArea(largestContour);
                        if (area >= minArea) {
                            org.opencv.core.Rect rect = Imgproc.boundingRect(largestContour);
                            float aspectRatio = (float) rect.width / rect.height;

                            if (aspectRatio > 0.15 && aspectRatio < 7) {
                                Mat roi = img.submat(rect);
                                Scalar meanColor = Core.mean(roi);
                                roi.release();

                                ColorRegion region = new ColorRegion();
                                region.bounds = rect;
                                region.area = (int) area;
                                region.color = new int[]{
                                        (int) meanColor.val[0],
                                        (int) meanColor.val[1],
                                        (int) meanColor.val[2]
                                };
                                addRegionWithHierarchyFilter(colorRegions, region);
                            }
                        }
                    }
                    hierarchy.release();
                }
                labelMask.release();
            }
            markers.release();
        } else {
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(cleanMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area >= minArea) {
                    org.opencv.core.Rect rect = Imgproc.boundingRect(contour);
                    float aspectRatio = (float) rect.width / rect.height;

                    if (aspectRatio > 0.15 && aspectRatio < 7) {
                        Mat roi = img.submat(rect);
                        Scalar meanColor = Core.mean(roi);
                        roi.release();

                        ColorRegion region = new ColorRegion();
                        region.bounds = rect;
                        region.area = (int) area;
                        region.color = new int[]{
                                (int) meanColor.val[0],
                                (int) meanColor.val[1],
                                (int) meanColor.val[2]
                        };
                        addRegionWithHierarchyFilter(colorRegions, region);
                    }
                }
            }
            hierarchy.release();
        }

        labels.release();
        stats.release();
        centroids.release();
        peaks.release();
        largePeaks.release();
        dist.release();
        cleanMask.release();
    }

    /**
     * Merge regions with similar colors that are nearby
     */
    private void mergeNearbyRegions(List<ColorRegion> regions) {
        boolean merged = true;
        int iterations = 0;
        int maxIterations = 3;

        while (merged && iterations < maxIterations) {
            merged = false;
            iterations++;

            for (int i = 0; i < regions.size() && !merged; i++) {
                for (int j = i + 1; j < regions.size() && !merged; j++) {
                    ColorRegion r1 = regions.get(i);
                    ColorRegion r2 = regions.get(j);

                    double colorDist = colorDistance(r1.color, r2.color);
                    double spatialDist = regionDistance(r1.bounds, r2.bounds);
                    double iou = rectIoU(r1.bounds, r2.bounds);

                    boolean shouldMerge = false;

                    if (iou > 0.25 && colorDist < 45 && !(r1.area/r2.area > 3f || r1.area/r2.area < 0.33f)) {
                        shouldMerge = true;
                    }
//                    else if (colorDist < 25 && spatialDist < 10) {
//                        shouldMerge = true;
//                    }

                    if (shouldMerge) {
                        int totalArea = r1.area + r2.area;
                        r1.bounds = unionRect(r1.bounds, r2.bounds);
                        for (int k = 0; k < 3; k++) {
                            r1.color[k] = (r1.color[k] * r1.area + r2.color[k] * r2.area) / totalArea;
                        }
                        r1.area = totalArea;
                        regions.remove(j);
                        merged = true;
                    }
                }
            }
        }

        // удаляем узкие или слишком маленькие боксы
        regions.removeIf(r -> {
            int w = r.bounds.width;
            int h = r.bounds.height;
            if (w <= 0 || h <= 0) return true;
            float aspectRatio = (float) Math.max(w, h) / Math.min(w, h);
            return aspectRatio > 7.0f || r.area < 200;
        });

        finalNMSFilter(regions, 0.5);
    }

    /**
     * Финальная не максимальная подавляющая фильтрация (аналог NMS как в детекторах объектов)
     */
    private void finalNMSFilter(List<ColorRegion> regions, double iouThreshold) {
        Collections.sort(regions, (a, b) -> Integer.compare(b.area, a.area));
        boolean[] removed = new boolean[regions.size()];

        for (int i = 0; i < regions.size(); i++) {
            if (removed[i]) continue;
            ColorRegion r1 = regions.get(i);

            for (int j = i + 1; j < regions.size(); j++) {
                if (removed[j]) continue;
                ColorRegion r2 = regions.get(j);
                double iou = rectIoU(r1.bounds, r2.bounds);
                if (iou > iouThreshold) {
                    removed[j] = true;
                }
            }
        }

        List<ColorRegion> filtered = new ArrayList<>();
        for (int i = 0; i < regions.size(); i++) {
            if (!removed[i]) filtered.add(regions.get(i));
        }
        regions.clear();
        regions.addAll(filtered);
    }


    /**
     * Calculate Euclidean distance between two colors
     */
    private double colorDistance(int[] c1, int[] c2) {
        int dr = c1[0] - c2[0];
        int dg = c1[1] - c2[1];
        int db = c1[2] - c2[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /**
     * Calculate minimum distance between two rectangles
     */
    private double regionDistance(org.opencv.core.Rect r1, org.opencv.core.Rect r2) {
        int dx = Math.max(0, Math.max(r1.x - (r2.x + r2.width), r2.x - (r1.x + r1.width)));
        int dy = Math.max(0, Math.max(r1.y - (r2.y + r2.height), r2.y - (r1.y + r1.height)));
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Union of two rectangles
     */
    private org.opencv.core.Rect unionRect(org.opencv.core.Rect r1, org.opencv.core.Rect r2) {
        int x = Math.min(r1.x, r2.x);
        int y = Math.min(r1.y, r2.y);
        int right = Math.max(r1.x + r1.width, r2.x + r2.width);
        int bottom = Math.max(r1.y + r1.height, r2.y + r2.height);
        return new org.opencv.core.Rect(x, y, right - x, bottom - y);
    }

    /**
     * Helper class for color region data
     */
    private static class ColorRegion {
        org.opencv.core.Rect bounds;
        int area;
        int[] color;
    }
}
