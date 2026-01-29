package com.example.miminor.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.example.miminor.segmentation.ImageSegment;
import com.example.miminor.segmentation.SegmentationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom View для отображения интерактивной разметки сегментов поверх изображения
 */
public class SegmentOverlayView extends View {
    private static final String TAG = "SegmentOverlayView";

    private SegmentationResult segmentationResult;
    private ImageSegment selectedSegment;
    private List<ImageSegment> highlightedSegments = new ArrayList<>();

    private Paint overlayPaint;
    private Paint borderPaint;
    private Paint selectedPaint;

    private float imageScaleX = 1.0f;
    private float imageScaleY = 1.0f;
    private float imageOffsetX = 0;
    private float imageOffsetY = 0;

    private ValueAnimator pulseAnimator;
    private float pulseAlpha = 0.3f;

    private OnSegmentClickListener segmentClickListener;
    private OnTouchListener touchListener;

    public interface OnSegmentClickListener {
        void onSegmentClick(ImageSegment segment, float x, float y);
    }

    public interface OnTouchListener {
        void onTouch(int imageX, int imageY);
    }

    public SegmentOverlayView(Context context) {
        super(context);
        init();
    }

    public SegmentOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SegmentOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        overlayPaint = new Paint();
        overlayPaint.setStyle(Paint.Style.FILL);
        overlayPaint.setAntiAlias(true);

        borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setAntiAlias(true);

        selectedPaint = new Paint();
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(3);
        selectedPaint.setColor(Color.YELLOW);
        selectedPaint.setAntiAlias(true);

