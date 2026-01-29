package com.example.miminor;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.miminor.dialogs.ColorInfoDialog;
import com.example.miminor.segmentation.BaseSegmenter;
import com.example.miminor.segmentation.SlicSegmenter;
import com.example.miminor.segmentation.ContourSegmenter;
import com.example.miminor.segmentation.ImageSegment;
import com.example.miminor.segmentation.SegmentationResult;
import com.example.miminor.utils.PreferencesHelper;
import com.example.miminor.views.SegmentOverlayView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ImageView imageView;
    private SegmentOverlayView overlayView;
    private MaterialButton btnTakePhoto;
    private MaterialButton btnFromGallery;
    private TextView statusText;
    private TextView hintText;
    private ProgressBar progressBar;
    private FloatingActionButton fabSettings;

    private BaseSegmenter segmenter;
    private ExecutorService executorService;

    private Bitmap currentBitmap;
    private Bitmap displayedBitmap;
    private Uri photoUri;
    private SegmentationResult segmentationResult;

    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
            Toast.makeText(this, "OpenCV initialization failed", Toast.LENGTH_LONG).show();
        } else {
            Log.d(TAG, "OpenCV initialized successfully");
        }
        
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeServices();
        setupActivityResultLaunchers();
        setupListeners();
        checkPermissions();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.imageView);
        overlayView = findViewById(R.id.overlayView);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnFromGallery = findViewById(R.id.btnFromGallery);
        statusText = findViewById(R.id.statusText);
        hintText = findViewById(R.id.hintText);
        progressBar = findViewById(R.id.progressBar);
        fabSettings = findViewById(R.id.fabSettings);
    }

    private void initializeServices() {
        PreferencesHelper prefs = new PreferencesHelper(this);
        segmenter = createSegmenter(prefs.getSegmentationMode());
        executorService = Executors.newSingleThreadExecutor();
    }

    private BaseSegmenter createSegmenter(PreferencesHelper.SegmentationMode mode) {
        if (mode == PreferencesHelper.SegmentationMode.STREAMING) {
            return new SlicSegmenter();
        } else {
            return new ContourSegmenter();
        }
    }

    private void setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    if (selectedImage != null) {
                        loadImageFromUri(selectedImage);
                    }
                }
            }
        );

        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.TakePicture(),
            success -> {
                if (success && photoUri != null) {
                    loadImageFromUri(photoUri);
                } else {
                    Log.e(TAG, "Failed to capture photo");
                }
            }
        );
    }

    private void setupListeners() {
        btnTakePhoto.setOnClickListener(v -> openCamera());
        btnFromGallery.setOnClickListener(v -> openGallery());

        fabSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        overlayView.setOnTouchListener((imageX, imageY) -> {
            if (currentBitmap != null) {
                analyzeColorAtPoint(imageX, imageY);
            }
        });

        overlayView.setOnSegmentClickListener((segment, x, y) -> {
            if (segment != null) {
                showColorInfo(segment);
            }
        });
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Snackbar.make(findViewById(android.R.id.content),
                    R.string.permission_denied, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(android.R.id.content),
                R.string.permission_camera_rationale, Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            File photoFile = createImageFile();
            photoUri = FileProvider.getUriForFile(this,
                getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(photoUri);
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file", e);
            Toast.makeText(this, R.string.error_camera_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "PHOTO_" + System.currentTimeMillis();
        File storageDir = getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void openGallery() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            ? Manifest.permission.READ_MEDIA_IMAGES
            : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(findViewById(android.R.id.content),
                R.string.permission_storage_rationale, Snackbar.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    private void loadImageFromUri(Uri uri) {
        executorService.execute(() -> {
            try {
                Bitmap bitmap;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                    bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
                } else {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                }

                bitmap = ensureMutableBitmap(bitmap);
                final Bitmap finalBitmap = scaleBitmapIfNeeded(bitmap);

                runOnUiThread(() -> {
                    displayImage(finalBitmap);
                    statusText.setText("Нажмите на объект для выделения");
                    statusText.setVisibility(View.VISIBLE);
                    hintText.setVisibility(View.VISIBLE);
                });

            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                runOnUiThread(() ->
                    Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private Bitmap ensureMutableBitmap(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.getConfig() == Bitmap.Config.HARDWARE) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        if (!bitmap.isMutable() || bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
            return bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return bitmap;
    }

    private Bitmap scaleBitmapIfNeeded(Bitmap bitmap) {
        int maxSize = 1440;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }

        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void displayImage(Bitmap bitmap) {
        currentBitmap = bitmap;
        displayedBitmap = null;
        imageView.setImageBitmap(bitmap);
        overlayView.clearSegments();
        segmentationResult = null;

        imageView.post(() -> updateOverlayTransform());
    }

    private void updateOverlayTransform() {
        Bitmap bitmapToUse = displayedBitmap != null ? displayedBitmap : currentBitmap;
        if (bitmapToUse == null) return;

        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();
        int imageWidth = bitmapToUse.getWidth();
        int imageHeight = bitmapToUse.getHeight();

        if (viewWidth == 0 || viewHeight == 0) return;

        float scale = Math.min(
            (float) viewWidth / imageWidth,
            (float) viewHeight / imageHeight
        );

        float scaledWidth = imageWidth * scale;
        float scaledHeight = imageHeight * scale;

        float offsetX = (viewWidth - scaledWidth) / 2f;
        float offsetY = (viewHeight - scaledHeight) / 2f;

        Log.d(TAG, String.format("Transform: scale=%.3f, offset=(%.1f, %.1f), view=(%d, %d), image=(%d, %d)",
                                scale, offsetX, offsetY, viewWidth, viewHeight, imageWidth, imageHeight));

        overlayView.setImageTransform(scale, scale, offsetX, offsetY);
    }

    private void analyzeColorAtPoint(int x, int y) {
        if (currentBitmap == null || x < 0 || y < 0 || 
            x >= currentBitmap.getWidth() || y >= currentBitmap.getHeight()) {
            return;
        }

        showProgress(true);
        
        PreferencesHelper prefs = new PreferencesHelper(this);
        int sensitivity = prefs.getSensitivity();

        executorService.execute(() -> {
            Log.d(TAG, "Analyzing color at: " + x + ", " + y);
            long start = System.currentTimeMillis();
            
            int targetColor = currentBitmap.getPixel(x, y);
            ImageSegment segment = segmenter.segmentByColor(currentBitmap, targetColor, x, y, sensitivity);
            long time = System.currentTimeMillis() - start;

            runOnUiThread(() -> {
                showProgress(false);

                if (segment != null) {
                    overlayView.clearSegments();
                    overlayView.addSegment(segment);
                    imageView.post(() -> updateOverlayTransform());

                    statusText.setText(String.format("Объект выделен (%.2f сек)", time / 1000.0));
                    statusText.setVisibility(View.VISIBLE);
                    
                    showColorInfo(segment);
                } else {
                    Toast.makeText(this, "Не удалось выделить объект", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }


    private void showColorInfo(ImageSegment segment) {
        if (segment == null || segment.getDominantColor() == null) {
            return;
        }

        Log.d(TAG, "Showing color info for segment: " + segment.getId());
        ColorInfoDialog dialog = new ColorInfoDialog(this, segment.getDominantColor());
        dialog.show();
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        statusText.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) {
            statusText.setText(R.string.analyzing);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferencesHelper prefs = new PreferencesHelper(this);
        BaseSegmenter newSegmenter = createSegmenter(prefs.getSegmentationMode());
        if (segmenter == null || !segmenter.getClass().equals(newSegmenter.getClass())) {
            segmenter = newSegmenter;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            currentBitmap.recycle();
            currentBitmap = null;
        }
        if (displayedBitmap != null && !displayedBitmap.isRecycled()) {
            displayedBitmap.recycle();
            displayedBitmap = null;
        }
    }
}
