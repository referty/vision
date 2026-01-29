package com.example.miminor.segmentation;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Сегментация по цвету для режима контуров
 */
public class ContourSegmenterColor {

    public static BaseSegmenter.RegionData extractByColor(Mat img, int targetColor, int clickX, int clickY, int sensitivity) {
        if (clickX < 0 || clickY < 0 || clickX >= img.cols() || clickY >= img.rows()) {
            return null;
        }

        Mat segmented = new Mat();
        Imgproc.pyrMeanShiftFiltering(img, segmented, 15, 30, 0);

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
        int satRange = (int) (80 * factor);
        int valRange = (int) (80 * factor);

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

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        BaseSegmenter.RegionData result = null;

        for (MatOfPoint contour : contours) {
            org.opencv.core.Rect rect = Imgproc.boundingRect(contour);
            if (clickX >= rect.x && clickX < rect.x + rect.width &&
                clickY >= rect.y && clickY < rect.y + rect.height) {
                
                double area = Imgproc.contourArea(contour);
                Mat roi = segmented.submat(rect);
                Scalar meanColor = Core.mean(roi);
                roi.release();

                List<org.opencv.core.Point> contourPoints = new ArrayList<>();
                org.opencv.core.Point[] pts = contour.toArray();
                for (org.opencv.core.Point p : pts) {
                    contourPoints.add(p);
                }

                result = new BaseSegmenter.RegionData(rect, (int) area, contourPoints);
                result.color = new int[]{
                    (int) meanColor.val[0],
                    (int) meanColor.val[1],
                    (int) meanColor.val[2]
                };
                break;
            }
        }

        hierarchy.release();
        kernel.release();
        mask.release();
        hsv.release();
        segmented.release();

        return result;
    }
}
