package com.example.miminor.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper for managing app preferences
 */
public class PreferencesHelper {
    private static final String PREFS_NAME = "MiMinorPrefs";
    private static final String KEY_SEGMENTATION_MODE = "segmentation_mode";
    private static final String KEY_SENSITIVITY = "sensitivity";

    public enum SegmentationMode {
        STREAMING,
        PRECISION
    }

    private final SharedPreferences prefs;

    public PreferencesHelper(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public SegmentationMode getSegmentationMode() {
        String mode = prefs.getString(KEY_SEGMENTATION_MODE, SegmentationMode.STREAMING.name());
        try {
            return SegmentationMode.valueOf(mode);
        } catch (IllegalArgumentException e) {
            if ("BOXES".equals(mode) || "CONTOURS".equals(mode)) {
                setSegmentationMode(SegmentationMode.STREAMING);
                return SegmentationMode.STREAMING;
            }
            return SegmentationMode.STREAMING;
        }
    }

    public void setSegmentationMode(SegmentationMode mode) {
        prefs.edit().putString(KEY_SEGMENTATION_MODE, mode.name()).apply();
    }

    public int getSensitivity() {
        return prefs.getInt(KEY_SENSITIVITY, 50);
    }

    public void setSensitivity(int sensitivity) {
        prefs.edit().putInt(KEY_SENSITIVITY, sensitivity).apply();
    }
}
