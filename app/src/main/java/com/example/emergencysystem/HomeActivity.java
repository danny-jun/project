package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class HomeActivity extends AppCompatActivity {

    private TextView txtUserName;
    private Button btnEmergency, btnReportHistory;
    private CardView cardCardiac, cardInjury, cardFainting, cardAllergy, cardAsthma, cardOther;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize all UI components
        initUI();

        // Set click listeners
        setupClickListeners();

        // Set user info (you can get this from login)
        txtUserName.setText("Student");
    }

    private void initUI() {
        txtUserName = findViewById(R.id.txtUserName);
        btnEmergency = findViewById(R.id.btnEmergency);
        btnReportHistory = findViewById(R.id.btnReportHistory);

        cardCardiac = findViewById(R.id.cardCardiac);
        cardInjury = findViewById(R.id.cardInjury);
        cardFainting = findViewById(R.id.cardFainting);
        cardAllergy = findViewById(R.id.cardAllergy);
        cardAsthma = findViewById(R.id.cardAsthma);
        cardOther = findViewById(R.id.cardOther);
    }

    private void setupClickListeners() {
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

        // Quick Report Cards with error handling
        View.OnClickListener cardClickListener = v -> {
            try {
                String emergencyType = "";
                if (v.getId() == R.id.cardCardiac) emergencyType = "Cardiac Arrest";
                else if (v.getId() == R.id.cardInjury) emergencyType = "Accident/Injury";
                else if (v.getId() == R.id.cardFainting) emergencyType = "Fainting";
                else if (v.getId() == R.id.cardAllergy) emergencyType = "Allergic Reaction";
                else if (v.getId() == R.id.cardAsthma) emergencyType = "Asthma Attack";
                else if (v.getId() == R.id.cardOther) emergencyType = "Other Emergency";

                Toast.makeText(HomeActivity.this, "Reporting: " + emergencyType, Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(HomeActivity.this, EmergencyReportActivity.class);
                intent.putExtra("EMERGENCY_TYPE", emergencyType);
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(HomeActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        };

        cardCardiac.setOnClickListener(cardClickListener);
        cardInjury.setOnClickListener(cardClickListener);
        cardFainting.setOnClickListener(cardClickListener);
        cardAllergy.setOnClickListener(cardClickListener);
        cardAsthma.setOnClickListener(cardClickListener);
        cardOther.setOnClickListener(cardClickListener);
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