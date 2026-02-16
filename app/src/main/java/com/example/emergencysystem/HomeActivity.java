package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class HomeActivity extends AppCompatActivity {

    private TextView txtUserName;
    private Button btnEmergency, btnReportHistory, btnProfile, btnViewEmergencies;
    
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        
        // Initialize all UI components
        initUI();

        // Set click listeners
        setupClickListeners();

        // Load user info
        loadUserInfo();
    }

    private void initUI() {
        txtUserName = findViewById(R.id.txtUserName);
        btnEmergency = findViewById(R.id.btnEmergency);
        btnReportHistory = findViewById(R.id.btnReportHistory);
        btnProfile = findViewById(R.id.btnProfile);
        btnViewEmergencies = findViewById(R.id.btnViewEmergencies);
    }

    private void setupClickListeners() {
        // Profile Button - Navigate based on user role
        btnProfile.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
                
                userRef.get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        String role = snapshot.child("role").getValue(String.class);
                        
                        if ("staff".equals(role)) {
                            // Staff profile
                            Intent intent = new Intent(HomeActivity.this, StaffProfileActivity.class);
                            startActivity(intent);
                        } else {
                            // Student profile
                            Intent intent = new Intent(HomeActivity.this, UserProfileActivity.class);
                            startActivity(intent);
                        }
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(HomeActivity.this, "Error loading profile", Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Emergency Button with error handling
        btnEmergency.setOnClickListener(v -> {
            try {
                Toast.makeText(HomeActivity.this, "Emergency Alert Activated!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, EmergencyReportActivity.class);
                intent.putExtra("EMERGENCY_TYPE", "Panic Alert");
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error opening Emergency Report", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        // View Reported Emergencies Button
        btnViewEmergencies.setOnClickListener(v -> {
            try {
                Toast.makeText(HomeActivity.this, "Opening Reported Emergencies", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, AllEmergenciesActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error opening Emergencies: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

        // Report History Button with error handling
        btnReportHistory.setOnClickListener(v -> {
            try {
                Toast.makeText(HomeActivity.this, "Opening Report History", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(HomeActivity.this, ReportHistoryActivity.class);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error opening Report History", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });
    }

    private void loadUserInfo() {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            
            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String fullName = snapshot.child("fullName").getValue(String.class);
                    if (fullName != null && !fullName.isEmpty()) {
                        txtUserName.setText(fullName);
                    }
                }
            }).addOnFailureListener(e -> {
                // If loading fails, keep default name
                txtUserName.setText("Student");
            });
        }
    }

    @SuppressLint("GestureBackNavigation")
    @Override
    public void onBackPressed() {
        // Confirm exit
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", null)
                .show();
    }
}