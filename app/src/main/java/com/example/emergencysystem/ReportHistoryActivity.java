package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysystem.adapters.ReportAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportHistoryActivity extends AppCompatActivity {

    private static final String TAG = "ReportHistory";
    private RecyclerView recyclerView;
    private ReportAdapter reportAdapter;
    private List<EmergencyReport> reportList;
    private DatabaseReference databaseReference;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView txtNoReports, txtStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_history);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("emergency_reports");

        initUI();
        setupRecyclerView();

        // Debug button - remove after testing
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button btnDebug = findViewById(R.id.btnDebug);
        if (btnDebug != null) {
            btnDebug.setOnClickListener(v -> {
                debugDatabaseContents();
                testDatabaseConnection();
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadUserReports();
    }

    private void initUI() {
        recyclerView = findViewById(R.id.recyclerViewReports);
        progressBar = findViewById(R.id.progressBar);
        txtNoReports = findViewById(R.id.txtNoReports);
        txtStats = findViewById(R.id.txtStats);

        Button btnBack = findViewById(R.id.btnBack);
        Button btnRefresh = findViewById(R.id.btnRefresh);

        btnBack.setOnClickListener(v -> finish());

        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            loadUserReports();
        });
    }

    private void setupRecyclerView() {
        reportList = new ArrayList<>();
        reportAdapter = new ReportAdapter(reportList, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(reportAdapter);

        reportAdapter.setOnItemClickListener(position -> {
            if (position < reportList.size()) {
                EmergencyReport report = reportList.get(position);
                if (report != null && report.getId() != null) {
                    Intent intent = new Intent(this, ReportDetailActivity.class);
                    intent.putExtra("REPORT_ID", report.getId());
                    startActivity(intent);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadUserReports() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, "Loading reports for user: " + userId);
        Log.d(TAG, "User email: " + currentUser.getEmail());

        progressBar.setVisibility(View.VISIBLE);
        txtNoReports.setVisibility(View.GONE);

        databaseReference.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        reportList.clear();

                        Log.d(TAG, "Snapshot exists: " + dataSnapshot.exists());
                        Log.d(TAG, "Children count: " + dataSnapshot.getChildrenCount());

                        if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                            int count = 0;
                            int reportsWithImages = 0;
                            int reportsWithVideos = 0;

                            for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                try {
                                    EmergencyReport report = snapshot.getValue(EmergencyReport.class);
                                    if (report != null) {
                                        report.setId(snapshot.getKey());

                                        // Log media information for debugging
                                        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
                                            reportsWithImages++;
                                            Log.d(TAG, "Report #" + (count+1) + " has " +
                                                    report.getImageUrls().size() + " image(s)");
                                        }

                                        if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
                                            reportsWithVideos++;
                                            Log.d(TAG, "Report #" + (count+1) + " has " +
                                                    report.getVideoUrls().size() + " video(s)");
                                        }

                                        reportList.add(report);
                                        count++;
                                        Log.d(TAG, "Found report #" + count + ": " +
                                                report.getEmergencyType() + " at " + report.getTimestamp());
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing report: " + e.getMessage());
                                }
                            }

                            // Sort by date (newest first)
                            sortReportsByDate();

                            // Update UI
                            reportAdapter.notifyDataSetChanged();
                            recyclerView.setVisibility(View.VISIBLE);
                            txtNoReports.setVisibility(View.GONE);

                            // Update stats including media info
                            updateReportStats(reportsWithImages, reportsWithVideos);

                            Toast.makeText(ReportHistoryActivity.this,
                                    "Found " + reportList.size() + " emergency reports",
                                    Toast.LENGTH_SHORT).show();

                        } else {
                            // No reports found
                            recyclerView.setVisibility(View.GONE);
                            txtNoReports.setVisibility(View.VISIBLE);
                            txtNoReports.setText("📭 No emergency reports found\n\n" +
                                    "Submit your first report from the home screen!");
                            txtStats.setText("No reports yet");

                            // Create a test report with videoUrls field
                            createTestReportForUser(userId);
                        }

                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);

                        Log.e(TAG, "Database error: " + databaseError.getMessage() +
                                " | Code: " + databaseError.getCode());

                        String errorMsg;
                        switch (databaseError.getCode()) {
                            case DatabaseError.PERMISSION_DENIED:
                                errorMsg = "Permission denied. Check Firebase security rules.";
                                break;
                            case DatabaseError.DISCONNECTED:
                                errorMsg = "Network disconnected. Check internet connection.";
                                break;
                            default:
                                errorMsg = "Error: " + databaseError.getMessage();
                        }

                        Toast.makeText(ReportHistoryActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        txtNoReports.setText("⚠️ " + errorMsg);
                        txtNoReports.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void sortReportsByDate() {
        Collections.sort(reportList, new Comparator<EmergencyReport>() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

            @Override
            public int compare(EmergencyReport r1, EmergencyReport r2) {
                try {
                    Date d1 = sdf.parse(r1.getTimestamp());
                    Date d2 = sdf.parse(r2.getTimestamp());
                    return d2.compareTo(d1); // Newest first
                } catch (ParseException | NullPointerException e) {
                    return 0;
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void updateReportStats(int reportsWithImages, int reportsWithVideos) {
        int total = reportList.size();
        int pending = 0, responded = 0, completed = 0;

        for (EmergencyReport report : reportList) {
            String status = report.getStatus();
            if (status == null) continue;

            switch (status.toLowerCase()) {
                case "pending":
                    pending++;
                    break;
                case "responded":
                    responded++;
                    break;
                case "completed":
                    completed++;
                    break;
            }
        }

        if (txtStats != null) {
            String stats = String.format(Locale.getDefault(),
                    "📊 Reports: %d total | ⏳ %d pending | 🚑 %d responded | ✅ %d completed\n" +
                            "📷 Reports with images: %d | 🎬 Reports with videos: %d",
                    total, pending, responded, completed, reportsWithImages, reportsWithVideos);
            txtStats.setText(stats);
        }
    }

    // CREATE TEST REPORT WITH VIDEOURLS FIELD
    private void createTestReportForUser(String userId) {
        Log.d(TAG, "Creating test report for user: " + userId);

        DatabaseReference newReportRef = databaseReference.push();
        String reportId = newReportRef.getKey();

        Map<String, Object> testReport = new HashMap<>();
        testReport.put("id", reportId);
        testReport.put("userId", userId);
        testReport.put("userName", "Test User");
        testReport.put("userRegNo", "COM/0000/2024");
        testReport.put("emergencyType", "Sample Emergency");
        testReport.put("description", "This is a sample emergency report for testing");
        testReport.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
        testReport.put("status", "pending");
        testReport.put("severity", "Medium");
        testReport.put("latitude", -0.3673);
        testReport.put("longitude", 36.9456);
        testReport.put("isPanicAlert", false);
        testReport.put("imageUrls", new ArrayList<String>());

        // ADDED: Include empty videoUrls array for compatibility with video uploads
        testReport.put("videoUrls", new ArrayList<String>());

        // Additional fields for complete test
        testReport.put("responderName", "");
        testReport.put("notes", "");
        testReport.put("respondedAt", "");
        testReport.put("completedAt", "");

        newReportRef.setValue(testReport)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Test report created successfully with videoUrls field");
                    Toast.makeText(this,
                            "Created test report with video support. Refresh to see it!",
                            Toast.LENGTH_SHORT).show();

                    // Also test with a report that has videos
                    createSampleVideoReport(userId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create test report: " + e.getMessage());
                    Toast.makeText(this,
                            "Failed to create test report: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // OPTIONAL: Create a sample report with video URLs for testing
    private void createSampleVideoReport(String userId) {
        DatabaseReference videoReportRef = databaseReference.push();
        String reportId = videoReportRef.getKey();

        // Sample video URLs (replace with actual Cloudinary URLs in production)
        List<String> sampleVideoUrls = new ArrayList<>();
        sampleVideoUrls.add("https://res.cloudinary.com/dknax0k01/video/upload/v1700000000/sample-emergency-video.mp4");

        List<String> sampleImageUrls = new ArrayList<>();
        sampleImageUrls.add("https://res.cloudinary.com/dknax0k01/image/upload/v1700000000/sample-emergency.jpg");

        Map<String, Object> videoReport = new HashMap<>();
        videoReport.put("id", reportId);
        videoReport.put("userId", userId);
        videoReport.put("userName", "Test Video User");
        videoReport.put("userRegNo", "COM/1111/2024");
        videoReport.put("emergencyType", "Video Test Emergency");
        videoReport.put("description", "This report includes both images and videos for testing");
        videoReport.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                new Date(System.currentTimeMillis() - 3600000))); // 1 hour ago
        videoReport.put("status", "responded");
        videoReport.put("severity", "High");
        videoReport.put("latitude", -0.3675);
        videoReport.put("longitude", 36.9458);
        videoReport.put("isPanicAlert", false);
        videoReport.put("imageUrls", sampleImageUrls);
        videoReport.put("videoUrls", sampleVideoUrls); // Video URLs included
        videoReport.put("responderName", "Dr. John Doe");
        videoReport.put("notes", "Video evidence available for review");
        videoReport.put("respondedAt", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(System.currentTimeMillis() - 1800000))); // 30 minutes ago
        videoReport.put("completedAt", "");

        videoReportRef.setValue(videoReport)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Sample video report created successfully");
                    Toast.makeText(this,
                            "Also created a sample report with video for testing",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create video report: " + e.getMessage());
                });
    }

    // DEBUG METHODS
    private void testDatabaseConnection() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference testRef = FirebaseDatabase.getInstance().getReference("test_connection");

        Map<String, Object> testData = new HashMap<>();
        testData.put("test_time", new Date().toString());
        testData.put("user_id", user.getUid());
        testData.put("app_name", "DKUT Medical Alert");
        testData.put("has_video_support", true); // Indicate video feature is enabled

        testRef.setValue(testData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✓ Firebase write SUCCESS (Video support: ON)", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Write successful to: test_connection");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "✗ Write FAILED: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Write failed: " + e.getMessage());
                });
    }

    private void debugDatabaseContents() {
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        rootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                StringBuilder debugInfo = new StringBuilder();
                debugInfo.append("=== DATABASE CONTENTS ===\n");

                for (DataSnapshot child : snapshot.getChildren()) {
                    debugInfo.append("Node: ").append(child.getKey())
                            .append(" | Children: ").append(child.getChildrenCount())
                            .append("\n");

                    // Check if this is the emergency_reports node
                    if ("emergency_reports".equals(child.getKey())) {
                        debugInfo.append("  Scanning emergency_reports for videoUrls...\n");
                        for (DataSnapshot report : child.getChildren()) {
                            if (report.hasChild("videoUrls")) {
                                debugInfo.append("    Report ").append(report.getKey())
                                        .append(" has videoUrls field\n");
                            }
                        }
                    }
                }

                Log.d(TAG, debugInfo.toString());
                Toast.makeText(ReportHistoryActivity.this,
                        "Database scan complete. Video support check done.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Debug scan failed: " + error.getMessage());
            }
        });
    }
}