package com.example.miminor;

import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.miminor.utils.PreferencesHelper;
import com.google.android.material.appbar.MaterialToolbar;

/**
 * Settings screen
 */
public class SettingsActivity extends AppCompatActivity {
    private PreferencesHelper prefs;
    private RadioGroup modeRadioGroup;
    private SeekBar sensitivitySeekBar;
    private TextView sensitivityValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = new PreferencesHelper(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Настройки");

        modeRadioGroup = findViewById(R.id.modeRadioGroup);
        sensitivitySeekBar = findViewById(R.id.sensitivitySeekBar);
        sensitivityValue = findViewById(R.id.sensitivityValue);

        PreferencesHelper.SegmentationMode currentMode = prefs.getSegmentationMode();
        if (currentMode == PreferencesHelper.SegmentationMode.STREAMING) {
            modeRadioGroup.check(R.id.radioStreaming);
        } else {
            modeRadioGroup.check(R.id.radioPrecision);
        }

        int sensitivity = prefs.getSensitivity();
        sensitivitySeekBar.setProgress(sensitivity);
        sensitivityValue.setText(String.valueOf(sensitivity));

        modeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioStreaming) {
                prefs.setSegmentationMode(PreferencesHelper.SegmentationMode.STREAMING);
            } else {
                prefs.setSegmentationMode(PreferencesHelper.SegmentationMode.PRECISION);
            }
        });

        sensitivitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sensitivityValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.setSensitivity(seekBar.getProgress());
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
