package com.example.miminor.segmentation;

import android.graphics.Color;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Сегментация по цвету для режима боксов
 */
public class BoxSegmenterColor {

    public static BaseSegmenter.RegionData extractByColor(Mat img, int targetColor, int clickX, int clickY, int sensitivity) {
        if (clickX < 0 || clickY < 0 || clickX >= img.cols() || clickY >= img.rows()) {
            return null;
        }

        Mat segmented = new Mat();
        Imgproc.pyrMeanShiftFiltering(img, segmented, 8, 16, 0);
        Imgproc.GaussianBlur(segmented, segmented, new Size(3, 3), 0);

        Mat hsv = new Mat();
        Imgproc.cvtColor(segmented, hsv, Imgproc.COLOR_RGB2HSV);

        double[] hsvClick = hsv.get(clickY, clickX);
        if (hsvClick == null) {
            hsv.release();
            segmented.release();
            return null;
        }

        int hueTarget = (int) hsvClick[0];
        int satTarget = (int) hsvClick[1];
        int valTarget = (int) hsvClick[2];

        float factor = sensitivity / 50.0f;
        int hueRange = (int) (20 * factor);
        int satRange = (int) (70 * factor);
        int valRange = (int) (70 * factor);

        int hMin = Math.max(0, hueTarget - hueRange);
        int hMax = Math.min(180, hueTarget + hueRange);
        int sMin = Math.max(0, satTarget - satRange);
        int sMax = Math.min(255, satTarget + satRange);
        int vMin = Math.max(0, valTarget - valRange);
        int vMax = Math.min(255, valTarget + valRange);

        Mat mask = new Mat();
        Core.inRange(hsv, new Scalar(hMin, sMin, vMin), new Scalar(hMax, sMax, vMax), mask);

        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);
        Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);

        Mat dist = new Mat();
        Imgproc.distanceTransform(mask, dist, Imgproc.DIST_L2, 3);
        Core.normalize(dist, dist, 0, 1.0, Core.NORM_MINMAX);

        Mat peaks = new Mat();
        Imgproc.threshold(dist, peaks, 0.3, 1.0, Imgproc.THRESH_BINARY);
        peaks.convertTo(peaks, CvType.CV_8U, 255);

        Mat labels = new Mat();
        Mat stats = new Mat();
        Mat centroids = new Mat();
        int numLabels = Imgproc.connectedComponentsWithStats(peaks, labels, stats, centroids);

        BaseSegmenter.RegionData result = null;

        if (numLabels > 1) {
            Mat markers = new Mat();
            labels.convertTo(markers, CvType.CV_32S);

            Mat imgCopy = new Mat();
            segmented.copyTo(imgCopy);
            Imgproc.watershed(imgCopy, markers);
            imgCopy.release();

            int clickLabel = (int) markers.get(clickY, clickX)[0];

            if (clickLabel > 0) {
                Mat regionMask = new Mat();
                Core.compare(markers, new Scalar(clickLabel), regionMask, Core.CMP_EQ);

                List<MatOfPoint> contours = new ArrayList<>();
                Mat hierarchy = new Mat();
                Imgproc.findContours(regionMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

                if (!contours.isEmpty()) {
                    MatOfPoint largestContour = contours.get(0);
                    for (MatOfPoint c : contours) {
                        if (Imgproc.contourArea(c) > Imgproc.contourArea(largestContour)) {
                            largestContour = c;
                        }
                    }

                    double area = Imgproc.contourArea(largestContour);
                    org.opencv.core.Rect rect = Imgproc.boundingRect(largestContour);

                    Mat roi = segmented.submat(rect);
                    Scalar meanColor = Core.mean(roi);
                    roi.release();

                    result = new BaseSegmenter.RegionData(rect, (int) area);
                    result.color = new int[]{
                        (int) meanColor.val[0],
                        (int) meanColor.val[1],
                        (int) meanColor.val[2]
                    };
                }

                hierarchy.release();
                regionMask.release();
            }

            markers.release();
        } else {
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : contours) {
                org.opencv.core.Rect rect = Imgproc.boundingRect(contour);
                if (clickX >= rect.x && clickX < rect.x + rect.width &&
                    clickY >= rect.y && clickY < rect.y + rect.height) {
                    
                    double area = Imgproc.contourArea(contour);
                    Mat roi = segmented.submat(rect);
                    Scalar meanColor = Core.mean(roi);
                    roi.release();

                    result = new BaseSegmenter.RegionData(rect, (int) area);
                    result.color = new int[]{
                        (int) meanColor.val[0],
                        (int) meanColor.val[1],
                        (int) meanColor.val[2]
                    };
                    break;
                }
            }

            hierarchy.release();
        }

        labels.release();
        stats.release();
        centroids.release();
        peaks.release();
        dist.release();
        kernel.release();
        mask.release();
        hsv.release();
        segmented.release();

        return result;
    }
}
