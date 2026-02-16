package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.os.Bundle;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllEmergenciesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewEmergencies;
    private ProgressBar progressBar;
    private TextView txtNoEmergencies;
    private Button btnBack;
    private DatabaseReference reportsRef;
    private List<EmergencyReport> emergenciesList;
    private AllEmergenciesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_emergencies);

        // Initialize UI
        initUI();

        // Setup RecyclerView
        setupRecyclerView();

        // Load emergencies from Firebase
        loadAllEmergencies();

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void initUI() {
        recyclerViewEmergencies = findViewById(R.id.recyclerViewEmergencies);
        progressBar = findViewById(R.id.progressBar);
        txtNoEmergencies = findViewById(R.id.txtNoEmergencies);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        emergenciesList = new ArrayList<>();
        adapter = new AllEmergenciesAdapter(emergenciesList);
        recyclerViewEmergencies.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewEmergencies.setAdapter(adapter);
    }

    private void loadAllEmergencies() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        txtNoEmergencies.setVisibility(android.view.View.GONE);
        recyclerViewEmergencies.setVisibility(android.view.View.GONE);

        reportsRef = FirebaseDatabase.getInstance().getReference("emergency_reports");

        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                emergenciesList.clear();

                if (snapshot.exists()) {
                    for (DataSnapshot reportSnapshot : snapshot.getChildren()) {
                        try {
                            EmergencyReport report = reportSnapshot.getValue(EmergencyReport.class);
                            if (report != null) {
                                report.setId(reportSnapshot.getKey());
                                emergenciesList.add(0, report); // Add to beginning (newest first)
                            }
                        } catch (Exception e) {
                            Toast.makeText(AllEmergenciesActivity.this, "Error parsing report", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if (emergenciesList.isEmpty()) {
                        txtNoEmergencies.setVisibility(android.view.View.VISIBLE);
                        recyclerViewEmergencies.setVisibility(android.view.View.GONE);
                    } else {
                        txtNoEmergencies.setVisibility(android.view.View.GONE);
                        recyclerViewEmergencies.setVisibility(android.view.View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    txtNoEmergencies.setVisibility(android.view.View.VISIBLE);
                    recyclerViewEmergencies.setVisibility(android.view.View.GONE);
                }

                progressBar.setVisibility(android.view.View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(android.view.View.GONE);
                Toast.makeText(AllEmergenciesActivity.this, "Error loading emergencies", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