        setupPulseAnimation();
    }

    private void setupPulseAnimation() {
        pulseAnimator = ValueAnimator.ofFloat(0.6f, 0.85f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setInterpolator(new DecelerateInterpolator());
        pulseAnimator.addUpdateListener(animation -> {
            pulseAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
    }

    public void setSegmentationResult(SegmentationResult result) {
        this.segmentationResult = result;
        this.selectedSegment = null;
        this.highlightedSegments.clear();
        invalidate();
    }
    
    public void addSegment(ImageSegment segment) {
        this.highlightedSegments.clear();
        this.highlightedSegments.add(segment);
        this.selectedSegment = segment;
        if (!pulseAnimator.isRunning()) {
            pulseAnimator.start();
        }
        invalidate();
    }
    
    public void clearSegments() {
        this.highlightedSegments.clear();
        this.selectedSegment = null;
        this.segmentationResult = null;
        if (pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        invalidate();
    }

    public void setImageTransform(float scaleX, float scaleY, float offsetX, float offsetY) {
        this.imageScaleX = scaleX;
        this.imageScaleY = scaleY;
        this.imageOffsetX = offsetX;
        this.imageOffsetY = offsetY;
        Log.d(TAG, String.format("Overlay transform set: scale=(%.3f, %.3f), offset=(%.1f, %.1f)", 
                                scaleX, scaleY, offsetX, offsetY));
        invalidate();
    }

    public void setOnSegmentClickListener(OnSegmentClickListener listener) {
        this.segmentClickListener = listener;
    }

    public void setOnTouchListener(OnTouchListener listener) {
        this.touchListener = listener;
    }

    public void highlightSegments(List<ImageSegment> segments) {
        this.highlightedSegments = new ArrayList<>(segments);
        invalidate();
    }

    public void clearHighlight() {
        this.highlightedSegments.clear();
        this.selectedSegment = null;
        if (pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (highlightedSegments.isEmpty() && (segmentationResult == null || !segmentationResult.isSuccess())) {
            return;
        }

        if (segmentationResult != null && segmentationResult.isSuccess()) {
            for (ImageSegment segment : segmentationResult.getSegments()) {
                drawSegment(canvas, segment, false);
            }
        }

        for (ImageSegment segment : highlightedSegments) {
            drawSegment(canvas, segment, true);
        }

        if (selectedSegment != null) {
            drawSelectedSegment(canvas, selectedSegment);
        }
    }

    private void drawSegment(Canvas canvas, ImageSegment segment, boolean highlighted) {
        List<Point> contour = segment.getContour();
        Rect bounds = segment.getBounds();
        int color = segment.getDominantColor().getColor();
        
        if (contour.isEmpty()) {
            Rect scaledBounds = scaleRect(bounds);
            overlayPaint.setColor(color);
            overlayPaint.setAlpha(25);
            canvas.drawRect(scaledBounds, overlayPaint);

            borderPaint.setColor(color);
            borderPaint.setStrokeWidth(highlighted ? 3 : 2);
            borderPaint.setAlpha(255);
            canvas.drawRect(scaledBounds, borderPaint);
            return;
        }

        Path path = createContourPath(contour, bounds);

        int alpha = 25;
        overlayPaint.setColor(color);
        overlayPaint.setAlpha(alpha);
        canvas.drawPath(path, overlayPaint);

        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int brightColor = Color.rgb(
            Math.min(255, r + 80),
            Math.min(255, g + 80),
            Math.min(255, b + 80)
        );

        borderPaint.setColor(brightColor);
        borderPaint.setStrokeWidth(highlighted ? 3 : 2);
        borderPaint.setAlpha(255);
        canvas.drawPath(path, borderPaint);
    }

    private void drawSelectedSegment(Canvas canvas, ImageSegment segment) {
        List<Point> contour = segment.getContour();
        Rect bounds = segment.getBounds();
        int color = segment.getDominantColor().getColor();

        Path path;
        if (contour.isEmpty()) {
            path = new Path();
            Rect scaledBounds = scaleRect(bounds);
            path.addRect(scaledBounds.left, scaledBounds.top, scaledBounds.right, scaledBounds.bottom, Path.Direction.CW);
        } else {
            path = createContourPath(contour, bounds);
        }

        overlayPaint.setColor(color);
        overlayPaint.setAlpha((int)(pulseAlpha * 250));
        canvas.drawPath(path, overlayPaint);

        selectedPaint.setColor(Color.YELLOW);
        selectedPaint.setStrokeWidth(4);
        selectedPaint.setAlpha((int)(pulseAlpha * 255));
        canvas.drawPath(path, selectedPaint);

        selectedPaint.setColor(Color.WHITE);
        selectedPaint.setStrokeWidth(5);
        selectedPaint.setAlpha((int)(pulseAlpha * 150));
        canvas.drawPath(path, selectedPaint);
    }

    private Path createContourPath(List<Point> contour, Rect bounds) {
        Path path = new Path();

        if (contour.isEmpty()) {
            return path;
        }

        Point first = contour.get(0);
        float startX = first.x * imageScaleX + imageOffsetX;
        float startY = first.y * imageScaleY + imageOffsetY;

        path.moveTo(startX, startY);

        for (int i = 1; i < contour.size(); i++) {
            Point p = contour.get(i);
            float x = p.x * imageScaleX + imageOffsetX;
            float y = p.y * imageScaleY + imageOffsetY;
            path.lineTo(x, y);
        }

        path.close();
        return path;
    }

    private Rect scaleRect(Rect original) {
        return new Rect(
            (int) (original.left * imageScaleX + imageOffsetX),
            (int) (original.top * imageScaleY + imageOffsetY),
            (int) (original.right * imageScaleX + imageOffsetX),
            (int) (original.bottom * imageScaleY + imageOffsetY)
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            handleTouch(event.getX(), event.getY());
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void handleTouch(float x, float y) {
        int imageX = (int) ((x - imageOffsetX) / imageScaleX);
        int imageY = (int) ((y - imageOffsetY) / imageScaleY);

        Log.d(TAG, String.format("Touch: screen=(%.1f,%.1f) -> image=(%d,%d)", x, y, imageX, imageY));

        if (segmentationResult != null && segmentationResult.isSuccess()) {
            ImageSegment touchedSegment = segmentationResult.findSegmentAt(imageX, imageY);

            if (touchedSegment != null) {
                Log.d(TAG, "Found segment: " + touchedSegment);
                selectedSegment = touchedSegment;

                if (!pulseAnimator.isRunning()) {
                    pulseAnimator.start();
                }

                invalidate();

                if (segmentClickListener != null) {
                    segmentClickListener.onSegmentClick(touchedSegment, x, y);
                }
                return;
            }
        }

        if (touchListener != null) {
            touchListener.onTouch(imageX, imageY);
        }
    }

    public ImageSegment getSelectedSegment() {
        return selectedSegment;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (pulseAnimator != null && pulseAnimator.isRunning()) {
            pulseAnimator.cancel();
        }
    }
}
