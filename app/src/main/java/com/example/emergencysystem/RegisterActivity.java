package com.example.emergencysystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword, etFullName, etRegNo, etPFNo, etCourse;
    RadioGroup radioGroupRole;
    Button btnRegister;

    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFullName = findViewById(R.id.etFullName);
        etRegNo = findViewById(R.id.etRegNo);
        etPFNo = findViewById(R.id.etPFNo);
        etCourse = findViewById(R.id.etCourse);
        radioGroupRole = findViewById(R.id.radioGroupRole);
        btnRegister = findViewById(R.id.btnRegister);

        // Set up role change listener to show/hide role-specific fields
        radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioRoleMedicalOfficer || checkedId == R.id.radioRoleStaff) {
                // Medical Officer and Staff - hide registration no and course, change hint to PF Number
                etRegNo.setVisibility(android.view.View.GONE);
                etCourse.setVisibility(android.view.View.GONE);
                etPFNo.setHint("PF Number (e.g., +254712345678)");
            } else if (checkedId == R.id.radioRoleUser) {
                // Student - show registration no and course, change hint to Phone Number
                etRegNo.setVisibility(android.view.View.VISIBLE);
                etCourse.setVisibility(android.view.View.VISIBLE);
                etPFNo.setHint("Phone Number (e.g., +254712345678)");
            }
        });

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String regNo = etRegNo.getText().toString().trim();
        String pfNo = etPFNo.getText().toString().trim();
        String course = etCourse.getText().toString().trim();
        
        int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();
        String role = "user"; // default
        
        if (selectedRoleId == R.id.radioRoleMedicalOfficer) {
            role = "medicalOfficer";
        } else if (selectedRoleId == R.id.radioRoleStaff) {
            role = "staff";
        } else if (selectedRoleId == R.id.radioRoleUser) {
            role = "user";
        }

        // Validation - Common for all roles
        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            Toast.makeText(this, "Email, password, and full name are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (pfNo.isEmpty()) {
            String fieldName = role.equals("user") ? "Phone Number" : "PF Number";
            Toast.makeText(this, fieldName + " is required", Toast.LENGTH_SHORT).show();
            return;
        }

        // User-specific validation
        if (role.equals("user")) {
            if (course.isEmpty()) {
                Toast.makeText(this, "Course/Department is required for students", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Validate phone/PF number format (basic validation)
        if (!pfNo.matches("^[+]?[0-9]{10,}$")) {
            String fieldName = role.equals("user") ? "Phone Number" : "PF Number";
            Toast.makeText(this, "Please enter a valid " + fieldName, Toast.LENGTH_SHORT).show();
            return;
        }

        final String userRole = role;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        String userId = mAuth.getCurrentUser().getUid();
                        
                        // Save user profile to database
                        Map<String, Object> userProfile = new HashMap<>();
                        userProfile.put("email", email);
                        userProfile.put("fullName", fullName);
                        userProfile.put("pfNo", pfNo);
                        userProfile.put("role", userRole);
                        userProfile.put("createdAt", System.currentTimeMillis());
                        
                        // Only add user-specific fields for user role
                        if (userRole.equals("user")) {
                            userProfile.put("regNo", regNo.isEmpty() ? "N/A" : regNo);
                            userProfile.put("course", course);
                        }
                        
                        databaseReference.child(userId).setValue(userProfile)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registration successful as " + userRole, Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, LoginActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(this,
                                Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
