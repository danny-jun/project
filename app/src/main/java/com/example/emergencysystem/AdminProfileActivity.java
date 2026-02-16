package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView txtFullName, txtEmail, txtRole, txtCreatedAt, txtPFNo;
    private TextView txtTotalReports, txtCriticalReports;
    private Button btnLogout, btnBack;
    private ProgressBar progressBar;
    private LinearLayout layoutProfileContent;
    private CardView cardProfileInfo, cardStats;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private DatabaseReference reportsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_profile);

        mAuth = FirebaseAuth.getInstance();
        initUI();
        setupClickListeners();
        loadAdminProfile();
    }

    private void initUI() {
        txtFullName = findViewById(R.id.txtFullName);
        txtEmail = findViewById(R.id.txtEmail);
        txtRole = findViewById(R.id.txtRole);
        txtCreatedAt = findViewById(R.id.txtCreatedAt);
        txtPFNo = findViewById(R.id.txtPFNo);
        txtTotalReports = findViewById(R.id.txtTotalReports);
        txtCriticalReports = findViewById(R.id.txtCriticalReports);
        btnLogout = findViewById(R.id.btnLogout);
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        layoutProfileContent = findViewById(R.id.layoutProfileContent);
        cardProfileInfo = findViewById(R.id.cardProfileInfo);
        cardStats = findViewById(R.id.cardStats);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            logoutAdmin();
        });
    }

    private void loadAdminProfile() {
        progressBar.setVisibility(android.view.View.VISIBLE);
        layoutProfileContent.setVisibility(android.view.View.GONE);

        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            String email = mAuth.getCurrentUser().getEmail();

            userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String fullName = snapshot.child("fullName").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);
                        String pfNo = snapshot.child("pfNo").getValue(String.class);
                        Long createdAt = snapshot.child("createdAt").getValue(Long.class);

                        txtFullName.setText(fullName != null ? fullName : "N/A");
                        txtEmail.setText(email != null ? email : "N/A");
                        txtPFNo.setText(pfNo != null ? pfNo : "Not provided");
                        txtRole.setText(role != null ? role.toUpperCase() : "MEDICAL OFFICER");
                        txtRole.setBackgroundColor(0xFFE53935); // Red for medical officer/staff

                        if (createdAt != null) {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                                    "yyyy-MM-dd HH:mm:ss",
                                    java.util.Locale.getDefault()
                            );
                            txtCreatedAt.setText(sdf.format(new java.util.Date(createdAt)));
                        }

                        layoutProfileContent.setVisibility(android.view.View.VISIBLE);
                        progressBar.setVisibility(android.view.View.GONE);
                        
                        // Load reports statistics
                        loadAdminStats();
                    } else {
                        showError("Profile not found");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showError("Error loading profile: " + error.getMessage());
                }
            });
        } else {
            showError("User not authenticated");
        }
    }

    private void loadAdminStats() {
        reportsRef = FirebaseDatabase.getInstance().getReference("emergency_reports");
        
        reportsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalReports = 0;
                int criticalCount = 0;

                for (DataSnapshot snap : snapshot.getChildren()) {
                    try {
                        EmergencyReport report = snap.getValue(EmergencyReport.class);
                        if (report != null) {
                            totalReports++;
                            if ("Critical".equalsIgnoreCase(report.getSeverity())) {
                                criticalCount++;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                }

                txtTotalReports.setText(String.valueOf(totalReports));
                txtCriticalReports.setText(String.valueOf(criticalCount));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Ignore errors
            }
        });
    }

    private void logoutAdmin() {
        // Show confirmation dialog
        new android.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    progressBar.setVisibility(android.view.View.VISIBLE);
                    mAuth.signOut();
                    Toast.makeText(AdminProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(AdminProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(android.view.View.GONE);
        layoutProfileContent.setVisibility(android.view.View.VISIBLE);
        txtFullName.setText(message);
    }
}
