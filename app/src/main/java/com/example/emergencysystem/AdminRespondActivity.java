package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminRespondActivity extends AppCompatActivity {

    private TextView txtEmergencyType, txtDescription, txtDateTime, txtSeverity;
    private TextView txtLocation, txtUserName, txtCurrentStatus;
    private RadioGroup radioGroupStatus;
    private EditText edtResponderNotes;
    private Button btnUpdateStatus, btnBack, btnViewOnMap;
    private ProgressBar progressBar;
    private CardView layoutPanicAlert, cardMedia;
    private LinearLayout layoutImages, layoutVideos;

    private String reportId;
    private EmergencyReport currentReport;
    private DatabaseReference reportRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_respond);

        reportId = getIntent().getStringExtra("REPORT_ID");
        if (reportId == null) {
            Toast.makeText(this, "Invalid report", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mAuth = FirebaseAuth.getInstance();
        reportRef = FirebaseDatabase.getInstance()
                .getReference("emergency_reports").child(reportId);

        initUI();
        loadReportDetails();
    }

    private void initUI() {
        txtEmergencyType = findViewById(R.id.txtEmergencyType);
        txtDescription = findViewById(R.id.txtDescription);
        txtDateTime = findViewById(R.id.txtDateTime);
        txtSeverity = findViewById(R.id.txtSeverity);
        txtLocation = findViewById(R.id.txtLocation);
        txtUserName = findViewById(R.id.txtUserName);
        txtCurrentStatus = findViewById(R.id.txtCurrentStatus);
        
        radioGroupStatus = findViewById(R.id.radioGroupStatus);
        edtResponderNotes = findViewById(R.id.edtResponderNotes);
        
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnBack = findViewById(R.id.btnBack);
        btnViewOnMap = findViewById(R.id.btnViewOnMap);
        progressBar = findViewById(R.id.progressBar);
        
        layoutPanicAlert = findViewById(R.id.layoutPanicAlert);
        cardMedia = findViewById(R.id.cardMedia);
        layoutImages = findViewById(R.id.layoutImages);
        layoutVideos = findViewById(R.id.layoutVideos);

        btnBack.setOnClickListener(v -> finish());
        btnUpdateStatus.setOnClickListener(v -> updateReportStatus());
        btnViewOnMap.setOnClickListener(v -> openLocationOnMap());
    }

    @SuppressLint("SetTextI18n")
    private void loadReportDetails() {
        progressBar.setVisibility(View.VISIBLE);

        reportRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentReport = snapshot.getValue(EmergencyReport.class);
                    if (currentReport != null) {
                        currentReport.setId(reportId);
                        displayReportDetails();
                    }
                } else {
                    Toast.makeText(AdminRespondActivity.this,
                            "Report not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminRespondActivity.this,
                        "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void displayReportDetails() {
        txtEmergencyType.setText(currentReport.getEmergencyType() != null ?
                currentReport.getEmergencyType() : "Unknown Emergency");

        txtDescription.setText(currentReport.getDescription() != null ?
                currentReport.getDescription() : "No description provided");

        txtDateTime.setText(currentReport.getFormattedDate() != null ?
                currentReport.getFormattedDate() : "Unknown time");

        String severity = currentReport.getSeverity() != null ?
                currentReport.getSeverity() : "Medium";
        txtSeverity.setText(severity);
        setSeverityColor(txtSeverity, severity);

        txtLocation.setText(currentReport.getLocation() != null ?
                "📍 " + currentReport.getLocation() : "📍 Location not available");

        // Load user info
        if (currentReport.getUserId() != null) {
            loadUserInfo(currentReport.getUserId());
        }

        // Current status
        String status = currentReport.getStatus() != null ?
                currentReport.getStatus() : "pending";
        txtCurrentStatus.setText(getStatusText(status));

        // Set radio button based on current status
        if ("responded".equalsIgnoreCase(status)) {
            radioGroupStatus.check(R.id.radioResponded);
        } else if ("completed".equalsIgnoreCase(status)) {
            radioGroupStatus.check(R.id.radioCompleted);
        } else {
            radioGroupStatus.check(R.id.radioPending);
        }

        // Show existing notes if any
        if (currentReport.getResponderNotes() != null && !currentReport.getResponderNotes().isEmpty()) {
            edtResponderNotes.setText(currentReport.getResponderNotes());
        }

        // Panic alert
        if (currentReport.isPanicAlert()) {
            layoutPanicAlert.setVisibility(View.VISIBLE);
        } else {
            layoutPanicAlert.setVisibility(View.GONE);
        }

        // Display images and videos
        displayMedia();
    }

    private void displayMedia() {
        boolean hasMedia = false;

        // Display images
        if (currentReport.getImageUrls() != null && !currentReport.getImageUrls().isEmpty()) {
            displayImages(currentReport.getImageUrls());
            hasMedia = true;
        } else {
            layoutImages.setVisibility(View.GONE);
        }

        // Display videos
        if (currentReport.getVideoUrls() != null && !currentReport.getVideoUrls().isEmpty()) {
            displayVideos(currentReport.getVideoUrls());
            hasMedia = true;
        } else {
            layoutVideos.setVisibility(View.GONE);
        }

        // Show/hide media card
        if (hasMedia) {
            cardMedia.setVisibility(View.VISIBLE);
        } else {
            cardMedia.setVisibility(View.GONE);
        }
    }

    private void displayImages(java.util.List<String> imageUrls) {
        layoutImages.removeAllViews();
        layoutImages.setVisibility(View.VISIBLE);

        // Add section title
        TextView imagesTitle = new TextView(this);
        imagesTitle.setText("📷 Images (" + imageUrls.size() + ")");
        imagesTitle.setTextSize(14);
        imagesTitle.setTextColor(0xFF1F2A44);
        imagesTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 16);
        imagesTitle.setLayoutParams(titleParams);
        layoutImages.addView(imagesTitle);

        // Create horizontal scroll for images
        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout imagesContainer = new LinearLayout(this);
        imagesContainer.setOrientation(LinearLayout.HORIZONTAL);
        imagesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.trim().isEmpty()) continue;

            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    300, 300
            );
            params.setMargins(0, 0, 16, 0);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setBackgroundResource(R.drawable.image_background);

            // Load image using Glide
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .override(300, 300)
                    .into(imageView);

            // Click to view full image
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminRespondActivity.this, FullScreenImageActivity.class);
                intent.putExtra("IMAGE_URL", imageUrl);
                startActivity(intent);
            });

            imagesContainer.addView(imageView);
        }

        scrollView.addView(imagesContainer);
        layoutImages.addView(scrollView);
    }

    private void displayVideos(java.util.List<String> videoUrls) {
        layoutVideos.removeAllViews();
        layoutVideos.setVisibility(View.VISIBLE);

        // Add section title
        TextView videosTitle = new TextView(this);
        videosTitle.setText("🎬 Videos (" + videoUrls.size() + ")");
        videosTitle.setTextSize(14);
        videosTitle.setTextColor(0xFF1F2A44);
        videosTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 16, 0, 16);
        videosTitle.setLayoutParams(titleParams);
        layoutVideos.addView(videosTitle);

        // Create horizontal scroll for videos
        android.widget.HorizontalScrollView scrollView = new android.widget.HorizontalScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout videosContainer = new LinearLayout(this);
        videosContainer.setOrientation(LinearLayout.HORIZONTAL);
        videosContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (String videoUrl : videoUrls) {
            if (videoUrl == null || videoUrl.trim().isEmpty()) continue;

            // Create a frame layout to hold thumbnail and play button
            android.widget.FrameLayout frameLayout = new android.widget.FrameLayout(this);
            LinearLayout.LayoutParams frameParams = new LinearLayout.LayoutParams(300, 300);
            frameParams.setMargins(0, 0, 16, 0);
            frameLayout.setLayoutParams(frameParams);
            frameLayout.setBackgroundResource(R.drawable.image_background);

            // Thumbnail view
            ImageView thumbnailView = new ImageView(this);
            android.widget.FrameLayout.LayoutParams thumbParams = new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
            thumbnailView.setLayoutParams(thumbParams);
            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);

            // Generate Cloudinary video thumbnail
            String thumbnailUrl = videoUrl;
            if (videoUrl.contains("cloudinary.com")) {
                thumbnailUrl = videoUrl.replace("/upload/", "/upload/so_0,w_300,h_300,c_fill/") + ".jpg";
            }

            // Load video thumbnail using Glide
            Glide.with(this)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .override(300, 300)
                    .into(thumbnailView);

            // Add play button overlay
            ImageView playIcon = new ImageView(this);
            android.widget.FrameLayout.LayoutParams playParams = new android.widget.FrameLayout.LayoutParams(
                    100, 100
            );
            playParams.gravity = android.view.Gravity.CENTER;
            playIcon.setLayoutParams(playParams);
            playIcon.setImageResource(R.drawable.ic_play);
            playIcon.setScaleType(ImageView.ScaleType.FIT_CENTER);

            // Add thumbnail and play button to frame
            frameLayout.addView(thumbnailView);
            frameLayout.addView(playIcon);

            // Click to play video
            frameLayout.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot play video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            videosContainer.addView(frameLayout);
        }

        scrollView.addView(videosContainer);
        layoutVideos.addView(scrollView);
    }

    private void loadUserInfo(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId);
        
        userRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                String fullName = snapshot.child("fullName").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String regNo = snapshot.child("regNo").getValue(String.class);
                String role = snapshot.child("role").getValue(String.class);
                
                StringBuilder userInfo = new StringBuilder();
                if (fullName != null && !fullName.isEmpty()) {
                    userInfo.append(fullName);
                } else {
                    userInfo.append("Unknown User");
                }
                
                // Show registration number only for students (user role)
                if ("user".equals(role)) {
                    if (regNo != null && !regNo.isEmpty() && !regNo.equals("N/A")) {
                        userInfo.append("\nReg: ").append(regNo);
                    }
                }
                
                if (email != null && !email.isEmpty()) {
                    userInfo.append("\n📧 ").append(email);
                }
                
                txtUserName.setText(userInfo.toString());
            } else {
                txtUserName.setText("Reporter: Unknown User");
            }
        }).addOnFailureListener(e -> {
            // If we can't load the full info, at least show something
            Log.e("AdminRespond", "Error loading user info: " + e.getMessage());
            txtUserName.setText("Reporter information unavailable");
        });
    }

    private void updateReportStatus() {
        int selectedStatusId = radioGroupStatus.getCheckedRadioButtonId();
        String newStatus = "pending";
        
        if (selectedStatusId == R.id.radioResponded) {
            newStatus = "responded";
        } else if (selectedStatusId == R.id.radioCompleted) {
            newStatus = "completed";
        }

        String responderNotes = edtResponderNotes.getText().toString().trim();
        
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String responderId = mAuth.getCurrentUser().getUid();
        String responderEmail = mAuth.getCurrentUser().getEmail();
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("responderNotes", responderNotes);
        updates.put("responderId", responderId);
        updates.put("responderEmail", responderEmail);
        updates.put("respondedAt", timestamp);

        progressBar.setVisibility(View.VISIBLE);
        btnUpdateStatus.setEnabled(false);

        reportRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    btnUpdateStatus.setEnabled(true);
                });
    }

    private void setSeverityColor(TextView textView, String severity) {
        int color;
        switch (severity) {
            case "Low":
                color = 0xFF4CAF50;
                break;
            case "Medium":
                color = 0xFFFFA726;
                break;
            case "High":
                color = 0xFFFF5722;
                break;
            case "Critical":
                color = 0xFFE53935;
                break;
            default:
                color = 0xFF757575;
        }
        textView.setTextColor(color);
    }

    private String getStatusText(String status) {
        switch (status.toLowerCase()) {
            case "pending":
                return "⏳ Pending";
            case "responded":
                return "🚑 Responded";
            case "completed":
                return "✅ Completed";
            default:
                return status;
        }
    }

    private void openLocationOnMap() {
        if (currentReport == null) {
            Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
            return;
        }

        double latitude = currentReport.getLatitude();
        double longitude = currentReport.getLongitude();

        if (latitude == 0 && longitude == 0) {
            Toast.makeText(this, "Location coordinates not available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a URI for Google Maps
        String label = currentReport.getLocation() != null ? 
                      currentReport.getLocation() : "Emergency Location";
        Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + 
                                    "?q=" + latitude + "," + longitude + 
                                    "(" + Uri.encode(label) + ")");

        // Create an Intent from gmmIntentUri
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        // Try to start the intent
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // If Google Maps is not installed, open in browser
            Uri browserUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + 
                                      latitude + "," + longitude);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
            startActivity(browserIntent);
        }
    }
}
