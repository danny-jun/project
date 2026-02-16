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
                    "📊 Total: %d | ⏳ Pending: %d | � Responded: %d | ✅ Completed: %d\n" +
                            "📷 Images: %d reports | 🎬 Videos: %d reports",
                    total, pending, responded, completed, reportsWithImages, reportsWithVideos);
            txtStats.setText(stats);
        }
    }
}