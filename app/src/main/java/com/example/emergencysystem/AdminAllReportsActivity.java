package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysystem.adapters.AdminReportAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminAllReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminReportAdapter adapter;
    private List<EmergencyReport> allReports;
    private List<EmergencyReport> filteredReports;
    
    private Spinner spinnerFilter;
    private ProgressBar progressBar;
    private TextView txtNoReports, txtResultCount;
    private Button btnBack;
    
    private DatabaseReference reportsRef;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_all_reports);

        reportsRef = FirebaseDatabase.getInstance().getReference("emergency_reports");

        initUI();
        setupRecyclerView();
        setupFilterSpinner();
        loadAllReports();
    }

    private void initUI() {
        recyclerView = findViewById(R.id.recyclerViewAllReports);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        progressBar = findViewById(R.id.progressBar);
        txtNoReports = findViewById(R.id.txtNoReports);
        txtResultCount = findViewById(R.id.txtResultCount);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        allReports = new ArrayList<>();
        filteredReports = new ArrayList<>();
        adapter = new AdminReportAdapter(filteredReports, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            if (position < filteredReports.size()) {
                EmergencyReport report = filteredReports.get(position);
                if (report != null && report.getId() != null) {
                    Intent intent = new Intent(this, AdminRespondActivity.class);
                    intent.putExtra("REPORT_ID", report.getId());
                    startActivity(intent);
                }
            }
        });
    }

    private void setupFilterSpinner() {
        String[] filters = {"All Reports", "Pending", "Responded", "Completed", 
                           "Critical", "High", "Panic Alerts", "Today"};
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filters);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(spinnerAdapter);

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentFilter = "all"; break;
                    case 1: currentFilter = "pending"; break;
                    case 2: currentFilter = "responded"; break;
                    case 3: currentFilter = "completed"; break;
                    case 4: currentFilter = "critical"; break;
                    case 5: currentFilter = "high"; break;
                    case 6: currentFilter = "panic"; break;
                    case 7: currentFilter = "today"; break;
                }
                applyFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadAllReports() {
        progressBar.setVisibility(View.VISIBLE);

        reportsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allReports.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    try {
                        EmergencyReport report = snapshot.getValue(EmergencyReport.class);
                        if (report != null) {
                            report.setId(snapshot.getKey());
                            
                            // Auto-categorize severity based on emergency type
                            String categorizedSeverity = report.getAutoCategorizedSeverity();
                            report.setSeverity(categorizedSeverity);
                            
                            allReports.add(report);
                        }
                    } catch (Exception e) {
                        // Skip invalid reports
                    }
                }

                // Sort by priority (Critical > High > Medium > Low) and then by time (newest first)
                Collections.sort(allReports, new java.util.Comparator<EmergencyReport>() {
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

                applyFilter();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminAllReportsActivity.this,
                        "Error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressLint({"SetTextI18n", "NotifyDataSetChanged"})
    private void applyFilter() {
        filteredReports.clear();

        long todayStart = getTodayStartMillis();

        for (EmergencyReport report : allReports) {
            boolean include = false;

            switch (currentFilter) {
                case "all":
                    include = true;
                    break;
                case "pending":
                    include = "pending".equalsIgnoreCase(report.getStatus());
                    break;
                case "responded":
                    include = "responded".equalsIgnoreCase(report.getStatus());
                    break;
                case "completed":
                    include = "completed".equalsIgnoreCase(report.getStatus());
                    break;
                case "critical":
                    include = "Critical".equalsIgnoreCase(report.getSeverity());
                    break;
                case "high":
                    include = "High".equalsIgnoreCase(report.getSeverity());
                    break;
                case "panic":
                    include = report.isPanicAlert();
                    break;
                case "today":
                    try {
                        include = report.getTimestampMillis() >= todayStart;
                    } catch (Exception e) {
                        include = false;
                    }
                    break;
            }

            if (include) {
                filteredReports.add(report);
            }
        }

        // Update UI
        if (filteredReports.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            txtNoReports.setVisibility(View.VISIBLE);
            txtResultCount.setText("No reports found");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            txtNoReports.setVisibility(View.GONE);
            txtResultCount.setText(filteredReports.size() + " report(s) found");
            adapter.notifyDataSetChanged();
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
     * 1. Critical + Panic (450+)
     * 2. Critical (400)
     * 3. High + Panic (350+)
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
