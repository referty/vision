package com.example.miminor.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.miminor.R;
import com.example.miminor.utils.ColorInfo;
import com.example.miminor.utils.ColorNameMapper;
import com.example.miminor.utils.ColorConverter;
import com.example.miminor.utils.OklabColor;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Dialog displaying detailed color information with OKLAB-based analysis.
 * Shows perceptually accurate color metrics and WCAG contrast ratings.
 */
public class ColorInfoDialog extends Dialog {
    private static final String TAG = "ColorInfoDialog";

    private ColorInfo colorInfo;
    private TextToSpeech tts;
    private boolean ttsInitialized = false;

    public ColorInfoDialog(@NonNull Context context, ColorInfo colorInfo) {
        super(context);
        this.colorInfo = colorInfo;
        initTTS(context);
    }

    private void initTTS(Context context) {
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("ru"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Russian language not supported, using default");
                    tts.setLanguage(Locale.getDefault());
                }
                ttsInitialized = true;
                Log.d(TAG, "TTS initialized successfully");
            } else {
                Log.e(TAG, "TTS initialization failed");
                ttsInitialized = false;
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_color_info);

        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        setupViews();
    }

    private void setupViews() {
        View colorPreview = findViewById(R.id.colorPreview);
        colorPreview.setBackgroundColor(colorInfo.getColor());

        TextView colorName = findViewById(R.id.colorName);
        colorName.setText(colorInfo.getColorName());

        TextView hexCode = findViewById(R.id.hexCode);
        hexCode.setText(colorInfo.getHexCode());

        TextView rgbCode = findViewById(R.id.rgbCode);
        rgbCode.setText(colorInfo.getRgbString());

        TextView contrastRating = findViewById(R.id.contrastRating);
        contrastRating.setText(String.format("%s (%.2f:1)",
            colorInfo.getContrastRating(),
            colorInfo.getContrast()));

        MaterialButton btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(v -> speakColor());

        MaterialButton btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(v -> dismiss());
    }

    private void speakColor() {
        if (!ttsInitialized) {
            Toast.makeText(getContext(), R.string.error_tts_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }

        String description = ColorNameMapper.getColorDescription(colorInfo.getColor());
        String text = String.format("Цвет: %s. Код: %s", description, colorInfo.getHexCode());

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "colorInfo");
        Log.d(TAG, "Speaking: " + text);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tts != null && ttsInitialized) {
            tts.stop();
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (tts != null) {
            tts.shutdown();
        }
    }
}
