package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysystem.adapters.AdminReportAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    
    // Analytics Cards
    private TextView txtTotalReports, txtPendingReports, txtRespondedReports, txtCompletedReports;
    private TextView txtCriticalReports, txtHighReports, txtTodayReports, txtPanicAlerts;
    
    // Charts/Stats
    private CardView cardPending, cardResponded, cardCompleted, cardCritical;
    private LinearLayout statsContainer;
    
    // RecyclerView for pending reports
    private RecyclerView recyclerViewPending;
    private AdminReportAdapter adminReportAdapter;
    private List<EmergencyReport> pendingReports;
    
    private ProgressBar progressBar;
    private TextView txtNoPending;
    private Button btnLogout, btnRefresh, btnViewAll, btnAdminProfile;
    
    private DatabaseReference reportsRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        reportsRef = FirebaseDatabase.getInstance().getReference("emergency_reports");

        initUI();
        setupRecyclerView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadDashboardData();
    }

    private void initUI() {
        // Analytics TextViews
        txtTotalReports = findViewById(R.id.txtTotalReports);
        txtPendingReports = findViewById(R.id.txtPendingReports);
        txtRespondedReports = findViewById(R.id.txtRespondedReports);
        txtCompletedReports = findViewById(R.id.txtCompletedReports);
        txtCriticalReports = findViewById(R.id.txtCriticalReports);
        txtHighReports = findViewById(R.id.txtHighReports);
        txtTodayReports = findViewById(R.id.txtTodayReports);
        txtPanicAlerts = findViewById(R.id.txtPanicAlerts);
        
        // Cards
        cardPending = findViewById(R.id.cardPending);
        cardResponded = findViewById(R.id.cardResponded);
        cardCompleted = findViewById(R.id.cardCompleted);
        cardCritical = findViewById(R.id.cardCritical);
        
        // RecyclerView
        recyclerViewPending = findViewById(R.id.recyclerViewPending);
        progressBar = findViewById(R.id.progressBar);
        txtNoPending = findViewById(R.id.txtNoPending);
        
        // Buttons
        btnLogout = findViewById(R.id.btnLogout);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnViewAll = findViewById(R.id.btnViewAll);
        btnAdminProfile = findViewById(R.id.btnAdminProfile);

        btnAdminProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        mAuth.signOut();
                        Toast.makeText(AdminDashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        btnRefresh.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            loadDashboardData();
        });

        btnViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminAllReportsActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        pendingReports = new ArrayList<>();
        adminReportAdapter = new AdminReportAdapter(pendingReports, this);

        recyclerViewPending.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewPending.setAdapter(adminReportAdapter);

        adminReportAdapter.setOnItemClickListener(position -> {
            if (position < pendingReports.size()) {
                EmergencyReport report = pendingReports.get(position);
                if (report != null && report.getId() != null) {
                    Intent intent = new Intent(this, AdminRespondActivity.class);
                    intent.putExtra("REPORT_ID", report.getId());
                    startActivity(intent);
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadDashboardData() {
        progressBar.setVisibility(View.VISIBLE);

        reportsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Analytics counters
                int totalReports = 0;
                int pendingCount = 0;
                int respondedCount = 0;
                int completedCount = 0;
                int criticalCount = 0;
                int highCount = 0;
                int todayCount = 0;
                int panicAlertCount = 0;

                pendingReports.clear();

                long todayStart = getTodayStartMillis();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        EmergencyReport report = snapshot.getValue(EmergencyReport.class);
                        if (report != null) {
                            report.setId(snapshot.getKey());
                            
                            // Auto-categorize severity based on emergency type
                            String categorizedSeverity = report.getAutoCategorizedSeverity();
                            report.setSeverity(categorizedSeverity);
                            
                            totalReports++;

                            // Status counts
                            String status = report.getStatus() != null ? report.getStatus().toLowerCase() : "pending";
                            switch (status) {
                                case "pending":
                                    pendingCount++;
                                    pendingReports.add(report);
                                    break;
                                case "responded":
                                    respondedCount++;
                                    break;
                                case "completed":
                                    completedCount++;
                                    break;
                            }

                            // Severity counts using auto-categorized severity
                            if ("Critical".equalsIgnoreCase(categorizedSeverity)) {
                                criticalCount++;
                            } else if ("High".equalsIgnoreCase(categorizedSeverity)) {
                                highCount++;
                            }

                            // Today's reports
                            try {
                                long reportTime = report.getTimestampMillis();
                                if (reportTime >= todayStart) {
                                    todayCount++;
                                }
                            } catch (Exception e) {
                                // Ignore parsing errors
                            }

                            // Panic alerts
                            if (report.isPanicAlert()) {
                                panicAlertCount++;
                            }
                        }
                    } catch (Exception e) {
                        // Skip invalid reports
                    }
                }

                // Sort pending reports by priority (Critical > High > Medium > Low) and then by time (newest first)
                Collections.sort(pendingReports, new Comparator<EmergencyReport>() {
                    @Override
                    public int compare(EmergencyReport r1, EmergencyReport r2) {
                        try {
                            // First, compare by priority (severity)
                            int priority1 = getPriorityValue(r1.getSeverity(), r1.isPanicAlert());
                            int priority2 = getPriorityValue(r2.getSeverity(), r2.isPanicAlert());
                            
                            if (priority1 != priority2) {
                                return Integer.compare(priority2, priority1); // Higher priority first (descending)
                            }
                            
                            // If same priority, sort by time (newest first)
                            return Long.compare(r2.getTimestampMillis(), r1.getTimestampMillis());
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                });

                // Update UI
                txtTotalReports.setText(String.valueOf(totalReports));
                txtPendingReports.setText(String.valueOf(pendingCount));
                txtRespondedReports.setText(String.valueOf(respondedCount));
                txtCompletedReports.setText(String.valueOf(completedCount));
                txtCriticalReports.setText(String.valueOf(criticalCount));
                txtHighReports.setText(String.valueOf(highCount));
                txtTodayReports.setText(String.valueOf(todayCount));
                txtPanicAlerts.setText(String.valueOf(panicAlertCount));

                // Update card colors based on urgency
                updateCardColors(pendingCount, criticalCount);

                // Update RecyclerView
                if (pendingReports.isEmpty()) {
                    recyclerViewPending.setVisibility(View.GONE);
                    txtNoPending.setVisibility(View.VISIBLE);
                } else {
                    recyclerViewPending.setVisibility(View.VISIBLE);
                    txtNoPending.setVisibility(View.GONE);
                    adminReportAdapter.notifyDataSetChanged();
                }

                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminDashboardActivity.this,
                        "Error loading data: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCardColors(int pendingCount, int criticalCount) {
        // Urgent if there are critical reports or many pending
        if (criticalCount > 0) {
            cardCritical.setCardBackgroundColor(0xFFE53935); // Red
        } else {
            cardCritical.setCardBackgroundColor(0xFFEF5350); // Light red
        }

        if (pendingCount > 10) {
            cardPending.setCardBackgroundColor(0xFFFFA726); // Orange
        } else if (pendingCount > 5) {
            cardPending.setCardBackgroundColor(0xFFFFB74D); // Light orange
        } else {
            cardPending.setCardBackgroundColor(0xFFFFCC80); // Very light orange
        }
    }

    private long getTodayStartMillis() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /**
     * Get priority value for sorting reports in priority queue
     * Higher value = higher priority (should be processed first)
     * 
     * Priority order:
     * 1. Critical + Panic (500+)
     * 2. Critical (400)
     * 3. High + Panic (300+)
     * 4. High (300)
     * 5. Medium (200)
     * 6. Low (100)
     */
    private int getPriorityValue(String severity, boolean isPanicAlert) {
        if (severity == null || severity.isEmpty()) {
            severity = "Medium"; // Default to medium if null
        }

        // Panic alerts get a boost to priority
        int panicBoost = isPanicAlert ? 50 : 0;

        switch (severity.toLowerCase()) {
            case "critical":
                return 400 + panicBoost;
            case "high":
                return 300 + panicBoost;
            case "medium":
                return 200;
            case "low":
                return 100;
            default:
                return 200; // Default to medium
        }
    }
}
