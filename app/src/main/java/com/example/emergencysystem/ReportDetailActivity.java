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
    private TextView txtSeverity, txtStatus, txtLocation, txtResponder, txtNotes;
    private TextView txtUserName, txtUserRegNo;
    private LinearLayout layoutImages, layoutVideos, layoutPanicAlert;
    private Button btnViewOnMap;
    private Button btnShareReport;
    private ProgressBar progressBar;
    private CardView cardStatus, cardLocation;

    private String reportId;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void initUI() {
        // TextViews
        txtEmergencyType = findViewById(R.id.txtEmergencyType);
        txtDescription = findViewById(R.id.txtDescription);
        txtDateTime = findViewById(R.id.txtDateTime);
        txtSeverity = findViewById(R.id.txtSeverity);
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
        btnBack.setOnClickListener(v -> finish());

        btnViewOnMap.setOnClickListener(v -> {
            // Open Google Maps with the location
            Toast.makeText(this, "Opening location on map...", Toast.LENGTH_SHORT).show();
            // You can implement map navigation here
        });

        btnShareReport.setOnClickListener(v -> {
            shareReport();
        });
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
        // Emergency Type
        txtEmergencyType.setText(report.getEmergencyType());

        // Description
        if (report.getDescription() != null && !report.getDescription().isEmpty()) {
            txtDescription.setText(report.getDescription());
            txtDescription.setVisibility(View.VISIBLE);
        } else {
            txtDescription.setVisibility(View.GONE);
        }

        // Date & Time
        txtDateTime.setText(getFormattedDateTime(report.getTimestamp()));

        // Severity
        txtSeverity.setText(report.getSeverity());
        setSeverityColor(txtSeverity, report.getSeverity());

        // Status
        txtStatus.setText(getStatusText(report.getStatus()));
        setStatusCardColor(cardStatus, report.getStatus());

        // Location
        if (report.getLatitude() != 0 && report.getLongitude() != 0) {
            String locationText = String.format(Locale.getDefault(),
                    "📍 Lat: %.6f\nLong: %.6f",
                    report.getLatitude(), report.getLongitude());
            txtLocation.setText(locationText);
        } else {
            txtLocation.setText("📍 Location not available");
        }

        // User Info
        if (report.getUserName() != null) {
            txtUserName.setText("Reported by: " + report.getUserName());
        }
        if (report.getUserRegNo() != null) {
            txtUserRegNo.setText("Reg No: " + report.getUserRegNo());
        }

        // Responder Info
        if (report.getResponderName() != null && !report.getResponderName().isEmpty()) {
            txtResponder.setText("Responder: " + report.getResponderName());
            txtResponder.setVisibility(View.VISIBLE);
        } else {
            txtResponder.setVisibility(View.GONE);
        }

        // Notes
        if (report.getNotes() != null && !report.getNotes().isEmpty()) {
            txtNotes.setText("Medical Notes: " + report.getNotes());
            txtNotes.setVisibility(View.VISIBLE);
        } else {
            txtNotes.setVisibility(View.GONE);
        }

        // Panic Alert
        if (report.isPanicAlert()) {
            layoutPanicAlert.setVisibility(View.VISIBLE);
        } else {
            layoutPanicAlert.setVisibility(View.GONE);
        }

        // Images
        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            displayImages(report.getImageUrls());
        } else {
            layoutImages.setVisibility(View.GONE);
        }

        // Videos
        if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
            displayVideos(report.getVideoUrls());
        } else {
            if (layoutVideos != null) {
                layoutVideos.setVisibility(View.GONE);
            }
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
                    .into(imageView);

            // Click to view full image
            imageView.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(imageUrl));
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
            ImageView thumbnailView = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    300, 300
            );
            params.setMargins(0, 0, 16, 0);
            thumbnailView.setLayoutParams(params);
            thumbnailView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnailView.setBackgroundResource(R.drawable.image_background);

            // Set video icon or thumbnail (you can implement video thumbnail extraction if needed)
            thumbnailView.setImageResource(R.drawable.ic_video_placeholder);

            // Add play button overlay
            ImageView playIcon = new ImageView(this);
            playIcon.setImageResource(R.drawable.ic_play);
            LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(
                    80, 80
            );
            playIcon.setLayoutParams(playParams);

            // Create a container for the video thumbnail with play button
            LinearLayout videoContainer = new LinearLayout(this);
            videoContainer.setOrientation(LinearLayout.VERTICAL);
            videoContainer.setLayoutParams(new LinearLayout.LayoutParams(
                    300, 300
            ));
            videoContainer.addView(thumbnailView);
            videoContainer.addView(playIcon);

            // Click to play video
            videoContainer.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                startActivity(intent);
            });

            videosContainer.addView(videoContainer);
        }

        layoutVideos.addView(videosContainer);
    }

    private void shareReport() {
        String shareText = "DKUT Medical Emergency Report:\n" +
                "Type: " + txtEmergencyType.getText() + "\n" +
                "Severity: " + txtSeverity.getText() + "\n" +
                "Status: " + txtStatus.getText() + "\n" +
                "Time: " + txtDateTime.getText() + "\n" +
                "Location: " + txtLocation.getText();

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DKUT Emergency Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Report"));
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}