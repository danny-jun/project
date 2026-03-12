package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportDetailActivity extends AppCompatActivity {

    private TextView txtEmergencyType, txtDescription, txtDateTime;
    private TextView txtStatus, txtLocation, txtResponder, txtNotes;
    private TextView txtUserName, txtUserRegNo;
    private LinearLayout layoutImages, layoutVideos;
    private CardView layoutPanicAlert;
    private Button btnViewOnMap;
    private Button btnShareReport;
    private ProgressBar progressBar;
    private CardView cardStatus, cardLocation;

    private String reportId;
    private DatabaseReference databaseReference;
    private EmergencyReport currentReport;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_report_detail);

            // Get report ID from intent
            Intent intent = getIntent();
            if (intent != null && intent.hasExtra("REPORT_ID")) {
                reportId = intent.getStringExtra("REPORT_ID");
            } else {
                Toast.makeText(this, "No report selected", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Initialize Firebase
            databaseReference = FirebaseDatabase.getInstance().getReference("emergency_reports");

            // Initialize UI components
            initUI();

            // Load report details
            loadReportDetails();
        } catch (Exception e) {
            android.util.Log.e("ReportDetailActivity", "Error in onCreate", e);
            Toast.makeText(this, "Error opening report: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initUI() {
        try {
            // TextViews
            txtEmergencyType = findViewById(R.id.txtEmergencyType);
            txtDescription = findViewById(R.id.txtDescription);
            txtDateTime = findViewById(R.id.txtDateTime);
            txtStatus = findViewById(R.id.txtStatus);
            txtLocation = findViewById(R.id.txtLocation);
            txtResponder = findViewById(R.id.txtResponder);
            txtNotes = findViewById(R.id.txtNotes);
            txtUserName = findViewById(R.id.txtUserName);
            txtUserRegNo = findViewById(R.id.txtUserRegNo);

            // Layouts
            layoutImages = findViewById(R.id.layoutImages);
            layoutVideos = findViewById(R.id.layoutVideos);
            layoutPanicAlert = findViewById(R.id.layoutPanicAlert);

            // Buttons
            Button btnBack = findViewById(R.id.btnBack);
            btnViewOnMap = findViewById(R.id.btnViewOnMap);
            btnShareReport = findViewById(R.id.btnShareReport);

            // Progress and Cards
            progressBar = findViewById(R.id.progressBar);
            cardStatus = findViewById(R.id.cardStatus);
            cardLocation = findViewById(R.id.cardLocation);

            // Set click listeners
            if (btnBack != null) {
                btnBack.setOnClickListener(v -> finish());
            }

            if (btnViewOnMap != null) {
                btnViewOnMap.setOnClickListener(v -> openLocationOnMap());
            }

            if (btnShareReport != null) {
                btnShareReport.setOnClickListener(v -> {
                    shareReport();
                });
            }
        } catch (Exception e) {
            android.util.Log.e("ReportDetailActivity", "Error in initUI", e);
            Toast.makeText(this, "Error initializing UI: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void loadReportDetails() {
        if (reportId == null || reportId.isEmpty()) {
            Toast.makeText(this, "Invalid report ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        databaseReference.child(reportId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.GONE);

                if (dataSnapshot.exists()) {
                    EmergencyReport report = dataSnapshot.getValue(EmergencyReport.class);
                    if (report != null) {
                        report.setId(reportId);
                        
                        // Auto-categorize severity based on emergency type
                        String categorizedSeverity = report.getAutoCategorizedSeverity();
                        report.setSeverity(categorizedSeverity);
                        
                        displayReportDetails(report);
                    }
                } else {
                    Toast.makeText(ReportDetailActivity.this,
                            "Report not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReportDetailActivity.this,
                        "Error loading report: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReportDetails(EmergencyReport report) {
        try {
            // Store current report for map functionality
            currentReport = report;
            
            // Emergency Type
            if (txtEmergencyType != null) {
                txtEmergencyType.setText(report.getEmergencyType());
            }

            // Description
            if (report.getDescription() != null && !report.getDescription().isEmpty()) {
                if (txtDescription != null) {
                    txtDescription.setText(report.getDescription());
                    txtDescription.setVisibility(View.VISIBLE);
                }
            } else {
                if (txtDescription != null) {
                    txtDescription.setVisibility(View.GONE);
                }
            }

            // Date & Time
            if (txtDateTime != null) {
                txtDateTime.setText(getFormattedDateTime(report.getTimestamp()));
            }

            // Severity is now auto-categorized on backend and not shown to students
            
            // Status
            if (txtStatus != null) {
                txtStatus.setText(getStatusText(report.getStatus()));
            }
            if (cardStatus != null) {
                setStatusCardColor(cardStatus, report.getStatus());
            }

            // Location
            if (txtLocation != null) {
                if (report.getLocation() != null && !report.getLocation().isEmpty()) {
                    txtLocation.setText("📍 " + report.getLocation());
                } else if (report.getLatitude() != 0 && report.getLongitude() != 0) {
                    String locationText = String.format(Locale.getDefault(),
                            "📍 Lat: %.4f, Long: %.4f",
                            report.getLatitude(), report.getLongitude());
                    txtLocation.setText(locationText);
                } else {
                    txtLocation.setText("📍 Location not available");
                }
            }

            // User Info
            if (report.getUserName() != null && txtUserName != null) {
                txtUserName.setText("Reported by: " + report.getUserName());
            }
            if (report.getUserRegNo() != null && txtUserRegNo != null) {
                txtUserRegNo.setText("Reg No: " + report.getUserRegNo());
            }

            // Responder Info
            if (report.getResponderName() != null && !report.getResponderName().isEmpty()) {
                if (txtResponder != null) {
                    txtResponder.setText("Responder: " + report.getResponderName());
                    txtResponder.setVisibility(View.VISIBLE);
                }
            } else {
                if (txtResponder != null) {
                    txtResponder.setVisibility(View.GONE);
                }
            }

            // Notes
            if (report.getNotes() != null && !report.getNotes().isEmpty()) {
                if (txtNotes != null) {
                    txtNotes.setText("Medical Notes: " + report.getNotes());
                    txtNotes.setVisibility(View.VISIBLE);
                }
            } else {
                if (txtNotes != null) {
                    txtNotes.setVisibility(View.GONE);
                }
            }

            // Panic Alert
            if (layoutPanicAlert != null) {
                if (report.isPanicAlert()) {
                    layoutPanicAlert.setVisibility(View.VISIBLE);
                } else {
                    layoutPanicAlert.setVisibility(View.GONE);
                }
            }

            // Images
            if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
                displayImages(report.getImageUrls());
            } else {
                if (layoutImages != null) {
                    layoutImages.setVisibility(View.GONE);
                }
            }

            // Videos
            if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
                displayVideos(report.getVideoUrls());
            } else {
                if (layoutVideos != null) {
                    layoutVideos.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ReportDetailActivity", "Error in displayReportDetails", e);
            Toast.makeText(this, "Error displaying report: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String getFormattedDateTime(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEE, MMM dd, yyyy • hh:mm a", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return timestamp;
        }
    }

    private void setSeverityColor(TextView textView, String severity) {
        int color;
        switch (severity) {
            case "Low":
                color = getResources().getColor(R.color.green_success);
                break;
            case "Medium":
                color = getResources().getColor(R.color.orange_warning);
                break;
            case "High":
                color = getResources().getColor(R.color.red_emergency);
                break;
            case "Critical":
                color = getResources().getColor(R.color.red_danger);
                break;
            default:
                color = getResources().getColor(R.color.text_secondary);
        }
        textView.setTextColor(color);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "pending":
                return "⏳ Pending - Waiting for medical staff";
            case "responded":
                return "🚑 Responded - Medical team en route";
            case "completed":
                return "✅ Completed - Emergency handled";
            default:
                return status;
        }
    }

    private void setStatusCardColor(CardView card, String status) {
        int backgroundColor;
        switch (status) {
            case "pending":
                backgroundColor = 0x30FFC107; // Light orange
                break;
            case "responded":
                backgroundColor = 0x302196F3; // Light blue
                break;
            case "completed":
                backgroundColor = 0x304CAF50; // Light green
                break;
            default:
                backgroundColor = 0x30E0E0E0; // Light gray
        }
        card.setCardBackgroundColor(backgroundColor);
    }

    private void displayImages(List<String> imageUrls) {
        if (layoutImages == null) return;

        layoutImages.removeAllViews();
        layoutImages.setVisibility(View.VISIBLE);

        // Add section title
        TextView imagesTitle = new TextView(this);
        imagesTitle.setText("📷 Attached Images:");
        imagesTitle.setTextSize(16);
        imagesTitle.setTextColor(getResources().getColor(R.color.text_primary));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 16);
        imagesTitle.setLayoutParams(titleParams);
        layoutImages.addView(imagesTitle);

        LinearLayout imagesContainer = new LinearLayout(this);
        imagesContainer.setOrientation(LinearLayout.HORIZONTAL);
        imagesContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
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

            // Load image using Glide with Cloudinary optimization
            // Cloudinary URLs work directly with Glide
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .thumbnail(0.1f) // Show low-res preview while loading
                    .centerCrop()
                    .override(300, 300) // Optimize loading for thumbnail size
                    .into(imageView);

            // Click to view full image in custom full-screen viewer
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(ReportDetailActivity.this, FullScreenImageActivity.class);
                intent.putExtra("IMAGE_URL", imageUrl);
                startActivity(intent);
            });

            imagesContainer.addView(imageView);
        }

        layoutImages.addView(imagesContainer);
    }

    private void displayVideos(List<String> videoUrls) {
        if (layoutVideos == null) return;

        layoutVideos.removeAllViews();
        layoutVideos.setVisibility(View.VISIBLE);

        // Add section title
        TextView videosTitle = new TextView(this);
        videosTitle.setText("🎬 Attached Videos:");
        videosTitle.setTextSize(16);
        videosTitle.setTextColor(getResources().getColor(R.color.text_primary));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 16);
        videosTitle.setLayoutParams(titleParams);
        layoutVideos.addView(videosTitle);

        LinearLayout videosContainer = new LinearLayout(this);
        videosContainer.setOrientation(LinearLayout.HORIZONTAL);
        videosContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
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

            // For Cloudinary videos, generate thumbnail URL by replacing video path with image path
            // Cloudinary format: https://res.cloudinary.com/[cloud]/video/upload/[path]
            // Thumbnail format: https://res.cloudinary.com/[cloud]/video/upload/so_0/[path].jpg
            String thumbnailUrl = videoUrl;
            if (videoUrl.contains("cloudinary.com")) {
                // Extract the video path and generate thumbnail
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

        layoutVideos.addView(videosContainer);
    }

    private void shareReport() {
        String shareText = "DKUT Medical Emergency Report:\n" +
                "Type: " + txtEmergencyType.getText() + "\n" +
                "Status: " + txtStatus.getText() + "\n" +
                "Time: " + txtDateTime.getText() + "\n" +
                "Location: " + txtLocation.getText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DKUT Emergency Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Report"));
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

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}