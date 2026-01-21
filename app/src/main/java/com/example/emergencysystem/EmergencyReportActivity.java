package com.example.emergencysystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmergencyReportActivity extends AppCompatActivity {

    private static final String TAG = "EmergencyReport";
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int REQUEST_STORAGE_PERMISSION = 2;
    private static final int PICK_IMAGE_REQUEST = 3;
    private static final int PICK_VIDEO_REQUEST = 4;

    private EditText edtDescription;
    private Spinner spinnerEmergencyType;
    private RadioGroup radioGroupSeverity;
    private ImageView imgPreview1, imgPreview2, imgPreview3;
    private TextView txtSelectedFiles;
    private Button btnSubmitReport, btnPanicAlert;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<String> cloudinaryImageUrls = new ArrayList<>();
    private final List<String> cloudinaryVideoUrls = new ArrayList<>();

    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency_report);

        Log.d(TAG, "EmergencyReportActivity created");

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("emergency_reports");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get emergency type from intent (from home page cards)
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("EMERGENCY_TYPE")) {
            String emergencyType = intent.getStringExtra("EMERGENCY_TYPE");
            if (emergencyType != null) {
                // Set emergency type in spinner if possible
                // You'll need to find the position in your spinner
                Log.d(TAG, "Received emergency type: " + emergencyType);
            }
        }

        initializeCloudinary();
        initUI();
        requestLocation();
    }

    private void initUI() {
        try {
            edtDescription = findViewById(R.id.edtDescription);
            spinnerEmergencyType = findViewById(R.id.spinnerEmergencyType);
            radioGroupSeverity = findViewById(R.id.radioGroupSeverity);

            imgPreview1 = findViewById(R.id.imgPreview1);
            imgPreview2 = findViewById(R.id.imgPreview2);
            imgPreview3 = findViewById(R.id.imgPreview3);

            txtSelectedFiles = findViewById(R.id.txtSelectedFiles);

            Button btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
            Button btnUploadVideo = findViewById(R.id.btnUploadVideo);
            btnSubmitReport = findViewById(R.id.btnSubmitReport);
            btnPanicAlert = findViewById(R.id.btnPanicAlert);

            // Check if UI elements were found
            if (btnUploadPhoto == null) Log.e(TAG, "btnUploadPhoto not found");
            if (btnUploadVideo == null) Log.e(TAG, "btnUploadVideo not found");
            if (btnSubmitReport == null) Log.e(TAG, "btnSubmitReport not found");
            if (btnPanicAlert == null) Log.e(TAG, "btnPanicAlert not found");

            // Set default severity to Medium
            if (radioGroupSeverity != null) {
                radioGroupSeverity.check(R.id.radioMedium);
            }

            // Set click listeners with error handling
            if (btnUploadPhoto != null) {
                btnUploadPhoto.setOnClickListener(v -> {
                    try {
                        requestStoragePermissionForImage();
                    } catch (Exception e) {
                        Log.e(TAG, "Upload photo error", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (btnUploadVideo != null) {
                btnUploadVideo.setOnClickListener(v -> {
                    try {
                        requestStoragePermissionForVideo();
                    } catch (Exception e) {
                        Log.e(TAG, "Upload video error", e);
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (btnSubmitReport != null) {
                btnSubmitReport.setOnClickListener(v -> {
                    try {
                        submitReport(false);
                    } catch (Exception e) {
                        Log.e(TAG, "Submit report error", e);
                        Toast.makeText(this, "Error submitting report", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            if (btnPanicAlert != null) {
                btnPanicAlert.setOnClickListener(v -> {
                    try {
                        submitReport(true);
                    } catch (Exception e) {
                        Log.e(TAG, "Panic alert error", e);
                        Toast.makeText(this, "Error sending panic alert", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing UI", e);
            Toast.makeText(this, "Error loading form", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeCloudinary() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", "dknax0k01");
            config.put("api_key", "685787515875988");
            config.put("secure", "true");
            MediaManager.init(this, config);
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Cloudinary initialization failed", e);
            Toast.makeText(this, "Media upload service unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.d(TAG, "Location obtained: " + latitude + ", " + longitude);
            } else {
                Log.w(TAG, "Location is null, using default coordinates");
                // Set default coordinates for DKUT
                latitude = -0.3673;
                longitude = 36.9456;
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location", e);
            // Set default coordinates
            latitude = -0.3673;
            longitude = 36.9456;
        });
    }

    private void requestStoragePermissionForImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openImagePicker();
        }
    }

    private void requestStoragePermissionForVideo() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
        } else {
            openVideoPicker();
        }
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            Toast.makeText(this, "Cannot open gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void openVideoPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            startActivityForResult(intent, PICK_VIDEO_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error opening video picker", e);
            Toast.makeText(this, "Cannot open video gallery", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitReport(boolean isPanicAlert) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = user.getUid();
        String userEmail = user.getEmail();

        String description;
        if (isPanicAlert) {
            description = "🚨 PANIC ALERT - Immediate assistance required!";
        } else {
            description = edtDescription.getText().toString().trim();
            if (description.isEmpty()) {
                Toast.makeText(this, "Please describe the emergency", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String emergencyType;
        if (spinnerEmergencyType != null && spinnerEmergencyType.getSelectedItem() != null) {
            emergencyType = spinnerEmergencyType.getSelectedItem().toString();
        } else {
            emergencyType = "Emergency";
        }

        if (isPanicAlert) {
            emergencyType = "Panic Alert";
        }

        // Disable buttons to prevent multiple submissions
        if (btnSubmitReport != null) btnSubmitReport.setEnabled(false);
        if (btnPanicAlert != null) btnPanicAlert.setEnabled(false);

        Map<String, Object> report = new HashMap<>();
        report.put("id", ""); // Will be set when saving
        report.put("userId", userId);
        report.put("userEmail", userEmail);
        report.put("userName", "DKUT Student"); // You can get this from user profile
        report.put("emergencyType", emergencyType);
        report.put("description", description);
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        report.put("status", "pending");
        report.put("panicAlert", isPanicAlert); // Changed from "panic" to "panicAlert"

        // Set severity
        String severity = "Medium";
        if (radioGroupSeverity != null) {
            int selectedId = radioGroupSeverity.getCheckedRadioButtonId();
            if (selectedId == R.id.radioLow) severity = "Low";
            else if (selectedId == R.id.radioHigh) severity = "High";
            else if (selectedId == R.id.radioCritical) severity = "Critical";
        }
        if (isPanicAlert) severity = "Critical";
        report.put("severity", severity);

        // Initialize empty arrays for media
        report.put("imageUrls", new ArrayList<String>());
        report.put("videoUrls", new ArrayList<String>());

        Log.d(TAG, "Submitting report: " + emergencyType);

        if (!mediaUris.isEmpty()) {
            uploadMedia(report);
        } else {
            saveReport(report);
        }
    }

    private void uploadMedia(Map<String, Object> report) {
        cloudinaryImageUrls.clear();
        cloudinaryVideoUrls.clear();

        int totalMedia = mediaUris.size();
        final int[] uploadedCount = {0};

        for (Uri uri : mediaUris) {
            String mimeType = getContentResolver().getType(uri);
            String resourceType = (mimeType != null && mimeType.startsWith("video")) ? "video" : "image";

            String userId = (String) report.get("userId");
            String folder = "dkut_emergency/" + userId;

            MediaManager.get().upload(uri)
                    .option("resource_type", resourceType)
                    .option("folder", folder)
                    .option("timestamp", System.currentTimeMillis())
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String id, Map data) {
                            String mediaUrl = (String) data.get("secure_url");
                            Log.d(TAG, "Media uploaded successfully: " + mediaUrl);

                            if (resourceType.equals("video")) {
                                cloudinaryVideoUrls.add(mediaUrl);
                            } else {
                                cloudinaryImageUrls.add(mediaUrl);
                            }

                            uploadedCount[0]++;

                            if (uploadedCount[0] == totalMedia) {
                                // Update report with media URLs
                                report.put("imageUrls", cloudinaryImageUrls);
                                report.put("videoUrls", cloudinaryVideoUrls);
                                saveReport(report);
                            }
                        }

                        @Override
                        public void onError(String id, ErrorInfo error) {
                            Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                            Toast.makeText(EmergencyReportActivity.this,
                                    "Media upload failed, saving report without media",
                                    Toast.LENGTH_SHORT).show();
                            uploadedCount[0]++;
                            if (uploadedCount[0] == totalMedia) {
                                saveReport(report);
                            }
                        }

                        @Override
                        public void onStart(String id) {
                            Log.d(TAG, "Upload started for: " + id);
                        }

                        @Override
                        public void onProgress(String id, long bytes, long total) {}

                        @Override
                        public void onReschedule(String id, ErrorInfo error) {}
                    }).dispatch();
        }
    }

    private void saveReport(Map<String, Object> report) {
        String reportId = databaseReference.push().getKey();
        if (reportId == null) {
            Toast.makeText(this, "Error generating report ID", Toast.LENGTH_SHORT).show();
            // Re-enable buttons
            if (btnSubmitReport != null) btnSubmitReport.setEnabled(true);
            if (btnPanicAlert != null) btnPanicAlert.setEnabled(true);
            return;
        }

        report.put("id", reportId);

        databaseReference.child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Report saved successfully: " + reportId);
                    Toast.makeText(this, "✅ Emergency report submitted successfully!", Toast.LENGTH_LONG).show();

                    // Navigate back to home
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save report", e);
                    Toast.makeText(this, "Failed to submit report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Re-enable buttons
                    if (btnSubmitReport != null) btnSubmitReport.setEnabled(true);
                    if (btnPanicAlert != null) btnPanicAlert.setEnabled(true);
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(TAG, "onActivityResult: " + requestCode + ", result: " + resultCode);

        if (resultCode == RESULT_OK && data != null && mediaUris.size() < 3) {
            Uri uri = data.getData();

            if (uri == null) {
                Log.w(TAG, "URI is null in onActivityResult");
                return;
            }

            if (requestCode == PICK_IMAGE_REQUEST || requestCode == PICK_VIDEO_REQUEST) {
                mediaUris.add(uri);
                updateMediaPreviews();
                Log.d(TAG, "Media added: " + uri.toString() + ", Total: " + mediaUris.size());
            }
        }
    }

    private void updateMediaPreviews() {
        if (txtSelectedFiles != null) {
            String fileText = "Selected files: " + mediaUris.size() + "/3";
            txtSelectedFiles.setText(fileText);
            txtSelectedFiles.setVisibility(View.VISIBLE);
        }

        // Reset all previews
        if (imgPreview1 != null) imgPreview1.setVisibility(View.GONE);
        if (imgPreview2 != null) imgPreview2.setVisibility(View.GONE);
        if (imgPreview3 != null) imgPreview3.setVisibility(View.GONE);

        // Show previews for first 3 images
        int imageCount = 0;
        for (int i = 0; i < Math.min(mediaUris.size(), 3); i++) {
            Uri uri = mediaUris.get(i);
            String mimeType = getContentResolver().getType(uri);

            if (mimeType != null && mimeType.startsWith("image")) {
                imageCount++;
                switch (imageCount) {
                    case 1:
                        if (imgPreview1 != null) {
                            imgPreview1.setImageURI(uri);
                            imgPreview1.setVisibility(View.VISIBLE);
                        }
                        break;
                    case 2:
                        if (imgPreview2 != null) {
                            imgPreview2.setImageURI(uri);
                            imgPreview2.setVisibility(View.VISIBLE);
                        }
                        break;
                    case 3:
                        if (imgPreview3 != null) {
                            imgPreview3.setImageURI(uri);
                            imgPreview3.setVisibility(View.VISIBLE);
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_STORAGE_PERMISSION) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show();
            } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                requestLocation();
            }
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}