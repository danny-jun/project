package com.example.emergencysystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AllEmergenciesAdapter extends RecyclerView.Adapter<AllEmergenciesAdapter.ViewHolder> {

    private List<EmergencyReport> emergenciesList;

    public AllEmergenciesAdapter(List<EmergencyReport> emergenciesList) {
        this.emergenciesList = emergenciesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_emergency, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyReport report = emergenciesList.get(position);

        // Set emergency type and user info
        holder.txtEmergencyType.setText(report.getEmergencyType() != null ? 
                report.getEmergencyType() : "Unknown Emergency");
        holder.txtUserName.setText("Reported by: " + (report.getUserName() != null ? 
                report.getUserName() : "Unknown User"));
        holder.txtDescription.setText(report.getDescription() != null ? 
                report.getDescription() : "No description provided");

        // Set severity with color
        if (report.getSeverity() != null) {
            holder.txtSeverity.setText("Severity: " + report.getSeverity());
            switch (report.getSeverity().toLowerCase()) {
                case "critical":
                    holder.cardEmergency.setCardBackgroundColor(0xFFffebee); // Light red
                    holder.txtSeverity.setTextColor(0xFFc62828); // Dark red
                    break;
                case "high":
                    holder.cardEmergency.setCardBackgroundColor(0xFFffe0b2); // Light orange
                    holder.txtSeverity.setTextColor(0xFFe65100); // Dark orange
                    break;
                case "medium":
                    holder.cardEmergency.setCardBackgroundColor(0xFFfff9c4); // Light yellow
                    holder.txtSeverity.setTextColor(0xFFf57f17); // Dark yellow
                    break;
                default:
                    holder.cardEmergency.setCardBackgroundColor(0xFFe8f5e9); // Light green
                    holder.txtSeverity.setTextColor(0xFF2e7d32); // Dark green
            }
        }

        // Set status with color badge
        if (report.getStatus() != null) {
            holder.txtStatus.setText("Status: " + report.getStatus().toUpperCase());
            switch (report.getStatus().toLowerCase()) {
                case "pending":
                    holder.txtStatus.setBackgroundColor(0xFFff9800); // Orange
                    break;
                case "responded":
                    holder.txtStatus.setBackgroundColor(0xFF2196F3); // Blue
                    break;
                case "completed":
                    holder.txtStatus.setBackgroundColor(0xFF4CAF50); // Green
                    break;
                default:
                    holder.txtStatus.setBackgroundColor(0xFF757575); // Gray
            }
            holder.txtStatus.setTextColor(0xFFffffff); // White text
        }

        // Set timestamp
        if (report.getTimestamp() != null) {
            holder.txtTimestamp.setText("Reported: " + report.getTimestamp());
        }

        // Show responder info if responded
        if (report.getResponderName() != null && !report.getResponderName().isEmpty()) {
            holder.txtResponder.setVisibility(View.VISIBLE);
            holder.txtResponder.setText("Responded by: " + report.getResponderName());
        } else {
            holder.txtResponder.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return emergenciesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtEmergencyType, txtUserName, txtDescription, txtSeverity, 
                 txtStatus, txtTimestamp, txtResponder;
        CardView cardEmergency;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardEmergency = itemView.findViewById(R.id.cardEmergency);
            txtEmergencyType = itemView.findViewById(R.id.txtEmergencyType);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            txtSeverity = itemView.findViewById(R.id.txtSeverity);
            txtStatus = itemView.findViewById(R.id.txtStatus);
            txtTimestamp = itemView.findViewById(R.id.txtTimestamp);
            txtResponder = itemView.findViewById(R.id.txtResponder);
        }
    }
}
