package com.example.emergencysystem;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.IOException;
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
    private static final int REQUEST_IMAGE_PERMISSION = 2;
    private static final int REQUEST_VIDEO_PERMISSION = 3;
    private static final int PICK_IMAGE_REQUEST = 4;
    private static final int PICK_VIDEO_REQUEST = 5;

    private EditText edtDescription, edtManualLocation;
    private Spinner spinnerEmergencyType;
    private RadioGroup radioGroupSeverity, radioGroupLocation;
    private ImageView imgPreview1, imgPreview2, imgPreview3;
    private TextView txtSelectedFiles, txtCurrentLocation;
    private Button btnSubmitReport, btnPanicAlert, btnRefreshLocation;
    private LinearLayout layoutGPSLocation, layoutManualLocation;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;

    private final List<Uri> mediaUris = new ArrayList<>();
    private final List<String> cloudinaryImageUrls = new ArrayList<>();
    private final List<String> cloudinaryVideoUrls = new ArrayList<>();

    private double latitude = 0.0;
    private double longitude = 0.0;
    private String locationName = "Location not available";

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
            edtManualLocation = findViewById(R.id.edtManualLocation);
            spinnerEmergencyType = findViewById(R.id.spinnerEmergencyType);
            radioGroupSeverity = findViewById(R.id.radioGroupSeverity);
            radioGroupLocation = findViewById(R.id.radioGroupLocation);

            imgPreview1 = findViewById(R.id.imgPreview1);
            imgPreview2 = findViewById(R.id.imgPreview2);
            imgPreview3 = findViewById(R.id.imgPreview3);

            txtSelectedFiles = findViewById(R.id.txtSelectedFiles);
            txtCurrentLocation = findViewById(R.id.txtCurrentLocation);

            layoutGPSLocation = findViewById(R.id.layoutGPSLocation);
            layoutManualLocation = findViewById(R.id.layoutManualLocation);

            Button btnUploadPhoto = findViewById(R.id.btnUploadPhoto);
            Button btnUploadVideo = findViewById(R.id.btnUploadVideo);
            btnSubmitReport = findViewById(R.id.btnSubmitReport);
            btnPanicAlert = findViewById(R.id.btnPanicAlert);
            btnRefreshLocation = findViewById(R.id.btnRefreshLocation);

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

            if (btnRefreshLocation != null) {
                btnRefreshLocation.setOnClickListener(v -> {
                    txtCurrentLocation.setText("🔄 Detecting location...");
                    requestLocation();
                });
            }

            // Location toggle listener
            if (radioGroupLocation != null) {
                radioGroupLocation.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.radioUseGPS) {
                        layoutGPSLocation.setVisibility(View.VISIBLE);
                        layoutManualLocation.setVisibility(View.GONE);
                        requestLocation();
                    } else if (checkedId == R.id.radioManualLocation) {
                        layoutGPSLocation.setVisibility(View.GONE);
                        layoutManualLocation.setVisibility(View.VISIBLE);
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
            // Try to get existing instance first
            try {
                MediaManager.get();
                Log.d(TAG, "Cloudinary already initialized");
            } catch (IllegalStateException e) {
                // MediaManager not initialized yet, initialize it now
                Map<String, String> config = new HashMap<>();
                config.put("cloud_name", "dknax0k01");
                config.put("api_key", "685787515875988");
                config.put("api_secret", "LkjCm1pUcrc3JSgYafsA54naZ0w"); // Required for uploads
                config.put("secure", "true");
                MediaManager.init(this, config);
                Log.d(TAG, "Cloudinary initialized successfully");
            }
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
                
                // Get location name using reverse geocoding
                getLocationName(latitude, longitude);
                updateLocationDisplay();
            } else {
                Log.w(TAG, "Location is null, using default coordinates");
                // Set default coordinates for DKUT
                latitude = -0.3673;
                longitude = 36.9456;
                locationName = "DKUT Campus, Nyeri";
                updateLocationDisplay();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get location", e);
            // Set default coordinates
            latitude = -0.3673;
            longitude = 36.9456;
            locationName = "DKUT Campus, Nyeri";
            updateLocationDisplay();
        });
    }

    private void updateLocationDisplay() {
        if (txtCurrentLocation != null) {
            txtCurrentLocation.setText("📍 " + locationName + "\n" + 
                                      "Coordinates: " + String.format("%.4f, %.4f", latitude, longitude));
        }
    }

    private void getLocationName(double lat, double lon) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder locationBuilder = new StringBuilder();
                
                // Build a readable location string
                if (address.getSubLocality() != null) {
                    locationBuilder.append(address.getSubLocality()).append(", ");
                }
                if (address.getLocality() != null) {
                    locationBuilder.append(address.getLocality());
                } else if (address.getSubAdminArea() != null) {
                    locationBuilder.append(address.getSubAdminArea());
                }
                if (address.getAdminArea() != null && locationBuilder.length() > 0) {
                    locationBuilder.append(", ").append(address.getAdminArea());
                }
                
                locationName = locationBuilder.length() > 0 ? locationBuilder.toString() : 
                              "Lat: " + String.format("%.4f", lat) + ", Lon: " + String.format("%.4f", lon);
                
                Log.d(TAG, "Location name: " + locationName);
                updateLocationDisplay();
            } else {
                locationName = "Lat: " + String.format("%.4f", lat) + ", Lon: " + String.format("%.4f", lon);
                updateLocationDisplay();
            }
        } catch (IOException e) {
            Log.e(TAG, "Geocoder failed", e);
            locationName = "Lat: " + String.format("%.4f", lat) + ", Lon: " + String.format("%.4f", lon);
            updateLocationDisplay();
        }
    }

    private void requestStoragePermissionForImage() {
        // Android 13+ (API 33+) requires specific media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_IMAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_IMAGE_PERMISSION);
            } else {
                openImagePicker();
            }
        }
    }

    private void requestStoragePermissionForVideo() {
        // Android 13+ (API 33+) requires specific media permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_VIDEO},
                        REQUEST_VIDEO_PERMISSION);
            } else {
                openVideoPicker();
            }
        } else {
            // Android 12 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_VIDEO_PERMISSION);
            } else {
                openVideoPicker();
            }
        }
    }

    private void openImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
            Log.d(TAG, "Image picker opened");
        } catch (Exception e) {
            Log.e(TAG, "Error opening image picker", e);
            Toast.makeText(this, "Cannot open gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openVideoPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
            intent.setType("video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_VIDEO_REQUEST);
            Log.d(TAG, "Video picker opened");
        } catch (Exception e) {
            Log.e(TAG, "Error opening video picker", e);
            Toast.makeText(this, "Cannot open video gallery: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

        // Check user role - only students and staff can submit reports
        checkUserRoleAndSubmit(userId, userEmail, isPanicAlert);
    }

    private void checkUserRoleAndSubmit(String userId, String userEmail, boolean isPanicAlert) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);

        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String role = snapshot.child("role").getValue(String.class);
                String fullName = snapshot.child("fullName").getValue(String.class);

                // Only students (user) and staff can submit reports
                if ("medicalOfficer".equals(role)) {
                    Toast.makeText(EmergencyReportActivity.this,
                            "Medical Officers cannot submit emergency reports. Use the dashboard to respond to emergencies.",
                            Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }

                // Proceed with report submission for students and staff
                submitReportForApprovedRole(userId, userEmail, fullName, isPanicAlert);
            } else {
                Toast.makeText(EmergencyReportActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(EmergencyReportActivity.this,
                    "Error verifying user role: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void submitReportForApprovedRole(String userId, String userEmail, String fullName, boolean isPanicAlert) {
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

        // Check location method and validate
        String finalLocationName = locationName;
        if (radioGroupLocation != null && radioGroupLocation.getCheckedRadioButtonId() == R.id.radioManualLocation) {
            // User chose manual location
            String manualLocation = edtManualLocation.getText().toString().trim();
            if (manualLocation.isEmpty()) {
                Toast.makeText(this, "Please enter a location or switch to GPS", Toast.LENGTH_SHORT).show();
                return;
            }
            finalLocationName = manualLocation;
            // Keep latitude/longitude as 0 or use DKUT default for manual entries
            if (latitude == 0 && longitude == 0) {
                latitude = -0.3673;
                longitude = 36.9456;
            }
        } else {
            // Using GPS location
            if (latitude == 0 && longitude == 0) {
                Toast.makeText(this, "Waiting for location... Please try again", Toast.LENGTH_SHORT).show();
                requestLocation();
                return;
            }
        }

        // Disable buttons to prevent multiple submissions
        if (btnSubmitReport != null) btnSubmitReport.setEnabled(false);
        if (btnPanicAlert != null) btnPanicAlert.setEnabled(false);

        Map<String, Object> report = new HashMap<>();
        report.put("id", ""); // Will be set when saving
        report.put("userId", userId);
        report.put("userEmail", userEmail);
        report.put("userName", fullName != null ? fullName : "DKUT User"); // Use actual user name
        report.put("emergencyType", emergencyType);
        report.put("description", description);
        report.put("latitude", latitude);
        report.put("longitude", longitude);
        report.put("location", finalLocationName);
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
        final int[] failedCount = {0};

        Toast.makeText(this, "Uploading " + totalMedia + " file(s)...", Toast.LENGTH_SHORT).show();

        for (int i = 0; i < mediaUris.size(); i++) {
            Uri uri = mediaUris.get(i);
            String mimeType = getContentResolver().getType(uri);
            String resourceType = (mimeType != null && mimeType.startsWith("video")) ? "video" : "image";

            String userId = (String) report.get("userId");
            String folder = "dkut_emergency/" + userId;
            
            final int currentIndex = i + 1;
            Log.d(TAG, "Starting upload " + currentIndex + "/" + totalMedia + " - Type: " + resourceType);

            MediaManager.get().upload(uri)
                    .option("resource_type", resourceType)
                    .option("folder", folder)
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .callback(new UploadCallback() {
                        @Override
                        public void onSuccess(String id, Map data) {
                            String mediaUrl = (String) data.get("secure_url");
                            Log.d(TAG, "Media uploaded successfully (" + uploadedCount[0] + 1 + "/" + totalMedia + "): " + mediaUrl);

                            if (resourceType.equals("video")) {
                                cloudinaryVideoUrls.add(mediaUrl);
                            } else {
                                cloudinaryImageUrls.add(mediaUrl);
                            }

                            uploadedCount[0]++;

                            if (uploadedCount[0] + failedCount[0] == totalMedia) {
                                // All uploads processed (success or failure)
                                report.put("imageUrls", cloudinaryImageUrls);
                                report.put("videoUrls", cloudinaryVideoUrls);
                                
                                if (uploadedCount[0] > 0) {
                                    Toast.makeText(EmergencyReportActivity.this,
                                            "Uploaded " + uploadedCount[0] + "/" + totalMedia + " file(s)",
                                            Toast.LENGTH_SHORT).show();
                                }
                                
                                saveReport(report);
                            }
                        }

                        @Override
                        public void onError(String id, ErrorInfo error) {
                            Log.e(TAG, "Cloudinary upload error: " + error.getDescription());
                            failedCount[0]++;
                            
                            if (uploadedCount[0] + failedCount[0] == totalMedia) {
                                if (failedCount[0] == totalMedia) {
                                    Toast.makeText(EmergencyReportActivity.this,
                                            "All media uploads failed, saving report without media",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(EmergencyReportActivity.this,
                                            failedCount[0] + " file(s) failed to upload",
                                            Toast.LENGTH_SHORT).show();
                                }
                                
                                report.put("imageUrls", cloudinaryImageUrls);
                                report.put("videoUrls", cloudinaryVideoUrls);
                                saveReport(report);
                            }
                        }

                        @Override
                        public void onStart(String id) {
                            Log.d(TAG, "Upload started for: " + id);
                        }

                        @Override
                        public void onProgress(String id, long bytes, long total) {
                            int progress = (int) ((bytes * 100) / total);
                            if (progress % 20 == 0) { // Log every 20%
                                Log.d(TAG, "Upload progress: " + progress + "%");
                            }
                        }

                        @Override
                        public void onReschedule(String id, ErrorInfo error) {
                            Log.w(TAG, "Upload rescheduled: " + error.getDescription());
                        }
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

        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();

            if (uri == null) {
                Log.w(TAG, "URI is null in onActivityResult");
                Toast.makeText(this, "Failed to get file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check if we can add more media files (limit to 3)
            if (mediaUris.size() >= 3) {
                Toast.makeText(this, "Maximum 3 files allowed", Toast.LENGTH_SHORT).show();
                return;
            }

            if (requestCode == PICK_IMAGE_REQUEST || requestCode == PICK_VIDEO_REQUEST) {
                try {
                    // Take persistable URI permission for later access
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    Log.d(TAG, "Persistable URI permission granted for: " + uri);
                } catch (SecurityException e) {
                    Log.w(TAG, "Could not take persistable permission, continuing anyway: " + e.getMessage());
                    // Continue even if we can't get persistable permission - it might still work
                }

                // Add to our media list
                mediaUris.add(uri);
                updateMediaPreviews();
                
                String mediaType = (requestCode == PICK_IMAGE_REQUEST) ? "Image" : "Video";
                Log.d(TAG, mediaType + " added: " + uri.toString() + ", Total files: " + mediaUris.size());
                Toast.makeText(this, mediaType + " selected (" + mediaUris.size() + "/3)", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(TAG, "No file selected or operation cancelled");
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

        // Show previews for images
        int imageCount = 0;
        for (int i = 0; i < Math.min(mediaUris.size(), 3); i++) {
            Uri uri = mediaUris.get(i);
            String mimeType = getContentResolver().getType(uri);

            if (mimeType != null && mimeType.startsWith("image")) {
                imageCount++;
                ImageView targetPreview = null;
                
                switch (imageCount) {
                    case 1:
                        targetPreview = imgPreview1;
                        break;
                    case 2:
                        targetPreview = imgPreview2;
                        break;
                    case 3:
                        targetPreview = imgPreview3;
                        break;
                }
                
                if (targetPreview != null) {
                    try {
                        targetPreview.setImageURI(uri);
                        targetPreview.setVisibility(View.VISIBLE);
                        Log.d(TAG, "Preview set for image " + imageCount + ": " + uri);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting image preview", e);
                    }
                }
            } else if (mimeType != null && mimeType.startsWith("video")) {
                // For videos, we could show a video icon in the preview
                imageCount++;
                ImageView targetPreview = null;
                
                switch (imageCount) {
                    case 1:
                        targetPreview = imgPreview1;
                        break;
                    case 2:
                        targetPreview = imgPreview2;
                        break;
                    case 3:
                        targetPreview = imgPreview3;
                        break;
                }
                
                if (targetPreview != null) {
                    targetPreview.setImageResource(R.drawable.ic_video_placeholder);
                    targetPreview.setVisibility(View.VISIBLE);
                    Log.d(TAG, "Video icon set for preview " + imageCount);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == REQUEST_IMAGE_PERMISSION) {
                Log.d(TAG, "Image permission granted");
                Toast.makeText(this, "Permission granted, opening gallery", Toast.LENGTH_SHORT).show();
                openImagePicker();
            } else if (requestCode == REQUEST_VIDEO_PERMISSION) {
                Log.d(TAG, "Video permission granted");
                Toast.makeText(this, "Permission granted, opening video gallery", Toast.LENGTH_SHORT).show();
                openVideoPicker();
            } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
                Log.d(TAG, "Location permission granted");
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                requestLocation();
            }
        } else {
            // Permission denied
            if (requestCode == REQUEST_IMAGE_PERMISSION) {
                Toast.makeText(this, "Image permission denied - Cannot select images", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQUEST_VIDEO_PERMISSION) {
                Toast.makeText(this, "Video permission denied - Cannot select videos", Toast.LENGTH_LONG).show();
            } else if (requestCode == REQUEST_LOCATION_PERMISSION) {
                Toast.makeText(this, "Location permission denied - Using default location", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}