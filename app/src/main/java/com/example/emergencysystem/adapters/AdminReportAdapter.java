package com.example.emergencysystem.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.emergencysystem.EmergencyReport;
import com.example.emergencysystem.R;

import java.util.List;

public class AdminReportAdapter extends RecyclerView.Adapter<AdminReportAdapter.AdminReportViewHolder> {

    private final List<EmergencyReport> reportList;
    private final Context context;
    private OnItemClickListener onItemClickListener;

    public AdminReportAdapter(List<EmergencyReport> reportList, Context context) {
        this.reportList = reportList;
        this.context = context;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @NonNull
    @Override
    public AdminReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_report, parent, false);
        return new AdminReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminReportViewHolder holder, int position) {
        EmergencyReport report = reportList.get(position);

        if (report == null) return;

        // Emergency type
        holder.txtEmergencyType.setText(report.getEmergencyType() != null ?
                report.getEmergencyType() : "Emergency");

        // Time ago
        holder.txtTimeAgo.setText(getTimeAgo(report));

        // Severity
        String severity = report.getSeverity() != null ? report.getSeverity() : "Medium";
        holder.txtSeverity.setText(severity);
        holder.txtSeverity.setTextColor(getSeverityColor(severity));

        // Location
        holder.txtLocation.setText(report.getLocation() != null ?
                "📍 " + report.getLocation() : "📍 Location unknown");

        // Description preview
        String description = report.getDescription();
        if (description != null && !description.isEmpty()) {
            holder.txtDescription.setText(description.length() > 80 ?
                    description.substring(0, 80) + "..." : description);
            holder.txtDescription.setVisibility(View.VISIBLE);
        } else {
            holder.txtDescription.setVisibility(View.GONE);
        }

        // Panic alert indicator
        if (report.isPanicAlert()) {
            holder.panicIndicator.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(0x20F44336); // Light red tint
        } else {
            holder.panicIndicator.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(0xFFFFFFFF);
        }

        // Load thumbnail if available
        loadThumbnail(holder, report);

        // Click handler
        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    private String getTimeAgo(EmergencyReport report) {
        try {
            long reportTime = report.getTimestampMillis();
            long now = System.currentTimeMillis();
            long diff = now - reportTime;

            long minutes = diff / (60 * 1000);
            long hours = diff / (60 * 60 * 1000);
            long days = diff / (24 * 60 * 60 * 1000);

            if (minutes < 1) {
                return "Just now";
            } else if (minutes < 60) {
                return minutes + " min ago";
            } else if (hours < 24) {
                return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
            } else {
                return days + " day" + (days > 1 ? "s" : "") + " ago";
            }
        } catch (Exception e) {
            return report.getFormattedDate();
        }
    }

    private int getSeverityColor(String severity) {
        if (severity == null) return 0xFF757575;

        switch (severity.toLowerCase()) {
            case "low":
                return 0xFF4CAF50; // Green
            case "medium":
                return 0xFFFFA726; // Orange
            case "high":
                return 0xFFFF5722; // Deep Orange
            case "critical":
                return 0xFFE53935; // Red
            default:
                return 0xFF757575; // Gray
        }
    }

    private void loadThumbnail(AdminReportViewHolder holder, EmergencyReport report) {
        String thumbnailUrl = null;

        if (report.getImageUrls() != null && !report.getImageUrls().isEmpty()) {
            thumbnailUrl = report.getImageUrls().get(0);
        } else if (report.getVideoUrls() != null && !report.getVideoUrls().isEmpty()) {
            String videoUrl = report.getVideoUrls().get(0);
            if (videoUrl.contains("cloudinary.com")) {
                thumbnailUrl = videoUrl.replace("/upload/", "/upload/so_0,w_120,h_120,c_fill/") + ".jpg";
            } else {
                thumbnailUrl = videoUrl;
            }
        }

        if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
            holder.imgThumbnail.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(thumbnailUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_broken_image)
                    .centerCrop()
                    .override(120, 120)
                    .into(holder.imgThumbnail);
        } else {
            holder.imgThumbnail.setVisibility(View.GONE);
        }
    }

    static class AdminReportViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView txtEmergencyType, txtTimeAgo, txtSeverity, txtLocation, txtDescription;
        ImageView imgThumbnail, panicIndicator;

        AdminReportViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardReport);
            txtEmergencyType = itemView.findViewById(R.id.txtEmergencyType);
            txtTimeAgo = itemView.findViewById(R.id.txtTimeAgo);
            txtSeverity = itemView.findViewById(R.id.txtSeverity);
            txtLocation = itemView.findViewById(R.id.txtLocation);
            txtDescription = itemView.findViewById(R.id.txtDescription);
            imgThumbnail = itemView.findViewById(R.id.imgThumbnail);
            panicIndicator = itemView.findViewById(R.id.panicIndicator);
        }
    }
}
